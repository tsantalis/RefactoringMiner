package gr.uom.java.xmi.diff;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;

public class StringDistance {
	private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*(//|\\*).*");
	
	public static int editDistance(String a, String b, int threshold) {
		return new LevenshteinDistance(threshold).apply(a, b);
	}

	public static int editDistance(String a, String b) {
		return new LevenshteinDistance().apply(a, b);
	}

	public static boolean trivialCommentChange(String fileBefore, String fileAfter) throws IOException {
		if(fileBefore.length() == fileAfter.length()) {
			List<String> original = IOUtils.readLines(new StringReader(fileBefore));
			List<String> revised = IOUtils.readLines(new StringReader(fileAfter));

			Patch<String> patch = DiffUtils.diff(original, revised);
			List<AbstractDelta<String>> deltas = patch.getDeltas();
			for(AbstractDelta<String> delta : deltas) {
				Chunk<String> source = delta.getSource();
				if(source.getLines().size() > 0 && !COMMENT_LINE.matcher(source.getLines().get(0)).matches()) {
					return false;
				}
			}
			return true;
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
