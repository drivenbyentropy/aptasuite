/**
 * 
 */
package lib.aptanet;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.logging.Level;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import gnu.trove.map.hash.TByteIntHashMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.StructurePool;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Index;

/**
 * @author Jan Hoinka
 * Provides a custom DataSetIterator for Aptamers encoding sequence
 * and structure to use for Deep Learning applications such as DeepLearing4J
 */
public class SequenceStructureDatasetIterator implements DataSetIterator{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5934657646058196141L;

	/**
	 * Defines if this is a training set or testing set iterator
	 */
	private DataType type;
	
	/**
	 * The data to iterate over
	 */
	private DataInputIterator dataInputIterator;
	
	/**
	 * Calls <code>hasNextTrain</code> or <code>hasNextTest</code> of 
	 * the <code>DataInputIterator</code> depending on <code>type</code>
	 */
	private java.util.function.BooleanSupplier hasNext;
	
	/**
	 * Calls <code>nextTrain</code> or <code>nextTest</code> of 
	 * the <code>DataInputIterator</code> depending on <code>type</code>
	 */
	private java.util.function.Supplier<AptamerLabelPair>  dataInputIteratorNext;
	
	/**
	 * Size of each minibatch (number of examples)
	 */
	private int miniBatchSize = 1;
	
	/**
	 * The aptamer sequences of the experiment
	 */
	private AptamerPool pool = Configuration.getExperiment().getAptamerPool();
	
	/**
	 * The structure information for this iterator
	 */
	private StructurePool bppmPool = Configuration.getExperiment().getBppmPool();
	
	/**
	 * The corresponding base pair probabilities to for the sequences 
	 */
	private StructurePool bppm = Configuration.getExperiment().getBppmPool();
	
	/**
	 * We ignore the primers during the learning process as they are static
	 */
	private int randomizedRegionSize;
	
	/**
	 * If true, fill only the upper triangular matrix in each channel
	 */
	private boolean upperOnly = true;
	
	/**
	 * Defines the order of the channels in the resulting INDArrays
	 */
	private TByteObjectHashMap<TByteIntHashMap> pairsToChannelIndex = new TByteObjectHashMap<TByteIntHashMap>();
	
	/**
	 * The number of channels for the tensor. 16 for each pair of nucleotides + 1 for the Bppm
	 */
	private int channels = 17;
	
	/**
	 * If true, it adds a small percentage of noise to the one hot representation of the
	 * RNA (First 16 channels) in the hopes this will improve learning
	 */
	private boolean withNoise = false;
	
	private double noisePercentage = 0.1;
	
	private Random rand = new Random();
	
	private int cursor = 0;
	
	public SequenceStructureDatasetIterator(DataInputIterator data_input_iterator, DataType data_type, boolean upper_only) {
		
		this.upperOnly = upper_only;
		
		this.type = data_type;
		
		this.dataInputIterator = data_input_iterator;
		
		// set the lambda expressions
		if (type == DataType.TEST) {
			this.hasNext = () -> this.dataInputIterator.hasNextTestData();
			this.dataInputIteratorNext = () -> this.dataInputIterator.nextTestData();
		}
		if (type == DataType.TRAIN) {
			this.hasNext = () -> this.dataInputIterator.hasNextTrainData();
			this.dataInputIteratorNext = () -> this.dataInputIterator.nextTrainData();
		}

		// Set the desired randomized region size
		try {
			randomizedRegionSize = Configuration.getParameters().getInt("Experiment.randomizedRegionSize");
		} catch (NoSuchElementException e) {
			
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			AptaLogger.log(Level.SEVERE, this.getClass(), "No randomized region size was specified. Please check your configuration file.");
			System.exit(1);
		}
		
		// Set the channel index mappings
		this.pairsToChannelIndex.put((byte)'A', new TByteIntHashMap());
		this.pairsToChannelIndex.put((byte)'C', new TByteIntHashMap());
		this.pairsToChannelIndex.put((byte)'G', new TByteIntHashMap());
		this.pairsToChannelIndex.put((byte)'T', new TByteIntHashMap());

		this.pairsToChannelIndex.get((byte)'A').put((byte)'A', 0);
		this.pairsToChannelIndex.get((byte)'A').put((byte)'C', 1);
		this.pairsToChannelIndex.get((byte)'A').put((byte)'G', 2);
		this.pairsToChannelIndex.get((byte)'A').put((byte)'T', 3);
		this.pairsToChannelIndex.get((byte)'C').put((byte)'A', 4);
		this.pairsToChannelIndex.get((byte)'C').put((byte)'C', 5);
		this.pairsToChannelIndex.get((byte)'C').put((byte)'G', 6);
		this.pairsToChannelIndex.get((byte)'C').put((byte)'T', 7);
		this.pairsToChannelIndex.get((byte)'G').put((byte)'A', 8);
		this.pairsToChannelIndex.get((byte)'G').put((byte)'C', 9);
		this.pairsToChannelIndex.get((byte)'G').put((byte)'G', 10);
		this.pairsToChannelIndex.get((byte)'G').put((byte)'T', 11);
		this.pairsToChannelIndex.get((byte)'T').put((byte)'A', 12);
		this.pairsToChannelIndex.get((byte)'T').put((byte)'C', 13);
		this.pairsToChannelIndex.get((byte)'T').put((byte)'G', 14);
		this.pairsToChannelIndex.get((byte)'T').put((byte)'T', 15);
		
	}
	
	
	@Override
	public boolean hasNext() {
		return this.hasNext.getAsBoolean();
	}

	@Override
	public DataSet next() {
		return next(miniBatchSize);
	}

	
	/* 
	 * Does this DataSetIterator support asynchronous prefetching of multiple DataSet objects? 
	 * Most DataSetIterators do, but in some cases it may not make sense to wrap this iterator 
	 * in an iterator that does asynchronous prefetching.
	 */
	@Override
	public boolean asyncSupported() {
		return false;
	}

	@Override
	public int batch() {
		return miniBatchSize;
	}
	
	public void setBatchsize(int size) {
		miniBatchSize = size;
	}

	@Override
	public int cursor() {
		return cursor;
	}

	@Override
	public List<String> getLabels() {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public DataSetPreProcessor getPreProcessor() {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public int inputColumns() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DataSet next(int num) {
		
		// Obtain as many aptamer ids as required and allowed
		ArrayList<AptamerLabelPair> batch_ids = new ArrayList<AptamerLabelPair>(num);
		while (batch_ids.size() < num && this.hasNext()) {
			
			batch_ids.add(this.dataInputIteratorNext.get());
			
		}
		
		// Allocate space:
        // Note the order here:
        //  dimension 0 = number of examples in minibatch
		//  dimension 1 = Number of channels (5 for A,C,G,T,PairProb) 
        //  dimension 2 = aptamer length (height)
        //  dimension 3 = aptamer length (width)

        // Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"
		INDArray input = Nd4j.zeros(new int[]{batch_ids.size(),this.channels,this.randomizedRegionSize, this.randomizedRegionSize}, 'f');

	
		// We have as many labels as we have items in the batch. 
		// Here, they correspond to the chosen score since we are dealing with a
		// classification problem.
		INDArray labels = Nd4j.create(new int[]{batch_ids.size(), 1 }, 'f'); //1 because its regression based
		
		// Now iterate over the batch of aptamers and create fill the tensor
		for (int b=0; b<batch_ids.size(); b++) {

			// Add noise if required
			if (this.withNoise) {
				for (int c=0; c<this.channels-1; c++) {
					for (int h=0; h<this.randomizedRegionSize; h++) {
						for (int w=0; w<this.randomizedRegionSize; w++) {
							input.putScalar(new int[] {b, c , h, w}, rand.nextDouble()*noisePercentage);
						}
					}
				}
			}
			
			// Take care of the label first
			labels.putScalar(new int[] {b,0}, batch_ids.get(b).score);
			
			// Get the aptamer sequence
			AptamerBounds bounds = pool.getAptamerBounds(batch_ids.get(b).aptamer_id);
			byte[] aptamer = pool.getAptamer(batch_ids.get(b).aptamer_id);
			
			// And the Base Pair Probability Matrix
			double[] bppm = bppmPool.getStructure(batch_ids.get(b).aptamer_id);
			
			int h = -1; // width and height index
			int w = 0;

			// Fill the sequence portion with a one-hot representation of the aptamer
			for(int x=bounds.startIndex; x<bounds.endIndex; x++) {
				h++;
				w= this.upperOnly ? h-1 : -1;
				
				for(int y=this.upperOnly ? x : bounds.startIndex; y<bounds.endIndex; y++) {
					w++;					
					
					// Set the right position hot
					input.putScalar(new int[] {b, pairsToChannelIndex.get(aptamer[x]).get(aptamer[y]) , h, w}, 1.0);
					
					// Fill the Base Pair Probability Channel if appropriate
					if (x != y) {
						// since we store bppms as upper triangular matrixes, we need to switch indices to fill the lower portion of the channel
						if (x<y) input.putScalar(new int[] {b, channels-1 , h, w}, bppm[Index.triu(x, y, randomizedRegionSize)]);
						else     input.putScalar(new int[] {b, channels-1 , h, w}, bppm[Index.triu(y, x, randomizedRegionSize)]);
					}
				}
				
			}
			 
			cursor++;
			
//			System.out.println(String.format("%s %s", bounds.startIndex, bounds.endIndex));
//			System.out.println(new String(aptamer).substring(bounds.startIndex,bounds.endIndex));
//			System.out.println(input);
			
		}
		
		return new DataSet(input,labels);
	}

	@Override
	public int numExamples() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void reset() {
		if(type == DataType.TEST) {
			this.dataInputIterator.resetTestData();
		}
		else {
			this.dataInputIterator.resetTrainData();
		}
	}

	@Override
	public boolean resetSupported() {
		return true;
	}

	@Override
	public void setPreProcessor(DataSetPreProcessor arg0) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public int totalExamples() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int totalOutcomes() {
		return 1; //regression
	}

	public void setWithNoise(boolean withnoise) {
		
		this.withNoise = withnoise;
		
	}
	
	public void setNoisePercentage(double percentage) {
		this.noisePercentage = percentage;
	}
	
}
