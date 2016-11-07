/**
 * 
 */
package lib.parser.aptaplex;

/**
 * @author Jan Hoinka
 * Interface to abstract from different file formats including
 * fasta, fastq, and others.
 */
public interface FileFormat {

	
	/**
	 * Provided with an appropriate file handler, read and extract the
	 * next contig and, if applicable, quality scores. If the data is paired end
	 * both forward and reverse reads must be returned. 
	 * @return [read_forward, quality_forward, read_reverse, quality_reverse]
	 */
	public Read getNextRead();
	
}
