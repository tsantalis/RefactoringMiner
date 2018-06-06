package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public abstract class AbstractCall implements LocationInfoProvider {
	protected int typeArguments;
	protected String expression;
	protected List<String> arguments;
	protected LocationInfo locationInfo;

	public String getExpression() {
		return expression;
	}

    public List<String> getArguments() {
		return arguments;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public boolean identicalExpressions(AbstractCall call) {
		return (getExpression() != null && call.getExpression() != null &&
				getExpression().equals(call.getExpression())) ||
				(getExpression() == null && call.getExpression() == null);
	}

	public boolean identicalExpressionsAfterTypeReplacements(AbstractCall call, Set<Replacement> replacements) {
		if(getExpression() != null && call.getExpression() != null) {
			String expression1 = getExpression();
			String expression2 = call.getExpression();
			String expression1AfterReplacements = new String(expression1);
			for(Replacement replacement : replacements) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					expression1AfterReplacements = ReplacementUtil.performReplacement(expression1AfterReplacements, replacement.getBefore(), replacement.getAfter());
				}
			}
			if(expression1AfterReplacements.equals(expression2)) {
				return true;
			}
		}
		return false;
	}

	public boolean identicalArguments(AbstractCall call, Set<String> set1, Set<String> set2) {
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() != arguments2.size())
			return false;
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			if(!argument1.equals(argument2) && !set1.contains(argument2) && !set2.contains(argument1))
				return false;
		}
		return true;
	}

	public boolean identicalOrReplacedArguments(AbstractCall call, Set<Replacement> replacements) {
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() != arguments2.size())
			return false;
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean argumentReplacement = false;
			for(Replacement replacement : replacements) {
				if(replacement.getBefore().equals(argument1) &&	replacement.getAfter().equals(argument2)) {
					argumentReplacement = true;
					break;
				}
			}
			if(!argument1.equals(argument2) && !argumentReplacement)
				return false;
		}
		return true;
	}

	public boolean allArgumentsReplaced(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		int replacedArguments = 0;
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() == arguments2.size()) {
			for(int i=0; i<arguments1.size(); i++) {
				String argument1 = arguments1.get(i);
				String argument2 = arguments2.get(i);
				for(Replacement replacement : replacements) {
					if( (replacement.getBefore().equals(argument1) || replacement.getBefore().equals(parameterToArgumentMap.get(argument1))) &&
							(replacement.getAfter().equals(argument2) || replacement.getAfter().equals(parameterToArgumentMap.get(argument2))) ) {
						replacedArguments++;
						break;
					}
				}
			}
		}
		return replacedArguments > 0 && replacedArguments == arguments1.size();
	}
}
