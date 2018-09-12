package gui.wizards.newexperiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import gui.core.RootLayoutController;
import gui.misc.ControlFXValidatorFactory;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utilities.AptaLogger;

public class Wizard1Controller{

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
	private Spinner<Integer> randomizedRegionSizeLowerSpinner;
	
	@FXML
	private Spinner<Integer> randomizedRegionSizeUpperSpinner;
    
	@FXML
	private HBox actionBar;
	
	@FXML
	private ActionBarController actionBarController;
	
	@FXML
	private CheckBox exactLengthCheckBox;
	
	@FXML
	private CheckBox rangeLengthCheckBox;	
	
	
    private List<SelectionCycleDetailsController> selectionCycleDetailsControllers = new ArrayList<SelectionCycleDetailsController>();
    
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
    	
    	// Bind the data to the content
    	primer5TextField.textProperty().bindBidirectional(getDataModel().getPrimer5());
    	primer3TextField.textProperty().bindBidirectional(getDataModel().getPrimer3());
    	
    	randomizedRegionSizeSpinner.getValueFactory().valueProperty().bindBidirectional(getDataModel().getRandomizedRegionSize());
    	randomizedRegionSizeLowerSpinner.getValueFactory().valueProperty().bindBidirectional(getDataModel().getRandomizedRegionSizeLower());
    	randomizedRegionSizeUpperSpinner.getValueFactory().valueProperty().bindBidirectional(getDataModel().getRandomizedRegionSizeUpper());

    	// Make sure that either only the exact size or the range or non of them is selected
    	exactLengthCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
    	    if (isNowSelected) {
    	    	rangeLengthCheckBox.setSelected(false);
    	    }
    	});

    	rangeLengthCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
    	    if (isNowSelected) {
    	    	exactLengthCheckBox.setSelected(false);
    	    }
    	});
    	
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
		
    }
    
   
    /**
     * Makes sure that all data fields are correct and valid
     */
    public boolean validateData() {
    	
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
    	
    	// Make sure that if a range for the randomized region size is specified, it is a valid one
    	if (this.rangeLengthCheckBox.isSelected()) {
    		
    		int min = randomizedRegionSizeLowerSpinner.getValueFactory().getValue();
    		int max = randomizedRegionSizeUpperSpinner.getValueFactory().getValue();
    		
    		if (min == max || min == 0 || max == 0) {
    			
        		ControlFXValidatorFactory.setTemporaryValidation(
        				randomizedRegionSizeLowerSpinner, 
            			ControlFXValidatorFactory.AllwaysWrongValidator("Min cannot be equal to Max or zero"), 
            			this.validationSupport, 
            			ControlFXValidatorFactory.AllwaysCorrectValidator,
            			false
            			);

        		ControlFXValidatorFactory.setTemporaryValidation(
        				randomizedRegionSizeUpperSpinner, 
            			ControlFXValidatorFactory.AllwaysWrongValidator("Max cannot be equal to Min or zero"), 
            			this.validationSupport, 
            			ControlFXValidatorFactory.AllwaysCorrectValidator,
            			false
            			);
        		
        		is_valid = false;
    			
    		}
    		
    		if (min >= max) {
    			
        		ControlFXValidatorFactory.setTemporaryValidation(
        				randomizedRegionSizeLowerSpinner, 
            			ControlFXValidatorFactory.AllwaysWrongValidator("Min cannot be larger or equal to Max"), 
            			this.validationSupport, 
            			ControlFXValidatorFactory.AllwaysCorrectValidator,
            			false
            			);

        		ControlFXValidatorFactory.setTemporaryValidation(
        				randomizedRegionSizeUpperSpinner, 
            			ControlFXValidatorFactory.AllwaysWrongValidator("Max cannot be smaller than to Min"), 
            			this.validationSupport, 
            			ControlFXValidatorFactory.AllwaysCorrectValidator,
            			false
            			);
        		
        		is_valid = false;
    			
    		}
    		
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
    	
        	// Make sure there are no empty round names
        	if ( selection_cycle_controller.getSelectionCycleDataModel().getRoundName().isEmpty().get() ) {
        		
        		ControlFXValidatorFactory.setTemporaryValidation(
    					selection_cycle_controller.getRoundNameTextField(), 
            			ControlFXValidatorFactory.AllwaysWrongValidator("The round name must not be empty."), 
            			this.validationSupport, 
            			Validator.createEmptyValidator("Please specify a name for this round."),
            			false
            			);
			    			
    			is_valid = false;
        		
        	}
    		
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
    	
    	
    	
    	if(getDataModel().getIsDemultiplexed().not().get()) {
    		
	    	// Make sure that if the user specified a 3' primer barcode once, they specify it everywhere
	    	Map<Boolean, Long> num_true = hasBarcode3.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
	    	// Add 0 counts manually if required
	    	num_true.putIfAbsent(true, 0L);
	    	num_true.putIfAbsent(false, 0L);
	    	
	    	// If we have no data, we can skip the next step
	    	if (num_true != null) { 
	    	
		    	if (num_true.get(true) != 0 && num_true.get(true) != this.selectionCycleDetailsControllers.size()) {
		    	
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
    		
        	// Attempt to delete the folder if it exists. Note that this will fail on certain OSes if
        	// a file or folder is being accessed by a third party. In that case, the user must delete 
        	// the folder manually.
        	boolean success = false;
        	while (!success) {
        		
	        	try {
	        		
		        	if ( Files.exists(experiment_path) ) {
		        		
		        		deleteFolderContent(experiment_path.toFile(),experiment_path.toFile());
		        		
		        	} else {
		        		
		        		Files.createDirectories(experiment_path);
		        		
		        	}

	        		
	        	} catch (Exception e) {
					e.printStackTrace();
					AptaLogger.log(Level.SEVERE, this.getClass(), e);
					
					//Inform the user
					Alert alert = new Alert(AlertType.CONFIRMATION);
		    		alert.setTitle("Error in deleting existing folder.");
		    		alert.setHeaderText("The experiment folder " + experiment_path.toAbsolutePath() + " could not be deleted. \nPlease make sure that no third party applications are accessing the folder/files\nin that directory and click 'OK' to try again.");
		    		alert.setContentText("Error message: " + e.toString());

		    		Optional<ButtonType> result = alert.showAndWait();

		    		if (result.get() == ButtonType.OK){
			    		continue;
		    		}
		    		else { // Return to the previous screen
		    			
		    			return false;
		    			
		    		}
					
				}
        		
        		success = true;
        	
        	}
        	
    		// Write this configuration to file before going to the next screen
        	Path configuration_file = Paths.get(experiment_path.toAbsolutePath().toString(), "configuration.aptasuite");
    		utilities.Configuration.createConfiguration(configuration_file);
    		
    		// Experiment Name and Description
    		utilities.Configuration.getParameters().setProperty("Experiment.name", getDataModel().getExperimentName().get());
    		utilities.Configuration.getParameters().setProperty("Experiment.description", getDataModel().getExperimentDescription().get());
    		
    		// Project Path
    		utilities.Configuration.getParameters().setProperty("Experiment.projectPath", experiment_path.toAbsolutePath().toFile());
    		
    		// Primers and RR size
    		utilities.Configuration.getParameters().setProperty("Experiment.primer5", getDataModel().getPrimer5().get());
    		if (getDataModel().getPrimer3().isEmpty().not().get()) {
    		
    			utilities.Configuration.getParameters().setProperty("Experiment.primer3", getDataModel().getPrimer3().get());
    		
    		}
    		
    		if (this.exactLengthCheckBox.isSelected()) {
    			
    			utilities.Configuration.getParameters().setProperty("Experiment.randomizedRegionSize", getDataModel().getRandomizedRegionSize().get());
    			
    		}
    		
    		if (this.rangeLengthCheckBox.isSelected()) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.randomizedRegionSizeLowerBound", getDataModel().getRandomizedRegionSizeLower().get());
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.randomizedRegionSizeUpperBound", getDataModel().getRandomizedRegionSizeUpper().get());
    			
    		}
    		
    		// isPerFile
    		utilities.Configuration.getParameters().setProperty("AptaplexParser.isPerFile", getDataModel().getIsDemultiplexed().get());
    		
    		// File format
    		switch ( getDataModel().getFileFormat().get()) {
    		
    			case "FASTQ":	utilities.Configuration.getParameters().setProperty("AptaplexParser.reader", "FastqReader");
    							break;
    							
    			case "RAW":		utilities.Configuration.getParameters().setProperty("AptaplexParser.reader", "RawReader");
    							break;
    		}
    		
    		// Selection cycle details
    		List<String> names = new ArrayList<String>();
    		List<String> forwardFiles = new ArrayList<String>();
    		List<String> reverseFiles = new ArrayList<String>();
    		
    		List<String> barcodes5 = new ArrayList<String>();
    		List<String> barcodes3 = new ArrayList<String>();
    		
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
    			else {
    				
    				barcodes5.add(selection_cycle_model.getBarcode5().get());
    				if (selection_cycle_model.getBarcode3().isEmpty().not().get()) {
    					
    					barcodes3.add(selection_cycle_model.getBarcode3().get());
    					
    				}
    				
    			}
    			
    			isControl.add(selection_cycle_model.getIsControlCycle().get());
    			isCounter.add(selection_cycle_model.getIsCounterSelectionCycle().get());
    			
    		}
    		
    		utilities.Configuration.getParameters().setProperty("SelectionCycle.name", names);
    		utilities.Configuration.getParameters().setProperty("SelectionCycle.round", round);
    		utilities.Configuration.getParameters().setProperty("SelectionCycle.isControlSelection", isControl);
    		utilities.Configuration.getParameters().setProperty("SelectionCycle.isCounterSelection", isCounter);
    		
    		if (getDataModel().getIsDemultiplexed().get()) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.forwardFiles", forwardFiles);
    			if (getDataModel().getIsPairedEnd().get()) {
					
    				utilities.Configuration.getParameters().setProperty("AptaplexParser.reverseFiles", reverseFiles);
					
				}
    			
    		}
    		else {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.forwardFiles", getDataModel().getForwardReadsFile().get());
    			
    			if (getDataModel().getIsPairedEnd().get()) {
    				
    				utilities.Configuration.getParameters().setProperty("AptaplexParser.reverseFiles", getDataModel().getReverseReadsFile().get());
    				
    			}
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.barcodes5Prime", barcodes5);
    			if (barcodes3.size() == barcodes5.size()) {
    				utilities.Configuration.getParameters().setProperty("AptaplexParser.barcodes3Prime", barcodes3);
    			}
    			
    		}
    		
    		// DNA or RNA aptamers?
    		if (getDataModel().getStoreReverseComplement().get())
    		{
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.StoreReverseComplement", true);
    			
    		}
    		
    		// Finally, add changes from the advanced option scene
    		if (getDataModel().getMapDBAptamerPoolBloomFilterCapacity().get() != utilities.Configuration.getDefaults().getInt("MapDBAptamerPool.bloomFilterCapacity")) {
    			
    			utilities.Configuration.getParameters().setProperty("MapDBAptamerPool.bloomFilterCapacity", getDataModel().getMapDBAptamerPoolBloomFilterCapacity().get());
    			
    		}
    		
    		if (getDataModel().getMapDBAptamerPoolBloomFilterCollisionProbability().get() != utilities.Configuration.getDefaults().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability")) {
    			
    			utilities.Configuration.getParameters().setProperty("MapDBAptamerPool.bloomFilterCollisionProbability", getDataModel().getMapDBAptamerPoolBloomFilterCollisionProbability().get());
    			
    		}
    		
    		if (getDataModel().getMapDBAptamerPoolMaxTreeMapCapacity().get() != utilities.Configuration.getDefaults().getInt("MapDBAptamerPool.maxTreeMapCapacity")) {
    			
    			utilities.Configuration.getParameters().setProperty("MapDBAptamerPool.maxTreeMapCapacity", getDataModel().getMapDBAptamerPoolMaxTreeMapCapacity().get());
    			
    		}
    		
    		if (getDataModel().getMapDBSelectionCycleBloomFilterCollisionProbability().get() != utilities.Configuration.getDefaults().getDouble("MapDBSelectionCycle.bloomFilterCollisionProbability")) {
    			
    			utilities.Configuration.getParameters().setProperty("MapDBSelectionCycle.bloomFilterCollisionProbability", getDataModel().getMapDBSelectionCycleBloomFilterCollisionProbability().get());
    			
    		}
    		
    		if (getDataModel().getPerformanceMaxNumberOfCores().get() != utilities.Configuration.getDefaults().getInt("Performance.maxNumberOfCores")) {
    			
    			utilities.Configuration.getParameters().setProperty("Performance.maxNumberOfCores", getDataModel().getPerformanceMaxNumberOfCores().get());
    			
    		}
    		
    		if (getDataModel().getAptaplexParserPairedEndMinOverlap().get() != utilities.Configuration.getDefaults().getInt("AptaplexParser.PairedEndMinOverlap")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.PairedEndMinOverlap", getDataModel().getAptaplexParserPairedEndMinOverlap().get());
    			
    		}
    		
    		if (getDataModel().getAptaplexParserPairedEndMaxMutations().get() != utilities.Configuration.getDefaults().getInt("AptaplexParser.PairedEndMaxMutations")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.PairedEndMaxMutations", getDataModel().getAptaplexParserPairedEndMaxMutations().get());
    			
    		}
    		
    		if (getDataModel().getAptaplexParserPairedEndMaxScoreValue().get() != utilities.Configuration.getDefaults().getInt("AptaplexParser.PairedEndMaxScoreValue")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.PairedEndMaxScoreValue", getDataModel().getAptaplexParserPairedEndMaxScoreValue().get());
    			
    		}   		
    		
    		if (getDataModel().getAptaplexParserBarcodeTolerance().get() != utilities.Configuration.getDefaults().getInt("AptaplexParser.BarcodeTolerance")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.BarcodeTolerance", getDataModel().getAptaplexParserBarcodeTolerance().get());
    			
    		}   
    		
    		if (getDataModel().getAptaplexParserPrimerTolerance().get() != utilities.Configuration.getDefaults().getInt("AptaplexParser.PrimerTolerance")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.PrimerTolerance", getDataModel().getAptaplexParserPrimerTolerance().get());

    		}   
    		
    		if (getDataModel().getOnlyRandomizedRegionInData().get() != utilities.Configuration.getDefaults().getBoolean("AptaplexParser.OnlyRandomizedRegionInData")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.OnlyRandomizedRegionInData", getDataModel().getOnlyRandomizedRegionInData().get());

    		} 
    		
    		if (getDataModel().getUndeterminedToFile().get() != utilities.Configuration.getDefaults().getBoolean("AptaplexParser.UndeterminedToFile")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.UndeterminedToFile", getDataModel().getUndeterminedToFile().get());

    		}
    		
    		if (getDataModel().getCheckReverseComplement().get() != utilities.Configuration.getDefaults().getBoolean("AptaplexParser.CheckReverseComplement")) {
    			
    			utilities.Configuration.getParameters().setProperty("AptaplexParser.CheckReverseComplement", getDataModel().getCheckReverseComplement().get());

    		}
    		
    		// Save to file 
    		utilities.Configuration.writeConfiguration();
    		
			return true;
    	}
    	else { // Else inform the user to correct the mistakes before clicking next again
    		
    		this.actionBarController.getErrorLabel().setVisible(true);
    		
    		return false;
    		
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
    	if (getDataModel().getProjectPath().isNotEmpty().get()) {
    		fileChooser.setInitialDirectory(new File(getDataModel().getProjectPath().get()));
    	}
    	FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter("Sequencing Files", "*.fastq", "*.fastq.gz", "*.txt", "*.txt.gz");
    	fileChooser.getExtensionFilters().add(fileExtensions);
    	fileChooser.setInitialDirectory(new File(getDataModel().getLastSelectedDirectory().getAbsolutePath()));
    	File cfp = fileChooser.showOpenDialog(null);
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (cfp != null) {
    		
    		target.setText(cfp.getAbsolutePath());
    		getDataModel().setLastSelectedDirectory(cfp.getParentFile());
    		
    	}
    }

    
    /**
     * Deletes the content of a folder recursively without deleting the folder itself
     * @param file
     */
    private static void deleteFolderContent(File file, File rootfolder) {
        
    	//to end the recursive loop
        if (!file.exists())
            return;
         
        //if directory, go inside and call recursively
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                //call recursively
            	deleteFolderContent(f, rootfolder);
            }
        }
        //call delete to delete files and empty directory
        if (!file.equals(rootfolder)) {
        	file.delete();
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
    	
    	this.finishButton.setDisable(true);
        
    	// Next Action
        this.nextButton.setOnAction( (event)->{
        	
        	//make sure we are validated
        	if( validateData() ) {
	        	
	        	// Load the advanced option controller
	        	Parent root;
	            try {
	            																				
	            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/wizard2.fxml"));
	            	
	                root = loader.load();
	                Wizard2Controller controller = (Wizard2Controller) loader.getController();
	                
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
    	
    	// Back Action
        this.backButton.setOnAction( (event)->{
        	
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