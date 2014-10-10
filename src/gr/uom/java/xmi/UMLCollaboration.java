package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.UMLCollaborationDiff;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class UMLCollaboration {
	private String name;
	private String xmiID;
	private List<UMLClassifierRole> classifierRoles;
	private List<UMLAssociationRole> associationRoles;
	private List<UMLAction> actions;
	private List<UMLMessage> messages;
	
	public UMLCollaboration(String name, String xmiID) {
		this.name = name;
		this.xmiID = xmiID;
		this.classifierRoles = new ArrayList<UMLClassifierRole>();
		this.associationRoles = new ArrayList<UMLAssociationRole>();
		this.actions = new ArrayList<UMLAction>();
		this.messages = new ArrayList<UMLMessage>();
	}

	public String getName() {
		return name;
	}

	public String getXmiID() {
		return xmiID;
	}

	public void addClassifierRole(UMLClassifierRole umlClassifierRole) {
		this.classifierRoles.add(umlClassifierRole);
	}

	public void addAssociationRole(UMLAssociationRole umlAssociationRole) {
		this.associationRoles.add(umlAssociationRole);
	}

	public void addAction(UMLAction umlAction) {
		this.actions.add(umlAction);
	}

	public void addMessage(UMLMessage umlMessage) {
		this.messages.add(umlMessage);
	}

	public ListIterator<UMLMessage> getMessageListIterator() {
		return messages.listIterator();
	}

	public UMLClassifierRole getClassifierRole(String xmiID) {
		for(UMLClassifierRole classifierRole : classifierRoles) {
			if(classifierRole.getXmiID().equals(xmiID))
				return classifierRole;
		}
		return null;
	}

	public UMLAction getAction(String xmiID) {
		for(UMLAction action : actions) {
			if(action.getXmiID().equals(xmiID))
				return action;
		}
		return null;
	}

	public UMLMessage getMessage(String xmiID) {
		for(UMLMessage message : messages) {
			if(message.getXmiID().equals(xmiID))
				return message;
		}
		return null;
	}

	//this must be a message with call action
	/*public UMLOperation getInvokingOperation(UMLMessage message) {
		UMLClassifierRole sender = message.getSender();
		UMLAssociationRole associationRole = getAssociationRoleWithClassifierRoleAsRightEnd(sender);
		if(associationRole != null) {
			ListIterator<String> messageIdIterator = associationRole.getMessageIdIterator();
			while(messageIdIterator.hasNext()) {
				String messageID = messageIdIterator.next();
				UMLMessage invokingMessage = getMessage(messageID);
				if(invokingMessage.getReceiver().equals(sender)) {
					UMLAction action = invokingMessage.getAction();
					if(action instanceof UMLCallAction) {
						UMLCallAction callAction = (UMLCallAction)action;
						if(callAction.getOperation() != null)
							return callAction.getOperation();
					}
				}
			}
		}
		return null;
	}

	private UMLAssociationRole getAssociationRoleWithClassifierRoleAsRightEnd(UMLClassifierRole umlClassifierRole) {
		for(UMLAssociationRole associationRole : associationRoles) {
			if(umlClassifierRole.equals(associationRole.getRightEnd()))
				return associationRole;
		}
		return null;
	}*/

	public UMLCollaborationDiff diff(UMLCollaboration umlCollaboration) {
		UMLCollaborationDiff collaborationDiff = new UMLCollaborationDiff(name);
		for(UMLClassifierRole clasifierRole : this.classifierRoles) {
    		if(!umlCollaboration.classifierRoles.contains(clasifierRole))
    			collaborationDiff.reportRemovedClassifierRole(clasifierRole);
    	}
    	for(UMLClassifierRole clasifierRole : umlCollaboration.classifierRoles) {
    		if(!this.classifierRoles.contains(clasifierRole))
    			collaborationDiff.reportAddedClassifierRole(clasifierRole);
    	}
    	for(UMLMessage message : this.messages) {
    		if(!umlCollaboration.messages.contains(message))
    			collaborationDiff.reportRemovedMessage(message);
    	}
    	for(UMLMessage message : umlCollaboration.messages) {
    		if(!this.messages.contains(message))
    			collaborationDiff.reportAddedMessage(message);
    	}
		return collaborationDiff;
	}

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLCollaboration) {
    		UMLCollaboration umlCollaboration = (UMLCollaboration)o;
    		return this.name.equals(umlCollaboration.name);
    	}
    	return false;
    }

    public String toString() {
    	return name;
    }
}
