package lib.export;

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
	 * The total size of the primers, required to 
	 * compute the overall aptamer length to be exported 
	 */
	Integer primerLengths = Configuration.getParameters().getString("Experiment.primer5").length() + Configuration.getParameters().getString("Experiment.primer3").length(); 
	
	/**
	 * The 5' primer sequence 
	 */
	String primer5 = Configuration.getParameters().getString("Experiment.primer5");

	/**
	 * The 3' primer sequence 
	 */
	String primer3 = Configuration.getParameters().getString("Experiment.primer3");
	
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
		
		// Compute length depending whether primers should be exported or not
		int length = withPrimers ? sequence.length + primerLengths : sequence.length;
		
		// Build the sequence with newline if the total lengths exceeds the specification
		String rawSequence = (withPrimers ? primer5 + new String(sequence) + primer3 : new String(sequence));
		StringBuilder formattedSequence = new StringBuilder(length);
		int breakCounter = 0;
		for (int x = 0; x<length; x++,breakCounter++){

			if (breakCounter == 80){
				formattedSequence.append("\n");
				breakCounter = 0;
			}
			
			formattedSequence.append(rawSequence.charAt(x));
		}
	
		return String.format(">%s\n%s\n", 
				"AptaSuite_" + id + "|" + name + "|length=" + length,
				formattedSequence.toString()
				);
		
	}

}
