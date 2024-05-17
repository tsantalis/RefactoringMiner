package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.Constants.JAVA;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONDITIONAL_PATTERN;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.subConditionIntersection;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.hasElseBranch;
import static gr.uom.java.xmi.decomposition.UMLOperationBodyMapper.extractCommentsWithinStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.StringDistance;

public class LeafMapping extends AbstractCodeMapping implements Comparable<LeafMapping> {
	private List<Double> levelParentEditDistance;
	private boolean identicalPreviousStatement;
	private boolean identicalPreviousAndNextStatement;
	private boolean equalNumberOfAssertions;
	private boolean ifParentWithIdenticalElse;
	private boolean ifParentWithIdenticalThen;

	public LeafMapping(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
		super(statement1, statement2, operation1, operation2);
		CompositeStatementObject parent1 = statement1.getParent();
		CompositeStatementObject parent2 = statement2.getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
			parent1 = parent1.getParent(); 
		}
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
			parent2 = parent2.getParent(); 
		}
		if(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			if(parent1.getStatements().size() > 0 && parent2.getStatements().size() > 0 &&
					parent1.getStatements().get(0) instanceof CompositeStatementObject &&
					parent2.getStatements().get(0) instanceof CompositeStatementObject) {
				CompositeStatementObject ifBlock1 = (CompositeStatementObject) parent1.getStatements().get(0);
				CompositeStatementObject ifBlock2 = (CompositeStatementObject) parent2.getStatements().get(0);
				if(ifBlock1.stringRepresentation().equals(ifBlock2.stringRepresentation()) && !ifBlock1.isBlockReturningDefault()) {
					ifParentWithIdenticalThen = true;
				}
			}
			if(hasElseBranch(parent1) && hasElseBranch(parent2)) {
				CompositeStatementObject elseBlock1 = (CompositeStatementObject) parent1.getStatements().get(1);
				CompositeStatementObject elseBlock2 = (CompositeStatementObject) parent2.getStatements().get(1);
				if(elseBlock1.stringRepresentation().equals(elseBlock2.stringRepresentation()) && !elseBlock1.isBlockReturningDefault()) {
					ifParentWithIdenticalElse = true;
				}
			}
		}
	}

	public boolean hasIdenticalPreviousAndNextStatement() {
		if(onlyStatementWithinIdenticalComposite()) {
			return true;
		}
		return identicalPreviousAndNextStatement;
	}

	public void setEqualNumberOfAssertions(boolean equalNumberOfAssertions) {
		this.equalNumberOfAssertions = equalNumberOfAssertions;
	}

	@Override
	public int compareTo(LeafMapping o) {
		CompositeReplacement compositeReplacement1 = this.containsCompositeReplacement();
		CompositeReplacement compositeReplacement2 = o.containsCompositeReplacement();
		if(compositeReplacement1 != null || compositeReplacement2 != null) {
			if(compositeReplacement1 != null && compositeReplacement2 == null) {
				return -1;
			}
			else if(compositeReplacement1 == null && compositeReplacement2 != null) {
				return 1;
			}
			else {
				return -Integer.compare(compositeReplacement1.getTotalAdditionallyMatchedStatements(),
						compositeReplacement2.getTotalAdditionallyMatchedStatements());
			}
		}
		else {
			double distance1 = this.editDistance();
			double distance2 = o.editDistance();
			if(distance1 != distance2) {
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
				if(this.isFieldAssignmentWithParameter() && o.isFieldAssignmentWithParameter()) {
					if(this.isFieldAssignmentWithParameterHavingSameType() && !o.isFieldAssignmentWithParameterHavingSameType()) {
						return -1;
					}
					else if(!this.isFieldAssignmentWithParameterHavingSameType() && o.isFieldAssignmentWithParameterHavingSameType()) {
						return 1;
					}
				}
				computeIdenticalPreviousAndNextStatements(o);
				if(this.identicalPreviousAndNextStatement && !o.identicalPreviousAndNextStatement) {
					return -1;
				}
				else if(!this.identicalPreviousAndNextStatement && o.identicalPreviousAndNextStatement) {
					return 1;
				}
				if(this.identicalPreviousStatement && !o.identicalPreviousStatement) {
					return -1;
				}
				else if(!this.identicalPreviousStatement && o.identicalPreviousStatement) {
					return 1;
				}
				if(this.identicalDepthIndexAndParentType() && !o.identicalDepthIndexAndParentType()) {
					return -1;
				}
				else if(!this.identicalDepthIndexAndParentType() && o.identicalDepthIndexAndParentType()) {
					return 1;
				}
				if(this.referencesMapping(o)) {
					return 1;
				}
				else if(o.referencesMapping(this)) {
					return -1;
				}
				Set<ReplacementType> thisReplacementTypes = this.getReplacementTypes();
				Set<ReplacementType> otherReplacementTypes = o.getReplacementTypes();
				Set<ReplacementType> intersection = new LinkedHashSet<>(thisReplacementTypes);
				intersection.retainAll(otherReplacementTypes);
				if(intersection.size() > 0 && (intersection.equals(thisReplacementTypes) || intersection.equals(otherReplacementTypes))) {
					Set<ReplacementType> thisReplacementTypesWithoutCommon = new LinkedHashSet<>(thisReplacementTypes);
					thisReplacementTypesWithoutCommon.removeAll(intersection);
					Set<ReplacementType> otherReplacementTypesWithoutCommon = new LinkedHashSet<>(otherReplacementTypes);
					otherReplacementTypesWithoutCommon.removeAll(intersection);
					int sameReplacementCount = 0;
					for(ReplacementType type : intersection) {
						String before = null, after = null;
						for(Replacement r : this.getReplacements()) {
							if(r.getType().equals(type)) {
								before = r.getBefore();
								after = r.getAfter();
								break;
							}
						}
						for(Replacement r : o.getReplacements()) {
							if(r.getType().equals(type)) {
								if(before.equals(r.getBefore()) || after.equals(r.getAfter())) {
									sameReplacementCount++;
									break;
								}
							}
						}
					}
					int identicalNodesCount = 0;
					for(ReplacementType type : thisReplacementTypesWithoutCommon) {
						if(type.equals(ReplacementType.STRING_LITERAL) && o.getFragment1().getStringLiterals().equals(o.getFragment2().getStringLiterals()) && o.getFragment1().getStringLiterals().size() > 0) {
							identicalNodesCount++;
						}
						else if(type.equals(ReplacementType.NUMBER_LITERAL) && o.getFragment1().getNumberLiterals().equals(o.getFragment2().getNumberLiterals()) && o.getFragment1().getNumberLiterals().size() > 0) {
							identicalNodesCount++;
						}
					}
					for(ReplacementType type : otherReplacementTypesWithoutCommon) {
						if(type.equals(ReplacementType.STRING_LITERAL) && this.getFragment1().getStringLiterals().equals(this.getFragment2().getStringLiterals()) && this.getFragment1().getStringLiterals().size() > 0) {
							identicalNodesCount++;
						}
						else if(type.equals(ReplacementType.NUMBER_LITERAL) && this.getFragment1().getNumberLiterals().equals(this.getFragment2().getNumberLiterals()) && this.getFragment1().getNumberLiterals().size() > 0) {
							identicalNodesCount++;
						}
					}
					boolean identicalNodes = false;
					if(thisReplacementTypesWithoutCommon.size() > 0) {
						identicalNodes = identicalNodesCount == thisReplacementTypesWithoutCommon.size();
					}
					else if(otherReplacementTypesWithoutCommon.size() > 0) {
						identicalNodes = identicalNodesCount == otherReplacementTypesWithoutCommon.size();
					}
					if(sameReplacementCount == intersection.size() && identicalNodes) {
						if(intersection.equals(thisReplacementTypes) && !intersection.equals(otherReplacementTypes)) {
							return -1;
						}
						else if(intersection.equals(otherReplacementTypes) && !intersection.equals(thisReplacementTypes)) {
							return 1;
						}
					}
					if(thisReplacementTypes.equals(otherReplacementTypes) && thisReplacementTypes.size() == 1 && thisReplacementTypes.iterator().next().equals(ReplacementType.TYPE)) {
						int thisCompositeTypeReplacements = 0;
						for(Replacement r : this.getReplacements()) {
							if(r.getBefore().contains(".") && r.getBefore().endsWith("." + r.getAfter())) {
								thisCompositeTypeReplacements++;
							}
							else if(r.getAfter().contains(".") && r.getAfter().endsWith("." + r.getBefore())) {
								thisCompositeTypeReplacements++;
							}
						}
						int otherCompositeTypeReplacements = 0;
						for(Replacement r : o.getReplacements()) {
							if(r.getBefore().contains(".") && r.getBefore().endsWith("." + r.getAfter())) {
								otherCompositeTypeReplacements++;
							}
							else if(r.getAfter().contains(".") && r.getAfter().endsWith("." + r.getBefore())) {
								otherCompositeTypeReplacements++;
							}
						}
						if(thisCompositeTypeReplacements == this.getReplacements().size() && otherCompositeTypeReplacements != o.getReplacements().size()) {
							return -1;
						}
						else if(thisCompositeTypeReplacements != this.getReplacements().size() && otherCompositeTypeReplacements == o.getReplacements().size()) {
							return 1;
						}
					}
				}
				if(intersection.size() == 1 && intersection.contains(ReplacementType.STRING_LITERAL) && this.stringLiteralRatio() > 0.5 && o.stringLiteralRatio() > 0.5) {
					return Double.compare(distance1, distance2);
				}
				List<Double> levelParentEditDistance1 = this.levelParentEditDistance();
				List<Double> levelParentEditDistance2 = o.levelParentEditDistance();
				double nLevelParentEditDistance1 = 0, nLevelParentEditDistance2 = 0;
				int minSize = Math.min(levelParentEditDistance1.size(), levelParentEditDistance2.size());
				int headZeros1 = 0;
				int headZeros2 = 0;
				for(int i=0; i<minSize; i++) {
					double d1 = levelParentEditDistance1.get(i);
					nLevelParentEditDistance1 += d1;
					if(d1 == 0 && nLevelParentEditDistance1 == 0) {
						headZeros1++;
					}
					double d2 = levelParentEditDistance2.get(i);
					nLevelParentEditDistance2 += d2;
					if(d2 == 0 && nLevelParentEditDistance2 == 0) {
						headZeros2++;
					}
				}
				if(levelParentEditDistance1.size() > 2 && levelParentEditDistance2.size() > 2 &&
						((levelParentEditDistance1.contains(0.0) && !levelParentEditDistance2.contains(0.0)) ||
						(levelParentEditDistance2.contains(0.0) && !levelParentEditDistance1.contains(0.0))) &&
						(levelParentEditDistance1.get(0) == nLevelParentEditDistance1 ||
						levelParentEditDistance2.get(0) == nLevelParentEditDistance2)) {
					if(nLevelParentEditDistance1 < nLevelParentEditDistance2 && !levelParentEditDistance2.get(0).equals(0.0)) {
						return -1;
					}
					else if(nLevelParentEditDistance2 < nLevelParentEditDistance1 && !levelParentEditDistance1.get(0).equals(0.0)) {
						return 1;
					}
					if(headZeros1 > headZeros2) {
						return -1;
					}
					else if(headZeros2 > headZeros1) {
						return 1;
					}
				}
				if(equalNumberOfAssertions && this.getFragment1().isAssertCall() && this.getFragment2().isAssertCall() && o.getFragment1().isAssertCall() && o.getFragment2().isAssertCall()) {
					if(distance1 < distance2 && thisReplacementTypes.size() < otherReplacementTypes.size()) {
						return Double.compare(distance1, distance2);
					}
					else if(distance1 > distance2 && thisReplacementTypes.size() > otherReplacementTypes.size()) {
						return Double.compare(distance1, distance2);
					}
					int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
					int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
					if(indexDiff1 != indexDiff2) {
						return Integer.valueOf(indexDiff1).compareTo(Integer.valueOf(indexDiff2));
					}
				}
				if(this.getMatchingArgumentsWithOperationInvocation() != o.getMatchingArgumentsWithOperationInvocation()) {
					return -Integer.valueOf(this.getMatchingArgumentsWithOperationInvocation()).compareTo(Integer.valueOf(o.getMatchingArgumentsWithOperationInvocation()));
				}
				int typeIntersection1 = 0;
				int identicalTypeIntersection1 = 0;
				for(String type1 : this.getFragment1().getTypes()) {
					for(String type2 : this.getFragment2().getTypes()) {
						if(type1.equals(type2) || type1.endsWith(type2) || type2.endsWith(type1)) {
							typeIntersection1++;
						}
						if(type1.equals(type2)) {
							identicalTypeIntersection1++;
						}
					}
				}
				int typeIntersection2 = 0;
				int identicalTypeIntersection2 = 0;
				for(String type1 : o.getFragment1().getTypes()) {
					for(String type2 : o.getFragment2().getTypes()) {
						if(type1.equals(type2) || type1.endsWith(type2) || type2.endsWith(type1)) {
							typeIntersection2++;
						}
						if(type1.equals(type2)) {
							identicalTypeIntersection2++;
						}
					}
				}
				if(typeIntersection1 > 0 && typeIntersection2 == 0 && o.getFragment1().getTypes().size() > 0 && o.getFragment2().getTypes().size() > 0 && identicalTypeIntersection1 == 0 && identicalTypeIntersection2 == 0) {
					return -1;
				}
				else if(typeIntersection2 > 0 && typeIntersection1 == 0 && this.getFragment1().getTypes().size() > 0 && this.getFragment2().getTypes().size() > 0 && identicalTypeIntersection1 == 0 && identicalTypeIntersection2 == 0) {
					return 1;
				}
				return Double.compare(distance1, distance2);
			}
			else {
				if(this.isIdenticalWithExtractedVariable() && !o.isIdenticalWithExtractedVariable()) {
					return -1;
				}
				else if(!this.isIdenticalWithExtractedVariable() && o.isIdenticalWithExtractedVariable()) {
					return 1;
				}
				else if(this.isIdenticalWithExtractedVariable() && o.isIdenticalWithExtractedVariable()) {
					boolean thisIdenticalCommentsInParent = this.identicalCommentsInParent();
					boolean otherIdenticalCommentsInParent = o.identicalCommentsInParent();
					if(thisIdenticalCommentsInParent && !otherIdenticalCommentsInParent) {
						return -1;
					}
					else if(!thisIdenticalCommentsInParent && otherIdenticalCommentsInParent) {
						return 1;
					}
				}
				if(this.isIdenticalWithInlinedVariable() && !o.isIdenticalWithInlinedVariable()) {
					return -1;
				}
				else if(!this.isIdenticalWithInlinedVariable() && o.isIdenticalWithInlinedVariable()) {
					return 1;
				}
				List<Double> levelParentEditDistance1 = this.levelParentEditDistance();
				List<Double> levelParentEditDistance2 = o.levelParentEditDistance();
				double nLevelParentEditDistance1 = 0, nLevelParentEditDistance2 = 0;
				int minSize = Math.min(levelParentEditDistance1.size(), levelParentEditDistance2.size());
				int headZeros1 = 0;
				int headZeros2 = 0;
				for(int i=0; i<minSize; i++) {
					double d1 = levelParentEditDistance1.get(i);
					nLevelParentEditDistance1 += d1;
					if(d1 == 0 && nLevelParentEditDistance1 == 0) {
						headZeros1++;
					}
					double d2 = levelParentEditDistance2.get(i);
					nLevelParentEditDistance2 += d2;
					if(d2 == 0 && nLevelParentEditDistance2 == 0) {
						headZeros2++;
					}
				}
				boolean identicalCompositeChildren1 = this.identicalCompositeChildrenStructure();
				boolean identicalCompositeChildren2 = o.identicalCompositeChildrenStructure();
				boolean zeroDistanceWithMoreThanTwoParents1 = nLevelParentEditDistance1 == 0 && levelParentEditDistance1.size() > 2;
				boolean zeroDistanceWithMoreThanTwoParents2 = nLevelParentEditDistance2 == 0 && levelParentEditDistance2.size() > 2;
				if(identicalCompositeChildren1 && !identicalCompositeChildren2 && !zeroDistanceWithMoreThanTwoParents2) {
					return -1;
				}
				else if(!identicalCompositeChildren1 && identicalCompositeChildren2 && !zeroDistanceWithMoreThanTwoParents1) {
					return 1;
				}
				if(levelParentEditDistance1.size() == levelParentEditDistance2.size()) {
					if(nLevelParentEditDistance1 == 0 && nLevelParentEditDistance2 > 0) { 
						return -1; 
					} 
					else if(nLevelParentEditDistance2 == 0 && nLevelParentEditDistance1 > 0) { 
						return 1; 
					}
					if(headZeros1 > headZeros2) {
						return -1;
					}
					else if(headZeros2 > headZeros1) {
						return 1;
					}
					if(levelParentEditDistance1.size() == 2 && levelParentEditDistance1.get(1).equals(0.0) &&
							levelParentEditDistance2.size() == 2 && levelParentEditDistance2.get(1).equals(0.0) &&
							!levelParentEditDistance1.get(0).equals(levelParentEditDistance2.get(0))) {
						double difference = Math.abs(levelParentEditDistance1.get(0) - levelParentEditDistance2.get(0));
						double min = Math.min(levelParentEditDistance1.get(0), levelParentEditDistance2.get(0));
						if(difference > min) {
							return Double.compare(levelParentEditDistance1.get(0), levelParentEditDistance2.get(0));
						}
					}
				}
				if((levelParentEditDistance1.size() != levelParentEditDistance2.size() ||
						(levelParentEditDistance1.contains(0.0) && !levelParentEditDistance2.contains(0.0)) ||
						(levelParentEditDistance2.contains(0.0) && !levelParentEditDistance1.contains(0.0))) &&
						!levelParentEditDistance1.get(0).equals(levelParentEditDistance2.get(0))) {
					if(nLevelParentEditDistance1 < nLevelParentEditDistance2 && !levelParentEditDistance2.get(0).equals(0.0) && !o.ifParentWithIdenticalElse) {
						return -1;
					}
					else if(nLevelParentEditDistance2 < nLevelParentEditDistance1 && !levelParentEditDistance1.get(0).equals(0.0) && !this.ifParentWithIdenticalElse) {
						return 1;
					}
					if(headZeros1 > headZeros2) {
						return -1;
					}
					else if(headZeros2 > headZeros1) {
						return 1;
					}
				}
				if(this.ifParentWithIdenticalElse && !o.ifParentWithIdenticalElse) {
					return -1;
				}
				else if(o.ifParentWithIdenticalElse && !this.ifParentWithIdenticalElse) {
					return 1;
				}
				if(this.ifParentWithIdenticalThen && !o.ifParentWithIdenticalThen) {
					return -1;
				}
				else if(o.ifParentWithIdenticalThen && !this.ifParentWithIdenticalThen) {
					return 1;
				}
				int depthDiff1 = Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth());
				int depthDiff2 = Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth());
				if(depthDiff1 != depthDiff2) {
					return Integer.valueOf(depthDiff1).compareTo(Integer.valueOf(depthDiff2));
				}
				else {
					int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
					int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
					if(indexDiff1 != indexDiff2) {
						computeIdenticalPreviousAndNextStatements(o);
						if(this.identicalPreviousAndNextStatement && !o.identicalPreviousAndNextStatement) {
							return -1;
						}
						else if(!this.identicalPreviousAndNextStatement && o.identicalPreviousAndNextStatement) {
							return 1;
						}
						return Integer.valueOf(indexDiff1).compareTo(Integer.valueOf(indexDiff2));
					}
					else {
						double parentEditDistance1 = levelParentEditDistance1.get(0);
						double parentEditDistance2 = levelParentEditDistance2.get(0);
						boolean sameVariableDeclarationTypeInParent1 = this.sameVariableDeclarationTypeInParent();
						boolean sameVariableDeclarationTypeInParent2 = o.sameVariableDeclarationTypeInParent();
						if(parentEditDistance1 >= 0 && parentEditDistance2 >= 0 && sameVariableDeclarationTypeInParent1 != sameVariableDeclarationTypeInParent2) {
							if(sameVariableDeclarationTypeInParent1 && !sameVariableDeclarationTypeInParent2) {
								return -1;
							}
							if(!sameVariableDeclarationTypeInParent1 && sameVariableDeclarationTypeInParent2) {
								return 1;
							}
						}
						Set<String> set1 = this.parentVariableTokenIntersection();
						Set<String> set2 = o.parentVariableTokenIntersection();
						if(parentEditDistance1 >= 0 && parentEditDistance2 >= 0 && set1.size() != set2.size()) {
							if(set1.size() > set2.size()) {
								return -1;
							}
							else if(set1.size() < set2.size()) {
								return 1;
							}
						}
						boolean directParentsReferenceSameVariables1 = this.directParentsReferenceSameVariables();
						boolean directParentsReferenceSameVariables2 = o.directParentsReferenceSameVariables();
						if(parentEditDistance1 >= 0 && parentEditDistance2 >= 0 && directParentsReferenceSameVariables1 != directParentsReferenceSameVariables2) {
							if(directParentsReferenceSameVariables1 && !directParentsReferenceSameVariables2) {
								return -1;
							}
							if(!directParentsReferenceSameVariables1 && directParentsReferenceSameVariables2) {
								return 1;
							}
						}
						int commonConditionalsInParent1 = this.commonConditionalsInParent();
						int commonConditionalsInParent2 = o.commonConditionalsInParent();
						if(parentEditDistance1 >= 0 && parentEditDistance2 >= 0 && commonConditionalsInParent1 != commonConditionalsInParent2) {
							if(commonConditionalsInParent1 > commonConditionalsInParent2) {
								return -1;
							}
							else if(commonConditionalsInParent1 < commonConditionalsInParent2) {
								return 1;
							}
						}
						if(this.equalContainer() && o.equalContainer() &&
								levelParentEditDistance1.size() == 2 && levelParentEditDistance1.get(1).equals(0.0) &&
								levelParentEditDistance2.size() == 2 && levelParentEditDistance2.get(1).equals(0.0) &&
								!levelParentEditDistance1.get(0).equals(levelParentEditDistance2.get(0))) {
							int parentIndexDiff1 = this.parentIndexDiff();
							int parentIndexDiff2 = o.parentIndexDiff();
							if(parentIndexDiff1 != parentIndexDiff2) {
								return Integer.valueOf(parentIndexDiff1).compareTo(Integer.valueOf(parentIndexDiff2));
							}
						}
						if(parentEditDistance1 == parentEditDistance2) {
							computeIdenticalPreviousAndNextStatements(o);
							if(this.identicalPreviousAndNextStatement && !o.identicalPreviousAndNextStatement) {
								return -1;
							}
							else if(!this.identicalPreviousAndNextStatement && o.identicalPreviousAndNextStatement) {
								return 1;
							}
							boolean underElse1 = this.underElse();
							boolean underElse2 = o.underElse();
							if(underElse1 && !underElse2) {
								return -1;
							}
							else if(!underElse1 && underElse2) {
								return 1;
							}
							int locationSum1 = this.getFragment1().getLocationInfo().getStartLine() + this.getFragment2().getLocationInfo().getStartLine();
							int locationSum2 = o.getFragment1().getLocationInfo().getStartLine() + o.getFragment2().getLocationInfo().getStartLine();
							return Integer.valueOf(locationSum1).compareTo(Integer.valueOf(locationSum2));
						}
						return Double.compare(parentEditDistance1, parentEditDistance2);
					}
				}
			}
		}
	}

	private boolean underElse() {
		return isUnderElseBranch(getFragment1()) && isUnderElseBranch(getFragment2());
	}
	
	private boolean isUnderElseBranch(AbstractCodeFragment fragment) {
		if(fragment.getParent() != null) {
			CompositeStatementObject child = fragment.getParent();
			if(child.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && child.getParent() != null) {
				CompositeStatementObject parent = child.getParent();
				return parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						child.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						parent.getStatements().size() == 2 && parent.getStatements().indexOf(child) == 1;
			}
			else if(child.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				return child.getStatements().size() == 2 && child.getStatements().indexOf(fragment) == 1;
			}
		}
		return false;
	}

	private boolean onlyStatementWithinIdenticalComposite() {
		//handle the case of a single statement within a block
		CompositeStatementObject thisComp1 = this.getFragment1().getParent();
		CompositeStatementObject thisComp2 = this.getFragment2().getParent();
		if(thisComp1 != null && this.getFragment1().getIndex() == 0 && this.getFragment1().getIndex() == thisComp1.getStatements().size()-1 &&
				thisComp2 != null && this.getFragment2().getIndex() == 0 && this.getFragment2().getIndex() == thisComp2.getStatements().size()-1) {
			if(thisComp1.getString().equals(thisComp2.getString())) {
				if(thisComp1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && thisComp2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
					if(thisComp1.getParent() != null && thisComp2.getParent() != null && thisComp1.getParent().getString().equals(thisComp2.getParent().getString())) {
						return true;
					}
				}
				else {
					return true;
				}
			}
		}
		return false;
	}

	private void computeIdenticalPreviousAndNextStatements(LeafMapping o) {
		CompositeStatementObject thisComp1 = this.getFragment1().getParent();
		CompositeStatementObject thisComp2 = this.getFragment2().getParent();
		CompositeStatementObject oComp1 = o.getFragment1().getParent();
		CompositeStatementObject oComp2 = o.getFragment2().getParent();
		boolean lastStatement1 = thisComp1 != null && this.getFragment1().getIndex() > 0 && this.getFragment1().getIndex() == thisComp1.getStatements().size()-1 &&
				oComp1 != null && o.getFragment1().getIndex() > 0 && o.getFragment1().getIndex() == oComp1.getStatements().size()-1;
		boolean lastStatement2 = thisComp2 != null && this.getFragment2().getIndex() > 0 && this.getFragment2().getIndex() == thisComp2.getStatements().size()-1 &&
				oComp2 != null && o.getFragment2().getIndex() > 0 && o.getFragment2().getIndex() == oComp2.getStatements().size()-1;
		if(thisComp1 != null && this.getFragment1().getIndex() > 0 && this.getFragment1().getIndex() < thisComp1.getStatements().size()-1 &&
				thisComp2 != null && this.getFragment2().getIndex() > 0 && this.getFragment2().getIndex() < thisComp2.getStatements().size()-1 &&
				oComp1 != null && o.getFragment1().getIndex() > 0 && o.getFragment1().getIndex() < oComp1.getStatements().size()-1 &&
				oComp2 != null && o.getFragment2().getIndex() > 0 && o.getFragment2().getIndex() < oComp2.getStatements().size()-1) {
			AbstractCodeFragment thisPrevious1 = thisComp1.getStatements().get(this.getFragment1().getIndex()-1);
			AbstractCodeFragment thisNext1 = thisComp1.getStatements().get(this.getFragment1().getIndex()+1);
			AbstractCodeFragment thisPrevious2 = thisComp2.getStatements().get(this.getFragment2().getIndex()-1);
			AbstractCodeFragment thisNext2 = thisComp2.getStatements().get(this.getFragment2().getIndex()+1);
			
			AbstractCodeFragment oPrevious1 = oComp1.getStatements().get(o.getFragment1().getIndex()-1);
			AbstractCodeFragment oNext1 = oComp1.getStatements().get(o.getFragment1().getIndex()+1);
			AbstractCodeFragment oPrevious2 = oComp2.getStatements().get(o.getFragment2().getIndex()-1);
			AbstractCodeFragment oNext2 = oComp2.getStatements().get(o.getFragment2().getIndex()+1);
			
			boolean thisEqualPrevious = thisPrevious1.getString().equals(thisPrevious2.getString());
			boolean thisEqualNext = thisNext1.getString().equals(thisNext2.getString());
			boolean thisEqualPreviousAndNext = thisEqualPrevious && thisEqualNext;
			boolean oEqualPrevious = oPrevious1.getString().equals(oPrevious2.getString());
			boolean oEqualNext = oNext1.getString().equals(oNext2.getString());
			boolean oEqualPreviousAndNext = oEqualPrevious && oEqualNext;
			boolean thisVariableDeclarationsWithSameName = false;
			if(this.getFragment1().getVariableDeclarations().size() > 0 && this.getFragment2().getVariableDeclarations().size() > 0) {
				if(this.getFragment1().getVariableDeclarations().get(0).getVariableName().equals(this.getFragment2().getVariableDeclarations().get(0).getVariableName())) {
					thisVariableDeclarationsWithSameName = true;
				}
			}
			boolean oVariableDeclarationsWithSameName = false;
			if(o.getFragment1().getVariableDeclarations().size() > 0 && o.getFragment2().getVariableDeclarations().size() > 0) {
				if(o.getFragment1().getVariableDeclarations().get(0).getVariableName().equals(o.getFragment2().getVariableDeclarations().get(0).getVariableName())) {
					oVariableDeclarationsWithSameName = true;
				}
			}
			if(thisEqualPreviousAndNext) {
				this.identicalPreviousAndNextStatement = true;
			}
			else if(thisEqualPrevious && thisNext2 != null && thisNext2.equals(o.getFragment2()) && !oEqualNext && !oVariableDeclarationsWithSameName) {
				this.identicalPreviousStatement = true;
			}
			if(oEqualPreviousAndNext) {
				o.identicalPreviousAndNextStatement = true;
			}
			else if(oEqualPrevious && oNext2 != null && oNext2.equals(this.getFragment2()) && !thisEqualNext && !thisVariableDeclarationsWithSameName) {
				o.identicalPreviousStatement = true;
			}
		}
		// last statement in block
		else if(lastStatement1 && lastStatement2) {
			AbstractCodeFragment thisPrevious1 = thisComp1.getStatements().get(this.getFragment1().getIndex()-1);
			AbstractCodeFragment thisPrevious2 = thisComp2.getStatements().get(this.getFragment2().getIndex()-1);
			
			AbstractCodeFragment oPrevious1 = oComp1.getStatements().get(o.getFragment1().getIndex()-1);
			AbstractCodeFragment oPrevious2 = oComp2.getStatements().get(o.getFragment2().getIndex()-1);
			
			boolean thisEqualPrevious = thisPrevious1.getString().equals(thisPrevious2.getString());
			if(thisEqualPrevious) {
				this.identicalPreviousAndNextStatement = true;
			}
			boolean oEqualPrevious = oPrevious1.getString().equals(oPrevious2.getString());
			if(oEqualPrevious) {
				o.identicalPreviousAndNextStatement = true;
			}
		}
		// the second is last statement in block
		else if(lastStatement2 && !lastStatement1 &&
				this.getFragment1().getIndex() > 0 && this.getFragment1().getIndex() < thisComp1.getStatements().size()-1 &&
				o.getFragment1().getIndex() > 0 && o.getFragment1().getIndex() < oComp1.getStatements().size()-1) {
			AbstractCodeFragment thisPrevious1 = thisComp1.getStatements().get(this.getFragment1().getIndex()-1);
			AbstractCodeFragment thisPrevious2 = thisComp2.getStatements().get(this.getFragment2().getIndex()-1);
			
			AbstractCodeFragment oPrevious1 = oComp1.getStatements().get(o.getFragment1().getIndex()-1);
			AbstractCodeFragment oPrevious2 = oComp2.getStatements().get(o.getFragment2().getIndex()-1);
			boolean thisEqualPrevious = thisPrevious1.getString().equals(thisPrevious2.getString());
			if(thisEqualPrevious) {
				if(!isFieldAssignment(thisPrevious1) && !isFieldAssignment(thisPrevious2)) {
					this.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(thisPrevious1, thisPrevious2)) {
				this.identicalPreviousStatement = true;
			}
			boolean oEqualPrevious = oPrevious1.getString().equals(oPrevious2.getString());
			if(oEqualPrevious) {
				if(!isFieldAssignment(oPrevious1) && !isFieldAssignment(oPrevious2)) {
					o.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(oPrevious1, oPrevious2)) {
				o.identicalPreviousStatement = true;
			}
		}
		else if(lastStatement1 && !lastStatement2 && this != o && !this.containsRefactoringOfType(RefactoringType.EXTRACT_VARIABLE) &&
				this.getFragment2().getIndex() > 0 && this.getFragment2().getIndex() < thisComp2.getStatements().size()-1 &&
				o.getFragment2().getIndex() > 0 && o.getFragment2().getIndex() < oComp2.getStatements().size()-1) {
			AbstractCodeFragment thisPrevious1 = thisComp1.getStatements().get(this.getFragment1().getIndex()-1);
			AbstractCodeFragment thisPrevious2 = thisComp2.getStatements().get(this.getFragment2().getIndex()-1);
			
			AbstractCodeFragment oPrevious1 = oComp1.getStatements().get(o.getFragment1().getIndex()-1);
			AbstractCodeFragment oPrevious2 = oComp2.getStatements().get(o.getFragment2().getIndex()-1);
			boolean thisEqualPrevious = thisPrevious1.getString().equals(thisPrevious2.getString());
			if(thisEqualPrevious) {
				if(!isFieldAssignment(thisPrevious1) && !isFieldAssignment(thisPrevious2)) {
					this.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(thisPrevious1, thisPrevious2)) {
				this.identicalPreviousStatement = true;
			}
			boolean oEqualPrevious = oPrevious1.getString().equals(oPrevious2.getString());
			if(oEqualPrevious) {
				if(!isFieldAssignment(oPrevious1) && !isFieldAssignment(oPrevious2)) {
					o.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(oPrevious1, oPrevious2)) {
				o.identicalPreviousStatement = true;
			}
		}
	}

	private static boolean isFieldAssignment(AbstractCodeFragment fragment) {
		String statement = fragment.getString();
		if(statement.contains(JAVA.ASSIGNMENT)) {
			List<LeafExpression> variables = fragment.getVariables();
			if(variables.size() > 0) {
				String variable = variables.get(0).getString();
				if(statement.startsWith(variable + JAVA.ASSIGNMENT) && variable.startsWith(JAVA.THIS_DOT)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean sameCall(AbstractCodeFragment previous1, AbstractCodeFragment previous2) {
		AbstractCall call1 = previous1.invocationCoveringEntireFragment();
		AbstractCall call2 = previous2.invocationCoveringEntireFragment();
		if(call1 != null && call2 != null && call1.getCoverage().equals(call2.getCoverage())) {
			boolean equalVariableDeclarations = previous1.getVariableDeclarations().size() > 0 && previous1.getVariableDeclarations().toString().equals(previous2.getVariableDeclarations().toString());
			if(equalVariableDeclarations && call1.identicalName(call2) && call1.identicalExpression(call2) && call1.arguments().size() == call2.arguments().size()) {
				return true;
			}
		}
		return false;
	}

	private double stringLiteralRatio() {
		int length1 = getFragment1().getString().length();
		int stringLiteralLength1 = 0;
		for(LeafExpression s1 : getFragment1().getStringLiterals()) {
			stringLiteralLength1 += s1.getString().length();
		}
		double ratio1 = (double)stringLiteralLength1/(double)length1;
		
		int length2 = getFragment2().getString().length();
		int stringLiteralLength2 = 0;
		for(LeafExpression s2 : getFragment2().getStringLiterals()) {
			stringLiteralLength2 += s2.getString().length();
		}
		double ratio2 = (double)stringLiteralLength2/(double)length2;
		return (ratio1 + ratio2)/2.0;
	}

	public double editDistance() {
		double distance1;
		if(this.getFragment1().getString().equals(this.getFragment2().getString())) {
			distance1 = 0;
		}
		else {
			String s1 = removeGenericTypeAfterDot(this.getFragment1().getString().toLowerCase());
			String s2 = removeGenericTypeAfterDot(this.getFragment2().getString().toLowerCase());
			int distance = StringDistance.editDistance(s1, s2);
			distance1 = (double)distance/(double)Math.max(s1.length(), s2.length());
		}
		return distance1;
	}

	private boolean referencesMapping(LeafMapping o) {
		if(getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
				getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
				o.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
				o.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
				this.getFragment1().equals(o.getFragment1()) &&
				o.getFragment2().getLocationInfo().getEndOffset() < this.getFragment2().getLocationInfo().getStartOffset()) {
			List<VariableDeclaration> variableDeclarations2 = o.getFragment2().getVariableDeclarations();
			List<AbstractCall> creations2 = this.getFragment2().getCreations();
			for(VariableDeclaration declaration2 : variableDeclarations2) {
				for(AbstractCall creation : creations2) {
					if(((ObjectCreation)creation).getAnonymousClassDeclaration() != null) {
						return false;
					}
					List<String> arguments = creation.arguments();
					if(arguments.size() == 1 && arguments.contains(declaration2.getVariableName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static String removeGenericTypeAfterDot(String s) {
		if(s.contains(".<")) {
			int indexOfGenericTypeStart = s.indexOf(".<");
			int indexOfGenericTypeEnd = s.indexOf(">", indexOfGenericTypeStart);
			if(indexOfGenericTypeStart < indexOfGenericTypeEnd) {
				s = s.substring(0, indexOfGenericTypeStart) + "." + s.substring(indexOfGenericTypeEnd + 1, s.length());
			}
		}
		return s;
	}

	private boolean identicalDepthIndexAndParentType() {
		if(getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT) &&
				getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.VARIABLE_DECLARATION_STATEMENT)) {
			return false;
		}
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent1 != null && parent2 != null) {
			if(parent1.getLocationInfo().getCodeElementType().equals(parent2.getLocationInfo().getCodeElementType()) &&
					!parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				if(parent1.getString().equals(parent2.getString()) && !parent1.getString().equals(JAVA.TRY)) {
					return true;
				}
				return getFragment1().getDepth() == getFragment2().getDepth() && getFragment1().getIndex() == getFragment2().getIndex();
			}
		}
		return false;
	}

	private int parentIndexDiff() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		return Math.abs(parent1.getIndex() - parent2.getIndex());
	}

	private int commonConditionalsInParent() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent1 != null && parent2 != null && parent1.getExpressions().size() == 1 && parent2.getExpressions().size() == 1) {
			AbstractExpression expr1 = parent1.getExpressions().get(0);
			AbstractExpression expr2 = parent2.getExpressions().get(0);
			String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(expr1.getString());
			List<String> subConditionsAsList1 = new ArrayList<String>();
			for(String s : subConditions1) {
				subConditionsAsList1.add(s.trim());
			}
			String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(expr2.getString());
			List<String> subConditionsAsList2 = new ArrayList<String>();
			for(String s : subConditions2) {
				subConditionsAsList2.add(s.trim());
			}
			Set<String> intersection = subConditionIntersection(subConditionsAsList1, subConditionsAsList2);
			int increment = 0;
			for(String s1 : subConditions1) {
				if(!intersection.contains(s1) && s1.contains(".")) {
					String suffix1 = s1.substring(s1.indexOf("."));
					for(String s2 : subConditions2) {
						if(s2.contains(".")) {
							String suffix2 = s2.substring(s2.indexOf("."));
							if(suffix1.equals(suffix2)) {
								increment++;
								break;
							}
						}
					}
				}
			}
			return intersection.size() + increment;
		}
		return 0;
	}

	private boolean sameVariableDeclarationTypeInParent() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent1 != null && parent2 != null) {
			List<VariableDeclaration> declarations1 = parent1.getVariableDeclarations();
			List<VariableDeclaration> declarations2 = parent2.getVariableDeclarations();
			if(declarations1.size() == declarations2.size()) {
				for(int i=0; i< declarations1.size(); i++) {
					VariableDeclaration declaration1 = declarations1.get(i);
					VariableDeclaration declaration2 = declarations2.get(i);
					if(!declaration1.equalType(declaration2)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private boolean directParentsReferenceSameVariables() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent1 == null && parent2 == null) {
			//method signature is the parent
			return false;
		}
		else if(parent1 == null && parent2 != null) {
			return false;
		}
		else if(parent1 != null && parent2 == null) {
			return false;
		}
		List<LeafExpression> variables1 = parent1.getVariables();
		List<LeafExpression> variables2 = parent2.getVariables();
		Set<String> lowerCaseVariables1 = new LinkedHashSet<>();
		for(LeafExpression variable1 : variables1) {
			if(Character.isLowerCase(variable1.getString().charAt(0))) {
				lowerCaseVariables1.add(variable1.getString());
			}
		}
		Set<String> lowerCaseVariables2 = new LinkedHashSet<>();
		for(LeafExpression variable2 : variables2) {
			if(Character.isLowerCase(variable2.getString().charAt(0))) {
				lowerCaseVariables2.add(variable2.getString());
			}
		}
		if(lowerCaseVariables1.size() > 0 && lowerCaseVariables1.equals(lowerCaseVariables2)) {
			return true;
		}
		return false;
	}

	private Set<String> parentVariableTokenIntersection() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent1 == null && parent2 == null) {
			//method signature is the parent
			return Collections.emptySet();
		}
		else if(parent1 == null && parent2 != null) {
			return Collections.emptySet();
		}
		else if(parent1 != null && parent2 == null) {
			return Collections.emptySet();
		}
		List<LeafExpression> variables1 = parent1.getVariables();
		List<LeafExpression> variables2 = parent2.getVariables();
		if(variables1.size() == 1 && variables2.size() == 1) {
			Set<String> tokens1 = new LinkedHashSet<>();
			for(LeafExpression variable : variables1) {
				String[] array = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variable.getString());
				for(String s : array) {
					tokens1.add(s.toLowerCase());
				}
			}
			Set<String> tokens2 = new LinkedHashSet<>();
			for(LeafExpression variable : variables2) {
				String[] array = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variable.getString());
				for(String s : array) {
					tokens2.add(s.toLowerCase());
				}
			}
			tokens1.retainAll(tokens2);
			return tokens1;
		}
		return Collections.emptySet();
	}

	public double levelParentEditDistanceSum() {
		return levelParentEditDistance().stream().collect(Collectors.summingDouble(Double::doubleValue));
	}

	private List<Double> levelParentEditDistance() {
		if(this.levelParentEditDistance != null) {
			return this.levelParentEditDistance;
		}
		List<Double> levelParentEditDistance = new ArrayList<>();
		CompositeStatementObject firstLevelParent1 = getFragment1().getParent();
		while(firstLevelParent1 != null && firstLevelParent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			firstLevelParent1 = firstLevelParent1.getParent();
		}
		CompositeStatementObject firstLevelParent2 = getFragment2().getParent();
		while(firstLevelParent2 != null && firstLevelParent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			firstLevelParent2 = firstLevelParent2.getParent();
		}
		double firstLevel = parentEditDistance(firstLevelParent1, firstLevelParent2);
		levelParentEditDistance.add(firstLevel);
		CompositeStatementObject currentLevel1 = firstLevelParent1;
		CompositeStatementObject currentLevel2 = firstLevelParent2;
		while(currentLevel1 != null && currentLevel2 != null) {
			CompositeStatementObject secondLevelParent1 = currentLevel1.getParent();
			while(secondLevelParent1 != null && secondLevelParent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				secondLevelParent1 = secondLevelParent1.getParent();
			}
			CompositeStatementObject secondLevelParent2 = currentLevel2.getParent();
			while(secondLevelParent2 != null && secondLevelParent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				secondLevelParent2 = secondLevelParent2.getParent();
			}
			double secondLevel = parentEditDistance(secondLevelParent1, secondLevelParent2);
			levelParentEditDistance.add(secondLevel);
			currentLevel1 = secondLevelParent1;
			currentLevel2 = secondLevelParent2;
		}
		this.levelParentEditDistance = levelParentEditDistance;
		return levelParentEditDistance;
	}

	private double parentEditDistance(CompositeStatementObject parent1, CompositeStatementObject parent2) {
		if(parent1 == null && parent2 == null) {
			//method signature is the parent
			return 0;
		}
		else if(parent1 == null && parent2 != null) {
			String s2 = parent2.getString();
			int distance = StringDistance.editDistance(JAVA.OPEN_BLOCK, s2);
			double normalized = (double)distance/(double)Math.max(1, s2.length());
			return normalized;
		}
		else if(parent1 != null && parent2 == null) {
			String s1 = parent1.getString();
			int distance = StringDistance.editDistance(s1, JAVA.OPEN_BLOCK);
			double normalized = (double)distance/(double)Math.max(s1.length(), 1);
			return normalized;
		}
		String s1 = parent1.getString();
		String s2 = parent2.getString();
		if(!s1.equals(parent1.getArgumentizedString()) && s2.equals(parent1.getArgumentizedString())) {
			return 0;
		}
		if(!s2.equals(parent2.getArgumentizedString()) && s1.equals(parent2.getArgumentizedString())) {
			return 0;
		}
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	private boolean identicalCompositeChildrenStructure() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		CompositeStatementObject parent2 = getFragment2().getParent();
		if(parent1 != null && parent2 != null) {
			List<AbstractStatement> statements1 = parent1.getStatements();
			List<AbstractStatement> statements2 = parent2.getStatements();
			List<CompositeStatementObject> composites1 = new ArrayList<>();
			for(AbstractStatement statement1 : statements1) {
				if(statement1 instanceof CompositeStatementObject) {
					composites1.add((CompositeStatementObject)statement1);
				}
			}
			List<CompositeStatementObject> composites2 = new ArrayList<>();
			for(AbstractStatement statement2 : statements2) {
				if(statement2 instanceof CompositeStatementObject) {
					composites2.add((CompositeStatementObject)statement2);
				}
			}
			if(composites1.size() == composites2.size() && composites1.size() == 1) {
				CompositeStatementObject comp1 = composites1.get(0);
				CompositeStatementObject comp2 = composites2.get(0);
				List<CompositeStatementObject> innerNodes1 = comp1.getInnerNodes();
				List<CompositeStatementObject> innerNodes2 = comp2.getInnerNodes();
				int count = 0;
				if(innerNodes1.size() == innerNodes2.size()) {
					for(int i=0; i<innerNodes1.size(); i++) {
						if(innerNodes1.get(i).getString().equals(innerNodes2.get(i).getString())) {
							count++;
						}
						else {
							break;
						}
					}
				}
				if(count == innerNodes1.size() && count > 0) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean identicalCommentsInParent() {
		if(getFragment1().getParent() != null && getFragment2().getParent() != null) {
			List<String> commentsWithinStatement1 = extractCommentsWithinStatement(getFragment1().getParent(), getOperation1());
			List<String> commentsWithinStatement2 = extractCommentsWithinStatement(getFragment2().getParent(), getOperation2());
			return commentsWithinStatement1.size() > 0 && commentsWithinStatement1.equals(commentsWithinStatement2);
		}
		return false;
	}

	public Set<String> callChainIntersection() {
		AbstractCall invocation1 = this.getFragment1().invocationCoveringEntireFragment();
		AbstractCall invocation2 = this.getFragment2().invocationCoveringEntireFragment();
		return invocation1.callChainIntersection(invocation2);
	}
}
