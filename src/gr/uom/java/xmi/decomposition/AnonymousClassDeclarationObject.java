package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.LocationInfoProvider;

public class AnonymousClassDeclarationObject implements LocationInfoProvider {
	private LocationInfo locationInfo;
	private AnonymousClassDeclaration astNode;
	private String astNodeString;
	private List<LeafExpression> variables = new ArrayList<>();
	private List<String> types = new ArrayList<>();
	private Map<String, List<AbstractCall>> methodInvocationMap = new LinkedHashMap<String, List<AbstractCall>>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<LeafExpression> stringLiterals = new ArrayList<>();
	private List<LeafExpression> numberLiterals = new ArrayList<>();
	private List<LeafExpression> nullLiterals = new ArrayList<>();
	private List<LeafExpression> booleanLiterals = new ArrayList<>();
	private List<LeafExpression> typeLiterals = new ArrayList<>();
	private Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<String, List<ObjectCreation>>();
	private List<LeafExpression> infixExpressions = new ArrayList<>();
	private List<String> infixOperators = new ArrayList<>();
	private List<LeafExpression> arrayAccesses = new ArrayList<>();
	private List<LeafExpression> prefixExpressions = new ArrayList<>();
	private List<LeafExpression> postfixExpressions = new ArrayList<>();
	private List<LeafExpression> thisExpressions = new ArrayList<>();
	private List<LeafExpression> arguments = new ArrayList<>();
	private List<LeafExpression> parenthesizedExpressions = new ArrayList<>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	
	public AnonymousClassDeclarationObject(CompilationUnit cu, String filePath, AnonymousClassDeclaration anonymous) {
		this.locationInfo = new LocationInfo(cu, filePath, anonymous, CodeElementType.ANONYMOUS_CLASS_DECLARATION);
		this.astNode = anonymous;
		this.astNodeString = stringify(anonymous);
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public AnonymousClassDeclaration getAstNode() {
		return astNode;
	}

	public void setAstNode(AnonymousClassDeclaration node) {
		this.astNode = node;
	}
	
	public String toString() {
		return astNodeString;
	}

	public Map<String, List<AbstractCall>> getMethodInvocationMap() {
		return this.methodInvocationMap;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
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

	public Map<String, List<ObjectCreation>> getCreationMap() {
		return creationMap;
	}

	public List<LeafExpression> getInfixExpressions() {
		return infixExpressions;
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
		return this.arguments;
	}

	public List<LeafExpression> getParenthesizedExpressions() {
		return parenthesizedExpressions;
	}

	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	public List<LeafExpression> getVariables() {
		return variables;
	}

	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
