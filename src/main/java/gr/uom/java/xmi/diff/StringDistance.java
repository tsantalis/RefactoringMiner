package gr.uom.java.xmi.diff;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

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
	
	public static int editDistance(String a, String b, int threshold) {
		int length1 = a.length();
		int length2 = b.length();
		if(length1 > MAX_STRING_LENGTH || length2 > MAX_STRING_LENGTH) {
			return threshold;
		}
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
		int distance = new LevenshteinDistance(threshold).apply(a.substring(prefix, length1 - suffix), b.substring(prefix, length2 - suffix));
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
		if(length1 > MAX_STRING_LENGTH || length2 > MAX_STRING_LENGTH) {
			return Math.max(length1, length2);
		}
		StringPair key = new StringPair(a, b);
		Integer cached = EDIT_DISTANCE_CACHE.get(key);
		if(cached != null) {
			return cached;
		}
		int prefix = commonPrefixLength(a, b);
		int suffix = commonSuffixLength(a, b, prefix);
		int distance = new LevenshteinDistance().apply(a.substring(prefix, length1 - suffix), b.substring(prefix, length2 - suffix));
		cache(key, distance);
		return distance;
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
