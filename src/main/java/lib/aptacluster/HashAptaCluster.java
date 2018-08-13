/**
 * 
 */
package lib.aptacluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.factory.primitive.IntLists;

import com.google.common.primitives.Ints;

import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Index;
import utilities.QSComparator;
import utilities.Quicksort;


/**
 * @author Jan Hoinka
 * Implementation of AptaCluster in Java
 */
public class HashAptaCluster implements AptaCluster {
	
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
	
	/**
	 * The LSH dimension 
	 */
	private Integer localityHashDimension = null;
	
	/**
	 * Experiment reference
	 */
	private Experiment experiment = null;
	
	/**
	 * Kmer size for distance computation
	 */
	private Integer kmer_size = null;
	
	/**
	 * Maximal number of mutations an aptamer is allowed to have
	 * w.r.t. the seed sequence to be contained in the cluster
	 */
	private Integer edit_distance = null;
	
	/**
	 * Number of iterations 
	 */
	private Integer kmer_cutoff_iterations = null; 
	
	/**
	 * The edit distance converted into the kmer distance space
	 */
	private Double kmer_cutoff = null;
	
	/**
	 * Counter for unique cluster ids, thread safe
	 */
	private AtomicInteger cluster_id = new AtomicInteger(-1);
	
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
	public HashAptaCluster(
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
		
		for (int x=0; x<this.lshIterations; x++){
			LSHIteration(x+1, lsh.get(x));
		}
		
		AdjustClusterIDs();
		
		clusters.setReadOnly();

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
		
			// Process the buckets in parallel
			AtomicInteger progress = new AtomicInteger(0);
			
			// Creating shared object
			BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(500); //TODO MAKE THIS A PARAMETER
			
			// We need to know how many threads we can use on the system
			int num_threads = Math.min( Runtime.getRuntime().availableProcessors(), Configuration.getParameters().getInt("Performance.maxNumberOfCores"));
			
			// Creating Producer and Consumer Thread
			Thread prodThread  = new Thread(new LSHInitializationProducer(buckets, sharedQueue), "LSHInitialization Producer");
			
			ArrayList<Thread> consumers = new ArrayList<Thread>();
			
			// We need at least one consumer
			consumers.add(new Thread(new LSHInitializationConsumer(sharedQueue, cluster_id, clusters, kmer_size, kmer_cutoff, progress), "LSHInitialization Consumer 1"));
			
			// Add remaining consumers
			for (int x=1; x<num_threads-1; x++){
				consumers.add(new Thread(new LSHInitializationConsumer(sharedQueue, cluster_id, clusters, kmer_size, kmer_cutoff, progress), ("LSHInitialization Consumer " + (x+1)) ));
			}

			// Start the producer and consumer threads
			for (int x=0; x<consumers.size(); x++){
				consumers.get(x).start();
			}
			prodThread.start();

			
			// Update the progress to the user
			while (prodThread.isAlive() && !prodThread.isInterrupted()) {
				try {
					
					System.out.print(String.format("Processed %s/%s buckets\r", progress.intValue(), buckets.size()));
					
					// Once every second should suffice
					Thread.sleep(1000);
					
				} catch (InterruptedException ie) {
				}
			}
			
			// Make sure the threads wait until completion
			try {
				for (int x=0; x<consumers.size(); x++){
					consumers.get(x).join();
				}
				prodThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			
			// Clear resources used by the threads
			for (int x=0; x<consumers.size(); x++){
				consumers.set(x, null);
			}
			prodThread  = null;
			
			// Final update
			System.out.println(String.format("Processed %s/%s buckets", progress.intValue(), buckets.size()));
			AptaLogger.log(Level.INFO, this.getClass(), String.format("Clustering completed, found %s clusters", cluster_id));
		}
		else{ // consecutive iterations need to be handled slightly different
			
				AtomicInteger processed = new AtomicInteger(0);
				AtomicInteger reassignment_counter= new AtomicInteger(0);
			
				// we need to know if the corresponding aptamer has already been processed in this iteration,
				// use aptamer id as index for a bitset 
				BloomFilter<Integer> visited = new FilterBuilder(aptamersBySize.length, 0.001).buildBloomFilter(); 
				
				// Creating shared object
				BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(1000); //TODO MAKE THIS A PARAMETER
				
				// We need to know how many threads we can use on the system
				int num_threads = Math.min( Runtime.getRuntime().availableProcessors(), Configuration.getParameters().getInt("Performance.maxNumberOfCores"));
				
				// Creating Producer and Consumer Thread
				Thread prodThread  = new Thread(
											new LSHProducer(
													sharedQueue,
													aptamersBySize, 
													processed,
													buckets,
													randomizedRegionSize,
													visited,
													lsh,
													clusters
													)
											, "LSH Producer");
				
				ArrayList<Thread> consumers = new ArrayList<Thread>();
				
				// We need at least one consumer
				consumers.add(new Thread(new LSHConsumer(
						sharedQueue, 
						clusters, 
						kmer_size, 
						kmer_cutoff, 
						processed, 
						visited,
						reassignment_counter)
						, "LSH Consumer 1"));
				
				// Add remaining consumers
				for (int x=1; x<num_threads-1; x++){
					consumers.add(new Thread(new LSHConsumer(
							sharedQueue, 
							clusters, 
							kmer_size, 
							kmer_cutoff, 
							processed, 
							visited,
							reassignment_counter), ("LSH Consumer " + (x+1)) ));
				}

				// Start the producer and consumer threads
				for (int x=0; x<consumers.size(); x++){
					consumers.get(x).start();
				}
				prodThread.start();

				
				// Update the progress to the user
				while (prodThread.isAlive() && !prodThread.isInterrupted()) {
					try {
						
						System.out.print(String.format("Processed %s/%s items\r", processed.intValue(), aptamersBySize.length));
						
						// Once every second should suffice
						Thread.sleep(1000);
						
					} catch (InterruptedException ie) {
					}
				}
				
				// Make sure the threads wait until completion
				try {
					for (int x=0; x<consumers.size(); x++){
						consumers.get(x).join();
					}
					prodThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Clear resources used by the threads
				for (int x=0; x<consumers.size(); x++){
					consumers.set(x, null);
				}
				prodThread  = null;
				
				//Final update
				System.out.print(String.format("Processed %s/%s items\r", processed.intValue(), aptamersBySize.length));
				System.out.println(String.format("Clustering Completed, reassigned  %s items", reassignment_counter.intValue()));
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
		
		int counter = 0;
		for ( int item_id : this.aptamersBySize){
			
			counter++;
			if(counter % 1000 == 0) {
				System.out.print(String.format("Processed %s/%s items\r", counter, this.aptamersBySize.length));	
			}
			
			
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
		System.out.println(String.format("Processed %s/%s items", counter, this.aptamersBySize.length));
		
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
	
	
	/**
	 * During the LSH iterations, cluster ids might have changed in such a way that some clusters
	 * are now missing (i.e. contain 0 members). However, AptaSuite requires clusters to be ordered
	 * in +1 increments. This function takes care of reordering them.
	 */
	private void AdjustClusterIDs() {
		
		// will contain a negative number at a particular index, indicating
		// the shift of that cluster id to make it monotonically increasing in +1 increments.
		// a 1 indicates this cluster ID did not exist before
		int[] cluster_adjustment_map = new int[cluster_id.get()+1];
		
		// Initialize everything to 1. 1 takes the place of null here
		Arrays.fill(cluster_adjustment_map, 1);
		
		// Place a 2 at the index corresponding to an existing cluster id
		clusters.iterator().forEach( entry -> {
			
			cluster_adjustment_map[entry.getValue()] = 2;
			
		});
		
		int idx1       = Index.indexOf(cluster_adjustment_map, 2); // find the first cluster id
		int idx2       = Index.indexOf(cluster_adjustment_map, 2, idx1+1); // and the next one after that
		int prev_idx   =  idx1; 
		int prev_value = -idx1; // the initial shift
		
		while (idx2 < cluster_adjustment_map.length && idx2 != -1) {

			// update previous position
			cluster_adjustment_map[prev_idx] = prev_value;

			// compute shift value for this position 
			prev_value = -(idx2 - idx1) + prev_value + 1;
			
			// move the pointers
			prev_idx = idx2;
			idx1 = idx2;
			idx2 = Index.indexOf(cluster_adjustment_map, 2, idx2+1);
			
		}
		
		// finally, we have one last update to make
		if (prev_idx != -1) cluster_adjustment_map[prev_idx] = prev_value;

		// now we can update the ids in the cluster container
		clusters.iterator().forEach( entry -> {
			
			clusters.reassignClusterId(entry.getKey(), entry.getValue() + cluster_adjustment_map[entry.getValue()]);
			
		});
		
	}

}
