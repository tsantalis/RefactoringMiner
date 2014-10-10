package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class UMLAssociationRole {
	private String xmiID;
	private UMLClassifierRole end1;
	private UMLClassifierRole end2;
	private List<String> messageIdList;
	
	public UMLAssociationRole(String xmiID, UMLClassifierRole end1, UMLClassifierRole end2) {
		this.xmiID = xmiID;
		this.end1 = end1;
		this.end2 = end2;
		this.messageIdList = new ArrayList<String>();
	}

    public String getXmiID() {
		return xmiID;
	}

    public UMLClassifierRole getLeftEnd() {
		return end1;
	}

	public UMLClassifierRole getRightEnd() {
		return end2;
	}

	public void addMessageID(String messageID) {
    	messageIdList.add(messageID);
    }

	public ListIterator<String> getMessageIdIterator() {
		return messageIdList.listIterator();
	}

	public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLAssociationRole) {
    		UMLAssociationRole umlAssociationRole = (UMLAssociationRole)o;
    		return this.end1.equals(umlAssociationRole.end1) &&
    		this.end2.equals(umlAssociationRole.end2);
    	}
    	return false;
    }

	public String toString() {
		return end1 + "--" + end2;
	}
}
