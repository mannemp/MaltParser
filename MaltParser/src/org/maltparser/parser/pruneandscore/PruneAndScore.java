package org.maltparser.parser.pruneandscore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.maltparser.MaltConsoleEngine;
import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.config.ConfigurationException;
import org.maltparser.core.config.ConfigurationRegistry;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.SystemLogger;
import org.maltparser.core.helper.URLFinder;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.Algorithm;
import org.maltparser.parser.BatchTrainer;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.DeterministicParser;
import org.maltparser.parser.Parser;
import org.maltparser.parser.ParserState;
import org.maltparser.parser.SingleMalt;
import org.maltparser.parser.Trainer;
import org.maltparser.parser.algorithm.nivre.NivreConfig;
import org.maltparser.parser.evaluation.Evaluate;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.OracleGuide;
import org.maltparser.parser.history.action.ComplexDecisionAction;

/**
 * @author Prashanth Mannem
 *
 */
public class PruneAndScore implements DependencyParserConfig {
	
	protected ConfigurationDir configDir;
	protected Logger configLogger;
	protected int optionContainerIndex;
	protected Algorithm parsingAlgorithm = null;
	protected int mode;
	protected ConfigurationRegistry registry;
	protected SymbolTableHandler symbolTableHandler;
	protected DataFormatInstance dataFormatInstance;
	protected long startTime;
	protected long endTime;
	protected int nIterations = 0;
	protected PropagationManager propagationManager;
	private Parser parser;
	private Trainer trainer;
	private OracleGuide oracleGuide;
//	private Pruner pruner;
	protected int pruneTopK;
	protected int pruneCV;
	protected int exploreAfterIter;
	protected double exploreProb;
	protected int currentSentNo;
	protected int currentIterNo;
	protected SingleMalt dynamicOracle;
	public Evaluate evaluator;
	public DependencyStructure goldGraph;
	
	public void initialize(int containerIndex, DataFormatInstance dataFormatInstance, ConfigurationDir configDir, int mode) throws MaltChainedException {

		this.optionContainerIndex = containerIndex;
		this.mode = mode;
		setConfigurationDir(configDir);
		startTime = System.currentTimeMillis();
		configLogger = initConfigLogger(getOptionValue("config", "logfile").toString(), getOptionValue("config", "logging").toString());
		registry = new ConfigurationRegistry();
		this.dataFormatInstance = dataFormatInstance;
		symbolTableHandler = dataFormatInstance.getSymbolTables();
		
//		if (OptionManager.instance().getOptionValueNoDefault(MaltConsoleEngine.OPTION_CONTAINER, "pruneandscore", "pasprunek") != null) {
		if(getOptionValue("pruneandscore","pasprunek")!=null){
			String pk = getOptionValue("pruneandscore", "pasprunek").toString();
			pruneTopK = Integer.parseInt(pk);
		}
		else
			pruneTopK = Integer.MAX_VALUE;
		if(getOptionValue("pruneandscore","pasprunecv")!=null){
			String pk = getOptionValue("pruneandscore", "pasprunecv").toString();
			pruneCV = Integer.parseInt(pk);
		}
		else
			pruneCV = 1;
		if(getOptionValue("pruneandscore","pasexpiter")!=null){
			String pk = getOptionValue("pruneandscore", "pasexpiter").toString();
			exploreAfterIter = Integer.parseInt(pk);
		}
		else
			exploreAfterIter = Integer.MAX_VALUE;
		if(getOptionValue("pruneandscore","pasexpprob")!=null){
			String pk = getOptionValue("pruneandscore", "pasexpprob").toString();
			exploreProb = Double.parseDouble(pk);
		}
		else
			exploreProb = 0.0;
		if (mode == PLEARN || mode == LEARN) {
			checkOptionDependency();
		}
		currentSentNo = -1;
		currentIterNo = 1;
		registry.put(org.maltparser.core.symbol.SymbolTableHandler.class, getSymbolTables());
		registry.put(org.maltparser.core.io.dataformat.DataFormatInstance.class, dataFormatInstance);
//		registry.put(org.maltparser.parser.DependencyParserConfig.class, this);
		initPropagation();
		initParsingAlgorithm(); 
		if (configLogger.isInfoEnabled()) {
			URL inputFormatURL = configDir.getInputFormatURL(); 
			URL outputFormatURL = configDir.getOutputFormatURL();
			if (inputFormatURL != null) {
				if (outputFormatURL == null || outputFormatURL.toString().equals(inputFormatURL.toString())) {
					int index = inputFormatURL.toString().indexOf('!');
					if (index == -1) {
						configLogger.info("  Data Format          : "+inputFormatURL.toString()+"\n");
					} else {
						configLogger.info("  Data Format          : "+inputFormatURL.toString().substring(index+1)+"\n");
					}
				} else {
					int indexIn = inputFormatURL.toString().indexOf('!');
					int indexOut = outputFormatURL.toString().indexOf('!');
					if (indexIn == -1) {
						configLogger.info("  Input Data Format    : "+inputFormatURL.toString()+"\n");
					} else {
						configLogger.info("  Input Data Format    : "+inputFormatURL.toString().substring(indexIn+1)+"\n");
					}
					if (indexOut == -1) {
						configLogger.info("  Output Data Format   : "+outputFormatURL.toString()+"\n");
					} else {
						configLogger.info("  Output Data Format   : "+outputFormatURL.toString().substring(indexOut+1)+"\n");
					}
				}
			}
		}
		evaluator = new Evaluate(dataFormatInstance);
	}
	
	private void initPropagation()  throws MaltChainedException {
		String propagationSpecFileName = getOptionValue("pruneandscore", "propagation").toString();
		if (propagationSpecFileName == null || propagationSpecFileName.length() == 0) {
			return;
		}
		propagationManager = new PropagationManager(configDir);
		if (isInLearnMode()) {
			propagationSpecFileName = configDir.copyToConfig(propagationSpecFileName);
			OptionManager.instance().overloadOptionValue(optionContainerIndex, "pruneandscore", "propagation", propagationSpecFileName);
		}
		getConfigLogger().info("  Propagation          : " + propagationSpecFileName+"\n");
		propagationManager.loadSpecification(propagationSpecFileName);
	}
	
	/**
	 * Initialize the parsing algorithm
	 * 
	 * @throws MaltChainedException
	 */
	protected void initParsingAlgorithm() throws MaltChainedException {
		if (isInLearnMode()) {
			parsingAlgorithm = trainer = new BatchTrainer(this);
		} else if (isInInferenceMode()) {
			parsingAlgorithm = parser = new DeterministicParser(this);
		}
	}
	
	public boolean isInLearnMode()
	{
		if (mode == SLEARN || mode == PLEARN || mode == LEARN || mode == PEVAL)
			return true;
		else
			return false;
	}

	public boolean isInInferenceMode()
	{
		if (mode == PARSE || mode == PRUNE || mode == SCORE || mode == PRUNEANDSCORE)
			return true;
		else
			return false;
	}
	
	public void addRegistry(Class<?> clazz, Object o) {
		registry.put(clazz, o);
	}
	
	public void process(Object[] arguments) throws MaltChainedException {
		if (isInLearnMode()) {
			if (arguments.length < 2 || !(arguments[0] instanceof DependencyStructure) || !(arguments[1] instanceof DependencyStructure)) {
				throw new MaltChainedException("The pruneandscore learn task must be supplied with at least two dependency structures. ");
			}
			DependencyStructure systemGraph = (DependencyStructure)arguments[0]; // target
			goldGraph = (DependencyStructure)arguments[1]; // source
			if(getAlgorithm().getParserState().getConfiguration() instanceof NivreConfig)
				((NivreConfig) getAlgorithm().getParserState().getConfiguration()).setGoldArcs(goldGraph);
			if (systemGraph.hasTokens() && getGuide() != null) {
				getGuide().finalizeSentence(((Trainer)getAlgorithm()).pasParse(goldGraph, systemGraph));
			}
			int curIter = (1 << 9) -1 ;
			curIter = (goldGraph.getSentenceID() & curIter) ;
//			if(getMode() == PEVAL || curIter%1 == 0)
			evaluator.evaluate(goldGraph, systemGraph);
		} else if (isInInferenceMode()) {
			if (arguments.length < 1 || !(arguments[0] instanceof DependencyStructure)) {
				throw new MaltChainedException("The pruneandscore parse task must be supplied with at least one input terminal structure and one output dependency structure. ");
			}
			DependencyStructure processGraph = (DependencyStructure)arguments[0];
			if (arguments.length > 1 && (arguments[1] instanceof DependencyStructure)) {
				goldGraph = (DependencyStructure)arguments[1];
				if(parser.getParserState().getConfiguration() instanceof NivreConfig)
					((NivreConfig) parser.getParserState().getConfiguration()).setGoldArcs(goldGraph);
			}
			if (processGraph.hasTokens()) {
				parser.pasParse(processGraph);
//				((Parser)getAlgorithm()).parse(processGraph);
			}
			if (arguments.length > 1 && (arguments[1] instanceof DependencyStructure)) {
				DependencyStructure goldGraph = (DependencyStructure)arguments[1];
				evaluator.evaluate(goldGraph, processGraph);
			}
		}
	}
	
	public void postProcess()
	{
		if (isInInferenceMode() || mode == PEVAL)
			evaluator.printMetrics();
		else
		{
			System.out.println("ITERATION:"+(currentIterNo));
			evaluator.printMetrics();
			evaluator.saveMetrics(currentIterNo);
			evaluator.printHashMetrics();
			evaluator.reset();
		}
	}
	
	public void parse(DependencyStructure graph) throws MaltChainedException {
		if (graph.hasTokens()) {
//			((Parser)getAlgorithm()).parse(graph);
			parser.parse(graph);
		}
	}
	
	public void parseGold(DependencyStructure graph, DependencyStructure sysGraph) throws MaltChainedException {
		if (graph.hasTokens()) {
//			((Parser)getAlgorithm()).parse(graph);
			parser.oracleParse(graph,sysGraph);
		}
	}
	
	public DependencyStructure parse( DependencyStructure sysGraph, ArrayList<Integer> partialActionSeq) throws MaltChainedException {
		if (sysGraph.hasTokens()) {
//			((Parser)getAlgorithm()).parse(graph);
			return parser.parse(sysGraph, partialActionSeq);
		}
		return sysGraph;
	}
	
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException {
		if (oracleGraph.hasTokens()) {
			if (getGuide() != null) {
				getGuide().finalizeSentence(trainer.parse(goldGraph, oracleGraph));
			} else {
				trainer.parse(goldGraph, oracleGraph);
			}
		}
	}
	
	public void pasOracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException {
		if (oracleGraph.hasTokens()) {
			if (getGuide() != null) {
				getGuide().finalizeSentence(trainer.pasParse(goldGraph, oracleGraph));
			} else {
				trainer.pasParse(goldGraph, oracleGraph);
			}
		}
	}
	
	public void train() throws MaltChainedException {
		if (getGuide() == null) {
			((Trainer)getAlgorithm()).train();
		}
	}
	
	public void terminate(Object[] arguments) throws MaltChainedException {
//		if (getAlgorithm() instanceof Trainer) {
//			((Trainer)getAlgorithm()).terminate();
//		}
		getAlgorithm().terminate();
		if (getGuide() != null) {
			getGuide().terminate();
		}
		if (isInLearnMode()) {
			endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			if (configLogger.isInfoEnabled()) {
				configLogger.info("Learning time: " +new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)\n");
			}
		} else if (isInInferenceMode()) {
			endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			if (configLogger.isInfoEnabled()) {
				configLogger.info("Parsing time: " +new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)\n");
			}
		}
		if (SystemLogger.logger() != configLogger && configLogger != null) {
			configLogger.removeAllAppenders();
		}
	}
	
	/**
	 * Initialize the configuration logger
	 * 
	 * @return the configuration logger
	 * @throws MaltChainedException
	 */
	public Logger initConfigLogger(String logfile, String level) throws MaltChainedException {
		if (logfile != null && logfile.length() > 0 && !logfile.equalsIgnoreCase("stdout") && configDir != null) {
			configLogger = Logger.getLogger(logfile);
			FileAppender fileAppender = null;
			try {
				fileAppender = new FileAppender(new PatternLayout("%m"),configDir.getWorkingDirectory().getPath()+File.separator+logfile, true);
			} catch(IOException e) {
				throw new ConfigurationException("It is not possible to create a configuration log file. ", e);
			}
			fileAppender.setThreshold(Level.toLevel(level, Level.INFO));
			configLogger.addAppender(fileAppender);
			configLogger.setLevel(Level.toLevel(level, Level.INFO));	
		} else {
			configLogger = SystemLogger.logger();
		}

		return configLogger;
	}
	
	public Logger getConfigLogger() {
		return configLogger;
	}

	public void setConfigLogger(Logger logger) {
		configLogger = logger;
	}
	
	/*public Pruner getPruner()
	{
		return pruner;
	}*/
	
	public int getPruneTopK()
	{
		return pruneTopK;
	}
	
	public int getPruneCV()
	{
		return pruneCV;
	}
	public int getExploreAfterIter()
	{
		return exploreAfterIter;
	}
	
	public double getExploreProb()
	{
		return exploreProb;
	}
	public int getCurrentSentNo()
	{
		return currentSentNo;
	}
	
	public int getCurrentIterNo()
	{
		return currentIterNo;
	}
	
	public ConfigurationDir getConfigurationDir() {
		return configDir;
	}
	
	public void setConfigurationDir(ConfigurationDir configDir) {
		this.configDir = configDir;
	}
	
	public int getMode() {
		return mode;
	}
	
	public ConfigurationRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(ConfigurationRegistry registry) {
		this.registry = registry;
	}

	public Object getOptionValue(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValue(optionContainerIndex, optiongroup, optionname);
	}
	
	public String getOptionValueString(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValueString(optionContainerIndex, optiongroup, optionname);
	}
	
	public OptionManager getOptionManager() throws MaltChainedException {
		return OptionManager.instance();
	}
	/******************************** MaltParserConfiguration specific  ********************************/
	
	/**
	 * Returns the list of symbol tables
	 * 
	 * @return the list of symbol tables
	 */
	public SymbolTableHandler getSymbolTables() {
		return symbolTableHandler;
	}
	
	public PropagationManager getPropagationManager() {
		return propagationManager;
	}

	public Algorithm getAlgorithm() {
		return parsingAlgorithm;
	}
	/**
	 * Returns the guide
	 * 
	 * @return the guide
	 */
	public ClassifierGuide getGuide() {
		return parsingAlgorithm.getGuide();
	}
	
	public void checkOptionDependency() throws MaltChainedException {
		try {
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().write("\nDEPENDENCIES\n");
			}
			
			// Copy the feature model file into the configuration directory
			String featureModelFileName = getOptionValue("guide", "features").toString().trim();
			if (featureModelFileName.equals("")) {
				// use default feature model depending on the selected parser algorithm
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "features", getOptionValueString("pruneandscore", "parsing_algorithm"));
				featureModelFileName = getOptionValue("guide", "features").toString().trim();
				/* START: Temp fix during development of new liblinear and libsvm interface */
				String learner = getOptionValueString("guide", "learner");
				if (!learner.startsWith("lib")) {
					learner = "lib"+learner;
				}
				/* END: Temp fix during development of new liblinear and libsvm interface */
				featureModelFileName = featureModelFileName.replace("{learner}", learner);
				final URLFinder f = new URLFinder();
				featureModelFileName = configDir.copyToConfig(f.findURLinJars(featureModelFileName));
			} else {
				featureModelFileName = configDir.copyToConfig(featureModelFileName);
			}
			OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "features", featureModelFileName);
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().write("--guide-features (  -F)                 "+getOptionValue("guide", "features").toString()+"\n");
			}

			if (getOptionValue("guide", "data_split_column").toString().equals("") && !getOptionValue("guide", "data_split_structure").toString().equals("")) {
				configLogger.warn("Option --guide-data_split_column = '' and --guide-data_split_structure != ''. Option --guide-data_split_structure is overloaded with '', this will cause the parser to induce a single model.\n ");
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "data_split_structure", "");
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-data_split_structure (  -s)\n");
				}
			}
			if (!getOptionValue("guide", "data_split_column").toString().equals("") && getOptionValue("guide", "data_split_structure").toString().equals("")) {
				configLogger.warn("Option --guide-data_split_column != '' and --guide-data_split_structure = ''. Option --guide-data_split_column is overloaded with '', this will cause the parser to induce a single model.\n");
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "data_split_column", "");
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-data_split_column (  -d)\n");
				}
			}
			
			String decisionSettings = getOptionValue("guide", "decision_settings").toString().trim();
			String markingStrategy = getOptionValue("pproj", "marking_strategy").toString().trim();
			String coveredRoot = getOptionValue("pproj", "covered_root").toString().trim();
			StringBuilder newDecisionSettings = new StringBuilder();

			if (decisionSettings == null || decisionSettings.length() < 1 || decisionSettings.equals("default")) {
				decisionSettings = "T.TRANS+A.DEPREL";
			} else {
				decisionSettings = decisionSettings.toUpperCase();
			}
			
			if (markingStrategy.equalsIgnoreCase("head") || markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				if (!Pattern.matches(".*A\\.PPLIFTED.*", decisionSettings)) {
					newDecisionSettings.append("+A.PPLIFTED");
				}
			}
			if (markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				if (!Pattern.matches(".*A\\.PPPATH.*", decisionSettings)) {
					newDecisionSettings.append("+A.PPPATH");
				}
			}
			if (!coveredRoot.equalsIgnoreCase("none") && !Pattern.matches(".*A\\.PPCOVERED.*", decisionSettings)) {
				newDecisionSettings.append("+A.PPCOVERED");
			}
			if (!getOptionValue("guide", "decision_settings").toString().equals(decisionSettings) || newDecisionSettings.length() > 0) {
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "decision_settings", decisionSettings+newDecisionSettings.toString());
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-decision_settings (  -gds)                 "+getOptionValue("guide", "decision_settings").toString()+"\n");
				}
			}
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().flush();
			}
		} catch (IOException e) {
			throw new ConfigurationException("Could not write to the configuration information file. ", e);
		}
	}
}
