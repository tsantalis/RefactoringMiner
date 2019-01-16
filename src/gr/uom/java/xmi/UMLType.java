package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

public class UMLType implements Serializable {
    private String classType;
    private String nonQualifiedClassType;
    private String typeArguments;
    private int arrayDimension;
    private List<String> typeArgumentDecomposition;
    private volatile int hashCode = 0;
    private static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    private static final Pattern GENERIC_TYPE_SPLIT_PATTERN = Pattern.compile("<|>|,|\\s");

    public UMLType(String type) {
        this.classType = type;
        this.nonQualifiedClassType = simpleNameOf(type);
        this.typeArgumentDecomposition = new ArrayList<String>();
    }

    public String getClassType() {
        return classType;
    }

    public List<String> getTypeArguments() {
    	return typeArgumentDecomposition;
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

            if(equalClassType(typeObject)) {
                if(this.typeArguments == null && typeObject.typeArguments == null)
                    return this.arrayDimension == typeObject.arrayDimension;
                else if(this.typeArguments != null && typeObject.typeArguments != null)
                    return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
            }
        }
        return false;
    }

	public boolean equalsQualified(UMLType typeObject) {
		if(this.classType.equals(typeObject.classType)) {
            if(this.typeArguments == null && typeObject.typeArguments == null)
                return this.arrayDimension == typeObject.arrayDimension;
            else if(this.typeArguments != null && typeObject.typeArguments != null)
                return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
        }
		return false;
	}

    public boolean equalsWithSubType(UMLType typeObject) {
    	if(lastCamelCaseTokenMatch(this.nonQualifiedClassType, typeObject.nonQualifiedClassType)) {
            if(this.typeArguments == null && typeObject.typeArguments == null)
                return this.arrayDimension == typeObject.arrayDimension;
            else if(this.typeArguments != null && typeObject.typeArguments != null)
                return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
            else if(this.typeArguments != null && this.typeArguments.equals("<?>") && typeObject.typeArguments == null)
            	return this.arrayDimension == typeObject.arrayDimension;
            else if(this.typeArguments == null && typeObject.typeArguments != null && typeObject.typeArguments.equals("<?>"))
            	return this.arrayDimension == typeObject.arrayDimension;
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

	private boolean equalClassType(UMLType type) {
    	return this.nonQualifiedClassType.equals(type.nonQualifiedClassType);
	}

	private boolean equalTypeArguments(UMLType type) {
		if((this.typeArguments.equals("<?>") && type.typeArguments.startsWith("<? ")) || 
				(this.typeArguments.startsWith("<? ") && type.typeArguments.equals("<?>"))) {
			return true;
		}
		if(this.typeArgumentDecomposition.size() != type.typeArgumentDecomposition.size()) {
			return false;
		}
		for(int i=0; i<this.typeArgumentDecomposition.size(); i++) {
			String thisComponent = this.typeArgumentDecomposition.get(i);
			String otherComponent = type.typeArgumentDecomposition.get(i);
			if(!thisComponent.equals(otherComponent)) {
				return false;
			}
		}
		return true;
	}

	public boolean compatibleTypes(UMLType type) {
		return this.getClassType().equals(type.getClassType()) ||
				this.getClassType().equals("Object") ||
				type.getClassType().equals("Object") ||
				this.getClassType().startsWith(type.getClassType()) ||
				type.getClassType().startsWith(this.getClassType()) ||
				this.getClassType().endsWith(type.getClassType()) ||
				type.getClassType().endsWith(this.getClassType()) ||
				this.getTypeArguments().contains(type.getClassType()) ||
				type.getTypeArguments().contains(this.getClassType()) ||
				this.commonTokenInClassType(type);
	}

	private boolean commonTokenInClassType(UMLType type) {
		String[] tokens1 = CAMEL_CASE_SPLIT_PATTERN.split(this.nonQualifiedClassType);
		String[] tokens2 = CAMEL_CASE_SPLIT_PATTERN.split(type.nonQualifiedClassType);
		for(String token1 : tokens1) {
			for(String token2 : tokens2) {
				if(token1.equals(token2) || token1.equals(token2 + "s") || token2.equals(token1 + "s")) {
					return true;
				}
			}
		}
		return false;
	}

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + classType.hashCode();
    		if(typeArguments != null)
    			result = 37*result + typeArguments.hashCode();
    		result = 37*result + arrayDimension;
    		hashCode = result;
    	}
    	return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nonQualifiedClassType);
        if(typeArguments != null)
            sb.append(typeArguments);
        for(int i=0; i<arrayDimension; i++)
        	sb.append("[]");
        return sb.toString();
    }

	public static String getTypeName(Type type, int extraDimensions) {
		ITypeBinding binding = type.resolveBinding();
		if (binding != null) {
			return binding.getQualifiedName();
		}
		String typeToString = type.toString();
		for(int i=0; i<extraDimensions; i++) {
			typeToString += "[]";
		}
		return typeToString;
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
		String typeArguments = null;
		String[] typeArgumentDecomposition = {};
		if(qualifiedName.endsWith("[]")) {
			while(qualifiedName.endsWith("[]")) {
				qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
				arrayDimension++;
			}
		}
		if(qualifiedName.contains("<") && qualifiedName.contains(">")) {
			typeArguments = qualifiedName.substring(qualifiedName.indexOf("<"), qualifiedName.lastIndexOf(">")+1);
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
			typeArgumentDecomposition = GENERIC_TYPE_SPLIT_PATTERN.split(typeArguments);
		}
		UMLType typeObject = new UMLType(qualifiedName);
		typeObject.typeArguments = typeArguments;
		typeObject.arrayDimension = arrayDimension;
		for(String type : typeArgumentDecomposition) {
			if(!type.equals("")) {
				typeObject.typeArgumentDecomposition.add(simpleNameOf(type));
			}
		}
		return typeObject;
	}
}
