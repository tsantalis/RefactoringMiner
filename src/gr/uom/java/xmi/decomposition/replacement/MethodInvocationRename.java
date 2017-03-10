package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.OperationInvocation;

public class MethodInvocationRename extends MethodInvocationReplacement {

	public MethodInvocationRename(String before, String after, OperationInvocation invokedOperationBefore, OperationInvocation invokedOperationAfter) {
		super(before, after, invokedOperationBefore, invokedOperationAfter);
	}

}
