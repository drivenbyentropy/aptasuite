/**
 * 
 */
package lib.export;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.factory.primitive.IntLists;

import exceptions.InvalidConfigurationException;
import lib.aptacluster.Buckets;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.ClusterContainer;
import lib.aptamer.datastructures.SelectionCycle;
import lib.aptamer.datastructures.StructurePool;
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
		System.out.println(total_number_of_clusters + "   " + buckets.size());
		
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
			writer.write(">>Cluster_" + cluster_id + "\n");
			
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
	
}
