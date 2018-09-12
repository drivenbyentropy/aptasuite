/**
 * 
 */
package lib.parser.aptaplex;

import java.nio.file.Path;

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
	 *  Placeholder for format dependent data which might need to be carried over to other places
	 *  Eg. read name etc
	 */
	public byte[] metadata_forward = null;

	/**
	 *  Placeholder for format dependent data which might need to be carried over to other places
	 *  Eg. read name etc
	 */
	public byte[] metadata_reverse = null;
	
	/**
	 * The forward source file of this read
	 */
	public Path source_forward = null;
	
	/**
	 * The reverse source file of this read
	 */
	public Path source_reverse = null;
	
	
	
	/**
	 * Used for isPerFile cases. i.e. if demultiplexing has already been
	 * performed. We need to preassign the selection cycle
	 */
	public SelectionCycle selection_cycle = null;
	
	@Override
	public String toString() {
		
		return String.format("Forward Read:    %s\nForward Quality: %s\nReverse Read:    %s\nReverse Quality: %s",
				this.forward_read == null ? "null" : new String(this.forward_read), 
				this.forward_quality == null ? "null" : new String(this.forward_quality), 
				this.reverse_read == null ? "null" : new String(this.reverse_read), 
				this.reverse_quality == null ? "null" : new String(this.reverse_quality) );
		
	}
	
}
