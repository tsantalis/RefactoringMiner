package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractCall;

public class ObjectCreationReplacement extends Replacement {
	private AbstractCall createdObjectBefore;
	private AbstractCall createdObjectAfter;

	public ObjectCreationReplacement(String before, String after,
			AbstractCall createdObjectBefore, AbstractCall createdObjectAfter,
			ReplacementType type) {
		super(before, after, type);
		this.createdObjectBefore = createdObjectBefore;
		this.createdObjectAfter = createdObjectAfter;
	}

	public AbstractCall getCreatedObjectBefore() {
		return createdObjectBefore;
	}

	public AbstractCall getCreatedObjectAfter() {
		return createdObjectAfter;
	}

}
