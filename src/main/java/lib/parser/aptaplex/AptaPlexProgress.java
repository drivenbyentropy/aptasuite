/**
 * 
 */
package lib.parser.aptaplex;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jan Hoinka
 * Wrapper containing information regarding the progress of the parsing process.
 * Objects of this type should be accessible to all consumers and must guarantee 
 * thread-safe updating of members.
 */
public class AptaPlexProgress {

	/**
	 * The total number of currently processed reads. This member should
	 * be updated by the consumers
	 */
	public AtomicInteger totalProcessedReads = new AtomicInteger(0);
	
	
	/**
	 * The total number of accepted reads. This member should
	 * be updated by the consumers
	 */
	public AtomicInteger totalAcceptedReads = new AtomicInteger(0);
	
	
	/**
	 * The total number of times, we were not able to assemble the contig.
	 */
	public AtomicInteger totalContigAssemblyFails = new AtomicInteger(0);
	
	/**
	 * The total number of contigs with invalid alphabet, i.e. anything
	 * other than A C G or T
	 */
	public AtomicInteger totalInvalidContigs = new AtomicInteger(0);
	
	/**
	 * The total number of cases in which the 5' primer could not be matched
	 */
	public AtomicInteger totalUnmatchablePrimer5 = new AtomicInteger(0);
	
	/**
	 * The total number of cases in which the 3' primer could not be matched
	 */
	public AtomicInteger totalUnmatchablePrimer3 = new AtomicInteger(0);
	
	/**
	 * The total number of cases in which the 5' primer and the 3' primer 
	 * overlap
	 */
	public AtomicInteger totalPrimerOverlaps = new AtomicInteger(0);
	
	/**
	 * The total number of cases in which we were not able to determine a
	 * selection cycle
	 */
	public AtomicInteger totalInvalidCycle = new AtomicInteger(0);
	
	
}
