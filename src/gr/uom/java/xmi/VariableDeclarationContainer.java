package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public interface VariableDeclarationContainer extends LocationInfoProvider {
	
	default List<VariableDeclaration> getAllVariableDeclarations() {
		OperationBody operationBody = getBody();
		if(operationBody != null) {
			List<VariableDeclaration> allVariableDeclarations = new ArrayList<VariableDeclaration>();
			allVariableDeclarations.addAll(this.getParameterDeclarationList());
			allVariableDeclarations.addAll(operationBody.getAllVariableDeclarations());
			return allVariableDeclarations;
		}
		return getParameterDeclarationList();
	}

	default List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		for(VariableDeclaration parameterDeclaration : getParameterDeclarationList()) {
			if(parameterDeclaration.getScope().subsumes(location)) {
				variableDeclarations.add(parameterDeclaration);
			}
		}
		OperationBody operationBody = getBody();
		if(operationBody != null) {
			variableDeclarations.addAll(operationBody.getVariableDeclarationsInScope(location));
		}
		return variableDeclarations;
	}

	List<VariableDeclaration> getParameterDeclarationList();
	List<String> getParameterNameList();
	OperationBody getBody();
	List<UMLAnonymousClass> getAnonymousClassList();
	List<LambdaExpressionObject> getAllLambdas();
	List<String> getAllVariables();
	String getClassName();
	String toQualifiedString();

	default CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		OperationBody operationBody = getBody();
		if(operationBody != null) {
			return operationBody.loopWithVariables(currentElementName, collectionName);
		}
		return null;
	}
}
