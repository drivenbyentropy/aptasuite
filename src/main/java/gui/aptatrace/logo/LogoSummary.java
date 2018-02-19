package gui.aptatrace.logo;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

public class LogoSummary
{
	// Instance attributes used in this example
	private	ArrayList<ArrayList<Object>>table;
	
	private float						document_margin = 50;
	
	private float 						pheight;
	private float						header_height = 150;	
	
	private float						pwidth;
	private float						width_id;
	private float						width_motif;
	private float						width_seed;
	private float						width_pvalue;
	private float						width_abundance;
	private float						width_trace;
	private float						width_margin = 110;
	
	private int							logo_letter_width = 150;
	private int 						font_size = 65;
	private BaseFont 					bf;
	private BaseFont 					bf_header;
	private BaseFont 					bf_id;
	
	private DecimalFormat 			formatter_pvalue = new DecimalFormat("0.###E0");
	private NumberFormat			formatter_abundance = new DecimalFormat("#0.00");
	DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance();
	
	// Constructor of main frame
	/**
	 * 
	 */
	public LogoSummary()
	{
		// We need to set the decimal separator, otherwise Parsing to double will fail in countries using comma
		sym.setDecimalSeparator('.');
		formatter_pvalue.setDecimalFormatSymbols(sym);
		
		table = new ArrayList<ArrayList<Object>>();
		//add header
		ArrayList<Object> header = new ArrayList<Object>();
		header.add("ID");
		header.add("Motif Profile");
		header.add("Seed");
		header.add("P-value");
		header.add("Abundance");
		header.add("K-context Trace");
		table.add(header);
		
		try {
			bf  = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			bf_header  = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			bf_id  = BaseFont.createFont(BaseFont.COURIER_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a row to the table. 
	 * @param motif A Logo instance containing the sequence motif 
	 * @param kmer The seed kmer of the cluster
	 * @param pvalue The pvalue of the seed
	 * @param abundance The abundance of the motif in the dataset in percent (0-100)
	 * @param trace A Logo instance containing the K-context Trace of the motif
	 */
	public void AddRow(Logo motif, String kmer, Double pvalue, Double abundance, Logo trace)
	{
		ArrayList<Object> row = new ArrayList<Object>();
		row.add( String.valueOf(table.size()) + ")" );
		row.add(motif);
		row.add(kmer);
		row.add(pvalue);
		row.add(abundance);
		row.add(trace);
		
		table.add(row);
	}
	
	
	private void computeDimensions() 
	{
	    pheight = 150;
	    pwidth = 0;

	    width_id = 0;
    	width_motif = 0;
    	width_seed = 0;
    	width_pvalue = 0;
    	width_abundance = 0;
    	width_trace = 0;	    
    	
    	//header widths
    	width_id = Math.max(width_id, bf_header.getWidthPoint((String) table.get(0).get(0), font_size));
    	width_motif = Math.max(width_motif, bf_header.getWidthPoint((String) table.get(0).get(1), font_size));
    	width_seed = Math.max(width_seed, bf_header.getWidthPoint((String) table.get(0).get(2), font_size));
    	width_pvalue = Math.max(width_pvalue, bf_header.getWidthPoint((String) table.get(0).get(3), font_size));
    	width_abundance = Math.max(width_abundance, bf_header.getWidthPoint((String) table.get(0).get(4), font_size));	    	
    	width_trace = Math.max(width_trace, bf_header.getWidthPoint((String) table.get(0).get(5), font_size));	     	
    	
    	
	    for (int x = 1; x<table.size(); x++)
	    {
	    	//id
	    	width_id = Math.max(width_id, bf_id.getWidthPoint((String) table.get(x).get(0), font_size));
	    	
	    	//motif
	    	Logo motif = (Logo) table.get(x).get(1);
	    	width_motif = Math.max(width_motif, motif.getMatrixDimension().height * logo_letter_width);
	    
	    	//seed
	    	width_seed = Math.max(width_seed, bf.getWidthPoint((String) table.get(x).get(2), font_size));
	    	

	    	//pvalue
	    	width_pvalue = Math.max(width_pvalue, bf.getWidthPoint(formatter_pvalue.format((Double)table.get(x).get(3)), font_size));
	    	
	    	//abundance
	    	width_abundance = Math.max(width_abundance, bf.getWidthPoint(formatter_abundance.format((Double)table.get(x).get(4)) + "%", font_size));	    	
	    	
	    	//trace
	    	Logo trace = (Logo) table.get(x).get(5);
	    	width_trace = Math.max(width_trace, trace.getMatrixDimension().height * logo_letter_width);	    
	    }
	    
	    pwidth = width_id + width_motif + width_seed + width_pvalue + width_abundance + width_trace;
	    
	}	
	
	
	/**
	 * Store the table as PDF
	 * @param fileName path to the file the table will be saved
	 */
	public void saveAsPDF(String fileName) 
	{
		computeDimensions();
		
	    PdfWriter writer = null;
	 
	    Rectangle pagesize = new Rectangle(pwidth + 5*width_margin + 2*document_margin, table.size() * pheight + header_height);
	    Document document = new Document(pagesize);
	 
	    try 
	    {
	        writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
	        document.open();
	        PdfContentByte contentByte = writer.getDirectContent();
	        PdfTemplate template = contentByte.createTemplate(pwidth + 5*width_margin + 2*document_margin, table.size() * pheight + header_height);
	        @SuppressWarnings("deprecation")
	        Graphics2D graphics2d = template.createGraphics(pwidth + 5*width_margin + 2*document_margin, table.size() * pheight + header_height);
	         
	        // make header
	        float header_x = 0;

		    contentByte.saveState();
		    contentByte.beginText();
		    contentByte.moveText(header_x+75, (float) ( table.size()*pheight + header_height - (pheight/2.0) ) );
		    contentByte.setFontAndSize(bf_header, font_size);
		    contentByte.showText((String) table.get(0).get(0));
		    contentByte.endText();
		    contentByte.restoreState();
		    
		    header_x += width_id + width_margin;	        
	        
		    contentByte.saveState();
		    contentByte.beginText();
		    contentByte.moveText(header_x+75, (float) ( table.size()*pheight + header_height - (pheight/2.0) ) );
		    contentByte.setFontAndSize(bf_header, font_size);
		    contentByte.showText((String) table.get(0).get(1));
		    contentByte.endText();
		    contentByte.restoreState();
		    
		    header_x += width_motif + width_margin;
	
		    contentByte.saveState();
		    contentByte.beginText();
		    contentByte.moveText(header_x, (float) ( table.size()*pheight + header_height - (pheight/2.0) ) );
		    contentByte.setFontAndSize(bf_header, font_size);
		    contentByte.showText((String) table.get(0).get(2));
		    contentByte.endText();
		    contentByte.restoreState();
		    
		    header_x += width_seed + width_margin;
		    
		    contentByte.saveState();
		    contentByte.beginText();
		    contentByte.moveText(header_x, (float) ( table.size()*pheight + header_height - (pheight/2.0) ) );
		    contentByte.setFontAndSize(bf_header, font_size);
		    contentByte.showText((String) table.get(0).get(3));
		    contentByte.endText();
		    contentByte.restoreState();
		    
		    header_x += width_pvalue + width_margin;
		    
		    contentByte.saveState();
		    contentByte.beginText();
		    contentByte.moveText(header_x, (float) ( table.size()*pheight + header_height - (pheight/2.0) ) );
		    contentByte.setFontAndSize(bf_header, font_size);
		    contentByte.showText((String) table.get(0).get(4));
		    contentByte.endText();
		    contentByte.restoreState();
		    
		    header_x += width_abundance + width_margin;
		    
		    contentByte.saveState();
		    contentByte.beginText();
		    contentByte.moveText(header_x+75, (float) ( table.size()*pheight + header_height - (pheight/2.0) ) );
		    contentByte.setFontAndSize(bf_header, font_size);
		    contentByte.showText((String) table.get(0).get(5));
		    contentByte.endText();
		    contentByte.restoreState();
		    		    
	        //draw line
		    LineSeparator ls = new LineSeparator();
		    ls.setOffset(-1 * (header_height/2));
		    document.add(new Chunk(ls));
		    
		    
	        float row_y =  header_height;
	        
			for (int row = 1; row < table.size(); row++)  
	        {
				float row_x = 0;
				
				//gray background
				Rectangle rect = new Rectangle(	width_margin/2, 
												table.size() * pheight + header_height - ((row+1)*pheight), 
												pwidth + 5*width_margin + 2*document_margin - width_margin/2, 
												table.size() * pheight + header_height - (row*pheight)
												);
			    if (row % 2 == 0)
			    {
			    	rect.setBackgroundColor(new BaseColor(204, 204, 204, 150));
			    }
			    contentByte.rectangle(rect);

				// paint id
			    contentByte.saveState();
			    contentByte.beginText();
			    contentByte.moveText(row_x + 75, (float) ( table.size()*pheight + header_height - row_y - (pheight/2.0) - (font_size/2.0)) );
			    contentByte.setFontAndSize(bf_id, font_size);
			    contentByte.showText((String) table.get(row).get(0));
			    contentByte.endText();
			    contentByte.restoreState();
			    
				row_x += width_id + width_margin;
			    
				// paint motif
				Rectangle2D rectangle2d = new Rectangle2D.Double(row_x, row_y, ((Logo) table.get(row).get(1)).getMatrixDimension().height * logo_letter_width, pheight);
				JFreeChart chart = ((ChartPanel) ((Logo)table.get(row).get(1)).getSummaryLogoPanel() ).getChart();
				chart.draw(graphics2d, rectangle2d);

				row_x += width_motif + width_margin;
				
				// paint seed
			    contentByte.saveState();
			    contentByte.beginText();
			    contentByte.moveText(row_x, (float) ( table.size()*pheight + header_height - row_y - (pheight/2.0) - (font_size/2.0)) );
			    contentByte.setFontAndSize(bf, font_size);
			    contentByte.showText((String) table.get(row).get(2));
			    contentByte.endText();
			    contentByte.restoreState();
			    
				row_x += width_seed + width_margin;

				
				// paint pvalue
			    contentByte.saveState();
			    contentByte.beginText();
			    contentByte.moveText(row_x, (float) ( table.size()*pheight + header_height - row_y - (pheight/2.0) - (font_size/2.0)) );
			    contentByte.setFontAndSize(bf, font_size);
			    contentByte.showText(formatter_pvalue.format((Double)table.get(row).get(3)));
			    contentByte.endText();
			    contentByte.restoreState();
			    
				row_x += width_pvalue + width_margin;
				
				
				// paint abundance
			    contentByte.saveState();
			    contentByte.beginText();
			    contentByte.moveText(row_x, (float) ( table.size()*pheight + header_height - row_y - (pheight/2.0) - (font_size/2.0)) );
			    contentByte.setFontAndSize(bf, font_size);
			    contentByte.showText(formatter_abundance.format((Double)table.get(row).get(4)) + "%");
			    contentByte.endText();
			    contentByte.restoreState();
			    
				row_x += width_abundance + width_margin;
				
				// paint motif
				rectangle2d = new Rectangle2D.Double(row_x, row_y, width_trace, pheight);
				chart = ((ChartPanel) ((Logo)table.get(row).get(5)).getSummaryLogoPanel() ).getChart();
				chart.draw(graphics2d, rectangle2d);
				
				row_y += pheight;
	        }

			graphics2d.dispose();
	        
	        contentByte.addTemplate(template, document_margin, 0);
	 
	    } 
	    catch (Exception e) 
	    {
	        e.printStackTrace();
	    }
	    document.close();
	}	
}