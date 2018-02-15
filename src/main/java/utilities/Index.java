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
	
}
