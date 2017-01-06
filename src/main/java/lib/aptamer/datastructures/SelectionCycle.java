/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Jan Hoinka
 * 
 * This interface defines the methods required to handle
 * the selection cycle memberships of all aptamers in an
 * instance of <code>AptamerPool</code>. 
 * 
 * The implementing class must provide, at minimum, the 
 * ability to add an aptamer to a particular selection 
 * cycle, to get/set its cardinality, and to provide 
 * atomic membership queries. In addition it must provide
 * the ability to access previous and consecutive, as well as
 * control and counter selection cycles
 * 
 * In addition, iterators over the aptamers for each selection 
 * cycle should be provided for ease of use. 
 * 
 * Finally, it is expected that the constructor of an implementing 
 * class can be called with the following parameter:
 * Constructor(String name, int round_id, boolean isControlSelection, boolean isCounterSelection)
 */
public interface SelectionCycle extends Serializable{

	/**
	 * Adds aptamer <code>a</code> to this selection cycle. If the aptamer is already present
	 * it adds 1 to its cardinality. If the aptamer has never been observerd in the <code>AptamerPool</code>, this
	 * function also add it to the corresponding instance.
	 * @param a the aptamer in question
	 */
	public void addToSelectionCycle(String a);
	
	/**
	 * @see SelectionCycle#addToSelectionCycle(String a)
	 */
	public void addToSelectionCycle(byte[] a);
	
	/**
	 * Checks whether an aptamer is part of this selection cycle
	 * @param a the aptamer in question
	 * @return true if the aptamer is present, false otherwise
	 */
	public boolean containsAptamer(String a);
	
	/**
	 * @see SelectionCycle#containsAptamer(String a)
	 */
	public boolean containsAptamer(byte[] a);
	
	/**
	 * Return the cardinality of a particular aptamer in this pool.
	 * @param a The aptamer sequence in question
	 * @return The cardinality of the aptamer. If the aptamer is not part of this selection cycle, 0 is returned.
	 */
	public int getAptamerCardinality(String a); 

	/**
	 * @see SelectionCycle#getAptamerCardinality(String a)
	 */
	public int getAptamerCardinality(byte[] a); 
	
	/**
	 * The total number of aptamer molecules in this pool. This is typically defined as 
	 * the sum over the cardinality of all aptamers.
	 * @return
	 */
	public int getSize();
	
	
	/**
	 * The number of unique aptamers in this pool.
	 * @return
	 */
	public int getUniqueSize();
	
	
	/**
	 * Provides access to the selection cycle of the next round.
	 * @return null if this is the last selection cycle
	 */
	public SelectionCycle getNextSelectionCycle();
	
	
	/**
	 * Provides access to the selection cycles of the previous round
	 * @return null if this is the first selection cycle
	 */
	public SelectionCycle getPreviousSelectionCycle();
	
	
	/**
	 * Provides access to the control cycles 
	 * @return Empty list if no control cycles are present for this round
	 */
	public ArrayList<SelectionCycle> getControlCycles();
	
	
	/**
	 * Provides access to the counter selection cycles 
	 * @return Empty list if no counter selection cycles are present for this round
	 */
	public ArrayList<SelectionCycle> getCounterSelectionCycles();
	
	
	/**
	 * Returns a unique identifier for this selection cycle
	 * @return unique id
	 */
	public String getName();
	
	
	/**
	 * Return the round number of this selection cycle 
	 * @return round number >= 0
	 */
	public int getRound();
	
	
	/**
	 * Defines if the selection cycle is a control
	 * @return true if selection cycle is control
	 */
	public boolean isControlSelection();
	
	
	/**
	 * Defines if the selection cycle is a counter selection.
	 * A counter selection is assumed to be performed BEFOR the actual selection.
	 * Hence, a counter selection belonging to round X is defined as the 
	 * input for the selection cycle of round X.
	 * @return true if selection cycle is a counter selection
	 */
	public boolean isCounterSelection();
	
	
	/**
	 * Optional. Performs any additional logic on the selection cycle such as
	 * optimizing the data structures once it is known no more items will
	 * be added to it (i.e. upon completing the parsing).
	 */
	public void setReadOnly();
	
	
	//TODO: +iteratator for all aptamers in this pool
	
}
