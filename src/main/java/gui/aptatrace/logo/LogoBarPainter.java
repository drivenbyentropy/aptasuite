package gui.aptatrace.logo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.swing.JPanel;

import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.ui.GradientPaintTransformer;
import org.jfree.ui.RectangleEdge;

public class LogoBarPainter extends StandardBarPainter{

	private static final long serialVersionUID = 1998800511042516903L;
	
	// Colors for plots
	public static HashMap<Character, Color> plot_colors = new HashMap<Character, Color>();
	static
	{
		// Nucleotides
	    plot_colors.put(Character.valueOf('A'), Color.decode("#007F00"));
	    plot_colors.put(Character.valueOf('C'), Color.yellow);
	    plot_colors.put(Character.valueOf('G'), Color.decode("#0000FF"));
	    plot_colors.put(Character.valueOf('T'), Color.decode("#FF0000"));
	    plot_colors.put(Character.valueOf('U'), Color.decode("#FF0000"));
	    plot_colors.put(Character.valueOf('N'), Color.gray);
	    
	    plot_colors.put(Character.valueOf('H'), Color.decode("#FF7070"));
	    plot_colors.put(Character.valueOf('B'), Color.decode("#FA9600"));
	    plot_colors.put(Character.valueOf('I'), Color.decode("#A0A0FF"));
	    plot_colors.put(Character.valueOf('M'), Color.cyan);
	    plot_colors.put(Character.valueOf('D'), Color.pink);
	    plot_colors.put(Character.valueOf('P'), Color.decode("#C8C8C8"));
	}
	
	// Font used for letters. Since we want to be system independent we ship it as a resource
	private static Font f = null;
	static
	{
		//InputStream istream = LogoBarPainter.class.getResourceAsStream("SourceCodePro-Regular.ttf");
		InputStream istream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SourceCodePro-Regular.ttf");
		try {
			f = Font.createFont(Font.TRUETYPE_FONT, istream);
		} catch (FontFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		f = f.deriveFont(Font.PLAIN, 500);	
	}
	

	
	/**
	 * Matrix of size 4 x logo length containing the color for each letter.
	 * This is necessary since each column of the barchart is sorted by decreasing order
	 * and because jfreechart only allows to set colors for each series. 
	 */
	private char[][] letter_matrix;

	public LogoBarPainter(char[][] letter_matrix)
	{
		this.letter_matrix = letter_matrix;
	}
	
    /**
     * Paints a single bar instance.
     *
     * @param g2  the graphics target.
     * @param renderer  the renderer.
     * @param row  the row index.
     * @param column  the column index.
     * @param bar  the bar
     * @param base  indicates which side of the rectangle is the base of the bar.
     */
    @Override
	public void paintBar(Graphics2D g2, BarRenderer renderer, int row, int column, RectangularShape bar, RectangleEdge base) {
    	
        ShapeDummy dummy = new ShapeDummy();
        Shape letter = null;
        
        //set letter according to row
        letter = dummy.convert(letter_matrix[row][column], bar);
        
        Paint itemPaint = plot_colors.get(letter_matrix[row][column]);
        GradientPaintTransformer t = renderer.getGradientPaintTransformer();
        if (t != null && itemPaint instanceof GradientPaint) {
        	itemPaint = t.transform((GradientPaint) itemPaint, letter);
        }
        g2.setPaint(itemPaint);
        g2.fill(letter);
        
        // draw the outline...
        if (renderer.isDrawBarOutline()) 
        {
            Stroke stroke = renderer.getItemOutlineStroke(row, column);
            Paint paint = renderer.getItemOutlinePaint(row, column);
            if (stroke != null && paint != null) {
                g2.setStroke(stroke);
            	g2.setPaint(paint);
            	//g2.draw(bar);
            	g2.draw(letter);
            }
        }
        
    }
    
    
    /**
     * Dummy class in order to convert a character into an object of type <code>shape</code>
     * @author hoinkaj
     *
     */
    class ShapeDummy extends JPanel
    {
		private static final long serialVersionUID = -4036899781931086036L;

		public Shape convert(char c, RectangularShape bar) 
        {
            FontRenderContext frc = getFontMetrics(f).getFontRenderContext();
            GlyphVector v = f.createGlyphVector(frc, new char[] { c });

                        
            // Scale letter to fit the bar bounds
            AffineTransform at = new AffineTransform();
            at.scale( bar.getBounds2D().getWidth() / v.getOutline().getBounds2D().getWidth() , bar.getBounds2D().getHeight() / v.getOutline().getBounds2D().getHeight() );
            v.setGlyphTransform(0, at);

            // Convert into shape
            Shape letter = v.getOutline( (float) bar.getBounds2D().getMinX() , (float) (bar.getBounds2D().getMinY() + bar.getBounds2D().getHeight()) );

            //make fine adjustments to fit bar boundaries
            double difference_x = bar.getBounds2D().getX() - letter.getBounds2D().getX();
            double difference_y = bar.getBounds2D().getY() - letter.getBounds2D().getY();
            
            // Reassign with refinements
            letter = v.getOutline( (float) bar.getBounds2D().getMinX() + (float) difference_x , (float) (bar.getBounds2D().getMinY() + bar.getBounds2D().getHeight() + difference_y) );
            
            return letter;
        }
    }
    
}
