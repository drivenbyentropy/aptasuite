/**
 * 
 */
package gui.wizards.newexperiment;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Jan Hoinka
 * DataModel storing the information for each selection cycle
 * using bindable properties
 */
public class SelectionCycleDataModel {

	private ObjectProperty<Integer> roundNumber = new SimpleObjectProperty<Integer>(); 
	
	private StringProperty roundName = new SimpleStringProperty();
	
	private BooleanProperty isControlCycle = new SimpleBooleanProperty();
	
	private BooleanProperty isCounterSelectionCycle = new SimpleBooleanProperty();
	
	private StringProperty forwardReadsFile = new SimpleStringProperty();
	
	private StringProperty reverseReadsFile = new SimpleStringProperty();
	
	private StringProperty barcode5 = new SimpleStringProperty();
	
	private StringProperty barcode3 = new SimpleStringProperty();

	/**
	 * @return the roundNumber
	 */
	public ObjectProperty<Integer> getRoundNumber() {
		return roundNumber;
	}

	/**
	 * @param roundNumber the roundNumber to set
	 */
	public void setRoundNumber(ObjectProperty<Integer> roundNumber) {
		this.roundNumber = roundNumber;
	}

	/**
	 * @return the roundName
	 */
	public StringProperty getRoundName() {
		return roundName;
	}

	/**
	 * @param roundName the roundName to set
	 */
	public void setRoundName(StringProperty roundName) {
		this.roundName = roundName;
	}

	/**
	 * @return the isControlCycle
	 */
	public BooleanProperty getIsControlCycle() {
		return isControlCycle;
	}

	/**
	 * @param isControlCycle the isControlCycle to set
	 */
	public void setIsControlCycle(BooleanProperty isControlCycle) {
		this.isControlCycle = isControlCycle;
	}

	/**
	 * @return the isSelectionCycle
	 */
	public BooleanProperty getIsCounterSelectionCycle() {
		return isCounterSelectionCycle;
	}

	/**
	 * @param isSelectionCycle the isSelectionCycle to set
	 */
	public void setIsCounterSelectionCycle(BooleanProperty isSelectionCycle) {
		this.isCounterSelectionCycle = isSelectionCycle;
	}

	/**
	 * @return the forwardReadsFile
	 */
	public StringProperty getForwardReadsFile() {
		return forwardReadsFile;
	}

	/**
	 * @param forwardReadsFile the forwardReadsFile to set
	 */
	public void setForwardReadsFile(StringProperty forwardReadsFile) {
		this.forwardReadsFile = forwardReadsFile;
	}

	/**
	 * @return the reverseReadsFile
	 */
	public StringProperty getReverseReadsFile() {
		return reverseReadsFile;
	}

	/**
	 * @param reverseReadsFile the reverseReadsFile to set
	 */
	public void setReverseReadsFile(StringProperty reverseReadsFile) {
		this.reverseReadsFile = reverseReadsFile;
	}

	/**
	 * @return the barcode
	 */
	public StringProperty getBarcode5() {
		return barcode5;
	}

	/**
	 * @param barcode the barcode to set
	 */
	public void setBarcode5(StringProperty barcode5) {
		this.barcode5 = barcode5;
	}
	

	/**
	 * @return the barcode
	 */
	public StringProperty getBarcode3() {
		return barcode3;
	}

	/**
	 * @param barcode the barcode to set
	 */
	public void setBarcode3(StringProperty barcode3) {
		this.barcode3 = barcode3;
	}

}
