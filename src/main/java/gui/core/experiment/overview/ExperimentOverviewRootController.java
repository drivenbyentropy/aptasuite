/**
 * 
 */
package gui.core.experiment.overview;

import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 */
public class ExperimentOverviewRootController {

	@FXML
	private Label generalInformationNameLabel;
	
	@FXML
	private Label generalInformationDescriptionLabel;
	
	@FXML
	private Label generalInformationAptamerSizeLabel;
	
	@FXML
	private Label generalInformationPrimer5Label;
	
	@FXML
	private Label generalInformationPrimer3Label;
	
	@FXML
	private Label sequenceImportStatisticsTotalProcessedReadsLabel;
	
	@FXML
	private Label sequenceImportStatisticsTotalAcceptedReadsLabel;
	
	@FXML
	private Label sequenceImportStatisticsContigAssemblyLabel;
	
	@FXML
	private Label sequenceImportStatisticsInvalidAlphabetLabel;
	
	@FXML
	private Label sequenceImportStatisticsPrimer5ErrorLabel;
	
	@FXML
	private Label sequenceImportStatisticsPrimer3ErrorLabel;
	
	@FXML
	private Label sequenceImportStatisticsInvalidCycleLabel;
	
	@FXML
	private Label sequenceImportStatisticsTotalPrimerOverlapsLabel;
	
	@FXML
	private GridPane selectionCyclePercentagesGridPane;
	
	private Experiment experiment = Configuration.getExperiment();
	
	@PostConstruct
	public void initialize() {
		
		// Set all the labels to the corresponding properties.
		generalInformationNameLabel.setText(experiment.getName());
		generalInformationDescriptionLabel.setText(experiment.getDescription());
		try {
			generalInformationAptamerSizeLabel.setText(Configuration.getParameters().getString("Experiment.randomizedRegionSize"));
		} catch (NoSuchElementException e) {
			
		}
		
		generalInformationPrimer5Label.setText(Configuration.getParameters().getString("Experiment.primer5"));
		generalInformationPrimer3Label.setText(Configuration.getParameters().getString("Experiment.primer3"));

		sequenceImportStatisticsTotalProcessedReadsLabel.setText(experiment.getMetadata().parserStatistics.get("processed_reads").toString());
		sequenceImportStatisticsTotalAcceptedReadsLabel.setText(experiment.getMetadata().parserStatistics.get("accepted_reads").toString());
		sequenceImportStatisticsContigAssemblyLabel.setText(experiment.getMetadata().parserStatistics.get("contig_assembly_fails").toString());
		sequenceImportStatisticsInvalidAlphabetLabel.setText(experiment.getMetadata().parserStatistics.get("invalid_alphabet").toString());
		sequenceImportStatisticsPrimer5ErrorLabel.setText(experiment.getMetadata().parserStatistics.get("5_prime_error").toString());
		sequenceImportStatisticsPrimer3ErrorLabel.setText(experiment.getMetadata().parserStatistics.get("3_prime_error").toString());
		sequenceImportStatisticsInvalidCycleLabel.setText(experiment.getMetadata().parserStatistics.get("invalid_cycle").toString());
		sequenceImportStatisticsTotalPrimerOverlapsLabel.setText(experiment.getMetadata().parserStatistics.get("total_primer_overlaps").toString());
		
		// Initialize the Selection Cycle Percentags 
		initializeSelectionCyclePercentages();
		
	}
	
	private void initializeSelectionCyclePercentages() {
		
		int row = 2;
		int col = 0;
		
		for ( SelectionCycle cycle : experiment.getAllSelectionCycles()) {
			
			if (row == 6) {
				
				row = 2;
				col += 2;
				
			}
			
			Label name = new Label(cycle.getName()+":");
			name.setPadding(new Insets(0,10,0,0));
			name.setFont(Font.font("System", 12));
			name.minWidth(Control.USE_PREF_SIZE);
			
			Label size = new Label(String.format("%.2f%%", ((double)cycle.getSize() / experiment.getMetadata().parserStatistics.get("accepted_reads").doubleValue())*100.0  ));
			size.setPadding(new Insets(0,20,0,0));
			size.setFont(Font.font("System", 12));
			size.minWidth(Control.USE_PREF_SIZE);
			
			selectionCyclePercentagesGridPane.add(name, col, row);
			selectionCyclePercentagesGridPane.add(size, col+1, row);
			
			row++;
		}
		
	}
	
}
