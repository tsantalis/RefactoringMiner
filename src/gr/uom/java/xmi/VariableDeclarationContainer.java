package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
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

	default VariableDeclaration getVariableDeclaration(String variableName) {
		OperationBody operationBody = getBody();
		if(operationBody != null) {
			VariableDeclaration variableDeclatation = operationBody.getVariableDeclaration(variableName);
			if(variableDeclatation != null) {
				return variableDeclatation;
			}
		}
		for(VariableDeclaration parameterDeclaration : getParameterDeclarationList()) {
			if(parameterDeclaration.getVariableName().equals(variableName)) {
				return parameterDeclaration;
			}
		}
		return null;
	}

	List<VariableDeclaration> getParameterDeclarationList();
	List<UMLType> getParameterTypeList();
	List<String> getParameterNameList();
	List<UMLParameter> getParametersWithoutReturnType();

	default boolean equalReturnParameter(VariableDeclarationContainer operation) {
		if(this instanceof UMLOperation && operation instanceof UMLOperation) {
			return ((UMLOperation)this).equalReturnParameter((UMLOperation)operation);
		}
		return false;
	}

	int getNumberOfNonVarargsParameters();
	boolean hasVarargsParameter();
	OperationBody getBody();
	List<UMLAnonymousClass> getAnonymousClassList();
	List<LambdaExpressionObject> getAllLambdas();
	List<AbstractCall> getAllOperationInvocations();
	List<String> getAllVariables();
	List<UMLComment> getComments();
	String getName();
	String getElementType();
	String getClassName();
	String toQualifiedString();
	Map<String, Set<VariableDeclaration>> variableDeclarationMap();
	UMLAnonymousClass findAnonymousClass(AnonymousClassDeclarationObject anonymousClassDeclaration);
	boolean hasTestAnnotation();
	boolean isDeclaredInAnonymousClass();
	boolean isGetter();
	boolean isConstructor();
	AbstractCall isDelegate();

	default int getBodyHashCode() {
		OperationBody operationBody = getBody();
		if(operationBody != null)
			return operationBody.getBodyHashCode();
		return 0;
	}

	default List<String> stringRepresentation() {
		OperationBody operationBody = getBody();
		if(operationBody != null)
			return operationBody.stringRepresentation();
		return Collections.emptyList();
	}

	default List<UMLType> commonParameterTypes(VariableDeclarationContainer operation) {
		List<UMLType> commonParameterTypes = new ArrayList<UMLType>();
		List<UMLType> thisParameterTypeList = this.getParameterTypeList();
		List<UMLType> otherParameterTypeList = operation.getParameterTypeList();
		int min = Math.min(thisParameterTypeList.size(), otherParameterTypeList.size());
		for(int i=0; i<min; i++) {
			UMLType thisParameterType = thisParameterTypeList.get(i);
			UMLType otherParameterType = otherParameterTypeList.get(i);
			if(thisParameterType.equals(otherParameterType)) {
				commonParameterTypes.add(thisParameterType);
			}
		}
		return commonParameterTypes;
	}

	default CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		OperationBody operationBody = getBody();
		if(operationBody != null) {
			return operationBody.loopWithVariables(currentElementName, collectionName);
		}
		return null;
	}

	default Map<String, Set<String>> aliasedVariables() {
		OperationBody operationBody = getBody();
		if(operationBody != null) {
			Map<String, Set<VariableDeclaration>> variableDeclarationMap = variableDeclarationMap();
			Map<String, Set<String>> map = operationBody.aliasedVariables();
			Map<String, Set<String>> toBeAdded = new LinkedHashMap<String, Set<String>>();
			Set<String> keysToBeRemoved = new LinkedHashSet<String>();
			for(String key : map.keySet()) {
				if(!variableDeclarationMap.containsKey(key)) {
					keysToBeRemoved.add(key);
				}
				else {
					//exclude exception variables declared in catch blocks
					for(VariableDeclaration variable : variableDeclarationMap.get(key)) {
						UMLType variableType = variable.getType();
						if(variableType != null && variableType.getClassType().endsWith("Exception")) {
							keysToBeRemoved.add(key);
							break;
						}
					}
					//exclude variables aliased with fields
					boolean foundInLocalVariables = false;
					for(String value : map.get(key)) {
						if(variableDeclarationMap.containsKey(value)) {
							foundInLocalVariables = true;
							break;
						}
						else {
							String[] tokens = value.split("\\s");
							if(tokens.length >= 2) {
								String lastToken = tokens[tokens.length-1];
								String beforeLastToken = tokens[tokens.length-2];
								if(variableDeclarationMap.containsKey(lastToken)) {
									UMLType variableType = variableDeclarationMap.get(lastToken).iterator().next().getType();
									if(variableType != null && variableType.toString().equals(beforeLastToken)) {
										Set<String> values = new LinkedHashSet<>();
										values.add(lastToken);
										toBeAdded.put(key, values);
										break;
									}
								}
							}
						}
					}
					if(!foundInLocalVariables) {
						keysToBeRemoved.add(key);
					}
				}
			}
			for(String key : keysToBeRemoved) {
				map.remove(key);
			}
			map.putAll(toBeAdded);
			return map;
		}
		return new LinkedHashMap<String, Set<String>>();
	}

	default Map<String, Set<String>> aliasedAttributes() {
		OperationBody operationBody = getBody();
		if(operationBody != null && isConstructor()) {
			List<String> parameterNames = getParameterNameList();
			Map<String, Set<String>> map = operationBody.aliasedAttributes();
			Set<String> keysToBeRemoved = new LinkedHashSet<String>();
			for(String key : map.keySet()) {
				if(!parameterNames.contains(key)) {
					keysToBeRemoved.add(key);
				}
			}
			for(String key : keysToBeRemoved) {
				map.remove(key);
			}
			return map;
		}
		return new LinkedHashMap<String, Set<String>>();
	}
}
