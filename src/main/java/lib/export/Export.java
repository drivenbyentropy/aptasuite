/**
 * 
 */
package lib.export;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

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

		// Do the same for the formatter
		ExportFormat<byte[]> formatter = null;
		switch (Configuration.getParameters().getString("Export.SequenceFormat")) {
		
			case "fastq": 
				formatter = new FastqExportFormat(Configuration.getExperiment().getName());
				break;
				
			case "fasta":
				formatter = new FastaExportFormat(Configuration.getExperiment().getName());
				break;
		
			default:
				AptaLogger.log(Level.SEVERE, this.getClass(), "Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized. Exiting");
				throw new InvalidConfigurationException("Export format " + Configuration.getParameters().getString("Export.SequenceFormat") + " not recognized.");
		}
		
		// Write sequences
		for ( Entry<byte[], Integer> entry : ap.iterator()){
			writer.write(formatter.format(entry.getValue(), entry.getKey()));
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
			writer.write(formatter.format(entry.getValue(), entry.getKey()));
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
		
		for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
			
			if( bucket.getValue().size() < minimal_cluster_size ){
			
				System.out.println("here");
				buckets.justRemove(bucket.getKey());
			
			}
			
		}
		System.out.println(total_number_of_clusters + "   " + buckets.size());
		
		// We need to sort the clusters according to their sizes
		int[] cluster_ids_by_size = new int[buckets.size()];
		
		int counter = 0;
		for ( Entry<Integer, MutableIntList> bucket : buckets.entrySet() ){
			
			cluster_ids_by_size[counter] = bucket.getKey();
			
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
		
	}
	
}
