package gr.uom.java.xmi.diff;

import java.util.Set;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public class CandidateMergeVariableRefactoring implements ReferenceBasedRefactoring {
	private Set<String> mergedVariables;
	private String newVariable;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> variableReferences;
	private Set<UMLAttribute> mergedAttributes;
	private UMLAttribute newAttribute;

	public CandidateMergeVariableRefactoring(Set<String> mergedVariables, String newVariable,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, Set<AbstractCodeMapping> variableReferences) {
		this.mergedVariables = mergedVariables;
		this.newVariable = newVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.variableReferences = variableReferences;
	}

	public Set<String> getMergedVariables() {
		return mergedVariables;
	}

	public String getNewVariable() {
		return newVariable;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return variableReferences;
	}

	public Set<UMLAttribute> getMergedAttributes() {
		return mergedAttributes;
	}

	public void setMergedAttributes(Set<UMLAttribute> mergedAttributes) {
		this.mergedAttributes = mergedAttributes;
	}

	public UMLAttribute getNewAttribute() {
		return newAttribute;
	}

	public void setNewAttribute(UMLAttribute newAttribute) {
		this.newAttribute = newAttribute;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Merge Attribute").append("\t");
		sb.append(mergedVariables);
		sb.append(" to ");
		sb.append(newVariable);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mergedVariables == null) ? 0 : mergedVariables.hashCode());
		result = prime * result + ((newVariable == null) ? 0 : newVariable.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
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
		CandidateMergeVariableRefactoring other = (CandidateMergeVariableRefactoring) obj;
		if (mergedVariables == null) {
			if (other.mergedVariables != null)
				return false;
		} else if (!mergedVariables.equals(other.mergedVariables))
			return false;
		if (newVariable == null) {
			if (other.newVariable != null)
				return false;
		} else if (!newVariable.equals(other.newVariable))
			return false;
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
		return true;
	}
}
