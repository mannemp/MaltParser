package org.maltparser.ml.lib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.core.helper.Util;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.parser.guide.instance.InstanceModel;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.SparseDataPoint;

public class LibRanker extends Lib {
	
	public LibRanker(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		super(owner, learnerMode, "libranker");
		if (learnerMode == RANK) {
			try {
			    ObjectInputStream input = new ObjectInputStream(getInputStreamFromConfigFileEntry(".moo"));
			    try {
			    	model = (MaltLibModel)input.readObject();
			    } finally {
			    	input.close();
			    }
			} catch (ClassNotFoundException e) {
				throw new LibException("Couldn't load the liblinear model", e);
			} catch (Exception e) {
				throw new LibException("Couldn't load the liblinear model", e);
			}
		}

	}
	
	/** TODO:
	 * */
	protected void trainInternal(FeatureVector featureVector) throws MaltChainedException {
		if (configLogger.isInfoEnabled()) {
			configLogger.info("Creating Libranker model "+getFile(".moo").getName()+"\n");
		}
//		List<RankList> trainSamples = readProblem(getInstanceInputStreamReader(".ins"));
		try {
			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
		        try{
//		          output.writeObject(xmodel);
		        } finally {
		          output.close();
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
//        int ne = 0;
//        int nr = 0;
//        int no = 0;
//        int n = 0;
        
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
//            	ne++;
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
//		            		nr++;
		            		break;
		            	}
	            	}
	            }
	            if (reuse == false) {
	                // if no reuse has done use the new weight vector in the weight matrix 
//	            	no++;
	            	wmatrix[in] = copy;
	            }
	            in++;
            }
//            n++;
        }
        featureMap.setFeatureCounter(nr_nfeature);
//        System.out.println("NE:"+ne);
//        System.out.println("NR:"+nr);
//        System.out.println("NO:"+no);
//        System.out.println("N :"+n);
        return wmatrix;
    }
	
    private double[][] convert(double[] w, int nr_class, int nr_feature) {
        double[][] wmatrix = new double[nr_feature][];
        double[] wsignature = new double[nr_feature];
        boolean reuse = false;
        int ne = 0;
        int nr = 0;
        int no = 0;
        int n = 0;
        Long[] reverseMap = featureMap.reverseMap();
        for (int i = 0; i < nr_feature; i++) {
        	reuse = false;
        	int k = nr_class;
        	for (int t = i * nr_class; (t + (k - 1)) >= t; k--) {
        		if (w[t + k - 1] != 0.0) {
        			break;
        		}
        	}
        	double[] copy = new double[k];
            System.arraycopy(w, i * nr_class, copy, 0,k);
            if (eliminate(copy)) {
            	ne++;
            	featureMap.removeIndex(reverseMap[i + 1]);
            	reverseMap[i + 1] = null;
            	wmatrix[i] = null;
            } else {
            	featureMap.setIndex(reverseMap[i + 1], i + 1 - ne);
            	for (int j=0; j<copy.length; j++) wsignature[i] += copy[j];
	            for (int j = 0; j < i; j++) {
	            	if (wsignature[j] == wsignature[i]) {
		            	if (Util.equals(copy, wmatrix[j])) {
		            		wmatrix[i] = wmatrix[j];
		            		reuse = true;
		            		nr++;
		            		break;
		            	}
	            	}
	            }
	            if (reuse == false) {
	            	no++;
	            	wmatrix[i] = copy;
	            }
            }
            n++;
        }
        featureMap.setFeatureCounter(featureMap.getFeatureCounter()- ne);
        double[][] wmatrix_reduced = new double[nr_feature-ne][];
        for (int i = 0, j = 0; i < wmatrix.length; i++) {
        	if (wmatrix[i] != null) {
        		wmatrix_reduced[j++] = wmatrix[i];
        	}
        }
//        System.out.println("NE:"+ne);
//        System.out.println("NR:"+nr);
//        System.out.println("NO:"+no);
//        System.out.println("N :"+n);

        return wmatrix_reduced;
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

	
	private List<RankList> readProblem(InputStreamReader isr) throws MaltChainedException {
		
		final FeatureList featureList = new FeatureList();
		if (configLogger.isInfoEnabled()) {
			owner.getGuide().getConfiguration().getConfigLogger().info("- Read all training instances.\n");
		}
		String decisionSettings = getConfiguration().getOptionValue("guide", "decision_settings").toString().trim();
		SymbolTable actionTable = getConfiguration().getSymbolTables().getSymbolTable(decisionSettings);
		
		List<RankList> samples = new ArrayList<RankList>();
		try {
			final BufferedReader fp = new BufferedReader(isr);
			
			int noOfSamples = getNumberOfInstances();
			int sampleno = 1;
			
			while(true) {
				String line = fp.readLine();
				if(line == null) break;
				String[] columns = tabPattern.split(line);
				int trueCode = Integer.parseInt(columns[0]);
				StringBuilder sb = new StringBuilder();
				for(int l = 1; l <columns.length-1; l++)
				{
					sb.append(columns[l]);
					sb.append("\t");
				}
				sb.append(columns[columns.length-1]);
				String onlyfeatline = sb.toString();
				String[] rankliblines = new String[actionTable.size()];
				int c = 0;
				for(int code: actionTable.getCodes())
				{
					String unbinarizedline = code+"\t"+onlyfeatline;
					int y = binariesInstance(unbinarizedline, featureList);
					if (y == -1) 
						break;
					String rlline = "";
					if(y==trueCode)
						rlline += "1";
					else
						rlline += "0";
					rlline +=" "+"qid:"+sampleno;
					for(int f = 0; f < featureList.size(); f++)
						rlline += " "+featureList.get(f).index+":"+featureList.get(f).value; 
					rankliblines[c++] = rlline;
				}
				sampleno++;
				populateRankList(rankliblines, samples);
			}
			fp.close();
		} catch (IOException e) {
			throw new LibException("Cannot read from the instance file. ", e);
		}
		
		return samples;
	}
	
	protected int binariesInstance(String line, FeatureList featureList) throws MaltChainedException {
		int y = -1; 
		featureList.clear();
		try {	
			String[] columns = tabPattern.split(line);

			if (columns.length == 0) {
				return -1;
			}
			try {
				y = Integer.parseInt(columns[0]);
			} catch (NumberFormatException e) {
				throw new LibException("The instance file contain a non-integer value '"+columns[0]+"'", e);
			}
			for(int j = 1; j < columns.length; j++) {
				final String[] items = pipePattern.split(columns[j]);
				for (int k = 0; k < items.length; k++) {
					try {
						int colon = items[k].indexOf(':');
						if (colon == -1) {
							if (Integer.parseInt(items[k]) != -1) {
								int v = featureMap.addIndex(j, Integer.parseInt(items[k]),y);
								if (v != -1) {
									featureList.add(v,1);
								}
							}
						} else {
							int index = featureMap.addIndex(j, Integer.parseInt(items[k].substring(0,colon)),y);
							double value;
							if (items[k].substring(colon+1).indexOf('.') != -1) {
								value = Double.parseDouble(items[k].substring(colon+1));
							} else {
								value = Integer.parseInt(items[k].substring(colon+1));
							}
							featureList.add(index,value);
						}
					} catch (NumberFormatException e) {
						throw new LibException("The instance file contain a non-numeric value '"+items[k]+"'", e);
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new LibException("Couln't read from the instance file. ", e);
		}
		return y;
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
}
