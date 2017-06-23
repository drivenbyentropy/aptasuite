/**
 * 
 */
package aptasuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;

import exceptions.InvalidConfigurationException;
import lib.aptacluster.AptaCluster;
import lib.aptacluster.HashAptaCluster;
import lib.aptacluster.LocalitySensitiveHash;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.MapDBClusterContainer;
import lib.aptamer.datastructures.SelectionCycle;
import lib.export.Export;
import lib.parser.aptaplex.AptaPlexParser;
import lib.parser.aptasim.AptaSimParser;
import lib.structure.capr.CapR;
import lib.structure.capr.CapRFactory;
import lib.structure.capr.CapROriginal;
import lib.structure.capr.InitLoops;
import utilities.AptaLogger;
import utilities.CLIOptions;
import utilities.Configuration;
import lib.aptatrace.AptaTraceMotif;
/**
 * @author Jan Hoinka Implements the command line interface version of
 *         aptasuite.
 */
public class CLI {

	/**
	 * Instance of the current experiment
	 */
	Experiment experiment = null;

	/**
	 * The thread used to control the parser. Running the parser in this thread
	 * allows for nearly real-time estimates of the parsing progress.
	 */
	Thread parserThread = null;
	
	/**
	 * The thread used to control the parser. Running the parser in this thread
	 * allows for nearly real-time estimates of the parsing progress.
	 */
	Thread structureThread = null;	

	public CLI(CommandLine line) {

		// Make sure the parameter for the configuration file is present
		if(!line.hasOption("config")){
			throw new InvalidConfigurationException("No configuration file was specified. Please use the option --config /path/to/configuiration/file.cfg");
		}
		
		// Make sure the configuration file is valid
		Path cfp = Paths.get(line.getOptionValue("config"));

		if (Files.notExists(cfp)) {
				throw new InvalidConfigurationException("The configuration file could not be found at the specified path.");
		}
		
		// Read config file and set defaults
		Configuration.setConfiguration(line.getOptionValue("config"));
		
		AptaLogger.log(Level.INFO, Configuration.class, "Using the following parameters: " + "\n" +  Configuration.printParameters());
		
		// Make sure the project folder exists and create it if not
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		if (Files.notExists(projectPath)){
				AptaLogger.log(Level.INFO, this.getClass(), "The project path does not exist on the file system. Creating folder " + projectPath);
				try {
					Files.createDirectories(Paths.get(projectPath.toString()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		// Case for AptaPLEX, create a database or overwrite an existing one
		if (line.hasOption("parse")){
			
			// clean up old data if required
			try {
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "pooldata").toFile());
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "cycledata").toFile());
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "clusterdata").toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// call data input logic
			createDatabase( line.getOptionValue("config") );
		}
		
		// Case for AptaSIM, create a database or overwrite an existing one
		if (line.hasOption("simulate")){
			
			// clean up old data if required
			try {
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "pooldata").toFile());
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "cycledata").toFile());
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "clusterdata").toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// call data input logic
			runAptaSim( line.getOptionValue("config") );
		}
		
		// Case for Structure Prediction
		if (line.hasOption("structures")){
			
			// clean up old data if required
			try {
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
						
			runStructurePrediction( line.getOptionValue("config") );

		}		
		
		// Case for AptaCluster
		if (line.hasOption("cluster")){

			// clean up old data if required
			try {
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "clusterdata").toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			runAptaCluster( line.getOptionValue("config") );

		}
		
		// Case for AptaTRACE
		if (line.hasOption("trace")){

			runAptaTrace( line.getOptionValue("config") );

		}
		
		// Case for data export
		if (line.hasOption("export")){

			exportData( line.getOptionValue("config"), line.getOptionValue("export") );

		}
		
		//TODO: Remove this for production
		System.out.println("Final Wait");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Implements the logic for calling APTAPlex and for creating a local database 
	 * using the sequencing data specified in the configuration file
	 * @param configFile
	 */
	private void createDatabase(String configFile) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Creating Database");
		
		// Initialize the experiment
		this.experiment = new Experiment(configFile, true);

		AptaLogger.log(Level.INFO, this.getClass(), "Initializing Experiment");
		AptaLogger.log(Level.INFO, this.getClass(), experiment.getSelectionCycleConfiguration());

		// Initialize the parser and run it in a thread
		AptaLogger.log(Level.INFO, this.getClass(), "Initializing parser " + Configuration.getParameters().getString("Parser.backend"));
		AptaPlexParser parser = new AptaPlexParser();

		parserThread = new Thread(parser, "AptaPlex Main");

		AptaLogger.log(Level.INFO, this.getClass(), "Starting Parser:");
		long tParserStart = System.currentTimeMillis();
		parserThread.start();

		// we need to add a shutdown hook for the parserThread in case the
		// user presses ctl-c
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (parserThread != null) {

						parserThread.interrupt();
						parserThread.join();

					}
				} catch (InterruptedException e) {
					AptaLogger.log(Level.SEVERE, this.getClass(), "User interrupt on parserThread");
				}
			}
		});

		// Update user about parsing progress
		

		AptaLogger.log(Level.INFO, this.getClass(), "Parsing...");
		System.out.println( parser.Progress().getHeader() );
		System.out.flush();
		while (parserThread.isAlive() && !parserThread.isInterrupted()) {
			try {
				System.out.print(parser.Progress().getProgress() + "\r");
				// Once every second should suffice
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
			}
		}
		// final update
		System.out.println(parser.Progress().getProgress() + "\r");

		// now that we have the data set any file backed implementations of the
		// pools and cycles to read only
		experiment.getAptamerPool().setReadOnly();
		for (SelectionCycle cycle : experiment.getAllSelectionCycles()) {
			if (cycle != null) {
				cycle.setReadOnly();
			}
		}

		AptaLogger.log(Level.INFO, this.getClass(), String.format("Parsing Completed in %s seconds.\n",
				((System.currentTimeMillis() - tParserStart) / 1000.0)));

		// TODO: print parsing statistics here
		AptaLogger.log(Level.INFO, this.getClass(), "Selection Cycle Statistics");
		for (SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles()) {
			if (cycle != null) {
				AptaLogger.log(Level.INFO, this.getClass(), cycle.toString());
			}
		}

		// clean up
		parserThread = null;
		parser = null;

	}

	
	/**
	 * Implements the logic for calling AptaTrace
	 */
	private void runAptaSim(String configFile){
		
		AptaLogger.log(Level.INFO, this.getClass(), "Creating Database");
		
		// Initialize the experiment
		this.experiment = new Experiment(configFile, true);

		// Initialize the parser and run it in a thread
		AptaLogger.log(Level.INFO, this.getClass(), "Initializing AptaSIM");
		AptaSimParser parser = new AptaSimParser();

		parserThread = new Thread(parser, "AptaSIM Main");

		AptaLogger.log(Level.INFO, this.getClass(), "Starting Simulation:");
		long tParserStart = System.currentTimeMillis();
		parserThread.start();

		// we need to add a shutdown hook for the parserThread in case the
		// user presses ctl-c
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (parserThread != null) {

						parserThread.interrupt();
						parserThread.join();

					}
				} catch (InterruptedException e) {
					AptaLogger.log(Level.SEVERE, this.getClass(), "User interrupt on parserThread");
				}
			}
		});

		// Update progress to user
		while (parserThread.isAlive() && !parserThread.isInterrupted()) {
			try {
				System.out.print(parser.Progress().getProgress() + "\r");
				
				// Once every half second should suffice
				Thread.sleep(100);
			} catch (InterruptedException ie) {
			}
		}
		
		// Print Simulation Statistics
		AptaLogger.log(Level.INFO, this.getClass(), "Simulation Statistics:");
		for (SelectionCycle c : Configuration.getExperiment().getSelectionCycles()){
			AptaLogger.log(Level.INFO, this.getClass(), 
					String.format("%-20s: Total Pool Size: %s\t Unique Pool Size: %s\t Diversity: %.4f %%", 
							c.getName(), 
							c.getSize(), 
							c.getUniqueSize(), 
							(c.getUniqueSize()/ new Double (c.getSize()) * 100 ) ));
		}
		
		
		// now that we have the data set any file backed implementations of the
		// pools and cycles to read only
		experiment.getAptamerPool().setReadOnly();
		for (SelectionCycle cycle : experiment.getAllSelectionCycles()) {
			if (cycle != null) {
				cycle.setReadOnly();
			}
		}
		
		// clean up
		parserThread = null;
		parser = null;
		
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Simulation completed in %s seconds.\n",
				((System.currentTimeMillis() - tParserStart) / 1000.0)));
	}
	
	
	private void runStructurePrediction(String configFile){
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting Structure Predition");
		
		// Make sure we have prior data or load it from disk
		if (experiment == null) {
			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
			this.experiment = new Experiment(configFile, false);
		}
		else{
			AptaLogger.log(Level.INFO, this.getClass(), "Using existing sequencing data");
		}
		
		// Create a new instance of the StructurePool
		experiment.instantiateStructurePool(true);
		
		// Start parallel processing of structure prediction
		CapRFactory caprf = new CapRFactory(experiment.getAptamerPool().iterator());
		
		structureThread = new Thread(caprf);

		AptaLogger.log(Level.INFO, this.getClass(), "Starting Structure Prediction using " + Configuration.getParameters().getInt("Performance.maxNumberOfCores") + " threads:");
		long tParserStart = System.currentTimeMillis();
		structureThread.start();

		// we need to add a shutdown hook for the CapRFactory in case the
		// user presses ctl-c
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (structureThread != null) {

						structureThread.interrupt();
						structureThread.join();

					}
				} catch (InterruptedException e) {
					AptaLogger.log(Level.SEVERE, this.getClass(), "User interrupt on structureTread");
				}
			}
		});

		AptaLogger.log(Level.INFO, this.getClass(), "Predicting...");

		long sps = 0;
		while (structureThread.isAlive() && !structureThread.isInterrupted()) {
			try {
				long current_progress = caprf.getProgress().longValue();
				long eta = (experiment.getAptamerPool().size()-current_progress)/(current_progress-sps+1);
				System.out.print(String.format("Completed: %s/%s (%s structures per second  ETA:%s)     " + "\r", current_progress, experiment.getAptamerPool().size(), current_progress-sps, String.format("%02d:%02d:%02d", eta / 3600, (eta % 3600) / 60, eta % 60)));
				sps = current_progress;
				
				// Once every second should suffice
				Thread.sleep(1000);
				
			} catch (InterruptedException ie) {
			}
		}
		// final update
		System.out.print(        String.format("Completed: %s/%s                                            ", caprf.getProgress(), experiment.getAptamerPool().size()));

		AptaLogger.log(Level.INFO, this.getClass(), String.format("Structure prediction completed in %s seconds.\n",
				((System.currentTimeMillis() - tParserStart) / 1000.0)));
		
	
		// now that we have the data, set any file backed implementations 
		// of StructurePool to read only
		experiment.getStructurePool().setReadOnly();
	}
	

	/**
	 * Implements the logic for calling AptaTrace
	 */
	private void runAptaCluster(String configFile) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaCluster");
		
		// Make sure we have data prior or load it from disk
		if (experiment == null) {
			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
			this.experiment = new Experiment(configFile, false);
		}
		else{
			AptaLogger.log(Level.INFO, this.getClass(), "Using existing data");
		}
		
		// Create a new instance of the ClusterContainer
		experiment.instantiateClusterContainer(true);

		// Create AptaCluster instance
		AptaCluster ac = new HashAptaCluster(
				Configuration.getParameters().getInt("Aptacluster.RandomizedRegionSize"),
				Configuration.getParameters().getInt("Aptacluster.LSHDimension"),
				Configuration.getParameters().getInt("Aptacluster.LSHIterations"),
				Configuration.getParameters().getInt("Aptacluster.EditDistance"),
				Configuration.getParameters().getInt("Aptacluster.KmerSize"),
				Configuration.getParameters().getInt("Aptacluster.KmerCutoffIterations"),
				experiment
				);
		
		// Run
		ac.performLSH();
		
		AptaLogger.log(Level.INFO, this.getClass(), "AptaCluster Comleted");
		
	}	
	
	
	/**
	 * Implements the logic for calling AptaTrace
	 */
	private void runAptaTrace(String configFile) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaTRACE");
		
		// Make sure we have data prior or load it from disk
		if (experiment == null) {
			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
			this.experiment = new Experiment(configFile, false);
		}
		else{
			AptaLogger.log(Level.INFO, this.getClass(), "Using existing data");
		}
		
		// Get the instance of the StructurePool
		if (experiment.getStructurePool() == null)
		{
			experiment.instantiateStructurePool(false);
		}	
		
		AptaTraceMotif motifFinder=new AptaTraceMotif(experiment); //TODO: SEPARATE INTO CONSTRUCTOR AND RUN LOGIC
		
	}
	
	/**
	 * Implements the logic concerning the export of the data to text files as
	 * specified in the configuration
	 */
	private void exportData( String configFile, String items ){
		
		
		
		// Make sure we have prior data or load it from disk
		if (experiment == null) {
			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
			this.experiment = new Experiment(configFile, false);
		}
		else{
			AptaLogger.log(Level.INFO, this.getClass(), "Using existing sequencing data");
		}
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting Data Export");
		
		// Make sure the export folder exists and create it if not
		Path exportPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "export");
		if (Files.notExists(exportPath)){
				AptaLogger.log(Level.INFO, this.getClass(), "The export path does not exist on the file system. Creating folder " + exportPath);
				try {
					Files.createDirectories(Paths.get(exportPath.toString()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		Export export = new Export();
		Boolean compress = Configuration.getParameters().getBoolean("Export.compress");
		String extension = Configuration.getParameters().getString("Export.SequenceFormat") + (compress ? ".gz" : "");
		
		// Get the tokens we need to export
		String[] tokens = items.split(",");
		
		// Handle the different cases for each token
		for (String token : tokens){
			
			switch(token){
				case "pool":
					Path poolexportpath = Paths.get(exportPath.toString(), "pool." + extension);
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting pool data to file " + poolexportpath.toString());
					export.Pool(Configuration.getExperiment().getAptamerPool(), poolexportpath);
					break;
					
				case "cycles":
					
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting selection cycle data");
					
					// Determine which cycles need to be exported
					ArrayList<SelectionCycle> cycles_to_export = null;
					if (Configuration.getParameters().getStringArray("Export.Cycles").length == 0){
						cycles_to_export = Configuration.getExperiment().getAllSelectionCycles();
					}
					else{
						cycles_to_export = new ArrayList<SelectionCycle>();
						for (String cycle_id : Configuration.getParameters().getStringArray("Export.Cycles")){
							SelectionCycle sc = Configuration.getExperiment().getSelectionCycleById(cycle_id);
							
							if(sc == null){ //Something went wrong here
								AptaLogger.log(Level.SEVERE, this.getClass(), "Could not find cycle with id " + cycle_id + " for export. Exiting.");
								throw new InvalidConfigurationException("Could not find cycle with id " + cycle_id + " for export.");
							}
							
							cycles_to_export.add(sc);
						}
						
						// Now run the export
						for (SelectionCycle sc : cycles_to_export){
							Path cycleexportpath = Paths.get(exportPath.toString(), sc.getName() + "." + extension);
							AptaLogger.log(Level.INFO, this.getClass(), "Exporting selection cycle " + sc.getName() + " to file " + cycleexportpath.toString());
							export.Cycle(sc, cycleexportpath );
						}
						
					}
					break;
					
				case "clusters":
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting clusters");
					
					// Get the instance of the StructurePool
					if (experiment.getClusterContainer() == null)
					{
						experiment.instantiateClusterContainer(false);
					}	
					
					// Determine which cycles need to be exported
					cycles_to_export = null;
					if (Configuration.getParameters().getStringArray("Export.Cycles").length == 0){
						cycles_to_export = Configuration.getExperiment().getAllSelectionCycles();
					}
					else{
						cycles_to_export = new ArrayList<SelectionCycle>();
						for (String cycle_id : Configuration.getParameters().getStringArray("Export.Cycles")){
							SelectionCycle sc = Configuration.getExperiment().getSelectionCycleById(cycle_id);
							
							if(sc == null){ //Something went wrong here
								AptaLogger.log(Level.SEVERE, this.getClass(), "Could not find cycle with id " + cycle_id + " for export. Exiting.");
								throw new InvalidConfigurationException("Could not find cycle with id " + cycle_id + " for export.");
							}
							
							cycles_to_export.add(sc);
						}
						
						// Now run the export
						for (SelectionCycle sc : cycles_to_export){
							Path cycleexportpath = Paths.get(exportPath.toString(), "clusters_" + sc.getName() + "." + extension);
							AptaLogger.log(Level.INFO, this.getClass(), "Exporting clusters of selection cycle " + sc.getName() + " to file " + cycleexportpath.toString());
							export.Clusters(sc, experiment.getClusterContainer(), cycleexportpath );
						}
						
					}
					break;
					
				case "structures":
					
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting structure data");
					
					//Make sure we have structures
					if (experiment.getStructurePool() == null)
					{
						experiment.instantiateStructurePool(false);
					}	
					
					export.Structures(Configuration.getExperiment().getStructurePool(), Paths.get(exportPath.toString(), "structures.txt" + (compress ? ".gz" : "")));
					break;
					
				default:
					AptaLogger.log(Level.SEVERE, this.getClass(), "Export option " + token + " not recognized. Exiting.");
					throw new InvalidConfigurationException("Export option " + token + " not recognized.");
			}
			
		}
		
	}
	
}
