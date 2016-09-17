/**
 * 
 */
package lib.aptamer.pool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import logger.AptaLogger;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;

/**
 * Implements the AptamerPool interface using a non-volatile based storage solution in order
 * to minimize memory useage at the expense of speed.
 * @author Jan Hoinka
 *
 */
public class PoolMapDB implements AptamerPool {
	
	/**
	 * Enable logging for debuging and information
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

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
	private int bloomFilterCapacity = 1000000000;
	
	
	/**
	 * The expected false positive rate
	 */
	private double bloomFilterCollisionProbability = 0.001;
	
	
	/**
	 * The bloom filter is used in order to provide atomic and space efficient checks on 
	 * whether a sequence is already contained in the pool.
	 */
	private BloomFilter<String> bloomFilter = new FilterBuilder(bloomFilterCapacity, bloomFilterCollisionProbability).buildBloomFilter(); //TODO: make parameters class vars and implement getters and setters
	
	
	/**
	 * Collection of HashMaps backed by MapDB which stores aptamer data on disk for
	 * memory efficiency.
	 */
	private List<HTreeMap<byte[], Integer>> poolData = new ArrayList<HTreeMap<byte[], Integer>>();
	
	
	/**
	 * The maximal number of unique aptamers to store in one TreeMap. The performance of TreeMap decreases
	 * noticeable with large volumes of data. As a workaround, we split the data into buckets of TreeMaps
	 * of size <code>maxItemsPerTreeMap</code>. 
	 */
	private int maxTreeMapCapacity = 1000000;
	
	
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
	 * in that path. It must exist and be writable for the user the VM is running from.
	 * @throws FileNotFoundException if projectPath does not exist on file system.
	 */
	public PoolMapDB(Path projectPath) throws IOException{
		
		LOGGER.info("Instantiating PoolMapDB");
		
		// Make sure the folder exists
		if (Files.notExists(projectPath)){
			throw (new java.io.FileNotFoundException("The project path does not exist on the file system."));
		}
		
		// Make sure the folder is writable
		if (!Files.isWritable(projectPath)){
			throw (new IllegalStateException("The project path is not writable.") );
		}
		
		// Set the project path and pool data path
		this.projectPath = projectPath;
		
		// Check if the data path exists, and if not create it
		this.poolDataPath = Files.createDirectories(Paths.get(this.projectPath.toString(), "pooldata"));
		
		// Iterate over the folder and open the individual MapDB instances
		LOGGER.info("Searching for existing datasets in " + poolDataPath.toString());
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(poolDataPath.toString()))) {
			
			for (Path file : directoryStream) {
                
    			// Open and read the TreeMap
    			if (Files.isRegularFile(file)){
    				
    				DB db = DBMaker
    					    .fileDB(file.toFile())
    					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
    					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
    					    .executorEnable()
    					    .make();

    				HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
    						.keySerializer(Serializer.BYTE_ARRAY)
    						.valueSerializer(Serializer.INTEGER)
    				        .open();
    				
    				poolData.add(dbmap);
    				
    				// Update values
    				int currentDBmapSize = dbmap.size();
    				poolSize += currentDBmapSize;
    				currentTreeMapSize = currentDBmapSize;
    				
    				// Update bloom filter content
    				Iterator<byte[]> dbmapIterator = dbmap.getKeys().iterator();
    				while (dbmapIterator.hasNext()){
    					bloomFilter.add(dbmapIterator.next());
    				}
    				
    				LOGGER.info(
    						"Found and loaded file " + file.toString() + "\n" +
    						"Total number of aptamers in file: " + currentTreeMapSize + "\n" +
    						"Total number of aptamers: " + poolSize
    						);
    			}
                
            }
        } catch (IOException ex) {}
		
		
		// If no existing mapped were found, create an empty one
		if (poolData.isEmpty()){

			DB db = DBMaker
				    .fileDB(Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toFile())
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();
	
			System.out.println(poolData.size());
			HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
					.keySerializer(Serializer.BYTE_ARRAY)
					.valueSerializer(Serializer.INTEGER)
			        .create();
			
			poolData.add(dbmap);
			currentTreeMapSize = 0;
			
			LOGGER.info("No data found on disk. Created new file Found and loaded file " + Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toString());
		}
		
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#registerAptamer(byte[] a)
	 */
	public int registerAptamer(byte[] a){
		
		// Check if the item is already registered, and if so, return its identifier
		int identifier = this.getIdentifier(a);
		if (identifier != -1){
			return identifier;
		}
		
		// Check that the current map is not at max capacity
		if (currentTreeMapSize == maxTreeMapCapacity){
			
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
			
			poolData.add(dbmap);
			currentTreeMapSize = 0;
		
			LOGGER.info(
					"Current Map is at max capacity creating new file " + Paths.get(poolDataPath.toString(), "data" + String.format("%04d", poolData.size()) + ".mapdb").toString() + "\n" +
					"Total number of aptamers: " + poolSize 
					);
			
		}

		// Now insert the sequence
		poolData.get(poolData.size()-1).put(a, ++poolSize);
		currentTreeMapSize++;
		bloomFilter.add(a);
		
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
		// Note, that the result might be a false negative...
		if (!containsAptamer(a)){
			return -1;
		}
		
		Integer identifier = null;
		
		// Iterate over all treeMaps
		ListIterator<HTreeMap<byte[], Integer>> li = poolData.listIterator(poolData.size());

		// Iterate in reverse
		while(li.hasPrevious() && identifier == null) {
			
			identifier = li.previous().get(a);

		}
		
		// ...so we need to catch the false negatives here.
		if (identifier == null){ identifier = -1;}
		
		return identifier;
	}

	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#getIdentifier(byte[] a)
	 */
	public int getIdentifier(String a) {
		
		return getIdentifier(a.getBytes());
		
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#hasAptamer(byte[])
	 */
	public Boolean containsAptamer(byte[] a) {
		
		// Use bloom filter for efficiency. This way we do not have to go to 
		// disk I/O for each contains call.
		return bloomFilter.contains(a);

	}

	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#hasAptamer(java.lang.String)
	 */
	public Boolean containsAptamer(String a) {
		
		// Use bloom filter for efficiency. This way we do not have to go to 
		// disk I/O for each contains call.
		return bloomFilter.contains(a);

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
		
	}
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.pool.AptamerPool#clear()
	 */
	public void clear(){
		
		// Make sure all file handles are closed before deleting the files.
		this.close();
		
		// Now delete all the content in the project folder
		LOGGER.info("Deleting all content in " + poolDataPath.toString());
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(poolDataPath.toString()))) {
			
			for (Path file : directoryStream) {
				
				Files.delete(file);
			}
			
        } catch (IOException ex) {}
		
		// Reset the pool data
		this.poolData.clear();
		
		// Reset the bloom filter
		bloomFilter.clear();
		
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
	
	
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
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
