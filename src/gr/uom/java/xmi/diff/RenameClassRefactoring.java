package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLBaseClass;
import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class RenameClassRefactoring implements Refactoring {

	private UMLBaseClass originalClass;
	private UMLBaseClass renamedClass;
	
	public RenameClassRefactoring(UMLBaseClass originalClass,  UMLBaseClass renamedClass) {
		this.originalClass = originalClass;
		this.renamedClass = renamedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(getOriginalClassName());
		sb.append(" renamed to ");
		sb.append(getRenamedClassName());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getRenamedClassName() {
		return renamedClass.getName();
	}

	public UMLBaseClass getOriginalClass() {
		return originalClass;
	}

	public UMLBaseClass getRenamedClass() {
		return renamedClass;
	}
	
}
