package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.List;

import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class UMLParameter implements Serializable, VariableDeclarationProvider {
	private String name;
	private UMLType type;
	private String kind;
	private boolean varargs;
	private VariableDeclaration variableDeclaration;

	public UMLParameter(String name, UMLType type, String kind, boolean varargs) {
		this.name = name;
		this.type = type;
		this.kind = kind;
		this.varargs = varargs;
	}

	public UMLType getType() {
		return type;
	}

	public VariableDeclaration getVariableDeclaration() {
		return variableDeclaration;
	}

	public void setVariableDeclaration(VariableDeclaration variableDeclaration) {
		this.variableDeclaration = variableDeclaration;
	}

	public String getName() {
		return name;
	}

	public String getKind() {
		return kind;
	}

	public boolean isVarargs() {
		return varargs;
	}

	public boolean isFinal() {
		if(variableDeclaration != null) {
			return variableDeclaration.isFinal();
		}
		return false;
	}

	public List<UMLAnnotation> getAnnotations() {
		return variableDeclaration.getAnnotations();
	}

	public boolean equalsExcludingType(UMLParameter parameter) {
		return this.name.equals(parameter.name) &&
				this.kind.equals(parameter.kind);
	}

	public boolean equalsIncludingName(UMLParameter parameter) {
		return this.name.equals(parameter.name) &&
				this.type.equals(parameter.type) &&
				this.kind.equals(parameter.kind) &&
				this.varargs == parameter.varargs;
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof UMLParameter) {
			UMLParameter parameter = (UMLParameter)o;
			return this.type.equals(parameter.type) &&
				this.kind.equals(parameter.kind) &&
				this.varargs == parameter.varargs;
		}
		return false;
	}

	public boolean equalsQualified(UMLParameter parameter) {
		return this.type.equalsQualified(parameter.type) &&
				this.kind.equals(parameter.kind) &&
				this.varargs == parameter.varargs;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (varargs ? 1231 : 1237);
		return result;
	}

	public String toString() {
		if(kind.equals("return"))
			return type.toString();
		else {
			if(varargs) {
				return name + " " + type.toString().substring(0, type.toString().lastIndexOf("[]")) + "...";
			}
			else {
				return name + " " + type;
			}
		}
	}

	public String toQualifiedString() {
		if(kind.equals("return"))
			return type.toQualifiedString();
		else {
			if(varargs) {
				return name + " " + type.toQualifiedString().substring(0, type.toQualifiedString().lastIndexOf("[]")) + "...";
			}
			else {
				return name + " " + type.toQualifiedString();
			}
		}
	}
}
