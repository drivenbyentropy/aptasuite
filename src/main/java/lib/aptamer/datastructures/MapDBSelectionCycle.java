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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
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
	 * The name of this selection cycle as defined in the configuration file
	 */
	private String name;
	
	
	/**
	 * The selection cycle number corresponding to this instance. The initial
	 * pool should have a value of 0.
	 */
	private int round;
	
	
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
	 * The initial space reserved on disk for each map 
	 */
	private Integer allocateStartSize = Configuration.getParameters().getInt("MapDBAllocateStartSize");
	
	
	/**
	 * The amount by which each map will be incremented once it is full 
	 */
	private Integer allocateIncrement = Configuration.getParameters().getInt("MapDBAllocateIncrement");
	
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
	
	
	/**
	 * The 5' barcode used to demultiplex the cycle data, null if non present 
	 */
	private byte[] barcodeFive = null;
	
	
	/**
	 * The 3' barcode used to demultiplex the cycle data, null if non present
	 */
	private byte[] barcodeThree = null;
	
	public MapDBSelectionCycle(String name, int round, boolean isControlSelection, boolean isCounterSelection, boolean newdb) throws IOException{
		
		AptaLogger.log(Level.INFO, this.getClass(), "Processing selection cycle " + name );
		
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

		// Create map or read from file
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), cycleFileName).toFile())
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();
		
		// Creating a new database
		if (newdb)
		{
			AptaLogger.log(Level.CONFIG, this.getClass(), "Creating new file '" + Paths.get(poolDataPath.toString(), cycleFileName).toFile() + "' for selection cycle " + name + ".");
	
			poolContentCounts = db.treeMap("map")
					//.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(Serializer.INTEGER)
			        .create();
		}
		else { // we need to read from file and update class members
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reading from file '" + Paths.get(poolDataPath.toString(), cycleFileName).toFile() + "' for selection cycle " + name + ".");
			
			poolContentCounts = db.treeMap("map")
					//.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(Serializer.INTEGER)
			        .open();
			
			// update class members
//			Iterator<Entry<Integer, Integer>> entryit = poolContentCounts.entryIterator();
//			while ( entryit.hasNext() ){
//				Entry<Integer, Integer> entry = entryit.next();
//				
//				poolContent.add(entry.getKey());
//				size += entry.getValue();
//				unique_size++;
//			}
			
			poolContentCounts.forEach( (key,value) ->{
				poolContent.add(key);
				size += value;
				unique_size++;
			});
			
		}
	}
	
	@Override
	public String toString(){
		
		return this.name; // + " (" + this.size + ")";
		
	}


	public synchronized int addToSelectionCycle(byte[] a, int rr_start, int rr_end, int count) {
		
		// Check if the aptamer is already present in the pool and add it if not
		int id_a = Configuration.getExperiment().getAptamerPool().registerAptamer(a, rr_start, rr_end);
		
		// Update the pool size
		size+=count;
				
		// Fast membership checking due to bloom filter
		if (! poolContent.contains(id_a)){ // this is always accurate, no false negatives
			
			unique_size++;
			poolContentCounts.put(id_a, count);
			poolContent.add(id_a);
			
		}
		else{ // we need to update the count...
			
			Integer current_count = poolContentCounts.get(id_a);
			
			if (current_count == null){ // catch false positives
				current_count = 0;
				poolContent.add(id_a);
			}
			poolContentCounts.put(id_a, current_count+count);
			
		}
		
		return id_a;
		
	}
	
	public synchronized int addToSelectionCycle(byte[] a, int rr_start, int rr_end) {
		return addToSelectionCycle(a, rr_start, rr_end, 1);
	}
	
	public synchronized int addToSelectionCycle(String a, int rr_start, int rr_end){
		return addToSelectionCycle(a.getBytes(), rr_start, rr_end, 1);
	}

	public synchronized int addToSelectionCycle(String a, int rr_start, int rr_end, int count){
		return addToSelectionCycle(a.getBytes(), rr_start, rr_end, count);
	}
	
	public boolean containsAptamer(byte[] a) {
		
		// Get the corresponding aptamer id from the pool
		int id_a = Configuration.getExperiment().getAptamerPool().getIdentifier(a);
		
		return containsAptamer(id_a);
		
	}
	
	public boolean containsAptamer(String a){
		return containsAptamer(a.getBytes());
	}

	@Override
	public boolean containsAptamer(int id_a) {
		
		if (! poolContent.contains(id_a)){
			return false;
		}
		
		Integer current_count = poolContentCounts.get(id_a);
			
		return current_count != null;
	}

	public int getAptamerCardinality(byte[] a) {
		
		int id_a = Configuration.getExperiment().getAptamerPool().getIdentifier(a);
		
		return getAptamerCardinality(id_a);
	}

	public int getAptamerCardinality(String a) {
		return getAptamerCardinality(a.getBytes());
	}
	
	public int getAptamerCardinality(int id){
		
		Integer count = poolContentCounts.get(id);
		
		if (count == null){
			count = 0;
		}
		
		return count;
		
	}
	
	public int getSize() {
		return size;
	}

	public int getUniqueSize() {
		return unique_size;
	}
	
	public String getName(){	
		return this.name;
	}

	public int getRound() {
		return this.round;
	}

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

	public ArrayList<SelectionCycle> getControlCycles() {
		
		// If no control cycle is present, we return an empty list as specified by the interface
		if (Configuration.getExperiment().getControlSelectionCycles().get(this.round) == null){
			return new ArrayList<SelectionCycle>();
		}
		
		// Otherwise, we return the actual cycles
		return Configuration.getExperiment().getControlSelectionCycles().get(this.round);
	
	}

	public ArrayList<SelectionCycle> getCounterSelectionCycles() {
		
		// If no control cycle is present, we return an empty list as specified by the interface
		if (Configuration.getExperiment().getCounterSelectionCycles().get(this.round) == null){
			return new ArrayList<SelectionCycle>();
		}
		
		// Otherwise, we return the actual cycles
		return Configuration.getExperiment().getCounterSelectionCycles().get(this.round);
	
	}

	public boolean isControlSelection() {
		return isControlSelection;
	}

	public boolean isCounterSelection() {
		return isCounterSelection;
	}

	public void setReadOnly() {
		
		poolContentCounts.close();
		
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		Path poolDataPath = Paths.get(projectPath.toString(), "cycledata");
		String cycleFileName = round + "_" + name + ".mapdb";
		
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), cycleFileName).toFile())
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .readOnly()
			    .make();

		poolContentCounts = db.treeMap("map")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .open();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + Paths.get(poolDataPath.toString(), cycleFileName).toString() );
		
	}
	
	@Override
	public void setReadWrite() {
		
		poolContentCounts.close();
		
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		Path poolDataPath = Paths.get(projectPath.toString(), "cycledata");
		String cycleFileName = round + "_" + name + ".mapdb";
		
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), cycleFileName).toFile())
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		poolContentCounts = db.treeMap("map")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .open();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + Paths.get(poolDataPath.toString(), cycleFileName).toString() );
	}
	
	
	public void close(){
		this.poolContentCounts.close();
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
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
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

	
	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id->count and sequence->count to the API.
	 * This class implements the id->count view.
	 * Eg. <code>for ( cycle_it : cycle.iterator() ){ }</code>
	 */
	private class SelectionCycleIterator implements Iterable<Entry<Integer, Integer>> {

		public Iterator<Entry<Integer, Integer>> iterator() {
			return poolContentCounts.entryIterator();
		}

	}
	
	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id->count and sequence->count to the API.
	 * This class implements the sequence->count view.
	 */
	private class SelectionCycleSequenceIterator implements Iterable<Entry<byte[], Integer>> {

		@Override
		public Iterator<Entry<byte[], Integer>> iterator() {
			Iterator<Entry<byte[], Integer>> it = new Iterator<Entry<byte[], Integer>>() {
	
				Iterator<Entry<Integer, Integer>> pool_iterator = poolContentCounts.entryIterator();
	            
	            @Override
	            public boolean hasNext() {
	                return pool_iterator.hasNext();
	            }
	
	            @Override
	            public Entry<byte[], Integer> next() {
	            	
	            	Entry<Integer, Integer> entry = pool_iterator.next();
	            	
	            	//get the next Id from the map and look up the corresponding sequence
	            	return new AbstractMap.SimpleEntry<byte[], Integer>(Configuration.getExperiment().getAptamerPool().getAptamer(entry.getKey()) , entry.getValue());
	            	
	            }
	
	            @Override
	            public void remove() {
	                throw new UnsupportedOperationException();
	            }
	        };
	        return it;
		}	
		
	}
	
	
	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id to the API.
	 */
	private class IdIterator implements Iterable<Integer> {

		public Iterator<Integer> iterator() {
			return poolContentCounts.keyIterator();
		}

	}
	
	public Iterable<Entry<Integer, Integer>> iterator(){
		return new SelectionCycleIterator();
	}

	public Iterable<Entry<byte[], Integer>> sequence_iterator(){
		return new SelectionCycleSequenceIterator();
	}

	@Override
	public Iterable<Integer> id_iterator() {
		return new IdIterator();
	}

	@Override
	public void setBarcodeFivePrime(byte[] barcode) {
		this.barcodeFive = barcode;
	}

	@Override
	public byte[] getBarcodeFivePrime() {
		return this.barcodeFive;
	}

	@Override
	public void setBarcodeThreePrime(byte[] barcode) {
		this.barcodeThree = barcode;
	}

	@Override
	public byte[] getBarcodeThreePrime() {
		return this.barcodeThree;
	}

}
