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
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import logger.AptaLogger;

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
	 * Collection of HashMaps backed by MapDB which stores aptamer data on disk for
	 * memory efficiency.
	 */
	private List<HTreeMap<byte[], Integer>> poolData = new ArrayList<HTreeMap<byte[], Integer>>();
	
	
	/**
	 * The maximal number of unique aptamers to store in one TreeMap. The performance of TreeMap decreases
	 * noticable with large volumes of data. As a workaround, we split the data into buckets of TreeMaps
	 * of size <code>maxItemsPerTreeMap</code>. 
	 */
	private int maxTreeMapCapacity = 1000000; //TODO: Implement getter and setter
	
	
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
    				poolSize += dbmap.size();
    				currentTreeMapSize = dbmap.size();
    				
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
				    .fileDB(Paths.get(poolDataPath.toString(), "data" + Integer.toString(poolData.size()) + ".mapdb").toFile())
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
			
			LOGGER.info("No data found on disk. Created new file Found and loaded file " + Paths.get(poolDataPath.toString(), "data" + Integer.toString(poolData.size()) + ".mapdb").toString());
		}
		
	}
	
	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#registerAptamer(java.lang.String)
	 */
	public int registerAptamer(String a){
		
		// Check if the item is already registered, and if so, return its identifier
		int identifier = this.getIdentifier(a);
		if (identifier != -1){
			return identifier;
		}
		
		// Check that the current map is not at max capacity
		if (currentTreeMapSize == maxTreeMapCapacity){
			
			DB db = DBMaker
				    .fileDB(Paths.get(poolDataPath.toString(), "data" + Integer.toString(poolData.size()) + ".mapdb").toFile())
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
					"Current Map is at max capacity creating new file " + Paths.get(poolDataPath.toString(), "data" + Integer.toString(poolData.size()) + ".mapdb").toString() + "\n" +
					"Total number of aptamers: " + poolSize 
					);
			
		}

		// Now insert the sequence
		poolData.get(poolData.size()-1).put(a.getBytes(), ++poolSize);
		currentTreeMapSize++;
		
		return poolSize;
	}

	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#getIdentifier(java.lang.String)
	 */
	public int getIdentifier(String a) {
		
		Integer identifier = null;
		
		// Iterate over all treeMaps
		ListIterator<HTreeMap<byte[], Integer>> li = poolData.listIterator(poolData.size());

		// Iterate in reverse
		while(li.hasPrevious() && identifier == null) {
			
			identifier = li.previous().get(a.getBytes());

		}
		
		// Conform to interface definition
		if (identifier == null){ identifier = -1;}
		
		return identifier;
	}

	/* (non-Javadoc)
	 * @see aptamer.pool.AptamerPool#hasAptamer(java.lang.String)
	 */
	public Boolean hasAptamer(String a) {

		Boolean found = false;
		
		// Iterate over all treeMaps
		ListIterator<HTreeMap<byte[], Integer>> li = poolData.listIterator(poolData.size());

		// Iterate in reverse
		while(li.hasPrevious() && !found) {
			
			found = li.previous().containsKey(a.getBytes());

		}
		
		return found;
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

}
