package aptasuite;

import java.util.Locale;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import gui.core.RootClass;
import utilities.AptaLogger;
import utilities.CLIOptions;

/**
 * @author Jan Hoinka The main class of the aptasuite implementation. This class
 *         controls the program flow for the command line interface as well as
 *         the graphical user interface.
 */
public class Aptasuite {
	
	// Turn off debugging logs of third party libraies
	//TODO: make this work...
	static {
	      System.setProperty("org.apache.commons.logging.Log",
	                         "org.apache.commons.logging.impl.NoOpLog");
	      System.setProperty("org.apache.commons.beanutils.level" , "SEVERE");
	   }

	
	public static void main(String[] args) {

		// Set a local for the instance so that we do not run into formatting exceptions in different countries
		Locale.setDefault( new Locale("en", "US") );
		
		// Log system info for debugging purposes
		
		StringBuilder sysinfo = new StringBuilder();
		String sep = System.lineSeparator();
		sysinfo.append(String.format("System Information:%sJava Version: %s%s", sep, System.getProperty("java.version"), sep));
		sysinfo.append(String.format("JavaFX Version: %s%s", System.getProperties().get("javafx.runtime.version"), sep));
		sysinfo.append(String.format("OS Name: %s%s", System.getProperties().get("os.name"), sep));
		sysinfo.append(String.format("OS Version: %s%s", System.getProperties().get("os.version"), sep));
		sysinfo.append(String.format("OS Architecture: %s%s", System.getProperties().get("os.arch"), sep));
		sysinfo.append(String.format("OS Architecture Model: %s%s", System.getProperty("sun.arch.data.model"), sep));
		sysinfo.append(String.format("CPU cores: %s%s", Runtime.getRuntime().availableProcessors(), sep));
		sysinfo.append(String.format("System Memory: %s Mb%s", Runtime.getRuntime().totalMemory(), sep));
		
		
		AptaLogger.log(Level.CONFIG, Aptasuite.class, sysinfo.toString());
		
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
					formatter.setWidth(120);
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
				formatter.setWidth(120);
				formatter.printHelp("AptaSUITE", "", CLIOptions.parameters,
						"\nPlease report issues at https://github.com/drivenbyentropy/aptasuite", true);
			}
			
			AptaLogger.log(Level.INFO, Aptasuite.class, "Exiting.");
		}
		
		// case gui
		else {
			RootClass mainWindow = new RootClass();
			mainWindow.lauchMainWindow();
			System.out.println("Exiting");
			System.exit(0);
		}

		
	}

}
