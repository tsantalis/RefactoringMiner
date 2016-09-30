package gr.uom.java.xmi;

import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.diff.StringDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.util.AstUtils;

public class UMLOperation implements Comparable<UMLOperation>, Serializable {
	private String name;
	private String visibility;
	private boolean isAbstract;
	private List<UMLParameter> parameters;
	private String className;
	private Set<AccessedMember> accessedMembers;
	private boolean isConstructor;
	private boolean isFinal;
	private boolean isStatic;
	private boolean emptyBody;
	private OperationBody operationBody;
	
	public UMLOperation(String name) {
        this.name = name;
        this.parameters = new ArrayList<UMLParameter>();
        this.accessedMembers = new LinkedHashSet<AccessedMember>();
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

	public void addAccessedMember(AccessedMember member) {
		this.accessedMembers.add(member);
	}

	public Set<AccessedMember> getAccessedMembers() {
		return accessedMembers;
	}

	public void setAccessedMembers(Set<AccessedMember> accessedMembers) {
		this.accessedMembers = accessedMembers;
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
			//String thisParameterType = thisParameter.getType().getClassType();
			//String otherParameterType = otherParameter.getType().getClassType();
			//if(!thisParameterType.equals(this.className) && !otherParameterType.equals(operation.className)) {
			if(!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter))
				return false;
			//}
			i++;
		}
		return true;
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
				this.parameters.equals(operation.parameters);
		}
		return false;
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
}
