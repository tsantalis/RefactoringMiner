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

public class MergeVariableRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private Set<VariableDeclaration> mergedVariables;
	private VariableDeclaration newVariable;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> variableReferences;
	private boolean insideExtractedOrInlinedMethod;
	
	public MergeVariableRefactoring(Set<VariableDeclaration> mergedVariables, VariableDeclaration newVariable,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, Set<AbstractCodeMapping> variableReferences,
			boolean insideExtractedOrInlinedMethod) {
		this.mergedVariables = mergedVariables;
		this.newVariable = newVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.variableReferences = variableReferences;
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public Set<VariableDeclaration> getMergedVariables() {
		return mergedVariables;
	}

	public VariableDeclaration getNewVariable() {
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

	public boolean isInsideExtractedOrInlinedMethod() {
		return insideExtractedOrInlinedMethod;
	}

	private boolean allVariablesAreParameters() {
		for(VariableDeclaration declaration : mergedVariables) {
			if(!declaration.isParameter()) {
				return false;
			}
		}
		return newVariable.isParameter();
	}

	public RefactoringType getRefactoringType() {
		if(allVariablesAreParameters())
			return RefactoringType.MERGE_PARAMETER;
		return RefactoringType.MERGE_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
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
		MergeVariableRefactoring other = (MergeVariableRefactoring) obj;
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

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(VariableDeclaration mergedVariable : mergedVariables) {
			ranges.add(mergedVariable.codeRange()
					.setDescription("merged variable declaration")
					.setCodeElement(mergedVariable.toString()));
		}
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(newVariable.codeRange()
				.setDescription("new variable declaration")
				.setCodeElement(newVariable.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with merged variables")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}
}
