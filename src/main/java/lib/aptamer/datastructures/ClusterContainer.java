/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * @author Jan Hoinka
 * 
 * This interface defines the methods required to handle
 * the memberships of all aptamers into clusters related to each other
 * by sime similarity measure.
 * 
 * The implementing class must provide, at minimum, the 
 * ability to add an aptamer to a particular cluster, and to provide 
 * atomic membership queries. 
 * 
 * Cluster ids must be assigned consecutively and starting at 0.
 * 
 * In addition, iterators over the aptamers should be provided for ease of use. 
 */
public interface ClusterContainer extends Serializable{

	/**
	 * Adds aptamer <code>a</code> to cluster <code>cluster_id</code. If the aptamer was previously assigned 
	 * to another cluster, this setting will be overwritten.
	 * @param a the aptamer in question
	 * @return the unique cluster id
	 */
	public int addToCluster(String a, int cluster_id);
	
	/**
	 * Adds aptamer <code>a</code> to cluster <code>cluster_id</code. If the aptamer was previously assigned 
	 * to another cluster, this setting will be overwritten.
	 * @param a the aptamer in question
	 * @return the unique cluster id
	 */
	public int addToCluster(byte[] a, int cluster_id);
	
	/**
	 * Adds aptamer <code>a</code> to cluster <code>cluster_id</code>. If the aptamer was previously assigned 
	 * to another cluster, this setting will be overwritten.
	 * @param a the aptamer in question
	 * @return the unique cluster id
	 */
	public int addToCluster(int a, int cluster_id);
	
	/**
	 * Reassigns aptamer <code>a</code> to cluster <code>cluster_id</code>.
	 * Aptamer must already be assigned in the Cluster Container, otherwise no change will be 
	 * performed.
	 * @param a the aptamer in question
	 * @param cluster_id
	 * @return the new cluster id or -1 if <code>a</code> was not present
	 */
	public int reassignClusterId(int a, int cluster_id);
	
	/**
	 * Checks whether an aptamer has already been assigned a cluster
	 * @param a the aptamer in question
	 * @return true if the aptamer is present, false otherwise
	 */
	public boolean containsAptamer(String a);
	
	/**
	 * @see ClusterContainer#containsAptamer(String a)
	 */
	public boolean containsAptamer(byte[] a);

	/**
	 * @see ClusterContainer#containsAptamer(String a)
	 */
	public boolean containsAptamer(int a);
	
	/**
	 * Retrieves a cluster id for a corresponding aptamer
	 * @param a the aptamer in question
	 * @return the cluster id corresponding to <code>a</code>, -1 if no cluster id for <code>a</code> exists
	 */
	public int getClusterId(int a);
	
	/**
	 * The total number of aptamer which have been assigend a cluster
	 * @return
	 */
	public int getSize();

	/**
	 * The total number of clusters in this database
	 * @return
	 */
	public int getNumberOfClusters();
	
	/**
	 * Optional. Performs any additional logic on the selection cycle such as
	 * optimizing the data structures once it is known no more items will
	 * be added to it (i.e. upon completing the parsing).
	 */
	public void setReadOnly();
	
	
	/**
	 * Optional. Sets the implementing class to read/write mode in case
	 * persistent storage is used.
	 */
	public void setReadWrite();	
	
	/**
	 * Optional. Closes the underlying data structure, freeing any resources attached
	 * to it.
	 */
	public void close();
	
	/**
	 * Provides an iterator over every aptamer id (key) together with its cluster id.
	 * Note that the order of iteration is dependent on the implementing class
	 */
	public Iterable<Entry<Integer, Integer>> iterator();
	
	
	/**
	 * Provides an iterator over every aptamer id (key) together with its cluster id.
	 * Note that the order of iteration is dependent on the implementing class
	 */
	public Iterable<Entry<byte[],Integer>> sequence_iterator();

}
