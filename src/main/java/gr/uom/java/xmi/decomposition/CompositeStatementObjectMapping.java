package gr.uom.java.xmi.decomposition;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.IntersectionReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.StringDistance;

public class CompositeStatementObjectMapping extends AbstractCodeMapping implements Comparable<CompositeStatementObjectMapping> {

	private double compositeChildMatchingScore;
	private boolean identicalCommentsInBody;
	
	public CompositeStatementObjectMapping(CompositeStatementObject statement1, CompositeStatementObject statement2,
			VariableDeclarationContainer operation1, VariableDeclarationContainer operation2, double score, boolean identicalCommentsInBody) {
		super(statement1, statement2, operation1, operation2);
		this.compositeChildMatchingScore = score;
		this.identicalCommentsInBody = identicalCommentsInBody;
	}

	public double getCompositeChildMatchingScore() {
		return compositeChildMatchingScore;
	}

	@Override
	public int compareTo(CompositeStatementObjectMapping o) {
		if(this.compositeChildMatchingScore == 1.0 && o.getReplacementTypes().contains(ReplacementType.COMPOSITE) && o.compositeChildMatchingScore == 0.99) {
			return -Double.compare(this.compositeChildMatchingScore, o.compositeChildMatchingScore);
		}
		else if(o.compositeChildMatchingScore == 1.0 && this.getReplacementTypes().contains(ReplacementType.COMPOSITE) && this.compositeChildMatchingScore == 0.99) {
			return -Double.compare(this.compositeChildMatchingScore, o.compositeChildMatchingScore);
		}
		if(this.compositeChildMatchingScore < 0.99 && this.compositeChildMatchingScore > 0 && o.getReplacementTypes().contains(ReplacementType.INVERT_CONDITIONAL) && o.compositeChildMatchingScore == 0.99) {
			return -1;
		}
		else if(o.compositeChildMatchingScore < 0.99 && o.compositeChildMatchingScore > 0 && this.getReplacementTypes().contains(ReplacementType.INVERT_CONDITIONAL) && this.compositeChildMatchingScore == 0.99) {
			return 1;
		}
		if(this.identicalCommentsInBody && !this.isMatchedWithNullReplacements() && !o.identicalCommentsInBody && this.compositeChildMatchingScore > 0) {
			return -1;
		}
		if(o.identicalCommentsInBody && !o.isMatchedWithNullReplacements() && !this.identicalCommentsInBody && o.compositeChildMatchingScore > 0) {
			return 1;
		}
		if(this.compositeChildMatchingScore >= 2.0*o.compositeChildMatchingScore ||
				o.compositeChildMatchingScore >= 2.0*this.compositeChildMatchingScore) {
			return -Double.compare(this.compositeChildMatchingScore, o.compositeChildMatchingScore);
		}
		double distance1 = this.editDistance();
		double distance2 = o.editDistance();
		boolean depthVSDistanceConflict = distance1 < distance2 && Math.abs(distance1 - distance2) < 0.02 &&
				Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth()) > 0 &&
				Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth()) == 0;
		if(distance1 != distance2 && !replacementsOnSameASTNodes(o) && !depthVSDistanceConflict) {
			if(this.isIdenticalWithExtractedVariable() && !o.isIdenticalWithExtractedVariable()) {
				return -1;
			}
			else if(!this.isIdenticalWithExtractedVariable() && o.isIdenticalWithExtractedVariable()) {
				return 1;
			}
			if(this.isIdenticalWithInlinedVariable() && !o.isIdenticalWithInlinedVariable()) {
				return -1;
			}
			else if(!this.isIdenticalWithInlinedVariable() && o.isIdenticalWithInlinedVariable()) {
				return 1;
			}
			return Double.compare(distance1, distance2);
		}
		else {
			int identicalCompositeChildren1 = this.numberOfIdenticalCompositeChildren();
			int identicalCompositeChildren2 = o.numberOfIdenticalCompositeChildren();
			if(identicalCompositeChildren1 > identicalCompositeChildren2) {
				return -1;
			}
			else if(identicalCompositeChildren1 < identicalCompositeChildren2) {
				return 1;
			}
			if(this.compositeChildMatchingScore != o.compositeChildMatchingScore) {
				return -Double.compare(this.compositeChildMatchingScore, o.compositeChildMatchingScore);
			}
			else {
				int identicalDirectlyNestedChildren1 = this.numberOfIdenticalDirectlyNestedChildren();
				int identicalDirectlyNestedChildren2 = o.numberOfIdenticalDirectlyNestedChildren();
				if(identicalDirectlyNestedChildren1 > identicalDirectlyNestedChildren2) {
					return -1;
				}
				else if(identicalDirectlyNestedChildren1 < identicalDirectlyNestedChildren2) {
					return 1;
				}
				int depthDiff1 = Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth());
				int depthDiff2 = Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth());

				if(depthDiff1 != depthDiff2) {
					return Integer.valueOf(depthDiff1).compareTo(Integer.valueOf(depthDiff2));
				}
				else {
					if(this.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
							this.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
							o.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
							o.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
						List<VariableDeclaration> thisVariableDeclarations1 = this.getFragment1().getVariableDeclarations();
						List<VariableDeclaration> thisVariableDeclarations2 = this.getFragment2().getVariableDeclarations();
						boolean equalType1 = false;
						if(thisVariableDeclarations1.size() == 1 && thisVariableDeclarations2.size() == 1 &&
								thisVariableDeclarations1.get(0).getType() != null && thisVariableDeclarations2.get(0).getType() != null &&
								thisVariableDeclarations1.get(0).getType().equals(thisVariableDeclarations2.get(0).getType())) {
							equalType1 = true;
						}
						List<VariableDeclaration> otherVariableDeclarations1 = o.getFragment1().getVariableDeclarations();
						List<VariableDeclaration> otherVariableDeclarations2 = o.getFragment2().getVariableDeclarations();
						boolean equalType2 = false;
						if(otherVariableDeclarations1.size() == 1 && otherVariableDeclarations2.size() == 1 &&
								otherVariableDeclarations1.get(0).getType() != null && otherVariableDeclarations2.get(0).getType() != null &&
								otherVariableDeclarations1.get(0).getType().equals(otherVariableDeclarations2.get(0).getType())) {
							equalType2 = true;
						}
						if(equalType1 && !equalType2) {
							return -1;
						}
						if(!equalType1 && equalType2) {
							return 1;
						}
					}
					int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
					int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
					if(indexDiff1 != indexDiff2) {
						return Integer.valueOf(indexDiff1).compareTo(Integer.valueOf(indexDiff2));
					}
					else {
						int locationSum1 = this.getFragment1().getLocationInfo().getStartLine() + this.getFragment2().getLocationInfo().getStartLine();
						int locationSum2 = o.getFragment1().getLocationInfo().getStartLine() + o.getFragment2().getLocationInfo().getStartLine();
						return Integer.valueOf(locationSum1).compareTo(Integer.valueOf(locationSum2));
					}
				}
			}
		}
	}

	private boolean replacementsOnSameASTNodes(CompositeStatementObjectMapping o) {
		Set<Replacement> thisReplacements = this.getReplacements();
		Set<Replacement> otherReplacements = o.getReplacements();
		if(thisReplacements.size() == otherReplacements.size()) {
			Iterator<Replacement> thisIterator = thisReplacements.iterator();
			Iterator<Replacement> otherIterator = otherReplacements.iterator();
			int identicalBefore = 0;
			int identicalAfter = 0;
			while(thisIterator.hasNext()) {
				Replacement thisReplacement = thisIterator.next();
				Replacement otherReplacement = otherIterator.next();
				if(!(thisReplacement instanceof IntersectionReplacement) && !(thisReplacement instanceof CompositeReplacement) && !thisReplacement.getType().equals(ReplacementType.TYPE)) {
					if(thisReplacement.getBefore().equals(otherReplacement.getBefore()) && thisReplacement.getType().equals(otherReplacement.getType())) {
						identicalBefore++;
					}
					if(thisReplacement.getAfter().equals(otherReplacement.getAfter()) && thisReplacement.getType().equals(otherReplacement.getType())) {
						identicalAfter++;
					}
				}
			}
			if(identicalBefore == thisReplacements.size() || identicalAfter == thisReplacements.size()) {
				return true;
			}
		}
		return false;
	}

	public double editDistance() {
		double distance1;
		if(this.getFragment1().getString().equals(this.getFragment2().getString())) {
			distance1 = 0;
		}
		else {
			String s1 = this.getFragment1().getString().toLowerCase();
			String s2 = this.getFragment2().getString().toLowerCase();
			int distance = StringDistance.editDistance(s1, s2);
			distance1 = (double)distance/(double)Math.max(s1.length(), s2.length());
		}
		return distance1;
	}

	private int numberOfIdenticalCompositeChildren() {
		int count = 0;
		CompositeStatementObject comp1 = (CompositeStatementObject)getFragment1();
		CompositeStatementObject comp2 = (CompositeStatementObject)getFragment2();
		while(comp1.getStatements().size() >= 1 && comp2.getStatements().size() >= 1 &&
				comp1.getStatements().get(0) instanceof CompositeStatementObject && comp2.getStatements().get(0) instanceof CompositeStatementObject) {
			CompositeStatementObject nestedComp1 = (CompositeStatementObject)comp1.getStatements().get(0);
			CompositeStatementObject nestedComp2 = (CompositeStatementObject)comp2.getStatements().get(0);
			String s1 = nestedComp1.getString();
			String s2 = nestedComp2.getString();
			if(s1.equals(s2)) {
				count++;
			}
			comp1 = nestedComp1;
			comp2 = nestedComp2;
		}
		return count;
	}

	private int numberOfIdenticalDirectlyNestedChildren() {
		CompositeStatementObject comp1 = (CompositeStatementObject)getFragment1();
		CompositeStatementObject comp2 = (CompositeStatementObject)getFragment2();
		if((comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) ||
				(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) && comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) ||
				(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) && comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) ||
				(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) && comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK))) {
			return numberOfIdenticalDirectlyNestedChildren(comp1, comp2);
		}
		else if(comp1.getStatements().size() == 1 && comp2.getStatements().size() == 1 &&
				comp1.getStatements().get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
				comp2.getStatements().get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			return numberOfIdenticalDirectlyNestedChildren((CompositeStatementObject)comp1.getStatements().get(0), (CompositeStatementObject)comp2.getStatements().get(0));
		}
		return 0;
	}

	private int numberOfIdenticalDirectlyNestedChildren(CompositeStatementObject comp1, CompositeStatementObject comp2) {
		int count = 0;
		List<AbstractStatement> statements1 = comp1.getStatements();
		List<AbstractStatement> statements2 = comp2.getStatements();
		for(AbstractStatement statement1 : statements1) {
			String s1 = statement1.getString();
			for(AbstractStatement statement2 : statements2) {
				String s2 = statement2.getString();
				if(s1.equals(s2)) {
					count++;
				}
			}
		}
		return count;
	}
}
