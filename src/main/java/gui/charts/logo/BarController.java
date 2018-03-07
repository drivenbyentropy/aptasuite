/**
 * 
 */
package gui.charts.logo;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * @author Jan Hoinka
 *
 */
public class BarController {

	@FXML
	StackPane rootStackPane;
	
	@FXML
	AnchorPane rootAnchorPane;
	
	SVGPath svg;
	
	double width;
	
	public void drawBar() {
		
//        Bounds b = svg.boundsInLocalProperty().getValue();
//        System.out.println("Befor Resize" + b.getHeight() + "    " + b.getWidth() );
//        
//        b = svg.boundsInLocalProperty().getValue();
//        System.out.println("After Resize" + b.getHeight() + "    " + b.getWidth() );
        
        svg.minHeight(5);
        svg.prefHeight(Control.USE_COMPUTED_SIZE);
        svg.maxHeight(Control.USE_COMPUTED_SIZE);
        
        svg.minWidth(5);
        svg.prefWidth(Control.USE_COMPUTED_SIZE);
        svg.maxWidth(Control.USE_COMPUTED_SIZE);        
        
        rootStackPane.getChildren().add(svg);
        
        rootAnchorPane.widthProperty().addListener( stageSizeListener );
        rootAnchorPane.heightProperty().addListener( stageSizeListener );
        
//        rootStackPane.setBackground( new Background( new BackgroundFill( (Color.color(Math.random(), Math.random(), Math.random()) ), CornerRadii.EMPTY, Insets.EMPTY )));
        
	}
	
	ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
    
		resize(svg, rootAnchorPane.getWidth(), rootAnchorPane.getHeight());
		
	};
	
    private void resize(SVGPath svg, double width, double height) {
    	
        double originalWidth = svg.prefWidth(-1);
        double originalHeight = svg.prefHeight(originalWidth);

        double scaleX = width / originalWidth;
        double scaleY = height / originalHeight;

        svg.setScaleX(scaleX);
        svg.setScaleY(scaleY);
        
    }

	/**
	 * @return the svg
	 */
	public SVGPath getSvg() {
		return svg;
	}

	/**
	 * @param svg the svg to set
	 */
	public void setSvg(SVGPath svg) {
		this.svg = svg;
	}

	/**
	 * @return the width
	 */
	public double getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(double width) {
		this.width = width;
	}
	
	
}
