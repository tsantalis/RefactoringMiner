package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLTypeParameter implements LocationInfoProvider {
	private String name;
	private List<UMLType> typeBounds;
	private List<UMLAnnotation> annotations;
	private LocationInfo locationInfo;

	public UMLTypeParameter(String name, LocationInfo locationInfo) {
		this.name = name;
		this.locationInfo = locationInfo;
		this.typeBounds = new ArrayList<UMLType>();
		this.annotations = new ArrayList<UMLAnnotation>();
	}

	public String getName() {
		return name;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public List<UMLType> getTypeBounds() {
		return typeBounds;
	}

	public void addTypeBound(UMLType type) {
		typeBounds.add(type);
	}

	public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

	public void addAnnotation(UMLAnnotation annotation) {
		annotations.add(annotation);
	}

	protected String typeBoundsToString() {
		StringBuilder sb = new StringBuilder();
		if(!typeBounds.isEmpty()) {
			sb.append(" extends ");
			for(int i = 0; i < typeBounds.size(); i++) {
				sb.append(typeBounds.get(i).toQualifiedString());
				if(i < typeBounds.size() - 1)
					sb.append(" & ");
			}
		}
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(typeBoundsToString());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((typeBounds == null) ? 0 : typeBounds.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLTypeParameter other = (UMLTypeParameter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (typeBounds == null) {
			if (other.typeBounds != null)
				return false;
		} else if (!typeBounds.equals(other.typeBounds))
			return false;
		return true;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
