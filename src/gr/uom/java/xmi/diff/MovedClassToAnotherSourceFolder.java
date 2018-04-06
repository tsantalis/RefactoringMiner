package gr.uom.java.xmi.diff;

public class MovedClassToAnotherSourceFolder {
	private String className;
	private String originalPath;
	private String movedPath;

	public MovedClassToAnotherSourceFolder(String className, String originalPath, String movedPath) {
		this.className = className;
		this.originalPath = originalPath;
		this.movedPath = movedPath;
	}
	
	public RenamePattern getRenamePattern() {
		int separatorPos = separatorPosOfCommonSuffix('/', originalPath, movedPath);
		if (separatorPos == -1) {
			return new RenamePattern(originalPath, movedPath);
		}
		String original = originalPath.substring(0, originalPath.length() - separatorPos);
		String moved = movedPath.substring(0, movedPath.length() - separatorPos);
		return new RenamePattern(original, moved);
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
