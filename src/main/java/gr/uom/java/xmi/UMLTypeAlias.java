package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLTypeAlias implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private String name;
	private List<UMLTypeParameter> typeParameters = new ArrayList<>();
	private UMLType rightType;

	public UMLTypeAlias(String name, UMLType rightType, LocationInfo locationInfo) {
		this.name = name;
		this.rightType = rightType;
		this.locationInfo = locationInfo;
	}

	public String getName() {
		return name;
	}

	public UMLType getRightType() {
		return rightType;
	}

	public void addTypeParameter(UMLTypeParameter typeParameter) {
		typeParameters.add(typeParameter);
	}

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("typealias ");
		sb.append(name);
		if(typeParameters.size() > 0) {
			sb.append("<");
			for(int i = 0; i < typeParameters.size(); i++) {
				sb.append(typeParameters.get(i).toString());
				if(i < typeParameters.size() - 1)
					sb.append(",");
			}
			sb.append(">");
		}
		sb.append(" = ");
		sb.append(rightType);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, rightType, typeParameters);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLTypeAlias other = (UMLTypeAlias) obj;
		return Objects.equals(name, other.name) && Objects.equals(rightType, other.rightType)
				&& Objects.equals(typeParameters, other.typeParameters);
	}
}
