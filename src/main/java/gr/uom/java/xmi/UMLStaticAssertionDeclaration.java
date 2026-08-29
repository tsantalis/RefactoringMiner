package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLStaticAssertionDeclaration implements Serializable, LocationInfoProvider {
	private LocationInfo location;
	private String rawString;

	public UMLStaticAssertionDeclaration(LocationInfo location, String rawString) {
		this.location = location;
		this.rawString = rawString;
	}

	public String getRawString() {
		return rawString;
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
		return Objects.hash(rawString);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLStaticAssertionDeclaration other = (UMLStaticAssertionDeclaration) obj;
		return Objects.equals(rawString, other.rawString);
	}
}
