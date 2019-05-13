package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RenameAttributeRefactoring implements Refactoring {
	private VariableDeclaration originalAttribute;
	private VariableDeclaration renamedAttribute;
	private Set<CandidateAttributeRefactoring> attributeRenames;
	private String classNameBefore;
	private String classNameAfter;

	public RenameAttributeRefactoring(VariableDeclaration originalAttribute, VariableDeclaration renamedAttribute,
			String classNameBefore, String classNameAfter, Set<CandidateAttributeRefactoring> attributeRenames) {
		this.originalAttribute = originalAttribute;
		this.renamedAttribute = renamedAttribute;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.attributeRenames = attributeRenames;
	}

	public VariableDeclaration getOriginalAttribute() {
		return originalAttribute;
	}

	public VariableDeclaration getRenamedAttribute() {
		return renamedAttribute;
	}

	public Set<CandidateAttributeRefactoring> getAttributeRenames() {
		return attributeRenames;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalAttribute);
		sb.append(" to ");
		sb.append(renamedAttribute);
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
		result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
		result = prime * result + ((originalAttribute == null) ? 0 : originalAttribute.hashCode());
		result = prime * result + ((renamedAttribute == null) ? 0 : renamedAttribute.hashCode());
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
		RenameAttributeRefactoring other = (RenameAttributeRefactoring) obj;
		if (classNameAfter == null) {
			if (other.classNameAfter != null)
				return false;
		} else if (!classNameAfter.equals(other.classNameAfter))
			return false;
		if (classNameBefore == null) {
			if (other.classNameBefore != null)
				return false;
		} else if (!classNameBefore.equals(other.classNameBefore))
			return false;
		if (originalAttribute == null) {
			if (other.originalAttribute != null)
				return false;
		} else if (!originalAttribute.equals(other.originalAttribute))
			return false;
		if (renamedAttribute == null) {
			if (other.renamedAttribute != null)
				return false;
		} else if (!renamedAttribute.equals(other.renamedAttribute))
			return false;
		return true;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(classNameBefore);
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(classNameAfter);
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalAttribute.codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(originalAttribute.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedAttribute.codeRange()
				.setDescription("renamed attribute declaration")
				.setCodeElement(renamedAttribute.toString()));
		return ranges;
	}
}
