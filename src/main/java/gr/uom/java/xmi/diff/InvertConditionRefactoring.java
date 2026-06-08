package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;

public class InvertConditionRefactoring extends AbstractRefactoring implements MethodLevelRefactoring {
	private AbstractCodeFragment originalConditional;
	private AbstractCodeFragment invertedConditional;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	
	public InvertConditionRefactoring(AbstractCodeFragment originalConditional, AbstractCodeFragment invertedConditional,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.originalConditional = originalConditional;
		this.invertedConditional = invertedConditional;
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

	public Optional<String> getTemplateParameterBefore() {
		String conditionalString = originalConditional.getString();
		String oldConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
		return Optional.of(oldConditional);
	}

	public String getTemplateParameterAfter() {
		String conditionalString = invertedConditional.getString();
		String newConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
		return newConditional;
	}

	public AbstractCodeFragment getOriginalConditional() {
		return originalConditional;
	}

	public AbstractCodeFragment getInvertedConditional() {
		return invertedConditional;
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
		ranges.add(originalConditional.codeRange()
				.setDescription("original conditional")
				.setCodeElement(originalConditional.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(invertedConditional.codeRange()
				.setDescription("inverted conditional")
				.setCodeElement(invertedConditional.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with inverted conditional")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.INVERT_CONDITION;
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
		return Objects.hash(invertedConditional, operationAfter, operationBefore, originalConditional);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvertConditionRefactoring other = (InvertConditionRefactoring) obj;
		return Objects.equals(invertedConditional, other.invertedConditional)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore)
				&& Objects.equals(originalConditional, other.originalConditional);
	}
}
