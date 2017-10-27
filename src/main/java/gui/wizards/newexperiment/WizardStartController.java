package gui.wizards.newexperiment;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

import javax.annotation.PostConstruct;

import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import io.datafx.controller.ViewController;
import io.datafx.controller.flow.action.LinkAction;

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
    @LinkAction(Wizard1Controller.class)
    private Button nextButton;

    /**
     * Validation Support to ensure correct user input
     */
    ValidationSupport validationSupport = new ValidationSupport();
    
    @PostConstruct
    public void init() {
        getBackButton().setDisable(true);

        // Bind the corresponding input fields to the DataModel associated with the wizard
        newExperimentName.textProperty().bindBidirectional(getDataModel().getExperimentName());
        newExperimentDescription.textProperty().bindBidirectional(getDataModel().getExperimentDescription());

        newExperimentIsDemultiplexed.selectedProperty().bindBidirectional(getDataModel().getIsDemultiplexed());
       	demultiplexedToggleGroup.selectToggle(getDataModel().getIsDemultiplexed().get() ? newExperimentIsDemultiplexed : newExperimentIsNotDemultiplexed);

        newExperimentIsPairedEnd.selectedProperty().bindBidirectional(getDataModel().getIsPairedEnd());
        pairedEndToggleGroup.selectToggle(getDataModel().getIsPairedEnd().get() ? newExperimentIsPairedEnd : newExperimentIsSingleEnd);
        projectPathTextField.textProperty().bindBidirectional(getDataModel().getProjectPath());
        
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
    	File selectedDirectory = chooser.showDialog(null);
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (selectedDirectory != null) {
    		
    		projectPathTextField.setText(selectedDirectory.getAbsolutePath());

    	}
    	
    }
    
    
}