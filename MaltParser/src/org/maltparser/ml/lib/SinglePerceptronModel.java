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
import org.maltparser.parser.history.kbest.Candidate;

public class SinglePerceptronModel extends MaltPerceptronModel implements Serializable  {
	
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
    protected int exploreAfterMIter;
    protected double exploreProb;
    protected Random random;    
    protected int currentSentNo;
    
    /*public MaltPerceptronModel(Lib l){
    	this(l,1);
    }
    
    public MaltPerceptronModel(Lib l, int topk){
    	owner = l;
		init(topk);
    }*/
    
    public SinglePerceptronModel(FeatureMap map, int topk, int miter, double prob){
    	featureMap = map;
		init(topk, miter, prob);
    }

    public void init(int topk, int miter, double prob){
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
		exploreAfterMIter = miter;
		exploreProb = prob;
		random = new Random();
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
		    if(id2feat.contains(feaid))
		    	score += ((PFeat)id2feat.get(feaid)).wt.getWeight();
		}
		return score;
    }
    
    public double  getCmltScore(FeatureList feats){
    	double  score = 0;
		for (int i=0; i<feats.size(); i++){
		    int feaid = feats.get(i).index;
		    if(id2feat.contains(feaid))
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
    
    public static int[] getCodesFromCandidates(ArrayList<Candidate> pruned)
    {
    	int[] prunedArr = new int[pruned.size()];
    	int k = 0;
    	for(Candidate cand:pruned)
    		prunedArr[k] = pruned.get(k++).getActionCode();
    		
    	return prunedArr;
    }
    
    @Override
	public int train(HashMap<Integer, Integer> actionCosts, MaltFeatureNode[] x, int curIter)
			throws MaltChainedException {
		// TODO Auto-generated method stub
		return train(actionCosts, x, actionCodes, curIter);
	}
    
    public int train(HashMap<Integer, Integer> actionCosts, MaltFeatureNode[] x, int[] prunedList, int curIter) throws MaltChainedException {
		
    	double[][] scoreList = scorePredict(x,prunedList,false);

    	// get Top Oracle Action
    	int bestAllowedAction = prunedList[0];
		double bestAllowedCodeScore = Double.NEGATIVE_INFINITY; 
		int bestAllowedCodeCost = Integer.MAX_VALUE;
		for(int code:prunedList)
		{
			if(actionCosts.get(code).intValue() < bestAllowedCodeCost)
			{
				bestAllowedAction = code;
				bestAllowedCodeCost = actionCosts.get(code).intValue();
				for(int s = 0 ; s <scoreList[0].length; s++)
				{
					if((int)scoreList[0][s] == code)
					{
						bestAllowedCodeScore = scoreList[1][s];
						break;
					}
				}
			}
			else if(actionCosts.get(code).intValue() == bestAllowedCodeCost)
			{
				double curScore = 0;
				for(int s = 0 ; s <scoreList[0].length; s++)
				{
					if((int)scoreList[0][s] == code)
					{
						curScore = scoreList[1][s];
						break;
					}
				}
				if(bestAllowedCodeScore < curScore)
				{
					bestAllowedAction = code;
					bestAllowedCodeScore = curScore;
				}
			}
		}
		/*if(bestAllowedCodeCost != 0)
			System.err.println("Can't find a 0 cost action");*/
		
		// Get topK predictions
		ArrayList<Integer> prunedPredictions = new ArrayList<Integer>();
		for(int i = 0; i < scoreList[0].length && prunedPredictions.size() <= getK(); i++)
			prunedPredictions.add((int)scoreList[0][i]);
		
		// get least cost Predicted Action in topKPredictions
    	int bestPredictedAction = prunedPredictions.get(0).intValue();
    	int bestPredictionCost = actionCosts.get(bestPredictedAction);
    	for(int i = 1; i < prunedPredictions.size(); i++)
    	{
    		int pcode = prunedPredictions.get(i).intValue();
			if(actionCosts.get(pcode) < bestPredictionCost)
			{
				bestPredictedAction = pcode;
				bestPredictionCost = actionCosts.get(bestPredictedAction);
			}
    	}

    	// < is not needed !! 
//    	if(actionCosts.get(bestPredictedAction).intValue() == actionCosts.get(bestAllowedAction).intValue())
    	if(bestPredictionCost <= bestAllowedCodeCost)
    		return bestPredictedAction;
    	// else i.e. predictedAction's cost is greater than that of bestAllowedCode
    	
    	allround ++;
    	FeatureList goldFeats = new FeatureList();
		binarizeFVWithAction(x, bestAllowedAction, goldFeats, true);
		
		for(int i = 0; i < prunedPredictions.size(); i++)
		{
			updateFeats(goldFeats, +1.0, allround);
			
			int pcode = prunedPredictions.get(i).intValue();
			FeatureList feats = new FeatureList();
			binarizeFVWithAction(x, pcode, feats, false);
			updateFeats(feats, -1.0, allround);
			
			double gscore = getScore(goldFeats);
			double pscore = getScore(feats);
			pscore+=0;
		}
		return explore(bestAllowedAction,bestPredictedAction,curIter);
	}

    public int explore(int topOracleAction, int bestPredictedAction, int curIter)
    {
    	
    	if(curIter > exploreAfterMIter && random.nextDouble() < exploreProb)
    		return bestPredictedAction;
    	else
    		return topOracleAction;
    }
    
    public void train(SingleDecision decision, MaltFeatureNode[] x, int[] prunedList) throws MaltChainedException {
		
    	int[] predictionList = predict(x,false);
    	int truecode = decision.getDecisionCode();
    	FeatureList goldFeats = new FeatureList();

    	// TODO: WHAT IF truecode ISN'T IN prunedList ???
    	
    	int bestcode = 1;
    	for(int i = 0; i < predictionList.length ; i++)
    	{
    		for(int pcode : prunedList)
    		{
    			if(pcode == predictionList[i])
    			{
    				bestcode = pcode;
    				i = predictionList.length;
    				break;
    			}
    		}
    	}
    	/*boolean found = false;
    	for(int i = 0; i < predictionList.length && i < inTopK; i++)
    	{
    		if(predictionList[i] == truecode)
    		{
    			found = true;
    			break;
    		}
    	}*/
//    	if(found)
    	if(bestcode == truecode)
    		return;
//		if(predictionList[0] != truecode)
		{
			binarizeFVWithAction(x, truecode, goldFeats, true);
			allround ++;
		}
		
//    	for(int i = 0 ; i < inTopK && i < predictionList.length; i++)
    	{
    		/*if(predictionList[i] == truecode)
    			break;*/
    		// Update weights
    		updateFeats(goldFeats, +1.0, allround);
    		FeatureList feats = new FeatureList();
    		binarizeFVWithAction(x, bestcode, feats, false);
    		updateFeats(feats, -1.0, allround);
    		double gscore = getScore(goldFeats);
    		double pscore = getScore(feats);
    		pscore+=0;
    	}
    	
    	return;
	}

    public void train(SingleDecision decision, MaltFeatureNode[] x) throws MaltChainedException {
//    	String decisionSettings = owner.getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
//		SymbolTable actionTable = owner.getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
		
//    	MaltFeatureNode[][] binarizedActionMFNs = new MaltFeatureNode[actionTable.getCodes().size()][x.length];
    	int[] predictionList = predict(x,false);
    	int truecode = decision.getDecisionCode();
    	FeatureList goldFeats = new FeatureList();
		
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
//		if(predictionList[0] != truecode)
		{
			binarizeFVWithAction(x, truecode, goldFeats, true);
			allround ++;
		}
		
    	for(int i = 0 ; i < getK() && i < predictionList.length; i++)
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
	public int[] predict(MaltFeatureNode[] x, boolean cmlWts) {
		return predict(x,getActionCodes(),cmlWts);
	}
    
    @Override
	public int[] predict(MaltFeatureNode[] x,int[] prunedActionList, boolean cmlWts) {
		// DONE: Auto-generated method stub
		try {
			/*String decisionSettings = owner.getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
			SymbolTable actionTable = owner.getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
			noOfActions = actionTable.size();*/

			double[] actionScores = new double[prunedActionList.length];
			int[] predictionList = new int[prunedActionList.length];
//			MaltFeatureNode[][] binarizedActionMFNs = new MaltFeatureNode[actionTable.getCodes().size()][x.length];
		
			int k = 0; 
//			for(int code: actionTable.getCodes())
			for(int code:prunedActionList)
			{
				FeatureList feats = new FeatureList();
				binarizeFVWithAction(x, code, feats, false);
				if(cmlWts)
					actionScores[k] = getCmltScore(feats);
				else	
					actionScores[k] = getScore(feats);
				predictionList[k++] = code;
			}

			double tmpDec;
			int tmpObj;
			int lagest;
			
			for (int i=0; i < prunedActionList.length-1; i++) {
				lagest = i;
				for (int j=i; j < prunedActionList.length; j++) {
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
	public double[][] scorePredict(MaltFeatureNode[] x, int[] prunedActionList, boolean cmlWts) {
		// DONE: Auto-generated method stub
//		int len = getK() > getActionCodes().length ? getActionCodes().length : getK();
		int len = prunedActionList.length;
    	double[] actionScores = new double[prunedActionList.length];
    	final double[][] scoredPredictions = new double[2][len];
    	int[] predictionList = new int[prunedActionList.length];
		try {
			/*String decisionSettings = owner.getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
			SymbolTable actionTable = owner.getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);*/
//			MaltFeatureNode[][] binarizedActionMFNs = new MaltFeatureNode[actionTable.getCodes().size()][x.length];
		
			int k = 0;
			for(int code: prunedActionList)
			{
				FeatureList feats = new FeatureList();
				binarizeFVWithAction(x, code, feats, false);
				if(cmlWts)
					actionScores[k] = getCmltScore(feats);
				else	
					actionScores[k] = getScore(feats);
//				actionScores[k] = getCmltScore(feats);
				predictionList[k++] = code;
			}

			double tmpDec;
			int tmpObj;
			int lagest;
			
			for (int i=0; i < prunedActionList.length-1; i++) {
				lagest = i;
				for (int j=i; j < prunedActionList.length; j++) {
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
	public double[][] scorePredict(MaltFeatureNode[] x, boolean cmlWts) {
		return scorePredict(x,getActionCodes(),cmlWts);
	}

	@Override
	public int[] predict(MaltFeatureNode[] x) { // NEVER CALLED
		// TODO Auto-generated method stub
		return predict(x,false);
	}

	@Override
	public double[][] scorePredict(MaltFeatureNode[] x) { // NEVER CALLED
		// TODO Auto-generated method stub
		return scorePredict(x, false);
	}

	
}



