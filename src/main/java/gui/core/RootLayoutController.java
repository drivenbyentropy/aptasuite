/**
 * 
 */
package gui.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import gui.activity.ProgressPaneController;
import gui.wizards.newexperiment.WizardStartController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Controls the programmatic behavior of the elements
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
    
    @FXML
    private TabPane rootTabPane;
    
    @FXML
    private MenuItem showOverviewTabMenuItem;    
    
    @FXML
    private MenuItem showSequencingDataTabMenuItem;
    
    @FXML
    private MenuItem showAptamerPoolTabMenuItem;
    
    @FXMLApplicationContext
    private ApplicationContext context;
    
    /**
     * Contains all currently opened tabs
     */
    private ObservableList<Tab> tabs = FXCollections.observableArrayList( new ArrayList<Tab>() );
    
    /**
     * Handle to the overview tab 
     */
    private Tab overviewTab;
    
    /**
     * Handle to the overview tab 
     */
    private Tab sequencingDataTab;
    
    /**
     * Handle to the aptamer pool 
     */
    private Tab aptamerPoolTab;
    
    /**
     * Handle to the primary stage 
     */
    private Stage primaryStage = null;
    
	/**
	 * Instance of the current experiment
	 */
	Experiment experiment = null;
    
	
	@PostConstruct
	public void initialize() {
		
		
		// Bind menu button availability
		showOverviewTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( overviewTab ), tabs ) );
		showSequencingDataTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( sequencingDataTab ), tabs ) );
		showAptamerPoolTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( aptamerPoolTab ), tabs ) );
		
		testAction();
		
//		BEGIN TEMP AUTO LOAD DATASET
//		File cfp = Paths.get("C:\\Users\\hoinkaj\\Downloads\\TestMultiplexedPaired\\configuration.aptasuite").toFile();
//		
//		// Read config file and set defaults
//		Configuration.setConfiguration(cfp.getAbsolutePath());
//		
//		ProgressPaneController pp = ProgressPaneController.getProgressPane(this.rootStackPane, new Runnable() {
//    			
//			@Override
//			public void run() {
//				
//				//AptaLogger.log(Level.INFO, Configuration.class, "Using the following parameters: " + "\n" +  Configuration.printParameters());
//				
//				// Make sure the project folder exists and create it if not
//				Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
//				if (Files.notExists(projectPath)){
//						AptaLogger.log(Level.INFO, this.getClass(), "The project path does not exist on the file system. Creating folder " + projectPath);
//						try {
//							Files.createDirectories(Paths.get(projectPath.toString()));
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//				}
//				
//				// Initialize the experiment
//				experiment = new Experiment(cfp.getAbsolutePath(), false);
//				preferencesMenu.setDisable(false);
//				
//				// Initialize the GUI elements
//				Platform.runLater(() -> {
//					
//					AptaLogger.log(Level.INFO, this.getClass(), "Initializing GUI elements");
//					showInitialTabs();
//					
//                });
//				
//			}
//		
//		});
//		
//		pp.run();
		// END TEMP AUTOLOAD DATASET
		
	}
	
	
	/**
	 * Opens a series of tabs depending on the data
	 * Will return immediately
	 */
	public void showInitialTabs() {
		
		// Reset in case a previous experiment was open
		this.overviewTab = null;
		this.sequencingDataTab = null;
		this.tabs.clear();
		this.rootTabPane.getTabs().clear();
		
		Platform.runLater(() -> {
		
			// only add them if they are not already open
			if (!this.tabs.contains(overviewTab)) {
				addExperimentOverviewTab();
			}
			
			if (!this.tabs.contains(sequencingDataTab)) {
				addSequencingDataTab();
			}
			
			if (!this.tabs.contains(aptamerPoolTab)) {
				addAptamerPoolTab();
			}
		
		});
		
	}

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
    		
    		ProgressPaneController pp = ProgressPaneController.getProgressPane(this.rootStackPane, new Runnable() {
	    			
				@Override
				public void run() {
					
					//AptaLogger.log(Level.INFO, Configuration.class, "Using the following parameters: " + "\n" +  Configuration.printParameters());
					
					// Make sure the project folder exists and create it if not
					Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
					if (Files.notExists(projectPath)){
						
							AptaLogger.log(Level.SEVERE, this.getClass(), "The configuration file contains invalid entries. The project path does not exist.");

							Platform.runLater(() -> {
								Alert alert = new Alert(AlertType.INFORMATION);
								alert.setTitle("Error Opening Experiment");
								alert.setHeaderText("An error occured while opening the experiment.");
								alert.setContentText("The project path specified in the configuration file does not exists. Please make sure your configuration file is valid and try again.");
								alert.showAndWait();
							});
							
							
							return;
					}
					
					// Initialize the experiment
					experiment = new Experiment(cfp.getAbsolutePath(), false);
					preferencesMenu.setDisable(false);
					
					// Initialize the GUI elements
					Platform.runLater(() -> {
						
						AptaLogger.log(Level.INFO, this.getClass(), "Initializing GUI elements");
						showInitialTabs();
						
	                });
					
				}
			
    		});
    		
    		pp.run();
    		
    	}
    	
    	
    }
    
    /**
     * Calls new instance of the new experiment wizard.
     * @param event
     */
    @FXML
    private void fileNewExperimentButtonAction(ActionEvent event) {
    	
    	// Create a context with the main class as content so that we can inistialize the 
    	// main GUI after parsing is completed
    	context = ApplicationContext.getInstance();
    	
    	// Run the wizard
    	Flow wizard = null;
    	try {
    		
			wizard = new Flow(WizardStartController.class);
			context.register("RootLayoutController", this);
			
			wizard.startInStage(new Stage());
			
		} catch (FlowException e) {
			e.printStackTrace();
		}
    	
    }
    
    
    /**
     * Once data has been loaded by either creating a new experiment
     * or opening and existing one, this function takes care of 
     * adding the experiment overview tab to the main view
     */
    @FXML
    private void addExperimentOverviewTab() {
    	
    	// Only create a new instance if this tab has not been opened before
    	if ( overviewTab == null) {
    	
	    	// create a new tab 
	    	overviewTab = new Tab("Experiment Overview");
	    	
	    	
	    	// set properties
	    	overviewTab.setClosable(true);
    	    // remove the instance from the tabs list
	    	overviewTab.setOnCloseRequest(e -> { tabs.remove(overviewTab); });
	    	
	    	
	    	
	    	// load the content 
	    	try {
	    		overviewTab.setContent(FXMLLoader.load(getClass().getResource("/gui/core/experiment/overview/ExperimentOverviewRoot.fxml")));
			} catch (IOException e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
				e.printStackTrace();
			}
	    	
    	}
    	
    	// and add it to the tabs of the pane
    	this.rootTabPane.getTabs().add(0, overviewTab);
    	this.tabs.add(overviewTab);
    	
    	
    }
    
    
    /**
     * Once data has been loaded by either creating a new experiment
     * or opening and existing one, this function takes care of 
     * adding the Sequencing data tab to the main view
     */
    @FXML
    private void addSequencingDataTab() {
    	
    	// Only create a new instance if this tab has not been opened before
    	if ( sequencingDataTab == null) {
    	
	    	// create a new tab 
    		sequencingDataTab = new Tab("Sequencing Data");
	    	
	    	// set properties
    		sequencingDataTab.setClosable(true);
    		
    	    // remove the instance from the tabs list
    		sequencingDataTab.setOnCloseRequest(e -> { tabs.remove(sequencingDataTab); });
	    	
	    	
	    	
	    	// load the content 
	    	try {
	    		sequencingDataTab.setContent(FXMLLoader.load(getClass().getResource("/gui/core/sequencing/data/SequencingDataRoot.fxml")));
			} catch (IOException e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
				e.printStackTrace();
			}
	    	
    	}
    	
    	// and add it to the tabs of the pane
    	this.rootTabPane.getTabs().add(1, sequencingDataTab);
    	this.tabs.add(sequencingDataTab);
    	   	
    }
    
    /**
     * Once data has been loaded by either creating a new experiment
     * or opening and existing one, this function takes care of 
     * adding the aptamer pool tab to the main view
     */
    @FXML
    private void addAptamerPoolTab() {
    	
    	// Only create a new instance if this tab has not been opened before
    	if ( aptamerPoolTab == null) {
    	
	    	// create a new tab 
    		aptamerPoolTab = new Tab("Aptamer Pool");
	    	
	    	// set properties
    		aptamerPoolTab.setClosable(true);
    		
    	    // remove the instance from the tabs list
    		aptamerPoolTab.setOnCloseRequest(e -> { tabs.remove(aptamerPoolTab); });
	    	
	    	
	    	
	    	// load the content 
	    	try {
	    		aptamerPoolTab.setContent(FXMLLoader.load(getClass().getResource("/gui/core/aptamer/pool/AptamerPoolRoot.fxml")));
			} catch (IOException e) {
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
				e.printStackTrace();
			}
	    	
    	}
    	
    	// and add it to the tabs of the pane
    	this.rootTabPane.getTabs().add(2, aptamerPoolTab);
    	this.tabs.add(aptamerPoolTab);
    	   	
    }
    

    public void setPrimaryStage(Stage primaryStage) {
    	
    	this.primaryStage = primaryStage;
    	
    }

    @FXML
    private void testAction() {
    	
    	Stage stage = new Stage();
        
    	stage.setMinWidth(5);
    	stage.setMinHeight(5);
    	
    	Parent root = null;
		try {
			root = FXMLLoader.load(getClass().getResource("/gui/charts/logo/LogoChartPanel.fxml"));
			
			root.minHeight(5);
			root.minWidth(5);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	//Fill stage with content
    	stage.setScene(new Scene(root));
    	
    	
        stage.show();
    	
    }
    
}
