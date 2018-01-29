/**
 * 
 */
package gui.misc;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

/**
 * @author Jan Hoinka
 * Misc helper functions regarding multithreaded use
 * of JavaX
 */
public class FXConcurrent {

	/**
	 * Runs the specified {@link Runnable} on the
	 * JavaFX application thread and waits for completion.
	 *
	 * @param action the {@link Runnable} to run
	 * @throws NullPointerException if {@code action} is {@code null}
	 */
	public static void runAndWait(Runnable action) {
	    if (action == null)
	        throw new NullPointerException("action");
	 
	    // run synchronously on JavaFX thread
	    if (Platform.isFxApplicationThread()) {
	        action.run();
	        return;
	    }
	 
	    // queue on JavaFX thread and wait for completion
	    final CountDownLatch doneLatch = new CountDownLatch(1);
	    Platform.runLater(() -> {
	        try {
	            action.run();
	        } finally {
	            doneLatch.countDown();
	        }
	    });
	 
	    try {
	        doneLatch.await();
	    } catch (InterruptedException e) {
	        // ignore exception
	    }
	}
	
}
