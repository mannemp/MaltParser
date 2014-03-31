package org.maltparser.parser.evaluation;

import java.util.HashMap;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;

public class Evaluate {
//	configDir.getInputDataFormatInstance();
	DataFormatInstance dataFormatInstance;
	SymbolTable deprelSymbolTable;
	protected int totalSents;
	protected int totalTokens;
	protected int correctUAttachments;
	protected int correctLAttachments;
	protected int correctLabels;
	protected int correctUCompleteAttachments;
	protected int correctLCompleteAttachments;
	protected double LAS; // Labeled Attachment Score
	protected double UAS; // Unlabeled Attachment Score
	protected double LA; // Label Accuracy
	protected double UCAS; // Unlabeled Complete Attachment Score
	protected double LCAS; // Labeled Complete Attachment Score
	protected double ActionAccuracy ; // Oracle Action Accuracy
	protected int totalActions;
	protected int correctActions;
	protected HashMap<Integer,Integer> positionHits, positionCorrectActions;
	protected HashMap<Integer,Double> iterLAS; // Labeled Attachment Score
	protected HashMap<Integer,Double> iterUAS; // Unlabeled Attachment Score
	protected HashMap<Integer,Double> iterLA; // Label Accuracy
	protected HashMap<Integer,Double> iterUCAS; // Unlabeled Complete Attachment Score
	protected HashMap<Integer,Double> iterLCAS; // Labeled Complete Attachment Score
	protected HashMap<Integer,Double> iterActionAccuracy ; // Oracle Action Accuracy
	protected HashMap<Integer,Double> positionActionAccuracy ; // Position Action Accuracy
	
	protected HashMap<Integer,Double> iterPActionAccuracy ; // Pruner's Action Accuracy
	protected HashMap<Integer,Double> iterSActionAccuracy ; // Scorer's Action Accuracy
	protected double PActionAccuracy ; // Pruner's Action Accuracy
	protected double SActionAccuracy ; // Scorer's Action Accuracy
	protected int totalPActions;
	protected int correctPActions;
	protected int totalSActions;
	protected int correctSActions;
	
	public Evaluate(DataFormatInstance df)
	{
		dataFormatInstance = df;
		deprelSymbolTable = dataFormatInstance.getColumnDescriptionByName("DEPREL").getSymbolTable();
		reset();
		iterLAS = new HashMap<Integer,Double>();
		iterUAS = new HashMap<Integer,Double>();
		iterLA = new HashMap<Integer,Double>();
		iterUCAS = new HashMap<Integer,Double>();
		iterLCAS = new HashMap<Integer,Double>();
		iterActionAccuracy = new HashMap<Integer,Double>();
		iterPActionAccuracy = new HashMap<Integer,Double>();
		iterSActionAccuracy = new HashMap<Integer,Double>();
	}
	
	public void reset()
	{
		totalSents = totalTokens = 0;
		correctLabels = correctLAttachments = correctLCompleteAttachments = correctUAttachments = correctUCompleteAttachments = 0;
		LAS = UAS = LA = UCAS = LCAS = 0;
		totalActions = correctActions = 0;
		positionHits = new HashMap<Integer,Integer>();;
		positionCorrectActions = new HashMap<Integer,Integer>();;
		positionActionAccuracy = new HashMap<Integer,Double>();
	}
	
	public boolean evaluate(int truecode, int[] list)
	{
		totalActions++;
		boolean found = false;
		for(int act:list)
			if(truecode == act)
			{
				correctActions++;
				found = true;
				break;
			}
		updateMetrics();
		return found;
	}
	
	public boolean evaluate(int truecode, int[] list, int position)
	{
		boolean found = evaluate(truecode, list);
		/*if(found)
			positionCorrectActions.put(position,positionCorrectActions.containsKey(position) ? positionCorrectActions.get(position) +1 : 1);
		positionHits.put(position,positionHits.containsKey(position) ? positionHits.get(position) +1 : 1);
		positionActionAccuracy.put(position, (double)positionCorrectActions.get(position)/positionHits.get(position));*/
		return found;
	}
	
	public boolean evaluate(int truecode, int predictedCode)
	{
		totalActions++;
		if(truecode == predictedCode)
		{
			correctActions++;
			updateMetrics();
			return true;
		}
		return false;
	}
	
	public boolean evaluateSAction(HashMap<Integer,Integer> actionCosts, int predictedCode)
	{
		totalSActions++;
		if(actionCosts.get(predictedCode) == 0)
		{
			correctSActions++;
			return true;
		}
		return false;
	}
	
	public boolean evaluatePAction(HashMap<Integer,Integer> actionCosts, int[] topKActions)
	{
		totalPActions++;
		for(int pcode:topKActions)
		{
			if(actionCosts.get(pcode) == 0)
			{
				correctPActions++;
				return true;
			}
		}
		return false;
	}
	
	public boolean evaluate(DependencyStructure goldGraph, DependencyStructure sysGraph ) throws MaltChainedException
	{
		try{
			if(goldGraph.nEdges() != sysGraph.nEdges())
				return false;
			boolean unlabComplete = true;
			boolean labComplete = true;
			for(Edge e:goldGraph.getEdges())
			{
				int childIdx = e.getTarget().getIndex();
				int parentIdx = e.getSource().getIndex();
				int labelCode;
				if(parentIdx != 0)
					labelCode = e.getLabelCode(deprelSymbolTable);
				else 
					labelCode = 0;
				
				if(sysGraph.getDependencyNode(childIdx).getHead().getIndex() == parentIdx )
					correctUAttachments++;
				else
					unlabComplete = false;
				
				int slabelCode;
				if(sysGraph.getDependencyNode(childIdx).getHead().getIndex() !=0)
					slabelCode = sysGraph.getDependencyNode(childIdx).getHeadEdgeLabelCode(deprelSymbolTable);
				else 
					slabelCode = 0;
				if( slabelCode == labelCode)
					correctLabels++;
				
				if(slabelCode == labelCode &&
						sysGraph.getDependencyNode(childIdx).getHead().getIndex() == parentIdx )
					correctLAttachments++;
				else
					labComplete = false;
				
				totalTokens ++;
			}
			if(labComplete)
				correctLCompleteAttachments++;
			if(unlabComplete)
				correctUCompleteAttachments++;
			totalSents++;
			updateMetrics();
		}
		catch(MaltChainedException e)
		{
			throw new MaltChainedException("Error in evaluation:",e);
		}
		return true;
	}
	
	public void updateMetrics()
	{
		UAS = (double)correctUAttachments/totalTokens;
		LAS = (double)correctLAttachments/totalTokens;
		LA = (double)correctLabels/totalTokens;
		LCAS = (double)correctLCompleteAttachments/totalSents;
		UCAS = (double)correctUCompleteAttachments/totalSents;
		ActionAccuracy = (double) correctActions/totalActions;
		PActionAccuracy = (double) correctPActions/totalPActions;
		SActionAccuracy = (double) correctSActions/totalSActions;
		
	}
	
	public double getLAS()
	{
		return LAS;
	}
	
	public double getLA()
	{
		return LA;
	}
	public double getUAS()
	{
		return UAS;
	}
	
	public void printMetrics()
	{
		if(totalSents!=0)
		{
			System.err.println("Unlabeled Attachment Score (UAS):"+UAS+ "\t"+correctUAttachments+"/"+totalTokens);
			System.err.println("Labeling Accuracy (LA):"+LA+"\t"+correctLabels+"/"+totalTokens);
			System.err.println("Labeled Attachment Score (LAS):"+LAS+"\t"+correctLAttachments+"/"+totalTokens);
			System.err.println("Complete Unlabeled Attachment Score (CUAS):"+UCAS+"\t"+correctUCompleteAttachments+"/"+totalSents);
			System.err.println("Complete Labeled Attachment Score (CLAS):"+LCAS+"\t"+correctLCompleteAttachments+"/"+totalSents);
		}
		if(totalActions != 0)
			System.err.println("On-Traj Action Accuracy (OAS):"+ActionAccuracy+"\t\t("+correctActions+"/"+totalActions+")");
		if(totalPActions != 0)
			System.err.println("Pruner's Action Accuracy (PAS):"+PActionAccuracy+"\t\t("+correctPActions+"/"+totalPActions+")");
		if(totalSActions != 0)
			System.err.println("Scorer's Action Accuracy (SAS):"+SActionAccuracy+"\t\t("+correctSActions+"/"+totalSActions+")");
		/*if(positionActionAccuracy.size() >0)
			printHash("Position vs. Action Accuracy",positionActionAccuracy);*/
	}
	
	public void saveMetrics(int iteration)
	{
		iterUAS.put(iteration, UAS);
		iterLAS.put(iteration, LAS);
		iterLA.put(iteration, LA);
		iterUCAS.put(iteration, UCAS);
		iterLCAS.put(iteration, LCAS);
		iterActionAccuracy.put(iteration, ActionAccuracy);
		iterPActionAccuracy.put(iteration, PActionAccuracy);
		iterSActionAccuracy.put(iteration, SActionAccuracy);
	}
	
	public void printHashMetrics()
	{
		if(iterUAS.size() >0)
		{
			System.out.println("----------------------------------------------------------------------");
			printHash("Unlabeled Attachment Score (UAS):",iterUAS);
			printHash("Labeling Accuracy (LA):",iterLA);
			printHash("Labeled Attachment Score (UAS):",iterLAS);
			printHash("Complete Unlabeled Attachment Score (CUAS):",iterUCAS);
			printHash("Complete Labeled Attachment Score (CLAS):",iterLCAS);
		}
		if(iterActionAccuracy.size() > 0)
			printHash("On-Traj Action Accuracy (OAS):",iterActionAccuracy);
		if(iterPActionAccuracy.size() > 0)
			printHash("Pruner's Action Accuracy (PAS):",iterPActionAccuracy);
		if(iterSActionAccuracy.size() > 0)
			printHash("Scorer's Action Accuracy (SAS):",iterSActionAccuracy);
	}
	
	public void printHash(String mesg, HashMap<Integer,Double> hash)
	{
		if(hash.size() == 0)
			return;
		System.out.println(mesg);
		for(Integer i:hash.keySet())
		{
			System.out.println(i.intValue()+"\t"+hash.get(i.intValue()));
		}
		return;
	}
}
