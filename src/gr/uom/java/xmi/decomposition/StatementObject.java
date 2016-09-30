package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class StatementObject extends AbstractStatement {
	
	private String statement;
	private List<String> variables;
	private List<VariableDeclaration> variableDeclarations;
	private Map<String, OperationInvocation> methodInvocationMap;
	
	public StatementObject(Statement statement, int depth) {
		super();
		SimpleNameVisitor visitor = new SimpleNameVisitor();
		statement.accept(visitor);
		this.variables = visitor.getVariables();
		this.variableDeclarations = visitor.getVariableDeclarations();
		this.methodInvocationMap = visitor.getMethodInvocationMap();
		setDepth(depth);
		if(statement.toString().matches("!(\\w|\\.)*@\\w*")) {
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
							sb.append(SimpleNameVisitor.processMethodInvocation(methodInvocation));
						}
						else if(initializer instanceof ClassInstanceCreation) {
							ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)initializer;
							sb.append(SimpleNameVisitor.processClassInstanceCreation(classInstanceCreation));
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
					sb.append(SimpleNameVisitor.processMethodInvocation(methodInvocation));
				}
				else if(expression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					sb.append(SimpleNameVisitor.processClassInstanceCreation(classInstanceCreation));
				}
				this.statement = sb.toString();
			}
			else if(statement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement)statement;
				StringBuilder sb = new StringBuilder();
				Expression expression = expressionStatement.getExpression();
				if(expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation)expression;
					sb.append(SimpleNameVisitor.processMethodInvocation(methodInvocation));
				}
				else if(expression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation)expression;
					sb.append(SimpleNameVisitor.processClassInstanceCreation(classInstanceCreation));
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
	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	@Override
	public Map<String, OperationInvocation> getMethodInvocationMap() {
		return methodInvocationMap;
	}
}