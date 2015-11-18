package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.LinesToCharsResult;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

public class DiffUtils {

	public static List<Integer> computeHashes(String text) {
		try {
			IScanner scanner = ToolFactory.createScanner(true, true, false, "1.8");
			char[] charArray = text.toCharArray();
			scanner.setSource(charArray);
			scanner.resetTo(0, text.length() - 1);

			List<Integer> hashes = new ArrayList<Integer>();
			int token;
			while ((token = scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				int tokenStart = scanner.getCurrentTokenStartPosition();
				int tokenEnd = scanner.getCurrentTokenEndPosition();
				if (token != ITerminalSymbols.TokenNameWHITESPACE) {
					// String tokenString =
					// text.substring(scanner.getCurrentTokenStartPosition(),
					// scanner.getCurrentTokenEndPosition() + 1);
					int h = hash(charArray, tokenStart, tokenEnd);
					hashes.add(h);
				} else {
					if (indexOf(charArray, tokenStart, tokenEnd, '\n') != -1) {
						// new line
						
					}
				}
			}
			return hashes;
		} catch (InvalidInputException e) {
			throw new RuntimeException(e);
		}
	}

	public static double similarity(String s1, String s2) {
		if (s1 == null || s2 == null) {
			return 0.0;
		}
		if (s1 == s2) {
			return 1.0;
		}
		List<Integer> hashesS1 = computeHashes(s1);
		List<Integer> hashesS2 = computeHashes(s2);
		int l1 = hashesS1.size();
		int l2 = hashesS2.size();
		if (l1 == 1 && l2 == 1) {
			return hashesS1.get(0) == hashesS2.get(0) ? 1.0 : 0.0;
		}
		if (l1 < 2 || l2 < 2) {
			return 0.0;
		}
		long[] sPairs = computeBigrams(hashesS1);
		long[] tPairs = computeBigrams(hashesS2);
		return computeSimilarity(sPairs, tPairs);
	}

	public static double computeSimilarity(long[] sPairs, long[] tPairs) {
		return computeSimilarity(sPairs, tPairs, false);
	}

	public static double computeSimilarity(long[] sPairs, long[] tPairs,
			boolean partialSimilarity) {
		return computeSimilarity(sPairs, 1, tPairs, partialSimilarity);
	}

	public static double computeSimilarity(long[] sPairs, int sFactor,
			long[] tPairs, boolean partialSimilarity) {
		int n = sPairs.length * sFactor;
		int m = tPairs.length;

		int matches = 0, i = 0, j = 0;
		while (i < n && j < m) {
			if (sPairs[i / sFactor] == tPairs[j]) {
				matches += 1;
				i++;
				j++;
			} else if (sPairs[i / sFactor] < tPairs[j])
				i++;
			else
				j++;
		}
		if (partialSimilarity) {
			return (double) (matches) / n;
		}
		return (double) (2 * matches) / (n + m);
	}

	public static long[] computeBigrams(List<Integer> hashes) {
		int tokens = hashes.size();
		if (tokens < 2) {
			throw new IllegalArgumentException("one or less tokens");
		}
		long[] bigrams = new long[tokens - 1];
		for (int i = 1; i < tokens; i++) {
			int h1 = hashes.get(i - 1);
			int h2 = hashes.get(i);
			long bh = (long) h1 << 32 | h2 & 0xFFFFFFFFL;
			bigrams[i - 1] = bh;
		}
		Arrays.sort(bigrams);
		return bigrams;
	}

	public static int hash(char[] charArray, int tokenStart, int tokenEnd) {
		int h = 0;
		if (tokenEnd >= tokenStart) {
			for (int i = tokenStart; i <= tokenEnd; i++) {
				h = 31 * h + charArray[i];
			}
		}
		return h;
	}

	public static LinkedList<Diff> tokenBasedDiff(String s1, String s2) {
		diff_match_patch dmp = new diff_match_patch();
		List<String> tokenArray = new ArrayList<String>();
		tokenArray.add("");
		Map<String, Integer> tokenHash = new HashMap<String, Integer>();
		String sb1 = tokensToChars(s1, tokenArray, tokenHash);
		String sb2 = tokensToChars(s2, tokenArray, tokenHash);
		LinkedList<Diff> diffs = dmp.diff_main(sb1, sb2, true);
		charsToTokens(tokenArray, diffs);
		return diffs;
	}

	public static LinkedList<Diff> lineBasedDiff(String s1, String s2) {
		diff_match_patch dmp = new diff_match_patch();
		LinesToCharsResult r = dmp.diff_linesToChars(s1, s2);
		LinkedList<Diff> diffs = dmp.diff_main(r.chars1, r.chars2);
		dmp.diff_charsToLines(diffs, r.lineArray);
		return diffs;
	}

	public static LinkedList<String> getDeletedXorInsertedLines(
			LinkedList<Diff> diffs, boolean delete, double threshold) {
		Iterator<Diff> iter = diffs.iterator();
		int count = 0;
		int equalCount = 0;
		StringBuilder sb = new StringBuilder();
		LinkedList<String> lines = new LinkedList<String>();
		Operation ignore = delete ? Operation.INSERT : Operation.DELETE;
		while (iter.hasNext()) {
			Diff diff = iter.next();
			if (diff.operation != ignore) {
				int newLinePos = diff.text.indexOf('\n');
				if (newLinePos == -1) {
					if (diff.operation == Operation.EQUAL) {
						equalCount += diff.text.length();
					} else {
						count += diff.text.length();
					}
					sb.append(diff.text);
				} else {
					if (diff.operation == Operation.EQUAL) {
						equalCount += newLinePos;
					} else {
						count += newLinePos;
					}
					sb.append(diff.text, 0, newLinePos);

					if (sb.length() > 0) {
						if (((double) count) / (count + equalCount) >= threshold) {
							lines.add(sb.toString());
						}
					}
					sb.setLength(0);
					count = 0;
					equalCount = 0;

					if (diff.operation == Operation.EQUAL) {
						equalCount += diff.text.length() - (newLinePos + 1);
					} else {
						count += diff.text.length() - (newLinePos + 1);
					}
					sb.append(diff.text, newLinePos + 1, diff.text.length());
				}
			}
		}
		if (sb.length() > 0) {
			if (((double) count) / (count + equalCount) >= threshold) {
				lines.add(sb.toString());
			}
		}
		return lines;
	}

	public static String joinLines(Iterable<String> lines) {
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line);
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String tokensToChars(String text, List<String> tokenArray,
			Map<String, Integer> tokenHash) {
		try {
			StringBuilder chars = new StringBuilder();
			IScanner scanner = ToolFactory.createScanner(true, true, false,
					"1.8");
			char[] charArray = text.toString().toCharArray();
			scanner.setSource(charArray);

			int token;
			while ((token = scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				String tokenString;
				if (token == ITerminalSymbols.TokenNameWHITESPACE) {
					if (indexOf(charArray,
							scanner.getCurrentTokenStartPosition(),
							scanner.getCurrentTokenEndPosition(), '\n') != -1) {
						tokenString = "\n";
					} else {
						tokenString = null;
					}
				} else {
					tokenString = new String(scanner.getCurrentTokenSource());
				}
				if (tokenString != null) {
					if (tokenHash.containsKey(tokenString)) {
						chars.append(String.valueOf((char) (int) tokenHash
								.get(tokenString)));
					} else {
						tokenArray.add(tokenString);
						tokenHash.put(tokenString, tokenArray.size() - 1);
						chars.append(String.valueOf((char) (tokenArray.size() - 1)));
					}
				}
			}
			return chars.toString();
		} catch (InvalidInputException e) {
			throw new RuntimeException(e);
		}
	}

	private static void charsToTokens(List<String> tokenArray,
			LinkedList<Diff> diffs) {
		StringBuilder text;
		for (Diff diff : diffs) {
			text = new StringBuilder();
			for (int y = 0; y < diff.text.length(); y++) {
				text.append(tokenArray.get(diff.text.charAt(y)));
				text.append(' ');
			}
			diff.text = text.toString();
		}
	}

	private static int indexOf(char[] charArray, int start, int end, char c) {
		for (int i = start; i <= end; i++) {
			if (charArray[i] == c) {
				return i;
			}
		}
		return -1;
	}

	public static List<String> splitLines(String source) {
		LinkedList<String> lines = new LinkedList<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '\n' && sb.length() > 0) {
				lines.add(sb.toString());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 0) {
			lines.add(sb.toString());
		}
		return lines;
	}

	public static String toHtml(LinkedList<Diff> diffs) {
		diff_match_patch dmp = new diff_match_patch();
		return "<code>" + dmp.diff_prettyHtml(diffs) + "</code>";
	}
}
