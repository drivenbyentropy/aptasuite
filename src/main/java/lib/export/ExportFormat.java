/**
 * 
 */
package lib.export;

import lib.parser.aptaplex.Read;

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
	
	/**
	 * This function should return a valid formated 
	 * representation of and aptamer read acccording to
	 * AptaSuites internal <code>Read</code> structure
	 * @param the Read instance to extract the data from
	 * @return a string formatted in the correct way
	 */
	String format(Read r, SequencingDirection d);
}
