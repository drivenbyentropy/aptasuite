/**
 * 
 */
package lib.parser.aptaplex.distances;

/**
 * @author Jan Hoinka
 * Implements a simple edit distance based search. This search will be used if the
 * Bitap algorithm is not applicable. I.e. for word sizes > 32.
 *
 */
public class EditDistance implements Distance{
	
	@Override
	public Result indexOf(byte[] doc, byte[] pattern, int tolerance, int doc_range_lower, int doc_range_upper) {
		
		// no match
		if (doc_range_upper - doc_range_lower + 1 < pattern.length){
			return null; 
		}
		
		Integer best_index = null;
		Integer best_score = null;
		
		for (int k=0; k<doc_range_upper - doc_range_lower - pattern.length + 1; k++){
			
			int current_best_index = doc_range_lower+k;
			int current_best_score = 0;
			
			for (int x=doc_range_lower+k, y=0; y<pattern.length; x++, y++){
				
				if (doc[x] != pattern[y]){
					current_best_score++;
				}
				
			}
			
			if ((best_score == null || current_best_score < best_score) && current_best_score <= tolerance){
				best_index = current_best_index;
				best_score = current_best_score;
			}
		
		}	
		
		if (best_index == null){
			return null;
		}
			
		Result best_match = new Result();
		best_match.index = best_index;
		best_match.errors = best_score;
		
		return best_match;
	}

}
