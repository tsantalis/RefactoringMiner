package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAnnotation;

public class MoveAnnotationRefactoring extends AnnotationRefactoring {
	private UMLAnnotation originalAnnotation;
	private UMLAnnotation movedAnnotation;
	private AnnotationProvider originalAnnotationProvider;
	private AnnotationProvider movedAnnotationProvider;

	public MoveAnnotationRefactoring(UMLAnnotation annotationBefore, UMLAnnotation annotationAfter,
			AnnotationProvider annotationProviderBefore, AnnotationProvider annotationProviderAfter) {
		this.originalAnnotation = annotationBefore;
		this.movedAnnotation = annotationAfter;
		this.originalAnnotationProvider = annotationProviderBefore;
		this.movedAnnotationProvider = annotationProviderAfter;
	}

	@Override
	public UMLAnnotation getAnnotation() {
		return movedAnnotation;
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return originalAnnotationProvider;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return movedAnnotationProvider;
	}

	public boolean addCodeElementDescription() {return false;}

	public UMLAnnotation getOriginalAnnotation() {
		return originalAnnotation;
	}

	public UMLAnnotation getMovedAnnotation() {
		return movedAnnotation;
	}

	public AnnotationProvider getOriginalAnnotationProvider() {
		return originalAnnotationProvider;
	}

	public AnnotationProvider getMovedAnnotationProvider() {
		return movedAnnotationProvider;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalAnnotation.codeRange()
				.setDescription("original annotation")
				.setCodeElement(originalAnnotation.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(movedAnnotation.codeRange()
				.setDescription("moved annotation")
				.setCodeElement(movedAnnotation.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_ANNOTATION;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalAnnotationProvider().getLocationInfo().getFilePath(), getOriginalAnnotationProvider().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getMovedAnnotationProvider().getLocationInfo().getFilePath(), getMovedAnnotationProvider().getClassName()));
		return pairs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(movedAnnotation, movedAnnotationProvider, originalAnnotation, originalAnnotationProvider);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveAnnotationRefactoring other = (MoveAnnotationRefactoring) obj;
		return Objects.equals(movedAnnotation, other.movedAnnotation)
				&& Objects.equals(movedAnnotationProvider, other.movedAnnotationProvider)
				&& Objects.equals(originalAnnotation, other.originalAnnotation)
				&& Objects.equals(originalAnnotationProvider, other.originalAnnotationProvider);
	}
}
