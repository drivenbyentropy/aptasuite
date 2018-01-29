package lib.aptatrace;

import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.logging.Level;

import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.Experiment;
import utilities.AptaLogger;

//import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.itextpdf.text.pdf.parser.clipper.Paths;

import gui.aptatrace.logo.Logo;
import gui.aptatrace.logo.LogoSummary;
import gui.aptatrace.logo.LogoSummary2;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lib.aptamer.datastructures.SelectionCycle;
import utilities.Configuration;
import utilities.Quicksort;

/**
 * @author Phoung Dao, modifications by Jan Hoinka
 *
 */
public class AptaTraceMotif {

	private Experiment experiment = null;

	private static char[] nu = { 'A', 'G', 'T', 'C' };
	private static int[] fourToPower = { 1, 4, 16, 64, 256, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216 };

	private double theta = 10.0;

	private String outputPrefix = "motif";

	private String outputPath;

	private int klength;

	private boolean filterClusters;

	private boolean outputClusters;

	private int singletonThres;

	private File resultDirectory = null;
	
	public AptaTraceMotif(Experiment exp, String outputPath, int klength, boolean filterClusters,
			boolean outputClusters, int singletonThres) {

		// We need to be able to create graphical instances in pure console mode
		System.setProperty("java.awt.headless", "true");

		this.experiment = exp;
		this.outputPath = outputPath;
		this.klength = klength;
		this.filterClusters = filterClusters;
		this.outputClusters = outputClusters;
		this.singletonThres = singletonThres;

	}

	/**
	 * Deletes a directory, possibly containing files and subfolders, from the
	 * medium.
	 * 
	 * @param dir
	 *            The directory to delete
	 */
	private static void removeDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File aFile : files) {
					removeDirectory(aFile);
				}
			}
			dir.delete();
		} else {
			dir.delete();
		}
	}

	/**
	 * Removes all files and subfolders from a directory without deleting the folder
	 * itself.
	 * 
	 * @param dir
	 *            The directory to clean.
	 */
	private static void cleanDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File aFile : files) {
					removeDirectory(aFile);
				}
			}
		}
	}

	/**
	 * Return an id of a character
	 * 
	 * @param c
	 *            the character
	 * @return the correpsonding id
	 */
	private static int getNuId(char c) {
		switch (c) {
		case 'A':
			return 0;
		case 'G':
			return 1;
		case 'T':
			return 2;
		case 'U':
			return 2;
		case 'C':
			return 3;
		default:
			return -1;
		}
	}

	/**
	 * return an id of a character provided character byte code
	 * 
	 * @param c
	 *            the character
	 * @return the correpsonding id
	 */
	private static int getNuId(byte c) {
		switch (c) {
		case 65:
			return 0;
		case 71:
			return 1;
		case 84:
			return 2;
		case 85:
			return 2;
		case 67:
			return 3;
		default:
			return -1;
		}
	}

	/**
	 * @param alignment
	 * @param aptamer
	 * @param pos
	 * @return
	 */
	private String fillBlanks(String alignment, String aptamer, int pos) {
		String a = "";
		int firstPos = 0;

		for (int i = 0; i < alignment.length(); i++)
			if (alignment.charAt(i) != '-') {
				firstPos = i;
				break;
			}

		for (int i = 0; i < alignment.length(); i++)
			if (alignment.charAt(i) != '-')
				a = a + alignment.charAt(i);
			else {
				a = a + aptamer.charAt(pos - (firstPos - i));
			}

		return a;
	}

	/**
	 * A procedure to calculate the id of a kmer when it is overlapping with its
	 * left kmer when sliding character by character of when sliding from left to
	 * right of an aptamer sequence from left to right
	 * 
	 * @param oldId
	 *            the id of left kmer
	 * @param lastNu
	 *            the leftmost character of the left kmer
	 * @param newNu
	 *            the rightmost character of the right kmer
	 * @param klen
	 *            length of the kmer
	 * @return id of the right kmer
	 */
	private static int calulateNewId(int oldId, char lastNu, char newNu, int klen) {
		return (4 * (oldId - getNuId(lastNu) * fourToPower[klen - 1]) + getNuId(newNu));
	}

	/**
	 * return the id of a given k-mer, the id will be the index of the given k-mer
	 * in kmersArr
	 * 
	 * @param kmer
	 * @return
	 */
	private static int calculateId(String kmer) {
		int id = 0;
		for (int i = 0; i < kmer.length(); i++)
			id += getNuId(kmer.charAt(i)) * fourToPower[kmer.length() - i - 1];
		return id;
	}

	/**
	 * A recursive procedure to generate all possible number of k-mers given the
	 * length klength
	 * 
	 * @param k
	 * @param current
	 * @param arr
	 * @param klength
	 */
	private void generateAllKmers(int k, String current, ArrayList<String> arr, int klength) {
		if (k == klength)
			arr.add(current);
		else {
			for (int i = 0; i < 4; i++) {
				String next = current + nu[i];
				generateAllKmers(k + 1, next, arr, klength);
			}
		}
	}

	/**
	 * Pair alignment of two kmers a and b
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private String[] pairAlignment(String a, String b) {
		String savea = "";
		String saveb = "";
		String ret[] = new String[2];

		String tmpa;
		String tmpb;

		int mS = 2;
		int nR;
		int nI;
		int sC = a.length() + 1;
		int bcs = 0;
		for (int i = -mS; i <= mS; i++) {
			tmpa = a;
			tmpb = b;
			nR = 0;
			nI = 0;
			if (i < 0) {
				for (int j = 1; j <= -i; j++) {
					tmpa = "-" + tmpa;
					tmpb = tmpb + "-";
				}

			} else if (i > 0) {
				for (int j = 1; j <= i; j++) {
					tmpa = tmpa + "-";
					tmpb = "-" + tmpb;
				}
			}

			int nc = 0;

			for (int j = 0; j < tmpa.length(); j++)
				if ((tmpa.charAt(j) == '-') || (tmpb.charAt(j) == '-')) {
					nI++;
					nc = 0;
				} else if (tmpa.charAt(j) != tmpb.charAt(j)) {
					nR++;
					nc = 0;
				} else {
					nc += 1;
					if (nc > bcs)
						bcs = nc;
				}

			if (((nI + nR) <= sC) && (nI <= 4)) {
				sC = nI + nR;
				savea = tmpa;
				saveb = tmpb;
			}
		}

		ret[0] = savea;
		ret[1] = saveb;

		return ret;
	}

	/**
	 * To compute alignment of all the kmers in a give cluster of kmers stored in
	 * sArr
	 * 
	 * @param sArr
	 * @return
	 */
	private String[] multipleAlignment(ArrayList<String> sArr) {
		String seed = sArr.get(0);
		String[] a = new String[sArr.size()];

		String seedA = seed; // seed alignment
		String[] result;
		int[] l = new int[sArr.size()]; // number of left gaps
		int[] r = new int[sArr.size()]; // number of right gaps
		int cl;
		int cr;
		int ml = 0; // max gaps on the left
		int mr = 0; // max gaps on the right

		a[0] = seed;
		l[0] = 0;
		r[0] = 0;
		for (int i = 1; i < sArr.size(); i++) {
			result = pairAlignment(sArr.get(0), sArr.get(i));
			seedA = result[0];
			a[i] = result[1];
			cl = -1;
			while (seedA.charAt(cl + 1) == '-')
				cl++;

			l[i] = cl + 1;

			cr = seedA.length();
			while (seedA.charAt(cr - 1) == '-')
				cr--;

			r[i] = a[i].length() - cr;

			if (r[i] > mr)
				mr = r[i];

			if (l[i] > ml)
				ml = l[i];
		}

		for (int i = 0; i < sArr.size(); i++) {
			for (int k = l[i] + 1; k <= ml; k++)
				a[i] = "-" + a[i];

			for (int k = r[i] + 1; k <= mr; k++)
				a[i] = a[i] + "-";
		}

		return a;
	}

	/**
	 * Determine whether two kmers has good overlap to put in the same cluster or
	 * not
	 * 
	 * @param a
	 *            the first kmer
	 * @param b
	 *            the second kmer
	 * @return true if they have good overlap
	 */
	private boolean hasGoodOverlap(String a, String b) {
		String tmpa;
		String tmpb;

		int mS = 2;
		int nR;
		int nI;
		int sC = a.length() + 1;
		int sI = sC;
		int sR = sC;

		int bcs = 0; // save the longest substring given an alignment of the
						// kmer a and b
		int scs = 0; // save the longest common substring of all the alignments
						// when sliding kmer a from left to right of kmer b

		// sliding the kmer a from left to right of kmer b
		for (int i = -mS; i <= mS; i++) {
			tmpa = a;
			tmpb = b;
			nR = 0;
			nI = 0;
			if (i < 0) {
				for (int j = 1; j <= -i; j++) {
					tmpa = "-" + tmpa;
					tmpb = tmpb + "-";
				}

			} else if (i > 0) {
				for (int j = 1; j <= i; j++) {
					tmpa = tmpa + "-";
					tmpb = "-" + tmpb;
				}
			}

			int nc = 0;

			for (int j = 0; j < tmpa.length(); j++)
				if ((tmpa.charAt(j) == '-') || (tmpb.charAt(j) == '-')) {
					nI++;
					nc = 0;
				} else if (tmpa.charAt(j) != tmpb.charAt(j)) {
					nR++;
					nc = 0;
				} else {
					nc += 1;
					if (nc > bcs)
						bcs = nc;
				}

			if ((nI + nR) <= sC) {
				sC = nI + nR;
				sI = nI;
				sR = nR;
				scs = bcs;
			}
		}

		if (sI == 0) {
			if (sR == 1)
				return true;
		} else {
			if ((a.length() <= 6) && (scs >= 4))
				return true;
			else if ((a.length() >= 7) && (scs >= 5))
				return true;
		}

		return false;
	}

	/**
	 * Performs the main computation
	 */
	public void run() {
		long startTime, endTime, rms;
		int rmh, rmm;

		// get a list of selection cycles with data
		ArrayList<SelectionCycle> cycles = new ArrayList<SelectionCycle>();
		ArrayList<String> roundArr = new ArrayList<String>();
		for (SelectionCycle sc : Configuration.getExperiment().getSelectionCycles()) {
			if (sc != null) {
				cycles.add(sc);
				roundArr.add(sc.getName());
			}
		}
		
		int[] rc = new int[roundArr.size()];
		int numR = roundArr.size();
		HashMap<String, Integer> round2Id = new HashMap<String, Integer>();
		final int numOfContexts = 6;
		for (int i = 0; i < roundArr.size(); i++) {
			round2Id.put(roundArr.get(i), i);
			rc[i] = 0;
		}

//		new LogoSummary();
		LogoSummary2 summary2 = new LogoSummary2();

		String resultFolder = "k" + klength + "alpha" + singletonThres;

		for (int i = 0; i < roundArr.size(); i++) {
			roundArr.get(i);
		}

		String[] roundIDs = roundArr.toArray(new String[roundArr.size()]);

		// to generate all possible number of kmers
		int numk = 0;
		final ArrayList<String> kmersArr = new ArrayList<String>();
		generateAllKmers(0, "", kmersArr, klength);
		numk = kmersArr.size();

		final double kCountPerR[][] = new double[kmersArr.size()][];
		for (int i = 0; i < kmersArr.size(); i++) {
			kCountPerR[i] = new double[roundArr.size()];
			for (int j = 0; j < roundArr.size(); j++)
				kCountPerR[i][j] = 0.0f;
		}

		KContextTrace[] mkc = new KContextTrace[numk];
		;
		for (int i = 0; i < numk; i++) {
			mkc[i] = new KContextTrace(kmersArr.get(i), numR, numOfContexts - 1);
		}

		System.out.println();
		AptaLogger.log(Level.INFO, this.getClass(), "Reading secondary structures of aptamers from database...");

		int lastRoundCount = 0;
		String aptamer = null;

		int differentLength = 0;

		// iterate through the secondary structure profiles of all the aptamers
		// to calculate the context shifting scores of the kmers
		try {
			int numOR;
			int[] occRArr = new int[roundArr.size()];
			int[] occCArr = new int[roundArr.size()];
			int cardinality;
			int id = 0;
			int startPos;
			int endPos;
			int rid;
			int aptamerLen;
			int aptamerId;
			double[] contextLongArr;
			double[][] contextProbArr = new double[5][];
			double[] avgContextProbArr = new double[5];
			boolean firstRead = true;
			IntOpenHashSet seen = new IntOpenHashSet();

			int poolSize = experiment.getAptamerPool().size();
			int numDone = 0;
			int[] bounds_array;
			
			Iterator<Entry<Integer, int[]>> bounds_it = experiment.getAptamerPool().bounds_iterator().iterator();
			
			startTime = System.nanoTime();
			for (Entry<Integer,byte[]> aptamerArr : experiment.getAptamerPool().inverse_view_iterator()) {
				seen.clear();
				aptamer = new String(aptamerArr.getValue());
				aptamerLen = aptamer.length();
				aptamerId = aptamerArr.getKey();
				//AptamerBounds aptamerBounds = experiment.getAptamerPool().getAptamerBounds(aptamerId);
				bounds_array = bounds_it.next().getValue();
				
				numOR = 0;
				rid = 0;
				for (SelectionCycle sc : cycles) {
					
					cardinality = sc.getAptamerCardinality(aptamerId);
					if (cardinality > 0) {
						occCArr[numOR] = cardinality;
						occRArr[numOR] = rid;
						numOR++;
					}
					rid++;
				}

				for (int r = 0; r < numOR; r++) {
					rc[occRArr[r]] += occCArr[r];
					if (occRArr[r] == roundArr.size() - 1)
						lastRoundCount += occCArr[r];
				}

				if (firstRead) {
					for (int j = 0; j < 5; j++)
						contextProbArr[j] = new double[aptamerLen];
				}

				if (contextProbArr[0].length != aptamerLen) {
					differentLength += 1;

					if (contextProbArr[0].length < aptamerLen) {
						for (int j = 0; j < 5; j++)
							contextProbArr[j] = new double[aptamerLen];
					}

					if (differentLength == 1) {
						System.out.println();
						AptaLogger.log(Level.CONFIG, this.getClass(), "Warning: aptamers have different lengths!\n");
					}
				}

				contextLongArr = experiment.getStructurePool().getStructure(aptamerId);
				for (int j = 0; j < 5; j++) {

					if (aptamerLen != ((int) (contextLongArr.length / 5.0))) {
						throw new Exception("The profile array length is not the same as the aptamer length!!!");
					}

					contextProbArr[j][0] = contextLongArr[j * aptamerLen];

					for (int k = 1; k < aptamerLen; k++) {
						contextProbArr[j][k] = contextLongArr[j * aptamerLen + k] + contextProbArr[j][k - 1];
					}
				}

				// startPos = (klength + fivePrime.length() - 1);
				startPos = bounds_array[0] + klength - 1;
				endPos = bounds_array[1];

				// iterate through every kmer of the aptamer under consideration
				// and sum up its number of occurrences and the sums of the
				// probabilities of being in various structural context
				for (int k = startPos; k < endPos; k++) {
					if (k == startPos)
						id = calculateId(aptamer.substring(k - klength + 1, k + 1));
					else
						id = calulateNewId(id, aptamer.charAt(k - klength), aptamer.charAt(k), klength);

					for (int j = 0; j < 5; j++)
						avgContextProbArr[j] = (contextProbArr[j][k] - contextProbArr[j][k - klength + 1])
								/ (klength * 1.0f);

					if (!seen.contains(id)) {
						seen.add(id);
						for (int l = 0; l < numOR; l++) {
							mkc[id].addTotalCount(occRArr[l], occCArr[l]);
						}
					}
					for (int l = 0; l < numOR; l++) {
						if (singletonThres > 0) {
							if (occCArr[l] > singletonThres)
								mkc[id].addContextProb(occRArr[l], occCArr[l], avgContextProbArr);
							if (occCArr[l] <= singletonThres)
								mkc[id].addSingletonContextProb(occRArr[l], occCArr[l], avgContextProbArr);
						} else
							mkc[id].addSingletonContextProb(occRArr[l], occCArr[l], avgContextProbArr);
					}
				}

				firstRead = false;
				numDone++;
				if ((numDone == poolSize) || (numDone % 10000 == 0)) {
					endTime = System.nanoTime();
					rms = (long) (((endTime - startTime) / (numDone * 1000000000.0)) * (poolSize - numDone));
					rmh = (int) (rms / 3600.0);
					rmm = (int) ((rms % 3600) / 60);
					
					String info = "Finished reading " + numDone + "/" + poolSize + " structures, ETA "
							+ String.format("%02d:%02d:%02d", rmh, rmm, rms % 60); 
					
					System.out.print(info + "\r");
					
					
					AptaLogger.log(Level.FINEST, this.getClass(), info + "\r");
				}
			}
		} catch (Exception e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error in reading aptamer " + aptamer);
			AptaLogger.log(Level.SEVERE, this.getClass(),
					org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
			e.printStackTrace();
			System.exit(1);
		}

		
		try {
			// create the result folder if it does not exist
			 java.nio.file.Path resultPath = java.nio.file.Paths.get(outputPath, "export", "aptatrace"); 
					
					//new File(outputPath + "/export/");
			if (! resultPath.toFile().exists() ) {
				//resultPath.mkdir();
				 java.nio.file.Files.createDirectories(resultPath);
			}

			// create the specific folder for this run
			resultDirectory = java.nio.file.Paths.get(resultPath.toString(), resultFolder).toFile();
			if (!resultDirectory.exists()) {
				resultDirectory.mkdir();
			}
			// otherwise remove any previous content
			else {
				cleanDirectory(resultDirectory);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = numk - 1; i >= 0; i--) {
			mkc[i].setStrongPresence(lastRoundCount);
			mkc[i].checkEnoughOccurrences();
			if (mkc[i].hasEnoughOccurrences()) {
				mkc[i].normalizeProfile();
				mkc[i].deriveSelectionContext();
				mkc[i].calculateSingletonKLScore(rc);
				mkc[i].calculateKLScore(rc);
			}
		}

		// class Pair<F, S>
		class Pair<F extends Comparable<F>, S extends Comparable<S>> implements Comparable<Pair<F, S>> {
			private F first;
			private S second;

			public Pair(F first, S second) {
				this.first = first;
				this.second = second;
			}

			public void setFirst(F first) {
				this.first = first;
			}

			public void setSecond(S second) {
				this.second = second;
			}

			public F getFirst() {
				return first;
			}

			public S getSecond() {
				return second;
			}

			public int compareTo(Pair<F, S> pair) {
				int result = second.compareTo(pair.getSecond());
				return result;
			}
		}

		ArrayList<Pair<Integer, Integer>> lastRoundPool = new ArrayList<Pair<Integer, Integer>>();

		// the arraylist storing background context shifting scores for aptamers with
		// singleton occurrences
		ArrayList<Double> singletonKLScores = new ArrayList<Double>();

		// the arraylist storing context shifting scores for aptamers with non-singleton
		// occurrences
		ArrayList<Double> kmerKLScores = new ArrayList<Double>();

		// the arraylist storing the proprotions of kmer occurrences in the final
		// selection round
		ArrayList<Double> proportionAL = new ArrayList<Double>();

		for (int i = 0; i < mkc.length; i++)
			if ((mkc[i].hasEnoughOccurrences())
					&& (!((mkc[i].getSingletonKLScore() < 0.000001) || (mkc[i].getKLScore() < 0.000001)))) {
				singletonKLScores.add(Math.log((double) mkc[i].getSingletonKLScore()));
				kmerKLScores.add(Math.log((double) mkc[i].getKLScore()));
				proportionAL.add(mkc[i].getProportion());
			}

		// If, after this, no scores are present in the list, we finish with no results.
		if (kmerKLScores.size() == 0) {
			
			AptaLogger.log(Level.INFO, this.getClass(), "Could not find enough aptamers above alpha to continue with motif elucidation.");
			return;
			
		}
		
		// the array storing background context shifting scores for aptamers with
		// singleton occurrences
		double singletonKLScoreArr[] = new double[singletonKLScores.size()];

		// the array storing context shifting scores for aptamers with non-singleton
		// occurrences
		double kmerKLScoreArr[] = new double[kmerKLScores.size()];

		// the sorted array storing context shifting scores for aptamers with
		// non-singleton occurrences
		double sortedKLScoreArr[] = new double[kmerKLScores.size()];

		// the arraylist storing the proportions of kmer occurrences in the final
		// selection round
		double sortedProportionArr[] = new double[kmerKLScores.size()];

		for (int i = 0; i < singletonKLScores.size(); i++)
			singletonKLScoreArr[i] = singletonKLScores.get(i);

		for (int i = 0; i < kmerKLScores.size(); i++) {
			kmerKLScoreArr[i] = kmerKLScores.get(i);
			sortedKLScoreArr[i] = kmerKLScores.get(i);
			sortedProportionArr[i] = proportionAL.get(i);
		}
		Arrays.sort(sortedKLScoreArr);
		Arrays.sort(sortedProportionArr);

		DescriptiveStatistics ds = new DescriptiveStatistics(singletonKLScoreArr);
		double sMean = ds.getMean();
		double sStd = ds.getStandardDeviation();
		
		// in the case we have many significant context shifting scores just
		// take at most top 10 percent of the scores
		// and topThetaValue is the 90 quantile of all the context shifting
		// scores
		System.out.println(sortedKLScoreArr.length);
		System.out.println((int) Math.floor(((100.0 - theta) / 100.0) * sortedKLScoreArr.length));
		
		double topThetaValue = sortedKLScoreArr[(int) Math.floor(((100.0 - theta) / 100.0) * sortedKLScoreArr.length)];

		double topThetaProportion = sortedProportionArr[(int) Math
				.floor(((100.0 - theta) / 100.0) * sortedKLScoreArr.length)];

		ds = new DescriptiveStatistics(kmerKLScoreArr);
		
		sMean = ds.getMean();
		
		sStd = ds.getStandardDeviation();

		try {
			File out = new File(resultDirectory, "singletonKLScore.txt");
			PrintWriter writer = new PrintWriter(out, "UTF-8");
			for (int i = 0; i < mkc.length; i++) {
				if ((mkc[i].hasEnoughOccurrences()) && (mkc[i].getSingletonKLScore() > 0)) {
					writer.println(mkc[i].getKmer() + "\t" + Math.log(mkc[i].getSingletonKLScore()));
				}
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		double pvalue = 0.01;
		boolean hasSigKmer = false;

		for (int i = 0; i < numk; i++)
			if ((mkc[i].hasEnoughOccurrences()) && (mkc[i].isSignificant(singletonKLScoreArr, topThetaValue, pvalue)))
				hasSigKmer = true;

		if (!hasSigKmer)
			pvalue = 0.05;

		// to select the kmers with statistically significant context scores
		// in the case there are many such scores just take the ones within top
		// 10 percent only
		ArrayList<KContextTrace> sorted = new ArrayList<KContextTrace>();
		for (int i = 0; i < numk; i++)
			if (mkc[i].hasEnoughOccurrences())
				if ((mkc[i].isSignificant(singletonKLScoreArr, topThetaValue, pvalue))
						|| ((mkc[i].getProportion() >= topThetaProportion)
								&& (mkc[i].isSignificant(singletonKLScoreArr, -3.0, pvalue))
								&& (mkc[i].hasStrongPresence())))
					sorted.add(mkc[i]);

		Collections.sort(sorted);

		
		boolean[] got = new boolean[sorted.size()];
		for (int i = sorted.size() - 1; i >= 0; i--)
			got[i] = false;

		int totalSeeds = 0;
		try {
			File out = new File(resultDirectory, "KLScore.txt");
			PrintWriter writer = new PrintWriter(out, "UTF-8");
			for (int i = numk - 1; i >= 0; i--)
				if ((mkc[i].hasEnoughOccurrences()) && (mkc[i].getSingletonKLScore() > 0))
					writer.println(mkc[i].getKmer() + "\t" + Math.log(mkc[i].getKLScore()));
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		int numClus = 0;

		for (int i = sorted.size() - 1; i >= 0; i--)
			if (!got[i]) {
			}

		ArrayList<Pair<Integer, Double>> sortedClus = new ArrayList<Pair<Integer, Double>>();

		System.out.println();
		AptaLogger.log(Level.INFO, this.getClass(), "Clustering significant kmers...");
		
		// map a kmer id to a cluster id
		Int2IntOpenHashMap kmer2Clus = new Int2IntOpenHashMap(); 
		
		// the motif profiles of clusters of kmers
		ArrayList<MotifProfile> outputMotifs = new ArrayList<MotifProfile>(); 
		
		// the array storing the ids of the seeds of the clusters
		ArrayList<Integer> MotifSeedIDArr = new ArrayList<Integer>(); 

		// iterate the sorted list of kmers by their proportions starting with
		// the kmer with the highest proportion in the last selection round
		// pick the first one that is not chosen as the seed of a new cluster
		// then find similar kmers with less portion and put in the cluster
		for (int i = sorted.size() - 1; i >= 0; i--)
			if ((!got[i]) && (sorted.get(i).hasStrongPresence())) {
				ArrayList<String> clus = new ArrayList<String>();
				String clusAlignment[];
				double seedProportion = sorted.get(i).getProportion();
				double seedPValue = sorted.get(i).getPValue();

				numClus += 1;
				sortedClus.add(new Pair<Integer, Double>(numClus, seedPValue));
				kmer2Clus.put(calculateId(sorted.get(i).getKmer()), numClus - 1);

				got[i] = true;
				clus.add(sorted.get(i).getKmer());

				totalSeeds += 1;
				Math.abs((sorted.get(i).getKLScore() - sMean) / sStd);

				// pick similar kmers and add to the cluster
				for (int j = i - 1; j >= 0; j--)
					if ((!got[j]) && (hasGoodOverlap(sorted.get(i).getKmer(), sorted.get(j).getKmer()))
							&& (sorted.get(j).getSelectionContext() == sorted.get(i).getSelectionContext())) {

						clus.add(sorted.get(j).getKmer());
						kmer2Clus.put(calculateId(sorted.get(j).getKmer()), numClus - 1);
						got[j] = true;
					}

				clusAlignment = multipleAlignment(clus);

				MotifSeedIDArr.add(i);
				outputMotifs.add(new MotifProfile(clusAlignment[0].length(), numR));
				outputMotifs.get(numClus - 1).setSeed(sorted.get(i).getKmer());
				for (int j = 0; j < clus.size(); j++) {
					outputMotifs.get(numClus - 1).addKmer(sorted.get(j).getKmer());
				}

				try {
					File out = new File(resultDirectory,
							"motif_tmp_" + numClus + ".txt");
					PrintWriter writer = new PrintWriter(out, "UTF8");
					writer.format("%g\t%.2f%%\n", seedPValue, seedProportion * 100.0);
					
					//StringBuilder sb = new StringBuilder();
					for (int j = 0; j < clus.size(); j++) {
						outputMotifs.get(numClus - 1).addKmerAlignment(clus.get(j), clusAlignment[j]);
						writer.println(clus.get(j) + "\t" + clusAlignment[j]);
//						sb.append(clus.get(j) + "\t" + clusAlignment[j] + "\n");
					}
					
//					outputMotifs.get(numClus - 1).calculateProportion(rc);
//					writer.format("%g\t%.2f%%\t%.2f%%\n", seedPValue, seedProportion * 100.0, outputMotifs.get(numClus - 1).getProportion() * 100.00);
//					writer.print(sb.toString());
					
					writer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		AptaLogger.log(Level.INFO, this.getClass(), "There are " + totalSeeds + " seeds/clusters.");

		System.out.println();
		AptaLogger.log(Level.INFO, this.getClass(), "Compute context trace and PWM of each cluster...");

		// last round aptamer id with counts
		Int2IntOpenHashMap id2Count = new Int2IntOpenHashMap(); 

		int numOR;
		int[] occRArr = new int[roundArr.size()];
		int[] occCArr = new int[roundArr.size()];
		int cardinality;
		int id = 0;
		int startPos;
		int endPos;
		int rid;
		int aptamerLen;
		int aptamerId;
		double[] contextLongArr;
		double[][] contextProbArr = new double[5][];
		double[] avgContextProbArr = new double[5];
		boolean firstRead = true;
		IntOpenHashSet seen = new IntOpenHashSet();
		int mid;
		String kmer;

		int poolSize = experiment.getAptamerPool().size();
		int numDone = 0;
		int[] bounds_array;
		Iterator<Entry<Integer, int[]>> bounds_it = experiment.getAptamerPool().bounds_iterator().iterator();
		
		startTime = System.nanoTime();
		try {
			for (Entry<byte[], Integer> aptamerArr : experiment.getAptamerPool().iterator()) {
				seen.clear();
				aptamer = new String(aptamerArr.getKey());
				aptamerLen = aptamer.length();
				aptamerId = aptamerArr.getValue();
				//AptamerBounds aptamerBounds = experiment.getAptamerPool().getAptamerBounds(aptamerId);
				bounds_array = bounds_it.next().getValue();
				
				numOR = 0;
				rid = 0;
				for (SelectionCycle sc : cycles) {

					cardinality = sc.getAptamerCardinality(aptamerId);
					if (cardinality > 0) {
						occCArr[numOR] = cardinality;
						occRArr[numOR] = rid;
						numOR++;
					}
					rid++;
				}
				
				// we need to make sure that the aptamer in question is not specific to a negative round. 
				// in that case numOR would be 0 here and we can skip the rest of the calculation
				if (numOR == 0) {
					continue;
				}

				if (firstRead) {
					for (int j = 0; j < 5; j++)
						contextProbArr[j] = new double[aptamerLen];
				}

				if (contextProbArr[0].length != aptamerLen) {
					if (contextProbArr[0].length < aptamerLen) {
						for (int j = 0; j < 5; j++)
							contextProbArr[j] = new double[aptamerLen];
					}
				}

				contextLongArr = experiment.getStructurePool().getStructure(aptamerId);
				for (int j = 0; j < 5; j++) {

					contextProbArr[j][0] = contextLongArr[j * aptamerLen];
					for (int k = 1; k < aptamerLen; k++)
						contextProbArr[j][k] = contextLongArr[j * aptamerLen + k] + contextProbArr[j][k - 1];
				}

				startPos = bounds_array[0] + klength - 1;
				endPos = bounds_array[1];
 
				if (occRArr[numOR - 1] == roundArr.size() - 1)
					lastRoundPool.add(new Pair<Integer, Integer>(aptamerId, occCArr[numOR - 1]));

				// iterate through each kmer of the aptamer and decide whether the
				// kmer is in the list of kmers with significant context shifting
				// scores
				// if it is, summing the total number of occurrences and the sums of
				// probabilities of being in various structural context of its
				// motifs
				for (int k = startPos; k < endPos; k++) {
					kmer = aptamer.substring(k - klength + 1, k + 1);
					if (k == startPos)
						id = calculateId(kmer);
					else
						id = calulateNewId(id, aptamer.charAt(k - klength), aptamer.charAt(k), klength);

					if (kmer2Clus.containsKey(id)) {
						mid = kmer2Clus.get(id);

						if (occRArr[numOR - 1] == roundArr.size() - 1) {
							if (occCArr[numOR - 1] > singletonThres) {

								outputMotifs.get(mid).addToPWM(fillBlanks(outputMotifs.get(mid).getKmerAlignment(kmer),
										aptamer, k - klength + 1), occCArr[numOR - 1]);
							} else {
								outputMotifs.get(mid)
										.addToSingletonPWM(fillBlanks(outputMotifs.get(mid).getKmerAlignment(kmer),
												aptamer, k - klength + 1), occCArr[numOR - 1]);
							}
							id2Count.put(aptamerId, occCArr[numOR - 1]);
						}

						for (int j = 0; j < 5; j++)
							avgContextProbArr[j] = (contextProbArr[j][k] - contextProbArr[j][k - klength + 1])
									/ (klength * 1.0f);

						if (!seen.contains(mid)) {
							seen.add(mid);
							for (int l = 0; l < numOR; l++)
								outputMotifs.get(mid).addTotalCount(occRArr[l], occCArr[l]);
							if ((occRArr[numOR - 1] == roundArr.size() - 1) && (occCArr[numOR - 1] > singletonThres))
								outputMotifs.get(mid).addOccId(aptamerId, occCArr[numOR - 1]);
						}

						for (int l = 0; l < numOR; l++) {
							if ((occCArr[l] > singletonThres) || (occRArr[l] == 0)) {
								outputMotifs.get(mid).addContextProb(occRArr[l], occCArr[l], avgContextProbArr);
							}
							if (occCArr[l] <= singletonThres)
								outputMotifs.get(mid).addSingletonContextProb(occRArr[l], occCArr[l],
										avgContextProbArr);
						}
					}
				}

				numDone++;
				if ((numDone == poolSize) || (numDone % 10000 == 0)) {
					endTime = System.nanoTime();
					rms = (long) (((endTime - startTime) / (numDone * 1000000000.0)) * (poolSize - numDone));
					rmh = (int) (rms / 3600.0);
					rmm = (int) ((rms % 3600) / 60);
					String info = "Finished reading " + numDone + "/" + poolSize + " structures, ETA "
							+ String.format("%02d:%02d:%02d", rmh, rmm, rms % 60);
					System.out.print(info+"\r"); 
					
					AptaLogger.log(Level.FINEST, this.getClass(), info + "\r");
				}

				firstRead = false;
			}
		} catch (Exception e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
			System.exit(1);
		}

		boolean[] filtered = new boolean[outputMotifs.size()];
		for (int j = 0; j < filtered.length; j++)
			filtered[j] = false;

		if (filterClusters) {
			System.out.println();
			AptaLogger.log(Level.INFO, this.getClass(), "Filtering motifs:");

			IntOpenHashSet curOccSet = new IntOpenHashSet();
			// filters out the smaller motifs that their intersection with
			// larger motifs more than 2/3 of their sizes
			for (int j = 0; j < outputMotifs.size(); j++) {
				if (outputMotifs.get(j).getOverlapPercentage(curOccSet, id2Count) <= 0.67) {
					outputMotifs.get(j).addTo(curOccSet, id2Count);
					filtered[j] = false;
				} else
					filtered[j] = true;
			}
		}

		ArrayList<PrintWriter> clusterWriter = new ArrayList<PrintWriter>();
		HashMap<Integer, Integer> c2fc = new HashMap<Integer, Integer>();

		// prints out the PWM matrixes and aptamers that contain the motifs
		try {
			int numRM = 0;
			for (int j = 0; j < outputMotifs.size(); j++)
				if (!filtered[j]) {
					numRM++;
					outputMotifs.get(j).normalizeProfile();
					outputMotifs.get(j).calculateProportion(rc);
					File out = new File(resultDirectory, "motif_" + (numRM) + "_pwm.txt");
					PrintWriter writer = new PrintWriter(out, "UTF8");

					if (outputClusters) {
						c2fc.put(j, numRM - 1);
						File outlocal = new File(resultDirectory,
								"motif_" + (numRM) + "_aptamers.txt");
						clusterWriter.add(new PrintWriter(outlocal, "UTF8"));
					}

					outputMotifs.get(j).trim();
					outputMotifs.get(j).printPWM(writer);
					writer.close();

					double[][] pwm = outputMotifs.get(j).getPWM();
					String[] pid = new String[pwm.length];
					for (int k = 0; k < pid.length; k++)
						pid[k] = String.valueOf(k + 1);
					Logo seq = new Logo(pwm, pid);
					seq.setAlphabetNucleotides();
					seq.setAlphabetRibonucleotides();
					seq.setBit(true);
					File outPdf = new File(resultDirectory,
							"motif_" + (numRM) + "_pwm.pdf");
					seq.saveAsPDF(600, 400, outPdf.toString());

					double[][] traceMat = outputMotifs.get(j).getTraceMatrix();

					Logo trace = new Logo(traceMat, roundIDs);
					trace.setAlphabetContexts();
					trace.setBit(false);
					outPdf = new File(resultDirectory,
							"motif_" + (numRM) + "_context.pdf");
					trace.saveAsPDF(600, 400, outPdf.toString());

					File outTxt = new File(resultDirectory,
							"motif_" + (numRM) + "_context.txt");
					writer = new PrintWriter(outTxt.toString(), "UTF8");
					outputMotifs.get(j).printContextTrace(writer, roundArr);
					writer.close();

					File file1 = new File(resultDirectory,
							"motif_tmp_" + (j + 1) + ".txt");
					File file2 = new File(resultDirectory, "motif_" + (numRM) + ".txt");
					file1.renameTo(file2);
				} else {
					File file1 = new File(resultDirectory, "motif_tmp_" + (j + 1) + ".txt");
					file1.delete();
				}
			if (filterClusters)
				AptaLogger.log(Level.INFO, this.getClass(), numRM + " motifs remained after filtered.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		for (int j = 0; j < outputMotifs.size(); j++)
			if (!filtered[j]) {

				int idl = outputMotifs.get(j).getPWM().length;

				String[] idArr = new String[idl];
				for (int k = 0; k < idArr.length; k++)
					idArr[k] = Integer.toString(k + 1);

				summary2.AddRow(new Logo(outputMotifs.get(j).getPWM(), idArr),
						sorted.get(MotifSeedIDArr.get(j)).getKmer(), sorted.get(MotifSeedIDArr.get(j)).getPValue(),
						sorted.get(MotifSeedIDArr.get(j)).getProportion() * 100.0,
						outputMotifs.get(j).getProportion() * 100.00, outputMotifs.get(j).getTrace(roundIDs));
			}

		File out = new File(resultDirectory, "fullsummary.pdf");
		summary2.saveAsPDF(out.toString());

		if (outputClusters) {

			System.out.println();
			AptaLogger.log(Level.INFO, this.getClass(),
					"Writing aptamers in the last cycle where the motifs occur to file...");
			
			//presort the aptamers by count
			SelectionCycle lastRound= experiment.getSelectionCycles().get(experiment.getSelectionCycles().size()-1);
			int[] aptamer_ids = new int[lastRound.getUniqueSize()];
			int[] aptamer_counts = new int[lastRound.getUniqueSize()];
			
			int c = 0;
			for ( Entry<Integer, Integer> aptamer_id : lastRound.iterator()) {
				
				aptamer_ids[c] = aptamer_id.getKey();
				aptamer_counts[c] = aptamer_id.getValue();
				
				c++;
			}
			
			Quicksort.sort(aptamer_ids, aptamer_counts);
			
			int aptamerCount;
			numDone = 0;
			startTime = System.nanoTime();
			for (int x = aptamer_ids.length - 1; x >= 0; x--) {
				
				int aptamer_id = aptamer_ids[x];
				
				if (x < aptamer_ids.length - 1) {
					int lastid = aptamer_ids[x + 1];
					String thisS = new String(experiment.getAptamerPool().getAptamer(aptamer_id));
					if (thisS.equals(aptamer)) {
						System.out.println("Something wrong " + thisS + " " + aptamer + " " + aptamer_id + " " + lastid);
						System.exit(0);
					}
				}

				aptamer = new String(experiment.getAptamerPool().getAptamer(aptamer_id));
				AptamerBounds aptamerBounds = experiment.getAptamerPool()
						.getAptamerBounds(aptamer_id);
				aptamerCount = aptamer_counts[x];

				startPos = aptamerBounds.startIndex + klength - 1;
				endPos = aptamerBounds.endIndex;

				IntOpenHashSet seencid = new IntOpenHashSet();

				// iterate through each kmer of the aptamer and decide whether
				// the aptamer contains a motif
				for (int k = startPos; k < endPos; k++) {
					kmer = aptamer.substring(k - klength + 1, k + 1);
					if (k == startPos)
						id = calculateId(kmer);
					else
						id = calulateNewId(id, aptamer.charAt(k - klength), aptamer.charAt(k), klength);

					if (kmer2Clus.containsKey(id)) {
						mid = kmer2Clus.get(id);
						if ((!filtered[mid]) && (!seencid.contains(mid))) {
							clusterWriter.get(c2fc.get(mid)).format("%s\t%s\t%d\t%d\t%.2f%%\n",aptamer_id, aptamer, aptamerCount,
									(int) (aptamerCount * 1000000.0 / (rc[rc.length - 1] * 1.0)),
									aptamerCount / (rc[rc.length - 1] * 1.0));
							seencid.add(mid);
						}
					}
				}

				numDone++;
				if ((numDone == poolSize) || (numDone % 10000 == 0)) {
					endTime = System.nanoTime();
					rms = (long) (((endTime - startTime) / (numDone * 1000000000.0))
							* (lastRoundPool.size() - numDone));
					rmh = (int) (rms / 3600.0);
					rmm = (int) ((rms % 3600) / 60);
					System.out.print("Finished reading " + numDone + "/" + lastRoundPool.size() + " structures, ETA "
							+ String.format("%02d:%02d:%02d", rmh, rmm, rms % 60) + "\r");
				}
			}

			// Final Update
			endTime = System.nanoTime();
			rms = (long) (((endTime - startTime) / (numDone * 1000000000.0)) * (lastRoundPool.size() - numDone));
			rmh = (int) (rms / 3600.0);
			rmm = (int) ((rms % 3600) / 60);
			System.out.print("Finished reading " + numDone + "/" + lastRoundPool.size() + " structures, ETA "
					+ String.format("%02d:%02d:%02d", 0, 0, 0) + "\n");

			if (outputClusters) {
				for (int i = 0; i < clusterWriter.size(); i++)
					clusterWriter.get(i).close();
			}
		}		
		
		
		// prints out the aptamers in the last selection round that have
		// frequency more than singleton threshold and contain the motifs
		if (false) {

			System.out.println();
			AptaLogger.log(Level.INFO, this.getClass(),
					"Outputing aptamers in the last cycle where the motifs occur...");
			int aptamerCount;
			numDone = 0;
			startTime = System.nanoTime();
			for (int i = lastRoundPool.size() - 1; i >= 0; i--) {
				if (i < lastRoundPool.size() - 1) {
					int currentid = lastRoundPool.get(i).getFirst();
					int lastid = lastRoundPool.get(i + 1).getFirst();
					String thisS = new String(experiment.getAptamerPool().getAptamer(lastRoundPool.get(i).getFirst()));
					if (thisS.equals(aptamer)) {
						System.out.println("Something wrong " + thisS + " " + aptamer + " " + currentid + " " + lastid);
						System.exit(0);
					}
				}

				aptamer = new String(experiment.getAptamerPool().getAptamer(lastRoundPool.get(i).getFirst()));
				AptamerBounds aptamerBounds = experiment.getAptamerPool()
						.getAptamerBounds(lastRoundPool.get(i).getFirst());
				aptamerCount = lastRoundPool.get(i).getSecond();

				startPos = aptamerBounds.startIndex + klength - 1;
				endPos = aptamerBounds.endIndex;

				IntOpenHashSet seencid = new IntOpenHashSet();

				// iterate through each kmer of the aptamer and decide whether
				// the aptamer contains a motif
				for (int k = startPos; k < endPos; k++) {
					kmer = aptamer.substring(k - klength + 1, k + 1);
					if (k == startPos)
						id = calculateId(kmer);
					else
						id = calulateNewId(id, aptamer.charAt(k - klength), aptamer.charAt(k), klength);

					if (kmer2Clus.containsKey(id)) {
						mid = kmer2Clus.get(id);
						if ((!filtered[mid]) && (!seencid.contains(mid))) {
							clusterWriter.get(c2fc.get(mid)).format("%s\t%d\t%d\t%.2f%%\n", aptamer, aptamerCount,
									(int) (aptamerCount * 1000000.0 / (rc[rc.length - 1] * 1.0)),
									aptamerCount / (rc[rc.length - 1] * 1.0), lastRoundPool.get(i).getFirst());
							seencid.add(mid);
						}
					}
				}

				numDone++;
				if ((numDone == poolSize) || (numDone % 10000 == 0)) {
					endTime = System.nanoTime();
					rms = (long) (((endTime - startTime) / (numDone * 1000000000.0))
							* (lastRoundPool.size() - numDone));
					rmh = (int) (rms / 3600.0);
					rmm = (int) ((rms % 3600) / 60);
					System.out.print("Finished reading " + numDone + "/" + lastRoundPool.size() + " structures, ETA "
							+ String.format("%02d:%02d:%02d", rmh, rmm, rms % 60) + "\r");
				}
			}

			// Final Update
			endTime = System.nanoTime();
			rms = (long) (((endTime - startTime) / (numDone * 1000000000.0)) * (lastRoundPool.size() - numDone));
			rmh = (int) (rms / 3600.0);
			rmm = (int) ((rms % 3600) / 60);
			System.out.print("Finished reading " + numDone + "/" + lastRoundPool.size() + " structures, ETA "
					+ String.format("%02d:%02d:%02d", 0, 0, 0) + "\n");

			if (outputClusters) {
				for (int i = 0; i < clusterWriter.size(); i++)
					clusterWriter.get(i).close();
			}
		}
		System.out.println();
	}
}