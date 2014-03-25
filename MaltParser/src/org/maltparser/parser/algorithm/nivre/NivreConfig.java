package org.maltparser.parser.algorithm.nivre;

import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.ParsingException;
/**
 * @author Johan Hall
 *
 */
public class NivreConfig extends ParserConfiguration {
	private final Stack<DependencyNode> stack;
	private final Stack<DependencyNode> input;
	private final DependencyStructure dependencyGraph;

	private boolean allowRoot;
	private boolean allowReduce;
	protected boolean[][] reachableArcs;
	protected boolean[][] goldArcs;

	public NivreConfig(SymbolTableHandler symbolTableHandler, boolean allowRoot, boolean allowReduce) throws MaltChainedException {
		super();
		stack = new Stack<DependencyNode>();
		input = new Stack<DependencyNode>();
		dependencyGraph = new DependencyGraph(symbolTableHandler);
		setAllowRoot(allowRoot);
		setAllowReduce(allowReduce);
	}
	
	public void initReachableArcs()
	{
		int nTokens = dependencyGraph.nTokenNode()+1;
		reachableArcs = new boolean[nTokens][nTokens];
		for(int i = 0 ; i < nTokens; i++)
		{
			for(int j = 0 ; j < nTokens; j++)
			{
				// i-> j
				if(i==j || j == 0)
					reachableArcs[i][j] = false;
				else
					reachableArcs[i][j] = true;
			}
		}
		return;
	}
	
	public void setGoldArcs(DependencyStructure goldGraph)
	{
		goldArcs = initReachableArcs(goldGraph);
		return;
	}
	
	public boolean[][] initReachableArcs(DependencyStructure graph)
	{
		int nTokens = graph.nTokenNode()+1;
		boolean[][] graphArcs = new boolean[nTokens][nTokens];
		for(int i = 0 ; i < nTokens; i++)
		{
			for(int j = 0 ; j < nTokens; j++)
			{
					graphArcs[i][j] = false;
			}
		}
		for(Edge e:graph.getEdges())
			graphArcs[e.getSource().getIndex()][e.getTarget().getIndex()] = true;
		
		return graphArcs;
	}
	
	public Stack<DependencyNode> getStack() {
		return stack;
	}
	
	public Stack<DependencyNode> getInput() {
		return input;
	}
	
	public DependencyStructure getDependencyStructure() {
		return dependencyGraph;
	}
	
	public boolean isTerminalState() {
		return input.isEmpty();
	}
	
	public DependencyNode getStackNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Stack index must be non-negative in feature specification. ");
		}
		if (stack.size()-index > 0) {
			return stack.get(stack.size()-1-index);
		}
		return null;
	}
	
	public DependencyNode getInputNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Input index must be non-negative in feature specification. ");
		}
		if (input.size()-index > 0) {
			return input.get(input.size()-1-index);
		}	
		return null;
	}
	
	public void setDependencyGraph(DependencyStructure source) throws MaltChainedException {
		dependencyGraph.clear();
		for (int index : source.getTokenIndices()) {
			final DependencyNode gnode = source.getTokenNode(index);
			final DependencyNode pnode = dependencyGraph.addTokenNode(gnode.getIndex());
			for (SymbolTable table : gnode.getLabelTypes()) {
				pnode.addLabel(table, gnode.getLabelSymbol(table));
			}
			
			if (gnode.hasHead()) {
				final Edge s = gnode.getHeadEdge();
				final Edge t = dependencyGraph.addDependencyEdge(s.getSource().getIndex(), s.getTarget().getIndex());
				
				for (SymbolTable table : s.getLabelTypes()) {
					t.addLabel(table, s.getLabelSymbol(table));
				}
			}
		}
		for (SymbolTable table : source.getDefaultRootEdgeLabels().keySet()) {
			dependencyGraph.setDefaultRootEdgeLabel(table, source.getDefaultRootEdgeLabelSymbol(table));
		}
	}
	
	public DependencyStructure getDependencyGraph() {
		return dependencyGraph;
	}
	
	public void initialize(ParserConfiguration parserConfiguration) throws MaltChainedException {
		if (parserConfiguration != null) {
			final NivreConfig nivreConfig = (NivreConfig)parserConfiguration;
			final Stack<DependencyNode> sourceStack = nivreConfig.getStack();
			final Stack<DependencyNode> sourceInput = nivreConfig.getInput();
			setDependencyGraph(nivreConfig.getDependencyGraph());
			for (int i = 0, n = sourceStack.size(); i < n; i++) {
				stack.add(dependencyGraph.getDependencyNode(sourceStack.get(i).getIndex()));
			}
			for (int i = 0, n = sourceInput.size(); i < n; i++) {
				input.add(dependencyGraph.getDependencyNode(sourceInput.get(i).getIndex()));
			}
		} else {
			stack.push(dependencyGraph.getDependencyRoot());
			for (int i = dependencyGraph.getHighestTokenIndex(); i > 0; i--) {
				final DependencyNode node = dependencyGraph.getDependencyNode(i);
				if (node != null && !node.hasHead()) { // added !node.hasHead()
					input.push(node);
				}
			}
		}
		initReachableArcs();
	}
	
    public boolean isAllowRoot() {
        return allowRoot;
	}
	
	public void setAllowRoot(boolean allowRoot) {
	        this.allowRoot = allowRoot;
	}
	
	public boolean isAllowReduce() {
	        return allowReduce;
	}
	
	public void setAllowReduce(boolean allowReduce) {
	        this.allowReduce = allowReduce;
	}
	
	public void clear() throws MaltChainedException {
		dependencyGraph.clear();
		stack.clear();
		input.clear();
		historyNode = null;
	}
	
	public boolean permissible(int trans, ParserConfiguration config) throws MaltChainedException {
		if(((NivreConfig)config).getStack().size() == 0)
			return false;
		final DependencyNode stackPeek = ((NivreConfig)config).getStack().peek();
		if ((trans == ArcEager.LEFTARC || trans == ArcEager.REDUCE) && stackPeek.isRoot()) { 
			return false;
		}
		if (trans == ArcEager.LEFTARC && stackPeek.hasHead()) { 
			return false;
		}
		if (trans == ArcEager.REDUCE && !stackPeek.hasHead() && !((NivreConfig)config).isAllowReduce()) {
			return false;
		}
		return true;
	}
	

	public boolean permissible(int trans) throws MaltChainedException {
		return permissible(trans,this);
	}

	
	public void updateReachableArcs(int actionCode, boolean[][] copyReachableArcs)
	{
		int topInputIdx = input.peek().getIndex();
		int topStackIdx = stack.peek().getIndex();
		switch (actionCode) {
		case ArcEager.LEFTARC:
			for(int i = 0; i < copyReachableArcs[topStackIdx].length ;i++)
			{
				// topStack can not head any items
				copyReachableArcs[topStackIdx][i] = false;
				// topStack can not modify any items
				copyReachableArcs[i][topStackIdx] = false;
			}
			copyReachableArcs[topInputIdx][topStackIdx] = true;
			break;
		case ArcEager.RIGHTARC:
			for(DependencyNode iNode : stack)
			{
				// topInput can not head stack items
				copyReachableArcs[topInputIdx][iNode.getIndex()] = false;
			}
			for(int i = 0; i < copyReachableArcs[topInputIdx].length ;i++)
			{
				// topInput can not modify any items
				copyReachableArcs[i][topInputIdx] = false;
			}
			copyReachableArcs[topStackIdx][topInputIdx] = true;
			break;
		case ArcEager.REDUCE:
			for(DependencyNode iNode : input)
			{
				// topStack can not be head input items
				copyReachableArcs[topStackIdx][iNode.getIndex()] = false;
			}
			for(int i = 0; i < copyReachableArcs[topStackIdx].length ;i++)
			{	// topStack can not modify any item
				copyReachableArcs[i][topStackIdx] = false;
			}
			break;
		default:
			// SHIFT operation
			for(DependencyNode iNode : stack)
			{
				// topInput can not modify stack items
				copyReachableArcs[iNode.getIndex()][topInputIdx] = false;
				// topInput can not head stack items
				copyReachableArcs[topInputIdx][iNode.getIndex()] = false;
			}
			break;
		}
		
		// Add already existing arcs 
		addCurrentArcsToReachableArcs(copyReachableArcs);
		
		return;
	}
	

	public int getYGCostAfterAction(int actionCode) throws MaltChainedException
	{
		try {
			if(!permissible(actionCode, this))
				return goldArcs.length;
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		int topInputIdx = input.peek().getIndex();
		int topStackIdx = stack.peek().getIndex();
		int cost = 0;
		switch (actionCode) {
		case ArcEager.LEFTARC:
			for(DependencyNode iNode : input)
			{
				if(goldArcs[iNode.getIndex()][topStackIdx])
					cost++;
				if(goldArcs[topStackIdx][iNode.getIndex()])
					cost++;
				
			}
			if(goldArcs[topInputIdx][topStackIdx])
				cost = 0;
			else
			{
				boolean found = false;
				int headOfStack = topStackIdx;
				for(int k = 0 ;  k < goldArcs[topStackIdx].length ; k++)
					if(goldArcs[k][topStackIdx])
						headOfStack = k;
				for(DependencyNode iNode : input)
				{
					if(iNode.getIndex() == headOfStack )
					{
						found = true;
						break;
					}
					if(goldArcs[topStackIdx][iNode.getIndex()])
					{
						found = true;
						break;
					}
				}
				if(!found)
					cost = 0;
			}
			return cost;
		case ArcEager.RIGHTARC:
			for(DependencyNode sNode : stack)
			{
				if(goldArcs[sNode.getIndex()][topInputIdx])
					cost++;
				if(goldArcs[topInputIdx][sNode.getIndex()])
					cost++;
			}
			for(DependencyNode iNode : input)
			{
				if(goldArcs[iNode.getIndex()][topInputIdx])
					cost++;
			}
			if(goldArcs[topStackIdx][topInputIdx])
				cost = 0;
			else
			{
				boolean found = false;
				int headOfInput = topInputIdx;
				for(int k = 0 ;  k < goldArcs[topInputIdx].length ; k++)
					if(goldArcs[k][topInputIdx])
						headOfInput = k;
				for(DependencyNode sNode : stack)
				{
					if(sNode.getIndex() == headOfInput )
					{
						found = true;
						break;
					}
					if(goldArcs[topInputIdx][sNode.getIndex()])
					{
						found = true;
						break;
					}
				}
				if(found)
					return cost;
				for(DependencyNode iNode : input)
					if(iNode.getIndex() == headOfInput)
					{
						found = true;
						break;
					}
				if(!found)
					cost = 0;
			}
			return cost;
		case ArcEager.REDUCE:
			cost =0;
			for(DependencyNode iNode : input)
			{
				if(goldArcs[topStackIdx][iNode.getIndex()])
					cost++;
				// topStack can not be head input items
			}
			// Removed the other case
			return cost;
		default:
			cost = 0;
			for(DependencyNode knode: stack)
			{
				if(goldArcs[topInputIdx][knode.getIndex()])
					cost++;
				if(goldArcs[knode.getIndex()][topInputIdx])
					cost++;
			}
			return cost;
		}
	}
	
	public boolean[][] getReachableArcsAfterAction(int actionCode)
	{
		// first make a copy of reachableArcs
		boolean[][] copyReachableArcs = new boolean[reachableArcs.length][];
		for(int i = 0; i < reachableArcs.length; i++)
		{
		  copyReachableArcs[i] = new boolean[reachableArcs[i].length];
		  System.arraycopy(reachableArcs[i], 0, copyReachableArcs[i], 0, reachableArcs[i].length);
		}
		// update 
		updateReachableArcs(actionCode, copyReachableArcs);
		
		return copyReachableArcs;
	}
	
	/*public boolean[][] getReachableArcs(int actionCode)
	{
		updateReachableArcs(actionCode, reachableArcs);
		return reachableArcs;
	}*/
	
	/*public boolean[][] getReachableArcs()
	{
		return reachableArcs;
	}*/
	
	/*public boolean[][] getLostArcsAfterAction(int actionCode)
	{
		boolean[][] copyReachableArcs = getReachableArcsAfterAction(actionCode);
		for(int i = 0; i < reachableArcs.length; i++)
		{
			for(int j = 0; j < reachableArcs[i].length; j++)
			{
				if(!copyReachableArcs[i][j] && reachableArcs[i][j])
					copyReachableArcs[i][j] = true;
				else
					copyReachableArcs[i][j] = false;
			}
		}
		return copyReachableArcs;
	}*/
	
	/*public int getCostAfterAction(int actionCode, DependencyStructure goldGraph)
	{
		try {
			if(!permissible(actionCode, this))
				return Integer.MAX_VALUE;
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		int cost = 0;
		boolean[][] graphArcs = initReachableArcs(goldGraph);
		boolean[][] copyReachableArcs = getReachableArcsAfterAction(actionCode);
		for(int i = 0; i < copyReachableArcs.length; i++)
		{
			for(int j = 0; j < copyReachableArcs[i].length; j++)
			{
				if(!copyReachableArcs[i][j] && graphArcs[i][j])
					cost++;
			}
		}
		return cost;
	}*/
	
	public int getCostAfterAction(int actionCode)
	{
		try {
			if(!permissible(actionCode, this))
				return Integer.MAX_VALUE;
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
		int cost = 0;
		boolean[][] copyReachableArcs = getReachableArcsAfterAction(actionCode);
		for(int i = 0; i < copyReachableArcs.length; i++)
		{
			for(int j = 0; j < copyReachableArcs[i].length; j++)
			{
				if(!copyReachableArcs[i][j] && goldArcs[i][j])
					cost++;
			}
		}
		return cost;
	}
	
	public void addCurrentArcsToReachableArcs(boolean[][] rArcs)
	{
		for(Edge e:getDependencyStructure().getEdges())
		{
			rArcs[e.getSource().getIndex()][e.getTarget().getIndex()] = true;
		}
		return;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NivreConfig that = (NivreConfig)obj;
		
		if (stack.size() != that.getStack().size()) 
			return false;
		if (input.size() != that.getInput().size())
			return false;
		if (dependencyGraph.nEdges() != that.getDependencyGraph().nEdges())
			return false;
		for (int i = 0; i < stack.size(); i++) {
			if (stack.get(i).getIndex() != that.getStack().get(i).getIndex()) {
				return false;
			}
		}
		for (int i = 0; i < input.size(); i++) {
			if (input.get(i).getIndex() != that.getInput().get(i).getIndex()) {
				return false;
			}
		}		
		return dependencyGraph.getEdges().equals(that.getDependencyGraph().getEdges());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(stack.size());
		sb.append(", ");
		sb.append(input.size());
		sb.append(", ");
		sb.append(dependencyGraph.nEdges());
		return sb.toString();
	}
}
