package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import extension.ast.node.expression.LangComprehensionExpression;
import extension.ast.node.expression.LangComprehensionExpression.LangComprehensionClause;
import extension.ast.node.unit.LangCompilationUnit;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class ComprehensionExpression extends LeafExpression {
	private AbstractExpression expression;
	private AbstractExpression keyExpression;
	private AbstractExpression valueExpression;
	private List<ComprehensionClause> clauses;

	public ComprehensionExpression(LangCompilationUnit cu, String sourceFolder, String filePath, LangComprehensionExpression expression, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		super(cu, sourceFolder, filePath, expression, CodeElementType.COMPREHENSION, container);
		if(expression.getExpression() != null) {
			this.expression = new AbstractExpression(cu, sourceFolder, filePath, expression.getExpression(), CodeElementType.COMPREHENSION_EXPRESSION, container, activeVariableDeclarations, fileContent);
		}
		if(expression.getKeyExpression() != null) {
			this.keyExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getKeyExpression(), CodeElementType.COMPREHENSION_KEY_EXPRESSION, container, activeVariableDeclarations, fileContent);
		}
		if(expression.getValueExpression() != null) {
			this.valueExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getValueExpression(), CodeElementType.COMPREHENSION_VALUE_EXPRESSION, container, activeVariableDeclarations, fileContent);
		}
		this.clauses = new ArrayList<>();
		for(LangComprehensionClause clause : expression.getClauses()) {
			clauses.add(new ComprehensionClause(cu, sourceFolder, filePath, clause, container, activeVariableDeclarations, fileContent));
		}
	}

	public AbstractExpression getExpression() {
		return expression;
	}

	public AbstractExpression getKeyExpression() {
		return keyExpression;
	}

	public AbstractExpression getValueExpression() {
		return valueExpression;
	}

	public List<ComprehensionClause> getClauses() {
		return clauses;
	}

	public LeafExpression asLeafExpression() {
		return new LeafExpression(getString(), getLocationInfo());
	}

	public boolean equalExpression(ComprehensionExpression other) {
		if(this.expression != null && other.expression != null) {
			return this.expression.getString().equals(other.expression.getString());
		}
		else if(this.expression == null && other.expression == null) {
			return true;
		}
		return false;
	}

	public boolean equalKeyExpression(ComprehensionExpression other) {
		if(this.keyExpression != null && other.keyExpression != null) {
			return this.keyExpression.getString().equals(other.keyExpression.getString());
		}
		else if(this.keyExpression == null && other.keyExpression == null) {
			return true;
		}
		return false;
	}

	public boolean equalValueExpression(ComprehensionExpression other) {
		if(this.valueExpression != null && other.valueExpression != null) {
			return this.valueExpression.getString().equals(other.valueExpression.getString());
		}
		else if(this.valueExpression == null && other.valueExpression == null) {
			return true;
		}
		return false;
	}

	public boolean equalClauses(ComprehensionExpression other) {
		if(this.clauses.size() == other.clauses.size()) {
			int identical = 0;
			for(int i=0; i<this.clauses.size(); i++) {
				if(this.clauses.get(i).getString().equals(other.clauses.get(i).getString())) {
					identical++;
				}
			}
			return identical == this.clauses.size();
		}
		return false;
		
	}
}
