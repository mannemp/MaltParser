package org.maltparser.parser.hcsearch;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Vector;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.config.ConfigurationException;
import org.maltparser.core.config.ConfigurationRegistry;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.flow.FlowChartManager;
import org.maltparser.core.helper.SystemLogger;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.TokenStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.Algorithm;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.Parser;
import org.maltparser.parser.SingleMalt;
import org.maltparser.parser.Trainer;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.kbest.ScoredCandidate;

public class HCSearch {
//	public static enum SearchMode { LEARN, PARSE};
	private SingleMalt singleMalt;
//	public static final int LEARN = 0;
	public static final int PARSE = 1;
	public static final int HLEARN = 2;
	public static final int CLEARN = 3;
	public static int KBest = 2;//5;
	public static float ActionThreshold = Float.MAX_VALUE;
	public static float DiscThreshold = Float.MIN_VALUE;
	public static float GapThreshold = Float.MAX_VALUE;
	public static int maxDepth = 70;
	
	protected ArrayList<HCSearchState> curStepStates;
	protected ArrayList<HCSearchState> allStates;
	protected HCSearchState curBestState;
	protected int mode;
	protected DependencyGraph initGraph;
	protected DependencyGraph goldGraph;
	protected ArrayList<ScoredCandidate> oracleActionList;
	
	public static PFeatLib featW;
	
	public void initialize(int m) throws MaltChainedException
	{
		curStepStates = new ArrayList<HCSearchState>();
		allStates = new ArrayList<HCSearchState>();
		mode = m;
		featW = new PFeatLib();
	}
	
	public void process(DependencyGraph graph) throws MaltChainedException
	{
		long startTime = System.currentTimeMillis();
		Stats.noOfSents ++;
        Stats.print("\n"+Stats.noOfSents+"["+graph.nDependencyNode()+"]");
        /*if(Stats.noOfSents%50 == 0)
        	System.err.println();*/
        
		if(inLearnMode())
		{
			goldGraph = graph;
			DependencyGraph gsysGraph = getDuplicateDepGraph(graph);
			singleMalt.parseGold(goldGraph,gsysGraph);
			oracleActionList = singleMalt.getAlgorithm().getParserState().getHistory().getScoredActionsList();//getActionsCodeList());
		}
		
		DependencyGraph initGraph = getDuplicateDepGraph(graph);
		singleMalt.parse(initGraph);
		ArrayList<ScoredCandidate> clonedActionList = singleMalt.getAlgorithm().getParserState().getHistory().getScoredActionsList();
		curBestState = new HCSearchState(this, initGraph, clonedActionList, 0, 0);
		allStates.add(curBestState);
		populateStats(curBestState, startTime);

		
//		Stats.initStateTime += Stats.getTimeTillNow(startTime);
		/*Stats.lossAtDepth[0] += curBestState.getLoss();
		Stats.accuracyAtDepth[0] += 1-curBestState.getAccuracy();
		Stats.bestAccuracyAtDepth[0] += 1-curBestState.getAccuracy();
		Stats.visitsAtDepth[0]++;
		Stats.timeAtDepth[0] += Stats.getTimeTillNow(startTime);*/
		
		traverseLL(curBestState);

		// find the best one according to the cost function
		/*HCSearchState cBestState = findMaxCState(allStates);
		if(inLearnMode())
		{
			Stats.cRounds++;
			// @bestlossstates : set of best loss states from curStepStates;
			ArrayList<HCSearchState> bestLossStates = getBestLossStates(allStates);
			// and then pick the one with the highest hscore.
			HCSearchState cGoodState = findMaxCState(bestLossStates);
			// hbeststate is the one in curStepStates with the highest hscore.
			double scoreMargin = cGoodState.getHScore() - cBestState.getHScore();
			double lossMargin = cGoodState.getLoss() - cBestState.getLoss();
			if(lossMargin > scoreMargin)
			{
				int round = Stats.cRounds; // DONE
				double param = (lossMargin - scoreMargin)/Math.pow(scoreMargin, 2);
				featW.updateHFeat(cGoodState.getFeatureVector(),param,round);
				featW.updateHFeat(cGoodState.getFeatureVector(),-1*param,round);
			}
			updateCScores(allStates);
		}*/
	}
	
	public HCSearchState runPolicy(DependencyGraph curGraph, ArrayList<Integer> partialActionSeq) throws MaltChainedException
	{
		curGraph = (DependencyGraph)singleMalt.parse(curGraph,partialActionSeq);
		if(curGraph == null)
			return null;
		ArrayList<ScoredCandidate> clonedActionList = singleMalt.getAlgorithm().getParserState().getHistory().getScoredActionsList();
		HCSearchState newHCSearchState = new HCSearchState(this, curGraph, clonedActionList,
				partialActionSeq.size(),partialActionSeq.get(partialActionSeq.size()-1).intValue());
		return newHCSearchState;
	}
	

	public void traverseLL(HCSearchState curState) throws MaltChainedException
	{
		/*int hValue = getPositionOfFirstWrongAction(curState.getActionSequence());
		if(hValue == oracleActionList.size())
		{
			Stats.noOfSentsWithZeroLossAtDepth[curState.getDepth()] += 1;
			Stats.avgDepth += curState.getDepth();
			if(Stats.maxDepth < curState.getDepth())
				Stats.maxDepth = curState.getDepth();
			return;
		}*/
		
		long startTime = System.currentTimeMillis();

		prepareCurStepStatesForNextSteps();
		getNextStates(curState);
		HCSearchState hBestState = getOracleBestStateInStep(curState);

		allStates.addAll(curStepStates);
		Stats.print("-"+curState.getDepth()+":"+curStepStates.size());
		
		if(hBestState == curState || hBestState.getDepth() >= maxDepth)
		{
			// EITHER: Reached maximum depth
			// OR: The best possible state at this step is the state that has been searched for discrepancies; 
			// TODO: Run the EndOfSearch formalities
			populateStatsOfRemainingDepths(hBestState, startTime);
			HCSearchState cBestState = getOracleBestStateFromAll();
			curBestState = cBestState; // with LL search, hBestState is the same as cBestState
			if(hBestState.getLoss()!=0)
				System.err.print(" ERROR: Loss not zero at the end");
			return;
		}
		else
		{
			populateStats(hBestState, startTime);
			traverseLL(hBestState);
			// Proceed with further search
		}
		
		return;
	}

	
	public void traverse(HCSearchState curState) throws MaltChainedException
	{
		/*int hValue = getPositionOfFirstWrongAction(curState.getActionSequence());
		if(hValue == oracleActionList.size())
		{
			Stats.noOfSentsWithZeroLossAtDepth[curState.getDepth()] += 1;
			Stats.avgDepth += curState.getDepth();
			if(Stats.maxDepth < curState.getDepth())
				Stats.maxDepth = curState.getDepth();
			return;
		}*/
		
		long startTime = System.currentTimeMillis();
		Stats.hRounds++;
		prepareCurStepStatesForNextSteps();
		getNextStates(curState); // DONE: Handle curState in findMaxHState and other such functions
		allStates.addAll(curStepStates);
		Stats.print("-"+curState.getDepth()+":"+curStepStates.size());
		
		curStepStates.add(curState);
		HCSearchState hBestState = findMaxHState(curStepStates);
		if(inHLearnMode())
		{
			HCSearchState hOracleState = getOracleBestStateInStep(curState);
			/*// @bestlossstates : set of best loss states from curStepStates;
			ArrayList<HCSearchState> bestLossStates = getBestLossStates(curStepStates);
			// and then pick the one with the highest hscore.
			HCSearchState hGoodState = findMaxHState(bestLossStates);
			// hbeststate is the one in curStepStates with the highest hscore.
			double scoreMargin = hGoodState.getHScore() - hBestState.getHScore();
			double lossMargin = hGoodState.getLoss() - hBestState.getLoss();
			if(lossMargin > scoreMargin)
			{
				int round = Stats.hRounds; // DONE
				double param = (lossMargin - scoreMargin)/Math.pow(scoreMargin, 2);
				featW.updateHFeat(hGoodState.getFeatureVector(),param,round);
				featW.updateHFeat(hBestState.getFeatureVector(),-1*param,round);
			}*/
			double trueMargin = Math.max( 1, hOracleState.getLoss() - hBestState.getLoss());
			int round = Stats.hRounds; // DONE
			double param = 1;
			featW.updateHFeat(hOracleState.getFeatureVector(),param,round);
			featW.updateHFeat(hBestState.getFeatureVector(),-1*param,round);

			updateHScores(curStepStates);
		}
//		HCSearchState hBestState = getOracleBestStateInStep(curState);

		if(hBestState == curState || hBestState.getDepth() >= maxDepth)
		{
			// EITHER: Reached maximum depth
			// OR: The best possible state at this step is the state that has been searched for discrepancies; 
			// TODO: Run the EndOfSearch formalities
			populateStatsOfRemainingDepths(hBestState, startTime);
			HCSearchState cBestState = findMaxCState(allStates);

			if(inCLearnMode())
			{
				HCSearchState cOracleState = getOracleBestStateFromAll();
				/*// @bestlossstates : set of best loss states from curStepStates;
				ArrayList<HCSearchState> bestLossStates = getBestLossStates(curStepStates);
				// and then pick the one with the highest hscore.
				HCSearchState hGoodState = findMaxHState(bestLossStates);
				// hbeststate is the one in curStepStates with the highest hscore.
				double scoreMargin = hGoodState.getHScore() - hBestState.getHScore();
				double lossMargin = hGoodState.getLoss() - hBestState.getLoss();
				if(lossMargin > scoreMargin)
				{
					int round = Stats.hRounds; // DONE
					double param = (lossMargin - scoreMargin)/Math.pow(scoreMargin, 2);
					featW.updateHFeat(hGoodState.getFeatureVector(),param,round);
					featW.updateHFeat(hBestState.getFeatureVector(),-1*param,round);http://techmeme.com/
				}*/
				double trueMargin = Math.max( 1, cOracleState.getLoss() - hBestState.getLoss());
				int round = Stats.cRounds; // DONE
				double param = 1;
				featW.updateCFeat(cOracleState.getFeatureVector(),param,round);
				featW.updateCFeat(cBestState.getFeatureVector(),-1*param,round);
				updateCScores(allStates);
			}
			curBestState = cBestState; // with LL search, hBestState is the same as cBestState
			if(hBestState.getLoss()!=0)
				Stats.print(" ERROR: Loss not zero at the end");
			return;
		}
		else
		{
			populateStats(hBestState, startTime);
			traverse(hBestState);
			// Proceed with further search
		}
		
		return;
	}
	
	public void updateHScores(ArrayList<HCSearchState> states)
	{
		for(HCSearchState curS :states)
			curS.updateHScore();
		return;
	}
	
	public void updateCScores(ArrayList<HCSearchState> states)
	{
		for(HCSearchState curS :states)
			curS.updateCScore();
		return;
	}
	
	public HCSearchState findMaxHState(ArrayList<HCSearchState> states)
	{
		HCSearchState maxpt = states.get(0);
		double maxscore = Double.MIN_VALUE;
		for(HCSearchState curpt : states)
		{
			double curscore = curpt.getHScore();
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
			double curscore = curpt.getCScore();
			if(curscore > maxscore)
			{
				maxscore = curscore;
				maxpt = curpt;
			}
		}
		return maxpt;
	}
	
	public void populateStatsOfRemainingDepths(HCSearchState curState, long startTime)
	{
		// TODO: check for noOracleInSearch 
		int d = curState.getDepth()+1;
		for(;d<maxDepth; d++)
		{
			Stats.lossAtDepth[d] += curState.getLoss();
//			Stats.nodesAtDepth[d] += 0;
			Stats.accuracyAtDepth[d] += 1 - curState.getAccuracy();
//			Stats.visitsAtDepth[d]++;
//			Stats.timeAtDepth[d] += Stats.getTimeTillNow(startTime);
		}
		
		return;
	}
	
	public void populateStats(HCSearchState curState, long startTime)
	{
		// TODO: check for noOracleInSearch 
		int d = curState.getDepth();
		Stats.lossAtDepth[d] += curState.getLoss();
		Stats.nodesAtDepth[d] += curStepStates.size();
		Stats.accuracyAtDepth[d] += 1 - curState.getAccuracy();
		Stats.visitsAtDepth[d]++;
		Stats.timeAtDepth[d] += Stats.getTimeTillNow(startTime);
		
		return;
	}
	
	public ArrayList<HCSearchState> getBestLossStates(ArrayList<HCSearchState> curStates)
	{
		int bestValue = Integer.MAX_VALUE;
		for(HCSearchState curS: curStates)
		{
			int curVal = curS.getLoss();
			if(curVal < bestValue)
				bestValue= curVal;
		}
		
		ArrayList<HCSearchState> bestStates = new ArrayList<HCSearchState>();
		for(HCSearchState curS: curStates)
		{
			int curVal = curS.getLoss();
			if(curVal == bestValue)
				bestStates.add(curS);
		}
		return bestStates;
	}
	
	public HCSearchState getOracleBestStateInStep(HCSearchState curState)
	{
		double bestValue = getPositionOfFirstWrongAction(curState.getActionSequence());
		HCSearchState bestState = curState;
		for(HCSearchState curS: curStepStates)
		{
			double posOfFirstWrongAction = getPositionOfFirstWrongAction(curS.getActionSequence());
//			posOfFirstWrongAction = goldGraph.nEdges() - curS.getLoss();
//			posOfFirstWrongAction = 1-curS.getAccuracy();
			if(posOfFirstWrongAction  > bestValue)
			{
				bestValue= posOfFirstWrongAction;
				bestState = curS;
			}
		}
		return bestState;
	}
	
	public HCSearchState getOracleBestStateFromAll()
	{
		double bestValue = Double.MIN_VALUE;
		HCSearchState bestState = null;
		for(HCSearchState curS: allStates)
		{
			double posOfFirstWrongAction = getPositionOfFirstWrongAction(curS.getActionSequence());
//			posOfFirstWrongAction = goldGraph.nEdges() - curS.getLoss();
//			posOfFirstWrongAction = 1-curS.getAccuracy();
			if(posOfFirstWrongAction  > bestValue)
			{
				bestValue= posOfFirstWrongAction;
				bestState = curS;
			}
		}
		return bestState;
	}
	
	public void prepareCurStepStatesForNextSteps()
	{
		curStepStates.clear();
		return;
	}
	
	public void getNextStates(HCSearchState curState) throws MaltChainedException
	{
		DependencyGraph curGraph = curState.getGraph();
//		DependencyNode node = curGraph.getTokenNode(2);
//		String pos = node.getLabelSymbol(curGraph.getSymbolTables().getSymbolTable("POSTAG"));
//		String pos = curGraph.getSymbolTables().getSymbolTable("POSTAG");
		ArrayList<ScoredCandidate> actionSeq = curState.getActionSequence();
		
		String decisionSettings = singleMalt.getAlgorithm().getManager().getOptionValue("guide", "decision_settings").toString().trim();
		SymbolTable actionTable = singleMalt.getSymbolTables().getSymbolTable(decisionSettings);

		int noofpolicyruns = 0;
		for(int i = curState.getDiscrepancyPosition(); i < actionSeq.size(); i++)
		{
			if(actionSeq.get(i).getScore() > ActionThreshold)
				continue; // do not consider discrepancies at this position
			
			// create a partial list of actions (from left to right) until the discrepant position
			ArrayList<Integer> partialList = new ArrayList<Integer>();
			for (int j = 0; j <i ; j++)
				partialList.add(actionSeq.get(j).getActionCode());
			int psize = partialList.size();
			partialList.add(null);

			for(int c : actionTable.getCodes() )
			{
//				if(c == actionSeq.get(curState.getDiscrepancyPosition()).getSingleDecision(0).getDecisionCode())
				if(c == actionSeq.get(i).getActionCode())
					continue;
				
				// create a new dep graph
				DependencyGraph successorGraph = getDuplicateDepGraph(curState.getGraph());
				
				String act = actionTable.getSymbolCodeToString(c);
//				ComplexDecisionAction disAction = new ComplexDecisionAction((History)(singleMalt.getAlgorithm().getParserState().getHistory()));
//				disAction.getSingleDecision(0).getKBestList().add(c);
				
				partialList.set(psize,new Integer(c));
				
				HCSearchState newHCSearchState = runPolicy(successorGraph, partialList);
				noofpolicyruns++;
				if(newHCSearchState != null)
				{
					newHCSearchState.setDiscrepantActionSymbol(actionTable.getSymbolCodeToString(c));
					curState.addChildState(newHCSearchState);
					curStepStates.add(newHCSearchState);
					newHCSearchState.setDepth(curState.getDepth()+1);
				}
			}
		}
//		statesVisited.clear();
		Stats.policyRunsAtDepth[curState.getDepth()+1] += noofpolicyruns;
		return;
	}
	
	public int getMode()
	{
		return mode;
	}
	
	/*public boolean inLearnMode()
	{
		return (mode == HCSearch.LEARN);
	}*/
	
	public boolean inLearnMode()
	{
		return (mode == HCSearch.HLEARN || mode == HCSearch.CLEARN);
	}
	
	public boolean inCLearnMode()
	{
		return (mode == HCSearch.CLEARN);
	}

	public boolean inHLearnMode()
	{
		return (mode == HCSearch.HLEARN);
	}
	
	public boolean inParseMode()
	{
		return (mode == HCSearch.PARSE);
	}
	
	public int getPositionOfFirstWrongAction(ArrayList<ScoredCandidate> actions)
	{
		int i =0;
		for (i = 0 ; i < oracleActionList.size() && i < actions.size(); i++)
			if(actions.get(i).getActionCode() != oracleActionList.get(i).getActionCode())
				break;
		
		return i;
	}
	
	public DependencyGraph getDuplicateDepGraph(DependencyGraph source) throws MaltChainedException
	{
		DependencyGraph duplicate = new DependencyGraph(source.getSymbolTables());
		copyTerminalStructure(source, duplicate);
		for (SymbolTable table : source.getDefaultRootEdgeLabels().keySet()) {
			duplicate.setDefaultRootEdgeLabel(table, source.getDefaultRootEdgeLabelSymbol(table));
		}
		return duplicate;
	}
	
	public void copyTerminalStructure(TokenStructure sourceGraph, TokenStructure targetGraph) throws MaltChainedException {
		targetGraph.clear();
		for (int index : sourceGraph.getTokenIndices()) {
			DependencyNode gnode = sourceGraph.getTokenNode(index);
			DependencyNode pnode = targetGraph.addTokenNode(gnode.getIndex());
			for (SymbolTable table : gnode.getLabelTypes()) {
				pnode.addLabel(table, gnode.getLabelSymbol(table));
			}
		}
	}
	
	public void setSingleMalt(SingleMalt smalt)
	{
		singleMalt = smalt;
	}
	
	public SingleMalt getSingleMalt()
	{
		return singleMalt;
	}
	
	public void terminate()
	{
		
		Stats.printStats();
	}

}
