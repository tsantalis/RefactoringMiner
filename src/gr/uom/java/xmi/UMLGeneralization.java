package gr.uom.java.xmi;

public class UMLGeneralization implements Comparable<UMLGeneralization> {
    private String child;
    private String parent;
    private String xmiID;

    public UMLGeneralization(String child, String parent) {
        this.child = child;
        this.parent = parent;
    }

    public String getChild() {
        return child;
    }

    public void setChild(String child) {
		this.child = child;
	}

	public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
		this.parent = parent;
	}

	public String getXmiID() {
		return xmiID;
	}

	public void setXmiID(String xmiID) {
		this.xmiID = xmiID;
	}

	public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLGeneralization) {
    		UMLGeneralization umlGeneralization = (UMLGeneralization)o;
    		return this.child.equals(umlGeneralization.child) &&
    			this.parent.equals(umlGeneralization.parent);
    	}
    	return false;
    }

    public String toString() {
    	return child + "->" + parent;
    }

	public int compareTo(UMLGeneralization generalization) {
		return this.toString().compareTo(generalization.toString());
	}
}
