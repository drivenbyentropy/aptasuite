/**
 * 
 */
package lib.structure.rnafold;

/**
 * @author Jan Hoinka
 * Return value for the RNAFold4j API
 */
public class MFEData {

	public byte[] structure;
	public double mfe;
	
	public MFEData(byte[] structure, double mfe) {
		this.structure = structure;
		this.mfe = mfe;
	}
	
}
