/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import exceptions.InvalidConfigurationException;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * Implements the AptamerPool interface using a non-volatile based storage solution in order
 * to minimize memory usage at the expense of speed.
 * @author Jan Hoinka
 *
 */
public class MapDBAptamerPool implements AptamerPool {
	
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
	private transient BloomFilter<String> bloomFilter = new FilterBuilder(bloomFilterCapacity, bloomFilterCollisionProbability).buildBloomFilter(); //TODO: make parameters class vars and implement getters and setters
	
	
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
	 * The inverse view of the <code>poolData</code> mapping ids to aptamers
	 */
	private transient BTreeMap<Integer, byte[]> poolDataInverse = null;
	
	
	/**
	 * Fast lookup of membership for <code>poolDataInverse</code>
	 */
	private BitSet poolDataInverseFilter = new BitSet(bloomFilterCapacity);
	
	
	/**
	 * Stores the file locations of the mapdb instance in <code>poolData</code>.
	 */
	private List<Path> poolDataPaths = new ArrayList<Path>();
	
	
	/**
	 * The maximal number of unique aptamers to store in one TreeMap. The performance of TreeMap decreases
	 * noticeable with large volumes of data. As a workaround, we split the data into buckets of TreeMaps
	 * of size <code>maxItemsPerTreeMap</code>. 
	 */
	private int maxTreeMapCapacity = Configuration.getParameters().getInt("MapDBAptamerPool.maxTreeMapCapacity");
	
	
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
		
		// Make sure the folder is writable
		if (!Files.isWritable(projectPath)){
			AptaLogger.log(Level.SEVERE, this.getClass(),"The project path is not writable.");
			throw (new IllegalStateException("The project path is not writable.") );
		}
		
		// Set the project path and pool data path
		this.projectPath = projectPath;
		
		// Check if the data path exists, and if not create it
		this.poolDataPath = Files.createDirectories(Paths.get(this.projectPath.toString(), "pooldata"));
		
		
		// If we are reading an existing database, iterate over the folder and open the individual MapDB instances
		if (! newdb){ 
			AptaLogger.log(Level.INFO, this.getClass(), "Searching for existing datasets in " + poolDataPath.toString());
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(poolDataPath.toString()))) {
				
				for (Path file : directoryStream) {
	                
	    			// Open and read the TreeMap, skip the inverse file
	    			if (Files.isRegularFile(file) && !file.getFileName().toString().equals("data_inverse.mapdb") ){
	    				
	    				AptaLogger.log(Level.INFO, this.getClass(), "Processing " + file.getFileName().toString());
	    				long tParserStart = System.currentTimeMillis();
	    		
	    				DB db = DBMaker
	    					    .fileDB(file.toFile())
	    					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
	    					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
	    					    .executorEnable()
	    					    .make();
	
	    				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
	    						.keySerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
	    						.valueSerializer(Serializer.INTEGER)
	    						.open();
	    				
	    				poolData.add(dbmap);
	    				poolDataPaths.add(file);
	    				
	    				BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
	    				poolDataBloomFilter.add(localBloomFilter);
	    				
	    				
	    				// Update values
	    				int currentDBmapSize = dbmap.size();
	    				poolSize += currentDBmapSize;
	    				currentTreeMapSize = currentDBmapSize;

	    				// Update bloom filter content
	    				Iterator<byte[]> dbmapIterator = dbmap.getKeys().iterator();
	    				while (dbmapIterator.hasNext()){
	    					byte[] current_element = dbmapIterator.next(); 
	    					bloomFilter.add(current_element);
	    					localBloomFilter.add(current_element);
	    				}
	    				
	    				AptaLogger.log(Level.CONFIG, this.getClass(), 
	    						"Found and loaded file " + file.toString() + "\n" +
	    						"Total number of aptamers in file: " + currentTreeMapSize + "\n" +
	    						"Total number of aptamers: " + poolSize + "\n" + 
	    						"Processed in " + ((System.currentTimeMillis() - tParserStart) / 1000.0) + " seconds."
	    						);
	    			}
	                
	            }
	        } catch (IOException ex) {}
			
			// Now load and initialize the inverse view of the data
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reading inverse view.");
			
			DB db_inverse = DBMaker
				    .fileDB(Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toFile())
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();

			poolDataInverse = db_inverse.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
			        .open();
			
			// Update the filter content
			Iterator<Integer> inverse_iterator = poolDataInverse.keyIterator();
			while (inverse_iterator.hasNext()){
				poolDataInverseFilter.set(inverse_iterator.next());
			}
			
			AptaLogger.log(Level.INFO, this.getClass(), "Found and loaded a total of " + poolSize + " aptamers on disk.");
			
		}
		else{ // Create an empty instance of the MapDB Container

		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
				.keySerializer(Serializer.BYTE_ARRAY)
				.valueSerializer(Serializer.INTEGER)
		        .create();
		
		poolDataPaths.add(Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb"));
		poolData.add(dbmap);

		BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
		poolDataBloomFilter.add(localBloomFilter);
		
		currentTreeMapSize = 0;
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created new file " + Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toString());
	
		// Create the inverse view files
		DB db_inverse = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		poolDataInverse = db_inverse.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
		        .create();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created new file " + Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toFile());
		
		}
	}
		
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#registerAptamer(byte[] a)
	 */
	public synchronized int registerAptamer(byte[] a){
		
		// Check if the item is already registered, and if so, return its identifier
		int identifier = this.getIdentifier(a);
		if (identifier != -1){
			return identifier;
		}
		
		// Check that the current map is not at max capacity and create a new map if that is the case
		if (currentTreeMapSize == maxTreeMapCapacity){
			
			AptaLogger.log(Level.CONFIG, this.getClass(), 
					"Current Map is at max capacity creating new file " + Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toString() + "\n" +
					"Total number of aptamers: " + poolSize 
					);
			
			DB db = DBMaker
				    .fileDB(Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toFile())
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();

			HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
					.keySerializer(Serializer.BYTE_ARRAY)
					.valueSerializer(Serializer.INTEGER)
			        .create();

			poolDataPaths.add(Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb"));
			poolData.add(dbmap);
			
			BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
			poolDataBloomFilter.add(localBloomFilter);
			
			currentTreeMapSize = 0;
			
		}

		// Now insert the sequence
		poolData.get(poolData.size()-1).put(a, ++poolSize);
		currentTreeMapSize++;
		bloomFilter.add(a);
		poolDataBloomFilter.get(poolData.size()-1).add(a);
		
		poolDataInverse.put(poolSize, a);
		poolDataInverseFilter.set(poolSize);
		
		return poolSize;
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#registerAptamer(byte[] a)
	 */
	public int registerAptamer(String a){
		
		return registerAptamer(a.getBytes());
	
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
		return this.poolDataInverse.get(id);
		
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
		
		// Iterate over each TreeMap instance and close it
		ListIterator<HTreeMap<byte[], Integer>> li = poolData.listIterator(poolData.size());

		// Iterate in reverse
		while(li.hasPrevious()) {
			
			li.previous().close();

		}
		
		// Close the inverse view
		poolDataInverse.close();
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
		
		// Reset the inverse pool data
		this.poolDataInverse.clear();
		this.poolDataInverseFilter.clear();
		
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
    	
    	// reopen as read only
		for (Path file : poolDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = DBMaker
					    .fileDB(file.toFile())
					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
					    .executorEnable()
					    .readOnly()
					    .make();

				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
						.keySerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
						.valueSerializer(Serializer.INTEGER)
						.open();
				
				poolData.add(dbmap);
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + file.toString() );
			}
            
        }
		
		// do the same for the inverse view
		DB db_inverse = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .readOnly()
			    .make();

		poolDataInverse = db_inverse.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
		        .open();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toString() );
    }	
	
	@Override
    public void setReadWrite(){
    	
    	// close all the file handles
    	close();
    	
    	// clear references
    	poolData.clear();
    	
    	// reopen as read/write
		for (Path file : poolDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = DBMaker
					    .fileDB(file.toFile())
					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
					    .executorEnable()
					    .make();

				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
						.keySerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
						.valueSerializer(Serializer.INTEGER)
						.open();
				
				poolData.add(dbmap);
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + file.toString() );
			}
            
        }
		
		// and the inverse view
		DB db_inverse = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		poolDataInverse = db_inverse.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
		        .open();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + Paths.get(poolDataPath.toString(), "data_inverse" + ".mapdb").toString() );
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

    	// open as read only
	    poolData = new ArrayList<HTreeMap<byte[], Integer>>();
	    
	    // since we are opening the maps as read-only, we can set the size of the bloom filters to exactly 
	    // the number of aptamers in the corresponding mapdb contrainers
		bloomFilter = new FilterBuilder(poolSize, bloomFilterCollisionProbability).buildBloomFilter(); 
		poolDataBloomFilter = new ArrayList<BloomFilter<String>>();
		
	    
		for (Path file : poolDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = DBMaker
					    .fileDB(file.toFile())
					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
					    .executorEnable()
					    .readOnly()
					    .make();

				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
						.keySerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
						.valueSerializer(Serializer.INTEGER)
						.open();
				
				poolData.add(dbmap);
				
//				update counts
				BloomFilter<String> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
				poolDataBloomFilter.add(localBloomFilter);
				
				for ( Entry<byte[], Integer> entry : dbmap.getEntries()){
					bloomFilter.add(entry.getKey());
					localBloomFilter.add(entry.getKey());
				}
				
				AptaLogger.log(Level.CONFIG, this.getClass(),  "Reopened as read only file " + file.toString() );
			}
        }
	}
	
	

	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for aptamer->id and id->aptamer to the API.
	 * This class implements the aptamer->id view.
	 * Eg. <code>for ( pool_it : pool.iterator() ){ }</code>
	 */
	private class PoolIterator implements Iterable<Entry<byte[], Integer>> {

		@Override
	    public Iterator<Entry<byte[], Integer>> iterator() {
	        Iterator<Map.Entry<byte[], Integer>> it = new Iterator<Map.Entry<byte[], Integer>>() {

	            private int currentTreeMapIndex = 0;
	            private Iterator<Entry<byte[], Integer>> currentTreeMapIterator= poolData.get(currentTreeMapIndex).getEntries().iterator();
	            
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
	                	currentTreeMapIterator= poolData.get(currentTreeMapIndex).getEntries().iterator();
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
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id.
	 */
	private class IdIterator implements Iterable<Integer> {

		@Override
	    public Iterator<Integer> iterator() {
	        
	        return poolDataInverse.keyIterator();
	        
	    }
	}
	
	/**
	 * @author Jan Hoinka
	 * Internal class implementing the iterator of the inverse view of the pool content, 
	 * i.e id->aptamer
	 */
	private class InverseViewPoolIterator implements Iterable<Entry<Integer,byte[]>> {
		
		@Override
		public Iterator<Entry<Integer,byte[]>> iterator(){
	    	return poolDataInverse.getEntries().iterator();
	    }
		
	}

        
    /**
     * Provide public access to the iterator, since <code>PoolCollection</code> implements 
     * <code>Iterable</code>.
     * @return Instance of <code>PoolCollection</code>.
     */
    public Iterable<Entry<byte[], Integer>> iterator(){
    	return new PoolIterator();
    }
    
    /**
     * Provide public access to the iterator of the inverse view of the pool.
     * @return Instance of <code>PoolCollectionInverse</code>
     */
    public Iterable<Entry<Integer,byte[]>> inverse_view_iterator(){
    	return new InverseViewPoolIterator();
    }

	@Override
	public Iterable<Integer> id_iterator() {
		
		return new IdIterator();
		
	}
}
