/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.CountingBloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * 
 * This class contains all information regarding a single selection cycle.
 * 
 */
public class MapDBSelectionCycle implements SelectionCycle{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5440879993287731191L;

	/**
	 * Enable logging for debuging and information
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);	
	
	/**
	 * The name of this selection cycle as defined in the configuration file
	 */
	private String name;
	
	
	/**
	 * The selection cycle number corresponding to this instance. The initial
	 * pool should have a value of 0.
	 */
	private int round;
	
	
	/**
	 * A unique identifier for this selection cycle. This id can be used for indexing
	 * purposes in other data structures as required.
	 */
	private int id;
	
	/**
	 * The 5' barcode should it be present in the raw sequencing file. It is typically
	 * located at the 5' start of the 5' primer in the raw sequencing file. If no barcode 
	 * is present at this location, the value is null.
	 * 
	 * Any parser implemented in this project will access this value in order to 
	 * perform multiplexing.
	 */
	private String barcode5 = null;
	
	
	/**
	 * The 3' barcode should it be present in the raw sequencing file. It is typically
	 * located at the 3' start of the 3' primer in the raw sequencing file. If no barcode 
	 * is present at this location, the value is null.
	 * 
	 * Any parser implemented in this project will access this value in order to 
	 * perform de-multiplexing.
	 */
	private String barcode3 = null;
	
	
	/**
	 * True if this cycle corresponds to a control cycle. A control cycle is defined
	 * as a selection round performed on target homologs in order to identify non-specific 
	 * binders.
	 * 
	 * The default value is false.
	 */
	private Boolean isControlSelection = false;
	
	
	/**
	 * True if this cycle corresponds to a counter selection. A counter selection is
	 * performed between two selection rounds as a means of removing non-binders from 
	 * the pool.
	 * 
	 * The default value is false.
	 */
	private Boolean isCounterSelection = false;
	
	
	/**
	 * Bloom Filter for fast member lookup
	 */
	private transient BloomFilter<Integer> poolContent = new FilterBuilder(Configuration.getParameters().getInt("MapDBAptamerPool.bloomFilterCapacity"), Configuration.getParameters().getDouble("MapDBSelectionCycle.bloomFilterCollisionProbability")).buildBloomFilter();
	
	
	/**
	 * File backed map containing the IDs of each aptamer (as stored in <code>AptamerPool</code>)
	 * and the number of times they have been sequenced for this particular selection cycle.
	 */
	private transient BTreeMap<Integer,Integer> poolContentCounts = null;
	
	
	/**
	 * Counts the total number of aptamer molecules belonging to this selection cycle
	 */
	private int size = 0;
	
	
	/**
	 * Counts the total number of unique aptamers belonging to this selection cycle
	 */
	private int unique_size = 0;
	
	public MapDBSelectionCycle(String name, int round, boolean isControlSelection, boolean isCounterSelection) throws IOException{
		
		// Set basic information
		this.name = name;
		this.round = round;
		this.isControlSelection = isControlSelection;
		this.isCounterSelection = isCounterSelection;
		
		// Create the file backed map and perform sanity checks
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
				
		// Check if the data path exists, and if not create it
		Path poolDataPath = Files.createDirectories(Paths.get(projectPath.toString(), "cycledata"));

		// Determine the unique file name associated with this cycle
		String cycleFileName = round + "_" + name + ".mapdb";

		LOGGER.info("Creating new file '" + Paths.get(poolDataPath.toString(), cycleFileName).toFile() + "' for selection cycle " + name + ".");

		// Create map or read from file
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), cycleFileName).toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		poolContentCounts = db.treeMap("map")
				//.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .create();
		
	}
	
	@Override
	public String toString(){
		
		return this.name + " (" + this.size + ")";
		
	}


	@Override
	public void addToSelectionCycle(byte[] a) {
		
		// Check if the aptamer is already present in the pool and add it if not
		int id_a = Configuration.getExperiment().getAptamerPool().registerAptamer(a);
		
		// Update the pool size
		size++;
				
		// Fast membership checking due to bloom filter
		if (! poolContent.contains(id_a)){ // this is always accurate, no false negatives
			unique_size++;
			poolContentCounts.put(id_a, 1);
		}
		else{ // we need to update the count...
			
			Integer current_count = poolContentCounts.get(id_a);
			
			if (current_count == null){ // catch false positives
				current_count = 0;
			}
			poolContentCounts.put(id_a, current_count+1);
			
		}
		
	}

	@Override
	public void addToSelectionCycle(String a){
		addToSelectionCycle(a.getBytes());
	}

	@Override
	public boolean containsAptamer(byte[] a) {
		
		// Get the corresponding aptamer id from the pool
		int id_a = Configuration.getExperiment().getAptamerPool().getIdentifier(a);
		
		if (! poolContent.contains(id_a)){
			return false;
		}
		
		Integer current_count = poolContentCounts.get(id_a);
			
		return current_count != null;
	}
	
	@Override
	public boolean containsAptamer(String a){
		return containsAptamer(a.getBytes());
	}


	@Override
	public int getAptamerCardinality(byte[] a) {
		
		int id_a = Configuration.getExperiment().getAptamerPool().getIdentifier(a);
		
		Integer count = poolContentCounts.get(id_a);
		
		if (count == null){
			count = 0;
		}
		
		return count;
	}

	@Override
	public int getAptamerCardinality(String a) {
		return getAptamerCardinality(a.getBytes());
	}
	
	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getUniqueSize() {
		return unique_size;
	}
	
	@Override
	public String getName(){	
		return this.name;
	}

	@Override
	public int getRound() {
		return this.round;
	}

	@Override
	public SelectionCycle getNextSelectionCycle() {
		
		ArrayList<SelectionCycle> cycles = Configuration.getExperiment().getSelectionCycles();

		// The element we aim to find
		SelectionCycle next = null;
		
		// Create iterator starting at the selection cycle and advance until we find the next element
		ListIterator<SelectionCycle> li = cycles.listIterator(this.round);
		while (li.hasNext() && next==null){
			
			SelectionCycle current_cycle = li.next();
			if (current_cycle != null){
				next = current_cycle;
			}
			
		}
		
		return next;
		
	}

	@Override
	public SelectionCycle getPreviousSelectionCycle() {
		ArrayList<SelectionCycle> cycles = Configuration.getExperiment().getSelectionCycles();

		// The element we aim to find
		SelectionCycle previous = null;
		
		// Create iterator starting at the selection cycle and advance until we find the next element
		ListIterator<SelectionCycle> li = cycles.listIterator(this.round);
		while (li.hasPrevious() && previous==null){
			
			SelectionCycle current_cycle = li.previous();
			if (current_cycle != null){
				previous = current_cycle;
			}
			
		}
		
		return previous;
	}

	@Override
	public ArrayList<SelectionCycle> getControlCycles() {
		
		// If no control cycle is present, we return an empty list as specified by the interface
		if (Configuration.getExperiment().getControlSelectionCycles().get(this.round) == null){
			return new ArrayList<SelectionCycle>();
		}
		
		// Otherwise, we return the actual cycles
		return Configuration.getExperiment().getControlSelectionCycles().get(this.round);
	
	}

	@Override
	public ArrayList<SelectionCycle> getCounterSelectionCycles() {
		
		// If no control cycle is present, we return an empty list as specified by the interface
		if (Configuration.getExperiment().getCounterSelectionCycles().get(this.round) == null){
			return new ArrayList<SelectionCycle>();
		}
		
		// Otherwise, we return the actual cycles
		return Configuration.getExperiment().getCounterSelectionCycles().get(this.round);
	
	}

	@Override
	public boolean isControlSelection() {
		return isControlSelection;
	}

	@Override
	public boolean isCounterSelection() {
		return isCounterSelection;
	}

	@Override
	public void setReadOnly() {
		
		poolContentCounts.close();
		
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		Path poolDataPath = Paths.get(projectPath.toString(), "cycledata");
		String cycleFileName = round + "_" + name + ".mapdb";
		
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), cycleFileName).toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .readOnly()
			    .make();

		poolContentCounts = db.treeMap("map")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .createOrOpen();
		
	}
	
	/**
	 * Since MapDB objects are not serializable in itself, we need to 
	 * handle storage and retrieval manually so we can use the java
	 * Serializable interface with the main instance
	 * 
	 * Note: Calling this function will set the MapDB into read-only
	 * mode
	 * 
	 * @param oos
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		
		// default serialization 
	    oos.defaultWriteObject();
		
	    // set into read-only mode
	    this.setReadOnly();
	}
	
	/**
	 * Since MapDB objects are not serializable in itself, we need to 
	 * handle storage and retrieval manually so we can use the java
	 * Serializable interface with the main instance
	 * 
	 * Note: Calling this function will road the MapDB in read-only
	 * mode
	 * @param ois
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {

		// default deserialization
	    ois.defaultReadObject();

		// read the mapdb instance	    
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		Path poolDataPath = Paths.get(projectPath.toString(), "cycledata");
		String cycleFileName = round + "_" + name + ".mapdb";
		
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), cycleFileName).toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .readOnly()
			    .make();

		poolContentCounts = db.treeMap("map")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .createOrOpen();
	
		// Fill the bloom filter. Since we know the precise size by now, and the maps are read only
		// we can save resources by setting the size to the dbmap size
		poolContent = new FilterBuilder(unique_size, Configuration.getParameters().getDouble("MapDBSelectionCycle.bloomFilterCollisionProbability")).buildBloomFilter();
		
		Iterator<Entry<Integer, Integer>> contentit = poolContentCounts.entryIterator();
		while (contentit.hasNext()){
			poolContent.add(contentit.next().getKey());
		}
	}
	
	
}
