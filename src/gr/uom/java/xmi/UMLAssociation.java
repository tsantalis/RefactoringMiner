package gr.uom.java.xmi;

public class UMLAssociation implements Comparable<UMLAssociation> {
    private UMLAssociationEnd end1;
    private UMLAssociationEnd end2;

    public UMLAssociation(UMLAssociationEnd end1, UMLAssociationEnd end2) {
        this.end1 = end1;
        this.end2 = end2;
    }

    public UMLAssociationEnd getEnd1() {
		return end1;
	}

	public UMLAssociationEnd getEnd2() {
		return end2;
	}

	public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLAssociation) {
    		UMLAssociation umlAssociation = (UMLAssociation)o;
    		return this.end1.equals(umlAssociation.end1) &&
    		this.end2.equals(umlAssociation.end2);
    	}
    	return false;
    }

    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(end1.getParticipant());
    	sb.append(end1);
    	if(end1.isNavigable())
    		sb.append("<");
    	else
    		sb.append("-");
    	//
    	sb.append("-");
    	//
    	if(end2.isNavigable())
    		sb.append(">");
    	else
    		sb.append("-");
    	sb.append(end2);
    	sb.append(end2.getParticipant());
    	return sb.toString();
    }

	public int compareTo(UMLAssociation association) {
		return this.toString().compareTo(association.toString());
	}
}
