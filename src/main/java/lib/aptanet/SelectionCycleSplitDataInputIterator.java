/**
 * 
 */
package lib.aptanet;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;

import exceptions.InvalidConfigurationException;
import exceptions.InvalidSelectionCycleException;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka 
 * 
 * Provided with a selection cycle, a score method, and
 * cutoff bounds for the score, partition the data into train and test
 * sets. The resulting iterators guarantee that any element retrieved 
 * with the next methods are valid data.
 */
public class SelectionCycleSplitDataInputIterator implements DataInputIterator {

	/**
	 * The selection cycle to get the data form
	 */
	private SelectionCycle selectionCycle;
	
	/**
	 * The selection cylce to be used for computing the enrichment if selected in 
	 * <code>scoreMethod</code>
	 */
	private SelectionCycle referenceSelectionCycle = null;

	/**
	 * A cutoff which decides weather to include a data item in an iterator or not
	 */
	private double cutoff;

	/**
	 * Lamda expression according to the choice of inequality
	 */
	
	private BiFunction<Double, Double, Boolean> inequalityFunction;

	/**
	 * Number of total training items including the rejected ones
	 */
	private int dataSizeTrain;

	/**
	 * Number of total testing items including the rejected ones
	 */
	private int dataSizeTest;

	/**
	 * Reference to the score function to be used according to <code>scoreMethod</code>
	 */
	private ToDoubleFunction<Entry<Integer, Integer>> scoreFunction;
	
	/**
	 * Entropy!!!
	 */
	private Random rand = new Random();

	/**
	 * Iterator over the data contained in <code> selectionCycle </code>
	 */
	private Iterator<Entry<Integer, Integer>> dataIteratorTest;
	private Iterator<Entry<Integer, Integer>> dataIteratorTrain;

	/**
	 * Number of used test and train data items
	 */
	private int iteratedTestDataSize = 0;
	private int iteratedTrainDataSize = 0;
	
	/**
	 * Keeps track of the position of the iterator in the data
	 */
	private int currentTestIndex;
	private int currentTrainIndex;
	
	/**
	 * Keeps track of the position of the iterator in the data
	 */
	private int initialTestIndex;
	private int initialTrainIndex;
	

	/**
	 * Stores the information whether a data item belongs to the training or testing set
	 * 1 = Training
	 * 0 = Testing
	 */
	BitSet dataMembership = null;

	/**
	 * Defines which items can be used for training data and which not. It servers as an 
	 * additional filter, e.g to select/exclude specific data in order to balance positive and
	 * negative examples.
	 * 
	 * Array has the same dimension as dataMembership. If null, the full training data will be used.
	 * 1 = Include
	 * 0 = Exclude
	 */
	BitSet dataMask = null;
	
	
	/**
	 * Will always have the next element to return, or null if no other element is available
	 */
	private AptamerLabelPair cache_train;
	private AptamerLabelPair cache_test;	
	
	/**
	 * The number of unique aptamers in <code> selectionCycle </code>
	 */
	private int dataSize;
	
	/**
	 * Since we need to guarantee that the order in which we process is random, we
	 * will iterate over the data using a stepSize which is prime and larger than
	 * <code>dataSize</code>. We can guarantee to touch each item only once if we
	 * iterate exactly <code>dataSize</code> times over the elements.
	 */
	private int stepSize;

	/**
	 * We need to ignore the primers during the learning process as they are static
	 */
	int randomizedRegionSize;
	
	/**
	 * The aptamer sequences of the experiment
	 */
	AptamerPool pool = Configuration.getExperiment().getAptamerPool();

	double maxCount = 0;
	
	/**
	 * @param sc
	 *            the SelectionCycle to take the data from
	 * @param cutoff
	 *            a cutoff value deciding if an aptamer in <code>sc</code> should be
	 *            considered as training or testing item
	 * @param score_method
	 * @param inequality
	 *            if sorting the data by the scoring method, defines if the cutoff
	 *            refers to the right or left side of a cut
	 * @param split
	 *            percentage of the data to be used as training data. the rest
	 *            (1-spilt) will be used as test set. Must be within [0-1]
	 * @param mask
	 * 			  defines which training data will be used and which not. Must be of same size
	 * 		      as sc.getUniqueSize();
	 */
	public SelectionCycleSplitDataInputIterator(SelectionCycle sc, ScoreMethod score_method, double cutoff,
			Inequality inequality, double split) {
		
		this(sc, score_method, cutoff, inequality, split, null);
		
	}
	

	/**
	 * @param sc
	 *            the SelectionCycle to take the data from
	 * @param cutoff
	 *            a cutoff value deciding if an aptamer in <code>sc</code> should be
	 *            considered as training or testing item
	 * @param score_method
	 * @param inequality
	 *            if sorting the data by the scoring method, defines if the cutoff
	 *            refers to the right or left side of a cut
	 * @param split
	 *            percentage of the data to be used as training data. the rest
	 *            (1-spilt) will be used as test set. Must be within [0-1]
	 * @param mask
	 * 			  defines which training data will be used and which not. Must be of same size
	 * 		      as sc.getUniqueSize();
	 */
	public SelectionCycleSplitDataInputIterator(SelectionCycle sc, ScoreMethod score_method, double cutoff,
			Inequality inequality, double split, BitSet mask) {
		
		// TODO: Add some kind of size check here. Note that BitSet.size() does not return what one might think
		this.dataMask = mask;
		
		this.selectionCycle = sc;
		this.cutoff = cutoff;
		if (split < 0 || split > 1) {
			throw new InvalidConfigurationException("Split must be between 0 and 1.");
		}

		// We also need to know the total number of items to iterate
		this.dataSize = this.selectionCycle.getUniqueSize();

		// Compute how many elements belong to the training and testing partitions
		// accordig to the split
		this.dataSizeTrain = (int) (this.dataSize * split);
		this.dataSizeTest = this.dataSize - this.dataSizeTrain;
		
		// If we have a datamask we need to adjust the training size
		if(this.dataMask != null) {
			
			this.dataSizeTrain = this.dataMask.cardinality();
			
		}

		// Create the bitset, with all 1 initially and then randomly set dataSizeTrain bits to 0.
		this.dataMembership = new BitSet(this.dataSize);
		this.dataMembership.flip(0, this.dataMembership.size()-1);

		int flip_counter = 0;
		while( flip_counter < this.dataSizeTest) {
			
			// Choose a random position
			int pos = rand.nextInt(this.dataSize);
			
			// Make sure we have not previously flipped this position
			if (this.dataMembership.get(pos)) {
				this.dataMembership.flip(pos);
				flip_counter++;
			}
		}
		
		// Compute a prime number larger than dataSize,
		// add a bit of randomness here by choosing a multiple of dataSize
		BigInteger bi = BigInteger.valueOf(this.dataSize); // * rand.nextInt(dataSize));
		this.stepSize = bi.nextProbablePrime().intValue();

		// Set the initial position of the points for the current test and train data
		this.currentTestIndex = rand.nextInt(this.dataSize);
		while(this.dataMembership.get(this.currentTestIndex)) {
			this.currentTestIndex = rand.nextInt(this.dataSize);			
		}
		while(this.currentTrainIndex == this.currentTestIndex && !this.dataMembership.get(this.currentTrainIndex)) {
			this.currentTrainIndex = rand.nextInt(this.dataSize);
		}
		
		// Store the initial values in order to reset iterators when required
		initialTestIndex = currentTestIndex;
		initialTrainIndex = currentTrainIndex;
		
		// We need two iterators over the selection cycle, one for training and one for
		// testing. These are set to the position corresponding to current[Train/Test]Index
		this.dataIteratorTest = sc.iterator().iterator();
		this.dataIteratorTrain = sc.iterator().iterator();
		for(int counter=0; counter < this.currentTestIndex; this.dataIteratorTest.next(), counter++);
		for(int counter=0; counter < this.currentTrainIndex; this.dataIteratorTrain.next(), counter++);
		
		// Set score function according to the score method. This way we only have to switch once
		// instead of each time a next element if requested
		switch (score_method) {

		case COUNT:
			this.scoreFunction = this::getCountScore; // Creates a reference to a function. Java 1.8 and above
			break;
		case FREQUENCY:
			this.scoreFunction = this::getFrequencyScore;
			break;
		case ENRICHMENT:
			if (this.referenceSelectionCycle == null) {
				throw new InvalidSelectionCycleException("No reference selection cycle has been assigned to the iterator. You need to call setReferenceSelectionCycle when using ENRICHMENT as ScoreMethod.");
			}
			this.scoreFunction = this::getEnrichmentScore;
			break;
		default:
			break;

		}
		
		// Set the inequality function depending on the choice of inequality
		switch (inequality) {
		
			case STRICTLYSMALLER:
				this.inequalityFunction = (score,cut) -> score < cut;
				break;
				
			case SMALLER:
				this.inequalityFunction = (score,cut) -> score <= cut;
				break;
				
			case EQUAL:
				this.inequalityFunction = (score,cut) -> score == cut;
				break;
				
			case GREATER:
				this.inequalityFunction = (score,cut) -> score >= cut;
				break;
				
			case STRICTLYGREATER:
				this.inequalityFunction = (score,cut) -> score > cut;
				break;
		default:
			break; 
		
		}

		// Set the desired randomized region size
		try {
			randomizedRegionSize = Configuration.getParameters().getInt("Experiment.randomizedRegionSize");
		} catch (NoSuchElementException e) {
			
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			AptaLogger.log(Level.SEVERE, this.getClass(), "No randomized region size was specified. Please check your configuration file.");
			System.exit(1);
		}
		
		
		// Get the largest element
		for ( Entry<Integer,Integer> item : sc.iterator()) {
			
			maxCount = Math.max(maxCount, item.getValue());
			
		}
		
		
		// Finally cache the first elements for training and testing
		this.cacheNextTestElement();
		this.cacheNextTrainElement();
		
	}


	/**
	 * Advances through the data in a pseudo-random manner. Automatically resets
	 * the iterator if required. No filter checks are performed here.
	 * 
	 * @return null if all elements have been returned exactly once
	 */
	private Entry<Integer, Integer> getNextTrainItem() {

		Entry<Integer, Integer> item = null;
		
		// Do we have more items to return?
		if (iteratedTrainDataSize !=  dataSizeTrain) {

			// Get the item to return
			item = dataIteratorTrain.next();
			
			// Compute next position, make sure this element represents a valid training data
			int new_iterator_position = (currentTrainIndex + stepSize) % dataSize;
			
			if (dataMask == null) { // Case no dataMask 
				while (!dataMembership.get(new_iterator_position)) {
					
					new_iterator_position = (new_iterator_position + stepSize) % dataSize;
					
				}
			}
			else { // Case with dataMask
				while (!dataMembership.get(new_iterator_position) && dataMask.get(new_iterator_position)) {
					
					new_iterator_position = (new_iterator_position + stepSize) % dataSize;
					
				}
			}
			
			if (currentTrainIndex < new_iterator_position) { // Do we have to circle around?

				for (int x = currentTrainIndex; x < new_iterator_position - 1; ++x, dataIteratorTrain.next());

			} else {

				dataIteratorTrain = selectionCycle.iterator().iterator();
				for (int x = 0; x < new_iterator_position; ++x, dataIteratorTrain.next());

			}

			// Update parameters
			iteratedTrainDataSize++;
			currentTrainIndex = new_iterator_position;
		}

		return item;
		
	}
	
	
	/**
	 * Advances through the data in a pseudo-random manner. Automatically resets
	 * the iterator if required. No filter checks are performed here.
	 * 
	 * @return null if all elements have been returned exactly once
	 */
	private Entry<Integer, Integer> getNextTestItem() {

		Entry<Integer, Integer> item = null;
		
		// Do we have more items to return?
		if (iteratedTestDataSize != dataSizeTest) {

			// Get the item to return
			item = dataIteratorTest.next();
			
			// Compute next position, make sure this element represents a training data
			int new_iterator_position = (currentTestIndex + stepSize) % dataSize;
			
			while (dataMembership.get(new_iterator_position)) {
				
				new_iterator_position = (new_iterator_position + stepSize) % dataSize;
				
			}
			
			if (currentTestIndex < new_iterator_position) { // Do we have to circle around?

				for (int x = currentTestIndex; x < new_iterator_position - 1; ++x, dataIteratorTest.next());

			} else {

				dataIteratorTest = selectionCycle.iterator().iterator();
				for (int x = 0; x < new_iterator_position; ++x, dataIteratorTest.next());

			}

			// Update parameters
			iteratedTestDataSize++;
			currentTestIndex = new_iterator_position;
		}

		return item;

	}	
	
	
	/**
	 * Returns the raw count of the aptamer in selection cycle
	 * 
	 * @param item key:aptemer id, value:count
	 * @return
	 */
	private double getCountScore(Entry<Integer, Integer> item) {

		return (double) item.getValue();

	}

	/**
	 * Returns the frequency of the aptamer in selection cycle
	 * 
	 * @param item key:aptemer id, value:count
	 * @return
	 */
	private double getFrequencyScore(Entry<Integer, Integer> item) {

		return item.getValue() / selectionCycle.getSize();

	}

	/**
	 * Returns the enrichment of the aptamer in selection cycle
	 * w.r.t. to the selection cycles specified in <code>referenceSelectionCycles</code>
	 * 
	 * @param item key:aptemer id, value:count
	 * @return
	 */
	private double getEnrichmentScore(Entry<Integer, Integer> item) {

		// we add a pseudocount of one to avoid division by 0
		double current_frequency = (item.getValue()+1.0) / (selectionCycle.getSize()+1.0);
		double prev_frequency = (referenceSelectionCycle.getAptamerCardinality(item.getKey())+1.0) / (referenceSelectionCycle.getSize()+1.0);

		return current_frequency/prev_frequency;
		
	}
	
	/**
	 * Preloads the next item, null if not available
	 */
	private void cacheNextTrainElement() {
		
		// make sure we are allowed to get another element
		if (iteratedTrainDataSize >= dataSizeTrain) {
			cache_train = null;
			return;
		}
		
		// get next element 
		int skipped_items = 1; // number of items skipped due to cutoff
		Entry<Integer, Integer> item = getNextTrainItem();
		
		// make sure we have a valid element and that we are allowed to get another element
		while(item != null && skipped_items+iteratedTrainDataSize < dataSizeTrain) {
		 
			// make sure the aptamer size is correct
			AptamerBounds bound = pool.getAptamerBounds(item.getKey());
			
			// are we compliant with the size?
			if(bound.endIndex - bound.startIndex == this.randomizedRegionSize) {
			
				// compute the score  
				double score = scoreFunction.applyAsDouble(item);
				
				// are we compliant with the threshold?
				if (inequalityFunction.apply(score,cutoff)) {
					
					cache_train = new AptamerLabelPair(item.getKey(), score);
					iteratedTrainDataSize += skipped_items;
					
					break;
				}	
			}
			
			// if not we need to continue searching
			skipped_items++;
			item = getNextTrainItem();
		}
		
	}
	
	/**
	 * Preloads the next item, null if not available
	 */
	private void cacheNextTestElement() {

		// make sure we are allowed to get another element
		if (iteratedTestDataSize >= dataSizeTest) {
			cache_test = null;
			return;
		}
		
		// get next element
		int skipped_items = 1; // number of items skipped due to cutoff
		Entry<Integer, Integer> item = getNextTestItem();

		// make sure we have a valid element and that we are allowed to get another
		// element
		while (item != null && skipped_items + iteratedTestDataSize < dataSizeTest) {

			// make sure the aptamer size is correct
			AptamerBounds bound = pool.getAptamerBounds(item.getKey());

			// are we compliant with the size?
			if (bound.endIndex - bound.startIndex == this.randomizedRegionSize) {

				// compute the score
				double score = scoreFunction.applyAsDouble(item);

				// are we compliant with the threshold?
				if (inequalityFunction.apply(score, cutoff)) {

					cache_test = new AptamerLabelPair(item.getKey(), score);
					iteratedTestDataSize += skipped_items;

					break;
				}
			}
			// if not we need to continue searching
			skipped_items++;
			item = getNextTestItem();
		}

	}
	
	@Override
	public boolean hasNextTrainData() {
		
		return cache_train != null;
		
	}

	@Override
	public boolean hasNextTestData() {
		
		return cache_test != null;
		
	}

	@Override
	public AptamerLabelPair nextTrainData() {

		// create the return value
		AptamerLabelPair pair = cache_train;
		//BEGIN TEMP REMOVE AFTER DEBUG
		//pair.score = pair.score / maxCount;
		pair.score = Math.log(pair.score);
		//BEGIN TEMP REMOVE AFTER DEBUG		

		// now precache the next element
		cache_train = null;
		cacheNextTrainElement();
		
		return pair;
	}

	@Override
	public AptamerLabelPair nextTestData() {
		// create the return value
		AptamerLabelPair pair = cache_test;
		//BEGIN TEMP REMOVE AFTER DEBUG
		//pair.score = pair.score / maxCount;
		pair.score = Math.log(pair.score);
		//BEGIN TEMP REMOVE AFTER DEBUG
		
		// now precache the next element
		cache_test = null;
		cacheNextTestElement();
		
		return pair;
	}

	public void setReferenceSelectionCycle(SelectionCycle rsc) {
		
		// make sure the reference selection cycle is smaller than the current one
		if( selectionCycle.getRound() >= rsc.getRound()) {
			throw new InvalidSelectionCycleException(String.format("The reference selection cycle must be of a smaller round than %s." , selectionCycle.getRound()));
		}
		
		referenceSelectionCycle = rsc;
		
	}

	@Override
	public void resetTestData() {
		
		// restore the initial values
		currentTestIndex  = initialTestIndex;
		
		iteratedTestDataSize = 0;
		
		this.dataIteratorTest = selectionCycle.iterator().iterator();
		for(int counter=0; counter < this.currentTestIndex; this.dataIteratorTest.next(), counter++);
		
		this.cacheNextTestElement();
		
	}

	@Override
	public void resetTrainData() {

		// restore the initial values
		currentTrainIndex = initialTrainIndex;
		
		iteratedTrainDataSize = 0;

		this.dataIteratorTrain = selectionCycle.iterator().iterator();
		for(int counter=0; counter < this.currentTrainIndex; this.dataIteratorTrain.next(), counter++);
		
		this.cacheNextTrainElement();
		
	}

	
}
