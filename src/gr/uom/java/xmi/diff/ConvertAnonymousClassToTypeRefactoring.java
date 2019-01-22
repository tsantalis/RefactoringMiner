package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;

public class ConvertAnonymousClassToTypeRefactoring implements Refactoring {
	private UMLClass baseClass;
	private UMLAnonymousClass anonymousClass;
	private UMLClass addedClass;
	
	public ConvertAnonymousClassToTypeRefactoring(UMLClass baseClass, UMLAnonymousClass anonymousClass, UMLClass addedClass) {
		this.baseClass = baseClass;
		this.anonymousClass = anonymousClass;
		this.addedClass = addedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(anonymousClass);
		sb.append(" was converted to ");
		sb.append(addedClass);
		return sb.toString();
	}

	@Override
	public String getRefactoredClass () {
		return baseClass.getName();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.CONVERT_ANONYMOUS_CLASS_TO_TYPE;
	}

}
