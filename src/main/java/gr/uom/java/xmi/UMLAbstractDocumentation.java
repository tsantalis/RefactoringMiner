package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.CodeRange;

public abstract class UMLAbstractDocumentation implements LocationInfoProvider {
	protected String text;
	protected LocationInfo locationInfo;

	public UMLAbstractDocumentation(String text, LocationInfo locationInfo) {
		this.text = text;
		this.locationInfo = locationInfo;
	}

	public String getFullText() {
		return text;
	}

	public abstract String getText();

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
