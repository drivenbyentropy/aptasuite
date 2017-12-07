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
import utilities.QSComparator;
import utilities.QSDoubleComparator;
import utilities.Quicksort;

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
	
	/**
	 * @author Jan Hoinka
	 * Comparator for argsort
	 */
	class AscQSDoubleComparator implements QSDoubleComparator{
		
		@Override
		public int compare(double a, double b) {
			
			return Double.compare(b,a);
		}
					
	}
	
	AscQSDoubleComparator comp = new AscQSDoubleComparator();
	
	private SVGPath[] contextAlphabet = {SvgAlphabet.Hairpin(), SvgAlphabet.BulgeLoop(), SvgAlphabet.InnerLoop(), SvgAlphabet.MultipleLoop(), SvgAlphabet.DanglingEnd(), SvgAlphabet.Paired()};
	private SVGPath[] DNAAlphabet =     {SvgAlphabet.Adenine(), SvgAlphabet.Cytosine(), SvgAlphabet.Guanine(), SvgAlphabet.Thymine(), SvgAlphabet.N()};
	private SVGPath[] RNAAlphabet =     {SvgAlphabet.Adenine(), SvgAlphabet.Cytosine(), SvgAlphabet.Guanine(), SvgAlphabet.Uracil(), SvgAlphabet.N()};
	
	private String alphabet_string;
	private SVGPath[] alphabet;
	
	
	public void drawColumn() {
		
		int[] order = this.computeDrawingOrder();
		
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
				controller.setSvg(alphabet[order[row_index]]);
				controller.drawBar();

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
				row.setPercentHeight(data[order[row_index]][columnIndex] * 100);
				row.setFillHeight(true);
				row.setVgrow(Priority.ALWAYS);
				this.columnGridPane.getRowConstraints().add(row);
				
				columnGridPane.add(ap, 0, row_index);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	}

	/**
	 * Given data and an index, compute the order in which 
	 * the letters should be drawn
	 */
	public int[] computeDrawingOrder() {
		
		// Temporarily copy the data
		double[] column = new double[data.length];
		// create original index array
		int[] order = new int[data.length];
		
		for (int row=0; row<data.length; row++) {
			
			order[row] = row;
			column[row] = data[row][columnIndex];
			
		}
		
		//Argsort
		Quicksort.quicksort(order, column, 0, order.length-1, comp);
		
		return order;
		
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
