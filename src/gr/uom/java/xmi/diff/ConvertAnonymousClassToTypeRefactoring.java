package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;

public class ConvertAnonymousClassToTypeRefactoring implements Refactoring {
	private UMLAnonymousClass anonymousClass;
	private UMLClass addedClass;
	
	public ConvertAnonymousClassToTypeRefactoring(UMLAnonymousClass anonymousClass, UMLClass addedClass) {
		this.anonymousClass = anonymousClass;
		this.addedClass = addedClass;
	}

	public UMLAnonymousClass getAnonymousClass() {
		return anonymousClass;
	}

	public UMLClass getAddedClass() {
		return addedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(anonymousClass);
		sb.append(" was converted to ");
		sb.append(addedClass);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.CONVERT_ANONYMOUS_CLASS_TO_TYPE;
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
