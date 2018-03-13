/**
 * 
 */
package lib.structure.capr;

import java.util.ArrayList;

/**
 * @author Jan Hoinka 
 * Implements the <code>CapR.cpp</code> and
 * <code>CapR.h</code> of the CapR package available at <a href=
 * "https://github.com/fukunagatsu/CapR">https://github.com/fukunagatsu/CapR</a>.
 * All intellectual credits of this work goes to the original authors. Please note
 * the original code is licensed under the MIT license (see LICENSE.txt).
 */
public class CapR {

	int _maximal_span;

	double[] hairpin = new double[31];
	double[][][] mismatchH = new double[7][5][5];
	double[][][] mismatchI = new double[7][5][5];
	double[][] stack = new double[7][7];
	double[] bulge = new double[31];
	double TermAU;
	double[][][][] int11 = new double[8][8][5][5];
	double[][][][][] int21 = new double[8][8][5][5][5];
	double[][][][][][] int22 = new double[8][8][5][5][5][5];
	double[] internal = new double[31];
	double MLclosing;
	double MLintern;
	double MLbase;
	double[][] dangle5 = new double[8][5];
	double[][] dangle3 = new double[8][5];
	double[] ninio = new double[EnergyPar.MAXLOOP + 1];

	ArrayList<Integer> _int_sequence = new ArrayList<Integer>();
	int _seq_length;

	ArrayList<Double> _Alpha_outer = new ArrayList<Double>();
	DataMatrix<Double> _Alpha_stem = new DataMatrix<Double>();
	DataMatrix<Double> _Alpha_stemend = new DataMatrix<Double>();
	DataMatrix<Double> _Alpha_multi = new DataMatrix<Double>();
	DataMatrix<Double> _Alpha_multibif = new DataMatrix<Double>();
	DataMatrix<Double> _Alpha_multi1 = new DataMatrix<Double>();
	DataMatrix<Double> _Alpha_multi2 = new DataMatrix<Double>();

	ArrayList<Double> _Beta_outer = new ArrayList<Double>();
	DataMatrix<Double> _Beta_stem = new DataMatrix<Double>();
	DataMatrix<Double> _Beta_stemend = new DataMatrix<Double>();
	DataMatrix<Double> _Beta_multi = new DataMatrix<Double>();
	DataMatrix<Double> _Beta_multibif = new DataMatrix<Double>();
	DataMatrix<Double> _Beta_multi1 = new DataMatrix<Double>();
	DataMatrix<Double> _Beta_multi2 = new DataMatrix<Double>();

	public CapR(){
		set_energy_parameters();
	}

	public void ComputeStructuralProfile(byte[] sequence, int maximal_span) {
		_maximal_span = maximal_span;
		_seq_length = 0;
		
		Clear();
		Initiallize(sequence);
		CalcInsideVariable();
		CalcOutsideVariable();
		
	}

	private void set_energy_parameters() {
		MLclosing = -EnergyPar.ML_closing37 * 10 / EnergyPar.kT;
		MLintern = -EnergyPar.ML_intern37 * 10. / EnergyPar.kT;
		MLbase = -EnergyPar.ML_BASE37 * 10. / EnergyPar.kT;
		TermAU = -EnergyPar.TerminalAU * 10 / EnergyPar.kT;

		for (int i = 0; i <= 30; i++) {
			hairpin[i] = -EnergyPar.hairpin37[i] * 10. / EnergyPar.kT;
			bulge[i] = -EnergyPar.bulge37[i] * 10. / EnergyPar.kT;
			internal[i] = -EnergyPar.internal_loop37[i] * 10. / EnergyPar.kT;
		}

		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				for (int k = 0; k < 5; k++) {
					mismatchI[i][j][k] = -EnergyPar.mismatchI37[i][j][k] * 10.0 / EnergyPar.kT;
					mismatchH[i][j][k] = -EnergyPar.mismatchH37[i][j][k] * 10.0 / EnergyPar.kT;
				}
			}

			for (int j = 0; j < 7; j++) {
				stack[i][j] = -EnergyPar.stack37[i][j] * 10. / EnergyPar.kT;
			}

			for (int j = 0; j <= 4; j++) {
				dangle5[i][j] = -EnergyPar.dangle5_37[i][j] * 10. / EnergyPar.kT;
				dangle3[i][j] = -EnergyPar.dangle3_37[i][j] * 10. / EnergyPar.kT;
				if (i > 2) {
					dangle3[i][j] += TermAU;
				}
			}
		}

		for (int i = 0; i <= 7; i++) {
			for (int j = 0; j <= 7; j++) {
				for (int k = 0; k < 5; k++) {
					for (int l = 0; l < 5; l++) {
						int11[i][j][k][l] = -InitLoops.int11_37[i][j][k][l] * 10. / EnergyPar.kT;
						for (int m = 0; m < 5; m++) {
							int21[i][j][k][l][m] = -InitLoops.int21_37[i][j][k][l][m] * 10. / EnergyPar.kT;
							for (int n = 0; n < 5; n++) {
								int22[i][j][k][l][m][n] = -InitLoops.int22_37[i][j][k][l][m][n] * 10. / EnergyPar.kT;
							}
						}
					}
				}
			}
		}

		for (int i = 0; i <= EnergyPar.MAXLOOP; i++) {
			ninio[i] = -Math.min(EnergyPar.MAX_NINIO, i * EnergyPar.F_ninio37) * 10 / EnergyPar.kT;
		}
	}

	private void Initiallize(byte[] sequence) {
		
		_seq_length = sequence.length;
		_int_sequence.ensureCapacity(_seq_length + 1);
		
		for (int x = 0; x < _seq_length + 1; x++) {
			
			_int_sequence.add(0);
			_Alpha_outer.add(0.0);
			_Beta_outer.add(0.0);
			
		}
		
		for (int i = 0; i < _seq_length; i++) {
			if (sequence[i] == 'A' || sequence[i] == 'a') {
				_int_sequence.set(i + 1, 1);
			} else if (sequence[i] == 'C' || sequence[i] == 'c') {
				_int_sequence.set(i + 1, 2);
			} else if (sequence[i] == 'G' || sequence[i] == 'g') {
				_int_sequence.set(i + 1, 3);
			} else if (sequence[i] == 'T' || sequence[i] == 't' || sequence[i] == 'U' || sequence[i] == 'u') {
				_int_sequence.set(i + 1, 4);
			} else {
				_int_sequence.set(i + 1, 0);
			}
		}
		
		
		_Alpha_stem.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Alpha_stemend.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Alpha_multi.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Alpha_multibif.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Alpha_multi1.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Alpha_multi2.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);

		_Beta_stem.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Beta_stemend.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Beta_multi.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Beta_multibif.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Beta_multi1.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		_Beta_multi2.reshape(_seq_length + 1, _maximal_span + 2).clear((double) -EnergyPar.INF);
		
	}

	private void CalcInsideVariable() {
		for (int j = EnergyPar.TURN + 1; j <= _seq_length; j++) {
			for (int i = j - EnergyPar.TURN; i >= Math.max(0, j - _maximal_span - 1); i--) {
				// Alpha_stem
				int type = EnergyPar.BP_pair[_int_sequence.get(i + 1)][_int_sequence.get(j)];
				int type2 = EnergyPar.BP_pair[_int_sequence.get(i + 2)][_int_sequence.get(j - 1)];

				double temp = 0;
				boolean flag = false;
				if (type != 0) {
					type2 = EnergyPar.rtype[type2];
					if (_Alpha_stem.get(i + 1, j - i - 2) != -EnergyPar.INF) {
						// Stem¨Stem
						if (type2 != 0) {
							temp = _Alpha_stem.get(i + 1,j - i - 2)
									+ LoopEnergy(type, type2, i + 1, j, i + 2, j - 1);
						}
						flag = true;
					}

					if (_Alpha_stemend.get(i + 1,j - i - 2) != -EnergyPar.INF) {
						// Stem¨StemEnd
						temp = (flag == true) ? logsumexp(temp, _Alpha_stemend.get(i + 1,j - i - 2))
								: _Alpha_stemend.get(i + 1,j - i - 2);
						flag = true;
					}

					_Alpha_stem.set(i, j - i, (flag == false) ? -EnergyPar.INF : temp);
				} else {
					_Alpha_stem.set(i, j - i, (double) -EnergyPar.INF);
				}

				// Alpha_multiBif
				temp = 0;
				flag = false;
				for (int k = i + 1; k <= j - 1; k++) {
					if (_Alpha_multi1.get(i,k - i) != -EnergyPar.INF
							&& _Alpha_multi2.get(k,j - k) != -EnergyPar.INF) {
						temp = (flag == false) ? _Alpha_multi1.get(i,k - i) + _Alpha_multi2.get(k,j - k)
								: logsumexp(temp, _Alpha_multi1.get(i,k - i) + _Alpha_multi2.get(k,j - k));
						flag = true;
					}
				}
				_Alpha_multibif.set(i, j - i, (flag == false) ? -EnergyPar.INF : temp);

				// Alpha_multi2
				temp = 0;
				flag = false;
				if (type != 0) {
					if (_Alpha_stem.get(i,j - i) != -EnergyPar.INF) {
						temp = _Alpha_stem.get(i,j - i) + MLintern + CalcDangleEnergy(type, i, j);
						flag = true;
					}
				}
				if (_Alpha_multi2.get(i,j - i - 1) != -EnergyPar.INF) {
					_Alpha_multi2.set(i, j - i, _Alpha_multi2.get(i,j - i - 1) + MLbase);
					if (flag == true) {
						_Alpha_multi2.set(i, j - i, logsumexp(temp, _Alpha_multi2.get(i,j - i)));
					}
				} else {
					_Alpha_multi2.set(i, j - i, (flag == false) ? -EnergyPar.INF : temp);
				}

				// Alpha_multi1
				if (_Alpha_multi2.get(i,j - i) != -EnergyPar.INF
						&& _Alpha_multibif.get(i,j - i) != -EnergyPar.INF) {
					_Alpha_multi1.set(i, j - i,
							logsumexp(_Alpha_multi2.get(i,j - i), _Alpha_multibif.get(i,j - i)));
				} else if (_Alpha_multi2.get(i,j - i) == -EnergyPar.INF) {
					_Alpha_multi1.set(i, j - i, _Alpha_multibif.get(i,j - i));
				} else if (_Alpha_multibif.get(i,j - i) == -EnergyPar.INF) {
					_Alpha_multi1.set(i, j - i, _Alpha_multi2.get(i,j - i));
				} else {
					_Alpha_multi1.set(i, j - i, (double) -EnergyPar.INF);
				}

				// Alpha_multi
				flag = false;
				if (_Alpha_multi.get(i + 1,j - i - 1) != -EnergyPar.INF) {
					_Alpha_multi.set(i, j - i, _Alpha_multi.get(i + 1,j - i - 1) + MLbase);
					flag = true;
				}

				if (flag == true) {
					if (_Alpha_multibif.get(i,j - i) != -EnergyPar.INF) {
						_Alpha_multi.set(i, j - i,
								logsumexp(_Alpha_multi.get(i,j - i), _Alpha_multibif.get(i,j - i)));
					}
				} else {
					_Alpha_multi.set(i, j - i, _Alpha_multibif.get(i,j - i));
				}

				// Alpha_stemend
				if (j != _seq_length) {
					temp = 0;
					type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j + 1)];
					if (type != 0) {
						// StemEnd¨sn
						temp = HairpinEnergy(type, i, j + 1);

						// StemEnd¨sm_Stem_sn
						for (int p = i; p <= Math.min(i + EnergyPar.MAXLOOP, j - EnergyPar.TURN - 2); p++) {
							int u1 = p - i;
							for (int q = Math.max(p + EnergyPar.TURN + 2, j - EnergyPar.MAXLOOP + u1); q <= j; q++) {
								type2 = EnergyPar.BP_pair[_int_sequence.get(p + 1)][_int_sequence.get(q)];
								if (_Alpha_stem.get(p,q - p) != -EnergyPar.INF) {
									if (type2 != 0 && !(p == i && q == j)) {
										type2 = EnergyPar.rtype[type2];
										temp = logsumexp(temp, _Alpha_stem.get(p,q - p)
												+ LoopEnergy(type, type2, i, j + 1, p + 1, q));
									}
								}
							}
						}

						// StemEnd¨Multi
						int tt = EnergyPar.rtype[type];
						temp = logsumexp(temp, _Alpha_multi.get(i,j - i) + MLclosing + MLintern
								+ dangle3[tt][_int_sequence.get(i + 1)] + dangle5[tt][_int_sequence.get(j)]);
						_Alpha_stemend.set(i, j - i, temp);
					} else {
						_Alpha_stemend.set(i, j - i, (double) -EnergyPar.INF);
					}
				}
			}
		}

		// Alpha_Outer
		for (int i = 1; i <= _seq_length; i++) {
			double temp = _Alpha_outer.get(i - 1);
			for (int p = Math.max(0, i - _maximal_span - 1); p < i; p++) {
				if (_Alpha_stem.get(p,i - p) != -EnergyPar.INF) {
					int type = EnergyPar.BP_pair[_int_sequence.get(p + 1)][_int_sequence.get(i)];
					double ao = _Alpha_stem.get(p,i - p) + CalcDangleEnergy(type, p, i);
					temp = logsumexp(temp, ao + _Alpha_outer.get(p));
				}
			}
			_Alpha_outer.set(i, temp);
		}
	}
	

	/**
	 * Returns the structural profile in accordance with the StructurePool Interface 
	 * @return array containing <code>[h1,h2,...hn,i1,i2,...,in,b1,b2,...,bn,m1,m2,...mn,d1,d2,...,dn]</code>
	 * for a sequence of size <code>n</code> and where <code>h,i,b,m</code>, and <code>d</code> stand for hairpin, inner loop, bulge loop,
	 * multi-loop, and dangling end respectively.
	 */
	public double[] getStructuralProfile(){ 
		
		// compute the required size of the array and allocate it
		double[] profile = new double[_seq_length*5];
		
		// fill the array
		double pf = _Alpha_outer.get(_seq_length);
		if (pf >= -690 && pf <= 690) {
			CalcBulgeAndInternalProbability2(profile, 1*_seq_length,2*_seq_length);
		} else {
			CalcLogSumBulgeAndInternalProbability2(profile, 1*_seq_length,2*_seq_length);
		}

		CalcHairpinProbability2(profile, 0*_seq_length);

		for (int i = 1; i <= _seq_length; i++) {
			
			profile[4*_seq_length + i -1] = CalcExteriorProbability(i);
			profile[3*_seq_length + i -1] = CalcMultiProbability(i);
			
		}

		// return it
		return profile;
	}
	

	public void CalcStructuralProfile(String name) {
		
		ArrayList<Double> bulge_probability = new ArrayList<Double>(_seq_length);
		ArrayList<Double> internal_probability = new ArrayList<Double>(_seq_length);
		ArrayList<Double> hairpin_probability = new ArrayList<Double>(_seq_length);
		ArrayList<Double> multi_probability = new ArrayList<Double>(_seq_length);
		ArrayList<Double> exterior_probability = new ArrayList<Double>(_seq_length);
		ArrayList<Double> stem_probability = new ArrayList<Double>(_seq_length);

		for (int x = 0; x < _seq_length; x++) {
			bulge_probability.add(0.0);
			internal_probability.add(0.0);
			hairpin_probability.add(0.0);
			multi_probability.add(0.0);
			exterior_probability.add(0.0);
			stem_probability.add(0.0);
		}

		double pf = _Alpha_outer.get(_seq_length);
		if (pf >= -690 && pf <= 690) {
			CalcBulgeAndInternalProbability(bulge_probability, internal_probability);
		} else {
			CalcLogSumBulgeAndInternalProbability(bulge_probability, internal_probability);
		}

		CalcHairpinProbability(hairpin_probability);

		for (int i = 1; i <= _seq_length; i++) {
			exterior_probability.set(i - 1, CalcExteriorProbability(i));
			multi_probability.set(i - 1, CalcMultiProbability(i));
			//stem_probability.set(i - 1, 1.0 - bulge_probability.get(i - 1) - exterior_probability.get(i - 1)
			//		- hairpin_probability.get(i - 1) - internal_probability.get(i - 1) - multi_probability.get(i - 1));
		}

		StringBuilder sb = new StringBuilder();

		sb.append(">" + name + "\n");
		sb.append("Bulge ");
		for (int i = 0; i < _seq_length; i++) {
			sb.append(bulge_probability.get(i) + " ");
		}
		sb.append("\n");
		sb.append("Exterior ");
		for (int i = 0; i < _seq_length; i++) {
			sb.append(exterior_probability.get(i) + " ");
		}
		sb.append("\n");
		sb.append("Hairpin ");
		for (int i = 0; i < _seq_length; i++) {
			sb.append(hairpin_probability.get(i) + " ");
		}
		sb.append("\n");
		sb.append("Internal ");
		for (int i = 0; i < _seq_length; i++) {
			sb.append(internal_probability.get(i) + " ");
		}
		sb.append("\n");
		sb.append("Multibranch ");
		for (int i = 0; i < _seq_length; i++) {
			sb.append(multi_probability.get(i) + " ");
		}
//		sb.append("\n");
//		sb.append("Stem ");
//		for (int i = 0; i < _seq_length; i++) {
//			sb.append(stem_probability.get(i) + " ");
//		}
		sb.append("\n\n");
//		System.out.println(sb.toString());
	}

	private double CalcExteriorProbability(int x) {
		double probability = Math.exp(_Alpha_outer.get(x - 1) + _Beta_outer.get(x) - _Alpha_outer.get(_seq_length));
		return (probability);
	}

	private void CalcHairpinProbability2(double[] profile, int hairpin_offset) {
		for (int x = 1; x <= _seq_length; x++) {
			double temp = 0.0;
			int type = 0;
			boolean flag = false;
			double h_energy = 0.0;

			for (int i = Math.max(1, x - _maximal_span); i < x; i++) {
				for (int j = x + 1; j <= Math.min(i + _maximal_span, _seq_length); j++) {
					type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j)];
					if (_Beta_stemend.get(i,j - i - 1) != -EnergyPar.INF) {
						h_energy = _Beta_stemend.get(i,j - i - 1) + HairpinEnergy(type, i, j);
						temp = flag == true ? logsumexp(temp, h_energy) : h_energy;
						flag = true;
					}
				}
			}

			if (flag == true) {
				profile[hairpin_offset + x - 1] = Math.exp(temp - _Alpha_outer.get(_seq_length));
			} else {
				profile[hairpin_offset + x - 1] = 0.0;
			}
		}
	}
	
	private void CalcHairpinProbability(ArrayList<Double> hairpin_probability) {
		for (int x = 1; x <= _seq_length; x++) {
			double temp = 0.0;
			int type = 0;
			boolean flag = false;
			double h_energy = 0.0;

			for (int i = Math.max(1, x - _maximal_span); i < x; i++) {
				for (int j = x + 1; j <= Math.min(i + _maximal_span, _seq_length); j++) {
					type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j)];
					if (_Beta_stemend.get(i,j - i - 1) != -EnergyPar.INF) {
						h_energy = _Beta_stemend.get(i,j - i - 1) + HairpinEnergy(type, i, j);
						temp = flag == true ? logsumexp(temp, h_energy) : h_energy;
						flag = true;
					}
				}
			}

			if (flag == true) {
				hairpin_probability.set(x - 1, Math.exp(temp - _Alpha_outer.get(_seq_length)));
			} else {
				hairpin_probability.set(x - 1, 0.0);
			}
		}
	}

	private double CalcMultiProbability(int x) {
		double probability = 0.0;
		double temp = 0.0;
		boolean flag = false;

		for (int i = x; i <= Math.min(x + _maximal_span, _seq_length); i++) {
			if (_Beta_multi.get(x - 1,i - x + 1) != -EnergyPar.INF
					&& _Alpha_multi.get(x,i - x) != -EnergyPar.INF) {
				temp = (flag == false) ? _Beta_multi.get(x - 1,i - x + 1) + _Alpha_multi.get(x,i - x)
						: logsumexp(temp, _Beta_multi.get(x - 1,i - x + 1) + _Alpha_multi.get(x,i - x));
				flag = true;
			}
		}

		for (int i = Math.max(0, x - _maximal_span); i < x; i++) {
			if (_Beta_multi2.get(i,x - i) != -EnergyPar.INF
					&& _Alpha_multi2.get(i,x - i - 1) != -EnergyPar.INF) {
				temp = (flag == false) ? _Beta_multi2.get(i,x - i) + _Alpha_multi2.get(i,x - i - 1)
						: logsumexp(temp, _Beta_multi2.get(i,x - i) + _Alpha_multi2.get(i,x - i - 1));
				flag = true;
			}
		}
		if (flag == true) {
			probability = Math.exp(temp - _Alpha_outer.get(_seq_length));
		}
		return (probability);
	}

	
	private void CalcBulgeAndInternalProbability2(double[] profile, int bulge_offset, int internal_offset) {
		double temp = 0;
		int type = 0;
		int type2 = 0;

		for (int i = 1; i < _seq_length - EnergyPar.TURN - 2; i++) {
			for (int j = i + EnergyPar.TURN + 3; j <= Math.min(i + _maximal_span, _seq_length); j++) {
				type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j)];
				if (type != 0) {
					for (int p = i + 1; p <= Math.min(i + EnergyPar.MAXLOOP + 1, j - EnergyPar.TURN - 2); p++) {
						int u1 = p - i - 1;
						for (int q = Math.max(p + EnergyPar.TURN + 1, j - EnergyPar.MAXLOOP + u1 - 1); q < j; q++) {
							type2 = EnergyPar.BP_pair[_int_sequence.get(p)][_int_sequence.get(q)];
							if (type2 != 0 && !(p == i + 1 && q == j - 1)) {
								type2 = EnergyPar.rtype[type2];
								if (_Beta_stemend.get(i,j - i - 1) != -EnergyPar.INF
										&& _Alpha_stem.get(p - 1,q - p + 1) != -EnergyPar.INF) {
									temp = Math.exp(
											_Beta_stemend.get(i,j - i - 1) + LoopEnergy(type, type2, i, j, p, q)
													+ _Alpha_stem.get(p - 1,q - p + 1));

									for (int k = i + 1; k <= p - 1; k++) {
										if (j == q + 1) {
											profile[bulge_offset + k - 1] += temp;
										} else {
											profile[internal_offset + k - 1] += temp;
										}
									}

									for (int k = q + 1; k <= j - 1; k++) {
										if (i == p - 1) {
											profile[bulge_offset + k - 1] += temp;
										} else {
											profile[internal_offset + k - 1] += temp;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < _seq_length; i++) {
			if (profile[bulge_offset + i] != 0) {
				profile[bulge_offset + i] /= Math.exp(_Alpha_outer.get(_seq_length));
			}
			if (profile[internal_offset + i] != 0) {
				profile[internal_offset + i] /= Math.exp(_Alpha_outer.get(_seq_length));
			}
		}
	}	
	
	private void CalcBulgeAndInternalProbability(ArrayList<Double> bulge_probability,
			ArrayList<Double> internal_probability) {
		double temp = 0;
		int type = 0;
		int type2 = 0;

		for (int i = 1; i < _seq_length - EnergyPar.TURN - 2; i++) {
			for (int j = i + EnergyPar.TURN + 3; j <= Math.min(i + _maximal_span, _seq_length); j++) {
				type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j)];
				if (type != 0) {
					for (int p = i + 1; p <= Math.min(i + EnergyPar.MAXLOOP + 1, j - EnergyPar.TURN - 2); p++) {
						int u1 = p - i - 1;
						for (int q = Math.max(p + EnergyPar.TURN + 1, j - EnergyPar.MAXLOOP + u1 - 1); q < j; q++) {
							type2 = EnergyPar.BP_pair[_int_sequence.get(p)][_int_sequence.get(q)];
							if (type2 != 0 && !(p == i + 1 && q == j - 1)) {
								type2 = EnergyPar.rtype[type2];
								if (_Beta_stemend.get(i,j - i - 1) != -EnergyPar.INF
										&& _Alpha_stem.get(p - 1,q - p + 1) != -EnergyPar.INF) {
									temp = Math.exp(
											_Beta_stemend.get(i,j - i - 1) + LoopEnergy(type, type2, i, j, p, q)
													+ _Alpha_stem.get(p - 1,q - p + 1));

									for (int k = i + 1; k <= p - 1; k++) {
										if (j == q + 1) {
											bulge_probability.set(k - 1, bulge_probability.get(k - 1) + temp);
										} else {
											internal_probability.set(k - 1, internal_probability.get(k - 1) + temp);
										}
									}

									for (int k = q + 1; k <= j - 1; k++) {
										if (i == p - 1) {
											bulge_probability.set(k - 1, bulge_probability.get(k - 1) + temp);
										} else {
											internal_probability.set(k - 1, internal_probability.get(k - 1) + temp);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < _seq_length; i++) {
			if (bulge_probability.get(i) != 0) {
				bulge_probability.set(i, bulge_probability.get(i) / Math.exp(_Alpha_outer.get(_seq_length)));
			}
			if (internal_probability.get(i) != 0) {
				internal_probability.set(i, internal_probability.get(i) / Math.exp(_Alpha_outer.get(_seq_length)));
			}
		}
	}
	
	
	private void CalcLogSumBulgeAndInternalProbability2(double[] profile, int bulge_offset, int internal_offset) {
		double temp = 0;
		int type = 0;
		int type2 = 0;

		ArrayList<Boolean> b_flag_array = new ArrayList<Boolean>(_seq_length);
		ArrayList<Boolean> i_flag_array = new ArrayList<Boolean>(_seq_length);
		for (int x = 0; x < _seq_length; x++) {
			b_flag_array.add(false);
			i_flag_array.add(false);
		}

		for (int i = 1; i < _seq_length - EnergyPar.TURN - 2; i++) {
			for (int j = i + EnergyPar.TURN + 3; j <= Math.min(i + _maximal_span, _seq_length); j++) {
				type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j)];
				if (type != 0) {
					for (int p = i + 1; p <= Math.min(i + EnergyPar.MAXLOOP + 1, j - EnergyPar.TURN - 2); p++) {
						int u1 = p - i - 1;
						for (int q = Math.max(p + EnergyPar.TURN + 1, j - EnergyPar.MAXLOOP + u1 - 1); q < j; q++) {
							type2 = EnergyPar.BP_pair[_int_sequence.get(p)][_int_sequence.get(q)];
							if (type2 != 0 && !(p == i + 1 && q == j - 1)) {
								type2 = EnergyPar.rtype[type2];
								if (_Beta_stemend.get(i,j - i - 1) != -EnergyPar.INF
										&& _Alpha_stem.get(p - 1,q - p + 1) != -EnergyPar.INF) {
									temp = _Beta_stemend.get(i,j - i - 1) + LoopEnergy(type, type2, i, j, p, q)
											+ _Alpha_stem.get(p - 1,q - p + 1);

									for (int k = i + 1; k <= p - 1; k++) {
										if (j == q + 1) {
											profile[bulge_offset + k - 1] = (b_flag_array.get(k - 1) == true)
													? logsumexp(profile[bulge_offset + k - 1], temp) : temp;
											b_flag_array.set(k - 1, true);
										} else {
											profile[internal_offset + k - 1] = (i_flag_array.get(k - 1) == true)
													? logsumexp(profile[internal_offset + k - 1], temp) : temp;
											i_flag_array.set(k - 1, true);
										}
									}

									for (int k = q + 1; k <= j - 1; k++) {
										if (i == p - 1) {
											profile[bulge_offset + k - 1] = (b_flag_array.get(k - 1) == true)
													? logsumexp(profile[bulge_offset + k - 1], temp) : temp;
											b_flag_array.set(k - 1, true);
										} else {
											profile[internal_offset + k - 1] = (i_flag_array.get(k - 1) == true)
													? logsumexp(profile[internal_offset + k - 1], temp) : temp;
											i_flag_array.set(k - 1, true);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < _seq_length; i++) {
			if (b_flag_array.get(i) == true) {
				profile[bulge_offset + i] = Math.exp(profile[bulge_offset + i] - _Alpha_outer.get(_seq_length));
			}
			if (i_flag_array.get(i) == true) {
				profile[internal_offset + i] = Math.exp(profile[internal_offset + i] - _Alpha_outer.get(_seq_length));
			}
		}
	}

	

	private void CalcLogSumBulgeAndInternalProbability(ArrayList<Double> bulge_probability,
			ArrayList<Double> internal_probability) {
		double temp = 0;
		int type = 0;
		int type2 = 0;

		ArrayList<Boolean> b_flag_array = new ArrayList<Boolean>(_seq_length);
		ArrayList<Boolean> i_flag_array = new ArrayList<Boolean>(_seq_length);
		for (int x = 0; x < _seq_length; x++) {
			b_flag_array.add(false);
			i_flag_array.add(false);
		}

		for (int i = 1; i < _seq_length - EnergyPar.TURN - 2; i++) {
			for (int j = i + EnergyPar.TURN + 3; j <= Math.min(i + _maximal_span, _seq_length); j++) {
				type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j)];
				if (type != 0) {
					for (int p = i + 1; p <= Math.min(i + EnergyPar.MAXLOOP + 1, j - EnergyPar.TURN - 2); p++) {
						int u1 = p - i - 1;
						for (int q = Math.max(p + EnergyPar.TURN + 1, j - EnergyPar.MAXLOOP + u1 - 1); q < j; q++) {
							type2 = EnergyPar.BP_pair[_int_sequence.get(p)][_int_sequence.get(q)];
							if (type2 != 0 && !(p == i + 1 && q == j - 1)) {
								type2 = EnergyPar.rtype[type2];
								if (_Beta_stemend.get(i,j - i - 1) != -EnergyPar.INF
										&& _Alpha_stem.get(p - 1,q - p + 1) != -EnergyPar.INF) {
									temp = _Beta_stemend.get(i,j - i - 1) + LoopEnergy(type, type2, i, j, p, q)
											+ _Alpha_stem.get(p - 1,q - p + 1);

									for (int k = i + 1; k <= p - 1; k++) {
										if (j == q + 1) {
											bulge_probability.set(k - 1, (b_flag_array.get(k - 1) == true)
													? logsumexp(bulge_probability.get(k - 1), temp) : temp);
											b_flag_array.set(k - 1, true);
										} else {
											internal_probability.set(k - 1, (i_flag_array.get(k - 1) == true)
													? logsumexp(internal_probability.get(k - 1), temp) : temp);
											i_flag_array.set(k - 1, true);
										}
									}

									for (int k = q + 1; k <= j - 1; k++) {
										if (i == p - 1) {
											bulge_probability.set(k - 1, (b_flag_array.get(k - 1) == true)
													? logsumexp(bulge_probability.get(k - 1), temp) : temp);
											b_flag_array.set(k - 1, true);
										} else {
											internal_probability.set(k - 1, (i_flag_array.get(k - 1) == true)
													? logsumexp(internal_probability.get(k - 1), temp) : temp);
											i_flag_array.set(k - 1, true);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < _seq_length; i++) {
			if (b_flag_array.get(i) == true) {
				bulge_probability.set(i, Math.exp(bulge_probability.get(i) - _Alpha_outer.get(_seq_length)));
			}
			if (i_flag_array.get(i) == true) {
				internal_probability.set(i, Math.exp(internal_probability.get(i) - _Alpha_outer.get(_seq_length)));
			}
		}
	}

	private double CalcDangleEnergy(int type, int a, int b) {
		double x = 0;
		if (type != 0) {
			if (a > 0)
				x += dangle5[type][_int_sequence.get(a)];
			if (b < _seq_length)
				x += dangle3[type][_int_sequence.get(b + 1)];
			if (b == _seq_length && type > 2) {
				x += TermAU;
			}
		}
		return (x);
	}

	private void CalcOutsideVariable() {
		// Beta_outer
		for (int i = _seq_length - 1; i >= 0; i--) {
			double temp = _Beta_outer.get(i + 1);
			for (int p = i + 1; p <= Math.min(i + _maximal_span + 1, _seq_length); p++) {
				if (_Alpha_stem.get(i,p - i) != -EnergyPar.INF) {
					int type = EnergyPar.BP_pair[_int_sequence.get(i + 1)][_int_sequence.get(p)];
					double bo = _Alpha_stem.get(i,p - i) + CalcDangleEnergy(type, i, p);
					temp = logsumexp(temp, bo + _Beta_outer.get(p));
				}
			}
			_Beta_outer.set(i, temp);
		}

		for (int q = _seq_length; q >= EnergyPar.TURN + 1; q--) {
			for (int p = Math.max(0, q - _maximal_span - 1); p <= q - EnergyPar.TURN; p++) {
				int type = 0;
				int type2 = 0;

				double temp = 0;
				boolean flag = false;
				if (p != 0 && q != _seq_length) {
					// Beta_stemend
					_Beta_stemend.set(p, q - p,
							(q - p >= _maximal_span) ? -EnergyPar.INFD : _Beta_stem.get(p - 1,q - p + 2));

					// Beta_Multi
					flag = false;
					if (q - p + 1 <= _maximal_span + 1) {
						if (_Beta_multi.get(p - 1,q - p + 1) != -EnergyPar.INF) {
							temp = _Beta_multi.get(p - 1,q - p + 1) + MLbase;
							flag = true;
						}
					}

					type = EnergyPar.BP_pair[_int_sequence.get(p)][_int_sequence.get(q + 1)];
					int tt = EnergyPar.rtype[type];
					if (flag == true) {
						if (_Beta_stemend.get(p,q - p) != -EnergyPar.INF) {
							temp = logsumexp(temp, _Beta_stemend.get(p,q - p) + MLclosing + MLintern
									+ dangle3[tt][_int_sequence.get(p + 1)] + dangle5[tt][_int_sequence.get(q)]);
						}
					} else {
						if (_Beta_stemend.get(p,q - p) != -EnergyPar.INF) {
							temp = _Beta_stemend.get(p,q - p) + MLclosing + MLintern
									+ dangle3[tt][_int_sequence.get(p + 1)] + dangle5[tt][_int_sequence.get(q)];
						} else {
							temp = -EnergyPar.INF;
						}
					}
					_Beta_multi.set(p, q - p, temp);

					// Beta_Multi1
					temp = 0;
					flag = false;
					for (int k = q + 1; k <= Math.min(_seq_length, p + _maximal_span); k++) {
						if (_Beta_multibif.get(p,k - p) != -EnergyPar.INF
								&& _Alpha_multi2.get(q,k - q) != -EnergyPar.INF) {
							temp = (flag == false) ? _Beta_multibif.get(p,k - p) + _Alpha_multi2.get(q,k - q)
									: logsumexp(temp,
											_Beta_multibif.get(p,k - p) + _Alpha_multi2.get(q,k - q));
							flag = true;
						}
					}
					_Beta_multi1.set(p, q - p, (flag == true) ? temp : -EnergyPar.INF);

					// Beta_Multi2
					temp = 0;
					flag = false;
					if (_Beta_multi1.get(p,q - p) != -EnergyPar.INF) {
						temp = _Beta_multi1.get(p,q - p);
						flag = true;
					}
					if (q - p <= _maximal_span) {
						if (_Beta_multi2.get(p,q - p + 1) != -EnergyPar.INF) {
							temp = (flag == true) ? logsumexp(temp, _Beta_multi2.get(p,q - p + 1) + MLbase)
									: _Beta_multi2.get(p,q - p + 1) + MLbase;
							flag = true;
						}
					}

					for (int k = Math.max(0, q - _maximal_span); k < p; k++) {
						if (_Beta_multibif.get(k,q - k) != -EnergyPar.INF
								&& _Alpha_multi1.get(k,p - k) != -EnergyPar.INF) {
							temp = (flag == false) ? _Beta_multibif.get(k,q - k) + _Alpha_multi1.get(k,p - k)
									: logsumexp(temp,
											_Beta_multibif.get(k,q - k) + _Alpha_multi1.get(k,p - k));
							flag = true;
						}
					}
					_Beta_multi2.set(p, q - p, (flag == false) ? -EnergyPar.INF : temp);

					// Beta_multibif
					if (_Beta_multi1.get(p,q - p) != -EnergyPar.INF
							&& _Beta_multi.get(p,q - p) != -EnergyPar.INF) {
						_Beta_multibif.set(p, q - p,
								logsumexp(_Beta_multi1.get(p,q - p), _Beta_multi.get(p,q - p)));
					} else if (_Beta_multi.get(p,q - p) == -EnergyPar.INF) {
						_Beta_multibif.set(p, q - p, _Beta_multi1.get(p,q - p));
					} else if (_Beta_multi1.get(p,q - p) == -EnergyPar.INF) {
						_Beta_multibif.set(p, q - p, _Beta_multi.get(p,q - p));
					} else {
						_Beta_multibif.set(p, q - p, (double) -EnergyPar.INF);
					}

				}

				// Beta_stem
				type2 = EnergyPar.BP_pair[_int_sequence.get(p + 1)][_int_sequence.get(q)];
				if (type2 != 0) {
					temp = _Alpha_outer.get(p) + _Beta_outer.get(q) + CalcDangleEnergy(type2, p, q);

					type2 = EnergyPar.rtype[type2];
					for (int i = Math.max(1, p - EnergyPar.MAXLOOP); i <= p; i++) {
						for (int j = q; j <= Math.min(q + EnergyPar.MAXLOOP - p + i, _seq_length - 1); j++) {
							type = EnergyPar.BP_pair[_int_sequence.get(i)][_int_sequence.get(j + 1)];
							if (type != 0 && !(i == p && j == q)) {
								if (j - i <= _maximal_span + 1 && _Beta_stemend.get(i,j - i) != -EnergyPar.INF) {
									temp = logsumexp(temp, _Beta_stemend.get(i,j - i)
											+ LoopEnergy(type, type2, i, j + 1, p + 1, q));
								}
							}
						}
					}

					if (p != 0 && q != _seq_length) {
						type = EnergyPar.BP_pair[_int_sequence.get(p)][_int_sequence.get(q + 1)];
						if (type != 0) {
							if (q - p + 2 <= _maximal_span + 1
									&& _Beta_stem.get(p - 1,q - p + 2) != -EnergyPar.INF) {
								temp = logsumexp(temp, _Beta_stem.get(p - 1,q - p + 2)
										+ LoopEnergy(type, type2, p, q + 1, p + 1, q));
							}
						}
					}
					_Beta_stem.set(p, q - p, temp);

					if (_Beta_multi2.get(p,q - p) != -EnergyPar.INF) {
						type2 = EnergyPar.rtype[type2];
						temp = _Beta_multi2.get(p,q - p) + MLintern + CalcDangleEnergy(type2, p, q);
						_Beta_stem.set(p, q - p, logsumexp(temp, _Beta_stem.get(p,q - p)));
					}
				} else {
					_Beta_stem.set(p, q - p, (double) -EnergyPar.INF);
				}
			}
		}
	}

	private double logsumexp(double x, double y) {
		double temp = x > y ? x + Math.log(Math.exp(y - x) + 1.0) : y + Math.log(Math.exp(x - y) + 1.0);
		return (temp);
	}

	private double LoopEnergy(int type, int type2, int i, int j, int p, int q) {
		double z = 0;
		int u1 = p - i - 1;
		int u2 = j - q - 1;

		if ((u1 == 0) && (u2 == 0)) {
			z = stack[type][type2];
		} else {
			if ((u1 == 0) || (u2 == 0)) {
				int u;
				u = u1 == 0 ? u2 : u1;
				z = u <= 30 ? bulge[u] : bulge[30] - EnergyPar.lxc37 * Math.log(u / 30.) * 10. / EnergyPar.kT;

				if (u == 1) {
					z += stack[type][type2];
				} else {
					if (type > 2) {
						z += TermAU;
					}
					if (type2 > 2) {
						z += TermAU;
					}
				}
			} else {
				if (u1 + u2 == 2) {
					z = int11[type][type2][_int_sequence.get(i + 1)][_int_sequence.get(j - 1)];
				} else if ((u1 == 1) && (u2 == 2)) {
					z = int21[type][type2][_int_sequence.get(i + 1)][_int_sequence.get(q + 1)][_int_sequence
							.get(j - 1)];
				} else if ((u1 == 2) && (u2 == 1)) {
					z = int21[type2][type][_int_sequence.get(q + 1)][_int_sequence.get(i + 1)][_int_sequence
							.get(p - 1)];
				} else if ((u1 == 2) && (u2 == 2)) {
					z = int22[type][type2][_int_sequence.get(i + 1)][_int_sequence.get(p - 1)][_int_sequence
							.get(q + 1)][_int_sequence.get(j - 1)];
				} else {
					z = internal[u1 + u2] + mismatchI[type][_int_sequence.get(i + 1)][_int_sequence.get(j - 1)]
							+ mismatchI[type2][_int_sequence.get(q + 1)][_int_sequence.get(p - 1)];
					z += ninio[Math.abs(u1 - u2)];
				}
			}
		}
		return z;
	}

	private double HairpinEnergy(int type, int i, int j) {
		int d = j - i - 1;
		double q = 0;

		q = d <= 30 ? hairpin[d] : hairpin[30] - EnergyPar.lxc37 * Math.log(d / 30.) * 10. / EnergyPar.kT;
		if (d != 3) {
			q += mismatchH[type][_int_sequence.get(i + 1)][_int_sequence.get(j - 1)];
		} else {
			if (type > 2) {
				q += TermAU;
			}
		}
		return q;
	}

	private void Clear() {
		
		_int_sequence.clear();
		_seq_length = 0;
		
		_Alpha_outer.clear();
		_Beta_outer.clear();
		
	}

}