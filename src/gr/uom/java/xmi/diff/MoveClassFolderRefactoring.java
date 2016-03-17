package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveClassFolderRefactoring implements Refactoring {
	private String className;
	private String originalPath;
	private String movedPath;
	
	public MoveClassFolderRefactoring(String className, String originalPath, String movedPath) {
		this.className = className;
		int separatorPos = separatorPosOfCommonSuffix('/', originalPath, movedPath);
		this.originalPath = originalPath.substring(0, originalPath.length() - 1 - separatorPos);
		this.movedPath = movedPath.substring(0, movedPath.length() - 1 - separatorPos);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(className);
		sb.append(" moved from ");
		sb.append(originalPath);
		sb.append(" to ");
		sb.append(movedPath);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_CLASS_FOLDER;
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
	
}
