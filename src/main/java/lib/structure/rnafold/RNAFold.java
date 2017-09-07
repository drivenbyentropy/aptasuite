package lib.structure.rnafold;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

import org.apache.commons.lang.NotImplementedException;

/**
 * @author Jan Hoinka
 *
 * RNAFold4j is a port of RNAFold to Java. RNAFold is implemented and developed by Ivo L Hofacker et al.
 * as part of the ViennaRNA package v 1.8.5. All intellectual credits of this work 
 * go to the original authors and the Institute for Theoretical Chemistry of the 
 * University of Vienna. My only contribution is the adaptation of the C source code to Java.
 * 
 */
public class RNAFold {

	static String scale1 = "....,....1....,....2....,....3....,....4";
	static String scale2 = "....,....5....,....6....,....7....,....8";

	static FoldVars fold_vars = new FoldVars();
	static PairMat pair_mat = new PairMat(fold_vars);
	static Params params = new Params(fold_vars);
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int argc = args.length;
		byte[] string;
		String line;
		byte[] structure = null, cstruc = null;
		String fname = "sequence";
		String ns_bases = null, c;
		int i, length = 0, l, sym, r;
		double energy, min_en;
		double kT, sfact = 1.07;
		int pf = 0;
		int noconv = 0;
		int circ = 0;
		Path outputPath = Paths.get(System.getProperty("user.dir"));

		fold_vars.do_backtrack = 1;
		string = null;
		for (i = 0; i < args.length; i++) {
			if (args[i].charAt(0) == '-')
				switch (args[i].charAt(1)) {
				case 'T':
					if (args[i].charAt(2) != '\0') {
						usage();
						System.exit(1);
					}
					if (i == argc - 1) {
						usage();
						System.exit(1);
					}
					try {
						fold_vars.temperature = Double.parseDouble(args[++i]);
					} catch (NumberFormatException e) {
						usage();
						System.exit(1);
					}
					break;
				case 'p':
					pf = 1;
					if (args[i].length() > 2)
						fold_vars.do_backtrack = Integer.parseInt(args[i].substring(2));
					break;
				case 'n':
					if (args[i].equals("-noGU"))
						fold_vars.noGU = true;
					if (args[i].equals("-noCloseGU"))
						fold_vars.no_closingGU = true;
					if (args[i].equals("-noLP"))
						fold_vars.noLonelyPairs = true;
					if (args[i].equals("-nsp")) {
						if (i == argc - 1) {
							System.exit(1);
							usage();
						}
						ns_bases = args[++i];
					}
					if (args[i].equals("-noconv"))
						noconv = 1;
					break;
				case '4':
					fold_vars.tetra_loop = false;
					break;
				case 'e':
					if (i == argc - 1) {
						usage();
						System.exit(1);
					}
					try {
						fold_vars.energy_set = Integer.parseInt(args[++i]);
					} catch (NumberFormatException e) {
						usage();
						System.exit(1);
					}
					break;
				case 'C':
					fold_vars.fold_constrained = true;
					break;
				case 'c':
					if (args[i].equals("-circ"))
						circ = 1;
					break;
				case 'S':
					if (i == argc - 1) {
						usage();
						System.exit(1);
					}
					try {
						sfact = Double.parseDouble(args[++i]);
					} catch (NumberFormatException e) {
						usage();
						System.exit(1);
					}
					break;
				case 'd':
					fold_vars.dangles = 0;
					if (args[i].length() > 1) {
						try {
							fold_vars.dangles = Integer.parseInt(args[i].substring(1));
						} catch (NumberFormatException e) {
							usage();
							System.exit(1);
						}
					}
					break;
				case 'P':
					throw new NotImplementedException("ParameterFile is currently not implemented.");
				case 'M':
					throw new NotImplementedException("MEA is currently not imlpemented.");
				case 'o':
					if (i == argc - 1) {
						usage();
						System.exit(1);
					}
					outputPath = Paths.get(args[++i]);
					break;
				case 'h':
					usage();
					System.exit(1);
					break;
				default:
					usage();
					System.exit(1);
				}
		}

		if ((circ != 0) && fold_vars.noLonelyPairs)
			System.out.println(
					"warning, depending on the origin of the circular sequence, some structures may be missed when using -noLP\nTry rotating your sequence a few times\n");

		if (ns_bases != null) {
			fold_vars.nonstandards = new byte[33];
			c = ns_bases;
			i = sym = 0;
			int c_indx = 0;
			if (c.charAt(c_indx) == '-') {
				sym = 1;
				c_indx++;
			}
			while (c_indx < c.length()) {
				if (c.charAt(c_indx) != ',') {
					fold_vars.nonstandards[i++] = (byte) c.charAt(c_indx++);
					fold_vars.nonstandards[i++] = (byte) c.charAt(c_indx);
					if ((sym != 0) && (c.charAt(c_indx) != c.charAt(c_indx - 1))) {
						fold_vars.nonstandards[i++] = (byte) c.charAt(c_indx);
						fold_vars.nonstandards[i++] = (byte) c.charAt(c_indx - 1);
					}
				}
				c_indx++;
			}
		}
		if (fold_vars.fold_constrained) {
			System.out.println("Input constraints using the following notation:\n" + "| : paired with another base\n"
					+ ". : no constraint at all\n" + "x : base must not pair\n"
					+ "< : base i is paired with a base j<i\n" + "> : base i is paired with a base j>i\n"
					+ "matching brackets ( ): base i pairs base j\n");
		}

		// Prepare loop instances
		Scanner scanner = new Scanner(System.in);
		Fold fold = new Fold(fold_vars, pair_mat, params);
		PartFunc part_func = new PartFunc(fold_vars, pair_mat);

		do { /* main loop: continue until end of file */

			System.out.println("\nInput string (upper or lower case); @ to quit");
			System.out.println(String.format("%s%n", scale1, scale2));
			line = scanner.next();

			/* skip comment lines and get filenames */
			while ((line.charAt(0) == '*') || (line.charAt(0) == '\0') || (line.charAt(0) == '>')) {
				if (line.charAt(0) == '>')
					fname = line;
				System.out.println(String.format("%s\n", line));
				line = scanner.next();
			}

			if ((line == null) || line.equals("@"))
				break;

			// this is the sequence
			string = line.toUpperCase().getBytes();
			line = null;
			length = string.length;

			// structure contstains
			structure = new byte[length];
			if (fold_vars.fold_constrained) {
				cstruc = scanner.next().getBytes();
				if (cstruc.length > 0)
					System.arraycopy(cstruc, 0, structure, 0, length);
				else
					System.out.println("constraints missing\n");
			}
			for (l = 0; l < length; l++) {
				if (noconv == 0 && string[l] == 'T')
					string[l] = 'U';
			}
			System.out.println(String.format("length = %d\n", length));

			/* initialize_fold(length); */
			if (circ != 0)
				min_en = fold.circfold(string, structure);
			else
				min_en = fold.fold(string, structure);

			System.out.print(String.format("%s\n%s", new String(string), new String(structure)));
			System.out.println(String.format(" (%6.2f)", min_en));
			System.out.flush();

			if (length > 2000)
				fold.free_arrays();
			if (pf != 0) {
				byte[] pf_struc = new byte[length + 1];
				if (fold_vars.dangles == 1) {
					fold_vars.dangles = 2; /* recompute with dangles as in pf_fold() */
					min_en = (circ != 0) ? fold.energy_of_circ_struct(string, structure)
							: fold.energy_of_struct(string, structure);
					fold_vars.dangles = 1;
				}

				kT = (fold_vars.temperature + 273.15) * 1.98717 / 1000.; /* in Kcal */
				fold_vars.pf_scale = Math.exp(-(sfact * min_en) / kT / length);
				if (length > 2000)
					System.out.println(String.format("scaling factor %f", fold_vars.pf_scale));

				if (circ != 0) {
					part_func.init_pf_circ_fold(length);
				} else {
					part_func.init_pf_fold(length);
				}

				if (cstruc != null)
					pf_struc = Arrays.copyOf(cstruc, length + 1);
				energy = (circ != 0) ? part_func.pf_circ_fold(string, pf_struc) : part_func.pf_fold(string, pf_struc);

				if (fold_vars.do_backtrack != 0) {
					System.out.print(String.format("%s ", new String(pf_struc)));
					System.out.println(String.format("[%6.2f]", energy));
				}
				if (fold_vars.do_backtrack == 0)
					System.out.println(String.format(" free energy of ensemble = %6.2f kcal/mol", energy));
				if (fold_vars.do_backtrack != 0) {
					PList[] pl1, pl2;
					byte[] cent;
					double cent_en, dist = 0;
					CentroidData centroid = part_func.centroid(length);

					cent = centroid.structure;
					dist = centroid.distance;

					cent_en = (circ != 0) ? fold.energy_of_circ_struct(string, cent)
							: fold.energy_of_struct(string, cent);
					System.out.println(String.format("%s {%6.2f d=%.2f}", new String(cent), cent_en, dist));
					cent = null;
					pl1 = make_plist(length, 1e-15);
					writePListToFile(pl1, String.format("%s_bpp.txt", fname), outputPath);
					pl2 = null;
					if (fold_vars.do_backtrack == 2) {
						pl2 = part_func.stackProb(1e-15);
						writePListToFile(pl2, String.format("%s_sbpp.txt", fname), outputPath);
						pl2 = null;
					}
					pl1 = null;
					pf_struc = null;
				}
				System.out.print(String.format(" frequency of mfe structure in ensemble %g; ",
						Math.exp((energy - min_en) / kT)));
				if (fold_vars.do_backtrack != 0)
					System.out.println(String.format("ensemble diversity %-6.2f", part_func.mean_bp_dist(length)));

				System.out.println();

				part_func.free_pf_arrays();

			}
			if (cstruc != null)
				cstruc = null;
			System.out.flush();
			string = null;
			structure = null;

		} while (true); // TODO: set to true once debugged
		if (length <= 2000)
			fold.free_arrays();

		scanner.close();
		fold = null;
		part_func = null;

	}

	static PList[] b2plist(byte[] struc) {
		/* convert bracket string to plist */
		short[] pt;
		PList[] pl;
		int i, k = 0;
		pt = Utils.make_pair_table(struc);
		pl = new PList[struc.length / 2];
		for (i = 1; i < struc.length; i++) {
			if (pt[i] > i) {
				pl[k].i = i;
				pl[k].j = pt[i];
				pl[k++].p = 0.95 * 0.95;
			}
		}
		pt = null;
		pl[k].i = 0;
		pl[k].j = 0;
		pl[k++].p = 0.;
		return pl;
	}

	static PList[] make_plist(int length, double pmin) {
		/* convert matrix of pair probs to plist */
		int i, j, k = 0, maxl;
		maxl = 2 * length;
		PList[] pl = new PList[maxl];
		for (int x = 0; x < pl.length; pl[x++] = new PList())
			;
		k = 0;
		for (i = 1; i < length; i++)
			for (j = i + 1; j <= length; j++) {
				if (fold_vars.pr[fold_vars.iindx[i] - j] < pmin)
					continue;
				if (k >= maxl - 1) {
					maxl *= 2;
					pl = Arrays.copyOf(pl, maxl);
					for (int x = (maxl / 2); x < pl.length; pl[x++] = new PList())
						;
				}
				pl[k].i = i;
				pl[k].j = j;
				pl[k++].p = fold_vars.pr[fold_vars.iindx[i] - j];
			}
		pl[k].i = 0;
		pl[k].j = 0;
		pl[k++].p = 0.;

		return pl;
	}

	static void writePListToFile(PList[] pl, String filename, Path outfolder) {

		BufferedWriter writer;
		try {

			writer = new BufferedWriter(new FileWriter(Paths.get(outfolder.toString(), filename).toFile()));

			for (PList pli : pl) {
				if (pli.i == 0)
					break;
				writer.write(String.format("%d %d %f\n", pli.i, pli.j, pli.p));
			}

			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static void usage() {
		System.out.println("usage:\n" + "java -jar rnafold4j [-p[0|2]] [-C] [-T temp] [-4] [-d[2|3]] [-noGU] [-noCloseGU]\n"
				+ "        [-noLP] [-e e_set] [-nsp pairs] [-S scale]\n"
				+ "        [-noconv] [-circ] [-o outputFolder]\n");
	}

}
