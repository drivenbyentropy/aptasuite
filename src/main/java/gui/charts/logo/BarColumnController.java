/**
 * 
 */
package gui.charts.logo;

import java.io.IOException;

import javax.annotation.PostConstruct;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.SVGPath;

/**
 * @author Jan Hoinka
 *
 */
public class BarColumnController {

	@FXML
	GridPane columnGridPane;
	
	/**
	 * The data to be used for plotting
	 */
	private double[][] data;
	
	/**
	 * The column in data to take the information from
	 */
	private int columnIndex;
	
	private SVGPath[] contextAlphabet = {SvgAlphabet.Hairpin(), SvgAlphabet.BulgeLoop(), SvgAlphabet.InnerLoop(), SvgAlphabet.MultipleLoop(), SvgAlphabet.DanglingEnd(), SvgAlphabet.Paired()};
	private SVGPath[] DNAAlphabet =     {SvgAlphabet.Adenine(), SvgAlphabet.Cytosine(), SvgAlphabet.Guanine(), SvgAlphabet.Thymine(), SvgAlphabet.N()};
	private SVGPath[] RNAAlphabet =     {SvgAlphabet.Adenine(), SvgAlphabet.Cytosine(), SvgAlphabet.Guanine(), SvgAlphabet.Uracil(), SvgAlphabet.N()};
	
	private String alphabet_string;
	private SVGPath[] alphabet;
	
	
	public void drawColumn() {
		
		// Remove any previous constraints
		columnGridPane.getRowConstraints().clear();
		columnGridPane.getColumnConstraints().clear();
		
		// Set the column Constraints
		ColumnConstraints col0 = new ColumnConstraints();
		col0.setMinWidth(5);
		col0.setPrefWidth(Control.USE_COMPUTED_SIZE);
		col0.setMaxWidth(Control.USE_COMPUTED_SIZE);
		col0.setFillWidth(true);
		col0.setHgrow(Priority.ALWAYS);
		this.columnGridPane.getColumnConstraints().add(col0);

		// Loop over column data and add the content 
		for (int row_index=0; row_index<data.length; row_index++) {
		
			try {
	
				// Get node and controller
				FXMLLoader loader = new FXMLLoader(getClass().getResource("Bar.fxml"));
				Node node = loader.load();
				BarController controller = loader.getController();
				
				controller.setWidth(50);
				controller.setSvg(alphabet[row_index]);
				controller.drawBar();

				System.out.println("Height: " + data[row_index][columnIndex]);
				
				// Add the bar to the column in an anchor pane
				AnchorPane ap = new AnchorPane();
				
				// Size properties
				ap.minWidth(5);
				ap.minHeight(5);
				ap.prefWidth(Control.USE_COMPUTED_SIZE);
				ap.prefHeight(Control.USE_COMPUTED_SIZE);
				ap.maxWidth(Control.USE_COMPUTED_SIZE);
				ap.maxHeight(Control.USE_COMPUTED_SIZE);
				ap.getChildren().add(node);
				
				// Make sure the node resizes with its parent
				AnchorPane.setBottomAnchor(node, 0.0);
				AnchorPane.setTopAnchor(node, 0.0);
				AnchorPane.setLeftAnchor(node, 0.0);
				AnchorPane.setRightAnchor(node, 0.0);
				
				// Add the row constraint
				RowConstraints row = new RowConstraints();
				//row.setMinHeight(Control.USE_COMPUTED_SIZE);
				//row.setPrefHeight(Control.USE_COMPUTED_SIZE);
				//row.setMaxHeight(Control.USE_COMPUTED_SIZE);
				row.setPercentHeight(data[row_index][columnIndex] * 100);
				row.setFillHeight(true);
				row.setVgrow(Priority.ALWAYS);
				this.columnGridPane.getRowConstraints().add(row);
				
				columnGridPane.add(ap, 0, row_index);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
//		System.out.println("TEST");
//		
//		try {
//			Node n1 = FXMLLoader.load(getClass().getResource("/gui/charts/logo/LogoRoot.fxml"));
//			anchorPane1.getChildren().add(n1);
//			AnchorPane.setBottomAnchor(n1, 0.0);
//			AnchorPane.setTopAnchor(n1, 0.0);
//			AnchorPane.setLeftAnchor(n1, 0.0);
//			AnchorPane.setRightAnchor(n1, 0.0);
//
//			Node n2 = FXMLLoader.load(getClass().getResource("/gui/charts/logo/LogoRoot.fxml"));
//			anchorPane2.getChildren().add(n2);
//			AnchorPane.setBottomAnchor(n2, 0.0);
//			AnchorPane.setTopAnchor(n2, 0.0);
//			AnchorPane.setLeftAnchor(n2, 0.0);
//			AnchorPane.setRightAnchor(n2, 0.0);
//
//			Node n3 = FXMLLoader.load(getClass().getResource("/gui/charts/logo/LogoRoot.fxml"));
//			anchorPane3.getChildren().add(n3);
//			AnchorPane.setBottomAnchor(n3, 0.0);
//			AnchorPane.setTopAnchor(n3, 0.0);
//			AnchorPane.setLeftAnchor(n3, 0.0);
//			AnchorPane.setRightAnchor(n3, 0.0);
//
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}

	/**
	 * @return the data
	 */
	public double[][] getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(double[][] data) {
		
		System.out.println("Setting data " + (data ==null));
		
		this.data = data;
	}

	/**
	 * @return the index
	 */
	public int getColumnIndex() {
		return columnIndex;
	}

	/**
	 * @param index the index to set
	 */
	public void setColumnIndex(int index) {
		this.columnIndex = index;
	}

	/**
	 * @return the alphablet
	 */
	public String getAlphabet() {
		return alphabet_string;
	}

	/**
	 * @param alphablet the alphablet to set
	 */
	public void setAlphabet(String alphabet) {
		this.alphabet_string = alphabet;
		
		switch(alphabet) {
		
			case "dna":	this.alphabet = this.DNAAlphabet;
						break;
		
			case "rna":	this.alphabet = this.RNAAlphabet;
			break;
			
			case "context":	this.alphabet = this.contextAlphabet;
			break;
		
		}
		
	}
	
}
