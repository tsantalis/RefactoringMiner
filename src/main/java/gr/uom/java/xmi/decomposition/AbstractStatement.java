package gr.uom.java.xmi.decomposition;

import java.util.List;

import gr.uom.java.xmi.VariableDeclarationContainer;

public abstract class AbstractStatement extends AbstractCodeFragment {
	private CompositeStatementObject parent;
	protected String actualSignature;

	public String getActualSignature() {
		return actualSignature;
	}

	public void setParent(CompositeStatementObject parent) {
    	this.parent = parent;
    }

    public CompositeStatementObject getParent() {
    	return this.parent;
    }

	public String getString() {
    	return toString();
    }

    public VariableDeclaration searchVariableDeclaration(String variableName) {
    	VariableDeclaration variableDeclaration = this.getVariableDeclaration(variableName);
    	if(variableDeclaration != null) {
    		return variableDeclaration;
    	}
    	else if(parent != null) {
    		return parent.searchVariableDeclaration(variableName);
    	}
    	else if(this instanceof CompositeStatementObject) {
    		CompositeStatementObject comp = (CompositeStatementObject)this;
    		if(comp.getOwner().isPresent()) {
    			VariableDeclarationContainer container = comp.getOwner().get();
    			if(container instanceof LambdaExpressionObject) {
	    			VariableDeclaration declaration = container.getVariableDeclaration(variableName);
	    			if(declaration != null) {
	    				return declaration;
	    			}
    			}
    		}
    	}
    	return null;
    }

    public abstract List<AbstractCodeFragment> getLeaves();
    public abstract int statementCount();
    public abstract int statementCountIncludingBlocks();
	public abstract List<String> stringRepresentation();
}
