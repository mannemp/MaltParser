package org.maltparser.parser.pruneandscore;

import java.text.DecimalFormat;
import java.util.Vector;

public class Stats {
//	public static long initStateTime = 0;
	public static long[] timeAtDepth = new long[HCSearch.maxDepth];
	public static int[] visitsAtDepth = new int[HCSearch.maxDepth];
	
	public static int[] lossAtDepth = new int[HCSearch.maxDepth];
	public static double[] accuracyAtDepth = new double[HCSearch.maxDepth];
	public static double[] nodesAtDepth = new double[HCSearch.maxDepth];
	public static double[] policyRunsAtDepth = new double[HCSearch.maxDepth];
	public static double[] bestAccuracyAtDepth = new double[HCSearch.maxDepth];
	public static double[] noOfSentsWithZeroLossAtDepth = new double[HCSearch.maxDepth];

	public static int noOracleInSearch = 0;
	public static int noOfSents=0;
	public static long startTime;
	public static long endTime;
	public static int maxDepth = 0;
	public static double avgDepth = 0;
	public static final double initialWeight = 0;
	public static int hRounds = 0;
	public static int cRounds = 0;
	public static double threshold = 0.5;
	
	public static void init()
	{
		timeAtDepth = new long[HCSearch.maxDepth];
		visitsAtDepth = new int[HCSearch.maxDepth];
		
		lossAtDepth = new int[HCSearch.maxDepth];
		accuracyAtDepth = new double[HCSearch.maxDepth];
		nodesAtDepth = new double[HCSearch.maxDepth];
		policyRunsAtDepth = new double[HCSearch.maxDepth];
		bestAccuracyAtDepth = new double[HCSearch.maxDepth];
		noOfSentsWithZeroLossAtDepth = new double[HCSearch.maxDepth];

		for(int i = 0 ; i < HCSearch.maxDepth; i++)
		{
			timeAtDepth[i] = 0;
			visitsAtDepth[i] = 0;
			lossAtDepth[i] = 0;
			nodesAtDepth[i] = 0;
			policyRunsAtDepth[i] = 0;
			accuracyAtDepth[i] = 0;
			bestAccuracyAtDepth[i] = 0;
			noOfSentsWithZeroLossAtDepth[i] = 0;
		}
	}
	
	public static void printTime(String mesg) {
    	double timeTillNow = System.currentTimeMillis() - startTime;
    	
        System.out.println(mesg+"--> Time: " + timeTillNow/(1000*60) + " mins i.e "+ timeTillNow/(1000*60*60)+ " hrs");
    }
	
	public static long getTimeTillNow(long st) {
    	return System.currentTimeMillis() - st;
    }
	
	public static void printStats()
	{
		/*System.err.println("Average depth:"+Stats.avgDepth/(Stats.noOfSents-Stats.noOracleInSearch));
		System.err.println("Max depth:"+Stats.maxDepth);*/
		
		System.err.println("Average time taken at depth d:");
		for(int i = 0 ; i < HCSearch.maxDepth; i++)
		{
			if(Stats.visitsAtDepth[i] ==0)
				continue;
			System.err.println(i+"\t"+Stats.timeAtDepth[i]/Stats.visitsAtDepth[i]);
		}
		/*System.err.println("Average no of nodes at depth d:");
		for(int i = 0 ; i < HCSearch.maxDepth; i++)
		{
			if(Stats.visitsAtDepth[i] ==0)
				continue;
			System.err.println(i+"\t"+Stats.nodesAtDepth[i]/Stats.visitsAtDepth[i] + " visits:"+Stats.visitsAtDepth[i]);
		}*/
		System.err.println("Depth \"Accuracy\"     \"Sents at d\"    \"Nodes\" at depth d for "+noOfSents+" sentences");
//		System.err.println("Depth \"Best Accuracy\" \t \"Accuracy\"     \"Sents at d\"    \"Nodes\" at depth d for "+noOfSents+" sentences");
		for(int i = 0 ; i < HCSearch.maxDepth; i++)
		{
			if(Stats.visitsAtDepth[i] ==0)
				continue;
			DecimalFormat d = new DecimalFormat("#.###");
//			double dacc = Stats.accuracyAtDepth[i]/Stats.visitsAtDepth[i];
			double dacc = Stats.accuracyAtDepth[i]/Stats.noOfSents;
			double nacc = Stats.nodesAtDepth[i]/Stats.visitsAtDepth[i];
//			double dacc = (noOfSents - Stats.visitsAtDepth[i])+Stats.accuracyAtDepth[i]/Stats.noOfSents;
			double dacc2 = (noOfSents - Stats.visitsAtDepth[i]+Stats.accuracyAtDepth[i])/Stats.noOfSents;
			String acc = d.format(dacc);
			String acc2 = d.format(dacc2);
			String nacc2 = d.format(nacc);
//			System.err.println(i+"\t"+acc2+"\t"+acc + "\t "+Stats.visitsAtDepth[i] +"\t "+acc3) ;
			System.err.println(i+"\t"+acc + "\t "+Stats.visitsAtDepth[i] +"\t "+nacc2) ;
		}
		/*System.err.println("Percentage of sentences with correct trees at depth d:");
		int cumCount = 0;
		for(int i = 0 ; i < HCSearch.maxDepth; i++)
		{
			cumCount += noOfSentsWithZeroLossAtDepth[i];
			if(Stats.noOfSentsWithZeroLossAtDepth[i] ==0)
				continue;
			DecimalFormat d = new DecimalFormat("#.##");
			String acc = d.format((double)Stats.noOfSentsWithZeroLossAtDepth[i]/Stats.noOfSents);
			String cumAcc = d.format((double)cumCount/Stats.noOfSents);
			System.err.println(i+"\t"+acc+"\t"+cumAcc);
		}*/
	}
	
	public static void println(String mesg)
	{
		System.err.println(mesg);
	}
	
	public static void print(String mesg)
	{
		System.err.print(mesg);
	}
	
}
