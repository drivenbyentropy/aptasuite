package gui.wizards.newexperiment;

import java.io.IOException;

import org.controlsfx.validation.ValidationSupport;

import gui.core.RootLayoutController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;


public class WizardAdvancedOptionsController {

	@FXML
	private TextField aptaplexParserPairedEndMinOverlapTextField;
	
	@FXML
	private TextField aptaplexParserPairedEndMaxMutationsTextField;
	
	@FXML
	private TextField aptaplexParserPairedEndMaxScoreValueTextField;
	
	@FXML 
	private TextField aptaplexParserBarcodeToleranceTextField;
	
	@FXML
	private TextField aptaplexParserPrimerToleranceTextField;
	
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
    private ComboBox<Boolean> storeReverseComplementComboBox;
    
    @FXML
    private ComboBox<Boolean> onlyRandomizedRegionInData;
    
    @FXML
    private ComboBox<Boolean> undeterminedToFileComboBox;
    
    @FXML
    private ComboBox<Boolean> checkReverseComplementComboBox;
    
    
	@FXML
	private HBox actionBar;
	
	@FXML
	private ActionBarController actionBarController;
	
	

    /**
     * Validation Support to ensure correct user input
     */
    private ValidationSupport validationSupport = new ValidationSupport();

    /**
     * Buttons from the included XFML file
     */
    private Button backButton;
    private Button nextButton;
    private Button finishButton;
   
    
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
    	
    	setButtonActions();
    	
    	// Combo Boxes
    	storeReverseComplementComboBox.setItems(FXCollections.observableArrayList(true,false));
    	storeReverseComplementComboBox.setConverter(new YesNoStringConverter());
    	
    	onlyRandomizedRegionInData.setItems(FXCollections.observableArrayList(true,false));
    	onlyRandomizedRegionInData.setConverter(new YesNoStringConverter());
    	onlyRandomizedRegionInData.disableProperty().bind(dataModel.getIsDemultiplexed());
    	
    	undeterminedToFileComboBox.setItems(FXCollections.observableArrayList(true,false));
    	undeterminedToFileComboBox.setConverter(new YesNoStringConverter());

    	checkReverseComplementComboBox.setItems(FXCollections.observableArrayList(true,false));
    	checkReverseComplementComboBox.setConverter(new YesNoStringConverter());
    	
    	// Bind to datamodel
    	aptaplexParserPairedEndMinOverlapTextField.textProperty().bindBidirectional(getDataModel().getAptaplexParserPairedEndMinOverlap(), new NumberStringConverter());
    	aptaplexParserPairedEndMaxMutationsTextField.textProperty().bindBidirectional(getDataModel().getAptaplexParserPairedEndMaxMutations(), new NumberStringConverter());
    	aptaplexParserPairedEndMaxScoreValueTextField.textProperty().bindBidirectional(getDataModel().getAptaplexParserPairedEndMaxScoreValue(), new NumberStringConverter());
    	aptaplexParserBarcodeToleranceTextField.textProperty().bindBidirectional(getDataModel().getAptaplexParserBarcodeTolerance(), new NumberStringConverter());
    	aptaplexParserPrimerToleranceTextField.textProperty().bindBidirectional(getDataModel().getAptaplexParserPrimerTolerance(), new NumberStringConverter());
    	
    	
    	mapDBAptamerPoolBloomFilterCapacityTextField.textProperty().bindBidirectional(getDataModel().getMapDBAptamerPoolBloomFilterCapacity(), new NumberStringConverter());
    	mapDBAptamerPoolBloomFilterCollisionProbabilityTextField.textProperty().bindBidirectional(getDataModel().getMapDBAptamerPoolBloomFilterCollisionProbability(), new NumberStringConverter());
    	mapDBAptamerPoolMaxTreeMapCapacityTextField.textProperty().bindBidirectional(getDataModel().getMapDBAptamerPoolMaxTreeMapCapacity(), new NumberStringConverter());
    	mapDBSelectionCycleBloomFilterCollisionProbabilityTextField.textProperty().bindBidirectional(getDataModel().getMapDBSelectionCycleBloomFilterCollisionProbability(), new NumberStringConverter());
    	performanceMaxNumberOfCoresSpinner.getValueFactory().valueProperty().bindBidirectional(getDataModel().getPerformanceMaxNumberOfCores());
    	storeReverseComplementComboBox.valueProperty().bindBidirectional(getDataModel().getStoreReverseComplement());
    	onlyRandomizedRegionInData.valueProperty().bindBidirectional(getDataModel().getOnlyRandomizedRegionInData() );
    	undeterminedToFileComboBox.valueProperty().bindBidirectional(getDataModel().getUndeterminedToFile());
    	checkReverseComplementComboBox.valueProperty().bindBidirectional(getDataModel().getCheckReverseComplement());
    	
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
    
    /**
     * Defines the actions to be taken when any of the three buttons is pressed
     */
    private void setButtonActions() {
    	
    	// Inject buttons from included controller
    	this.backButton = this.actionBarController.getBackButton();
    	this.nextButton = this.actionBarController.getNextButton();
    	this.finishButton = this.actionBarController.getFinishButton();
    	
    	// Since this is a special page, we only allow to go back from here. 
    	this.backButton.setVisible(false);
    	this.nextButton.setVisible(false);
    	this.finishButton.setText("Back");
        
    	// Finish Action
        this.finishButton.setOnAction( (event)->{
        	
        	// Load the advanced option controller
        	Parent root;
            try {
            																				
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/wizardStart.fxml"));
            	
                root = loader.load();
                WizardStartController controller = (WizardStartController) loader.getController();
                
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