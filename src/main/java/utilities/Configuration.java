/**
 * 
 */
package utilities;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.Experiment;

/**
 * @author Jan Hoinka
 * 
 * This class functions as a container for configuration options for all
 * the libraries of AptaSuite including the command line interface and
 * the graphical user interface. It provides a static member for
 * property retrieval (<code>getProperty</code>) which should be
 * available to all classes.
 * 
 * In addition it is used as a centralized hub for access to all instances of
 * aptamer related classes (such as Experiment, SelectionCycles, etc) via appropriate 
 * getters and setters.
 *
 */
public class Configuration {
	
	/**
	 * Enable logging for debuging and information
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	/**
	 * Apache commons configuration API.
	 */
	private static org.apache.commons.configuration2.builder.fluent.Configurations configs = new org.apache.commons.configuration2.builder.fluent.Configurations();	
	
	/**
	 * The properties instance. See Javas <code>Properties</code> class.
	 */
	private static org.apache.commons.configuration2.Configuration parameters;
	
	
	/**
	 * The instance of the experiment. Must be registered with <code>setExperiment</code>
	 */
	private static Experiment experiment = null;


	/**
	 * Adds the configuration stored on disk to the current set of parameters. 
	 * The file <code>fileName</code> must be a valid configuration file
	 * as per Javas <code>Properties</code> class.
	 * @param fileName
	 */
	public static void setConfiguration(String fileName){
		
			try {
				LOGGER.info("Reading configuration from file.");
				parameters = configs.properties(new File(fileName));
			} catch (Exception e) {
				
				LOGGER.info("Error, could not read configuration file. Please check it for correctness");
				e.printStackTrace();
			}	
		
			// Define default parameters
			HashMap<String, Object> defaults= new HashMap<String,Object>()
			{{
			     // PoolBackend
			     put("AptamerPool.backend", "MapDBAptamerPool");
				
				 // PoolMapDB Options
			     put("MapDBAptamerPool.bloomFilterCapacity", 500000000);
			     put("MapDBAptamerPool.bloomFilterCollisionProbability", 0.001);
			     put("MapDBAptamerPool.maxTreeMapCapacity", 7500000);
			     
			     
			     // SelectionCycle Backend
			     put("SelectionCycle.backend", "MapDBSelectionCycle");
			     
			     // BloomFilterSelectionCycle  Options
			     put("MapDBSelectionCycle.bloomFilterCollisionProbability", 0.001); //the capacity must be the same as MapDBAptamerPool.bloomFilterCapacity
			     
			     
			     // Parser Options
			     put("Parser.backend", "AptaplexParser");
			     
			     // AptaplexParser Options
			     put("AptaplexParser.isPerFile", false);
			     
			}};
			
			// add to parameters only if they have not been defined in the file
			for ( Entry<String, Object> item : defaults.entrySet()){
				if (!parameters.containsKey(item.getKey())){
					parameters.setProperty(item.getKey(), item.getValue());
				}
			}
			
			LOGGER.info("Using the following parameters: " + printParameters());
			// TODO: Sanity checks!
	}
	
	/**
	 * Returns the parameter set including the return value
	 * default parameters as well the properties imported via file. The individual parameters
	 * can the be accessed using Apache Common Configuration 2.x APIs.
	 * @return the parameter set
	 */
	public static org.apache.commons.configuration2.Configuration getParameters() {
		return parameters;
	}
	
	
	/**
	 * Registers the instance of an Experiment with the Configuration class
	 * @param e null if not set
	 */
	public static void setExperiment(Experiment e){
		experiment = e;
	}
	
	
	/**
	 * Returns the Experiment instance.
	 * @return
	 */
	public static Experiment getExperiment(){
		return experiment;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		return printParameters();
	}
	
	
	public static String printParameters(){
		StringBuilder sb = new StringBuilder();
		
		Iterator<String> keys = parameters.getKeys();
		
		while (keys.hasNext()){
			String key = keys.next();
			System.out.printf("%s : %s\n", (String) key, parameters.getProperty(key) );
		}
		
		return sb.toString();
	}
}