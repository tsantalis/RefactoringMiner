package gr.uom.java.xmi;

public class UMLDataType {
	private String name;
    private String xmiID;
    
    public UMLDataType(String name, String xmiID) {
    	this.name = name;
    	this.xmiID = xmiID;
    }

	public String getName() {
		return name;
	}

	public String getXmiID() {
		return xmiID;
	}

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLDataType) {
    		UMLDataType umlDataType = (UMLDataType)o;
    		return this.name.equals(umlDataType.name);
    	}
    	return false;
    }

    public String toString() {
    	return name;
    }
}
