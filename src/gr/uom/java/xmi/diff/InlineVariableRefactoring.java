package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class InlineVariableRefactoring implements Refactoring {
	private VariableDeclaration variableDeclaration;
	private UMLOperation operation;
	private Set<AbstractCodeMapping> references;

	public InlineVariableRefactoring(VariableDeclaration variableDeclaration, UMLOperation operation) {
		this.variableDeclaration = variableDeclaration;
		this.operation = operation;
		this.references = new LinkedHashSet<AbstractCodeMapping>();
	}

	public void addReference(AbstractCodeMapping mapping) {
		references.add(mapping);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.INLINE_VARIABLE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public VariableDeclaration getVariableDeclaration() {
		return variableDeclaration;
	}

	public UMLOperation getOperation() {
		return operation;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(variableDeclaration);
		sb.append(" in method ");
		sb.append(operation);
		sb.append(" from class ");
		sb.append(operation.getClassName());
		return sb.toString();
	}

	/**
	 * @return the code range of the inlined variable declaration in the <b>parent</b> commit
	 */
	public CodeRange getInlinedVariableDeclarationCodeRange() {
		return variableDeclaration.codeRange();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operation == null) ? 0 : operation.hashCode());
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
		InlineVariableRefactoring other = (InlineVariableRefactoring) obj;
		if (operation == null) {
			if (other.operation != null)
				return false;
		} else if (!operation.equals(other.operation))
			return false;
		if (variableDeclaration == null) {
			if (other.variableDeclaration != null)
				return false;
		} else if (!variableDeclaration.equals(other.variableDeclaration))
			return false;
		return true;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(operation.getClassName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(operation.getClassName());
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(variableDeclaration.codeRange()
				.setDescription("inlined variable declaration")
				.setCodeElement(variableDeclaration.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		return ranges;
	}
}
