/**
 * 
 */
package lib.structure.rnafold;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import utilities.Configuration;

/**
 * @author Jan Hoinka 
 * Java implementation of RNAFold. This class controls the producer-consumer pattern 
 * for parallel structure prediction.
 */
public class RNAFoldFactory implements Runnable{

	/**
	 * The progress of the parser instance. Writable to the consumers and thread-safe
	 */
	private static final AtomicInteger progress = new AtomicInteger();
	
	private Iterable<Entry<byte[], Integer>> items = null;
	
	public RNAFoldFactory(Iterable<Entry<byte[], Integer>> items){
		
		this.items = items;
		
	}
	
	public void predict() {

		// Creating shared object
		BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(500); //TODO: MAKE THIS A PARAMETER

		// We need to know how many threads we can use on the system
		int num_threads = Math.min( Runtime.getRuntime().availableProcessors(), Configuration.getParameters().getInt("Performance.maxNumberOfCores"));
		
		// Creating Producer and Consumer Thread
		Thread prodThread  = new Thread(new RNAFoldFactoryProducer(items, sharedQueue), "RNAFold Producer");
		
		ArrayList<Thread> consumers = new ArrayList<Thread>();
		
		// We need at least one consumer
		consumers.add(new Thread(new RNAFoldFactoryConsumer(sharedQueue, progress), "RNAFold Consumer 1"));
		
		// Add remaining consumers
		for (int x=1; x<num_threads-1; x++){
			consumers.add(new Thread(new RNAFoldFactoryConsumer(sharedQueue, progress), ("RNAFold Consumer " + (x+1)) ));
		}

		// Start the producer and consumer threads
		for (int x=0; x<consumers.size(); x++){
			consumers.get(x).start();
		}
		prodThread.start();

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
		
	}


	public AtomicInteger getProgress() {
		return progress;
	}

	@Override
	public void run() {
		
		predict();
		
	}

}
