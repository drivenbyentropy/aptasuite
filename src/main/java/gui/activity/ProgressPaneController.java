package gui.activity;

import java.io.IOException;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
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
	
	/**
	 * The pane onto which the progress pane should be overlayed.
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
	
	/**
	 * Logger if required
	 */
	private ProgressPaneLoggerHandler loghandler = null;
	
	/**
	 * Factory which instantiates a new progressPane and returns the corresponding 
	 * controller which allows to interact with it
	 * @return
	 */
	public static ProgressPaneController getProgressPane(Pane rootPane, Runnable logic) {
		
    	// load the content 
		ProgressPaneController progressPaneController = null;
		
		try {
			
			FXMLLoader loader = new FXMLLoader(ProgressPaneController.class.getClassLoader().getResource("gui/activity/ProgressPane.fxml"));
			loader.load();
			progressPaneController = loader.getController();
			progressPaneController.setLogic(logic);
			progressPaneController.setRootPane(rootPane);
			progressPaneController.setTask(logic);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return progressPaneController;
		
	}
	
	@PostConstruct
	public void initialize() {
		
		
		
		System.out.println("ProgressPaneInitialized");
		
	}

	/**
	 * Starts the task and shows the pane on the parent node.
	 */
	public void run() {
		
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
		
		// Do the work
		new Thread(task).start();
		rootPane.getChildren().add(progressAnchorPane);
		
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
		
		task = new Task<Void>(){
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                	
                	// Move the old messages down
            		String cache = progressPaneLabel2.getText();
            		progressPaneLabel2.setText(progressPaneLabel1.getText());
            		progressPaneLabel3.setText(cache);
            		progressPaneLabel1.setText(message);
            		
                });
                return null;
            }
        };
        
        new Thread(task).start();
		
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
	 * @param rootPane the rootPane to set
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
	
}
