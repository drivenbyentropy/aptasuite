package gui.wizards.aptamut;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.PascalDistribution;

import java.util.concurrent.*;

public class MutantScore {
	/**
	 * Compute the probability of observing <code>k</code> successes in a Bernoulli's experiment with <code>n</code> trials 
	 * and success probability <code>p</code>
	 * @param k	number of successes
	 * @param n number of trials
	 * @param p the success probability 
	 * @return	the probability
	 */
	public static double computeBinomialProb(int k,int n,double p){
		BinomialDistribution bd=new BinomialDistribution(n,p);	
		return bd.probability(k);
	}
	
	public static double computeNegBinoProb(int k,int l,int r,double p){
		if ((l<k)||(r<k))
			return 0;
		PascalDistribution pd=new PascalDistribution(k,p);	
		return pd.cumulativeProbability(r-k)-pd.cumulativeProbability(l-k);
	}
	
	/**
	 * Compute the probability of observing a count <code>m_s_x</code> in round X, a count <code>m_s_xp1</code> in round X+1 
	 * for a mutant and the seed enrichment <code>enr</code>.
	 * @param m_s_x	observed count in sequencing portion of round X of the mutant  
	 * @param m_s_xp1	observed count in sequencing portion of round X+1 of the mutant
	 * @param enr	the seed enrichment
	 * @param p_x	the total number of species in sequencing portion of round X 
	 * @param p_xp1	the total number of species in sequencing portion of round X+1 
	 * @return	the probability
	 */
	public static double computeProb(int m_s_x,int m_s_xp1,double enr,int p_x,int p_xp1){
    	int m_p_xp1;
    	double bp_x,bp_xp1;
    	boolean done=false;
    	double sum=0.0,tmp;
    	double scale=(p_x*1.0)/(p_xp1*1.0);
    	
    	// Scalethe observed count in round X+1 of the mutant 
    	// since the total of numbers of species in sequencing portions of round X and X+1 are not equal
    	m_s_xp1=(int)Math.round((m_s_xp1*scale));
    	int m_p_x=m_s_x+1;
    	// Iterater through all possible count of the mutant in pool X 
    	do{       		
    		m_p_xp1=(int)Math.round((2.0*enr*(m_p_x-m_s_x)));
    		if ((m_p_xp1>=m_s_xp1)&&(m_p_x>=m_s_x)){
    			
    			bp_x=computeBinomialProb(m_s_x,m_p_x,0.5);
    			bp_xp1=computeBinomialProb(m_s_xp1,m_p_xp1,0.5);
    			tmp=bp_x*bp_xp1;
    			// we stop when the probability does not change much
    			if ((bp_x<=0)&&(bp_xp1<=0))
    				done=true;
    			sum+=tmp;
    		}
    		m_p_x++;
    	} while ((!done)&&(m_p_x<=p_x));
		return sum;
	}
	
	public static double computeAppProb(int m_s_x,int m_s_xp1,double enr,int p_x,int p_xp1){
    	
		int m_p_x_l,m_p_x_r;		// the range of counts in pool X
		int m_p_xp1_l,m_p_xp1_r,i;	// the range of counts in pool X+1
    	double nbp_x,nbp_xp1;
    	boolean done=false;
    	double sum=0.0,tmp;
    	double scale=(p_x*1.0)/(p_xp1*1.0);
    	double std=Math.sqrt(m_s_x/0.5);
		double inc=std/2.0;		// the increment is half of the standard deviation
    	
    	// Scale the observed count in round X+1 of the mutant 
    	// since the total of numbers of species in sequencing portions of round X and X+1 are not equal
    	m_s_xp1=(int)Math.round((m_s_xp1*scale));
    	m_p_x_l=m_s_x-(int)Math.round(4.0*std);
    	do{       		
    		m_p_x_r=m_p_x_l+(int)Math.round(inc);
    		m_p_xp1_l=(int)Math.round((2.0*enr*(m_p_x_l-m_s_x)));
    		m_p_xp1_r=(int)Math.round((2.0*enr*(m_p_x_r-m_s_x)));
    		
    		if (m_p_xp1_l<m_s_xp1)
    			m_p_xp1_l=m_s_xp1;
    		
    		//System.out.println("m_p_x_l, m_p_x_r: "+m_p_x_l+" "+m_p_x_r);
    		//System.out.println("m_p_xp1_l, m_p_xp1_r: "+m_p_xp1_l+" "+m_p_xp1_r);
    		
    		if (m_p_xp1_r>m_s_xp1){
    			//System.out.println("Get here");
    			nbp_x=computeNegBinoProb(m_s_x,m_p_x_l,m_p_x_r,0.5);
    			nbp_xp1=computeNegBinoProb(m_s_xp1,m_p_xp1_l,m_p_xp1_r,0.5);
    			sum+=nbp_x*nbp_xp1;
    		}
    		m_p_x_l=m_p_x_r;
    	} while (m_p_x_r-m_s_x<=m_s_x+4.0*Math.sqrt(m_s_x/0.5));
    	
		return sum;
	}
	
	public static AppMutScore computeAppLogScore(int m_s_x,int m_s_xp1,double enr,int p_x,int p_xp1){
		double slen;	// the strip length for computing the interval
		if (enr*m_s_x*2.0>=11.0)
			slen=2.0*enr/11.0;	
		else 
			slen=2.0*enr/5.0;
    	double score=0.0;		
		double up=Math.ceil(enr*2.0);
       	double scale=(p_x*1.0)/(p_xp1*1.0);
		// Set up a normal distribution for the seed enrichment with standard deviation is a third of the enrichment
		// and the mean is the enrichment
		NormalDistribution nd=new NormalDistribution(enr,enr/3.0); 
		
		double num=0.0;
		double den=0.0;

		

		if (Math.min(m_s_xp1*scale,m_s_x)<=1000.01){
			
			// divide the integral of into strip of length slen and compute the area

			for (double f=0;f<up;f=f+slen){ 
				num+=nd.probability(f*1.0, (f+slen)*1.0)*computeProb(m_s_x,m_s_xp1,f*1.0+slen/2.0,p_x,p_xp1);
				den+=nd.probability(f*1.0, (f+slen)*1.0)*computeProb(m_s_x,(int)Math.round(m_s_x*enr),f*1.0+slen/2.0,p_xp1,p_xp1);
			}
			
			score=Math.log((num/den));
			
			return new AppMutScore(score,0);
		}
		else{
			num=0.0;
			den=0.0;
			
			for (double f=0;f<up;f=f+slen){ 
				num+=nd.probability(f*1.0, (f+slen)*1.0)*computeAppProb(m_s_x,m_s_xp1,f*1.0+slen/2.0,p_x,p_xp1);
				den+=nd.probability(f*1.0, (f+slen)*1.0)*computeAppProb(m_s_x,(int)Math.round(m_s_x*enr),f*1.0+slen/2.0,p_xp1,p_xp1);
			}
			
			score=Math.log((num/den));
			
			return new AppMutScore(score,1);
		}
	}
	
	public static ArrayList<AppMutScore> computeLogScoresWithApproximation(int m_s_x,int m_s_xp1,int p_x,int p_xp1,Iterator<Integer> x_counts,Iterator<Integer> xp1_counts) {
		ArrayList<Future<AppMutScore>> a=new ArrayList<Future<AppMutScore>>();
		ArrayList<AppMutScore> result=new ArrayList<AppMutScore>();
		double enr=((m_s_xp1*1.0)/(p_xp1*1.0))/((m_s_x*1.0)/(p_x*1.0));
		
		class WorkerThread implements Callable<AppMutScore>{
		    int p_x,p_xp1;
		    int x_count;
		    int xp1_count;
		    double enr;
		    
		    WorkerThread(int x_count,int xp1_count,double enr,int p_x,int p_xp1) {
		        this.p_x=p_x;
		        this.p_xp1=p_xp1;
		        this.enr=enr;
		        this.x_count=x_count;
		        this.xp1_count=xp1_count;
		    }

		    public AppMutScore call() {
		    	return computeAppLogScore(x_count,xp1_count,enr,p_x,p_xp1);
		        //return computeLogScoreWithApproximation(x_count,xp1_count,enr,p_x,p_xp1);
		    }
		}
		
		// THE NUMBER OF THREADS
		int numWorkers = 4;	        
        ExecutorService pool = Executors.newFixedThreadPool(numWorkers);
     

		while (x_counts.hasNext()){
			Callable<AppMutScore> callable=new WorkerThread(x_counts.next(),xp1_counts.next(),enr,p_x,p_xp1);
			Future<AppMutScore> futureAppMutScore=pool.submit(callable);
			a.add(futureAppMutScore);
		}
		
		pool.shutdown();
		try {
			pool.awaitTermination(365,TimeUnit.DAYS);
	
		
			for (int i=0;i<a.size();i++){
				result.add(a.get(i).get());
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}

}
