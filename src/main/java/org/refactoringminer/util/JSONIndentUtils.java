package org.refactoringminer.util;

public class JSONIndentUtils {

	public static final String SINGLE_TAB = "\t";
	public static final String DOUBLE_TAB = SINGLE_TAB + SINGLE_TAB;
	public static final String NEW_LINE = "\n";

	private JSONIndentUtils() {
	}

	/**
	 * Prefixes every line of a self-contained, pretty-printed JSON fragment with the given indent,
	 * so it can be embedded as an element of an array/object one or more nesting levels deeper
	 * without losing its own internal indentation.
	 */
	public static String indentLines(String text, String indent) {
		String[] lines = text.split(NEW_LINE, -1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			sb.append(indent).append(lines[i]);
			if (i < lines.length - 1) {
				sb.append(NEW_LINE);
			}
		}
		return sb.toString();
	}
}
