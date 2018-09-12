package lib.export;

import lib.aptamer.datastructures.AptamerBounds;
import lib.parser.aptaplex.Read;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Implements a formatter for the FastA format. The format
 * specification follows the <a href="https://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE_TYPE=BlastDocs&DOC_TYPE=BlastHelp">NCBI</a> format.
 * 
 */
public class FastaExportFormat implements ExportFormat<byte[]> {

	/**
	 * String written into the description of the reads to itentify 
	 * the selection round, pool name etc 
	 */
	String name = null;
	
	/**
	 * Whether to include primers in the export or not
	 */
	Boolean withPrimers = Configuration.getParameters().getBoolean("Export.IncludePrimerRegions");
	
	/**
	 * The maximum number of characters per line.  It is recommended that all 
	 * lines of text be shorter than 80 characters in length according to the NCBI 
	 * specification.
	 */
	Integer lineWidth = 80;
	
	/**
	 * Constructor
	 * @param name an id uniquely identifying the data to be exported. 
	 * eg. cycle name, pool name etc
	 */
	public FastaExportFormat(String name){
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
		
		// Build the sequence with newline if the total lengths exceeds the specification
		StringBuilder formattedSequence = new StringBuilder(length + new Double(length/80.).intValue());
		int breakCounter = 0;
		for (int x = withPrimers ?  0 : bounds.startIndex; x < (withPrimers ? sequence.length : bounds.endIndex); x++,breakCounter++){

			if (breakCounter == 80){
				formattedSequence.append("\n");
				breakCounter = 0;
			}
			
			formattedSequence.append((char) sequence[x]);
		}
	
		return String.format(">%s\n%s\n", 
				"AptaSuite_" + id + "|" + name + "|length=" + length,
				formattedSequence.toString()
				);
		
	}

	@Override
	public String format(Read r, SequencingDirection d) {
		
		// get data according to direction
		byte[] sequence = null;
		byte[] description = null;
		
		if (d == SequencingDirection.FORWARD) {
			
			sequence = r.forward_read;
			description = r.metadata_forward;
			
		}
		else {
			
			sequence = r.reverse_read;
			description = r.metadata_reverse;
			
		}
		
		// Build the sequence with newline if the total lengths exceeds the specification
		StringBuilder formattedSequence = new StringBuilder(sequence.length + new Double(sequence.length/80.).intValue());
		int breakCounter = 0;
		for (int x = 0; x < sequence.length; x++,breakCounter++){

			if (breakCounter == 80){
				formattedSequence.append("\n");
				breakCounter = 0;
			}
			
			formattedSequence.append((char) sequence[x]);
		}
		
		// Do the same for the description
		StringBuilder formattedDescription = new StringBuilder(description.length + new Double(description.length/80.).intValue());
		breakCounter = 0;
		for (int x = 0; x < description.length; x++,breakCounter++){

			if (breakCounter == 80){
				formattedDescription.append("\n");
				breakCounter = 0;
			}
			
			formattedDescription.append((char) description[x]);
		}
	
		return String.format(">%s\n%s\n", 
				formattedDescription.toString(),
				formattedSequence.toString()
				);
		
	}
	
	

}
