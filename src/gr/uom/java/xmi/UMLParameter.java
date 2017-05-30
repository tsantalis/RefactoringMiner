package gr.uom.java.xmi;

import java.io.Serializable;

public class UMLParameter implements Serializable {
	private String name;
	private UMLType type;
	private String kind;
	private boolean varargs;

	public UMLParameter(String name, UMLType type, String kind, boolean varargs) {
		this.name = name;
		this.type = type;
		this.kind = kind;
		this.varargs = varargs;
	}

	public UMLType getType() {
		return type;
	}

	public void setType(UMLType type) {
		this.type = type;
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

	public boolean equalsExcludingType(UMLParameter parameter) {
		return this.name.equals(parameter.name) &&
				this.kind.equals(parameter.kind);
	}

	public boolean equalsIncludingName(UMLParameter parameter) {
		return this.name.equals(parameter.name) &&
				this.type.equals(parameter.type) &&
				this.kind.equals(parameter.kind);
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof UMLParameter) {
			UMLParameter parameter = (UMLParameter)o;
			return this.type.equals(parameter.type) &&
				this.kind.equals(parameter.kind);
		}
		return false;
	}

	public String toString() {
		if(kind.equals("return"))
			return type.toString();
		else
			return name + " " + type;
	}
}
