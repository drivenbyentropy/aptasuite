package lib.aptatrace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author hoinkaj
 * Implements an External Sort routine for Aptamers with counts. Given the location of a file
 * in which each line contains the aptamer sequence and the count separated by a tab, it partitions the 
 * file into smaller chunks, sorts these individually in place, and performs and N-way merge.
 */
public class ExternalSort 
{
	private Path root_folder; 
	private Path input_file; //path to the original data to be sorted
	private Path output_file; //path to the file into which the sorted list will be written
	private Path temp_dir; //will contain the temporary files created during sorting.
	private int max_items_per_file; //the number of aptamers each partial file can contain
	private int last_round_count; //the number of aptamers in the last selection round
	
	private ArrayList<Path> temp_files;
	
	/**
	 * Creates an instance of External Sort.
	 * Note this class is NOT thread save. You can call <code>sort</code> multiple times using the same instance, but not in parallel.
	 * For parallel execution, create on instance per thread.
	 */
	public ExternalSort(int last_round_count){
		this.last_round_count=last_round_count;
	}
	
	/**
	 * Starts the External Sort procedure
	 * @param input_file Path to the original data to be sorted. The file is expected to contain the aptamer and count, separated by tab, in each line
	 * @param output_file path to the file into which the sorted list will be written. Can be identical to <code>input_file</code>.
	 * @param root_folder Used to store any temporary files created during sorting. This folder will be created if it does not exist. A sub-folder called will be created in <code>temp_folder</code> which will be removed after sorting is complete.
	 * @param max_items_per_file defines the maximal number of items each temporary file will contain.
	 */
	public void sort(String input_file, String output_file, String root_folder, int max_items_per_file)
	{
		this.input_file = Paths.get(input_file);
		this.output_file = Paths.get(output_file);
		this.root_folder = Paths.get(root_folder);
		this.temp_files = new ArrayList<Path>();
		this.max_items_per_file = max_items_per_file;
		
		try 
		{
			// make sure the temp folder exists
			if (!Files.exists(this.root_folder))
			{
				Files.createDirectory(this.root_folder);
			}
			// create a temp folder inside into which the partial files will be written 
			this.temp_dir = Files.createTempDirectory(this.root_folder, "externalsort_");
			// let java take care of deleting this folder once the virtual machine terminates
			this.temp_dir.toFile().deleteOnExit();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		//execute sorting
		SortAndPartition();
		NwayMerge();
	}
	
	/**
	 * Reads <code>max_items_per_file</code> from the input data, sorts it, and stores the sorted version in a temporary file, until all input data has been processed.
	 */
	private void SortAndPartition()
	{
		// temps
		BufferedReader br= null; //input file
		ArrayList<Object[]> elements = new ArrayList<Object[]>(max_items_per_file);
		QuickSort qs = new QuickSort();
		
		try 
		{
			br = new BufferedReader(new FileReader(input_file.toFile()));
		
			// read data in max_items_per_file chunks
			int counter = 0;
			String[] splitted_line;
			
			for (String line = br.readLine(); line != null; line = br.readLine()) 
			{
				splitted_line = line.split("\\s+");
				Object[] l = {splitted_line[0], Integer.parseInt(splitted_line[1])};
				elements.add(l);
				counter += 1;
				
				// once we have reached the total number of items for one file, we need to sort the data and store it
				if (counter == max_items_per_file)
				{
					//sort data
					qs.sort(elements);
					
					//save data
					Path file = Files.createTempFile(temp_dir, "externalsort_", ".txt");
					file.toFile().deleteOnExit(); //remove file automatically
					
					PrintWriter pw = new PrintWriter(new FileWriter(file.toFile()));
					
					for (int j = elements.size() - 1; j >= 0; j--){
						pw.printf("%s\t%d\t%d\t%.2f%%\n", (String) elements.get(j)[0], (int) elements.get(j)[1],(int)(((int)elements.get(j)[1])*1000000.0/(last_round_count*1.0)),(((int)elements.get(j)[1])*100.0)/(last_round_count*1.0) );
					}
					
					pw.close();
					
					//add the file to the list for N-way merging
					temp_files.add(file);
					
					//clear elements
					elements.clear();
					
					//reset counter
					counter = 0;
				}
			}
			
			//take care of the remaining elements, if any
			if (!elements.isEmpty())
			{
				//sort data
				qs.sort(elements);
				
				//save data
				Path file = Files.createTempFile(temp_dir, "externalsort_", ".txt");
				file.toFile().deleteOnExit(); //remove file automatically
				
				PrintWriter pw = new PrintWriter(new FileWriter(file.toFile()));
				
				for (int j = elements.size() - 1; j >= 0; j--)
				{
					//pw.printf("%s\t%d\n", (String) elements.get(j)[0], (int) elements.get(j)[1]);
					pw.printf("%s\t%d\t%d\t%.2f%%\n", (String) elements.get(j)[0], (int) elements.get(j)[1],(int)(((int)elements.get(j)[1])*1000000.0/(last_round_count*1.0)),(((int)elements.get(j)[1])*100.0)/(last_round_count*1.0) );
				}
				
				pw.close();
				
				//add the file to the list for N-way merging
				temp_files.add(file);
				
				//clear elements
				elements.clear();
			}
		
			br.close();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}	
	}
	
	public class PairComparator implements Comparator<Pair>
	{
	    @Override
	    public int compare(Pair x, Pair y)
	    {
	    	return -1*Integer.compare(Integer.valueOf(x.getAptamer().split("\t")[1]), Integer.valueOf(y.getAptamer().split("\t")[1]));
	    }
	}
	
	
	/**
	 * @author hoinkaj
	 * Structure to be used in a priority queue for fast retrival of the max element during N-way merging.  
	 * The index corresponds to the position of the BufferedReader in <code>temp_files</code>.
	 */
	public class Pair
	{
	    private Integer index;
	    private String aptamer;

	    public Pair(Integer index, String aptamer) 
	    {
	    	super();
	    	this.index = index;
	    	this.aptamer = aptamer;
	    }

	    public Integer getIndex() 
	    {
	    	return index;
	    }

	    public void setIndex(Integer index) 
	    {
	    	this.index = index;
	    }

	    public String getAptamer() 
	    {
	    	return aptamer;
	    }

	    public void setAptamer(String aptamer) 
	    {
	    	this.aptamer = aptamer;
	    }
	}
	
	/**
	 * Performs an N-way Merging of the data contained in all files specified in <code>temp_files</code>
	 * and stores the resulting sorted items file in <code>output_file</code>
	 */
	private void NwayMerge()
	{
		try 
		{
			// open as many readers as required, one for each chunk
			ArrayList<BufferedReader> readers = new ArrayList<BufferedReader>(temp_files.size());
			for (Path p : temp_files)
			{
				readers.add(new BufferedReader(new FileReader(p.toFile())));
			}
			
			// create output file
			Path result_file = Files.createTempFile(temp_dir, "result", ".txt");
			PrintWriter pw = new PrintWriter(new FileWriter(result_file.toFile()));
			
			// read data and merge
			PriorityQueue<Pair> queue = new PriorityQueue<Pair>(readers.size(), new PairComparator());
	
			// initialize with the first element from each file, which are guaranteed to exist
			int counter = 0;
			for (BufferedReader reader : readers)
			{
				queue.add(new Pair(counter,reader.readLine()));
				counter+=1;
			}

			while(!queue.isEmpty()) //queue will contain at least one element as long as not all files have processed
			{
				// get the largest element
				Pair max = queue.remove();
				
				// write the largest item into the output file
				pw.println(max.getAptamer());
				
				// read the next item
				max.setAptamer(readers.get(max.getIndex()).readLine());
				if (max.getAptamer() != null)
				{
					queue.add(max);
				}

			}
			
			// close all reader and writers
			pw.close();
			for (BufferedReader reader : readers)
			{
				reader.close();
			}
			
			//finally, move the result file to the specified location, overwrite if another file exists
			Files.deleteIfExists(output_file);
			Files.move(result_file, output_file);

		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
