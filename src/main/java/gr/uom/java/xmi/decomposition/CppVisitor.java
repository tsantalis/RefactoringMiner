package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambdaExpression;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class CppVisitor extends ASTVisitor {
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
