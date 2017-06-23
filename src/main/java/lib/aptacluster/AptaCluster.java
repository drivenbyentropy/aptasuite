package lib.aptacluster;

/**
 * @author Jan Hoinka
 * Implements the logic of AptaCluster as described in Res Comput Mol Biol. 2014;8394:115-128.
 */
public interface AptaCluster {

	/**
	 * Performs locality sensitive hashing on the data and assigns each aptamer to one cluster
	 */
	public void performLSH();
	
}
