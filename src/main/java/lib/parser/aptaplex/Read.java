/**
 * 
 */
package lib.parser.aptaplex;

import lib.aptamer.datastructures.SelectionCycle;

/**
 * @author Jan Hoinka
 * Format independent implementation of a sequencing read with support for paired end sequencing.
 * This class is used to optimize passing reads between classes and functions
 */
public class Read {

	/**
	 * Byte array representation of the forward read. 
	 * This is the minimal requirement for this class
	 */
	public byte[] forward_read = null;
	
	
	/**
	 * Byte array representation of the quality scores as
	 * they are present in formats such as fasta and fastq
	 */
	public byte[] forward_quality = null;
	
	
	/**
	 * Byte array representation of the reverse read. 
	 */
	public byte[] reverse_read = null;
	
	
	/**
	 * Byte array representation of the quality scores as
	 * they are present in formats such as fasta and fastq
	 */
	public byte[] reverse_quality = null;
	
	
	/**
	 * Used for isPerFile cases. i.e. if demultiplexing has already been
	 * performed. We need to preassign the selection cycle
	 */
	public SelectionCycle selection_cycle = null;
}
