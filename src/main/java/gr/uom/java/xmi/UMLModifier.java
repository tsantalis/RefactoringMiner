package gr.uom.java.xmi;

import java.io.Serializable;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLModifier implements Serializable, LocationInfoProvider {
	private String keyword;
	private LocationInfo locationInfo;
	
	public UMLModifier(CompilationUnit cu, String filePath, Modifier modifier) {
		this.keyword = modifier.getKeyword().toString();
		this.locationInfo = new LocationInfo(cu, filePath, modifier, CodeElementType.MODIFIER);
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
}
