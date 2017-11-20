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
import lib.aptamer.datastructures.Experiment;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Controlls the chart displaying the randomized region size distribution
 */
public class ExperimentOverviewRandomizedRegionSizeDistributionController {

	@FXML
	BarChart<String,Number> randomizedRegionSizeDistributionBarChart;
	
	private Experiment experiment = Configuration.getExperiment(); 
	
	@PostConstruct
	public void initialize() {
		
		// Create the barchart
		CategoryAxis xAxis = new CategoryAxis();
        final Axis<Number> yAxis = new NumberAxis();
        xAxis.setLabel("Randomized Region Size");       
        yAxis.setLabel("Frequency");
        
			
        // Add data
        randomizedRegionSizeDistributionBarChart.getData().add(computeStatistics());
        
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
		
		// Create the dataset for plotting
		XYChart.Series<String,Number> chart_data = new XYChart.Series(); 
		chart_data.setName("Randomized Region Sizes in the Aptamer Pool");
		for ( Entry<Integer, Integer> item : totals.entrySet()) {
			
			chart_data.getData().add(new XYChart.Data<String,Number>(item.getKey().toString(), 55));
			
			System.out.println(String.format("Key: %s, Value: %s", item.getKey().toString(), item.getValue()));
			
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
				System.out.println(nucleotide.getValue());
			}
			
			// Add it to the cache
			cache.put(rand_size.getKey(), sum);
			
		}
		
		
		return cache;
	}
	
	
}
