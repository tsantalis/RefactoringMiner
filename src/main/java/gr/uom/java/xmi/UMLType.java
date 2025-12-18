package gr.uom.java.xmi;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunctionType;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNullableType;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtTypeElement;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtUserType;
import org.refactoringminer.util.PathFileUtils;

import gr.uom.java.xmi.ListCompositeType.Kind;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLTypeParameterDiff;

public abstract class UMLType implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private int arrayDimension;
	private boolean parameterized;
	private boolean nullable;
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

	public boolean isNullable() {
		return nullable;
	}

	public String typeArgumentsToString() {
		boolean isPython = locationInfo != null && PathFileUtils.isPythonFile(locationInfo.getFilePath());
		String openingTag = isPython ? "[" : "<";
		String closingTag = isPython ? "]" : ">";
		StringBuilder sb = new StringBuilder();
		if(typeArguments.isEmpty()) {
			if(parameterized) {
				sb.append(openingTag + closingTag);
			}
		}
		else {
			sb.append(openingTag);
			for(int i = 0; i < typeArguments.size(); i++) {
				sb.append(typeArguments.get(i).toQualifiedString());
				if(i < typeArguments.size() - 1)
					sb.append(",");
			}
			sb.append(closingTag);
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
		else if(this.isParameterized() != typeObject.isParameterized())
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
	public boolean pluralClassType(UMLType type) {
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

	/**
	 * This method should be used only to generate fake types that do not correspond to an actual program element in the source code.
	 * In contrast to {@link #extractTypeObject(CompilationUnit, String, String, Type, int, String)}, this method does not create a LocationInfo.
	 * @param qualifiedName
	 * @return
	 */
	public static LeafType extractTypeObject(String qualifiedName) {
		String openingTag = "<";
		String closingTag = ">";
		return extractTypeObject(qualifiedName, openingTag, closingTag, null);
	}

	public static LeafType extractTypeObject(String qualifiedName, String openingTag, String closingTag, LocationInfo location) {
		int arrayDimension = 0;
		boolean parameterized = false;
		List<UMLType> typeArgumentDecomposition = new ArrayList<UMLType>();
		if(qualifiedName.endsWith("[]")) {
			while(qualifiedName.endsWith("[]")) {
				qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
				arrayDimension++;
			}
		}
		if(qualifiedName.contains(openingTag) && qualifiedName.contains(closingTag) &&
				!closingTagBeforeOpeningTag(qualifiedName.substring(qualifiedName.indexOf(openingTag)+1, qualifiedName.lastIndexOf(closingTag)), openingTag, closingTag)) {
			String typeArguments = qualifiedName.substring(qualifiedName.indexOf(openingTag)+1, qualifiedName.lastIndexOf(closingTag));
			parameterized = true;
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<typeArguments.length(); i++) {
				char charAt = typeArguments.charAt(i);
				if(charAt != ',') {
					sb.append(charAt);
				}
				else {
					if(sb.length() > 0 && equalOpeningClosingTags(sb.toString(), openingTag, closingTag)) {
						typeArgumentDecomposition.add(extractTypeObject(sb.toString(), openingTag, closingTag, location));
						sb = new StringBuilder();
					}
					else {
						sb.append(charAt);
					}
				}
			}
			if(sb.length() > 0) {
				typeArgumentDecomposition.add(extractTypeObject(sb.toString(), openingTag, closingTag, location));
			}
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf(openingTag));
		}
		UMLType typeObject = new LeafType(qualifiedName);
		typeObject.arrayDimension = arrayDimension;
		typeObject.typeArguments = typeArgumentDecomposition;
		typeObject.parameterized = parameterized;
		typeObject.locationInfo = location;
		return (LeafType)typeObject;
	}

	private static boolean closingTagBeforeOpeningTag(String typeArguments, String openingTag, String closingTag) {
		int indexOfOpeningTag = typeArguments.indexOf(openingTag);
		int indexOfClosingTag = typeArguments.lastIndexOf(closingTag);
		return indexOfClosingTag < indexOfOpeningTag;
	}

	private static boolean equalOpeningClosingTags(String typeArguments, String openingTag, String closingTag) {
		int openingTags = 0;
		int closingTags = 0;
		for(int i=0; i<typeArguments.length(); i++) {
			if(String.valueOf(typeArguments.charAt(i)).equals(openingTag)) {
				openingTags++;
			}
			else if(String.valueOf(typeArguments.charAt(i)).equals(closingTag)) {
				closingTags++;
			}
		}
		return openingTags == closingTags;
	}

	/**
	 * Use this method to generate a type for a Java program element present in the source code.
	 * @param cu
	 * @param sourceFolder
	 * @param filePath
	 * @param type
	 * @param extraDimensions
	 * @param javaFileContent
	 * @return
	 */
	public static UMLType extractTypeObject(CompilationUnit cu, String sourceFolder, String filePath, Type type, int extraDimensions, String javaFileContent) {
		UMLType umlType = extractTypeObject(cu, sourceFolder, filePath, type, javaFileContent);
		umlType.locationInfo = new LocationInfo(cu, sourceFolder, filePath, type, CodeElementType.TYPE);
		umlType.arrayDimension += extraDimensions;
		return umlType;
	}

	private static UMLType extractTypeObject(CompilationUnit cu, String sourceFolder, String filePath, Type type, String javaFileContent) {
		if(type.isPrimitiveType() || type.isSimpleType()) {
			LeafType leafType = extractTypeObject(stringify(type));
			AnnotatableType annotatableType = (AnnotatableType)type;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				leafType.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
			}
			return leafType;
		}
		else if(type instanceof QualifiedType) {
			QualifiedType qualified = (QualifiedType)type;
			UMLType leftType = extractTypeObject(cu, sourceFolder, filePath, qualified.getQualifier(), javaFileContent);
			LeafType rightType = extractTypeObject(qualified.getName().getFullyQualifiedName());
			AnnotatableType annotatableType = (AnnotatableType)qualified;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				rightType.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
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
				rightType.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
			}
			return new CompositeType(leftType, rightType);
		}
		else if(type instanceof WildcardType) {
			WildcardType wildcard = (WildcardType)type;
			gr.uom.java.xmi.WildcardType myWildcardType = null;
			if(wildcard.getBound() != null) {
				UMLType bound = extractTypeObject(cu, sourceFolder, filePath, wildcard.getBound(), javaFileContent);
				myWildcardType = new gr.uom.java.xmi.WildcardType(bound, wildcard.isUpperBound());
			}
			else {
				myWildcardType = new gr.uom.java.xmi.WildcardType(null, false);
			}
			AnnotatableType annotatableType = (AnnotatableType)wildcard;
			List<Annotation> annotations = annotatableType.annotations();
			for(Annotation annotation : annotations) {
				myWildcardType.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
			}
			return myWildcardType;
		}
		else if(type instanceof ArrayType) {
			ArrayType array = (ArrayType)type;
			UMLType arrayType = extractTypeObject(cu, sourceFolder, filePath, array.getElementType(), javaFileContent);
			for(Object dim : array.dimensions()) {
				Dimension dimension = (Dimension)dim;
				List<Annotation> annotations = dimension.annotations();
				for(Annotation annotation : annotations) {
					arrayType.annotations.add(new UMLAnnotation(cu, sourceFolder, filePath, annotation, javaFileContent));
				}
			}
			arrayType.arrayDimension = array.getDimensions();
			return arrayType;
		}
		else if(type instanceof ParameterizedType) {
			ParameterizedType parameterized = (ParameterizedType)type;
			UMLType container = extractTypeObject(cu, sourceFolder, filePath, parameterized.getType(), javaFileContent);
			container.parameterized = true;
			List<Type> typeArguments = parameterized.typeArguments();
			for(Type argument : typeArguments) {
				container.typeArguments.add(extractTypeObject(cu, sourceFolder, filePath, argument, javaFileContent));
			}
			return container;
		}
		else if(type instanceof UnionType) {
			UnionType union = (UnionType)type;
			List<Type> types = union.types();
			List<UMLType> umlTypes = new ArrayList<UMLType>();
			for(Type unionType : types) {
				umlTypes.add(extractTypeObject(cu, sourceFolder, filePath, unionType, javaFileContent));
			}
			return new ListCompositeType(umlTypes, Kind.UNION);
		}
		else if(type instanceof IntersectionType) {
			IntersectionType intersection = (IntersectionType)type;
			List<Type> types = intersection.types();
			List<UMLType> umlTypes = new ArrayList<UMLType>();
			for(Type unionType : types) {
				umlTypes.add(extractTypeObject(cu, sourceFolder, filePath, unionType, javaFileContent));
			}
			return new ListCompositeType(umlTypes, Kind.INTERSECTION);
		}
		return null;
	}

	public LeafExpression asLeafExpression() {
		return new LeafExpression(toQualifiedString(), getLocationInfo());
	}

	public boolean matchWithParentClassTypeParameterChange(UMLType otherType, Set<UMLTypeParameterDiff> set) {
		if(equalClassType(otherType)) {
			List<UMLType> thisTypeArguments = getTypeArguments();
			List<UMLType> otherTypeArguments = otherType.getTypeArguments();
			int matchingTypeArguments = 0;
			if(thisTypeArguments.size() == otherTypeArguments.size()) {
				for(int i=0; i<thisTypeArguments.size(); i++) {
					UMLType type1 = thisTypeArguments.get(i);
					UMLType type2 = otherTypeArguments.get(i);
					if(type1.equals(type2)) {
						matchingTypeArguments++;
					}
					else {
						for(UMLTypeParameterDiff diff : set) {
							if(type1.getClassType().equals(diff.getRemovedTypeParameter().getName()) &&
									type2.getClassType().equals(diff.getAddedTypeParameter().getName())) {
								matchingTypeArguments++;
								break;
							}
						}
					}
				}
				if(matchingTypeArguments == thisTypeArguments.size()) {
					return true;
				}
			}
		}
		return false;
	}

	public static UMLType extractTypeObject(KtFile ktFile, String sourceFolder, String filePath, String fileContent, KtElement type, int extraDimensions) {
		UMLType umlType = extractTypeObject(ktFile, sourceFolder, filePath, fileContent, type);
		if (!(umlType instanceof InferredType)) {
			umlType.locationInfo = new LocationInfo(ktFile, sourceFolder, filePath, type, CodeElementType.TYPE);
			umlType.arrayDimension += extraDimensions;
		}
		return umlType;
	}

	private static UMLType extractTypeObject(KtFile ktFile, String sourceFolder, String filePath, String fileContent, KtElement type) {
		if (type == null) {
			return new InferredType();
		} else if (type instanceof KtUserType userType) {
			KtUserType qualifier = userType.getQualifier();
			if (qualifier != null) {
				UMLType left = extractTypeObject(ktFile, sourceFolder, filePath, fileContent, qualifier);
				LeafType rightType = extractTypeObject(userType.getReferencedName());
				return new CompositeType(left, rightType);
			} else return extractTypeObject(userType.getReferencedName());
		} else if (type instanceof KtTypeReference typeReference) {
			KtTypeElement element = typeReference.getTypeElement();
			if (element instanceof KtFunctionType functionType) {
				KtTypeReference returnTypeReference = functionType.getReturnTypeReference();
				UMLType returnType = returnTypeReference == null ? null : extractTypeObject(ktFile, sourceFolder, filePath, fileContent, returnTypeReference);
				KtTypeReference receiverTypeReference = functionType.getReceiverTypeReference();
				UMLType receiverType = receiverTypeReference == null ? null : extractTypeObject(ktFile, sourceFolder, filePath, fileContent, receiverTypeReference);
				List<UMLType> parameterTypeList = new ArrayList<>();
				if (functionType.getParameterList() != null) {
					List<KtParameter> parameterList = functionType.getParameterList().getParameters();
					for (KtParameter ktParameter : parameterList) {
						KtTypeReference parameterTypeReference = ktParameter.getTypeReference();
						UMLType umlType = extractTypeObject(ktFile, sourceFolder, filePath, fileContent, parameterTypeReference);
						parameterTypeList.add(umlType);
					}
				}
				FunctionType umlType = new FunctionType(receiverType, returnType, parameterTypeList);
				processAnnotations(ktFile, sourceFolder, filePath, fileContent, typeReference, umlType);
				return umlType;
			} else if(element instanceof KtUserType userType) {
				UMLType umlType = extractTypeObject(ktFile, sourceFolder, filePath, fileContent, userType);
				processAnnotations(ktFile, sourceFolder, filePath, fileContent, typeReference, umlType);
				return umlType;
			} else if(element instanceof KtNullableType nullableType) {
				UMLType umlType = extractTypeObject(ktFile, sourceFolder, filePath, fileContent, nullableType.getInnerType());
				umlType.nullable = true;
				processAnnotations(ktFile, sourceFolder, filePath, fileContent, typeReference, umlType);
				return umlType;
			}
		}
		return null;
	}

	private static void processAnnotations(KtFile ktFile, String sourceFolder, String filePath, String fileContent, KtTypeReference typeReference, UMLType umlType) {
		KtModifierList modifierList = typeReference.getModifierList();
		if(modifierList != null) {
			for (PsiElement modifier : modifierList.getChildren()) {
				if (modifier instanceof KtAnnotationEntry annotationEntry) {
					umlType.annotations.add(new UMLAnnotation(ktFile, sourceFolder, filePath, annotationEntry, fileContent));
				}
			}
		}
	}
}
