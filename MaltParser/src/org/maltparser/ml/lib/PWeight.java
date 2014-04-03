package org.maltparser.ml.lib;

import java.io.Serializable;


public class PWeight implements Serializable{
	private static final long serialVersionUID = 7526471155622776147L;

	public static double InitialWeight = 0;
    protected double weight;
//    public int noOfUpdates;
    protected int update; // the last round of udpating
    protected double cmlwt; // cumulative weight
    protected PFeat pfeat;
	
    public PWeight(PFeat pf){
    pfeat = pf;
	weight = InitialWeight;
//	noOfUpdates = 0;
	update = 0;
	cmlwt = InitialWeight;
    }

    public PWeight(PWeight f){
    	pfeat = f.pfeat;
    	weight = f.weight;
//    	noOfUpdates = f.noOfUpdates;
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
    
    public double updateWt(double para, double allround)
    {
    	weight += para;
    	return weight;
    }
    
    public double updateCmlwt(double allround){
    	cmlwt += (allround - update) * weight;
    	update = (int)allround;
    	return cmlwt;
	}
    
    public double updateCmlwt(double para, double allround){
    	cmlwt += allround * para;
    	update = (int)allround;
    	return cmlwt;
	}
    
    public double returnCmlwt(int allround){
    	return (cmlwt/(double)allround); ///allround;
//    	return (cmlwt + (allround - update) * weight); ///allround;
    }
    
    public double returnAvgwt(int allround){
    	return weight - (cmlwt/(double)allround); 
    }
    
    public double averagaParam(double para)
    {
    	cmlwt *= para;
    	weight = cmlwt;
    	return weight;
    }
    
    public String toString()
    {
    	final StringBuilder sb = new StringBuilder();
    	sb.append("wt:");
    	sb.append(weight);
    	sb.append("/");
    	sb.append(cmlwt);
    	return sb.toString();
    }
    
    public double getWeight()
    {
   		return weight;
    }
    
    
}
