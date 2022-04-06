package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;

public class RemoveClassAnnotationRefactoring implements Refactoring {
	private UMLAnnotation annotation;
	private UMLAbstractClass classBefore;
	private UMLAbstractClass classAfter;

	public RemoveClassAnnotationRefactoring(UMLAnnotation annotation, UMLAbstractClass classBefore, UMLAbstractClass classAfter) {
		this.annotation = annotation;
		this.classBefore = classBefore;
		this.classAfter = classAfter;
	}

	public UMLAnnotation getAnnotation() {
		return annotation;
	}

	public UMLAbstractClass getClassBefore() {
		return classBefore;
	}

	public UMLAbstractClass getClassAfter() {
		return classAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(annotation.codeRange()
				.setDescription("removed annotation")
				.setCodeElement(annotation.toString()));
		ranges.add(classBefore.codeRange()
				.setDescription("original class declaration")
				.setCodeElement(classBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(classAfter.codeRange()
				.setDescription("class declaration with removed annotation")
				.setCodeElement(classAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.REMOVE_CLASS_ANNOTATION;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getClassBefore().getLocationInfo().getFilePath(), getClassBefore().getName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getClassAfter().getLocationInfo().getFilePath(), getClassAfter().getName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(annotation);
		sb.append(" in class ");
		sb.append(classBefore.getName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((classAfter == null) ? 0 : classAfter.hashCode());
		result = prime * result + ((classBefore == null) ? 0 : classBefore.hashCode());
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
		RemoveClassAnnotationRefactoring other = (RemoveClassAnnotationRefactoring) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (classAfter == null) {
			if (other.classAfter != null)
				return false;
		} else if (!classAfter.equals(other.classAfter))
			return false;
		if (classBefore == null) {
			if (other.classBefore != null)
				return false;
		} else if (!classBefore.equals(other.classBefore))
			return false;
		return true;
	}
}
