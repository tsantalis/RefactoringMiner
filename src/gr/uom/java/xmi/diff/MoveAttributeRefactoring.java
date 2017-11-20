package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;

public class MoveAttributeRefactoring implements Refactoring {
	protected UMLAttribute originalAttribute;
	protected UMLAttribute movedAttribute;
	
	public MoveAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
		this.originalAttribute = originalAttribute;
		this.movedAttribute = movedAttribute;
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

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_ATTRIBUTE;
	}

	public UMLAttribute getOriginalAttribute() {
		return originalAttribute;
	}

	public UMLAttribute getMovedAttribute() {
		return movedAttribute;
	}

	public String getSourceClassName() {
		return originalAttribute.getClassName();
	}

	public String getTargetClassName() {
		return movedAttribute.getClassName();
	}
}
