/**
 * 
 */
package gui.charts.logo;

import java.io.IOException;

import gui.core.sequencing.data.SequencingDataCycleController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * @author Jan Hoinka
 *
 */
public class LogoChartPanelController {

	@FXML
	private GridPane chartGridPane;
	
	@FXML
	private AnchorPane logoAnchorPane;
	
	@FXML
	private VBox noDataVBox;
	
	/**
	 * The initial width for each of the bars
	 */
	private int barWidth = 25;
	
	/**
	 * The raw data to be converted into a logo 
	 */
	private double[][] raw_data;

	
	/**
	 * The processed data, might be identical to raw_data
	 */
	private double[][] data;
	
	/**
	 * Labels for the logo columns, must have the same width as data 
	 */
	private String[] labels;
	
	/**
	 * The Alphabet to be used in the logos
	 */
	private Alphabet alphabet = Alphabet.DNA;
	
	private int logo_size;
	
	/**
	 * The number of sequences to create the frequency profile.
	 * This must be set for the bit-score to be computed
	 */
	private int number_of_sequences = 1000;
	
	/**
	 * The scale for the y axis 
	 */
	private Scale scale = Scale.FREQUENCY;

	public void createChart() {
		
		prepareData();
		
		clearChart();
		drawChart();
		
	}
	
	/**
	 * Processes the data if required prior to drawing the logo
	 */
	private void prepareData() {

		// deep copy
		data = java.util.Arrays.stream(raw_data).map(el -> el.clone()).toArray($ -> raw_data.clone());
			
		if (scale == Scale.BITSCORE) {

			// Compute entropies
			double[] entropy = new double[logo_size];
			for (int x=0; x<logo_size; x++)
			{
				double current_entropy = 0.0;
				for (int y=0; y<raw_data.length; y++)
				{
					Double current = raw_data[y][x] * (Math.log(raw_data[y][x])/Math.log(2));
					current_entropy += current.isNaN() ? 0 : current ;
				}
				entropy[x] = current_entropy * -1.0;
			}
			
			double e_n = ( 1.0/Math.log(2) ) * ((4.0-1.0) / (2.0*this.number_of_sequences));
			
			// Compute R_i
			double[] information_content = new double[logo_size];
			for (int x=0; x<logo_size; x++)
			{
				information_content[x] =  2.0 - (entropy[x] + e_n);
			}

			// Now scale the data
			for (int x=0; x<logo_size; x++) {
				
				for (int y=0; y<data.length; y++) {
					
					data[y][x] = data[y][x] * information_content[x];
					
				}
			}
			
		}
		

		// Add and artifical row to the end to simulate Ns
		if ( (alphabet == Alphabet.RNA || alphabet == Alphabet.DNA) && this.data.length == 4 ) {
			
			double[][] temp = new double[data.length+1][logo_size];
			for (int x=0; x<data.length; x++) {
				
				for (int y=0; y<logo_size; y++){
					
					temp[x][y] = data[x][y];
					
				}
				
			}
			
			temp[data.length] = new double[logo_size];
			for (int x=0; x<data[0].length; temp[data.length][x++]=0);
			data = temp;
			
		}
		
	}
	
	/**
	 * Draws the chart 
	 */
	private void drawChart() {
		
		this.noDataVBox.setVisible(false);
		
		//First compute the overall size of the chart
		logoAnchorPane.setMinWidth(barWidth * logo_size);
		logoAnchorPane.setPrefWidth(Control.USE_PREF_SIZE);
		logoAnchorPane.setMaxWidth(Control.USE_PREF_SIZE);
		
		logoAnchorPane.setMinHeight(100);
		logoAnchorPane.setPrefHeight(100);
		//logoAnchorPane.setMaxHeight(100);
		
		
		// Add the row constraint (we only have one row)
		RowConstraints row = new RowConstraints();
		row.setMinHeight(5);
		row.setPrefHeight(Control.USE_COMPUTED_SIZE);
		row.setMaxHeight(Control.USE_COMPUTED_SIZE);
		row.setFillHeight(true);
		row.setVgrow(Priority.ALWAYS);
		this.chartGridPane.getRowConstraints().add(row);
		
		// Add the Y-Axis
		try {
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("YAxis.fxml"));
			HBox yAxis = (HBox) loader.load();
			YAxisController yAxisController = loader.getController();
			yAxisController.drawColumn();
			
			// Set Y scale
			if (scale == Scale.BITSCORE) {
				
				yAxisController.setYLabels(0+"", 2+"");
				
			}
			else if (scale == Scale.FREQUENCY) {
				
				yAxisController.setYLabels(0+"", 1+"");
				
			}
			
			this.chartGridPane.add(yAxis, 0, 0);
			
			// Set the column constraints
			ColumnConstraints col = new ColumnConstraints();
			col.setMinWidth(Control.USE_PREF_SIZE);
			col.setPrefWidth(Control.USE_COMPUTED_SIZE);
			col.setMaxWidth(Control.USE_COMPUTED_SIZE);
			col.setFillWidth(true);
			col.setHgrow(Priority.NEVER);
			this.chartGridPane.getColumnConstraints().add(col);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Add the remaining columns
		for(int column_index=0; column_index<data[0].length; column_index++) {
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("BarColumn.fxml"));
			
			try {
				
				VBox barColumn = (VBox) loader.load();
				BarColumnController barColumnController = loader.getController();

				barColumnController.setData(data);
				barColumnController.setColumnIndex(column_index);
				barColumnController.setAlphabet(alphabet);
				barColumnController.setLabels(labels);
				barColumnController.setScale(scale);
				barColumnController.drawColumn();
				
				this.chartGridPane.add(barColumn, column_index+1, 0);
				
				// Set the column constraints
				ColumnConstraints col = new ColumnConstraints();
				col.setMinWidth(5);
				col.setPrefWidth(Control.USE_COMPUTED_SIZE);
				col.setMaxWidth(Control.USE_COMPUTED_SIZE);
				col.setFillWidth(true);
				col.setHgrow(Priority.ALWAYS);
				this.chartGridPane.getColumnConstraints().add(col);
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}

	private void clearChart() {
		
		// Remove any previous constraints
		chartGridPane.getRowConstraints().clear();
		chartGridPane.getColumnConstraints().clear();
		
		// And any content
		chartGridPane.getChildren().clear();
		
	}
	
	
	public void setAlphabet(Alphabet a) {
		
		this.alphabet = a;
		
	}
	
	public void setScale(Scale s) {
		
		this.scale = s;
		
	}
	
	/**
	 * Sets the data to be drawn as a sequence logo
	 * For DNA and RNA of length n an array of data[4][n] for logos with A,C,G,T
	 * or of data[5][n] for logos with A,C,G,T,N is expected.
	 * 
	 * For Secondary Structure Context, an array of data[6][n] for logos with H,B,I,M,D,P 
	 * is expected.
	 * 
	 * @param data
	 */
	public void setData(double[][] data) {
		
		this.raw_data = data;
		this.logo_size = data[0].length;
		
	}
	
	/**
	 * Sets the labels for the columns. must be of the same length as data.
	 * @param labels
	 */
	public void setLabels(String[] labels) {
		
		this.labels = labels;
		
	}

	/**
	 * The number of sequences to create the frequency profile.
	 * This must be set for the bit-score to be computed
	 * @param number
	 */
	public void setNumberOfSequencesInAlignment( int number) {
		
		this.number_of_sequences = number;
		
	}
	
	/**
	 * @return the outmost anchor pane. can be used to control its sizing behaviour 
	 */
	public AnchorPane getRootPane() {
		
		return this.logoAnchorPane;
		
	}
	
	/**
	 * Resets the plot instance to its initial state
	 */
	public void clear() {
		
		clearChart();
		
		// Reset fields
		barWidth = 25;
		raw_data = null;
		data = null;
		labels = null;
		alphabet = Alphabet.DNA;
		scale = Scale.FREQUENCY;
	
		this.noDataVBox.setVisible(true);
		
	}
	
}
