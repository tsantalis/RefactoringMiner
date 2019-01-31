package gr.uom.java.xmi.diff;

import org.refactoringminer.util.PrefixSuffixUtils;

public class MovedClassToAnotherSourceFolder {
	private String className;
	private String originalPath;
	private String movedPath;

	public MovedClassToAnotherSourceFolder(String className, String originalPath, String movedPath) {
		this.className = className;
		this.originalPath = originalPath;
		this.movedPath = movedPath;
	}
	
	public String getClassName() {
		return className;
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('/', originalPath, movedPath);
		if (separatorPos == -1) {
			return new RenamePattern(originalPath, movedPath);
		}
		String original = originalPath.substring(0, originalPath.length() - separatorPos);
		String moved = movedPath.substring(0, movedPath.length() - separatorPos);
		return new RenamePattern(original, moved);
	}
}
