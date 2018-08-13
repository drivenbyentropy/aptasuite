/**
 * 
 */
package utilities;

/**
 * @author Jan Hoinka
 * Implement various routines for translating 
 * mappings of 2d matrices into 1d arrays and back.
 * 
 * Note that only indices above the diagonal will return meaningful values;
 * 
 * This class only computes the indices to be accessed,
 * and does not return any values of the matrix itself.
 */
public class Index {

	/**
	 * Calculates the position of an element in an upper triangular matrix
	 * without the diagonal when stored in a linear fashion from left to right
	 * up to down. Assumes underlying matrix to be square.
	 * @param row
	 * @param col
	 * @param size dimension of the matrix
	 * @return
	 */
	public static int triu(int row, int col, int size){
		
		return (size*(size-1)/2) - (size-row)*((size-row)-1)/2 + col - row - 1;
		
	}
	
	/**
	 * Returns the row index of an upper triangular matrix without the 
	 * diagonal when stored in a linear fashion from left to right
	 * up to down. Assumes underlying matrix to be square. 
	 * @param pos position in the flat representation of the matrix
	 * @param size dimension of the matrix
	 * @return
	 */
	public static int triu_row(int pos, int size) {
		
		return size - 2 - (int) (Math.floor(Math.sqrt(-8*pos + 4*size*(size-1)-7)/2.0 - 0.5));
		
	}
	
	/**
	 * Returns the column index of an upper triangular matrix without the 
	 * diagonal when stored in a linear fashion from left to right
	 * up to down. Assumes underlying matrix to be square. 
	 * @param pos position in the flat representation of the matrix
	 * @param size dimension of the matrix
	 * @return
	 */
	public static int triu_col(int pos, int size) {
		
		int i = triu_row(pos, size);
		
		return pos + i + 1 - size*(size-1)/2 + (size-i)*((size-i)-1)/2;
		
	}	
	
	/**
	 * Returns the index of the first occurrence of <code>element</code> in the array <code>arr</code> 
	 * @param arr the array to be searched
	 * @param element the element to be searched for
	 * @param from the start position (inclusive) to search from 
	 * @return the index of the first occurrence of the element or -1 if not found 
	 */
	public static int indexOf(int[] arr, int element, int from) {
		
		int pos = -1;
		for (int x=from; x<arr.length; x++) {
			
			if (arr[x] == element) {
				
				pos = x;
				break;
				
			}
			
		}
		
		return pos;
		
	}
	
	/**
	 * Returns the index of the first occurrence of <code>element</code> in the array <code>arr</code> 
	 * @param arr the array to be searched
	 * @param element the element to be searched for
	 * @return the index of the first occurrence of the element or -1 if not found 
	 */
	public static int indexOf(int[] arr, int element) {
		
		return indexOf(arr, element, 0);
		
	}
	
	
}
