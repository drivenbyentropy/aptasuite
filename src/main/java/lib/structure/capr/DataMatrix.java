/**
 * 
 */
package lib.structure.capr;

import java.util.ArrayList;

/**
 * @author Jan Hoinka
 * 
 * This class implements a two dimensional bit array based on a one dimensional bit array.
 * The indices are transparently translated from 2D to 1D. This allows for space efficient
 * storage of binary data.
 */
public class DataMatrix<T>{

	/**
	 * The BitSet storing the matrix data.
	 */
	private ArrayList<T> data = null;
	
	
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
	public DataMatrix(int rows, int cols){
		
		if (rows <= 0 || cols <= 0){
			throw new IndexOutOfBoundsException("The number of rows (" + rows + ") or columns (" + cols + ") is invalid");
		}
		
		data = new ArrayList<T>(rows*cols);
		this.rows = rows;
		this.cols = cols;
		
	}
	
	public DataMatrix(){
		
		data = new ArrayList<T>();
		this.rows = 0;
		this.cols = 0;
		
	}
	
	
	/**
	 * Sets the cell corresponding  to (<code>row</code>,<code>col</code>) of the matrix to <code>value</code>.
	 * @throws IndexOutOfBoundsException If row or column exceeds the matrix dimension.
	 * @param row
	 * @param col
	 * @param value
	 */
	public void set(int row, int col, T value){

		data.set(cols * row + col , value);
		
	}
	
	/**
	 * Gets the values of the cell corresponding  to (<code>row</code>,<code>col</code>).
	 * @throws IndexOutOfBoundsException If row or column exceeds the matrix dimension.
	 * @param row
	 * @param col
	 * @param value
	 */
	public T get(int row, int col){
		
		return data.get(cols * row + col);
		
	}
	
	
	/**
	 * Resets all values of the matrix to its initial state.
	 */
	public void clear(T default_value){
		
		for (int x = 0; x<rows*cols; x++){
			data.set(x, default_value);
		}
	
	}

	/**
	 * Changes the dimension of the matrix and allocates more space if required
	 * @param rows
	 * @param cols
	 */
	public DataMatrix<T> reshape(int rows, int cols){
		
		// resize data if required
		int target_size = rows*cols;
		
		if ( data.size() < (rows*cols) ){
			while (data.size() < target_size) {
				
				data.add(null);
				
			}
		}
	
		this.rows = rows;
		this.cols = cols;
		
		return this;
	}
	
}
