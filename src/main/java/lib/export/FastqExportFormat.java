/**
 * 
 */
package lib.export;

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
		
		// compute length depending whether primers should be exported or not
		int length = withPrimers ? sequence.length + primerLengths : sequence.length;
		
		// make sure the qualityScore is at least as long as the sequence
		while (qualityScores.length() < sequence.length+primerLengths){
			qualityScores.append('B');
		}
		
		return String.format("@%s\n%s\n+\n%s\n", 
				"AptaSuite_" + id + " " + name + " length=" + length,
				(withPrimers ? primer5 + new String(sequence) + primer3 : new String(sequence)),
				qualityScores.subSequence(0, length)
				);
		
	}

}
