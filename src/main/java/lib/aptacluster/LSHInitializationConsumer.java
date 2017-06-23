/**
 * 
 */
package lib.aptacluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;

import com.milaboratory.core.PairedEndReadsLayout;
import com.milaboratory.core.alignment.AffineGapAlignmentScoring;
import com.milaboratory.core.alignment.Aligner;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.io.sequence.PairedRead;
import com.milaboratory.core.io.sequence.SingleReadImpl;
import com.milaboratory.core.merger.MismatchOnlyPairedReadMerger;
import com.milaboratory.core.merger.PairedReadMergingResult;
import com.milaboratory.core.merger.QualityMergingAlgorithm;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;

import exceptions.InvalidConfigurationException;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.aptamer.datastructures.StructurePool;
import lib.parser.aptaplex.distances.BitapDistance;
import lib.parser.aptaplex.distances.Distance;
import lib.parser.aptaplex.distances.EditDistance;
import lib.parser.aptaplex.distances.Result;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 *         The consumer implementation of LSH Clustering. The consumer takes items
 *         from the queue, processes them and adds them to the structure database.
 */
public class LSHInitializationConsumer implements Runnable {
	
	/**
	 * The instance of StructurePool to store the data in 
	 */
	private ClusterContainer clusters = null;

	/**
	 * The experiment instance
	 */
	private Experiment experiment = Configuration.getExperiment();
	
	/**
	 * The queue to consume from
	 */
	private BlockingQueue<Object> queue = null;

	/**
	 * Currently processed item
	 */
	private Entry<Integer, MutableIntList> bucket = null;
	
	/**
	 * Unique cluster id. Writable to the consumers and
	 * thread-safe
	 */
	private AtomicInteger cluster_id = null;
	
	/**
	 * Shares the number of processed buckets with the main thread
	 */
	private AtomicInteger progress = null;
	
	/**
	 * K-mer size used for the distance computation
	 */
	private Integer kmer_size = null;
	
	/**
	 * The cutoff value in the k-mer distance space for which aptamers 
	 * which have a larger distance compared to the seed will be 
	 * excluded from a particular cluster 
	 */
	private Double kmer_cutoff = null; 
	
	/**
	 * Switch to let the consumer know when to finish
	 */
	private Boolean isRunning = true;
	
	
	public LSHInitializationConsumer(
			BlockingQueue<Object> queue, 
			AtomicInteger cluster_id,  
			ClusterContainer clusters, 
			Integer kmer_size, 
			Double kmer_cutoff, 
			AtomicInteger progress) {

		this.queue = queue;
		this.cluster_id = cluster_id;
		this.clusters = clusters;
		this.kmer_size = kmer_size;
		this.kmer_cutoff = kmer_cutoff;
		this.progress = progress;

	}

	@Override
	public void run() {

		// keep taking elements from the queue
		while (isRunning) {
			try {
				Object queueElement = queue.take();

				if (queueElement == Configuration.POISON_PILL) {
					AptaLogger.log(Level.CONFIG, this.getClass(), "Encountered poison pill. Exiting thread.");
					queue.put(Configuration.POISON_PILL); // notify other threads to stop
					
					return;
				}

				// process queueElement
				bucket = (Entry<Integer, MutableIntList>) queueElement;
				
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
				
				progress.incrementAndGet();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
