/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.FileUtilities;

/**
 * Implements the AptamerPool interface using a non-volatile based storage solution in order
 * to minimize memory usage at the expense of speed.
 * @author Jan Hoinka
 *
 */
public class MapDBAptamerPool implements AptamerPool {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1376976629428257697L;

	/**
	 * Path on file system for the current experiment
	 */
	private Path projectPath = null;
	
	/**
	 * Folder in <code>projectPath</code> which contains all the sequencing data
	 * of an experiment that does not fit into volatile memory.
	 */
	private Path poolDataPath = null;
	
	
	/**
	 * The initial space reserved on disk for each map 
	 */
	private Integer allocateStartSize = Configuration.getParameters().getInt("MapDBAllocateStartSize");
	
	
	/**
	 * The amount by which each map will be incremented once it is full 
	 */
	private Integer allocateIncrement = Configuration.getParameters().getInt("MapDBAllocateIncrement");
	
	/**
	 * The maximal number of unique aptamers to store in one TreeMap. The performance of TreeMap decreases
	 * noticeable with large volumes of data. As a workaround, we split the data into buckets of TreeMaps
	 * of size <code>maxItemsPerTreeMap</code>. 
	 */
	private int maxTreeMapCapacity = Configuration.getParameters().getInt("MapDBAptamerPool.maxTreeMapCapacity");

	
	/**
	 * The total number of expected items in the bloom filter.
	 */
	private int bloomFilterCapacity = Configuration.getParameters().getInt("MapDBAptamerPool.bloomFilterCapacity");
	
	
	/**
	 * The expected false positive rate
	 */
	private double bloomFilterCollisionProbability = Configuration.getParameters().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability");
	
	
	/**
	 * The bloom filter is used in order to provide atomic and space efficient checks on 
	 * whether a sequence is already contained in the pool.
	 */
	private transient BloomFilter<String> bloomFilter = new FilterBuilder(bloomFilterCapacity, bloomFilterCollisionProbability).buildBloomFilter(); 
	
	
	/**
	 * List of bloom filters, one per entry in <code>poolData</code>. This will be used to speed up
	 * retrieval time of sequences.
	 */
	private transient List<BloomFilter<String>> poolDataBloomFilter = new ArrayList<BloomFilter<String>>();
	
	
	/**
	 * Collection of HashMaps backed by MapDB which stores aptamer data on disk for
	 * memory efficiency.
	 */
	private transient List<HTreeMap<byte[], Integer>> poolData = new ArrayList<HTreeMap<byte[], Integer>>();

	/**
	 * HashMap backed by MapDB which stores aptamer boundary data on disk for
	 * memory efficiency. Here, boundary stands for start (inclusive) and end (exclusive) index 
	 * of the randomized region.
	 */
	private transient List<BTreeMap<Integer, int[]>> boundsData = new ArrayList<BTreeMap<Integer, int[]>>();
	
	/**
	 * The inverse view of the <code>poolData</code> mapping ids to aptamers
	 */
	private transient List<BTreeMap<Integer, byte[]>> poolDataInverse = new ArrayList<BTreeMap<Integer, byte[]>>();
	
	
	/**
	 * Fast lookup of membership for the inverse pool
	 */
	private BitSet poolDataInverseFilter = new BitSet(bloomFilterCapacity);
	
	
	/**
	 * List of BitSets, one per entry in <code>poolDataInverse</code>. This will be used to speed up
	 * retrieval time of sequences.
	 */
	private transient List<BitSet> poolDataInverseFilters = new ArrayList<BitSet>();
	
	
	
	/**
	 * Stores the file locations of the mapdb instance in <code>poolData</code>.
	 */
	private List<Path> poolDataPaths = new ArrayList<Path>();

	
	/**
	 * The number of elements of the tree map that is currently been filled.
	 * We need to keep this record separately as the .size() function of dbmap
	 * objects is noticeably slow.
	 */
	private int currentTreeMapSize = 0;
	
	/**
	 * The total number of all items stored in this class
	 */
	private int poolSize = 0;
	
	/**
	 * Constructor
	 * @param projectPath must point to the current projects working directory
	 * all files related to the analysis of the HT-SELEX experiment are stored 
	 * in that path. It must exist and be writable for the user.
	 * @param newdb if true, a new database is created on file. 
	 * Any previously existing database will be deleted. If false, the existing database 
	 * will be read from disk.
	 * @throws FileNotFoundException if projectPath does not exist on file system.
	 */
	public MapDBAptamerPool(Path projectPath, boolean newdb) throws IOException{
		
		AptaLogger.log(Level.INFO, this.getClass(), "Instantiating MapDBAptamerPool");
		
		// Time it for logging purposes
		long tReadFromDisk = System.currentTimeMillis();
		
		// Make sure the folder is writable
		// Files.isWritable() might fail on different platforms and 
		// permission combinations, hence we need to check it manually.
		try{
			File sample = new File(projectPath.toFile(), "deleteme.txt");
			sample.createNewFile();
			sample.delete();
		}
		catch (IOException e){
			AptaLogger.log(Level.SEVERE, this.getClass(),"The project path " + projectPath.toString() + " is not writable.");
			throw (new IllegalStateException("The project path " + projectPath.toString() + " is not writable.") );
		}
		
		// Set the project path and pool data path
		this.projectPath = projectPath;
		
		// Check if the data path exists, and if not create it
		this.poolDataPath = Files.createDirectories(Paths.get(this.projectPath.toString(), "pooldata"));
		
		
		// If we are reading an existing database, iterate over the folder and open the individual MapDB instances
		if (! newdb){ 
			AptaLogger.log(Level.INFO, this.getClass(), "Searching for existing datasets in " + poolDataPath.toString());
			
								
			// Get the correct order of the paths
			List<Path> sorted_paths = FileUtilities.getSortedPaths(Files.newDirectoryStream(Paths.get(poolDataPath.toString()), "data*" ));
			
			AptaLogger.log(Level.INFO, this.getClass(), "Found a total of " + sorted_paths.size() + " files on disk.");
			
			// Process them in the correct order
			for (Path file : sorted_paths) { // this will only get the data*.mapdb 
                
    			// Open and read the TreeMap
				AptaLogger.log(Level.INFO, this.getClass(), "Processing " + file.toString());
				long tParserStart = System.currentTimeMillis();
		
				DB db = this.getMapDBInstance(file.toFile(),false);
				
				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
						.keySerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
						.valueSerializer(Serializer.INTEGER)
						.open();
				
				poolData.add(dbmap);
				poolDataPaths.add(file);

				// Setup helper variables	    				
				BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
				poolDataBloomFilter.add(localBloomFilter);
				
				// Now load and initialize the inverse view of the data
				Path inverseFile = Paths.get(file.getParent().toString(), "inverse_" + file.getFileName().toString());
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reading inverse view from " + inverseFile.toString());
				
				DB db_inverse = this.getMapDBInstance(inverseFile.toFile(),false);

				BTreeMap<Integer, byte[]> inverse_dbmap = db_inverse.treeMap("map")
						.valuesOutsideNodesEnable()
						.keySerializer(Serializer.INTEGER)
						.valueSerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
				        .open();
				
				poolDataInverse.add(inverse_dbmap);
				
				// Setup helper variables	    				
				BitSet localBitSet = new BitSet(maxTreeMapCapacity);
				poolDataInverseFilters.add(localBitSet);
				
				// Update bloom filter content and determine database size
				final AtomicInteger currentDBmapSize = new AtomicInteger(0);
				inverse_dbmap.forEach((key,value) -> {
					
					bloomFilter.addRaw(value);
					localBloomFilter.addRaw(value);
					
					this.poolDataInverseFilter.set(key);
					localBitSet.set(key);
					
					currentDBmapSize.getAndIncrement();
					
				});
				
				// Update values
				poolSize += currentDBmapSize.intValue();
				currentTreeMapSize = currentDBmapSize.intValue();
				
				// And finally load the bounds data
				Path boundsFile = Paths.get(file.getParent().toString(), "bounds_" + file.getFileName().toString());
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reading bounds data from " + boundsFile.toString());
				
				DB db_bounds = this.getMapDBInstance(boundsFile.toFile(),false);

				BTreeMap<Integer, int[]> bounds_dbmap = db_bounds.treeMap("map")
						.valuesOutsideNodesEnable()
						.keySerializer(Serializer.INTEGER)
						.valueSerializer(Serializer.INT_ARRAY)
				        .open();
				
				boundsData.add(bounds_dbmap);
				
				
				AptaLogger.log(Level.CONFIG, this.getClass(), 
						"Total number of aptamers in file: " + currentTreeMapSize + "\n" +
						"Total number of aptamers: " + poolSize + "\n" + 
						"Processed in " + ((System.currentTimeMillis() - tParserStart) / 1000.0) + " seconds."
						);
                
            }
			
			AptaLogger.log(Level.INFO, this.getClass(), "Found and loaded a total of " + poolSize + " aptamers from disk.");

			
		}
		else{ 
			
		// Create an empty instance of the MapDB Container...
		Path file = Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb");
		DB db = this.getMapDBInstance(file.toFile(),false);

		HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
				.keySerializer(Serializer.BYTE_ARRAY)
				.valueSerializer(Serializer.INTEGER)
		        .create();
		
		poolDataPaths.add(file);
		poolData.add(dbmap);
		
		BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
		poolDataBloomFilter.add(localBloomFilter);
		
		currentTreeMapSize = 0;
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created new file " + file.toString());

		
		
		// ... as well as a new bounds file
		Path boundsfile = Paths.get(file.getParent().toString(), "bounds_" + file.getFileName().toString());
		
		DB db_bounds = this.getMapDBInstance(boundsfile.toFile(),false);

		BTreeMap<Integer, int[]> dbmap_bounds = db_bounds.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INT_ARRAY)
				.create();
		
		boundsData.add(dbmap_bounds);
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created new bounds file " + boundsfile.toString());
		
		
	
		// Create the inverse view files
		Path inverse_file = Paths.get(file.getParent().toString(), "inverse_" + file.getFileName().toString());
		DB db_inverse = this.getMapDBInstance(inverse_file.toFile(),false);

		BTreeMap<Integer, byte[]> dbmap_inverse = db_inverse.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
		        .create();
		
		//add and create filters
		poolDataInverse.add(dbmap_inverse);
		this.poolDataInverseFilters.add(new BitSet(maxTreeMapCapacity));
		
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created new inverse file " + Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toFile());
		
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "AptamerPool instantiation took " + ((System.currentTimeMillis() - tReadFromDisk) / 1000.0) + " seconds");
		
	}
		
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#registerAptamer(byte[] a)
	 */
	public synchronized int registerAptamer(byte[] a, int rr_start, int rr_end){
		
		// Check if the item is already registered, and if so, return its identifier
		int identifier = this.getIdentifier(a);
		if (identifier != -1){
			return identifier;
		}
		
		// Check that the current map is not at max capacity and create a new map if that is the case
		if (currentTreeMapSize == maxTreeMapCapacity){
			
			// Forward View
			Path file = Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb");
			
			AptaLogger.log(Level.CONFIG, this.getClass(), 
					"Current Map is at max capacity creating new file " + file.toString() + "\n" +
					"Total number of aptamers: " + poolSize 
					);
			
			DB db = this.getMapDBInstance(file.toFile(),false);

			HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
					.keySerializer(Serializer.BYTE_ARRAY)
					.valueSerializer(Serializer.INTEGER)
			        .create();

			poolDataPaths.add(file);
			poolData.add(dbmap);
			
			BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
			poolDataBloomFilter.add(localBloomFilter);
			
			// Reverse View
			Path inverse_file = Paths.get(file.getParent().toString(), "inverse_" + file.getFileName().toString());
			
			DB db_inverse = this.getMapDBInstance(inverse_file.toFile(),false);

			BTreeMap<Integer, byte[]> dbmap_inverse = db_inverse.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
			        .create();

			// Add and create filters
			poolDataInverse.add(dbmap_inverse);
			this.poolDataInverseFilters.add(new BitSet(maxTreeMapCapacity));
			
			
			// Bounds
			Path bounds_file = Paths.get(file.getParent().toString(), "bounds_" + file.getFileName().toString());
			
			DB db_bounds = this.getMapDBInstance(bounds_file.toFile(),false);

			BTreeMap<Integer, int[]> dbmap_bounds = db_bounds.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(Serializer.INT_ARRAY)
			        .create();
			
			// Add
			boundsData.add(dbmap_bounds);
			
			// Reset current capacity
			currentTreeMapSize = 0;
			
		}

		// Now insert the sequence
		poolData.get(poolData.size()-1).put(a, ++poolSize);
		currentTreeMapSize++;
		bloomFilter.addRaw(a);
		poolDataBloomFilter.get(poolData.size()-1).addRaw(a);
		
		poolDataInverse.get(poolDataInverse.size()-1).put(poolSize, a);
		poolDataInverseFilter.set(poolSize);
		poolDataInverseFilters.get(poolData.size()-1).set(poolSize);
		
		// and the bounds data
		boundsData.get(boundsData.size()-1).put(poolSize, new int[]{rr_start,rr_end});
		
		return poolSize;
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#registerAptamer(byte[] a)
	 */
	public int registerAptamer(String a, int rr_start, int rr_end){
		
		return registerAptamer(a.getBytes(), rr_start, rr_end);
	
	}

	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#getIdentifier(byte[] a)
	 */
	public int getIdentifier(byte[] a) {
		
		// Check for existence using bloom filter. 
		if (!bloomFilter.contains(a)){
			return -1; // This result is always accurate (no false negatives)
		}
		
		Integer identifier = null;
		
		// Iterate over all treeMaps and bloomFilters
		ListIterator<HTreeMap<byte[], Integer>> lim = poolData.listIterator(poolData.size());
		ListIterator<BloomFilter<String>> lib = poolDataBloomFilter.listIterator(poolData.size());
		
		// Iterate in reverse
		while(lim.hasPrevious() && identifier == null) {
			
			// Prevent expensive disk lookups by using the bloom filters...
			if(! lib.previous().contains(a) ){
				lim.previous();
				continue;
			}
			
			// ... and only look it up when we have to
			identifier = lim.previous().get(a); //note, in case of a false positive, this returns null

		}
		
		if (identifier == null){identifier = -1;}
		
		return identifier;
	}

	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#getIdentifier(byte[] a)
	 */
	public int getIdentifier(String a) {
		
		return getIdentifier(a.getBytes());
		
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#getAptamer(int id)
	 */
	public byte[] getAptamer(int id) {
		
		// Check if the aptamer is present in the pool via fast lookup
		if (!containsAptamer(id)){
			return null;
		}
		
		// find and return the sequence
		byte[] aptamer = null;
		
		ListIterator<BTreeMap<Integer, byte[]>> lim = poolDataInverse.listIterator(poolDataInverse.size());
		ListIterator<BitSet> lib = poolDataInverseFilters.listIterator(poolDataInverse.size());
		
		// Iterate in reverse
		while(lim.hasPrevious() && aptamer == null) {
			
			// Prevent expensive disk lookups by using the bloom filters...
			if(! lib.previous().get(id) ){
				lim.previous();
				continue;
			}
			
			// ... and only look it up when we have to
			aptamer = lim.previous().get(id); //note, in case of a false positive, this returns null

		}
		
		return aptamer; 
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#getAptamerBounds(int id)
	 */
	public AptamerBounds getAptamerBounds(int id) {
		
		// Check if the aptamer is present in the pool via fast lookup
		if (!containsAptamer(id)){
			return null;
		}
		
		ListIterator<BTreeMap<Integer, int[]>> lim = boundsData.listIterator(boundsData.size());
		ListIterator<BitSet> lib = poolDataInverseFilters.listIterator(poolDataInverse.size());
		AptamerBounds bounds = null;		
		
		// Iterate in reverse
		while(lim.hasPrevious()) {
			
			// Prevent expensive disk lookups by using the bloom filters...
			if(! lib.previous().get(id) ){
				lim.previous();
				continue;
			}
			
			// ... and only look it up when we know where
			bounds = new AptamerBounds(lim.previous().get(id));
		}
		
		return bounds; 
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#hasAptamer(byte[])
	 */
	public Boolean containsAptamer(byte[] a) {
		
		return getIdentifier(a) != -1;

	}

	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#hasAptamer(java.lang.String)
	 */
	public Boolean containsAptamer(String a) {
		
		return getIdentifier(a.getBytes()) != -1;

	}	
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#containsAptamer(int)
	 */
	public Boolean containsAptamer(int id) {
		
		return this.poolDataInverseFilter.get(id);
				
	}
	
	@Override
	public int size() {
		return poolSize;
	}
	
	
	/**
	 * This function closes all the file handles that where created or opened
	 * during the lifetime of the class instance. If it is not called, a
	 * <code>org.mapdb.DBException$DataCorruption</code> will be thrown on the
	 *  next execution of the application when using the same project path.
	 */
	public void close(){
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Closing pool file handles.");
		
		// Iterate over each TreeMap instance and close it
		ListIterator<HTreeMap<byte[], Integer>> li = poolData.listIterator(poolData.size());
		while(li.hasPrevious()) { li.previous().close(); }

		// Close the bounds data
		ListIterator<BTreeMap<Integer, int[]>> bi = boundsData.listIterator(boundsData.size());
		while(bi.hasPrevious()) { bi.previous().close(); }

		// Close the inverse view
		ListIterator<BTreeMap<Integer, byte[]>> lii = poolDataInverse.listIterator(poolDataInverse.size());
		while(lii.hasPrevious()) { lii.previous().close(); }
	}
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.pool.AptamerPool#clear()
	 */
	public void clear(){
		
		// Make sure all file handles are closed before deleting the files.
		this.close();
		
		// Now delete all the content in the project folder
		AptaLogger.log(Level.CONFIG, this.getClass(), "Deleting all content in " + poolDataPath.toString());
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(poolDataPath.toString()))) {
			
			for (Path file : directoryStream) {
				
				Files.delete(file);
			}
			
        } catch (IOException ex) {}
		
		// Reset the pool data
		this.poolData.clear();
		this.poolDataPaths.clear();

		// Reset the bounds data
		this.boundsData.clear();
		
		// Reset the inverse pool data
		this.poolDataInverse.clear();
		this.poolDataInverseFilter.clear();
		this.poolDataInverseFilters.clear();
		
		// Reset the bloom filters
		bloomFilter.clear();
		poolDataBloomFilter.clear();
		
		// Reset the counts
		this.poolSize = 0;
		this.currentTreeMapSize = 0;
		
	}
	
	
	/**
	 * Sets the maximal number of items per TreeMap. This operation is only valid
	 * for new data sets and an exception is thrown if this function is called on 
	 * existing data sets.
	 */
	public void setMaxTreeMapCapacity(int maxCapacity){
		
		if (poolSize != 0){
			throw new UnsupportedOperationException("Cannot change the maximal capacity for existing projects.");
		}
		
		this.maxTreeMapCapacity = maxCapacity;
	}
	
	
	/**
	 * Set the new number of expected elements to be inserted into the bloom filter.
	 * Note, this function can only be called before the first aptamer is registered 
	 * using <code>registerAptamer(String)</code>. Otherwise an exception is thrown
	 * @param capacity
	 */
	public void setBloomFilterCapacity(int capacity) throws UnsupportedOperationException{
		
		// Make sure no aptamer has been registered
		if (this.size() != 0)
		{
			throw new UnsupportedOperationException("Cannot change bloom filter capacity. The pool is not empty.");
		}
		
		this.bloomFilterCapacity = capacity;
		bloomFilter = new FilterBuilder(bloomFilterCapacity, bloomFilterCollisionProbability).buildBloomFilter();
		
	}
	
	
	/**
	 * Returns the current bloom filter capacity
	 * @return
	 */
	public int getBloomFilterCapacity(){
		
		return this.bloomFilterCapacity;
	
	}
	

	/**
	 * Set the new collision probability for the bloom filter.
	 * Note, this function can only be called before the first aptamer is registered 
	 * using <code>registerAptamer(String)</code>. Otherwise an exception is thrown
	 * @param capacity
	 */
	public void setBloomFilterCollisionProbability(double prob) throws UnsupportedOperationException{
		
		// Make sure no aptamer has been registered
		if (this.size() != 0)
		{
			throw new UnsupportedOperationException("Cannot change bloom filter capacity. The pool is not empty.");
		}
		
		this.bloomFilterCollisionProbability = prob;
		bloomFilter = new FilterBuilder(bloomFilterCapacity, bloomFilterCollisionProbability).buildBloomFilter();
		
	}
	
	
	/**
	 * Returns the current bloom filter capacity
	 * @return
	 */
	public double getBloomFilterCollisionProbability(){
		
		return this.bloomFilterCollisionProbability;
	
	}	
	
	 @Override
    public void setReadOnly(){
    	
    	// close all the file handles
    	close();
    	
    	// clear references
    	poolData.clear();
    	poolDataInverse.clear();
    	boundsData.clear();
    	
    	// reopen as read only
		for (Path file : poolDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = this.getMapDBInstance(file.toFile(),true);

				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
						.keySerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
						.valueSerializer(Serializer.INTEGER)
						.open();
				
				poolData.add(dbmap);
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + file.toString() );
			}
			
			// Do the same for the inverse files
			Path inverse_file = Paths.get(file.getParent().toString(), "inverse_"+ file.getFileName().toString());
			DB db_inverse = this.getMapDBInstance(inverse_file.toFile(),true);

			BTreeMap<Integer, byte[]> dbmap_inverse = db_inverse.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
			        .open();
			
			poolDataInverse.add(dbmap_inverse);
			
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + inverse_file.toString() );
			
			
			// Do the same for the inverse files
			Path bounds_file = Paths.get(file.getParent().toString(), "bounds_"+ file.getFileName().toString());
			DB db_bounds = this.getMapDBInstance(bounds_file.toFile(),true);

			BTreeMap<Integer, int[]> dbmap_bounds = db_bounds.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(Serializer.INT_ARRAY)
			        .open();
			
			boundsData.add(dbmap_bounds);
			
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + bounds_file.toString() );

        }
		
    }	
	
	@Override
    public void setReadWrite(){
    	
    	// close all the file handles
    	close();
    	
    	// clear references
    	poolData.clear();
    	poolDataInverse.clear();
    	boundsData.clear();
    	
    	// reopen as read/write
		for (Path file : poolDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = this.getMapDBInstance(file.toFile(),true);

				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
						.keySerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
						.valueSerializer(Serializer.INTEGER)
						.open();
				
				poolData.add(dbmap);
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + file.toString() );
			}
			
			// Do the same for the inverse files
			Path inverse_file = Paths.get(file.getParent().toString(), "inverse_"+ file.getFileName().toString());
			DB db_inverse = this.getMapDBInstance(inverse_file.toFile(),true);

			BTreeMap<Integer, byte[]> dbmap_inverse = db_inverse.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper<byte[]>(Serializer.BYTE_ARRAY))
			        .open();
			
			poolDataInverse.add(dbmap_inverse);
			
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + inverse_file.toString() );
            
			
			// Do the same for the bounds files
			Path bounds_file = Paths.get(file.getParent().toString(), "bounds_"+ file.getFileName().toString());
			DB db_bounds = this.getMapDBInstance(bounds_file.toFile(),true);

			BTreeMap<Integer, int[]> dbmap_bounds = db_bounds.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(Serializer.INT_ARRAY)
			        .open();
			
			boundsData.add(dbmap_bounds);
			
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + bounds_file.toString() );
			
        }
		
    }		 

	/**
	 * Make use of internal classes so we can provide iterators for aptamer->id and id->aptamer to the API.
	 * This class implements the aptamer->id view.
	 * Eg. <code>for ( pool_it : pool.iterator() ){ }</code>
	 */
	private class PoolIterator implements Iterable<Entry<byte[], Integer>> {
//
//		@Override
//	    public Iterator<Entry<byte[], Integer>> iterator() {
//	        Iterator<Map.Entry<byte[], Integer>> it = new Iterator<Map.Entry<byte[], Integer>>() {
//
//	            private int currentTreeMapIndex = 0;
//	            private Iterator<Entry<byte[], Integer>> currentTreeMapIterator= poolData.get(currentTreeMapIndex).getEntries().iterator();
//	            
//	            @Override
//	            public boolean hasNext() {
//	                return currentTreeMapIterator.hasNext() || currentTreeMapIndex < poolData.size()-1;
//	            }
//
//	            @Override
//	            public Entry<byte[], Integer> next() {
//	            	
//	            	// Move on to the next map if all items from the previous have been iterated over
//	                if (!currentTreeMapIterator.hasNext() && currentTreeMapIndex < poolData.size()-1)
//	                {
//	                	currentTreeMapIndex++;
//	                	currentTreeMapIterator= poolData.get(currentTreeMapIndex).getEntries().iterator();
//	                }
//	                
//	                return currentTreeMapIterator.next();
//	            }
//
//	            @Override
//	            public void remove() {
//	                throw new UnsupportedOperationException();
//	            }
//	        };
//	        return it;
//	    }
		
		@Override
		public Iterator<Entry<byte[], Integer>> iterator() {
			Iterator<Map.Entry<byte[], Integer>> it = new Iterator<Map.Entry<byte[], Integer>>() {

	            // Use the inverse map for iteration as its underlaying BTree structure
	            // is several orders faster to traverse
	            private int currentTreeMapIndex = 0;
	            private Iterator<Entry<Integer, byte[]>> currentTreeMapIterator= poolDataInverse.get(currentTreeMapIndex).getEntries().iterator();
	            
	            @Override
	            public boolean hasNext() {
	                return currentTreeMapIterator.hasNext() || currentTreeMapIndex < poolData.size()-1;
	            }

	            @Override
	            public Entry<byte[], Integer> next() {
	            	
	            	// Move on to the next map if all items from the previous have been iterated over
	                if (!currentTreeMapIterator.hasNext() && currentTreeMapIndex < poolData.size()-1)
	                {
	                	currentTreeMapIndex++;
	                	currentTreeMapIterator= poolDataInverse.get(currentTreeMapIndex).getEntries().iterator();
	                }
	                
	                Entry<Integer, byte[]> temp = currentTreeMapIterator.next();
	                return new AbstractMap.SimpleEntry<byte[], Integer>(temp.getValue(), temp.getKey());
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
	 * Make use of internal classes so we can provide iterators for id.
	 */
	private class IdIterator implements Iterable<Integer> {

		@Override
	    public Iterator<Integer> iterator() {
	        
			Iterator<Integer> it = new Iterator<Integer>() {

	            private int currentTreeMapIndex = 0;
	            private Iterator<Integer> currentTreeMapIterator= poolDataInverse.get(currentTreeMapIndex).getKeys().iterator();
	            
	            @Override
	            public boolean hasNext() {
	                return currentTreeMapIterator.hasNext() || currentTreeMapIndex < poolDataInverse.size()-1;
	            }

	            @Override
	            public Integer next() {
	            	
	            	// Move on to the next map if all items from the previous have been iterated over
	                if (!currentTreeMapIterator.hasNext() && currentTreeMapIndex < poolDataInverse.size()-1)
	                {
	                	currentTreeMapIndex++;
	                	currentTreeMapIterator= poolDataInverse.get(currentTreeMapIndex).getKeys().iterator();
	                }
	                	
	                return currentTreeMapIterator.next();
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
	 * Internal class implementing the iterator of the inverse view of the pool content, 
	 * i.e id->aptamer
	 */
	private class InverseViewPoolIterator implements Iterable<Entry<Integer,byte[]>> {
		
		@Override
		public Iterator<Entry<Integer,byte[]>> iterator(){
	        Iterator<Map.Entry<Integer, byte[]>> it = new Iterator<Map.Entry<Integer, byte[]>>() {

	            private int currentTreeMapIndex = 0;
	            private Iterator<Entry<Integer, byte[]>> currentTreeMapIterator= poolDataInverse.get(currentTreeMapIndex).getEntries().iterator();
	            
	            @Override
	            public boolean hasNext() {
	                return currentTreeMapIterator.hasNext() || currentTreeMapIndex < poolData.size()-1;
	            }

	            @Override
	            public Entry<Integer, byte[]> next() {
	            	
	            	// Move on to the next map if all items from the previous have been iterated over
	                if (!currentTreeMapIterator.hasNext() && currentTreeMapIndex < poolData.size()-1)
	                {
	                	currentTreeMapIndex++;
	                	currentTreeMapIterator= poolDataInverse.get(currentTreeMapIndex).getEntries().iterator();
	                }
	                	
	                return currentTreeMapIterator.next();
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
	 * Internal class implementing the iterator of the aptamer bounds
	 * i.e id->bounds
	 */
	private class BoundsIterator implements Iterable<Entry<Integer,int[]>> {
		
		@Override
		public Iterator<Entry<Integer,int[]>> iterator(){
	        Iterator<Map.Entry<Integer, int[]>> it = new Iterator<Map.Entry<Integer, int[]>>() {

	            private int currentTreeMapIndex = 0;
	            private Iterator<Entry<Integer, int[]>> currentTreeMapIterator= boundsData.get(currentTreeMapIndex).getEntries().iterator();
	            
	            @Override
	            public boolean hasNext() {
	                return currentTreeMapIterator.hasNext() || currentTreeMapIndex < boundsData.size()-1;
	            }

	            @Override
	            public Entry<Integer, int[]> next() {
	            	
	            	// Move on to the next map if all items from the previous have been iterated over
	                if (!currentTreeMapIterator.hasNext() && currentTreeMapIndex < poolData.size()-1)
	                {
	                	currentTreeMapIndex++;
	                	currentTreeMapIterator= boundsData.get(currentTreeMapIndex).getEntries().iterator();
	                }
	                	
	                return currentTreeMapIterator.next();
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
     * Provides public access to the iterator, since <code>PoolCollection</code> implements 
     * <code>Iterable</code>.
     * @return Instance of <code>PoolCollection</code>.
     */
    public Iterable<Entry<byte[], Integer>> iterator(){
    	return new PoolIterator();
    }
    
    /**
     * Provides public access to the iterator of the inverse view of the pool.
     * @return Instance of <code>PoolCollectionInverse</code>
     */
    public Iterable<Entry<Integer,byte[]>> inverse_view_iterator(){
    	return new InverseViewPoolIterator();
    }

    /**
     * Provides public access to the iterator of the bounds data
     * @return Instance of <code>BoundsIterator</code>
     */
	@Override
	public Iterable<Entry<Integer, int[]>> bounds_iterator() {
		return new BoundsIterator();
	}

    
	@Override
	public Iterable<Integer> id_iterator() {
		
		return new IdIterator();
		
	}

	/**
	 * Central getter to open a channel to a mapdb on file
	 * @param file
	 */
	private DB getMapDBInstance(File file, boolean readonly) {
		
		DB db;
		
		if (!readonly) {
			db = DBMaker
			    .fileDB(file)
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .fileMmapPreclearDisable() // Make mmap file faster
			    //.cleanerHackEnable() 	// Unmap (release resources) file when its closed. 
			    						//Note that this is not compatible with MacOS and Java 9...
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .fileChannelEnable() 
			    .make();
		}
		
		else {
			db = DBMaker
			    .fileDB(file)
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .fileMmapPreclearDisable() // Make mmap file faster
			    //.cleanerHackEnable() 	// Unmap (release resources) file when its closed. 
										//Note that this is not compatible with MacOS and Java 9...
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .fileChannelEnable()
			    .readOnly()
			    .make();
		}
		
		return db;
		
	}

}