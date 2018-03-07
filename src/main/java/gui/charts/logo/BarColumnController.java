/**
 * 
 */
package gui.charts.logo;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import utilities.QSDoubleComparator;
import utilities.Quicksort;

/**
 * @author Jan Hoinka
 *
 */
/**
 * @author matrix
 *
 */
public class BarColumnController {

	@FXML
	GridPane columnGridPane;
	
	@FXML
	HBox xAxisHBox;
	
	@FXML
	AnchorPane axisCenterAnchorPane;
	
	@FXML
	StackPane axisCenterStackPane;

	@FXML
	AnchorPane axisLeftAnchorPane;
	
	@FXML
	StackPane axisLeftStackPane;
	
	@FXML
	AnchorPane axisRightAnchorPane;
	
	@FXML
	StackPane axisRightStackPane;
	
	@FXML
	Label xTickLabel;
	
	private SVGPath axisLeft   = this.getAxisLeft();
	private SVGPath axisCenter = this.getAxisCenter();
	private SVGPath axisRight  = this.getAxisRight();
	
	/**
	 * The data to be used for plotting
	 */
	private double[][] data;
	
	/**
	 * The x-tick labels
	 */
	private String[] labels;
	
	/**
	 * The column in data to take the information from
	 */
	private int columnIndex;
	
	private Scale scale;
	
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
	
	private Alphabet alphabet_type;
	private SVGPath[] alphabet;
	
	
	public void drawColumn() {
		
		// Compute the order in which the letters are to be drawn
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

		
		// Add fake row for bit scores
		if ( scale == Scale.BITSCORE) {

			// Compute height
			double height = 0.0;
			for (int row_index=0; row_index<data.length; row_index++) {
				height += (data[order[row_index]][columnIndex]/2.0) * 100.0;
			}
			height = 100 - height;
			
			// create and add an empty column
			try {
				
				// Get node and controller
				FXMLLoader loader = new FXMLLoader(getClass().getResource("Bar.fxml"));
				Node node = loader.load();
				BarController controller = loader.getController();
				
				controller.setWidth(50);
				controller.setSvg(new SVGPath());
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
				row.setPercentHeight(height);
				row.setFillHeight(true);
				row.setVgrow(Priority.ALWAYS);
				this.columnGridPane.getRowConstraints().add(row);
				
				// Skip elements with 0 percent height
				if (row.getPercentHeight() != 0.0) {
					
					columnGridPane.addColumn(0, ap);
					
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}	
		
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
				if ( scale == Scale.BITSCORE) {
					row.setPercentHeight((data[order[row_index]][columnIndex]/2.0) * 100.0);
				}
				else {
					row.setPercentHeight(data[order[row_index]][columnIndex] * 100.0);
				}
				row.setFillHeight(true);
				row.setVgrow(Priority.ALWAYS);
				this.columnGridPane.getRowConstraints().add(row);
				
				// Skip elements with 0 percent height
				if (row.getPercentHeight() == 0.0) {
					
					continue;
					
				}
				
				columnGridPane.addColumn(0, ap);
//				columnGridPane.add(ap, 0, row_counter);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		
		// Now add the X-Axis
		axisLeftStackPane.getChildren().add(axisLeft);
		this.axisLeftAnchorPane.widthProperty().addListener( stageSizeListener );
		this.axisLeftAnchorPane.heightProperty().addListener( stageSizeListener );

		
		axisCenterStackPane.getChildren().add(axisCenter);
		this.axisCenterAnchorPane.widthProperty().addListener( stageSizeListener );
		this.axisCenterAnchorPane.heightProperty().addListener( stageSizeListener );
		
		axisRightStackPane.getChildren().add(axisRight);
		this.axisRightAnchorPane.widthProperty().addListener( stageSizeListener );
		this.axisRightAnchorPane.heightProperty().addListener( stageSizeListener );
		
		// And add the label for this tick
		this.xTickLabel.setText(this.labels[this.columnIndex]);
		
	}
	
	// Defines the desired dimensions of the svgs
	ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
	    
		resize(this.axisLeft, axisLeftAnchorPane.getWidth(), axisLeftAnchorPane.getHeight()/2.);
		resize(this.axisCenter, axisCenterAnchorPane.getWidth(), axisCenterAnchorPane.getHeight()/2.);
		resize(this.axisRight, axisRightAnchorPane.getWidth(), axisRightAnchorPane.getHeight()/2.);
		
	};

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
	public Alphabet getAlphabet() {
		return alphabet_type;
	}

	/**
	 * @param alphablet the alphablet to set
	 */
	public void setAlphabet(Alphabet alphabet) {
		
		this.alphabet_type = alphabet;
		
		switch(alphabet) {
		
			case DNA :				this.alphabet = this.DNAAlphabet;
									break;
		
			case RNA:				this.alphabet = this.RNAAlphabet;
									break;
			
			case STRUCTURE_CONTEXT:	this.alphabet = this.contextAlphabet;
									break;
		
		}
		
	}
	
	public void setLabels(String[] labels) {
		
		this.labels = labels;
		
	}
	
    private void resize(SVGPath svg, double width, double height) {
    	
        double originalWidth = svg.prefWidth(-1);
        double originalHeight = svg.prefHeight(originalWidth);

        double scaleX = width / originalWidth;
        double scaleY = height / originalHeight;

        svg.setScaleX(scaleX);
        svg.setScaleY(scaleY);
        
    }
	
    
    
	private SVGPath getAxisCenter() {
		
		SVGPath svg = new SVGPath();
		svg.setContent("M 290.50781 391.08984 L 290.50781 431.08984 L 294.50781 431.08984 L 294.50781 391.08984 L 290.50781 391.08984 z ");
		svg.setFill(Color.BLACK);
		svg.setStrokeWidth(0);
		return svg;
		
	}

	private SVGPath getAxisLeft() {
		
		SVGPath svg = new SVGPath();
		svg.setContent("M 290.50781 391.08984 L 290.50781 407.3125 L 32.285156 407.3125 L 32.285156 414.87109 L 290.50781 414.87109 L 290.50781 431.08984 L 290.55078 431.08984 L 290.55078 391.08984 L 290.50781 391.08984 z ");
		return svg;
		
	}
	
	private SVGPath getAxisRight() {
		
		SVGPath svg = new SVGPath();
		svg.setContent("M 32.285156 391.08984 L 32.285156 431.08984 L 32.328125 431.08984 L 32.328125 414.87109 L 290.55078 414.87109 L 290.55078 407.3125 L 32.328125 407.3125 L 32.328125 391.08984 L 32.285156 391.08984 z ");
		return svg;
		
	}
	
	public void setScale(Scale s) {
		
		this.scale = s;
		
	}
	
}
