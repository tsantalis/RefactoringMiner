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

public class ExtractVariableRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private VariableDeclaration variableDeclaration;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private Set<AbstractCodeMapping> references;
	private boolean insideExtractedOrInlinedMethod;

	public ExtractVariableRefactoring(VariableDeclaration variableDeclaration,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter,
			boolean insideExtractedOrInlinedMethod) {
		this.variableDeclaration = variableDeclaration;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.references = new LinkedHashSet<AbstractCodeMapping>();
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public void addReference(AbstractCodeMapping mapping) {
		references.add(mapping);
	}

	public void addReferences(Set<AbstractCodeMapping> mappings) {
		references.addAll(mappings);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public VariableDeclaration getVariableDeclaration() {
		return variableDeclaration;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public boolean isInsideExtractedOrInlinedMethod() {
		return insideExtractedOrInlinedMethod;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(variableDeclaration);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ");
		sb.append(operationAfter.getClassName());
		return sb.toString();
	}

	/**
	 * @return the code range of the extracted variable declaration in the <b>child</b> commit
	 */
	public CodeRange getExtractedVariableDeclarationCodeRange() {
		return variableDeclaration.codeRange();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((variableDeclaration == null) ? 0 : variableDeclaration.hashCode());
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
		ExtractVariableRefactoring other = (ExtractVariableRefactoring) obj;
		if (operationAfter == null) {
			if (other.operationAfter != null)
				return false;
		} else if (!operationAfter.equals(other.operationAfter))
			return false;
		if (variableDeclaration == null) {
			if (other.variableDeclaration != null)
				return false;
		} else if (!variableDeclaration.equals(other.variableDeclaration))
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
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment1().codeRange().setDescription("statement with the initializer of the extracted variable"));
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
		ranges.add(variableDeclaration.codeRange()
				.setDescription("extracted variable declaration")
				.setCodeElement(variableDeclaration.toString()));
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment2().codeRange().setDescription("statement with the name of the extracted variable"));
		}
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with extracted variable")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}
}
