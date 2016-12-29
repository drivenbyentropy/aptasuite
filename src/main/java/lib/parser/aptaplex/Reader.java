/**
 * 
 */
package lib.parser.aptaplex;

/**
 * @author Jan Hoinka
 * Interface defining the contract for the different file readers
 * depending on the sequencing format.
 * 
 * The constructor of any implementing class must have 2 parameters of 
 * Path forward_read_file, and Path reverse_read_file
 */
public interface Reader {

	/**
	 * Returns the next read object according to the file order
	 * @return the next read. If no more reads are present, return null.
	 */
	public Read getNextRead();
	
	
	/**
	 * Perform any cleanup if necessary. This function is expected to be called
	 * by the Producer once all reads have been put into the queue.
	 */
	public void close();
	
}
