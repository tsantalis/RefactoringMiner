package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.CodeRange;

public abstract class AbstractCall implements LocationInfoProvider {
	protected int typeArguments;
	protected String expression;
	protected List<String> arguments;
	protected LocationInfo locationInfo;
	protected StatementCoverageType coverage = StatementCoverageType.NONE;

	public String getExpression() {
		return expression;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public StatementCoverageType getCoverage() {
		return coverage;
	}

	public abstract boolean identicalName(AbstractCall call);
	public abstract String getName();
	public abstract double normalizedNameDistance(AbstractCall call);
	public abstract AbstractCall update(String oldExpression, String newExpression);

	public boolean identicalExpression(AbstractCall call, Set<Replacement> replacements) {
		return identicalExpression(call) ||
		identicalExpressionAfterTypeReplacements(call, replacements);
	}

	public boolean identicalExpression(AbstractCall call) {
		return (getExpression() != null && call.getExpression() != null &&
				getExpression().equals(call.getExpression())) ||
				(getExpression() == null && call.getExpression() == null);
	}

	public boolean identicalExpressionAfterTypeReplacements(AbstractCall call, Set<Replacement> replacements) {
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

	public boolean equalArguments(AbstractCall call) {
		return getArguments().equals(call.getArguments());
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

	public boolean renamedWithIdenticalExpressionAndArguments(AbstractCall call, Set<Replacement> replacements) {
		return getExpression() != null && call.getExpression() != null &&
				identicalExpression(call, replacements) &&
				!identicalName(call) &&
				equalArguments(call);
	}

	public boolean renamedWithIdenticalArgumentsAndNoExpression(AbstractCall call, double distance) {
		return getExpression() == null && call.getExpression() == null &&
				!identicalName(call) &&
				normalizedNameDistance(call) <= distance &&
				equalArguments(call);
	}

	public boolean renamedWithIdenticalExpressionAndDifferentNumberOfArguments(AbstractCall call, Set<Replacement> replacements, double distance) {
		return getExpression() != null && call.getExpression() != null &&
				identicalExpression(call, replacements) &&
				normalizedNameDistance(call) <= distance &&
				!equalArguments(call) &&
				getArguments().size() != call.getArguments().size();
	}

	public boolean onlyArgumentsChanged(AbstractCall call, Set<Replacement> replacements) {
		return identicalExpression(call, replacements) &&
				identicalName(call) &&
				!equalArguments(call) &&
				getArguments().size() != call.getArguments().size();
	}

	public boolean identical(AbstractCall call, Set<Replacement> replacements) {
		return identicalExpression(call, replacements) &&
				identicalName(call) &&
				equalArguments(call);
	}

	public Set<String> argumentIntersection(AbstractCall call) {
		Set<String> argumentIntersection = new LinkedHashSet<String>(getArguments());
		argumentIntersection.retainAll(call.getArguments());
		return argumentIntersection;
	}

	private boolean argumentIsReturned(String statement) {
		return statement.startsWith("return ") && getArguments().size() == 1 &&
				//length()-2 to remove ";\n" from the end of the return statement, 7 to remove the prefix "return "
				equalsIgnoringExtraParenthesis(getArguments().get(0), statement.substring(7, statement.length()-2));
	}

	public Replacement makeReplacementForReturnedArgument(String statement) {
		if(argumentIsReturned(statement)) {
			return new Replacement(getArguments().get(0), statement.substring(7, statement.length()-2),
					ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
		}
		return null;
	}

	private boolean argumentIsAssigned(String statement) {
		return getArguments().size() == 1 && statement.contains("=") && statement.endsWith(";\n") &&
				//length()-2 to remove ";\n" from the end of the assignment statement, indexOf("=")+1 to remove the left hand side of the assignment
				equalsIgnoringExtraParenthesis(getArguments().get(0), statement.substring(statement.indexOf("=")+1, statement.length()-2));
	}

	public Replacement makeReplacementForAssignedArgument(String statement) {
		if(argumentIsAssigned(statement)) {
			return new Replacement(statement.substring(statement.indexOf("=")+1, statement.length()-2),
					getArguments().get(0), ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION);
		}
		return null;
	}

	private static boolean equalsIgnoringExtraParenthesis(String s1, String s2) {
		if(s1.equals(s2))
			return true;
		String parenthesizedS1 = "("+s1+")";
		if(parenthesizedS1.equals(s2))
			return true;
		String parenthesizedS2 = "("+s2+")";
		if(parenthesizedS2.equals(s1))
			return true;
		return false;
	}

	protected void update(AbstractCall newCall, String oldExpression, String newExpression) {
		newCall.typeArguments = this.typeArguments;
		if(this.expression != null && this.expression.equals(oldExpression)) {
			newCall.expression = newExpression;
		}
		else {
			newCall.expression = this.expression;
		}
		newCall.arguments = new ArrayList<String>();
		for(String argument : this.arguments) {
			newCall.arguments.add(
				ReplacementUtil.performReplacement(argument, oldExpression, newExpression));
		}
	}

	public CodeRange codeRange() {
		LocationInfo info = getLocationInfo();
		return info.codeRange();
	}

	public enum StatementCoverageType {
		NONE, ONLY_CALL, RETURN_CALL, THROW_CALL, CAST_CALL, VARIABLE_DECLARATION_INITIALIZER_CALL;
	}
}
