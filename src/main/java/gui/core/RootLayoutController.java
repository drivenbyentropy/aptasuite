/**
 * 
 */
package gui.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import gui.activity.ProgressPaneController;
import gui.core.aptamer.family.analysis.AptamerFamilyAnalysisRootController;
import gui.core.aptamer.pool.AptamerPoolRootController;
import gui.core.motifs.MotifAnalysisRootController;
import gui.core.sequencing.data.SequencingDataRootController;
import gui.wizards.aptasim.AptaSimWizardController;
import gui.wizards.export.ExportWizardController;
import gui.wizards.newexperiment.DataModel;
import gui.wizards.newexperiment.WizardStartController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    
    @FXML
    private MenuItem showAptamerFamilyAnalysisTabMenuItem;
    
    @FXML
    private MenuItem showMotifAnalysisTabMenuItem;
    
    @FXML
    private MenuItem exportDataMenuItem;
    
    @FXML
    private VBox noExperimentVBox;
    
    @FXML
    private Menu numberOfCoresMenu;
    
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
     * Handle to the aptamer pool 
     */
    private Tab aptamerFamilyAnalysisTab;
    
    /**
     * Handle to the motif tab
     */
    private Tab motifAnalysisTab;

    /**
     * Handle to dynamically initialize the tabs
     */
    private HashMap<Tab,Initializable> tabInitializationMap = new HashMap<Tab,Initializable>(); 
    
    /**
     * Handle to the primary stage 
     */
    private Stage primaryStage = null;
    
	/**
	 * Instance of the current experiment
	 */
    private SimpleObjectProperty<Experiment> experiment = new SimpleObjectProperty<>(null);

    
	@PostConstruct
	public void initialize() {
		
		// Bind menu button availability
		showOverviewTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( overviewTab ), tabs ).or(experiment.isNull()) );
		showSequencingDataTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( sequencingDataTab ), tabs ).or(experiment.isNull()) );
		showAptamerPoolTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( aptamerPoolTab ), tabs ).or(experiment.isNull()) );
		showAptamerFamilyAnalysisTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( aptamerFamilyAnalysisTab ), tabs ).or(experiment.isNull()) );
		showMotifAnalysisTabMenuItem.disableProperty().bind( Bindings.createBooleanBinding( () -> tabs.contains( motifAnalysisTab ), tabs ).or(experiment.isNull()) );
		exportDataMenuItem.disableProperty().bind( experiment.isNull() );
		
		// Listen to tab clicks an dynamically initialize them 
		rootTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() { 
		    @Override 
		    public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
		        
		    	// check if it has been initialized and if not, do it
		    	try {
		    	if(!tabInitializationMap.get(newTab).isInitialized()) {
		    		
		    		tabInitializationMap.get(newTab).initializeContent();
		        }
		    	}
		    	catch(NullPointerException e) {
		    		
		    		
		    	}
		    	
		    }
		});
		
		
//////		BEGIN TEMP AUTO LOAD DATASET
//		File cfp = Paths.get("C:/Users/hoinkaj/temp/AptaSim/configuration.aptasuite").toFile();
//		
//		// Read config file and set defaults
//		Configuration.setConfiguration(cfp.getAbsolutePath());
//		
//		ProgressPaneController pp = ProgressPaneController.getProgressPane(new Runnable() {
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
//				experiment.set(new Experiment(cfp.getAbsolutePath(), false));
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
//		}, this.rootStackPane);
//		
//		pp.run();
////		// END TEMP AUTOLOAD DATASET
		
	}
	
	private void setConfigurationDependentGUIItems() {
		
	    
		// Set the number of cores in the menu
		numberOfCoresMenu.getItems().clear();
		
		// Default
		int default_cores = Math.min(Configuration.getParameters().getInt("Performance.maxNumberOfCores"), Runtime.getRuntime().availableProcessors());
	
		// If we have more than one core, we leave one core for other operations
		if (default_cores > 2) default_cores--;
		
		for (int x=1; x<=Runtime.getRuntime().availableProcessors(); x++) {
			
			CheckMenuItem core = new CheckMenuItem(""+x);
			core.selectedProperty().addListener(new ChangeListener<Boolean>()
			{
				//We need to save this change to file and config
				
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
				{
					if (core.isSelected()) {
						
						Configuration.getParameters().setProperty("Performance.maxNumberOfCores", core.getText());
						Configuration.writeConfiguration();
						
						//Deselect the other
						for (MenuItem item : numberOfCoresMenu.getItems()) {
							
							if (item != core) {
								
								((CheckMenuItem) item).setSelected(false);
								
							}
							
						}
						
					}
				}
			});
			
			if (x==default_cores) {
				core.setSelected(true);
			}
			
			this.numberOfCoresMenu.getItems().add(core);	
		}
		
		preferencesMenu.setDisable(false);
		
	}
	
	/**
	 * Opens a series of tabs depending on the data
	 * Will return immediately
	 */
	public void showInitialTabs() {
		
		// By now we have a configuraiton and can set the Settings menu and others
		setConfigurationDependentGUIItems();
		
		
		// Remove cover layer
		noExperimentVBox.setVisible(false);
		
		// Reset in case a previous experiment was open
		this.overviewTab = null;
		this.sequencingDataTab = null;
		this.aptamerPoolTab = null;
		this.aptamerFamilyAnalysisTab = null;
		this.motifAnalysisTab = null;
		
	    this.tabInitializationMap.clear();
	    
		
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
			
			if (!this.tabs.contains(aptamerFamilyAnalysisTab)) {
				addAptamerFamilyAnalysisTab();
			}
			
			if (!this.tabs.contains(motifAnalysisTab)) {
				addMotifAnalysisTab();
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
    	
    	// Close any currently open experiments prior to opening a new one
    	if (experiment.get() != null) {
    		
    		Alert alert = new Alert(AlertType.CONFIRMATION);
    		alert.setTitle("Close current experiment?");
    		alert.setHeaderText("Creating a new experiment will close the current view.");
    		alert.setContentText("Press OK to continue or Cancel to keep working with the current experiment.");

    		Optional<ButtonType> result = alert.showAndWait();
    		if (result.get() == ButtonType.CANCEL){
    		    return;
    		} 
    		
        	// Reset app
        	this.reset();    		
    	}


    	
    	
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
    		
    		ProgressPaneController pp = ProgressPaneController.getProgressPane(new Runnable() {
	    			
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
					experiment.set(new Experiment(cfp.getAbsolutePath(), false));
					preferencesMenu.setDisable(false);
					
					// Initialize the GUI elements
					Platform.runLater(() -> {
						
						AptaLogger.log(Level.INFO, this.getClass(), "Initializing GUI elements");
						showInitialTabs();
						
	                });
					
				}
			
    		}, this.rootStackPane);
    		
    		pp.run();
    		
    	}
    	
    	
    }
    
    /**
     * Calls new instance of the new experiment wizard.
     * @param event
     */
    @FXML
    private void fileNewExperimentButtonAction(ActionEvent event) {
    	
    	// Close any currently open experiments prior to opening a new one
    	if (experiment.get() != null) {
    		
    		Alert alert = new Alert(AlertType.CONFIRMATION);
    		alert.setTitle("Close current experiment?");
    		alert.setHeaderText("Creating a new experiment will close the current view.");
    		alert.setContentText("Press OK to continue or Cancel to keep working with the current experiment.");

    		Optional<ButtonType> result = alert.showAndWait();
    		if (result.get() == ButtonType.CANCEL){
    		    return;
    		} 

        	// Reset app
        	this.reset();
    		
    	}

   	
    	// Run the wizard
    	Parent root;
        try {
        																				
        	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/wizardStart.fxml"));
        	
            root = loader.load();
            WizardStartController controller = (WizardStartController) loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("Create a New Experiment");
            stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("logo.png")));           
            stage.setScene(new Scene(root, Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE));
    		
    		// The datamodel to be passed from scene to scene
    		controller.setDataModel(new DataModel());
    		// We need this to initialize the controller when the window gets closed
            controller.setRootLayoutController(this);
            // And this to change between different Scenes
            controller.setStage(stage);
            
            // Initialize the controller
            controller.init();
            
            // And show the wizard
            stage.show();
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    	
    }
    
    
    /**
     * Calls new instance of the new simulated experiment wizard.
     */
    @FXML
    private void fileNewSimulatedExperimentButtonAction() {
    	
    	// Close any currently open experiments prior to opening a new one
    	if (experiment.get() != null) {
    		
    		Alert alert = new Alert(AlertType.CONFIRMATION);
    		alert.setTitle("Close current experiment?");
    		alert.setHeaderText("Creating a new simulated experiment will close the current view.");
    		alert.setContentText("Press OK to continue or Cancel to keep working with the current experiment.");

    		Optional<ButtonType> result = alert.showAndWait();
    		if (result.get() == ButtonType.CANCEL){
    		    return;
    		} 

        	// Reset app
        	this.reset();
    		
    	}

    	// Run the wizard
    	Parent root;
        try {
        	
        	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/aptasim/AptaSimWizard.fxml"));
        	
            root = loader.load();
            AptaSimWizardController controller = (AptaSimWizardController) loader.getController();
            
            // We need this to initialize the controller when the window gets closed
            controller.setRootLayoutController(this);
            
            Stage stage = new Stage();
            stage.setTitle("Create Simulated Data Set");
            stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("logo.png")));
            stage.setResizable(true);
            stage.setScene(new Scene(root, 800, 730));
    		stage.setOnCloseRequest((e) -> { e.consume(); });
            stage.show();
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    	
    }
    
    /**
     * Calls new instance of the export data wizard.
     */
    @FXML
    private void experimentExportDataButtonAction() {
    	
    	// Run the wizard
    	Parent root;
        try {
        	
        	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/export/exportWizard.fxml"));
        	
            root = loader.load();
            ExportWizardController controller = (ExportWizardController) loader.getController();
            
            
            Stage stage = new Stage();
            stage.setTitle("Export Data");
            stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("logo.png")));
            stage.setResizable(true);
            stage.setMinWidth(835);
            stage.setMinHeight(650);
            stage.setScene(new Scene(root,  Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE));
    		stage.setOnCloseRequest((e) -> { e.consume(); });
            stage.show();
            
            
        }
        catch (IOException e) {
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
    		SequencingDataRootController controller;
            try {
            	
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/core/sequencing/data/SequencingDataRoot.fxml"));
            	
            	Parent root = loader.load();
            	controller = (SequencingDataRootController) loader.getController();
                sequencingDataTab.setContent(root);
                
                tabInitializationMap.put(sequencingDataTab, controller);
                
            }
            catch (IOException e) {
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
    		AptamerPoolRootController controller;
            try {
            	
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/core/aptamer/pool/AptamerPoolRoot.fxml"));
            	
            	Parent root = loader.load();
            	controller = (AptamerPoolRootController) loader.getController();
            	aptamerPoolTab.setContent(root);
                
                tabInitializationMap.put(aptamerPoolTab, controller);
                
            }
            catch (IOException e) {
            	AptaLogger.log(Level.SEVERE, this.getClass(), e);
                e.printStackTrace();
            }
	    	
    	}
    	
    	// and add it to the tabs of the pane
    	this.rootTabPane.getTabs().add(2, aptamerPoolTab);
    	this.tabs.add(aptamerPoolTab);
    	   	
    }
    
    /**
     * Once data has been loaded by either creating a new experiment
     * or opening and existing one, this function takes care of 
     * adding the experiment overview tab to the main view
     */
    @FXML
    private void addAptamerFamilyAnalysisTab() {
    	
    	// Only create a new instance if this tab has not been opened before
    	if ( aptamerFamilyAnalysisTab == null) {
    	
	    	// create a new tab 
    		aptamerFamilyAnalysisTab = new Tab("Aptamer Family Analysis");
	    	
	    	
	    	// set properties
    		aptamerFamilyAnalysisTab.setClosable(true);
    	    // remove the instance from the tabs list
    		aptamerFamilyAnalysisTab.setOnCloseRequest(e -> { tabs.remove(aptamerFamilyAnalysisTab); });
	    	
    		AptamerFamilyAnalysisRootController controller;
            try {
            	
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/core/aptamer/family/analysis/AptamerFamilyAnalysisRoot.fxml"));
            	
            	Parent root = loader.load();
            	controller = (AptamerFamilyAnalysisRootController) loader.getController();
            	aptamerFamilyAnalysisTab.setContent(root);
                
                tabInitializationMap.put(aptamerFamilyAnalysisTab, controller);
                
            }
            catch (IOException e) {
            	AptaLogger.log(Level.SEVERE, this.getClass(), e);
                e.printStackTrace();
            }
	    	
    	}
    	
    	// and add it to the tabs of the pane
    	this.rootTabPane.getTabs().add(3, aptamerFamilyAnalysisTab);
    	this.tabs.add(aptamerFamilyAnalysisTab);
    	    	
    }
 
    
    /**
     * Once data has been loaded by either creating a new experiment
     * or opening and existing one, this function takes care of 
     * adding the experiment overview tab to the main view
     */
    @FXML
    private void addMotifAnalysisTab() {
    	
    	// Only create a new instance if this tab has not been opened before
    	if ( motifAnalysisTab == null) {
    	
	    	// create a new tab 
    		motifAnalysisTab = new Tab("Motif Analysis");
	    	
	    	
	    	// set properties
    		motifAnalysisTab.setClosable(true);
    	    // remove the instance from the tabs list
    		motifAnalysisTab.setOnCloseRequest(e -> { tabs.remove(motifAnalysisTab); });
	    	
    		MotifAnalysisRootController controller;
            try {
            	
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/core/motifs/MotifAnalysisRoot.fxml"));
            	
            	Parent root = loader.load();
            	controller = (MotifAnalysisRootController) loader.getController();
            	motifAnalysisTab.setContent(root);
                
                tabInitializationMap.put(motifAnalysisTab, controller);
                
            }
            catch (IOException e) {
            	AptaLogger.log(Level.SEVERE, this.getClass(), e);
                e.printStackTrace();
            }
	    	
	    	
    	}
    	
    	// and add it to the tabs of the pane
    	this.rootTabPane.getTabs().add(4, motifAnalysisTab);
    	this.tabs.add(motifAnalysisTab);
    	
    }    
    

    public void setPrimaryStage(Stage primaryStage) {
    	
    	this.primaryStage = primaryStage;
    	
    }

    /**
     * Resets the app to its initial state
     */
    private void reset() {
    	
    	rootTabPane.getTabs().clear();
    	tabs.clear();
    	
    	noExperimentVBox.setVisible(true);
    	
    	overviewTab = null;
    	sequencingDataTab = null;
    	aptamerPoolTab = null;
    	aptamerFamilyAnalysisTab = null;
    	motifAnalysisTab = null;
    	
    	tabInitializationMap.clear();

    	experiment.get().close();
    	experiment.set(null);
    }
    
    
    @FXML
    private void howToCiteAction() {
    	
    	Alert alert = new Alert(AlertType.INFORMATION);
    	alert.setTitle("How to cite this work");
    	alert.setHeaderText("If you used this work in your reserach, please cite the corresponding publications.");
    	alert.setContentText(
    			"AptaSuite \n" +
    			"Hoinka, J., Backofen, R. & Przytycka, T. M. (2018).\n"+
    			"AptaSUITE: A Full-Featured Bioinformatics Framework for the Comprehensive Analysis of Aptamers from HT-SELEX Experiments.\n"+ 
    			"Molecular Therapy - Nucleic Acids, 11, 515–517. https://doi.org/10.1016/j.omtn.2018.04.006\n\n"+   			
    			
    			"AptaPLEX \n" + 
    			"Hoinka, J., & Przytycka, T. (2016). \n" + 
    			"AptaPLEX - A dedicated, multithreaded demultiplexer for HT-SELEX data.\n" + 
    			"Methods. http://doi.org/10.1016/j.ymeth.2016.04.011\n\n" +
    			
    			"AptaSIM and AptaMUT\n" +
				"Hoinka, J., Berezhnoy, A., Dao, P., Sauna, Z. E., Gilboa, E., & Przytycka, T. M. (2015). \n" +
				"Large scale analysis of the mutational landscape in HT-SELEX improves aptamer discovery. \n" +
				"Nucleic Acids Research, 43(12), 5699–5707. http://doi.org/10.1093/nar/gkv308\n\n" +
				
				"AptaCLUSTER\n" + 
				"Hoinka, J., Berezhnoy, A., Sauna, Z. E., Gilboa, E., & Przytycka, T. M. (2014). \n" +
				"AptaCluster - A method to cluster HT-SELEX aptamer pools and lessons from its application.\n" +
				"In Lecture Notes in Computer Science  (Vol. 8394 LNBI, pp. 115–128). http://doi.org/10.1007/978-3-319-05269-4_9\n\n" +
    			
				"AptaTRACE  \n" +
				"Dao, P., Hoinka, J., Takahashi, M., Zhou, J., Ho, M., Wang, Y., Costa, F., Rossi, J. J., Backofen, R., Burnett, J., Przytycka, T. M. (2016). \n" +
				"AptaTRACE Elucidates RNA Sequence-Structure Motifs from Selection Trends in HT-SELEX Experiments. \n" +
				"Cell Systems, 3(1), 62–70. http://doi.org/10.1016/j.cels.2016.07.003"
    			
    			);

    	alert.showAndWait();
    	
    }
    
    @FXML
    private void openUserManualAction() {
    	
    	// TODO find JavaFX version of this
        try {
        	URI u = new URI("https://github.com/drivenbyentropy/aptasuite/wiki");
			java.awt.Desktop.getDesktop().browse(u);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    }

	/**
	 * @return the experiment
	 */
	public Experiment getExperiment() {
		return experiment.get();
	}

	/**
	 * @param experiment the experiment to set
	 */
	public void setExperiment(Experiment experiment) {
		this.experiment.set(experiment);
	}
    
    
}
