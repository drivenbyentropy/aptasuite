/**
 * 
 */
package gui.charts.logo;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * @author Jan Hoinka
 *
 */
public class YAxisController {

	@FXML
	private GridPane columnGridPane;
	
	@FXML
	private HBox xAxisHBox;
	
	@FXML
	private AnchorPane axisCenterAnchorPane;
	
	@FXML
	private StackPane axisCenterStackPane;

	@FXML
	private AnchorPane axisLeftAnchorPane;
	
	@FXML
	private StackPane axisLeftStackPane;
	
	@FXML
	private AnchorPane axisRightAnchorPane;
	
	@FXML
	private StackPane axisRightStackPane;
	
	@FXML
	private StackPane yAxisBottomStackPane;
	
	@FXML
	private AnchorPane yAxisBottomAnchorPane;
	
	@FXML
	private StackPane yAxisCenterStackPane;
	
	@FXML
	private AnchorPane yAxisCenterAnchorPane;
	
	@FXML
	private StackPane yAxisTopStackPane;
	
	@FXML
	private AnchorPane yAxisTopAnchorPane;
	
	@FXML
	private Label xTickLabel;
	
	@FXML
	private Label yAxisMaxLabel;
	
	@FXML
	private Label yAxisMinLabel;
	
	private SVGPath axisTop   = this.getYAxis();
	private SVGPath axisMiddle = this.getYAxis();
	private SVGPath axisBottom  = this.getYAxis();
	
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

		
		// Add the Y-Axis
		this.yAxisTopStackPane.getChildren().add(axisTop);
		this.yAxisTopAnchorPane.widthProperty().addListener( stageSizeListener );
		this.yAxisTopAnchorPane.heightProperty().addListener( stageSizeListener );
				
		this.yAxisCenterStackPane.getChildren().add(axisMiddle);
		this.yAxisCenterAnchorPane.widthProperty().addListener( stageSizeListener );
		this.yAxisCenterAnchorPane.heightProperty().addListener( stageSizeListener );
		
		this.yAxisBottomStackPane.getChildren().add(axisBottom);
		this.yAxisBottomAnchorPane.widthProperty().addListener( stageSizeListener );
		this.yAxisBottomAnchorPane.heightProperty().addListener( stageSizeListener );
		
	}
	
	// Defines the desired dimensions of the svgs
	ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
	    
		// Y
		resize(this.axisTop, yAxisTopAnchorPane.getWidth(), yAxisTopAnchorPane.getHeight()/2.);
		resize(this.axisMiddle, yAxisCenterAnchorPane.getWidth()/8., yAxisCenterAnchorPane.getHeight());
		resize(this.axisBottom, yAxisBottomAnchorPane.getWidth(), yAxisBottomAnchorPane.getHeight()/2.);

	};


    private void resize(SVGPath svg, double width, double height) {
    	
        double originalWidth = svg.prefWidth(-1);
        double originalHeight = svg.prefHeight(originalWidth);

        double scaleX = width / originalWidth;
        double scaleY = height / originalHeight;

        svg.setScaleX(scaleX);
        svg.setScaleY(scaleY);
        
    }
    
	private SVGPath getYAxis() {
		
		SVGPath svg = new SVGPath();
		svg.setContent("M 6992.5137 4895.6348 L 6992.5137 5095.6348 L 7192.5137 5095.6348 L 7192.5137 4895.6348 L 6992.5137 4895.6348 z");
		return svg;
		
	}
	
	public void setYLabels(String min, String max) {
		
		this.yAxisMinLabel.setText(min);
		this.yAxisMaxLabel.setText(max);
		
	}
	 
}
