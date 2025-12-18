package gr.uom.java.xmi;

import java.util.List;

/**
 * Wrapper for function types in Kotlin.
 * Examples of function types:
 * (Int) -> String
 * (Int, Int) -> String
 * () -> Unit
 */
public class FunctionType extends UMLType {
	private final UMLType receiver;
	private final UMLType returnType;
	private final List<UMLType> parameterList;

	public FunctionType(UMLType receiver, UMLType returnType, List<UMLType> parameterList) {
		this.receiver = receiver;
		this.returnType = returnType;
		this.parameterList = parameterList;
	}

	public UMLType getReturnType() {
		return returnType;
	}

	public UMLType getReceiver() {
		return receiver;
	}

	public List<UMLType> getParameterList() {
		return parameterList;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionType other = (FunctionType) obj;
		if (returnType == null) {
			if (other.returnType != null)
				return false;
		} else if (!returnType.equals(other.returnType))
			return false;
		if (receiver == null) {
			if (other.receiver != null)
				return false;
		} else if (!receiver.equals(other.receiver))
			return false;
		if (parameterList == null) {
			if (other.parameterList != null)
				return false;
		} else if (!parameterList.equals(other.parameterList))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
		result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
		result = prime * result + ((parameterList == null) ? 0 : parameterList.hashCode());
		return result;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if(receiver != null) {
			s.append(receiver.toString()).append(".");
		}
		s.append("(");
		for (int i = 0; i < parameterList.size(); i++) {
			UMLType parameter = parameterList.get(i);
			s.append(parameter.toString());
			if (i < parameterList.size() - 1) {
				s.append(",");
			}
		}
		s.append(")").append(" -> ").append(returnType.toString());
		return s.toString();
	}

	@Override
	public String toQualifiedString() {
		StringBuilder s = new StringBuilder();
		if(receiver != null) {
			s.append(receiver.toQualifiedString()).append(".");
		}
		s.append("(");
		for (int i = 0; i < parameterList.size(); i++) {
			UMLType parameter = parameterList.get(i);
			s.append(parameter.toQualifiedString());
			if (i < parameterList.size() - 1) {
				s.append(",");
			}
		}
		s.append(")").append(" -> ").append(returnType.toQualifiedString());
		return s.toString();
	}

	@Override
	public String getClassType() {
		return returnType.getClassType();
	}
}
