package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractExpression;

public class InitializerReplacement extends Replacement {
	private AbstractExpression initializerBefore;
	private AbstractExpression initializerAfter;
	
	public InitializerReplacement(AbstractExpression initializerBefore, AbstractExpression initializerAfter) {
		super(initializerBefore.getString(), initializerAfter.getString(), ReplacementType.VARIABLE_DECLARATION_INITIALIZER);
		this.initializerBefore = initializerBefore;
		this.initializerAfter = initializerAfter;
	}

	public AbstractExpression getInitializerBefore() {
		return initializerBefore;
	}

	public AbstractExpression getInitializerAfter() {
		return initializerAfter;
	}
}
