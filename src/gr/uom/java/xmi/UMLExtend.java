package gr.uom.java.xmi;

public class UMLExtend implements Comparable<UMLExtend> {
	private String xmiID;
	private String extension;
	private String base;
	
	public UMLExtend(String xmiID, String extension, String base) {
		this.xmiID = xmiID;
		this.extension = extension;
		this.base = base;
	}

	public String getXmiID() {
		return xmiID;
	}

	public void setXmiID(String xmiID) {
		this.xmiID = xmiID;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
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
    	
    	if(o instanceof UMLExtend) {
    		UMLExtend umlExtend = (UMLExtend)o;
    		return this.base.equals(umlExtend.base) &&
    			this.extension.equals(umlExtend.extension);
    	}
    	return false;
	}

    public String toString() {
    	return extension + "->" + base;
    }

	public int compareTo(UMLExtend extend) {
		return this.toString().compareTo(extend.toString());
	}

}
