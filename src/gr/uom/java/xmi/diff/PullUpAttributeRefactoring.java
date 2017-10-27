package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLBaseClass;
import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;

public class PullUpAttributeRefactoring extends MoveAttributeRefactoring {

	public PullUpAttributeRefactoring(UMLAttribute movedAttribute,
									  UMLBaseClass sourceClass, UMLBaseClass targetClass) {
		super(movedAttribute, sourceClass, targetClass);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(movedAttribute);
		sb.append(" from class ");
		sb.append(sourceClass);
		sb.append(" to class ");
		sb.append(targetClass);
		return sb.toString();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PULL_UP_ATTRIBUTE;
	}
}
