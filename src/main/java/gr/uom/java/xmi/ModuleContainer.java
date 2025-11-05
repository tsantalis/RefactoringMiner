package gr.uom.java.xmi;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;

public class ModuleContainer implements VariableDeclarationContainer {
	private List<AbstractStatement> statementList;
	private LocationInfo locationInfo;
	private String name;
	private String className;

	public ModuleContainer(List<AbstractStatement> statements, LocationInfo locationInfo, String name) {
		this.statementList = statements;
		this.locationInfo = locationInfo;
		this.name = name;
		this.className = name;
	}

	public List<AbstractStatement> getStatementList() {
		return statementList;
	}

	public List<AbstractStatement> getAllStatements() {
		List<AbstractStatement> allStatements = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			allStatements.add(statement);
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				allStatements.addAll(composite.getAllStatements());
			}
		}
		return allStatements;
	}

	public List<CompositeStatementObject> getInnerNodes() {
		List<CompositeStatementObject> innerNodes = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				innerNodes.addAll(composite.getInnerNodes());
			}
		}
		return innerNodes;
	}

	public List<AbstractCodeFragment> getLeaves() {
		List<AbstractCodeFragment> leaves = new ArrayList<AbstractCodeFragment>();
		for(AbstractStatement statement : statementList) {
			leaves.addAll(statement.getLeaves());
		}
		return leaves;
	}

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@Override
	public OperationBody getBody() {
		return null;
	}

	@Override
	public List<UMLAnonymousClass> getAnonymousClassList() {
		return Collections.emptyList();
	}

	@Override
	public List<LambdaExpressionObject> getAllLambdas() {
		List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				lambdas.addAll(composite.getAllLambdas());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				lambdas.addAll(statementObject.getLambdas());
			}
		}
		return lambdas;
	}

	@Override
	public List<AbstractCall> getAllOperationInvocations() {
		List<AbstractCall> list = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				list.addAll(composite.getAllMethodInvocations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				list.addAll(statementObject.getMethodInvocations());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					if(lambda.getString().contains(JAVA.LAMBDA_ARROW)) {
						list.addAll(lambda.getAllOperationInvocations());
					}
				}
				for(AnonymousClassDeclarationObject anonymous : statementObject.getAnonymousClassDeclarations()) {
					list.addAll(anonymous.getMethodInvocations());
				}
			}
		}
		return list;
	}

	@Override
	public List<AbstractCall> getAllCreations() {
		List<AbstractCall> list = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				list.addAll(composite.getAllCreations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				list.addAll(statementObject.getCreations());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					list.addAll(lambda.getAllCreations());
				}
				for(AnonymousClassDeclarationObject anonymous : statementObject.getAnonymousClassDeclarations()) {
					list.addAll(anonymous.getCreations());
				}
			}
		}
		return list;
	}

	@Override
	public List<String> getAllVariables() {
		List<String> variables = new ArrayList<>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variables.addAll(composite.getAllVariables());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				for(LeafExpression variable : statementObject.getVariables()) {
					variables.add(variable.getString());
				}
			}
		}
		return variables;
	}

	@Override
	public List<UMLComment> getComments() {
		return Collections.emptyList();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getElementType() {
		return "module";
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getNonQualifiedClassName() {
		return className.contains(".") ? className.substring(className.lastIndexOf(".")+1, className.length()) : className;
	}

	public String toString() {
		return name;
	}

	@Override
	public String toQualifiedString() {
		return name;
	}

	@Override
	public Map<String, Set<VariableDeclaration>> variableDeclarationMap() {
		Map<String, Set<VariableDeclaration>> variableDeclarationMap = new LinkedHashMap<String, Set<VariableDeclaration>>();
		for(VariableDeclaration declaration : getAllVariableDeclarations()) {
			if(variableDeclarationMap.containsKey(declaration.getVariableName())) {
				variableDeclarationMap.get(declaration.getVariableName()).add(declaration);
			}
			else {
				Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
				variableDeclarations.add(declaration);
				variableDeclarationMap.put(declaration.getVariableName(), variableDeclarations);
			}
		}
		return variableDeclarationMap;
	}

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(VariableDeclaration variableDeclaration : getAllVariableDeclarations()) {
			if(variableDeclaration.getScope().subsumes(location)) {
				variableDeclarations.add(variableDeclaration);
			}
		}
		return variableDeclarations;
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variableDeclarations.addAll(composite.getAllVariableDeclarations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				variableDeclarations.addAll(statementObject.getVariableDeclarations());
				for(LambdaExpressionObject lambda : statementObject.getLambdas()) {
					variableDeclarations.addAll(lambda.getParameters());
					if(lambda.getBody() != null) {
						variableDeclarations.addAll(lambda.getBody().getAllVariableDeclarations());
					}
				}
			}
		}
		return variableDeclarations;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations();
		List<VariableDeclaration> matchingDeclarations = new ArrayList<>();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				matchingDeclarations.add(declaration);
			}
		}
		if(matchingDeclarations.size() > 0) {
			return matchingDeclarations.get(matchingDeclarations.size()-1);
		}
		return null;
	}

}
