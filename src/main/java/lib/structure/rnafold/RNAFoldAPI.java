/**
 * 
 */
package lib.structure.rnafold;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import utilities.Index;

/**
 * @author Jan Hoinka
 * 
 *         Implements a API for RNAfold to perform folding and control
 *         parameters programmatically
 *
 */
public class RNAFoldAPI {

	private String ns_bases = null, c;
	private double kT, sfact = 1.07;
	private int noconv = 0;
	private int circ = 0;
	private Path outputPath = Paths.get(System.getProperty("user.dir"));
	private byte[] foldconstrains = null;
	private double pmin = 1e-15;
	
	// set up parameters and classes
	private FoldVars fold_vars = new FoldVars();
	private PairMat pair_mat = new PairMat(fold_vars);
	private PartFunc part_func = new PartFunc(fold_vars, pair_mat);
	private Params params = new Params(fold_vars);
	private Fold fold = new Fold(fold_vars, pair_mat, params);
	
	
	/**
	 * Standard Constructor, sets all default values as if calling RNAFold without
	 * any parameters
	 */
	public RNAFoldAPI() {

		fold_vars.do_backtrack = 1;

	}

	/**
	 * 
	 * @param string
	 *            assumed to be in upper case and no undefined alphabet
	 */
	public MFEData getMFE(byte[] string) {

		// Initialize variables
		int l;
		int length = string.length;
		double energy;
		byte[] structure = null;
		byte[] cstruc = null; // structure constaints

		// structure contstains
		if (fold_vars.fold_constrained) {
			if (length != foldconstrains.length) {

				throw new RuntimeException("The fold contrains and input sequence have unequal length");

			}
			structure = Arrays.copyOf(foldconstrains, foldconstrains.length);
		} else {
			structure = new byte[length];
		}

		// Convert U to Ts
		if (noconv == 0) {
			for (l = 0; l < length; l++) {
				if (string[l] == 'T')
					string[l] = 'U';
			}
		}

		// get mfe
		if (circ != 0)
			energy = fold.circfold(string, structure);
		else
			energy = fold.fold(string, structure);

		// clean up
		cstruc = null;
		string = null;
		fold.free_arrays();

		return new MFEData(structure, energy);
	}

	/**
	 * Returns the base pair probability matrix for sequence string 
	 * as a linearized upper triangular matrix.
	 * 
	 * @param string
	 *            assumed to be in upper case and no undefined alphabet
	 */
	public double[] getBppm(byte[] string) {

		// Initialize variables
		int l;
		int length = string.length;
		double energy;
		byte[] structure = null;
		byte[] cstruc = null; // structure constaints


		// structure contstains
		if (fold_vars.fold_constrained) {
			if (length != foldconstrains.length) {

				throw new RuntimeException("The fold contrains and input sequence have unequal length");

			}
			structure = cstruc;
		} else {

			structure = new byte[length];

		}

		// Convert T to Us
		if (noconv == 0) {
			for (l = 0; l < length; l++) {
				if (string[l] == 'T')
					string[l] = 'U';
			}
		}

		// get mfe
		if (circ != 0)
			energy = fold.circfold(string, structure);
		else
			energy = fold.fold(string, structure);

		byte[] pf_struc = new byte[length + 1];
		if (fold_vars.dangles == 1) {
			fold_vars.dangles = 2; /* recompute with dangles as in pf_fold() */
			energy = (circ != 0) ? fold.energy_of_circ_struct(string, structure)
					: fold.energy_of_struct(string, structure);
			fold_vars.dangles = 1;
		}

		kT = (fold_vars.temperature + 273.15) * 1.98717 / 1000.; /* in Kcal */
		fold_vars.pf_scale = Math.exp(-(sfact * energy) / kT / length);

		if (circ != 0) {
			part_func.init_pf_circ_fold(length);
		} else {
			part_func.init_pf_fold(length);
		}

		if (foldconstrains != null) {
			pf_struc = Arrays.copyOf(foldconstrains, length + 1);
		}
		energy = (circ != 0) ? part_func.pf_circ_fold(string, pf_struc) : part_func.pf_fold(string, pf_struc);

		// store the base pair probability matrix 
		double[] bppm = new double[(((length+1)*length)/2) - length];
		for (int i = 1; i < length; i++)
			for (int j = i + 1; j <= length; j++) {
				// bppm is 0 indexed, hence -1
				bppm[Index.triu(i-1, j-1, length)] = fold_vars.pr[fold_vars.iindx[i] - j];
		}
		
		// cleanup
		pf_struc = null;

		part_func.free_pf_arrays();

		cstruc = null;
		string = null;
		structure = null;

		fold.free_arrays();

		return bppm;
	}

	/**
	 * Sets the folding temperature of this instance. Equivalent to the -T option in
	 * RNAFold
	 * 
	 * @param temp
	 */
	public void setTemperature(double temp) {

		fold_vars.temperature = temp;

	}

	/**
	 * Sets whether GU pairs should be allowed or not Equivalent to the -noGU option
	 * in RNAFold
	 * 
	 * @param nogu
	 */
	public void setNoGU(boolean nogu) {

		fold_vars.noGU = nogu;

	}

	/**
	 * Sets whether closing GU pairs are allowed Equivalent to the -noCloseGU option
	 * in RNAFold
	 * 
	 * @param noclosegu
	 */
	public void setNoCloseGU(boolean noclosegu) {

		fold_vars.no_closingGU = noclosegu;
	}

	/**
	 * Sets whether lonely pairs should be allowed or not. Equivalent to the
	 * -noLonelyPair option in RNAFold
	 * 
	 * @param nolonelypair
	 */
	public void setNoLonelyPairs(boolean nolonelypair) {

		fold_vars.noLonelyPairs = nolonelypair;

	}

	/**
	 * Allow other pairs in addition to the usual AU,GC,and GU pairs. Nonstandard
	 * pairs are given 0 stacking energy.
	 * 
	 * @param nsp
	 *            a string of comma separated, additionally allowed nucleotide
	 *            pairs. If a the first character is a "-" then AB will imply that
	 *            AB and BA are allowed pairs. "-GA" will allow GA and AG pairs
	 */
	public void setNonStandardPairs(String nsp) {

		ns_bases = nsp;

	}

	/**
	 * Rarely used option to fold sequences from the artificial ABCD... alphabet,
	 * where A pairs B, C-D etc. Use the energy parameters for GC (e = 1) or AU (e =
	 * 2) pairs.
	 * 
	 * @param e
	 */
	public void setEnergySet(int e) {

		fold_vars.energy_set = e;

	}

	/**
	 * Sets whether the folding is constrained or not
	 * 
	 * @param foldc
	 */
	public void setFoldConstrained(boolean foldc) {

		fold_vars.fold_constrained = foldc;

	}

	/**
	 * Sets whether the the actual constraints. Requires call to
	 * setFoldConstrained(true)
	 * 
	 * @param foldconstrains
	 */
	public void setFoldConstrains(byte[] fc) {

		foldconstrains = fc;

	}

	/**
	 * In the calculation of the pf use scale*mfe as an estimate for the ensemble
	 * free energy (used to avoid overflows). The default is 1.07, useful values are
	 * 1.0 to 1.2. Occasionally needed for long sequences.
	 * 
	 * @param scale
	 */
	public void setScale(double scale) {

		sfact = scale;

	}

	/**
	 * How to treat "dangling end" energies for bases adjacent to helices in free
	 * ends and multi-loops: With d=1 only unpaired bases can participate in at most
	 * one dangling end, this is the default for mfe folding but unsupported for the
	 * partition function folding. With d=2 this check is ignored, dangling energies
	 * will be added for the bases adjacent to a helix on both sides in any case;
	 * this is the default for partition function folding (p). d=0 ignores dangling
	 * ends altogether (mostly for debugging).
	 * 
	 * With d=3, mfe folding will allow coaxial stacking of adjacent helices in
	 * multi-loops. At the moment the implementation will not allow coaxial stacking
	 * of the two interior pairs in a loop of degree 3 and works only for mfe
	 * folding.
	 * 
	 * Note that by default (as well as with d=1 and d=3) pf and mfe folding treat
	 * dangling ends differently. Use d=2 in addition to p to ensure that both
	 * algorithms use the same energy model.
	 * 
	 * @param d
	 *            allowed values: 0,1,2 or 3
	 */
	public void setDangle(int d) {

		if (d == 0 || d == 1 || d == 2 || d == 3) {

			fold_vars.dangles = d;

		}

		else {

			throw new RuntimeException(String.format("invalid value '%s' in setDangle()", d));

		}

	}

	/**
	 * Set whether folding should assume a circular rna or not
	 * 
	 * @param c
	 */
	public void setCircular(boolean c) {

		circ = c ? 1 : 0;
	}

}
