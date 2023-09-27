package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;

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
	public abstract List<String> getVariables();
	public abstract List<String> getTypes();
	public abstract List<VariableDeclaration> getVariableDeclarations();
	public abstract Map<String, List<AbstractCall>> getMethodInvocationMap();
	public abstract List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations();
	public abstract List<String> getStringLiterals();
	public abstract List<String> getNumberLiterals();
	public abstract List<String> getNullLiterals();
	public abstract List<String> getBooleanLiterals();
	public abstract List<String> getTypeLiterals();
	public abstract Map<String, List<ObjectCreation>> getCreationMap();
	public abstract List<String> getInfixExpressions();
	public abstract List<String> getInfixOperators();
	public abstract List<String> getArrayAccesses();
	public abstract List<String> getPrefixExpressions();
	public abstract List<String> getPostfixExpressions();
	public abstract List<String> getArguments();
	public abstract List<TernaryOperatorExpression> getTernaryOperatorExpressions();
	public abstract List<LambdaExpressionObject> getLambdas();
	public abstract VariableDeclaration searchVariableDeclaration(String variableName);
	public abstract VariableDeclaration getVariableDeclaration(String variableName);

	public boolean isKeyword() {
		String statement = getString();
		return statement.startsWith("return;") ||
				statement.startsWith("break;") ||
				statement.startsWith("continue;");
	}

	public boolean isLogCall() {
		AbstractCall call = invocationCoveringEntireFragment();
		if(call != null && call.isLog()) {
			return true;
		}
		return false;
	}

	public void replaceParametersWithArguments(Map<String, String> parameterToArgumentMap) {
		String afterReplacements = getString();
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
						if(previousChar.equals("(") || previousChar.equals(",") || previousChar.equals(" ") || previousChar.equals("=")) {
							isArgument = true;
						}
						String beforeMatch = afterReplacements.substring(0, start);
						String afterMatch = afterReplacements.substring(start+parameter.length(), afterReplacements.length());
						if(quoteBefore(beforeMatch) && quoteAfter(afterMatch)) {
							isInsideStringLiteral = true;
						}
					}
					else if(start == 0 && !afterReplacements.startsWith("return ")) {
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
					if(isArgument && !isInsideStringLiteral) {
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
		for(String infix : getInfixExpressions()) {
			if((infix + ";\n").equals(statement) || infix.equals(statement)) {
				return infix;
			}
			else if(("return " + infix + ";\n").equals(statement)) {
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
		Map<String, List<ObjectCreation>> creationMap = getCreationMap();
		String statement = getString();
		for(String objectCreation : creationMap.keySet()) {
			List<ObjectCreation> creations = creationMap.get(objectCreation);
			for(ObjectCreation creation : creations) {
				if((objectCreation + ";\n").equals(statement) || objectCreation.equals(statement)) {
					creation.coverage = StatementCoverageType.ONLY_CALL;
					return creation;
				}
				else if(("return " + objectCreation + ";\n").equals(statement)) {
					creation.coverage = StatementCoverageType.RETURN_CALL;
					return creation;
				}
				else if(("throw " + objectCreation + ";\n").equals(statement)) {
					creation.coverage = StatementCoverageType.THROW_CALL;
					return creation;
				}
				else if(expressionIsTheInitializerOfVariableDeclaration(objectCreation)) {
					creation.coverage = StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL;
					return creation;
				}
			}
		}
		return null;
	}

	public AbstractCall invocationCoveringEntireFragment() {
		Map<String, List<AbstractCall>> methodInvocationMap = getMethodInvocationMap();
		String statement = getString();
		for(String methodInvocation : methodInvocationMap.keySet()) {
			List<AbstractCall> invocations = methodInvocationMap.get(methodInvocation);
			for(AbstractCall invocation : invocations) {
				if((methodInvocation + ";\n").equals(statement) || methodInvocation.equals(statement) || ("!" + methodInvocation).equals(statement)) {
					invocation.coverage = StatementCoverageType.ONLY_CALL;
					return invocation;
				}
				else if(("return " + methodInvocation + ";\n").equals(statement)) {
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
		}
		return null;
	}

	public ObjectCreation assignmentCreationCoveringEntireStatement() {
		Map<String, List<ObjectCreation>> creationMap = getCreationMap();
		for(String objectCreation : creationMap.keySet()) {
			List<ObjectCreation> creations = creationMap.get(objectCreation);
			for(ObjectCreation creation : creations) {
				if(expressionIsTheRightHandSideOfAssignment(objectCreation)) {
					return creation;
				}
			}
		}
		return null;
	}

	public AbstractCall assignmentInvocationCoveringEntireStatement() {
		Map<String, List<AbstractCall>> methodInvocationMap = getMethodInvocationMap();
		for(String methodInvocation : methodInvocationMap.keySet()) {
			List<AbstractCall> invocations = methodInvocationMap.get(methodInvocation);
			for(AbstractCall invocation : invocations) {
				if(expressionIsTheRightHandSideOfAssignment(methodInvocation)) {
					return invocation;
				}
			}
		}
		return null;
	}

	public AbstractCall fieldAssignmentInvocationCoveringEntireStatement() {
		Map<String, List<AbstractCall>> methodInvocationMap = getMethodInvocationMap();
		for(String methodInvocation : methodInvocationMap.keySet()) {
			List<AbstractCall> invocations = methodInvocationMap.get(methodInvocation);
			for(AbstractCall invocation : invocations) {
				if(expressionIsTheRightHandSideOfAssignmentAndLeftHandSideIsField(methodInvocation)) {
					return invocation;
				}
			}
		}
		return null;
	}

	private boolean isCastExpressionCoveringEntireFragment(String expression) {
		String statement = getString();
		int index = -1;
		if(statement.endsWith(";\n")) {
			index = statement.indexOf(expression + ";\n");
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
					if(statement.endsWith(";\n") && ("return " + casting + expression + ";\n").equals(statement)) {
						return true;
					}
					if(!statement.endsWith(";\n") && (casting + expression).equals(statement)) {
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
			if(expressions.contains(initializer)) {
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
		if(statement.contains("=")) {
			List<String> variables = getVariables();
			if(variables.size() > 0) {
				String s = variables.get(0) + "=" + expression + ";\n";
				if(statement.equals(s)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean expressionIsTheRightHandSideOfAssignmentAndLeftHandSideIsField(String expression) {
		String statement = getString();
		if(statement.contains("=")) {
			List<String> variables = getVariables();
			if(variables.size() > 0) {
				String s = variables.get(0) + "=" + expression + ";\n";
				if(statement.equals(s) && variables.get(0).startsWith("this.")) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean throwsNewException() {
		return getString().startsWith("throw new ");
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
		return !statement.equals("{") && !statement.startsWith("catch(") && !statement.startsWith("case ") &&
				!statement.startsWith("return true;") && !statement.startsWith("return false;") && !statement.startsWith("return this;") && !statement.startsWith("return null;") && !statement.startsWith("return;");
	}
}
