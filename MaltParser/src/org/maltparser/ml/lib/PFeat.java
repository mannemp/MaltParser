package org.maltparser.ml.lib;

import java.io.Serializable;


public class PFeat extends MaltFeatureNode  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7526471155622776147L;
	public static double InitialValue = 1.0;
    protected int freq;   // freqenecy in the training data
    protected PWeight wt;
	
    public PFeat(int idx){
		super(idx,InitialValue);
    	freq = 0;
		wt = new PWeight(this);
    }

    /**
       update cmlwt according to the current allround
    */
    public double returnCmlwt(int allround){
    	return wt.returnCmlwt(allround);
    }
    
    public double returnAvgwt(int allround){
    	return wt.returnAvgwt(allround);
    }
    
    public double updateWt(double para, double allround){
    	wt.updateWt(para,allround);
    	wt.updateCmlwt(allround);
    	return wt.getWeight();
	}
    
    public double updateCmlwt(double allround){
    	return wt.updateCmlwt(allround);
	}
       
    public double averageParam(double para)
    {
    	return wt.averagaParam(para);
    }
    
    public PWeight getPWeight()
    {
   		return wt;
    }
    
    public double getWeight()
    {
   		return wt.getWeight();
    }
    
    public int hashCode() {
		final int prime = 31;
		final long temp = Double.doubleToLongBits(value);
		return prime * (prime  + index) + (int) (temp ^ (temp >>> 32));
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PFeat other = (PFeat) obj;
		if (index != other.index)
			return false;
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
			return false;
		return true;
	}
	
	public int compareTo(PFeat aThat) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if (this == aThat)
			return EQUAL;

		if (this.index < aThat.index)
			return BEFORE;
		if (this.index > aThat.index)
			return AFTER;

		if (this.getWeight() < aThat.getWeight())
			return BEFORE;
		if (this.getWeight() > aThat.getWeight())
			return AFTER;

		return EQUAL;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("PFeat [index=");
		sb.append(index);
		sb.append(", ");
		sb.append(wt.toString());
		sb.append("]");
		return sb.toString();
	}
    
}
