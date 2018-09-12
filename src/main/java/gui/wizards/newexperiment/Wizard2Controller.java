package gui.wizards.newexperiment;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import gui.core.RootLayoutController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import lib.parser.aptaplex.AptaPlexParser;
import lib.parser.aptaplex.AptaPlexProgress;
import utilities.AptaLogger;
import utilities.Configuration;

public class Wizard2Controller{

	@FXML
	private TitledPane importStatisticsGridPane;

	@FXML
	private TextField totalProcessedReadsTextField;
	
	@FXML
	private TextField totalAcceptedReadsTextField;
	
	@FXML
	private TextField contigAssemblyFailureTextField;
	
	@FXML
	private TextField invalidAlphabetTextField;
	
	@FXML
	private TextField primer5ErrorTextField;
	
	@FXML
	private TextField primer3ErrorTextField;
	
	@FXML
	private TextField invalidCycleTextField;
	
	@FXML
	private TextField totalPrimerOverlapsTextField;
	
	@FXML
	private Label parsingCompletedLabel;
	
    
    @FXML
    private Button importDataButton;
    
    @FXML
    private Label loggerLabel1;
    
    @FXML
    private Label loggerLabel2;
    
    @FXML
    private Label loggerLabel3;
    
	@FXML
	private HBox actionBar;
	
	@FXML
	private ActionBarController actionBarController;
    
    /**
     * Hook to the experiment instance. 
     * We will pass it to the main application once parsing is complerted 
     * and the window closed.
     */
    private Experiment experiment = null;
    
    private AptaPlexParser parser = null;
    
    private Thread parserThread = null;
    
    private Thread updateThread = null;
    
    private AptaPlexProgress progress = null;
    
    /**
     * Buttons from the included XFML file
     */
    private Button backButton;
    private Button nextButton;
    private Button finishButton;
    
    /**
     * Reference to the root layout controller. will be passed from scene to scene
     */
    private RootLayoutController rootLayoutController;
    
    /**
     * Reference to the stage, will be passed from scene to scene
     */
    private Stage stage;
    
    /**
     * The datamodel storing all the information from the wizard. will be passed from scene to scene
     */
    private DataModel dataModel;
    
    public void init() {
    	
    	setButtonActions();
    	
    }

    
    /**
     * Instaniates a new Experiment and starts AptaPLEX
     * @param event
     */
    @FXML
    private void importDataButtonAction(ActionEvent event) {
    	
    	// First take care of the UI. Once parsing starts, there is no way back, so disable the controls
        nextButton.setDisable(true);
        backButton.setDisable(true);
        finishButton.setDisable(true);
        importDataButton.setDisable(true);
    	
        // Register the logger
        Wizard2ControllerLogHandler handler = new Wizard2ControllerLogHandler(this);
        AptaLogger.getLogger().addHandler(handler);
    	
    	Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                	
                	// Read config file and set defaults
                	String safe_experiment_name = getDataModel().getExperimentName().get().replaceAll("[^a-zA-Z0-9]+", "").trim();
                	Path experiment_path = Paths.get(getDataModel().getProjectPath().get(), safe_experiment_name);
                	Path configuration_file = Paths.get(experiment_path.toAbsolutePath().toString(), "configuration.aptasuite");
            		utilities.Configuration.setConfiguration(configuration_file.toAbsolutePath().toString());
                	
               		AptaLogger.log(Level.CONFIG, this.getClass(), "Creating Database");
               		
               		// Initialize the experiment
                	experiment = new Experiment(configuration_file.toAbsolutePath().toString(), true);

            		AptaLogger.log(Level.CONFIG, this.getClass(), "Initializing Experiment");

            		// Initialize the parser and run it in a thread
            		AptaLogger.log(Level.CONFIG, this.getClass(), "Initializing parser " + Configuration.getParameters().getString("Parser.backend"));
            		parser = new AptaPlexParser();

            		AptaLogger.log(Level.CONFIG, this.getClass(), "Starting AptaPlex:");
            		long tParserStart = System.currentTimeMillis();
            		
            		parser.run();

            		AptaLogger.log(Level.CONFIG, this.getClass(), String.format("Parsing Completed in %s seconds.\n",
            				((System.currentTimeMillis() - tParserStart) / 1000.0)));

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        //Add logic to enable next/prev buttons here
        task.setOnSucceeded(taskFinishEvent -> { 
        	
        	finishButton.setDisable(false);
        	parsingCompletedLabel.setVisible(true);
        	
        	loggerLabel1.setVisible(false);
    		loggerLabel2.setVisible(false);
    		loggerLabel3.setVisible(false);
        	
    		AptaLogger.getLogger().removeHandler(handler);
    		
    		// Final update
    		Platform.runLater(new Runnable() {
				@Override
				public void run() {
        				totalAcceptedReadsTextField.textProperty().set(progress.totalAcceptedReads.toString());
        				totalProcessedReadsTextField.textProperty().set(progress.totalProcessedReads.toString());
        				contigAssemblyFailureTextField.textProperty().set(progress.totalContigAssemblyFails.toString());
        				invalidAlphabetTextField.textProperty().set(progress.totalInvalidContigs.toString());
        				primer5ErrorTextField.textProperty().set(progress.totalUnmatchablePrimer5.toString());
        				primer3ErrorTextField.textProperty().set(progress.totalUnmatchablePrimer3.toString());
        				invalidCycleTextField.textProperty().set(progress.totalInvalidCycle.toString());
        				totalPrimerOverlapsTextField.textProperty().set(progress.totalPrimerOverlaps.toString());
				}
			});
    		
        	// clean up
        	parserThread = null;
        	parser = null;
    	} );
        
        // Start the task in a thread
        parserThread = new Thread(task);
        parserThread.start();

        // Prepare the task to update the UI
		Task<Void> updateUITask = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				
				// First we need to get a hook to the progress but we
				// must make sure the parser thread has instantiated 
				// the parser yet.
				while (progress == null) {
					if (parser != null) {
						progress = (AptaPlexProgress) parser.Progress();
					}
					else {
						Thread.sleep(1000);
					}
				}
				
				// Update logic
				while (parserThread.isAlive() && !parserThread.isInterrupted()) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
                				totalAcceptedReadsTextField.textProperty().set(progress.totalAcceptedReads.toString());
                				totalProcessedReadsTextField.textProperty().set(progress.totalProcessedReads.toString());
                				contigAssemblyFailureTextField.textProperty().set(progress.totalContigAssemblyFails.toString());
                				invalidAlphabetTextField.textProperty().set(progress.totalInvalidContigs.toString());
                				primer5ErrorTextField.textProperty().set(progress.totalUnmatchablePrimer5.toString());
                				primer3ErrorTextField.textProperty().set(progress.totalUnmatchablePrimer3.toString());
                				invalidCycleTextField.textProperty().set(progress.totalInvalidCycle.toString());
                				totalPrimerOverlapsTextField.textProperty().set(progress.totalPrimerOverlaps.toString());
						}
					});
    				// Once every second should suffice
					Thread.sleep(1000);
				}
				
				return null;
			}
		};
		updateThread = new Thread(updateUITask);
		updateThread.setDaemon(true);
		updateThread.start();
        
    }
    
	/**
	 * Adds a new message to the logging labels, shifting old ones down
	 * @param message
	 */
	public void addLogMessage(String message) {
		
		Task<Void> task = new Task<Void>(){
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                	
                	// Move the old messages down
            		String cache = loggerLabel2.getText();
            		loggerLabel2.setText(loggerLabel1.getText());
            		loggerLabel3.setText(cache);
            		loggerLabel1.setText(message);
            		
                });
                return null;
            }
        };
        
        new Thread(task).start();
		
	}
    
    /**
     * Closes the wizard
     */
    public void closeWizard() {
    	
    	// We need to set the experiment in the main controller
    	rootLayoutController.setExperiment(experiment);
    	
    	// Notify the gui to initialize loading the tabs
    	rootLayoutController.showInitialTabs();
    	
    	// Get a handle to the stage
        Stage stage = (Stage) this.finishButton.getScene().getWindow();

        // Close it
        stage.close();
    	
    }
    
    /**
     * Defines the actions to be taken when any of the three buttons is pressed
     */
    private void setButtonActions() {
    	
    	// Inject buttons from included controller
    	this.backButton = this.actionBarController.getBackButton();
    	this.nextButton = this.actionBarController.getNextButton();
    	this.finishButton = this.actionBarController.getFinishButton();
    	
    	this.nextButton.setDisable(true);
    	this.finishButton.setDisable(true);
    	
    	// Back Action
        this.backButton.setOnAction( (event)->{
        	
        	// Load the advanced option controller
        	Parent root;
            try {
            																				
            	FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/wizards/newexperiment/wizard1.fxml"));
            	
                root = loader.load();
                Wizard1Controller controller = (Wizard1Controller) loader.getController();
                
        		// Pass instances and initialize
                controller.setRootLayoutController(this.rootLayoutController);
                controller.setStage(this.stage);
                controller.setDataModel(this.dataModel);
                controller.init();
                
                stage.setScene(new Scene(root, Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE));
                
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        	
        });
        
        
    	// Finish Action
        this.finishButton.setOnAction( (event)->{
        	
        	this.closeWizard();
        	
        });
        
    	
    }
    
    private DataModel getDataModel() {
    	
    	return this.dataModel;
    	
    }
    
    public void setRootLayoutController( RootLayoutController rlc) {
    	
    	this.rootLayoutController = rlc;
    	
    }
    
    public void setDataModel( DataModel datamodel ) {
    	
    	this.dataModel = datamodel;
    	
    }
    
    public void setStage( Stage s) {
    	
    	this.stage = s;
    	
    }
    
    
    
}