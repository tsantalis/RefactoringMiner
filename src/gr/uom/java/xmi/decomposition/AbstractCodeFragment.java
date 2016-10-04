package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCodeFragment {
	private int depth;
	private int index;
	private String codeFragmentAfterReplacingParametersWithArguments;

	public String getArgumentizedString() {
		return codeFragmentAfterReplacingParametersWithArguments != null ? codeFragmentAfterReplacingParametersWithArguments : getString();
	}

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
	public abstract List<VariableDeclaration> getVariableDeclarations();
	public abstract Map<String, OperationInvocation> getMethodInvocationMap();
	
	public void replaceParametersWithArguments(Map<String, String> parameterToArgumentMap) {
		String afterReplacements = getString();
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(!parameter.equals(argument)) {
				StringBuffer sb = new StringBuffer();
				Pattern p = Pattern.compile(Pattern.quote(parameter));
				Matcher m = p.matcher(afterReplacements);
				while(m.find()) {
					//avoid replacing the parameter if it has "this." as a prefix
					int start = m.start();
					boolean thisPrefixFound = false;
					if(start >= 5) {
						String prefix = afterReplacements.substring(start-5, start);
						if(prefix.equals("this.")) {
							thisPrefixFound = true;
						}
					}
					if(!thisPrefixFound) {
						m.appendReplacement(sb, Matcher.quoteReplacement(argument));
					}
				}
				m.appendTail(sb);
				afterReplacements = sb.toString();
			}
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
