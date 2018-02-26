package gui.wizards.newexperiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import gui.core.RootLayoutController;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.action.LinkAction;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.aptaplex.AptaPlexParser;
import lib.parser.aptaplex.AptaPlexProgress;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * This is a view controller for one of the steps in the wizard. The "back" and "finish" buttons of the action-bar that
 * is shown on each view of the wizard are defined in the AbstractWizardController class. So this class only needs to
 * define the "next" button. By using the @LinkAction annotation this button will link on the next step of the
 * wizard. This annotation was already described in tutorial 2.
 *
 * When looking at the @FXMLController annotation of the class you can find a new feature. next to the fxml files that
 * defines the view of the wizard step a "title" is added. This defines the title of the view. Because the wizard is
 * added to a Stage by using the Flow.startInStage() method the title of the flow is automatically bound to the window
 * title of the Stage. So whenever the view in the flow changes the title of the application window will change to the
 * defined title of the view. As you will learn in future tutorial you can easily change the title of a view in code.
 * In addition to the title other metadata like a icon can be defined for a view or flow.
 */
@ViewController(value="wizard2.fxml", title = "Wizard: Step 2")
public class Wizard2Controller extends AbstractWizardController {

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
    @LinkAction(WizardAdvancedOptionsController.class)
    private Button nextButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    @ActionTrigger("closeWizard")
    private Button finishButton;
    
    @FXML
    private Button importDataButton;
    
    @FXML
    private Label loggerLabel1;
    
    @FXML
    private Label loggerLabel2;
    
    @FXML
    private Label loggerLabel3;
    
    @FXMLApplicationContext
    private ApplicationContext context;
    
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
    
    private RootLayoutController rootLayoutController;
    
    
    @PostConstruct
    public void init() {
    	
    	// Get the main GUI controller form the DataFX context so that we initiate the tabs once parsing completes
    	context = ApplicationContext.getInstance();
    	rootLayoutController = (RootLayoutController) context.getRegisteredObject("RootLayoutController");
    	
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
						System.out.println("GOT PROGRESS INSTANCE  " + progress);
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
								System.out.println("Progress " + progress);
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
    @ActionMethod("closeWizard")
    public void closeWizard() {
    	
    	// We need to set the experiment in the main controller
    	rootLayoutController.setExperiment(experiment);
    	
    	// Notify the gui to initialize loading the tabs
    	rootLayoutController.showInitialTabs();
    	
    	// Get a handle to the stage
        Stage stage = (Stage) getFinishButton().getScene().getWindow();

        // Close it
        stage.close();
    	
    }
    
    
}