package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.ListCompositeType;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAnnotation;

import static gr.uom.java.xmi.Constants.JAVA;
import static gr.uom.java.xmi.LocationInfo.CodeElementType.EXPRESSION_STATEMENT;
import static gr.uom.java.xmi.LocationInfo.CodeElementType.METHOD_INVOCATION;
import static gr.uom.java.xmi.UMLModelASTReader.processBlock;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.findReplacementsWithExactMatching;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.isForEach;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.processLambdas;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.streamAPICalls;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.streamAPIName;
import static gr.uom.java.xmi.decomposition.ReplacementUtil.isDefaultValue;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.*;
import static gr.uom.java.xmi.decomposition.Visitor.stringify;
import static gr.uom.java.xmi.diff.UMLClassBaseDiff.matchParamsWithReplacements;
import static gr.uom.java.xmi.diff.UMLClassBaseDiff.sanitizeStringLiteral;

import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.IntersectionReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.ClassInstanceCreationWithMethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.AddParameterRefactoring;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import gr.uom.java.xmi.diff.AssertTimeoutRefactoring;
import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;
import gr.uom.java.xmi.diff.CandidateMergeVariableRefactoring;
import gr.uom.java.xmi.diff.CandidateSplitVariableRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractPreconditionRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationDetection;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.InvertConditionRefactoring;
import gr.uom.java.xmi.diff.LeafMappingProvider;
import gr.uom.java.xmi.diff.MergeCatchRefactoring;
import gr.uom.java.xmi.diff.MergeConditionalRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.ReferenceBasedRefactoring;
import gr.uom.java.xmi.diff.RemoveParameterRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithLambdaRefactoring;
import gr.uom.java.xmi.diff.ReplaceLoopWithPipelineRefactoring;
import gr.uom.java.xmi.diff.ReplacePipelineWithLoopRefactoring;
import gr.uom.java.xmi.diff.SplitConditionalRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLCommentListDiff;
import gr.uom.java.xmi.diff.UMLDocumentationDiffProvider;
import gr.uom.java.xmi.diff.UMLJavadocDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLParameterDiff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jdt.core.dom.ASTNode;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

public class UMLOperationBodyMapper implements Comparable<UMLOperationBodyMapper>, UMLDocumentationDiffProvider {
	private VariableDeclarationContainer container1;
	private VariableDeclarationContainer container2;
	private Set<AbstractCodeMapping> mappings;
	private List<AbstractCodeFragment> nonMappedLeavesT1;
	private List<AbstractCodeFragment> nonMappedLeavesT2;
	private List<CompositeStatementObject> nonMappedInnerNodesT1;
	private List<CompositeStatementObject> nonMappedInnerNodesT2;
	private Set<Integer> mappingHashcodesT1 = new HashSet<Integer>();
	private Set<Integer> mappingHashcodesT2 = new HashSet<Integer>();
	private Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
	private Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables = new LinkedHashSet<>();
	private Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRefactoring>();
	private Set<CandidateMergeVariableRefactoring> candidateAttributeMerges = new LinkedHashSet<CandidateMergeVariableRefactoring>();
	private Set<CandidateSplitVariableRefactoring> candidateAttributeSplits = new LinkedHashSet<CandidateSplitVariableRefactoring>();
	private Set<UMLAnonymousClassDiff> anonymousClassDiffs = new LinkedHashSet<UMLAnonymousClassDiff>();
	private Set<UMLOperationBodyMapper> childMappers = new LinkedHashSet<UMLOperationBodyMapper>();
	private UMLOperationBodyMapper parentMapper;
	private static final int MAXIMUM_NUMBER_OF_COMPARED_STATEMENTS = 2000;
	private UMLOperationDiff operationSignatureDiff;
	private UMLAbstractClassDiff classDiff;
	private UMLModelDiff modelDiff;
	private VariableDeclarationContainer callSiteOperation;
	private Map<AbstractCodeFragment, VariableDeclarationContainer> codeFragmentOperationMap1 = new LinkedHashMap<AbstractCodeFragment, VariableDeclarationContainer>();
	private Map<AbstractCodeFragment, VariableDeclarationContainer> codeFragmentOperationMap2 = new LinkedHashMap<AbstractCodeFragment, VariableDeclarationContainer>();
	private Set<VariableDeclaration> removedVariables;
	private Set<VariableDeclaration> addedVariables;
	private Set<Pair<VariableDeclaration, VariableDeclaration>> movedVariables;
	private int callsToExtractedMethod = 0;
	private int callsToInlinedMethod = 0;
	private List<Set<AbstractCodeMapping>> internalParameterizeTestMultiMappings = new ArrayList<Set<AbstractCodeMapping>>();
	private boolean nested;
	private boolean lambdaBodyMapper;
	private boolean anonymousCollapse;
	private AbstractCall operationInvocation;
	private Map<String, String> parameterToArgumentMap1;
	private Map<String, String> parameterToArgumentMap2;
	private Set<CompositeStatementObjectMapping> ifBecomingElseIf = new HashSet<>();
	private Set<CompositeStatementObjectMapping> ifAddingElseIf = new HashSet<>();
	private Map<UMLOperation, Set<AbstractCodeFragment>> extractedStatements = new LinkedHashMap<>();
	private List<AbstractCall> invocationsInSourceOperationAfterExtraction;
	private Optional<UMLJavadocDiff> javadocDiff = Optional.empty();
	private UMLCommentListDiff commentListDiff;
	private Set<Pair<AbstractCodeFragment, UMLComment>> commentedCode = new LinkedHashSet<>();
	private Set<Pair<UMLComment, AbstractCodeFragment>> unCommentedCode = new LinkedHashSet<>();
	
	public List<AbstractCall> getInvocationsInSourceOperationAfterExtraction() {
		if(invocationsInSourceOperationAfterExtraction == null) {
			this.invocationsInSourceOperationAfterExtraction = ExtractOperationDetection.getInvocationsInSourceOperationAfterExtraction(this);
		}
		return invocationsInSourceOperationAfterExtraction;
	}

	public boolean isNested() {
		return nested;
	}

	public boolean isAnonymousCollapse() {
		return anonymousCollapse;
	}

	private Set<AbstractCodeFragment> statementsWithStreamAPICalls(List<AbstractCodeFragment> leaves) {
		Set<AbstractCodeFragment> streamAPICalls = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(JAVA.LAMBDA_ARROW) ||
					invocation.actualString().contains(JAVA.METHOD_REFERENCE))) {
				for(AbstractCall inv : statement.getMethodInvocations()) {
					if(streamAPIName(inv.getName())) {
						streamAPICalls.add(statement);
						break;
					}
				}
			}
		}
		return streamAPICalls;
	}

	//Mappers for Move Code
	public UMLOperationBodyMapper(UMLOperationBodyMapper mapper1, UMLOperationBodyMapper mapper2, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = mapper1.getContainer1();
		this.container2 = mapper2.getContainer2();
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		List<AbstractCodeFragment> leaves1 = new ArrayList<>(mapper1.getNonMappedLeavesT1());
		for(AbstractCodeMapping mapping : mapper1.getMappings()) {
			if(mapping instanceof LeafMapping && !mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				leaves1.add(mapping.getFragment1());
			}
			if(mapping instanceof CompositeStatementObjectMapping) {
				break;
			}
		}
		List<AbstractCodeFragment> leaves2 = new ArrayList<>(mapper2.getNonMappedLeavesT2());
		List<CompositeStatementObject> innerNodes1 = new ArrayList<>(mapper1.getNonMappedInnerNodesT1());
		List<CompositeStatementObject> innerNodes2 = new ArrayList<>(mapper2.getNonMappedInnerNodesT2());
		resetNodes(leaves1);
		resetNodes(leaves2);
		processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		resetNodes(innerNodes1);
		resetNodes(innerNodes2);
		processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		updateNonMappedLeavesT1(leaves1);
		updateNonMappedLeavesT2(leaves2);
		nonMappedInnerNodesT1.addAll(innerNodes1);
		nonMappedInnerNodesT2.addAll(innerNodes2);
		if(mapper1.commentListDiff != null && mapper2.commentListDiff != null) {
			this.commentListDiff = new UMLCommentListDiff(mapper1.commentListDiff.getDeletedComments(), mapper2.commentListDiff.getAddedComments(), this);
			checkUnmatchedStatementsBeingCommented();
		}
	}

	public UMLOperationBodyMapper(VariableDeclarationContainer removedOperation, UMLOperationBodyMapper mapper2, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = removedOperation;
		this.container2 = mapper2.getContainer2();
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		OperationBody body1 = removedOperation.getBody();
		if(body1 != null) {
			List<AbstractCodeFragment> leaves1 = new ArrayList<>(body1.getCompositeStatement().getLeaves());
			List<AbstractCodeFragment> leaves2 = new ArrayList<>(mapper2.getNonMappedLeavesT2());
			List<CompositeStatementObject> innerNodes1 = new ArrayList<>(body1.getCompositeStatement().getInnerNodes());
			List<CompositeStatementObject> innerNodes2 = new ArrayList<>(mapper2.getNonMappedInnerNodesT2());
			resetNodes(leaves1);
			resetNodes(leaves2);
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
			resetNodes(innerNodes1);
			resetNodes(innerNodes2);
			processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), false);
			updateNonMappedLeavesT1(leaves1);
			updateNonMappedLeavesT2(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			if(mapper2.commentListDiff != null) {
				this.commentListDiff = new UMLCommentListDiff(container1.getComments(), mapper2.commentListDiff.getAddedComments(), this);
				checkUnmatchedStatementsBeingCommented();
			}
		}
	}

	public UMLOperationBodyMapper(UMLOperationBodyMapper mapper1, VariableDeclarationContainer addedOperation, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = mapper1.getContainer1();
		this.container2 = addedOperation;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		OperationBody body2 = addedOperation.getBody();
		if(body2 != null) {
			List<AbstractCodeFragment> leaves1 = new ArrayList<>(mapper1.getNonMappedLeavesT1());
			List<AbstractCodeFragment> leaves2 = new ArrayList<>(body2.getCompositeStatement().getLeaves());
			List<CompositeStatementObject> innerNodes1 = new ArrayList<>(mapper1.getNonMappedInnerNodesT1());
			List<CompositeStatementObject> innerNodes2 = new ArrayList<>(body2.getCompositeStatement().getInnerNodes());
			resetNodes(leaves1);
			resetNodes(leaves2);
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
			resetNodes(innerNodes1);
			resetNodes(innerNodes2);
			processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), false);
			updateNonMappedLeavesT1(leaves1);
			updateNonMappedLeavesT2(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			if(mapper1.commentListDiff != null) {
				this.commentListDiff = new UMLCommentListDiff(mapper1.commentListDiff.getDeletedComments(), container2.getComments(), this);
				checkUnmatchedStatementsBeingCommented();
			}
		}
	}

	private static boolean containsAll(LambdaExpressionObject bigLambda, List<LambdaExpressionObject> small) {
		int count = 0;
		for(LambdaExpressionObject lambda : small) {
			for(LambdaExpressionObject nestedLambda : bigLambda.getAllLambdas()) {
				if(nestedLambda.getString().equals(lambda.getString())) {
					count++;
					break;
				}
			}
		}
		return count == small.size();
	}

	public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = operation1;
		this.container2 = operation2;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		this.operationSignatureDiff = new UMLOperationDiff(operation1, operation2, classDiff);
		OperationBody body1 = operation1.getBody();
		OperationBody body2 = operation2.getBody();
		if(body1 != null && body2 != null) {
			if(classDiff != null) {
				List<String> list1 = body1.stringRepresentation();
				for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
					if(addedOperation.getBody() != null) {
						List<String> list2 = new ArrayList<>(addedOperation.getBody().stringRepresentation());
						if(list2.size() > 3) {
							//remove first and last blocks
							list2.remove(0);
							list2.remove(list2.size()-1);
							int indexOfSubList = Collections.indexOfSubList(list1, list2);
							if(indexOfSubList >= 0) {
								while(list2.contains("}")) {
									list2.remove("}");
								}
								List<AbstractStatement> allStatements = body1.getCompositeStatement().getAllStatements();
								Set<AbstractCodeFragment> subSet = new LinkedHashSet<AbstractCodeFragment>();
								boolean firstFound = false;
								int index = 0;
								for(AbstractStatement statement1 : allStatements) {
									if(index == list2.size()) {
										firstFound = false;
										index = 0;
									}
									if(!firstFound) {
										if(statement1 instanceof CompositeStatementObject) {
											CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
											if(comp1.toStringForStringRepresentation().equals(list1.get(indexOfSubList))) {
												firstFound = true;
												subSet.add(statement1);
												index++;
											}
										}
										else if(statement1.getString().equals(list1.get(indexOfSubList))) {
											firstFound = true;
											subSet.add(statement1);
											index++;
										}
									}
									else {
										if(statement1 instanceof CompositeStatementObject) {
											CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
											if(comp1.toStringForStringRepresentation().equals(list2.get(index))) {
												subSet.add(statement1);
											}
										}
										else if(statement1.getString().equals(list2.get(index))) {
											subSet.add(statement1);
										}
										index++;
									}
								}
								extractedStatements.put(addedOperation, subSet);
							}
							else if(list1.containsAll(list2) && list2.size() >= 10) {
								while(list2.contains("}")) {
									list2.remove("}");
								}
								List<AbstractStatement> allStatements = body1.getCompositeStatement().getAllStatements();
								Set<AbstractCodeFragment> subSet = new LinkedHashSet<AbstractCodeFragment>();
								boolean firstFound = false;
								int index = 0;
								for(AbstractStatement statement1 : allStatements) {
									if(statement1.isLastStatementWithReturn()) {
										break;
									}
									if(!firstFound) {
										if(statement1 instanceof CompositeStatementObject) {
											CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
											if(comp1.toStringForStringRepresentation().equals(list2.get(0))) {
												firstFound = true;
												subSet.add(statement1);
												index++;
											}
										}
										else if(statement1.getString().equals(list2.get(0))) {
											firstFound = true;
											subSet.add(statement1);
											index++;
										}
									}
									else if(index < list2.size()) {
										if(statement1 instanceof CompositeStatementObject) {
											CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
											if(comp1.toStringForStringRepresentation().equals(list2.get(index))) {
												subSet.add(statement1);
												index++;
											}
										}
										else if(statement1.getString().equals(list2.get(index))) {
											subSet.add(statement1);
											index++;
										}
										else {
											int tmpIndex = index + 1;
											//skip statements in extracted method
											for(int i=tmpIndex; i<list2.size(); i++) {
												if(statement1 instanceof CompositeStatementObject) {
													CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
													if(comp1.toStringForStringRepresentation().equals(list2.get(i))) {
														subSet.add(statement1);
														index = i+1;
														break;
													}
												}
												else if(statement1.getString().equals(list2.get(i))) {
													subSet.add(statement1);
													index = i+1;
													break;
												}
											}
										}
									}
								}
								extractedStatements.put(addedOperation, subSet);
							}
						}
					}
				}
			}
			List<AnonymousClassDeclarationObject> anonymous1 = body1.getAllAnonymousClassDeclarations();
			List<AnonymousClassDeclarationObject> nestedAnonymous1 = new ArrayList<AnonymousClassDeclarationObject>();
			int doubleBraceInitializations1 = 0;
			for(AnonymousClassDeclarationObject anonymous : anonymous1) {
				if(anonymous.toString().startsWith("{\n{")) {
					doubleBraceInitializations1++;
				}
				nestedAnonymous1.addAll(anonymous.getAnonymousClassDeclarationsRecursively());
			}
			List<AnonymousClassDeclarationObject> anonymous2 = body2.getAllAnonymousClassDeclarations();
			List<AnonymousClassDeclarationObject> nestedAnonymous2 = new ArrayList<AnonymousClassDeclarationObject>();
			int doubleBraceInitializations2 = 0;
			for(AnonymousClassDeclarationObject anonymous : anonymous2) {
				if(anonymous.toString().startsWith("{\n{")) {
					doubleBraceInitializations2++;
				}
				nestedAnonymous2.addAll(anonymous.getAnonymousClassDeclarationsRecursively());
			}
			List<LambdaExpressionObject> lambdas1 = body1.getAllLambdas();
			List<LambdaExpressionObject> nestedLambdas1 = new ArrayList<>();
			int lambdasWithBody1 = 0;
			int lambdasWithExpression1 = 0;
			for(LambdaExpressionObject lambda1 : lambdas1) {
				if(lambda1.getBody() != null)
					lambdasWithBody1++;
				if(lambda1.getExpression() != null)
					lambdasWithExpression1++;
				collectNestedLambdaExpressions(lambda1, nestedLambdas1);
			}
			List<LambdaExpressionObject> lambdas2 = body2.getAllLambdas();
			List<LambdaExpressionObject> nestedLambdas2 = new ArrayList<>();
			int lambdasWithBody2 = 0;
			int lambdasWithExpression2 = 0;
			for(LambdaExpressionObject lambda2 : lambdas2) {
				if(lambda2.getBody() != null)
					lambdasWithBody2++;
				if(lambda2.getExpression() != null)
					lambdasWithExpression2++;
				collectNestedLambdaExpressions(lambda2, nestedLambdas2);
			}
			CompositeStatementObject composite1 = body1.getCompositeStatement();
			CompositeStatementObject composite2 = body2.getCompositeStatement();
			List<AbstractCodeFragment> leaves1 = composite1.getLeaves();
			List<AbstractCodeFragment> leaves2 = composite2.getLeaves();
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			innerNodes1.remove(composite1);
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			innerNodes2.remove(composite2);
			int totalNodes1 = leaves1.size() + innerNodes1.size();
			int totalNodes2 = leaves2.size() + innerNodes2.size();
			int assertThrows1 = 0;
			int assertThatThrownBy1 = 0;
			for(AbstractCall call : container1.getAllOperationInvocations()) {
				if(call.getName().equals(JAVA.ASSERT_THROWS)) {
					assertThrows1++;
				}
				else if(call.getName().equals(JAVA.ASSERT_THAT_THROWN_BY)) {
					assertThatThrownBy1++;
				}
			}
			int assertThrows2 = 0;
			int assertThatThrownBy2 = 0;
			for(AbstractCall call : container2.getAllOperationInvocations()) {
				if(call.getName().equals(JAVA.ASSERT_THROWS)) {
					assertThrows2++;
				}
				else if(call.getName().equals(JAVA.ASSERT_THAT_THROWN_BY)) {
					assertThatThrownBy2++;
				}
			}
			boolean anonymousCollapse = Math.abs(totalNodes1 - totalNodes2) > Math.min(totalNodes1, totalNodes2);
			if(!operation1.isDeclaredInAnonymousClass() && !operation2.isDeclaredInAnonymousClass() && anonymousCollapse) {
				if((anonymous1.size() == 1 && anonymous2.size() == 0) ||
						(anonymous1.size() == 1 && anonymous2.size() == 1 && anonymous1.get(0).getAnonymousClassDeclarations().size() > 0 && anonymous2.get(0).getAnonymousClassDeclarations().size() == 0) ||
						(anonymous1.size() + nestedAnonymous1.size() == anonymous2.size() + nestedAnonymous2.size() + 1 && anonymous1.get(0).getAnonymousClassDeclarations().size() > 0)) {
					this.anonymousCollapse = true;
					AbstractCodeFragment anonymousFragment = null;
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(leaf1.getAnonymousClassDeclarations().size() > 0) {
							anonymousFragment = leaf1;
							break;
						}
					}
					if(anonymousFragment != null) {
						expandAnonymousAndLambdas(anonymousFragment, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
						if(leaves2.size() == 1 && lambdas2.size() > 0) {
							expandAnonymousAndLambdas(leaves2.get(0), leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
							for(AbstractCodeFragment leaf1 : new ArrayList<>(leaves1)) {
								if(!leaf1.equals(anonymousFragment) && leaf1.getLambdas().size() > 0) {
									expandAnonymousAndLambdas(leaf1, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
								}
							}
						}
					}
				}
				else if(anonymous2.size() == 0 && ((lambdas1.size() == 1 && lambdas2.size() == 0) || (lambdas1.size() == 1 && containsAll(lambdas1.get(0), lambdas2)))) {
					AbstractCodeFragment lambdaFragment = null;
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(leaf1.getLambdas().size() > 0) {
							lambdaFragment = leaf1;
							break;
						}
					}
					if(lambdaFragment != null) {
						expandAnonymousAndLambdas(lambdaFragment, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
					}
				}
				else if((anonymous1.size() == 0 && anonymous2.size() == 1) ||
						(anonymous1.size() == 1 && anonymous2.size() == 1 && anonymous1.get(0).getAnonymousClassDeclarations().size() == 0 && anonymous2.get(0).getAnonymousClassDeclarations().size() > 0) ||
						(anonymous1.size() + nestedAnonymous1.size() + 1 == anonymous2.size() + nestedAnonymous2.size() && anonymous2.get(0).getAnonymousClassDeclarations().size() > 0)) {
					this.anonymousCollapse = true;
					AbstractCodeFragment anonymousFragment = null;
					for(AbstractCodeFragment leaf2 : leaves2) {
						if(leaf2.getAnonymousClassDeclarations().size() > 0) {
							anonymousFragment = leaf2;
							break;
						}
					}
					if(anonymousFragment != null) {
						expandAnonymousAndLambdas(anonymousFragment, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
						if(leaves1.size() == 1 && lambdas1.size() > 0) {
							expandAnonymousAndLambdas(leaves1.get(0), leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
							for(AbstractCodeFragment leaf2 : new ArrayList<>(leaves2)) {
								if(!leaf2.equals(anonymousFragment) && leaf2.getLambdas().size() > 0) {
									expandAnonymousAndLambdas(leaf2, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
								}
							}
						}
					}
				}
				else if(anonymous1.size() == 0 && ((lambdas1.size() == 0 && lambdas2.size() == 1) || (lambdas2.size() == 1 && containsAll(lambdas2.get(0), lambdas1)))) {
					AbstractCodeFragment lambdaFragment = null;
					for(AbstractCodeFragment leaf2 : leaves2) {
						if(leaf2.getLambdas().size() > 0) {
							lambdaFragment = leaf2;
							break;
						}
					}
					if(lambdaFragment != null) {
						expandAnonymousAndLambdas(lambdaFragment, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
					}
				}
				else if (assertThrows1 == 0 && assertThrows2 > 0) {
					handleAssertThrowsLambda(leaves1, leaves2, innerNodes2, lambdas2, operation2);
				}
				else if (assertThatThrownBy1 == 0 && assertThatThrownBy2 > 0) {
					handleAssertThrowsLambda(leaves1, leaves2, innerNodes2, lambdas2, operation2);
				}
			}
			else if(operation1.hasTestAnnotation() && operation2.hasTestAnnotation() && assertThrows2 > 0 &&
					(lambdas2.size() + nestedLambdas2.size() == lambdas1.size() + nestedLambdas1.size() + assertThrows2 ||
					lambdas2.size() == lambdas1.size() + assertThrows2 || assertThrows1 == 0)) {
				handleAssertThrowsLambda(leaves1, leaves2, innerNodes2, lambdas2, operation2);
			}
			else if(operation1.hasTestAnnotation() && operation2.hasTestAnnotation() && assertThatThrownBy2 > 0 &&
					(lambdas2.size() + nestedLambdas2.size() == lambdas1.size() + nestedLambdas1.size() + assertThatThrownBy2 ||
					lambdas2.size() == lambdas1.size() + assertThatThrownBy2 || assertThatThrownBy1 == 0)) {
				handleAssertThrowsLambda(leaves1, leaves2, innerNodes2, lambdas2, operation2);
			}
			else if(doubleBraceInitializations1 > 0 && doubleBraceInitializations2 == 0) {
				for(AbstractCodeFragment leaf1 : new ArrayList<>(leaves1)) {
					if(leaf1.getAnonymousClassDeclarations().size() > 0) {
						expandAnonymousAndLambdas(leaf1, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
					}
				}
			}
			else if(doubleBraceInitializations1 == 0 && doubleBraceInitializations2 > 0) {
				for(AbstractCodeFragment leaf2 : new ArrayList<>(leaves2)) {
					if(leaf2.getAnonymousClassDeclarations().size() > 0) {
						expandAnonymousAndLambdas(leaf2, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
					}
				}
			}
			else if(lambdasWithBody2 == 1 && lambdasWithBody1 == 0) {
				AbstractCodeFragment lambdaFragment = null;
				for(AbstractCodeFragment leaf2 : leaves2) {
					if(leaf2.getLambdas().size() > 0 && leaf2.getLambdas().get(0).getBody() != null) {
						lambdaFragment = leaf2;
						break;
					}
				}
				if(lambdaFragment != null) {
					expandAnonymousAndLambdas(lambdaFragment, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
				}
			}
			Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
			Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				for(AbstractCodeFragment streamAPICall : streamAPIStatements2) {
					if(streamAPICall.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(streamAPICall, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, false);
					}
				}
				for(AbstractCodeFragment leaf1 : new ArrayList<>(leaves1)) {
					if(leaf1.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(leaf1, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
					}
				}
			}
			else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
				for(AbstractCodeFragment streamAPICall : streamAPIStatements1) {
					if(streamAPICall.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(streamAPICall, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, false);
					}
				}
				for(AbstractCodeFragment leaf2 : new ArrayList<>(leaves2)) {
					if(leaf2.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(leaf2, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
					}
				}
			}
			if(lambdas1.size() == lambdas2.size() && (lambdasWithBody1 != lambdasWithBody2 || nestedLambdas1.size() != nestedLambdas2.size())) {
				for(AbstractCodeFragment leaf1 : new ArrayList<>(leaves1)) {
					if(leaf1.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(leaf1, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
					}
				}
				for(AbstractCodeFragment leaf2 : new ArrayList<>(leaves2)) {
					if(leaf2.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(leaf2, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
					}
				}
			}
			Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<String, String>();
			Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<String, String>();
			List<VariableDeclaration> addedParameters = operationSignatureDiff.getAddedParameters();
			if(addedParameters.size() == 1) {
				VariableDeclaration addedParameter = addedParameters.get(0);
				if(!operation1.isDeclaredInAnonymousClass() && UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName())) {
					parameterToArgumentMap1.put(JAVA.THIS_DOT, "");
					//replace "parameterName." with ""
					parameterToArgumentMap2.put(addedParameter.getVariableName() + ".", "");
				}
			}
			List<VariableDeclaration> removedParameters = operationSignatureDiff.getRemovedParameters();
			if(removedParameters.size() == 1) {
				VariableDeclaration removedParameter = removedParameters.get(0);
				if(!operation2.isDeclaredInAnonymousClass() && UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put(removedParameter.getVariableName() + ".", "");
					parameterToArgumentMap2.put(JAVA.THIS_DOT, "");
				}
			}
			List<UMLParameterDiff> parameterDiffList = operationSignatureDiff.getParameterDiffList();
			for(UMLParameterDiff parameterDiff : parameterDiffList) {
				VariableDeclaration addedParameter = parameterDiff.getAddedParameter();
				VariableDeclaration removedParameter = parameterDiff.getRemovedParameter();
				if(!operation1.isDeclaredInAnonymousClass() && !operation2.isDeclaredInAnonymousClass() &&
						UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName()) &&
						UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put(JAVA.THIS_DOT, "");
					parameterToArgumentMap2.put(addedParameter.getVariableName() + ".", "");
					parameterToArgumentMap1.put(removedParameter.getVariableName() + ".", "");
					parameterToArgumentMap2.put(JAVA.THIS_DOT, "");
				}
			}
			if(classDiff != null) {
				for(UMLAttribute attribute : classDiff.getOriginalClass().getAttributes()) {
					if(!operation2.isDeclaredInAnonymousClass() && UMLModelDiff.looksLikeSameType(attribute.getType().getClassType(), operation2.getClassName())) {
						parameterToArgumentMap1.put(attribute.getName() + ".", "");
						parameterToArgumentMap2.put(JAVA.THIS_DOT, "");
					}
				}
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(AbstractCodeFragment leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(AbstractCodeFragment leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			boolean isomorphic = isomorphicCompositeStructure(innerNodes1, innerNodes2);
			//check if it is possible to group statements based on comments, useful for junit tests
			if((operation1.hasTestAnnotation() || operation1.hasDataPointsAnnotation() || operation1.hasDataProviderAnnotation() || operation1.hasParametersAnnotation()) &&
					(operation2.hasTestAnnotation() || operation2.hasDataPointsAnnotation() || operation2.hasDataProviderAnnotation() || operation2.hasParametersAnnotation()) &&
					operation1.getBodyHashCode() != operation2.getBodyHashCode()) {
				List<CodeBlockBetweenComments> blocks1 = CodeBlockBetweenComments.generateCodeBlocks(leaves1, operation1);
				List<CodeBlockBetweenComments> blocks2 = CodeBlockBetweenComments.generateCodeBlocks(leaves2, operation2);
				if(blocks1.size() <= blocks2.size()) {
					List<CodeBlockBetweenComments> nonMatchedBlocks1 = new ArrayList<CodeBlockBetweenComments>();
					for(CodeBlockBetweenComments block1 : blocks1) {
						boolean matchFound = false;
						for(CodeBlockBetweenComments block2 : blocks2) {
							if(block1.compatible(block2)) {
								matchFound = true;
								processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
								break;
							}
						}
						if(!matchFound) {
							for(CodeBlockBetweenComments block2 : blocks2) {
								if(block1.compatibleWithAfterEnd(block2)) {
									matchFound = true;
									processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
									break;
								}
							}
						}
						if(!matchFound) {
							for(CodeBlockBetweenComments block2 : blocks2) {
								if(block1.identicalStartCommentAndCode(block2)) {
									matchFound = true;
									processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
									break;
								}
							}
						}
						if(!matchFound) {
							for(CodeBlockBetweenComments block2 : blocks2) {
								if(block1.identicalEndCommentAndCode(block2)) {
									matchFound = true;
									processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
									break;
								}
							}
						}
						if(!matchFound) {
							nonMatchedBlocks1.add(block1);
						}
					}
					for(CodeBlockBetweenComments block1 : nonMatchedBlocks1) {
						int index = blocks1.indexOf(block1);
						boolean previousMatched = index > 0 && !nonMatchedBlocks1.contains(blocks1.get(index-1));
						boolean nextMatched = index < blocks1.size()-1 && !nonMatchedBlocks1.contains(blocks1.get(index+1));
						if(previousMatched && nextMatched) {
							leaves1.removeAll(block1.getLeaves());
						}
					}
				}
				else {
					List<CodeBlockBetweenComments> nonMatchedBlocks2 = new ArrayList<CodeBlockBetweenComments>();
					for(CodeBlockBetweenComments block2 : blocks2) {
						boolean matchFound = false;
						for(CodeBlockBetweenComments block1 : blocks1) {
							if(block1.compatible(block2)) {
								matchFound = true;
								processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
								break;
							}
						}
						if(!matchFound) {
							for(CodeBlockBetweenComments block1 : blocks1) {
								if(block1.compatibleWithAfterEnd(block2)) {
									matchFound = true;
									int index = blocks1.indexOf(block1);
									CodeBlockBetweenComments nextBlock1 = index < blocks1.size()-1 ? blocks1.get(index+1) : null;
									CodeBlockBetweenComments union = null;
									if(nextBlock1 != null && nextBlock1.equalEndComment(block2)) {
										union = CodeBlockBetweenComments.union(block1, nextBlock1);
									}
									processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, union != null ? union : block1, block2);
									break;
								}
							}
						}
						if(!matchFound) {
							for(CodeBlockBetweenComments block1 : blocks1) {
								if(block1.identicalStartCommentAndCode(block2)) {
									matchFound = true;
									processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
									break;
								}
							}
						}
						if(!matchFound) {
							for(CodeBlockBetweenComments block1 : blocks1) {
								if(block1.identicalEndCommentAndCode(block2)) {
									matchFound = true;
									processCodeBlocksBetweenComments(leaves1, leaves2, isomorphic, block1, block2);
									break;
								}
							}
						}
						if(!matchFound) {
							nonMatchedBlocks2.add(block2);
						}
					}
					for(CodeBlockBetweenComments block2 : nonMatchedBlocks2) {
						int index = blocks2.indexOf(block2);
						boolean previousMatched = index > 0 && !nonMatchedBlocks2.contains(blocks2.get(index-1));
						boolean nextMatched = index < blocks2.size()-1 && !nonMatchedBlocks2.contains(blocks2.get(index+1));
						if(previousMatched && nextMatched) {
							leaves2.removeAll(block2.getLeaves());
						}
					}
				}
			}
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), isomorphic);
			
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNodes2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			boolean containsCallToExtractedMethod = containsCallToExtractedMethod(leaves2);
			processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), containsCallToExtractedMethod);
			
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
				processStreamAPIStatements(leaves1, leaves2, streamAPIStatements1, innerNodes2);
			}
			else if(streamAPIStatements1.size() > 1 || streamAPIStatements2.size() > 1) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			else if(streamAPIStatements1.size() == 1 && streamAPIStatements2.size() == 1 && nestedLambdas1.size() != nestedLambdas2.size()) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			
			for(Refactoring r : this.refactorings) {
				if(r instanceof ReplaceLoopWithPipelineRefactoring) {
					ReplaceLoopWithPipelineRefactoring refactoring = (ReplaceLoopWithPipelineRefactoring)r;
					CompositeStatementObject parent1 = null;
					for(AbstractCodeFragment fragment : refactoring.getCodeFragmentsBefore()) {
						if(fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
								fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
								fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
								fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
							parent1 = fragment.getParent();
							break;
						}
					}
					CompositeStatementObject parent2 = null;
					for(AbstractCodeFragment fragment : refactoring.getCodeFragmentsAfter()) {
						parent2 = fragment.getParent();
						break;
					}
					if(parent1 != null && parent2 != null && parent1.getParent() != null && parent2.getParent() != null) {
						boolean parentMappingFound = false;
						for(AbstractCodeMapping mapping : this.mappings) {
							if(mapping.getFragment1().equals(parent1) && mapping.getFragment2().equals(parent2)) {
								parentMappingFound = true;
							}
						}
						if(!parentMappingFound) {
							List<CompositeStatementObject> nodes1 = new ArrayList<>();
							while(parent1.getParent() != null) {
								if(innerNodes1.contains(parent1)) {
									nodes1.add(parent1);
								}
								parent1 = parent1.getParent();
							}
							List<CompositeStatementObject> nodes2 = new ArrayList<>();
							while(parent2.getParent() != null) {
								if(innerNodes2.contains(parent2)) {
									nodes2.add(parent2);
								}
								parent2 = parent2.getParent();
							}
							processInnerNodes(nodes1, nodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), false);
						}
					}
				}
			}
			//match expressions in inner nodes from T1 with leaves from T2
			List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
			int remainingUnmatchedIfStatements1 = 0;
			for(CompositeStatementObject composite : innerNodes1) {
				if(!nonMappedCompositeExistsIdenticalInExtractedMethod(composite)) {
					if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						remainingUnmatchedIfStatements1++;
					}
					for(AbstractExpression expression : composite.getExpressions()) {
						expression.replaceParametersWithArguments(parameterToArgumentMap1);
						expressionsT1.add(expression);
					}
				}
			}
			for(AbstractCodeMapping mapping : mappings) {
				if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().equalFragment(mapping.getFragment2()) && !mapping.isIdenticalWithExtractedVariable() && !mapping.isIdenticalWithInlinedVariable()) {
					CompositeStatementObject composite = (CompositeStatementObject)mapping.getFragment1();
					for(AbstractExpression expression : composite.getExpressions()) {
						if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME) &&
								mapping.getFragment1().getVariableDeclarations().toString().equals(mapping.getFragment2().getVariableDeclarations().toString())) {
							continue;
						}
						AbstractCall call1 = expression.invocationCoveringEntireFragment();
						if(call1 != null) {
							CompositeStatementObject comp2 = (CompositeStatementObject)mapping.getFragment2();
							if(comp2.getExpressions().size() == 1) {
								AbstractExpression expression2 = comp2.getExpressions().get(0);
								AbstractCall call2 = expression2.invocationCoveringEntireFragment();
								if(call2 != null && call1.identicalExpression(call2) && (call1.equalArguments(call2) ||
										call1.identicalOrReplacedArguments(call2, mapping.getReplacements(), Collections.emptyList()))) {
									continue;
								}
							}
						}
						expression.replaceParametersWithArguments(parameterToArgumentMap1);
						expressionsT1.add(expression);
					}
				}
				if(remainingUnmatchedIfStatements1 > 0 && mapping instanceof LeafMapping && mapping.getFragment1().getTernaryOperatorExpressions().size() == 0 &&
						(mapping.getFragment2().getTernaryOperatorExpressions().size() > 0 || mapping.getFragment2().getString().contains(" == ") || mapping.getFragment2().getString().contains(" != ")) &&
						!leaves2.contains(mapping.getFragment2())) {
					leaves2.add(mapping.getFragment2());
					//remove from hashCodes, so that it can be re-matched
					mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
				}
			}
			int numberOfMappings = mappings.size();
			processLeaves(expressionsT1, leaves2, parameterToArgumentMap2, false);
			List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
			for(int i = numberOfMappings; i < mappings.size(); i++) {
				AbstractCodeMapping m = mappings.get(i);
				if(!isSplitConditionalExpression(m)) {
					m.temporaryVariableAssignment(refactorings, leaves2, parentMapper != null);
				}
				else {
					this.mappings.remove(m);
					if(!leaves2.contains(m.getFragment2())) {
						leaves2.add(m.getFragment2());
					}
				}
			}
			
			if(container1.getBodyHashCode() != container2.getBodyHashCode() && containsCallToExtractedMethod) {
				for(Iterator<AbstractCodeMapping> mappingIterator = this.mappings.iterator(); mappingIterator.hasNext();) {
					AbstractCodeMapping mapping = mappingIterator.next();
					boolean ifChangedToElseIf = false;
					if(ifBecomingElseIf.contains(mapping)) {
						int mappedChildrenSize = 0;
						for(AbstractCodeMapping m : this.mappings) {
							if(!mapping.equals(m) && !m.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
									mapping.getFragment1().getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) &&
									mapping.getFragment2().getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
								mappedChildrenSize++;
							}
						}
						ifChangedToElseIf = mappedChildrenSize > 0;
					}
					if(!mapping.containsReplacement(ReplacementType.COMPOSITE) && !nestedUnderSplitConditional(mapping) && !ifChangedToElseIf) {
						AbstractCodeFragment child1 = mapping.getFragment1();
						AbstractCodeFragment child2 = mapping.getFragment2();
						CompositeStatementObject parent1 = child1.getParent();
						CompositeStatementObject parent2 = child2.getParent();
						boolean unmatchedParent = false;
						while(parent1 != null && parent2 != null) {
							if(parent1.getParent() == null || parent2.getParent() == null) {
								if(parent1 instanceof TryStatementObject) {
									break;
								}
								if(parent2 instanceof TryStatementObject) {
									break;
								}
								if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
									break;
								}
								if(parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
									break;
								}
								if(child1.getAnonymousClassDeclarations().size() > 0 && child2.getAnonymousClassDeclarations().size() > 0) {
									break;
								}
								if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) != parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
										!ifToSwitch(parent1, child2)) {
									unmatchedParent = true;
								}
								break;
							}
							if(alreadyMatched1(parent1) && alreadyMatched2(parent2) &&
									!parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
									!parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
								if(parent1.getString().equals(parent2.getString())) {
									break;
								}
								else if(containsMapping(parent1.getParent(), parent2)) {
									break;
								}
								else if(containsMapping(parent1, parent2.getParent())) {
									break;
								}
								else if(parent2.getStatements().size() > 0 && containsMapping(parent1.getParent(), parent2.getStatements().get(0))) {
									break;
								}
								else if(parent1.getStatements().size() > 0 && containsMapping(parent1.getStatements().get(0), parent2.getParent())) {
									break;
								}
							}
							if(!alreadyMatched1(parent1) || !alreadyMatched2(parent2)) {
								int indexOfChildInParent1 = parent1.getStatements().indexOf(child1);
								int indexOfChildInParent2 = parent2.getStatements().indexOf(child2);
								if(indexOfChildInParent1 != indexOfChildInParent2 &&
										!isElseBranch(child1, parent1) && !isElseBranch(child2, parent2) &&
										!elseWithoutBlockBecomingElseWithBlock(parent1, parent2) &&
										!isTryBlock(child1, parent1) && !isTryBlock(child2, parent2) &&
										!isFinallyBlock(child1, parent1) && !isFinallyBlock(child2, parent2) &&
										!ifAddingElseIf(parent1.getParent()) && !ifAddingElseIf(parent2.getParent()) &&
										!ifToSwitch(parent1, parent2)) {
									boolean additionalVariableDeclarationStatements = false;
									if(indexOfChildInParent1 > indexOfChildInParent2) {
										int referencedVariableDeclarationStatements = 0;
										for(int i=0; i<indexOfChildInParent1; i++) {
											AbstractStatement statement = parent1.getStatements().get(i);
											if(statement.getVariableDeclarations().size() > 0) {
												for(LeafExpression variableReference : child1.getVariables()) {
													if(statement.getVariableDeclarations().get(0).getVariableName().equals(variableReference.getString())) {
														referencedVariableDeclarationStatements++;
														break;
													}
												}
											}
										}
										if(referencedVariableDeclarationStatements == Math.abs(indexOfChildInParent1 - indexOfChildInParent2)) {
											additionalVariableDeclarationStatements = true;
										}
									}
									else if(indexOfChildInParent2 > indexOfChildInParent1) {
										int referencedVariableDeclarationStatements = 0;
										for(int i=0; i<indexOfChildInParent2; i++) {
											AbstractStatement statement = parent2.getStatements().get(i);
											if(statement.getVariableDeclarations().size() > 0) {
												for(LeafExpression variableReference : child2.getVariables()) {
													if(statement.getVariableDeclarations().get(0).getVariableName().equals(variableReference.getString())) {
														referencedVariableDeclarationStatements++;
														break;
													}
												}
											}
										}
										if(referencedVariableDeclarationStatements == Math.abs(indexOfChildInParent1 - indexOfChildInParent2)) {
											additionalVariableDeclarationStatements = true;
										}
									}
									if(!additionalVariableDeclarationStatements) {
										unmatchedParent = true;
									}
								}
								break;
							}
							child1 = parent1;
							child2 = parent2;
							parent1 = parent1.getParent();
							parent2 = parent2.getParent();
						}
						if(unmatchedParent) {
							mappingIterator.remove();
							if(mapping instanceof LeafMapping) {
								LeafMapping leafMapping = (LeafMapping)mapping;
								leaves1.add(leafMapping.getFragment1());
								leaves2.add(leafMapping.getFragment2());
								mappingHashcodesT1.remove(mapping.getFragment1().hashCode());
								mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
							}
							else if(mapping instanceof CompositeStatementObjectMapping) {
								CompositeStatementObjectMapping compositeMapping = (CompositeStatementObjectMapping)mapping;
								innerNodes1.add((CompositeStatementObject)compositeMapping.getFragment1());
								innerNodes2.add((CompositeStatementObject)compositeMapping.getFragment2());
							}
						}
					}
				}
			}
			else if(container1.getBodyHashCode() != container2.getBodyHashCode() && !containsCallToExtractedMethod) {
				for(Iterator<AbstractCodeMapping> mappingIterator = this.mappings.iterator(); mappingIterator.hasNext();) {
					AbstractCodeMapping mapping = mappingIterator.next();
					if(mapping.getFragment1().isKeyword() && mapping.getFragment2().isKeyword()) {
						CompositeStatementObject parent1 = mapping.getFragment1().getParent();
						int unmatchedParents1 = 0;
						if(parent1 != null && !alreadyMatched1(parent1)) {
							unmatchedParents1++;
						}
						while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
							parent1 = parent1.getParent();
							if(parent1 != null && !alreadyMatched1(parent1)) {
								unmatchedParents1++;
							}
						}
						CompositeStatementObject parent2 = mapping.getFragment2().getParent();
						int unmatchedParents2 = 0;
						if(parent2 != null && !alreadyMatched2(parent2)) {
							unmatchedParents2++;
						}
						while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
							parent2 = parent2.getParent();
							if(parent2 != null && !alreadyMatched2(parent2)) {
								unmatchedParents2++;
							}
						}
						if(unmatchedParents1 > 1 || unmatchedParents2 > 1) {
							mappingIterator.remove();
							if(mapping instanceof LeafMapping) {
								LeafMapping leafMapping = (LeafMapping)mapping;
								leaves1.add(leafMapping.getFragment1());
								leaves2.add(leafMapping.getFragment2());
								mappingHashcodesT1.remove(mapping.getFragment1().hashCode());
								mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
							}
						}
					}
				}
			}
			
			updateNonMappedLeavesT1(leaves1);
			updateNonMappedLeavesT2(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			Set<AbstractCodeFragment> leavesToBeRemovedT1 = new LinkedHashSet<>();
			Set<AbstractCodeFragment> leavesToBeRemovedT2 = new LinkedHashSet<>();
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
				if(statement.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration = statement.getVariableDeclarations().get(0);
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && (initializer.getMethodInvocations().size() > 0 || initializer.getCreations().size() > 0 || initializer.getTypeLiterals().size() > 0 ||
							initializer.getStringLiterals().size() > 0 || initializer.getCastExpressions().size() > 0 || initializer.getLambdas().size() > 0 || initializer.getInstanceofExpressions().size() > 0 || (initializer.getNumberLiterals().size() > 0 && !isDefaultValue(initializer.getString())))) {
						for(AbstractCodeFragment nonMappedLeaf1 : nonMappedLeavesT1) {
							boolean matchingVariableDeclaration = false;
							List<VariableDeclaration> declarations1 = nonMappedLeaf1.getVariableDeclarations();
							for(VariableDeclaration declaration1 : declarations1) {
								if(declaration1.getVariableName().equals(declaration.getVariableName()) && declaration1.equalType(declaration)) {
									matchingVariableDeclaration = true;
									break;
								}
							}
							String matchingString = null;
							if(!matchingVariableDeclaration && !containsAnonymousClass(nonMappedLeaf1.getString()) &&
									!nonMappedLeaf1.getString().endsWith(JAVA.ASSIGNMENT + initializer + JAVA.STATEMENT_TERMINATION) && !nonMappedLeaf1.getString().contains(JAVA.ASSIGNMENT + initializer + ".") &&
									(matchingString = matchingString(initializer, nonMappedLeaf1)) != null) {
								UMLOperation inlinedOperation = callToInlinedMethod(nonMappedLeaf1);
								boolean matchingInlinedOperationLeaf = false;
								if(inlinedOperation != null) {
									List<AbstractCodeFragment> inlinedOperationLeaves = inlinedOperation.getBody().getCompositeStatement().getLeaves();
									for(AbstractCodeFragment inlinedOperationLeaf : inlinedOperationLeaves) {
										if(statement.getString().equals(inlinedOperationLeaf.getString())) {
											matchingInlinedOperationLeaf = true;
											break;
										}
									}
								}
								boolean anotherExtractVariableWithSameInitializer = false;
								for(Refactoring r : refactorings) {
									if(r instanceof ExtractVariableRefactoring) {
										ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
										if(extract.getVariableDeclaration().getInitializer() != null && extract.getVariableDeclaration().getInitializer().getString().equals(initializer.getString()) &&
												!extract.getVariableDeclaration().getInitializer().getLocationInfo().equals(initializer.getLocationInfo())) {
											continue;
										}
										if(extract.getVariableDeclaration().getInitializer() != null && extract.getVariableDeclaration().getInitializer().getString().contains(initializer.getString())) {
											anotherExtractVariableWithSameInitializer = true;
											break;
										}
									}
								}
								if(!matchingInlinedOperationLeaf && !anotherExtractVariableWithSameInitializer) {
									ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, parentMapper != null);
									ref.addUnmatchedStatementReference(nonMappedLeaf1);
									List<LeafExpression> subExpressions = nonMappedLeaf1.findExpression(initializer.getString());
									if(subExpressions.isEmpty() && matchingString != null) {
										subExpressions = nonMappedLeaf1.findExpression(matchingString);
										if(subExpressions.isEmpty()) {
											subExpressions = nonMappedLeaf1.findExpression(matchingString + ".class");
										}
									}
									for(LeafExpression subExpression : subExpressions) {
										LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
									}
									if(initializer.getString().contains(JAVA.STRING_CONCATENATION)) {
										String[] tokens = StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(initializer.getString());
										for(String token : tokens) {
											List<LeafExpression> concatenatedSubExpressions = nonMappedLeaf1.findExpression(token);
											for(LeafExpression subExpression : concatenatedSubExpressions) {
												LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
												ref.addSubExpressionMapping(leafMapping);
											}
										}
									}
									if(!refactorings.contains(ref)) {
										refactorings.add(ref);
										leavesToBeRemovedT2.add(statement);
									}
								}
							}
						}
					}
				}
			}
			nonMappedLeavesT2.removeAll(leavesToBeRemovedT2);
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
				if(statement.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration = statement.getVariableDeclarations().get(0);
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && (initializer.getMethodInvocations().size() > 0 || initializer.getCreations().size() > 0 || initializer.getTypeLiterals().size() > 0 ||
							initializer.getStringLiterals().size() > 0 || initializer.getCastExpressions().size() > 0 || initializer.getLambdas().size() > 0 || initializer.getInstanceofExpressions().size() > 0 || (initializer.getNumberLiterals().size() > 0 && !isDefaultValue(initializer.getString())))) {
						for(AbstractCodeFragment nonMappedLeaf2 : nonMappedLeavesT2) {
							boolean matchingVariableDeclaration = false;
							List<VariableDeclaration> declarations2 = nonMappedLeaf2.getVariableDeclarations();
							for(VariableDeclaration declaration2 : declarations2) {
								if(declaration2.getVariableName().equals(declaration.getVariableName()) && declaration2.equalType(declaration)) {
									matchingVariableDeclaration = true;
									break;
								}
							}
							String initializerAfterRename = null;
							String matchingString = null;
							if(!matchingVariableDeclaration && !containsAnonymousClass(nonMappedLeaf2.getString()) &&
									!nonMappedLeaf2.getString().endsWith(JAVA.ASSIGNMENT + initializer + JAVA.STATEMENT_TERMINATION) && !nonMappedLeaf2.getString().contains(JAVA.ASSIGNMENT + initializer + ".") &&
									((matchingString = matchingString(initializer, nonMappedLeaf2)) != null || (initializerAfterRename = matchesWithOverlappingRenameVariable(initializer, nonMappedLeaf2)) != null) &&
									existsMappingSubsumingBoth(statement, nonMappedLeaf2)) {
								UMLOperation extractedOperation = callToExtractedMethod(nonMappedLeaf2);
								boolean matchingExtractedOperationLeaf = false;
								if(extractedOperation != null) {
									List<AbstractCodeFragment> extractedOperationLeaves = extractedOperation.getBody().getCompositeStatement().getLeaves();
									for(AbstractCodeFragment extractedOperationLeaf : extractedOperationLeaves) {
										if(statement.getString().equals(extractedOperationLeaf.getString())) {
											matchingExtractedOperationLeaf = true;
											break;
										}
									}
								}
								boolean anotherInlineVariableWithSameInitializer = false;
								for(Refactoring r : refactorings) {
									if(r instanceof InlineVariableRefactoring) {
										InlineVariableRefactoring inline = (InlineVariableRefactoring)r;
										if(inline.getVariableDeclaration().getInitializer() != null && inline.getVariableDeclaration().getInitializer().getString().equals(initializer.getString()) &&
												!inline.getVariableDeclaration().getInitializer().getLocationInfo().equals(initializer.getLocationInfo())) {
											continue;
										}
										if(inline.getVariableDeclaration().getInitializer() != null && inline.getVariableDeclaration().getInitializer().getString().contains(initializer.getString())) {
											anotherInlineVariableWithSameInitializer = true;
											break;
										}
									}
								}
								if(!matchingExtractedOperationLeaf && !anotherInlineVariableWithSameInitializer) {
									InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, parentMapper != null);
									ref.addUnmatchedStatementReference(nonMappedLeaf2);
									List<LeafExpression> subExpressions = nonMappedLeaf2.findExpression(initializerAfterRename != null ? initializerAfterRename : initializer.getString());
									if(subExpressions.isEmpty() && matchingString != null) {
										subExpressions = nonMappedLeaf2.findExpression(matchingString);
										if(subExpressions.isEmpty()) {
											subExpressions = nonMappedLeaf2.findExpression(matchingString + ".class");
										}
									}
									for(LeafExpression subExpression : subExpressions) {
										LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
									}
									if(!refactorings.contains(ref)) {
										refactorings.add(ref);
										leavesToBeRemovedT1.add(statement);
									}
								}
							}
						}
						for(Refactoring r : new ArrayList<>(refactorings)) {
							if(r instanceof ReplaceLoopWithPipelineRefactoring) {
								ReplaceLoopWithPipelineRefactoring pipeline = (ReplaceLoopWithPipelineRefactoring)r;
								for(AbstractCodeFragment nonMappedLeaf2 : pipeline.getCodeFragmentsAfter()) {
									boolean matchingVariableDeclaration = false;
									List<VariableDeclaration> declarations2 = nonMappedLeaf2.getVariableDeclarations();
									for(VariableDeclaration declaration2 : declarations2) {
										if(declaration2.getVariableName().equals(declaration.getVariableName()) && declaration2.equalType(declaration)) {
											matchingVariableDeclaration = true;
											break;
										}
									}
									String initializerAfterRename = null;
									String matchingString = null;
									if(!matchingVariableDeclaration &&
											!nonMappedLeaf2.getString().endsWith(JAVA.ASSIGNMENT + initializer + JAVA.STATEMENT_TERMINATION) &&
											((matchingString = matchingString(initializer, nonMappedLeaf2)) != null || (initializerAfterRename = matchesWithOverlappingRenameVariable(initializer, nonMappedLeaf2)) != null) &&
											existsMappingSubsumingBoth(statement, nonMappedLeaf2)) {
										boolean anotherInlineVariableWithSameInitializer = false;
										for(Refactoring r2 : refactorings) {
											if(r2 instanceof InlineVariableRefactoring) {
												InlineVariableRefactoring inline = (InlineVariableRefactoring)r2;
												if(inline.getVariableDeclaration().getInitializer().getString().equals(initializer.getString()) &&
														!inline.getVariableDeclaration().getInitializer().getLocationInfo().equals(initializer.getLocationInfo())) {
													continue;
												}
												if(inline.getVariableDeclaration().getInitializer().getString().contains(initializer.getString())) {
													anotherInlineVariableWithSameInitializer = true;
													break;
												}
											}
										}
										if(!anotherInlineVariableWithSameInitializer) {
											InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, parentMapper != null);
											ref.addUnmatchedStatementReference(nonMappedLeaf2);
											List<LeafExpression> subExpressions = nonMappedLeaf2.findExpression(initializerAfterRename != null ? initializerAfterRename : initializer.getString());
											if(subExpressions.isEmpty() && matchingString != null) {
												subExpressions = nonMappedLeaf2.findExpression(matchingString);
												if(subExpressions.isEmpty()) {
													subExpressions = nonMappedLeaf2.findExpression(matchingString + ".class");
												}
											}
											for(LeafExpression subExpression : subExpressions) {
												LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
												ref.addSubExpressionMapping(leafMapping);
											}
											if(!refactorings.contains(ref)) {
												refactorings.add(ref);
												leavesToBeRemovedT1.add(statement);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			nonMappedLeavesT1.removeAll(leavesToBeRemovedT1);
		}
		AbstractExpression defaultExpression1 = operation1.getDefaultExpression();
		AbstractExpression defaultExpression2 = operation2.getDefaultExpression();
		if(defaultExpression1 != null && defaultExpression2 != null) {
			List<AbstractExpression> leaves1 = new ArrayList<AbstractExpression>();
			leaves1.add(defaultExpression1);
			List<AbstractExpression> leaves2 = new ArrayList<AbstractExpression>();
			leaves2.add(defaultExpression2);
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		}
		if(operation1.getJavadoc() != null && operation2.getJavadoc() != null) {
			this.operationSignatureDiff = new UMLOperationDiff(this);
			UMLJavadocDiff diff = new UMLJavadocDiff(operation1.getJavadoc(), operation2.getJavadoc(), operationSignatureDiff);
			this.javadocDiff = Optional.of(diff);
		}
		this.commentListDiff = new UMLCommentListDiff(container1.getComments(), container2.getComments(), this);
		checkUnmatchedStatementsBeingCommented();
	}

	private String matchingString(AbstractExpression initializer, AbstractCodeFragment nonMappedLeaf) {
		String string = initializer.getString();
		if(nonMappedLeaf.getString().contains(string)) {
			return string;
		}
		if(string.contains(JAVA.ASSIGNMENT)) {
			String afterAssignment = string.substring(string.indexOf(JAVA.ASSIGNMENT) + 1, string.length());
			String initializerLambdaParameter = null;
			if(string.contains(JAVA.LAMBDA_ARROW)) {
				initializerLambdaParameter = string.substring(0, string.indexOf(JAVA.LAMBDA_ARROW));
			}
			if(nonMappedLeaf.getString().contains(afterAssignment)) {
				return afterAssignment;
			}
			if(initializerLambdaParameter != null) {
				for(LambdaExpressionObject lambda : nonMappedLeaf.getLambdas()) {
					for(String lambdaParameterName : lambda.getParameterNameList()) {
						String temp = ReplacementUtil.performReplacement(afterAssignment, initializerLambdaParameter, lambdaParameterName);
						if(nonMappedLeaf.getString().contains(temp)) {
							boolean skip = false;
							for(Refactoring r : refactorings) {
								if(r instanceof InlineVariableRefactoring) {
									InlineVariableRefactoring inline = (InlineVariableRefactoring)r;
									for(LeafMapping leafMapping : inline.getSubExpressionMappings()) {
										if(leafMapping.getFragment2().getLocationInfo().equals(lambda.getLocationInfo())) {
											skip = true;
											break;
										}
									}
								}
							}
							if(!skip) {
								VariableDeclaration v1 = null;
								for(LambdaExpressionObject l : initializer.getLambdas()) {
									v1 = l.getVariableDeclaration(initializerLambdaParameter);
									if(v1 != null) {
										break;
									}
								}		
								VariableDeclaration v2 = nonMappedLeaf.getVariableDeclaration(lambdaParameterName);
								if(v1 != null && v2 != null && !v1.getVariableName().equals(v2.getVariableName()) &&
										container1.getLocationInfo().getFilePath().equals(v1.getLocationInfo().getFilePath()) &&
										container2.getLocationInfo().getFilePath().equals(v2.getLocationInfo().getFilePath())) {
									UMLParameterDiff parameterDiff = new UMLParameterDiff(v1, v2, container1, container2, mappings, refactorings, classDiff);
									if(!parameterDiff.isEmpty()) {
										refactorings.addAll(parameterDiff.getRefactorings());
									}
								}
								return lambda.getString();
							}
						}
					}
				}
			}
		}
		else if(string.contains(" instanceof ")) {
			String afterInstanceOf = string.substring(string.indexOf(" instanceof ") + " instanceof ".length(), string.length());
			if(nonMappedLeaf.getString().contains(afterInstanceOf)) {
				return afterInstanceOf;
			}
		}
		return null;
	}

	private void processCodeBlocksBetweenComments(List<AbstractCodeFragment> leaves1,
			List<AbstractCodeFragment> leaves2, boolean isomorphic, CodeBlockBetweenComments block1,
			CodeBlockBetweenComments block2) throws RefactoringMinerTimedOutException {
		List<AbstractCodeFragment> l1 = new ArrayList<AbstractCodeFragment>(block1.getLeaves());
		List<AbstractCodeFragment> l2 = new ArrayList<AbstractCodeFragment>(block2.getLeaves());
		int mappingCount = mappings.size();
		processLeaves(l1, l2, new LinkedHashMap<String, String>(), isomorphic);
		if(mappings.size() > mappingCount) {
			for(AbstractCodeFragment leaf1 : block1.getLeaves()) {
				if(alreadyMatched1(leaf1)) {
					leaves1.remove(leaf1);
				}
			}
			for(AbstractCodeFragment leaf2 : block2.getLeaves()) {
				if(alreadyMatched2(leaf2)) {
					leaves2.remove(leaf2);
				}
			}
		}
	}

	private boolean ifToSwitch(AbstractCodeFragment parent1, AbstractCodeFragment parent2) {
		if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
			return true;
		}
		else if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			return true;
		}
		return false;
	}

	private boolean elseWithoutBlockBecomingElseWithBlock(CompositeStatementObject parent1, CompositeStatementObject parent2) {
		if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			CompositeStatementObject grandParent2 = parent2.getParent();
			for(AbstractCodeMapping mapping : this.mappings) {
				if(mapping.getFragment1().equals(parent1) && mapping.getFragment2().equals(grandParent2)) {
					return true;
				}
			}
		}
		else if(parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			CompositeStatementObject grandParent1 = parent1.getParent();
			for(AbstractCodeMapping mapping : this.mappings) {
				if(mapping.getFragment1().equals(grandParent1) && mapping.getFragment2().equals(parent2)) {
					return true;
				}
			}
		}
		return false;
	}

	private void checkUnmatchedStatementsBeingCommented() {
		List<UMLComment> uniqueComments1 = commentListDiff.getDeletedComments();
		List<UMLComment> uniqueComments2 = commentListDiff.getAddedComments();
		// check if unmatched statements from left side have been commented
		if(uniqueComments2.size() > 0 && nonMappedLeavesT1.size() > 0) {
			for(UMLComment comment : uniqueComments2) {
				String text = comment.getText();
				if(text.startsWith("//")) {
					text = text.substring(2);
				}
				text = text.trim();
				ASTNode node = processBlock(text);
				if(node != null) {
					String nodeAsString = stringify(node);
					Set<AbstractCodeFragment> matchingNodes1 = new LinkedHashSet<AbstractCodeFragment>();
					for(AbstractCodeFragment leaf1 : nonMappedLeavesT1) {
						if(leaf1.getString().equals(nodeAsString)) {
							matchingNodes1.add(leaf1);
						}
					}
					if(matchingNodes1.size() == 1) {
						commentedCode.add(Pair.of(matchingNodes1.iterator().next(), comment));
					}
				}
			}
		}
		// check if unmatched statements from right side have been uncommented
		if(uniqueComments1.size() > 0 && nonMappedLeavesT2.size() > 0) {
			//List<ASTNode>
			for(UMLComment comment : uniqueComments1) {
				String text = comment.getText();
				if(text.startsWith("//")) {
					text = text.substring(2);
				}
				text = text.trim();
				ASTNode node = processBlock(text);
				if(node != null) {
					String nodeAsString = stringify(node);
					Set<AbstractCodeFragment> matchingNodes2 = new LinkedHashSet<AbstractCodeFragment>();
					for(AbstractCodeFragment leaf2 : nonMappedLeavesT2) {
						if(leaf2.getString().equals(nodeAsString)) {
							matchingNodes2.add(leaf2);
						}
					}
					if(matchingNodes2.size() == 1) {
						unCommentedCode.add(Pair.of(comment, matchingNodes2.iterator().next()));
					}
				}
			}
		}
	}

	private boolean existsMappingSubsumingBoth(AbstractCodeFragment nonMappedLeaf1, AbstractCodeFragment nonMappedLeaf2) {
		if((nonMappedLeaf1.getParent() != null && nonMappedLeaf1.getParent().getParent() == null) || (nonMappedLeaf2.getParent() != null && nonMappedLeaf2.getParent().getParent() == null)) {
			return true;
		}
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getFragment1().getLocationInfo().subsumes(nonMappedLeaf1.getLocationInfo()) && mapping.getFragment2().getLocationInfo().subsumes(nonMappedLeaf2.getLocationInfo())) {
				return true;
			}
		}
		return false;
	}

	private String matchesWithOverlappingRenameVariable(AbstractExpression initializer, AbstractCodeFragment nonMappedLeaf2) {
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getFragment2().getLocationInfo().subsumes(nonMappedLeaf2.getLocationInfo())) {
				Set<Replacement> replacements = mapping.getReplacements();
				for(Replacement r : replacements) {
					if(r.getType().equals(ReplacementType.VARIABLE_NAME) && initializer.getString().contains(r.getBefore())) {
						String temp = initializer.getString();
						temp = ReplacementUtil.performReplacement(temp, r.getBefore(), r.getAfter());
						if(nonMappedLeaf2.getString().contains(temp)) {
							return temp;
						}
					}
				}
			}
		}
		return null;
	}

	private void handleAssertThrowsLambda(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			List<CompositeStatementObject> innerNodes2, List<LambdaExpressionObject> lambdas2, UMLOperation operation2) {
		Set<AbstractCodeFragment> lambdaFragments = new LinkedHashSet<AbstractCodeFragment>();
		if(lambdas2.size() == 1) {
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(leaf2.getLambdas().size() > 0) {
					lambdaFragments.add(leaf2);
					break;
				}
			}
		}
		else {
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(leaf2.getLambdas().size() > 0) {
					boolean identicalLeaf1Found = false;
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(leaf1.getString().equals(leaf2.getString())) {
							identicalLeaf1Found = true;
							break;
						}
					}
					if(identicalLeaf1Found) {
						continue;
					}
					lambdaFragments.add(leaf2);
				}
			}
		}
		for(AbstractCodeFragment lambdaFragment : lambdaFragments) {
			expandAnonymousAndLambdas(lambdaFragment, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
		}
	}

	private boolean isTryBlock(AbstractCodeFragment child, CompositeStatementObject parent) {
		return parent.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
				parent.getStatements().indexOf(child) != -1;
	}

	private boolean isFinallyBlock(AbstractCodeFragment child, CompositeStatementObject parent) {
		return parent.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) &&
				parent.getStatements().indexOf(child) != -1;
	}

	private boolean ifAddingElseIf(CompositeStatementObject parent) {
		for(AbstractCodeMapping mapping : ifAddingElseIf) {
			if(mapping.getFragment1().equals(parent) || mapping.getFragment2().equals(parent)) {
				return true;
			}
		}
		return false;
	}

	private boolean ifAddingElseIf(AbstractCodeMapping mapping) {
		if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			boolean hasElseIfBranch1 = hasElseIfBranch((CompositeStatementObject)mapping.getFragment1());
			boolean hasElseIfBranch2 = hasElseIfBranch((CompositeStatementObject)mapping.getFragment2());
			return hasElseIfBranch1 != hasElseIfBranch2;
		}
		return false;
	}

	private boolean ifBecomingElseIf(AbstractCodeMapping mapping) {
		AbstractCodeFragment fragment1 = mapping.getFragment1();
		AbstractCodeFragment fragment2 = mapping.getFragment2();
		return ifBecomingElseIf(fragment1, fragment2);
	}

	private boolean ifBecomingElseIf(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2) {
		if(fragment1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			boolean isElseIf1 = isElseIfBranch(fragment1, fragment1.getParent());
			boolean isElseIf2 = isElseIfBranch(fragment2, fragment2.getParent());
			return isElseIf1 != isElseIf2;
		}
		return false;
	}

	private boolean isInvolvedInSplitConditional(AbstractCodeMapping mapping) {
		for(Refactoring r : this.refactorings) {
			if(r instanceof SplitConditionalRefactoring) {
				SplitConditionalRefactoring split = (SplitConditionalRefactoring)r;
				if(split.getOriginalConditional().equals(mapping.getFragment1()) && split.getSplitConditionals().contains(mapping.getFragment2())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isSplitConditionalExpression(AbstractCodeMapping mapping) {
		for(Refactoring r : this.refactorings) {
			if(r instanceof SplitConditionalRefactoring) {
				SplitConditionalRefactoring split = (SplitConditionalRefactoring)r;
				if(split.getOriginalConditional() instanceof CompositeStatementObject) {
					CompositeStatementObject comp = (CompositeStatementObject)split.getOriginalConditional();
					if(comp.getExpressions().contains(mapping.getFragment1())) {
						return true;
					}
				}
			}
			if(r instanceof MergeConditionalRefactoring) {
				MergeConditionalRefactoring merge = (MergeConditionalRefactoring)r;
				for(AbstractCodeFragment fragment : merge.getMergedConditionals()) {
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject comp = (CompositeStatementObject)fragment;
						if(comp.getExpressions().contains(mapping.getFragment1())) {
							return true;
						}
					}
				}
			}
		}
		for(AbstractCodeMapping previousMapping : this.mappings) {
			for(Refactoring r : previousMapping.getRefactorings()) {
				if(r instanceof ExtractVariableRefactoring) {
					ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
					if(mapping.getFragment2().getVariableDeclarations().contains(extract.getVariableDeclaration())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean nestedUnderSplitConditional(AbstractCodeMapping mapping) {
		for(Refactoring r : this.refactorings) {
			if(r instanceof SplitConditionalRefactoring) {
				SplitConditionalRefactoring split = (SplitConditionalRefactoring)r;
				boolean fragmentSubsumed1 = split.getOriginalConditional().getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo());
				boolean fragmentSubsumed2 = false;
				for(AbstractCodeFragment conditional2 : split.getSplitConditionals()) {
					if(conditional2.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())) {
						fragmentSubsumed2 = true;
						break;
					}
				}
				if(fragmentSubsumed1 && fragmentSubsumed2) {
					return true;
				}
			}
		}
		return false;
	}

	public UMLOperationBodyMapper(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2,
			VariableDeclarationContainer container1, VariableDeclarationContainer container2,
			UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		this.container1 = container1;
		this.container2 = container2;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		if(fragment1 != null && fragment2 != null) {
			List<AbstractCodeFragment> leaves1 = new ArrayList<AbstractCodeFragment>();
			leaves1.add(fragment1);
			List<AbstractCodeFragment> leaves2 = new ArrayList<AbstractCodeFragment>();
			leaves2.add(fragment2);
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		}
	}

	public UMLOperationBodyMapper(UMLAttribute removedAttribute, UMLAttribute addedAttribute, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		this.container1 = removedAttribute;
		this.container2 = addedAttribute;
		AbstractExpression expression1 = removedAttribute.getVariableDeclaration().getInitializer();
		AbstractExpression expression2 = addedAttribute.getVariableDeclaration().getInitializer();
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		if(expression1 != null && expression2 != null) {
			List<AbstractExpression> leaves1 = new ArrayList<AbstractExpression>();
			leaves1.add(expression1);
			List<AbstractExpression> leaves2 = new ArrayList<AbstractExpression>();
			leaves2.add(expression2);
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		}
	}

	public UMLOperationBodyMapper(UMLInitializer initializer1, UMLInitializer initializer2, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = initializer1;
		this.container2 = initializer2;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		CompositeStatementObject composite1 = initializer1.getBody().getCompositeStatement();
		CompositeStatementObject composite2 = initializer2.getBody().getCompositeStatement();
		processCompositeStatements(composite1.getLeaves(), composite2.getLeaves(), composite1.getInnerNodes(), composite2.getInnerNodes());
		if(initializer1.getJavadoc() != null && initializer2.getJavadoc() != null) {
			UMLJavadocDiff diff = new UMLJavadocDiff(initializer1.getJavadoc(), initializer2.getJavadoc());
			this.javadocDiff = Optional.of(diff);
		}
		this.commentListDiff = new UMLCommentListDiff(initializer1.getComments(), initializer2.getComments(), this);
		checkUnmatchedStatementsBeingCommented();
	}

	protected UMLOperationBodyMapper(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2, UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
		this.parentMapper = parentMapper;
		this.classDiff = parentMapper.classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = parentMapper.container1;
		this.container2 = parentMapper.container2;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		this.operationSignatureDiff =  new UMLOperationDiff(lambda1, lambda2, classDiff);
		
		if(lambda1.getExpression() != null && lambda2.getExpression() != null) {
			List<AbstractExpression> leaves1 = new ArrayList<AbstractExpression>();
			leaves1.add(lambda1.getExpression());
			List<AbstractExpression> leaves2 = new ArrayList<AbstractExpression>();
			leaves2.add(lambda2.getExpression());
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		}
		else if(lambda1.getBody() != null && lambda2.getBody() != null) {
			CompositeStatementObject composite1 = lambda1.getBody().getCompositeStatement();
			CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
			if(composite1.getStatements().size() == 0 && composite2.getStatements().size() == 0) {
				CompositeStatementObjectMapping mapping = createCompositeMapping(composite1, composite2, new LinkedHashMap<String, String>(), 0);
				addMapping(mapping);
			}
			else {
				this.lambdaBodyMapper = true;
				processCompositeStatements(composite1.getLeaves(), composite2.getLeaves(), composite1.getInnerNodes(), composite2.getInnerNodes());
			}
		}
		else if(lambda1.getExpression() != null && lambda2.getBody() != null) {
			List<AbstractCodeFragment> leaves1 = new ArrayList<AbstractCodeFragment>();
			leaves1.add(lambda1.getExpression());
			CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
			this.lambdaBodyMapper = true;
			processCompositeStatements(leaves1, composite2.getLeaves(), Collections.emptyList(), composite2.getInnerNodes());
		}
		else if(lambda1.getBody() != null && lambda2.getExpression() != null) {
			CompositeStatementObject composite1 = lambda1.getBody().getCompositeStatement();
			List<AbstractCodeFragment> leaves2 = new ArrayList<AbstractCodeFragment>();
			leaves2.add(lambda2.getExpression());
			this.lambdaBodyMapper = true;
			processCompositeStatements(composite1.getLeaves(), leaves2, composite1.getInnerNodes(), Collections.emptyList());
		}
		this.commentListDiff = new UMLCommentListDiff(lambda1.getComments(), lambda2.getComments(), this);
		checkUnmatchedStatementsBeingCommented();
	}

	private void processCompositeStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2, List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2)
			throws RefactoringMinerTimedOutException {
		Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
		Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
		if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
			for(AbstractCodeFragment streamAPICall : streamAPIStatements2) {
				if(streamAPICall.getLambdas().size() > 0) {
					expandAnonymousAndLambdas(streamAPICall, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, container2, false);
				}
			}
		}
		else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
			for(AbstractCodeFragment streamAPICall : streamAPIStatements1) {
				if(streamAPICall.getLambdas().size() > 0) {
					expandAnonymousAndLambdas(streamAPICall, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, container1, false);
				}
			}
		}
		boolean isomorphic = isomorphicCompositeStructure(innerNodes1, innerNodes2);
		int anonymousMappers = this.anonymousClassDiffs.size();
		processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), isomorphic);
		if(this.anonymousClassDiffs.size() > anonymousMappers && parentMapper != null) {
			parentMapper.anonymousClassDiffs.addAll(this.anonymousClassDiffs);
		}
		
		processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), containsCallToExtractedMethod(leaves2));
		
		if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
			processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
		}
		else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
			processStreamAPIStatements(leaves1, leaves2, streamAPIStatements1, innerNodes2);
		}
		
		updateNonMappedLeavesT1(leaves1);
		updateNonMappedLeavesT2(leaves2);
		nonMappedInnerNodesT1.addAll(innerNodes1);
		nonMappedInnerNodesT2.addAll(innerNodes2);
		
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			temporaryVariableAssignment(statement, nonMappedLeavesT2);
		}
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			inlinedVariableAssignment(statement, nonMappedLeavesT2);
		}
	}

	private List<UMLAnonymousClass> anonymousClassList1() {
		return container1.getAnonymousClassList();
	}

	private List<UMLAnonymousClass> anonymousClassList2() {
		return container2.getAnonymousClassList();
	}

	private AbstractCodeFragment containLambdaExpression(List<AbstractCodeFragment> compositeLeaves, AbstractCodeFragment lambdaExpression) {
		for(AbstractCodeFragment leaf : compositeLeaves) {
			for(LambdaExpressionObject lambda : leaf.getLambdas()) {
				if(lambda.getExpression() != null && lambda.getExpression().equals(lambdaExpression)) {
					return leaf;
				}
			}
		}
		return null;
	}

	private void collectNestedLambdaExpressions(LambdaExpressionObject parentLambda, List<LambdaExpressionObject> nestedLambdas) {
		if(parentLambda.getExpression() != null) {
			nestedLambdas.addAll(parentLambda.getExpression().getLambdas());
			for(LambdaExpressionObject nestedLambda : parentLambda.getExpression().getLambdas()) {
				collectNestedLambdaExpressions(nestedLambda, nestedLambdas);
			}
		}
		if(parentLambda.getBody() != null) {
			CompositeStatementObject comp = parentLambda.getBody().getCompositeStatement();
			for(AbstractCodeFragment fragment : comp.getLeaves()) {
				nestedLambdas.addAll(fragment.getLambdas());
				for(LambdaExpressionObject nestedLambda : fragment.getLambdas()) {
					collectNestedLambdaExpressions(nestedLambda, nestedLambdas);
				}
			}
		}
	}

	private boolean nestedLambdaExpressionMatch(List<LambdaExpressionObject> lambdas, AbstractCodeFragment lambdaExpression) {
		for(LambdaExpressionObject lambda : lambdas) {
			if(lambda.getExpression() != null) {
				if(lambda.getExpression().equals(lambdaExpression)) {
					return true;
				}
				else if(nestedLambdaExpressionMatch(lambda.getExpression().getLambdas(), lambdaExpression)) {
					return true;
				}
			}
		}
		return false;
	}

	private List<VariableDeclaration> nestedLambdaParameters(List<LambdaExpressionObject> lambdas) {
		List<VariableDeclaration> lambdaParameters = new ArrayList<>();
		for(LambdaExpressionObject lambda : lambdas) {
			lambdaParameters.addAll(lambda.getParameters());
			if(lambda.getExpression() != null) {
				lambdaParameters.addAll(nestedLambdaParameters(lambda.getExpression().getLambdas()));
			}
		}
		return lambdaParameters;
	}

	private void processStreamAPIStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			Set<AbstractCodeFragment> streamAPIStatements1, List<CompositeStatementObject> innerNodes2)
			throws RefactoringMinerTimedOutException {
		//match expressions in inner nodes from T2 with leaves from T1
		List<AbstractExpression> expressionsT2 = new ArrayList<AbstractExpression>();
		for(CompositeStatementObject composite : innerNodes2) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT2.add(expression);
				}
			}
		}
		int numberOfMappings = mappings.size();
		processLeaves(leaves1, expressionsT2, new LinkedHashMap<String, String>(), false);
		
		List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
		if(numberOfMappings == mappings.size()) {
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator2.next();
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
				List<AbstractCodeFragment> compositeLeaves = composite.getLeaves();
				for(AbstractCodeMapping mapping : mappings) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					if((composite.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) &&
							(compositeLeaves.contains(fragment2) || containLambdaExpression(compositeLeaves, fragment2) != null)) {
						AbstractCodeFragment streamAPICallStatement = null;
						List<AbstractCall> streamAPICalls = null;
						for(AbstractCodeFragment leaf1 : streamAPIStatements1) {
							if(leaves1.contains(leaf1)) {
								boolean matchingLambda = nestedLambdaExpressionMatch(leaf1.getLambdas(), fragment1);
								AbstractCall call = leaf1.invocationCoveringEntireFragment();
								boolean findCall = call != null && (call.getName().equals("findFirst") || call.getName().equals("findAny"));
								if(matchingLambda || findCall) {
									streamAPICallStatement = leaf1;
									streamAPICalls = streamAPICalls(leaf1);
									break;
								}
							}
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
							List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
							AbstractCall call1 = fragment1.invocationCoveringEntireFragment();
							AbstractCall call2 = fragment2.invocationCoveringEntireFragment();
							if(call1 != null && call2 != null) {
								LeafMapping leafMapping = new LeafMapping(call1, call2, container1, container2);
								leafMappings.add(leafMapping);
							}
							else {
								call1 = fragment1.creationCoveringEntireFragment();
								call2 = fragment2.creationCoveringEntireFragment();
								if(call1 != null && call2 != null) {
									LeafMapping leafMapping = new LeafMapping(call1, call2, container1, container2);
									leafMappings.add(leafMapping);
								}
								else if(fragment1 instanceof AbstractExpression) {
									List<LeafExpression> leafExpressions = fragment2.findExpression(fragment1.getString());
									for(LeafExpression leafExpression : leafExpressions) {
										LeafMapping leafMapping = new LeafMapping(fragment1, leafExpression, container1, container2);
										leafMappings.add(leafMapping);
									}
								}
							}
							additionallyMatchedStatements1.add(streamAPICallStatement);
							additionallyMatchedStatements2.add(fragment2);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(isForEach(streamAPICall.getName())) {
									if(!additionallyMatchedStatements2.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(leafExpression, expression, container1, container2);
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements2.add(composite);
												break;
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("stream")) {
									if(!additionallyMatchedStatements2.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(leafExpression, expression, container1, container2);
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements2.add(composite);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(argument);
													for(LeafExpression leafExpression : leafExpressions) {
														LeafMapping leafMapping = new LeafMapping(leafExpression, expression, container1, container2);
														leafMappings.add(leafMapping);
													}
													additionallyMatchedStatements2.add(composite);
													break;
												}
											}
										}
									}
								}
							}
							CompositeReplacement replacement = new CompositeReplacement(streamAPICallStatement.getString(), composite.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							Set<Replacement> replacements = new LinkedHashSet<>();
							replacements.add(replacement);
							LeafMapping newMapping = createLeafMapping(streamAPICallStatement, composite, new LinkedHashMap<String, String>(), false);
							newMapping.addReplacements(replacements);
							TreeSet<LeafMapping> mappingSet = new TreeSet<>();
							mappingSet.add(newMapping);
							if(!additionallyMatchedStatements2.contains(composite)) {
								additionallyMatchedStatements2.add(composite);
							}
							for(VariableDeclaration lambdaParameter : lambdaParameters) {
								for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
									if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
										Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
										matchedVariables.add(pair);
									}
									else {
										for(Replacement r : mapping.getReplacements()) {
											if(r.getBefore().equals(lambdaParameter.getVariableName()) && r.getAfter().equals(compositeParameter.getVariableName())) {
												Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
												matchedVariables.add(pair);
												break;
											}
										}
									}
								}
							}
							ReplacePipelineWithLoopRefactoring ref = new ReplacePipelineWithLoopRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, container1, container2);
							for(LeafMapping leafMapping : leafMappings) {
								ref.addSubExpressionMapping(leafMapping);
							}
							for(AbstractCodeMapping m : this.mappings) {
								if(composite.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment1().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
							newMapping.addRefactoring(ref);
							addToMappings(newMapping, mappingSet);
							leaves1.remove(newMapping.getFragment1());
						}
					}
				}
				if(additionallyMatchedStatements2.contains(composite)) {
					innerNodeIterator2.remove();
				}
			}
		}
		for(int i = numberOfMappings; i < mappings.size(); i++) {
			AbstractCodeMapping mapping = mappings.get(i);
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator2.next();
				if(composite.getExpressions().contains(fragment2)) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf1 : streamAPIStatements1) {
						if(leaves1.contains(leaf1)) {
							boolean matchingLambda = nestedLambdaExpressionMatch(leaf1.getLambdas(), fragment1);
							if(matchingLambda) {
								streamAPICallStatement = leaf1;
								streamAPICalls = streamAPICalls(leaf1);
								break;
							}
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(streamAPICallStatement);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(composite);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(streamAPICall.getName().equals("filter")) {
								for(AbstractCodeFragment leaf2 : leaves2) {
									AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getName().equals("add")) {
										for(String argument : invocation.arguments()) {
											if(streamAPICall.arguments().get(0).startsWith(argument + JAVA.LAMBDA_ARROW)) {
												additionallyMatchedStatements2.add(leaf2);
												break;
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("removeIf")) {
								for(AbstractCodeFragment leaf2 : leaves2) {
									AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getExpression() != null) {
										if(invocation.getName().equals("next")) {
											for(VariableDeclaration variableDeclaration : leaf2.getVariableDeclarations()) {
												if(streamAPICall.arguments().get(0).startsWith(variableDeclaration.getVariableName() + JAVA.LAMBDA_ARROW)) {
													additionallyMatchedStatements2.add(leaf2);
													break;
												}
											}
										}
										else if(invocation.getName().equals("remove")) {
											additionallyMatchedStatements2.add(leaf2);
											for(ListIterator<CompositeStatementObject> it = innerNodes2.listIterator(); it.hasNext();) {
												CompositeStatementObject comp = it.next();
												if(comp.getVariableDeclaration(invocation.getExpression()) != null) {
													additionallyMatchedStatements2.add(comp);
													composite = comp;
													break;
												}
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("stream")) {
								for(CompositeStatementObject comp2 : innerNodes2) {
									if(!additionallyMatchedStatements2.contains(comp2)) {
										for(AbstractExpression expression : comp2.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements2.add(comp2);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													additionallyMatchedStatements2.add(comp2);
													break;
												}
											}
										}
									}
								}
							}
							else if(isForEach(streamAPICall.getName())) {
								for(CompositeStatementObject comp2 : innerNodes2) {
									if(!additionallyMatchedStatements2.contains(comp2)) {
										for(AbstractExpression expression : comp2.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements2.add(comp2);
												break;
											}
										}
										if(comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
												comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
												comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
												comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
											List<AbstractCodeFragment> compositeLeaves = comp2.getLeaves();
											for(AbstractCodeMapping m : mappings) {
												if(!m.equals(mapping)) {
													AbstractCodeFragment leaf2 = null;
													if(compositeLeaves.contains(m.getFragment2()) || (leaf2 = containLambdaExpression(compositeLeaves, m.getFragment2())) != null) {
														if(leaf2 != null && composite.getLocationInfo().subsumes(leaf2.getLocationInfo())) {
															additionallyMatchedStatements2.add(comp2);
															additionallyMatchedStatements2.add(leaf2);
															composite = comp2;
															break;
														}
														else if(composite.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
															additionallyMatchedStatements2.add(comp2);
															additionallyMatchedStatements2.add(m.getFragment2());
															composite = comp2;
															break;
														}
													}
												}
											}
										}
									}
								}
							}
						}
						CompositeReplacement replacement = new CompositeReplacement(streamAPICallStatement.getString(), composite.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
						Set<Replacement> replacements = new LinkedHashSet<>();
						replacements.add(replacement);
						LeafMapping newMapping = createLeafMapping(streamAPICallStatement, composite, new LinkedHashMap<String, String>(), false);
						newMapping.addReplacements(replacements);
						TreeSet<LeafMapping> mappingSet = new TreeSet<>();
						mappingSet.add(newMapping);
						for(VariableDeclaration lambdaParameter : lambdaParameters) {
							for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
								if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
									Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
									matchedVariables.add(pair);
								}
								else {
									for(Replacement r : mapping.getReplacements()) {
										if(r.getBefore().equals(lambdaParameter.getVariableName()) && r.getAfter().equals(compositeParameter.getVariableName())) {
											Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
											matchedVariables.add(pair);
											break;
										}
									}
								}
							}
						}
						int count = 0;
						if(parentMapper != null) {
							for(AbstractCodeFragment fragment : additionallyMatchedStatements1) {
								if(parentMapper.alreadyMatched1(fragment)) {
									count++;
								}
							}
						}
						boolean allAdditionalMatchedStatementsInParentMapper = count == additionallyMatchedStatements1.size();
						if(!allAdditionalMatchedStatementsInParentMapper) {
							ReplacePipelineWithLoopRefactoring ref = new ReplacePipelineWithLoopRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, container1, container2);
							newMapping.addRefactoring(ref);
							for(AbstractCodeMapping m : this.mappings) {
								if(composite.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment1().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
						}
						addToMappings(newMapping, mappingSet);
						leaves1.remove(newMapping.getFragment1());
						innerNodeIterator2.remove();
					}
				}
			}
		}
	}

	private void processStreamAPIStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			List<CompositeStatementObject> innerNodes1, Set<AbstractCodeFragment> streamAPIStatements2)
			throws RefactoringMinerTimedOutException {
		Map<VariableDeclarationContainer, List<AbstractCodeFragment>> map = new LinkedHashMap<VariableDeclarationContainer, List<AbstractCodeFragment>>();
		int mapSize = 0;
		streamAPICallsInExtractedMethods(container2, leaves2, streamAPIStatements2, map);
		while(mapSize < map.size()) {
			int i=0;
			int tmpMapSize = map.size();
			for(VariableDeclarationContainer key : new LinkedHashSet<>(map.keySet())) {
				if(i >= mapSize) {
					streamAPICallsInExtractedMethods(key, map.get(key), streamAPIStatements2, map);
				}
				i++;
			}
			mapSize = tmpMapSize;
		}
		List<AbstractCodeFragment> newLeaves2 = new ArrayList<AbstractCodeFragment>();
		for(VariableDeclarationContainer key : map.keySet()) {
			newLeaves2.addAll(map.get(key));
		}
		//match expressions in inner nodes from T1 with leaves from T2
		List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
		for(CompositeStatementObject composite : innerNodes1) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT1.add(expression);
				}
			}
		}
		int numberOfMappings = mappings.size();
		processLeaves(expressionsT1, leaves2, new LinkedHashMap<String, String>(), false);
		boolean onlyNestedMappings = this.mappings.size() == numberOfMappings;
		processLeaves(expressionsT1, newLeaves2, new LinkedHashMap<String, String>(), false);
		
		List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
		if(numberOfMappings == mappings.size()) {
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator1.next();
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
				List<AbstractCodeFragment> compositeLeaves = composite.getLeaves();
				for(AbstractCodeMapping mapping : mappings) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					List<VariableDeclaration> declarations1 = fragment1.getVariableDeclarations();
					boolean matchingDeclaration = false;
					if(declarations1.size() > 0 && composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						for(AbstractExpression expression1 : composite.getExpressions()) {
							if(expression1.getString().equals(declarations1.get(0).getVariableName())) {
								matchingDeclaration = true;
								break;
							}
						}
					}
					if((composite.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) &&
							(compositeLeaves.contains(fragment1) || containLambdaExpression(compositeLeaves, fragment1) != null) || matchingDeclaration) {
						AbstractCodeFragment streamAPICallStatement = null;
						List<AbstractCall> streamAPICalls = null;
						for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
							if(leaves2.contains(leaf2)) {
								boolean matchingLambda = nestedLambdaExpressionMatch(leaf2.getLambdas(), fragment2);
								if(matchingLambda) {
									streamAPICallStatement = leaf2;
									streamAPICalls = streamAPICalls(leaf2);
									break;
								}
							}
							else {
								for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
									if(lambda.getBody() != null) {
										for(AbstractCodeFragment leaf : lambda.getBody().getCompositeStatement().getLeaves()) {
											boolean matchingLambda = nestedLambdaExpressionMatch(leaf.getLambdas(), fragment2);
											if(matchingLambda) {
												streamAPICallStatement = leaf;
												streamAPICalls = streamAPICalls(leaf);
												break;
											}
										}
									}
								}
							}
							List<AbstractCall> tmpStreamAPICalls = streamAPICalls(leaf2);
							if(matchingDeclaration) {
								boolean matchFound = false;
								for(AbstractCall call : tmpStreamAPICalls) {
									if(call.arguments().size() > 0 && call.arguments().get(0).startsWith(composite.getExpressions().get(0).getString() + JAVA.LAMBDA_ARROW)) {
										matchFound = true;
										break;
									}
									else if(call.arguments().size() > 0 && call.arguments().get(0).contains(JAVA.LAMBDA_ARROW)) {
										String name = call.arguments().get(0).substring(0, call.arguments().get(0).indexOf(JAVA.LAMBDA_ARROW));
										if(composite.getExpressions().get(0).getString().toLowerCase().contains(name.toLowerCase())) {
											matchFound = true;
											break;
										}
									}
								}
								if(matchFound) {
									streamAPICallStatement = leaf2;
									streamAPICalls = streamAPICalls(leaf2);
									break;
								}
							}
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
							List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
							AbstractCall call1 = fragment1.invocationCoveringEntireFragment();
							AbstractCall call2 = fragment2.invocationCoveringEntireFragment();
							if(call1 != null && call2 != null) {
								LeafMapping leafMapping = new LeafMapping(call1, call2, container1, container2);
								leafMappings.add(leafMapping);
							}
							else {
								call1 = fragment1.creationCoveringEntireFragment();
								call2 = fragment2.creationCoveringEntireFragment();
								if(call1 != null && call2 != null) {
									LeafMapping leafMapping = new LeafMapping(call1, call2, container1, container2);
									leafMappings.add(leafMapping);
								}
								else if(fragment2 instanceof AbstractExpression) {
									List<LeafExpression> leafExpressions = fragment1.findExpression(fragment2.getString());
									for(LeafExpression leafExpression : leafExpressions) {
										LeafMapping leafMapping = new LeafMapping(leafExpression, fragment2, container1, container2);
										leafMappings.add(leafMapping);
									}
								}
							}
							additionallyMatchedStatements1.add(fragment1);
							additionallyMatchedStatements2.add(streamAPICallStatement);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(isForEach(streamAPICall.getName())) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(expression, leafExpression, container1, container2);
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements1.add(composite);
												break;
											}
										}
									}
									if(matchingDeclaration) {
										List<LambdaExpressionObject> lambdas = streamAPICallStatement.getLambdas();
										for(LambdaExpressionObject lambda : lambdas) {
											if(lambda.getBody() != null) {
												CompositeStatementObject composite2 = lambda.getBody().getCompositeStatement();
												processCompositeStatements(composite.getLeaves(), composite2.getLeaves(), composite.getInnerNodes(), composite2.getInnerNodes());
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("stream")) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(expression, leafExpression, container1, container2);
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements1.add(composite);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(argument);
													for(LeafExpression leafExpression : leafExpressions) {
														LeafMapping leafMapping = new LeafMapping(expression, leafExpression, container1, container2);
														leafMappings.add(leafMapping);
													}
													additionallyMatchedStatements1.add(composite);
													break;
												}
											}
										}
									}
								}
							}
							CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							Set<Replacement> replacements = new LinkedHashSet<>();
							replacements.add(replacement);
							LeafMapping newMapping = createLeafMapping(composite, streamAPICallStatement, new LinkedHashMap<String, String>(), false);
							newMapping.addReplacements(replacements);
							TreeSet<LeafMapping> mappingSet = new TreeSet<>();
							mappingSet.add(newMapping);
							if(!additionallyMatchedStatements1.contains(composite)) {
								additionallyMatchedStatements1.add(composite);
							}
							for(VariableDeclaration lambdaParameter : lambdaParameters) {
								for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
									if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
										Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
										matchedVariables.add(pair);
									}
									else {
										for(Replacement r : mapping.getReplacements()) {
											if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
												Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
												matchedVariables.add(pair);
												break;
											}
										}
									}
								}
							}
							ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, container1, container2);
							for(LeafMapping leafMapping : leafMappings) {
								ref.addSubExpressionMapping(leafMapping);
							}
							for(AbstractCodeMapping m : this.mappings) {
								if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
							newMapping.addRefactoring(ref);
							addToMappings(newMapping, mappingSet);
							leaves2.remove(newMapping.getFragment2());
						}
					}
				}
				if(additionallyMatchedStatements1.contains(composite)) {
					innerNodeIterator1.remove();
				}
			}
		}
		for(int i = numberOfMappings; i < mappings.size(); i++) {
			AbstractCodeMapping mapping = mappings.get(i);
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator1.next();
				if((composite.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
						composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
						composite.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
						composite.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) &&
						composite.getLocationInfo().subsumes(fragment1.getLocationInfo()) &&
						onlyNestedMappings) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
						if(leaves2.contains(leaf2)) {
							streamAPICallStatement = leaf2;
							streamAPICalls = streamAPICalls(leaf2);
							break;
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(composite);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(streamAPICallStatement);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(isForEach(streamAPICall.getName())) {
								CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
								Set<Replacement> replacements = new LinkedHashSet<>();
								replacements.add(replacement);
								LeafMapping newMapping = createLeafMapping(composite, streamAPICallStatement, new LinkedHashMap<String, String>(), false);
								newMapping.addReplacements(replacements);
								TreeSet<LeafMapping> mappingSet = new TreeSet<>();
								mappingSet.add(newMapping);
								for(VariableDeclaration lambdaParameter : lambdaParameters) {
									for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
										if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
											Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
											matchedVariables.add(pair);
										}
										else {
											for(Replacement r : mapping.getReplacements()) {
												if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
													Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
													matchedVariables.add(pair);
													break;
												}
											}
										}
									}
								}
								ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, container1, container2);
								newMapping.addRefactoring(ref);
								for(AbstractCodeMapping m : this.mappings) {
									if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
										ref.addNestedStatementMapping(m);
									}
								}
								addToMappings(newMapping, mappingSet);
								leaves2.remove(newMapping.getFragment2());
								innerNodeIterator1.remove();
							}
						}
					}
				}
				else if(composite.getExpressions().contains(fragment1)) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
						if(leaves2.contains(leaf2)) {
							boolean matchingLambda = nestedLambdaExpressionMatch(leaf2.getLambdas(), fragment2);
							if(matchingLambda) {
								streamAPICallStatement = leaf2;
								streamAPICalls = streamAPICalls(leaf2);
								break;
							}
						}
						else if(fragment2.equals(leaf2)) {
							streamAPICallStatement = leaf2;
							streamAPICalls = streamAPICalls(leaf2);
							break;
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(composite);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(streamAPICallStatement);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(streamAPICall.getName().equals("filter")) {
								for(AbstractCodeFragment leaf1 : leaves1) {
									AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getName().equals("add")) {
										for(String argument : invocation.arguments()) {
											if(streamAPICall.arguments().get(0).startsWith(argument + JAVA.LAMBDA_ARROW)) {
												additionallyMatchedStatements1.add(leaf1);
												break;
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("removeIf")) {
								for(AbstractCodeFragment leaf1 : leaves1) {
									AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getExpression() != null) {
										if(invocation.getName().equals("next")) {
											for(VariableDeclaration variableDeclaration : leaf1.getVariableDeclarations()) {
												if(streamAPICall.arguments().get(0).startsWith(variableDeclaration.getVariableName() + JAVA.LAMBDA_ARROW)) {
													additionallyMatchedStatements1.add(leaf1);
													break;
												}
											}
										}
										else if(invocation.getName().equals("remove")) {
											additionallyMatchedStatements1.add(leaf1);
											for(ListIterator<CompositeStatementObject> it = innerNodes1.listIterator(); it.hasNext();) {
												CompositeStatementObject comp = it.next();
												if(comp.getVariableDeclaration(invocation.getExpression()) != null) {
													additionallyMatchedStatements1.add(comp);
													composite = comp;
													break;
												}
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("stream")) {
								for(CompositeStatementObject comp1 : innerNodes1) {
									if(!additionallyMatchedStatements1.contains(comp1)) {
										for(AbstractExpression expression : comp1.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(comp1);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													additionallyMatchedStatements1.add(comp1);
													break;
												}
											}
										}
									}
								}
							}
							else if(isForEach(streamAPICall.getName())) {
								for(CompositeStatementObject comp1 : innerNodes1) {
									if(!additionallyMatchedStatements1.contains(comp1)) {
										for(AbstractExpression expression : comp1.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(comp1);
												break;
											}
										}
										if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
												comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
												comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
												comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
											List<AbstractCodeFragment> compositeLeaves = comp1.getLeaves();
											for(AbstractCodeMapping m : mappings) {
												if(!m.equals(mapping)) {
													AbstractCodeFragment leaf1 = null;
													if(compositeLeaves.contains(m.getFragment1()) || (leaf1 = containLambdaExpression(compositeLeaves, m.getFragment1())) != null) {
														if(leaf1 != null && composite.getLocationInfo().subsumes(leaf1.getLocationInfo())) {
															additionallyMatchedStatements1.add(comp1);
															additionallyMatchedStatements1.add(leaf1);
															composite = comp1;
															break;
														}
														else if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo())) {
															additionallyMatchedStatements1.add(comp1);
															additionallyMatchedStatements1.add(m.getFragment1());
															composite = comp1;
															break;
														}
													}
												}
											}
										}
									}
								}
							}
						}
						CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
						Set<Replacement> replacements = new LinkedHashSet<>();
						replacements.add(replacement);
						for(VariableDeclaration lambdaParameter : lambdaParameters) {
							for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
								if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
									Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
									matchedVariables.add(pair);
								}
								else {
									for(Replacement r : mapping.getReplacements()) {
										if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
											Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
											matchedVariables.add(pair);
											break;
										}
									}
								}
							}
						}
						AbstractCodeFragment loop = null;
						for(AbstractCodeFragment fragment : additionallyMatchedStatements1) {
							if(fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
									fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
									fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
									fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
								loop = fragment;
								break;
							}
						}
						LeafMapping newMapping = createLeafMapping(loop != null ? loop : composite, streamAPICallStatement, new LinkedHashMap<String, String>(), false);
						newMapping.addReplacements(replacements);
						TreeSet<LeafMapping> mappingSet = new TreeSet<>();
						mappingSet.add(newMapping);
						LeafMapping newMapping2 = null;
						if(loop != null) {
							ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, container1, container2);
							newMapping.addRefactoring(ref);
							for(AbstractCodeMapping m : this.mappings) {
								if(loop.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
							if(!loop.equals(composite) && (composite.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
									composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
									composite.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
									composite.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT))) {
								for(AbstractCodeFragment streamAPIStatement : streamAPIStatements2) {
									if(!streamAPIStatement.equals(streamAPICallStatement)) {
										if(composite.getExpressions().size() > 0 && streamAPIStatement.getString().startsWith(composite.getExpressions().get(0).getString())) {
											List<VariableDeclaration> lambdaParameters2 = nestedLambdaParameters(streamAPIStatement.getLambdas());
											for(VariableDeclaration lambdaParameter : lambdaParameters2) {
												for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
													if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
														Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
														matchedVariables.add(pair);
													}
													else {
														for(Replacement r : mapping.getReplacements()) {
															if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
																Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
																matchedVariables.add(pair);
																break;
															}
														}
													}
												}
											}
											newMapping2 = createLeafMapping(composite, streamAPIStatement, new LinkedHashMap<String, String>(), false);
											Set<AbstractCodeFragment> a1 = new LinkedHashSet<>(additionallyMatchedStatements1);
											a1.remove(loop);
											a1.add(composite);
											Set<AbstractCodeFragment> a2 = new LinkedHashSet<>(additionallyMatchedStatements2);
											a2.remove(streamAPICallStatement);
											a2.add(streamAPIStatement);
											ReplaceLoopWithPipelineRefactoring ref2 = new ReplaceLoopWithPipelineRefactoring(a1, a2, container1, container2);
											newMapping2.addRefactoring(ref2);
											for(AbstractCodeMapping m : this.mappings) {
												if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPIStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
													ref2.addNestedStatementMapping(m);
												}
											}
										}
									}
								}
							}
						}
						addToMappings(newMapping, mappingSet);
						leaves2.remove(newMapping.getFragment2());
						innerNodeIterator1.remove();
						if(newMapping2 != null) {
							mappingSet.add(newMapping2);
							addToMappings(newMapping2, mappingSet);
							leaves2.remove(newMapping2.getFragment2());
						}
					}
				}
			}
		}
	}

	private void streamAPICallsInExtractedMethods(VariableDeclarationContainer callerOperation, List<AbstractCodeFragment> leaves2, Set<AbstractCodeFragment> streamAPIStatements2, Map<VariableDeclarationContainer, List<AbstractCodeFragment>> map) {
		if(classDiff != null) {
			for(AbstractCodeFragment leaf2 : leaves2) {
				List<AbstractCall> calls = leaf2.getMethodInvocations();
				for(AbstractCall call : calls) {
					UMLOperation addedOperation = classDiff.matchesOperation(call, classDiff.getAddedOperations(), callerOperation);
					if(addedOperation != null && !map.keySet().contains(addedOperation)) {
						List<AbstractCodeFragment> newLeaves2 = new ArrayList<AbstractCodeFragment>();
						if(!addedOperation.hasEmptyBody() && addedOperation.getBody() != null) {
							Set<AbstractCodeFragment> newStreamAPIStatements2 = statementsWithStreamAPICalls(addedOperation.getBody().getCompositeStatement().getLeaves());
							for(AbstractCodeFragment streamAPICall : newStreamAPIStatements2) {
								if(streamAPICall.getLambdas().size() > 0) {
									streamAPIStatements2.add(streamAPICall);
									expandAnonymousAndLambdas(streamAPICall, newLeaves2, new ArrayList<CompositeStatementObject>(), new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, container2, false);
								}
							}
						}
						map.put(addedOperation, newLeaves2);
					}
				}
			}
		}
	}

	protected UMLOperationBodyMapper(UMLOperation anonymousClassOperation, LambdaExpressionObject lambda2, UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
		this.classDiff = parentMapper.classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.container1 = parentMapper.container1;
		this.container2 = parentMapper.container2;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		if(anonymousClassOperation.getBody() != null) {
			CompositeStatementObject composite1 = anonymousClassOperation.getBody().getCompositeStatement();
			if(lambda2.getBody() != null) {
				CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
				processCompositeStatements(composite1.getLeaves(), composite2.getLeaves(), composite1.getInnerNodes(), composite2.getInnerNodes());
			}
			else if(lambda2.getExpression() != null) {
				List<AbstractCodeFragment> leaves2 = new ArrayList<AbstractCodeFragment>();
				leaves2.add(lambda2.getExpression());
				processCompositeStatements(composite1.getLeaves(), leaves2, composite1.getInnerNodes(), new ArrayList<CompositeStatementObject>());
			}
		}
		this.commentListDiff = new UMLCommentListDiff(anonymousClassOperation.getComments(), lambda2.getComments(), this);
		checkUnmatchedStatementsBeingCommented();
	}

	public void addChildMapper(UMLOperationBodyMapper mapper) throws RefactoringMinerTimedOutException {
		this.childMappers.add(mapper);
		//check for variable extracted in parentMapper, but referenced in childMapper
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			for(AbstractCodeMapping mapping : mapper.getMappings()) {
				int refactoringCount = mapping.getRefactorings().size();
				mapping.temporaryVariableAssignment(statement, nonMappedLeavesT2, classDiff, true, mappings);
				if(refactoringCount < mapping.getRefactorings().size()) {
					for(Refactoring newRefactoring : mapping.getRefactorings()) {
						if(newRefactoring instanceof ExtractVariableRefactoring) {
							ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
							newExtractVariableRefactoring.updateOperationAfter(mapper.getParentMapper().getContainer2());
						}
						if(!this.refactorings.contains(newRefactoring)) {
							this.refactorings.add(newRefactoring);
						}
						else {
							for(Refactoring refactoring : this.refactorings) {
								if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractVariableRefactoring) {
									ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
									Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
									Set<AbstractCodeFragment> newUnmatchedStatementReferences = newExtractVariableRefactoring.getUnmatchedStatementReferences();
									ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
									oldExtractVariableRefactoring.addReferences(newReferences);
									oldExtractVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
									for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
										oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
									}
									break;
								}
								if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
									InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
									Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
									Set<AbstractCodeFragment> newUnmatchedStatementReferences = newInlineVariableRefactoring.getUnmatchedStatementReferences();
									InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
									oldInlineVariableRefactoring.addReferences(newReferences);
									oldInlineVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
									for(LeafMapping newLeafMapping : newInlineVariableRefactoring.getSubExpressionMappings()) {
										oldInlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
									}
									break;
								}
							}
						}
					}
				}
			}
		}
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			for(AbstractCodeMapping mapping : mapper.getMappings()) {
				int refactoringCount = mapping.getRefactorings().size();
				mapping.inlinedVariableAssignment(statement, nonMappedLeavesT1, classDiff, true);
				if(refactoringCount < mapping.getRefactorings().size()) {
					for(Refactoring newRefactoring : mapping.getRefactorings()) {
						if(newRefactoring instanceof ExtractVariableRefactoring) {
							ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
							newExtractVariableRefactoring.updateOperationAfter(mapper.getParentMapper().getContainer2());
						}
						if(!this.refactorings.contains(newRefactoring)) {
							this.refactorings.add(newRefactoring);
						}
						else {
							for(Refactoring refactoring : this.refactorings) {
								if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractVariableRefactoring) {
									ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
									Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
									Set<AbstractCodeFragment> newUnmatchedStatementReferences = newExtractVariableRefactoring.getUnmatchedStatementReferences();
									ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
									oldExtractVariableRefactoring.addReferences(newReferences);
									oldExtractVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
									for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
										oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
									}
									break;
								}
								if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
									InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
									Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
									Set<AbstractCodeFragment> newUnmatchedStatementReferences = newInlineVariableRefactoring.getUnmatchedStatementReferences();
									InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
									oldInlineVariableRefactoring.addReferences(newReferences);
									oldInlineVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
									for(LeafMapping newLeafMapping : newInlineVariableRefactoring.getSubExpressionMappings()) {
										oldInlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
									}
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	public UMLAbstractClassDiff getClassDiff() {
		return classDiff;
	}

	public UMLModelDiff getModelDiff() {
		return modelDiff;
	}

	public Optional<UMLOperationDiff> getOperationSignatureDiff() {
		return Optional.ofNullable(operationSignatureDiff);
	}

	public Optional<Map<String, String>> getParameterToArgumentMap1() {
		return Optional.ofNullable(parameterToArgumentMap1);
	}

	public Optional<Map<String, String>> getParameterToArgumentMap2() {
		return Optional.ofNullable(parameterToArgumentMap2);
	}

	public Set<UMLOperationBodyMapper> getLambdaMappers() {
		Set<UMLOperationBodyMapper> lambdaMappers = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : mappings) {
			lambdaMappers.addAll(mapping.getLambdaMappers());
		}
		return lambdaMappers;
	}

	public Set<UMLOperationBodyMapper> getChildMappers() {
		return childMappers;
	}

	public UMLOperationBodyMapper getParentMapper() {
		return parentMapper;
	}

	public VariableDeclarationContainer getCallSiteOperation() {
		return callSiteOperation;
	}

	public int getCallsToExtractedMethod() {
		return callsToExtractedMethod;
	}

	public AbstractCall getOperationInvocation() {
		return operationInvocation;
	}

	public Map<UMLOperation, Set<AbstractCodeFragment>> getExtractedStatements() {
		return extractedStatements;
	}

	public void updateJavadocDiff(UMLJavadocDiff diff) {
		javadocDiff = Optional.of(diff);
	}

	public Optional<UMLJavadocDiff> getJavadocDiff() {
		return javadocDiff;
	}

	public UMLCommentListDiff getCommentListDiff() {
		return commentListDiff;
	}

	public Set<Pair<AbstractCodeFragment, UMLComment>> getCommentedCode() {
		return commentedCode;
	}

	public Set<Pair<UMLComment, AbstractCodeFragment>> getUnCommentedCode() {
		return unCommentedCode;
	}

	private void resetNodes(List<? extends AbstractCodeFragment> nodes) {
		for(AbstractCodeFragment node : nodes) {
			node.resetArgumentization();
		}
	}
	
	private boolean returnWithVariableReplacement(AbstractCodeMapping mapping) {
		if(mapping.getReplacements().size() == 1) {
			Replacement r = mapping.getReplacements().iterator().next();
			if(r.getType().equals(ReplacementType.VARIABLE_NAME)) {
				String fragment1 = mapping.getFragment1().getString();
				String fragment2 = mapping.getFragment2().getString();
				if(fragment1.equals(JAVA.RETURN_SPACE + r.getBefore() + JAVA.STATEMENT_TERMINATION) && fragment2.equals(JAVA.RETURN_SPACE + r.getAfter() + JAVA.STATEMENT_TERMINATION)) {
					return true;
				}
			}
		}
		return false;
	}

	public UMLOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper, UMLOperation addedOperation,
			Map<String, String> parameterToArgumentMap1, Map<String, String> parameterToArgumentMap2, UMLAbstractClassDiff classDiff, AbstractCall operationInvocation, boolean nested, Optional<List<AbstractCodeFragment>> leaves1Sublist) throws RefactoringMinerTimedOutException {
		this.parentMapper = operationBodyMapper;
		this.operationInvocation = operationInvocation;
		this.nested = nested;
		this.parameterToArgumentMap1 = parameterToArgumentMap1;
		this.parameterToArgumentMap2 = parameterToArgumentMap2;
		this.container1 = operationBodyMapper.container1;
		this.callSiteOperation = operationBodyMapper.container2;
		this.container2 = addedOperation;
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		
		OperationBody addedOperationBody = addedOperation.getBody();
		if(addedOperationBody != null) {
			List<AbstractCodeFragment> leavesT2 = new ArrayList<AbstractCodeFragment>();
			List<AbstractExpression> expressionsT2 = new ArrayList<AbstractExpression>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject composite = (CompositeStatementObject)fragment;
						for(AbstractExpression expression : composite.getExpressions()) {
							if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME) &&
									mapping.getFragment1().getVariableDeclarations().toString().equals(mapping.getFragment2().getVariableDeclarations().toString())) {
								continue;
							}
							expressionsT2.add(expression);
						}
					}
					else {
						if(!leavesT2.contains(fragment))
							leavesT2.add(fragment);
					}
				}
			}
			leavesT2.addAll(operationBodyMapper.getNonMappedLeavesT2());
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					if(!expressionsT2.contains(expression))
						expressionsT2.add(expression);
				}
			}
			this.callsToExtractedMethod = callsToExtractedMethod(leavesT2);
			this.callsToExtractedMethod += callsToExtractedMethod(expressionsT2);
			if(operationBodyMapper.extractedStatements.containsKey(addedOperation)) {
				Set<AbstractCodeFragment> fragments = operationBodyMapper.extractedStatements.get(addedOperation);
				List<AbstractCodeFragment> leaves1 = new ArrayList<AbstractCodeFragment>();
				List<CompositeStatementObject> innerNodes1 = new ArrayList<CompositeStatementObject>();
				for(AbstractCodeFragment fragment : fragments) {
					if(fragment instanceof CompositeStatementObject) {
						innerNodes1.add((CompositeStatementObject) fragment);
					}
					else {
						leaves1.add(fragment);
					}
					expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
				}
				CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
				List<AbstractCodeFragment> leaves2 = composite2.getLeaves();
				List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
				for(AbstractCodeFragment statement : new ArrayList<>(leaves2)) {
					expandAnonymousAndLambdas(statement, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, container2, false);
				}
				innerNodes2.remove(composite2);
				resetNodes(leaves1);
				//replace parameters with arguments in leaves1
				if(!parameterToArgumentMap1.isEmpty()) {
					for(AbstractCodeFragment leave1 : leaves1) {
						leave1.replaceParametersWithArguments(parameterToArgumentMap1);
					}
				}
				resetNodes(leaves2);
				//replace parameters with arguments in leaves2
				if(!parameterToArgumentMap2.isEmpty()) {
					for(AbstractCodeFragment leave2 : leaves2) {
						leave2.replaceParametersWithArguments(parameterToArgumentMap2);
					}
				}
				resetNodes(innerNodes1);
				//replace parameters with arguments in innerNodes1
				if(!parameterToArgumentMap1.isEmpty()) {
					for(CompositeStatementObject innerNode1 : innerNodes1) {
						innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
					}
				}
				resetNodes(innerNodes2);
				//replace parameters with arguments in innerNode2
				if(!parameterToArgumentMap2.isEmpty()) {
					for(CompositeStatementObject innerNode2 : innerNodes2) {
						innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
					}
				}
				//compare leaves from T1 with leaves from T2
				processLeaves(leaves1, leaves2, parameterToArgumentMap2, false);
				
				//compare inner nodes from T1 with inner nodes from T2
				processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, parameterToArgumentMap2, false);
				updateNonMappedLeavesT1(leaves1);
				updateNonMappedLeavesT2(leaves2);
				nonMappedInnerNodesT1.addAll(innerNodes1);
				nonMappedInnerNodesT2.addAll(innerNodes2);
				processCommentsAndJavadoc(addedOperation);
				return;
			}
			CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
			List<AbstractCodeFragment> leaves1 = leaves1Sublist.isPresent() ? leaves1Sublist.get() : operationBodyMapper.getNonMappedLeavesT1();
			List<CompositeStatementObject> innerNodes1 = leaves1Sublist.isPresent() ? new ArrayList<>() : operationBodyMapper.getNonMappedInnerNodesT1();
			for(Pair<AbstractCodeFragment, UMLComment> pair : operationBodyMapper.getCommentedCode()) {
				if(leaves1.contains(pair.getLeft())) {
					leaves1.remove(pair.getLeft());
				}
				if(innerNodes1.contains(pair.getLeft())) {
					innerNodes1.remove(pair.getLeft());
				}
			}
			//adding leaves that were mapped with replacements
			Set<AbstractCodeFragment> addedLeaves1 = new LinkedHashSet<AbstractCodeFragment>();
			Set<CompositeStatementObject> addedInnerNodes1 = new LinkedHashSet<CompositeStatementObject>();
			List<AbstractCodeFragment> leaves2 = composite2.getLeaves();
			if(leaves1Sublist.isPresent()) {
				for(AbstractCodeFragment nonMappedLeaf1 : new ArrayList<>(leaves1Sublist.get())) {
					expandAnonymousAndLambdas(nonMappedLeaf1, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
				}
			}
			else {
				for(AbstractCodeFragment nonMappedLeaf1 : new ArrayList<>(operationBodyMapper.getNonMappedLeavesT1())) {
					expandAnonymousAndLambdas(nonMappedLeaf1, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
				}
				for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
					if(!returnWithVariableReplacement(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()) ||
							!mapping.getFragment1().getClass().equals(mapping.getFragment2().getClass()))) {
						AbstractCodeFragment fragment = mapping.getFragment1();
						expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
						if(fragment instanceof CompositeStatementObject) {
							CompositeStatementObject comp = (CompositeStatementObject)fragment;
							if(!innerNodes1.contains(comp)) {
								innerNodes1.add(comp);
								addedInnerNodes1.add(comp);
							}
							handleBlocks(comp, innerNodes1, addedInnerNodes1);
						}
					}
					else if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
						for(AbstractCodeFragment leaf2 : leaves2) {
							if(mapping.getFragment1().getString().equals(leaf2.getString())) {
								CompositeStatementObject parent1 = mapping.getFragment1().getParent();
								if(parent1.getParent() != null && (!operationBodyMapper.alreadyMatched1(parent1) || (parent1.isBlock() && !operationBodyMapper.alreadyMatched1(parent1.getParent())))) {
									AbstractCodeFragment fragment = mapping.getFragment1();
									expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
									break;
								}
							}
						}
					}
				}
				if(nested && operationBodyMapper.getParentMapper() != null && operationBodyMapper.getParentMapper().getChildMappers().isEmpty()) {
					for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
						if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
							if(mapping.getFragment1().getString().equals(JAVA.RETURN_NULL)) {
								AbstractCodeFragment fragment = mapping.getFragment1();
								expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
							}
						}
					}
					for(AbstractCodeMapping mapping : operationBodyMapper.getParentMapper().getMappings()) {
						if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
							if((mapping.getFragment1().getString().equals(JAVA.RETURN_TRUE) || mapping.getFragment1().getString().equals(JAVA.RETURN_FALSE)) && addedOperation.getReturnParameter().getType().toString().equals("boolean")) {
								AbstractCodeFragment fragment = mapping.getFragment1();
								expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
							}
						}
						else if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
							AbstractCodeFragment fragment = mapping.getFragment1();
							if(fragment instanceof CompositeStatementObject) {
								CompositeStatementObject statement = (CompositeStatementObject)fragment;
								if(!innerNodes1.contains(statement)) {
									innerNodes1.add(statement);
									addedInnerNodes1.add(statement);
								}
							}
						}
					}
				}
				for(UMLOperationBodyMapper childMapper : operationBodyMapper.childMappers) {
					if(childMapper.container1.getClassName().equals(addedOperation.getClassName()) || classDiff instanceof UMLClassMoveDiff) {
						for(AbstractCodeMapping mapping : childMapper.getMappings()) {
							AbstractCodeFragment fragment = mapping.getFragment1();
							if(!returnWithVariableReplacement(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()))) {
								expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, childMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
								if(fragment instanceof CompositeStatementObject) {
									CompositeStatementObject comp = (CompositeStatementObject)fragment;
									if(!innerNodes1.contains(comp)) {
										innerNodes1.add(comp);
										addedInnerNodes1.add(comp);
									}
									handleBlocks(comp, innerNodes1, addedInnerNodes1);
								}
							}
							else if(fragment instanceof TryStatementObject && ((TryStatementObject)fragment).identicalCatchOrFinallyBlocks((TryStatementObject)mapping.getFragment2())) {
								expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, childMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
								if(fragment instanceof CompositeStatementObject) {
									CompositeStatementObject comp = (CompositeStatementObject)fragment;
									if(!innerNodes1.contains(comp)) {
										innerNodes1.add(comp);
										addedInnerNodes1.add(comp);
									}
									handleBlocks(comp, innerNodes1, addedInnerNodes1);
								}
							}
							else if(fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
									!((CompositeStatementObject)fragment).stringRepresentation().equals(((CompositeStatementObject)mapping.getFragment2()).stringRepresentation())) {
								expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, childMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
								if(fragment instanceof CompositeStatementObject) {
									CompositeStatementObject comp = (CompositeStatementObject)fragment;
									if(!innerNodes1.contains(comp)) {
										innerNodes1.add(comp);
										addedInnerNodes1.add(comp);
									}
									handleBlocks(comp, innerNodes1, addedInnerNodes1);
								}
							}
						}
						for(AbstractCodeFragment fragment : childMapper.getNonMappedLeavesT1()) {
							expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, childMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
						}
					}
				}
			}
			
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			Set<AbstractCodeFragment> addedLeaves2 = new LinkedHashSet<AbstractCodeFragment>();
			Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeFragment statement : new ArrayList<>(leaves2)) {
				expandAnonymousAndLambdas(statement, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, anonymousClassList2(), codeFragmentOperationMap2, container2, false);
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(AbstractCodeFragment leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(AbstractCodeFragment leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			
			//adding innerNodes that were mapped with replacements
			if(leaves1Sublist.isEmpty()) {
				for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
					if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
						AbstractCodeFragment fragment = mapping.getFragment1();
						if(fragment instanceof CompositeStatementObject) {
							CompositeStatementObject statement = (CompositeStatementObject)fragment;
							if(!innerNodes1.contains(statement)) {
								innerNodes1.add(statement);
								addedInnerNodes1.add(statement);
							}
						}
					}
					else if(mapping instanceof CompositeStatementObjectMapping) {
						if(((CompositeStatementObjectMapping)mapping).getCompositeChildMatchingScore() <= 0.01) {
							AbstractCodeFragment fragment = mapping.getFragment1();
							if(fragment instanceof CompositeStatementObject) {
								CompositeStatementObject statement = (CompositeStatementObject)fragment;
								if(!innerNodes1.contains(statement)) {
									innerNodes1.add(statement);
									addedInnerNodes1.add(statement);
								}
							}
						}
						else {
							//search for leaf mappings being inexact
							int subsumedLeafMappings = 0;
							int inExactSubsumedLeafMappings = 0;
							for(AbstractCodeMapping mapping2 : operationBodyMapper.getMappings()) {
								if(mapping2.equals(mapping)) {
									break;
								}
								if((mapping.getFragment1().getLocationInfo().subsumes(mapping2.getFragment1().getLocationInfo()) ||
										mapping.getFragment2().getLocationInfo().subsumes(mapping2.getFragment2().getLocationInfo())) &&
										!mapping.getFragment1().equals(mapping2.getFragment1()) && !mapping.getFragment2().equals(mapping2.getFragment2())) {
									subsumedLeafMappings++;
									if(!mapping2.getReplacements().isEmpty() || !mapping2.getFragment1().equalFragment(mapping2.getFragment2())) {
										inExactSubsumedLeafMappings++;
									}
								}
							}
							if(inExactSubsumedLeafMappings == subsumedLeafMappings && subsumedLeafMappings > 0) {
								AbstractCodeFragment fragment = mapping.getFragment1();
								if(fragment instanceof CompositeStatementObject) {
									CompositeStatementObject statement = (CompositeStatementObject)fragment;
									if(!innerNodes1.contains(statement)) {
										innerNodes1.add(statement);
										addedInnerNodes1.add(statement);
									}
								}
							}
						}
					}
				}
			}
			innerNodes2.remove(composite2);
			innerNodes2.addAll(addedInnerNodes2);
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNode2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2, parameterToArgumentMap2, false);
			
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, parameterToArgumentMap2, false);
			
			Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
			Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
				processStreamAPIStatements(leaves1, leaves2, streamAPIStatements1, innerNodes2);
			}
			else if(streamAPIStatements1.size() > 1 || streamAPIStatements2.size() > 1) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			
			//match expressions in inner nodes from T1 with leaves from T2
			List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
				if(!addedInnerNodes1.contains(composite)) {
					for(AbstractExpression expression : composite.getExpressions()) {
						expression.replaceParametersWithArguments(parameterToArgumentMap1);
						expressionsT1.add(expression);
					}
				}
			}
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					CompositeStatementObject composite = (CompositeStatementObject)mapping.getFragment1();
					for(AbstractExpression expression : composite.getExpressions()) {
						if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME) &&
								mapping.getFragment1().getVariableDeclarations().toString().equals(mapping.getFragment2().getVariableDeclarations().toString())) {
							continue;
						}
						if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME)) {
							continue;
						}
						expression.replaceParametersWithArguments(parameterToArgumentMap1);
						expressionsT1.add(expression);
					}
				}
			}
			int numberOfMappings = mappings.size();
			for(AbstractCodeMapping mapping : this.mappings) {
				if(mapping instanceof LeafMapping) {
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					if(fragment2 instanceof StatementObject) {
						addedLeaves2.add((StatementObject)fragment2);
						leaves2.add((StatementObject)fragment2);
					}
				}
			}
			processLeaves(expressionsT1, leaves2, parameterToArgumentMap2, false);
			List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
			for(int i = numberOfMappings; i < mappings.size(); i++) {
				mappings.get(i).temporaryVariableAssignment(refactorings, leaves2, parentMapper != null);
			}
			// TODO remove non-mapped inner nodes from T1 corresponding to mapped expressions
						
			//find exact mappings, whose parents are not mapped, because they were mapped in the parent mapper
			List<CompositeStatementObject> composites1 = new ArrayList<>();
			List<CompositeStatementObject> composites2 = new ArrayList<>();
			for(AbstractCodeMapping mapping : this.mappings) {
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(mapping instanceof LeafMapping && fragment1.getString().equals(fragment2.getString())) {
					CompositeStatementObject parent1 = fragment1.getParent();
					CompositeStatementObject parent2 = fragment2.getParent();
					while(parent1 != null && parent2 != null && !composites1.contains(parent1) && !composites2.contains(parent2) &&
							!alreadyMatched2(parent2) && operationBodyMapper.alreadyMatched1(parent1) && !parent2.equals(composite2) && parent1.getString().equals(parent2.getString())) {
						composites2.add(parent2);
						composites1.add(parent1);
						parent2 = parent2.getParent();
						parent1 = parent1.getParent();
					}
				}
			}
			numberOfMappings = this.mappings.size();
			processInnerNodes(composites1, composites2, leaves1, leaves2, parameterToArgumentMap2, false);
			mappings = new ArrayList<>(this.mappings);
			for(int i = numberOfMappings; i < mappings.size(); i++) {
				innerNodes2.remove(mappings.get(i).getFragment2());
			}
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves1.removeAll(addedLeaves1);
			leaves2.removeAll(addedLeaves2);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes1.removeAll(addedInnerNodes1);
			innerNodes2.removeAll(addedInnerNodes2);
			updateNonMappedLeavesT1(leaves1);
			updateNonMappedLeavesT2(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
			}
			processCommentsAndJavadoc(addedOperation);
		}
	}

	private void processCommentsAndJavadoc(UMLOperation addedOperation) {
		int totalCallCountFromDifferentMethods = 0;
		if(classDiff != null) {
			for(UMLOperation operation : classDiff.getNextClass().getOperations()) {
				AbstractCall delegateCall = operation.isDelegate();
				for(AbstractCall call : operation.getAllOperationInvocations()) {
					if(delegateCall != null && call.equals(delegateCall) && operation.equals(container1)) {
						continue;
					}
					if(call.matchesOperation(addedOperation, operation, classDiff, modelDiff)) {
						if(classDiff.getOriginalClass().containsOperationWithTheSameSignature(operation) ||
								classDiff.getOriginalClass().containsOperationWithTheSameSignatureIgnoringTypeParameters(operation)) {
							totalCallCountFromDifferentMethods++;
							break;
						}
					}
				}
			}
		}
		if(parentMapper != null && parentMapper.commentListDiff != null) {
			AbstractCodeMapping parentMapping = findParentMappingContainingOperationInvocation();
			Set<UMLComment> deletedComments = new LinkedHashSet<UMLComment>();
			if(parentMapping != null) {
				boolean containsReturn = false;
				if(parentMapping.getFragment1() instanceof CompositeStatementObject) {
					CompositeStatementObject comp = (CompositeStatementObject)parentMapping.getFragment1();
					for(AbstractCodeFragment fragment : comp.getLeaves()) {
						if(fragment.commonReturnStatement()) {
							containsReturn = true;
							break;
						}
					}
					if(comp.getAllStatements().size() == 1 || (comp.getAllStatements().size() == 2 && comp.getAllStatements().get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK))) {
						containsReturn = true;
					}
				}
				if(!containsReturn) {
					for(UMLComment deletedComment : parentMapper.commentListDiff.getDeletedComments()) {
						if(parentMapping.getFragment1().getLocationInfo().subsumes(deletedComment.getLocationInfo())) {
							deletedComments.add(deletedComment);
						}
					}
				}
				else {
					deletedComments.addAll(parentMapper.commentListDiff.getDeletedComments());
				}
			}
			else {
				deletedComments.addAll(parentMapper.commentListDiff.getDeletedComments());
			}
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				if(childMapper.commentListDiff != null)
					deletedComments.addAll(childMapper.commentListDiff.getDeletedComments());
			}
			this.commentListDiff = new UMLCommentListDiff(new ArrayList<>(deletedComments), container2.getComments(), this);
			checkUnmatchedStatementsBeingCommented();
		}
		if(totalCallCountFromDifferentMethods > 1) {
			return;
		}
		if(parentMapper != null && parentMapper.javadocDiff.isPresent() && addedOperation.getJavadoc() != null) {
			if(parentMapper.javadocDiff.get().getDeletedTags().size() > 0 || parentMapper.javadocDiff.get().getDeletedDocElements().size() > 0) {
				UMLJavadocDiff childJavadocDiff = new UMLJavadocDiff(parentMapper.container1.getJavadoc(), addedOperation.getJavadoc(), parentMapper.javadocDiff.get());
				if(childJavadocDiff.getCommonTags().size() > 0 || childJavadocDiff.getCommonDocElements().size() > 0) {
					this.javadocDiff = Optional.of(childJavadocDiff);
				}
			}
		}
	}

	private void handleBlocks(CompositeStatementObject comp, List<CompositeStatementObject> innerNodes,
			Set<CompositeStatementObject> addedInnerNodes) {
		if(comp.getStatements().size() == 1 && comp.getStatements().get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			if(!innerNodes.contains(comp.getStatements().get(0))) {
				innerNodes.add((CompositeStatementObject)comp.getStatements().get(0));
				addedInnerNodes.add((CompositeStatementObject)comp.getStatements().get(0));
			}
		}
		if(comp.getStatements().size() == 2 && comp.getStatements().get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
				comp.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			if(!innerNodes.contains(comp.getStatements().get(0))) {
				innerNodes.add((CompositeStatementObject)comp.getStatements().get(0));
				addedInnerNodes.add((CompositeStatementObject)comp.getStatements().get(0));
			}
			if(!innerNodes.contains(comp.getStatements().get(1))) {
				innerNodes.add((CompositeStatementObject)comp.getStatements().get(1));
				addedInnerNodes.add((CompositeStatementObject)comp.getStatements().get(1));
			}
		}
	}

	private void expandAnonymousAndLambdas(AbstractCodeFragment fragment, List<AbstractCodeFragment> leaves, List<CompositeStatementObject> innerNodes,
			Set<AbstractCodeFragment> addedLeaves, Set<CompositeStatementObject> addedInnerNodes,
			List<UMLAnonymousClass> anonymousClassList, Map<AbstractCodeFragment, VariableDeclarationContainer> map, VariableDeclarationContainer parentOperation, boolean excludeRootBlock) {
		if(fragment instanceof StatementObject) {
			StatementObject statement = (StatementObject)fragment;
			if(!leaves.contains(statement)) {
				leaves.add(statement);
				addedLeaves.add(statement);
			}
			if(!statement.getAnonymousClassDeclarations().isEmpty()) {
				for(UMLAnonymousClass anonymous : anonymousClassList) {
					if(statement.getLocationInfo().subsumes(anonymous.getLocationInfo())) {
						for(UMLOperation anonymousOperation : anonymous.getOperations()) {
							if(anonymousOperation.getBody() != null) {
								boolean exclude = false;
								if(parentMapper != null) {
									for(UMLAnonymousClassDiff anonymousClassDiff : parentMapper.getAnonymousClassDiffs()) {
										for(UMLOperationBodyMapper anonymousMethodMapper : anonymousClassDiff.getOperationBodyMapperList()) {
											if(anonymousMethodMapper.getContainer1().equals(anonymousOperation) || anonymousMethodMapper.getContainer2().equals(anonymousOperation)) {
												if(anonymousMethodMapper.getContainer1().getBodyHashCode() == anonymousMethodMapper.getContainer2().getBodyHashCode() &&
														anonymousOperation.getBodyHashCode() == anonymousMethodMapper.getContainer1().getBodyHashCode()) {
													exclude = true;
													break;
												}
											}
										}
									}
								}
								if(!exclude) {
									List<AbstractCodeFragment> anonymousClassLeaves = anonymousOperation.getBody().getCompositeStatement().getLeaves();
									for(AbstractCodeFragment anonymousLeaf : anonymousClassLeaves) {
										if(!leaves.contains(anonymousLeaf)) {
											leaves.add(anonymousLeaf);
											addedLeaves.add(anonymousLeaf);
											map.put(anonymousLeaf, anonymousOperation);
										}
									}
									List<CompositeStatementObject> anonymousClassInnerNodes = anonymousOperation.getBody().getCompositeStatement().getInnerNodes();
									for(CompositeStatementObject anonymousInnerNode : anonymousClassInnerNodes) {
										if(excludeRootBlock && anonymousInnerNode.equals(anonymousOperation.getBody().getCompositeStatement())) {
											continue;
										}
										if(!innerNodes.contains(anonymousInnerNode)) {
											innerNodes.add(anonymousInnerNode);
											addedInnerNodes.add(anonymousInnerNode);
											map.put(anonymousInnerNode, anonymousOperation);
										}
									}
								}
							}
						}
						for(UMLInitializer initializer : anonymous.getInitializers()) {
							List<AbstractCodeFragment> anonymousClassLeaves = initializer.getBody().getCompositeStatement().getLeaves();
							for(AbstractCodeFragment anonymousLeaf : anonymousClassLeaves) {
								if(!leaves.contains(anonymousLeaf)) {
									leaves.add(anonymousLeaf);
									addedLeaves.add(anonymousLeaf);
									map.put(anonymousLeaf, initializer);
								}
							}
							List<CompositeStatementObject> anonymousClassInnerNodes = initializer.getBody().getCompositeStatement().getInnerNodes();
							for(CompositeStatementObject anonymousInnerNode : anonymousClassInnerNodes) {
								if(excludeRootBlock && anonymousInnerNode.equals(initializer.getBody().getCompositeStatement())) {
									continue;
								}
								if(!innerNodes.contains(anonymousInnerNode)) {
									innerNodes.add(anonymousInnerNode);
									addedInnerNodes.add(anonymousInnerNode);
									map.put(anonymousInnerNode, initializer);
								}
							}
						}
					}
				}
			}
			if(!statement.getLambdas().isEmpty()) {
				for(LambdaExpressionObject lambda : statement.getLambdas()) {
					List<LambdaExpressionObject> nestedLambdas = new ArrayList<LambdaExpressionObject>();
					collectNestedLambdaExpressions(lambda, nestedLambdas);
					expandLambda(lambda, leaves, innerNodes, addedLeaves, addedInnerNodes, map, parentOperation, excludeRootBlock);
					for(LambdaExpressionObject nestedLambda : nestedLambdas) {
						expandLambda(nestedLambda, leaves, innerNodes, addedLeaves, addedInnerNodes, map, parentOperation, excludeRootBlock);
					}
				}
			}
		}
	}

	private void expandLambda(LambdaExpressionObject lambda, List<AbstractCodeFragment> leaves,
			List<CompositeStatementObject> innerNodes, Set<AbstractCodeFragment> addedLeaves,
			Set<CompositeStatementObject> addedInnerNodes, Map<AbstractCodeFragment, VariableDeclarationContainer> map,
			VariableDeclarationContainer parentOperation, boolean excludeRootBlock) {
		if(lambda.getBody() != null) {
			List<AbstractCodeFragment> lambdaLeaves = lambda.getBody().getCompositeStatement().getLeaves();
			for(AbstractCodeFragment lambdaLeaf : lambdaLeaves) {
				if(!leaves.contains(lambdaLeaf)) {
					leaves.add(lambdaLeaf);
					addedLeaves.add(lambdaLeaf);
					map.put(lambdaLeaf, parentOperation);
				}
			}
			List<CompositeStatementObject> lambdaInnerNodes = lambda.getBody().getCompositeStatement().getInnerNodes();
			for(CompositeStatementObject lambdaInnerNode : lambdaInnerNodes) {
				if(excludeRootBlock && lambdaInnerNode.equals(lambda.getBody().getCompositeStatement())) {
					continue;
				}
				if(!innerNodes.contains(lambdaInnerNode)) {
					innerNodes.add(lambdaInnerNode);
					addedInnerNodes.add(lambdaInnerNode);
					map.put(lambdaInnerNode, parentOperation);
				}
			}
		}
		else if(lambda.getExpression() != null) {
			AbstractCodeFragment lambdaLeaf = lambda.getExpression();
			if(!leaves.contains(lambdaLeaf)) {
				leaves.add(lambdaLeaf);
				addedLeaves.add(lambdaLeaf);
				map.put(lambdaLeaf, parentOperation);
			}
		}
	}

	public UMLOperationBodyMapper(UMLOperation removedOperation, UMLOperationBodyMapper operationBodyMapper,
			Map<String, String> parameterToArgumentMap1, Map<String, String> parameterToArgumentMap2, UMLAbstractClassDiff classDiff, AbstractCall operationInvocation, boolean nested) throws RefactoringMinerTimedOutException {
		this.parentMapper = operationBodyMapper;
		this.operationInvocation = operationInvocation;
		this.nested = nested;
		this.parameterToArgumentMap1 = parameterToArgumentMap1;
		this.parameterToArgumentMap2 = parameterToArgumentMap2;
		this.container1 = removedOperation;
		this.container2 = operationBodyMapper.container2;
		this.callSiteOperation = operationBodyMapper.container1;
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		
		OperationBody removedOperationBody = removedOperation.getBody();
		if(removedOperationBody != null) {
			List<AbstractCodeFragment> leavesT1 = new ArrayList<AbstractCodeFragment>();
			List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment1();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject composite = (CompositeStatementObject)fragment;
						for(AbstractExpression expression : composite.getExpressions()) {
							if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME) &&
									mapping.getFragment1().getVariableDeclarations().toString().equals(mapping.getFragment2().getVariableDeclarations().toString())) {
								continue;
							}
							expressionsT1.add(expression);
						}
					}
					else {
						if(!leavesT1.contains(fragment))
							leavesT1.add(fragment);
					}
				}
			}
			leavesT1.addAll(operationBodyMapper.getNonMappedLeavesT1());
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					if(!expressionsT1.contains(expression))
						expressionsT1.add(expression);
				}
			}
			this.callsToInlinedMethod = callsToInlinedMethod(leavesT1);
			this.callsToInlinedMethod += callsToInlinedMethod(expressionsT1);
			
			CompositeStatementObject composite1 = removedOperationBody.getCompositeStatement();
			List<AbstractCodeFragment> leaves2 = operationBodyMapper.getNonMappedLeavesT2();
			List<CompositeStatementObject> innerNodes2 = operationBodyMapper.getNonMappedInnerNodesT2();
			//adding leaves that were mapped with replacements or are inexact matches
			Set<AbstractCodeFragment> addedLeaves2 = new LinkedHashSet<AbstractCodeFragment>();
			//adding innerNodes that were mapped with replacements or are inexact matches
			Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeFragment statement : new ArrayList<>(operationBodyMapper.getNonMappedLeavesT2())) {
				expandAnonymousAndLambdas(statement, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, operationBodyMapper.anonymousClassList2(), codeFragmentOperationMap2, container2, false);
			}
			List<AbstractCodeFragment> leaves1 = composite1.getLeaves();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!returnWithVariableReplacement(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()) ||
						!mapping.getFragment1().getClass().equals(mapping.getFragment2().getClass()))) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					expandAnonymousAndLambdas(fragment, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, operationBodyMapper.anonymousClassList2(), codeFragmentOperationMap2, container2, false);
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject comp = (CompositeStatementObject)fragment;
						if(!innerNodes2.contains(comp)) {
							innerNodes2.add(comp);
							addedInnerNodes2.add(comp);
						}
						handleBlocks(comp, innerNodes2, addedInnerNodes2);
					}
				}
				else if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(mapping.getFragment2().getString().equals(leaf1.getString())) {
							CompositeStatementObject parent2 = mapping.getFragment2().getParent();
							if(parent2.getParent() != null && (!operationBodyMapper.alreadyMatched2(parent2) || (parent2.isBlock() && !operationBodyMapper.alreadyMatched2(parent2.getParent())))) {
								AbstractCodeFragment fragment = mapping.getFragment2();
								expandAnonymousAndLambdas(fragment, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, operationBodyMapper.anonymousClassList2(), codeFragmentOperationMap2, container2, false);
								break;
							}
						}
					}
				}
			}
			for(UMLOperationBodyMapper childMapper : operationBodyMapper.childMappers) {
				if(childMapper.container2.getClassName().equals(removedOperation.getClassName()) || classDiff instanceof UMLClassMoveDiff) {
					for(AbstractCodeMapping mapping : childMapper.getMappings()) {
						if(!returnWithVariableReplacement(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()))) {
							AbstractCodeFragment fragment = mapping.getFragment2();
							expandAnonymousAndLambdas(fragment, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, childMapper.anonymousClassList2(), codeFragmentOperationMap2, container2, false);
							if(fragment instanceof CompositeStatementObject) {
								CompositeStatementObject comp = (CompositeStatementObject)fragment;
								if(!innerNodes2.contains(comp)) {
									innerNodes2.add(comp);
									addedInnerNodes2.add(comp);
								}
								handleBlocks(comp, innerNodes2, addedInnerNodes2);
							}
						}
					}
					for(AbstractCodeFragment fragment : childMapper.getNonMappedLeavesT2()) {
						expandAnonymousAndLambdas(fragment, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, childMapper.anonymousClassList2(), codeFragmentOperationMap2, container2, false);
					}
				}
			}
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			Set<AbstractCodeFragment> addedLeaves1 = new LinkedHashSet<AbstractCodeFragment>();
			Set<CompositeStatementObject> addedInnerNodes1 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeFragment statement : new ArrayList<>(leaves1)) {
				expandAnonymousAndLambdas(statement, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, anonymousClassList1(), codeFragmentOperationMap1, container1, false);
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				//check for temporary variables that the argument might be assigned to
				for(AbstractCodeFragment leave2 : leaves2) {
					List<VariableDeclaration> variableDeclarations = leave2.getVariableDeclarations();
					for(VariableDeclaration variableDeclaration : variableDeclarations) {
						for(String parameter : parameterToArgumentMap1.keySet()) {
							String argument = parameterToArgumentMap1.get(parameter);
							if(variableDeclaration.getInitializer() != null && argument.equals(variableDeclaration.getInitializer().toString())) {
								parameterToArgumentMap1.put(parameter, variableDeclaration.getVariableName());
							}
						}
					}
				}
				for(AbstractCodeFragment leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(AbstractCodeFragment leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject statement = (CompositeStatementObject)fragment;
						if(!innerNodes2.contains(statement)) {
							innerNodes2.add(statement);
							addedInnerNodes2.add(statement);
						}
					}
				}
				else if(mapping instanceof CompositeStatementObjectMapping) {
					if(((CompositeStatementObjectMapping)mapping).getCompositeChildMatchingScore() <= 0.01) {
						AbstractCodeFragment fragment = mapping.getFragment2();
						if(fragment instanceof CompositeStatementObject) {
							CompositeStatementObject statement = (CompositeStatementObject)fragment;
							if(!innerNodes2.contains(statement)) {
								innerNodes2.add(statement);
								addedInnerNodes2.add(statement);
							}
						}
					}
					else {
						//search for leaf mappings being inexact
						int subsumedLeafMappings = 0;
						int inExactSubsumedLeafMappings = 0;
						for(AbstractCodeMapping mapping2 : operationBodyMapper.getMappings()) {
							if(mapping2.equals(mapping)) {
								break;
							}
							if((mapping.getFragment1().getLocationInfo().subsumes(mapping2.getFragment1().getLocationInfo()) ||
									mapping.getFragment2().getLocationInfo().subsumes(mapping2.getFragment2().getLocationInfo())) &&
									!mapping.getFragment1().equals(mapping2.getFragment1()) && !mapping.getFragment2().equals(mapping2.getFragment2())) {
								subsumedLeafMappings++;
								if(!mapping2.getReplacements().isEmpty() || !mapping2.getFragment1().equalFragment(mapping2.getFragment2())) {
									inExactSubsumedLeafMappings++;
								}
							}
						}
						if(inExactSubsumedLeafMappings == subsumedLeafMappings && subsumedLeafMappings > 0) {
							AbstractCodeFragment fragment = mapping.getFragment2();
							if(fragment instanceof CompositeStatementObject) {
								CompositeStatementObject statement = (CompositeStatementObject)fragment;
								if(!innerNodes2.contains(statement)) {
									innerNodes2.add(statement);
									addedInnerNodes2.add(statement);
								}
							}
						}
					}
				}
			}
			innerNodes1.remove(composite1);
			innerNodes1.addAll(addedInnerNodes1);
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNode2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2, parameterToArgumentMap1, false);
			
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, parameterToArgumentMap1, false);
			
			Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
			Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
				processStreamAPIStatements(leaves1, leaves2, streamAPIStatements1, innerNodes2);
			}
			
			//match expressions in inner nodes from T2 with leaves from T1
			List<AbstractExpression> expressionsT2 = new ArrayList<AbstractExpression>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
				if(!addedInnerNodes2.contains(composite)) {
					for(AbstractExpression expression : composite.getExpressions()) {
						expression.replaceParametersWithArguments(parameterToArgumentMap2);
						expressionsT2.add(expression);
					}
				}
			}
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					CompositeStatementObject composite = (CompositeStatementObject)mapping.getFragment2();
					for(AbstractExpression expression : composite.getExpressions()) {
						if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME) &&
								mapping.getFragment1().getVariableDeclarations().toString().equals(mapping.getFragment2().getVariableDeclarations().toString())) {
							continue;
						}
						if(expression.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME)) {
							continue;
						}
						expression.replaceParametersWithArguments(parameterToArgumentMap2);
						expressionsT2.add(expression);
					}
				}
			}
			int numberOfMappings = mappings.size();
			for(AbstractCodeMapping mapping : this.mappings) {
				if(mapping instanceof LeafMapping) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					if(fragment1 instanceof StatementObject) {
						addedLeaves1.add((StatementObject)fragment1);
						leaves1.add((StatementObject)fragment1);
					}
				}
			}
			processLeaves(leaves1, expressionsT2, parameterToArgumentMap1, false);
			List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
			for(int i = numberOfMappings; i < mappings.size(); i++) {
				mappings.get(i).temporaryVariableAssignment(refactorings, leaves2, parentMapper != null);
			}
			
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves1.removeAll(addedLeaves1);
			leaves2.removeAll(addedLeaves2);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes1.removeAll(addedInnerNodes1);
			innerNodes2.removeAll(addedInnerNodes2);
			updateNonMappedLeavesT1(leaves1);
			updateNonMappedLeavesT2(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
			}
			if(parentMapper != null && parentMapper.commentListDiff != null) {
				AbstractCodeMapping parentMapping = findParentMappingContainingOperationInvocation();
				Set<UMLComment> addedComments = new LinkedHashSet<UMLComment>();
				if(parentMapping != null) {
					boolean containsReturn = false;
					if(parentMapping.getFragment2() instanceof CompositeStatementObject) {
						CompositeStatementObject comp = (CompositeStatementObject)parentMapping.getFragment2();
						for(AbstractCodeFragment fragment : comp.getLeaves()) {
							if(fragment.getString().equals(JAVA.RETURN_STATEMENT)) {
								containsReturn = true;
								break;
							}
						}
						if(comp.getAllStatements().size() == 1 || (comp.getAllStatements().size() == 2 && comp.getAllStatements().get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK))) {
							containsReturn = true;
						}
					}
					if(!containsReturn) {
						for(UMLComment addedComment : parentMapper.commentListDiff.getAddedComments()) {
							if(parentMapping.getFragment2().getLocationInfo().subsumes(addedComment.getLocationInfo())) {
								addedComments.add(addedComment);
							}
						}
					}
					else {
						addedComments.addAll(parentMapper.commentListDiff.getAddedComments());
					}
				}
				else {
					addedComments.addAll(parentMapper.commentListDiff.getAddedComments());
				}
				for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
					if(childMapper.commentListDiff != null)
						addedComments.addAll(childMapper.commentListDiff.getAddedComments());
				}
				this.commentListDiff = new UMLCommentListDiff(container1.getComments(), new ArrayList<>(addedComments), this);
				checkUnmatchedStatementsBeingCommented();
			}
		}
	}

	private void updateNonMappedLeavesT1(List<AbstractCodeFragment> leaves1) {
		for(AbstractCodeFragment leaf : leaves1) {
			if(!mappingHashcodesT1.contains(leaf.hashCode())) {
				nonMappedLeavesT1.add(leaf);
			}
		}
	}

	private void updateNonMappedLeavesT2(List<AbstractCodeFragment> leaves2) {
		for(AbstractCodeFragment leaf : leaves2) {
			if(!mappingHashcodesT2.contains(leaf.hashCode())) {
				nonMappedLeavesT2.add(leaf);
			}
		}
	}

	public VariableDeclarationContainer getContainer1() {
		return container1;
	}

	public VariableDeclarationContainer getContainer2() {
		return container2;
	}

	public UMLOperation getOperation1() {
		if(container1 instanceof UMLOperation)
			return (UMLOperation)container1;
		return null;
	}

	public UMLOperation getOperation2() {
		if(container2 instanceof UMLOperation)
			return (UMLOperation)container2;
		return null;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMatchedVariables() {
		return matchedVariables;
	}

	public Set<Refactoring> getRefactorings() throws RefactoringMinerTimedOutException {
		computeRefactoringsWithinBody();

		if(parentMapper == null && getOperation1() != null && getOperation2() != null) {
			this.operationSignatureDiff = new UMLOperationDiff(this);
			Set<Refactoring> temp = operationSignatureDiff.getRefactorings();
			for(Refactoring refactoring : refactorings) {
				//remove redundant Add/Remove Parameter refactorings
				Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
				if(refactoring.getRefactoringType().equals(RefactoringType.SPLIT_PARAMETER)) {
					SplitVariableRefactoring split = (SplitVariableRefactoring)refactoring;
					for(Refactoring ref : temp) {
						if(ref instanceof RemoveParameterRefactoring) {
							RemoveParameterRefactoring removeParameter = (RemoveParameterRefactoring)ref;
							if(split.getOldVariable().equals(removeParameter.getParameter().getVariableDeclaration())) {
								refactoringsToBeRemoved.add(ref);
							}
						}
						else if(ref instanceof AddParameterRefactoring) {
							AddParameterRefactoring addParameter = (AddParameterRefactoring)ref;
							if(split.getSplitVariables().contains(addParameter.getParameter().getVariableDeclaration())) {
								refactoringsToBeRemoved.add(ref);
							}
						}
					}
				}
				else if(refactoring.getRefactoringType().equals(RefactoringType.MERGE_PARAMETER)) {
					MergeVariableRefactoring merge = (MergeVariableRefactoring)refactoring;
					for(Refactoring ref : temp) {
						if(ref instanceof RemoveParameterRefactoring) {
							RemoveParameterRefactoring removeParameter = (RemoveParameterRefactoring)ref;
							if(merge.getMergedVariables().contains(removeParameter.getParameter().getVariableDeclaration())) {
								refactoringsToBeRemoved.add(ref);
							}
						}
						else if(ref instanceof AddParameterRefactoring) {
							AddParameterRefactoring addParameter = (AddParameterRefactoring)ref;
							if(merge.getNewVariable().equals(addParameter.getParameter().getVariableDeclaration())) {
								refactoringsToBeRemoved.add(ref);
							}
						}
					}
				}
				temp.removeAll(refactoringsToBeRemoved);
			}
			for(Refactoring refactoring : temp) {
				if(refactoring.getRefactoringType().equals(RefactoringType.RENAME_PARAMETER)) {
					RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
					matchedVariables.add(Pair.of(rename.getOriginalVariable(), rename.getRenamedVariable()));
				}
			}
			this.refactorings.addAll(temp);
		}
		return refactorings;
	}

	public void computeRefactoringsWithinBody() throws RefactoringMinerTimedOutException {
		VariableReplacementAnalysis analysis = new VariableReplacementAnalysis(this, refactorings, classDiff, matchedVariables);
		refactorings.addAll(analysis.getVariableRenames());
		refactorings.addAll(analysis.getVariableMerges());
		refactorings.addAll(analysis.getVariableSplits());
		matchedVariables.addAll(analysis.getMatchedVariables());
		candidateAttributeRenames.addAll(analysis.getCandidateAttributeRenames());
		candidateAttributeMerges.addAll(analysis.getCandidateAttributeMerges());
		candidateAttributeSplits.addAll(analysis.getCandidateAttributeSplits());

		removedVariables = analysis.getRemovedVariables();
		removedVariables.addAll(analysis.getRemovedVariablesStoringTheReturnOfInlinedMethod());
		addedVariables = analysis.getAddedVariables();
		addedVariables.addAll(analysis.getAddedVariablesStoringTheReturnOfExtractedMethod());
		movedVariables = analysis.getMovedVariables();
		int assertThrows1 = 0;
		int assertThatThrownBy1 = 0;
		int assertTimeout1 = 0;
		for(AbstractCall call : container1.getAllOperationInvocations()) {
			if(call.getName().equals(JAVA.ASSERT_THROWS)) {
				assertThrows1++;
			}
			if(call.getName().equals(JAVA.ASSERT_THAT_THROWN_BY)) {
				assertThatThrownBy1++;
			}
			else if(call.getName().equals("assertTimeout")) {
				assertTimeout1++;
			}
		}
		Map<String, Set<AbstractCodeMapping>> assertThrowsMappings = new LinkedHashMap<>();
		Map<String, Set<AbstractCodeMapping>> assertThatThrownByMappings = new LinkedHashMap<>();
		Map<String, Set<AbstractCodeMapping>> assertTimeoutMappings = new LinkedHashMap<>();
		List<AbstractCall> assertThrowsCalls = new ArrayList<AbstractCall>();
		List<AbstractCall> assertTimeoutCalls = new ArrayList<AbstractCall>();
		List<AbstractCall> assertThatThrownByCalls = new ArrayList<AbstractCall>();
		for(AbstractCall call : container2.getAllOperationInvocations()) {
			if(call.getName().equals(JAVA.ASSERT_THROWS)) {
				assertThrowsCalls.add(call);
				for(AbstractCodeMapping mapping : this.mappings) {
					if(call.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) || mapping.getFragment2().getLocationInfo().subsumes(call.getLocationInfo())) {
						if(parentMapper != null && parentMapper.getMappings().contains(mapping)) {
							continue;
						}
						if(assertThrowsMappings.containsKey(call.actualString())) {
							assertThrowsMappings.get(call.actualString()).add(mapping);
						}
						else {
							Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
							mappings.add(mapping);
							assertThrowsMappings.put(call.actualString(), mappings);
						}
					}
				}
			}
			else if(call.getName().equals("assertTimeout")) {
				assertTimeoutCalls.add(call);
				for(AbstractCodeMapping mapping : this.mappings) {
					if(call.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) || mapping.getFragment2().getLocationInfo().subsumes(call.getLocationInfo())) {
						if(parentMapper != null && parentMapper.getMappings().contains(mapping)) {
							continue;
						}
						if(assertTimeoutMappings.containsKey(call.actualString())) {
							assertTimeoutMappings.get(call.actualString()).add(mapping);
						}
						else {
							Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
							mappings.add(mapping);
							assertTimeoutMappings.put(call.actualString(), mappings);
						}
					}
				}
			}
			else if(call.getName().equals(JAVA.ASSERT_THAT_THROWN_BY)) {
				assertThatThrownByCalls.add(call);
				for(AbstractCodeMapping mapping : this.mappings) {
					if(call.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) || mapping.getFragment2().getLocationInfo().subsumes(call.getLocationInfo())) {
						if(parentMapper != null && parentMapper.getMappings().contains(mapping)) {
							continue;
						}
						if(assertThatThrownByMappings.containsKey(call.actualString())) {
							assertThatThrownByMappings.get(call.actualString()).add(mapping);
						}
						else {
							Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
							mappings.add(mapping);
							assertThatThrownByMappings.put(call.actualString(), mappings);
						}
					}
				}
			}
		}
		if(assertThrows1 < assertThrowsCalls.size()) {
			for(AbstractCall assertThrowsCall : assertThrowsCalls) {
				Set<AbstractCodeMapping> set = assertThrowsMappings.get(assertThrowsCall.actualString());
				if(set != null && set.size() > 0) {
					AbstractCodeMapping firstMapping = set.iterator().next();
					AbstractCall call2 = firstMapping.getFragment2().invocationCoveringEntireFragment();
					if(call2 != null && call2.equals(assertThrowsCall)) {
						AbstractCall call1 = firstMapping.getFragment1().invocationCoveringEntireFragment();
						if(call1 != null && call1.equalArguments(call2)) {
							continue;
						}
					}
					AssertThrowsRefactoring ref = new AssertThrowsRefactoring(set, assertThrowsCall, container1, container2);
					refactorings.add(ref);
					handleAdditionalAssertThrowMappings(firstMapping, ref);
				}
			}
		}
		if(assertTimeout1 < assertTimeoutCalls.size()) {
			for(AbstractCall assertTimeoutCall : assertTimeoutCalls) {
				Set<AbstractCodeMapping> set = assertTimeoutMappings.get(assertTimeoutCall.actualString());
				if(set != null && set.size() > 0) {
					AbstractCodeMapping firstMapping = set.iterator().next();
					AbstractCall call2 = firstMapping.getFragment2().invocationCoveringEntireFragment();
					if(call2 != null && call2.equals(assertTimeoutCall)) {
						AbstractCall call1 = firstMapping.getFragment1().invocationCoveringEntireFragment();
						if(call1 != null && call1.equalArguments(call2)) {
							continue;
						}
					}
					AssertTimeoutRefactoring ref = new AssertTimeoutRefactoring(set, assertTimeoutCall, container1, container2);
					refactorings.add(ref);
				}
			}
		}
		if(assertThatThrownBy1 < assertThatThrownByCalls.size()) {
			for(AbstractCall assertThrowsCall : assertThatThrownByCalls) {
				Set<AbstractCodeMapping> set = assertThatThrownByMappings.get(assertThrowsCall.actualString());
				if(set != null && set.size() > 0) {
					AbstractCodeMapping firstMapping = set.iterator().next();
					AbstractCall call2 = firstMapping.getFragment2().invocationCoveringEntireFragment();
					if(call2 != null && call2.equals(assertThrowsCall)) {
						AbstractCall call1 = firstMapping.getFragment1().invocationCoveringEntireFragment();
						if(call1 != null && call1.equalArguments(call2)) {
							continue;
						}
					}
					AssertThrowsRefactoring ref = new AssertThrowsRefactoring(set, assertThrowsCall, container1, container2);
					refactorings.add(ref);
					handleAdditionalAssertThrowMappings(firstMapping, ref);
				}
			}
		}
		Set<AbstractCodeMapping> assumptionMappings = new HashSet<>();
		for (Iterator<AbstractCodeMapping> it = mappings.iterator(); it.hasNext();) {
			AbstractCodeMapping mapping = it.next();
			if ((mapping.getOperation1().hasTestAnnotation() || mapping.getOperation1().hasSetUpAnnotation()) && (mapping.getOperation2().hasTestAnnotation() || mapping.getOperation2().hasSetUpAnnotation())) {
				if ((mapping.containsReplacement(ReplacementType.INVERT_CONDITIONAL) || mapping.containsReplacement(ReplacementType.CONDITIONAL) || mapping.containsReplacement(ReplacementType.ARGUMENT_REPLACED_WITH_EXPRESSION))) {
					checkForExtractedPrecondition(mapping).ifPresent(m -> {
						// Replace mapping with newMapping
						it.remove();
						assumptionMappings.add(m);
					});
				}
			}
		}
		assumptionMappings.forEach(this::addMapping);
	}

	private Optional<AbstractCodeMapping> checkForExtractedPrecondition(AbstractCodeMapping mapping) {
		if (isAssumption(mapping.getFragment2())) {
			AbstractCodeMapping newMapping = mapping;
			if (mapping.getFragment1() instanceof AbstractExpression) {
				CompositeStatementObject conditional = mapping.getFragment1().getParent();
				if (containsCommonReturn(conditional)) {
					newMapping = new LeafMapping(conditional, mapping.getFragment2(), mapping.getOperation1(), mapping.getOperation2());
					// Make mapping subExpressionMapping of newMapping
					newMapping.addSubExpressionMapping((LeafMapping) mapping);
				}
			}
			AbstractCall assumptionCall = switch (newMapping.getFragment2().getLocationInfo().getCodeElementType()) {
				case METHOD_INVOCATION -> (AbstractCall) newMapping.getFragment2();
				case EXPRESSION_STATEMENT -> ((StatementObject) newMapping.getFragment2()).getMethodInvocations().stream().filter(c->c.getName().startsWith("assume")).findFirst().orElse(null);
				default -> null;
			};
			assert assumptionCall != null : "Assumption call should only pass isAssumption being either an AbstractCall (or an AbstractCall wrapped into a StatementObject)";
			ExtractPreconditionRefactoring ref = new ExtractPreconditionRefactoring(assumptionCall, newMapping.getFragment1(), newMapping.getOperation1(), newMapping.getOperation2());
			if (Set.of(RefactoringType.REPLACE_IGNORE_WITH_ASSUMPTION, RefactoringType.REPLACE_CONDITIONAL_WITH_ASSUMPTION, RefactoringType.REPLACE_ASSERTION_WITH_ASSUMPTION).contains(ref.getRefactoringType())) {
				newMapping.addRefactoring(ref);
				return Optional.of(newMapping);
			}
		}
		return Optional.empty();
	}
	private static boolean containsCommonReturn(CompositeStatementObject conditional) {
		return conditional.getAllStatements().stream().anyMatch(s->s.getString().startsWith("return"));
	}

	private boolean isAssumption(AbstractCodeFragment abstractCodeFragment) {
		return switch (abstractCodeFragment.getLocationInfo().getCodeElementType()) {
			case METHOD_INVOCATION -> ((OperationInvocation) abstractCodeFragment).getMethodName().startsWith("assume");
			case EXPRESSION_STATEMENT -> ((StatementObject) abstractCodeFragment).getMethodInvocations().stream().anyMatch(this::isAssumption);
			default -> false;
		};
	}

	private void handleAdditionalAssertThrowMappings(AbstractCodeMapping firstMapping, AssertThrowsRefactoring ref) {
		UMLOperation operation1 = getOperation1();
		UMLOperation operation2 = getOperation2();
		if(operation1 != null && operation2 != null) {
			boolean expectedFound = false;
			for(UMLAnnotation annotation : operation1.getAnnotations()) {
				Map<String, AbstractExpression> memberValuePairs = annotation.getMemberValuePairs();
				if(memberValuePairs.containsKey("expected")) {
					expectedFound = true;
					AbstractExpression expectedException = memberValuePairs.get("expected");
					for(AbstractCodeFragment fragment2 : nonMappedLeavesT2) {
						List<LeafExpression> leafExpressions = fragment2.findExpression(expectedException.getString());
						if(leafExpressions.size() == 1) {
							LeafMapping leafMapping = new LeafMapping(expectedException, leafExpressions.get(0), operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
							break;
						}
						if(fragment2 instanceof AbstractExpression) {
							for(AbstractCodeMapping mapping : mappings) {
								if(mapping instanceof LeafMapping && !mapping.getFragment2().equals(fragment2) && mapping.getFragment2().getLocationInfo().subsumes(fragment2.getLocationInfo())) {
									leafExpressions = mapping.getFragment2().findExpression(expectedException.getString());
									if(leafExpressions.size() == 1) {
										LeafMapping leafMapping = new LeafMapping(expectedException, leafExpressions.get(0), operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
										break;
									}
								}
							}
						}
					}
					if(nonMappedLeavesT2.isEmpty()) {
						List<LeafExpression> leafExpressions = firstMapping.getFragment2().findExpression(expectedException.getString());
						if(leafExpressions.size() == 1) {
							LeafMapping leafMapping = new LeafMapping(expectedException, leafExpressions.get(0), operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
							break;
						}
					}
				}
			}
			if(!expectedFound) {
				List<LeafExpression> typeLiterals1 = new ArrayList<>();
				List<LeafExpression> stringLiterals1 = new ArrayList<>();
				for(AbstractCodeFragment fragment1 : nonMappedLeavesT1) {
					typeLiterals1.addAll(fragment1.getTypeLiterals());
					stringLiterals1.addAll(fragment1.getStringLiterals());
				}
				List<LeafExpression> typeLiterals2 = new ArrayList<>();
				List<LeafExpression> stringLiterals2 = new ArrayList<>();
				for(AbstractCodeFragment fragment2 : nonMappedLeavesT2) {
					typeLiterals2.addAll(fragment2.getTypeLiterals());
					stringLiterals2.addAll(fragment2.getStringLiterals());
				}
				if(typeLiterals1.size() == typeLiterals2.size()) {
					for(int i=0; i<typeLiterals1.size(); i++) {
						LeafExpression expr1 = typeLiterals1.get(i);
						LeafExpression expr2 = typeLiterals2.get(i);
						if(expr1.getString().equals(expr2.getString())) {
							LeafMapping leafMapping = new LeafMapping(expr1, expr2, operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
					}
				}
				if(stringLiterals1.size() == stringLiterals2.size()) {
					for(int i=0; i<stringLiterals1.size(); i++) {
						LeafExpression expr1 = stringLiterals1.get(i);
						LeafExpression expr2 = stringLiterals2.get(i);
						if(expr1.getString().equals(expr2.getString())) {
							LeafMapping leafMapping = new LeafMapping(expr1, expr2, operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
					}
				}
			}
		}
	}

	public Set<Refactoring> getRefactoringsAfterPostProcessing() {
		return refactorings;
	}

	public Set<CandidateAttributeRefactoring> getCandidateAttributeRenames() {
		return candidateAttributeRenames;
	}

	public Set<CandidateMergeVariableRefactoring> getCandidateAttributeMerges() {
		return candidateAttributeMerges;
	}

	public Set<CandidateSplitVariableRefactoring> getCandidateAttributeSplits() {
		return candidateAttributeSplits;
	}

	public Set<UMLAnonymousClassDiff> getAnonymousClassDiffs() {
		return anonymousClassDiffs;
	}

	public Set<AbstractCodeMapping> getMappings() {
		return mappings;
	}

	public List<AbstractCodeFragment> getNonMappedLeavesT1() {
		return nonMappedLeavesT1;
	}

	public List<AbstractCodeFragment> getNonMappedLeavesT2() {
		return nonMappedLeavesT2;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT1() {
		return nonMappedInnerNodesT1;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT2() {
		return nonMappedInnerNodesT2;
	}

	public List<CompositeReplacement> getCompositeReplacements() {
		List<CompositeReplacement> composites = new ArrayList<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			CompositeReplacement composite = mapping.containsCompositeReplacement();
			if(composite != null) {
				composites.add(composite);
			}
		}
		return composites;
	}

	public boolean containsOnlyBlockMappings() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				count++;
			}
			else if(mapping.getFragment1().getVariables().size() == 1 && mapping.getFragment2().getVariables().size() == 1 &&
					mapping.getFragment1().getString().equals(JAVA.RETURN_SPACE + mapping.getFragment1().getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION) &&
					mapping.getFragment2().getString().equals(JAVA.RETURN_SPACE + mapping.getFragment2().getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION)) {
				count++;
			}
		}
		return count > 0 && count == getMappings().size();
	}

	public boolean containsOnlySystemCalls() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getFragment1().getString().startsWith("System.") && mapping.getFragment2().getString().startsWith("System.") &&
					!mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				count++;
			}
			else if(mapping.getFragment1().getString().startsWith(JAVA.THROW_SPACE + "new") && mapping.getFragment2().getString().startsWith(JAVA.THROW_SPACE + "new") &&
					!mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				count++;
			}
			else if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				count++;
			}
			else if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) && mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
				count++;
			}
			else if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
					!mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				count++;
			}
		}
		return count > 0 && count == getMappings().size();
	}

	public int mappingsWithoutBlocks() {
		int count = 0;
		Set<LeafMapping> subExpressionMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			subExpressionMappings.addAll(mapping.getSubExpressionMappings());
			if(mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() && !subExpressionMappings.contains(mapping))
				count++;
		}
		return count;
	}

	public int nonMappedElementsT1() {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
			if(composite.countableStatement())
				nonMappedInnerNodeCount++;
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			if(statement.countableStatement())
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedLeafElementsT1() {
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			if(statement.countableStatement())
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount;
	}

	public int nonMappedElementsT2() {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
			if(composite.countableStatement())
				nonMappedInnerNodeCount++;
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			if(statement.countableStatement() && !isTemporaryVariableAssignment(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedLeafElementsT2() {
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			if(statement.countableStatement() && !isTemporaryVariableAssignment(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount;
	}

	private boolean isTemporaryVariableAssignment(AbstractCodeFragment statement) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariable = (ExtractVariableRefactoring)refactoring;
				if(statement.getVariableDeclarations().contains(extractVariable.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private void inlinedVariableAssignment(AbstractCodeFragment statement, List<AbstractCodeFragment> nonMappedLeavesT2) throws RefactoringMinerTimedOutException {
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeMapping mapping : getMappings()) {
			int refactoringCount = mapping.getRefactorings().size();
			mapping.inlinedVariableAssignment(statement, nonMappedLeavesT2, classDiff, parentMapper != null);
			if(refactoringCount < mapping.getRefactorings().size()) {
				this.anonymousClassDiffs.addAll(mapping.getAnonymousClassDiffs());
				for(Refactoring newRefactoring : mapping.getRefactorings()) {
					if(!this.refactorings.contains(newRefactoring)) {
						this.refactorings.add(newRefactoring);
					}
					else {
						for(Refactoring refactoring : this.refactorings) {
							if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
								InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
								Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
								Set<AbstractCodeFragment> newUnmatchedStatementReferences = newInlineVariableRefactoring.getUnmatchedStatementReferences();
								InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
								oldInlineVariableRefactoring.addReferences(newReferences);
								oldInlineVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
								for(LeafMapping newLeafMapping : newInlineVariableRefactoring.getSubExpressionMappings()) {
									oldInlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
								}
								break;
							}
						}
					}
				}
			}
		}
	}

	private void temporaryVariableAssignment(AbstractCodeFragment statement, List<AbstractCodeFragment> nonMappedLeavesT2) throws RefactoringMinerTimedOutException {
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeMapping mapping : getMappings()) {
			int refactoringCount = mapping.getRefactorings().size();
			mapping.temporaryVariableAssignment(statement, nonMappedLeavesT2, classDiff, parentMapper != null, mappings);
			if(refactoringCount < mapping.getRefactorings().size()) {
				this.anonymousClassDiffs.addAll(mapping.getAnonymousClassDiffs());
				for(Refactoring newRefactoring : mapping.getRefactorings()) {
					if(!this.refactorings.contains(newRefactoring)) {
						this.refactorings.add(newRefactoring);
					}
					else {
						for(Refactoring refactoring : this.refactorings) {
							if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractVariableRefactoring) {
								ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
								Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
								Set<AbstractCodeFragment> newUnmatchedStatementReferences = newExtractVariableRefactoring.getUnmatchedStatementReferences();
								ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
								oldExtractVariableRefactoring.addReferences(newReferences);
								oldExtractVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
								for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
									oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
								}
								break;
							}
						}
					}
				}
			}
		}
	}

	public int nonMappedElementsT2CallingAddedOperation(List<? extends VariableDeclarationContainer> addedOperations) {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
			if(composite.countableStatement()) {
				for(AbstractCall invocation : composite.getMethodInvocations()) {
					for(VariableDeclarationContainer operation : addedOperations) {
						if(invocation.matchesOperation(operation, container2, classDiff, modelDiff)) {
							nonMappedInnerNodeCount++;
							break;
						}
					}
				}
			}
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			if(statement.countableStatement()) {
				for(AbstractCall invocation : statement.getMethodInvocations()) {
					for(VariableDeclarationContainer operation : addedOperations) {
						if(invocation.matchesOperation(operation, container2, classDiff, modelDiff)) {
							nonMappedLeafCount++;
							break;
						}
					}
				}
			}
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int mappedElementsT2CallingAddedOperation(List<? extends VariableDeclarationContainer> addedOperations) {
		int mappedNodeCount = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment2().countableStatement()) {
				for(AbstractCall invocation : mapping.getFragment2().getMethodInvocations()) {
					for(VariableDeclarationContainer operation : addedOperations) {
						if(invocation.matchesOperation(operation, container2, classDiff, modelDiff)) {
							mappedNodeCount++;
							break;
						}
					}
				}
			}
		}
		return mappedNodeCount;
	}

	public List<VariableDeclarationContainer> addedOperationsCalledByMappedElementsT2(List<? extends VariableDeclarationContainer> addedOperations) {
		List<VariableDeclarationContainer> calledOperations = new ArrayList<>();
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment2().countableStatement()) {
				for(AbstractCall invocation : mapping.getFragment2().getMethodInvocations()) {
					for(VariableDeclarationContainer operation : addedOperations) {
						if(invocation.matchesOperation(operation, container2, classDiff, modelDiff)) {
							calledOperations.add(operation);
							break;
						}
					}
				}
			}
		}
		return calledOperations;
	}

	public int nonMappedElementsT1CallingRemovedOperation(List<? extends VariableDeclarationContainer> removedOperations) {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
			if(composite.countableStatement()) {
				for(AbstractCall invocation : composite.getMethodInvocations()) {
					for(VariableDeclarationContainer operation : removedOperations) {
						if(invocation.matchesOperation(operation, container1, classDiff, modelDiff)) {
							nonMappedInnerNodeCount++;
							break;
						}
					}
				}
			}
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			if(statement.countableStatement()) {
				for(AbstractCall invocation : statement.getMethodInvocations()) {
					for(VariableDeclarationContainer operation : removedOperations) {
						if(invocation.matchesOperation(operation, container1, classDiff, modelDiff)) {
							nonMappedLeafCount++;
							break;
						}
					}
				}
			}
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int exactMatches() {
		int count = 0;
		Set<LeafMapping> subExpressionMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			subExpressionMappings.addAll(mapping.getSubExpressionMappings());
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals(JAVA.TRY) && !subExpressionMappings.contains(mapping))
				count++;
		}
		return count;
	}

	public List<AbstractCodeMapping> getExactMatches() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		Set<LeafMapping> subExpressionMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			subExpressionMappings.addAll(mapping.getSubExpressionMappings());
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals(JAVA.TRY) && !subExpressionMappings.contains(mapping))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	public List<AbstractCodeMapping> getExactMatchesIncludingVariableRenames() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		Set<LeafMapping> subExpressionMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			subExpressionMappings.addAll(mapping.getSubExpressionMappings());
			if((mapping.isExact() || mapping.containsOnlyReplacement(ReplacementType.VARIABLE_NAME)) && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals(JAVA.TRY) && !subExpressionMappings.contains(mapping))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	public List<AbstractCodeMapping> getExactMatchesWithoutLoggingStatements() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		Set<LeafMapping> subExpressionMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			subExpressionMappings.addAll(mapping.getSubExpressionMappings());
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals(JAVA.TRY) && !subExpressionMappings.contains(mapping)) {
				boolean logCallFound = false;
				for(AbstractCall call : mapping.getFragment1().getMethodInvocations()) {
					if(call.isLog() || call.isLogGuard()) {
						logCallFound = true;
					}
				}
				if(!logCallFound) {
					exactMatches.add(mapping);
				}
			}
		}
		return exactMatches;
	}

	public List<AbstractCodeMapping> getExactMatchesWithoutMatchesInNestedContainers() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		Set<LeafMapping> subExpressionMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			subExpressionMappings.addAll(mapping.getSubExpressionMappings());
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals(JAVA.TRY) && !subExpressionMappings.contains(mapping) && mapping.getOperation1().equals(this.container1) && mapping.getOperation2().equals(this.container2))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	public boolean allMappingsHaveSameDepthAndIndex() {
		if(this.nonMappedInnerNodesT1.isEmpty() && this.nonMappedInnerNodesT2.isEmpty() &&
				this.nonMappedLeavesT1.isEmpty() && this.nonMappedLeavesT2.isEmpty()) {
			int count = 0;
			int compositeCount = 0;
			int identicalCompositeCount = 0;
			int identicalLeafCount = 0;
			for(AbstractCodeMapping mapping : mappings) {
				AbstractCodeFragment f1 = mapping.getFragment1();
				AbstractCodeFragment f2 = mapping.getFragment2();
				if(f1.getDepth() == f2.getDepth() && f1.getIndex() == f2.getIndex()) {
					count++;
					if(mapping instanceof CompositeStatementObjectMapping) {
						compositeCount++;
						if(f1.getString().equals(f2.getString())) {
							identicalCompositeCount++;
						}
					}
					else if(f1 instanceof StatementObject && f2 instanceof StatementObject) {
						if(f1.getString().equals(f2.getString())) {
							identicalLeafCount++;
						}
					}
				}
			}
			if(count == mappings.size() && compositeCount == identicalCompositeCount && compositeCount > 0 && identicalLeafCount > 0) {
				return true;
			}
		}
		return false;
	}

	public boolean allMappingsAreIdentical() {
		int identical = 0;
		for(AbstractCodeMapping mapping : this.getMappings()) {
			if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				identical++;
			}
			else {
				break;
			}
		}
		return identical == this.getMappings().size() && identical > 0;
	}

	public boolean allMappingsAreExactMatches() {
		int mappings = this.mappingsWithoutBlocks();
		int tryMappings = 0;
		int mappingsWithTypeReplacement = 0;
		int mappingsWithVariableReplacement = 0;
		int mappingsWithMethodInvocationRename = 0;
		for(AbstractCodeMapping mapping : this.getMappings()) {
			if(mapping.getFragment1().getString().equals(JAVA.TRY) && mapping.getFragment2().getString().equals(JAVA.TRY)) {
				tryMappings++;
			}
			if(mapping.containsOnlyReplacement(ReplacementType.TYPE)) {
				mappingsWithTypeReplacement++;
			}
			if(mapping.containsOnlyReplacement(ReplacementType.VARIABLE_NAME)) {
				mappingsWithVariableReplacement++;
			}
			if(mapping.containsOnlyReplacement(ReplacementType.METHOD_INVOCATION_NAME)) {
				mappingsWithMethodInvocationRename++;
			}
			if(mapping.getReplacements().size() == 2 && mapping.containsReplacement(ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT) &&
					mapping.containsReplacement(ReplacementType.VARIABLE_NAME)) {
				AbstractCall call1 = mapping.getFragment1().invocationCoveringEntireFragment();
				AbstractCall call2 = mapping.getFragment2().invocationCoveringEntireFragment();
				if(call1 != null && call2 != null) {
					for(Replacement r : mapping.getReplacements()) {
						if(r.getType().equals(ReplacementType.VARIABLE_NAME) && call1.arguments().contains(r.getBefore()) && call2.arguments().contains(r.getAfter())) {
							mappingsWithMethodInvocationRename++;
							break;
						}
					}
				}
			}
		}
		if(mappings == this.exactMatches() + tryMappings) {
			return true;
		}
		if(mappings == this.exactMatches() + tryMappings + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement) {
			return true;
		}
		if(mappings == this.exactMatches() + tryMappings + mappingsWithVariableReplacement && mappings > mappingsWithVariableReplacement) {
			return true;
		}
		if(mappings == this.exactMatches() + tryMappings + mappingsWithMethodInvocationRename && mappings > mappingsWithMethodInvocationRename) {
			return true;
		}
		return false;
	}

	private int editDistance() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				continue;
			}
			String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
			String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
			if(!s1.equals(s2)) {
				count += StringDistance.editDistance(s1, s2);
			}
		}
		return count;
	}

	public double normalizedEditDistance() {
		double editDistance = 0;
		double maxLength = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable() ||
					mapping.containsReplacement(ReplacementType.METHOD_INVOCATION_EXPRESSION_ARGUMENT_SWAPPED)) {
				continue;
			}
			String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
			String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
			if(!s1.equals(s2)) {
				editDistance += StringDistance.editDistance(s1, s2);
				maxLength += Math.max(s1.length(), s2.length());
			}
		}
		if(maxLength == 0)
			return 0;
		return editDistance/maxLength;
	}

	public int operationNameEditDistance() {
		return StringDistance.editDistance(this.container1.getName(), this.container2.getName());
	}

	public int packageNameEditDistance() {
		String className1 = this.container1.getClassName();
		String className2 = this.container2.getClassName();
		if(className1 != null && className2 != null) {
			if(className1.contains(".") && className2.contains(".")) {
				String packageName1 = className1.substring(0, className1.lastIndexOf("."));
				String packageName2 = className2.substring(0, className2.lastIndexOf("."));
				return StringDistance.editDistance(packageName1, packageName2);
			}
			return StringDistance.editDistance(className1, className2);
		}
		return Integer.MAX_VALUE;
	}

	public Set<Replacement> getReplacements() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			replacements.addAll(mapping.getReplacements());
		}
		return replacements;
	}

	public List<Set<AbstractCodeMapping>> getInternalParameterizeTestMultiMappings() {
		return internalParameterizeTestMultiMappings;
	}

	public int countMappingsForInternalParameterizedTest() {
		if(this.internalParameterizeTestMultiMappings.size() > 0) {
			int count = 0;
			Set<AbstractCodeMapping> processedMappings = new LinkedHashSet<AbstractCodeMapping>();
			for(Set<AbstractCodeMapping> set : internalParameterizeTestMultiMappings) {
				count++;
				processedMappings.addAll(set);
			}
			for(AbstractCodeMapping mapping : getMappings()) {
				if(!processedMappings.contains(mapping) && !(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)) {
					count++;
				}
			}
			return count;
		}
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(!(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)) {
				count++;
			}
		}
		return count;
	}

	public int countReplacementsForInternalParameterizedTest() {
		if(this.internalParameterizeTestMultiMappings.size() > 0) {
			int count = 0;
			Set<AbstractCodeMapping> processedMappings = new LinkedHashSet<AbstractCodeMapping>();
			for(Set<AbstractCodeMapping> set : internalParameterizeTestMultiMappings) {
				int localCount = 0;
				for(AbstractCodeMapping mapping : set) {
					localCount += mapping.getReplacements().size();
				}
				count += localCount/set.size();
				processedMappings.addAll(set);
			}
			for(AbstractCodeMapping mapping : getMappings()) {
				if(!processedMappings.contains(mapping)) {
					count += mapping.getReplacements().size();
				}
			}
			return count;
		}
		return getReplacements().size();
	}

	public Set<Replacement> getReplacementsOfType(ReplacementType type) {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement r : mapping.getReplacements()) {
				if(r.getType().equals(type)) {
					replacements.add(r);
				}
			}
		}
		return replacements;
	}

	public boolean containsLambdaMapper() {
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getLambdaMappers().size() > 0 && !mapping.getFragment1().getString().equals(mapping.getFragment2().getString()))
				return true;
		}
		return false;
	}

	public boolean containsCompositeMappingWithoutReplacements() {
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().equalFragment(mapping.getFragment2()) && mapping.getReplacements().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public Set<Replacement> getReplacementsInvolvingMethodInvocationForInline() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			Set<Replacement> replacementsInvolvingMethodInvocation = mapping.getReplacementsInvolvingMethodInvocation();
			for(Replacement r : replacementsInvolvingMethodInvocation) {
				if(r instanceof MethodInvocationReplacement) {
					AbstractCall before = ((MethodInvocationReplacement)r).getInvokedOperationBefore();
					AbstractCall after = ((MethodInvocationReplacement)r).getInvokedOperationAfter();
					if(!before.identicalName(after)) {
						replacements.add(r);
					}
					else if(before.arguments().size() != after.arguments().size()) {
						replacements.add(r);
					}
				}
				else {
					replacements.add(r);
				}
			}
			if(replacementsInvolvingMethodInvocation.isEmpty() && !mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				AbstractCall invocationCoveringEntireFragment1 = mapping.getFragment1().invocationCoveringEntireFragment();
				AbstractCall invocationCoveringEntireFragment2 = mapping.getFragment2().invocationCoveringEntireFragment();
				if(invocationCoveringEntireFragment1 != null && invocationCoveringEntireFragment2 != null) {
					String expression1 = invocationCoveringEntireFragment1.getExpression();
					String expression2 = invocationCoveringEntireFragment2.getExpression();
					if(expression1 != null && expression2 != null) {
						for(Replacement r : mapping.getReplacements()) {
							if(r.getBefore().equals(expression1) && r.getAfter().equals(expression2)) {
								if(Character.isUpperCase(expression1.charAt(0)) && Character.isUpperCase(expression2.charAt(0))) {
									replacements.add(r);
								}
							}
						}
					}
				}
				List<AbstractCall> methodInvocations1 = mapping.getFragment1().getMethodInvocations();
				List<AbstractCall> methodInvocations2 = mapping.getFragment2().getMethodInvocations();
				if(methodInvocations1.size() == methodInvocations2.size()) {
					for(int i=0; i<methodInvocations1.size(); i++) {
						AbstractCall call1 = methodInvocations1.get(i);
						AbstractCall call2 = methodInvocations2.get(i);
						if(!call1.equals(call2)) {
							MethodInvocationReplacement r = new MethodInvocationReplacement(call1.actualString(), call2.actualString(), call1, call2, ReplacementType.METHOD_INVOCATION);
							replacements.add(r);
						}
					}
				}
				List<LambdaExpressionObject> lambdas1 = mapping.getFragment1().getLambdas();
				List<LambdaExpressionObject> lambdas2 = mapping.getFragment2().getLambdas();
				if(lambdas1.size() == lambdas2.size()) {
					for(int j=0; j<lambdas1.size(); j++) {
						LambdaExpressionObject lambda1 = lambdas1.get(j);
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						methodInvocations1 = lambda1.getAllOperationInvocations();
						methodInvocations2 = lambda2.getAllOperationInvocations();
						if(methodInvocations1.size() == methodInvocations2.size()) {
							for(int i=0; i<methodInvocations1.size(); i++) {
								AbstractCall call1 = methodInvocations1.get(i);
								AbstractCall call2 = methodInvocations2.get(i);
								if(!call1.equals(call2)) {
									MethodInvocationReplacement r = new MethodInvocationReplacement(call1.actualString(), call2.actualString(), call1, call2, ReplacementType.METHOD_INVOCATION);
									replacements.add(r);
								}
							}
						}
					}
				}
			}
		}
		return replacements;
	}

	public Set<Replacement> getReplacementsInvolvingMethodInvocation() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			Set<Replacement> replacementsInvolvingMethodInvocation = mapping.getReplacementsInvolvingMethodInvocation();
			replacements.addAll(replacementsInvolvingMethodInvocation);
			if(replacementsInvolvingMethodInvocation.isEmpty() && !mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				AbstractCall invocationCoveringEntireFragment1 = mapping.getFragment1().invocationCoveringEntireFragment();
				AbstractCall invocationCoveringEntireFragment2 = mapping.getFragment2().invocationCoveringEntireFragment();
				if(invocationCoveringEntireFragment1 != null && invocationCoveringEntireFragment2 != null) {
					String expression1 = invocationCoveringEntireFragment1.getExpression();
					String expression2 = invocationCoveringEntireFragment2.getExpression();
					if(expression1 != null && expression2 != null) {
						for(Replacement r : mapping.getReplacements()) {
							if(r.getBefore().equals(expression1) && r.getAfter().equals(expression2)) {
								if(Character.isUpperCase(expression1.charAt(0)) && Character.isUpperCase(expression2.charAt(0))) {
									replacements.add(r);
								}
							}
						}
					}
				}
				List<AbstractCall> methodInvocations1 = mapping.getFragment1().getMethodInvocations();
				List<AbstractCall> methodInvocations2 = mapping.getFragment2().getMethodInvocations();
				if(methodInvocations1.size() == methodInvocations2.size()) {
					for(int i=0; i<methodInvocations1.size(); i++) {
						AbstractCall call1 = methodInvocations1.get(i);
						AbstractCall call2 = methodInvocations2.get(i);
						boolean unequalExpression = !call1.identicalExpression(call2) && call1.getExpression() != null && !call1.getExpression().equals(JAVA.THIS);
						boolean variableRenamed = false;
						if(unequalExpression) {
							for(Refactoring r : refactorings) {
								if(r instanceof ChangeVariableTypeRefactoring) {
									ChangeVariableTypeRefactoring change = (ChangeVariableTypeRefactoring)r;
									if(change.getOriginalVariable().getVariableName().equals(call1.getExpression()) && change.getChangedTypeVariable().getVariableName().equals(call2.getExpression())) {
										variableRenamed = true;
									}
								}
								else if(r instanceof RenameVariableRefactoring) {
									RenameVariableRefactoring change = (RenameVariableRefactoring)r;
									if(change.getOriginalVariable().getVariableName().equals(call1.getExpression()) && change.getRenamedVariable().getVariableName().equals(call2.getExpression())) {
										variableRenamed = true;
									}
								}
							}
						}
						if(!call1.equals(call2) || (unequalExpression && !variableRenamed)) {
							MethodInvocationReplacement r = new MethodInvocationReplacement(call1.actualString(), call2.actualString(), call1, call2, ReplacementType.METHOD_INVOCATION);
							replacements.add(r);
						}
					}
				}
				List<LambdaExpressionObject> lambdas1 = mapping.getFragment1().getLambdas();
				List<LambdaExpressionObject> lambdas2 = mapping.getFragment2().getLambdas();
				if(lambdas1.size() == lambdas2.size()) {
					for(int j=0; j<lambdas1.size(); j++) {
						LambdaExpressionObject lambda1 = lambdas1.get(j);
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						methodInvocations1 = lambda1.getAllOperationInvocations();
						methodInvocations2 = lambda2.getAllOperationInvocations();
						if(methodInvocations1.size() == methodInvocations2.size()) {
							for(int i=0; i<methodInvocations1.size(); i++) {
								AbstractCall call1 = methodInvocations1.get(i);
								AbstractCall call2 = methodInvocations2.get(i);
								if(!call1.equals(call2)) {
									MethodInvocationReplacement r = new MethodInvocationReplacement(call1.actualString(), call2.actualString(), call1, call2, ReplacementType.METHOD_INVOCATION);
									replacements.add(r);
								}
							}
						}
						else if(methodInvocations2.size() > methodInvocations1.size()) {
							List<AbstractCall> creations1 = lambda1.getAllCreations();
							List<AbstractCall> creations2 = lambda2.getAllCreations();
							if(creations2.isEmpty() && creations1.size() == 1) {
								for(int i=0; i<methodInvocations2.size(); i++) {
									AbstractCall call2 = methodInvocations2.get(i);
									if(!methodInvocations1.contains(call2)) {
										ClassInstanceCreationWithMethodInvocationReplacement r = new ClassInstanceCreationWithMethodInvocationReplacement(
												creations1.get(0).actualString(), call2.actualString(), creations1.get(0), call2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
										replacements.add(r);
									}
								}
							}
						}
					}
				}
			}
		}
		if(replacements.isEmpty()) {
			for(UMLOperationBodyMapper childMapper : childMappers) {
				for(AbstractCodeMapping mapping : childMapper.getMappings()) {
					replacements.addAll(mapping.getReplacementsInvolvingMethodInvocation());
				}
			}
		}
		return replacements;
	}

	public Set<MethodInvocationReplacement> getMethodInvocationRenameReplacements() {
		Set<MethodInvocationReplacement> replacements = new LinkedHashSet<MethodInvocationReplacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME) ||
						replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT) ||
						replacement.getType().equals(ReplacementType.METHOD_INVOCATION_ARGUMENT)) {
					replacements.add((MethodInvocationReplacement) replacement);
				}
				if(replacement.getType().equals(ReplacementType.METHOD_INVOCATION)) {
					MethodInvocationReplacement invocationReplacement = (MethodInvocationReplacement) replacement;
					AbstractCall callBefore = invocationReplacement.getInvokedOperationBefore();
					AbstractCall callAfter = invocationReplacement.getInvokedOperationAfter();
					if(callBefore.getExpression() != null && callAfter.getExpression() != null &&
							Character.isUpperCase(callBefore.getExpression().charAt(0)) &&
							Character.isUpperCase(callAfter.getExpression().charAt(0))) {
						replacements.add(invocationReplacement);
					}
				}
			}
		}
		return replacements;
	}

	public boolean involvesConstructors() {
		return container1.isConstructor() && container2.isConstructor();
	}

	public boolean involvesTestMethods() {
		return container1.hasTestAnnotation() && container2.hasTestAnnotation();
	}

	public boolean involvesSetUpMethods() {
		return container1.hasSetUpAnnotation() && container2.hasSetUpAnnotation();
	}

	public boolean involvesTearDownMethods() {
		return container1.hasTearDownAnnotation() && container2.hasTearDownAnnotation();
	}

	private void processInnerNodes(List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2,
			List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap, boolean containsCallToExtractedMethod) throws RefactoringMinerTimedOutException {
		List<CompositeStatementObject> blocks1 = new ArrayList<>();
		List<CompositeStatementObject> nonBlocks1 = new ArrayList<>();
		Map<String, List<CompositeStatementObject>> map1 = new LinkedHashMap<>();
		for(CompositeStatementObject innerNode : innerNodes1) {
			if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				blocks1.add(innerNode);
			}
			else {
				nonBlocks1.add(innerNode);
				String key = innerNode.getString();
				if(map1.containsKey(key)) {
					map1.get(key).add(innerNode);
				}
				else {
					List<CompositeStatementObject> list = new ArrayList<>();
					list.add(innerNode);
					map1.put(key, list);
				}
			}
		}
		List<CompositeStatementObject> blocks2 = new ArrayList<>();
		List<CompositeStatementObject> nonBlocks2 = new ArrayList<>();
		Map<String, List<CompositeStatementObject>> map2 = new LinkedHashMap<>();
		for(CompositeStatementObject innerNode : innerNodes2) {
			if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				blocks2.add(innerNode);
			}
			else {
				nonBlocks2.add(innerNode);
				String key = innerNode.getString();
				if(map2.containsKey(key)) {
					map2.get(key).add(innerNode);
				}
				else {
					List<CompositeStatementObject> list = new ArrayList<>();
					list.add(innerNode);
					map2.put(key, list);
				}
			}
		}
		List<UMLOperation> removedOperations = classDiff != null ? classDiff.getRemovedOperations() : new ArrayList<UMLOperation>();
		List<UMLOperation> addedOperations = classDiff != null ? classDiff.getAddedOperations() : new ArrayList<UMLOperation>();
		int tryWithResources1 = tryWithResourcesCount(innerNodes1);
		int tryWithResources2 = tryWithResourcesCount(innerNodes2);
		boolean tryWithResourceMigration = (tryWithResources1 == 0 && tryWithResources2 > 0) || (tryWithResources1 > 0 && tryWithResources2 == 0);
		processInnerNodes(nonBlocks1, nonBlocks2, leaves1, leaves2, parameterToArgumentMap, removedOperations, addedOperations, tryWithResourceMigration, containsCallToExtractedMethod, map1, map2);
		for(AbstractCodeMapping mapping : new LinkedHashSet<>(mappings)) {
			if(mapping.getFragment1() instanceof CompositeStatementObject && mapping.getFragment2() instanceof CompositeStatementObject &&
					(innerNodes1.contains(mapping.getFragment1()) || duplicateMapping1(mapping)) && (innerNodes2.contains(mapping.getFragment2()) || duplicateMapping2(mapping))) {
				CompositeStatementObject comp1 = (CompositeStatementObject) mapping.getFragment1();
				CompositeStatementObject comp2 = (CompositeStatementObject) mapping.getFragment2();
				innerNodes1.remove(comp1);
				innerNodes2.remove(comp2);
				if(comp1.getStatements().size() == 1 && comp2.getStatements().size() == 1) {
					AbstractStatement block1 = comp1.getStatements().get(0);
					AbstractStatement block2 = comp2.getStatements().get(0);
					if(blocks1.contains(block1) && blocks2.contains(block2)) {
						double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, Optional.empty(), removedOperations, addedOperations, tryWithResourceMigration);
						CompositeStatementObjectMapping newMapping = createCompositeMapping((CompositeStatementObject)block1, (CompositeStatementObject)block2, parameterToArgumentMap, score);
						addMapping(newMapping);
						innerNodes1.remove(block1);
						innerNodes2.remove(block2);
					}
				}
				else if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						comp1.getStatements().size() == 2 && comp2.getStatements().size() == 2) {
					for(int i=0; i<comp1.getStatements().size(); i++) {
						AbstractStatement block1 = comp1.getStatements().get(i);
						AbstractStatement block2 = comp2.getStatements().get(i);
						if(blocks1.contains(block1) && blocks2.contains(block2)) {
							double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, Optional.empty(), removedOperations, addedOperations, tryWithResourceMigration);
							CompositeStatementObjectMapping newMapping = createCompositeMapping((CompositeStatementObject)block1, (CompositeStatementObject)block2, parameterToArgumentMap, score);
							addMapping(newMapping);
							innerNodes1.remove(block1);
							innerNodes2.remove(block2);
						}
					}
				}
				else if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						comp1.getStatements().size() == 2 && comp2.getStatements().size() == 1 &&
						comp1.getString().equals(comp2.getString())) {
					AbstractStatement block1 = comp1.getStatements().get(0);
					AbstractStatement block2 = comp2.getStatements().get(0);
					if(blocks1.contains(block1) && blocks2.contains(block2)) {
						double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, Optional.empty(), removedOperations, addedOperations, tryWithResourceMigration);
						CompositeStatementObjectMapping newMapping = createCompositeMapping((CompositeStatementObject)block1, (CompositeStatementObject)block2, parameterToArgumentMap, score);
						addMapping(newMapping);
						innerNodes1.remove(block1);
						innerNodes2.remove(block2);
					}
				}
				else if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
						comp1.getStatements().size() == 1 && comp2.getStatements().size() == 2 &&
						comp1.getString().equals(comp2.getString())) {
					AbstractStatement block1 = comp1.getStatements().get(0);
					AbstractStatement block2 = comp2.getStatements().get(0);
					if(blocks1.contains(block1) && blocks2.contains(block2)) {
						double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, Optional.empty(), removedOperations, addedOperations, tryWithResourceMigration);
						CompositeStatementObjectMapping newMapping = createCompositeMapping((CompositeStatementObject)block1, (CompositeStatementObject)block2, parameterToArgumentMap, score);
						addMapping(newMapping);
						innerNodes1.remove(block1);
						innerNodes2.remove(block2);
					}
				}
			}
			else if(mapping.getFragment1().getLambdas().size() > 0 && mapping.getFragment2().getLambdas().size() > 0) {
				LambdaExpressionObject lambda1 = mapping.getFragment1().getLambdas().get(0);
				LambdaExpressionObject lambda2 = mapping.getFragment2().getLambdas().get(0);
				if(lambda1.getBody() != null && blocks1.contains(lambda1.getBody().getCompositeStatement()) &&
						lambda2.getBody() != null && blocks2.contains(lambda2.getBody().getCompositeStatement())) {
					double score = computeScore(lambda1.getBody().getCompositeStatement(), lambda2.getBody().getCompositeStatement(), Optional.empty(), removedOperations, addedOperations, tryWithResourceMigration);
					CompositeStatementObjectMapping newMapping = createCompositeMapping(lambda1.getBody().getCompositeStatement(), lambda2.getBody().getCompositeStatement(), parameterToArgumentMap, score);
					addMapping(newMapping);
					innerNodes1.remove(lambda1.getBody().getCompositeStatement());
					innerNodes2.remove(lambda2.getBody().getCompositeStatement());
				}
			}
		}
		boolean forEach1 = false;
		for(AbstractCodeFragment leaf1 : leaves1) {
			for(AbstractCall call : leaf1.getMethodInvocations()) {
				if(isForEach(call.getName())) {
					forEach1 = true;
					break;
				}
			}
		}
		boolean forEach2 = false;
		for(AbstractCodeFragment leaf2 : leaves2) {
			for(AbstractCall call : leaf2.getMethodInvocations()) {
				if(isForEach(call.getName())) {
					forEach2 = true;
					break;
				}
			}
		}
		Set<CompositeStatementObject> blocksOfUnmatchedNonBlocks1 = new LinkedHashSet<>();
		for(CompositeStatementObject nonBlock1 : nonBlocks1) {
			for(AbstractStatement statement : nonBlock1.getStatements()) {
				if(innerNodes1.contains(statement)) {
					CompositeStatementObject compStatement = (CompositeStatementObject) statement;
					boolean skip = false;
					if(compStatement.getParent().isLoop() && forEach2) {
						skip = true;
					}
					if(compStatement.getStatements().isEmpty()) {
						skip = true;
					}
					if(!skip && parentMapper != null) {
						for(AbstractCodeMapping mapping : parentMapper.mappings) {
							if(mapping.isExact() && compStatement.getStatements().contains(mapping.getFragment1())) {
								skip = true;
								break;
							}
						}
					}
					if(!skip) {
						for(AbstractCodeMapping mapping : this.mappings) {
							if(compStatement.getStatements().contains(mapping.getFragment1())) {
								skip = true;
								break;
							}
						}
					}
					if(!skip) {
						blocksOfUnmatchedNonBlocks1.add(compStatement);
					}
				}
			}
		}
		Set<CompositeStatementObject> blocksOfUnmatchedNonBlocks2 = new LinkedHashSet<>();
		for(CompositeStatementObject nonBlock2 : nonBlocks2) {
			for(AbstractStatement statement : nonBlock2.getStatements()) {
				if(innerNodes2.contains(statement)) {
					CompositeStatementObject compStatement = (CompositeStatementObject) statement;
					boolean skip = false;
					if(compStatement.getParent().isLoop() && forEach1) {
						skip = true;
					}
					if(compStatement.getStatements().isEmpty()) {
						skip = true;
					}
					for(AbstractStatement nestedStatement : compStatement.getStatements()) {
						AbstractCall call = nestedStatement.invocationCoveringEntireFragment();
						if(call == null) {
							call = nestedStatement.assignmentInvocationCoveringEntireStatement();
						}
						if(call != null && call.getName().equals("apply")) {
							skip = true;
							break;
						}
					}
					if(!skip && parentMapper != null) {
						for(AbstractCodeMapping mapping : parentMapper.mappings) {
							if(mapping.isExact() && compStatement.getStatements().contains(mapping.getFragment2())) {
								skip = true;
								break;
							}
						}
					}
					if(!skip) {
						for(AbstractCodeMapping mapping : this.mappings) {
							if(compStatement.getStatements().contains(mapping.getFragment2())) {
								skip = true;
								break;
							}
						}
					}
					if(!skip) {
						blocksOfUnmatchedNonBlocks2.add(compStatement);
					}
				}
			}
		}
		ArrayList<CompositeStatementObject> finalInnerNodes1 = new ArrayList<>(innerNodes1);
		finalInnerNodes1.removeAll(blocksOfUnmatchedNonBlocks1);
		ArrayList<CompositeStatementObject> finalInnerNodes2 = new ArrayList<>(innerNodes2);
		finalInnerNodes2.removeAll(blocksOfUnmatchedNonBlocks2);
		int numberOfMappings = mappings.size();
		processInnerNodes(finalInnerNodes1, finalInnerNodes2, leaves1, leaves2, parameterToArgumentMap, removedOperations, addedOperations, tryWithResourceMigration, containsCallToExtractedMethod, map1, map2);
		List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
		for(int i = numberOfMappings; i < mappings.size(); i++) {
			innerNodes1.remove(mappings.get(i).getFragment1());
			innerNodes2.remove(mappings.get(i).getFragment2());
		}
	}

	private boolean isInMergeConditionalRefactoring(CompositeStatementObject innerNode1) {
		for(Refactoring r : refactorings) {
			if(r instanceof MergeConditionalRefactoring) {
				MergeConditionalRefactoring merge = (MergeConditionalRefactoring)r;
				if(merge.getMergedConditionals().contains(innerNode1)) {
					for(LeafMapping leafMapping : merge.getSubExpressionMappings()) {
						if(innerNode1.getExpressions().size() > 0 && innerNode1.getExpressions().get(0).getString().equals(leafMapping.getFragment1().getString()) &&
								leafMapping.getFragment1().getString().equals(leafMapping.getFragment2().getString())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void processInnerNodes(List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2, List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap, List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration, boolean containsCallToExtractedMethod,
			Map<String, List<CompositeStatementObject>> map1, Map<String, List<CompositeStatementObject>> map2) throws RefactoringMinerTimedOutException {
		boolean sameNumberOfInnerNodesInMultiCalledExtractedMethod = innerNodes1.size() == innerNodes2.size() && multipleCallsToExtractedOrInlinedMethod();
		if(innerNodes1.size() <= innerNodes2.size() && !sameNumberOfInnerNodesInMultiCalledExtractedMethod) {
			//exact string matching - inner nodes - finds moves to another level
			Set<CompositeStatementObject> innerNodes1ToBeRemoved = new LinkedHashSet<>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject statement1 = innerNodeIterator1.next();
				if(!alreadyMatched1(statement1)) {
					List<CompositeStatementObject> matchingInnerNodes1 = map1.get(statement1.getString());
					if(matchingInnerNodes1 == null) {
						matchingInnerNodes1 = Collections.emptyList();
					}
					List<CompositeStatementObject> matchingInnerNodes2 = map2.get(statement1.getString());
					if(matchingInnerNodes2 == null) {
						matchingInnerNodes2 = Collections.emptyList();
					}
					if(matchingInnerNodes1.size() > matchingInnerNodes2.size() && matchingInnerNodes2.size() > 0) {
						int numberOfMappings = mappings.size();
						processInnerNodes(matchingInnerNodes1, matchingInnerNodes2, leaves1, leaves2, parameterToArgumentMap, removedOperations, addedOperations, tryWithResourceMigration, containsCallToExtractedMethod, map1, map2);
						List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
						for(int i = numberOfMappings; i < mappings.size(); i++) {
							AbstractCodeMapping mapping = mappings.get(i);
							if(mapping instanceof CompositeStatementObjectMapping) {
								innerNodes2.remove(mapping.getFragment2());
								innerNodes1ToBeRemoved.add((CompositeStatementObject)mapping.getFragment1());
							}
						}
						continue;
					}
					TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
					for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
						CompositeStatementObject statement2 = innerNodeIterator2.next();
						if(!alreadyMatched2(statement2)) {
							if(statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString()) || differOnlyInThis(statement1.getString(), statement2.getString())) {
								ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, matchingInnerNodes1, matchingInnerNodes2);
								double score = computeScore(statement1, statement2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
								if(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
									mappingSet.add(mapping);
								}
							}
							else if(checkForAlternativeTryOrSynchronizedBlocks(statement1, statement2, tryWithResourceMigration)) {
								List<AbstractCodeFragment> allUnmatchedNodes1 = new ArrayList<>();
								allUnmatchedNodes1.addAll(innerNodes1);
								allUnmatchedNodes1.addAll(leaves1);
								List<AbstractCodeFragment> allUnmatchedNodes2 = new ArrayList<>();
								allUnmatchedNodes2.addAll(innerNodes2);
								allUnmatchedNodes2.addAll(leaves2);
								ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, allUnmatchedNodes1, allUnmatchedNodes2);
								findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo, false, this);
								double score = computeScore(statement1, statement2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
								if(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
									mappingSet.add(mapping);
								}
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						Set<AbstractCodeMapping> movedInIfElseBranch = movedInIfElseIfBranch(mappingSet);
						if(movedInIfElseBranch.size() > 1 && multiMappingCondition(matchingInnerNodes1, matchingInnerNodes2)) {
							for(AbstractCodeMapping mapping : movedInIfElseBranch) {
								addMapping(mapping);
								innerNodes2.remove(mapping.getFragment2());
							}
							innerNodeIterator1.remove();
						}
						else {
							CompositeStatementObjectMapping oneTryBlockNestedUnderTheOther = oneTryBlockNestedUnderTheOther(mappingSet);
							Map<CompositeStatementObjectMapping, Boolean> mappingExistsIdenticalInExtractedMethod = new LinkedHashMap<>();
							if(parentMapper == null && containsCallToExtractedMethod && mappingSet.size() > 1) {
								for(CompositeStatementObjectMapping mapping : mappingSet) {
									mappingExistsIdenticalInExtractedMethod.put(mapping, mappingExistsIdenticalInExtractedMethod(mapping, leaves2, addedOperations));
								}
							}
							CompositeStatementObjectMapping minStatementMapping = null;
							if(oneTryBlockNestedUnderTheOther != null) {
								minStatementMapping = oneTryBlockNestedUnderTheOther;
							}
							else if(mappingExistsIdenticalInExtractedMethod.containsKey(mappingSet.first()) && mappingExistsIdenticalInExtractedMethod.get(mappingSet.first())) {
								for(CompositeStatementObjectMapping mapping : mappingSet) {
									if(!mappingExistsIdenticalInExtractedMethod.get(mapping)) {
										minStatementMapping = mapping;
										break;
									}
								}
								if(minStatementMapping == null) {
									minStatementMapping = mappingSet.first();
								}
							}
							else {
								minStatementMapping = mappingSet.first();
							}
							addMapping(minStatementMapping);
							innerNodes2.remove(minStatementMapping.getFragment2());
							innerNodeIterator1.remove();
						}
					}
				}
			}
			innerNodes1.removeAll(innerNodes1ToBeRemoved);
			
			// exact matching - inner nodes - with variable renames
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject statement1 = innerNodeIterator1.next();
				if(!alreadyMatched1(statement1)) {
					if(isInMergeConditionalRefactoring(statement1)) {
						continue;
					}
					List<CompositeStatementObject> matchingInnerNodes1 = map1.get(statement1.getString());
					if(matchingInnerNodes1 == null) {
						matchingInnerNodes1 = Collections.emptyList();
					}
					TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
					for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
						CompositeStatementObject statement2 = innerNodeIterator2.next();
						if(!alreadyMatched2(statement2)) {
							List<AbstractCodeFragment> allUnmatchedNodes1 = new ArrayList<>();
							allUnmatchedNodes1.addAll(innerNodes1);
							allUnmatchedNodes1.addAll(leaves1);
							List<AbstractCodeFragment> allUnmatchedNodes2 = new ArrayList<>();
							allUnmatchedNodes2.addAll(innerNodes2);
							allUnmatchedNodes2.addAll(leaves2);
							ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, allUnmatchedNodes1, allUnmatchedNodes2);
							Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo, false, this);
							
							double score = computeScore(statement1, statement2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
							if(score == 0 && replacements != null) {
								if(replacements.size() == 1 && (replacementInfo.getReplacements(ReplacementType.INFIX_OPERATOR).size() > 0 || replacementInfo.getReplacements(ReplacementType.INVERT_CONDITIONAL).size() > 0)) {
									//special handling when there is only an infix operator or invert conditional replacement, but no children mapped
									score = 0.99;
								}
								else if(replacements.size() <= 2 && replacementInfo.getReplacements(ReplacementType.INVERT_CONDITIONAL).size() > 0) {
									score = 0.99;
								}
								else if(containsInvertCondition(statement1, statement2)) {
									score = 0.99;
								}
								else if(replacementInfo.getReplacements(ReplacementType.COMPOSITE).size() > 0) {
									score = 0.99;
								}
								else if(replacementInfo.getReplacements(ReplacementType.PATTERN_INSTANCE).size() > 0) {
									score = 0.99;
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
									//find if the corresponding try blocks are already matched
									for(AbstractCodeMapping mapping : mappings) {
										if(mapping.getFragment1() instanceof TryStatementObject && mapping.getFragment2() instanceof TryStatementObject) {
											TryStatementObject try1 = (TryStatementObject)mapping.getFragment1();
											TryStatementObject try2 = (TryStatementObject)mapping.getFragment2();
											if(try1.getCatchClauses().contains(statement1) && try2.getCatchClauses().contains(statement2)) {
												int count = 0;
												if(try1.getCatchClauses().size() == try2.getCatchClauses().size()) {
													for(int i=0; i<try1.getCatchClauses().size(); i++) {
														CompositeStatementObject catch1 = try1.getCatchClauses().get(i);
														CompositeStatementObject catch2 = try2.getCatchClauses().get(i);
														List<VariableDeclaration> variableDeclarations1 = catch1.getVariableDeclarations();
														List<VariableDeclaration> variableDeclarations2 = catch2.getVariableDeclarations();
														if(variableDeclarations1.size() > 0 && variableDeclarations1.size() == variableDeclarations2.size()) {
															VariableDeclaration v1 = variableDeclarations1.get(0);
															VariableDeclaration v2 = variableDeclarations2.get(0);
															if(v1.getType().equals(v2.getType())) {
																count++;
															}
														}
													}
												}
												boolean equalNumberOfCatchClausesWithSameExceptionTypes = count == try1.getCatchClauses().size();
												if(replacements.isEmpty() || (try1.getCatchClauses().size() == 1 && try2.getCatchClauses().size() == 1) || equalNumberOfCatchClausesWithSameExceptionTypes) {
													score = 0.99;
												}
												break;
											}
										}
									}
								}
								else if(identicalCommentsInBody(statement1, statement2)) {
									score = 0.99;
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
										statement1.getVariableDeclarations().toString().equals(statement2.getVariableDeclarations().toString())) {
									score = 0.99;
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
									List<AbstractExpression> expressions2 = statement2.getExpressions();
									AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
									for(AbstractExpression expression1 : statement1.getExpressions()) {
										if(expression1.getString().contains(enhancedForExpression.getString() + ".length") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".size()") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".iterator()") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".listIterator()") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".descendingIterator()")) {
											score = 0.99;
											break;
										}
									}
								}
								else if(replacements.size() == 1 && replacementInfo.getReplacements(ReplacementType.TYPE).size() > 0) {
									List<Replacement> typeReplacements = replacementInfo.getReplacements(ReplacementType.TYPE);
									Replacement typeReplacement = typeReplacements.get(0);
									if(typeReplacement.getBefore().startsWith(typeReplacement.getAfter() + "<") || typeReplacement.getAfter().startsWith(typeReplacement.getBefore() + "<")) {
										score = 0.99;
									}
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										statement1.getParent() != null && statement2.getParent() != null &&
										statement1.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										statement2.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										((alreadyMatched1(statement1.getParent()) && alreadyMatched2(statement2.getParent())) ||
												compositeChildMatchingScore(statement1.getParent(), statement2.getParent(), Optional.of(replacementInfo), mappings, removedOperations, addedOperations) > 0)) {
									score = 0.99;
								}
								if(score == 0.99) {
									for(CompositeStatementObject matchingInnerNode1 : matchingInnerNodes1) {
										if(!matchingInnerNode1.equals(statement1)) {
											ReplacementInfo replacementInfo2 = initializeReplacementInfo(matchingInnerNode1, statement2, allUnmatchedNodes1, allUnmatchedNodes2);
											Set<Replacement> replacements2 = findReplacementsWithExactMatching(matchingInnerNode1, statement2, parameterToArgumentMap, replacementInfo2, false, this);
											
											double score2 = computeScore(matchingInnerNode1, statement2, Optional.of(replacementInfo2), removedOperations, addedOperations, tryWithResourceMigration);
											if(score2 > 0) {
												CompositeStatementObjectMapping mapping = createCompositeMapping(matchingInnerNode1, statement2, parameterToArgumentMap, score2);
												mapping.addReplacements(replacements2);
												mapping.addLambdaMappers(replacementInfo2.getLambdaMappers());
												mapping.addSubExpressionMappings(replacementInfo2.getSubExpressionMappings());
												mappingSet.add(mapping);
											}
										}
									}
								}
							}
							if((replacements != null || identicalBody(statement1, statement2) || allLeavesWithinBodyMapped(statement1, statement2)) &&
									(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
								CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
								if(replacements == null && statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
										statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
									if(statement1.getExpressions().size() == statement2.getExpressions().size()) {
										for(int i=0; i<statement1.getExpressions().size(); i++) {
											Replacement r = new Replacement(statement1.getExpressions().get(i).getString(), statement2.getExpressions().get(i).getString(), ReplacementType.ARGUMENT);
											mapping.addReplacement(r);
										}
									}
								}
								mapping.addReplacements(replacements);
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
								for(AbstractCodeFragment leaf : leaves2) {
									if(leaf.getLocationInfo().before(mapping.getFragment2().getLocationInfo())) {
										mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null, mappings);
										if(mapping.isIdenticalWithExtractedVariable()) {
											List<LambdaExpressionObject> lambdas1 = mapping.getFragment1().getLambdas();
											List<LambdaExpressionObject> lambdas2 = leaf.getLambdas();
											if(lambdas1.size() == lambdas2.size()) {
												for(int i=0; i<lambdas1.size(); i++) {
													LambdaExpressionObject lambda1 = lambdas1.get(i);
													LambdaExpressionObject lambda2 = lambdas2.get(i);
													processLambdas(lambda1, lambda2, replacementInfo, this);
												}
											}
											break;
										}
									}
								}
								for(AbstractExpression expression : statement2.getExpressions()) {
									if(expression.getPatternInstanceofExpressions().size() > 0) {
										mapping.temporaryVariableAssignment(expression, leaves2, classDiff, parentMapper != null, mappings);
									}
								}
								for(AbstractCodeFragment leaf : leaves1) {
									if(leaf.getLocationInfo().before(mapping.getFragment1().getLocationInfo())) {
										mapping.inlinedVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
										if(mapping.isIdenticalWithInlinedVariable()) {
											List<LambdaExpressionObject> lambdas1 = leaf.getLambdas();
											List<LambdaExpressionObject> lambdas2 = mapping.getFragment2().getLambdas();
											if(lambdas1.size() == lambdas2.size()) {
												for(int i=0; i<lambdas1.size(); i++) {
													LambdaExpressionObject lambda1 = lambdas1.get(i);
													LambdaExpressionObject lambda2 = lambdas2.get(i);
													processLambdas(lambda1, lambda2, replacementInfo, this);
												}
											}
											break;
										}
									}
								}
								mappingSet.add(mapping);
							}
							else if(replacements == null && !containsCallToExtractedMethod && statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType()) && score > 0 && mappingSet.size() > 0 && score >= 2.0*mappingSet.first().getCompositeChildMatchingScore()) {
								CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								mappingSet.add(mapping);
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						if(!duplicateMappingInParentMapper(mappingSet)) {
							CompositeStatementObjectMapping oneTryBlockNestedUnderTheOther = oneTryBlockNestedUnderTheOther(mappingSet);
							CompositeStatementObjectMapping minStatementMapping = oneTryBlockNestedUnderTheOther != null ? oneTryBlockNestedUnderTheOther : mappingSet.first();
							addMapping(minStatementMapping);
							innerNodes2.remove(minStatementMapping.getFragment2());
							innerNodeIterator1.remove();
						}
					}
				}
			}
		}
		else {
			AbstractCodeMapping parentMapping = null;
			if(parentMapper != null && operationInvocation != null) {
				parentMapping = findParentMappingContainingOperationInvocation();
			}
			AbstractCodeMapping startMapping = null;
			AbstractCodeMapping endMapping = null;
			Set<VariableDeclaration> referencedVariableDeclarations1 = new LinkedHashSet<>();
			Set<VariableDeclaration> referencedVariableDeclarations2 = new LinkedHashSet<>();
			if(parentMapper != null) {
				for(AbstractCodeMapping mapping : this.mappings) {
					if(startMapping == null) {
						startMapping = mapping;
					}
					else if(mapping.getFragment1().getLocationInfo().getStartLine() < startMapping.getFragment1().getLocationInfo().getStartLine() &&
							mapping.getFragment2().getLocationInfo().getStartLine() < startMapping.getFragment2().getLocationInfo().getStartLine()) {
						startMapping = mapping;
					}
					if(endMapping == null) {
						endMapping = mapping;
					}
					else if(mapping.getFragment1().getLocationInfo().getStartLine() > endMapping.getFragment1().getLocationInfo().getStartLine() &&
							mapping.getFragment2().getLocationInfo().getStartLine() > endMapping.getFragment2().getLocationInfo().getStartLine()) {
						endMapping = mapping;
					}
				}
				if(startMapping != null && endMapping != null && startMapping.equals(endMapping)) {
					List<VariableDeclaration> variableDeclarationsInScope1 = container1.getVariableDeclarationsInScope(startMapping.getFragment1().getLocationInfo());
					for(LeafExpression variable : startMapping.getFragment1().getVariables()) {
						for(VariableDeclaration variableDeclaration : variableDeclarationsInScope1) {
							if(variable.getString().equals(variableDeclaration.getVariableName())) {
								referencedVariableDeclarations1.add(variableDeclaration);
							}
						}
					}
					List<VariableDeclaration> variableDeclarationsInScope2 = container2.getVariableDeclarationsInScope(startMapping.getFragment2().getLocationInfo());
					for(LeafExpression variable : startMapping.getFragment2().getVariables()) {
						for(VariableDeclaration variableDeclaration : variableDeclarationsInScope2) {
							if(variable.getString().equals(variableDeclaration.getVariableName())) {
								referencedVariableDeclarations2.add(variableDeclaration);
							}
						}
					}
				}
				if(this.mappings.isEmpty() && operationInvocation != null && parentMapping == null) {
					AbstractCodeFragment statementContainingOperationInvocation = null;
					for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
						if(mapping instanceof LeafMapping) {
							if(mapping.getFragment2().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
								statementContainingOperationInvocation = mapping.getFragment2();
							}
						}
						if(statementContainingOperationInvocation != null && mapping.getFragment2().equals(statementContainingOperationInvocation)) {
							startMapping = mapping;
							endMapping = mapping;
							break;
						}
					}
				}
			}
			//exact string matching - inner nodes - finds moves to another level
			Set<CompositeStatementObject> innerNodes2ToBeRemoved = new LinkedHashSet<>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				if(!alreadyMatched2(statement2)) {
					List<CompositeStatementObject> matchingInnerNodes1 = map1.get(statement2.getString());
					if(matchingInnerNodes1 == null) {
						matchingInnerNodes1 = Collections.emptyList();
					}
					List<CompositeStatementObject> matchingInnerNodes2 = map2.get(statement2.getString());
					if(matchingInnerNodes2 == null) {
						matchingInnerNodes2 = Collections.emptyList();
					}
					if(matchingInnerNodes2.size() > matchingInnerNodes1.size() && matchingInnerNodes1.size() > 0) {
						int numberOfMappings = mappings.size();
						processInnerNodes(matchingInnerNodes1, matchingInnerNodes2, leaves1, leaves2, parameterToArgumentMap, removedOperations, addedOperations, tryWithResourceMigration, containsCallToExtractedMethod, map1, map2);
						List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
						for(int i = numberOfMappings; i < mappings.size(); i++) {
							AbstractCodeMapping mapping = mappings.get(i);
							innerNodes1.remove(mapping.getFragment1());
							innerNodes2ToBeRemoved.add((CompositeStatementObject)mapping.getFragment2());
						}
						continue;
					}
					TreeSet<CompositeStatementObjectMapping> mappingSet = parentMapping != null ? new TreeSet<CompositeStatementObjectMapping>(new ScopedCompositeMappingComparatorForExtract(parentMapping)) : new TreeSet<CompositeStatementObjectMapping>();
					for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
						CompositeStatementObject statement1 = innerNodeIterator1.next();
						if(!alreadyMatched1(statement1)) {
							if(statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString()) || differOnlyInThis(statement1.getString(), statement2.getString())) {
								ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, matchingInnerNodes1, matchingInnerNodes2);
								double score = computeScore(statement1, statement2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
								if(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
									mappingSet.add(mapping);
								}
							}
							else if(checkForAlternativeTryOrSynchronizedBlocks(statement1, statement2, tryWithResourceMigration)) {
								List<AbstractCodeFragment> allUnmatchedNodes1 = new ArrayList<>();
								allUnmatchedNodes1.addAll(innerNodes1);
								allUnmatchedNodes1.addAll(leaves1);
								List<AbstractCodeFragment> allUnmatchedNodes2 = new ArrayList<>();
								allUnmatchedNodes2.addAll(innerNodes2);
								allUnmatchedNodes2.addAll(leaves2);
								ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, allUnmatchedNodes1, allUnmatchedNodes2);
								findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo, false, this);
								double score = computeScore(statement1, statement2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
								if(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
									mappingSet.add(mapping);
								}
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						if(isScopedMatch(startMapping, endMapping, parentMapping) && (mappingSet.size() > 1 || (mappingSet.size() == 1 && debatableMapping(parentMapping, mappingSet.first())))) {
							TreeSet<CompositeStatementObjectMapping> scopedMappingSet = parentMapping != null ? new TreeSet<CompositeStatementObjectMapping>(new ScopedCompositeMappingComparatorForExtract(parentMapping)) : new TreeSet<CompositeStatementObjectMapping>();
							for(CompositeStatementObjectMapping mapping : mappingSet) {
								if(isWithinScope(startMapping, endMapping, parentMapping, mapping, referencedVariableDeclarations1, referencedVariableDeclarations2)) {
									scopedMappingSet.add(mapping);
								}
							}
							if(!scopedMappingSet.isEmpty()) {
								CompositeStatementObjectMapping minStatementMapping = scopedMappingSet.first();
								addMapping(minStatementMapping);
								innerNodes1.remove(minStatementMapping.getFragment1());
								innerNodeIterator2.remove();
							}
						}
						else {
							Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
							boolean equalStringRepresentationForFirstMapping = !movedOutOfIfElseBranch.contains(mappingSet.first()) &&
									((CompositeStatementObject)mappingSet.first().getFragment1()).stringRepresentation().equals(((CompositeStatementObject)mappingSet.first().getFragment2()).stringRepresentation());
							if(movedOutOfIfElseBranch.size() > 1 && !equalStringRepresentationForFirstMapping) {
								for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
									addMapping(mapping);
									innerNodes1.remove(mapping.getFragment1());
								}
								innerNodeIterator2.remove();
							}
							else {
								CompositeStatementObjectMapping oneTryBlockNestedUnderTheOther = oneTryBlockNestedUnderTheOther(mappingSet);
								Map<CompositeStatementObjectMapping, Boolean> mappingExistsIdenticalInExtractedMethod = new LinkedHashMap<>();
								if(parentMapper == null && containsCallToExtractedMethod && mappingSet.size() > 1) {
									for(CompositeStatementObjectMapping mapping : mappingSet) {
										mappingExistsIdenticalInExtractedMethod.put(mapping, mappingExistsIdenticalInExtractedMethod(mapping, leaves2, addedOperations));
									}
								}
								CompositeStatementObjectMapping minStatementMapping = null;
								if(oneTryBlockNestedUnderTheOther != null) {
									minStatementMapping = oneTryBlockNestedUnderTheOther;
								}
								else if(mappingExistsIdenticalInExtractedMethod.containsKey(mappingSet.first()) && mappingExistsIdenticalInExtractedMethod.get(mappingSet.first())) {
									for(CompositeStatementObjectMapping mapping : mappingSet) {
										if(!mappingExistsIdenticalInExtractedMethod.get(mapping)) {
											minStatementMapping = mapping;
											break;
										}
									}
									if(minStatementMapping == null) {
										minStatementMapping = mappingSet.first();
									}
								}
								else {
									minStatementMapping = mappingSet.first();
								}
								checkForCatchBlockMerge(mappingSet, parameterToArgumentMap);
								addMapping(minStatementMapping);
								innerNodes1.remove(minStatementMapping.getFragment1());
								innerNodeIterator2.remove();
							}
						}
					}
				}
			}
			innerNodes2.removeAll(innerNodes2ToBeRemoved);
			
			// exact matching - inner nodes - with variable renames
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				if(!alreadyMatched2(statement2)) {
					List<CompositeStatementObject> matchingInnerNodes2 = map2.get(statement2.getString());
					if(matchingInnerNodes2 == null) {
						matchingInnerNodes2 = Collections.emptyList();
					}
					TreeSet<CompositeStatementObjectMapping> mappingSet = parentMapping != null ? new TreeSet<CompositeStatementObjectMapping>(new ScopedCompositeMappingComparatorForExtract(parentMapping)) : new TreeSet<CompositeStatementObjectMapping>();
					for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
						CompositeStatementObject statement1 = innerNodeIterator1.next();
						if(!alreadyMatched1(statement1)) {
							List<AbstractCodeFragment> allUnmatchedNodes1 = new ArrayList<>();
							allUnmatchedNodes1.addAll(innerNodes1);
							allUnmatchedNodes1.addAll(leaves1);
							List<AbstractCodeFragment> allUnmatchedNodes2 = new ArrayList<>();
							allUnmatchedNodes2.addAll(innerNodes2);
							allUnmatchedNodes2.addAll(leaves2);
							ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, allUnmatchedNodes1, allUnmatchedNodes2);
							Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo, false, this);
							
							double score = computeScore(statement1, statement2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
							if(score == 0 && replacements != null) {
								if(replacements.size() == 1 && (replacementInfo.getReplacements(ReplacementType.INFIX_OPERATOR).size() > 0 || replacementInfo.getReplacements(ReplacementType.INVERT_CONDITIONAL).size() > 0)) {
									//special handling when there is only an infix operator or invert conditional replacement, but no children mapped
									score = 0.99;
								}
								else if(replacements.size() <= 2 && replacementInfo.getReplacements(ReplacementType.INVERT_CONDITIONAL).size() > 0) {
									score = 0.99;
								}
								else if(containsInvertCondition(statement1, statement2)) {
									score = 0.99;
								}
								else if(replacementInfo.getReplacements(ReplacementType.COMPOSITE).size() > 0) {
									score = 0.99;
								}
								else if(replacementInfo.getReplacements(ReplacementType.PATTERN_INSTANCE).size() > 0) {
									score = 0.99;
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
									//find if the corresponding try blocks are already matched
									for(AbstractCodeMapping mapping : mappings) {
										if(mapping.getFragment1() instanceof TryStatementObject && mapping.getFragment2() instanceof TryStatementObject) {
											TryStatementObject try1 = (TryStatementObject)mapping.getFragment1();
											TryStatementObject try2 = (TryStatementObject)mapping.getFragment2();
											if(try1.getCatchClauses().contains(statement1) && try2.getCatchClauses().contains(statement2)) {
												int count = 0;
												if(try1.getCatchClauses().size() == try2.getCatchClauses().size()) {
													for(int i=0; i<try1.getCatchClauses().size(); i++) {
														CompositeStatementObject catch1 = try1.getCatchClauses().get(i);
														CompositeStatementObject catch2 = try2.getCatchClauses().get(i);
														List<VariableDeclaration> variableDeclarations1 = catch1.getVariableDeclarations();
														List<VariableDeclaration> variableDeclarations2 = catch2.getVariableDeclarations();
														if(variableDeclarations1.size() > 0 && variableDeclarations1.size() == variableDeclarations2.size()) {
															VariableDeclaration v1 = variableDeclarations1.get(0);
															VariableDeclaration v2 = variableDeclarations2.get(0);
															if(v1.getType().equals(v2.getType())) {
																count++;
															}
														}
													}
												}
												boolean equalNumberOfCatchClausesWithSameExceptionTypes = count == try1.getCatchClauses().size();
												if(replacements.isEmpty() || (try1.getCatchClauses().size() == 1 && try2.getCatchClauses().size() == 1) || equalNumberOfCatchClausesWithSameExceptionTypes) {
													score = 0.99;
												}
												break;
											}
										}
									}
								}
								else if(identicalCommentsInBody(statement1, statement2)) {
									score = 0.99;
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
										statement1.getVariableDeclarations().toString().equals(statement2.getVariableDeclarations().toString())) {
									score = 0.99;
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
									List<AbstractExpression> expressions2 = statement2.getExpressions();
									AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
									for(AbstractExpression expression1 : statement1.getExpressions()) {
										if(expression1.getString().contains(enhancedForExpression.getString() + ".length") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".size()") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".iterator()") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".listIterator()") ||
												expression1.getString().contains(enhancedForExpression.getString() + ".descendingIterator()")) {
											score = 0.99;
											break;
										}
									}
								}
								else if(replacements.size() == 1 && replacementInfo.getReplacements(ReplacementType.TYPE).size() > 0) {
									List<Replacement> typeReplacements = replacementInfo.getReplacements(ReplacementType.TYPE);
									Replacement typeReplacement = typeReplacements.get(0);
									if(typeReplacement.getBefore().startsWith(typeReplacement.getAfter() + "<") || typeReplacement.getAfter().startsWith(typeReplacement.getBefore() + "<")) {
										score = 0.99;
									}
								}
								else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										statement1.getParent() != null && statement2.getParent() != null &&
										statement1.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										statement2.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
										((alreadyMatched1(statement1.getParent()) && alreadyMatched2(statement2.getParent())) ||
												compositeChildMatchingScore(statement1.getParent(), statement2.getParent(), Optional.of(replacementInfo), mappings, removedOperations, addedOperations) > 0)) {
									score = 0.99;
								}
								if(score == 0.99) {
									for(CompositeStatementObject matchingInnerNode2 : matchingInnerNodes2) {
										if(!matchingInnerNode2.equals(statement2)) {
											ReplacementInfo replacementInfo2 = initializeReplacementInfo(statement1, matchingInnerNode2, allUnmatchedNodes1, allUnmatchedNodes2);
											Set<Replacement> replacements2 = findReplacementsWithExactMatching(statement1, matchingInnerNode2, parameterToArgumentMap, replacementInfo2, false, this);
											
											double score2 = computeScore(statement1, matchingInnerNode2, Optional.of(replacementInfo2), removedOperations, addedOperations, tryWithResourceMigration);
											if(score2 > 0) {
												CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, matchingInnerNode2, parameterToArgumentMap, score2);
												mapping.addReplacements(replacements2);
												mapping.addLambdaMappers(replacementInfo2.getLambdaMappers());
												mapping.addSubExpressionMappings(replacementInfo2.getSubExpressionMappings());
												mappingSet.add(mapping);
											}
										}
									}
								}
							}
							if((replacements != null || identicalBody(statement1, statement2) || allLeavesWithinBodyMapped(statement1, statement2)) &&
									(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
								CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
								mapping.addReplacements(replacements);
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
								for(AbstractCodeFragment leaf : leaves2) {
									if(leaf.getLocationInfo().before(mapping.getFragment2().getLocationInfo())) {
										mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null, mappings);
										if(mapping.isIdenticalWithExtractedVariable()) {
											List<LambdaExpressionObject> lambdas1 = mapping.getFragment1().getLambdas();
											List<LambdaExpressionObject> lambdas2 = leaf.getLambdas();
											if(lambdas1.size() == lambdas2.size()) {
												for(int i=0; i<lambdas1.size(); i++) {
													LambdaExpressionObject lambda1 = lambdas1.get(i);
													LambdaExpressionObject lambda2 = lambdas2.get(i);
													processLambdas(lambda1, lambda2, replacementInfo, this);
												}
											}
											break;
										}
									}
								}
								for(AbstractExpression expression : statement2.getExpressions()) {
									if(expression.getPatternInstanceofExpressions().size() > 0) {
										mapping.temporaryVariableAssignment(expression, leaves2, classDiff, parentMapper != null, mappings);
									}
								}
								for(AbstractCodeFragment leaf : leaves1) {
									if(leaf.getLocationInfo().before(mapping.getFragment1().getLocationInfo())) {
										mapping.inlinedVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
										if(mapping.isIdenticalWithInlinedVariable()) {
											break;
										}
									}
								}
								mappingSet.add(mapping);
							}
							else if(replacements == null && !containsCallToExtractedMethod && statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType()) && score > 0 && mappingSet.size() > 0 && score >= 2.0*mappingSet.first().getCompositeChildMatchingScore()) {
								CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								mappingSet.add(mapping);
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						if(isScopedMatch(startMapping, endMapping, parentMapping) && (mappingSet.size() > 1 || (mappingSet.size() == 1 && debatableMapping(parentMapping, mappingSet.first())))) {
							TreeSet<CompositeStatementObjectMapping> scopedMappingSet = parentMapping != null ? new TreeSet<CompositeStatementObjectMapping>(new ScopedCompositeMappingComparatorForExtract(parentMapping)) : new TreeSet<CompositeStatementObjectMapping>();
							for(CompositeStatementObjectMapping mapping : mappingSet) {
								if(isWithinScope(startMapping, endMapping, parentMapping, mapping, referencedVariableDeclarations1, referencedVariableDeclarations2)) {
									scopedMappingSet.add(mapping);
								}
							}
							if(!scopedMappingSet.isEmpty()) {
								CompositeStatementObjectMapping minStatementMapping = scopedMappingSet.first();
								addMapping(minStatementMapping);
								innerNodes1.remove(minStatementMapping.getFragment1());
								innerNodeIterator2.remove();
							}
						}
						else {
							Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
							if(movedOutOfIfElseBranch.size() > 1) {
								for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
									addMapping(mapping);
									innerNodes1.remove(mapping.getFragment1());
								}
								innerNodeIterator2.remove();
							}
							else {
								CompositeStatementObjectMapping oneTryBlockNestedUnderTheOther = oneTryBlockNestedUnderTheOther(mappingSet);
								CompositeStatementObjectMapping minStatementMapping = oneTryBlockNestedUnderTheOther != null ? oneTryBlockNestedUnderTheOther : mappingSet.first();
								addMapping(minStatementMapping);
								innerNodes1.remove(minStatementMapping.getFragment1());
								innerNodeIterator2.remove();
							}
						}
					}
				}
			}
		}
		Set<CompositeStatementObject> switchStatements1 = new LinkedHashSet<>();
		Set<CompositeStatementObject> ifStatements1 = new LinkedHashSet<>();
		Set<AbstractCodeFragment> switchCases1 = new LinkedHashSet<>();
		for(CompositeStatementObject comp : innerNodes1) {
			if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
				switchStatements1.add(comp);
			}
			else if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				ifStatements1.add(comp);
			}
		}
		for(AbstractCodeFragment leaf : leaves1) {
			if(leaf.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE)) {
				switchCases1.add(leaf);
			}
		}
		
		Set<CompositeStatementObject> switchStatements2 = new LinkedHashSet<>();
		Set<CompositeStatementObject> ifStatements2 = new LinkedHashSet<>();
		Set<AbstractCodeFragment> switchCases2 = new LinkedHashSet<>();
		for(CompositeStatementObject comp : innerNodes2) {
			if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
				switchStatements2.add(comp);
			}
			else if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				ifStatements2.add(comp);
			}
		}
		for(AbstractCodeFragment leaf : leaves2) {
			if(leaf.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE)) {
				switchCases2.add(leaf);
			}
		}
		
		if(switchStatements1.size() == 0 && ifStatements1.size() > 0 && switchStatements2.size() > 0) {
			for(CompositeStatementObject switchStatement2 : switchStatements2) {
				AbstractExpression switchExpression = switchStatement2.getExpressions().get(0);
				for(CompositeStatementObject ifStatement1 : ifStatements1) {
					AbstractExpression ifExpression = ifStatement1.getExpressions().get(0);
					if(ifExpression.getString().contains(switchExpression.getString())) {
						for(AbstractCodeFragment switchCase2 : switchCases2) {
							if(switchCase2.getString().startsWith(JAVA.CASE_SPACE)) {
								String caseExpression = switchCase2.getString().substring(5, switchCase2.getString().length()-1);
								if(ifExpression.getString().contains(switchExpression.getString() + " == " + caseExpression) ||
										ifExpression.getString().contains(caseExpression + " == " + switchExpression.getString()) ||
										ifExpression.getString().contains(switchExpression.getString() + ".equals(" + caseExpression + ")") ||
										ifExpression.getString().contains(caseExpression + ".equals(" + switchExpression.getString() + ")")) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(ifStatement1, switchStatement2, parameterToArgumentMap, 0);
									Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
									additionallyMatchedStatements2.add(switchCase2);
									CompositeReplacement composite = new CompositeReplacement(ifStatement1.getString(), switchStatement2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
									mapping.addReplacement(composite);
									addMapping(mapping);
									List<LeafExpression> subExpressions1 = ifExpression.findExpression(caseExpression);
									List<LeafExpression> subExpressions2 = switchCase2.findExpression(caseExpression);
									if(subExpressions1.size() == 1 && subExpressions2.size() == 1) {
										LeafMapping leafMapping = new LeafMapping(subExpressions1.get(0), subExpressions2.get(0), container1, container2);
										addMapping(leafMapping);
									}
									leaves2.remove(switchCase2);
									innerNodes1.remove(ifStatement1);
									innerNodes2.remove(switchStatement2);
								}
							}
						}
					}
				}
			}
		}
		if(switchStatements2.size() == 0 && ifStatements2.size() > 0 && switchStatements1.size() > 0) {
			for(CompositeStatementObject switchStatement1 : switchStatements1) {
				AbstractExpression switchExpression = switchStatement1.getExpressions().get(0);
				for(CompositeStatementObject ifStatement2 : ifStatements2) {
					AbstractExpression ifExpression = ifStatement2.getExpressions().get(0);
					if(ifExpression.getString().contains(switchExpression.getString())) {
						for(AbstractCodeFragment switchCase1 : switchCases1) {
							if(switchCase1.getString().startsWith(JAVA.CASE_SPACE)) {
								String caseExpression = switchCase1.getString().substring(5, switchCase1.getString().length()-1);
								if(ifExpression.getString().contains(switchExpression.getString() + " == " + caseExpression) ||
										ifExpression.getString().contains(caseExpression + " == " + switchExpression.getString()) ||
										ifExpression.getString().contains(switchExpression.getString() + ".equals(" + caseExpression + ")") ||
										ifExpression.getString().contains(caseExpression + ".equals(" + switchExpression.getString() + ")")) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(switchStatement1, ifStatement2, parameterToArgumentMap, 0);
									Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
									additionallyMatchedStatements1.add(switchCase1);
									CompositeReplacement composite = new CompositeReplacement(switchStatement1.getString(), ifStatement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<>());
									mapping.addReplacement(composite);
									addMapping(mapping);
									List<LeafExpression> subExpressions1 = switchCase1.findExpression(caseExpression);
									List<LeafExpression> subExpressions2 = ifExpression.findExpression(caseExpression);
									if(subExpressions1.size() == 1 && subExpressions2.size() == 1) {
										LeafMapping leafMapping = new LeafMapping(subExpressions1.get(0), subExpressions2.get(0), container1, container2);
										addMapping(leafMapping);
									}
									leaves1.remove(switchCase1);
									innerNodes2.remove(ifStatement2);
									innerNodes1.remove(switchStatement1);
								}
							}
						}
					}
				}
			}
		}
		//check for possibly missed split conditional
		Set<AbstractCodeMapping> mappingsToBeRemoved = new LinkedHashSet<AbstractCodeMapping>();
		for(AbstractCodeMapping mapping : new LinkedHashSet<>(this.mappings)) {
			if(mapping.getFragment1() instanceof CompositeStatementObject && mapping.containsReplacement(ReplacementType.CONDITIONAL) && !isInvolvedInSplitConditional(mapping)) {
				String s1 = mapping.getFragment1().getString();
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					if(innerNode2.getExpressions().size() > 0 && s1.contains(innerNode2.getExpressions().get(0).getString()) &&
							!mapping.getFragment2().getString().contains(innerNode2.getExpressions().get(0).getString())) {
						String s2 = innerNode2.getString();
						List<AbstractCodeFragment> allUnmatchedNodes1 = new ArrayList<>();
						allUnmatchedNodes1.addAll(innerNodes1);
						allUnmatchedNodes1.addAll(leaves1);
						List<AbstractCodeFragment> allUnmatchedNodes2 = new ArrayList<>();
						allUnmatchedNodes2.addAll(innerNodes2);
						allUnmatchedNodes2.addAll(leaves2);
						ReplacementInfo replacementInfo = initializeReplacementInfo(mapping.getFragment1(), innerNode2, allUnmatchedNodes1, allUnmatchedNodes2);
						mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
						boolean commonConditional = commonConditional(s1, s2, parameterToArgumentMap, replacementInfo, mapping.getFragment1(), innerNode2, this);
						if(commonConditional) {
							double score = computeScore((CompositeStatementObject)mapping.getFragment1(), innerNode2, Optional.of(replacementInfo), removedOperations, addedOperations, tryWithResourceMigration);
							if(score > 0) {
								CompositeStatementObjectMapping newMapping = createCompositeMapping((CompositeStatementObject)mapping.getFragment1(), innerNode2, parameterToArgumentMap, score);
								newMapping.addReplacements(replacementInfo.getReplacements());
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								newMapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								addMapping(newMapping);
								mappingsToBeRemoved.add(mapping);
							}
						}
					}
				}
			}
		}
		for(AbstractCodeMapping mapping : mappingsToBeRemoved) {
			removeMapping(mapping);
		}
	}

	private boolean checkForAlternativeTryOrSynchronizedBlocks(CompositeStatementObject statement1,
			CompositeStatementObject statement2, boolean tryWithResourceMigration) {
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) && tryWithResourceMigration) {
			return true;
		}
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
			List<AbstractExpression> expressions1 = statement1.getExpressions();
			List<AbstractExpression> expressions2 = statement2.getExpressions();
			if(expressions1.size() == expressions2.size() && expressions1.size() > 0) {
				AbstractExpression expression1 = expressions1.get(0);
				AbstractExpression expression2 = expressions2.get(0);
				if(expression1.getString().contains(expression2.getString()) || expression2.getString().contains(expression1.getString())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean nonMappedCompositeExistsIdenticalInExtractedMethod(CompositeStatementObject comp1) {
		if(classDiff != null && classDiff.getAddedOperations().size() > 0) {
			for(AbstractCodeMapping mapping : mappings) {
				if(!mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
					AbstractCodeFragment leaf2 = mapping.getFragment2();
					AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
					if(invocation == null) {
						invocation = leaf2.assignmentInvocationCoveringEntireStatement();
					}
					UMLOperation matchingOperation = null;
					if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, classDiff.getAddedOperations(), container2)) != null && matchingOperation.getBody() != null) {
						List<String> fragmentStringRepresentation = comp1.stringRepresentation();
						List<String> operationStringRepresentation = matchingOperation.stringRepresentation();
						for(int index = 0; index<operationStringRepresentation.size(); index++) {
							if(operationStringRepresentation.get(index).equals(fragmentStringRepresentation.get(0)) &&
									operationStringRepresentation.size() >= index + fragmentStringRepresentation.size()) {
								List<String> subList = operationStringRepresentation.subList(index, index + fragmentStringRepresentation.size());
								if(subList.equals(fragmentStringRepresentation)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean mappingExistsIdenticalInExtractedMethod(CompositeStatementObjectMapping mapping,
			List<AbstractCodeFragment> leaves2, List<UMLOperation> addedOperations) {
		if(addedOperations.size() > 0) {
			for(AbstractCodeFragment leaf2 : leaves2) {
				AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
				if(invocation == null) {
					invocation = leaf2.assignmentInvocationCoveringEntireStatement();
				}
				UMLOperation matchingOperation = null;
				if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, addedOperations, container2)) != null && matchingOperation.getBody() != null) {
					List<String> fragmentStringRepresentation = ((CompositeStatementObject)mapping.getFragment1()).stringRepresentation();
					List<String> operationStringRepresentation = matchingOperation.stringRepresentation();
					for(int index = 0; index<operationStringRepresentation.size(); index++) {
						if(operationStringRepresentation.get(index).equals(fragmentStringRepresentation.get(0)) &&
								operationStringRepresentation.size() >= index + fragmentStringRepresentation.size()) {
							List<String> subList = operationStringRepresentation.subList(index, index + fragmentStringRepresentation.size());
							if(subList.equals(fragmentStringRepresentation)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private void checkForCatchBlockMerge(TreeSet<CompositeStatementObjectMapping> mappingSet, Map<String, String> parameterToArgumentMap) {
		if(mappingSet.size() > 1) {
			int catchMappingCount = 0;
			for(CompositeStatementObjectMapping mapping : mappingSet) {
				if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
						mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
					catchMappingCount++;
				}
			}
			if(catchMappingCount == mappingSet.size()) {
				
			}
		}
		else if(mappingSet.size() == 1) {
			//check if the parent try block has another catch
			CompositeStatementObjectMapping mapping = mappingSet.first();
			CompositeStatementObject composite1 = (CompositeStatementObject) mapping.getFragment1();
			CompositeStatementObject composite2 = (CompositeStatementObject) mapping.getFragment2();
			if(composite1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					composite2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				TryStatementObject tryContainer1 = composite1.getTryContainer().isPresent() ? composite1.getTryContainer().get() : null;
				TryStatementObject tryContainer2 = composite2.getTryContainer().isPresent() ? composite2.getTryContainer().get() : null;
				if(tryContainer1 != null && tryContainer2 != null && tryContainer1.getCatchClauses().size() > tryContainer2.getCatchClauses().size()) {
					Set<AbstractCodeFragment> mergedCatchClauses = new LinkedHashSet<>();
					mergedCatchClauses.add(composite1);
					for(CompositeStatementObject comp1 : tryContainer1.getCatchClauses()) {
						if(!comp1.equals(composite1) && !alreadyMatched1(comp1)) {
							VariableDeclaration exceptionDeclaration1 = comp1.getVariableDeclarations().get(0);
							VariableDeclaration exceptionDeclaration2 = composite2.getVariableDeclarations().get(0);
							if(exceptionDeclaration2.getType() instanceof ListCompositeType && ((ListCompositeType)exceptionDeclaration2.getType()).getTypes().contains(exceptionDeclaration1.getType())) {
								Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
								additionallyMatchedStatements1.add(comp1);
								CompositeReplacement composite = new CompositeReplacement(comp1.getString(), composite2.getString(), additionallyMatchedStatements1, new LinkedHashSet<>());
								mapping.addReplacement(composite);
								mergedCatchClauses.add(comp1);
							}
						}
					}
					if(mergedCatchClauses.size() > 1) {
						MergeCatchRefactoring merge = new MergeCatchRefactoring(mergedCatchClauses, composite2, container1, container2);
						refactorings.add(merge);
						createMultiMappingsForDuplicatedStatements(mergedCatchClauses, composite2, parameterToArgumentMap);
					}
				}
			}
		}
	}

	private CompositeStatementObjectMapping oneTryBlockNestedUnderTheOther(TreeSet<CompositeStatementObjectMapping> mappingSet) {
		if(mappingSet.size() > 1) {
			int tryMappingCount = 0;
			Map<CompositeStatementObjectMapping, Boolean> identicalCatchFinallyBlocksMap = new LinkedHashMap<>();
			for(CompositeStatementObjectMapping mapping : mappingSet) {
				if(mapping.getFragment1() instanceof TryStatementObject && mapping.getFragment2() instanceof TryStatementObject) {
					boolean identicalCatchFinallyBlocks = ((TryStatementObject)mapping.getFragment1()).identicalCatchOrFinallyBlocks((TryStatementObject)mapping.getFragment2());
					identicalCatchFinallyBlocksMap.put(mapping, identicalCatchFinallyBlocks);
					tryMappingCount++;
				}
			}
			if(tryMappingCount == mappingSet.size()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				for(CompositeStatementObjectMapping mapping : mappingSet) {
					if(!mapping.equals(minStatementMapping) &&
							(
							(mapping.getFragment1().getLocationInfo().subsumes(minStatementMapping.getFragment1().getLocationInfo()) && mapping.getFragment2().equals(minStatementMapping.getFragment2())) ||
							(mapping.getFragment2().getLocationInfo().subsumes(minStatementMapping.getFragment2().getLocationInfo()) && mapping.getFragment1().equals(minStatementMapping.getFragment1()))
							)) {
						if(identicalCatchFinallyBlocksMap.get(mapping) == true && identicalCatchFinallyBlocksMap.get(minStatementMapping) == false) {
							return mapping;
						}
					}
				}
			}
		}
		return null;
	}

	private boolean allLeavesWithinBodyMapped(CompositeStatementObject statement1, CompositeStatementObject statement2) {
		if(statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType())) {
			int allLeaves1 = 0;
			int mappedLeaves1 = 0;
			for(AbstractCodeFragment leaf1 : statement1.getLeaves()) {
				if(alreadyMatched1(leaf1)) {
					mappedLeaves1++;
				}
				allLeaves1++;
			}
			
			int allLeaves2 = 0;
			int mappedLeaves2 = 0;
			for(AbstractCodeFragment leaf2 : statement2.getLeaves()) {
				if(alreadyMatched2(leaf2)) {
					mappedLeaves2++;
				}
				allLeaves2++;
			}
			return allLeaves1 == allLeaves2 && allLeaves1 > 0 && allLeaves1 == mappedLeaves1 && allLeaves2 > 0 && allLeaves2 == mappedLeaves2;
		}
		return false;
	}

	private boolean identicalCommentsInBody(CompositeStatementObject statement1, CompositeStatementObject statement2) {
		List<String> commentsWithinStatement1 = extractCommentsWithinStatement(statement1, container1);
		List<String> commentsWithinStatement2 = extractCommentsWithinStatement(statement2, container2);
		return commentsWithinStatement1.size() > 0 && commentsWithinStatement1.equals(commentsWithinStatement2);
	}

	private boolean identicalBody(CompositeStatementObject statement1, CompositeStatementObject statement2) {
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			return false;
		}
		else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
			return false;
		}
		else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			return false;
		}
		else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) {
			return false;
		}
		else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			return false;
		}
		else if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			return false;
		}
		List<String> bodyStringRepresentation1 = statement1.bodyStringRepresentation();
		List<String> bodyStringRepresentation2 = statement2.bodyStringRepresentation();
		int size1 = bodyStringRepresentation1.size();
		int size2 = bodyStringRepresentation2.size();
		if(bodyStringRepresentation1.equals(bodyStringRepresentation2)) {
			return true;
		}
		else if(Math.abs(size1 - size2) <= size2/4.0 && bodyStringRepresentation1.containsAll(bodyStringRepresentation2)) {
			return true;
		}
		else if(Math.abs(size1 - size2) <= size1/4.0 && bodyStringRepresentation2.containsAll(bodyStringRepresentation1)) {
			return true;
		}
		else if(size1 == size2) {
			int identicalStatements = 0;
			for(int i=0; i<size1; i++) {
				if(bodyStringRepresentation1.get(i).equals(bodyStringRepresentation2.get(i))) {
					identicalStatements++;
				}
			}
			if(Math.abs(size1 - identicalStatements) <= size1/4.0) {
				return true;
			}
		}
		else {
			int identicalStatements = 0;
			if(size1 < size2) {
				for(int i=0; i<size1; i++) {
					if(bodyStringRepresentation2.contains(bodyStringRepresentation1.get(i))) {
						identicalStatements++;
					}
				}
			}
			else {
				for(int i=0; i<size2; i++) {
					if(bodyStringRepresentation1.contains(bodyStringRepresentation2.get(i))) {
						identicalStatements++;
					}
				}
			}
			int maxSize = Math.max(size1, size2);
			if(Math.abs(maxSize - identicalStatements) <= maxSize/4.0) {
				return true;
			}
		}
		if(!bodyStringRepresentation1.contains(statement2.getString()) && !bodyStringRepresentation2.contains(statement1.getString())) {
			List<String> commentsWithinStatement1 = extractCommentsWithinStatement(statement1, container1);
			List<String> commentsWithinStatement2 = extractCommentsWithinStatement(statement2, container2);
			if(commentsWithinStatement1.size() > 0 && commentsWithinStatement2.size() > 0) {
				int numberOfCommentsWithinStatement1 = commentsWithinStatement1.size();
				int numberOfCommentsWithinStatement2 = commentsWithinStatement2.size();
				Set<String> intersection = new LinkedHashSet<>(commentsWithinStatement1);
				intersection.retainAll(commentsWithinStatement2);
				commentsWithinStatement1.removeAll(intersection);
				commentsWithinStatement2.removeAll(intersection);
				if(intersection.size() > 0) {
					for(String comment1 : commentsWithinStatement1) {
						for(String comment2 : commentsWithinStatement2) {
							String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(comment1, comment2);
							String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(comment1, comment2);
							if(!commonPrefix.isBlank() && !commonSuffix.isBlank()) {
								if(commonPrefix.endsWith(" ") && commonSuffix.startsWith(" ")) {
									commonPrefix = commonPrefix.trim();
								}
								if(comment1.equals(commonPrefix + commonSuffix)) {
									intersection.add(comment1);
								}
								else if(comment2.equals(commonPrefix + commonSuffix)) {
									intersection.add(comment2);
								}
							}
						}
					}
				}
				if(intersection.size() == numberOfCommentsWithinStatement1 || intersection.size() == numberOfCommentsWithinStatement2) {
					return true;
				}
			}
		}
		return false;
	}

	public static List<String> extractCommentsWithinStatement(CompositeStatementObject statement, VariableDeclarationContainer container) {
		List<UMLComment> comments1 = container.getComments();
		List<String> commentsWithinStatement1 = new ArrayList<>();
		for(UMLComment comment1 : comments1) {
			if(statement.getLocationInfo().subsumes(comment1.getLocationInfo())) {
				commentsWithinStatement1.add(comment1.getFullText());
			}
		}
		if(statement.getTryContainer() != null && statement.getTryContainer().isPresent() && commentsWithinStatement1.isEmpty()) {
			TryStatementObject tryStatement = statement.getTryContainer().get();
			for(UMLComment comment1 : comments1) {
				if(tryStatement.getLocationInfo().subsumes(comment1.getLocationInfo())) {
					commentsWithinStatement1.add(comment1.getFullText());
				}
			}
		}
		return commentsWithinStatement1;
	}

	public boolean alreadyMatched1(AbstractCodeFragment fragment) {
		if(fragment instanceof AbstractExpression) {
			for(AbstractCodeMapping mapping : mappings) {
				if(!(mapping instanceof CompositeStatementObjectMapping) &&
						!mapping.getFragment1().getLambdas().contains(((AbstractExpression) fragment).getLambdaOwner()) &&
						mapping.getFragment1().getLocationInfo().subsumes(fragment.getLocationInfo())) {
					List<LambdaExpressionObject> nestedLambdas = new ArrayList<>();
					if(mapping.getFragment1().getLambdas().size() == 1) {
						collectNestedLambdaExpressions(mapping.getFragment1().getLambdas().get(0), nestedLambdas);
					}
					if(!nestedLambdas.contains(((AbstractExpression) fragment).getLambdaOwner())) {
						return true;
					}
				}
			}
			return false;
		}
		return mappingHashcodesT1.contains(fragment.hashCode());
	}

	public boolean alreadyMatched2(AbstractCodeFragment fragment) {
		if(fragment instanceof AbstractExpression) {
			for(AbstractCodeMapping mapping : mappings) {
				if(!(mapping instanceof CompositeStatementObjectMapping) &&
						!mapping.getFragment2().getLambdas().contains(((AbstractExpression) fragment).getLambdaOwner()) &&
						mapping.getFragment2().getLocationInfo().subsumes(fragment.getLocationInfo())) {
					List<LambdaExpressionObject> nestedLambdas = new ArrayList<>();
					if(mapping.getFragment2().getLambdas().size() == 1) {
						collectNestedLambdaExpressions(mapping.getFragment2().getLambdas().get(0), nestedLambdas);
					}
					if(!nestedLambdas.contains(((AbstractExpression) fragment).getLambdaOwner())) {
						return true;
					}
				}
			}
			return false;
		}
		return mappingHashcodesT2.contains(fragment.hashCode());
	}

	private static int tryWithResourcesCount(List<CompositeStatementObject> innerNodes) {
		int tryWithResources = 0;
		for(CompositeStatementObject comp1 : innerNodes) {
			if(comp1 instanceof TryStatementObject) {
				if(comp1.getExpressions().size() > 0) {
					tryWithResources++;
				}
			}
		}
		return tryWithResources;
	}

	private double computeScore(CompositeStatementObject statement1, CompositeStatementObject statement2, Optional<ReplacementInfo> replacementInfo,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration) {
		if(statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
			return compositeChildMatchingScore((TryStatementObject)statement1, (TryStatementObject)statement2, replacementInfo, mappings, removedOperations, addedOperations, tryWithResourceMigration);
		}
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			for(AbstractCodeMapping mapping : mappings) {
				if(mapping.getFragment1() instanceof TryStatementObject && mapping.getFragment2() instanceof TryStatementObject) {
					TryStatementObject try1 = (TryStatementObject)mapping.getFragment1();
					TryStatementObject try2 = (TryStatementObject)mapping.getFragment2();
					if(try1.getCatchClauses().contains(statement1) && try2.getCatchClauses().contains(statement2)) {
						return compositeChildMatchingScore(statement1, statement2, replacementInfo, mappings, removedOperations, addedOperations);
					}
				}
			}
			return -1;
		}
		return compositeChildMatchingScore(statement1, statement2, replacementInfo, mappings, removedOperations, addedOperations);
	}

	private CompositeStatementObjectMapping createCompositeMapping(CompositeStatementObject statement1,
			CompositeStatementObject statement2, Map<String, String> parameterToArgumentMap, double score) {
		VariableDeclarationContainer container1 = codeFragmentOperationMap1.containsKey(statement1) ? codeFragmentOperationMap1.get(statement1) : this.container1;
		VariableDeclarationContainer container2 = codeFragmentOperationMap2.containsKey(statement2) ? codeFragmentOperationMap2.get(statement2) : this.container2;
		CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, container1, container2, score, identicalCommentsInBody(statement1, statement2));
		if(ifBecomingElseIf(mapping)) {
			ifBecomingElseIf.add(mapping);
		}
		if(ifAddingElseIf(mapping)) {
			ifAddingElseIf.add(mapping);
		}
		for(String key : parameterToArgumentMap.keySet()) {
			String value = parameterToArgumentMap.get(key);
			if(!key.equals(value) && ReplacementUtil.contains(statement2.getString(), key) && ReplacementUtil.contains(statement1.getString(), value)) {
				mapping.addReplacement(new Replacement(value, key, ReplacementType.VARIABLE_NAME));
			}
		}
		return mapping;
	}

	private boolean isomorphicCompositeStructure(List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2) {
		if(innerNodes1.size() == innerNodes2.size()) {
			for(int i=0; i<innerNodes1.size(); i++) {
				CompositeStatementObject comp1 = innerNodes1.get(i);
				CompositeStatementObject comp2 = innerNodes2.get(i);
				if(!sameCodeElementType(comp1, comp2)) {
					return false;
				}
			}
			return true;
		}
		else if(innerNodes1.size() < innerNodes2.size()) {
			if(innerNodes1.isEmpty() && !innerNodes2.isEmpty()) {
				return false;
			}
			for(int i=0; i<innerNodes1.size(); i++) {
				CompositeStatementObject comp1 = innerNodes1.get(i);
				CompositeStatementObject comp2 = innerNodes2.get(i);
				if(!identical(comp1, comp2)) {
					return false;
				}
			}
			return true;
		}
		else if(innerNodes1.size() > innerNodes2.size()) {
			if(innerNodes2.isEmpty() && !innerNodes1.isEmpty()) {
				return false;
			}
			for(int i=0; i<innerNodes2.size(); i++) {
				CompositeStatementObject comp1 = innerNodes1.get(i);
				CompositeStatementObject comp2 = innerNodes2.get(i);
				if(!identical(comp1, comp2)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean sameCodeElementType(CompositeStatementObject comp1, CompositeStatementObject comp2) {
		if(comp1.getLocationInfo().getCodeElementType().getName().equals(comp2.getLocationInfo().getCodeElementType().getName())) {
			CompositeStatementObject parent1 = comp1.getParent();
			CompositeStatementObject parent2 = comp2.getParent();
			if(parent1 == null && parent2 != null) {
				return false;
			}
			else if(parent1 != null && parent2 == null) {
				return false;
			}
			else if(parent1 != null && parent2 != null) {
				if(!parent1.getLocationInfo().getCodeElementType().getName().equals(parent2.getLocationInfo().getCodeElementType().getName())) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	private boolean identical(CompositeStatementObject comp1, CompositeStatementObject comp2) {
		boolean identical = comp1.getString().equals(comp2.getString());
		if(identical) {
			return true;
		}
		else if(comp1.getLocationInfo().getCodeElementType().getName().equals(comp2.getLocationInfo().getCodeElementType().getName())) {
			CompositeStatementObject parent1 = comp1.getParent();
			CompositeStatementObject parent2 = comp2.getParent();
			if(parent1 == null && parent2 != null) {
				return false;
			}
			else if(parent1 != null && parent2 == null) {
				return false;
			}
			else if(parent1 != null && parent2 != null) {
				if(parent1.getParent() == null && parent2.getParent() == null) {
					return false;
				}
				if(!parent1.getLocationInfo().getCodeElementType().getName().equals(parent2.getLocationInfo().getCodeElementType().getName())) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	protected void processLeaves(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap, boolean isomorphic) throws RefactoringMinerTimedOutException {
		if(leaves1.size() > MAXIMUM_NUMBER_OF_COMPARED_STATEMENTS && leaves2.size() > MAXIMUM_NUMBER_OF_COMPARED_STATEMENTS &&
				container1.getBodyHashCode() != container2.getBodyHashCode()) {
			return;
		}
		List<TreeSet<LeafMapping>> postponedMappingSets = new ArrayList<TreeSet<LeafMapping>>();
		boolean leaves1LessThanLeaves2UnderComposites = true;
		int assertions1 = 0;
		int assertions2 = 0;
		if(leaves1.size() == leaves2.size() && !isomorphic) {
			Set<AbstractCodeFragment> leaves1NestedDirectlyUnderMethodBody = new LinkedHashSet<AbstractCodeFragment>();
			for(AbstractCodeFragment leaf1 : leaves1) {
				if(leaf1.getParent() != null && leaf1.getParent().getParent() == null) {
					leaves1NestedDirectlyUnderMethodBody.add(leaf1);
				}
			}
			Set<AbstractCodeFragment> leaves2NestedDirectlyUnderMethodBody = new LinkedHashSet<AbstractCodeFragment>();
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(leaf2.getParent() != null && leaf2.getParent().getParent() == null) {
					leaves2NestedDirectlyUnderMethodBody.add(leaf2);
				}
			}
			int leaves1WithoutDirectlyNestedUnderMethodBody = leaves1.size() - leaves1NestedDirectlyUnderMethodBody.size();
			int leaves2WithoutDirectlyNestedUnderMethodBody = leaves2.size() - leaves2NestedDirectlyUnderMethodBody.size();
			leaves1LessThanLeaves2UnderComposites = leaves1WithoutDirectlyNestedUnderMethodBody <= leaves2WithoutDirectlyNestedUnderMethodBody;
		}
		else if(isomorphic && (container1.hasTestAnnotation() || container1.getName().startsWith("test")) && (container2.hasTestAnnotation() || container2.getName().startsWith("test"))) {
			for(AbstractCodeFragment leaf1 : leaves1) {
				if(leaf1.getParent() != null && leaf1.getParent().getParent() == null && leaf1.getString().startsWith("assert")) {
					assertions1++;
				}
			}
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(leaf2.getParent() != null && leaf2.getParent().getParent() == null && leaf2.getString().startsWith("assert")) {
					assertions2++;
				}
			}
		}
		boolean leaves1LessThanLeaves2 = leaves1.size() <= leaves2.size() && leaves1LessThanLeaves2UnderComposites;
		boolean equalNumberOfAssertions = assertions1 == assertions2 && assertions1 > 0;
		if(leaves1LessThanLeaves2) {
			//exact string+depth matching - leaf nodes
			if(isomorphic) {
				for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
					AbstractCodeFragment leaf1 = leafIterator1.next();
					if(!alreadyMatched1(leaf1)) {
						List<AbstractCodeFragment> matchingLeaves1 = new ArrayList<>();
						Set<AbstractCodeFragment> parents1 = new HashSet<>();
						for(AbstractCodeFragment l1 : leaves1) {
							if(l1.getString().equals(leaf1.getString())) {
								matchingLeaves1.add(l1);
								parents1.add(l1.getParent());
							}
						}
						List<AbstractCodeFragment> matchingLeaves2 = new ArrayList<>();
						Set<AbstractCodeFragment> parents2 = new HashSet<>();
						for(AbstractCodeFragment l2 : leaves2) {
							if(l2.getString().equals(leaf1.getString())) {
								matchingLeaves2.add(l2);
								parents2.add(l2.getParent());
							}
						}
						boolean allMatchingLeaves1InMethodScope = parents1.size() == 1 && parents1.iterator().next() != null && parents1.iterator().next().getParent() == null;
						boolean allMatchingLeaves2InMethodScope = parents2.size() == 1 && parents2.iterator().next() != null && parents2.iterator().next().getParent() == null;
						if(matchingLeaves1.size() > matchingLeaves2.size() && matchingLeaves2.size() > 0 && !allMatchingLeaves1InMethodScope && !allMatchingLeaves2InMethodScope) {
							processLeaves(matchingLeaves1, matchingLeaves2, parameterToArgumentMap, isomorphic);
							continue;
						}
						TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
						int matchCount = 0;
						for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
							AbstractCodeFragment leaf2 = leafIterator2.next();
							if(!alreadyMatched2(leaf2)) {
								String argumentizedString1 = preprocessInput1(leaf1, leaf2);
								String argumentizedString2 = preprocessInput2(leaf1, leaf2);
								if(leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) {
									matchCount++;
									if(leaf1.getDepth() == leaf2.getDepth() && equalCatchClauseIndex(leaf1, leaf2)) {
										LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
										mappingSet.add(mapping);
									}
								}
							}
						}
						if(parentMapper != null && !this.lambdaBodyMapper && matchCount > 1) {
							Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet);
							boolean identicalSwitch = false;
							if(switchParentEntry != null) {
								identicalSwitch = switchParentEntry.getLeft().stringRepresentation().equals(switchParentEntry.getRight().stringRepresentation());
							}
							if(!identicalSwitch) {
								continue;
							}
						}
						if(equalNumberOfAssertions && leaf1.isAssertCall() && mappingSet.size() > 0) {
							LeafMapping minStatementMapping = mappingSet.first();
							int index1 = leaves1.indexOf(minStatementMapping.getFragment1());
							int index2 = leaves2.indexOf(minStatementMapping.getFragment2());
							if(index1 != index2) {
								continue;
							}
						}
						if(!mappingSet.isEmpty()) {
							Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = null;
							if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
								LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
								addToMappings(bestMapping, mappingSet);
								leaves2.remove(bestMapping.getFragment2());
								leafIterator1.remove();
							}
							else {
								LeafMapping minStatementMapping = mappingSet.first();
								addMapping(minStatementMapping);
								processAnonymousClassDeclarationsInIdenticalStatements(minStatementMapping);
								leaves2.remove(minStatementMapping.getFragment2());
								leafIterator1.remove();
							}
						}
					}
				}
			}
			AbstractCodeMapping parentMapping = null;
			if(parentMapper != null && operationInvocation != null) {
				parentMapping = findParentMappingContainingOperationInvocation();
			}
			//exact string matching - leaf nodes - finds moves to another level
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				if(!alreadyMatched1(leaf1)) {
					List<AbstractCodeFragment> matchingLeaves1 = new ArrayList<>();
					Set<AbstractCodeFragment> parents1 = new HashSet<>();
					for(AbstractCodeFragment l1 : leaves1) {
						if(l1.getString().equals(leaf1.getString())) {
							matchingLeaves1.add(l1);
							parents1.add(l1.getParent());
						}
					}
					List<AbstractCodeFragment> matchingLeaves2 = new ArrayList<>();
					Set<AbstractCodeFragment> parents2 = new HashSet<>();
					for(AbstractCodeFragment l2 : leaves2) {
						if(l2.getString().equals(leaf1.getString())) {
							matchingLeaves2.add(l2);
							parents2.add(l2.getParent());
						}
					}
					boolean foundInExtractedStatements = false;
					for(Set<AbstractCodeFragment> set : extractedStatements.values()) {
						if(set.contains(leaf1)) {
							foundInExtractedStatements = true;
							break;
						}
					}
					if(foundInExtractedStatements && !leaf1.isKeyword()) {
						continue;
					}
					boolean allMatchingLeaves1InMethodScope = parents1.size() == 1 && parents1.iterator().next() != null && parents1.iterator().next().getParent() == null;
					boolean allMatchingLeaves2InMethodScope = parents2.size() == 1 && parents2.iterator().next() != null && parents2.iterator().next().getParent() == null;
					if(matchingLeaves1.size() > matchingLeaves2.size() && matchingLeaves2.size() > 0 && !allMatchingLeaves1InMethodScope && !allMatchingLeaves2InMethodScope) {
						processLeaves(matchingLeaves1, matchingLeaves2, parameterToArgumentMap, isomorphic);
						boolean alreadyRemoved = false;
						for(AbstractCodeMapping mapping : this.mappings) {
							leaves2.remove(mapping.getFragment2());
							if(mapping.getFragment1().equals(leaf1) && !alreadyRemoved) {
								leafIterator1.remove();
								alreadyRemoved = true;
							}
						}
						continue;
					}
					TreeSet<LeafMapping> mappingSet = initializeMappingSetForInlineScenario(parentMapping);
					for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
						AbstractCodeFragment leaf2 = leafIterator2.next();
						if((mappingSet.size() == 1 || mappings.size() == 1) && parentMapper != null && operationInvocation != null && this.callsToExtractedMethod > matchingLeaves1.size() && matchingLeaves1.size() > 0) {
							//find previous and next mapping in parentMapper
							AbstractCodeMapping mappingBefore = null;
							AbstractCodeMapping mappingAfter = null;
							AbstractCodeMapping first = mappings.size() == 1 ? mappings.iterator().next() : mappingSet.iterator().next();
							for(AbstractCodeMapping pMapping : parentMapper.mappings) {
								if(pMapping.getFragment1().getLocationInfo().getStartLine() <= first.getFragment1().getLocationInfo().getStartLine()) {
									mappingBefore = pMapping;
								}
								else if(pMapping.getFragment1().getLocationInfo().getStartLine() >= first.getFragment1().getLocationInfo().getStartLine()) {
									mappingAfter = pMapping;
									break;
								}
							}
							if(mappingBefore != null && mappingAfter != null) {
								int inBetween = 0;
								for(AbstractCodeFragment leafX1 : matchingLeaves1) {
									if(!leafX1.equals(leaf1)) {
										if(leafX1.getLocationInfo().getStartLine() >= mappingBefore.getFragment1().getLocationInfo().getStartLine() &&
												leafX1.getLocationInfo().getStartLine() <= mappingAfter.getFragment1().getLocationInfo().getStartLine()) {
											inBetween++;
										}
									}
								}
								if(inBetween == 0) {
									continue;
								}
							}
						}
						if(!alreadyMatched2(leaf2)) {
							String argumentizedString1 = preprocessInput1(leaf1, leaf2);
							String argumentizedString2 = preprocessInput2(leaf1, leaf2);
							if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2))) {
								LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
								mappingSet.add(mapping);
							}
						}
					}
					if(equalNumberOfAssertions && leaf1.isAssertCall() && mappingSet.size() > 0) {
						LeafMapping minStatementMapping = mappingSet.first();
						int index1 = leaves1.indexOf(minStatementMapping.getFragment1());
						int index2 = leaves2.indexOf(minStatementMapping.getFragment2());
						if(index1 != index2) {
							continue;
						}
					}
					if(!mappingSet.isEmpty()) {
						boolean codeUnderIfMovedUnderElse = false;
						if(!leaf1.isKeyword()) {
							codeUnderIfMovedUnderElse = codeUnderIfMovedUnderElse(mappingSet);
						}
						boolean identicalPreviousAndNextStatement = mappingSet.first().hasIdenticalPreviousAndNextStatement();
						if(mappingSet.size() > 1 && (parentMapper != null || codeUnderIfMovedUnderElse) && mappings.size() > 1 && !identicalPreviousAndNextStatement) {
							TreeMap<Integer, LeafMapping> lineDistanceMap = new TreeMap<>();
							TreeMap<Double, LeafMapping> levelParentEditDistanceSum = new TreeMap<>();
							for(LeafMapping mapping : mappingSet) {
								int lineDistance = lineDistanceFromExistingMappings2(mapping).getMiddle();
								double parentEditDistanceSum = mapping.levelParentEditDistanceSum();
								if(!levelParentEditDistanceSum.containsKey(parentEditDistanceSum)) {
									levelParentEditDistanceSum.put(parentEditDistanceSum, mapping);
								}
								if(!lineDistanceMap.containsKey(lineDistance)) {
									lineDistanceMap.put(lineDistance, mapping);
								}
							}
							LeafMapping minLineDistanceStatementMapping = null;
							if(!levelParentEditDistanceSum.firstEntry().getValue().equals(lineDistanceMap.firstEntry().getValue())) {
								int lineDistance1 = 0, lineDistance2 = 0;
								for(Map.Entry<Integer,LeafMapping> entry : lineDistanceMap.entrySet()) {
									if(entry.getValue().equals(levelParentEditDistanceSum.firstEntry().getValue())) {
										lineDistance1 = entry.getKey();
									}
									if(entry.getValue().equals(lineDistanceMap.firstEntry().getValue())) {
										lineDistance2 = entry.getKey();
									}
								}
								if(levelParentEditDistanceSum.size() > 1 && Math.abs(lineDistance1 - lineDistance2) <= Math.min(lineDistance1, lineDistance2)) {
									minLineDistanceStatementMapping = levelParentEditDistanceSum.firstEntry().getValue();
								}
								else {
									minLineDistanceStatementMapping = lineDistanceMap.firstEntry().getValue();
								}
							}
							else {
								minLineDistanceStatementMapping = lineDistanceMap.firstEntry().getValue();
							}
							addMapping(minLineDistanceStatementMapping);
							processAnonymousClassDeclarationsInIdenticalStatements(minLineDistanceStatementMapping);
							leaves2.remove(minLineDistanceStatementMapping.getFragment2());
							leafIterator1.remove();
						}
						else {
							Set<AbstractCodeMapping> movedInIfElseBranch = movedInIfElseIfBranch(mappingSet);
							if(movedInIfElseBranch.size() > 1 && multiMappingCondition(matchingLeaves1, matchingLeaves2)) {
								for(AbstractCodeMapping mapping : movedInIfElseBranch) {
									addToMappings((LeafMapping) mapping, mappingSet);
									leaves2.remove(mapping.getFragment2());
								}
								leafIterator1.remove();
								checkForMatchingSplitVariableDeclaration(leaf1, leaves2, parameterToArgumentMap, equalNumberOfAssertions);
							}
							else {
								Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = null;
								if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
									LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
									addToMappings(bestMapping, mappingSet);
									leaves2.remove(bestMapping.getFragment2());
									leafIterator1.remove();
								}
								else {
									LeafMapping minStatementMapping = mappingSet.first();
									addMapping(minStatementMapping);
									processAnonymousClassDeclarationsInIdenticalStatements(minStatementMapping);
									leaves2.remove(minStatementMapping.getFragment2());
									leafIterator1.remove();
								}
							}
						}
					}
				}
			}
			
			boolean allIdenticalStatementsHaveSameIndex = false;
			boolean swappedVariableDeclarations = false;
			if(leaves1.size() == leaves2.size() && isomorphic) {
				int nonComposite = 0;
				int count = 0;
				int mappingsDirectlyNestedUnderMethodBody = 0;
				for(AbstractCodeMapping mapping : mappings) {
					if(!(mapping instanceof CompositeStatementObjectMapping)) {
						nonComposite++;
						AbstractCodeFragment f1 = mapping.getFragment1();
						AbstractCodeFragment f2 = mapping.getFragment2();
						if(f1.getIndex() == f2.getIndex()) {
							count++;
							if(nestedDirectlyUnderMethodBody(mapping)) {
								mappingsDirectlyNestedUnderMethodBody++;
							}
						}
					}
				}
				if(count == nonComposite && mappingsDirectlyNestedUnderMethodBody == nonComposite && count > 1) {
					List<String> declarationNames1 = new ArrayList<>();
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(leaf1.getVariableDeclarations().size() > 0) {
							declarationNames1.add(leaf1.getVariableDeclarations().get(0).getVariableName());
						}
					}
					List<String> declarationNames2 = new ArrayList<>();
					for(AbstractCodeFragment leaf2 : leaves2) {
						if(leaf2.getVariableDeclarations().size() > 0) {
							declarationNames2.add(leaf2.getVariableDeclarations().get(0).getVariableName());
						}
					}
					if(declarationNames1.size() > 0 && declarationNames1.containsAll(declarationNames2) && declarationNames2.containsAll(declarationNames1) && !declarationNames1.equals(declarationNames2)) {
						swappedVariableDeclarations = true;
					}
					if(!swappedVariableDeclarations)
						allIdenticalStatementsHaveSameIndex = true;
				}
			}
			AbstractCodeMapping startMapping = null;
			AbstractCodeMapping endMapping = null;
			Set<VariableDeclaration> referencedVariableDeclarations1 = new LinkedHashSet<>();
			Set<VariableDeclaration> referencedVariableDeclarations2 = new LinkedHashSet<>();
			if(parentMapper != null) {
				if(this.mappings.size() > 1) {
					for(AbstractCodeMapping mapping : this.mappings) {
						if(startMapping == null) {
							startMapping = mapping;
						}
						else if(mapping.getFragment1().getLocationInfo().getStartLine() < startMapping.getFragment1().getLocationInfo().getStartLine() &&
								mapping.getFragment2().getLocationInfo().getStartLine() < startMapping.getFragment2().getLocationInfo().getStartLine()) {
							startMapping = mapping;
						}
						if(endMapping == null) {
							endMapping = mapping;
						}
						else if(mapping.getFragment1().getLocationInfo().getStartLine() > endMapping.getFragment1().getLocationInfo().getStartLine() &&
								mapping.getFragment2().getLocationInfo().getStartLine() > endMapping.getFragment2().getLocationInfo().getStartLine()) {
							endMapping = mapping;
						}
					}
				}
				if(startMapping != null && endMapping != null && startMapping.equals(endMapping)) {
					List<VariableDeclaration> variableDeclarationsInScope1 = container1.getVariableDeclarationsInScope(startMapping.getFragment1().getLocationInfo());
					for(LeafExpression variable : startMapping.getFragment1().getVariables()) {
						for(VariableDeclaration variableDeclaration : variableDeclarationsInScope1) {
							if(variable.getString().equals(variableDeclaration.getVariableName())) {
								referencedVariableDeclarations1.add(variableDeclaration);
							}
						}
					}
					List<VariableDeclaration> variableDeclarationsInScope2 = container2.getVariableDeclarationsInScope(startMapping.getFragment2().getLocationInfo());
					for(LeafExpression variable : startMapping.getFragment2().getVariables()) {
						for(VariableDeclaration variableDeclaration : variableDeclarationsInScope2) {
							if(variable.getString().equals(variableDeclaration.getVariableName())) {
								referencedVariableDeclarations2.add(variableDeclaration);
							}
						}
					}
				}
				if(this.mappings.isEmpty() && operationInvocation != null && parentMapping == null) {
					AbstractCodeFragment statementContainingOperationInvocation = null;
					for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
						if(mapping instanceof LeafMapping) {
							if(mapping.getFragment1().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
								statementContainingOperationInvocation = mapping.getFragment1();
							}
						}
						if(statementContainingOperationInvocation != null && mapping.getFragment1().equals(statementContainingOperationInvocation)) {
							startMapping = mapping;
							endMapping = mapping;
							break;
						}
					}
				}
			}
			// exact matching with variable renames
			Set<AbstractCodeFragment> leaves1ToBeRemoved = new LinkedHashSet<>();
			Set<AbstractCodeFragment> leaves2ToBeRemoved = new LinkedHashSet<>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				if(!alreadyMatched1(leaf1)) {
					List<AbstractCodeFragment> matchingLeaves1 = new ArrayList<>();
					if(mappings.size() == 1) {
						for(AbstractCodeFragment l1 : leaves1) {
							if(l1.getString().equals(mappings.iterator().next().getFragment2().getString())) {
								matchingLeaves1.add(l1);
							}
						}
					}
					if(matchingLeaves1.size() > 0 && skipCurrentIteration(matchingLeaves1)) {
						continue;
					}
					TreeSet<LeafMapping> mappingSet = initializeMappingSetForInlineScenario(parentMapping);
					for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
						AbstractCodeFragment leaf2 = leafIterator2.next();
						if(!alreadyMatched2(leaf2)) {
							int mappingsBefore = mappings.size();
							int refactoringsBefore = refactorings.size();
							ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
							Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
							if (replacements != null) {
								LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
								if(leaf1.getVariableDeclarations().size() > 0 && leaf2.getVariableDeclarations().size() > 0 && swappedVariableDeclarations) {
									mapping.setSwappedVariableDeclaration(true);
								}
								mapping.addReplacements(replacements);
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf2, mapping, replacementInfo);
								mappingSet.add(mapping);
								if(allIdenticalStatementsHaveSameIndex) {
									break;
								}
							}
							else {
								//removed any nested mappings
								List<AbstractCodeMapping> orderedMappings = new ArrayList<AbstractCodeMapping>(mappings);
								for(int i=orderedMappings.size()-1; i>=0 && i>=mappingsBefore; i--) {
									AbstractCodeMapping m = orderedMappings.get(i);
									if(leaf1.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && leaf2.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
											replacementInfo.lambdaMapperContainsMapping(m)) {
										removeMapping(m);
									}
									else {
										break;
									}
								}
								List<Refactoring> orderedRefactorings = new ArrayList<>(refactorings);
								for(int i=orderedRefactorings.size()-1; i>=0 && i>=refactoringsBefore; i--) {
									Refactoring r = orderedRefactorings.get(i);
									refactorings.remove(r);
								}
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = null;
						Map<LeafMapping, Pair<CompositeStatementObject, CompositeStatementObject>> catchBlockMap = null;
						if(variableDeclarationMappingsWithSameReplacementTypes(mappingSet)) {
							//postpone mapping
							postponedMappingSets.add(mappingSet);
						}
						else if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
							LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
							if(canBeAdded(bestMapping, parameterToArgumentMap)) {
								addToMappings(bestMapping, mappingSet);
								leaves2.remove(bestMapping.getFragment2());
								leafIterator1.remove();
							}
						}
						else if((catchBlockMap = allMappingsNestedUnderCatchBlocks(mappingSet)) != null) {
							LeafMapping bestMapping = findBestMappingBasedOnTryBlockMappings(catchBlockMap, mappingSet);
							if(canBeAdded(bestMapping, parameterToArgumentMap)) {
								addToMappings(bestMapping, mappingSet);
								leaves2.remove(bestMapping.getFragment2());
								leafIterator1.remove();
							}
						}
						else {
							if(isScopedMatch(startMapping, endMapping, parentMapping) && (mappingSet.size() > 1 || (mappingSet.size() == 1 && debatableMapping(parentMapping, mappingSet.first())))) {
								TreeSet<LeafMapping> scopedMappingSet = initializeMappingSetForInlineScenario(parentMapping);
								for(LeafMapping mapping : mappingSet) {
									if(isWithinScope(startMapping, endMapping, parentMapping, mapping, referencedVariableDeclarations1, referencedVariableDeclarations2)) {
										scopedMappingSet.add(mapping);
									}
								}
								if(!scopedMappingSet.isEmpty()) {
									if(allUnderTheSameParent(scopedMappingSet)) {
										LeafMapping minStatementMapping = scopedMappingSet.first();
										if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
											addToMappings(minStatementMapping, scopedMappingSet);
											leaves2.remove(minStatementMapping.getFragment2());
											leafIterator1.remove();
										}
									}
									else {
										TreeMap<Integer, LeafMapping> lineDistanceMap = checkForOtherPossibleMatchesWithLineDistance1(
												leaves1, leaves2, leaf1, scopedMappingSet, parameterToArgumentMap, equalNumberOfAssertions);
										LeafMapping minLineDistanceStatementMapping = lineDistanceMap.firstEntry().getValue();
										addMapping(minLineDistanceStatementMapping);
										leaves2.remove(minLineDistanceStatementMapping.getFragment2());
										leafIterator1.remove();
									}
								}
							}
							else {
								boolean isTestMethod = (container1.hasTestAnnotation() || container2.hasTestAnnotation() || container1.getName().startsWith("test") || container2.getName().startsWith("test"))
										&& !container2.hasParameterizedTestAnnotation() && leaves2.size() < 2*leaves1.size();
								boolean mappingWithNestedLambdaMapper = mappingSet.first().getLambdaMappers().size() > 0/* && mappingSet.first().nestedLambdaMappers().size() > 0*/;
								if(!isTestMethod && !allIdenticalStatementsHaveSameIndex && !mappingWithNestedLambdaMapper)
									checkForOtherPossibleMatchesForFragment2(leaves1, leaves2, leaf1, mappingSet, parameterToArgumentMap, equalNumberOfAssertions);
								Set<AbstractCodeMapping> movedInIfElseBranch = movedInIfElseIfBranch(mappingSet);
								Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
								Set<AbstractCodeMapping> splitToMultipleAssignments = splitToMultipleAssignments(mappingSet);
								if(movedInIfElseBranch.size() > 1 && !mappingSet.first().hasIdenticalPreviousAndNextStatement()) {
									for(AbstractCodeMapping mapping : movedInIfElseBranch) {
										addToMappings((LeafMapping) mapping, mappingSet);
										leaves2.remove(mapping.getFragment2());
									}
									leafIterator1.remove();
									checkForMatchingSplitVariableDeclaration(leaf1, leaves2, parameterToArgumentMap, equalNumberOfAssertions);
								}
								else if(movedOutOfIfElseBranch.size() > 1) {
									for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
										addToMappings((LeafMapping) mapping, mappingSet);
										leaves2.remove(mapping.getFragment2());
										leaves1ToBeRemoved.add(mapping.getFragment1());
									}
									leafIterator1.remove();
								}
								else if(splitToMultipleAssignments.size() > 1) {
									for(AbstractCodeMapping mapping : splitToMultipleAssignments) {
										addToMappings((LeafMapping) mapping, mappingSet);
										leaves2.remove(mapping.getFragment2());
									}
									leafIterator1.remove();
								}
								else if(mappingSet.size() > 1 && internalParameterizeTest(mappingSet) && mappingsWithSameReplacementTypes(mappingSet)) {
									Set<AbstractCodeMapping> multiMappings = new LinkedHashSet<AbstractCodeMapping>();
									for(AbstractCodeMapping mapping : mappingSet) {
										multiMappings.add(mapping);
										addToMappings((LeafMapping) mapping, mappingSet);
										leaves2.remove(mapping.getFragment2());
									}
									this.internalParameterizeTestMultiMappings.add(multiMappings);
									leafIterator1.remove();
								}
								else {
									LeafMapping minStatementMapping = mappingSet.first();
									if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
										addToMappings(minStatementMapping, mappingSet);
										leaves2.remove(minStatementMapping.getFragment2());
										if(minStatementMapping.getFragment1().equals(leaf1)) {
											leafIterator1.remove();
										}
										else {
											leaves1ToBeRemoved.add(minStatementMapping.getFragment1());
										}
										// internalParameterizeTest() generates LeafExpression mappings for parameterized test
										internalParameterizeTest(mappingSet);
										checkForSplitVariableDeclaration(minStatementMapping.getFragment1(), leaves1, leaves2, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves2ToBeRemoved);
										checkForMergedVariableDeclaration(minStatementMapping.getFragment2(), leaves1, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves1ToBeRemoved);
									}
								}
							}
						}
					}
				}
			}
			leaves1.removeAll(leaves1ToBeRemoved);
			leaves2.removeAll(leaves2ToBeRemoved);
		}
		else {
			//exact string+depth matching - leaf nodes
			if(isomorphic) {
				for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
					AbstractCodeFragment leaf2 = leafIterator2.next();
					if(!alreadyMatched2(leaf2)) {
						List<AbstractCodeFragment> matchingLeaves1 = new ArrayList<>();
						Set<AbstractCodeFragment> parents1 = new HashSet<>();
						for(AbstractCodeFragment l1 : leaves1) {
							if(l1.getString().equals(leaf2.getString())) {
								matchingLeaves1.add(l1);
								parents1.add(l1.getParent());
							}
						}
						List<AbstractCodeFragment> matchingLeaves2 = new ArrayList<>();
						Set<AbstractCodeFragment> parents2 = new HashSet<>();
						for(AbstractCodeFragment l2 : leaves2) {
							if(l2.getString().equals(leaf2.getString())) {
								matchingLeaves2.add(l2);
								parents2.add(l2.getParent());
							}
						}
						boolean allMatchingLeaves1InMethodScope = parents1.size() == 1 && parents1.iterator().next() != null && parents1.iterator().next().getParent() == null;
						boolean allMatchingLeaves2InMethodScope = parents2.size() == 1 && parents2.iterator().next() != null && parents2.iterator().next().getParent() == null;
						if(matchingLeaves2.size() > matchingLeaves1.size() && matchingLeaves1.size() > 0 && !allMatchingLeaves1InMethodScope && !allMatchingLeaves2InMethodScope) {
							processLeaves(matchingLeaves1, matchingLeaves2, parameterToArgumentMap, isomorphic);
							continue;
						}
						TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
						int matchCount = 0;
						for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
							AbstractCodeFragment leaf1 = leafIterator1.next();
							if(!alreadyMatched1(leaf1)) {
								String argumentizedString1 = preprocessInput1(leaf1, leaf2);
								String argumentizedString2 = preprocessInput2(leaf1, leaf2);
								if(leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) {
									matchCount++;
									if(leaf1.getDepth() == leaf2.getDepth() && equalCatchClauseIndex(leaf1, leaf2)) {
										LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
										mappingSet.add(mapping);
									}
								}
							}
						}
						if(parentMapper != null && !this.lambdaBodyMapper && matchCount > 1) {
							Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet);
							boolean identicalSwitch = false;
							if(switchParentEntry != null) {
								identicalSwitch = switchParentEntry.getLeft().stringRepresentation().equals(switchParentEntry.getRight().stringRepresentation());
							}
							if(!identicalSwitch) {
								continue;
							}
						}
						if(!mappingSet.isEmpty()) {
							Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = null;
							if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
								LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
								addToMappings(bestMapping, mappingSet);
								leaves1.remove(bestMapping.getFragment1());
								leafIterator2.remove();
							}
							else {
								LeafMapping minStatementMapping = mappingSet.first();
								addMapping(minStatementMapping);
								processAnonymousClassDeclarationsInIdenticalStatements(minStatementMapping);
								leaves1.remove(minStatementMapping.getFragment1());
								leafIterator2.remove();
							}
						}
					}
				}
			}
			AbstractCodeMapping parentMapping = null;
			if(parentMapper != null && operationInvocation != null) {
				parentMapping = findParentMappingContainingOperationInvocation();
			}
			//exact string matching - leaf nodes - finds moves to another level
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				if(!alreadyMatched2(leaf2)) {
					List<AbstractCodeFragment> matchingLeaves1 = new ArrayList<>();
					Set<AbstractCodeFragment> parents1 = new HashSet<>();
					for(AbstractCodeFragment l1 : leaves1) {
						if(l1.getString().equals(leaf2.getString())) {
							matchingLeaves1.add(l1);
							parents1.add(l1.getParent());
						}
					}
					List<AbstractCodeFragment> matchingLeaves2 = new ArrayList<>();
					Set<AbstractCodeFragment> parents2 = new HashSet<>();
					for(AbstractCodeFragment l2 : leaves2) {
						if(l2.getString().equals(leaf2.getString())) {
							matchingLeaves2.add(l2);
							parents2.add(l2.getParent());
						}
					}
					boolean allMatchingLeaves1InMethodScope = parents1.size() == 1 && parents1.iterator().next() != null && parents1.iterator().next().getParent() == null;
					boolean allMatchingLeaves2InMethodScope = parents2.size() == 1 && parents2.iterator().next() != null && parents2.iterator().next().getParent() == null;
					if(matchingLeaves2.size() > matchingLeaves1.size() && matchingLeaves1.size() > 0 && !allMatchingLeaves1InMethodScope && !allMatchingLeaves2InMethodScope) {
						processLeaves(matchingLeaves1, matchingLeaves2, parameterToArgumentMap, isomorphic);
						boolean alreadyRemoved = false;
						for(AbstractCodeMapping mapping : this.mappings) {
							leaves1.remove(mapping.getFragment1());
							if(mapping.getFragment2().equals(leaf2) && !alreadyRemoved) {
								leafIterator2.remove();
								alreadyRemoved = true;
							}
						}
						continue;
					}
					if(matchingLeaves1.size() > 1 && skipCurrentIteration(matchingLeaves1)) {
						continue;
					}
					TreeSet<LeafMapping> mappingSet = initializeMappingSetForExtractScenario(parentMapping);
					for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
						AbstractCodeFragment leaf1 = leafIterator1.next();
						boolean foundInExtractedStatements = false;
						for(Set<AbstractCodeFragment> set : extractedStatements.values()) {
							if(set.contains(leaf1)) {
								foundInExtractedStatements = true;
								break;
							}
						}
						if(foundInExtractedStatements && !leaf1.isKeyword()) {
							continue;
						}
						if(!alreadyMatched1(leaf1)) {
							String argumentizedString1 = preprocessInput1(leaf1, leaf2);
							String argumentizedString2 = preprocessInput2(leaf1, leaf2);
							if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2))) {
								LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
								mappingSet.add(mapping);
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						boolean identicalDepthAndIndex = false;
						for(AbstractCodeMapping m : mappingSet) {
							int index1 = m.getFragment1().getIndex();
							int index2 = m.getFragment2().getIndex();
							if(index1 != index2) {
								//check if parent includes try-catch and adjust the index
								int catchFinallyBlockCount1 = 0;
								if(m.getFragment1().getParent() != null) {
									for(AbstractStatement parentStatement : m.getFragment1().getParent().getStatements()) {
										if(parentStatement.equals(m.getFragment1())) {
											break;
										}
										if(parentStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) ||
												parentStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
											catchFinallyBlockCount1++;
										}
									}
								}
								index1 = index1 - catchFinallyBlockCount1;
								int catchFinallyBlockCount2 = 0;
								if(m.getFragment2().getParent() != null) {
									for(AbstractStatement parentStatement : m.getFragment2().getParent().getStatements()) {
										if(parentStatement.equals(m.getFragment2())) {
											break;
										}
										if(parentStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) ||
												parentStatement.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
											catchFinallyBlockCount2++;
										}
									}
								}
								index2 = index2 - catchFinallyBlockCount2;
							}
							if(m.getFragment1().getDepth() == m.getFragment2().getDepth() && index1 == index2) {
								identicalDepthAndIndex = true;
								break;
							}
						}
						boolean identicalPreviousAndNextStatement = parentMapper == null && mappingSet.first().hasIdenticalPreviousAndNextStatement();
						if(mappingSet.size() > 1 && (parentMapper != null || (!identicalDepthAndIndex && !leaf2.isKeyword() && !leaf2.isLogCall())) && mappings.size() > 0 && !allMappingsAreKeywords() && !identicalPreviousAndNextStatement) {
							TreeMap<Integer, LeafMapping> lineDistanceMap = new TreeMap<>();
							TreeMap<Double, LeafMapping> levelParentEditDistanceSum = new TreeMap<>();
							for(LeafMapping mapping : mappingSet) {
								int lineDistance = lineDistanceFromExistingMappings1(mapping).getMiddle();
								double parentEditDistanceSum = mapping.levelParentEditDistanceSum();
								if(!levelParentEditDistanceSum.containsKey(parentEditDistanceSum)) {
									levelParentEditDistanceSum.put(parentEditDistanceSum, mapping);
								}
								if(!lineDistanceMap.containsKey(lineDistance)) {
									lineDistanceMap.put(lineDistance, mapping);
								}
							}
							LeafMapping minLineDistanceStatementMapping = null;
							if(!levelParentEditDistanceSum.firstEntry().getValue().equals(lineDistanceMap.firstEntry().getValue())) {
								int lineDistance1 = 0, lineDistance2 = 0;
								for(Map.Entry<Integer,LeafMapping> entry : lineDistanceMap.entrySet()) {
									if(entry.getValue().equals(levelParentEditDistanceSum.firstEntry().getValue())) {
										lineDistance1 = entry.getKey();
									}
									if(entry.getValue().equals(lineDistanceMap.firstEntry().getValue())) {
										lineDistance2 = entry.getKey();
									}
								}
								if(levelParentEditDistanceSum.size() > 1 && Math.abs(lineDistance1 - lineDistance2) <= Math.min(lineDistance1, lineDistance2)) {
									minLineDistanceStatementMapping = levelParentEditDistanceSum.firstEntry().getValue();
								}
								else {
									minLineDistanceStatementMapping = lineDistanceMap.firstEntry().getValue();
								}
							}
							else {
								minLineDistanceStatementMapping = lineDistanceMap.firstEntry().getValue();
							}
							Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
							if(movedOutOfIfElseBranch.size() > 1 && multiMappingCondition(matchingLeaves1, matchingLeaves2)) {
								for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
									addMapping(mapping);
									processAnonymousClassDeclarationsInIdenticalStatements((LeafMapping) mapping);
									leaves1.remove(mapping.getFragment1());
								}
								leafIterator2.remove();
							}
							else {
								if(!duplicateMappingInParentMapper(mappingSet)) {
									addMapping(minLineDistanceStatementMapping);
									processAnonymousClassDeclarationsInIdenticalStatements(minLineDistanceStatementMapping);
									leaves1.remove(minLineDistanceStatementMapping.getFragment1());
									leafIterator2.remove();
								}
							}
						}
						else {
							Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = null;
							if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
								LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
								addToMappings(bestMapping, mappingSet);
								leaves1.remove(bestMapping.getFragment1());
								leafIterator2.remove();
							}
							else {
								if(parentMapping != null && parentOrSiblingMapperContainsMapping(mappingSet) != null) {
									TreeSet<LeafMapping> scopedMappingSet = initializeMappingSetForExtractScenario(parentMapping);
									for(LeafMapping mapping : mappingSet) {
										if(parentMapping.getFragment1().getLocationInfo().subsumes(mappingSet.first().getFragment1().getLocationInfo()) &&
												parentMapping.getFragment2().getLocationInfo().subsumes(mappingSet.first().getFragment2().getLocationInfo())) {
											scopedMappingSet.add(mapping);
										}
									}
									if(!scopedMappingSet.isEmpty()) {
										if(allUnderTheSameParent(scopedMappingSet)) {
											LeafMapping minStatementMapping = scopedMappingSet.first();
											if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
												addToMappings(minStatementMapping, scopedMappingSet);
												leaves1.remove(minStatementMapping.getFragment1());
												leafIterator2.remove();
											}
										}
									}
								}
								else {
									Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
									if(movedOutOfIfElseBranch.size() > 1 && multiMappingCondition(matchingLeaves1, matchingLeaves2)) {
										for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
											addMapping(mapping);
											processAnonymousClassDeclarationsInIdenticalStatements((LeafMapping) mapping);
											leaves1.remove(mapping.getFragment1());
										}
										leafIterator2.remove();
									}
									else {
										if(!duplicateMappingInParentMapper(mappingSet)) {
											LeafMapping minStatementMapping = mappingSet.first();
											addMapping(minStatementMapping);
											processAnonymousClassDeclarationsInIdenticalStatements(minStatementMapping);
											leaves1.remove(minStatementMapping.getFragment1());
											leafIterator2.remove();
										}
									}
								}
							}
						}
					}
				}
			}
			boolean swappedVariableDeclarations = false;
			List<String> declarationNames1 = new ArrayList<>();
			for(AbstractCodeFragment leaf1 : leaves1) {
				if(leaf1.getVariableDeclarations().size() > 0) {
					declarationNames1.add(leaf1.getVariableDeclarations().get(0).getVariableName());
				}
			}
			List<String> declarationNames2 = new ArrayList<>();
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(leaf2.getVariableDeclarations().size() > 0) {
					declarationNames2.add(leaf2.getVariableDeclarations().get(0).getVariableName());
				}
			}
			if(declarationNames1.size() > 0 && declarationNames1.containsAll(declarationNames2) && declarationNames2.containsAll(declarationNames1) && !declarationNames1.equals(declarationNames2)) {
				swappedVariableDeclarations = true;
			}
			else if(declarationNames1.size() > 0 && declarationNames2.size() > 0 && declarationNames1.size() > declarationNames2.size() && declarationNames1.containsAll(declarationNames2) && !declarationNames2.contains(declarationNames1.get(0))) {
				swappedVariableDeclarations = true;
			}
			AbstractCodeMapping startMapping = null;
			AbstractCodeMapping endMapping = null;
			Set<VariableDeclaration> referencedVariableDeclarations1 = new LinkedHashSet<>();
			Set<VariableDeclaration> referencedVariableDeclarations2 = new LinkedHashSet<>();
			if(parentMapper != null) {
				for(AbstractCodeMapping mapping : this.mappings) {
					if(startMapping == null) {
						startMapping = mapping;
					}
					else if(mapping.getFragment1().getLocationInfo().getStartLine() < startMapping.getFragment1().getLocationInfo().getStartLine() &&
							mapping.getFragment2().getLocationInfo().getStartLine() < startMapping.getFragment2().getLocationInfo().getStartLine()) {
						startMapping = mapping;
					}
					if(endMapping == null) {
						endMapping = mapping;
					}
					else if(mapping.getFragment1().getLocationInfo().getStartLine() > endMapping.getFragment1().getLocationInfo().getStartLine() &&
							mapping.getFragment2().getLocationInfo().getStartLine() > endMapping.getFragment2().getLocationInfo().getStartLine()) {
						endMapping = mapping;
					}
				}
				if(startMapping != null && endMapping != null && startMapping.equals(endMapping)) {
					List<VariableDeclaration> variableDeclarationsInScope1 = container1.getVariableDeclarationsInScope(startMapping.getFragment1().getLocationInfo());
					for(LeafExpression variable : startMapping.getFragment1().getVariables()) {
						for(VariableDeclaration variableDeclaration : variableDeclarationsInScope1) {
							if(variable.getString().equals(variableDeclaration.getVariableName())) {
								referencedVariableDeclarations1.add(variableDeclaration);
							}
						}
					}
					List<VariableDeclaration> variableDeclarationsInScope2 = container2.getVariableDeclarationsInScope(startMapping.getFragment2().getLocationInfo());
					for(LeafExpression variable : startMapping.getFragment2().getVariables()) {
						for(VariableDeclaration variableDeclaration : variableDeclarationsInScope2) {
							if(variable.getString().equals(variableDeclaration.getVariableName())) {
								referencedVariableDeclarations2.add(variableDeclaration);
							}
						}
					}
				}
				if(this.mappings.isEmpty() && operationInvocation != null && parentMapping == null) {
					AbstractCodeFragment statementContainingOperationInvocation = null;
					for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
						if(mapping instanceof LeafMapping) {
							if(mapping.getFragment2().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
								statementContainingOperationInvocation = mapping.getFragment2();
							}
						}
						if(statementContainingOperationInvocation != null && mapping.getFragment2().equals(statementContainingOperationInvocation)) {
							startMapping = mapping;
							endMapping = mapping;
							break;
						}
					}
				}
			}
			// exact matching with variable renames
			Set<AbstractCodeFragment> leaves1ToBeRemoved = new LinkedHashSet<>();
			Set<AbstractCodeFragment> leaves2ToBeRemoved = new LinkedHashSet<>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				if(!alreadyMatched2(leaf2)) {
					List<AbstractCodeFragment> matchingLeaves1 = new ArrayList<>();
					if(mappings.size() == 1) {
						for(AbstractCodeFragment l1 : leaves1) {
							if(l1.getString().equals(mappings.iterator().next().getFragment2().getString())) {
								matchingLeaves1.add(l1);
							}
						}
					}
					if(matchingLeaves1.size() > 1 && skipCurrentIteration(matchingLeaves1)) {
						continue;
					}
					TreeSet<LeafMapping> mappingSet = initializeMappingSetForExtractScenario(parentMapping);
					for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
						AbstractCodeFragment leaf1 = leafIterator1.next();
						if(!alreadyMatched1(leaf1)) {
							int mappingsBefore = mappings.size();
							int refactoringsBefore = refactorings.size();
							ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
							Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
							if (replacements != null) {
								LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
								if(leaf1.getVariableDeclarations().size() > 0 && leaf2.getVariableDeclarations().size() > 0 && swappedVariableDeclarations) {
									mapping.setSwappedVariableDeclaration(true);
								}
								mapping.addReplacements(replacements);
								mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
								mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
								extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf2, mapping, replacementInfo);
								mappingSet.add(mapping);
							}
							else {
								//removed any nested mappings
								List<AbstractCodeMapping> orderedMappings = new ArrayList<AbstractCodeMapping>(mappings);
								for(int i=orderedMappings.size()-1; i>=0 && i>=mappingsBefore; i--) {
									AbstractCodeMapping m = orderedMappings.get(i);
									if(leaf1.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && leaf2.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
											replacementInfo.lambdaMapperContainsMapping(m)) {
										removeMapping(m);
									}
									else {
										break;
									}
								}
								List<Refactoring> orderedRefactorings = new ArrayList<>(refactorings);
								for(int i=orderedRefactorings.size()-1; i>=0 && i>=refactoringsBefore; i--) {
									Refactoring r = orderedRefactorings.get(i);
									refactorings.remove(r);
								}
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry = null;
						Map<LeafMapping, Pair<CompositeStatementObject, CompositeStatementObject>> catchBlockMap = null;
						if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
							LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
							if(canBeAdded(bestMapping, parameterToArgumentMap)) {
								addToMappings(bestMapping, mappingSet);
								leaves1.remove(bestMapping.getFragment1());
								leafIterator2.remove();
							}
						}
						else if((catchBlockMap = allMappingsNestedUnderCatchBlocks(mappingSet)) != null) {
							LeafMapping bestMapping = findBestMappingBasedOnTryBlockMappings(catchBlockMap, mappingSet);
							if(canBeAdded(bestMapping, parameterToArgumentMap)) {
								addToMappings(bestMapping, mappingSet);
								leaves1.remove(bestMapping.getFragment1());
								leafIterator2.remove();
							}
						}
						else {
							if(isScopedMatch(startMapping, endMapping, parentMapping) && (mappingSet.size() > 1 || (mappingSet.size() == 1 && debatableMapping(parentMapping, mappingSet.first())))) {
								TreeSet<LeafMapping> scopedMappingSet = initializeMappingSetForExtractScenario(parentMapping);
								for(LeafMapping mapping : mappingSet) {
									if(isWithinScope(startMapping, endMapping, parentMapping, mapping, referencedVariableDeclarations1, referencedVariableDeclarations2)) {
										scopedMappingSet.add(mapping);
									}
								}
								if(!scopedMappingSet.isEmpty()) {
									if(allUnderTheSameParent(scopedMappingSet)) {
										LeafMapping minStatementMapping = scopedMappingSet.first();
										if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
											addToMappings(minStatementMapping, scopedMappingSet);
											leaves1.remove(minStatementMapping.getFragment1());
											leafIterator2.remove();
										}
									}
									else {
										TreeMap<Integer, LeafMapping> lineDistanceMap = checkForOtherPossibleMatchesWithLineDistance2(
												leaves1, leaves2, leaf2, scopedMappingSet, parameterToArgumentMap, equalNumberOfAssertions);
										LeafMapping minLineDistanceStatementMapping = lineDistanceMap.firstEntry().getValue();
										addMapping(minLineDistanceStatementMapping);
										leaves1.remove(minLineDistanceStatementMapping.getFragment1());
										leafIterator2.remove();
									}
								}
							}
							else {
								Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
								if(movedOutOfIfElseBranch.size() > 1) {
									for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
										addToMappings((LeafMapping) mapping, mappingSet);
										leaves1.remove(mapping.getFragment1());
									}
									leafIterator2.remove();
									checkForMatchingMergedVariableDeclaration(leaf2, leaves1, parameterToArgumentMap, equalNumberOfAssertions);
									checkForSwitchExpressionMatches(movedOutOfIfElseBranch, leaves2);
								}
								else if(mappingSet.size() > 1 && internalParameterizeTest(mappingSet) && mappingsWithSameReplacementTypes(mappingSet)) {
									Set<AbstractCodeMapping> multiMappings = new LinkedHashSet<AbstractCodeMapping>();
									for(AbstractCodeMapping mapping : mappingSet) {
										multiMappings.add(mapping);
										addToMappings((LeafMapping) mapping, mappingSet);
										leaves1.remove(mapping.getFragment1());
									}
									this.internalParameterizeTestMultiMappings.add(multiMappings);
									leafIterator2.remove();
								}
								else {
									if(!duplicateMappingInParentMapper(mappingSet)) {
										AbstractCodeMapping alreadyMatched = null;
										if((alreadyMatched = parentOrSiblingMapperContainsMapping(mappingSet)) != null && mappingSet.size() > 1) {
											Iterator<LeafMapping> iterator = mappingSet.iterator();
											LeafMapping minStatementMapping = null;
											while(iterator.hasNext()) {
												LeafMapping next = iterator.next();
												if(!next.equals(alreadyMatched)) {
													minStatementMapping = next;
													break;
												}
											}
											if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
												addToMappings(minStatementMapping, mappingSet);
												leaves1.remove(minStatementMapping.getFragment1());
												leafIterator2.remove();
											}
										}
										else {
											checkForOtherPossibleMatchesForFragment1(leaves1, leaves2, leaf2, mappingSet, parameterToArgumentMap, equalNumberOfAssertions);
											LeafMapping minStatementMapping = mappingSet.first();
											if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
												boolean split = checkForSplitVariableDeclaration(minStatementMapping.getFragment1(), leaves1, leaves2, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves2ToBeRemoved);
												if(split) {
													addToMappings(minStatementMapping, mappingSet);
													leaves1.remove(minStatementMapping.getFragment1());
													if(minStatementMapping.getFragment2().equals(leaf2)) {
														leafIterator2.remove();
													}
													else {
														leaves2ToBeRemoved.add(minStatementMapping.getFragment2());
													}
												}
												checkForMergedVariableDeclaration(minStatementMapping.getFragment2(), leaves1, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves1ToBeRemoved);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			leaves1.removeAll(leaves1ToBeRemoved);
			leaves2.removeAll(leaves2ToBeRemoved);
		}
		for(TreeSet<LeafMapping> postponed : postponedMappingSets) {
			Set<LeafMapping> mappingsToBeAdded = new LinkedHashSet<LeafMapping>();
			for(LeafMapping variableDeclarationMapping : postponed) {
				for(AbstractCodeMapping previousMapping : this.mappings) {
					Set<Replacement> intersection = variableDeclarationMapping.commonReplacements(previousMapping);
					if(!intersection.isEmpty()) {
						for(Replacement commonReplacement : intersection) {
							if(commonReplacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
								if(variableDeclarationMapping.getFragment1().getVariableDeclaration(commonReplacement.getBefore()) != null &&
										variableDeclarationMapping.getFragment2().getVariableDeclaration(commonReplacement.getAfter()) != null) {
									mappingsToBeAdded.add(variableDeclarationMapping);
								}
								else if(existingMappingWithCommonParents(variableDeclarationMapping)) {
									mappingsToBeAdded.add(variableDeclarationMapping);
								}
							}
						}
					}
				}
			}
			if(mappingsToBeAdded.size() == 1) {
				LeafMapping minStatementMapping = mappingsToBeAdded.iterator().next();
				addToMappings(minStatementMapping, postponed);
				leaves1.remove(minStatementMapping.getFragment1());
				leaves2.remove(minStatementMapping.getFragment2());
			}
			else {
				if(!duplicateMappingInParentMapper(postponed)) {
					LeafMapping minStatementMapping = postponed.first();
					addToMappings(minStatementMapping, postponed);
					leaves1.remove(minStatementMapping.getFragment1());
					leaves2.remove(minStatementMapping.getFragment2());
				}
			}
		}
	}

	private void checkForSwitchExpressionMatches(Set<AbstractCodeMapping> movedOutOfIfElseBranch,
			List<? extends AbstractCodeFragment> leaves2) {
		for(AbstractCodeFragment l : leaves2) {
			if(l.getString().contains("=switch (")) {
				for(AbstractCodeMapping mapping : movedOutOfIfElseBranch) {
					for(LeafExpression expression1 : mapping.getFragment1().getArguments()) {
						List<LeafExpression> expressions2 = l.findExpression(expression1.getString());
						for(int i=0; i<expressions2.size(); i++) {
							LeafMapping leafMapping = new LeafMapping(expression1, expressions2.get(i), container1, container2);
							addMapping(leafMapping);
						}
					}
				}
			}
		}
	}

	private boolean allMappingsAreKeywords() {
		int keywords = 0;
		for(AbstractCodeMapping mapping : this.mappings) {
			if(mapping.getFragment1().isKeyword()) {
				keywords++;
			}
		}
		return keywords == mappings.size() && keywords > 0;
	}

	private TreeSet<LeafMapping> initializeMappingSetForInlineScenario(AbstractCodeMapping parentMapping) {
		if(parentMapping != null) {
			if(parentMapping instanceof CompositeStatementObjectMapping) {
				CompositeStatementObject comp1 = (CompositeStatementObject) parentMapping.getFragment1();
				CompositeStatementObject comp2 = (CompositeStatementObject) parentMapping.getFragment2();
				if(comp1.getExpressions().size() == 1 && comp2.getExpressions().size() == 1) {
					AbstractExpression expr1 = comp1.getExpressions().get(0);
					AbstractExpression expr2 = comp2.getExpressions().get(0);
					if(expr1.toString().equals("!" + expr2.toString()) || expr2.toString().equals("!" + expr1.toString())) {
						return new TreeSet<LeafMapping>();
					}
				}
			}
			return new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForInline(parentMapping));
		}
		return new TreeSet<LeafMapping>();
	}

	private TreeSet<LeafMapping> initializeMappingSetForExtractScenario(AbstractCodeMapping parentMapping) {
		if(parentMapping != null) {
			if(parentMapping instanceof CompositeStatementObjectMapping) {
				CompositeStatementObject comp1 = (CompositeStatementObject) parentMapping.getFragment1();
				CompositeStatementObject comp2 = (CompositeStatementObject) parentMapping.getFragment2();
				if(comp1.getExpressions().size() == 1 && comp2.getExpressions().size() == 1) {
					AbstractExpression expr1 = comp1.getExpressions().get(0);
					AbstractExpression expr2 = comp2.getExpressions().get(0);
					if(expr1.toString().equals("!" + expr2.toString()) || expr2.toString().equals("!" + expr1.toString())) {
						return new TreeSet<LeafMapping>();
					}
				}
			}
			return new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForExtract(parentMapping));
		}
		return new TreeSet<LeafMapping>();
	}

	private boolean internalParameterizeTest(Set<LeafMapping> mappingSet) {
		if(container2.hasParameterizedTestAnnotation() && !container1.hasParameterizedTestAnnotation() && classDiff instanceof UMLClassBaseDiff) {
			List<String> parameterNames = container2.getParameterNameList();
			List<List<String>> parameterValues = ((UMLClassBaseDiff)classDiff).getParameterValues((UMLOperation)container2);
			List<List<LeafExpression>> parameterValuesAsLeafExpressions = ((UMLClassBaseDiff)classDiff).getParameterValuesAsLeafExpressions((UMLOperation)container2);
			Set<Replacement> replacements = new LinkedHashSet<Replacement>();
			List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
			for(AbstractCodeMapping mapping : mappingSet) {
				Map<Integer, Integer> matchingTestParameters = matchParamsWithReplacements(parameterValues, parameterNames, mapping.getReplacements());
				Integer index = null;
				int max = -1;
				for(Integer key : matchingTestParameters.keySet()) {
					if(matchingTestParameters.get(key) > max) {
						max = matchingTestParameters.get(key);
						index = key;
					}
				}
				if(index != null && max >= 1 && parameterValuesAsLeafExpressions.size() > index) {
					List<LeafExpression> parameterValueList = parameterValuesAsLeafExpressions.get(index);
					for(Replacement r : mapping.getReplacements()) {
						List<LeafExpression> matchingExpressions = mapping.getFragment1().findExpression(r.getBefore());
						for(LeafExpression matchingExpression : matchingExpressions) {
							for(LeafExpression parameterValue : parameterValueList) {
								if(matchingExpression.getString().equals(parameterValue.getString()) ||
										// handle comma-separated csv values
										sanitizeStringLiteral(parameterValue.getString()).contains(sanitizeStringLiteral(matchingExpression.getString()))) {
									LeafMapping leafMapping = new LeafMapping(matchingExpression, parameterValue, container1, container2);
									leafMappings.add(leafMapping);
									break;
								}
							}
						}
					}
				}
				replacements.addAll(mapping.getReplacements());
			}
			Map<Integer, Integer> matchingTestParameters = matchParamsWithReplacements(parameterValues, parameterNames, replacements);
			int max = matchingTestParameters.isEmpty() ? 0 : Collections.max(matchingTestParameters.values());
			if(max >= 1) {
				for(LeafMapping leafMapping : leafMappings) {
					addMapping(leafMapping);
				}
				return true;
			}
		}
		return false;
	}

	private boolean nestedDirectlyUnderMethodBody(AbstractCodeMapping mapping) {
		AbstractCodeFragment f1 = mapping.getFragment1();
		AbstractCodeFragment f2 = mapping.getFragment2();
		if(f1.getParent() != null && f1.getParent().getParent() == null &&
				f2.getParent() != null && f2.getParent().getParent() == null) {
			return true;
		}
		if(f1.getParent() == null && f2.getParent() == null) {
			return true;
		}
		if(mapping instanceof LeafMapping) {
			LeafMapping leafMapping = (LeafMapping)mapping;
			if(leafMapping.levelParentEditDistanceSum() == 0) {
				CompositeStatementObject parent1 = f1.getParent();
				while(parent1 != null) {
					if(!parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							!parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
						return false;
					}
					parent1 = parent1.getParent();
				}
				return true;
			}
		}
		return false;
	}

	private boolean skipCurrentIteration(List<AbstractCodeFragment> matchingLeaves1) {
		if(mappings.size() == 1 && parentMapper != null && operationInvocation != null && this.callsToExtractedMethod > matchingLeaves1.size()) {
			//find previous and next mapping in parentMapper
			AbstractCodeMapping mappingBefore = null;
			AbstractCodeMapping mappingAfter = null;
			for(AbstractCodeMapping pMapping : parentMapper.mappings) {
				if(pMapping.getFragment1().getLocationInfo().getStartLine() <= mappings.iterator().next().getFragment1().getLocationInfo().getStartLine()) {
					mappingBefore = pMapping;
				}
				else if(pMapping.getFragment1().getLocationInfo().getStartLine() >= mappings.iterator().next().getFragment1().getLocationInfo().getStartLine()) {
					mappingAfter = pMapping;
					break;
				}
			}
			if(mappingBefore != null && mappingAfter != null) {
				int inBetween = 0;
				for(AbstractCodeFragment leaf1 : matchingLeaves1) {
					if(leaf1.getLocationInfo().getStartLine() >= mappingBefore.getFragment1().getLocationInfo().getStartLine() &&
							leaf1.getLocationInfo().getStartLine() <= mappingAfter.getFragment1().getLocationInfo().getStartLine()) {
						inBetween++;
					}
				}
				if(inBetween == 0) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean equalCatchClauseIndex(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		if(leaf1.getParent() != null && leaf1.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
				leaf2.getParent() != null && leaf2.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			Optional<TryStatementObject> tryContainer1 = leaf1.getParent().getTryContainer();
			Optional<TryStatementObject> tryContainer2 = leaf2.getParent().getTryContainer();
			if(tryContainer1.isPresent() && tryContainer2.isPresent()) {
				TryStatementObject try1 = tryContainer1.get();
				TryStatementObject try2 = tryContainer2.get();
				int catchIndex1 = try1.getCatchClauses().indexOf(leaf1.getParent());
				int catchIndex2 = try2.getCatchClauses().indexOf(leaf2.getParent());
				if(try1.getCatchClauses().size() == try2.getCatchClauses().size() && catchIndex1 != catchIndex2) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean debatableMapping(AbstractCodeMapping parentMapping, AbstractCodeMapping childMapping) {
		return parentContainsMultipleCallsToSameMethod(parentMapping) || inconsistentParentChildRelation(parentMapping, childMapping);
	}

	private boolean inconsistentParentChildRelation(AbstractCodeMapping parentMapping, AbstractCodeMapping childMapping) {
		if(parentMapping instanceof CompositeStatementObjectMapping && operationInvocation != null) {
			CompositeStatementObject comp1 = (CompositeStatementObject)parentMapping.getFragment1();
			CompositeStatementObject comp2 = (CompositeStatementObject)parentMapping.getFragment2();
			if(callsToExtractedMethod > 0 && !comp1.getLocationInfo().subsumes(childMapping.getFragment1().getLocationInfo()) && comp2.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
				return true;
			}
			if(callsToInlinedMethod > 0 && !comp2.getLocationInfo().subsumes(childMapping.getFragment2().getLocationInfo()) && comp1.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
				return true;
			}
		}
		return false;
	}

	private boolean parentContainsMultipleCallsToSameMethod(AbstractCodeMapping parentMapping) {
		if(parentMapping instanceof CompositeStatementObjectMapping && operationInvocation != null) {
			List<AbstractCall> calls = ((CompositeStatementObject)parentMapping.getFragment2()).getAllMethodInvocations();
			int count = 0;
			for(AbstractCall call : calls) {
				if(call.equals(operationInvocation)) {
					count++;
				}
			}
			return count > 1;
		}
		return false;
	}

	private boolean multiMappingCondition(List<? extends AbstractCodeFragment> matchingLeaves1, List<? extends AbstractCodeFragment> matchingLeaves2) {
		if(matchingLeaves1.size() != matchingLeaves2.size()) {
			return true;
		}
		else {
			int sameDepth = 0;
			int depthDiffOfOne = 0;
			for(int i=0; i<matchingLeaves1.size(); i++) {
				AbstractCodeFragment fragment1 = matchingLeaves1.get(i);
				AbstractCodeFragment fragment2 = matchingLeaves2.get(i);
				if(fragment1.getDepth() == fragment2.getDepth()) {
					sameDepth++;
				}
				else if(Math.abs(fragment1.getDepth() - fragment2.getDepth()) == 1) {
					depthDiffOfOne++;
				}
			}
			if(sameDepth != matchingLeaves1.size() && depthDiffOfOne != matchingLeaves1.size()) {
				return true;
			}
		}
		return false;
	}

	private void extractInlineVariableAnalysis(List<? extends AbstractCodeFragment> leaves1,
			List<? extends AbstractCodeFragment> leaves2, AbstractCodeFragment leaf1, AbstractCodeFragment leaf2,
			AbstractCodeMapping mapping, ReplacementInfo replacementInfo) throws RefactoringMinerTimedOutException {
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeFragment leaf : leaves2) {
			if(leaf.equals(leaf2)) {
				break;
			}
			mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null, mappings);
			if(mapping.isIdenticalWithExtractedVariable()) {
				List<LambdaExpressionObject> lambdas1 = mapping.getFragment1().getLambdas();
				List<LambdaExpressionObject> lambdas2 = leaf.getLambdas();
				if(lambdas1.size() == lambdas2.size()) {
					for(int i=0; i<lambdas1.size(); i++) {
						LambdaExpressionObject lambda1 = lambdas1.get(i);
						LambdaExpressionObject lambda2 = lambdas2.get(i);
						processLambdas(lambda1, lambda2, replacementInfo, this);
					}
				}
				break;
			}
		}
		CompositeStatementObject parent2 = mapping.getFragment2().getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		if(parent2 != null && parent2.getExpressions().size() > 0) {
			for(AbstractExpression expression : parent2.getExpressions()) {
				if(expression.getPatternInstanceofExpressions().size() > 0) {
					mapping.temporaryVariableAssignment(expression, leaves2, classDiff, parentMapper != null, mappings);
				}
			}
		}
		for(AbstractCodeFragment leaf : leaves1) {
			if(leaf.equals(leaf1)) {
				break;
			}
			mapping.inlinedVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
			if(mapping.isIdenticalWithInlinedVariable()) {
				List<LambdaExpressionObject> lambdas1 = leaf.getLambdas();
				List<LambdaExpressionObject> lambdas2 = mapping.getFragment2().getLambdas();
				if(lambdas1.size() == lambdas2.size()) {
					for(int i=0; i<lambdas1.size(); i++) {
						LambdaExpressionObject lambda1 = lambdas1.get(i);
						LambdaExpressionObject lambda2 = lambdas2.get(i);
						processLambdas(lambda1, lambda2, replacementInfo, this);
					}
				}
				break;
			}
		}
		CompositeReplacement composite = mapping.containsCompositeReplacement();
		if(composite != null) {
			for(AbstractCodeFragment leaf : composite.getAdditionallyMatchedStatements1()) {
				mapping.inlinedVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
				if(mapping.isIdenticalWithInlinedVariable()) {
					List<LambdaExpressionObject> lambdas1 = leaf.getLambdas();
					List<LambdaExpressionObject> lambdas2 = mapping.getFragment2().getLambdas();
					if(lambdas1.size() == lambdas2.size()) {
						for(int i=0; i<lambdas1.size(); i++) {
							LambdaExpressionObject lambda1 = lambdas1.get(i);
							LambdaExpressionObject lambda2 = lambdas2.get(i);
							processLambdas(lambda1, lambda2, replacementInfo, this);
						}
					}
					break;
				}
			}
		}
	}

	private void checkForOtherPossibleMatchesForFragment2(List<? extends AbstractCodeFragment> leaves1,
			List<? extends AbstractCodeFragment> leaves2, AbstractCodeFragment leaf1, TreeSet<LeafMapping> mappingSet,
			Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions)
			throws RefactoringMinerTimedOutException {
		LeafMapping first = mappingSet.first();
		AbstractCodeFragment leaf2 = first.getFragment2();
		if(leaf2 instanceof AbstractExpression) {
			return;
		}
		if(parentMapper != null && parentMapper.alreadyMatched2(leaf2)) {
			return;
		}
		for(AbstractCodeFragment leaf : leaves1) {
			if(!leaf.equals(leaf1)) {
				int mappingsBefore = mappings.size();
				int refactoringsBefore = refactorings.size();
				ReplacementInfo replacementInfo = initializeReplacementInfo(leaf, leaf2, leaves1, leaves2);
				Set<Replacement> replacements = findReplacementsWithExactMatching(leaf, leaf2, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
				if (replacements != null) {
					int matchingMappings = 0;
					for(LeafMapping m : mappingSet) {
						int matchingReplacements = 0;
						int min = Math.min(replacements.size(), m.getReplacements().size());
						Iterator<Replacement> iterator1 = replacements.iterator();
						Iterator<Replacement> iterator2 = m.getReplacements().iterator();
						int counter = 0;
						while(iterator1.hasNext() && counter < min) {
							Replacement r1 = iterator1.next();
							Replacement r2 = iterator2.next();
							if(r1.getBefore().equals(r2.getBefore()) || r1.getAfter().equals(r2.getAfter())) {
								matchingReplacements++;
							}
							else if(r1 instanceof MethodInvocationReplacement && r2 instanceof MethodInvocationReplacement) {
								MethodInvocationReplacement mir1 = (MethodInvocationReplacement)r1;
								MethodInvocationReplacement mir2 = (MethodInvocationReplacement)r2;
								if((mir1.getInvokedOperationBefore().getName().equals(mir2.getInvokedOperationBefore().getName()) &&
										mir1.getInvokedOperationBefore().equalArguments(mir2.getInvokedOperationBefore())) ||
										(mir1.getInvokedOperationAfter().getName().equals(mir2.getInvokedOperationAfter().getName()) &&
											mir1.getInvokedOperationAfter().equalArguments(mir2.getInvokedOperationAfter()))) {
									matchingReplacements++;
								}
							}
							counter++;
						}
						if(matchingReplacements == replacements.size() || (matchingReplacements > 0 && matchingReplacements == min)) {
							matchingMappings++;
						}
					}
					boolean matchWithLessReplacements = mappingSet.size() == 1 && replacements.size() < mappingSet.first().getReplacements().size();
					boolean differentVariableDeclarationNumber =
							mappingSet.last().getFragment1().getVariableDeclarations().size() != mappingSet.last().getFragment2().getVariableDeclarations().size() &&
							leaf.getVariableDeclarations().size() == leaf2.getVariableDeclarations().size();
					boolean ownedByLambdaWithMultipleCallsToExtractedMethod = leaf.ownedByLambda() && callsToExtractedMethod > 1;
					if(matchingMappings == mappingSet.size() || matchWithLessReplacements || differentVariableDeclarationNumber || ownedByLambdaWithMultipleCallsToExtractedMethod) {
						LeafMapping mapping = createLeafMapping(leaf, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
						mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
						extractInlineVariableAnalysis(leaves1, leaves2, leaf, leaf2, mapping, replacementInfo);
						mappingSet.add(mapping);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
						for(int i = mappingsBefore; i < mappings.size(); i++) {
							this.mappings.remove(mappings.get(i));
						}
					}
				}
				else {
					//removed any nested mappings
					List<AbstractCodeMapping> orderedMappings = new ArrayList<AbstractCodeMapping>(mappings);
					for(int i=orderedMappings.size()-1; i>=0 && i>=mappingsBefore; i--) {
						AbstractCodeMapping m = orderedMappings.get(i);
						if(leaf.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && leaf2.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
								replacementInfo.lambdaMapperContainsMapping(m)) {
							removeMapping(m);
						}
						else {
							break;
						}
					}
					List<Refactoring> orderedRefactorings = new ArrayList<>(refactorings);
					for(int i=orderedRefactorings.size()-1; i>=0 && i>=refactoringsBefore; i--) {
						Refactoring r = orderedRefactorings.get(i);
						refactorings.remove(r);
					}
				}
			}
		}
	}

	private void checkForOtherPossibleMatchesForFragment1(List<? extends AbstractCodeFragment> leaves1,
			List<? extends AbstractCodeFragment> leaves2, AbstractCodeFragment leaf2, TreeSet<LeafMapping> mappingSet,
			Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions)
			throws RefactoringMinerTimedOutException {
		AbstractCodeFragment leaf1 = mappingSet.first().getFragment1();
		if(parentMapper != null && parentMapper.alreadyMatched1(leaf1)) {
			return;
		}
		CompositeStatementObject parent1 = leaf1.getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		for(AbstractCodeFragment leaf : leaves2) {
			if(!leaf.equals(leaf2)) {
				CompositeStatementObject parent2 = leaf.getParent();
				while(parent2 != null && parent2.getParent() != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
					parent2 = parent2.getParent();
				}
				boolean possibleExtractVariable = false;
				if(leaf2.getVariableDeclarations().size() > 0 && leaf.getString().equals(JAVA.RETURN_SPACE + leaf2.getVariableDeclarations().get(0).getVariableName() + JAVA.STATEMENT_TERMINATION)) {
					possibleExtractVariable = true;
				}
				if(leaf2.getVariableDeclarations().size() > 0 && leaf2.getVariableDeclarations().get(0).getInitializer() != null &&
						leaf1.getString().contains(leaf2.getVariableDeclarations().get(0).getInitializer().getString()) && leaf.getString().contains(leaf2.getVariableDeclarations().get(0).getVariableName())) {
					possibleExtractVariable = true;
				}
				boolean possibleInlineVariable = false;
				for(AbstractCodeFragment l1 : leaves1) {
					if(l1.getVariableDeclarations().size() > 0 && leaf1.getString().equals(JAVA.RETURN_SPACE + l1.getVariableDeclarations().get(0).getVariableName() + JAVA.STATEMENT_TERMINATION) &&
							l1.getVariableDeclarations().get(0).getInitializer() != null &&
							leaf.getString().equals(JAVA.RETURN_SPACE + l1.getVariableDeclarations().get(0).getInitializer().getString() + JAVA.STATEMENT_TERMINATION)) {
						possibleInlineVariable = true;
						break;
					}
				}
				if(parent1 != null && parent2 != null && (parent1.getString().equals(parent2.getString()) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) ||
						possibleExtractVariable || possibleInlineVariable)) {
					int mappingsBefore = mappings.size();
					int refactoringsBefore = refactorings.size();
					ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf, leaves1, leaves2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
						mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
						extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf, mapping, replacementInfo);
						boolean allowAdd = false;
						for(LeafMapping m : mappingSet) {
							if(mapping.levelParentEditDistanceSum() < m.levelParentEditDistanceSum()) {
								allowAdd = true;
								break;
							}
							if(mapping.identicalCommentsInParent() && !m.identicalCommentsInParent()) {
								allowAdd = true;
								break;
							}
						}
						boolean matchWithLessReplacements = mappingSet.size() == 1 && replacements.size() < mappingSet.first().getReplacements().size();
						if(allowAdd || nested || matchWithLessReplacements || mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
							mappingSet.add(mapping);
						}
					}
					else {
						//removed any nested mappings
						List<AbstractCodeMapping> orderedMappings = new ArrayList<AbstractCodeMapping>(mappings);
						for(int i=orderedMappings.size()-1; i>=0 && i>=mappingsBefore; i--) {
							AbstractCodeMapping m = orderedMappings.get(i);
							if(leaf1.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && leaf.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
									replacementInfo.lambdaMapperContainsMapping(m)) {
								removeMapping(m);
							}
							else {
								break;
							}
						}
						List<Refactoring> orderedRefactorings = new ArrayList<>(refactorings);
						for(int i=orderedRefactorings.size()-1; i>=0 && i>=refactoringsBefore; i--) {
							Refactoring r = orderedRefactorings.get(i);
							refactorings.remove(r);
						}
					}
				}
				else if(parent1 == null && (parent2 == null || parent2.getParent() == null) && (possibleExtractVariable || possibleInlineVariable ||
						(leaf1.getVariableDeclarations().toString().equals(leaf.getVariableDeclarations().toString()) && leaf1.getVariableDeclarations().size() > 0))) {
					int mappingsBefore = mappings.size();
					int refactoringsBefore = refactorings.size();
					ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf, leaves1, leaves2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
						mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
						extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf, mapping, replacementInfo);
						mappingSet.add(mapping);
					}
					else {
						//removed any nested mappings
						List<AbstractCodeMapping> orderedMappings = new ArrayList<AbstractCodeMapping>(mappings);
						for(int i=orderedMappings.size()-1; i>=0 && i>=mappingsBefore; i--) {
							AbstractCodeMapping m = orderedMappings.get(i);
							if(leaf1.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && leaf.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
									replacementInfo.lambdaMapperContainsMapping(m)) {
								removeMapping(m);
							}
							else {
								break;
							}
						}
						List<Refactoring> orderedRefactorings = new ArrayList<>(refactorings);
						for(int i=orderedRefactorings.size()-1; i>=0 && i>=refactoringsBefore; i--) {
							Refactoring r = orderedRefactorings.get(i);
							refactorings.remove(r);
						}
					}
				}
			}
		}
	}

	private TreeMap<Integer, LeafMapping> checkForOtherPossibleMatchesWithLineDistance2(List<? extends AbstractCodeFragment> leaves1,
			List<? extends AbstractCodeFragment> leaves2, AbstractCodeFragment leaf2, TreeSet<LeafMapping> mappingSet,
			Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions)
			throws RefactoringMinerTimedOutException {
		int exactMappingsBefore = 0;
		int inexactMappingsBefore = 0;
		int exactMappingsAfter = 0;
		int inexactMappingsAfter = 0;
		for(AbstractCodeMapping mapping : this.mappings) {
			if(leaf2.getLocationInfo().getStartLine() > mapping.getFragment2().getLocationInfo().getStartLine()) {
				if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
					exactMappingsBefore++;
				}
				else {
					inexactMappingsBefore++;
				}
			}
			else if(leaf2.getLocationInfo().getStartLine() < mapping.getFragment2().getLocationInfo().getStartLine()) {
				if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
					exactMappingsAfter++;
				}
				else {
					inexactMappingsAfter++;
				}
			}
		}
		TreeMap<Integer, LeafMapping> lineDistanceMap = new TreeMap<>();
		for(LeafMapping mapping : mappingSet) {
			int lineDistance = 0;
			if(exactMappingsBefore + inexactMappingsBefore == 0 && callsToExtractedMethod == 1) {
				lineDistance = lineDistanceFromExistingMappings1(mapping).getRight();
			}
			else if(exactMappingsBefore + inexactMappingsBefore < exactMappingsAfter + inexactMappingsAfter) {
				lineDistance = lineDistanceFromExistingMappings1(mapping).getLeft();
			}
			else {
				lineDistance = lineDistanceFromExistingMappings1(mapping).getMiddle();
			}
			if(!lineDistanceMap.containsKey(lineDistance)) {
				lineDistanceMap.put(lineDistance, mapping);
			}
		}
		AbstractCodeFragment leaf1 = lineDistanceMap.firstEntry().getValue().getFragment1();
		CompositeStatementObject parent1 = leaf1.getParent();
		while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent1 = parent1.getParent();
		}
		for(AbstractCodeFragment leaf : leaves2) {
			if(!leaf.equals(leaf2)) {
				CompositeStatementObject parent2 = leaf.getParent();
				while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
					parent2 = parent2.getParent();
				}
				if(parent1 != null && parent2 != null && parent1.getString().equals(parent2.getString())) {
					ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf, leaves1, leaves2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
						mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
						int lineDistance = 0;
						if(exactMappingsBefore + inexactMappingsBefore == 0 && callsToExtractedMethod == 1) {
							lineDistance = lineDistanceFromExistingMappings1(mapping).getRight();
						}
						else if(exactMappingsBefore + inexactMappingsBefore < exactMappingsAfter + inexactMappingsAfter) {
							lineDistance = lineDistanceFromExistingMappings1(mapping).getLeft();
						}
						else {
							lineDistance = lineDistanceFromExistingMappings1(mapping).getMiddle();
						}
						if(!lineDistanceMap.containsKey(lineDistance)) {
							lineDistanceMap.put(lineDistance, mapping);
						}
						else {
							Set<LeafMapping> set = new LinkedHashSet<>();
							set.add(lineDistanceMap.get(lineDistance));
							set.add(mapping);
							if(allUnderTheSameParent(set)) {
								lineDistanceMap.put(lineDistance, mapping);
							}
						}
					}
				}
			}
		}
		return lineDistanceMap;
	}

	private TreeMap<Integer, LeafMapping> checkForOtherPossibleMatchesWithLineDistance1(List<? extends AbstractCodeFragment> leaves1,
			List<? extends AbstractCodeFragment> leaves2, AbstractCodeFragment leaf1, TreeSet<LeafMapping> mappingSet,
			Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions)
			throws RefactoringMinerTimedOutException {
		int exactMappingsBefore = 0;
		int inexactMappingsBefore = 0;
		int exactMappingsAfter = 0;
		int inexactMappingsAfter = 0;
		for(AbstractCodeMapping mapping : this.mappings) {
			if(leaf1.getLocationInfo().getStartLine() > mapping.getFragment1().getLocationInfo().getStartLine()) {
				if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
					exactMappingsBefore++;
				}
				else {
					inexactMappingsBefore++;
				}
			}
			else if(leaf1.getLocationInfo().getStartLine() < mapping.getFragment1().getLocationInfo().getStartLine()) {
				if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
					exactMappingsAfter++;
				}
				else {
					inexactMappingsAfter++;
				}
			}
		}
		TreeMap<Integer, LeafMapping> lineDistanceMap = new TreeMap<>();
		for(LeafMapping mapping : mappingSet) {
			int lineDistance = 0;
			if(exactMappingsBefore + inexactMappingsBefore == 0 && callsToInlinedMethod == 1) {
				lineDistance = lineDistanceFromExistingMappings2(mapping).getRight();
			}
			else if(exactMappingsBefore + inexactMappingsBefore < exactMappingsAfter + inexactMappingsAfter) {
				lineDistance = lineDistanceFromExistingMappings2(mapping).getLeft();
			}
			else {
				lineDistance = lineDistanceFromExistingMappings2(mapping).getMiddle();
			}
			if(!lineDistanceMap.containsKey(lineDistance)) {
				lineDistanceMap.put(lineDistance, mapping);
			}
		}
		AbstractCodeFragment leaf2 = lineDistanceMap.firstEntry().getValue().getFragment2();
		CompositeStatementObject parent2 = leaf2.getParent();
		while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			parent2 = parent2.getParent();
		}
		for(AbstractCodeFragment leaf : leaves1) {
			if(!leaf.equals(leaf1)) {
				CompositeStatementObject parent1 = leaf.getParent();
				while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
					parent1 = parent1.getParent();
				}
				if(parent1 != null && parent2 != null && parent1.getString().equals(parent2.getString())) {
					ReplacementInfo replacementInfo = initializeReplacementInfo(leaf, leaf2, leaves1, leaves2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf, leaf2, parameterToArgumentMap, replacementInfo, equalNumberOfAssertions, this);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						mapping.addLambdaMappers(replacementInfo.getLambdaMappers());
						mapping.addSubExpressionMappings(replacementInfo.getSubExpressionMappings());
						int lineDistance = 0;
						if(exactMappingsBefore + inexactMappingsBefore == 0 && callsToInlinedMethod == 1) {
							lineDistance = lineDistanceFromExistingMappings2(mapping).getRight();
						}
						else if(exactMappingsBefore + inexactMappingsBefore < exactMappingsAfter + inexactMappingsAfter) {
							lineDistance = lineDistanceFromExistingMappings2(mapping).getLeft();
						}
						else {
							lineDistance = lineDistanceFromExistingMappings2(mapping).getMiddle();
						}
						if(!lineDistanceMap.containsKey(lineDistance)) {
							lineDistanceMap.put(lineDistance, mapping);
						}
						else {
							Set<LeafMapping> set = new LinkedHashSet<>();
							set.add(lineDistanceMap.get(lineDistance));
							set.add(mapping);
							if(allUnderTheSameParent(set)) {
								lineDistanceMap.put(lineDistance, mapping);
							}
						}
					}
				}
			}
		}
		return lineDistanceMap;
	}

	private void checkForMatchingSplitVariableDeclaration(AbstractCodeFragment leaf1, List<? extends AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions) {
		if(leaf1.getVariableDeclarations().size() > 0) {
			VariableDeclaration declaration1 = leaf1.getVariableDeclarations().get(0);
			Set<AbstractCodeFragment> matchingVariableDeclarations2 = new LinkedHashSet<>();
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(!alreadyMatched2(leaf2)) {
					if(leaf2.getVariableDeclarations().size() > 0) {
						VariableDeclaration declaration2 = leaf2.getVariableDeclarations().get(0);
						if(declaration1.getVariableName().equals(declaration2.getVariableName()) &&
								declaration1.getType() != null &&
								declaration1.getType().equals(declaration2.getType())) {
							matchingVariableDeclarations2.add(leaf2);
						}
					}
				}
			}
			if(matchingVariableDeclarations2.size() == 1) {
				LeafMapping mapping = createLeafMapping(leaf1, matchingVariableDeclarations2.iterator().next(), parameterToArgumentMap, equalNumberOfAssertions);
				addMapping(mapping);
				leaves2.remove(mapping.getFragment2());
			}
		}
	}

	private void checkForMatchingMergedVariableDeclaration(AbstractCodeFragment leaf2, List<? extends AbstractCodeFragment> leaves1,
			Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions) {
		if(leaf2.getVariableDeclarations().size() > 0) {
			VariableDeclaration declaration2 = leaf2.getVariableDeclarations().get(0);
			Set<AbstractCodeFragment> matchingVariableDeclarations1 = new LinkedHashSet<>();
			for(AbstractCodeFragment leaf1 : leaves1) {
				if(!alreadyMatched1(leaf1)) {
					if(leaf1.getVariableDeclarations().size() > 0) {
						VariableDeclaration declaration1 = leaf1.getVariableDeclarations().get(0);
						if(declaration1.getVariableName().equals(declaration2.getVariableName()) &&
								declaration1.getType() != null &&
								declaration1.getType().equals(declaration2.getType())) {
							matchingVariableDeclarations1.add(leaf1);
						}
					}
				}
			}
			if(matchingVariableDeclarations1.size() == 1) {
				LeafMapping mapping = createLeafMapping(matchingVariableDeclarations1.iterator().next(), leaf2, parameterToArgumentMap, equalNumberOfAssertions);
				addMapping(mapping);
				leaves1.remove(mapping.getFragment1());
			}
		}
	}

	private void checkForMergedVariableDeclaration(AbstractCodeFragment leaf2, List<? extends AbstractCodeFragment> leaves1,
			AbstractCodeMapping mapping, Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions, Set<AbstractCodeFragment> leaves1ToBeRemoved) {
		if(leaf2.getVariableDeclarations().size() > 0 && mapping.getFragment1().getVariableDeclarations().size() == 0 && mapping.getFragment2().getVariableDeclarations().size() > 0) {
			VariableDeclaration declaration2 = leaf2.getVariableDeclarations().get(0);
			AbstractCall call = mapping.getFragment2().invocationCoveringEntireFragment();
			Set<AbstractCodeFragment> matchingVariableDeclarations1 = new LinkedHashSet<>();
			for(AbstractCodeFragment leaf1 : leaves1) {
				if(!alreadyMatched1(leaf1)) {
					if(leaf1.getVariableDeclarations().size() > 0) {
						VariableDeclaration declaration1 = leaf1.getVariableDeclarations().get(0);
						boolean variableRenamed = false;
						for(Replacement r : mapping.getReplacements()) {
							if(r.getBefore().equals(declaration1.getVariableName()) &&
									r.getAfter().equals(declaration2.getVariableName())) {
								variableRenamed = true;
								break;
							}
						}
						boolean equalName = declaration1.getVariableName().equals(declaration2.getVariableName()) &&
								(mapping.getFragment1().getString().startsWith(declaration1.getVariableName() + JAVA.ASSIGNMENT) ||
								(call != null && call.getName().equals("collect")) ||
								mapping.getFragment1().getString().startsWith(declaration1.getVariableName() + ".addAll("));
						if((equalName || variableRenamed) && declaration1.getType() != null && declaration2.getType() != null &&
								(declaration1.getType().equals(declaration2.getType()) || declaration1.getType().getClassType().equals("var") || declaration2.getType().getClassType().equals("var") ||
										OperationInvocation.compatibleTypes(declaration1.getType(), declaration2.getType(), classDiff, modelDiff, false))) {
							matchingVariableDeclarations1.add(leaf1);
						}
					}
				}
			}
			if(matchingVariableDeclarations1.size() == 1) {
				AbstractCodeFragment matchingVariableDeclaration1 = matchingVariableDeclarations1.iterator().next();
				LeafMapping newMapping = createLeafMapping(matchingVariableDeclaration1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
				addMapping(newMapping);
				leaves1ToBeRemoved.add(matchingVariableDeclaration1);
				mapping.setMergedVariableDeclaration(true);
			}
			if(matchingVariableDeclarations1.size() == 0) {
				AbstractCodeMapping mappingToBeRemoved = null;
				for(AbstractCodeMapping m : mappings) {
					AbstractCodeFragment leaf1 = m.getFragment1();
					if(leaf1.getVariableDeclarations().size() > 0) {
						VariableDeclaration declaration1 = leaf1.getVariableDeclarations().get(0);
						boolean variableRenamed = false;
						for(Replacement r : mapping.getReplacements()) {
							if(r.getBefore().equals(declaration1.getVariableName()) &&
									r.getAfter().equals(declaration2.getVariableName())) {
								variableRenamed = true;
								break;
							}
						}
						boolean equalName = declaration1.getVariableName().equals(declaration2.getVariableName()) && mapping.getFragment1().getString().startsWith(declaration1.getVariableName() + JAVA.ASSIGNMENT);
						if((equalName || variableRenamed) && declaration1.getType() != null && declaration1.getType().equals(declaration2.getType())) {
							matchingVariableDeclarations1.add(leaf1);
							mappingToBeRemoved = m;
						}
					}
				}
				if(matchingVariableDeclarations1.size() == 1) {
					boolean sameVariableName = 
							mappingToBeRemoved.getFragment2().getVariableDeclarations().size() > 0 &&
							mappingToBeRemoved.getFragment2().getVariableDeclarations().get(0).getVariableName().equals(declaration2.getVariableName());
					if(!sameVariableName)
						removeMapping(mappingToBeRemoved);
					AbstractCodeFragment matchingVariableDeclaration1 = matchingVariableDeclarations1.iterator().next();
					LeafMapping newMapping = createLeafMapping(matchingVariableDeclaration1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
					addMapping(newMapping);
					leaves1ToBeRemoved.add(matchingVariableDeclaration1);
				}
			}
		}
	}

	private boolean checkForSplitVariableDeclaration(AbstractCodeFragment leaf1, List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2,
			AbstractCodeMapping mapping, Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions, Set<AbstractCodeFragment> leaves2ToBeRemoved) {
		if(leaf1.getVariableDeclarations().size() > 0 && mapping.getFragment1().getVariableDeclarations().size() > 0 && mapping.getFragment2().getVariableDeclarations().size() == 0) {
			VariableDeclaration declaration1 = leaf1.getVariableDeclarations().get(0);
			Set<AbstractCodeFragment> matchingVariableDeclarations2 = new LinkedHashSet<>();
			for(AbstractCodeFragment leaf2 : leaves2) {
				if(!alreadyMatched1(leaf2)) {
					if(leaf2.getVariableDeclarations().size() > 0) {
						VariableDeclaration declaration2 = leaf2.getVariableDeclarations().get(0);
						boolean variableRenamed = false;
						for(Replacement r : mapping.getReplacements()) {
							if(r.getBefore().equals(declaration1.getVariableName()) &&
									r.getAfter().equals(declaration2.getVariableName())) {
								variableRenamed = true;
								break;
							}
						}
						boolean equalName = declaration1.getVariableName().equals(declaration2.getVariableName()) &&
								(mapping.getFragment2().getString().startsWith(declaration1.getVariableName() + JAVA.ASSIGNMENT) ||
										mapping.getFragment2().getString().startsWith(declaration1.getVariableName() + ".addAll("));
						if((equalName || variableRenamed) && declaration1.getType() != null && declaration2.getType() != null &&
								(declaration1.getType().equals(declaration2.getType()) || declaration1.getType().getClassType().equals("var") || declaration2.getType().getClassType().equals("var") ||
										OperationInvocation.compatibleTypes(declaration1.getType(), declaration2.getType(), classDiff, modelDiff, false))) {
							matchingVariableDeclarations2.add(leaf2);
						}
					}
				}
			}
			if(matchingVariableDeclarations2.size() == 1) {
				AbstractCodeFragment matchingVariableDeclaration2 = matchingVariableDeclarations2.iterator().next();
				LeafMapping newMapping = createLeafMapping(leaf1, matchingVariableDeclaration2, parameterToArgumentMap, equalNumberOfAssertions);
				addMapping(newMapping);
				leaves2ToBeRemoved.add(matchingVariableDeclaration2);
				mapping.setSplitVariableDeclaration(true);
				return true;
			}
		}
		else if(leaf1.getVariableDeclarations().size() == 0 && mapping.getFragment1().getVariableDeclarations().size() == 0 && mapping.getFragment2().getVariableDeclarations().size() > 0) {
			VariableDeclaration declaration2 = mapping.getFragment2().getVariableDeclarations().get(0);
			AbstractCall call = mapping.getFragment2().invocationCoveringEntireFragment();
			boolean streamCollector = call != null && call.getName().equals("collect");
			List<LeafExpression> patternInstanceExpressions = mapping.getFragment2().getPatternInstanceofExpressions();
			Set<AbstractCodeFragment> matchingVariableDeclarations1 = new LinkedHashSet<>();
			for(AbstractCodeFragment l1 : leaves1) {
				if(!alreadyMatched1(l1)) {
					if(l1.getVariableDeclarations().size() > 0) {
						VariableDeclaration declaration1 = l1.getVariableDeclarations().get(0);
						boolean equalName = declaration1.getVariableName().equals(declaration2.getVariableName()) && !mapping.getFragment1().getString().startsWith(declaration2.getVariableName() + JAVA.ASSIGNMENT);
						if(equalName && declaration1.getType() != null && declaration1.getType().equals(declaration2.getType())) {
							boolean foundInPatternInstance = false;
							for(LeafExpression patternInstanceExpression : patternInstanceExpressions) {
								if(patternInstanceExpression.getString().endsWith(" " + declaration1.getVariableName())) {
									foundInPatternInstance = true;
									break;
								}
							}
							if(!foundInPatternInstance) {
								matchingVariableDeclarations1.add(l1);
							}
						}
					}
				}
			}
			if(matchingVariableDeclarations1.size() == 1 && !streamCollector && mapping.containsCompositeReplacement() == null) {
				return false;
			}
		}
		return true;
	}

	private boolean codeUnderIfMovedUnderElse(TreeSet<LeafMapping> mappingSet) {
		boolean codeUnderIfMovedUnderElse = false;
		for(AbstractCodeMapping m : mappingSet) {
			if(m.getFragment1().getDepth() == m.getFragment2().getDepth() && m.getFragment1().getIndex() == m.getFragment2().getIndex()) {
				break;
			}
			AbstractCodeFragment child1 = m.getFragment1();
			AbstractCodeFragment child2 = m.getFragment2();
			CompositeStatementObject parent1 = child1.getParent();
			CompositeStatementObject parent2 = child2.getParent();
			boolean isUnderIf1 = false;
			boolean isUnderElse1 = false;
			while(parent1 != null) {
				if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) ||
						parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
					break;
				}
				if(isIfBranch(child1, parent1)) {
					isUnderIf1 = true;
					break;
				}
				if(isElseBranch(child1, parent1)) {
					isUnderElse1 = true;
					break;
				}
				child1 = parent1;
				parent1 = parent1.getParent();
			}
			boolean isUnderIf2 = false;
			boolean isUnderElse2 = false;
			while(parent2 != null) {
				if(parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) ||
						parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
					break;
				}
				if(isIfBranch(child2, parent2)) {
					isUnderIf2 = true;
					break;
				}
				if(isElseBranch(child2, parent2)) {
					isUnderElse2 = true;
					break;
				}
				child2 = parent2;
				parent2 = parent2.getParent();
			}
			codeUnderIfMovedUnderElse = isUnderIf1 != isUnderIf2 && isUnderElse1 != isUnderElse2;
			if(isUnderElse1 && isUnderElse2) {
				boolean isParentUnderIf1 = false;
				boolean isParentUnderElse1 = false;
				if(parent1.getParent() != null && parent1.getParent().getParent() != null) {
					if(isIfBranch(parent1.getParent(), parent1.getParent().getParent())) {
						isParentUnderIf1 = true;
					}
					else if(isElseBranch(parent1.getParent(), parent1.getParent().getParent())) {
						isParentUnderElse1 = true;
					}
				}
				boolean isParentUnderIf2 = false;
				boolean isParentUnderElse2 = false;
				if(parent2.getParent() != null && parent2.getParent().getParent() != null) {
					if(isIfBranch(parent2.getParent(), parent2.getParent().getParent())) {
						isParentUnderIf2 = true;
					}
					else if(isElseBranch(parent2.getParent(), parent2.getParent().getParent())) {
						isParentUnderElse2 = true;
					}
				}
				codeUnderIfMovedUnderElse = isParentUnderIf1 != isParentUnderIf2 && isParentUnderElse1 != isParentUnderElse2;
			}
			if(codeUnderIfMovedUnderElse) {
				break;
			}
		}
		return codeUnderIfMovedUnderElse;
	}
/*
	private ScopedMappingData findParentMappingContainingOperationInvocation(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2) {
		//Extract Method scenario
		ScopedMappingData data = new ScopedMappingData();
		AbstractCodeFragment statementContainingOperationInvocation = null;
		for(AbstractCodeFragment leaf : parentMapper.getNonMappedLeavesT2()) {
			if(leaf.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
				statementContainingOperationInvocation = leaf;
				findPreviousAndNextParentMappingForExtract(leaves1, leaves2, data);
				break;
			}
		}
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
			if(mapping instanceof LeafMapping) {
				if(mapping.getFragment2().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
					statementContainingOperationInvocation = mapping.getFragment2();
					findPreviousAndNextParentMappingForExtract(leaves1, leaves2, data);
				}
			}
			if(statementContainingOperationInvocation != null) {
				if(mapping.getFragment2().equals(statementContainingOperationInvocation.getParent())) {
					data.setParentMapping(mapping);
					return data;
				}
				if(statementContainingOperationInvocation.getParent() != null && statementContainingOperationInvocation.getParent().getParent() != null &&
						statementContainingOperationInvocation.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						mapping.getFragment2().equals(statementContainingOperationInvocation.getParent().getParent())) {
					data.setParentMapping(mapping);
					return data;
				}
			}
		}
		if(statementContainingOperationInvocation != null) {
			return data;
		}
		//Inline Method scenario
		for(AbstractCodeFragment leaf : parentMapper.getNonMappedLeavesT1()) {
			if(leaf.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
				statementContainingOperationInvocation = leaf;
				findPreviousAndNextParentMappingForInline(leaves1, leaves2, data);
				break;
			}
		}
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
			if(mapping instanceof LeafMapping) {
				if(mapping.getFragment1().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
					statementContainingOperationInvocation = mapping.getFragment1();
					findPreviousAndNextParentMappingForInline(leaves1, leaves2, data);
				}
			}
			if(statementContainingOperationInvocation != null) {
				if(mapping.getFragment1().equals(statementContainingOperationInvocation.getParent())) {
					data.setParentMapping(mapping);
					return data;
				}
				if(statementContainingOperationInvocation.getParent() != null && statementContainingOperationInvocation.getParent().getParent() != null &&
						statementContainingOperationInvocation.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						mapping.getFragment1().equals(statementContainingOperationInvocation.getParent().getParent())) {
					data.setParentMapping(mapping);
					return data;
				}
			}
		}
		return data;
	}

	private void findPreviousAndNextParentMappingForExtract(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2, ScopedMappingData data) {
		AbstractCodeFragment firstLeaf2 = null;
		AbstractCodeFragment lastLeaf2 = null;
		for(AbstractCodeFragment fragment : leaves2) {
			if(firstLeaf2 == null && !fragment.getString().startsWith(JAVA.RETURN) && !(fragment.getVariableDeclarations().size() > 0 && fragment.getVariableDeclarations().get(0).getInitializer() == null)) {
				firstLeaf2 = fragment;
			}
			else if(firstLeaf2 != null && fragment.getLocationInfo().getStartLine() < firstLeaf2.getLocationInfo().getStartLine()) {
				firstLeaf2 = fragment;
			}
			if(lastLeaf2 == null) {
				lastLeaf2 = fragment;
			}
			else if(fragment.getLocationInfo().getStartLine() > lastLeaf2.getLocationInfo().getStartLine() && !fragment.getString().startsWith(JAVA.RETURN)) {
				lastLeaf2 = fragment;
			}
		}
		List<AbstractCodeFragment> matchingFirstLeaves1 = new ArrayList<>();
		List<AbstractCodeFragment> matchingLastLeaves1 = new ArrayList<>();
		for(AbstractCodeFragment fragment : leaves1) {
			if(firstLeaf2 != null && fragment.getString().equals(firstLeaf2.getString())) {
				matchingFirstLeaves1.add(fragment);
			}
			if(lastLeaf2 != null && fragment.getString().equals(lastLeaf2.getString())) {
				matchingLastLeaves1.add(fragment);
			}
		}
		if(matchingFirstLeaves1.size() > 0 && matchingLastLeaves1.size() > 0) {
			int minBefore = Integer.MAX_VALUE;
			int minAfter = Integer.MAX_VALUE;
			AbstractCodeMapping previousMapping = null;
			AbstractCodeMapping nextMapping = null;
			for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
				int lineDistanceBefore = matchingFirstLeaves1.get(0).getLocationInfo().getStartLine() - mapping.getFragment1().getLocationInfo().getStartLine();
				if(lineDistanceBefore > 0 && lineDistanceBefore < minBefore) {
					previousMapping = mapping;
					minBefore = lineDistanceBefore;
				}
				int lineDistanceAfter = mapping.getFragment1().getLocationInfo().getStartLine() - matchingLastLeaves1.get(matchingLastLeaves1.size()-1).getLocationInfo().getStartLine();
				if(lineDistanceAfter > 0 && lineDistanceAfter < minAfter) {
					nextMapping = mapping;
					minAfter = lineDistanceAfter;
				}
			}
			data.setPreviousMapping(previousMapping);
			data.setNextMapping(nextMapping);
		}
	}

	private void findPreviousAndNextParentMappingForInline(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2, ScopedMappingData data) {
		AbstractCodeFragment firstLeaf1 = null;
		AbstractCodeFragment lastLeaf1 = null;
		for(AbstractCodeFragment fragment : leaves1) {
			if(firstLeaf1 == null && !fragment.getString().startsWith(JAVA.RETURN) && !(fragment.getVariableDeclarations().size() > 0 && fragment.getVariableDeclarations().get(0).getInitializer() == null)) {
				firstLeaf1 = fragment;
			}
			else if(firstLeaf1 != null && fragment.getLocationInfo().getStartLine() < firstLeaf1.getLocationInfo().getStartLine()) {
				firstLeaf1 = fragment;
			}
			if(lastLeaf1 == null) {
				lastLeaf1 = fragment;
			}
			else if(fragment.getLocationInfo().getStartLine() > lastLeaf1.getLocationInfo().getStartLine() && !fragment.getString().startsWith(JAVA.RETURN)) {
				lastLeaf1 = fragment;
			}
		}
		List<AbstractCodeFragment> matchingFirstLeaves2 = new ArrayList<>();
		List<AbstractCodeFragment> matchingLastLeaves2 = new ArrayList<>();
		for(AbstractCodeFragment fragment : leaves2) {
			if(firstLeaf1 != null && fragment.getString().equals(firstLeaf1.getString())) {
				matchingFirstLeaves2.add(fragment);
			}
			if(lastLeaf1 != null && fragment.getString().equals(lastLeaf1.getString())) {
				matchingLastLeaves2.add(fragment);
			}
		}
		if(matchingFirstLeaves2.size() > 0 && matchingLastLeaves2.size() > 0) {
			int minBefore = Integer.MAX_VALUE;
			int minAfter = Integer.MAX_VALUE;
			AbstractCodeMapping previousMapping = null;
			AbstractCodeMapping nextMapping = null;
			for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
				int lineDistanceBefore = matchingFirstLeaves2.get(0).getLocationInfo().getStartLine() - mapping.getFragment2().getLocationInfo().getStartLine();
				if(lineDistanceBefore > 0 && lineDistanceBefore < minBefore) {
					previousMapping = mapping;
					minBefore = lineDistanceBefore;
				}
				int lineDistanceAfter = mapping.getFragment2().getLocationInfo().getStartLine() - matchingLastLeaves2.get(matchingLastLeaves2.size()-1).getLocationInfo().getStartLine();
				if(lineDistanceAfter > 0 && lineDistanceAfter < minAfter) {
					nextMapping = mapping;
					minAfter = lineDistanceAfter;
				}
			}
			data.setPreviousMapping(previousMapping);
			data.setNextMapping(nextMapping);
		}
	}
*/
	private AbstractCodeFragment statementContainingExtractedOperationInvocation() {
		if(operationInvocation == null) {
			return null;
		}
		//Extract Method scenario 
		AbstractCodeFragment statementContainingOperationInvocation = null; 
		for(AbstractCodeFragment leaf : parentMapper.getNonMappedLeavesT2()) { 
			if(leaf.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
				statementContainingOperationInvocation = leaf; 
				break; 
			} 
		} 
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) { 
			if(mapping instanceof LeafMapping) { 
				if(mapping.getFragment2().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
					statementContainingOperationInvocation = mapping.getFragment2(); 
				} 
			} 
		}
		if(nested && parentMapper.getParentMapper() != null) {
			for(AbstractCodeFragment leaf : parentMapper.getParentMapper().getNonMappedLeavesT2()) { 
				AbstractCall call = leaf.invocationCoveringEntireFragment();
				if(call != null && call.matchesOperation(parentMapper.getContainer2(), parentMapper.getParentMapper().getContainer2(), classDiff, modelDiff)) {
					statementContainingOperationInvocation = leaf; 
					break; 
				}
			}
		}
		return statementContainingOperationInvocation;
	}

	private AbstractCodeFragment statementContainingInlinedOperationInvocation() {
		if(operationInvocation == null) {
			return null;
		}
		//Inline Method scenario 
		AbstractCodeFragment statementContainingOperationInvocation = null; 
		for(AbstractCodeFragment leaf : parentMapper.getNonMappedLeavesT1()) { 
			if(leaf.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
				statementContainingOperationInvocation = leaf; 
				break; 
			} 
		} 
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) { 
			if(mapping instanceof LeafMapping) { 
				if(mapping.getFragment1().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
					statementContainingOperationInvocation = mapping.getFragment1(); 
				} 
			} 
		}
		return statementContainingOperationInvocation;
	}

	private AbstractCodeMapping findParentMappingContainingOperationInvocation() {
		if(operationInvocation == null) {
			return null;
		}
		//Extract Method scenario 
		AbstractCodeFragment statementContainingOperationInvocation = null; 
		for(AbstractCodeFragment leaf : parentMapper.getNonMappedLeavesT2()) { 
			if(leaf.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
				statementContainingOperationInvocation = leaf; 
				break; 
			} 
		} 
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) { 
			if(mapping instanceof LeafMapping) { 
				if(mapping.getFragment2().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
					statementContainingOperationInvocation = mapping.getFragment2(); 
				} 
			} 
			if(statementContainingOperationInvocation != null) { 
				if(mapping.getFragment2().equals(statementContainingOperationInvocation.getParent())) { 
					boolean extractedStatement = parentMapper.extractedStatements.containsKey(this.container2) && parentMapper.extractedStatements.get(this.container2).contains(mapping.getFragment1());
					for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
						if(childMapper.mappingHashcodesT1.contains(mapping.getFragment1().hashCode())) {
							extractedStatement = true;
						}
					}
					if(!extractedStatement)
						return mapping; 
				} 
				if(statementContainingOperationInvocation.getParent() != null && statementContainingOperationInvocation.getParent().getParent() != null && 
						statementContainingOperationInvocation.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && 
						mapping.getFragment2().equals(statementContainingOperationInvocation.getParent().getParent())) { 
					return mapping; 
				} 
				if(operationInvocation.getContainer() instanceof LambdaExpressionObject && mapping.getFragment2().getLambdas().contains(operationInvocation.getContainer())) {
					return mapping;
				}
			} 
		} 
		if(nested && parentMapper.getParentMapper() != null) {
			for(AbstractCodeFragment leaf : parentMapper.getParentMapper().getNonMappedLeavesT2()) { 
				AbstractCall call = leaf.invocationCoveringEntireFragment();
				if(call != null && call.matchesOperation(parentMapper.getContainer2(), parentMapper.getParentMapper().getContainer2(), classDiff, modelDiff)) {
					statementContainingOperationInvocation = leaf; 
					break; 
				}
			}
			for(AbstractCodeMapping mapping : parentMapper.getParentMapper().getMappings()) { 
				if(statementContainingOperationInvocation != null) { 
					if(mapping.getFragment2().equals(statementContainingOperationInvocation.getParent())) { 
						return mapping; 
					} 
					if(statementContainingOperationInvocation.getParent() != null && statementContainingOperationInvocation.getParent().getParent() != null && 
							statementContainingOperationInvocation.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && 
							mapping.getFragment2().equals(statementContainingOperationInvocation.getParent().getParent())) { 
						return mapping; 
					} 
				}
			}
		}
		//check if the parent statement is the body of parentMapper.container2
		if(statementContainingOperationInvocation != null && statementContainingOperationInvocation.getParent() != null && container2.getBody() != null &&
				statementContainingOperationInvocation.getParent().equals(parentMapper.container2.getBody().getCompositeStatement())) {
			return null;
		}
		//Inline Method scenario 
		for(AbstractCodeFragment leaf : parentMapper.getNonMappedLeavesT1()) { 
			if(leaf.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
				statementContainingOperationInvocation = leaf; 
				break; 
			} 
		} 
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) { 
			if(mapping instanceof LeafMapping) { 
				if(mapping.getFragment1().getLocationInfo().subsumes(operationInvocation.getLocationInfo())) { 
					statementContainingOperationInvocation = mapping.getFragment1(); 
				} 
			} 
			if(statementContainingOperationInvocation != null) { 
				if(mapping.getFragment1().equals(statementContainingOperationInvocation.getParent())) { 
					return mapping; 
				} 
				if(statementContainingOperationInvocation.getParent() != null && statementContainingOperationInvocation.getParent().getParent() != null && 
						statementContainingOperationInvocation.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && 
						mapping.getFragment1().equals(statementContainingOperationInvocation.getParent().getParent())) { 
					return mapping; 
				} 
				if(operationInvocation.getContainer() instanceof LambdaExpressionObject && mapping.getFragment1().getLambdas().contains(operationInvocation.getContainer())) {
					return mapping;
				}
			} 
		} 
		return null; 
	} 

	private boolean duplicateMapping1(AbstractCodeMapping mapping) {
		AbstractCodeFragment fragment1 = null;
		for(AbstractCodeMapping parentMapping : this.getMappings()) {
			if(mapping.getFragment2().equals(parentMapping.getFragment2())) {
				fragment1 = parentMapping.getFragment1();
				break;
			}
		}
		if(fragment1 != null) {
			for(AbstractCodeMapping parentMapping : this.getMappings()) {
				if(parentMapping.getFragment1().equals(fragment1) && !mapping.getFragment2().equals(parentMapping.getFragment2())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean duplicateMapping2(AbstractCodeMapping mapping) {
		AbstractCodeFragment fragment2 = null;
		for(AbstractCodeMapping parentMapping : this.getMappings()) {
			if(mapping.getFragment1().equals(parentMapping.getFragment1())) {
				fragment2 = parentMapping.getFragment2();
				break;
			}
		}
		if(fragment2 != null) {
			for(AbstractCodeMapping parentMapping : this.getMappings()) {
				if(parentMapping.getFragment2().equals(fragment2) && !mapping.getFragment1().equals(parentMapping.getFragment1())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean duplicateMappingInParentMapper(TreeSet<? extends AbstractCodeMapping> mappingSet) {
		if(parentMapper != null) {
			if(mappingSet.size() > 1) {
				Set<AbstractCodeFragment> fragments2 = new LinkedHashSet<>();
				int matchedFragments1 = 0;
				for(AbstractCodeMapping mapping : mappingSet) {
					for(AbstractCodeMapping parentMapping : parentMapper.getMappings()) {
						if(mapping.getFragment1().equals(parentMapping.getFragment1())) {
							fragments2.add(parentMapping.getFragment2());
							matchedFragments1++;
							break;
						}
					}
				}
				if(matchedFragments1 > 1 && fragments2.size() == 1) {
					return true;
				}
				if(matchedFragments1 == 0) {
					Set<AbstractCodeFragment> parents1 = new LinkedHashSet<>();
					for(AbstractCodeMapping mapping : mappingSet) {
						CompositeStatementObject parent1 = mapping.getFragment1().getParent();
						while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
							parent1 = parent1.getParent();
						}
						if(parent1 != null) {
							parents1.add(parent1);
							for(AbstractCodeMapping parentMapping : parentMapper.getMappings()) {
								if(parent1.equals(parentMapping.getFragment1())) {
									fragments2.add(parentMapping.getFragment2());
									matchedFragments1++;
									break;
								}
							}
						}
					}
					if(parents1.size() > 1 && matchedFragments1 > 1 && fragments2.size() == 1) {
						return true;
					}
				}
			}
			else if(mappingSet.size() == 1) {
				AbstractCodeMapping mapping = mappingSet.first();
				if(mapping instanceof CompositeStatementObjectMapping && ((CompositeStatementObjectMapping)mapping).getCompositeChildMatchingScore() == 1.0) {
					return false;
				}
				AbstractCodeFragment fragment2 = null;
				for(AbstractCodeMapping parentMapping : parentMapper.getMappings()) {
					if(mapping.getFragment1().equals(parentMapping.getFragment1())) {
						fragment2 = parentMapping.getFragment2();
						break;
					}
				}
				if(fragment2 != null) {
					for(AbstractCodeMapping parentMapping : parentMapper.getMappings()) {
						if(parentMapping.getFragment2().equals(fragment2) && !mapping.getFragment1().equals(parentMapping.getFragment1())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private Set<AbstractCodeMapping> splitToMultipleAssignments(TreeSet<? extends AbstractCodeMapping> mappingSet) {
		if(mappingSet.size() == 1) {
			return Set.of(mappingSet.first());
		}
		if(container1.equals(container2) || (parentMapper != null && !nested)) {
			Set<AbstractCodeMapping> included = new LinkedHashSet<AbstractCodeMapping>();
			VariableDeclaration declaration = null;
			int count = 0;
			for(AbstractCodeMapping mapping : mappingSet) {
				if(mapping.isIdenticalWithExtractedVariable()) {
					included.add(mapping);
					count++;
					for(Refactoring r : mapping.getRefactorings()) {
						if(r instanceof ExtractVariableRefactoring) {
							ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
							declaration = extract.getVariableDeclaration();
							break;
						}
					}
				}
				if(declaration != null && mapping.getFragment2().getString().startsWith(declaration.getVariableName())) {
					for(Replacement r : mapping.getReplacements()) {
						if(r instanceof IntersectionReplacement) {
							IntersectionReplacement intersectionReplacement = (IntersectionReplacement)r;
							if(intersectionReplacement.getSubExpressionMappings().size() > 0) {
								included.add(mapping);
								break;
							}
						}
					}
				}
			}
			if(count == 1 && count < included.size()) {
				return included;
			}
		}
		return Set.of(mappingSet.first());
	}

	private Set<AbstractCodeMapping> movedInIfElseIfBranch(TreeSet<? extends AbstractCodeMapping> mappingSet) {
		if(mappingSet.size() == 1) {
			return Set.of(mappingSet.first());
		}
		if(container1.equals(container2) || (parentMapper != null && !nested)) {
			Map<CompositeStatementObject, AbstractCodeMapping> parentMap = new LinkedHashMap<>();
			Map<CompositeStatementObject, AbstractCodeMapping> grandParentMap = new LinkedHashMap<>();
			boolean ifFound = false, elseIfFound = false, elseFound = false, ifBecomingElseIf = false;
			for(AbstractCodeMapping mapping : mappingSet) {
				AbstractCodeFragment fragment = mapping.getFragment2();
				if(fragment.getParent() != null && fragment.getParent().getParent() != null) {
					boolean directlyNested = false;
					boolean isBlock = fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
					boolean isWithinIfBranch = isBlock ? isIfBranch(fragment, fragment.getParent()) : isIfBranch(fragment.getParent(), fragment.getParent().getParent());
					if(!isBlock && fragment.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							fragment.getParent().getStatements().indexOf(fragment) == 0) {
						isWithinIfBranch = true;
						directlyNested = true;
					}
					boolean isWithinElseBranch = isBlock ? isElseBranch(fragment, fragment.getParent()) : isElseBranch(fragment.getParent(), fragment.getParent().getParent());
					if(!isBlock && fragment.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							fragment.getParent().getStatements().size() == 2 && fragment.getParent().getStatements().indexOf(fragment) == 1) {
						isWithinElseBranch = true;
						directlyNested = true;
					}
					boolean isWithinElseIfBranch = false;
					CompositeStatementObject grandGrandParent = fragment.getParent().getParent().getParent();
					if(grandGrandParent != null) {
						isWithinElseIfBranch = isBlock ? isElseIfBranch(fragment.getParent(), fragment.getParent().getParent()) : isElseIfBranch(fragment.getParent().getParent(), grandGrandParent);
						if(!isBlock && fragment.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
								fragment.getParent().getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
								fragment.getParent().getParent().getStatements().size() == 2 && fragment.getParent().getParent().getStatements().indexOf(fragment.getParent()) == 1) {
							isWithinElseIfBranch = true;
							directlyNested = true;
						}
						if(mapping.getFragment1().getParent() != null) {
							CompositeStatementObject grandParent1 = mapping.getFragment1().getParent().getParent();
							CompositeStatementObject grandParent2 = fragment.getParent().getParent();
							while(grandParent1 != null && grandParent2 != null && grandParent1.getString().equals(grandParent2.getString())) {
								if(ifBecomingElseIf(grandParent1, grandParent2)) {
									ifBecomingElseIf = true;
									isWithinElseIfBranch = true;
									break;
								}
								grandParent1 = grandParent1.getParent();
								grandParent2 = grandParent2.getParent();
							}
						}
					}
					int depthDifference = fragment.getDepth() - mapping.getFragment1().getDepth();
					if(isWithinIfBranch && !isWithinElseIfBranch && depthDifference >= 2 - (directlyNested ? 2 : 0)) {
						if(!parentMap.containsKey(fragment.getParent())) {
							parentMap.put(fragment.getParent(), mapping);
							if(!directlyNested) {
								if(grandGrandParent != null && grandGrandParent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									grandParentMap.put(grandGrandParent, mapping);
								}
								else {
									grandParentMap.put(fragment.getParent().getParent(), mapping);
								}
							}
							ifFound = true;
						}
					}
					else if(isWithinElseIfBranch && depthDifference >= 3 - (directlyNested || ifBecomingElseIf ? 2 : 0)) {
						if(!parentMap.containsKey(fragment.getParent())) {
							parentMap.put(fragment.getParent(), mapping);
							if(!directlyNested) {
								if(grandGrandParent != null && grandGrandParent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									grandParentMap.put(grandGrandParent, mapping);
								}
								else {
									grandParentMap.put(fragment.getParent().getParent(), mapping);
								}
							}
							elseIfFound = true;
						}
					}
					else if(isWithinElseBranch && depthDifference >= 2 - (directlyNested ? 2 : 0)) {
						if(!parentMap.containsKey(fragment.getParent())) {
							parentMap.put(fragment.getParent(), mapping);
							if(!directlyNested) {
								if(grandGrandParent != null && grandGrandParent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									grandParentMap.put(grandGrandParent, mapping);
								}
								else {
									grandParentMap.put(fragment.getParent().getParent(), mapping);
								}
							}
							elseFound = true;
						}
					}
				}
			}
			boolean passDueToIfBecomingElseIf = ifBecomingElseIf && mappingSet.size() == parentMap.size();
			boolean elseCondition = false;
			if(parentMap.size() == grandParentMap.size()) {
				elseCondition = true;
			}
			else if(grandParentMap.size() == 1 && !(mappingSet.first().getFragment1() instanceof CompositeStatementObject)) {
				CompositeStatementObject grandParent = grandParentMap.keySet().iterator().next();
				if(grandParent.getParent() == null) {
					elseCondition = true;
				}
				else if(isElseBranch(grandParent, grandParent.getParent())) {
					elseCondition = true;
				}
				else if(isIfBranch(grandParent, grandParent.getParent()) && !hasElseBranch(grandParent.getParent()) && !hasElseIfBranch(grandParent.getParent())) {
					int identicalCount = 0;
					for(AbstractCodeMapping mapping : mappingSet) {
						if(mapping.getFragment2().getString().contains(mapping.getFragment1().getString()) || mapping.getFragment1().getString().contains(mapping.getFragment2().getString())) {
							identicalCount++;
						}
					}
					if(identicalCount == 0)
						elseCondition = true;
				}
				else {
					List<CompositeStatementObject> ifParents = extractIfParentsFromBlocks(parentMap.keySet());
					Set<CompositeStatementObject> grandParents = new LinkedHashSet<CompositeStatementObject>();
					for(CompositeStatementObject ifParent : ifParents) {
						CompositeStatementObject parent = ifParent.getParent();
						while(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
							parent = parent.getParent(); 
						}
						if(parent != null) {
							grandParents.add(parent);
						}
					}
					if(container1.getBody() != null && grandParents.size() == 1) {
						CompositeStatementObject grandParent2 = grandParents.iterator().next();
						List<CompositeStatementObject> innerNodes1 = container1.getBody().getCompositeStatement().getInnerNodes();
						for(CompositeStatementObject innerNode1 : innerNodes1) {
							if(innerNode1.getString().equals(grandParent2.getString())) {
								elseCondition = true;
								break;
							}
						}
					}
				}
			}
			if(ifFound && (elseIfFound || (elseFound && elseCondition)) && (ifElseIfChain(parentMap.keySet()) || passDueToIfBecomingElseIf)) {
				Set<String> variableDeclarationNames1 = new LinkedHashSet<>();
				Set<String> variableDeclarationNames2 = new LinkedHashSet<>();
				for(AbstractCodeMapping mapping : parentMap.values()) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					for(VariableDeclaration variable : fragment1.getVariableDeclarations()) {
						variableDeclarationNames1.add(variable.getVariableName());
					}
					for(VariableDeclaration variable : fragment2.getVariableDeclarations()) {
						variableDeclarationNames2.add(variable.getVariableName());
					}
					if(fragment1.assignmentInvocationCoveringEntireStatement() != null || fragment1.assignmentCreationCoveringEntireStatement() != null) {
						if(fragment1.getString().contains(JAVA.ASSIGNMENT)) {
							String assignedVariable = fragment1.getString().substring(0, fragment1.getString().indexOf(JAVA.ASSIGNMENT));
							variableDeclarationNames1.add(assignedVariable);
						}
					}
					if(fragment2.assignmentInvocationCoveringEntireStatement() != null || fragment2.assignmentCreationCoveringEntireStatement() != null) {
						if(fragment2.getString().contains(JAVA.ASSIGNMENT)) {
							String assignedVariable = fragment2.getString().substring(0, fragment2.getString().indexOf(JAVA.ASSIGNMENT));
							variableDeclarationNames2.add(assignedVariable);
						}
					}
				}
				AbstractCodeFragment fragment1 = mappingSet.first().getFragment1();
				boolean ifElseIfChainNestedUnderLoop = nestedUnderLoop(parentMap.keySet());
				boolean fragment1NestedUnderLoop = false;
				AbstractCodeFragment f1 = fragment1;
				while(f1.getParent() != null && f1.getParent().getParent() != null) {
					if(f1.getParent().isLoop() || f1.getParent().getParent().isLoop()) {
						fragment1NestedUnderLoop = true;
						break;
					}
					f1 = f1.getParent();
				}
				if(!(fragment1 instanceof AbstractExpression) && ifElseIfChainNestedUnderLoop == fragment1NestedUnderLoop && variableDeclarationNames1.equals(variableDeclarationNames2)) {
					boolean fragment1IsInsideIfElseIf = false;
					if(fragment1.getParent() != null && fragment1.getParent().getParent() != null) {
						boolean directlyNested = false;
						boolean isBlock = fragment1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
						boolean isWithinIfBranch = isBlock ? isIfBranch(fragment1, fragment1.getParent()) : isIfBranch(fragment1.getParent(), fragment1.getParent().getParent());
						if(!isBlock && fragment1.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
								fragment1.getParent().getStatements().indexOf(fragment1) == 0) {
							isWithinIfBranch = true;
							directlyNested = true;
						}
						//boolean isWithinElseBranch = isElseBranch(fragment1.getParent(), fragment1.getParent().getParent());
						//boolean isWithinElseIfBranch = false;
						//if(fragment1.getParent().getParent().getParent() != null) {
						//	isWithinElseIfBranch = isElseIfBranch(fragment1.getParent().getParent(), fragment1.getParent().getParent().getParent());
						//}
						boolean hasElseIfOrElseBranch = false;
						if(!directlyNested)
							hasElseIfOrElseBranch = hasElseBranch(fragment1.getParent().getParent()) || hasElseIfBranch(fragment1.getParent().getParent());
						else
							hasElseIfOrElseBranch = hasElseBranch(fragment1.getParent()) || hasElseIfBranch(fragment1.getParent());
						if(isWithinIfBranch && hasElseIfOrElseBranch) {
							fragment1IsInsideIfElseIf = true;
						}
					}
					if(parentMapper != null && mappingSet.first().getFragment2().getParent() != null && mappingSet.first().getFragment2().getParent().getParent() != null && parentMapper.alreadyMatched2(mappingSet.first().getFragment2().getParent().getParent())) {
						AbstractCodeMapping parentMapping = findParentMappingContainingOperationInvocation();
						if(parentMapping != null && parentMapping.getFragment2().equals(mappingSet.first().getFragment2().getParent().getParent())) {
							fragment1IsInsideIfElseIf = true;
						}
					}
					if(!fragment1IsInsideIfElseIf) {
						return new LinkedHashSet<AbstractCodeMapping>(parentMap.values());
					}
				}
			}
		}
		return Set.of(mappingSet.first());
	}

	private Set<AbstractCodeMapping> movedOutOfIfElseIfBranch(TreeSet<? extends AbstractCodeMapping> mappingSet) {
		if(mappingSet.size() == 1) {
			return Set.of(mappingSet.first());
		}
		Set<String> commonParameters = container1.commonParameters(container2);
		boolean compatibleSignature = container1.getName().equals(container2.getName()) &&
				commonParameters.size() == Math.min(container1.getParametersWithoutReturnType().size(), container2.getParametersWithoutReturnType().size()) &&
				container1.equalReturnParameter(container2);
		if(container1.equals(container2) || compatibleSignature || (parentMapper != null && !nested)) {
			Map<CompositeStatementObject, AbstractCodeMapping> parentMap = new LinkedHashMap<>();
			Map<CompositeStatementObject, AbstractCodeMapping> grandParentMap = new LinkedHashMap<>();
			Map<AbstractCodeFragment, CompositeStatementObject> ifStatementConditionalExpressions = new LinkedHashMap<>();
			boolean ifFound = false, elseIfFound = false, elseFound = false;
			for(AbstractCodeMapping mapping : mappingSet) {
				AbstractCodeFragment fragment = mapping.getFragment1();
				if(fragment.getParent() != null && fragment.getParent().getParent() != null) {
					boolean directlyNested = false;
					boolean isBlock = fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
					boolean isWithinIfBranch = isBlock ? isIfBranch(fragment, fragment.getParent()) : isIfBranch(fragment.getParent(), fragment.getParent().getParent());
					if(!isBlock && fragment.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							fragment.getParent().getStatements().indexOf(fragment) == 0) {
						isWithinIfBranch = true;
						directlyNested = true;
					}
					boolean isWithinElseBranch = isBlock ? isElseBranch(fragment, fragment.getParent()) : isElseBranch(fragment.getParent(), fragment.getParent().getParent());
					if(!isBlock && fragment.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							fragment.getParent().getStatements().size() == 2 && fragment.getParent().getStatements().indexOf(fragment) == 1) {
						isWithinElseBranch = true;
						directlyNested = true;
					}
					boolean isWithinElseIfBranch = false;
					CompositeStatementObject grandGrandParent = fragment.getParent().getParent().getParent();
					if(grandGrandParent != null) {
						isWithinElseIfBranch = isBlock ? isElseIfBranch(fragment.getParent(), fragment.getParent().getParent()) : isElseIfBranch(fragment.getParent().getParent(), grandGrandParent);
						if(!isBlock && fragment.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
								fragment.getParent().getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
								fragment.getParent().getParent().getStatements().size() == 2 && fragment.getParent().getParent().getStatements().indexOf(fragment.getParent()) == 1) {
							isWithinElseIfBranch = true;
							directlyNested = true;
						}
					}
					int depthDifference = fragment.getDepth() - mapping.getFragment2().getDepth();
					if(isWithinIfBranch && !isWithinElseIfBranch && depthDifference >= 2 - (directlyNested ? 2 : 0)) {
						if(!parentMap.containsKey(fragment.getParent())) {
							parentMap.put(fragment.getParent(), mapping);
							if(!directlyNested) {
								if(grandGrandParent != null && grandGrandParent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									grandParentMap.put(grandGrandParent, mapping);
								}
								else {
									grandParentMap.put(fragment.getParent().getParent(), mapping);
								}
							}
							ifFound = true;
						}
					}
					else if(isWithinElseIfBranch && depthDifference >= 3 - (directlyNested ? 2 : 0)) {
						if(!parentMap.containsKey(fragment.getParent())) {
							parentMap.put(fragment.getParent(), mapping);
							if(!directlyNested) {
								if(grandGrandParent != null && grandGrandParent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									grandParentMap.put(grandGrandParent, mapping);
								}
								else {
									grandParentMap.put(fragment.getParent().getParent(), mapping);
								}
							}
							elseIfFound = true;
						}
					}
					else if(isWithinElseBranch && depthDifference >= 2 - (directlyNested ? 2 : 0)) {
						if(!parentMap.containsKey(fragment.getParent())) {
							parentMap.put(fragment.getParent(), mapping);
							if(!directlyNested) {
								if(grandGrandParent != null && grandGrandParent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									grandParentMap.put(grandGrandParent, mapping);
								}
								else {
									grandParentMap.put(fragment.getParent().getParent(), mapping);
								}
							}
							elseFound = true;
						}
					}
				}
				if(fragment instanceof AbstractExpression) {
					CompositeStatementObject owner = ((AbstractExpression) fragment).getOwner();
					if(owner != null && owner.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						ifStatementConditionalExpressions.put(fragment, owner);
					}
				}
			}
			boolean parentIfElseIfChain = ifElseIfChain(parentMap.keySet());
			boolean grandParentIfElseIfChain = parentMapper == null && Math.abs(parentMap.size()-grandParentMap.size()) <= 1 && ifElseIfChain(grandParentMap.keySet()) && !parentIfElseIfChain;
			if(grandParentIfElseIfChain) {
				//check if any of the mappings has an identical parent
				Map<AbstractCodeMapping, Set<ReplacementType>> replacementTypes = new LinkedHashMap<>();
				for(AbstractCodeMapping mapping : mappingSet) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					CompositeStatementObject parent1 = fragment1.getParent();
					CompositeStatementObject parent2 = fragment2.getParent();
					while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
						parent1 = parent1.getParent(); 
					}
					while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
						parent2 = parent2.getParent(); 
					}
					if(parent1 != null && parent2 != null && parent1.getString().equals(parent2.getString())) {
						grandParentIfElseIfChain = false;
						break;
					}
					if(parent1 != null && parent2 != null) {
						String s1 = parent1.getString();
						String s2 = parent2.getString();
						if(s1.endsWith(")") && s2.endsWith(")") && (s2.startsWith(s1.substring(0, s1.length()-1)) || s1.startsWith(s2.substring(0, s2.length()-1)))) {
							grandParentIfElseIfChain = false;
							break;
						}
					}
					if(!mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
						replacementTypes.put(mapping, mapping.getReplacementTypes());
					}
				}
				if(replacementTypes.size() == mappingSet.size()) {
					Set<ReplacementType> first = null;
					for(AbstractCodeMapping key : replacementTypes.keySet()) {
						Set<ReplacementType> keyReplacementTypes = replacementTypes.get(key);
						if(first == null) {
							first = keyReplacementTypes;
						}
						else if(!first.equals(keyReplacementTypes) && !first.containsAll(keyReplacementTypes) && !keyReplacementTypes.containsAll(first)) {
							grandParentIfElseIfChain = false;
							break;
						}
					}
				}
			}
			boolean nestedTernary = mappingSet.first().getFragment2().getTernaryOperatorExpressions().size() > 1;
			if(ifFound && (elseIfFound || (elseFound && parentMap.size() == grandParentMap.size()) || grandParentIfElseIfChain || nestedTernary) && (parentIfElseIfChain || grandParentIfElseIfChain)) {
				Set<String> variableDeclarationNames1 = new LinkedHashSet<>();
				Set<String> variableDeclarationNames2 = new LinkedHashSet<>();
				for(AbstractCodeMapping mapping : parentMap.values()) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					for(VariableDeclaration variable : fragment1.getVariableDeclarations()) {
						variableDeclarationNames1.add(variable.getVariableName());
					}
					for(VariableDeclaration variable : fragment2.getVariableDeclarations()) {
						variableDeclarationNames2.add(variable.getVariableName());
					}
					if(fragment1.assignmentInvocationCoveringEntireStatement() != null || fragment1.assignmentCreationCoveringEntireStatement() != null ||
							fragment1.assignmentTernaryOperatorCoveringEntireStatement() != null) {
						if(fragment1.getString().contains(JAVA.ASSIGNMENT)) {
							String assignedVariable = fragment1.getString().substring(0, fragment1.getString().indexOf(JAVA.ASSIGNMENT));
							variableDeclarationNames1.add(assignedVariable);
						}
					}
					if(fragment2.assignmentInvocationCoveringEntireStatement() != null || fragment2.assignmentCreationCoveringEntireStatement() != null ||
							fragment2.assignmentTernaryOperatorCoveringEntireStatement() != null) {
						if(fragment2.getString().contains(JAVA.ASSIGNMENT)) {
							String assignedVariable = fragment2.getString().substring(0, fragment2.getString().indexOf(JAVA.ASSIGNMENT));
							variableDeclarationNames2.add(assignedVariable);
						}
					}
				}
				AbstractCodeFragment fragment2 = mappingSet.first().getFragment2();
				boolean ifElseIfChainNestedUnderLoop = nestedUnderLoop(parentMap.keySet());
				boolean fragment2NestedUnderLoop = false;
				AbstractCodeFragment f2 = fragment2;
				while(f2.getParent() != null && f2.getParent().getParent() != null) {
					if(f2.getParent().isLoop() || f2.getParent().getParent().isLoop()) {
						fragment2NestedUnderLoop = true;
						break;
					}
					f2 = f2.getParent();
				}
				if(!(fragment2 instanceof AbstractExpression) && ifElseIfChainNestedUnderLoop == fragment2NestedUnderLoop && variableDeclarationNames1.equals(variableDeclarationNames2)) {
					boolean fragment2IsInsideIfElseIf = false;
					if(fragment2.getParent() != null && fragment2.getParent().getParent() != null) {
						boolean directlyNested = false;
						boolean isBlock = fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
						boolean isWithinIfBranch = isBlock ? isIfBranch(fragment2, fragment2.getParent()) : isIfBranch(fragment2.getParent(), fragment2.getParent().getParent());
						if(!isBlock && fragment2.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
								fragment2.getParent().getStatements().indexOf(fragment2) == 0) {
							isWithinIfBranch = true;
							directlyNested = true;
						}
						//boolean isWithinElseBranch = isElseBranch(fragment2.getParent(), fragment2.getParent().getParent());
						//boolean isWithinElseIfBranch = false;
						//if(fragment2.getParent().getParent().getParent() != null) {
						//	isWithinElseIfBranch = isElseIfBranch(fragment2.getParent().getParent(), fragment2.getParent().getParent().getParent());
						//}
						boolean hasElseIfOrElseBranch = false;
						if(!directlyNested)
							hasElseIfOrElseBranch = hasElseBranch(fragment2.getParent().getParent()) || hasElseIfBranch(fragment2.getParent().getParent());
						else
							hasElseIfOrElseBranch = hasElseBranch(fragment2.getParent()) || hasElseIfBranch(fragment2.getParent());
						if(isWithinIfBranch && hasElseIfOrElseBranch) {
							fragment2IsInsideIfElseIf = true;
						}
					}
					if(parentMapper != null && mappingSet.first().getFragment1().getParent() != null && mappingSet.first().getFragment1().getParent().getParent() != null && parentMapper.alreadyMatched1(mappingSet.first().getFragment1().getParent().getParent())) {
						AbstractCodeMapping parentMapping = findParentMappingContainingOperationInvocation();
						if(parentMapping != null && parentMapping.getFragment1().equals(mappingSet.first().getFragment1().getParent().getParent())) {
							fragment2IsInsideIfElseIf = true;
						}
					}
					if(!fragment2IsInsideIfElseIf || grandParentIfElseIfChain) {
						return new LinkedHashSet<AbstractCodeMapping>(parentMap.values());
					}
				}
			}
			if(initializersMergedToVariableDeclaration(mappingSet)) {
				return new LinkedHashSet<AbstractCodeMapping>(mappingSet);
			}
			if(ifStatementConditionalExpressions.size() == mappingSet.size()) {
				boolean invalid = false;
				for(Refactoring r : refactorings) {
					if(r instanceof ExtractVariableRefactoring) {
						ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
						if(mappingSet.first().getFragment2().getVariableDeclarations().contains(extract.getVariableDeclaration())) {
							invalid = true;
							break;
						}
					}
				}
				if(!invalid) {
					return new LinkedHashSet<AbstractCodeMapping>(mappingSet);
				}
			}
		}
		return Set.of(mappingSet.first());
	}

	private boolean initializersMergedToVariableDeclaration(TreeSet<? extends AbstractCodeMapping> mappingSet) {
		if(mappingSet.size() > 1) {
			VariableDeclaration variableDeclaration = null;
			int matches = 0;
			for(AbstractCodeMapping mapping : mappingSet) {
				if(mapping.getFragment2().getVariableDeclarations().size() > 0) {
					if(variableDeclaration == null) {
						variableDeclaration = mapping.getFragment2().getVariableDeclarations().get(0);
						String fragment1 = mapping.getFragment1().getString();
						if(variableDeclaration.getInitializer() != null) {
							if(fragment1.equals(variableDeclaration.getVariableName() + JAVA.ASSIGNMENT + variableDeclaration.getInitializer().getString() + JAVA.STATEMENT_TERMINATION)) {
								matches++;
							}
						}
					}
					else if(variableDeclaration.equals(mapping.getFragment2().getVariableDeclarations().get(0))) {
						String fragment1 = mapping.getFragment1().getString();
						if(variableDeclaration.getInitializer() != null) {
							if(fragment1.equals(variableDeclaration.getVariableName() + JAVA.ASSIGNMENT + variableDeclaration.getInitializer().getString() + JAVA.STATEMENT_TERMINATION)) {
								matches++;
							}
						}
					}
				}
			}
			return matches == mappingSet.size();
		}
		return false;
	}

	private boolean nestedUnderLoop(Set<CompositeStatementObject> blocks) {
		List<CompositeStatementObject> ifParents = extractIfParentsFromBlocks(blocks);
		if(ifParents.size() > 0) {
			CompositeStatementObject firstParent = ifParents.get(0);
			while(firstParent.getParent() != null && firstParent.getParent().getParent() != null) {
				if(firstParent.getParent().isLoop() || firstParent.getParent().getParent().isLoop()) {
					return true;
				}
				firstParent = firstParent.getParent();
			}
		}
		return false;
	}

	private boolean ifElseIfChain(Set<CompositeStatementObject> blocks) {
		//sort by start offset
		List<CompositeStatementObject> ifParents = extractIfParentsFromBlocks(blocks);
		int chainMatches = 0;
		for(int i=0; i<ifParents.size()-1; i++) {
			CompositeStatementObject current = ifParents.get(i);
			CompositeStatementObject next = ifParents.get(i+1);
			if(current.getStatements().contains(next) || current.equals(next)) {
				chainMatches++;
			}
		}
		return chainMatches == ifParents.size()-1;
	}

	private List<CompositeStatementObject> extractIfParentsFromBlocks(Set<CompositeStatementObject> blocks) {
		List<CompositeStatementObject> ifParents = new ArrayList<>();
		for(CompositeStatementObject block : blocks) {
			CompositeStatementObject parent = block.getParent();
			if(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				if(ifParents.isEmpty()) {
					ifParents.add(parent);
				}
				else {
					int index=0;
					for(CompositeStatementObject ifParent : ifParents) {
						if(parent.getLocationInfo().getStartOffset() < ifParent.getLocationInfo().getStartOffset()) {
							break;
						}
						index++;
					}
					ifParents.add(index, parent);
				}
			}
		}
		return ifParents;
	}

	private boolean isScopedMatch(AbstractCodeMapping startMapping, AbstractCodeMapping endMapping, AbstractCodeMapping parentMapping) {
		if(parentMapper != null && (multipleCallsToExtractedOrInlinedMethod() || nested || parentMapper.getChildMappers().size() > 0)) {
			return (startMapping != null && endMapping != null && (mappings.size() > 1 || startMapping.equals(endMapping))) || parentMapping != null;
		}
		return false;
	}

	private boolean multipleCallsToExtractedOrInlinedMethod() {
		return callsToExtractedMethod > 1 || callsToInlinedMethod > 1;
	}

	private boolean isWithinScope(AbstractCodeMapping startMapping, AbstractCodeMapping endMapping, AbstractCodeMapping parentMapping, AbstractCodeMapping mappingToCheck,
			Set<VariableDeclaration> referencedVariableDeclarations1, Set<VariableDeclaration> referencedVariableDeclarations2) {
		if(parentMapper != null && (multipleCallsToExtractedOrInlinedMethod() || nested || parentMapper.getChildMappers().size() > 0)) {
			if(startMapping != null && endMapping != null) {
				List<VariableDeclaration> variableDeclarations1 = mappingToCheck.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mappingToCheck.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.size() > 0 && referencedVariableDeclarations1.containsAll(variableDeclarations1) &&
						variableDeclarations2.size() > 0 && referencedVariableDeclarations2.containsAll(variableDeclarations2)) {
					return true;
				}
				if(startMapping.equals(endMapping)) {
					if(callsToExtractedMethod > 0) {
						boolean isNextStatement = mappingToCheck.getFragment1().getLocationInfo().getStartLine() == startMapping.getFragment1().getLocationInfo().getEndLine() + 1 &&
								mappingToCheck.getFragment2().getLocationInfo().getStartLine() == startMapping.getFragment2().getLocationInfo().getEndLine() + 1;
						boolean isAfterNextStatement = mappingToCheck.getFragment1().getLocationInfo().getStartLine() == startMapping.getFragment1().getLocationInfo().getEndLine() + 2 &&
								mappingToCheck.getFragment2().getLocationInfo().getStartLine() == startMapping.getFragment2().getLocationInfo().getEndLine() + 1;
						if(isNextStatement || isAfterNextStatement) {
							return true;
						}
						if(parentMapping != null) {
							return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= parentMapping.getFragment1().getLocationInfo().getStartLine() &&
									mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= parentMapping.getFragment1().getLocationInfo().getEndLine();
						}
						AbstractCodeFragment statementContainingOperationInvocation = statementContainingExtractedOperationInvocation();
						if(statementContainingOperationInvocation != null) {
							AbstractCodeMapping newEndMapping = null;
							for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
								if(mapping.getFragment2().equals(statementContainingOperationInvocation)) {
									break;
								}
								if(mapping.getFragment2().getLocationInfo().getStartLine() > statementContainingOperationInvocation.getLocationInfo().getStartLine()) {
									newEndMapping = mapping;
									break;
								}
							}
							if(newEndMapping != null) {
								return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= startMapping.getFragment1().getLocationInfo().getStartLine() &&
										mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= newEndMapping.getFragment1().getLocationInfo().getStartLine();
							}
						}
						return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= startMapping.getFragment1().getLocationInfo().getStartLine() &&
								mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= endMapping.getFragment1().getLocationInfo().getStartLine();
					}
					else {
						boolean isNextStatement = mappingToCheck.getFragment2().getLocationInfo().getStartLine() == startMapping.getFragment2().getLocationInfo().getEndLine() + 1;
						boolean isAfterNextStatement = mappingToCheck.getFragment2().getLocationInfo().getStartLine() == startMapping.getFragment2().getLocationInfo().getEndLine() + 2;
						if(isNextStatement || isAfterNextStatement) {
							return true;
						}
						if(parentMapping != null) {
							return mappingToCheck.getFragment2().getLocationInfo().getStartLine() >= parentMapping.getFragment2().getLocationInfo().getStartLine() &&
									mappingToCheck.getFragment2().getLocationInfo().getStartLine() <= parentMapping.getFragment2().getLocationInfo().getEndLine();
						}
						/*AbstractCodeFragment statementContainingOperationInvocation = statementContainingInlinedOperationInvocation();
						if(statementContainingOperationInvocation != null) {
							AbstractCodeMapping newEndMapping = null;
							for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
								if(mapping.getFragment1().getLocationInfo().getStartLine() > statementContainingOperationInvocation.getLocationInfo().getStartLine()) {
									newEndMapping = mapping;
									break;
								}
							}
							if(newEndMapping != null) {
								return mappingToCheck.getFragment2().getLocationInfo().getStartLine() >= startMapping.getFragment2().getLocationInfo().getStartLine() &&
										mappingToCheck.getFragment2().getLocationInfo().getStartLine() <= newEndMapping.getFragment2().getLocationInfo().getStartLine();
							}
						}*/
						return mappingToCheck.getFragment2().getLocationInfo().getStartLine() >= startMapping.getFragment2().getLocationInfo().getStartLine() &&
								mappingToCheck.getFragment2().getLocationInfo().getStartLine() <= endMapping.getFragment2().getLocationInfo().getStartLine();
					}
				}
				if(mappingToCheck.getFragment2().getLocationInfo().getStartLine() >= startMapping.getFragment2().getLocationInfo().getStartLine() &&
						mappingToCheck.getFragment2().getLocationInfo().getStartLine() <= endMapping.getFragment2().getLocationInfo().getStartLine()) {
					return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= startMapping.getFragment1().getLocationInfo().getStartLine() &&
							mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= endMapping.getFragment1().getLocationInfo().getStartLine();
				}
				if(mappingToCheck instanceof CompositeStatementObjectMapping) {
					CompositeStatementObject comp1 = (CompositeStatementObject)mappingToCheck.getFragment1();
					CompositeStatementObject comp2 = (CompositeStatementObject)mappingToCheck.getFragment2();
					if(!comp1.getLocationInfo().subsumes(startMapping.getFragment1().getLocationInfo()) && comp2.getLocationInfo().subsumes(startMapping.getFragment2().getLocationInfo())) {
						return false;
					}
				}
			}
			else if(parentMapping != null) {
				if(mappingToCheck.getFragment2().getLocationInfo().getStartLine() >= parentMapping.getFragment2().getLocationInfo().getStartLine() &&
						mappingToCheck.getFragment2().getLocationInfo().getStartLine() <= parentMapping.getFragment2().getLocationInfo().getEndLine()) {
					return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= parentMapping.getFragment1().getLocationInfo().getStartLine() &&
							mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= parentMapping.getFragment1().getLocationInfo().getEndLine();
				}
			}
		}
		return true;
	}

	private boolean allUnderTheSameParent(Set<? extends AbstractCodeMapping> mappings) {
		CompositeStatementObject parent1 = null;
		CompositeStatementObject parent2 = null;
		Set<AbstractCodeMapping> commonParentMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : mappings) {
			if(parent1 == null && parent2 == null) {
				parent1 = mapping.getFragment1().getParent();
				parent2 = mapping.getFragment2().getParent();
				commonParentMappings.add(mapping);
			}
			else if(parent1 != null && parent2 != null && parent1.equals(mapping.getFragment1().getParent()) && parent2.equals(mapping.getFragment2().getParent())) {
				commonParentMappings.add(mapping);
			}
			else if(parent1 != null && parent2 != null && (parent1.equals(mapping.getFragment1().getParent()) || parent2.equals(mapping.getFragment2().getParent()))) {
				if(mapping.getFragment1().getVariableDeclarations().size() != mapping.getFragment2().getVariableDeclarations().size()) {
					commonParentMappings.add(mapping);
				}
			}
		}
		return mappings.size() > 1 && mappings.equals(commonParentMappings);
	}

	private Triple<Integer, Integer, Integer> lineDistanceFromExistingMappings1(AbstractCodeMapping mapping) {
		int lineDistanceBefore = 0;
		int lineDistanceAfter = 0;
		int lineDistance = 0;
		int fragmentLine = mapping.getFragment1().getLocationInfo().getStartLine();
		Set<AbstractCodeMapping> commonParentMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping previousMapping : this.mappings) {
			CompositeStatementObject parent1 = previousMapping.getFragment1().getParent();
			CompositeStatementObject parent2 = previousMapping.getFragment2().getParent();
			if(parent1 != null && parent2 != null && parent1.equals(mapping.getFragment1().getParent()) && parent2.equals(mapping.getFragment2().getParent())) {
				commonParentMappings.add(previousMapping);
			}
		}
		Set<AbstractCodeMapping> mappingsToCompareWith = commonParentMappings.size() > 0 ? commonParentMappings : this.mappings;
		for(AbstractCodeMapping previousMapping : mappingsToCompareWith) {
			int previousStartLine = previousMapping.getFragment1().getLocationInfo().getStartLine();
			if(previousStartLine < fragmentLine)
				lineDistanceBefore += Math.abs(fragmentLine - previousStartLine);
			else if(previousStartLine > fragmentLine)
				lineDistanceAfter += Math.abs(fragmentLine - previousStartLine);
			lineDistance += Math.abs(fragmentLine - previousStartLine);
		}
		return Triple.of(lineDistanceBefore, lineDistance, lineDistanceAfter);
	}

	private Triple<Integer, Integer, Integer> lineDistanceFromExistingMappings2(AbstractCodeMapping mapping) {
		int lineDistanceBefore = 0;
		int lineDistanceAfter = 0;
		int lineDistance = 0;
		int fragmentLine = mapping.getFragment2().getLocationInfo().getStartLine();
		Set<AbstractCodeMapping> commonParentMappings = new LinkedHashSet<>();
		for(AbstractCodeMapping previousMapping : this.mappings) {
			CompositeStatementObject parent1 = previousMapping.getFragment1().getParent();
			CompositeStatementObject parent2 = previousMapping.getFragment2().getParent();
			if(parent1 != null && parent2 != null && parent1.equals(mapping.getFragment1().getParent()) && parent2.equals(mapping.getFragment2().getParent())) {
				commonParentMappings.add(previousMapping);
			}
		}
		Set<AbstractCodeMapping> mappingsToCompareWith = commonParentMappings.size() > 0 ? commonParentMappings : this.mappings;
		for(AbstractCodeMapping previousMapping : mappingsToCompareWith) {
			int previousStartLine = previousMapping.getFragment2().getLocationInfo().getStartLine();
			if(previousStartLine < fragmentLine)
				lineDistanceBefore += Math.abs(fragmentLine - previousStartLine);
			else if(previousStartLine > fragmentLine)
				lineDistanceAfter += Math.abs(fragmentLine - previousStartLine);
			lineDistance += Math.abs(fragmentLine - previousStartLine);
		}
		return Triple.of(lineDistanceBefore, lineDistance, lineDistanceAfter);
	}

	private boolean existingMappingWithCommonParents(LeafMapping variableDeclarationMapping) {
		CompositeStatementObject parent1 = variableDeclarationMapping.getFragment1().getParent();
		CompositeStatementObject parent2 = variableDeclarationMapping.getFragment2().getParent();
		for(AbstractCodeMapping previousMapping : this.mappings) {
			CompositeStatementObject previousParent1 = previousMapping.getFragment1().getParent();
			CompositeStatementObject previousParent2 = previousMapping.getFragment2().getParent();
			if(previousParent1 != null && previousParent2 != null && previousParent1.equals(parent1) && previousParent2.equals(parent2)) {
				return true;
			}
		}
		return false;
	}

	private void processAnonymousClassDeclarationsInIdenticalStatements(LeafMapping minStatementMapping) throws RefactoringMinerTimedOutException {
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = minStatementMapping.getFragment1().getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = minStatementMapping.getFragment2().getAnonymousClassDeclarations();
		if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty() && container1 != null && container2 != null &&
				anonymousClassDeclarations1.size() == anonymousClassDeclarations2.size()) {
			for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
				AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
				AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(i);
				UMLAnonymousClass anonymousClass1 = findAnonymousClass1(anonymousClassDeclaration1);
				UMLAnonymousClass anonymousClass2 = findAnonymousClass2(anonymousClassDeclaration2);
				UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(anonymousClass1, anonymousClass2, classDiff, modelDiff);
				anonymousClassDiff.process();
				List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
				if(matchedOperationMappers.size() > 0) {
					this.refactorings.addAll(anonymousClassDiff.getRefactorings());
					this.anonymousClassDiffs.add(anonymousClassDiff);
					if(parentMapper != null && minStatementMapping.getFragment1() instanceof AbstractExpression && minStatementMapping.getFragment2() instanceof AbstractExpression) {
						parentMapper.anonymousClassDiffs.add(anonymousClassDiff);
					}
					if(classDiff != null && classDiff.getRemovedAnonymousClasses().contains(anonymousClass1)) {
						classDiff.getRemovedAnonymousClasses().remove(anonymousClass1);
					}
					if(classDiff != null && classDiff.getAddedAnonymousClasses().contains(anonymousClass2)) {
						classDiff.getAddedAnonymousClasses().remove(anonymousClass2);
					}
					for(UMLOperationBodyMapper mapper : matchedOperationMappers) {
						addAllMappings(mapper.mappings);
					}
				}
			}
		}
		List<LambdaExpressionObject> lambdas1 = minStatementMapping.getFragment1().getLambdas();
		List<LambdaExpressionObject> lambdas2 = minStatementMapping.getFragment2().getLambdas();
		if(!lambdas1.isEmpty() && !lambdas2.isEmpty() && lambdas1.size() == lambdas2.size()) {
			for(int i=0; i<lambdas1.size(); i++) {
				LambdaExpressionObject lambda1 = lambdas1.get(i);
				LambdaExpressionObject lambda2 = lambdas2.get(i);
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(lambda1, lambda2, this);
				int mappings = mapper.mappingsWithoutBlocks();
				if(mappings > 0) {
					int nonMappedElementsT1 = mapper.nonMappedElementsT1();
					int nonMappedElementsT2 = mapper.nonMappedElementsT2();
					if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
							nonMappedElementsT1 == 0 || nonMappedElementsT2 == 0) {
						addAllMappings(mapper.mappings);
						this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
						this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
						this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
						this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
						if(this.container1 != null && this.container2 != null) {
							this.refactorings.addAll(mapper.getRefactorings());
						}
					}
				}
				else if(mapper.getMappings().size() == 1 && mapper.getMappings().iterator().next() instanceof CompositeStatementObjectMapping) {
					addAllMappings(mapper.mappings);
				}
			}
		}
	}

	protected void addAllMappings(Set<AbstractCodeMapping> mappings) {
		this.mappings.addAll(mappings);
		for(AbstractCodeMapping mapping : mappings) {
			mappingHashcodesT1.add(mapping.getFragment1().hashCode());
			mappingHashcodesT2.add(mapping.getFragment2().hashCode());
		}
	}

	public void addMapping(AbstractCodeMapping mapping) {
		this.mappings.add(mapping);
		if(mapping.getSubExpressionMappings().size() > 0) {
			this.mappings.addAll(mapping.getSubExpressionMappings());
		}
		mappingHashcodesT1.add(mapping.getFragment1().hashCode());
		mappingHashcodesT2.add(mapping.getFragment2().hashCode());
		addRefactorings(mapping);
	}

	private void addRefactorings(AbstractCodeMapping mapping) {
		for(Refactoring newRefactoring : mapping.getRefactorings()) {
			if(!this.refactorings.contains(newRefactoring)) {
				this.refactorings.add(newRefactoring);
			}
			else {
				for(Refactoring refactoring : this.refactorings) {
					if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractVariableRefactoring) {
						ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
						Set<AbstractCodeFragment> newUnmatchedStatementReferences = newExtractVariableRefactoring.getUnmatchedStatementReferences();
						ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
						oldExtractVariableRefactoring.addReferences(newReferences);
						oldExtractVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
						for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
							oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						break;
					}
					if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
						InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
						Set<AbstractCodeFragment> newUnmatchedStatementReferences = newInlineVariableRefactoring.getUnmatchedStatementReferences();
						InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
						oldInlineVariableRefactoring.addReferences(newReferences);
						oldInlineVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
						for(LeafMapping newLeafMapping : newInlineVariableRefactoring.getSubExpressionMappings()) {
							oldInlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						break;
					}
				}
			}
		}
	}

	private void removeAllMappings(Set<AbstractCodeMapping> mappings) {
		this.mappings.removeAll(mappings);
		for(AbstractCodeMapping mapping : mappings) {
			mappingHashcodesT1.remove(mapping.getFragment1().hashCode());
			mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
		}
	}

	public void removeMapping(AbstractCodeMapping mapping) {
		this.mappings.remove(mapping);
		mappingHashcodesT1.remove(mapping.getFragment1().hashCode());
		mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
	}

	private boolean nestedUnderExistingMapping(AbstractExpression e1, AbstractExpression e2) {
		for(AbstractCodeMapping m : this.mappings) {
			if(m.getFragment1() instanceof AbstractStatement || m.getFragment2() instanceof AbstractStatement) {
				boolean subsumes1 = m.getFragment1().getLocationInfo().subsumes(e1.getLocationInfo());
				boolean subsumes2 = m.getFragment2().getLocationInfo().subsumes(e2.getLocationInfo());
				if(subsumes1 && subsumes2) {
					return true;
				}
			}
		}
		return false;
	}

	private void addToMappings(LeafMapping mapping, TreeSet<LeafMapping> mappingSet) {
		addMapping(mapping);
		CompositeReplacement compositeReplacement = mapping.containsCompositeReplacement();
		for(LeafMapping leafMapping : mappingSet) {
			if(!leafMapping.equals(mapping)) {
				if(compositeReplacement != null) {
					if(compositeReplacement.getAdditionallyMatchedStatements1().contains(leafMapping.getFragment1()) ||
							compositeReplacement.getAdditionallyMatchedStatements2().contains(leafMapping.getFragment2())) {
						refactorings.addAll(leafMapping.getRefactorings());
					}
				}
				if(leafMapping.isIdenticalWithExtractedVariable() || leafMapping.isIdenticalWithInlinedVariable()) {
					refactorings.addAll(leafMapping.getRefactorings());
				}
				//remove from this.mappings nested mappings (inside anonymous or lambdas) corresponding to loser mappings
				Set<AbstractCodeMapping> mappingsToBeRemoved = new LinkedHashSet<AbstractCodeMapping>();
				int counter = 0;
				for(AbstractCodeMapping m : this.mappings) {
					boolean subsumes1 = leafMapping.getFragment1().getLocationInfo().subsumes(m.getFragment1().getLocationInfo());
					boolean subsumes2 = leafMapping.getFragment2().getLocationInfo().subsumes(m.getFragment2().getLocationInfo());
					boolean isLast = counter == this.mappings.size() - 1;		
					if(subsumes1 &&
							!leafMapping.getFragment1().equals(m.getFragment1()) &&
							subsumes2 &&
							!leafMapping.getFragment2().equals(m.getFragment2())) {
						mappingsToBeRemoved.add(m);
					}
					else if(!isLast && m.getFragment1() instanceof AbstractExpression && m.getFragment2() instanceof AbstractExpression &&
							!nestedUnderExistingMapping((AbstractExpression)m.getFragment1(), (AbstractExpression)m.getFragment2())) {
						mappingsToBeRemoved.add(m);
					}
					counter++;
				}
				if(mapping.getFragment1().getString().contains(JAVA.LAMBDA_ARROW) || mapping.getFragment2().getString().contains(JAVA.LAMBDA_ARROW) ||
						mapping.getFragment1().getString().contains(JAVA.METHOD_REFERENCE) || mapping.getFragment2().getString().contains(JAVA.METHOD_REFERENCE)) {
					removeAllMappings(mappingsToBeRemoved);
				}
				//remove from this.refactorings nested refactorings (inside anonymous or lambdas) corresponding to loser mappings
				Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<Refactoring>();
				for(Refactoring r : this.refactorings) {
					if(r instanceof ReferenceBasedRefactoring) {
						ReferenceBasedRefactoring referenceBased = (ReferenceBasedRefactoring)r;
						Set<AbstractCodeMapping> references = referenceBased.getReferences();
						Set<AbstractCodeMapping> intersection = new LinkedHashSet<AbstractCodeMapping>();
						intersection.addAll(references);
						intersection.retainAll(mappingsToBeRemoved);
						if(!intersection.isEmpty()) {
							refactoringsToBeRemoved.add(r);
						}
					}
					else if(r instanceof ReplaceAnonymousWithLambdaRefactoring) {
						ReplaceAnonymousWithLambdaRefactoring lambdaRef = (ReplaceAnonymousWithLambdaRefactoring)r;
						LambdaExpressionObject lambda = lambdaRef.getLambda();
						if(lambda.getExpression() != null) {
							for(AbstractCodeMapping m : mappingsToBeRemoved) {
								if(m.getFragment2().equals(lambda.getExpression())) {
									refactoringsToBeRemoved.add(r);
									break;
								}
							}
						}
					}
				}
				this.refactorings.removeAll(refactoringsToBeRemoved);
			}
		}
	}

	private boolean canBeAdded(LeafMapping minStatementMapping, Map<String, String> parameterToArgumentMap) {
		int newMappingReplacents = validReplacements(minStatementMapping, parameterToArgumentMap);
		boolean intersectionReplacement = minStatementMapping.getReplacements().size() == 1 &&
				minStatementMapping.getReplacements().iterator().next() instanceof IntersectionReplacement &&
				minStatementMapping.getFragment2().getTernaryOperatorExpressions().size() > 0;
		AbstractCodeMapping mappingToBeRemoved = null;
		boolean conflictingMappingFound = false;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment1().equals(minStatementMapping.getFragment1()) ||
					mapping.getFragment2().equals(minStatementMapping.getFragment2())) {
				if(newMappingReplacents > 0 && !intersectionReplacement)
					conflictingMappingFound = true;
				int oldMappingReplacements = validReplacements(mapping, parameterToArgumentMap);
				if(newMappingReplacents < oldMappingReplacements) {
					mappingToBeRemoved = mapping;
					break;
				}
			}
		}
		AbstractCodeFragment fragment1 = minStatementMapping.getFragment1();
		boolean ownedByLambda1 = fragment1.ownedByLambda();
		AbstractCodeFragment fragment2 = minStatementMapping.getFragment2();
		boolean ownedByLambda2 = fragment2.ownedByLambda();
		for(Refactoring r : refactorings) {
			if(r instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
				if(minStatementMapping.getFragment2().getVariableDeclarations().contains(extract.getVariableDeclaration())) {
					if(ownedByLambda1 == ownedByLambda2) {
						conflictingMappingFound = true;
					}
					else {
						refactorings.remove(r);
					}
					break;
				}
			}
			else if(r instanceof SplitConditionalRefactoring) {
				SplitConditionalRefactoring split = (SplitConditionalRefactoring)r;
				if(split.getOriginalConditional() instanceof CompositeStatementObject) {
					CompositeStatementObject comp = (CompositeStatementObject)split.getOriginalConditional();
					if(comp.getExpressions().contains(minStatementMapping.getFragment1()) && minStatementMapping.getFragment2() instanceof StatementObject &&
							minStatementMapping.getFragment2().getVariableDeclarations().size() > 0) {
						conflictingMappingFound = true;
					}
				}
			}
		}
		if(mappingToBeRemoved != null && !mappingToBeRemoved.containsReplacement(ReplacementType.COMPOSITE)) {
			removeMapping(mappingToBeRemoved);
		}
		else if(conflictingMappingFound) {
			return false;
		}
		return true;
	}

	private int validReplacements(AbstractCodeMapping mapping, Map<String, String> parameterToArgumentMap) {
		int validReplacements = 0;
		for(Replacement r : mapping.getReplacements()) {
			if(parameterToArgumentMap.containsKey(r.getAfter()) && parameterToArgumentMap.get(r.getAfter()).equals(r.getBefore())) {
				
			}
			else if(r.getType().equals(ReplacementType.INFIX_OPERATOR)) {
				
			}
			else {
				validReplacements++;
			}
		}
		return validReplacements;
	}

	private ReplacementInfo initializeReplacementInfo(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2,
			List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2) {
		List<? extends AbstractCodeFragment> l1 = new ArrayList<AbstractCodeFragment>(leaves1);
		l1.remove(leaf1);
		List<? extends AbstractCodeFragment> l2 = new ArrayList<AbstractCodeFragment>(leaves2);
		l2.remove(leaf2);
		ReplacementInfo replacementInfo = new ReplacementInfo(
				preprocessInput1(leaf1, leaf2),
				preprocessInput2(leaf1, leaf2),
				l1, l2);
		return replacementInfo;
	}

	private boolean mappingsWithSameReplacementTypes(Set<LeafMapping> mappingSet) {
		if(mappingSet.size() > 1) {
			Set<ReplacementType> replacementTypes = null;
			Set<LeafMapping> mappingsWithSameReplacementTypes = new LinkedHashSet<LeafMapping>();
			for(LeafMapping mapping : mappingSet) {
				if(replacementTypes == null) {
					replacementTypes = mapping.getReplacementTypes();
					mappingsWithSameReplacementTypes.add(mapping);
				}
				else if(mapping.getReplacementTypes().equals(replacementTypes)) {
					mappingsWithSameReplacementTypes.add(mapping);
				}
			}
			if(mappingsWithSameReplacementTypes.size() == mappingSet.size()) {
				return true;
			}
		}
		return false;
	}

	private boolean variableDeclarationMappingsWithSameReplacementTypes(Set<LeafMapping> mappingSet) {
		if(mappingSet.size() > 1) {
			Set<LeafMapping> variableDeclarationMappings = new LinkedHashSet<LeafMapping>();
			for(LeafMapping mapping : mappingSet) {
				if(mapping.getFragment1().getVariableDeclarations().size() > 0 &&
						mapping.getFragment2().getVariableDeclarations().size() > 0) {
					variableDeclarationMappings.add(mapping);
				}
			}
			if(variableDeclarationMappings.size() == mappingSet.size()) {
				LeafMapping topMapping = variableDeclarationMappings.iterator().next();
				boolean topMappingHasIdenticalTypeAndInitializer = 
						topMapping.getFragment1().getVariableDeclarations().get(0).getInitializer() != null &&
						topMapping.getFragment2().getVariableDeclarations().get(0).getInitializer() != null &&
						topMapping.getFragment1().getVariableDeclarations().get(0).getType() != null &&
						topMapping.getFragment2().getVariableDeclarations().get(0).getType() != null &&
						topMapping.getFragment1().getVariableDeclarations().get(0).getInitializer().getString().equals(topMapping.getFragment2().getVariableDeclarations().get(0).getInitializer().getString()) &&
						topMapping.getFragment1().getVariableDeclarations().get(0).getType().equals(topMapping.getFragment2().getVariableDeclarations().get(0).getType());
				Set<ReplacementType> replacementTypes = null;
				Set<LeafMapping> mappingsWithSameReplacementTypes = new LinkedHashSet<LeafMapping>();
				List<Boolean> equalNames = new ArrayList<>();
				for(LeafMapping mapping : variableDeclarationMappings) {
					equalNames.add(mapping.getFragment1().getVariableDeclarations().get(0).getVariableName().equals(mapping.getFragment2().getVariableDeclarations().get(0).getVariableName()));
					if(replacementTypes == null) {
						replacementTypes = mapping.getReplacementTypes();
						mappingsWithSameReplacementTypes.add(mapping);
					}
					else if(mapping.getReplacementTypes().equals(replacementTypes)) {
						if(!(replacementTypes.size() == 1 && replacementTypes.contains(ReplacementType.ANONYMOUS_CLASS_DECLARATION_REPLACED_WITH_LAMBDA)) &&
								!(replacementTypes.size() == 1 && replacementTypes.contains(ReplacementType.TYPE))) {
							mappingsWithSameReplacementTypes.add(mapping);
						}
					}
					else if(mapping.getReplacementTypes().containsAll(replacementTypes) || replacementTypes.containsAll(mapping.getReplacementTypes())) {
						AbstractCall invocation1 = mapping.getFragment1().invocationCoveringEntireFragment();
						AbstractCall invocation2 = mapping.getFragment2().invocationCoveringEntireFragment();
						if(invocation1 != null && invocation2 != null) {
							for(Replacement replacement : mapping.getReplacements()) {
								if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) || replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME)) {
									if(invocation1.getName().equals(replacement.getBefore()) && invocation2.getName().equals(replacement.getAfter())) {
										if(!topMappingHasIdenticalTypeAndInitializer || !mapping.containsReplacement(ReplacementType.TYPE)) {
											mappingsWithSameReplacementTypes.add(mapping);
											break;
										}
									}
								}
							}
						}
					}
				}
				if(equalNames.contains(false) && equalNames.contains(true)) {
					return false;
				}
				if(mappingsWithSameReplacementTypes.size() == mappingSet.size()) {
					List<VariableScope> scopes = new ArrayList<VariableScope>();
					for(LeafMapping m : mappingsWithSameReplacementTypes) {
						scopes.add(m.getFragment2().getVariableDeclarations().get(0).getScope());
					}
					int overlaps = 0;
					for(int i=0; i<scopes.size(); i++) {
						for(int j=i+1; j<scopes.size(); j++) {
							if(scopes.get(i).overlaps(scopes.get(j))) {
								overlaps++;
							}
						}
					}
					if(overlaps > 0)
						return true;
				}
			}
		}
		return false;
	}

	private LeafMapping findBestMappingBasedOnTryBlockMappings(Map<LeafMapping, Pair<CompositeStatementObject, CompositeStatementObject>> map, TreeSet<LeafMapping> mappingSet) {
		Map<LeafMapping, Boolean> existsMappingInParentTryBlocks = new LinkedHashMap<>();
		for(LeafMapping mapping : map.keySet()) {
			Pair<CompositeStatementObject, CompositeStatementObject> pair = map.get(mapping);
			TryStatementObject try1 = pair.getLeft().getTryContainer().isPresent() ? pair.getLeft().getTryContainer().get() : null;
			TryStatementObject try2 = pair.getRight().getTryContainer().isPresent() ? pair.getRight().getTryContainer().get() : null;
			if(try1 != null && try2 != null) {
				for(AbstractCodeMapping alreadyEstablishedMapping : mappings) {
					if(try1.getLocationInfo().subsumes(alreadyEstablishedMapping.getFragment1().getLocationInfo()) &&
							try2.getLocationInfo().subsumes(alreadyEstablishedMapping.getFragment2().getLocationInfo())) {
						existsMappingInParentTryBlocks.put(mapping, true);
						break;
					}
				}
				if(!existsMappingInParentTryBlocks.containsKey(mapping)) {
					existsMappingInParentTryBlocks.put(mapping, false);
				}
			}
		}
		Collection<Boolean> values = existsMappingInParentTryBlocks.values();
		if(values.contains(false) && values.contains(true)) {
			for(LeafMapping mapping : existsMappingInParentTryBlocks.keySet()) {
				if(existsMappingInParentTryBlocks.get(mapping)) {
					return mapping;
				}
			}
		}
		return mappingSet.first();
	}

	private Map<LeafMapping, Pair<CompositeStatementObject, CompositeStatementObject>> allMappingsNestedUnderCatchBlocks(Set<LeafMapping> mappingSet) {
		if(mappingSet.size() > 1) {
			Map<LeafMapping, Pair<CompositeStatementObject, CompositeStatementObject>> map = new LinkedHashMap<>();
			for(LeafMapping mapping : mappingSet) {
				Pair<CompositeStatementObject, CompositeStatementObject> pair = mapping.nestedUnderCatchBlock();
				if(pair != null) {
					map.put(mapping, pair);
				}
			}
			if(map.size() == mappingSet.size()) {
				return map;
			}
		}
		return null;
	}

	private LeafMapping findBestMappingBasedOnMappedSwitchCases(Pair<CompositeStatementObject, CompositeStatementObject> switchParentEntry, TreeSet<LeafMapping> mappingSet) {
		CompositeStatementObject switchParent1 = switchParentEntry.getKey();
		CompositeStatementObject switchParent2 = switchParentEntry.getValue();
		AbstractCodeMapping currentSwitchCase = null;
		for(AbstractCodeMapping mapping : this.mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			if(fragment1 instanceof AbstractStatement && fragment2 instanceof AbstractStatement) {
				AbstractStatement statement1 = (AbstractStatement)fragment1;
				AbstractStatement statement2 = (AbstractStatement)fragment2;
				CompositeStatementObject parent1 = statement1.getParent();
				CompositeStatementObject parent2 = statement2.getParent();
				if(parent1 == switchParent1 && parent2 == switchParent2 && mapping.isExact() &&
						statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE) &&
						statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE)) {
					currentSwitchCase = mapping;
				}
				else if(parent1 == switchParent1 && parent2 == switchParent2 &&
						statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.BREAK_STATEMENT) &&
						statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.BREAK_STATEMENT)) {
					if(currentSwitchCase != null) {
						for(LeafMapping leafMapping : mappingSet) {
							if(leafMapping.getFragment1().getIndex() > currentSwitchCase.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() > currentSwitchCase.getFragment2().getIndex() &&
									leafMapping.getFragment1().getIndex() < mapping.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() < mapping.getFragment2().getIndex()) {
								return leafMapping;
							}
						}
					}
				}
				else if(parent1 == switchParent1 && parent2 == switchParent2 &&
						statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT) &&
						statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
					if(currentSwitchCase != null) {
						for(LeafMapping leafMapping : mappingSet) {
							if(leafMapping.getFragment1().getIndex() > currentSwitchCase.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() > currentSwitchCase.getFragment2().getIndex() &&
									leafMapping.getFragment1().getIndex() < mapping.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() < mapping.getFragment2().getIndex()) {
								return leafMapping;
							}
						}
					}
				}
			}
		}
		if(currentSwitchCase != null) {
			for(LeafMapping leafMapping : mappingSet) {
				if(leafMapping.getFragment1().getIndex() > currentSwitchCase.getFragment1().getIndex() &&
						leafMapping.getFragment2().getIndex() > currentSwitchCase.getFragment2().getIndex()) {
					return leafMapping;
				}
			}
		}
		return mappingSet.first();
	}

	private Pair<CompositeStatementObject, CompositeStatementObject> multipleMappingsUnderTheSameSwitch(Set<LeafMapping> mappingSet) {
		CompositeStatementObject switchParent1 = null;
		CompositeStatementObject switchParent2 = null;
		if(mappingSet.size() > 1) {
			for(LeafMapping mapping : mappingSet) {
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(fragment1 instanceof AbstractStatement && fragment2 instanceof AbstractStatement) {
					AbstractStatement statement1 = (AbstractStatement)fragment1;
					AbstractStatement statement2 = (AbstractStatement)fragment2;
					CompositeStatementObject parent1 = statement1.getParent();
					CompositeStatementObject parent2 = statement2.getParent();
					if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) &&
							parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
						if(switchParent1 == null && switchParent2 == null) {
							switchParent1 = parent1;
							switchParent2 = parent2;
						}
						else if(switchParent1 != parent1 || switchParent2 != parent2) {
							return null;
						}
					}
					else {
						return null;
					}
				}
			}
		}
		if(switchParent1 != null && switchParent2 != null) {
			return Pair.of(switchParent1, switchParent2);
		}
		return null;
	}

	protected LeafMapping createLeafMapping(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2, Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions) {
		VariableDeclarationContainer container1 = codeFragmentOperationMap1.containsKey(leaf1) ? codeFragmentOperationMap1.get(leaf1) : this.container1;
		VariableDeclarationContainer container2 = codeFragmentOperationMap2.containsKey(leaf2) ? codeFragmentOperationMap2.get(leaf2) : this.container2;
		LeafMapping mapping = new LeafMapping(leaf1, leaf2, container1, container2);
		mapping.setEqualNumberOfAssertions(equalNumberOfAssertions);
		if(extractedStatements.size() > 0) {
			mapping.setExtractedStatements(extractedStatements);
		}
		int matchingArguments = 0;
		for(String key : parameterToArgumentMap.keySet()) {
			String value = parameterToArgumentMap.get(key);
			if(operationInvocation != null) {
				for(String argument : operationInvocation.arguments()) {
					if(argument.equals(value)) {
						List<LeafExpression> matchingExpressions = leaf1.findExpression(argument);
						if(matchingExpressions.size() > 0) {
							matchingArguments++;
						}
					}
				}
			}
			if(!key.equals(value) && ReplacementUtil.contains(leaf2.getString(), key) && ReplacementUtil.contains(leaf1.getString(), value)) {
				mapping.addReplacement(new Replacement(value, key, ReplacementType.VARIABLE_NAME));
			}
		}
		mapping.setMatchingArgumentsWithOperationInvocation(matchingArguments);
		if(leaf1.getCreations().size() > 0 && leaf2.getCreations().size() > 0 &&
				leaf1.getCreations().get(0) instanceof ObjectCreation &&
				leaf2.getCreations().get(0) instanceof ObjectCreation) {
			ObjectCreation creation1 = (ObjectCreation) leaf1.getCreations().get(0);
			ObjectCreation creation2 = (ObjectCreation) leaf2.getCreations().get(0);
			if(creation1.isArray() && creation2.isArray() && creation1.getAnonymousClassDeclaration() != null && creation2.getAnonymousClassDeclaration() != null) {
				List<ObjectCreation> nestedCreations1 = new ArrayList<ObjectCreation>();
				for(int i=1; i<leaf1.getCreations().size(); i++) {
					if(creation1.getLocationInfo().subsumes(leaf1.getCreations().get(i).getLocationInfo()) && leaf1.getCreations().get(i) instanceof ObjectCreation) {
						nestedCreations1.add((ObjectCreation) leaf1.getCreations().get(i));
					}
				}
				List<ObjectCreation> nestedCreations2 = new ArrayList<ObjectCreation>();
				for(int i=1; i<leaf2.getCreations().size(); i++) {
					if(creation2.getLocationInfo().subsumes(leaf2.getCreations().get(i).getLocationInfo()) && leaf2.getCreations().get(i) instanceof ObjectCreation) {
						nestedCreations2.add((ObjectCreation) leaf2.getCreations().get(i));
					}
				}
				if(nestedCreations1.size() <= nestedCreations2.size()) {
					for(ObjectCreation nestedCreation1 : nestedCreations1) {
						int[] editDistance = new int[nestedCreations2.size()];
						int[] argumentIntersections = new int[nestedCreations2.size()];
						int index = 0;
						for(ObjectCreation nestedCreation2 : nestedCreations2) {
							int argumentIntersection = nestedCreation1.argumentIntersection(nestedCreation2).size();
							argumentIntersections[index] = argumentIntersection;
							int d = StringDistance.editDistance(nestedCreation1.actualString(), nestedCreation2.actualString());
							editDistance[index] = d;
							index++;
						}
						int minIndex = 0;
						int minValue = editDistance[0];
						int maxArgumentIntersection = argumentIntersections[0];
						for(int i=0; i<editDistance.length; i++) {
							if(editDistance[i] < minValue && argumentIntersections[i] > maxArgumentIntersection) {
								minValue = editDistance[i];
								maxArgumentIntersection = argumentIntersections[i];
								minIndex = i;
							}
						}
						LeafMapping newMapping = new LeafMapping(nestedCreation1.asLeafExpression(), nestedCreations2.get(minIndex).asLeafExpression(), container1, container2);
						mapping.addSubExpressionMapping(newMapping);
						nestedCreations2.remove(minIndex);
					}
				}
				else {
					for(ObjectCreation nestedCreation2 : nestedCreations2) {
						int[] editDistance = new int[nestedCreations1.size()];
						int[] argumentIntersections = new int[nestedCreations1.size()];
						int index = 0;
						for(ObjectCreation nestedCreation1 : nestedCreations1) {
							int argumentIntersection = nestedCreation1.argumentIntersection(nestedCreation2).size();
							argumentIntersections[index] = argumentIntersection;
							int d = StringDistance.editDistance(nestedCreation1.actualString(), nestedCreation2.actualString());
							editDistance[index] = d;
							index++;
						}
						int minIndex = 0;
						int minValue = editDistance[0];
						int maxArgumentIntersection = argumentIntersections[0];
						for(int i=0; i<editDistance.length; i++) {
							if(editDistance[i] < minValue && argumentIntersections[i] > maxArgumentIntersection) {
								minValue = editDistance[i];
								maxArgumentIntersection = argumentIntersections[i];
								minIndex = i;
							}
						}
						LeafMapping newMapping = new LeafMapping(nestedCreations1.get(minIndex).asLeafExpression(), nestedCreation2.asLeafExpression(), container1, container2);
						mapping.addSubExpressionMapping(newMapping);
						nestedCreations1.remove(minIndex);
					}
				}
			}
		}
		return mapping;
	}

	protected String preprocessInput1(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf1, leaf2);
	}

	protected String preprocessInput2(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf2, leaf1);
	}

	private String preprocessInput(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		String argumentizedString = new String(leaf1.getArgumentizedString());
		if (leaf1 instanceof StatementObject && leaf2 instanceof AbstractExpression) {
			if (argumentizedString.startsWith(JAVA.RETURN_SPACE) && argumentizedString.endsWith(JAVA.STATEMENT_TERMINATION)) {
				argumentizedString = argumentizedString.substring(JAVA.RETURN_SPACE.length(),
						argumentizedString.lastIndexOf(JAVA.STATEMENT_TERMINATION));
			}
		}
		return argumentizedString;
	}

	public void detectMatchingNonMappedExpression(CompositeStatementObject ifStatement, UMLOperation sourceOperationAfterExtraction) {
        for (Iterator<AbstractCodeFragment> iterator = getNonMappedLeavesT2().iterator(); iterator.hasNext(); ) {
            AbstractCodeFragment abstractCodeFragment = iterator.next();
            for (AbstractCall methodInvocation : abstractCodeFragment.getMethodInvocations()) {
                if (isAssumption(methodInvocation)) {
					LeafMapping mappingCandidate = new LeafMapping(ifStatement, methodInvocation, getOperation1(), getOperation2());
					for (AbstractExpression expression : ifStatement.getExpressions()) {
						for (String argument : methodInvocation.arguments()) {
							if (expression.getString().equals(argument)) {
								addMapping(mappingCandidate);
								iterator.remove();
                                refactorings.add(new ExtractOperationRefactoring(this, sourceOperationAfterExtraction, List.of()));
                                refactorings.add(new ExtractPreconditionRefactoring(methodInvocation, ifStatement, getOperation1(), getOperation2()));
                            }
                        }
                    }
                }
            }
        }
	}

	protected static class ReplacementInfo {
		private String argumentizedString1;
		private String argumentizedString2;
		private int rawDistance;
		private Set<Replacement> replacements;
		private List<LeafMapping> subExpressionMappings;
		private List<? extends AbstractCodeFragment> statements1;
		private List<? extends AbstractCodeFragment> statements2;
		private List<UMLOperationBodyMapper> lambdaMappers;
		
		public ReplacementInfo(String argumentizedString1, String argumentizedString2,
				List<? extends AbstractCodeFragment> statements1, List<? extends AbstractCodeFragment> statements2) {
			this.argumentizedString1 = argumentizedString1;
			this.argumentizedString2 = argumentizedString2;
			this.statements1 = statements1;
			this.statements2 = statements2;
			this.rawDistance = StringDistance.editDistance(argumentizedString1, argumentizedString2);
			this.replacements = new LinkedHashSet<Replacement>();
			this.subExpressionMappings = new ArrayList<LeafMapping>();
			this.lambdaMappers = new ArrayList<UMLOperationBodyMapper>();
		}
		public String getArgumentizedString1() {
			return argumentizedString1;
		}
		public String getArgumentizedString2() {
			return argumentizedString2;
		}
		public List<? extends AbstractCodeFragment> getStatements1() {
			return statements1;
		}
		public List<? extends AbstractCodeFragment> getStatements2() {
			return statements2;
		}
		public void setArgumentizedString1(String string) {
			this.argumentizedString1 = string;
			this.rawDistance = StringDistance.editDistance(this.argumentizedString1, this.argumentizedString2);
		}
		public int getRawDistance() {
			return rawDistance;
		}
		public void moveLast(Replacement oldReplacement) {
			this.replacements.remove(oldReplacement);
			this.replacements.add(oldReplacement);
		}
		public void addReplacement(Replacement r) {
			this.replacements.add(r);
		}
		public void addReplacements(Set<Replacement> replacementsToBeAdded) {
			this.replacements.addAll(replacementsToBeAdded);
		}
		public void removeReplacements(Set<Replacement> replacementsToBeRemoved) {
			this.replacements.removeAll(replacementsToBeRemoved);
		}
		public Set<Replacement> getReplacements() {
			return replacements;
		}
		public List<UMLOperationBodyMapper> getLambdaMappers() {
			return lambdaMappers;
		}
		public List<Replacement> getReplacements(ReplacementType type) {
			List<Replacement> replacements = new ArrayList<Replacement>();
			for(Replacement replacement : this.replacements) {
				if(replacement.getType().equals(type)) {
					replacements.add(replacement);
				}
			}
			return replacements;
		}

		public boolean containsOnlyReplacement(ReplacementType type) {
			for(Replacement replacement : replacements) {
				if(!replacement.getType().equals(type)) {
					return false;
				}
			}
			return replacements.size() > 0;
		}

		public boolean containsReplacement(ReplacementType type) {
			for(Replacement replacement : replacements) {
				if(replacement.getType().equals(type)) {
					return true;
				}
			}
			return false;
		}
		public void addSubExpressionMapping(LeafMapping newLeafMapping) {
			boolean alreadyPresent = false; 
			for(LeafMapping oldLeafMapping : subExpressionMappings) { 
				if(oldLeafMapping.getFragment1().getLocationInfo().equals(newLeafMapping.getFragment1().getLocationInfo()) && 
						oldLeafMapping.getFragment2().getLocationInfo().equals(newLeafMapping.getFragment2().getLocationInfo())) { 
					alreadyPresent = true; 
					break; 
				} 
			} 
			if(!alreadyPresent) { 
				subExpressionMappings.add(newLeafMapping); 
			}
		}
		public List<LeafMapping> getSubExpressionMappings() {
			for(Replacement r : replacements) {
				if(r instanceof LeafMappingProvider) {
					LeafMappingProvider provider = (LeafMappingProvider)r;
					for(LeafMapping mapping : provider.getSubExpressionMappings()) {
						addSubExpressionMapping(mapping);
					}
				}
			}
			return subExpressionMappings;
		}
		public void addLambdaMapper(UMLOperationBodyMapper mapper) {
			lambdaMappers.add(mapper);
		}
		public boolean lambdaMapperContainsMapping(AbstractCodeMapping mapping) {
			for(UMLOperationBodyMapper mapper : lambdaMappers) {
				if(mapper.getMappings().contains(mapping)) {
					return true;
				}
			}
			return false;
		}
	}

	protected UMLAnonymousClass findAnonymousClass1(AnonymousClassDeclarationObject anonymousClassDeclaration1) {
		UMLAnonymousClass anonymousClass1 = container1.findAnonymousClass(anonymousClassDeclaration1);
		if(anonymousClass1 == null && parentMapper != null) {
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				anonymousClass1 = childMapper.container1.findAnonymousClass(anonymousClassDeclaration1);
				if(anonymousClass1 != null) {
					break;
				}
			}
		}
		if(anonymousClass1 == null && container1.isConstructor() && modelDiff != null) {
			for(UMLClass umlClass : modelDiff.getParentModel().getClassList()) {
				if(umlClass.getName().equals(container1.getClassName())) {
					for(UMLAttribute attribute : umlClass.getAttributes()) {
						anonymousClass1 = attribute.findAnonymousClass(anonymousClassDeclaration1);
						if(anonymousClass1 != null) {
							break;
						}
					}
				}
			}
		}
		return anonymousClass1;
	}

	protected UMLAnonymousClass findAnonymousClass2(AnonymousClassDeclarationObject anonymousClassDeclaration2) {
		UMLAnonymousClass anonymousClass2 = container2.findAnonymousClass(anonymousClassDeclaration2);
		if(anonymousClass2 == null && parentMapper != null) {
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				anonymousClass2 = childMapper.container2.findAnonymousClass(anonymousClassDeclaration2);
				if(anonymousClass2 != null) {
					break;
				}
			}
		}
		if(anonymousClass2 == null && container2.isConstructor() && modelDiff != null) {
			for(UMLClass umlClass : modelDiff.getChildModel().getClassList()) {
				if(umlClass.getName().equals(container2.getClassName())) {
					for(UMLAttribute attribute : umlClass.getAttributes()) {
						anonymousClass2 = attribute.findAnonymousClass(anonymousClassDeclaration2);
						if(anonymousClass2 != null) {
							break;
						}
					}
				}
			}
		}
		return anonymousClass2;
	}

	public void createMultiMappingsForDuplicatedStatements(Set<AbstractCodeFragment> mergedStatements,
			AbstractCodeFragment mergedToStatement, Map<String, String> parameterToArgumentMap) {
		for(AbstractCodeFragment mergedConditional : mergedStatements) {
			if(mergedConditional instanceof CompositeStatementObject) {
				CompositeStatementObject comp = (CompositeStatementObject)mergedConditional;
				Set<AbstractCodeMapping> newMappingsToBeAdded = new LinkedHashSet<>();
				for(AbstractCodeFragment leaf : comp.getLeaves()) {
					if(!alreadyMatched1(leaf)) {
						for(AbstractCodeMapping mapping : this.mappings) {
							if(mapping instanceof LeafMapping && mapping.getFragment1().getString().equals(leaf.getString()) && mergedToStatement.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())) {
								LeafMapping newMapping = createLeafMapping(leaf, mapping.getFragment2(), parameterToArgumentMap, false);
								newMappingsToBeAdded.add(newMapping);
							}
						}
					}
				}
				for(CompositeStatementObject innerNode : comp.getInnerNodes()) {
					if(!alreadyMatched1(innerNode)) {
						for(AbstractCodeMapping mapping : this.mappings) {
							if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
									mapping.getFragment1().getString().equals(innerNode.getString()) && mergedToStatement.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())) {
								CompositeStatementObjectMapping oldMapping = (CompositeStatementObjectMapping)mapping;
								CompositeStatementObjectMapping newMapping = createCompositeMapping(innerNode, (CompositeStatementObject)mapping.getFragment2(), parameterToArgumentMap, oldMapping.getCompositeChildMatchingScore());
								newMappingsToBeAdded.add(newMapping);
								List<AbstractStatement> innerNode1Statements = innerNode.getStatements();
								List<AbstractStatement> innerNode2Statements = ((CompositeStatementObject)mapping.getFragment2()).getStatements();
								if(innerNode1Statements.size() == 1 && innerNode1Statements.get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
										innerNode2Statements.size() == 1 && innerNode2Statements.get(0).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
									CompositeStatementObjectMapping newBlockMapping = createCompositeMapping((CompositeStatementObject)innerNode1Statements.get(0), (CompositeStatementObject)innerNode2Statements.get(0), parameterToArgumentMap, oldMapping.getCompositeChildMatchingScore());
									newMappingsToBeAdded.add(newBlockMapping);
								}
							}
						}
					}
				}
				for(AbstractCodeMapping mapping : newMappingsToBeAdded) {
					addMapping(mapping);
				}
			}
		}
	}

	public boolean isEmpty() {
		return getNonMappedLeavesT1().isEmpty() && getNonMappedInnerNodesT1().isEmpty() &&
				getNonMappedLeavesT2().isEmpty() && getNonMappedInnerNodesT2().isEmpty();
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLOperationBodyMapper) {
    		UMLOperationBodyMapper other = (UMLOperationBodyMapper)o;
    		return this.container1.equals(other.container1) && this.container2.equals(other.container2);
    	}
    	return false;
	}

	public String toString() {
		String s1 = container1.toString();
		if(container1 instanceof LambdaExpressionObject) {
			s1 = s1.substring(0, s1.indexOf(" -> "));
		}
		String s2 = container2.toString();
		if(container2 instanceof LambdaExpressionObject) {
			s2 = s2.substring(0, s2.indexOf(" -> "));
		}
		return s1 + " -> " + s2;
	}

	@Override
	public int compareTo(UMLOperationBodyMapper operationBodyMapper) {
		boolean identicalStringRepresentation1 = this.identicalBody();
		boolean identicalStringRepresentation2 = operationBodyMapper.identicalBody();
		if(identicalStringRepresentation1 != identicalStringRepresentation2) {
			if(identicalStringRepresentation1) {
				return -1;
			}
			else if(identicalStringRepresentation2) {
				return 1;
			}
		}
		int thisMappings = this.mappingsWithoutBlocks();
		for(AbstractCodeMapping mapping : this.getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				thisMappings++;
			}
		}
		int otherMappings = operationBodyMapper.mappingsWithoutBlocks();
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				otherMappings++;
			}
		}
		int thisExactMatches = this.exactMatches();
		int otherExactMatches = operationBodyMapper.exactMatches();
		if(thisMappings > otherMappings && thisExactMatches >= otherExactMatches) {
			return -1;
		}
		else if(thisMappings < otherMappings && thisExactMatches <= otherExactMatches) {
			return 1;
		}
		//if(thisMappings != otherMappings) {
		//	return -Integer.compare(thisMappings, otherMappings);
		//}
		else {
			if(thisExactMatches != otherExactMatches) {
				return -Integer.compare(thisExactMatches, otherExactMatches);
			}
			else {
				int thisNonMapped = this.nonMappedElementsT2();
				int otherNonMapped = operationBodyMapper.nonMappedElementsT2();
				if(thisNonMapped != otherNonMapped) {
					return Integer.compare(thisNonMapped, otherNonMapped);
				}
				else {
					if(getOperation1() != null && getOperation2() != null && operationBodyMapper.getOperation1() != null && operationBodyMapper.getOperation2() != null) {
						boolean thisEqualSignature = this.getOperation1().equalsIgnoringTypeParameters(this.getOperation2());
						boolean otherEqualSignature = operationBodyMapper.getOperation1().equalsIgnoringTypeParameters(operationBodyMapper.getOperation2());
						if(thisEqualSignature && !otherEqualSignature) {
							return -1;
						}
						else if(!thisEqualSignature && otherEqualSignature) {
							return 1;
						}
					}
					if(this.getContainer1().getJavadoc() != null && this.getContainer2().getJavadoc() != null &&
							operationBodyMapper.getContainer1().getJavadoc() != null && operationBodyMapper.getContainer2().getJavadoc() != null) {
						boolean thisCommonJavadoc = 
								this.getContainer1().getJavadoc().getText().contains(this.getContainer2().getJavadoc().getText()) ||
								this.getContainer2().getJavadoc().getText().contains(this.getContainer1().getJavadoc().getText());
						boolean otherCommonJavadoc = 
								operationBodyMapper.getContainer1().getJavadoc().getText().contains(operationBodyMapper.getContainer2().getJavadoc().getText()) ||
								operationBodyMapper.getContainer2().getJavadoc().getText().contains(operationBodyMapper.getContainer1().getJavadoc().getText());
						if(thisCommonJavadoc && !otherCommonJavadoc) {
							return -1;
						}
						else if(!thisCommonJavadoc && otherCommonJavadoc) {
							return 1;
						}
					}
					int thisEditDistance = this.editDistance();
					int otherEditDistance = operationBodyMapper.editDistance();
					if(thisEditDistance != otherEditDistance) {
						return Integer.compare(thisEditDistance, otherEditDistance);
					}
					else {
						AbstractCall thisDelegate = this.getContainer2().singleStatementCallingMethod();
						AbstractCall otherDelegate = operationBodyMapper.getContainer2().singleStatementCallingMethod();
						boolean delegateArgumentInMethodName = false;
						if(thisDelegate != null && otherDelegate != null) {
							boolean thisMatch = false;
							for(String arg : thisDelegate.arguments()) {
								if(arg.startsWith("\"") && arg.endsWith("\""))
									arg = arg.substring(1, arg.length()-1);
								if(this.getContainer2().getName().toLowerCase().contains(arg)) {
									thisMatch = true;
									break;
								}
							}
							boolean otherMatch = false;
							for(String arg : otherDelegate.arguments()) {
								if(arg.startsWith("\"") && arg.endsWith("\""))
									arg = arg.substring(1, arg.length()-1);
								if(operationBodyMapper.getContainer2().getName().toLowerCase().contains(arg)) {
									otherMatch = true;
									break;
								}
							}
							delegateArgumentInMethodName = thisMatch && otherMatch;
						}
						int thisOperationNameEditDistance = this.operationNameEditDistance();
						int otherOperationNameEditDistance = operationBodyMapper.operationNameEditDistance();
						if(thisOperationNameEditDistance != otherOperationNameEditDistance && !delegateArgumentInMethodName) {
							return Integer.compare(thisOperationNameEditDistance, otherOperationNameEditDistance);
						}
						if(this.container1.getClassName().equals(this.container2.getClassName()) && operationBodyMapper.container1.getClassName().equals(operationBodyMapper.container2.getClassName())) {
							int locationSum1 = this.container1.getLocationInfo().getStartLine() + this.container2.getLocationInfo().getStartLine();
							int locationSum2 = operationBodyMapper.container1.getLocationInfo().getStartLine() + operationBodyMapper.container2.getLocationInfo().getStartLine();
							return Integer.valueOf(locationSum1).compareTo(Integer.valueOf(locationSum2));
						}
						else {
							//move method scenario
							return Integer.compare(this.packageNameEditDistance(), operationBodyMapper.packageNameEditDistance());
						}
					}
				}
			}
		}
	}

	private boolean identicalBody() {
		OperationBody body1 = container1.getBody();
		OperationBody body2 = container2.getBody();
		if(body1 != null && body2 != null) {
			return container1.getBodyHashCode() == container2.getBodyHashCode();
		}
		return false;
	}

	public boolean containsExtractOperationRefactoring(UMLOperation extractedOperation) {
		if(classDiff != null) {
			if(classDiff.containsExtractOperationRefactoring(container1, extractedOperation)) {
				return true;
			}
		}
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring)ref;
				if(extractRef.getExtractedOperation().equals(extractedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsInlineOperationRefactoring(UMLOperation inlinedOperation) {
		if(classDiff != null) {
			if(classDiff.containsInlineOperationRefactoring(inlinedOperation, container2)) {
				return true;
			}
		}
		for(Refactoring ref : refactorings) {
			if(ref instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring inlineRef = (InlineOperationRefactoring)ref;
				if(inlineRef.getInlinedOperation().equals(inlinedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	private List<CompositeStatementObject> getNestedTryCatch(List<AbstractStatement> compStatements) {
		List<CompositeStatementObject> nestedTryCatch = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : compStatements) {
			if(statement.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) ||
					statement.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				nestedTryCatch.add((CompositeStatementObject)statement);
			}
		}
		return nestedTryCatch;
	}

	private double compositeChildMatchingScore(CompositeStatementObject comp1, CompositeStatementObject comp2, 
			Optional<ReplacementInfo> replacementInfo, Set<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		List<AbstractStatement> compStatements1 = comp1.getStatements();
		List<AbstractStatement> compStatements2 = comp2.getStatements();
		int childrenSize1 = compStatements1.size();
		int childrenSize2 = compStatements2.size();
		List<CompositeStatementObject> nestedTryCatch1 = getNestedTryCatch(compStatements1);
		List<CompositeStatementObject> nestedTryCatch2 = getNestedTryCatch(compStatements2);
		boolean equalIfElseIfChain = false;
		boolean reorderedIfElseIfChain = false;
		
		if(parentMapper != null && comp1.getLocationInfo().getCodeElementType().equals(comp2.getLocationInfo().getCodeElementType()) &&
				childrenSize1 == 1 && childrenSize2 == 1 && !comp1.getString().equals(JAVA.OPEN_BLOCK) && !comp2.getString().equals(JAVA.OPEN_BLOCK)) {
			if(compStatements1.get(0).getString().equals(JAVA.OPEN_BLOCK) && !compStatements2.get(0).getString().equals(JAVA.OPEN_BLOCK)) {
				CompositeStatementObject block = (CompositeStatementObject)compStatements1.get(0);
				compStatements1 = new ArrayList<>(comp1.getStatements());
				compStatements1.addAll(block.getStatements());
			}
			if(!compStatements1.get(0).getString().equals(JAVA.OPEN_BLOCK) && compStatements2.get(0).getString().equals(JAVA.OPEN_BLOCK)) {
				CompositeStatementObject block = (CompositeStatementObject)compStatements2.get(0);
				compStatements2 = new ArrayList<>(comp2.getStatements());
				compStatements2.addAll(block.getStatements());
			}
		}
		int mappedChildrenSize = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(compStatements1.contains(mapping.getFragment1()) && compStatements2.contains(mapping.getFragment2())) {
				mappedChildrenSize++;
			}
		}
		if(parentMapper != null && comp1.getLocationInfo().getCodeElementType().equals(comp2.getLocationInfo().getCodeElementType()) &&
				comp1.getParent() != null && comp2.getParent() != null && comp1.getParent().getLocationInfo().getCodeElementType().equals(comp2.getParent().getLocationInfo().getCodeElementType())) {
			for(AbstractCodeMapping mapping : parentMapper.mappings) {
				if(compStatements1.contains(mapping.getFragment1()) && !compStatements2.contains(mapping.getFragment2()) &&
						mapping.getFragment2().getParent() != null &&
						mapping.getFragment2().getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						mapping.getFragment2().getParent().getParent() == null) {
					mappedChildrenSize++;
				}
				else if(compStatements2.contains(mapping.getFragment2()) && !compStatements1.contains(mapping.getFragment1()) &&
						mapping.getFragment1().getParent() != null &&
						mapping.getFragment1().getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						mapping.getFragment1().getParent().getParent() == null) {
					mappedChildrenSize++;
				}
			}
		}
		if(mappedChildrenSize == 0) {
			List<AbstractCodeFragment> leaves1 = comp1.getLeaves();
			List<AbstractCodeFragment> leaves2 = comp2.getLeaves();
			if(replacementInfo.isPresent()) {
				ReplacementInfo info = replacementInfo.get();
				List<Replacement> compositeReplacements = info.getReplacements(ReplacementType.COMPOSITE);
				for(Replacement r : compositeReplacements) {
					CompositeReplacement compositeReplacement = (CompositeReplacement)r;
					for(AbstractCodeFragment fragment : compositeReplacement.getAdditionallyMatchedStatements1()) {
						if(fragment instanceof CompositeStatementObject && !fragment.getLocationInfo().subsumes(comp1.getLocationInfo()) &&
								!comp1.getLocationInfo().subsumes(fragment.getLocationInfo())) {
							CompositeStatementObject additionalComposite = (CompositeStatementObject)fragment;
							List<AbstractCodeFragment> additionalLeaves = additionalComposite.getLeaves();
							if(!additionalLeaves.toString().equals(leaves2.toString())) {
								leaves1.addAll(additionalLeaves);
							}
						}
					}
					for(AbstractCodeFragment fragment : compositeReplacement.getAdditionallyMatchedStatements2()) {
						if(fragment instanceof CompositeStatementObject && !fragment.getLocationInfo().subsumes(comp2.getLocationInfo()) &&
								!comp2.getLocationInfo().subsumes(fragment.getLocationInfo())) {
							CompositeStatementObject additionalComposite = (CompositeStatementObject)fragment;
							List<AbstractCodeFragment> additionalLeaves = additionalComposite.getLeaves();
							if(!additionalLeaves.toString().equals(leaves1.toString())) {
								leaves2.addAll(additionalLeaves);
							}
						}
					}
				}
			}
			int leaveSize1 = leaves1.size();
			int leaveSize2 = leaves2.size();
			int mappedLeavesSize = 0;
			if(leaveSize1 == 0 && leaveSize2 == 0) {
				List<String> commentsWithinComp1 = extractCommentsWithinStatement(comp1, container1);
				List<String> commentsWithinComp2 = extractCommentsWithinStatement(comp2, container2);
				if(commentsWithinComp1.size() > 0 && commentsWithinComp1.equals(commentsWithinComp2)) {
					return 1.0;
				}
			}
			boolean blocksWithMappedTryContainer =
					(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) || comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) &&
					(comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) || comp2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) &&
					containsMapping(comp1.getTryContainer().get(), comp2.getTryContainer().get());
			for(AbstractCodeMapping mapping : mappings) {
				if(leaves1.contains(mapping.getFragment1()) && leaves2.contains(mapping.getFragment2())) {
					boolean mappingNestedAtSameLevel = false;
					CompositeStatementObject parent1 = mapping.getFragment1().getParent();
					CompositeStatementObject parent2 = mapping.getFragment2().getParent();
					while(parent1 != null && parent2 != null) {
						if(parent1.equals(comp1) && parent2.equals(comp2)) {
							mappingNestedAtSameLevel = true;
							break;
						}
						parent1 = parent1.getParent();
						parent2 = parent2.getParent();
					}
					if(!mappingNestedAtSameLevel) {
						Set<CompositeStatementObject> ifElseIfChain1 = constructIfElseIfChain(mapping.getFragment1());
						Set<CompositeStatementObject> ifElseIfChain2 = constructIfElseIfChain(mapping.getFragment2());
						equalIfElseIfChain = ifElseIfChain1.size() == ifElseIfChain2.size() && ifElseIfChain1.size() > 1;
						if(equalIfElseIfChain) {
							List<String> list1 = new ArrayList<String>();
							for(CompositeStatementObject if1 : ifElseIfChain1) {
								list1.add(if1.getString());
							}
							List<String> list2 = new ArrayList<String>();
							for(CompositeStatementObject if2 : ifElseIfChain2) {
								list2.add(if2.getString());
							}
							if(list1.containsAll(list2) && list2.containsAll(list1) && !list1.equals(list2)) {
								reorderedIfElseIfChain = true;
							}
						}
					}
					boolean mappingUnderNestedTryCatch = false;
					if(nestedTryCatch1.isEmpty() && !nestedTryCatch2.isEmpty() && !blocksWithMappedTryContainer) {
						for(CompositeStatementObject statement : nestedTryCatch2) {
							List<AbstractStatement> directlyNestedStatements = statement.getStatements();
							if(directlyNestedStatements.contains(mapping.getFragment2())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
							List<AbstractCodeFragment> leaves = statement.getLeaves();
							if(leaves.contains(mapping.getFragment2())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
						}
					}
					else if(!nestedTryCatch1.isEmpty() && nestedTryCatch2.isEmpty() && !blocksWithMappedTryContainer) {
						for(CompositeStatementObject statement : nestedTryCatch1) {
							List<AbstractStatement> directlyNestedStatements = statement.getStatements();
							if(directlyNestedStatements.contains(mapping.getFragment1())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
							List<AbstractCodeFragment> leaves = statement.getLeaves();
							if(leaves.contains(mapping.getFragment1())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
						}
					}
					if(!mappingUnderNestedTryCatch && (!equalIfElseIfChain || reorderedIfElseIfChain)) {
						mappedLeavesSize++;
					}
				}
			}
			if(mappedLeavesSize == 0) {
				//check for possible extract or inline
				if(leaveSize2 <= 2) {
					for(AbstractCodeFragment leaf2 : leaves2) {
						AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
						if(invocation == null) {
							invocation = leaf2.assignmentInvocationCoveringEntireStatement();
						}
						UMLOperation matchingOperation = null;
						if(invocation != null && classDiff != null && (matchingOperation = classDiff.matchesOperation(invocation, addedOperations, container2)) != null && !matchingOperation.equals(container2) && !matchesRemovedOperationWithIdenticalBody(matchingOperation)) {
							int matchingLeaves = matchingLeaves(matchingOperation, leaves1);
							mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
							leaveSize2 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
						}
						if(invocation != null && classDiff != null && invocation.actualString().contains(JAVA.LAMBDA_ARROW)) {
							for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
								for(AbstractCall inv : lambda.getAllOperationInvocations()) {
									if((matchingOperation = classDiff.matchesOperation(inv, addedOperations, container2)) != null && !matchingOperation.equals(container2) && !matchesRemovedOperationWithIdenticalBody(matchingOperation)) {
										int matchingLeaves = matchingLeaves(matchingOperation, leaves1);
										mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
										leaveSize2 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
									}
								}
							}
						}
						if(matchingOperation == null && classDiff != null) {
							for(AbstractCall call : leaf2.getMethodInvocations()) {
								if((matchingOperation = classDiff.matchesOperation(call, addedOperations, container2)) != null && !matchingOperation.equals(container2) && !matchesRemovedOperationWithIdenticalBody(matchingOperation) && matchingOperation.stringRepresentation().size() > 3) {
									int matchingLeaves = matchingLeaves(matchingOperation, leaves1);
									mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
									leaveSize2 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
								}
							}
						}
					}
				}
				else if(leaveSize1 <= 2) {
					for(AbstractCodeFragment leaf1 : leaves1) {
						AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
						if(invocation == null) {
							invocation = leaf1.assignmentInvocationCoveringEntireStatement();
						}
						UMLOperation matchingOperation = null;
						if(invocation != null && classDiff != null && (matchingOperation = classDiff.matchesOperation(invocation, removedOperations, container1)) != null && !matchingOperation.equals(container1) && !matchesAddedOperationWithIdenticalBody(matchingOperation)) {
							int matchingLeaves = matchingLeaves(matchingOperation, leaves2);
							mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
							leaveSize1 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
						}
						if(invocation != null && classDiff != null && invocation.actualString().contains(JAVA.LAMBDA_ARROW)) {
							for(LambdaExpressionObject lambda : leaf1.getLambdas()) {
								for(AbstractCall inv : lambda.getAllOperationInvocations()) {
									if((matchingOperation = classDiff.matchesOperation(inv, removedOperations, container1)) != null && !matchingOperation.equals(container1) && !matchesAddedOperationWithIdenticalBody(matchingOperation)) {
										int matchingLeaves = matchingLeaves(matchingOperation, leaves2);
										mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
										leaveSize1 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
									}
								}
							}
						}
						if(matchingOperation == null && classDiff != null) {
							for(AbstractCall call : leaf1.getMethodInvocations()) {
								if((matchingOperation = classDiff.matchesOperation(call, removedOperations, container1)) != null && !matchingOperation.equals(container1) && !matchesAddedOperationWithIdenticalBody(matchingOperation) && matchingOperation.stringRepresentation().size() > 3) {
									int matchingLeaves = matchingLeaves(matchingOperation, leaves2);
									mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
									leaveSize1 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
								}
							}
						}
					}
				}
				if(leaveSize1 == 1 && leaveSize2 == 1 && leaves1.get(0).getString().equals(JAVA.CONTINUE_STATEMENT) && leaves2.get(0).getString().equals(JAVA.RETURN_NULL)) {
					mappedLeavesSize++;
				}
				if(leaveSize1 == 2 && leaveSize2 == 1 && !leaves1.get(0).getString().equals(JAVA.BREAK_STATEMENT) && leaves1.get(1).getString().equals(JAVA.BREAK_STATEMENT) && leaves2.get(0).getString().startsWith(JAVA.RETURN_SPACE)) {
					mappedLeavesSize++;
				}
			}
			int max = Math.max(leaveSize1, leaveSize2);
			if(max == 0) {
				return 0;
			}
			else {
				if(mappedLeavesSize > 0) {
					return (double)mappedLeavesSize/(double)max;
				}
				if(comp1.getString().equals(comp2.getString()) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						(!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) ||
								(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) && blocksWithMappedTryContainer)) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
						!logGuard(comp1) && !nullCheck(comp1, comp2, replacementInfo) &&
						!parentMapperContainsExactMapping(comp1) && !equalIfElseIfChain) {
					return 0.01;
				}
			}
		}
		
		int max = Math.max(childrenSize1, childrenSize2);
		if(max == 0)
			return 0;
		else
			return (double)mappedChildrenSize/(double)max;
	}

	private Set<CompositeStatementObject> constructIfElseIfChain(AbstractCodeFragment fragment) {
		if(fragment.getParent() != null && fragment.getParent().getParent() != null) {
			boolean isWithinIfBranch = isIfBranch(fragment.getParent(), fragment.getParent().getParent());
			boolean isWithinElseBranch = isElseBranch(fragment.getParent(), fragment.getParent().getParent());
			boolean isWithinElseIfBranch = false;
			if(fragment.getParent().getParent().getParent() != null) {
				isWithinElseIfBranch = isElseIfBranch(fragment.getParent().getParent(), fragment.getParent().getParent().getParent());
			}
			Set<CompositeStatementObject> blocks = new LinkedHashSet<>();
			if(isWithinIfBranch && !isWithinElseIfBranch) {
				blocks.add(fragment.getParent());
				CompositeStatementObject currentIf = fragment.getParent().getParent();
				//collect child blocks
				while(currentIf != null && hasElseIfBranch(currentIf)) {
					currentIf = (CompositeStatementObject)currentIf.getStatements().get(1);
					if(currentIf.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						if(currentIf.getStatements().get(0) instanceof CompositeStatementObject) {
							blocks.add((CompositeStatementObject)currentIf.getStatements().get(0));
						}
					}
				}
				if(hasElseBranch(currentIf)) {
					if(currentIf.getStatements().get(1) instanceof CompositeStatementObject) {
						blocks.add((CompositeStatementObject)currentIf.getStatements().get(1));
					}
				}
			}
			else if(isWithinElseIfBranch) {
				blocks.add(fragment.getParent());
				CompositeStatementObject currentIf = fragment.getParent().getParent();
				CompositeStatementObject lastIf = currentIf;
				//collect parent blocks
				while(currentIf != null && currentIf.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					CompositeStatementObject parent = currentIf.getParent();
					if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							parent.getStatements().contains(currentIf)) {
						if(parent.getStatements().get(0) instanceof CompositeStatementObject) {
							blocks.add((CompositeStatementObject)parent.getStatements().get(0));
						}
					}
					currentIf = parent;
				}
				//collect child blocks
				while(lastIf != null && hasElseIfBranch(lastIf)) {
					lastIf = (CompositeStatementObject)lastIf.getStatements().get(1);
					if(lastIf.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						if(lastIf.getStatements().get(0) instanceof CompositeStatementObject) {
							blocks.add((CompositeStatementObject)lastIf.getStatements().get(0));
						}
					}
				}
				if(hasElseBranch(lastIf)) {
					if(lastIf.getStatements().get(1) instanceof CompositeStatementObject) {
						blocks.add((CompositeStatementObject)lastIf.getStatements().get(1));
					}
				}
			}
			else if(isWithinElseBranch) {
				blocks.add(fragment.getParent());
				CompositeStatementObject currentIf = fragment.getParent().getParent();
				//collect parent blocks
				while(currentIf != null && currentIf.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					CompositeStatementObject parent = currentIf.getParent();
					if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
							parent.getStatements().contains(currentIf)) {
						if(parent.getStatements().get(0) instanceof CompositeStatementObject) {
							blocks.add((CompositeStatementObject)parent.getStatements().get(0));
						}
					}
					currentIf = parent;
				}
			}
			return new LinkedHashSet<>(extractIfParentsFromBlocks(blocks));
		}
		return Collections.emptySet();
	}

	private boolean nullCheck(CompositeStatementObject comp1, CompositeStatementObject comp2, Optional<ReplacementInfo> replacementInfo) {
		if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			for(AbstractExpression expression : comp1.getExpressions()) {
				if(expression.getString().endsWith(" == null")) {
					List<String> commentsWithinStatement1 = extractCommentsWithinStatement(comp1, container1);
					List<String> commentsWithinStatement2 = extractCommentsWithinStatement(comp2, container2);
					if(commentsWithinStatement1.size() > 0 || commentsWithinStatement2.size() > 0) {
						Set<String> intersection = new LinkedHashSet<>(commentsWithinStatement1);
						intersection.retainAll(commentsWithinStatement2);
						boolean onlyStatementsInMethod = false;
						if(replacementInfo.isPresent()) {
							ReplacementInfo info = replacementInfo.get();
							if(info.getStatements1().isEmpty() && info.getStatements2().isEmpty()) {
								onlyStatementsInMethod = true;
							}
						}
						if(intersection.isEmpty() && !onlyStatementsInMethod) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean logGuard(CompositeStatementObject comp) {
		if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			for(AbstractExpression expression : comp.getExpressions()) {
				for(AbstractCall call : expression.getMethodInvocations()) {
					if(call.isLogGuard()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private LeafMapping parentOrSiblingMapperContainsMapping(Set<LeafMapping> mappings) {
		if(parentMapper != null) {
			for(LeafMapping mapping : mappings) {
				if(parentMapper.mappings.contains(mapping)) {
					return mapping;
				}
				for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
					if(childMapper.mappings.contains(mapping)) {
						return mapping;
					}
				}
			}
		}
		return null;
	}

	public boolean parentMapperContainsMapping(AbstractCodeFragment statement) {
		if(parentMapper != null) {
			for(AbstractCodeMapping mapping : parentMapper.mappings) {
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(fragment1.equals(statement) || fragment2.equals(statement)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean parentMapperContainsExactMapping(AbstractCodeFragment statement) {
		if(parentMapper != null) {
			for(AbstractCodeMapping mapping : parentMapper.mappings) {
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(fragment1.equals(statement) || fragment2.equals(statement)) {
					if(fragment1.getString().equals(fragment2.getString())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private double compositeChildMatchingScore(TryStatementObject try1, TryStatementObject try2, Optional<ReplacementInfo> replacementInfo, Set<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration) {
		double score = compositeChildMatchingScore((CompositeStatementObject)try1, (CompositeStatementObject)try2, replacementInfo, mappings, removedOperations, addedOperations);
		if(!tryWithResourceMigration) {
			List<CompositeStatementObject> catchClauses1 = try1.getCatchClauses();
			List<CompositeStatementObject> catchClauses2 = try2.getCatchClauses();
			if(catchClauses1.size() == catchClauses2.size()) {
				for(int i=0; i<catchClauses1.size(); i++) {
					double tmpScore = compositeChildMatchingScore(catchClauses1.get(i), catchClauses2.get(i), replacementInfo, mappings, removedOperations, addedOperations);
					if(tmpScore == 1) {
						score += tmpScore;
					}
				}
			}
			if(try1.getFinallyClause() != null && try2.getFinallyClause() != null) {
				double tmpScore = compositeChildMatchingScore(try1.getFinallyClause(), try2.getFinallyClause(), replacementInfo, mappings, removedOperations, addedOperations);
				if(tmpScore == 1) {
					score += tmpScore;
				}
			}
		}
		return score;
	}

	private int matchingLeaves(UMLOperation operation, List<AbstractCodeFragment> leaves) {
		int matchingLeaves = 0;
		if(operation.getBody() != null) {
			List<AbstractCodeFragment> operationLeaves = operation.getBody().getCompositeStatement().getLeaves();
			for(AbstractCodeFragment leaf : leaves) {
				for(AbstractCodeFragment operationLeaf : operationLeaves) {
					if(leaf.getString().equals(operationLeaf.getString())) {
						matchingLeaves++;
						break;
					}
				}
			}
		}
		return matchingLeaves;
	}

	private int callsToInlinedMethod(List<? extends AbstractCodeFragment> leaves1) {
		int counter = 0;
		for(AbstractCodeFragment leaf1 : leaves1) {
			AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = leaf1.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && invocation.matchesOperation(container1, callSiteOperation, classDiff, modelDiff)) {
				counter++;
			}
			else {
				for(AbstractCall call : leaf1.getMethodInvocations()) {
					if(!call.equals(invocation) && call.matchesOperation(container1, callSiteOperation, classDiff, modelDiff)) {
						counter++;
						break;
					}
				}
			}
		}
		return counter;
	}

	private int callsToExtractedMethod(List<? extends AbstractCodeFragment> leaves2) {
		int counter = 0;
		for(AbstractCodeFragment leaf2 : leaves2) {
			AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = leaf2.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && invocation.matchesOperation(container2, callSiteOperation, classDiff, modelDiff)) {
				counter++;
			}
			else {
				for(AbstractCall call : leaf2.getMethodInvocations()) {
					if(!call.equals(invocation) && call.matchesOperation(container2, callSiteOperation, classDiff, modelDiff)) {
						counter++;
						break;
					}
				}
			}
		}
		return counter;
	}

	private boolean matchesAddedOperationWithIdenticalBody(UMLOperation removedOperation) {
		if(classDiff != null) {
			List<String> stringRepresentation = removedOperation.stringRepresentation();
			for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
				if(removedOperation.getBodyHashCode() == addedOperation.getBodyHashCode()) {
					return true;
				}
				List<String> commonStatements = new ArrayList<String>();
				List<String> addedOperationStringRepresentation = addedOperation.stringRepresentation();
				if(stringRepresentation.size() == addedOperationStringRepresentation.size()) {
					for(int i=0; i<stringRepresentation.size(); i++) {
						if(stringRepresentation.get(i).equals(addedOperationStringRepresentation.get(i))) {
							commonStatements.add(stringRepresentation.get(i));
						}
					}
				}
				if(commonStatements.size() >= stringRepresentation.size() - 1) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean matchesRemovedOperationWithIdenticalBody(UMLOperation addedOperation) {
		if(classDiff != null) {
			List<String> stringRepresentation = addedOperation.stringRepresentation();
			for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
				if(removedOperation.getBodyHashCode() == addedOperation.getBodyHashCode()) {
					return true;
				}
				List<String> commonStatements = new ArrayList<String>();
				List<String> removedOperationStringRepresentation = removedOperation.stringRepresentation();
				if(stringRepresentation.size() == removedOperationStringRepresentation.size()) {
					for(int i=0; i<stringRepresentation.size(); i++) {
						if(stringRepresentation.get(i).equals(removedOperationStringRepresentation.get(i))) {
							commonStatements.add(stringRepresentation.get(i));
						}
					}
				}
				if(commonStatements.size() >= stringRepresentation.size() - 1) {
					return true;
				}
			}
		}
		return false;
	}

	private UMLOperation callToExtractedMethod(AbstractCodeFragment leaf2) {
		if(classDiff != null) {
			List<UMLOperation> addedOperations = classDiff.getAddedOperations();
			if(addedOperations.size() > 0) {
				AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
				if(invocation == null) {
					invocation = leaf2.assignmentInvocationCoveringEntireStatement();
				}
				UMLOperation matchingOperation = null;
				if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, addedOperations, container2)) != null && matchingOperation.getBody() != null) {
					return matchingOperation;
				}
			}
		}
		return null;
	}

	private UMLOperation callToInlinedMethod(AbstractCodeFragment leaf1) {
		if(classDiff != null) {
			List<UMLOperation> removedOperations = classDiff.getRemovedOperations();
			if(removedOperations.size() > 0) {
				AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
				if(invocation == null) {
					invocation = leaf1.assignmentInvocationCoveringEntireStatement();
				}
				UMLOperation matchingOperation = null;
				if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, removedOperations, container1)) != null && matchingOperation.getBody() != null) {
					return matchingOperation;
				}
			}
		}
		return null;
	}

	private boolean containsCallToExtractedMethod(List<? extends AbstractCodeFragment> leaves2) {
		if(classDiff != null) {
			List<UMLOperation> addedOperations = classDiff.getAddedOperations();
			if(addedOperations.size() > 0) {
				for(AbstractCodeFragment leaf2 : leaves2) {
					AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
					if(invocation == null) {
						invocation = leaf2.assignmentInvocationCoveringEntireStatement();
					}
					UMLOperation matchingOperation = null;
					if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, addedOperations, container2)) != null && matchingOperation.getBody() != null) {
						boolean removedMethodWithIdenticalBodyFound = false;
						for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
							if(removedOperation.getBodyHashCode() == matchingOperation.getBodyHashCode()) {
								removedMethodWithIdenticalBodyFound = true;
								break;
							}
						}
						if(!removedMethodWithIdenticalBodyFound) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean containsCallToExtractedMethod() {
		Set<AbstractCodeFragment> statementsToBeIgnored = new HashSet<>();
		if(classDiff != null) {
			List<UMLOperation> addedOperations = classDiff.getAddedOperations();
			if(addedOperations.size() > 0) {
				for(AbstractCodeFragment leaf2 : getNonMappedLeavesT2()) {
					AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
					if(invocation == null) {
						invocation = leaf2.assignmentInvocationCoveringEntireStatement();
					}
					UMLOperation matchingOperation = null;
					if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, addedOperations, container2)) != null && matchingOperation.getBody() != null) {
						statementsToBeIgnored.addAll(matchingOperation.getBody().getCompositeStatement().getLeaves());
						statementsToBeIgnored.addAll(matchingOperation.getBody().getCompositeStatement().getInnerNodes());
					}
				}
			}
		}
		if(statementsToBeIgnored.size() > 0) {
			Set<String> statements = statementsToBeIgnored.stream().map(statement -> statement.getString()).collect(Collectors.toSet());
			int matchingStatements = 0;
			for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
				if(composite.countableStatement() && statements.contains(composite.getString()))
					matchingStatements++;
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				if(statement.countableStatement() && statements.contains(statement.getString()))
					matchingStatements++;
			}
			return matchingStatements > 0;
		}
		return false;
	}

	public boolean containsCallToInlinedMethod() {
		Set<AbstractCodeFragment> statementsToBeIgnored = new HashSet<>();
		if(classDiff != null) {
			List<UMLOperation> removedOperations = classDiff.getRemovedOperations();
			if(removedOperations.size() > 0) {
				for(AbstractCodeFragment leaf1 : getNonMappedLeavesT1()) {
					AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
					if(invocation == null) {
						invocation = leaf1.assignmentInvocationCoveringEntireStatement();
					}
					UMLOperation matchingOperation = null;
					if(invocation != null && (matchingOperation = classDiff.matchesOperation(invocation, removedOperations, container2)) != null && matchingOperation.getBody() != null) {
						statementsToBeIgnored.addAll(matchingOperation.getBody().getCompositeStatement().getLeaves());
						statementsToBeIgnored.addAll(matchingOperation.getBody().getCompositeStatement().getInnerNodes());
					}
				}
			}
		}
		if(statementsToBeIgnored.size() > 0) {
			Set<String> statements = statementsToBeIgnored.stream().map(statement -> statement.getString()).collect(Collectors.toSet());
			int matchingStatements = 0;
			for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
				if(composite.countableStatement() && statements.contains(composite.getString()))
					matchingStatements++;
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				if(statement.countableStatement() && statements.contains(statement.getString()))
					matchingStatements++;
			}
			return matchingStatements > 0;
		}
		return false;
	}

	public boolean containsExtractedOrInlinedOperationInvocation(AbstractCodeMapping mapping) {
		if(containsExtractedOperationInvocation(mapping))
			return true;
		if(containsInlinedOperationInvocation(mapping))
			return true;
		return false;
	}

	public boolean containsInlinedOperationInvocation(AbstractCodeMapping mapping) {
		return containsOperationInvocation(mapping, mapping.getFragment1());
	}

	public boolean containsExtractedOperationInvocation(AbstractCodeMapping mapping) {
		return containsOperationInvocation(mapping, mapping.getFragment2());
	}

	private boolean containsOperationInvocation(AbstractCodeMapping mapping, AbstractCodeFragment fragment) {
		if(operationInvocation != null) {
			if(fragment.getLocationInfo().subsumes(operationInvocation.getLocationInfo())) {
				AbstractCall invocation = fragment.invocationCoveringEntireFragment();
				if(invocation == null) {
					invocation = fragment.assignmentInvocationCoveringEntireStatement();
				}
				if(invocation != null && invocation.equals(operationInvocation)) {
					return true;
				}
				else if(fragment instanceof StatementObject) {
					if(fragment.getMethodInvocations().contains(operationInvocation)) {
						return true;
					}
				}
				else if(fragment instanceof CompositeStatementObject) {
					CompositeStatementObject comp = (CompositeStatementObject)fragment;
					for(AbstractExpression exp : comp.getExpressions()) {
						if(exp.getMethodInvocations().contains(operationInvocation)) {
							return true;
						}
					}
				}
			}
			if(parentMapper != null) {
				for(UMLOperationBodyMapper childMapper : parentMapper.childMappers) {
					if(childMapper.operationInvocation != null && !this.operationInvocation.getName().equals(childMapper.operationInvocation.getName())) {
						for(AbstractCall call : fragment.getMethodInvocations()) {
							if(call.equals(childMapper.operationInvocation)) {
								return true;
							}
						}
					}
				}
			}
		}
		else if(childMappers.size() > 0) {
			for(UMLOperationBodyMapper childMapper : childMappers) {
				if(childMapper.containsExtractedOrInlinedOperationInvocation(mapping)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containsMapping(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2) {
		if(fragment1 != null && fragment2 != null) {
			for(AbstractCodeMapping mapping : this.mappings) {
				if(mapping.getFragment1().equals(fragment1) && mapping.getFragment2().equals(fragment2)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsParentMapping(AbstractCodeMapping mapping) {
		CompositeStatementObject parent1 = mapping.getFragment1().getParent();
		CompositeStatementObject parent2 = mapping.getFragment2().getParent();
		if(parent1 != null && parent2 != null) {
			for(AbstractCodeMapping previousMapping : this.mappings) {
				if(previousMapping.getFragment1().equals(parent1) && previousMapping.getFragment2().equals(parent2)) {
					boolean isMethodBodyBlock1 = parent1.getParent() == null;
					boolean isMethodBodyBlock2 = parent2.getParent() == null;
					if(isMethodBodyBlock1 == isMethodBodyBlock2) {
						return true;
					}
				}
			}
			if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent1 = parent1.getParent(); 
				}
				while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent2 = parent2.getParent(); 
				}
				for(AbstractCodeMapping previousMapping : this.mappings) {
					if(previousMapping.getFragment1().equals(parent1) && previousMapping.getFragment2().equals(parent2)) {
						return true;
					}
				}
			}
		}
		else if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
			//check if mapping corresponds to the bodies of matched lambda expressions
			for(AbstractCodeMapping previousMapping : this.mappings) {
				if(previousMapping.getFragment1().getLambdas().size() > 0 && previousMapping.getFragment2().getLambdas().size() > 0) {
					LambdaExpressionObject lambda1 = previousMapping.getFragment1().getLambdas().get(0);
					LambdaExpressionObject lambda2 = previousMapping.getFragment2().getLambdas().get(0);
					if(lambda1.getBody() != null && lambda1.getBody().getCompositeStatement().equals(mapping.getFragment1()) &&
							lambda2.getBody() != null && lambda2.getBody().getCompositeStatement().equals(mapping.getFragment2())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean parentIsContainerBody(AbstractCodeMapping mapping) {
		CompositeStatementObject parent1 = mapping.getFragment1().getParent();
		CompositeStatementObject parent2 = mapping.getFragment2().getParent();
		if(parent1 != null && parent2 != null) {
			if(parent1.equals(container1.getBody().getCompositeStatement()) || parent2.equals(container2.getBody().getCompositeStatement())) {
				return true;
			}
		}
		return false;
	}

	private boolean containsInvertCondition(CompositeStatementObject comp1, CompositeStatementObject comp2) {
		for(Refactoring refactoring : this.refactorings) {
			if(refactoring instanceof InvertConditionRefactoring) {
				InvertConditionRefactoring ref = (InvertConditionRefactoring)refactoring;
				if(ref.getOriginalConditional().equals(comp1) && ref.getInvertedConditional().equals(comp2)) {
					return true;
				}
			}
		}
		return false;
	}

	public Set<ReplacementType> getReplacementTypesExcludingParameterToArgumentMaps(AbstractCodeMapping mapping) {
		Set<ReplacementType> types = new LinkedHashSet<ReplacementType>();
		for(Replacement replacement : mapping.getReplacements()) {
			boolean skip = false;
			if(parameterToArgumentMap1 != null) {
				for(Entry<String, String> entry : parameterToArgumentMap1.entrySet()) {
					if(entry.getKey().equals(replacement.getBefore()) || entry.getValue().equals(replacement.getAfter())) {
						skip = true;
					}
				}
			}
			if(parameterToArgumentMap2 != null) {
				for(Entry<String, String> entry : parameterToArgumentMap2.entrySet()) {
					if(entry.getKey().equals(replacement.getBefore()) || entry.getValue().equals(replacement.getAfter())) {
						skip = true;
					}
				}
			}
			for(Refactoring r : this.refactorings) {
				if(r instanceof ExtractVariableRefactoring) {
					ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
					AbstractExpression initializer = extract.getVariableDeclaration().getInitializer();
					if(initializer != null && initializer.getString().equals(replacement.getBefore())) {
						skip = true;
						break;
					}
				}
			}
			if(!skip) {
				types.add(replacement.getType());
			}
		}
		return types;
	}

	public int exactMappingsNestedUnderCompositeExcludingBlocks(CompositeStatementObjectMapping compositeMapping) {
		int count = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.equals(compositeMapping)) {
				break;
			}
			if(compositeMapping.getFragment1().getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo()) &&
					compositeMapping.getFragment2().getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) &&
					!mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && 
					!mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
					(mapping.getFragment1().getString().equals(mapping.getFragment2().getString()) ||
					mapping.editDistance() < 0.1)) {
				count++;
			}
		}
		return count;
	}

	public int mappingsNestedUnderCompositeExcludingBlocks(CompositeStatementObjectMapping compositeMapping) {
		int count = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.equals(compositeMapping)) {
				break;
			}
			if(compositeMapping.getFragment1().getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo()) &&
					compositeMapping.getFragment2().getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) &&
					!mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && 
					!mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
				count++;
			}
		}
		return count;
	}

	public boolean identicalMappings(UMLOperationBodyMapper other) {
		if(this.mappings.size() == other.mappings.size()) {
			Iterator<AbstractCodeMapping> thisIt = this.mappings.iterator();
			Iterator<AbstractCodeMapping> otherIt = other.mappings.iterator();
			int matchCount = 0;
			while(thisIt.hasNext()) {
				AbstractCodeMapping thisMapping = thisIt.next();
				AbstractCodeMapping otherMapping = otherIt.next();
				if(thisMapping.getFragment1().getLocationInfo().equals(otherMapping.getFragment1().getLocationInfo()) &&
						thisMapping.getFragment2().getLocationInfo().equals(otherMapping.getFragment2().getLocationInfo())) {
					matchCount++;
				}
			}
			return matchCount == this.mappings.size();
		}
		return false;
	}

	public Set<VariableDeclaration> getRemovedVariables() {
		return removedVariables;
	}

	public Set<VariableDeclaration> getAddedVariables() {
		return addedVariables;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMovedVariables() {
		return movedVariables;
	}

	public boolean hasAnonymousClassDiffNestedUnderLambda() {
		for(UMLAnonymousClassDiff diff : anonymousClassDiffs) {
			UMLAnonymousClass anonymous1 = (UMLAnonymousClass) diff.getOriginalClass();
			UMLAnonymousClass anonymous2 = (UMLAnonymousClass) diff.getNextClass();
			if(anonymous1.getParentContainers().size() > 0 && anonymous2.getParentContainers().size() > 0) {
				VariableDeclarationContainer container1 = anonymous1.getParentContainers().iterator().next();
				VariableDeclarationContainer container2 = anonymous2.getParentContainers().iterator().next();
				boolean nestedUnderLambda1 = false;
				for(LambdaExpressionObject lambda1 : container1.getAllLambdas()) {
					if(lambda1.getAnonymousClassList().contains(anonymous1)) {
						nestedUnderLambda1 = true;
					}
				}
				boolean nestedUnderLambda2 = false;
				for(LambdaExpressionObject lambda2 : container2.getAllLambdas()) {
					if(lambda2.getAnonymousClassList().contains(anonymous2)) {
						nestedUnderLambda2 = true;
					}
				}
				return nestedUnderLambda1 && nestedUnderLambda2;
			}
		}
		return false;
	}
}
