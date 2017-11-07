package gui.wizards.newexperiment;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import utilities.AptaLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import io.datafx.controller.ViewController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.action.LinkAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;

/**
 * This is a view controller for the first steps in the wizard. The "back" and "finish" buttons of the action-bar that
 * is shown on each view of the wizard are defined in the AbstractWizardController class. So this class only needs to
 * define the "next" button. Because this is the first step in the wizard the "back" button should never be used. When
 * looking in the fxml files of the wizard the action-bar that contains all buttons is defined as a global fxml. By
 * doing so a developer doesn't need to recreate it for each view. Therefore the "back", "next" "finish" button will
 * automatically appear on each view. Because the "back" button shouldn't be used here it will become disabled by
 * setting the disable property in the init() method of the controller. As described in tutorial 1 the init() method
 * is annotated with the  @PostConstruct annotation and therefore this method will be called once all fields of the
 * controller instance were injected. So when the view appears on screen the "back" button will be disabled.
 *
 * When looking at the @FXMLController annotation of the class you can find a new feature. next to the fxml files that
 * defines the view of the wizard step a "title" is added. This defines the title of the view. Because the wizard is
 * added to a Stage by using the Flow.startInStage() method the title of the flow is automatically bound to the window
 * title of the Stage. So whenever the view in the flow changes the title of the application window will change to the
 * defined title of the view. As you will learn in future tutorial you can easily change the title of a view in code.
 * In addition to the title other metadata like a icon can be defined for a view or flow.
 */
@ViewController(value="wizardStart.fxml", title = "Wizard: Start")
public class WizardStartController extends AbstractWizardController {

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
    @ActionTrigger("validateData") //Calls method with corresponding @ActionMethod
    private Button nextButton;

    @FXML
    @ActionTrigger("advancedOptions")
    private Button finishButton;
    
    /**
     * Provdes access to DataFX's flow action handler
     */
    @ActionHandler
    protected FlowActionHandler actionHandler;
    
    /**
     * Validation Support to ensure correct user input
     */
    ValidationSupport validationSupport = new ValidationSupport();
    
    
    
    @PostConstruct
    public void init() {
        getBackButton().setDisable(true);

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
        
        // Give the user the opportunity to configure advanced options related to the parser and database
        // For this, we temporarly highjack the Finish Button
        this.finishButton.setText("Advanced Options");
        
        
    }
    
    
    /**
     * Implements the logic for choosing the folder to server as the base path of the project
     */
    @FXML
    private void projectPathFileChooserActionButton() {
    	
    	// Get the configuration file path
    	DirectoryChooser chooser = new DirectoryChooser();
    	chooser.setTitle("Choose the project location");
    	File selectedDirectory = chooser.showDialog(null);
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (selectedDirectory != null) {
    		
    		projectPathTextField.setText(selectedDirectory.getAbsolutePath());

    	}
    	
    }
    
    /**
     * Makes sure that all data fields are correct and valid
     */
    @ActionMethod("validateData")
    public void validateData() {
    	
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

    		if (result.get() == ButtonType.OK){
	    		try {
	    			actionHandler.navigate(Wizard1Controller.class);
	    		} catch (Exception e) {
	    			
	    			e.printStackTrace();
	    			AptaLogger.log(Level.SEVERE, this.getClass(), e);
	    			
	    		}
    		}
    	}
    	else {
    		
    		try {
    			
				actionHandler.navigate(Wizard1Controller.class);
				
			} catch (Exception e) {
				e.printStackTrace();
    			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			}
    		
    	}
    	
    }
    
    /**
     * Goes to the advanced options page
     */
    @ActionMethod("advancedOptions")
    public void advancedOptions() {
    	
    	
    	try {
			
			actionHandler.navigate(WizardAdvancedOptionsController.class);
			
		} catch (Exception e) {
			e.printStackTrace();
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
		}
    	
    }
    
}