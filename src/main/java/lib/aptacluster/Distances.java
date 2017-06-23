/**
 * 
 */
package lib.aptacluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Jan Hoinka
 * Implements a variety of distances used for
 * AptaCluster
 */
public class Distances {

	/**
	 * Implements the kmer distance. Assumes that <code>a.length == b.length</code>  
	 * @param a
	 * @param b
	 * @param kmer_size
	 * @return
	 */
	public static double KmerDistance(byte[] a, byte[] b, int kmer_size){
		
		// count kmers
		Map<String, int[]> kmer_map = new HashMap<String, int[]>();
		
		for (int x=0; x<a.length-kmer_size; x++){
			
			String kmer_a = new String(a, x, kmer_size);
			String kmer_b = new String(b, x, kmer_size);
		
			if (!kmer_map.containsKey(kmer_a)){
				kmer_map.put(kmer_a, new int[]{1,0});
			}
			else{
				kmer_map.get(kmer_a)[0] += 1;
			}
			
			if (!kmer_map.containsKey(kmer_b)){
				kmer_map.put(kmer_b, new int[]{0,1});
			}
			else{
				kmer_map.get(kmer_b)[1] += 1;
			}
			
		}
		
		// computes the distance
		double distance = 0.0;
		
		if (kmer_map.size() <= 1){ return 2.0; } //subject to impovement. set to maximal distance in cases where k is larger than both sequences 
		
		for (  Entry<String, int[]> kmer : kmer_map.entrySet()){
			
			double count_x = 0.0;
			double count_y = 0.0;
			
			count_x = kmer.getValue()[0];
			count_x /= a.length - kmer_size + 1;
			
			count_y = kmer.getValue()[1];
			count_y /= b.length - kmer_size + 1;
			
			distance += Math.pow(Math.abs(count_x - count_y), 2);
		}
		
		return distance;
	}
	
}
