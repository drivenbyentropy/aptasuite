/**
 * 
 */
package lib.export;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.logging.Level;
import utilities.AptaLogger;

/**
 * @author Jan Hoinka
 *
 */
public class UncompressedExportWriter implements ExportWriter{

	BufferedWriter writer = null;
	FileOutputStream  out = null;
	Path p = null;
	
	@Override
	public void open(Path p) {
		this.p = p;
		
		try {
			
			out = new FileOutputStream(p.toFile(), false);
			writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		
		} catch (IOException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Could not create file " + p.toString());
			AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
			e.printStackTrace();
			System.exit(1);
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Created file " + p.toString());
		
	}

	@Override
	public void write(String data) {
		
		try {
			
			writer.write(data);
			
		} catch (IOException e) {
			
			AptaLogger.log(Level.SEVERE, this.getClass(), "Could not write " + data + " to file " + p.toString());
			AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
			e.printStackTrace();
			System.exit(1);
			
		}

	}

	@Override
	public void close() {
		
		try {
			writer.close(); // will flush in the process
			out.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Closing file " + p.toString());
		
	}
	
}
