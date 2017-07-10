/**
 * 
 */
package utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;

import lib.aptamer.datastructures.Experiment;

/**
 * @author Jan Hoinka
 * 
 * This class functions as a container for configuration options for all
 * the libraries of AptaSuite including the command line interface and
 * the graphical user interface. It provides a static member for
 * property retrieval (<code>getProperty</code>) which should be
 * available to all classes.
 * 
 * In addition it is used as a centralized hub for access to all instances of
 * aptamer related classes (such as Experiment, SelectionCycles, etc) via appropriate 
 * getters and setters.
 *
 */
public class Configuration {
	
	/**
	 * The properties instance. Apache commons configuration API 2.x
	 */
	private static org.apache.commons.configuration2.Configuration parameters;
	
	
	/**
	 * The instance of the experiment. Must be registered with <code>setExperiment</code>
	 */
	private static Experiment experiment = null;

	
	/**
	 * A special object used to signal any consumer to finish in producer-consumer schemes
	 */
	public static final Object POISON_PILL = new Object();

	/**
	 * Adds the configuration stored on disk to the current set of parameters. 
	 * The file <code>fileName</code> must be a valid configuration file
	 * as per Javas <code>Properties</code> class.
	 * @param fileName
	 */
	public static void setConfiguration(String fileName){
		
			try {
				AptaLogger.log(Level.INFO, Configuration.class, "Reading configuration from file.");
				
				FileBasedConfigurationBuilder<org.apache.commons.configuration2.FileBasedConfiguration> builder =
					    new FileBasedConfigurationBuilder<org.apache.commons.configuration2.FileBasedConfiguration>(PropertiesConfiguration.class)
					    .configure(new Parameters().properties()
					        .setFileName(fileName)
					        .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
				
				parameters = builder.getConfiguration();
				
			} catch (Exception e) {
				
				AptaLogger.log(Level.SEVERE , Configuration.class, "Error, could not read configuration file. Please check it for correctness");
				e.printStackTrace();
			}	
		
			// Define default parameters
			HashMap<String, Object> defaults= new HashMap<String,Object>()
			{{
				 // Default Experiment Name, required for data export
				 put("Experiment.name", "Sample Experiment");
				
			     // PoolBackend
			     put("AptamerPool.backend", "MapDBAptamerPool");
				
				 // PoolMapDB Options
			     put("MapDBAptamerPool.bloomFilterCapacity", 500000000);
			     put("MapDBAptamerPool.bloomFilterCollisionProbability", 0.001);
			     put("MapDBAptamerPool.maxTreeMapCapacity", 1000000);  
			     

			     // SelectionCycle Backend
			     put("SelectionCycle.backend", "MapDBSelectionCycle");
			     
			     // StructureBackend
			     put("StructurePool.backend", "MapDBStructurePool");
			     
			     // StructurePoolMapDB Options
			     put("MapDBStructurePool.bloomFilterCollisionProbability", 0.001);
			     put("MapDBStructurePool.maxTreeMapCapacity", 500000); 
			     
			     // BloomFilterSelectionCycle  Options
			     put("MapDBSelectionCycle.bloomFilterCollisionProbability", 0.001); //the capacity must be the same as MapDBAptamerPool.bloomFilterCapacity
			     
			     // AptaCluster Option
			     put("ClusterContainer.backend", "MapDBClusterContainer");
			     put("Aptacluster.EditDistance", 5); // The maximal number of nucleodite differences between two sequences to be considered mememebers of the same cluster
			     put("Aptacluster.LSHIterations", 5); // The number of LSH iterations to be performed
			     put("Aptacluster.KmerSize", 3); // The kmer size used for the distance calculations
			     put("Aptacluster.KmerCutoffIterations", 10000); // The number of iterations to be performed for computing the kmer cutoff for cluster formation
			     
			     // Parser Options
			     put("Parser.backend", "AptaplexParser");
			     
			     // AptaplexParser Options
			     put("AptaplexParser.isPerFile", false);
			     put("AptaplexParser.BlockingQueueSize", 500); // 10
			     put("AptaplexParser.PairedEndMinOverlap", 15); // Milab option: smallest overlap required when creating contig
			     put("AptaplexParser.PairedEndMaxMutations", 5); // Maximal number of mutations in the overlapping region for a sequence to be accepted
			     put("AptaplexParser.PairedEndMaxScoreValue", 55); // Highest score of the current quality score model 55 for phred
			     put("AptaplexParser.BarcodeTolerance", 1); // Maximal number of mutations allowed in the barcodes
			     put("AptaplexParser.PrimerTolerance", 3); // Maximal number of mutations allowed in the primers
			     
			     // AptaSIM Options
			     put("Aptasim.HmmDegree", 2); // Degree of the Markov model
			     put("Aptasim.RandomizedRegionSize", 40); // Length of the randomized region in the aptamers
			     put("Aptasim.NumberOfSequences", 1000000); // Number of sequences in the initial pool
			     put("Aptasim.NumberOfSeeds", 100); // Number of high affinity sequences in the initial pool
			     put("Aptasim.MinSeedAffinity", 80); // The minimal affinity for seed sequences (INT range: 0-100)
			     put("Aptasim.MaxSequenceCount", 10); // Maximal count of remaining sequences
			     put("Aptasim.MaxSequenceAffinity", 25); // The maximal sequence affinity for non-seeds (INT range: 0-100)
			     put("Aptasim.NucleotideDistribution", "0.25, 0.25, 0.25, 0.25"); // If no training data is specified, create pool based on this distribution (order A,C,G,T)
			     put("Aptasim.SelectionPercentage", 0.20); // The percentage of sequences that remain after selection (DOUBLE range: 0-1)
			     put("Aptasim.BaseMutationRates", "0.25, 0.25, 0.25, 0.25"); // Mutation rates for individual nucleotides (order A,C,G,T)
			     put("Aptasim.MutationProbability", 0.05); // Mutation probability during PCR (DOUBLE range: 0-1)
			     put("Aptasim.AmplificationEfficiency", 0.995); // PCR amplification efficiency (DOUBLE range: 0-1)
				 
			     // AptaTRACE Options
			     put("Aptatrace.KmerLength",6); // Size of the kmers used as the initial motif length
			     put("Aptatrace.FilterClusters",true); // Whether to apply additional filtering methods to remove overlapping motifs
			     put("Aptatrace.OutputClusters",true); // Whether to write clusters to file or not
			     put("Aptatrace.Alpha",10); // The parameter alpha specifies which sequences should be included in the background model, i.e. all sequences whose number of occurrences is smaller than, or equal to this value are taken into account.
			     
			     // Export
			     put("Export.compress", true); // Whether the resulting files should be gzip compressed or not
			     put("Export.SequenceFormat", "fastq"); // The output format for nucleotide data [Fastq, Fasta, Raw]
			     put("Export.IncludePrimerRegions", true); // If false, the 5' and 3' primers will not be exported 
			     put("Export.MinimalClusterSize", 1); // The smallest amount of members a cluster should contain in order to be exported
			     put("Export.PoolCardinalityFormat", "frequencies"); // [counts, frequencies] The format in which the cardinalities of the aptamers should be exported for in each selection cycle.
			     
			     // Performance Options
			     put("Performance.maxNumberOfCores", 30); // if larger than available, min of both is taken
			     
			}};
			
			// add to parameters only if they have not been defined in the file
			for ( Entry<String, Object> item : defaults.entrySet()){
				if (!parameters.containsKey(item.getKey())){
					parameters.setProperty(item.getKey(), item.getValue());
				}
			}
			
			// TODO: Sanity checks!
	}
	
	/**
	 * Returns the parameter set including the return value
	 * default parameters as well the properties imported via file. The individual parameters
	 * can the be accessed using Apache Common Configuration 2.x APIs.
	 * @return the parameter set
	 */
	public static org.apache.commons.configuration2.Configuration getParameters() {
		return parameters;
	}
	
	
	/**
	 * Registers the instance of an Experiment with the Configuration class
	 * @param e null if not set
	 */
	public static void setExperiment(Experiment e){
		experiment = e;
	}
	
	
	/**
	 * Returns the Experiment instance.
	 * @return
	 */
	public static Experiment getExperiment(){
		return experiment;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		return printParameters();
	}
	
	
	public static String printParameters(){

		StringBuilder sb = new StringBuilder();
		Iterator<String> keys = parameters.getKeys();
		
		while (keys.hasNext()){
			String key = keys.next();
			sb.append(String.format("%s : %s\n", (String) key, parameters.getProperty(key)) );
		}
		
		return sb.toString();
	}
}