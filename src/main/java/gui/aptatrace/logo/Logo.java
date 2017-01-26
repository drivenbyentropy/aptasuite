package gui.aptatrace.logo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Rectangle;

public class Logo {
	
	// Frequency matrix. Rows are alphabet letters, columns are positions
	double[][] data;
	
	// Chart instance
	private JFreeChart chart = null;
	
	// Alphabet
	private final Character[] nucleotides = {'A','C','G','T'};
	private final Character[] nucleotides_rna = {'A','C','G','U'};
	private final Character[] contexts = {'H','B','I','M','D','P'};
	private Character[] letters = nucleotides;
	
	private String[] round_ids = null;
	
	private char[][] letter_matrix = null;
	
	// If true, use bit score, else use frequencies
	boolean asBit = true;

	
	/**
	 * Constructs a new instance of the logo generator. 
	 * @param data: number of positions x number of letters
	 * @param round_ids: the round ids for the x axis. must have same dimension as <code>this.data.length</code> and must be unique.
	 */
	public Logo( double[][] data, String[] round_ids)
	{
		this.round_ids = round_ids;
		
		//transpose the data prior to storing it
		this.data = new double[data[0].length][data.length];
		for (int x=0; x<data.length; x++)
		{
			for (int y=0; y<data[x].length; y++)
			{
				this.data[y][x] = data[x][y];
			}
		}	
		
	}
	
	public CategoryDataset prepareData()
	{
		
		if (!asBit) 
		{
			this.letter_matrix = this.getLetterOrder(this.data);
			return DatasetUtilities.createCategoryDataset(letters, round_ids, this.sortMatrixColumns(this.data));
		}
		
		//compute positional entropies
		ArrayList<Double> entropy = new ArrayList<Double>();
		for (int x=0; x<this.data[0].length; x++)
		{
			double current_entropy = 0.0;
			for (int y=0; y<this.data.length; y++)
			{
				Double current = this.data[y][x] * (Math.log(this.data[y][x])/Math.log(2.0));
				current_entropy += current.isNaN() ? 0.0 : current;
			}
			entropy.add(current_entropy == 0.0 ? 0.0 : current_entropy * -1.0);
		}
		
		double e_n = ( 1.0/Math.log(2.0) ) * ((4.0-1.0) / (2.0*1000.0));
		
		ArrayList<Double> infromation_content = new ArrayList<Double>();
		for (Double e : entropy)
		{
			infromation_content.add(2.0 - (e + e_n));
		}
		
		double[][] result_bit = new double[this.data.length][this.data[0].length];
		for (int x=0; x<this.data[0].length; x++)
		{
			for (int y=0; y<this.data.length; y++)
			{
				result_bit[y][x] = this.data[y][x] * infromation_content.get(x);
			}
		}

		this.letter_matrix = this.getLetterOrder(result_bit);
		return DatasetUtilities.createCategoryDataset(letters, round_ids, this.sortMatrixColumns(result_bit));
		
	}
	

	/**
	 * Return the column at index <code>col</code> 
	 * @param col
	 * @param matrix
	 * @return
	 */
	public double[] getColumn(int col, double[][] matrix)
	{
		double[] column = new double[matrix.length];
		for (int x=0; x<matrix.length; x++)
		{
			column[x] = matrix[x][col];
		}
		
		return column;
	}
	
	
	/**
	 * Return the matrix, sorted by value in each column
	 * @param matrix
	 * @return
	 */
	public double[][] sortMatrixColumns(double[][] matrix)
	{
		double[][] sorted_matrix = new double[matrix.length][matrix[0].length];
		
		for (int x=0; x<matrix[0].length; x++)
		{
			Integer[] column_indices = this.argSort(this.getColumn(x, matrix), true);
			for (int y =0; y<column_indices.length; y++)
			{
				sorted_matrix[y][x] = matrix[column_indices[y]][x];
			}
		}
		
		return sorted_matrix;
	}
	
	/**
	 * Return a matrix of nucleotides that correspond to the matrix after sorting.
	 * The matrix rows are assumed to correspond to A C G T, in that order
	 * @param matrix
	 * @return
	 */
	public char[][] getLetterOrder(double[][] matrix)
	{
		char[][] letter_matrix = new char[matrix.length][matrix[0].length];
		for (int x=0; x<matrix[0].length; x++)
		{
			Integer[] column_indices = this.argSort(this.getColumn(x, matrix), true);
			for (int y =0; y<column_indices.length; y++)
			{
				letter_matrix[y][x] = letters[column_indices[y]];
			}
		}
		return letter_matrix;
	}
	
	/**
	 * Returns the indices of <code>list</code> such that when accessing
	 * the elements in that order, they would be sorted
	 * @param list
	 * @return
	 */
	public Integer[] argSort(final double[] list, final boolean ascending)
	{
		Integer[] indexes = new Integer[list.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return (ascending ? 1 : -1) * Double.compare(list[i1], list[i2]);
            }
        });
        
        return indexes;
	}
	
	
	
	/**
	 * compute the sum of the columns for a 2d array
	 * @param d
	 * @return
	 */
	public ArrayList<Double> getColumnSums(double[][] d)
	{
		ArrayList<Double> sums = new ArrayList<Double>();
		
		for (int x =0; x < d[0].length; x++)
		{
			double current = 0.0;
			for (int y=0; y < d.length; y++)
			{
				current += d[y][x];
			}
			sums.add(current);
		}
		
		return sums;
	}
	
	
	/**
	 * Creates the logo instance
	 */
	private void makeSequenceLogo()
	{

		CategoryDataset dataSet = prepareData();

		chart = ChartFactory.createStackedBarChart(
				null,
				"", 
				this.asBit ? "Bit Score" : "Frequency",
				dataSet, 
				PlotOrientation.VERTICAL, 
				false, 
				true, 
				false
				);

		//set modified painter 
		StackedBarRenderer renderer = new StackedBarRenderer(); 
		renderer.setBarPainter(new LogoBarPainter(letter_matrix));
		chart.getCategoryPlot().setRenderer(renderer);
		
		//disable shadows
		renderer.setShadowVisible(false); 
		
		CategoryPlot plot = chart.getCategoryPlot();

		//space between bars
		plot.getDomainAxis().setCategoryMargin(0.05);
		plot.getDomainAxis().setLowerMargin(0.01);
		plot.getDomainAxis().setUpperMargin(0.01);
		

		//paint outlines around letters
		renderer.setDrawBarOutline(false);
		
        //background, transparent
        plot.setBackgroundPaint(null);
        plot.setBackgroundAlpha(0.0f);
        chart.setBackgroundPaint(null);
        
        //remove horizontal lines
      	plot.setDomainGridlinesVisible(false);
      	plot.setRangeGridlinesVisible(false);
        
        //disable border
        plot.setOutlineVisible(false);
		
        //rotate category axis 45 degrees, but only for context
        if (letters == contexts)
        {
        	CategoryAxis domainAxis = plot.getDomainAxis();
        	domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }
        
        //y axis ticks
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickUnit(new NumberTickUnit(1));
        
		//ranges
		if (this.asBit)
		{
			plot.getRangeAxis().setRange(0, 2);
		}
		else
		{
			plot.getRangeAxis().setRange(0, 1);	
		}
	}

	public ChartPanel getRawLogoPanel()
	{

		CategoryDataset dataSet = prepareData();

		JFreeChart rawchart = ChartFactory.createStackedBarChart(
				null,
				"", 
				null,
				dataSet, 
				PlotOrientation.VERTICAL, 
				false, 
				true, 
				false
				);

		//set modified painter 
		StackedBarRenderer renderer = new StackedBarRenderer(); 
		renderer.setBarPainter(new LogoBarPainter(letter_matrix));
		rawchart.getCategoryPlot().setRenderer(renderer);
		
		//disable shadows
		renderer.setShadowVisible(false); 
		
		CategoryPlot plot = rawchart.getCategoryPlot();

		//space between bars
		plot.getDomainAxis().setCategoryMargin(0.05);
		plot.getDomainAxis().setLowerMargin(0.01);
		plot.getDomainAxis().setUpperMargin(0.01);
		

		//paint outlines around letters
		renderer.setDrawBarOutline(false);
		
        //background, transparent
        plot.setBackgroundPaint(null);
        plot.setBackgroundAlpha(0.0f);
        rawchart.setBackgroundPaint(null);
        
        //disable border
        plot.setOutlineVisible(false);
		       
        //remove axis ticks
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);

        
		//ranges
		if (this.asBit)
		{
			plot.getRangeAxis().setRange(0, 2);
		}
		else
		{
			plot.getRangeAxis().setRange(0, 1);	
		}
		
		return new ChartPanel(rawchart);
	}	

	public ChartPanel getSummaryLogoPanel()
	{

		CategoryDataset dataSet = prepareData();

		JFreeChart rawchart = ChartFactory.createStackedBarChart(
				null,
				"", 
				null,
				dataSet, 
				PlotOrientation.VERTICAL, 
				false, 
				true, 
				false
				);

		//set modified painter 
		StackedBarRenderer renderer = new StackedBarRenderer(); 
		renderer.setBarPainter(new LogoBarPainter(letter_matrix));
		rawchart.getCategoryPlot().setRenderer(renderer);
		
		//disable shadows
		renderer.setShadowVisible(false); 
		
		CategoryPlot plot = rawchart.getCategoryPlot();

		//space between bars
		plot.getDomainAxis().setCategoryMargin(0.05);
		plot.getDomainAxis().setLowerMargin(0.01);
		plot.getDomainAxis().setUpperMargin(0.01);
		
		//remove horizontal lines
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		
		
		//paint outlines around letters
		renderer.setDrawBarOutline(false);
		
        //background, transparent
        plot.setBackgroundPaint(null);
        plot.setBackgroundAlpha(0.0f);
        rawchart.setBackgroundPaint(null);
        
        //disable border
        plot.setOutlineVisible(false);
		       
        //axis ticks
        plot.getRangeAxis().setVisible(true);
        plot.getDomainAxis().setVisible(true);

        
		//ranges
		if (this.asBit)
		{
			plot.getRangeAxis().setRange(0, 2);
		}
		else
		{
			plot.getRangeAxis().setRange(0, 1);	
		}
		
		return new ChartPanel(rawchart);
	}		
	
	/**
	 * Returns the ChartPanel containing the FrequencyPlot
	 * @return
	 * frequency plot
	 */
	public ChartPanel getLogoPanel()
	{
		if (chart == null)
		{
			makeSequenceLogo();
		}
		
		return new ChartPanel(chart);
	}
	
	
	/**
	 * Stores the logo in PDF format
	 * @param width
	 * @param height
	 * @param fileName 
	 */
	public void saveAsPDF(int width, int height, String fileName) 
	{
		if (chart == null)
		{
			makeSequenceLogo();
		}
		
	    PdfWriter writer = null;
	 
	    Rectangle pagesize = new Rectangle(width, height);
	    Document document = new Document(pagesize);
	 
	    try 
	    {
	        writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
	        document.open();
	        PdfContentByte contentByte = writer.getDirectContent();
	        PdfTemplate template = contentByte.createTemplate(width, height);
	        Graphics2D graphics2d = template.createGraphics(width, height, new DefaultFontMapper());
	        Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);
	 
	        chart.draw(graphics2d, rectangle2d);
	         
	        graphics2d.dispose();
	        contentByte.addTemplate(template, 0, 0);
	 
	    } 
	    catch (Exception e) 
	    {
	        e.printStackTrace();
	    }
	    document.close();
	}

	/**
	 * Stores the logo in PDF format
	 * @param width
	 * @param height
	 * @param fileName 
	 */
	public void saveRAWAsPDF(int width, int height, String fileName) 
	{
		
		ChartPanel logo = getRawLogoPanel();
		
	    PdfWriter writer = null;
	 
	    Rectangle pagesize = new Rectangle(width, height);
	    Document document = new Document(pagesize);
	 
	    try 
	    {
	        writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
	        document.open();
	        PdfContentByte contentByte = writer.getDirectContent();
	        PdfTemplate template = contentByte.createTemplate(width, height);
	        Graphics2D graphics2d = template.createGraphics(width, height, new DefaultFontMapper());
	        Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);
	 
	        logo.getChart().draw(graphics2d, rectangle2d);
	         
	        graphics2d.dispose();
	        contentByte.addTemplate(template, 0, 0);
	 
	    } 
	    catch (Exception e) 
	    {
	        e.printStackTrace();
	    }
	    document.close();
	}	
	
	
	public void setBit(boolean bit)
	{
		this.asBit = bit;
	}
	
	//changes between Nucleotides and Context
	public void setAlphabetNucleotides()
	{
		letters = nucleotides;
	}

	public void setAlphabetRibonucleotides()
	{
		this.letters = this.nucleotides_rna;
	}	
	
	public void setAlphabetContexts()
	{
		letters = contexts;
	}
	
	public Dimension getMatrixDimension()
	{
		return new Dimension(data.length, data[0].length);
	}

}
