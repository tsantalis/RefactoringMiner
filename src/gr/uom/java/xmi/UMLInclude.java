package gr.uom.java.xmi;

public class UMLInclude implements Comparable<UMLInclude> {
	private String xmiID;
	private String addition;
	private String base;
	
	public UMLInclude(String xmiID, String addition, String base) {
		this.xmiID = xmiID;
		this.addition = addition;
		this.base = base;
	}

	public String getXmiID() {
		return xmiID;
	}

	public void setXmiID(String xmiID) {
		this.xmiID = xmiID;
	}

	public String getAddition() {
		return addition;
	}

	public void setAddition(String addition) {
		this.addition = addition;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLInclude) {
    		UMLInclude umlInclude = (UMLInclude)o;
    		return this.base.equals(umlInclude.base) &&
    			this.addition.equals(umlInclude.addition);
    	}
    	return false;
	}

    public String toString() {
    	return base + "->" + addition;
    }

	public int compareTo(UMLInclude include) {
		return this.toString().compareTo(include.toString());
	}
}
