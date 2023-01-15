package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiStatement;
import gr.uom.java.xmi.Formatter;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.CodeRange;

public class StatementObject extends AbstractStatement {
	
	private String statement;
	private LocationInfo locationInfo;
	private List<LeafExpression> variables;
	private List<String> types;
	private List<VariableDeclaration> variableDeclarations;
	private List<AbstractCall> methodInvocations;
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
	private List<LeafExpression> stringLiterals;
	private List<LeafExpression> numberLiterals;
	private List<LeafExpression> nullLiterals;
	private List<LeafExpression> booleanLiterals;
	private List<LeafExpression> typeLiterals;
	private List<AbstractCall> creations;
	private List<LeafExpression> infixExpressions;
	private List<String> infixOperators;
	private List<LeafExpression> arrayAccesses;
	private List<LeafExpression> prefixExpressions;
	private List<LeafExpression> postfixExpressions;
	private List<LeafExpression> thisExpressions;
	private List<LeafExpression> arguments;
	private List<LeafExpression> parenthesizedExpressions;
	private List<TernaryOperatorExpression> ternaryOperatorExpressions;
	private List<LambdaExpressionObject> lambdas;
	
	public StatementObject(PsiFile cu, String filePath, PsiStatement statement, int depth, CodeElementType codeElementType, VariableDeclarationContainer container) {
		super();
		this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
		Visitor visitor = new Visitor(cu, filePath, container);
		statement.accept(visitor);
		this.variables = visitor.getVariables();
		this.types = visitor.getTypes();
		this.variableDeclarations = visitor.getVariableDeclarations();
		this.methodInvocations = visitor.getMethodInvocations();
		this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
		this.stringLiterals = visitor.getStringLiterals();
		this.numberLiterals = visitor.getNumberLiterals();
		this.nullLiterals = visitor.getNullLiterals();
		this.booleanLiterals = visitor.getBooleanLiterals();
		this.typeLiterals = visitor.getTypeLiterals();
		this.creations = visitor.getCreations();
		this.infixExpressions = visitor.getInfixExpressions();
		this.infixOperators = visitor.getInfixOperators();
		this.arrayAccesses = visitor.getArrayAccesses();
		this.prefixExpressions = visitor.getPrefixExpressions();
		this.postfixExpressions = visitor.getPostfixExpressions();
		this.thisExpressions = visitor.getThisExpressions();
		this.arguments = visitor.getArguments();
		this.parenthesizedExpressions = visitor.getParenthesizedExpressions();
		this.ternaryOperatorExpressions = visitor.getTernaryOperatorExpressions();
		this.lambdas = visitor.getLambdas();
		setDepth(depth);
		this.statement = Formatter.format(statement);
	}

	@Override
	public List<AbstractCodeFragment> getLeaves() {
		List<AbstractCodeFragment> leaves = new ArrayList<AbstractCodeFragment>();
		leaves.add(this);
		return leaves;
	}

	public String toString() {
		return statement;
	}

	@Override
	public List<LeafExpression> getVariables() {
		return variables;
	}

	@Override
	public List<String> getTypes() {
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	@Override
	public List<AbstractCall> getMethodInvocations() {
		return methodInvocations;
	}

	@Override
	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	@Override
	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
	}

	@Override
	public List<LeafExpression> getNumberLiterals() {
		return numberLiterals;
	}

	@Override
	public List<LeafExpression> getNullLiterals() {
		return nullLiterals;
	}

	@Override
	public List<LeafExpression> getBooleanLiterals() {
		return booleanLiterals;
	}

	@Override
	public List<LeafExpression> getTypeLiterals() {
		return typeLiterals;
	}

	@Override
	public List<AbstractCall> getCreations() {
		return creations;
	}

	@Override
	public List<LeafExpression> getInfixExpressions() {
		return infixExpressions;
	}

	@Override
	public List<String> getInfixOperators() {
		return infixOperators;
	}

	@Override
	public List<LeafExpression> getArrayAccesses() {
		return arrayAccesses;
	}

	@Override
	public List<LeafExpression> getPrefixExpressions() {
		return prefixExpressions;
	}

	@Override
	public List<LeafExpression> getPostfixExpressions() {
		return postfixExpressions;
	}

	@Override
	public List<LeafExpression> getThisExpressions() {
		return thisExpressions;
	}

	@Override
	public List<LeafExpression> getArguments() {
		return arguments;
	}

	@Override
	public List<LeafExpression> getParenthesizedExpressions() {
		return parenthesizedExpressions;
	}

	@Override
	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	@Override
	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	@Override
	public int statementCount() {
		return 1;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		for(LambdaExpressionObject lambda : getLambdas()) {
			for(VariableDeclaration declaration : lambda.getParameters()) {
				if(declaration.getVariableName().equals(variableName)) {
					return declaration;
				}
			}
		}
		return null;
	}

	@Override
	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		return stringRepresentation;
	}
}
