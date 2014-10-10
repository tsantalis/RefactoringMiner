package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLParameter;

public class UMLParameterDiff {
	private UMLParameter removedParameter;
	private UMLParameter addedParameter;
	private boolean typeChanged;
	
	public UMLParameterDiff(UMLParameter removedParameter, UMLParameter addedParameter) {
		this.removedParameter = removedParameter;
		this.addedParameter = addedParameter;
		this.typeChanged = false;
		if(!removedParameter.getType().equals(addedParameter.getType()))
			typeChanged = true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(typeChanged)
			sb.append("\t\t").append("parameter ").append(removedParameter).append(":").append("\n");
		if(typeChanged)
			sb.append("\t\t").append("type changed from " + removedParameter.getType() + " to " + addedParameter.getType()).append("\n");
		return sb.toString();
	}
}
