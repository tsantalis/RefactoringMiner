package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCodeFragment {
	private int depth;
	private int index;
	private String codeFragmentAfterReplacingParametersWithArguments;

    public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public abstract String getString();
	public abstract List<String> getVariables();
	public abstract Map<String, OperationInvocation> getMethodInvocationMap();
	
	public void replaceParametersWithArguments(Map<String, String> parameterToArgumentMap) {
		String afterReplacements = getString();
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			afterReplacements = afterReplacements.replaceAll(Pattern.quote(parameter), Matcher.quoteReplacement(argument));
		}
		this.codeFragmentAfterReplacingParametersWithArguments = afterReplacements;
	}

	public boolean equalFragment(AbstractCodeFragment other) {
		if(this.getString().equals(other.getString())) {
			return true;
		}
		else if(this.codeFragmentAfterReplacingParametersWithArguments != null) {
			return this.codeFragmentAfterReplacingParametersWithArguments.equals(other.getString());
		}
		else if(other.codeFragmentAfterReplacingParametersWithArguments != null) {
			return other.codeFragmentAfterReplacingParametersWithArguments.equals(this.getString());
		}
		return false;
	}
}
