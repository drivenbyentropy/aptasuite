/**
 * 
 */
package lib.structure.rnafold;

import java.util.Arrays;

/**
 * @author Jan Hoinka
 *
 * RNAFold4j is a port of RNAFold to Java. RNAFold is implemented and developed by Ivo L Hofacker et al.
 * as part of the ViennaRNA package v 1.8.5. All intellectual credits of this work 
 * go to the original authors and the Institute for Theoretical Chemistry of the 
 * University of Vienna. My only contribution is the adaptation of the C source code to Java.
 * 
 * minimum free energy
 * RNA secondary structure prediction
 * 
 * c Ivo Hofacker, Chrisoph Flamm original implementation by Walter Fontana
 * 
 * Vienna RNA package
 */
public class Fold {

	FoldVars fold_vars;
	Params params;
	PairMat pair_mat;
	
	int STACK_BULGE1  = 1;   /* stacking energies for bulges of size 1 */
	int NEW_NINIO   =   1;   /* new asymetry penalty */

	int logML=0;    /* if nonzero use logarithmic ML energy in
		   		     	energy_of_struct */
	int uniq_ML=0;  /* do ML decomposition uniquely (for subopt) */
	int MAXSECTORS=500;     /* dimension for a backtrack array */
	double LOCALITY = 0. ;     /* locality parameter for base-pairs */

	boolean SAME_STRAND(int I,int J) {
		
		return ((I>=cut_point)||(J<cut_point));
		
	}

	ParamT P = null;

	int[] indx; /* index for moving in the triangle matrices c[] and fMl[]*/
	int[]   c;       /* energy array, given that i-j pair */
	int[]   cc;      /* linear array for calculating canonical structures */
	int[]   cc1;     /*   "     "        */
	int[]   f5;      /* energy of 5' end */
	int[]   fML;     /* multi-loop auxiliary energy array */
	int[]   fM1;     /* second ML array, only for subopt */
	int[]   Fmi;     /* holds row i of fML (avoids jumps in memory) */
	int[]   DMLi;    /* DMLi[j] holds MIN(fML[i,k]+fML[k+1,j])  */
	int[]   DMLi1;   /*             MIN(fML[i+1,k]+fML[k+1,j])  */
	int[]   DMLi2;   /*             MIN(fML[i+2,k]+fML[k+1,j])  */
	byte[]  ptype;   /* precomputed array of pair types */
	short[] S;
	short[] S1;
	
	int   init_length=-1;

	byte  alpha[] = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
	
	/* needed by cofold/eval */
	int min_hairpin = EnergyConst.TURN;
	int cut_point = -1; /* set to first pos of second seq for cofolding */
	int   eos_debug=0;  /* verbose info from energy_of_struct */

	/* some definitions to take circfold into account...	*/
	int		circ = 0;
	int[]  fM2;	/* fM2 = multiloop region with exactly two stems, extending to 3' end	*/
	int   Fc, FcH, FcI, FcM; /* parts of the exterior loop energies			*/
	/*--------------------------------------------------------------------------*/

	
	Sector[] sector = new Sector[MAXSECTORS]; /* stack of partial structures for backtracking */
	
	public Fold(FoldVars fold_vars, PairMat pair_mat, Params params) {
		
		this.fold_vars = fold_vars;
		this.params = params;
		
		for (int x=0; x<MAXSECTORS; x++) {
			sector[x] = new Sector();
		}
		
		this.pair_mat = pair_mat;
		
	}
	
	void initialize_fold(int length)
	{
	  int n;
	  if (length<1) throw new RuntimeException("initialize_fold: argument must be greater 0");
	  if (init_length>0) free_arrays();
	  get_arrays(length);
	  init_length=length;

	  for (n = 1; n <= length; n++)
	    indx[n] = (n*(n-1)) >> 1;        /* n(n-1)/2 */

	  update_fold_params();
	}

	/*--------------------------------------------------------------------------*/

	void get_arrays(int size)
	{
	  indx =  new int[size+1];
	  c     = new int[((size*(size+1))/2+2)];
	  fML   = new int[((size*(size+1))/2+2)];
	  if (uniq_ML != 0)
	    fM1 = new int[((size*(size+1))/2+2)];

	  ptype = new byte[((size*(size+1))/2+2)];
	  f5    = new int[size+2];
	  cc    = new int[size+2];
	  cc1   = new int[size+2];
	  Fmi   = new int[size+1];
	  DMLi  = new int[size+1];
	  DMLi1  =new int[size+1];
	  DMLi2  =new int[size+1];
	  if (fold_vars.base_pair != null) { 
		  for(int x=0; x<fold_vars.base_pair.length; fold_vars.base_pair[x++] = null);
		  fold_vars.base_pair=null;
	  }
	  fold_vars.base_pair = new BondT[(1+size/2)];
	  for(int x=0; x<fold_vars.base_pair.length; fold_vars.base_pair[x++] = new BondT());
	  /* extra array(s) for circfold() */
	  if(circ != 0) fM2 =  new int[size+2];
	}

	/*--------------------------------------------------------------------------*/

	void free_arrays()
	{
	  indx = null; 
	  c = null; 
	  fML = null; 
	  f5 = null; 
	  cc = null; 
	  cc1 = null;
	  ptype = null;
	  if(fM1!=null){ fM1=null;}
	  if(fM2!=null){ fM2=null;}

	  for(int x=0; x<fold_vars.base_pair.length; fold_vars.base_pair[x++] = null);
	  fold_vars.base_pair=null; 
	  
	  Fmi = null;
	  DMLi = null; 
	  DMLi1 = null;
	  DMLi2 = null;
	  init_length=0;
	}

	/*--------------------------------------------------------------------------*/

	void export_fold_arrays(int[] f5_p, int[] c_p, int[] fML_p, int[] fM1_p,
				int[] indx_p, byte[] ptype_p) {
	  /* make the DP arrays available to routines such as subopt() */
	  f5_p = f5; 
	  c_p = c;
	  fML_p = fML; 
	  fM1_p = fM1;
	  indx_p = indx; 
	  ptype_p = ptype;
	}

	void export_circfold_arrays(int Fc_p, int FcH_p, int FcI_p, int FcM_p, int[] fM2_p,
				int[] f5_p, int[] c_p, int[] fML_p, int[] fM1_p,
				int[] indx_p, byte[] ptype_p) {
	  /* make the DP arrays available to routines such as subopt() */
	  f5_p = f5; 
	  c_p = c;
	  fML_p = fML; 
	  fM1_p = fM1; 
	  fM2_p = fM2;
	  Fc_p=Fc; 
	  FcH_p=FcH;
	  FcI_p=FcI;
	  FcM_p=FcM;
	  indx_p = indx; 
	  ptype_p = ptype;
	}

	/*--------------------------------------------------------------------------*/

	int[] BP; /* contains the structure constrainsts: BP[i]
				-1: | = base must be paired
				-2: < = base must be paired with j<i
				-3: > = base must be paired with j>i
				-4: x = base must not pair
				positive int: base is paired with int      */

	double fold(byte[] string, byte[] structure) {
	  int i, length, energy, bonus=0, bonus_cnt=0;

	  circ = 0;
	  length = string.length;
	  
	  if (length>init_length) initialize_fold(length);
	  if (Math.abs(P.temperature - fold_vars.temperature)>1e-6) update_fold_params();

	  
	  encode_seq(string);
	  BP = new int[length+2];
	  make_ptypes(S, structure);
	  energy = fill_arrays(string);
	  
	  backtrack(string, 0);
	  
//	  System.out.println("Finished Backtracking");

	  parenthesis_structure(structure, length);
	  
	  /* check constraints */
	  for(i=1;i<=length;i++) {
	    if((BP[i]<0)&&(BP[i]>-4)) {
	      bonus_cnt++;
	      if((BP[i]==-3)&&(structure[i-1]==')')) bonus++;
	      if((BP[i]==-2)&&(structure[i-1]=='(')) bonus++;
	      if((BP[i]==-1)&&(structure[i-1]!='.')) bonus++;
	    }

	    if(BP[i]>i) {
	      int l;
	      bonus_cnt++;
	      for(l=1; l<=fold_vars.base_pair[0].i; l++)
		if((i==fold_vars.base_pair[l].i)&&(BP[i]==fold_vars.base_pair[l].j)) bonus++;
	    }
	  }

	  if (bonus_cnt>bonus) System.out.println("\ncould not enforce all constraints\n");
	  bonus*=EnergyConst.BONUS;

	  S = null;
	  S1 = null;
	  BP = null;

	  energy += bonus;      /*remove bonus energies from result */

	  if (fold_vars.backtrack_type=='C')
	    return (float) c[indx[length]+1]/100.;
	  else if (fold_vars.backtrack_type=='M')
	    return (float) fML[indx[length]+1]/100.;
	  else
	    return (float) energy/100.;
	}

	int fill_arrays(byte[] string) {
	  /* fill "c", "fML" and "f5" arrays and return  optimal energy */

	  int   i, j, k, length, energy;
	  int   decomp, new_fML, max_separation;
	  int   type, type_2, tt;
	  int   bonus=0;
	  boolean no_close;

	  length = string.length;
	  
	  max_separation = (int) ((1.-LOCALITY)*(double)(length-2)); /* not in use */

	  for (j=1; j<=length; j++) {
	    Fmi[j]=DMLi[j]=DMLi1[j]=DMLi2[j]=EnergyConst.INF;
	  }

	  for (j = 1; j<=length; j++)
	    for (i=(j>EnergyConst.TURN?(j-EnergyConst.TURN):1); i<j; i++) {
	      c[indx[j]+i] = fML[indx[j]+i] = EnergyConst.INF;
	      if (uniq_ML != 0) fM1[indx[j]+i] = EnergyConst.INF;
	    }

	  if (length <= EnergyConst.TURN) return 0; 

	  for (i = length-EnergyConst.TURN-1; i >= 1; i--) { /* i,j in [1..length] */

	    for (j = i+EnergyConst.TURN+1; j <= length; j++) {
	      int p, q, ij;
	      ij = indx[j]+i;
	      bonus = 0;
	      type = ptype[ij];

	      /* enforcing structure constraints */
	      if ((BP[i]==j)||(BP[i]==-1)||(BP[i]==-2)) bonus -= EnergyConst.BONUS;
	      if ((BP[j]==-1)||(BP[j]==-3)) bonus -= EnergyConst.BONUS;
	      if ((BP[i]==-4)||(BP[j]==-4)) type=0;

	      no_close = (((type==3)||(type==4))&&fold_vars.no_closingGU&&(bonus==0));

	      if (j-i-1 > max_separation) type = 0;  /* forces locality degree */

	      if (type != 0) {   /* we have a pair */
		int new_c=0, stackEnergy=EnergyConst.INF;
		/* hairpin ----------------------------------------------*/

		if (no_close) new_c = EnergyConst.FORBIDDEN;
		else
		  new_c = HairpinE(j-i-1, type, (int)S1[i+1], (int)S1[j-1], string, i-1);

		/*--------------------------------------------------------
		  check for elementary structures involving more than one
		  closing pair.
		  --------------------------------------------------------*/

		for (p = i+1; p <= Math.min(j-2-EnergyConst.TURN,i+EnergyConst.MAXLOOP+1) ; p++) {
		  int minq = j-i+p-EnergyConst.MAXLOOP-2;
		  if (minq<p+1+EnergyConst.TURN) minq = p+1+EnergyConst.TURN;
		  for (q = minq; q < j; q++) {
		    type_2 = ptype[indx[q]+p];

		    if (type_2==0) continue;
		    type_2 = pair_mat.rtype[type_2];

		    if (fold_vars.no_closingGU)
		      if (no_close||(type_2==3)||(type_2==4))
			if ((p>i+1)||(q<j-1)) continue;  /* continue unless stack */

		    energy = LoopEnergy(p-i-1, j-q-1, type, type_2,
					S1[i+1], S1[j-1], S1[p-1], S1[q+1]);

		    new_c = Math.min(energy+c[indx[q]+p], new_c);
		    if ((p==i+1)&&(j==q+1)) stackEnergy = energy; /* remember stack energy */

		  } /* end q-loop */
		} /* end p-loop */

		/* multi-loop decomposition ------------------------*/


		if (!no_close) {
		  int MLenergy;
		  decomp = DMLi1[j-1];
		  if (fold_vars.dangles != 0) {
		    int d3=0, d5=0;
		    tt = pair_mat.rtype[type];
		    d3 = P.dangle3[tt][S1[i+1]];
		    d5 = P.dangle5[tt][S1[j-1]];
		    if (fold_vars.dangles==2) /* double dangles */
		      decomp += d5 + d3;
		    else {          /* normal dangles */
		      decomp = Math.min(DMLi2[j-1]+d3+P.MLbase, decomp);
		      decomp = Math.min(DMLi1[j-2]+d5+P.MLbase, decomp);
		      decomp = Math.min(DMLi2[j-2]+d5+d3+2*P.MLbase, decomp);
		    }
		  }

		  MLenergy = P.MLclosing+P.MLintern[type]+decomp;

		  new_c = MLenergy < new_c ? MLenergy : new_c;
		}

		/* coaxial stacking of (i.j) with (i+1.k) or (k+1.j-1) */

		if (fold_vars.dangles==3) {
		  decomp = EnergyConst.INF;
		  for (k = i+2+EnergyConst.TURN; k < j-2-EnergyConst.TURN; k++) {
		    type_2 = ptype[indx[k]+i+1]; type_2 = pair_mat.rtype[type_2];
		    if (type_2 != 0)
		      decomp = Math.min(decomp, c[indx[k]+i+1]+P.stack[type][type_2]+
				    fML[indx[j-1]+k+1]);
		    type_2 = ptype[indx[j-1]+k+1]; type_2 = pair_mat.rtype[type_2];
		    if (type_2 != 0)
		      decomp = Math.min(decomp, c[indx[j-1]+k+1]+P.stack[type][type_2]+
				    fML[indx[k]+i+1]);
		  }
		  /* no TermAU penalty if coax stack */
		  decomp += 2*P.MLintern[1] + P.MLclosing;
		  new_c = Math.min(new_c, decomp);
		}

		new_c = Math.min(new_c, cc1[j-1]+stackEnergy);
		cc[j] = new_c + bonus;
		if (fold_vars.noLonelyPairs)
		  c[ij] = cc1[j-1]+stackEnergy+bonus;
		else
		  c[ij] = cc[j];

	      } /* end >> if (pair) << */

	      else c[ij] = EnergyConst.INF;


	      /* done with c[i,j], now compute fML[i,j] */
	      /* free ends ? -----------------------------------------*/

	      new_fML = fML[ij+1]+P.MLbase;
	      new_fML = Math.min(fML[indx[j-1]+i]+P.MLbase, new_fML);
	      energy = c[ij]+P.MLintern[type];
	      if (fold_vars.dangles==2) {  /* double dangles */
		energy += (i==1) ? /* works also for circfold */
		  P.dangle5[type][S1[length]] : P.dangle5[type][S1[i-1]];
		/* if (j<length) */ energy += P.dangle3[type][S1[j+1]];
	      }
	      new_fML = Math.min(energy, new_fML);
	      if (uniq_ML != 0)
		fM1[ij] = Math.min(fM1[indx[j-1]+i] + P.MLbase, energy);

	      if (fold_vars.dangles%2==1) {  /* normal dangles */
		tt = ptype[ij+1]; /* i+1,j */
		new_fML = Math.min(c[ij+1]+P.dangle5[tt][S1[i]]
			       +P.MLintern[tt]+P.MLbase,new_fML);
		tt = ptype[indx[j-1]+i];
		new_fML = Math.min(c[indx[j-1]+i]+P.dangle3[tt][S1[j]]
			       +P.MLintern[tt]+P.MLbase, new_fML);
		tt = ptype[indx[j-1]+i+1];
		new_fML = Math.min(c[indx[j-1]+i+1]+P.dangle5[tt][S1[i]]+
			       P.dangle3[tt][S1[j]]+P.MLintern[tt]+2*P.MLbase, new_fML);
	      }

	      /* modular decomposition -------------------------------*/

	      for (decomp = EnergyConst.INF, k = i+1+EnergyConst.TURN; k <= j-2-EnergyConst.TURN; k++)
		decomp = Math.min(decomp, Fmi[k]+fML[indx[j]+k+1]);

	      DMLi[j] = decomp;               /* store for use in ML decompositon */
	      new_fML = Math.min(new_fML,decomp);

	      /* coaxial stacking */
	      if (fold_vars.dangles==3) {
		/* additional ML decomposition as two coaxially stacked helices */
		for (decomp = EnergyConst.INF, k = i+1+EnergyConst.TURN; k <= j-2-EnergyConst.TURN; k++) {
		  type = ptype[indx[k]+i]; type = pair_mat.rtype[type];
		  type_2 = ptype[indx[j]+k+1]; type_2 = pair_mat.rtype[type_2];
		  if ((type!=0) && (type_2!=0))
		    decomp = Math.min(decomp,
				  c[indx[k]+i]+c[indx[j]+k+1]+P.stack[type][type_2]);
		}

		decomp += 2*P.MLintern[1];	/* no TermAU penalty if coax stack */
//	#if 0
//		/* This is needed for Y shaped ML loops with coax stacking of
//		   interior pairts, but backtracking will fail if activated */
//		DMLi[j] = MIN2(DMLi[j], decomp);
//		DMLi[j] = MIN2(DMLi[j], DMLi[j-1]+P.MLbase);
//		DMLi[j] = MIN2(DMLi[j], DMLi1[j]+P.MLbase);
//		new_fML = MIN2(new_fML, DMLi[j]);
//	#endif
		new_fML = Math.min(new_fML, decomp);
	      }

	      fML[ij] = Fmi[j] = new_fML;     /* substring energy */

	    }

	    {
	      int[] FF; /* rotate the auxilliary arrays */
	      FF = DMLi2; DMLi2 = DMLi1; DMLi1 = DMLi; DMLi = FF;
	      FF = cc1; cc1=cc; cc=FF;
	      for (j=1; j<=length; j++) {cc[j]=Fmi[j]=DMLi[j]=EnergyConst.INF; }
	    }
	  }

	  /* calculate energies of 5' and 3' fragments */

	  f5[EnergyConst.TURN+1]=0;
	  for (j=EnergyConst.TURN+2; j<=length; j++) {
	    f5[j] = f5[j-1];
	    type=ptype[indx[j]+1];
	    if (type != 0) {
	      energy = c[indx[j]+1];
	      if (type>2) energy += P.TerminalAU;
	      if ((fold_vars.dangles==2)&&(j<length))  /* double dangles */
		energy += P.dangle3[type][S1[j+1]];
	      f5[j] = Math.min(f5[j], energy);
	    }
	    type=ptype[indx[j-1]+1];
	    if ((type != 0)&&(fold_vars.dangles%2==1)) {
	      energy = c[indx[j-1]+1]+P.dangle3[type][S1[j]];
	      if (type>2) energy += P.TerminalAU;
	      f5[j] = Math.min(f5[j], energy);
	    }
	    for (i=j-EnergyConst.TURN-1; i>1; i--) {
	      type = ptype[indx[j]+i];
	      if (type != 0) {
		energy = f5[i-1]+c[indx[j]+i];
		if (type>2) energy += P.TerminalAU;
		if (fold_vars.dangles==2) {
		  energy += P.dangle5[type][S1[i-1]];
		  if (j<length) energy += P.dangle3[type][S1[j+1]];
		}
		f5[j] = Math.min(f5[j], energy);
		if (fold_vars.dangles%2==1) {
		  energy = f5[i-2]+c[indx[j]+i]+P.dangle5[type][S1[i-1]];
		  if (type>2) energy += P.TerminalAU;
		  f5[j] = Math.min(f5[j], energy);
		}
	      }
	      type = ptype[indx[j-1]+i];
	      if ((type != 0)&&(fold_vars.dangles%2==1)) {
		energy = c[indx[j-1]+i]+P.dangle3[type][S1[j]];
		if (type>2) energy += P.TerminalAU;
		f5[j] = Math.min(f5[j], f5[i-1]+energy);
		f5[j] = Math.min(f5[j], f5[i-2]+energy+P.dangle5[type][S1[i-1]]);
	      }
	    }
	  }

	  return f5[length];
	}


	
	/* -*-C-*- */
	/* this file contains code for folding circular RNAs */
	/* it's #include'd into fold.c */

	double circfold(byte[] string, byte[] structure) {
	  /* variant of fold() for circular RNAs */
	  /* auxiliarry arrays:
	     fM2 = multiloop region with exactly two stems, extending to 3' end
	     for stupid dangles=1 case we also need:
	     fM_d3 = multiloop region with >= 2 stems, starting at pos 2
		     (a pair (k,n) will form 3' dangle with pos 1)
	     fM_d5 = multiloop region with >= 2 stems, extending to pos n-1
		     (a pair (1,k) will form a 5' dangle with pos n)
	  */
	  int Hi = 0, Hj = 0, Ii = 0, Ij = 0, Ip = 0, Iq = 0, Mi = 0;
	  int[] fM_d3;
	  int[] fM_d5; 
	  int Md3i = 0, Md5i = 0, FcMd3, FcMd5;
	  int i,j, p,q,length, energy, bonus=0, bonus_cnt=0;
	  int s=0;
	  /* if (!uniq_ML) {uniq_ML=1; init_length=-1;} */
	  circ = 1;

	 length = string.length;
	  if (length>init_length) initialize_fold(length);
	  if (Math.abs(P.temperature - fold_vars.temperature)>1e-6) update_fold_params();

	  encode_seq(string);

	  BP = new int[length+2];
	  make_ptypes(S, structure);

	  /* compute all arrays used by fold() for linear RNA */
	  energy = fill_arrays(string);

	  FcH = FcI= FcM = FcMd3= FcMd5= Fc = EnergyConst.INF;
	  for (i=1; i<length; i++)
	    for (j=i+EnergyConst.TURN+1; j <= length; j++) {
	      int ij, bonus1=0, type, u, new_c; 
	      boolean no_close;
	      u = length-j + i-1;
	      if (u<EnergyConst.TURN) continue;

	      ij = indx[j]+i;
	      type = ptype[ij];

	      /* enforcing structure constraints */
	      if ((BP[i]==j)||(BP[i]==-1)||(BP[i]==-2)) bonus1 -= EnergyConst.BONUS;
	      if ((BP[j]==-1)||(BP[j]==-3)) bonus1 -= EnergyConst.BONUS;
	      if ((BP[i]==-4)||(BP[j]==-4)) type=0;

	      no_close = (((type==3)||(type==4))&&fold_vars.no_closingGU&&(bonus1==0));

	      /* if (j-i-1 > max_separation) type = 0; */  /* forces locality degree */

	      type=pair_mat.rtype[type];
	      if (type == 0) continue;
	      if (no_close) new_c = EnergyConst.FORBIDDEN;
	      else {
	    	  byte[] loopseq = new byte[10];
		/*int si1, sj1;*/
		if (u<7) {
			System.arraycopy(string, j - 1, loopseq, 0, string.length - (j - 1));
			System.arraycopy(string, 0, loopseq, j, i);
			
		  //strcpy(loopseq , string+j-1);
		  //strncat(loopseq, string, i);
		}
		/*
		si1 = (i==1)?S1[length] : S1[i-1];
		sj1 = (j==length)?S1[1] : S1[j+1];
		*/
		new_c = HairpinE(u, type, /*sj1*/(int)S1[j+1], /*si1*/(int)S1[i-1],  loopseq, 0)+bonus1+c[ij];
	      }
	      if (new_c<FcH) {
		FcH = new_c; Hi=i; Hj=j;
	      }

	      for (p = j+1; p < length ; p++) {
		int u1, qmin;
		u1 = p-j-1;
		if (u1+i-1>EnergyConst.MAXLOOP) break;
		qmin = u1+i-1+length-EnergyConst.MAXLOOP;
		if (qmin<p+EnergyConst.TURN+1) qmin = p+EnergyConst.TURN+1;
		for (q = qmin; q <=length; q++) {
		  int u2, type_2/*, si1, sq1*/;
		  type_2 = pair_mat.rtype[ptype[indx[q]+p]];
		  if (type_2==0) continue;
		  u2 = i-1 + length-q;
		  if (u1+u2>EnergyConst.MAXLOOP) continue;
			  /* we dont need this due to init of S1 (see encode_seq)
			  si1 = (i==1)? S1[length] : S1[i-1];
			  sq1 = (q==length)? S1[1] : S1[q+1];
			  */
		  energy = LoopEnergy(u1, u2, type, type_2, S1[j+1], /*si1*/S1[i-1], S1[p-1], /*sq1*/S1[q+1]);
		  new_c = c[ij] + c[indx[q]+p] + energy;
		  if (new_c<FcI) {
		    FcI = new_c; Ii=i; Ij=j; Ip=p; Iq=q;
		  }
		}
	      }
	    }
	  Fc = Math.min(FcI, FcH);

	  /* compute the fM2 array (multi loops with exactly 2 helices) */
	  /* to get a unique ML decomposition, just use fM1 instead of fML
	     below. However, that will not work with dangles==1  */
	  for (i=1; i<length-EnergyConst.TURN; i++) {
	    int u;
	    fM2[i] = EnergyConst.INF;
	    for (u=i+EnergyConst.TURN; u<length-EnergyConst.TURN; u++)
	      fM2[i] = Math.min(fM2[i], fML[indx[u]+i] + fML[indx[length]+u+1]);
	  }

	  for (i=EnergyConst.TURN+1; i<length-2*EnergyConst.TURN; i++) {
	    int fm;
	    fm = fML[indx[i]+1]+fM2[i+1]+P.MLclosing;
	    if (fm<FcM) {
	      FcM=fm; Mi=i;
	    }
	  }
	  Fc = Math.min(Fc, FcM);

	  if (fold_vars.dangles==1) {
	    int u;
	    fM_d3 =  new int[length+2];
	    fM_d5 =  new int[length+2];
	    for (i=EnergyConst.TURN+1; i<length-EnergyConst.TURN; i++) {
	      fM_d3[i] = EnergyConst.INF;
	      for (u=2+EnergyConst.TURN; u<i-EnergyConst.TURN; u++)
		fM_d3[i] = Math.min(fM_d3[i], fML[indx[u]+2] + fML[indx[i]+u+1]);
	    }
	    for (i=2*EnergyConst.TURN+1; i<length-EnergyConst.TURN; i++) {
	      int fm, type;
	      type = ptype[indx[length]+i+1];
	      if (type==0) continue;
	      fm = fM_d3[i]+c[indx[length]+i+1]+P.MLclosing+P.MLintern[type]+P.dangle3[type][S1[1]];
	      if (fm<FcMd3) {
		FcMd3=fm; Md3i=i;
	      }
	      fm = fM_d3[i-1]+c[indx[length]+i+1]+P.MLclosing+P.MLintern[type]+P.dangle3[type][S1[1]]+P.dangle5[type][S1[i]];
	      if (fm<FcMd3) {
		FcMd3=fm; Md3i=-i;
	      }
	    }

	    for (i=EnergyConst.TURN+1; i<length-EnergyConst.TURN; i++) {
	      fM_d5[i] = EnergyConst.INF;
	      for (u=i+EnergyConst.TURN; u<length-EnergyConst.TURN; u++)
		fM_d5[i] = Math.min(fM_d5[i], fML[indx[u]+i] + fML[indx[length-1]+u+1]);
	    }
	    for (i=EnergyConst.TURN+1; i<length-2*EnergyConst.TURN; i++) {
	      int fm, type;
	      type = ptype[indx[i]+1];
	      if (type==0) continue;
	      fm = P.dangle5[type][S1[length]]+c[indx[i]+1]+fM_d5[i+1]+P.MLclosing+P.MLintern[type];
	      if (fm<FcMd5) {
		FcMd5=fm; Md5i=i;
	      }
	      fm = P.dangle5[type][S1[length]]+c[indx[i]+1]+P.dangle3[type][S1[i+1]]+
		fM_d5[i+2]+P.MLclosing+P.MLintern[type];
	      if (fm<FcMd5) {
		FcMd5=fm; Md5i=-i;
	      }
	    }
	    if (FcMd5<Math.min(Fc,FcMd3)) {
	      /* looks like we have to do this ... */
	      sector[++s].i = 1;
	      sector[s].j = (Md5i>0)?Md5i:-Md5i;
	      sector[s].ml = 2;
	      i = (Md5i>0)?Md5i+1 : -Md5i+2; /* let's backtrack fm_d5[Md5i+1] */
	      for (u=i+EnergyConst.TURN; u<length-EnergyConst.TURN; u++)
		if (fM_d5[i] == fML[indx[u]+i] + fML[indx[length-1]+u+1]) {
		  sector[++s].i = i;
		  sector[s].j = u;
		  sector[s].ml = 1;
		  sector[++s].i =u+1;
		  sector[s].j = length-1;
		  sector[s].ml = 1;
		  break;
		}
	      Fc = FcMd5;
	    } else if (FcMd3<Fc) {
	      /* here we go again... */
	      sector[++s].i = (Md3i>0)?Md3i+1:-Md3i+1;
	      sector[s].j = length;
	      sector[s].ml = 2;
	      i = (Md3i>0)? Md3i : -Md3i-1; /* let's backtrack fm_d3[Md3i] */
	      for (u=2+EnergyConst.TURN; u<i-EnergyConst.TURN; u++)
		if (fM_d3[i] == fML[indx[u]+2] + fML[indx[i]+u+1]) {
		  sector[++s].i = 2;
		  sector[s].j = u;
		  sector[s].ml = 1;
		  sector[++s].i =u+1;
		  sector[s].j = i;
		  sector[s].ml = 1;
		  break;
		}
	      Fc = FcMd3;
	    }
	    fM_d3 = null;
	    fM_d5 = null;
	  }
	  if(Fc < EnergyConst.INF){
	    if (FcH==Fc) {
	      sector[++s].i = Hi;
	      sector[s].j = Hj;
	      sector[s].ml = 2;
	    }
	    else if (FcI==Fc) {
	      sector[++s].i = Ii;
	      sector[s].j = Ij;
	      sector[s].ml = 2;
	      sector[++s].i = Ip;
	      sector[s].j = Iq;
	      sector[s].ml = 2;
	    }
	    else if (FcM==Fc) { /* grumpf we found a Multiloop */
	      int fm, u;
	      /* backtrack in fM2 */
	      fm = fM2[Mi+1];
	      for (u=Mi+EnergyConst.TURN+1; u<length-EnergyConst.TURN; u++)
	        if (fm == fML[indx[u]+Mi+1] + fML[indx[length]+u+1]) {
			sector[++s].i=Mi+1;
			sector[s].j=u;
			sector[s].ml = 1;
			sector[++s].i=u+1;
			sector[s].j=length;
			sector[s].ml = 1;
			break;
	        }
	      sector[++s].i = 1;
	      sector[s].j = Mi;
	      sector[s].ml = 1;
	    }
	  }
	  backtrack(string, s);

	  parenthesis_structure(structure, length);

	  /* check constraints */
	  for(i=1;i<=length;i++) {
	    if((BP[i]<0)&&(BP[i]>-4)) {
	      bonus_cnt++;
	      if((BP[i]==-3)&&(structure[i-1]==')')) bonus++;
	      if((BP[i]==-2)&&(structure[i-1]=='(')) bonus++;
	      if((BP[i]==-1)&&(structure[i-1]!='.')) bonus++;
	    }

	    if(BP[i]>i) {
	      int l;
	      bonus_cnt++;
	      for(l=1; l<=fold_vars.base_pair[0].i; l++)
		if((i==fold_vars.base_pair[l].i)&&(BP[i]==fold_vars.base_pair[l].j)) bonus++;
	    }
	  }

	  if (bonus_cnt>bonus) System.out.println("\ncould not enforce all constraints\n");
	  bonus*=EnergyConst.BONUS;

	  S = null;
	  S1 = null; 
	  BP = null;

	  energy = Fc + bonus;      /*remove bonus energies from result */
	  if(energy == EnergyConst.INF)
	    return (double) 0.0;
	  else
	    return (double) energy/100.;
	}

	
	

	void backtrack(byte[] string, int s) {

		/*------------------------------------------------------------------
		trace back through the "c", "f5" and "fML" arrays to get the
		base pairing list. No search for equivalent structures is done.
		This is fast, since only few structure elements are recalculated.
		------------------------------------------------------------------*/

		/*
		 * normally s=0. If s>0 then s items have been already pushed onto the sector
		 * stack
		 */
		int i = 0, j = 0, k = 0, length, energy = 0, new1 = 0;
		int no_close = 0, type = 0, type_2 = 0, tt = 0;
		int bonus = 0;
		int b = 0;

		length = string.length;
		if (s == 0) {
			sector[++s].i = 1;
			sector[s].j = length;
			sector[s].ml = (fold_vars.backtrack_type == 'M') ? 1 : ((fold_vars.backtrack_type == 'C') ? 2 : 0);
		}
		outer_while:
		while (s > 0) {
			int ml, fij = 0, fi = 0, cij = 0, traced = 0, i1 = 0, j1 = 0, d3 = 0, d5 = 0, mm = 0, p = 0, q = 0, jj = 0;
			int canonical = 1; /* (i,j) closes a canonical structure */
			
			// since Java does not support goto directives, we need to 
			// rewrite this as a series of while loops
			above_goto_while:
			while (true) {
				
				i = sector[s].i;
				j = sector[s].j;
				ml = sector[s--].ml; 	/*
										 * ml is a flag indicating if backtracking is to occur in the fML- (1) or in the
										 * f-array (0)
										 */
//				System.out.println(String.format("%d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d", 
//						s, i,j, k, length, energy, new1, no_close, type, type_2, tt, bonus, b,ml, fij, fi, cij, traced, i1, j1, d3, d5, mm, p, q, jj ));
				
				
				if (ml == 2) {
					fold_vars.base_pair[++b].i = i;
					fold_vars.base_pair[b].j = j;
					// goto repeat1 can be replaced with a break here
					// this will continue code execution at the original
					// repeat1 location
//					System.out.println("818 break above_goto_while");
					break above_goto_while;
				}

				if (j < i + EnergyConst.TURN + 1) {
//					System.out.println("823 continue outer_while");
					continue outer_while; /* no more pairs in this interval */
				}

				fij = (ml != 0) ? fML[indx[j] + i] : f5[j];
				fi = (ml != 0) ? (fML[indx[j - 1] + i] + P.MLbase) : f5[j - 1];

				if (fij == fi) { /* 3' end is unpaired */
					sector[++s].i = i;
					sector[s].j = j - 1;
					sector[s].ml = ml;
//					System.out.println("832 continue outer_while");
					continue outer_while;
				}

				if (ml == 0) { /* backtrack in f5 */
					/* j or j-1 is paired. Find pairing partner */
					for_loop:
					for (k = j - EnergyConst.TURN - 1, traced = 0; k >= 1; k--) {
						int cc, en;
						jj = k - 1;
						type = ptype[indx[j - 1] + k];
						if ((type != 0) && (fold_vars.dangles % 2 == 1)) {
							cc = c[indx[j - 1] + k] + P.dangle3[type][S1[j]];
							if (type > 2)
								cc += P.TerminalAU;
							if (fij == cc + f5[k - 1])
								traced = j - 1;
							if (k > i)
								if (fij == f5[k - 2] + cc + P.dangle5[type][S1[k - 1]]) {
									traced = j - 1;
									jj = k - 2;
								}
						}
						type = ptype[indx[j] + k];
						if (type != 0) {
							cc = c[indx[j] + k];
							if (type > 2)
								cc += P.TerminalAU;
							en = cc + f5[k - 1];
							if (fold_vars.dangles == 2) {
								if (k > 1)
									en += P.dangle5[type][S1[k - 1]];
								if (j < length)
									en += P.dangle3[type][S1[j + 1]];
							}
							if (fij == en)
								traced = j;
							if ((fold_vars.dangles % 2 == 1) && (k > 1))
								if (fij == f5[k - 2] + cc + P.dangle5[type][S1[k - 1]]) {
									traced = j;
									jj = k - 2;
								}
						}
						if (traced != 0) {
//							System.out.println("875 break for_loop");
							break for_loop;
						}
					}

					if (traced == 0)
						throw new RuntimeException("backtrack failed in f5");
					sector[++s].i = 1;
					sector[s].j = jj;
					sector[s].ml = ml;

					i = k;
					j = traced;
					fold_vars.base_pair[++b].i = i;
					fold_vars.base_pair[b].j = j;
					// goto repeat1;
					// same here, break to go to repeat1
//					System.out.println("891 break above_goto_while");
					break above_goto_while;
					
				} else { /* trace back in fML array */
					int cij1 = EnergyConst.INF, ci1j = EnergyConst.INF, ci1j1 = EnergyConst.INF;
					if (fML[indx[j] + i + 1] + P.MLbase == fij) { /* 5' end is unpaired */
						sector[++s].i = i + 1;
						sector[s].j = j;
						sector[s].ml = ml;
//						System.out.println("900 continue outer_while");
						continue outer_while;
					}

					tt = ptype[indx[j] + i];
					cij = c[indx[j] + i] + P.MLintern[tt];
					if (fold_vars.dangles == 2) { /* double dangles, works also for circfold */
						cij += (i == 1) ? P.dangle5[tt][S1[length]] : P.dangle5[tt][S1[i - 1]];
						/* if (j<length) */ cij += P.dangle3[tt][S1[j + 1]];
					} else if (fold_vars.dangles % 2 == 1) { /* normal dangles */
						tt = ptype[indx[j] + i + 1];
						ci1j = c[indx[j] + i + 1] + P.dangle5[tt][S1[i]] + P.MLintern[tt] + P.MLbase;
						tt = ptype[indx[j - 1] + i];
						cij1 = c[indx[j - 1] + i] + P.dangle3[tt][S1[j]] + P.MLintern[tt] + P.MLbase;
						tt = ptype[indx[j - 1] + i + 1];
						ci1j1 = c[indx[j - 1] + i + 1] + P.dangle5[tt][S1[i]] + P.dangle3[tt][S1[j]] + P.MLintern[tt]
								+ 2 * P.MLbase;
					}

					if ((fij == cij) || (fij == ci1j) || (fij == cij1) || (fij == ci1j1)) {
						/* found a pair */
						if (fij == ci1j)
							i++;
						else if (fij == cij1)
							j--;
						else if (fij == ci1j1) {
							i++;
							j--;
						}
						fold_vars.base_pair[++b].i = i;
						fold_vars.base_pair[b].j = j;
						//goto repeat1;
//						System.out.println("938 break above_goto_while");
						break above_goto_while;
					}

					for (k = i + 1 + EnergyConst.TURN; k <= j - 2 - EnergyConst.TURN; k++)
						if (fij == (fML[indx[k] + i] + fML[indx[j] + k + 1])) {
//							System.out.println("941 break outer_while");
							break outer_while;
						}

					if ((fold_vars.dangles == 3) && (k > j - 2 - EnergyConst.TURN)) { /* must be coax stack */
						ml = 2;
						for (k = i + 1 + EnergyConst.TURN; k <= j - 2 - EnergyConst.TURN; k++) {
							type = ptype[indx[k] + i];
							type = pair_mat.rtype[type];
							type_2 = ptype[indx[j] + k + 1];
							type_2 = pair_mat.rtype[type_2];
							if ((type != 0) && (type_2 != 0))
								if (fij == c[indx[k] + i] + c[indx[j] + k + 1] + P.stack[type][type_2]
										+ 2 * P.MLintern[1]) {
//									System.out.println("955 break outer_while");
									break outer_while;
								}
						}
					}

					sector[++s].i = i;
					sector[s].j = k;
					sector[s].ml = ml;
					sector[++s].i = k + 1;
					sector[s].j = j;
					sector[s].ml = ml;

					if (k > j - 2 - EnergyConst.TURN)
						throw new RuntimeException("backtrack failed in fML");
//					System.out.println("970 continue outer_while");
					continue outer_while;
				}
				
				//break above_goto_while; // this would be unreachible code as in the if else cluase above
				// we either break (in the if) or coninue in the esle anyway.
				
			}
			
			/*----- begin of "repeat:" -----*/
			//repeat1:
			
			// goto directives within repeat1 can be modeled as a continue in a while loop
			in_goto_while:
			while (true) {
				
				if (canonical != 0)
					cij = c[indx[j] + i];

				type = ptype[indx[j] + i];

				bonus = 0;

				if (fold_vars.fold_constrained) {
					if ((BP[i] == j) || (BP[i] == -1) || (BP[i] == -2))
						bonus -= EnergyConst.BONUS;
					if ((BP[j] == -1) || (BP[j] == -3))
						bonus -= EnergyConst.BONUS;
				}
				if (fold_vars.noLonelyPairs)
					if (cij == c[indx[j] + i]) {
						/*
						 * (i.j) closes canonical structures, thus (i+1.j-1) must be a pair
						 */
						type_2 = ptype[indx[j - 1] + i + 1];
						type_2 = pair_mat.rtype[type_2];
						cij -= P.stack[type][type_2] + bonus;
						fold_vars.base_pair[++b].i = i + 1;
						fold_vars.base_pair[b].j = j - 1;
						i++;
						j--;
						canonical = 0;
						//goto repeat1;
//						System.out.println("1017 continue in_goto_while");
						continue in_goto_while;
					}
				canonical = 1;

				no_close = (((type == 3) || (type == 4)) && fold_vars.no_closingGU && (bonus == 0)) ? 1 : 0;
				if (no_close != 0) {
					if (cij == EnergyConst.FORBIDDEN)
						continue outer_while;
				} else if (cij == HairpinE(j - i - 1, type, S1[i + 1], S1[j - 1], string, i - 1) + bonus) {
//					System.out.println("1026 continue outer_while");
					continue outer_while;
				}

				for (p = i + 1; p <= Math.min(j - 2 - EnergyConst.TURN, i + EnergyConst.MAXLOOP + 1); p++) {
					int minq;
					minq = j - i + p - EnergyConst.MAXLOOP - 2;
					if (minq < p + 1 + EnergyConst.TURN)
						minq = p + 1 + EnergyConst.TURN;
					inner_for_loop:
					for (q = j - 1; q >= minq; q--) {

						type_2 = ptype[indx[q] + p];
						if (type_2 == 0) {
//							System.out.println("1040 continue inner_for_loop");
							continue inner_for_loop;
						}
						type_2 = pair_mat.rtype[type_2];
						if (fold_vars.no_closingGU)
							if ((no_close != 0) || (type_2 == 3) || (type_2 == 4))
								if ((p > i + 1) || (q < j - 1)) {
//									System.out.println("1047 continue inner_for_loop");
									continue inner_for_loop; /* continue unless stack */
								}

						/* energy = oldLoopEnergy(i, j, p, q, type, type_2); */
//						System.out.println(String.format("LE: %d %d %d %d %d %d %d %d", p - i - 1, j - q - 1, type, type_2, S1[i + 1], S1[j - 1], S1[p - 1],
//								S1[q + 1]));
						energy = LoopEnergy(p - i - 1, j - q - 1, type, type_2, S1[i + 1], S1[j - 1], S1[p - 1],
								S1[q + 1]);

						new1 = energy + c[indx[q] + p] + bonus;
						
//						System.out.println(String.format("Energy= %d, new= %d", energy, new1));
						
						traced = (cij == new1) ? 1 : 0;
						if (traced != 0) {
							fold_vars.base_pair[++b].i = p;
							fold_vars.base_pair[b].j = q;
							i = p;
							j = q;
							//goto repeat1;
//							System.out.println("1066 continue in_goto_while");
							continue in_goto_while;
						}
					}
				}
//				System.out.println("1074 break in_goto_while");
				break in_goto_while; // make sure we exit here
			}
			/* end of repeat: -------------------------------------------------- */

			/* (i.j) must close a multi-loop */
			tt = pair_mat.rtype[type];
			mm = bonus + P.MLclosing + P.MLintern[tt];
			d5 = P.dangle5[tt][S1[j - 1]];
			d3 = P.dangle3[tt][S1[i + 1]];
			i1 = i + 1;
			j1 = j - 1;
			sector[s + 1].ml = sector[s + 2].ml = 1;

			for (k = i + 2 + EnergyConst.TURN; k < j - 2 - EnergyConst.TURN; k++) {
				int en;
				en = fML[indx[k] + i + 1] + fML[indx[j - 1] + k + 1] + mm;
				if (fold_vars.dangles == 2) /* double dangles */
					en += d5 + d3;
				if (cij == en)
					break outer_while;
				if (fold_vars.dangles % 2 == 1) { /* normal dangles */
					if (cij == (fML[indx[k] + i + 2] + fML[indx[j - 1] + k + 1] + mm + d3 + P.MLbase)) {
						i1 = i + 2;
						break outer_while;
					}
					if (cij == (fML[indx[k] + i + 1] + fML[indx[j - 2] + k + 1] + mm + d5 + P.MLbase)) {
						j1 = j - 2;
						break outer_while;
					}
					if (cij == (fML[indx[k] + i + 2] + fML[indx[j - 2] + k + 1] + mm + d3 + d5 + P.MLbase + P.MLbase)) {
						i1 = i + 2;
						j1 = j - 2;
						break outer_while;
					}
				}
				/* coaxial stacking of (i.j) with (i+1.k) or (k.j-1) */
				/* use MLintern[1] since coax stacked pairs don't get TerminalAU */
				if (fold_vars.dangles == 3) {
					type_2 = ptype[indx[k] + i + 1];
					type_2 = pair_mat.rtype[type_2];
					if (type_2 != 0) {
						en = c[indx[k] + i + 1] + P.stack[type][type_2] + fML[indx[j - 1] + k + 1];
						if (cij == en + 2 * P.MLintern[1] + P.MLclosing) {
							ml = 2;
							sector[s + 1].ml = 2;
							break outer_while;
						}
					}
					type_2 = ptype[indx[j - 1] + k + 1];
					type_2 = pair_mat.rtype[type_2];
					if (type_2 != 0) {
						en = c[indx[j - 1] + k + 1] + P.stack[type][type_2] + fML[indx[k] + i + 1];
						if (cij == en + 2 * P.MLintern[1] + P.MLclosing) {
							sector[s + 2].ml = 2;
							break outer_while;
						}
					}
				}

			}
			if (k <= j - 3 - EnergyConst.TURN) { /* found the decomposition */
				sector[++s].i = i1;
				sector[s].j = k;
				sector[++s].i = k + 1;
				sector[s].j = j1;
			} else {
				// #if 0
				// /* Y shaped ML loops fon't work yet */
				// if (dangles==3) {
				// /* (i,j) must close a Y shaped ML loop with coax stacking */
				// if (cij == fML[indx[j-2]+i+2] + mm + d3 + d5 + P.MLbase + P.MLbase) {
				// i1 = i+2;
				// j1 = j-2;
				// } else if (cij == fML[indx[j-2]+i+1] + mm + d5 + P.MLbase)
				// j1 = j-2;
				// else if (cij == fML[indx[j-1]+i+2] + mm + d3 + P.MLbase)
				// i1 = i+2;
				// else /* last chance */
				// if (cij != fML[indx[j-1]+i+1] + mm + P.MLbase)
				// fprintf(stderr, "backtracking failed in repeat");
				// /* if we arrive here we can express cij via fML[i1,j1]+dangles */
				// sector[++s].i = i1;
				// sector[s].j = j1;
				// }
				// else
				// #endif
				throw new RuntimeException("backtracking failed in repeat");
			}
		}
		
//		System.out.println(String.format("Saving b=%d", b));
		fold_vars.base_pair[0].i = b; /* save the total number of base pairs */
	}

 
	
	byte[] backtrack_fold_from_pair(byte[] sequence, int i, int j) {
	  byte[] structure;
	  sector[1].i  = i;
	  sector[1].j  = j;
	  sector[1].ml = 2;
	  fold_vars.base_pair[0].i=0;
	  encode_seq(sequence);
	  backtrack(sequence, 1);
	  structure = new byte[sequence.length+1];
	  parenthesis_structure(structure, sequence.length);
	  S = null;
	  S1 = null;
	  return structure;
	}
	/*---------------------------------------------------------------------------*/

	int HairpinE(int size, int type, int si1, int sj1, byte[] string, int spos) {
	  int energy;
	  energy = (size <= 30) ? P.hairpin[size] :
	    P.hairpin[30]+(int)(P.lxc*Math.log((size)/30.));
	  if (fold_vars.tetra_loop)
	    if (size == 4) { /* check for tetraloop bonus */
	    	byte[] tl = Arrays.copyOfRange(string, spos, spos + 6);
			int tsmt = P.Tetraloops.indexOf(new String(tl));
			if (tsmt != -1)
				energy += P.TETRA_ENERGY[tsmt/7];
	    }
	  if (size == 3) {
	    byte[] tl = Arrays.copyOfRange(string, spos, spos+5);
	    int tsmt = P.Triloops.indexOf(new String(tl));
	    if (tsmt != -1)
			energy += P.Triloop_E[tsmt/6];
	    
	    if (type>2)  /* neither CG nor GC */
	      energy += P.TerminalAU; /* penalty for closing AU GU pair */
	  }
	  else  /* no mismatches for tri-loops */
	    energy += P.mismatchH[type][si1][sj1];

	  return energy;
	}

	/*---------------------------------------------------------------------------*/

	int oldLoopEnergy(int i, int j, int p, int q, int type, int type_2) {
		/* compute energy of degree 2 loop (stack bulge or interior) */
		int n1, n2, m, energy;
		n1 = p - i - 1;
		n2 = j - q - 1;

		if (n1 > n2) {
			m = n1;
			n1 = n2;
			n2 = m;
		} /* so that n2>=n1 */

		if (n2 == 0)
			energy = P.stack[type][type_2]; /* stack */

		else if (n1 == 0) { /* bulge */
			energy = (n2 <= EnergyConst.MAXLOOP) ? P.bulge[n2] : (P.bulge[30] + (int) (P.lxc * Math.log(n2 / 30.)));

			if (STACK_BULGE1 != 0)
				if (n2 == 1)
					energy += P.stack[type][type_2];

		} else { /* interior loop */

			if ((n1 + n2 == 2) && (fold_vars.james_rule != 0))
				/* special case for loop size 2 */
				energy = P.int11[type][type_2][S1[i + 1]][S1[j - 1]];
			else {
				energy = (n1 + n2 <= EnergyConst.MAXLOOP) ? (P.internal_loop[n1 + n2])
						: (P.internal_loop[30] + (int) (P.lxc * Math.log((n1 + n2) / 30.)));

				if (NEW_NINIO != 0)
					energy += Math.min(EnergyPar.MAX_NINIO, (n2 - n1) * P.F_ninio[2]);
				else {
					m = Math.min(4, n1);
					energy += Math.min(EnergyPar.MAX_NINIO, ((n2 - n1) * P.F_ninio[m]));
				}
				energy += P.mismatchI[type][S1[i + 1]][S1[j - 1]] + P.mismatchI[type_2][S1[q + 1]][S1[p - 1]];
			}
		}
		return energy;
	}

	/*--------------------------------------------------------------------------*/

	int LoopEnergy(int n1, int n2, int type, int type_2, int si1, int sj1, int sp1, int sq1) {
	  /* compute energy of degree 2 loop (stack bulge or interior) */
	  int nl, ns, energy;

	  if (n1>n2) { nl=n1; ns=n2;}
	  else {nl=n2; ns=n1;}

	  if (nl == 0)
	    return P.stack[type][type_2];    /* stack */

	  if (ns==0) {                       /* bulge */
	    energy = (nl<=EnergyConst.MAXLOOP)?P.bulge[nl]:
	      (P.bulge[30]+(int)(P.lxc*Math.log(nl/30.)));
	    if (nl==1) energy += P.stack[type][type_2];
	    else {
	      if (type>2) energy += P.TerminalAU;
	      if (type_2>2) energy += P.TerminalAU;
	    }
	    return energy;
	  }
	  else {                             /* interior loop */
	    if (ns==1) {
	      if (nl==1)                     /* 1x1 loop */
		return P.int11[type][type_2][si1][sj1];
	      if (nl==2) {                   /* 2x1 loop */
		if (n1==1)
		  energy = P.int21[type][type_2][si1][sq1][sj1];
		else
		  energy = P.int21[type_2][type][sq1][si1][sp1];
		return energy;
	      }
	    }
	    else if (n1==2 && n2==2)         /* 2x2 loop */
	      return P.int22[type][type_2][si1][sp1][sq1][sj1];
	    { /* generic interior loop (no else here!)*/
	      energy = (n1+n2<=EnergyConst.MAXLOOP)?(P.internal_loop[n1+n2]):
		(P.internal_loop[30]+(int)(P.lxc*Math.log((n1+n2)/30.)));

	      energy += Math.min(EnergyPar.MAX_NINIO, (nl-ns)*P.F_ninio[2]);

	      energy += P.mismatchI[type][si1][sj1]+
		P.mismatchI[type_2][sq1][sp1];
	    }
	  }
	  return energy;
	}


	/*---------------------------------------------------------------------------*/

	void encode_seq(byte[] sequence) {
	  int i,l;

	  l = sequence.length;
	  S = new short[l+2];
	  S1= new short[l+2];
	  /* S1 exists only for the special X K and I bases and energy_set!=0 */
	  S[0] = (short) l;

	  for (i=1; i<=l; i++) { /* make numerical encoding of sequence */
	    S[i]= (short) pair_mat.encode_char(sequence[i-1]);
	    S1[i] = pair_mat.alias[S[i]];   /* for mismatches of nostandard bases */
	  }
	  /* for circular folding add first base at position n+1 and last base at
		position 0 in S1	*/
	  S[l+1] = S[1]; S1[l+1]=S1[1]; S1[0] = S1[l];
	}

	/*---------------------------------------------------------------------------*/

	void letter_structure(byte[] structure, int length)
	{
	  int n, k, x, y;

	  for (n = 0; n <= length-1; structure[n++] = ' ') ;
	  structure[length] = '\0';

	  for (n = 0, k = 1; k <= fold_vars.base_pair[0].i; k++) {
	    y = fold_vars.base_pair[k].j;
	    x = fold_vars.base_pair[k].i;
	    if (x-1 > 0 && y+1 <= length) {
	      if (structure[x-2] != ' ' && structure[y] == structure[x-2]) {
		structure[x-1] = structure[x-2];
		structure[y-1] = structure[x-1];
		continue;
	      }
	    }
	    if (structure[x] != ' ' && structure[y-2] == structure[x]) {
	      structure[x-1] = structure[x];
	      structure[y-1] = structure[x-1];
	      continue;
	    }
	    n++;
	    structure[x-1] = alpha[n-1];
	    structure[y-1] = alpha[n-1];
	  }
	}

	/*---------------------------------------------------------------------------*/

	void parenthesis_structure(byte[] structure, int length)
	{
	  int n, k;

	  for (n = 0; n <= length-1; structure[n++] = '.') ;
	  
	  for (k = 1; k <= fold_vars.base_pair[0].i; k++) {
	    structure[fold_vars.base_pair[k].i-1] = '(';
	    structure[fold_vars.base_pair[k].j-1] = ')';
	  }
	}
	/*---------------------------------------------------------------------------*/

	void update_fold_params()
	{
	  P = params.scale_parameters();
	  pair_mat.make_pair_matrix();
	  if (init_length < 0) init_length=0;
	}

	/*---------------------------------------------------------------------------*/
	short[] pair_table;

	double energy_of_struct(byte[] string, byte[] structure)
	{
	  int   energy;
	  short[] ss, ss1;

	  if ((init_length<0)||(P==null)) update_fold_params();
	  if (Math.abs(P.temperature - fold_vars.temperature)>1e-6) update_fold_params();

	  if (structure.length != string.length)
	    throw new RuntimeException(String.format("energy_of_struct: string and structure have unequal length (string: %d, structure: %d)", string.length, structure.length));

	  /* save the S and S1 pointers in case they were already in use */
	  ss = S; ss1 = S1;
	  encode_seq(string);

	  pair_table = Utils.make_pair_table(structure);

	  energy = energy_of_struct_pt(string, pair_table, S, S1);

	  pair_table = null;
	  S = null; 
	  S1 = null;
	  S=ss; S1=ss1;
	  return  (double) energy/100.;
	}

	int energy_of_struct_pt(byte[] string, short[] ptable,
				short[] s, short[] s1) {
	  /* auxiliary function for kinfold,
	     for most purposes call energy_of_struct instead */

	  int   i, length, energy;

	  pair_table = ptable;
	  S = s;
	  S1 = s1;

	  length = S[0];
	  energy =  fold_vars.backtrack_type=='M' ? ML_Energy(0, 0) : ML_Energy(0, 1);
	  if (eos_debug>0)
	    System.out.println(String.format("External loop                           : %5d\n", energy));
	  for (i=1; i<=length; i++) {
	    if (pair_table[i]==0) continue;
	    energy += stack_energy(i, string);
	    i=pair_table[i];
	  }
	  for (i=1; !SAME_STRAND(i,length); i++) {
	    if (!SAME_STRAND(i,pair_table[i])) {
	      energy+=P.DuplexInit;
	      break;
	    }
	  }
	  return energy;
	}

	double energy_of_circ_struct(byte[] string, byte[] structure) {
	  int   i, j, length, energy=0, en0, degree=0, type;
	  short[] ss, ss1;

	  if ((init_length<0)||(P==null)) update_fold_params();
	  if (Math.abs(P.temperature - fold_vars.temperature)>1e-6) update_fold_params();

	  if (structure.length != string.length)
	    throw new RuntimeException("energy_of_struct: string and structure have unequal length");

	  /* save the S and S1 pointers in case they were already in use */
	  ss = S; ss1 = S1;
	  encode_seq(string);

	  pair_table = Utils.make_pair_table(structure);

	  length = S[0];

	  for (i=1; i<=length; i++) {
	    if (pair_table[i]==0) continue;
	    degree++;
	    energy += stack_energy(i, string);
	    i=pair_table[i];
	  }

	  if (degree==0) return 0.;
	  for (i=1; pair_table[i]==0; i++);
	  j = pair_table[i];
	  type=pair_mat.pair[S[j]][S[i]];
	  if (type==0) type=7;
	  if (degree==1) {
	    byte[] loopseq = new byte[10];
	    int u, si1, sj1;
	    for (i=1; pair_table[i]==0; i++);
	    u = length-j + i-1;
	    if (u<7) {
			System.arraycopy(string, j - 1, loopseq, 0, string.length - (j - 1));
			System.arraycopy(string, 0, loopseq, j, i);
			
//	      strcpy(loopseq , string+j-1);
//	      strncat(loopseq, string, i);
	    }
	    si1 = (i==1)?S1[length] : S1[i-1];
	    sj1 = (j==length)?S1[1] : S1[j+1];
	    en0 = HairpinE(u, type, sj1, si1,  loopseq, 0);
	  } else
	    if (degree==2) {
	      int p,q, u1,u2, si1, sq1, type_2;
	      for (p=j+1; pair_table[p]==0; p++);
	      q=pair_table[p];
	      u1 = p-j-1;
	      u2 = i-1 + length-q;
	      type_2 = pair_mat.pair[S[q]][S[p]];
	      if (type_2==0) type_2=7;
	      si1 = (i==1)? S1[length] : S1[i-1];
	      sq1 = (q==length)? S1[1] : S1[q+1];
	      en0 = LoopEnergy(u1, u2, type, type_2,
			       S1[j+1], si1, S1[p-1], sq1);
	    } else { /* degree > 2 */
	      en0 = ML_Energy(0, 0) - P.MLintern[0];
	      if (fold_vars.dangles != 0) {
		int d5, d3;
		if (pair_table[1] != 0) {
		  j = pair_table[1];
		  type = pair_mat.pair[S[1]][S[j]];
		  if (fold_vars.dangles==2)
		    en0 += P.dangle5[type][S1[length]];
		  else { /* dangles==1 */
		    if (pair_table[length]==0) {
		      d5 = P.dangle5[type][S1[length]];
		      if (pair_table[length-1]!=0) {
			int tt;
			tt = pair_mat.pair[S[pair_table[length-1]]][S[length-1]];
			d3 = P.dangle3[tt][S1[length]];
			if (d3<d5) d5 = 0;
			else d5 -= d3;
		      }
		      en0 += d5;
		    }
		  }
		}
		if (pair_table[length] != 0) {
		  i = pair_table[length];
		  type = pair_mat.pair[S[i]][S[length]];
		  if (fold_vars.dangles==2)
		    en0 += P.dangle3[type][S1[1]];
		  else { /* dangles==1 */
		    if (pair_table[1]==0) {
		      d3 = P.dangle3[type][S1[1]];
		      if (pair_table[2] != 0) {
			int tt;
			tt = pair_mat.pair[S[2]][S[pair_table[2]]];
			d5 = P.dangle5[tt][1];
			if (d5<d3) d3=0;
			else d3 -= d5;
		      }
		      en0 += d3;
		    }
		  }
		}
	      }
	    }

	  if (eos_debug>0)
	    System.out.println(String.format("External loop                           : %5d\n", en0));
	  energy += en0;
	  /* fprintf(stderr, "ext loop degree %d tot %d\n", degree, energy); */
	  S=null; S1=null;
	  S=ss; S1=ss1;
	  return  (double) energy/100.0;
	}

	/*---------------------------------------------------------------------------*/
	int stack_energy(int i, byte[] string)
	{
	  /* calculate energy of substructure enclosed by (i,j) */
	  int ee, energy = 0;
	  int j, p, q, type;

	  j=pair_table[i];
	  type = pair_mat.pair[S[i]][S[j]];
	  if (type==0) {
	    type=7;
	    if (eos_debug>=0)
	      System.out.println(String.format("WARNING: bases %d and %d (%c%c) can't pair!\n", i, j,string[i-1],string[j-1]));
	  }

	  p=i; q=j;
	  while (p<q) { /* process all stacks and interior loops */
	    int type_2;
	    while (pair_table[++p]==0);
	    while (pair_table[--q]==0);
	    if ((pair_table[q]!=(short)p)||(p>q)) break;
	    type_2 = pair_mat.pair[S[q]][S[p]];
	    if (type_2==0) {
	      type_2=7;
	      if (eos_debug>=0)
		System.out.println(String.format("WARNING: bases %d and %d (%c%c) can't pair!\n", p, q, string[p-1],string[q-1]));
	    }
	    /* energy += LoopEnergy(i, j, p, q, type, type_2); */
	    if ( SAME_STRAND(i,p) && SAME_STRAND(q,j) )
	      ee = LoopEnergy(p-i-1, j-q-1, type, type_2,
			      S1[i+1], S1[j-1], S1[p-1], S1[q+1]);
	    else
	      ee = ML_Energy(cut_in_loop(i), 1);
	    if (eos_debug>0)
	      System.out.println(String.format("Interior loop (%3d,%3d) %c%c; (%3d,%3d) %c%c: %5d\n",i,j,string[i-1],string[j-1],p,q,string[p-1],string[q-1], ee));
	    energy += ee;
	    i=p; j=q; type = pair_mat.rtype[type_2];
	  } /* end while */

	  /* p,q don't pair must have found hairpin or multiloop */

	  if (p>q) {                       /* hair pin */
	    if (SAME_STRAND(i,j))
	      ee = HairpinE(j-i-1, type, S1[i+1], S1[j-1], string, i-1);
	    else
	      ee = ML_Energy(cut_in_loop(i), 1);
	    energy += ee;
	    if (eos_debug>0)
	      System.out.println(String.format("Hairpin  loop (%3d,%3d) %c%c              : %5d\n",i, j, string[i-1],string[j-1], ee));

	    return energy;
	  }

	  /* (i,j) is exterior pair of multiloop */
	  while (p<j) {
	    /* add up the contributions of the substructures of the ML */
	    energy += stack_energy(p, string);
	    p = pair_table[p];
	    /* search for next base pair in multiloop */
	    while (pair_table[++p]==0);
	  }
	  {
	    int ii;
	    ii = cut_in_loop(i);
	    ee = (ii==0) ? ML_Energy(i,0) : ML_Energy(ii, 1);
	  }
	  energy += ee;
	  if (eos_debug>0)
	    System.out.println(String.format("Multi    loop (%3d,%3d) %c%c              : %5d\n",i,j,string[i-1],string[j-1],ee));

	  return energy;
	}

	/*---------------------------------------------------------------------------*/

	int ML_Energy(int i, int is_extloop) {
	  /* i is the 5'-base of the closing pair (or 0 for exterior loop)
	     loop is scored as ML if extloop==0 else as exterior loop

	     since each helix can coaxially stack with at most one of its
	     neighbors we need an auxiliarry variable  cx_energy
	     which contains the best energy given that the last two pairs stack.
	     energy  holds the best energy given the previous two pairs do not
	     stack (i.e. the two current helices may stack)
	     We don't allow the last helix to stack with the first, thus we have to
	     walk around the Loop twice with two starting points and take the minimum
	  */

	  int energy, cx_energy, best_energy=EnergyConst.INF;
	  int i1, j, p, q, u = 0, x, type, count;
	  int mlintern[] = new int[EnergyConst.NBPAIRS+1]; 
	  int mlclosing, mlbase;

	  if (is_extloop != 0) {
	    for (x = 0; x <= EnergyConst.NBPAIRS; x++)
	      mlintern[x] = P.MLintern[x]-P.MLintern[1]; /* 0 or TerminalAU */
	    mlclosing = mlbase = 0;
	  } else {
	    for (x = 0; x <= EnergyConst.NBPAIRS; x++) mlintern[x] = P.MLintern[x];
	    mlclosing = P.MLclosing; mlbase = P.MLbase;
	  }

	  for (count=0; count<2; count++) { /* do it twice */
	    int ld5 = 0; /* 5' dangle energy on prev pair (type) */
	    if ( i==0 ) {
	      j = pair_table[0]+1;
	      type = 0;  /* no pair */
	    }
	    else {
	      j = pair_table[i];
	      type = pair_mat.pair[S[j]][S[i]]; if (type==0) type=7;

	      if (fold_vars.dangles==3) { /* prime the ld5 variable */
		if (SAME_STRAND(j-1,j)) {
		  ld5 = P.dangle5[type][S1[j-1]];
		  if ((p=pair_table[j-2]) != 0 && SAME_STRAND(j-2, j-1))
		      if (P.dangle3[pair_mat.pair[S[p]][S[j-2]]][S1[j-1]]<ld5) ld5 = 0;
		}
	      }
	    }
	    i1=i; p = i+1; u=0;
	    energy = 0; cx_energy=EnergyConst.INF;
	    do { /* walk around the multi-loop */
	      int tt, new_cx = EnergyConst.INF;

	      /* hope over unpaired positions */
	      while (p <= pair_table[0] && pair_table[p]==0) p++;

	      /* memorize number of unpaired positions */
	      u += p-i1-1;
	      /* get position of pairing partner */
	      if ( p == pair_table[0]+1 )
		q = tt = 0; /* virtual root pair */
	      else {
	      q  = pair_table[p];
		/* get type of base pair P.q */
	      tt = pair_mat.pair[S[p]][S[q]]; if (tt==0) tt=7;
	      }

	      energy += mlintern[tt];
	      cx_energy += mlintern[tt];

	      if (fold_vars.dangles != 0) {
		int dang5=0, dang3=0, dang;
		if ((SAME_STRAND(p-1,p))&&(p>1))
		  dang5=P.dangle5[tt][S1[p-1]];      /* 5'dangle of pq pair */
		if ((SAME_STRAND(i1,i1+1))&&(i1<S[0]))
		  dang3 = P.dangle3[type][S1[i1+1]]; /* 3'dangle of previous pair */

		switch (p-i1-1) {
		case 0: /* adjacent helices */
		  if (fold_vars.dangles==2)
		    energy += dang3+dang5;
		  else if (fold_vars.dangles==3 && i1!=0) {
		    if (SAME_STRAND(i1,p)) {
		      new_cx = energy + P.stack[pair_mat.rtype[type]][pair_mat.rtype[tt]];
		      /* subtract 5'dangle and TerminalAU penalty */
		      new_cx += -ld5 - mlintern[tt]-mlintern[type]+2*mlintern[1];
		    }
		    ld5=0;
		    energy = Math.min(energy, cx_energy);
		  }
		  break;
		case 1: /* 1 unpaired base between helices */
		  dang = (fold_vars.dangles==2)?(dang3+dang5):Math.min(dang3, dang5);
		  if (fold_vars.dangles==3) {
		    energy = energy +dang; ld5 = dang - dang3;
		    /* may be problem here: Suppose
		       cx_energy>energy, cx_energy+dang5<energy
		       and the following helices are also stacked (i.e.
		       we'll subtract the dang5 again */
		    if (cx_energy+dang5 < energy) {
		      energy = cx_energy+dang5;
		      ld5 = dang5;
		    }
		    new_cx = EnergyConst.INF;  /* no coax stacking with mismatch for now */
		  } else
		    energy += dang;
		  break;
		default: /* many unpaired base between helices */
		  energy += dang5 +dang3;
		  if (fold_vars.dangles==3) {
		    energy = Math.min(energy, cx_energy + dang5);
		    new_cx = EnergyConst.INF;  /* no coax stacking possible */
		    ld5 = dang5;
		  }
		}
		type = tt;
	      }
	      if (fold_vars.dangles==3) cx_energy = new_cx;
	      i1 = q; p=q+1;
	    } while (q!=i);
	    best_energy = Math.min(energy, best_energy); /* don't use cx_energy here */
	    /* fprintf(stderr, "%6.2d\t", energy); */
	    if (fold_vars.dangles!=3 || is_extloop != 0) break;  /* may break cofold with co-ax */
	    /* skip a helix and start again */
	    while (pair_table[p]==0) p++;
	    if (i == pair_table[p]) break;
	    i = pair_table[p];
	  }
	  energy = best_energy;
	  energy += mlclosing;
	  /* logarithmic ML loop energy if logML */
	  if ( (is_extloop == 0) && (logML != 0) && ( u >6) )
	    energy += 6*mlbase+(int)(P.lxc*Math.log((double)u/6.));
	  else
	    energy += mlbase*u;
	  /* fprintf(stderr, "\n"); */
	  return energy;
	}

	/*---------------------------------------------------------------------------*/

	int loop_energy(short[] ptable, short[] s, short[] s1, int i) {
	  /* compute energy of a single loop closed by base pair (i,j) */
	  int j, type, p,q, energy;
	  short[] Sold, S1old, ptold;

	  ptold=pair_table;   Sold = S;   S1old = S1;
	  pair_table = ptable;   S = s;   S1 = s1;

	  if (i==0) { /* evaluate exterior loop */
	    energy = ML_Energy(0,1);
	    pair_table=ptold; S=Sold; S1=S1old;
	    return energy;
	  }
	  j = pair_table[i];
	  if (j<i) throw new RuntimeException("i is unpaired in loop_energy()");
	  type = pair_mat.pair[S[i]][S[j]];
	  if (type==0) {
	    type=7;
	    if (eos_debug>=0)
	      System.out.println(String.format("WARNING: bases %d and %d (%c%c) can't pair!\n", i, j, pair_mat.Law_and_Order.charAt(S[i]),pair_mat.Law_and_Order.charAt(S[j])));
	  }
	  p=i; q=j;


	  while (pair_table[++p]==0);
	  while (pair_table[--q]==0);
	  if (p>q) { /* Hairpin */
	    byte[] loopseq = new byte[8];
	    if (SAME_STRAND(i,j)) {
	      if (j-i-1<7) {
		int u;
		for (u=0; i+u<=j; u++) loopseq[u] = (byte) pair_mat.Law_and_Order.charAt(S[i+u]);
		loopseq[u] = '\0';
	      }
	      energy = HairpinE(j-i-1, type, S1[i+1], S1[j-1], loopseq, 0);
	    } else {
	      energy = ML_Energy(cut_in_loop(i), 1);
	    }
	  }
	  else if (pair_table[q]!=(short)p) { /* multi-loop */
	    int ii;
	    ii = cut_in_loop(i);
	    energy = (ii==0) ? ML_Energy(i,0) : ML_Energy(ii, 1);
	  }
	  else { /* found interior loop */
	    int type_2;
	    type_2 = pair_mat.pair[S[q]][S[p]];
	    if (type_2==0) {
	      type_2=7;
	      if (eos_debug>=0)
		System.out.println(String.format("WARNING: bases %d and %d (%c%c) can't pair!\n", p, q,pair_mat.Law_and_Order.charAt(S[p]),pair_mat.Law_and_Order.charAt(S[q])));
	    }
	    /* energy += LoopEnergy(i, j, p, q, type, type_2); */
	    if ( SAME_STRAND(i,p) && SAME_STRAND(q,j) )
	      energy = LoopEnergy(p-i-1, j-q-1, type, type_2,
				  S1[i+1], S1[j-1], S1[p-1], S1[q+1]);
	    else
	      energy = ML_Energy(cut_in_loop(i), 1);
	  }

	  pair_table=ptold; S=Sold; S1=S1old;
	  return energy;
	}

	/*---------------------------------------------------------------------------*/

	int cut_in_loop(int i) {
	  /* walk around the loop;  return j pos of pair after cut if
	     cut_point in loop else 0 */
	  int  p, j;
	  p = j = pair_table[i];
	  do {
	    i  = pair_table[p];  p = i+1;
	    while ( pair_table[p]==0 ) p++;
	  } while (p!=j && SAME_STRAND(i,p));
	  return SAME_STRAND(i,p) ? 0 : pair_table[p];
	}

	/*---------------------------------------------------------------------------*/

	void make_ptypes(short[] S, byte[] structure) {
	  int n,i,j,k,l;

	  n=S[0];
	  for (k=1; k<n-EnergyConst.TURN; k++)
	    for (l=1; l<=2; l++) {
	      int type,ntype=0,otype=0;
	      i=k; j = i+EnergyConst.TURN+l; if (j>n) continue;
	      type = pair_mat.pair[S[i]][S[j]];
	      while ((i>=1)&&(j<=n)) {
		if ((i>1)&&(j<n)) ntype = pair_mat.pair[S[i-1]][S[j+1]];
		if (fold_vars.noLonelyPairs && (otype == 0) && (ntype == 0))
		  type = 0; /* i.j can only form isolated pairs */
		ptype[indx[j]+i] = (byte) type;
		otype =  type;
		type  = ntype;
		i--; j++;
	      }
	    }

	  if (fold_vars.fold_constrained&&(structure!=null)) {
	    int hx;
	    int[] stack = new int[n+1];
	    byte type;

	    for(hx=0, j=1; j<=n; j++) {
	      switch (structure[j-1]) {
	      case '|': BP[j] = -1; break;
	      case 'x': /* can't pair */
		for (l=1; l<j-EnergyConst.TURN; l++) ptype[indx[j]+l] = 0;
		for (l=j+EnergyConst.TURN+1; l<=n; l++) ptype[indx[l]+j] = 0;
		break;
	      case '(':
		stack[hx++]=j;
		/* fallthrough */
	      case '<': /* pairs upstream */
		for (l=1; l<j-EnergyConst.TURN; l++) ptype[indx[j]+l] = 0;
		break;
	      case ')':
		if (hx<=0) {
		  System.out.println(String.format("%s\n", structure));
		  throw new RuntimeException("unbalanced brackets in constraints");
		}
		i = stack[--hx];
		type = ptype[indx[j]+i];
		for (k=i+1; k<=n; k++) ptype[indx[k]+i] = 0;
		/* don't allow pairs i<k<j<l */
		for (l=j; l<=n; l++)
		  for (k=i+1; k<=j; k++) ptype[indx[l]+k] = 0;
		/* don't allow pairs k<i<l<j */
		for (l=i; l<=j; l++)
		  for (k=1; k<=i; k++) ptype[indx[l]+k] = 0;
		for (k=1; k<j; k++) ptype[indx[j]+k] = 0;
		ptype[indx[j]+i] = (type==0)?7:type;
		/* fallthrough */
	      case '>': /* pairs downstream */
		for (l=j+EnergyConst.TURN+1; l<=n; l++) ptype[indx[l]+j] = 0;
		break;
	      }
	    }
	    if (hx!=0) {
	      System.out.println(String.format("%s\n", structure));
	      throw new RuntimeException("unbalanced brackets in constraint string");
	    }
	    stack = null;
	  }
	}
	
	
	
	
	
	
	
	
	
}
