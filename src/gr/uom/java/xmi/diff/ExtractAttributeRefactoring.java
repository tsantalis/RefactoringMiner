package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public class ExtractAttributeRefactoring implements Refactoring {
	private UMLAttribute attributeDeclaration;
	private Set<AbstractCodeMapping> references;

	public ExtractAttributeRefactoring(UMLAttribute variableDeclaration) {
		this.attributeDeclaration = variableDeclaration;
		this.references = new LinkedHashSet<AbstractCodeMapping>();
	}

	public void addReference(AbstractCodeMapping mapping) {
		references.add(mapping);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLAttribute getVariableDeclaration() {
		return attributeDeclaration;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(attributeDeclaration);
		sb.append(" in class ");
		sb.append(attributeDeclaration.getClassName());
		return sb.toString();
	}

	/**
	 * @return the code range of the extracted variable declaration in the <b>child</b> commit
	 */
	public CodeRange getExtractedVariableDeclarationCodeRange() {
		return attributeDeclaration.codeRange();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeDeclaration == null) ? 0 : attributeDeclaration.hashCode());
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
		ExtractAttributeRefactoring other = (ExtractAttributeRefactoring) obj;
		if (attributeDeclaration == null) {
			if (other.attributeDeclaration != null)
				return false;
		} else if (!attributeDeclaration.equals(other.attributeDeclaration))
			return false;
		return true;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(attributeDeclaration.getClassName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(attributeDeclaration.getClassName());
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeDeclaration.codeRange()
				.setDescription("extracted attribute declaration")
				.setCodeElement(attributeDeclaration.toString()));
		return ranges;
	}
}
