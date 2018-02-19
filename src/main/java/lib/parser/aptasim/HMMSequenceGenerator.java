package lib.parser.aptasim;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

public class HMMSequenceGenerator 
{
	private HMMData probabilities = new HMMData(null, null);
	private Random rand = new Random();
	private Map<String, Double> dist = new HashMap<String, Double>();
	private static final DecimalFormat df = new DecimalFormat("0.00000000");
	DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance();
	
	private String p5 = null;
	private String p3 = null;
	

	/**
	 * Degree of the Markov Chain
	 */
	private Integer degree;
	
	/**
	 * Contains the total number of datapoint for each degree for normalization
	 */
	private Integer[] counts;
	
	public HMMSequenceGenerator(int degree, String p5, String p3)
	{
		// We need to set the decimal separator, otherwise Parsing to double will fail in countries using comma
		sym.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(sym);
		
		
		this.p3 = p3;
		this.p5 = p5;
		
		this.degree = degree;
		
		counts = new Integer[degree];
		for (int x=0; x<counts.length; x++)
		{
			counts[x] = 0;
		}
	}

	public void trainModel(Iterable<String> sequences)
	{
		for (String s : sequences)
		{
			trainModel(s);
		}
	}
	
	public void trainModel(String sequence)
	{
		//iterate sequence in window of size <code>1<=degree</code>
		String subseq;
		Double temp;
		for (int deg=1; deg<=degree; deg++)
		{
			for (int x=0; x<sequence.length()-deg+1; x++)
			{
				subseq = sequence.substring(x, x+deg);
				
				//add frequencies and totals
				temp = probabilities.containsKey(subseq) ? probabilities.get(subseq).getValue() : 0.0;
				probabilities.set(subseq, temp + 1);
				
				//just for easier access
				dist.put(subseq, temp+1);
				
				counts[subseq.length()-1]++;
			}
		}
	}
	
	public byte[] generateSequence(int sequence_length, boolean withPrimers)
	{
		StringBuilder sequence = new StringBuilder(sequence_length);
		
		sequence.append(withPrimers ? p5 : "");
		
		for (int x=0; x<sequence_length; x++)
		{
			sequence.append(generateNucleotide(sequence));
		}
		
		sequence.append(withPrimers ? p3 : "");
		
		return sequence.toString().getBytes();
	}
	
	public Character generateNucleotide(StringBuilder sb)
	{
		//how many nucleotides can we make this dependent on?
		int max_degree = Math.min(degree-1, sb.length());
		
		String last_n = sb.substring(sb.length()-max_degree);
		
		//forward to the corresponding probability branch
		HMMData current = this.probabilities;
		for (int x=0; x < max_degree; x++)
		{
			current = current.get(last_n.charAt(x));
		}
			
		return generateConditionalNucleotide(current);
	}
	
	/**
	 * Given the nucleotide described in <code>data</code>, generate another nucleotide corresponding to the probabilitiy
	 * @param data
	 * @return
	 */
	public Character generateConditionalNucleotide(HMMData data)
	{
		Double total_counts = data.getTotalCounts();
		int num = rand.nextInt(total_counts.intValue());
		
		Double comulative = 0.0; 
		Character n = 'N';
		
		for (  Entry<Character, HMMData> c : data.getEntrySet())
		{
			comulative += c.getValue().getValue();
			
			if (num < comulative)
			{
				n = c.getKey();
				break;
			}
		}
		
		return n;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String[] keys = this.dist.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		
		for (String s : keys)
		{
			sb.append(s);
			sb.append("=");
			sb.append(this.dist.get(s));
			sb.append(", ");
		}
		
		return sb.toString();
	}
	
	public String extrapolateFromDegree(int degree)
	{
		System.out.println("Extrapolated Probability Matrix for degree " + degree);
		ArrayList<String> singles = new ArrayList<String>();
		singles.add("A");
		singles.add("C");
		singles.add("G");
		singles.add("T");
		
		//get all keys of current degree
		List<String> keys = new ArrayList<String>();
		for (String s : this.dist.keySet())
		{
			if (s.length()==degree)
			{
				keys.add(s);
			}
		}
		Collections.sort(keys);
		
		StringBuilder sb = new StringBuilder();
		Double temp;
		for (int x=0; x<keys.size(); x++)
		{
			sb.append(keys.get(x) + "\t");
			for (int y=0; y<singles.size(); y++)
			{
				temp = (this.dist.get(keys.get(x)) / this.counts[degree-1]) * (this.dist.get(singles.get(y)) / this.counts[0] );
								
				sb.append( df.format(temp) );
				sb.append("\t");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	
	public String printMatrix(int degree)
	{
		System.out.println("Probability Matrix for degree " + degree);
		ArrayList<String> singles = new ArrayList<String>();
		singles.add("A");
		singles.add("C");
		singles.add("G");
		singles.add("T");
		
		//get all keys of current degree
		ArrayList<String> keys = new ArrayList<String>();
		for (String s : this.dist.keySet())
		{
			if (s.length()==degree)
			{
				keys.add(s);
			}
		}
		Collections.sort(keys);
		
		StringBuilder sb = new StringBuilder();
		String temp;
		for (int x=0; x<keys.size(); x++)
		{
			sb.append(keys.get(x) + "\t");
			for (int y=0; y<singles.size(); y++)
			{
				temp = keys.get(x) + singles.get(y);
				sb.append( df.format( this.dist.containsKey(temp) ? this.dist.get(temp)/this.counts[degree-1] : 0.0 ) );
				sb.append("\t");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}	
	
	public String printNucleotideDistribution()
	{
		System.out.println("Nucleotide Distribution:");
		ArrayList<String> singles = new ArrayList<String>();
		singles.add("A");
		singles.add("C");
		singles.add("G");
		singles.add("T");
		
		StringBuilder sb = new StringBuilder();
		for (int x=0; x<singles.size(); x++)
		{
			sb.append(singles.get(x) + ": " );
			sb.append( df.format( this.dist.containsKey(singles.get(x)) ? this.dist.get(singles.get(x))/this.counts[0] : 0.0 ) );
			sb.append("\t");
		}
		
		return sb.toString();
	}
	
}
