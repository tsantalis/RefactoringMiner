package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;

public abstract class AbstractCodeFragment implements LocationInfoProvider {
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
	
	public abstract CompositeStatementObject getParent();
	public abstract String getString();
	public abstract List<LeafExpression> getVariables();
	public abstract List<String> getTypes();
	public abstract List<VariableDeclaration> getVariableDeclarations();
	public abstract List<AbstractCall> getMethodInvocations();
	public abstract List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations();
	public abstract List<LeafExpression> getTextBlocks();
	public abstract List<LeafExpression> getStringLiterals();
	public abstract List<LeafExpression> getCharLiterals();
	public abstract List<LeafExpression> getNumberLiterals();
	public abstract List<LeafExpression> getNullLiterals();
	public abstract List<LeafExpression> getBooleanLiterals();
	public abstract List<LeafExpression> getTypeLiterals();
	public abstract List<AbstractCall> getCreations();
	public abstract List<LeafExpression> getInfixExpressions();
	public abstract List<LeafExpression> getAssignments();
	public abstract List<String> getInfixOperators();
	public abstract List<LeafExpression> getArrayAccesses();
	public abstract List<LeafExpression> getPrefixExpressions();
	public abstract List<LeafExpression> getPostfixExpressions();
	public abstract List<LeafExpression> getThisExpressions();
	public abstract List<LeafExpression> getArguments();
	public abstract List<LeafExpression> getParenthesizedExpressions();
	public abstract List<LeafExpression> getCastExpressions();
	public abstract List<TernaryOperatorExpression> getTernaryOperatorExpressions();
	public abstract List<LambdaExpressionObject> getLambdas();
	public abstract VariableDeclaration searchVariableDeclaration(String variableName);
	public abstract VariableDeclaration getVariableDeclaration(String variableName);

	public List<LeafExpression> findExpression(String s) {
		Set<LocationInfo> locations = new HashSet<>();
		List<LeafExpression> matchingExpressions = new ArrayList<>();
		for(LeafExpression expression : getVariables()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(AbstractCall expression : getMethodInvocations()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression.asLeafExpression());
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getStringLiterals()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getNumberLiterals()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getNullLiterals()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getBooleanLiterals()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getTypeLiterals()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(AbstractCall expression : getCreations()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression.asLeafExpression());
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getInfixExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getAssignments()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getArrayAccesses()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getPrefixExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getPostfixExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getThisExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getCastExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(LeafExpression expression : getParenthesizedExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
				locations.add(expression.getLocationInfo());
			}
		}
		for(TernaryOperatorExpression expression : getTernaryOperatorExpressions()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression.asLeafExpression());
				locations.add(expression.getLocationInfo());
			}
		}
		for(LambdaExpressionObject expression : getLambdas()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression.asLeafExpression());
				locations.add(expression.getLocationInfo());
			}
			if(expression.getExpression() != null) {
				matchingExpressions.addAll(expression.getExpression().findExpression(s));
			}
		}
		for(LeafExpression expression : getArguments()) {
			if(expression.getString().equals(s)) {
				if(!locations.contains(expression.getLocationInfo()))
					matchingExpressions.add(expression);
			}
		}
		return matchingExpressions;
	}

	public boolean isKeyword() {
		String statement = getString();
		return statement.equals(JAVA.RETURN_STATEMENT) ||
				statement.equals(JAVA.BREAK_STATEMENT) ||
				statement.equals(JAVA.CONTINUE_STATEMENT);
	}

	public boolean isLogCall() {
		AbstractCall call = invocationCoveringEntireFragment();
		if(call != null && call.isLog()) {
			return true;
		}
		return false;
	}

	public boolean isAssertCall() {
		AbstractCall call = invocationCoveringEntireFragment();
		if(call != null && call.getName().startsWith("assert")) {
			return true;
		}
		return false;
	}

	public void replaceParametersWithArguments(Map<String, String> parameterToArgumentMap) {
		String afterReplacements = getString();
		Set<String> anonymousClassMethodDeclarationLines = new LinkedHashSet<>();
		if(StringBasedHeuristics.containsMethodSignatureOfAnonymousClass(afterReplacements)) {
			String[] lines = afterReplacements.split("\\n");
			for(String line : lines) {
				line = VariableReplacementAnalysis.prepareLine(line);
				if(Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches()) {
					anonymousClassMethodDeclarationLines.add(line);
				}
			}
		}
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(!parameter.equals(argument)) {
				StringBuffer sb = new StringBuffer();
				Pattern p = Pattern.compile(Pattern.quote(parameter));
				Matcher m = p.matcher(afterReplacements);
				while(m.find()) {
					//check if the matched string is an argument
					//previous character should be "(" or "," or " " or there is no previous character
					int start = m.start();
					boolean isArgument = false;
					boolean isInsideStringLiteral = false;
					if(start >= 1) {
						String previousChar = afterReplacements.substring(start-1, start);
						if(previousChar.equals("(") || previousChar.equals(",") || previousChar.equals(" ") || previousChar.equals(JAVA.ASSIGNMENT)) {
							int indexOfNextChar = start + parameter.length();
							if(afterReplacements.length() > indexOfNextChar) {
								char nextChar = afterReplacements.charAt(indexOfNextChar);
								if(!Character.isLetterOrDigit(nextChar)) {
									isArgument = true;
								}
							}
							if(parameter.endsWith(".")) {
								isArgument = true;
							}
						}
						String beforeMatch = afterReplacements.substring(0, start);
						String afterMatch = afterReplacements.substring(start+parameter.length(), afterReplacements.length());
						if(quoteBefore(beforeMatch) && quoteAfter(afterMatch)) {
							isInsideStringLiteral = true;
						}
					}
					else if(start == 0 && !afterReplacements.startsWith(JAVA.RETURN_SPACE)) {
						int indexOfNextChar = start + parameter.length();
						if(afterReplacements.length() > indexOfNextChar) {
							char nextChar = afterReplacements.charAt(indexOfNextChar);
							if(!Character.isLetterOrDigit(nextChar)) {
								isArgument = true;
							}
						}
						if(parameter.endsWith(".")) {
							isArgument = true;
						}
					}
					boolean isWithinMethodSignature = false;
					for(String methodSignatureLine : anonymousClassMethodDeclarationLines) {
						int methodSignatureStart = getString().indexOf(methodSignatureLine);
						if(methodSignatureStart != -1 && methodSignatureStart < start && methodSignatureStart + methodSignatureLine.length() > start) {
							isWithinMethodSignature = true;
							break;
						}
					}
					if(isArgument && !isInsideStringLiteral && !isWithinMethodSignature) {
						m.appendReplacement(sb, Matcher.quoteReplacement(argument));
					}
				}
				m.appendTail(sb);
				afterReplacements = sb.toString();
			}
		}
		this.codeFragmentAfterReplacingParametersWithArguments = afterReplacements;
	}

	private static boolean quoteBefore(String beforeMatch) {
		if(beforeMatch.contains("\"")) {
			if(beforeMatch.contains("+")) {
				int indexOfQuote = beforeMatch.lastIndexOf("\"");
				int indexOfPlus = beforeMatch.lastIndexOf("+");
				if(indexOfPlus > indexOfQuote) {
					return false;
				}
				else {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	private static boolean quoteAfter(String afterMatch) {
		if(afterMatch.contains("\"")) {
			if(afterMatch.contains("+")) {
				int indexOfQuote = afterMatch.indexOf("\"");
				int indexOfPlus = afterMatch.indexOf("+");
				if(indexOfPlus < indexOfQuote) {
					return false;
				}
				else {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	public boolean equalFragment(AbstractCodeFragment other) {
		if(this.getString().equals(other.getString())) {
			return true;
		}
		else if(this.getString().contains(other.getString())) {
			return true;
		}
		else if(other.getString().contains(this.getString())) {
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

	public void resetArgumentization() {
		this.codeFragmentAfterReplacingParametersWithArguments = getString();
	}

	public String infixExpressionCoveringTheEntireFragment() {
		String statement = getString();
		for(LeafExpression infixExpression : getInfixExpressions()) {
			String infix = infixExpression.getString();
			if((infix + JAVA.STATEMENT_TERMINATION).equals(statement) || infix.equals(statement)) {
				return infix;
			}
			else if((JAVA.RETURN_SPACE + infix + JAVA.STATEMENT_TERMINATION).equals(statement)) {
				return infix;
			}
			else if(expressionIsTheInitializerOfVariableDeclaration(infix)) {
				return infix;
			}
			else if(("if(" + infix + ")").equals(statement)) {
				return infix;
			}
			else if(("while(" + infix + ")").equals(statement)) {
				return infix;
			}
		}
		return null;
	}

	public ObjectCreation creationCoveringEntireFragment() {
		String statement = getString();
		for(AbstractCall creation : getCreations()) {
			String objectCreation = creation.getString();
			if((objectCreation + JAVA.STATEMENT_TERMINATION).equals(statement) || objectCreation.equals(statement)) {
				creation.coverage = StatementCoverageType.ONLY_CALL;
				return (ObjectCreation) creation;
			}
			else if((JAVA.RETURN_SPACE + objectCreation + JAVA.STATEMENT_TERMINATION).equals(statement)) {
				creation.coverage = StatementCoverageType.RETURN_CALL;
				return (ObjectCreation) creation;
			}
			else if((JAVA.THROW_SPACE + objectCreation + JAVA.STATEMENT_TERMINATION).equals(statement)) {
				creation.coverage = StatementCoverageType.THROW_CALL;
				return (ObjectCreation) creation;
			}
			else if(expressionIsTheInitializerOfVariableDeclaration(objectCreation)) {
				creation.coverage = StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL;
				return (ObjectCreation) creation;
			}
		}
		return null;
	}

	public AbstractCall invocationCoveringEntireFragment() {
		String statement = getString();
		for(AbstractCall invocation : getMethodInvocations()) {
			String methodInvocation = invocation.getString();
			if((methodInvocation + JAVA.STATEMENT_TERMINATION).equals(statement) || methodInvocation.equals(statement) || ("!" + methodInvocation).equals(statement)) {
				invocation.coverage = StatementCoverageType.ONLY_CALL;
				return invocation;
			}
			else if((JAVA.RETURN_SPACE + methodInvocation + JAVA.STATEMENT_TERMINATION).equals(statement)) {
				invocation.coverage = StatementCoverageType.RETURN_CALL;
				return invocation;
			}
			else if(isCastExpressionCoveringEntireFragment(methodInvocation)) {
				invocation.coverage = StatementCoverageType.CAST_CALL;
				return invocation;
			}
			else if(expressionIsTheInitializerOfVariableDeclaration(methodInvocation)) {
				invocation.coverage = StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL;
				return invocation;
			}
			else if(invocation.getLocationInfo().getCodeElementType().equals(CodeElementType.SUPER_CONSTRUCTOR_INVOCATION) ||
					invocation.getLocationInfo().getCodeElementType().equals(CodeElementType.CONSTRUCTOR_INVOCATION)) {
				invocation.coverage = StatementCoverageType.ONLY_CALL;
				return invocation;
			}
		}
		return null;
	}

	public ObjectCreation assignmentCreationCoveringEntireStatement() {
		for(AbstractCall creation : getCreations()) {
			if(expressionIsTheRightHandSideOfAssignment(creation.getString())) {
				return (ObjectCreation) creation;
			}
		}
		return null;
	}

	public AbstractCall assignmentInvocationCoveringEntireStatement() {
		for(AbstractCall invocation : getMethodInvocations()) {
			if(expressionIsTheRightHandSideOfAssignment(invocation.getString())) {
				return invocation;
			}
		}
		return null;
	}

	public AbstractCall fieldAssignmentInvocationCoveringEntireStatement(UMLAbstractClassDiff classDiff) {
		for(AbstractCall invocation : getMethodInvocations()) {
			if(expressionIsTheRightHandSideOfAssignmentAndLeftHandSideIsField(invocation.getString(), classDiff)) {
				return invocation;
			}
			for(AbstractCall creation : getCreations()) {
				if(creation.arguments().contains(invocation.actualString())) {
					if(expressionIsTheRightHandSideOfAssignmentAndLeftHandSideIsField(creation.getString(), classDiff)) {
						return invocation;
					}
				}
			}
		}
		return null;
	}

	private boolean isCastExpressionCoveringEntireFragment(String expression) {
		String statement = getString();
		int index = -1;
		if(statement.endsWith(JAVA.STATEMENT_TERMINATION)) {
			index = statement.indexOf(expression + JAVA.STATEMENT_TERMINATION);
		}
		else {
			index = statement.indexOf(expression);
		}
		if(index != -1) {
			String prefix = statement.substring(0, index);
			if(prefix.contains("(") && prefix.contains(")")) {
				int indexOfOpeningParenthesis = prefix.indexOf("(");
				int indexOfClosingParenthesis = prefix.indexOf(")");
				boolean openingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(prefix, indexOfOpeningParenthesis);
				boolean closingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(prefix, indexOfClosingParenthesis);
				boolean openingParenthesisInsideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(prefix, indexOfOpeningParenthesis);
				boolean closingParenthesisIndideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(prefix, indexOfClosingParenthesis);
				if(indexOfOpeningParenthesis < indexOfClosingParenthesis &&
						!openingParenthesisInsideSingleQuotes && !closingParenthesisInsideSingleQuotes &&
						!openingParenthesisInsideDoubleQuotes && !closingParenthesisIndideDoubleQuotes) {
					String casting = prefix.substring(indexOfOpeningParenthesis, indexOfClosingParenthesis+1);
					if(statement.endsWith(JAVA.STATEMENT_TERMINATION) && (JAVA.RETURN_SPACE + casting + expression + JAVA.STATEMENT_TERMINATION).equals(statement)) {
						return true;
					}
					if(!statement.endsWith(JAVA.STATEMENT_TERMINATION) && (casting + expression).equals(statement)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean containsInitializerOfVariableDeclaration(Set<String> expressions) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		if(variableDeclarations.size() == 1 && variableDeclarations.get(0).getInitializer() != null) {
			String initializer = variableDeclarations.get(0).getInitializer().toString();
			if(expressions.contains(initializer) && !getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT)) {
				return true;
			}
		}
		return false;
	}

	private boolean expressionIsTheInitializerOfVariableDeclaration(String expression) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		if(variableDeclarations.size() > 0 && variableDeclarations.get(0).getInitializer() != null) {
			String initializer = variableDeclarations.get(0).getInitializer().toString();
			if(initializer.equals(expression))
				return true;
			if(initializer.startsWith("(")) {
				//ignore casting
				String initializerWithoutCasting = initializer.substring(initializer.indexOf(")")+1,initializer.length());
				if(initializerWithoutCasting.equals(expression))
					return true;
			}
		}
		return false;
	}

	private boolean expressionIsTheRightHandSideOfAssignment(String expression) {
		String statement = getString();
		if(statement.contains(JAVA.ASSIGNMENT)) {
			List<LeafExpression> variables = getVariables();
			if(variables.size() > 0) {
				String s = variables.get(0).getString() + JAVA.ASSIGNMENT + expression + JAVA.STATEMENT_TERMINATION;
				if(statement.equals(s)) {
					return true;
				}
				if(statement.startsWith(variables.get(0).getString() + JAVA.ASSIGNMENT)) {
					String suffix = statement.substring(statement.indexOf(JAVA.ASSIGNMENT) + 1);
					if(suffix.endsWith(expression + JAVA.STATEMENT_TERMINATION)) {
						int index = suffix.indexOf(expression + JAVA.STATEMENT_TERMINATION);
						String prefix = suffix.substring(0, index);
						if(prefix.startsWith("(") && prefix.endsWith(")")) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean expressionIsTheRightHandSideOfAssignmentAndLeftHandSideIsField(String expression, UMLAbstractClassDiff classDiff) {
		String statement = getString();
		if(statement.contains(JAVA.ASSIGNMENT)) {
			List<LeafExpression> variables = getVariables();
			if(variables.size() > 0) {
				String variable = variables.get(0).getString();
				String s = variable + JAVA.ASSIGNMENT + expression + JAVA.STATEMENT_TERMINATION;
				if(statement.equals(s) && (variable.startsWith(JAVA.THIS_DOT) || classDiff.getOriginalClass().getFieldDeclarationMap().containsKey(variable) ||
						classDiff.getNextClass().getFieldDeclarationMap().containsKey(variable))) {
					return true;
				}
				String beforeAssignment = statement.substring(0, statement.indexOf(JAVA.ASSIGNMENT));
				if(variables.size() >= 2) {
					if(beforeAssignment.equals(variable + "." + variables.get(1).getString())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean throwsNewException() {
		return getString().startsWith(JAVA.THROW_SPACE + "new ");
	}

	public boolean isLastStatementWithReturn() {
		if(isLastStatement()) {
			return getString().startsWith(JAVA.RETURN_SPACE);
		}
		return false;
	}

	public boolean isLastStatementWithBooleanReturn() {
		if(isLastStatement()) {
			return getString().equals(JAVA.RETURN_TRUE) || getString().equals(JAVA.RETURN_FALSE);
		}
		return false;
	}

	public boolean isLastStatement() {
		if(this instanceof AbstractExpression) {
			return false;
		}
		CompositeStatementObject parent = getParent();
		if(parent != null && parent.getParent() == null) {
			return index == parent.getStatements().size() - 1;
		}
		return false;
	}

	public boolean isLastStatementInParentBlock() {
		if(this instanceof AbstractExpression) {
			return false;
		}
		CompositeStatementObject parent = getParent();
		if(parent != null && parent.getParent() != null) {
			return index == parent.getStatements().size() - 1;
		}
		return false;
	}

	public boolean countableStatement() {
		String statement = getString();
		//covers the cases of lambda expressions having an expression as their body
		if(this instanceof AbstractExpression) {
			return true;
		}
		//covers the cases of methods with only one statement in their body
		if(this instanceof AbstractStatement && ((AbstractStatement)this).getParent() != null &&
				((AbstractStatement)this).getParent().statementCount() == 1 && ((AbstractStatement)this).getParent().getParent() == null) {
			return true;
		}
		return !statement.equals(JAVA.OPEN_BLOCK) && !statement.startsWith("catch(") && !statement.startsWith(JAVA.CASE_SPACE) &&
				!statement.equals(JAVA.RETURN_TRUE) && !statement.equals(JAVA.RETURN_FALSE) && !statement.equals(JAVA.RETURN_THIS) && !statement.equals(JAVA.RETURN_NULL) && !statement.equals(JAVA.RETURN_STATEMENT);
	}
}
