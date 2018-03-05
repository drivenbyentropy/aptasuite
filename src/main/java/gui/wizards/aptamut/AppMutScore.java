package gui.wizards.aptamut;

public class AppMutScore {
	// the provided score 
	private double score; 
	
	// app = 0 : the exact score is the same as the provided score
	// app = 1 : the exact score is within an error fraction of the provided score
	private int app;
	
	public AppMutScore(double score,int app){
		this.score=score;
		this.app=app;
	}
	
	public int getApp(){
		return app;
	}
	
	public double getScore(){
		return score;
	}
	
	public String toString(){
		return score+" "+app;
	}
}
