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
		parameters.addOption(Option.builder("config").hasArg().argName("path").desc("Path to the configuration file for APTASuite").required().build());
		
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
		
		// AptaCLUSTER
		parameters.addOption("cluster", false, "Applies AptaCLUSTER to the dataset using the parameters as specified in the configuration file");
		
		// AptaTRACE
		parameters.addOption("trace", false, "Applies AptaTRACE to the dataset using the parameters as specified in the configuration file");
		
		// Export
		Option export = new Option("export", true, "Writes the specified <data> to file. Multiple arguments must be comma-separated with not spaces in between. Arguments: \n\npool: every unique aptamer of the selection together with the counts of all selection cycles\n\ncycles: the aptamers sequences as present in the specified selection cycles. Each aptamer will be written to file as many times as its cardinality in the pool.\n\nclusters: Exports the clusters for the specified cycles sorted by cluster size\n\nstructures: writes the structural data for the aptamer pool to file.");
		export.setArgName("pool,cycles,clusters,structure");
		
		parameters.addOption(export);
		
		}
}
