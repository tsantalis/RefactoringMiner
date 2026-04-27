package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;
import java.util.Set;

import extension.ast.node.expression.LangTernaryExpression;
import extension.ast.node.unit.LangCompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;

import com.caoccao.javet.swc4j.ast.expr.Swc4jAstCondExpr;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class TernaryOperatorExpression extends LeafExpression {

	private AbstractExpression condition;
	private AbstractExpression thenExpression;
	private AbstractCodeFragment elseExpression;

    public TernaryOperatorExpression(LangCompilationUnit cu, String sourceFolder, String filePath, LangTernaryExpression expression, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String javaFileContent) {
        super(cu, sourceFolder, filePath, expression, CodeElementType.TERNARY_OPERATOR, container);
        this.condition = new AbstractExpression(cu, sourceFolder, filePath, expression.getCondition(), CodeElementType.TERNARY_OPERATOR_CONDITION, container, activeVariableDeclarations, javaFileContent);
        this.thenExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getThenExpression(), CodeElementType.TERNARY_OPERATOR_THEN_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
        if(expression.getElseExpression() instanceof LangTernaryExpression) {
            this.elseExpression = new TernaryOperatorExpression(cu, sourceFolder, filePath, (LangTernaryExpression) expression.getElseExpression(), container, activeVariableDeclarations, javaFileContent);
        }
        else {
            this.elseExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getElseExpression(), CodeElementType.TERNARY_OPERATOR_ELSE_EXPRESSION, container, activeVariableDeclarations, javaFileContent);
        }
    }

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

	public TernaryOperatorExpression(String sourceFolder, String filePath, Swc4jAstCondExpr expression, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent, List<UMLClass> typeDeclarations) {
		super(sourceFolder, filePath, expression, CodeElementType.TERNARY_OPERATOR, container, fileContent);
		this.condition = new AbstractExpression(sourceFolder, filePath, expression.getTest(), CodeElementType.TERNARY_OPERATOR_CONDITION, container, activeVariableDeclarations, fileContent, typeDeclarations);
		this.thenExpression = new AbstractExpression(sourceFolder, filePath, expression.getCons(), CodeElementType.TERNARY_OPERATOR_THEN_EXPRESSION, container, activeVariableDeclarations, fileContent, typeDeclarations);
		this.elseExpression = new AbstractExpression(sourceFolder, filePath, expression.getAlt(), CodeElementType.TERNARY_OPERATOR_ELSE_EXPRESSION, container, activeVariableDeclarations, fileContent, typeDeclarations);
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
