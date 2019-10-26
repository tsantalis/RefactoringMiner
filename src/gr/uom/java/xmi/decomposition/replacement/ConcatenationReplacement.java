package gr.uom.java.xmi.decomposition.replacement;

import java.util.Set;

public class ConcatenationReplacement extends Replacement {
	private Set<String> commonElements;
	public ConcatenationReplacement(String before, String after, Set<String> commonElements) {
		super(before, after, ReplacementType.CONCATENATION);
		this.commonElements = commonElements;
	}
	public Set<String> getCommonElements() {
		return commonElements;
	}
}
