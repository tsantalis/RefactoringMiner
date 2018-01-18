package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.OperationInvocation;

public class VariableReplacementWithMethodInvocation extends Replacement {
	private OperationInvocation invokedOperation;
	
	public VariableReplacementWithMethodInvocation(String before, String after, OperationInvocation invocation) {
		super(before, after, ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION);
		this.invokedOperation = invocation;
	}

	public OperationInvocation getInvokedOperation() {
		return invokedOperation;
	}
}
