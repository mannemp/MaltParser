package org.maltparser.parser.hcsearch;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashingStrategy;

import java.util.*;
import java.io.*;

public class PFeatLib{
//	FeatureDatum featDatum;
    public gnu.trove.TIntObjectHashMap id2feat; //HashMap<Integer,PFeat>;
    public gnu.trove.TObjectIntHashMap str2id; 
	public boolean growthStopped;

    public double upd;
    public int featCount;
    public double sqweight;
    
    public PFeatLib(){
		init();
    }

    public void init(){
		//feat2id = new Hashtable<String, Integer>(Statics.FEAT_HASH_INIT);
    	str2id = new gnu.trove.TObjectIntHashMap();
		id2feat = new gnu.trove.TIntObjectHashMap();
		featCount = 0;
		sqweight = 0;
    }
    
    public synchronized void loadFeatTable(String FeatFile){
	try {
		continueGrowth();
		BufferedReader in = new BufferedReader ( new InputStreamReader(new FileInputStream(FeatFile),"UTF-8"));             
	    System.err.println("Open Feature Table : "+FeatFile);
	    
	    String line = in.readLine();
            while ( line != null) {
		String[] arr = line.split(" ");		
		int id = regFeat(arr[0]);
		setHWeight(id, Float.parseFloat(arr[1]));
		setCWeight(id, Float.parseFloat(arr[2]));

		line = in.readLine();
            } // end of each line
            
	    in.close();
        } catch (FileNotFoundException e){
            System.err.println(e.toString());
        } catch (IOException e) {
            System.err.println(e.toString());
        }

		id2feat.trimToSize();
		stopGrowth();
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
    
    /**
       set the weights from the training result
    */
    private synchronized void setHWeight(int id, float weight){
	PFeat feat = (PFeat) id2feat.get(id);
	feat.heuristic.weight = weight;
    }
    
    private synchronized void setCWeight(int id, float weight){
    	PFeat feat = (PFeat) id2feat.get(id);
    	feat.cost.weight = weight;
    }

    public synchronized void regFeat(Vector<String> feats){
	for (int i=0; i<feats.size(); i++){
	    regFeat(feats.get(i));
	}
    }

    /*public int regFeat(String feat){
	Integer id = (Integer) feat2id.get(feat);
	if (id != null) {
	    int idx = id.intValue();
	    id2feat.get(idx).freq += 1;
	    return idx;
	}
	else if(parser.ISFIRST_ITER)
	{
	id = new Integer(id2feat.size());
	feat2id.put(feat, id);
	PFeat newfeat = new PFeat(feat);
	newfeat.freq = 1;
	id2feat.add(newfeat);	
	return id.intValue();
    }
	return -1;
    }*/
    
    public synchronized int regFeat(String feat){
    	int idx = str2id.get(feat);
    	if (idx != -1) {
    	    ((PFeat)id2feat.get(idx)).freq += 1;
    	    return idx;
    	}
    	else if(!growthStopped())
    	{
	    	idx = featCount ++;//= id2feat.size();
	    	str2id.put(feat, idx);
	    	PFeat newfeat = new PFeat(feat);
	    	newfeat.freq = 1;
	    	newfeat.value = 1;
	    	id2feat.put(idx,newfeat);
	    	return idx;
        }
    	return -1;
    }
    
    private int getFeatID(String feat){

	Integer id = (Integer) str2id.get(feat);
	if (id != null) {
	    return id.intValue();
	} 

	return -1;
    }

    private double getHWeight(String feat){	
		int feaid = getFeatID(feat);
		if (feaid == -1) 
		    return 0;
		return ((PFeat)id2feat.get(feaid)).heuristic.weight;
    }
    
    private double getCWeight(String feat){	
		int feaid = getFeatID(feat);
		if (feaid == -1) 
		    return 0;
		return ((PFeat)id2feat.get(feaid)).cost.weight;
    }

    public double getHWeight(int feat){	
    	if (feat == -1) 
    	    return 0;
    	return ((PFeat)id2feat.get(feat)).heuristic.weight;
	}
    
    public double getCWeight(int feat){	
    	if (feat == -1) 
    	    return 0;
    	return ((PFeat)id2feat.get(feat)).cost.weight;
	}
    
    public double  getHScore(Vector<Integer> feats){
    	double  score = 0;
	for (int i=0; i<feats.size(); i++){
	    int feaid = feats.get(i).intValue();
	    try{
//	    if (feaid > 0) 
//		score += ((PFeat)id2feat.get(feaid)).weight;
	    score += ((PFeat)id2feat.get(feaid)).heuristic.getScore();
	    }
	    catch(Exception e)
	    {System.err.println("exception in getScore "+e);}
	}
	return score;
    }
    
    public double  getCScore(Vector<Integer> feats){
    	double  score = 0;
	for (int i=0; i<feats.size(); i++){
	    int feaid = feats.get(i).intValue();
	    try{
//	    if (feaid > 0) 
//		score += ((PFeat)id2feat.get(feaid)).weight;
	    score += ((PFeat)id2feat.get(feaid)).cost.getScore();
	    }
	    catch(Exception e)
	    {System.err.println("exception in getScore "+e);}
	}
	return score;
    }

    public double  getVotedHScore(Vector<String> feats, int allround){
    	double  score = 0;
	for (int i=0; i<feats.size(); i++){
	    int feaid = getFeatID(feats.get(i));
	    if (feaid != -1){
		PFeat feat = (PFeat)id2feat.get(feaid);
		score += feat.updateHCmlwt(allround);
	    }
	}
	return score;
    }
    
    public double  getVotedCScore(Vector<String> feats, int allround){
    	double  score = 0;
	for (int i=0; i<feats.size(); i++){
	    int feaid = getFeatID(feats.get(i));
	    if (feaid != -1){
		PFeat feat = (PFeat)id2feat.get(feaid);
		score += feat.updateCCmlwt(allround);
	    }
	}
	return score;
    }
    
    public HCSearchState findMaxHState(ArrayList<HCSearchState> states)
	{
		HCSearchState maxpt = states.get(0);
		double maxscore = Double.MIN_VALUE;
		for(HCSearchState curpt : states)
		{
			double curscore = getHScore(curpt.getFeatureVector());
			if(curscore > maxscore)
			{
				maxscore = curscore;
				maxpt = curpt;
			}
		}
		return maxpt;
	}
    
    public HCSearchState findMaxCState(ArrayList<HCSearchState> states)
	{
		HCSearchState maxpt = states.get(0);
		double maxscore = Double.MIN_VALUE;
		for(HCSearchState curpt : states)
		{
			double curscore = getCScore(curpt.getFeatureVector());
			if(curscore > maxscore)
			{
				maxscore = curscore;
				maxpt = curpt;
			}
		}
		return maxpt;
	}

    public synchronized void updateCFeat(Hashtable<String, Integer> featval, double  para, int allround){
	Enumeration<String> featenu = featval.keys();
	while (featenu.hasMoreElements()){
	    String onefeat = featenu.nextElement();
	    int val = featval.get(onefeat).intValue();
	    if (val != 0){

		int feaid = getFeatID(onefeat);
		if (feaid == -1){
		    feaid = regFeat(onefeat);
		}  
		PFeat feat = (PFeat)id2feat.get(feaid);
		feat.updateCCmlwt(allround);
		feat.cost.weight += val*para;	

	    }
	}
    }

    public synchronized void updateHFeat(Hashtable<String, Integer> featval, double  para, int allround){
	Enumeration<String> featenu = featval.keys();
	while (featenu.hasMoreElements()){
	    String onefeat = featenu.nextElement();
	    int val = featval.get(onefeat).intValue();
	    if (val != 0){

		int feaid = getFeatID(onefeat);
		if (feaid == -1){
		    feaid = regFeat(onefeat);
		}  
		PFeat feat = (PFeat)id2feat.get(feaid);
		feat.updateHCmlwt(allround);
		feat.heuristic.weight += val*para;	

	    }
	}
    }

    public synchronized void updateHFeat(Vector<Integer>feats, double  para, int allround){

	// Debug
	if (para == 0.0 ){
		Stats.println("*** ZERO UPDATING***");
	}
	for (int i=0; i<feats.size(); i++){
	    int feaid = feats.get(i).intValue();
	    if (feaid == -1 ){
	    	continue;
	    }
	    PFeat feat = (PFeat)id2feat.get(feaid);
	    feat.updateHCmlwt(allround);
	    sqweight -= Math.pow(feat.heuristic.weight,2);
	    feat.heuristic.noOfUpdates ++;
	    feat.heuristic.weight += para;
	    sqweight += Math.pow(feat.heuristic.weight,2);
	}
    }
    
    public synchronized void updateCFeat(Vector<Integer>feats, double  para, int allround){

	// Debug
	if (para == 0.0 ){
		Stats.println("*** ZERO UPDATING***");
	}
	for (int i=0; i<feats.size(); i++){
	    int feaid = feats.get(i).intValue();
	    if (feaid == -1 ){
	    	continue;
	    }
	    PFeat feat = (PFeat)id2feat.get(feaid);
	    feat.updateCCmlwt(allround);
	    sqweight -= Math.pow(feat.cost.weight,2);
	    feat.cost.noOfUpdates ++;
	    feat.cost.weight += para;
	    sqweight += Math.pow(feat.cost.weight,2);
	}
    }

    /*
     * The function which adds the string feature to the integer feature vector
     * */
    public void add(String feature , Vector<Integer> allfeat)
    {
    	//int idx = parser.wtact.regFeat(feature);
    	int idx = regFeat(feature);
    	if(idx != -1)
    		allfeat.add(new Integer(idx));
    	return;
    }

    public void listHWeight(){
		System.err.println("list weights :");
	
		for (int i=0; i<id2feat.size(); i++){
		    System.err.println(""+i+" "+((PFeat)id2feat.get(i)).featstr+" "+((PFeat)id2feat.get(i)).heuristic.weight);
		}
	// 	for (int i=0; i<id2feat.size(); i++){
	// 	    System.out.println("v"+i+" "+id2feat.get(i).featstr+" "
	// 			       +id2feat.get(i).updateCmlwt(PTrain.allround));
	// 	}
    }

    public void listCWeight(){
    	System.err.println("list weights :");
    	
    	for (int i=0; i<id2feat.size(); i++){
    		System.err.println(""+i+" "+((PFeat)id2feat.get(i)).featstr+" "+((PFeat)id2feat.get(i)).cost.weight);
    	}
    }
    
    public void saveWeight(String filename, int round) throws IOException{
	
	try{
// 	    PrintWriter out = new PrintWriter (new FileOutputStream(filename+".wt"));
// 	    for (int i=0; i<id2feat.size(); i++){
// 		out.println(""+i+" "+id2feat.get(i).featstr+" "+id2feat.get(i).weight);
// 	    }
// 	    out.close();
		Stats.println("No. of keys: "+id2feat.keys().length);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter
           		(new FileOutputStream(new File(filename)),"UTF-8"));
		int[] keys = id2feat.keys();
        for (int i=0; i<keys.length; i++){
        int k = keys[i];
		out.write(""+i+" "+((PFeat)id2feat.get(k)).featstr+" "+
		 ((PFeat)id2feat.get(k)).returnHCmlwt(round)+" " +((PFeat)id2feat.get(k)).returnCCmlwt(round)+"\n");
		//((PFeat)id2feat.get(k)).weight+"\n");
		 }
	    out.close();
	} catch (FileNotFoundException e){
            System.err.println(e.toString());
        }
    }

    public synchronized void updateCCmlwt(int round){
	for (int i=0; i<id2feat.size(); i++){
	    ((PFeat)id2feat.get(i)).updateCCmlwt(round);
	}
    }

    public synchronized void updateHCmlwt(int round){
	for (int i=0; i<id2feat.size(); i++){
	    ((PFeat)id2feat.get(i)).updateHCmlwt(round);
	}
    }


    public synchronized void useVotedCFeat(int round){
	for (int i=0; i<id2feat.size(); i++){
	    PFeat fea = (PFeat)id2feat.get(i);
	    fea.cost.weight = fea.cost.cmlwt;
	}
    }
    
    public synchronized void useVotedHFeat(int round){
    	for (int i=0; i<id2feat.size(); i++){
    	    PFeat fea = (PFeat)id2feat.get(i);
    	    fea.heuristic.weight = fea.heuristic.cmlwt;
    	}
    }
    
    public synchronized void averageHParams(double avVal) {
    	for(int j = 0; j < id2feat.size(); j++)
    	{
    		PFeat fea = (PFeat)id2feat.get(j);
    	    fea.heuristic.cmlwt *= 1.0/((double)avVal);		
    	    fea.heuristic.weight = fea.heuristic.cmlwt;
    	}
	}
    
    public synchronized void averageCParams(double avVal) {
    	for(int j = 0; j < id2feat.size(); j++)
    	{
    		PFeat fea = (PFeat)id2feat.get(j);
    	    fea.cost.cmlwt *= 1.0/((double)avVal);		
    	    fea.cost.weight = fea.cost.cmlwt;
    	}
	}
    
    public static synchronized void removeFeatures(PFeatLib wt, int threshold)
    {
    	int removedCount = 0;
    	int keys[] = wt.id2feat.keys();
        for (int i=0; i<keys.length; i++){
        	int k = keys[i];
        	if(((PFeat)wt.id2feat.get(k)).freq < threshold)
        	{
        		//deletes the feature in id2feat and feat2id
//        		PFeat ft = wt.id2feat.remove(i);
        		int f = wt.str2id.remove(((PFeat)wt.id2feat.remove(k)).featstr);
        		removedCount ++;
//        		if(f != i)
//        			System.err.println("ERR(in PFeatLib.removeFeatures()): feat2id and id2feat doesn't match: f != i, "+f+"!="+k);
        	}
	    }
        System.err.println("Removed "+removedCount+" features by applying threshold of "+threshold);
    }
    
    public Hashtable<Integer,Integer> collectNoOfCUpdates()
    {
  	
  	  Hashtable<Integer,Integer> noOfupdates = new Hashtable<Integer,Integer>();
  	  for ( int idx : id2feat.keys())
  	  {
  		  PFeat pf = (PFeat)id2feat.get(idx);
  		  int curnoofupdates = pf.cost.noOfUpdates;
  		  int nooffeatures = noOfupdates.containsKey(curnoofupdates) ? noOfupdates.get(curnoofupdates) : 0;
  		  noOfupdates.put(curnoofupdates, nooffeatures+1);
  	  }
  	  return noOfupdates;
    }
    
    public Hashtable<Integer,Integer> collectNoOfHUpdates()
    {
  	
  	  Hashtable<Integer,Integer> noOfupdates = new Hashtable<Integer,Integer>();
  	  for ( int idx : id2feat.keys())
  	  {
  		  PFeat pf = (PFeat)id2feat.get(idx);
  		  int curnoofupdates = pf.heuristic.noOfUpdates;
  		  int nooffeatures = noOfupdates.containsKey(curnoofupdates) ? noOfupdates.get(curnoofupdates) : 0;
  		  noOfupdates.put(curnoofupdates, nooffeatures+1);
  	  }
  	  return noOfupdates;
    }
}



