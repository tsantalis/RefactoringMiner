package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.VariableDeclarationContainer;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.UMLModelDiff;

public abstract class AbstractCall implements LocationInfoProvider {
	protected int numberOfArguments;
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
	
	public boolean matchesOperation(VariableDeclarationContainer operation, VariableDeclarationContainer callerOperation, UMLModelDiff modelDiff) {
		if(this instanceof OperationInvocation) {
			return ((OperationInvocation)this).matchesOperation(operation, callerOperation, modelDiff);
		}
		return false;
	}

	public boolean compatibleExpression(AbstractCall other) {
		if(this instanceof OperationInvocation && other instanceof OperationInvocation) {
			return ((OperationInvocation)this).compatibleExpression((OperationInvocation)other);
		}
		else {
			if(this.expression != null && other.expression != null) {
				return this.expression.equals(other.expression);
			}
			return false;
		}
	}

	public boolean differentExpressionNameAndArguments(AbstractCall other) {
		boolean differentExpression = false;
		if(this.expression == null && other.expression != null)
			differentExpression = true;
		if(this.expression != null && other.expression == null)
			differentExpression = true;
		if(this.expression != null && other.expression != null)
			differentExpression = !this.expression.equals(other.expression) &&
			!this.expression.startsWith(other.expression) && !other.expression.startsWith(this.expression);
		boolean differentName = !this.getName().equals(other.getName());
		Set<String> argumentIntersection = new LinkedHashSet<String>(this.arguments);
		argumentIntersection.retainAll(other.arguments);
		boolean argumentFoundInExpression = false;
		if(this.expression != null) {
			for(String argument : other.arguments) {
				if(this.expression.contains(argument)) {
					argumentFoundInExpression = true;
				}
			}
		}
		if(other.expression != null) {
			for(String argument : this.arguments) {
				if(other.expression.contains(argument)) {
					argumentFoundInExpression = true;
				}
			}
		}
		boolean differentArguments = !this.arguments.equals(other.arguments) &&
				argumentIntersection.isEmpty() && !argumentFoundInExpression;
		return differentExpression && differentName && differentArguments;
	}

	public String actualString() {
		StringBuilder sb = new StringBuilder();
		if(expression != null) {
			sb.append(expression).append(".");
		}
		sb.append(getName());
		sb.append("(");
		int size = arguments.size();
		if(size > 0) {
			for(int i=0; i<size-1; i++)
				sb.append(arguments.get(i)).append(",");
			sb.append(arguments.get(size-1));
		}
		sb.append(")");
		return sb.toString();
	}

	public boolean expressionIsNullOrThis() {
		if(expression == null) {
			return true;
		}
		else if(expression.equals("this")) {
			return true;
		}
		return false;
	}

	public boolean identicalExpression(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		return identicalExpression(call) ||
		identicalExpressionAfterTypeReplacements(call, replacements, parameterToArgumentMap);
	}

	public boolean identicalExpression(AbstractCall call) {
		return (getExpression() != null && call.getExpression() != null &&
				getExpression().equals(call.getExpression())) ||
				(getExpression() == null && call.getExpression() == null);
	}

	private boolean identicalExpressionAfterTypeReplacements(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		if(getExpression() != null && call.getExpression() != null) {
			String expression1 = getExpression();
			String expression2 = call.getExpression();
			String expression1AfterReplacements = new String(expression1);
			for(Replacement replacement : replacements) {
				if(replacement.getType().equals(ReplacementType.TYPE) ||
						//allow only class names corresponding to static calls
						(replacement.getType().equals(ReplacementType.VARIABLE_NAME) && expressionCondition(expression1, expression2, parameterToArgumentMap))) {
					if(replacement.getBefore().equals(expression1) && (replacement.getAfter().equals(expression2) ||
							(parameterToArgumentMap.containsKey(expression2) && replacement.getAfter().equals(parameterToArgumentMap.get(expression2))))) {
						return true;
					}
					expression1AfterReplacements = ReplacementUtil.performReplacement(expression1AfterReplacements, expression2, replacement.getBefore(), replacement.getAfter());
				}
			}
			if(expression1AfterReplacements.equals(expression2)) {
				return true;
			}
		}
		return false;
	}

	private boolean expressionCondition(String expression1, String expression2, Map<String, String> parameterToArgumentMap) {
		if(Character.isUpperCase(expression1.charAt(0)) && Character.isUpperCase(expression2.charAt(0))) {
			return true;
		}
		else if(Character.isUpperCase(expression1.charAt(0)) && !Character.isUpperCase(expression2.charAt(0)) && parameterToArgumentMap.containsKey(expression2)) {
			return true;
		}
		return false;
	}

	public boolean staticInvokerExpressionReplaced(AbstractCall call, Set<Replacement> replacements) {
		if(getExpression() != null && call.getExpression() != null) {
			String expression1 = getExpression();
			String expression2 = call.getExpression();
			for(Replacement replacement : replacements) {
				if(replacement.getBefore().equals(expression1) && replacement.getAfter().equals(expression2)) {
					if(Character.isUpperCase(expression1.charAt(0)) && Character.isUpperCase(expression2.charAt(0))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean equalArgumentsExceptForStringLiterals(AbstractCall call) {
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() != arguments2.size())
			return false;
		int stringLiterals1 = 0;
		int stringLiterals2 = 0;
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean stringLiteral1 = isStringLiteral(argument1);
			if(stringLiteral1) {
				stringLiterals1++;
			}
			boolean stringLiteral2 = isStringLiteral(argument2);
			if(stringLiteral2) {
				stringLiterals2++;
			}
			if(!stringLiteral1 || !stringLiteral2) {
				if(!argument1.equals(argument2)) {
					return false;
				}
			}
		}
		return stringLiterals1 == stringLiterals2 && stringLiterals1 > 0;
	}

	public boolean isStringLiteral(String argument) {
		return argument.startsWith("\"") && argument.endsWith("\"");
	}

	public boolean equalArguments(AbstractCall call) {
		return getArguments().equals(call.getArguments());
	}

	public boolean reorderedArguments(AbstractCall call) {
		return getArguments().size() > 1 && getArguments().size() == call.getArguments().size() &&
				!getArguments().equals(call.getArguments()) && getArguments().containsAll(call.getArguments());
	}

	public boolean identicalOrReplacedArguments(AbstractCall call, Set<Replacement> replacements, List<UMLOperationBodyMapper> lambdaMappers) {
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
			boolean lambdaReplacement = false;
			if(argument1.contains("->") && argument2.contains("->")) {
				for(UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
					if(lambdaMapper.nonMappedElementsT1() == 0 && lambdaMapper.nonMappedElementsT2() == 0) {
						lambdaReplacement = true;
						break;
					}
				}
			}
			if(!argument1.equals(argument2) && !argumentReplacement && !lambdaReplacement)
				return false;
		}
		return true;
	}

	public boolean identicalOrConcatenatedArguments(AbstractCall call) {
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() != arguments2.size())
			return false;
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean argumentConcatenated = false;
			if((argument1.contains("+") || argument2.contains("+")) && !argument1.contains("++") && !argument2.contains("++")) {
				Set<String> tokens1 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(argument1)));
				Set<String> tokens2 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(argument2)));
				Set<String> intersection = new LinkedHashSet<String>(tokens1);
				intersection.retainAll(tokens2);
				int size = intersection.size();
				int threshold = Math.max(tokens1.size(), tokens2.size()) - size;
				if(size > 0 && size >= threshold) {
					argumentConcatenated = true;
				}
			}
			if(!argument1.equals(argument2) && !argumentConcatenated)
				return false;
		}
		return true;
	}

	public boolean identicalOrWrappedArguments(AbstractCall call) {
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() != arguments2.size())
			return false;
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean argumentWrapped = false;
			if(argument1.contains("(" + argument2 + ")") ||
					argument2.contains("(" + argument1 + ")")) {
				argumentWrapped = true;
			}
			if(!argument1.equals(argument2) && !argumentWrapped)
				return false;
		}
		return true;
	}

	public boolean allArgumentsReplaced(AbstractCall call, Set<Replacement> replacements) {
		int replacedArguments = 0;
		List<String> arguments1 = getArguments();
		List<String> arguments2 = call.getArguments();
		if(arguments1.size() == arguments2.size()) {
			for(int i=0; i<arguments1.size(); i++) {
				String argument1 = arguments1.get(i);
				String argument2 = arguments2.get(i);
				for(Replacement replacement : replacements) {
					if( (replacement.getBefore().equals(argument1) || argument1.contains(replacement.getBefore())) &&
							(replacement.getAfter().equals(argument2) || argument2.contains(replacement.getAfter())) ) {
						replacedArguments++;
						break;
					}
				}
			}
		}
		return replacedArguments > 0 && replacedArguments == arguments1.size();
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

	public boolean renamedWithIdenticalExpressionAndArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap, double distance,
			List<UMLOperationBodyMapper> lambdaMappers, boolean matchPairOfRemovedAddedOperationsWithIdenticalBody) {
		boolean identicalOrReplacedArguments = identicalOrReplacedArguments(call, replacements, lambdaMappers);
		boolean allArgumentsReplaced = allArgumentsReplaced(call, replacements);
		return ((getExpression() != null && call.getExpression() != null) || matchPairOfRemovedAddedOperationsWithIdenticalBody) &&
				identicalExpression(call, replacements, parameterToArgumentMap) &&
				!identicalName(call) &&
				(equalArguments(call) || reorderedArguments(call) || (allArgumentsReplaced && compatibleName(call, distance)) || (identicalOrReplacedArguments && !allArgumentsReplaced));
	}

	public boolean variableDeclarationInitializersRenamedWithIdenticalArguments(AbstractCall call) {
		return this.coverage.equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL) &&
				call.coverage.equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL) &&
				getExpression() != null && call.getExpression() != null &&
				!identicalName(call) && (equalArguments(call) || reorderedArguments(call) || this.getArguments().size() == 0 || call.getArguments().size() == 0);
	}

	public boolean renamedWithDifferentExpressionAndIdenticalArguments(AbstractCall call) {
		return (this.getName().contains(call.getName()) || call.getName().contains(this.getName())) &&
				(equalArguments(call) || reorderedArguments(call)) && this.arguments.size() > 0 &&
				((this.getExpression() == null && call.getExpression() != null) || (call.getExpression() == null && this.getExpression() != null));
	}

	public boolean renamedWithIdenticalArgumentsAndNoExpression(AbstractCall call, double distance, List<UMLOperationBodyMapper> lambdaMappers) {
		boolean allExactLambdaMappers = lambdaMappers.size() > 0;
		for(UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
			if(!lambdaMapper.allMappingsAreExactMatches()) {
				allExactLambdaMappers = false;
				break;
			}
		}
		return getExpression() == null && call.getExpression() == null &&
				!identicalName(call) &&
				(compatibleName(call, distance) || allExactLambdaMappers) &&
				(equalArguments(call) || reorderedArguments(call));
	}

	public boolean renamedWithIdenticalExpressionAndDifferentArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap, double distance, List<UMLOperationBodyMapper> lambdaMappers) {
		boolean allExactLambdaMappers = lambdaMappers.size() > 0;
		for(UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
			if(!lambdaMapper.allMappingsAreExactMatches()) {
				allExactLambdaMappers = false;
				break;
			}
		}
		return getExpression() != null && call.getExpression() != null &&
				identicalExpression(call, replacements, parameterToArgumentMap) &&
				(normalizedNameDistance(call) <= distance || allExactLambdaMappers || (this.methodNameContainsArgumentName() && call.methodNameContainsArgumentName()) || argumentIntersectionContainsClassInstanceCreation(call)) &&
				!equalArguments(call) &&
				!this.argumentContainsAnonymousClassDeclaration() && !call.argumentContainsAnonymousClassDeclaration();
	}

	private boolean compatibleName(AbstractCall call, double distance) {
		if(normalizedNameDistance(call) <= distance) {
			return true;
		}
		String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(this.getName());
		String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(call.getName());
		int commonTokens = 0;
		for(String token1 : tokens1) {
			for(String token2 : tokens2) {
				if(token1.equals(token2)) {
					commonTokens++;
				}
			}
		}
		if(commonTokens == Math.min(tokens1.length, tokens2.length)) {
			return true;
		}
		if(this.getExpression() != null && this.getExpression().toLowerCase().startsWith("log") &&
				call.getExpression() != null && call.getExpression().toLowerCase().startsWith("log") &&
				this.getExpression().equals(call.getExpression())) {
			List<String> logNames = List.of("trace", "debug", "info", "warn", "error", "fatal");
			if(logNames.contains(this.getName()) && logNames.contains(call.getName())) {
				if(this.getArguments().size() == call.getArguments().size() && this.getArguments().size() == 1) {
					String argument1 = this.getArguments().get(0);
					String argument2 = call.getArguments().get(0);
					String[] words1 = argument1.split("\\s");
					String[] words2 = argument2.split("\\s");
					int commonWords = 0;
					if(words1.length <= words2.length) {
						for(String word1 : words1) {
							String w1 = word1.replaceAll("^\"|\"$", "");
							if(!w1.equals("+") && !w1.equals("")) {
								for(String word2 : words2) {
									String w2 = word2.replaceAll("^\"|\"$", "");
									if(w1.equals(w2) || w1.equals(w2 + ".") || w2.equals(w1 + ".")) {
										commonWords++;
										break;
									}
								}
							}
						}
					}
					else {
						for(String word2 : words2) {
							String w2 = word2.replaceAll("^\"|\"$", "");
							if(!w2.equals("+") && !w2.equals("")) {
								for(String word1 : words1) {
									String w1 = word1.replaceAll("^\"|\"$", "");
									if(w1.equals(w2) || w1.equals(w2 + ".") || w2.equals(w1 + ".")) {
										commonWords++;
										break;
									}
								}
							}
						}
					}
					if(commonWords >= Math.max(words1.length, words2.length)/2) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean argumentIntersectionContainsClassInstanceCreation(AbstractCall call) {
		for(String argument : argumentIntersection(call)) {
			if(argument.startsWith("new ")) {
				return true;
			}
		}
		return false;
	}

	private boolean argumentContainsAnonymousClassDeclaration() {
		for(String argument : arguments) {
			if(argument.contains("{\n")) {
				return true;
			}
		}
		return false;
	}

	public boolean methodNameContainsArgumentName() {
		for(String argument : arguments) {
			if(getName().toLowerCase().endsWith(argument.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private boolean onlyArgumentsChanged(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		return identicalExpression(call, replacements, parameterToArgumentMap) &&
				identicalName(call) &&
				!equalArguments(call) &&
				getArguments().size() != call.getArguments().size();
	}

	public boolean identicalWithMergedArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		if(onlyArgumentsChanged(call, replacements, parameterToArgumentMap)) {
			List<String> updatedArguments1 = new ArrayList<String>(this.arguments);
			Map<String, Set<Replacement>> commonVariableReplacementMap = new LinkedHashMap<String, Set<Replacement>>();
			for(Replacement replacement : replacements) {
				if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
					String key = replacement.getAfter();
					if(commonVariableReplacementMap.containsKey(key)) {
						commonVariableReplacementMap.get(key).add(replacement);
						int index = updatedArguments1.indexOf(replacement.getBefore());
						if(index != -1) {
							updatedArguments1.remove(index);
						}
					}
					else {
						Set<Replacement> r = new LinkedHashSet<Replacement>();
						r.add(replacement);
						commonVariableReplacementMap.put(key, r);
						int index = updatedArguments1.indexOf(replacement.getBefore());
						if(index != -1) {
							updatedArguments1.remove(index);
							updatedArguments1.add(index, key);
						}
					}
				}
			}
			if(updatedArguments1.equals(call.arguments)) {
				for(String key : commonVariableReplacementMap.keySet()) {
					Set<Replacement> r = commonVariableReplacementMap.get(key);
					if(r.size() > 1) {
						replacements.removeAll(r);
						Set<String> mergedVariables = new LinkedHashSet<String>();
						for(Replacement replacement : r) {
							mergedVariables.add(replacement.getBefore());
						}
						MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
						replacements.add(merge);
					}
				}
				return true;
			}
		}
		return false;
	}

	public boolean identicalWithDifferentNumberOfArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		if(onlyArgumentsChanged(call, replacements, parameterToArgumentMap)) {
			int argumentIntersectionSize = argumentIntersectionSize(call, replacements, parameterToArgumentMap);
			if(argumentIntersectionSize > 0 || getArguments().size() == 0 || call.getArguments().size() == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean inlinedStatementBecomesAdditionalArgument(AbstractCall call, Set<Replacement> replacements, List<? extends AbstractCodeFragment> statements) {
		if(identicalName(call) && this.arguments.size() < call.arguments.size() && this.argumentIntersection(call).size() > 0) {
			int matchedArguments = 0;
			Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
			for(String arg : call.arguments) {
				if(this.arguments.contains(arg)) {
					matchedArguments++;
				}
				else {
					for(AbstractCodeFragment statement : statements) {
						if(statement.getVariableDeclarations().size() > 0) {
							VariableDeclaration variableDeclaration = statement.getVariableDeclarations().get(0);
							if(variableDeclaration.getInitializer() != null) {
								if(arg.equals(variableDeclaration.getInitializer().getExpression())) {
									matchedArguments++;
									additionallyMatchedStatements1.add(statement);
									break;
								}
							}
						}
					}
				}
			}
			if(matchedArguments == call.arguments.size()) {
				if(additionallyMatchedStatements1.size() > 0) {
					CompositeReplacement r = new CompositeReplacement(this.actualString(), call.actualString(), additionallyMatchedStatements1, Collections.emptySet());
					replacements.add(r);
				}
				return true;
			}
		}
		return false;
	}

	public boolean identicalWithInlinedStatements(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap, List<AbstractCodeFragment> statements) {
		if(identicalExpression(call, replacements, parameterToArgumentMap) && identicalName(call)) {
			if(this.arguments.size() == call.arguments.size()) {
				Set<Replacement> newReplacements = new LinkedHashSet<Replacement>();
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				for(int i=0; i<this.arguments.size(); i++) {
					String arg1 = this.arguments.get(i);
					String arg2 = call.arguments.get(i);
					if(!arg1.equals(arg2)) {
						boolean matchingInlineFound = false;
						for(AbstractCodeFragment statement : statements) {
							if(statement.getVariableDeclarations().size() > 0) {
								VariableDeclaration variableDeclaration = statement.getVariableDeclarations().get(0);
								if(variableDeclaration.getVariableName().equals(arg1) && variableDeclaration.getInitializer() != null) {
									AbstractCall statementCall = variableDeclaration.getInitializer().invocationCoveringEntireFragment();
									if(variableDeclaration.getInitializer().getExpression().equals(arg2)) {
										matchingInlineFound = true;
										if(statementCall != null) {
											Replacement r = new VariableReplacementWithMethodInvocation(arg1, variableDeclaration.getInitializer().getExpression(), statementCall, Direction.VARIABLE_TO_INVOCATION);
											newReplacements.add(r);
										}
										else {
											Replacement r = new Replacement(arg1, variableDeclaration.getInitializer().getExpression(), ReplacementType.VARIABLE_NAME);
											newReplacements.add(r);
										}
										additionallyMatchedStatements1.add(statement);
										break;
									}
									if(statementCall != null) {
										String actualString = statementCall.actualString();
										if(arg2.contains(actualString) || actualString.contains(arg2)) {
											matchingInlineFound = true;
											Replacement r = new VariableReplacementWithMethodInvocation(arg1, variableDeclaration.getInitializer().getExpression(), statementCall, Direction.VARIABLE_TO_INVOCATION);
											newReplacements.add(r);
											additionallyMatchedStatements1.add(statement);
											break;
										}
										if(actualString.contains(".") && arg2.contains(".") &&
												actualString.substring(actualString.indexOf(".")).equals(arg2.substring(arg2.indexOf(".")))) {
											matchingInlineFound = true;
											Replacement r = new VariableReplacementWithMethodInvocation(arg1, variableDeclaration.getInitializer().getExpression(), statementCall, Direction.VARIABLE_TO_INVOCATION);
											newReplacements.add(r);
											additionallyMatchedStatements1.add(statement);
											String invoker = actualString.substring(0, actualString.indexOf("."));
											if(invoker.equals(statementCall.getExpression())) {
												String arg2Invoker = arg2.substring(0, arg2.indexOf("."));
												Replacement rename = new Replacement(invoker, arg2Invoker, ReplacementType.VARIABLE_NAME);
												newReplacements.add(rename);
											}
											break;
										}
									}
								}
							}
						}
						if(!matchingInlineFound) {
							return false;
						}
					}
				}
				for(Replacement r : newReplacements) {
					if(!replacements.contains(r)) {
						replacements.add(r);
					}
				}
				if(additionallyMatchedStatements1.size() > 0) {
					CompositeReplacement r = new CompositeReplacement(this.actualString(), call.actualString(), additionallyMatchedStatements1, Collections.emptySet());
					replacements.add(r);
				}
				return true;
			}
		}
		return false;
	}

	public boolean identical(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap, List<UMLOperationBodyMapper> lambdaMappers) {
		return identicalExpression(call, replacements, parameterToArgumentMap) &&
				identicalName(call) &&
				(equalArguments(call) || onlyLambdaArgumentsDiffer(call, lambdaMappers));
	}

	private boolean onlyLambdaArgumentsDiffer(AbstractCall call, List<UMLOperationBodyMapper> lambdaMappers) {
		if(lambdaMappers.size() > 0) {
			List<String> arguments1 = getArguments();
			List<String> arguments2 = call.getArguments();
			if(arguments1.size() == arguments2.size()) {
				for(int i=0; i<arguments1.size(); i++) {
					String argument1 = arguments1.get(i);
					String argument2 = arguments2.get(i);
					if(argument1.contains("->") && argument2.contains("->")) {
						for(UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
							if(lambdaMapper.nonMappedElementsT1() > 0 && lambdaMapper.nonMappedElementsT2() > 0 &&
									!lambdaMapper.containsCallToExtractedMethod() && !lambdaMapper.containsCallToInlinedMethod()) {
								return false;
							}
						}
					}
					else if(!argument1.equals(argument2)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public Set<String> argumentIntersection(AbstractCall call) {
		List<String> args1 = preprocessArguments(getArguments());
		List<String> args2 = preprocessArguments(call.getArguments());
		Set<String> argumentIntersection = new LinkedHashSet<String>(args1);
		argumentIntersection.retainAll(args2);
		return argumentIntersection;
	}

	private List<String> preprocessArguments(List<String> arguments) {
		List<String> args = new ArrayList<String>();
		for(String arg : arguments) {
			if(arg.contains("\n")) {
				args.add(arg.substring(0, arg.indexOf("\n")));
			}
			else {
				args.add(arg);
			}
		}
		return args;
	}

	private int argumentIntersectionSize(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		Set<String> argumentIntersection = argumentIntersection(call);
		int argumentIntersectionSize = argumentIntersection.size();
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(getArguments().contains(argument) &&
					call.getArguments().contains(parameter)) {
				argumentIntersectionSize++;
			}
		}
		for(Replacement r : replacements) {
			if(r.isLiteral() && getArguments().contains(r.getBefore()) && call.getArguments().contains(r.getAfter())) {
				argumentIntersectionSize++;
			}
		}
		return argumentIntersectionSize;
	}

	private boolean argumentIsStatement(String statement) {
		if(statement.endsWith(";\n")) {
			for(String argument : getArguments()) {
				//length()-2 to remove ";\n" from the end of the statement
				if(equalsIgnoringExtraParenthesis(argument, statement.substring(0, statement.length()-2))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean argumentIsExpression(String expression) {
		if(!expression.endsWith(";\n")) {
			//statement is actually an expression
			for(String argument : getArguments()) {
				if(equalsIgnoringExtraParenthesis(argument, expression)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean argumentIsReturned(String statement) {
		if(statement.startsWith("return ")) {
			for(String argument : getArguments()) {
				//length()-2 to remove ";\n" from the end of the return statement, 7 to remove the prefix "return "
				if(equalsIgnoringExtraParenthesis(argument, statement.substring(7, statement.length()-2))) {
					return true;
				}
			}
		}
		return false;
	}

	public Replacement makeReplacementForReturnedArgument(String statement) {
		if(argumentIsReturned(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(getArguments().get(0), statement.substring(7, statement.length()-2),
					ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
		}
		else if(argumentIsStatement(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(getArguments().get(0), statement.substring(0, statement.length()-2),
					ReplacementType.ARGUMENT_REPLACED_WITH_STATEMENT);
		}
		else if(argumentIsExpression(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(getArguments().get(0), statement,
					ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION);
		}
		return null;
	}

	public Replacement makeReplacementForWrappedCall(String statement) {
		if(argumentIsReturned(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(statement.substring(7, statement.length()-2), getArguments().get(0),
					ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
		}
		else if(argumentIsStatement(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(statement.substring(0, statement.length()-2), getArguments().get(0),
					ReplacementType.ARGUMENT_REPLACED_WITH_STATEMENT);
		}
		else if(argumentIsExpression(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(statement, getArguments().get(0),
					ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION);
		}
		return null;
	}

	public Replacement makeReplacementForWrappedLambda(String statement) {
		if(argumentIsLambdaStatement(statement) && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(statement.substring(0, statement.length()-2), getArguments().get(0),
					ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION);
		}
		return null;
	}

	private boolean argumentIsLambdaStatement(String statement) {
		if(statement.endsWith(";\n")) {
			for(String argument : getArguments()) {
				//length()-2 to remove ";\n" from the end of the statement
				if(equalsIgnoringLambdaArrow(argument, statement.substring(0, statement.length()-2))) {
					return true;
				}
			}
		}
		return false;
	}

	private int argumentIsAssigned(String statement) {
		if(statement.contains("=") && statement.endsWith(";\n")) {
			int index = 0;
			for(String argument : getArguments()) {
				//length()-2 to remove ";\n" from the end of the assignment statement, indexOf("=")+1 to remove the left hand side of the assignment
				if(equalsIgnoringExtraParenthesis(argument, statement.substring(statement.indexOf("=")+1, statement.length()-2))) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	public Replacement makeReplacementForAssignedArgument(String statement) {
		int index = argumentIsAssigned(statement);
		if(index >= 0 && (getArguments().size() == 1 || (statement.contains("?") && statement.contains(":")))) {
			return new Replacement(statement.substring(statement.indexOf("=")+1, statement.length()-2),
					getArguments().get(index), ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION);
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

	private static boolean equalsIgnoringLambdaArrow(String s1, String s2) {
		if(s1.equals(s2))
			return true;
		String arrowS1 = "() -> " + s1;
		if(arrowS1.equals(s2))
			return true;
		String arrowS2 = "() -> " + s2;
		if(arrowS2.equals(s1))
			return true;
		return false;
	}

	protected void update(AbstractCall newCall, String oldExpression, String newExpression) {
		newCall.numberOfArguments = this.numberOfArguments;
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
	public Set<String> callChainIntersection(AbstractCall call) {
		if(this instanceof OperationInvocation && call instanceof OperationInvocation) {
			return ((OperationInvocation)this).callChainIntersection((OperationInvocation)call);
		}
		return Collections.emptySet();
	}

	public CodeRange codeRange() {
		LocationInfo info = getLocationInfo();
		return info.codeRange();
	}

	public enum StatementCoverageType {
		NONE, ONLY_CALL, RETURN_CALL, THROW_CALL, CAST_CALL, VARIABLE_DECLARATION_INITIALIZER_CALL;
	}
}
