package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import extension.ast.node.LangASTNode;
import extension.ast.node.expression.LangComprehensionExpression.LangComprehensionClause;
import extension.ast.node.unit.LangCompilationUnit;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class ComprehensionClause extends LeafExpression {
	private List<AbstractExpression> targets;
	private AbstractExpression iterable;
	private List<AbstractExpression> filters;

	public ComprehensionClause(LangCompilationUnit cu, String sourceFolder, String filePath, LangComprehensionClause clause, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		super(cu, sourceFolder, filePath, clause, CodeElementType.COMPREHENSION_CLAUSE, container);
		this.targets = new ArrayList<>();
		for(LangASTNode target : clause.getTargets()) {
			targets.add(new AbstractExpression(cu, sourceFolder, filePath, target, CodeElementType.COMPREHENSION_CLAUSE_TARGET, container, activeVariableDeclarations, fileContent));
		}
		if(clause.getIterable() != null) {
			this.iterable = new AbstractExpression(cu, sourceFolder, filePath, clause.getIterable(), CodeElementType.COMPREHENSION_CLAUSE_ITERABLE, container, activeVariableDeclarations, fileContent);
		}
		this.filters = new ArrayList<>();
		for(LangASTNode filter : clause.getFilters()) {
			filters.add(new AbstractExpression(cu, sourceFolder, filePath, filter, CodeElementType.COMPREHENSION_CLAUSE_FILTER, container, activeVariableDeclarations, fileContent));
		}
	}

	public LeafExpression asLeafExpression() {
		return new LeafExpression(getString(), getLocationInfo());
	}
}
