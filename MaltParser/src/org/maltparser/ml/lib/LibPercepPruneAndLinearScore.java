package org.maltparser.ml.lib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.MultipleFeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.core.helper.Util;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.ml.LearningMethod;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.pruneandscore.PruneAndScore;

public class LibPercepPruneAndLinearScore extends Lib {
	protected int pruneTopK;
	protected int pruneCV;
	public LibPercepPruneAndLinearScore(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		super(owner, learnerMode, "libperceppruneandlinearscore");
		pruneTopK = ((PruneAndScore)getConfiguration()).getPruneTopK();
		pruneCV = ((PruneAndScore)getConfiguration()).getPruneCV();
		if (learnerMode == CLASSIFY) {
			try {
			    ObjectInputStream pinput = new ObjectInputStream(getInputStreamFromConfigFileEntry(".pmoo"));
			    try {
			    	pmodel = (MaltPerceptronModel)pinput.readObject();
			    	((MaltPerceptronModel)pmodel).setK(pruneTopK); 
			    } finally {
			    	pinput.close();
			    }
//			    if(getFile(".moo").exists()){
			    	ObjectInputStream input = new ObjectInputStream(getInputStreamFromConfigFileEntry(".moo"));
				    try {
				    	model = (MaltLibModel)input.readObject();
				    } finally {
				    	input.close();
				    }
//			    }
			} catch (ClassNotFoundException e) {
				throw new LibException("Couldn't load the libpruneandscore models", e);
			} catch (Exception e) {
				throw new LibException("Couldn't load the libpruneandscore models", e);
			}
		}
		else if(learnerMode == PLEARN)
		{
//			pmodel = new SinglePerceptronModel(featureMap, pruneTopK);
			pmodel = new ManageCVPerceptron(featureMap, pruneTopK, pruneCV,5000,0,learnerMode);
		}
	}
	
	@Override
	public void addInstance(SingleDecision decision, FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibException("The decision cannot be found");
		}
		if(getLibMode() != PruneAndScore.SLEARN){
			throw new LibException("Not in scorer's learn mode");
		}
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(decision.getDecisionCode()+"\t");
			final int n = featureVector.size();
//			MaltFeatureNode[] mfns = new MaltFeatureNode[n];

			for (int i = 0; i < n; i++) {
				FeatureValue featureValue = featureVector.getFeatureValue(i);
				if (featureValue == null || (excludeNullValues == true && featureValue.isNullValue())) {
					sb.append("-1");
//					mfns[i] = new MaltFeatureNode();
				} else {
					if (!featureValue.isMultiple()) {
						SingleFeatureValue singleFeatureValue = (SingleFeatureValue)featureValue;
						if (singleFeatureValue.getValue() == 1) {
//							mfns[i] = new MaltFeatureNode(singleFeatureValue.getIndexCode(), 1.0);
							sb.append(singleFeatureValue.getIndexCode());
						} else if (singleFeatureValue.getValue() == 0) {
//							mfns[i] = new MaltFeatureNode();
							sb.append("-1");
						} else {
//							mfns[i] = new MaltFeatureNode(singleFeatureValue.getIndexCode(), singleFeatureValue.getValue());
							sb.append(singleFeatureValue.getIndexCode());
							sb.append(":");
							sb.append(singleFeatureValue.getValue());
						}
					} else { //if (featureValue instanceof MultipleFeatureValue) {
						Set<Integer> values = ((MultipleFeatureValue)featureValue).getCodes();
						int j=0;
						// TODO: Does not handle multiplefeaturevalue right now !!
//						((MaltPerceptronModel)model).regFeat(values.toString());
//						mfns[i] = new MaltFeatureNode(values.toString(),1.0);
						for (Integer value : values) {
							sb.append(value.toString());
							if (j != values.size()-1) {
								sb.append("|");
							}
							j++;
						}
					}
//					else {
//						throw new LibException("Don't recognize the type of feature value: "+featureValue.getClass());
//					}
				}
				sb.append('\t');
			}
			sb.append('\n');
			instanceOutput.write(sb.toString());
//			instanceOutput.write(createRankListInstance(sb.toString()));
			if(numberOfInstances%100==0)
				instanceOutput.flush();
//			((MaltPerceptronModel)model).train(decision,mfns);
			increaseNumberOfInstances();
			sb.setLength(0);
			
		} catch (Exception e) {
			throw new LibException("The learner cannot write to the instance file. ", e);
		}
	}
	@Override
	public void addPruneInstance(SingleDecision decision, FeatureVector featureVector) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The feature vector cannot be found");
		} else if (decision == null) {
			throw new LibException("The decision cannot be found");
		}
		if(getLibMode() != LearningMethod.PLEARN){
			throw new LibException("Not in pruner's learn mode");
		}
		
		try {
//			if(getCurrentSentNo() < 1000 )
			{
				String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
				SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
				pmodel.setActionCodes(actionTable.getCodes());
			}
			pmodel.setCurrentSentNo(getCurrentSentNo());
			final int n = featureVector.size();
			MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
			pmodel.train(decision,mfns);
			increaseNumberOfInstances();
			
		} catch (Exception e) {
			throw new LibException("The learner cannot write to the instance file. ", e);
		}
	}
	
	@Override
	public boolean predict(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		
		try {
//			final FeatureList featureList = getFeatureList(featureVector);
//			decision.getKBestList().addList(model.predict(featureList.toArray()));
			pmodel.setCurrentSentNo(getCurrentSentNo());
			decision.getKBestList().addList(predictPruneAndScore(featureVector));
//			decision.getKBestList().addList(predictPrune(featureVector));
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
		return true;
	}
	
	public int[] predictPruneAndScore(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (pmodel == null) {
			throw new LibException("The pruner model is null and hasn't been loaded.");
		}
		if (model == null) {
			throw new LibException("The scorer model is null and hasn't been loaded.");
		}
//		final FeatureList featureList = getFeatureList(featureVector);
		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		final FeatureList featureList = getFeatureList(featureVector);
		try {
//			decision.getKBestList().addList(model.predict(featureList.toArray()));
			double[][] pruneList = pmodel.scorePredict(mfns,true);
			double[][] scoreList = model.scorePredict(featureList.toArray(),true);
			int[] finalList = new int[pruneTopK];
			int k = 0;
			for(int i = 0 ; k < pruneTopK && i < scoreList[0].length; i++)
			{
//				if(Arrays.asList(pruneList[0]).contains(scoreList[0][i]))
				for(double pcode : pruneList[0])
					if(Double.compare(pcode, scoreList[0][i]) == 0)
						finalList[k++] = (int)scoreList[0][i];
			}
			/*int[] retList = new int[scoreList[0].length];
			k = 0;
			for(double code:scoreList[0])
				retList[k++] = (int)code;*/
			/*int[] retList = new int[k];
			System.arraycopy( scoreList[0], 0, retList, 0, k );*/
			return finalList;
//			return retList;
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	public int[] predictScore(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (model == null) {
			throw new LibException("The scorer model is null and hasn't been loaded.");
		}
//		final FeatureList featureList = getFeatureList(featureVector);
//		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		final FeatureList featureList = getFeatureList(featureVector);
		try {
//			decision.getKBestList().addList(model.predict(featureList.toArray()));
//			double[][] pruneList = pmodel.scorePredict(mfns);
			double[][] scoreList = model.scorePredict(featureList.toArray(),true);
	/*		int[] finalList = new int[pruneTopK];
			int k = 0;
			for(int i = 0 ; k < pruneTopK && i < scoreList[0].length; i++)
			{
//				if(Arrays.asList(pruneList[0]).contains(scoreList[0][i]))
				for(double pcode : pruneList[0])
					if(Double.compare(pcode, scoreList[0][i]) == 0)
						finalList[k++] = (int)scoreList[0][i];
			}*/
			int[] retList = new int[scoreList[0].length];
			int k = 0;
			for(double code:scoreList[0])
				retList[k++] = (int)code;
			/*int[] retList = new int[k];
			System.arraycopy( scoreList[0], 0, retList, 0, k );*/
//			return finalList;
			return retList;
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}

	public int[] predictPrune(FeatureVector featureVector) throws MaltChainedException 
	{
		if (featureVector == null) {
			throw new LibException("The learner cannot predict the next class, because the feature vector cannot be found. ");
		}
		if (pmodel == null) {
			throw new LibException("The pruner model is null and hasn't been loaded.");
		}
//		final FeatureList featureList = getFeatureList(featureVector);
		MaltFeatureNode[] mfns = MaltPerceptronModel.convertFVtoMFN(featureVector);
		try {
//			decision.getKBestList().addList(model.predict(featureList.toArray()));
			double[][] pruneList = pmodel.scorePredict(mfns,true);
//			double[][] scoreList = model.scorePredict(featureList.toArray());
			int[] finalList = new int[pruneTopK];
			int k = 0;
			for(int i = 0 ; i < pruneTopK; i++)
			{
				finalList[k++] = (int)pruneList[0][i];
			}
			return finalList;
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	@Override
	protected void trainInternal(FeatureVector featureVector) throws MaltChainedException {
		
		try{
			/*if(getLibMode() == LearningMethod.PLEARN)
				saveFeatureMap(new BufferedOutputStream(new FileOutputStream(getFile(".pmap").getAbsolutePath())), featureMap);
			else
				saveFeatureMap(new BufferedOutputStream(new FileOutputStream(getFile(".map").getAbsolutePath())), featureMap);*/
		}
		catch(Exception e){}
		if (getLibMode() == LearningMethod.PLEARN && pmodel != null) {
			try {
				if (configLogger.isInfoEnabled()) {
//					configLogger.info("Creating Libperceptron model "+getConfigNameFile(".pmoo").getCanonicalPath()+"\n");
					configLogger.info("Creating Libperceptron model "+getFile(".pmoo").getAbsolutePath()+"\n");
				}
//			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getConfigNameFile(".pmoo").getAbsolutePath())));
				ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".pmoo").getAbsolutePath())));
		        try{
		          output.writeObject(pmodel);
		        } finally {
		          output.close();
		        }
			} catch (OutOfMemoryError e) {
				throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
			} catch (IllegalArgumentException e) {
				throw new LibException("The LibPruneandscore learner was not able to redirect Standard Error stream. ", e);
			} catch (SecurityException e) {
				throw new LibException("The LibPruneandscore learner cannot remove the instance file. ", e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new LibException("The LibPruneandscore learner cannot save the model file '"+getFile(".pmoo").getAbsolutePath()+"'. ", e);
			}
			return;
		}
		if (configLogger.isInfoEnabled()) {
			configLogger.info("Creating Libperceptron model "+getFile(".moo").getName()+"\n");
		}
		// Libmode == LEARN i.e. scorer's learner
		double[] wmodel = null;
		int[] labels = null;
		int nr_class = 0;
		int nr_feature = 0;
		Parameter parameter = getLiblinearParameters();
		try {	
			Problem problem = readProblem(getInstanceInputStreamReader(".ins"));
			boolean res = checkProblem(problem);
			if (res == false) {
				throw new LibException("Abort (The number of training instances * the number of classes) > "+Integer.MAX_VALUE+" and this is not supported by LibLinear. ");
			}
			if (configLogger.isInfoEnabled()) {
				owner.getGuide().getConfiguration().getConfigLogger().info("- Train a parser model using LibLinear.\n");
			}
			final PrintStream out = System.out;
			final PrintStream err = System.err;
			System.setOut(NoPrintStream.NO_PRINTSTREAM);
			System.setErr(NoPrintStream.NO_PRINTSTREAM);
			Model model = Linear.train(problem, parameter);
			System.setOut(err);
			System.setOut(out);
			problem = null;
			wmodel = model.getFeatureWeights();
			labels = model.getLabels();
			nr_class = model.getNrClass();
			nr_feature = model.getNrFeature();
			if (!saveInstanceFiles) {
				getFile(".ins").delete();
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The Liblinear learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The Liblinear learner cannot remove the instance file. ", e);
		} catch (NegativeArraySizeException e) {
			throw new LibException("(The number of training instances * the number of classes) > "+Integer.MAX_VALUE+" and this is not supported by LibLinear.", e);
		}
		
		if (configLogger.isInfoEnabled()) {
			configLogger.info("- Optimize the memory usage\n");
		}
		MaltLiblinearModel xmodel = null;
		try {
//			System.out.println("Nr Features:" +  nr_feature);
//			System.out.println("nr_class:" + nr_class);
//			System.out.println("wmodel.length:" + wmodel.length);		
			double[][] wmatrix = convert2(wmodel, nr_class, nr_feature);
			xmodel = new MaltLiblinearModel(labels, nr_class, wmatrix.length, wmatrix, parameter.getSolverType());
			if (configLogger.isInfoEnabled()) {
				configLogger.info("- Save the Liblinear model "+getFile(".moo").getName()+"\n");
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}			
		try {
			if (xmodel != null) {
			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
		        try{
		          output.writeObject(xmodel);
		        } finally {
		          output.close();
		        }
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The Liblinear learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The Liblinear learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The Liblinear learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		}
	}
	
	 private double[][] convert2(double[] w, int nr_class, int nr_feature) {
	        int[] wlength = new int[nr_feature];
	        int nr_nfeature = 0;
//	        int ne = 0;
//	        int nr = 0;
//	        int no = 0;
//	        int n = 0;
	        
	        // Identify length of new weight array for each feature
	        for (int i = 0; i < nr_feature; i++) {
	        	int k = nr_class;       	
	        	for (int t = i * nr_class; (t + (k - 1)) >= t; k--) {
	        		if (w[t + k - 1] != 0.0) {
	        			break;
	        		}
	        	}
	        	int b = k;
	        	if (b != 0) {
		        	for (int t = i * nr_class; (t + (b - 1)) >= t; b--) {
		        		if (b != k) {
		        			if (w[t + b - 1] != w[t + b]) {
		        				break;
		        			}
		        		}
		        	}
	        	}
	        	if (k == 0 || b == 0) {
	        		wlength[i] = 0;
	        	} else {
	        		wlength[i] = k;
	        		nr_nfeature++;
	        	}        	
	        }
	        // Allocate the weight matrix with the new number of features and
	        // an array wsignature that efficient compare if weight vector can be reused by another feature. 
	        double[][] wmatrix = new double[nr_nfeature][];
	        double[] wsignature = new double[nr_nfeature];
	        Long[] reverseMap = featureMap.reverseMap();
	        int in = 0;
	        for (int i = 0; i < nr_feature; i++) {
	            if (wlength[i] == 0) {
	            	// if the length of the weight vector is zero than eliminate the feature from the feature map.
//	            	ne++;
	            	featureMap.removeIndex(reverseMap[i + 1]);
	            	reverseMap[i + 1] = null;
	            } else {          	
	            	boolean reuse = false;
	            	double[] copy = new double[wlength[i]];
	            	System.arraycopy(w, i * nr_class, copy, 0, wlength[i]);
	            	featureMap.setIndex(reverseMap[i + 1], in + 1);
	            	for (int j=0; j<copy.length; j++) wsignature[in] += copy[j];
		            for (int j = 0; j < in; j++) {
		            	if (wsignature[j] == wsignature[in]) {
		            		// if the signatures is equal then do more narrow comparison  
			            	if (Util.equals(copy, wmatrix[j])) {
			            		// if equal then reuse the weight vector
			            		wmatrix[in] = wmatrix[j];
			            		reuse = true;
//			            		nr++;
			            		break;
			            	}
		            	}
		            }
		            if (reuse == false) {
		                // if no reuse has done use the new weight vector in the weight matrix 
//		            	no++;
		            	wmatrix[in] = copy;
		            }
		            in++;
	            }
//	            n++;
	        }
	        featureMap.setFeatureCounter(nr_nfeature);
//	        System.out.println("NE:"+ne);
//	        System.out.println("NR:"+nr);
//	        System.out.println("NO:"+no);
//	        System.out.println("N :"+n);
	        return wmatrix;
	    }
		
	 
    public static boolean eliminate(double[] a) {
    	if (a.length == 0) {
    		return true;
    	}
    	for (int i = 1; i < a.length; i++) {
    		if (a[i] != a[i-1]) {
    			return false;
    		}
    	}
    	return true;
    }
    @Override
	protected void trainExternal(FeatureVector featureVector) throws MaltChainedException {
		try {		
			
			if (configLogger.isInfoEnabled()) {
				owner.getGuide().getConfiguration().getConfigLogger().info("Creating liblinear model (external) "+getFile(".mod").getName());
			}
			binariesInstances2SVMFileFormat(getInstanceInputStreamReader(".ins"), getInstanceOutputStreamWriter(".ins.tmp"));
			final String[] params = getLibParamStringArray();
			String[] arrayCommands = new String[params.length+3];
			int i = 0;
			arrayCommands[i++] = pathExternalTrain;
			for (; i <= params.length; i++) {
				arrayCommands[i] = params[i-1];
			}
			arrayCommands[i++] = getFile(".ins.tmp").getAbsolutePath();
			arrayCommands[i++] = getFile(".mod").getAbsolutePath();
			
	        if (verbosity == Verbostity.ALL) {
	        	owner.getGuide().getConfiguration().getConfigLogger().info('\n');
	        }
			final Process child = Runtime.getRuntime().exec(arrayCommands);
	        final InputStream in = child.getInputStream();
	        final InputStream err = child.getErrorStream();
	        int c;
	        while ((c = in.read()) != -1){
	        	if (verbosity == Verbostity.ALL) {
	        		owner.getGuide().getConfiguration().getConfigLogger().info((char)c);
	        	}
	        }
	        while ((c = err.read()) != -1){
	        	if (verbosity == Verbostity.ALL || verbosity == Verbostity.ERROR) {
	        		owner.getGuide().getConfiguration().getConfigLogger().info((char)c);
	        	}
	        }
            if (child.waitFor() != 0) {
            	owner.getGuide().getConfiguration().getConfigLogger().info(" FAILED ("+child.exitValue()+")");
            }
	        in.close();
	        err.close();
			if (configLogger.isInfoEnabled()) {
				configLogger.info("\nSaving Liblinear model "+getFile(".moo").getName()+"\n");
			}
			MaltLiblinearModel xmodel = new MaltLiblinearModel(getFile(".mod"));
		    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
	        try{
	          output.writeObject(xmodel);
	        } finally {
	          output.close();
	        }
	        if (!saveInstanceFiles) {
				getFile(".ins").delete();
				getFile(".mod").delete();
				getFile(".ins.tmp").delete();
	        }
	        if (configLogger.isInfoEnabled()) {
	        	configLogger.info('\n');
	        }
		} catch (InterruptedException e) {
			 throw new LibException("Learner is interrupted. ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	public void terminate() throws MaltChainedException { 
		super.terminate();
	}

	public void initLibOptions() {
		libOptions = new LinkedHashMap<String, String>();
		libOptions.put("s", "4"); // type = SolverType.L2LOSS_SVM_DUAL (default)
		libOptions.put("c", "0.1"); // cost = 1 (default)
		libOptions.put("e", "0.1"); // epsilon = 0.1 (default)
		libOptions.put("B", "-1"); // bias = -1 (default)
	}
	
	public void initAllowedLibOptionFlags() {
		allowedLibOptionFlags = "sceB";
	}
	
	private Problem readProblem(InputStreamReader isr) throws MaltChainedException {
		Problem problem = new Problem();
		final FeatureList featureList = new FeatureList();
		if (configLogger.isInfoEnabled()) {
			owner.getGuide().getConfiguration().getConfigLogger().info("- Read all training instances.\n");
		}
		try {
			final BufferedReader fp = new BufferedReader(isr);
			
			problem.bias = -1;
			problem.l = getNumberOfInstances();
			problem.x = new FeatureNode[problem.l][];
			problem.y = new int[problem.l];
			int i = 0;
			
			while(true) {
				String line = fp.readLine();
				if(line == null) break;
				int y = binariesInstance(line, featureList);
				if (y == -1) {
					continue;
				}
				try {
					problem.y[i] = y;
					problem.x[i] = new FeatureNode[featureList.size()];
					int p = 0;
			        for (int k=0; k < featureList.size(); k++) {
			        	MaltFeatureNode x = featureList.get(k);
						problem.x[i][p++] = new FeatureNode(x.getIndex(), x.getValue());
					}
					i++;
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new LibException("Couldn't read liblinear problem from the instance file. ", e);
				}

			}
			fp.close();
			problem.n = featureMap.size();
		} catch (IOException e) {
			throw new LibException("Cannot read from the instance file. ", e);
		}
		
		return problem;
	}
	
	private boolean checkProblem(Problem problem) throws MaltChainedException {
		int max_y = problem.y[0];
		for (int i = 1; i < problem.y.length; i++) {
			if (problem.y[i] > max_y) {
				max_y = problem.y[i];
			}
		}
		if (max_y * problem.l < 0) { // max_y * problem.l > Integer.MAX_VALUE
			if (configLogger.isInfoEnabled()) {
				owner.getGuide().getConfiguration().getConfigLogger().info("*** Abort (The number of training instances * the number of classes) > Max array size: ("+problem.l+" * "+max_y+") > "+Integer.MAX_VALUE+" and this is not supported by LibLinear.\n");
			}
			return false;
		}
		return true;
	}
	
	private Parameter getLiblinearParameters() throws MaltChainedException {
		Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
		String type = libOptions.get("s");
		
		if (type.equals("0")) {
			param.setSolverType(SolverType.L2R_LR);
		} else if (type.equals("1")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC_DUAL);
		} else if (type.equals("2")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC);
		} else if (type.equals("3")) {
			param.setSolverType(SolverType.L2R_L1LOSS_SVC_DUAL);
		} else if (type.equals("4")) {
			param.setSolverType(SolverType.MCSVM_CS);
		} else if (type.equals("5")) {
			param.setSolverType(SolverType.L1R_L2LOSS_SVC);	
		} else if (type.equals("6")) {
			param.setSolverType(SolverType.L1R_LR);	
		} else if (type.equals("7")) {
			param.setSolverType(SolverType.L2R_LR_DUAL);	
		} else {
			throw new LibException("The liblinear type (-s) is not an integer value between 0 and 4. ");
		}
		try {
			param.setC(Double.valueOf(libOptions.get("c")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear cost (-c) value is not numerical value. ", e);
		}
		try {
			param.setEps(Double.valueOf(libOptions.get("e")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear epsilon (-e) value is not numerical value. ", e);
		}
		return param;
	}
	
	public int getCurrentSentNo() throws MaltChainedException
	{
		return ((PruneAndScore)getConfiguration()).getCurrentSentNo();
	}
}
