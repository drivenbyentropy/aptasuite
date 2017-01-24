/**
 * 
 */
package lib.aptamer.datastructures;

import java.util.Map.Entry;

/**
 * @author Jan Hoinka
 * API for handling the structural data associated with an aptamer pool.
 * The implementing class is responsible for defending the data structure 
 * for encoding the structural information of an aptamer in the form of 
 * <code>double[]</code>.
 * 
 */
public interface StructurePool {

	
	/**
	 * Adds the structure corresponding to aptamer with id <code>id</code> to the
	 * structure pool.
	 * @param id the unique identifier corresponding to the aptamer in question
	 * @param structure the structural information for that aptamer
	 */
	void registerStructure(int id, double[] structure);
	
	/**
	 * Returns the structural information belonging to aptamer with id <code>id</code>
	 * @param id the unique identifier corresponding to the aptamer in question
	 * @return the structural information for that aptamer, null if aptamer id does
	 * not exist in the pool
	 */
	double[] getStructure(int id);
	
	
	/**
	 * Provides an iterator over every aptamer id in the pool  
	 * together with its corresponding structure.
	 * Note that the order of iteration is implementation dependent
	 */
	public Iterable<Entry<Integer, double[]>> iterator();
	
	
	/**
	 * Provides an iterator over every aptamer sequence in the pool  
	 * together with the corresponding structure.
	 * Note that the order of iteration is implementation dependent
	 */
	public Iterable<Entry<byte[],double[]>> sequence_iterator();
	
}
