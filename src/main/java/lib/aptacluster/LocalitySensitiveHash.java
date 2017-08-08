/**
 * 
 */
package lib.aptacluster;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

import lib.aptamer.datastructures.AptamerBounds;
import utilities.AptaLogger;

/**
 * @author Jan Hoinka
 * Implements the locality sensitive hashing family for
 * AptaCluster. 
 */
public class LocalitySensitiveHash {

	/**
	 * The randomized region size for which aptaCLUSTER should be applied to.
	 * All aptamers with different randomized region sizes will be ignored for this instance.
	 */
	private Integer randomizedRegionSize = null; 
	
	/**
	 * The first index that was sampled for this instance  
	 */
	private Integer startIndex = null;
	
	/**
	 * The coprime selected to sample the remaining 
	 */
	private Integer coprime = null;
	
	/**
	 * The locality sensitive hashing dimension into which the data will be 
	 * reduced to.
	 */
	private Integer lshDimension = null;
	
	/**
	 * Random seed for this instance
	 */
	private Random rand = new Random();
	
	/**
	 * List of instances of <code>LocalitySensitiveHash<code>. Used to make sure no two 
	 * equal hash functions are drawn.
	 */
	private ArrayList<LocalitySensitiveHash> lhsInstances = null;
	
	private ArrayList<Integer> lshPositions = null;
	
	/**
	 * Draws a LSH function from the LSH family. 
	 * @param randomizedRegionSize size of the nucleotide string for which the LSH should be designed
	 * @param lshDimension the dimension <code>x</code> of the reduced representation of the objects. 
	 * Note that <code>x <= randomizedRegionSize</code>. The smaller <code>x</code> the less similar
	 * the objects can be while still being hashed into the same bucket.
	 */
	public LocalitySensitiveHash(int randomizedRegionSize, int lshDimension){
		
		this.randomizedRegionSize = randomizedRegionSize;
		this.lshDimension = lshDimension;
	
		createHashPositions();
		logLSH();
	}
	
	/**
	 * Draws a unique LSH function from the LSH family. 
	 * @param randomizedRegionSize size of the nucleotide string for which the LSH should be designed
	 * @param lshDimension the dimension <code>x</code> of the reduced representation of the objects. 
	 * Note that <code>x <= randomizedRegionSize</code>. The smaller <code>x</code> the less similar
	 * the objects can be while still being hashed into the same bucket.
	 * @param lshInstances list of already drawn lsh functions. The new LSH function is guaranteed to be  
	 * distinct from any instance in <code>lshInstances</code>.
	 */
	public LocalitySensitiveHash(int randomizedRegionSize, int lshDimension, ArrayList<LocalitySensitiveHash> lshInstances){
		
		this.randomizedRegionSize = randomizedRegionSize;
		this.lshDimension = lshDimension;
		setLhsInstances(lshInstances);
	
		createHashPositions();
		logLSH();
	}
	
	/**
	 * Create all numbers that are coprime of, and smaller than x
	 * return random element from that list.
	 * This function is used to initialize the hash functions used in LSH in order to guarantee the minimal number of overlaps between indices
	 * @return a random number that is co-prime of x and smaller than x
	 */
	private int getRandomCoprimeNumber()
	{
		//trivial case
		if (randomizedRegionSize == 1)
		{
			return 1;
		}
		
		ArrayList<Integer> allCoprimes = new ArrayList<Integer>();
		
		//a=2   -> we don't want the tivial case of 1 which is coprime to every number and would create consecutive indices for the hash.
		//a<x-1 -> consecutive numbers are always coprime. this constitutes the same scenario as above.
		for (int a=2; a<(randomizedRegionSize-1); ++a)
		{
			//a and x are coprime iff their gcd is 1
			if (BigInteger.valueOf(a).gcd(BigInteger.valueOf(randomizedRegionSize)).intValue() == 1)
			{
				allCoprimes.add(a);
			}
		}
		
		return allCoprimes.get(rand.nextInt(allCoprimes.size()));
	}
	
	/**
	 * If multiple LSH instances exist, make sure no two contain the same parameters
	 * @param startIndex will be a unique start index between 0 and <code>randomizedRegionSize</code>
	 * @param coprime will be a coprime to <code>randomizedRegionSize</code> and unique in combination with <code>startIndex</code>
	 */
	private void setUniqueStartIndexAndCoprime(){
		
		//generate random start position and coprime
		startIndex = rand.nextInt(randomizedRegionSize); 
		coprime = getRandomCoprimeNumber();
		
		// make sure this combination has never been sampled before
		if (lhsInstances != null){
			
			boolean found = true;
			
			while (found){
			
				found = false;
				for ( LocalitySensitiveHash lsh : lhsInstances ){
					if (this.equals(lsh)){
						found = true;
					}
				}
				
				if (found){
					startIndex = rand.nextInt(randomizedRegionSize); 
					coprime = getRandomCoprimeNumber();
				}
			
			}
		}
		
	}

	/**
	 * Generate a set of indices at which define the reduced dimension of this instance.
	 */
	private void createHashPositions(){
		
		// generate a unique pair of startIndex and coprime
		setUniqueStartIndexAndCoprime();

		// initialize 
		lshPositions = new ArrayList<Integer>();
		lshPositions.add(startIndex);
		
		// compute remaining indices based on i_x = (i_(x-1) + k*n) mod m where k is any random number, 
		// n is a fixed relative prime number (coprime) to m, and m is the seq_length 
		for (int v=1; v<lshDimension-1; ++v)
		{
			lshPositions.add((lshPositions.get(v-1) + coprime) % randomizedRegionSize);
		}
		
		// sort the hash
		Collections.sort(lshPositions);
		
	}
	
	/**
	 * Computes the string resulting from applying the LSH
	 * onto <code>sequence<code>. 
	 * @param sequence the input sequence. Note that <code>sequence</code>  must be of size
	 * <code>randomizedRegionSize</code>
	 * @param bounds the bounds of the randomized region. primers will be ignored for the hash
	 * @return the string resulting from concatenating the nucleotides from <code>sequence</code> at the indices as specified in 
	 * <code>lshPositions</code>
	 */
	public byte[] getHash(byte[] sequence, AptamerBounds bounds){
		
		return getHash(sequence, bounds.startIndex, bounds.endIndex);
		
	}
	
	/**
	 * Computes the string resulting from applying the LSH
	 * onto <code>sequence<code>. 
	 * @param sequence the input sequence. Note that <code>sequence</code>  must be of size
	 * <code>randomizedRegionSize</code>
	 * @param bounds the bounds of the randomized region. primers will be ignored for the hash
	 * @return the string resulting from concatenating the nucleotides from <code>sequence</code> at the indices as specified in 
	 * <code>lshPositions</code>
	 */
	public byte[] getHash(byte[] sequence, int[] bounds){
		
		return getHash(sequence, bounds[0], bounds[1]);
		
	}
	
	/**
	 * Computes the string resulting from applying the LSH
	 * onto <code>sequence<code>. 
	 * @param sequence the input sequence. Note that <code>sequence</code>  must be of size
	 * <code>randomizedRegionSize</code>
	 * @param lower the lower bound of the randomized region. primers will be ignored for the hash
	 * @param upper the upper bound of the randomized region. primers will be ignored for the hash
	 * @return the string resulting from concatenating the nucleotides from <code>sequence</code> at the indices as specified in 
	 * <code>lshPositions</code>
	 */
	public byte[] getHash(byte[] sequence, int lower, int upper){

		//initiate array
		byte[] result = new byte[lshDimension];
		
		//fill array
		int counter = 0;
		for (int index : lshPositions){
			result[counter] = sequence[index+lower];
			counter++;
		}
		
		return result;		
		
	}
	
	public ArrayList<LocalitySensitiveHash> getLhsInstances() {
		return lhsInstances;
	}

	public void setLhsInstances(ArrayList<LocalitySensitiveHash> lhsInstances) {
		this.lhsInstances = lhsInstances;
	}

	public Integer getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
	}

	public Integer getCoprime() {
		return coprime;
	}

	public void setCoprime(Integer coprime) {
		this.coprime = coprime;
	}
	
	public Integer getRandomizedRegionSize() {
		return randomizedRegionSize;
	}

	@Override
	public boolean equals(Object o) {
	    // self check
	    if (this == o)
	        return true;
	    
	    // null check
	    if (o == null)
	        return false;
	    
	    // type check and cast
	    if (getClass() != o.getClass())
	        return false;
	    LocalitySensitiveHash lsh = (LocalitySensitiveHash) o;
	    
	    // field comparison
	    return Objects.equals(startIndex, lsh.startIndex)
	            && Objects.equals(coprime, lsh.coprime)
	            && Objects.equals(lshDimension, lsh.lshDimension);
	}
	
	/**
	 * Creates a string representation of the hash and appends it to the log
	 */
	private void logLSH(){
		byte[] indices = new byte[this.randomizedRegionSize];
				
		byte[] sequence = new byte[this.randomizedRegionSize];
		
		for (int i=0; i < this.randomizedRegionSize; i++){ 
			sequence[i] = 'N'; 
			indices[i] = ' ';
		}
		for (int i : this.lshPositions){ indices[i] = '|'; }
		indices[startIndex] = '*';
		
		String text = String.format("Hash contains the following indices: initial position (*) at %s random coprime c (1< c < 42): %s", this.startIndex, this.coprime);
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("            ").append(new String(indices)).append( "            ").append('\n');
		sb.append("PRIMER5-----").append(new String(sequence)).append("-----PRIMER3").append('\n');
		sb.append(text);
		
		AptaLogger.log(Level.CONFIG, this.getClass(), sb.toString());
	}
	
}
