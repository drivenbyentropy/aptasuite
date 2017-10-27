/**
 * 
 */
package lib.parser.aptaplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.lang3.ArrayUtils;

import com.milaboratory.core.PairedEndReadsLayout;
import com.milaboratory.core.io.sequence.PairedRead;
import com.milaboratory.core.io.sequence.SingleReadImpl;
import com.milaboratory.core.merger.MismatchOnlyPairedReadMerger;
import com.milaboratory.core.merger.PairedReadMergingResult;
import com.milaboratory.core.merger.QualityMergingAlgorithm;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import exceptions.InvalidConfigurationException;
import lib.aptamer.datastructures.Metadata;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.aptaplex.distances.BitapDistance;
import lib.parser.aptaplex.distances.Distance;
import lib.parser.aptaplex.distances.EditDistance;
import lib.parser.aptaplex.distances.Result;
import utilities.Accumulator;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 *         The consumer implementation of aptaplex. The consumer takes reads
 *         from the queue, processes them and adds them to the aptamer pool.
 */
public class AptaPlexConsumer implements Runnable {

	/**
	 * The queue to consume from
	 */
	private BlockingQueue<Object> queue = null;

	/**
	 * The progress of the parser instance. Writable to the consumers and
	 * thread-safe
	 */
	private AptaPlexProgress progress = null;

	/**
	 * Switch to let the consumer know when to finish
	 */
	private Boolean isRunning = true;

	/**
	 * Holds the read currently being processed
	 */
	private Read read = null;

	/**
	 * The 5 prime barcodes in the same order than specified in the
	 * configuration. If isPerFile is true, this is an empty list.
	 */
	private List<byte[]> barcodes5 = new ArrayList<byte[]>();

	/**
	 * Temporary parameters containing the determined index of matching the
	 * barcodes to the contig
	 */
	private Integer barcode_matched_index = null;

	/**
	 * The 3 prime barcodes in the same order than specified in the
	 * configuration. If isPerFile is true, this is an empty list.
	 */
	private List<byte[]> barcodes3 = new ArrayList<byte[]>();

	/**
	 * List of all cycles in the same order as <code>barcodes</code>
	 */
	private ArrayList<SelectionCycle> cycles = Configuration.getExperiment().getAllSelectionCycles();

	/**
	 * Access to the 5 prime primer
	 */
	private byte[] primer5 = Configuration.getParameters().getString("Experiment.primer5").getBytes();
	private byte[] primer5reverse = Configuration.getParameters().getString("Experiment.primer5").getBytes();
	
	/**
	 * Access to the 5 prime primer
	 */
	private byte[] primer3 = Configuration.getParameters().getString("Experiment.primer3").getBytes();

	/**
	 * True if the sequences have previously been demultiplexed
	 */
	private boolean isPerFile = Configuration.getParameters().getBoolean("AptaplexParser.isPerFile");

	/**
	 * If no 3' primer is present, the parser needs to know the size of the
	 * randomized region to extract
	 */
	private Integer randomizedRegionSize = null;
	
	/**
	 * Access to the data structures storing the Nucleotide distributions, 
	 * Quality Scores etc of the data
	 */
	private Metadata metadata = Configuration.getExperiment().getMetadata();

	/**
	 * Instance of the MiTools merger used to create the contig sequences in
	 * case of paired end sequencing
	 */
	MismatchOnlyPairedReadMerger merger = new MismatchOnlyPairedReadMerger(
			Configuration.getParameters().getInt("AptaplexParser.PairedEndMinOverlap"),
			1.0 - 1.0 * Configuration.getParameters().getInt("AptaplexParser.PairedEndMaxMutations")
					/ Configuration.getParameters().getInt("AptaplexParser.PairedEndMinOverlap"),
			Configuration.getParameters().getInt("AptaplexParser.PairedEndMaxScoreValue"),
			QualityMergingAlgorithm.SumSubtraction, PairedEndReadsLayout.Unknown);

	/**
	 * Distance class used to determine the best match for barcodes and primers
	 * in the contig.
	 */
	Distance bitapDistance = new BitapDistance();
	Distance editDistance = new EditDistance();

	/**
	 * Tolerances, i.e. maximal number of mismatches for matching primer and
	 * barcodes to the contig
	 */
	int primerTolerance = Configuration.getParameters().getInt("AptaplexParser.PrimerTolerance");
	int barcodeTolerance = Configuration.getParameters().getInt("AptaplexParser.BarcodeTolerance");

	public AptaPlexConsumer(BlockingQueue<Object> queue, AptaPlexProgress progress) {

		this.queue = queue;
		this.progress = progress;

		// get the barcodes in the correct format
		for (String barcode : Configuration.getParameters().getStringArray("AptaplexParser.barcodes5Prime")) {
			barcodes5.add(barcode.getBytes());
		}

		// get the barcodes in the correct format
		for (String barcode : Configuration.getParameters().getStringArray("AptaplexParser.barcodes3Prime")) {
			barcodes3.add(barcode.getBytes());
		}

		try {
			randomizedRegionSize = Configuration.getParameters().getInt("Experiment.randomizedRegionSize");
		} catch (NoSuchElementException e) {
		}

		// we need to make sure that if no 3'primer was specified, we do at
		// least have a
		// randomized region size. Otherwise, extraction will fail
		if (this.primer3 == null && this.randomizedRegionSize == null) {
			throw new InvalidConfigurationException(
					"Error. Neither the 3' primer nor a randomized region size was specified. Please add either of these parameters to the configuration.");
		}

		// compute the reverse of the 5 primer primer. This is required due to the inner workings of the Bitap algorithm
		ArrayUtils.reverse(this.primer5reverse);
	}

	@Override
	public void run() {

		// keep taking elements from the queue
		byte[] contig = null;
		int randomized_region_start_index = -1;
		int randomized_region_end_index = -1;
		
		while (isRunning) {
			try {
				Object queueElement = queue.take();

				if (queueElement == Configuration.POISON_PILL) {
					AptaLogger.log(Level.CONFIG, this.getClass(), "Encountered poison pill. Exiting thread.");
					queue.put(Configuration.POISON_PILL); // notify other threads to stop
					return;
				}

				// Update the progress in a thread-safe manner
				progress.totalProcessedReads.incrementAndGet();

				// process queueElement
				read = (Read) queueElement;


				// Differentiate between single-end and paired-end sequencing
				if (read.reverse_read != null) {

					// if paired end, we need to compute the transcribed inverse
					// for the reverse read
					computeTranscribedReverse();

					// and create the contig of the two
					contig = computeContig();
					
					// if we failed to assemble, there is no need to continue at
					// this point
					if (contig == null) {
						progress.totalContigAssemblyFails.incrementAndGet();
						continue;
					}
				} else {
					contig = read.forward_read;
				}

				// check for undetermined nucleotides and fail if present
				if (!isValidSequence(contig)) {
					progress.totalInvalidContigs.incrementAndGet();
					continue;
				}

				// Match the 5' primer
				// Since the bitab alrithms starts at the 3' end of the sequence
				// and returns the first element which matches, we need to turn the search 
				// problem around
				byte[] contigreverse = contig.clone();
				ArrayUtils.reverse(contigreverse);
				Result primer5_match = matchPrimer(contigreverse, primer5reverse);
				if (primer5_match != null){
					primer5_match.index = contig.length - primer5_match.index - primer5reverse.length; 
				}

				if (primer5_match == null) { // no match
					progress.totalUnmatchablePrimer3.incrementAndGet();
					continue;
				}

				// Match the 3' primer if present
				Result primer3_match = null;
				if (primer3 != null) {
					primer3_match = matchPrimer(contig, primer3);

					if (primer3_match == null){ // no match
						progress.totalUnmatchablePrimer3.incrementAndGet();
						continue;
					}
				}

				// Identify the selection cycle this read corresponds to
				if (!isPerFile) {
					read.selection_cycle = matchBarcodes(contig, primer5_match, primer3_match);
				}

				// Check for possible conflicts

				// selection cycle assignment failed
				if (read.selection_cycle == null){
					progress.totalInvalidCycle.getAndIncrement();
					continue;
				}
				
				// the primers need to be checked for overlap
				 if (primer3 != null && isOverlapped(primer5_match, primer5, primer3_match, primer3)){
					 progress.totalPrimerOverlaps.incrementAndGet();
					 continue;
				 }
				
				 // we can now extract the randomized region
				 randomized_region_start_index = primer5_match.index + primer5.length;
				 randomized_region_end_index = -1;
				
				 if (primer3 == null){ //use Experiment.randomizedRegionSize
					 	randomized_region_end_index = randomized_region_start_index + randomizedRegionSize -1;
				 }
				 else{ // use the boundaries defined by the primer regions
					 randomized_region_end_index = primer3_match.index;
				 }
				
				 // if the sequence was exacted successfully, we can finally
				 // add it to the selection cycle
				 if (
						    (randomized_region_start_index < randomized_region_end_index && randomized_region_end_index <= contig.length) //Primers are in the correct order
						 && (randomized_region_start_index-primer5.length >= 0) // 5' primer does not overshoot the contig to the left
						 && (randomized_region_end_index+primer3.length <= contig.length) // 3' primer does not overshoot the contig to the right
					){
					 
					 read.selection_cycle.addToSelectionCycle(
							 Arrays.copyOfRange(contig, randomized_region_start_index-primer5.length, randomized_region_end_index+primer3.length)
							 ,primer5.length
							 ,primer5.length + (randomized_region_end_index-randomized_region_start_index)
							 //,randomized_region_start_index
							 //,randomized_region_end_index
							 );
					 
					 // Add metadata information
					 addAcceptedNucleotideDistributions(read.selection_cycle, contig, randomized_region_start_index, randomized_region_end_index);
					 addNuceotideDistributions();
					 addQualityScores();
					 
					 progress.totalAcceptedReads.incrementAndGet();
					 
				 }else { // Handle errors
					 
					 if (randomized_region_start_index-primer5.length < 0) {
						 
						 progress.totalUnmatchablePrimer5.incrementAndGet();
						 
					 }
					 else if(randomized_region_end_index+primer3.length > contig.length) {
						 
						 progress.totalUnmatchablePrimer3.incrementAndGet();
						 
					 }
					 
				 }

			} catch (Exception e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), String.format("Aptamer: %s Bounds: %s %s", new String(contig), randomized_region_start_index, randomized_region_end_index));
				AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
			}
		}

	}

	/**
	 * Computes the transcribed inverse of the reverse read, also reverses the
	 * corresponding quality scores
	 */
	private void computeTranscribedReverse() {

		// sequence in byte array representation (ASCII)
		for (int x = 0; x < read.reverse_read.length; x++) {

			switch (read.reverse_read[x]) {
			case 65:
				read.reverse_read[x] = 84;
				break;

			case 67:
				read.reverse_read[x] = 71;
				break;

			case 71:
				read.reverse_read[x] = 67;
				break;

			case 84:
				read.reverse_read[x] = 65;
				break;
			}
		}

		// reverse both, sequence and quality scores
		for (int i = 0; i < read.reverse_read.length / 2; i++) {
			byte temp_s = read.reverse_read[i];
			read.reverse_read[i] = read.reverse_read[read.reverse_read.length - i - 1];
			read.reverse_read[read.reverse_read.length - i - 1] = temp_s;

			byte temp_q = read.reverse_quality[i];
			read.reverse_quality[i] = read.reverse_quality[read.reverse_quality.length - i - 1];
			read.reverse_quality[read.reverse_quality.length - i - 1] = temp_q;
		}

	}

	/**
	 * Given paired end reads and quality scores, compute the contig of the two
	 * corresponding reads using miLib
	 * 
	 * @see <a href=
	 *      "https://github.com/milaboratory/milib/">https://github.com/milaboratory/milib/</a>
	 * 
	 */
	private byte[] computeContig() {

		// Create a new new PairedRead
		PairedRead paired_read = new PairedRead(new SingleReadImpl(0,
				new NSequenceWithQuality(new String(read.forward_read), new String(read.forward_quality)), "forward"),
				new SingleReadImpl(0,
						new NSequenceWithQuality(new String(read.reverse_read), new String(read.reverse_quality)),
						"reverse"));

		// Align and Merge
		PairedReadMergingResult processed = merger.process(paired_read);

		// we could not merge successfully
		if (!processed.isSuccessful()) {
			return null;
		}

		// if the merger was successful, we return it
		return processed.getOverlappedSequence().getSequence().toString().getBytes();
	}

	/**
	 * Returns true if the contig does not contain any characters other that A C
	 * G and T
	 * 
	 * @param contig
	 *            the contig to check
	 * @return true if valid sequence
	 */
	private boolean isValidSequence(byte[] contig) {

		for (int x = 0; x < contig.length; x++) {
			if (contig[x] != 65 && contig[x] != 67 && contig[x] != 71 && contig[x] != 84) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Find the best fuzzy match for the primer using the bitap algorithm
	 * 
	 * @param c
	 *            contig
	 * @param primer
	 * @return Result object containing the start index of the position and the
	 *         score. null if no match was found
	 */
	public Result matchPrimer(byte[] c, byte[] primer) { //TODO: Change to private
		
		if (primer.length > 32) { // we default to the slower edit distance
			return editDistance.indexOf(c, primer, primerTolerance, 0, c.length);
		}
		else{ // we can use the fast bitap algorithm
			
			// and an approximate match
			Result best_match = bitapDistance.indexOf(c, primer, primerTolerance, 0, c.length);

			// if matching failed, there is no need to continue
			if (best_match == null) {
				return null;
			}

			// do we have a perfect match?
			if (best_match.errors == 0) {
				return best_match;
			}

			// else we need to recompute the edit distance in order to determine
			// the exact index
			else {

				// make sure the edit distance coincides with the number of
				// errors
				int mismatches = 0;
				for (int x = best_match.index, y = 0; y < primer.length; x++, y++) {

					try{
						if (c[x] != primer[y]) {
							mismatches++;
						}
					}
					// if the primer only overlaps the contig, we count it as a mismatch too
					catch(ArrayIndexOutOfBoundsException e){
						mismatches++;
					}
				}
				if (best_match.errors == mismatches) {
					return best_match;
				}

				// we have to shift to the left and compare again until the
				// tolerance is reached
				int best_mismatches = mismatches;
				int best_index = best_match.index;

				for (int t = 1; t < primerTolerance; t++) {
					mismatches = 0;
					for (int x = best_match.index - t, y = 0; y < primer.length; x++, y++) {
						
						try{
							if (c[x] != primer[y]) {
								mismatches++;
							}
						}
						catch(ArrayIndexOutOfBoundsException e){
							mismatches++;							
						}
					}

					// do we have a better match?
					if (best_mismatches > mismatches) {
						best_mismatches = mismatches;
						best_index = best_match.index - t;
					}
				}

				// did we find a good position?
				if (best_mismatches <= primerTolerance) {
					best_match.index = best_index;
					best_match.errors = best_mismatches;

					return best_match;
				}

			}
		}
		
		// no match was found
		return null;
	}

	/**
	 * Align all barcodes to the contig and assign it to the selection cycle
	 * with the best alignment score.
	 * 
	 * @param c
	 *            contig
	 * @param primermatch5
	 *            Result of the matching procedure for the 5 prime primer
	 * @param primermatch3
	 *            Result of the matching procedure for the 3 prime primer. null
	 *            if no primer specified.
	 * @return SelectionCycle corresponding to the best match
	 */
	private SelectionCycle matchBarcodes(byte[] c, Result primermatch5, Result primermatch3) {

		// match the 5' barcode if it exists
		Result barcodeMatch5 = null;
		Integer barcode_index5 = null;
		if (barcodes5.size() != 0) {

			for (int x = 0; x < barcodes5.size(); x++) {
				// restrict the search space to the left side of the 5' primer
				Result current_match = bitapDistance.indexOf(c, barcodes5.get(x), barcodeTolerance, 0,
						primermatch5.index);

				if (current_match != null && (barcodeMatch5 == null || barcodeMatch5.errors > current_match.errors)
						&& current_match.errors <= barcodeTolerance) {
					barcodeMatch5 = current_match;
					barcode_index5 = x;
				}
			}
		}

		// match the 3' barcode if it exists
		Result barcodeMatch3 = null;
		Integer barcode_index3 = null;
		if (barcodes3.size() != 0) {

			for (int x = 0; x < barcodes3.size(); x++) {
				// restrict the search space to the right side of the 3' primer
				Result current_match = bitapDistance.indexOf(c, barcodes3.get(x), barcodeTolerance,
						primermatch3.index + primer3.length, c.length);

				if (current_match != null && (barcodeMatch3 == null || barcodeMatch3.errors > current_match.errors)
						&& current_match.errors <= barcodeTolerance) {
					barcodeMatch3 = current_match;
					barcode_index3 = x;
				}
			}
		}
		
		// Differentiate between the different scenarios
		
		// If both barcodes are present, the identified cycles have to coincide
		if (barcodes5.size() != 0 && barcodes3.size() != 0){
			if (barcode_index3 == barcode_index5){
				return cycles.get(barcode_index5);
			}
		}
		
		// only the 5' barcode is present
		else if (barcodes5.size() != 0 && barcode_index5 != null){
			return cycles.get(barcode_index5);
		}
		
		// only the 3' barcode is present
		else if (barcodes3.size() != 0 && barcode_index3 != null){
			return cycles.get(barcode_index3);
		}

		// we could not assign a cycle 
		return null;
	}

	/**
	 * Given the alignment of 2 sequences onto the same contig, check if their
	 * alignment positions overlap.
	 * 
	 * @param al1
	 * @param al2
	 * @param seq1
	 * @param seq2
	 * @return true if overlap is detected, false otherwise
	 */
	private boolean isOverlapped(Result match1, byte[] seq1, Result match2, byte[] seq2) {

		// define the boundary of the sequences in coordinated of the contig
		int s1start = match1.index;
		int s1end = s1start + seq1.length - 1;

		int s2start = match2.index;
		int s2end = s2start + seq2.length - 1;

		// check for overlap
		if ( (s1end >= s2start && s1end >= s2end ) || (s1start <= s2end && s1start >= s2start) ) {
			return true;
		}

		return false;
	}

	
	/**
	 * Takes the quality scores of the current read and adds them to the 
	 * meta data accumulator of the selection cycle c. Note this function
	 * assumes all reads are of the same length.
	 * @param c the selection cycle corresponding to this read
	 */
	private void addQualityScores() {
		
		// Forward read
		if (read.forward_quality != null) {
		
			ConcurrentHashMap<Integer, Accumulator> forward = metadata.qualityScoresForward.get(read.selection_cycle.getName());
			
			// Iterate over the read and add quality scores to the accumulators
			for (int i= 0; i < read.forward_quality.length; i++) {
				
				if(!forward.contains(i)) forward.put(i, new Accumulator());
				forward.get(i).addDataValue(read.forward_quality[i]-33);
				
			}
			
		}
		
		// Reverse read
		if (read.reverse_quality != null) {
		
			ConcurrentHashMap<Integer, Accumulator> reverse = metadata.qualityScoresReverse.get(read.selection_cycle.getName());
			
			// Iterate over the read and add quality scores to the accumulators
			for (int i= 0; i < read.reverse_quality.length; i++) {
				
				if(!reverse.contains(i)) reverse.put(i, new Accumulator());
				reverse.get(i).addDataValue(read.reverse_quality[i]-33);
				
			}
			
		}
		
	}
	
	/**
	 * Iterates over the forward and reverse read (if present) and adds 
	 * the nucleotide counts to the meta data
	 * @param c
	 */
	private void addNuceotideDistributions() {
		
		// Forward read
		if (read.forward_read != null) {
		
			ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>> forward = metadata.nucleotideDistributionForward.get(read.selection_cycle.getName());
			
			// Iterate over the read add add quality scores to the accumulators
			for (int i= 0; i < read.forward_read.length; i++) {
				
				// Make sure the entry exists prior to adding
				if(!forward.contains(i)) {
					ConcurrentHashMap<Byte,Integer> map = new ConcurrentHashMap<Byte,Integer>();
					map.put((byte) 'A', 0); 
					map.put((byte) 'C', 0);
					map.put((byte) 'G', 0);
					map.put((byte) 'T', 0);
					map.put((byte) 'N', 0);
					forward.put(i, map);
				}
				
				// Add nucleotides
				forward.get(i).put(read.forward_read[i], forward.get(i).get(read.forward_read[i])+1 );
				
			}
			
		}
		
		// Reverse read
		if (read.reverse_read != null) {
		
			ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>> reverse = metadata.nucleotideDistributionReverse.get(read.selection_cycle.getName());
			
			// Iterate over the read add add quality scores to the accumulators
			for (int i= 0; i < read.reverse_read.length; i++) {
				
				// Make sure the entry exists prior to adding
				if(!reverse.contains(i)) {
					ConcurrentHashMap<Byte,Integer> map = new ConcurrentHashMap<Byte,Integer>();
					map.put((byte) 'A', 0); 
					map.put((byte) 'C', 0);
					map.put((byte) 'G', 0);
					map.put((byte) 'T', 0);
					map.put((byte) 'N', 0);
					reverse.put(i, map);
				}
				
				// Add nucleotides
				reverse.get(i).put(read.reverse_read[i], reverse.get(i).get(read.reverse_read[i])+1 );
				
			}
			
		}
		
	}
	
	/**
	 * Adds the nucleotide distribution of the randomized region to the meta data, categorized 
	 * by the length of the region
	 * @param sc
	 * @param contig
	 * @param randomized_region_start_index
	 * @param randomized_region_end_index
	 */
	private void addAcceptedNucleotideDistributions(SelectionCycle sc, byte[] contig, int randomized_region_start_index, int randomized_region_end_index) {
		
		int randomized_region_size = randomized_region_end_index - randomized_region_start_index;

		// Make sure we have seen this randomized region size before, else create placeholder
		if (!metadata.nucleotideDistributionAccepted.get(sc.getName()).contains(randomized_region_size)) {
					
			metadata.nucleotideDistributionAccepted.get(sc.getName()).put(randomized_region_size, new ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>());
			
		}
		
		ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>> accepted = metadata.nucleotideDistributionAccepted.get(sc.getName()).get(randomized_region_size);
		
		int i = 0;
		for (int x=randomized_region_start_index; x<randomized_region_end_index; x++) {
			
			// Make sure the entry exists prior to adding
			if (!accepted.contains(i)) {
				
				ConcurrentHashMap<Byte,Integer> map = new ConcurrentHashMap<Byte,Integer>(5);
				map.put((byte) 'A', 0); 
				map.put((byte) 'C', 0);
				map.put((byte) 'G', 0);
				map.put((byte) 'T', 0);
				map.put((byte) 'N', 0);
				accepted.put(i, map);
				
			}
			
			// Add nucleotides
			accepted.get(i).put(contig[x], accepted.get(i).get(contig[x])+1);
			
			i++;
		}
		
	}
	
	
}
