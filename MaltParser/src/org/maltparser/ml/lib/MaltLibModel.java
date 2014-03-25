package org.maltparser.ml.lib;

public interface MaltLibModel {
	public int[] predict(MaltFeatureNode[] x, boolean cmltWts);
	public double[][] scorePredict(MaltFeatureNode[] x, boolean cmltWts);
	public int[] predict(MaltFeatureNode[] x);
	public double[][] scorePredict(MaltFeatureNode[] x);
}
