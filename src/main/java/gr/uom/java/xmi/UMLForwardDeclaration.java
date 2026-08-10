package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLForwardDeclaration implements Serializable, LocationInfoProvider {
	private LocationInfo location;
	private String type;
	private String name;
	private boolean isFriend;
	private Optional<UMLOperation> function;

	public UMLForwardDeclaration(LocationInfo location, String type, String name, boolean isFriend) {
		this.location = location;
		this.type = type;
		this.name = name;
		this.isFriend = isFriend;
		this.function = Optional.empty();
	}

	public Optional<UMLOperation> getFunction() {
		return function;
	}

	public void setFunction(UMLOperation function) {
		this.function = Optional.of(function);
	}

	@Override
	public LocationInfo getLocationInfo() {
		return location;
	}

	@Override
	public CodeRange codeRange() {
		return location.codeRange();
	}

	@Override
	public int hashCode() {
		return Objects.hash(Boolean.valueOf(isFriend), name, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLForwardDeclaration other = (UMLForwardDeclaration) obj;
		return isFriend == other.isFriend && Objects.equals(name, other.name) && Objects.equals(type, other.type);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(isFriend)
			sb.append("friend ");
		sb.append(type).append(" " );
		sb.append(name);
		return sb.toString();
	}
}
