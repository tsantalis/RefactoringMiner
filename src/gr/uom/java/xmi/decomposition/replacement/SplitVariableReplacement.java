package gr.uom.java.xmi.decomposition.replacement;

import java.util.Set;

public class SplitVariableReplacement extends Replacement {
	private Set<String> splitVariables;

	public SplitVariableReplacement(String oldVariable, Set<String> newVariables) {
		super(oldVariable, newVariables.toString(), ReplacementType.SPLIT_VARIABLE);
		this.splitVariables = newVariables;
	}

	public Set<String> getSplitVariables() {
		return splitVariables;
	}

}
