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

public class ChangeVariableTypeRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private VariableDeclaration originalVariable;
	private VariableDeclaration changedTypeVariable;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> variableReferences;
	private Set<Refactoring> relatedRefactorings;
	private boolean insideExtractedOrInlinedMethod;

	public ChangeVariableTypeRefactoring(VariableDeclaration originalVariable, VariableDeclaration changedTypeVariable,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, Set<AbstractCodeMapping> variableReferences,
			boolean insideExtractedOrInlinedMethod) {
		this.originalVariable = originalVariable;
		this.changedTypeVariable = changedTypeVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.variableReferences = variableReferences;
		this.relatedRefactorings = new LinkedHashSet<Refactoring>();
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public void addRelatedRefactoring(Refactoring refactoring) {
		this.relatedRefactorings.add(refactoring);
	}

	public Set<Refactoring> getRelatedRefactorings() {
		return relatedRefactorings;
	}

	public RefactoringType getRefactoringType() {
		if(originalVariable.isParameter() && changedTypeVariable.isParameter())
			return RefactoringType.CHANGE_PARAMETER_TYPE;
		return RefactoringType.CHANGE_VARIABLE_TYPE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public VariableDeclaration getOriginalVariable() {
		return originalVariable;
	}

	public VariableDeclaration getChangedTypeVariable() {
		return changedTypeVariable;
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
		boolean qualified = originalVariable.equalType(changedTypeVariable) && !originalVariable.equalQualifiedType(changedTypeVariable);
		sb.append(getName()).append("\t");
		sb.append(qualified ? originalVariable.toQualifiedString() : originalVariable.toString());
		sb.append(" to ");
		sb.append(qualified ? changedTypeVariable.toQualifiedString() : changedTypeVariable.toString());
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(qualified ? operationAfter.toQualifiedString() : operationAfter.toString());
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedTypeVariable == null) ? 0 : changedTypeVariable.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalVariable == null) ? 0 : originalVariable.hashCode());
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
		ChangeVariableTypeRefactoring other = (ChangeVariableTypeRefactoring) obj;
		if (changedTypeVariable == null) {
			if (other.changedTypeVariable != null)
				return false;
		} else if (!changedTypeVariable.equals(other.changedTypeVariable))
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
		if (originalVariable == null) {
			if (other.originalVariable != null)
				return false;
		} else if (!originalVariable.equals(other.originalVariable))
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
		ranges.add(changedTypeVariable.codeRange()
				.setDescription("changed-type variable declaration")
				.setCodeElement(changedTypeVariable.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with changed variable type")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}
}
