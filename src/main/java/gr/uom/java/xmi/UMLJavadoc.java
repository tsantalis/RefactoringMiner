package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLJavadoc implements LocationInfoProvider {
	private LocationInfo locationInfo;
	private List<UMLTagElement> tags;

	public UMLJavadoc(LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		this.tags = new ArrayList<UMLTagElement>();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public void addTag(UMLTagElement tag) {
		tags.add(tag);
	}

	public List<UMLTagElement> getTags() {
		return tags;
	}

	public boolean contains(String s) {
		for(UMLTagElement tag : tags) {
			if(tag.contains(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsIgnoreCase(String s) {
		for(UMLTagElement tag : tags) {
			if(tag.containsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean equalText(UMLJavadoc other) {
		return this.tags.equals(other.tags);
	}
}
