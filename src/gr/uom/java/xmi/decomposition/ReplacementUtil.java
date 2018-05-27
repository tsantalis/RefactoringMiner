package gr.uom.java.xmi.decomposition;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacementUtil {
	private static final String[] SPECIAL_CHARACTERS = {";", ",", ")", "=", "+", "-", ">", "<", ".", "]"};
	
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


}
