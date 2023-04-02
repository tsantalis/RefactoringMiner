package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiFile;

import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.LocationInfoProvider;

public class AnonymousClassDeclarationObject implements LocationInfoProvider {
	private LocationInfo locationInfo;
	private PsiAnonymousClass astNode;
	private String astNodeString;
	private List<LeafExpression> variables = new ArrayList<>();
	private List<String> types = new ArrayList<>();
	private List<AbstractCall> methodInvocations = new ArrayList<>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<LeafExpression> stringLiterals = new ArrayList<>();
	private List<LeafExpression> numberLiterals = new ArrayList<>();
	private List<LeafExpression> nullLiterals = new ArrayList<>();
	private List<LeafExpression> booleanLiterals = new ArrayList<>();
	private List<LeafExpression> typeLiterals = new ArrayList<>();
	private List<AbstractCall> creations = new ArrayList<>();
	private List<LeafExpression> infixExpressions = new ArrayList<>();
	private List<String> infixOperators = new ArrayList<>();
	private List<LeafExpression> arrayAccesses = new ArrayList<>();
	private List<LeafExpression> prefixExpressions = new ArrayList<>();
	private List<LeafExpression> postfixExpressions = new ArrayList<>();
	private List<LeafExpression> thisExpressions = new ArrayList<>();
	private List<LeafExpression> arguments = new ArrayList<>();
	private List<LeafExpression> parenthesizedExpressions = new ArrayList<>();
	private List<LeafExpression> castExpressions = new ArrayList<>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	
	public AnonymousClassDeclarationObject(PsiFile cu, String filePath, PsiAnonymousClass anonymous) {
		this.locationInfo = new LocationInfo(cu, filePath, anonymous, CodeElementType.ANONYMOUS_CLASS_DECLARATION);
		this.astNode = anonymous;
		String stringIncludingClassInstantiation = Formatter.format(anonymous);
		int index = stringIncludingClassInstantiation.indexOf("){\n");
		if(index != -1) {
			this.astNodeString = stringIncludingClassInstantiation.substring(index + 1);
		}
		else {
			this.astNodeString = stringIncludingClassInstantiation;
		}
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public PsiAnonymousClass getAstNode() {
		return astNode;
	}

	public void setAstNode(PsiAnonymousClass node) {
		this.astNode = node;
	}
	
	public String toString() {
		return astNodeString;
	}

	public void clearAll() {
		this.variables.clear();
		this.types.clear();
		this.methodInvocations.clear();
		this.creations.clear();
		this.variableDeclarations.clear();
		this.stringLiterals.clear();
		this.nullLiterals.clear();
		this.booleanLiterals.clear();
		this.typeLiterals.clear();
		this.numberLiterals.clear();
		this.infixExpressions.clear();
		this.infixOperators.clear();
		this.postfixExpressions.clear();
		this.prefixExpressions.clear();
		this.getThisExpressions().clear();
		this.parenthesizedExpressions.clear();
		this.arguments.clear();
		this.ternaryOperatorExpressions.clear();
		this.anonymousClassDeclarations.clear();
		this.lambdas.clear();
		this.arrayAccesses.clear();
	}

	public List<AbstractCall> getMethodInvocations() {
		return methodInvocations;
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

	public List<AbstractCall> getCreations() {
		return creations;
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

	public List<LeafExpression> getCastExpressions() {
		return castExpressions;
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
