package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

public class TryStatementObject extends CompositeStatementObject {
	private List<CompositeStatementObject> catchClauses;
	private CompositeStatementObject finallyClause;

	public TryStatementObject(CompilationUnit cu, String filePath, Statement statement, int depth) {
		super(cu, filePath, statement, depth, "try");
		this.catchClauses = new ArrayList<CompositeStatementObject>();
	}

	public void addCatchClause(CompositeStatementObject catchClause) {
		catchClauses.add(catchClause);
	}

	public List<CompositeStatementObject> getCatchClauses() {
		return catchClauses;
	}

	public void setFinallyClause(CompositeStatementObject finallyClause) {
		this.finallyClause = finallyClause;
	}

	public CompositeStatementObject getFinallyClause() {
		return finallyClause;
	}

	protected double compositeChildMatchingScore(CompositeStatementObject other, List<AbstractCodeMapping> mappings) {
		double score = super.compositeChildMatchingScore(other, mappings);
		if(other instanceof TryStatementObject) {
			TryStatementObject otherTry = (TryStatementObject)other;
			if(catchClauses.size() == otherTry.catchClauses.size()) {
				for(int i=0; i<catchClauses.size(); i++) {
					double tmpScore = catchClauses.get(i).compositeChildMatchingScore(otherTry.catchClauses.get(i), mappings);
					if(tmpScore == 1) {
						score += tmpScore;
					}
				}
			}
			if(finallyClause != null && otherTry.finallyClause != null) {
				double tmpScore = finallyClause.compositeChildMatchingScore(otherTry.finallyClause, mappings);
				if(tmpScore == 1) {
					score += tmpScore;
				}
			}
		}
		return score;
	}
}
