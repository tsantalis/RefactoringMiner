package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveAndRenameClassRefactoring implements Refactoring {

	private UMLClass originalClass;
	private UMLClass renamedClass;
	
	public MoveAndRenameClassRefactoring(UMLClass originalClass,  UMLClass renamedClass) {
		this.originalClass = originalClass;
		this.renamedClass = renamedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClass.getName());
		sb.append(" moved and renamed to ");
		sb.append(renamedClass.getName());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getRenamedClassName() {
		return renamedClass.getName();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getRenamedClass() {
		return renamedClass;
	}


}
