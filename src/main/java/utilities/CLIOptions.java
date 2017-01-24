package utilities;

import org.apache.commons.cli.Options;

/**
 * @author hoinkaj
 * This class handles the list of paramters that are admissable
 * when using the command line version of aptasuite.
 */
public class CLIOptions {

	public static Options parameters = new Options();
	static{
		
		// Configuration file location
		parameters.addOption("config", true, "Path to the configuration file for APTASuite");
		
		// Help
		parameters.addOption("help", false, "print this message");
		
		// AptaPLEX 
		parameters.addOption("createdb", false, "Creates a new aptamer pool and associated selection cycles according to the configuration file");
	
		// Structure Prediction
		parameters.addOption("structures", false, "Predicts the structural ensamble of all aptamers in the pool and stores them on disk");
		
		// AptaTRACE
		parameters.addOption("trace", false, "Applies AptaTRACE to the dataset using the parameters as specified in the configuration file");
	}
}
