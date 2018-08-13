/**
 * 
 */
package lib.parser.aptasim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.logging.Level;

import lib.aptamer.datastructures.Metadata;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.Parser;
import lib.parser.ParserProgress;
import utilities.Accumulator;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * Java implementation of AptaSIM
 */
public class AptaSimParser implements Parser, Runnable{

	/**
	 * Track the progress of the simulation
	 */
	private AptaSimProgress progress = new AptaSimProgress();
	
	/**
	 * The degree of the Markov model 
	 */
	private int hmm_degree = Configuration.getParameters().getInt("Aptasim.HmmDegree");
	
	/**
	 * Fastq file containing training sequences
	 */
	private String filename = Configuration.getParameters().getString("Aptasim.HmmFile");
	
	/**
	 * The 5 prime primer in the Experiment
	 */
	private String primer5 = Configuration.getParameters().getString("Experiment.primer5");
	
	/**
	 * The 3 prime primer in the Experiment
	 */
	private String primer3 = Configuration.getParameters().getString("Experiment.primer3");
	
	/**
	 * Number of sequences in the initial pool
	 */
	private int number_of_sequences = Configuration.getParameters().getInt("Aptasim.NumberOfSequences");
	
	/**
	 * Number of high affinity sequences in the initial pool
	 */
	private int number_of_seeds = Configuration.getParameters().getInt("Aptasim.NumberOfSeeds");
	
	/**
	 * Length of the randomized region in the aptamers
	 */
	private int randomized_region_size = Configuration.getParameters().getInt("Aptasim.RandomizedRegionSize"); 
	
	/**
	 * Maximal count of remaining sequences
	 */
	private int max_sequence_count = Configuration.getParameters().getInt("Aptasim.MaxSequenceCount");
	
	/**
	 * The minimal affinity for seed sequences (INT range: 0-100)
	 */
	private int min_seed_affinity = Configuration.getParameters().getInt("Aptasim.MinSeedAffinity");
	
	/**
	 * The maximal sequence affinity for non-seeds (INT range: 0-100)
	 */
	private int max_sequence_affinity = Configuration.getParameters().getInt("Aptasim.MaxSequenceAffinity");
	
	/**
	 * Random generator for the simulation
	 */
	private Random rand = new Random();
	
	/**
	 * If no training data is specified, create pool based on this distribution
	 */
	private Map<Character, Double> nucleotide_distribution = new HashMap<Character,Double>(); 
	
	/**
	 * The percentage of sequences that remain after selection (DOUBLE range: 0-1)
	 */
	private double selection_percentage = Configuration.getParameters().getDouble("Aptasim.SelectionPercentage");
	
	/**
	 * PCR amplification efficiency (DOUBLE range: 0-1) 
	 */
	private double amplification_efficiency = Configuration.getParameters().getDouble("Aptasim.AmplificationEfficiency");
	
	/**
	 * Mutation probability during PCR (DOUBLE range: 0-1)
	 */
	private double mutation_probability = Configuration.getParameters().getDouble("Aptasim.MutationProbability");
	
	/**
	 * Mutation rates for individual nucleotides (order A,C,G,T)
	 */
	private byte[] base_mutation_rates = null; 
	
	/**
	 * Temporary storage for the affinities required during the selection stage of AptaSim 
	 */
	private HashMap<Integer, Integer> affinities = new HashMap<Integer, Integer>();
	
	/**
	 * Store a list of all selection cycles not specified in the configuration
	 * but required for the simulation. These cycles will be removed from the experiment
	 * at the end of the AptaSim run.
	 */
	private ArrayList<SelectionCycle> temporary_cycles = new ArrayList<SelectionCycle>();
	
	/**
	 * Metadata such as nucleotide counts
	 */
	private Metadata metadata = Configuration.getExperiment().getMetadata();
	
	/**
	 * Sum over all processed reads 
	 */
	private Integer grand_total_processed_reads = 0;
	
	public AptaSimParser(){

		// Set the nucleotide distribution
		Double[] dist = (Double[]) Configuration.getParameters().getArray(Double.class, "Aptasim.NucleotideDistribution");
		nucleotide_distribution.put('A', dist[0]);
		nucleotide_distribution.put('C', dist[1]);
		nucleotide_distribution.put('G', dist[2]);
		nucleotide_distribution.put('T', dist[3]);
		
		// Set the base mutation rates
		Double[] rates = (Double[]) Configuration.getParameters().getArray(Double.class, "Aptasim.BaseMutationRates");
		// Compute the size of the array
		int size = 0;
		for (Double c : rates){ size+= new Double(c*100.0).intValue(); }
		base_mutation_rates = new byte[size];
		
		size = 0;
		// A
		for (int x=0; x<new Double(rates[0]*100.0).intValue(); x++){
			base_mutation_rates[size] = 'A';
			size++;
		}
		
		// C
		for (int x=0; x<new Double(rates[1]*100.0).intValue(); x++){
			base_mutation_rates[size] = 'C';
			size++;
		}
		
		// G
		for (int x=0; x<new Double(rates[2]*100.0).intValue(); x++){
			base_mutation_rates[size] = 'G';
			size++;
		}
		
		// T
		for (int x=0; x<new Double(rates[3]*100.0).intValue(); x++){
			base_mutation_rates[size] = 'T';
			size++;
		}
		
	}
	
	@Override
	public void parse() {
		
		// Prepare the experiment
		createTemporarySelectionCycles();
		
		// Generate the first pool
		if (filename != null){
			progress.trainingStage(filename);
			HMMSequenceGenerator hmm = trainModel();
			progress.reset();
		
			progress.initialPoolStage(0);
			generatePoolWithModel(hmm);

		}
		else{
			progress.initialPoolStage(0);
			generatePoolWithoutModel();
		}

		grand_total_processed_reads += progress.totalProcessedReads.get();
		progress.reset();
	
		// Perform selection and amplification
		ArrayList<SelectionCycle> cycles = Configuration.getExperiment().getSelectionCycles();
		for (int x=1; x<cycles.size(); x++){
			
			//do selection
			progress.selectionStage(x);
			selectBinders(cycles.get(x-1), cycles.get(x));
			grand_total_processed_reads += progress.totalProcessedReads.get();
			progress.reset();
			
			
			//amplify
			progress.amplificationStage(x);
			amplifyPool(cycles.get(x));
			grand_total_processed_reads += progress.totalProcessedReads.get();
			progress.reset();
			
		}
		
		
		// Clean up
		removeTemporarySelectionCycles();
	}

	@Override
	public void parsingCompleted() {
		
		// now that we have the data set any file backed implementations of the
		// pools and cycles to read only
		Configuration.getExperiment().getAptamerPool().setReadOnly();
		for (SelectionCycle cycle : Configuration.getExperiment().getAllSelectionCycles()) {
			if (cycle != null) {
				cycle.setReadOnly();
			}
		}
		
		// Store the final progress data to the metadata statistics
		Metadata metadata = Configuration.getExperiment().getMetadata();
		
		metadata.parserStatistics.put("processed_reads", this.grand_total_processed_reads);
		int total_accepted = 0;
		for (SelectionCycle sc : Configuration.getExperiment().getAllSelectionCycles()) {total_accepted += sc.getSize();}
		metadata.parserStatistics.put("accepted_reads", total_accepted);
		metadata.parserStatistics.put("contig_assembly_fails", 0);
		metadata.parserStatistics.put("invalid_alphabet", 0);
		metadata.parserStatistics.put("5_prime_error", 0);
		metadata.parserStatistics.put("3_prime_error", 0);
		metadata.parserStatistics.put("invalid_cycle", 0);
		metadata.parserStatistics.put("total_primer_overlaps", 0);
		
		// Finally, store the metadata to disk
		metadata.saveDataToFile();
		
		AptaLogger.log(Level.INFO, this.getClass(), "Parsing Completed, Data storage set to read-only and metadata written to file");
		
		// Save metadata to disk
		metadata.saveDataToFile();
		
	}

	@Override
	public ParserProgress Progress() {
		return progress;
	}

	@Override
	public void run() {
		
		parse();
		parsingCompleted();
		
	}

	/**
	 * Train the Markov Model with the sequencing data as specified in the configuration
	 */
	private HMMSequenceGenerator trainModel(){
		
		long tStart = System.currentTimeMillis();
		
		// Read sequences from file and train the model
		AptaLogger.log(Level.CONFIG, this.getClass(), "Training Markov Model with data from " + filename);
		HMMSequenceGenerator hmm = new HMMSequenceGenerator(hmm_degree, primer5, primer3);
		try(BufferedReader br = new BufferedReader(new FileReader(new File(filename)))) 
		{
			int i = 0;
		    for(String line; (line = br.readLine()) != null; i++) 
		    {
		    	if (i%4 == 1)
		    	{
		    		hmm.trainModel(line);
		    		progress.totalProcessedReads.getAndIncrement();
		    	}
		    }
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		AptaLogger.log(Level.CONFIG, this.getClass(), String.format("Training Completed in %s seconds.\n",
				((System.currentTimeMillis() - tStart) / 1000.0)));

		return hmm;
	}
	
	/**
	 * Creates and initial pool based on the training data 
	 * @param filename
	 * @return
	 */
	private void generatePoolWithModel(HMMSequenceGenerator hmm)	{
		
		long tStart = System.currentTimeMillis();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Creating inital pool");
		
		SelectionCycle cycle = Configuration.getExperiment().getSelectionCycles().get(0);
		
		//add small number of seeds
		int total = 0;
		for (int x=0; x<number_of_seeds; x++)
		{
			byte[] n = hmm.generateSequence(randomized_region_size, true);
			int c = rand.nextInt(max_sequence_count); 
			int a = min_seed_affinity + rand.nextInt(21);
			
			// Add aptamer to pool and update affinity 
			int a_id = cycle.addToSelectionCycle(n, primer5.length(), randomized_region_size+primer5.length(),  c);
			affinities.put(a_id, a);
			
			// And Metadata
			this.addAcceptedNucleotideDistributions(cycle, n, primer5.length(), randomized_region_size+primer5.length());
			this.addNuceotideDistributions(n, cycle);
			
			total += c;
			progress.totalProcessedReads.getAndAdd(c);
			progress.totalPoolSize.getAndIncrement();
		}
		
		//add remaining sequences
		for (int x=number_of_seeds; x<number_of_sequences; x++)
		{
			byte[] n = hmm.generateSequence(randomized_region_size, true);
			int c = rand.nextInt(max_sequence_count);
			int a = rand.nextInt(max_sequence_affinity);
			
			// Add aptamer to pool and update affinity 
			int a_id = cycle.addToSelectionCycle(n, primer5.length(), randomized_region_size+primer5.length(),  c);
			affinities.put(a_id, a);
			
			// And Metadata
			this.addAcceptedNucleotideDistributions(cycle, n, primer5.length(), randomized_region_size+primer5.length());
			this.addNuceotideDistributions(n, cycle);
			
			total += c;
			progress.totalProcessedReads.getAndAdd(c);
			progress.totalPoolSize.getAndIncrement();
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), String.format("Sequence generation completed in %s seconds. Pool size: %s",
				((System.currentTimeMillis() - tStart) / 1000.0), total ));
		
	}
	
	/**
	 * Creates a pool based on the specified nucleotide distribution
	 */
	private void generatePoolWithoutModel() {
		
		long tStart = System.currentTimeMillis();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Creating inital pool");
		
		SelectionCycle cycle = Configuration.getExperiment().getSelectionCycles().get(0);
		
		//add small number of seeds
		int total = 0;
		for (int x=0; x<number_of_seeds; x++)
		{
			byte[] n = generateSequence(randomized_region_size, true);
			int c = Math.max(1, rand.nextInt(max_sequence_count)); 
			int a = min_seed_affinity + rand.nextInt(21);
			
			// Add aptamer to pool and update affinity 
			int a_id = cycle.addToSelectionCycle(n, primer5.length(), randomized_region_size+primer5.length(),  c);
			affinities.put(a_id, a);
			
			// And Metadata
			this.addAcceptedNucleotideDistributions(cycle, n, primer5.length(), randomized_region_size+primer5.length());
			this.addNuceotideDistributions(n, cycle);
			
			total += c;
			progress.totalProcessedReads.getAndAdd(c);	
			progress.totalPoolSize.getAndIncrement();
		}
		
		//add remaining sequences
		for (int x=number_of_seeds; x<number_of_sequences; x++)
		{
			byte[] n = generateSequence(randomized_region_size, true);
			int c = Math.max(1, rand.nextInt(max_sequence_count));
			int a = Math.max(1, rand.nextInt(max_sequence_affinity));
			
			// Add aptamer to pool and update affinity 
			int a_id = cycle.addToSelectionCycle(n, primer5.length(), randomized_region_size+primer5.length(),  c);
			affinities.put(a_id, a);
			
			// And Metadata
			this.addAcceptedNucleotideDistributions(cycle, n, primer5.length(), randomized_region_size+primer5.length());
			this.addNuceotideDistributions(n, cycle);
			
			total += c;
			progress.totalProcessedReads.getAndAdd(c);	
			progress.totalPoolSize.getAndIncrement();
			
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), String.format("Sequence generation completed in %s seconds. Pool size: %s",
				((System.currentTimeMillis() - tStart) / 1000.0), total ));
		
	}
	
	
	/**
	 * Simulates selection
	 * @param sequences
	 * @return
	 */
	private void selectBinders(SelectionCycle current, SelectionCycle next)
	{
		long tStart = System.currentTimeMillis();
		
		AptaLogger.log(Level.CONFIG, this.getClass(), "Selecting binders for cycle " + next.getName());
		
		int sequences_total = current.getSize();
		
		// Temporary data structure for fast weighted sampling
		// Put all aptamer ids according to their count into sampler
		BitSet bit = new BitSet(sequences_total);
		int[] sampler = new int[sequences_total];
		int counter = 0;
		
		for (Entry<Integer, Integer> entry : current.iterator())
		{
			for (int x = 0; x<entry.getValue(); x++)
			{
				sampler[counter] = entry.getKey();
				counter++;
			}
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), String.format("Created temporary array for fast weighted sampling. Number of items: %s", sampler.length ));
		
		int number_of_sequences_to_sample = new Double (number_of_sequences * selection_percentage).intValue();
		int sample_total = 0;
		
		while (sample_total < number_of_sequences_to_sample)
		{
			
//			generate random number between 0 and sequences_total
			int pick = rand.nextInt(sequences_total);
			
			//pick sequence 
			int a_id = sampler[pick];
			while (bit.get(pick))
			{
				pick = rand.nextInt(sequences_total);
				a_id = sampler[pick];
			}
			
			
			//accept sequence based on affinity
			if (rand.nextInt(101) <= affinities.get(a_id))
			{
				//set sequence as chosen
				bit.flip(pick);
				
				//add or update sample
				byte[] aptamer = Configuration.getExperiment().getAptamerPool().getAptamer(a_id);
				
				next.addToSelectionCycle(aptamer, primer5.length(), randomized_region_size+primer5.length() );
				progress.totalSampledReads.getAndIncrement();
				
				// And Metadata
				this.addAcceptedNucleotideDistributions(next, aptamer, primer5.length(), randomized_region_size+primer5.length());
				this.addNuceotideDistributions(aptamer, next);
				
				sample_total++;
			}
			else{
				progress.totalDiscardedReads.getAndIncrement();
			}
			progress.totalProcessedReads.getAndIncrement();
			
		}
	
		AptaLogger.log(Level.CONFIG, this.getClass(), 
				String.format("Binder selection completed in %s seconds. Total Processed: %s  Accepted: %s  Discarded: %s",
				((System.currentTimeMillis() - tStart) / 1000.0), 
				progress.totalProcessedReads.get(), 
				progress.totalSampledReads.get(),
				progress.totalDiscardedReads.get()));
	}	
	
	
	/**
	 * Simulates error prone PCR
	 * @param sequences
	 * @return
	 */
	public void amplifyPool(SelectionCycle cycle)
	{
		long tStart = System.currentTimeMillis();
	
		AptaLogger.log(Level.CONFIG, this.getClass(), "Amplifying binders for cycle " + cycle.getName());
		
		//compute number of pcr_cycles
		int pcr_cycles = (int) Math.ceil(Math.log( ((double)number_of_sequences)/((double)cycle.getSize())) / Math.log(1.0+amplification_efficiency) );
		
		for (int x=0; x<pcr_cycles; x++)
		{
			//iterate over every sequence and amplify
			for (Entry<byte[], Integer> entry : cycle.sequence_iterator())
			{
				int nr_molecules = entry.getValue();
				
				//we need to amplify each molecule
				for (int y=0; y<nr_molecules; y++)
				{
					//only amplify if according to the amplification_efficiency
					double amp = rand.nextDouble();
					
					if (amp <= amplification_efficiency)
					{
					
						//do we have to mutate?
						double mut = rand.nextDouble();
						if(mut <= mutation_probability)
						{
							//Create mutant
							int pos = rand.nextInt(primer5.length()+randomized_region_size);
							byte[] mutant = entry.getKey().clone();
							mutant[pos] = base_mutation_rates[rand.nextInt(base_mutation_rates.length)];
													
							//update
							int m_id = cycle.addToSelectionCycle(mutant, primer5.length(), randomized_region_size+primer5.length());
							progress.totalMutatedReads.getAndIncrement();
							
							this.addAcceptedNucleotideDistributions(cycle, mutant, primer5.length(), randomized_region_size+primer5.length());
							this.addNuceotideDistributions(mutant, cycle);
							
							
							// The mutant is set to the affinity of its parent sequence
							int a_id = Configuration.getExperiment().getAptamerPool().getIdentifier(entry.getKey());
							affinities.put(m_id, affinities.get(a_id));
						}
						else
						{
							cycle.addToSelectionCycle(entry.getKey(), primer5.length(), randomized_region_size+primer5.length());
							progress.totalSampledReads.getAndIncrement();
							
							this.addAcceptedNucleotideDistributions(cycle, entry.getKey(), primer5.length(), randomized_region_size+primer5.length());
							this.addNuceotideDistributions(entry.getKey(), cycle);
							
						}
					}
					else{
						progress.totalDiscardedReads.getAndIncrement();
					}
					progress.totalProcessedReads.getAndIncrement();
					progress.totalPoolSize.set(cycle.getSize());
				}
			}
		}
		
		AptaLogger.log(Level.CONFIG, this.getClass(), 
				String.format("Amplification completed in %s seconds. %s  %s  %s  %s  %s",
				((System.currentTimeMillis() - tStart) / 1000.0), 
				("Processed: " + progress.totalProcessedReads.get()), 
				("Selected: " + progress.totalSampledReads.get()), 
				("Discarded: " + progress.totalDiscardedReads.get()), 
				("Mutated: " + progress.totalMutatedReads.get()), 
				("Pool Size: " + progress.totalPoolSize.get())));
		
	}	
	
	
	/**
	 * Creates a sequence based on the specified distribution 
	 * @param size
	 * @return
	 */
	private byte[] generateSequence(int size, boolean withPrimers)
	{
		StringBuilder sb = new StringBuilder(size);
		
		sb.append(withPrimers ? primer5 : "");
		
		for(int i=0; i<size; i++)
		{
			double p = rand.nextDouble();
			double cumsum = 0.0;
			for ( Entry<Character, Double> item : nucleotide_distribution.entrySet())
			{
				cumsum += item.getValue();
				if (cumsum >= p)
				{
					sb.append(item.getKey());
					break;
				}
			}
		}
		
		sb.append(withPrimers ? primer3 : "");
		
		return sb.toString().getBytes();		
	}
	
	/**
	 * Create instances of those selection cycles between round 0
	 * and the largest cycle as specified in the configuration file
	 * which the user does not which to retain but are required as 
	 * intermediate steps for the simulation. These cycles will 
	 * deleted from the experiment upon completion of the simulation.
	 */
	private void createTemporarySelectionCycles(){
		
		// Get a list of all selection cycles
		ArrayList<SelectionCycle> cycles = Configuration.getExperiment().getSelectionCycles();
		AptaLogger.log(Level.CONFIG, this.getClass(), "Found a total of " + cycles.size() + " selection cycles in the configuration");
		
		// Create a temporary cycle if not specified in the configuration
		for (int x=0; x<cycles.size(); x++){

			SelectionCycle cycle = Configuration.getExperiment().getSelectionCycles().get(x);
			
			// If this cycle was not specified by the user, we create a temporary cycle and
			// flag it for removal.
			if (cycle == null){
				cycle = Configuration.getExperiment().registerSelectionCycle("Temp"+x, x, false, false, null, null, true);			
				temporary_cycles.add(cycle);
				AptaLogger.log(Level.CONFIG, this.getClass(), "Created temporary cycle for round " + x);
			}
		}
		
	}
	
	/**
	 * Deletes the temporary instances of SelectionCycle required for the simulation
	 * from the experiment
	 */
	private void removeTemporarySelectionCycles(){
		
		for (SelectionCycle c : this.temporary_cycles){
			Configuration.getExperiment().unregisterSelectionCycle(c);
		}
		
		temporary_cycles.clear();
		
	}
	
	
	/**
	 * Iterates over the forward and reverse read (if present) and adds 
	 * the nucleotide counts to the meta data
	 * @param c
	 */
	void addNuceotideDistributions(byte[] read, SelectionCycle sc) {
		
		ConcurrentHashMap<Integer,ConcurrentHashMap<Byte,Integer>> forward = metadata.nucleotideDistributionForward.get(sc.getName());
		
		// Iterate over the read add add quality scores to the accumulators
		for (int i= 0; i < read.length; i++) {
			
			// Make sure the entry exists prior to adding
			if(!forward.contains(i)) {
				ConcurrentHashMap<Byte,Integer> map = new ConcurrentHashMap<Byte,Integer>();
				map.put((byte) 'A', 0); 
				map.put((byte) 'C', 0);
				map.put((byte) 'G', 0);
				map.put((byte) 'T', 0);
				map.put((byte) 'N', 0);
				forward.put(i, map);
			}
			
			// Add nucleotides
			forward.get(i).put(read[i], forward.get(i).get(read[i])+1 );
			
		}
	}
	
	/**
	 * Adds the nucleotide distribution of the randomized region to the meta data, categorized 
	 * by the length of the region
	 * @param sc
	 * @param contig
	 * @param randomized_region_start_index
	 * @param randomized_region_end_index
	 */
	void addAcceptedNucleotideDistributions(SelectionCycle sc, byte[] contig, int randomized_region_start_index, int randomized_region_end_index) {
		
		int randomized_region_size = randomized_region_end_index - randomized_region_start_index;

		// Make sure we have seen this randomized region size before, else create placeholder
		if (!metadata.nucleotideDistributionAccepted.get(sc.getName()).contains(randomized_region_size)) {
					
			metadata.nucleotideDistributionAccepted.get(sc.getName()).put(randomized_region_size, new ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>());
			
		}
		
		ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>> accepted = metadata.nucleotideDistributionAccepted.get(sc.getName()).get(randomized_region_size);
		
		int i = 0;
		for (int x=randomized_region_start_index; x<randomized_region_end_index; x++) {
			
			// Make sure the entry exists prior to adding
			if (!accepted.contains(i)) {
				
				ConcurrentHashMap<Byte,Integer> map = new ConcurrentHashMap<Byte,Integer>(5);
				map.put((byte) 'A', 0); 
				map.put((byte) 'C', 0);
				map.put((byte) 'G', 0);
				map.put((byte) 'T', 0);
				map.put((byte) 'N', 0);
				accepted.put(i, map);
				
			}
			
			// Add nucleotides
			accepted.get(i).put(contig[x], accepted.get(i).get(contig[x])+1);
			
			i++;
		}
		
	}
	
	
	
}
