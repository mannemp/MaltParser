package org.maltparser.parser.pruneandscore;

public class FeatureDatum {
	public gnu.trove.TObjectIntHashMap str2id; // HashMap<String,Integer>
	public boolean growthStopped;
	 
	public FeatureDatum()
	{
		str2id = new gnu.trove.TObjectIntHashMap();
		growthStopped = true;
	}
	
	public boolean stopGrowth()
    {
    	growthStopped = true;
    	return growthStopped;
    }
    
    public boolean continueGrowth()
    {
    	growthStopped = false;
    	return growthStopped;
    }
    
    public boolean growthStopped()
    {
    	return growthStopped;
    }
    
    
	
}
