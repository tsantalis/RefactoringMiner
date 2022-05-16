package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.List;

public class UMLAnonymousClass extends UMLAbstractClass implements Comparable<UMLAnonymousClass>, Serializable, LocationInfoProvider {
	private String codePath;
	
	public UMLAnonymousClass(String packageName, String name, String codePath, LocationInfo locationInfo, List<String> importedTypes) {
    	super(packageName, name, locationInfo, importedTypes);
        this.codePath = codePath;
    }

	public boolean isDirectlyNested() {
		return !name.contains(".");
	}

	public String getCodePath() {
		if(packageName.equals(""))
    		return codePath;
    	else
    		return packageName + "." + codePath;
	}

    public String getName() {
    	if(packageName.equals(""))
    		return name;
    	else
    		return packageName + "." + name;
    }

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLAnonymousClass) {
    		UMLAnonymousClass umlClass = (UMLAnonymousClass)o;
    		return this.packageName.equals(umlClass.packageName) && this.name.equals(umlClass.name) && this.getSourceFile().equals(umlClass.getSourceFile());
    	}
    	return false;
    }

    public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((getSourceFile() == null) ? 0 : getSourceFile().hashCode());
		return result;
	}

    public String toString() {
    	return getName();
    }

	public int compareTo(UMLAnonymousClass umlClass) {
		return this.toString().compareTo(umlClass.toString());
	}

	public boolean isSingleAbstractMethodInterface() {
		return false;
	}

	public boolean isSingleMethodClass() {
		return false;
	}

	public boolean isInterface() {
		return false;
	}

	public boolean isAbstract() {
		return false;
	}

	public String getTypeDeclarationKind() {
		return "anonymous class";
	}

	public boolean isFinal() {
		return false;
	}

	public boolean isStatic() {
		return false;
	}

	public String getVisibility() {
		return "private";
	}

	public boolean isTopLevel() {
		return false;
	}
}
