/**
 * 
 */
package lib.aptanet;

/**
 * @author Jan Hoinka
 * Interface for providing input data (training and testing)
 * to the DatasetIterator implementation in AptaNET. Each 
 * implementing class can then choose which input data to use
 * based on its own criteria.
 *
 */
public interface DataInputIterator {

	/**
	 * Defines if the Iterator has another training item available
	 * @return 
	 */
	public boolean hasNextTrainData();
	
	/**
	 * Defines if the Iterator has another test item available
	 * @return
	 */
	public boolean hasNextTestData();
	
	/**
	 * Return the next pair of aptamer and label from the training 
	 * data
	 * @return
	 */
	public AptamerLabelPair nextTrainData();
	
	/**
	 * Return the next pair of aptamer and label from the testing
	 * data
	 * @return
	 */
	public AptamerLabelPair nextTestData();
	
	/**
	 * Resets the iteration of the Test data such that the first call to 
	 * nextTestData() returns the same element as when the implementing
	 * class had first been instantiated. 
	 */
	public void resetTestData();
	
	
	/**
	 * Resets the iteration of the Train data such that the first call to 
	 * nextTestData() returns the same element as when the implementing
	 * class had first been instantiated. 
	 */
	public void resetTrainData();
	
}
