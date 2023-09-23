package gr.uom.java.xmi;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLComment implements LocationInfoProvider {
	private String text;
	private LocationInfo locationInfo;

	public UMLComment(String text, LocationInfo locationInfo) {
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
		StringBuilder sb = new StringBuilder();
		if(locationInfo.getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
			sb.append(locationInfo.getStartLine());
		}
		if(locationInfo.getCodeElementType().equals(CodeElementType.BLOCK_COMMENT)) {
			sb.append(locationInfo.getStartLine()).append("-").append(locationInfo.getEndLine());
		}
		sb.append(": ");
		sb.append(text);
		return sb.toString();
	}
}
