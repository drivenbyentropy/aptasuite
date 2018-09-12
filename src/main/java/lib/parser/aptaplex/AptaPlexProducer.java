/**
 * 
 */
package lib.parser.aptaplex;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import exceptions.InvalidSequenceReadFileException;
import lib.export.CompressedExportWriter;
import lib.export.ExportWriter;
import lib.export.UncompressedExportWriter;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Pair;

/**
 * @author Jan Hoinka
 * This class implements the Producer of the parser. 
 * Depending on the specified file format and single/paired end sequencing,
 * it iterates over the file, exacts the relevant information, and add it
 * to a queue for the consumers to process.
 *
 */
public class AptaPlexProducer implements Runnable{


	/**
	 * The queue to fill
	 */
	private BlockingQueue<Object> queue = null;
	
	/**
	 * The map containing the information for exporting the failed reads to undetermined
	 */
	private Map<Path, Pair<ExportWriter,ExportWriter>> undeterminedExportWriterMap = null; //TODO: CONTINUE HERE, EXPAND TO FORWARD AND REVERSE READS
	
	/**
	 * Defines whether this instance is concerned with multiplexing the data, or if it has
	 * previously been demultiplexed
	 */
	private Boolean isPerFile = Configuration.getParameters().getBoolean("AptaplexParser.isPerFile");
	
	/**
	 * Path to export data for easy access
	 */
	private Path exportPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "export");
	
	public AptaPlexProducer(BlockingQueue<Object> queue, Map<Path, Pair<ExportWriter,ExportWriter>> undeterminedExportWriterMap){
		
		this.queue = queue;
		this.undeterminedExportWriterMap = undeterminedExportWriterMap;
	
	}
	
	/**
	 * The total number of processed reads
	 */
	public int totalProcessedReads = 0;
	
	@Override
	public void run() {

		// the files we need to process
		String[] forward_files = Configuration.getParameters().getStringArray("AptaplexParser.forwardFiles");
		String[] reverse_files = Configuration.getParameters().getStringArray("AptaplexParser.reverseFiles");

		// Sanity checks
		// make sure the forward read files exist
		if (forward_files.length == 0)
		{
			throw new InvalidSequenceReadFileException("No forward read files where specified. Please check your configuration.");
		}
		
		// if they exist, we need the same number of forward and reverse files, or no reverse files in 
		// case of single end sequencing
		if (reverse_files.length != 0 && forward_files.length!=reverse_files.length){
			throw new InvalidSequenceReadFileException("The number of forward and reverse read files must be identical.");
		}
		
		//iterate over all files and populate the queue
		for (int x=0; x<forward_files.length; x++){
			
			Path current_forward_file_path = Paths.get(forward_files[x]);
			
			Path current_reverse_file_path = null;
			// at this point we know reverse_files.length is either 0 or equal to forward_file.length
			if (reverse_files.length != 0){
				current_reverse_file_path = Paths.get(reverse_files[x]);
			}
			
			// Create a new Reader instance. Use reflection so we can define the backend in the configuration
			Reader reader = null;
			Class reader_class = null;
			try {
				reader_class = Class.forName("lib.parser.aptaplex." + Configuration.getParameters().getString("AptaplexParser.reader"));
			} catch (ClassNotFoundException e) {

				AptaLogger.log(Level.SEVERE, this.getClass(), "Error, the backend for the Reader could not be found.");
				AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
				e.printStackTrace();
				System.exit(0);
			}
			
			// Try to instantiate the class
			boolean instanceSuccess = false;
			try {
				reader = (Reader)reader_class.getConstructor(Path.class, Path.class).newInstance(current_forward_file_path, current_reverse_file_path);
				instanceSuccess = true;
			} catch (InstantiationException e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), "Error, could not instantiate the backend for the AptaplexParser.reader");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking construtor of AptaplexParser.reader backend");
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} finally{
				if (!instanceSuccess){
					AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking AptaplexParser.reader backend");
					System.exit(0);
				}
			}
			
			// at this point we can fill the undetermined map if requested by the user
			if (this.undeterminedExportWriterMap != null) {
				
				// first create the export writers
				Path forward_export_path = Paths.get(exportPath.toString(), "undetermined_"+current_forward_file_path.getFileName().toString()); 
				ExportWriter forward_export_writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
				forward_export_writer.open(forward_export_path);
				
				Path reverse_export_path = null;
				if (current_reverse_file_path != null) {
					
					reverse_export_path = Paths.get(exportPath.toString(), "undetermined_"+current_reverse_file_path.getFileName().toString()); 
					ExportWriter reverse_export_writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
					reverse_export_writer.open(reverse_export_path);
					
				}
				
				undeterminedExportWriterMap.put(current_forward_file_path, new Pair(forward_export_writer,reverse_export_path));
				
			}
			
			try {
				// get the first read
				Read read = reader.getNextRead();
				
				// do the same for the remaining reads
				while (read != null){
					
					// are we multiplexing?
					if(isPerFile){

						// add cycle information to read
						read.selection_cycle = Configuration.getExperiment().getAllSelectionCycles().get(x);
						
					}
					
					// add source files
					read.source_forward = current_forward_file_path;
					read.source_reverse = current_reverse_file_path;
					
					// put read into to queue
					queue.put(read);
					totalProcessedReads++;
					
					// get the next read
					read = reader.getNextRead();
				}
			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			reader.close();
			reader = null;
			
		}
		
		// at the end we need to add a poison pill to 
		// the queue to let the consumers know when to stop
		AptaLogger.log(Level.CONFIG, this.getClass(), "Added poison pill to parsing queue");
		try {
			queue.put(Configuration.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		
	}
}