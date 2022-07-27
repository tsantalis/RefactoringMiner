package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiFile;

import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class TernaryOperatorExpression {

	private AbstractExpression condition;
	private AbstractExpression thenExpression;
	private AbstractExpression elseExpression;
	private String expression;

	public TernaryOperatorExpression(PsiFile cu, String filePath, PsiConditionalExpression expression, VariableDeclarationContainer container) {
		this.condition = new AbstractExpression(cu, filePath, expression.getCondition(), CodeElementType.TERNARY_OPERATOR_CONDITION, container);
		if(expression.getThenExpression() != null) {
			this.thenExpression = new AbstractExpression(cu, filePath, expression.getThenExpression(), CodeElementType.TERNARY_OPERATOR_THEN_EXPRESSION, container);
		}
		if(expression.getElseExpression() != null) {
			this.elseExpression = new AbstractExpression(cu, filePath, expression.getElseExpression(), CodeElementType.TERNARY_OPERATOR_ELSE_EXPRESSION, container);
		}
		this.expression = Formatter.format(expression);
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

	public Replacement makeReplacementWithTernaryOnTheRight(String statement) {
		if(getElseExpression().getString().equals(statement)) {
			return new Replacement(statement, getExpression(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
		}
		if(getThenExpression().getString().equals(statement)) {
			return new Replacement(statement, getExpression(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
		}
		return null;
	}

	public Replacement makeReplacementWithTernaryOnTheLeft(String statement) {
		if(getElseExpression().getString().equals(statement)) {
			return new Replacement(getExpression(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
		}
		if(getThenExpression().getString().equals(statement)) {
			return new Replacement(getExpression(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
		}
		return null;
	}
}
