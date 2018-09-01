package gr.uom.java.xmi;

import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.util.AstUtils;

public class UMLOperation implements Comparable<UMLOperation>, Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private String name;
	private String visibility;
	private boolean isAbstract;
	private List<UMLParameter> parameters;
	private String className;
	private boolean isConstructor;
	private boolean isFinal;
	private boolean isStatic;
	private boolean emptyBody;
	private OperationBody operationBody;
	private boolean testAnnotation;
	private List<UMLAnonymousClass> anonymousClassList;
	
	public UMLOperation(String name, LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
        this.name = name;
        this.parameters = new ArrayList<UMLParameter>();
        this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
    }

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public String getName() {
		return name;
	}

	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public void setConstructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public boolean hasEmptyBody() {
		return emptyBody;
	}

	public void setEmptyBody(boolean emptyBody) {
		this.emptyBody = emptyBody;
	}

	public OperationBody getBody() {
		return operationBody;
	}

	public boolean hasTestAnnotation() {
		return testAnnotation;
	}

	public void setTestAnnotation(boolean testAnnotation) {
		this.testAnnotation = testAnnotation;
	}

	public Set<OperationInvocation> getAllOperationInvocations() {
		if(operationBody != null)
			return operationBody.getAllOperationInvocations();
		return new LinkedHashSet<OperationInvocation>();
	}

	public List<String> getAllVariables() {
		if(operationBody != null)
			return operationBody.getAllVariables();
		return new ArrayList<String>();
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		if(operationBody != null)
			return operationBody.getAllVariableDeclarations();
		return new ArrayList<VariableDeclaration>();
	}

	public Map<String, UMLType> variableTypeMap() {
		Map<String, UMLType> variableTypeMap = new LinkedHashMap<String, UMLType>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				variableTypeMap.put(parameter.getName(), parameter.getType());
		}
		for(VariableDeclaration declaration : getAllVariableDeclarations()) {
			variableTypeMap.put(declaration.getVariableName(), declaration.getType());
		}
		return variableTypeMap;
	}

	public int statementCount() {
		if(operationBody != null)
			return operationBody.statementCount();
		return 0;
	}

	public void setBody(OperationBody body) {
		this.operationBody = body;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void addParameter(UMLParameter parameter) {
		this.parameters.add(parameter);
	}

	public List<UMLParameter> getParameters() {
		return parameters;
	}

	public void addAnonymousClass(UMLAnonymousClass anonymous) {
		this.anonymousClassList.add(anonymous);
	}

	public List<UMLAnonymousClass> getAnonymousClassList() {
		return anonymousClassList;
	}

	public UMLParameter getReturnParameter() {
		for(UMLParameter parameter : parameters) {
			if(parameter.getKind().equals("return"))
				return parameter;
		}
		return null;
	}

	public boolean equalReturnParameter(UMLOperation operation) {
		UMLParameter thisReturnParameter = this.getReturnParameter();
		UMLParameter otherReturnParameter = operation.getReturnParameter();
		if(thisReturnParameter != null && otherReturnParameter != null)
			return thisReturnParameter.equals(otherReturnParameter);
		else if(thisReturnParameter == null && otherReturnParameter == null)
			return true;
		else
			return false;
	}

	public boolean equalSignature(UMLOperation operation) {
		return this.name.equals(operation.name) &&	this.parameters.equals(operation.parameters);
	}

	public boolean equalSignatureIgnoringOperationName(UMLOperation operation) {
		return this.visibility.equals(operation.visibility) &&
				this.isAbstract == operation.isAbstract &&
				this.isFinal == operation.isFinal &&
				this.isStatic == operation.isStatic &&
				this.parameters.equals(operation.parameters);
	}

	public boolean equalSignatureIgnoringChangedTypes(UMLOperation operation) {
		if(!(this.isConstructor && operation.isConstructor || this.name.equals(operation.name)))
			return false;
		if(this.isAbstract != operation.isAbstract)
			return false;
		if(this.isStatic != operation.isStatic)
			return false;
		if(this.isFinal != operation.isFinal)
			return false;
		if(this.parameters.size() != operation.parameters.size())
			return false;
		int i=0;
		for(UMLParameter thisParameter : this.parameters) {
			UMLParameter otherParameter = operation.parameters.get(i);
			if(!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter))
				return false;
			i++;
		}
		return true;
	}

	public List<UMLParameter> getParametersWithoutReturnType() {
		List<UMLParameter> params = new ArrayList<UMLParameter>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				params.add(parameter);
		}
		return params;
	}

	public List<UMLType> commonParameterTypes(UMLOperation operation) {
		List<UMLType> commonParameterTypes = new ArrayList<UMLType>();
		List<UMLType> thisParameterTypeList = getParameterTypeList();
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

	public List<UMLType> getParameterTypeList() {
		List<UMLType> parameterTypeList = new ArrayList<UMLType>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterTypeList.add(parameter.getType());
		}
		return parameterTypeList;
	}

	public List<String> getParameterNameList() {
		List<String> parameterNameList = new ArrayList<String>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterNameList.add(parameter.getName());
		}
		return parameterNameList;
	}

	public int getNumberOfNonVarargsParameters() {
		int counter = 0;
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return") && !parameter.isVarargs()) {
				counter++;
			}
		}
		return counter;
	}

	public boolean hasVarargsParameter() {
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return") && parameter.isVarargs()) {
				return true;
			}
		}
		return false;
	}

	public OperationInvocation isDelegate() {
		if(getBody() != null) {
			List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
			if(statements.size() == 1 && statements.get(0) instanceof StatementObject) {
				StatementObject statement = (StatementObject)statements.get(0);
				Map<String, OperationInvocation> operationInvocationMap = statement.getMethodInvocationMap();
				for(String key : operationInvocationMap.keySet()) {
					OperationInvocation operationInvocation = operationInvocationMap.get(key);
					if(operationInvocation.matchesOperation(this) || operationInvocation.getMethodName().equals(this.getName())) {
						return operationInvocation;
					}
				}
			}
		}
		return null;
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof UMLOperation) {
			UMLOperation operation = (UMLOperation)o;
			return this.className.equals(operation.className) &&
				this.name.equals(operation.name) &&
				this.visibility.equals(operation.visibility) &&
				this.isAbstract == operation.isAbstract &&
				this.getParameterTypeList().equals(operation.getParameterTypeList());
		}
		return false;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + (isAbstract ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((getParameterTypeList() == null) ? 0 : getParameterTypeList().hashCode());
		result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(visibility);
		sb.append(" ");
		if(isAbstract) {
			sb.append("abstract");
			sb.append(" ");
		}
		sb.append(name);
		UMLParameter returnParameter = getReturnParameter();
		List<UMLParameter> parameters = new ArrayList<UMLParameter>(this.parameters);
		parameters.remove(returnParameter);
		sb.append("(");
		for(int i=0; i<parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				sb.append(parameter);
				if(i < parameters.size()-1)
					sb.append(", ");
			}
		}
		sb.append(")");
		if(returnParameter != null) {
			sb.append(" : ");
			sb.append(returnParameter.toString());
		}
		return sb.toString();
	}

	public String getKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(className);
		sb.append('#');
		sb.append(name);
		UMLParameter returnParameter = getReturnParameter();
		List<UMLParameter> parameters = new ArrayList<UMLParameter>(this.parameters);
		parameters.remove(returnParameter);
		sb.append("(");
		for (int i = 0; i < parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				sb.append(AstUtils.stripTypeParamsFromTypeName(parameter.getType().toString()));
				if(i < parameters.size() - 1)
					sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	public int compareTo(UMLOperation operation) {
		return this.toString().compareTo(operation.toString());
	}

	public double normalizedNameDistance(UMLOperation operation) {
		String s1 = getName().toLowerCase();
		String s2 = operation.getName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean equalParameters(UMLOperation operation) {
		return this.equalReturnParameter(operation) && this.getParameters().equals(operation.getParameters());
	}

	public boolean equalParameterTypes(UMLOperation operation) {
		return this.equalReturnParameter(operation) && this.getParameterTypeList().equals(operation.getParameterTypeList());
	}

	public boolean equalParameterNames(UMLOperation operation) {
		return this.equalReturnParameter(operation) && this.getParameterNameList().equals(operation.getParameterNameList());
	}

	public boolean overloadedParameters(UMLOperation operation) {
		return this.equalReturnParameter(operation) &&
				(this.getParameters().containsAll(operation.getParameters()) || operation.getParameters().containsAll(this.getParameters()));
	}

	public boolean overloadedParameterTypes(UMLOperation operation) {
		return this.equalReturnParameter(operation) &&
				(this.getParameterTypeList().containsAll(operation.getParameterTypeList()) || operation.getParameterTypeList().containsAll(this.getParameterTypeList()));
	}
	
	public boolean replacedParameterTypes(UMLOperation operation) {
		List<UMLType> thisParameterTypes = this.getParameterTypeList();
		List<UMLType> otherParameterTypes = operation.getParameterTypeList();
		
		if(thisParameterTypes.size() == otherParameterTypes.size() && thisParameterTypes.size() > 0) {
			int commonParameterTypes = 0;
			int differentParameterTypes = 0;
			for(int i=0; i<thisParameterTypes.size(); i++) {
				UMLType thisParameterType = thisParameterTypes.get(i);
				UMLType otherParameterType = otherParameterTypes.get(i);
				if(thisParameterType.equals(otherParameterType) ||
						(thisParameterType.getClassType().equals(otherParameterType.getClassType()) && thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
						thisParameterType.equalsWithSubType(otherParameterType)) {
					commonParameterTypes++;
				}
				else {
					differentParameterTypes++;
				}
			}
			return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
		}
		else if(thisParameterTypes.size() > otherParameterTypes.size() && thisParameterTypes.size() > 0) {
			int commonParameterTypes = 0;
			int differentParameterTypes = 0;
			for(int i=0; i<otherParameterTypes.size(); i++) {
				UMLType thisParameterType = thisParameterTypes.get(i);
				UMLType otherParameterType = otherParameterTypes.get(i);
				if(thisParameterType.equals(otherParameterType) ||
						(thisParameterType.getClassType().equals(otherParameterType.getClassType()) && thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
						thisParameterType.equalsWithSubType(otherParameterType)) {
					commonParameterTypes++;
				}
				else {
					differentParameterTypes++;
				}
			}
			return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
		}
		else if(otherParameterTypes.size() > thisParameterTypes.size() && thisParameterTypes.size() > 0) {
			int commonParameterTypes = 0;
			int differentParameterTypes = 0;
			for(int i=0; i<thisParameterTypes.size(); i++) {
				UMLType thisParameterType = thisParameterTypes.get(i);
				UMLType otherParameterType = otherParameterTypes.get(i);
				if(thisParameterType.equals(otherParameterType) ||
						(thisParameterType.getClassType().equals(otherParameterType.getClassType()) && thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
						thisParameterType.equalsWithSubType(otherParameterType)) {
					commonParameterTypes++;
				}
				else {
					differentParameterTypes++;
				}
			}
			return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
		}
		return false;
	}

	public List<UMLOperation> getOperationsInsideAnonymousClass(List<UMLAnonymousClass> allAddedAnonymousClasses) {
		List<UMLOperation> operationsInsideAnonymousClass = new ArrayList<UMLOperation>();
		if(this.operationBody != null) {
			List<String> anonymousClassDeclarations = this.operationBody.getAllAnonymousClassDeclarations();
			for(String anonymousClassDeclaration : anonymousClassDeclarations) {
				for(UMLAnonymousClass anonymousClass : allAddedAnonymousClasses) {
					int foundOperations = 0;
					List<UMLOperation> operations = anonymousClass.getOperations();
					for(UMLOperation operation : operations) {
						if(anonymousClassDeclaration.contains(operation.getName())) {
							foundOperations++;
						}
					}
					if(foundOperations == operations.size()) {
						operationsInsideAnonymousClass.addAll(operations);
					}
				}
			}
		}
		return operationsInsideAnonymousClass;
	}

	public CodeRange codeRange() {
		LocationInfo info = getLocationInfo();
		return info.codeRange();
	}

	public boolean overridesObject() {
		return isEquals() || isHashCode() || isToString() || isClone() || isCompareTo();
	}

	private boolean isEquals() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("equals") && getReturnParameter().getType().equals("boolean") &&
				parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("Object");
	}

	private boolean isHashCode() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("hashCode") && getReturnParameter().getType().equals("int") && parameterTypeList.size() == 0;
	}

	private boolean isToString() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("toString") && getReturnParameter().getType().equals("String") && parameterTypeList.size() == 0;
	}

	private boolean isClone() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("clone") && getReturnParameter().getType().equals("Object") && parameterTypeList.size() == 0;
	}

	private boolean isCompareTo() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("compareTo") && getReturnParameter().getType().equals("int") && parameterTypeList.size() == 1;
	}

	public boolean compatibleSignature(UMLOperation removedOperation) {
		return equalParameterTypes(removedOperation) || overloadedParameterTypes(removedOperation) || replacedParameterTypes(removedOperation) || equalParameterNames(removedOperation);
	}
}
