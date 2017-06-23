/**
 * 
 */
package lib.parser;

/**
 * @author Jan Hoinka
 * This interface defines the type which all classes which implement must
 * comply with. The implementing classes typically allow for parsing different
 * sequencing formats such as fasta, fastq, but also AptaSIM-like approaches.
 */
public interface Parser {
	
	/**
	 * Implements the main logic behind the parser in question. 
	 * All parameters required parsers should be taken from the configuration
	 * file.
	 */
	public void parse();
	
	
	/**
	 * This function must be called once all reads have been processed.
	 * The function can be used as a callback in order to initialize e.g.
	 * indexing and/or update procedures for the underlying pool and 
	 * selection cycle instances.
	 */
	public void parsingCompleted();
	
	
	/**
	 * Produces an estimate of the progress of the parser implementation
	 */
	public ParserProgress Progress();
	
}
