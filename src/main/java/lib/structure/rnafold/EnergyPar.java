/**
 * 
 */
package lib.structure.rnafold;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jan Hoinka
 *
 * RNAFold4j is a port of RNAFold to Java. RNAFold is implemented and developed by Ivo L Hofacker et al.
 * as part of the ViennaRNA package v 1.8.5. All intellectual credits of this work 
 * go to the original authors and the Institute for Theoretical Chemistry of the 
 * University of Vienna. My only contribution is the adaptation of the C source code to Java.
 * 
 * Current free energy parameters are summarized in:
 *
 *         D.H.Mathews, J. Sabina, M. ZUker, D.H. Turner Expanded sequence
 *         dependence of thermodynamic parameters improves prediction of RNA
 *         secondary structure" JMB, 288, pp 911-940, 1999
 *
 *         Enthalpies taken from:
 * 
 *         A. Walter, D Turner, J Kim, M Lyttle, P M"uller, D Mathews, M Zuker
 *         "Coaxial stckaing of helices enhances binding of
 *         oligoribonucleotides.." PNAS, 91, pp 9218-9222, 1994
 * 
 *         D.H. Turner, N. Sugimoto, and S.M. Freier. "RNA Structure
 *         Prediction", Ann. Rev. Biophys. Biophys. Chem. 17, 167-192, 1988.
 *
 *         John A.Jaeger, Douglas H.Turner, and Michael Zuker. "Improved
 *         predictions of secondary structures for RNA", PNAS, 86, 7706-7710,
 *         October 1989.
 * 
 *         L. He, R. Kierzek, J. SantaLucia, A.E. Walter, D.H. Turner
 *         "Nearest-Neughbor Parameters for GU Mismatches...." Biochemistry
 *         1991, 30 11124-11132
 *
 *         A.E. Peritz, R. Kierzek, N, Sugimoto, D.H. Turner "Thermodynamic
 *         Study of Internal Loops in Oligoribonucleotides..." Biochemistry
 *         1991, 30, 6428--6435
 *
 */
public final class EnergyPar {

	static int NST = 0; /* Energy for nonstandard stacked pairs */
	static int DEF = -50; /* Default terminal mismatch, used for I */
	/* and any non_pairing bases */
	static int NSM = 0; /* terminal mismatch for non standard pairs */

	static double Tmeasure = 37 + EnergyConst.K0; /* temperature of param measurements */
	static double lxc37 = 107.856; /*
									 * parameter for logarithmic loop energy extrapolation
									 */

	static int stack37[][] =
			/* CG GC GU UG AU UA */
			{ { EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF,
					EnergyConst.INF, EnergyConst.INF }, { EnergyConst.INF, -240, -330, -210, -140, -210, -210, NST },
					{ EnergyConst.INF, -330, -340, -250, -150, -220, -240, NST },
					{ EnergyConst.INF, -210, -250, 130, -50, -140, -130, NST },
					{ EnergyConst.INF, -140, -150, -50, 30, -60, -100, NST },
					{ EnergyConst.INF, -210, -220, -140, -60, -110, -90, NST },
					{ EnergyConst.INF, -210, -240, -130, -100, -90, -130, NST },
					{ EnergyConst.INF, NST, NST, NST, NST, NST, NST, NST } };

	/* enthalpies (0.01*kcal/mol at 37 C) for stacked pairs */
	/* different from mfold-2.3, which uses values from mfold-2.2 */
	static int enthalpies[][] =
			/* CG GC GU UG AU UA */
			{ { EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF,
					EnergyConst.INF, EnergyConst.INF },
					{ EnergyConst.INF, -1060, -1340, -1210, -560, -1050, -1040, NST },
					{ EnergyConst.INF, -1340, -1490, -1260, -830, -1140, -1240, NST },
					{ EnergyConst.INF, -1210, -1260, -1460, -1350, -880, -1280, NST },
					{ EnergyConst.INF, -560, -830, -1350, -930, -320, -700, NST },
					{ EnergyConst.INF, -1050, -1140, -880, -320, -940, -680, NST },
					{ EnergyConst.INF, -1040, -1240, -1280, -700, -680, -770, NST },
					{ EnergyConst.INF, NST, NST, NST, NST, NST, NST, NST } };

	/* old values are here just for comparison */
	static int oldhairpin37[] = { /* from ViennaRNA 1.3 */
			EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, 410, 490, 440, 470, 500, 510, 520, 531, 542, 551, 560,
			568, 575, 582, 589, 595, 601, 606, 611, 616, 621, 626, 630, 634, 638, 642, 646, 650 };

	static int hairpin37[] = { EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, 570, 560, 560, 540, 590, 560, 640,
			650, 660, 670, 678, 686, 694, 701, 707, 713, 719, 725, 730, 735, 740, 744, 749, 753, 757, 761, 765, 769 };

	static int oldbulge37[] = { EnergyConst.INF, 390, 310, 350, 420, 480, 500, 516, 531, 543, 555, 565, 574, 583, 591,
			598, 605, 612, 618, 624, 630, 635, 640, 645, 649, 654, 658, 662, 666, 670, 673 };

	static int bulge37[] = { EnergyConst.INF, 380, 280, 320, 360, 400, 440, 459, 470, 480, 490, 500, 510, 519, 527, 534,
			541, 548, 554, 560, 565, 571, 576, 580, 585, 589, 594, 598, 602, 605, 609 };

	static int oldinternal_loop37[] = { EnergyConst.INF, EnergyConst.INF, 410, 510, 490, 530, 570, 587, 601, 614, 625,
			635, 645, 653, 661, 669, 676, 682, 688, 694, 700, 705, 710, 715, 720, 724, 728, 732, 736, 740, 744 };

	static int internal_loop37[] = { EnergyConst.INF, EnergyConst.INF, 410, 510, 170, 180, 200, 220, 230, 240, 250, 260,
			270, 278, 286, 294, 301, 307, 313, 319, 325, 330, 335, 340, 345, 349, 353, 357, 361, 365, 369 };

	/* terminal mismatches */
	/* mismatch free energies for interior loops at 37C */
	static int mismatchI37[][][] = { /* @@ */
			{ { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } }, { /*
																													 * CG
																													 */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 0, 0, -110, 0 }, /* A@ AA AC AG AU */
					{ 0, 0, 0, 0, 0 }, /* C@ CA CC CG CU */
					{ 0, -110, 0, 0, 0 }, /* G@ GA GC GG GU */
					{ 0, 0, 0, 0, -70 } }, /* U@ UA UC UG UU */
			{ /* GC */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 0, 0, -110, 0 }, /* A@ AA AC AG AU */
					{ 0, 0, 0, 0, 0 }, /* C@ CA CC CG CU */
					{ 0, -110, 0, 0, 0 }, /* G@ GA GC GG GU */
					{ 0, 0, 0, 0, -70 } }, /* U@ UA UC UG UU */
			{ /* GU */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 70, 70, -40, 70 }, /* A@ AA AC AG AU */
					{ 0, 70, 70, 70, 70 }, /* C@ CA CC CG CU */
					{ 0, -40, 70, 70, 70 }, /* G@ GA GC GG GU */
					{ 0, 70, 70, 70, 0 } }, /* U@ UA UC UG UU */
			{ /* UG */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 70, 70, -40, 70 }, /* A@ AA AC AG AU */
					{ 0, 70, 70, 70, 70 }, /* C@ CA CC CG CU */
					{ 0, -40, 70, 70, 70 }, /* G@ GA GC GG GU */
					{ 0, 70, 70, 70, 0 } }, /* U@ UA UC UG UU */
			{ /* AU */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 70, 70, -40, 70 }, /* A@ AA AC AG AU */
					{ 0, 70, 70, 70, 70 }, /* C@ CA CC CG CU */
					{ 0, -40, 70, 70, 70 }, /* G@ GA GC GG GU */
					{ 0, 70, 70, 70, 0 } }, /* U@ UA UC UG UU */
			{ /* UA */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 70, 70, -40, 70 }, /* A@ AA AC AG AU */
					{ 0, 70, 70, 70, 70 }, /* C@ CA CC CG CU */
					{ 0, -40, 70, 70, 70 }, /* G@ GA GC GG GU */
					{ 0, 70, 70, 70, 0 } }, /* U@ UA UC UG UU */
			{ /* @@ */
					{ 90, 90, 90, 90, 90 }, { 90, 90, 90, 90, -20 }, { 90, 90, 90, 90, 90 }, { 90, -20, 90, 90, 90 },
					{ 90, 90, 90, 90, 20 } } };

	/* mismatch free energies for hairpins at 37C */
	static int mismatchH37[][][] = { /* @@ */
			{ { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } }, { /*
																													 * CG
																													 */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ -90, -150, -150, -140, -180 }, /* A@ AA AC AG AU */
					{ -90, -100, -90, -290, -80 }, /* C@ CA CC CG CU */
					{ -90, -220, -200, -160, -110 }, /* G@ GA GC GG GU */
					{ -90, -170, -140, -180, -200 } }, /* U@ UA UC UG UU */
			{ /* GC */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ -70, -110, -150, -130, -210 }, /* A@ AA AC AG AU */
					{ -70, -110, -70, -240, -50 }, /* C@ CA CC CG CU */
					{ -70, -240, -290, -140, -120 }, /* G@ GA GC GG GU */
					{ -70, -190, -100, -220, -150 } }, /* U@ UA UC UG UU */
			{ /* GU */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, 20, -50, -30, -30 }, /* A@ AA AC AG AU */
					{ 0, -10, -20, -150, -20 }, /* C@ CA CC CG CU */
					{ 0, -90, -110, -30, 0 }, /* G@ GA GC GG GU */
					{ 0, -30, -30, -40, -110 } }, /* U@ UA UC UG UU */
			{ /* UG */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, -50, -30, -60, -50 }, /* A@ AA AC AG AU */
					{ 0, -20, -10, -170, 0 }, /* C@ CA CC CG CU */
					{ 0, -80, -120, -30, -70 }, /* G@ GA GC GG GU */
					{ 0, -60, -10, -60, -80 } }, /* U@ UA UC UG UU */
			{ /* AU */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, -30, -50, -30, -30 }, /* A@ AA AC AG AU */
					{ 0, -10, -20, -150, -20 }, /* C@ CA CC CG CU */
					{ 0, -110, -120, -20, 20 }, /* G@ GA GC GG GU */
					{ 0, -30, -30, -60, -110 } }, /* U@ UA UC UG UU */
			{ /* UA */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ 0, -50, -30, -60, -50 }, /* A@ AA AC AG AU */
					{ 0, -20, -10, -120, -0 }, /* C@ CA CC CG CU */
					{ 0, -140, -120, -70, -20 }, /* G@ GA GC GG GU */
					{ 0, -30, -10, -50, -80 } }, /* U@ UA UC UG UU */
			{ /* @@ */
					{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } } };

	/* mismatch energies in multiloops */
	static int[][][] mismatchM37 = new int[EnergyConst.NBPAIRS + 1][5][5];

	/* these are probably junk */
	/* mismatch enthalpies for temperature scaling */
	static int mism_H[][][] = { /* no pair */
			{ { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 } }, { /*
																													 * CG
																													 */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ DEF, -1030, -950, -1030, -1030 }, /* A@ AA AC AG AU */
					{ DEF, -520, -450, -520, -670 }, /* C@ CA CC CG CU */
					{ DEF, -940, -940, -940, -940 }, /* G@ GA GC GG GU */
					{ DEF, -810, -740, -810, -860 } }, /* U@ UA UC UG UU */
			{ /* GC */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ DEF, -520, -880, -560, -880 }, /* A@ AA AC AG AU */
					{ DEF, -720, -310, -310, -390 }, /* C@ CA CC CG CU */
					{ DEF, -710, -740, -620, -740 }, /* G@ GA GC GG GU */
					{ DEF, -500, -500, -500, -570 } }, /* U@ UA UC UG UU */
			{ /* GU */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ DEF, -430, -600, -600, -600 }, /* A@ AA AC AG AU */
					{ DEF, -260, -240, -240, -240 }, /* C@ CA CC CG CU */
					{ DEF, -340, -690, -690, -690 }, /* G@ GA GC GG GU */
					{ DEF, -330, -330, -330, -330 } }, /* U@ UA UC UG UU */
			{ /* UG */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ DEF, -720, -790, -960, -810 }, /* A@ AA AC AG AU */
					{ DEF, -480, -480, -360, -480 }, /* C@ CA CC CG CU */
					{ DEF, -660, -810, -920, -810 }, /* G@ GA GC GG GU */
					{ DEF, -550, -440, -550, -360 } }, /* U@ UA UC UG UU */
			{ /* AU */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ DEF, -430, -600, -600, -600 }, /* A@ AA AC AG AU */
					{ DEF, -260, -240, -240, -240 }, /* C@ CA CC CG CU */
					{ DEF, -340, -690, -690, -690 }, /* G@ GA GC GG GU */
					{ DEF, -330, -330, -330, -330 } }, /* U@ UA UC UG UU */
			{ /* UA */
					{ 0, 0, 0, 0, 0 }, /* @@ @A @C @G @U */
					{ DEF, -400, -630, -890, -590 }, /* A@ AA AC AG AU */
					{ DEF, -430, -510, -200, -180 }, /* C@ CA CC CG CU */
					{ DEF, -380, -680, -890, -680 }, /* G@ GA GC GG GU */
					{ DEF, -280, -140, -280, -140 } }, /* U@ UA UC UG UU */
			{ /* nonstandard pair */
					{ DEF, DEF, DEF, DEF, DEF }, { DEF, DEF, DEF, DEF, DEF }, { DEF, DEF, DEF, DEF, DEF },
					{ DEF, DEF, DEF, DEF, DEF }, { DEF, DEF, DEF, DEF, DEF } } };

	/* 5' dangling ends (unpaird base stacks on first paired base) */
	static int dangle5_37[][] = { /* @ A C G U */
			{ EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF }, /* no pair */
			{ EnergyConst.INF, -50, -30, -20, -10 }, /* CG (stacks on C) */
			{ EnergyConst.INF, -20, -30, -0, -0 }, /* GC (stacks on G) */
			{ EnergyConst.INF, -30, -30, -40, -20 }, /* GU */
			{ EnergyConst.INF, -30, -10, -20, -20 }, /* UG */
			{ EnergyConst.INF, -30, -30, -40, -20 }, /* AU */
			{ EnergyConst.INF, -30, -10, -20, -20 }, /* UA */
			{ 0, 0, 0, 0, 0 } /* @ */
	};

	/* 3' dangling ends (unpaired base stacks on second paired base */
	static int dangle3_37[][] = { /* @ A C G U */
			{ EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF }, /* no pair */
			{ EnergyConst.INF, -110, -40, -130, -60 }, /* CG (stacks on G) */
			{ EnergyConst.INF, -170, -80, -170, -120 }, /* GC */
			{ EnergyConst.INF, -70, -10, -70, -10 }, /* GU */
			{ EnergyConst.INF, -80, -50, -80, -60 }, /* UG */
			{ EnergyConst.INF, -70, -10, -70, -10 }, /* AU */
			{ EnergyConst.INF, -80, -50, -80, -60 }, /* UA */
			{ 0, 0, 0, 0, 0 } /* @ */
	};

	/* enthalpies for temperature scaling */
	static int dangle3_H[][] = { /* @ A C G U */
			{ EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF }, /* no pair */
			{ 0, -740, -280, -640, -360 }, { 0, -900, -410, -860, -750 }, { 0, -740, -240, -720, -490 },
			{ 0, -490, -90, -550, -230 }, { 0, -570, -70, -580, -220 }, { 0, -490, -90, -550, -230 },
			{ 0, 0, 0, 0, 0 } };

	static int dangle5_H[][] = { /* @ A C G U */
			{ EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF, EnergyConst.INF }, /* no pair */
			{ 0, -240, 330, 80, -140 }, { 0, -160, 70, -460, -40 }, { 0, 160, 220, 70, 310 }, { 0, -150, 510, 10, 100 },
			{ 0, 160, 220, 70, 310 }, { 0, -50, 690, -60, -60 }, { 0, 0, 0, 0, 0 } };

	/*
	 * constants for linearly destabilizing contributions for multi-loops F =
	 * ML_closing + ML_intern*k + ML_BASE*u
	 */
	/* old versions erroneously used ML_intern*(k-1) */
	static int ML_BASE37 = 0;
	static int ML_closing37 = 340;
	static int ML_intern37 = 40;

	/* Ninio-correction for asymmetric internal loops with branches n1 and n2 */
	/* ninio_energy = min{max_ninio, |n1-n2|*F_ninio[min{4.0, n1, n2}] } */
	static int MAX_NINIO = 300; /* maximum correction */
	static int F_ninio37[] = { 0, 40, 50, 20, 10 }; /* only F[2] used */

	/* stabilizing contribution due to special hairpins of size 4 (tetraloops) */

	static String Tetraloops = /* place for up to 200 tetra loops */
			"GGGGAC " + "GGUGAC " + "CGAAAG " + "GGAGAC " + "CGCAAG " + "GGAAAC " + "CGGAAG " + "CUUCGG " + "CGUGAG "
					+ "CGAAGG " + "CUACGG " + "GGCAAC " + "CGCGAG " + "UGAGAG " + "CGAGAG " + "AGAAAU " + "CGUAAG "
					+ "CUAACG " + "UGAAAG " + "GGAAGC " + "GGGAAC " + "UGAAAA " + "AGCAAU " + "AGUAAU " + "CGGGAG "
					+ "AGUGAU " + "GGCGAC " + "GGGAGC " + "GUGAAC " + "UGGAAA ";

	static int TETRA_ENERGY37[] = { -300, -300, -300, -300, -300, -300, -300, -300, -300, -250, -250, -250, -250, -250,
			-200, -200, -200, -200, -200, -150, -150, -150, -150, -150, -150, -150, -150, -150, -150, -150 };

	static int TETRA_ENTH37 = -400;

	static String Triloops = "";

	static int[] Triloop_E37 = new int[40];

	/* penalty for AU (or GU) terminating helix) */
	/* mismatches already contain these */
	static int TerminalAU = 50;

	/* penalty for forming a bi-molecular duplex */
	static int DuplexInit = 410;

	public static int[][][][] int11_37 = new int[EnergyConst.NBPAIRS+1][EnergyConst.NBPAIRS+1][5][5];
	static {

		// Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("RNAFold_int11_37.txt");
		BufferedInputStream bis = new BufferedInputStream(is);

		String token;
		int level = -1;
		int[] levels = { 0, 0, 0, 0 };
		while (true) {

			token = getNextToken(bis);
			if (token == null) {
				break;
			}

			if (token.equals("{")) {
				level += 1;
			} else if (token.equals("}")) {
				levels[level] = 0;
				level -= 1;
				// last case would otherwise crash on access
				if (level != -1) {
					levels[level] += 1;
				}
			} else {
				// add the number
				int11_37[levels[0]][levels[1]][levels[2]][levels[3]] = Integer.parseInt(token);
				levels[level] += 1;
			}
		}
	}

	public static int[][][][] int11_H = new int[EnergyConst.NBPAIRS+1][EnergyConst.NBPAIRS+1][5][5];
	static {

		// Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("RNAFold_int11_H.txt");
		BufferedInputStream bis = new BufferedInputStream(is);

		String token;
		int level = -1;
		int[] levels = { 0, 0, 0, 0 };
		while (true) {

			token = getNextToken(bis);
			if (token == null) {
				break;
			}

			if (token.equals("{")) {
				level += 1;
			} else if (token.equals("}")) {
				levels[level] = 0;
				level -= 1;
				// last case would otherwise crash on access
				if (level != -1) {
					levels[level] += 1;
				}
			} else {
				// add the number
				int11_H[levels[0]][levels[1]][levels[2]][levels[3]] = Integer.parseInt(token);
				levels[level] += 1;
			}
		}
	}

	public static int[][][][][] int21_37 = new int[EnergyConst.NBPAIRS+1][EnergyConst.NBPAIRS+1][5][5][5];
	static {

		// Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("RNAFold_int21_37.txt");
		BufferedInputStream bis = new BufferedInputStream(is);

		String token;
		int level = -1;
		int[] levels = { 0, 0, 0, 0, 0 };
		while (true) {

			token = getNextToken(bis);
			if (token == null) {
				break;
			}

			if (token.equals("{")) {
				level += 1;
			} else if (token.equals("}")) {
				levels[level] = 0;
				level -= 1;
				// last case would otherwise crash on access
				if (level != -1) {
					levels[level] += 1;
				}
			} else {
				// add the number
				int21_37[levels[0]][levels[1]][levels[2]][levels[3]][levels[4]] = Integer.parseInt(token);
				levels[level] += 1;
			}
		}
	}

	public static int[][][][][] int21_H = new int[EnergyConst.NBPAIRS+1][EnergyConst.NBPAIRS+1][5][5][5];
	static {

		// Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("RNAFold_int21_H.txt");
		BufferedInputStream bis = new BufferedInputStream(is);

		String token;
		int level = -1;
		int[] levels = { 0, 0, 0, 0, 0 };
		while (true) {

			token = getNextToken(bis);
			if (token == null) {
				break;
			}

			if (token.equals("{")) {
				level += 1;
			} else if (token.equals("}")) {
				levels[level] = 0;
				level -= 1;
				// last case would otherwise crash on access
				if (level != -1) {
					levels[level] += 1;
				}
			} else {
				// add the number
				int21_H[levels[0]][levels[1]][levels[2]][levels[3]][levels[4]] = Integer.parseInt(token);
				levels[level] += 1;
			}
		}
	}

	public static int[][][][][][] int22_37 = new int[EnergyConst.NBPAIRS+1][EnergyConst.NBPAIRS+1][5][5][5][5];
	static {

		// Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("RNAFold_int22_37.txt");
		BufferedInputStream bis = new BufferedInputStream(is);

		String token;
		int level = -1;
		int[] levels = { 0, 0, 0, 0, 0, 0 };
		while (true) {

			token = getNextToken(bis);
			if (token == null) {
				break;
			}

			if (token.equals("{")) {
				level += 1;
			} else if (token.equals("}")) {
				levels[level] = 0;
				level -= 1;
				// last case would otherwise crash on access
				if (level != -1) {
					levels[level] += 1;
				}
			} else {
				// add the number
				int22_37[levels[0]][levels[1]][levels[2]][levels[3]][levels[4]][levels[5]] = Integer.parseInt(token);
				levels[level] += 1;
			}
		}
	}

	public static int[][][][][][] int22_H = new int[EnergyConst.NBPAIRS+1][EnergyConst.NBPAIRS+1][5][5][5][5];
	static {

		// Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("RNAFold_int22_H.txt");
		BufferedInputStream bis = new BufferedInputStream(is);

		String token;
		int level = -1;
		int[] levels = { 0, 0, 0, 0, 0, 0 };
		while (true) {

			token = getNextToken(bis);
			if (token == null) {
				break;
			}

			if (token.equals("{")) {
				level += 1;
			} else if (token.equals("}")) {
				levels[level] = 0;
				level -= 1;
				// last case would otherwise crash on access
				if (level != -1) {
					levels[level] += 1;
				}
			} else {
				// add the number
				int22_H[levels[0]][levels[1]][levels[2]][levels[3]][levels[4]][levels[5]] = Integer.parseInt(token);
				levels[level] += 1;
			}
		}
	}

	/**
	 * Provided with an inputstream, this function returns the next valid token as a
	 * string. A valid token here is either a positive or negative integer, { or }.
	 * All other elements in the file are ignored.
	 * 
	 * @param bis
	 *            the stream to read from
	 * @return null if eof
	 */
	private static String getNextToken(BufferedInputStream bis) {

		try {
			while (bis.available() > 0) {

				// read the first char
				char c = (char) bis.read();

				// chase for changing dimension
				if (c == '{' || c == '}') {
					return c + "";
				}

				// do we have a negative number?
				boolean negative = false;
				if (c == '-') {
					negative = true;
					c = (char) bis.read();
				}

				// case for positive number
				String n = "";
				if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7'
						|| c == '8' || c == '9') {
					boolean scanning = true;
					char lc;
					n = c + "";
					while (scanning) {

						// we need to peek ahead.
						bis.mark(1);
						lc = (char) bis.read();
						bis.reset();

						// is this still part of the number?
						if (lc == '0' || lc == '1' || lc == '2' || lc == '3' || lc == '4' || lc == '5' || lc == '6'
								|| lc == '7' || lc == '8' || lc == '9') {
							n += lc;
							bis.read();
						} else { // if not, we are done here
							scanning = false;
						}
					}
					if (negative) {
						return "-" + n;
					} else {
						return n;
					}
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// end of file case
		return null;
	}

}
