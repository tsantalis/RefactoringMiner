package gr.uom.java.xmi;

import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLDocElement implements LocationInfoProvider {
	private String text;
	private LocationInfo locationInfo;
	public UMLDocElement(String text, LocationInfo locationInfo) {
		this.text = text;
		this.locationInfo = locationInfo;
	}

	public String getText() {
		return text;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	@Override
	public int hashCode() {
		return Objects.hash(text);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLDocElement other = (UMLDocElement) obj;
		return Objects.equals(text, other.text);
	}
}
