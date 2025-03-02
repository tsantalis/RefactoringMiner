package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractCall;

public class MethodInvocationWithClassInstanceCreationReplacement extends Replacement {
	private AbstractCall invokedOperationBefore;
	private AbstractCall objectCreationAfter;
	
	public MethodInvocationWithClassInstanceCreationReplacement(String before, String after, AbstractCall invokedOperationBefore,
			AbstractCall objectCreationAfter, ReplacementType type) {
		super(before, after, type);
		this.invokedOperationBefore = invokedOperationBefore;
		this.objectCreationAfter = objectCreationAfter;
	}

	public AbstractCall getInvokedOperationBefore() {
		return invokedOperationBefore;
	}

	public AbstractCall getObjectCreationAfter() {
		return objectCreationAfter;
	}

}
