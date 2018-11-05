package utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jan Hoinka
 *
 *         This class handles all the logging configuration required for
 *         informational and debugging purposes of AptaSuite.
 */
public class AptaLogger {
	
	private static final Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	static private Logger logger;
	static private Handler fileHandler;
	static private AptaLoggerFormat formatterTxt;
	static private String filepath;
	static private ArrayList<String> messageBuffer = new ArrayList<String>();
	static private ArrayList<Level> levelBuffer = new ArrayList<Level>();
	static private ArrayList<Class> callerBuffer = new ArrayList<Class>();
	private static final String lineSep = System.getProperty("line.separator");	

    private AptaLogger() throws IOException{
      
    	
  		// Make sure the log folder exists and create it if not
      	String projectPath = Configuration.getParameters().getString("Experiment.projectPath");
      	
    	if ( projectPath == null || !Paths.get(projectPath).toAbsolutePath().toFile().exists()) {
    		
    		throw new IOException("The Experiment.projectPath variable is not set yet.");
    		
    	}
    	
    	

  		Path logPath = Paths.get(projectPath, "logs");
  		
  		if (Files.notExists(logPath)){
  				Files.createDirectories(logPath);
  		}    	
    	
        //instance the file handler
        filepath = Paths.get(logPath.toString(), "log_" + formatter.format(new Date()) + ".txt").toString();
        fileHandler = new FileHandler(filepath,true);
        fileHandler.setLevel(Level.ALL); // We want everything logged on file
        
    	//instance the logger
        logger = Logger.getLogger(AptaLogger.class.getName());  
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        
        //instance formatter, set formatting, and handler
        formatterTxt = new AptaLoggerFormat();
        fileHandler.setFormatter(formatterTxt);
        
        logger.addHandler(fileHandler);

    }	
	
	
    public static Logger getLogger(){
        if(logger == null){
            try {
                new AptaLogger();
            } catch (Exception e) {
            	logger = null;
            }
        }
        return logger;
    }
    
    
    public static synchronized void log(Level level, Class caller, String msg){
    	// if the logger is not ready yet, we put the messages into a queue
    	if (getLogger() == null){
    		
    		messageBuffer.add(msg);
    		levelBuffer.add(level);
    		callerBuffer.add(caller);
    		
    		// Only certain levels should make it to the user screen
			if (level.equals(Level.INFO) || level.equals(Level.SEVERE)){
        		System.out.println(msg);
        		System.out.flush();
        	}
    	}
    	else{
    		// add the messages in the buffer to the log 
    		if (!messageBuffer.isEmpty()){
    			
    			for (int x=0; x<messageBuffer.size(); x++){
    				
    				getLogger().log(levelBuffer.get(x), callerBuffer.get(x).getName() + "\n" + messageBuffer.get(x));
    	        	
    			
    			}
    			messageBuffer.clear();
    			levelBuffer.clear();
    			callerBuffer.clear();
    			
    		}
    		
    		// log the current message
			getLogger().log(level, caller.getName() + lineSep + msg);
    		
    		// Only certain levels should make it to the user screen
    		if (level.equals(Level.INFO) || level.equals(Level.SEVERE)){
        		System.out.println(msg);
        		// Make sure we get it right away on screen
        		System.out.flush();
        	}
    	}
    }

    public static synchronized void log(Level level, Class caller, Exception e) {
    	log(level, caller, org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
    }
    
    /**
     * Closes all file handlers and logger instances. This function must be called 
     * e.g. when changing data sets.
     */
    public static void close() {
    	
        filepath = null;
        fileHandler.close();
        logger = null;
        formatterTxt = null;
        
    }
    
}


