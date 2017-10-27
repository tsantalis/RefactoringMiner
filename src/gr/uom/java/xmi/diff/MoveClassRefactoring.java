package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLBaseClass;
import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveClassRefactoring implements Refactoring {

	private UMLBaseClass originalClass;
	private UMLBaseClass movedClass;
	
	public MoveClassRefactoring(UMLBaseClass originalClass,  UMLBaseClass movedClass) {
		this.originalClass = originalClass;
		this.movedClass = movedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(getOriginalClassName());
		sb.append(" moved to ");
		sb.append(getMovedClassName());
		return sb.toString();
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = separatorPosOfCommonSuffix('.', getOriginalClassName(), getMovedClassName());
		String originalPath = getOriginalClassName().substring(0, getOriginalClassName().length() - separatorPos);
		String movedPath = getMovedClassName().substring(0, getMovedClassName().length() - separatorPos);
		return new RenamePattern(originalPath, movedPath);
	}

	private int separatorPosOfCommonSuffix(char separator, String s1, String s2) {
		int l1 = s1.length();
		int l2 = s2.length();
		int separatorPos = -1; 
		int lmin = Math.min(s1.length(), s2.length());
		boolean equal = true;
		for (int i = 0; i < lmin; i++) {
			char c1 = s1.charAt(l1 - i - 1);
			char c2 = s2.charAt(l2 - i - 1);
			equal = equal && c1 == c2;
			if (equal && c1 == separator) {
				separatorPos = i;
			}
		}
		return separatorPos;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getMovedClassName() {
		return movedClass.getName();
	}

	public UMLBaseClass getOriginalClass() {
		return originalClass;
	}

	public UMLBaseClass getMovedClass() {
		return movedClass;
	}

}
