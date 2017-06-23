/**
 * 
 */
package lib.aptacluster;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;

import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.factory.primitive.IntLists;

import com.koloboke.collect.map.hash.HashIntIntMaps;

import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.QSComparator;
import utilities.Quicksort;

/**
 * @author Jan Hoinka
 * Implementation of AptaCluster in Java
 */
public class HashAptaClusterLINEAR implements AptaCluster {
	
	/**
	 * Stores all the LSH instances according to <code>lsh_iterations</code>
	 */
	private ArrayList<LocalitySensitiveHash> lsh = new ArrayList<LocalitySensitiveHash>(); 
	
	/**
	 * The number of LSH iterations to perform 
	 */
	private Integer lshIterations = null;
	
	/**
	 * The size of the randomized region of the aptamers. Aptamers which differ from that  
	 * size will be ignored
	 */
	private Integer randomizedRegionSize = null;
	
	private Integer localityHashDimension = null;
	
	private Experiment experiment = null;
	
	private Integer kmer_size = null;
	
	private Integer edit_distance = null;
	
	private Integer kmer_cutoff_iterations = null; 
	
	private Double kmer_cutoff = null;
	
	/**
	 * Counter for unique cluster ids, thread safe
	 */
	private AtomicInteger cluster_id = new AtomicInteger(0);
	
	private Iterable<Entry<byte[], Integer>> poolIt =  null;
	
	/**
	 * List of aptamer ids sorted (descending) by the commutative cardinality in all selection cycles
	 */
	private int[] aptamersBySize = null;
	
	
	/**
	 * The storage for the cluter intformation.
	 * Key = aptamer id
	 * Value = cluster id
	 */
	private ClusterContainer clusters = null;
	
	/**
	 * @param randomized_region_size
	 * @param locality_hash_dimension
	 * @param min_hash_overlap
	 * @param lsh_iterations
	 * @param pool_it
	 */
	public HashAptaClusterLINEAR(
			int randomized_region_size, 
			int locality_hash_dimension, 
			int lsh_iterations, 
			int edit_distance,
			int kmer_size,
			int kmer_cutoff_iterations,
			Experiment experiment){
		
		this.randomizedRegionSize = randomized_region_size;
		this.localityHashDimension = locality_hash_dimension;
		this.lshIterations = lsh_iterations;
		this.poolIt = experiment.getAptamerPool().iterator();
		this.clusters = experiment.getClusterContainer();
		this.experiment = experiment;
		this.kmer_size = kmer_size;
		this.edit_distance = edit_distance;
		this.kmer_cutoff_iterations = kmer_cutoff_iterations;
		
		for (int x=0; x<lshIterations; x++){
			lsh.add(new LocalitySensitiveHash(randomized_region_size, localityHashDimension, lsh));
		}
						
	}
	

	/* (non-Javadoc)
	 * @see lib.aptacluster.AptaCluster#performLSH(lib.aptamer.datastructures.AptamerPool, lib.aptacluster.LocalitySensitiveHash)
	 */
	@Override
	public void performLSH() {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaCluster");
		
		initializeAptamersBySize();
		
		ComputeKmerCutoff();
		
		LSHIteration(1, lsh.get(0));
		LSHIteration(2, lsh.get(1));
		
	}	
	
	
	private void LSHIteration(int iteration_number, LocalitySensitiveHash lsh){

		AptaLogger.log(Level.INFO, this.getClass(), String.format("Starting LSH Iteration %s", iteration_number));
		long tstart = System.currentTimeMillis();
		
		// Create an LSH hashing representation of the data
		Buckets buckets = generateBuckets(lsh);
		
		AptaLogger.log(Level.INFO, this.getClass(), "Assigning Clusters based on kmer distance");
		//we need to differenciate between the first iteration and all consecutive ones
		//at the end of this run every aptamer will have a cluster assignment
		if (iteration_number == 1)
		{
		
			int progress = 0;
			
			for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet()){
				
				// iterate over the list of aptamers and form clusters
				int cluster_assignment_counter = 0;
				while( cluster_assignment_counter < bucket.getValue().size()){
					
					Integer current_seed = null;
					byte[] current_seed_sequence = null;
					int current_cluster_id = 0;
					
					MutableIntIterator it = bucket.getValue().intIterator();
					
					while( it.hasNext() ){
						
						int aptamer_id = it.next();
						
						//find the seed
						if(current_seed == null && !clusters.containsAptamer(aptamer_id)){
							
							current_cluster_id = this.cluster_id.incrementAndGet();
							current_seed = aptamer_id;
							current_seed_sequence = experiment.getAptamerPool().getAptamer(current_seed);
							clusters.addToCluster(aptamer_id, current_cluster_id);
							cluster_assignment_counter++;
							continue; 
							
						}
						if (!clusters.containsAptamer(aptamer_id)){
							
							double distance = Distances.KmerDistance(
									experiment.getAptamerPool().getAptamer(aptamer_id), 
									current_seed_sequence, 
									kmer_size);
							
							if (distance <= kmer_cutoff){
							
								clusters.addToCluster(aptamer_id, current_cluster_id);
								cluster_assignment_counter++;
								
							}
						}
					}
				}
				
				progress++;
				if (progress % 100 == 0){
					System.out.print(String.format("Processed %s/%s buckets\r", progress, buckets.size()));
				}
				
			}
			// Final update
			System.out.println(String.format("Processed %s/%s buckets", progress, buckets.size()));
			AptaLogger.log(Level.INFO, this.getClass(), String.format("Clustering completed, found %s clusters", cluster_id));
		}
		else{ // consecutive iterations need to be handled slighlty different
		
			int processed = 0;
			int reassignment_counter=0;
			
			// we need to know if the corresponding aptamer has already been processed in this iteration,
			// use aptamer id as index for a bitset 
			BloomFilter<Integer> visited = new FilterBuilder(aptamersBySize.length, 0.001).buildBloomFilter(); 
			
			
			while (processed < this.aptamersBySize.length){
			
				BloomFilter<Integer> locked_buckets = new FilterBuilder(buckets.size(), 0.001).buildBloomFilter(); 
				
				for ( int aptamer_id : this.aptamersBySize){
					
					byte[] aptamer_sequence = experiment.getAptamerPool().getAptamer(aptamer_id);
					AptamerBounds aptamer_bounds = experiment.getAptamerPool().getAptamerBounds(aptamer_id);
					
	
					// skip all aptamers which do not have the appropriate size
					if ((aptamer_bounds.endIndex-aptamer_bounds.startIndex) != this.randomizedRegionSize){
						continue;
					}
					
					int current_hash = Arrays.hashCode( lsh.getHash(aptamer_sequence, aptamer_bounds) );
					
					// check if bucket is already assigned
					if(!locked_buckets.contains(current_hash) && !visited.contains(aptamer_id)){
						
						// singleton clusters will not require any processing downstream, so we can ignore them in this LSH iteration
						if(buckets.get(current_hash).size() == 1)
						{
							visited.add(aptamer_id);
							locked_buckets.add(current_hash);
							processed++;
							continue;
						}
						
						// add aptamer for processing
						locked_buckets.add(current_hash);
						
						// now process the bucket TODO: In parallel in the future
						
						// set as processed
						visited.add(aptamer_id);
						processed++;
						
						// iterate over the bucket and reassign cluster members if appropriate 
						MutableIntIterator it = buckets.get(current_hash).intIterator();
						while ( it.hasNext() ){
							
							int item_id = it.next();
							// we only want to recompute distances between items that are not already in the same cluster
							if (!visited.contains(item_id) && aptamer_id!=item_id){
								
								double distance = Distances.KmerDistance(
										experiment.getAptamerPool().getAptamer(item_id), 
										aptamer_sequence, 
										kmer_size);
								
								if (distance <= kmer_cutoff){
								
									clusters.addToCluster(item_id, clusters.getClusterId(aptamer_id));
									reassignment_counter++;
									processed++;
								}
								
							}
							
							
						}
						
					}
					if (processed % 100 == 0){
						System.out.print(String.format("Processed %s/%s items\r", processed, aptamersBySize.length));
					}
				}
			}
			//Final update
			System.out.print(String.format("Processed %s/%s items\r", processed, aptamersBySize.length));
			System.out.println(String.format("Clustering Completed, reassigned  %s items", reassignment_counter));
		}
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Finished LSH iteration %s in %s seconds", iteration_number, ((System.currentTimeMillis() - tstart) / 1000.0)));
		
	}
	
	
	/**
	 * Creates a partitioning of the data according to the locality sensitive function.
	 * The clusters are sorted in descending order by overall aptamer size.
	 * @param lsh the function to use for hashing
	 * @return the partitioned pool
	 */
	private Buckets generateBuckets(LocalitySensitiveHash lsh){
		
		AptaLogger.log(Level.INFO, this.getClass(), "Generating LSH Buckets");
		long tstart = System.currentTimeMillis();
		
		// create buckets and fill them with their corresponding hash
		Buckets buckets =  Buckets.withExpectedSize(experiment.getAptamerPool().size());
		
		
		for ( int item_id : this.aptamersBySize){
			
			byte[] item = experiment.getAptamerPool().getAptamer(item_id);
			AptamerBounds item_bounds = experiment.getAptamerPool().getAptamerBounds(item_id);
			
			// skip all aptamers which do not have the appropriate size
			if ((item_bounds.endIndex-item_bounds.startIndex) != this.randomizedRegionSize){
				continue;
			}
			
			int hash = Arrays.hashCode(lsh.getHash(item, item_bounds));
			
			if (!buckets.contains(hash)){
				buckets.justPut(hash, IntLists.mutable.of(item_id));
			}
			else {
				buckets.get(hash).add(item_id);
			}
			
		}
		
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Finished generating %s buckets in %s seconds", buckets.size(), ((System.currentTimeMillis() - tstart) / 1000.0)));
		
		return buckets;
	}
	
	/**
	 * Creates the list of aptamers sorted, in descending order, by their
	 * commulative size in all selection cycles.
	 */
	private void initializeAptamersBySize(){
		
		AptaLogger.log(Level.INFO, this.getClass(), "Initiallizing auxilliary datastructures");
		long tstart = System.currentTimeMillis();
		
		// create array of aptamer ids for all pools
		AptaLogger.log(Level.CONFIG, this.getClass(), "Generating aptamer id array");
		
		this.aptamersBySize = new int[experiment.getAptamerPool().size()];

		int counter = 0;
		for ( Integer item : this.experiment.getAptamerPool().id_iterator()){
			
			this.aptamersBySize[counter] = item;
			counter++;
			
		}
		
		Quicksort.sort(this.aptamersBySize);
		
		//create a temporary array containing the cumulative sum of aptamers in all selection cycles
		AptaLogger.log(Level.CONFIG, this.getClass(), "Generating aptamer counts id");
		int[] aptamer_sums = new int[experiment.getAptamerPool().size()];
		
		for ( SelectionCycle cycle : experiment.getAllSelectionCycles() ){
			
			counter = 0;
			for ( Entry<Integer, Integer> cycle_it : cycle.iterator()){
				
				// this is guaranteed to terminate since cycle ids are a subset 
				// of the aptmer pool ids
				while (this.aptamersBySize[counter] != cycle_it.getKey()){
					counter++;
				}
				aptamer_sums[counter] += cycle_it.getValue();
			}
			
		}
				
		// now sort the id list according to the sizes
		AptaLogger.log(Level.CONFIG, this.getClass(), "Sorting arrays");
		/**
		 * @author Jan Hoinka
		 * This comparator sorts in descending order
		 */
		class AptamerSizeQSComparator implements QSComparator{

				@Override
				public int compare(int a, int b) {
					return -Integer.compare(a, b);
				}
							
		}
		Quicksort.sort(this.aptamersBySize, aptamer_sums, new AptamerSizeQSComparator());
		
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Finished initialization in %s seconds", ((System.currentTimeMillis() - tstart) / 1000.0)));
	}
	
	/**
	 * Estimates the optimal cutoff value for two aptamers to be considered similar based on the kmer distance. This is done by randomly selecting a number
	 * of sequences and introducing "user_similarity" point mutations. The mutated species are then compared to the original and their distance is computed.
	 * The overall average distance then constitutes the cutoff.
	 * @param user_similarity maximal number of mutations the sequences are allowed to have
	 * @return optimal estimated cutoff
	 */
	private double ComputeKmerCutoff()
	{
		AptaLogger.log(Level.INFO, this.getClass(), "Computing kmer cutoff for cluster generation");
		long tstart = System.currentTimeMillis();
		
		Random rand = new Random();
		double cutoff = 0.;
		int counter = 0;
		byte[] nucleotides = {'A','C','G','T'};
		
		for (int x=0; x<this.kmer_cutoff_iterations; ++x)
		{
			//pick a sample sequence from the pool
			byte[] parent_sequence = experiment.getAptamerPool().getAptamer(this.aptamersBySize[rand.nextInt(this.aptamersBySize.length)]);
			for (int y=0; y<100; ++y)
			{
				//randomly mutate the sequence
				byte[] mutant_sequence = Arrays.copyOf(parent_sequence, parent_sequence.length);
				for (int z=0; z<edit_distance; ++z)
				{
					mutant_sequence[rand.nextInt(mutant_sequence.length)] = nucleotides[rand.nextInt(4)];
				}
				cutoff += Distances.KmerDistance(parent_sequence, mutant_sequence, kmer_size);
				counter++;
			}
		}
		
		// return the averaged cutoff
		kmer_cutoff = cutoff/counter; 
		
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Finished cutoff computation in %s seconds. Using cutoff %s", ((System.currentTimeMillis() - tstart) / 1000.0), kmer_cutoff));
		
		return cutoff/counter;
	}

}
