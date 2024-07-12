package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLPackage implements LocationInfoProvider, Serializable {
	private String name;
	private LocationInfo locationInfo;

	public UMLPackage(String name, LocationInfo locationInfo) {
		this.name = name;
		this.locationInfo = locationInfo;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLPackage other = (UMLPackage) obj;
		return Objects.equals(name, other.name);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("package ");
		sb.append(name);
		return sb.toString();
	}
}
