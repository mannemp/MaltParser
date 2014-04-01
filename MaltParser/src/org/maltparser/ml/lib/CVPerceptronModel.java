package org.maltparser.ml.lib;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashingStrategy;

import java.util.*;
import java.io.*;
import java.lang.management.ManagementPermission;
import java.nio.charset.Charset;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.MultipleFeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.history.action.SingleDecision;

public class CVPerceptronModel  implements Serializable {
	
	private static final long serialVersionUID = 7526471155622776147L;
	private static final Charset FILE_CHARSET = Charset.forName("ISO-8859-1");

    
//    public Lib owner;
    protected gnu.trove.TIntObjectHashMap id2feat; //HashMap<Integer,PFeat>;
    protected boolean growthStopped;
    public double upd;
    public int featCount;
    protected int allround;
    protected Random random; 
    
   /* protected int[] actionCodes;
    protected int noOfActions;
    protected int inTopK;
    protected FeatureMap featureMap;*/
    
    protected ManageCVPerceptron manager;
    
    /*public MaltPerceptronModel(Lib l){
    	this(l,1);
    }
    
    public MaltPerceptronModel(Lib l, int topk){
    	owner = l;
		init(topk);
    }*/
    
    public CVPerceptronModel(ManageCVPerceptron m){
    	manager = m;
    	init();
    }

    public void init(){
		//feat2id = new Hashtable<String, Integer>(Statics.FEAT_HASH_INIT);
		id2feat = new gnu.trove.TIntObjectHashMap();
		featCount = 0;
		upd = 0;
		growthStopped = false;
		allround = 0;
		random = new Random();
    }
    
    public int getK()
    {
    	return manager.getK();
    }
    
    public int getNoOfUpdates()
    {
    	return allround;
    }
    
    public int getNoOfActions()
    {
    	return manager.getNoOfActions();
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
    
    public synchronized void regFeat(int[] feats){
		for (int i=0; i<feats.length; i++){
		    regFeat(feats[i]);
		}
    }

    public synchronized PFeat regFeat(int idx){
    	if (id2feat.get(idx) !=null ) {
    	    ((PFeat)id2feat.get(idx)).freq += 1;
    	    return ((PFeat)id2feat.get(idx));
    	}
    	else if(!growthStopped())
    	{
	    	PFeat newfeat = new PFeat(idx);
	    	newfeat.freq = 1;
	    	newfeat.value = 1;
	    	id2feat.put(idx,newfeat);
	    	return newfeat;
        }
    	return null;
    }
    
    public double getWeight(int feat){	
    	if (feat <= 0) 
    	    return 0;
    	return ((PFeat)id2feat.get(feat)).getWeight();
	}
    
    public double  getScore(Vector<Integer> feats){
    	double  score = 0;
	for (int i=0; i<feats.size(); i++){
	    int feaid = feats.get(i).intValue();
    	score += ((PFeat)id2feat.get(feaid)).wt.getWeight();
	}
	return score;
    }

    public double  getScore(FeatureList feats){
    	double  score = 0;
		for (int i=0; i<feats.size(); i++){
		    int feaid = feats.get(i).index;
		    if(id2feat.containsKey(feaid))
		    	score += ((PFeat)id2feat.get(feaid)).wt.getWeight();
		}
		return score;
    }
    
    public double  getCmltScore(FeatureList feats){
    	double  score = 0;
		for (int i=0; i<feats.size(); i++){
		    int feaid = feats.get(i).index;
		    if(id2feat.containsKey(feaid))
		    	score += ((PFeat)id2feat.get(feaid)).wt.returnCmlwt(allround);//getWeight();
		}
		return score;
    }
    
    public double  getVotedScore(Vector<Integer> feats, int allround){
    	double  score = 0;
		for (int i=0; i<feats.size(); i++){
		    int feaid = feats.get(i).intValue();
		    if (feaid != -1){
			PFeat feat = (PFeat)id2feat.get(feaid);
			score += feat.returnCmlwt(allround);
		    }
		}
		return score;
    }
    
    public synchronized void updateFeats(FeatureList feats, double  para, int allround){
    	
    	for(int i = 0 ; i < feats.size(); i++)
    	{
    		int feaid = feats.get(i).index;
//			PFeat feat = (PFeat)id2feat.get(feaid);
    		PFeat feat = regFeat(feaid);
			feat.updateWt(para,allround);
	    }
    	return;
    }
    
    public synchronized void updateFeats(Vector<Integer> feats, double  para, int allround){
	
    	for(int feaid :feats)
    	{
		PFeat feat = (PFeat)id2feat.get(feaid);
		feat.updateWt(para,allround);
	    }
    	return;
    }

    public synchronized void useVotedFeat(int round){
	for (int i=0; i<id2feat.size(); i++){
	    PFeat fea = (PFeat)id2feat.get(i);
	    fea.wt.weight = fea.wt.cmlwt;
	}
    }

    public synchronized void averageParams(double avVal) {
    	for(int j = 0; j < id2feat.size(); j++)
    	{
    		PFeat fea = (PFeat)id2feat.get(j);
    	    fea.averageParam(1.0/((double)avVal));		
    	}
	}
    
    public int[] getActionCodes()
    {
    	return manager.getActionCodes();
    }
    
    protected int binarizeFVWithAction(MaltFeatureNode[] unbinarizedMFNs, int action, FeatureList featureList, boolean gold) throws MaltChainedException {
    	return manager.binarizeFVWithAction(unbinarizedMFNs, action, featureList, gold);
    }
    
    public int train(HashMap<Integer,Integer> actionCosts, MaltFeatureNode[] x, HashMap<Integer,FeatureList> actionFeats, int curIter) throws MaltChainedException {
    	
    	int[] predictionList = predict(x,actionFeats,false);
//    	FeatureList goldFeats = new FeatureList();
		int bestAllowedCode = predictionList[0];
		double bestAllowedCodeScore = Double.NEGATIVE_INFINITY; 
		int bestAllowedCodeCost = Integer.MAX_VALUE;
		for(Integer code:actionCosts.keySet())
		{
			if(actionCosts.get(code.intValue()).intValue() < bestAllowedCodeCost)
			{
				bestAllowedCode = code.intValue();
				bestAllowedCodeCost = actionCosts.get(code.intValue()).intValue();
				bestAllowedCodeScore = getScore(actionFeats.get(code.intValue()));
			}
			else if(actionCosts.get(code.intValue()).intValue() == bestAllowedCodeCost)
			{
				double curScore = getScore(actionFeats.get(code.intValue()));
				if(bestAllowedCodeScore < curScore)
				{
					bestAllowedCode = code.intValue();
					bestAllowedCodeScore = curScore;
				}
			}
		}
    	boolean found = false;
    	for(int i = 0; i < predictionList.length && i < getK(); i++)
    	{
    		if(actionCosts.get(predictionList[i]).intValue() == bestAllowedCodeCost) // which is 0
    		{
    			found = true;
    			break;
    		}
    	}
    	if(found)
    		return bestAllowedCode;
    	
    	int bestPredictedCode = predictionList[0];
		double bestPredictedCodeScore = getScore(actionFeats.get(predictionList[0])); 
		int bestPredictedCodeCost = actionCosts.get(predictionList[0]).intValue();
		for(int i = 1 ; i < getK() && i < predictionList.length; i++)
		{
			int pcode = predictionList[i];
			if(actionCosts.get(pcode) < bestPredictedCodeCost)
			{
				bestPredictedCode = pcode;
				bestPredictedCodeCost = actionCosts.get(pcode).intValue();
				bestPredictedCodeScore = getScore(actionFeats.get(pcode));
			}
			else if(actionCosts.get(pcode).intValue() == bestPredictedCodeCost)
			{
				double curScore = getScore(actionFeats.get(pcode));
				if(bestPredictedCodeScore < curScore)
				{
					bestPredictedCode = pcode;
					bestPredictedCodeScore = curScore;
				}
			}
		}
		allround ++;
		
    	for(int i = 0 ; i < getK() && i < predictionList.length; i++)
    	{
    		// Update weights
    		updateFeats(actionFeats.get(bestAllowedCode), +1.0, allround);
    		updateFeats(actionFeats.get(predictionList[i]), -1.0, allround);
    		double gscore = getScore(actionFeats.get(bestAllowedCode));
    		double pscore = getScore(actionFeats.get(predictionList[i]));
    		pscore+=0;
    	}
//    	return explore(bestAllowedCode, bestPredictedCode, curIter);
    	return explore(bestAllowedCode, bestPredictedCode, curIter);
	}
    
    public int explore(int topOracleAction, int bestPredictedAction, int curIter)
    {
    	
    	if(curIter+1 > manager.exploreAfterMIter && random.nextDouble() < manager.exploreProb)
    		return bestPredictedAction;
    	else
    		return topOracleAction;
    }

    public void train(SingleDecision decision, MaltFeatureNode[] x, HashMap<Integer,FeatureList> actionFeats) throws MaltChainedException {
    	
    	int[] predictionList = predict(x,actionFeats,false);
    	int truecode = decision.getDecisionCode();
//    	FeatureList goldFeats = new FeatureList();
		
    	boolean found = false;
    	for(int i = 0; i < predictionList.length && i < getK(); i++)
    	{
    		if(predictionList[i] == truecode)
    		{
    			found = true;
    			break;
    		}
    	}
    	if(found)
    		return;
		allround ++;
		
    	for(int i = 0 ; i < getK() && i < predictionList.length; i++)
    	{
    		/*if(predictionList[i] == truecode)
    			break;*/
    		// Update weights
    		updateFeats(actionFeats.get(truecode), +1.0, allround);
//    		FeatureList feats = new FeatureList();
//    		binarizeFVWithAction(x, predictionList[i], feats, false);
    		updateFeats(actionFeats.get(predictionList[i]), -1.0, allround);
    		double gscore = getScore(actionFeats.get(truecode));
    		double pscore = getScore(actionFeats.get(predictionList[i]));
    		pscore+=0;
    	}
    	
    	return;
	}


    public int[] predict(MaltFeatureNode[] x, HashMap<Integer,FeatureList> actionFeats, boolean cmlWts) {
		double[] actionScores = new double[getNoOfActions()];
		int[] predictionList = new int[getNoOfActions()];
	
		int k = 0; 
		for(int code:getActionCodes())
		{
			if(cmlWts)
				actionScores[k] = getCmltScore(actionFeats.get(code));
			else	
				actionScores[k] = getScore(actionFeats.get(code));
			predictionList[k++] = code;
		}

		double tmpDec;
		int tmpObj;
		int lagest;
		
		for (int i=0; i < getNoOfActions()-1; i++) {
			lagest = i;
			for (int j=i; j < getNoOfActions(); j++) {
				if (actionScores[j] > actionScores[lagest]) {
					lagest = j;
				}
			}
			tmpDec = actionScores[lagest];
			actionScores[lagest] = actionScores[i];
			actionScores[i] = tmpDec;
			tmpObj = predictionList[lagest];
			predictionList[lagest] = predictionList[i];
			predictionList[i] = tmpObj;
		}
		return predictionList;
	}

	public double[][] scorePredict(MaltFeatureNode[] x, HashMap<Integer,FeatureList> actionFeats, boolean cmlWts) {
		// DONE: Auto-generated method stub
		int len = getK() > getActionCodes().length ? getActionCodes().length : getK();
    	double[] actionScores = new double[getNoOfActions()];
    	final double[][] scoredPredictions = new double[2][len];
    	int[] predictionList = new int[getNoOfActions()];
		int k = 0;
		for(int code: getActionCodes())
		{
			if(cmlWts)
				actionScores[k] = getCmltScore(actionFeats.get(code));
			else	
				actionScores[k] = getScore(actionFeats.get(code));
			
//			actionScores[k] = getCmltScore(actionFeats.get(code));
			predictionList[k++] = code;
		}

		double tmpDec;
		int tmpObj;
		int lagest;
		
		for (int i=0; i < getNoOfActions()-1; i++) {
			lagest = i;
			for (int j=i; j < getNoOfActions(); j++) {
				if (actionScores[j] > actionScores[lagest]) {
					lagest = j;
				}
			}
			tmpDec = actionScores[lagest];
			actionScores[lagest] = actionScores[i];
			actionScores[i] = tmpDec;
			tmpObj = predictionList[lagest];
			predictionList[lagest] = predictionList[i];
			predictionList[i] = tmpObj;
		}
//			for(int i = 0; i < predictionList.length; i++)
		for(int i = 0; i < len; i++)
		{
			scoredPredictions[0][i] = predictionList[i];
			scoredPredictions[1][i] = actionScores[i];
		}
//			scoredPredictions[1] = dec_values;
//			scoredPredictions[1] = actionScores;

		return scoredPredictions;
	}
	
}



