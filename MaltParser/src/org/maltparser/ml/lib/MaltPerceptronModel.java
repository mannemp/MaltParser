package org.maltparser.ml.lib;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashingStrategy;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.MultipleFeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.history.action.SingleDecision;

public abstract class MaltPerceptronModel implements MaltLibModel{
	
    public abstract int getK();
    public abstract void setK(int k);
    public abstract void setCurrentSentNo(int n) throws MaltChainedException;
    public abstract int getCurrentSentNo() throws MaltChainedException;
    public abstract void setActionCodes(Set<Integer> codes);
    public abstract int[] getActionCodes();
    // return the next action 
    public abstract int train(HashMap<Integer,Integer> actionCosts, MaltFeatureNode[] x, int curIter) throws MaltChainedException;
    public abstract int train(HashMap<Integer,Integer> actionCosts, MaltFeatureNode[] x, int[] pruned, int curIter) throws MaltChainedException;
    public abstract void train(SingleDecision decision, MaltFeatureNode[] x) throws MaltChainedException;
    public abstract void train(SingleDecision decision, MaltFeatureNode[] x, int[] pruned) throws MaltChainedException;
    
    public static MaltFeatureNode[] convertFVtoMFN(FeatureVector featureVector)
	{
		MaltFeatureNode[] mfns = new MaltFeatureNode[featureVector.size()];

		for (int i = 0; i < featureVector.size(); i++) {
			FeatureValue featureValue = featureVector.getFeatureValue(i);
			if (featureValue == null) {
//				sb.append("-1");
				mfns[i] = new MaltFeatureNode();
			} else {
				if (!featureValue.isMultiple()) {
					SingleFeatureValue singleFeatureValue = (SingleFeatureValue)featureValue;
					if (singleFeatureValue.getValue() == 1) {
						mfns[i] = new MaltFeatureNode(singleFeatureValue.getIndexCode(), 1.0);
//						sb.append(singleFeatureValue.getIndexCode());
					} else if (singleFeatureValue.getValue() == 0) {
						mfns[i] = new MaltFeatureNode();
//						sb.append("-1");
					} else {
						mfns[i] = new MaltFeatureNode(singleFeatureValue.getIndexCode(), singleFeatureValue.getValue());
//						sb.append(singleFeatureValue.getIndexCode());
//						sb.append(":");
//						sb.append(singleFeatureValue.getValue());
					}
				} else { //if (featureValue instanceof MultipleFeatureValue) {
					Set<Integer> values = ((MultipleFeatureValue)featureValue).getCodes();
					int j=0;
					// TODO: Does not handle multiplefeaturevalue right now !!
//					((MaltPerceptronModel)model).regFeat(values.toString());
//					mfns[i] = new MaltFeatureNode(values.toString(),1.0);
					/*for (Integer value : values) {
						sb.append(value.toString());
						if (j != values.size()-1) {
							sb.append("|");
						}
						j++;
					}*/
				}
//				else {
//					throw new LibException("Don't recognize the type of feature value: "+featureValue.getClass());
//				}
			}
//			sb.append('\t');
		}
//		sb.append('\n');
		return mfns;
	}
}



