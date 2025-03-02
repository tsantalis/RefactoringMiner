package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractCall;

public class ClassInstanceCreationWithMethodInvocationReplacement extends Replacement {
	private AbstractCall objectCreationBefore;
	private AbstractCall invokedOperationAfter;

	public ClassInstanceCreationWithMethodInvocationReplacement(String before, String after, AbstractCall objectCreationBefore,
			AbstractCall invokedOperationAfter, ReplacementType type) {
		super(before, after, type);
		this.objectCreationBefore = objectCreationBefore;
		this.invokedOperationAfter = invokedOperationAfter;
	}

	public AbstractCall getObjectCreationBefore() {
		return objectCreationBefore;
	}

	public AbstractCall getInvokedOperationAfter() {
		return invokedOperationAfter;
	}

}
