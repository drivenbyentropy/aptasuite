/**
 * 
 */
package lib.aptamer.datastructures;

/**
 * @author Jan Hoinka
 * Interface defining the contract for a generic storage
 * of arbitrary datatypes in a key,value fashion
 */
public interface GenericStorage<T,U> {

	/**
	 * Return the value associated with key <code>key<code>
	 * @param key
	 * @return value or null if value does not exist
	 */
	U get( T key );
	
	/**
	 * Stores value <code>value<code> at key <code>key<code> in the
	 * associated data structure. If a key already exists, its value will 
	 * be overwritten with the new value 
	 * @param key 
	 * @param value
	 * @return the previous value associated with key, or null if there was no mapping for key.
	 */
	U put( T key, U value);
	
	/**
	 * Deletes the key <code>key<code> from the storage.
	 * @param key
	 * @return value associated with <code>key<code> 
	 * or null if <code>key<code> does not exist.
	 */
	U remove(T key);
	
	/**
	 * Membership query for key <code>key<code>
	 * @param key
	 * @return true if <code>key<code> is contained in the strorage
	 * false otherwise
	 */
	Boolean containsKey(T key);
	
	/**
	 * Performs any actions required to gracefully
	 * close the storage instance. I.e. this function 
	 * should close any file handles etc.
	 */
	void close();
	
}
