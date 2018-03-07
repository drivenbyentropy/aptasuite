/**
 * 
 */
package gui.activity;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * @author Jan Hoinka
 * Creates an overlay on a Stacking Pane so that the user
 * cannot click anywhere.
 */
public class BlockingOverlayPane {

	/**
	 * The pane onto which the progress pane should be overlayed.
	 * Note, this must be of a type which stacks new elements when
	 * add() is called.
	 */
	Pane rootPane = null;
	
	/**
	 * The Function to be executed for duration of the progress
	 * indicator
	 */
	Runnable logic = null;
	
	/**
	 * True when the panel is overlaying a pane
	 */
	Boolean overlayed = false;
	
	/**
	 * Access to the overlay pane
	 */
	StackPane pane = null;
	
	public BlockingOverlayPane(Pane rootPane, Runnable logic) {
		
		this(rootPane);
		this.logic = logic;

	}
	
	public BlockingOverlayPane(Pane rootPane) {
		
		this.rootPane = rootPane;
		
		pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: rgba(160,160,160,0.7)");
		
	}
	
	/**
	 * Sets the pane on the scene 
	 */
	public void start() {
		
		// Start only if not already called
		if (overlayed) {return;}
        
		rootPane.getChildren().add(pane);
		overlayed = true;
		
	}
	
	/**
	 * Removes the pane from the scene
	 */
	public void end() {
		
		rootPane.getChildren().remove(pane);
		
	}
	
	/**
	 * Creates the pane for the duration <code>logic</> is
	 * running.  
	 */
	public void run() {
		
		// Only do this if we have a logic to perform 
		if (logic == null) { return; }
		
		StackPane progress = createProgressPane();
		rootPane.getChildren().add(progress);
		
	}
	
    public StackPane createProgressPane() {

        Task<Void> task = new Task<Void>(){
            protected Void call() throws Exception {
                // Your process here.
                // Any changes to UI components must be inside Platform.runLater() or else it will hang.
            	logic.run();

                Platform.runLater(() -> {
                	rootPane.getChildren().remove(pane);
                	overlayed = false;
                });
                return null;
            }
        };
        new Thread(task).start();
        overlayed = true;
        
        return pane;
    }
	
}
