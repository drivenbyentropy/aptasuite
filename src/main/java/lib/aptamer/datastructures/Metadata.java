/**
 * 
 */
package lib.aptamer.datastructures;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import utilities.Accumulator;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Contains additional information regarding the experiment
 * that is not required strictly required for any routines but
 * might still be of interest to the user.
 * 
 * Examples of these data might be the nucleotide frequency 
 * distributions of the aptmers, the quality scroes of the reads
 * etc.
 * 
 */
public class Metadata {

	/**
	 *  Stores the quality scores for each nucleotide position averaged over all the reads per selection cycle
	 *  Key:   SelectionCycle name
	 *  Value: <Nucleotide Position, Averaged Quality Score>
	 */
	public HashMap<String, ConcurrentHashMap<Integer, Accumulator>> qualityScoresForward = null;
	
	/**
	 *  Stores the quality scores for each nucleotide position averaged over all the reads per selection cycle
	 *  Key:   SelectionCycle name
	 *  Value: <Nucleotide Position, Averaged Quality Score>
	 */
	public HashMap<String, ConcurrentHashMap<Integer,Accumulator>> qualityScoresReverse = null;	
		
	/**
	 * The nucleotide distribution at each position of the forward read per selection cycle
	 * Key:   SelectionCycle name
	 * Value: <Nucleotide Position, <Nucleotide, Count>>
	 */
	public HashMap<String, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>> nucleotideDistributionForward = null;
	
	/**
	 * The nucleotide distribution at each position of the reverse read per selection cycle
	 * Key:   SelectionCycle name
	 * Value: <Nucleotide Position, <Nucleotide, Count>>
	 */
	public HashMap<String, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>> nucleotideDistributionReverse = null;
	
	/**
	 * The nucleotide distribution at each position of the accepted reads per length and per selection cycle
	 * Key:   SelectionCycle name
	 * Value: <Randomized Region Size, <Nucleotide Position, <Nucleotide, Count>>>
	 */
	public HashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>>> nucleotideDistributionAccepted = null;

	
	
	/**
	 * Contains the parsing statistics after all the data has been processed. Currently, the following
	 * keys are expected to exist:
	 * 1) processed_reads
	 * 2) accepted_reads
	 * 3) contig_assembly_fails
	 * 4) invalid_alphabet
	 * 5) 5_prime_error
	 * 6) 3_prime_error
	 * 7) invalid_cycle
	 * 8) total_primer_overlaps
	 */
	public HashMap<String, Integer> parserStatistics = null;
	
	/**
	 * Storage location 
	 */
	private Path metadataPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "metadata.mapdb");
	
	
	/**
	 * Constructor.
	 * @param newdb if true a new mapDB File will be created, 
	 * else values will be read from the existing file according to the
	 * configuration.
	 * 
	 */
	public Metadata(boolean newdb) {
		
		// we need to read from file and update class members
		if(!newdb){  loadDataFromFile();  }
		
		else { // create new instances
			
			AptaLogger.log(Level.CONFIG, this.getClass(), "Creating new Metadata instance.");
			
			qualityScoresForward = new HashMap<String, ConcurrentHashMap<Integer, Accumulator>>();
			qualityScoresReverse = new HashMap<String, ConcurrentHashMap<Integer, Accumulator>>();
			
			nucleotideDistributionForward = new HashMap<String, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>>();
			nucleotideDistributionReverse = new HashMap<String, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>>();
			
			nucleotideDistributionAccepted = new HashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>>>();
		
			// Instantiate the selection cycle 
			for (SelectionCycle sc : Configuration.getExperiment().getAllSelectionCycles()) {
				
				qualityScoresForward.put(sc.getName(), new ConcurrentHashMap<Integer, Accumulator>());
				qualityScoresReverse.put(sc.getName(), new ConcurrentHashMap<Integer, Accumulator>());
				
				nucleotideDistributionForward.put(sc.getName(), new ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>());
				nucleotideDistributionReverse.put(sc.getName(), new ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>());
				
				nucleotideDistributionAccepted.put(sc.getName(), new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>>>());
				
			}
			
			// parser statistics
			parserStatistics = new HashMap<String, Integer>(8);
		}
		
	}
	
	
	/**
	 * Reads all Metadata internal datastructure from file to the instance of this class
	 */
	public void loadDataFromFile() {

		AptaLogger.log(Level.CONFIG, this.getClass(), "Reading metadata file from '" + this.metadataPath.toFile());
		
		// Open a connection to disk
		DB db = DBMaker
		        .fileDB(this.metadataPath.toFile())
		        .readOnly()
		        .fileMmapEnableIfSupported()
		        .closeOnJvmShutdown()
		        .make();
		
		// Load all class members from db
		Atomic.Var<Object> variable;
		
		variable = db.atomicVar("qualityScoresForward").createOrOpen();
		qualityScoresForward = (HashMap<String, ConcurrentHashMap<Integer, Accumulator>>) variable.get();

		variable = db.atomicVar("qualityScoresReverse").createOrOpen();
		qualityScoresReverse = (HashMap<String, ConcurrentHashMap<Integer, Accumulator>>) variable.get();
		
		variable = db.atomicVar("nucleotideDistributionForward").createOrOpen();
		nucleotideDistributionForward = (HashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>) variable.get();
		
		variable = db.atomicVar("nucleotideDistributionReverse").createOrOpen();
		nucleotideDistributionReverse = (HashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>) variable.get();

		variable = db.atomicVar("nucleotideDistributionAccepted").createOrOpen();
		nucleotideDistributionAccepted = (HashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>>) variable.get();
		
		variable = db.atomicVar("parserStatistics").createOrOpen();
		parserStatistics = (HashMap<String, Integer>) variable.get();
		
		// Close database connection
        db.close();
		
	}
	
	/**
	 * Takes the current state of all class members of Metadata and dumps them on disk
	 */
	public void saveDataToFile() {
		
		// Open a connection to disk
		DB db = DBMaker
		        .fileDB(this.metadataPath.toFile())
		        .fileMmapEnableIfSupported()
		        .closeOnJvmShutdown()
		        .make();
		
		// Load all class members from db
		Atomic.Var<Object> variable;
		
		variable = db.atomicVar("qualityScoresForward").createOrOpen();
		variable.set(qualityScoresForward);

		variable = db.atomicVar("qualityScoresReverse").createOrOpen();
		variable.set(qualityScoresReverse);
		
		variable = db.atomicVar("nucleotideDistributionForward").createOrOpen();
		variable.set(nucleotideDistributionForward);
		
		variable = db.atomicVar("nucleotideDistributionReverse").createOrOpen();
		variable.set(nucleotideDistributionReverse);

		variable = db.atomicVar("nucleotideDistributionAccepted").createOrOpen();
		variable.set(nucleotideDistributionAccepted);
		
		variable = db.atomicVar("parserStatistics").createOrOpen();
		variable.set(parserStatistics);
		
		// Close database connection
        db.close();
		
        AptaLogger.log(Level.CONFIG, this.getClass(), "Saved metadata to file '" + this.metadataPath.toFile());
        
	}
	
}
