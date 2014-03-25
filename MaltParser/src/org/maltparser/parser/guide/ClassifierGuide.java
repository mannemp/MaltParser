package org.maltparser.parser.guide;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.history.GuideHistory;
import org.maltparser.parser.history.action.GuideDecision;

public interface ClassifierGuide extends Guide {
	public enum GuideMode { BATCH, CLASSIFY}
	
	public void addInstance(GuideDecision decision) throws MaltChainedException;
	public void addInstance(GuideDecision decision, DependencyStructure goldGraph) throws MaltChainedException;
	public void addPruneInstance(GuideDecision decision) throws MaltChainedException;
	public void noMoreInstances() throws MaltChainedException;
	public void saveModel(String preSuffix) throws MaltChainedException;
	public void predict(GuideDecision decision) throws MaltChainedException;
	public FeatureVector predictExtract(GuideDecision decision) throws MaltChainedException;
	public FeatureVector extract() throws MaltChainedException;
	public boolean predictFromKBestList(GuideDecision decision) throws MaltChainedException;
	
	public GuideMode getGuideMode();
	public GuideHistory getHistory();
	public FeatureModelManager getFeatureModelManager();
}
