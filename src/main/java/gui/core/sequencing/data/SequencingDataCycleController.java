/**
 * 
 */
package gui.core.sequencing.data;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import gui.activity.ProgressPaneController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 */
public class SequencingDataCycleController {

	@FXML
	private Label roundNumberLabel;
	
	@FXML
	private Label cycleIDLabel;
	
	@FXML
	private Label typeOfSelectionLabel;
	
	@FXML
	private Label barcode5Label;
	
	@FXML
	private Label barcode3Label;
	
	@FXML
	private Label poolSizeLabel;
	
	@FXML
	private Label uniqueFractionLabel;
	
	@FXML
	private Label baseDistributionLabel;
	
	@FXML
	private LineChart<String, Number> forwardReadsLineChart;
	
	@FXML
	private LineChart<String,Number> reverseReadsLineChart;
	
	@FXML
	private LineChart<String,Number> randomizedRegionLineChart;
	
	@FXML
	private BarChart<String,Number> randomizedRegionSizeDistributionBarChart;
	
	@FXML
	private RadioButton countRadioButton;
	
	@FXML
	private RadioButton percentageRadioButton;

	@FXML
	private RadioButton linearRadioButton;
	
	@FXML
	private RadioButton logarithmicRadioButton;
	
	@FXML
	private StackPane cycleInformationStackPane;
	
	@FXML
	private ComboBox<Integer> randomizedRegionSizeComboBox;
	
	@FXML
	private StackPane reverseReadsStackPane;
	
	@FXML
	private GridPane chartGridPane;
	
	/**
	 * The selection cycle the data corresponds to
	 */
	private SelectionCycle cycle;
	
	private Experiment experiment = Configuration.getExperiment();
	
	/**
	 * Loads the cycle specific information into the view
	 */
	public void setContent(SelectionCycle cycle, RadioButton countRadioButton, RadioButton percentageRadioButton, RadioButton linearRadioButton, RadioButton logarithmicRadioButton, ComboBox<Integer> randomizedRegionSizeComboBox) {
		
		this.cycle = cycle;
		this.countRadioButton = countRadioButton;
		this.percentageRadioButton = percentageRadioButton;
		this.randomizedRegionSizeComboBox = randomizedRegionSizeComboBox;
		this.linearRadioButton = linearRadioButton;
		this.logarithmicRadioButton = logarithmicRadioButton;
		
		// Update cycle info
		updateCycleInformation();
		
		// Update the charts
		updateForwardReadNucleotideDistribution();
		updateReverseReadNucleotideDistribution();
		updateRandomizedRegionFrequencyCharts();
		updateRandomizedRegionSizeDistribution();
		
	}
	
	/**
	 * Calculates the cycle information and updates the fields accordingly
	 */
	private void updateCycleInformation() {
		
		Runnable cycleInfoRunnable  = new Runnable(){
			 
		    @Override
		    public void run(){
				
				// Set the cycle information
		    	Platform.runLater(() -> {
		    		
		    		roundNumberLabel.setText(""+cycle.getRound());
					cycleIDLabel.setText(cycle.getName());
					typeOfSelectionLabel.setText(cycle.isControlSelection() ? "Control" : cycle.isCounterSelection() ? "Negative" : "Positive");
					barcode5Label.setText(cycle.getBarcodeFivePrime() == null ? "n/a" : new String(cycle.getBarcodeFivePrime()) );
					barcode3Label.setText(cycle.getBarcodeThreePrime() == null ? "n/a" : new String(cycle.getBarcodeThreePrime()) );
					poolSizeLabel.setText(""+cycle.getSize());
					uniqueFractionLabel.setText(String.format("%s (%.2f%%)", cycle.getUniqueSize(), (cycle.getUniqueSize()/(double)cycle.getSize())*100 ));

					
                });
								
				int as = 0;
				int cs = 0;
				int gs = 0;
				int ts = 0;
				
				for ( Entry<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> data : experiment.getMetadata().nucleotideDistributionAccepted.get(cycle.getName()).entrySet()) {
					
					for ( Entry<Integer, ConcurrentHashMap<Byte, Integer>> position : data.getValue().entrySet() ) {
						
							as += position.getValue().get((byte)'A');
							cs += position.getValue().get((byte)'C');
							gs += position.getValue().get((byte)'G');
							ts += position.getValue().get((byte)'T');
						
					}
					
				}
				
				double sum = as + cs + gs + ts;
				
				// create a temporary class so that we can pass non-final parameters to the runnable
				class BDLUpdate implements Runnable {

					int as;
					int cs;
					int gs;
					int ts;
					
					public BDLUpdate(int as, int cs, int gs, int ts) {
						
						this.as = as;
						this.cs = cs;
						this.gs = gs;
						this.ts = ts;
						
					}
					
					@Override
					public void run() {
						baseDistributionLabel.setText(String.format("A:%.2f C:%.2f\nG:%.2f T:%.2f", (as/sum)*100, (cs/sum)*100, (gs/sum)*100, (ts/sum)*100));
					}
					
				};
				
				Platform.runLater(new BDLUpdate(as,cs,gs,ts));
		    	
		    }
		    
		};
		
		// Start the computation using a progress bar
		ProgressPaneController pp = ProgressPaneController.getProgressPane(cycleInfoRunnable, cycleInformationStackPane);
		pp.setShowLogs(false);
		pp.run();

		
	}
	
	/**
	 * Creates the line chart for the forward read nucleotide distribution data
	 */
	private void updateForwardReadNucleotideDistribution() {
		
		forwardReadsLineChart.getXAxis().setLabel("Nucleotide Index");
		forwardReadsLineChart.getYAxis().setLabel(this.countRadioButton.selectedProperty().get() ? "Frequency" : "Percentage");
		
		XYChart.Series<String, Number> seriesA = new XYChart.Series<String, Number>();
		XYChart.Series<String, Number> seriesC = new XYChart.Series<String, Number>();
		XYChart.Series<String, Number> seriesG = new XYChart.Series<String, Number>();
		XYChart.Series<String, Number> seriesT = new XYChart.Series<String, Number>();
		
		seriesA.setName("A");
		seriesC.setName("C");
		seriesG.setName("G");
		seriesT.setName("T");
		
		for ( Entry<Integer, ConcurrentHashMap<Byte, Integer>> position : experiment.getMetadata().nucleotideDistributionForward.get(cycle.getName()).entrySet()) {
			
			Number a = position.getValue().get((byte) 'A' );
			if (this.percentageRadioButton.selectedProperty().get()) { a = a.doubleValue() / cycle.getSize(); a = a.doubleValue()*100.0; }
			seriesA.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), a) );
			
			Number c = position.getValue().get((byte) 'C' );
			if (this.percentageRadioButton.selectedProperty().get()) { c = c.doubleValue() / cycle.getSize(); c = c.doubleValue()*100.0;}
			seriesC.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), c) );
			
			Number g = position.getValue().get((byte) 'G' );
			if (this.percentageRadioButton.selectedProperty().get()) { g = g.doubleValue() / cycle.getSize(); g = g.doubleValue()*100.0;}
			seriesG.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), g) );
			
			Number t = position.getValue().get((byte) 'T' );
			if (this.percentageRadioButton.selectedProperty().get()) { t = t.doubleValue() / cycle.getSize(); t = t.doubleValue()*100.0;}
			seriesT.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), t) );
			
		}
		
		// Needs to be in this order because the color choice will be determined based on this order and the CSS file 
		this.forwardReadsLineChart.getData().clear();
		this.forwardReadsLineChart.getData().setAll(seriesA, seriesC, seriesG, seriesT);
		
	}
	
	/**
	 * Creates the line chart for the reverse reads nucleotide distribution data
	 */
	private void updateReverseReadNucleotideDistribution() {
		
		// Determine if we have paired end data
		if (experiment.getMetadata().nucleotideDistributionReverse.get(cycle.getName()).size() == 0) {
			
			//this.reverseReadsLineChart.setDisable(true);
			chartGridPane.getChildren().remove(this.reverseReadsStackPane);
			
		}
		else {
			
			reverseReadsLineChart.getXAxis().setLabel("Nucleotide Index");
			reverseReadsLineChart.getYAxis().setLabel(this.countRadioButton.selectedProperty().get() ? "Frequency" : "Percentage");
			
			XYChart.Series<String, Number> seriesA = new XYChart.Series<String, Number>();
			XYChart.Series<String, Number> seriesC = new XYChart.Series<String, Number>();
			XYChart.Series<String, Number> seriesG = new XYChart.Series<String, Number>();
			XYChart.Series<String, Number> seriesT = new XYChart.Series<String, Number>();
			
			seriesA.setName("A");
			seriesC.setName("C");
			seriesG.setName("G");
			seriesT.setName("T");
			
			for ( Entry<Integer, ConcurrentHashMap<Byte, Integer>> position : experiment.getMetadata().nucleotideDistributionReverse.get(cycle.getName()).entrySet()) {
				
				Number a = position.getValue().get((byte) 'A' );
				if (this.percentageRadioButton.selectedProperty().get()) { a = a.doubleValue() / cycle.getSize(); a = a.doubleValue()*100.0; }
				seriesA.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), a) );
				
				Number c = position.getValue().get((byte) 'C' );
				if (this.percentageRadioButton.selectedProperty().get()) { c = c.doubleValue() / cycle.getSize(); c = c.doubleValue()*100.0;}
				seriesC.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), c) );
				
				Number g = position.getValue().get((byte) 'G' );
				if (this.percentageRadioButton.selectedProperty().get()) { g = g.doubleValue() / cycle.getSize(); g = g.doubleValue()*100.0;}
				seriesG.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), g) );
				
				Number t = position.getValue().get((byte) 'T' );
				if (this.percentageRadioButton.selectedProperty().get()) { t = t.doubleValue() / cycle.getSize(); t = t.doubleValue()*100.0;}
				seriesT.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), t) );
				
			}
			
			// Needs to be in this order because the color choice will be determined based on this order and the CSS file 
			this.reverseReadsLineChart.getData().clear();
			this.reverseReadsLineChart.getData().setAll(seriesA, seriesC, seriesG, seriesT);
		
		}
	}	
	
	/**
	 * Creates the line chart for the accepted read nucleotide distribution data
	 */
	private void updateAcceptedReadNucleotideDistribution() {
		
		randomizedRegionLineChart.getXAxis().setLabel("Nucleotide Index");
		randomizedRegionLineChart.getYAxis().setLabel(this.countRadioButton.selectedProperty().get() ? "Frequency" : "Percentage");
		
		randomizedRegionLineChart.setTitle(String.format("Randomized Region Nucleotide Distribution (filtered, %s nt)", this.randomizedRegionSizeComboBox.getValue()));
		
		XYChart.Series<String, Number> seriesA = new XYChart.Series<String, Number>();
		XYChart.Series<String, Number> seriesC = new XYChart.Series<String, Number>();
		XYChart.Series<String, Number> seriesG = new XYChart.Series<String, Number>();
		XYChart.Series<String, Number> seriesT = new XYChart.Series<String, Number>();
		
		seriesA.setName("A");
		seriesC.setName("C");
		seriesG.setName("G");
		seriesT.setName("T");
		
		// Skip in case not a single read was accepted
		if (experiment.getMetadata().nucleotideDistributionAccepted.get(cycle.getName()).containsKey(this.randomizedRegionSizeComboBox.getValue())) {
		
			for ( Entry<Integer, ConcurrentHashMap<Byte, Integer>> position : experiment.getMetadata().nucleotideDistributionAccepted.get(cycle.getName()).get(this.randomizedRegionSizeComboBox.getValue()).entrySet()) {
				
				Number a = position.getValue().get((byte) 'A' );
				if (this.percentageRadioButton.selectedProperty().get()) { a = a.doubleValue() / cycle.getSize(); a = a.doubleValue()*100.0; }
				seriesA.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), a) );
				
				Number c = position.getValue().get((byte) 'C' );
				if (this.percentageRadioButton.selectedProperty().get()) { c = c.doubleValue() / cycle.getSize(); c = c.doubleValue()*100.0;}
				seriesC.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), c) );
				
				Number g = position.getValue().get((byte) 'G' );
				if (this.percentageRadioButton.selectedProperty().get()) { g = g.doubleValue() / cycle.getSize(); g = g.doubleValue()*100.0;}
				seriesG.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), g) );
				
				Number t = position.getValue().get((byte) 'T' );
				if (this.percentageRadioButton.selectedProperty().get()) { t = t.doubleValue() / cycle.getSize(); t = t.doubleValue()*100.0;}
				seriesT.getData().add( new XYChart.Data<String, Number>(position.getKey().toString(), t) );
				
			}
		
		}
		
		// Needs to be in this order because the color choice will be determined based on this order and the CSS file 
		this.randomizedRegionLineChart.getData().clear();
		this.randomizedRegionLineChart.getData().setAll(seriesA, seriesC, seriesG, seriesT);
		
	}	
	
	/**
	 * Creates the barchart plot for the randomized region sizes
	 */
	private void updateRandomizedRegionSizeDistribution() {
		
		HashMap<Integer, Integer> totals = new HashMap<Integer, Integer>();
		for ( Entry<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> rand_size : experiment.getMetadata().nucleotideDistributionAccepted.get(cycle.getName()).entrySet() ) {
			
			// Take position 0 and sum up the counts of the nucleotides
			int sum = 0;
			
			for ( Entry<Byte, Integer> nucleotide : rand_size.getValue().get(0).entrySet() ) {
				
				sum += nucleotide.getValue();
				
			}
			
			totals.put(rand_size.getKey(), sum);
			
		}
		
		// Get the largest size (but at least 10) and pad with zeros and 5 to the right
		int maximum = Math.max( 10, totals.keySet().stream().max( (x,y) -> Integer.compare(x, y) ).get() ) + 5; 
		
		for (int x=0; x<maximum; x++) {
			
			if (!totals.containsKey(x)) {
				
				totals.put(x, 0);
				
			}
			
		}
		
		
		// Collect the data
		XYChart.Series<String,Number> chart_data_raw = new XYChart.Series(); 
		Integer total = 0;
		for ( int x=0; x<maximum; x++) {
			
			// Add it to chart data
			chart_data_raw.getData().add(new XYChart.Data<String,Number>(x+"", totals.get(x)));
			total += totals.get(x);
			
		}
		
		
		// Iterate over the original data and adjust
		XYChart.Series<String,Number> chart_data = new XYChart.Series(); 
		for ( Data<String, Number> item : chart_data_raw.getData() ) {
			
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
	
	/**
	 * Updates the charts when UI has changed
	 */
	public void updateFrequencyCharts() {
		
		updateForwardReadNucleotideDistribution();
		updateReverseReadNucleotideDistribution();
		
	}
	
	public void updateRandomizedRegionFrequencyCharts() {
		
		updateAcceptedReadNucleotideDistribution();
		
	}
	
	public void updateRandomizedRegionSizeDistributionCharts() {
		
		updateRandomizedRegionSizeDistribution();
		
	}
	
	
}
