package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class AddThrownExceptionTypeRefactoring extends ThrownExceptionTypeRefactoring implements MethodLevelRefactoring {
	private UMLType exceptionType;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;

	public AddThrownExceptionTypeRefactoring(UMLType exceptionType, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.exceptionType = exceptionType;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return operationBefore;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return operationAfter;
	}

	public String getTemplateParameterAfter() {
		return exceptionType.toString();
	}

	public UMLType getExceptionType() {
		return exceptionType;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(operationBefore.codeRange()
				.setDescription("original method declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(exceptionType.codeRange()
				.setDescription("added thrown exception type")
				.setCodeElement(exceptionType.toString()));
		ranges.add(operationAfter.codeRange()
				.setDescription("method declaration with added thrown exception type")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.ADD_THROWN_EXCEPTION_TYPE;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exceptionType == null) ? 0 : exceptionType.hashCode());
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
		AddThrownExceptionTypeRefactoring other = (AddThrownExceptionTypeRefactoring) obj;
		if (exceptionType == null) {
			if (other.exceptionType != null)
				return false;
		} else if (!exceptionType.equals(other.exceptionType))
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
