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
 */
public class ParamT implements Cloneable{
	public int id;
	public int[][] stack = new int[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1];
	public int[] hairpin = new int[31];
	public int[] bulge = new int[EnergyConst.MAXLOOP + 1];
	public int[] internal_loop = new int[EnergyConst.MAXLOOP + 1];
	public int[][][] mismatchI = new int[EnergyConst.NBPAIRS + 1][5][5];
	public int[][][] mismatchH = new int[EnergyConst.NBPAIRS + 1][5][5];
	public int[][][] mismatchM = new int[EnergyConst.NBPAIRS + 1][5][5];
	public int[][] dangle5 = new int[EnergyConst.NBPAIRS + 1][5];
	public int[][] dangle3 = new int[EnergyConst.NBPAIRS + 1][5];
	public int[][][][] int11 = new int[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5];
	public int[][][][][] int21 = new int[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5][5];
	public int[][][][][][] int22 = new int[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5][5][5];
	public int[] F_ninio = new int[5];
	public double lxc;
	public int MLbase;
	public int[] MLintern = new int[EnergyConst.NBPAIRS + 1];
	public int MLclosing;
	public int TerminalAU;
	public int DuplexInit;
	public int[] TETRA_ENERGY = new int[200];
	public String Tetraloops;
	public int[] Triloop_E = new int[40];
	public String Triloops;
	public double temperature;

	
	public void set(ParamT p) {
		
		this.id = p.id;
		Utils.CopyArray(p.stack, this.stack);
		Utils.CopyArray(p.hairpin, this.hairpin);
		Utils.CopyArray(p.bulge, this.bulge);
		Utils.CopyArray(p.internal_loop, this.internal_loop);
		Utils.CopyArray(p.mismatchI, this.mismatchI);
		Utils.CopyArray(p.mismatchH, this.mismatchH);
		Utils.CopyArray(p.mismatchM, this.mismatchM);
		Utils.CopyArray(p.dangle5, this.dangle5);
		Utils.CopyArray(p.dangle3, this.dangle3);
		Utils.CopyArray(p.int11, this.int11);
		Utils.CopyArray(p.int21, this.int21);
		Utils.CopyArray(p.int22, this.int22);
		Utils.CopyArray(p.F_ninio, F_ninio);
		this.lxc = p.lxc;
		this.MLbase = p.MLbase;
		Utils.CopyArray(p.MLintern, this.MLintern);
		this.MLclosing = p.MLclosing;
		this.TerminalAU = p.TerminalAU;
		this.DuplexInit = p.DuplexInit;
		Utils.CopyArray(p.TETRA_ENERGY, this.TETRA_ENERGY);
		this.Tetraloops = p.Tetraloops;
		Utils.CopyArray(p.Triloop_E, this.Triloop_E);
		this.Triloops = p.Triloops;
		this.temperature = p.temperature;
		
	}
	
	
	public ParamT clone() {
		
		ParamT clone = new ParamT();
		
		clone.id = id;
		Utils.CopyArray(stack, clone.stack);
		Utils.CopyArray(hairpin, clone.hairpin);
		Utils.CopyArray(bulge, clone.bulge);
		Utils.CopyArray(internal_loop, clone.internal_loop);
		Utils.CopyArray(mismatchI, clone.mismatchI);
		Utils.CopyArray(mismatchH, clone.mismatchH);
		Utils.CopyArray(mismatchM, clone.mismatchM);
		Utils.CopyArray(dangle5, clone.dangle5);
		Utils.CopyArray(dangle3, clone.dangle3);
		Utils.CopyArray(int11, clone.int11);
		Utils.CopyArray(int21, clone.int21);
		Utils.CopyArray(int22, clone.int22);
		Utils.CopyArray(F_ninio, F_ninio);
		clone.lxc = lxc;
		clone.MLbase = MLbase;
		Utils.CopyArray(MLintern, clone.MLintern);
		clone.MLclosing = MLclosing;
		clone.TerminalAU = TerminalAU;
		clone.DuplexInit = DuplexInit;
		Utils.CopyArray(TETRA_ENERGY, clone.TETRA_ENERGY);
		clone.Tetraloops = Tetraloops;
		Utils.CopyArray(Triloop_E, clone.Triloop_E);
		clone.Triloops = Triloops;
		clone.temperature = temperature;
		
		return clone;
		
	}
	
	
	
	
}
