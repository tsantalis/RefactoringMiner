package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class ReplaceAnonymousWithLambdaRefactoring implements MethodLevelRefactoring {
	private UMLAnonymousClass anonymousClass;
	private LambdaExpressionObject lambda;
	private AbstractCodeFragment anonymousOwner;
	private AbstractCodeFragment lambdaOwner;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> mappings;
	private UMLOperationBodyMapper bodyMapper;

	public ReplaceAnonymousWithLambdaRefactoring(UMLAnonymousClass anonymousClass, LambdaExpressionObject lambda,
			AbstractCodeFragment anonymousOwner, AbstractCodeFragment lambdaOwner,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, UMLOperationBodyMapper mapper) {
		this.anonymousClass = anonymousClass;
		this.lambda = lambda;
		this.anonymousOwner = anonymousOwner;
		this.lambdaOwner = lambdaOwner;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.mappings = mapper.getMappings();
		this.bodyMapper = mapper;
	}

	public UMLAnonymousClass getAnonymousClass() {
		return anonymousClass;
	}

	public LambdaExpressionObject getLambda() {
		return lambda;
	}

	public AbstractCodeFragment getAnonymousOwner() {
		return anonymousOwner;
	}

	public AbstractCodeFragment getLambdaOwner() {
		return lambdaOwner;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	public Set<AbstractCodeMapping> getMappings() {
		return mappings;
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(anonymousClass.codeRange()
				.setDescription("anonymous class declaration")
				.setCodeElement(anonymousClass.getCodePath()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
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
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with introduced lambda")
				.setCodeElement(operationAfter.toString()));
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
		sb.append(anonymousClass.getCodePath());
		sb.append(" with ");
		String string = lambda.toString();
		sb.append(string.contains("\n") ? string.substring(0, string.indexOf("\n")) : string);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter.toQualifiedString());
		sb.append(" from class ");
		sb.append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((anonymousClass == null) ? 0 : anonymousClass.getLocationInfo().hashCode());
		result = prime * result + ((lambda == null) ? 0 : lambda.getLocationInfo().hashCode());
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
		} else if (!anonymousClass.getLocationInfo().equals(other.anonymousClass.getLocationInfo()))
			return false;
		if (lambda == null) {
			if (other.lambda != null)
				return false;
		} else if (!lambda.getLocationInfo().equals(other.lambda.getLocationInfo()))
			return false;
		return true;
	}
}
