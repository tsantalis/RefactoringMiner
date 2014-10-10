package gr.uom.java.xmi;

public class UMLClassifierRole {
	private String name;
	private String xmiID;
	private String base;
	
	public UMLClassifierRole(String name, String xmiID) {
		this.name = name;
		this.xmiID = xmiID;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
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
    	
    	if(o instanceof UMLClassifierRole) {
    		UMLClassifierRole umlClassifierRole = (UMLClassifierRole)o;
    		if(this.name.equals(umlClassifierRole.name)) {
    			if(this.base != null && umlClassifierRole.base != null) {
    				return this.base.equals(umlClassifierRole.base);
    			}
    			else if(this.base == null && umlClassifierRole.base == null) {
    				return true;
    			}
    			else {
    				return false;
    			}
    		}
    	}
    	return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		if(base != null) {
			sb.append(":");
			sb.append(base);
		}
		return sb.toString();
	}
}
