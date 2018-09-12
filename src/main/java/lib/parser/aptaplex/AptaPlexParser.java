/**
 * 
 */
package lib.parser.aptaplex;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import lib.aptamer.datastructures.Metadata;
import lib.aptamer.datastructures.SelectionCycle;
import lib.export.CompressedExportWriter;
import lib.export.ExportWriter;
import lib.export.UncompressedExportWriter;
import lib.parser.Parser;
import lib.parser.ParserProgress;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Pair;

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
	
	/**
	 * Instance of the exporter that will be passed to all consumer threads
	 * Pair.first = forward lane, Pair.second = reverse lane;
	 */
	private Map<Path, Pair<ExportWriter,ExportWriter>> undeterminedExportWriterMap = null;
	
	public AptaPlexParser() {
		
		progress = new AptaPlexProgress();
		
	}
	
	@Override
	public void parse() {

		// Creating shared object
		BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(Configuration.getParameters().getInt("AptaplexParser.BlockingQueueSize"));																// this

		// We need to know how many threads we can use on the system
		int num_threads = Math.min( Runtime.getRuntime().availableProcessors(), Configuration.getParameters().getInt("Performance.maxNumberOfCores"));
		
		// We need to pass a write if failed reads are to be written to file
		if (Configuration.getParameters().getBoolean("AptaplexParser.UndeterminedToFile")) {
			
			// we need to make sure the export folder is available
			Path directory = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "export");
		    if (! directory.toFile().exists()){
		        directory.toFile().mkdir();
		        // If you require it to make the entire directory path including parents,
		        // use directory.mkdirs(); here instead.
		    }
			
			undeterminedExportWriterMap = new HashMap<Path, Pair<ExportWriter,ExportWriter>>();
			
		}
		
		// Creating Producer and Consumer Threads using the ExecutorService to manage them
		ExecutorService es = Executors.newCachedThreadPool();
		es.execute(new Thread(new AptaPlexProducer(sharedQueue, undeterminedExportWriterMap), "AptaPlex Producer"));
		es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress, undeterminedExportWriterMap), "AptaPlex Consumer 1"));
		
		for (int x=1; x<num_threads-1; x++){
			es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress, undeterminedExportWriterMap), "AptaPlex Consumer " + (x+1)));
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
		
		// At this point we have to close all file handles in the undetermined lanes
		if (undeterminedExportWriterMap != null) {
			
			for ( Entry<Path, Pair<ExportWriter, ExportWriter>> entry : this.undeterminedExportWriterMap.entrySet()) {
				 
				entry.getValue().first.close();
				if (entry.getValue().second != null) {
					
					entry.getValue().second.close();
					
				}
				
			}
			
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
