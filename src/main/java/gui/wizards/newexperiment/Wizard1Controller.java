package gui.wizards.newexperiment;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.decoration.Decorator;
import org.controlsfx.control.decoration.GraphicDecoration;
import org.controlsfx.control.decoration.StyleClassDecoration;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import gui.core.RootLayoutController;
import gui.misc.ControlFXValidatorFactory;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.action.LinkAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

/**
 * This is a view controller for one of the steps in the wizard. The "back" and "finish" buttons of the action-bar that
 * is shown on each view of the wizard are defined in the AbstractWizardController class. So this class only needs to
 * define the "next" button. By using the @LinkAction annotation this button will link on the next step of the
 * wizard. This annotation was already described in tutorial 2.
 *
 * When looking at the @FXMLController annotation of the class you can find a new feature. next to the fxml files that
 * defines the view of the wizard step a "title" is added. This defines the title of the view. Because the wizard is
 * added to a Stage by using the Flow.startInStage() method the title of the flow is automatically bound to the window
 * title of the Stage. So whenever the view in the flow changes the title of the application window will change to the
 * defined title of the view. As you will learn in future tutorial you can easily change the title of a view in code.
 * In addition to the title other metadata like a icon can be defined for a view or flow.
 */
@ViewController(value="wizard1.fxml", title = "Wizard: Step 1")
public class Wizard1Controller extends AbstractWizardController {

	@FXML
	private BorderPane rootBorderPane;
	
	@FXML
	private VBox selectionCycleContainers;
	
	@FXML
	private Button addSelectionCycleButton;
	
	@FXML
	private Label forwardReadsFileLabel;
	
	@FXML
	private TextField forwardReadsFileTextField;
	
	@FXML
	private Button forwardReadsFileButton;
	
	@FXML
	private Label reverseReadsFileLabel;
	
	@FXML
	private TextField reverseReadsFileTextField;
	
	@FXML
	private Button reverseReadsFileButton;
	
	@FXML
	private TextField primer5TextField;
	
	@FXML
	private TextField primer3TextField;
	
	@FXML
	private Spinner<Integer> randomizedRegionSizeSpinner;
	
    @FXML
    @ActionTrigger("validateData") //Calls method with corresponding @ActionMethod
    private Button nextButton;
    
    private NotificationPane notificationPane;
    
    /**
     * Proides access to DataFX's flow action handler
     */
    @ActionHandler
    protected FlowActionHandler actionHandler;
    
    /**
     * Validation Support to ensure correct user input
     */
    private ValidationSupport validationSupport = new ValidationSupport();
    
    @PostConstruct
    public void init() {
    
    	// Bind the data to the content
    	primer5TextField.textProperty().bindBidirectional(getDataModel().getPrimer5());
    	primer3TextField.textProperty().bindBidirectional(getDataModel().getPrimer3());
    	
    	randomizedRegionSizeSpinner.getValueFactory().valueProperty().bindBidirectional(getDataModel().getRandomizedRegionSize());

    	forwardReadsFileLabel.textProperty().bind(getDataModel().getForwardReadsFile());
    	reverseReadsFileLabel.textProperty().bind(getDataModel().getReverseReadsFile());
    	
    	// Depending on whether the data is demultiplexed or not, we need to disable the file choosers
    	forwardReadsFileLabel.visibleProperty().bind(getDataModel().getIsDemultiplexed().not());
    	forwardReadsFileTextField.visibleProperty().bind(getDataModel().getIsDemultiplexed().not());
    	forwardReadsFileButton.visibleProperty().bind(getDataModel().getIsDemultiplexed().not());
    	
    	// For the reverse files, we also need to check if the data is paired end or not
    	reverseReadsFileLabel.visibleProperty().bind(Bindings.and(getDataModel().getIsDemultiplexed().not(), getDataModel().getIsPairedEnd()));
    	reverseReadsFileTextField.visibleProperty().bind(Bindings.and(getDataModel().getIsDemultiplexed().not(), getDataModel().getIsPairedEnd()));
    	reverseReadsFileButton.visibleProperty().bind(Bindings.and(getDataModel().getIsDemultiplexed().not(), getDataModel().getIsPairedEnd()));
    	
    	// Add any SelectionCycleContains which have previously been created to the view
    	for (TitledPane tp : getDataModel().getSelectionCycleDetailPanes()) {
    		
    		selectionCycleContainers.getChildren().add(tp);
    		
    	}
    
    	// We also need to update the parent of the selection cycle controllers in 
    	// case the user has moved between views in the meantime
    	for ( SelectionCycleDetailsController controller : getDataModel().getSelectionCycleDetailControllers()) {

    		controller.setParent(this);
    		
    	}
    	
    	// Primers must be all DNA and at least for the 5' end, it must be present
		validationSupport.registerValidator(primer5TextField, true, Validator.combine(ControlFXValidatorFactory.DNAStringValidator, Validator.createEmptyValidator("At least the 5' primer must be specified")) );
		validationSupport.registerValidator(primer3TextField, true, ControlFXValidatorFactory.DNAStringOrEmptyValidator);
		
		// Add some additional contraints to the fields
		primer5TextField.textProperty().addListener((ov, oldValue, newValue) -> { primer5TextField.setText(newValue.toUpperCase()); });
		primer3TextField.textProperty().addListener((ov, oldValue, newValue) -> { primer3TextField.setText(newValue.toUpperCase());	});
		
		// Require forward and reverse read if multiplexed data is present
		if (getDataModel().getIsDemultiplexed().not().get()) {
			
			validationSupport.registerValidator(this.forwardReadsFileTextField, Validator.createEmptyValidator("The forward read file must be specified"));
			
			if (getDataModel().getIsPairedEnd().get()) {
				
				validationSupport.registerValidator(this.reverseReadsFileTextField, Validator.createEmptyValidator("The reverse read file must be specified"));
				
			}
		}
		
		// Bind the validator to the next button so that it is only available if all fields have been filled
        nextButton.disableProperty().bind(validationSupport.invalidProperty()); 
    }
    
    
    private Node createDecoratorNode(Color color) {
    	Rectangle d = new Rectangle(7, 7);
        d.setFill(color);
        return d;
    }
    
    /**
     * Makes sure that all data fields are correct and valid
     */
    @ActionMethod("validateData")
    public void validateData() {
    	
    	// Add some CSS for the validator
    	rootBorderPane.getScene().getStylesheets().add(this.getClass().getResource("/gui/misc/decorations.css").toExternalForm());
    	
    	//First the trivial things
    	Decorator.addDecoration(primer5TextField, new StyleClassDecoration("warning"));

    	validationSupport.registerValidator(primer5TextField, ControlFXValidatorFactory.AllwaysWrongValidator("testttttttda d ass"));
    	validationSupport.errorDecorationEnabledProperty().set(true);
    	
//    	write a function which does the following
//    	1) sets validationSupport for a node (passed as paramter) with error message
//		2) sets a handler which removes that valididator from the validationSupport when the node is clicked
//
//    	
    	
//    	try {
//			actionHandler.navigate(Wizard2Controller.class);
//		} catch (VetoException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (FlowException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    	
    }
    
    /**
     * Adds a new instance of the Selection Cycle Inputs to the VBOX
     */
    @FXML
    private void addSelectionCycleButtonAction() {

    	// Create an instance of the node
    	TitledPane selectionCyclePane = null;
    	SelectionCycleDetailsController selectionCycleController = null;
    	
    	try {
    		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/selectionCycleDetails.fxml"));
			selectionCyclePane = (TitledPane) loader.load();
			selectionCycleController = loader.getController();
			
			// Make the corresponding data models available to the controller. Inject did not work for some reason
			selectionCycleController.setDataModel(getDataModel());
			selectionCycleController.setSelectionCycleDataModel(new SelectionCycleDataModel());
			selectionCycleController.setParent(this);
			
			// Initialize any fields
			selectionCycleController.init();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	// Add it to the scene
    	selectionCycleContainers.getChildren().add(selectionCyclePane);
    	
    	// And store it in the data model
    	getDataModel().getSelectionCycleDetailControllers().add(selectionCycleController);
    	getDataModel().getSelectionCycleDetailPanes().add(selectionCyclePane);
    	
    }
    
    /**
     * Removes the controller and any data associated with it from the wizard
     * @param controller
     */
    public void removeSelectionCycle(SelectionCycleDetailsController controller) {
    	
    	// First identify the index at which we need to delete the elements
    	int index_to_delete = getDataModel().getSelectionCycleDetailControllers().indexOf(controller);
    	
    	// Now remove them from the view...
    	selectionCycleContainers.getChildren().remove(index_to_delete);
    	
    	// ...and the data model
    	getDataModel().getSelectionCycleDetailControllers().remove(index_to_delete);
    	getDataModel().getSelectionCycleDetailPanes().remove(index_to_delete);
    	
    }
    
    /**
     * Logic to choose the multiplexed forward read files
     * @param event
     */
    @FXML
    private void chooseForwardReadFileButtonAction(ActionEvent event) {
    	
    	chooseInputFile(forwardReadsFileTextField);
    	
    }
    
    /**
     * Logic to choose the multiplexed reverse read files
     * @param event
     */
    @FXML
    private void chooseReverseReadFileButtonAction(ActionEvent event) {
    	
    	chooseInputFile(reverseReadsFileTextField);
    	
    }
    
    /**
     * Handles selection of the sequencing files and sets the file
     * chosen by the used in <code>target</code>
     * @param target
     */
    private void chooseInputFile(TextField target) {
    	
    	// Get the configuration file path
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Choose the sequencing data file");
    	FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter("Sequencing Files", "*.fastq", ".fastq.gz", "*.txt", "*.txt.gz");
    	fileChooser.getExtensionFilters().add(fileExtensions);
    	File cfp = fileChooser.showOpenDialog(null);
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (cfp != null) {
    		
    		target.setText(cfp.getAbsolutePath());

    	}
    }

}