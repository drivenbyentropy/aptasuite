/**
 * 
 */
package lib.parser.aptaplex;

import java.util.concurrent.atomic.AtomicInteger;

import lib.parser.ParserProgress;

/**
 * @author Jan Hoinka
 * Wrapper containing information regarding the progress of the parsing process.
 * Objects of this type should be accessible to all consumers and must guarantee 
 * thread-safe updating of members.
 */
public class AptaPlexProgress implements ParserProgress {

	/**
	 * Spacing for formating purposes
	 */
	private String spacing = "%1$-23s %2$-23s %3$-23s %4$-23s %5$-23s %6$-23s %7$-23s %8$-23s";
	
	/**
	 * Static header for the parser progress
	 */
	private String header = String.format(spacing, "Total Reads:", "Accepted Reads:", "Contig Assembly Fails:", "Invalid Alphabet:",
			"5' Primer Error:", "3' Primer Error:", "Invalid Cycle:", "Total Primer Overlaps:");
	
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

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public String getProgress() {
		return String.format(spacing + "\r", totalProcessedReads.get(),
				totalAcceptedReads.get(),
				totalContigAssemblyFails.get(),
				totalInvalidContigs.get(),
				totalUnmatchablePrimer5.get(),
				totalUnmatchablePrimer3.get(),
				totalInvalidCycle.get(), 
				totalPrimerOverlaps.get()
				);
	}

	
}
