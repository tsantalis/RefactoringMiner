package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;

public class ReplaceAnonymousWithClassRefactoring extends ChangeTypeRefactoring {
	private UMLAnonymousClass anonymousClass;
	private UMLClass addedClass;
	private UMLAnonymousToClassDiff diff;
	
	public ReplaceAnonymousWithClassRefactoring(UMLAnonymousClass anonymousClass, UMLClass addedClass, UMLAnonymousToClassDiff diff) {
		this.anonymousClass = anonymousClass;
		this.addedClass = addedClass;
		this.diff = diff;
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return anonymousClass;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return addedClass;
	}

	public Optional<String> getTemplateParameterBefore() {
		return Optional.of(anonymousClass.getCodePath());
	}

	public String getTemplateParameterAfter() {
		return addedClass.toString();
	}

	public UMLAnonymousClass getAnonymousClass() {
		return anonymousClass;
	}

	public UMLClass getAddedClass() {
		return addedClass;
	}

	public UMLAnonymousToClassDiff getDiff() {
		return diff;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.REPLACE_ANONYMOUS_WITH_CLASS;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getAnonymousClass().getLocationInfo().getFilePath(), getAnonymousClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getAddedClass().getLocationInfo().getFilePath(), getAddedClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(anonymousClass.codeRange()
				.setDescription("anonymous class declaration")
				.setCodeElement(anonymousClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(addedClass.codeRange()
				.setDescription("added type declaration")
				.setCodeElement(addedClass.getName()));
		return ranges;
	}
}
