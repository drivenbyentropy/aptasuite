/**
 * 
 */
package utilities;

/**
 * @author Jan Hoinka
 * Custom Comparator implementation for primitive doubles
 */
public interface QSDoubleComparator {

	/**
	 * 
	 * @return  -1, 0 or 1 to say if it is less than, equal, or greater to the other.
	 */
	public int compare(double a, double b);
	
}
