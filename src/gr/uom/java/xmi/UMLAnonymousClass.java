package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UMLAnonymousClass implements Comparable<UMLAnonymousClass>, Serializable, LocationInfoProvider {
	private String packageName;
    private String name;
    private LocationInfo locationInfo;
    
    private List<UMLOperation> operations;
    private List<UMLAttribute> attributes;
    
    public UMLAnonymousClass(String packageName, String name, LocationInfo locationInfo) {
    	this.packageName = packageName;
        this.name = name;
        this.operations = new ArrayList<UMLOperation>();
        this.attributes = new ArrayList<UMLAttribute>();
        this.locationInfo = locationInfo;
    }

    public LocationInfo getLocationInfo() {
		return locationInfo;
	}

    public String getName() {
    	if(packageName.equals(""))
    		return name;
    	else
    		return packageName + "." + name;
    }

	public void addOperation(UMLOperation operation) {
    	this.operations.add(operation);
    }

    public void addAttribute(UMLAttribute attribute) {
    	this.attributes.add(attribute);
    }

    public List<UMLOperation> getOperations() {
		return operations;
	}

	public List<UMLAttribute> getAttributes() {
		return attributes;
	}

    public String getSourceFile() {
		return locationInfo.getFilePath();
	}

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLAnonymousClass) {
    		UMLAnonymousClass umlClass = (UMLAnonymousClass)o;
    		return this.packageName.equals(umlClass.packageName) && this.attributes.equals(umlClass.attributes) &&
    				this.operations.equals(umlClass.operations) && this.getSourceFile().equals(umlClass.getSourceFile());
    	}
    	return false;
    }

    public String toString() {
    	return getName();
    }

	public int compareTo(UMLAnonymousClass umlClass) {
		return this.toString().compareTo(umlClass.toString());
	}
}
