package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstKeyValueProp;
import com.caoccao.javet.swc4j.ast.enums.Swc4jAstBinaryOp;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstArrowExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstAssignExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstBinExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstCallExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstCondExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstExprOrSpread;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdent;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstMemberExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstNewExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstParenExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstTpl;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstTsAsExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstUnaryExpr;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstBool;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstNull;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstNumber;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstObjectLit;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstRegex;
import com.caoccao.javet.swc4j.ast.expr.lit.Swc4jAstStr;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAst;
import com.caoccao.javet.swc4j.ast.miscs.Swc4jAstTplElement;
import com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstBlockStmt;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDeclarator;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypeAnn;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitor;
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitorResponse;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.UMLOperation;

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

	public Swc4jAstVisitorResponse visitMemberExpr(Swc4jAstMemberExpr node) {
		if(node.getParent() instanceof Swc4jAstMemberExpr) {
			ISwc4jAst parent = node.getParent().getParent();
			boolean keyValuePropParent = false;
			while(parent != null) {
				if(parent instanceof Swc4jAstKeyValueProp) {
					keyValuePropParent = true;
					break;
				}
				else if(parent instanceof Swc4jAstBlockStmt) {
					break;
				}
				parent = parent.getParent();
			}
			if(!keyValuePropParent) {
				LeafExpression name = new LeafExpression(sourceFolder, filePath, node, CodeElementType.QUALIFIED_NAME, container, fileContent);
				variables.add(name);
			}
		}
		return super.visitMemberExpr(node);
	}

	public Swc4jAstVisitorResponse visitIdent(Swc4jAstIdent node) {
		String identifier = node.getSym();
		if(activeVariableDeclarations.containsKey(identifier)) {
			LeafExpression name = new LeafExpression(sourceFolder, filePath, node, CodeElementType.SIMPLE_NAME, container, fileContent);
			variables.add(name);
		}
		else if(node.getParent() instanceof Swc4jAstExprOrSpread expr && expr.getParent() instanceof Swc4jAstCallExpr call && call.getArgs().contains(expr)) {
			//identifier is argument of call
			if(call.getCallee() instanceof Swc4jAstMemberExpr memberExpr) {
				String propName = memberExpr.getProp().toString();
				if(propName.equals("flatMap") || propName.equals("map") || propName.equals("filter") || propName.equals("collect") || propName.equals("reduce")) {
					// the identifier is a function name
					// Direct Method Passing (Point-Free Style)
					OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, node, container, fileContent);
					methodInvocations.add(invocation);
				}
			}
			else if(call.getCallee() instanceof Swc4jAstIdent ident) {
				String name = ident.getSym();
				if(container instanceof ModuleContainer module) {
					for(UMLImport imp : module.getNestedImports()) {
						if(imp.getName().endsWith(name)) {
							// the identifier is a function name
							// Direct Method Passing (Point-Free Style)
							OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, node, container, fileContent);
							methodInvocations.add(invocation);
							break;
						}
					}
				}
				else if(container instanceof UMLOperation operation) {
					for(UMLImport imp : operation.getNestedImports()) {
						if(imp.getName().endsWith(name)) {
							// the identifier is a function name
							// Direct Method Passing (Point-Free Style)
							OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, node, container, fileContent);
							methodInvocations.add(invocation);
							break;
						}
					}
				}
			}
		}
		else if(node.getParent() instanceof Swc4jAstKeyValueProp prop && prop.getValue().equals(node)) {
			String name = node.getSym();
			if(container instanceof ModuleContainer module) {
				for(UMLOperation op : module.getNestedOperations()) {
					if(op.getName().equals(name)) {
						// the identifier is a function name
						// Direct Method Passing (Point-Free Style)
						OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, node, container, fileContent);
						methodInvocations.add(invocation);
						break;
					}
				}
			}
			else if(container instanceof UMLOperation operation) {
				for(UMLOperation op : operation.getNestedOperations()) {
					if(op.getName().equals(name)) {
						// the identifier is a function name
						// Direct Method Passing (Point-Free Style)
						OperationInvocation invocation = new OperationInvocation(sourceFolder, filePath, node, container, fileContent);
						methodInvocations.add(invocation);
						break;
					}
				}
			}
		}
		return super.visitIdent(node);
	}

	public Swc4jAstVisitorResponse visitCondExpr(Swc4jAstCondExpr node) {
		TernaryOperatorExpression ternary = new TernaryOperatorExpression(sourceFolder, filePath, node, container, activeVariableDeclarations, fileContent, typeDeclarations);
		ternaryOperatorExpressions.add(ternary);
		return super.visitCondExpr(node);
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
		LeafExpression literal = new LeafExpression(sourceFolder, filePath, node, CodeElementType.REGEX, container, fileContent);
		stringLiterals.add(literal);
		return super.visitRegex(node);
	}

	public Swc4jAstVisitorResponse visitNull(Swc4jAstNull node) {
		LeafExpression literal = new LeafExpression(sourceFolder, filePath, node, CodeElementType.NULL_LITERAL, container, fileContent);
		nullLiterals.add(literal);
		return super.visitNull(node);
	}

	public Swc4jAstVisitorResponse visitBool(Swc4jAstBool node) {
		LeafExpression literal = new LeafExpression(sourceFolder, filePath, node, CodeElementType.BOOLEAN_LITERAL, container, fileContent);
		booleanLiterals.add(literal);
		return super.visitBool(node);
	}

	public Swc4jAstVisitorResponse visitNumber(Swc4jAstNumber node) {
		LeafExpression literal = new LeafExpression(sourceFolder, filePath, node, CodeElementType.NUMBER_LITERAL, container, fileContent);
		numberLiterals.add(literal);
		return super.visitNumber(node);
	}

	public Swc4jAstVisitorResponse visitParenExpr(Swc4jAstParenExpr node) {
		LeafExpression expression = new LeafExpression(sourceFolder, filePath, node, CodeElementType.PARENTHESIZED_EXPRESSION, container, fileContent);
		parenthesizedExpressions.add(expression);
		return super.visitParenExpr(node);
	}

	public Swc4jAstVisitorResponse visitUnaryExpr(Swc4jAstUnaryExpr node) {
		LeafExpression expression = new LeafExpression(sourceFolder, filePath, node, CodeElementType.PREFIX_EXPRESSION, container, fileContent);
		prefixExpressions.add(expression);
		return super.visitUnaryExpr(node);
	}

	public Swc4jAstVisitorResponse visitAssignExpr(Swc4jAstAssignExpr node) {
		LeafExpression expression = new LeafExpression(sourceFolder, filePath, node, CodeElementType.ASSIGNMENT, container, fileContent);
		assignments.add(expression);
		return super.visitAssignExpr(node);
	}

	public Swc4jAstVisitorResponse visitTsAsExpr(Swc4jAstTsAsExpr node) {
		LeafExpression expression = new LeafExpression(sourceFolder, filePath, node, CodeElementType.CAST_EXPRESSION, container, fileContent);
		castExpressions.add(expression);
		return super.visitTsAsExpr(node);
	}

	public Swc4jAstVisitorResponse visitNewExpr(Swc4jAstNewExpr node) {
		ObjectCreation creation = new ObjectCreation(sourceFolder, filePath, node, container, fileContent);
		creations.add(creation);
		return super.visitNewExpr(node);
	}

	public Swc4jAstVisitorResponse visitTpl(Swc4jAstTpl node) {
		//List<ISwc4jAstExpr> expressions = node.getExprs();
		List<Swc4jAstTplElement> quasis = node.getQuasis();
		for(Swc4jAstTplElement element : quasis) {
			LeafExpression literal = new LeafExpression(sourceFolder, filePath, element, CodeElementType.STRING_LITERAL, container, fileContent);
			stringLiterals.add(literal);
		}
		return super.visitTpl(node);
	}

	public Swc4jAstVisitorResponse visitObjectLit(Swc4jAstObjectLit node) {
		if(node.getProps().isEmpty()) {
			return super.visitObjectLit(node);
		}
		TypeScriptOperationBody.createAnonymousClass(node, sourceFolder, filePath, container, activeVariableDeclarations, fileContent, typeDeclarations);
		AnonymousClassDeclarationObject anonymousObject = new AnonymousClassDeclarationObject(sourceFolder, filePath, node, fileContent);
		anonymousClassDeclarations.add(anonymousObject);
		return super.visitObjectLit(node);
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
