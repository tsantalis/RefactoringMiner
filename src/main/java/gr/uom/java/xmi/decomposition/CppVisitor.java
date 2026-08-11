package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambdaExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class CppVisitor extends ASTVisitor {
	private static final Pattern WITHOUT_INVOKER = Pattern.compile("(::)?\\b(?!if|while|for|switch|catch|return\\b)[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(");
	private static final Pattern WITH_INVOKER = Pattern.compile("(\\b[a-zA-Z_][a-zA-Z0-9_]*|::)(\\.|->|::)([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
	private String sourceFolder;
	private String filePath;
	private VariableDeclarationContainer container;
	private Map<String, Set<VariableDeclaration>> activeVariableDeclarations;
	private final String fileContent;
	
	private List<LeafExpression> variables = new ArrayList<>();
	private List<String> types = new ArrayList<>();
	private List<AbstractCall> methodInvocations = new ArrayList<>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<LeafExpression> textBlocks = new ArrayList<>();
	private List<LeafExpression> stringLiterals = new ArrayList<>();
	private List<LeafExpression> charLiterals = new ArrayList<>();
	private List<LeafExpression> numberLiterals = new ArrayList<>();
	private List<LeafExpression> nullLiterals = new ArrayList<>();
	private List<LeafExpression> booleanLiterals = new ArrayList<>();
	private List<LeafExpression> typeLiterals = new ArrayList<>();
	private List<AbstractCall> creations = new ArrayList<>();
	private List<LeafExpression> infixExpressions = new ArrayList<>();
	private List<LeafExpression> assignments = new ArrayList<>();
	private List<String> infixOperators = new ArrayList<>();
	private List<LeafExpression> arrayAccesses = new ArrayList<>();
	private List<LeafExpression> prefixExpressions = new ArrayList<>();
	private List<LeafExpression> postfixExpressions = new ArrayList<>();
	private List<LeafExpression> thisExpressions = new ArrayList<>();
	private List<LeafExpression> arguments = new ArrayList<>();
	private List<LeafExpression> parenthesizedExpressions = new ArrayList<>();
	private List<LeafExpression> castExpressions = new ArrayList<>();
	private List<LeafExpression> instanceofExpressions = new ArrayList<>();
	private List<LeafExpression> patternInstanceofExpressions = new ArrayList<>();
	private List<LeafExpression> tupleLiterals = new ArrayList<>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	private List<ComprehensionExpression> comprehensions = new ArrayList<ComprehensionExpression>();

	public CppVisitor(String sourceFolder, String filePath, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.container = container;
		this.activeVariableDeclarations = activeVariableDeclarations;
		this.fileContent = fileContent;
		this.shouldVisitDeclarations = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitExpressions = true;
		this.shouldVisitProblems = true;
		this.shouldVisitNames = true;
	}

	public int visit(IASTDeclaration declaration) {
		if(declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
			IASTDeclarator[] declarators = simpleDeclaration.getDeclarators();
			for(IASTDeclarator declarator : declarators) {
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, declarator, simpleDeclaration.getDeclSpecifier(), container, activeVariableDeclarations, fileContent);
				variableDeclarations.add(variableDeclaration);
			}
		}
		return super.visit(declaration);
	}

	public int visit(IASTName name) {
		LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, name, CodeElementType.SIMPLE_NAME, container, fileContent);
		variables.add(leafExpression);
		return super.visit(name);
	}

	private Map<String, OperationInvocation> extractAllMethodCalls(int startOffset, String input, Pattern pattern) {
		Map<String, OperationInvocation> results = new LinkedHashMap<>();
		if (input == null || input.isEmpty()) return results;
		Matcher matcher = pattern.matcher(input);

		int searchIdx = 0;
		while (matcher.find(searchIdx)) {
			int startOfCall = matcher.start();
			int openParenIdx = matcher.end() - 1; // Index of the opening '('
			String beforeParenthesis = input.substring(startOfCall, openParenIdx);

			// Find the true matching closing parenthesis
			int closeParenIdx = findMatchingParenthesis(input, openParenIdx);

			if (closeParenIdx != -1) {
				// Extract the full method call
				String fullCall = input.substring(startOfCall, closeParenIdx + 1);
				int start = startOffset + startOfCall;
				int end = startOffset + closeParenIdx + 1;
				int length = end - start;
				LocationInfo location = new LocationInfo(sourceFolder, filePath, start, length, end, CodeElementType.METHOD_INVOCATION, fileContent);
				String expression = null;
				String name = beforeParenthesis;
				if(beforeParenthesis.contains("::")) {
					expression = beforeParenthesis.substring(0, beforeParenthesis.lastIndexOf("::"));
					name = beforeParenthesis.substring(beforeParenthesis.lastIndexOf("::") + 2, beforeParenthesis.length());
				}
				else if(beforeParenthesis.contains("->")) {
					expression = beforeParenthesis.substring(0, beforeParenthesis.lastIndexOf("->"));
					name = beforeParenthesis.substring(beforeParenthesis.lastIndexOf("->") + 2, beforeParenthesis.length());
				}
				else if(beforeParenthesis.contains(".")) {
					expression = beforeParenthesis.substring(0, beforeParenthesis.lastIndexOf("."));
					name = beforeParenthesis.substring(beforeParenthesis.lastIndexOf(".") + 1, beforeParenthesis.length());
				}
				String arguments = input.substring(openParenIdx + 1, closeParenIdx);
				String[] args = arguments.length() > 0 ? arguments.split("\\s*,\\s*") : new String[] {};
				OperationInvocation invocation = new OperationInvocation(fullCall, location, container, name, expression, args);
				results.put(fullCall, invocation);
				// Advance search index just past the opening token to catch nested calls inside this one
				searchIdx = matcher.start() + 1;
			} else {
				searchIdx = matcher.end();
			}
		}
		return results;
	}

	private static int findMatchingParenthesis(String str, int openIdx) {
		int counter = 0;
		for (int i = openIdx; i < str.length(); i++) {
			if (str.charAt(i) == '(') {
				counter++;
			} else if (str.charAt(i) == ')') {
				counter--;
				if (counter == 0) {
					return i; // Found the matching closing parenthesis
				}
			}
		}
		return -1; // Unmatched parenthesis
	}

	public int visit(IASTStatement statement) {
		if(statement instanceof IASTProblemStatement problem) {
			IASTFileLocation fileLocation = problem.getFileLocation();
			int startOffset = fileLocation.getNodeOffset();
			Map<String, OperationInvocation> calls = extractAllMethodCalls(startOffset, problem.getRawSignature(), WITH_INVOKER);
			Map<String, OperationInvocation> callsWithout = extractAllMethodCalls(startOffset, problem.getRawSignature(), WITHOUT_INVOKER);
			Map<String, OperationInvocation> toAdd = new LinkedHashMap<>();
			for(String call : callsWithout.keySet()) {
				boolean found = false;
				for(String previous : calls.keySet()) {
					if(previous.contains(call)) {
						found = true;
						break;
					}
				}
				if(!found) {
					toAdd.put(call, callsWithout.get(call));
				}
			}
			methodInvocations.addAll(calls.values());
			methodInvocations.addAll(toAdd.values());
		}
		return super.visit(statement);
	}

	public int visit(IASTExpression expression) {
		if (expression instanceof IASTFunctionCallExpression functionCall) {
			OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, functionCall, container, fileContent);
			methodInvocations.add(invocation);
		}
		else if(expression instanceof IASTBinaryExpression binaryExpression) {
			if(binaryExpression.getOperator() == IASTBinaryExpression.op_assign) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, binaryExpression, CodeElementType.ASSIGNMENT, container, fileContent);
				assignments.add(leafExpression);
			}
			else {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, binaryExpression, CodeElementType.INFIX_EXPRESSION, container, fileContent);
				infixExpressions.add(leafExpression);
				infixOperators.add(getOperatorString(binaryExpression));
			}
		}
		else if(expression instanceof ICPPASTLambdaExpression lambdaExpression) {
			LambdaExpressionObject lambda = new LambdaExpressionObject(sourceFolder, filePath, lambdaExpression, container, activeVariableDeclarations, fileContent);
			lambdas.add(lambda);
		}
		else if(expression instanceof ICPPASTNewExpression newExpression) {
			ObjectCreation invocation = new ObjectCreation(sourceFolder, filePath, newExpression, container, fileContent);
			creations.add(invocation);
		}
		else if(expression instanceof IASTConditionalExpression conditionalExpression) {
			TernaryOperatorExpression ternary = new TernaryOperatorExpression(sourceFolder, filePath, conditionalExpression, container, activeVariableDeclarations, fileContent);
			ternaryOperatorExpressions.add(ternary);
		}
		else if(expression instanceof IASTLiteralExpression literal) {
			if(literal.getKind() == IASTLiteralExpression.lk_string_literal) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, literal, CodeElementType.STRING_LITERAL, container, fileContent);
				stringLiterals.add(leafExpression);
			}
			else if(literal.getKind() == IASTLiteralExpression.lk_integer_constant || literal.getKind() == IASTLiteralExpression.lk_float_constant) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, literal, CodeElementType.NUMBER_LITERAL, container, fileContent);
				numberLiterals.add(leafExpression);
			}
			else if(literal.getKind() == IASTLiteralExpression.lk_false || literal.getKind() == IASTLiteralExpression.lk_true) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, literal, CodeElementType.BOOLEAN_LITERAL, container, fileContent);
				booleanLiterals.add(leafExpression);
			}
			else if(literal.getKind() == IASTLiteralExpression.lk_char_constant) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, literal, CodeElementType.CHAR_LITERAL, container, fileContent);
				charLiterals.add(leafExpression);
			}
			else if(literal.getKind() == IASTLiteralExpression.lk_this) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, literal, CodeElementType.THIS_EXPRESSION, container, fileContent);
				thisExpressions.add(leafExpression);
			}
			else if(literal.getKind() == IASTLiteralExpression.lk_nullptr) {
				LeafExpression leafExpression = new LeafExpression(sourceFolder, filePath, literal, CodeElementType.NULL_LITERAL, container, fileContent);
				nullLiterals.add(leafExpression);
			}
		}
		return super.visit(expression);
	}

	public static String getOperatorString(IASTBinaryExpression binaryExpr) {
		switch (binaryExpr.getOperator()) {
			case IASTBinaryExpression.op_plus:               return "+";
			case IASTBinaryExpression.op_minus:              return "-";
			case IASTBinaryExpression.op_multiply:           return "*";
			case IASTBinaryExpression.op_divide:             return "/";
			case IASTBinaryExpression.op_modulo:             return "%";
			case IASTBinaryExpression.op_assign:             return "=";
			case IASTBinaryExpression.op_equals:             return "==";
			case IASTBinaryExpression.op_notequals:          return "!=";
			case IASTBinaryExpression.op_greaterThan:        return ">";
			case IASTBinaryExpression.op_greaterEqual:       return ">=";
			case IASTBinaryExpression.op_lessThan:           return "<";
			case IASTBinaryExpression.op_lessEqual:          return "<=";
			case IASTBinaryExpression.op_binaryAnd:          return "&";
			case IASTBinaryExpression.op_binaryAndAssign:    return "&=";
			case IASTBinaryExpression.op_binaryOr:           return "|";
			case IASTBinaryExpression.op_binaryOrAssign:     return "|=";
			case IASTBinaryExpression.op_binaryXor:          return "^";
			case IASTBinaryExpression.op_binaryXorAssign:    return "^=";
			case IASTBinaryExpression.op_logicalAnd:         return "&&";
			case IASTBinaryExpression.op_logicalOr:          return "||";
			case IASTBinaryExpression.op_shiftLeft:          return "<<";
			case IASTBinaryExpression.op_shiftRight:         return ">>";
			case IASTBinaryExpression.op_shiftLeftAssign:    return "<<=";
			case IASTBinaryExpression.op_shiftRightAssign:   return ">>=";
			case IASTBinaryExpression.op_plusAssign:         return "+=";
			case IASTBinaryExpression.op_minusAssign:        return "-=";
			case IASTBinaryExpression.op_multiplyAssign:     return "*=";
			case IASTBinaryExpression.op_divideAssign:       return "/=";
			case IASTBinaryExpression.op_moduloAssign:       return "%=";
			case IASTBinaryExpression.op_pmdot:              return ".**";
			case IASTBinaryExpression.op_pmarrow:            return "->*";
			case IASTBinaryExpression.op_ellipses:           return "...";
			case IASTBinaryExpression.op_max:                return ">?";
			case IASTBinaryExpression.op_min:                return ">?";
			default:                                         return "unknown";
		}
	}

	public List<LeafExpression> getVariables() {
		return variables;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<AbstractCall> getMethodInvocations() {
		return methodInvocations;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public List<LeafExpression> getTextBlocks() {
		return textBlocks;
	}

	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
	}

	public List<LeafExpression> getCharLiterals() {
		return charLiterals;
	}

	public List<LeafExpression> getNumberLiterals() {
		return numberLiterals;
	}

	public List<LeafExpression> getNullLiterals() {
		return nullLiterals;
	}

	public List<LeafExpression> getBooleanLiterals() {
		return booleanLiterals;
	}

	public List<LeafExpression> getTypeLiterals() {
		return typeLiterals;
	}

	public List<AbstractCall> getCreations() {
		return creations;
	}

	public List<LeafExpression> getInfixExpressions() {
		return infixExpressions;
	}

	public List<LeafExpression> getAssignments() {
		return assignments;
	}

	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public List<LeafExpression> getArrayAccesses() {
		return arrayAccesses;
	}

	public List<LeafExpression> getPrefixExpressions() {
		return prefixExpressions;
	}

	public List<LeafExpression> getPostfixExpressions() {
		return postfixExpressions;
	}

	public List<LeafExpression> getThisExpressions() {
		return thisExpressions;
	}

	public List<LeafExpression> getArguments() {
		return arguments;
	}

	public List<LeafExpression> getParenthesizedExpressions() {
		return parenthesizedExpressions;
	}

	public List<LeafExpression> getCastExpressions() {
		return castExpressions;
	}

	public List<LeafExpression> getInstanceofExpressions() {
		return instanceofExpressions;
	}

	public List<LeafExpression> getPatternInstanceofExpressions() {
		return patternInstanceofExpressions;
	}

	public List<LeafExpression> getTupleLiterals() {
		return tupleLiterals;
	}

	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	public List<ComprehensionExpression> getComprehensions() {
		return comprehensions;
	}
}
