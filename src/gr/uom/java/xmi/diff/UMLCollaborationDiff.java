package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClassifierRole;
import gr.uom.java.xmi.UMLMessage;

import java.util.ArrayList;
import java.util.List;

public class UMLCollaborationDiff {
	private String collaborationName;
	private List<UMLClassifierRole> addedClassifierRoles;
	private List<UMLClassifierRole> removedClassifierRoles;
	private List<UMLMessage> addedMessages;
	private List<UMLMessage> removedMessages;
	
	public UMLCollaborationDiff(String collaborationName) {
		this.collaborationName = collaborationName;
		this.addedClassifierRoles = new ArrayList<UMLClassifierRole>();
		this.removedClassifierRoles = new ArrayList<UMLClassifierRole>();
		this.addedMessages = new ArrayList<UMLMessage>();
		this.removedMessages = new ArrayList<UMLMessage>();
	}

	public void reportAddedClassifierRole(UMLClassifierRole umlClassifierRole) {
		this.addedClassifierRoles.add(umlClassifierRole);
	}

	public void reportRemovedClassifierRole(UMLClassifierRole umlClassifierRole) {
		this.removedClassifierRoles.add(umlClassifierRole);
	}

	public void reportAddedMessage(UMLMessage umlMessage) {
		this.addedMessages.add(umlMessage);
	}

	public void reportRemovedMessage(UMLMessage umlMessage) {
		this.removedMessages.add(umlMessage);
	}

	public boolean isEmpty() {
		return addedClassifierRoles.isEmpty() && removedClassifierRoles.isEmpty() &&
			addedMessages.isEmpty() && removedMessages.isEmpty();
	}

	public List<UMLClassifierRole> getAddedClassifierRoles() {
		return addedClassifierRoles;
	}

	public List<UMLClassifierRole> getRemovedClassifierRoles() {
		return removedClassifierRoles;
	}

	public List<UMLMessage> getAddedMessages() {
		return addedMessages;
	}

	public List<UMLMessage> getRemovedMessages() {
		return removedMessages;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append(collaborationName + ":").append("\n");
		for(UMLClassifierRole umlClassifierRole : removedClassifierRoles) {
			sb.append("classifierRole " + umlClassifierRole + " removed").append("\n");
		}
		for(UMLClassifierRole umlClassifierRole : addedClassifierRoles) {
			sb.append("classifierRole " + umlClassifierRole + " added").append("\n");
		}
		for(UMLMessage umlMessage : removedMessages) {
			sb.append("message " + umlMessage + " removed").append("\n");
		}
		for(UMLMessage umlMessage : addedMessages) {
			sb.append("message " + umlMessage + " added").append("\n");
		}
		return sb.toString();
	}
}
