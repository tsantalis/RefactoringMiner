package gr.uom.java.xmi;

public class UMLCallAction extends UMLAction {
	private UMLOperation operation;
	private String actionExpression;
	
	public UMLCallAction(String xmiID) {
		super(xmiID);
	}

	public UMLOperation getOperation() {
		return operation;
	}

	public void setOperation(UMLOperation operation) {
		this.operation = operation;
	}

	public String getActionExpression() {
		return actionExpression;
	}

	public void setActionExpression(String actionExpression) {
		this.actionExpression = actionExpression;
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLCallAction) {
    		UMLCallAction umlCallAction = (UMLCallAction)o;
    		if(this.operation != null && umlCallAction.operation != null)
    			return this.operation.equals(umlCallAction.operation);
    		else if(this.actionExpression != null && umlCallAction.actionExpression != null)
    			return this.actionExpression.equals(umlCallAction.actionExpression);
    		else
    			return false;
    	}
    	return false;
	}

	public String toString() {
		if(actionExpression != null)
			return actionExpression;
		if(operation != null)
			return operation.toString();
		return "unknown";
	}
}
