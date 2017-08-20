/**
 * 
 */
package lib.structure.rnafold;

/**
 * @author Jan Hoinka
 *
 * RNAFold4j is a port of RNAFold implemented and developed by Ivo L Hofacker et al.
 * as part of the ViennaRNA package v 1.8.5. All intellectual credits of this work 
 * go to the original authors and the Institute for Theoretical Chemistry of the 
 * University of Vienna. My only contribution is the adaptation of the C source to Java.
 * 
 * global variables to change behaviour of folding routines Vienna RNA
 * package
 *
 */
public final class FoldVars {

	static boolean noGU = false; /* GU not allowed at all */
	static boolean no_closingGU = false; /* GU allowed only inside stacks */
	static boolean tetra_loop = true; /* Fold with specially stable 4-loops */
	static int energy_set = 0; /* 0 = BP; 1=any with GC; 2=any with AU parameters */
	static int dangles = 1; /* use dangling end energies */
	static byte[] nonstandards = null; /* contains allowed non standard bases */
	static double temperature = 37.0;
	static int james_rule = 1; /*
								 * interior loops of size 2 get energy 0.8Kcal and no mismatches (no longer
								 * used)
								 */
	int oldAliEn = 0; /* use old alifold-energies (without removing gaps) */
	int ribo = 0; /* use ribosum instead of classic covariance term */
	Character RibosumFile = null; /*
									 * TODO: compile ribosums into program Warning: this variable will vanish
									 */

	static BondT[] base_pair = null;

	static double[] pr = null; /* base pairing prob. matrix */
	static int[] iindx; /* pr[i,j] -> pr[iindx[i]-j] */
	static double pf_scale = -1; /* scaling factor to avoid floating point overflows */
	static boolean fold_constrained = false; /* fold with constraints */
	static int do_backtrack = 1; /* calculate pair prob matrix in part_func() */
	static boolean noLonelyPairs = false; /* avoid helices of length 1 */
	static char backtrack_type = 'F'; /*
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
