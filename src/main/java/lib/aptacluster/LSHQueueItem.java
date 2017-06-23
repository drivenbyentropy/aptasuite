package lib.aptacluster;

import org.eclipse.collections.api.iterator.MutableIntIterator;

/**
 * @author Jan Hoinka
 * Wrapper class to pass all required elements to the consumers
 * for parallel processing.
 */
public class LSHQueueItem {

	/**
	 * The aptamer id of the seed sequence
	 */
	public int aptamer_id;
	
	/**
	 * the sequence of the seed id
	 */
	public byte[] aptamer_sequence;
	
	/**
	 * Iterator over the bucket the seed is contained in
	 */
	public MutableIntIterator it;
	
	/**
	 * The cluster id assigned to this seed
	 */
	public int cluster_id;
}
