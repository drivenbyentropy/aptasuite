/**
 * 
 */
package lib.aptacluster;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.collections.api.list.primitive.MutableIntList;

import exceptions.InvalidSequenceReadFileException;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * This class implements the Producer of the CapR multiprocessing pattern.
 *
 */
public class LSHInitializationProducer implements Runnable{


	/**
	 * The queue to fill
	 */
	BlockingQueue<Object> queue = null;
	
	
	/**
	 * The data to be put into the queue
	 */
	Buckets items = null;
	
	/**
	 * The constructor expects an Iterable over either an aptamer pool or a selection
	 * cycle. The key contains the randomized region of the aptamer and the value its
	 * corresponding unique id.
	 * @param items
	 * @param queue
	 */
	public LSHInitializationProducer(Buckets items, BlockingQueue<Object> queue){
		
		this.queue = queue;
		this.items = items;
	}
	
	@Override
	public void run() {

		// iterate over the data and put it in the queue
		for(Entry<Integer, MutableIntList> item : items.entrySet()){
			
			try {
				queue.put(item);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// at the end we need to add a poison pill to 
		// the queue to let the consumers know when to stop
		AptaLogger.log(Level.CONFIG, this.getClass(), "Added poison pill to LSHInitialization queue");
		try {
			queue.put(Configuration.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
