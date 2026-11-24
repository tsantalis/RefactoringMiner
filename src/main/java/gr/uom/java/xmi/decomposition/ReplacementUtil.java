package gr.uom.java.xmi.decomposition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.containsMethodSignatureOfAnonymousClass;

public class ReplacementUtil {
	private static final String[] SPECIAL_CHARACTERS = {";", ",", ")", "=", "+", "-", ">", "<", ".", "]", " ", "(", "[", "}", "{", "`"};
	private static final String SPECIAL_CHARACTER_SET = String.join("", SPECIAL_CHARACTERS);
	private static final String[] SPECIAL_ARGUMENT_CHARACTERS = {";", ",", ")", "=", "+", "-", ">", "<", ".", "]", " "};
	private static final Pattern DOUBLE_QUOTES = Pattern.compile("\"([^\"]*)\"|(\\S+)");
	//the same replacements are performed repeatedly, e.g., when the same pair of statements is compared by different mappers
	private static final int MAX_CACHE_SIZE = 100000;
	private static final Map<ReplacementKey, String> REPLACEMENT_CACHE = new ConcurrentHashMap<>();
	//the language ordinals are used, because Enum.hashCode() is an identity hash code and computing it shifts the identity hash codes of other objects
	private record ReplacementKey(String completeString1, String completeString2, String subString1, String subString2, int language1, int language2) {}

	public static boolean isInsideSingleQuotes(String argument, int indexOfChar) {
		if(indexOfChar > 0 && indexOfChar < argument.length()-1) {
			return argument.charAt(indexOfChar-1) == '\'' &&
					argument.charAt(indexOfChar+1) == '\'';
		}
		return false;
	}

	public static boolean isInsideDoubleQuotes(String argument, int indexOfChar) {
		Matcher m = DOUBLE_QUOTES.matcher(argument);
		while (m.find()) {
			if (m.group(1) != null) {
				if(indexOfChar > m.start() && indexOfChar < m.end()) {
					return true;
				}
			}
		}
		return false;
	}

	public static String keepReservedTokens(String input) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<input.length(); i++) {
			char c = input.charAt(i);
			if(SPECIAL_CHARACTER_SET.indexOf(c) != -1) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static int countInstances(String completeString, String subString) {
		int count = 0;
		for(String character : SPECIAL_CHARACTERS) {
			int index = completeString.indexOf(subString + character);
			if(index != -1) {
				count += (completeString.length() - completeString.replace(subString + character, "").length()) / (subString.length() + 1);
			}
		}
		return count;
	}

	public static boolean contains(String completeString, String subString) {
		for(String character : SPECIAL_CHARACTERS) {
			if(completeString.contains(subString + character)) {
				return true;
			}
		}
		return false;
	}

	public static String performArgumentReplacement(String completeString, String subString, String replacement) {
		String temp = new String(completeString);
		for(String character : SPECIAL_ARGUMENT_CHARACTERS) {
			if(completeString.contains(subString + character)) {
				temp = temp.replace(subString + character, replacement + character);
			}
		}
		return temp;
	}

	public static String performReplacement(String completeString, String subString, String replacement) {
		String temp = new String(completeString);
		if(completeString.equals(subString)) {
			temp = temp.replace(subString, replacement);
			return temp;
		}
		boolean replacementDone = false;
		for(String character : SPECIAL_CHARACTERS) {
			if(completeString.contains(subString + character)) {
				temp = temp.replace(subString + character, replacement + character);
				replacementDone = true;
			}
		}
		if(!replacementDone) {
			for(String character : SPECIAL_CHARACTERS) {
				if(completeString.contains(character + subString)) {
					temp = temp.replace(character + subString, character + replacement);
				}
			}
		}
		return temp;
	}

	//the replacements applied in their order like performReplacement, except that a replacement cannot match inside or partly over the text inserted by an earlier one,
	//i.e., 0 -> 180.0 followed by 0 -> Math.PI does not turn 180.0 into 18Math.PI.0, which also compounds into exponential growth when the new text of a replacement
	//contains the old text of a later one, i.e., t -> function(e){...t...} in minified code, while a replacement whose old text contains the whole text inserted by an earlier one
	//is applied, i.e., - -> >= followed by f(a >= 1) -> g(), since the replacements are found on the text with the earlier replacements applied
	public static String performReplacements(String completeString, Collection<Replacement> replacements) {
		GuardedText text = new GuardedText(completeString);
		for(Replacement replacement : replacements) {
			String subString = replacement.getBefore();
			String newText = replacement.getAfter();
			String current = text.toString();
			if(current.equals(subString)) {
				text = new GuardedText(newText, text.nextRegion++);
				continue;
			}
			boolean replacementDone = false;
			for(String character : SPECIAL_CHARACTERS) {
				if(current.contains(subString + character)) {
					text.replace(subString + character, 0, subString.length(), newText);
					replacementDone = true;
				}
			}
			if(!replacementDone) {
				for(String character : SPECIAL_CHARACTERS) {
					if(current.contains(character + subString)) {
						text.replace(character + subString, character.length(), subString.length(), newText);
					}
				}
			}
		}
		return text.toString();
	}

	//a text whose characters remember the replacement that inserted them, as region numbers, or -1 for the characters of the original text
	private static class GuardedText {
		private String text;
		private int[] regions;
		private int nextRegion;

		private GuardedText(String text) {
			this.text = text;
			this.regions = new int[text.length()];
			Arrays.fill(this.regions, -1);
		}

		private GuardedText(String text, int region) {
			this.text = text;
			this.regions = new int[text.length()];
			Arrays.fill(this.regions, region);
			this.nextRegion = region + 1;
		}

		//replaces the occurrences of the pattern from left to right like String.replace, where the old text is the part of the pattern at the offset,
		//skipping an occurrence whose old text starts or ends inside an inserted region
		private void replace(String pattern, int offset, int length, String newText) {
			int index = text.indexOf(pattern);
			if(index == -1) {
				return;
			}
			StringBuilder builder = new StringBuilder(text.length());
			int[] newRegions = new int[text.length() + 16];
			int size = 0;
			int i = 0;
			while(i < text.length()) {
				if(index == i && outsideInsertedRegions(i + offset, i + offset + length)) {
					int region = nextRegion++;
					newRegions = ensureCapacity(newRegions, size + pattern.length() - length + newText.length());
					for(int k = i; k < i + offset; k++) {
						builder.append(text.charAt(k));
						newRegions[size++] = regions[k];
					}
					builder.append(newText);
					for(int k = 0; k < newText.length(); k++) {
						newRegions[size++] = region;
					}
					for(int k = i + offset + length; k < i + pattern.length(); k++) {
						builder.append(text.charAt(k));
						newRegions[size++] = regions[k];
					}
					i += pattern.length();
					index = text.indexOf(pattern, i);
				}
				else {
					int end = index == i ? i + 1 : (index == -1 ? text.length() : index);
					newRegions = ensureCapacity(newRegions, size + end - i);
					for(int k = i; k < end; k++) {
						builder.append(text.charAt(k));
						newRegions[size++] = regions[k];
					}
					i = end;
					if(index != -1 && index < i) {
						index = text.indexOf(pattern, i);
					}
				}
			}
			text = builder.toString();
			regions = Arrays.copyOf(newRegions, size);
		}

		//whether the range does not start or end inside an inserted region, so that it contains each inserted region it overlaps entirely
		private boolean outsideInsertedRegions(int start, int end) {
			if(start == end) {
				return true;
			}
			if(start > 0 && regions[start] != -1 && regions[start - 1] == regions[start]) {
				return false;
			}
			if(end < regions.length && regions[end - 1] != -1 && regions[end] == regions[end - 1]) {
				return false;
			}
			return true;
		}

		private static int[] ensureCapacity(int[] array, int capacity) {
			return capacity <= array.length ? array : Arrays.copyOf(array, Math.max(capacity, array.length * 2));
		}

		public String toString() {
			return text;
		}
	}

	//equivalent to s.contains(subString + suffix) without creating the concatenated string
	private static boolean containsFollowedBy(String s, String subString, String suffix) {
		int index = s.indexOf(subString);
		while(index != -1) {
			if(s.startsWith(suffix, index + subString.length())) {
				return true;
			}
			if(index == s.length()) {
				return false;
			}
			index = s.indexOf(subString, index + 1);
		}
		return false;
	}

	//equivalent to s.contains(prefix + subString) without creating the concatenated string
	private static boolean containsPrecededBy(String s, String prefix, String subString) {
		int index = s.indexOf(subString, prefix.length());
		while(index != -1) {
			if(s.startsWith(prefix, index - prefix.length())) {
				return true;
			}
			if(index == s.length()) {
				return false;
			}
			index = s.indexOf(subString, index + 1);
		}
		return false;
	}

	public static String performReplacement(String completeString1, String completeString2, String subString1, String subString2, Constants LANG1, Constants LANG2) {
		ReplacementKey key = new ReplacementKey(completeString1, completeString2, subString1, subString2, LANG1.ordinal(), LANG2.ordinal());
		String cached = REPLACEMENT_CACHE.get(key);
		if(cached != null) {
			return cached;
		}
		String result = computeReplacement(completeString1, completeString2, subString1, subString2, LANG1, LANG2);
		if(REPLACEMENT_CACHE.size() >= MAX_CACHE_SIZE) {
			REPLACEMENT_CACHE.clear();
		}
		REPLACEMENT_CACHE.put(key, result);
		return result;
	}

	private static String computeReplacement(String completeString1, String completeString2, String subString1, String subString2, Constants LANG1, Constants LANG2) {
		String temp = new String(completeString1);
		boolean replacementOccurred = false;
		for(String character : SPECIAL_CHARACTERS) {
			String character2 = character;
			//cross-language assignment with different formatting, i.e., Java x=y; and Kotlin x = y
			if(character.equals("=") && !LANG1.equals(LANG2) &&
					temp.startsWith(subString1 + character) && completeString2.startsWith(subString2 + LANG2.ASSIGNMENT)) {
				character2 = LANG2.ASSIGNMENT;
			}
			//cross-language compound assignment with different formatting, i.e., Java x-=y; and Kotlin x -= y
			else if((character.equals("+") || character.equals("-")) && !LANG1.equals(LANG2) &&
					temp.startsWith(subString1 + character + "=") && completeString2.startsWith(subString2 + " " + character + "=")) {
				character2 = " ";
			}
			//cross-language statement termination, i.e., Java return x; and Kotlin return x
			//the Kotlin trailing lambda is not the last expression of a Java statement, i.e., return shutdown; -> pushExecutorExecute("...") {...}
			else if(character.equals(";") && !LANG1.equals(LANG2) && !subString2.startsWith("{") &&
					temp.endsWith(subString1 + LANG1.STATEMENT_TERMINATION) && completeString2.endsWith(subString2 + LANG2.STATEMENT_TERMINATION)) {
				character2 = LANG2.STATEMENT_TERMINATION;
			}
			if(containsFollowedBy(temp, subString1, character) && containsFollowedBy(completeString2, subString2, character2)) {
				StringBuffer sb = new StringBuffer();
				Pattern p1 = Pattern.compile(Pattern.quote(subString1 + character));
				Matcher m1 = p1.matcher(temp);
				Pattern p2 = Pattern.compile(Pattern.quote(subString2 + character2));
				Matcher m2 = p2.matcher(completeString2);
				while(m1.find() && m2.find()) {
					int start1 = m1.start();
					int start2 = m2.start();
					String characterBeforeMatch1 = start1 == 0 ? "" : String.valueOf(temp.charAt(start1 - 1));
					String characterBeforeMatch2 = start2 == 0 ? "" : String.valueOf(completeString2.charAt(start2 - 1));
					//cross-language arguments with different formatting, i.e., Java min(x,y) and Kotlin min(x, y)
					//the Kotlin trailing lambda is not an argument replacing a Java argument, i.e., pushExecutor.execute(new NamedRunnable("...",streamId) {...}) -> pushExecutorExecute("...") {...}
					if(!LANG1.equals(LANG2) && !subString2.startsWith("{") && characterBeforeMatch1.equals(",") && characterBeforeMatch2.equals(" ") && start2 > 1 && completeString2.charAt(start2 - 2) == ',') {
						characterBeforeMatch2 = ",";
					}
					boolean compatible = compatibleCharacterBeforeMatch(characterBeforeMatch1, characterBeforeMatch2, LANG1, LANG2);
					//the qualified part of an operand negated only in one of the inverted conditions is compared by the character before the negation, i.e., if(!executable.ok()) -> if(candidate.executable.ok()),
					//but not the entire negated operand, i.e., while(!stack.isEmpty()) -> if(topLevel), or a replaced operand, i.e., if(PARAMS.containsKey(x)) -> if(!request.getHeaders().containsKey(y)),
					//which would differ only in the negation
					boolean qualified = subString2.endsWith("." + subString1) || subString1.endsWith("." + subString2);
					if(!compatible && qualified && character.equals(".") && characterBeforeMatch1.equals(LANG1.NOT) && !characterBeforeMatch2.equals(LANG2.NOT)) {
						compatible = compatibleCharacterBeforeMatch(start1 == 1 ? "" : String.valueOf(temp.charAt(start1 - 2)), characterBeforeMatch2, LANG1, LANG2);
					}
					else if(!compatible && qualified && character.equals(".") && characterBeforeMatch2.equals(LANG2.NOT) && !characterBeforeMatch1.equals(LANG1.NOT)) {
						compatible = compatibleCharacterBeforeMatch(characterBeforeMatch1, start2 == 1 ? "" : String.valueOf(completeString2.charAt(start2 - 2)), LANG1, LANG2);
					}
					if(!compatible && isStringLiteral(subString1) && isStringLiteral(subString2)) {
						compatible = elementShiftedInList(temp, start1, completeString2, start2);
					}
					if(compatible) {
						m1.appendReplacement(sb, Matcher.quoteReplacement(subString2 + character));
						replacementOccurred = true;
					}
				}
				m1.appendTail(sb);
				temp = sb.toString();
			}
		}
		boolean resume = !replacementOccurred || (temp.contains(": " + subString1) && completeString2.contains(": " + subString2));
		if(resume && !containsMethodSignatureOfAnonymousClass(completeString1, LANG1) && !containsMethodSignatureOfAnonymousClass(completeString2, LANG2)) {
			for(String character : SPECIAL_CHARACTERS) {
				if(containsPrecededBy(temp, character, subString1) && containsPrecededBy(completeString2, character, subString2)) {
					StringBuffer sb = new StringBuffer();
					Pattern p1 = Pattern.compile(Pattern.quote(character + subString1));
					Matcher m1 = p1.matcher(temp);
					Pattern p2 = Pattern.compile(Pattern.quote(character + subString2));
					Matcher m2 = p2.matcher(completeString2);
					while(m1.find() && m2.find()) {
						int end1 = m1.end();
						int end2 = m2.end();
						String characterAfterMatch1 = end1 == temp.length() ? "" : String.valueOf(temp.charAt(end1));
						String characterAfterMatch2 = end2 == completeString2.length() ? "" : String.valueOf(completeString2.charAt(end2));
						if(compatibleCharacterAfterMatch(characterAfterMatch1, characterAfterMatch2) ||
								stringConcatWithCompatibleCharacterAfterMatch(subString1, subString2, characterAfterMatch1, characterAfterMatch2)) {
							m1.appendReplacement(sb, Matcher.quoteReplacement(character + subString2));
							replacementOccurred = true;
						}
					}
					m1.appendTail(sb);
					temp = sb.toString();
				}
			}
		}
		return temp;
	}

	private static boolean isStringLiteral(String s) {
		return s.length() > 1 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0);
	}

	//a string literal following a ", " separator moved to the start of a list, or vice versa, in statements printed with spaces after the commas,
	//i.e., ['prefix', 'prefix *', 'prefix Yukikaze'] -> ['*', 'Yukikaze'], like Java f(a,x) -> f(x)
	private static boolean elementShiftedInList(String completeString1, int start1, String completeString2, int start2) {
		boolean afterSeparator1 = start1 > 1 && completeString1.charAt(start1 - 1) == ' ' && completeString1.charAt(start1 - 2) == ',';
		boolean afterSeparator2 = start2 > 1 && completeString2.charAt(start2 - 1) == ' ' && completeString2.charAt(start2 - 2) == ',';
		if(afterSeparator1 == afterSeparator2)
			return false;
		char characterBefore = afterSeparator1 ? (start2 == 0 ? ' ' : completeString2.charAt(start2 - 1)) : (start1 == 0 ? ' ' : completeString1.charAt(start1 - 1));
		return characterBefore == '(' || characterBefore == '[';
	}

	private static boolean compatibleCharacterBeforeMatch(String characterBefore1, String characterBefore2, Constants LANG1, Constants LANG2) {
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
			if(characterBefore1.equals("") && characterBefore2.equals(LANG2.NOT))
				return true;
			if(characterBefore1.equals("=") && LANG1.equals(Constants.JAVA) && characterBefore2.equals(" ") && LANG2.equals(Constants.KOTLIN))
				return true;
		}
		return false;
	}

	private static boolean stringConcatWithCompatibleCharacterAfterMatch(String subString1, String subString2, String characterAfter1, String characterAfter2) {
		if(characterAfter1 != null && characterAfter2 != null) {
			if(subString1.startsWith("\"") && subString2.startsWith("\"") && subString1.endsWith("\"") && subString2.endsWith("\"")) {
				if(characterAfter1.equals(" ") && characterAfter2.equals(")"))
					return true;
				if(characterAfter1.equals(")") && characterAfter2.equals(" "))
					return true;
			}
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
			if(characterAfter1.equals("-") && characterAfter2.equals("."))
				return true;
			if(characterAfter1.equals(".") && characterAfter2.equals("-"))
				return true;
		}
		return false;
	}
	
	public static boolean sameCharsBeforeAfter(String completeString1, String completeString2, String commonSubString) {
		Pattern p = Pattern.compile(Pattern.quote(commonSubString));
		Matcher m1 = p.matcher(completeString1);
		Matcher m2 = p.matcher(completeString2);
		int matches = 0;
		int compatibleMatches = 0;
		while(m1.find() && m2.find()) {
			int start1 = m1.start();
			int start2 = m2.start();
			String characterBeforeMatch1 = start1 == 0 ? "" : String.valueOf(completeString1.charAt(start1 - 1));
			String characterBeforeMatch2 = start2 == 0 ? "" : String.valueOf(completeString2.charAt(start2 - 1));
			int end1 = m1.end();
			int end2 = m2.end();
			String characterAfterMatch1 = end1 == completeString1.length() ? "" : String.valueOf(completeString1.charAt(end1));
			String characterAfterMatch2 = end2 == completeString2.length() ? "" : String.valueOf(completeString2.charAt(end2));
			if(characterBeforeMatch1.equals(characterBeforeMatch2) && characterAfterMatch1.equals(characterAfterMatch2)) {
				compatibleMatches++;
			}
			matches++;
		}
		return matches == compatibleMatches;
	}

	public static boolean isDefaultValue(String argument, Constants LANG) {
		return argument.equals(LANG.NULL) || argument.equals("0") || argument.equals("1") || argument.equals(LANG.FALSE) || argument.equals(LANG.TRUE);
	}

	public static String getSHA256Hash(String input) {
		try {
			// 1. Initialize the MessageDigest for SHA-256
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			// 2. Convert the input string to a byte array
			byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			// 3. Convert the byte array to a hexadecimal string
			StringBuilder hexString = new StringBuilder();
			for (byte b : encodedHash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not found", e);
		}
	}
}
