package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class AddVariableAnnotationRefactoring implements Refactoring {
	private UMLAnnotation annotation;
	private VariableDeclaration variableBefore;
	private VariableDeclaration variableAfter;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private boolean insideExtractedOrInlinedMethod;
	
	public AddVariableAnnotationRefactoring(UMLAnnotation annotation, VariableDeclaration variableBefore,
			VariableDeclaration variableAfter, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter,
			boolean insideExtractedOrInlinedMethod) {
		this.annotation = annotation;
		this.variableBefore = variableBefore;
		this.variableAfter = variableAfter;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public UMLAnnotation getAnnotation() {
		return annotation;
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
		ranges.add(annotation.codeRange()
				.setDescription("added annotation")
				.setCodeElement(annotation.toString()));
		ranges.add(variableAfter.codeRange()
				.setDescription("variable declaration with added annotation")
				.setCodeElement(variableAfter.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with added variable annotation")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		if(variableBefore.isParameter() && variableAfter.isParameter())
			return RefactoringType.ADD_PARAMETER_ANNOTATION;
		if(variableAfter.isParameter())
			return RefactoringType.ADD_PARAMETER_ANNOTATION;
		else
			return RefactoringType.ADD_VARIABLE_ANNOTATION;
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
		sb.append(annotation);
		if(variableAfter.isParameter())
			sb.append(" in parameter ");
		else
			sb.append(" in variable ");
		sb.append(variableAfter);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ");
		sb.append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
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
		AddVariableAnnotationRefactoring other = (AddVariableAnnotationRefactoring) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
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
