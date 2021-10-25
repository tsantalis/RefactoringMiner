package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.ObjectCreation;

public class MethodInvocationWithClassInstanceCreationReplacement extends Replacement {
	private AbstractCall invokedOperationBefore;
	private ObjectCreation objectCreationAfter;
	
	public MethodInvocationWithClassInstanceCreationReplacement(String before, String after, ReplacementType type,
			AbstractCall invokedOperationBefore, ObjectCreation objectCreationAfter) {
		super(before, after, type);
		this.invokedOperationBefore = invokedOperationBefore;
		this.objectCreationAfter = objectCreationAfter;
	}

	public AbstractCall getInvokedOperationBefore() {
		return invokedOperationBefore;
	}

	public ObjectCreation getObjectCreationAfter() {
		return objectCreationAfter;
	}

}
