/**
 * 
 */
package gui.wizards.aptamut;

import java.text.DecimalFormat;
import javafx.application.Platform;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

/**
 * @author Jan Hoinka
 * JavaFX compliant class implementing the content for a single row in the table
 */
public class TableRowData {

	private ObservableValue<Integer> id;
    private SimpleStringProperty sequence;
    private AptaMutRootController aptaMutRootController;
    private Number frequency1;
    private Number frequency2;
    private Number enrichment;
    private Number pool_size1;
    private Number pool_size2;
    private Number seed_enrichment;
    
    // Used to lazy load the score
    private ObservableValue<Number> score = new SimpleObjectProperty<Number>(this, "score", Float.NaN);
   
    public  SimpleObjectProperty<Number> scoreProperty() { 

    	if (Double.isNaN(score.getValue().doubleValue())) {
    		
    		return new SimpleObjectProperty<Number>(score.getValue());
    		
    	}
    	
    	String formatted_score = df_count.format(score.getValue());
    	
    	return  new SimpleObjectProperty<Number>(Double.parseDouble(formatted_score)); 
    	
    }
	
    private DecimalFormat df_count = new DecimalFormat("#.###");
    
    public TableRowData( 
    		int id, 
    		String sequence, 
    		Number frequency1, 
    		Number frequency2, 
    		Number enrichment, 
    		Number pool_size1,
    		Number pool_size2,
    		Number seed_enrichment,
    		AptaMutRootController amrc
    		) {
    	
    	this.id = new SimpleObjectProperty<Integer>(id);
    	this.sequence = new SimpleStringProperty(sequence);
    	this.frequency1 = frequency1; // scx
    	this.frequency2 = frequency2; // scxp1
    	this.enrichment = enrichment;
    	this.pool_size1 = pool_size1;
    	this.pool_size2 = pool_size2;    	
    	this.aptaMutRootController = amrc;
    	this.seed_enrichment = seed_enrichment;
    	
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
    
	
	public void setScore(Double value)
    {
		
        Platform.runLater( () -> ((ObjectPropertyBase<Number>) this.score).set( value ));
        
    }
	
	
	public ObservableValue<Number> getCount1() {
		
		Number count = null;
		
		if (this.aptaMutRootController.getCmpRadioButton().isSelected()) {
			count = (this.frequency1.doubleValue() / pool_size1.doubleValue()) * 1000000;
		}

		else {
			count = this.frequency1.doubleValue();
		} 

		count = Double.parseDouble(df_count.format(count));
		
		return new SimpleObjectProperty<Number>( count );
	}
	
	
	public ObservableValue<Number> getCount2() {
		
		Number count = null;
		
		if (this.aptaMutRootController.getCmpRadioButton().isSelected()) {
			count = (this.frequency2.doubleValue() / pool_size2.doubleValue()) * 1000000;
		}

		else {
			count = this.frequency2.doubleValue();
		} 

		count = Double.parseDouble(df_count.format(count));
		
		return new SimpleObjectProperty<Number>( count );
	}
	
	public void computeAptaMutScore(boolean l) {
		
    	final boolean last = l;
		
    	Thread aptamut_task = new Thread(new Runnable() {
    		
			@Override
			public void run() {
				
				AppMutScore appscore = MutantScore.computeAppLogScore(
							frequency1.intValue(),
							frequency2.intValue(),
							seed_enrichment.doubleValue(),
							pool_size1.intValue(),
							pool_size2.intValue()
							);
				
				setScore(appscore.getScore());
				
				aptaMutRootController.refreshTables();
					
				if (last) {
					
					aptaMutRootController.setProgressDone();
					
				}
			}
			
		});
    	
    	this.aptaMutRootController.getExecutorService().execute(aptamut_task);
		
	}
	
	/**
	 * Returns the enrichment of the aptamer for the selecion cycle
	 * 
	 * @param sc
	 * @return
	 */
	public ObservableValue<Number> getEnrichment() {
		
		return new SimpleObjectProperty<Number>(Double.parseDouble(df_count.format(enrichment)));
		
	}
	
	
	@Override
	public String toString() {
		
		return this.getId().getValue()  + "\t" + this.getSequence().getValue() + "\t" + this.getCount1().getValue() + "\t" + this.getCount2().getValue()  + "\t" + this.getEnrichment().getValue()  + "\t" + this.scoreProperty().getValue();
		
	}
	
}
