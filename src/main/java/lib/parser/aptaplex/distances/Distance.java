/**
 * 
 */
package lib.parser.aptaplex.distances;

/**
 * @author Jan Hoinka
 * 
 * Defines the contract all matching algorithms used to identify the 
 * best matching barcodes or primers to the contigs have to adhere to
 *
 */
public interface Distance {
	
	/**
	 * Performs the distance calculation
	 * @param doc the sequence to find the <code>pattern</code> in
	 * @param pattern the pattern to search for
	 * @param tolerance the maximal number of allowed mismatches between the pattern and the document
	 * @param doc_range_lower Start index (inclusive) of the range to search in the document. 
	 * @param doc_range_upper End index (exclusive) of the range to search in the document. 
	 * @return Result instance containing start index of the match and score. null if matching failed.
	 */
	public Result indexOf(byte[] doc, byte[] pattern, int tolerance, int doc_range_lower, int doc_range_upper);
	
}
