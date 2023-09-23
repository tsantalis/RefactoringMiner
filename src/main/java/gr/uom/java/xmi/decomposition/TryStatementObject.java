package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class TryStatementObject extends CompositeStatementObject {
	private List<CompositeStatementObject> catchClauses;
	private CompositeStatementObject finallyClause;

	public TryStatementObject(CompilationUnit cu, String filePath, Statement statement, int depth) {
		super(cu, filePath, statement, depth, CodeElementType.TRY_STATEMENT);
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

	public boolean isTryWithResources() {
		return getExpressions().size() > 0;
	}

	public boolean identicalCatchOrFinallyBlocks(TryStatementObject other) {
		int identicalCatchClauses = 0;
		boolean identicalFinallyClause = false;
		if(this.catchClauses.size() == other.catchClauses.size()) {
			for(int i=0; i<this.catchClauses.size(); i++) {
				CompositeStatementObject thisCatch = this.catchClauses.get(i);
				CompositeStatementObject otherCatch = other.catchClauses.get(i);
				if(thisCatch.stringRepresentation().equals(otherCatch.stringRepresentation())) {
					identicalCatchClauses++;
				}
			}
		}
		else {
			return false;
		}
		if(this.finallyClause != null && other.finallyClause != null) {
			if(this.finallyClause.stringRepresentation().equals(other.finallyClause.stringRepresentation())) {
				identicalFinallyClause = true;
			}
		}
		else if(this.finallyClause == null && other.finallyClause == null) {
			identicalFinallyClause = true;
		}
		return (identicalCatchClauses > 0 && identicalCatchClauses == this.catchClauses.size()) || identicalFinallyClause;
	}
}
