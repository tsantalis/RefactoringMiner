package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RenameVariableRefactoring implements Refactoring {
	private VariableDeclaration originalVariable;
	private VariableDeclaration renamedVariable;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;

	public RenameVariableRefactoring(
			VariableDeclaration originalVariable,
			VariableDeclaration renamedVariable,
			UMLOperation operationBefore,
			UMLOperation operationAfter) {
		this.originalVariable = originalVariable;
		this.renamedVariable = renamedVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public RefactoringType getRefactoringType() {
		if(originalVariable == null || renamedVariable == null)
			return RefactoringType.RENAME_VARIABLE;
		if(originalVariable.isParameter() && renamedVariable.isParameter())
			return RefactoringType.RENAME_PARAMETER;
		return RefactoringType.RENAME_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalVariable);
		sb.append(" to ");
		sb.append(renamedVariable);
		sb.append(" in method ");
		sb.append(operationAfter);
		sb.append(" in class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalVariable == null) ? 0 : originalVariable.hashCode());
		result = prime * result + ((renamedVariable == null) ? 0 : renamedVariable.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RenameVariableRefactoring other = (RenameVariableRefactoring) obj;
		if (operationAfter == null) {
			if (other.operationAfter != null)
				return false;
		} else if (!operationAfter.equals(other.operationAfter))
			return false;
		if (operationBefore == null) {
			if (other.operationBefore != null)
				return false;
		} else if (!operationBefore.equals(other.operationBefore))
			return false;
		if (originalVariable == null) {
			if (other.originalVariable != null)
				return false;
		} else if (!originalVariable.equals(other.originalVariable))
			return false;
		if (renamedVariable == null) {
			if (other.renamedVariable != null)
				return false;
		} else if (!renamedVariable.equals(other.renamedVariable))
			return false;
		return true;
	}

}
