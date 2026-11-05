package gr.uom.java.xmi.diff;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;

public class StringDistance {
	private static final int MAX_STRING_LENGTH = 2000;
	private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*(//|\\*|import\\s).*");
	//most edit distance computations are repeated for the same pair of strings
	private static final int MAX_CACHE_SIZE = 100000;
	private static final Map<StringPair, Integer> EDIT_DISTANCE_CACHE = new ConcurrentHashMap<>();
	//the lower bounds of the edit distances that exceeded the threshold of a computation, i.e., threshold + 1
	private static final Map<StringPair, Integer> EDIT_DISTANCE_LOWER_BOUNDS = new ConcurrentHashMap<>();
	private record StringPair(String a, String b) {}
	
	//the strings longer than MAX_STRING_LENGTH are compared by the part they differ in, so that a statement with a long string literal,
	//i.e., static const char descriptor[] = {"..."}; in a protobuf source, is at distance 0 from an identical one, not at the length of the longer
	public static int editDistance(String a, String b, int threshold) {
		int length1 = a.length();
		int length2 = b.length();
		StringPair key = new StringPair(a, b);
		Integer cached = EDIT_DISTANCE_CACHE.get(key);
		if(cached != null && threshold >= 0) {
			return cached <= threshold ? cached : -1;
		}
		Integer lowerBound = EDIT_DISTANCE_LOWER_BOUNDS.get(key);
		if(lowerBound != null && threshold >= 0 && threshold < lowerBound) {
			return -1;
		}
		int prefix = commonPrefixLength(a, b);
		int suffix = commonSuffixLength(a, b, prefix);
		if(length1 - suffix - prefix > MAX_STRING_LENGTH || length2 - suffix - prefix > MAX_STRING_LENGTH) {
			return threshold;
		}
		int distance = levenshtein(a.substring(prefix, length1 - suffix), b.substring(prefix, length2 - suffix), threshold);
		if(distance != -1) {
			cache(key, distance);
		}
		else if(threshold >= 0) {
			if(EDIT_DISTANCE_LOWER_BOUNDS.size() >= MAX_CACHE_SIZE) {
				EDIT_DISTANCE_LOWER_BOUNDS.clear();
			}
			EDIT_DISTANCE_LOWER_BOUNDS.merge(key, threshold + 1, Math::max);
		}
		return distance;
	}

	public static int editDistance(String a, String b) {
		int length1 = a.length();
		int length2 = b.length();
		StringPair key = new StringPair(a, b);
		Integer cached = EDIT_DISTANCE_CACHE.get(key);
		if(cached != null) {
			return cached;
		}
		int prefix = commonPrefixLength(a, b);
		int suffix = commonSuffixLength(a, b, prefix);
		if(length1 - suffix - prefix > MAX_STRING_LENGTH || length2 - suffix - prefix > MAX_STRING_LENGTH) {
			return Math.max(length1 - suffix - prefix, length2 - suffix - prefix);
		}
		int distance = levenshtein(a.substring(prefix, length1 - suffix), b.substring(prefix, length2 - suffix), -1);
		cache(key, distance);
		return distance;
	}

	//the Levenshtein distance with the bit-parallel algorithm of Myers (1999) in the form of Hyyrö (2003) for the edit distance of whole strings:
	//the vertical and horizontal differences of the dynamic programming table (-1, 0, +1) are bit vectors, so that 64 rows are computed per step;
	//the shorter string is split in blocks of 64 characters, and the horizontal difference at the bottom of a block is the input of the next block;
	//returns -1 when the distance exceeds threshold (a negative threshold has no limit), like the LevenshteinDistance of Commons Text
	static int levenshtein(String a, String b, int threshold) {
		String pattern = a.length() <= b.length() ? a : b;
		String text = pattern == a ? b : a;
		int m = pattern.length();
		int n = text.length();
		if(threshold >= 0 && n - m > threshold) {
			return -1;
		}
		if(m == 0) {
			return n;
		}
		int blocks = (m + 63) >>> 6;
		//the positions of each character in the pattern, ASCII characters in an array, the others in a map
		long[] ascii = new long[128 * blocks];
		Map<Character, long[]> nonAscii = null;
		for(int i = 0; i < m; i++) {
			char c = pattern.charAt(i);
			long bit = 1L << (i & 63);
			if(c < 128) {
				ascii[c * blocks + (i >>> 6)] |= bit;
			}
			else {
				if(nonAscii == null) {
					nonAscii = new HashMap<>();
				}
				nonAscii.computeIfAbsent(c, k -> new long[blocks])[i >>> 6] |= bit;
			}
		}
		//the first column is 0, 1, ..., m, i.e., all vertical differences are +1
		long[] positiveVertical = new long[blocks];
		long[] negativeVertical = new long[blocks];
		Arrays.fill(positiveVertical, -1L);
		long lastRow = 1L << ((m - 1) & 63);
		int score = m;
		for(int j = 0; j < n; j++) {
			char c = text.charAt(j);
			long[] equal = null;
			int offset = 0;
			if(c < 128) {
				equal = ascii;
				offset = c * blocks;
			}
			else if(nonAscii != null) {
				equal = nonAscii.get(c);
			}
			//the first row is 0, 1, ..., n, i.e., the horizontal difference entering the first block is +1
			int horizontal = 1;
			for(int k = 0; k < blocks; k++) {
				long eq = equal == null ? 0L : equal[offset + k];
				long pv = positiveVertical[k];
				long mv = negativeVertical[k];
				long negativeInput = horizontal < 0 ? 1L : 0L;
				long xv = eq | mv;
				eq |= negativeInput;
				long xh = (((eq & pv) + pv) ^ pv) | eq;
				long ph = mv | ~(xh | pv);
				long mh = pv & xh;
				long bottom = k == blocks - 1 ? lastRow : Long.MIN_VALUE;
				int output = (ph & bottom) != 0 ? 1 : (mh & bottom) != 0 ? -1 : 0;
				ph = (ph << 1) | (horizontal > 0 ? 1L : 0L);
				mh = (mh << 1) | negativeInput;
				positiveVertical[k] = mh | ~(xv | ph);
				negativeVertical[k] = ph & xv;
				horizontal = output;
			}
			score += horizontal;
			//each remaining column lowers the last row by at most 1
			if(threshold >= 0 && score - (n - 1 - j) > threshold) {
				return -1;
			}
		}
		return threshold >= 0 && score > threshold ? -1 : score;
	}

	private static void cache(StringPair key, int distance) {
		if(EDIT_DISTANCE_CACHE.size() >= MAX_CACHE_SIZE) {
			EDIT_DISTANCE_CACHE.clear();
		}
		EDIT_DISTANCE_CACHE.put(key, distance);
	}

	//the edit distance is not affected by removing the common prefix and suffix of the compared strings
	private static int commonPrefixLength(String a, String b) {
		int max = Math.min(a.length(), b.length());
		int i = 0;
		while(i < max && a.charAt(i) == b.charAt(i)) {
			i++;
		}
		return i;
	}

	private static int commonSuffixLength(String a, String b, int prefix) {
		int max = Math.min(a.length(), b.length()) - prefix;
		int i = 0;
		while(i < max && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
			i++;
		}
		return i;
	}

	public static boolean trivialCommentChange(String fileBefore, String fileAfter) throws IOException {
		if(fileBefore.length() == fileAfter.length()) {
			List<String> original = IOUtils.readLines(new StringReader(fileBefore));
			List<String> revised = IOUtils.readLines(new StringReader(fileAfter));

			Patch<String> patch = DiffUtils.diff(original, revised);
			List<AbstractDelta<String>> deltas = patch.getDeltas();
			for(AbstractDelta<String> delta : deltas) {
				Chunk<String> source = delta.getSource();
				if(source.getLines().size() > 0 && !source.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(source.getLines().get(0)).matches()) {
					return false;
				}
				Chunk<String> target = delta.getTarget();
				if(target.getLines().size() > 0 && !target.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(target.getLines().get(0)).matches()) {
					return false;
				}
			}
			return true;
		}
		else {
			List<String> original = IOUtils.readLines(new StringReader(fileBefore));
			List<String> revised = IOUtils.readLines(new StringReader(fileAfter));

			if(original.size() == revised.size()) {
				Patch<String> patch = DiffUtils.diff(original, revised);
				List<AbstractDelta<String>> deltas = patch.getDeltas();
				for(AbstractDelta<String> delta : deltas) {
					Chunk<String> source = delta.getSource();
					if(source.getLines().size() > 0 && !source.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(source.getLines().get(0)).matches()) {
						return false;
					}
					Chunk<String> target = delta.getTarget();
					if(target.getLines().size() > 0 && !target.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(target.getLines().get(0)).matches()) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isNumeric(String str) {
		for(char c : str.toCharArray()) {
			if(!Character.isDigit(c)) return false;
		}
		return true;
	}
}
