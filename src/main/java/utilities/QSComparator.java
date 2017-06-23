/**
 * 
 */
package utilities;

/**
 * @author Jan Hoinka
 * Custom Comparator implementation for primitive integers
 */
public interface QSComparator {

	/**
	 * 
	 * @return  -1, 0 or 1 to say if it is less than, equal, or greater to the other.
	 */
	public int compare(int a, int b);
	
}
