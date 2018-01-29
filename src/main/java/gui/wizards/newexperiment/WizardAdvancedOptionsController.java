package gui.wizards.newexperiment;

import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.validation.Validator;

import org.controlsfx.validation.ValidationSupport;

import io.datafx.controller.ViewController;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import utilities.AptaLogger;

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
@ViewController(value="wizardAdvancedOptions.fxml", title = "Wizard: Step 3")
public class WizardAdvancedOptionsController extends AbstractWizardController {

	@FXML
	private TextField mapDBAptamerPoolBloomFilterCapacityTextField;

	@FXML
	private TextField mapDBAptamerPoolBloomFilterCollisionProbabilityTextField;
	
	@FXML
	private TextField mapDBAptamerPoolMaxTreeMapCapacityTextField;
	
	@FXML
	private TextField mapDBSelectionCycleBloomFilterCollisionProbabilityTextField;
	
	@FXML
	private Spinner<Integer> performanceMaxNumberOfCoresSpinner;	
	
    @FXML
    private Button nextButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private ComboBox<Boolean> storeReverseComplementComboBox;
    
    @FXML
    @ActionTrigger("goBack")
    private Button finishButton;
   
    /**
     * Provides access to DataFX's flow action handler
     */
    @ActionHandler
    protected FlowActionHandler actionHandler;
    
    /**
     * Validation Support to ensure correct user input
     */
    private ValidationSupport validationSupport = new ValidationSupport();
    
    @PostConstruct
    public void init() {
    	
    	// Since this is a special page, we only allow to go back from here. 
    	this.backButton.setVisible(false);
    	this.nextButton.setVisible(false);
    	this.finishButton.setText("Back");
    	
    	ObservableList<Boolean> list = FXCollections.observableArrayList(true,false);
    	storeReverseComplementComboBox.setItems(list);
    	storeReverseComplementComboBox.setConverter(new YesNoStringConverter());
    	
    	// Bind to datamodel
    	mapDBAptamerPoolBloomFilterCapacityTextField.textProperty().bindBidirectional(getDataModel().getMapDBAptamerPoolBloomFilterCapacity(), new NumberStringConverter());
    	mapDBAptamerPoolBloomFilterCollisionProbabilityTextField.textProperty().bindBidirectional(getDataModel().getMapDBAptamerPoolBloomFilterCollisionProbability(), new NumberStringConverter());
    	mapDBAptamerPoolMaxTreeMapCapacityTextField.textProperty().bindBidirectional(getDataModel().getMapDBAptamerPoolMaxTreeMapCapacity(), new NumberStringConverter());
    	mapDBSelectionCycleBloomFilterCollisionProbabilityTextField.textProperty().bindBidirectional(getDataModel().getMapDBSelectionCycleBloomFilterCollisionProbability(), new NumberStringConverter());
    	performanceMaxNumberOfCoresSpinner.getValueFactory().valueProperty().bindBidirectional(getDataModel().getPerformanceMaxNumberOfCores());
    	storeReverseComplementComboBox.valueProperty().bindBidirectional(getDataModel().getStoreReverseComplement());
    	
    }
    
    
    @ActionMethod("goBack")
    public void goBack() { 
    	
    	try {
			
			actionHandler.navigate(WizardStartController.class);
			
		} catch (Exception e) {
			e.printStackTrace();
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
		}
    	
    }
    
    public class YesNoStringConverter extends StringConverter<Boolean> {
        @Override
        public String toString(Boolean bool) {
            return (bool?"Yes":"No");
        }

        @Override
        public Boolean fromString(String s) {
            return s.equalsIgnoreCase("yes");
        }
    }
    
}