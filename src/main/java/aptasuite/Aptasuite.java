package aptasuite;

import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import utilities.AptaLogger;
import utilities.CLIOptions;

/**
 * @author Jan Hoinka The main class of the aptasuite implementation. This class
 *         controls the program flow for the command line interface as well as
 *         the graphical user interface.
 */
public class Aptasuite {

	public static void main(String[] args) {

		// case command line interface
		if (args.length != 0) { 

			// parse the command line
			CommandLineParser parser = new DefaultParser();
			try {
				// parse the command line arguments
				CommandLine line = parser.parse(CLIOptions.parameters, args);

				// print the help if requested
				if (line.hasOption("help")) {
					HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp("AptaSUITE", "", CLIOptions.parameters,
							"\nPlease report issues at https://github.com/drivenbyentropy/aptasuite", true);
					System.exit(1);
				}

				CLI cli = new CLI(line);

			}

			catch (ParseException exp) {
				// oops, something went wrong
				System.err.println("Parameter Error.  Reason: " + exp.getMessage());
				System.out.println("\n");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("AptaSUITE", "", CLIOptions.parameters,
						"\nPlease report issues at https://github.com/drivenbyentropy/aptasuite", true);
			}
			
			AptaLogger.log(Level.INFO, Aptasuite.class, "Exiting.");
		}
		// case gui
		else {
//			// TODO: implement the gui handling
//			RootClass mainWindow = new RootClass();
//			mainWindow.lauchMainWindow();
//			System.out.println("Exiting");
			System.exit(0);
		}

		
	}

}
