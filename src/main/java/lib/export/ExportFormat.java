/**
 * 
 */
package lib.export;

/**
 * @author Jan Hoinka
 * API for defining an export format of the sequence data
 * utilized by AptaSUITE for exporting purposes.
 */
public interface ExportFormat<T> {

	/**
	 * This function should return a valid formated 
	 * representation of and aptamer sequence and its
	 * unique id according to the implementing class'
	 * specification 
	 * @param id a unique identifier assigned to the data
	 * @param data an array of the datatype corresponding to the data to be written. 
	 * In case of sequences this would be byte[] whereas in the case of 
	 * structure it would be double[] 
	 * @return
	 */
	String format(int id, T data);
	
}
