package gr.uom.java.xmi.decomposition;

public class MethodInvocationReplacement extends Replacement {
	private OperationInvocation invokedOperationBefore;
	private OperationInvocation invokedOperationAfter;
	
	public MethodInvocationReplacement(String before, String after, OperationInvocation invokedOperationBefore, OperationInvocation invokedOperationAfter) {
		super(before, after);
		this.invokedOperationBefore = invokedOperationBefore;
		this.invokedOperationAfter = invokedOperationAfter;
	}

	public OperationInvocation getInvokedOperationBefore() {
		return invokedOperationBefore;
	}

	public OperationInvocation getInvokedOperationAfter() {
		return invokedOperationAfter;
	}

}
