/**
 * 
 */
package gui.wizards.structureprofileprediction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.EvictingQueue;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import lib.aptamer.datastructures.Experiment;
import lib.structure.capr.CapRFactory;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 */
public class StructureProfilePredictionWizardController {

	@FXML
	private ProgressBar progressBar;
	
	@FXML
	private Label progressLabel;
	
	@FXML
	private Label etaLabel;
	
	@FXML
	private Label percentLabel;
	
	@FXML
	private Button closeButton;
	
	@FXML
	private Button cancelButton;
	
	private Experiment experiment = Configuration.getExperiment();
	
	private Thread structureThread;
	
	private Path projectPath = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"));
	
	/**
	 * Average the eta time over 10 seconds
	 */
	private Queue<Long> etaQueue = EvictingQueue.create(10);
	
	/**
	 * Thread safe hooks which can be monitored 
	 * to decide a programs flow  
	 */
	private AtomicBoolean interrupted;
	
	private AtomicBoolean completed;
	
	
	public void start() {

		// clean up old data if required
		try {
			FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		//Start prediction right away, dont block UI
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				
				runStructurePrediction();
				
				// now that we have the data, set any file backed implementations 
				// of StructurePool to read only. Do this only if the prediction has not 
				// been cancled by the user.
				experiment.getStructurePool().setReadOnly();
			
				closeButton.setDisable(false);
				cancelButton.setDisable(true);
				
				completed.set(true);
			}
						
		});
		
		t.start();
		
	}
	
	
	private void runStructurePrediction() {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Starting Structure Predition");
		
		// Create a new instance of the StructurePool
		experiment.instantiateStructurePool(true, true);
		
		// Start parallel processing of structure prediction
		CapRFactory caprf = new CapRFactory(experiment.getAptamerPool().iterator());
		
		structureThread = new Thread(caprf);

		AptaLogger.log(Level.INFO, this.getClass(), "Starting Structure Prediction using " + Configuration.getParameters().getInt("Performance.maxNumberOfCores") + " threads:");
		long tParserStart = System.currentTimeMillis();
		structureThread.start();

		// we need to add a shutdown hook for the CapRFactory in case the
		// user presses ctl-c
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (structureThread != null) {

						structureThread.interrupt();
						structureThread.join();

					}
				} catch (InterruptedException e) {
					AptaLogger.log(Level.SEVERE, this.getClass(), "User interrupt on structureTread");
				}
			}
		});

		AptaLogger.log(Level.INFO, this.getClass(), "Predicting...");

		long sps = 0;
		try {
			while (structureThread.isAlive() && !structureThread.isInterrupted()) {
				
					long current_progress = caprf.getProgress().longValue();
					long eta = (experiment.getAptamerPool().size()-current_progress)/(current_progress-sps+1);
					etaQueue.add(eta);
					//System.out.print(String.format("Completed: %s/%s (%s structures per second  ETA:%s)     " + "\r", current_progress, experiment.getAptamerPool().size(), current_progress-sps, String.format("%02d:%02d:%02d", eta / 3600, (eta % 3600) / 60, eta % 60)));
					
					long average_eta = 0;
					for (long item : etaQueue) {
						average_eta += item;
					}
					average_eta /= etaQueue.size();
					
					long sps_final = sps;
					long average_eta_final = average_eta;
					Platform.runLater(() -> { 
						progressLabel.setText(String.format("Predicted %s of %s structures", current_progress, experiment.getAptamerPool().size()));
						etaLabel.setText(String.format("%s structures per second  (ETA: %s)", current_progress-sps_final, String.format("%02d:%02d:%02d", average_eta_final / 3600, (average_eta_final % 3600) / 60, average_eta_final % 60)));
						
						progressBar.setProgress( (double) current_progress / experiment.getAptamerPool().size());
						percentLabel.setText(String.format("%.1f%%", ((double)current_progress / experiment.getAptamerPool().size())*100  ));
					});
					
					sps = current_progress;
					
					// Once every second should suffice
					Thread.sleep(1000);
			}
		} catch (InterruptedException ie) {
			
		}
		
		// final update
		if (!interrupted.get()) {
			
			Platform.runLater(() -> { 
				progressLabel.setText(String.format("Predicted %s of %s structures", experiment.getAptamerPool().size(), experiment.getAptamerPool().size()));
				etaLabel.setText(String.format("%s structures per second  (ETA: %s)", 0, String.format("%02dh:%02dm:%02ds", 0, 0, 0)));
				progressBar.setProgress( 1.0 );
				percentLabel.setText(String.format("%.1f%%", 100.0));
			});

		}
		
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Structure prediction completed in %s seconds.\n",
				((System.currentTimeMillis() - tParserStart) / 1000.0)));
		
	}
	
	@FXML
	public void cancelPrediction() {
		
		interrupted.set(true);
		
		structureThread.interrupt();
		try {
			structureThread.join();
		} catch (InterruptedException e) {
		}
		this.experiment.getStructurePool().close();
		this.experiment.setStructurePool(null);
		
		// clean up old data if required
		try {
			FileUtils.deleteDirectory(Paths.get(projectPath.toString(), "structuredata").toFile());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Platform.runLater(() -> { 
			progressLabel.setText(String.format("Structure precition canceled"));
			etaLabel.setText(String.format("%s structures per second  (ETA: %s)", 0, String.format("%02dh:%02dm:%02ds", 0, 0, 0)));
		});
		
		
		this.closeButton.setDisable(false);
		this.cancelButton.setDisable(true);
	}
	
	@FXML
	public void closeWindow() {
		
		// get a handle to the stage
        Stage stage = (Stage) this.closeButton.getScene().getWindow();
        // close it
        stage.close();
		
	}
	
	public void setInterrupted( AtomicBoolean interrupted ) {
		
		this.interrupted = interrupted;
		
	}
	
	public void setCompleted( AtomicBoolean completed ) {
		
		this.completed = completed;
		
	}
	
}
