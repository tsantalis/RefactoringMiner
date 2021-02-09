package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RemoveVariableAnnotationRefactoring implements Refactoring {
	private UMLAnnotation annotation;
	private VariableDeclaration variableBefore;
	private VariableDeclaration variableAfter;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;
	
	public RemoveVariableAnnotationRefactoring(UMLAnnotation annotation, VariableDeclaration variableBefore,
			VariableDeclaration variableAfter, UMLOperation operationBefore, UMLOperation operationAfter) {
		this.annotation = annotation;
		this.variableBefore = variableBefore;
		this.variableAfter = variableAfter;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
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

	public UMLOperation getOperationBefore() {
		return operationBefore;
	}

	public UMLOperation getOperationAfter() {
		return operationAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(annotation.codeRange()
				.setDescription("removed annotation")
				.setCodeElement(annotation.toString()));
		ranges.add(variableBefore.codeRange()
				.setDescription("original variable declaration")
				.setCodeElement(variableBefore.toString()));
		ranges.add(operationBefore.codeRange()
				.setDescription("original method declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(variableAfter.codeRange()
				.setDescription("variable declaration with removed annotation")
				.setCodeElement(variableAfter.toString()));
		ranges.add(operationAfter.codeRange()
				.setDescription("method declaration with removed variable annotation")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		if(variableBefore.isParameter() && variableAfter.isParameter())
			return RefactoringType.REMOVE_PARAMETER_ANNOTATION;
		if(variableBefore.isParameter())
			return RefactoringType.REMOVE_PARAMETER_ANNOTATION;
		else
			return RefactoringType.REMOVE_VARIABLE_ANNOTATION;
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
		if(variableBefore.isParameter())
			sb.append(" in parameter ");
		else
			sb.append(" in variable ");
		sb.append(variableBefore);
		sb.append(" in method ");
		sb.append(operationBefore);
		sb.append(" from class ");
		sb.append(operationBefore.getClassName());
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
		RemoveVariableAnnotationRefactoring other = (RemoveVariableAnnotationRefactoring) obj;
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
