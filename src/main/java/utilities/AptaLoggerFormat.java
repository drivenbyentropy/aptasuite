/**
 * 
 */
package utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author hoinkaj
 * Defines the format used for logging the process in AptaSuite
 */
public class AptaLoggerFormat extends Formatter{

	private static final DateFormat format = new SimpleDateFormat("h:mm:ss");
	private static final String lineSep = System.getProperty("line.separator");	
	
	public AptaLoggerFormat() {super();}
	
	@Override
	public String format(final LogRecord record){
		
		StringBuilder output = new StringBuilder()
			.append("[")
			.append(format.format(new Date(record.getMillis()))).append(" | ")
			.append(record.getLevel()).append(" | ")
			.append(Thread.currentThread().getName())
			.append("]: ")
			.append(record.getMessage()).append(' ')
			.append(lineSep)
			.append(lineSep);
		return output.toString();		
		
	}
	
}
