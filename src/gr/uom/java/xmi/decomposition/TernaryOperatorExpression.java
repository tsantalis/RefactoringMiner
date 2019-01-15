package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;

public class TernaryOperatorExpression {

	private AbstractExpression condition;
	private AbstractExpression thenExpression;
	private AbstractExpression elseExpression;
	private String expression;

	public TernaryOperatorExpression(CompilationUnit cu, String filePath, ConditionalExpression expression) {
		this.condition = new AbstractExpression(cu, filePath, expression.getExpression());
		this.thenExpression = new AbstractExpression(cu, filePath, expression.getThenExpression());
		this.elseExpression = new AbstractExpression(cu, filePath, expression.getElseExpression());
		this.expression = expression.toString();
	}

	public AbstractExpression getCondition() {
		return condition;
	}

	public AbstractExpression getThenExpression() {
		return thenExpression;
	}

	public AbstractExpression getElseExpression() {
		return elseExpression;
	}

    public String getExpression() {
    	return expression;
    }
}
