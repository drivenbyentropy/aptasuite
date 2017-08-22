/**
 * 
 */
package lib.structure.rnafold;

import java.util.Arrays;
import java.util.Random;

import exceptions.InvalidConfigurationException;

/**
 * @author Jan Hoinka
 *
 * RNAFold4j is a port of RNAFold to Java. RNAFold is implemented and developed by Ivo L Hofacker et al.
 * as part of the ViennaRNA package v 1.8.5. All intellectual credits of this work 
 * go to the original authors and the Institute for Theoretical Chemistry of the 
 * University of Vienna. My only contribution is the adaptation of the C source code to Java.
 * 
 */
public class PartFunc {

	FoldVars fold_vars;
	PairMat pair_mat;
	
	int st_back = 0;
	double expMLclosing;
	double[] expMLintern = new double[EnergyConst.NBPAIRS + 1];
	double[] expMLbase;
	double expTermAU;
	double[][] expdangle5 = new double[EnergyConst.NBPAIRS + 1][5];
	double[][] expdangle3 = new double[EnergyConst.NBPAIRS + 1][5];
	double lxc;
	double[] exptetra = new double[40];
	double[] expTriloop = new double[40];
	double[][] expstack = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1];
	double[][][] expmismatchI = new double[EnergyConst.NBPAIRS + 1][5][5];
	double[][][] expmismatchH = new double[EnergyConst.NBPAIRS + 1][5][5];
	double[][][] expmismatchM = new double[EnergyConst.NBPAIRS + 1][5][5];
	double[][][][] expint11 = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5];
	double[][][][][] expint21 = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5][5];
	double[][][][][][] expint22 = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5][5][5];
	double[] exphairpin;
	double[] expbulge = new double[EnergyConst.MAXLOOP + 1];
	double[] expinternal = new double[EnergyConst.MAXLOOP + 1];
	double[][] expninio = new double[5][EnergyConst.MAXLOOP + 1];
	double[] q;
	double[] qb = null;
	double[] qm;
	double[] qm1;
	double[] qqm;
	double[] qqm1;
	double[] qq;
	double[] qq1;
	double[] prml;
	double[] prm_l;
	double[] prm_l1;
	double[] q1k;
	double[] qln;
	double[] scale;
	char[] ptype; /* precomputed array of pair types */
	int[] jindx;
	int init_length; /* length in last call to init_pf_fold() */
	double init_temp; /* temperature in last call to scale_pf_params */
	int circ = 0;
	double qo;
	double qho;
	double qio;
	double qmo;
	double[] qm2;
	byte[] pstruc;
	byte[] sequence;

	double ISOLATED = 256.0;

	static Random rand = new Random();

	/*-----------------------------------------------------------------*/
	short[] S;
	short[] S1;

	public PartFunc(FoldVars fold_vars, PairMat pair_mat) {
		
		this.fold_vars = fold_vars;
		this.pair_mat = pair_mat;
		
	}
	
	double pf_fold(byte[] sequence, byte[] structure) {

		double Q;

		double free_energy;
		int n = sequence.length;

		circ = 0;

		/* do the linear pf fold and fill all matrices */
		pf_linear(sequence, structure);

		if (fold_vars.backtrack_type == 'C')
			Q = qb[fold_vars.iindx[1] - n];
		else if (fold_vars.backtrack_type == 'M')
			Q = qm[fold_vars.iindx[1] - n];
		else
			Q = q[fold_vars.iindx[1] - n];

		/* ensemble free energy in Kcal/mol */
		if (Q <= Float.MIN_VALUE)
			System.out.println("pf_scale too large\n");
		free_energy = (-Math.log(Q) - n * Math.log(fold_vars.pf_scale)) * (fold_vars.temperature + EnergyConst.K0)
				* EnergyConst.GASCONST / 1000.0;
		/* in case we abort because of floating point errors */
		if (n > 1600)
			System.out.println(String.format("free energy = %8.2f\n", free_energy));

		/* calculate base pairing probability matrix (bppm) */
		if (fold_vars.do_backtrack != 0)
			pf_create_bppm(sequence, structure);

		return free_energy;
	}

	double pf_circ_fold(byte[] sequence, byte[] structure) {

		double Q;

		double free_energy;
		int n = sequence.length;

		circ = 1;
		/* do the linear pf fold and fill all matrices */
		pf_linear(sequence, structure);

		/* calculate post processing step for circular */
		/* RNAs */
		pf_circ(sequence, structure);

		if (fold_vars.backtrack_type == 'C')
			Q = qb[fold_vars.iindx[1] - n];
		else if (fold_vars.backtrack_type == 'M')
			Q = qm[fold_vars.iindx[1] - n];
		else
			Q = qo;

		/* ensemble free energy in Kcal/mol */
		if (Q <= Double.MIN_VALUE)
			System.out.println("pf_scale too large\n");
		free_energy = (-Math.log(Q) - n * Math.log(fold_vars.pf_scale)) * (fold_vars.temperature + EnergyConst.K0)
				* EnergyConst.GASCONST / 1000.0;
		/* in case we abort because of floating point errors */
		if (n > 1600)
			System.out.println(String.format("free energy = %8.2f\n", free_energy));

		/* calculate base pairing probability matrix (bppm) */
		if (fold_vars.do_backtrack != 0)
			pf_create_bppm(sequence, structure);

		return free_energy;
	}

	void pf_linear(byte[] sequence, byte[] structure) // Sequence MUST encode capital letter representation
	{

		int n, i, j, k, l, ij, u, u1, d, ii, type, type_2, tt;
		double temp;
		double Qmax = 0;
		double qbt1;
		double[] tmp;

		double max_real;

		max_real = Double.MAX_VALUE;

		n = sequence.length;
		if (n > init_length)
			init_pf_fold(n); /* (re)allocate space */
		if ((init_temp - fold_vars.temperature) > 1e-6)
			update_pf_params(n);

		S  = (S==null) ? new short[n+2] : Arrays.copyOf(S, n + 2);
		S1 = (S1==null) ? new short[n+2] : Arrays.copyOf(S1, n + 2);

		S[0] = (short) n;
		for (l = 1; l <= n; l++) {
			S[l] = (short) pair_mat.encode_char(sequence[l - 1]);
			S1[l] = pair_mat.alias[S[l]];
		}
		make_ptypes(S, structure);

		/* add first base at position n+1 and n'th base at position 0 */
		S[n + 1] = S[1];
		S1[n + 1] = S1[1];
		S1[0] = S1[n];

		/*
		 * array initialization ; qb,qm,q qb,qm,q (i,j) are stored as ((n+1-i)*(n-i) div
		 * 2 + n+1-j
		 */

		for (d = 0; d <= EnergyConst.TURN; d++)
			for (i = 1; i <= n - d; i++) {
				j = i + d;
				ij = fold_vars.iindx[i] - j;
				q[ij] = 1.0 * scale[d + 1];
				qb[ij] = qm[ij] = 0.0;
			}

		for (i = 1; i <= n; i++)
			qq[i] = qq1[i] = qqm[i] = qqm1[i] = 0;

		for (j = EnergyConst.TURN + 2; j <= n; j++) {
			for (i = j - EnergyConst.TURN - 1; i >= 1; i--) {
				/* construction of partition function of segment i,j */
				/* firstly that given i bound to j : qb(i,j) */
				u = j - i - 1;
				ij = fold_vars.iindx[i] - j;
				type = ptype[ij];
				if (type != 0) {
					/* hairpin contribution */
					if (((type == 3) || (type == 4)) && fold_vars.no_closingGU)
						qbt1 = 0;
					else
						qbt1 = expHairpinEnergy(u, type, S1[i + 1], S1[j - 1], sequence, i - 1)
								* scale[u + 2];/* add scale[u+2] */
					/* interior loops with interior pair k,l */
					for (k = i + 1; k <= Math.min(i + EnergyConst.MAXLOOP + 1, j - EnergyConst.TURN - 2); k++) {
						u1 = k - i - 1;
						for (l = Math.max(k + EnergyConst.TURN + 1, j - 1 - EnergyConst.MAXLOOP + u1); l < j; l++) {
							type_2 = ptype[fold_vars.iindx[k] - l];
							if (type_2 != 0) {
								type_2 = pair_mat.rtype[type_2];
								/* add *scale[u1+u2+2] */
								qbt1 += qb[fold_vars.iindx[k] - l] * (scale[u1 + j - l + 1] * expLoopEnergy(u1,
										j - l - 1, type, type_2, S1[i + 1], S1[j - 1], S1[k - 1], S1[l + 1]));
							}
						}
					}
					/* multiple stem loop contribution */
					ii = fold_vars.iindx[i + 1]; /* ii-k=[i+1,k-1] */
					temp = 0.0;
					for (k = i + 2; k <= j - 1; k++)
						temp += qm[ii - (k - 1)] * qqm1[k];
					tt = pair_mat.rtype[type];
					qbt1 += temp * expMLclosing * expMLintern[tt] * scale[2] * expdangle3[tt][S1[i + 1]]
							* expdangle5[tt][S1[j - 1]];

					qb[ij] = qbt1;
				} /* end if (type!=0) */
				else
					qb[ij] = 0.0;

				/*
				 * construction of qqm matrix containing final stem contributions to multiple
				 * loop partition function from segment i,j
				 */
				qqm[i] = qqm1[i] * expMLbase[1];
				if (type != 0) {
					qbt1 = qb[ij] * expMLintern[type];
					if ((i > 1) || (circ != 0))
						qbt1 *= expdangle5[type][S1[i - 1]];
					if ((j < n) || (circ != 0))
						qbt1 *= expdangle3[type][S1[j + 1]];
					else if (type > 2)
						qbt1 *= expTermAU;
					qqm[i] += qbt1;
				}
				if (qm1 != null)
					qm1[jindx[j] + i] = qqm[i]; /* for stochastic backtracking and circfold */

				/*
				 * construction of qm matrix containing multiple loop partition function
				 * contributions from segment i,j
				 */
				temp = 0.0;
				ii = fold_vars.iindx[i]; /* ii-k=[i,k-1] */
				for (k = i + 1; k <= j; k++)
					temp += (qm[ii - (k - 1)] + expMLbase[k - i]) * qqm[k];
				qm[ij] = (temp + qqm[i]);

				/* auxiliary matrix qq for cubic order q calculation below */
				qbt1 = qb[ij];
				if (type != 0) {
					if ((i > 1) || (circ != 0))
						qbt1 *= expdangle5[type][S1[i - 1]];
					if ((j < n) || (circ != 0))
						qbt1 *= expdangle3[type][S1[j + 1]];
					else if (type > 2)
						qbt1 *= expTermAU;
				}
				qq[i] = qq1[i] * scale[1] + qbt1;

				/* construction of partition function for segment i,j */
				temp = 1.0 * scale[1 + j - i] + qq[i];
				for (k = i; k <= j - 1; k++)
					temp += q[ii - k] * qq[k + 1];
				q[ij] = temp;

				if (temp > Qmax) {
					Qmax = temp;
					if (Qmax > max_real / 10.)
						throw new RuntimeException(String.format("Q close to overflow: %d %d %g\n", i, j, temp));
				}
				if (temp >= max_real) {
					throw new RuntimeException(
							String.format("overflow in pf_fold while calculating q[%d,%d]\nuse larger pf_scale", i, j));
				}
			}
			tmp = qq1;
			qq1 = qq;
			qq = tmp;
			tmp = qqm1;
			qqm1 = qqm;
			qqm = tmp;
		}
	}

	/* calculate partition function for circular case */
	/* NOTE: this is the postprocessing step ONLY */
	/* You have to call pf_linear first to calculate */
	/* complete circular case!!! */
	void pf_circ(byte[] sequence, byte[] structure) {

		int u, p, q, k, l;
		int n = sequence.length;

		double qot;

		qo = qho = qio = qmo = 0.;
		/* construct qm2 matrix with from qm1 entries */
		for (k = 1; k < n - EnergyConst.TURN - 1; k++) {
			qot = 0.;
			for (u = k + EnergyConst.TURN + 1; u < n - EnergyConst.TURN - 1; u++)
				qot += qm1[jindx[u] + k] * qm1[jindx[n] + (u + 1)];
			qm2[k] = qot;
		}

		for (p = 1; p < n; p++) {
			for (q = p + EnergyConst.TURN + 1; q <= n; q++) {
				int type;
				byte[] loopseq = new byte[10];
				/* 1. get exterior hairpin contribution */
				u = n - q + p - 1;
				if (u < EnergyConst.TURN)
					continue;
				type = ptype[fold_vars.iindx[p] - q];
				if (type == 0)
					continue;
				/*
				 * cause we want to calc the exterior loops, we need the reversed pair type from
				 * now on
				 */
				type = pair_mat.rtype[type];

				if (u < 7) {
					System.arraycopy(sequence, q - 1, loopseq, 0, sequence.length - (q - 1));
					System.arraycopy(sequence, 0, loopseq, q, p);
					// strcpy(loopseq , sequence+q-1);
					// strncat(loopseq, sequence, p);
				}
				/*
				 * We have to divide the returned expHairpinEnergy by scale[2] cause in the
				 * function call, the
				 */
				/*
				 * scale for the closing pair was already done in the forward recursion, as it
				 * is done again by
				 */
				/* calling the expHairpinEnergy function here */
				qho += (((type == 3) || (type == 4)) && fold_vars.no_closingGU) ? 0.
						: qb[fold_vars.iindx[p] - q] * expHairpinEnergy(u, type, S1[q + 1], S1[p - 1], loopseq, 0)
								* scale[u];

				/* 2. exterior interior loops, i "define" the (k,l) pair as "outer pair" */
				/* so "outer type" is rtype[type[k,l]] and inner type is type[p,q] */
				qot = 0.;
				for (k = q + 1; k < n; k++) {
					int ln1, lstart;
					ln1 = k - q - 1;
					if (ln1 + p - 1 > EnergyConst.MAXLOOP)
						break;
					lstart = ln1 + p - 1 + n - EnergyConst.MAXLOOP;
					if (lstart < k + EnergyConst.TURN + 1)
						lstart = k + EnergyConst.TURN + 1;
					for (l = lstart; l <= n; l++) {
						int ln2, type2;
						ln2 = (p - 1) + (n - l);

						if ((ln1 + ln2) > EnergyConst.MAXLOOP)
							continue;

						type2 = ptype[fold_vars.iindx[k] - l];
						if (type2 == 0)
							continue;
						/*
						 * for division by scale[2] just have a look at hairpin energy calculation above
						 */
						qio += qb[fold_vars.iindx[p] - q] * qb[fold_vars.iindx[k] - l] * expLoopEnergy(ln2, ln1,
								pair_mat.rtype[type2], type, S1[l + 1], S1[k - 1], S1[p - 1], S1[q + 1])
								* scale[ln1 + ln2];
					}
				} /* end of kl double loop */
			}
		} /* end of pq double loop */

		/* 3. Multiloops */
		for (k = EnergyConst.TURN + 2; k < n - 2 * EnergyConst.TURN - 3; k++)
			qmo += qm[fold_vars.iindx[1] - k] * qm2[k + 1] * expMLclosing;

		/* add an additional pf of 1.0 to take the open chain into account too */
		qo = qho + qio + qmo + 1.0 * scale[n];
	}

	/* calculate base pairing probs */
	void pf_create_bppm(byte[] sequence, byte[] structure) {
		int n, i, j, k, l, ij, kl, ii, ll, type, type_2, tt, ov = 0;
		double temp, Qmax = 0, prm_MLb;
		double prmt, prmt1;
		double[] tmp;
		double tmp2;

		double max_real = Double.MAX_VALUE;

		if ((S != null) && (S1 != null)) {
			n = S[0];
			Qmax = 0;

			for (k = 1; k <= n; k++) {
				q1k[k] = q[fold_vars.iindx[1] - k];
				qln[k] = q[fold_vars.iindx[k] - n];
			}
			q1k[0] = 1.0;
			qln[n + 1] = 1.0;

			fold_vars.pr = q; /* recycling */

			/* 1. exterior pair i,j and initialization of pr array */
			if (circ != 0) {
				for (i = 1; i <= n; i++) {
					for (j = i; j <= Math.min(i + EnergyConst.TURN, n); j++)
						fold_vars.pr[fold_vars.iindx[i] - j] = 0;
					for (j = i + EnergyConst.TURN + 1; j <= n; j++) {
						ij = fold_vars.iindx[i] - j;
						type = ptype[ij];
						if ((type != 0) && (qb[ij] > 0.)) {
							int rt, u;
							byte[] loopseq = new byte[10];

							u = i + n - j - 1;
							rt = pair_mat.rtype[type];
							fold_vars.pr[ij] = 1. / qo;

							/* 1.1. Exterior Hairpin Contribution */
							/* get the loop sequence */
							if (u < 7) {

								System.arraycopy(sequence, j - 1, loopseq, 0, sequence.length - (j - 1));
								System.arraycopy(sequence, 0, loopseq, j, i);

								// strcpy(loopseq , sequence+j-1); //TODO: Make sure this is correcly translated
								// into Java
								// strncat(loopseq, sequence, i);
							}
							tmp2 = expHairpinEnergy(u, rt, S1[j + 1], S1[i - 1], loopseq, 0) * scale[u];

							/* 1.2. Exterior Interior Loop Contribution */
							/* 1.2.1. i,j delimtis the "left" part of the interior loop */
							/* (j,i) is "outer pair" */
							for (k = 1; k < i - EnergyConst.TURN - 1; k++) {
								int ln1, lstart;
								ln1 = k + n - j - 1;
								if (ln1 > EnergyConst.MAXLOOP)
									break;
								lstart = ln1 + i - 1 - EnergyConst.MAXLOOP;
								if (lstart < k + EnergyConst.TURN + 1)
									lstart = k + EnergyConst.TURN + 1;
								for (l = lstart; l < i; l++) {
									int ln2; // , type_2;
									type_2 = ptype[fold_vars.iindx[k] - l];
									if (type_2 == 0)
										continue;
									ln2 = i - l - 1;
									if (ln1 + ln2 > EnergyConst.MAXLOOP)
										continue;
									tmp2 += qb[fold_vars.iindx[k] - l] * expLoopEnergy(ln1, ln2, rt,
											pair_mat.rtype[type_2], S1[j + 1], S1[i - 1], S1[k - 1], S1[l + 1])
											* scale[ln1 + ln2];
								}
							}
							/* 1.2.2. i,j delimtis the "right" part of the interior loop */
							for (k = j + 1; k < n - EnergyConst.TURN; k++) {
								int ln1, lstart;
								ln1 = k - j - 1;
								if ((ln1 + i - 1) > EnergyConst.MAXLOOP)
									break;
								lstart = ln1 + i - 1 + n - EnergyConst.MAXLOOP;
								if (lstart < k + EnergyConst.TURN + 1)
									lstart = k + EnergyConst.TURN + 1;
								for (l = lstart; l <= n; l++) {
									int ln2; // , type_2;
									type_2 = ptype[fold_vars.iindx[k] - l];
									if (type_2 == 0)
										continue;
									ln2 = i - 1 + n - l;
									if (ln1 + ln2 > EnergyConst.MAXLOOP)
										continue;
									tmp2 += qb[fold_vars.iindx[k] - l] * expLoopEnergy(ln2, ln1, pair_mat.rtype[type_2],
											rt, S1[l + 1], S1[k - 1], S1[i - 1], S1[j + 1]) * scale[ln1 + ln2];
								}
							}
							/* 1.3 Exterior multiloop decomposition */
							/* 1.3.1 Middle part */
							if ((i > EnergyConst.TURN + 2) && (j < n - EnergyConst.TURN - 1))
								tmp2 += qm[fold_vars.iindx[1] - i + 1] * qm[fold_vars.iindx[j + 1] - n] * expMLclosing
										* expMLintern[type] * expdangle3[type][S1[j + 1]] * expdangle5[type][S1[i - 1]];

							/* 1.3.2 Left part */
							for (k = EnergyConst.TURN + 2; k < i - EnergyConst.TURN - 2; k++)
								tmp2 += qm[fold_vars.iindx[1] - k] * qm1[jindx[i - 1] + k + 1] * expMLbase[n - j]
										* expMLclosing * expMLintern[type] * expdangle3[type][S1[j + 1]]
										* expdangle5[type][S1[i - 1]];

							/* 1.3.3 Right part */
							for (k = j + EnergyConst.TURN + 2; k < n - EnergyConst.TURN - 1; k++)
								tmp2 += qm[fold_vars.iindx[j + 1] - k] * qm1[jindx[n] + k + 1] * expMLbase[i - 1]
										* expMLclosing * expMLintern[type] * expdangle3[type][S1[j + 1]]
										* expdangle5[type][S1[i - 1]];

							/* all exterior loop decompositions for pair i,j done */
							fold_vars.pr[ij] *= tmp2;

						} else
							fold_vars.pr[ij] = 0;
					}
				}
			} /* end if(circ) */
			else {
				for (i = 1; i <= n; i++) {
					for (j = i; j <= Math.min(i + EnergyConst.TURN, n); j++)
						fold_vars.pr[fold_vars.iindx[i] - j] = 0;
					for (j = i + EnergyConst.TURN + 1; j <= n; j++) {
						ij = fold_vars.iindx[i] - j;
						type = ptype[ij];
						if ((type != 0) && (qb[ij] > 0.)) {
							fold_vars.pr[ij] = q1k[i - 1] * qln[j + 1] / q1k[n];
							if (i > 1)
								fold_vars.pr[ij] *= expdangle5[type][S1[i - 1]];
							if (j < n)
								fold_vars.pr[ij] *= expdangle3[type][S1[j + 1]];
							else if (type > 2)
								fold_vars.pr[ij] *= expTermAU;
						} else
							fold_vars.pr[ij] = 0;
					}
				}
			} /* end if(!circ) */

			for (l = n; l > EnergyConst.TURN + 1; l--) {

				/* 2. bonding k,l as substem of 2:loop enclosed by i,j */
				for (k = 1; k < l - EnergyConst.TURN; k++) {
					kl = fold_vars.iindx[k] - l;
					type_2 = ptype[kl];
					type_2 = pair_mat.rtype[type_2];
					if (qb[kl] == 0)
						continue;

					for (i = Math.max(1, k - EnergyConst.MAXLOOP - 1); i <= k - 1; i++)
						for (j = l + 1; j <= Math.min(l + EnergyConst.MAXLOOP - k + i + 2, n); j++) {
							ij = fold_vars.iindx[i] - j;
							type = ptype[ij];
							if ((fold_vars.pr[ij] > 0)) {
								/* add *scale[u1+u2+2] */
								fold_vars.pr[kl] += fold_vars.pr[ij] * (scale[k - i + j - l] * expLoopEnergy(k - i - 1,
										j - l - 1, type, type_2, S1[i + 1], S1[j - 1], S1[k - 1], S1[l + 1]));
							}
						}
				}
				/* 3. bonding k,l as substem of multi-loop enclosed by i,j */
				prm_MLb = 0.;
				if (l < n)
					for (k = 2; k < l - EnergyConst.TURN; k++) {
						i = k - 1;
						prmt = prmt1 = 0.0;

						ii = fold_vars.iindx[i]; /* ii-j=[i,j] */
						ll = fold_vars.iindx[l + 1]; /* ll-j=[l+1,j-1] */
						tt = ptype[ii - (l + 1)];
						tt = pair_mat.rtype[tt];
						prmt1 = fold_vars.pr[ii - (l + 1)] * expMLclosing * expMLintern[tt] * expdangle3[tt][S1[i + 1]]
								* expdangle5[tt][S1[l]];
						for (j = l + 2; j <= n; j++) {
							tt = ptype[ii - j];
							tt = pair_mat.rtype[tt];
							prmt += fold_vars.pr[ii - j] * expdangle3[tt][S1[i + 1]] * expdangle5[tt][S1[j - 1]]
									* qm[ll - (j - 1)];
						}
						kl = fold_vars.iindx[k] - l;
						tt = ptype[kl];
						prmt *= expMLclosing * expMLintern[tt];
						prml[i] = prmt;
						prm_l[i] = prm_l1[i] * expMLbase[1] + prmt1;

						prm_MLb = prm_MLb * expMLbase[1] + prml[i];
						/*
						 * same as: prm_MLb = 0; for (i=1; i<=k-1; i++) prm_MLb +=
						 * prml[i]*expMLbase[k-i-1];
						 */

						prml[i] = prml[i] + prm_l[i];

						if (qb[kl] == 0.)
							continue;

						temp = prm_MLb;

						for (i = 1; i <= k - 2; i++)
							temp += prml[i] * qm[fold_vars.iindx[i + 1] - (k - 1)];

						temp *= expMLintern[tt] * scale[2];
						if (k > 1)
							temp *= expdangle5[tt][S1[k - 1]];
						if (l < n)
							temp *= expdangle3[tt][S1[l + 1]];
						fold_vars.pr[kl] += temp;

						if (fold_vars.pr[kl] > Qmax) {
							Qmax = fold_vars.pr[kl];
							if (Qmax > max_real / 10.)
								System.out.println(String.format("P close to overflow: %d %d %g %g\n", i, j,
										fold_vars.pr[kl], qb[kl]));
						}
						if (fold_vars.pr[kl] >= max_real) {
							ov++;
							fold_vars.pr[kl] = Double.MAX_VALUE;
						}

					} /* end for (k=..) */
				tmp = prm_l1;
				prm_l1 = prm_l;
				prm_l = tmp;

			} /* end for (l=..) */

			for (i = 1; i <= n; i++)
				for (j = i + EnergyConst.TURN + 1; j <= n; j++) {
					ij = fold_vars.iindx[i] - j;
					fold_vars.pr[ij] *= qb[ij];
				}

			if (structure != null)
				sprintf_bppm(n, structure);
			if (ov > 0)
				throw new RuntimeException(String.format(
						"%d overflows occurred while backtracking;\nyou might try a smaller pf_scale than %g\n", ov,
						fold_vars.pf_scale));
		} /* end if((S != NULL) && (S1 != NULL)) */
		else
			throw new RuntimeException("bppm calculations have to be done after calling forward recursion\n");
		return;
	}

	/*------------------------------------------------------------------------*/
	/* dangling ends should never be destabilizing, i.e. expdangle>=1 */
	/* specific heat needs smooth function (2nd derivative) */
	/* we use a*(sin(x+b)+1)^2, with a=2/(3*sqrt(3)), b=Pi/6-sqrt(3)/2, */
	/* in the interval b<x<sqrt(3)/2 */

	int SCALE = 10;

	double SMOOTH(double X) {

		return ((X) / SCALE < -1.2283697) ? 0
				: (((X) / SCALE > 0.8660254) ? (X)
						: SCALE * 0.38490018 * (Math.sin((X) / SCALE - 0.34242663) + 1)
								* (Math.sin((X) / SCALE - 0.34242663) + 1));

	}

	void scale_pf_params(int length) {
		/* scale energy parameters and pre-calculate Boltzmann weights */
		int i, j, k, l;
		double kT, TT;
		double GT;

		init_temp = fold_vars.temperature;
		kT = (fold_vars.temperature + EnergyConst.K0) * EnergyConst.GASCONST; /* kT in cal/mol */
		TT = (fold_vars.temperature + EnergyConst.K0) / (EnergyPar.Tmeasure);

		/* scaling factors (to avoid overflows) */
		if (fold_vars.pf_scale == -1) { /* mean energy for random sequences: 184.3*length cal */
			fold_vars.pf_scale = Math.exp(-(-185 + (fold_vars.temperature - 37.) * 7.27) / kT);
			if (fold_vars.pf_scale < 1)
				fold_vars.pf_scale = 1;
		}
		scale[0] = 1.;
		scale[1] = 1. / fold_vars.pf_scale;
		for (i = 2; i <= length; i++) {
			scale[i] = scale[i / 2] * scale[i - (i / 2)];
		}

		/* loop energies: hairpins, bulges, interior, mulit-loops */
		for (i = 0; i <= Math.min(30, length); i++) {
			GT = EnergyPar.hairpin37[i] * TT;
			exphairpin[i] = Math.exp(-GT * 10. / kT);
		}
		for (i = 0; i <= Math.min(30, EnergyConst.MAXLOOP); i++) {
			GT = EnergyPar.bulge37[i] * TT;
			expbulge[i] = Math.exp(-GT * 10. / kT);
			GT = EnergyPar.internal_loop37[i] * TT;
			expinternal[i] = Math.exp(-GT * 10. / kT);
		}
		/* special case of size 2 interior loops (single mismatch) */
		if (fold_vars.james_rule != 0)
			expinternal[2] = Math.exp(-80 * 10 / kT);

		lxc = EnergyPar.lxc37 * TT;
		for (i = 31; i < length; i++) {
			GT = EnergyPar.hairpin37[30] * TT + (lxc * Math.log(i / 30.));
			exphairpin[i] = Math.exp(-GT * 10. / kT);
		}
		for (i = 31; i <= EnergyConst.MAXLOOP; i++) {
			GT = EnergyPar.bulge37[30] * TT + (lxc * Math.log(i / 30.));
			expbulge[i] = Math.exp(-GT * 10. / kT);
			GT = EnergyPar.internal_loop37[30] * TT + (lxc * Math.log(i / 30.));
			expinternal[i] = Math.exp(-GT * 10. / kT);
		}

		for (i = 0; i < 5; i++) {
			GT = EnergyPar.F_ninio37[i] * TT;
			for (j = 0; j <= EnergyConst.MAXLOOP; j++)
				expninio[i][j] = Math.exp(-Math.min(EnergyPar.MAX_NINIO, j * GT) * 10 / kT);
		}
		for (i = 0; (i * 7) < EnergyPar.Tetraloops.length(); i++) {
			GT = EnergyPar.TETRA_ENTH37 - (EnergyPar.TETRA_ENTH37 - EnergyPar.TETRA_ENERGY37[i]) * TT;
			exptetra[i] = Math.exp(-GT * 10. / kT);
		}
		for (i = 0; (i * 5) < EnergyPar.Triloops.length(); i++)
			expTriloop[i] = Math.exp(-EnergyPar.Triloop_E37[i] * 10 / kT);

		GT = EnergyPar.ML_closing37 * TT;
		expMLclosing = Math.exp(-GT * 10 / kT);

		for (i = 0; i <= EnergyConst.NBPAIRS; i++) { /* includes AU penalty */
			GT = EnergyPar.ML_intern37 * TT;
			/* if (i>2) GT += TerminalAU; */
			expMLintern[i] = Math.exp(-GT * 10. / kT);
		}
		expTermAU = Math.exp(-EnergyPar.TerminalAU * 10 / kT);

		GT = EnergyPar.ML_BASE37 * TT;
		for (i = 0; i < length; i++) {
			expMLbase[i] = Math.exp(-10. * i * GT / kT) * scale[i];
		}

		/*
		 * if dangles==0 just set their energy to 0, don't let dangle energies become >
		 * 0 (at large temps), but make sure go smoothly to 0
		 */
		for (i = 0; i <= EnergyConst.NBPAIRS; i++)
			for (j = 0; j <= 4; j++) {
				if (fold_vars.dangles != 0) {
					GT = EnergyPar.dangle5_H[i][j] - (EnergyPar.dangle5_H[i][j] - EnergyPar.dangle5_37[i][j]) * TT;
					expdangle5[i][j] = Math.exp(SMOOTH(-GT) * 10. / kT);
					GT = EnergyPar.dangle3_H[i][j] - (EnergyPar.dangle3_H[i][j] - EnergyPar.dangle3_37[i][j]) * TT;
					expdangle3[i][j] = Math.exp(SMOOTH(-GT) * 10. / kT);
				} else
					expdangle3[i][j] = expdangle5[i][j] = 1;
				if (i > 2) /* add TermAU penalty into dangle3 */
					expdangle3[i][j] *= expTermAU;
			}

		/* stacking energies */
		for (i = 0; i <= EnergyConst.NBPAIRS; i++)
			for (j = 0; j <= EnergyConst.NBPAIRS; j++) {
				GT = EnergyPar.enthalpies[i][j] - (EnergyPar.enthalpies[i][j] - EnergyPar.stack37[i][j]) * TT;
				expstack[i][j] = Math.exp(-GT * 10 / kT);
			}

		/* mismatch energies */
		for (i = 0; i <= EnergyConst.NBPAIRS; i++)
			for (j = 0; j < 5; j++)
				for (k = 0; k < 5; k++) {
					GT = EnergyPar.mism_H[i][j][k] - (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchI37[i][j][k]) * TT;
					expmismatchI[i][j][k] = Math.exp(-GT * 10.0 / kT);
					GT = EnergyPar.mism_H[i][j][k] - (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchH37[i][j][k]) * TT;
					expmismatchH[i][j][k] = Math.exp(-GT * 10.0 / kT);
					GT = EnergyPar.mism_H[i][j][k] - (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchM37[i][j][k]) * TT;
					expmismatchM[i][j][k] = Math.exp(-GT * 10.0 / kT);
				}

		/* interior lops of length 2 */
		for (i = 0; i <= EnergyConst.NBPAIRS; i++)
			for (j = 0; j <= EnergyConst.NBPAIRS; j++)
				for (k = 0; k < 5; k++)
					for (l = 0; l < 5; l++) {
						GT = EnergyPar.int11_H[i][j][k][l]
								- (EnergyPar.int11_H[i][j][k][l] - EnergyPar.int11_37[i][j][k][l]) * TT;
						expint11[i][j][k][l] = Math.exp(-GT * 10. / kT);
					}
		/* interior 2x1 loops */
		for (i = 0; i <= EnergyConst.NBPAIRS; i++)
			for (j = 0; j <= EnergyConst.NBPAIRS; j++)
				for (k = 0; k < 5; k++)
					for (l = 0; l < 5; l++) {
						int m;
						for (m = 0; m < 5; m++) {
							GT = EnergyPar.int21_H[i][j][k][l][m]
									- (EnergyPar.int21_H[i][j][k][l][m] - EnergyPar.int21_37[i][j][k][l][m]) * TT;
							expint21[i][j][k][l][m] = Math.exp(-GT * 10. / kT);
						}
					}
		/* interior 2x2 loops */
		for (i = 0; i <= EnergyConst.NBPAIRS; i++)
			for (j = 0; j <= EnergyConst.NBPAIRS; j++)
				for (k = 0; k < 5; k++)
					for (l = 0; l < 5; l++) {
						int m, n;
						for (m = 0; m < 5; m++)
							for (n = 0; n < 5; n++) {
								GT = EnergyPar.int22_H[i][j][k][l][m][n]
										- (EnergyPar.int22_H[i][j][k][l][m][n] - EnergyPar.int22_37[i][j][k][l][m][n])
												* TT;
								expint22[i][j][k][l][m][n] = Math.exp(-GT * 10. / kT);
							}
					}
	}

	/*----------------------------------------------------------------------*/
	double expHairpinEnergy(int u, int type, short si1, short sj1, byte[] string, int spos) {
		/* compute Boltzmann weight of a hairpin loop, multiply by scale[u+2] */
		double q;
		q = exphairpin[u];
		if ((fold_vars.tetra_loop) && (u == 4)) {
			byte[] tl = Arrays.copyOfRange(string, spos, spos + 6);

			int tsmt = EnergyPar.Tetraloops.indexOf(new String(tl));
			if (tsmt != -1)
				q *= exptetra[tsmt / 7];
		}
		if (u == 3) {
			byte[] tl = Arrays.copyOfRange(string, spos, spos + 5);

			int tsmt = EnergyPar.Triloops.indexOf(new String(tl));
			if (tsmt != -1)
				q *= expTriloop[tsmt / 5];
			if (type > 2)
				q *= expTermAU;
		} else /* no mismatches for tri-loops */
			q *= expmismatchH[type][si1][sj1];

		return q;
	}

	double expLoopEnergy(int u1, int u2, int type, int type2, short si1, short sj1, short sp1, short sq1) {
		/*
		 * compute Boltzmann weight of interior loop, multiply by scale[u1+u2+2] for
		 * scaling
		 */
		double z = 0;
		int no_close = 0;

		if ((fold_vars.no_closingGU) && ((type2 == 3) || (type2 == 4) || (type == 2) || (type == 4)))
			no_close = 1;

		if ((u1 == 0) && (u2 == 0)) /* stack */
			z = expstack[type][type2];
		else if (no_close == 0) {
			if ((u1 == 0) || (u2 == 0)) { /* bulge */
				int u;
				u = (u1 == 0) ? u2 : u1;
				z = expbulge[u];
				if (u2 + u1 == 1)
					z *= expstack[type][type2];
				else {
					if (type > 2)
						z *= expTermAU;
					if (type2 > 2)
						z *= expTermAU;
				}
			} else { /* interior loop */
				if (u1 + u2 == 2) /* size 2 is special */
					z = expint11[type][type2][si1][sj1];
				else if ((u1 == 1) && (u2 == 2))
					z = expint21[type][type2][si1][sq1][sj1];
				else if ((u1 == 2) && (u2 == 1))
					z = expint21[type2][type][sq1][si1][sp1];
				else if ((u1 == 2) && (u2 == 2))
					z = expint22[type][type2][si1][sp1][sq1][sj1];
				else {
					z = expinternal[u1 + u2] * expmismatchI[type][si1][sj1] * expmismatchI[type2][sq1][sp1];
					z *= expninio[2][Math.abs(u1 - u2)];
				}
			}
		}
		return z;
	}

	/*----------------------------------------------------------------------*/

	void get_arrays(int length) {
		int size, i;

		size = ((length + 1) * (length + 2) / 2);
		q = new double[size];
		qb = new double[size];
		qm = new double[size];

		if (st_back != 0) {
			qm1 = new double[size];
		}
		ptype = new char[((length + 1) * (length + 2) / 2)];
		q1k = new double[(length + 1)];
		qln = new double[(length + 2)];
		qq = new double[(length + 2)];
		qq1 = new double[(length + 2)];
		qqm = new double[(length + 2)];
		qqm1 = new double[(length + 2)];
		prm_l = new double[(length + 2)];
		prm_l1 = new double[(length + 2)];
		prml = new double[(length + 2)];
		exphairpin = new double[(length + 1)];
		expMLbase = new double[(length + 1)];
		scale = new double[(length + 1)];
		fold_vars.iindx = new int[(length + 1)];
		jindx = new int[(length + 1)];
		for (i = 1; i <= length; i++) {
			fold_vars.iindx[i] = ((length + 1 - i) * (length - i)) / 2 + length + 1;
			jindx[i] = (i * (i - 1)) / 2;
		}
		if (circ != 0) {
			/* qm1 array is used for folding of circular RNA too */
			if (qm1 != null)
				qm1 = new double[size];
			qm2 = new double[length + 2];
		}
	}

	/*----------------------------------------------------------------------*/

	void init_pf_circ_fold(int length) {
		circ = 1;
		init_pf_fold(length);
	}

	void init_pf_fold(int length) {
		if (length < 1)
			throw new InvalidConfigurationException("init_pf_fold: length must be greater 0");
		if (init_length > 0)
			free_pf_arrays(); /* free previous allocation */

		pair_mat.make_pair_matrix();
		get_arrays(length);
		scale_pf_params(length);
		init_length = length;
	}

	void free_pf_arrays() {
		q = null;
		fold_vars.pr = null;
		qb = null;
		qm = null;
		qm1 = null;
		qm2 = null;
		ptype = null;
		qq = null;
		qq1 = null;
		qqm = null;
		qqm1 = null;
		q1k = null;
		qln = null;
		prm_l = null;
		prm_l1 = null;
		prml = null;
		exphairpin = null;
		expMLbase = null;
		scale = null;
		fold_vars.iindx = null;
		jindx = null;
		init_length = 0;
		S = null;
		S1 = null;
	}
	/*---------------------------------------------------------------------------*/

	void update_pf_params(int length) {
		if (length > init_length)
			init_pf_fold(length); /* init not update */
		else {
			pair_mat.make_pair_matrix();
			scale_pf_params(length);
		}
	}

	/*---------------------------------------------------------------------------*/

	byte bppm_symbol(double[] x) {
		if (x[0] > 0.667)
			return '.';
		if (x[1] > 0.667)
			return '(';
		if (x[2] > 0.667)
			return ')';
		if ((x[1] + x[2]) > x[0]) {
			if ((x[1] / (x[1] + x[2])) > 0.667)
				return '{';
			if ((x[2] / (x[1] + x[2])) > 0.667)
				return '}';
			else
				return '|';
		}
		if (x[0] > (x[1] + x[2]))
			return ',';
		return ':';
	}

	/*---------------------------------------------------------------------------*/
	int L = 3;

	void sprintf_bppm(int length, byte[] structure) {
		int i, j;
		double[] P = new double[L]; /* P[][0] unpaired, P[][1] upstream p, P[][2] downstream p */

		for (j = 1; j <= length; j++) {
			P[0] = 1.0;
			P[1] = P[2] = 0.0;
			for (i = 1; i < j; i++) {
				P[2] += fold_vars.pr[fold_vars.iindx[i] - j]; /* j is paired downstream */
				P[0] -= fold_vars.pr[fold_vars.iindx[i] - j]; /* j is unpaired */
			}
			for (i = j + 1; i <= length; i++) {
				P[1] += fold_vars.pr[fold_vars.iindx[j] - i]; /* j is paired upstream */
				P[0] -= fold_vars.pr[fold_vars.iindx[j] - i]; /* j is unpaired */
			}
			structure[j - 1] = bppm_symbol(P);
		}
		structure[length] = '\0';
	}

	/*---------------------------------------------------------------------------*/
	void make_ptypes(short[] S, byte[] structure) {
		int n, i, j, k, l;

		n = S[0];
		for (k = 1; k < n - EnergyConst.TURN; k++)
			for (l = 1; l <= 2; l++) {
				int type, ntype = 0, otype = 0;
				i = k;
				j = i + EnergyConst.TURN + l;
				if (j > n)
					continue;
				type = pair_mat.pair[S[i]][S[j]];
				while ((i >= 1) && (j <= n)) {
					if ((i > 1) && (j < n))
						ntype = pair_mat.pair[S[i - 1]][S[j + 1]];
					if (fold_vars.noLonelyPairs && (otype == 0) && (ntype == 0))
						type = 0; /* i.j can only form isolated pairs */
					qb[fold_vars.iindx[i] - j] = 0.;
					ptype[fold_vars.iindx[i] - j] = (char) type;
					otype = type;
					type = ntype;
					i--;
					j++;
				}
			}

		if (fold_vars.fold_constrained && (structure != null)) {
			int hx;
			int[] stack = new int[n + 1];
			char type;

			for (hx = 0, j = 1; j <= n; j++) {
				switch (structure[j - 1]) {
				case 'x': /* can't pair */
					for (l = 1; l < j - EnergyConst.TURN; l++)
						ptype[fold_vars.iindx[l] - j] = 0;
					for (l = j + EnergyConst.TURN + 1; l <= n; l++)
						ptype[fold_vars.iindx[j] - l] = 0;
					break;
				case '(':
					stack[hx++] = j;
					/* fallthrough */
				case '<': /* pairs upstream */
					for (l = 1; l < j - EnergyConst.TURN; l++)
						ptype[fold_vars.iindx[l] - j] = 0;
					break;
				case ')':
					if (hx <= 0) {
						throw new RuntimeException(
								String.format("%s\nunbalanced brackets in constraints", new String(structure)));
					}
					i = stack[--hx];
					type = ptype[fold_vars.iindx[i] - j];
					/* don't allow pairs i<k<j<l */
					for (k = i; k <= j; k++)
						for (l = j; l <= n; l++)
							ptype[fold_vars.iindx[k] - l] = 0;
					/* don't allow pairs k<i<l<j */
					for (k = 1; k <= i; k++)
						for (l = i; l <= j; l++)
							ptype[fold_vars.iindx[k] - l] = 0;
					ptype[fold_vars.iindx[i] - j] = (type == 0) ? 7 : type;
					/* fallthrough */
				case '>': /* pairs downstream */
					for (l = j + EnergyConst.TURN + 1; l <= n; l++)
						ptype[fold_vars.iindx[j] - l] = 0;
					break;
				}
			}
			if (hx != 0) {
				throw new RuntimeException(
						String.format("%s\nunbalanced brackets in constraints", new String(structure)));
			}
			stack = null;
		}
	}

	/*
	 * stochastic backtracking in pf_fold arrays returns random structure S with
	 * Boltzman probabilty p(S) = exp(-E(S)/kT)/Z
	 */
	byte[] pbacktrack(byte[] seq) {
		double r, qt;
		int i, j, n, start;

		sequence = seq;
		n = sequence.length;

		if (init_length < 1)
			throw new RuntimeException("can't backtrack without pf arrays.\nCall pf_fold() before pbacktrack()");
		pstruc = new byte[n + 1];

		for (i = 0; i < n; i++)
			pstruc[i] = '.';

		start = 1;
		while (start < n) {
			/* find i position of first pair */
			for (i = start; i < n; i++) {
				r = rand.nextDouble() * qln[i];
				if (r > qln[i + 1] * scale[1])
					break; /* i is paired */
			}
			if (i >= n)
				break; /* no more pairs */
			/* now find the pairing partner j */
			r = rand.nextDouble() * (qln[i] - qln[i + 1] * scale[1]);
			for (qt = 0, j = i + 1; j <= n; j++) {
				int type;
				type = ptype[fold_vars.iindx[i] - j];
				if (type != 0) {
					double qkl;
					qkl = qb[fold_vars.iindx[i] - j];
					if (j < n)
						qkl *= qln[j + 1];
					if (i > 1)
						qkl *= expdangle5[type][S1[i - 1]];
					if (j < n)
						qkl *= expdangle3[type][S1[j + 1]];
					else if (type > 2)
						qkl *= expTermAU;
					qt += qkl;
					if (qt > r)
						break; /* j is paired */
				}
			}
			if (j == n + 1)
				throw new RuntimeException("backtracking failed in ext loop");
			start = j + 1;
			backtrack(i, j);
		}

		return pstruc;
	}

	byte[] pbacktrack_circ(byte[] seq) {
		double r, qt;
		int i, j, k, l, n;

		sequence = seq;
		n = sequence.length;

		if (init_length < 1)
			throw new RuntimeException(
					"can't backtrack without pf arrays.\nCall pf_circ_fold() before pbacktrack_circ()");

		pstruc = new byte[n + 1];

		/* initialize pstruct with single bases */
		for (i = 0; i < n; i++)
			pstruc[i] = '.';

		qt = 1.0 * scale[n];
		r = rand.nextDouble() * qo;

		/* open chain? */
		if (qt > r)
			return pstruc;

		for (i = 1; (i < n); i++) {
			for (j = i + EnergyConst.TURN + 1; (j <= n); j++) {
				int type, u;
				byte[] loopseq = new byte[10];

				/* 1. first check, wether we can do a hairpin loop */
				u = n - j + i - 1;
				if (u < EnergyConst.TURN)
					continue;

				type = ptype[fold_vars.iindx[i] - j];
				if (type == 0)
					continue;

				type = pair_mat.rtype[type];

				if (u < 7) {
					System.arraycopy(sequence, j - 1, loopseq, 0, sequence.length - (j - 1));
					System.arraycopy(sequence, 0, loopseq, j, i);
					// strcpy(loopseq , sequence+j-1);
					// strncat(loopseq, sequence, i);
				}

				qt += qb[fold_vars.iindx[i] - j] * expHairpinEnergy(u, type, S1[j + 1], S1[i - 1], loopseq, 0)
						* scale[u];
				/* found a hairpin? so backtrack in the enclosed part and we're done */
				if (qt > r) {
					backtrack(i, j);
					return pstruc;
				}

				/* 2. search for (k,l) with which we can close an interior loop */
				for (k = j + 1; (k < n); k++) {
					int ln1, lstart;
					ln1 = k - j - 1;
					if (ln1 + i - 1 > EnergyConst.MAXLOOP)
						break;

					lstart = ln1 + i - 1 + n - EnergyConst.MAXLOOP;
					if (lstart < k + EnergyConst.TURN + 1)
						lstart = k + EnergyConst.TURN + 1;
					for (l = lstart; (l <= n); l++) {
						int ln2, type2;
						ln2 = (i - 1) + (n - l);
						if ((ln1 + ln2) > EnergyConst.MAXLOOP)
							continue;

						type2 = ptype[fold_vars.iindx[k] - l];
						if (type == 0)
							continue;
						type2 = pair_mat.rtype[type2];
						qt += qb[fold_vars.iindx[i] - j] * qb[fold_vars.iindx[k] - l]
								* expLoopEnergy(ln2, ln1, type2, type, S1[l + 1], S1[k - 1], S1[i - 1], S1[j + 1])
								* scale[ln1 + ln2];
						/* found an exterior interior loop? also this time, we can go straight */
						/* forward and backtracking the both enclosed parts and we're done */
						if (qt > r) {
							backtrack(i, j);
							backtrack(k, l);
							return pstruc;
						}
					}
				} /* end of kl double loop */
			}
		} /* end of ij double loop */
		{
			/*
			 * so cause we reach this part, we have to search for our barrier between qm and
			 * qm2
			 */
			qt = 0.;
			r = rand.nextDouble() * qmo;
			for (k = EnergyConst.TURN + 2; k < n - 2 * EnergyConst.TURN - 3; k++) {
				qt += qm[fold_vars.iindx[1] - k] * qm2[k + 1] * expMLclosing;
				/* backtrack in qm and qm2 if we've found a valid barrier k */
				if (qt > r) {
					backtrack_qm(1, k);
					backtrack_qm2(k + 1, n);
					return pstruc;
				}
			}
		}
		/* if we reach the real end of this function, an error has occured */
		/* cause we HAVE TO find an exterior loop or an open chain!!! */
		throw new RuntimeException("backtracking failed in exterior loop");
		// return pstruc;
	}

	void backtrack_qm(int i, int j) {
		/* divide multiloop into qm and qm1 */
		double qmt, r;
		int k;
		while (j > i) {
			/* now backtrack [i ... j] in qm[] */
			r = rand.nextDouble() * qm[fold_vars.iindx[i] - j];
			qmt = qm1[jindx[j] + i];
			k = i;
			if (qmt < r)
				for (k = i + 1; k <= j; k++) {
					qmt += (qm[fold_vars.iindx[i] - (k - 1)] + expMLbase[k - i]) * qm1[jindx[j] + k];
					if (qmt >= r)
						break;
				}
			if (k > j)
				throw new RuntimeException("backtrack failed in qm");

			backtrack_qm1(k, j);

			if (k < i + EnergyConst.TURN)
				break; /* no more pairs */
			r = rand.nextDouble() * (qm[fold_vars.iindx[i] - (k - 1)] + expMLbase[k - i]);
			if (expMLbase[k - i] >= r)
				break; /* no more pairs */
			j = k - 1;
		}
	}

	void backtrack_qm1(int i, int j) {
		/* i is paired to l, i<l<j; backtrack in qm1 to find l */
		int ii, l, type;
		double qt, r;

		r = rand.nextDouble() * qm1[jindx[j] + i];
		ii = fold_vars.iindx[i];
		for (qt = 0., l = i + EnergyConst.TURN + 1; l <= j; l++) {
			type = ptype[ii - l];
			if (type != 0)
				qt += qb[ii - l] * expMLintern[type] * expdangle5[type][S1[i - 1]] * expdangle3[type][S1[l + 1]]
						* expMLbase[j - l];
			if (qt >= r)
				break;
		}
		if (l > j)
			throw new RuntimeException("backtrack failed in qm1");
		backtrack(i, l);
	}

	void backtrack_qm2(int k, int n) {
		double qom2t, r;
		int u;
		r = rand.nextDouble() * qm2[k];
		/* we have to search for our barrier u between qm1 and qm1 */
		for (qom2t = 0., u = k + EnergyConst.TURN + 1; u < n - EnergyConst.TURN - 1; u++) {
			qom2t += qm1[jindx[u] + k] * qm1[jindx[n] + (u + 1)];
			if (qom2t > r)
				break;
		}
		if (u == n - EnergyConst.TURN)
			throw new RuntimeException("backtrack failed in qm2");
		backtrack_qm1(k, u);
		backtrack_qm1(u + 1, n);
	}

	void backtrack(int i, int j) {
		do {
			double r, qbt1;
			int k, l = 0, type, u, u1;

			pstruc[i - 1] = '(';
			pstruc[j - 1] = ')';

			r = rand.nextDouble() * qb[fold_vars.iindx[i] - j];
			type = ptype[fold_vars.iindx[i] - j];
			u = j - i - 1;
			/* hairpin contribution */
			if (((type == 3) || (type == 4)) && fold_vars.no_closingGU)
				qbt1 = 0;
			else
				qbt1 = expHairpinEnergy(u, type, S1[i + 1], S1[j - 1], sequence, i - 1)
						* scale[u + 2]; /* add scale[u+2] */

			if (qbt1 >= r)
				return; /* found the hairpin we're done */

			for (k = i + 1; k <= Math.min(i + EnergyConst.MAXLOOP + 1, j - EnergyConst.TURN - 2); k++) {
				u1 = k - i - 1;
				for (l = Math.max(k + EnergyConst.TURN + 1, j - 1 - EnergyConst.MAXLOOP + u1); l < j; l++) {
					int type_2;
					type_2 = ptype[fold_vars.iindx[k] - l];
					if (type_2 != 0) {
						type_2 = pair_mat.rtype[type_2];
						/* add *scale[u1+u2+2] */
						qbt1 += qb[fold_vars.iindx[k] - l] * (scale[u1 + j - l + 1] * expLoopEnergy(u1, j - l - 1, type,
								type_2, S1[i + 1], S1[j - 1], S1[k - 1], S1[l + 1]));
					}
					if (qbt1 > r)
						break;
				}
				if (qbt1 > r)
					break;
			}
			if (l < j) {
				i = k;
				j = l;
			} else
				break;
		} while (true);

		/* backtrack in multi-loop */
		{
			double r, qt;
			int k, ii, jj;

			i++;
			j--;
			/* find the first split index */
			ii = fold_vars.iindx[i]; /* ii-j=[i,j] */
			jj = jindx[j]; /* jj+i=[j,i] */
			for (qt = 0., k = i + 1; k < j; k++)
				qt += qm[ii - (k - 1)] * qm1[jj + k];
			r = rand.nextDouble() * qt;
			for (qt = 0., k = i + 1; k < j; k++) {
				qt += qm[ii - (k - 1)] * qm1[jj + k];
				if (qt >= r)
					break;
			}
			if (k >= j)
				throw new RuntimeException("backtrack failed, can't find split index ");

			backtrack_qm1(k, j);

			j = k - 1;
			backtrack_qm(i, j);
		}
	}

	double mean_bp_dist(int length) {
		/* compute the mean base pair distance in the thermodynamic ensemble */
		/*
		 * <d> = \sum_{a,b} p_a p_b d(S_a,S_b) this can be computed from the pair probs
		 * p_ij as <d> = \sum_{ij} p_{ij}(1-p_{ij})
		 */
		int i, j;
		double d = 0;

		if (fold_vars.pr == null)
			throw new RuntimeException("pr==NULL. You need to call pf_fold() before mean_bp_dist()");

		for (i = 1; i <= length; i++)
			for (j = i + EnergyConst.TURN + 1; j <= length; j++)
				d += fold_vars.pr[fold_vars.iindx[i] - j] * (1 - fold_vars.pr[fold_vars.iindx[i] - j]);
		return 2 * d;
	}

	CentroidData centroid(int length) {
		/*
		 * compute the centroid structure of the ensemble, i.e. the strutcure with the
		 * minimal average distance to all other structures <d(S)> = \sum_{(i,j) \in S}
		 * (1-p_{ij}) + \sum_{(i,j) \notin S} p_{ij} Thus, the centroid is simply the
		 * structure containing all pairs with p_ij>0.5
		 */
		int i, j;
		double p;
		CentroidData centroid = new CentroidData();

		if (fold_vars.pr == null)
			throw new RuntimeException("pr==NULL. You need to call pf_fold() before centroid()");

		centroid.structure = new byte[length];
		for (i = 0; i < length; i++)
			centroid.structure[i] = '.';
		for (i = 1; i <= length; i++)
			for (j = i + EnergyConst.TURN + 1; j <= length; j++) {
				if ((p = fold_vars.pr[fold_vars.iindx[i] - j]) > 0.5) {
					centroid.structure[i - 1] = '(';
					centroid.structure[j - 1] = ')';
					centroid.distance += (1 - p);
				} else
					centroid.distance += p;
			}
		
		return centroid;
	}

	PList[] stackProb(double cutoff) {

		PList[] pl;
		int i, j, plsize = 256;
		int length, num = 0;
		if (fold_vars.pr == null)
			throw new RuntimeException("pr==NULL. You need to call pf_fold() before stackProb()");

		pl = new PList[plsize];
		for(int x=0; x<pl.length; pl[x++]=new PList());
		length = S[0];
		for (i = 1; i < length; i++)
			for (j = i + EnergyConst.TURN + 3; j <= length; j++) {
				double p;
				if ((p = fold_vars.pr[fold_vars.iindx[i] - j]) < cutoff)
					continue;
				if (qb[fold_vars.iindx[i + 1] - (j - 1)] < Double.MIN_VALUE)
					continue;
				p *= qb[fold_vars.iindx[i + 1] - (j - 1)] / qb[fold_vars.iindx[i] - j];
				p *= expLoopEnergy(0, 0, (int) ptype[fold_vars.iindx[i] - j],
						pair_mat.rtype[ptype[fold_vars.iindx[i + 1] - (j - 1)]], (short) 0, (short) 0, (short) 0,
						(short) 0) * scale[2];/* add *scale[u1+u2+2] */
				if (p > cutoff) {
					pl[num].i = i;
					pl[num].j = j;
					pl[num++].p = p;
					if (num >= plsize) {
						plsize *= 2;
						pl = Arrays.copyOf(pl, plsize);
						for(int x=(plsize/2); x<pl.length; pl[x++]=new PList());
					}
				}
			}
		pl[num].i = 0;
		return pl;
	}

	// /*-------------------------------------------------------------------------*/
	// /* make arrays used for pf_fold available to other routines */
	// PUBLIC int get_pf_arrays(short **S_p, short **S1_p, char **ptype_p,
	// FLT_OR_DBL **qb_p, FLT_OR_DBL **qm_p, FLT_OR_DBL **q1k_p, FLT_OR_DBL **qln_p)
	// {
	// if(qb == NULL) return(0); /* check if pf_fold() has been called */
	// *S_p = S; *S1_p = S1; *ptype_p = ptype;
	// *qb_p = qb; *qm_p = qm;
	// *q1k_p = q1k; *qln_p = qln;
	// return(1); /* success */
	// }

}
