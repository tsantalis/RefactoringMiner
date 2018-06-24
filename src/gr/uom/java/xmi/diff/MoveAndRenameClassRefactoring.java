package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveAndRenameClassRefactoring implements Refactoring {
	private String originalClassName;
	private String renamedClassName;
	
	public MoveAndRenameClassRefactoring(String originalClassName,  String renamedClassName) {
		this.originalClassName = originalClassName;
		this.renamedClassName = renamedClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClassName);
		sb.append(" moved and renamed to ");
		sb.append(renamedClassName);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClassName;
	}

	public String getRenamedClassName() {
		return renamedClassName;
	}

}
