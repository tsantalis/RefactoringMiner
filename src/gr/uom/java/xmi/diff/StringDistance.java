package gr.uom.java.xmi.diff;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

public class StringDistance {
	
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
			
			DiffRowGenerator generator = DiffRowGenerator.create()
	                .showInlineDiffs(true)
	                .inlineDiffByWord(true)
	                .build();
			
			Patch<String> patch = DiffUtils.diff(original, revised);
			List<AbstractDelta<String>> deltas = patch.getDeltas();
			for(AbstractDelta<String> delta : deltas) {
				Chunk<String> source = delta.getSource();
				Chunk<String> target = delta.getTarget();
				if(source.getLines().size() == 1 && target.getLines().size() == 1) {
					List<DiffRow> rows = generator.generateDiffRows(
							Arrays.asList(source.getLines().get(0)),
							Arrays.asList(target.getLines().get(0)));
					if(rows.size() == 1) {
						String oldLine = rows.get(0).getOldLine();
						String newLine = rows.get(0).getNewLine();
						String oldEditStartTag = "<span class=\"editOldInline\">";
						String oldValue = oldLine.substring(oldLine.indexOf(oldEditStartTag) + oldEditStartTag.length(), oldLine.indexOf("</span>"));
						String newEditStartTag = "<span class=\"editNewInline\">";
						String newValue = newLine.substring(newLine.indexOf(newEditStartTag) + newEditStartTag.length(), newLine.indexOf("</span>"));
						if(!isNumeric(oldValue) || !isNumeric(newValue)) {
							return false;
						}
					}
				}
				else {
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
