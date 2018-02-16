/**
 * 
 */
package gui.wizards.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import exceptions.InformationNotFoundException;
import exceptions.InvalidConfigurationException;
import gui.activity.ProgressPaneController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.export.Export;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * GUI controller for the export window
 */
public class ExportWizardController {

	@FXML
	private CheckBox exportSequencesCheckBox;
	
	@FXML
	private CheckBox exportStructureInformationCheckBox;
	
	@FXML 
	private ComboBox<String> poolCardinalityFormatComboBox;
	
	@FXML
	private ListView<CheckBox> selectionCycleListView;
	
	@FXML
	private CheckBox exportClustersCheckBox;
	
	@FXML
	private TextField minimalClusterSizeTextField;
	
	@FXML
	private ComboBox<String> clusterFilterCriteriaComboBox;
	
	@FXML
	private CheckBox compressFilesCheckbox; 
	
	@FXML
	private CheckBox withPrimersCheckBox;

	@FXML
	private Label clusterFilterCriteriaLabel;
	
	@FXML
	private Label minimalClusterSizeLabel;
	
	@FXML
	private ComboBox<String> sequenceFormatComboBox;
	
	@FXML
	private StackPane rootStackPane;
	
	@FXML
	private Button closeButton;
	
	Experiment experiment = Configuration.getExperiment();
	
	@PostConstruct
	public void initialize() {

		// Set values for the Cardinaliy Format ComboBox
		this.poolCardinalityFormatComboBox.getItems().add("Counts");
		this.poolCardinalityFormatComboBox.getItems().add("Frequencies");
		String pcfcbDefault = Configuration.getParameters().getString("Export.PoolCardinalityFormat", "Frequencies");
		this.poolCardinalityFormatComboBox.setValue(pcfcbDefault.substring(0, 1).toUpperCase() + pcfcbDefault.substring(1));

		// Fill all selection cycles
		ArrayList<CheckBox> items = new ArrayList<CheckBox>();
		for ( SelectionCycle cycle : experiment.getAllSelectionCycles()) {
			
			CheckBox box = new CheckBox(cycle.getName());
			items.add(box);
			
		}
		selectionCycleListView.getItems().addAll(items);

		// Cluster Filter Criteria
		clusterFilterCriteriaComboBox.getItems().add("ClusterDiversity");
		clusterFilterCriteriaComboBox.getItems().add("ClusterSize");
		String cfccb = Configuration.getParameters().getString("Export.ClusterFilterCriteria", "ClusterSize");
		clusterFilterCriteriaComboBox.setValue(cfccb);
		
		
		// Minimal Cluster Size
		minimalClusterSizeTextField.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, 
		        String newValue) {
		        if (!newValue.matches("\\d*")) {
		        	minimalClusterSizeTextField.setText(newValue.replaceAll("[^\\d]", ""));
		        }
		    }
		});
		minimalClusterSizeTextField.setText("1");
		
		minimalClusterSizeTextField.disableProperty().bind(this.exportClustersCheckBox.selectedProperty().not());
		clusterFilterCriteriaComboBox.disableProperty().bind(this.exportClustersCheckBox.selectedProperty().not());
		clusterFilterCriteriaLabel.disableProperty().bind(this.exportClustersCheckBox.selectedProperty().not());
		minimalClusterSizeLabel.disableProperty().bind(this.exportClustersCheckBox.selectedProperty().not());
		
		//Sequence Format Options
		sequenceFormatComboBox.getItems().add("fastq");
		sequenceFormatComboBox.getItems().add("fasta");
		sequenceFormatComboBox.getItems().add("raw");
		sequenceFormatComboBox.setValue(Configuration.getParameters().getString("Export.SequenceFormat","fastq"));
	}
	
	
	/**
	 * Checks input for correctness, writes to the contig file and exports the data
	 */
	@FXML
	private void exportDataButtonAction() {
		
		Runnable logic = new Runnable() {

			@Override
			public void run() {
				
				// Prevent the user from interrupting the process
				closeButton.setDisable(true);
				
				AptaLogger.log(Level.INFO, this.getClass(), "Starting Data Export");
				
				// Make sure the export folder exists and create it if not
				Path exportPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "export");
				if (Files.notExists(exportPath)){
						AptaLogger.log(Level.INFO, this.getClass(), "The export path does not exist on the file system. Creating folder " + exportPath);
						try {
							Files.createDirectories(Paths.get(exportPath.toString()));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
				
				Export export = new Export();
				Boolean compress = compressFilesCheckbox.isSelected();
				String extension = sequenceFormatComboBox.getSelectionModel().getSelectedItem() + (compress ? ".gz" : "");
				
				Configuration.getParameters().setProperty("Export.compress", compress);
				
				// Export Pool	
				if (exportSequencesCheckBox.isSelected()) {
				
					Path poolexportpath = Paths.get(exportPath.toString(), "pool.txt" + (compress ? ".gz" : ""));
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting pool data to file " + poolexportpath.toString());
					export.Pool(Configuration.getExperiment().getAptamerPool(), poolexportpath);
				
				}
				
				// Export Cycles
				ArrayList<SelectionCycle> cycles_to_export = getSelectedCycles();
				if (cycles_to_export.size() != 0) {
							
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting selection cycle data");
					// Run the export
					for (SelectionCycle sc : cycles_to_export){
						Path cycleexportpath = Paths.get(exportPath.toString(), sc.getName() + "." + extension);
						AptaLogger.log(Level.INFO, this.getClass(), "Exporting selection cycle " + sc.getName() + " to file " + cycleexportpath.toString());
						export.Cycle(sc, cycleexportpath );
					}
							
				}
				
				if (exportClustersCheckBox.isSelected()) {
							
							AptaLogger.log(Level.INFO, this.getClass(), "Exporting clusters");
							
							// Get the instance of the StructurePool
							try {
								if (experiment.getClusterContainer() == null)
								{
									experiment.instantiateClusterContainer(false, true);
								}	
								
								// Now run the export
								for (SelectionCycle sc : cycles_to_export){
									Path cycleexportpath = Paths.get(exportPath.toString(), "clusters_" + sc.getName() + "." + extension);
									AptaLogger.log(Level.INFO, this.getClass(), "Exporting clusters of selection cycle " + sc.getName() + " to file " + cycleexportpath.toString());
									export.Clusters(sc, experiment.getClusterContainer(), cycleexportpath );
								}
								
							} catch(Exception e) { // We need to make sure a cluster pool exists
								
								AptaLogger.log(Level.WARNING, this.getClass(), new InformationNotFoundException("No cluster information was found to export. Did you run AptaCLUSTER?"));
								AptaLogger.log(Level.FINEST, this.getClass(), new InformationNotFoundException("No cluster information was found to export. Did you run AptaCLUSTER?"));
							}
							
				}
				
				if (exportStructureInformationCheckBox.isSelected()) {
							
					AptaLogger.log(Level.INFO, this.getClass(), "Exporting structure data");
					
					//Make sure we have structures
					try {
						if (experiment.getStructurePool() == null)
						{
							experiment.instantiateStructurePool(false, true);
						}
						
						export.Structures(Configuration.getExperiment().getStructurePool(), Paths.get(exportPath.toString(), "structures.txt" + (compress ? ".gz" : "")));
						
					} catch(Exception e) { // We need to make sure a cluster pool exists
						
						AptaLogger.log(Level.WARNING, this.getClass(), new InformationNotFoundException("No structure information was found to export. Did you run AptaSUITE with the -structures option?"));
						AptaLogger.log(Level.FINEST, this.getClass(), new InformationNotFoundException("No structure information was found to export. Did you run AptaSUITE with the -structures option?"));
						
					}
				}

				// Re enable the close button
				closeButton.setDisable(false);
				
			}
			
		};
		
		
		ProgressPaneController pp = ProgressPaneController.getProgressPane(logic, rootStackPane);
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	/**
	 * Compiles a list of selection cycles which the user has selected in the GUI
	 * @return a list of selected cycles to export
	 * 
	 */
	private ArrayList<SelectionCycle> getSelectedCycles(){
		
		ArrayList<SelectionCycle> cycles = new ArrayList<SelectionCycle>();
		for ( CheckBox box : this.selectionCycleListView.getItems()) {
			
			if (box.isSelected()) {
				
				cycles.add(experiment.getSelectionCycleById(box.getText()));
				
			}
			
		}
		
		return cycles;
	}
	
	
	@FXML
	private void closeButtonAction() {
		
		// get a handle to the stage
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // close it
        stage.close();
		
	}
	
}
