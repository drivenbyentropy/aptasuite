package gui.activity;

import java.io.IOException;
import java.util.logging.Level;

import gui.misc.FXConcurrent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import utilities.AptaLogger;

public class ProgressPaneController {

	
	@FXML
	private AnchorPane progressAnchorPane;
	
	@FXML
	private Label progressPaneLabel1;
	
	@FXML
	private Label progressPaneLabel2;
	
	@FXML
	private Label progressPaneLabel3;
	
	@FXML
	private ProgressIndicator progressIndicator;
	
	@FXML
	private ProgressBar progressBar;
	
	@FXML
	private Label progressBarLabel;
	
	@FXML
	private VBox progressVBox;
	
	/**
	 * The panes onto which the progress pane should be overlayed.
	 * Note, this must be of a type which stacks new elements when
	 * add() is called.
	 */
	private Pane rootPane = null;
	
	/**
	 * The Function to be executed for duration of the progress
	 * indicator
	 */
	private Runnable logic;
	
	/**
	 * Task in which the logic is running in
	 */
	private Task<Void> task;
	
	/**
	 * Flag to indicate if the logging messages should be shown on screen or not
	 */
	private boolean showLogs = true;
	
	private boolean showBar = false;
	
	/**
	 * Logger if required
	 */
	private ProgressPaneLoggerHandler loghandler = null;
	
	/**
	 * In case more than one loading screen is required for the same task, they are stored here
	 */
	private ProgressPaneController[] auxiliaryProgressPaneControllers;
	
	/**
	 * The thread in which the task will run in 
	 */
	private Thread work = null;
	
	/**
	 * Factory which instantiates a new progressPane and returns the corresponding 
	 * controller which allows to interact with it
	 * @return
	 */
	public static ProgressPaneController getProgressPane(Runnable logic, Pane rootPane, ProgressPaneController... ppc) {
		
    	// load the content 
		ProgressPaneController progressPaneController = null;
		
		try {
			
			FXMLLoader loader = new FXMLLoader(ProgressPaneController.class.getClassLoader().getResource("gui/activity/ProgressPane.fxml"));
			loader.load();
			progressPaneController = loader.getController();
			progressPaneController.setRootPane(rootPane);
			progressPaneController.setAuxiliaryProgressPaneControllers(ppc);
			progressPaneController.setLogic(logic);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return progressPaneController;
		
	}

	/**
	 * Starts the task and shows the pane on the parent node.
	 */
	public void run() {
		
		FXConcurrent.runAndWait( () -> {
			// Register log handler if requried
			if (showLogs) {
				
				// Clear and Show Labels
				progressPaneLabel1.setText("");
				progressPaneLabel2.setText("");
				progressPaneLabel3.setText("");
				progressPaneLabel1.setVisible(true);
				progressPaneLabel2.setVisible(true);
				progressPaneLabel3.setVisible(true);
					
				
				loghandler =  new ProgressPaneLoggerHandler(this);
				loghandler.setLevel(Level.ALL);
				
				AptaLogger.getLogger().addHandler(loghandler);
				
			}
			else { // Otherwise we permanently remove them from the VBox
					this.progressVBox.getChildren().remove(this.progressPaneLabel1);
					this.progressVBox.getChildren().remove(this.progressPaneLabel2);
					this.progressVBox.getChildren().remove(this.progressPaneLabel3);
			}
			
			// The same is true for the progress bar and label
			if (! showBar ) {
					this.progressVBox.getChildren().remove(this.progressBar);
					this.progressVBox.getChildren().remove(this.progressBarLabel);
			}
			
			// Set progress icons
			if (this.logic != null) {
				rootPane.getChildren().add(progressAnchorPane);
			}
			
			if (this.auxiliaryProgressPaneControllers != null) {
				
				for (ProgressPaneController ppc : this.auxiliaryProgressPaneControllers) {
					
					ppc.getRootPane().getChildren().add(ppc.getProgressAnchorPane());
					
				}
				
			}
			
		});
		
		// Do the work
		work = new Thread(task);
		work.start();
		
	}
	
	/**
	 * Lets the task to run while the progress indicator is showing
	 * @param logic
	 */
	public void setTask(Runnable logic) {
		
        task = new Task<Void>(){
            protected Void call() throws Exception {
                // Your process here.
                // Any changes to UI components must be inside Platform.runLater() or else it will hang.
            	logic.run();

            	// Cleanup to perform after task is done
            	Platform.runLater(() -> {
            		rootPane.getChildren().remove(progressAnchorPane);
                	
            		if (auxiliaryProgressPaneControllers != null) {
        				
        				for (ProgressPaneController ppc : auxiliaryProgressPaneControllers) {
        					
        					ppc.getRootPane().getChildren().remove(ppc.getProgressAnchorPane());
        					
        				}
        				
        			}
            		
                	// Remove the handler
            		if (showLogs) {
            			
            			AptaLogger.getLogger().removeHandler(loghandler);
            			
            			progressPaneLabel1.setVisible(false);
            			progressPaneLabel2.setVisible(false);
            			progressPaneLabel3.setVisible(false);
            			
            		}
                	
                });
                return null;
            }
        };
        
	}
	
	/**
	 * Adds a new message to the logging labels, shifting old ones down
	 * @param message
	 */
	public void addLogMessage(String message) {
		
		if (this.showLogs) {
		
	        Platform.runLater(() -> {
	        	
	        	// Move the old messages down
	    		String cache = progressPaneLabel2.getText();
	    		progressPaneLabel2.setText(progressPaneLabel1.getText());
	    		progressPaneLabel3.setText(cache);
	    		progressPaneLabel1.setText(message);
	        	
	        });
	        
		}
	}
	
	/**
	 * Adds a new message to the logging labels, without shifting down.
	 * @param message
	 */
	public void refreshLogMessage(String message) {
		
		if (this.showLogs) {
		
	        Platform.runLater(() -> {
	        	
	    		progressPaneLabel1.setText(message);
	        	
	        });
	        
		}
	}
	
	
	/**
	 * @return the progressAnchorPane
	 */
	public AnchorPane getProgressAnchorPane() {
		return progressAnchorPane;
	}

	
	/**
	 * @return the rootPane
	 */
	public Pane getRootPane() {
		return rootPane;
	}

	/**
	 * @param rootPane2 the rootPane to set
	 */
	public void setRootPane(Pane rootPane) {
		this.rootPane = rootPane;
	}

	/**
	 * @return the logic
	 */
	public Runnable getLogic() {
		return logic;
	}

	/**
	 * @param logic the logic to set
	 */
	public void setLogic(Runnable logic) {
		this.logic = logic;
		setTask(this.logic);
	}

	/**
	 * @return the showLogs
	 */
	public boolean isShowLogs() {
		return showLogs;
	}

	/**
	 * @param showLogs the showLogs to set
	 */
	public void setShowLogs(boolean showLogs) {
		this.showLogs = showLogs;
	}

	/**
	 * @return the auxiliaryProgressPaneControllers
	 */
	public ProgressPaneController[] getAuxiliaryProgressPaneControllers() {
		return auxiliaryProgressPaneControllers;
	}

	/**
	 * @param auxiliaryProgressPaneControllers the auxiliaryProgressPaneControllers to set
	 */
	public void setAuxiliaryProgressPaneControllers(ProgressPaneController[] auxiliaryProgressPaneControllers) {
		this.auxiliaryProgressPaneControllers = auxiliaryProgressPaneControllers;
	}
	
	/**
	 * Sets the corresponding loading icon 
	 * @param set if true, a progress bar will be displayed which can be controlled 
	 * by <code>setProgress</code>.
	 * If false, the ProgressIndicator is shown
	 */
	public void setShowProgressBar(boolean set) {
		
		Platform.runLater(()-> {
			
			this.progressBar.setVisible(set);
			this.progressBarLabel.setVisible(set);

		});
	
		showBar = set;
		
	}
	
	/**
	 * Sets the current progress for the progress bar
	 * @param progress value between 0 and 1 
	 */
	public void setProgress(double progress) {
		
		if (this.showBar) {
		
			Platform.runLater(()-> {
				this.progressBar.setProgress(Math.min(1.0, progress));
			});
		
		}
		
	}
	
	/**
	 * Sets the current progress for the progress bar
	 * @param progress value between 0 and 1 
	 */
	public void setProgressLabel(String text) {
		
		if (this.showBar) {
		
			Platform.runLater(()-> {
				this.progressBarLabel.setText(text);
			});
			
		}
	}
	
	/**
	 * Returns the Thread in which the task is running in. 
	 * @return Thread or null if run() has not been started yet
	 */
	public Thread getTaskThread() {
		
		return this.work;
		
	}
	
}
