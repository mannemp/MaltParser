package org.maltparser.parser;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.algorithm.nivre.NivreConfig;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.OracleGuide;
import org.maltparser.parser.guide.SingleGuide;
import org.maltparser.parser.hcsearch.HCSearch;
import org.maltparser.parser.history.GuideHistory;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.GuideDecision;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.action.MultipleDecision;
import org.maltparser.parser.history.action.SingleDecision;
/**
 * @author Johan Hall
 *
 */
public class DeterministicParser extends Parser {
	private int parseCount;
	protected ArrayList<ComplexDecisionAction> discrepantActions;
	private OracleGuide oracleGuide;
	
	public DeterministicParser(DependencyParserConfig manager) throws MaltChainedException {
		super(manager);
		setManager(manager);
		initParserState(1);
		manager.addRegistry(org.maltparser.parser.Algorithm.class, this);
		setGuide(new SingleGuide(manager, (GuideHistory)parserState.getHistory(), ClassifierGuide.GuideMode.CLASSIFY));
		oracleGuide = parserState.getFactory().makeOracleGuide(parserState.getHistory());
	}
	
	/*public DeterministicParser(DependencyParserConfig manager, ArrayList<ComplexDecisionAction> partialList) throws MaltChainedException {
		this(manager);
		discrepantActions = partialList;
	}*/
	
	public void setDiscrepantActions(ArrayList<ComplexDecisionAction> partialList)
	{
		discrepantActions = partialList;
	}
	
	public ArrayList<ComplexDecisionAction>  getDiscrepantActions()
	{
		return discrepantActions;
	}
	
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		if (diagnostics == true) {
			return parseDiagnostic(parseDependencyGraph);
		}
		parserState.clear();
		parserState.initialize(parseDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;
		TransitionSystem ts = parserState.getTransitionSystem();
		while (!parserState.isTerminalState()) {
			GuideUserAction action = ts.getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			if (action == null) {
				action = predict();
			}
			parserState.apply(action);
		} 
		copyEdges(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		parseDependencyGraph.linkAllTreesToRoot();
		currentParserConfiguration.getDependencyGraph().clear();
		return parseDependencyGraph;
	}
	
	public DependencyStructure pasParse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		if (diagnostics == true) {
			return parseDiagnostic(parseDependencyGraph);
		}
		parserState.clear();
		parserState.initialize(parseDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;
		TransitionSystem ts = parserState.getTransitionSystem();
		while (!parserState.isTerminalState()) {
			GuideUserAction action = ts.getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			if (action == null) {
				action = predict();
			}
			parserState.apply(action);
		} 
		copyEdges(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		parseDependencyGraph.linkAllTreesToRoot();
		currentParserConfiguration.getDependencyGraph().clear();
		return parseDependencyGraph;
	}
	
	public DependencyStructure normalparse(DependencyStructure sysDependencyGraph, ArrayList<Integer> partialActions) throws MaltChainedException {
//		setDiscrepantActions(partialActions);
		if (diagnostics == true) {
			return parseDiagnostic(sysDependencyGraph);
		}
		parserState.clear();
		parserState.initialize(sysDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;
		TransitionSystem ts = parserState.getTransitionSystem();
		while (!parserState.isTerminalState()) {
			GuideUserAction action = null ;
			ArrayList<ComplexDecisionAction> curActions = parserState.getHistory().getActionList();
//			ArrayList<Integer> curActions = parserState.getHistory().getActionsCodeList();
			// Get the discrepant action from @partialActions if available
			if(curActions.size() < partialActions.size())
			{
				// Treat the (except the last one) actions in partialAction as deterministic 
				int code = partialActions.get(curActions.size());
				GuideUserAction act2 = parserState.getHistory().getEmptyGuideUserAction();
				SingleDecision singleDecision = (act2 instanceof SingleDecision)?(SingleDecision)act2:((MultipleDecision)act2).getSingleDecision(0);
				singleDecision.getKBestList().add(code,1.0);
//				ComplexDecisionAction act = new ComplexDecisionAction((History)(parserState.getHistory()));
//				act.getSingleDecision(0).getKBestList().add(code);
				action = act2;
				
				// The discrepantAction (last one in partialActions) is not a legal one
				if(curActions.size() == partialActions.size() && !parserState.permissible(action))
				{
					sysDependencyGraph = null;
					return sysDependencyGraph;
				}
			}
			else
			{
				// If not available, proceed with normal parsing 
				action = ts.getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			}
			if (action == null) {
				action = predict();
			}
			parserState.apply(action);
		} 
		copyEdges(currentParserConfiguration.getDependencyGraph(), sysDependencyGraph);
		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), sysDependencyGraph);
		sysDependencyGraph.linkAllTreesToRoot();
		currentParserConfiguration.getDependencyGraph().clear();
		return sysDependencyGraph;
	}
	

	public DependencyStructure parse(DependencyStructure sysDependencyGraph, ArrayList<Integer> partialActions) throws MaltChainedException {
//		setDiscrepantActions(partialActions);
		try{
		if (diagnostics == true) {
			return parseDiagnostic(sysDependencyGraph);
		}
		parserState.clear();
		parserState.initialize(sysDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;
		TransitionSystem ts = parserState.getTransitionSystem();
		while (!parserState.isTerminalState()) {
			GuideUserAction action = null ;
			ArrayList<ComplexDecisionAction> curActions = parserState.getHistory().getActionList();
//			ArrayList<Integer> curActions = parserState.getHistory().getActionsCodeList();
			// Get the discrepant action from @partialActions if available
			if(curActions.size() +1 < partialActions.size())
			{
				// Treat the (except the last one) actions in partialAction as deterministic 
				int code = partialActions.get(curActions.size());
				GuideUserAction act2 = parserState.getHistory().getEmptyGuideUserAction();
				SingleDecision singleDecision = (act2 instanceof SingleDecision)?(SingleDecision)act2:((MultipleDecision)act2).getSingleDecision(0);
				singleDecision.getKBestList().add(code,1.0f);
//				ComplexDecisionAction act = new ComplexDecisionAction((History)(parserState.getHistory()));
//				act.getSingleDecision(0).getKBestList().add(code);
				action = act2;
				
				// The discrepantAction (last one in partialActions) is not a legal one
				if(curActions.size() == partialActions.size() && !parserState.permissible(action))
				{
					sysDependencyGraph = null;
					return sysDependencyGraph;
				}
			}
			else if(curActions.size()+1== partialActions.size())
			{
				int disCode = partialActions.get(curActions.size());
				action = predict();
				SingleDecision singleDecision = (action instanceof SingleDecision)?(SingleDecision)action:((MultipleDecision)action).getSingleDecision(0);
				boolean contains = false;
				int index = -1;
				float topScore = singleDecision.getKBestList().getCandidate(0).getScore();
				for(int i = 0; i < HCSearch.KBest && i < singleDecision.getKBestList().getCurrentSize(); i++)
				{
					if(singleDecision.getKBestList().getCandidate(i).getScore() > HCSearch.DiscThreshold
							&& topScore - singleDecision.getKBestList().getCandidate(i).getScore()  < HCSearch.GapThreshold
							&& disCode == singleDecision.getKBestList().getCandidate(i).getActionCode())
					{
						contains = true;
						index = i;
						break;
					}
				}
				if(contains)
				{
					singleDecision.getKBestList().bringToTop(index);
					singleDecision.addDecision(disCode);
					if(!parserState.permissible(action))
					{
						sysDependencyGraph = null;
						return sysDependencyGraph;
					}
					else{
						contains = true;
					}
				}
				else
				{
					sysDependencyGraph = null;
					return sysDependencyGraph;
				}
			}
			else
			{
				// If not available, proceed with normal parsing 
				action = ts.getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			}
			if (action == null) {
				action = predict();
			}
			parserState.apply(action);
		} 
		copyEdges(currentParserConfiguration.getDependencyGraph(), sysDependencyGraph);
		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), sysDependencyGraph);
		sysDependencyGraph.linkAllTreesToRoot();
		currentParserConfiguration.getDependencyGraph().clear();
		}
		catch(MaltChainedException e)
		{
			sysDependencyGraph = null;
			return null;
		}
		return sysDependencyGraph;
	}
	
	public DependencyStructure oracleParse(DependencyStructure goldDependencyGraph, DependencyStructure parseDependencyGraph) throws MaltChainedException {
//		setDiscrepantActions(partialActions);
		if (diagnostics == true) {
			return parseDiagnostic(parseDependencyGraph);
		}
		parserState.clear();
		parserState.initialize(parseDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;
		TransitionSystem ts = parserState.getTransitionSystem();
		while (!parserState.isTerminalState()) {
			GuideUserAction action = null ;
			action = ts.getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			if (action == null) {
//				action = predict();
				action = oracleGuide.predict(goldDependencyGraph, currentParserConfiguration);
			}
			parserState.apply(action);
		} 
//		copyEdges(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
//		parseDependencyGraph.linkAllTreesToRoot();
//		oracleGuide.finalizeSentence(parseDependencyGraph);
//		copyEdges(currentParserConfiguration.getDependencyGraph(), goldDependencyGraph);
//		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), goldDependencyGraph);
//		goldDependencyGraph.linkAllTreesToRoot();
		currentParserConfiguration.getDependencyGraph().clear();
		return parseDependencyGraph;
	}
	
	private DependencyStructure parseDiagnostic(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		parserState.clear();
		parserState.initialize(parseDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;
		if (diagnostics == true) {
			writeToDiaFile(parseCount + "");
		}
		while (!parserState.isTerminalState()) {
			GuideUserAction action = parserState.getTransitionSystem().getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			if (action == null) {
				action = predict();
			} else if (diagnostics == true) {
				writeToDiaFile(" *");
			}
			if (diagnostics == true) {
				writeToDiaFile(" " + parserState.getTransitionSystem().getActionString(action));
			}
			parserState.apply(action);
		} 
		copyEdges(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		parseDependencyGraph.linkAllTreesToRoot();
		if (diagnostics == true) {
			writeToDiaFile("\n");
		}
		return parseDependencyGraph;
	}
	
	
	private GuideUserAction predict() throws MaltChainedException {
		GuideUserAction currentAction = parserState.getHistory().getEmptyGuideUserAction();
		try {
			classifierGuide.predict((GuideDecision)currentAction);
			while (!parserState.permissible(currentAction)) {
				if (classifierGuide.predictFromKBestList((GuideDecision)currentAction) == false) {
					currentAction = getParserState().getTransitionSystem().defaultAction(parserState.getHistory(), currentParserConfiguration);
					break;
				}
			}
		} catch (NullPointerException e) {
			throw new MaltChainedException("The guide cannot be found. ", e);
		}
		return currentAction;
	}
	
	public void terminate() throws MaltChainedException {
		if (diagnostics == true) {
			closeDiaWriter();
		}
	}
}
