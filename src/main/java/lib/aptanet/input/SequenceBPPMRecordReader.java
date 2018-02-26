/**
 * 
 */
package lib.aptanet.input;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.datavec.api.conf.Configuration;
import org.datavec.api.records.Record;
import org.datavec.api.records.listener.RecordListener;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataURI;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.InputSplit;
import org.datavec.api.util.files.URIUtil;
import org.datavec.api.util.ndarray.RecordConverter;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.BaseImageRecordReader;
import org.nd4j.linalg.api.concurrency.AffinityManager;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import lib.aptamer.datastructures.Experiment;

/**
 * @author Jan Hoinka
 *
 */
public class SequenceBPPMRecordReader implements RecordReader {

	/**
	 * Bitset where 1 at a particular position means that an aptamer with the id at that
	 * index is to be considered for processing 
	 */
	private BitSet split;
	
	/**
	 * Number of processed items since initialiation
	 */
	private int processed = 0;
	
	/**
	 * Total number of items in the split
	 */
	private int total_items; 
	
	/**
	 * The iterator over the aptamer pool
	 */
	private Iterator<Entry<Integer, byte[]>> sequence_iter; 
	
	/**
	 * The iterator over the aptamer bounds
	 */
	private Iterator<Entry<Integer, int[]>> bounds_iter;
	
	/**
	 * The iterator over the aptamer pool
	 */
	private Iterator<Entry<Integer, double[]>> structure_iter; 
	
	/**
	 * List of labels 
	 */
	private List<String> labels = new ArrayList<>();
	
	/**
	 * Label generator
	 */
	private AptamerLabelGenerator labelGenerator;
	
	private boolean with_primers;
	
	/**
	 * Whether to use labled data or not
	 */
	private boolean appendLabel = false;
	
	/**
	 * The Aptamer id currently being processed
	 */
	private int current_id;

	private Experiment experiment = utilities.Configuration.getExperiment();
	
	private InputEncoding encoding;
	
	protected Configuration conf;
	
	/**
	 * @param with_primers
	 * @param labelGenerator
	 */
	public SequenceBPPMRecordReader(boolean with_primers, AptamerLabelGenerator labelGenerator) {
		
        this.with_primers = with_primers;
        this.labelGenerator = labelGenerator;
        this.appendLabel = labelGenerator != null ? true : false;
		
	}
	
	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.conf.Configurable#getConf()
	 */
	@Override
	public Configuration getConf() {
		return conf;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.conf.Configurable#setConf(org.datavec.api.conf.Configuration)
	 */
	@Override
	public void setConf(Configuration arg0) {

		this.conf = arg0;
		
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#batchesSupported()
	 */
	@Override
	public boolean batchesSupported() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#getLabels()
	 */
	@Override
	public List<String> getLabels() {

		return labelGenerator.getLabels().stream().map( e -> e.toString() ).collect(Collectors.toList());
		
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#getListeners()
	 */
	@Override
	public List<RecordListener> getListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return processed < total_items;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#initialize(org.datavec.api.split.InputSplit)
	 */
	@Override
	public void initialize(InputSplit arg0) throws IOException, InterruptedException {

		throw new UnsupportedOperationException();
	
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#initialize(org.datavec.api.conf.Configuration, org.datavec.api.split.InputSplit)
	 */
	@Override
	public void initialize(Configuration arg0, InputSplit arg1) throws IOException, InterruptedException {

		throw new UnsupportedOperationException();

	}
	
	/**
	 * Called once at initialization. 
	 * @param split BitSet of length <code>AptamerPool.size()</code> where 1 at a particular index 
	 * means that the aptamer with id=index will be included. Note that this BitSet should represent
	 * an already filtered list of species. 
	 * @param encoding the encoding to be used to represent the aptamer data
	 */
	public void initialize(BitSet split, InputEncoding encoding) {
		
		sequence_iter = experiment.getAptamerPool().inverse_view_iterator().iterator();
		structure_iter = experiment.getBppmPool().iterator().iterator();
		bounds_iter = experiment.getAptamerPool().bounds_iterator().iterator();
		experiment.getBppmPool().iterator().iterator();

		this.split = split;
		this.encoding = encoding;
		this.processed = 0;
		this.total_items = split.cardinality();
		
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#loadFromMetaData(org.datavec.api.records.metadata.RecordMetaData)
	 */
	@Override
	public Record loadFromMetaData(RecordMetaData arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#loadFromMetaData(java.util.List)
	 */
	@Override
	public List<Record> loadFromMetaData(List<RecordMetaData> arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#next()
	 */
	@Override
	public List<Writable> next() {

		// The representation for a single item is
		// List<Writable> list =
		// [[[[[N,N,N],           Row 1, Channel 1
		// 	   [N,N,N],           Row 2, Channel 1
		//     [N,N,N]],          Row 3, Channel 1
		//
		//    [[N,N,N],           Row 1, Channel 2
		//	   [N,N,N],           Row 2, Channel 2
		//     [N,N,N]],          Row 3, Channel 2
		//
		//    [[N,N,N],           Row 1, Channel 3
		//	   [N,N,N],           Row 2, Channel 3
		//     [N,N,N]]]],   L]   Row 3, Channel 3, Label Index  (Scalar)
		// ^^^^^^^^^^^^^^    ^
		// 1st list element, 2nd list element
		
		List<Writable> ret = null;
		
		if (this.hasNext()) {
			
			Entry<Integer, byte[]> item = this.sequence_iter.next();
			current_id = item.getKey();
			byte[] sequence = item.getValue();
			double[] structure = this.structure_iter.next().getValue();
			int[] bounds = this.bounds_iter.next().getValue();
			
			
			
			//TODO: INVOKELISTENERS
			INDArray row;
			if (this.with_primers) {
				
				row = TensorFactory.getTensor(sequence, structure, 0, sequence.length, encoding);
						
			} else {
				
				row = TensorFactory.getTensor(sequence, structure, bounds[0], bounds[1], encoding);
				
			}
			
			ret = RecordConverter.toRecord(row);
			
			// Add a label if required
			if (appendLabel) {
				ret.add( this.labelGenerator.getLabelForAptamer(current_id) );
			}
			
			
		} else {
			throw new IllegalStateException("No more elements");
		}
		//TODO: WHAT IS RECORD ELSE IF?
		
		// Update
		this.processed++;
		
		return ret;
		
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#next(int)
	 */
	@Override
	public List<Writable> next(int num) {
		
		// The representation for a batch is
		// [[[[[N,N,N],         Row 1, Channel 1, Item 1
		// 	   [N,N,N],         Row 2, Channel 1, Item 1
		//     [N,N,N]],        Row 3, Channel 1, Item 1
		//
		//    [[N,N,N],         Row 1, Channel 2, Item 1
		//	   [N,N,N],         Row 2, Channel 2, Item 1
		//     [N,N,N]],        Row 3, Channel 2, Item 1
		//
		//    [[N,N,N],         Row 1, Channel 3, Item 1
		//	   [N,N,N],         Row 2, Channel 3, Item 1
		//     [N,N,N]]]],      Row 3, Channel 3, Item 1
		//
		//		
		//  [[[[N,N,N],         Row 1, Channel 1, Item 2
		// 	   [N,N,N],         Row 2, Channel 1, Item 2
		//     [N,N,N]],        Row 3, Channel 1, Item 2
		//
		//    [[N,N,N],         Row 1, Channel 2, Item 2
		//	   [N,N,N],         Row 2, Channel 2, Item 2
		//     [N,N,N]],        Row 3, Channel 2, Item 2
		//
		//    [[N,N,N],         Row 1, Channel 3, Item 2
		//	   [N,N,N],         Row 2, Channel 3, Item 2
		//     [N,N,N]]]],      Row 3, Channel 3, Item 2
		//
		//
		//	[[[[N,N,N],         Row 1, Channel 1, Item 3
		// 	   [N,N,N],         Row 2, Channel 1, Item 3
		//     [N,N,N]],        Row 3, Channel 1, Item 3
		//
		//    [[N,N,N],         Row 1, Channel 2, Item 3
		//	   [N,N,N],         Row 2, Channel 2, Item 3
		//     [N,N,N]],        Row 3, Channel 2, Item 3
		//
		//    [[N,N,N],         Row 1, Channel 3, Item 3
		//	   [N,N,N],         Row 2, Channel 3, Item 3
		//     [N,N,N]]]],      Row 3, Channel 3, Item 3
		//
		//    [[L,L],[L,L],[L,L]]] One hot representation of the label for each item (in this case there are 2 classes)
		
		
		// We need to store the data for the batch size
		List<INDArray> tensors = new ArrayList<>(); 
		List<Integer> label_indices = new ArrayList<>();
		
		int cnt = 0;
		while (cnt < num && hasNext() ) {
			
			Entry<Integer, byte[]> item = this.sequence_iter.next();
			current_id = item.getKey();
			byte[] sequence = item.getValue();
			double[] structure = this.structure_iter.next().getValue();
			int[] bounds = this.bounds_iter.next().getValue();
			
			if (this.with_primers) {
				
				tensors.add( TensorFactory.getTensor(sequence, structure, 0, sequence.length, encoding) );
						
			} else {
				
				tensors.add( TensorFactory.getTensor(sequence, structure, bounds[0], bounds[1], encoding) );
				
			}
			
			label_indices.add(labelGenerator.getHotIndexForLabel(current_id));
			
			cnt++;
			
		}
		
		
		// Now we add everything into one large tensor
		int[] shape = tensors.get(0).shape(); // get the shape from the individual tensors...
		shape[0] = tensors.size(); // ...set first dimension to the number of samples we have...
		INDArray features = Nd4j.createUninitialized(shape, 'c'); // ...and create a tensor with these specifications
	
		// Assign the individual tensors
		for (int x = 0; x<tensors.size(); x++) {
			
			features.putRow(x, tensors.get(x));
			
		}
		
		// Convert to output
		List<Writable> ret = (RecordConverter.toRecord(features));

		// Take care of labels if required
        if (appendLabel) {
        	
            INDArray labels = Nd4j.create(cnt, labelGenerator.getNumberOfLabels(), 'c');

            for (int i = 0; i < labelGenerator.getNumberOfLabels(); i++) {
                labels.putScalar(i, label_indices.get(i), 1.0f);
            }
            ret.add(new NDArrayWritable(labels));
            
        }

		// Update
		this.processed += tensors.size();
        
        return ret;

	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#nextRecord()
	 */
	@Override
	public Record nextRecord() {
		
		List<Writable> list = next();
        return new org.datavec.api.records.impl.Record(list, new AptamerRecordMetaData(""+ current_id, null, this.getClass()));
	
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#record(java.net.URI, java.io.DataInputStream)
	 */
	@Override
	public List<Writable> record(URI arg0, DataInputStream arg1) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#reset()
	 */
	@Override
	public void reset() {

		sequence_iter = experiment.getAptamerPool().inverse_view_iterator().iterator();
		structure_iter = experiment.getBppmPool().iterator().iterator();
		bounds_iter = experiment.getAptamerPool().bounds_iterator().iterator();
		
		this.processed = 0;
		this.total_items = split.cardinality();

	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#setListeners(org.datavec.api.records.listener.RecordListener[])
	 */
	@Override
	public void setListeners(RecordListener... arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.reader.RecordReader#setListeners(java.util.Collection)
	 */
	@Override
	public void setListeners(Collection<RecordListener> arg0) {
		// TODO Auto-generated method stub

	}

}
