/**
 * 
 */
package utilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.ConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;


import lib.aptamer.datastructures.Experiment;

/**
 * @author Jan Hoinka
 * 
 *         This class functions as a container for configuration options for all
 *         the libraries of AptaSuite including the command line interface and
 *         the graphical user interface. It provides a static member for
 *         property retrieval (<code>getProperty</code>) which should be
 *         available to all classes.
 * 
 *         In addition it is used as a centralized hub for access to all
 *         instances of aptamer related classes (such as Experiment,
 *         SelectionCycles, etc) via appropriate getters and setters.
 *
 */
public class Configuration {

	/**
	 * Default parameters for AptaSUITE
	 */
	private static HashMap<String, Object> defaults = new HashMap<String, Object>();
	static {

		// Default Experiment Name, required for data export
		defaults.put("Experiment.name", "Sample Experiment");

		// PoolBackend
		defaults.put("AptamerPool.backend", "MapDBAptamerPool");

		// PoolMapDB Options
		defaults.put("MapDBAptamerPool.bloomFilterCapacity", 250000000); // This should be set to a value >= than the total number of unique aptamers in the experiment
		defaults.put("MapDBAptamerPool.bloomFilterCollisionProbability", 0.001);
		defaults.put("MapDBAptamerPool.maxTreeMapCapacity", 1000000);

		// SelectionCycle Backend
		defaults.put("SelectionCycle.backend", "MapDBSelectionCycle");

		// StructureBackend
		defaults.put("StructurePool.backend", "MapDBStructurePool");

		// StructurePoolMapDB Options
		defaults.put("MapDBStructurePool.bloomFilterCollisionProbability", 0.001);
		defaults.put("MapDBStructurePool.maxTreeMapCapacity", 500000);
		defaults.put("MapDBStructurePool.maxTreeMapCapacityBppm", 150000);

		// BloomFilterSelectionCycle Options
		defaults.put("MapDBSelectionCycle.bloomFilterCollisionProbability", 0.001); // the capacity must be the same as
																					// MapDBAptamerPool.bloomFilterCapacity

		// AptaCluster Options
		defaults.put("ClusterContainer.backend", "MapDBClusterContainer");
		defaults.put("Aptacluster.EditDistance", 5); // The maximal number of nucleodite differences between two
														// sequences to be considered mememebers of the same cluster
		defaults.put("Aptacluster.LSHIterations", 5); // The number of LSH iterations to be performed
		defaults.put("Aptacluster.KmerSize", 3); // The kmer size used for the distance calculations
		defaults.put("Aptacluster.KmerCutoffIterations", 10000);    // The number of iterations to be performed for
																	// computing the kmer cutoff for cluster formation

		// Parser Options
		defaults.put("Parser.backend", "AptaplexParser");

		// AptaplexParser Options
		defaults.put("AptaplexParser.reader", "FastqReader"); //Current options are FastqReader and RawReader
		defaults.put("AptaplexParser.isPerFile", true); 
		defaults.put("AptaplexParser.BlockingQueueSize", 5000); 
		defaults.put("AptaplexParser.PairedEndMinOverlap", 15); // Milab option: smallest overlap required when creating
																// contig
		defaults.put("AptaplexParser.PairedEndMaxMutations", 5); // Maximal number of mutations in the overlapping
																	// region for a sequence to be accepted
		defaults.put("AptaplexParser.PairedEndMaxScoreValue", 55); // Highest score of the current quality score model
																	// 55 for phred
		defaults.put("AptaplexParser.BarcodeTolerance", 1); // Maximal number of mutations allowed in the barcodes
		defaults.put("AptaplexParser.PrimerTolerance", 3); // Maximal number of mutations allowed in the primers

		defaults.put("AptaplexParser.StoreReverseComplement", false); // Useful for DNA aptamers. If set to true, the reverse complement of the
																	  // of the identified primers and randomized region of the contig is stored in the database 
		defaults.put("AptaplexParser.CheckReverseComplement", false); // If set to true, AptaPlex will attempt to demultiplex and extract the randomized region of 
																	  // of the reverse complement of a contig should the initial attempt have failed. This setting is
																	  // useful if you expect aptamers to be present in a mixture of forward and reverse-complements
																	  // in your sequencing data
		defaults.put("AptaplexParser.OnlyRandomizedRegionInData", false); 	// If set to true, AptaPlex will assume that the barcodes AND the Primers have already been removed
																			// by a third party application and will import the sequences without any checks (other than nucleotide validity).
																			// In addition, it is assumed that the data has already been demultiplexed.		
		defaults.put("AptaplexParser.UndeterminedToFile", false); 	// If set to true, AptaPlex will dump all read which failed processing for any reason to a fastq file located
																	// in the export folder of the project and named undetermined.fastq.gz
		
		// AptaSIM Options
		defaults.put("Aptasim.HmmDegree", 2); // Degree of the Markov model
		defaults.put("Aptasim.RandomizedRegionSize", 40); // Length of the randomized region in the aptamers
		defaults.put("Aptasim.NumberOfSequences", 1000000); // Number of sequences in the initial pool
		defaults.put("Aptasim.NumberOfSeeds", 100); // Number of high affinity sequences in the initial pool
		defaults.put("Aptasim.MinSeedAffinity", 80); // The minimal affinity for seed sequences (INT range: 0-100)
		defaults.put("Aptasim.MaxSequenceCount", 10); // Maximal count of remaining sequences
		defaults.put("Aptasim.MaxSequenceAffinity", 25); // The maximal sequence affinity for non-seeds (INT range:
															// 0-100)
		
		String[] nucleotideDistribution = {"0.25","0.25","0.25","0.25"};		
		defaults.put("Aptasim.NucleotideDistribution", nucleotideDistribution); // If no training data is specified,
																					// create pool based on this
																					// distribution (order A,C,G,T)
		defaults.put("Aptasim.SelectionPercentage", 0.20); // The percentage of sequences that remain after selection
															// (DOUBLE range: 0-1)
		
		String[] baseMutationRates = {"0.25","0.25","0.25","0.25"};	
		defaults.put("Aptasim.BaseMutationRates", baseMutationRates); // Mutation rates for individual
																				// nucleotides (order A,C,G,T)
		defaults.put("Aptasim.MutationProbability", 0.05); // Mutation probability during PCR (DOUBLE range: 0-1)
		defaults.put("Aptasim.AmplificationEfficiency", 0.995); // PCR amplification efficiency (DOUBLE range: 0-1)

		// AptaTRACE Options
		defaults.put("Aptatrace.KmerLength", 6);        // Size of the kmers used as the initial motif length
		defaults.put("Aptatrace.FilterClusters", true); // Whether to apply additional filtering methods to remove
														// overlapping motifs
		defaults.put("Aptatrace.OutputClusters", true); // Whether to write clusters to file or not
		defaults.put("Aptatrace.Alpha", 10);    // The parameter alpha specifies which sequences should be included in the
												// background model, i.e. all sequences whose number of occurrences is
												// smaller than, or equal to this value are taken into account.

		// Export
		defaults.put("Export.compress", true); // Whether the resulting files should be gzip compressed or not
		defaults.put("Export.SequenceFormat", "fastq"); // The output format for nucleotide data [fastq, fasta]
		defaults.put("Export.IncludePrimerRegions", true); // If false, the 5' and 3' primers will not be exported
		defaults.put("Export.MinimalClusterSize", 1); // The smallest amount of members a cluster should contain in
														// order to be exported
		defaults.put("Export.ClusterFilterCriteria", "ClusterSize"); 	// [ClusterDiversity, ClusterSize] Defines by which criteria Export.MinimalClusterSize should filter clusters. 
															// ClusterDiversity measures the total number of unique sequences in a cluster.
															// Cluster Size measures the sum of aptamer cardinalities over all cluster members. 
		defaults.put("Export.PoolCardinalityFormat", "frequencies"); // [counts, frequencies] The format in which the
																		// cardinalities of the aptamers should be
																		// exported for in each selection cycle.
		
		// Performance Options
		defaults.put("Performance.maxNumberOfCores", 50); // if larger than available, min of both is taken
		defaults.put("MapDBAllocateStartSize", 1 * 1024*1024*1024); // for each file, initially allocate 1GB of storage, workaround for bug https://github.com/jankotek/mapdb/issues/723
		defaults.put("MapDBAllocateIncrement", 100 * 1024*1024); // and increment it in 100mb chunks to keep the number of mmap file handles managable
		
	}

	/**
	 * The properties instance. Apache commons configuration API 2.x
	 */
	private static org.apache.commons.configuration2.Configuration parameters;

	/**
	 * The instance of the experiment. Must be registered with
	 * <code>setExperiment</code>
	 */
	private static Experiment experiment = null;

	/**
	 * A special object used to signal any consumer to finish in producer-consumer
	 * schemes
	 */
	public static final Object POISON_PILL = new Object();
	
	/**
	 * Configuration file builder used to load and save the current config to file.
	 */
	private static FileBasedConfigurationBuilder<org.apache.commons.configuration2.FileBasedConfiguration> builder;

	/**
	 * In-memory configuration builder for the default values
	 */
	private static BasicConfigurationBuilder<PropertiesConfiguration> defaultBuilder = null;
	
	private static Path configurationPath;
	
	/**
	 * Adds the configuration stored on disk to the current set of parameters. The
	 * file <code>fileName</code> must be a valid configuration file as per Javas
	 * <code>Properties</code> class.
	 * 
	 * @param fileName
	 */
	public static void setConfiguration(String fileName) {

		try {
			AptaLogger.log(Level.INFO, Configuration.class, "Reading configuration from file.");

			builder = new FileBasedConfigurationBuilder<org.apache.commons.configuration2.FileBasedConfiguration>(
					PropertiesConfiguration.class)
							.configure(new Parameters().properties().setFileName(fileName)
									.setListDelimiterHandler(new DefaultListDelimiterHandler(',')));

			
			// Create a composite configuration which allows to keep user parameters and defaults separated
			CompositeConfiguration  cc = new CompositeConfiguration(); 
			
			cc.addConfiguration(builder.getConfiguration(), true); // changes will be saved in the user config
			cc.addConfiguration(getDefaultParametersBuilder().getConfiguration(), false);
						
			parameters = cc;
			
			// Store the configuration path
			configurationPath = Paths.get(fileName).toAbsolutePath();
			
			// Make sure the project path exists on disk. If not, attempt to guess the pass by using the 
			// path of the configuration file
			Path projectPath;
			if (Configuration.getParameters().containsKey("Experiment.projectPath")) {
				projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath")).toAbsolutePath();
				
				// If the directory does not exist, we assume parent of the config file location to be the path
				if (!Files.exists(projectPath)) {
					projectPath = configurationPath.getParent();
					AptaLogger.log(Level.WARNING, Configuration.class, "Could not find Experiment.projectPath on disk. Assuming parent of config file " + projectPath.toString());
					
				}
				
			}
			else {
				projectPath = configurationPath.getParent();
				AptaLogger.log(Level.WARNING, Configuration.class, "Could not find Experiment.projectPath on disk. Assuming parent of config file " + projectPath.toString());
			}
			
			// Write the configuration to volatile memory
			Configuration.getParameters().setProperty("Experiment.projectPath", projectPath.toString());
		
			
			
			
		} catch (Exception e) {

			AptaLogger.log(Level.SEVERE, Configuration.class,
					"Error, could not read configuration file. Please check it for correctness");
			AptaLogger.log(Level.SEVERE, Configuration.class, e);
			e.printStackTrace();
		}


		// TODO: Sanity checks!
	}
	
	/**
	 * Creates an empty configuration and configures the class 
	 * @param filename location at which the config should be stored in the future
	 */
	public static void createConfiguration(Path filePath) {
		
		try {
			
			builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
			    .configure(new Parameters().properties()
			        //.setFile(filePath.toFile())
			        .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
			
			// For some reason this needs be called to initialize correctly
			builder.getConfiguration();
			
			//We need to explicitly save the file before setting it in the builder, for whatever reason...
			builder.getFileHandler().save(filePath.toFile());
			builder.getFileHandler().setFile(filePath.toFile());
			
			// Create a composite configuration which allows to keep user parameters and defaults separated
			CompositeConfiguration  cc = new CompositeConfiguration();
			
			cc.addConfiguration(builder.getConfiguration(), true); // changes will be saved in the user config
			cc.addConfiguration(getDefaultParametersBuilder().getConfiguration(), false);
			
			parameters = cc;
			
		} catch (Exception e) {

			AptaLogger.log(Level.SEVERE, Configuration.class,
					"Error, could not create configuration file.");
			AptaLogger.log(Level.SEVERE, Configuration.class, e);
			e.printStackTrace();
		}

	}

	
	private static BasicConfigurationBuilder<PropertiesConfiguration> getDefaultParametersBuilder(){
		
		// If we already have an instance, return it
		if(defaultBuilder != null) {
			
			return defaultBuilder;
			
		}
		
		// Create an in-memory configuration instance
		defaultBuilder = new BasicConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class);
		
		PropertiesConfiguration config = null;
		
		try {
			
			config = defaultBuilder.getConfiguration();
			
			
		} catch (ConfigurationException e) {
			AptaLogger.log(Level.SEVERE, Configuration.class, e);
			e.printStackTrace();
		}
		
		// Fill it with the default configuration
		for (Entry<String, Object> item : defaults.entrySet()) {
			if (!config.containsKey(item.getKey())) {
				config.setProperty(item.getKey(), item.getValue());
			}
		}
		
		return defaultBuilder;
		
	}
	
	/**
	 * Returns the parameter set including the return value default parameters as
	 * well the properties imported via file. The individual parameters can the be
	 * accessed using Apache Common Configuration 2.x APIs.
	 * 
	 * @return the parameter set
	 */
	public static org.apache.commons.configuration2.Configuration getParameters() {
		return parameters;
	}

	/**
	 * Registers the instance of an Experiment with the Configuration class
	 * 
	 * @param e
	 *            null if not set
	 */
	public static void setExperiment(Experiment e) {
		experiment = e;
	}

	/**
	 * Returns the Experiment instance.
	 * 
	 * @return
	 */
	public static Experiment getExperiment() {
		return experiment;
	}

	
	/**
	 * Writes the current configuration to file
	 */
	public static void writeConfiguration() {
		
		try {
			
			builder.save();
			AptaLogger.log(Level.CONFIG, Configuration.class, "Written Configuration file to " + builder.getFileHandler().getPath());
			
		} catch (ConfigurationException e) {
			
			// TODO Auto-generated catch block
			AptaLogger.log(Level.SEVERE, Configuration.class, e);
			e.printStackTrace();
			
		}
		
	}
	
	/**
	 * Returns PropertiesConfiguration of the default values
	 * @return
	 */
	public static PropertiesConfiguration getDefaults(){
		
		PropertiesConfiguration default_configuration = null;
		
		try {
			default_configuration = getDefaultParametersBuilder().getConfiguration();
		} catch (ConfigurationException e) {
			AptaLogger.log(Level.SEVERE, Configuration.class, e);
			e.printStackTrace();
		}
		
		return default_configuration;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return printParameters();
	}

	public static String printParameters() {

		StringBuilder sb = new StringBuilder();
		Iterator<String> keys = parameters.getKeys();

		while (keys.hasNext()) {
			String key = keys.next();
			sb.append(String.format("%s : %s\n", (String) key, parameters.getProperty(key)));
		}

		return sb.toString();
	}
	
	public Path getConfigurationPath() {
		
		return configurationPath;
		
	}
	
	/**
	 * Resets the config to its initial state
	 */
	public static void reset() {
		
		Configuration.experiment = null;
		Configuration.parameters = null;
		Configuration.builder = null;
		Configuration.defaultBuilder = null;
		Configuration.configurationPath = null;
		
	}
	
}