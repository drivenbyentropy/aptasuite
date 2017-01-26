/**
 * 
 */
package lib.aptamer.datastructures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import exceptions.DuplicateSelectionCycleException;
import exceptions.InvalidConfigurationException;
import exceptions.InvalidSelectionCycleException;
import utilities.AptaLogger;
import utilities.Configuration;


/**
 * @author Jan Hoinka
 *
 *         The class represents the parent class of all data structures related
 *         to AptaSUITE. It provides thread-safe, centralized access to all data
 *         (such as aptamers and selection cycles) and the results from the
 *         different algorithms implemented in this project.
 * 
 *         Each algorithm should receive a pointer of this class in its
 *         constructor and use the provided access methods to retrieve and store
 *         its data.
 */
public class Experiment implements Serializable{
	
	/**
	 * A unique name of this experiment
	 */
	private String name = null;
	
	/**
	 * A short, concise description of the experiment, such as target information
	 * aptamer size, etc.
	 */
	private String description = null;

	/**
	 * The aptamer pool instance
	 */
	private AptamerPool pool = null;
	
	/**
	 * Stores all structural data regarding this experiment 
	 */
	private StructurePool structures = null;

	/**
	 * The main selection cycles, sorted in increasing order.
	 * The size of this ArrayList should be equal to the highest sequenced 
	 * selection cycles of the experiment including the initial round which is
	 * assumed to always corresponding to index 0. The position of any selection cycle 
	 * which was not sequenced will be set to <code>null</code>.
	 */
	private ArrayList<SelectionCycle> selectionCycles = new ArrayList<>(); 
	
	
	/**
	 * The counter selection cycles. The index of the outer list, corresponds to the index
	 * of <code>selectionCycles</code>. If no counter selection exists for a specified round
	 * the value of that position is expected to be null
	 */
	private ArrayList<ArrayList<SelectionCycle>> counterSelectionCycles = new ArrayList<ArrayList<SelectionCycle>>();
	
	
	/**
	 * The control cycles. The index of the outer list, corresponds to the index
	 * of <code>selectionCycles</code>. If no counter selection exists for a specified round
	 * the value of that position is expected to be null
	 */
	private ArrayList<ArrayList<SelectionCycle>> controlSelectionCycles = new ArrayList<ArrayList<SelectionCycle>>();
	
	
	/**
	 * All selection cycles in the order as they appear in the configuration. This member is used
	 * to facilitate access to the cycle instances from other classes. 
	 */
	private ArrayList<SelectionCycle> allSelectionCycles = new ArrayList<SelectionCycle>();
	
	/**
	 * Constructs a new experiment. If <code>configFile</code> is null, an empty
	 * experiment is created, otherwise the data as defined in the file, will be
	 * loaded from disk.
	 * 
	 * @param configFile path to the configuration file
	 * @param newdb if true, a new database is created on file. 
	 * Any previously existing database will be deleted. If false, the existing database 
	 * will be read from disk.
	 */
	public Experiment(String configFile, boolean newdb) {
		
		// Register the Experiment with the configuration class
		Configuration.setExperiment(this);

		// Now set name and description
		this.name = Configuration.getParameters().getString("Experiment.name");
		this.description = Configuration.getParameters().getString("Experiment.description");

		// Create a new AptamerPool instance. Use reflection so we can define the backend
		// in the configuration file
		Class c = null;
		try {
			c = Class.forName("lib.aptamer.datastructures." + Configuration.getParameters().getString("AptamerPool.backend"));
		} catch (ClassNotFoundException e) {

			AptaLogger.log(Level.SEVERE, this.getClass(), "Error, the backend for the AptamerPool could not be found.");
			e.printStackTrace();
			System.exit(0);
		}
		
		// Try to instantiate the class...
		boolean instanceSuccess = false;
		try {
			pool = (AptamerPool)c.getConstructor(Path.class, boolean.class).newInstance(Paths.get(Configuration.getParameters().getString("Experiment.projectPath")), newdb);
			instanceSuccess = true;
		} catch (InstantiationException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error, could not instantiate the backend for the AptamerPool");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking construtor of AptamerPool backend");
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} finally{ // ... we cannot continue if this fails 
			if (!instanceSuccess){
				AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking AptamerPool backend");
				System.exit(0);
			}
		}
		
		// Set the SelectionCycle instances
		
		// Get all information regarding the selection cycles
		Integer[] rounds = null;
		String[] names = null;
		Boolean[] isControls = null;
		Boolean[] isCounters = null;
		
		try{
			rounds = (Integer[]) Configuration.getParameters().getArray(Integer.class, "SelectionCycle.round");
		}
		catch (Exception e){
			AptaLogger.log(Level.SEVERE, this.getClass(), "One or more SelectionCycle.round parameters are non-numerical. Please check your configuration.");
			throw new InvalidSelectionCycleException("One or more SelectionCycle.round parameters are non-numerical. Please check your configuration.");
		}
		
		try{
			names = Configuration.getParameters().getStringArray("SelectionCycle.name");
		}
		catch (Exception e){
			AptaLogger.log(Level.SEVERE, this.getClass(), "One or more SelectionCycle.name parameters are invalid. Please check your configuration.");
			throw new InvalidSelectionCycleException("One or more SelectionCycle.name parameters are invalid. Please check your configuration.");
		}
		
		try{
			isControls = (Boolean[]) Configuration.getParameters().getArray(Boolean.class,"SelectionCycle.isControlSelection");
		}
		catch (Exception e){
			AptaLogger.log(Level.SEVERE, this.getClass(), "One or more SelectionCycle.isCounterSelection parameters are non-boolean. Please check your configuration.");
			throw new InvalidSelectionCycleException("One or more SelectionCycle.isCounterSelection parameters are non-boolean. Please check your configuration.");
		}
		
		try{
			isCounters = (Boolean[]) Configuration.getParameters().getArray(Boolean.class,"SelectionCycle.isCounterSelection");
		}
		catch (Exception e){
			AptaLogger.log(Level.SEVERE, this.getClass(), "One or more SelectionCycle.isCounterSelection parameters are non-boolean. Please check your configuration.");
			throw new InvalidSelectionCycleException("One or more SelectionCycle.isCounterSelection parameters are non-boolean. Please check your configuration.");
		}		
		
		// Make sure all parameters required for instantiating the selection cycles are present in the configuration
		if (! (rounds.length == names.length && names.length == isControls.length && isControls.length == isCounters.length)){
			AptaLogger.log(Level.SEVERE, this.getClass(), "One or more parameters pertaining SelectionCycle.* are missing. Please check your configuration.");
			throw new InvalidSelectionCycleException("One or more parameters pertaining SelectionCycle.* are missing. Please check your configuration.");
		}
		
		// We need to determine the largest cycle number so we can allocate the ArrayLists of the
		// selection, counter, and control cycles
		int max_selection_cycles = rounds[0];
		for (Integer x: rounds){
			max_selection_cycles = Math.max(max_selection_cycles, x);
		}
		
		// Add as many null elements to the cycles as max_selection_cycle dictates
		for (int x=0; x<=max_selection_cycles; x++){
			selectionCycles.add(null);
			counterSelectionCycles.add(null);
			controlSelectionCycles.add(null);
		}
		
		// Now we can instantiate the selection cycles
		for (int x=0; x<rounds.length; x++){
			registerSelectionCycle(names[x], rounds[x], isControls[x], isCounters[x], newdb);
		}
		
	}

	/**
	 * Instantiates an implementation of <code>StructurePool</code>
	 * @param newdb if <code>true</code>, a new instance is created. if <code>false</code>, 
	 *  an existing instance is loaded from disk
	 */
	public void instantiateStructurePool(boolean newdb){
		
		// Create a new StructurePool instance. Use reflection so we can define the backend
		// in the configuration file
		Class s = null;
		try {
			s = Class.forName("lib.aptamer.datastructures." + Configuration.getParameters().getString("StructurePool.backend"));
		} catch (ClassNotFoundException e) {

			AptaLogger.log(Level.SEVERE, this.getClass(), "Error, the backend for the AptamerPool could not be found.");
			e.printStackTrace();
			System.exit(0);
		}
		
		// Try to instantiate the class...
		boolean instanceSuccess = false;
		try {
			structures = (StructurePool)s.getConstructor(Path.class, boolean.class).newInstance(Paths.get(Configuration.getParameters().getString("Experiment.projectPath")), newdb);
			instanceSuccess = true;
		} catch (InstantiationException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error, could not instantiate the backend for the StructurePool");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking construtor of StructurePool backend");
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} finally{ // ... we cannot continue if this fails 
			if (!instanceSuccess){
				AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking StructurePool backend");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Retrieves the AptamerPool instance of this Experiment
	 * @return
	 */
	public AptamerPool getAptamerPool() {
		return pool;
	}

	
	/**
	 * Registers the an aptamerpool instance for this experiment
	 * @param pool
	 */
	public void setAptamerPool(AptamerPool pool) {
		this.pool = pool;
	}
	
	/**
	 * Creates a new instance of <code>SelectionCycle<code> and adds it to the Experiment.
	 * This function performs error checking on the corresponding section of the configuration.
	 * @param name The unique name of the selection cycle
	 * @param round The selection round this cycle belongs to
	 * @param isControl Whether this selection cycle represents a control selection, eg. against a homologous target. Mutually exclusive with <code>isCounterSelection</code>.
	 * @param isCounterSelection Whether this selection represents a counter selection. Mutually exclusive with <code>isControl</code>.
	 * @return the instance of the SelectionCycle that was created in the process
	 */
	public SelectionCycle registerSelectionCycle(String name, int round, boolean isControlSelection, boolean isCounterSelection, boolean newdb){
		
		// Sanity Checks
		// We cannot add this cycle if another cycle with the same name is already present
		if (getSelectionCycleById(name) != null){
			AptaLogger.log(Level.SEVERE, this.getClass(), "One or more selection cycles contain the same name: " + name + " . Please check your configuration");
			throw new DuplicateSelectionCycleException("One or more selection cycles contain the same name: " + name + " . Please check your configuration");
		}
		
		// Make sure the selection round is a valid number and >=0
		if (round < 0){
			AptaLogger.log(Level.SEVERE, this.getClass(), "The selection cycle round " + round + " of " + name + " is invalid. Please check your configuration.");
			throw new InvalidSelectionCycleException("The selection cycle round " + round + " of " + name + " is invalid. Please check your configuration.");
		}
		
		// If the selection cycle is not a control, nor a counter selection, we 
		
		SelectionCycle cycle = null;
		
		// Create a new SelectionCycle instance. Use reflection so we can define the backend in the configuration
		Class selection_cycle_class = null;
		try {
			selection_cycle_class = Class.forName("lib.aptamer.datastructures." + Configuration.getParameters().getString("SelectionCycle.backend"));
		} catch (ClassNotFoundException e) {

			AptaLogger.log(Level.SEVERE, this.getClass(), "Error, the backend for the SelectionCycle could not be found.");
			e.printStackTrace();
			System.exit(0);
		}
		
		// Try to instantiate the class
		boolean instanceSuccess = false; 
		try {
			cycle = (SelectionCycle)selection_cycle_class.getConstructor(String.class, int.class, boolean.class, boolean.class, boolean.class).newInstance(name, round, isControlSelection, isCounterSelection, newdb);
			instanceSuccess = true;
		} catch (InstantiationException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error, could not instantiate the backend for the SelectionCycle");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking construtor of SelectionCycle backend");
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} finally{
			if (!instanceSuccess){
				AptaLogger.log(Level.SEVERE, this.getClass(), "Error invoking SelectionCycle backend");
				System.exit(0);
			}
		}
		
		// Assign the selection cycle to the corresponding datastructure
		if (!cycle.isControlSelection() && !cycle.isCounterSelection()){
			selectionCycles.set(cycle.getRound(), cycle);
		}
		
		else if (cycle.isControlSelection()){
			
			// We need to create a container if not present 
			if (controlSelectionCycles.get(cycle.getRound()) == null){
				controlSelectionCycles.set(cycle.getRound(), new ArrayList<SelectionCycle>());
			}
			// Now we can add the cycle
			controlSelectionCycles.get(cycle.getRound()).add(cycle);
		}
		
		else if (cycle.isCounterSelection()){
			
			// We need to create a container if not present 
			if (counterSelectionCycles.get(cycle.getRound()) == null){
				counterSelectionCycles.set(cycle.getRound(), new ArrayList<SelectionCycle>());
			}
			// Now we can add the cycle
			counterSelectionCycles.get(cycle.getRound()).add(cycle);
		}
		
		// finally, add it to the complete list of cyckes
		allSelectionCycles.add(cycle);
		
		return cycle;
	}
	
	
	/**
	 * Searches for the SelectionCycle with name <code>id</code> in all cycles (i.e. counter and controls as well)
	 * @param id the name of the selection cycle to be found
	 * @return Instance of <code>SelectionCycle</code> or null if no such cycle exists.
	 */
	public SelectionCycle getSelectionCycleById(String id){
		
		//iterate over all cycles and search for a matching id
		for (SelectionCycle c : selectionCycles){
			
			//skip empty slots
			if (c == null){
				continue;
			}
			
			if (c.getName().equals("id")){
				return c;
			}
			
			// if not found, we also have to go though the counter and control selections
			else{
				for(SelectionCycle d : c.getControlCycles()){
					if (d.getName().equals("id")){
						return d;
					}
				}
				
				for(SelectionCycle d : c.getCounterSelectionCycles()){
					if (d.getName().equals("id")){
						return d;
					}
				}
			}
		}
		
		// the cycle does not exist
		return null;
	}
	
	/**
	 * Provides access to all registered selection cycles of this experiment
	 * @return
	 */
	public ArrayList<SelectionCycle> getSelectionCycles(){
		return this.selectionCycles;
	}
	
	
	/**
	 * Provides access to all registered counter selection cycles of this experiment
	 * @return
	 */
	public ArrayList<ArrayList<SelectionCycle>> getCounterSelectionCycles(){
		return this.counterSelectionCycles;
	}
	
	
	/**
	 * Provides access to all registered control selection cycles of this experiment
	 * @return
	 */
	public ArrayList<ArrayList<SelectionCycle>> getControlSelectionCycles(){
		return this.controlSelectionCycles;
	}
	
	/**
	 * Provides access to all created instances of selection cycles in order of apperance
	 * in the configuration
	 * @return ArrayList<SelectionCycle>
	 */
	public ArrayList<SelectionCycle> getAllSelectionCycles() {
		return allSelectionCycles;
	}

	/**
	 * Produces a string representation of the selection cycle topology as defined in the configuration
	 * @return
	 */
	public String getSelectionCycleConfiguration(){
		
		StringBuilder sb = new StringBuilder("Experiment Setup\n");
		sb.append("│\n");
		
		for( int x=0; x<selectionCycles.size(); x++ ){
			
			if(x == selectionCycles.size()-1){
				sb.append("└── Round " + x +": ");
			}
			else{
				sb.append("├── Round " + x +": ");
			}
				
			if (selectionCycles.get(x) != null){
				sb.append(selectionCycles.get(x));
			}
			else{
				sb.append("N/A");
			}
			sb.append("\n");
			
			sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    │\n");
			sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    ├─ Counter Selections:");
			
			if (counterSelectionCycles.get(x) != null){
				sb.append("\n");
				for (SelectionCycle c : counterSelectionCycles.get(x)){
					sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    │  │\n");
					sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    │  └── " + c + "\n");
				}
			}
			else{
				sb.append(" N/A\n");
			}
			
			
			sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    │\n");
			sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    ├─ Control Selections:");
			
			if (controlSelectionCycles.get(x) != null){
				sb.append("\n");
				for (SelectionCycle c : controlSelectionCycles.get(x)){
					sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    │  │\n");
					sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "    │  └── " + c + "\n");
				}
			}			
			else{
				sb.append(" N/A\n");
			}
			
			sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "\n");
			sb.append((x==selectionCycles.size()-1 ? "" : "│") +  "\n");
			
		}
		
		return sb.toString();
		
	}
	
	/**
	 * Get the name of this experiment
	 * @return
	 */
	public String getName(){
		return this.name;
	}
	
	
	/**
	 * Get the description of this experiment
	 * @return
	 */
	public String getDescriptio(){
		return this.description;
	}
	
	/**
	 * Get the structural pool instance of this experiment
	 * @return
	 */
	public StructurePool getStructurePool(){
		return this.structures;
	}
}
