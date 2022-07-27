package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.UMLAttribute;

public class ChangeAttributeTypeRefactoring implements Refactoring, ReferenceBasedRefactoring {
	private UMLAttribute originalAttribute;
	private UMLAttribute changedTypeAttribute;
	private String classNameBefore;
	private String classNameAfter;
	private Set<AbstractCodeMapping> attributeReferences;
	private Set<Refactoring> relatedRefactorings;
	
	public ChangeAttributeTypeRefactoring(UMLAttribute originalAttribute,
										  UMLAttribute changedTypeAttribute, Set<AbstractCodeMapping> attributeReferences) {
		this.originalAttribute = originalAttribute;
		this.changedTypeAttribute = changedTypeAttribute;
		this.classNameBefore = originalAttribute.getClassName();
		this.classNameAfter = changedTypeAttribute.getClassName();
		this.attributeReferences = attributeReferences;
		this.relatedRefactorings = new LinkedHashSet<Refactoring>();
	}

	public void addRelatedRefactoring(Refactoring refactoring) {
		this.relatedRefactorings.add(refactoring);
	}

	public Set<Refactoring> getRelatedRefactorings() {
		return relatedRefactorings;
	}

	public UMLAttribute getOriginalAttribute() {
		return originalAttribute;
	}

	public UMLAttribute getChangedTypeAttribute() {
		return changedTypeAttribute;
	}

	public String getClassNameBefore() {
		return classNameBefore;
	}

	public String getClassNameAfter() {
		return classNameAfter;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return attributeReferences;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_ATTRIBUTE_TYPE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		VariableDeclaration originalVariableDeclaration = originalAttribute.getVariableDeclaration();
		VariableDeclaration changedTypeVariableDeclaration = changedTypeAttribute.getVariableDeclaration();
		boolean qualified = originalVariableDeclaration.equalType(changedTypeVariableDeclaration) && !originalVariableDeclaration.equalQualifiedType(changedTypeVariableDeclaration);
		sb.append(getName()).append("\t");
		sb.append(qualified ? originalVariableDeclaration.toQualifiedString() : originalVariableDeclaration.toString());
		sb.append(" to ");
		sb.append(qualified ? changedTypeVariableDeclaration.toQualifiedString() : changedTypeVariableDeclaration.toString());
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedTypeAttribute == null || changedTypeAttribute.getVariableDeclaration() == null) ? 0 : changedTypeAttribute.getVariableDeclaration().hashCode());
		result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
		result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
		result = prime * result + ((originalAttribute == null || originalAttribute.getVariableDeclaration() == null) ? 0 : originalAttribute.getVariableDeclaration().hashCode());
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
		ChangeAttributeTypeRefactoring other = (ChangeAttributeTypeRefactoring) obj;
		if (changedTypeAttribute == null) {
			if (other.changedTypeAttribute != null)
				return false;
		} else if(changedTypeAttribute.getVariableDeclaration() == null) {
			if(other.changedTypeAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!changedTypeAttribute.getVariableDeclaration().equals(other.changedTypeAttribute.getVariableDeclaration()))
			return false;
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
		} else if(originalAttribute.getVariableDeclaration() == null) {
			if(other.originalAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!originalAttribute.getVariableDeclaration().equals(other.originalAttribute.getVariableDeclaration()))
			return false;
		return true;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalAttribute().getLocationInfo().getFilePath(), getClassNameBefore()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getChangedTypeAttribute().getLocationInfo().getFilePath(), getClassNameAfter()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalAttribute.getVariableDeclaration().codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(originalAttribute.getVariableDeclaration().toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(changedTypeAttribute.getVariableDeclaration().codeRange()
				.setDescription("changed-type attribute declaration")
				.setCodeElement(changedTypeAttribute.getVariableDeclaration().toString()));
		return ranges;
	}
}
