/**
 * 
 */
package lib.aptanet;

/**
 * @author Jan Hoinka
 * Class representing a combination of an aptamer id
 * and some form of label (eg count, freuqncy, enrichment)
 * of the species. To be used as a return value for next()
 * in DataInputIterator.
 */
public class AptamerLabelPair {

	int aptamer_id;
	double score;

	public AptamerLabelPair(int a_id, double s) {
		
		this.aptamer_id = a_id;
		this.score = s;
		
	}
	
}
