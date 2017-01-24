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
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;
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
	 * The total number of expected items in the bloom filter.
	 */
	private int bitSetCapacity = Configuration.getParameters().getInt("MapDBAptamerPool.bloomFilterCapacity");

	
	/**
	 * The structural data of the aptamer 
	 */
	private transient BTreeMap<Integer, double[]> structureData = null;
	
	
	/**
	 * Fast lookup of membership for <code>structureData</code>
	 */
	private BitSet structureDataFilter = new BitSet(bitSetCapacity);
	
	
	/**
	 * Number of element found on disk
	 */
	private int structureDataSize = 0;
	
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
			
			DB db_structure = DBMaker
				    .fileDB(Paths.get(structureDataPath.toString(), "structure_data" + ".mapdb").toFile())
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();

			structureData = db_structure.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
			        .open();
			
			// Update the filter content
			Iterator<Integer> iterator = structureData.keyIterator();
			while (iterator.hasNext()){
				structureDataFilter.set(iterator.next());
				structureDataSize++;
			}
			
			AptaLogger.log(Level.INFO, this.getClass(), "Found and loaded a total of " + structureDataSize + " structures on disk.");
			
		}
		else{ // Create an empty instance of the MapDB Container

		DB db_structure = DBMaker
			    .fileDB(Paths.get(this.structureDataPath.toString(), "structure_data" + ".mapdb").toFile())
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		structureData = db_structure.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new SerializerCompressionWrapper(Serializer.DOUBLE_ARRAY))
		        .create();
		
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created new file " + Paths.get(structureDataPath.toString(), "structure_data" + ".mapdb").toFile());
	}
	
	
	
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#registerStructure(int, double[])
	 */
	@Override
	public synchronized void registerStructure(int id, double[] structure) {
		
		// Add structure to map
		structureData.put(id, structure);

		// and update bitset
		structureDataFilter.set(id);
		
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.StructurePool#getStructure(int)
	 */
	@Override
	public double[] getStructure(int id) {

		//check if structure is available
		if (!structureDataFilter.get(id)){
			return null;
		}
		
		return structureData.get(id);
		
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
	            	return new AbstractMap.SimpleEntry<Integer, double[]>(entry, structureData.get(entry));
	            	
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
	            	return new AbstractMap.SimpleEntry<byte[], double[]>(entry.getKey(), structureData.get(entry.getValue()));
	            	
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
