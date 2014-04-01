package org.maltparser.parser.pruneandscore;

import java.util.Set;
import java.util.regex.Pattern;

import org.maltparser.MaltConsoleEngine;
import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.flow.FlowChartInstance;
import org.maltparser.core.flow.item.ChartItem;
import org.maltparser.core.flow.spec.ChartItemSpecification;
import org.maltparser.core.helper.SystemLogger;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.io.dataformat.DataFormatManager;
import org.maltparser.core.io.dataformat.DataFormatSpecification.DataStructure;
import org.maltparser.core.io.dataformat.DataFormatSpecification.Dependency;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.MappablePhraseStructureGraph;
import org.maltparser.ml.lib.LibException;
import org.maltparser.parser.DependencyParserConfig;
/**
 * @author Johan Hall
 *
 */
public class PruneAndScoreChartItem extends ChartItem {
	private PruneAndScore pruneAndScore;
	private String idName;
	private String targetName;
	private String sourceName;
	private String modeName;
	private String taskName;
	private DependencyStructure cachedSourceGraph = null;
	private DependencyStructure cachedTargetGraph = null;

	
	
	public void initialize(FlowChartInstance flowChartinstance, ChartItemSpecification chartItemSpecification) throws MaltChainedException {
		super.initialize(flowChartinstance, chartItemSpecification);
		
		for (String key : chartItemSpecification.getChartItemAttributes().keySet()) {
			if (key.equals("target")) {
				targetName = chartItemSpecification.getChartItemAttributes().get(key);
			} else if (key.equals("source")) {
				sourceName = chartItemSpecification.getChartItemAttributes().get(key);
			}  else if (key.equals("mode")) {
				modeName = chartItemSpecification.getChartItemAttributes().get(key);
			}  else if (key.equals("task")) {
				taskName = chartItemSpecification.getChartItemAttributes().get(key);
			} else if (key.equals("id")) {
				idName = chartItemSpecification.getChartItemAttributes().get(key);
			}
		}
		if (targetName == null) {
			targetName = getChartElement("pruneandscore").getAttributes().get("target").getDefaultValue();
		} else if (sourceName == null) {
			sourceName = getChartElement("pruneandscore").getAttributes().get("source").getDefaultValue();
		} else if (modeName == null) {
			modeName = getChartElement("pruneandscore").getAttributes().get("mode").getDefaultValue();
		} else if (taskName == null) {
			taskName = getChartElement("pruneandscore").getAttributes().get("task").getDefaultValue();
		} else if (idName == null) {
			idName = getChartElement("pruneandscore").getAttributes().get("id").getDefaultValue();
		}
		pruneAndScore = (PruneAndScore)flowChartinstance.getFlowChartRegistry(org.maltparser.parser.pruneandscore.PruneAndScore.class, idName);
		if (pruneAndScore == null) {
			pruneAndScore = new PruneAndScore();
			flowChartinstance.addFlowChartRegistry(org.maltparser.parser.pruneandscore.PruneAndScore.class, idName, pruneAndScore);
			flowChartinstance.addFlowChartRegistry(org.maltparser.core.config.Configuration.class, idName, pruneAndScore);
		}
	}
	
	public int getMode(String modestr) throws MaltChainedException
	{
		if(modestr.equals("learn"))
			return DependencyParserConfig.LEARN;
		else if(modestr.equals("parse"))
			return DependencyParserConfig.PARSE;
		else if(modestr.equals("plearn"))
			return DependencyParserConfig.PLEARN;
		else if(modestr.equals("prune"))
			return DependencyParserConfig.PRUNE;
		else if(modestr.equals("slearn"))
			return DependencyParserConfig.SLEARN;
		else if(modestr.equals("score"))
			return DependencyParserConfig.SCORE;
		else if(modestr.equals("pruneandscore"))
			return DependencyParserConfig.PRUNEANDSCORE;
		else if(modestr.equals("peval"))
			return DependencyParserConfig.PEVAL;
		
		throw new MaltChainedException("The modename is invalid:"+modestr);
		
	}
	
	
	public int preprocess(int signal) throws MaltChainedException {
		if (taskName.equals("init")) {
			if(modeName.length() > 0){ // i.e. there actually is a mode 
				OptionManager.instance().overloadOptionValue(getOptionContainerIndex(), "pruneandscore", "mode", modeName);
				ConfigurationDir configDir = (ConfigurationDir)flowChartinstance.getFlowChartRegistry(org.maltparser.core.config.ConfigurationDir.class, idName);
				DataFormatManager dataFormatManager = configDir.getDataFormatManager();
				DataFormatInstance dataFormatInstance = null;
				dataFormatInstance = configDir.getDataFormatInstance(dataFormatManager.getInputDataFormatSpec().getDataFormatName());
				pruneAndScore.initialize(getOptionContainerIndex(), dataFormatInstance, configDir, getMode(modeName));

			} else {
				return ChartItem.TERMINATE;
			}
		}
		return signal;
	}
	
	public void createConfigFile(int iteration) throws MaltChainedException
	{
		String outCharSet;
		if (OptionManager.instance().getOptionValue(getOptionContainerIndex(), "output", "charset") != null) {
			outCharSet = OptionManager.instance().getOptionValue(getOptionContainerIndex(), "output", "charset").toString();
		} else {
			outCharSet = "UTF-8";
		}
		pruneAndScore.configDir.getSymbolTables().save(pruneAndScore.configDir.getOutputStreamWriter("symboltables.sym", outCharSet));
		OptionManager.instance().saveOptions(getOptionContainerIndex(), pruneAndScore.configDir.getOutputStreamWriter("savedoptions.sop"));
		pruneAndScore.configDir.createConfigFile(".i"+iteration);
	}
	
	public int process(int signal) throws MaltChainedException {
		if (taskName.equals("process")) {
			if (cachedSourceGraph == null) {
				cachedSourceGraph = (DependencyStructure)flowChartinstance.getFlowChartRegistry(org.maltparser.core.syntaxgraph.DependencyStructure.class, sourceName);
			}
			if (cachedTargetGraph == null) {
				cachedTargetGraph = (DependencyStructure)flowChartinstance.getFlowChartRegistry(org.maltparser.core.syntaxgraph.DependencyStructure.class, targetName);
			}
			pruneAndScore.currentSentNo = (cachedSourceGraph.getSentenceID() >> 9);
//			int curIter = ((cachedSourceGraph.getSentenceID() << 22) >> 10);
			int curIter = (1 << 9) -1 ;
			curIter = (cachedSourceGraph.getSentenceID() & curIter) ;
			if(pruneAndScore.currentIterNo < curIter && isInLearnMode() && !modeName.equals("peval"))
			{
					//TODO: store models to ".i"+iteration
//					pruneAndScore.getGuide().saveModel(".i"+(curIter-1));
					if (OptionManager.instance().getOptionValue(getOptionContainerIndex(), "config", "saveconfigaftereachiter").toString().equals("true")) {
						pruneAndScore.getGuide().saveModel("");
						createConfigFile(curIter);
					}
					System.out.println("ITERATION:"+(curIter-1));
					pruneAndScore.evaluator.printMetrics();
					pruneAndScore.evaluator.saveMetrics(curIter-1);
					pruneAndScore.evaluator.printHashMetrics();
					pruneAndScore.evaluator.reset();
					pruneAndScore.currentIterNo = curIter;
			}
			Object[] args = new Object[2];
			args[0] = cachedTargetGraph;
			args[1] = cachedSourceGraph;
			pruneAndScore.process(args);
			/*if (modeName.equals("learn") ) {
				pruneAndScore.oracleParse(cachedSourceGraph, cachedTargetGraph);
			} 
			else if (modeName.equals("plearn")) {
				pruneAndScore.pasOracleParse(cachedSourceGraph, cachedTargetGraph);
			}else if (modeName.equals("parse")) {
				pruneAndScore.parse(cachedSourceGraph);
//				if (cachedSourceGraph instanceof MappablePhraseStructureGraph) {
//					System.out.println("MappablePhraseStructureGraph");
//					((MappablePhraseStructureGraph)cachedSourceGraph).getMapping().connectUnattachedSpines((MappablePhraseStructureGraph)cachedSourceGraph);
//				}	
			}*/
		}
		return signal;
	}
	
	public int postprocess(int signal) throws MaltChainedException {
		if (taskName.equals("train") && pruneAndScore.getGuide() != null) {
			pruneAndScore.getGuide().noMoreInstances();
		} else if (taskName.equals("train") && pruneAndScore.getGuide() == null) {
			pruneAndScore.train();
		}
		pruneAndScore.postProcess();
		return signal;
	}

	public void terminate() throws MaltChainedException {
		if (flowChartinstance.getFlowChartRegistry(org.maltparser.parser.pruneandscore.PruneAndScore.class, idName) != null) {
//			pruneAndScore.postProcess();
			pruneAndScore.terminate(null);
			flowChartinstance.removeFlowChartRegistry(org.maltparser.parser.pruneandscore.PruneAndScore.class, idName);
			flowChartinstance.removeFlowChartRegistry(org.maltparser.core.config.Configuration.class, idName);
			pruneAndScore = null;
		} else {
			pruneAndScore = null;
		}
		cachedSourceGraph = null;
		cachedTargetGraph = null;
	}
	
	public boolean isInLearnMode()
	{
		int mode;
		try {
			mode = getMode(modeName);
			if (mode == PruneAndScore.SLEARN || mode == PruneAndScore.PLEARN || mode == PruneAndScore.LEARN || mode == PruneAndScore.PEVAL)
				return true;
			else
				return false;
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isInInferenceMode()
	{
		int mode;
		try {
			mode = getMode(modeName);
			if (mode == PruneAndScore.PARSE || mode == PruneAndScore.PRUNE || mode == PruneAndScore.SCORE || mode == PruneAndScore.PRUNEANDSCORE)
				return true;
			else
				return false;
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public PruneAndScore getPruneAndScore() {
		return pruneAndScore;
	}
	
	public void setPruneAndScore(PruneAndScore npruneAndScore) {
		this.pruneAndScore = npruneAndScore;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return obj.toString().equals(this.toString());
	}
	
	public int hashCode() {
		return 217 + (null == toString() ? 0 : toString().hashCode());
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("    pruneandscore ");
		sb.append("id:");sb.append(idName);
		sb.append(' ');
		sb.append("mode:");sb.append(modeName);
		sb.append(' ');
		sb.append("task:");sb.append(taskName);
		sb.append(' ');
		sb.append("source:");sb.append(sourceName);
		sb.append(' ');
		sb.append("target:");sb.append(targetName);
		return sb.toString();
	}
}