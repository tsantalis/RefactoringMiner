package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.ObjectCreation;

public class ObjectCreationReplacement extends Replacement {
	private ObjectCreation createdObjectBefore;
	private ObjectCreation createdObjectAfter;

	public ObjectCreationReplacement(String before, String after,
			ObjectCreation createdObjectBefore, ObjectCreation createdObjectAfter,
			ReplacementType type) {
		super(before, after, type);
		this.createdObjectBefore = createdObjectBefore;
		this.createdObjectAfter = createdObjectAfter;
	}

	public ObjectCreation getCreatedObjectBefore() {
		return createdObjectBefore;
	}

	public ObjectCreation getCreatedObjectAfter() {
		return createdObjectAfter;
	}

}
