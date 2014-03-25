package org.maltparser.parser.pruneandscore;

import java.util.ArrayList;
import java.util.Vector;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.core.syntaxgraph.node.TokenNode;
import org.maltparser.parser.AbstractParserFactory;
import org.maltparser.parser.Algorithm;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.TransitionSystem;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.HistoryList;
import org.maltparser.parser.history.HistoryStructure;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.kbest.ScoredCandidate;
/**
 * @author Johan Hall
 *
 */
public class HCSearchState {
	private HCSearch manager;
	private DependencyGraph graph;
	private DependencyGraph parentGraph;
	private ArrayList<HCSearchState> childStates;
	private HCSearchState parentState;
	private ArrayList<ScoredCandidate> actionSequence;
	private int discrepancyPosition;
	private int discrepantActionCode;
	private String discrepantActionSymbol;
	private double heuristicScore = 0;
	private double costFunctionScore = 0; 
	private int loss;
	private double accuracy;
	private int depth;
    private Vector<Integer> featureVector;

	
	public HCSearchState(HCSearch m, DependencyGraph g, ArrayList<ScoredCandidate> a)
	{
		manager = m;
		graph = g;
		setActionSequence(a);
		parentGraph = null;
		childStates = new ArrayList<HCSearchState>(0);
		parentState = null;
		discrepancyPosition = 0;
		discrepantActionCode = 0;
		discrepantActionSymbol = "NIL";
		depth = 0;
		featureVector = new Vector<Integer>();
	}
	
	public HCSearchState(HCSearch m, DependencyGraph g, ArrayList<ScoredCandidate> a, int disPos, 
			int disAction)
	{
		this(m,g,a);
		parentGraph = null;
		childStates = new ArrayList<HCSearchState>(0);
		parentState = null;
		discrepancyPosition = disPos;
		discrepantActionCode = disAction;
		loss = 0;
		accuracy = 0;
		try{
			if(manager.inLearnMode())
			{
				loss = g.getLoss(manager.goldGraph);
				accuracy = (double)loss/g.nDependencyNode();
			}
			featureVector = generateFeatures();
			heuristicScore = manager.featW.getHScore(featureVector);
			costFunctionScore = manager.featW.getCScore(featureVector);
		}
		catch(MaltChainedException e)
		{
			e.printStackTrace();
		}
	}
	
	public void updateHScore()
	{
		heuristicScore = manager.featW.getHScore(featureVector);
	}
	
	public void updateCScore()
	{
		costFunctionScore= manager.featW.getCScore(featureVector);
	}
	public Vector<Integer> getFeatureVector()
	{
		return featureVector;
	}
	
	public Vector<Integer> generateFeatures() throws MaltChainedException
	{// DONE: Generate features 
		
		SymbolTable posTable = graph.getSymbolTables().getSymbolTable("POSTAG");
		SymbolTable cposTable = graph.getSymbolTables().getSymbolTable("CPOSTAG");
		SymbolTable formTable = graph.getSymbolTables().getSymbolTable("FORM");
		SymbolTable lemmaTable = graph.getSymbolTables().getSymbolTable("LEMMA");
		
		String[] pos = new String[graph.nDependencyNode()];
		String[] posA = new String[graph.nDependencyNode()];
		String[] toks = new String[graph.nDependencyNode()];
		for(int idx : graph.getDependencyIndices())
		{
			if(idx ==0)
			{
				pos[0] = "_ROOT_";
				posA[0] = "_ROOT_";
				toks[0] = "_ROOT_";
				continue;
			}
			pos[idx] = graph.getDependencyNode(idx).getLabelSymbol(posTable);
			posA[idx] = graph.getDependencyNode(idx).getLabelSymbol(cposTable);
			toks[idx] = graph.getDependencyNode(idx).getLabelSymbol(formTable);
		}
		
		Vector<String> feats = new Vector<String>();		
		for(int idx : graph.getDependencyIndices())
		{
			if(idx == 0)
				continue;
			DependencyNode child = graph.getTokenNode(idx);
			DependencyNode head = child.getHead();
			generateFeatures(child,pos,posA,toks,feats);
		}
		Vector<Integer> ifeats = new Vector<Integer>();
		for (String f: feats)
			add(f,ifeats);
		return ifeats;
	}
	
	public void generateFeatures(DependencyNode childNode, String[] pos, String[] posA, String[] toks, Vector<String> fv) throws MaltChainedException
	{
	/*	SymbolTable idTable = graph.getSymbolTables().getSymbolTable("ID");
		SymbolTable posTable = graph.getSymbolTables().getSymbolTable("POSTAG");
		SymbolTable cposTable = graph.getSymbolTables().getSymbolTable("CPOSTAG");
		SymbolTable formTable = graph.getSymbolTables().getSymbolTable("FORM");
		SymbolTable lemmaTable = graph.getSymbolTables().getSymbolTable("LEMMA");
		SymbolTable deprelTable = graph.getSymbolTables().getSymbolTable("DEPREL");*/
//		SymbolTable actionTable = graph.getSymbolTables().getSymbolTable("T.TRANS+A.DEPREL");
		// T.TRANS+A.DEPREL, DEPREL
		/*String cid = child.getLabelSymbol(idTable);
		String cpos = child.getLabelSymbol(posTable);
		String ccpos = child.getLabelSymbol(cposTable);
		String cform = child.getLabelSymbol(formTable);
		String clemma = child.getLabelSymbol(lemmaTable);
		String cdeprel = child.getLabelSymbol(deprelTable);*/
//		String caction = childNode.getLabelSymbol(actionTable);
		
		DependencyNode headNode = childNode.getHead();
		int cidx = childNode.getIndex();
		int hidx = headNode.getIndex();
		
		String att = "";
		if(hidx < cidx) 
			att = "RA";
		else
			att = "LA";
		int dist = Math.abs(headNode.getIndex() - childNode.getIndex());
		String distBool = "0";
    	if(dist > 1)
    	    distBool = "1";
    	if(dist > 2)
    	    distBool = "2";
    	if(dist > 3)
    	    distBool = "3";
    	if(dist > 4)
    	    distBool = "4";
    	if(dist > 5)
    	    distBool = "5";
    	if(dist > 10)
    	    distBool = "10";
    	
    	String attDist = "&"+att+"&"+distBool;
    	int small = cidx < hidx ? cidx : hidx;
    	int large = cidx < hidx ? hidx : cidx;
    		

    	String pLeft = small > 0 ? pos[small-1] : "STR";
    	String pRight = large < pos.length-1 ? pos[large+1] : "END";
    	String pLeftRight = small < large-1 ? pos[small+1] : "MID";
    	String pRightLeft = large > small+1 ? pos[large-1] : "MID";
    	String pLeftA = small > 0 ? posA[small-1] : "STR";
    	String pRightA = large < pos.length-1 ? posA[large+1] : "END";
    	String pLeftRightA = small < large-1 ? posA[small+1] : "MID";
    	String pRightLeftA = large > small+1 ? posA[large-1] : "MID";
    		
    	// feature posR posMid posL
    	for(int i = small+1; i < large; i++) {
    	    String allPos = pos[small]+" "+pos[i]+" "+pos[large];
    	    String allPosA = posA[small]+" "+posA[i]+" "+posA[large];
    	    add("PC="+allPos+attDist,1.0,fv);
    	    add("1PC="+allPos,1.0,fv);
    	    add("XPC="+allPosA+attDist,1.0,fv);
    	    add("X1PC="+allPosA,1.0,fv);
    	}

    	// feature posL-1 posL posR posR+1
    	add("PT="+pLeft+" "+pos[small]+" "+pos[large]+" "+pRight+attDist,1.0,fv);
    	add("PT1="+pos[small]+" "+pos[large]+" " +pRight+attDist,1.0,fv);
    	add("PT2="+pLeft+" "+pos[small]+" "+pos[large]+attDist,1.0,fv);
    	add("PT3="+pLeft+" "+pos[large]+" "+pRight+attDist,1.0,fv);
    	add("PT4="+pLeft+" "+pos[small]+" "+pRight+attDist,1.0,fv);
    		
    	add("1PT="+pLeft+" "+pos[small]+" "+pos[large]+" "+pRight,1.0,fv);
    	add("1PT1="+pos[small]+" "+pos[large]+" " +pRight,1.0,fv);
    	add("1PT2="+pLeft+" "+pos[small]+" "+pos[large],1.0,fv);
    	add("1PT3="+pLeft+" "+pos[large]+" "+pRight,1.0,fv);
    	add("1PT4="+pLeft+" "+pos[small]+" "+pRight,1.0,fv);
    		
    	add("XPT="+pLeftA+" "+posA[small]+" "+posA[large]+" "+pRightA+attDist,1.0,fv);
    	add("XPT1="+posA[small]+" "+posA[large]+" " +pRightA+attDist,1.0,fv);
    	add("XPT2="+pLeftA+" "+posA[small]+" "+posA[large]+attDist,1.0,fv);
    	add("XPT3="+pLeftA+" "+posA[large]+" "+pRightA+attDist,1.0,fv);
    	add("XPT4="+pLeftA+" "+posA[small]+" "+pRightA+attDist,1.0,fv);
    		
    	add("X1PT="+pLeftA+" "+posA[small]+" "+posA[large]+" "+pRightA,1.0,fv);
    	add("X1PT1="+posA[small]+" "+posA[large]+" " +pRightA,1.0,fv);
    	add("X1PT2="+pLeftA+" "+posA[small]+" "+posA[large],1.0,fv);
    	add("X1PT3="+pLeftA+" "+posA[large]+" "+pRightA,1.0,fv);
    	add("X1PT4="+pLeftA+" "+posA[small]+" "+pRightA,1.0,fv);
    		
    	// feature posL posL+1 posR-1 posR
    	add("APT="+pos[small]+" "+pLeftRight+" "
    		 +pRightLeft+" "+pos[large]+attDist,1.0,fv);
    	add("APT1="+pos[small]+" "+pRightLeft+" "+pos[large]+attDist,1.0,fv);
    	add("APT2="+pos[small]+" "+pLeftRight+" "+pos[large]+attDist,1.0,fv);
    	add("APT3="+pLeftRight+" "+pRightLeft+" "+pos[large]+attDist,1.0,fv);
    	add("APT4="+pos[small]+" "+pLeftRight+" "+pRightLeft+attDist,1.0,fv);

    	add("1APT="+pos[small]+" "+pLeftRight+" "
    		 +pRightLeft+" "+pos[large],1.0,fv);
    	add("1APT1="+pos[small]+" "+pRightLeft+" "+pos[large],1.0,fv);
    	add("1APT2="+pos[small]+" "+pLeftRight+" "+pos[large],1.0,fv);
    	add("1APT3="+pLeftRight+" "+pRightLeft+" "+pos[large],1.0,fv);
    	add("1APT4="+pos[small]+" "+pLeftRight+" "+pRightLeft,1.0,fv);
    		
    	add("XAPT="+posA[small]+" "+pLeftRightA+" "
    		 +pRightLeftA+" "+posA[large]+attDist,1.0,fv);
    	add("XAPT1="+posA[small]+" "+pRightLeftA+" "+posA[large]+attDist,1.0,fv);
    	add("XAPT2="+posA[small]+" "+pLeftRightA+" "+posA[large]+attDist,1.0,fv);
    	add("XAPT3="+pLeftRightA+" "+pRightLeftA+" "+posA[large]+attDist,1.0,fv);
    	add("XAPT4="+posA[small]+" "+pLeftRightA+" "+pRightLeftA+attDist,1.0,fv);

    	add("X1APT="+posA[small]+" "+pLeftRightA+" "
    		 +pRightLeftA+" "+posA[large],1.0,fv);
    	add("X1APT1="+posA[small]+" "+pRightLeftA+" "+posA[large],1.0,fv);
    	add("X1APT2="+posA[small]+" "+pLeftRightA+" "+posA[large],1.0,fv);
    	add("X1APT3="+pLeftRightA+" "+pRightLeftA+" "+posA[large],1.0,fv);
    	add("X1APT4="+posA[small]+" "+pLeftRightA+" "+pRightLeftA,1.0,fv);
    		
    	// feature posL-1 posL posR-1 posR
    	// feature posL posL+1 posR posR+1
    	add("BPT="+pLeft+" "+pos[small]+" "+pRightLeft+" "+pos[large]+attDist,1.0,fv);
    	add("1BPT="+pLeft+" "+pos[small]+" "+pRightLeft+" "+pos[large],1.0,fv);
    	add("CPT="+pos[small]+" "+pLeftRight+" "+pos[large]+" "+pRight+attDist,1.0,fv);
    	add("1CPT="+pos[small]+" "+pLeftRight+" "+pos[large]+" "+pRight,1.0,fv);
    		
    	add("XBPT="+pLeftA+" "+posA[small]+" "+pRightLeftA+" "+posA[large]+attDist,1.0,fv);
    	add("X1BPT="+pLeftA+" "+posA[small]+" "+pRightLeftA+" "+posA[large],1.0,fv);
    	add("XCPT="+posA[small]+" "+pLeftRightA+" "+posA[large]+" "+pRightA+attDist,1.0,fv);
    	add("X1CPT="+posA[small]+" "+pLeftRightA+" "+posA[large]+" "+pRightA,1.0,fv);

    	String head = toks[headNode.getIndex()];
    	String headP = pos[headNode.getIndex()];
    	String child = toks[childNode.getIndex()];
    	String childP = pos[childNode.getIndex()];
    		
    	String all = head + " " + headP + " " + child + " " + childP;
    	String hPos = headP + " " + child + " " + childP;
    	String cPos = head + " " + headP + " " + childP;
    	String hP = headP + " " + child;
    	String cP = head + " " + childP;
    	String oPos = headP + " " + childP;
    	String oLex = head + " " + child;

    	add("A="+all+attDist,1.0,fv); //this
    	add("B="+hPos+attDist,1.0,fv);
    	add("C="+cPos+attDist,1.0,fv);
    	add("D="+hP+attDist,1.0,fv);
    	add("E="+cP+attDist,1.0,fv);
    	add("F="+oLex+attDist,1.0,fv); //this
    	add("G="+oPos+attDist,1.0,fv);
    	add("H="+head+" "+headP+attDist,1.0,fv);
    	add("I="+headP+attDist,1.0,fv);
    	add("J="+head+attDist,1.0,fv); //this
    	add("K="+child+" "+childP+attDist,1.0,fv);
    	add("L="+childP+attDist,1.0,fv);
    	add("M="+child+attDist,1.0,fv); //this

    	add("AA="+all,1.0,fv); //this
    	add("BB="+hPos,1.0,fv);
    	add("CC="+cPos,1.0,fv);
    	add("DD="+hP,1.0,fv);
    	add("EE="+cP,1.0,fv);
    	add("FF="+oLex,1.0,fv); //this
    	add("GG="+oPos,1.0,fv);
    	add("HH="+head+" "+headP,1.0,fv);
    	add("II="+headP,1.0,fv);
    	add("JJ="+head,1.0,fv); //this
    	add("KK="+child+" "+childP,1.0,fv);
    	add("LL="+childP,1.0,fv);
    	add("MM="+child,1.0,fv); //this

    	if(head.length() > 5 || child.length() > 5) {
    	    int hL = head.length();
    	    int cL = child.length();
    		    
    	    head = hL > 5 ? head.substring(0,5) : head;
    	    child = cL > 5 ? child.substring(0,5) : child;
    		    
    	    all = head + " " + headP + " " + child + " " + childP;
    	    hPos = headP + " " + child + " " + childP;
    	    cPos = head + " " + headP + " " + childP;
    	    hP = headP + " " + child;
    	    cP = head + " " + childP;
    	    oPos = headP + " " + childP;
    	    oLex = head + " " + child;
    	
    	    add("SA="+all+attDist,1.0,fv); //this
    	    add("SF="+oLex+attDist,1.0,fv); //this
    	    add("SAA="+all,1.0,fv); //this
    	    add("SFF="+oLex,1.0,fv); //this

    	    if(cL > 5) {
    		add("SB="+hPos+attDist,1.0,fv);
    		add("SD="+hP+attDist,1.0,fv);
    		add("SK="+child+" "+childP+attDist,1.0,fv);
    		add("SM="+child+attDist,1.0,fv); //this
    		add("SBB="+hPos,1.0,fv);
    		add("SDD="+hP,1.0,fv);
    		add("SKK="+child+" "+childP,1.0,fv);
    		add("SMM="+child,1.0,fv); //this
    	    }
    	    if(hL > 5) {
    		add("SC="+cPos+attDist,1.0,fv);
    		add("SE="+cP+attDist,1.0,fv);
    		add("SH="+head+" "+headP+attDist,1.0,fv);
    		add("SJ="+head+attDist,1.0,fv); //this
    			
    		add("SCC="+cPos,1.0,fv);
    		add("SEE="+cP,1.0,fv);
    		add("SHH="+head+" "+headP,1.0,fv);
    		add("SJJ="+head,1.0,fv); //this
    	    }
    	}
		return;
	}
	
	//Replicating the MST parser features for Global (maybe local too) TPartTree
    /*public void getMSTFeatures(DependencyNode head, DependencyNode child, boolean dir, Vector<Integer> fv)
    {
    	String att = "";
    	int dist = Math.abs(head.getIndex() - child.getIndex());
    	String distBool = "0";
    	if(dist > 1)
    	    distBool = "1";
    	if(dist > 2)
    	    distBool = "2";
    	if(dist > 3)
    	    distBool = "3";
    	if(dist > 4)
    	    distBool = "4";
    	if(dist > 5)
    	    distBool = "5";
    	if(dist > 10)
    	    distBool = "10";
    		
    	//if(Integer.parseInt(distBool) == 0)
    	//	System.out.println("dist = 0");
    	
    	String spos = head..toString();
    	String lpos = sent.goldpos.get(large).toString();
    	String sposA = sent.goldpos.get(small).aString();
    	String lposA = sent.goldpos.get(large).aString();
    	
    	String attDist = "&"+att+"&"+distBool;
    	String pLeft = small > 0 ? sent.goldpos.get(small-1).toString() : "STR";
    	String pRight = large < sent.goldpos.size()-1 ? sent.goldpos.get(large+1).toString() : "END";
    	String pLeftRight = small < large-1 ? sent.goldpos.get(small+1).toString() : "MID";
    	String pRightLeft = large > small+1 ? sent.goldpos.get(large-1).toString() : "MID";
    	String pLeftA = small > 0 ? sent.goldpos.get(small-1).aString() : "STR";
    	String pRightA = large < sent.goldpos.size()-1 ? sent.goldpos.get(large+1).aString() : "END";
    	String pLeftRightA = small < large-1 ? sent.goldpos.get(small+1).aString() : "MID";
    	String pRightLeftA = large > small+1 ? sent.goldpos.get(large-1).aString() : "MID";
    		
    	
    	// feature posR posMid posL
    	for(int i = small+1; i < large; i++) {
    	    String allPos = spos+" "+sent.goldpos.get(i).toString()+" "+lpos;
    	    String allPosA = sposA+" "+sent.goldpos.get(i).aString()+" "+lposA;
    	    add("PC="+allPos+attDist,fv);
    	    add("1PC="+allPos,fv);
    	    add("XPC="+allPosA+attDist,fv);
    	    add("X1PC="+allPosA,fv);
    	}
    	 
    	// feature posL-1 posL posR posR+1
    	add("PT="+pLeft+" "+spos+" "+lpos+" "+pRight+attDist,fv);
    	add("PT1="+spos+" "+lpos+" " +pRight+attDist,fv);
    	add("PT2="+pLeft+" "+spos+" "+lpos+attDist,fv);
    	add("PT3="+pLeft+" "+lpos+" "+pRight+attDist,fv); //r
    	add("PT4="+pLeft+" "+spos+" "+pRight+attDist,fv); //r
    		
    	add("1PT="+pLeft+" "+spos+" "+lpos+" "+pRight,fv);
    	add("1PT1="+spos+" "+lpos+" " +pRight,fv);
    	add("1PT2="+pLeft+" "+spos+" "+lpos,fv);
    	add("1PT3="+pLeft+" "+lpos+" "+pRight,fv); //r
    	add("1PT4="+pLeft+" "+spos+" "+pRight,fv); //r
    		
    	add("XPT="+pLeftA+" "+sposA+" "+lposA+" "+pRightA+attDist,fv);
    	add("XPT1="+sposA+" "+lposA+" " +pRightA+attDist,fv);
    	add("XPT2="+pLeftA+" "+sposA+" "+lposA+attDist,fv);
    	add("XPT3="+pLeftA+" "+lposA+" "+pRightA+attDist,fv);
    	add("XPT4="+pLeftA+" "+sposA+" "+pRightA+attDist,fv);
    		
    	add("X1PT="+pLeftA+" "+sposA+" "+lposA+" "+pRightA,fv);
    	add("X1PT1="+sposA+" "+lposA+" " +pRightA,fv);
    	add("X1PT2="+pLeftA+" "+sposA+" "+lposA,fv);
    	add("X1PT3="+pLeftA+" "+lposA+" "+pRightA,fv);
    	add("X1PT4="+pLeftA+" "+sposA+" "+pRightA,fv);
    		
    	// feature posL posL+1 posR-1 posR
    	add("APT="+spos+" "+pLeftRight+" "
    		 +pRightLeft+" "+lpos+attDist,fv);
    	add("APT1="+spos+" "+pRightLeft+" "+lpos+attDist,fv);
    	add("APT2="+spos+" "+pLeftRight+" "+lpos+attDist,fv);
    	add("APT3="+pLeftRight+" "+pRightLeft+" "+lpos+attDist,fv); //r
    	add("APT4="+spos+" "+pLeftRight+" "+pRightLeft+attDist,fv); //r

    	add("1APT="+spos+" "+pLeftRight+" "
    		 +pRightLeft+" "+lpos,fv);
    	add("1APT1="+spos+" "+pRightLeft+" "+lpos,fv);
    	add("1APT2="+spos+" "+pLeftRight+" "+lpos,fv);
    	add("1APT3="+pLeftRight+" "+pRightLeft+" "+lpos,fv); //r
    	add("1APT4="+spos+" "+pLeftRight+" "+pRightLeft,fv); //r
    		
    	add("XAPT="+sposA+" "+pLeftRightA+" "
    		 +pRightLeftA+" "+lposA+attDist,fv);
    	add("XAPT1="+sposA+" "+pRightLeftA+" "+lposA+attDist,fv);
    	add("XAPT2="+sposA+" "+pLeftRightA+" "+lposA+attDist,fv);
    	add("XAPT3="+pLeftRightA+" "+pRightLeftA+" "+lposA+attDist,fv);
    	add("XAPT4="+sposA+" "+pLeftRightA+" "+pRightLeftA+attDist,fv);

    	add("X1APT="+sposA+" "+pLeftRightA+" "
    		 +pRightLeftA+" "+lposA,fv);
    	add("X1APT1="+sposA+" "+pRightLeftA+" "+lposA,fv);
    	add("X1APT2="+sposA+" "+pLeftRightA+" "+lposA,fv);
    	add("X1APT3="+pLeftRightA+" "+pRightLeftA+" "+lposA,fv);
    	add("X1APT4="+sposA+" "+pLeftRightA+" "+pRightLeftA,fv);
    		
    	// feature posL-1 posL posR-1 posR
    	// feature posL posL+1 posR posR+1
    	add("BPT="+pLeft+" "+spos+" "+pRightLeft+" "+lpos+attDist,fv);
    	add("1BPT="+pLeft+" "+spos+" "+pRightLeft+" "+lpos,fv);
    	add("CPT="+spos+" "+pLeftRight+" "+lpos+" "+pRight+attDist,fv);
    	add("1CPT="+spos+" "+pLeftRight+" "+lpos+" "+pRight,fv);
    		
    	add("XBPT="+pLeftA+" "+sposA+" "+pRightLeftA+" "+lposA+attDist,fv);
    	add("X1BPT="+pLeftA+" "+sposA+" "+pRightLeftA+" "+lposA,fv);
    	add("XCPT="+sposA+" "+pLeftRightA+" "+lposA+" "+pRightA+attDist,fv);
    	add("X1CPT="+sposA+" "+pLeftRightA+" "+lposA+" "+pRightA,fv);

    	String head = head.anchorWord.toString();
    	String headP = head.label.toString();
    	String child = child.anchorWord.toString();
    	String childP = child.label.toString();
    		
    	String all = head + " " + headP + " " + child + " " + childP;
    	String hPos = headP + " " + child + " " + childP;
    	String cPos = head + " " + headP + " " + childP;
    	String hP = headP + " " + child;
    	String cP = head + " " + childP;
    	String oPos = headP + " " + childP;
    	String oLex = head + " " + child;

    	add("A="+all+attDist,fv); //this
    	add("B="+hPos+attDist,fv);
    	add("C="+cPos+attDist,fv);
    	add("D="+hP+attDist,fv);
    	add("E="+cP+attDist,fv);
    	add("F="+oLex+attDist,fv); //this
    	add("G="+oPos+attDist,fv);
    	add("H="+head+" "+headP+attDist,fv);
    	add("I="+headP+attDist,fv);
    	add("J="+head+attDist,fv); //this
    	add("K="+child+" "+childP+attDist,fv);
    	add("L="+childP+attDist,fv);
    	add("M="+child+attDist,fv); //this

    	add("AA="+all,fv); //this
    	add("BB="+hPos,fv);
    	add("CC="+cPos,fv);
    	add("DD="+hP,fv);
    	add("EE="+cP,fv);
    	add("FF="+oLex,fv); //this
    	add("GG="+oPos,fv);
    	add("HH="+head+" "+headP,fv);
    	add("II="+headP,fv);
    	add("JJ="+head,fv); //this
    	add("KK="+child+" "+childP,fv);
    	add("LL="+childP,fv);
    	add("MM="+child,fv); //this

    	if(head.length() > 5 || child.length() > 5) {
    	    int hL = head.length();
    	    int cL = child.length();
    		    
    	    head = hL > 5 ? head.substring(0,5) : head;
    	    child = cL > 5 ? child.substring(0,5) : child;
    		    
    	    all = head + " " + headP + " " + child + " " + childP;
    	    hPos = headP + " " + child + " " + childP;
    	    cPos = head + " " + headP + " " + childP;
    	    hP = headP + " " + child;
    	    cP = head + " " + childP;
    	    oPos = headP + " " + childP;
    	    oLex = head + " " + child;
    	
    	    add("SA="+all+attDist,fv); //this
    	    add("SF="+oLex+attDist,fv); //this
    	    add("SAA="+all,fv); //this
    	    add("SFF="+oLex,fv); //this

    	    if(cL > 5) {
    		add("SB="+hPos+attDist,fv);
    		add("SD="+hP+attDist,fv);
    		add("SK="+child+" "+childP+attDist,fv);
    		add("SM="+child+attDist,fv); //this
    		add("SBB="+hPos,fv);
    		add("SDD="+hP,fv);
    		add("SKK="+child+" "+childP,fv);
    		add("SMM="+child,fv); //this
    	    }
    	    if(hL > 5) {
    		add("SC="+cPos+attDist,fv);
    		add("SE="+cP+attDist,fv);
    		add("SH="+head+" "+headP+attDist,fv);
    		add("SJ="+head+attDist,fv); //this
    			
    		add("SCC="+cPos,fv);
    		add("SEE="+cP,fv);
    		add("SHH="+head+" "+headP,fv);
    		add("SJJ="+head,fv); //this
    	    }
    	}
    }

    */
    /*
public void getGlobalFeatures(Vector<Integer> feats, TPartTree pt)
    {
    	
    	String curpos = pt.label.toString();
    	String curlex = pt.anchorWord.toString();

    	// as a grandparent
    	//TGraph.add("ggp1:"+curpos,feats);
    	for (int i = pt.leftChild.size() -1  ; i > -1 ; i--)
    	{
    		// Each child of "pt" being a parent and "pt" being a grand parent..
    		String curlcpos = pt.leftChild.get(i).label.toString();
    		//String curlclex = pt.leftChild.get(i).anchorWord.toString();
    		for (int j = pt.leftChild.get(i).leftChild.size() -1  ; j > -1 ; j--)
        	{
    			// Each grand child of "pt" being a child.
    			String curlclcpos = pt.leftChild.get(i).leftChild.get(j).label.toString();
    			//String curlclclex = pt.leftChild.get(i).leftChild.get(j).anchorWord.toString();
    			
    			Grand parent with parent and child features
        		add("ggp:"+curpos+"#"+curlcpos+"#"+curlclcpos,feats);
        		add("ggp:"+curlex+"#"+curlcpos+"#"+curlclcpos,feats);
        	}
    		for (int j = pt.leftChild.get(i).rightChild.size() -1  ; j > -1 ; j--)
        	{
    			// Each grand child of "pt" being a child.
        		String curlcrcpos = pt.leftChild.get(i).rightChild.get(j).label.toString();
    			
        		 Grand parent with parent and child features
        		add("ggp:"+curpos+"#"+curlcpos+"#"+curlcrcpos,feats);
        		add("ggp:"+curlex+"#"+curlcpos+"#"+curlcrcpos,feats);
        	}
    	}
    	for (int i = pt.rightChild.size() -1  ; i > -1 ; i--)
    	{
    		// Each child of "pt" being a parent and "pt" being a grand parent.
    		String currcpos = pt.rightChild.get(i).label.toString();
    		//String curlclex = pt.leftChild.get(i).anchorWord.toString();
    		for (int j = pt.rightChild.get(i).leftChild.size() -1  ; j > -1 ; j--)
        	{
    			// Each grand child of "pt" being a child.
    			String currclcpos = pt.rightChild.get(i).leftChild.get(j).label.toString();
    			//String curlclclex = pt.leftChild.get(i).leftChild.get(j).anchorWord.toString();
    			
    			 Grand parent with parent and child features 
        		add("Ggp1:"+curpos+"#"+currcpos+"#"+currclcpos,feats);
        		add("Ggp2:"+curlex+"#"+currcpos+"#"+currclcpos,feats);
        	}
    		for (int j = pt.rightChild.get(i).rightChild.size() -1  ; j > -1 ; j--)
        	{
    			// Each child of pt being a grand child of "pt" being a child.
        		String currcrcpos = pt.rightChild.get(i).rightChild.get(j).label.toString();

    			 Grand parent with parent and child features 
        		add("Ggp1:"+curpos+"#"+currcpos+"#"+currcrcpos,feats);
        		add("Ggp2:"+curlex+"#"+currcpos+"#"+currcrcpos,feats);
        	}
    	}
    	
    	Check for nulls at each TPartTree
    	//as a parent 
    	for (int i = pt.leftChild.size() -1  ; i > -1 ; i--)
    	{
    		// Each child of "pt" being a child and "pt" being a parent.
 
    		//  sibling , parent and child features
    		String curlcpos = pt.leftChild.get(i).label.toString();
    		String curlclex = pt.leftChild.get(i).anchorWord.toString();
    		
    		String curlslcpos = i+1 >= pt.leftChild.size() ? "NIL" : pt.leftChild.get(i+1).label.toString();
    		String curlslclex = i+1 >= pt.leftChild.size() ? "NIL" : pt.leftChild.get(i+1).label.toString();
    		
    		String currslcpos = i-1 <= -1 ? ( pt.rightChild.size() <= 0 ? "NIL" : pt.rightChild.firstElement().label.toString() ): pt.leftChild.get(i-1).label.toString();
    		String currslclex = i-1 <= -1 ? ( pt.rightChild.size() <= 0 ? "NIL" : pt.rightChild.firstElement().anchorWord.toString() ): pt.leftChild.get(i-1).anchorWord.toString();
    		
    		 child and parent features with pos n lex
    		add("Gp1:"+curpos+"#"+curlcpos,feats);
    		add("Gp2:"+curpos+"#"+curlcpos+"#"+curlclex,feats);
    		
    		left sibling with parent and child
    		add("Gls1:"+curpos+"#"+curlcpos+"#"+curlslcpos,feats);
    		add("Gls2:"+curpos+"#"+curlcpos+"#"+curlslclex,feats);
    		
    		right sibling with parent and child
    		add("Grs1:"+curpos+"#"+curlcpos+"#"+currslclex,feats);
    		add("Grs2:"+curpos+"#"+curlcpos+"#"+currslcpos,feats);
    		
    		// Grand children features
    		for(int j = 0 ; j < pt.leftChild.get(i).leftChild.size(); j++)
    		{
    			// "pt" being a parent and child of "pt" being a child and grand children of "pt" being grand children.
    			String curlcgcpos = pt.leftChild.get(i).leftChild.get(j).label.toString();
    			String curlcgclex = pt.leftChild.get(i).leftChild.get(j).anchorWord.toString();
    			
    			add("Ggc1:"+curpos+"#"+curlcpos+"#"+curlcgcpos,feats);
    			add("Ggc2:"+curpos+"#"+curlcpos+"#"+curlcgclex,feats);
    		}
    		
    		for(int j = 0 ; j < pt.leftChild.get(i).rightChild.size(); j++)
    		{
    			String curlcgcpos = pt.leftChild.get(i).rightChild.get(j).label.toString();
    			String curlcgclex = pt.leftChild.get(i).rightChild.get(j).anchorWord.toString();
    			
    			add("Ggc1:"+curpos+"#"+curlcpos+"#"+curlcgcpos,feats);
    			add("Ggc2:"+curpos+"#"+curlcpos+"#"+curlcgclex,feats);
    		}

    		getGlobalFeatures(feats,pt.leftChild.get(i));
    		//getMSTFeatures(pt.leftChild.get(i), pt, Statics.getLinkString(pt.leftLink.get(i)), true, pt.sentence, feats);
    	}
    	
    	for (int i = pt.rightChild.size() -1  ; i > -1 ; i--)
    	{
    		// Each child of "pt" being a child and "pt" being a parent.

    		String currcpos = pt.rightChild.get(i).label.toString();
    		String currclex = pt.rightChild.get(i).anchorWord.toString();
    		
    		String curlsrcpos = i-1 <= -1 ? ( pt.leftChild.size() <= 0 ? "NIL" : pt.leftChild.lastElement().label.toString() ): pt.rightChild.get(i-1).label.toString();
    		String curlsrclex = i-1 <= -1 ? ( pt.leftChild.size() <= 0 ? "NIL" : pt.leftChild.lastElement().anchorWord.toString() ): pt.rightChild.get(i-1).anchorWord.toString();
    		
    		String currsrcpos = i+1 >= pt.rightChild.size() ? "NIL" : pt.rightChild.get(i+1).label.toString();
    		String currsrclex = i+1 >= pt.rightChild.size() ? "NIL" : pt.rightChild.get(i+1).label.toString();
    		
    		 child and parent features with pos n lex
    		add("Gp1:"+curpos+"#"+currcpos,feats);
    		add("Gp2:"+curpos+"#"+currcpos+"#"+currclex,feats);
    		
    		left sibling with parent and child
    		add("Gls1:"+curpos+"#"+currcpos+"#"+curlsrcpos,feats);
    		add("Gls2:"+curpos+"#"+currcpos+"#"+curlsrclex,feats);
    		
    		right sibling with parent and child
    		add("Grs1:"+curpos+"#"+currcpos+"#"+currsrclex,feats);
    		add("Grs2:"+curpos+"#"+currcpos+"#"+currsrcpos,feats);

    		// Grand children features
    		for(int j = 0 ; j < pt.rightChild.get(i).leftChild.size(); j++)
    		{
    			// "pt" being a parent and child of "pt" being a child and grand children of "pt" being grand children.
    			String currcgcpos = pt.rightChild.get(i).leftChild.get(j).label.toString();
    			String currcgclex = pt.rightChild.get(i).leftChild.get(j).anchorWord.toString();
    			
    			add("Ggc1:"+curpos+"#"+currcpos+"#"+currcgcpos,feats);
    			add("Ggc2:"+curpos+"#"+currcpos+"#"+currcgclex,feats);
    		}
    		
    		for(int j = 0 ; j < pt.rightChild.get(i).rightChild.size(); j++)
    		{
    			
    			String currcgcpos = pt.rightChild.get(i).rightChild.get(j).label.toString();
    			String currcgclex = pt.rightChild.get(i).rightChild.get(j).anchorWord.toString();
    			
    			add("Ggc1:"+curpos+"#"+currcpos+"#"+currcgcpos,feats);
    			add("Ggc2:"+curpos+"#"+currcpos+"#"+currcgclex,feats);
    		}

    		getGlobalFeatures(feats,pt.rightChild.get(i));
    		//getMSTFeatures(pt, pt.rightChild.get(i), Statics.getLinkString(pt.rightLink.get(i)), false, pt.sentence, feats);
    	}
    	return;
    }
  */  
    public  void add(String feature , Vector<Integer> allfeat)
    {
    	//int idx = localwact.gwt.regFeat(feature);
    	int idx = manager.featW.regFeat(feature);
    	if(idx != -1)
    		allfeat.add(idx);
    	return;
    }

    // double val is not used at all
    public  void add(String feature , double val, Vector<String> allfeat)
    {
    	allfeat.add(feature);
    }
  
	
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("D:"+getDepth());
		sb.append("[PDis:");
		if(parentState!=null)
			sb.append(parentState.discrepancyPosition);
		else
			sb.append("NIL");
		sb.append("]");
		sb.append("[CurDis:");
		sb.append(discrepancyPosition);
		sb.append("]");
		sb.append("[Loss:");
		sb.append(loss);
		sb.append("]");
		sb.append("[Acc:");
		sb.append((1-accuracy));
		sb.append("]");sb.append('\n');
		sb.append(actionSequence.toString());
		return sb.toString();
	}
	
	public HCSearch getManager()
	{
		return manager;
	}
	
	public int getDepth()
	{
		return depth;
	}
	
	public int getMode()
	{
		return manager.getMode(); 
	}
	
	public void setDepth(int d)
	{
		depth = d;
	}
	
	public void addChildState(HCSearchState newChildState)
	{
		childStates.add(newChildState);
		newChildState.parentState = this;
		newChildState.parentGraph = graph;
	}
	
	public ArrayList<HCSearchState> getChildStates()
	{
		return childStates;
	}
	
	public double getHScore()
	{
		return heuristicScore;
	}
	public double getCScore()
	{
		return costFunctionScore;
	}
	public int getLoss()
	{
		return loss;
	}
	public double getAccuracy()
	{
		return accuracy;
	}
	public DependencyGraph getGraph()
	{
		return graph;
	}
	public DependencyGraph getParentGraph()
	{
		return parentGraph;
	}
	public HCSearchState getParentState()
	{
		return parentState;
	}
	public int getDiscrepancyPosition()
	{
		return discrepancyPosition;
	}
	
	public int getDiscrepancyAction()
	{
		return discrepantActionCode;
	}

	public String getDiscrepantActionSymbol()
	{
		return discrepantActionSymbol;
	}
	
	public void setDiscrepantActionSymbol(String sym)
	{
		discrepantActionSymbol = sym;
	}
	
	public void setActionSequence(ArrayList<ScoredCandidate> a)
	{
		actionSequence = a;
	}
	public ArrayList<ScoredCandidate> getActionSequence()
	{
		return actionSequence;
	}
}
