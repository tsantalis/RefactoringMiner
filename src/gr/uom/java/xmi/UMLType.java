package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UMLType implements Serializable {
    private String classType;
    private String genericType;
    private int arrayDimension;
    private List<String> genericTypeDecomposition;
    private volatile int hashCode = 0;
    private static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    private static final Pattern GENERIC_TYPE_SPLIT_PATTERN = Pattern.compile("<|>|,|\\s");

    public UMLType(String type) {
        this.classType = type;
        this.genericTypeDecomposition = new ArrayList<String>();
    }

    public String getClassType() {
        return classType;
    }

    public String getGenericType() {
        return genericType;
    }

    public int getArrayDimension() {
    	return this.arrayDimension;
    }

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof UMLType) {
        	UMLType typeObject = (UMLType)o;

            if(this.classType.equals(typeObject.classType)) {
                if(this.genericType == null && typeObject.genericType == null)
                    return this.arrayDimension == typeObject.arrayDimension;
                else if(this.genericType != null && typeObject.genericType != null)
                    return equalGenericType(typeObject) && this.arrayDimension == typeObject.arrayDimension;
            }
        }
        return false;
    }

    public boolean equalsWithSubType(UMLType typeObject) {
    	if(lastCamelCaseTokenMatch(this.classType, typeObject.classType)) {
            if(this.genericType == null && typeObject.genericType == null)
                return this.arrayDimension == typeObject.arrayDimension;
            else if(this.genericType != null && typeObject.genericType != null)
                return equalGenericType(typeObject) && this.arrayDimension == typeObject.arrayDimension;
        }
    	return false;
    }

	private static boolean lastCamelCaseTokenMatch(String classType1, String classType2) {
		String[] tokens1 = CAMEL_CASE_SPLIT_PATTERN.split(classType1);
		String[] tokens2 = CAMEL_CASE_SPLIT_PATTERN.split(classType2);
		if(tokens1.length > 0 && tokens2.length > 0) {
			return tokens1[tokens1.length-1].equals(tokens2[tokens2.length-1]);
		}
		return false;
	}

	private boolean equalGenericType(UMLType typeObject) {
		if((this.genericType.equals("<?>") && typeObject.genericType.startsWith("<? ")) || 
				(this.genericType.startsWith("<? ") && typeObject.genericType.equals("<?>"))) {
			return true;
		}
		if(this.genericTypeDecomposition.size() != typeObject.genericTypeDecomposition.size()) {
			return false;
		}
		for(int i=0; i<this.genericTypeDecomposition.size(); i++) {
			String thisComponent = this.genericTypeDecomposition.get(i);
			String otherComponent = typeObject.genericTypeDecomposition.get(i);
			int thisLength = thisComponent.length();
			int otherLength = otherComponent.length();
			if(thisLength > 1 && otherLength > 1 && !thisComponent.equals(otherComponent)) {
				return false;
			}
			if(thisLength == 1 && otherLength > 1) {
				return false;
			}
			if(thisLength > 1 && otherLength == 1) {
				return false;
			}
			//type parameter names, such as E, K, N, T, V, S, U, having a length of 1 are always equal
		}
		return true;
	}

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + classType.hashCode();
    		if(genericType != null)
    			result = 37*result + genericType.hashCode();
    		result = 37*result + arrayDimension;
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(simpleNameOf(classType));
        if(genericType != null)
            sb.append(genericType);
        for(int i=0; i<arrayDimension; i++)
        	sb.append("[]");
        return sb.toString();
    }

    private static String simpleNameOf(String name) {
    	int numberOfDots = 0;
    	for (int i = 0; i < name.length(); i++) {
    		if (name.charAt(i) == '.') {
    			numberOfDots++;
    		}
    	}
    	if(numberOfDots > 2) {
    		int dotPosition = name.lastIndexOf('.');
    		return name.substring(dotPosition + 1);
    	}
    	return name;
	}

	public static UMLType extractTypeObject(String qualifiedName) {
		int arrayDimension = 0;
		String generic = null;
		String[] genericTypeDecomposition = {};
		if(qualifiedName.endsWith("[]")) {
			while(qualifiedName.endsWith("[]")) {
				qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
				arrayDimension++;
			}
		}
		if(qualifiedName.contains("<") && qualifiedName.contains(">")) {
			generic = qualifiedName.substring(qualifiedName.indexOf("<"), qualifiedName.lastIndexOf(">")+1);
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
			genericTypeDecomposition = GENERIC_TYPE_SPLIT_PATTERN.split(generic);
		}
		UMLType typeObject = new UMLType(qualifiedName);
		typeObject.genericType = generic;
		typeObject.arrayDimension = arrayDimension;
		for(String type : genericTypeDecomposition) {
			typeObject.genericTypeDecomposition.add(simpleNameOf(type));
		}
		return typeObject;
	}
}
