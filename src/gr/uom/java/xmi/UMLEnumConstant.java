package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class UMLEnumConstant extends UMLAttribute {
	private List<String> arguments;

	public UMLEnumConstant(String name, UMLType type, LocationInfo locationInfo) {
		super(name, type, locationInfo);
		this.arguments = new ArrayList<String>();
	}

	public void addArgument(String argument) {
		this.arguments.add(argument);
	}

	public List<String> getArguments() {
		return arguments;
	}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
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
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
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
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		return true;
	}
}
