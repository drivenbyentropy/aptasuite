/**
 * 
 */
package lib.structure.rnafold;

/**
 * @author Jan Hoinka
 *
 * RNAFold4j is a port of RNAFold to Java. RNAFold is implemented and developed by Ivo L Hofacker et al.
 * as part of the ViennaRNA package v 1.8.5. All intellectual credits of this work 
 * go to the original authors and the Institute for Theoretical Chemistry of the 
 * University of Vienna. My only contribution is the adaptation of the C source code to Java.
 * 
 * global variables to change behaviour of folding routines Vienna RNA
 * package
 *
 */
public class FoldVars {

	public boolean noGU = false; /* GU not allowed at all */
	public boolean no_closingGU = false; /* GU allowed only inside stacks */
	public boolean tetra_loop = true; /* Fold with specially stable 4-loops */
	public int energy_set = 0; /* 0 = BP; 1=any with GC; 2=any with AU parameters */
	public int dangles = 1; /* use dangling end energies */
	public byte[] nonstandards = null; /* contains allowed non standard bases */
	public double temperature = 37.0;
	public int james_rule = 1; /*
								 * interior loops of size 2 get energy 0.8Kcal and no mismatches (no longer
								 * used)
								 */
	int oldAliEn = 0; /* use old alifold-energies (without removing gaps) */
	int ribo = 0; /* use ribosum instead of classic covariance term */
	Character RibosumFile = null; /*
									 * TODO: compile ribosums into program Warning: this variable will vanish
									 */

	public BondT[] base_pair = null;

	public double[] pr = null; /* base pairing prob. matrix */
	public int[] iindx; /* pr[i,j] -> pr[iindx[i]-j] */
	public double pf_scale = -1; /* scaling factor to avoid floating point overflows */
	public boolean fold_constrained = false; /* fold with constraints */
	public int do_backtrack = 1; /* calculate pair prob matrix in part_func() */
	public boolean noLonelyPairs = false; /* avoid helices of length 1 */
	public char backtrack_type = 'F'; /*
										 * 'C' require (1,N) to be bonded; 'M' seq is part of s multi loop
										 */

	int cut_points;
	int strand;

	String option_string() {
		StringBuilder options = new StringBuilder();
		if (noGU)
			options.append("-noGU ");
		if (no_closingGU)
			options.append("-noCloseGU ");
		if (!tetra_loop)
			options.append("-4 ");
		if (noLonelyPairs)
			options.append("-noLP ");
		if (fold_constrained)
			options.append("-C ");
		if (dangles != 1)
			options.append(String.format("-d%d ", dangles));
		if (temperature != 37.0)
			options.append(String.format("-T %f ", temperature));
		return options.toString();
	}

}
