/**
 * 
 */
package lib.export;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import exceptions.InvalidConfigurationException;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.SelectionCycle;
import lib.aptamer.datastructures.StructurePool;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * This class contains all the functions required
 * to export the data of an experiment into 
 * text files.
 */
public class Export {

	
	/**
	 * Writes the specified pool <code>p</code> to 
	 * persistent storage
	 * @param ap instance of <code>AptamerPool</code>
	 * @param p the location at which the file should be created
	 */
	public void Pool(AptamerPool ap, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);

		// Do the same for the formatter
		ExportFormat<byte[]> formatter = null;
		switch (Configuration.getParameters().getString("Export.SequenceFormat")) {
		
			case "fastq": 
				formatter = new FastqExportFormat(Configuration.getExperiment().getName());
				break;
				
			case "fasta":
				formatter = new FastaExportFormat(Configuration.getExperiment().getName());
				break;
		
			default:
				AptaLogger.log(Level.SEVERE, this.getClass(), "Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized. Exiting");
				throw new InvalidConfigurationException("Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized.");
		}
		
		// Write sequences
		for ( Entry<byte[], Integer> entry : ap.iterator()){
			writer.write(formatter.format(entry.getValue(), entry.getKey()));
		}
		
		// Finalize
		writer.close();
	}

	/**
	 * Writes the specified SelectionCycle <code>p</code> to 
	 * persistent storage
	 * @param ap instance of <code>AptamerPool</code>
	 * @param p the location at which the file should be created
	 */
	public void Cycle(SelectionCycle sc, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);

		// Do the same for the formatter
		ExportFormat<byte[]> formatter = null;
		switch (Configuration.getParameters().getString("Export.SequenceFormat")) {
		
			case "fastq": 
				formatter = new FastqExportFormat(sc.getName());
				break;
				
			case "fasta":
				formatter = new FastaExportFormat(sc.getName());
				break;
		
			default:
				AptaLogger.log(Level.SEVERE, this.getClass(), "Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized. Exiting");
				throw new InvalidConfigurationException("Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized.");
		}
		
		// Write sequences
		for ( Entry<byte[], Integer> entry : sc.sequence_iterator()){
			writer.write(formatter.format(entry.getValue(), entry.getKey()));
		}
		
		// Finalize
		writer.close();
	}
	
	/**
	 * Writes the specified SelectionCycle <code>p</code> to 
	 * persistent storage
	 * @param ap instance of <code>AptamerPool</code>
	 * @param p the location at which the file should be created
	 */
	public void Structures(StructurePool sp, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);

		// Write structures 
		for ( Map.Entry<Integer, double[]> entry : sp.iterator() ){
			//ID and Sequence
			writer.write(">Aptasuite_"+ entry.getKey() + " ");
		}
		
		// Finalize
		writer.close();
	}
	
}
