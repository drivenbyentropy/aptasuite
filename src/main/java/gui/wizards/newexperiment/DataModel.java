/**
 * 
 */
package gui.wizards.newexperiment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
public class DataModel {

	private StringProperty experimentName = new SimpleStringProperty();
	
	private StringProperty experimentDescription = new SimpleStringProperty();
	
	private BooleanProperty isDemultiplexed = new SimpleBooleanProperty(true);

	private BooleanProperty isPairedEnd = new SimpleBooleanProperty(false);
	
	private BooleanProperty storeReverseComplement = new SimpleBooleanProperty(false);
	
	private BooleanProperty onlyRandomizedRegionInData = new SimpleBooleanProperty(false);
	
	private BooleanProperty undeterminedToFile = new SimpleBooleanProperty(false);
	
	private BooleanProperty checkReverseComplement = new SimpleBooleanProperty(false);
	
	private StringProperty primer5 = new SimpleStringProperty();
	
	private StringProperty primer3 = new SimpleStringProperty();
	
	private ObjectProperty<Integer> randomizedRegionSize = new SimpleObjectProperty<Integer>(0);
	
	private ObjectProperty<Integer> randomizedRegionSizeLower = new SimpleObjectProperty<Integer>(0);
	
	private ObjectProperty<Integer> randomizedRegionSizeUpper = new SimpleObjectProperty<Integer>(0);
	
	private StringProperty forwardReadsFile = new SimpleStringProperty();
	
	private StringProperty reverseReadsFile = new SimpleStringProperty();
	
	private StringProperty projectPath = new SimpleStringProperty();
	
	private StringProperty fileFormat = new SimpleStringProperty("FASTQ");
	
	private SimpleIntegerProperty mapDBAptamerPoolBloomFilterCapacity= new SimpleIntegerProperty( Configuration.getDefaults().containsKey("MapDBAptamerPool.bloomFilterCapacity") ? Configuration.getDefaults().getInt("MapDBAptamerPool.bloomFilterCapacity") : Configuration.getParameters().getInt("MapDBAptamerPool.bloomFilterCapacity") );

	private SimpleDoubleProperty mapDBAptamerPoolBloomFilterCollisionProbability = new SimpleDoubleProperty( Configuration.getDefaults().containsKey("MapDBAptamerPool.bloomFilterCollisionProbability") ? Configuration.getDefaults().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability") : Configuration.getParameters().getDouble("MapDBAptamerPool.bloomFilterCollisionProbability") );
	
	private SimpleIntegerProperty mapDBAptamerPoolMaxTreeMapCapacity =  new SimpleIntegerProperty( Configuration.getDefaults().containsKey("MapDBAptamerPool.maxTreeMapCapacity") ? Configuration.getDefaults().getInt("MapDBAptamerPool.maxTreeMapCapacity") : Configuration.getParameters().getInt("MapDBAptamerPool.maxTreeMapCapacity") );
	
	private SimpleDoubleProperty mapDBSelectionCycleBloomFilterCollisionProbability = new SimpleDoubleProperty( Configuration.getDefaults().containsKey("MapDBSelectionCycle.bloomFilterCollisionProbability") ? Configuration.getDefaults().getDouble("MapDBSelectionCycle.bloomFilterCollisionProbability") : Configuration.getParameters().getDouble("MapDBSelectionCycle.bloomFilterCollisionProbability") ); 
	
	private ObjectProperty<Integer> performanceMaxNumberOfCores  = new SimpleObjectProperty<Integer>( (Integer) (Configuration.getDefaults().containsKey("Performance.maxNumberOfCores") ? Configuration.getDefaults().getInt("Performance.maxNumberOfCores") : Configuration.getParameters().getInt("Performance.maxNumberOfCores")) );
	
	private SimpleIntegerProperty aptaplexParserPairedEndMinOverlap = new SimpleIntegerProperty( Configuration.getDefaults().containsKey("AptaplexParser.PairedEndMinOverlap") ? Configuration.getDefaults().getInt("AptaplexParser.PairedEndMinOverlap") : Configuration.getParameters().getInt("AptaplexParser.PairedEndMinOverlap") );
	
	private SimpleIntegerProperty aptaplexParserPairedEndMaxMutations = new SimpleIntegerProperty( Configuration.getDefaults().containsKey("AptaplexParser.PairedEndMaxMutations") ? Configuration.getDefaults().getInt("AptaplexParser.PairedEndMaxMutations") : Configuration.getParameters().getInt("AptaplexParser.PairedEndMaxMutations") );
	
	private SimpleIntegerProperty aptaplexParserPairedEndMaxScoreValue = new SimpleIntegerProperty( Configuration.getDefaults().containsKey("AptaplexParser.PairedEndMaxScoreValue") ? Configuration.getDefaults().getInt("AptaplexParser.PairedEndMaxScoreValue") : Configuration.getParameters().getInt("AptaplexParser.PairedEndMaxScoreValue") );
	
	private SimpleIntegerProperty aptaplexParserBarcodeTolerance = new SimpleIntegerProperty( Configuration.getDefaults().containsKey("AptaplexParser.BarcodeTolerance") ? Configuration.getDefaults().getInt("AptaplexParser.BarcodeTolerance") : Configuration.getParameters().getInt("AptaplexParser.BarcodeTolerance") ); 
	
	private SimpleIntegerProperty aptaplexParserPrimerTolerance = new SimpleIntegerProperty( Configuration.getDefaults().containsKey("AptaplexParser.PrimerTolerance") ? Configuration.getDefaults().getInt("AptaplexParser.PrimerTolerance") : Configuration.getParameters().getInt("AptaplexParser.PrimerTolerance") );
	
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
	 * @return the randomizedRegionSizeLower
	 */
	public ObjectProperty<Integer> getRandomizedRegionSizeLower() {
		return randomizedRegionSizeLower;
	}

	/**
	 * @param randomizedRegionSizeLower the randomizedRegionSizeLower to set
	 */
	public void setRandomizedRegionSizeLower(ObjectProperty<Integer> randomizedRegionSizeLower) {
		this.randomizedRegionSizeLower = randomizedRegionSizeLower;
	}
	
	/**
	 * @return the randomizedRegionSizeUpper
	 */
	public ObjectProperty<Integer> getRandomizedRegionSizeUpper() {
		return randomizedRegionSizeUpper;
	}

	/**
	 * @param randomizedRegionSizeUpper the randomizedRegionSizeUpper to set
	 */
	public void setRandomizedRegionSizeUpper(ObjectProperty<Integer> randomizedRegionSizeUpper) {
		this.randomizedRegionSizeUpper = randomizedRegionSizeUpper;
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

	/**
	 * @return the storeReverseComplement
	 */
	public BooleanProperty getStoreReverseComplement() {
		return storeReverseComplement;
	}

	/**
	 * @param storeReverseComplement the storeReverseComplement to set
	 */
	public void setStoreReverseComplement(BooleanProperty storeReverseComplement) {
		this.storeReverseComplement = storeReverseComplement;
	}

	/**
	 * @return the storeReverseComplement
	 */
	public BooleanProperty getOnlyRandomizedRegionInData() {
		return onlyRandomizedRegionInData;
	}

	/**
	 * @param storeReverseComplement the storeReverseComplement to set
	 */
	public void setOnlyRandomizedRegionInData(BooleanProperty onlyRandomizedRegionInData) {
		this.onlyRandomizedRegionInData = onlyRandomizedRegionInData;
	}
	
	
	/**
	 * @return the storeReverseComplement
	 */
	public BooleanProperty getCheckReverseComplement() {
		return checkReverseComplement;
	}

	/**
	 * @param storeReverseComplement the storeReverseComplement to set
	 */
	public void setCheckReverseComplement(BooleanProperty checkReverseComplement) {
		this.checkReverseComplement = checkReverseComplement;
	}
	
	
	/**
	 * @return the storeReverseComplement
	 */
	public BooleanProperty getUndeterminedToFile() {
		return undeterminedToFile;
	}

	/**
	 * @param storeReverseComplement the storeReverseComplement to set
	 */
	public void setUndeterminedToFile(BooleanProperty undeterminedToFile) {
		this.undeterminedToFile = undeterminedToFile;
	}
	
	/**
	 * @return the aptaplexParserPairedEndMinOverlap
	 */
	public SimpleIntegerProperty getAptaplexParserPairedEndMinOverlap() {
		return aptaplexParserPairedEndMinOverlap;
	}

	/**
	 * @param aptaplexParserPairedEndMinOverlap the aptaplexParserPairedEndMinOverlap to set
	 */
	public void setAptaplexParserPairedEndMinOverlap(SimpleIntegerProperty aptaplexParserPairedEndMinOverlap) {
		this.aptaplexParserPairedEndMinOverlap = aptaplexParserPairedEndMinOverlap;
	}

	/**
	 * @return the aptaplexParserPairedEndMaxMutations
	 */
	public SimpleIntegerProperty getAptaplexParserPairedEndMaxMutations() {
		return aptaplexParserPairedEndMaxMutations;
	}

	/**
	 * @param aptaplexParserPairedEndMaxMutations the aptaplexParserPairedEndMaxMutations to set
	 */
	public void setAptaplexParserPairedEndMaxMutations(SimpleIntegerProperty aptaplexParserPairedEndMaxMutations) {
		this.aptaplexParserPairedEndMaxMutations = aptaplexParserPairedEndMaxMutations;
	}

	/**
	 * @return the aptaplexParserPairedEndMaxScoreValue
	 */
	public SimpleIntegerProperty getAptaplexParserPairedEndMaxScoreValue() {
		return aptaplexParserPairedEndMaxScoreValue;
	}

	/**
	 * @param aptaplexParserPairedEndMaxScoreValue the aptaplexParserPairedEndMaxScoreValue to set
	 */
	public void setAptaplexParserPairedEndMaxScoreValue(SimpleIntegerProperty aptaplexParserPairedEndMaxScoreValue) {
		this.aptaplexParserPairedEndMaxScoreValue = aptaplexParserPairedEndMaxScoreValue;
	}

	/**
	 * @return the aptaplexParserBarcodeTolerance
	 */
	public SimpleIntegerProperty getAptaplexParserBarcodeTolerance() {
		return aptaplexParserBarcodeTolerance;
	}

	/**
	 * @param aptaplexParserBarcodeTolerance the aptaplexParserBarcodeTolerance to set
	 */
	public void setAptaplexParserBarcodeTolerance(SimpleIntegerProperty aptaplexParserBarcodeTolerance) {
		this.aptaplexParserBarcodeTolerance = aptaplexParserBarcodeTolerance;
	}

	/**
	 * @return the aptaplexParserPrimerTolerance
	 */
	public SimpleIntegerProperty getAptaplexParserPrimerTolerance() {
		return aptaplexParserPrimerTolerance;
	}

	/**
	 * @param aptaplexParserPrimerTolerance the aptaplexParserPrimerTolerance to set
	 */
	public void setAptaplexParserPrimerTolerance(SimpleIntegerProperty aptaplexParserPrimerTolerance) {
		this.aptaplexParserPrimerTolerance = aptaplexParserPrimerTolerance;
	}
	
	
}
