package gr.uom.java.xmi;

public class CompositeType extends UMLType {
	private UMLType leftType;
	private LeafType rightType;

	public CompositeType(UMLType leftType, LeafType rightType) {
		this.leftType = leftType;
		this.rightType = rightType;
	}

	public UMLType getLeftType() {
		return leftType;
	}

	public LeafType getRightType() {
		return rightType;
	}

	//the hash code is computed as in LeafType, so that the CompositeType has the same hash code with an equal LeafType of another language (see LeafType.equalsCompositeTypeInOtherLanguage)
	@Override
	public int hashCode() {
		int result = 17;
		result = 37*result + qualifiedClassType().hashCode();
		if(rightType != null && rightType.isParameterized())
			result = 37*result + rightType.typeArgumentsToString().hashCode();
		result = 37*result + arrayDimensionIncludingRightType();
		return result;
	}

	//the dot-separated qualified name of the type, without the type arguments of the right type, i.e., MockResponse.Builder
	String qualifiedClassType() {
		return (leftType == null ? "" : leftType.toQualifiedString() + ".") + (rightType == null ? "" : rightType.getClassType());
	}

	int arrayDimensionIncludingRightType() {
		return getArrayDimension() + (rightType == null ? 0 : rightType.getArrayDimension());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof LeafType leafType)
			return leafType.equalsCompositeTypeInOtherLanguage(this);
		if (getClass() != obj.getClass())
			return false;
		CompositeType other = (CompositeType) obj;
		if (leftType == null) {
			if (other.leftType != null)
				return false;
		} else if (!leftType.equals(other.leftType))
			return false;
		if (rightType == null) {
			if (other.rightType != null)
				return false;
		} else if (!rightType.equals(other.rightType))
			return false;
		return true;
	}

	@Override
	public boolean equalsQualified(UMLType type) {
		if (type instanceof LeafType leafType)
			return leafType.equalsCompositeTypeInOtherLanguage(this);
		return super.equalsQualified(type);
	}

	@Override
	public String toString() {
		return leftType.toString() + "." + rightType.toString();
	}

	@Override
	public String toQualifiedString() {
		return leftType.toQualifiedString() + "." + rightType.toQualifiedString();
	}

	@Override
	public String getClassType() {
		return rightType.getClassType();
	}
}
