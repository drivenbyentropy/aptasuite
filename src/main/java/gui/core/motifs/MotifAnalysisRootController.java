/**
 * 
 */
package gui.core.motifs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itextpdf.text.log.SysoCounter;

import exceptions.InformationNotFoundException;
import gui.activity.ProgressPaneController;
import gui.charts.logo.LogoChartPanelController;
import gui.core.Initializable;
import gui.wizards.structureprofileprediction.StructureProfilePredictionWizardController;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.aptatrace.AptaTraceMotif;
import utilities.AptaColors;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.GUIUtilities;
import utilities.Quicksort;
import utilities.SequenceTableUtils;

/**
 * @author Jan Hoinka
 * Contains the logic for the Motif GUI section of AptaSUITE
 */
public class MotifAnalysisRootController implements Initializable{

	@FXML
	private StackPane rootStackPane;
	
	@FXML
	private ComboBox<Integer> kmerSizeComboBox;
	
	@FXML
	private TextField alphaTextField;
	
	@FXML
	private ComboBox<Boolean> filterClusterComboBox;
	
	@FXML
	private TreeView resultTreeView;
	
	@FXML
	private StackPane noClusterInformationFoundStackPane;
	
	@FXML
	private AnchorPane motifSequenceLogoAnchorPane;
	
	@FXML
	private AnchorPane contextLogoAnchorPane;
	
	@FXML
	private RadioButton cmpRadioButton;
	
	@FXML
	private RadioButton rawCountsRadionButton;
	
	@FXML
	private StackPane sequenceTableStackPane;
	
	@FXML
	private RadioButton showPrimersRadioButton;
	
	@FXML
	private Label seedKmerLabel;
	
	@FXML
	private Label seedPValueLabel;
	
	@FXML
	private Label seedFrequencyLabel;
	
	@FXML
	private Label motifFrequencyLabel;
	
	@FXML
	private ListView<String> kmerListView;
	
	@FXML
	private AreaChart<Number,Number> motifCoverageAreaChart;
	
	@FXML
	private ComboBox<SelectionCycle> motifCoverageCycleComboBox;
	
	@FXML
	private LineChart<String,Double> cardinalityLineChart;
	
	@FXML
	private RadioButton plotCountsRadioButton;
	
	@FXML
	private StackPane motifCoverageStackPane;
	
	@FXML
	private CheckBox plotIncludeNegativeSelectionsCheckBox;
	
	/**
	 * Instance of the pagination for the table
	 */
	private Pagination pagination;
	
	/**
	 * The table instance
	 */
	private TableView<TableRowData> poolTableView;
	
	/**
	 * The number of rows per page
	 */
	private IntegerProperty rows_per_page = new SimpleIntegerProperty(500);
	
	/**
	 * Observable to keep track of the total number of aptamers 
	 */
	private IntegerProperty total_items = new SimpleIntegerProperty(0);
	
	/**
	 * Hook to the motif pwm controller
	 */
	private LogoChartPanelController motifController;
	
	/**
	 * Hook to the motif pwm controller
	 */
	private LogoChartPanelController contextController;
	
	/**
	 * Indicates if search is on or of to color the matching items in the table
	 */
	private boolean is_search = false;
	
	/**
	 * Sequence ids of aptamers in which the motif occurs
	 */
	private int[] motif_sequence_ids = new int[0];
	
	/**
	 * Indicates if primers are to be displayed or not
	 */
	private boolean with_primers = true;
	
	/**
	 * The strings to match in the table. currently they are the kmers of the motifs
	 */
	private String[] query_strings = null;
	
	/**
	 * The font used in the table
	 */
	private Font table_font = Font.font("monospace", FontWeight.BOLD, 14);
	
	private Experiment experiment = Configuration.getExperiment();
	
	private AptamerPool pool = experiment.getAptamerPool();
	
	private ArrayList<SelectionCycle> positive = experiment.getSelectionCycles();
	private ArrayList<ArrayList<SelectionCycle>> negative = experiment.getCounterSelectionCycles();
	private ArrayList<ArrayList<SelectionCycle>> control = experiment.getControlSelectionCycles();
	
	private Path resultPath = java.nio.file.Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "export", "aptatrace");
	
	private boolean isInitialized = false;
	
	public Boolean isInitialized() {
		
		return isInitialized;
		
	}
	
	public void initializeContent() {
		
		AptaLogger.log(Level.INFO,  this.getClass(), "Initializing Motif Analysis Content");
		
		initializeControlBars();

		// Temp, load this depending on the data available on disk
		ProgressPaneController pp = ProgressPaneController.getProgressPane(new Runnable() {
			
			@Override
			public void run() {
				loadMotifInformation();
				isInitialized = true;
			}
		
		}, this.rootStackPane);
		
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	/**
	 * Initializes any nodes in the control bars (top and right) which have dynamic content
	 */
	private void initializeControlBars() {
		
		// Determine all possible randomized region sizes from the selection cycles
		Set<Integer> sizes = new HashSet<Integer>();
		for (  Entry<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>> cycle : experiment.getMetadata().nucleotideDistributionAccepted.entrySet() ) {
			for ( Entry<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> size : cycle.getValue().entrySet()) {
				sizes.add(size.getKey());
			}
		}
		
		// Set possible values
		for (int x=2; x<sizes.stream().max(Integer::compare).get(); x++) {
			
			this.kmerSizeComboBox.getItems().add(x);
			
		}
		
		// Set the default according to the configuration
		this.kmerSizeComboBox.setValue(Configuration.getParameters().getInteger("Aptatrace.KmerLength", 6));

		// Alpha 
		alphaTextField.setText(Configuration.getParameters().getString("Aptatrace.Alpha", "10"));
		// force the field to be numeric only
		alphaTextField.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, 
		        String newValue) {
		        if (!newValue.matches("\\d*")) {
		        	Platform.runLater( () -> alphaTextField.setText(newValue.replaceAll("[^\\d]", "")));
		        }
		    }
		    
		});
		
		ObservableList<Boolean> list = FXCollections.observableArrayList(true,false);
		filterClusterComboBox.setItems(list);
		filterClusterComboBox.setConverter(new YesNoStringConverter());
		filterClusterComboBox.setValue(true);
		
		// Add changelistener to the TreeView
		resultTreeView.getSelectionModel().selectedItemProperty().addListener( new ChangeListener() {
		
			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
			
			    TreeItem selectedItem = (TreeItem) newValue;

			    if (selectedItem != null && selectedItem.isLeaf()) {
			    	
			    	// Make sure this is not a dummy 
			    	MotifFileHook item = ((MotifFileHook) selectedItem.getValue());
			    	
			    	if (!item.isDummy()) {
			    	
			    		// Update the UI if the user has selected a leaf
			    		updateMotifInformation(selectedItem);
			    	
			    	}
			    	
			    }
			    
			}
		
		});
		
		// Add the Sequence Motif Instance
		try {
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/charts/logo/LogoChartPanel.fxml"));
			AnchorPane logo = loader.load();
			
			motifController = (LogoChartPanelController) loader.getController(); 
			
			motifController.getRootPane().setMinHeight(Control.USE_PREF_SIZE);
			motifController.getRootPane().setPrefHeight(Control.USE_PREF_SIZE);
			motifController.getRootPane().setMaxHeight(Control.USE_PREF_SIZE);
			
			AnchorPane.setTopAnchor(logo, 0.0);
			AnchorPane.setBottomAnchor(logo, 0.0);
			AnchorPane.setRightAnchor(logo, 0.0);
			AnchorPane.setLeftAnchor(logo, 0.0);
			
			motifSequenceLogoAnchorPane.getChildren().add(logo);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// Add the Context Trace Instance
		try {
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/charts/logo/LogoChartPanel.fxml"));
			AnchorPane logo = loader.load();
			
			contextController = (LogoChartPanelController) loader.getController(); 
			
			AnchorPane.setTopAnchor(logo, 0.0);
			AnchorPane.setBottomAnchor(logo, 0.0);
			AnchorPane.setRightAnchor(logo, 0.0);
			AnchorPane.setLeftAnchor(logo, 0.0);
			
			contextLogoAnchorPane.getChildren().add(logo);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// And the table view
		createTable();
		
		// Add the cycles to the combobox in the motif coverage section
		this.motifCoverageCycleComboBox.getItems().addAll(experiment.getAllSelectionCycles());
		this.motifCoverageCycleComboBox.getSelectionModel().select(experiment.getSelectionCycles().get(experiment.getSelectionCycles().size()-1));
		
		// Bind disabled property of the counter selection box to whether it is available
		if (experiment.getCounterSelectionCycles().stream().filter( (item) -> item != null ).count() == 0 & experiment.getControlSelectionCycles().stream().filter( (item) -> item != null ).count() == 0 ) {
			
			plotIncludeNegativeSelectionsCheckBox.setDisable(true);
			
		}	
		
	}
	
	/**
	 * Updates the User Interface with all the information regarding the motif
	 * selected by the user
	 * @param selectedItem
	 */
	private void updateMotifInformation(TreeItem selectedItem) {
		
		Runnable logic = new Runnable() {

			@Override
			public void run() {
				
				// reset the cardinality chart
				Platform.runLater( ()->{ 
					
					cardinalityLineChart.getData().clear();
					
				});
				
				
				AptaLogger.log(Level.INFO,  this.getClass(), "Retrieving Motif Information");
				
				// TODO Auto-generated method stub
				MotifFileHook item = ((MotifFileHook) selectedItem.getValue());
				
				// Add General Information
				Platform.runLater(() -> {
					seedKmerLabel.setText(item.getKmers()[0]);
					seedPValueLabel.setText(String.format("%.5f%%", item.getPvalue()));
					seedFrequencyLabel.setText(String.format("%.2f%%", item.getSeed_frequency()));
					motifFrequencyLabel.setText("N/A");
					
					// Add aligned kmers
					
					kmerListView.getItems().setAll(item.getKmers_aligned());
				});
				
				// Plot the PWM
				Platform.runLater(() -> {
					item.setPwmData(motifController);
					motifController.createChart();
				});
				
				// Plot the Context Trace
				Platform.runLater(() -> {
					item.setContextData(contextController);
					contextController.createChart();
				});
						
				// Create the sequence table
				motif_sequence_ids = item.getSequenceIds();
				query_strings = item.getKmers();
				is_search = true;
				
				initializePagination();
				
				// Create distribution plot as a separate thread
				ProgressPaneController ppc_dist = ProgressPaneController.getProgressPane(null, motifCoverageStackPane);
				
				Runnable logic = new Runnable() {

					@Override
					public void run() {

						updateDistributionPlot(ppc_dist);
						
					}
					
				};
				
				ppc_dist.setLogic(logic);
				ppc_dist.setShowProgressBar(true);
				ppc_dist.setShowLogs(false);
				ppc_dist.run();
				
			}
			
		};
		
		ProgressPaneController ppc = ProgressPaneController.getProgressPane(logic, rootStackPane);
		ppc.setShowLogs(true);
		ppc.run();
		
	}
	
	/**
	 * Searches for motif information on disk and initiates the panels accordingy
	 */
	public void loadMotifInformation() {
		
		// first see if we have motif information present
		if (this.resultPath.toFile().exists()) {
			
			TreeItem root = new TreeItem("Motifs");
			
			// iterate over folders
			ArrayList<TreeItem> parameter_combinations = new ArrayList<TreeItem>();
			
			try {
				Files.list(this.resultPath).forEach( (p) -> {
					
					// Get K and alpha from string
					Matcher matcher = Pattern.compile("\\d+").matcher(p.getFileName().toString());
					ArrayList<String> matches = new ArrayList<String>();
					while(matcher.find()) {  matches.add(matcher.group());  }
					String name = String.format("AptaTrace k=%s alpha=%s", matches.get(0), matches.get(1));
					
					TreeItem<MotifFileHook> item = new TreeItem(name);
					item.setExpanded(true);
					
					// Add to list 
					parameter_combinations.add(item);
					
					// Now add all motifs to the view
					try (final DirectoryStream<Path> stream = Files.newDirectoryStream(p, "motif_*")) {
						
						HashMap<Integer, TreeItem<MotifFileHook>> motifs = new HashMap<Integer, TreeItem<MotifFileHook>>(); 
						
					    stream.forEach( e -> {
					    	
					    	// Skip non-useful files
					    	if (e.getFileName().toString().endsWith("txt")) {
						    					
						    	//First extract the motif id
						    	String[] filename_tokens = e.getFileName().toString().split("_");

						    	
						    	if (filename_tokens[1].endsWith("txt")) {
						    		filename_tokens[1] = filename_tokens[1].split("\\.")[0];  
						    	}
						    	
						    	Integer id = Integer.parseInt(filename_tokens[1]);
						    	
						    	// Create instance if not present
						    	if (!motifs.containsKey(id)) {
						    		motifs.put(id, new TreeItem(new MotifFileHook(id)));
						    	}
						    	
						    	if (filename_tokens.length == 3) {
							    	
							    	String type = filename_tokens[2];
							    	
							    	// Now add the information to the instance
							    	switch(type) {
							    	
								    	case "aptamers.txt": 	motifs.get(id).getValue().setAptamers(e);
								    							break;
							    	
								    	case "context.txt": 	motifs.get(id).getValue().setContext(e);
		    													break;
		    							
								    	case "pwm.txt": 		motifs.get(id).getValue().setPwm(e);
		    													break;
							    	
							    	}
							    	
						    	}
						    	
						    	else {
						    		
						    		motifs.get(id).getValue().setGeneral(e);
						    		
						    	}
					    	}
					    	
					    });
					    
					    // Finally add to the TreeView
					    item.getChildren().addAll(motifs.values());
					    
					    // Add a dummy if no motifs were found
					    if (motifs.isEmpty()) {
					    	MotifFileHook dummy_hook = new MotifFileHook(0);
					    	dummy_hook.setIsDummy(true);
					    	item.getChildren().add(new TreeItem(dummy_hook));
					    }
					    
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				});
				
				root.getChildren().addAll(parameter_combinations);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Update the UI
			Platform.runLater( () -> {
				
				// Clear any previous tree structure
				this.resultTreeView.setRoot(null);

				
				noClusterInformationFoundStackPane.setVisible(false);
				
				this.resultTreeView.setRoot(root);
				this.resultTreeView.setShowRoot(false);
				
			});
			
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
		    	
		    	setChoiceDependentContent(selected_indices);
		    	
		    }
		});
		
		// enable copy/paste
		new SequenceTableUtils(poolTableView, 1, showPrimersRadioButton);
		
		
	}
	
	
	/**
	 * When the user clicks on an element in the table, we need to call certain
	 * routines to update the details on the right hand side
	 */
	private void setChoiceDependentContent(ObservableList<Integer> selected_indices) {
		
		showCardinalityPlots(selected_indices);
		
	}
	
	/**
	 * Helper function to redraw the plot when user changes from enrichment to counts
	 */
	@FXML
	private void showCardinalityPlots() {
		
		showCardinalityPlots(poolTableView.getSelectionModel().getSelectedIndices());
		
	}
	
	
	/**
	 * Plots either the enrichment or the counts of the selected aptamers
	 * @param selected_indices
	 */
	private void showCardinalityPlots(ObservableList<Integer> selected_indices) {
		
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				
				ArrayList<SelectionCycle> selectionCycles = plotIncludeNegativeSelectionsCheckBox.isSelected() ? experiment.getAllSelectionCycles() : experiment.getSelectionCycles();
				
				// Iterate over the selected indices, get the aptamer information and plot
				ObservableList<XYChart.Series<String,Double>> series = FXCollections.observableList(new ArrayList<XYChart.Series<String,Double>>());
				
				for (Integer index : selected_indices) {
					
					// we need the aptamer ids
					TableRowData row = poolTableView.getItems().get(index);
					
					XYChart.Series dataSeries = new XYChart.Series();
			        dataSeries.setName("Id " + row.getId().getValue());
					
			        for (SelectionCycle cycle : selectionCycles) {
			        	
			        	// Skip non-existing cycle 
			        	if (cycle == null) continue;
			        	
			        	if(plotCountsRadioButton.isSelected()) {
			        		
			        		dataSeries.getData().add(new XYChart.Data(cycle.getName(), row.getCount(cycle).getValue().doubleValue()));
			        		
			        	}
			        	else {
			        		
			        		// Skip first cycle
			        		if (cycle.getPreviousSelectionCycle() == null) continue;
			        		
			        		dataSeries.getData().add(new XYChart.Data(String.format("%s -> %s", cycle.getPreviousSelectionCycle().getName(), cycle.getName()), row.getEnrichment(cycle).getValue().doubleValue()));
			        		
			        	}
			        	
			        }
			        
			        series.add(dataSeries);
					
				}
				
				Platform.runLater(() -> {
		        	cardinalityLineChart.getData().setAll(series);
		        });
			}
			
		});
		
		t.setName("chartThread");
		t.setDaemon(true);
		t.start();
		
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
	    double textwidth = GUIUtilities.computeStringWidth(title.getText(), table_font);
		
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
	    textwidth = GUIUtilities.computeStringWidth(title2.getText(), table_font);
		
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
		    textwidth = GUIUtilities.computeStringWidth(title3.getText(), table_font);
		    
			enrichment_column.setCellValueFactory(param -> param.getValue().getEnrichment( cycle ));
			enrichment_column.setStyle( "-fx-alignment: CENTER-LEFT;");
			enrichment_column.setPrefWidth(Math.max(enrichment_column.getPrefWidth() , textwidth +  50) );					

			
			cycle_column.getColumns().add(enrichment_column);
		}
		
		return cycle_column;
		
	}
	
	private void initializePagination() {
		
		// Pagination properties
		pagination = new Pagination((this.motif_sequence_ids.length / rows_per_page.getValue() + 1), 0);
		pagination.setPageFactory(this::createPage);
		this.rows_per_page.addListener( (e) -> pagination.setPageCount(this.motif_sequence_ids.length / rows_per_page.get() ) );
		
		Platform.runLater(()->{
			
			// Remove any previous elements
			this.sequenceTableStackPane.getChildren().clear();
			
	        this.sequenceTableStackPane.getChildren().add(pagination);			
			
		});
		
	}
	
	
	
	/**
	 * Creates the page with the content
	 * @param pageIndex
	 * @return
	 */
	private Node createPage(int pageIndex) {

		// Calculate the iterator position
        int fromIndex = pageIndex * rows_per_page.get();
        int toIndex = Math.min(fromIndex + rows_per_page.get(), this.motif_sequence_ids.length);
        
        // Populate data
        ObservableList<TableRowData> data = FXCollections.observableArrayList( new ArrayList<TableRowData>() );
        for (int x=fromIndex; x<toIndex; x++) {
        	
        	data.add(new TableRowData( motif_sequence_ids[x], new String(pool.getAptamer(motif_sequence_ids[x])), this));
        	
        }
        
        // Set Data
        poolTableView.setItems(data);

        return new BorderPane(poolTableView);
    }

	
	/**
	 * Redraws the table content when the configuration 
	 * has changed
	 */
	@FXML
	private void redrawTableContent() {
		
		Platform.runLater(() -> {
			
			poolTableView.refresh();
			
		});
		
	}

	
	/**
	 * Redraws the table content and plots when the configuration 
	 * has changed
	 */
	@FXML
	private void redrawTableAndPlotContent() {
		
		Platform.runLater(() -> {
			
			poolTableView.refresh();
			
			ProgressPaneController ppc = ProgressPaneController.getProgressPane(null, motifCoverageStackPane);
			
			Runnable logic = new Runnable() {

				@Override
				public void run() {

					updateDistributionPlot(ppc);
					
				}
				
			};
			
			ppc.setLogic(logic);
			ppc.setShowProgressBar(true);
			ppc.setShowLogs(false);
			ppc.run();
			
			
		});
		
	}
	
	/**
	 * Redraws the table content and plots when the configuration 
	 * has changed
	 */
	@FXML
	private void redrawDistributionPlotContent() {
		
		Platform.runLater(() -> {
			
			ProgressPaneController ppc = ProgressPaneController.getProgressPane(null, motifCoverageStackPane);
			
			Runnable logic = new Runnable() {

				@Override
				public void run() {

					updateDistributionPlot(ppc);
					
				}
				
			};
			
			ppc.setLogic(logic);
			ppc.setShowProgressBar(true);
			ppc.setShowLogs(false);
			ppc.run();
			
			
		});
		
	}
	
	/**
	 * Goes through the aptamers and generates a distribution chart of the motif
	 * occurrences on the aptamers
	 */
	@FXML
	private void updateDistributionPlot(ProgressPaneController pp) {
		
		// Dont do anything if nothing has been selected yet
		if (query_strings == null) {
			
			return;
			
		}
		
		SelectionCycle reference_cycle = this.motifCoverageCycleComboBox.getValue();
		
		// Key=Index, Value=Counts
		Map<Integer,Double> occurences = new HashMap<Integer,Double>();
		final AtomicInteger max_rand_size = new AtomicInteger(0);

		// Create one pattern per kmer
		Pattern[] patterns = new Pattern[query_strings.length];
		
		for (int x=0; x<patterns.length; x++) {
						
			patterns[x] = Pattern.compile(query_strings[x], Pattern.CASE_INSENSITIVE);
			
		}

		// Sort the aptamer ids
		int[] sorted_ids = new int[motif_sequence_ids.length];
		for (int x=0; x<sorted_ids.length; sorted_ids[x] = motif_sequence_ids[x++]);
		Quicksort.sort(sorted_ids);
		
		
		AtomicInteger progress = new AtomicInteger(0);
		
		Runnable logic = new Runnable() {

			/**
			 * Subroutine doing the actual counting
			 */
			private void processData(	Entry<Integer, byte[]> pool_entry, 
										Entry<Integer, int[]> bounds_entry, 
										Entry<Integer, Integer> cardinality_entry  ) {
				
				// Extract data
				int[] bounds = bounds_entry.getValue();
				String sequence = new String(pool_entry.getValue()).substring(bounds[0], bounds[1]);
				
				max_rand_size.set( Math.max(max_rand_size.get(), bounds[1]-bounds[0]) );
				
				// Search for matches
				for ( Pattern p : patterns) {
					
					if (is_search) {
						
						Matcher m = p.matcher(sequence);
						
						while (m.find()) {
							
							for (int x=m.start(); x<m.end(); x++) { 
							
								// Update counts
								Double current = occurences.putIfAbsent(x, 0.0);
								if (current != null) {
									
									int count = cardinality_entry.getValue();
									
									double contribution = cmpRadioButton.isSelected() ? (count / (double) reference_cycle.getSize()) * 1000000 : count;
									
									occurences.put(x, current + contribution);
									
								}
								
							}
							
						}
						
					}
				}
				
			}
			
			@Override
			public void run() {
				
				// Get through the data and count occurences
				Iterator<Entry<Integer, byte[]>> pool_it = pool.inverse_view_iterator().iterator();
				Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
				Iterator<Entry<Integer, int[]>> bounds_it = pool.bounds_iterator().iterator();
				
				Entry<Integer, byte[]> pool_entry = pool_it.next();
				Entry<Integer, int[]> bounds_entry = bounds_it.next();
				Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
				
				
				for ( int id : sorted_ids ) {
					
					progress.incrementAndGet();
					
					// Forward the pool iterator
					while( pool_entry.getKey() != id) { 
						pool_entry = pool_it.next(); 
						bounds_entry = bounds_it.next();
					}
					
					
					// Check if we have a match from a forwarded cycle iterator
					if (cardinality_entry.getKey().equals(id)) {
						
						processData(pool_entry, bounds_entry, cardinality_entry);
						if (cardinality_it.hasNext()) {
							
							cardinality_entry = cardinality_it.next();
							
						}
						else { // we are done
							
							break;
							
						}
						
					} else if (((int) cardinality_entry.getKey()) < id ) { // have found a future match
						
						while(((int) cardinality_entry.getKey()) < id && cardinality_it.hasNext() ) {
							
							cardinality_entry = cardinality_it.next(); 
							
						}
						
						if (cardinality_entry.getKey().equals(id)) {
							
							processData(pool_entry, bounds_entry, cardinality_entry);
							if (cardinality_it.hasNext()) {
								
								cardinality_entry = cardinality_it.next();
								
							}
							else { // we are done
								
								break;
								
							}
							
						}
						
					}
					
					
				}
				
			}
			
		};
		
		Thread logic_thread = new Thread(logic);
		logic_thread.start();
		
		while (logic_thread.isAlive() && !logic_thread.isInterrupted()) {
			try {
				pp.setProgress(progress.doubleValue()/sorted_ids.length);
				pp.setProgressLabel(String.format("%.2f%%", (progress.doubleValue()/sorted_ids.length)*100.0) );
				
				// Once every half a second should suffice
				Thread.sleep(500);
				
			} catch (InterruptedException ie) {
				break;
			}
		}

		// Last Update
		pp.setProgress(1.0);
		pp.setProgressLabel(String.format("%.2f%%", 100.0));
		
		// Add zero values to non-matching indices
		for (int x=0; x<max_rand_size.get(); x++) {
			
			occurences.putIfAbsent(x, 0.0);
			
		}
		
		// Get primitive arrays of keys and values
		int[] indexes = occurences.keySet().stream().mapToInt(i->i).toArray();
		double[] values  = occurences.values().stream().mapToDouble(i->i).toArray();
		
		// Create series for the chart 
		XYChart.Series<Number, Number> occ_series = new XYChart.Series<Number, Number>();
		for (int x=0; x<values.length; x++) {
			
			occ_series.getData().add(new XYChart.Data(indexes[x], values[x]));
			
		}
		
		Platform.runLater(() -> {
			this.motifCoverageAreaChart.getData().setAll(occ_series);
			this.motifCoverageAreaChart.getXAxis().setLabel("Randomized Region Index");
			this.motifCoverageAreaChart.getYAxis().setLabel("Motif Coverage" + (this.cmpRadioButton.isSelected() ? " (CMP)" : ""));
		});
	}
	
	
	@FXML
	public void runAptaTrace() {
		
		AtomicBoolean motifs_present = new AtomicBoolean(true);
		AtomicBoolean prediction_done = new AtomicBoolean(false);
		AtomicBoolean interrupted = new AtomicBoolean(false);
		
		Runnable loadClusterInfo = new Runnable() {

			@Override
			public void run() {
				
				// Make sure we have structure information at this point
				if (experiment.getStructurePool() == null)
				{
					try {
						experiment.instantiateStructurePool(false, false);
					}
					catch (InformationNotFoundException e) {
						motifs_present.set(false);
					}
					
				}
				
				if (!motifs_present.get()) { // and if not, run the prediction routine
					
			
					Platform.runLater(() -> {
						
						Alert alert = new Alert(AlertType.CONFIRMATION);
						alert.setTitle("Structure Profiles Required");
						alert.setHeaderText("No secondary structure profiles where found for this experiment.\nThis information is required to proceed with AptaTRACE.");
						alert.setContentText("Predict the profiles now (this might take while)?");
			
						Optional<ButtonType> result = alert.showAndWait();
						if (result.get() == ButtonType.OK){
						    
							Parent root;
					        try {
					        	
					        	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/structureprofileprediction/StructureProfilePredictionWizard.fxml"));
					        	
					            root = loader.load();
					            StructureProfilePredictionWizardController controller = (StructureProfilePredictionWizardController) loader.getController();
					            
					            
					            Stage stage = new Stage();
					            stage.setTitle("Secondary Structure Profile Prediction");
					            stage.setResizable(false);
					            stage.setScene(new Scene(root, 420, 160));
					    		stage.setOnCloseRequest((e) -> { e.consume(); });
					            stage.show();
					            
					            controller.setInterrupted(interrupted);
					            controller.setCompleted(prediction_done);
					            controller.start();
					            
					            
					        }
					        catch (IOException e) {
					            e.printStackTrace();
					        }
							
						} else { 
							interrupted.set(true); 
						}
					
					});
					
					// We need to make sure we wait until the wizard above has completed
					while(!interrupted.get() && !prediction_done.get()) {
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					
					// If we do not have structure data until here, we cannot run aptatrace
					if (interrupted.get()) {
						
						return;
						
					}
					
				}	
				
				AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaTRACE");
				
				AptaTraceMotif aptatrace =new AptaTraceMotif(
						experiment,
						Configuration.getParameters().getString("Experiment.projectPath"),
						kmerSizeComboBox.getValue(),
						filterClusterComboBox.getValue(),
						true,
						Integer.parseInt(alphaTextField.getText())
				); 
				
				aptatrace.run();
				
				// Update the UI
				loadMotifInformation();
				
			}		
		};
		
		ProgressPaneController pp = ProgressPaneController.getProgressPane( loadClusterInfo, rootStackPane);  
		pp.setShowLogs(true);
		pp.run();

		
	}
	
	public class YesNoStringConverter extends StringConverter<Boolean> {
        @Override
        public String toString(Boolean bool) {
            return (bool?"Yes":"No");
        }

        @Override
        public Boolean fromString(String s) {
            return s.equalsIgnoreCase("yes");
        }
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
			
			// create one pattern per kmer
			for (String query_string : this.query_strings) {
				
				Pattern p = Pattern.compile(query_string, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(new String(sequence).substring(bounds.startIndex, bounds.endIndex));
				
				while (m.find()) {
					
					for (int x=m.start(); x<m.end(); x++) { 
					
						
						matches.set(with_primers ? bounds.startIndex + x : x); 
						
					}
					
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
					
					n.setBackground(new Background(new BackgroundFill(Color.web("#c5e0d9"), CornerRadii.EMPTY, Insets.EMPTY)));
					
				}
				
			}

			hb.getChildren().add(n);
			
		}
		
	    return hb;
	}	
	
	
	/**
	 * @return the cmpRadioButton
	 */
	public RadioButton getCmpRadioButton() {
		return cmpRadioButton;
	}

	/**
	 * @param cmpRadioButton the cmpRadioButton to set
	 */
	public void setCmpRadioButton(RadioButton cmpRadioButton) {
		this.cmpRadioButton = cmpRadioButton;
	}

	/**
	 * @return the rawCountsRadionButton
	 */
	public RadioButton getRawCountsRadionButton() {
		return rawCountsRadionButton;
	}

	/**
	 * @param rawCountsRadionButton the rawCountsRadionButton to set
	 */
	public void setRawCountsRadionButton(RadioButton rawCountsRadionButton) {
		this.rawCountsRadionButton = rawCountsRadionButton;
	}
	
}
