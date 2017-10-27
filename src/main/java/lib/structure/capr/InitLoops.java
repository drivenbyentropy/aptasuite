/**
 * 
 */
package lib.structure.capr;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jan Hoinka
 * Original source code was extracted from Vienna RNA package (version 1.8.5)
 * The author of the original code is Dr. Ivo L Hofacker. The author of the c++ 
 * implementation is Tsukasa Fukunaga. This code represents a java implementation of
 * <code>initloops.h</code> of the 
 * CapR package available at <a href="https://github.com/fukunagatsu/CapR">https://github.com/fukunagatsu/CapR</a>.
 */
public final class InitLoops {
	
	public static  int[][][][] int11_37 = new int[8][8][5][5];
	static
	 {
		 
		//Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("capR_int11_37.txt");
		BufferedInputStream bis = new BufferedInputStream(is);
		
		String token;
		int level = -1;
		int[] levels = {0,0,0,0};
	    while (true) {
		    
			token = getNextToken(bis);
			if (token == null){break;}
		    
			if (token.equals("{")){
				level+=1;
			}
			else if (token.equals("}")){
				levels[level]=0;
				level-=1;
				// last case would otherwise crash on access
				if (level != -1){
					levels[level]+=1;					
				}
			}
			else{
				// add the number
				int11_37[levels[0]][levels[1]][levels[2]][levels[3]] = Integer.parseInt(token);
				levels[level]+=1;
			}
		}	
	}
	
	public static  int[][][][][] int21_37 = new int[8][8][5][5][5];	
	static
	 {
		
		//Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("capR_int21_37.txt");
		BufferedInputStream bis = new BufferedInputStream(is);
		
		String token;
		int level = -1;
		int[] levels = {0,0,0,0,0};
	    while (true) {
		    
			token = getNextToken(bis);
			if (token == null){break;}
		    
			if (token.equals("{")){
				level+=1;
			}
			else if (token.equals("}")){
				levels[level]=0;
				level-=1;
				// last case would otherwise crash on access
				if (level != -1){
					levels[level]+=1;					
				}
			}
			else{
				// add the number
				int21_37[levels[0]][levels[1]][levels[2]][levels[3]][levels[4]] = Integer.parseInt(token);
				levels[level]+=1;
			}
		}	
	}
	
	/**
	 * Adding this array in a  way would exceed javas 64k limit.
	 * Hence, we read it from file
	 */
	public static  int[][][][][][] int22_37 = new int[8][8][5][5][5][5] ;
	static
	 {
		
		//Get file from resources folder
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("capR_int22_37.txt");
		BufferedInputStream bis = new BufferedInputStream(is);
		
		String token;
		int level = -1;
		int[] levels = {0,0,0,0,0,0};
	    while (true) {
		    
			token = getNextToken(bis);
			if (token == null){break;}
		    
			if (token.equals("{")){
				level+=1;
			}
			else if (token.equals("}")){
				levels[level]=0;
				level-=1;
				// last case would otherwise crash on access
				if (level != -1){
					levels[level]+=1;					
				}
			}
			else{
				// add the number
				int22_37[levels[0]][levels[1]][levels[2]][levels[3]][levels[4]][levels[5]] = Integer.parseInt(token);
				levels[level]+=1;
			}
		}
	}
	
	/**
	 * Provided with an inputstream, this function returns the next valid token
	 * as a string. A valid token here is either a positive or negative integer,
	 * { or }. All other elements in the file are ignored.  
	 * @param bis the stream to read from
	 * @return null if eof
	 */
	private static  String getNextToken(BufferedInputStream bis){
		
		try {
			while (bis.available() > 0) {
				
				// read the first char
				char c = (char) bis.read();
				
				// chase for changing dimension
				if (c == '{' || c == '}'){
					return c+"";
				}
				
				// do we have a negative number?
				boolean negative = false;
				if (c == '-'){
					negative = true;
					c = (char) bis.read();
				}
				
				// case for positive number
				String n = "";
				if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9'){
					boolean scanning = true;
					char lc;
					n = c+"";
					while (scanning){
						
						//we need to peek ahead.
						bis.mark(1);
						lc = (char) bis.read();
						bis.reset();
						
						//is this still part of the number?
						if (lc == '0' || lc == '1' || lc == '2' || lc == '3' || lc == '4' || lc == '5' || lc == '6' || lc == '7' || lc == '8' || lc == '9'){
							n += lc;
							bis.read();
						}
						else{ // if not, we are done here
							scanning = false;
						}
					}
					if (negative){ return "-"+n;}
					else{ return n;}
				}
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//end of file case
		return null;
	}
	
	
}
