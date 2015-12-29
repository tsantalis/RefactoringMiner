package gr.uom.java.xmi.diff;

public enum RefactoringType {

	EXTRACT_OPERATION("Extract Method"),
	RENAME_CLASS("Rename Class"),
	MOVE_ATTRIBUTE("Move Attribute"),
	RENAME_METHOD("Rename Method"),
	INLINE_OPERATION("Inline Method"),
	MOVE_OPERATION("Move Method"),
	PULL_UP_OPERATION("Pull Up Method"),
	MOVE_CLASS("Move Class"),
	MOVE_RENAME_CLASS("Move And Rename Class"),
	MOVE_CLASS_FOLDER("Move Class Folder"),
	PULL_UP_ATTRIBUTE("Pull Up Attribute"),
	PUSH_DOWN_ATTRIBUTE("Push Down Attribute"),
	PUSH_DOWN_OPERATION("Push Down Method"),
	EXTRACT_INTERFACE("Extract Interface"),
	EXTRACT_SUPERCLASS("Extract Superclass"),
	MERGE_OPERATION("Merge Method"),
	EXTRACT_AND_MOVE_OPERATION("Extract And Move Method"),
	CONVERT_ANONYMOUS_CLASS_TO_TYPE("Convert Anonymous Class to Type"),
	INTRODUCE_POLYMORPHISM("Introduce Polymorphism");

	private String displayName;

	private RefactoringType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

}
