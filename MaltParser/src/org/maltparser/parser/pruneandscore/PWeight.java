package org.maltparser.parser.pruneandscore;


public class PWeight{

    public double weight;
    public int noOfUpdates;
    public int update; // the last round of udpating
    public double cmlwt; // cumulative weight
    public PFeat pfeat;
	
    public PWeight(PFeat pf){
    pfeat = pf;
	weight = Stats.initialWeight;
	noOfUpdates = 0;
	update = 0;
	cmlwt = Stats.initialWeight;
    }

    public PWeight(PWeight f){
    	pfeat = f.pfeat;
    	weight = f.weight;
    	noOfUpdates = f.noOfUpdates;
    	update = f.update;
    	cmlwt = f.cmlwt;
    }
    
    /**
       update cmlwt according to the current allround
    */
    public double updateCmlwt(int allround){
	cmlwt += (allround - update) * weight;
	update = allround;
	return cmlwt;
    }
    
    public double updateCmlwt(double allround){
    	cmlwt += (allround) * weight;
    	update = (int)allround;
    	return cmlwt;
	}
    
    public double returnCmlwt(int allround){
    	return (cmlwt + (allround - update) * weight);
    }
    
    public String toString()
    {
    	return weight+"##"+pfeat.featstr;
    }
    
    public double getScore()
    {
   		return weight;
    }
}
