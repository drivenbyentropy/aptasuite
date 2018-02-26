/**
 * 
 */
package lib.aptanet.input;

import java.util.List;

import org.datavec.api.writable.Writable;

/**
 * @author Jan Hoinka
 * Given an aptamer id, return a corresponding label depending on 
 * the task at hand 
 */
public interface AptamerLabelGenerator {

	/**
	 * Returns a label id for aptamer with id <code>id</code>
	 * @param id
	 * @return
	 */
	public Writable getLabelForAptamer(int id);
	
	
	/**
	 * Returns the index at which to write a 1 in a one hot representation of the labels
	 * @param id
	 * @return
	 */
	public int getHotIndexForLabel(int id);
	
	/**
	 * Returns the total number of labels which are trainable
	 * @return
	 */
	public int getNumberOfLabels();
	
	
	/**
	 * Return a list of all Labels
	 * @return
	 */
	public List<Writable> getLabels();
	
}
