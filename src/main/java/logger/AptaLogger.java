package logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Jan Hoinka
 *
 *         This class handles all the logging configuration required for
 *         informational and debugging purposes of AptaSuite.
 */
public class AptaLogger {
	static private FileHandler fileTxt;
	static private SimpleFormatter formatterTxt;

	static public void setup() throws IOException {

	    // get the global logger to configure it
	    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	    logger.setLevel(Level.INFO);
	    fileTxt = new FileHandler("Logging.txt");

	    // create a TXT formatter
	    formatterTxt = new SimpleFormatter();
	    fileTxt.setFormatter(formatterTxt);
	    logger.addHandler(fileTxt);

	  }

}
