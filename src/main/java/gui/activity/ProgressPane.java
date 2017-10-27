/**
 * 
 */
package gui.activity;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * @author Jan Hoinka
 * Implements a custom progress pane so that meant to 
 * be used by any tasks which require some time to 
 * complete locks the GUI in the meanwhile.
 */
public class ProgressPane {

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
	Runnable logic;
	
	public ProgressPane(Pane rootPane, Runnable logic) {
		
		this.rootPane = rootPane;
		this.logic = logic;
		
	}
	
	public void run() {
		
    	System.out.println("Here");
		StackPane progress = createProgressPane();
		rootPane.getChildren().add(progress);
    	System.out.println("Here2");
		
	}
	
    public StackPane createProgressPane() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxHeight(50);
        indicator.setMaxWidth(50);

        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: rgba(160,160,160,0.7)");
        pane.getChildren().add(indicator);

        Task<Void> task = new Task<Void>(){
            protected Void call() throws Exception {
                // Your process here.
                // Any changes to UI components must be inside Platform.runLater() or else it will hang.
            	logic.run();

                Platform.runLater(() -> {
                	rootPane.getChildren().remove(pane);
                });
                return null;
            }
        };
        new Thread(task).start();
        return pane;
    }
	
}
