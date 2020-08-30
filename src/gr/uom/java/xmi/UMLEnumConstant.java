package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.diff.CodeRange;

public class UMLEnumConstant implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private List<String> arguments;
	private String name;
	private String className;
	private UMLJavadoc javadoc;

	public UMLEnumConstant(String name, LocationInfo locationInfo) {
		this.name = name;
		this.locationInfo = locationInfo;
		this.arguments = new ArrayList<String>();
	}

	public String getName() {
		return name;
	}

	public void addArgument(String argument) {
		this.arguments.add(argument);
	}

	public List<String> getArguments() {
		return arguments;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public UMLJavadoc getJavadoc() {
		return javadoc;
	}

	public void setJavadoc(UMLJavadoc javadoc) {
		this.javadoc = javadoc;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public CodeRange codeRange() {
		LocationInfo info = getLocationInfo();
		return info.codeRange();
	}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if(arguments.size() > 0) {
        	sb.append("(");
            for(int i=0; i<arguments.size()-1; i++) {
                sb.append(arguments.get(i)).append(", ");
            }
            sb.append(arguments.get(arguments.size()-1));
            sb.append(")");
        }
        return sb.toString();
    }

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLEnumConstant other = (UMLEnumConstant) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
