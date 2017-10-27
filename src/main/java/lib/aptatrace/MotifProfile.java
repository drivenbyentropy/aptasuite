package lib.aptatrace;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import gui.aptatrace.logo.Logo;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class MotifProfile {
	private int len;																//	len of the motif
	private double[][] pwm;														    //	position weight matrix, second dimension indexes in the order A C G T 	
	private double[][] singletonPwm;
	private String consensus;														//	consensus string of the PWM
	private int trims;																//	after trimmed start position of PWM
	private int trime;																//	after trimmed end position of PWM
	private String seed;															//	the seed kmer 
	private double pValue;
	private double seedpValue;														//	pvalue of the seed kmer
	
	private int totalOccs;															//	total aptamers containing the set of kmers of the motif in the last round
	private IntOpenHashSet occSet=new IntOpenHashSet();								//	containing the aptamers containing the set of kmers of the motif in the last round
	private ArrayList<String> kmerSet=new ArrayList<String>();						// consists of all the kmers composing of the motif
	private HashMap<String,String> kmerAlignment=new HashMap<String,String>();		// consists of all the alignments of all the kmer with seed
	private double proportion;														// percentage of all the aptamers containing the motif in the last cycle/round
	private int[] singletonCount;													// total frequencies of all the aptamers containing the kmers of the motif with frequencies <= singletonThres	
	private int[] nonSingletonCount;												// total frequencies of all the aptamers containing the kmers of the motif with frequencies > singletonThres
	private int[] totalCount;														// total frequencies of all the aptamers containing the kmers composing of the motif 
	private int numOfContexts=5;													// number of structural contexts except for paired
	
	private double[][] structProbArr;												//	containing average structural probability profile of all the kmers of motifs in each round of aptamers with frequencies > singletonThres
	private double[][] singletonStructProbArr;										//	containing average structural probability profile of all the kmers of motifs in each round of aptamers with frequencies <= singletonThres
		
	
	// calculates the proportions of the sequencing pools given the frequencies of kmers
	public void calculateProportion(int[] rc){
		proportion=totalCount[totalCount.length-1]/(rc[rc.length-1]*1.0f);
	}
	
	
	// returns the proportion of the motif profile in the last sequencing round 
	public double getProportion(){
		return proportion;
	}
	
	// sets the proportion of the last sequencing round
	public void setProportion(double proportion){
		this.proportion=proportion;
	}
	
	public void setSeedpValue(double pValue){
		this.seedpValue=pValue;
	}
	
	public double getSeedpValue(){
		return this.seedpValue;
	}
	
	public int getLastRoundCount(){
		return totalCount[totalCount.length-1];
	}
		
	private static String getContext(int c){
		if (c==0)
			return "H";
		else if (c==1)
			return "B";
		else if (c==2)
			return "I";
		else if (c==3)
			return "M";
		else if (c==4)
			return "D";
		else
			return "P";
	}
	
	public void addKmer(String kmer){
		kmerSet.add(kmer);
	}
	
	public void addKmerAlignment(String kmer,String alignment){
		kmerAlignment.put(kmer, alignment);
	}
	
	public String getKmerAlignment(String kmer){
		return kmerAlignment.get(kmer);
	}
	
	
	// adds up the frequency of a kmer to the overall frequency of the motif profile in the last round
	public void addOccId(int id,int num){
		if (!occSet.contains(id)){
			occSet.add(id);
			totalOccs+=num;
			if (totalOccs>100000000){
				System.out.println(occSet.size()+" something wrong here "+totalOccs);
				System.exit(0);
			}
		}
	}
	
	// adds up the probability of being in a structural context r of the motif profile for non-singleton occurrences 
	public void addContextProb(int r,int count,double[] avgContextProbArr){
		for (int c=0;c<5;c++)
			structProbArr[r][c]+=avgContextProbArr[c]*count;
		nonSingletonCount[r]+=count;
		//totalCount[r]+=count;
	}
	
	// adds up the probability of being in a structural context r of the motif profile for singleton occurrences 
	public void addSingletonContextProb(int r,int count,double[] avgContextProbArr){
		for (int c=0;c<5;c++)
			singletonStructProbArr[r][c]+=avgContextProbArr[c]*count;
		singletonCount[r]+=count;
	}
	
	public void addTotalCount(int r,int count){
		totalCount[r]+=count;
	}
	
	public int getTotalOccs(){
		return totalOccs;
	}
	
	public IntOpenHashSet getOccSet(){
		return occSet;
	}
	
	
	public double addTo(IntOpenHashSet another,Int2IntOpenHashMap id2Count){
		int totalno=0;
		
		for (int k : occSet) {
			if (!another.contains(k)){
				totalno+=id2Count.get(k);
				another.add(k);
			}
		}
		
		return (totalno/((totalOccs)*1.0f));
	}
	
	
	public double getOverlapPercentage(IntOpenHashSet another,Int2IntOpenHashMap id2Count){
		int totalO=0;
				
		for (int k : occSet) {
			if (another.contains(k)){
				totalO+=id2Count.get(k);
			}
		}		
		
		return (totalO/((totalOccs)*1.0f));
	}
	
	public double getOverlapPercentage(MotifProfile another,Int2IntOpenHashMap id2Count){	
		return (getOverlapPercentage(another.getOccSet(),id2Count));
	}
	
	public void setSeed(String s){
		seed=s;
	}
	
	public String getSeed(){
		return seed;
	}
	
	public void setpValue(double p){
		this.pValue=p;
	}
	
	public double getpValue(){
		return pValue;
	}
	
	// adds up the frequency of the motif profile for non-singleton occurrences 
	public void addToPWM(String s, int count){
		for (int i=0;i<s.length();i++){
			pwm[i][toIndex(s.charAt(i))]+=count*1.0;
		}
	}
	
	// adds up the frequency of the motif profile for singleton occurrences 
	public void addToSingletonPWM(String s, int count){
		for (int i=0;i<s.length();i++){
			singletonPwm[i][toIndex(s.charAt(i))]+=count*1.0;
		}
	}
	
	public void setPWM(double[][] p){
		for (int i=0;i<len;i++){
			for (int j=0;j<4;j++)
				pwm[i][j]=p[i][j];
		}
	}
	
	public MotifProfile(int len,double[][] p){
		this.len=len;
		this.pwm=new double[len][];
		this.singletonPwm=new double[len][];
		for (int i=0;i<len;i++){
			pwm[i]=new double[4];
			singletonPwm[i]=new double[4];
			for (int j=0;j<4;j++){
				pwm[i][j]=p[i][j];
				singletonPwm[i][j]=0.0f;
			}
		}
		trims=0;
		trime=len-1;
	}
	
	public MotifProfile(int len){
		this.len=len;
		this.pwm=new double[len][];
		this.singletonPwm=new double[len][];
		for (int i=0;i<len;i++){
			pwm[i]=new double[4];
			singletonPwm[i]=new double[4];
			for (int j=0;j<4;j++){
				pwm[i][j]=0.0f;
				singletonPwm[i][j]=0.0f;
			}
		}
		trims=0;
		trime=len-1;
		totalOccs=0;
	}
	
	public double[][] getPWM(){
		double[][] ret=new double[trime-trims+1][];
		for (int i=trims;i<=trime;i++){
			ret[i-trims]=new double[4];
			ret[i-trims][0]=pwm[i][0];
			ret[i-trims][1]=pwm[i][3];
			ret[i-trims][2]=pwm[i][1];
			ret[i-trims][3]=pwm[i][2];
		}
		return ret;
	}
	
	// calculates the PWM matrix for singleton and non-singleton occurrences of the motif 
	// and the context trace by dividing the total probability by the frequency for singleton and non-singleton occurrences
	public void normalizeProfile(){				
		//printPWM();
		
		for (int i=0;i<nonSingletonCount.length;i++)
	    if ((nonSingletonCount[i])<100){
	    	if ((singletonCount[i]+nonSingletonCount[i])>=100){
	    		nonSingletonCount[i]=singletonCount[i]+nonSingletonCount[i];
	    		
	    		for (int c=0;c<structProbArr[i].length;c++)
	    			structProbArr[i][c]+=(float)(structProbArr[i][c]+singletonStructProbArr[i][c]);
	    		
	    		if (i==(nonSingletonCount.length-1)){
	    			for (int j=0;j<pwm.length;j++)
	    			for (int k=0;k<4;k++)
	    				pwm[j][k]+=singletonPwm[j][k];
	    		}
	    	}
	    }
		
		//printPWM();
		
		// calculates the context trace for non-singleton counts		
		for (int i=0;i<nonSingletonCount.length;i++){
			for (int c=0;c<structProbArr[i].length;c++){
				structProbArr[i][c]=(float)(structProbArr[i][c]/(1.0*nonSingletonCount[i]));
				singletonStructProbArr[i][c]=(float)(singletonStructProbArr[i][c]/(1.0*singletonCount[i]));
			}
		}
		
		double totalSum=0.0;
		for (int i=0;i<pwm.length;i++){
			totalSum=0.0;
			for (int j=0;j<4;j++)
				totalSum+=pwm[i][j];
			for (int j=0;j<4;j++)
				pwm[i][j]/=totalSum;
				//pwm[i][j]/=nonSingletonCount[nonSingletonCount.length-1]*1.0;
			
		}
		//printPWM();
	}
	
	public int getAlignmentLen(){
		return kmerAlignment.get(0).length();
	}
	
	public MotifProfile(int len,int numRounds){
		this.len=len;
		this.pwm=new double[len][];
		this.singletonPwm=new double[len][];
		for (int i=0;i<len;i++){
			pwm[i]=new double[4];
			singletonPwm[i]=new double[4];
			for (int j=0;j<4;j++){
				pwm[i][j]=0.0f;
				singletonPwm[i][j]=0.0f;
			}
		}
		trims=0;
		trime=len-1;
		
		
		structProbArr= new double[numRounds][];
		singletonStructProbArr= new double[numRounds][];
		totalCount=new int[numRounds];
		singletonCount=new int[numRounds];
		nonSingletonCount=new int[numRounds];

		for (int i=0;i<numRounds;i++){
			structProbArr[i]=new double[numOfContexts];
			singletonStructProbArr[i]=new double[numOfContexts];
			singletonCount[i]=0;
			totalCount[i]=0;
		}
		
		for (int i=0;i<numRounds;i++){
			for (int j=0;j<numOfContexts;j++){
				structProbArr[i][j]=0.0f;
				singletonStructProbArr[i][j]=0.0f;
			}
		}
		
		totalOccs=0;
	}
	
	public MotifProfile(String s){
		this.len=s.length();
		this.pwm=new double[s.length()][];
		for (int i=0;i<len;i++){
			pwm[i]=new double[4];
			for (int j=0;j<4;j++)
				pwm[i][j]=0.0f;
			
			pwm[i][toIndex(s.charAt(i))]=1.0f;
		}
		this.consensus=s;
		trims=0;
		trime=len-1;
		totalOccs=0;
	}
	
	
	// trims uninformative positions from PWM matrix 
	public void trim(){
		boolean trimmed=true;
		
		double sum;
		
		trims=0;
		while (trimmed){
			sum=0.0f;
			for (int i=0;i<4;i++)
			if (pwm[trims][i]>0.000001)
				sum=sum-pwm[trims][i]*Math.log(pwm[trims][i])/Math.log(2.0);
			if ((sum<1.70)||(trims==len-1))
				break;
			trims++;
		}
		
		trime=len-1;
		trimmed=true;
		
		while (trimmed){
			sum=0.0f;
			for (int i=0;i<4;i++)
			if (pwm[trime][i]>0.000001)
				sum=sum-pwm[trime][i]*Math.log(pwm[trime][i])/Math.log(2.0);
			if ((sum<1.70)||(trime==trims))
				break;
			trime--;
		}
	}
	
	public String getConsensus(){
		return consensus;
	}
	
	public void buildConsensus(){
		String ret="";
		for (int i=0;i<len;i++){
			double max=pwm[i][0];
			int savej=0;
			for (int j=1;j<4;j++)
			if (pwm[i][j]>max){
				max=pwm[i][j];
				savej=j;
			}
			ret+=toBase(savej);	
		}
		this.consensus=ret;
	}
	
	public void buildConsensus(char[] toBase){
		String ret="";
		for (int i=0;i<len;i++){
			double max=pwm[i][0];
			int savej=0;
			for (int j=1;j<4;j++)
			if (pwm[i][j]>max){
				max=pwm[i][j];
				savej=j;
			}
			ret+=toBase[savej];	
		}
		this.consensus=ret;
	}
	
	private static char toBase(int i){
		if (i==0)
			return 'A';
		if (i==1)
			return 'G';
		if (i==2)
			return 'T';
		return 'C';
	}
	
	private static char toIndex(char c){
		if (c=='A')
			return 0;
		if (c=='G')
			return 1;
		if (c=='T')
			return 2;
		return 3;
	}
		
	public double[][] getProfile(){
		return pwm;
	}
	
	public int getLen(){
		return len;
	}

	public void printContextTrace(PrintWriter writer, ArrayList<String> roundArr){
		writer.print("RD");
		double sum;
	    for (int c=0;c<6;c++)
			writer.print(" "+getContext(c));
		writer.println();
		for (int r=0;r<structProbArr.length;r++){
			writer.print(roundArr.get(r));
			sum=0.0f;
			for (int c=0;c<5;c++){
				writer.print(" "+(structProbArr[r][c]));
				sum+=structProbArr[r][c];
			}
			writer.print(" "+(1.0-sum));
			writer.println();
		}
	}
	
	public double[][] getTraceMatrix(){
		double ret[][]=new double[structProbArr.length][];
		for (int r=0;r<structProbArr.length;r++){
			double sum=0.0f;
			ret[r]=new double[6];
			for (int c=0;c<5;c++){
				ret[r][c]=structProbArr[r][c];
				sum+=structProbArr[r][c];
			}
			ret[r][5]=(1.0-sum);
		}
		return ret;	
	}
	
	public Logo getTrace(String[] rid){
		double ret[][]=new double[structProbArr.length][];
		for (int r=0;r<structProbArr.length;r++){
			double sum=0.0f;
			ret[r]=new double[6];
			for (int c=0;c<5;c++){
				ret[r][c]=structProbArr[r][c];
				sum+=structProbArr[r][c];
			}
			ret[r][5]=(1.0-sum);
		}
		
		Logo trace = new Logo(ret, rid);
		trace.setAlphabetContexts();
		trace.setBit(false);
		
		return trace;	
	}
	
	// prints the motif PWM matrix to the console
	public void printPWM(){
		System.out.print("PO");
		for (int i=0;i<4;i++)
			if (toBase(i)!='T')
				System.out.print(" "+toBase(i));
			else
				System.out.print(" "+"T/U");
		System.out.println();
		
		for (int i=trims;i<=trime;i++){
			System.out.print((i-trims)+1);
			for (int j=0;j<4;j++)
				System.out.format(" %.2f", pwm[i][j]);
			System.out.println();
		}
	}
	
	// prints the motif PWM matrix to a writer stream
	public void printPWM(PrintWriter writer){
		writer.print("PO");
		for (int i=0;i<4;i++)
			if (toBase(i)!='T')
				writer.print(" "+toBase(i));
			else
				writer.print(" "+"T/U");
		writer.println();
		
		for (int i=trims;i<=trime;i++){
			writer.print(i-trims+1);
			for (int j=0;j<4;j++)
				writer.format(" %.2f", pwm[i][j]);
			writer.println();
		}
	}
}