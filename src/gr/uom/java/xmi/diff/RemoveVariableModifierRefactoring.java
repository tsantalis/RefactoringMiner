package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RemoveVariableModifierRefactoring implements Refactoring {
	private String modifier;
	private VariableDeclaration variableBefore;
	private VariableDeclaration variableAfter;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private boolean insideExtractedOrInlinedMethod;

	public RemoveVariableModifierRefactoring(String modifier, VariableDeclaration variableBefore,
			VariableDeclaration variableAfter, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter,
			boolean insideExtractedOrInlinedMethod) {
		this.modifier = modifier;
		this.variableBefore = variableBefore;
		this.variableAfter = variableAfter;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public String getModifier() {
		return modifier;
	}

	public VariableDeclaration getVariableBefore() {
		return variableBefore;
	}

	public VariableDeclaration getVariableAfter() {
		return variableAfter;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	public boolean isInsideExtractedOrInlinedMethod() {
		return insideExtractedOrInlinedMethod;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(variableBefore.codeRange()
				.setDescription("original variable declaration")
				.setCodeElement(variableBefore.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(variableAfter.codeRange()
				.setDescription("variable declaration with removed modifier")
				.setCodeElement(variableAfter.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with removed variable modifier")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		if(variableBefore.isParameter() && variableAfter.isParameter())
			return RefactoringType.REMOVE_PARAMETER_MODIFIER;
		if(variableBefore.isParameter())
			return RefactoringType.REMOVE_PARAMETER_MODIFIER;
		else
			return RefactoringType.REMOVE_VARIABLE_MODIFIER;
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
		sb.append(modifier);
		if(variableBefore.isParameter())
			sb.append(" in parameter ");
		else
			sb.append(" in variable ");
		sb.append(variableBefore);
		String elementType = operationBefore.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationBefore);
		sb.append(" from class ");
		sb.append(operationBefore.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((variableAfter == null) ? 0 : variableAfter.hashCode());
		result = prime * result + ((variableBefore == null) ? 0 : variableBefore.hashCode());
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
		RemoveVariableModifierRefactoring other = (RemoveVariableModifierRefactoring) obj;
		if (modifier == null) {
			if (other.modifier != null)
				return false;
		} else if (!modifier.equals(other.modifier))
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
		if (variableAfter == null) {
			if (other.variableAfter != null)
				return false;
		} else if (!variableAfter.equals(other.variableAfter))
			return false;
		if (variableBefore == null) {
			if (other.variableBefore != null)
				return false;
		} else if (!variableBefore.equals(other.variableBefore))
			return false;
		return true;
	}
}
