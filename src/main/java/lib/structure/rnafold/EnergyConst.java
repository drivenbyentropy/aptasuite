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
 * energy constants, formerly defined in
 *           energy_par.h
 *
 * customized for use with RNAedit by
 * S.Kopp, IMB-Jena, Germany, Mar 1996
 */


public final class EnergyConst {
	static double GASCONST = 1.98717;  /* in [cal/K] */
	static double K0 =  273.15;
	static int INF = 1000000;
	static int FORBIDDEN = 9999;
	static int BONUS = 10000;
	static int NBPAIRS = 7;
	static int TURN = 3;
	static int MAXLOOP = 30;
}
