package gr.uom.java.xmi;

import java.util.Objects;
import java.util.Optional;

import gr.uom.java.xmi.decomposition.AbstractExpression;

public class UMLPreprocessorStatement {
	
	enum Directive {
		DEFINE("#define"),
		UNDEF("#undef"),
		INCLUDE("#include"),
		IF("#if"),
		IFDEF("#ifdef"),
		IFNDEF("#ifndef"),
		ELIF("#elif"),
		ELSE("#else"),
		ENDIF("#endif"),
		PRAGMA("#pragma"),
		ERROR("#error"); 

		private String name;

		private Directive(String name) {
			this.name = name;
		} 
	}
	private LocationInfo location;
	private Directive type;
	private Optional<String> value;
	private Optional<String> name;
	
	public UMLPreprocessorStatement(LocationInfo location, Directive type) {
		this.location = location;
		this.type = type;
	}

	public UMLPreprocessorStatement(LocationInfo location, Directive type, String value) {
		this(location,type);
		this.value = value !=null ? Optional.of(value) : Optional.empty();
	}

	public UMLPreprocessorStatement(LocationInfo location,Directive type, String value, String name) {
		this(location,type,value);
		this.name = name !=null ? Optional.of(name) : Optional.empty();
	}

	public LocationInfo getLocationInfo() {
		return location;
	}

	public Directive getType() {
		return type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLPreprocessorStatement other = (UMLPreprocessorStatement) obj;
		return Objects.equals(name, other.name) && type == other.type;
	}
}
