package org.maltparser.parser.history;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;
import org.maltparser.parser.history.kbest.Candidate;
import org.maltparser.parser.history.kbest.ScoredCandidate;
/**
*
* @author Johan Hall
* @since 1.1
**/
public interface GuideUserHistory {
	public GuideUserAction getEmptyGuideUserAction() throws MaltChainedException; 
	public ArrayList<ActionContainer> getActionContainers();
	public ActionContainer[] getActionContainerArray();
	public int getNumberOfDecisions();
	public ArrayList<ComplexDecisionAction> getActionList();
	public ArrayList<Integer> getActionsCodeList() throws MaltChainedException;
	public ArrayList<ScoredCandidate> getScoredActionsList() throws MaltChainedException;
	public void clear() throws MaltChainedException;
	public ArrayList<Candidate> getActionsList() throws MaltChainedException; 
}
