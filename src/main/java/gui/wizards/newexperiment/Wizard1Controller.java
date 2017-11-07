package gui.wizards.newexperiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import gui.misc.ControlFXValidatorFactory;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
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
    private Label errorLabel;
    
    @FXML
    @ActionTrigger("validateData") //Calls method with corresponding @ActionMethod
    private Button nextButton;
    
    private List<SelectionCycleDetailsController> selectionCycleDetailsControllers = new ArrayList<SelectionCycleDetailsController>();
    
    /**
     * Provdes access to DataFX's flow action handler
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

    	forwardReadsFileTextField.textProperty().bindBidirectional(getDataModel().getForwardReadsFile());
    	reverseReadsFileTextField.textProperty().bindBidirectional(getDataModel().getReverseReadsFile());
    	
    	// Depending on whether the data is demultiplexed or not, we need to disable the file choosers
    	forwardReadsFileLabel.visibleProperty().bind(getDataModel().getIsDemultiplexed().not());
    	forwardReadsFileTextField.visibleProperty().bind(getDataModel().getIsDemultiplexed().not());
    	forwardReadsFileButton.visibleProperty().bind(getDataModel().getIsDemultiplexed().not());
    	
    	// For the reverse files, we also need to check if the data is paired end or not
    	reverseReadsFileLabel.visibleProperty().bind(Bindings.and(getDataModel().getIsDemultiplexed().not(), getDataModel().getIsPairedEnd()));
    	reverseReadsFileTextField.visibleProperty().bind(Bindings.and(getDataModel().getIsDemultiplexed().not(), getDataModel().getIsPairedEnd()));
    	reverseReadsFileButton.visibleProperty().bind(Bindings.and(getDataModel().getIsDemultiplexed().not(), getDataModel().getIsPairedEnd()));

    	// Add any SelectionCycleContains which have previously been created to the view
    	for (SelectionCycleDataModel model : getDataModel().getSelectionCycleDataModels()) {
    		
    		this.addSelectionCycle(model, false);
    		
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
        // nextButton.disableProperty().bind(validationSupport.invalidProperty()); 
    }
    
   
    /**
     * Makes sure that all data fields are correct and valid
     */
    @ActionMethod("validateData")
    public void validateData() {
    	
    	boolean is_valid = true;
    	
    	// Add some CSS for the validator
    	rootBorderPane.getScene().getStylesheets().add(this.getClass().getResource("/gui/misc/decorations.css").toExternalForm());

    	// First the trivial things:
    	
    	// Make sure the 5' primer is present
    	if (primer5TextField.textProperty().isEmpty().get()) {
    		
    		ControlFXValidatorFactory.setTemporaryValidation(
    				primer5TextField, 
        			ControlFXValidatorFactory.AllwaysWrongValidator("The 5' primer must be specified"), 
        			this.validationSupport, 
        			ControlFXValidatorFactory.DNAStringValidator,
        			true
        			);
    		
    		is_valid = false;
    		
    	}
    	
    	// Make sure that either the 3' primer is present or the  randomized region size is not 0 
    	if (primer3TextField.textProperty().isEmpty().get() && randomizedRegionSizeSpinner.getValueFactory().getValue().equals(0)) {
    		
    		ControlFXValidatorFactory.setTemporaryValidation(
        			primer3TextField, 
        			ControlFXValidatorFactory.AllwaysWrongValidator("Either a 3' primer or a randomized region must be specified."), 
        			this.validationSupport, 
        			ControlFXValidatorFactory.DNAStringValidator,
        			true
        			);
    		
    		ControlFXValidatorFactory.setTemporaryValidation(
        			randomizedRegionSizeSpinner, 
        			ControlFXValidatorFactory.AllwaysWrongValidator("Either a 3' primer or a randomized region must be specified."), 
        			this.validationSupport, 
        			ControlFXValidatorFactory.AllwaysCorrectValidator,
        			false
        			);

    		is_valid = false;
    		
    	}
    	
    	// If the data is multiplexed, we need at least the forward read file
    	if(getDataModel().getIsDemultiplexed().not().get()) {
    		
    		if (forwardReadsFileTextField.textProperty().isEmpty().get()) {
    			
    			ControlFXValidatorFactory.setTemporaryValidation(
    					forwardReadsFileTextField, 
            			ControlFXValidatorFactory.AllwaysWrongValidator("Please specify the file for the forward reads"), 
            			this.validationSupport, 
            			Validator.createEmptyValidator("The forward read file must be specified"),
            			false
            			);
    			
    			is_valid = false;
    			
    		}
    		
    		// in addition if we are paired end, we also need that file
			if (getDataModel().getIsPairedEnd().get() && reverseReadsFileTextField.textProperty().isEmpty().get()) {
			    			
    			ControlFXValidatorFactory.setTemporaryValidation(
    					reverseReadsFileTextField, 
            			ControlFXValidatorFactory.AllwaysWrongValidator("Please specify the file for the reverse reads"), 
            			this.validationSupport, 
            			Validator.createEmptyValidator("The reverse read file must be specified"),
            			false
            			);
			    			
    			is_valid = false;
			}
    		
    	}
    	
    	// First, we must make sure that we have at least one selection cycle
    	if (getDataModel().getSelectionCycleDataModels().isEmpty()) {
    		
    		ControlFXValidatorFactory.setTemporaryValidation(
					addSelectionCycleButton, 
        			ControlFXValidatorFactory.AllwaysWrongValidator("Please add at least one selection cycle"), 
        			this.validationSupport, 
        			ControlFXValidatorFactory.AllwaysCorrectValidator,
        			false
        			);
    		
    		is_valid = false;
    		
    	}
    	
    	// Iterate over the selection cycles and validate them
    	List<Boolean> hasBarcode3 = new ArrayList<Boolean>();
    	Map<String, ArrayList<TextField>> round_name_map = new HashMap<String, ArrayList<TextField>>();
    	for (SelectionCycleDetailsController selection_cycle_controller : this.selectionCycleDetailsControllers) {
    		
    		// Prepare for round name check
    		if (!round_name_map.containsKey(selection_cycle_controller.getSelectionCycleDataModel().getRoundName().get())) {
    			
    			round_name_map.put(selection_cycle_controller.getSelectionCycleDataModel().getRoundName().get(), new ArrayList<TextField>());
    			
    		}
    		
    		round_name_map.get(selection_cycle_controller.getSelectionCycleDataModel().getRoundName().get()).add(selection_cycle_controller.getRoundNameTextField());
    	
    		// Make sure we have the sequencing files if data is demultiplexed
    		if(getDataModel().getIsDemultiplexed().get()) {
    			
    			if ( selection_cycle_controller.getForwardReadsFileTextField().textProperty().isEmpty().get()) {
        			
        			ControlFXValidatorFactory.setTemporaryValidation(
        					selection_cycle_controller.getForwardReadsFileTextField(), 
                			ControlFXValidatorFactory.AllwaysWrongValidator("Please specify the file for the forward reads"), 
                			this.validationSupport, 
                			Validator.createEmptyValidator("The forward read file must be specified"),
                			false
                			);
        			
        			is_valid = false;
        			
        		}
        		
        		// in addition if we are paired end, we also need that file
    			if (getDataModel().getIsPairedEnd().get() && selection_cycle_controller.getReverseReadsFileTextField().textProperty().isEmpty().get()) {
    			    			
        			ControlFXValidatorFactory.setTemporaryValidation(
        					selection_cycle_controller.getReverseReadsFileTextField(), 
                			ControlFXValidatorFactory.AllwaysWrongValidator("Please specify the file for the reverse reads"), 
                			this.validationSupport, 
                			Validator.createEmptyValidator("The reverse read file must be specified"),
                			false
                			);
    			    			
        			is_valid = false;
    			}
    			
    		}
    		else { // Otherwise we need barcodes
    			
    			if (selection_cycle_controller.getBarcode5TextField().textProperty().isEmpty().get()) {
    			
	    			ControlFXValidatorFactory.setTemporaryValidation(
	    					selection_cycle_controller.getBarcode5TextField(), 
	            			ControlFXValidatorFactory.AllwaysWrongValidator("Please specify the barcode/index for this round"), 
	            			this.validationSupport, 
	            			Validator.createEmptyValidator("The barcode/index for this round must be specified"),
	            			false
	            			);
				    			
	    			is_valid = false;
    			
    			}
    			
    			// record if we have a 3' barcode
    			hasBarcode3.add(selection_cycle_controller.getBarcode3TextField().textProperty().isEmpty().not().get());
    		}
    		
    	}
    	
    	// Make sure that if the user specified a 3' primer barcode once, they specify it everywhere
    	Map<Boolean, Long> num_true = hasBarcode3.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    	
    	if (num_true.get(true) != 0 || num_true.get(true) != this.selectionCycleDetailsControllers.size()) {
    	
	    	for (int x=0; x<this.selectionCycleDetailsControllers.size(); x++) {
	    		
	    		if (!hasBarcode3.get(x)) {
	    			
	    			ControlFXValidatorFactory.setTemporaryValidation(
	    					selectionCycleDetailsControllers.get(x).getBarcode3TextField(),
	            			ControlFXValidatorFactory.AllwaysWrongValidator("Please specify the 3' barcode/index for this round"), 
	            			this.validationSupport, 
	            			Validator.createEmptyValidator("The 3' barcode/index for this round must be specified"),
	            			false
	            			);
				    			
	    			is_valid = false;
	    			
	    		}
	    		
	    	}
    	
    	}
    	
    	
    	// Make sure we have no duplicate round names
    	for ( Entry<String, ArrayList<TextField>> item : round_name_map.entrySet()) {
    		
    		if (item.getValue().size() > 1) {
    			
    			for( TextField textfield : item.getValue() ) {
    				
    				ControlFXValidatorFactory.setTemporaryValidation(
    						textfield, 
    	        			ControlFXValidatorFactory.AllwaysWrongValidator("The round names must be unique."), 
    	        			this.validationSupport, 
    	        			ControlFXValidatorFactory.AllwaysCorrectValidator,
    	        			false
    	        			);
    				
    			}
    			
    			is_valid = false;
    			
    		}
    		
    	}
    	
    	
    	// Enable the support without the user having to interact with a node first 
    	validationSupport.setErrorDecorationEnabled(true);
    	validationSupport.redecorate();
    	
    	// We only go the the next screen if validation is successful
    	if (is_valid) {
    		
    		// Create the base folder
    		String safe_experiment_name = getDataModel().getExperimentName().get().replaceAll("[^a-zA-Z0-9]+", "").trim();
        	Path experiment_path = Paths.get(getDataModel().getProjectPath().get(), safe_experiment_name);
    		
        	try {
        		
	        	if ( Files.exists(experiment_path) ) {
	        		
//	        			Files.walk - return all files/directories below rootPath including
//  	      			.sorted - sort the list in reverse order, so the directory itself comes after the including subdirectories and files
//      	  			.map - map the Path to File
//        				.forEach - calls the .delete() method on every File object
						Files.walk(experiment_path, FileVisitOption.FOLLOW_LINKS)
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
	        		
	        	}
	        	
        		Files.createDirectories(experiment_path);
        		
        	} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
			}
        	
    		// Write this configuration to file before going to the next screen
        	Path configuration_file = Paths.get(experiment_path.toAbsolutePath().toString(), "configuration.aptasuite");
    		utilities.Configuration.createConfiguration(configuration_file);
    		
    		// Experiment Name and Description
    		utilities.Configuration.getParameters().addProperty("Experiment.name", getDataModel().getExperimentName().get());
    		utilities.Configuration.getParameters().addProperty("Experiment.description", getDataModel().getExperimentDescription().get());
    		
    		// Project Path
    		utilities.Configuration.getParameters().addProperty("Experiment.projectPath", experiment_path.toAbsolutePath().toFile());
    		
    		// Primers and RR size
    		utilities.Configuration.getParameters().addProperty("Experiment.primer5", getDataModel().getPrimer5().get());
    		if (getDataModel().getPrimer3().isEmpty().not().get()) {
    		
    			utilities.Configuration.getParameters().addProperty("Experiment.primer3", getDataModel().getPrimer3().get());
    		
    		}
    		
    		if (getDataModel().getRandomizedRegionSize().isEqualTo(0).not().get()) {
    			
    			utilities.Configuration.getParameters().addProperty("Experiment.randomizedRegionSize", getDataModel().getRandomizedRegionSize().get());
    			
    		}
    		
    		// isPerFile
    		utilities.Configuration.getParameters().addProperty("AptaplexParser.isPerFile", getDataModel().getIsDemultiplexed().get());
    		
    		// File format
    		switch ( getDataModel().getFileFormat().get()) {
    		
    			case "FASTQ":	utilities.Configuration.getParameters().addProperty("AptaplexParser.reader", "FastqReader");
    		
    			case "RAW":		utilities.Configuration.getParameters().addProperty("AptaplexParser.reader", "RawReader");
    		}
    		
    		// Selection cycle details
    		List<String> names = new ArrayList<String>();
    		List<String> forwardFiles = new ArrayList<String>();
    		List<String> reverseFiles = new ArrayList<String>();
    		List<Integer> round = new ArrayList<Integer>();
    		List<Boolean> isControl = new ArrayList<Boolean>();
    		List<Boolean> isCounter = new ArrayList<Boolean>();
    		for ( SelectionCycleDataModel selection_cycle_model : getDataModel().getSelectionCycleDataModels()) {
    			
    			names.add(selection_cycle_model.getRoundName().get());
    			round.add(selection_cycle_model.getRoundNumber().get());
    			
    			if (getDataModel().getIsDemultiplexed().get()) {
    				
    				forwardFiles.add(selection_cycle_model.getForwardReadsFile().get());
    			
    				if (getDataModel().getIsPairedEnd().get()) {
    					
    					reverseFiles.add(selection_cycle_model.getReverseReadsFile().get());
    					
    				}
    				
    			}
    			
    			isControl.add(selection_cycle_model.getIsControlCycle().get());
    			isCounter.add(selection_cycle_model.getIsCounterSelectionCycle().get());
    			
    			
    		}
    		
    		
    		utilities.Configuration.getParameters().addProperty("listtest", new String[] {"this" , "is", "a", "test"});
    		
    		
    		utilities.Configuration.writeConfiguration();
    		
    		try {
				actionHandler.navigate(Wizard2Controller.class);
			} catch (Exception e) {
				
				e.printStackTrace();
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
				
			}
    	}
    	else { // Else inform the user to correct the mistakes before clicking next again
    		
    		errorLabel.setVisible(true);
    		
    	}
    	
    }
    
    /**
     * Adds a new instance of the Selection Cycle Inputs to the VBOX
     * when the button is pressed
     */
    @FXML
    private void addSelectionCycleButtonAction() {
    	
    	SelectionCycleDataModel scdm = new SelectionCycleDataModel();
    	
    	// Instantiate the node
    	addSelectionCycle(scdm, true);
    	
    	// And store it in the data model
    	getDataModel().getSelectionCycleDataModels().add(scdm);
    	
    }
    
    
    /**
     * Adds a new instance of the Selection Cycle Inputs to the VBOX
     */
    private void addSelectionCycle(SelectionCycleDataModel scdm, boolean isNewCycle) {

    	// Create an instance of the node
    	TitledPane selectionCyclePane = null;
    	SelectionCycleDetailsController selectionCycleController = null;
    	
    	try {
    		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/selectionCycleDetails.fxml"));
			selectionCyclePane = (TitledPane) loader.load();
			selectionCycleController = loader.getController();
			
			// Make the corresponding data models available to the controller. Inject did not work for some reason
			selectionCycleController.setDataModel(getDataModel());
			selectionCycleController.setSelectionCycleDataModel(scdm);
			selectionCycleController.setParent(this);
			
			// Initialize any fields
			selectionCycleController.init(isNewCycle);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	// Add it to the scene
    	selectionCycleContainers.getChildren().add(selectionCyclePane);
    	selectionCycleDetailsControllers.add(selectionCycleController);
    	
    }
    
    
    
    /**
     * Removes the controller and any data associated with it from the wizard
     * @param controller
     */
    public void removeSelectionCycle(SelectionCycleDataModel model) {
    	
    	// First identify the index at which we need to delete the elements
    	int index_to_delete = getDataModel().getSelectionCycleDataModels().indexOf(model);
    	
    	// Now remove them from the view...
    	selectionCycleContainers.getChildren().remove(index_to_delete);
    	
    	// ...and the data model
    	getDataModel().getSelectionCycleDataModels().remove(index_to_delete);
    	
    	selectionCycleDetailsControllers.remove(index_to_delete);
    	
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