package gr.uom.java.xmi.diff;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class StringDistance {
	
	public static int editDistance(String a, String b, int threshold) {
		return new LevenshteinDistance(threshold).apply(a, b);
	}

	public static int editDistance(String a, String b) {
		return new LevenshteinDistance().apply(a, b);
	}
}
