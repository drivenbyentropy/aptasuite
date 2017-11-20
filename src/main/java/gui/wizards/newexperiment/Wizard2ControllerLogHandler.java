package gui.wizards.newexperiment;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import utilities.AptaLoggerFormat;

public class Wizard2ControllerLogHandler extends Handler{

	Wizard2Controller w2c = null;
	
	public Wizard2ControllerLogHandler(Wizard2Controller w2c) {
		
		this.w2c = w2c;
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
		if (arg0.getLevel().equals(Level.INFO) || arg0.getLevel().equals(Level.CONFIG) || arg0.getLevel().equals(Level.SEVERE)){
			w2c.addLogMessage(getFormatter().format(arg0));
    	}
		
	}
	
}
