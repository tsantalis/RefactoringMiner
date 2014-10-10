package gr.uom.java.xmi;

public class UMLDependency implements Comparable<UMLDependency> {
    private String client;
    private String supplier;
    private String xmiID;

    public UMLDependency(String client, String supplier) {
        this.supplier = supplier;
        this.client = client;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
		this.supplier = supplier;
	}

	public String getClient() {
        return client;
    }

    public void setClient(String client) {
		this.client = client;
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
    	
    	if(o instanceof UMLDependency) {
    		UMLDependency umlDependency = (UMLDependency)o;
    		return this.client.equals(umlDependency.client) &&
    			this.supplier.equals(umlDependency.supplier);
    	}
    	return false;
    }

    public String toString() {
    	return client + "->" + supplier;
    }

	public int compareTo(UMLDependency dependency) {
		return this.toString().compareTo(dependency.toString());
	}
}
