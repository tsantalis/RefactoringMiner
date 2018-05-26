package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;

public class VariableDeclaration implements LocationInfoProvider {
	private String variableName;
	private String initializer;
	private LocationInfo locationInfo;
	
	public VariableDeclaration(CompilationUnit cu, String filePath, VariableDeclarationFragment fragment) {
		this.locationInfo = new LocationInfo(cu, filePath, fragment);
		this.variableName = fragment.getName().getIdentifier();
		this.initializer = fragment.getInitializer() != null ? fragment.getInitializer().toString() : null;
	}

	public String getVariableName() {
		return variableName;
	}

	public String getInitializer() {
		return initializer;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((initializer == null) ? 0 : initializer.hashCode());
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableDeclaration other = (VariableDeclaration) obj;
		if (initializer == null) {
			if (other.initializer != null)
				return false;
		} else if (!initializer.equals(other.initializer))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append(variableName);
        if(initializer != null) {
        	sb.append("=").append(initializer);
        }
        return sb.toString();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}
}
