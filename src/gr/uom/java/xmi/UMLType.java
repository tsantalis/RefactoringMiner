package gr.uom.java.xmi;

import java.io.Serializable;

public class UMLType implements Serializable {
    private String classType;
    private String genericType;
    private int arrayDimension;
    private volatile int hashCode = 0;

    public UMLType(String type) {
        this.classType = type;
    }

    public String getClassType() {
        return classType;
    }

    public String getGenericType() {
        return genericType;
    }

    public void setGeneric(String g) {
        this.genericType = g;
    }
    
    public void setArrayDimension(int dimension) {
    	this.arrayDimension = dimension;
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

	private boolean lastCamelCaseTokenMatch(String classType1, String classType2) {
		String regex = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";
		String[] tokens1 = classType1.split(regex);
		String[] tokens2 = classType2.split(regex);
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
		return this.genericType.equals(typeObject.genericType);
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

    private String simpleNameOf(String name) {
    	int numberOfDots = 0;
    	for (int i = 0; i < name.length(); i++) {
    	    if (name.charAt(i) == '.') {
    	        numberOfDots++;
    	    }
    	}
    	if(numberOfDots > 1) {
    		int dotPosition = name.lastIndexOf('.');
    		return name.substring(dotPosition + 1);
    	}
    	return name;
	}

	public static UMLType extractTypeObject(String qualifiedName) {
		int arrayDimension = 0;
		String generic = null;
		if(qualifiedName.endsWith("[]")) {
			while(qualifiedName.endsWith("[]")) {
				qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf("[]"));
				arrayDimension++;
			}
		}
		if(qualifiedName.contains("<") && qualifiedName.contains(">")) {
			generic = qualifiedName.substring(qualifiedName.indexOf("<"), qualifiedName.lastIndexOf(">")+1);
			qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
		}
		UMLType typeObject = new UMLType(qualifiedName);
		typeObject.setGeneric(generic);
		typeObject.setArrayDimension(arrayDimension);
		return typeObject;
	}
}
