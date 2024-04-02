package gr.uom.java.xmi;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.WildcardType;

import gr.uom.java.xmi.ListCompositeType.Kind;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;

public abstract class UMLType implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private int arrayDimension;
	private boolean parameterized;
	private List<UMLType> typeArguments = new ArrayList<UMLType>();
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
			if(parameterized) {
				sb.append("<>");
			}
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
		for(int i=0; i<getArrayDimension(); i++) {
			for(int j=0; j<annotations.size(); j++) {
				if(i == j) {
					sb.append(" " + annotations.get(j) + " ");
				}
			}
			sb.append("[]");
		}
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
			return this.arrayDimension == typeObject.arrayDimension && this.annotations.equals(typeObject.annotations);
		else if(this.isParameterized() && typeObject.isParameterized())
			return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension && this.annotations.equals(typeObject.annotations);
		return false;
	}

	protected boolean equalTypeArgumentsAndArrayDimensionForSubType(UMLType typeObject) {
		if(!this.isParameterized() && !typeObject.isParameterized())
			return this.arrayDimension == typeObject.arrayDimension && this.annotations.equals(typeObject.annotations);
		else if(this.isParameterized() && typeObject.isParameterized())
			return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension && this.annotations.equals(typeObject.annotations);
		else if(this.isParameterized() && this.typeArgumentsToString().equals("<?>") && !typeObject.isParameterized())
			return this.arrayDimension == typeObject.arrayDimension && this.annotations.equals(typeObject.annotations);
		else if(!this.isParameterized() && typeObject.isParameterized() && typeObject.typeArgumentsToString().equals("<?>"))
			return this.arrayDimension == typeObject.arrayDimension && this.annotations.equals(typeObject.annotations);
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
		return typeArguments.size() > 0 || parameterized;
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
		boolean parameterized = false;
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
			parameterized = true;
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
		typeObject.parameterized = parameterized;
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

	public static UMLType extractTypeObject(CompilationUnit cu, String filePath, Type type, int extraDimensions) {
		UMLType umlType = extractTypeObject(cu, filePath, type);
		umlType.locationInfo = new LocationInfo(cu, filePath, type, CodeElementType.TYPE);
		umlType.arrayDimension += extraDimensions;
		return umlType;
	}

	private static UMLType extractTypeObject(CompilationUnit cu, String filePath, Type type) {
		if(type.isPrimitiveType() || type.isSimpleType()) {
			LeafType leafType = extractTypeObject(stringify(type));
			AnnotatableType annotatableType = (AnnotatableType)type;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				leafType.annotations.add(new UMLAnnotation(cu, filePath, annotation));
			}
			return leafType;
		}
		else if(type instanceof QualifiedType) {
			QualifiedType qualified = (QualifiedType)type;
			UMLType leftType = extractTypeObject(cu, filePath, qualified.getQualifier());
			LeafType rightType = extractTypeObject(qualified.getName().getFullyQualifiedName());
			AnnotatableType annotatableType = (AnnotatableType)qualified;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				rightType.annotations.add(new UMLAnnotation(cu, filePath, annotation));
			}
			return new CompositeType(leftType, rightType);
		}
		else if(type instanceof NameQualifiedType) {
			NameQualifiedType nameQualified = (NameQualifiedType)type;
			LeafType leftType = extractTypeObject(nameQualified.getQualifier().getFullyQualifiedName());
			LeafType rightType = extractTypeObject(nameQualified.getName().getFullyQualifiedName());
			AnnotatableType annotatableType = (AnnotatableType)nameQualified;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				rightType.annotations.add(new UMLAnnotation(cu, filePath, annotation));
			}
			return new CompositeType(leftType, rightType);
		}
		else if(type instanceof WildcardType) {
			WildcardType wildcard = (WildcardType)type;
			gr.uom.java.xmi.WildcardType myWildcardType = null;
			if(wildcard.getBound() != null) {
				UMLType bound = extractTypeObject(cu, filePath, wildcard.getBound());
				myWildcardType = new gr.uom.java.xmi.WildcardType(bound, wildcard.isUpperBound());
			}
			else {
				myWildcardType = new gr.uom.java.xmi.WildcardType(null, false);
			}
			AnnotatableType annotatableType = (AnnotatableType)wildcard;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				myWildcardType.annotations.add(new UMLAnnotation(cu, filePath, annotation));
			}
			return myWildcardType;
		}
		else if(type instanceof ArrayType) {
			ArrayType array = (ArrayType)type;
			UMLType arrayType = extractTypeObject(cu, filePath, array.getElementType());
			for(Object dim : array.dimensions()) {
				Dimension dimension = (Dimension)dim;
				List<Annotation> annotations = dimension.annotations();
				for(Annotation annotation : annotations) {
					arrayType.annotations.add(new UMLAnnotation(cu, filePath, annotation));
				}
			}
			arrayType.arrayDimension = array.getDimensions();
			return arrayType;
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterized = (ParameterizedType)type;
			UMLType container = extractTypeObject(cu, filePath, parameterized.getType());
			container.parameterized = true;
			List<Type> typeArguments = parameterized.typeArguments();
			for(Type argument : typeArguments) {
				container.typeArguments.add(extractTypeObject(cu, filePath, argument));
			}
			return container;
		}
		else if(type instanceof UnionType) {
			UnionType union = (UnionType)type;
			List<Type> types = union.types();
			List<UMLType> umlTypes = new ArrayList<UMLType>();
			for(Type unionType : types) {
				umlTypes.add(extractTypeObject(cu, filePath, unionType));
			}
			return new ListCompositeType(umlTypes, Kind.UNION);
		}
		else if(type instanceof IntersectionType) {
			IntersectionType intersection = (IntersectionType)type;
			List<Type> types = intersection.types();
			List<UMLType> umlTypes = new ArrayList<UMLType>();
			for(Type unionType : types) {
				umlTypes.add(extractTypeObject(cu, filePath, unionType));
			}
			return new ListCompositeType(umlTypes, Kind.INTERSECTION);
		}
		return null;
	}

	public LeafExpression asLeafExpression() {
		return new LeafExpression(toQualifiedString(), getLocationInfo());
	}
}
