package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private List<String> variables;
	private List<String> types;
	private List<VariableDeclaration> variableDeclarations;
	private Map<String, List<AbstractCall>> methodInvocationMap;
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
	private List<String> stringLiterals;
	private List<String> numberLiterals;
	private List<String> nullLiterals;
	private List<String> booleanLiterals;
	private List<String> typeLiterals;
	private Map<String, List<ObjectCreation>> creationMap;
	private List<String> infixExpressions;
	private List<String> infixOperators;
	private List<String> arrayAccesses;
	private List<String> prefixExpressions;
	private List<String> postfixExpressions;
	private List<String> arguments;
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
		this.methodInvocationMap = visitor.getMethodInvocationMap();
		this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
		this.stringLiterals = visitor.getStringLiterals();
		this.numberLiterals = visitor.getNumberLiterals();
		this.nullLiterals = visitor.getNullLiterals();
		this.booleanLiterals = visitor.getBooleanLiterals();
		this.typeLiterals = visitor.getTypeLiterals();
		this.creationMap = visitor.getCreationMap();
		this.infixExpressions = visitor.getInfixExpressions();
		this.infixOperators = visitor.getInfixOperators();
		this.arrayAccesses = visitor.getArrayAccesses();
		this.prefixExpressions = visitor.getPrefixExpressions();
		this.postfixExpressions = visitor.getPostfixExpressions();
		this.arguments = visitor.getArguments();
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
	public List<String> getVariables() {
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
	public Map<String, List<AbstractCall>> getMethodInvocationMap() {
		return methodInvocationMap;
	}

	@Override
	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	@Override
	public List<String> getStringLiterals() {
		return stringLiterals;
	}

	@Override
	public List<String> getNumberLiterals() {
		return numberLiterals;
	}

	@Override
	public List<String> getNullLiterals() {
		return nullLiterals;
	}

	@Override
	public List<String> getBooleanLiterals() {
		return booleanLiterals;
	}

	@Override
	public List<String> getTypeLiterals() {
		return typeLiterals;
	}

	@Override
	public Map<String, List<ObjectCreation>> getCreationMap() {
		return creationMap;
	}

	@Override
	public List<String> getInfixExpressions() {
		return infixExpressions;
	}

	@Override
	public List<String> getInfixOperators() {
		return infixOperators;
	}

	@Override
	public List<String> getArrayAccesses() {
		return arrayAccesses;
	}

	@Override
	public List<String> getPrefixExpressions() {
		return prefixExpressions;
	}

	@Override
	public List<String> getPostfixExpressions() {
		return postfixExpressions;
	}

	@Override
	public List<String> getArguments() {
		return arguments;
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
