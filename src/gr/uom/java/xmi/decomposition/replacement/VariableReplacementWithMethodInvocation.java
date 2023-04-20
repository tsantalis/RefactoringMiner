package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractCall;

public class VariableReplacementWithMethodInvocation extends Replacement {
	private AbstractCall invokedOperation;
	private Direction direction;
	
	public VariableReplacementWithMethodInvocation(String before, String after, AbstractCall invocation, Direction direction) {
		super(before, after, ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION);
		this.invokedOperation = invocation;
		this.direction = direction;
	}

	public boolean getterReplacement() {
		if(direction.equals(Direction.VARIABLE_TO_INVOCATION)) {
			String variable = getBefore();
			String capitalizedFirstLetter = Character.toUpperCase(variable.charAt(0)) + variable.substring(1, variable.length());
			String methodCall = getAfter();
			return methodCall.equals("get" + capitalizedFirstLetter + "()");
		}
		else if(direction.equals(Direction.INVOCATION_TO_VARIABLE) ) {
			String variable = getAfter();
			String capitalizedFirstLetter = Character.toUpperCase(variable.charAt(0)) + variable.substring(1, variable.length());
			String methodCall = getBefore();
			return methodCall.equals("get" + capitalizedFirstLetter + "()");
		}
		return false;
	}

	public AbstractCall getInvokedOperation() {
		return invokedOperation;
	}

	public Direction getDirection() {
		return direction;
	}

	public enum Direction {
		VARIABLE_TO_INVOCATION, INVOCATION_TO_VARIABLE;
	}
}
