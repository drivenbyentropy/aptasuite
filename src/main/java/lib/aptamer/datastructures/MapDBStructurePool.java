/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Implementation of the StructurePool Interface using ensamble based structure prediction
 * tools such as CapR or SFold.
 * 
 * In this implementation the data of the secondary structure contexts hairpin, bulge, inner,
 * and multi-loop is stored continuously in the <code>double[]</code>. First, all hairpin probabilities, then
 * all bulge probabilities, etc. Hence, a sequence of length <code>N</code> requires <code>4*N<code> 
 * elements of storage.
 */
public class MapDBStructurePool implements StructurePool {

	/**
	 * Path on file system for the current experiment
	 */
	private Path projectPath = null;
	
	
	/**
	 * Folder in <code>projectPath</code> which contains all the sequencing data
	 * of an experiment that does not fit into volatile memory.
	 */
	private Path structureDataPath = null;
	
	/**
	 * Stores the file locations of the mapdb instance in <code>structureData</code>.
	 */
	private List<Path> structureDataPaths = new ArrayList<Path>();
	
	/**
	 * The total number of expected items in the bitSet
	 */
	private int globalFilterCapacity = Configuration.getParameters().getInt("MapDBAptamerPool.bloomFilterCapacity");

	
	/**
	 * The expected false positive rate
	 */
	private double bloomFilterCollisionProbability = Configuration.getParameters().getDouble("MapDBStructurePool.bloomFilterCollisionProbability");
	
	
	/**
	 * Maximal number of items to store in one treemap
	 */
	private int maxTreeMapCapacity = Configuration.getParameters().getInt("MapDBStructurePool.maxTreeMapCapacity");
	
	/**
	 * The structural data of the aptamer 
	 */
	private transient List<BTreeMap<Integer, double[]>> structureData = new ArrayList<BTreeMap<Integer, double[]>>();
	
	
	/**
	 * Fast lookup of membership for <code>structureData</code>
	 */
	private BloomFilter<Integer> globalStructureDataFilter = new FilterBuilder(globalFilterCapacity, bloomFilterCollisionProbability).buildBloomFilter(); 
	
	/**
	 * Fast lookup of membership for <code>structureData</code>
	 */
	private List<BloomFilter<Integer>> structureDataFilter = new ArrayList<BloomFilter<Integer>>();
	
	/**
	 * Number of element found on disk
	 */
	private int structureDataSize = 0;
	
	/**
	 * The number of elements of the tree map that is currently been filled.
	 * We need to keep this record separately as the .size() function of dbmap
	 * objects is noticeably slow.
	 */
	private int currentTreeMapSize = 0;
	
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
	public MapDBStructurePool(Path projectPath, boolean newdb) throws IOException{
		
		AptaLogger.log(Level.INFO, this.getClass(), "Instantiating MapDBStructurePool");
		
		// Time it for logging purposes
		long tReadFromDisk = System.currentTimeMillis();
		
		// Make sure the folder is writable
		if (!Files.isWritable(projectPath)){
			AptaLogger.log(Level.SEVERE, this.getClass(),"The project path is not writable.");
			throw (new IllegalStateException("The project path is not writable.") );
		}
		
		// Set the project path and pool data path
		this.projectPath = projectPath;
		
		// Check if the data path exists, and if not create it
		this.structureDataPath = Files.createDirectories(Paths.get(this.projectPath.toString(), "structuredata"));
		
		
		// If we are reading an existing database, iterate over the folder and open the individual MapDB instances
		if (! newdb){ 
			
			AptaLogger.log(Level.INFO, this.getClass(), "Searching for existing datasets in " + structureDataPath.toString());

			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(structureDataPath.toString()))) {
				
				for (Path file : directoryStream) {
	                
	    			// Open and read the TreeMap, skip the inverse file
	    			if (Files.isRegularFile(file)){
	    				
	    				AptaLogger.log(Level.INFO, this.getClass(), "Processing file " + file.toString());
	    				
	    				DB db_structure = DBMaker
					    .fileDB(file.toFile())
					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					    .fileMmapPreclearDisable() // Make mmap file faster
					    .cleanerHackEnable() // Unmap (release resources) file when its closed.
					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
					    .executorEnable()
					    .make();
	
	    				BTreeMap<Integer, double[]> dbmap = db_structure.treeMap("map")
						.valuesOutsideNodesEnable()
						.keySerializer(Serializer.INTEGER)
						.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
				        .open();
	    				
	    				structureData.add(dbmap);
	    				structureDataPaths.add(file);
	    				
	    				BloomFilter<Integer> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
	    				structureDataFilter.add(localBloomFilter);
	    				
	    				// Update values
	    				currentTreeMapSize = dbmap.size();
	    				structureDataSize += currentTreeMapSize;
	    				
	    				// Update the filter content
	    				Iterator<Integer> iterator = dbmap.keyIterator();
	    				while (iterator.hasNext()){
	    					Integer item = iterator.next();
	    					localBloomFilter.add(item);
	    					globalStructureDataFilter.add(item);
	    				}
	    				
	    				AptaLogger.log(Level.CONFIG, this.getClass(), 
	    						"Found and loaded file " + file.toString() + "\n" +
	    						"Total number of aptamers in file: " + currentTreeMapSize + "\n" +
	    						"Total number of aptamers: " + structureDataSize
	    						);
		                
		            }
				}
        	} catch (IOException ex) {}
			
			// If no structure data exists on disk, we need to fail here
			if (structureData.isEmpty()){
				AptaLogger.log(Level.SEVERE, this.getClass(), "No structure data was found on disk but is required for this operation.");
				throw new IllegalStateException("No structure data was found on disk but is required for this operation.");
			}
			
			AptaLogger.log(Level.INFO, this.getClass(), "Found and loaded a total of " + structureDataSize + " structures on disk.");
			
			
		}
		else{ // Create an empty instance of the MapDB Container

			DB db_structure = DBMaker
				    .fileDB(Paths.get(structureDataPath.toString(), "data" + String.format("%04d", structureData.size()) + ".mapdb").toFile())
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .fileMmapPreclearDisable() // Make mmap file faster
				    .cleanerHackEnable() // Unmap (release resources) file when its closed.
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();
	
			BTreeMap<Integer, double[]> dbmap = db_structure.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
			        .create();
			
			structureDataPaths.add(Paths.get(structureDataPath.toString(), "data" + String.format("%04d", structureData.size()) + ".mapdb"));
			structureData.add(dbmap);
	
			BloomFilter<Integer> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
			structureDataFilter.add(localBloomFilter);
			
			currentTreeMapSize = 0;
			
			AptaLogger.log(Level.CONFIG, this.getClass(), "Created new file " + Paths.get(structureDataPath.toString(), "data" + String.format("%04d", structureData.size()) + ".mapdb").toFile());
		
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "StructurePool instantiation took " + ((System.currentTimeMillis() - tReadFromDisk) / 1000.0) + " seconds");
		
	}
	
	
	
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#registerStructure(int, double[])
	 */
	@Override
	public synchronized void registerStructure(int id, double[] structure) {
		
		// Check that the current map is not at max capacity and create a new map if that is the case
		if (currentTreeMapSize == maxTreeMapCapacity){
			
			AptaLogger.log(Level.CONFIG, this.getClass(), 
					"Current Structure Map is at max capacity creating new file " + Paths.get(structureDataPath.toString(), "data" + String.format("%04d", structureData.size()) + ".mapdb").toString() + "\n" +
					"Total number of aptamers: " + this.structureDataSize 
					);
			
			DB db_structure = DBMaker
				    .fileDB(Paths.get(structureDataPath.toString(), "data" + String.format("%04d", structureData.size()) + ".mapdb").toFile())
				    .fileMmapPreclearDisable() // Make mmap file faster
				    .cleanerHackEnable() // Unmap (release resources) file when its closed.
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();

			BTreeMap<Integer, double[]> dbmap = db_structure.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
			        .create();

			structureDataPaths.add(Paths.get(structureDataPath.toString(), "data" + String.format("%04d", structureData.size()) + ".mapdb"));
			structureData.add(dbmap);
			
			BloomFilter<Integer> localBloomFilter = new FilterBuilder(maxTreeMapCapacity, bloomFilterCollisionProbability).buildBloomFilter();
			this.structureDataFilter.add(localBloomFilter);
			
			currentTreeMapSize = 0;
			
		}

		// Now insert the sequence
		structureDataSize++;
		structureData.get(structureData.size()-1).put(id,structure);
		currentTreeMapSize++;
		this.globalStructureDataFilter.add(id);
		this.structureDataFilter.get(structureData.size()-1).add(id);
		
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#getStructure(int)
	 */
	@Override
	public double[] getStructure(int id) {

		// Check for existence using bloom filter. 
		if (!this.globalStructureDataFilter.contains(id)){
			return null; // This result is always accurate (no false negatives)
		}
		
		double[] structure = null;
		
		// Iterate over all treeMaps and bloomFilters
		ListIterator<BTreeMap<Integer,double[]>> lim = structureData.listIterator(structureData.size());
		ListIterator<BloomFilter<Integer>> lib = this.structureDataFilter.listIterator(structureData.size());
		
		// Iterate in reverse
		while(lim.hasPrevious() && structure == null) {
			
			// Prevent expensive disk lookups by using the bloom filters...
			if(! lib.previous().contains(id) ){
				lim.previous();
				continue;
			}
			
			// ... and only look it up when we have to
			structure = lim.previous().get(id); //note, in case of a false positive, this returns null

		}
		
		return structure;
		
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#close()
	 */
	@Override
	public void close(){
		
		// Iterate over each TreeMap instance and close it
		ListIterator<BTreeMap<Integer, double[]>> li = structureData.listIterator(structureData.size());

		// Iterate in reverse
		while(li.hasPrevious()) {
			
			li.previous().close();

		}
	}
	

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#setReadOnly()
	 */
	@Override
    public void setReadOnly(){
    	
    	// close all the file handles
    	close();
    	
    	// clear references
    	structureData.clear();
    	
    	// reopen as read only
		for (Path file : structureDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = DBMaker
					    .fileDB(file.toFile())
					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					    .fileMmapPreclearDisable() // Make mmap file faster
					    .cleanerHackEnable() // Unmap (release resources) file when its closed.
					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
					    .executorEnable()
					    .readOnly()
					    .make();

				BTreeMap<Integer, double[]> dbmap = db.treeMap("map")
						.valuesOutsideNodesEnable()
						.keySerializer(Serializer.INTEGER)
						.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
				        .open();
				
				structureData.add(dbmap);
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + file.toString() );
			}
            
        }
    }
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#setReadWrite()
	 */
	@Override
    public void setReadWrite(){
    	
    	// close all the file handles
    	close();
    	
    	// clear references
    	structureData.clear();
    	
    	// reopen as read only
		for (Path file : structureDataPaths) {
            
			// Open and read the TreeMap
			if (Files.isRegularFile(file)){
				
				DB db = DBMaker
					    .fileDB(file.toFile())
					    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					    .fileMmapPreclearDisable() // Make mmap file faster
					    .cleanerHackEnable() // Unmap (release resources) file when its closed.
					    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
					    .executorEnable()
					    .make();

				BTreeMap<Integer, double[]> dbmap = db.treeMap("map")
						.valuesOutsideNodesEnable()
						.keySerializer(Serializer.INTEGER)
						.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
				        .open();
				
				structureData.add(dbmap);
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + file.toString() );
			}
            
        }
    }	
	
	
	
	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id->structure to the API.
	 */
	private class IdStructureIterator implements Iterable<Entry<Integer, double[]>> {

		@Override
		public Iterator<Entry<Integer, double[]>> iterator() {
			Iterator<Entry<Integer, double[]>> it = new Iterator<Entry<Integer, double[]>>() {
	
				Iterator<Integer> pool_iterator = Configuration.getExperiment().getAptamerPool().id_iterator().iterator();
	            
	            @Override
	            public boolean hasNext() {
	                return pool_iterator.hasNext();
	            }
	
	            @Override
	            public Entry<Integer, double[]> next() {
	            	
	            	Integer entry = pool_iterator.next();
	            	
	            	//get the next Id from the map and look up the corresponding sequence
	            	return new AbstractMap.SimpleEntry<Integer, double[]>(entry, getStructure(entry));
	            	
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
	 * Make use of internal classes so we can provide iterators for id->structure to the API.
	 */
	private class AptamerStructureIterator implements Iterable<Entry<byte[], double[]>> {

		@Override
		public Iterator<Entry<byte[], double[]>> iterator() {
			Iterator<Entry<byte[], double[]>> it = new Iterator<Entry<byte[], double[]>>() {
	
				Iterator<Entry<byte[], Integer>> pool_iterator = Configuration.getExperiment().getAptamerPool().iterator().iterator();
	            
	            @Override
	            public boolean hasNext() {
	                return pool_iterator.hasNext();
	            }
	
	            @Override
	            public Entry<byte[], double[]> next() {
	            	
	            	Entry<byte[], Integer> entry = pool_iterator.next();
	            	
	            	//get the next Id from the map and look up the corresponding sequence
	            	return new AbstractMap.SimpleEntry<byte[], double[]>(entry.getKey(), getStructure(entry.getValue()));
	            	
	            }
	
	            @Override
	            public void remove() {
	                throw new UnsupportedOperationException();
	            }
	        };
	        return it;
		}	
		
	}	
	

	@Override
	public Iterable<Entry<Integer, double[]>> iterator() {
		return new IdStructureIterator();
	}


	@Override
	public Iterable<Entry<byte[], double[]>> sequence_iterator() {
		return new AptamerStructureIterator();
	}




}
