package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class ExtractVariableRefactoring implements Refactoring {
	private String variableName;
	private UMLOperation operation;

	public ExtractVariableRefactoring(VariableDeclaration variableDeclaration, UMLOperation operation) {
		this.variableName = variableDeclaration.getVariableName();
		this.operation = operation;
	}

	public ExtractVariableRefactoring(String variableName, UMLOperation operation) {
		String[] tokens = variableName.split("\\s");
		this.variableName = tokens[tokens.length-1];
		this.operation = operation;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(variableName);
		sb.append(" in method ");
		sb.append(operation);
		sb.append(" from class ");
		sb.append(operation.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operation == null) ? 0 : operation.hashCode());
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
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
		ExtractVariableRefactoring other = (ExtractVariableRefactoring) obj;
		if (operation == null) {
			if (other.operation != null)
				return false;
		} else if (!operation.equals(other.operation))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		return true;
	}
}
