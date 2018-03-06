package gui.wizards.newexperiment;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import utilities.AptaLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;

import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import gui.core.RootLayoutController;

public class WizardStartController{

	@FXML
	private TextField newExperimentName;
	
	@FXML
	private TextArea newExperimentDescription;
	
	@FXML
	private RadioButton newExperimentIsDemultiplexed;
	
	@FXML
	private RadioButton newExperimentIsNotDemultiplexed;
	
	@FXML
	private ToggleGroup demultiplexedToggleGroup;
	
	@FXML
	private RadioButton newExperimentIsPairedEnd;
	
	@FXML
	private RadioButton newExperimentIsSingleEnd;
	
	@FXML
	private ToggleGroup pairedEndToggleGroup;
	
	@FXML
	private TextField projectPathTextField;

	@FXML
	private ComboBox<String> fileFormatComboBox;
	
	@FXML
	private HBox actionBar;
	
	@FXML
	private ActionBarController actionBarController;
	
    /**
     * Buttons from the included XFML file
     */
    private Button backButton;
    private Button nextButton;
    private Button finishButton;
    
    /**
     * Validation Support to ensure correct user input
     */
    private ValidationSupport validationSupport = new ValidationSupport();
    
    /**
     * Reference to the root layout controller. will be passed from scene to scene
     */
    private RootLayoutController rootLayoutController;
    
    /**
     * Reference to the stage, will be passed from scene to scene
     */
    private Stage stage;
    
    /**
     * The datamodel storing all the information from the wizard. will be passed from scene to scene
     */
    private DataModel dataModel;
    
    public void init() {
    	
    	AptaLogger.log(Level.INFO, getClass(), "Starting New Experiment Wizard");
    	
    	setButtonActions();
    	
        // Fill the combobox
        //fileFormatComboBox.getItems().add("FASTA");
        fileFormatComboBox.getItems().add("FASTQ");
        fileFormatComboBox.getItems().add("RAW");
        
        // Bind the corresponding input fields to the DataModel associated with the wizard
        newExperimentName.textProperty().bindBidirectional(getDataModel().getExperimentName());
        newExperimentDescription.textProperty().bindBidirectional(getDataModel().getExperimentDescription());

        newExperimentIsDemultiplexed.selectedProperty().bindBidirectional(getDataModel().getIsDemultiplexed());
       	demultiplexedToggleGroup.selectToggle(getDataModel().getIsDemultiplexed().get() ? newExperimentIsDemultiplexed : newExperimentIsNotDemultiplexed);

        newExperimentIsPairedEnd.selectedProperty().bindBidirectional(getDataModel().getIsPairedEnd());
        pairedEndToggleGroup.selectToggle(getDataModel().getIsPairedEnd().get() ? newExperimentIsPairedEnd : newExperimentIsSingleEnd);
        projectPathTextField.textProperty().bindBidirectional(getDataModel().getProjectPath());
        
        this.fileFormatComboBox.valueProperty().bindBidirectional(getDataModel().getFileFormat());
        
        // Define the validation properties for the different fields
        validationSupport.registerValidator(newExperimentName, Validator.createEmptyValidator("The experiment name cannot be empty"));
        validationSupport.registerValidator(newExperimentDescription, Validator.createEmptyValidator("The experiment description cannot be empty"));
        validationSupport.registerValidator(projectPathTextField, Validator.createEmptyValidator("You must specify the base location for this experiment"));
        
        // Bind the validator to the next button so that it is only available if all fields have been filled
        nextButton.disableProperty().bind(validationSupport.invalidProperty()); 
        
    }
    
    
    /**
     * Implements the logic for choosing the folder to server as the base path of the project
     */
    @FXML
    private void projectPathFileChooserActionButton() {
    	
    	// Get the configuration file path
    	DirectoryChooser chooser = new DirectoryChooser();
    	chooser.setTitle("Choose the project location");
    	if (getDataModel().getProjectPath().isNotEmpty().get()) {
    		chooser.setInitialDirectory(new File(getDataModel().getProjectPath().get()));
    	}
    	File selectedDirectory = chooser.showDialog(null);
    	
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (selectedDirectory != null) {
    		
    		projectPathTextField.setText(selectedDirectory.getAbsolutePath());
    		getDataModel().setLastSelectedDirectory(selectedDirectory);
    	}
    	
    }
    
    
    /**
     * Makes sure that all data fields are correct and valid
     */
    public boolean validateData() {
    	
    	// We need to make sure the the user understands that overwriting an existing folder will 
    	// results in data loss
    	
    	// Create a safe string of the experiment name
    	String safe_experiment_name = getDataModel().getExperimentName().get().replaceAll("[^a-zA-Z0-9]+", "").trim();
    	
    	Path experiment_path = Paths.get(getDataModel().getProjectPath().get(), safe_experiment_name);
    	
    	// Check that the folder does not exist already and inform the user of the consequence
    	if (Files.exists(experiment_path)) {
    		
    		Alert alert = new Alert(AlertType.CONFIRMATION);
    		alert.setTitle("Overwrite Project?");
    		alert.setHeaderText("A folder with the experiment name already exists in the project path. \nAre you sure you want to overwrite it with a new experiment?");
    		alert.setContentText("Folder path: " + experiment_path.toAbsolutePath());

    		Optional<ButtonType> result = alert.showAndWait();

    		return (result.get() == ButtonType.OK);

    	}
    	else {
    		
    		return true;
    		
    	}
    	
    }
    
    
    /**
     * Defines the actions to be taken when any of the three buttons is pressed
     */
    private void setButtonActions() {
    	
    	// Inject buttons from included controller
    	this.backButton = this.actionBarController.getBackButton();
    	this.nextButton = this.actionBarController.getNextButton();
    	this.finishButton = this.actionBarController.getFinishButton();
    	
    	// Back Action
        backButton.setDisable(true);
        
        // Next Action
        this.nextButton.setOnAction( (event)->{
        	
        	if (validateData()) {
        	
	        	// Load the advanced option controller
	        	Parent root;
	            try {
	            																				
	            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/wizard1.fxml"));
	            	
	                root = loader.load();
	                Wizard1Controller controller = (Wizard1Controller) loader.getController();
	                
	        		// Pass instances and initialize
	                controller.setRootLayoutController(this.rootLayoutController);
	                controller.setStage(this.stage);
	                controller.setDataModel(this.dataModel);
	                controller.init();
	                
	                stage.setScene(new Scene(root, Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE));
	                
	            }
	            catch (IOException e) {
	                e.printStackTrace();
	            }
        	
        	}
            
        });
        
        
    	// Finish Action
        
        // Give the user the opportunity to configure advanced options related to the parser and database
        // For this, we temporarly highjack the Finish Button
        this.finishButton.setText("Advanced Options");
        
        this.finishButton.setOnAction( (event)->{
        	
        	// Load the advanced option controller
        	Parent root;
            try {
            																				
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/wizardAdvancedOptions.fxml"));
            	
                root = loader.load();
                WizardAdvancedOptionsController controller = (WizardAdvancedOptionsController) loader.getController();
                
        		// Pass instances and initialize
                controller.setRootLayoutController(this.rootLayoutController);
                controller.setStage(this.stage);
                controller.setDataModel(this.dataModel);
                controller.init();
                
                stage.setScene(new Scene(root, Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE));
                
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        	
        });
    	
    }
    
    private DataModel getDataModel() {
    	
    	return this.dataModel;
    	
    }
    
    public void setRootLayoutController( RootLayoutController rlc) {
    	
    	this.rootLayoutController = rlc;
    	
    }
    
    public void setDataModel( DataModel datamodel ) {
    	
    	this.dataModel = datamodel;
    	
    }
    
    public void setStage( Stage s) {
    	
    	this.stage = s;
    	
    }
    
    
    
    
}