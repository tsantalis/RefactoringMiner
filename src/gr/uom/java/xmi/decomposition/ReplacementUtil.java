package gr.uom.java.xmi.decomposition;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacementUtil {
	private static final String[] SPECIAL_CHARACTERS = {";", ",", ")", "=", "+", "-", ">", "<", ".", "]", " "};
	
	public static int countInstances(String completeString, String subString) {
		for(String character : SPECIAL_CHARACTERS) {
			int index = completeString.indexOf(subString + character);
			if(index != -1) {
				return (completeString.length() - completeString.replace(subString + character, "").length()) / (subString.length() + 1);
			}
		}
		return 0;
	}

	public static boolean contains(String completeString, String subString) {
		for(String character : SPECIAL_CHARACTERS) {
			if(completeString.contains(subString + character)) {
				return true;
			}
		}
		return false;
	}

	public static String performReplacement(String completeString, String subString, String replacement) {
		String temp = new String(completeString);
		if(completeString.equals(subString)) {
			temp = temp.replace(subString, replacement);
			return temp;
		}
		for(String character : SPECIAL_CHARACTERS) {
			if(completeString.contains(subString + character)) {
				temp = temp.replace(subString + character, replacement + character);
			}
		}
		return temp;
	}

	public static String performReplacement(String completeString1, String completeString2, String subString1, String subString2, Set<String> variables1, Set<String> variables2) {	
		String temp = new String(completeString1);
		boolean replacementOccurred = false;
		for(String character : SPECIAL_CHARACTERS) {
			if(variables1.contains(subString1) && variables2.contains(subString2) && completeString1.contains(subString1 + character) && completeString2.contains(subString2 + character)) {
				temp = temp.replace(subString1 + character, subString2 + character);
				replacementOccurred = true;
			}
		}
		if(!replacementOccurred) {
			for(String character : SPECIAL_CHARACTERS) {
				if(variables1.contains(subString1) && variables2.contains(subString2) && completeString1.contains(subString1 + character) && syntaxAwareReplacement(subString1, subString2, completeString1, completeString2)) {
					temp = temp.replace(subString1 + character, subString2 + character);
					replacementOccurred = true;
				}
			}
		}
		if(!replacementOccurred && completeString1.contains(subString1) && completeString2.contains(subString2)) {
			try {
				char nextCharacter1 = completeString1.charAt(completeString1.indexOf(subString1) + subString1.length());
				char nextCharacter2 = completeString2.charAt(completeString2.indexOf(subString2) + subString2.length());
				if(nextCharacter1 == nextCharacter2) {
					temp = completeString1.replaceAll(Pattern.quote(subString1), Matcher.quoteReplacement(subString2));
				}
			} catch(IndexOutOfBoundsException e) {
				return temp;
			}
		}
		return temp;
	}

	public static int indexOf(String completeString, String subString) {
		for(String character : SPECIAL_CHARACTERS) {
			int index = completeString.indexOf(subString + character);
			if(index != -1) {
				return index;
			}
		}
		return -1;
	}

	public static int lastIndexOf(String completeString, String subString) {
		for(String character : SPECIAL_CHARACTERS) {
			int index = completeString.lastIndexOf(subString + character);
			if(index != -1) {
				return index;
			}
		}
		return -1;
	}

	public static boolean syntaxAwareReplacement(String s1, String s2, String argumentizedString1, String argumentizedString2) {
		int smallStringLength = 4;
		int firstIndex1 = s1.length() < smallStringLength ? ReplacementUtil.indexOf(argumentizedString1, s1) : argumentizedString1.indexOf(s1);
		int lastIndex1 = s1.length() < smallStringLength ? ReplacementUtil.lastIndexOf(argumentizedString1, s1) : argumentizedString1.lastIndexOf(s1);
		int length1 = argumentizedString1.length();
		String firstCharacterBefore1 = null;
		String firstCharacterAfter1 = null;
		String lastCharacterBefore1 = null;
		String lastCharacterAfter1 = null;
		if(firstIndex1 != -1) {
			firstCharacterBefore1 = firstIndex1 == 0 ? "" : Character.toString(argumentizedString1.charAt(firstIndex1-1));
			firstCharacterAfter1 = firstIndex1 + s1.length() == length1 ? "" : Character.toString(argumentizedString1.charAt(firstIndex1 + s1.length()));
			if(lastIndex1 != firstIndex1) {
				lastCharacterBefore1 = lastIndex1 == 0 ? "" : Character.toString(argumentizedString1.charAt(lastIndex1-1));
				lastCharacterAfter1 = lastIndex1 + s1.length() == length1 ? "" : Character.toString(argumentizedString1.charAt(lastIndex1 + s1.length()));
			}
		}
		
		int firstIndex2 = s2.length() < smallStringLength ? ReplacementUtil.indexOf(argumentizedString2, s2) : argumentizedString2.indexOf(s2);
		int lastIndex2 = s2.length() < smallStringLength ? ReplacementUtil.lastIndexOf(argumentizedString2, s2) : argumentizedString2.lastIndexOf(s2);
		int length2 = argumentizedString2.length();
		String firstCharacterBefore2 = null;
		String firstCharacterAfter2 = null;
		String lastCharacterBefore2 = null;
		String lastCharacterAfter2 = null;
		if(firstIndex2 != -1) {
			firstCharacterBefore2 = firstIndex2 == 0 ? "" : Character.toString(argumentizedString2.charAt(firstIndex2-1));
			firstCharacterAfter2 = firstIndex2 + s2.length() == length2 ? "" : Character.toString(argumentizedString2.charAt(firstIndex2 + s2.length()));
			if(lastIndex2 != firstIndex2) {
				lastCharacterBefore2 = lastIndex2 == 0 ? "" : Character.toString(argumentizedString2.charAt(lastIndex2-1));
				lastCharacterAfter2 = lastIndex2 + s2.length() == length2 ? "" : Character.toString(argumentizedString2.charAt(lastIndex2 + s2.length()));
			}
		}
		return (compatibleCharacterBeforeMatch(firstCharacterBefore1, firstCharacterBefore2) && compatibleCharacterAfterMatch(firstCharacterAfter1, firstCharacterAfter2)) ||
				(compatibleCharacterBeforeMatch(firstCharacterBefore1, lastCharacterBefore2) && compatibleCharacterAfterMatch(firstCharacterAfter1, lastCharacterAfter2)) ||
				(compatibleCharacterBeforeMatch(lastCharacterBefore1, firstCharacterBefore2) && compatibleCharacterAfterMatch(lastCharacterAfter1, firstCharacterAfter2)) ||
				(compatibleCharacterBeforeMatch(lastCharacterBefore1, lastCharacterBefore2) && compatibleCharacterAfterMatch(lastCharacterAfter1, lastCharacterAfter2));
	}

	private static boolean compatibleCharacterBeforeMatch(String characterBefore1, String characterBefore2) {
		if(characterBefore1 != null && characterBefore2 != null) {
			if(characterBefore1.equals(characterBefore2))
				return true;
			if(characterBefore1.equals(",") && characterBefore2.equals("("))
				return true;
			if(characterBefore1.equals("(") && characterBefore2.equals(","))
				return true;
			if(characterBefore1.equals(" ") && characterBefore2.equals(""))
				return true;
			if(characterBefore1.equals("") && characterBefore2.equals(" "))
				return true;
		}
		return false;
	}

	private static boolean compatibleCharacterAfterMatch(String characterAfter1, String characterAfter2) {
		if(characterAfter1 != null && characterAfter2 != null) {
			if(characterAfter1.equals(characterAfter2))
				return true;
			if(characterAfter1.equals(",") && characterAfter2.equals(")"))
				return true;
			if(characterAfter1.equals(")") && characterAfter2.equals(","))
				return true;
		}
		return false;
	}
}
