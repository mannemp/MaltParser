package org.maltparser.ml.lib;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashingStrategy;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.MultipleFeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.history.action.SingleDecision;

public class PrunerSinglePerceptronModel extends MaltPerceptronModel implements Serializable  {
	
	private static final long serialVersionUID = 7526471155622776147L;
	private static final Charset FILE_CHARSET = Charset.forName("ISO-8859-1");

    
//    public Lib owner;
    protected gnu.trove.TIntObjectHashMap id2feat; //HashMap<Integer,PFeat>;
    protected boolean growthStopped;
    public double upd;
    public int featCount;
    protected int allround;

    protected int[] actionCodes;
    protected int noOfActions;
    protected int inTopK;
    protected FeatureMap featureMap;
    
    protected int currentSentNo;
    
    /*public MaltPerceptronModel(Lib l){
    	this(l,1);
    }
    
    public MaltPerceptronModel(Lib l, int topk){
    	owner = l;
		init(topk);
    }*/
    
    public PrunerSinglePerceptronModel(FeatureMap map, int topk){
    	featureMap = map;
		init(topk);
    }

    public void init(int topk){
		//feat2id = new Hashtable<String, Integer>(Statics.FEAT_HASH_INIT);
		id2feat = new gnu.trove.TIntObjectHashMap();
		featCount = 0;
		upd = 0;
		growthStopped = false;
		noOfActions = 0;
		setK(topk);
		allround = 0;
		actionCodes = new int[0];
		currentSentNo = -1;
//		setActionCodes(new HashSet<Integer>());
		//TODO: set action codes
    }
    
    public int getCurrentSentNo() throws MaltChainedException
    {
    	return currentSentNo;
//    	return owner.getCurrentSentNo();
    }
    
    public void setCurrentSentNo(int sno) throws MaltChainedException
    {
    	currentSentNo = sno;
    	return;
    }
    
    public int getK()
    {
    	return inTopK;
    }
    public void setK(int k)
    {
    	inTopK = k;
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
	    	score += ((PFeat)id2feat.get(feaid)).wt.getWeight();
		}
		return score;
    }
    
    public double  getCmltScore(FeatureList feats){
    	double  score = 0;
		for (int i=0; i<feats.size(); i++){
		    int feaid = feats.get(i).index;
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
    
    public void setActionCodes(Set<Integer> codes)
    {
    	int k = 0;
    	actionCodes = new int[codes.size()];
    	for(Integer i:codes)
    		actionCodes[k++] = i.intValue();
    	noOfActions = codes.size();
    }
    
    public int[] getActionCodes()
    {
    	return actionCodes;
    }
    
    protected int binarizeFVWithAction(MaltFeatureNode[] unbinarizedMFNs, int action, FeatureList featureList, boolean gold) throws MaltChainedException {
		int y = -1; 
		featureList.clear();
		try {	
			if (unbinarizedMFNs.length == 0) {
				return -1;
			}
			y = action;
			for(int j = 1; j < unbinarizedMFNs.length; j++) {
				MaltFeatureNode mfn = unbinarizedMFNs[j];
				int v = gold ? featureMap.addIndex(j, mfn.index,y) : featureMap.getIndex(j, mfn.index, y);
				if (v != -1) {
					featureList.add(v,1);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new LibException("Couln't read from the instance file. ", e);
		}
		return y;
	}
	
    
    public void train(SingleDecision decision, MaltFeatureNode[] x) throws MaltChainedException {
//    	String decisionSettings = owner.getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
//		SymbolTable actionTable = owner.getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
		
//    	MaltFeatureNode[][] binarizedActionMFNs = new MaltFeatureNode[actionTable.getCodes().size()][x.length];
    	int[] predictionList = predict(x);
    	int truecode = decision.getDecisionCode();
    	FeatureList goldFeats = new FeatureList();
		
    	boolean found = false;
    	for(int i = 0; i < predictionList.length && i < inTopK; i++)
    	{
    		if(predictionList[i] == truecode)
    		{
    			found = true;
    			break;
    		}
    	}
    	if(found)
    		return;
//		if(predictionList[0] != truecode)
		{
			binarizeFVWithAction(x, truecode, goldFeats, true);
			allround ++;
		}
		
    	for(int i = 0 ; i < inTopK && i < predictionList.length; i++)
    	{
    		/*if(predictionList[i] == truecode)
    			break;*/
    		// Update weights
    		updateFeats(goldFeats, +1.0, allround);
    		FeatureList feats = new FeatureList();
    		binarizeFVWithAction(x, predictionList[i], feats, false);
    		updateFeats(feats, -1.0, allround);
    		double gscore = getScore(goldFeats);
    		double pscore = getScore(feats);
    		pscore+=0;
    	}
    	
    	return;
	}

    @Override
	public int[] predict(MaltFeatureNode[] x) {
		// DONE: Auto-generated method stub
		try {
			/*String decisionSettings = owner.getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
			SymbolTable actionTable = owner.getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
			noOfActions = actionTable.size();*/
			double[] actionScores = new double[noOfActions];
			int[] predictionList = new int[noOfActions];
//			MaltFeatureNode[][] binarizedActionMFNs = new MaltFeatureNode[actionTable.getCodes().size()][x.length];
		
			int k = 0; 
//			for(int code: actionTable.getCodes())
			for(int code:actionCodes)
			{
				FeatureList feats = new FeatureList();
				binarizeFVWithAction(x, code, feats, false);
				actionScores[k] = getScore(feats);
				predictionList[k++] = code;
			}

			double tmpDec;
			int tmpObj;
			int lagest;
			
			for (int i=0; i < noOfActions-1; i++) {
				lagest = i;
				for (int j=i; j < noOfActions; j++) {
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

		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public double[][] scorePredict(MaltFeatureNode[] x) {
		// DONE: Auto-generated method stub
    	double[] actionScores = new double[noOfActions];
    	final double[][] scoredPredictions = new double[2][inTopK];
    	int[] predictionList = new int[noOfActions];
		try {
			/*String decisionSettings = owner.getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
			SymbolTable actionTable = owner.getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);*/
//			MaltFeatureNode[][] binarizedActionMFNs = new MaltFeatureNode[actionTable.getCodes().size()][x.length];
		
			int k = 0;
			for(int code: actionCodes)
			{
				FeatureList feats = new FeatureList();
				binarizeFVWithAction(x, code, feats, false);
//				actionScores[k] = getScore(feats);
				actionScores[k] = getCmltScore(feats);
				predictionList[k++] = code;
			}

			double tmpDec;
			int tmpObj;
			int lagest;
			
			for (int i=0; i < noOfActions-1; i++) {
				lagest = i;
				for (int j=i; j < noOfActions; j++) {
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
			for(int i = 0; i < inTopK; i++)
			{
				scoredPredictions[0][i] = predictionList[i];
				scoredPredictions[1][i] = actionScores[i];
			}
//			scoredPredictions[1] = dec_values;
//			scoredPredictions[1] = actionScores;

		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		return scoredPredictions;
	}

	@Override
	public void train(SingleDecision decision, MaltFeatureNode[] x, int[] pruned)
			throws MaltChainedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int train(HashMap<Integer, Integer> actionCosts, MaltFeatureNode[] x, int curIter)
			throws MaltChainedException {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int train(HashMap<Integer, Integer> actionCosts,
			MaltFeatureNode[] x, int[] pruned, int curIter) throws MaltChainedException {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int[] predict(MaltFeatureNode[] x, boolean cmltWts) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] scorePredict(MaltFeatureNode[] x, boolean cmltWts) {
		// TODO Auto-generated method stub
		return null;
	}

}



