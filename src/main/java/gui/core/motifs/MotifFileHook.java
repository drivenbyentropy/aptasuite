/**
 * 
 */
package gui.core.motifs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;

import gui.charts.logo.Alphabet;
import gui.charts.logo.LogoChartPanelController;
import gui.charts.logo.Scale;
import utilities.AptaLogger;
import utilities.Quicksort;

/**
 * @author Jan Hoinka
 * Helper class to get quick access to the motif data 
 * on file
 */
public class MotifFileHook {

	private Integer motif_id;
	
	private Path aptamers;
	
	private Path context;
	
	private Path pwm;
	
	private Path general;

	private String[] kmers = null;
	
	private String[] kmers_aligned = null;
	
	private double pvalue;
	
	private double seed_frequency;
	
	private double motif_frequency;
	
	private boolean is_dummy = false;
	
	public MotifFileHook(Integer id) {
		
		this.motif_id = id;
		
	}
	
	/**
	 * @return the motif_id
	 */
	public Integer getMotif_id() {
		return motif_id;
	}

	/**
	 * @param motif_id the motif_id to set
	 */
	public void setMotif_id(Integer motif_id) {
		this.motif_id = motif_id;
	}

	/**
	 * @return the aptamers
	 */
	public Path getAptamers() {
		return aptamers;
	}

	/**
	 * @param aptamers the aptamers to set
	 */
	public void setAptamers(Path aptamers) {
		this.aptamers = aptamers;
	}

	/**
	 * @return the context
	 */
	public Path getContext() {
		return context;
	}

	/**
	 * Returns the ids of the aptemers the motif is contained in
	 * @return
	 */
	public int[] getSequenceIds() {
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		try {
			
			Scanner input = new Scanner(this.aptamers.toFile());

			// Skip the first line
			while (input.hasNextLine()) {
				
				String[] tokens = input.nextLine().split("\t");
				ids.add( Integer.parseInt(tokens[0]) );
				
			}
			
			input.close();
			
		} catch (Exception e) {
			
			AptaLogger.log(Level.INFO, this.getClass(), e);
			
		}
		
		int[] result = ids.stream().mapToInt(i->i).toArray();
		Quicksort.sort(result);
		
		return result;
		
	}
	
	/**
	 * @param context the context to set
	 */
	public void setContext(Path context) {
		this.context = context;
	}

	/**
	 * Sets an the data of the motifs PWM as 
	 * for <code>LogoChartPanelController</code> instance.
	 */
	public void setPwmData(LogoChartPanelController pwm) {
		
		// Clear anything previously stored in the pwm
		pwm.clear();
		
		try {
			
			// get the logo information 
			double[][] data = new double[4][];
			
			ArrayList<Double> As = new ArrayList<Double>();
			ArrayList<Double> Cs = new ArrayList<Double>();
			ArrayList<Double> Gs = new ArrayList<Double>();
			ArrayList<Double> Ts = new ArrayList<Double>();
			
			Scanner input = new Scanner(this.pwm.toFile());
			// Skip the first line
			input.nextLine();
			while (input.hasNextLine()) {
				
				String[] tokens = input.nextLine().split(" ");
				As.add(Double.parseDouble(tokens[1]));
				Gs.add(Double.parseDouble(tokens[2]));
				Ts.add(Double.parseDouble(tokens[3]));
				Cs.add(Double.parseDouble(tokens[4]));
				
			}
			
			input.close();
			
			// Add to data
			data[0] = As.stream().mapToDouble(d->d).toArray();
			data[1] = Cs.stream().mapToDouble(d->d).toArray();
			data[2] = Gs.stream().mapToDouble(d->d).toArray();
			data[3] = Ts.stream().mapToDouble(d->d).toArray();
			
			
			pwm.setAlphabet(Alphabet.DNA);
			pwm.setData(data);
			pwm.setLabels( java.util.stream.IntStream.rangeClosed(1, As.size()).mapToObj( i -> ""+i).toArray(String[]::new) );
			pwm.setScale(Scale.BITSCORE);
			
		} catch (Exception e) {
			
			AptaLogger.log(Level.INFO, this.getClass(), e);
			
			pwm.clear();
			
		}
		
	}

	/**
	 * Sets an the data of the motifs PWM as 
	 * for <code>LogoChartPanelController</code> instance.
	 */
	public void setContextData(LogoChartPanelController context) {
		
		// Clear anything previously stored in the pwm
		context.clear();
		
		try {
			
			// get the logo information 
			double[][] data = new double[6][];
			
			ArrayList<String> rounds = new ArrayList<String>();
			ArrayList<Double> Hs = new ArrayList<Double>();
			ArrayList<Double> Bs = new ArrayList<Double>();
			ArrayList<Double> Is = new ArrayList<Double>();
			ArrayList<Double> Ms = new ArrayList<Double>();
			ArrayList<Double> Ds = new ArrayList<Double>();
			ArrayList<Double> Ps = new ArrayList<Double>();
			
			Scanner input = new Scanner(this.context.toFile());
			// Skip the first line
			input.nextLine();
			while (input.hasNextLine()) {
				
				String[] tokens = input.nextLine().split(" ");
				
				rounds.add(tokens[0]);
				Hs.add(Double.parseDouble(tokens[1]));
				Bs.add(Double.parseDouble(tokens[2]));
				Is.add(Double.parseDouble(tokens[3]));
				Ms.add(Double.parseDouble(tokens[4]));
				Ds.add(Double.parseDouble(tokens[5]));
				Ps.add(Double.parseDouble(tokens[6]));
				
			}
			
			input.close();
			
			// Add to data
			data[0] = Hs.stream().mapToDouble(d->d).toArray();
			data[1] = Bs.stream().mapToDouble(d->d).toArray();
			data[2] = Is.stream().mapToDouble(d->d).toArray();
			data[3] = Ms.stream().mapToDouble(d->d).toArray();
			data[4] = Ds.stream().mapToDouble(d->d).toArray();
			data[5] = Ps.stream().mapToDouble(d->d).toArray();
			
			
			context.setAlphabet(Alphabet.STRUCTURE_CONTEXT);
			context.setData(data);
			context.setLabels( rounds.toArray(new String[rounds.size()]));
			context.setScale(Scale.FREQUENCY);
			
		} catch (Exception e) {
			
			AptaLogger.log(Level.INFO, this.getClass(), e);
			
			context.clear();
			
		}
		
	}
	
	
	/**
	 * @return the seed k-mer this motif is based on 
	 */
	public String getSeed() {
		
		return this.kmers[0];
		
	}
	
	/**
	 * @param pwm the pwm to set
	 */
	public void setPwm(Path pwm) {
		this.pwm = pwm;
	}
	
	@Override
	public String toString() {
		
		if (is_dummy) return "No motifs were found.";
		
		// Lazy load motif info
		if (kmers == null) {
			
			getMotifInfo();
			
		}
		
		return "Motif " + this.motif_id + " " + this.kmers[0];
		
	}

	public void getMotifInfo() {
		
		try {

			ArrayList<String> kmerslocal = new ArrayList<String>();
			ArrayList<String> kmersalignedlocal = new ArrayList<String>();
			
			Scanner input = new Scanner(this.general.toFile());

			boolean first_line = true;
			while (input.hasNextLine()) {
				
				String[] tokens = input.nextLine().split("\t");
				
				// First line contains the percentages of the motif
				if (first_line) {
					
					this.pvalue = Double.parseDouble(tokens[0]);
					this.seed_frequency = Double.parseDouble(tokens[1].substring(0, tokens[1].length() - 1));
					
					first_line = false;
					continue;
				}
				
				kmerslocal.add(tokens[0]);
				kmersalignedlocal.add(tokens[1]);
				
			}
			
			input.close();
			
			kmers = kmerslocal.toArray(new String[kmerslocal.size()]);
			kmers_aligned = kmersalignedlocal.toArray(new String[kmersalignedlocal.size()]);
			
			
		} catch (Exception e) {
			
			AptaLogger.log(Level.INFO, this.getClass(), e);
			
		}
		
	}
	
	/**
	 * @return the general
	 */
	public Path getGeneral() {
		return general;
	}

	/**
	 * @param general the general to set
	 */
	public void setGeneral(Path general) {
		this.general = general;
	}

	/**
	 * @return the kmers
	 */
	public String[] getKmers() {
		
		// Lazy Load
		if (this.kmers == null) { getMotifInfo(); }
		
		return kmers;
	}

	/**
	 * @param kmers the kmers to set
	 */
	public void setKmers(String[] kmers) {
		this.kmers = kmers;
	}

	/**
	 * @return the kmers_aligned
	 */
	public String[] getKmers_aligned() {
		
		// Lazy Load
		if (this.kmers == null) { getMotifInfo(); }
		
		return kmers_aligned;
	}

	/**
	 * @param kmers_aligned the kmers_aligned to set
	 */
	public void setKmers_aligned(String[] kmers_aligned) {
		this.kmers_aligned = kmers_aligned;
	}

	/**
	 * @return the pvalue
	 */
	public double getPvalue() {
		
		// Lazy Load
		if (this.kmers == null) { getMotifInfo(); }
		
		return pvalue;
	}

	/**
	 * @param pvalue the pvalue to set
	 */
	public void setPvalue(double pvalue) {
		this.pvalue = pvalue;
	}

	/**
	 * @return the seed_frequency
	 */
	public double getSeed_frequency() {
		
		// Lazy Load
		if (this.kmers == null) { getMotifInfo(); }
		
		return seed_frequency;
	}

	/**
	 * @param seed_frequency the seed_frequency to set
	 */
	public void setSeed_frequency(double seed_frequency) {
		this.seed_frequency = seed_frequency;
	}

	/**
	 * @return the motif_frequency
	 */
	public double getMotif_frequency() {
		
		// Lazy Load
		if (this.kmers == null) { getMotifInfo(); }
		
		return motif_frequency;
	}

	/**
	 * @param motif_frequency the motif_frequency to set
	 */
	public void setMotif_frequency(double motif_frequency) {
		this.motif_frequency = motif_frequency;
	}
	
	public void setIsDummy(boolean dummy) {
		
		this.is_dummy = dummy;
		
	}
	
	public boolean isDummy() {
		return this.is_dummy;
	}
	
}
