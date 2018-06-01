/**
 * 
 */
package gui.wizards.aptamut;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import javax.annotation.PostConstruct;

import com.google.common.collect.Tables;

import gui.misc.FXConcurrent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.AptaColors;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.GUIUtilities;
import utilities.SequenceTableUtils;

/**
 * @author Jan Hoinka
 *
 */
public class AptaMutRootController {

	@FXML
	private TableView<TableRowData> seedTableView;
	
	@FXML
	private TableColumn<TableRowData, Integer> seedIDTableColumn;
	
	@FXML
	private TableColumn<TableRowData, String> seedSequenceTableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> seedFrequency1TableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> seedFrequency2TableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> seedEnrichmentTableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> seedScoreTableColumn;
	
	
	@FXML
	private TableView<TableRowData> enrichedTableView;
	
	@FXML
	private TableColumn<TableRowData, Integer> enrichedIDTableColumn;
	
	@FXML
	private TableColumn<TableRowData, String> enrichedSequenceTableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> enrichedFrequency1TableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> enrichedFrequency2TableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> enrichedEnrichmentTableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> enrichedScoreTableColumn;
	
	
	@FXML
	private TableView<TableRowData> depleatedTableView;
	
	@FXML
	private TableColumn<TableRowData, Integer> depleatedIDTableColumn;
	
	@FXML
	private TableColumn<TableRowData, String> depleatedSequenceTableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> depleatedFrequency1TableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> depleatedFrequency2TableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> depleatedEnrichmentTableColumn;
	
	@FXML
	private TableColumn<TableRowData, Number> depleatedScoreTableColumn;	
	
	@FXML
	private RadioButton cmpRadioButton;
	
	@FXML
	private RadioButton showPrimersRadioButton;
	
	@FXML
	private ProgressIndicator progressIndicator;
	
	@FXML
	private Button exportButton;
	
    TableColumn[] seed_columns;
    TableColumn[] enriched_columns;
    TableColumn[] depleated_columns;

	
	/**
	 * Selection cycle x 
	 */
	private SelectionCycle scx;
	
	/**
	 * Selection cycle x plus 1
	 */
	private SelectionCycle scxp1;
	
	private BitSet cluster_membership;
	
	private Integer seed_id;
	
	private ExecutorService es;
	
	/**
	 * The font used in the table
	 */
	private Font table_font = Font.font("monospace", FontWeight.BOLD, 14);
	
	@PostConstruct
	public void initialize() {
		
		AptaLogger.log(Level.INFO,  this.getClass(), "Initializing AptaMUT content");
		
		// Bind the export button to the progress indicator
		this.exportButton.disableProperty().bind(this.progressIndicator.progressProperty().isNotEqualTo(1));
		
		// Creating Producer and Consumer Threads using the ExecutorService to manage them
		es = Executors.newFixedThreadPool(Configuration.getParameters().getInteger("Performance.maxNumberOfCores", Runtime.getRuntime().availableProcessors()));

		Thread it = new Thread(new Runnable() {

			@Override
			public void run() {
				
				initializeTables();
				
			}
			
		});
		
		es.execute(it);
		
	}
	
	/**
	 * Fill the tables and initiate computation of AptaMUT
	 */
	public void populateTableData() {
		
		// Get the seed first
		Integer seed_count_sxc = this.scx.getAptamerCardinality(seed_id);
		Integer seed_count_sxcp1 = this.scxp1.getAptamerCardinality(seed_id);
		
		Integer pool_size_sxc = this.scx.getSize();
		Integer pool_size_sxcp1 = this.scxp1.getSize();
		
		byte[] seed_sequence = Configuration.getExperiment().getAptamerPool().getAptamer(seed_id);
		
		Number seed_enrichment = ( seed_count_sxcp1.doubleValue() / pool_size_sxcp1 ) / ( seed_count_sxc.doubleValue() / pool_size_sxc ); 
		
        // Prepare tables
        ObservableList<TableRowData> seed_data =      FXCollections.observableArrayList( new ArrayList<TableRowData>() );
        ObservableList<TableRowData> enriched_data =  FXCollections.observableArrayList( new ArrayList<TableRowData>() );
        ObservableList<TableRowData> depleated_data = FXCollections.observableArrayList( new ArrayList<TableRowData>() );
 
		Platform.runLater(()->{
			enrichedTableView.setItems(enriched_data);
        	depleatedTableView.setItems(depleated_data);
        	seedTableView.setItems(seed_data);
		});
        
        // Populate seed data
        seed_data.add( new TableRowData( 
        		seed_id, 
        		new String(seed_sequence), 
        		seed_count_sxc, 
        		seed_count_sxcp1, 
        		seed_enrichment, 
        		pool_size_sxc,
        		pool_size_sxcp1,
        		seed_enrichment,
        		this) );
        
        
        // Enriched and Depleated
        Iterator<Entry<Integer, Integer>> iterator_sxc = this.scx.iterator().iterator();
        Iterator<Entry<Integer, Integer>> iterator_sxcp1 = this.scxp1.iterator().iterator();
        Entry<Integer, Integer> entry_sxc = iterator_sxc.next();
        Entry<Integer, Integer> entry_sxcp1 = iterator_sxcp1.next();
        
		for ( Entry<Integer, byte[]> entry : Configuration.getExperiment().getAptamerPool().inverse_view_iterator()) {
			
			// Only take cluster members into account
			if (entry.getKey().intValue() != seed_id && this.cluster_membership.get(entry.getKey())) {

				// Determine if the item is in the cluster for this cycle
				while (iterator_sxcp1.hasNext() && entry_sxcp1.getKey() < entry.getKey()) {
					
					entry_sxcp1 = iterator_sxcp1.next();
					
				}
				
//				// Determine if the previous cycle has this aptamer as well
				while (iterator_sxc.hasNext() && entry_sxc.getKey() < entry.getKey()) {
					
					entry_sxc = iterator_sxc.next();
					
				}
				
				
				// Now we either have a match or we don't
				if (entry_sxc.getKey().intValue() == entry_sxcp1.getKey().intValue() && entry_sxcp1.getKey().intValue() == entry.getKey().intValue() ) {

					// Compute enrichment
					Number enrichment = ( entry_sxcp1.getValue().doubleValue() / pool_size_sxcp1 ) / ( entry_sxc.getValue().doubleValue() / pool_size_sxc ); 

					final Number entry_sxc_final = entry_sxc.getValue();
					final Number entry_sxcp1_final = entry_sxcp1.getValue(); 
					
					// Is it enriched or depleated?
					if (enrichment.doubleValue() > 1.0) {

				        // Populate enriched data
						enriched_data.add( new TableRowData( 
				        		entry.getKey(), 
				        		new String(entry.getValue()), 
				        		entry_sxc_final, 
				        		entry_sxcp1_final, 
				        		enrichment, 
				        		pool_size_sxc,
				        		pool_size_sxcp1,
				        		seed_enrichment,
				        		this) );
				        
						
					} else {
						
				        // Populate depleated data
				        depleated_data.add( new TableRowData( 
				        		entry.getKey(), 
				        		new String(entry.getValue()), 
				        		entry_sxc_final,
				        		entry_sxcp1_final, 
				        		enrichment, 
				        		pool_size_sxc,
				        		pool_size_sxcp1,
				        		seed_enrichment,
				        		this) );
						
					}
					
				}
				
			}
			
		}
		
		// now initialize the computation for the aptamut scores
		int x = 0;
		int total = this.enrichedTableView.getItems().size();
		
		for ( TableRowData row : enriched_data ) {
			
			row.computeAptaMutScore( this.depleatedTableView.getItems().size() != 0 &&  !(x<total-1) );
			x++;
			
		}
		
		x = 0;
		total = depleated_data.size();
		for ( TableRowData row : depleated_data ) {

			row.computeAptaMutScore( !(x<total-1) );
			x++;
			
		}
		
		if ( enriched_data.size() == 0 && depleated_data.size() == 0 ) {
			
			this.setProgressDone();
			
		}
		
		if ( depleated_data.size() == 0 ) {
			
			this.setProgressDone();
			
		}
		
	}

	
	/**
	 * Set the table details and shape
	 */
	private void initializeTables() {
		
	    TableColumn[] seed_columns = {seedIDTableColumn, seedSequenceTableColumn, seedFrequency1TableColumn, seedFrequency2TableColumn, seedEnrichmentTableColumn, seedScoreTableColumn};
	    TableColumn[] enriched_columns = {enrichedIDTableColumn, enrichedSequenceTableColumn, enrichedFrequency1TableColumn, enrichedFrequency2TableColumn, enrichedEnrichmentTableColumn, enrichedScoreTableColumn};
	    TableColumn[] depleated_columns = {depleatedIDTableColumn, depleatedSequenceTableColumn, depleatedFrequency1TableColumn, depleatedFrequency2TableColumn, depleatedEnrichmentTableColumn, depleatedScoreTableColumn};

	    Platform.runLater(() ->{
			
			this.seedTableView.setStyle("-fx-font: 14px monospace;");
			this.enrichedTableView.setStyle("-fx-font: 14px monospace;");
			this.depleatedTableView.setStyle("-fx-font: 14px monospace;");
			
			this.seedTableView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
			this.enrichedTableView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
			this.depleatedTableView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
			
		});
		
		// Style the sequence columns
		// We need to style the sequence according to their dna color, so we add a custom
		seedSequenceTableColumn.setCellFactory(column -> {
			return new TableCell<TableRowData, String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setGraphic(null);
						setText(null);
						setStyle("");
					} else {
						setGraphic(null);

						HBox textHBox = buildTextHBox(
								item, 
								Configuration.getExperiment().getAptamerPool().getAptamerBounds(this.getTableView().getItems().get(getIndex()).getId().getValue())
								);
						textHBox.setAlignment(Pos.CENTER_LEFT);

						setGraphic(textHBox);
						setHeight(textHBox.getPrefHeight());
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}
				}
			};
		});
		
		enrichedSequenceTableColumn.setCellFactory(column -> {
			return new TableCell<TableRowData, String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setGraphic(null);
						setText(null);
						setStyle("");
					} else {
						setGraphic(null);

						HBox textHBox = buildTextHBox(
								item, 
								Configuration.getExperiment().getAptamerPool().getAptamerBounds(this.getTableView().getItems().get(getIndex()).getId().getValue())
								);
						textHBox.setAlignment(Pos.CENTER_LEFT);

						setGraphic(textHBox);
						setHeight(textHBox.getPrefHeight());
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}
				}
			};
		});
		
		depleatedSequenceTableColumn.setCellFactory(column -> {
			return new TableCell<TableRowData, String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setGraphic(null);
						setText(null);
						setStyle("");
					} else {
						setGraphic(null);

						HBox textHBox = buildTextHBox(
								item, 
								Configuration.getExperiment().getAptamerPool().getAptamerBounds(this.getTableView().getItems().get(getIndex()).getId().getValue())
								);
						textHBox.setAlignment(Pos.CENTER_LEFT);

						setGraphic(textHBox);
						setHeight(textHBox.getPrefHeight());
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}
				}
			};
		});
		
		// Seed
		seedIDTableColumn.setCellValueFactory( C -> C.getValue().getId() );
		seedSequenceTableColumn.setCellValueFactory( C -> C.getValue().getSequence() );
		seedFrequency1TableColumn.setCellValueFactory( C -> C.getValue().getCount1() );
		seedFrequency2TableColumn.setCellValueFactory( C -> C.getValue().getCount2() );
		seedEnrichmentTableColumn.setCellValueFactory( C -> C.getValue().getEnrichment() );
		seedScoreTableColumn.setCellValueFactory( C -> C.getValue().scoreProperty() );
		
		// Enriched
		enrichedIDTableColumn.setCellValueFactory( C -> C.getValue().getId() );
		enrichedSequenceTableColumn.setCellValueFactory( C -> C.getValue().getSequence() );
		enrichedFrequency1TableColumn.setCellValueFactory( C -> C.getValue().getCount1() );
		enrichedFrequency2TableColumn.setCellValueFactory( C -> C.getValue().getCount2() );
		enrichedEnrichmentTableColumn.setCellValueFactory( C -> C.getValue().getEnrichment() );
		enrichedScoreTableColumn.setCellValueFactory( C -> C.getValue().scoreProperty() );
		
		// Depleated 
		depleatedIDTableColumn.setCellValueFactory( C -> C.getValue().getId() );
		depleatedSequenceTableColumn.setCellValueFactory( C -> C.getValue().getSequence() );
		depleatedFrequency1TableColumn.setCellValueFactory( C -> C.getValue().getCount1() );
		depleatedFrequency2TableColumn.setCellValueFactory( C -> C.getValue().getCount2() );
		depleatedEnrichmentTableColumn.setCellValueFactory( C -> C.getValue().getEnrichment() );
		depleatedScoreTableColumn.setCellValueFactory( C -> C.getValue().scoreProperty() );
		
		// Headers and Column Widths
	    double textwidth = GUIUtilities.computeStringWidth(seedIDTableColumn.getText(), table_font);
	    seedIDTableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
	    seedIDTableColumn.setPrefWidth(Math.max(seedIDTableColumn.getPrefWidth() , textwidth +  50) );	
		
	    for (int x=0; x<seed_columns.length; x++) {
	    	
	    	enriched_columns[x].minWidthProperty().bind(seed_columns[x].widthProperty());
	    	enriched_columns[x].maxWidthProperty().bind(seed_columns[x].widthProperty());
	    	enriched_columns[x].prefWidthProperty().bind(seed_columns[x].widthProperty());
	    	
	    	depleated_columns[x].minWidthProperty().bind(seed_columns[x].widthProperty());
	    	depleated_columns[x].maxWidthProperty().bind(seed_columns[x].widthProperty());
	    	depleated_columns[x].prefWidthProperty().bind(seed_columns[x].widthProperty());
	    	
	    }
	    
	    // Bind the sorting properties from the seed table to the other two such that
	    // when the user selects a column to sort on, the other two tables follow this behaviour
	    GUIUtilities.createSortBinding(this.seedTableView, this.enrichedTableView);
	    GUIUtilities.createSortBinding(this.seedTableView, this.depleatedTableView);
	    
	    // enable copy/paste
	 	new SequenceTableUtils(seedTableView, 1, showPrimersRadioButton);
	 	new SequenceTableUtils(enrichedTableView, 1, showPrimersRadioButton);
	 	new SequenceTableUtils(depleatedTableView, 1, showPrimersRadioButton);
	    
	}
	
	
	
	/**
	 * Build TextFlow with the sequence
	 * 
	 * @param sequence - the aptamer with primers
	 * @param bounds - the bounds destribing the start and end (exclusive) of the randomized region
	 * @return - TextFlow with dna colored according to its standard color and grayed primers 
	 */
	private HBox buildTextHBox(String sequence, AptamerBounds bounds) {
		
		HBox hb = new HBox();
		for ( int x=0; x<sequence.length(); x++ ) {
			
			Label n = new Label(""+sequence.charAt(x));
			
			if ( x==bounds.startIndex || x==bounds.endIndex ) {
				
				Label spacer = new Label();
				spacer.setPadding(new Insets(0,1,0,1));
				hb.getChildren().add(spacer);
				
			}
			
			// Primers
			if ( x<bounds.startIndex || x>=bounds.endIndex ) {

				if (!this.showPrimersRadioButton.isSelected()) {
					
					continue;
					
				}
				
				n.setTextFill(AptaColors.Nucleotides.PRIMERS);
				
			}

			
			// Randomized Region
			else if ( x>=bounds.startIndex && x<bounds.endIndex) {
				
				n.setTextFill(AptaColors.NucleotidesMap.get( (byte) sequence.charAt(x)));
				n.setPadding(new Insets(0,0.5,0,0.5));
				
			}

			hb.getChildren().add(n);
			
		}
		
	    return hb;
	}		
	
	
	/**
	 * Set all required ino for this instance of aptamut
	 * @param scx
	 * @param scxp1
	 * @param cluster_membership
	 * @param seed_id
	 */
	public void setInputData(SelectionCycle scx, SelectionCycle scxp1, BitSet cluster_membership, int seed_id) {
		
		this.scx = scx;
		this.scxp1 = scxp1;
		this.seed_id = seed_id;
		this.cluster_membership = cluster_membership;
		
		this.seedFrequency1TableColumn.setText((this.cmpRadioButton.isSelected() ? "CMP " : "RAW Counts ") +  this.scx.getName());
		this.seedFrequency2TableColumn.setText((this.cmpRadioButton.isSelected() ? "CMP " : "RAW Counts ") +  this.scxp1.getName());
		
	    // Bind Scrollbars
	    ScrollBar enriched_scrollbar = getScrollBar(this.enrichedTableView, Orientation.HORIZONTAL);
	    ScrollBar seed_scrollbar = getScrollBar(this.seedTableView, Orientation.HORIZONTAL);
	    ScrollBar depleated_scrollbar = getScrollBar(this.depleatedTableView, Orientation.HORIZONTAL);
	    
	    enriched_scrollbar.setVisible(false);
	    seed_scrollbar.setVisible(false);

	    depleated_scrollbar.valueProperty().addListener( (val, ov, nv) -> {
	    	
	    	try {
	    		
	    		enriched_scrollbar.setValue(nv.doubleValue());
	    		seed_scrollbar.setValue(nv.doubleValue());
	    		
	    	} catch( Exception e) {
	    		
	    		e.printStackTrace();
	    		
	    	}
	    	
	    });
	    
	    // Table Widths
	    double textwidth = GUIUtilities.computeStringWidth(seedFrequency1TableColumn.getText(), table_font);
	    seedFrequency1TableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
	    seedFrequency1TableColumn.setPrefWidth(Math.max(seedFrequency1TableColumn.getPrefWidth() , textwidth +  50) );	

	    textwidth = GUIUtilities.computeStringWidth(seedFrequency2TableColumn.getText(), table_font);
	    seedFrequency2TableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
	    seedFrequency2TableColumn.setPrefWidth(Math.max(seedFrequency2TableColumn.getPrefWidth() , textwidth +  50) );	

	    textwidth = GUIUtilities.computeStringWidth(seedEnrichmentTableColumn.getText(), table_font);
	    seedEnrichmentTableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
	    seedEnrichmentTableColumn.setPrefWidth(Math.max(seedEnrichmentTableColumn.getPrefWidth() , textwidth +  50) );	

	    textwidth = GUIUtilities.computeStringWidth(seedScoreTableColumn.getText(), table_font);
	    seedScoreTableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
	    seedScoreTableColumn.setPrefWidth(Math.max(seedScoreTableColumn.getPrefWidth() , textwidth +  50) );	

	    byte[] seed_sequence = Configuration.getExperiment().getAptamerPool().getAptamer(seed_id);
	    AptamerBounds seed_bounds = Configuration.getExperiment().getAptamerPool().getAptamerBounds(seed_id);
	    textwidth = GUIUtilities.computeStringWidth(new String(seed_sequence).substring(this.showPrimersRadioButton.isSelected() ?  0 : seed_bounds.startIndex, this.showPrimersRadioButton.isSelected() ? seed_sequence.length : seed_bounds.endIndex), table_font);
	    seedSequenceTableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
	    seedSequenceTableColumn.setPrefWidth( textwidth + 150 );
	    
	}

	/**
	 * Provide access to the executor service 
	 * @return
	 */
	public ExecutorService getExecutorService() {
		
		return es;
		
	}

	/**
	 * Returns the ScrollBar instance of a Tableview
	 * @param firstTable
	 * @param orientation
	 * @return
	 */
	public ScrollBar getScrollBar(TableView<?> firstTable, Orientation orientation){
	    ScrollBar firstScrollBar = null;

	    for (Node node : firstTable.lookupAll(".scroll-bar")) {
	            if (node instanceof ScrollBar && ((ScrollBar) node).getOrientation().equals(orientation)) {
	                firstScrollBar= (ScrollBar) node;
	            }
	    }
	    
	    return firstScrollBar;
	}
	
	public void close() {
		
		es.shutdownNow();
		
		// get a handle to the stage
        Stage stage = (Stage) this.depleatedTableView.getScene().getWindow();
        
        // close it
        stage.close();
		
	}

	@FXML
	public void refreshTables() {
		
		Platform.runLater(() -> {
			
			this.seedTableView.refresh();
			this.enrichedTableView.refresh();
			this.depleatedTableView.refresh();
			
		    byte[] seed_sequence = Configuration.getExperiment().getAptamerPool().getAptamer(seed_id);
		    AptamerBounds seed_bounds = Configuration.getExperiment().getAptamerPool().getAptamerBounds(seed_id);
		    double textwidth = GUIUtilities.computeStringWidth(new String(seed_sequence).substring(this.showPrimersRadioButton.isSelected() ?  0 : seed_bounds.startIndex, this.showPrimersRadioButton.isSelected() ? seed_sequence.length : seed_bounds.endIndex), table_font);
		    seedSequenceTableColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
		    seedSequenceTableColumn.setPrefWidth( textwidth + 150 );
			
		});
		
	}
	
	/**
	 * Exports the tables to text file
	 */
	@FXML
	public void exportButtonAction() {
		
		FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file to export AptaMUT data");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {

            	String str = "Hello";
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                
                // Header
                writer.write("TYPE\tID\tSEQUENCE\tCOUNTS " + this.scx.getName() + "\tCOUTNS " + this.scxp1.getName() + "\tENRICHMENT\tLOGSCORE\n");
                
                // Seed first
                for ( TableRowData row : this.seedTableView.getItems()) {
                	
                	writer.write("SEED\t" + row.toString() +"\n");
                	
                }
                
                // Seed first
                for ( TableRowData row : this.enrichedTableView.getItems()) {
                	
                	writer.write("ENRICHED\t" + row.toString() +"\n");
                	
                }
                
                // Seed first
                for ( TableRowData row : this.depleatedTableView.getItems()) {
                	
                	writer.write("DEPLEATED\t" + row.toString() +"\n");
                	
                }
                
                writer.close();
            	
            } catch (IOException ex) {

            	AptaLogger.log(Level.WARNING, this.getClass(), "AptaMUT Export Failed");
            	AptaLogger.log(Level.WARNING, this.getClass(), ex);

            }
        }
		
	}
	
	/**
	 * @return the cmpRadioButton
	 */
	public RadioButton getCmpRadioButton() {
		return cmpRadioButton;
	}


	/**
	 * @return the showPrimersRadioButton
	 */
	public RadioButton getShowPrimersRadioButton() {
		return showPrimersRadioButton;
	}

	/**
	 * @return the progressIndicator
	 */
	public void setProgressDone() {
		
		Platform.runLater(() -> {
			progressIndicator.setProgress(1);
		});
		
	}

}
