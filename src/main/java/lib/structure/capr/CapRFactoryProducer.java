/**
 * 
 */
package lib.structure.capr;

import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * This class implements the Producer of the CapR multiprocessing pattern.
 *
 */
public class CapRFactoryProducer implements Runnable{


	/**
	 * The queue to fill
	 */
	BlockingQueue<Object> queue = null;
	
	
	/**
	 * The data to be put into the queue
	 */
	Iterable<Entry<byte[], Integer>> items = null;
	
	/**
	 * The constructor expects an Iterable over either an aptamer pool or a selection
	 * cycle. The key contains the randomized region of the aptamer and the value its
	 * corresponding unique id.
	 * @param items
	 * @param queue
	 */
	public CapRFactoryProducer(Iterable<Entry<byte[], Integer>> items, BlockingQueue<Object> queue){
		
		this.queue = queue;
		this.items = items;
	}
	
	/**
	 * The total number of processed items
	 */
	public int totalProcessedItems = 0;
	
	@Override
	public void run() {

		// iterate over the data and put it in the queue
		for(Entry<byte[], Integer> item : items){
			
			try {
				
				queue.put(item);
				
			} catch (InterruptedException e) {
				
				AptaLogger.log(Level.CONFIG, this.getClass(), "CapR producer encountered an InterruptedException. Breaking;");
				
				break;
				
			}
			
		}
		
		
		// at the end we need to add a poison pill to 
		// the queue to let the consumers know when to stop
		AptaLogger.log(Level.CONFIG, this.getClass(), "Added poison pill to CapR queue");
		try {
			queue.put(Configuration.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
