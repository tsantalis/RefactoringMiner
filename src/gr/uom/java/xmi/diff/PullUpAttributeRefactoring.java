package gr.uom.java.xmi.diff;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;

public class PullUpAttributeRefactoring extends MoveAttributeRefactoring {

	public PullUpAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
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
		return RefactoringType.PULL_UP_ATTRIBUTE;
	}
}
