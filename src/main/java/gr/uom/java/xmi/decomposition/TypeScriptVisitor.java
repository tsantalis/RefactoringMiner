package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.caoccao.javet.swc4j.ast.enums.Swc4jAstBinaryOp;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstArrowExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstBinExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstCallExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdent;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstRegex;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstStr;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstUsingDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDeclarator;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypeAnn;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitor;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitorResponse;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLClass;

public class TypeScriptVisitor extends Swc4jAstVisitor {
	private String sourceFolder;
	private String filePath;
	private Map<String, Set<VariableDeclaration>> activeVariableDeclarations; 
	private final String fileContent;
	private VariableDeclarationContainer container;
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
	private List<UMLClass> typeDeclarations = new ArrayList<>();

	public TypeScriptVisitor(String sourceFolder, String filePath, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.container = container;
		this.activeVariableDeclarations = activeVariableDeclarations;
		this.fileContent = fileContent;
	}

	public TypeScriptVisitor(String sourceFolder, String filePath, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, List<UMLClass> typeDeclarations) {
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.container = container;
		this.activeVariableDeclarations = activeVariableDeclarations;
		this.fileContent = fileContent;
		this.typeDeclarations = typeDeclarations;
	}

	public Swc4jAstVisitorResponse visitVarDeclarator(Swc4jAstVarDeclarator declarator) {
		List<Swc4jAstBindingIdent> identifiers = VariableDeclaration.extractVariables(declarator.getName());
		if(identifiers.size() == 1) {
			VariableDeclaration vd = new VariableDeclaration(sourceFolder, filePath, declarator, container, activeVariableDeclarations, fileContent, typeDeclarations);
			variableDeclarations.add(vd);
			LeafExpression name = new LeafExpression(sourceFolder, filePath, identifiers.get(0), CodeElementType.SIMPLE_NAME, container, fileContent);
			variables.add(name);
		}
		else {
			Swc4jAstTsTypeAnn typeAnnotation = VariableDeclaration.extractTypeAnnotation(declarator.getName());
			for(Swc4jAstBindingIdent identifier : identifiers) {
				VariableDeclaration vd = new VariableDeclaration(sourceFolder, filePath, typeAnnotation, identifier, container, activeVariableDeclarations, fileContent);
				variableDeclarations.add(vd);
				LeafExpression name = new LeafExpression(sourceFolder, filePath, identifier, CodeElementType.SIMPLE_NAME, container, fileContent);
				variables.add(name);
			}
		}
		return super.visitVarDeclarator(declarator);
	}

	public Swc4jAstVisitorResponse visitIdent(Swc4jAstIdent node) {
		String identifier = node.getSym();
		if(activeVariableDeclarations.containsKey(identifier)) {
			LeafExpression name = new LeafExpression(sourceFolder, filePath, node, CodeElementType.SIMPLE_NAME, container, fileContent);
			variables.add(name);
		}
		return super.visitIdent(node);
	}

	public Swc4jAstVisitorResponse visitArrowExpr(Swc4jAstArrowExpr node) {
		LambdaExpressionObject lambda = new LambdaExpressionObject(sourceFolder, filePath, node, container, activeVariableDeclarations, fileContent, typeDeclarations);
		lambdas.add(lambda);
		return super.visitArrowExpr(node);
	}

	public Swc4jAstVisitorResponse visitCallExpr(Swc4jAstCallExpr node) {
		OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, node, container, fileContent);
		methodInvocations.add(invocation);
		return super.visitCallExpr(node);
	}

	public Swc4jAstVisitorResponse visitBinExpr(Swc4jAstBinExpr node) {
		LeafExpression infix = new LeafExpression(sourceFolder, filePath, node, CodeElementType.INFIX_EXPRESSION, container, fileContent);
		infixExpressions.add(infix);
		Swc4jAstBinaryOp operator = node.getOp();
		infixOperators.add(operator.getName());
		return super.visitBinExpr(node);
	}

	public Swc4jAstVisitorResponse visitStr(Swc4jAstStr node) {
		LeafExpression literal = new LeafExpression(sourceFolder, filePath, node, CodeElementType.STRING_LITERAL, container, fileContent);
		stringLiterals.add(literal);
		return super.visitStr(node);
	}

	public Swc4jAstVisitorResponse visitRegex(Swc4jAstRegex node) {
		LeafExpression literal = new LeafExpression(sourceFolder, filePath, node, CodeElementType.STRING_LITERAL, container, fileContent);
		stringLiterals.add(literal);
		return super.visitRegex(node);
	}

	public Swc4jAstVisitorResponse visitUsingDecl(Swc4jAstUsingDecl node) {
		
		return super.visitUsingDecl(node);
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
