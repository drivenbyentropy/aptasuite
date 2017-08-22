/**
 * 
 */
package lib.structure.rnafold;

import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import lib.aptamer.datastructures.StructurePool;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 *         The consumer implementation of RNAFoldFactory. The consumer takes items
 *         from the queue, processes them and adds them to the structure database.
 */
public class RNAFoldFactoryConsumer implements Runnable {
	
	/**
	 * The instance of StructurePool to store the data in 
	 */
	private StructurePool pool = Configuration.getExperiment().getBppmPool();

	/**
	 * The queue to consume from
	 */
	private BlockingQueue<Object> queue = null;

	/**
	 * Currently processed item
	 */
	private Entry<byte[], Integer> item = null;
	
	/**
	 * The progress of the CapR instances. Writable to the consumers and
	 * thread-safe
	 */
	private AtomicInteger progress = null;

	/**
	 * Switch to let the consumer know when to finish
	 */
	private Boolean isRunning = true;
	
	/**
	 * The local RNAFold instance
	 */
	private RNAFoldAPI rfa = new RNAFoldAPI();

	
	public RNAFoldFactoryConsumer(BlockingQueue<Object> queue, AtomicInteger progress) {

		this.queue = queue;
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
					
					// Notify GC
					rfa = null;
					
					return;
				}

				// Update the progress in a thread-safe manner
				progress.incrementAndGet();

				// process queueElement
				item = (Entry<byte[], Integer>) queueElement;

				double[] bppm = rfa.getBppm(item.getKey());
				
				// add item to storage
				pool.registerStructure(item.getValue(), bppm);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
