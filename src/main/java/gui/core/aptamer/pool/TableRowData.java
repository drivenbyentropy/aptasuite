/**
 * 
 */
package gui.core.aptamer.pool;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import lib.aptamer.datastructures.SelectionCycle;

/**
 * @author Jan Hoinka
 * JavaFX compliant class implementing the content for a single row in the table
 */
public class TableRowData {

	private ObservableValue<Integer> id;
    private SimpleStringProperty sequence;
    private AptamerPoolRootController aptamerPoolRootController;
	
    private DecimalFormat df = new DecimalFormat("0.00E00");
    private DecimalFormat df_count = new DecimalFormat("#.###");
    DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance();
    
    public TableRowData(int id, String sequence, AptamerPoolRootController aprc) {
    	
    	this.id = new SimpleObjectProperty<Integer>(id);
    	this.sequence = new SimpleStringProperty(sequence);
    	this.aptamerPoolRootController = aprc;
    	
    	// We need to set the decimal separator, otherwise Parsing to double will fail in countries using comma
    	sym.setDecimalSeparator('.');
    	df.setDecimalFormatSymbols(sym);
    	df_count.setDecimalFormatSymbols(sym);
    }

	/**
	 * @return the id
	 */
	public ObservableValue<Integer> getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(ObservableValue<Integer> id) {
		this.id = id;
	}

	/**
	 * @return the sequence
	 */
	public SimpleStringProperty getSequence() {
		return sequence;
	}

	/**
	 * @param sequence the sequence to set
	 */
	public void setSequence(SimpleStringProperty sequence) {
		this.sequence = sequence;
	}
    
	
	/**
	 * Returns the raw count or CMP of that aptamer in selection cycle <code>sc</code>
	 * @param sc
	 * @return
	 */
	public ObservableValue<Number> getCount(SelectionCycle sc) {
		
		Number count = null;
		
		if (this.aptamerPoolRootController.getCmpRadioButton().isSelected()) {
			count = (sc.getAptamerCardinality(id.getValue()) / (double) sc.getSize()) * 1000000;
		}

		else if (this.aptamerPoolRootController.getRawCountsRadionButton().isSelected()) {
			count = sc.getAptamerCardinality(id.getValue());
		} 

		count = Double.parseDouble(df_count.format(count));
		
		return new SimpleObjectProperty<Number>( count );
	}
	
	/**
	 * Returns the frequency of that aptamer in selection cycle <code>sc</code>
	 * 
	 * @param sc
	 * @return
	 */
	public ObservableValue<Number> getFrequency(SelectionCycle sc) {
		
		Number count = Double.parseDouble( df.format(sc.getAptamerCardinality(id.getValue()) / (double) sc.getSize()).toLowerCase() ); 
		
		return new SimpleObjectProperty<Number>( count );

	}
	
		
	/**
	 * Returns the enrichment of the aptamer for the selecion cycle
	 * 
	 * @param sc
	 * @return
	 */
	public ObservableValue<Number> getEnrichment(SelectionCycle sc) {
		
		Number count_n = sc.getAptamerCardinality(this.id.getValue());
		Number count_nmo = sc.getPreviousSelectionCycle().getAptamerCardinality(this.id.getValue());
		
		if (count_nmo.intValue() == 0) { return getCount(sc); }
		
		Number enrichment = ( count_n.doubleValue() / sc.getSize() ) / ( count_nmo.doubleValue() / sc.getPreviousSelectionCycle().getSize() );
		
		enrichment = Double.parseDouble(df.format(enrichment));
		
		return new SimpleObjectProperty<Number>(enrichment);
		
	}
	
	
}
