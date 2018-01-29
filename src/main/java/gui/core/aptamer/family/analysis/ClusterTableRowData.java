/**
 * 
 */
package gui.core.aptamer.family.analysis;

import java.text.DecimalFormat;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import lib.aptamer.datastructures.SelectionCycle;

/**
 * @author Jan Hoinka
 * JavaFX compliant class implementing the content for a single row in the table
 */
public class ClusterTableRowData {

	private ObservableValue<Integer> id;
	private ObservableValue<Integer> cardinality; // either count or diversity at this moment
	
    public ClusterTableRowData(int id, int cardinality) {
    	
    	this.id = new SimpleObjectProperty<Integer>(id);
    	this.cardinality = new SimpleObjectProperty<Integer>(cardinality);
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
	 * @return the id
	 */
	public ObservableValue<Integer> getCardinality() {
		return cardinality;
	}

	/**
	 * @param id the id to set
	 */
	public void setCardinality(ObservableValue<Integer> cardinality) {
		this.cardinality = cardinality;
	}

	
}
