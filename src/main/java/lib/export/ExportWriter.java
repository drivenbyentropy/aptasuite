/**
 * 
 */
package lib.export;

import java.nio.file.Path;

/**
 * @author Jan Hoinka
 * Generalized interface for writing data to files
 */
public interface ExportWriter {

	/**
	 * Creates the file on disk and prepares the 
	 * implementing class such that <code>write()</code>
	 * becomes ready
	 * @param p
	 */
	void open(Path p);
	
	/**
	 * Writes the data to disk by appending
	 * it to the file as specified in <code>p</code>
	 * @param data the information to write
	 */
	void write(String data);
	
	/**
	 * Closes any file handles the implementing 
	 * class has
	 */
	void close();
}
