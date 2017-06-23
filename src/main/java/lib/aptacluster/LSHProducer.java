/**
 * 
 */
package lib.aptacluster;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;

import exceptions.InvalidSequenceReadFileException;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * This class implements the Producer of the CapR multiprocessing pattern.
 *
 */
public class LSHProducer implements Runnable{


	/**
	 * The queue to fill
	 */
	BlockingQueue<Object> queue = null;
	
	
	/**
	 * The data to be put into the queue
	 */
	Buckets buckets = null;
	
	private AtomicInteger processed = null;
	
	private int[] aptamersBySize = null;
	
	private Experiment experiment = Configuration.getExperiment();
	
	private int randomizedRegionSize = -1; 
	
	private BloomFilter<Integer> visited = null;
	
	private LocalitySensitiveHash lsh = null;
	
	private ClusterContainer clusters = null;
	
	/**
	 * The constructor expects an Iterable over either an aptamer pool or a selection
	 * cycle. The key contains the randomized region of the aptamer and the value its
	 * corresponding unique id.
	 * @param items
	 * @param queue
	 */
	public LSHProducer(
			BlockingQueue<Object> queue,
			int[] aptamers_by_size, 
			AtomicInteger processed,
			Buckets buckets,
			int randomizedRegionSize,
			BloomFilter<Integer> visited,
			LocalitySensitiveHash lsh,
			ClusterContainer clusters
			){
		
		this.queue = queue;
		this.aptamersBySize = aptamers_by_size;
		this.processed = processed;
		this.buckets = buckets;
		this.randomizedRegionSize = randomizedRegionSize; 
		this.visited = visited;
		this.lsh = lsh;
		this.clusters = clusters;
		
	}
	
	@Override
	public void run() {

		// iterate over the data and put it in the queue
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
					processed.incrementAndGet();
					continue;
				}
				
				// add aptamer for processing
				locked_buckets.add(current_hash);
				visited.add(aptamer_id);
				
				try {
					LSHQueueItem item = new LSHQueueItem();
					item.aptamer_id = aptamer_id;
					item.aptamer_sequence = aptamer_sequence;
					item.it = buckets.get(current_hash).intIterator(); 
					item.cluster_id = clusters.getClusterId(aptamer_id);
					
					queue.put(item);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// at the end we need to add a poison pill to 
		// the queue to let the consumers know when to stop
		AptaLogger.log(Level.CONFIG, this.getClass(), "Added poison pill to LSH queue");
		try {
			queue.put(Configuration.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
