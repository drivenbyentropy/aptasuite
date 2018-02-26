/**
 * 
 */
package lib.parser.aptaplex;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import lib.aptamer.datastructures.Metadata;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.Parser;
import lib.parser.ParserProgress;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka Java implementation of AptaPlex as described in Hoinka et
 *         al. 2016, Methods This class controls the producer-consumer pattern
 *         implemented in lib.parser.aptaplex
 */
public class AptaPlexParser implements Parser, Runnable{

	/**
	 * The progress of the parser instance. Writable to the consumers and thread-safe
	 */
	private static AptaPlexProgress progress;
	
	public AptaPlexParser() {
		
		progress = new AptaPlexProgress();
		
	}
	
	@Override
	public void parse() {

		// Creating shared object
		BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(Configuration.getParameters().getInt("AptaplexParser.BlockingQueueSize"));																// this

		// We need to know how many threads we can use on the system
		int num_threads = Math.min( Runtime.getRuntime().availableProcessors(), Configuration.getParameters().getInt("Performance.maxNumberOfCores"));
		
		// Creating Producer and Consumer Threads using the ExecutorService to manage them
		ExecutorService es = Executors.newCachedThreadPool();
		es.execute(new Thread(new AptaPlexProducer(sharedQueue), "AptaPlex Producer"));
		es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress), "AptaPlex Consumer 1"));
		
		for (int x=1; x<num_threads-1; x++){
			es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress), "AptaPlex Consumer " + (x+1)));
		}
		
		// Make sure threads are GCed once completed
		es.shutdown();
		
		// Wait until all threads are done
		try {
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); //wait forever
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void parsingCompleted() {

		// Now that we have the data set any pools and cycles to read only
		Configuration.getExperiment().getAptamerPool().setReadOnly();
		for (SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles()) {
			if (cycle != null) {
				cycle.setReadOnly();
			}
		}
		
		// Store the final progress data to the metadata statistics
		Metadata metadata = Configuration.getExperiment().getMetadata();
		
		metadata.parserStatistics.put("processed_reads", progress.totalProcessedReads.get());
		metadata.parserStatistics.put("accepted_reads", progress.totalAcceptedReads.get());
		metadata.parserStatistics.put("contig_assembly_fails", progress.totalContigAssemblyFails.get());
		metadata.parserStatistics.put("invalid_alphabet", progress.totalInvalidContigs.get());
		metadata.parserStatistics.put("5_prime_error", progress.totalUnmatchablePrimer5.get());
		metadata.parserStatistics.put("3_prime_error", progress.totalUnmatchablePrimer3.get());
		metadata.parserStatistics.put("invalid_cycle", progress.totalInvalidCycle.get());
		metadata.parserStatistics.put("total_primer_overlaps", progress.totalPrimerOverlaps.get());
		
		// Finally, store the metadata to disk
		metadata.saveDataToFile();
		
		AptaLogger.log(Level.INFO, this.getClass(), "Parsing Completed, Data storage set to read-only and metadata written to file");
	}

	@Override
	public ParserProgress Progress() {
		return progress;
	}

	@Override
	public void run() {
		
		parse();
		
		parsingCompleted();
		
	}

}
