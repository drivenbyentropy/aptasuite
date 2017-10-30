/**
 * 
 */
package gui.wizards.newexperiment;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import gui.misc.ControlFXValidatorFactory;

/**
 * @author Jan Hoinka
 * Implements the controller logic for the selection cycle
 * details the user has to create when defining what is con-
 * tained in the data.
 */
public class SelectionCycleDetailsController {

	@FXML
	private TextField roundNameTextField;
	
	@FXML
	private TitledPane containerTitledPane;
	
	@FXML
	private Spinner<Integer> roundNumberSpinner;
	
	@FXML
	private TextField forwardReadsFileTextField;
	
	@FXML
	private TextField reverseReadsFileTextField;
	
	@FXML
	private TextField barcodeTextField;
	
	@FXML
	private CheckBox isControlCycleCheckBox;
	
	@FXML
	private CheckBox isCounterSelectionCycleCheckBox;
	
	@FXML
	private HBox forwardReadFileChooserHBox;
	
	@FXML
	private HBox reverseReadFileChooserHBox;
	
	@FXML 
	private HBox barcodeHBox;
	
	/**
	 * The container holding the data for this selection cycle
	 */
	private SelectionCycleDataModel selectionCycleDataModel;
	
	/**
	 * The container holding the data for the entire wizard
	 */
	private DataModel dataModel;
	
	/**
	 * We need the parent to inform it of the delete operation
	 */
	private Wizard1Controller parent;
	
	/**
     * Validation Support to ensure correct user input
     */
    private ValidationSupport validationSupport = new ValidationSupport();
	
    /**
     * Sets default parameters. Must be called after constructor and after setting the data models
     */
    public void init() {
		
		// Set up the spinner, we will set the initial values according 
		// to SelectionCycles already defined. set the round number to previous plus 1
		if (!dataModel.getSelectionCycleDetailControllers().isEmpty()) {

			roundNumberSpinner.getValueFactory().setValue(
					dataModel.getSelectionCycleDetailControllers()
					.get(dataModel.getSelectionCycleDetailControllers().size()-1)
					.getSelectionCycleDataModel()
					.getRoundNumber().get()+1
					);
			
		}
		
		// Bind the title to a combination of round number and round name
		//containerTitledPane.textProperty().bind(Bindings.concat(this.roundNameTextField.textProperty(), " (Cycle # ",this.roundNumberSpinner.getValueFactory().valueProperty(), ")"));
		containerTitledPane.textProperty().bind(
				Bindings.when(this.roundNameTextField.textProperty().isNotEqualTo(""))
				.then(Bindings.concat(this.roundNameTextField.textProperty(), " (Cycle # ",this.roundNumberSpinner.getValueFactory().valueProperty(), ")"))
				.otherwise("Untitled SELEX Cycle")
				);

		
		// Make sure we disable the file choosers if we have multiplexed data
		// The same is true for single end
		forwardReadFileChooserHBox.disableProperty().bind(dataModel.getIsDemultiplexed().not());
		reverseReadFileChooserHBox.disableProperty().bind(
				Bindings.when(dataModel.getIsDemultiplexed())
					.then(
							Bindings.when(dataModel.getIsPairedEnd())
							.then(false)
							.otherwise(true)
						)
					.otherwise(true)
				); 
		
		// Same for the barcode
		barcodeHBox.disableProperty().bind(dataModel.getIsDemultiplexed());
		
		
		// Bind the content to the data model
		selectionCycleDataModel.getRoundNumber().bind(roundNumberSpinner.getValueFactory().valueProperty());
		selectionCycleDataModel.getRoundName().bind(roundNameTextField.textProperty());
		
		selectionCycleDataModel.getForwardReadsFile().bind(forwardReadsFileTextField.textProperty());
		selectionCycleDataModel.getReverseReadsFile().bind(reverseReadsFileTextField.textProperty());
		
		selectionCycleDataModel.getBarcode().bind(roundNameTextField.textProperty());
		selectionCycleDataModel.getRoundName().bind(roundNameTextField.textProperty());
		selectionCycleDataModel.getRoundName().bind(roundNameTextField.textProperty());
		
		selectionCycleDataModel.getIsControlCycle().bind(this.isControlCycleCheckBox.selectedProperty());
		selectionCycleDataModel.getIsCounterSelectionCycle().bind(this.isCounterSelectionCycleCheckBox.selectedProperty());
	
		// Add validation to the field to make sure we have correct user input
		validationSupport.registerValidator(roundNameTextField, Validator.createEmptyValidator("The round name cannot be empty"));

		// Forward and reverse reads
		validationSupport.registerValidator(forwardReadsFileTextField, Validator.createEmptyValidator("The forward read file must be specified"));
		if (getDataModel().getIsPairedEnd().get()) {
			validationSupport.registerValidator(forwardReadsFileTextField, Validator.createEmptyValidator("The reverse read file must be specified"));
		}
		
		// Barcode
		if (getDataModel().getIsDemultiplexed().not().get()) {
			validationSupport.registerValidator(barcodeTextField, ControlFXValidatorFactory.DNAStringValidator);
		}
		barcodeTextField.textProperty().addListener((ov, oldValue, newValue) -> { barcodeTextField.setText(newValue.toUpperCase());	});
		
    }

    /**
     * Removes the selection cycle and any associated data from the view and data model
     */
    @FXML
    private void deleteCycleActionButton() {
    	
    	parent.removeSelectionCycle(this);
    	
    }
    
    /**
     * Sets the forward read file path
     * @param event
     */
    @FXML
    private void chooseForwardReadFileButtonAction(ActionEvent event) {

    	chooseInputFile(forwardReadsFileTextField);
    	
    }
    
    /**
     * Sets the reverse read file path
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
    
	/**
	 * @return the roundNumberSpinner
	 */
	public Spinner<Integer> getRoundNumberSpinner() {
		return roundNumberSpinner;
	}

	/**
	 * @return the selectionCycleDataModel
	 */
	public SelectionCycleDataModel getSelectionCycleDataModel() {
		return selectionCycleDataModel;
	}

	/**
	 * @param selectionCycleDataModel the selectionCycleDataModel to set
	 */
	public void setSelectionCycleDataModel(SelectionCycleDataModel selectionCycleDataModel) {
		this.selectionCycleDataModel = selectionCycleDataModel;
	}

	/**
	 * @return the dataModel
	 */
	public DataModel getDataModel() {
		return dataModel;
	}

	/**
	 * @param dataModel the dataModel to set
	 */
	public void setDataModel(DataModel dataModel) {
		this.dataModel = dataModel;
	}

	/**
	 * @return the parent
	 */
	public Wizard1Controller getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Wizard1Controller parent) {
		this.parent = parent;
	}
	
}
