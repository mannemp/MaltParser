package org.maltparser.ml.lib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.MultipleFeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.core.helper.Util;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.Table;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.ml.LearningMethod;
import org.maltparser.parser.Trainer;
import org.maltparser.parser.algorithm.nivre.ArcEager;
import org.maltparser.parser.algorithm.nivre.NivreConfig;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.kbest.Candidate;
import org.maltparser.parser.pruneandscore.PruneAndScore;

public class LibPruneAndScore extends Lib {
	protected int pruneTopK;
	protected int pruneCV;
	protected int exploreAfterIter;
	protected double exploreProb;
	
	
	public LibPruneAndScore(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		super(owner, learnerMode, "libpruneandscore");
		pruneTopK = ((PruneAndScore)getConfiguration()).getPruneTopK();
		pruneCV = ((PruneAndScore)getConfiguration()).getPruneCV();
		exploreAfterIter = ((PruneAndScore)getConfiguration()).getExploreAfterIter();
		exploreProb = ((PruneAndScore)getConfiguration()).getExploreProb();
		
		try{
			// The following two are empty 
			String prepSuffix = "", preSuffix= "";
			
			if(getLibMode() == LEARN)
			{
				model = new SinglePerceptronModel(featureMap, Integer.MAX_VALUE,exploreAfterIter,exploreProb);
				pmodel = null;
			}
			else if(getLibMode() == PLEARN)
			{
				model = null;
				pmodel = new ManageCVPerceptron(featureMap, pruneTopK, pruneCV,exploreAfterIter,exploreProb,learnerMode);
			}
			else if(getLibMode() == PEVAL)
			{
				ObjectInputStream pinput = new ObjectInputStream(getInputStreamFromConfigFileEntry(prepSuffix+".pmoo"));
			    try {
			    	pmodel = (MaltPerceptronModel)pinput.readObject();
			    	/*if(getConfiguration().getOptionValue("pruneandscore","pasprunek") !=null){
						String pk = getConfiguration().getOptionValue("pruneandscore", "pasprunek").toString();
						pruneTopK = Integer.parseInt(pk);
						((MaltPerceptronModel)pmodel).setK(pruneTopK);
					}
			    	else*/
			    		pruneTopK = ((MaltPerceptronModel)pmodel).getK();
			    		System.err.println("PModel:"+getFile(prepSuffix+".pmoo"));
			    		System.err.println("Model Pruner P@K            : "+pruneTopK);
			    } finally {
			    	pinput.close();
			    }
			    model = new SinglePerceptronModel(featureMap, Integer.MAX_VALUE,exploreAfterIter,exploreProb);
			}
			else if(getLibMode() == SLEARN)
			{
				ObjectInputStream pinput = new ObjectInputStream(getInputStreamFromConfigFileEntry(prepSuffix+".pmoo"));
			    try {
			    	pmodel = (MaltPerceptronModel)pinput.readObject();
			    	/*if(getConfiguration().getOptionValue("pruneandscore","pasprunek") !=null){
						String pk = getConfiguration().getOptionValue("pruneandscore", "pasprunek").toString();
						pruneTopK = Integer.parseInt(pk);
						((MaltPerceptronModel)pmodel).setK(pruneTopK);
					}
			    	else*/
			    		pruneTopK = ((MaltPerceptronModel)pmodel).getK();
			    		pruneCV = ((ManageCVPerceptron)pmodel).getNCV();
			    } finally {
			    	pinput.close();
			    }
			    model = new SinglePerceptronModel(featureMap, Integer.MAX_VALUE,exploreAfterIter,exploreProb);
			}
			else if (getLibMode() == CLASSIFY || getLibMode() == SCORE)
			{
				ObjectInputStream input = new ObjectInputStream(getInputStreamFromConfigFileEntry(preSuffix+".moo"));
			    try {
			    	model = (MaltLibModel)input.readObject();
			    	System.err.println("Read Object in Parse Mode");
			    	((MaltPerceptronModel)model).setK(Integer.MAX_VALUE);
			    } finally {
			    	input.close();
			    }
			}
			else if(getLibMode() == PRUNE)
			{
				ObjectInputStream pinput = new ObjectInputStream(getInputStreamFromConfigFileEntry(prepSuffix+".pmoo"));
			    try {
			    	pmodel = (MaltPerceptronModel)pinput.readObject();
			    	/*if(getConfiguration().getOptionValue("pruneandscore","pasprunek") !=null){
						String pk = getConfiguration().getOptionValue("pruneandscore", "pasprunek").toString();
						pruneTopK = Integer.parseInt(pk);
						((MaltPerceptronModel)pmodel).setK(pruneTopK);
					}
			    	else*/
			    		pruneTopK = ((MaltPerceptronModel)pmodel).getK();
			    } finally {
			    	pinput.close();
			    }
			}
			else if (getLibMode() == PRUNEANDSCORE)
			{
				ObjectInputStream pinput = new ObjectInputStream(getInputStreamFromConfigFileEntry(prepSuffix+".pmoo"));
			    try {
			    	pmodel = (MaltPerceptronModel)pinput.readObject();
			    	/*if(getConfiguration().getOptionValue("pruneandscore","pasprunek") !=null){
						String pk = getConfiguration().getOptionValue("pruneandscore", "pasprunek").toString();
						pruneTopK = Integer.parseInt(pk);
						((MaltPerceptronModel)pmodel).setK(pruneTopK);
					}
			    	else*/
			    		pruneTopK = ((MaltPerceptronModel)pmodel).getK();
			    } finally {
			    	pinput.close();
			    }
				ObjectInputStream input = new ObjectInputStream(getInputStreamFromConfigFileEntry(preSuffix+".moo"));
			    try {
			    	model = (MaltLibModel)input.readObject();
			    	((MaltPerceptronModel)model).setK(Integer.MAX_VALUE);
			    } finally {
			    	input.close();
			    }
			}
			System.err.println("  Pruner P@K            : "+pruneTopK);
			System.err.println("  Pruner N-CrossVal     : "+pruneCV);
			System.err.println("  Explore Iter          : "+exploreAfterIter);
			System.err.println("  Explore Prob          : "+exploreProb);
			
		} catch (ClassNotFoundException e) {
			throw new LibException("Couldn't load the libpruneandscore models", e);
		} catch (Exception e) {
			throw new LibException("Couldn't load the libpruneandscore models", e);
		}
	}
	
	public MaltLibModel getModel()
	{
		if(getLibMode() == LEARN)
			return model;
		else if(getLibMode() == PLEARN)
			return pmodel;
		else if(getLibMode() == SLEARN)
			return model;
		else if(getLibMode() == PEVAL)
			return model;
		else if(getLibMode() == CLASSIFY)
			return model;
		else if(getLibMode() == PRUNE)
			return pmodel;
		else if(getLibMode() == SCORE)
			return model;
		else if(getLibMode() == PRUNEANDSCORE)
			return model;
		else
			return model;
	}

	public void setLibModeFromConfig() throws MaltChainedException {
		this.libMode = getConfiguration().getMode();
	}
	
	/*public int getCost(int code, DependencyStructure goldGraph) throws MaltChainedException
	{
		NivreConfig parseConfig = (NivreConfig)(getConfiguration().getAlgorithm().getCurrentParserConfiguration());
		parseConfig.getCostAfterAction(code, goldGraph);
		return 0;
	}*/
	
	/*public int getCost(int code) throws MaltChainedException
	{
		NivreConfig parseConfig = (NivreConfig)(getConfiguration().getAlgorithm().getCurrentParserConfiguration());
		return parseConfig.getCostAfterAction(code);
	}*/
	
	public int getActionCost(int actionCode, DependencyStructure goldGraph) throws MaltChainedException
	{
		NivreConfig parseConfig = (NivreConfig)(getConfiguration().getAlgorithm().getCurrentParserConfiguration());
//		int transCost = parseConfig.getCostAfterAction(getTransitionCode(actionCode));
		int transYGCost = parseConfig.getYGCostAfterAction(getTransitionCode(actionCode));
		
		/*int labelCode = getLabelCode(actionCode);
		
		if(transCost == 0 && labelCode != -1 && actionCode != trueCode)
			return transCost+1;
		*/
		return transYGCost+getLabelCost(actionCode, goldGraph);
	}
	
	public int getLabelCost(int actionCode, DependencyStructure goldGraph) throws MaltChainedException
	{
		NivreConfig parseConfig = (NivreConfig)(getConfiguration().getAlgorithm().getCurrentParserConfiguration());
		int labelCode = getLabelCode(actionCode);
		int transCode = getTransitionCode(actionCode);
		
		if(labelCode == -1)
			return 0;
		
		// ArcEager.LEFTARC
		int headIdx = parseConfig.getInput().peek().getIndex();
		int childIdx = parseConfig.getStack().peek().getIndex();
		if(transCode == ArcEager.RIGHTARC)
		{
			headIdx = parseConfig.getStack().peek().getIndex();
			childIdx = parseConfig.getInput().peek().getIndex();
		}

		String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
		SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
		String actionSymbol = actionTable.getSymbolCodeToString(actionCode);
		
		Table labelTable = getConfiguration().getGuide().getHistory().getTableHandler("A").getSymbolTable("DEPREL");
		String labelSymbol = actionSymbol.indexOf('~') == -1 ? null :actionSymbol.substring(actionSymbol.indexOf('~')+1);
		
		int goldLabelCode = goldGraph.getDependencyNode(childIdx).getHeadEdgeLabelCode((SymbolTable)labelTable);
		if( goldLabelCode != labelCode)
			return 1;
		
		return 0;
	}
	
	public boolean permissible(int actionCode) throws MaltChainedException
	{
		NivreConfig parseConfig = (NivreConfig)(getConfiguration().getAlgorithm().getCurrentParserConfiguration());
		return parseConfig.permissible(getTransitionCode(actionCode));
	}
	
	public int getTransitionCode(int actionCode) throws MaltChainedException
	{
		
		String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
		SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
		String actionSymbol = actionTable.getSymbolCodeToString(actionCode);
		
		Table transTable = getConfiguration().getGuide().getHistory().getTableHandler("T").getSymbolTable("TRANS");
		String transSymbol = actionSymbol.indexOf('~') == -1 ? actionSymbol :actionSymbol.substring(0, actionSymbol.indexOf('~')); 
		int  transCode = transTable.getSymbolStringToCode(transSymbol);
		
		/*Table labelTable = getConfiguration().getGuide().getHistory().getTableHandler("A").getSymbolTable("DEPREL");
		int  labelCode = labelTable.getSymbolStringToCode(actionSymbol.substring(actionSymbol.indexOf('~')+1));*/
		return transCode;
	}
	
	public int getLabelCode(int actionCode) throws MaltChainedException
	{
		
		String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
		SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
		String actionSymbol = actionTable.getSymbolCodeToString(actionCode);
		
		Table labelTable = getConfiguration().getGuide().getHistory().getTableHandler("A").getSymbolTable("DEPREL");
		String labelSymbol = actionSymbol.indexOf('~') == -1 ? null :actionSymbol.substring(actionSymbol.indexOf('~')+1);
		if(labelSymbol == null)
			return -1;
		
		int  labelCode = labelTable.getSymbolStringToCode(labelSymbol);
		return labelCode;
	}
	
	public void addInstance(SingleDecision decision, FeatureVector featureVector, DependencyStructure  goldGraph) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibException("The decision cannot be found");
		}
		MaltLibModel curModel = getModel();
		int curIter = ((PruneAndScore)getConfiguration()).getCurrentIterNo();
		try {
			String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
			SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
			((MaltPerceptronModel)curModel).setActionCodes(actionTable.getCodes());
//			String actionSymbol = actionTable.getSymbolCodeToString(3);//getConfiguration().getGuide().getHistory().
			((MaltPerceptronModel)curModel).setCurrentSentNo(getCurrentSentNo());
			final int n = featureVector.size();
			
			// get Action Costs 
			HashMap<Integer,Integer> actionCosts = new HashMap<Integer,Integer>();
			for(int code:((MaltPerceptronModel)curModel).getActionCodes())
			{
				int cost = getActionCost(code,goldGraph);
				actionCosts.put(code, cost);
			}
			
			// get Legal Actions
			ArrayList<Integer> permissibleActions = new ArrayList<Integer>();
			for(int code: ((MaltPerceptronModel)curModel).getActionCodes())
				if(permissible(code))
					permissibleActions.add(code);
			int[] prunedActions = new int[permissibleActions.size()];
			int l = 0;
			for(Integer code: permissibleActions)
				prunedActions[l++] = code.intValue();
			
			
			if(getLibMode() != PEVAL)
			{ // training

				if(getLibMode() == SLEARN)
					prunedActions = predictPrune(featureVector);
				MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
				int nextAction = ((MaltPerceptronModel)curModel).train(actionCosts,mfns,prunedActions,curIter/2);
				
//				if(actionCosts.get(decision.getDecisionCode()).intValue() !=0)
					decision.addDecision(nextAction);
				
				increaseNumberOfInstances();
				return;
			}
			else if(getLibMode() == PEVAL)
			{ 
				prunedActions = predictPrune(featureVector);
				
				int bestAllowedCode = prunedActions[0];
				double bestAllowedCodeScore = Double.NEGATIVE_INFINITY; 
				int bestAllowedCodeCost = Integer.MAX_VALUE;
				
				for(int code:prunedActions)
				{
					if(actionCosts.get(code).intValue() < bestAllowedCodeCost)
					{
						bestAllowedCode = code;
						bestAllowedCodeCost = actionCosts.get(code).intValue(); 
					}
					/*else if(actionCosts.get(code).intValue() == bestAllowedCodeCost)
					{
						double curScore = scoreList[1][code-1]; // WRONG
						if(bestAllowedCodeScore < curScore)
						{
							bestAllowedCode = code;
							bestAllowedCodeScore = curScore;
						}
					}*/
				}

				NivreConfig nConfig = (NivreConfig)getConfiguration().getAlgorithm().getParserState().getConfiguration();
				int position = nConfig.getInput().peek().getIndex();
				((PruneAndScore)getConfiguration()).evaluator.evaluate(bestAllowedCodeCost == 0 ? prunedActions[0] : 10000 , prunedActions, position);
				
//					if(actionCosts.get(decision.getDecisionCode()).intValue() !=0)
					decision.addDecision(bestAllowedCode);

				increaseNumberOfInstances();
				return;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new LibException("The learner cannot add instance ", e);
		}
	}
	
	@Override
	public void addInstance(SingleDecision decision, FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibException("The decision cannot be found");
		}
		MaltLibModel curModel = getModel();
		int curIter = ((PruneAndScore)getConfiguration()).getCurrentIterNo();
		try {
//			if(getCurrentSentNo() < 1000 )
			{
				String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
				SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
				((MaltPerceptronModel)curModel).setActionCodes(actionTable.getCodes());
			}
			((MaltPerceptronModel)curModel).setCurrentSentNo(getCurrentSentNo());
			final int n = featureVector.size();
			
			int[] prunedActions = ((MaltPerceptronModel)curModel).getActionCodes();
			if(getLibMode() == SLEARN || getLibMode() == PEVAL || (getLibMode() == PLEARN && curIter%2 ==0))
				prunedActions = predictPrune(featureVector);
			
			if(getLibMode() == PEVAL || (getLibMode() == PLEARN && curIter%2 ==0))
			{
				int truecode = decision.getDecisionCode();
				((PruneAndScore)getConfiguration()).evaluator.evaluate(truecode, prunedActions);
				increaseNumberOfInstances();
				return;
			}
			if(curIter %2 == 1)
			{
				MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
				((MaltPerceptronModel)curModel).train(decision,mfns,prunedActions);
				increaseNumberOfInstances();
			}
			else if(getLibMode() == LEARN )
			{
				prunedActions = predict(featureVector);
				int truecode = decision.getDecisionCode();
//				((PruneAndScore)getConfiguration()).evaluator.evaluate(truecode, prunedActions);
				((PruneAndScore)getConfiguration()).evaluator.evaluate(truecode, prunedActions[0]);
				increaseNumberOfInstances();
			}
			
		} catch (Exception e) {
			throw new LibException("The learner cannot write to the instance file. ", e);
		}
	}
	
	public void addInstanceLinear(SingleDecision decision, FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibException("The decision cannot be found");
		}
		if(getLibMode() != PruneAndScore.SLEARN){
			throw new LibException("Not in scorer's learn mode");
		}
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(decision.getDecisionCode()+"\t");
			final int n = featureVector.size();
//			MaltFeatureNode[] mfns = new MaltFeatureNode[n];

			for (int i = 0; i < n; i++) {
				FeatureValue featureValue = featureVector.getFeatureValue(i);
				if (featureValue == null || (excludeNullValues == true && featureValue.isNullValue())) {
					sb.append("-1");
//					mfns[i] = new MaltFeatureNode();
				} else {
					if (!featureValue.isMultiple()) {
						SingleFeatureValue singleFeatureValue = (SingleFeatureValue)featureValue;
						if (singleFeatureValue.getValue() == 1) {
//							mfns[i] = new MaltFeatureNode(singleFeatureValue.getIndexCode(), 1.0);
							sb.append(singleFeatureValue.getIndexCode());
						} else if (singleFeatureValue.getValue() == 0) {
//							mfns[i] = new MaltFeatureNode();
							sb.append("-1");
						} else {
//							mfns[i] = new MaltFeatureNode(singleFeatureValue.getIndexCode(), singleFeatureValue.getValue());
							sb.append(singleFeatureValue.getIndexCode());
							sb.append(":");
							sb.append(singleFeatureValue.getValue());
						}
					} else { //if (featureValue instanceof MultipleFeatureValue) {
						Set<Integer> values = ((MultipleFeatureValue)featureValue).getCodes();
						int j=0;
						// TODO: Does not handle multiplefeaturevalue right now !!
//						((MaltPerceptronModel)model).regFeat(values.toString());
//						mfns[i] = new MaltFeatureNode(values.toString(),1.0);
						for (Integer value : values) {
							sb.append(value.toString());
							if (j != values.size()-1) {
								sb.append("|");
							}
							j++;
						}
					}
//					else {
//						throw new LibException("Don't recognize the type of feature value: "+featureValue.getClass());
//					}
				}
				sb.append('\t');
			}
			sb.append('\n');
			instanceOutput.write(sb.toString());
//			instanceOutput.write(createRankListInstance(sb.toString()));
			if(numberOfInstances%100==0)
				instanceOutput.flush();
//			((MaltPerceptronModel)model).train(decision,mfns);
			increaseNumberOfInstances();
			sb.setLength(0);
			
		} catch (Exception e) {
			throw new LibException("The learner cannot write to the instance file. ", e);
		}
	}
	@Override
	public void addPruneInstance(SingleDecision decision, FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibException("The decision cannot be found");
		}
		if(getLibMode() != LearningMethod.PLEARN){
			throw new LibException("Not in pruner's learn mode");
		}
		
		try {
//			if(getCurrentSentNo() < 1000 )
			{
				String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
				SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
				pmodel.setActionCodes(actionTable.getCodes());
			}
			pmodel.setCurrentSentNo(getCurrentSentNo());
			final int n = featureVector.size();
			MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
			pmodel.train(decision,mfns);
			increaseNumberOfInstances();
			
		} catch (Exception e) {
			throw new LibException("The learner cannot write to the instance file. ", e);
		}
	}
	
	@Override
	public boolean predict(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		MaltLibModel curModel = getModel();
		try {
//			final FeatureList featureList = getFeatureList(featureVector);
//			decision.getKBestList().addList(model.predict(featureList.toArray()));
			((MaltPerceptronModel)curModel).setCurrentSentNo(getCurrentSentNo());
			try
			{
				int[] actionList;
				if(getLibMode() == PRUNEANDSCORE)
				{
					((MaltPerceptronModel)pmodel).setCurrentSentNo(getCurrentSentNo());
					actionList = predictPruneAndScore(featureVector);
				}
				
				else if(getLibMode() == PRUNE)
				{
					actionList = predictPrune(featureVector);
				}
				else //if(getLibMode() == SCORE || getLibMode() == CLASSIFY)
				{
					actionList = predict(featureVector);
				}
				decision.getKBestList().addList(actionList);
//				decision.getKBestList().addList(predictPruneAndScore(featureVector));
//				decision.getKBestList().addList(predictPrune(featureVector));
			}
			catch(Exception e){
				int[] tmp = new int[]{1};
				decision.getKBestList().addList(tmp);
				e.printStackTrace();
				throw new LibException("Exception in setCurrentSentNo", e);
			}
			
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
		catch(Exception e){
			e.printStackTrace();
			throw new LibException("Exception in predict", e);
		}
		
		return true;
	}
	
	public int[] predictPruneAndScore(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (pmodel == null || model == null) {
			throw new LibException("Either pruner or scorer model is null and hasn't been loaded.");
		}
		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		try {
			int[] pruneList = new int[0];
			pruneList = pmodel.predict(mfns,true);//scorePredict(mfns);
			double[][] scoreList = new double[2][0]; 
			scoreList = model.scorePredict(mfns,true);
			
			int len = pmodel.getK() > pruneList.length ? pruneList.length : pmodel.getK();

			int[] finalList = new int[len];
			int k = 0;
			for(int i = 0 ; k < len && i < scoreList[0].length; i++)
			{
//				if(Arrays.asList(pruneList[0]).contains(scoreList[0][i]))
//				for(double pcode : pruneList[0])
				for(int pcode : pruneList)
					if(Double.compare((double)pcode, scoreList[0][i]) == 0)
						finalList[k++] = (int)scoreList[0][i];
			}
			/*int[] retList = new int[scoreList[0].length];
			k = 0;
			for(double code:scoreList[0])
				retList[k++] = (int)code;*/
			/*int[] retList = new int[k];
			System.arraycopy( scoreList[0], 0, retList, 0, k );*/
			return finalList;
//			return retList;
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
		catch(Exception e){
			e.printStackTrace();
			throw new LibException("Exception in pruneAndScore ", e);
		}
	}
	
	public double[][] scorePredict(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (model == null) {
			throw new LibException("The scorer model is null and hasn't been loaded.");
		}
		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		try {
//			System.err.println(Arrays.toString(mfns));
			return model.scorePredict(mfns,true);
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	public int[] predict(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (model == null) {
			throw new LibException("The scorer model is null and hasn't been loaded.");
		}
		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		try {
			double[][] scoreList = model.scorePredict(mfns,true);
			/*System.err.println(Arrays.toString(mfns));
			System.err.println("Actions:"+Arrays.toString(scoreList[0]));
			System.err.println("Scores :"+Arrays.toString(scoreList[1]));*/
			/*scoreList = model.scorePredict(mfns,false);
			System.err.println("Actions:"+Arrays.toString(scoreList[0]));
			System.err.println("Scores :"+Arrays.toString(scoreList[1]));*/
	/*		int[] finalList = new int[pruneTopK];
			int k = 0;
			for(int i = 0 ; k < pruneTopK && i < scoreList[0].length; i++)
			{
//				if(Arrays.asList(pruneList[0]).contains(scoreList[0][i]))
				for(double pcode : pruneList[0])
					if(Double.compare(pcode, scoreList[0][i]) == 0)
						finalList[k++] = (int)scoreList[0][i];
			}*/
			int[] retList = new int[scoreList[0].length];
			int k = 0;
			for(double code:scoreList[0])
				retList[k++] = (int)code;
			/*int[] retList = new int[k];
			System.arraycopy( scoreList[0], 0, retList, 0, k );*/
//			return finalList;
			return retList;
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}

	public int[] predictPrune(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (pmodel == null) {
			throw new LibException("The pruner model is null and hasn't been loaded.");
		}
		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		try {
			double[][] pruneList = pmodel.scorePredict(mfns,true);
			int len = pmodel.getK() > pruneList[0].length ? pruneList[0].length : pmodel.getK();
			int[] finalList = new int[len];
			int k = 0;
			for(int i = 0 ; i < len; i++)
			{
//				if(permissible((int)pruneList[0][i]))
				finalList[k++] = (int)pruneList[0][i];
			}
			return finalList;
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	@Override
	public void saveModel(String preSuffix) throws MaltChainedException{
		if (getLibMode() == LearningMethod.PLEARN && pmodel != null) {
			try {
				if (configLogger.isInfoEnabled()) {
//					configLogger.info("Creating Libperceptron model "+getConfigNameFile(".pmoo").getCanonicalPath()+"\n");
					configLogger.info("Creating Pruner Libperceptron model "+getFile(preSuffix+".pmoo").getAbsolutePath()+"\n");
				}
//			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getConfigNameFile(".pmoo").getAbsolutePath())));
				ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(preSuffix+".pmoo").getAbsolutePath())));
		        try{
		          output.writeObject(pmodel);
		        } finally {
		          output.close();
		        }
			} catch (OutOfMemoryError e) {
				throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
			} catch (IllegalArgumentException e) {
				throw new LibException("The LibPruneandscore learner was not able to redirect Standard Error stream. ", e);
			} catch (SecurityException e) {
				throw new LibException("The LibPruneandscore learner cannot remove the instance file. ", e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new LibException("The LibPruneandscore learner cannot save the model file '"+getFile(".pmoo").getAbsolutePath()+"'. ", e);
			}
			return;
		}
		else if ((getLibMode() == LearningMethod.LEARN || getLibMode() ==  SLEARN ) && model != null) {
			try {
				if (configLogger.isInfoEnabled()) {
//					configLogger.info("Creating Libperceptron model "+getConfigNameFile(".pmoo").getCanonicalPath()+"\n");
					configLogger.info("Saving Scorer Libperceptron model "+getFile(preSuffix+".moo").getAbsolutePath()+"\n");
				}
//			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getConfigNameFile(".pmoo").getAbsolutePath())));
				ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(preSuffix+".moo").getAbsolutePath())));
		        try{
		          output.writeObject(model);
		        } finally {
		          output.close();
		        }
			} catch (OutOfMemoryError e) {
				throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
			} catch (IllegalArgumentException e) {
				throw new LibException("The LibPruneandscore learner was not able to redirect Standard Error stream. ", e);
			} catch (SecurityException e) {
				throw new LibException("The LibPruneandscore learner cannot remove the instance file. ", e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new LibException("The LibPruneandscore learner cannot save the model file '"+getFile(".moo").getAbsolutePath()+"'. ", e);
			}
			return;
		}
		
//		trainInternal(null);
		/*try {
//			if (configLogger.isInfoEnabled()) {
//				configLogger.info("\nSaving feature map "+getFile(".map").getName()+"\n");
//			}
			saveFeatureMap(new BufferedOutputStream(new FileOutputStream(getFile(".map").getAbsolutePath())), featureMap);
		} catch (FileNotFoundException e) {
			throw new LibException("The learner cannot save the feature map file '"+getFile(".map").getAbsolutePath()+"'. ", e);
		}*/
	}
	
	@Override
	protected void trainInternal(FeatureVector featureVector) throws MaltChainedException {
		
		try{
			/*if(getLibMode() == LearningMethod.PLEARN)
				saveFeatureMap(new BufferedOutputStream(new FileOutputStream(getFile(".pmap").getAbsolutePath())), featureMap);
			else
				saveFeatureMap(new BufferedOutputStream(new FileOutputStream(getFile(".map").getAbsolutePath())), featureMap);*/
		}
		catch(Exception e){}
		if (getLibMode() == LearningMethod.PLEARN && pmodel != null) {
			try {
				if (configLogger.isInfoEnabled()) {
//					configLogger.info("Creating Libperceptron model "+getConfigNameFile(".pmoo").getCanonicalPath()+"\n");
					configLogger.info("Creating Pruner Libperceptron model "+getFile(".pmoo").getAbsolutePath()+"\n");
				}
//			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getConfigNameFile(".pmoo").getAbsolutePath())));
				ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".pmoo").getAbsolutePath())));
		        try{
		          output.writeObject(pmodel);
		        } finally {
		          output.close();
		        }
			} catch (OutOfMemoryError e) {
				throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
			} catch (IllegalArgumentException e) {
				throw new LibException("The LibPruneandscore learner was not able to redirect Standard Error stream. ", e);
			} catch (SecurityException e) {
				throw new LibException("The LibPruneandscore learner cannot remove the instance file. ", e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new LibException("The LibPruneandscore learner cannot save the model file '"+getFile(".pmoo").getAbsolutePath()+"'. ", e);
			}
			return;
		}
		else if ((getLibMode() == LearningMethod.LEARN || getLibMode() ==  SLEARN ) && model != null) {
			try {
				if (configLogger.isInfoEnabled()) {
//					configLogger.info("Creating Libperceptron model "+getConfigNameFile(".pmoo").getCanonicalPath()+"\n");
					configLogger.info("Saving Scorer Libperceptron model "+getFile(".moo").getAbsolutePath()+"\n");
				}
//			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getConfigNameFile(".pmoo").getAbsolutePath())));
				ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
		        try{
		          output.writeObject(model);
		        } finally {
		          output.close();
		        }
			} catch (OutOfMemoryError e) {
				throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
			} catch (IllegalArgumentException e) {
				throw new LibException("The LibPruneandscore learner was not able to redirect Standard Error stream. ", e);
			} catch (SecurityException e) {
				throw new LibException("The LibPruneandscore learner cannot remove the instance file. ", e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new LibException("The LibPruneandscore learner cannot save the model file '"+getFile(".moo").getAbsolutePath()+"'. ", e);
			}
			return;
		}
		
	}
	
	public void noMoreInstances() throws MaltChainedException { 
		// EMPTY
	}
	
	 
    public static boolean eliminate(double[] a) {
    	if (a.length == 0) {
    		return true;
    	}
    	for (int i = 1; i < a.length; i++) {
    		if (a[i] != a[i-1]) {
    			return false;
    		}
    	}
    	return true;
    }
    @Override
	protected void trainExternal(FeatureVector featureVector) throws MaltChainedException {
		try {		
			
			if (configLogger.isInfoEnabled()) {
				owner.getGuide().getConfiguration().getConfigLogger().info("Creating liblinear model (external) "+getFile(".mod").getName());
			}
			binariesInstances2SVMFileFormat(getInstanceInputStreamReader(".ins"), getInstanceOutputStreamWriter(".ins.tmp"));
			final String[] params = getLibParamStringArray();
			String[] arrayCommands = new String[params.length+3];
			int i = 0;
			arrayCommands[i++] = pathExternalTrain;
			for (; i <= params.length; i++) {
				arrayCommands[i] = params[i-1];
			}
			arrayCommands[i++] = getFile(".ins.tmp").getAbsolutePath();
			arrayCommands[i++] = getFile(".mod").getAbsolutePath();
			
	        if (verbosity == Verbostity.ALL) {
	        	owner.getGuide().getConfiguration().getConfigLogger().info('\n');
	        }
			final Process child = Runtime.getRuntime().exec(arrayCommands);
	        final InputStream in = child.getInputStream();
	        final InputStream err = child.getErrorStream();
	        int c;
	        while ((c = in.read()) != -1){
	        	if (verbosity == Verbostity.ALL) {
	        		owner.getGuide().getConfiguration().getConfigLogger().info((char)c);
	        	}
	        }
	        while ((c = err.read()) != -1){
	        	if (verbosity == Verbostity.ALL || verbosity == Verbostity.ERROR) {
	        		owner.getGuide().getConfiguration().getConfigLogger().info((char)c);
	        	}
	        }
            if (child.waitFor() != 0) {
            	owner.getGuide().getConfiguration().getConfigLogger().info(" FAILED ("+child.exitValue()+")");
            }
	        in.close();
	        err.close();
			if (configLogger.isInfoEnabled()) {
				configLogger.info("\nSaving Liblinear model "+getFile(".moo").getName()+"\n");
			}
			MaltLiblinearModel xmodel = new MaltLiblinearModel(getFile(".mod"));
		    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
	        try{
	          output.writeObject(xmodel);
	        } finally {
	          output.close();
	        }
	        if (!saveInstanceFiles) {
				getFile(".ins").delete();
				getFile(".mod").delete();
				getFile(".ins.tmp").delete();
	        }
	        if (configLogger.isInfoEnabled()) {
	        	configLogger.info('\n');
	        }
		} catch (InterruptedException e) {
			 throw new LibException("Learner is interrupted. ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	public void terminate() throws MaltChainedException { 
		super.terminate();
	}

	public void initLibOptions() {
		libOptions = new LinkedHashMap<String, String>();
		libOptions.put("s", "4"); // type = SolverType.L2LOSS_SVM_DUAL (default)
		libOptions.put("c", "0.1"); // cost = 1 (default)
		libOptions.put("e", "0.1"); // epsilon = 0.1 (default)
		libOptions.put("B", "-1"); // bias = -1 (default)
	}
	
	public void initAllowedLibOptionFlags() {
		allowedLibOptionFlags = "sceB";
	}
	
	private Problem readProblem(InputStreamReader isr) throws MaltChainedException {
		Problem problem = new Problem();
		final FeatureList featureList = new FeatureList();
		if (configLogger.isInfoEnabled()) {
			owner.getGuide().getConfiguration().getConfigLogger().info("- Read all training instances.\n");
		}
		try {
			final BufferedReader fp = new BufferedReader(isr);
			
			problem.bias = -1;
			problem.l = getNumberOfInstances();
			problem.x = new FeatureNode[problem.l][];
			problem.y = new int[problem.l];
			int i = 0;
			
			while(true) {
				String line = fp.readLine();
				if(line == null) break;
				int y = binariesInstance(line, featureList);
				if (y == -1) {
					continue;
				}
				try {
					problem.y[i] = y;
					problem.x[i] = new FeatureNode[featureList.size()];
					int p = 0;
			        for (int k=0; k < featureList.size(); k++) {
			        	MaltFeatureNode x = featureList.get(k);
						problem.x[i][p++] = new FeatureNode(x.getIndex(), x.getValue());
					}
					i++;
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new LibException("Couldn't read liblinear problem from the instance file. ", e);
				}

			}
			fp.close();
			problem.n = featureMap.size();
		} catch (IOException e) {
			throw new LibException("Cannot read from the instance file. ", e);
		}
		
		return problem;
	}
	
	private boolean checkProblem(Problem problem) throws MaltChainedException {
		int max_y = problem.y[0];
		for (int i = 1; i < problem.y.length; i++) {
			if (problem.y[i] > max_y) {
				max_y = problem.y[i];
			}
		}
		if (max_y * problem.l < 0) { // max_y * problem.l > Integer.MAX_VALUE
			if (configLogger.isInfoEnabled()) {
				owner.getGuide().getConfiguration().getConfigLogger().info("*** Abort (The number of training instances * the number of classes) > Max array size: ("+problem.l+" * "+max_y+") > "+Integer.MAX_VALUE+" and this is not supported by LibLinear.\n");
			}
			return false;
		}
		return true;
	}
	
	private Parameter getLiblinearParameters() throws MaltChainedException {
		Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
		String type = libOptions.get("s");
		
		if (type.equals("0")) {
			param.setSolverType(SolverType.L2R_LR);
		} else if (type.equals("1")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC_DUAL);
		} else if (type.equals("2")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC);
		} else if (type.equals("3")) {
			param.setSolverType(SolverType.L2R_L1LOSS_SVC_DUAL);
		} else if (type.equals("4")) {
			param.setSolverType(SolverType.MCSVM_CS);
		} else if (type.equals("5")) {
			param.setSolverType(SolverType.L1R_L2LOSS_SVC);	
		} else if (type.equals("6")) {
			param.setSolverType(SolverType.L1R_LR);	
		} else if (type.equals("7")) {
			param.setSolverType(SolverType.L2R_LR_DUAL);	
		} else {
			throw new LibException("The liblinear type (-s) is not an integer value between 0 and 4. ");
		}
		try {
			param.setC(Double.valueOf(libOptions.get("c")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear cost (-c) value is not numerical value. ", e);
		}
		try {
			param.setEps(Double.valueOf(libOptions.get("e")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear epsilon (-e) value is not numerical value. ", e);
		}
		return param;
	}
	
	public int getCurrentSentNo() throws MaltChainedException
	{
		return ((PruneAndScore)getConfiguration()).getCurrentSentNo();
	}
}
