/**
 * 
 */
package aptasuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.parallelism.ParallelWrapper;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import exceptions.InformationNotFoundException;
import exceptions.InvalidConfigurationException;
import lib.aptacluster.AptaCluster;
import lib.aptacluster.HashAptaCluster;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.aptanet.DataInputIterator;
import lib.aptanet.DataType;
import lib.aptanet.Inequality;
import lib.aptanet.PlayGround;
import lib.aptanet.ScoreMethod;
import lib.aptanet.SelectionCycleSplitDataInputIterator;
import lib.aptanet.SequenceStructureDatasetIterator;
import lib.export.Export;
import lib.parser.aptaplex.AptaPlexParser;
import lib.parser.aptasim.AptaSimParser;
import lib.structure.capr.CapRFactory;
import lib.structure.rnafold.RNAFoldFactory;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Quicksort;
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
		
		System.out.println( Configuration.getParameters().getString("Experiment.projectPath") );
		
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
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "bppmdata").toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// reference publication to cite
			AptaLogger.log(Level.INFO, this.getClass(), "If you use this software in your research, please cite AptaPLEX as "
					+ "Hoinka, J., & Przytycka, T. (2016). "
					+ "AptaPLEX - A dedicated, multithreaded demultiplexer for HT-SELEX data. "
					+ "Methods. http://doi.org/10.1016/j.ymeth.2016.04.011"
					+ "");
			
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
				FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "bppmdata").toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// reference publication to cite
			AptaLogger.log(Level.INFO, this.getClass(), "If you use this software in your research, please cite AptaSIM as \n"
					+ "Hoinka, J., Berezhnoy, A., Dao, P., Sauna, Z. E., Gilboa, E., & Przytycka, T. M. (2015). \n"
					+ "Large scale analysis of the mutational landscape in HT-SELEX improves aptamer discovery. \n"
					+ "Nucleic Acids Research, 43(12), 5699–5707. http://doi.org/10.1093/nar/gkv308"
					+ "");
			
			// call data input logic
			runAptaSim( line.getOptionValue("config") );
		}
		
		// Case for Structure Prediction
		if (line.hasOption("predict")){
			
			// differentiable between the different cases and make sure the options are valid
			String[] prediction_options = line.getOptionValue("predict").split(",");
			
			// we need at least one option
			if(prediction_options.length == 0) {
				
				AptaLogger.log(Level.SEVERE, this.getClass(), "Predict option require at least on argument (allowed values: structure,bppm). Exiting.");
				throw new InvalidConfigurationException("Predict option require at least on argument (allowed values: structure,bppm)");
				
			}
			
			for (String option : prediction_options) {
				
				if (!option.equals("structure") && !option.equals("bppm")) {
				
					AptaLogger.log(Level.SEVERE, this.getClass(), "Predict option " + option + " not recognized. Exiting.");
					throw new InvalidConfigurationException("Predict option " + option + " not recognized.");
					
				}
				
			}
			
			// now call the corresponding routine
			for (String option : prediction_options) {
				
				// CapR
				if (option.equals("structure")) {
					// clean up old data if required
					try {
						FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					runStructurePrediction( line.getOptionValue("config") );
				}
			
				// RNAFold -p
				if (option.equals("bppm")) {
					// clean up old data if required
					try {
						FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "bppmdata").toFile());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					runBppmPrediction( line.getOptionValue("config") );
				}
			}
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
			
			// reference publication to cite
			AptaLogger.log(Level.INFO, this.getClass(), "If you use this software in your research, please cite AptaCLUSTER as \n"
					+ "Hoinka, J., Berezhnoy, A., Sauna, Z. E., Gilboa, E., & Przytycka, T. M. (2014). \n"
					+ "AptaCluster - A method to cluster HT-SELEX aptamer pools and lessons from its application. \n"
					+ "In Lecture Notes in Computer Science  (Vol. 8394 LNBI, pp. 115–128). http://doi.org/10.1007/978-3-319-05269-4_9"
					+ "");
			
			runAptaCluster( line.getOptionValue("config") );

		}
		
		// Case for AptaTRACE
		if (line.hasOption("trace")){

			// reference publication to cite
			AptaLogger.log(Level.INFO, this.getClass(), "If you use this software in your research, please cite AptaTRACE as "
					+ "Dao, P., Hoinka, J., Takahashi, M., Zhou, J., Ho, M., Wang, Y., Costa, F., Rossi, J. J., Backofen, R., Burnett, J., Przytycka, T. M. (2016). "
					+ "AptaTRACE Elucidates RNA Sequence-Structure Motifs from Selection Trends in HT-SELEX Experiments. "
					+ "Cell Systems, 3(1), 62–70. http://doi.org/10.1016/j.cels.2016.07.003"
					+ "");
			
			runAptaTrace( line.getOptionValue("config") );

		}
		
		// Case for AptaNET
		if (line.hasOption("net")){

			// reference publication to cite
			AptaLogger.log(Level.INFO, this.getClass(), "If you use this software in your research, please cite AptaNET as "
					+ "");
			
			//runAptaNET( line.getOptionValue("config") );
			new PlayGround(line);

		}
		
		// Case for data export
		if (line.hasOption("export")){

			exportData( line.getOptionValue("config"), line.getOptionValue("export") );

		}
		
		//TODO: Remove this for production
//		System.out.println("Final Wait");
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
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

		AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaPlex:");
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

//		// now that we have the data set any file backed implementations of the
//		// pools and cycles to read only
//		experiment.getAptamerPool().setReadOnly();
//		for (SelectionCycle cycle : experiment.getAllSelectionCycles()) {
//			if (cycle != null) {
//				cycle.setReadOnly();
//			}
//		}

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
		experiment.instantiateStructurePool(true, true);
		
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
	 * Implements the logic for predicting and storing the base pair probability
	 * matrices of the pool members
	 * @param configFile
	 */
	private void runBppmPrediction(String configFile){
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting Prediction of Base Pair Probabilities");
		
		// Make sure we have prior data or load it from disk
		if (experiment == null) {
			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
			this.experiment = new Experiment(configFile, false);
		}
		else{
			AptaLogger.log(Level.INFO, this.getClass(), "Using existing sequencing data");
		}
		
		// Create a new instance of the StructurePool
		experiment.instantiateBppmPool(true);
		
		// Start parallel processing of structure prediction
		RNAFoldFactory rnaff = new RNAFoldFactory(experiment.getAptamerPool().iterator());
		
		structureThread = new Thread(rnaff);

		AptaLogger.log(Level.INFO, this.getClass(), "Starting Prediction of Base Pair Probabilities using " + Configuration.getParameters().getInt("Performance.maxNumberOfCores") + " threads:");
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
				long current_progress = rnaff.getProgress().longValue();
				long eta = (experiment.getAptamerPool().size()-current_progress)/(current_progress-sps+1);
				System.out.print(String.format("Completed: %s/%s (%s predictions per second  ETA:%s)     " + "\r", current_progress, experiment.getAptamerPool().size(), current_progress-sps, String.format("%02d:%02d:%02d", eta / 3600, (eta % 3600) / 60, eta % 60)));
				sps = current_progress;
				
				// Once every second should suffice
				Thread.sleep(1000);
				
			} catch (InterruptedException ie) {
			}
		}
		// final update
		System.out.print(        String.format("Completed: %s/%s                                            ", rnaff.getProgress(), experiment.getAptamerPool().size()));

		AptaLogger.log(Level.INFO, this.getClass(), String.format("Structure prediction completed in %s seconds.\n",
				((System.currentTimeMillis() - tParserStart) / 1000.0)));
		
	
		// now that we have the data, set any file backed implementations 
		// of StructurePool to read only
		experiment.getBppmPool().setReadOnly();
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
		experiment.instantiateClusterContainer(true, true);

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
			experiment.instantiateStructurePool(false, true);
		}	
		
		AptaTraceMotif aptatrace =new AptaTraceMotif(
				experiment,
				Configuration.getParameters().getString("Experiment.projectPath"),
				Configuration.getParameters().getInt("Aptatrace.KmerLength"),
				Configuration.getParameters().getBoolean("Aptatrace.FilterClusters"),
				Configuration.getParameters().getBoolean("Aptatrace.OutputClusters"),
				Configuration.getParameters().getInt("Aptatrace.Alpha")
				
				); 
		
		aptatrace.run();
	}
	
	/**
	 * Implements the logic for calling AptaGAN
	 */
	private void runAptaNET(String configFile) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaNET");
		
		// Make sure we have data prior or load it from disk
		if (experiment == null) {
			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
			this.experiment = new Experiment(configFile, false);
		}
		else{
			AptaLogger.log(Level.INFO, this.getClass(), "Using existing data");
		}
		
		// Get the instance of the BPPM StructurePool
		if (experiment.getBppmPool() == null)
		{
			experiment.instantiateBppmPool(false);
		}	
		
		// Hardware tuning
//		CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true).allowCrossDeviceAccess(true);
//		Nd4j.getMemoryManager().setAutoGcWindow(1000); // Garbage collection every XXX ms 
//		long max_chache_in_gb = 2L;
//		CudaEnvironment.getInstance().getConfiguration()
//	    .setMaximumDeviceCacheableLength(1024 * 1024 * 1024L)
//	    .setMaximumDeviceCache(max_chache_in_gb * 1024 * 1024 * 1024L)
//	    .setMaximumHostCacheableLength(1024 * 1024 * 1024L)
//	    .setMaximumHostCache(max_chache_in_gb * 1024 * 1024 * 1024L);
		
		System.out.println("Load Data");
		
		// Create a mask so that we balance positive and negative examples
		SelectionCycle sc = experiment.getSelectionCycleById("R12");
		BitSet mask = new BitSet(sc.getUniqueSize());
		int[] indices = new int[sc.getUniqueSize()];
		int[] counts = new int[sc.getUniqueSize()];
		
		int counter = 0;
		for ( Entry<Integer, Integer> item : sc.iterator()) {
			
			indices[counter] = item.getKey();
			counts[counter] = item.getValue();
			counter++;
			
		}
		Quicksort.sort(indices, counts);
		
		//Take the top and bottom pv percent of the data
		double pv = 0.15;
		int percentage = new Double(sc.getUniqueSize() * pv).intValue();
		System.out.println("\nBottom:");
		for (int x=0; x<percentage; x++) { mask.flip(indices[x]); if (x==0 || x==percentage-1) System.out.print(" " + counts[x]); }
		System.out.println("\nTop:");
		for (int x=sc.getUniqueSize()-1; x>sc.getUniqueSize()-percentage; x--) { mask.flip(indices[x]); if (x-1 == sc.getUniqueSize()-percentage || x==sc.getUniqueSize()-1) System.out.print(" " + counts[x]);}
		
		
		// Create a dataiterator based on a single selection cycle
		// using the raw count as the score, only accepting sequences 
		// with a count >= 10, splitting the dataset 80% Train 20% test
		DataInputIterator scsdii = new SelectionCycleSplitDataInputIterator(
				sc, 
				ScoreMethod.COUNT, 
				1 , 
				Inequality.GREATER, 
				0.8,
				mask);
		
		// Create a DataSetIterator for training data based on scsdii
		DataSetIterator train_it = new SequenceStructureDatasetIterator(scsdii, DataType.TRAIN, true);
		((SequenceStructureDatasetIterator)train_it).setBatchsize(32);
		//((SequenceStructureDatasetIterator)train_it).setWithNoise(true);
		//((SequenceStructureDatasetIterator)train_it).setNoisePercentage(0.05);
		
		// Do the same thing for testing
		DataSetIterator test_it = new SequenceStructureDatasetIterator(scsdii, DataType.TEST, true);

		
		//Build model
		int nChannels = 17; // Number of input channels
        int outputNum = 1; // The number of possible outcomes, one for regression
        
        
        // An epoch is defined as a full pass of the data set. 
        // An iteration in DL4J is defined as the number of parameter 
        // updates in a row, for each minibatch.
        
        // Generally, you want to use multiple epochs and one iteration 
        // (.iterations(1) option) when training; multiple iterations are generally 
        // only used when doing full-batch training on very small data sets.
        //Too few epochs don’t give your network enough time to learn good parameters; 
        //too many and you might overfit the training data. One way to choose the number 
        //of epochs is to use early stopping. Early stopping can also help to prevent 
        //the neural network from overfitting (i.e., can help the net generalize better to unseen data).
        int nEpochs = 20; // Number of training epochs
        int iterations = 1; // Number of training iterations
        int seed = 123; //


        /*
            Construct the neural network
         */
        System.out.println("Build model....");

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations) // Training iterations as above
                .regularization(true).l2(0.005)
                .dropOut(0.5)
                .learningRate(.001).biasLearningRate(0.02)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.ADAGRAD)
                .weightInit(WeightInit.RELU)
                .list()
                //First two layer: Convolution on Base Pairs
                .layer(0, new ConvolutionLayer.Builder()
                		.name("Convolution on Base Pairs 1")
                        // nIn and nOut specify depth. nIn here is the nChannels and 
                		// nOut is the number of filters to be applied
                        .nIn(nChannels)
                        .stride(1, 1)
                        .nOut(64) // We will gradually increase the depth while reducing width and height
                        .kernelSize(1,1)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new ConvolutionLayer.Builder()
                		.name("Convolution on Base Pairs 2")
                        // nIn and nOut specify depth. nIn here is the nChannels and 
                		// nOut is the number of filters to be applied
                        .stride(1, 1)
                        .nOut(64) // We will gradually increase the depth while reducing width and height
                        .kernelSize(1,1)
                        .activation(Activation.RELU)
                        .build())
                // Now perform a max pool, this reduces spacial dimensions
                .layer(2, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2,2)
                        .stride(2,2)
                        .build())
                // Capture higher order correlations such as tacks
                .layer(3, new ConvolutionLayer.Builder()
                		.name("Convolution on higher order features 1")
                        //Note that nIn need not be specified in later layers
                		.kernelSize(4,4)
                		.stride(1, 1)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(4, new ConvolutionLayer.Builder()
                		.name("Convolution on higher order features 2")
                        //Note that nIn need not be specified in later layers
                		.kernelSize(2,2)
                		.stride(1, 1)
                        .nOut(256)
                        .activation(Activation.RELU)
                        .build())
                // Pool these features
                .layer(5, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2,2)
                        .stride(2,2)
                        .build())
                // Perform regression on the selected features
                .layer(6, new DenseLayer.Builder().activation(Activation.RELU)
                		.name("Fully Connected Layer 1")
                        .nOut(1024).build())
                .layer(7, new DenseLayer.Builder().activation(Activation.RELU)
                		.name("Fully Connected Layer 2")
                        .nOut(1024).build())
                .layer(8, new DenseLayer.Builder().activation(Activation.SOFTMAX)
                		.name("Soft Max")
                        .nOut(150).build())
                .layer(9, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1) //one vector (scalar in this case) of one real value
                        .activation(Activation.IDENTITY)
                        .build())
                // 42 is randomized region size
                .setInputType(InputType.convolutional(42,42,17)) //See note below 
                .backprop(true).pretrain(false).build();        
        
        
//        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
//                .seed(seed)
//                .iterations(iterations) // Training iterations as above
//                .regularization(true).l2(0.005)
//                .dropOut(0.5)
//                .learningRate(.005).biasLearningRate(0.02)
//                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
//                .updater(Updater.ADAGRAD)
//                .weightInit(WeightInit.RELU)
//                .list()
//                .layer(0, new ConvolutionLayer.Builder()
//                        // nIn and nOut specify depth. nIn here is the nChannels and 
//                		// nOut is the number of filters to be applied
//                        .nIn(nChannels)
//                        .stride(1, 1)
//                        .nOut(40)
//                        .kernelSize(4,4)
//                        .activation(Activation.RELU)
//                        .build())
//                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
//                        .kernelSize(2,2)
//                        .stride(1,1)
//                        .build())
//                .layer(2, new ConvolutionLayer.Builder()
//                        //Note that nIn need not be specified in later layers
//                        .stride(1, 1)
//                        .kernelSize(2,2)
//                        .nOut(20)
//                        .activation(Activation.RELU)
//                        .build())
//                .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
//                        .kernelSize(2,2)
//                        .stride(2,2)
//                        .build())
//                .layer(4, new DenseLayer.Builder().activation(Activation.RELU)
//                        .nOut(100).build())
//                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
//                        .nOut(1) //one vector (scalar in this case) of one real value
//                        .activation(Activation.IDENTITY)
//                        .build())
//                // 42 is randomized region size
//                .setInputType(InputType.convolutional(42,42,17)) //See note below 
//                .backprop(true).pretrain(false).build();

        /*
        Regarding the .setInputType(InputType.convolutionalFlat(28,28,1)) line: This does a few things.
        (a) It adds preprocessors, which handle things like the transition between the convolutional/subsampling layers
            and the dense layer
        (b) Does some additional configuration validation
        (c) Where necessary, sets the nIn (number of input neurons, or input depth in the case of CNNs) values for each
            layer based on the size of the previous layer (but it won't override values manually set by the user)

        InputTypes can be used with other layer types too (RNNs, MLPs etc) not just CNNs.
        For normal images (when using ImageRecordReader) use InputType.convolutional(height,width,depth).
        MNIST record reader is a special case, that outputs 28x28 pixel grayscale (nChannels=1) images, in a "flattened"
        row vector format (i.e., 1x784 vectors), hence the "convolutionalFlat" input type used here.
        */

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        
//        // ParallelWrapper will take care of load balancing between GPUs.
//        ParallelWrapper wrapper = new ParallelWrapper.Builder(model)
//            // DataSets prefetching options. Set this value with respect to number of actual devices
//            .prefetchBuffer(2)
//
//            // set number of workers equal or higher then number of available devices. x1-x2 are good values to start with
//            .workers(4)
//
//            // rare averaging improves performance, but might reduce model accuracy
//            .averagingFrequency(3)
//
//            // if set to TRUE, on every averaging model score will be reported
//            .reportScoreAfterAveraging(true)
//
//            // optinal parameter, set to false ONLY if your system has support P2P memory access across PCIe (hint: AWS do not support P2P)
//            .useLegacyAveraging(false)
//
//            .build();
        
        
        //Initialize the user interface backend
        UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        StatsStorage statsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later
        
        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);
//        StatsStorageRouter remoteUIRouter = new RemoteUIStatsStorageRouter("http://localhost:9000");

        //Then add the StatsListener to collect this information from the network, as it trains
//        model.setListeners(new StatsListener(remoteUIRouter));
        ArrayList<IterationListener> listeners = new ArrayList<IterationListener>();
        listeners.add(new StatsListener(statsStorage));
        
        model.setListeners(listeners);

        System.out.println("Train model....");
        for( int i=0; i<nEpochs; i++ ) {
            model.fit(train_it);
            System.out.println(String.format("*** Completed epoch %s ***", i));

            System.out.println("Evaluate model....");
            RegressionEvaluation eval = new RegressionEvaluation(outputNum);
            while(test_it.hasNext()){
                DataSet ds = test_it.next();
                INDArray output = model.output(ds.getFeatureMatrix(), false);
//                System.out.println(ds.getLabels());
//                System.out.println(output);
                eval.eval(ds.getLabels(), output);

            }
            System.out.println(eval.stats());
            test_it.reset();
            //train_it.reset();
        }
        System.out.println("****************Example finished********************");
        
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
					Path poolexportpath = Paths.get(exportPath.toString(), "pool.txt" + (compress ? ".gz" : ""));
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
					}
					// Now run the export
					for (SelectionCycle sc : cycles_to_export){
						Path cycleexportpath = Paths.get(exportPath.toString(), sc.getName() + "." + extension);
						AptaLogger.log(Level.INFO, this.getClass(), "Exporting selection cycle " + sc.getName() + " to file " + cycleexportpath.toString());
						export.Cycle(sc, cycleexportpath );
					}
					break;
					
				case "clusters":
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting clusters");
					
					// Get the instance of the StructurePool
					try {
						if (experiment.getClusterContainer() == null)
						{
							experiment.instantiateClusterContainer(false, true);
						}	
					} catch(Exception e) { // We need to make sure a cluster pool exists
						
						throw new InformationNotFoundException("No cluster information was found to export. Did you run AptaCLUSTER?");
						
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
						
					}
					
					// Now run the export
					for (SelectionCycle sc : cycles_to_export){
						Path cycleexportpath = Paths.get(exportPath.toString(), "clusters_" + sc.getName() + "." + extension);
						AptaLogger.log(Level.INFO, this.getClass(), "Exporting clusters of selection cycle " + sc.getName() + " to file " + cycleexportpath.toString());
						export.Clusters(sc, experiment.getClusterContainer(), cycleexportpath );
					}
					
					break;
					
				case "structures":
					
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting structure data");
					
					//Make sure we have structures
					try {
						if (experiment.getStructurePool() == null)
						{
							experiment.instantiateStructurePool(false, true);
						}	
					} catch(Exception e) { // We need to make sure a cluster pool exists
						
						throw new InformationNotFoundException("No structure information was found to export. Did you run AptaSUITE with the -structures option?");
						
					}
						
						
					export.Structures(Configuration.getExperiment().getStructurePool(), Paths.get(exportPath.toString(), "structures.txt" + (compress ? ".gz" : "")));
					break;
					
				case "clustertable":
					
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting cluster table");
					
					// Get the instance of the StructurePool
					try {
						if (experiment.getClusterContainer() == null)
						{
							experiment.instantiateClusterContainer(false, true);
						}	
					} catch(Exception e) { // We need to make sure a cluster pool exists
						
						throw new InformationNotFoundException("No cluster information was found to export. Did you run AptaCLUSTER?");
						
					}
						
						
					export.ClusterTable(experiment, exportPath, "cluster_table.txt" + (compress ? ".gz" : ""));
					break;	
					
				default:
					AptaLogger.log(Level.SEVERE, this.getClass(), "Export option " + token + " not recognized. Exiting.");
					throw new InvalidConfigurationException("Export option " + token + " not recognized.");
			}
			
		}
		
	}
	
}
