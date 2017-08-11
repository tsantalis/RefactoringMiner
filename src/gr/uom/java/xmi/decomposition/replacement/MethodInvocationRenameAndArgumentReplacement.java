package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.OperationInvocation;

public class MethodInvocationRenameAndArgumentReplacement extends MethodInvocationReplacement {

	public MethodInvocationRenameAndArgumentReplacement(String before, String after,
			OperationInvocation invokedOperationBefore, OperationInvocation invokedOperationAfter) {
		super(before, after, invokedOperationBefore, invokedOperationAfter);
	}

}
