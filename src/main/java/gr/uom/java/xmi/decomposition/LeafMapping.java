package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONDITIONAL_PATTERN_JAVA;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONDITIONAL_PATTERN_PYTHON;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.subConditionIntersection;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.hasElseBranch;
import static gr.uom.java.xmi.decomposition.UMLOperationBodyMapper.extractCommentsWithinStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.InferredType;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.StringDistance;

public class LeafMapping extends AbstractCodeMapping implements Comparable<LeafMapping> {
	private List<Double> levelParentEditDistance;
	private Integer renamedVariablesWithIdenticalInitializers;
	private boolean identicalPreviousStatement;
	private boolean identicalPreviousAndNextStatement;
	private boolean identicalNextStatement;
	private boolean equalNumberOfAssertions;
	private boolean isomorphic;
	private boolean ifParentWithIdenticalElse;
	private boolean ifParentWithIdenticalThen;
	private boolean isKeyword;
	private Map<UMLOperation, Set<AbstractCodeFragment>> extractedStatements;
	private boolean previousCodeExtracted;
	private Set<AbstractCodeMapping> mappings = Collections.emptySet();
	private Map<String, String> renamedParameters = Collections.emptyMap();
	private Boolean mutuallyMostSimilarParents;
	private boolean distinguishingSurroundingStatements;

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
		if(statement1.isKeyword()) {
			isKeyword = true;
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

	public void setIsomorphic(boolean isomorphic) {
		this.isomorphic = isomorphic;
	}

	//the mappings of the mapper, which provide the mapped neighbouring statements
	public void setMappings(Set<AbstractCodeMapping> mappings) {
		this.mappings = mappings;
	}

	//the parameters renamed with very high confidence according to the signatures, i.e., alphabet -> sample_space
	public void setRenamedParameters(Map<String, String> renamedParameters) {
		this.renamedParameters = renamedParameters;
	}

	//the replacements agreeing with a parameter rename, i.e., alphabet = outcomes -> sample_space = outcomes, not pmf = outcomes;
	//a renamed parameter replaced otherwise gives no evidence, i.e., len(alphabet) -> len(self.alphabet)
	private int parameterRenameAgreements() {
		int agreements = 0;
		for(Replacement r : getReplacements()) {
			if(r.getType().equals(ReplacementType.VARIABLE_NAME) && r.getAfter().equals(renamedParameters.get(r.getBefore()))) {
				agreements++;
			}
		}
		return agreements;
	}

	//the surrounding statements distinguish identical statements, i.e., the statements of reordered test cases:
	//first the previous or next statements mapped to each other (or the then branches of else branches), then the blocks that are each other's most similar block, then the identical previous and next statements
	public int compareSurroundingStatements(LeafMapping o) {
		int result = 0;
		boolean mappedNeighbour1 = this.mappedPreviousOrNextStatement() || this.mappedThenBranch();
		boolean mappedNeighbour2 = o.mappedPreviousOrNextStatement() || o.mappedThenBranch();
		int mutuallyMostSimilarParents;
		if(mappedNeighbour1 != mappedNeighbour2) {
			result = mappedNeighbour1 ? -1 : 1;
		}
		else if((mutuallyMostSimilarParents = compareMutuallyMostSimilarParents(o)) != 0) {
			result = mutuallyMostSimilarParents;
		}
		else {
			boolean identicalNeighbours1 = this.identicalNeighbours();
			boolean identicalNeighbours2 = o.identicalNeighbours();
			if(identicalNeighbours1 != identicalNeighbours2) {
				result = identicalNeighbours1 ? -1 : 1;
			}
		}
		//the mapper keeps the mapping selected by the surrounding statements, as with identical previous and next statements
		if(result < 0) {
			this.distinguishingSurroundingStatements = true;
		}
		else if(result > 0) {
			o.distinguishingSurroundingStatements = true;
		}
		return result;
	}

	public boolean hasDistinguishingSurroundingStatements() {
		return distinguishingSurroundingStatements || hasIdenticalPreviousAndNextStatement();
	}

	//the statement before (offset -1) or after (offset 1) the fragment in its block
	private static AbstractCodeFragment neighbour(AbstractCodeFragment fragment, int offset) {
		CompositeStatementObject parent = fragment.getParent();
		if(parent == null) {
			return null;
		}
		List<AbstractStatement> statements = parent.getStatements();
		//the index of the statement in its block, unless the block changed after the statement was added
		int position = fragment.getIndex();
		if(position < 0 || position >= statements.size() || statements.get(position) != fragment) {
			position = statements.indexOf(fragment);
		}
		int index = position + offset;
		return index >= 0 && index < statements.size() ? statements.get(index) : null;
	}

	//the fragments, or the statements enclosing them, if both fragments are the only statements of their blocks,
	//i.e., the if statements of if(response != null) return response; or the catch clauses of a single throw statement
	private AbstractCodeFragment[] surroundedStatements() {
		AbstractCodeFragment statement1 = getFragment1(), statement2 = getFragment2();
		while(onlyStatement(statement1) && onlyStatement(statement2) && statement1.getParent().getParent() != null && statement2.getParent().getParent() != null) {
			statement1 = statement1.getParent();
			statement2 = statement2.getParent();
		}
		return new AbstractCodeFragment[] {statement1, statement2};
	}

	private static boolean onlyStatement(AbstractCodeFragment fragment) {
		return fragment.getParent() != null && neighbour(fragment, -1) == null && neighbour(fragment, 1) == null;
	}

	//the previous and the next statements are looked up in a single pass over the mappings
	private boolean mappedPreviousOrNextStatement() {
		AbstractCodeFragment previous1 = neighbour(getFragment1(), -1), previous2 = neighbour(getFragment2(), -1);
		AbstractCodeFragment next1 = neighbour(getFragment1(), 1), next2 = neighbour(getFragment2(), 1);
		boolean previous = previous1 != null && previous2 != null;
		boolean next = next1 != null && next2 != null;
		if(!previous && !next) {
			return false;
		}
		for(AbstractCodeMapping mapping : mappings) {
			if((previous && mapping.getFragment1() == previous1 && mapping.getFragment2() == previous2) ||
					(next && mapping.getFragment1() == next1 && mapping.getFragment2() == next2)) {
				return true;
			}
		}
		return false;
	}

	//when the mappings share the block of one fragment and their other blocks are in the same container, at most one of them, the one whose other block has more
	//identical statements with the shared block, can have mutually most similar blocks, so the blocks are searched only for this mapping, and not at all when the
	//identical statements are as many
	private int compareMutuallyMostSimilarParents(LeafMapping o) {
		CompositeStatementObject parent1 = getFragment1().getParent(), otherParent1 = o.getFragment1().getParent();
		CompositeStatementObject parent2 = getFragment2().getParent(), otherParent2 = o.getFragment2().getParent();
		if(parent1 == otherParent1 && parent2 == otherParent2) {
			return 0;
		}
		LeafMapping candidate = null;
		if(parent1 != null && parent1 == otherParent1 && parent2 != null && otherParent2 != null && root(parent2) == root(otherParent2)) {
			int common = commonStatements(parent1, parent2), otherCommon = commonStatements(parent1, otherParent2);
			if(common == otherCommon) {
				return 0;
			}
			candidate = common > otherCommon ? this : o;
		}
		else if(parent2 != null && parent2 == otherParent2 && parent1 != null && otherParent1 != null && root(parent1) == root(otherParent1)) {
			int common = commonStatements(parent1, parent2), otherCommon = commonStatements(otherParent1, parent2);
			if(common == otherCommon) {
				return 0;
			}
			candidate = common > otherCommon ? this : o;
		}
		if(candidate != null) {
			return candidate.mutuallyMostSimilarParents() ? (candidate == this ? -1 : 1) : 0;
		}
		boolean mutuallyMostSimilarParents1 = this.mutuallyMostSimilarParents();
		boolean mutuallyMostSimilarParents2 = o.mutuallyMostSimilarParents();
		if(mutuallyMostSimilarParents1 != mutuallyMostSimilarParents2) {
			return mutuallyMostSimilarParents1 ? -1 : 1;
		}
		return 0;
	}

	//the blocks of the fragments are each other's most similar block in the other container, i.e., the moved block of a reordered test case
	private boolean mutuallyMostSimilarParents() {
		if(mutuallyMostSimilarParents == null) {
			CompositeStatementObject parent1 = getFragment1().getParent();
			CompositeStatementObject parent2 = getFragment2().getParent();
			mutuallyMostSimilarParents = parent1 != null && parent2 != null &&
					mostSimilarBlock(parent1, root(parent2)) == parent2 && mostSimilarBlock(parent2, root(parent1)) == parent1;
		}
		return mutuallyMostSimilarParents;
	}

	private static CompositeStatementObject root(CompositeStatementObject composite) {
		while(composite.getParent() != null) {
			composite = composite.getParent();
		}
		return composite;
	}

	//the block under the root with the most identical statements to the block, or null if there is no such unique block
	private static CompositeStatementObject mostSimilarBlock(CompositeStatementObject block, CompositeStatementObject root) {
		CompositeStatementObject mostSimilar = null;
		int maxCommonStatements = 0;
		boolean unique = false;
		for(CompositeStatementObject candidate : root.getInnerNodes()) {
			//a block with fewer statements than the most identical statements so far cannot reach them
			if(candidate.getStatements().size() < maxCommonStatements) {
				continue;
			}
			int common = commonStatements(block, candidate);
			if(common > maxCommonStatements) {
				mostSimilar = candidate;
				maxCommonStatements = common;
				unique = true;
			}
			else if(common == maxCommonStatements && common > 0) {
				unique = false;
			}
		}
		return unique ? mostSimilar : null;
	}

	//the number of identical statements in the blocks, counting each statement at most once
	private static int commonStatements(CompositeStatementObject block1, CompositeStatementObject block2) {
		List<String> statements2 = new ArrayList<>();
		for(AbstractStatement statement2 : block2.getStatements()) {
			statements2.add(statement2.getString());
		}
		int common = 0;
		for(AbstractStatement statement1 : block1.getStatements()) {
			if(statements2.remove(statement1.getString())) {
				common++;
			}
		}
		return common;
	}

	//identical previous and next statements, where the start or end of both blocks counts as identical,
	//of the enclosing statements for fragments without neighbours, unlike their mapped neighbours, which might be mapped wrongly
	private boolean identicalNeighbours() {
		AbstractCodeFragment[] statements = surroundedStatements();
		AbstractCodeFragment previous1 = neighbour(statements[0], -1), previous2 = neighbour(statements[1], -1);
		AbstractCodeFragment next1 = neighbour(statements[0], 1), next2 = neighbour(statements[1], 1);
		if((previous1 == null && next1 == null) || (previous2 == null && next2 == null)) {
			return false;
		}
		return identicalStatements(previous1, previous2) && identicalStatements(next1, next2);
	}

	private static boolean identicalStatements(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2) {
		if(fragment1 == null || fragment2 == null) {
			return fragment1 == null && fragment2 == null;
		}
		return fragment1.getString().equals(fragment2.getString()) && identicalCaughtExceptionTypes(fragment1, fragment2);
	}

	//the string of a catch clause does not contain the type of its exception, i.e., catch(e) for catch(NoClassDefFoundError e) and catch(ClassFormatError e)
	private static boolean identicalCaughtExceptionTypes(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2) {
		if(!fragment1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) ||
				!fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			return true;
		}
		List<String> types1 = fragment1.getVariableDeclarations().stream().map(d -> String.valueOf(d.getType())).collect(Collectors.toList());
		List<String> types2 = fragment2.getVariableDeclarations().stream().map(d -> String.valueOf(d.getType())).collect(Collectors.toList());
		return types1.equals(types2);
	}

	public void setExtractedStatements(Map<UMLOperation, Set<AbstractCodeFragment>> extractedStatements) {
		this.extractedStatements = extractedStatements;
	}

	private int lambdaExactMatches() {
		int exactMatches = 0;
		for(UMLOperationBodyMapper lambdaMapper : getLambdaMappers()) {
			exactMatches += lambdaMapper.exactMatches();
		}
		return exactMatches;
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
			int parameterRenameAgreements1 = this.parameterRenameAgreements();
			int parameterRenameAgreements2 = o.parameterRenameAgreements();
			if(parameterRenameAgreements1 != parameterRenameAgreements2) {
				return -Integer.compare(parameterRenameAgreements1, parameterRenameAgreements2);
			}
			//the variable renames backed by declarations with identical initializers distinguish the mappings of statements in loops with identical bodies,
			//i.e., s.remove() -> openClientSocket.remove(), when Iterator<Socket> s=openClientSockets.iterator() -> val openClientSocket = openClientSockets.iterator()
			int renamedVariablesWithIdenticalInitializers1 = this.renamedVariablesWithIdenticalInitializers();
			int renamedVariablesWithIdenticalInitializers2 = o.renamedVariablesWithIdenticalInitializers();
			if(renamedVariablesWithIdenticalInitializers1 >= 0 && renamedVariablesWithIdenticalInitializers2 >= 0 &&
					renamedVariablesWithIdenticalInitializers1 != renamedVariablesWithIdenticalInitializers2) {
				return -Integer.compare(renamedVariablesWithIdenticalInitializers1, renamedVariablesWithIdenticalInitializers2);
			}
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
				else if(!this.identicalPreviousAndNextStatement && o.identicalPreviousAndNextStatement && !this.isSwappedVariableDeclaration() && !o.isSwappedVariableDeclaration()) {
					return 1;
				}
				if(this.identicalPreviousStatement && !o.identicalPreviousStatement) {
					return -1;
				}
				else if(!this.identicalPreviousStatement && o.identicalPreviousStatement) {
					return 1;
				}
				if(this.identicalNextStatement && !o.identicalNextStatement) {
					return -1;
				}
				else if(!this.identicalNextStatement && o.identicalNextStatement) {
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
				if(this.differOnlyInVal() && !o.differOnlyInVal()) {
					return -1;
				}
				else if(!this.differOnlyInVal() && o.differOnlyInVal()) {
					return 1;
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
						if(type.equals(ReplacementType.STRING_LITERAL) && o.identicalStringLiterals()) {
							identicalNodesCount++;
						}
						else if(type.equals(ReplacementType.NUMBER_LITERAL) && o.identicalNumberLiterals()) {
							identicalNodesCount++;
						}
					}
					for(ReplacementType type : otherReplacementTypesWithoutCommon) {
						if(type.equals(ReplacementType.STRING_LITERAL) && this.identicalStringLiterals()) {
							identicalNodesCount++;
						}
						else if(type.equals(ReplacementType.NUMBER_LITERAL) && this.identicalNumberLiterals()) {
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
					for(ReplacementType type : intersection) {
						boolean identicalStringLiterals1 = false;
						for(Replacement r : this.getReplacements()) {
							if(r.getType().equals(type) && r.getBefore().contains("\"") && r.getAfter().contains("\"")) {
								identicalStringLiterals1 = this.identicalStringLiterals();
								break;
							}
						}
						boolean identicalStringLiterals2 = false;
						for(Replacement r : o.getReplacements()) {
							if(r.getType().equals(type) && r.getBefore().contains("\"") && r.getAfter().contains("\"")) {
								identicalStringLiterals2 = o.identicalStringLiterals();
								break;
							}
						}
						if(identicalStringLiterals1 != identicalStringLiterals2) {
							if(identicalStringLiterals1)
								return -1;
							else if(identicalStringLiterals2)
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
				//the bodies of the lambdas distinguish statements that differ in their lambdas, i.e., logger.debug(() -> {...}) with different messages
				if(this.getLambdaMappers().size() > 0 && o.getLambdaMappers().size() > 0) {
					int lambdaExactMatches1 = this.lambdaExactMatches();
					int lambdaExactMatches2 = o.lambdaExactMatches();
					if(lambdaExactMatches1 != lambdaExactMatches2) {
						return -Integer.compare(lambdaExactMatches1, lambdaExactMatches2);
					}
				}
				if(this.getMatchingArgumentsWithOperationInvocation() != o.getMatchingArgumentsWithOperationInvocation()) {
					return -Integer.valueOf(this.getMatchingArgumentsWithOperationInvocation()).compareTo(Integer.valueOf(o.getMatchingArgumentsWithOperationInvocation()));
				}
				int typeIntersection1 = 0;
				int identicalTypeIntersection1 = 0;
				for(String type1 : this.getFragment1().getTypes()) {
					for(String type2 : this.getFragment2().getTypes()) {
						if(type1.equals(type2) || type1.endsWith(type2) || type2.endsWith(type1) || type2.startsWith(type1) || type2.contains(type1)) {
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
						if(type1.equals(type2) || type1.endsWith(type2) || type2.endsWith(type1) || type2.startsWith(type1) || type2.contains(type1)) {
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
				if(this.getFragment1().getVariableDeclarations().size() == this.getFragment2().getVariableDeclarations().size() && typeIntersection1 > 0 && typeIntersection2 == 0 && o.getFragment1().getTypes().size() > 0 && o.getFragment2().getTypes().size() > 0 && identicalTypeIntersection1 >= 0 && identicalTypeIntersection2 == 0) {
					return -1;
				}
				if(o.getFragment1().getVariableDeclarations().size() == o.getFragment2().getVariableDeclarations().size() && typeIntersection2 > 0 && typeIntersection1 == 0 && this.getFragment1().getTypes().size() > 0 && this.getFragment2().getTypes().size() > 0 && identicalTypeIntersection1 == 0 && identicalTypeIntersection2 >= 0) {
					return 1;
				}
				boolean nestedUnderCatchBlockOfSameType1 = this.nestedUnderCatchBlockOfSameExceptionType();
				boolean nestedUnderCatchBlockOfSameType2 = o.nestedUnderCatchBlockOfSameExceptionType();
				if(nestedUnderCatchBlockOfSameType1 && !nestedUnderCatchBlockOfSameType2) {
					return -1;
				}
				else if(!nestedUnderCatchBlockOfSameType1 && nestedUnderCatchBlockOfSameType2) {
					return 1;
				}
				boolean streamAPIMigration1 = this.streamAPIMigration();
				boolean streamAPIMigration2 = o.streamAPIMigration();
				if(streamAPIMigration1 && !streamAPIMigration2) {
					return -1;
				}
				else if(streamAPIMigration2 && !streamAPIMigration1) {
					return 1;
				}
				if(this.getFragment1().getVariableDeclarations().size() > 0 && this.getFragment2().getVariableDeclarations().size() > 0 &&
						o.getFragment1().getVariableDeclarations().size() > 0 && o.getFragment2().getVariableDeclarations().size() > 0 &&
						Math.abs(distance1 - distance2) < 0.02) {
					int depthDiff1 = Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth());
					int depthDiff2 = Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth());
					if(depthDiff1 != depthDiff2) {
						return Integer.valueOf(depthDiff1).compareTo(Integer.valueOf(depthDiff2));
					}
					else {
						int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
						int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
						if(indexDiff1 != indexDiff2 && Math.abs(indexDiff1 - indexDiff2) > 1) {
							return Integer.valueOf(indexDiff1).compareTo(Integer.valueOf(indexDiff2));
						}
					}
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
				//the statements of reordered when entries follow their entries
				boolean identicalWhenEntry1 = this.identicalWhenEntry();
				boolean identicalWhenEntry2 = o.identicalWhenEntry();
				if(identicalWhenEntry1 != identicalWhenEntry2) {
					return identicalWhenEntry1 ? -1 : 1;
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
				boolean allZerosExceptLast1 = headZeros1 == levelParentEditDistance1.size() - 1 && levelParentEditDistance1.get(levelParentEditDistance1.size() - 1) > 0;
				boolean allZerosExceptLast2 = headZeros2 == levelParentEditDistance2.size() - 1 && levelParentEditDistance2.get(levelParentEditDistance2.size() - 1) > 0;
				boolean identicalCompositeChildren1 = this.identicalCompositeChildrenStructure();
				boolean identicalCompositeChildren2 = o.identicalCompositeChildrenStructure();
				boolean zeroDistanceWithMoreThanTwoParents1 = nLevelParentEditDistance1 == 0 && levelParentEditDistance1.size() > 2;
				boolean zeroDistanceWithMoreThanTwoParents2 = nLevelParentEditDistance2 == 0 && levelParentEditDistance2.size() > 2;
				if(identicalCompositeChildren1 && !identicalCompositeChildren2 && !zeroDistanceWithMoreThanTwoParents2) {
					boolean skip = false;
					if(nLevelParentEditDistance2 == 0 && this.isKeyword)
						skip = true;
					if(!skip)
						return -1;
				}
				else if(!identicalCompositeChildren1 && identicalCompositeChildren2 && !zeroDistanceWithMoreThanTwoParents1) {
					boolean skip = false;
					if(nLevelParentEditDistance1 == 0 && o.isKeyword)
						skip = true;
					if(!skip)
						return 1;
				}
				if(identicalCompositeChildren1 && identicalCompositeChildren2 && levelParentEditDistance1.size() > 2 && levelParentEditDistance2.size() > 2
						&& nLevelParentEditDistance1 > 0 && nLevelParentEditDistance2 > 0) {
					if(allZerosExceptLast1 && !allZerosExceptLast2) {
						return -1;
					}
					else if(!allZerosExceptLast1 && allZerosExceptLast2) {
						return 1;
					}
				}
				if(levelParentEditDistance1.size() == levelParentEditDistance2.size()) {
					if(nLevelParentEditDistance1 == 0 && nLevelParentEditDistance2 > 0) { 
						return -1; 
					} 
					else if(nLevelParentEditDistance2 == 0 && nLevelParentEditDistance1 > 0) { 
						return 1; 
					}
					double commonCommentRatio1 = this.commonCommentRatioInVariableScope();
					double commonCommentRatio2 = o.commonCommentRatioInVariableScope();
					if(headZeros1 > headZeros2) {
						if(commonCommentRatio2 > commonCommentRatio1) {
							return 1;
						}
						return -1;
					}
					else if(headZeros2 > headZeros1) {
						if(commonCommentRatio1 > commonCommentRatio2) {
							return -1;
						}
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
					else if(levelParentEditDistance1.size() == 2 && levelParentEditDistance1.get(1).equals(1.0) &&
							levelParentEditDistance2.size() == 2 && levelParentEditDistance2.get(1).equals(1.0) &&
							!levelParentEditDistance1.get(0).equals(levelParentEditDistance2.get(0))) {
						if(levelParentEditDistance1.get(0) < levelParentEditDistance2.get(0) && this.directParentHaveExpressionOverlap()) {
							return Double.compare(levelParentEditDistance1.get(0), levelParentEditDistance2.get(0));
						}
						else if(levelParentEditDistance1.get(0) > levelParentEditDistance2.get(0) && o.directParentHaveExpressionOverlap()) {
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
					//identical statements cannot be distinguished by their replacements, but by their surrounding statements
					if(this.getReplacements().isEmpty() && o.getReplacements().isEmpty()) {
						int surroundingStatements = this.compareSurroundingStatements(o);
						if(surroundingStatements != 0) {
							return surroundingStatements;
						}
					}
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
						boolean sameEnhancedForCollectionInParent1 = this.sameEnhancedForCollectionInParent();
						boolean sameEnhancedForCollectionInParent2 = o.sameEnhancedForCollectionInParent();
						if(sameEnhancedForCollectionInParent1 && !sameEnhancedForCollectionInParent2) {
							return -1;
						}
						else if(!sameEnhancedForCollectionInParent1 && sameEnhancedForCollectionInParent2) {
							return 1;
						}
						if(this.previousCodeExtracted && !o.previousCodeExtracted) {
							return -1;
						}
						else if(!this.previousCodeExtracted && o.previousCodeExtracted) {
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
						boolean streamAPIMigration1 = this.streamAPIMigration();
						boolean streamAPIMigration2 = o.streamAPIMigration();
						if(streamAPIMigration1 && !streamAPIMigration2) {
							return -1;
						}
						else if(streamAPIMigration2 && !streamAPIMigration1) {
							return 1;
						}
						boolean thisIdenticalCommentsInParent = this.identicalCommentsInParent();
						boolean otherIdenticalCommentsInParent = o.identicalCommentsInParent();
						if(thisIdenticalCommentsInParent && !otherIdenticalCommentsInParent) {
							return -1;
						}
						else if(!thisIdenticalCommentsInParent && otherIdenticalCommentsInParent) {
							return 1;
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

	private boolean identicalStringLiterals() {
		List<LeafExpression> stringLiterals1 = this.getFragment1().getStringLiterals();
		List<LeafExpression> stringLiterals2 = this.getFragment2().getStringLiterals();
		if(stringLiterals1.size() > 0 && stringLiterals1.size() == stringLiterals2.size()) {
			int matches = 0;
			for(int i=0; i<stringLiterals1.size(); i++) {
				LeafExpression expr1 = stringLiterals1.get(i);
				LeafExpression expr2 = stringLiterals2.get(i);
				if(expr1.getString().equals(expr2.getString())) {
					matches++;
				}
			}
			return matches == stringLiterals1.size();
		}
		return false;
	}

	private boolean identicalNumberLiterals() {
		List<LeafExpression> numberLiterals1 = this.getFragment1().getNumberLiterals();
		List<LeafExpression> numberLiterals2 = this.getFragment2().getNumberLiterals();
		if(numberLiterals1.size() > 0 && numberLiterals1.size() == numberLiterals2.size()) {
			int matches = 0;
			for(int i=0; i<numberLiterals1.size(); i++) {
				LeafExpression expr1 = numberLiterals1.get(i);
				LeafExpression expr2 = numberLiterals2.get(i);
				if(expr1.getString().equals(expr2.getString())) {
					matches++;
				}
			}
			return matches == numberLiterals1.size();
		}
		return false;
	}

	public List<UMLOperationBodyMapper> nestedLambdaMappers() {
		List<UMLOperationBodyMapper> nestedLambdaMappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper lambdaMapper : this.getLambdaMappers()) {
			for(AbstractCodeMapping mapping : lambdaMapper.getMappings()) {
				if(mapping.getLambdaMappers().size() > 0) {
					nestedLambdaMappers.addAll(mapping.getLambdaMappers());
				}
			}
		}
		return nestedLambdaMappers;
	}

	private boolean streamAPIMigration() {
		if(getFragment1() instanceof StatementObject && getFragment2() instanceof AbstractExpression) {
			AbstractExpression expr2 = (AbstractExpression)getFragment2();
			VariableDeclarationContainer lambda = expr2.getLambdaOwner();
			if(lambda != null) {
				AbstractCodeFragment lambdaStatement2 = null;
				while(lambda instanceof LambdaExpressionObject &&
						((LambdaExpressionObject)lambda).getOwner() instanceof LambdaExpressionObject) {
					lambda = ((LambdaExpressionObject)lambda).getOwner();
				}
				if(((LambdaExpressionObject)lambda).getOwner() != null && ((LambdaExpressionObject)lambda).getOwner().getBody() != null) {
					for(AbstractCodeFragment fragment2 : ((LambdaExpressionObject)lambda).getOwner().getBody().getCompositeStatement().getLeaves()) {
						if(fragment2.getLambdas().contains(lambda)) {
							lambdaStatement2 = fragment2;
							break;
						}
					}
				}
				CompositeStatementObject parent1 = getFragment1().getParent();
				while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
					parent1 = parent1.getParent();
				}
				if(parent1 != null && lambdaStatement2 != null) {
					for(AbstractExpression expr1 : parent1.getExpressions()) {
						if(lambdaStatement2.findExpression(expr1.getString()).size() > 0) {
							return true;
						}
						if(expr1.getString().endsWith(".get()")) {
							String prefix = expr1.getString().substring(0, expr1.getString().length()-6);
							if(lambdaStatement2.findExpression(prefix).size() > 0) {
								return true;
							}
						}
						if(expr1.getString().endsWith(".isPresent()")) {
							String prefix = expr1.getString().substring(0, expr1.getString().length()-12);
							if(lambdaStatement2.findExpression(prefix).size() > 0) {
								return true;
							}
						}
					}
				}
			}
		}
		else if(getFragment1() instanceof StatementObject && getFragment2() instanceof StatementObject &&
				getFragment1().getLambdas().size() == 0 && getFragment2().getLambdas().size() > 0 &&
				getFragment1().getMethodInvocations().size() > 0 && getFragment2().getMethodInvocations().size() > 0) {
			AbstractCall call1 = getFragment1().getMethodInvocations().get(getFragment1().getMethodInvocations().size()-1);
			AbstractCall call2 = getFragment2().getMethodInvocations().get(getFragment2().getMethodInvocations().size()-1);
			if(call1.actualString().equals(call2.actualString())) {
				return true;
			}
		}
		return false;
	}

	private boolean underElse() {
		return isUnderElseBranch(getFragment1()) && isUnderElseBranch(getFragment2());
	}
	
	//the fragments are nested in when entries with identical conditions, i.e., STRING_IN_UTF8 -> { ... }
	private boolean identicalWhenEntry() {
		CompositeStatementObject whenEntry1 = enclosingWhenEntry(getFragment1());
		CompositeStatementObject whenEntry2 = enclosingWhenEntry(getFragment2());
		return whenEntry1 != null && whenEntry2 != null && whenEntry1.getString().equals(whenEntry2.getString());
	}

	private static CompositeStatementObject enclosingWhenEntry(AbstractCodeFragment fragment) {
		for(CompositeStatementObject parent = fragment.getParent(); parent != null; parent = parent.getParent()) {
			if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.WHEN_ENTRY)) {
				return parent;
			}
		}
		return null;
	}

	private boolean isUnderElseBranch(AbstractCodeFragment fragment) {
		return ifWithElseBranch(fragment) != null;
	}

	//the if statement whose else branch, not an else-if branch, contains the fragment
	private static CompositeStatementObject ifWithElseBranch(AbstractCodeFragment fragment) {
		if(fragment.getParent() != null) {
			CompositeStatementObject child = fragment.getParent();
			if(child.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && child.getParent() != null) {
				CompositeStatementObject parent = child.getParent();
				if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						parent.getStatements().size() == 2 && parent.getStatements().indexOf(child) == 1) {
					return parent;
				}
			}
			else if(child.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				if(child.getStatements().size() == 2 && child.getStatements().indexOf(fragment) == 1) {
					return child;
				}
			}
		}
		return null;
	}

	//the then branches of the if statements, whose else branches contain the fragments, have statements mapped to each other,
	//i.e., the else branches of the if statements of identical cases contain only identical statements
	private boolean mappedThenBranch() {
		CompositeStatementObject if1 = ifWithElseBranch(getFragment1());
		CompositeStatementObject if2 = ifWithElseBranch(getFragment2());
		if(if1 == null || if2 == null) {
			return false;
		}
		LocationInfo then1 = if1.getStatements().get(0).getLocationInfo();
		LocationInfo then2 = if2.getStatements().get(0).getLocationInfo();
		for(AbstractCodeMapping mapping : mappings) {
			if(then1.subsumes(mapping.getFragment1().getLocationInfo()) && then2.subsumes(mapping.getFragment2().getLocationInfo())) {
				return true;
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
		boolean firstStatement1 = thisComp1 != null && this.getFragment1().getIndex() == 0 && oComp1 != null && o.getFragment1().getIndex() == 0;
		boolean firstStatement2 = thisComp2 != null && this.getFragment2().getIndex() == 0 && oComp2 != null && o.getFragment2().getIndex() == 0;
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
			if(this.extractedStatements != null) {
				for(UMLOperation key : this.extractedStatements.keySet()) {
					Set<AbstractCodeFragment> set = this.extractedStatements.get(key);
					if(set.contains(thisPrevious1) && thisPrevious2.getString().contains(key.getName())) {
						this.previousCodeExtracted = true;
					}
				}
			}
			if(o.extractedStatements != null) {
				for(UMLOperation key : o.extractedStatements.keySet()) {
					Set<AbstractCodeFragment> set = o.extractedStatements.get(key);
					if(set.contains(oPrevious1) && oPrevious2.getString().contains(key.getName())) {
						o.previousCodeExtracted = true;
					}
				}
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
				if(!isFieldAssignment(thisPrevious1, LANG1) && !isFieldAssignment(thisPrevious2, LANG2)) {
					this.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(thisPrevious1, thisPrevious2)) {
				this.identicalPreviousStatement = true;
			}
			boolean oEqualPrevious = oPrevious1.getString().equals(oPrevious2.getString());
			if(oEqualPrevious) {
				if(!isFieldAssignment(oPrevious1, LANG1) && !isFieldAssignment(oPrevious2, LANG2)) {
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
				if(!isFieldAssignment(thisPrevious1, LANG1) && !isFieldAssignment(thisPrevious2, LANG2)) {
					this.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(thisPrevious1, thisPrevious2)) {
				this.identicalPreviousStatement = true;
			}
			boolean oEqualPrevious = oPrevious1.getString().equals(oPrevious2.getString());
			if(oEqualPrevious) {
				if(!isFieldAssignment(oPrevious1, LANG1) && !isFieldAssignment(oPrevious2, LANG2)) {
					o.identicalPreviousStatement = true;
				}
			}
			else if(sameCall(oPrevious1, oPrevious2)) {
				o.identicalPreviousStatement = true;
			}
		}
		else if(firstStatement1 && firstStatement2 &&
				this.getFragment1().getIndex() < thisComp1.getStatements().size()-1 &&
				this.getFragment2().getIndex() < thisComp2.getStatements().size()-1 &&
				o.getFragment1().getIndex() < oComp1.getStatements().size()-1 &&
				o.getFragment2().getIndex() < oComp2.getStatements().size()-1) {
			AbstractCodeFragment thisNext1 = thisComp1.getStatements().get(this.getFragment1().getIndex()+1);
			AbstractCodeFragment thisNext2 = thisComp2.getStatements().get(this.getFragment2().getIndex()+1);
			AbstractCodeFragment oNext1 = oComp1.getStatements().get(o.getFragment1().getIndex()+1);
			AbstractCodeFragment oNext2 = oComp2.getStatements().get(o.getFragment2().getIndex()+1);
			
			boolean thisEqualNext = thisNext1.getString().equals(thisNext2.getString());
			boolean oEqualNext = oNext1.getString().equals(oNext2.getString());
			if(thisEqualNext)
				this.identicalNextStatement = true;
			if(oEqualNext)
				o.identicalNextStatement = true;
		}
	}

	private boolean isFieldAssignment(AbstractCodeFragment fragment, Constants LANG) {
		String statement = fragment.getString();
		if(statement.contains(LANG.ASSIGNMENT)) {
			List<LeafExpression> variables = fragment.getVariables();
			if(variables.size() > 0) {
				String variable = variables.get(0).getString();
				if(statement.startsWith(variable + LANG.ASSIGNMENT) && variable.startsWith(LANG.THIS_DOT)) {
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
			//the changed type annotations of the declarations are TYPE replacements, not text differences
			String s1 = removeGenericTypeAfterDot(StringBasedHeuristics.withoutChangedTypeAnnotations(this.getFragment1().getString(), this.getFragment1(), this.getFragment2()).toLowerCase());
			String s2 = removeGenericTypeAfterDot(StringBasedHeuristics.withoutChangedTypeAnnotations(this.getFragment2().getString(), this.getFragment2(), this.getFragment1()).toLowerCase());
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
					if(creation instanceof ObjectCreation && ((ObjectCreation)creation).getAnonymousClassDeclaration() != null) {
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

	private boolean differOnlyInVal() {
		String s1 = this.getFragment1().getString();
		String s2 = this.getFragment2().getString();
		if(!s1.equals(s2)) {
			if(s1.equals("val " + s2))
				return true;
			else if(s1.equals("var " + s2))
				return true;
			if(s2.equals("val " + s1))
				return true;
			else if(s2.equals("var " + s1))
				return true;
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
			List<VariableDeclaration> list1 = getFragment1().getVariableDeclarations();
			List<VariableDeclaration> list2 = getFragment2().getVariableDeclarations();
			boolean equalTypeAndInitializer = false;
			if(list1.size() == list2.size() && list1.size() > 0) {
				VariableDeclaration v1 = list1.get(0);
				VariableDeclaration v2 = list2.get(0);
				//the Kotlin variable without declared type has the type of its initializer, i.e., String protocolString=x; -> val protocolString = x
				boolean inferredType = v1.getType() instanceof InferredType || v2.getType() instanceof InferredType;
				if((v1.equalType(v2) || inferredType) && v1.getInitializer() != null && v2.getInitializer() != null &&
						v1.getInitializer().getString().equals(v2.getInitializer().getString())) {
					equalTypeAndInitializer = true;
				}
			}
			if(!equalTypeAndInitializer) {
				return false;
			}
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
				if(parent1.getString().equals(parent2.getString()) && !parent1.getString().equals(LANG1.TRY)) {
					return true;
				}
				boolean multipleVarDeclarations = false;
				if(getFragment1().getAssignments().size() == 1 && getFragment2().getAssignments().size() == 1 && !isomorphic) {
					String assignment1 = getFragment1().getAssignments().get(0).getString();
					String assignedVar1 = assignment1.contains(LANG1.ASSIGNMENT) ? assignment1.substring(0, assignment1.indexOf(LANG1.ASSIGNMENT)) : null;
					String assignment2 = getFragment2().getAssignments().get(0).getString();
					String assignedVar2 = assignment2.contains(LANG2.ASSIGNMENT) ? assignment2.substring(0, assignment2.indexOf(LANG2.ASSIGNMENT)) : null;
					if(assignedVar1 != null && assignedVar2 != null) {
						Map<String, Set<VariableDeclaration>> map1 = this.getOperation1().variableDeclarationMap();
						Map<String, Set<VariableDeclaration>> map2 = this.getOperation2().variableDeclarationMap();
						if(map1.containsKey(assignedVar1) && map2.containsKey(assignedVar2)) {
							if(map1.get(assignedVar1).size() > 1 && map2.get(assignedVar2).size() > 1) {
								multipleVarDeclarations = true;
							}
						}
					}
				}
				if(!multipleVarDeclarations) {
					return getFragment1().getDepth() == getFragment2().getDepth() && getFragment1().getIndex() == getFragment2().getIndex();
				}
			}
		}
		return false;
	}

	//-1 when there are no variable renames or a renamed variable has no declaration with initializer, 1 when all renamed variables are declared with identical initializers, 0 otherwise
	//the renamed variables declared by the mapped statements themselves are not considered, as their initializers are part of the mapped statements
	private int renamedVariablesWithIdenticalInitializers() {
		if(this.renamedVariablesWithIdenticalInitializers != null) {
			return this.renamedVariablesWithIdenticalInitializers;
		}
		int renamedVariables = 0;
		int identicalInitializers = 0;
		for(Replacement r : getReplacements()) {
			if(r.getType().equals(ReplacementType.VARIABLE_NAME)) {
				if(getFragment1().getVariableDeclaration(r.getBefore()) != null || getFragment2().getVariableDeclaration(r.getAfter()) != null) {
					this.renamedVariablesWithIdenticalInitializers = -1;
					return this.renamedVariablesWithIdenticalInitializers;
				}
				renamedVariables++;
				VariableDeclaration v1 = getFragment1().searchVariableDeclaration(r.getBefore());
				VariableDeclaration v2 = getFragment2().searchVariableDeclaration(r.getAfter());
				if(v1 == null || v2 == null || v1.getInitializer() == null || v2.getInitializer() == null) {
					//a renamed variable without declaration (i.e., a field) or initializer gives no evidence, so the other criteria decide,
					//i.e., mergeThreads.get(i) -> activeMerges.get(threadIdx), when mergeThreads is a field
					this.renamedVariablesWithIdenticalInitializers = -1;
					return this.renamedVariablesWithIdenticalInitializers;
				}
				if(v1.getInitializer().getString().equals(v2.getInitializer().getString())) {
					identicalInitializers++;
				}
			}
		}
		this.renamedVariablesWithIdenticalInitializers = renamedVariables == 0 ? -1 : (identicalInitializers == renamedVariables ? 1 : 0);
		return this.renamedVariablesWithIdenticalInitializers;
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
		Pattern SPLIT_CONDITIONAL_PATTERN_1 = LANG1.equals(Constants.PYTHON) ? SPLIT_CONDITIONAL_PATTERN_PYTHON : SPLIT_CONDITIONAL_PATTERN_JAVA;
		Pattern SPLIT_CONDITIONAL_PATTERN_2 = LANG2.equals(Constants.PYTHON) ? SPLIT_CONDITIONAL_PATTERN_PYTHON : SPLIT_CONDITIONAL_PATTERN_JAVA;
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
			String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN_1.split(expr1.getString());
			List<String> subConditionsAsList1 = new ArrayList<String>();
			for(String s : subConditions1) {
				subConditionsAsList1.add(s.trim());
			}
			String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN_2.split(expr2.getString());
			List<String> subConditionsAsList2 = new ArrayList<String>();
			for(String s : subConditions2) {
				subConditionsAsList2.add(s.trim());
			}
			Set<String> intersection = subConditionIntersection(subConditionsAsList1, subConditionsAsList2, null, LANG1, LANG2);
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

	private boolean sameEnhancedForCollectionInParent() {
		CompositeStatementObject parent1 = getFragment1().getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		CompositeStatementObject parent2 = getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent1 != null && parent2 != null &&
				parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
				parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
				parent1.getExpressions().size() > 0 && parent2.getExpressions().size() > 0) {
			AbstractExpression expr1 = parent1.getExpressions().get(parent1.getExpressions().size()-1);
			AbstractExpression expr2 = parent2.getExpressions().get(parent2.getExpressions().size()-1);
			if(expr1.getString().contains(expr2.getString()) || expr2.getString().contains(expr1.getString())) {
				return true;
			}
		}
		return false;
	}

	private boolean directParentHaveExpressionOverlap() {
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
		List<AbstractExpression> expressions1 = parent1.getExpressions();
		List<AbstractExpression> expressions2 = parent2.getExpressions();
		if(expressions1.size() == expressions2.size()) {
			int matches = 0;
			for(int i=0; i<expressions1.size(); i++) {
				AbstractExpression expr1 = expressions1.get(i);
				AbstractExpression expr2 = expressions2.get(i);
				if(expr1.getString().contains(expr2.getString()) || expr2.getString().contains(expr1.getString())) {
					matches++;
				}
			}
			return matches > 0;
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
			if(variable1.getString().length() > 0 && Character.isLowerCase(variable1.getString().charAt(0))) {
				lowerCaseVariables1.add(variable1.getString());
			}
		}
		Set<String> lowerCaseVariables2 = new LinkedHashSet<>();
		for(LeafExpression variable2 : variables2) {
			if(variable2.getString().length() > 0 && Character.isLowerCase(variable2.getString().charAt(0))) {
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
			int distance = StringDistance.editDistance(LANG2.OPEN_BLOCK, s2);
			double normalized = (double)distance/(double)Math.max(1, s2.length());
			return normalized;
		}
		else if(parent1 != null && parent2 == null) {
			String s1 = parent1.getString();
			int distance = StringDistance.editDistance(s1, LANG1.OPEN_BLOCK);
			double normalized = (double)distance/(double)Math.max(s1.length(), 1);
			return normalized;
		}
		String s1 = parent1.getString();
		String s2 = parent2.getString();
		if(!s1.equals(getArgumentizedString(parent1)) && s2.equals(getArgumentizedString(parent1))) {
			return 0;
		}
		if(!s2.equals(getArgumentizedString(parent2)) && s1.equals(getArgumentizedString(parent2))) {
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
			else if(composites1.size() == composites2.size() && composites1.size() == 0 &&
					parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
					parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent1 = parent1.getParent(); 
				}
				while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent2 = parent2.getParent(); 
				}
				if(parent1 != null && parent2 != null && parent1.getString().equals(parent2.getString())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean identicalCommentsInParent() {
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

	private boolean nestedUnderCatchBlockOfSameExceptionType() {
		CompositeStatementObject comp1 = getFragment1().getParent();
		CompositeStatementObject comp2 = getFragment2().getParent();
		if(comp1 != null && comp2 != null &&
				comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
				comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			List<VariableDeclaration> exceptionDeclarations1 = comp1.getVariableDeclarations();
			List<VariableDeclaration> exceptionDeclarations2 = comp2.getVariableDeclarations();
			if(exceptionDeclarations1.size() > 0 && exceptionDeclarations2.size() > 0) {
				VariableDeclaration v1 = exceptionDeclarations1.get(0);
				VariableDeclaration v2 = exceptionDeclarations2.get(0);
				if(v1.getType().equals(v2.getType())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private double commonCommentRatioInVariableScope() {
		if(getFragment1().getVariableDeclarations().size() > 0 && getFragment2().getVariableDeclarations().size() > 0) {
			VariableDeclaration v1 = getFragment1().getVariableDeclarations().get(0);
			VariableDeclaration v2 = getFragment2().getVariableDeclarations().get(0);
			List<String> commentsInScope1 = new ArrayList<>();
			for(UMLComment comment : getOperation1().getComments()) {
				if(v1.getScope().subsumes(comment.getLocationInfo())) {
					commentsInScope1.add(comment.getText());
				}
			}
			List<String> commentsInScope2 = new ArrayList<>();
			for(UMLComment comment : getOperation2().getComments()) {
				if(v2.getScope().subsumes(comment.getLocationInfo())) {
					commentsInScope2.add(comment.getText());
				}
			}
			Set<String> union = new LinkedHashSet<>(commentsInScope1);
			union.addAll(commentsInScope2);
			Set<String> intersection = new LinkedHashSet<>(commentsInScope1);
			intersection.retainAll(commentsInScope2);
			if(union.size() > 0)
				return (double)intersection.size()/(double)union.size();
		}
		return 0;
	}

	public boolean sameVariableDeclarationName() {
		if(this.getFragment1().getVariableDeclarations().size() > 0 && this.getFragment2().getVariableDeclarations().size() > 0) {
			return this.getFragment1().getVariableDeclarations().get(0).getVariableName().equals(this.getFragment2().getVariableDeclarations().get(0).getVariableName());
		}
		return false;
	}
}
