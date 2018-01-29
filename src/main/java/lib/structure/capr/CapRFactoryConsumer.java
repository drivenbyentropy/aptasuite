/**
 * 
 */
package lib.structure.capr;

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
 *         The consumer implementation of CapRFactory. The consumer takes items
 *         from the queue, processes them and adds them to the structure database.
 */
public class CapRFactoryConsumer implements Runnable {
	
	/**
	 * The instance of StructurePool to store the data in 
	 */
	private StructurePool pool = Configuration.getExperiment().getStructurePool();

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
	 * The local capr instance
	 */
	private CapR capr = new CapR();

	
	public CapRFactoryConsumer(BlockingQueue<Object> queue, AtomicInteger progress) {

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
					capr = null;
					
					return;
				}

				// Update the progress in a thread-safe manner
				progress.incrementAndGet();

				// process queueElement
				item = (Entry<byte[], Integer>) queueElement;

				capr.ComputeStructuralProfile(item.getKey(), item.getKey().length);
				
				// add item to storage
				pool.registerStructure(item.getValue(), capr.getStructuralProfile());
				
			} catch (InterruptedException e) { // stop computation on interrupt signal
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "CapR consumer thread " + Thread.currentThread().getName() +  " encountered an InterruptedException. Breaking;");
				
				queue.clear();
				
				return;
			}
			
		}

	}

}
