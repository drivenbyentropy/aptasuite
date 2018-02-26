/**
 * 
 */
package gui.wizards.aptasim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;

import gui.activity.ProgressPaneController;
import gui.core.RootLayoutController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.aptasim.AptaSimParser;
import lib.parser.aptasim.AptaSimProgress;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * The GUI controller for generating a simlulated dataset
 * using AptaSIM
 */
public class AptaSimWizardController {

	@FXML
	private TextField projectNameTextField;
	
	@FXML
	private TextField hmmFileTextField;
	
	@FXML
	private Button hmmChooseButton;
	
	@FXML
	private ComboBox<Integer> hmmDegreeComboBox;
	
	@FXML 
	private Spinner<Integer> radomizedRegionSizeSpinner;
	
	@FXML
	private Spinner<Integer> numberOfSelectionCycleSpinner;
	
	@FXML
	private TextField fivePrimePrimerTextField;
	
	@FXML
	private TextField threePrimePrimerTextField;
	
	@FXML
	private TextField numberOfInitialSequencesTextField;
	
	@FXML
	private TextField numberOfSeedsTextField;
	
	@FXML
	private Spinner<Integer> minimalSeedAffinitySpinner;
	
	@FXML
	private TextField maximalSequenceCountTextField;
	
	@FXML
	private Spinner<Integer> maximalSequenceAffinitySpinner;
	
	@FXML
	private Spinner<Integer> nucleotideDistributionASpinner;
	
	@FXML
	private Spinner<Integer> nucleotideDistributionCSpinner;
	
	@FXML
	private Spinner<Integer> nucleotideDistributionGSpinner;
	
	@FXML
	private Spinner<Integer> nucleotideDistributionTSpinner;
	
	@FXML 
	private ComboBox<Integer> selectionPercentageComboBox;
	
	@FXML
	private Spinner<Integer> baseMutationRateASpinner;
	
	@FXML
	private Spinner<Integer> baseMutationRateCSpinner;
	
	@FXML
	private Spinner<Integer> baseMutationRateGSpinner;
	
	@FXML
	private Spinner<Integer> baseMutationRateTSpinner;
	
	@FXML
	private ComboBox<Integer> mutationProbabilityComboBox;
	
	@FXML
	private ComboBox<Integer> amplificationEfficiencyComboBox;
	
	@FXML
	private TextField projectPathTextField;
	
	@FXML
	private Button runAptaSimButton;
	
	@FXML
	private StackPane inputStackPane;
	
	@FXML
	private Button cancelButton;
	
	@FXML
	private Button closeButton;
	
	@FXML
	private Label finishedLabel;
	
	private Path projectPath;
	
	private Experiment experiment;
	
	private Thread parserThread;
	
	private RootLayoutController rootLayoutController; 
	
	private Boolean was_interrupted = true;
	
	@PostConstruct
	public void initialize() {
		
		
		IntStream.rangeClosed(0, 5).boxed().collect(Collectors.toList());
		
		this.hmmDegreeComboBox.getItems().addAll( IntStream.rangeClosed(0, 5).boxed().collect(Collectors.toList()) );
		this.hmmDegreeComboBox.setValue( Configuration.getDefaults().getInt("Aptasim.HmmDegree"));
		
		SpinnerValueFactory<Integer> valueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 40);
		radomizedRegionSizeSpinner.setValueFactory(valueFactory);
		
		SpinnerValueFactory<Integer> numcycValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 10);
		numberOfSelectionCycleSpinner.setValueFactory(numcycValueFactory);
		
		this.fivePrimePrimerTextField.setText("ATACCAGCTTATTCAATT");
		this.threePrimePrimerTextField.setText("AGATAGTAAGTGGCAATCT");
		
		this.numberOfInitialSequencesTextField.setText(Configuration.getDefaults().getString("Aptasim.NumberOfSequences"));
		this.numberOfSeedsTextField.setText(Configuration.getDefaults().getString("Aptasim.NumberOfSeeds"));
		
		SpinnerValueFactory<Integer> minSeedValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, Configuration.getDefaults().getInt("Aptasim.MinSeedAffinity"));
		this.minimalSeedAffinitySpinner.setValueFactory(minSeedValueFactory);
		
		this.maximalSequenceCountTextField.setText( Configuration.getDefaults().getString("Aptasim.MaxSequenceCount") );
		
		SpinnerValueFactory<Integer> maxSequenceValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, Configuration.getDefaults().getInt("Aptasim.MaxSequenceAffinity"));
		this.maximalSequenceAffinitySpinner.setValueFactory(maxSequenceValueFactory);
		
		Double[] ndist = (Double[]) Configuration.getDefaults().getArray(Double.class ,"Aptasim.NucleotideDistribution");
		
		SpinnerValueFactory<Integer> nucdistAValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (ndist[0]*100));
		this.nucleotideDistributionASpinner.setValueFactory(nucdistAValueFactory);
		
		SpinnerValueFactory<Integer> nucdistCValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (ndist[1]*100));
		this.nucleotideDistributionCSpinner.setValueFactory(nucdistCValueFactory);
		
		SpinnerValueFactory<Integer> nucdistGValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (ndist[2]*100));
		this.nucleotideDistributionGSpinner.setValueFactory(nucdistGValueFactory);
		
		SpinnerValueFactory<Integer> nucdistTValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (ndist[3]*100));
		this.nucleotideDistributionTSpinner.setValueFactory(nucdistTValueFactory);
		
		selectionPercentageComboBox.getItems().addAll(IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList()));
		selectionPercentageComboBox.setValue((int) (Configuration.getDefaults().getDouble("Aptasim.SelectionPercentage")*100)); 
		
		
		
		Double[] bdist = (Double[]) Configuration.getDefaults().getArray(Double.class ,"Aptasim.BaseMutationRates");
		
		SpinnerValueFactory<Integer> basedistAValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (bdist[0]*100));
		this.baseMutationRateASpinner.setValueFactory(basedistAValueFactory);
		
		SpinnerValueFactory<Integer> basedistCValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (bdist[1]*100));
		this.baseMutationRateCSpinner.setValueFactory(basedistCValueFactory);
		
		SpinnerValueFactory<Integer> basedistGValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (bdist[2]*100));
		this.baseMutationRateGSpinner.setValueFactory(basedistGValueFactory);
		
		SpinnerValueFactory<Integer> basedistTValueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, (int) (bdist[3]*100));
		this.baseMutationRateTSpinner.setValueFactory(basedistTValueFactory);
		
		
		mutationProbabilityComboBox.getItems().addAll(IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList()));
		mutationProbabilityComboBox.setValue((int) (Configuration.getDefaults().getDouble("Aptasim.MutationProbability")*100));
	
		amplificationEfficiencyComboBox.getItems().addAll(IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList()));
		amplificationEfficiencyComboBox.setValue((int) (Configuration.getDefaults().getDouble("Aptasim.AmplificationEfficiency")*100));
		
		//Listeners 
		this.numberOfInitialSequencesTextField.textProperty().addListener(IntegerOnlyListener(numberOfInitialSequencesTextField));
		this.numberOfSeedsTextField.textProperty().addListener(IntegerOnlyListener(numberOfSeedsTextField));
		this.maximalSequenceCountTextField.textProperty().addListener(IntegerOnlyListener(maximalSequenceCountTextField));
		
		//Bindings
		this.hmmDegreeComboBox.disableProperty().bind(this.hmmFileTextField.textProperty().isEmpty());
		this.runAptaSimButton.disableProperty().bind(this.projectPathTextField.textProperty().isEmpty());
	
	}
	
	private ChangeListener<String> IntegerOnlyListener (TextField textField){
		
		return new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, 
		        String newValue) {
		        if (!newValue.matches("\\d*")) {
		            textField.setText(newValue.replaceAll("[^\\d]", ""));
		        }
		    }
		};
		
	}
	
	
	@FXML
	private void hmmTrainingFileChooserActionButton() {
		
    	// Get the configuration file path
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Choose the FASTQ file to train the HMM");
    	FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter("Sequencing Files", "*.fastq,*.txt");
    	fileChooser.getExtensionFilters().add(fileExtensions);
    	File cfp = fileChooser.showOpenDialog(null);
    	
    	// Load configuration unless the user has chosen not to complete the dialog
    	if (cfp != null) {
    		
    		hmmFileTextField.setText(cfp.getAbsolutePath());
    		
    	}
		
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
    		projectPath = selectedDirectory.toPath();
    		
    	}
    	
    }
    
	
	@FXML
	private void runAptaSim() {
		
		// We need to make sure the the user understands that overwriting an existing folder will 
    	// results in data loss
    	
    	// Create a safe string of the experiment name
    	String safe_experiment_name = this.projectNameTextField.getText().replaceAll("[^a-zA-Z0-9]+", "").trim();
    	Path experiment_path = Paths.get(this.projectPath.toString(), safe_experiment_name);
    	
    	// Check that the folder does not exist already and inform the user of the consequence
    	if (Files.exists(experiment_path)) {
    		
    		Alert alert = new Alert(AlertType.CONFIRMATION);
    		alert.setTitle("Overwrite Project?");
    		alert.setHeaderText("A folder with the experiment name already exists in the project path. \nAre you sure you want to overwrite it with a new experiment?");
    		alert.setContentText("Folder path: " + experiment_path.toAbsolutePath());

    		Optional<ButtonType> result = alert.showAndWait();

    		if (result.get() == ButtonType.CANCEL){
    			
    			return;
    			
			}
    		
    	}
		
		// Create configuration
		
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
	    			
	    			return;
	    			
	    		}
				
			}
    		
    		success = true;
    	
    	}
    	
		// Write this configuration to file before going to the next screen
    	Path configuration_file = Paths.get(experiment_path.toAbsolutePath().toString(), "configuration.aptasuite");
		utilities.Configuration.createConfiguration(configuration_file);
		
		// Experiment Name and Description
		utilities.Configuration.getParameters().setProperty("Experiment.name", projectNameTextField.getText());
		utilities.Configuration.getParameters().setProperty("Experiment.description", "Simulated dataset.");
		
		// Project Path
		utilities.Configuration.getParameters().setProperty("Experiment.projectPath", experiment_path.toAbsolutePath().toFile());
		
		// Primers and RR size
		utilities.Configuration.getParameters().setProperty("Experiment.primer5", fivePrimePrimerTextField.getText());
		utilities.Configuration.getParameters().setProperty("Experiment.primer3", threePrimePrimerTextField.getText());
		
		utilities.Configuration.getParameters().setProperty( "Aptasim.HmmDegree", hmmDegreeComboBox.getValue());
		utilities.Configuration.getParameters().setProperty( "Aptasim.RandomizedRegionSize", radomizedRegionSizeSpinner.getValue());
		utilities.Configuration.getParameters().setProperty( "Aptasim.NumberOfSequences", numberOfInitialSequencesTextField.getText());
		
		utilities.Configuration.getParameters().setProperty( "MapDBAptamerPool.bloomFilterCapacity", Integer.parseInt(numberOfInitialSequencesTextField.getText()) * numberOfSelectionCycleSpinner.getValue().intValue() );
		
		utilities.Configuration.getParameters().setProperty( "Aptasim.NumberOfSeeds", numberOfSeedsTextField.getText());
		utilities.Configuration.getParameters().setProperty( "Aptasim.MinSeedAffinity", minimalSeedAffinitySpinner.getValue());
		utilities.Configuration.getParameters().setProperty( "Aptasim.MaxSequenceCount", maximalSequenceCountTextField.getText());
		utilities.Configuration.getParameters().setProperty( "Aptasim.MaxSequenceAffinity", maximalSequenceAffinitySpinner.getValue() );
		
		Double[] nd = new Double[4];
		nd[0] = nucleotideDistributionASpinner.getValue().doubleValue() / 100.0;
		nd[1] = nucleotideDistributionCSpinner.getValue().doubleValue() / 100.0;
		nd[2] = nucleotideDistributionGSpinner.getValue().doubleValue() / 100.0;
		nd[3] = nucleotideDistributionTSpinner.getValue().doubleValue() / 100.0;
		utilities.Configuration.getParameters().setProperty( "Aptasim.NucleotideDistribution",  nd);
		
		utilities.Configuration.getParameters().setProperty( "Aptasim.SelectionPercentage", selectionPercentageComboBox.getValue().doubleValue() / 100.0);
		
		Double[] bmr = new Double[4];
		bmr[0] = baseMutationRateASpinner.getValue().doubleValue() / 100.0;
		bmr[1] = baseMutationRateCSpinner.getValue().doubleValue() / 100.0;
		bmr[2] = baseMutationRateGSpinner.getValue().doubleValue() / 100.0;
		bmr[3] = baseMutationRateTSpinner.getValue().doubleValue() / 100.0;
		utilities.Configuration.getParameters().setProperty( "Aptasim.BaseMutationRates", bmr);
		
		utilities.Configuration.getParameters().setProperty( "Aptasim.MutationProbability", mutationProbabilityComboBox.getValue().doubleValue()/100.0);
		utilities.Configuration.getParameters().setProperty( "Aptasim.AmplificationEfficiency", amplificationEfficiencyComboBox.getValue().doubleValue()/100.0);
		
		// Selection cycle details
		List<String> names = new ArrayList<String>();
		List<Integer> round = new ArrayList<Integer>();
		List<Boolean> isControl = new ArrayList<Boolean>();
		List<Boolean> isCounter = new ArrayList<Boolean>();

		int num_cycles = numberOfSelectionCycleSpinner.getValue().intValue();
		for ( int cycle = 0; cycle <= num_cycles; cycle++ ) {
			
			names.add("Cycle"+cycle);
			round.add(cycle);
			isControl.add(false);
			isCounter.add(false);
			
		}
		
		utilities.Configuration.getParameters().setProperty("SelectionCycle.name", names);
		utilities.Configuration.getParameters().setProperty("SelectionCycle.round", round);
		utilities.Configuration.getParameters().setProperty("SelectionCycle.isControlSelection", isControl);
		utilities.Configuration.getParameters().setProperty("SelectionCycle.isCounterSelection", isCounter);
		
		// Save to file
		utilities.Configuration.writeConfiguration();
    	
    	//Create experiment and run aptasim
		utilities.Configuration.setConfiguration(configuration_file.toAbsolutePath().toString());
    	

    	ProgressPaneController pp = ProgressPaneController.getProgressPane(null, this.inputStackPane);
    	
    	// Lock closing the window. 
    	this.closeButton.setDisable(true);
    	this.cancelButton.setDisable(false);
    	finishedLabel.setText("");
    	was_interrupted = false;
    	
    	Runnable logic = new Runnable() {

			@Override
			public void run() {
				
				AptaLogger.log(Level.CONFIG, getClass(), "Initializing Experiment");
				
				// Initialize the experiment
		    	experiment = new Experiment(configuration_file.toAbsolutePath().toString(), true);

				// Initialize the parser and run it in a thread
				AptaLogger.log(Level.CONFIG, getClass(), "Initializing AptaSIM");
				AptaSimParser parser = new AptaSimParser();

				parserThread = new Thread(parser, "AptaSIM Main");

				AptaLogger.log(Level.INFO, getClass(), "Starting Simulation:");
				long tParserStart = System.currentTimeMillis();
				parserThread.start();

				// we need to add a shutdown hook for the parserThread in case the
				// user presses ctl-c
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							if (parserThread != null) {

								parserThread.interrupt();
								parserThread.join();

							}
						} catch (InterruptedException e) {
							AptaLogger.log(Level.SEVERE, getClass(), "User interrupt on parserThread");
						}
					}
				});

				// Update progress to user
				while (parserThread.isAlive() && !parserThread.isInterrupted()) {
					try {
						// Update Gui Log
						pp.refreshLogMessage(parser.Progress().getProgress());

						// Update progress bar
						pp.setProgress(((AptaSimProgress) parser.Progress()).round.doubleValue() / num_cycles);
						pp.setProgressLabel(String.format("Completed %s of %s cycles", ((AptaSimProgress) parser.Progress()).round, num_cycles));
						
						// Once every half second should suffice
						Thread.sleep(500);
					} catch (InterruptedException ie) {
					}
				}
				
				// Print Simulation Statistics
				AptaLogger.log(Level.INFO, getClass(), "Simulation Statistics:");
				for (SelectionCycle c : Configuration.getExperiment().getSelectionCycles()){
					AptaLogger.log(Level.INFO, getClass(), 
							String.format(":%-20s Total Pool Size: %s\t Unique Pool Size: %s\t Diversity: %.4f %%", 
									c.getName(), 
									c.getSize(), 
									c.getUniqueSize(), 
									(c.getUniqueSize()/ new Double (c.getSize()) * 100 ) ));
				}
				
				
				// clean up
				parser = null;
				
				AptaLogger.log(Level.INFO, getClass(), String.format("Simulation completed in %s seconds.\n",
						((System.currentTimeMillis() - tParserStart) / 1000.0)));
				
				// allow to close the window since we are done
				Platform.runLater(() -> {
					closeButton.setDisable(false);
					cancelButton.setDisable(true);
					finishedLabel.setText("ApataSim simulation completed successfully.");
				});
				
			}
    		
    	};
    	
    	pp.setShowLogs(true);
    	pp.setLogic(logic);
    	pp.setShowProgressBar(true);
    	pp.run();
    	
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
	
    @FXML
    private void cancelButtonAction() {
    	
    	parserThread.interrupt();
		try {
			parserThread.join();
		} catch (InterruptedException e) {
		}
		this.experiment.close();
		
		// clean up old data if required
		try {
			FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		AptaLogger.log(Level.INFO, this.getClass(), "ApataSim simulation cancled by user.");
		
		finishedLabel.setText("ApataSim simulation cancled by user.");
		
		this.closeButton.setDisable(false);
		this.cancelButton.setDisable(true);
		was_interrupted = true;
		
    }
    
	@FXML
	public void closeWindow() {
		
		if (!was_interrupted) {
			
			this.rootLayoutController.setExperiment(experiment);
			this.rootLayoutController.showInitialTabs();
			
		}
		
		// get a handle to the stage
        Stage stage = (Stage) this.closeButton.getScene().getWindow();
        // close it
        stage.close();
		
	}
	
	public void setRootLayoutController(RootLayoutController rootLayoutController) {
		
		this.rootLayoutController = rootLayoutController;
		
	}
	
}
