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
public final class Utils {

	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(int[] src, int[] dest){
		
		for (int i=0; i<src.length; i++) {
				dest[i] = src[i];
			}
	}
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(int[][] src, int[][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				dest[i][j] = src[i][j];
			}
		}
	}
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(int[][][] src, int[][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					dest[i][j][k] = src[i][j][k];
				}
			}
		}
	}
	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(int[][][][] src, int[][][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					for(int l=0; l<src[i][j][k].length; l++) {
					dest[i][j][k][l] = src[i][j][k][l];
					}
				}
			}
		}
	}		
	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(int[][][][][] src, int[][][][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					for(int l=0; l<src[i][j][k].length; l++) {
						for(int m=0; m<src[i][j][k][l].length; m++) {
							dest[i][j][k][l][m] = src[i][j][k][l][m];
						}
					}
				}
			}
		}
	}	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(int[][][][][][] src, int[][][][][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					for(int l=0; l<src[i][j][k].length; l++) {
						for(int m=0; m<src[i][j][k][l].length; m++) {
							for(int n=0; n<src[i][j][k][l][m].length; n++) {
								dest[i][j][k][l][m][n] = src[i][j][k][l][m][n];
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(double[] src, double[] dest){
		
		for (int i=0; i<src.length; i++) {
				dest[i] = src[i];
			}
		
	}
	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(double[][] src, double[][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				dest[i][j] = src[i][j];
			}
		}
		
	}
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(double[][][] src, double[][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					dest[i][j][k] = src[i][j][k];
				}
			}
		}
	}
	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(double[][][][] src, double[][][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					for(int l=0; l<src[i][j][k].length; l++) {
					dest[i][j][k][l] = src[i][j][k][l];
					}
				}
			}
		}
	}		
	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(double[][][][][] src, double[][][][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					for(int l=0; l<src[i][j][k].length; l++) {
						for(int m=0; m<src[i][j][k][l].length; m++) {
							dest[i][j][k][l][m] = src[i][j][k][l][m];
						}
					}
				}
			}
		}
	}	
	
	/**
	 * Copies all values from src to dest. Note that they 
	 * MUSint have the same dimensions
	 * @param src
	 * @param dest
	 */
	static void CopyArray(double[][][][][][] src, double[][][][][][] dest){
		
		for (int i=0; i<src.length; i++) {
			for (int j=0; j<src[i].length; j++) {
				for(int k=0; k<src[i][j].length; k++) {
					for(int l=0; l<src[i][j][k].length; l++) {
						for(int m=0; m<src[i][j][k][l].length; m++) {
							for(int n=0; n<src[i][j][k][l][m].length; n++) {
								dest[i][j][k][l][m][n] = src[i][j][k][l][m][n];
							}
						}
					}
				}
			}
		}
	}
	
	static short[] make_pair_table(byte[] structure)
	{
	    /* returns array representation of structure.
	       table[i] is 0 if unpaired or j if (i.j) pair.  */
	   short i,j,hx;
	   short length;
	   short[] stack;
	   short[] table;

	   length = (short) structure.length;
	   stack = new short[length+1];
	   table = new short[length+2];
	   table[0] = length;

	   for (hx=0, i=1; i<=length; i++) {
	      switch (structure[i-1]) {
	       case '(':
		 stack[hx++]=i;
		 break;
	       case ')':
		 j = stack[--hx];
		 if (hx<0) {
		    System.out.println(String.format("%s\n", structure));
		    throw new RuntimeException("unbalanced brackets in make_pair_table");
		 }
		 table[i]=j;
		 table[j]=i;
		 break;
	       default:   /* unpaired base, usually '.' */
		 table[i]= 0;
		 break;
	      }
	   }
	   if (hx!=0) {
	      System.out.println(String.format("%s\n", structure));
	      throw new RuntimeException("unbalanced brackets in make_pair_table");
	   }
	   stack = null;
	   return(table);
	}
	
	
	
}
