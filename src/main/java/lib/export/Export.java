/**
 * 
 */
package lib.export;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.factory.primitive.IntLists;

import exceptions.InvalidConfigurationException;
import gnu.trove.map.hash.TIntIntHashMap;
import lib.aptacluster.Buckets;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.SelectionCycle;
import lib.aptamer.datastructures.StructurePool;
import scala.annotation.compileTimeOnly;
import utilities.AptaLogger;
import utilities.Configuration;
import utilities.QSComparator;
import utilities.Quicksort;

/**
 * @author Jan Hoinka
 * This class contains all the functions required
 * to export the data of an experiment into 
 * text files.
 */
public class Export {

	
	/**
	 * Writes the specified pool <code>p</code> to 
	 * persistent storage
	 * @param ap instance of <code>AptamerPool</code>
	 * @param p the location at which the file should be created
	 */
	public void Pool(AptamerPool ap, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);
		
		// we want to sort the output by the cumulative frequency of the aptamers 
		int[] aptamer_ids = new int[ap.size()];
		int[] aptamer_sums = new int[ap.size()];
		
		int counter = 0;
		for ( Integer item : ap.id_iterator()){
			
			aptamer_ids[counter] = item;
			counter++;
			
		}
		
		Quicksort.sort(aptamer_ids);
		
		for ( SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles() ){
			
			counter = 0;
			for ( Entry<Integer, Integer> cycle_it : cycle.iterator()){
				
				// this is guaranteed to terminate since cycle ids are a subset 
				// of the aptamer pool ids
				while (aptamer_ids[counter] != cycle_it.getKey()){
					counter++;
				}
				aptamer_sums[counter] += cycle_it.getValue();
			}
			
		}
		
		/**
		 * @author Jan Hoinka
		 * This comparator sorts in descending order
		 */
		class AptamerSizeQSComparator implements QSComparator{

				@Override
				public int compare(int a, int b) {
					return -Integer.compare(a, b);
				}
							
		}
		Quicksort.sort(aptamer_ids, aptamer_sums, new AptamerSizeQSComparator());
		
		// Allow unused space to be garbadge collected
		aptamer_sums = null;
		
		// Prepare the header
		String[] buffer = new String[2+Configuration.getExperiment().getAllSelectionCycles().size()];
		buffer[0] = "Aptamer Id";
		buffer[1] = "Sequence";
		counter = 2;
		for ( SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles() ){
			
			buffer[counter] = cycle.getName();
			
			counter++;
		}
		writer.write(String.join("\t",buffer)+"\n");
		
		// Write sequences
		boolean includer_primer_regions = Configuration.getParameters().getBoolean("Export.IncludePrimerRegions");
		String cardinality_format = Configuration.getParameters().getString("Export.PoolCardinalityFormat");
		
		int pool_size = ap.size();
		int progress = 0;
		for ( int aptamer_id : aptamer_ids){
			
			
			progress++;
			if (progress % 1000 == 0){
				System.out.print(String.format("Progress %s/%s\r", progress,pool_size));
			}
			
			// First id and and sequence
			buffer[0] = String.format("%s", aptamer_id);
			
			byte[] sequence = ap.getAptamer(aptamer_id);
			
			if (includer_primer_regions){
				
				buffer[1] = new String(sequence);
				
			}
			else{
				AptamerBounds ab = ap.getAptamerBounds(aptamer_id);
				try{
					buffer[1] = new String(sequence, ab.startIndex, (ab.endIndex-ab.startIndex));
				}
				catch(Exception e){
					System.out.println(new String(sequence));
					System.out.println(ab.startIndex + "   "+ ab.endIndex  );
					AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			// Now the counts
			counter = 2;
			for ( SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles() ){
				
				if (cardinality_format.equals("counts")) {
					buffer[counter] = String.format("%s", cycle.getAptamerCardinality(aptamer_id));
				}
				if (cardinality_format.equals("frequencies")) {
					buffer[counter] = String.format("%10.3e", new Double(cycle.getAptamerCardinality(aptamer_id)) / new Double(cycle.getSize()) );
				}
				
				counter++;
				
			}
			
			// Finally write to file
			writer.write(String.join("\t",buffer)+"\n");
		}
		
		// Final update
		System.out.print(String.format("Progress %s/%s\r", progress,pool_size));
		System.out.println();
		
		// Finalize
		writer.close();
	}

	/**
	 * Writes the specified SelectionCycle <code>p</code> to 
	 * file
	 * @param ap instance of <code>AptamerPool</code>
	 * @param p the location at which the file should be created
	 */
	public void Cycle(SelectionCycle sc, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);

		// Do the same for the formatter
		ExportFormat<byte[]> formatter = null;
		switch (Configuration.getParameters().getString("Export.SequenceFormat")) {
		
			case "fastq": 
				formatter = new FastqExportFormat(sc.getName());
				break;
				
			case "fasta":
				formatter = new FastaExportFormat(sc.getName());
				break;
		
			default:
				AptaLogger.log(Level.SEVERE, this.getClass(), "Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized. Exiting");
				throw new InvalidConfigurationException("Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized.");
		}
		
		// Write sequences
		for ( Entry<byte[], Integer> entry : sc.sequence_iterator()){
			
			// get the id of that aptamer
			int id = Configuration.getExperiment().getAptamerPool().getIdentifier(entry.getKey());
			
			// write sequence an many times as its cardinality
			for( int x=0; x<entry.getValue(); x++) {
				writer.write(formatter.format(id, entry.getKey()));
			}
		}
		
		// Finalize
		writer.close();
	}
	
	/**
	 * Writes the specified SelectionCycle <code>p</code> to 
	 * persistent storage
	 * @param ap instance of <code>AptamerPool</code>
	 * @param p the location at which the file should be created
	 */
	public void Structures(StructurePool sp, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);

		// Load the formatter
		ExportFormat<double[]> formatter= new StructureExportFormat();
		
		// Write structures 
		for ( Map.Entry<Integer, double[]> entry : sp.iterator() ){
			//ID and Sequence
			writer.write(formatter.format(entry.getKey(), entry.getValue()));
		}
		
		// Finalize
		writer.close();
	}


	/**
	 * Writes the clusters as computed by AptaCLUSTER to file in the following format.
	 * Cluster <code>id</code>
	 * Sequence_member1 count1
	 * Sequence_member2 count2
	 * Sequence_member3 count3
	 * 
	 * Sorted by cluster size. Sorted by aptamer count inside clusters.
	 * @param sc the selection cycles whos clusters to export
	 * @param cc the cluster container produces by AptaCluster
	 * @param p the path to which export the data to
	 */
	public void Clusters(SelectionCycle sc, ClusterContainer cc, Path p){
		
		//Depending on the ClusterFilterCriteria, call the appropriate export routines
		switch (Configuration.getParameters().getString("Export.ClusterFilterCriteria")) {
		
			case "ClusterSize": 		ClustersBySize(sc, cc, p);
										break;
		
			case "ClusterDiversity":	ClustersByDiversity(sc, cc, p);
										break;
		
		}
		
	}
	
	/**
	 * Exports clusters sorted by the cluster diversity, i.e. the number of unique sequences per cluster.
	 * 
	 * Sorted by cluster size. Sorted by aptamer count inside clusters.
	 * @param sc the selection cycles whos clusters to export
	 * @param cc the cluster container produces by AptaCluster
	 * @param p the path to which export the data to
	 */
	private void ClustersByDiversity(SelectionCycle sc, ClusterContainer cc, Path p){
		
		// Load a writer instance depending on the configuration
		ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
		writer.open(p);
		
		// Arrange the data for export
		// Specifically we need a hashmap with cluster ids as keys and lists of aptamer ids as values
		Buckets buckets = Buckets.withExpectedSize(sc.getSize());
		
		// We will also need to sort the keys by size of their values
		
		// Iterate over the aptamers in the selection cycle and extract cluster membership
		for ( Entry<Integer, Integer> item : sc.iterator()){

			int cluster_id = cc.getClusterId(item.getKey());
			
			// Only take into account items which have a cluster membership
			if (cluster_id == -1) {
				continue;
			}
			
			if ( !buckets.contains(cluster_id) ){
				
				buckets.justPut(cluster_id, IntLists.mutable.of(item.getKey()));
				
			}
			else {
				buckets.get(cluster_id).add(item.getKey());
			}
			
		}
		int total_number_of_clusters = buckets.size();
		
		// Now remove all clusters which do not contain the specified minimal number of items
		int minimal_cluster_size = Configuration.getParameters().getInt("Export.MinimalClusterSize");

		MutableIntList to_be_deleted = IntLists.mutable.empty();
		for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
			
			if( bucket.getValue().size() <= minimal_cluster_size ){
			
				to_be_deleted.add(bucket.getKey());
			
			}
			
		}
		MutableIntIterator it = to_be_deleted.intIterator();
		while (it.hasNext()){

			buckets.justRemove(it.next());
			
		}
		
		// We need to sort the clusters according to their sizes
		int[] cluster_ids_by_size = new int[buckets.size()];
		
		int counter = 0;
		for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
			
			cluster_ids_by_size[counter] = bucket.getKey();
			counter++;
			
		}
		
		/**
		 * @author Jan Hoinka
		 * This comparator sorts in descending order according to the
		 * size of the clusters
		 */
		class ClusterSizeQSComparator implements QSComparator{

				private Buckets buckets = null;
			
				public ClusterSizeQSComparator(Buckets buckets){
					
					this.buckets = buckets;
					
				}
			
				@Override
				public int compare(int a, int b) {
					return -Integer.compare(buckets.get(a).size(), buckets.get(b).size());
				}
							
		}
		Quicksort.sort(cluster_ids_by_size, new ClusterSizeQSComparator(buckets));
		
		// Export the clusters to file
		/**
		 * @author Jan Hoinka
		 * This comparator sorts in descending order according to the
		 * size of the clusters
		 */
		class AptamerSizeQSComparator implements QSComparator{

				private SelectionCycle sc = null;
				
				public AptamerSizeQSComparator(SelectionCycle sc){
					
					this.sc = sc;
					
				}

				@Override
				public int compare(int arg1, int arg2) {
					
					return -Integer.compare(sc.getAptamerCardinality(arg1), sc.getAptamerCardinality(arg2));
				
				}
							
		}
		
		//Iterate over all remaining clusters and write to file
		AptamerPool ap = Configuration.getExperiment().getAptamerPool();
		boolean include_primer_regions = Configuration.getParameters().getBoolean("Export.IncludePrimerRegions");
		
		for (int cluster_id : cluster_ids_by_size){
			
			// we need to sort the aptamers by size first
			int[] aptamer_ids = buckets.get(cluster_id).toArray();
			Quicksort.sort(aptamer_ids, new AptamerSizeQSComparator(sc));
			
			//Finally we can write the data to file
			writer.write(">>Cluster_" + cluster_id + "\t" + buckets.get(cluster_id).size() +"\n");
			
			for ( int aptamer_id : aptamer_ids){
				
				writer.write(">Aptamer_" + aptamer_id + "\n");
				
				String sequence;
				if (include_primer_regions){
					sequence = new String(ap.getAptamer(aptamer_id));
				}
				else{
					AptamerBounds ab = ap.getAptamerBounds(aptamer_id);
					sequence = new String(ap.getAptamer(aptamer_id), ab.startIndex, (ab.endIndex-ab.startIndex));
				}
				
				writer.write(String.format("%s %s\n", sequence, sc.getAptamerCardinality(aptamer_id)));
			}
			
			writer.write("\n");
		}
		
		writer.close();
		
	}
	
	
	/**
	 * Exports the clusters according to their size. Size is defined as 
	 * the sum of aptamer cardinalities over all cluster members.
	 * @param sc the selection cycles who's clusters to export
	 * @param cc the cluster container produces by AptaCluster
	 * @param p the path to which export the data to
	 */
	private void ClustersBySize(SelectionCycle sc, ClusterContainer cc, Path p){
			
			// Load a writer instance depending on the configuration
			ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
			writer.open(p);
			
			// Arrange the data for export
			// Specifically we need a hashmap with cluster ids as keys and lists of aptamer ids as values
			Buckets buckets = Buckets.withExpectedSize(sc.getSize());
			
			// We will also need to sort the keys by size of their values
			
			// Iterate over the aptamers in the selection cycle and extract cluster membership
			for ( Entry<Integer, Integer> item : sc.iterator()){
	
				int cluster_id = cc.getClusterId(item.getKey());
				
				// Only take into account items which have a cluster membership
				if (cluster_id == -1) {
					continue;
				}
				
				if ( !buckets.contains(cluster_id) ){
					
					buckets.justPut(cluster_id, IntLists.mutable.of(item.getKey()));
					
				}
				else {
					buckets.get(cluster_id).add(item.getKey());
				}
				
			}
			int total_number_of_clusters = buckets.size();
			
			// Now remove all clusters which do not contain the specified minimal number of items
			int minimal_cluster_size = Configuration.getParameters().getInt("Export.MinimalClusterSize");
	
			// For this, we need an auxiliary map recording the cluster sizes
			TIntIntHashMap cluster_sizes = new TIntIntHashMap(total_number_of_clusters);
			for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
				
				// Cmpute the cluster size and store it
				int sum = 0;
				MutableIntIterator it = bucket.getValue().intIterator();
				while (it.hasNext()){ 
					
					sum += sc.getAptamerCardinality(it.next());
					
				}	
				
				cluster_sizes.put(bucket.getKey().intValue(), sum);
				
			}
			
			
			MutableIntList to_be_deleted = IntLists.mutable.empty();
			for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
				
				if( cluster_sizes.get(bucket.getKey().intValue()) <= minimal_cluster_size ){
				
					to_be_deleted.add(bucket.getKey());
				
				}
				
			}
			MutableIntIterator it = to_be_deleted.intIterator();
			while (it.hasNext()){
	
				buckets.justRemove(it.next());
				
			}
			
			// We need to sort the clusters according to their sizes
			int[] cluster_ids_by_size = new int[buckets.size()];
			
			int counter = 0;
			for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
				
				cluster_ids_by_size[counter] = bucket.getKey();
				counter++;
				
			}
			
			/**
			 * @author Jan Hoinka
			 * This comparator sorts in descending order according to the
			 * size of the clusters
			 */
			class ClusterSizeQSComparator implements QSComparator{
	
					private TIntIntHashMap cluster_sizes = null;
				
					public ClusterSizeQSComparator(TIntIntHashMap cluster_sizes){
						
						this.cluster_sizes = cluster_sizes;
						
					}
				
					@Override
					public int compare(int a, int b) {
						return -Integer.compare(cluster_sizes.get(a), cluster_sizes.get(b));
					}
								
			}
			Quicksort.sort(cluster_ids_by_size, new ClusterSizeQSComparator(cluster_sizes));
			
			// Export the clusters to file
			/**
			 * @author Jan Hoinka
			 * This comparator sorts in descending order according to the
			 * size of the clusters
			 */
			class AptamerSizeQSComparator implements QSComparator{
	
					private SelectionCycle sc = null;
					
					public AptamerSizeQSComparator(SelectionCycle sc){
						
						this.sc = sc;
						
					}
	
					@Override
					public int compare(int arg1, int arg2) {
						
						return -Integer.compare(sc.getAptamerCardinality(arg1), sc.getAptamerCardinality(arg2));
					
					}
								
			}
			
			//Iterate over all remaining clusters and write to file
			AptamerPool ap = Configuration.getExperiment().getAptamerPool();
			boolean include_primer_regions = Configuration.getParameters().getBoolean("Export.IncludePrimerRegions");
			
			for (int cluster_id : cluster_ids_by_size){
				
				// we need to sort the aptamers by size first
				int[] aptamer_ids = buckets.get(cluster_id).toArray();
				Quicksort.sort(aptamer_ids, new AptamerSizeQSComparator(sc));
				
				//Finally we can write the data to file
				writer.write(">>Cluster_" + cluster_id + "\t" + cluster_sizes.get(cluster_id) + "\n");
				
				for ( int aptamer_id : aptamer_ids){
					
					writer.write(">Aptamer_" + aptamer_id + "\n");
					
					String sequence;
					if (include_primer_regions){
						sequence = new String(ap.getAptamer(aptamer_id));
					}
					else{
						AptamerBounds ab = ap.getAptamerBounds(aptamer_id);
						sequence = new String(ap.getAptamer(aptamer_id), ab.startIndex, (ab.endIndex-ab.startIndex));
					}
					
					writer.write(String.format("%s %s\n", sequence, sc.getAptamerCardinality(aptamer_id)));
				}
				
				writer.write("\n");
			}
			
			writer.close();
			
		}
	
	/**
	 * Export a table of all clusters sorted by cluster size of the highest selection 
	 * round including Cluster ID, Seed Sequence and ID, and Size, Diversity and CMP 
	 * for each round.
	 * 
	 * Note, this function assumes that cluster information is present, i.e. that <code>experiment.getClusterContainer() != null </code>
	 * 
	 * @param export_folder the folder into which <code>filename</code> will be stored in
	 * @param experiment 
	 * @param filename the name of the file that will be created to contain the final table
	 */
	public void ClusterTable(Experiment experiment, Path export_folder, String filename) {
		
		AptaLogger.log(Level.INFO, this.getClass(), "Initializing cluster data for export." );
		
		ClusterContainer clusters = experiment.getClusterContainer(); 
		
		// Set the total number of clusters
		int numberOfClusters = clusters.getNumberOfClusters();
		
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Total clusters to export: %s", numberOfClusters));
		
		// Create initial cluster ids
		int[] cluster_ids_argsort = new int[numberOfClusters];

		// We need a secondary array to store the counts
		int[] cluster_sizes = new int[numberOfClusters];
		
		// We also need to store diversity
		int[] cluster_diversities = new int[numberOfClusters];
		
		// And a reference to the seed
		int[] seed_ids = new int[numberOfClusters];
		int[] seed_sizes = new int[numberOfClusters];
		
		// Finally, we use a bitset to track which clusters seed we have already determined
		BitSet found_clusters = new BitSet(numberOfClusters);
		
		// Initialize
		for (int x=0; x<numberOfClusters; x++) {
			
			cluster_sizes[x] = 0;
			cluster_diversities[x] =   0;
			seed_ids[x] =      -1;
			seed_sizes[x] =    -1;
			cluster_ids_argsort[x] = x;
			
		}

		// find the seed for each cluster. the seed is defined as the aptamer with largest cardinality 
		// starting from the last selection cycle. If the last selection cycle does not contain this cluster
		// the previous cycle will be considered and so on.
		for( int i = experiment.getAllSelectionCycles().size()-1; i>=0; i-- ) {
			
			try {
				SelectionCycle reference_cycle = experiment.getAllSelectionCycles().get(i);
				
				// Fill cardinality array by size
				Iterator<Entry<Integer, Integer>> cluster_it = clusters.iterator().iterator();
				Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
				Entry<Integer, Integer> cluster_entry = cluster_it.next();
				Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
				
				while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases
	
					if (cluster_entry.getKey() < cardinality_entry.getKey()) {
					
						cluster_entry = cluster_it.next();
						
					}
					else if (cluster_entry.getKey() > cardinality_entry.getKey()){
						
						cardinality_entry = cardinality_it.next();
					}
					
					// Process...
					else {
									
						// ...but only if we do not already have a cluster seed
						if (!found_clusters.get(cluster_entry.getValue())) {
						
							//cardinalities[cluster_entry.getValue()] += cardinality_entry.getValue(); 
							//diversities[cluster_entry.getValue()] += 1;
							
							// is this our seed? 
							if ( cardinality_entry.getValue() >= seed_sizes[cluster_entry.getValue()] ) {
								
								seed_ids[cluster_entry.getValue()] = cardinality_entry.getKey();
								seed_sizes[cluster_entry.getValue()] = cardinality_entry.getValue();
						 
							}
						
						}
						
						cluster_entry = cluster_it.next();
					}
		
				}
			}
			catch (Exception e) {
				
				AptaLogger.log(Level.WARNING, this.getClass(), e);
				System.out.println(e.toString());
				e.printStackTrace();
				
			}
	
			// we now have to determine those entries for which we have found a cluster seed in this round 
			// to exclude them from being considered further
			for (int j=0; j<seed_ids.length; j++) {
				
				if (seed_ids[j] != -1)  found_clusters.set(j);
				
			}
			
		}

		
		// Compute the cluster diversities and sizes for the last selection round
		SelectionCycle reference_cycle = experiment.getAllSelectionCycles().get(experiment.getAllSelectionCycles().size()-1);
		try {
			
			// Fill cardinality array by size
			Iterator<Entry<Integer, Integer>> cluster_it = clusters.iterator().iterator();
			Iterator<Entry<Integer, Integer>> cardinality_it = reference_cycle.iterator().iterator();
			Entry<Integer, Integer> cluster_entry = cluster_it.next();
			Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
			
			while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases

				if (cluster_entry.getKey() < cardinality_entry.getKey()) {
				
					cluster_entry = cluster_it.next();
					
				}
				else if (cluster_entry.getKey() > cardinality_entry.getKey()){
					
					cardinality_entry = cardinality_it.next();
				}
				
				// Process...
				else {
					
					cluster_sizes[cluster_entry.getValue()] += cardinality_entry.getValue();
					cluster_diversities[cluster_entry.getValue()]++;
					
					cluster_entry = cluster_it.next();
				}
	
			}
		}
		catch (Exception e) {
			
			AptaLogger.log(Level.WARNING, this.getClass(), e);
			System.out.println(e.toString());
			e.printStackTrace();
			
		}
		
		// Sort the cluster id array according to the cardinalities
		AptaLogger.log(Level.INFO, this.getClass(), String.format("Sorting clusters by size in round %s" , experiment.getAllSelectionCycles().get(experiment.getAllSelectionCycles().size()-1)));
		Quicksort.sort(cluster_ids_argsort, cluster_sizes, Quicksort.DescendingQSComparator());
		
		// Keep track of the files we need to merge later
		List<File> table_slices = new ArrayList<File>(); 
				
		// Now write this portion to a temporary file before continuing with the remaining cycle
		File first_columns_file = null;
		try {
			first_columns_file = File.createTempFile("tableslice_", ".temp", export_folder.toFile());
			first_columns_file.deleteOnExit();
			
			table_slices.add(first_columns_file);
			
		} catch (IOException e) {
			e.printStackTrace();
			AptaLogger.log(Level.SEVERE, this.getClass(), "Could not create temporary file");
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			return;
		}
		
		try {
			
			AptaLogger.log(Level.INFO, this.getClass(), "Populating temporary file " + first_columns_file.toString());
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(first_columns_file.toString()));
			
			int card_idx = 0; 	//since we are using the cluster size array to sort the indices, we need to use 
								// a running index for this array
			
			Boolean withPrimers = Configuration.getParameters().getBoolean("Export.IncludePrimerRegions");
			
			double reference_cycle_size = reference_cycle.getSize();
			
			for (int index : cluster_ids_argsort) {
				
				// Create sequence with, or without primers, if no sequence is present in the ref cycle for this cluster, write N/A
				byte[] sequence = (seed_ids[index] == -1) ? "N/A".getBytes() : experiment.getAptamerPool().getAptamer(seed_ids[index]);
				
				AptamerBounds bounds = (seed_ids[index] == -1) ? new AptamerBounds(0,3) : withPrimers ? new AptamerBounds(0, sequence.length) : Configuration.getExperiment().getAptamerPool().getAptamerBounds(seed_ids[index]);
				
				bw.write(String.format("%s\t%s\t%s\t%.5f\t%s\t%.5f\n", 
						index, // cluster id 
						(withPrimers ? new String(sequence) : new String(Arrays.copyOfRange(sequence, bounds.startIndex, bounds.endIndex))), // Seed sequence
						seed_ids[index] == -1 ? "N/A" : seed_ids[index], // seed id
						cluster_sizes[card_idx] / reference_cycle_size, // Cluster size is percent of pool size
						cluster_diversities[index], // cluster diversity
						((double) cluster_sizes[card_idx] / reference_cycle_size) * 1000000.0 // cluster CPM
						));
				
				card_idx++;
				
			}
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			AptaLogger.log(Level.SEVERE, this.getClass(), "Could not write to temporary file");
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			return;
		}
		
		// free space
		seed_ids = null;
		seed_sizes = null;
		
		// we now have to compute the cluster size, diversity, and CPM for each cluster in the remaining selection cycles
		for( int i = experiment.getAllSelectionCycles().size()-1; i>=0; i--) {
			
			SelectionCycle cycle = experiment.getAllSelectionCycles().get(i);
			double cycle_size = cycle.getSize();
			
			if (cycle == reference_cycle) continue;
			
			AptaLogger.log(Level.INFO, this.getClass(), String.format("Sorting clusters by size in round %s" , cycle.getName()));
			
			// we are going to reuse the arrays created for the reference cycle, 
			// but we need to reinitialize them
			for (int x=0; x<numberOfClusters; x++) {
				
				cluster_sizes[x] = 0;
				cluster_diversities[x] =   0;
				
			} 
			
			// Fill the arrays
			Iterator<Entry<Integer, Integer>> cluster_it = clusters.iterator().iterator();
			Iterator<Entry<Integer, Integer>> cardinality_it = cycle.iterator().iterator();
			Entry<Integer, Integer> cluster_entry = cluster_it.next();
			Entry<Integer, Integer> cardinality_entry = cardinality_it.next();
			
			while ( cluster_it.hasNext() && cardinality_it.hasNext() ) { // Ids are sorted in both cases

				if (cluster_entry.getKey() < cardinality_entry.getKey()) {
					
					cluster_entry = cluster_it.next();
					
				}
				else if (cluster_entry.getKey() > cardinality_entry.getKey()){
					
					cardinality_entry = cardinality_it.next();
					
				}
				
				// Process
				else {

					cluster_sizes[cluster_entry.getValue()] += cardinality_entry.getValue(); 
					cluster_diversities[cluster_entry.getValue()] += 1;
					cluster_entry = cluster_it.next();
					
				}

			}
			
			
			// Now write this portion to a temporary file
			File next_columns_file = null;
			try {
				next_columns_file = File.createTempFile("tableslice_", ".temp", export_folder.toFile());
				next_columns_file.deleteOnExit();
				
				table_slices.add(next_columns_file);
				
			} catch (IOException e) {
				e.printStackTrace();
				AptaLogger.log(Level.SEVERE, this.getClass(), "Could not create temporary file");
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
				return;
			}

			try {
				
				AptaLogger.log(Level.INFO, this.getClass(), "Populating temporary file " + next_columns_file.toString());
				
				BufferedWriter bw = new BufferedWriter(new FileWriter(next_columns_file.toString()));
				
				for (int index : cluster_ids_argsort) {
					
					bw.write(String.format("%.5f\t%s\t%.5f\n", 
							cluster_sizes[index] / cycle_size, // Cluster size is percent of pool size
							cluster_diversities[index], // cluster diversity
							((double) cluster_sizes[index] / cycle_size) * 1000000.0 // cluster CPM
							));
					
				}
				
				bw.close();
										
			} catch (IOException e) {
				e.printStackTrace();
				AptaLogger.log(Level.SEVERE, this.getClass(), "Could not write to temporary file");
				AptaLogger.log(Level.SEVERE, this.getClass(), e);
				return;
			}
			
			
		}
		
		// Finally we merge the file into a new one, line by line
		List<BufferedReader> fileReaders = new ArrayList<BufferedReader>();
		try {
			
			// Open all files
			for ( File f : table_slices ) {
				
			    BufferedReader br = new BufferedReader(new FileReader(f));
			    fileReaders.add(br);
				
			}
			
			// Create the final file that will contain the table
			AptaLogger.log(Level.INFO, this.getClass(), "Merging table slices into " + Paths.get(export_folder.toString(), filename).toString());
			
			ExportWriter writer = Configuration.getParameters().getBoolean("Export.compress") ? new CompressedExportWriter() : new UncompressedExportWriter();
			writer.open(Paths.get(export_folder.toString(), filename));
			
			
			// Create header 
			StringBuilder header = new StringBuilder("Cluster ID\tSeed Sequence\tSeed ID\t");
			for( int i = experiment.getAllSelectionCycles().size()-1; i>=0; i--) {
				
				SelectionCycle cycle = experiment.getAllSelectionCycles().get(i);
				
				header.append(cycle.getName() + " Size\t");
				header.append(cycle.getName() + " Diversity\t");
				header.append(cycle.getName() + " CPM" + ((i!=0) ? "\t" : ""));
				
			}
			writer.write(header.toString()+"\n");
			
			// Join the table columns, line by line
			ArrayList<String> current_lines = new ArrayList<String>();
			StringBuilder concatenated_line = new StringBuilder();
			
			while ( 
					fileReaders.stream().map( t1 -> {
						String line = null;
						try {
							line = t1.readLine();
						} catch (IOException e) {
							e.printStackTrace();
						}
						current_lines.add(line);
						return line;
					} ).noneMatch(Objects::isNull) ) {
				
				// process the lines
				concatenated_line.append(current_lines.get(0).trim());
				current_lines.stream().skip(1).forEach( line -> {concatenated_line.append("\t"+line.trim());});
				
				// write to file
				writer.write(concatenated_line+"\n");
				
				// reset line container
				current_lines.clear();
				concatenated_line.setLength(0);
			}
			
			// Close handle to the table file
			writer.close();
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
			AptaLogger.log(Level.SEVERE, this.getClass(), "Could not open temporary file");
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			return;
			
		}
		
		// close all file handles and remove temporary files
		try {
			
			
			for (BufferedReader reader : fileReaders) {
				
				reader.close();
				
			}
		} catch (IOException e) {
			
			e.printStackTrace();
			AptaLogger.log(Level.SEVERE, this.getClass(), "Could not close one ore more file reader handles");
			AptaLogger.log(Level.SEVERE, this.getClass(), e);
			return;
			
		}
		
		// delete temporary files
		for (File f : table_slices) {
			
			f.delete();
			
		}
		
		
	}
	
}
