package lib.export;

import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Formats a structure array from CapR or SFold into 
 * a human readable output for data export
 */
public class StructureExportFormat implements ExportFormat<double[]> {

	
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
	
	
	
	/* (non-Javadoc)
	 * @see lib.export.ExportFormat#format(int, java.lang.Object)
	 */
	@Override
	public String format(int id, double[] structure) {
		
		// We need the actual sequence as well
		byte[] sequence = Configuration.getExperiment().getAptamerPool().getAptamer(id);
		
		// Compute length depending whether primers should be exported or not
		int structure_length = structure.length/5;
		int length = withPrimers ? structure_length + primerLengths : structure_length;
		
		
		int index_start = withPrimers ? 0 : primer5.length()-1;
		int index_end = withPrimers ? length : primer5.length() + sequence.length;
		
		
		
		// Build the sequence with newline if the total lengths exceeds the specification
		String rawSequence = (withPrimers ? primer5 + new String(sequence) + primer3 : new String(sequence));
		
		StringBuilder formattedStructure = new StringBuilder(6*length);

		// Add the structure information
		for (int s=0; s<5; s++){ // 0=Hairpin, 1=Buldge, 2=Inner, 3=Multi, 4=Dangling
			
			for (int x=index_start; x<index_end; x++){
				formattedStructure.append(structure[s*structure_length + x]);
				
				if (x<index_end-1){
					formattedStructure.append("\t");
				}
				
			}
			
			if (s < 4){
				formattedStructure.append("\n");
			}
			
		}
		
		return String.format(">%s\n%s\n", 
				"AptaSuite_" + id + "\t" + name + "\t" + rawSequence + "\t" + "length=" + length,
				formattedStructure.toString()
				);
	}

}
