package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLComment implements LocationInfoProvider {
	private String text;
	private LocationInfo locationInfo;

	public UMLComment(String text, LocationInfo locationInfo) {
		this.text = text;
		this.locationInfo = locationInfo;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
