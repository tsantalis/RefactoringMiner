package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.xmi.LocationInfo;

public class CompositeStatementObject extends AbstractStatement {

	private List<AbstractStatement> statementList;
	private List<AbstractExpression> expressionList;
	private String type;
	private LocationInfo locationInfo;

	public CompositeStatementObject(CompilationUnit cu, String filePath, Statement statement, int depth, String type) {
		super();
		this.type = type;
		this.setDepth(depth);
		this.locationInfo = new LocationInfo(cu, filePath, statement);
		this.statementList = new ArrayList<AbstractStatement>();
		this.expressionList = new ArrayList<AbstractExpression>();
	}

	public void addStatement(AbstractStatement statement) {
		statement.setIndex(statementList.size());
		statementList.add(statement);
		statement.setParent(this);
	}

	public List<AbstractStatement> getStatements() {
		return statementList;
	}

	public void addExpression(AbstractExpression expression) {
		//an expression has the same index and depth as the composite statement it belong to
		expression.setDepth(this.getDepth());
		expression.setIndex(this.getIndex());
		expressionList.add(expression);
		expression.setOwner(this);
	}

	public List<AbstractExpression> getExpressions() {
		return expressionList;
	}

	@Override
	public List<StatementObject> getLeaves() {
		List<StatementObject> leaves = new ArrayList<StatementObject>();
		for(AbstractStatement statement : statementList) {
			leaves.addAll(statement.getLeaves());
		}
		return leaves;
	}

	public List<CompositeStatementObject> getInnerNodes() {
		List<CompositeStatementObject> innerNodes = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				innerNodes.addAll(composite.getInnerNodes());
			}
		}
		innerNodes.add(this);
		return innerNodes;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		if(expressionList.size() > 0) {
			sb.append("(");
			for(int i=0; i<expressionList.size()-1; i++) {
				sb.append(expressionList.get(i).toString()).append("; ");
			}
			sb.append(expressionList.get(expressionList.size()-1).toString());
			sb.append(")");
		}
		return sb.toString();
	}

	@Override
	public List<String> getVariables() {
		List<String> variables = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			variables.addAll(expression.getVariables());
		}
		return variables;
	}

	@Override
	public List<String> getTypes() {
		List<String> types = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			types.addAll(expression.getTypes());
		}
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(AbstractExpression expression : expressionList) {
			variableDeclarations.addAll(expression.getVariableDeclarations());
		}
		return variableDeclarations;
	}

	@Override
	public Map<String, OperationInvocation> getMethodInvocationMap() {
		Map<String, OperationInvocation> map = new LinkedHashMap<String, OperationInvocation>();
		for(AbstractExpression expression : expressionList) {
			map.putAll(expression.getMethodInvocationMap());
		}
		return map;
	}

	@Override
	public List<String> getAnonymousClassDeclarations() {
		List<String> anonymousClassDeclarations = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			anonymousClassDeclarations.addAll(expression.getAnonymousClassDeclarations());
		}
		return anonymousClassDeclarations;
	}

	@Override
	public List<String> getStringLiterals() {
		List<String> stringLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			stringLiterals.addAll(expression.getStringLiterals());
		}
		return stringLiterals;
	}

	@Override
	public List<String> getInfixOperators() {
		List<String> infixOperators = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			infixOperators.addAll(expression.getInfixOperators());
		}
		return infixOperators;
	}

	@Override
	public Map<String, ObjectCreation> getCreationMap() {
		Map<String, ObjectCreation> creationMap = new LinkedHashMap<String, ObjectCreation>();
		for(AbstractExpression expression : expressionList) {
			creationMap.putAll(expression.getCreationMap());
		}
		return creationMap;
	}

	public Map<String, OperationInvocation> getAllMethodInvocations() {
		Map<String, OperationInvocation> map = new LinkedHashMap<String, OperationInvocation>();
		map.putAll(getMethodInvocationMap());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				map.putAll(composite.getAllMethodInvocations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				map.putAll(statementObject.getMethodInvocationMap());
			}
		}
		return map;
	}

	public List<String> getAllAnonymousClassDeclarations() {
		List<String> anonymousClassDeclarations = new ArrayList<String>();
		anonymousClassDeclarations.addAll(getAnonymousClassDeclarations());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				anonymousClassDeclarations.addAll(composite.getAllAnonymousClassDeclarations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				anonymousClassDeclarations.addAll(statementObject.getAnonymousClassDeclarations());
			}
		}
		return anonymousClassDeclarations;
	}

	@Override
	public int statementCount() {
		int count = 0;
		if(!this.getString().equals("{"))
			count++;
		for(AbstractStatement statement : statementList) {
			count += statement.statementCount();
		}
		return count;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}
}
