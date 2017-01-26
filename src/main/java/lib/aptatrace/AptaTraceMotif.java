package lib.aptatrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList; 
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import gui.aptatrace.logo.Logo;
import gui.aptatrace.logo.LogoSummary;
import gui.aptatrace.logo.LogoSummary2;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class AptaTraceMotif {
		
	private static char[] nu={'A','G','T','C'};
	private static int[] fourToPower={1,4,16,64,256,1024,4096,16384,65536,262144,1048576,4194304,16777216};
		
	/**
	 * Deletes a directory, possibly containing files and subfolders, from the medium.
	 * @param dir The directory to delete
	 */
	public static void removeDirectory(File dir) 
	{
	    if (dir.isDirectory()) 
	    {
	        File[] files = dir.listFiles();
	        if (files != null && files.length > 0) 
	        {
	            for (File aFile : files) 
	            {
	                removeDirectory(aFile);
	            }
	        }
	        dir.delete();
	    } 
	    else 
	    {
	        dir.delete();
	    }
	}
	
	/**
	 * Removes all files and subfolders from a directory without deleting the folder itself. 
	 * @param dir The directory to clean.
	 */
	public static void cleanDirectory(File dir) 
	{
	    if (dir.isDirectory()) 
	    {
	        File[] files = dir.listFiles();
	        if (files != null && files.length > 0) 
	        {
	            for (File aFile : files) 
	            {
	                removeDirectory(aFile);
	            }
	        }
	    }
	}
	
	// return an id of a character 
	public static int getNuId(char c){
		switch ( c ) {
			case 'A': return 0; 
			case 'G': return 1; 
			case 'T': return 2;
			case 'U': return 2;
			case 'C': return 3;
			default: return -1;
		}
	}
	
	public static String fillBlanks(String alignment,String aptamer,int pos){
		String a="";
		int firstPos=0;
				
		for (int i=0;i<alignment.length();i++)
			if (alignment.charAt(i)!='-'){
				firstPos=i;
				break;
			}
		
		for (int i=0;i<alignment.length();i++)
		if (alignment.charAt(i)!='-')
			a=a+alignment.charAt(i);
		else{
			a=a+aptamer.charAt(pos-(firstPos-i));
		}
		
		return a;
	}
	
	/**
	 * A procedure to calculate the id of a kmer when it is overlapping with its left kmer when sliding character by character of 
	 * when sliding from left to right of an aptamer sequence from left to right 
	 * @param oldId	the id of left kmer 
	 * @param lastNu the leftmost character of the left kmer
	 * @param newNu	the rightmost character of the right kmer
	 * @param klen length of the kmer
	 * @return id of the right kmer
	 */
	public static int calulateNewId(int oldId,char lastNu,char newNu,int klen){
		return	(4*(oldId-getNuId(lastNu)*fourToPower[klen-1])+getNuId(newNu)); 
	}
	
	// return the id of a given k-mer, the id will be the index of the given k-mer in kmersArr
	public static int calculateId(String kmer){
		int id=0;
		for (int i=0;i<kmer.length();i++)
			id+=getNuId(kmer.charAt(i))*fourToPower[kmer.length()-i-1];
		return id;
	}
		
	
	// A recursive procedure to generate all possible number of k-mers given the length klength
	static void generateAllKmers(int k,String current,ArrayList<String> arr,int klength){
		if (k==klength)
			arr.add(current);
		else{
			for (int i=0;i<4;i++){
				String next=current+nu[i];
				generateAllKmers(k+1,next,arr,klength);
			}
		}
	}

	// pair alignment of two kmers a and b
	public static String[] pairAlignment(String a,String b){
		String savea="";
		String saveb="";
		String ret[]=new String[2];
		
		String tmpa;
		String tmpb;
		
		int mS=2;
		int nR;
		int nI;
		int sC=a.length()+1;
		int sI=sC;
		int sR=sC;
		
		int bcs=0;
		int scs=0;
		
		for (int i=-mS;i<=mS;i++){
			tmpa=a;
			tmpb=b;
			nR=0;
			nI=0;
			if (i<0){
				for (int j=1;j<=-i;j++){
					tmpa="-"+tmpa;
					tmpb=tmpb+"-";
				}
					
			}
			else if (i>0){
				for (int j=1;j<=i;j++){
					tmpa=tmpa+"-";
					tmpb="-"+tmpb;
				}
			}
			
			int nc=0;
			
			for (int j=0;j<tmpa.length();j++)
			if ((tmpa.charAt(j)=='-')||(tmpb.charAt(j)=='-')){
				nI++;
				nc=0;
			}
			else if (tmpa.charAt(j)!=tmpb.charAt(j)){
				nR++;
				nc=0;
			}
			else{
				nc+=1;
				if (nc>bcs)
					bcs=nc;
			}
			
			if (((nI+nR)<=sC)&&(nI<=4)){
					sC=nI+nR;
					sI=nI;
					sR=nR;
					scs=bcs;
					savea=tmpa;
					saveb=tmpb;
			}
		}
		
		ret[0]=savea;
		ret[1]=saveb;
		
		return ret;
	}
	
	
	// to compute alignment of all the kmers in a give cluster of kmers stored in sArr 
    public static String[] multipleAlignment(ArrayList<String> sArr){    	
    	String seed=sArr.get(0);
    	String[] a=new String[sArr.size()];
    	
    	String seedA=seed;					// seed alignment
    	String[] result;
    	int[] l=new int[sArr.size()];		// number of left gaps
    	int[] r=new int[sArr.size()];		// number of right gaps
    	int cl;
    	int cr;
    	int ml=0;							// max gaps on the left
    	int mr=0;							// max gaps on the right
    	
    	a[0]=seed;
    	l[0]=0;
    	r[0]=0;
    	for (int i=1;i<sArr.size();i++){
    		result = pairAlignment(sArr.get(0), sArr.get(i));
    		seedA=result[0];
    		a[i]=result[1];
    		cl=-1;
    		while (seedA.charAt(cl+1)=='-')
    			cl++;

    		l[i]=cl+1;
    		
    		cr=seedA.length();
    		while (seedA.charAt(cr-1)=='-')
    			cr--;
    		
    		r[i]=a[i].length()-cr;
    		
    		if (r[i]>mr)
    			mr=r[i];
    		
    		if (l[i]>ml)
    			ml=l[i];
    	}
    	
    	for (int i=0;i<sArr.size();i++){
    		for (int k=l[i]+1;k<=ml;k++)
    			a[i]="-"+a[i];
    		
    		for (int k=r[i]+1;k<=mr;k++)
    			a[i]=a[i]+"-";
    	}
    	
    	return a;
    }
	
    /**
     * Determine whether two kmers has good overlap to put in the same cluster or not
     * @param a	the first kmer
     * @param b	the second kmer
     * @return	true if they have good overlap
     */
	public static boolean hasGoodOverlap(String a,String b){
		String savea="";
		String saveb="";
		
		String tmpa;
		String tmpb;
		
		int mS=2;
		int nR;
		int nI;
		int sC=a.length()+1;
		int sI=sC;
		int sR=sC;
		
		int bcs=0;	// save the longest substring given an alignment of the kmer a and b
		int scs=0;	// save the longest common substring of all the alignments when sliding kmer a from left to right of kmer b
		
		// sliding the kmer a from left to right of kmer b
		for (int i=-mS;i<=mS;i++){
			tmpa=a;
			tmpb=b;
			nR=0;
			nI=0;
			if (i<0){
				for (int j=1;j<=-i;j++){
					tmpa="-"+tmpa;
					tmpb=tmpb+"-";
				}
					
			}
			else if (i>0){
				for (int j=1;j<=i;j++){
					tmpa=tmpa+"-";
					tmpb="-"+tmpb;
				}
			}
			
			int nc=0;
			
			for (int j=0;j<tmpa.length();j++)
			if ((tmpa.charAt(j)=='-')||(tmpb.charAt(j)=='-')){
				nI++;
				nc=0;
			}
			else if (tmpa.charAt(j)!=tmpb.charAt(j)){
				nR++;
				nc=0;
			}
			else{
				nc+=1;
				if (nc>bcs)
					bcs=nc;
			}
			
			if ((nI+nR)<=sC){
					sC=nI+nR;
					sI=nI;
					sR=nR;
					scs=bcs;
					savea=tmpa;
					saveb=tmpb;
			}
		}
		

			if (sI==0){
				if (sR==1)
					return true;
			}
			else{
				if ((a.length()<=6)&&(scs>=4))
					return true;
				else if ((a.length()>=7)&&(scs>=5))
					return true;
			}	
			
		return false;
	}
	
		
	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "true");
		String outputPath="";			
		String outputPrefix="aptatrace";			
		String fivePrime="";				
		String threePrime="";
		ArrayList<String> roundArr=new ArrayList<String>();
		int klength=0;
		boolean filterClusters=true;
		boolean outputClusters=false;
		LogoSummary summary = new LogoSummary();
		LogoSummary2 summary2 = new LogoSummary2();
		
		if ((args.length==0)||(!(new File(args[0]).exists()))){		
			System.out.println("AptaTrace Motif Extraction Utility v0.3");
			System.out.println("Usage: java -jar AptaTraceMotif.jar path/to/config/file.cfg");
			System.exit(0);
		}
		
		String configFile=args[0];
		int singletonThres = 3;
		double theta=10.0;
		String line="";
		int currentr=0;
		
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
			
			// reading the config file and setting parameters	
		    while ((line = br.readLine()) != null) {
		    	line=line.trim();
		    	String[] tokens = line.split("[\t=]");
		    	if (!line.startsWith("#")){
			    	if (line.contains("output_folder")){
			    		outputPath=tokens[1];
			    	}
			    	else if (line.contains("result_path")){
			    		outputPath=tokens[1];
			    	}
			    	else if (line.contains("primer5")){
			    		fivePrime=tokens[1];
			    	}
			    	else if (line.contains("primer3")){
			    		threePrime=tokens[1];
			    	}
			    	else if ((line.contains("["))&&(!line.contains("Settings"))&&(!line.contains("#"))){
			    		roundArr.add(tokens[0].substring(1,tokens[0].length()-1));
			    		currentr+=1;
			    	}
			    	else if (line.contains("kmer_length")){
			    		klength=Integer.parseInt(tokens[1]);
			    	}
			    	else if (line.contains("alpha")){
			    		singletonThres=Integer.parseInt(tokens[1]);
			    	}
			    	else if (line.contains("theta")){
			    		theta=Double.parseDouble(tokens[1]);
			    	}
			    	else if (line.contains("filter_clusters")){
			    		if (line.contains("True"))
			    			filterClusters=true;
			    		else
			    			filterClusters=false;
			    	}
			    	else if (line.contains("output_aptamers")){
			    		if (line.contains("True"))
			    			outputClusters=true;
			    		else
			    			outputClusters=false;
			    	}
		    	}
		    }
			br.close();
		}
		catch (Exception e){
			System.out.println(line);
			e.printStackTrace();
			System.exit(1);
		}
		
		int[] rc=new int[roundArr.size()];
		int numR=roundArr.size();
		HashMap<String,Integer> round2Id=new HashMap<String,Integer>();
		final int numOfContexts=6;
		for(int i=0;i<roundArr.size();i++){
			round2Id.put(roundArr.get(i), i);
			rc[i]=0;
		}
		
		String resultFolder="k"+klength+"alpha"+singletonThres;
		
		// print out all the paramters
		System.out.println("\nREADING ALL THE PARAMETERS:");	
		System.out.println("Kmer length: "+klength);		
		System.out.println("Five prime end: "+fivePrime+" and three prime end: "+threePrime);
		System.out.println("Singleton threshold (alpha): "+singletonThres);
		System.out.println("Output path: "+outputPath);
		System.out.println("Prefix output files and directory: "+outputPrefix);
				
		System.out.println("And the cycles/rounds:");
		for (int i=0;i<roundArr.size();i++){
			String rd=roundArr.get(i);
			System.out.println(rd);
		}
		
		String[] roundIDs=roundArr.toArray(new String[roundArr.size()]);
		
		// to generate all possible number of kmers
		int numk=0;
		final ArrayList<String> kmersArr=new ArrayList<String>();
		generateAllKmers(0,"",kmersArr,klength);
		numk=kmersArr.size();
				
		final double kCountPerR[][]=new double[kmersArr.size()][];
		for (int i=0;i<kmersArr.size();i++){
			kCountPerR[i]=new double[roundArr.size()];
			for (int j=0;j<roundArr.size();j++)
				kCountPerR[i][j]=0.0f;
		}
		       
		KContextTrace[] mkc=new KContextTrace[numk];;
		for (int i=0;i<numk;i++){
			//mkc[i]=new KContextTrace(i2k.get(i),numR,numOfContexts-1);
			mkc[i]=new KContextTrace(kmersArr.get(i),numR,numOfContexts-1);
		}
		
		System.out.println("\nREADING SECONDARY STRUCTURE PROFILES OF APTAMERS FROM FOLDERS:");
		
		int lastRoundCount=0;
		String aptamer="";
		
		int differentLength=0;
		
		// iterate through the secondary structure profiles of all the aptamers 
		// to calculate the context shifting scores of the kmers
		try {
			int i=0;
			String dataFolder=outputPath+"/data/"+i;
			
			while (new File(dataFolder).exists()){
				System.out.print(dataFolder+"\r");
				String inputStructureFile=dataFolder+"/aptatrace_prepare_sequence_profiles.txt.gz";
				
				GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(inputStructureFile));
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				int numOR;
				String[] arr;
				String[] arr1;
				int[] occRArr;
				int[] occCArr;
				int id=0;
				int startPos;
				int rid;
				double[][] contextProbArr=new double[5][];
				double[] avgContextProbArr=new double[5];
				boolean firstRead=true;
				IntOpenHashSet seen=new IntOpenHashSet();
				
				while ((line = br.readLine()) != null) {
					line=line.trim();
					arr=line.split("[>\t]+");
					seen.clear();
					aptamer=arr[1];
					arr1=arr[4].split(",");
					numOR=arr1.length;
					occRArr=new int[numOR];
					occCArr=new int[numOR];
					for (int r=0;r<numOR;r++)
						occCArr[r]=Integer.parseInt(arr1[r]);
					
					arr1=arr[3].split(",");
					for (int r=0;r<numOR;r++){
						rid=round2Id.get(arr1[r]);
						occRArr[r]=rid;
						//System.out.print(" "+rid);
						rc[occRArr[r]]+=occCArr[r];
						if (occRArr[r]==roundArr.size()-1)
							lastRoundCount+=occCArr[r];
					}
				
					if (firstRead){
						for (int j=0;j<5;j++)
							contextProbArr[j]=new double[arr[1].length()];
					}
					
					if (contextProbArr[0].length!=aptamer.length()){
						differentLength+=1;
						
						if (contextProbArr[0].length<aptamer.length()){
							for (int j=0;j<5;j++)
								contextProbArr[j]=new double[aptamer.length()];
						}
						
						if (differentLength==1)
							System.out.println("\nWarnings: aptamers have different lengths !");
					}
					
					for (int j=0;j<5;j++){
						line = br.readLine();
						line=line.trim();
						arr1=line.split("[\t ]+");
						
						if (aptamer.length()!=arr1.length)
							throw new Exception("The profile array length is not the same as the aptamer length!!!");
												
						contextProbArr[j][0]=Double.parseDouble(arr1[0]);
						for (int k=1;k<arr1.length;k++){
							contextProbArr[j][k]=Double.parseDouble(arr1[k])+contextProbArr[j][k-1];
						}
					}
					
					startPos=(klength+fivePrime.length()-1);
												
					// iterate through every kmer of the aptamer under consideration
					// and sum up its number of occurrences and the sums of the probabilities of being in various structural context 
					for (int k=startPos;k<(arr[1].length()-threePrime.length());k++){
						if (k==startPos)
							id=calculateId(arr[1].substring(k-klength+1,k+1));
						else
							id=calulateNewId(id,arr[1].charAt(k-klength),arr[1].charAt(k),klength);
						
						
						for (int j=0;j<5;j++)
							avgContextProbArr[j]=(contextProbArr[j][k]-contextProbArr[j][k-klength+1])/(klength*1.0f);
						
						if (!seen.contains(id)){
							seen.add(id);
							for (int l=0;l<numOR;l++){
								mkc[id].addTotalCount(occRArr[l],occCArr[l]);
							}
						}
						
						for (int l=0;l<numOR;l++){
							if (singletonThres>0){
								if (occCArr[l]>singletonThres)
									mkc[id].addContextProb(occRArr[l], occCArr[l],avgContextProbArr);
								if (occCArr[l]<=singletonThres)
									mkc[id].addSingletonContextProb(occRArr[l], occCArr[l], avgContextProbArr);	
							}
							else
								mkc[id].addSingletonContextProb(occRArr[l], occCArr[l], avgContextProbArr);
						}	
					}
					
					firstRead=false;
				}
				
			    br.close();
			    gzip.close();
				i++;
				dataFolder=outputPath+"/data/"+i;
			}
			
		}
		catch (Exception e){
			System.out.println();
			System.out.println("Error in reading aptamer "+aptamer);
			System.out.println(line);
			e.printStackTrace();
			System.exit(1);
		}
		
		try 
		{
			// create the result folder if it does not exist
			File resultPath = new File(outputPath+"/results");
			if (!resultPath.exists()) 
			{
				resultPath.mkdir();
			}
			
			// create the specific folder for this run
			File resultDirectory = new File(outputPath+"/results/"+resultFolder);
			if (!resultDirectory.exists())
			{
				resultDirectory.mkdir();
			}
			// otherwise remove any previous content
			else
			{
				cleanDirectory(resultDirectory);
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
				
		for (int i=numk-1;i>=0;i--){	
			mkc[i].setStrongPresence(lastRoundCount);
			mkc[i].checkEnoughOccurrences();
			if (mkc[i].hasEnoughOccurrences()){
				mkc[i].normalizeProfile();
				mkc[i].deriveSelectionContext();
				mkc[i].calculateSingletonKLScore(rc);
				mkc[i].calculateKLScore(rc);
			}
		}
		
		class Pair<F, S> {
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
	    }
				
		ArrayList<Double> singletonKLScores=new ArrayList<Double>();	// the arraylist storing background context shifting scores for aptamers with singleton occurrences
		ArrayList<Double> kmerKLScores=new ArrayList<Double>();			// the arraylist storing context shifting scores for aptamers with non-singleton occurrences
		ArrayList<Double> proportionAL=new ArrayList<Double>();			// the arraylist storing the proprotions of kmer occurrences in the final selection round
		for (int i=0;i<mkc.length;i++)
		if (mkc[i].hasEnoughOccurrences()){
			singletonKLScores.add(Math.log((double)mkc[i].getSingletonKLScore()));
			kmerKLScores.add(Math.log((double)mkc[i].getKLScore()));
			proportionAL.add(mkc[i].getProportion());
		}
		
		double singletonKLScoreArr[]=new double[singletonKLScores.size()];		// the array storing background context shifting scores for aptamers with singleton occurrences
		double kmerKLScoreArr[]=new double[kmerKLScores.size()];				// the array storing context shifting scores for aptamers with non-singleton occurrences
		double sortedKLScoreArr[]=new double[kmerKLScores.size()];				// the sorted array storing context shifting scores for aptamers with non-singleton occurrences
		double sortedProportionArr[]=new double[kmerKLScores.size()];			// the arraylist storing the proportions of kmer occurrences in the final selection round
		for (int i=0;i<singletonKLScores.size();i++)
			singletonKLScoreArr[i]=singletonKLScores.get(i);
		
		/*
		for (int i=0;i<mkc.length;i++)
		if (mkc[i].hasEnoughOccurrences()){
			mkc[i].computePValue(singletonKLScoreArr);
		}
		*/
		
		for (int i=0;i<kmerKLScores.size();i++){
			kmerKLScoreArr[i]=kmerKLScores.get(i);
			sortedKLScoreArr[i]=kmerKLScores.get(i);
			sortedProportionArr[i]=proportionAL.get(i);
		}
		Arrays.sort(sortedKLScoreArr);
		Arrays.sort(sortedProportionArr);
		
		DescriptiveStatistics ds=new DescriptiveStatistics(singletonKLScoreArr);
		double sMean=ds.getMean();
		double sStd=ds.getStandardDeviation();
		
		//theta=(mkc.length*theta)/(kmerKLScoreArr.length*1.0);
		//theta=10.0;
		
		// in the case we have many significant context shifting scores just take at most top 10 percent of the scores
		// and topThetaValue is the 90 quantile of all the context shifting scores
		double topThetaValue=sortedKLScoreArr[(int)Math.floor(((100.0-theta)/100.0)*sortedKLScoreArr.length)];
		double topThetaProportion=sortedProportionArr[(int)Math.floor(((100.0-theta)/100.0)*sortedKLScoreArr.length)];		
		/*
		System.out.println("NULL DISTRIBUTIONS: ");
		System.out.println("Range: ["+ds.getMin()+","+ds.getMax()+"]"+" with mean "+sMean+" and standard deviation: "+sStd);
		
		
		ds=new DescriptiveStatistics(kmerKLScoreArr);
		sMean=ds.getMean();
		sStd=ds.getStandardDeviation();
		System.out.println("SCORE DISTRIBUTIONS: ");
		System.out.println("Range: ["+ds.getMin()+","+ds.getMax()+"]"+" with mean "+sMean+" and standard deviation: "+sStd);
		*/
		
		try {
			PrintWriter writer=new PrintWriter(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_singletonKLScore.txt", "UTF-8");
			for (int i=0;i<mkc.length;i++)
			if ((mkc[i].hasEnoughOccurrences())&&(mkc[i].getSingletonKLScore()>0)){
				writer.println(mkc[i].getKmer()+"\t"+Math.log(mkc[i].getSingletonKLScore()));
			}
			writer.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		double pvalue=0.01;
		boolean hasSigKmer=false;
		
		for (int i=0;i<numk;i++)
		if ((mkc[i].hasEnoughOccurrences())&&(mkc[i].isSignificant(singletonKLScoreArr,topThetaValue,pvalue)))
				hasSigKmer=true;
		
		if (!hasSigKmer)
			pvalue=0.05;
		
		// to select the kmers with statistically significant context scores 
		// in the case there are many such scores just take the ones within top 10 percent only
		ArrayList<KContextTrace> sorted=new ArrayList<KContextTrace>();
		for (int i=0;i<numk;i++)
		if (mkc[i].hasEnoughOccurrences())
		if ((mkc[i].isSignificant(singletonKLScoreArr,topThetaValue,pvalue)) || ( (mkc[i].isSignificant(singletonKLScoreArr,topThetaValue,pvalue)) && (mkc[i].getProportion()>=topThetaProportion) &&(sorted.get(i).hasStrongPresence())))
			sorted.add(mkc[i]);
		
		Collections.sort(sorted);
		
		int lastk=-1;
		boolean[] got=new boolean[sorted.size()];
		for (int i=sorted.size()-1;i>=0;i--)
			got[i]=false;
		
		
		int totalSeeds=0; 		
		double totalAcceptedScore=0.0;
		
		try {
			PrintWriter writer=new PrintWriter(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_KLScore.txt", "UTF-8");
			for (int i=numk-1;i>=0;i--)
			if ((mkc[i].hasEnoughOccurrences())&&(mkc[i].getSingletonKLScore()>0))
				writer.println(mkc[i].getKmer()+"\t"+Math.log(mkc[i].getKLScore()));
			writer.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		
		int numClus=0;
		
		
		int numPasses=0;
		for (int i=sorted.size()-1;i>=0;i--)
		if (!got[i])
			numPasses++;
		
		/*
		System.out.println("\nSIGNIFICANT KMERS:");
		for (int i=sorted.size()-1;i>=0;i--)
			System.out.println(sorted.get(i).getKmer()+" "+sorted.get(i).getKLScore()+" "+sorted.get(i).hasStrongPresence());
		*/
		
	    ArrayList<Pair<Integer,Double>> sortedClus=new ArrayList<Pair<Integer,Double>>();
			    
	    System.out.println("\nCLUSTERING SIGNIFICANT KMERS:");
	    Int2IntOpenHashMap kmer2Clus=new Int2IntOpenHashMap();					//	map a kmer id to a cluster id
	    ArrayList<MotifProfile> outputMotifs=new ArrayList<MotifProfile>();		//	the motif profiles of clusters of kmers	
	    //ArrayList<Double> seedPValueArr=new ArrayList<Double>();				
	    ArrayList<Integer> MotifSeedIDArr=new ArrayList<Integer>();				//	the array storing the ids of the seeds of the clusters		
	    
	    // iterate the sorted list of kmers by their proportions starting with the kmer with the highest proportion in the last selection round
	    // pick the first one that is not chosen as the seed of a new cluster
	    // then find similar kmers with less portion and put in the cluster
		for (int i=sorted.size()-1;i>=0;i--)
		if ((!got[i])&&(sorted.get(i).hasStrongPresence())){
			ArrayList<String> clus=new ArrayList<String>();
			String clusAlignment[];
			double seedProportion=sorted.get(i).getProportion();
			double seedPValue=sorted.get(i).getPValue();
			
			
			numClus+=1;
			sortedClus.add(new Pair<Integer,Double>(numClus,seedPValue));
			kmer2Clus.put(calculateId(sorted.get(i).getKmer()), numClus-1);
			
			got[i]=true;
			clus.add(sorted.get(i).getKmer());
			
			
			totalSeeds+=1;
			totalAcceptedScore+=Math.abs((sorted.get(i).getKLScore()-sMean)/sStd);
			//System.out.println("--------------------------------------------------------------");
			//sorted.get(i).printOut();
			
			// pick similar kmers and add to the cluster
			for (int j=i-1;j>=0;j--)
			if ((!got[j])&&(hasGoodOverlap(sorted.get(i).getKmer(),sorted.get(j).getKmer()))&&(sorted.get(j).getSelectionContext()==sorted.get(i).getSelectionContext()))
			{
				clus.add(sorted.get(j).getKmer());				
				kmer2Clus.put(calculateId(sorted.get(j).getKmer()), numClus-1);
				got[j]=true;
				//sorted.get(j).printOut();
			}
			//System.out.println("--------------------------------------------------------------");
			
			clusAlignment=multipleAlignment(clus);
			
			MotifSeedIDArr.add(i);
			outputMotifs.add(new MotifProfile(clusAlignment[0].length(),numR));
			for (int j=0;j<clus.size();j++){
				outputMotifs.get(numClus-1).addKmer(sorted.get(j).getKmer());
			}
			
			try{
				PrintWriter writer=new PrintWriter(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus_tmp_"+numClus+".txt");
				//writer.println(seedPValue+"\t"+seedProportion);
				writer.format("%g\t%.2f%%\n",seedPValue,seedProportion*100.0);
				for (int j=0;j<clus.size();j++){
					outputMotifs.get(numClus-1).addKmerAlignment(clus.get(j),clusAlignment[j]);
					writer.println(clus.get(j)+"\t"+clusAlignment[j]);
				}
				writer.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		/*
		Collections.sort(sortedClus);
		for (int i=0;i<sortedClus.size();i++){
			try{
				String cmd="mv "+experiment_name+"_"+klength+"_clus"+(sortedClus.get(i).getFirst())+"_tmp.txt "+experiment_name+"_"+klength+"_clus"+(i+1)+".txt";
				System.out.println(cmd);
				Process p = Runtime.getRuntime().exec(cmd);
				p.waitFor();
		
				cmd="mv "+experiment_name+"_"+klength+"_clus"+(sortedClus.get(i).getFirst())+"_context_tmp.txt "+experiment_name+"_"+klength+"_clus"+(i+1)+"_context.txt";
				System.out.println(cmd);
				p = Runtime.getRuntime().exec(cmd);
				p.waitFor();
			}
			catch(Exception e){
				e.printStackTrace();
			} 
		}
		*/	
		
		System.out.println("Number of seeds/clusters before filtering: "+totalSeeds);
		System.out.println("\nCOMPUTE THE CONTEXT TRACE AND PWM OF EACH CLUSTER:");
		
		Int2IntOpenHashMap id2Count=new Int2IntOpenHashMap();	// last round aptamer id with counts  
		
		// iterate through structural context profiles of aptamers 
		// to calculate the PWM and context trace of the motifs
		try {
			int i=0;
			String dataFolder=outputPath+"/data/"+i;
			
			while (new File(dataFolder).exists()){
				System.out.print(dataFolder+"\r");
				String inputStructureFile=dataFolder+"/aptatrace_prepare_sequence_profiles.txt.gz";
				
				GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(inputStructureFile));
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				int numOR;
				String[] arr;
				String[] arr1;
				String[] occR;
				String[] occC;
				int[] occRArr;
				int[] occCArr;
				int id=0;
				int startPos;
				int rid;
				int aid;
				double[][] contextProbArr=new double[5][];
				double[] avgContextProbArr=new double[5];
				boolean firstRead=true;
				IntOpenHashSet seen=new IntOpenHashSet();
				int mid;
				String kmer;
				
				while ((line = br.readLine()) != null) {
					line=line.trim();
					arr=line.split("[>\t]+");
					aid=Integer.parseInt(arr[2]);
					seen.clear();
					aptamer=arr[1];
					arr1=arr[4].split(",");
					numOR=arr1.length;
					occRArr=new int[numOR];
					occCArr=new int[numOR];
					for (int r=0;r<numOR;r++)
						occCArr[r]=Integer.parseInt(arr1[r]);
					
					arr1=arr[3].split(",");
					for (int r=0;r<numOR;r++){
						rid=round2Id.get(arr1[r]);
						occRArr[r]=rid;
					}
					

				
					if (firstRead){
						for (int j=0;j<5;j++)
							contextProbArr[j]=new double[arr[1].length()];
					}
					
					if (aptamer.length()>contextProbArr[0].length){
						for (int j=0;j<5;j++)
							contextProbArr[j]=new double[aptamer.length()];
					}
					
					for (int j=0;j<5;j++){
						line = br.readLine();
						line=line.trim();
						arr1=line.split("[\t ]+");
						contextProbArr[j][0]=Double.parseDouble(arr1[0]);
						for (int k=1;k<arr1.length;k++)
							contextProbArr[j][k]=Double.parseDouble(arr1[k])+contextProbArr[j][k-1];
					}
					
					startPos=(klength+fivePrime.length()-1);
					
					// iterate through each kmer of the aptamer and decide whether the kmer is in the list of kmers with significant context shifting scores
					// if it is, summing the total number of occurrences and the sums of probabilities of being in various structural context  of its motifs
					for (int k=startPos;k<(arr[1].length()-threePrime.length());k++){
						kmer=arr[1].substring(k-klength+1,k+1);
						if (k==startPos)
							id=calculateId(kmer);
						else
							id=calulateNewId(id,arr[1].charAt(k-klength),arr[1].charAt(k),klength);
						
						if (kmer2Clus.containsKey(id)){ 
							mid=kmer2Clus.get(id);

							if ((occRArr[occRArr.length-1]==roundArr.size()-1)){
								if (occCArr[occCArr.length-1]>singletonThres){
										outputMotifs.get(mid).addToPWM(fillBlanks(outputMotifs.get(mid).getKmerAlignment(kmer),arr[1],k-klength+1), occCArr[occCArr.length-1]);
										outputMotifs.get(mid).addOccId(aid, occCArr[occCArr.length-1]);
								}
								else{
									outputMotifs.get(mid).addToSingletonPWM(fillBlanks(outputMotifs.get(mid).getKmerAlignment(kmer),arr[1],k-klength+1), occCArr[occCArr.length-1]);
								}
								id2Count.put(aid, occCArr[occCArr.length-1]);
							}
							
							for (int j=0;j<5;j++)
								avgContextProbArr[j]=(contextProbArr[j][k]-contextProbArr[j][k-klength+1])/(klength*1.0f);
							
							if (!seen.contains(mid)){
								seen.add(mid);
								for (int l=0;l<numOR;l++)
									outputMotifs.get(mid).addTotalCount(occRArr[l],occCArr[l]);
							}
							
							for (int l=0;l<numOR;l++){
								if ((occCArr[l]>singletonThres)||(occRArr[l]==0)){
									outputMotifs.get(mid).addContextProb(occRArr[l], occCArr[l],avgContextProbArr);
								}
								if (occCArr[l]<=singletonThres)
									outputMotifs.get(mid).addSingletonContextProb(occRArr[l], occCArr[l], avgContextProbArr);							
							}	
						}
					}
					
					firstRead=false;
				}
				
			    br.close();
			    gzip.close();
				i++;
				dataFolder=outputPath+"/data/"+i;
			}
			
			boolean[] filtered=new boolean[outputMotifs.size()];
			for (int j=0;j<filtered.length;j++)
				filtered[j]=false;
			
			if (filterClusters){
				System.out.println("\nFILTERING MOTIFS...");
				
				IntOpenHashSet curOccSet=new IntOpenHashSet();
				// filters out the smaller motifs that their intersection with larger motifs more than 2/3 of their sizes 
				for (int j=0;j<outputMotifs.size();j++){
					if (outputMotifs.get(j).getOverlapPercentage(curOccSet, id2Count)<=0.67){
						outputMotifs.get(j).addTo(curOccSet, id2Count);
						filtered[j]=false;
					}
					else
						filtered[j]=true;
				}
			}
			
			
			ArrayList<PrintWriter> clusterWriter=new ArrayList<PrintWriter>();
			HashMap<Integer,Integer> c2fc=new HashMap<Integer,Integer>();

			// prints out the PWM matrixes and aptamers that contain the motifs
			int numRM=0;
			for (int j=0;j<outputMotifs.size();j++)
			if (!filtered[j]){
				numRM++;
				outputMotifs.get(j).normalizeProfile();
				outputMotifs.get(j).calculateProportion(rc);
				PrintWriter writer=new PrintWriter(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(numRM)+"_pwm.txt");
								
				if (outputClusters){
					c2fc.put(j, numRM-1);
					clusterWriter.add(new PrintWriter(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(numRM)+"_aptamers.txt"));
				}
				
			    outputMotifs.get(j).trim();
			    outputMotifs.get(j).printPWM(writer);
				writer.close();
				
				double[][] pwm=outputMotifs.get(j).getPWM();
				String[] pid=new String[pwm.length];
				for (int k=0;k<pid.length;k++)
					pid[k]=String.valueOf(k+1);
				Logo seq = new Logo(pwm, pid);
				seq.setAlphabetNucleotides();
				seq.setAlphabetRibonucleotides();
				seq.setBit(true);
				seq.saveAsPDF(600, 400, outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(numRM)+"_pwm.pdf");
				
				double[][] traceMat=outputMotifs.get(j).getTraceMatrix();
				
				Logo trace = new Logo(traceMat, roundIDs);
				trace.setAlphabetContexts();
				trace.setBit(false);
				trace.saveAsPDF(600, 400, outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(numRM)+"_context.pdf");
				
				
				writer=new PrintWriter(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(numRM)+"_context.txt");
				outputMotifs.get(j).printContextTrace(writer,roundArr);
				writer.close();
				
				File file1 = new File(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus_tmp_"+(j+1)+".txt");
				File file2 = new File(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(numRM)+".txt");
				file1.renameTo(file2);
			}
			else{
				File file1 = new File(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus_tmp_"+(j+1)+".txt");
				file1.delete();
			}
			
			String cmd;
			Process p;
			
			if (filterClusters)
				System.out.println("Number of remaining motifs: "+numRM);
			
			/*
			System.out.println();
			for (int j=0;j<outputMotifs.size();j++)
			if (!filtered[j]){
				//System.out.println("Motif "+(j+1)+" occupies "+outputMotifs.get(j).getProportion()+" of final pool");
				System.out.format("Motif %d occupies %.2f%% of final pool\n",(j+1),outputMotifs.get(j).getProportion()*100.0);
			}
			*/
			
			for (int j=0;j<outputMotifs.size();j++)
			if (!filtered[j]){
				
				int idl=outputMotifs.get(j).getPWM().length;
				
				String[] id = new String[idl];
				for (int k=0;k<id.length;k++)
					id[k]=Integer.toString(k+1);
		
				//summary.AddRow(new Logo(outputMotifs.get(j).getPWM(),id),sorted.get(MotifSeedIDArr.get(j)).getKmer(),sorted.get(MotifSeedIDArr.get(j)).getPValue(), outputMotifs.get(j).getProportion()*100.00,outputMotifs.get(j).getTrace(roundIDs));
				summary2.AddRow(new Logo(outputMotifs.get(j).getPWM(),id),sorted.get(MotifSeedIDArr.get(j)).getKmer(),sorted.get(MotifSeedIDArr.get(j)).getPValue(), sorted.get(MotifSeedIDArr.get(j)).getProportion()*100.0, outputMotifs.get(j).getProportion()*100.00,outputMotifs.get(j).getTrace(roundIDs));
			}
			
			//summary.saveAsPDF(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_summary.pdf");
			summary2.saveAsPDF(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_fullsummary.pdf");
			
			// prints out the aptamers in the last selection round that have frequency more than singleton threshold and contain the motifs
			if (outputClusters){
				i=0;
				dataFolder=outputPath+"/data/"+i;
				
				System.out.println("\nREADING DATA AGAIN AND OUTPUTING APTAMERS CONTAINING THE MOTIFS:");
				while (new File(dataFolder).exists()){
					System.out.print(dataFolder+"\r");
					String inputStructureFile=dataFolder+"/aptatrace_prepare_sequence_profiles.txt.gz";
					
					GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(inputStructureFile));
					BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
					int numOR;
					String[] arr;
					String[] arr1;
					String[] occR;
					String[] occC;
					int[] occRArr;
					int[] occCArr;
					int id=0;
					int startPos;
					int rid;
					int aid;
					boolean firstRead=true;
					IntOpenHashSet seen=new IntOpenHashSet();
					int mid;
					String kmer;
					
					while ((line = br.readLine()) != null) {
						line=line.trim();
						arr=line.split("[>\t]+");
						aid=Integer.parseInt(arr[2]);
						seen.clear();
						arr1=arr[4].split(",");
						numOR=arr1.length;
						occRArr=new int[numOR];
						occCArr=new int[numOR];
						for (int r=0;r<numOR;r++)
							occCArr[r]=Integer.parseInt(arr1[r]);
						
						arr1=arr[3].split(",");
						for (int r=0;r<numOR;r++){
							rid=round2Id.get(arr1[r]);
							occRArr[r]=rid;
						}
						
						for (int j=0;j<5;j++){
							line = br.readLine();
							line=line.trim();
							arr1=line.split("[\t ]+");
							
						}
						
						startPos=(klength+fivePrime.length()-1);
						IntOpenHashSet seencid=new IntOpenHashSet();
						
						// iterate through each kmer of the aptamer and decide whether the aptamer contains a motif
						for (int k=startPos;k<(arr[1].length()-threePrime.length());k++){
							kmer=arr[1].substring(k-klength+1,k+1);
							if (k==startPos)
								id=calculateId(kmer);
							else
								id=calulateNewId(id,arr[1].charAt(k-klength),arr[1].charAt(k),klength);
							
							if (kmer2Clus.containsKey(id)){ 
								mid=kmer2Clus.get(id);
								if ((occRArr[occRArr.length-1]==roundArr.size()-1)){
									if ((!filtered[mid])&&(occCArr[occCArr.length-1]>singletonThres)&&(!seencid.contains(mid))){
										clusterWriter.get(c2fc.get(mid)).println(arr[1]+"\t"+occCArr[occCArr.length-1]);
										//clusterWriter.get(c2fc.get(mid)).format("%s\t%d\t%d\t%.2f%%\n",arr[1],occCArr[occCArr.length-1],(int)(occCArr[occCArr.length-1]*1000000.0/(rc[rc.length-1]*1.0)),occCArr[occCArr.length-1]/(rc[rc.length-1]*1.0));
										seencid.add(mid);
									}
								}	
							}
						}
						
						firstRead=false;
					}
					
				    br.close();
				    gzip.close();
					i++;
					dataFolder=outputPath+"/data/"+i;
				}
				
				if (outputClusters)
				{
					for (i=0;i<clusterWriter.size();i++)
						clusterWriter.get(i).close();
					
					// sort the aptamers according their frequencies
					ExternalSort es = new ExternalSort(rc[rc.length-1]);
					for (i=0;i<clusterWriter.size();i++)
						es.sort(outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(i+1)+"_aptamers.txt", outputPath+"/results/"+resultFolder+"/"+outputPrefix+"_"+klength+"_clus"+(i+1)+"_aptamers.txt",outputPath+"/results/"+resultFolder+"/", 10000);
				}
			}
			System.out.println();
		}
		catch (Exception e){
			System.out.println();
			System.out.println(line);
			e.printStackTrace();
			System.exit(1);
		}
	}
}
