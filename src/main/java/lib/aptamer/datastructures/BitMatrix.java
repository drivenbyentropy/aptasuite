/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.Serializable;
import java.util.BitSet;

/**
 * @author Jan Hoinka
 * 
 * This class implements a two dimensional bit array based on a one dimensional bit array.
 * The indices are transparently translated from 2D to 1D. This allows for space efficient
 * storage of binary data.
 */
public class BitMatrix implements Serializable{
	
	/**
	 * UID for Serializable interface
	 */
	private static final long serialVersionUID = 8760015085362970029L;


	/**
	 * The BitSet storing the matrix data.
	 */
	private BitSet data = null;
	
	
	/**
	 * The number of rows of the matrix
	 */
	private int rows = 0;
	
	
	/**
	 * The number of columns of the matrix
	 */
	private int cols = 0;
	
	
	/**
	 * Initialize the BitMatrix in a "row major" manner. All values are initially set to false.
	 * @param rows number of rows
	 * @param cols number of columns
	 */
	public BitMatrix(int rows, int cols){
		
		if (rows <= 0 || cols <= 0){
			throw new IndexOutOfBoundsException("The number of rows (" + rows + ") or columns (" + cols + ") is invalid");
		}
		
		data = new BitSet(rows*cols);
		this.rows = rows;
		this.cols = cols;
		
	}
	
	
	/**
	 * Sets the cell corresponding  to (<code>row</code>,<code>col</code>) of the matrix to <code>value</code>.
	 * @throws IndexOutOfBoundsException If row or column exceeds the matrix dimension.
	 * @param row
	 * @param col
	 * @param value
	 */
	public void set(int row, int col, boolean value){
		
		// Boundary check
		if ( (row < 0 && row >= rows) || (col < 0 && col >= cols) ){
			
			throw new IndexOutOfBoundsException("The matrix of size (" + rows + "," + cols + ") cannot be accessed at (" + row + "," + col + ")");
			
		}
		
		data.set(cols * row + col , value);
		
	}
	
	/**
	 * Gets the values of the cell corresponding  to (<code>row</code>,<code>col</code>).
	 * @throws IndexOutOfBoundsException If row or column exceeds the matrix dimension.
	 * @param row
	 * @param col
	 * @param value
	 */
	public boolean get(int row, int col){
		
		// Boundary check
		if ( (row < 0 && row >= rows) || (col < 0 && col >= cols) ){
			
			throw new IndexOutOfBoundsException("The matrix of size (" + rows + "," + cols + ") cannot be accessed at (" + row + "," + col + ")");
			
		}
		
		return data.get(cols * row + col);
		
	}
	
	
	/**
	 * Resets all values of the matrix to its initial state (false).
	 */
	public void clear(){
		
		data.clear();
	
	}


	/**
	 * Returns the number of rows
	 * @return
	 */
	public int getRows() {
		return rows;
	}


	/**
	 * Returns the number of columns
	 * @return
	 */
	public int getCols() {
		return cols;
	}
	
}
