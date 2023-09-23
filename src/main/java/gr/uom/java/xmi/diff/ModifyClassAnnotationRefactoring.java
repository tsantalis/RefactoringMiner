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

public class ModifyClassAnnotationRefactoring implements Refactoring {
	private UMLAnnotation annotationBefore;
	private UMLAnnotation annotationAfter;
	private UMLAbstractClass classBefore;
	private UMLAbstractClass classAfter;

	public ModifyClassAnnotationRefactoring(UMLAnnotation annotationBefore, UMLAnnotation annotationAfter,
			UMLAbstractClass classBefore, UMLAbstractClass classAfter) {
		this.annotationBefore = annotationBefore;
		this.annotationAfter = annotationAfter;
		this.classBefore = classBefore;
		this.classAfter = classAfter;
	}

	public UMLAnnotation getAnnotationBefore() {
		return annotationBefore;
	}

	public UMLAnnotation getAnnotationAfter() {
		return annotationAfter;
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
		ranges.add(annotationBefore.codeRange()
				.setDescription("original annotation")
				.setCodeElement(annotationBefore.toString()));
		ranges.add(classBefore.codeRange()
				.setDescription("original class declaration")
				.setCodeElement(classBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(annotationAfter.codeRange()
				.setDescription("modified annotation")
				.setCodeElement(annotationAfter.toString()));
		ranges.add(classAfter.codeRange()
				.setDescription("class declaration with modified annotation")
				.setCodeElement(classAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MODIFY_CLASS_ANNOTATION;
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
		sb.append(annotationBefore);
		sb.append(" to ");
		sb.append(annotationAfter);
		sb.append(" in class ");
		sb.append(classAfter.getName());
		return sb.toString();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotationAfter == null) ? 0 : annotationAfter.hashCode());
		result = prime * result + ((annotationBefore == null) ? 0 : annotationBefore.hashCode());
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
		ModifyClassAnnotationRefactoring other = (ModifyClassAnnotationRefactoring) obj;
		if (annotationAfter == null) {
			if (other.annotationAfter != null)
				return false;
		} else if (!annotationAfter.equals(other.annotationAfter))
			return false;
		if (annotationBefore == null) {
			if (other.annotationBefore != null)
				return false;
		} else if (!annotationBefore.equals(other.annotationBefore))
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
