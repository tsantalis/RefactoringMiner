package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RenameVariableRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private VariableDeclaration originalVariable;
	private VariableDeclaration renamedVariable;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> variableReferences;
	private boolean insideExtractedOrInlinedMethod;

	public RenameVariableRefactoring(
			VariableDeclaration originalVariable,
			VariableDeclaration renamedVariable,
			VariableDeclarationContainer operationBefore,
			VariableDeclarationContainer operationAfter,
			Set<AbstractCodeMapping> variableReferences,
			boolean insideExtractedOrInlinedMethod) {
		this.originalVariable = originalVariable;
		this.renamedVariable = renamedVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.variableReferences = variableReferences;
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public RefactoringType getRefactoringType() {
		if(originalVariable.isParameter() && renamedVariable.isParameter())
			return RefactoringType.RENAME_PARAMETER;
		if(originalVariable.isAttribute() && renamedVariable.isParameter())
			return RefactoringType.PARAMETERIZE_ATTRIBUTE;
		if(!originalVariable.isParameter() && renamedVariable.isParameter())
			return RefactoringType.PARAMETERIZE_VARIABLE;
		if(!originalVariable.isAttribute() && renamedVariable.isAttribute())
			return RefactoringType.REPLACE_VARIABLE_WITH_ATTRIBUTE;
		if(originalVariable.isAttribute() && !renamedVariable.isAttribute())
			return RefactoringType.REPLACE_ATTRIBUTE_WITH_VARIABLE;
		if(originalVariable.isParameter() && renamedVariable.isLocalVariable())
			return RefactoringType.LOCALIZE_PARAMETER;
		return RefactoringType.RENAME_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public VariableDeclaration getOriginalVariable() {
		return originalVariable;
	}

	public VariableDeclaration getRenamedVariable() {
		return renamedVariable;
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

	public boolean isInsideExtractedOrInlinedMethod() {
		return insideExtractedOrInlinedMethod;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalVariable);
		sb.append(" to ");
		sb.append(renamedVariable);
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

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalVariable.codeRange()
				.setDescription("original variable declaration")
				.setCodeElement(originalVariable.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedVariable.codeRange()
				.setDescription("renamed variable declaration")
				.setCodeElement(renamedVariable.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with renamed variable")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}
}
