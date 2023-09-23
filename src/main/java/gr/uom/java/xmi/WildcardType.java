package gr.uom.java.xmi;

public class WildcardType extends UMLType {
	private UMLType bound;
	private boolean upperBound;
	
	public WildcardType(UMLType bound, boolean upperBound) {
		this.bound = bound;
		this.upperBound = upperBound;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bound == null) ? 0 : bound.hashCode());
		result = prime * result + (upperBound ? 1231 : 1237);
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
		WildcardType other = (WildcardType) obj;
		if (bound == null) {
			if (other.bound != null)
				return false;
		} else if (!bound.equals(other.bound))
			return false;
		if (upperBound != other.upperBound)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("?");
		if(bound != null) {
			if(upperBound)
				sb.append(" extends ");
			else
				sb.append(" super ");
			sb.append(bound.toString());
		}
		return sb.toString();
	}

	@Override
	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		sb.append("?");
		if(bound != null) {
			if(upperBound)
				sb.append(" extends ");
			else
				sb.append(" super ");
			sb.append(bound.toQualifiedString());
		}
		return sb.toString();
	}

	@Override
	public String getClassType() {
		if(bound != null) {
			return bound.getClassType();
		}
		return "Object";
	}
}
