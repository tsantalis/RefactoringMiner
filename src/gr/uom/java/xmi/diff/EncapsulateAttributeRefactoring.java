package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;

public class EncapsulateAttributeRefactoring implements Refactoring {
	private UMLAttribute attributeBefore;
	private UMLAttribute attributeAfter;
	private UMLOperation addedGetter;
	private UMLOperation addedSetter;

	public EncapsulateAttributeRefactoring(UMLAttribute attributeBefore, UMLAttribute attributeAfter,
			UMLOperation addedGetter, UMLOperation addedSetter) {
		this.attributeBefore = attributeBefore;
		this.attributeAfter = attributeAfter;
		this.addedGetter = addedGetter;
		this.addedSetter = addedSetter;
	}

	public UMLAttribute getAttributeBefore() {
		return attributeBefore;
	}

	public UMLAttribute getAttributeAfter() {
		return attributeAfter;
	}

	public UMLOperation getAddedGetter() {
		return addedGetter;
	}

	public UMLOperation getAddedSetter() {
		return addedSetter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeBefore.codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(attributeBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeAfter.codeRange()
				.setDescription("encapsulated attribute declaration")
				.setCodeElement(attributeAfter.toString()));
		if(addedGetter != null) {
			ranges.add(addedGetter.codeRange()
					.setDescription("added getter method")
					.setCodeElement(addedGetter.toString()));
		}
		if(addedSetter != null) {
			ranges.add(addedSetter.codeRange()
					.setDescription("added setter method")
					.setCodeElement(addedSetter.toString()));
		}
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.ENCAPSULATE_ATTRIBUTE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getAttributeBefore().getLocationInfo().getFilePath(), getAttributeBefore().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getAttributeAfter().getLocationInfo().getFilePath(), getAttributeAfter().getClassName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(attributeAfter);
		sb.append(" from class ");
		sb.append(attributeAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeAfter == null || attributeAfter.getVariableDeclaration() == null) ? 0 : attributeAfter.getVariableDeclaration().hashCode());
		result = prime * result + ((attributeBefore == null || attributeBefore.getVariableDeclaration() == null) ? 0 : attributeBefore.getVariableDeclaration().hashCode());
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
		EncapsulateAttributeRefactoring other = (EncapsulateAttributeRefactoring) obj;
		if (attributeBefore == null) {
			if (other.attributeBefore != null)
				return false;
		} else if(attributeBefore.getVariableDeclaration() == null) {
			if(other.attributeBefore.getVariableDeclaration() != null)
				return false;
		} else if (!attributeBefore.getVariableDeclaration().equals(other.attributeBefore.getVariableDeclaration()))
			return false;
		if (attributeAfter == null) {
			if (other.attributeAfter != null)
				return false;
		} else if(attributeAfter.getVariableDeclaration() == null) {
			if(other.attributeAfter.getVariableDeclaration() != null)
				return false;
		} else if (!attributeAfter.getVariableDeclaration().equals(other.attributeAfter.getVariableDeclaration()))
			return false;
		return true;
	}
}
