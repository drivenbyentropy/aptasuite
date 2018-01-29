/**
 * 
 */
package gui.core.experiment.overview;

import java.util.ArrayList;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;

import gui.activity.ProgressPaneController;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Spinner;
import javafx.scene.layout.StackPane;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Controls the charts displaying the pool composition over time
 */
public class ExperimentOverviewSelectionCycleCompositionController {

	@FXML
	BarChart<String,Number> positiveSelectionCyclesBarChart;
	
	@FXML
	BarChart<String,Number> negativeSelectionCyclesBarChart;
	
	@FXML
	BarChart<String,Number> controlSelectionCyclesBarChart;
	
	@FXML
	Spinner<Integer> singletonCountSpinner;
	
	@FXML
	StackPane selectionCycleCompositionStackPane;
	
	private ObjectProperty<Integer> singletonCount = new SimpleObjectProperty<Integer>(1);
	
	private Experiment experiment = Configuration.getExperiment(); 
	
	@PostConstruct
	public void initialize() {
		
		// Bind the spinner to a property object
		singletonCount.bindBidirectional(singletonCountSpinner.getValueFactory().valueProperty());
       
		updateCharts();
		
	}

	/**
	 * Regenerates the charts based on the new value of the spinner 
	 */
	@FXML
	private void updateCharts() {
		
		ProgressPaneController pp = ProgressPaneController.getProgressPane(new Runnable() {
			
			@Override
			public void run() {
				
				//Prepare the data to be computed
				ArrayList<SelectionCycle> counter_selections = new ArrayList<SelectionCycle>();
				for ( ArrayList<SelectionCycle> cycles : experiment.getCounterSelectionCycles()) {

					if (cycles != null) counter_selections.addAll(cycles);
				
				}

				ArrayList<SelectionCycle> control_selections = new ArrayList<SelectionCycle>();
				for ( ArrayList<SelectionCycle> cycles : experiment.getControlSelectionCycles()) {

					if (cycles != null) control_selections.addAll(cycles);
				
				}

				
				// Initialize the GUI elements
				Platform.runLater(() -> {
					
					createBarChart(experiment.getSelectionCycles(), positiveSelectionCyclesBarChart);
					
					// Only compute negative cycles if they are present
					if (!counter_selections.isEmpty()) {

						createBarChart(counter_selections, negativeSelectionCyclesBarChart);
						
					} else {
						
						negativeSelectionCyclesBarChart.setDisable(true);
						
					}
					
					
					if (!control_selections.isEmpty()) {

						createBarChart(control_selections, controlSelectionCyclesBarChart);
						
					} else {
						
						controlSelectionCyclesBarChart.setDisable(true);
						
					}
					
                });
				
			}
		
		}, selectionCycleCompositionStackPane);
		
		pp.run();
		
	}
	
	/**
	 * Creates the a barchart with the required content based on the cycles provided
	 * @param cycles
	 */
	private void createBarChart(ArrayList<SelectionCycle> cycles, BarChart<String,Number> barchart) {
		
		Series<String, Number> singleton_frequencies = new XYChart.Series();
		singleton_frequencies.setName("Singletons");
		
		Series<String, Number> enriched_frequencies = new XYChart.Series();
		enriched_frequencies.setName("Enriched Species");
		
		Series<String, Number> unique_fraction = new XYChart.Series();
		unique_fraction.setName("Unique Fraction");
		
		for (SelectionCycle cycle : cycles) {
			
			// Skip non-existing cycles
			if (cycle == null) continue;
			
			String cycle_label = String.format("Round %s (%s)", cycle.getRound(), cycle.getName()); 
			
			// Unique fraction is the ratio of unique sequences and the total pool size
			Number unique_fraction_value = (cycle.getUniqueSize() / (double) cycle.getSize()) * 100; 
			unique_fraction.getData().add(new XYChart.Data<String,Number>( cycle_label , unique_fraction_value));
			
			// The singleton frequency is the number of aptamers with a count <= singletonCount normalized by the total number of unique aptamers in the pool
			// Enriched corresponds to those with a count > singletonCount normalized by the total number of unique aptamers
			int singleton_count = 0;
			int enriched_count = 0;
			int cutoff = this.singletonCount.get();
			
			for ( Entry<Integer, Integer> aptamer : cycle.iterator()) {
				
				if (aptamer.getValue() > cutoff) { enriched_count++; }
				else { singleton_count++; }
				
			}
			
			Number singleton_count_value = (singleton_count / (double) cycle.getUniqueSize()) * 100;
			singleton_frequencies.getData().add(new XYChart.Data<String,Number>( cycle_label , singleton_count_value));
			
			Number enriched_count_value = (enriched_count / (double) cycle.getUniqueSize()) * 100;
			enriched_frequencies.getData().add(new XYChart.Data<String,Number>( cycle_label , enriched_count_value));
			
		}
		
		barchart.getData().clear();
		barchart.getData().addAll(singleton_frequencies, enriched_frequencies, unique_fraction);
		
	}
	
}
