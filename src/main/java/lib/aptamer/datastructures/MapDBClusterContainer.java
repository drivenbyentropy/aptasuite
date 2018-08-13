/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.IOException;
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
import org.mapdb.Serializer;
import org.mapdb.DBException.WrongConfiguration;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 */
public class MapDBClusterContainer implements ClusterContainer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3411912606711493897L;

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
	 * Worst case scenario is we have as many clusters as we have aptamers
	 */
	private transient BloomFilter<Integer> containerContent = new FilterBuilder(Configuration.getExperiment().getAptamerPool().size(), Configuration.getParameters().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability")).buildBloomFilter();
	//containerContent = new FilterBuilder(Configuration.getParameters().getInt("MapDBAptamerPool.bloomFilterCapacity"), Configuration.getParameters().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability")).buildBloomFilter();
	
	/**
	 * Keep track of what ID has already been used.
	 * Worst case scenario is we have as many clusters as we have aptamers 
	 */
	private transient BitSet clusterIDUsage = new BitSet(Configuration.getExperiment().getAptamerPool().size());
	
	private Integer totalNumberOfClusters = 0;
	
	/**
	 * File backed map containing the IDs of each aptamer (as stored in <code>AptamerPool</code>)
	 * and the cluster id as value.
	 */
	private transient BTreeMap<Integer,Integer> clusterContainer = null;
	
	
	/**
	 * Counts the total number of aptamers which have been assigned a cluster.
	 */
	private int size = 0;
	
	private int max_cluster_id = 0;

	public MapDBClusterContainer(boolean newdb) throws IOException {

		// Create the file backed map and perform sanity checks
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
				
		// Check if the data path exists, and if not create it
		Path poolDataPath = Files.createDirectories(Paths.get(projectPath.toString(), "clusterdata"));

		// Determine the unique file name associated with this cycle
		String containerFileName = "clusters.mapdb";

		// Create map or read from file
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), containerFileName).toFile())
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();
		
		// Creating a new database
		if (newdb)
		{
			AptaLogger.log(Level.CONFIG, this.getClass(), "Creating new file '" + Paths.get(poolDataPath.toString(), containerFileName).toFile() + "' for cluster storage.");
	
			clusterContainer = db.treeMap("map")
					//.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(Serializer.INTEGER)
			        .create();			
		}
		else { // we need to read from file and update class members
			AptaLogger.log(Level.CONFIG, this.getClass(), "Reading from file '" + Paths.get(poolDataPath.toString(), containerFileName).toFile() + "' for cluster storage.");
			
			try {
				clusterContainer = db.treeMap("map")
						//.valuesOutsideNodesEnable()
						.keySerializer(Serializer.INTEGER)
						.valueSerializer(Serializer.INTEGER)
				        .open();
			}
			//If this fails, we need to make sure that all file handles have been closed
			catch(WrongConfiguration e) {
				
				db.close();
				throw(e);
				
			}
			
			
			clusterContainer.forEach( (key,value) -> {
				containerContent.add(key);
				
				// update number of clusters
				if (!clusterIDUsage.get(value)) {
					
					totalNumberOfClusters += 1;
					clusterIDUsage.set(value);
					
				}
				
				size++;
				max_cluster_id = Math.max(max_cluster_id, value);
			});
			
		}
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#addToCluster(java.lang.String, int)
	 */
	@Override
	public int addToCluster(String a, int cluster_id) {
		
		return addToCluster(a.getBytes(), cluster_id);
		
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#addToCluster(byte[], int)
	 */
	@Override
	public int addToCluster(byte[] a, int cluster_id) {
		
		// Check if the aptamer is already present in the pool and add it if not
		int id_a = Configuration.getExperiment().getAptamerPool().getIdentifier(a);
		
		return addToCluster(id_a, cluster_id);
		
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#addToCluster(byte[], int)
	 */
	@Override
	public int addToCluster(int a, int cluster_id) {
		
		// Update the size
		size++;
				
		clusterContainer.put(a, cluster_id);
		containerContent.add(a);
		
		// update number of clusters
		if (!clusterIDUsage.get(cluster_id)) {
			
			totalNumberOfClusters += 1;
			clusterIDUsage.set(cluster_id);
			
		}
		
		max_cluster_id = Math.max(max_cluster_id, cluster_id);
		
		return a;
		
	}
	
	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#containsAptamer(java.lang.String)
	 */
	@Override
	public boolean containsAptamer(String a) {
		
		return containsAptamer(a.getBytes());

	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#containsAptamer(byte[])
	 */
	@Override
	public boolean containsAptamer(byte[] a) {
	
		// Get the corresponding aptamer id from the pool
		int id_a = Configuration.getExperiment().getAptamerPool().getIdentifier(a);
		
		if (! containerContent.contains(id_a)){
			return false;
		}
		
		Integer current_count = clusterContainer.get(id_a);
			
		return current_count != null;
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#containsAptamer(int)
	 */
	@Override
	public boolean containsAptamer(int a) {

		if (! containerContent.contains(a)){
			return false;
		}
		
		Integer current_count = clusterContainer.get(a);
			
		return current_count != null;
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#getClusterId()
	 */
	@Override
	public int getClusterId(int a) {

		if (!containsAptamer(a)){
			return -1;
		}
		
		return this.clusterContainer.get(a);
		
	}
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#getSize()
	 */
	@Override
	public int getSize() {
		return size;
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#setReadOnly()
	 */
	@Override
	public void setReadOnly() {

		clusterContainer.close();
		
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		Path poolDataPath = Paths.get(projectPath.toString(), "clusterdata");
		String containerFileName = "clusters.mapdb";
		
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), containerFileName).toFile())
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .readOnly()
			    .make();

		clusterContainer = db.treeMap("map")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .open();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read only file " + Paths.get(poolDataPath.toString(), containerFileName).toString() );
		
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#setReadWrite()
	 */
	@Override
	public void setReadWrite() {
		
		clusterContainer.close();
		
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		Path poolDataPath = Paths.get(projectPath.toString(), "clusterdata");
		String containerFileName = "clusters.mapdb";
		
		DB db = DBMaker
			    .fileDB(Paths.get(poolDataPath.toString(), containerFileName).toFile())
			    .allocateStartSize( this.allocateStartSize )
			    .allocateIncrement( this.allocateIncrement )
			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
			    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
			    .executorEnable()
			    .make();

		clusterContainer = db.treeMap("map")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(Serializer.INTEGER)
		        .open();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Reopened as read/write file " + Paths.get(poolDataPath.toString(), containerFileName).toString() );

	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#close()
	 */
	@Override
	public void close() {
		
		this.clusterContainer.close();
		
	}

	
	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id->cluster and sequence->cluster to the API.
	 * This class implements the id->cluster view.
	 */
	private class clusterIterator implements Iterable<Entry<Integer, Integer>> {

		public Iterator<Entry<Integer, Integer>> iterator() {
			return clusterContainer.entryIterator();
		}

	}
	
	/**
	 * @author Jan Hoinka
	 * Make use of internal classes so we can provide iterators for id->cluster and sequence->cluster to the API.
	 * This class implements the sequence->cluster view.
	 */
	private class clusterSequenceIterator implements Iterable<Entry<byte[], Integer>> {

		@Override
		public Iterator<Entry<byte[], Integer>> iterator() {
			Iterator<Entry<byte[], Integer>> it = new Iterator<Entry<byte[], Integer>>() {
	
				Iterator<Entry<Integer, Integer>> container_iterator = clusterContainer.entryIterator();
	            
	            @Override
	            public boolean hasNext() {
	                return container_iterator.hasNext();
	            }
	
	            @Override
	            public Entry<byte[], Integer> next() {
	            	
	            	Entry<Integer, Integer> entry = container_iterator.next();
	            	
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
	
	
	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#iterator()
	 */
	@Override
	public Iterable<Entry<Integer, Integer>> iterator() {
		return new clusterIterator();
	}

	/* (non-Javadoc)
	 * @see lib.aptamer.datastructures.ClusterContainer#sequence_iterator()
	 */
	@Override
	public Iterable<Entry<byte[], Integer>> sequence_iterator() {
		return new clusterSequenceIterator();
	}

	@Override
	public int getNumberOfClusters() {

		return totalNumberOfClusters;
		
	}

	@Override
	public int reassignClusterId(int a, int cluster_id) {
		
		Integer r = clusterContainer.replace(a, cluster_id);
		
		return r == null? -1 : r;
	
	}

}
