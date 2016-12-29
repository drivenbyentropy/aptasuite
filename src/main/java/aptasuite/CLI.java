/**
 * 
 */
package aptasuite;


import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.aptaplex.AptaplexParser;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Implements the command line interface version of aptasuite.
 */
public class CLI {

	/**
	 * Instance of the current experiment
	 */
	Experiment experiment = null; 
	
	
	/**
	 * The thread used to control the parser. Running the 
	 * parser in this thread allows for nearly read-time
	 * estimates of the parsing progress.
	 */
	Thread parserThread = null;
			
	public CLI(String configFile){
		
		// Initialize the experiment
		this.experiment = new Experiment(configFile);
		
		System.out.println("Initializing Experiment");
		System.out.println(experiment.getSelectionCycleConfiguration());
		
		// Initialize the parser and run it in a thread
		System.out.println("Initializing parser " + Configuration.getParameters().getString("Parser.backend"));
		AptaplexParser parser = new AptaplexParser();
		
		parserThread = new Thread (parser);
		
		System.out.println("Starting Parser:");
		long tParserStart = System.currentTimeMillis();
		parserThread.start();
		
		// we need to add a shutdown hook for the parserThread in case the 
		// user presses ctl-c 
		Runtime.getRuntime().addShutdownHook(new Thread() {
	         @Override
	         public void run() {
	            try {
	            	if(parserThread != null){
	            		
	            		parserThread.interrupt();
	            		parserThread.join();
	            	
	            	}
	            } catch (InterruptedException e) {
	            }
	         }
	      });
		
		// Update user about parsing progress
		String spacing = "%1$-23s %2$-23s %3$-23s %4$-23s %5$-23s %6$-23s %7$-23s %8$-23s";
		
		System.out.println("Parsing...");
		System.out.println(String.format(spacing,
				"Total Reads:", 
				"Accepted Reads:",
				"Contig Assembly Fails:",
				"Invalid Alphabet:",
				"5' Primer Error:",
				"3' Primer Error:",
				"Invalid Cycle",
				"Total Primer Overlaps:"
				) );
		System.out.flush();
		while(parserThread.isAlive() && !parserThread.isInterrupted()){
			// Once every second should suffice
			try {
				System.out.print(String.format(spacing+"\r",  
						parser.getProgress().totalProcessedReads.get(), 
						parser.getProgress().totalAcceptedReads.get(),
						parser.getProgress().totalContigAssemblyFails.get(),
						parser.getProgress().totalInvalidContigs.get(),
						parser.getProgress().totalUnmatchablePrimer5.get(),
						parser.getProgress().totalUnmatchablePrimer3.get(),
						parser.getProgress().totalInvalidCycle.get(),
						parser.getProgress().totalPrimerOverlaps.get()
						) );
		        Thread.sleep(1000);
		    } catch(InterruptedException ie) {}
		}
		// final update
		System.out.println(String.format(spacing+"\r",  
				parser.getProgress().totalProcessedReads.get(), 
				parser.getProgress().totalAcceptedReads.get(),
				parser.getProgress().totalContigAssemblyFails.get(),
				parser.getProgress().totalInvalidContigs.get(),
				parser.getProgress().totalUnmatchablePrimer5.get(),
				parser.getProgress().totalUnmatchablePrimer3.get(),
				parser.getProgress().totalInvalidCycle.get(),
				parser.getProgress().totalPrimerOverlaps.get()
				) );
		
		// now that we have the data set any file backed implementations of the pools and cycles to read only
		experiment.getAptamerPool().setReadOnly();
		for (SelectionCycle cycle : experiment.getAllSelectionCycles()){
			if (cycle != null){
				cycle.setReadOnly();
			}
		}
		
		
		System.out.println(String.format("Parsing Completed in %s seconds.\n", ((System.currentTimeMillis() - tParserStart) / 1000.0)));
		
		//TODO: print parsing statistics here
		System.out.println("Selection Cycle Statistics");
		for (SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles()){
			if (cycle != null){
				System.out.println(cycle);
			}
		}
		
		//clean up
		parserThread = null;
		parser = null;
		
		System.out.println("Final Wait");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
