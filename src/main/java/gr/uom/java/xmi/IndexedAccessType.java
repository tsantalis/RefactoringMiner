package gr.uom.java.xmi;

public class IndexedAccessType extends UMLType {
	private UMLType objectType;
	private UMLType indexType;
	
	public IndexedAccessType(UMLType objectType, UMLType indexType) {
		this.objectType = objectType;
		this.indexType = indexType;
	}

	public UMLType getObjectType() {
		return objectType;
	}

	public UMLType getIndexType() {
		return indexType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexedAccessType other = (IndexedAccessType) obj;
		if (objectType == null) {
			if (other.objectType != null)
				return false;
		} else if (!objectType.equals(other.objectType))
			return false;
		if (indexType == null) {
			if (other.indexType != null)
				return false;
		} else if (!indexType.equals(other.indexType))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((indexType == null) ? 0 : indexType.hashCode());
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(objectType.toString());
		sb.append("[");
		sb.append(indexType.toString());
		sb.append("]");
		return sb.toString();
	}

	@Override
	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(objectType.toQualifiedString());
		sb.append("[");
		sb.append(indexType.toQualifiedString());
		sb.append("]");
		return sb.toString();
	}

	@Override
	public String getClassType() {
		return objectType.getClassType();
	}
}
