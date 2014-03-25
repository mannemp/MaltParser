package org.maltparser.parser.hcsearch;


public class PFeat{
    
    public String featstr;
    public double value;
    public int freq;   // freqenecy in the training data
    public PWeight heuristic;
    public PWeight cost;
	
    public PFeat(String str){
		featstr = str;
		freq = 0;
		value = 1;
		heuristic = new PWeight(this);
		cost = new PWeight(this);
    }

    /**
       update cmlwt according to the current allround
    */
    public double updateHCmlwt(int allround){
	return heuristic.updateCmlwt(allround);
    }
    
    public double updateHCmlwt(double allround){
    	return heuristic.updateCmlwt(allround);
	}
    
    public double returnHCmlwt(int allround){
    	return heuristic.returnCmlwt(allround);
    }
    
    public double updateCCmlwt(int allround){
    	return heuristic.updateCmlwt(allround);
    }
        
    public double updateCCmlwt(double allround){
    	return heuristic.updateCmlwt(allround);
	}
        
    public double returnCCmlwt(int allround){
    	return heuristic.returnCmlwt(allround);
    }
    
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append(heuristic.toString());
    	sb.append("\n");
    	sb.append(cost.toString());
    	sb.append("\n");
    	return sb.toString();
    }
    
    public double getHScore()
    {
   		return heuristic.getScore();
    }
    
    public double getCScore()
    {
   		return cost.getScore();
    }
}
