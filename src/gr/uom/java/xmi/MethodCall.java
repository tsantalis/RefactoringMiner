package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class MethodCall extends AccessedMember {
	private String originClassName;
	private String methodName;
    private UMLType returnType;
    private List<UMLType> parameterTypeList;
    private boolean isConstructorCall;
    private volatile int hashCode = 0;
    
    public MethodCall(String originClassName, String methodName, UMLType returnType) {
    	this.originClassName = originClassName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypeList = new ArrayList<UMLType>();
        this.isConstructorCall = false;
    }

    public String getOriginClassName() {
		return originClassName;
	}

	public String getMethodName() {
		return methodName;
	}

	public UMLType getReturnType() {
		return returnType;
	}

	public boolean addParameter(UMLType parameterType) {
        return parameterTypeList.add(parameterType);
    }

    public void setConstructorCall(boolean isConstructorCall) {
		this.isConstructorCall = isConstructorCall;
	}

    public boolean matchesOperation(UMLOperation operation) {
    	if(this.originClassName.equals(operation.getClassName()) && this.methodName.equals(operation.getName())) {
    		UMLParameter returnParameter = operation.getReturnParameter();
    		if(returnParameter != null && returnParameter.getType().equals(this.returnType)) {
    			List<UMLType> operationParameterTypeList = operation.getParameterTypeList();
    			if(parameterTypeList.size() == operationParameterTypeList.size()) {
    				boolean allTypesMatch = true;
    				for(int i=0; i<parameterTypeList.size(); i++) {
    					UMLType methodCallParameterType = parameterTypeList.get(i);
    					UMLType operationParameterType = operationParameterTypeList.get(i);
    					if(!(methodCallParameterType.getClassType().equals(operationParameterType.getClassType()) &&
    							methodCallParameterType.getArrayDimension() == operationParameterType.getArrayDimension()) ) {
    						allTypesMatch = false;
    						break;
    					}
    				}
    				if(allTypesMatch)
    					return true;
    			}
    		}
    	}
    	return false;
    }

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof MethodCall) {
        	MethodCall methodCall = (MethodCall)o;
            return originClassName.equals(methodCall.originClassName) &&
                methodName.equals(methodCall.methodName) &&
                returnType.equals(methodCall.returnType) &&
                parameterTypeList.equals(methodCall.parameterTypeList);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(originClassName).append(".");
        if(isConstructorCall) {
        	if(originClassName.contains("."))
				sb.append(originClassName.substring(originClassName.lastIndexOf(".")+1));
			else
				sb.append(originClassName);
        }
        else
        	sb.append(methodName);
        sb.append("(");
        if(!parameterTypeList.isEmpty()) {
            for(int i=0; i<parameterTypeList.size()-1; i++)
                sb.append(parameterTypeList.get(i)).append(", ");
            sb.append(parameterTypeList.get(parameterTypeList.size()-1));
        }
        sb.append(")");
        if(!isConstructorCall)
        	sb.append(" : ").append(returnType);
        return sb.toString();
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + originClassName.hashCode();
    		result = 37*result + methodName.hashCode();
    		result = 37*result + returnType.hashCode();
    		for(UMLType parameterType : parameterTypeList) {
    			result = 37*result + parameterType.hashCode();
    		}
    		hashCode = result;
    	}
    	return hashCode;
    }
}
