/**
 * 
 */
package gui.core;

/**
 * @author Jan Hoinka
 * An interface for Controllers that can be initialized dynamically
 */
public interface Initializable {

	public void initializeContent();
	
	public Boolean isInitialized();
	
}
