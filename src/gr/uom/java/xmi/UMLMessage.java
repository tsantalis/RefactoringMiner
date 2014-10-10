package gr.uom.java.xmi;

public class UMLMessage {
	private String name;
	private String xmiID;
	private UMLClassifierRole sender;
	private UMLClassifierRole receiver;
	private String activatorID;
	private UMLMessage activatorMessage;
	private String predecessorID;
	private UMLMessage predecessorMessage;
	private UMLAction action;
	
	public UMLMessage(String xmiID, UMLClassifierRole sender, UMLClassifierRole receiver,
			String activatorID, UMLAction action) {
		this.xmiID = xmiID;
		this.sender = sender;
		this.receiver = receiver;
		this.activatorID = activatorID;
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getXmiID() {
		return xmiID;
	}

	public UMLClassifierRole getSender() {
		return sender;
	}

	public UMLClassifierRole getReceiver() {
		return receiver;
	}

	public String getActivatorID() {
		return activatorID;
	}

	public UMLMessage getActivatorMessage() {
		return activatorMessage;
	}

	public void setActivatorMessage(UMLMessage activatorMessage) {
		this.activatorMessage = activatorMessage;
	}

	public String getPredecessorID() {
		return predecessorID;
	}

	public void setPredecessorID(String predecessorID) {
		this.predecessorID = predecessorID;
	}

	public UMLMessage getPredecessorMessage() {
		return predecessorMessage;
	}

	public void setPredecessorMessage(UMLMessage predecessorMessage) {
		this.predecessorMessage = predecessorMessage;
	}

	public UMLAction getAction() {
		return action;
	}

	public boolean isSelfMessage() {
		return sender.equals(receiver);
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLMessage) {
    		UMLMessage umlMessage = (UMLMessage)o;
    		if(this.sender.equals(umlMessage.sender) && this.receiver.equals(umlMessage.receiver)) {
    			if(this.action instanceof UMLCallAction && umlMessage.action instanceof UMLCallAction) {
    				return this.action.equals(umlMessage.action);
    			}
    			else if(this.action instanceof UMLReturnAction && umlMessage.action instanceof UMLReturnAction) {
    				return this.activatorMessage.action.equals(umlMessage.activatorMessage.action);
    			}
    			else if(this.action instanceof UMLCreateAction && umlMessage.action instanceof UMLCreateAction) {
    				if(this.name != null && umlMessage.name != null)
    					return this.name.equals(umlMessage.name);
    				return true;
    			}
    			else
    				return false;
    		}
    	}
    	return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(sender);
		sb.append("--");
		if(action instanceof UMLCallAction)
			sb.append(action.toString());
		else if(action instanceof UMLReturnAction)
			sb.append("return of ").append(activatorMessage.getAction().toString());
		else if(action instanceof UMLCreateAction)
			sb.append("new");
		sb.append("->");
		sb.append(receiver);
		return sb.toString();
	}
}
