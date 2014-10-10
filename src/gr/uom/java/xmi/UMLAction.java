package gr.uom.java.xmi;

public abstract class UMLAction {
	private String xmiID;
	
	public UMLAction(String xmiID) {
		this.xmiID = xmiID;
	}

	public String getXmiID() {
		return xmiID;
	}
}
