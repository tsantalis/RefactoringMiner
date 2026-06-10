package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;

public class ExtractClassRefactoring extends ChangeTypeRefactoring {
	private UMLClass extractedClass;
	private UMLClassBaseDiff classDiff;
	private Map<UMLOperation, UMLOperation> extractedOperations;
	private Map<UMLAttribute, UMLAttribute> extractedAttributes;
	private UMLAttribute attributeOfExtractedClassTypeInOriginalClass;

	public ExtractClassRefactoring(UMLClass extractedClass, UMLClassBaseDiff classDiff,
			Map<UMLOperation, UMLOperation> extractedOperations, Map<UMLAttribute, UMLAttribute> extractedAttributes, UMLAttribute attributeOfExtractedClassType) {
		this.extractedClass = extractedClass;
		this.classDiff = classDiff;
		this.extractedOperations = extractedOperations;
		this.extractedAttributes = extractedAttributes;
		this.attributeOfExtractedClassTypeInOriginalClass = attributeOfExtractedClassType;
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return extractedClass;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return classDiff.getOriginalClass();
	}

	public RefactoringType getRefactoringType() {
		if(extractedClass.isSubTypeOf(classDiff.getOriginalClass()) || extractedClass.isSubTypeOf(classDiff.getNextClass()))
			return RefactoringType.EXTRACT_SUBCLASS;
		if(classDiff.getOriginalClass().isSubTypeOf(extractedClass) || classDiff.getNextClass().isSubTypeOf(extractedClass))
			return RefactoringType.EXTRACT_SUPERCLASS;
		return RefactoringType.EXTRACT_CLASS;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLClass getExtractedClass() {
		return extractedClass;
	}

	public UMLClass getOriginalClass() {
		return classDiff.getOriginalClass();
	}

	public Map<UMLOperation, UMLOperation> getExtractedOperations() {
		return extractedOperations;
	}

	public Map<UMLAttribute, UMLAttribute> getExtractedAttributes() {
		return extractedAttributes;
	}

	public UMLAttribute getAttributeOfExtractedClassTypeInOriginalClass() {
		return attributeOfExtractedClassTypeInOriginalClass;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(classDiff.getNextClass().getLocationInfo().getFilePath(), classDiff.getNextClass().getName()));
		pairs.add(new ImmutablePair<String, String>(getExtractedClass().getLocationInfo().getFilePath(), getExtractedClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(classDiff.getOriginalClass().codeRange()
				.setDescription("original type declaration")
				.setCodeElement(classDiff.getOriginalClass().getName()));
		for(UMLOperation extractedOperation : extractedOperations.keySet()) {
			ranges.add(extractedOperation.codeRange()
					.setDescription("original method declaration")
					.setCodeElement(extractedOperation.toString()));
		}
		for(UMLAttribute extractedAttribute : extractedAttributes.keySet()) {
			ranges.add(extractedAttribute.codeRange()
					.setDescription("original attribute declaration")
					.setCodeElement(extractedAttribute.toString()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(classDiff.getNextClass().codeRange()
				.setDescription("type declaration after extraction")
				.setCodeElement(classDiff.getNextClass().getName()));
		ranges.add(extractedClass.codeRange()
				.setDescription("extracted type declaration")
				.setCodeElement(extractedClass.getName()));
		for(UMLOperation extractedOperation : extractedOperations.values()) {
			ranges.add(extractedOperation.codeRange()
					.setDescription("extracted method declaration")
					.setCodeElement(extractedOperation.toString()));
		}
		for(UMLAttribute extractedAttribute : extractedAttributes.values()) {
			ranges.add(extractedAttribute.codeRange()
					.setDescription("extracted attribute declaration")
					.setCodeElement(extractedAttribute.toString()));
		}
		return ranges;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classDiff.getOriginalClass() == null) ? 0 : classDiff.getOriginalClass().hashCode());
		result = prime * result + ((extractedClass == null) ? 0 : extractedClass.hashCode());
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
		ExtractClassRefactoring other = (ExtractClassRefactoring) obj;
		if (classDiff.getOriginalClass() == null) {
			if (other.classDiff.getOriginalClass() != null)
				return false;
		} else if (!classDiff.getOriginalClass().equals(other.classDiff.getOriginalClass()))
			return false;
		if (extractedClass == null) {
			if (other.extractedClass != null)
				return false;
		} else if (!extractedClass.equals(other.extractedClass))
			return false;
		return true;
	}
}
