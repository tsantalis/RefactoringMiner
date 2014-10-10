package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;

public class PullUpAttributeRefactoring extends MoveAttributeRefactoring {

	public PullUpAttributeRefactoring(UMLAttribute movedAttribute,
			String sourceClassName, String targetClassName) {
		super(movedAttribute, sourceClassName, targetClassName);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(movedAttribute);
		sb.append(" from class ");
		sb.append(sourceClassName);
		sb.append(" to class ");
		sb.append(targetClassName);
		return sb.toString();
	}

	@Override
	public String getName() {
		return "Pull Up Attribute";
	}
}
