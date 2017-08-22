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
public class PFParamT implements Cloneable{

	public int id;
	public double[][] expstack = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1];
	public double[] exphairpin = new double[31];
	public double[] expbulge = new double[EnergyConst.MAXLOOP + 1];
	public double[] expinternal = new double[EnergyConst.MAXLOOP + 1];
	public double[][][] expmismatchI = new double[EnergyConst.NBPAIRS + 1][5][5];
	public double[][][] expmismatchH = new double[EnergyConst.NBPAIRS + 1][5][5];
	public double[][][] expmismatchM = new double[EnergyConst.NBPAIRS + 1][5][5];
	public double[][] expdangle5 = new double[EnergyConst.NBPAIRS + 1][5];
	public double[][] expdangle3 = new double[EnergyConst.NBPAIRS + 1][5];
	public double[][][][] expint11 = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5];
	public double[][][][][] expint21 = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5][5];
	public double[][][][][][] expint22 = new double[EnergyConst.NBPAIRS + 1][EnergyConst.NBPAIRS + 1][5][5][5][5];
	public double[][] expninio = new double[5][EnergyConst.MAXLOOP + 1];
	public double lxc;
	public double expMLbase;
	public double[] expMLintern = new double[EnergyConst.NBPAIRS + 1];
	public double expMLclosing;
	public double expTermAU;
	public double expDuplexInit;
	public double[] exptetra = new double[40];
	public String Tetraloops;
	public double[] expTriloop = new double[40];
	public String Triloops;
	public double temperature;

	
	public void set(PFParamT p) {
		
		this.id = p.id;
		Utils.CopyArray(p.expstack, this.expstack);
		Utils.CopyArray(p.exphairpin, this.exphairpin);
		Utils.CopyArray(p.expbulge, this.expbulge);
		Utils.CopyArray(p.expinternal, this.expinternal);
		Utils.CopyArray(p.expmismatchI, this.expmismatchI);
		Utils.CopyArray(p.expmismatchH, this.expmismatchH);
		Utils.CopyArray(p.expmismatchM, this.expmismatchM);
		Utils.CopyArray(p.expdangle5, this.expdangle5);
		Utils.CopyArray(p.expdangle3, this.expdangle3);
		Utils.CopyArray(p.expint11, this.expint11);
		Utils.CopyArray(p.expint21, this.expint21);
		Utils.CopyArray(p.expint22, this.expint22);
		Utils.CopyArray(p.expninio, this.expninio);
		this.lxc = p.lxc;
		this.expMLbase = p.expMLbase;
		Utils.CopyArray(p.expMLintern, this.expMLintern);
		this.expMLclosing = p.expMLclosing;
		this.expTermAU = p.expTermAU;
		this.expDuplexInit = p.expDuplexInit;
		Utils.CopyArray(p.exptetra, this.exptetra);
		this.Tetraloops = p.Tetraloops;
		Utils.CopyArray(p.expTriloop, this.expTriloop);
		this.Triloops = p.Triloops;
		this.temperature = p.temperature;
		
	}
	
	public PFParamT clone() {
		
		PFParamT clone = new PFParamT();
		
		clone.id = id;
		Utils.CopyArray(expstack, clone.expstack);
		Utils.CopyArray(exphairpin, clone.exphairpin);
		Utils.CopyArray(expbulge, clone.expbulge);
		Utils.CopyArray(expinternal, clone.expinternal);
		Utils.CopyArray(expmismatchI, clone.expmismatchI);
		Utils.CopyArray(expmismatchH, clone.expmismatchH);
		Utils.CopyArray(expmismatchM, clone.expmismatchM);
		Utils.CopyArray(expdangle5, clone.expdangle5);
		Utils.CopyArray(expdangle3, clone.expdangle3);
		Utils.CopyArray(expint11, clone.expint11);
		Utils.CopyArray(expint21, clone.expint21);
		Utils.CopyArray(expint22, clone.expint22);
		Utils.CopyArray(expninio, clone.expninio);
		clone.lxc = lxc;
		clone.expMLbase = expMLbase;
		Utils.CopyArray(expMLintern, clone.expMLintern);
		clone.expMLclosing = expMLclosing;
		clone.expTermAU = expTermAU;
		clone.expDuplexInit = expDuplexInit;
		Utils.CopyArray(exptetra, clone.exptetra);
		clone.Tetraloops = Tetraloops;
		Utils.CopyArray(expTriloop, clone.expTriloop);
		clone.Triloops = Triloops;
		clone.temperature = temperature;
		
		return clone;
	}
	
}
