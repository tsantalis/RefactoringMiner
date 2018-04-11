package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveClassRefactoring implements Refactoring {
	private String originalClassName;
	private String movedClassName;
	
	public MoveClassRefactoring(String originalClassName,  String movedClassName) {
		this.originalClassName = originalClassName;
		this.movedClassName = movedClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClassName);
		sb.append(" moved to ");
		sb.append(movedClassName);
		return sb.toString();
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = separatorPosOfCommonSuffix('.', originalClassName, movedClassName);
		if (separatorPos == -1) {
			return new RenamePattern(originalClassName, movedClassName);
		}
		String originalPath = originalClassName.substring(0, originalClassName.length() - separatorPos);
		String movedPath = movedClassName.substring(0, movedClassName.length() - separatorPos);
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
		return originalClassName;
	}

	public String getMovedClassName() {
		return movedClassName;
	}
	
}
