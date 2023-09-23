package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLAnnotation;

public class UMLAnnotationListDiff {
	private Set<UMLAnnotation> removedAnnotations;
	private Set<UMLAnnotation> addedAnnotations;
	private Set<UMLAnnotationDiff> annotationDiffs;
	private Set<Pair<UMLAnnotation, UMLAnnotation>> commonAnnotations;

	public UMLAnnotationListDiff(List<UMLAnnotation> annotations1, List<UMLAnnotation> annotations2) {
		this.removedAnnotations = new LinkedHashSet<UMLAnnotation>();
		this.addedAnnotations = new LinkedHashSet<UMLAnnotation>();
		this.annotationDiffs = new LinkedHashSet<UMLAnnotationDiff>();
		this.commonAnnotations = new LinkedHashSet<Pair<UMLAnnotation,UMLAnnotation>>();
		Set<Pair<UMLAnnotation, UMLAnnotation>> matchedAnnotations = new LinkedHashSet<Pair<UMLAnnotation,UMLAnnotation>>();
		for(UMLAnnotation annotation1 : annotations1) {
			boolean found = false;
			for(UMLAnnotation annotation2 : annotations2) {
				if(annotation1.equals(annotation2)) {
					matchedAnnotations.add(Pair.of(annotation1, annotation2));
					found = true;
					break;
				}
			}
			if(!found) {
				for(UMLAnnotation annotation2 : annotations2) {
					if(annotation1.getTypeName().equals(annotation2.getTypeName())) {
						matchedAnnotations.add(Pair.of(annotation1, annotation2));
						found = true;
						break;
					}
				}
			}
			if(!found) {
				removedAnnotations.add(annotation1);
			}
		}
		for(UMLAnnotation annotation2 : annotations2) {
			boolean found = false;
			for(UMLAnnotation annotation1 : annotations1) {
				if(annotation1.equals(annotation2)) {
					matchedAnnotations.add(Pair.of(annotation1, annotation2));
					found = true;
					break;
				}
			}
			if(!found) {
				for(UMLAnnotation annotation1 : annotations1) {
					if(annotation1.getTypeName().equals(annotation2.getTypeName())) {
						matchedAnnotations.add(Pair.of(annotation1, annotation2));
						found = true;
						break;
					}
				}
			}
			if(!found) {
				addedAnnotations.add(annotation2);
			}
		}
		for(Pair<UMLAnnotation, UMLAnnotation> pair : matchedAnnotations) {
			UMLAnnotationDiff annotationDiff = new UMLAnnotationDiff(pair.getLeft(), pair.getRight());
			if(!annotationDiff.isEmpty() && !annotationDiffs.contains(annotationDiff)) {
				annotationDiffs.add(annotationDiff);
			}
			else if(!commonAnnotations.contains(pair)){
				commonAnnotations.add(pair);
			}
		}
	}

	public Set<UMLAnnotation> getRemovedAnnotations() {
		return removedAnnotations;
	}

	public Set<UMLAnnotation> getAddedAnnotations() {
		return addedAnnotations;
	}

	public Set<UMLAnnotationDiff> getAnnotationDiffs() {
		return annotationDiffs;
	}

	public Set<Pair<UMLAnnotation, UMLAnnotation>> getCommonAnnotations() {
		return commonAnnotations;
	}

	public boolean isEmpty() {
		return removedAnnotations.isEmpty() && addedAnnotations.isEmpty() && annotationDiffs.isEmpty();
	}
}
