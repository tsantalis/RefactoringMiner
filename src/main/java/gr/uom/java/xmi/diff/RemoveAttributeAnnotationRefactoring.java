package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLEnumConstant;

public class RemoveAttributeAnnotationRefactoring implements Refactoring {
	private UMLAnnotation annotation;
	private UMLAttribute attributeBefore;
	private UMLAttribute attributeAfter;

	public RemoveAttributeAnnotationRefactoring(UMLAnnotation annotation, UMLAttribute attributeBefore,
			UMLAttribute attributeAfter) {
		this.annotation = annotation;
		this.attributeBefore = attributeBefore;
		this.attributeAfter = attributeAfter;
	}

	public UMLAnnotation getAnnotation() {
		return annotation;
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
		ranges.add(annotation.codeRange()
				.setDescription("removed annotation")
				.setCodeElement(annotation.toString()));
		ranges.add(attributeBefore.codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(attributeBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeAfter.codeRange()
				.setDescription("attribute declaration with removed annotation")
				.setCodeElement(attributeAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.REMOVE_ATTRIBUTE_ANNOTATION;
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
		sb.append(annotation);
		if(attributeBefore instanceof UMLEnumConstant) {
			sb.append(" in enum constant ");
		}
		else {
			sb.append(" in attribute ");
		}
		sb.append(attributeBefore);
		sb.append(" from class ");
		sb.append(attributeBefore.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
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
		RemoveAttributeAnnotationRefactoring other = (RemoveAttributeAnnotationRefactoring) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
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
