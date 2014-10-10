package gr.uom.java.xmi;

public class UMLRealization implements Comparable<UMLRealization> {
    private String client;
    private String supplier;

    public UMLRealization(String client, String supplier) {
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

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLRealization) {
    		UMLRealization umlRealization = (UMLRealization)o;
    		return this.client.equals(umlRealization.client) &&
    			this.supplier.equals(umlRealization.supplier);
    	}
    	return false;
    }

    public String toString() {
    	return client + "->" + supplier;
    }

	public int compareTo(UMLRealization realization) {
		return this.toString().compareTo(realization.toString());
	}
}
