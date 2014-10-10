package gr.uom.java.xmi;

public class FieldAccess extends AccessedMember {
    private String ownerClassName;
    private String classType;
    private String fieldName;
    private volatile int hashCode = 0;

    public FieldAccess(String ownerClassName, String classType, String name) {
        this.ownerClassName = ownerClassName;
        this.classType = classType;
        this.fieldName = name;
    }

    public boolean matchesAttribute(UMLAttribute attribute) {
    	return this.ownerClassName.equals(attribute.getClassName()) &&
    		this.fieldName.equals(attribute.getName()) &&
    		this.classType.equals(attribute.getType().getClassType());
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof FieldAccess) {
            FieldAccess fieldAccess = (FieldAccess)o;
            return this.ownerClassName.equals(fieldAccess.ownerClassName) &&
            this.fieldName.equals(fieldAccess.fieldName) &&
            this.classType.equals(fieldAccess.classType);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ownerClassName).append(".");
        sb.append(fieldName);
        sb.append(" : ").append(classType);
        return sb.toString();
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + ownerClassName.hashCode();
    		result = 37*result + classType.hashCode();
    		result = 37*result + fieldName.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }
}
