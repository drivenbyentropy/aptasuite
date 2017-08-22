/**
 * 
 */
package lib.structure.rnafold;

import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * This class implements the Producer of the RNAFold multiprocessing pattern.
 *
 */
public class RNAFoldFactoryProducer implements Runnable{


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
	public RNAFoldFactoryProducer(Iterable<Entry<byte[], Integer>> items, BlockingQueue<Object> queue){
		
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
			//TODO: check if item has already been prediced and skip if required
			
			try {
				queue.put(item);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// at the end we need to add a poison pill to 
		// the queue to let the consumers know when to stop
		AptaLogger.log(Level.CONFIG, this.getClass(), "Added poison pill to RNAFold queue");
		try {
			queue.put(Configuration.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
