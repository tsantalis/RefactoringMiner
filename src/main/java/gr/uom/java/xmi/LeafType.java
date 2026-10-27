package gr.uom.java.xmi;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class LeafType extends UMLType implements Cloneable {
	private String classType;
	private String nonQualifiedClassType;
	private volatile int hashCode = 0;
	public static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
	private final Constants LANG;
	
	public LeafType(String type, Constants LANG) {
		this.classType = type;
		this.LANG = LANG;
		if(type.equals("..."))
			this.nonQualifiedClassType = type;
		else
			this.nonQualifiedClassType = simpleNameOf(type);
	}

	public LeafType cloneAsQualified() {
		try {
			LeafType type = (LeafType)super.clone();
			type.nonQualifiedClassType = type.classType;
			return type;
		} catch (CloneNotSupportedException e) {
			return this;
		}
	}

	@Override
	public String getClassType() {
		return classType;
	}

	private static String simpleNameOf(String name) {
		int numberOfDots = 0;
		int indexOfFirstUpperCaseCharacterFollowedByDot = -1;
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) == '.') {
				numberOfDots++;
				if(i+1 < name.length() && Character.isUpperCase(name.charAt(i+1)) &&
						indexOfFirstUpperCaseCharacterFollowedByDot == -1) {
					indexOfFirstUpperCaseCharacterFollowedByDot = i+1;
				}
			}
		}
		if(numberOfDots == 0 || Character.isUpperCase(name.charAt(0))) {
			return name;
		}
		if(numberOfDots > 1 && indexOfFirstUpperCaseCharacterFollowedByDot != -1) {
			return name.substring(indexOfFirstUpperCaseCharacterFollowedByDot);
		}
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}

		if (o instanceof LeafType) {
			LeafType typeObject = (LeafType)o;

			if(equalClassType(typeObject) && equalTypeArgumentsAndArrayDimension(typeObject)) {
				return true;
			}
			if(arrayInOtherLanguage(typeObject, false)) {
				return true;
			}
		}
		if (o instanceof CompositeType compositeType) {
			return equalsCompositeTypeInOtherLanguage(compositeType);
		}
		return false;
	}

	//the Java qualified type is a LeafType, while the Kotlin qualified type is a CompositeType, i.e., MockResponse.Builder is LeafType in Java and CompositeType(MockResponse, Builder) in Kotlin
	//the types are equal, if they have identical qualified names, type arguments, and array dimension, and are from different languages
	//the types within the same language are never compared as LeafType and CompositeType, since they are parsed in the same way
	boolean equalsCompositeTypeInOtherLanguage(CompositeType compositeType) {
		LeafType rightType = compositeType.getRightType();
		if(rightType == null || this.LANG.equals(rightType.LANG))
			return false;
		return this.classType.equals(compositeType.qualifiedClassType()) &&
				this.typeArgumentsToString().equals(rightType.typeArgumentsToString()) &&
				this.getArrayDimension() == compositeType.arrayDimensionIncludingRightType();
	}

	@Override
	public boolean equalsQualified(UMLType type) {
		if(this.getClass() == type.getClass()) {
			LeafType leafType = (LeafType)type;
			if(this.classType.equals(leafType.classType) && equalTypeArgumentsAndArrayDimension(type)) {
				return true;
			}
			if(!this.LANG.equals(leafType.LANG) && (this.classType.toLowerCase().equals(leafType.classType.toLowerCase()) || rootTypeInOtherLanguage(leafType)) && equalTypeArgumentsAndArrayDimension(type)) {
				return true;
			}
			if(arrayInOtherLanguage(leafType, true)) {
				return true;
			}
			if(equalsTypeScript(type)) {
				return true;
			}
		}
		if(type instanceof CompositeType compositeType) {
			return equalsCompositeTypeInOtherLanguage(compositeType);
		}
		return false;
	}

	private boolean equalsTypeScript(UMLType type) {
		// https://www.typescriptlang.org/docs/handbook/utility-types.html
		// handle Extract and Exclude utility types in TypeScript
		if(type instanceof LeafType leafType) {
			if(this.classType.equals("Extract") && !leafType.classType.equals("Extract")) {
				if(this.getTypeArguments().size() > 0) {
					if(this.getTypeArguments().get(0).equals(type)) {
						return true;
					}
				}
			}
			else if(!this.classType.equals("Extract") && leafType.classType.equals("Extract")) {
				if(type.getTypeArguments().size() > 0) {
					if(type.getTypeArguments().get(0).equals(this)) {
						return true;
					}
				}
			}
			if(this.classType.equals("Exclude") && !leafType.classType.equals("Exclude")) {
				if(this.getTypeArguments().size() > 0) {
					if(this.getTypeArguments().get(0).equals(type)) {
						return true;
					}
				}
			}
			else if(!this.classType.equals("Exclude") && leafType.classType.equals("Exclude")) {
				if(type.getTypeArguments().size() > 0) {
					if(type.getTypeArguments().get(0).equals(this)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean equalsWithSubType(UMLType type) {
		if(this.getClass() == type.getClass()) {
			if(firstOrLastCamelCaseTokenMatch(this.nonQualifiedClassType, ((LeafType)type).nonQualifiedClassType) && equalTypeArgumentsAndArrayDimensionForSubType(type)) {
				return true;
			}
		}
		return false;
	}

	private static boolean firstOrLastCamelCaseTokenMatch(String classType1, String classType2) {
		String[] tokens1 = CAMEL_CASE_SPLIT_PATTERN.split(classType1);
		String[] tokens2 = CAMEL_CASE_SPLIT_PATTERN.split(classType2);
		if(tokens1.length > 0 && tokens2.length > 0) {
			return tokens1[0].equals(tokens2[0]) || tokens1[tokens1.length-1].equals(tokens2[tokens2.length-1]);
		}
		return false;
	}

	@Override
	public boolean equalClassType(UMLType type) {
		if(this.getClass() == type.getClass()) {
			LeafType leafType = (LeafType)type;
			if(!this.LANG.equals(leafType.LANG) && (this.nonQualifiedClassType.toLowerCase().equals(leafType.nonQualifiedClassType.toLowerCase()) || rootTypeInOtherLanguage(leafType)))
				return true;
			return this.nonQualifiedClassType.equals(leafType.nonQualifiedClassType);
		}
		if(equalsTypeScript(type)) {
			return true;
		}
		return false;
	}

	//the Kotlin classes of the Java arrays of primitive types
	private static final java.util.Map<String, String> KOTLIN_PRIMITIVE_ARRAYS = java.util.Map.of(
			"boolean", "BooleanArray", "byte", "ByteArray", "char", "CharArray", "short", "ShortArray",
			"int", "IntArray", "long", "LongArray", "float", "FloatArray", "double", "DoubleArray");

	//a Java array is a Kotlin Array of its element type, or a Kotlin primitive array, i.e., Http2Stream[] -> Array<Http2Stream>, byte[] -> ByteArray,
	//with the element types compared by their qualified names, when qualified
	private boolean arrayInOtherLanguage(LeafType leafType, boolean qualified) {
		if(this.LANG.equals(leafType.LANG))
			return false;
		return (this.getArrayDimension() > 0 && javaArrayInKotlin(this, this.getArrayDimension(), leafType, qualified)) ||
				(leafType.getArrayDimension() > 0 && javaArrayInKotlin(leafType, leafType.getArrayDimension(), this, qualified));
	}

	//whether the Kotlin type is the Java array with the given number of dimensions, i.e., the Java int[][] is the Kotlin Array<IntArray>
	private static boolean javaArrayInKotlin(LeafType java, int dimensions, UMLType kotlin, boolean qualified) {
		if(!(kotlin instanceof LeafType kotlinType) || kotlinType.getArrayDimension() != 0)
			return false;
		if(dimensions == 0) {
			boolean equalClassType = qualified ?
					java.classType.toLowerCase().equals(kotlinType.classType.toLowerCase()) || java.rootTypeInOtherLanguage(kotlinType) :
					java.equalClassType(kotlinType);
			return equalClassType && java.typeArgumentsToString().equals(kotlinType.typeArgumentsToString());
		}
		if(dimensions == 1 && kotlinType.nonQualifiedClassType.equals(KOTLIN_PRIMITIVE_ARRAYS.get(java.nonQualifiedClassType)))
			return true;
		if(!kotlinType.nonQualifiedClassType.equals("Array") || kotlinType.getTypeArguments().size() != 1)
			return false;
		return javaArrayInKotlin(java, dimensions - 1, kotlinType.getTypeArguments().get(0), qualified);
	}

	//Kotlin Any is the root of the class hierarchy, like Java Object
	private boolean rootTypeInOtherLanguage(LeafType leafType) {
		return !this.LANG.equals(leafType.LANG) &&
				((this.nonQualifiedClassType.equals("Object") && leafType.nonQualifiedClassType.equals("Any")) ||
				(this.nonQualifiedClassType.equals("Any") && leafType.nonQualifiedClassType.equals("Object")));
	}

	@Override
	public boolean compatibleTypes(UMLType type) {
		if(this.getClass() == type.getClass()) {
			LeafType leafType = (LeafType)type;
			return this.getClassType().equals(leafType.getClassType()) ||
					this.getClassType().equals("Object") ||
					leafType.getClassType().equals("Object") ||
					this.getClassType().equals("Any") ||
					leafType.getClassType().equals("Any") ||
					this.getClassType().startsWith(leafType.getClassType()) ||
					leafType.getClassType().startsWith(this.getClassType()) ||
					this.getClassType().endsWith(leafType.getClassType()) ||
					leafType.getClassType().endsWith(this.getClassType()) ||
					this.containsTypeArgument(leafType.getClassType()) ||
					leafType.containsTypeArgument(this.getClassType()) ||
					this.commonTokenInClassType(leafType) ||
					(this.getClassType().equals("List") && leafType.getClassType().equals("Set") && this.getTypeArguments().equals(leafType.getTypeArguments())) ||
					(this.getClassType().equals("ArrayList") && leafType.getClassType().equals("LinkedHashSet") && this.getTypeArguments().equals(leafType.getTypeArguments()));
		}
		return false;
	}

	public boolean pluralClassType(UMLType type) {
		if(this.getClass() == type.getClass()) {
			LeafType leafType = (LeafType)type;
			return this.nonQualifiedClassType.equals(leafType.nonQualifiedClassType + "s") || leafType.nonQualifiedClassType.equals(this.nonQualifiedClassType + "s");
		}
		return false;
	}

	private boolean commonTokenInClassType(LeafType type) {
		Set<String> excludedTokens = new HashSet<>();
		excludedTokens.add("Psi");
		String[] tokens1 = CAMEL_CASE_SPLIT_PATTERN.split(this.nonQualifiedClassType);
		String[] tokens2 = CAMEL_CASE_SPLIT_PATTERN.split(type.nonQualifiedClassType);
		for(String token1 : tokens1) {
			if(!excludedTokens.contains(token1)) {
				for(String token2 : tokens2) {
					if((token1.equals(token2) && token1.length() > 1) || token1.equals(token2 + "s") || token2.equals(token1 + "s")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = 37*result + classType.hashCode();
			if(isParameterized())
				result = 37*result + typeArgumentsToString().hashCode();
			result = 37*result + getArrayDimension();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nonQualifiedClassType);
		sb.append(typeArgumentsAndArrayDimensionToString());
		return sb.toString();
	}

	@Override
	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(classType);
		sb.append(typeArgumentsAndArrayDimensionToString());
		return sb.toString();
	}
}
