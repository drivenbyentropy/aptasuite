/**
 * 
 */
package gui.core.sequencing.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 */
public class SequencingDataRootController {

	@FXML
	VBox selectionCyclesVBox;
	
	@FXML
	RadioButton countRadioButton;
	
	@FXML
	RadioButton percentageRadioButton;
	
	@FXML
	ComboBox<Integer> randomizedRegionSizeComboBox;
	
	@FXML
	RadioButton linearRadioButton;
	
	@FXML
	RadioButton logarithmicRadioButton;
	
	
	private Experiment experiment = Configuration.getExperiment();
	
	/**
	 * Keep record of the controllers
	 */
	private ObservableList<SequencingDataCycleController> cycle_controllers = FXCollections.observableArrayList( new ArrayList<SequencingDataCycleController>() );
	
	@PostConstruct
	public void initialize() {
		
		// Initialize the control bar
		initializeControlBar();
		
		// Iterate over the Selection cycles and instantiate the controllers		
		ArrayList<SelectionCycle> positive = experiment.getSelectionCycles();
		ArrayList<ArrayList<SelectionCycle>> negative = experiment.getCounterSelectionCycles();
		ArrayList<ArrayList<SelectionCycle>> control = experiment.getControlSelectionCycles();
		
		int number_of_cycles = experiment.getSelectionCycles().size();
		for (int x=0; x<number_of_cycles; x++) {
			
			// Skip non-existing cycles
			if (positive.get(x) == null && negative.get(x) == null && control.get(x) == null) continue;
			
			//TODO: ADD SPACER HERE
			
			// Take care of the positive selections
			if (positive.get(x) != null) {
				
				SelectionCycle cycle = positive.get(x);
				
				FXMLLoader loader = new FXMLLoader(getClass().getResource("SequencingDataCycle.fxml"));
				
				try {
					
					TitledPane pane = (TitledPane) loader.load();
					pane.setText(String.format("Selection Cycle %s: %s (Positive Selection)", cycle.getRound(), cycle.getName()));
					
					SequencingDataCycleController paneController = loader.getController();
					paneController.setContent(cycle, countRadioButton, percentageRadioButton, linearRadioButton, logarithmicRadioButton, randomizedRegionSizeComboBox);
					cycle_controllers.add(paneController);

					VBox.setVgrow(pane, Priority.ALWAYS);
					selectionCyclesVBox.getChildren().add(pane);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
			
			if (negative.get(x) != null) {
				
				for ( SelectionCycle cycle : negative.get(x)) {
					
					FXMLLoader loader = new FXMLLoader(getClass().getResource("SequencingDataCycle.fxml"));
					
					try {
						
						TitledPane pane = (TitledPane) loader.load();
						pane.setText(String.format("Selection Cycle %s: %s (Negative Selection)", cycle.getRound(), cycle.getName()));
						pane.setExpanded(false);
						
						SequencingDataCycleController paneController = loader.getController();
						paneController.setContent(cycle, countRadioButton, percentageRadioButton, linearRadioButton, logarithmicRadioButton, randomizedRegionSizeComboBox);
						cycle_controllers.add(paneController);
						
						VBox.setVgrow(pane, Priority.ALWAYS);
						selectionCyclesVBox.getChildren().add(pane);
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			
			}
			
			if (control.get(x) != null) {
				
				for ( SelectionCycle cycle : control.get(x)) {
					
					FXMLLoader loader = new FXMLLoader(getClass().getResource("SequencingDataCycle.fxml"));
					
					try {
						
						TitledPane pane = (TitledPane) loader.load();
						pane.setText(String.format("Selection Cycle %s: %s (Control Selection)", cycle.getRound(), cycle.getName()));
						pane.setExpanded(false);
						
						SequencingDataCycleController paneController = loader.getController();
						paneController.setContent(cycle, countRadioButton, percentageRadioButton, linearRadioButton, logarithmicRadioButton, randomizedRegionSizeComboBox);
						cycle_controllers.add(paneController);
						
						VBox.setVgrow(pane, Priority.ALWAYS);
						selectionCyclesVBox.getChildren().add(pane);
						
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			
			}
			
			//TODO: ADD SPACER HERE
			if (x+1<number_of_cycles) {
				
				Separator sep = new Separator();
				sep.setMinHeight(15);
				selectionCyclesVBox.getChildren().add(sep);
				
			}
			
			
		}
		
		
		
	}
	
	
	/**
	 * Initializes any nodes in the control bar which have dynamic content
	 */
	public void initializeControlBar() {
		
		// Determine all possible randomized region sizes from the selection cycles
		Set<Integer> sizes = new HashSet<Integer>();
		Map<Integer,Integer> maxima = new HashMap<Integer,Integer>();
		for (  Entry<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>> cycle : experiment.getMetadata().nucleotideDistributionAccepted.entrySet() ) {
			
			for ( Entry<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> size : cycle.getValue().entrySet()) {
				
				sizes.add(size.getKey());
				
				maxima.putIfAbsent(size.getKey(), 0);
				maxima.put(size.getKey(), maxima.get(size.getKey()) + size.getValue().get(0).get((byte) 'A') + size.getValue().get(0).get((byte) 'C') + size.getValue().get(0).get((byte) 'G') + size.getValue().get(0).get((byte) 'T') );
				
			}
			
		}
		
		// Set possible values
		sizes.stream().sorted().forEach(e -> this.randomizedRegionSizeComboBox.getItems().add(e) ); 
			
		// Set default
		this.randomizedRegionSizeComboBox.setValue( maxima.entrySet().stream().max( Comparator.comparing( e -> e.getValue() )).get().getKey() );
	}
	
	/**
	 * Informs the child nodes to redraw the charts
	 */
	@FXML
	private void updateFrequencyCharts() {
		
		for ( SequencingDataCycleController controller : cycle_controllers) {
			
			controller.updateFrequencyCharts();
			controller.updateRandomizedRegionFrequencyCharts();
			controller.updateRandomizedRegionSizeDistributionCharts();
			
		}
		
	}
	
	
	/**
	 * Informs the child nodes to redraw the charts
	 */
	@FXML
	private void updateAcceptedReadsFrequencyCharts() {
		
		for ( SequencingDataCycleController controller : cycle_controllers) {
			
			controller.updateRandomizedRegionFrequencyCharts();
			
		}
		
	}
	
	
	/**
	 * Informs the child nodes to redraw the charts
	 */
	@FXML
	private void updateRandomizedRegionBarChart() {
		
		for ( SequencingDataCycleController controller : cycle_controllers) {
			
			controller.updateRandomizedRegionSizeDistributionCharts();
						
		}
		
	}
	
}
