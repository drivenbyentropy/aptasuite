/**
 * 
 */
package aptamer.pool;

import exceptions.InvalidAlphabetException;

/**
 * @author Jan Hoinka
 *
 * API for handling aptamer pools. An aptamer pool is defined
 * as every sequenced aptamer of an HT-SELEX experiment and its
 * respective rounds. 
 * 
 * This API provides efficient and transparent methods for aptamer 
 * storage, retrieval, and manipulation.
 */
public interface AptamerPool {


	/**
	 * Adds (i.e. registers) an aptamer in the pool and assigns a unique identifier to it.
	 * This identifier corresponds to an integer which can be used for indexing purposes for
	 * additional properties of the aptamer (e.g. to which selection cycles it belongs, counts, etc)
	 * 
	 * If the aptamer already exists in the pool, it will not be inserted again, but 
	 * the existing identifier will be returned. 
	 * 
	 * If the <code>a<code> contains any other characters than A C G or T, a <code>InvalidAlphabetException<code> 
	 * must be thrown. The final aptamer sequence must all be captial letters.
	 * 
	 * @param a The aptamer sequence to be added to the pool
	 * @return A unique integer for that sequence
	 */
	public int registerAptamer(String a) throws InvalidAlphabetException;	
	
	
	
	/**
	 * Retrieves the unique identifier on an aptamer in the pool. 
	 * @param a The aptamer sequence 
	 * @return an integer >= 0. If the aptamer does not exist in the pool, -1 is returned.
	 */
	public int getIdentifier(String a);
	
	
	/**
	 * Checks for the existence of a specific aptamer <code>a</code> in the pool.
	 * @param a The aptamer sequence 
	 * @return
	 */
	public Boolean hasAptamer(String a);
}
