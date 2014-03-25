package org.maltparser.parser.hcsearch;

import java.util.Set;
import java.util.regex.Pattern;

import org.maltparser.MaltConsoleEngine;
import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.flow.FlowChartInstance;
import org.maltparser.core.flow.item.ChartItem;
import org.maltparser.core.flow.spec.ChartItemSpecification;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.io.dataformat.DataFormatManager;
import org.maltparser.core.io.dataformat.DataFormatSpecification.DataStructure;
import org.maltparser.core.io.dataformat.DataFormatSpecification.Dependency;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.TokenStructure;
import org.maltparser.parser.SingleMalt;

public class HCSearchChartItem extends ChartItem {
	private HCSearch hcSearch;
	private String idName;
	private String modeName;
	private String targetName;
	private String sourceName;
	private String taskName;
	

	private DependencyStructure cachedSourceGraph = null;
	private DependencyStructure cachedTargetGraph = null;
	
	public void initialize(FlowChartInstance flowChartinstance, ChartItemSpecification chartItemSpecification) throws MaltChainedException {
		Stats.startTime = System.currentTimeMillis();
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
			targetName = getChartElement("singlemalt").getAttributes().get("target").getDefaultValue();
		} else if (sourceName == null) {
			sourceName = getChartElement("singlemalt").getAttributes().get("source").getDefaultValue();
		} else if (modeName == null) {
			modeName = getChartElement("singlemalt").getAttributes().get("mode").getDefaultValue();
		} else if (taskName == null) {
			taskName = getChartElement("singlemalt").getAttributes().get("task").getDefaultValue();
		} else if (idName == null) {
			idName = getChartElement("singlemalt").getAttributes().get("id").getDefaultValue();
		}
		
		hcSearch = (HCSearch)flowChartinstance.getFlowChartRegistry(org.maltparser.parser.hcsearch.HCSearch.class, idName);
		if (hcSearch == null) {
			hcSearch = new HCSearch();
			if (OptionManager.instance().getOptionValueNoDefault(MaltConsoleEngine.OPTION_CONTAINER, "config", "hckbest") != null) {
				String hck = OptionManager.instance().getOptionValue(MaltConsoleEngine.OPTION_CONTAINER, "config", "hckbest").toString();
				HCSearch.KBest = Integer.parseInt(hck);
			}
			if (OptionManager.instance().getOptionValueNoDefault(MaltConsoleEngine.OPTION_CONTAINER, "config", "hcthreshold") != null) {
				String hct = OptionManager.instance().getOptionValue(MaltConsoleEngine.OPTION_CONTAINER, "config", "hcthreshold").toString();
				HCSearch.DiscThreshold = Float.parseFloat(hct);
			}
			if (OptionManager.instance().getOptionValueNoDefault(MaltConsoleEngine.OPTION_CONTAINER, "config", "hcactionthreshold") != null) {
				String hcat = OptionManager.instance().getOptionValue(MaltConsoleEngine.OPTION_CONTAINER, "config", "hcactionthreshold").toString();
				HCSearch.ActionThreshold = Float.parseFloat(hcat);
			}
			if (OptionManager.instance().getOptionValueNoDefault(MaltConsoleEngine.OPTION_CONTAINER, "config", "hcgapthreshold") != null) {
				String hcat = OptionManager.instance().getOptionValue(MaltConsoleEngine.OPTION_CONTAINER, "config", "hcgapthreshold").toString();
				HCSearch.GapThreshold = Float.parseFloat(hcat);
			}
			flowChartinstance.addFlowChartRegistry(org.maltparser.parser.hcsearch.HCSearch.class, idName, hcSearch);
			System.err.println("\n  HCSearch K-best pruning        : "+HCSearch.KBest);
			System.err.println("  HCSearch action threshold      : "+HCSearch.ActionThreshold);
			System.err.println("  HCSearch discrepancy threshold : "+HCSearch.DiscThreshold);
			System.err.println("  HCSearch gap threshold         : "+HCSearch.GapThreshold+"\n");
		}
	}
	
	@Override
	public int preprocess(int signal) throws MaltChainedException {
		if (taskName.equals("init")) {
			if (modeName.equals("hlearn") || modeName.equals("clearn") || modeName.equals("parse")) {
			
				/*if (modeName.equals("learn"))
					hcSearch.initialize(HCSearch.LEARN);
				else*/ if (modeName.equals("hlearn"))
					hcSearch.initialize(HCSearch.HLEARN);
				else if (modeName.equals("clearn"))
					hcSearch.initialize(HCSearch.CLEARN);
				else
					hcSearch.initialize(HCSearch.PARSE);
				
				SingleMalt singleMalt = (SingleMalt)flowChartinstance.getFlowChartRegistry(org.maltparser.parser.SingleMalt.class, idName);
				hcSearch.setSingleMalt(singleMalt);
			} else {
				return ChartItem.TERMINATE;
			}
		}
		return signal;
	}

	@Override
	public int process(int signal) throws MaltChainedException {
		if (taskName.equals("process")) {
			// SingleMalt should always be in parse mode for hcsearch
			OptionManager.instance().overloadOptionValue(getOptionContainerIndex(), "singlemalt", "mode", "parse");
			if (cachedSourceGraph == null) {
				cachedSourceGraph = (DependencyStructure)flowChartinstance.getFlowChartRegistry(org.maltparser.core.syntaxgraph.DependencyStructure.class, sourceName);
			}
			
			if (cachedTargetGraph == null) {
				cachedTargetGraph = (DependencyStructure)flowChartinstance.getFlowChartRegistry(org.maltparser.core.syntaxgraph.DependencyStructure.class, targetName);
			}

			if(hcSearch.getSingleMalt() == null)
				hcSearch.setSingleMalt( (SingleMalt)flowChartinstance.getFlowChartRegistry(org.maltparser.parser.SingleMalt.class, "singlemalt"));
			
//			if(cachedSourceGraph instanceof DependencyGraph)
			hcSearch.process((DependencyGraph)cachedSourceGraph);
//			else
//				throw new MaltChainedException("hcSearchChartItem: Parse from SingleMalt not an instance of DependencyGraph");
			if (modeName.equals("learn")) {
//				hcSearch.traverse(cachedSourceGraph, cachedTargetGraph);
			} else if (modeName.equals("parse")) {
//				hcSearch.traverse(hcSearch.curHCSearchState);//(cachedSourceGraph);
			}
		}
		return signal;
	}

	@Override
	public int postprocess(int signal) throws MaltChainedException {
		// TODO Auto-generated method stub
		return signal;
	}

	@Override
	public void terminate() throws MaltChainedException {
		if (flowChartinstance.getFlowChartRegistry(org.maltparser.parser.hcsearch.HCSearch.class, idName) != null) {
			hcSearch.terminate();
			flowChartinstance.removeFlowChartRegistry(org.maltparser.parser.SingleMalt.class, idName);
			flowChartinstance.removeFlowChartRegistry(org.maltparser.parser.hcsearch.HCSearch.class, idName);
			flowChartinstance.removeFlowChartRegistry(org.maltparser.core.config.Configuration.class, idName);
			hcSearch = null;
		} else {
			hcSearch = null;
		}
		cachedSourceGraph = null;
		cachedTargetGraph = null;
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
		sb.append("    hcsearch ");
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
