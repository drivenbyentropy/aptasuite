/**
 * 
 */
package aptasuite;

import java.nio.file.Path;

import lib.aptamer.datastructures.Experiment;

/**
 * @author Jan Hoinka
 * Implements the command line interface version of aptasuite.
 */
public class CLI {

	/**
	 * Instance of the current experiment
	 */
	Experiment experiment = null; 
			
	public CLI(String configFile){
		
		// Initialize the experiment
		this.experiment = new Experiment(configFile);
		
		System.out.println(experiment.getSelectionCycleConfiguration());
	}
	
}
