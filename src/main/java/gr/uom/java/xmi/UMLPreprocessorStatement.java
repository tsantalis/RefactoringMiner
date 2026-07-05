package gr.uom.java.xmi;

import java.util.Objects;
import java.util.Optional;

import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLPreprocessorStatement implements LocationInfoProvider {
	
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
	private Optional<UMLPreprocessorStatement> previous;
	
	public UMLPreprocessorStatement(LocationInfo location, Directive type) {
		this.location = location;
		this.type = type;
		this.name = Optional.empty();
		this.value = Optional.empty();
		this.previous = Optional.empty();
	}

	public UMLPreprocessorStatement(LocationInfo location, Directive type, String value) {
		this(location, type);
		this.value = value != null ? Optional.of(value) : Optional.empty();
	}

	public UMLPreprocessorStatement(LocationInfo location, Directive type, String value, String name) {
		this(location, type, value);
		this.name = name != null ? Optional.of(name) : Optional.empty();
	}

	public LocationInfo getLocationInfo() {
		return location;
	}

	public CodeRange codeRange() {
		return location.codeRange();
	}

	public Directive getType() {
		return type;
	}

	public Optional<String> getValue() {
		return value;
	}

	public Optional<String> getName() {
		return name;
	}

	public void setPreviousStatement(UMLPreprocessorStatement previous) {
		this.previous = Optional.of(previous);
	}

	public Optional<UMLPreprocessorStatement> getPrevious() {
		return previous;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type.name);
		if(name.isPresent())
			sb.append(" ").append(name.get());
		if(value.isPresent())
			sb.append(" ").append(value.get());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, previous);
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
		return Objects.equals(name, other.name) && type == other.type && Objects.equals(previous, other.previous);
	}
}
