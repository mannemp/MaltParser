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

public class ManageCVPerceptron  extends MaltPerceptronModel implements Serializable {
	
	private static final long serialVersionUID = 7526471155622776147L;
	private static final Charset FILE_CHARSET = Charset.forName("ISO-8859-1");
    
//	public LibPruneAndScore owner;
    
    protected int[] actionCodes;
    protected int noOfActions;
    protected int inTopK;
    protected FeatureMap featureMap;
    
    protected int exploreAfterMIter;
    protected double exploreProb;

    protected int nCrossVal;
    protected CVPerceptronModel[] cvmodel;
    protected CVPerceptronModel fmodel;
    
    protected int currentSentNo;
    
    protected int mode;
    /*public ManageCVPerceptron(LibPruneAndScore l, int topk, int cv){
    	owner = l;
    	featureMap = owner.featureMap;
    	cvmodel = new CVPerceptronModel[cv];
		init(topk,cv);
    }*/
    
    public ManageCVPerceptron(FeatureMap map, int topk, int cv, int miter, double prob, int mod){
    	featureMap = map;
    	cvmodel = new CVPerceptronModel[cv];
    	fmodel = new CVPerceptronModel(this);
		init(topk,cv,miter,prob);
		mode = mod;
    }

    public void init(int topk, int crossval, int miter, double prob){
		//feat2id = new Hashtable<String, Integer>(Statics.FEAT_HASH_INIT);
		noOfActions = 0;
		setK(topk);
		setNCV(crossval);
		actionCodes = new int[0]; 
		for(int i = 0 ; i < getNCV(); i++)
		{
			cvmodel[i] = new CVPerceptronModel(this);
		}
		currentSentNo = -1;
		exploreAfterMIter = miter;
		exploreProb = prob;
//		setActionCodes(new HashSet<Integer>());
		//TODO: set action codes
    }
    
    public int getK()
    {
    	return inTopK;
    }
    
    public void setK(int k)
    {
    	inTopK = k;
    }
    
    public int getNCV()
    {
    	return nCrossVal;
    }
    
    public void setNCV(int cv)
    {
    	nCrossVal = cv;
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
    
    public int getNoOfActions()
    {
    	return noOfActions;
    }
    
    public int getCurrentCVIdx() throws MaltChainedException
    {
    	return  (getCurrentSentNo() % getNCV());
    }
    

	@Override
	public int  train(HashMap<Integer, Integer> actionCosts,
			MaltFeatureNode[] x, int[] pruned, int curIter) throws MaltChainedException {
		return train(actionCosts, x, curIter);
	}
    
	@Override
	public int train(HashMap<Integer,Integer> actionCosts, MaltFeatureNode[] x, int curIter) throws MaltChainedException {
		int curCVIdx =  getCurrentCVIdx();
    	
    	HashMap<Integer,FeatureList> actionFeats = new HashMap<Integer,FeatureList>();
    	for(int code:actionCodes)
		{
			FeatureList feats = new FeatureList();
			boolean gold = (actionCosts.get(code).intValue() == 0);
			binarizeFVWithAction(x, code, feats, gold);
			actionFeats.put(new Integer(code), feats);
		}
    	
    	for(int m = 0 ; m < getNCV(); m++)
    	{
    		// update every model except the curCVIdx
    		if(m == curCVIdx && getNCV() != 1)
    			continue;
    		cvmodel[m].train(actionCosts, x,actionFeats,curIter);
    	}
    	int retAction = fmodel.train(actionCosts, x, actionFeats, curIter);
    	actionFeats.clear();
    	return retAction;
	}

	
    public void train(SingleDecision decision, MaltFeatureNode[] x, int[] pruned) throws MaltChainedException {
    	train(decision,x);
    }
    
    public void train(SingleDecision decision, MaltFeatureNode[] x) throws MaltChainedException {

    	int curCVIdx =  getCurrentCVIdx();
    	
    	HashMap<Integer,FeatureList> actionFeats = new HashMap<Integer,FeatureList>();
    	for(int code:actionCodes)
		{
			FeatureList feats = new FeatureList();
			boolean gold = (code == decision.getDecisionCode());
			binarizeFVWithAction(x, code, feats, gold);
			actionFeats.put(new Integer(code), feats);
		}
    	
    	for(int m = 0 ; m < getNCV(); m++)
    	{
    		// update every model except the curCVIdx
    		if(m == curCVIdx && getNCV() != 1)
    			continue;
    		cvmodel[m].train(decision, x,actionFeats);
    	}
    	fmodel.train(decision, x, actionFeats);
    	actionFeats.clear();
    	return;
	}
    
    public double getScoreFromCVModel(int cvidx, FeatureList feats)
    {
    	return cvmodel[cvidx].getScore(feats);
    }
    
    public double getCmltScoreFromCVModel(int cvidx, FeatureList feats)
    {
    	return cvmodel[cvidx].getCmltScore(feats);
    }
    
    public double getScoreFromFModel(FeatureList feats)
    {
    	return fmodel.getScore(feats);
    }
    
    public double getCmltScoreFromFModel(FeatureList feats)
    {
    	return fmodel.getCmltScore(feats);
    }

    @Override
	public int[] predict(MaltFeatureNode[] x, boolean cmlWts) {
		try { // ONLU USED FOR PRUNEANDSCORE MODE
			double[] actionScores = new double[noOfActions];
			int[] predictionList = new int[noOfActions];

			HashMap<Integer,FeatureList> actionFeats = new HashMap<Integer,FeatureList>();
	    	for(int code:actionCodes)
			{
				FeatureList feats = new FeatureList();
				binarizeFVWithAction(x, code, feats, false);
				actionFeats.put(new Integer(code), feats);
			}
	    	
			int k = 0; 
			for(int code:actionCodes)
			{
//				actionScores[k] = getScoreFromCVModel(getCurrentCVIdx(),actionFeats.get(code));
//				actionScores[k] = getScoreFromFModel(actionFeats.get(code));
				if(cmlWts)
					actionScores[k] = getCmltScoreFromFModel(actionFeats.get(code));
				else	
					actionScores[k] = getScoreFromFModel(actionFeats.get(code));
				
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
	public double[][] scorePredict(MaltFeatureNode[] x, boolean cmlWts) {
		int len = inTopK > actionCodes.length ? actionCodes.length : inTopK;
		final double[][] scoredPredictions = new double[2][len];
		try {
	    	double[] actionScores = new double[noOfActions];
	    	int[] predictionList = new int[noOfActions];
	
			HashMap<Integer,FeatureList> actionFeats = new HashMap<Integer,FeatureList>();
	    	for(int code:actionCodes)
			{
				FeatureList feats = new FeatureList();
				binarizeFVWithAction(x, code, feats, false);
				actionFeats.put(new Integer(code), feats);
			}
	    	
			int k = 0; 
			for(int code:actionCodes)
			{
				if(cmlWts)
					actionScores[k] = getCmltScoreFromCVModel(getCurrentCVIdx(),actionFeats.get(code));
				else	
					actionScores[k] = getScoreFromCVModel(getCurrentCVIdx(),actionFeats.get(code));
				
//				actionScores[k] = getCmltScoreFromCVModel(getCurrentCVIdx(),actionFeats.get(code));
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
			for(int i = 0; i < len; i++)
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
	public int[] predict(MaltFeatureNode[] x) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] scorePredict(MaltFeatureNode[] x) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] predict(MaltFeatureNode[] x, int[] prunedActionList,
			boolean cmltWts) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] scorePredict(MaltFeatureNode[] x, int[] prunedActionList,
			boolean cmltWts) {
		// TODO Auto-generated method stub
		return null;
	}


}



