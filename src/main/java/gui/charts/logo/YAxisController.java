/**
 * 
 */
package gui.charts.logo;

import java.io.IOException;

import javax.annotation.PostConstruct;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import utilities.QSComparator;
import utilities.QSDoubleComparator;
import utilities.Quicksort;

/**
 * @author Jan Hoinka
 *
 */
public class YAxisController {

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
	StackPane yAxisBottomStackPane;
	
	@FXML
	AnchorPane yAxisBottomAnchorPane;
	
	@FXML
	StackPane yAxisCenterStackPane;
	
	@FXML
	AnchorPane AxisCenterAnchorPane;
	
	@FXML
	StackPane yAxisTopStackPane;
	
	
	@FXML
	Label xTickLabel;
	
	private SVGPath axisLeft   = this.getAxisLeft();
	private SVGPath axisCenter = this.getAxisCenter();
	private SVGPath axisRight  = this.getAxisRight();
	
	
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
//		this.yAxisCenterStackPane.getChildren().add(axisCenter);
//		this.axisCenterAnchorPane.widthProperty().addListener( stageSizeListener );
//		this.axisCenterAnchorPane.heightProperty().addListener( stageSizeListener );
				
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
		
		
	}
	
	// Defines the desired dimensions of the svgs
	ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
	    
		resize(this.axisLeft, axisLeftAnchorPane.getWidth(), axisLeftAnchorPane.getHeight());
		resize(this.axisCenter, axisCenterAnchorPane.getWidth(), axisCenterAnchorPane.getHeight());
		resize(this.axisRight, axisRightAnchorPane.getWidth(), axisRightAnchorPane.getHeight());
		
	};


    private void resize(SVGPath svg, double width, double height) {
    	
        double originalWidth = svg.prefWidth(-1);
        double originalHeight = svg.prefHeight(originalWidth);

        double scaleX = width / originalWidth;
        double scaleY = height / originalHeight;

        System.out.println("Scale X  " + scaleX);
        System.out.println("Scale Y  " + scaleY);
        
        svg.setScaleX(scaleX);
        svg.setScaleY(scaleY);
        
    }
	
    
    
	private SVGPath getAxisCenter() {
		
		SVGPath svg = new SVGPath();
		//svg.setContent("M 290.50781 391.08984 L 290.50781 431.08984 L 298.50781 431.08984 L 298.50781 391.08984 L 290.50781 391.08984 z ");
		svg.setContent("M 290.50781 391.08984 L 290.50781 431.08984 L 294.50781 431.08984 L 294.50781 391.08984 L 290.50781 391.08984 z ");
		
		//svg.setContent("M 290.50781 391.08984 L 290.50781 407.3125 L 274.28516 407.3125 L 274.28516 414.87109 L 290.50781 414.87109 L 290.50781 431.08984 L 298.06445 431.08984 L 298.06445 414.87109 L 314.28516 414.87109 L 314.28516 407.3125 L 298.06445 407.3125 L 298.06445 391.08984 L 290.50781 391.08984 z ");
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
	
}
