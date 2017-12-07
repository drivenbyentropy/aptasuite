/**
 * 
 */
package gui.core.aptamer.pool;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;

import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import gui.activity.ProgressPaneController;
import gui.aptatrace.logo.Logo;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.converter.NumberStringConverter;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.structure.capr.CapR;
import lib.structure.rnafold.MFEData;
import lib.structure.rnafold.RNAFoldAPI;
import utilities.AptaColors;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.Index;
import utilities.QSComparator;
import utilities.QSDoubleComparator;
import utilities.Quicksort;

/**
 * @author Jan Hoinka
 *
 */
public class AptamerPoolRootController {

	@FXML
	private StackPane poolTableViewStackPane;
	
	@FXML
	private RadioButton showPrimersRadioButton;
	
	@FXML
	private RadioButton hidePrimersRadioButton;
	
	@FXML
	private RadioButton cmpRadioButton;
	
	@FXML
	private RadioButton rawCountsRadionButton;
	
	@FXML
	private ComboBox<String> sortByComboBox;
	
	@FXML
	private ComboBox<String> onCycleComboBox;
	
	@FXML
	private TextField maxItemTextField;
	
	@FXML
	private TextField itemsPerPageTextField;
	
	@FXML
	private Label totalItemsLabel;
	
	@FXML
	private TextField searchTextField;
	
	@FXML
	private VBox secondaryStructureVBox;
	
	@FXML
	private SwingNode varnaSwingNode;
	
	@FXML
	private Label varnaSequenceLabel;
	
	@FXML
	private Label varnaStructureLabel;
	
	@FXML
	private Label varnaMFELabel;
	
	@FXML
	private SplitPane tableDetailsSplitPane;
	
	@FXML
	private SplitPane detailsSplitPane;
	
	@FXML
	private StackPane aptamerPoolRootStackPane;
	
	@FXML
	private GridPane bppmGridPane;
	
	@FXML
	private SwingNode contextProbabilitySwingNode;
	
	/**
	 * Instance of the pagination for the table
	 */
	private Pagination pagination;
	
	/**
	 * The table instance
	 */
	private TableView<TableRowData> poolTableView;
	
	private Experiment experiment = Configuration.getExperiment();
	
	/**
	 * The number of rows per page
	 */
	private IntegerProperty rows_per_page = new SimpleIntegerProperty(500);
	
	/**
	 * Observable to keep track of the total number of aptamers 
	 */
	private IntegerProperty total_items = new SimpleIntegerProperty(0);
	
	private Integer total_items_int = 0;
	
	/**
	 * Bound to the selection of <code>sortByComboBox</code>
	 */
	private String cycle_to_sort_by_string;
	
	/**
	 * Bound to sortByComboBox
	 */
	private String criteria_to_sort_by_string;
	
	/**
	 * Access to the max_items box outside the JavaFX thread
	 */
	private Integer max_items_int = 0;
	
	private AptamerPool pool = experiment.getAptamerPool();
	
	/**
	 * The total number of aptamers to show in the table
	 */
	private IntegerProperty max_items = new SimpleIntegerProperty(Math.min(100000, pool.size()));
	
	/**
	 * The ids of the aptamers as present in the pool, sorted by the criteria
	 * as specified in the GUI elements
	 */
	private int[] aptamer_ids;
	
	/**
	 * As large as aptamer_ids and 1 if that position should be considered for the current table
	 */
	private BitSet id_mask;
	
	/**
	 * Indicates if search is on or of to color the matching items in the table
	 */
	private boolean is_search = false;
	
	/**
	 * Indicates if primers are to be displayed or not
	 */
	private boolean with_primers = true;
	
	/**
	 * Same as above but is true at those positions where the search has found a match
	 */
	private BitSet search_mask;

	private ArrayList<SelectionCycle> positive = experiment.getSelectionCycles();
	private ArrayList<ArrayList<SelectionCycle>> negative = experiment.getCounterSelectionCycles();
	private ArrayList<ArrayList<SelectionCycle>> control = experiment.getControlSelectionCycles();
	
	/**
	 * The string the user has entered into the query field
	 */
	private String query_string = null;
	
	/**
	 * The panel into which the rna structure will be drawn into
	 */
	private VARNAPanel vp = null;
	
	/**
	 * instance of our RNAFold port to predict secondary structures
	 * 
	 */
	private RNAFoldAPI rnafoldapi = new RNAFoldAPI();
	
	/**
	 * The font used in the table
	 */
	private Font table_font = Font.font("monospace", FontWeight.BOLD, 14);
	
	/**
	 * Used to estimate the width on certain elements
	 */
	private FontMetrics fontMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(table_font);
	
	/**
	 * The local capr instance
	 */
	private CapR capr = new CapR();
	
	@PostConstruct
	public void initialize() {
		
		initializeControlElements();
		
		// We want the gui not to block on initialization, hence we wrap this into a progress pane
		ProgressPaneController pp = ProgressPaneController.getProgressPane(this.aptamerPoolRootStackPane, new Runnable() {
			
			@Override
			public void run() {
				
				AptaLogger.log(Level.INFO, this.getClass(), "Initializing data structures");
				
				initializeControlArrays();
				
				computeIndexOrder();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Creating Table and Pagination");
				
				createTable();
				
				initializePagination();
				
				redrawTableContent();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Initializing Swing components");
				
				initializeSwingComponents();
				
				repaintSwingComponents();
				
			}
		
		});
		
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	/**
	 * Takes care of the logic for initializing the arrays 
	 * containing the aptamer indices for display in the table
	 */
	private void initializeControlArrays() {
		
		// First copy all aptamer ids from the pool to the array
		aptamer_ids = new int[pool.size()];
		
		// Set all bits to false
		this.id_mask = new BitSet(aptamer_ids.length);
		
		this.search_mask = new BitSet(aptamer_ids.length);
		
	}
	
	/**
	 * Performs all required operations to integrate the VARNA components
	 * into the JavaFX structure
	 */
	private void initializeSwingComponents() {
		
		SwingUtilities.invokeLater(() -> {
            	vp = new VARNAPanel();
            	varnaSwingNode.setContent(vp);
            	predictAndShowStructure(null);
            	//varnaSwingNode.getContent().repaint();
        });
		
		// We need to repaint the RNA on resize
		tableDetailsSplitPane.getDividers().get(0).positionProperty().addListener(e1->repaintSwingComponents());
		detailsSplitPane.getDividers().get(0).positionProperty().addListener(e1->repaintSwingComponents());
	}
	
	@FXML
	private void repaintSwingComponents() {
		
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	
            	varnaSwingNode.getContent().repaint();
            	//Platform.runLater(() -> varnaSwingNode.requestFocus());
            	
            	if(contextProbabilitySwingNode.getContent() != null) {
            		contextProbabilitySwingNode.getContent().repaint();
            		//Platform.runLater(() -> contextProbabilitySwingNode.requestFocus());
            	}
            	
            }
        });
		
	}
	
	
	/**
	 * When the user clicks on an element in the table, we need to call certain
	 * routines to update the details on the right hand side
	 */
	private void setChoiceDependentContent(Integer row_index) {
		
		predictAndShowStructure(row_index);
		predictAndShowBPPM(row_index);
		predictAndShowContextProbabilities(row_index);
	}
	
	
	/**
	 * Uses CapR4J to predict and visualize the base pair probabilities
	 * @param row_index
	 */
	private void predictAndShowContextProbabilities(Integer row_index) {
		
		// Remove the structure when multiple items are selected
		Platform.runLater(() -> {
			
			if(this.contextProbabilitySwingNode.getContent() != null) {
				
				this.contextProbabilitySwingNode.getContent().removeAll();
				//contextProbabilitySwingNode.requestFocus();
				
			}
			
		});
		
		if (row_index != null) {
			
			// Get the sequence
			TableRowData row = this.poolTableView.getItems().get(row_index);
			int size = row.getSequence().get().length();
			
			// Predict
			capr.ComputeStructuralProfile(row.getSequence().get().getBytes(), size);
			
			// Convert format
			double[] raw = capr.getStructuralProfile();
			double[][] data = new double[size][6];
			
			for (int index=0; index<size; index++) {
					
					data[index][0] = raw[0*size + index];
					data[index][1] = raw[1*size + index];
					data[index][2] = raw[2*size + index];
					data[index][3] = raw[3*size + index];
					data[index][4] = raw[4*size + index];
					data[index][5] = 1 - data[index][0] - data[index][1] - data[index][2] - data[index][3] - data[index][4];
			}
			
			
			// Create metadata
			ArrayList<String> labels = new ArrayList<String>();
			for (int x=0; x<size; x++){ labels.add(""+x); }
			
			// Visualize
			SwingUtilities.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	            	
	            		Logo logo = new Logo( data , labels.toArray(new String[0]));
	            		logo.setAlphabetContexts();
	            		logo.setBit(false);
	            		
	            		ChartPanel logo_panel = logo.getLogoPanel();
	            		
	            		Dimension d = new Dimension(40*size, 125);
	            		
	            		logo_panel.setSize(d);
	            		logo_panel.setMinimumSize(d);
	            		logo_panel.setPreferredSize(d);
	            		
	            		//repaintSwingComponents();
	            		Platform.runLater(() -> {
	            			contextProbabilitySwingNode.setContent(logo_panel);
	            			//contextProbabilitySwingNode.requestFocus();
	            		});
	            }
	        });
			
		}
		
	}
	
	/**
	 * Implements the logic to predict and visualize the base pair 
	 * probability matrix 
	 * @param row_index
	 */
	private void predictAndShowBPPM(Integer row_index) {
		
		// Remove the structure when multiple items are selected
		Platform.runLater(() -> {
			this.bppmGridPane.getChildren().clear();
			bppmGridPane.getColumnConstraints().clear();
			bppmGridPane.getRowConstraints().clear();
		
			if (row_index != null) {
				
				// Get the sequence
				TableRowData rowdata = this.poolTableView.getItems().get(row_index);
				
				// Predict the structure
				double[] result = rnafoldapi.getBppm(rowdata.getSequence().get().getBytes());
			
				int size = rowdata.getSequence().get().length();
				String sequence = rowdata.getSequence().get();
				AptamerBounds bounds = pool.getAptamerBounds(rowdata.getId().getValue());
				
				ArrayList<Node> labels = new ArrayList<Node>(sequence.length()*2);
				
				// Labels
				for (int index = 0; index < size; index++) {
					
					Label n1 = new Label(""+sequence.charAt(index));
					n1.setFont(Font.font("monospace", FontWeight.BOLD, 10));
					
					
					Label n2 = new Label(""+sequence.charAt(index));
					n2.setFont(Font.font("monospace", FontWeight.BOLD, 10));
					
					// Primers
					if ( index<bounds.startIndex || index>=bounds.endIndex ) {

						n1.setTextFill(AptaColors.Nucleotides.PRIMERS);
						n2.setTextFill(AptaColors.Nucleotides.PRIMERS);
						
					}

					
					// Randomized Region
					else if ( index>=bounds.startIndex && index<bounds.endIndex) {
						n1.setTextFill(AptaColors.NucleotidesMap.get( (byte) sequence.charAt(index)));
						n2.setTextFill(AptaColors.NucleotidesMap.get( (byte) sequence.charAt(index)));
					}
					
					this.bppmGridPane.add(n1, 0, index);
					this.bppmGridPane.add(n2, index, 0);
					
					labels.add(n1);
					labels.add(n2);
					
				}
				
				// We need a hook to the one square
				StackPane sentinel = null;
				
				// Binding Probabilities
				for ( int index=0; index<result.length; index++ ) {
					
					int col = Index.triu_col(index, size);
					int row = Index.triu_row(index, size);
					
					StackPane square = new StackPane();
	                if (sentinel == null) sentinel = square;
					
	                
	                int cval = new Double(result[index]*255).intValue();
	                Color c = Color.rgb(cval,cval,cval).invert();	                
	                square.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
	                
	                this.bppmGridPane.add(square, col+1, row+1);	                
				}
				
		        for (int i = 1; i < size+1; i++) {
		        	bppmGridPane.getColumnConstraints().add(new ColumnConstraints(0, Control.USE_COMPUTED_SIZE, Double.POSITIVE_INFINITY, Priority.ALWAYS, HPos.CENTER, true));
		        	bppmGridPane.getRowConstraints().add(new RowConstraints(0, Control.USE_COMPUTED_SIZE, Double.POSITIVE_INFINITY, Priority.ALWAYS, VPos.CENTER, true));
		        }
				
		        StackPane squarel = sentinel;
		        
		        // Resize the nucleotide texts to match the square dimensions
		        sentinel.widthProperty().addListener( event -> changeFontSize(labels, squarel));
		        sentinel.heightProperty().addListener( event -> changeFontSize(labels, squarel));
		        
			}
		
			bppmGridPane.setPrefHeight(200);
			
		});
		
	}
	
	
	
	/**
	 * Changes the font size when resizing the BPPM Plot
	 * @param labels
	 * @param reference
	 */
	private void changeFontSize(List<Node> labels, StackPane reference) {
		for ( Node label : labels) {
    		
			reference.requestLayout();
    		
    		double newsize = Math.min(reference.getBoundsInParent().getWidth(), reference.getBoundsInParent().getHeight());
    		
    		// Set the new size
    		((Label) label).setFont(Font.font("monospace", FontWeight.BOLD, newsize+5));
    		
    	}
	}
	
	
	/**
	 * Implements the logic for predicting the secondary structure 
	 * and set the content when done
	 * @param row_index the row at which the aptamer to be predicted is located. If null,
	 * the secondary structure panel will be cleared
	 */
	private void predictAndShowStructure(Integer row_index) {
		
		// Remove the structure when multiple items are selected
		if (row_index == null) {
			
			Platform.runLater(() -> {
				this.varnaSequenceLabel.setText("");
				this.varnaStructureLabel.setText("");
				this.varnaMFELabel.setText("");
			});
			
			
			SwingUtilities.invokeLater( () -> {
	            	
	            	try {
						vp.drawRNA("", "");
						//Platform.runLater(() -> varnaSwingNode.requestFocus());
						
					} catch (ExceptionNonEqualLength e) {
						e.printStackTrace();
					}

	        });
				
		}
		else {
			
			// Get the sequence
			TableRowData row = this.poolTableView.getItems().get(row_index);
			
			// Predict the structure
			MFEData result = rnafoldapi.getMFE(row.getSequence().get().getBytes());
			
			SwingUtilities.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	            	
	            	try {
	            		vp.drawRNA(row.getSequence().get(), new String(result.structure), 3);
	            		//Platform.runLater(() -> varnaSwingNode.requestFocus());
					} catch (ExceptionNonEqualLength e) {
						e.printStackTrace();
					}

	            }
	        });
			
			Platform.runLater(() -> {
				this.varnaSequenceLabel.setText(row.getSequence().get());
				this.varnaStructureLabel.setText(new String(result.structure));
				this.varnaMFELabel.setText("Binding Free Energy: " + result.mfe + " kcal/mol");
			});
			
		}
		
	}
	
	private void initializePagination() {
		
		// Pagination properties
		pagination = new Pagination((this.max_items_int / rows_per_page.getValue() + 1), 0);
		pagination.setPageFactory(this::createPage);
		this.rows_per_page.addListener( (e) -> pagination.setPageCount(max_items_int / rows_per_page.get() ) );
		
		Platform.runLater(()->{
			
			// Remove any previous elements
			this.poolTableViewStackPane.getChildren().clear();
			
	        this.poolTableViewStackPane.getChildren().add(pagination);			
			
		});
		
	}
	
	/**
	 * Sets control element values for the table
	 */
	private void initializeControlElements() {
		
		// Bind total items to the integer property
		totalItemsLabel.textProperty().bind(this.total_items.asString());
		this.total_items.addListener( (e)-> this.total_items_int = this.total_items.get() );
		
		
		// Sort by options
		ObservableList<String> sort_options =  
			    FXCollections.observableArrayList(
			        "Count (desc)",
			        "Count (asc)",
			        "Enrichment (desc)",
			        "Enrichment (asc)"
			    );
		
		// Bind the cycle name to a variable so we can access it outside the JavaFX thread
		this.getOnCycleComboBox().valueProperty().addListener( e -> this.cycle_to_sort_by_string = this.onCycleComboBox.getSelectionModel().getSelectedItem());
		this.sortByComboBox.valueProperty().addListener( e -> this.criteria_to_sort_by_string = this.sortByComboBox.getSelectionModel().getSelectedItem());
		
		this.sortByComboBox.getItems().setAll(sort_options);
		this.sortByComboBox.getSelectionModel().selectFirst();
		
	
		// On cycle options
		for (int x=experiment.getSelectionCycles().size()-1; x>=0; x--) {
			
			// Skip non-existing cycles
			if (positive.get(x) == null && negative.get(x) == null && control.get(x) == null) continue;

			// Take care of the positive selections
			if (positive.get(x) != null) {

				 this.onCycleComboBox.getItems().add(positive.get(x).getName());
				
			}
			
			// Take care of the negative selections
			if (negative.get(x) != null) {
				
				for ( SelectionCycle cycle : negative.get(x)) {
					
					this.onCycleComboBox.getItems().add(cycle.getName());
					
				}
			
			}
			
			// Take care of the control selections
			if (control.get(x) != null) {
				
				for ( SelectionCycle cycle : control.get(x)) {
					
					this.onCycleComboBox.getItems().add(cycle.getName());
					
				}
			
			}
			
		}
		// Set last positive cycle as default
		this.onCycleComboBox.getSelectionModel().selectFirst();
		
		// Items per page
		this.itemsPerPageTextField.textProperty().bindBidirectional(this.rows_per_page, new NumberStringConverter());
		// force the field to be numeric only
		this.itemsPerPageTextField.textProperty().addListener(new ChangeListener<String>() {
			
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, 
		        String newValue) {
		        if (!newValue.matches("\\d*")) {
		        	Platform.runLater( () -> itemsPerPageTextField.setText(newValue.replaceAll("[^\\d]", "")));
		        }
		    }
		    
		});
		
		
		// Max items
		this.maxItemTextField.textProperty().bindBidirectional(this.max_items, new NumberStringConverter());
		// force the field to be numeric only
		this.maxItemTextField.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, 
		        String newValue) {
		        if (!newValue.matches("\\d*")) {
		        	// set the int version 
		        	max_items_int = Integer.parseInt(newValue.replaceAll("[^\\d]",""));
		        	// and the integerpropoerty 
		        	Platform.runLater( () -> maxItemTextField.setText(newValue.replaceAll("[^\\d]", "")));
		        }
		        max_items_int = Integer.parseInt(newValue);
		    }
		});
		
		max_items = new SimpleIntegerProperty(Math.min(100000, pool.size()));
		
		// Max items int initial value
		max_items_int = max_items.get();

		// Query 
		this.searchTextField.textProperty().addListener((e) -> this.query_string = this.searchTextField.getText());
	
		// With primers
		showPrimersRadioButton.selectedProperty().addListener( (e) -> this.with_primers = this.showPrimersRadioButton.selectedProperty().get());
		
	}
	
	
	
	/**
	 * Given the constraints from the controls, calculate the order in which the 
	 * aptamers will appear in the list 
	 */
	private void computeIndexOrder() {

		// Set the bit array to true at the locations where we want to consider the data
		SelectionCycle cycle_to_sort_by = experiment.getSelectionCycleById(cycle_to_sort_by_string);
		
		AptaLogger.log(Level.INFO, this.getClass(), "Preparing to sort data");
		
		// Reset the aptamer ids, they are guaranteed to be from 1 to aptamer_ids.length 
		for ( int x=0; x<aptamer_ids.length; x++) {
			
			aptamer_ids[x] = x+1;
			
		}
		
		// Reset the mask
		this.id_mask.clear();
		
		// Keep record to know how many items pass the filter
		int items_to_sort = 0;
		
		
		// we have to differentiate between counts and enrichment 
		if ( criteria_to_sort_by_string.contains("Count") ) {

			AptaLogger.log(Level.INFO, this.getClass(), "Retrieving data for selection cycle " + this.cycle_to_sort_by_string);
			
			// Temporarily store the counts in an array as random access to mapdb is slow but sequential access is fast
			int[] counts = new int[aptamer_ids.length];
			
			for ( Entry<Integer, Integer> item : cycle_to_sort_by.iterator() ) { 
			
				// since aptamers are one-indexed, we need to substract one
				this.id_mask.set(item.getKey()-1); 
				items_to_sort++;
				
				// Get the count
				counts[item.getKey()-1] = item.getValue();
				
			}
			
			AptaLogger.log(Level.INFO, this.getClass(), "Prioritizing data");
			
			// We can now move the items of this selection cycle to the front
			moveMaskedToFront(id_mask, aptamer_ids.length, aptamer_ids, counts);
			
			// And sort the ids according to the count data
			AptaLogger.log(Level.INFO, this.getClass(), "Sorting data");
			
			// now we can sort inline with custom comparator
			if ( this.sortByComboBox.getSelectionModel().getSelectedItem() == "Count (desc)" ) {
				
				class DescQSComparator implements QSComparator{
	
					@Override
					public int compare(int a, int b) {
						
						return Integer.compare(b,a);
					}
								
				}
				
				Quicksort.quicksort(aptamer_ids, counts, 0, items_to_sort-1, new DescQSComparator());
				
			}
			
			// now we can sort inline with custom comparator
			if ( this.sortByComboBox.getSelectionModel().getSelectedItem() == "Count (asc)" ) {
				
				class AscQSComparator implements QSComparator{
					
					@Override
					public int compare(int a, int b) {
						
						return Integer.compare(a,b);
					}
								
				}
				
				Quicksort.quicksort(aptamer_ids, counts, 0, items_to_sort-1, new AscQSComparator()); 
				
			}			
			
		}
		
		else { // Case for enrichment
			
			AptaLogger.log(Level.INFO, this.getClass(), "Retrievingfff data for selection cycle " + this.cycle_to_sort_by_string);
			
			SelectionCycle previous_cycle = cycle_to_sort_by.getPreviousSelectionCycle();
			double previous_cycle_size = previous_cycle.getSize();
			double cycle_size = cycle_to_sort_by.getSize();
			
			// We need to precompute the enrichment values 
			double[] enrichments = new double[aptamer_ids.length];
			
			// Create another BitSets to store membership of this cycle
			BitSet temp_mask = new BitSet(aptamer_ids.length);
			
			// We start by setting all enrichment values to the numerator of the enrichment term
			for ( Entry<Integer,Integer> item : cycle_to_sort_by.iterator() ) { 
			
				// since aptamers are one-indexed, we need to substract one
				temp_mask.set(item.getKey()-1);
				
				enrichments[item.getKey()-1] = ( item.getValue() / cycle_size );
				
			}
			
			// Now we iterate over the second cycle and add the denominator of the enrichment term
			for ( Entry<Integer,Integer> item : previous_cycle.iterator() ) { 
				
				// make sure want to compute the enrichment for this item
				if(temp_mask.get(item.getKey()-1)) {
					
					this.id_mask.set(item.getKey()-1);
					enrichments[item.getKey()-1] /= ( item.getValue() / previous_cycle_size );
					items_to_sort++;
					
				}
				
			}
			
			// Cleanup
			temp_mask = null;
			
			AptaLogger.log(Level.INFO, this.getClass(), "Prioritizing data");
			
			// We can now move the ids to the front for sorting
			moveMaskedToFront(id_mask, aptamer_ids.length, aptamer_ids, enrichments);
			
			// And finally sort the data
			if ( this.sortByComboBox.getSelectionModel().getSelectedItem() == "Enrichment (desc)" ) {
			
				class DescQSComparator implements QSDoubleComparator{
	
					@Override
					public int compare(double a, double b) {
	
						return Double.compare(b, a);
						
					}
								
				}
				
				Quicksort.quicksort(aptamer_ids, enrichments, 0, items_to_sort-1, new DescQSComparator());  
			
			}
		
			// now we can sort inline with custom comparator
			if ( this.sortByComboBox.getSelectionModel().getSelectedItem() == "Enrichment (asc)" ) {
				
				class AscQSComparator implements QSDoubleComparator{
	
					@Override
					public int compare(double a, double b) {
	
						return Double.compare(a, b);
						
					}
								
				}
				
				Quicksort.quicksort(aptamer_ids, enrichments, 0, items_to_sort-1, new AscQSComparator()); 
				
			}
			
		}
		
		Platform.runLater(() -> {
			// Inform the observable about the size
			this.total_items.set(aptamer_ids.length);
		
			// Make sure the Items per page is not larger than the total number of items
			this.itemsPerPageTextField.setText(""+Math.min(aptamer_ids.length, rows_per_page.get()));
			
		});

	}	
	
	
	/**
	 * Given an array and a mask, this function rearranges the elements
	 * in <code>arrays</code> such that at the end, all indices set to true
	 * in <code>mask</code> will be at the beginning. 
	 * @param mask 
	 * @param size
	 * @param int[]...arrays
	 */
	private void moveMaskedToFront(BitSet mask, int size, int[] ... arrays ) {
		
		// We look for 0s from the left and 1s from the right and swap them 
		int left = 0;
		int right = size-1;
		while (left<right-1) {
			
			// move to the right until we find a 0
			while( mask.get(left) ) left++;
			
			// move to the left until we find a 1
			while ( !mask.get(right) ) right--;
			
			// now swap
			mask.flip(left);
			mask.flip(right);
			
			for(int[] array : arrays) {
			
				int temp = array[left];
				array[left] = array[right];
				array[right] = temp;
				
			}
			
		}
		
	}
	
	/**
	 * Swapping logic for mixed, int and double arrays
	 * @param mask
	 * @param array1
	 * @param array2
	 */
	private void moveMaskedToFront(BitSet mask, int size, int[] int_array, double[] double_array , double[] ... double_arrays ) {
		
		// We look for 0s from the left and 1s from the right and swap them 
		int left = 0;
		int right = size-1;
		while (left<right-1) {
			
			// move to the right until we find a 0
			while( mask.get(left) ) left++;
			
			// move to the left until we find a 1
			while ( !mask.get(right) ) right--;
			
			// now swap
			mask.flip(left);
			mask.flip(right);
			
			int temp = int_array[left];
			int_array[left] = int_array[right];
			int_array[right] = temp;
			
			double dtemp = double_array[left];
			double_array[left]  = double_array[right];
			double_array[right] = dtemp;
			
			for (double[] darray : double_arrays) {
				
				dtemp = darray[left];
				darray[left]  = darray[right];
				darray[right] = dtemp;
				
			}
			
		}
		
	}
	
	/**
	 * Build the table columns according to the data
	 * Style the columns according to their content
	* @return
	*/
	private void createTable() {

		poolTableView = new TableView<>();
		
		// Configure properties
		poolTableView.setColumnResizePolicy ( TableView.UNCONSTRAINED_RESIZE_POLICY );
		poolTableView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		poolTableView.setStyle("-fx-font: 14px monospace;");
		
		// Aptamer ID
		TableColumn<TableRowData, Integer> id_column = new TableColumn<>("Id");
		id_column.setCellValueFactory(param -> param.getValue().getId());
		id_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		
		// Aptamer Sequence
		TableColumn<TableRowData, String> sequence_column = new TableColumn<>("Randomized Region");
		sequence_column.setCellValueFactory(param -> param.getValue().getSequence() );
		sequence_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		
		poolTableView.getColumns().setAll(id_column, sequence_column);
		
		// We need to style the sequence according to their dna color, so we add a custom
		// TableCell implementation to sequence_column
		sequence_column.setCellFactory(column -> {
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
								experiment.getAptamerPool().getAptamerBounds(this.getTableView().getItems().get(getIndex()).getId().getValue())
								);
						textHBox.setAlignment(Pos.CENTER_LEFT);

						setGraphic(textHBox);
						setHeight(textHBox.getPrefHeight());
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}
				}
			};
		});
		
		// Now add the selection cycles
		for (int x=experiment.getSelectionCycles().size()-1; x>=0; x--) {
			
			// Skip non-existing cycles
			if (positive.get(x) == null && negative.get(x) == null && control.get(x) == null) continue;

			// Take care of the positive selections
			if (positive.get(x) != null) {

				// Get the cycle instance
				SelectionCycle cycle = positive.get(x);
				
				// Create the column
				TableColumn cycle_column = generateColumn(cycle);
				
				// Add to round column
				poolTableView.getColumns().add(cycle_column);
				
			}
			
			// Take care of the negative selections
			if (negative.get(x) != null) {
				
				for ( SelectionCycle cycle : negative.get(x)) {
					
					// Create the column
					TableColumn cycle_column = generateColumn(cycle);
					
					// Add to round column
					poolTableView.getColumns().add(cycle_column);
					
				}
			
			}
			
			// Take care of the control selections
			if (control.get(x) != null) {
				
				for ( SelectionCycle cycle : control.get(x)) {
					
					// Create the column
					TableColumn cycle_column = generateColumn(cycle);
					
					// Add to round column
					poolTableView.getColumns().add(cycle_column);
					
				}
			
			}
			
		}
		
		// Add listeners for when user selects rows
		poolTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
		    if (newSelection != null) {
		    	
		    	// We want to predict and display the secondary structure only when one row is selected
		    	ObservableList<Integer> selected_indices = poolTableView.getSelectionModel().getSelectedIndices();
		    	
		    	if (selected_indices.size() == 1) {
		    		setChoiceDependentContent(selected_indices.get(0));
		    	}
		    	else {
		    		setChoiceDependentContent(null);
		    	}
		    	
		    }
		});
		
		
		
	}
	

	/**
	 * Create a column instance for a particular selection cycle
	 * @param cycle
	 * @return
	 */
	private TableColumn generateColumn(SelectionCycle cycle) {
		
		Insets header_padding = new Insets(0,10,0,10);
		
		// Create the main column
		TableColumn<TableRowData, Integer> cycle_column = new TableColumn<>();
		Label cycle_column_title = new Label(String.format( "Round no.%s, %s", cycle.getRound(), cycle.getName() ));
		cycle_column_title.setFont(table_font);
		cycle_column_title.setWrapText(false);
		cycle_column_title.setPadding(header_padding);
		cycle_column.setGraphic(cycle_column_title);
		
		// COUNT COLUMN
		TableColumn<TableRowData, Number> count_column = new TableColumn<>();
		
		Label title = new Label("Count");
		title.setFont(table_font);
		title.setWrapText(false);
		title.setPadding(header_padding);
		count_column.setGraphic(title);
	    double textwidth = fontMetrics.computeStringWidth(title.getText());
		
		count_column.setCellValueFactory(param -> param.getValue().getCount( cycle ));
		count_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		count_column.setPrefWidth(Math.max(count_column.getPrefWidth() , textwidth +  50) );	
	
		
		
		// FREQUENCY COLUMN
		TableColumn<TableRowData, Number> frequency_column = new TableColumn<>();
		Label title2 = new Label("Frequency");
		title2.setFont(table_font);
		title2.setWrapText(false);
		title2.setPadding(header_padding);
		frequency_column.setGraphic(title2);
	    textwidth = fontMetrics.computeStringWidth(title2.getText());
		
		frequency_column.setCellValueFactory(param -> param.getValue().getFrequency( cycle ));
		frequency_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		frequency_column.setPrefWidth(Math.max(frequency_column.getPrefWidth() , textwidth +  50) );

		
		// Put them together
		cycle_column.getColumns().addAll(count_column, frequency_column);
		
		// Do not add enrichment for first cycle 
		if (cycle.getPreviousSelectionCycle() != null) {
			
			TableColumn<TableRowData, Number> enrichment_column = new TableColumn<>();
			
			// This makes sure the width of the column is at least as wide as the label
			Label title3 = new Label("Enrichment");
			title3.setFont(table_font);
			title3.setWrapText(false);
			title3.setPadding(header_padding);
			enrichment_column.setGraphic(title3);
		    textwidth = fontMetrics.computeStringWidth(title3.getText());
		    
			enrichment_column.setCellValueFactory(param -> param.getValue().getEnrichment( cycle ));
			enrichment_column.setStyle( "-fx-alignment: CENTER-LEFT;");
			enrichment_column.setPrefWidth(Math.max(enrichment_column.getPrefWidth() , textwidth +  50) );					

			
			cycle_column.getColumns().add(enrichment_column);
		}
		
		return cycle_column;
		
	}
	
	 /**
	 * Build TextFlow with the sequence
	 * 
	 * @param sequence - the aptamer with primers
	 * @param bounds - the bounds destribing the start and end (exclusive) of the randomized region
	 * @return - TextFlow with dna colored according to its standard color and grayed primers 
	 */
	private HBox buildTextHBox(String sequence, AptamerBounds bounds) {
		
		// If search is on, we need to color all the matches as well
		BitSet matches = new BitSet(sequence.length());
		if (this.is_search) {
			
			Pattern p = Pattern.compile(query_string, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(new String(sequence).substring(bounds.startIndex, bounds.endIndex));
			
			while (m.find()) {
				
				for (int x=m.start(); x<m.end(); x++) { 
				
					
					matches.set(with_primers ? bounds.startIndex + x : x); 
					
				}
				
			}
			
		}
		
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
				
				if (this.is_search && matches.get(x)) {
					
					n.setBackground(new Background(new BackgroundFill(Color.web("#967c7c"), CornerRadii.EMPTY, Insets.EMPTY)));
					
				}
				
			}

			hb.getChildren().add(n);
			
		}
		
	    return hb;
	}	
	

	
	
	/**
	 * Creates the page with the content
	 * @param pageIndex
	 * @return
	 */
	private Node createPage(int pageIndex) {

		// Calculate the iterator position
        int fromIndex = pageIndex * rows_per_page.get();
        int toIndex = Math.min(fromIndex + rows_per_page.get(), this.max_items_int);
        
        // Populate data
        ObservableList<TableRowData> data = FXCollections.observableArrayList( new ArrayList<TableRowData>() );
        for (int x=fromIndex; x<toIndex; x++) {
        	
        	data.add(new TableRowData( aptamer_ids[x], new String(pool.getAptamer(aptamer_ids[x])), this));
        	
        }
        
        // Set Data
        poolTableView.setItems(data);

        return new BorderPane(poolTableView);
    }


	/**
	 * Redraws the table content when the configuration 
	 * has changed
	 */
	public void redrawTableContent() {
		
		Platform.runLater(() -> poolTableView.refresh());
		
	}
	
	
	/**
	 * Recomputes the aptamer id order based on the 
	 * user selections
	 */
	public void recomputeAptamerIds() {
		
		// Make sure we can sort
		// We cannot sort by enrichment if we do not have a previous selection cycle
		if ( criteria_to_sort_by_string.contains("Enrichment") && experiment.getSelectionCycleById(cycle_to_sort_by_string).getPreviousSelectionCycle() == null) {
			
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Sorting not possible");
			alert.setHeaderText("The selected sorting criteria is invalid.");
			alert.setContentText("The data cannot be sorted by the first selection cycle using the enrichment criteria.");

			alert.showAndWait();
			
			return;			
		}
		
		// We want the gui not to block on initialization, hence we wrap this into a progress pane
		ProgressPaneController pp = ProgressPaneController.getProgressPane(this.aptamerPoolRootStackPane, new Runnable() {
			
			@Override
			public void run() {
				
				computeIndexOrder();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Creating Table and Pagination");
				
				initializePagination();
				
			}
		
		});
		
		pp.setShowLogs(true);
		pp.run();

	}
	
	
	/**
	 * Performs the search when GO is pressed
	 */
	private void performSearch() {
		
		// Reset
		this.search_mask.clear();
		
		// Create regex expression
		Pattern p = Pattern.compile(query_string, Pattern.CASE_INSENSITIVE);

		// Iterate over the user specified limit of the table and set the mask when a match is found
		int number_of_matches = 0;
		for ( int x=0; x<this.max_items_int; x++ ) {
			
			AptamerBounds bounds = pool.getAptamerBounds(aptamer_ids[x]);
			String sequence = new String(pool.getAptamer(aptamer_ids[x])).substring(bounds.startIndex, bounds.endIndex);
			
			if (p.matcher(sequence).find() ) {
				
				search_mask.set(x);
				number_of_matches++;
				
			}
			
		}
		
		// Now update the order of the aptamer_ids to move all matches to the beginning
		// while preserving the order of the list
		int zeros = 0;
		int ones = 1;
		while(ones < this.max_items_int) {

			// Find the next 0
			while(search_mask.get(zeros) && zeros < this.max_items_int) {
				zeros++;
			}
			
			// Find the next one
			while(!search_mask.get(ones) && ones < this.max_items_int) {
				ones++;
			}

			if (ones == this.max_items_int) break;
			
			// And swap
			search_mask.flip(ones);
			search_mask.flip(zeros);
		
			int temp = aptamer_ids[zeros];
			aptamer_ids[zeros] = aptamer_ids[ones];
			aptamer_ids[ones] = temp;
			
		}

		
		// Finally we set the max items to match the number of identified
		int nom = number_of_matches;
		Platform.runLater( () -> {
			
			this.total_items.set(nom);
			this.maxItemTextField.setText(""+nom);
			
		});
		
	}
	
	/**
	 * Perform the logic for the search in the table
	 */
	@FXML
	private void goSearchButtonAction() {		
		
		is_search = true;
		
		// We want the gui not to block on initialization, hence we wrap this into a progress pane
		ProgressPaneController pp = ProgressPaneController.getProgressPane(this.aptamerPoolRootStackPane, new Runnable() {
			
			@Override
			public void run() {
				
				AptaLogger.log(Level.INFO, this.getClass(), "Searching for matches");
				
				performSearch();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Creating Table and Pagination");
				
				initializePagination();
				
			}
		
		});
		
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	/**
	 * Performs the logic to reset the search and bring the tab back to its
	 * initial state.
	 */
	@FXML
	private void resetSearchAction() {
		
		is_search = false;
		
		initialize();
		
	}
	
	/**
	 * @return the showPrimersRadioButton
	 */
	public RadioButton getShowPrimersRadioButton() {
		return showPrimersRadioButton;
	}


	/**
	 * @return the hidePrimersRadioButton
	 */
	public RadioButton getHidePrimersRadioButton() {
		return hidePrimersRadioButton;
	}


	/**
	 * @return the cmpRadioButton
	 */
	public RadioButton getCmpRadioButton() {
		return cmpRadioButton;
	}


	/**
	 * @return the rawCountsRadionButton
	 */
	public RadioButton getRawCountsRadionButton() {
		return rawCountsRadionButton;
	}


	/**
	 * @return the sortByComboBox
	 */
	public ComboBox<String> getSortByComboBox() {
		return sortByComboBox;
	}


	/**
	 * @return the onCycleComboBox
	 */
	public ComboBox<String> getOnCycleComboBox() {
		return onCycleComboBox;
	}
	
}
