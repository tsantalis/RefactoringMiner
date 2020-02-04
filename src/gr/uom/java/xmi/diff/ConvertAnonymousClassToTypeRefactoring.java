package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	public Set<String> getInvolvedFilesBeforeRefactoring() {
		HashSet<String> files = new HashSet<>();
		files.add(anonymousClass.getLocationInfo().getFilePath());
		return files;
	}

	public Set<String> getInvolvedFilesAfterRefactoring() {
		HashSet<String> files = new HashSet<>();
		files.add(addedClass.getLocationInfo().getFilePath());
		return files;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.CONVERT_ANONYMOUS_CLASS_TO_TYPE;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getAnonymousClass().getName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getAddedClass().getName());
		return classNames;
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
