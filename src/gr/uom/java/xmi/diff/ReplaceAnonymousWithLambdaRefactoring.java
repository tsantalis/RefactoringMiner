package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;

public class ReplaceAnonymousWithLambdaRefactoring implements Refactoring {
	private UMLAnonymousClass anonymousClass;
	private LambdaExpressionObject lambda;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;
	private UMLAttribute attributeBefore;
	private UMLAttribute attributeAfter;

	public ReplaceAnonymousWithLambdaRefactoring(UMLAnonymousClass anonymousClass, LambdaExpressionObject lambda,
			UMLOperation operationBefore, UMLOperation operationAfter) {
		this.anonymousClass = anonymousClass;
		this.lambda = lambda;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public ReplaceAnonymousWithLambdaRefactoring(UMLAnonymousClass anonymousClass, LambdaExpressionObject lambda,
			UMLAttribute attributeBefore, UMLAttribute attributeAfter) {
		this.anonymousClass = anonymousClass;
		this.lambda = lambda;
		this.attributeBefore = attributeBefore;
		this.attributeAfter = attributeAfter;
	}

	public UMLAnonymousClass getAnonymousClass() {
		return anonymousClass;
	}

	public LambdaExpressionObject getLambda() {
		return lambda;
	}

	public UMLOperation getOperationBefore() {
		return operationBefore;
	}

	public UMLOperation getOperationAfter() {
		return operationAfter;
	}

	public UMLAttribute getAttributeBefore() {
		return attributeBefore;
	}

	public UMLAttribute getAttributeAfter() {
		return attributeAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(anonymousClass.codeRange()
				.setDescription("anonymous class declaration")
				.setCodeElement(anonymousClass.getCodePath()));
		if(operationBefore != null)  {
			ranges.add(operationBefore.codeRange()
					.setDescription("original method declaration")
					.setCodeElement(operationBefore.toString()));
		}
		if(attributeBefore != null) {
			ranges.add(attributeBefore.codeRange()
					.setDescription("original attribute declaration")
					.setCodeElement(attributeBefore.toString()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		String string = lambda.toString();
		String lambdaString = string.contains("\n") ? string.substring(0, string.indexOf("\n")) : string;
		ranges.add(lambda.codeRange()
				.setDescription("lambda expression")
				.setCodeElement(lambdaString));
		if(operationAfter != null) {
			ranges.add(operationAfter.codeRange()
					.setDescription("method declaration with introduced lambda")
					.setCodeElement(operationAfter.toString()));
		}
		if(attributeAfter != null) {
			ranges.add(attributeAfter.codeRange()
					.setDescription("attribute declaration with introduced lambda")
					.setCodeElement(attributeAfter.toString()));
		}
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.REPLACE_ANONYMOUS_WITH_LAMBDA;
	}

	@Override
	public String getName() {
		return getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		if(getOperationBefore() != null)
			pairs.add(new ImmutablePair<String, String>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
		if(getAttributeBefore() != null)
			pairs.add(new ImmutablePair<String, String>(getAttributeBefore().getLocationInfo().getFilePath(), getAttributeBefore().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		if(getOperationAfter() != null)
			pairs.add(new ImmutablePair<String, String>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
		if(getAttributeAfter() != null)
			pairs.add(new ImmutablePair<String, String>(getAttributeAfter().getLocationInfo().getFilePath(), getAttributeAfter().getClassName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(anonymousClass.getCodePath());
		sb.append(" with ");
		String string = lambda.toString();
		sb.append(string.contains("\n") ? string.substring(0, string.indexOf("\n")) : string);
		if(operationAfter != null) {
			sb.append(" in method ");
			sb.append(operationAfter);
			sb.append(" from class ");
			sb.append(operationAfter.getClassName());
		}
		if(attributeAfter != null) {
			sb.append(" in attribute ");
			sb.append(attributeAfter);
			sb.append(" from class ");
			sb.append(attributeAfter.getClassName());
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((anonymousClass == null) ? 0 : anonymousClass.hashCode());
		result = prime * result + ((attributeAfter == null) ? 0 : attributeAfter.getVariableDeclaration().hashCode());
		result = prime * result + ((attributeBefore == null) ? 0 : attributeBefore.getVariableDeclaration().hashCode());
		result = prime * result + ((lambda == null) ? 0 : lambda.hashCode());
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
		ReplaceAnonymousWithLambdaRefactoring other = (ReplaceAnonymousWithLambdaRefactoring) obj;
		if (anonymousClass == null) {
			if (other.anonymousClass != null)
				return false;
		} else if (!anonymousClass.equals(other.anonymousClass))
			return false;
		if (attributeAfter == null) {
			if (other.attributeAfter != null)
				return false;
		} else if (!attributeAfter.getVariableDeclaration().equals(other.attributeAfter.getVariableDeclaration()))
			return false;
		if (attributeBefore == null) {
			if (other.attributeBefore != null)
				return false;
		} else if (!attributeBefore.getVariableDeclaration().equals(other.attributeBefore.getVariableDeclaration()))
			return false;
		if (lambda == null) {
			if (other.lambda != null)
				return false;
		} else if (!lambda.equals(other.lambda))
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
