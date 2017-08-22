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
public class Params {

	FoldVars fold_vars;
	public ParamT p = new ParamT();
	
	public int id=-1;
	/* variables for partition function */
	PFParamT pf = new PFParamT();
	int pf_id=-1;

	public Params(FoldVars fold_vars) {
		
		this.fold_vars = fold_vars;
		
	}
	
	public ParamT scale_parameters()
	{
	  int i,j,k,l;
	  double tempf;

	  tempf = ((fold_vars.temperature+EnergyConst.K0)/EnergyPar.Tmeasure);
	  	  
	  for (i=0; i<31; i++) 
	    p.hairpin[i] = (int) ((double) EnergyPar.hairpin37[i]*(tempf));
	  for (i=0; i<=Math.min(30,EnergyConst.MAXLOOP); i++) {
	    p.bulge[i] = (int) ((double) EnergyPar.bulge37[i]*tempf);
	    p.internal_loop[i]= (int) ((double) EnergyPar.internal_loop37[i]*tempf);
	  }
	  p.lxc = EnergyPar.lxc37*tempf;
	  for (; i<=EnergyConst.MAXLOOP; i++) {
	    p.bulge[i] = p.bulge[30]+(int)(p.lxc*Math.log((double)(i)/30.));
	    p.internal_loop[i] = p.internal_loop[30]+(int)(p.lxc*Math.log((double)(i)/30.));
	  }
	  for (i=0; i<5; i++)
	    p.F_ninio[i] = (int) ((double) EnergyPar.F_ninio37[i]*tempf);
	   
	  for (i=0; (i*7)<EnergyPar.Tetraloops.length(); i++) 
	    p.TETRA_ENERGY[i] = (int) (EnergyPar.TETRA_ENTH37 - (EnergyPar.TETRA_ENTH37 - EnergyPar.TETRA_ENERGY37[i])*tempf);
	  for (i=0; (i*5)<EnergyPar.Triloops.length(); i++) 
	    p.Triloop_E[i] =  EnergyPar.Triloop_E37[i];
	   
	  p.MLbase = (int) (EnergyPar.ML_BASE37*tempf);
	  for (i=0; i<=EnergyConst.NBPAIRS; i++) { /* includes AU penalty */
	    p.MLintern[i] = (int) (EnergyPar.ML_intern37*tempf);
	    p.MLintern[i] +=  (i>2)?EnergyPar.TerminalAU:0;
	  }
	  p.MLclosing = (int) (EnergyPar.ML_closing37*tempf);

	  p.TerminalAU = EnergyPar.TerminalAU;
	  
	  p.DuplexInit = (int) (EnergyPar.DuplexInit*tempf);

	  /* stacks    G(T) = H - [H - G(T0)]*T/T0 */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      p.stack[i][j] = (int) (EnergyPar.enthalpies[i][j] -
		(EnergyPar.enthalpies[i][j] - EnergyPar.stack37[i][j])*tempf);

	  /* mismatches */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<5; j++)
	      for (k=0; k<5; k++) {
		p.mismatchI[i][j][k] = (int) (EnergyPar.mism_H[i][j][k] -
		  (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchI37[i][j][k])*tempf);
		p.mismatchH[i][j][k] = (int) (EnergyPar.mism_H[i][j][k] -
		  (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchH37[i][j][k])*tempf);
		p.mismatchM[i][j][k] = (int) (EnergyPar.mism_H[i][j][k] -
		  (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchM37[i][j][k])*tempf);
	      }
	   
	  /* dangles */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<5; j++) {
	      int dd;
	      dd = (int) (EnergyPar.dangle5_H[i][j] - (EnergyPar.dangle5_H[i][j] - EnergyPar.dangle5_37[i][j])*tempf); 
	      p.dangle5[i][j] = (dd>0) ? 0 : dd;  /* must be <= 0 */
	      dd = (int) (EnergyPar.dangle3_H[i][j] - (EnergyPar.dangle3_H[i][j] - EnergyPar.dangle3_37[i][j])*tempf);
	      p.dangle3[i][j] = (dd>0) ? 0 : dd;  /* must be <= 0 */
	    }
	  /* interior 1x1 loops */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      for (k=0; k<5; k++)
		for (l=0; l<5; l++) 
		  p.int11[i][j][k][l] = (int) (EnergyPar.int11_H[i][j][k][l] -
		    (EnergyPar.int11_H[i][j][k][l] - EnergyPar.int11_37[i][j][k][l])*tempf);

	  /* interior 2x1 loops */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      for (k=0; k<5; k++)
		for (l=0; l<5; l++) {
		  int m;
		  for (m=0; m<5; m++)
		    p.int21[i][j][k][l][m] = (int) (EnergyPar.int21_H[i][j][k][l][m] -
		      (EnergyPar.int21_H[i][j][k][l][m] - EnergyPar.int21_37[i][j][k][l][m])*tempf);
		}
	  /* interior 2x2 loops */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      for (k=0; k<5; k++)
		for (l=0; l<5; l++) {
		  int m,n;
		  for (m=0; m<5; m++)
		    for (n=0; n<5; n++)	     
		      p.int22[i][j][k][l][m][n] = (int) (EnergyPar.int22_H[i][j][k][l][m][n] -
			(EnergyPar.int22_H[i][j][k][l][m][n]-EnergyPar.int22_37[i][j][k][l][m][n])*tempf);
		}

	  p.Tetraloops = EnergyPar.Tetraloops;
	  p.Triloops = EnergyPar.Triloops;

	  p.temperature = fold_vars.temperature;
	  p.id = ++id;
	  return p;
	}

	ParamT copy_parameters() {
	  if (p.id != id) scale_parameters();
	  
	  return p.clone();
	}

	ParamT set_parameters(ParamT dest) {
	  p.set(dest);
	  return p;
	}

	/*------------------------------------------------------------------------*/
	/* functions for partition function */

	/* dangling ends should never be destabilizing, i.e. expdangle>=1         */
	/* specific heat needs smooth function (2nd derivative)                   */
	/* we use a*(sin(x+b)+1)^2, with a=2/(3*sqrt(3)), b=Pi/6-sqrt(3)/2,       */
	/* in the interval b<x<sqrt(3)/2                                          */

	int SCALE = 10;
	
	double SMOOTH(double X) { 
		return ((X)/SCALE<-1.2283697)?0:(((X)/SCALE>0.8660254)?(X):SCALE*0.38490018*(Math.sin((X)/SCALE-0.34242663)+1)*(Math.sin((X)/SCALE-0.34242663)+1));
			
	}			
			
	PFParamT scale_pf_parameters()  {  
	  
		/* scale energy parameters and pre-calculate Boltzmann weights */
	  int i, j, k, l;
	  double  kT, TT;
	  double  GT;

	  /* scale pf_params() in partfunc.c is only a wrapper, that calls
	     this functions !! */

	  pf.temperature = fold_vars.temperature;
	  kT = (pf.temperature+EnergyConst.K0)*EnergyConst.GASCONST;   /* kT in cal/mol  */
	  TT = (pf.temperature+EnergyConst.K0)/(EnergyPar.Tmeasure);

	   /* loop energies: hairpins, bulges, interior, mulit-loops */
	  for (i=0; i<31; i++) {
	    GT =  EnergyPar.hairpin37[i]*TT;
	    pf.exphairpin[i] = Math.exp( -GT*10./kT);
	  }
	  for (i=0; i<=Math.min(30, EnergyConst.MAXLOOP); i++) {
	    GT =  EnergyPar.bulge37[i]*TT;
	    pf.expbulge[i] = Math.exp( -GT*10./kT);
	    GT =  EnergyPar.internal_loop37[i]*TT;
	    pf.expinternal[i] = Math.exp( -GT*10./kT);
	  }
	  /* special case of size 2 interior loops (single mismatch) */
	  if (fold_vars.james_rule != 0) pf.expinternal[2] = Math.exp( -80*10/kT);
	   
	  pf.lxc = EnergyPar.lxc37*TT;
	  
	  GT =  EnergyPar.DuplexInit*TT;
	  pf.expDuplexInit = Math.exp( -GT*10./kT);
	  
	  for (i=31; i<=EnergyConst.MAXLOOP; i++) {
	    GT = EnergyPar.bulge37[30]*TT + (pf.lxc*Math.log( i/30.));
	    pf.expbulge[i] = Math.exp( -GT*10./kT);
	    GT = EnergyPar.internal_loop37[30]*TT + (pf.lxc*Math.log( i/30.));
	    pf.expinternal[i] = Math.exp( -GT*10./kT);
	  }

	  for (i=0; i<5; i++) {
	    GT = EnergyPar.F_ninio37[i]*TT;
	    for (j=0; j<=EnergyConst.MAXLOOP; j++)
	      pf.expninio[i][j]=Math.exp(-Math.min(EnergyPar.MAX_NINIO,j*GT)*10/kT);
	  }
	  for (i=0; (i*7)<EnergyPar.Tetraloops.length(); i++) {
	    GT = EnergyPar.TETRA_ENTH37 - (EnergyPar.TETRA_ENTH37-EnergyPar.TETRA_ENERGY37[i])*TT;
	    pf.exptetra[i] = Math.exp( -GT*10./kT);
	  }
	  for (i=0; (i*5)<EnergyPar.Triloops.length(); i++) 
	    pf.expTriloop[i] = Math.exp(-EnergyPar.Triloop_E37[i]*10/kT);

	  GT =  EnergyPar.ML_closing37*TT;
	  pf.expMLclosing = Math.exp( -GT*10/kT);

	  for (i=0; i<=EnergyConst.NBPAIRS; i++) { /* includes AU penalty */
	    GT =  EnergyPar.ML_intern37*TT;
	    /* if (i>2) GT += TerminalAU; */
	    pf.expMLintern[i] = Math.exp( -GT*10./kT);
	  }
	  pf.expTermAU = Math.exp(-EnergyPar.TerminalAU*10/kT);

	  GT = EnergyPar.ML_BASE37*TT;
	  /* pf.expMLbase=(-10.*GT/kT); old */
	  pf.expMLbase=Math.exp(-10.*GT/kT);
	  
	 
	  /* if dangles==0 just set their energy to 0,
	     don't let dangle energies become > 0 (at large temps),
	     but make sure go smoothly to 0                        */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=4; j++) {
	      if (fold_vars.dangles != 0) {
		GT = EnergyPar.dangle5_H[i][j] - (EnergyPar.dangle5_H[i][j] - EnergyPar.dangle5_37[i][j])*TT;
		pf.expdangle5[i][j] = Math.exp(SMOOTH(-GT)*10./kT);
		GT = EnergyPar.dangle3_H[i][j] - (EnergyPar.dangle3_H[i][j] - EnergyPar.dangle3_37[i][j])*TT;
		pf.expdangle3[i][j] =  Math.exp(SMOOTH(-GT)*10./kT);
	      } else
		pf.expdangle3[i][j] = pf.expdangle5[i][j] = 1;
	      if (i>2) /* add TermAU penalty into dangle3 */
		pf.expdangle3[i][j] *= pf.expTermAU;
	    }

	  /* stacking energies */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++) {
	      GT =  EnergyPar.enthalpies[i][j] - (EnergyPar.enthalpies[i][j] - EnergyPar.stack37[i][j])*TT;
	      pf.expstack[i][j] = Math.exp( -GT*10/kT);
	    }

	  /* mismatch energies */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<5; j++)
	      for (k=0; k<5; k++) {
		GT = EnergyPar.mism_H[i][j][k] - (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchI37[i][j][k])*TT;
		pf.expmismatchI[i][j][k] = Math.exp(-GT*10.0/kT);
		GT = EnergyPar.mism_H[i][j][k] - (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchH37[i][j][k])*TT;
		pf.expmismatchH[i][j][k] = Math.exp(-GT*10.0/kT);
		GT = EnergyPar.mism_H[i][j][k] - (EnergyPar.mism_H[i][j][k] - EnergyPar.mismatchM37[i][j][k])*TT;
		pf.expmismatchM[i][j][k] = Math.exp(-GT*10.0/kT);
	      }
	  
	  
	  /* interior lops of length 2 */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      for (k=0; k<5; k++)
		for (l=0; l<5; l++) {
		  GT = EnergyPar.int11_H[i][j][k][l] -
		    (EnergyPar.int11_H[i][j][k][l] - EnergyPar.int11_37[i][j][k][l])*TT;
		  pf.expint11[i][j][k][l] = Math.exp(-GT*10./kT);
		}
	  /* interior 2x1 loops */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      for (k=0; k<5; k++)
		for (l=0; l<5; l++) {
		  int m;
		  for (m=0; m<5; m++) {
		    GT = EnergyPar.int21_H[i][j][k][l][m] - 
		      (EnergyPar.int21_H[i][j][k][l][m] - EnergyPar.int21_37[i][j][k][l][m])*TT;
		    pf.expint21[i][j][k][l][m] = Math.exp(-GT*10./kT);
		  }
		}

	  /* interior 2x2 loops */
	  for (i=0; i<=EnergyConst.NBPAIRS; i++)
	    for (j=0; j<=EnergyConst.NBPAIRS; j++)
	      for (k=0; k<5; k++)
		for (l=0; l<5; l++) {
		  int m,n;
		  for (m=0; m<5; m++)
		    for (n=0; n<5; n++) {            
		      GT = EnergyPar.int22_H[i][j][k][l][m][n] -
			(EnergyPar.int22_H[i][j][k][l][m][n]-EnergyPar.int22_37[i][j][k][l][m][n])*TT;
		      pf.expint22[i][j][k][l][m][n] = Math.exp(-GT*10./kT);
		    }
		}

	  pf.Tetraloops = EnergyPar.Tetraloops;
	  pf.Triloops = EnergyPar.Triloops;
	  
	  pf.id = ++pf_id;
	  return pf;
	}


	PFParamT copy_pf_param()   {
	  if (pf.id != pf_id) scale_pf_parameters();
	  
	  return pf.clone();
	}


	PFParamT set_pf_param(PFParamT dest)  {
	  pf.set(dest);
	  return pf;
	}

	
	
}
