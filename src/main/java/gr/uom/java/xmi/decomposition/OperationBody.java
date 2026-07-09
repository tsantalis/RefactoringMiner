package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;

public abstract class OperationBody {
	protected CompositeStatementObject compositeStatement;
	protected List<String> stringRepresentation;
	protected boolean containsAssertion;
	protected Map<String, Set<VariableDeclaration>> activeVariableDeclarations;
	protected VariableDeclarationContainer container;
	protected int bodyHashCode;
	protected List<UMLComment> comments;

	public Map<String, Set<VariableDeclaration>> getActiveVariableDeclarations() {
		return activeVariableDeclarations;
	}

	protected void addInActiveVariableDeclarations(VariableDeclaration v) {
		if(v == null)
			return;
		if(activeVariableDeclarations.containsKey(v.getVariableName())) {
			activeVariableDeclarations.get(v.getVariableName()).add(v);
		}
		else {
			Set<VariableDeclaration> set = new HashSet<VariableDeclaration>();
			set.add(v);
			activeVariableDeclarations.put(v.getVariableName(), set);
		}
	}

	private void removeFromActiveVariableDeclarations(VariableDeclaration v) {
		if(v == null)
			return;
		if(activeVariableDeclarations.containsKey(v.getVariableName())) {
			activeVariableDeclarations.get(v.getVariableName()).remove(v);
		}
	}

	protected void addAllInActiveVariableDeclarations(List<VariableDeclaration> variables) {
		for(VariableDeclaration v : variables) {
			addInActiveVariableDeclarations(v);
		}
	}

	protected void removeAllFromActiveVariableDeclarations(List<VariableDeclaration> variables) {
		for(VariableDeclaration v : variables) {
			removeFromActiveVariableDeclarations(v);
		}
	}

	public double builderStatementRatio() {
		List<AbstractStatement> fragments = compositeStatement.getStatements();
		int builderCount = 0;
		for(AbstractCodeFragment fragment : fragments) {
			AbstractCall invocation = fragment.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = fragment.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation instanceof OperationInvocation) {
				OperationInvocation inv = (OperationInvocation)invocation;
				if(inv.numberOfSubExpressions() > 3) {
					builderCount++;
				}
			}
		}
		//fragments.size() == 1 corresponds to a single-statement method
		if(fragments.size() > 1)
			return (double)builderCount/(double)fragments.size();
		return 0;
	}

	public int statementCount() {
		return compositeStatement.statementCount();
	}

	public int statementCountIncludingBlocks() {
		return compositeStatement.statementCountIncludingBlocks();
	}

	public CompositeStatementObject getCompositeStatement() {
		return compositeStatement;
	}

	public boolean containsAssertion() {
		return containsAssertion;
	}

	public List<CompositeStatementObject> getSynchronizedStatements() {
		List<CompositeStatementObject> synchronizedStatements = new ArrayList<CompositeStatementObject>();
		for(CompositeStatementObject innerNode : compositeStatement.getInnerNodes()) {
			if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
				synchronizedStatements.add(innerNode);
			}
		}
		return synchronizedStatements;
	}

	public List<AnonymousClassDeclarationObject> getAllAnonymousClassDeclarations() {
		return new ArrayList<AnonymousClassDeclarationObject>(compositeStatement.getAllAnonymousClassDeclarations());
	}

	public List<AbstractCall> getAllOperationInvocations() {
		return new ArrayList<AbstractCall>(compositeStatement.getAllMethodInvocations());
	}

	public List<AbstractCall> getAllCreations() {
		return new ArrayList<AbstractCall>(compositeStatement.getAllCreations());
	}

	public List<LeafExpression> getAllStringLiterals() {
		return new ArrayList<LeafExpression>(compositeStatement.getAllStringLiterals());
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		return new ArrayList<LambdaExpressionObject>(compositeStatement.getAllLambdas());
	}

	public List<String> getAllVariables() {
		return new ArrayList<String>(compositeStatement.getAllVariables());
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		return new ArrayList<VariableDeclaration>(compositeStatement.getAllVariableDeclarations());
	}

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		return new ArrayList<VariableDeclaration>(compositeStatement.getVariableDeclarationsInScope(location));
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		return compositeStatement.getVariableDeclaration(variableName);
	}

	protected void addStatementInVariableScopes(AbstractStatement statement) {
		for(String variableName : activeVariableDeclarations.keySet()) {
			Set<VariableDeclaration> variableDeclarations = activeVariableDeclarations.get(variableName);
			for(VariableDeclaration variableDeclaration : variableDeclarations) {
				boolean localVariableWithSameName = false;
				if(variableDeclaration.isAttribute() && variableDeclarations.size() > 1) {
					localVariableWithSameName = true;
				}
				variableDeclaration.addStatementInScope(statement, localVariableWithSameName);
				if(container != null) {
					for(AnonymousClassDeclarationObject anonymous : statement.getAnonymousClassDeclarations()) {
						UMLAnonymousClass anonymousClass = container.findAnonymousClass(anonymous);
						for(UMLOperation operation : anonymousClass.getOperations()) {
							if(operation.getBody() != null) {
								CompositeStatementObject composite = operation.getBody().getCompositeStatement();
								for(AbstractStatement anonymousStatement : composite.getInnerNodes()) {
									variableDeclaration.addStatementInScope(anonymousStatement, localVariableWithSameName);
								}
								for(AbstractCodeFragment anonymousStatement : composite.getLeaves()) {
									variableDeclaration.addStatementInScope(anonymousStatement, localVariableWithSameName);
								}
							}
						}
					}
					for(LambdaExpressionObject lambda : statement.getLambdas()) {
						OperationBody lambdaBody = lambda.getBody();
						if(lambdaBody != null) {
							CompositeStatementObject composite = lambdaBody.getCompositeStatement();
							for(AbstractStatement lambdaStatement : composite.getInnerNodes()) {
								variableDeclaration.addStatementInScope(lambdaStatement, localVariableWithSameName);
							}
							for(AbstractCodeFragment lambdaStatement : composite.getLeaves()) {
								variableDeclaration.addStatementInScope(lambdaStatement, localVariableWithSameName);
							}
						}
						AbstractExpression lambdaExpression = lambda.getExpression();
						if(lambdaExpression != null) {
							variableDeclaration.addStatementInScope(lambdaExpression, localVariableWithSameName);
						}
					}
				}
			}
		}
		if(container instanceof ModuleContainer module) {
			for(UMLClass nestedClass : module.getNestedClasses()) {
				if(statement.getLocationInfo().subsumes(nestedClass.getLocationInfo())) {
					nestedClass.setParentStatement(statement);
				}
			}
		}
		else if(container instanceof UMLOperation op) {
			for(UMLClass nestedClass : op.getNestedClasses()) {
				if(statement.getLocationInfo().subsumes(nestedClass.getLocationInfo())) {
					nestedClass.setParentStatement(statement);
				}
			}
		}
		for(UMLComment comment : comments) {
			if(comment.getLocationInfo().nextLine(statement.getLocationInfo())) {
				comment.addPreviousLocation(statement.getLocationInfo());
			}
		}
	}

	public Map<String, Set<String>> aliasedVariables() {
		return compositeStatement.aliasedVariables();
	}

	public Map<String, Set<String>> aliasedAttributes() {
		return compositeStatement.aliasedAttributes();
	}

	public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		return compositeStatement.loopWithVariables(currentElementName, collectionName);
	}

	public int getBodyHashCode() {
		return bodyHashCode;
	}

	public List<String> stringRepresentation() {
		if(stringRepresentation == null) {
			stringRepresentation = compositeStatement.stringRepresentation();
		}
		return stringRepresentation;
	}
}
