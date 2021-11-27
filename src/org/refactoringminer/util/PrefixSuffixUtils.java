package org.refactoringminer.util;

public class PrefixSuffixUtils {

	public static String longestCommonPrefix(String s1, String s2) {
		int minLength = Math.min(s1.length(), s2.length());
		int i = 0;
		while (i < minLength && s1.charAt(i) == s2.charAt(i)) {
			i++;
		}
		return s1.substring(0, i);
	}
	
	public static String longestCommonSuffix(String s1, String s2) {
		int minLength = Math.min(s1.length(), s2.length());
		int i = 0;
		while (i<minLength && s1.charAt(s1.length() - i - 1) == s2.charAt(s2.length() - i - 1)) {
			i++;
		}
		return s1.substring(s1.length() - i, s1.length());
	}

	public static int separatorPosOfCommonSuffix(char separator, String s1, String s2) {
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

	public static String normalize(String input) {
		String output = null;
		if(input.startsWith("this.")) {
			output = input.substring(5, input.length());
		}
		else {
			output = input;
		}
		return output;
	}
}
