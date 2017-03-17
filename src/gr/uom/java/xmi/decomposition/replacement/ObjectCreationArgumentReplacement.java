package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.ObjectCreation;

public class ObjectCreationArgumentReplacement extends Replacement {
	private ObjectCreation createdObjectBefore;
	private ObjectCreation createdObjectAfter;

	public ObjectCreationArgumentReplacement(String before, String after, ObjectCreation createdObjectBefore, ObjectCreation createdObjectAfter) {
		super(before, after);
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
