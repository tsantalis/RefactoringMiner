package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;

public class ExtractClassRefactoring implements Refactoring {
	private UMLClass extractedClass;
	private UMLClass originalClass;
	private Set<UMLOperation> extractedOperations;
	private Set<UMLAttribute> extractedAttributes;
	private UMLAttribute attributeOfExtractedClassTypeInOriginalClass;

	public ExtractClassRefactoring(UMLClass extractedClass, UMLClass originalClass,
			Set<UMLOperation> extractedOperations, Set<UMLAttribute> extractedAttributes, UMLAttribute attributeOfExtractedClassType) {
		this.extractedClass = extractedClass;
		this.originalClass = originalClass;
		this.extractedOperations = extractedOperations;
		this.extractedAttributes = extractedAttributes;
		this.attributeOfExtractedClassTypeInOriginalClass = attributeOfExtractedClassType;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedClass);
		sb.append(" from class ");
		sb.append(originalClass);
		return sb.toString();
	}

	public RefactoringType getRefactoringType() {
		if(extractedClass.isSubTypeOf(originalClass))
			return RefactoringType.EXTRACT_SUBCLASS;
		return RefactoringType.EXTRACT_CLASS;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLClass getExtractedClass() {
		return extractedClass;
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public Set<UMLOperation> getExtractedOperations() {
		return extractedOperations;
	}

	public Set<UMLAttribute> getExtractedAttributes() {
		return extractedAttributes;
	}

	public UMLAttribute getAttributeOfExtractedClassTypeInOriginalClass() {
		return attributeOfExtractedClassTypeInOriginalClass;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getOriginalClass().getName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getExtractedClass().getName());
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalClass.codeRange()
				.setDescription("original type declaration")
				.setCodeElement(originalClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(extractedClass.codeRange()
				.setDescription("extracted type declaration")
				.setCodeElement(extractedClass.getName()));
		return ranges;
	}
}
