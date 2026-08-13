package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLImport implements Serializable, LocationInfoProvider {
	private String name;
	private boolean isOnDemand;
	private boolean isStatic;
	private LocationInfo locationInfo;
	private Optional<String> alias;

	public UMLImport(String name, boolean isOnDemand, boolean isStatic, LocationInfo locationInfo) {
		this.name = name;
		this.isOnDemand = isOnDemand;
		this.isStatic = isStatic;
		this.locationInfo = locationInfo;
		this.alias = Optional.empty();
	}

	public Optional<String> getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = Optional.of(alias);
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

	public boolean isOnDemand() {
		return isOnDemand;
	}

	public boolean isStatic() {
		return isStatic;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, isOnDemand, isStatic);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLImport other = (UMLImport) obj;
		return Objects.equals(name, other.name) && isOnDemand == other.isOnDemand
				&& isStatic == other.isStatic;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("import ");
		if(isStatic) {
			sb.append("static ");
		}
		sb.append(name);
		if(isOnDemand) {
			sb.append(".*");
		}
		return sb.toString();
	}
}
