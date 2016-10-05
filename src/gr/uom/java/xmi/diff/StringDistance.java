package gr.uom.java.xmi.diff;

import org.apache.commons.lang3.StringUtils;

public class StringDistance {
	
	public static int editDistance(String a, String b, int threshold) {
		return StringUtils.getLevenshteinDistance(a, b, threshold);
	}

	public static int editDistance(String a, String b) {
		return StringUtils.getLevenshteinDistance(a, b);
	}
}
