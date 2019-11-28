package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class StatementObject extends AbstractStatement {
	
	private String statement;
	private LocationInfo locationInfo;
	private List<String> variables;
	private List<String> types;
	private List<VariableDeclaration> variableDeclarations;
	private Map<String, List<OperationInvocation>> methodInvocationMap;
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
	private List<String> stringLiterals;
	private List<String> numberLiterals;
	private List<String> nullLiterals;
	private List<String> booleanLiterals;
	private List<String> typeLiterals;
	private Map<String, List<ObjectCreation>> creationMap;
	private List<String> infixOperators;
	private List<String> arrayAccesses;
	private List<String> prefixExpressions;
	private List<String> postfixExpressions;
	private List<String> arguments;
	private List<TernaryOperatorExpression> ternaryOperatorExpressions;
	private List<LambdaExpressionObject> lambdas;
	
	public StatementObject(CompilationUnit cu, String filePath, Statement statement, int depth, CodeElementType codeElementType) {
		super();
		this.locationInfo = new LocationInfo(cu, filePath, statement, codeElementType);
		Visitor visitor = new Visitor(cu, filePath);
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
		this.infixOperators = visitor.getInfixOperators();
		this.arrayAccesses = visitor.getArrayAccesses();
		this.prefixExpressions = visitor.getPrefixExpressions();
		this.postfixExpressions = visitor.getPostfixExpressions();
		this.arguments = visitor.getArguments();
		this.ternaryOperatorExpressions = visitor.getTernaryOperatorExpressions();
		this.lambdas = visitor.getLambdas();
		setDepth(depth);
		if(Visitor.METHOD_INVOCATION_PATTERN.matcher(statement.toString()).matches()) {
			if(statement instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
				StringBuilder sb = new StringBuilder();
				List<IExtendedModifier> modifiers = variableDeclarationStatement.modifiers();
				for(IExtendedModifier modifier : modifiers) {
					sb.append(modifier.toString()).append(" ");
				}
				sb.append(variableDeclarationStatement.getType().toString());
				List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
				for(VariableDeclarationFragment fragment : fragments) {
					sb.append(fragment.getName().getIdentifier());
					Expression initializer = fragment.getInitializer();
					if(initializer != null) {
						sb.append(" = ");
						if(initializer instanceof MethodInvocation) {
							MethodInvocation methodInvocation = (MethodInvocation)initializer;
							sb.append(Visitor.processMethodInvocation(methodInvocation));
						}
						else if(initializer instanceof ClassInstanceCreation) {
							ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)initializer;
							sb.append(Visitor.processClassInstanceCreation(classInstanceCreation));
						}
					}
				}
				this.statement = sb.toString();
			}
			else if(statement instanceof ReturnStatement) {
				ReturnStatement returnStatement = (ReturnStatement)statement;
				StringBuilder sb = new StringBuilder();
				sb.append("return").append(" ");
				Expression expression = returnStatement.getExpression();
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					sb.append(Visitor.processMethodInvocation(methodInvocation));
				}
				else if(expression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					sb.append(Visitor.processClassInstanceCreation(classInstanceCreation));
				}
				this.statement = sb.toString();
			}
			else if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				StringBuilder sb = new StringBuilder();
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					sb.append(Visitor.processMethodInvocation(methodInvocation));
				}
				else if(expression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					sb.append(Visitor.processClassInstanceCreation(classInstanceCreation));
				}
				this.statement = sb.toString();
			}
			else {
				this.statement = statement.toString();
			}
		}
		else {
			this.statement = statement.toString();
		}
	}

	public List<String> stringRepresentation() {
		List<String> stringRepresentation = new ArrayList<String>();
		stringRepresentation.add(this.toString());
		return stringRepresentation;
	}

	@Override
	public List<StatementObject> getLeaves() {
		List<StatementObject> leaves = new ArrayList<StatementObject>();
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
	public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
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
		return null;
	}
}