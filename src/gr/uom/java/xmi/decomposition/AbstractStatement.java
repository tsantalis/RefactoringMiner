package gr.uom.java.xmi.decomposition;

import java.util.List;

public abstract class AbstractStatement extends AbstractCodeFragment {
	private CompositeStatementObject parent;
	
	public void setParent(CompositeStatementObject parent) {
    	this.parent = parent;
    }

    public CompositeStatementObject getParent() {
    	return this.parent;
    }

	public String getString() {
    	return toString();
    }
    
    public abstract List<StatementObject> getLeaves();
    public abstract int statementCount();
}
