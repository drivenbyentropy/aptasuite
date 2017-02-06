/**
 * 
 */
package lib.parser;

/**
 * @author Jan Hoinka
 * This interface provides a unified way of querying the progress
 * of the different implementing classes regarding the parsing
 * status in a multi-threaded environment.
 */
public interface ParserProgress {

	/**
	 * Generates a static header line that corresponds to the 
	 * dynamic data obtained via <code>getProgress()</code>.  
	 * @return
	 */
	public String getHeader(); 
	
	/**
	 * Generates a line containing the progress of the parser
	 * and any additional information that should be made 
	 * available to the user. The implementing class should
	 * generate the return value on-the-fly each time this
	 * function is called. 
	 * @return
	 */
	public String getProgress();
	
}
