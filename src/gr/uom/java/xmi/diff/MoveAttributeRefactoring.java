package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLBaseClass;
import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;

public class MoveAttributeRefactoring implements Refactoring {

	protected UMLAttribute movedAttribute;
	protected UMLBaseClass sourceClass;
	protected UMLBaseClass targetClass;
	
	public MoveAttributeRefactoring(UMLAttribute movedAttribute,
									UMLBaseClass sourceClass, UMLBaseClass targetClass) {

		this.movedAttribute = movedAttribute;
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
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

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_ATTRIBUTE;
	}

  public UMLAttribute getMovedAttribute() {
    return movedAttribute;
  }

  public String getSourceClassName() {
    return sourceClass.getName();
  }

  public String getTargetClassName() {
    return targetClass.getName();
  }

	public UMLBaseClass getSourceClass() {
		return sourceClass;
	}

	public UMLBaseClass getTargetClass() {
		return targetClass;
	}

}
