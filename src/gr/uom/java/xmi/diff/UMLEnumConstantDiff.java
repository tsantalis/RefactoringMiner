package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLEnumConstant;

public class UMLEnumConstantDiff {
	private UMLEnumConstant removedEnumConstant;
	private UMLEnumConstant addedEnumConstant;
	private boolean renamed;
	private UMLAnnotationListDiff annotationListDiff;

	public UMLEnumConstantDiff(UMLEnumConstant removedEnumConstant, UMLEnumConstant addedEnumConstant) {
		this.removedEnumConstant = removedEnumConstant;
		this.addedEnumConstant = addedEnumConstant;
		if(!removedEnumConstant.getName().equals(addedEnumConstant.getName()))
			renamed = true;
		this.annotationListDiff = new UMLAnnotationListDiff(removedEnumConstant.getAnnotations(), addedEnumConstant.getAnnotations());
	}

	public UMLEnumConstant getRemovedEnumConstant() {
		return removedEnumConstant;
	}

	public UMLEnumConstant getAddedEnumConstant() {
		return addedEnumConstant;
	}

	public boolean isRenamed() {
		return renamed;
	}

	public boolean isEmpty() {
		return !renamed && annotationListDiff.isEmpty();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append("\t").append(removedEnumConstant).append("\n");
		if(renamed)
			sb.append("\t").append("renamed from " + removedEnumConstant.getName() + " to " + addedEnumConstant.getName()).append("\n");
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			sb.append("\t").append("annotation " + annotation + " removed").append("\n");
		}
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			sb.append("\t").append("annotation " + annotation + " added").append("\n");
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
			sb.append("\t").append("annotation " + annotationDiff.getRemovedAnnotation() + " modified to " + annotationDiff.getAddedAnnotation()).append("\n");
		}
		return sb.toString();
	}

	private Set<Refactoring> getAnnotationRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddAttributeAnnotationRefactoring refactoring = new AddAttributeAnnotationRefactoring(annotation, removedEnumConstant, addedEnumConstant);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveAttributeAnnotationRefactoring refactoring = new RemoveAttributeAnnotationRefactoring(annotation, removedEnumConstant, addedEnumConstant);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
			ModifyAttributeAnnotationRefactoring refactoring = new ModifyAttributeAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), removedEnumConstant, addedEnumConstant);
			refactorings.add(refactoring);
		}
		return refactorings;
	}

	public Set<Refactoring> getRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		refactorings.addAll(getAnnotationRefactorings());
		return refactorings;
	}
}
