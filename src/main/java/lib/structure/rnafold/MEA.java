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
 *			       MEA.c
 *		 c  Ivo L Hofacker, Vienna RNA package
 *
 * Last changed Time-stamp: <2009-06-18 14:04:21 ivo> 
 * 
 * compute an MEA structure, i.e. the structure maximising
 *  EA = \sum_{(i,j) \in S} 2\gamma p_{i,j} + \sum_{i is unpaired} p^u_i
 * 
 * This can be computed by a variant of the Nussinov recursion:
 * M(i,j) = min(M(i,j-1)+pu[j], min_k M(i,k-1)+C(k,j)
 * C(i,j) = 2*gamma*p_ij + M(i+1,j-1)
 * 
 * Just for fun, we implement it as a sparse DP algorithm.
 * At any time we store only the current and previous row of M.
 * The C matrix is implemented as a sparse matrix:
 * For each j we store in C[j] a list of values (i, MEA([i..j])), containing
 * the MEA over all structures closed by (i,j).
 * The list is sparse since only C values where C(i,j)==M(i,j) can
 * contribute to the optimal solution.
 * 
 * 
 */
public class MEA {

//	// Note this is not a constructor. Keeping the name for historical reasons
//	double MEA(PList[] p, byte[] structure, double gamma) {
//
//		  int i,j,n;
//		  PList[] pp, pl;
//
//		  LItemList[] C;
//		  double MEA;
//		  double[] Mi, Mi1, tmp, pu;
//		  MEAdat bdat;
//
//		  n = structure.length;
//		  for (i=0; i<n; i++) structure[i] = '.';
//
//		  pu = new double[n+1];
//		  pp = pl = prune_sort(p, pu, n, gamma);
//		  PList ppi = pp[0];
//		  int ppipos = 0;
//
//		  C = new LItemList[n+1];
//
//		  Mi = new double[n+1];
//		  Mi1 = new double[n+1];
//
//		  for (i=n; i>0; i--) {
//		    Mi[i] = pu[i];
//		    for (j=i+1; j<=n; j++) {
//		      double EA;
//		      Mi[j] = Mi[j-1] + pu[j];
//		      for (LItem li : C[j].list) {
//		    	  EA = li.A + Mi[(li.i) -1];
//		    	  Mi[j] = Math.max(Mi[j], EA);
//		      }
//		      if (ppi.i == i && ppi.j ==j) {
//			EA = 2*gamma*ppi.p +  Mi1[j-1];
//			if (Mi[j]<EA) {
//			  Mi[j]=EA;
//			  pushC(C[j], i, EA); /* only push into C[j] list if optimal */
//			}
//			ppi = pp[++ppipos];
//		      }
//
//		    }
//		    tmp = Mi1; Mi1 = Mi; Mi = tmp;
//		  }
//
//		  MEA = Mi1[n];
//
//		  bdat.structure = structure; bdat.gamma = gamma;
//		  bdat.C = C;  bdat.Mi=Mi1; bdat.pl=pl; bdat.pu = pu;
//		  mea_backtrack(&bdat, 1, n, 0);
//		  Mi=null; 
//		  Mi1=null; 
//		  pl=null; 
//		  pu=null;
//		  for (i=1; i<=n; i++)
//		    if (C[i].list != null) C[i].list = null;
//		  C = null;
//		  return MEA;
//		}
//
//		int comp_plist(const void *a, const void *b) {
//		  plist *A, *B;
//		  int di;
//		  A = (plist *)a;
//		  B = (plist *)b;
//		  di = (B.i - A.i);
//		  if (di!=0) return di;
//		  return (A.j - B.j);
//		}
//
//
//		PList[] prune_sort(PList[] p, double[] pu, int n, double gamma) {
//		  /*
//		     produce a list containing all base pairs with
//		     2*gamma*p_ij > p^u_i + p^u_j
//		     already sorted to be in the order we need them within the DP
//		  */
//		  int size, i, nump = 0;
//		  PList[] pp, pc;
//
//		  for (i=1; i<=n; i++) pu[i]=1.;
//
//		  PList pci;
//		  for(int x=0; pci.i > 0; x++) {
//			  pci = p[x];
//			  pu[pci.i] -= pci.p;
//			  pu[pci.j] -= pci.p;
//		  }
//		  size = n+1;
//		  pp = new PList[n+1];
//		  for (int x=0; pci.i > 0; x++) {
//			  pci = p[x];
//		    if (pci.i > n) System.out.println("mismatch between plist and structure in MEA()");
//		    if (pci.p*2*gamma > pu[pci.i] + pu[pci.j]) {
//		      if (nump+1 >= size) {
//		    	  size += size/2 + 1;
//		    	  pp = Arrays.copyOf(pp, size);
//		      }
//		      pp[nump++] = pci;
//		    }
//		  }
//		  pp[nump].i = pp[nump].j = (int) (pp[nump].p = 0);
//		  qsort(pp, nump, sizeof(plist), comp_plist);
//		  return pp;
//		}
//
//		static void pushC(LItemList c, int i, double a) {
//		  if (c.nelem+1>=c.size) {
//		    c.size = (int) Math.max(8,c.size*Math.sqrt(2));
//		    c.list = Arrays.copyOf(c.list, c.size); 
//		  }
//		  c.list[c.nelem].i = i;
//		  c.list[c.nelem].A = a;
//		  c.nelem++;
//		}
//
//		static void mea_backtrack(const struct MEAdat *bdat, int i, int j, int pair) {
//		  /* backtrack structure for the interval [i..j] */
//		  /* recursively calls itself, recomputes the necessary parts of the M matrix */
//		  List *C; Litem *li;
//		  double *Mi, prec;
//		  double *pu;
//		  int fail=1;
//
//		  C = bdat.C;
//		  Mi = bdat.Mi;
//		  pu = bdat.pu;
//
//		  if (pair) {
//		    int k;
//		    /* if pair == 1, insert pair and re-compute Mi values */
//		    /* else Mi is already filled */
//		    bdat.structure[i-1] = '(';
//		    bdat.structure[j-1] = ')';
//		    i++; j--;
//		    /* We've done this before in MEA() but didn't keep the results */
//		    Mi[i-1]=0; Mi[i]=pu[i];
//		    for (k=i+1; k<=j; k++) {
//		      Mi[k] = Mi[k-1] + pu[k];
//		      for (li=C[k].list; li<C[k].list+C[k].nelem && li.i >= i; li++) {
//			double EA;
//			EA = li.A + Mi[(li.i) -1];
//			Mi[k] = MAX2(Mi[k], EA);
//		      }
//		    }
//		  }
//
//		  prec = DBL_EPSILON * Mi[j];
//		  /* Mi values are filled, do the backtrace */
//		  while (j>i && Mi[j] <= Mi[j-1] + pu[j] + prec) {
//		    bdat.structure[j-1]='.';
//		    j--;
//		  }
//		  for (li=C[j].list; li<C[j].list + C[j].nelem && li.i >= i; li++) {
//		    double EA;
//		    if (Mi[j] <= li.A + Mi[(li.i) -1] + prec) {
//		      if (li.i > i+3) mea_backtrack(bdat, i, (li.i)-1, 0);
//		      mea_backtrack(bdat, li.i, j, 1);
//		      fail = 0;
//		    }
//		  }
//		  if (fail && j>i) nrerror("backtrack failed for MEA()");
//		}
//	
	
}
