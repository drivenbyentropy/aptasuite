/**
 * 
 */
package utilities;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

/**
 * @author Jan Hoinka
 * A series of color schemes used throughout the application
 */
public class AptaColors {

	public static class Nucleotides { 
	
		public static final Color ADENINE = Color.GREEN;
		public static final Color CYTOSINE = Color.web("#cec700");
		public static final Color GUANINE = Color.BLUE;
		public static final Color THYMINE = Color.RED;
		public static final Color URACIL = Color.RED;
		public static final Color PRIMERS = Color.web("#a5a5a5");
		
	}
	
	public static final Map<Byte, Color> NucleotidesMap = new HashMap<Byte, Color>();
	static {
		
		NucleotidesMap.put((byte)'A', Nucleotides.ADENINE);
		NucleotidesMap.put((byte)'C', Nucleotides.CYTOSINE);
		NucleotidesMap.put((byte)'G', Nucleotides.GUANINE);
		NucleotidesMap.put((byte)'T', Nucleotides.THYMINE);
		NucleotidesMap.put((byte)'U', Nucleotides.URACIL);
		
	}
	
	public static class Contexts {
		
		public static final Color HAIRPIN = Color.web("#FF7070");
		public static final Color BULGELOOP = Color.web("#FA9600");
		public static final Color INNERLOOP = Color.web("#A0A0FF");
		public static final Color MULTIPLELOOP = Color.CYAN;
		public static final Color DANGLINGEND =  Color.PINK;
		public static final Color PAIRED = Color.web("#C8C8C8");
		
	}
	
}
