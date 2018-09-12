/**
 * 
 */
package lib.parser.aptaplex;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import lib.export.CompressedExportWriter;
import lib.export.ExportWriter;
import lib.export.FastqExportFormat;
import lib.export.SequencingDirection;
import lib.export.UncompressedExportWriter;
import lib.parser.aptaplex.distances.BitapDistance;
import lib.parser.aptaplex.distances.Distance;
import lib.parser.aptaplex.distances.EditDistance;
import lib.parser.aptaplex.distances.Result;
import utilities.Accumulator;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Pair;

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
	 * True if the sequences have previously been stripped of their primers and barcodes
	 */
	private boolean onlyRandomizedRegionInData = Configuration.getParameters().getBoolean("AptaplexParser.OnlyRandomizedRegionInData");
	
	/**
	 * If true, the reverse complement of the sequences will be stored. This is usefull for DNA aptamers 
	 */
	private boolean storeReverseComplement = Configuration.getParameters().getBoolean("AptaplexParser.StoreReverseComplement"); 
			
	/**
	 * If true, the reverse complement of the contig will be checked for barcodes, primers and randomized regions 
	 * should the extraction from the original contig fail
	 */
	private boolean checkReverseComplement = Configuration.getParameters().getBoolean("AptaplexParser.CheckReverseComplement");
	
	/**
	 * If no 3' primer is present, the parser needs to know the size of the
	 * randomized region to extract
	 */
	private Integer randomizedRegionSizeExactBound = null;
	
	/**
	 * The user can specify a range of size to be accepted. 
	 * This is the smallest size of the randomized region to be accepted.
	 */
	private Integer randomizedRegionSizeLowerBound = null;
	
	/**
	 * The user can specify a range of size to be accepted. 
	 * This is the largest size of the randomized region to be accepted.
	 */
	private Integer randomizedRegionSizeUpperBound = null;
	
	/**
	 * Access to the data structures storing the Nucleotide distributions, 
	 * Quality Scores etc of the data
	 */
	private Metadata metadata = Configuration.getExperiment().getMetadata();

	/**
	 * Instance of the MiTools merger used to create the contig sequences in
	 * case of paired end sequencing
	 */
	private MismatchOnlyPairedReadMerger merger = new MismatchOnlyPairedReadMerger(
			Configuration.getParameters().getInt("AptaplexParser.PairedEndMinOverlap"),
			1.0 - 1.0 * Configuration.getParameters().getInt("AptaplexParser.PairedEndMaxMutations")
					/ Configuration.getParameters().getInt("AptaplexParser.PairedEndMinOverlap"),
			Configuration.getParameters().getInt("AptaplexParser.PairedEndMaxScoreValue"),
			QualityMergingAlgorithm.SumSubtraction, PairedEndReadsLayout.Unknown);

	/**
	 * Distance class used to determine the best match for barcodes and primers
	 * in the contig.
	 */
	private Distance bitapDistance = new BitapDistance();
	private Distance editDistance = new EditDistance();

	/**
	 * Tolerances, i.e. maximal number of mismatches for matching primer and
	 * barcodes to the contig
	 */
	private int primerTolerance = Configuration.getParameters().getInt("AptaplexParser.PrimerTolerance");
	private int barcodeTolerance = Configuration.getParameters().getInt("AptaplexParser.BarcodeTolerance");

	/**
	 * Placeholder in case failed reads are to be exported to file. If not null
	 * reads will be written to file. 
	 */
	private Map<Path, Pair<ExportWriter,ExportWriter>> undeterminedExportWriterMap = null;
	
	/**
	 * Writes a read in fastq format
	 */
	private FastqExportFormat fastqExportFormat = new FastqExportFormat("undetermined");
	
	public AptaPlexConsumer(BlockingQueue<Object> queue, AptaPlexProgress progress, Map<Path, Pair<ExportWriter,ExportWriter>> undeterminedExportWriterMap) {

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

		// do we have restrictions on the randomized region sizes?
		try {
			randomizedRegionSizeExactBound = Configuration.getParameters().getInt("Experiment.randomizedRegionSize");
		} catch (NoSuchElementException e) {
		}
		
		try {
			randomizedRegionSizeLowerBound = Configuration.getParameters().getInt("AptaplexParser.randomizedRegionSizeLowerBound");
		} catch (NoSuchElementException e) {
		}
		
		try {
			randomizedRegionSizeUpperBound = Configuration.getParameters().getInt("AptaplexParser.randomizedRegionSizeUpperBound");
		} catch (NoSuchElementException e) {
		}

		// we need to make sure that if no 3'primer was specified, we do at
		// least have a
		// randomized region size. Otherwise, extraction will fail
		if (this.primer3 == null && this.randomizedRegionSizeExactBound == null) {
			throw new InvalidConfigurationException("Error. Neither the 3' primer nor a randomized region size was specified. Please add either of these parameters to the configuration.");
		}
		
		// the exact randomized region size takes precedence over the range
		if (this.randomizedRegionSizeExactBound != null) {
			
			this.randomizedRegionSizeLowerBound = null;
			this.randomizedRegionSizeUpperBound = null;
			
		}
		
		// if using the range, we need both upper and lower and lower < upper
		if (
				(this.randomizedRegionSizeLowerBound != null &&  this.randomizedRegionSizeUpperBound == null) ||
				(this.randomizedRegionSizeLowerBound == null &&  this.randomizedRegionSizeUpperBound != null) 						
			) {
			throw new InvalidConfigurationException("Error. Either the lower or upper bound for the range of the randomized region size was not specified. Please add these missing parameters to the configuration.");
		}

		if ( 
				(this.randomizedRegionSizeLowerBound != null &&  this.randomizedRegionSizeUpperBound != null) && 
				(this.randomizedRegionSizeLowerBound >= this.randomizedRegionSizeUpperBound)
				
			) {
			throw new InvalidConfigurationException( "Error. The lower bound for the range of the randomized region size must be smaller than the upper bound. Please check your configuration");
		}
		
		// instantiate a file writer
		this.undeterminedExportWriterMap = undeterminedExportWriterMap;
		
		// compute the reverse of the 5 primer primer. This is required due to the inner workings of the Bitap algorithm
		ArrayUtils.reverse(this.primer5reverse);
	}

	@Override
	public void run() {

		// keep taking elements from the queue
		byte[] contig = null;
		
		while (isRunning) {
			try {
				Object queueElement = queue.take();

				if (queueElement == Configuration.POISON_PILL) {
					AptaLogger.log(Level.CONFIG, this.getClass(), "Encountered poison pill. Exiting thread.");
					queue.put(Configuration.POISON_PILL); // notify other threads to stop
					return;
				}

				// get the contig...
				contig = getContig(queueElement);
				
				// ...check for undetermined nucleotides and fail if present...
				if (!isValidSequence(contig)) {
					progress.totalInvalidContigs.incrementAndGet();
					continue;
				}
				
				
				// we need to differentiate between randomized region only mode, 
				// and fully parsable data
				if (this.onlyRandomizedRegionInData) {
					
					this.processReadRandomizedRegionOnly(contig);
					
				}
				else {
				
					// ...now try to process it
					AtomicInteger returnCode = processRead(contig, false, null);
					
					// only look into reverse complement if configuration indicates it and above matching has failed
					if (returnCode != null && checkReverseComplement) {
					
						// if reverse complement is requested, compute it, in-place
						// sequence in byte array representation (ASCII)
						for (int x = 0; x < contig.length; x++) {
			
							switch (contig[x]) {
							case 65:
								contig[x] = 84;
								break;
			
							case 67:
								contig[x] = 71;
								break;
			
							case 71:
								contig[x] = 67;
								break;
			
							case 84:
								contig[x] = 65;
								break;
							}
						}
						
						for (int i = 0; i < contig.length / 2; i++) {
							byte temp_s = contig[i];
							contig[i] = contig[contig.length - i - 1];
							contig[contig.length - i - 1] = temp_s;
						}
						
						// process as usual
						returnCode = processRead(contig, true, returnCode);
							
					}
					
					// if we could still not parse this read, we write it to the 
					// undetermined lane if the user requested it
					if (returnCode != null && undeterminedExportWriterMap != null) {
						
						// Forward 
						undeterminedExportWriterMap.get(read.source_forward).first.write(fastqExportFormat.format(read, SequencingDirection.FORWARD));
						
						// Reverse
						if (undeterminedExportWriterMap.get(read.source_forward).second != null) {
						
							undeterminedExportWriterMap.get(read.source_forward).second.write(fastqExportFormat.format(read, SequencingDirection.REVERSE));
							
						}
						
					}
					
				}
				
				// Update the progress in a thread-safe manner
				progress.totalProcessedReads.incrementAndGet();


			} catch (Exception e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
				AptaLogger.log(Level.SEVERE, this.getClass(), String.format("Aptamer: %s", new String(contig)));
			}
		}

	}

	private byte[] getContig(Object queueElement) {
		
		// keep taking elements from the queue
		byte[] contig = null;
		
		// Update the progress in a thread-safe manner
		// progress.totalProcessedReads.incrementAndGet(); TODO: Move this to the end of run();

		// process queueElement
		read = (Read) queueElement;
		
		// Differentiate between single-end and paired-end sequencing
		if (read.reverse_read != null) {

			// if paired end, we need to compute the transcribed inverse
			// for the reverse read
			computeTranscribedReverse();

			// and create the contig of the two
			contig = computeContig();
			
		} else {
			contig = read.forward_read;
		}
		
		// check for undetermined nucleotides and fail if present
		if (!isValidSequence(contig)) {
			return null;
		}
		
		return contig;
		
	}
	
	/**
	 * Processes the current queueElement. Assumes that the read consists only of the randomized region 
	 * @param queueElement the element to be processed 
	 * @return the error progress counter that was incremented in the last call of this function. null if success.  
	 */
	private AtomicInteger processReadRandomizedRegionOnly(byte[] contig_rr) {
		
		 // add the primers synthetically to the reads
		byte[] contig = new byte[primer5.length + contig_rr.length + primer3.length];

		System.arraycopy(primer5, 0, contig, 0, primer5.length);
		System.arraycopy(contig_rr, 0, contig, primer5.length, contig_rr.length);
		System.arraycopy(primer3, 0, contig, primer5.length+contig_rr.length, primer3.length);
		
		 // we extract the randomized region
		 int randomized_region_start_index = primer5.length;
		 int randomized_region_end_index = primer5.length+contig_rr.length;
		
		 // and add it to the selection cycle	 
		 if (!storeReverseComplement) { // Do we have to compute the reverse complement?
		 
			read.selection_cycle.addToSelectionCycle(
				 contig,
				 primer5.length,
				 primer5.length + (randomized_region_end_index-randomized_region_start_index)
				 );
			 
		 	// Add metadata information
			addAcceptedNucleotideDistributions(read.selection_cycle, contig, randomized_region_start_index, randomized_region_end_index);

		 } else { // We do!
			 
			// compute the complement...
			for (int x = 0; x < contig.length; x++) {

				switch (contig[x]) {
				case 65:
					contig[x] = 84;
					break;

				case 67:
					contig[x] = 71;
					break;

				case 71:
					contig[x] = 67;
					break;

				case 84:
					contig[x] = 65;
					break;
				}
			}
			
			// ...and reverse it
			for(int i = 0; i < contig.length / 2; i++)
			{
			    byte temp = contig[i];
			    contig[i] = contig[contig.length - i - 1];
			    contig[contig.length - i - 1] = temp;
			}
			
			// We also need to recompute the boundaries
			int start = contig.length - (primer5.length + (randomized_region_end_index-randomized_region_start_index));
			int end = contig.length - primer5.length;
			
			read.selection_cycle.addToSelectionCycle(
					 contig
					 ,start
					 ,end
					 );
			 
			 
			 // Add metadata information
			 addAcceptedNucleotideDistributions(read.selection_cycle, contig, start, end);
			 
		 }
		 
	 	 // Store nucleotide distribution and quality score
	 	 addNuceotideDistributions();
		 addQualityScores();
		 
		 progress.totalAcceptedReads.incrementAndGet();
		 
		 // if we are here, all went well and we can go home.
		 return null;
		
	}
	
	
	/**
	 * Processes the current queueElement and attempts to demultiplex and extract the randomized region 
	 * @param queueElement the element to be processed
	 * @param reverse_direction if true, the reverse complement of the read will be returned. 
	 * @param previous_return_code result of a previous call of processRead. -1 on first call
	 * This is useful if the raw reads are present in reverse complemented orientation.
	 * @return the error progress counter that was incremented in the last call of this function. null if success.  
	 */
	private AtomicInteger processRead(byte[] contig, boolean reverse_complement, AtomicInteger previous_return_code) {
		
		int randomized_region_start_index = -1;
		int randomized_region_end_index = -1;
		
		// Match the 5' primer
		// Since the bitab algorithms starts at the 3' end of the sequence
		// and returns the first element which matches, we need to turn the search 
		// problem around
		byte[] contigreverse = contig.clone();
		ArrayUtils.reverse(contigreverse);		
		
		Result primer5_match = matchPrimer(contigreverse, primer5reverse);
		if (primer5_match != null){
			primer5_match.index = contig.length - primer5_match.index - primer5reverse.length; 
		}

		if (primer5_match == null) { // no match
			progress.totalUnmatchablePrimer5.incrementAndGet();
			if(previous_return_code != null) {
				previous_return_code.decrementAndGet();
			}
			return progress.totalUnmatchablePrimer5;
		}

		// Match the 3' primer if present
		Result primer3_match = null;
		if (primer3 != null) {
			primer3_match = matchPrimer(contig, primer3);

			if (primer3_match == null){ // no match
				progress.totalUnmatchablePrimer3.incrementAndGet();
				if(previous_return_code != null) {
					previous_return_code.decrementAndGet();
				}				
				return progress.totalUnmatchablePrimer3;
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
			if(previous_return_code != null) {
				previous_return_code.decrementAndGet();
			}
			return progress.totalInvalidCycle;
		}
		
		// the primers need to be checked for overlap
		 if (primer3 != null && isOverlapped(primer5_match, primer5, primer3_match, primer3)){
			 progress.totalPrimerOverlaps.incrementAndGet();
			 if(previous_return_code != null) {
				 previous_return_code.decrementAndGet();
			 }
			 return progress.totalPrimerOverlaps;
		 }
		
		 // we can now extract the randomized region
		 randomized_region_start_index = primer5_match.index + primer5.length;
		 randomized_region_end_index = -1;
		
		 if (primer3 == null){ //use Experiment.randomizedRegionSize
			 	randomized_region_end_index = randomized_region_start_index + randomizedRegionSizeExactBound -1;
		 }
		 else{ // use the boundaries defined by the primer regions
			 randomized_region_end_index = primer3_match.index;
		 }
		
		 // if the sequence was exacted successfully, we can finally
		 // add it to the selection cycle
		 if (		// Sanity checks
				    (randomized_region_start_index < randomized_region_end_index && randomized_region_end_index <= contig.length) //Primers are in the correct order
				 && (randomized_region_start_index-primer5.length >= 0) // 5' primer does not overshoot the contig to the left
				 && (randomized_region_end_index+primer3.length <= contig.length) // 3' primer does not overshoot the contig to the right
				 && (randomizedRegionSizeExactBound != null ? (randomized_region_end_index-randomized_region_start_index) == randomizedRegionSizeExactBound : true) // if the randomized region is specified, only let those aptamers through that have the desired lenght
				 && (randomizedRegionSizeLowerBound != null ? randomizedRegionSizeLowerBound <= (randomized_region_end_index-randomized_region_start_index) && (randomized_region_end_index-randomized_region_start_index) <= randomizedRegionSizeUpperBound : true) // if a range of sizes is specified, make sure the aptamer falls into these categories
			){
			 
			 if (!storeReverseComplement) { // Do we have to compute the reverse complement?
			 
				read.selection_cycle.addToSelectionCycle(
					 Arrays.copyOfRange(contig, randomized_region_start_index-primer5.length, randomized_region_end_index+primer3.length)
					 ,primer5.length
					 ,primer5.length + (randomized_region_end_index-randomized_region_start_index)
					 );
				 
			 	// Add metadata information
				addAcceptedNucleotideDistributions(read.selection_cycle, contig, randomized_region_start_index, randomized_region_end_index);

			 } else { // We do!
				 
				// First, extract the relevant region from the read
				contig = Arrays.copyOfRange(contig, randomized_region_start_index-primer5.length, randomized_region_end_index+primer3.length);
				 
				// Now compute the reverse complement
				for (int x = 0; x < contig.length; x++) {

					switch (contig[x]) {
					case 65:
						contig[x] = 84;
						break;

					case 67:
						contig[x] = 71;
						break;

					case 71:
						contig[x] = 67;
						break;

					case 84:
						contig[x] = 65;
						break;
					}
				}
				
				// And reverse it
				for(int i = 0; i < contig.length / 2; i++)
				{
				    byte temp = contig[i];
				    contig[i] = contig[contig.length - i - 1];
				    contig[contig.length - i - 1] = temp;
				}
				

				// We also need tyo recompute the boundaries
				int start = contig.length - (primer5.length + (randomized_region_end_index-randomized_region_start_index));
				int end = contig.length - primer5.length;
				
				read.selection_cycle.addToSelectionCycle(
						 contig
						 ,start
						 ,end
						 );
				 
				 // Add metadata information
				 addAcceptedNucleotideDistributions(read.selection_cycle, contig, start, end);
				 
			 }
			 
		 	 // Store nucleotide distribution and quality score
		 	 addNuceotideDistributions();
			 addQualityScores();
			 
			 progress.totalAcceptedReads.incrementAndGet();
			 
			 if(previous_return_code != null) {
				 previous_return_code.decrementAndGet();
			 }
			 
		 }else { // Handle errors
			 
			 if (randomized_region_start_index-primer5.length < 0) {
				 
				 progress.totalUnmatchablePrimer5.incrementAndGet();
				 if(previous_return_code != null) {
					 previous_return_code.decrementAndGet();
				 }
				 return progress.totalUnmatchablePrimer5;
				 
			 }
			 else if(randomized_region_end_index+primer3.length > contig.length) {
				 
				 progress.totalUnmatchablePrimer3.incrementAndGet();
				 if(previous_return_code != null) {
					 previous_return_code.decrementAndGet();
				 }
				 return progress.totalUnmatchablePrimer3;
				 
			 }
			 
		 }
		 
		 // if we are here, all went well and we can go home.
		 return null;
		
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

		if (contig == null) {
			
			return false;
			
		}
		
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
	private Result matchPrimer(byte[] c, byte[] primer) { 
		
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
			if (barcode_index3 == barcode_index5 && barcode_index5 != null){
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
				
				if(!forward.containsKey(i)) forward.put(i, new Accumulator());
				forward.get(i).addDataValue(read.forward_quality[i]-33);
				
			}
			
		}
		
		// Reverse read
		if (read.reverse_quality != null) {
		
			ConcurrentHashMap<Integer, Accumulator> reverse = metadata.qualityScoresReverse.get(read.selection_cycle.getName());
			
			// Iterate over the read and add quality scores to the accumulators
			for (int i= 0; i < read.reverse_quality.length; i++) {
				
				if(!reverse.containsKey(i)) reverse.put(i, new Accumulator());
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
				if(!forward.containsKey(i)) {
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
				if(!reverse.containsKey(i)) {
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
		if (!metadata.nucleotideDistributionAccepted.get(sc.getName()).containsKey(randomized_region_size)) {
					
			metadata.nucleotideDistributionAccepted.get(sc.getName()).put(randomized_region_size, new ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>());
			
		}
		
		ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>> accepted = metadata.nucleotideDistributionAccepted.get(sc.getName()).get(randomized_region_size);
		
		int i = 0;
		for (int x=randomized_region_start_index; x<randomized_region_end_index; x++) {
			
			// Make sure the entry exists prior to adding
			if (!accepted.containsKey(i)) {
				
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
