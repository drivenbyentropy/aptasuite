/**
 * 
 */
package gui.activity;

import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import utilities.AptaLoggerFormat;

/**
 * @author Jan Hoinka
 * Logging handler specialized for the <code>ProgressPane</code>
 * which publishes the logging events in the labels below the 
 * loading animation
 */
public class ProgressPaneLoggerHandler extends Handler{

	ProgressPaneController pp = null;
	
	public ProgressPaneLoggerHandler(ProgressPaneController pp) {
		
		this.pp = pp;
		this.setFormatter(new AptaLoggerFormat());
		
	}
	
	@Override
	public void close() throws SecurityException {
		
	}

	@Override
	public void flush() {
		
	}

	@Override
	public void publish(LogRecord arg0) {
		
		// Only certain levels should make it to the user screen
		if (arg0.getLevel().equals(Level.INFO) || arg0.getLevel().equals(Level.SEVERE) || arg0.getLevel().equals(Level.FINEST)){
			pp.addLogMessage(getFormatter().format(arg0));
    	}
		
	}

}
