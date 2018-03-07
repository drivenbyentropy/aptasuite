/**
 * 
 */
package gui.core.experiment.overview;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import javafx.fxml.FXML;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.RadioButton;
import lib.aptamer.datastructures.Experiment;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Controls the chart displaying the randomized region size distribution
 */
public class ExperimentOverviewRandomizedRegionSizeDistributionController {

	@FXML
	BarChart<String,Number> randomizedRegionSizeDistributionBarChart;
	
	@FXML
	RadioButton percentageRadioButton;
	
	@FXML
	RadioButton logarithmicRadioButton;
	
	private Experiment experiment = Configuration.getExperiment(); 
	
	/**
	 * Contains the orignial data for the barchart
	 */
	private Series<String, Number> chartData;
	
	/**
	 * Total number of items, for normalization
	 */
	private Integer total = 0;
	
	@PostConstruct
	public void initialize() {
		
        // Store the data to be used
		chartData = computeStatistics();
		
		// Add initial data to barchart
        randomizedRegionSizeDistributionBarChart.getData().add(chartData);

	}

	
	/**
	 * Calculates the randomized region size distribution over all selection cycles
	 */
	private XYChart.Series<String,Number> computeStatistics(){
		
		HashMap<Integer, Integer> totals = new HashMap<Integer, Integer>();
		
		for ( Entry<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>>cycle : experiment.getMetadata().nucleotideDistributionAccepted.entrySet()) {
			
			for (Entry<Integer, Integer> cache_data : getStatisticsForSelectionCycle(cycle.getKey()).entrySet()) {
				
				if (totals.containsKey(cache_data.getKey())) {
					
					totals.put(cache_data.getKey(), totals.get(cache_data.getKey()) + cache_data.getValue());
					
				}
				else {
					
					totals.put(cache_data.getKey(), cache_data.getValue());
					
				}
				
			}
			
		}
		
		// Get the largest size (but at least 10) and pad with zeros and 5 to the right
		int maximum = Math.max( 10, totals.keySet().stream().max( (x,y) -> Integer.compare(x, y) ).get() ) + 5; 
		
		for (int x=0; x<maximum; x++) {
			
			if (!totals.containsKey(x)) {
				
				totals.put(x, 0);
				
			}
			
		}
		
		// Create the dataset for plotting
		XYChart.Series<String,Number> chart_data = new XYChart.Series(); 
		chart_data.setName("Randomized Region Sizes in the Aptamer Pool");
		for ( int x=0; x<maximum; x++) {
			
			chart_data.getData().add(new XYChart.Data<String,Number>(x+"", totals.get(x)));
			total += totals.get(x);
		}
		
		return chart_data;
		
	}
	
	
	
	private HashMap<Integer,Integer> getStatisticsForSelectionCycle(String cycle){
		
//		Extract the required information from the metadata
		HashMap<Integer, Integer> cache = new HashMap<Integer, Integer>();
		
		for ( Entry<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> rand_size : experiment.getMetadata().nucleotideDistributionAccepted.get(cycle).entrySet() ) {
			
			// Take position 0 and sum up the counts of the nucleotides
			int sum = 0;
			
			for ( Entry<Byte, Integer> nucleotide : rand_size.getValue().get(0).entrySet() ) {
				
				sum += nucleotide.getValue();
				
			}
			
			// Add it to the cache
			cache.put(rand_size.getKey(), sum);
			
		}
		
		
		return cache;
	}
	
	/**
	 * Updates the bar chart according to the selection of the radio buttons 
	 */
	@FXML
	private void updateBarChart() { 
		
		XYChart.Series<String,Number> chart_data = new XYChart.Series(); 
		chart_data.setName("Randomized Region Sizes in the Aptamer Pool");
		
		// Iterate over the original data and adjust
		for ( Data<String, Number> item : this.chartData.getData() ) {
			
			Number value = item.getYValue();
			
			// Units 
			if (this.percentageRadioButton.selectedProperty().get()) {
				
				value = value.doubleValue() / total;
				
			}
			
			// Scale 
			if (this.logarithmicRadioButton.selectedProperty().get()) {
				
				value = Math.log(value.doubleValue()+1);
				
			}
			
			chart_data.getData().add(new XYChart.Data<String,Number>(item.getXValue(), value));
			
		}
		
		// Adjust axis labels
		// Units 
		if (this.percentageRadioButton.selectedProperty().get()) {
			
			randomizedRegionSizeDistributionBarChart.getYAxis().setLabel("Percentage of Occurence");
			
		} else {
			
			randomizedRegionSizeDistributionBarChart.getYAxis().setLabel("Frequency of Occurence");
			
		}
		
		// Scale 
		if (this.logarithmicRadioButton.selectedProperty().get()) {
			
			randomizedRegionSizeDistributionBarChart.getYAxis().setLabel( randomizedRegionSizeDistributionBarChart.getYAxis().getLabel() + " ( log(x + 1) )");
			
		}
		
		// Set the new data
		randomizedRegionSizeDistributionBarChart.getData().clear();
		randomizedRegionSizeDistributionBarChart.getData().add(chart_data);
	}
	
}
