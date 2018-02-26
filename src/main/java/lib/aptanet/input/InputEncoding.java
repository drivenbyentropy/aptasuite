/**
 * 
 */
package lib.aptanet.input;

/**
 * @author Jan Hoinka
 * 
 * Enumeration of valid encodings for aptamer data.
 */
public enum InputEncoding {

	
	/**
	 * First channel corresponds to BPPM.
	 * Next 16 channels encode base pairings for all
	 * possible combinations of nucleoties, ie.
	 * AA, AC, AG, AT, CA, CC, CG, CT, GA, GC, GG, GT, TA, TC, TG, TT 
	 */
	BPPM_PLUS_SIXTEEN_CHANNELS,
	
	/**
	 *  First channel corresponds to BPPM.
	 *  Next channel represents base pairings with the following
	 *  encoding (normalized between 0-1):
	 *  AA = 0
	 *  AC = 1
	 *  AG = 2 
	 *  AT = 3
	 *  CA = 4
	 *  CC = 5
	 *  CG = 6
	 *  CT = 7
	 *  GA = 8
	 *  GC = 9
	 *  GG = 10
	 *  GT = 11
	 *  TA = 12
	 *  TC = 13
	 *  TG = 14
	 *  TT = 15
	 */
	BPPM_PLUS_ONE_CHANNEL
	
}
