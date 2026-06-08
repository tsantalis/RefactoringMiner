package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

public class ExtractSuperclassRefactoring extends ChangeTypeRefactoring {
	private UMLClass extractedClass;
	private Set<UMLClass> subclassSetBefore;
	private Set<UMLClass> subclassSetAfter;
	
	public ExtractSuperclassRefactoring(UMLClass extractedClass, Set<UMLClass> subclassSetBefore, Set<UMLClass> subclassSetAfter) {
		this.extractedClass = extractedClass;
		this.subclassSetBefore = subclassSetBefore;
		this.subclassSetAfter = subclassSetAfter;
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return subclassSetBefore.iterator().next();
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return extractedClass;
	}

	@Override
	public Optional<String> getTemplateParameterBefore() {return Optional.of(extractedClass.toString());}

	@Override
	public String getTemplateParameterAfter() {return subclassSetBefore.toString();}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		if(extractedClass.isInterface())
			return RefactoringType.EXTRACT_INTERFACE;
		else
			return RefactoringType.EXTRACT_SUPERCLASS;
	}

	public UMLClass getExtractedClass() {
		return extractedClass;
	}

	public Set<String> getSubclassSetBefore() {
		Set<String> subclassSet = new LinkedHashSet<String>();
		for(UMLClass umlClass : this.subclassSetBefore) {
			subclassSet.add(umlClass.getName());
		}
		return subclassSet;
	}

	public Set<UMLClass> getUMLSubclassSetBefore() {
		return new LinkedHashSet<UMLClass>(subclassSetBefore);
	}

	public Set<String> getSubclassSetAfter() {
		Set<String> subclassSet = new LinkedHashSet<String>();
		for(UMLClass umlClass : this.subclassSetAfter) {
			subclassSet.add(umlClass.getName());
		}
		return subclassSet;
	}

	public Set<UMLClass> getUMLSubclassSetAfter() {
		return new LinkedHashSet<UMLClass>(subclassSetAfter);
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(UMLClass umlClass : this.subclassSetBefore) {
			pairs.add(new ImmutablePair<String, String>(umlClass.getLocationInfo().getFilePath(), umlClass.getName()));
		}
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(UMLClass umlClass : this.subclassSetAfter) {
			pairs.add(new ImmutablePair<String, String>(umlClass.getLocationInfo().getFilePath(), umlClass.getName()));
		}
		pairs.add(new ImmutablePair<String, String>(getExtractedClass().getLocationInfo().getFilePath(), getExtractedClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(UMLClass subclass : subclassSetBefore) {
			ranges.add(subclass.codeRange()
					.setDescription("original sub-type declaration")
					.setCodeElement(subclass.getName()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(UMLClass subclass : subclassSetAfter) {
			ranges.add(subclass.codeRange()
					.setDescription("sub-type declaration after extraction")
					.setCodeElement(subclass.getName()));
		}
		ranges.add(extractedClass.codeRange()
				.setDescription("extracted super-type declaration")
				.setCodeElement(extractedClass.getName()));
		return ranges;
	}
}
