/**
 * 
 */
package lib.parser.aptasim;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import lib.parser.ParserProgress;

/**
 * @author Jan Hoinka
 * This class tracks the progress of AptaSIM in a multi-threaded
 * environment according to the ParserProgress API
 */
public class AptaSimProgress implements ParserProgress{

	/**
	 * Spacing for formating purposes
	 */
	private String spacing = null;
	private String spacing_init_pool_stage     = "%1$-23s %2$-23s";
	private String spacing_selection_stage     = "%1$-23s %2$-23s %3$-23s";
	private String spacing_amplification_stage = "%1$-23s %2$-23s %3$-23s %4$-23s %5$-23s";
	private String spacing_training_stage      = "%1$-23s";
	
	/**
	 * 0 = training
	 * 1 = init pool
	 * 2 = selection stage
	 * 3 = amplification stage
	 */
	public int stage = 0;
	
	/**
	 * Header portion of the progress report
	 */
	private String header = null;
	
	/**
	 * The round currently being simulated
	 */
	public Integer round = null;
	
	/**
	 * Progress portion of the report
	 */
	public AtomicInteger totalProcessedReads = new AtomicInteger();
	public AtomicInteger totalAcceptedReads = new AtomicInteger();
	public AtomicInteger totalDiscardedReads = new AtomicInteger();
	public AtomicInteger totalMutatedReads = new AtomicInteger();
	public AtomicInteger totalSampledReads = new AtomicInteger();
	public AtomicInteger totalPoolSize = new AtomicInteger();
	
	public AptaSimProgress(){
		initialPoolStage(0);
	}
	
	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public String getProgress() {
		String progress = null;
		
		if (stage == 0){
			progress = header + String.format(spacing, "Processed: " + totalProcessedReads) + "\r";
		}
		if (stage == 1){
			progress = header + String.format(spacing, "Pool Size: " + totalProcessedReads, "Unique Pool Size: " + totalPoolSize) + "\r";
		}
		if (stage == 2){
			progress = header + String.format(spacing, ("Processed: " + totalProcessedReads), ("Selected: " + totalSampledReads), ("Discarded: " + totalDiscardedReads)) + "\r";
		}
		if (stage == 3){
			progress = header + String.format(spacing, ("Processed: " + totalProcessedReads), ("Selected: " + totalSampledReads), ("Discarded: " + totalDiscardedReads), ("Mutated: " + totalMutatedReads), ("Pool Size: " + totalPoolSize)) + "\r";
		}
		
		return progress;
	}

	public void trainingStage(String filename){
		stage = 0;
		spacing = spacing_training_stage;
		header = "Training model with data from file " + Paths.get(filename).getFileName().toString() + ": ";
	}
	
	public void initialPoolStage(int round){
		stage = 1;
		this.round = round;
		spacing = spacing_init_pool_stage;
		header = "Generating inital pool:  ";
	}
	
	public void selectionStage(int round){
		stage = 2;
		this.round = round;
		spacing = spacing_selection_stage;
		header = "Simulating SELEX for round " + this.round + ": ";
	}
	
	public void amplificationStage(int round){
		stage = 3;
		this.round = round;
		spacing = spacing_amplification_stage;
		header = "Simulating PCR   for round " + this.round + ": ";
	}
	
	/**
	 * Prepare the progress for the next round
	 */
	public void reset(){
		
		totalProcessedReads.set(0);
		totalAcceptedReads.set(0);
		totalDiscardedReads.set(0);
		totalMutatedReads.set(0);
		totalSampledReads.set(0);
		totalPoolSize.set(0);
		
	}
	
}
