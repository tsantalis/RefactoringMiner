package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public abstract class AbstractCall extends LeafExpression {
	protected int numberOfArguments;
	protected String expression;
	protected List<String> arguments;
	protected StatementCoverageType coverage = StatementCoverageType.NONE;
	private static final List<String> logNames = List.of("trace", "debug", "info", "warn", "error", "fatal", "log");
	private static final List<String> logGuardNames = List.of("isDebugEnabled", "isEnabled", "isErrorEnabled", "isFatalEnabled", "isInfoEnabled", "isTraceEnabled", "isWarnEnabled");

	public AbstractCall(CompilationUnit cu, String filePath, ASTNode expression, CodeElementType codeElementType, VariableDeclarationContainer container) {
		super(cu, filePath, expression, codeElementType, container);
	}

	protected AbstractCall() {
		
	}

	public LeafExpression asLeafExpression() {
		return new LeafExpression(getString(), getLocationInfo());
	}

	public String getExpression() {
		return expression;
	}

	public List<String> arguments() {
		return arguments;
	}

	public StatementCoverageType getCoverage() {
		return coverage;
	}

	public boolean isSuperCall() {
		if(expression != null && expression.equals("super")) {
			return true;
		}
		if(getName().equals("super")) {
			return true;
		}
		return false;
	}

	public abstract boolean identicalName(AbstractCall call);
	public abstract String getName();
	public abstract double normalizedNameDistance(AbstractCall call);
	public abstract AbstractCall update(String oldExpression, String newExpression);
	
	public boolean matchesOperation(VariableDeclarationContainer operation, VariableDeclarationContainer callerOperation,
			UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
		if(this instanceof OperationInvocation) {
			return ((OperationInvocation)this).matchesOperation(operation, callerOperation, classDiff, modelDiff);
		}
		else if(this instanceof MethodReference) {
			MethodReference methodReference = (MethodReference)this;
			return methodReference.getName().equals(operation.getName());
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

	public boolean isLogGuard() {
		return loggerExpression() && logGuardNames.contains(this.getName());
	}

	public boolean isLog() {
		return loggerExpression() && matchesLogName();
	}

	public boolean loggerExpression() {
		if(expression != null) {
			if(expression.equals("log") || expression.equals("LOG") || expression.equals("logger") || expression.equals("LOGGER") || expression.equals("Log")) {
				return true;
			}
		}
		return false;
	}

	public boolean matchesLogName() {
		//special handling for Android Log.e()
		return logNames.contains(this.getName()) || (expression != null && expression.equals("Log") && this.getName().equals("e"));
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
		identicalExpressionAfterTypeReplacements(call, replacements, parameterToArgumentMap) ||
		identicalExpressionAfterArgumentAddition(call, replacements);
	}

	public boolean identicalExpression(AbstractCall call) {
		return (getExpression() != null && call.getExpression() != null &&
				getExpression().equals(call.getExpression())) ||
				(getExpression() == null && call.getExpression() == null);
	}

	private boolean identicalExpressionAfterArgumentAddition(AbstractCall call, Set<Replacement> replacements) {
		if(getExpression() != null && call.getExpression() != null) {
			int methodInvocationReplacements = 0;
			int argumentAdditionReplacements = 0;
			for(Replacement replacement : replacements) {
				if(replacement instanceof MethodInvocationReplacement) {
					if(getExpression().contains(replacement.getBefore()) && call.getExpression().contains(replacement.getAfter())) {
						methodInvocationReplacements++;
						MethodInvocationReplacement r = (MethodInvocationReplacement)replacement;
						AbstractCall callBefore = r.getInvokedOperationBefore();
						AbstractCall callAfter = r.getInvokedOperationAfter();
						Set<String> argumentIntersection = callBefore.argumentIntersection(callAfter);
						if(argumentIntersection.size() > 0 &&
								argumentIntersection.size() == Math.min(new LinkedHashSet<>(callBefore.arguments()).size(), new LinkedHashSet<>(callAfter.arguments()).size()) &&
								callBefore.arguments().size() != callAfter.arguments().size()) {
							argumentAdditionReplacements++;
						}
					}
				}
			}
			if(methodInvocationReplacements == argumentAdditionReplacements && methodInvocationReplacements > 0) {
				return true;
			}
		}
		return false;
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
				else if(replacement instanceof MethodInvocationReplacement) {
					MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
					AbstractCall before = methodInvocationReplacement.getInvokedOperationBefore();
					AbstractCall after = methodInvocationReplacement.getInvokedOperationAfter();
					if(before.identicalExpression(after) && before.equalArguments(after) && before.arguments.size() > 0) {
						if(expression1.equals(replacement.getBefore()) && expression2.equals(replacement.getAfter())) {
							return true;
						}
					}
				}
				else if(replacement instanceof VariableReplacementWithMethodInvocation) {
					VariableReplacementWithMethodInvocation methodInvocationReplacement = (VariableReplacementWithMethodInvocation)replacement;
					AbstractCall invokedOperation = methodInvocationReplacement.getInvokedOperation();
					if(invokedOperation.actualString().contains("(")) {
						String subString = invokedOperation.actualString().substring(0, invokedOperation.actualString().indexOf("("));
						if(replacement.getBefore().startsWith(subString) && replacement.getAfter().startsWith(subString)) {
							if(replacement.getBefore().equals(expression1) && (replacement.getAfter().equals(expression2) ||
									(parameterToArgumentMap.containsKey(expression2) && replacement.getAfter().equals(parameterToArgumentMap.get(expression2))))) {
								return true;
							}
						}
					}
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

	public boolean equalArgumentsExceptForAnonymousClassArguments(AbstractCall call) {
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
		if(arguments1.size() != arguments2.size())
			return false;
		int anonymousClassArguments1 = 0;
		int anonymousClassArguments2 = 0; 
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean anonymousClassArgument1 = argument1.contains("{\n");
			if(anonymousClassArgument1) {
				anonymousClassArguments1++;
			}
			boolean anonymousClassArgument2 = argument2.contains("{\n");
			if(anonymousClassArgument2) {
				anonymousClassArguments2++;
			}
			if(!anonymousClassArgument1 || !anonymousClassArgument2) {
				if(!argument1.equals(argument2)) {
					return false;
				}
			}
		}
		return anonymousClassArguments1 == anonymousClassArguments2 && anonymousClassArguments1 > 0;
	}

	public boolean equalArgumentsExceptForStringLiterals(AbstractCall call) {
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
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
		return (argument.startsWith("\"") && argument.endsWith("\"")) ||
				(argument.contains("(\"") && argument.endsWith("\")"));
	}

	public boolean equalArguments(AbstractCall call) {
		if(this instanceof MethodReference && call instanceof MethodReference) {
			if(!this.identicalName(call)) {
				return false;
			}
		}
		return arguments().equals(call.arguments());
	}

	public boolean reorderedArguments(AbstractCall call) {
		return arguments().size() > 1 && arguments().size() == call.arguments().size() &&
				!arguments().equals(call.arguments()) && arguments().containsAll(call.arguments());
	}

	public boolean identicalOrReplacedArguments(AbstractCall call, Set<Replacement> replacements, List<UMLOperationBodyMapper> lambdaMappers) {
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
		if(arguments1.size() != arguments2.size())
			return false;
		if(this instanceof MethodReference && call instanceof MethodReference) {
			if(!this.identicalName(call)) {
				return false;
			}
		}
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean argumentReplacement = false;
			for(Replacement replacement : replacements) {
				String before = replacement.getBefore();
				String after = replacement.getAfter();
				if(before.equals(argument1) &&	after.equals(argument2)) {
					argumentReplacement = true;
					break;
				}
				else if(argument1.contains(before) && argument2.contains(after)) {
					String before1 = argument1.substring(0, argument1.indexOf(before));
					String before2 = argument2.substring(0, argument2.indexOf(after));
					String after1 = argument1.substring(argument1.indexOf(before) + before.length(), argument1.length());
					String after2 = argument2.substring(argument2.indexOf(after) + after.length(), argument2.length());
					if(before1.equals(before2) && after1.equals(after2)) {
						argumentReplacement = true;
						break;
					}
				}
			}
			boolean lambdaReplacement = false;
			if(argument1.contains(JAVA.LAMBDA_ARROW) && argument2.contains(JAVA.LAMBDA_ARROW)) {
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
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
		if(arguments1.size() != arguments2.size())
			return false;
		for(int i=0; i<arguments1.size(); i++) {
			String argument1 = arguments1.get(i);
			String argument2 = arguments2.get(i);
			boolean argumentConcatenated = false;
			if(argument1.contains(JAVA.STRING_CONCATENATION) || argument2.contains(JAVA.STRING_CONCATENATION)) {
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
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
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
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
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
		List<String> arguments1 = arguments();
		List<String> arguments2 = call.arguments();
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
			List<UMLOperationBodyMapper> lambdaMappers, boolean matchPairOfRemovedAddedOperationsWithIdenticalBody, boolean argumentsWithVariableDeclarationMapping) {
		boolean identicalOrReplacedArguments = identicalOrReplacedArguments(call, replacements, lambdaMappers);
		boolean allArgumentsReplaced = allArgumentsReplaced(call, replacements);
		return ((getExpression() != null && call.getExpression() != null) || matchPairOfRemovedAddedOperationsWithIdenticalBody) &&
				identicalExpression(call, replacements, parameterToArgumentMap) &&
				!identicalName(call) &&
				(equalArguments(call) || reorderedArguments(call) || (allArgumentsReplaced && compatibleName(call, distance)) || (identicalOrReplacedArguments && !allArgumentsReplaced) || argumentsWithVariableDeclarationMapping);
	}

	public boolean variableDeclarationInitializersRenamedWithIdenticalArguments(AbstractCall call) {
		return this.coverage.equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL) &&
				call.coverage.equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL) &&
				getExpression() != null && call.getExpression() != null &&
				!identicalName(call) && (equalArguments(call) || reorderedArguments(call) || this.arguments().size() == 0 || call.arguments().size() == 0);
	}

	public boolean renamedWithDifferentExpressionAndIdenticalArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		boolean expressionBecomesArgument = false;
		int commonTokens = 0;
		if(this.getExpression() != null && call.getExpression() == null && call.arguments().contains(this.getExpression())) {
			expressionBecomesArgument = true;
			String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(this.getName());
			String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(call.getName());
			for(String token1 : tokens1) {
				for(String token2 : tokens2) {
					if(token1.equals(token2)) {
						commonTokens++;
					}
				}
			}
		}
		else if(this.getExpression() == null && call.getExpression() != null && this.arguments().contains(call.getExpression())) {
			expressionBecomesArgument = true;
			String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(this.getName());
			String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(call.getName());
			for(String token1 : tokens1) {
				for(String token2 : tokens2) {
					if(token1.equals(token2)) {
						commonTokens++;
					}
				}
			}
		}
		return (oneNameContainsTheOther(call) || (expressionBecomesArgument && commonTokens > 1)) &&
				this.arguments.size() > 0 && call.arguments.size() > 0 && (equalArguments(call) || reorderedArguments(call) ||
				argumentIntersectionSize(call, replacements, parameterToArgumentMap) == Math.min(this.arguments.size(), call.arguments.size())) &&
				((this.getExpression() == null && call.getExpression() != null) || (call.getExpression() == null && this.getExpression() != null));
	}

	public boolean renamedWithNoExpressionAndArgumentIntersection(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap, boolean possiblyRenamedBasedOnClassDiff) {
		int argumentIntersectionSize = argumentIntersection(call).size();
		return (oneNameContainsTheOther(call) || (possiblyRenamedBasedOnClassDiff && (equalArguments(call) || commonTokens(call)))) &&
				(this.getExpression() == null && call.getExpression() == null) &&
				this.arguments.size() > 0 && call.arguments.size() > 0 &&
				(argumentIntersectionSize >= Math.floor(Math.min(this.arguments.size(), call.arguments.size())/2) ||
				(argumentIntersectionSize > 0 && this.getName().equals("super") && call.getName().equals("super")) ||
				argumentIntersectionSize(call, replacements, parameterToArgumentMap) == Math.min(this.arguments.size(), call.arguments.size()));
	}

	private boolean oneNameContainsTheOther(AbstractCall call) {
		return this.getName().contains(call.getName()) || call.getName().contains(this.getName());
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
		return compatibleName(call);
	}

	private boolean commonTokens(AbstractCall call) {
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
		return false;
	}

	public boolean compatibleName(AbstractCall call) {
		if(commonTokens(call)) {
			return true;
		}
		if(this.loggerExpression() && call.loggerExpression() && this.getExpression().equals(call.getExpression())) {
			if(this.matchesLogName() && call.matchesLogName()) {
				if(this.arguments().size() == call.arguments().size() && this.arguments().size() == 1) {
					String argument1 = this.arguments().get(0);
					String argument2 = call.arguments().get(0);
					List<String> words1 = extractWords(argument1);
					List<String> words2 = extractWords(argument2);
					int commonWords = 0;
					if(words1.size() <= words2.size()) {
						int index = 0;
						for(String word1 : words1) {
							if(words2.contains(word1)) {
								commonWords++;
							}
							if(word1.equals(words2.get(index) + "ing") || words2.get(index).equals(word1 + "ing")) {
								commonWords++;
							}
							index++;
						}
					}
					else {
						int index = 0;
						for(String word2 : words2) {
							if(words1.contains(word2)) {
								commonWords++;
							}
							if(word2.equals(words1.get(index) + "ing") || words1.get(index).equals(word2 + "ing")) {
								commonWords++;
							}
							index++;
						}
					}
					if(commonWords >= Math.max(words1.size(), words2.size())/2) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static List<String> extractWords(String argument) {
		String[] initialWords = argument.split("\\s");
		List<String> finalWords = new ArrayList<>();
		for(String word : initialWords) {
			//remove " : . , from the beginning and end of the string
			String w = word.replaceAll("^\"|\"$", "").replaceAll("^\\.|\\.$", "").replaceAll("^\\:|\\:$", "").replaceAll("^\\,|\\,$", "");
			if(!w.equals("+") && !w.equals("")) {
				finalWords.add(w);
			}
		}
		return finalWords;
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
				arguments().size() != call.arguments().size();
	}

	public boolean identicalWithOnlyChangesInAnonymousClassArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
		return identicalExpression(call, replacements, parameterToArgumentMap) &&
				identicalName(call) && !equalArguments(call) &&
				arguments().size() == call.arguments().size() &&
				equalArgumentsExceptForAnonymousClassArguments(call);
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
			if(argumentIntersectionSize > 0 || arguments().size() == 0 || call.arguments().size() == 0) {
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

	public boolean identicalWithExpressionArgumentSwap(AbstractCall call) {
		if(getExpression() != null && call.getExpression() != null && (identicalName(call) || oneNameContainsTheOther(call))) {
			int argumentIndex1 = arguments().indexOf(call.getExpression());
			int argumentIndex2 = call.arguments().indexOf(getExpression());
			if(argumentIndex1 != -1 && argumentIndex2 != -1 && argumentIndex1 == argumentIndex2) {
				Set<String> argumentIntersection = argumentIntersection(call);
				if(argumentIntersection.size() == arguments().size()-1 && argumentIntersection.size() == call.arguments().size()-1) {
					return true;
				}
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
			List<String> arguments1 = arguments();
			List<String> arguments2 = call.arguments();
			if(arguments1.size() == arguments2.size()) {
				for(int i=0; i<arguments1.size(); i++) {
					String argument1 = arguments1.get(i);
					String argument2 = arguments2.get(i);
					if(argument1.contains(JAVA.LAMBDA_ARROW) && argument2.contains(JAVA.LAMBDA_ARROW)) {
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
		List<String> args1 = preprocessArguments(arguments());
		List<String> args2 = preprocessArguments(call.arguments());
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
			if(arguments().contains(argument) &&
					call.arguments().contains(parameter)) {
				argumentIntersectionSize++;
			}
		}
		for(Replacement r : replacements) {
			int index1 = arguments().indexOf(r.getBefore());
			int index2 = call.arguments().indexOf(r.getAfter());
			if(index1 == -1 && arguments().contains(r.getBefore() + ".length")) {
				index1 = arguments().indexOf(r.getBefore() + ".length");
			}
			if(index2 == -1 && call.arguments().contains(r.getAfter() + ".length")) {
				index2 = call.arguments().indexOf(r.getAfter() + ".length");
			}
			if(index1 != -1 && index2 != -1) {
				if(arguments().size() == call.arguments().size()) {
					if(index1 == index2) {
						argumentIntersectionSize++;
					}
				}
				else {
					argumentIntersectionSize++;
				}
			}
		}
		return argumentIntersectionSize;
	}

	private int argumentIsStatement(String statement) {
		if(statement.endsWith(JAVA.STATEMENT_TERMINATION)) {
			int index = 0;
			for(String argument : arguments()) {
				if(argument.equals("true") || argument.equals("false") || argument.equals("null")) {
					return -1;
				}
				if(equalsIgnoringExtraParenthesis(argument, statement.substring(0, statement.length()-JAVA.STATEMENT_TERMINATION.length()))) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	private int argumentIsExpression(String expression) {
		if(!expression.endsWith(JAVA.STATEMENT_TERMINATION)) {
			//statement is actually an expression
			int index = 0;
			for(String argument : arguments()) {
				if(argument.equals("true") || argument.equals("false") || argument.equals("null")) {
					return -1;
				}
				if(equalsIgnoringExtraParenthesis(argument, expression)) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	private int argumentIsReturned(String statement) {
		if(statement.startsWith(JAVA.RETURN_SPACE)) {
			int index = 0;
			for(String argument : arguments()) {
				if(argument.equals("true") || argument.equals("false") || argument.equals("null")) {
					return -1;
				}
				if(equalsIgnoringExtraParenthesis(argument, statement.substring(JAVA.RETURN_SPACE.length(), statement.length()-JAVA.STATEMENT_TERMINATION.length()))) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	public Replacement makeReplacementForReturnedArgument(String statement) {
		int index = -1;
		if((index = argumentIsReturned(statement)) != -1 && ((arguments().size() <= 2 && (index == 0 || this.getName().equals("assertThrows"))) || (statement.contains(" ? ") && statement.contains(" : ")))) {
			String arg = statement.substring(JAVA.RETURN_SPACE.length(), statement.length()-JAVA.STATEMENT_TERMINATION.length());
			return new Replacement(arguments().get(index), arg,
					ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
		}
		else if((index = argumentIsStatement(statement)) != -1 && ((arguments().size() <= 2 && (index == 0 || this.getName().equals("assertThrows"))) || (statement.contains(" ? ") && statement.contains(" : ")))) {
			String arg = statement.substring(0, statement.length()-JAVA.STATEMENT_TERMINATION.length());
			return new Replacement(arguments().get(index), arg,
					ReplacementType.ARGUMENT_REPLACED_WITH_STATEMENT);
		}
		else if((index = argumentIsExpression(statement)) != -1 && ((arguments().size() <= 2 && (index == 0 || this.getName().equals("assertThrows"))) || (statement.contains(" ? ") && statement.contains(" : ")))) {
			return new Replacement(arguments().get(index), statement,
					ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION);
		}
		return null;
	}

	public Replacement makeReplacementForWrappedCall(String statement) {
		int index = -1;
		if((index = argumentIsReturned(statement)) != -1 && ((arguments().size() <= 2 && (index == 0 || this.getName().equals("assertThrows"))) || (statement.contains(" ? ") && statement.contains(" : ")))) {
			String arg = statement.substring(JAVA.RETURN_SPACE.length(), statement.length()-JAVA.STATEMENT_TERMINATION.length());
			return new Replacement(arg, arguments().get(index),
					ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
		}
		else if((index = argumentIsStatement(statement)) != -1 && ((arguments().size() <= 2 && (index == 0 || this.getName().equals("assertThrows"))) || (statement.contains(" ? ") && statement.contains(" : ")))) {
			String arg = statement.substring(0, statement.length()-JAVA.STATEMENT_TERMINATION.length());
			return new Replacement(arg, arguments().get(index),
					ReplacementType.ARGUMENT_REPLACED_WITH_STATEMENT);
		}
		else if((index = argumentIsExpression(statement)) != -1 && ((arguments().size() <= 2 && (index == 0 || this.getName().equals("assertThrows"))) || (statement.contains(" ? ") && statement.contains(" : ")))) {
			return new Replacement(statement, arguments().get(index),
					ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION);
		}
		return null;
	}

	public Replacement makeReplacementForWrappedLambda(String statement) {
		if(argumentIsLambdaStatement(statement) && (arguments().size() == 1 || (statement.contains(" ? ") && statement.contains(" : ")))) {
			return new Replacement(statement.substring(0, statement.length()-JAVA.STATEMENT_TERMINATION.length()), arguments().get(0),
					ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION);
		}
		return null;
	}

	private boolean argumentIsLambdaStatement(String statement) {
		if(statement.endsWith(JAVA.STATEMENT_TERMINATION)) {
			for(String argument : arguments()) {
				if(equalsIgnoringLambdaArrow(argument, statement.substring(0, statement.length()-JAVA.STATEMENT_TERMINATION.length()))) {
					return true;
				}
			}
		}
		return false;
	}

	private int argumentIsAssigned(String statement) {
		if(statement.contains(JAVA.ASSIGNMENT) && statement.endsWith(JAVA.STATEMENT_TERMINATION)) {
			int index = 0;
			for(String argument : arguments()) {
				if(equalsIgnoringExtraParenthesis(argument, statement.substring(statement.indexOf(JAVA.ASSIGNMENT)+1, statement.length()-JAVA.STATEMENT_TERMINATION.length()))) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	private boolean expressionIsAssigned(String statement) {
		if(statement.contains(JAVA.ASSIGNMENT) && statement.endsWith(JAVA.STATEMENT_TERMINATION) && expression != null) {
			if(equalsIgnoringExtraParenthesis(expression, statement.substring(statement.indexOf(JAVA.ASSIGNMENT)+1, statement.length()-JAVA.STATEMENT_TERMINATION.length()))) {
				return true;
			}
		}
		return false;
	}

	public Replacement makeReplacementForAssignedArgument(String statement) {
		int index = argumentIsAssigned(statement);
		if(index >= 0 && (arguments().size() == 1 || (statement.contains(" ? ") && statement.contains(" : ")))) {
			return new Replacement(statement.substring(statement.indexOf(JAVA.ASSIGNMENT)+1, statement.length()-JAVA.STATEMENT_TERMINATION.length()),
					arguments().get(index), ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION);
		}
		return null;
	}

	public Replacement makeReplacementForAssignedExpression(String statement) {
		if(expressionIsAssigned(statement)) {
			return new Replacement(statement.substring(statement.indexOf(JAVA.ASSIGNMENT)+1, statement.length()-JAVA.STATEMENT_TERMINATION.length()),
					expression, ReplacementType.EXPRESSION_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION);
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
		if(s1.contains(".<") && !s2.contains(".<")) {
			String s1WithoutGenerics = s1.substring(0, s1.indexOf(".<") + 1) + s1.substring(s1.indexOf(">") + 1, s1.length());
			if(s1WithoutGenerics.equals(s2))
				return true;
		}
		if(s2.contains(".<") && !s1.contains(".<")) {
			String s2WithoutGenerics = s2.substring(0, s2.indexOf(".<") + 1) + s2.substring(s2.indexOf(">") + 1, s2.length());
			if(s2WithoutGenerics.equals(s1))
				return true;
		}
		if(s1.contains(JAVA.METHOD_REFERENCE) && !s2.contains(JAVA.METHOD_REFERENCE)) {
			String methodReferenceName = s1.substring(s1.indexOf(JAVA.METHOD_REFERENCE) + 2, s1.length());
			if(s2.endsWith(methodReferenceName + "()")) {
				return true;
			}
		}
		if(s2.contains(JAVA.METHOD_REFERENCE) && !s1.contains(JAVA.METHOD_REFERENCE)) {
			String methodReferenceName = s2.substring(s2.indexOf(JAVA.METHOD_REFERENCE) + 2, s2.length());
			if(s1.endsWith(methodReferenceName + "()")) {
				return true;
			}
		}
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

	public boolean isAssertion() {
		return getName().startsWith("assert") || getName().equals("fail");
	}

	public enum StatementCoverageType {
		NONE, ONLY_CALL, RETURN_CALL, THROW_CALL, CAST_CALL, VARIABLE_DECLARATION_INITIALIZER_CALL;
	}
}
