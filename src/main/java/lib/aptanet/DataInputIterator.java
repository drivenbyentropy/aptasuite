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
	boolean hasNextTrainData();
	
	/**
	 * Defines if the Iterator has another test item available
	 * @return
	 */
	boolean hasNextTestData();
	
	/**
	 * Return the next pair of aptamer and label from the training 
	 * data
	 * @return
	 */
	AptamerLabelPair nextTrainData();
	
	/**
	 * Return the next pair of aptamer and label from the testing
	 * data
	 * @return
	 */
	AptamerLabelPair nextTestData();
	
}
