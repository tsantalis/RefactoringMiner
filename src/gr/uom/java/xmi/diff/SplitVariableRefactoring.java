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

public class SplitVariableRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private Set<VariableDeclaration> splitVariables;
	private VariableDeclaration oldVariable;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> variableReferences;
	private boolean insideExtractedOrInlinedMethod;
	
	public SplitVariableRefactoring(VariableDeclaration oldVariable, Set<VariableDeclaration> splitVariables,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, Set<AbstractCodeMapping> variableReferences,
			boolean insideExtractedOrInlinedMethod) {
		this.splitVariables = splitVariables;
		this.oldVariable = oldVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.variableReferences = variableReferences;
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public Set<VariableDeclaration> getSplitVariables() {
		return splitVariables;
	}

	public VariableDeclaration getOldVariable() {
		return oldVariable;
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
		for(VariableDeclaration declaration : splitVariables) {
			if(!declaration.isParameter()) {
				return false;
			}
		}
		return oldVariable.isParameter();
	}

	@Override
	public RefactoringType getRefactoringType() {
		if(allVariablesAreParameters())
			return RefactoringType.SPLIT_PARAMETER;
		return RefactoringType.SPLIT_VARIABLE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(oldVariable);
		sb.append(" to ");
		sb.append(splitVariables);
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
		result = prime * result + ((oldVariable == null) ? 0 : oldVariable.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((splitVariables == null) ? 0 : splitVariables.hashCode());
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
		SplitVariableRefactoring other = (SplitVariableRefactoring) obj;
		if (oldVariable == null) {
			if (other.oldVariable != null)
				return false;
		} else if (!oldVariable.equals(other.oldVariable))
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
		if (splitVariables == null) {
			if (other.splitVariables != null)
				return false;
		} else if (!splitVariables.equals(other.splitVariables))
			return false;
		return true;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(oldVariable.codeRange()
				.setDescription("original variable declaration")
				.setCodeElement(oldVariable.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(VariableDeclaration splitVariable : splitVariables) {
			ranges.add(splitVariable.codeRange()
					.setDescription("split variable declaration")
					.setCodeElement(splitVariable.toString()));
		}
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with split variable")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}
}
