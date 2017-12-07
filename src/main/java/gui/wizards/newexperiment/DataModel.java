/**
 * 
 */
package gui.wizards.newexperiment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.datafx.controller.injection.scopes.FlowScoped;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * DataModel for storing and retrieving the information 
 * set by the user while navigating the wizard.
 */
@FlowScoped
public class DataModel {

	private StringProperty experimentName = new SimpleStringProperty();
	
	private StringProperty experimentDescription = new SimpleStringProperty();
	
	private BooleanProperty isDemultiplexed = new SimpleBooleanProperty(true);

	private BooleanProperty isPairedEnd = new SimpleBooleanProperty(false);
	
	private StringProperty primer5 = new SimpleStringProperty();
	
	private StringProperty primer3 = new SimpleStringProperty();
	
	private ObjectProperty<Integer> randomizedRegionSize = new SimpleObjectProperty<Integer>(0);
	
	private StringProperty forwardReadsFile = new SimpleStringProperty();
	
	private StringProperty reverseReadsFile = new SimpleStringProperty();
	
	private StringProperty projectPath = new SimpleStringProperty();
	
	private StringProperty fileFormat = new SimpleStringProperty("FASTQ");
	
	private SimpleIntegerProperty mapDBAptamerPoolBloomFilterCapacity= new SimpleIntegerProperty((Integer) Configuration.getDefaults().getInt("MapDBAptamerPool.bloomFilterCapacity"));

	private SimpleDoubleProperty mapDBAptamerPoolBloomFilterCollisionProbability = new SimpleDoubleProperty((Double) Configuration.getDefaults().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability"));
	
	private SimpleIntegerProperty mapDBAptamerPoolMaxTreeMapCapacity =  new SimpleIntegerProperty((Integer) Configuration.getDefaults().getInt("MapDBAptamerPool.maxTreeMapCapacity"));
	
	private SimpleDoubleProperty mapDBSelectionCycleBloomFilterCollisionProbability = new SimpleDoubleProperty((Double) Configuration.getDefaults().getDouble("MapDBSelectionCycle.bloomFilterCollisionProbability"));
	
	private ObjectProperty<Integer> performanceMaxNumberOfCores  = new SimpleObjectProperty<Integer>(utilities.Configuration.getDefaults().getInt("Performance.maxNumberOfCores"));
	
	private File lastSelectedDirectory = null;
	
	
	/**
	 * Container storing all instances of the selectionCycleDetails for later access
	 */
	private List<SelectionCycleDataModel> selectionCycleDataModels = new ArrayList<SelectionCycleDataModel>();
	
	
	/**
	 * @return the experimentName
	 */
	public StringProperty getExperimentName() {
		return experimentName;
	}

	/**
	 * @param experimentName the experimentName to set
	 */
	public void setExperimentName(StringProperty experimentName) {
		this.experimentName = experimentName;
	}

	/**
	 * @return the experimentDescription
	 */
	public StringProperty getExperimentDescription() {
		return experimentDescription;
	}

	/**
	 * @param experimentDescription the experimentDescription to set
	 */
	public void setExperimentDescription(StringProperty experimentDescription) {
		this.experimentDescription = experimentDescription;
	}

	/**
	 * @return the isDemultiplexed
	 */
	public BooleanProperty getIsDemultiplexed() {
		return isDemultiplexed;
	}

	/**
	 * @param isDemultiplexed the isDemultiplexed to set
	 */
	public void setIsDemultiplexed(BooleanProperty isDemultiplexed) {
		this.isDemultiplexed = isDemultiplexed;
	}

	/**
	 * @return the isPairedEnd
	 */
	public BooleanProperty getIsPairedEnd() {
		return isPairedEnd;
	}

	/**
	 * @param isPairedEnd the isPairedEnd to set
	 */
	public void setIsPairedEnd(BooleanProperty isPairedEnd) {
		this.isPairedEnd = isPairedEnd;
	}

	/**
	 * @return the selectionCycleDetailControllers
	 */
	public List<SelectionCycleDataModel> getSelectionCycleDataModels() {
		return selectionCycleDataModels;
	}

	/**
	 * @return the primer5
	 */
	public StringProperty getPrimer5() {
		return primer5;
	}

	/**
	 * @return the primer3
	 */
	public StringProperty getPrimer3() {
		return primer3;
	}

	/**
	 * @return the randomizedRegionSize
	 */
	public ObjectProperty<Integer> getRandomizedRegionSize() {
		return randomizedRegionSize;
	}

	/**
	 * @param randomizedRegionSize the randomizedRegionSize to set
	 */
	public void setRandomizedRegionSize(ObjectProperty<Integer> randomizedRegionSize) {
		this.randomizedRegionSize = randomizedRegionSize;
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
	 * @return the projectPath
	 */
	public StringProperty getProjectPath() {
		return projectPath;
	}

	/**
	 * @param projectPath the projectPath to set
	 */
	public void setProjectPath(StringProperty projectPath) {
		this.projectPath = projectPath;
	}

	/**
	 * @return the fileFormat
	 */
	public StringProperty getFileFormat() {
		return fileFormat;
	}

	/**
	 * @param fileFormat the fileFormat to set
	 */
	public void setFileFormat(StringProperty fileFormat) {
		this.fileFormat = fileFormat;
	}

	/**
	 * @return the mapDBAptamerPoolBloomFilterCapacityTextField
	 */
	public SimpleIntegerProperty getMapDBAptamerPoolBloomFilterCapacity() {
		return mapDBAptamerPoolBloomFilterCapacity;
	}

	/**
	 * @return the mapDBAptamerPoolBloomFilterCollisionProbabilityTextField
	 */
	public SimpleDoubleProperty getMapDBAptamerPoolBloomFilterCollisionProbability() {
		return mapDBAptamerPoolBloomFilterCollisionProbability;
	}

	/**
	 * @return the mapDBAptamerPoolMaxTreeMapCapacityTextField
	 */
	public SimpleIntegerProperty  getMapDBAptamerPoolMaxTreeMapCapacity() {
		return mapDBAptamerPoolMaxTreeMapCapacity;
	}

	/**
	 * @return the mapDBStructurePoolBloomFilterCollisionProbabilityTextField
	 */
	public SimpleDoubleProperty getMapDBSelectionCycleBloomFilterCollisionProbability() {
		return mapDBSelectionCycleBloomFilterCollisionProbability;
	}

	/**
	 * @return the performanceMaxNumberOfCoresSpinner
	 */
	public ObjectProperty<Integer> getPerformanceMaxNumberOfCores() {
		return performanceMaxNumberOfCores;
	}

	/**
	 * @return the lastSelectedDirectory
	 */
	public File getLastSelectedDirectory() {
		return lastSelectedDirectory;
	}

	/**
	 * @param lastSelectedDirectory the lastSelectedDirectory to set
	 */
	public void setLastSelectedDirectory(File lastSelectedDirectory) {
		this.lastSelectedDirectory = lastSelectedDirectory;
	}
	
	
}
