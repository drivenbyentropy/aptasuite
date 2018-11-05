/**
 * 
 */
package gui.core.aptamer.family.analysis;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.mapdb.Serializer;

import exceptions.InformationNotFoundException;
import gui.activity.ProgressPaneController;
import gui.charts.logo.Alphabet;
import gui.charts.logo.LogoChartPanelController;
import gui.charts.logo.Scale;
import gui.core.Initializable;
import gui.misc.FXConcurrent;
import gui.wizards.aptamut.AptaMutRootController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lib.aptacluster.AptaCluster;
import lib.aptacluster.HashAptaCluster;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.GenericStorage;
import lib.aptamer.datastructures.MapDBGenericStorage;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.AptaColors;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.GUIUtilities;
import utilities.Quicksort;
import utilities.SequenceTableUtils;

/**
 * @author Jan Hoinka
 * Root Controller of the aptamer familiy analysis tab
 */
public class AptamerFamilyAnalysisRootController implements Initializable{

	@FXML
	private ComboBox<Integer> randomizedRegionSizeComboBox;
	
	@FXML
	private ComboBox<Integer> localitySensitiveHashingDimensionComboBox;
	
	@FXML
	private ComboBox<Integer> localitySensitiveHashingIterationsComboBox;
	
	@FXML
	private TextField editDistanceTextField;
	
	@FXML
	private TextField kmerSizeTextField;
	
	@FXML
	private TextField kmerCutoffIterationsTextField;
	
	@FXML
	private StackPane rootStackPane;
	
	@FXML
	private BorderPane clusterBorderPane;
	
	@FXML
	private RadioButton clusterOrderSizeRadioButton;
	
	@FXML 
	private ComboBox<SelectionCycle> referenceSelectionCycleComboBox;
	
	@FXML
	private Label noClustersFoundLabel;
	
	@FXML
	private StackPane clusterTableStackPane;
	
	@FXML
	private RadioButton showPrimersRadioButton;
	
	@FXML
	private RadioButton rawCountsRadionButton;
	
	@FXML 
	private RadioButton cmpRadioButton;
	
	@FXML
	private StackPane sequenceTableStackPane;
	
	@FXML
	private StackPane sequenceLogoStackPane;
	
	@FXML
	private StackedBarChart<String, Number> mutationStackedBarchart;
	
	@FXML
	private ComboBox<SelectionCycle> clusterEnrichmentCompareToCycleComboBox;
	
	@FXML
	private ScatterChart<Number,Number> clusterComparisonScatterChart;
	
	@FXML
	private Button clusterComparisonGoButton;
	
	@FXML
	private LineChart<String, Double> cardinalityLineChart;
	
	@FXML
	private RadioButton plotCountsRadioButton;
	
	@FXML
	private StackPane clusterSequenceLogoStackPane;
	
	@FXML
	private StackPane mutationRatesStackPane;
	
	@FXML
	private StackPane clusterEnrichmentStackPane;
	
	@FXML
	private StackPane cardinalityChartStackPane;
	
	@FXML
	private StackPane noClusterInformationFoundStackPane;
	
	@FXML
	private RadioButton clusterEnrichmentScaleLogarithmicRadioButton;
	
	@FXML
	private GridPane aptaMutGridPane;
	
	@FXML
	private ComboBox<SelectionCycle> aptaMutReferenceCycleComboBox;
	
	@FXML
	/**
	 * The left cluster table instance
	 */
	private TableView<ClusterTableRowData> clusterTableView;
	
	@FXML
	private BarChart<String, Number> clusterCardinalityBarChart;
	
	@FXML
	private StackPane clusterCardinalityBarChartStackPane;
	
	@FXML
	private RadioButton showClusterSizesRadioButton;
	
	@FXML
	private CheckBox plotIncludeNegativeSelectionsCheckBox;
	
	@FXML
	private ToggleGroup clusterOrderToggleGroup;
	
	/**
	 * Instance of the pagination for the cluster table
	 */
	private Pagination clusterPagination;
	
	
	
	/**
	 * Instance of the pagination for the sequence table
	 */
	private Pagination sequencePagination;
	
	/**
	 * The sequence table instance
	 */
	private TableView<SequenceTableRowData> sequenceTableView;
	
	/**
	 * If true, the user has selected Diversity as sorting criteria the
	 * last time he pressed GO
	 */
	private boolean cluster_sorting_criteria_diversity = false;
	
	/**
	 * Stores the total number of clusters on file
	 */
	private Integer numberOfClusters = null; 
	
	private Experiment experiment = Configuration.getExperiment();
	private AptamerPool pool = experiment.getAptamerPool();
	private ClusterContainer clusters = experiment.getClusterContainer();
	
	private ArrayList<SelectionCycle> all_cycles = experiment.getAllSelectionCycles();
	private ArrayList<SelectionCycle> positive = experiment.getSelectionCycles();
	private ArrayList<ArrayList<SelectionCycle>> negative = experiment.getCounterSelectionCycles();
	private ArrayList<ArrayList<SelectionCycle>> control = experiment.getControlSelectionCycles();
	
	/**
	 * Contains the cluster ids in the order defined by the user interface
	 */
	private int[] cluster_ids;
	
	/**
	 * Stores the sorting criteria of the clusters
	 * Either count or diversity
	 */
	private int[] cardinalities;
	
	/**
	 * The aptamer ids corresponding to the cluster the user has selected
	 */
	private int[] aptamer_ids;
	
	/**
	 * Bitset representation of the cluster members in the selected reference cycle
	 */
	BitSet cluster_membership = new BitSet(experiment.getAptamerPool().size());

	/**
	 * Bitset representation of the cluster members for the entire pool
	 */
	BitSet pool_cluster_membership = new BitSet(experiment.getAptamerPool().size());
	
	/**
	 * the total number of items to display in the cluster table
	 */
	private int total_cluster_table_items;
	
	/**
	 * The number of items per page of the cluster table
	 */
	private int rows_per_cluster_table_page = 100;

	/**
	 * The number of items per page of the cluster table
	 */
	private int rows_per_sequence_table_page = 1000;
	
	/**
	 * The font used in the table
	 */
	private Font table_font = Font.font("monospace", FontWeight.BOLD, 14);

	
	/**
	 * True if the user has search of clusters containing aptamers with a particular regular expression
	 * TODO: Make this an option in the GUI 
	 */
	private boolean is_search = false;
	
	/**
	 * The string to be converted into a regular expression when is_search is true
	 */
	private String query_string;
	
	private boolean isInitialized = false;
	

	/**
	 * Global access to the thread computing the logo and mutation chart data
	 * Access is required because the thread need to be stopped if the user selects
	 * a different cluster before these computations are completed.
	 */
	private Thread logo_thread = null;
	
	/**
	 * Used to prematurely terminate computations if the user changes clusters
	 */
	private boolean logoThreadInterrupted = false;
	
	/**
	 * Global access to the thread computing the cycle cardinality chart data
	 * Access is required because the thread need to be stopped if the user selects
	 * a different cluster before these computations are completed.
	 */
	private Thread cardinality_thread = null;
	
	/**
	 * Used to prematurely terminate computations if the user changes clusters
	 */
	private boolean cardinalityThreadInterrupted = false;
	
	
	/**
	 * Lazy loading caches for various data in this view 
	 */
	//Key: [CycleID]$["Size"|"Diversity"]
	private GenericStorage<String, int[]> lazyCacheClusterIDs = null;
	private GenericStorage<String, int[]> lazyCacheClusterCardinalities = null;
	
	//Key: ClusterID
	private GenericStorage<Integer, int[]> lazyCacheAptamerIDs = null;
	private GenericStorage<Integer, byte[]> lazyCachePoolClusterMembership = null;
	private GenericStorage<Integer, byte[]> lazyCacheClusterMembership = null;
	
	//Key: ClusterID
	private GenericStorage<Integer, double[]> lazyCacheLogoData = null;
	private GenericStorage<Integer, double[]> lazyCacheMutationData = null;
	
	//Key: ClusterID
	private GenericStorage<Integer, long[]> lazyCacheClusterCardinalityData = null;
	
	public Boolean isInitialized() {
		
		return isInitialized;
		
	}
	
	
	public void initializeContent() {
		
		initializeControlBars();
		
		// Temp, load this depending on the data available on disk
		ProgressPaneController pp = ProgressPaneController.getProgressPane(null, this.rootStackPane);
		
		Runnable logic = new Runnable() {
			
			@Override
			public void run() {
				
				// Fill any UI elements of the clusterview which require dynamic content
				initializeClusterViewElements();
				
				loadClusterInformation(pp);
				
				isInitialized = true;
				
			}
		
		};
		
		pp.setLogic(logic);
		pp.setShowProgressBar(true);
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	
	/**
	 * Initializes any nodes in the control bars (top and right) which have dynamic content
	 */
	private void initializeControlBars() {
		
		// Determine all possible randomized region sizes from the selection cycles
		Set<Integer> sizes = new HashSet<Integer>();
		Map<Integer,Integer> maxima = new HashMap<Integer,Integer>();
		for (  Entry<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>> cycle : experiment.getMetadata().nucleotideDistributionAccepted.entrySet() ) {
			
			for ( Entry<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> size : cycle.getValue().entrySet()) {
				
				sizes.add(size.getKey());
				
				maxima.putIfAbsent(size.getKey(), 0);
				maxima.put(size.getKey(), maxima.get(size.getKey()) + size.getValue().get(0).get((byte) 'A') + size.getValue().get(0).get((byte) 'C') + size.getValue().get(0).get((byte) 'G') + size.getValue().get(0).get((byte) 'T') );
				
			}
			
		}
		
		// Set possible values
		sizes.stream().sorted().forEach(e -> this.randomizedRegionSizeComboBox.getItems().add(e) ); 
			
		// Set default based on what is defined in the config file. If AptaClsuer has never been run, take the 
		// randomized region with is most represented in the data.
		if (Configuration.getParameters().containsKey("Aptacluster.RandomizedRegionSize")) {
			
			this.randomizedRegionSizeComboBox.setValue( Configuration.getParameters().getInt("Aptacluster.RandomizedRegionSize") );
			
		}
		else {
			
			this.randomizedRegionSizeComboBox.setValue( maxima.entrySet().stream().max( Comparator.comparing( e -> e.getValue() )).get().getKey() );
			
		}
		
		this.randomizedRegionSizeComboBox.getSelectionModel().selectedItemProperty().addListener( (op,o,n) -> setLSHDimensionOptions() );
		setLSHDimensionOptions();		
		
		
		
		// Set The LSH Hashing Iterations (max 20)
		for (int x=1; x<=20; x++) {
			
			this.localitySensitiveHashingIterationsComboBox.getItems().add(x);
			
		}
		this.localitySensitiveHashingIterationsComboBox.setValue( Configuration.getParameters().getInt("Aptacluster.LSHIterations"));
	
		
		// Edit distance
		this.editDistanceTextField.setText(Configuration.getParameters().getString("Aptacluster.EditDistance"));
		
		// Kmer size
		this.kmerSizeTextField.setText(Configuration.getParameters().getString("Aptacluster.KmerSize"));
		
		// Cutoff
		this.kmerCutoffIterationsTextField.setText(Configuration.getParameters().getString("Aptacluster.KmerCutoffIterations"));
		
		// Selection Cycles for cluster comparison
		this.clusterEnrichmentCompareToCycleComboBox.getItems().addAll(experiment.getAllSelectionCycles());
		
		// Make sure the user selects a comparison cycle before being able to press GO
		this.clusterComparisonGoButton.disableProperty().bind(this.clusterEnrichmentCompareToCycleComboBox.valueProperty().isNull());
		
		// Bind disabled property of the counter selection box to whether it is available
		if (experiment.getCounterSelectionCycles().stream().filter( (item) -> item != null ).count() == 0 & experiment.getControlSelectionCycles().stream().filter( (item) -> item != null ).count() == 0 ) {
			
			plotIncludeNegativeSelectionsCheckBox.setDisable(true);
			
		}	
		
	}
	
	/**
	 * Since the LSH Dimension must be smaller or equal to the randomized region
	 * size, we update the content of the combo box when the user selects a different
	 * LSH size.
	 */
	private void setLSHDimensionOptions() {

		// Reset
		this.localitySensitiveHashingDimensionComboBox.getItems().clear();
		
		// Iterate over randomized region size combobox and only add values to the LSH combobox which 
		// are <= to the selected item of the former.
		for ( Integer item=1; item<= this.randomizedRegionSizeComboBox.getSelectionModel().getSelectedItem(); item++ ) {
			
				this.localitySensitiveHashingDimensionComboBox.getItems().add(0,item);
				
		}
		
		// Set the default to 75% of the Randomized Region size
		Double default_value = this.randomizedRegionSizeComboBox.getSelectionModel().getSelectedItem() / 100. * 75.;
		
		this.localitySensitiveHashingDimensionComboBox.setValue(default_value.intValue());
		
		
	}
	
	/**
	 * Implements the logic when the user wants to sort the clusters by
	 * a different criteria or cycle 
	 */
	@FXML
	private void clusterTableGOButtonAction() {
		
		this.cluster_sorting_criteria_diversity = !this.clusterOrderSizeRadioButton.isSelected();
		
		ProgressPaneController pp = ProgressPaneController.getProgressPane(null, this.rootStackPane);
		
		Runnable logic = new Runnable() {
			
			@Override
			public void run() {
				loadClusterInformation(pp);
			}
		
		};
		
		pp.setLogic(logic);
		pp.setShowProgressBar(true);
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	/**
	 * Loads the cluster information into the scene
	 */
	private void loadClusterInformation(ProgressPaneController pp) {
		
				AptaLogger.log(Level.INFO, this.getClass(), "Loading Cluster Information (this process might take a while)");
		
				// Make sure we have cluster information at this point
				boolean clusters_present = true;
				if (experiment.getClusterContainer() == null)
				{
					try {
						experiment.instantiateClusterContainer(false, false);
					}
					catch (InformationNotFoundException e) {
						clusters_present = false;
					}
					
				}	
				
				if (clusters_present) {
				
					clusters = experiment.getClusterContainer();
					
					// Load the LazyCache if possible or create a new one
					Path clusterDataPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"),"clusterdata");
					try {
						
						// Cluster Table
						this.lazyCacheClusterIDs = new MapDBGenericStorage<String, int[]>(clusterDataPath, "lazycacheclusterids.mapdb", Serializer.STRING, Serializer.INT_ARRAY);
						this.lazyCacheClusterCardinalities = new MapDBGenericStorage<String, int[]>(clusterDataPath, "lazycacheclustercardinalities.mapdb", Serializer.STRING, Serializer.INT_ARRAY);
						
						// Aptamer Table
						this.lazyCacheAptamerIDs = new MapDBGenericStorage<Integer, int[]>(clusterDataPath, "lazycacheaptamerids.mapdb", Serializer.INTEGER, Serializer.INT_ARRAY);
						this.lazyCachePoolClusterMembership = new MapDBGenericStorage<Integer, byte[]>(clusterDataPath, "lazycachepoolclustermembership.mapdb", Serializer.INTEGER, Serializer.BYTE_ARRAY);
						this.lazyCacheClusterMembership = new MapDBGenericStorage<Integer, byte[]>(clusterDataPath, "lazycacheclustermembership.mapdb", Serializer.INTEGER, Serializer.BYTE_ARRAY);
						
						// Logo and Mutation Data
						this.lazyCacheLogoData = new MapDBGenericStorage<Integer, double[]>(clusterDataPath, "lazycachelogodata.mapdb", Serializer.INTEGER, Serializer.DOUBLE_ARRAY);
						this.lazyCacheMutationData = new MapDBGenericStorage<Integer, double[]>(clusterDataPath, "lazycachemutationdata.mapdb", Serializer.INTEGER, Serializer.DOUBLE_ARRAY);						
						
						// Cluster Cardiniality Chart
						this.lazyCacheClusterCardinalityData = new MapDBGenericStorage<Integer, long[]>(clusterDataPath, "lazycacheclustercardinalitydata.mapdb", Serializer.INTEGER, Serializer.LONG_ARRAY);
					}
					catch (Exception e) {
						
						AptaLogger.log(Level.WARNING, this.getClass(), "Failed to load the LazyCache for Aptamer Families. Error:");
						AptaLogger.log(Level.WARNING, this.getClass(), e);

					}
					
					// Show the pane
					clusterBorderPane.setVisible(true);
					noClusterInformationFoundStackPane.setVisible(false);
	
					AptaLogger.log(Level.INFO, this.getClass(), "Retrieving Cluster Data");
					
					// Set and sort the cluster data
					initializeClusterData(pp);
					
					// Initiate the cluster view 
					initializeClusterTable();
					
					initializeClusterTablePagination();
					
					// Make AptaMUT available once the user has selected a cluster
					// Disable AptaMut if user has not selected any cluster
					this.aptaMutGridPane.disableProperty().bind( 
							Bindings.size(this.clusterTableView.getSelectionModel().getSelectedIndices()).isNotEqualTo(1)
							.or(Bindings.equal(this.referenceSelectionCycleComboBox.getSelectionModel().selectedIndexProperty(), 0) )
					);
				}
		
	}
	
	/**
	 * Contains the logic for filling any interface elemetns which we
	 * have dynamic content and which must be loaded after cluster information
	 * is available.
	 */
	private void initializeClusterViewElements() {
		
		// We need to make sure that this runs in the JavaFX application thread but also
		// that the combobox is fully loaded prior to running other functions accessing this data.
		FXConcurrent.runAndWait( () -> {
		
			// In case there was any previous elements in the combo box, remove these
			this.referenceSelectionCycleComboBox.getItems().clear();
			
			// Fill the combo box with all selection cycles
			this.referenceSelectionCycleComboBox.getItems().addAll(all_cycles);
		
			// Set listener to fill the reference cycle for AptaMUT according to the selection of this combobox
			this.referenceSelectionCycleComboBox.valueProperty().addListener((options, oldValue, newValue) -> {
				
				this.aptaMutReferenceCycleComboBox.getItems().clear();
				
				// Only add up to the cycle selected for the cluster table 
				all_cycles.forEach((cycle) -> {
					
					if (cycle.getRound() < newValue.getRound()) {
						
						this.aptaMutReferenceCycleComboBox.getItems().add(cycle);
						
					}
					
				});
				
				this.aptaMutReferenceCycleComboBox.getSelectionModel().selectLast();
				
			});
			
			// And set the last cycle as default
			this.referenceSelectionCycleComboBox.getSelectionModel().selectLast();
			
		});
		
	}
	
	
	/**
	 * Prepares auxiliary data structures to handle the displaying of 
	 * the cluster information
	 */
	private void initializeClusterData(ProgressPaneController pp) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Initializing cluster data structures");
		
		// Set the total number of clusters
		numberOfClusters = clusters.getNumberOfClusters();
		
		// Create initial cluster ids
		cluster_ids = new int[numberOfClusters];

		// We need a secondary array to store the counts
		cardinalities = new int[numberOfClusters];
		
		// For the lazy cache, we need to use a combination key of
		// cycle name and cardinality criteria
		String combined_key = this.referenceSelectionCycleComboBox.getValue().getName() + "$" + (((RadioButton) clusterOrderToggleGroup.getSelectedToggle()).getText());
		
		//check if we have this required information precomputed in the lazy cache
		if (this.lazyCacheClusterIDs.containsKey(combined_key)) {
			
			AptaLogger.log(Level.INFO, this.getClass(), "Loading information from the Lazy Cache");
			
			//we can directly set the cluster_ids and cardinality arrays
			cluster_ids = this.lazyCacheClusterIDs.get(combined_key);
			cardinalities = this.lazyCacheClusterCardinalities.get(combined_key);
			
		}
		else {
		
			// Initialize
			for (int x=0; x<numberOfClusters; x++) {
				
				cardinalities[x] = 0;
				cluster_ids[x] = x;
				
			}
	
			SelectionCycle reference_cycle = referenceSelectionCycleComboBox.getSelectionModel().getSelectedItem();
			
			// We use a runnable to obtain a nice GUI update 
			AtomicInteger progress = new AtomicInteger(0);
			Double total = (double) Math.max( clusters.getSize(), reference_cycle.getUniqueSize() );
			
			Runnable get_cluster_information  = new Runnable() {
	
				@Override
				public void run() {
					
					// Fill cardinality array
					if (clusterOrderSizeRadioButton.isSelected()) { // For size
						
						Iterator<Entry<Integer, Integer>> cluster_it = clusters.iterator().iterator();
						Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
						Entry<Integer, Integer> cluster_entry = cluster_it.next();
						Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
						
						
						while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases
	
							progress.incrementAndGet();
	
							if (cluster_entry.getKey() < cardinality_entry.getKey()) {
								
								cluster_entry = cluster_it.next();
								
							}
							else if (cluster_entry.getKey() > cardinality_entry.getKey()){
								
								cardinality_entry = cardinality_it.next();
								
							}
							
							// Process
							else {
	
								cardinalities[cluster_entry.getValue()] += cardinality_entry.getValue();  
								cluster_entry = cluster_it.next();
								
							}
	
	
						}	
								
						
					} else { //For Diversity
						
						Iterator<Entry<Integer, Integer>> cluster_it = clusters.iterator().iterator();
						Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
						Entry<Integer, Integer> cluster_entry = cluster_it.next();
						Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
						
						while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases
	
							progress.incrementAndGet();
							
							if (cluster_entry.getKey() < cardinality_entry.getKey()) {
								
								cluster_entry = cluster_it.next();
								
							}
							else if (cluster_entry.getKey() > cardinality_entry.getKey()){
								
								cardinality_entry = cardinality_it.next();
								
							}
							
							else {
	
								cardinalities[cluster_entry.getValue()] += 1;  
								cluster_entry = cluster_it.next();
								
							}
							
						}		
						
					}
					
				}
				
			};
			
			Thread get_cluster_information_thread = new Thread(get_cluster_information);
			get_cluster_information_thread.start();
			
			while (get_cluster_information_thread.isAlive() && !get_cluster_information_thread.isInterrupted()) {
				try {
					pp.setProgress(progress.doubleValue()/total);
					pp.setProgressLabel(String.format("%.2f%% Completed.", (progress.doubleValue()/total)*100.0));
					
					// Once every half a second should suffice
					Thread.sleep(500);
					
				} catch (InterruptedException ie) {
					break;
				}
			}
	
			// Last Update
			pp.setProgress(1.0);
			pp.setProgressLabel(String.format("%.2f%% Completed.", (progress.doubleValue()/total)*100.0));
			
			
			AptaLogger.log(Level.INFO, this.getClass(), "Sorting Clusters");
			
			// Sort the cluster id array according to the cardinalities
			Quicksort.sort(cluster_ids, cardinalities, Quicksort.DescendingQSComparator());
		
			// Add this result to the lazy cache
			AptaLogger.log(Level.INFO, this.getClass(), "Storing information in the Lazy Cache");
			this.lazyCacheClusterIDs.put(combined_key, cluster_ids);
			this.lazyCacheClusterCardinalities.put(combined_key, cardinalities);
			
		}
		
		// Set the number of items to display
		total_cluster_table_items = cluster_ids.length;
		
	}
	
	/**
	 * Contains the logic for creating and loading the left cluster table
	 */
	private void initializeClusterTable() {
		
		clusterTableView = new TableView<>();
		
		// Configure properties
		clusterTableView.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
		clusterTableView.getSelectionModel().setSelectionMode( SelectionMode.SINGLE );
		
		// Cluster ID
		TableColumn<ClusterTableRowData, Integer> id_column = new TableColumn<>("Cluster Id");
		id_column.setCellValueFactory(param -> param.getValue().getId());
		id_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		
		// Cluster Size or Diversity
		TableColumn<ClusterTableRowData, Integer> cardinality_column = new TableColumn<>("Cluster " + (this.clusterOrderSizeRadioButton.isSelected() ? "Size" : "Diversity"));
		cardinality_column.setCellValueFactory(param -> param.getValue().getCardinality());
		cardinality_column.setStyle( "-fx-alignment: CENTER-LEFT;" );
		
		clusterTableView.getColumns().setAll(id_column, cardinality_column);
		
		// Add listeners for when user selects rows
		clusterTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
		    if (newSelection != null) {
		    	
		    	// We want to predict and display the secondary structure only when one row is selected
		    	loadSelectedClusterDetails();
		    	
		    }
		});
		
	}
	
	
	private void initializeClusterTablePagination() {
		
		// Pagination properties
		clusterPagination = new Pagination((this.total_cluster_table_items / rows_per_cluster_table_page + 1), 0);
		clusterPagination.setPageFactory(this::createClusterPage);
		
		
		Platform.runLater(()->{
			
			// Remove any previous elements
			this.clusterTableStackPane.getChildren().clear();
			
	        this.clusterTableStackPane.getChildren().add(clusterPagination);			
			
		});
		
	}
	
	
	/**
	 * Creates the page with the content
	 * @param pageIndex
	 * @return
	 */
	private Node createClusterPage(int pageIndex) {
		
		// Calculate the iterator position
        int fromIndex = pageIndex * rows_per_cluster_table_page;
        int toIndex = Math.min(fromIndex + rows_per_cluster_table_page, this.total_cluster_table_items);
        
        // Populate data
        ObservableList<ClusterTableRowData> data = FXCollections.observableArrayList( new ArrayList<ClusterTableRowData>() );
        for (int x=fromIndex; x<toIndex; x++) {
        	
        	// we need to filter out clusters which have no aptamers in the selected cycle
        	if (cardinalities[x] != 0) {
        		data.add(new ClusterTableRowData( cluster_ids[x], cardinalities[x]));
        	}
        	
        }
        
        // Set Data
        clusterTableView.setItems(data);

        return new BorderPane(clusterTableView);
    }
	
	
	/**
	 * Takes care of loading all the details associated with 
	 * a particular cluster when the user selects that cluster
	 * from the cluster table
	 */
	private void loadSelectedClusterDetails() {

		ProgressPaneController pp = ProgressPaneController.getProgressPane(null, this.rootStackPane);
		
		Runnable logic = new Runnable() {
			
			@Override
			public void run() {
				
				// Clear any previous content from the Cardinality Chart and Cluster Enrichment
				Platform.runLater( () -> {
					cardinalityLineChart.getData().clear();
					clusterComparisonScatterChart.getData().clear();	
					mutationStackedBarchart.getData().clear();
					sequenceLogoStackPane.getChildren().clear();
					clusterCardinalityBarChart.getData().clear();
				});
				
				// Get the selected cluster index
				ClusterTableRowData row = clusterTableView.getSelectionModel().getSelectedItem();
				
				// If cluster diversity is selected as the sorting criteria
				computeAptamerIdOrder(row.getId().getValue(), cluster_sorting_criteria_diversity ? row.getCardinality().getValue() : -1, pp);
				
				// First load and create the sequence table
				createSequenceTable();
				initializeSequenceTablePagination();

				// Then the remaining elements, in a separate thread. Make sure we have 
				// properly stopped any previously running thread.
//				if (logo_thread != null && logo_thread.isAlive()) {
//					
//					logo_thread.interrupt();
//					
//				}
				if (logo_thread != null && logo_thread.isAlive()) {
					
					logoThreadInterrupted = true;
					
				}				
				
				
				
				Thread thread = new Thread(new Runnable() {

					@Override
					public void run() {
						
						// Dummy to cover second node
						ProgressPaneController ppl2 = ProgressPaneController.getProgressPane( null , clusterSequenceLogoStackPane);
						ppl2.setShowLogs(false);
						ppl2.run();
						
						ProgressPaneController ppl = ProgressPaneController.getProgressPane(null, mutationRatesStackPane, ppl2);

						Runnable logic2 = new Runnable() {
							
							@Override
							public void run() {
								
								createLogoAndMutationPanels(ppl);
							}
						
						};
						
						ppl.setLogic(logic2);
						ppl.setShowLogs(false);
						ppl.setShowProgressBar(true);
						ppl.setProgress(0);
						ppl.setProgressLabel("Completed 0%");
						ppl.run();
						
						// we need to be able to interrupt
						logo_thread = ppl.getTaskThread();

					}
					
				});
				
				thread.start();
				
				if ( cardinality_thread != null && cardinality_thread.isAlive()) {
					
					cardinalityThreadInterrupted = true;
					
				}	
				
				// Compute the cluster cardinality bar charts
				Thread thread2 = new Thread(new Runnable() {

					@Override
					public void run() {
						
						
						ProgressPaneController ppl = ProgressPaneController.getProgressPane(null, clusterCardinalityBarChartStackPane);
						
						Runnable logic = new Runnable() {
							
							@Override
							public void run() {
								createClusterCardinalityPanel(ppl);
							}
						
						};
						
						ppl.setLogic(logic);
						ppl.setShowLogs(false);
						ppl.setShowProgressBar(true);
						ppl.run();
						
						// we need to be able to interrupt
						cardinality_thread = ppl.getTaskThread();

					}
				});
				thread2.start();
				
			}
			
		};
		
		pp.setLogic(logic);
		pp.setShowProgressBar(true);
		pp.setShowLogs(true);
		pp.run();
		
	}
	
	
	/**
	 * Build the table columns according to the data
	 * Style the columns according to their content
	* @return
	*/
	private void createSequenceTable() {

		sequenceTableView = new TableView<>();
		
		// Configure properties
		sequenceTableView.setColumnResizePolicy ( TableView.UNCONSTRAINED_RESIZE_POLICY );
		sequenceTableView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		sequenceTableView.setStyle("-fx-font: 14px monospace;");
		
		// Aptamer ID
		TableColumn<SequenceTableRowData, Integer> id_column = new TableColumn<>("Id");
		id_column.setCellValueFactory(param -> param.getValue().getId());
		id_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		
		// Aptamer Sequence
		TableColumn<SequenceTableRowData, String> sequence_column = new TableColumn<>("Randomized Region");
		sequence_column.setCellValueFactory(param -> param.getValue().getSequence() );
		sequence_column.setStyle( "-fx-alignment: CENTER-LEFT;");
		
		sequenceTableView.getColumns().setAll(id_column, sequence_column);
		
		// We need to style the sequence according to their dna color, so we add a custom
		// TableCell implementation to sequence_column
		sequence_column.setCellFactory(column -> {
			return new TableCell<SequenceTableRowData, String>() {
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
				sequenceTableView.getColumns().add(cycle_column);
				
			}
			
			// Take care of the negative selections
			if (negative.get(x) != null) {
				
				for ( SelectionCycle cycle : negative.get(x)) {
					
					// Create the column
					TableColumn cycle_column = generateColumn(cycle);
					
					// Add to round column
					sequenceTableView.getColumns().add(cycle_column);
					
				}
			
			}
			
			// Take care of the control selections
			if (control.get(x) != null) {
				
				for ( SelectionCycle cycle : control.get(x)) {
					
					// Create the column
					TableColumn cycle_column = generateColumn(cycle);
					
					// Add to round column
					sequenceTableView.getColumns().add(cycle_column);
					
				}
			
			}
			
		}
		
		// Add listeners for when user selects rows
		sequenceTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
		    if (newSelection != null) {
		    	
		    	// We want to predict and display the secondary structure only when one row is selected
		    	ObservableList<Integer> selected_indices = sequenceTableView.getSelectionModel().getSelectedIndices();
		    	
		    	setChoiceDependentContent(selected_indices);
		    	
		    }
		});
		
		// enable copy/paste
		new SequenceTableUtils(sequenceTableView, 1, showPrimersRadioButton);
		
		
	}
	
	
	/**
	 * Creates the page with the content
	 * @param pageIndex
	 * @return
	 */
	private Node createSequencePage(int pageIndex) {

		// Calculate the iterator position
        int fromIndex = pageIndex * rows_per_sequence_table_page;
        int toIndex = Math.min(fromIndex + rows_per_sequence_table_page, aptamer_ids.length);
        
        // Populate data
        ObservableList<SequenceTableRowData> data = FXCollections.observableArrayList( new ArrayList<SequenceTableRowData>() );
        for (int x=fromIndex; x<toIndex; x++) {
        	
        	data.add(new SequenceTableRowData( aptamer_ids[x], new String(pool.getAptamer(aptamer_ids[x])), this));
        	
        }
        
        // Set Data
        sequenceTableView.setItems(data);

        return new BorderPane(sequenceTableView);
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
			
			boolean with_primers = showPrimersRadioButton.isSelected();
			
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
	 * Create a column instance for a particular selection cycle
	 * @param cycle
	 * @return
	 */
	private TableColumn generateColumn(SelectionCycle cycle) {
		
		Insets header_padding = new Insets(0,10,0,10);
		
		// Create the main column
		TableColumn<SequenceTableRowData, Integer> cycle_column = new TableColumn<>();
		Label cycle_column_title = new Label(String.format( "Round no.%s, %s", cycle.getRound(), cycle.getName() ));
		cycle_column_title.setFont(table_font);
		cycle_column_title.setWrapText(false);
		cycle_column_title.setPadding(header_padding);
		cycle_column.setGraphic(cycle_column_title);
		
		// COUNT COLUMN
		TableColumn<SequenceTableRowData, Number> count_column = new TableColumn<>();
		
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
		TableColumn<SequenceTableRowData, Number> frequency_column = new TableColumn<>();
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
			
			TableColumn<SequenceTableRowData, Number> enrichment_column = new TableColumn<>();
			
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
	
	
	private void initializeSequenceTablePagination() {
		
		// Pagination properties
		this.sequencePagination = new Pagination((this.aptamer_ids.length / rows_per_sequence_table_page + 1), 0);
		sequencePagination.setPageFactory(this::createSequencePage);
		
		Platform.runLater(()->{
			
			// Remove any previous elements
			this.sequenceTableStackPane.getChildren().clear();
			
	        this.sequenceTableStackPane.getChildren().add(sequencePagination);			
			
		});
		
	}
	
	/**
	 * Creates the plots for the cluster cardinality panel
	 */
	private void createClusterCardinalityPanel(ProgressPaneController ppl) {
		
		ppl.setProgress(0);
		ppl.setProgressLabel("Initializing...");
		
		List<SelectionCycle> cycles = experiment.getAllSelectionCycles();
		
		Series<String, Number> cluster_sizes = new XYChart.Series();
		cluster_sizes.setName("Cluster Sizes (CPM)");
		
		Series<String, Number> cluster_diversities = new XYChart.Series();
		cluster_diversities.setName("Cluster Diversities (CPM)");
		
		// Cluster ID
		int cluster_id = this.clusterTableView.getSelectionModel().getSelectedItem().getId().getValue();
		
		long[] cluster_data = null;
		
		if ( lazyCacheClusterCardinalityData.containsKey( cluster_id) ) { //lazy loading
		
			AptaLogger.log(Level.INFO, this.getClass(), "Loading Cycle Cardinality Information from Lazy Cache");
			
			ppl.setProgressLabel("Loading data from Cache.");
			cluster_data = lazyCacheClusterCardinalityData.get( cluster_id );
		
		}
		else { // we need to compute
			
			AptaLogger.log(Level.INFO, this.getClass(), "Calculatig Cycle Cardinality Information");
			
			// Determine the number of non-null cycles
			int num_cycles = 0;
			for (SelectionCycle cycle : cycles) {
				
				// Skip non-existing cycles
				if (cycle == null) continue;
				
				num_cycles++;
				
			}
			
			ppl.setProgressLabel("Computing cluster size and diversities. Completed 0%");
			
			// Compute the values
			int idx = 0;
			cluster_data = new long[2*num_cycles];
			
			for (SelectionCycle cycle : cycles) {
				
				// Skip non-existing cycles
				if (cycle == null) continue;
				
				Iterable<Entry<Integer, Integer>> cycle_it = cycle.iterator();
				Integer raw_size = 0;
				Integer raw_diversity = 0;
				
				for (Entry<Integer,Integer> item : cycle_it) {
					
					// stop computation if required
					if(cardinalityThreadInterrupted) {
						
						cardinalityThreadInterrupted = false;
						return;
						
					}
					
					if (this.pool_cluster_membership.get(item.getKey())) {
						
						raw_size += item.getValue();
						raw_diversity++;
						
					}
					
				}
				
				cluster_data[idx++] = raw_size;
				cluster_data[idx++] = raw_diversity;
				
				ppl.setProgress(idx / ((double) cluster_data.length+1));
				ppl.setProgressLabel("Computing cluster size and diversities. Completed " + ((int) (idx / ((double) cluster_data.length+1)*100) )  + "%");
				
			}
			
			// Store in lazy cache
			ppl.setProgressLabel("Storing data in LazyCache");
			lazyCacheClusterCardinalityData.put(cluster_id, cluster_data);
			
		}
		
		// now visualize
		ppl.setProgress(0.99);
		ppl.setProgressLabel("Visualizing.");
		int idx = 0;
		for (SelectionCycle cycle : cycles) {
			
			// stop computation if required
			if(cardinalityThreadInterrupted) {
				
				cardinalityThreadInterrupted = false;
				return;
				
			}
			
			
			// Skip non-existing cycles
			if (cycle == null) continue;
			
			Long raw_size = cluster_data[idx++];
			Long raw_diversity = cluster_data[idx++];
			
			String cycle_label = String.format("Round %s (%s)", cycle.getRound(), cycle.getName()); 

			Number cluster_size_value = (raw_size.intValue() / (double) cycle.getUniqueSize()) * 1000000;
			cluster_sizes.getData().add(new XYChart.Data<String,Number>( cycle_label , cluster_size_value));
			
			Number cluster_diversity_value = (raw_diversity.intValue() / (double) cycle.getUniqueSize()) * 1000000;
			cluster_diversities.getData().add(new XYChart.Data<String,Number>( cycle_label , cluster_diversity_value));
			
		}
		
		Platform.runLater( ()-> {
			
			if (this.showClusterSizesRadioButton.isSelected()) {
				
				clusterCardinalityBarChart.getData().clear();
				clusterCardinalityBarChart.getData().addAll(cluster_sizes);
				clusterCardinalityBarChart.getYAxis().setLabel("Cluster Sizes (CPM)");
				
			}
			else {
			
				clusterCardinalityBarChart.getData().clear();
				clusterCardinalityBarChart.getData().addAll(cluster_diversities);
				clusterCardinalityBarChart.getYAxis().setLabel("Cluster Diversities (CPM)");
				
			}
			
			
		});
		
		ppl.setProgress(1);
		ppl.setProgressLabel("Completed 100%");
	}
	
	/**
	 * Creates the sequence cluster logo and the mutation barcharts associated with the selected 
	 * cluster
	 * @param pp 
	 */
	private void createLogoAndMutationPanels(ProgressPaneController ppl) {
		
		// Used to quickly map byte to indices
		HashMap<Byte,Integer> index_mapping = new HashMap<Byte,Integer>();
		index_mapping.put((byte) 'A', 0);
		index_mapping.put((byte) 'C', 1);
		index_mapping.put((byte) 'G', 2);
		index_mapping.put((byte) 'T', 3);
		
		// Reference Cycle 
		SelectionCycle reference_cycle = this.referenceSelectionCycleComboBox.getSelectionModel().getSelectedItem();
		
		// Get the seed sequence of the cluster
		byte[] seed = pool.getAptamer(this.aptamer_ids[0]);
		AptamerBounds seed_bounds = pool.getAptamerBounds(this.aptamer_ids[0]);
		
		// Determine the size of the randomized region based on the sequece table content
		int randomized_region_size = seed_bounds.endIndex - seed_bounds.startIndex;
		
		// Initialize logo and mutation data structures
		double[][] logo_data = new double[4][randomized_region_size];
		double[][] mutation_data = new double[4][randomized_region_size];
		
		// Cluster ID
		int cluster_id = this.clusterTableView.getSelectionModel().getSelectedItem().getId().getValue();
		
		if ( this.lazyCacheLogoData.containsKey( cluster_id) ) { //lazy loading
			
			// Load from lazy cache
			double[] logo_data_serialized = lazyCacheLogoData.get(cluster_id);
			ppl.setProgress(0.25);
			ppl.setProgressLabel("Completed 25%");
			
			double[] mutation_data_serialized = lazyCacheMutationData.get(cluster_id);
			ppl.setProgress(0.50);
			ppl.setProgressLabel("Completed 50%");
			
			System.arraycopy(logo_data_serialized, 0*randomized_region_size, logo_data[0], 0, randomized_region_size);
			System.arraycopy(logo_data_serialized, 1*randomized_region_size, logo_data[1], 0, randomized_region_size);
			System.arraycopy(logo_data_serialized, 2*randomized_region_size, logo_data[2], 0, randomized_region_size);
			System.arraycopy(logo_data_serialized, 3*randomized_region_size, logo_data[3], 0, randomized_region_size);
			
			System.arraycopy(mutation_data_serialized, 0*randomized_region_size, mutation_data[0], 0, randomized_region_size);
			System.arraycopy(mutation_data_serialized, 1*randomized_region_size, mutation_data[1], 0, randomized_region_size);
			System.arraycopy(mutation_data_serialized, 2*randomized_region_size, mutation_data[2], 0, randomized_region_size);
			System.arraycopy(mutation_data_serialized, 3*randomized_region_size, mutation_data[3], 0, randomized_region_size);
			
			ppl.setProgress(0.55);
			ppl.setProgressLabel("Completed 55%");
			
		}		
		else { // no data in lazy cache
			
			Iterator<Entry<Integer, byte[]>> pool_it = pool.inverse_view_iterator().iterator();
			Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
			Entry<Integer, byte[]> pool_entry = pool_it.next();
			Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
			
			int counter = 0;
			double max = reference_cycle.getUniqueSize();
			while ( pool_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases
	
				// update UI
				if (counter % 1000 == 0) {
					
					ppl.setProgress(counter / max);
					int p = (int)( (counter / max) * 100);
					ppl.setProgressLabel("Extracting data from cluster.\nCompleted " + p + "%");//+ (int)( (counter / max) * 100) + "%");
				
				}
				
				
				// check if we should cancel this action
				if (logoThreadInterrupted) { //(this.logo_thread.isInterrupted()) {
					
					logoThreadInterrupted = false;
					return;
					
				}
				
				if (pool_entry.getKey() < cardinality_entry.getKey()) {
					
					pool_entry = pool_it.next();
					
				}
				else if (pool_entry.getKey() > cardinality_entry.getKey()) {
					
					cardinality_entry = cardinality_it.next();
					counter++;
					
				}
				
				else { // aptamer is in the referece cycle
	
					if ( cluster_membership.get(pool_entry.getKey()) ) {
					
						int cardinality = cardinality_entry.getValue();
						byte[] sequence = pool_entry.getValue();
						
						for (int i = seed_bounds.startIndex, j=0; i<seed_bounds.endIndex; i++,j++) {
						
							// Frequency plot
							logo_data[index_mapping.get(sequence[i])][j] += (double)cardinality / cluster_membership.cardinality();
							
							// Mutation plot
							if (sequence[i] != seed[i]) {
								
								mutation_data[index_mapping.get(sequence[i])][j] += (double)cardinality / cluster_membership.cardinality();
								
							}
						
						}
					
					}
					
					// Advance the iterators
					pool_entry = pool_it.next();
					
				}
				
			}
		
			// Add to lazy cache
			double[] logo_data_serialized = new double[4*randomized_region_size];
			double[] mutation_data_serialized = new double[4*randomized_region_size];
			
			System.arraycopy(logo_data[0], 0, logo_data_serialized, 0*randomized_region_size, randomized_region_size);
			System.arraycopy(logo_data[1], 0, logo_data_serialized, 1*randomized_region_size, randomized_region_size);
			System.arraycopy(logo_data[2], 0, logo_data_serialized, 2*randomized_region_size, randomized_region_size);
			System.arraycopy(logo_data[3], 0, logo_data_serialized, 3*randomized_region_size, randomized_region_size);
			
			System.arraycopy(mutation_data[0], 0, mutation_data_serialized, 0*randomized_region_size, randomized_region_size);
			System.arraycopy(mutation_data[1], 0, mutation_data_serialized, 1*randomized_region_size, randomized_region_size);
			System.arraycopy(mutation_data[2], 0, mutation_data_serialized, 2*randomized_region_size, randomized_region_size);
			System.arraycopy(mutation_data[3], 0, mutation_data_serialized, 3*randomized_region_size, randomized_region_size);
			
			lazyCacheLogoData.put(cluster_id, logo_data_serialized);
			lazyCacheMutationData.put(cluster_id, mutation_data_serialized);
			
			ppl.setProgress(0.55);
			ppl.setProgressLabel("Finalizing process. Completed 55%");
			
		}
		
		// Create metadata
		String[] labels = new String[seed_bounds.endIndex - seed_bounds.startIndex];
		for (int x=0; x<seed_bounds.endIndex - seed_bounds.startIndex; labels[x++] = ""+x);
		
		// Visualize sequence logo
		Platform.runLater(() -> {
			
			sequenceLogoStackPane.getChildren().clear();
			
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/charts/logo/LogoChartPanel.fxml"));
				AnchorPane logo = loader.load();
				
				LogoChartPanelController lcpc = (LogoChartPanelController) loader.getController(); 
				
				lcpc.setNumberOfSequencesInAlignment(cluster_membership.cardinality());
				lcpc.setData(logo_data);
				lcpc.setLabels(labels);
				lcpc.setScale(Scale.FREQUENCY);					
				
				lcpc.setAlphabet(Alphabet.DNA);
				lcpc.createChart();

				sequenceLogoStackPane.getChildren().add(logo);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
		
		ppl.setProgress(0.75);
		ppl.setProgressLabel("Finalizing process. Completed 75%");
		
		// Prepare data for stacked mutation plot
		XYChart.Series<String, Number> A = new XYChart.Series<String, Number>();
		A.setName("A");
		XYChart.Series<String, Number> C = new XYChart.Series<String, Number>();
		C.setName("C");
		XYChart.Series<String, Number> G = new XYChart.Series<String, Number>();
		G.setName("G");
		XYChart.Series<String, Number> T = new XYChart.Series<String, Number>();
		T.setName("T");
		
		for ( int x=0; x<mutation_data[0].length; x++) {
			
			A.getData().add(new XYChart.Data<String, Number>((char)seed[seed_bounds.startIndex + x] + " (" + x + ")", mutation_data[0][x]));
			C.getData().add(new XYChart.Data<String, Number>((char)seed[seed_bounds.startIndex + x] + " (" + x + ")", mutation_data[1][x]));
			G.getData().add(new XYChart.Data<String, Number>((char)seed[seed_bounds.startIndex + x] + " (" + x + ")", mutation_data[2][x]));
			T.getData().add(new XYChart.Data<String, Number>((char)seed[seed_bounds.startIndex + x] + " (" + x + ")", mutation_data[3][x]));
			
		}
		
		// Visualize sequence logo
		Platform.runLater(() -> {
			
			mutationStackedBarchart.getData().clear();
			mutationStackedBarchart.getData().setAll(A,C,G,T);
			mutationStackedBarchart.getXAxis().setLabel("Nucleotide Position");
			mutationStackedBarchart.getYAxis().setLabel("Mutation Frequency");
			
		});
		
		ppl.setProgress(1.0);
		ppl.setProgressLabel("Finalizing process. Completed 100%");
		
	}
	
	/**
	 * Given the user has selected a cluster id form the cluster table,
	 * this function extracts and sorts the aptamers which are cluster 
	 * members for display in the sequence table
	 * 
	 * @param cluster_id
	 * @param max_item since we need to reserve memory for the ids, if the exact amount of ids is know, 
	 * it can be specified here. If value is -1, it will be calculated.
	 * 
	 * @return a BitSet with 1 at the index at which an aptamer with ID x is part of the cluster 
	 */
	private BitSet computeAptamerIdOrder(int cluster_id, int max_items, ProgressPaneController pp) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Extracting aptamers for cluster " + cluster_id);
		
		SelectionCycle reference_cycle = this.referenceSelectionCycleComboBox.getValue();
		
		// Use a bitset to store if an aptamer belongs to the cluster and the reference cycle for fast lookup
		cluster_membership.clear();
		pool_cluster_membership.clear();

		AtomicInteger progress = new AtomicInteger(0);
		AtomicInteger total = new AtomicInteger(Math.max( clusters.getSize(), reference_cycle.getUniqueSize() ));
		
		Runnable get_cluster_information_lazy = new Runnable() {

			@Override
			public void run() {
				
				AptaLogger.log(Level.INFO, this.getClass(), "Loading cluster data from Lazy Cache");
				total.set(3);				
				
				AptaLogger.log(Level.INFO, this.getClass(), "Loading Aptamer Pool Memberships");
				pool_cluster_membership = BitSet.valueOf(lazyCachePoolClusterMembership.get(cluster_id));
				progress.incrementAndGet();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Loading Aptamer Cluster Memberships");
				cluster_membership = BitSet.valueOf(lazyCacheClusterMembership.get(cluster_id));
				progress.incrementAndGet();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Loading Aptamer IDs");
				aptamer_ids = lazyCacheAptamerIDs.get(cluster_id);
				progress.incrementAndGet();
				
			}
			
		};
		
		Runnable get_cluster_information = new Runnable() {

			@Override
			public void run() {
				
				// Compute the number of aptamers belonging to this cluster
				int cluster_diversity = 0;
					
				AptaLogger.log(Level.INFO, this.getClass(), "Determining cluster diversity");
				
				Iterator<Entry<Integer, Integer>> cluster_it = clusters.iterator().iterator();
				Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
				Entry<Integer, Integer> cluster_entry = cluster_it.next();
				Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
				
				while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases

					progress.incrementAndGet();
					
					if (cluster_entry.getKey() < cardinality_entry.getKey()) {
						
						cluster_entry = cluster_it.next();
						
					}
					else if (cluster_entry.getKey() > cardinality_entry.getKey()) {
						
						cardinality_entry = cardinality_it.next();
						
					}
					
					else {

						if (cluster_entry.getValue() == cluster_id) {
							
							cluster_diversity += 1;  
							cluster_membership.set(cluster_entry.getKey());
							
						}
						
						cluster_entry = cluster_it.next();
						
					}
					
				}
				
				
				// Get all Aptamers which are members of the cluster and are present in the selected cycle
				aptamer_ids = new int[cluster_diversity];
				
				AptaLogger.log(Level.INFO, this.getClass(), "Determining cluster membership of aptamers");
				progress.set(0);
				total.set(clusters.getSize());
				
				int counter = 0;
				for ( Entry<Integer, Integer> member : clusters.iterator()) {
					
					progress.incrementAndGet();
					
					// Make use of this loop to fill out the cluster membership for the entire pool
					if (member.getValue() == cluster_id) {
						
						pool_cluster_membership.set(member.getKey());
						
					}
					
					// Skip other clusters and aptamers which are not part of this cluster in the selected cycle
					if (!cluster_membership.get(member.getKey())) continue;
					
					aptamer_ids[counter++] = member.getKey();  
					
				}
				
				AptaLogger.log(Level.INFO, this.getClass(), "Extracting aptamer sizes" );
				progress.set(0);
				total.set(Math.max( clusters.getSize(), reference_cycle.getUniqueSize() ));
				
				// Get the aptamer cardinalities for sorting
				int[] counts = new int[aptamer_ids.length];
				counter = 0;
				
				cluster_it = clusters.iterator().iterator();
				cardinality_it = reference_cycle.iterator().iterator();
				cluster_entry = cluster_it.next();
				cardinality_entry = cardinality_it.next();
				
				while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases

					progress.incrementAndGet();
					
					if (cluster_entry.getKey() < cardinality_entry.getKey()) {
						
						cluster_entry = cluster_it.next();
						
					}
					else if (cluster_entry.getKey() > cardinality_entry.getKey()){
						
						cardinality_entry = cardinality_it.next();
						
					}
					
					else {

						if (cluster_entry.getValue() == cluster_id) {
							
							counts[counter++] = cardinality_entry.getValue();
							
						}
						
						cluster_entry = cluster_it.next();
						
					}
					
				}
				
				// Now sort the aptamers
				AptaLogger.log(Level.INFO, this.getClass(), "Sorting cluster members");
				Quicksort.sort(aptamer_ids, counts, Quicksort.DescendingQSComparator());
				counts = null;
				
				// Store into Lazy Cache
				AptaLogger.log(Level.INFO, this.getClass(), "Storing Aptamer Pool Memberships in Lazy Cache");
				lazyCachePoolClusterMembership.put(cluster_id, pool_cluster_membership.toByteArray());
				progress.incrementAndGet();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Storing Aptamer Cluster Memberships in Lazy Cache");
				lazyCacheClusterMembership.put(cluster_id, cluster_membership.toByteArray());
				progress.incrementAndGet();
				
				AptaLogger.log(Level.INFO, this.getClass(), "Storing Aptamer IDs in Lazy Cache");
				lazyCacheAptamerIDs.put(cluster_id, aptamer_ids);
				
			}
			
		};
		
		
		Thread get_cluster_information_thread = new Thread( this.lazyCacheClusterMembership.containsKey(cluster_id) ? get_cluster_information_lazy : get_cluster_information);
		get_cluster_information_thread.start();
		
		while (get_cluster_information_thread.isAlive() && !get_cluster_information_thread.isInterrupted()) {
			try {
				pp.setProgress(progress.doubleValue()/total.doubleValue());
				pp.setProgressLabel(String.format("%.2f%% Completed.", (progress.doubleValue()/total.doubleValue())*100.0) );
				
				// Once every half a second should suffice
				Thread.sleep(500);
				
			} catch (InterruptedException ie) {
				break;
			}
		}

		// Last Update
		pp.setProgress(1.0);
		pp.setProgressLabel(String.format("%.2f%% Completed.", 100.0));

		return cluster_membership;
	}
	
	
	/**
	 * Redraws the table content when the configuration 
	 * has changed
	 */
	@FXML
	private void redrawTableContent() {
		
		Platform.runLater(() -> sequenceTableView.refresh());
		
	}
	
	/**
	 * Updates the scatter plot when the user has pressed GO
	 */
	@FXML
	private void updateClusterComparisonScatterPlot() {
		
		SelectionCycle compare_to = this.clusterEnrichmentCompareToCycleComboBox.getValue();
		SelectionCycle reference = this.referenceSelectionCycleComboBox.getValue();
		Boolean log = this.clusterEnrichmentScaleLogarithmicRadioButton.isSelected();
		
		ProgressPaneController pp = ProgressPaneController.getProgressPane(null, this.clusterEnrichmentStackPane);
		
		Runnable logic = new Runnable() {
			
			@Override
			public void run() {
				
				XYChart.Series series = new XYChart.Series();
				
				// Keep track of the max value to scale the plot equally on both axis
				double max = 0;
				
				// Iterate over the aptamer ids of there reference cycle and compute the CMP values
				int counter = 0;
				for (int id : aptamer_ids) {
					
					//update UI
					if (counter % 100 == 0) {
						
						pp.setProgress(counter / (double)aptamer_ids.length );
						pp.setProgressLabel("Completed " + (int)((counter / (double)aptamer_ids.length) * 100) + "%");
						
					}
					
					Number cmpX = (reference.getAptamerCardinality(id) / (double) reference.getSize()) * 1000000;
					Number cmpY = (compare_to.getAptamerCardinality(id) / (double) compare_to.getSize()) * 1000000;
					
					// Scale to log if required
					if (log) {
						
						cmpX = Math.log(cmpX.doubleValue());
						cmpY = Math.log(cmpY.doubleValue());
						
					}
					
					series.getData().add(new XYChart.Data(cmpX , cmpY));
					
					max = Math.max(max, cmpX.doubleValue());
					max = Math.max(max, cmpY.doubleValue());
					
					counter++;
				}
				
				// Compute the upper bound so that the ticks are rounded
				String str_max = ((int)max)+"";
				StringBuilder sb = new StringBuilder();
				sb.append("1");
				for (int x=0; x<str_max.length(); x++) {
					
					sb.append("0");
					
				}
				int upper_bound = Integer.parseInt(sb.toString());
				

				((NumberAxis) clusterComparisonScatterChart.getXAxis()).setAutoRanging(false);
				((NumberAxis) clusterComparisonScatterChart.getYAxis()).setAutoRanging(false);
				
				((NumberAxis) clusterComparisonScatterChart.getXAxis()).setUpperBound((((int)max+5)/10)*10);
				((NumberAxis) clusterComparisonScatterChart.getYAxis()).setUpperBound((((int)max+5)/10)*10);
				
				if (!log) {
					((NumberAxis) clusterComparisonScatterChart.getXAxis()).setTickUnit((int)(upper_bound/10));
					((NumberAxis) clusterComparisonScatterChart.getYAxis()).setTickUnit((int)(upper_bound/10));
					
					((NumberAxis) clusterComparisonScatterChart.getXAxis()).setMinorTickVisible(false);
					((NumberAxis) clusterComparisonScatterChart.getYAxis()).setMinorTickVisible(false);
				} else
				{
					
					((NumberAxis) clusterComparisonScatterChart.getXAxis()).setTickUnit((int)(upper_bound/100));
					((NumberAxis) clusterComparisonScatterChart.getYAxis()).setTickUnit((int)(upper_bound/100));
					
					((NumberAxis) clusterComparisonScatterChart.getXAxis()).setMinorTickVisible(true);
					((NumberAxis) clusterComparisonScatterChart.getYAxis()).setMinorTickVisible(true);
					
				}
				
				Platform.runLater(() -> {
				
					clusterComparisonScatterChart.getData().setAll(series); 
					clusterComparisonScatterChart.getXAxis().setLabel(reference.getName() + " (CMP)");
					clusterComparisonScatterChart.getYAxis().setLabel(compare_to.getName() + " (CMP)");
								
				});
			
				pp.setProgress(1);
				pp.setProgressLabel("Completed 100%");
			}
			
		};
		
		pp.setLogic(logic);
		pp.setShowProgressBar(true);
		pp.setProgressLabel("Completed 0%");
		pp.setProgress(0);
		pp.setShowLogs(false);
		pp.run();
		
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
		
		showCardinalityPlots(sequenceTableView.getSelectionModel().getSelectedIndices());
		
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
					SequenceTableRowData row = sequenceTableView.getItems().get(index);
					
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
					
					cardinalityLineChart.getData().clear();
		        	cardinalityLineChart.getData().setAll(series);
		        	
		        });
				
			}
			
		});
		
		t.setName("chartThread");
		t.setDaemon(true);
		t.start();
		
	}
	
	
	/**
	 * Performs the logic to run AptaCluster using the user
	 * defined parameters
	 */
	@FXML
	private void runAptaCluster() {
		
		Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
		
		// If we already have clsuter data, ask the user if it is to be overwritten
		if (experiment.getClusterContainer() != null) {
			
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Overwrite Cluster Data?");
			alert.setHeaderText("AptaCluster has detected previously generated cluster results\n on disk. This data will be overwritten by this action.");
			alert.setContentText("Click OK to proceed or Cancel to keep the current data.");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.CANCEL){
				   return;
			}
			
			// We need to close all possible channels to the cluster database
			experiment.getClusterContainer().close();
			
			// We also need to close any lazy cache hanldes
			this.closeLazyCaches();
		}
		
		
		// We want the gui not to block on initialization, hence we wrap this into a progress pane
		ProgressPaneController pp = ProgressPaneController.getProgressPane(null, this.rootStackPane);
		
		Runnable logic = new Runnable() {
			
			@Override
			public void run() {
				
				// Delete any previously existing cluster data
				try {
					FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "clusterdata").toFile());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				AptaLogger.log(Level.INFO, this.getClass(), "Initializing AptaCluster");
				
				// Create a new instance of the ClusterContainer
				experiment.instantiateClusterContainer(true, true);

				AptaLogger.log(Level.INFO, this.getClass(), "Initialization Complete");
				
				// Either use defaults, or set new user value in config
				int randomizedRegionSize = Configuration.getParameters().containsKey("Aptacluster.RandomizedRegionSize") ? Configuration.getParameters().getInt("Aptacluster.RandomizedRegionSize") : randomizedRegionSizeComboBox.getValue();
				int lshDimension = Configuration.getParameters().containsKey("Aptacluster.LSHDimension") ? Configuration.getParameters().getInt("Aptacluster.LSHDimension") : localitySensitiveHashingDimensionComboBox.getValue();
				int lshIterations = Configuration.getParameters().getInt("Aptacluster.LSHIterations");
				int editDistance = Configuration.getParameters().getInt("Aptacluster.EditDistance");
				int kmerSize = Configuration.getParameters().getInt("Aptacluster.KmerSize");
				int kmerCutoffIterations = Configuration.getParameters().getInt("Aptacluster.KmerCutoffIterations");
				
				if ( Configuration.getParameters().containsKey("Aptacluster.RandomizedRegionSize") && randomizedRegionSizeComboBox.getValue() != Configuration.getParameters().getInt("Aptacluster.RandomizedRegionSize") ) {
					
					randomizedRegionSize = randomizedRegionSizeComboBox.getValue();
					Configuration.getParameters().setProperty("Aptacluster.RandomizedRegionSize", randomizedRegionSize);
					
				}
				
				if ( Configuration.getParameters().containsKey("Aptacluster.LSHDimension") && localitySensitiveHashingDimensionComboBox.getValue() != Configuration.getParameters().getInt("Aptacluster.LSHDimension") ) {
					
					lshDimension = localitySensitiveHashingDimensionComboBox.getValue();
					Configuration.getParameters().setProperty("Aptacluster.LSHDimension", lshDimension);
					
				}

				if ( localitySensitiveHashingIterationsComboBox.getValue() != Configuration.getParameters().getInt("Aptacluster.LSHIterations") ) {
					
					lshIterations = localitySensitiveHashingIterationsComboBox.getValue();
					Configuration.getParameters().setProperty("Aptacluster.LSHIterations", lshIterations);
					
				}
				
				if ( Integer.parseInt(editDistanceTextField.getText()) != Configuration.getParameters().getInt("Aptacluster.EditDistance") ) {
					
					editDistance = Integer.parseInt(editDistanceTextField.getText());
					Configuration.getParameters().setProperty("Aptacluster.EditDistance", editDistance);
					
				}
				
				if ( Integer.parseInt(kmerSizeTextField.getText()) != Configuration.getParameters().getInt("Aptacluster.KmerSize") ) {
					
					kmerSize = Integer.parseInt(kmerSizeTextField.getText());
					Configuration.getParameters().setProperty("Aptacluster.KmerSize", kmerSize);
					
				}

				if ( Integer.parseInt(kmerCutoffIterationsTextField.getText()) != Configuration.getParameters().getInt("Aptacluster.KmerCutoffIterations") ) {
					
					kmerCutoffIterations = Integer.parseInt(kmerCutoffIterationsTextField.getText());
					Configuration.getParameters().setProperty("Aptacluster.KmerCutoffIterations", kmerCutoffIterations);
					
				}				
				
				int randomizedRegionSizeFinal = randomizedRegionSize;
				int lshDimensionFinal = lshDimension;
				int lshIterationsFinal = lshIterations;
				int editDistanceFinal = editDistance;
				int kmerSizeFinal = kmerSize;
				int kmerCutoffIterationsFinal = kmerCutoffIterations;
				
				AptaLogger.log(Level.INFO, this.getClass(), "Starting AptaCluster");
				
				// Create AptaCluster instance
				AptaCluster ac = new HashAptaCluster(
						randomizedRegionSizeFinal,
						lshDimensionFinal,
						lshIterationsFinal,
						editDistanceFinal,
						kmerSizeFinal,
						kmerCutoffIterationsFinal,
						experiment
						);
				
				// Run
				ac.performLSH();
				
				AptaLogger.log(Level.INFO, this.getClass(), "AptaCluster Comleted");
				
				loadClusterInformation(pp);
			}
		
		};
		
		pp.setLogic(logic);
		pp.setShowProgressBar(true);
		pp.setShowLogs(true);
		pp.run();
		
	}

	@FXML
	public void runAptaMutButtonAction() {
		
		// Run the wizard
    	Parent root;
        try {
        	
        	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/aptamut/AptaMutRoot.fxml"));
        	
            root = loader.load();
            AptaMutRootController controller = (AptaMutRootController) loader.getController();
            
            
            Stage stage = new Stage();
            stage.setTitle("AptaMut for Cycles " + this.aptaMutReferenceCycleComboBox.getSelectionModel().getSelectedItem() + " and " + this.referenceSelectionCycleComboBox.getSelectionModel().getSelectedItem());
            stage.setScene(new Scene(root,  Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE));
    		
            stage.setOnCloseRequest((e) -> { 
    			e.consume(); 
    			controller.close(); 
    			});
            
            stage.show();
            
            controller.setInputData(
            		this.aptaMutReferenceCycleComboBox.getSelectionModel().getSelectedItem(), 
            		this.referenceSelectionCycleComboBox.getSelectionModel().getSelectedItem(),
            		this.pool_cluster_membership, 
            		this.aptamer_ids[0]
            		);
            controller.populateTableData();
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
	@FXML
	private void updateClusterCardinalityChart() {
		
		// Compute the cardinality bar charts
		Thread thread2 = new Thread(new Runnable() {

			@Override
			public void run() {
				
				ProgressPaneController ppl = ProgressPaneController.getProgressPane(null, clusterCardinalityBarChartStackPane);
				
				Runnable logic = new Runnable() {
					
					@Override
					public void run() {
						createClusterCardinalityPanel(ppl);
					}
				
				};
				
				ppl.setLogic(logic);				
				ppl.setShowLogs(false);
				ppl.run();
				
			}
		});
		thread2.start();
		
	}

	/**
	 * @return the rawCountsRadionButton
	 */
	public RadioButton getRawCountsRadionButton() {
		return rawCountsRadionButton;
	}


	/**
	 * @return the cmpRadioButton
	 */
	public RadioButton getCmpRadioButton() {
		return cmpRadioButton;
	}
	
	/**
	 * Closes all file handles to the storage 
	 */
	private void closeLazyCaches() {
		
		lazyCacheClusterIDs.close();
		lazyCacheClusterCardinalities.close();
		lazyCacheAptamerIDs.close();
		lazyCachePoolClusterMembership.close();
		lazyCacheClusterMembership.close();
		lazyCacheLogoData.close();
		lazyCacheMutationData.close();
		lazyCacheClusterCardinalityData.close();
		
	}
	
}
