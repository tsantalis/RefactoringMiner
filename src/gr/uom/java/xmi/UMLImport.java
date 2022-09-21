package gr.uom.java.xmi;

import java.util.Objects;

public class UMLImport {
	private String name;
	private boolean isOnDemand;
	private boolean isStatic;

	public UMLImport(String name, boolean isOnDemand, boolean isStatic) {
		this.name = name;
		this.isOnDemand = isOnDemand;
		this.isStatic = isStatic;
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
