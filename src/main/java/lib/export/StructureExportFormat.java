package lib.export;

import java.util.Arrays;

import lib.aptamer.datastructures.AptamerBounds;
import lib.parser.aptaplex.Read;
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
	
	
	/* (non-Javadoc)
	 * @see lib.export.ExportFormat#format(int, java.lang.Object)
	 */
	@Override
	public String format(int id, double[] structure) {
		
		// We need the actual sequence as well
		byte[] sequence = Configuration.getExperiment().getAptamerPool().getAptamer(id);
		
		// Compute bounds depending on the settings in the configuration file
		AptamerBounds bounds = withPrimers ? new AptamerBounds(0, sequence.length) : Configuration.getExperiment().getAptamerPool().getAptamerBounds(id);
		
		// Compute length depending whether primers should be exported or not
		int structure_length = structure.length/5;
		int length = withPrimers ? sequence.length : bounds.endIndex - bounds.startIndex;
		
		
		// Build the sequence with newline if the total lengths exceeds the specification
		String rawSequence = new String(Arrays.copyOfRange(sequence, bounds.startIndex, bounds.endIndex));
		
		StringBuilder formattedStructure = new StringBuilder(6*length);

		// Add the structure information
		for (int s=0; s<5; s++){ // 0=Hairpin, 1=Buldge, 2=Inner, 3=Multi, 4=Dangling
			
			for (int x=bounds.startIndex; x<bounds.endIndex; x++){
				formattedStructure.append(structure[s*structure_length + x]);
				
				if (x<bounds.endIndex-1){
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


	@Override
	public String format(Read r, SequencingDirection d) {
		// TODO Auto-generated method stub
		return null;
	}

}
