package gr.uom.java.xmi.decomposition;

public class VariableReplacementWithMethodInvocation extends Replacement {
	private OperationInvocation invokedOperation;
	
	public VariableReplacementWithMethodInvocation(String before, String after, OperationInvocation invocation) {
		super(before, after);
		this.invokedOperation = invocation;
	}

	public OperationInvocation getInvokedOperation() {
		return invokedOperation;
	}
}
