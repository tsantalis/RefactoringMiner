package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class ListCompositeType extends UMLType {
	public enum Kind {
		UNION("|"), INTERSECTION("&");
		private String operand;
		private Kind(String operand) {
			this.operand = operand;
		}
	}
	private List<UMLType> types = new ArrayList<UMLType>();
	private Kind kind;

	public ListCompositeType(List<UMLType> types, Kind kind) {
		this.types = types;
		this.kind = kind;
	}

	public List<UMLType> getTypes() {
		return types;
	}

	public Kind getKind() {
		return kind;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
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
		ListCompositeType other = (ListCompositeType) obj;
		if (kind != other.kind)
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < types.size(); i++) {
			sb.append(types.get(i).toString());
			if(i < types.size() - 1)
				sb.append(kind.operand);
		}
		return sb.toString();
	}

	@Override
	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < types.size(); i++) {
			sb.append(types.get(i).toQualifiedString());
			if(i < types.size() - 1)
				sb.append(kind.operand);
		}
		return sb.toString();
	}

	@Override
	public String getClassType() {
		return types.get(types.size()-1).getClassType();
	}
}
