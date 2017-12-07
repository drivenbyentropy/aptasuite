/**
 * 
 */
package gui.charts.logo;

import java.io.IOException;

import javax.annotation.PostConstruct;

import gui.core.sequencing.data.SequencingDataCycleController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
	GridPane chartGridPane;
	
	/**
	 * The initial width for each of the bars
	 */
	private int barWidth = 20;
	
	private double[][] data = { {0.0,0.2,0.3,0.5,1.0},
								{0.0,0.1,0.3,0.4,0.0},
								{0.0,0.5,0.1,0.1,0.0},
								{0.0,0.2,0.3,0.0,0.0},
								{1.0,0.0,0.0,0.0,0.0}
							  };

	
	@PostConstruct
	public void initialize() {

		clearChart();
		
		createChart();
	
	}
	
	/**
	 * 
	 */
	public void createChart() {
		
		
		// Add the row constraint (we only have one row)
		RowConstraints row = new RowConstraints();
		row.setMinHeight(5);
		row.setPrefHeight(Control.USE_COMPUTED_SIZE);
		row.setMaxHeight(Control.USE_COMPUTED_SIZE);
		row.setFillHeight(true);
		row.setVgrow(Priority.ALWAYS);
		this.chartGridPane.getRowConstraints().add(row);
		
		for(int column_index=0; column_index<data[0].length; column_index++) {
			
			System.out.println("Column " + column_index);
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("BarColumn.fxml"));
			
			try {
				
				GridPane barColumn = (GridPane) loader.load();
				BarColumnController barColumnController = loader.getController();

				barColumnController.setData(data);
				barColumnController.setColumnIndex(column_index);
				barColumnController.setAlphabet("dna");
				barColumnController.drawColumn();
				
				this.chartGridPane.add(barColumn, column_index, 0);
				
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
		
		
	}
	
}
