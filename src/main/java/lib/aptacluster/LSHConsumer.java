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
import orestes.bloomfilter.BloomFilter;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 *         The consumer implementation of LSH Clustering. The consumer takes items
 *         from the queue, processes them and adds them to the structure database.
 */
public class LSHConsumer implements Runnable {
	
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
	 * Shares the number of processed items with the main thread
	 */
	private AtomicInteger processed = null;
	
	private AtomicInteger reassignment_counter = null;
	
	private BloomFilter<Integer> visited = null;
	
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
	
	
	public LSHConsumer(
			BlockingQueue<Object> queue, 
			ClusterContainer clusters, 
			Integer kmer_size, 
			Double kmer_cutoff, 
			AtomicInteger processed, 
			BloomFilter<Integer> visited,
			AtomicInteger reassignment_counter) {

		this.queue = queue;
		this.clusters = clusters;
		this.kmer_size = kmer_size;
		this.kmer_cutoff = kmer_cutoff;
		this.processed = processed; 
		this.visited = visited;
		this.reassignment_counter = reassignment_counter;

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
				LSHQueueItem item = (LSHQueueItem) queueElement;
				
				// set as processed
				processed.incrementAndGet();
				
				// iterate over the bucket and reassign cluster members if appropriate 
				while ( item.it.hasNext() ){
					
					int item_id = item.it.next();
					// we only want to recompute distances between items that are not already in the same cluster
					if (!visited.contains(item_id) && item.cluster_id!=clusters.getClusterId(item_id)){
						
						double distance = Distances.KmerDistance(
								experiment.getAptamerPool().getAptamer(item_id), 
								item.aptamer_sequence, 
								kmer_size);
						
						if (distance <= kmer_cutoff){
						
							clusters.addToCluster(item_id, item.cluster_id);
							reassignment_counter.incrementAndGet();
							visited.add(item_id);
							processed.incrementAndGet();
						}
						
					}
					
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
