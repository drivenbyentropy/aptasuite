/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.Serializable;
import java.util.Map.Entry;

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
public interface AptamerPool extends Serializable{


	/**
	 * Adds (i.e. registers) an aptamer in the pool and assigns a unique identifier to it.
	 * This identifier corresponds to an integer which can be used for indexing purposes for
	 * additional properties of the aptamer (e.g. to which selection cycles it belongs, counts, etc).
	 * 
	 * The identifier _must_ be continuous and start at 0 such that after parsing, all unique aptamers
	 * can be addressed from [0..poolsize-1]
	 * 
	 * If the aptamer already exists in the pool, it will not be inserted again, but 
	 * the existing identifier will be returned. 
	 * 
	 * It is assumed that the parent class performing the registration takes care of
	 * the alphabet validation, i.e. only A C G and T are allowed in all capital letters
	 * 
	 * @param a The aptamer sequence to be added to the pool
	 * @param rr_start the index (inclusive) in <code>a</code> at which the randomized region begins  
	 * @param rr_end the index (exclusive) in <code>a</code> at which the randomized region ends
	 * @return A unique integer for sequence <code>a</code>
	 */
	public int registerAptamer(byte[] a, int rr_start, int rr_end);
	
	
	/**
	 * @see AptamerPool#registerAptamer(byte[] a)
	 */
	public int registerAptamer(String a, int rr_start, int rr_end);
	
	
	/**
	 * Retrieves the unique identifier on an aptamer in the pool. 
	 * @param a The aptamer sequence 
	 * @return an integer >= 0. If the aptamer does not exist in the pool, -1 is returned.
	 */
	public int getIdentifier(byte[] a);
	

	/**
	 * @see AptamerPool#getIdentifier(byte[] a)
	 */
	public int getIdentifier(String a);

	/**
	 * Returns the aptamer (including flanking primer regions) corresponding to <code>id</>
	 * @param id the unique identifier of an aptamer in the pool
	 * @return aptamer sequence or null if no key with <code>id</code> could be found
	 */
	public byte[] getAptamer(int id);
	
	/**
	 * Returns the start index (inclusive) and end index (exclusive)
	 * of the randomized region for aptamer with unique identifier <code>id</code>.
	 * @param id
	 * @return bounds such that <code>Arrays.copyOfRange(getAptamer(id), getAptamerBounds(id).startIndex, getAptamerBounds(id).endIndex)</code>
	 * returns the randomized region of the aptamer
	 */
	public AptamerBounds getAptamerBounds(int id); 
	
	/**
	 * Checks for the existence of a specific aptamer <code>a</code> in the pool.
	 * @param a The aptamer sequence 
	 * @return
	 */
	public Boolean containsAptamer(byte[] a);
	
	/**
	 * @see AptamerPool#containsAptamer(byte[] a)
	 */	
	public Boolean containsAptamer(String a);	
	
	/**
	 * Checks for the existence of an aptamer with a particular id
	 * @param id the id to be checked for
	 * @return true if aptamer exists, false otherwise
	 */
	public Boolean containsAptamer(int id);
	
	/**
	 * Returns the total number of unique aptamers in the pool
	 * @return
	 */
	public int size();
	
	
	/**
	 * Removes all items from the current pool.
	 */
	public void clear();
	
	
	/**
	 * Optional. Closes any file handles the implementing class might have.
	 */
	public void close();
	
	
	/**
	 * Optional. Sets the underlying data structure of the implementing class to read only mode. 
	 */
	public void setReadOnly();
	
	/**
	 * Optional. Sets the underlying data structure of the implementing class to read/write mode. 
	 */
	public void setReadWrite();
	
		
	/**
	 * Provides an iterator over every aptamer in the pool  
	 * together with its unique id.
	 * Note that the order of iteration is implementation dependent
	 */
	public Iterable<Entry<byte[], Integer>> iterator();
	
	
	/**
	 * Provides an iterator over every aptamer in the pool  
	 * in inverse view, i.e. it provides every unique ID
	 * along with the corresponding aptamer.
	 * Note that the order of iteration is implementation dependent
	 */
	public Iterable<Entry<Integer,byte[]>> inverse_view_iterator();
	
	
	/**
	 * Provides an iterator over every id in the pool
	 */
	public Iterable<Integer> id_iterator();
	
	
	/**
	 * Provides public access to the iterator of the bounds data
	 */
	public Iterable<Entry<Integer,int[]>> bounds_iterator();
}
