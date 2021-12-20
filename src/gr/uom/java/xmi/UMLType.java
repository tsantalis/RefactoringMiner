package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;

public abstract class UMLType implements Serializable, LocationInfoProvider {
	protected LocationInfo locationInfo;
	protected int arrayDimension;
	protected List<UMLType> typeArguments = new ArrayList<UMLType>();
	protected List<UMLAnnotation> annotations = new ArrayList<UMLAnnotation>();

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public int getArrayDimension() {
		return this.arrayDimension;
	}

	public List<UMLType> getTypeArguments() {
		return typeArguments;
	}

	public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public void setVarargs() {
		arrayDimension++;
	}

	protected String typeArgumentsToString() {
		StringBuilder sb = new StringBuilder();
		if(typeArguments.isEmpty()) {
			sb.append("");
		}
		else {
			sb.append("<");
			for(int i = 0; i < typeArguments.size(); i++) {
				sb.append(typeArguments.get(i).toQualifiedString());
				if(i < typeArguments.size() - 1)
					sb.append(",");
			}
			sb.append(">");
		}
		return sb.toString();
	}

	protected String typeArgumentsAndArrayDimensionToString() {
		StringBuilder sb = new StringBuilder();
		if(isParameterized())
			sb.append(typeArgumentsToString());
		for(int i=0; i<getArrayDimension(); i++)
			sb.append("[]");
		return sb.toString();
	}

	private boolean equalTypeArguments(UMLType type) {
		String thisTypeArguments = this.typeArgumentsToString();
		String otherTypeArguments = type.typeArgumentsToString();
		if((thisTypeArguments.equals("<?>") && otherTypeArguments.startsWith("<? ")) || 
				(thisTypeArguments.startsWith("<? ") && otherTypeArguments.equals("<?>"))) {
			return true;
		}
		if((thisTypeArguments.equals("<Object>") && otherTypeArguments.contains("<Object>")) ||
				(otherTypeArguments.equals("<Object>") && thisTypeArguments.contains("<Object>"))) {
			return true;
		}
		if(this.typeArguments.size() != type.typeArguments.size()) {
			return false;
		}
		for(int i=0; i<this.typeArguments.size(); i++) {
			UMLType thisComponent = this.typeArguments.get(i);
			UMLType otherComponent = type.typeArguments.get(i);
			if(!thisComponent.equals(otherComponent)) {
				return false;
			}
		}
		return true;
	}

	protected boolean equalTypeArgumentsAndArrayDimension(UMLType typeObject) {
		if(!this.isParameterized() && !typeObject.isParameterized())
			return this.arrayDimension == typeObject.arrayDimension;
		else if(this.isParameterized() && typeObject.isParameterized())
			return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
		return false;
	}

	protected boolean equalTypeArgumentsAndArrayDimensionForSubType(UMLType typeObject) {
		if(!this.isParameterized() && !typeObject.isParameterized())
			return this.arrayDimension == typeObject.arrayDimension;
		else if(this.isParameterized() && typeObject.isParameterized())
			return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
		else if(this.isParameterized() && this.typeArgumentsToString().equals("<?>") && !typeObject.isParameterized())
			return this.arrayDimension == typeObject.arrayDimension;
		else if(!this.isParameterized() && typeObject.isParameterized() && typeObject.typeArgumentsToString().equals("<?>"))
			return this.arrayDimension == typeObject.arrayDimension;
		return false;
	}

	public boolean containsTypeArgument(String type) {
		for(UMLType typeArgument : typeArguments) {
			if(typeArgument.toString().equals(type)) {
				return true;
			}
		}
		return false;
	}

	public boolean isParameterized() {
		return typeArguments.size() > 0;
	}

	public abstract boolean equals(Object o);
	public abstract int hashCode();
	public abstract String toString();
	public abstract String toQualifiedString();
	public abstract String getClassType();

	public boolean equalsQualified(UMLType type) {
		if(this.getClass() == type.getClass()) {
			return this.equals(type);
		}
		return false;
	}
	public boolean equalsWithSubType(UMLType type) {
		if(this.getClass() == type.getClass()) {
			return this.equals(type);
		}
		return false;
	}
	public boolean equalClassType(UMLType type) {
		if(this.getClass() == type.getClass()) {
			return this.equals(type);
		}
		return false;
	}
	public boolean compatibleTypes(UMLType type) {
		if(this.getClass() == type.getClass()) {
			return this.equals(type);
		}
		return false;
	}

	public double normalizedNameDistance(UMLType type) {
		String s1 = this.toString();
		String s2 = type.toString();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public static LeafType extractTypeObject(String qualifiedName) {
		int arrayDimension = 0;
		List<UMLType> typeArgumentDecomposition = new ArrayList<UMLType>();
		if(qualifiedName.endsWith("[]")) {
			while(qualifiedName.endsWith("[]")) {
				qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
				arrayDimension++;
			}
		}
		if(qualifiedName.contains("<") && qualifiedName.contains(">") &&
				!closingTagBeforeOpeningTag(qualifiedName.substring(qualifiedName.indexOf("<")+1, qualifiedName.lastIndexOf(">")))) {
			String typeArguments = qualifiedName.substring(qualifiedName.indexOf("<")+1, qualifiedName.lastIndexOf(">"));
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<typeArguments.length(); i++) {
				char charAt = typeArguments.charAt(i);
				if(charAt != ',') {
					sb.append(charAt);
				}
				else {
					if(sb.length() > 0 && equalOpeningClosingTags(sb.toString())) {
						typeArgumentDecomposition.add(extractTypeObject(sb.toString()));
						sb = new StringBuilder();
					}
					else {
						sb.append(charAt);
					}
				}
			}
			if(sb.length() > 0) {
				typeArgumentDecomposition.add(extractTypeObject(sb.toString()));
			}
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
		}
		UMLType typeObject = new LeafType(qualifiedName);
		typeObject.arrayDimension = arrayDimension;
		typeObject.typeArguments = typeArgumentDecomposition;
		return (LeafType)typeObject;
	}

	private static boolean closingTagBeforeOpeningTag(String typeArguments) {
		int indexOfOpeningTag = typeArguments.indexOf("<");
		int indexOfClosingTag = typeArguments.lastIndexOf(">");
		return indexOfClosingTag < indexOfOpeningTag;
	}

	private static boolean equalOpeningClosingTags(String typeArguments) {
		int openingTags = 0;
		int closingTags = 0;
		for(int i=0; i<typeArguments.length(); i++) {
			if(typeArguments.charAt(i) == '>') {
				openingTags++;
			}
			else if(typeArguments.charAt(i) == '<') {
				closingTags++;
			}
		}
		return openingTags == closingTags;
	}
}
