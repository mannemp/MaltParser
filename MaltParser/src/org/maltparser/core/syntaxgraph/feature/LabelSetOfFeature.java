package org.maltparser.core.syntaxgraph.feature;

import java.util.LinkedHashMap;
import java.util.Map;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.helper.HashSet;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;
import org.maltparser.core.syntaxgraph.SyntaxGraphException;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

public class LabelSetOfFeature implements FeatureFunction {
	public enum LabelSetOfRelation {
		LDEPS, RDEPS, DEPS
	};
	private final DataFormatInstance dataFormatInstance;
	protected ColumnDescription column;
	protected AddressFunction addressFunction;
	protected SymbolTableHandler tableHandler;
	protected SymbolTable table;
	protected SingleFeatureValue featureValue;
	protected LabelSetOfRelation labelsetOfRelation;
	protected String labelsetOfRelationName;
	
	public LabelSetOfFeature(SymbolTableHandler tableHandler, DataFormatInstance dataformat) throws MaltChainedException {
		super();
		dataFormatInstance = dataformat; 
		featureValue = new SingleFeatureValue(this);
		setTableHandler(tableHandler);
	}
	
	/**
	 * Initialize the distance feature function
	 * 
	 * @param arguments an array of arguments with the type returned by getParameterTypes()
	 * @throws MaltChainedException
	 */
	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 2) {
			throw new SyntaxGraphException("Could not initialize LabelSetOfFeature: number of arguments are not correct. ");
		}
		// Checks that the two arguments are address functions
		if (!(arguments[0] instanceof AddressFunction)) {
			throw new SyntaxGraphException("Could not initialize LabelSetOfFeature: the first argument is not an address function. ");
		}
		if (!(arguments[1] instanceof java.lang.String)) {
			throw new SyntaxGraphException("Could not initialize LabelSetOfFeature: the second argument (relation) is not a string. ");
		}
		setAddressFunction((AddressFunction)arguments[0]);
		setNumOfRelation((String)arguments[1]);
		// Creates a symbol table called "NUMOF" using one null value
		setSymbolTable(tableHandler.addSymbolTable("LABELSETOF", ColumnDescription.DEPENDENCY_EDGE_LABEL, "one"));
		ColumnDescription column = dataFormatInstance.getColumnDescriptionByName("DEPREL");
		if (column == null) {
			throw new SyntaxGraphException("Could not initialize OutputColumnFeature: the output column type '"+(String)arguments[0]+"' could not be found in the data format specification. ' ");
		}
		setColumn(column);
	}
	
	protected void setColumn(ColumnDescription column) {
		this.column = column;
	}
	
	/**
	 * Returns an array of class types used by the feature extraction system to invoke initialize with
	 * correct arguments.
	 * 
	 * @return an array of class types
	 */
	public Class<?>[] getParameterTypes() {
		Class<?>[] paramTypes = { org.maltparser.core.feature.function.AddressFunction.class, 
								  java.lang.String.class};
		return paramTypes; 
	}
	
	/**
	 * Returns the string representation of the integer <code>code</code> according to the numof feature function. 
	 * 
	 * @param code the integer representation of the symbol
	 * @return the string representation of the integer <code>code</code> according to the numof feature function.
	 * @throws MaltChainedException
	 */
	public String getSymbol(int code) throws MaltChainedException {
		return table.getSymbolCodeToString(code);
	}
	
	/**
	 * Returns the integer representation of the string <code>symbol</code> according to the numof feature function.
	 * 
	 * @param symbol the string representation of the symbol
	 * @return the integer representation of the string <code>symbol</code> according to the numof feature function.
	 * @throws MaltChainedException
	 */
	public int getCode(String symbol) throws MaltChainedException {
		return table.getSymbolStringToCode(symbol);
	}
	
	/**
	 * Cause the numof feature function to update the cardinality of the feature value.
	 * 
	 * @throws MaltChainedException
	 */
	public void updateCardinality() {
//		featureValue.setCardinality(table.getValueCounter()); 
	}
	
	/**
	 * Cause the feature function to update the feature value.
	 * 
	 * @throws MaltChainedException
	 */
	public void update() throws MaltChainedException {
		// Retrieve the address value 
		final AddressValue arg1 = addressFunction.getAddressValue();
		// if arg1 or arg2 is null, then set a NO_NODE null value as feature value
		if (arg1.getAddress() == null ) { 
			featureValue.setIndexCode(table.getNullValueCode(NullValueId.NO_NODE));
			featureValue.setSymbol(table.getNullValueSymbol(NullValueId.NO_NODE));
			featureValue.setNullValue(true);			
		} else {
			// Unfortunately this method takes a lot of time  arg1.getAddressClass().asSubclass(org.maltparser.core.syntaxgraph.node.DependencyNode.class);
			// Cast the address arguments to dependency nodes
			final DependencyNode node = (DependencyNode)arg1.getAddress();
			HashSet<String> uDeprels = new HashSet<String>();
			
			/*if (!node.isRoot()) {
				if (node.hasHead()) {
					int indexCode = node.getHeadEdge().getLabelCode(column.getSymbolTable());
					String symbol = column.getSymbolTable().getSymbolCodeToString(indexCode);
					if (column.getType() == ColumnDescription.STRING) {
						featureValue.update(indexCode, symbol, false, 1);
					} else {
						castFeatureValue(symbol);
					}
				} else {
					featureValue.update(column.getSymbolTable().getNullValueCode(NullValueId.NO_VALUE), 
							column.getSymbolTable().getNullValueSymbol(NullValueId.NO_VALUE), true, 1);
				}	
			} else {
				featureValue.update(column.getSymbolTable().getNullValueCode(NullValueId.ROOT_NODE), 
						column.getSymbolTable().getNullValueSymbol(NullValueId.ROOT_NODE), true, 1);
			}*/
			
			if (labelsetOfRelation == LabelSetOfRelation.DEPS) {
				for(DependencyNode child:node.getLeftDependents())
					uDeprels.add(child.getHeadEdgeLabelSymbol(column.getSymbolTable()));
				for(DependencyNode child:node.getRightDependents())
					uDeprels.add(child.getHeadEdgeLabelSymbol(column.getSymbolTable()));
			} else if (labelsetOfRelation == LabelSetOfRelation.LDEPS) {
				for(DependencyNode child:node.getLeftDependents())
					uDeprels.add(child.getHeadEdgeLabelSymbol(column.getSymbolTable()));
			} else if (labelsetOfRelation == LabelSetOfRelation.RDEPS) {
				for(DependencyNode child:node.getRightDependents())
					uDeprels.add(child.getHeadEdgeLabelSymbol(column.getSymbolTable()));
			}
			if(uDeprels.size() !=0)
			{
				String uDeprelStr = uDeprels.toString();
				featureValue.setIndexCode(table.addSymbol(uDeprelStr));
				featureValue.setSymbol(uDeprelStr);
				featureValue.setNullValue(false);
			}
			else
			{
				featureValue.update(table.getNullValueCode(NullValueId.NO_VALUE), 
						table.getNullValueSymbol(NullValueId.NO_VALUE), true, 1);				
			}
		}
		featureValue.setValue(1);
//		featureValue.setKnown(true);
	}
	
	public void setNumOfRelation(String numOfRelationName) {
		this.labelsetOfRelationName = numOfRelationName;
		labelsetOfRelation = LabelSetOfRelation.valueOf(numOfRelationName.toUpperCase());
	}
	
	public LabelSetOfRelation getNumOfRelation() {
		return labelsetOfRelation;
	}
	
	/**
	 * Returns the feature value
	 * 
	 * @return the feature value
	 */
	public FeatureValue getFeatureValue() {
		return featureValue;
	}
	
	/**
	 * Returns the symbol table used by the numof feature function
	 * 
	 * @return the symbol table used by the numof feature function
	 */
	public SymbolTable getSymbolTable() {
		return table;
	}
	
	/**
	 * Returns the address function 
	 * 
	 * @return the address function 
	 */
	public AddressFunction getAddressFunction() {
		return addressFunction;
	}


	/**
	 * Sets the address function 
	 * 
	 * @param addressFunction a address function 
	 */
	public void setAddressFunction(AddressFunction addressFunction) {
		this.addressFunction = addressFunction;
	}
	
	/**
	 * Sets the symbol table handler
	 * 
	 * @param tableHandler a symbol table handler
	 */
	public void setTableHandler(SymbolTableHandler tableHandler) {
		this.tableHandler = tableHandler;
	}

	/**
	 * Sets the symbol table used by the numof feature function
	 * 
	 * @param table
	 */
	public void setSymbolTable(SymbolTable table) {
		this.table = table;
	}
	
	public  int getType() {
		return ColumnDescription.STRING;
	}
	
	public String getMapIdentifier() {
		return getSymbolTable().getName();
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return obj.toString().equals(this.toString());
	}
	
	public int hashCode() {
		return 217 + (null == toString() ? 0 : toString().hashCode());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("LabelSetOf(");
		sb.append(addressFunction.toString());
		sb.append(", ");
		sb.append(labelsetOfRelationName);
		sb.append(')');
		return sb.toString();
	}
}
