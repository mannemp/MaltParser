package org.maltparser.parser;

import org.maltparser.core.config.Configuration;
import org.maltparser.core.config.ConfigurationRegistry;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;
/**
 * @author Johan Hall
 *
 */
public interface DependencyParserConfig extends Configuration {
	public static final int LEARN = 0;
	public static final int PARSE = 1;
	
	public static final int PLEARN = 2;
	public static final int PRUNE = 3;
	public static final int PEVAL = 7;
	
	public static final int SLEARN = 4;
	public static final int SCORE= 5;
	public static final int PRUNEANDSCORE= 6;
	
	public static final int RANK = 10;
	
	public void parse(DependencyStructure graph) throws MaltChainedException;
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException;
	public ClassifierGuide getGuide();
	public Algorithm getAlgorithm();
	public PropagationManager getPropagationManager();
	public void addRegistry(Class<?> clazz, Object o);
	public ConfigurationRegistry getRegistry();
	public int getMode();
}
