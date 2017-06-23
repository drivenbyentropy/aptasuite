package lib.aptatrace;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathUtils;

public class KContextTrace implements Comparable<KContextTrace> {
	
	private String kmer;
	private int numOfRounds;
	private int numOfContexts;
	private double[][] structProbArr;						//	containing average structural probability profile of the kmer in each round of aptamers with frequencies > singletonThres
	private double[][] singletonStructProbArr;				//	containing average structural probability profile of the kmer in each round of aptamers with frequencies <= singletonThres
	private int[] singletonCount;							//	total frequencies of all the aptamers containing the kmer with frequencies <= singletonThres		
	private int[] nonSingletonCount;						//	total frequencies of all the aptamers containing the kmer with frequencies > singletonThres	
	private int[] totalCount;								//	total frequencies of all the aptamers containing the kmer
	
	private double contextShiftingScore;					//	context shifting score of aptamers with frequencies greater than singletonThres
	private double singletonContextShiftingScore;			//	context shifting score of the aptamers with small frequency (<= singletonThres)
	private int selectionContext;							//	the selection context
	private double maxShift=0;								//	maximum shifting average probablity of the selection context
	private static int minimalSingletonCount=100;
	private static int minimalCount=100;
	
	private double[] totalSingletonStructs;
	private boolean strongPresence=false;					//	true if number of aptamers containing the kmer is more than 1% in the last round
	private double proportion;								//	proportion or percentage of the aptamers containing the motif in the last round
	private double pvalue;									//	pvalue of the score of kmer
	private boolean enoughOccurrences=true;					//	whether it has enough occurrences 

	// sets the proportion of kmer in the last selection round
	public void setProportion(double p){
		this.proportion=p;
	}
	
	public double getProportion(){
		return this.proportion;
	} 
	
	// sets whether the kmer occupies more than one percent of occurrences in the last sequencing round
	public void setStrongPresence(int lastRoundCount){
		if (totalCount[totalCount.length-1]>=0.01*lastRoundCount)
			strongPresence=true;
		else 
			strongPresence=false;
		
		setProportion(totalCount[totalCount.length-1]/(lastRoundCount*1.00f));
	}
	
	public boolean hasStrongPresence(){
		return strongPresence;
	}
	
	// returns true if the kmer can be chosen as seed which needs to have 
	// more than one percent of occurrences in the last sequencing round and its score has to be greater than a given value.
	// The given value is the 90 quantile of all the context shifting scores. 
	public boolean canBeSeed(double topValue){
		if ((strongPresence) && (Math.log(contextShiftingScore)>=topValue))
			return true;
		return false;
	}
	
	public double getPValue(){
		return pvalue;
	}

	private String total;
	
	
	// Approximate p value of a z score in the right tail of a normal distribution
	public static double computePValue(double z){
		return (1.0/(z*Math.sqrt(MathUtils.TWO_PI)*Math.exp(z*z*0.5)));
	}
	
	public void computePValue(double singletonKL[]){
		DescriptiveStatistics ds=new DescriptiveStatistics((double[])singletonKL);
		double mean=ds.getMean();
		double std=ds.getStandardDeviation();
		double z=(Math.log(contextShiftingScore)-mean)/std;
		
		if (z>0.0)
			pvalue = computePValue(z);
		else
			pvalue = 1.0;
	}
	
	
	// Check whether the context trace score of the kmer is significant 
	public boolean isSignificant(double singletonKL[],double topValue,double p){
		DescriptiveStatistics ds=new DescriptiveStatistics((double[])singletonKL);
		double mean=ds.getMean();
		double std=ds.getStandardDeviation();
		double z=(Math.log(contextShiftingScore)-mean)/std;
		//double pValue1;
		
		// Compute z value of the scores based on the null distribution of scores of kmers computed from aptamers with small counts
		if (z>0.0)
			pvalue = computePValue(z);
		else 
			pvalue=1.0;
	
		if ((pvalue<=p)&&(Math.log(contextShiftingScore)>=topValue))
			return true;
		else
			return false;
	}
	
	
	// Check whether the context trace score of the kmer is significant 
		public boolean isSignificant(double singletonKL[],double p){
			DescriptiveStatistics ds=new DescriptiveStatistics((double[])singletonKL);
			double mean=ds.getMean();
			double std=ds.getStandardDeviation();
			double z=(Math.log(contextShiftingScore)-mean)/std;
			//double pValue1;
			
			// Compute z value of the scores based on the null distribution of scores of kmers computed from aptamers with small counts
			if (z>0.0)
				pvalue = computePValue(z);
			else 
				pvalue=1.0;
		
			if (pvalue<=p)
				return true;
			else
				return false;
		}
	
	
	// In case of too many significant scores only take top theta scores 
	public boolean inTopTheta(double topThetaValue){
		if (Math.log(contextShiftingScore)>=topThetaValue)
			return true;
		return false;
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
	
	public int getLen(){
		return kmer.length();
	}
	
	public String getKmer(){
		return kmer;
	}
	
	public int getSelectionContext(){
		return selectionContext;
	}
		
	// derive the selection structural context which is the one that 
	// have the highest average shifting score
	public void deriveSelectionContext(){
		int savec=-1;
		double maxSum=-1000000.0;

		double sum=0;
		double pProb[]=new double[numOfRounds];
		
		
		for (int i=0;i<numOfRounds;i++){
			sum=0;
			for (int c=0;c<numOfContexts;c++)
				sum+=structProbArr[i][c];
			pProb[i]=1-sum;
		}
		
		double tmpMaxShift;
		
		for (int c=0;c<=numOfContexts;c++){
			sum=0;
			tmpMaxShift=0.0;
			if (c<numOfContexts){
				for (int i=1;i<numOfRounds;i++)
					for (int j=0;j<i;j++){
						sum+=structProbArr[i][c]-structProbArr[j][c];
						if ((j==0)&&(tmpMaxShift<structProbArr[i][c]-structProbArr[j][c]))
							tmpMaxShift=structProbArr[i][c]-structProbArr[j][c];
					}
			}else{
				for (int i=1;i<numOfRounds;i++)
					for (int j=0;j<i;j++){
						sum+=pProb[i]-pProb[j];
						if ((j==0)&&(tmpMaxShift<pProb[i]-pProb[j]))
							tmpMaxShift=pProb[i]-pProb[j];
					}
			}
			
			if (sum>maxSum){
				maxSum=sum;
				savec=c;
				maxShift=tmpMaxShift;
			}
		}
		
		
		
		this.selectionContext=savec;
	}
	
		
	public 	KContextTrace(String kmer,int numOfRounds,int numOfContexts){
		this.numOfRounds=numOfRounds;
		this.kmer=kmer;
				
		this.total="";
		for (int i=0;i<numOfContexts+1;i++)
			this.total=this.total+"9";
		
		totalSingletonStructs=new double[numOfRounds];
		
		this.numOfContexts=numOfContexts;
		structProbArr= new double[numOfRounds][];
		singletonStructProbArr= new double[numOfRounds][];

		for (int i=0;i<numOfRounds;i++){
			structProbArr[i]=new double[numOfContexts];
			singletonStructProbArr[i]=new double[numOfContexts];

		}
		for (int i=0;i<numOfRounds;i++){
			for (int j=0;j<numOfContexts;j++){
				structProbArr[i][j]=0.0f;					// bug fixed here
				singletonStructProbArr[i][j]=0.0f;			// bug fixed here
			}
		}
		
		singletonCount=new int[numOfRounds];
		nonSingletonCount=new int[numOfRounds];
		totalCount=new int[numOfRounds];
		for (int i=0;i<numOfRounds;i++){
			singletonCount[i]=0;
			nonSingletonCount[i]=0;
			//totalStructs[i]=0;
			totalSingletonStructs[i]=0;
			totalCount[i]=0;
		}
	}
				
	
	public double getContext(int r,int c)
	{
		return structProbArr[r][c];
	}
	
	// adds up the probability of being in a structural context r of the motif profile for non-singleton occurrences 
	public void addContextProb(int r,int count,double[] avgContextProbArr){
		for (int c=0;c<5;c++)
			structProbArr[r][c]+=avgContextProbArr[c]*count;
		nonSingletonCount[r]+=count;
	}
	
	// adds up the probability of being in a structural context r of the motif profile for singleton occurrences
	public void addSingletonContextProb(int r,int count,double[] avgContextProbArr){
		/*
		if (kmer.equals("GGGAGC")){
			System.out.println(kmer+" r "+r+" "+(singletonStructProbArr[r][0])+" "+count);
		}
		*/
		
		for (int c=0;c<5;c++)
			singletonStructProbArr[r][c]+=avgContextProbArr[c]*count;
		singletonCount[r]+=count;
		
		/*
		if (kmer.equals("GGGAGC")){
			System.out.println(kmer+" r "+r+" "+(singletonStructProbArr[r][0]+singletonStructProbArr[r][1]+singletonStructProbArr[r][2]+singletonStructProbArr[r][3]+singletonStructProbArr[r][4])+" "+singletonCount[r]);
		}
		*/
		
		/*
		if (singletonStructProbArr[r][0]+singletonStructProbArr[r][1]+singletonStructProbArr[r][2]+singletonStructProbArr[r][3]+singletonStructProbArr[r][4]>singletonCount[r]){
			System.out.println(kmer+" r "+r+" "+(singletonStructProbArr[r][0]+singletonStructProbArr[r][1]+singletonStructProbArr[r][2]+singletonStructProbArr[r][3]+singletonStructProbArr[r][4])+" "+singletonCount[r]);
			System.exit(1);
		}
		*/
	}
	
	
	public void addTotalCount(int r,int count){
		totalCount[r]+=count;
	}
	
	
	public int getLastCount(){
		return totalCount[totalCount.length-1];
	}
	
	
	public boolean hasEnoughOccurrences(){
		return enoughOccurrences;
	}
	
	
	public boolean hasEnoughOccurrences(int rn){
		enoughOccurrences=true;
		for (int i=0;i<=rn;i++)
			if (totalCount[i]<minimalCount){
				enoughOccurrences=false;
				break;
			}
		return enoughOccurrences;
	}
	
	public double getProportion(int rc[],int rn){
		return (totalCount[rn]/(rc[rn]*1.00));
	}
	
	public boolean hasStrongOccurrences(int rc[],int rn){
		if (totalCount[rn]>=rc[rn]*0.01)
			return true;
		else 
			return false;
	}
	
	public void checkEnoughOccurrences(){
		enoughOccurrences=true;
		for (int i=0;i<nonSingletonCount.length;i++)
		if (totalCount[i]<minimalCount){
			enoughOccurrences=false;
			break;
		}
	}
		
	// calculates the PWM matrix for singleton and non-singleton occurrences of the motif 
	// and the context trace by dividing the total probability by the frequency for singleton and non-singleton occurrences	
	public void normalizeProfile(){
		/*
		for (int i=0;i<nonSingletonCount.length;i++)
			System.out.print(" RO "+singletonCount[i]+nonSingletonCount[i]);
		System.out.println();
		
		for (int i=0;i<nonSingletonCount.length;i++)
		for (int c=0;c<structProbArr[i].length;c++)
		if  (structProbArr[i][c]+singletonStructProbArr[i][c]>singletonCount[i]+nonSingletonCount[i]){
			System.out.println(structProbArr[i][c]+singletonStructProbArr[i][c]+" XXX "+singletonCount[i]+nonSingletonCount[i]);
			//System.exit(1);
		}
		*/
		
		//System.out.println(minimalCount);
		
		/*
		for (int i=0;i<nonSingletonCount.length;i++)
		if  (structProbArr[i][0]+structProbArr[i][1]+structProbArr[i][2]+structProbArr[i][3]+structProbArr[i][4]>nonSingletonCount[i]){
			System.out.println("k greater");
			System.exit(1);
		}
		*/
		
		/*
		System.out.println("sca "+singletonCount[0]+" "+singletonCount[1]+" "+singletonCount[2]+" "+singletonCount[3]);
		System.out.println("nsca "+nonSingletonCount[0]+" "+nonSingletonCount[1]+" "+nonSingletonCount[2]+" "+nonSingletonCount[3]);
		System.out.println("sstrucpa "+singletonStructProbArr[3][0]+" "+singletonStructProbArr[3][1]+" "+singletonStructProbArr[3][2]+" "+singletonStructProbArr[3][3]+" "+singletonStructProbArr[3][4]);
		System.out.println("nsstrucpa "+structProbArr[0][0]+" "+structProbArr[0][1]+" "+structProbArr[0][2]+" "+structProbArr[0][3]+" "+structProbArr[0][4]);
		*/
		
		/*
		for (int i=0;i<nonSingletonCount.length;i++)
			if  (singletonStructProbArr[i][0]+singletonStructProbArr[i][1]+singletonStructProbArr[i][2]+singletonStructProbArr[i][3]+singletonStructProbArr[i][4]>singletonCount[i]){
				System.out.println(i+" s greater");
				System.exit(1);
			}
		*/
		
			for (int i=0;i<nonSingletonCount.length;i++)
		    if ((nonSingletonCount[i]<minimalCount)||(i==0)){
		    	if ((singletonCount[i]+nonSingletonCount[i])>=minimalCount){
		    		nonSingletonCount[i]=singletonCount[i]+nonSingletonCount[i];
		    		
		    		for (int c=0;c<structProbArr[i].length;c++){
		    			//if (i==0)
		    				//System.out.println("before "+structProbArr[i][c]+" "+singletonStructProbArr[i][c]);
		    			structProbArr[i][c]=(float)(structProbArr[i][c]+singletonStructProbArr[i][c]);
		    			//if (i==0)
		    				//System.out.println("after "+structProbArr[i][c]+" "+singletonStructProbArr[i][c]);
		    		}
		    	}
		    }
			
			/*
			System.out.println("nsca "+nonSingletonCount[0]+" "+nonSingletonCount[1]+" "+nonSingletonCount[2]+" "+nonSingletonCount[3]);
			System.out.println("sstrucpa1 "+singletonStructProbArr[0][0]+" "+singletonStructProbArr[0][1]+" "+singletonStructProbArr[0][2]+" "+singletonStructProbArr[0][3]+" "+singletonStructProbArr[0][4]);
			System.out.println("nsstrucpa1 "+structProbArr[0][0]+" "+structProbArr[0][1]+" "+structProbArr[0][2]+" "+structProbArr[0][3]+" "+structProbArr[0][4]);
			*/
			
			for (int i=0;i<nonSingletonCount.length;i++)
			if (totalCount[i]>=minimalCount){
				for (int c=0;c<structProbArr[i].length;c++){
					structProbArr[i][c]=(float)(structProbArr[i][c]/(1.0*nonSingletonCount[i]));
					singletonStructProbArr[i][c]=(float)(singletonStructProbArr[i][c]/(1.0*singletonCount[i]));
				}
			}
			
			//System.out.println("nsstrucpa2 "+structProbArr[0][0]+" "+structProbArr[0][1]+" "+structProbArr[0][2]+" "+structProbArr[0][3]+" "+structProbArr[0][4]);
			/*
			for (int i=0;i<nonSingletonCount.length;i++){
				if (singletonStructProbArr[i][0]+singletonStructProbArr[i][1]+singletonStructProbArr[i][2]+singletonStructProbArr[i][3]+singletonStructProbArr[i][4]>1.0){
					System.out.println((singletonStructProbArr[i][0]+singletonStructProbArr[i][1]+singletonStructProbArr[i][2]+singletonStructProbArr[i][3]+singletonStructProbArr[i][4])+" s greater than 1 "+totalCount[i]);
					System.exit(1);
				}
				
				if (structProbArr[i][0]+structProbArr[i][1]+structProbArr[i][2]+structProbArr[i][3]+structProbArr[i][4]>1.0){
					System.out.println("xxx "+i+" k greater than 1 "+(structProbArr[i][0]+structProbArr[i][1]+structProbArr[i][2]+structProbArr[i][3]+structProbArr[i][4]));
					//System.exit(1);
				}
			}
			*/
	}
	
	// calculates the KL divergence of p from q
	private double calculateRelativeEntropy(double p[],double q[]){
		double sump=0.0,sumq=0.0;
		double sum=0.0;
		
		for (int i=0;i<p.length;i++){
			sump+=p[i];
			sumq+=q[i];
			
			if ((p[i]>0.0)&&(q[i]>0.0))
				sum+=p[i]*Math.log(p[i]/q[i]);
		}
		
		if (((1-sump)>0.0)&&((1-sumq)>0.0))
			sum+=(1-sump)*Math.log((1-sump)/(1-sumq));
				
		return sum;
	}
	
	/*
	private double entropy(double p[]){
		double sump=0.0;
		double sum=0.0;
		
		for (int i=0;i<p.length;i++){
			sump+=p[i];
			
			if (p[i]>0.0)
				sum+=p[i]*Math.log(p[i]);
		}
		
		if ((1-sump)>0.0)
			sum+=(1-sump)*Math.log(1-sump);

		return sum+0.01;

	}
	*/
	
	// calculates the context shifting scores for singleton occurrences used for the background
	public void calculateSingletonKLScore(int rc[]){
		singletonContextShiftingScore=0.0f;
		double times=0.0f;
		double sumj;
		double sumk;
		double pre=0;

		
		for (int k=1;k<singletonStructProbArr.length;k++)
		{
			for (int j=0;j<k;j++)
				if ((singletonStructProbArr[j][0]<=1.0)&&(singletonStructProbArr[k][0]<=1.0))
				if ((singletonCount[j]>=minimalSingletonCount)&&(singletonCount[k]>=minimalSingletonCount)){
					times+=1.0;
					
					sumj=0.0f;
					sumk=0.0f;
					for (int l=0;l<numOfContexts;l++){
						sumj+=singletonStructProbArr[j][l];
						sumk+=singletonStructProbArr[k][l];
					}
					
					/*
					if (calculateRelativeEntropy(singletonStructProbArr[k],singletonStructProbArr[j])<0.0){
						for (int l=0;l<numOfContexts;l++)
							System.out.print(" S "+singletonStructProbArr[k][l]);
						System.out.println();
						for (int l=0;l<numOfContexts;l++)
							System.out.print(" S "+singletonStructProbArr[j][l]);
						System.out.println();
						System.out.println(calculateRelativeEntropy(singletonStructProbArr[k],singletonStructProbArr[j]));
						System.exit(1);
					}
					*/
					
					singletonContextShiftingScore+=calculateRelativeEntropy(singletonStructProbArr[k],singletonStructProbArr[j]);
					
				}
		}
		
		//System.out.println("singletonContextSScore "+getSingletonKLScore());
		
		double thres=(singletonStructProbArr.length-1)*(singletonStructProbArr.length)/2.0f;
		
		if (times>thres-0.01)
			singletonContextShiftingScore/=times;
		else
			singletonContextShiftingScore=0.0f;
	}
	
	
	// calculates the context shifting scores for singleton occurrences given the number of rounds rn
	public void calculateSingletonKLScore(int rc[],int rn){
		singletonContextShiftingScore=0.0f;
		double times=0.0f;
		double sumj;
		double sumk;
		double pre=0;

		
		double upTo=0.0;
		for (int k=1;k<rn;k++)
		{
			for (int j=0;j<k;j++)
				if ((singletonStructProbArr[j][0]<=1.0)&&(singletonStructProbArr[k][0]<=1.0))
				if ((singletonCount[j]>=minimalSingletonCount)&&(singletonCount[k]>=minimalSingletonCount)){
					times+=1.0;
					
					sumj=0.0f;
					sumk=0.0f;
					for (int l=0;l<numOfContexts;l++){
						sumj+=singletonStructProbArr[j][l];
						sumk+=singletonStructProbArr[k][l];
					}
					
					singletonContextShiftingScore+=calculateRelativeEntropy(singletonStructProbArr[k],singletonStructProbArr[j]);
					
				}

			upTo=singletonContextShiftingScore;
		}
		
		double thres=(rn)*(rn-1)/2.0f;
		
		if (times>thres-0.01)
			singletonContextShiftingScore/=times;
		else
			singletonContextShiftingScore=0.0f;
	}
	
	// calculates the context shifting scores for singleton occurrences for a particular round given the round number round
	public void calculateSingletonKLScoreOneRound(int rc[],int round){
		singletonContextShiftingScore=0.0f;
		double times=0.0f;
		double sumj;
		double sumk;
		double pre=0;
		
		double upTo=0.0;
		int k=round;
		int j=0;

				if ((singletonStructProbArr[j][0]<=1.0)&&(singletonStructProbArr[k][0]<=1.0))
				if ((singletonCount[j]>=minimalSingletonCount)&&(singletonCount[k]>=minimalSingletonCount)){
					times+=1.0;
					
					sumj=0.0f;
					sumk=0.0f;
					for (int l=0;l<numOfContexts;l++){
						sumj+=singletonStructProbArr[j][l];
						sumk+=singletonStructProbArr[k][l];
					}
					
					singletonContextShiftingScore+=calculateRelativeEntropy(singletonStructProbArr[k],singletonStructProbArr[j]);
				}

			upTo=singletonContextShiftingScore;
	}
	
	// calculates the context shifting scores for non-singleton occurrences for a particular round given the round number round
	public void calculateKLScoreOneRound(int[] rc,int round){
		contextShiftingScore=0.0f;
		double times=0.0f;
		double sumj;
		double sumk;
		
		double upTo=0.0;
		int k=round;
		int j=0;
		
			if ((structProbArr[j][0]<=1.0)&&(structProbArr[k][0]<=1.0))
			if ((nonSingletonCount[j]>=minimalCount)&&(nonSingletonCount[k]>=minimalCount)){
				times+=1.0;
				sumj=0.0f;
				sumk=0.0f;
				for (int l=0;l<numOfContexts;l++){
					sumj+=structProbArr[j][l];
					sumk+=structProbArr[k][l];
				}
					
				contextShiftingScore+=calculateRelativeEntropy(structProbArr[k],structProbArr[j]);	
			}
							
			upTo=contextShiftingScore;
	}
	
	// calculates the context shifting scores for non-singleton occurrences 
	public void calculateKLScore(int[] rc){
		contextShiftingScore=0.0f;
		double times=0.0f;
		double sumj;
		double sumk;
		double pre=0;
		
		double upTo=0.0;
		for (int k=1;k<structProbArr.length;k++)
		{
			for (int j=0;j<k;j++)
				if ((structProbArr[j][0]<=1.0)&&(structProbArr[k][0]<=1.0))
				if ((nonSingletonCount[j]>=minimalCount)&&(nonSingletonCount[k]>=minimalCount)){
					times+=1.0;
					sumj=0.0f;
					sumk=0.0f;
					for (int l=0;l<numOfContexts;l++){
						sumj+=structProbArr[j][l];
						sumk+=structProbArr[k][l];
					}
					/*
					if (calculateRelativeEntropy(structProbArr[k],structProbArr[j])<0.0){
						for (int l=0;l<numOfContexts;l++)
							System.out.print(" K "+structProbArr[k][l]);
						System.out.println();
						for (int l=0;l<numOfContexts;l++)
							System.out.print(" K "+structProbArr[j][l]);
						System.out.println();
						System.out.println(calculateRelativeEntropy(structProbArr[k],structProbArr[j]));
						System.exit(1);
					}
					*/
					contextShiftingScore+=calculateRelativeEntropy(structProbArr[k],structProbArr[j]);
				}
							
			upTo=contextShiftingScore;
		}
		
		double thres=(structProbArr.length-1)*(structProbArr.length)/2.0f;
		
		if (times>thres-0.01)
			contextShiftingScore/=times;
		else
			contextShiftingScore=0.0f;
	}
	
	// calculates the context shifting scores for non-singleton occurrences given the number of rounds rn
	public void calculateKLScore(int[] rc,int rn){
		contextShiftingScore=0.0f;
		double times=0.0f;
		double sumj;
		double sumk;
		double pre=0;
		
		double upTo=0.0;
		for (int k=1;k<rn;k++)
		{
			for (int j=0;j<k;j++)
				if ((structProbArr[j][0]<=1.0)&&(structProbArr[k][0]<=1.0))
				if ((nonSingletonCount[j]>=minimalCount)&&(nonSingletonCount[k]>=minimalCount)){
					times+=1.0;
					sumj=0.0f;
					sumk=0.0f;
					for (int l=0;l<numOfContexts;l++){
						sumj+=structProbArr[j][l];
						sumk+=structProbArr[k][l];
					}
					
					contextShiftingScore+=calculateRelativeEntropy(structProbArr[k],structProbArr[j]);
				}
							
			upTo=contextShiftingScore;
		}
		
		double thres=(rn-1)*(rn)/2.0f;
		
		if (times>thres-0.01)
			contextShiftingScore/=times;
		else
			contextShiftingScore=0.0f;
	}
	
	public void printOut(){
		System.out.println(kmer+" , score: "+getKLScore()+" , proportion: "+getProportion()+" , context: "+getContext(getSelectionContext()));
		double sum=0.0f;
		for (int j=0;j<numOfContexts;j++){
			System.out.print(getContext(j));
			for (int i=0;i<structProbArr.length;i++)
				System.out.print(" "+structProbArr[i][j]);
			System.out.println();
		}
		
		System.out.print(getContext(numOfContexts));
		for (int i=0;i<structProbArr.length;i++){
			sum=0.0f;
			for (int j=0;j<numOfContexts;j++)
				sum+=structProbArr[i][j];
			System.out.print(" "+(1-sum));
		}
		System.out.println();
	}
	
	public void printOutSingleton(){
		System.out.println(kmer+" , score: "+getSingletonKLScore());
		double sum=0.0f;
		for (int j=0;j<numOfContexts;j++){
			System.out.print(getContext(j));
			for (int i=0;i<singletonStructProbArr.length;i++)
				System.out.print(" "+singletonStructProbArr[i][j]);
			System.out.println();
		}
		
		System.out.print(getContext(numOfContexts));
		for (int i=0;i<singletonStructProbArr.length;i++){
			sum=0.0f;
			for (int j=0;j<numOfContexts;j++)
				sum+=singletonStructProbArr[i][j];
			System.out.print(" "+(1-sum));
		}
		System.out.println();
	}
	
	public void printOut(PrintWriter writer){
		writer.print("PO");
		for (int j=0;j<numOfContexts+1;j++)
			writer.print(" "+getContext(j));
		writer.println();
		
		for (int i=0;i<structProbArr.length;i++){
			double sum=0.0f;
			writer.print(i);
			for (int j=0;j<numOfContexts;j++){
				sum+=structProbArr[i][j];
				writer.print(" "+structProbArr[i][j]);
			}
			writer.print(" "+(1-sum));
			writer.println();
		}
	}
		
	public double getKLScore(){
		return contextShiftingScore;
	}
	
	public double getSingletonKLScore(){
		return singletonContextShiftingScore;
	}
	
	@Override
	public int compareTo(KContextTrace mkc) {
		if (Math.abs(this.proportion-mkc.getProportion())<0.000001)
			return 0;
	    else if (this.proportion<mkc.getProportion())
			return -1;
		else 
			return 1;
	}
}