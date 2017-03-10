package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.OperationInvocation;

public class MethodInvocationArgumentReplacement extends MethodInvocationReplacement {

	public MethodInvocationArgumentReplacement(String before, String after, OperationInvocation invokedOperationBefore, OperationInvocation invokedOperationAfter) {
		super(before, after, invokedOperationBefore, invokedOperationAfter);
	}

}
