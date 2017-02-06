package utilities;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
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
		
		// Data input options, these are mutually exclusive
				
		// AptaPLEX 
		Option aptaplex = new Option("parse", false, "Creates a new aptamer pool and associated selection cycles according to the configuration file. Note, this option is mutually exclusive with -simulate.");
	
		// AptaSIM
		Option aptasim = new Option("simulate", false, "Creates a new aptamer pool using AptaSIM according to the configuration file. Note, this option is mutually exclusive with -parse");
		
		OptionGroup datainput = new OptionGroup();
		datainput.addOption(aptaplex);
		datainput.addOption(aptasim);
		
		parameters.addOptionGroup(datainput);
		
		// Structure Prediction
		parameters.addOption("structures", false, "Predicts the structural ensamble of all aptamers in the pool and stores them on disk");
		
		// AptaTRACE
		parameters.addOption("trace", false, "Applies AptaTRACE to the dataset using the parameters as specified in the configuration file");
		
		}
}
