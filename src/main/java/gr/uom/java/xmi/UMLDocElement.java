package gr.uom.java.xmi;

import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLDocElement implements LocationInfoProvider {
	private String text;
	private LocationInfo locationInfo;
	private boolean isMemberRef;
	private boolean isMethodRef;
	private boolean isName;
	private boolean isTagProperty;

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

	public String toString() {
		return text;
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

	public boolean isCodeReference() {
		return isMemberRef || isMethodRef || isName || isTagProperty;
	}

	public boolean isMemberRef() {
		return isMemberRef;
	}

	public void setMemberRef(boolean isMemberRef) {
		this.isMemberRef = isMemberRef;
	}

	public boolean isMethodRef() {
		return isMethodRef;
	}

	public void setMethodRef(boolean isMethodRef) {
		this.isMethodRef = isMethodRef;
	}

	public boolean isName() {
		return isName;
	}

	public void setName(boolean isName) {
		this.isName = isName;
	}

	public boolean isTagProperty() {
		return isTagProperty;
	}

	public void setTagProperty(boolean isTagProperty) {
		this.isTagProperty = isTagProperty;
	}
}
