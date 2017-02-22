/**
 * 
 */
package lib.parser.aptaplex.distances;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jan Hoinka
 * 
 *         Implements the Bitap algorithm. The main routine was released under
 *         the Apache License 2 at <a href=
 *         "http://code.google.com/p/google-diff-match-patch/">http://code.google.com/p/google-diff-match-patch/</a>
 *         and adapted to our needs.
 * 
 *         Locate the best instance of 'pattern' in 'text' near 'loc' using the
 *         Bitap algorithm. Returns -1 if no match found.
 *
 * @param text
 *            The text to search.
 * @param pattern
 *            The pattern to search for.
 * @return Best match index or -1.
 */
public class BitapDistance implements Distance{

	
	/**
	 * The alphabet for the bitap algorithm, adapted for DNA. Since we are matching a limited
	 * number of barcodes and primers, we can lazy load these as they come in.
	 */
	private Map<byte[], HashMap<Byte, Integer>> alphabets = new HashMap<byte[], HashMap<Byte, Integer>>();

	
	@Override
	public Result indexOf(byte[] text, byte[] pattern, int tolerance, int doc_range_lower, int doc_range_upper) {
		
		Result result = null;

		// Initialize the alphabet.
        Map<Byte, Integer> alphabet = initAlphabet(pattern);

		// Initialize the bit arrays.
		int matchmask = 1 << (pattern.length - 1);

		int[] last_rd = new int[0];
		for (int d = 0; d <= tolerance; d++) {

			int[] rd = new int[text.length + pattern.length + 2];
			rd[text.length + pattern.length + 1] = (1 << d) - 1;
			for (int j = doc_range_upper + pattern.length; j > doc_range_lower; j--) {
				int charMatch;
				if (text.length <= j - 1 || !alphabet.containsKey(text[j - 1])) {
					// Out of range.
					charMatch = 0;
				} else {
					charMatch = alphabet.get(text[j - 1]);
				}
				if (d == 0) {
					// First pass: exact match.
					rd[j] = ((rd[j + 1] << 1) | 1) & charMatch;
				} else {
					// Subsequent passes: fuzzy match.
					rd[j] = (((rd[j + 1] << 1) | 1) & charMatch) | (((last_rd[j + 1] | last_rd[j]) << 1) | 1)
							| last_rd[j + 1];
				}
				if ((rd[j] & matchmask) != 0) {
					result = new Result();
					result.index = j - 1;
					result.errors = d;
					return result;
				}
			}
			last_rd = rd;
		}
		return result;
	}

    /**
     * Initialize the alphabet for the Bitap algorithm.
     *
     * @param pattern The text to encode.
     * @return Hash of character locations.
     */
    public Map<Byte, Integer> initAlphabet(byte[] pattern) {
    	
    	// check if we have previously computed the patterns alphabet
    	if ( alphabets.containsKey(pattern) ){
    		return alphabets.get(pattern);
    	}
    	
    	// else we need to compute it
        HashMap<Byte, Integer> s = new HashMap<Byte, Integer>();
        for (byte c : pattern) {
            s.put(c, 0);
        }
        int i = 0;
        for (byte c : pattern) {
            s.put(c, s.get(c) | (1 << (pattern.length - i - 1)));
            i++;
        }
        
        // and add it to the map before returning
        alphabets.put(pattern, s);
        
        return s;
    }	
	
}
