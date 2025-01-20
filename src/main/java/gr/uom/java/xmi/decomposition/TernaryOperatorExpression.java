package gr.uom.java.xmi.decomposition;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class TernaryOperatorExpression extends LeafExpression {

	private AbstractExpression condition;
	private AbstractExpression thenExpression;
	private AbstractCodeFragment elseExpression;

	public TernaryOperatorExpression(CompilationUnit cu, String sourceFolder, String filePath, ConditionalExpression expression, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
		super(cu, sourceFolder, filePath, expression, CodeElementType.TERNARY_OPERATOR, container);
		this.condition = new AbstractExpression(cu, sourceFolder, filePath, expression.getExpression(), CodeElementType.TERNARY_OPERATOR_CONDITION, container, activeVariableDeclarations, javaFileContent);
		this.thenExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getThenExpression(), CodeElementType.TERNARY_OPERATOR_THEN_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
		if(expression.getElseExpression() instanceof ConditionalExpression) {
			this.elseExpression = new TernaryOperatorExpression(cu, sourceFolder, filePath, (ConditionalExpression)expression.getElseExpression(), container, activeVariableDeclarations, javaFileContent);
		}
		else {
			this.elseExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getElseExpression(), CodeElementType.TERNARY_OPERATOR_ELSE_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
		}
	}

	public LeafExpression asLeafExpression() {
		return new LeafExpression(getString(), getLocationInfo());
	}

	public AbstractExpression getCondition() {
		return condition;
	}

	public AbstractExpression getThenExpression() {
		return thenExpression;
	}

	public AbstractCodeFragment getElseExpression() {
		return elseExpression;
	}

	public String getExpression() {
		return getString();
	}

	public Replacement makeReplacementWithTernaryOnTheRight(String statement, Map<String, String> parameterToArgumentMap) {
		if(getElseExpression().getString().equals(statement) || getElseExpression().getString().equals(statement + ".get()")) {
			return new Replacement(statement, getElseExpression().getString(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
		}
		if(getThenExpression().getString().equals(statement) || getThenExpression().getString().equals(statement + ".get()")) {
			return new Replacement(statement, getThenExpression().getString(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
		}
		if(getCondition().getString().equals(statement) || getCondition().getString().equals(statement + ".get()")) {
			return new Replacement(statement, getCondition().getString(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_CONDITION);
		}
		String temp = new String(statement);
		for(String key : parameterToArgumentMap.keySet()) {
			if(!key.equals(parameterToArgumentMap.get(key))) {
				temp = ReplacementUtil.performReplacement(temp, parameterToArgumentMap.get(key), key);
			}
		}
		if(!temp.equals(statement)) {
			if(getElseExpression().getString().equals(temp)) {
				return new Replacement(statement, getElseExpression().getString(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
			}
			if(getThenExpression().getString().equals(temp)) {
				return new Replacement(statement, getThenExpression().getString(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
			}
			if(getCondition().getString().equals(temp)) {
				return new Replacement(statement, getCondition().getString(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_CONDITION);
			}
		}
		return null;
	}

	public Replacement makeReplacementWithTernaryOnTheLeft(String statement, Map<String, String> parameterToArgumentMap) {
		if(getElseExpression().getString().equals(statement) || getElseExpression().getString().equals(statement + ".get()")) {
			return new Replacement(getElseExpression().getString(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
		}
		if(getThenExpression().getString().equals(statement) || getThenExpression().getString().equals(statement + ".get()")) {
			return new Replacement(getThenExpression().getString(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
		}
		if(getCondition().getString().equals(statement) || getCondition().getString().equals(statement + ".get()")) {
			return new Replacement(getCondition().getString(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_CONDITION);
		}
		String temp = new String(statement);
		for(String key : parameterToArgumentMap.keySet()) {
			if(!key.equals(parameterToArgumentMap.get(key))) {
				temp = ReplacementUtil.performReplacement(temp, parameterToArgumentMap.get(key), key);
			}
		}
		if(!temp.equals(statement)) {
			if(getElseExpression().getString().equals(temp)) {
				return new Replacement(getElseExpression().getString(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
			}
			if(getThenExpression().getString().equals(temp)) {
				return new Replacement(getThenExpression().getString(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
			}
			if(getCondition().getString().equals(temp)) {
				return new Replacement(getCondition().getString(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_CONDITION);
			}
		}
		return null;
	}
}
