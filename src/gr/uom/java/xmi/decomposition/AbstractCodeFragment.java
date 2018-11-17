package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public abstract String getString();
	public abstract List<String> getVariables();
	public abstract List<String> getTypes();
	public abstract List<VariableDeclaration> getVariableDeclarations();
	public abstract Map<String, OperationInvocation> getMethodInvocationMap();
	public abstract List<String> getAnonymousClassDeclarations();
	public abstract List<String> getStringLiterals();
	public abstract List<String> getNumberLiterals();
	public abstract Map<String, ObjectCreation> getCreationMap();
	public abstract List<String> getInfixOperators();
	public abstract List<String> getArguments();
	public abstract VariableDeclaration searchVariableDeclaration(String variableName);
	public abstract VariableDeclaration getVariableDeclaration(String variableName);
	
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
					else if(start == 0) {
						isArgument = true;
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

	public ObjectCreation creationCoveringEntireFragment() {
		Map<String, ObjectCreation> creationMap = getCreationMap();
		String statement = getString();
		for(String objectCreation : creationMap.keySet()) {
			ObjectCreation creation = creationMap.get(objectCreation);
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
		return null;
	}

	public OperationInvocation invocationCoveringEntireFragment() {
		Map<String, OperationInvocation> methodInvocationMap = getMethodInvocationMap();
		String statement = getString();
		for(String methodInvocation : methodInvocationMap.keySet()) {
			OperationInvocation invocation = methodInvocationMap.get(methodInvocation);
			if((methodInvocation + ";\n").equals(statement) || methodInvocation.equals(statement)) {
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
		}
		return null;
	}

	private boolean isCastExpressionCoveringEntireFragment(String expression) {
		String statement = getString();
		int index = statement.indexOf(expression + ";\n");
		if(index != -1) {
			String prefix = statement.substring(0, index);
			if(prefix.contains("(") && prefix.contains(")")) {
				String casting = prefix.substring(prefix.indexOf("("), prefix.indexOf(")")+1);
				if(("return " + casting + expression + ";\n").equals(statement)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean expressionIsTheInitializerOfVariableDeclaration(String expression) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		if(variableDeclarations.size() == 1 && variableDeclarations.get(0).getInitializer() != null) {
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

	public boolean throwsNewException() {
		return getString().startsWith("throw new ");
	}
}
