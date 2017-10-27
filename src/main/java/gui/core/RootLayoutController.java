/**
 * 
 */
package gui.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;


import gui.activity.ProgressPane;
import gui.wizards.newexperiment.WizardStartController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Controlls the programatic behaviour of the elements
 * in the main window.
 *
 */
public class RootLayoutController {

    @FXML
    private Menu preferencesMenu;
    
    @FXML
    private Menu helpMenu; 
    
    @FXML
    private BorderPane rootLayout;
    
    @FXML
    private StackPane rootStackPane;
    
    /**
     * Handle to the primary stage 
     */
    private Stage primaryStage = null;
    
	/**
	 * Instance of the current experiment
	 */
	Experiment experiment = null;
    

    /**
     * Handles the logic when closing the application
     * @param event
     */
    @FXML
    private void menuFileCloseButtonAction(ActionEvent event){
    	
    	//TODO: Handle MapDB and other logics befor exiting
    	System.exit(0);
    	
    }
    
    /**
     * Calls a FileChooser to direct to the configuration file.
     * @param event
     */
    @FXML
    private void fileOpenExperimentButtonAction(ActionEvent event) {
    	
    	// Get the configuration file path
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Configuration File");
    	FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter("AptaSuite Configuration Files", "*.aptasuite");
    	fileChooser.getExtensionFilters().add(fileExtensions);
    	File cfp = fileChooser.showOpenDialog(null);
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (cfp != null) {
    		
    		// Read config file and set defaults
    		Configuration.setConfiguration(cfp.getAbsolutePath());
    		AptaLogger.log(Level.INFO, Configuration.class, "Using the following parameters: " + "\n" +  Configuration.printParameters());
    		
    		// Make sure the project folder exists and create it if not
    		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
    		if (Files.notExists(projectPath)){
    				AptaLogger.log(Level.INFO, this.getClass(), "The project path does not exist on the file system. Creating folder " + projectPath);
    				try {
    					Files.createDirectories(Paths.get(projectPath.toString()));
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    		}
    		
    		// Initialize the experiment
    		this.experiment = new Experiment(cfp.getAbsolutePath(), false);
    		this.preferencesMenu.setDisable(false);
    	}
    	
    	
    }
    
    /**
     * Calls new instance of the new experiment wizard.
     * @param event
     */
    @FXML
    private void fileNewExperimentButtonAction(ActionEvent event) {
    	
    	try {
			new Flow(WizardStartController.class).startInStage(new Stage());
		} catch (FlowException e) {
			e.printStackTrace();
		}
    	
    }
    
    @FXML
    private void testitButton(ActionEvent event) {

    	ProgressPane pp = new ProgressPane(this.rootStackPane, new Runnable() {
    		
    		@Override
    		public void run() {
    			try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		
    	});
    	
    	pp.run();
    	
    }

    public void setPrimaryStage(Stage primaryStage) {
    	
    	this.primaryStage = primaryStage;
    	
    }


    
}
