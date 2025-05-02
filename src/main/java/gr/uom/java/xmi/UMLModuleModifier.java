package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ModuleModifier;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLModuleModifier implements Serializable, LocationInfoProvider {
	private String keyword;
	private LocationInfo locationInfo;
	
	public UMLModuleModifier(CompilationUnit cu, String sourceFolder, String filePath, ModuleModifier modifier) {
		this.keyword = modifier.getKeyword().toString();
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, modifier, CodeElementType.MODULE_MODIFIER);
	}

	public String getKeyword() {
		return keyword;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public String toString() {
		return keyword;
	}

	@Override
	public int hashCode() {
		return Objects.hash(keyword);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLModuleModifier other = (UMLModuleModifier) obj;
		return Objects.equals(keyword, other.keyword);
	}
}
