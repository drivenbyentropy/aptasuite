/**
 * 
 */
package lib.export;

import java.util.Arrays;

import lib.aptamer.datastructures.AptamerBounds;
import lib.parser.aptaplex.Read;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Implements a formatter for the FastQ format. Since we do not
 * store the quality scores of the reads (yet?), we replace these
 * with 'B' indicating an 'Unknown quality score' according to 
 * Iluminas specifications of pipelines >= 1.6
 */
public class FastqExportFormat implements ExportFormat<byte[]> {

	/**
	 * Provide a fast way of writing fake quality scores 
	 * without having to recreate the string every time
	 */
	StringBuilder qualityScores = new StringBuilder();
	
	/**
	 * The experiment name as specified in the configuration. If not name
	 * was defined, the default value is used 
	 */
	String name = Configuration.getExperiment().getName();
	
	/**
	 * Whether to include primers in the export or not
	 */
	Boolean withPrimers = Configuration.getParameters().getBoolean("Export.IncludePrimerRegions");
	
	
	/**
	 * Constructor
	 * @param name an id uniquely identifying the data to be exported. 
	 * eg. cycle name, pool name etc
	 */
	public FastqExportFormat(String name){
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see lib.export.ExportFormat#format(int, byte[])
	 */
	@Override
	public String format(int id, byte[] sequence) {
		
		// Compute bounds depending on the settings in the configuration file
		AptamerBounds bounds = withPrimers ? new AptamerBounds(0, sequence.length) : Configuration.getExperiment().getAptamerPool().getAptamerBounds(id);
		
		// Compute length depending whether primers should be exported or not
		int length = withPrimers ? sequence.length : bounds.endIndex - bounds.startIndex;
		
		// make sure the qualityScore is at least as long as the sequence
		while (qualityScores.length() < sequence.length){
			qualityScores.append('B');
		}
		
		return String.format("@%s\n%s\n+\n%s\n", 
				"AptaSuite_" + id + " " + name + " length=" + length,
				(withPrimers ? new String(sequence) : new String(Arrays.copyOfRange(sequence, bounds.startIndex, bounds.endIndex))),
				qualityScores.subSequence(0, length)
				);
		
	}

	@Override
	public String format(Read r, SequencingDirection d) {
		
		byte[] sequence = null;
		byte[] quality = null;
		String[] meta = null;
		
		// Get the data depending on the direction
		if (d == SequencingDirection.FORWARD) {
			
			meta = r.metadata_forward == null ? null : new String(r.metadata_forward).split("\n");
			sequence = r.forward_read;
			quality = r.forward_quality;
			
		} else {
			
			meta = r.metadata_reverse == null ? null : new String(r.metadata_reverse).split("\n");
			sequence = r.reverse_read;
			quality = r.reverse_quality;
			
		}

		return String.format("%s\n%s\n%s\n%s\n", 
				meta == null ? "n/a" : meta[0],
				sequence == null ? "n/a" : new String(sequence),
				meta == null ? "n/a" : meta[1],
				quality == null ? "n/a" : new String(quality)
				);
		
	}

}
