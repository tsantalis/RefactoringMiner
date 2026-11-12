package gr.uom.java.xmi;

//a TypeScript conditional type, i.e., C extends Check<infer T> ? T : never
public class ConditionalType extends UMLType {
	private UMLType checkType;
	private UMLType extendsType;
	private UMLType trueType;
	private UMLType falseType;

	public ConditionalType(UMLType checkType, UMLType extendsType, UMLType trueType, UMLType falseType) {
		this.checkType = checkType;
		this.extendsType = extendsType;
		this.trueType = trueType;
		this.falseType = falseType;
	}

	public UMLType getCheckType() {
		return checkType;
	}

	public UMLType getExtendsType() {
		return extendsType;
	}

	public UMLType getTrueType() {
		return trueType;
	}

	public UMLType getFalseType() {
		return falseType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConditionalType other = (ConditionalType) obj;
		return checkType.equals(other.checkType) && extendsType.equals(other.extendsType) &&
				trueType.equals(other.trueType) && falseType.equals(other.falseType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + checkType.hashCode();
		result = prime * result + extendsType.hashCode();
		result = prime * result + trueType.hashCode();
		result = prime * result + falseType.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return checkType.toString() + " extends " + extendsType.toString() + " ? " + trueType.toString() + " : " + falseType.toString();
	}

	@Override
	public String toQualifiedString() {
		return checkType.toQualifiedString() + " extends " + extendsType.toQualifiedString() + " ? " + trueType.toQualifiedString() + " : " + falseType.toQualifiedString();
	}

	@Override
	public String getClassType() {
		return checkType.getClassType();
	}
}
