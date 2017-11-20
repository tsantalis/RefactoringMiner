package gr.uom.java.xmi.diff;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;

public class PushDownAttributeRefactoring extends MoveAttributeRefactoring {

	public PushDownAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
		super(originalAttribute, movedAttribute);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(movedAttribute);
		sb.append(" from class ");
		sb.append(getSourceClassName());
		sb.append(" to class ");
		sb.append(getTargetClassName());
		return sb.toString();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PUSH_DOWN_ATTRIBUTE;
	}
}
