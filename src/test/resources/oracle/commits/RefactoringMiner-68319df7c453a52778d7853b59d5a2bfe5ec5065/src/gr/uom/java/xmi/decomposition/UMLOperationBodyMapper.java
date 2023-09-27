package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.ListCompositeType;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.*;
import gr.uom.java.xmi.decomposition.replacement.ClassInstanceCreationWithMethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.InitializerReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationWithClassInstanceCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.ObjectCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.AddParameterRefactoring;
import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;
import gr.uom.java.xmi.diff.CandidateMergeVariableRefactoring;
import gr.uom.java.xmi.diff.CandidateSplitVariableRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.InvertConditionRefactoring;
import gr.uom.java.xmi.diff.MergeCatchRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.ReferenceBasedRefactoring;
import gr.uom.java.xmi.diff.RemoveParameterRefactoring;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithLambdaRefactoring;
import gr.uom.java.xmi.diff.ReplaceLoopWithPipelineRefactoring;
import gr.uom.java.xmi.diff.ReplacePipelineWithLoopRefactoring;
import gr.uom.java.xmi.diff.SplitConditionalRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
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
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

public class UMLOperationBodyMapper implements Comparable<UMLOperationBodyMapper> {
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
	private static final int MAXIMUM_NUMBER_OF_COMPARED_STRINGS = 100;
	private static final int MAXIMUM_NUMBER_OF_COMPARED_STATEMENTS = 1000;
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
	private boolean nested;
	private AbstractCall operationInvocation;
	private Map<String, String> parameterToArgumentMap1;
	private Map<String, String> parameterToArgumentMap2;
	private Set<CompositeStatementObjectMapping> ifBecomingElseIf = new HashSet<>();
	private Set<CompositeStatementObjectMapping> ifAddingElseIf = new HashSet<>();

	public boolean isNested() {
		return nested;
	}

	private Set<AbstractCodeFragment> statementsWithStreamAPICalls(List<AbstractCodeFragment> leaves) {
		Set<AbstractCodeFragment> streamAPICalls = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(" -> ") ||
					invocation.actualString().contains("::"))) {
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

	private boolean streamAPIName(String name) {
		return name.equals("stream") || name.equals("filter") || name.equals("forEach") || name.equals("collect") || name.equals("map") || name.equals("removeIf");
	}

	private List<AbstractCall> streamAPICalls(AbstractCodeFragment leaf) {
		List<AbstractCodeFragment> list = new ArrayList<>();
		list.add(leaf);
		return streamAPICalls(list);
	}

	private List<AbstractCall> streamAPICalls(List<AbstractCodeFragment> leaves) {
		List<AbstractCall> streamAPICalls = new ArrayList<AbstractCall>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(" -> ") ||
					invocation.actualString().contains("::"))) {
				for(AbstractCall inv : statement.getMethodInvocations()) {
					if(streamAPIName(inv.getName())) {
						streamAPICalls.add(inv);
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
		List<AbstractCodeFragment> leaves2 = new ArrayList<>(mapper2.getNonMappedLeavesT2());
		List<CompositeStatementObject> innerNodes1 = new ArrayList<>(mapper1.getNonMappedInnerNodesT1());
		List<CompositeStatementObject> innerNodes2 = new ArrayList<>(mapper2.getNonMappedInnerNodesT2());
		resetNodes(leaves1);
		resetNodes(leaves2);
		processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		resetNodes(innerNodes1);
		resetNodes(innerNodes2);
		processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), false);
		nonMappedLeavesT1.addAll(leaves1);
		nonMappedLeavesT2.addAll(leaves2);
		nonMappedInnerNodesT1.addAll(innerNodes1);
		nonMappedInnerNodesT2.addAll(innerNodes2);
	}

	public UMLOperationBodyMapper(UMLOperation removedOperation, UMLOperationBodyMapper mapper2, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
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
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}

	public UMLOperationBodyMapper(UMLOperationBodyMapper mapper1, UMLOperation addedOperation, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
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
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
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
		OperationBody body1 = operation1.getBody();
		OperationBody body2 = operation2.getBody();
		if(body1 != null && body2 != null) {
			List<AnonymousClassDeclarationObject> anonymous1 = body1.getAllAnonymousClassDeclarations();
			List<AnonymousClassDeclarationObject> anonymous2 = body2.getAllAnonymousClassDeclarations();
			List<LambdaExpressionObject> lambdas1 = body1.getAllLambdas();
			List<LambdaExpressionObject> lambdas2 = body2.getAllLambdas();
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
			boolean anonymousCollapse = Math.abs(totalNodes1 - totalNodes2) > 2*Math.min(totalNodes1, totalNodes2);
			if(!operation1.isDeclaredInAnonymousClass() && !operation2.isDeclaredInAnonymousClass() && anonymousCollapse) {
				if((anonymous1.size() == 1 && anonymous2.size() == 0) ||
						(anonymous1.size() == 1 && anonymous2.size() == 1 && anonymous1.get(0).getAnonymousClassDeclarations().size() > 0 && anonymous2.get(0).getAnonymousClassDeclarations().size() == 0)) {
					AbstractCodeFragment anonymousFragment = null;
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(leaf1.getAnonymousClassDeclarations().size() > 0) {
							anonymousFragment = leaf1;
							break;
						}
					}
					if(anonymousFragment != null) {
						expandAnonymousAndLambdas(anonymousFragment, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, true);
					}
				}
				else if(lambdas1.size() == 1 && anonymous2.size() == 0 && lambdas2.size() == 0) {
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
						(anonymous1.size() == 1 && anonymous2.size() == 1 && anonymous1.get(0).getAnonymousClassDeclarations().size() == 0 && anonymous2.get(0).getAnonymousClassDeclarations().size() > 0)) {
					AbstractCodeFragment anonymousFragment = null;
					for(AbstractCodeFragment leaf2 : leaves2) {
						if(leaf2.getAnonymousClassDeclarations().size() > 0) {
							anonymousFragment = leaf2;
							break;
						}
					}
					if(anonymousFragment != null) {
						expandAnonymousAndLambdas(anonymousFragment, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, true);
					}
				}
				else if(anonymous1.size() == 0 && lambdas1.size() == 0 && lambdas2.size() == 1) {
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
			}
			Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
			Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				for(AbstractCodeFragment streamAPICall : streamAPIStatements2) {
					if(streamAPICall.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(streamAPICall, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2, false);
					}
				}
			}
			else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
				for(AbstractCodeFragment streamAPICall : streamAPIStatements1) {
					if(streamAPICall.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(streamAPICall, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1, false);
					}
				}
			}
			this.operationSignatureDiff = new UMLOperationDiff(operation1, operation2, classDiff);
			Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<String, String>();
			Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<String, String>();
			List<UMLParameter> addedParameters = operationSignatureDiff.getAddedParameters();
			if(addedParameters.size() == 1) {
				UMLParameter addedParameter = addedParameters.get(0);
				if(!operation1.isDeclaredInAnonymousClass() && UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName())) {
					parameterToArgumentMap1.put("this.", "");
					//replace "parameterName." with ""
					parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
				}
			}
			List<UMLParameter> removedParameters = operationSignatureDiff.getRemovedParameters();
			if(removedParameters.size() == 1) {
				UMLParameter removedParameter = removedParameters.get(0);
				if(!operation2.isDeclaredInAnonymousClass() && UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
					parameterToArgumentMap2.put("this.", "");
				}
			}
			List<UMLParameterDiff> parameterDiffList = operationSignatureDiff.getParameterDiffList();
			for(UMLParameterDiff parameterDiff : parameterDiffList) {
				UMLParameter addedParameter = parameterDiff.getAddedParameter();
				UMLParameter removedParameter = parameterDiff.getRemovedParameter();
				if(!operation1.isDeclaredInAnonymousClass() && !operation2.isDeclaredInAnonymousClass() &&
						UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName()) &&
						UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put("this.", "");
					parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
					parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
					parameterToArgumentMap2.put("this.", "");
				}
			}
			if(classDiff != null) {
				for(UMLAttribute attribute : classDiff.getOriginalClass().getAttributes()) {
					if(!operation2.isDeclaredInAnonymousClass() && UMLModelDiff.looksLikeSameType(attribute.getType().getClassType(), operation2.getClassName())) {
						parameterToArgumentMap1.put(attribute.getName() + ".", "");
						parameterToArgumentMap2.put("this.", "");
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
				if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					remainingUnmatchedIfStatements1++;
				}
				for(AbstractExpression expression : composite.getExpressions()) {
					expression.replaceParametersWithArguments(parameterToArgumentMap1);
					expressionsT1.add(expression);
				}
			}
			for(AbstractCodeMapping mapping : mappings) {
				if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					CompositeStatementObject composite = (CompositeStatementObject)mapping.getFragment1();
					for(AbstractExpression expression : composite.getExpressions()) {
						expression.replaceParametersWithArguments(parameterToArgumentMap1);
						expressionsT1.add(expression);
					}
				}
				if(remainingUnmatchedIfStatements1 > 0 && mapping instanceof LeafMapping && mapping.getFragment1().getTernaryOperatorExpressions().size() == 0 &&
						mapping.getFragment2().getTernaryOperatorExpressions().size() > 0 && !leaves2.contains(mapping.getFragment2())) {
					leaves2.add(mapping.getFragment2());
					//remove from hashCodes, so that it can be re-matched
					mappingHashcodesT2.remove(mapping.getFragment2().hashCode());
				}
			}
			int numberOfMappings = mappings.size();
			processLeaves(expressionsT1, leaves2, parameterToArgumentMap2, false);
			List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
			for(int i = numberOfMappings; i < mappings.size(); i++) {
				if(!isSplitConditionalExpression(mappings.get(i))) {
					mappings.get(i).temporaryVariableAssignment(refactorings, parentMapper != null);
				}
				else {
					this.mappings.remove(mappings.get(i));
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
								if(parent1 instanceof TryStatementObject && parent1.getParent().getParent() == null) {
									break;
								}
								if(parent2 instanceof TryStatementObject && parent2.getParent().getParent() == null) {
									break;
								}
								if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) && parent1.getParent().getParent() == null) {
									break;
								}
								if(parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) && parent2.getParent().getParent() == null) {
									break;
								}
								if(child1.getAnonymousClassDeclarations().size() > 0 && child2.getAnonymousClassDeclarations().size() > 0) {
									break;
								}
								if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) != parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
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
										!isTryBlock(child1, parent1) && !isTryBlock(child2, parent2) &&
										!isFinallyBlock(child1, parent1) && !isFinallyBlock(child2, parent2) &&
										!ifAddingElseIf(parent1.getParent()) && !ifAddingElseIf(parent2.getParent())) {
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
			
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			Set<AbstractCodeFragment> leavesToBeRemovedT1 = new LinkedHashSet<>();
			Set<AbstractCodeFragment> leavesToBeRemovedT2 = new LinkedHashSet<>();
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
				if(statement.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration = statement.getVariableDeclarations().get(0);
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && (initializer.getMethodInvocations().size() > 0 || initializer.getCreations().size() > 0)) {
						for(AbstractCodeFragment nonMappedLeaf1 : nonMappedLeavesT1) {
							boolean matchingVariableDeclaration = false;
							List<VariableDeclaration> declarations1 = nonMappedLeaf1.getVariableDeclarations();
							for(VariableDeclaration declaration1 : declarations1) {
								if(declaration1.getVariableName().equals(declaration.getVariableName()) && declaration1.equalType(declaration)) {
									matchingVariableDeclaration = true;
									break;
								}
							}
							if(!matchingVariableDeclaration && !containsMethodSignatureOfAnonymousClass(nonMappedLeaf1.getString()) &&
									!nonMappedLeaf1.getString().endsWith("=" + initializer + ";\n") && !nonMappedLeaf1.getString().contains("=" + initializer + ".") &&
									nonMappedLeaf1.getString().contains(initializer.getString())) {
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
								if(!matchingInlinedOperationLeaf) {
									ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, parentMapper != null);
									List<LeafExpression> subExpressions = nonMappedLeaf1.findExpression(initializer.getString());
									for(LeafExpression subExpression : subExpressions) {
										LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
									}
									refactorings.add(ref);
									leavesToBeRemovedT2.add(statement);
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
					if(initializer != null && (initializer.getMethodInvocations().size() > 0 || initializer.getCreations().size() > 0)) {
						for(AbstractCodeFragment nonMappedLeaf2 : nonMappedLeavesT2) {
							boolean matchingVariableDeclaration = false;
							List<VariableDeclaration> declarations2 = nonMappedLeaf2.getVariableDeclarations();
							for(VariableDeclaration declaration2 : declarations2) {
								if(declaration2.getVariableName().equals(declaration.getVariableName()) && declaration2.equalType(declaration)) {
									matchingVariableDeclaration = true;
									break;
								}
							}
							if(!matchingVariableDeclaration && !containsMethodSignatureOfAnonymousClass(nonMappedLeaf2.getString()) &&
									!nonMappedLeaf2.getString().endsWith("=" + initializer + ";\n") && !nonMappedLeaf2.getString().contains("=" + initializer + ".") &&
									nonMappedLeaf2.getString().contains(initializer.getString())) {
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
								if(!matchingExtractedOperationLeaf) {
									InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, parentMapper != null);
									List<LeafExpression> subExpressions = nonMappedLeaf2.findExpression(initializer.getString());
									for(LeafExpression subExpression : subExpressions) {
										LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
									}
									refactorings.add(ref);
									leavesToBeRemovedT1.add(statement);
								}
							}
						}
					}
				}
			}
			nonMappedLeavesT1.removeAll(leavesToBeRemovedT1);
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

	private boolean isIfBranch(AbstractCodeFragment child, CompositeStatementObject parent) {
		return parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				child.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
				parent.getStatements().size() >= 1 && parent.getStatements().indexOf(child) == 0;
	}

	private boolean isElseBranch(AbstractCodeFragment child, CompositeStatementObject parent) {
		return parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				child.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
				parent.getStatements().size() == 2 && parent.getStatements().indexOf(child) == 1;
	}

	private boolean hasElseBranch(CompositeStatementObject parent) {
		if(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			return parent.getStatements().size() == 2 && parent.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
		}
		return false;
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
	}

	private UMLOperationBodyMapper(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2, UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
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
				processCompositeStatements(composite1.getLeaves(), composite2.getLeaves(), composite1.getInnerNodes(), composite2.getInnerNodes());
			}
		}
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
		processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>(), isomorphic);
		
		processInnerNodes(innerNodes1, innerNodes2, leaves1, leaves2, new LinkedHashMap<String, String>(), containsCallToExtractedMethod(leaves2));
		
		if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
			processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
		}
		else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
			processStreamAPIStatements(leaves1, leaves2, streamAPIStatements1, innerNodes2);
		}
		
		nonMappedLeavesT1.addAll(leaves1);
		nonMappedLeavesT2.addAll(leaves2);
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
								if(matchingLambda) {
									streamAPICallStatement = leaf1;
									streamAPICalls = streamAPICalls(leaf1);
									break;
								}
							}
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
							additionallyMatchedStatements1.add(streamAPICallStatement);
							additionallyMatchedStatements2.add(fragment2);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(streamAPICall.getName().equals("forEach")) {
									if(!additionallyMatchedStatements2.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
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
												additionallyMatchedStatements2.add(composite);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
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
											if(streamAPICall.arguments().get(0).startsWith(argument + " -> ")) {
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
												if(streamAPICall.arguments().get(0).startsWith(variableDeclaration.getVariableName() + " -> ")) {
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
							else if(streamAPICall.getName().equals("forEach")) {
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
						ReplacePipelineWithLoopRefactoring ref = new ReplacePipelineWithLoopRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, container1, container2);
						newMapping.addRefactoring(ref);
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
					if((composite.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) &&
							(compositeLeaves.contains(fragment1) || containLambdaExpression(compositeLeaves, fragment1) != null)) {
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
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
							additionallyMatchedStatements1.add(fragment1);
							additionallyMatchedStatements2.add(streamAPICallStatement);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(streamAPICall.getName().equals("forEach")) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(composite);
												break;
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("stream")) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(composite);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
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
				if(composite.getExpressions().contains(fragment1)) {
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
											if(streamAPICall.arguments().get(0).startsWith(argument + " -> ")) {
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
												if(streamAPICall.arguments().get(0).startsWith(variableDeclaration.getVariableName() + " -> ")) {
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
							else if(streamAPICall.getName().equals("forEach")) {
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
						addToMappings(newMapping, mappingSet);
						leaves2.remove(newMapping.getFragment2());
						innerNodeIterator1.remove();
					}
				}
			}
		}
	}

	private UMLOperationBodyMapper(UMLOperation anonymousClassOperation, LambdaExpressionObject lambda2, UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
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
	}

	public void addChildMapper(UMLOperationBodyMapper mapper) {
		this.childMappers.add(mapper);
		//TODO add logic to remove the mappings from "this" mapper,
		//which are less similar than the mappings of the mapper passed as parameter
	}

	public UMLAbstractClassDiff getClassDiff() {
		return classDiff;
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
				if(fragment1.equals("return " + r.getBefore() + ";\n") && fragment2.equals("return " + r.getAfter() + ";\n")) {
					return true;
				}
			}
		}
		return false;
	}

	public UMLOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper, UMLOperation addedOperation,
			Map<String, String> parameterToArgumentMap1, Map<String, String> parameterToArgumentMap2, UMLAbstractClassDiff classDiff, AbstractCall operationInvocation, boolean nested) throws RefactoringMinerTimedOutException {
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
							expressionsT2.add(expression);
						}
					}
					else {
						leavesT2.add(fragment);
					}
				}
			}
			leavesT2.addAll(operationBodyMapper.getNonMappedLeavesT2());
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT2.add(expression);
				}
			}
			this.callsToExtractedMethod = callsToExtractedMethod(leavesT2);
			this.callsToExtractedMethod += callsToExtractedMethod(expressionsT2);
			CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
			List<AbstractCodeFragment> leaves1 = operationBodyMapper.getNonMappedLeavesT1();
			List<CompositeStatementObject> innerNodes1 = operationBodyMapper.getNonMappedInnerNodesT1();
			//adding leaves that were mapped with replacements
			Set<AbstractCodeFragment> addedLeaves1 = new LinkedHashSet<AbstractCodeFragment>();
			Set<CompositeStatementObject> addedInnerNodes1 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeFragment nonMappedLeaf1 : new ArrayList<>(operationBodyMapper.getNonMappedLeavesT1())) {
				expandAnonymousAndLambdas(nonMappedLeaf1, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
			}
			List<AbstractCodeFragment> leaves2 = composite2.getLeaves();
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
						if(mapping.getFragment1().getString().equals("return null;\n")) {
							AbstractCodeFragment fragment = mapping.getFragment1();
							expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, container1, false);
						}
					}
				}
				for(AbstractCodeMapping mapping : operationBodyMapper.getParentMapper().getMappings()) {
					if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
						if((mapping.getFragment1().getString().equals("return true;\n") || mapping.getFragment1().getString().equals("return false;\n")) && addedOperation.getReturnParameter().getType().toString().equals("boolean")) {
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
						if(!returnWithVariableReplacement(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()))) {
							AbstractCodeFragment fragment = mapping.getFragment1();
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
					if(((CompositeStatementObjectMapping)mapping).getCompositeChildMatchingScore() == 0) {
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
				mappings.get(i).temporaryVariableAssignment(refactorings, parentMapper != null);
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
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
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
					}
				}
			}
			if(!statement.getLambdas().isEmpty()) {
				for(LambdaExpressionObject lambda : statement.getLambdas()) {
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
					if(((CompositeStatementObjectMapping)mapping).getCompositeChildMatchingScore() == 0) {
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
				mappings.get(i).temporaryVariableAssignment(refactorings, parentMapper != null);
			}
			
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves1.removeAll(addedLeaves1);
			leaves2.removeAll(addedLeaves2);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes1.removeAll(addedInnerNodes1);
			innerNodes2.removeAll(addedInnerNodes2);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
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

	public int mappingsWithoutBlocks() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement())
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

	private void inlinedVariableAssignment(AbstractCodeFragment statement, List<AbstractCodeFragment> nonMappedLeavesT2) {
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeMapping mapping : getMappings()) {
			mapping.inlinedVariableAssignment(statement, nonMappedLeavesT2, classDiff, parentMapper != null);
			for(Refactoring newRefactoring : mapping.getRefactorings()) {
				if(!this.refactorings.contains(newRefactoring)) {
					this.refactorings.add(newRefactoring);
				}
				else {
					for(Refactoring refactoring : this.refactorings) {
						if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
							InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
							Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
							InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
							oldInlineVariableRefactoring.addReferences(newReferences);
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

	private void temporaryVariableAssignment(AbstractCodeFragment statement, List<AbstractCodeFragment> nonMappedLeavesT2) {
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeMapping mapping : getMappings()) {
			mapping.temporaryVariableAssignment(statement, nonMappedLeavesT2, classDiff, parentMapper != null);
			for(Refactoring newRefactoring : mapping.getRefactorings()) {
				if(!this.refactorings.contains(newRefactoring)) {
					this.refactorings.add(newRefactoring);
				}
				else {
					for(Refactoring refactoring : this.refactorings) {
						if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractVariableRefactoring) {
							ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
							Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
							ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
							oldExtractVariableRefactoring.addReferences(newReferences);
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
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals("try"))
				count++;
		}
		return count;
	}

	public List<AbstractCodeMapping> getExactMatches() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals("try"))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	public List<AbstractCodeMapping> getExactMatchesWithoutLoggingStatements() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals("try")) {
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
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals("try") && mapping.getOperation1().equals(this.container1) && mapping.getOperation2().equals(this.container2))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	public boolean allMappingsAreExactMatches() {
		int mappings = this.mappingsWithoutBlocks();
		int tryMappings = 0;
		int mappingsWithTypeReplacement = 0;
		int mappingsWithVariableReplacement = 0;
		int mappingsWithMethodInvocationRename = 0;
		for(AbstractCodeMapping mapping : this.getMappings()) {
			if(mapping.getFragment1().getString().equals("try") && mapping.getFragment2().getString().equals("try")) {
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

	public boolean containsCompositeMappingWithoutReplacements() {
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping instanceof CompositeStatementObjectMapping && !mapping.getFragment1().equalFragment(mapping.getFragment2()) && mapping.getReplacements().isEmpty()) {
				return true;
			}
		}
		return false;
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
			}
		}
		return replacements;
	}

	public boolean involvesTestMethods() {
		return container1.hasTestAnnotation() && container2.hasTestAnnotation();
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
						double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, removedOperations, addedOperations, tryWithResourceMigration);
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
							double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, removedOperations, addedOperations, tryWithResourceMigration);
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
						double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, removedOperations, addedOperations, tryWithResourceMigration);
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
						double score = computeScore((CompositeStatementObject)block1, (CompositeStatementObject)block2, removedOperations, addedOperations, tryWithResourceMigration);
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
					double score = computeScore(lambda1.getBody().getCompositeStatement(), lambda2.getBody().getCompositeStatement(), removedOperations, addedOperations, tryWithResourceMigration);
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
				if(call.getName().equals("forEach")) {
					forEach1 = true;
					break;
				}
			}
		}
		boolean forEach2 = false;
		for(AbstractCodeFragment leaf2 : leaves2) {
			for(AbstractCall call : leaf2.getMethodInvocations()) {
				if(call.getName().equals("forEach")) {
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

	private void processInnerNodes(List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2, List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap, List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration, boolean containsCallToExtractedMethod,
			Map<String, List<CompositeStatementObject>> map1, Map<String, List<CompositeStatementObject>> map2) throws RefactoringMinerTimedOutException {
		if(innerNodes1.size() <= innerNodes2.size()) {
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
								double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
								if(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
									mappingSet.add(mapping);
								}
							}
						}
					}
					if(!mappingSet.isEmpty()) {
						Set<AbstractCodeMapping> movedInIfElseBranch = movedInIfElseIfBranch(mappingSet);
						if(movedInIfElseBranch.size() > 1) {
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
							Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo);
							
							double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
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
							}
							if((replacements != null || identicalBody(statement1, statement2) || allLeavesWithinBodyMapped(statement1, statement2)) &&
									(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
								CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
								mapping.addReplacements(replacements);
								UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
								for(AbstractCodeFragment leaf : leaves2) {
									if(leaf.getLocationInfo().before(mapping.getFragment2().getLocationInfo())) {
										mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
										if(mapping.isIdenticalWithExtractedVariable()) {
											break;
										}
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
					TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
					for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
						CompositeStatementObject statement1 = innerNodeIterator1.next();
						if(!alreadyMatched1(statement1)) {
							if(statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString()) || differOnlyInThis(statement1.getString(), statement2.getString())) {
								double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
								if(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
									CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
									mappingSet.add(mapping);
								}
							}
						}
					}
					if(!mappingSet.isEmpty()) {
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
			innerNodes2.removeAll(innerNodes2ToBeRemoved);
			
			// exact matching - inner nodes - with variable renames
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				if(!alreadyMatched2(statement2)) {
					TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
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
							Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo);
							
							double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
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
							}
							if((replacements != null || identicalBody(statement1, statement2) || allLeavesWithinBodyMapped(statement1, statement2)) &&
									(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
								CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
								mapping.addReplacements(replacements);
								UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
								for(AbstractCodeFragment leaf : leaves2) {
									if(leaf.getLocationInfo().before(mapping.getFragment2().getLocationInfo())) {
										mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
										if(mapping.isIdenticalWithExtractedVariable()) {
											break;
										}
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
								mappingSet.add(mapping);
							}
						}
					}
					if(!mappingSet.isEmpty()) {
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
							if(switchCase2.getString().startsWith("case ")) {
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
							if(switchCase1.getString().startsWith("case ")) {
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

	private List<String> extractCommentsWithinStatement(CompositeStatementObject statement, VariableDeclarationContainer container) {
		List<UMLComment> comments1 = container.getComments();
		List<String> commentsWithinStatement1 = new ArrayList<>();
		for(UMLComment comment1 : comments1) {
			if(statement.getLocationInfo().subsumes(comment1.getLocationInfo())) {
				commentsWithinStatement1.add(comment1.getText());
			}
		}
		return commentsWithinStatement1;
	}

	public boolean alreadyMatched1(AbstractCodeFragment fragment) {
		if(fragment instanceof AbstractExpression) {
			for(AbstractCodeMapping mapping : mappings) {
				if(!(mapping instanceof CompositeStatementObjectMapping) && mapping.getFragment1().getLocationInfo().subsumes(fragment.getLocationInfo())) {
					return true;
				}
			}
			return false;
		}
		return mappingHashcodesT1.contains(fragment.hashCode());
	}

	public boolean alreadyMatched2(AbstractCodeFragment fragment) {
		if(fragment instanceof AbstractExpression) {
			for(AbstractCodeMapping mapping : mappings) {
				if(!(mapping instanceof CompositeStatementObjectMapping) && mapping.getFragment2().getLocationInfo().subsumes(fragment.getLocationInfo())) {
					return true;
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

	private double computeScore(CompositeStatementObject statement1, CompositeStatementObject statement2,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration) {
		if(statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
			return compositeChildMatchingScore((TryStatementObject)statement1, (TryStatementObject)statement2, mappings, removedOperations, addedOperations, tryWithResourceMigration);
		}
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			for(AbstractCodeMapping mapping : mappings) {
				if(mapping.getFragment1() instanceof TryStatementObject && mapping.getFragment2() instanceof TryStatementObject) {
					TryStatementObject try1 = (TryStatementObject)mapping.getFragment1();
					TryStatementObject try2 = (TryStatementObject)mapping.getFragment2();
					if(try1.getCatchClauses().contains(statement1) && try2.getCatchClauses().contains(statement2)) {
						return compositeChildMatchingScore(statement1, statement2, mappings, removedOperations, addedOperations);
					}
				}
			}
			return -1;
		}
		return compositeChildMatchingScore(statement1, statement2, mappings, removedOperations, addedOperations);
	}

	private CompositeStatementObjectMapping createCompositeMapping(CompositeStatementObject statement1,
			CompositeStatementObject statement2, Map<String, String> parameterToArgumentMap, double score) {
		VariableDeclarationContainer container1 = codeFragmentOperationMap1.containsKey(statement1) ? codeFragmentOperationMap1.get(statement1) : this.container1;
		VariableDeclarationContainer container2 = codeFragmentOperationMap2.containsKey(statement2) ? codeFragmentOperationMap2.get(statement2) : this.container2;
		CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, container1, container2, score);
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

	private void processLeaves(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2,
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
						TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
						int matchCount = 0;
						for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
							AbstractCodeFragment leaf2 = leafIterator2.next();
							if(!alreadyMatched2(leaf2)) {
								String argumentizedString1 = preprocessInput1(leaf1, leaf2);
								String argumentizedString2 = preprocessInput2(leaf1, leaf2);
								if(leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) {
									matchCount++;
									if(leaf1.getDepth() == leaf2.getDepth()) {
										LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
										mappingSet.add(mapping);
									}
								}
							}
						}
						if(parentMapper != null && matchCount > 1) {
							continue;
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
					boolean allMatchingLeaves1InMethodScope = parents1.size() == 1 && parents1.iterator().next() != null && parents1.iterator().next().getParent() == null;
					boolean allMatchingLeaves2InMethodScope = parents2.size() == 1 && parents2.iterator().next() != null && parents2.iterator().next().getParent() == null;
					if(matchingLeaves1.size() > matchingLeaves2.size() && matchingLeaves2.size() > 0 && !allMatchingLeaves1InMethodScope && !allMatchingLeaves2InMethodScope) {
						processLeaves(matchingLeaves1, matchingLeaves2, parameterToArgumentMap, isomorphic);
						for(AbstractCodeMapping mapping : this.mappings) {
							leaves2.remove(mapping.getFragment2());
							if(mapping.getFragment1().equals(leaf1)) {
								leafIterator1.remove();
							}
						}
						continue;
					}
					TreeSet<LeafMapping> mappingSet = parentMapping != null ? new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForInline(parentMapping)) : new TreeSet<LeafMapping>();
					for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
						AbstractCodeFragment leaf2 = leafIterator2.next();
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
						boolean identicalPreviousAndNextStatement = parentMapper == null && mappingSet.first().hasIdenticalPreviousAndNextStatement();
						if(mappingSet.size() > 1 && (parentMapper != null || codeUnderIfMovedUnderElse) && mappings.size() > 1 && !identicalPreviousAndNextStatement) {
							TreeMap<Integer, LeafMapping> lineDistanceMap = new TreeMap<>();
							TreeMap<Double, LeafMapping> levelParentEditDistanceSum = new TreeMap<>();
							for(LeafMapping mapping : mappingSet) {
								int lineDistance = lineDistanceFromExistingMappings2(mapping).getMiddle();
								levelParentEditDistanceSum.put(mapping.levelParentEditDistanceSum(), mapping);
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
			
			// exact matching with variable renames
			Set<AbstractCodeFragment> leaves1ToBeRemoved = new LinkedHashSet<>();
			Set<AbstractCodeFragment> leaves2ToBeRemoved = new LinkedHashSet<>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				if(!alreadyMatched1(leaf1)) {
					TreeSet<LeafMapping> mappingSet = parentMapping != null ? new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForInline(parentMapping)) : new TreeSet<LeafMapping>();
					for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
						AbstractCodeFragment leaf2 = leafIterator2.next();
						if(!alreadyMatched2(leaf2)) {
							ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
							Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo);
							if (replacements != null) {
								LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
								mapping.addReplacements(replacements);
								extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf2, mapping);
								mappingSet.add(mapping);
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
							boolean isTestMethod = container1.hasTestAnnotation() || container2.hasTestAnnotation() || container1.getName().startsWith("test") || container2.getName().startsWith("test");
							if(!isTestMethod)
								checkForOtherPossibleMatchesForFragment2(leaves1, leaves2, leaf1, mappingSet, parameterToArgumentMap, equalNumberOfAssertions);
							Set<AbstractCodeMapping> movedInIfElseBranch = movedInIfElseIfBranch(mappingSet);
							Set<AbstractCodeMapping> movedOutOfIfElseBranch = movedOutOfIfElseIfBranch(mappingSet);
							if(movedInIfElseBranch.size() > 1) {
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
							else {
								LeafMapping minStatementMapping = mappingSet.first();
								if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
									addToMappings(minStatementMapping, mappingSet);
									leaves2.remove(minStatementMapping.getFragment2());
									if(minStatementMapping.getFragment1().equals(leaf1)) {
										leafIterator1.remove();
									}
									checkForSplitVariableDeclaration(minStatementMapping.getFragment1(), leaves2, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves2ToBeRemoved);
									checkForMergedVariableDeclaration(minStatementMapping.getFragment2(), leaves1, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves1ToBeRemoved);
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
						TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
						int matchCount = 0;
						for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
							AbstractCodeFragment leaf1 = leafIterator1.next();
							if(!alreadyMatched1(leaf1)) {
								String argumentizedString1 = preprocessInput1(leaf1, leaf2);
								String argumentizedString2 = preprocessInput2(leaf1, leaf2);
								if(leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) {
									matchCount++;
									if(leaf1.getDepth() == leaf2.getDepth()) {
										LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
										mappingSet.add(mapping);
									}
								}
							}
						}
						if(parentMapper != null && matchCount > 1) {
							continue;
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
						for(AbstractCodeMapping mapping : this.mappings) {
							leaves1.remove(mapping.getFragment1());
							if(mapping.getFragment2().equals(leaf2)) {
								leafIterator2.remove();
							}
						}
						continue;
					}
					TreeSet<LeafMapping> mappingSet = parentMapping != null ? new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForExtract(parentMapping)) : new TreeSet<LeafMapping>();
					for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
						AbstractCodeFragment leaf1 = leafIterator1.next();
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
						if(mappingSet.size() > 1 && (parentMapper != null || (!identicalDepthAndIndex && !leaf2.isKeyword() && !leaf2.isLogCall())) && mappings.size() > 0 && !identicalPreviousAndNextStatement) {
							TreeMap<Integer, LeafMapping> lineDistanceMap = new TreeMap<>();
							TreeMap<Double, LeafMapping> levelParentEditDistanceSum = new TreeMap<>();
							for(LeafMapping mapping : mappingSet) {
								int lineDistance = lineDistanceFromExistingMappings1(mapping).getMiddle();
								levelParentEditDistanceSum.put(mapping.levelParentEditDistanceSum(), mapping);
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
									TreeSet<LeafMapping> scopedMappingSet = parentMapping != null ? new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForExtract(parentMapping)) : new TreeSet<LeafMapping>();
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
					TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
					for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
						AbstractCodeFragment leaf1 = leafIterator1.next();
						if(!alreadyMatched1(leaf1)) {
							ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
							Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo);
							if (replacements != null) {
								LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
								mapping.addReplacements(replacements);
								extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf2, mapping);
								mappingSet.add(mapping);
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
							if(isScopedMatch(startMapping, endMapping, parentMapping) && mappingSet.size() > 1) {
								TreeSet<LeafMapping> scopedMappingSet = parentMapping != null ? new TreeSet<LeafMapping>(new ScopedLeafMappingComparatorForExtract(parentMapping)) : new TreeSet<LeafMapping>();
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
										TreeMap<Integer, LeafMapping> lineDistanceMap = checkForOtherPossibleMatchesWithLineDistance(
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
												addToMappings(minStatementMapping, mappingSet);
												leaves1.remove(minStatementMapping.getFragment1());
												if(minStatementMapping.getFragment2().equals(leaf2)) {
													leafIterator2.remove();
												}
												checkForSplitVariableDeclaration(minStatementMapping.getFragment1(), leaves2, minStatementMapping, parameterToArgumentMap, equalNumberOfAssertions, leaves2ToBeRemoved);
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

	private boolean multiMappingCondition(List<AbstractCodeFragment> matchingLeaves1, List<AbstractCodeFragment> matchingLeaves2) {
		if(matchingLeaves1.size() != matchingLeaves2.size()) {
			return true;
		}
		else {
			int sameDepth = 0;
			for(int i=0; i<matchingLeaves1.size(); i++) {
				AbstractCodeFragment fragment1 = matchingLeaves1.get(i);
				AbstractCodeFragment fragment2 = matchingLeaves2.get(i);
				if(fragment1.getDepth() == fragment2.getDepth()) {
					sameDepth++;
				}
			}
			if(sameDepth != matchingLeaves1.size()) {
				return true;
			}
		}
		return false;
	}

	private void extractInlineVariableAnalysis(List<? extends AbstractCodeFragment> leaves1,
			List<? extends AbstractCodeFragment> leaves2, AbstractCodeFragment leaf1, AbstractCodeFragment leaf2,
			LeafMapping mapping) {
		UMLAbstractClassDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
		for(AbstractCodeFragment leaf : leaves2) {
			if(leaf.equals(leaf2)) {
				break;
			}
			mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
			if(mapping.isIdenticalWithExtractedVariable()) {
				break;
			}
		}
		for(AbstractCodeFragment leaf : leaves1) {
			if(leaf.equals(leaf1)) {
				break;
			}
			mapping.inlinedVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
			if(mapping.isIdenticalWithInlinedVariable()) {
				break;
			}
		}
		CompositeReplacement composite = mapping.containsCompositeReplacement();
		if(composite != null) {
			for(AbstractCodeFragment leaf : composite.getAdditionallyMatchedStatements1()) {
				mapping.inlinedVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
				if(mapping.isIdenticalWithInlinedVariable()) {
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
		for(AbstractCodeFragment leaf : leaves1) {
			if(!leaf.equals(leaf1)) {
				int numberOfMappings = mappings.size();
				ReplacementInfo replacementInfo = initializeReplacementInfo(leaf, leaf2, leaves1, leaves2);
				Set<Replacement> replacements = findReplacementsWithExactMatching(leaf, leaf2, parameterToArgumentMap, replacementInfo);
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
							counter++;
						}
						if(matchingReplacements == replacements.size()) {
							matchingMappings++;
						}
					}
					if(matchingMappings == mappingSet.size()) {
						LeafMapping mapping = createLeafMapping(leaf, leaf2, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						extractInlineVariableAnalysis(leaves1, leaves2, leaf, leaf2, mapping);
						mappingSet.add(mapping);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
						for(int i = numberOfMappings; i < mappings.size(); i++) {
							this.mappings.remove(mappings.get(i));
						}
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
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf, parameterToArgumentMap, replacementInfo);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
						boolean allowAdd = false;
						for(LeafMapping m : mappingSet) {
							if(mapping.levelParentEditDistanceSum() < m.levelParentEditDistanceSum()) {
								allowAdd = true;
								break;
							}
						}
						if(allowAdd) {
							extractInlineVariableAnalysis(leaves1, leaves2, leaf1, leaf, mapping);
							mappingSet.add(mapping);
						}
					}
				}
			}
		}
	}

	private TreeMap<Integer, LeafMapping> checkForOtherPossibleMatchesWithLineDistance(List<? extends AbstractCodeFragment> leaves1,
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
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf, parameterToArgumentMap, replacementInfo);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf, parameterToArgumentMap, equalNumberOfAssertions);
						mapping.addReplacements(replacements);
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
						boolean equalName = declaration1.getVariableName().equals(declaration2.getVariableName()) && mapping.getFragment1().getString().startsWith(declaration1.getVariableName() + "=");
						if((equalName || variableRenamed) && declaration1.getType().equals(declaration2.getType())) {
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
			}
		}
	}

	private void checkForSplitVariableDeclaration(AbstractCodeFragment leaf1, List<? extends AbstractCodeFragment> leaves2,
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
						boolean equalName = declaration1.getVariableName().equals(declaration2.getVariableName()) && mapping.getFragment2().getString().startsWith(declaration1.getVariableName() + "=");
						if((equalName || variableRenamed) && declaration1.getType().equals(declaration2.getType())) {
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
			}
		}
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
			if(firstLeaf2 == null && !fragment.getString().startsWith("return ") && !(fragment.getVariableDeclarations().size() > 0 && fragment.getVariableDeclarations().get(0).getInitializer() == null)) {
				firstLeaf2 = fragment;
			}
			else if(firstLeaf2 != null && fragment.getLocationInfo().getStartLine() < firstLeaf2.getLocationInfo().getStartLine()) {
				firstLeaf2 = fragment;
			}
			if(lastLeaf2 == null) {
				lastLeaf2 = fragment;
			}
			else if(fragment.getLocationInfo().getStartLine() > lastLeaf2.getLocationInfo().getStartLine() && !fragment.getString().startsWith("return ")) {
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
			if(firstLeaf1 == null && !fragment.getString().startsWith("return ") && !(fragment.getVariableDeclarations().size() > 0 && fragment.getVariableDeclarations().get(0).getInitializer() == null)) {
				firstLeaf1 = fragment;
			}
			else if(firstLeaf1 != null && fragment.getLocationInfo().getStartLine() < firstLeaf1.getLocationInfo().getStartLine()) {
				firstLeaf1 = fragment;
			}
			if(lastLeaf1 == null) {
				lastLeaf1 = fragment;
			}
			else if(fragment.getLocationInfo().getStartLine() > lastLeaf1.getLocationInfo().getStartLine() && !fragment.getString().startsWith("return ")) {
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
					return mapping; 
				} 
				if(statementContainingOperationInvocation.getParent() != null && statementContainingOperationInvocation.getParent().getParent() != null && 
						statementContainingOperationInvocation.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && 
						mapping.getFragment2().equals(statementContainingOperationInvocation.getParent().getParent())) { 
					return mapping; 
				} 
			} 
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
						if(fragment1.getString().contains("=")) {
							String assignedVariable = fragment1.getString().substring(0, fragment1.getString().indexOf("="));
							variableDeclarationNames1.add(assignedVariable);
						}
					}
					if(fragment2.assignmentInvocationCoveringEntireStatement() != null || fragment2.assignmentCreationCoveringEntireStatement() != null) {
						if(fragment2.getString().contains("=")) {
							String assignedVariable = fragment2.getString().substring(0, fragment2.getString().indexOf("="));
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
						boolean isWithinIfBranch = isIfBranch(fragment1.getParent(), fragment1.getParent().getParent());
						//boolean isWithinElseBranch = isElseBranch(fragment1.getParent(), fragment1.getParent().getParent());
						//boolean isWithinElseIfBranch = false;
						//if(fragment1.getParent().getParent().getParent() != null) {
						//	isWithinElseIfBranch = isElseIfBranch(fragment1.getParent().getParent(), fragment1.getParent().getParent().getParent());
						//}
						if(isWithinIfBranch && (hasElseBranch(fragment1.getParent().getParent()) || hasElseIfBranch(fragment1.getParent().getParent()))) {
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
		if(container1.equals(container2) || (parentMapper != null && !nested)) {
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
			if(ifFound && (elseIfFound || (elseFound && parentMap.size() == grandParentMap.size()) || grandParentIfElseIfChain) && (parentIfElseIfChain || grandParentIfElseIfChain)) {
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
						if(fragment1.getString().contains("=")) {
							String assignedVariable = fragment1.getString().substring(0, fragment1.getString().indexOf("="));
							variableDeclarationNames1.add(assignedVariable);
						}
					}
					if(fragment2.assignmentInvocationCoveringEntireStatement() != null || fragment2.assignmentCreationCoveringEntireStatement() != null) {
						if(fragment2.getString().contains("=")) {
							String assignedVariable = fragment2.getString().substring(0, fragment2.getString().indexOf("="));
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
						boolean isWithinIfBranch = isIfBranch(fragment2.getParent(), fragment2.getParent().getParent());
						//boolean isWithinElseBranch = isElseBranch(fragment2.getParent(), fragment2.getParent().getParent());
						//boolean isWithinElseIfBranch = false;
						//if(fragment2.getParent().getParent().getParent() != null) {
						//	isWithinElseIfBranch = isElseIfBranch(fragment2.getParent().getParent(), fragment2.getParent().getParent().getParent());
						//}
						if(isWithinIfBranch && (hasElseBranch(fragment2.getParent().getParent()) || hasElseIfBranch(fragment2.getParent().getParent()))) {
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
				return new LinkedHashSet<AbstractCodeMapping>(mappingSet);
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
							if(fragment1.equals(variableDeclaration.getVariableName() + "=" + variableDeclaration.getInitializer().getString() + ";\n")) {
								matches++;
							}
						}
					}
					else if(variableDeclaration.equals(mapping.getFragment2().getVariableDeclarations().get(0))) {
						String fragment1 = mapping.getFragment1().getString();
						if(variableDeclaration.getInitializer() != null) {
							if(fragment1.equals(variableDeclaration.getVariableName() + "=" + variableDeclaration.getInitializer().getString() + ";\n")) {
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
		if(parentMapper != null && (callsToExtractedMethod > 1 || nested || parentMapper.getChildMappers().size() > 0)) {
			return (startMapping != null && endMapping != null && (mappings.size() > 1 || startMapping.equals(endMapping))) || parentMapping != null;
		}
		return false;
	}

	private boolean isWithinScope(AbstractCodeMapping startMapping, AbstractCodeMapping endMapping, AbstractCodeMapping parentMapping, AbstractCodeMapping mappingToCheck,
			Set<VariableDeclaration> referencedVariableDeclarations1, Set<VariableDeclaration> referencedVariableDeclarations2) {
		if(parentMapper != null && (callsToExtractedMethod > 1 || nested || parentMapper.getChildMappers().size() > 0)) {
			if(startMapping != null && endMapping != null) {
				List<VariableDeclaration> variableDeclarations1 = mappingToCheck.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mappingToCheck.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.size() > 0 && referencedVariableDeclarations1.containsAll(variableDeclarations1) &&
						variableDeclarations2.size() > 0 && referencedVariableDeclarations2.containsAll(variableDeclarations2)) {
					return true;
				}
				if(startMapping.equals(endMapping)) {
					boolean isNextStatement = mappingToCheck.getFragment1().getLocationInfo().getStartLine() == startMapping.getFragment1().getLocationInfo().getEndLine() + 1 &&
							mappingToCheck.getFragment2().getLocationInfo().getStartLine() == startMapping.getFragment2().getLocationInfo().getEndLine() + 1;
					boolean isAfterNextStatement = mappingToCheck.getFragment1().getLocationInfo().getStartLine() == startMapping.getFragment1().getLocationInfo().getEndLine() + 2 &&
							mappingToCheck.getFragment2().getLocationInfo().getStartLine() == startMapping.getFragment2().getLocationInfo().getEndLine() + 1;
					if(isNextStatement || isAfterNextStatement) {
						return true;
					}
					return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= startMapping.getFragment1().getLocationInfo().getStartLine() &&
							mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= endMapping.getFragment1().getLocationInfo().getStartLine();
				}
				if(mappingToCheck.getFragment2().getLocationInfo().getStartLine() >= startMapping.getFragment2().getLocationInfo().getStartLine() &&
						mappingToCheck.getFragment2().getLocationInfo().getStartLine() <= endMapping.getFragment2().getLocationInfo().getStartLine()) {
					return mappingToCheck.getFragment1().getLocationInfo().getStartLine() >= startMapping.getFragment1().getLocationInfo().getStartLine() &&
							mappingToCheck.getFragment1().getLocationInfo().getStartLine() <= endMapping.getFragment1().getLocationInfo().getStartLine();
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

	private boolean allUnderTheSameParent(Set<LeafMapping> mappings) {
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
			}
		}
	}

	private void addAllMappings(Set<AbstractCodeMapping> mappings) {
		this.mappings.addAll(mappings);
		for(AbstractCodeMapping mapping : mappings) {
			mappingHashcodesT1.add(mapping.getFragment1().hashCode());
			mappingHashcodesT2.add(mapping.getFragment2().hashCode());
		}
	}

	public void addMapping(AbstractCodeMapping mapping) {
		this.mappings.add(mapping);
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
						ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
						oldExtractVariableRefactoring.addReferences(newReferences);
						for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
							oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						break;
					}
					if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
						InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
						InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
						oldInlineVariableRefactoring.addReferences(newReferences);
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
				for(AbstractCodeMapping m : this.mappings) {
					if(leafMapping.getFragment1().getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) &&
							!leafMapping.getFragment1().equals(m.getFragment1()) &&
							leafMapping.getFragment2().getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
							!leafMapping.getFragment2().equals(m.getFragment2())) {
						mappingsToBeRemoved.add(m);
					}
				}
				removeAllMappings(mappingsToBeRemoved);
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
		AbstractCodeMapping mappingToBeRemoved = null;
		boolean conflictingMappingFound = false;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment1().equals(minStatementMapping.getFragment1()) ||
					mapping.getFragment2().equals(minStatementMapping.getFragment2())) {
				conflictingMappingFound = true;
				int oldMappingReplacements = validReplacements(mapping, parameterToArgumentMap);
				if(newMappingReplacents < oldMappingReplacements) {
					mappingToBeRemoved = mapping;
					break;
				}
			}
		}
		if(mappingToBeRemoved != null) {
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
										mappingsWithSameReplacementTypes.add(mapping);
										break;
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

	private LeafMapping createLeafMapping(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2, Map<String, String> parameterToArgumentMap, boolean equalNumberOfAssertions) {
		VariableDeclarationContainer container1 = codeFragmentOperationMap1.containsKey(leaf1) ? codeFragmentOperationMap1.get(leaf1) : this.container1;
		VariableDeclarationContainer container2 = codeFragmentOperationMap2.containsKey(leaf2) ? codeFragmentOperationMap2.get(leaf2) : this.container2;
		LeafMapping mapping = new LeafMapping(leaf1, leaf2, container1, container2);
		mapping.setEqualNumberOfAssertions(equalNumberOfAssertions);
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
		return mapping;
	}

	private String preprocessInput1(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf1, leaf2);
	}

	private String preprocessInput2(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf2, leaf1);
	}

	private String preprocessInput(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		String argumentizedString = new String(leaf1.getArgumentizedString());
		if (leaf1 instanceof StatementObject && leaf2 instanceof AbstractExpression) {
			if (argumentizedString.startsWith("return ") && argumentizedString.endsWith(";\n")) {
				argumentizedString = argumentizedString.substring("return ".length(),
						argumentizedString.lastIndexOf(";\n"));
			}
		}
		return argumentizedString;
	}

	protected static class ReplacementInfo {
		private String argumentizedString1;
		private String argumentizedString2;
		private int rawDistance;
		private Set<Replacement> replacements;
		private List<? extends AbstractCodeFragment> statements1;
		private List<? extends AbstractCodeFragment> statements2;
		
		public ReplacementInfo(String argumentizedString1, String argumentizedString2,
				List<? extends AbstractCodeFragment> statements1, List<? extends AbstractCodeFragment> statements2) {
			this.argumentizedString1 = argumentizedString1;
			this.argumentizedString2 = argumentizedString2;
			this.statements1 = statements1;
			this.statements2 = statements2;
			this.rawDistance = StringDistance.editDistance(argumentizedString1, argumentizedString2);
			this.replacements = new LinkedHashSet<Replacement>();
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
		public List<Replacement> getReplacements(ReplacementType type) {
			List<Replacement> replacements = new ArrayList<Replacement>();
			for(Replacement replacement : this.replacements) {
				if(replacement.getType().equals(type)) {
					replacements.add(replacement);
				}
			}
			return replacements;
		}
	}

	private boolean nonMatchedStatementUsesVariableInArgument(List<? extends AbstractCodeFragment> statements, String variable, String otherArgument) {
		for(AbstractCodeFragment statement : statements) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation != null) {
				for(String argument : invocation.arguments()) {
					String argumentNoWhiteSpace = argument.replaceAll("\\s","");
					if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
							!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) && !argument.equals(otherArgument)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Set<Replacement> findReplacementsWithExactMatching(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo) throws RefactoringMinerTimedOutException {
		List<VariableDeclaration> variableDeclarations1 = new ArrayList<VariableDeclaration>(statement1.getVariableDeclarations());
		List<VariableDeclaration> variableDeclarations2 = new ArrayList<VariableDeclaration>(statement2.getVariableDeclarations());
		VariableDeclaration variableDeclarationWithArrayInitializer1 = declarationWithArrayInitializer(variableDeclarations1);
		VariableDeclaration variableDeclarationWithArrayInitializer2 = declarationWithArrayInitializer(variableDeclarations2);
		AbstractCall invocationCoveringTheEntireStatement1 = statement1.invocationCoveringEntireFragment();
		AbstractCall invocationCoveringTheEntireStatement2 = statement2.invocationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
		Map<String, List<AbstractCall>> methodInvocationMap1 = convertToMap(statement1.getMethodInvocations());
		Map<String, List<AbstractCall>> methodInvocationMap2 = convertToMap(statement2.getMethodInvocations());
		Set<String> variables1 = convertToStringSet(statement1.getVariables());
		Set<String> variables2 = convertToStringSet(statement2.getVariables());
		Set<String> variableIntersection = new LinkedHashSet<String>(variables1);
		variableIntersection.retainAll(variables2);
		// ignore the variables in the intersection that also appear with "this." prefix in the sets of variables
		// ignore the variables in the intersection that are static fields
		// ignore the variables in the intersection that one of them is a variable declaration and the other is not
		// ignore the variables in the intersection that one of them is part of a method invocation, but the same method invocation does not appear on the other side
		Set<String> variablesToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
		for(String variable : variableIntersection) {
			if(!variable.startsWith("this.") && !variableIntersection.contains("this."+variable) &&
					(variables1.contains("this."+variable) || variables2.contains("this."+variable))) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
					invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				if(!invocationCoveringTheEntireStatement1.arguments().contains(variable) &&
						invocationCoveringTheEntireStatement2.arguments().contains(variable)) {
					for(String argument : invocationCoveringTheEntireStatement1.arguments()) {
						String argumentNoWhiteSpace = argument.replaceAll("\\s","");
						if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
								!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) &&
								!nonMatchedStatementUsesVariableInArgument(replacementInfo.statements1, variable, argument)) {
							variablesToBeRemovedFromTheIntersection.add(variable);
						}
					}
				}
				else if(invocationCoveringTheEntireStatement1.arguments().contains(variable) &&
						!invocationCoveringTheEntireStatement2.arguments().contains(variable)) {
					for(String argument : invocationCoveringTheEntireStatement2.arguments()) {
						String argumentNoWhiteSpace = argument.replaceAll("\\s","");
						if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
								!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) &&
								!nonMatchedStatementUsesVariableInArgument(replacementInfo.statements2, variable, argument)) {
							variablesToBeRemovedFromTheIntersection.add(variable);
						}
					}
				}
			}
			if(variable.toUpperCase().equals(variable) && !ReplacementUtil.sameCharsBeforeAfter(statement1.getString(), statement2.getString(), variable)) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			boolean foundInDeclaration1 = false;
			boolean foundInInitializer1 = false;
			for(VariableDeclaration declaration : variableDeclarations1) {
				if(declaration.getVariableName().equals(variable)) {
					foundInDeclaration1 = true;
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && initializer.getString().endsWith("." + declaration.getVariableName())) {
						variablesToBeRemovedFromTheIntersection.add(variable);
					}
					break;
				}
				AbstractExpression initializer = declaration.getInitializer();
				if(initializer != null && initializer.getString().endsWith("." + variable)) {
					foundInInitializer1 = true;
				}
			}
			boolean foundInDeclaration2 = false;
			boolean foundInInitializer2 = false;
			for(VariableDeclaration declaration : variableDeclarations2) {
				if(declaration.getVariableName().equals(variable)) {
					foundInDeclaration2 = true;
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && initializer.getString().endsWith("." + declaration.getVariableName())) {
						variablesToBeRemovedFromTheIntersection.add(variable);
					}
					break;
				}
				AbstractExpression initializer = declaration.getInitializer();
				if(initializer != null && initializer.getString().endsWith("." + variable)) {
					foundInInitializer2 = true;
				}
			}
			if(foundInDeclaration1 != foundInDeclaration2 || foundInInitializer1 != foundInInitializer2) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			else if(!variable.contains(".")) {
				boolean foundInInvocation1 = false;
				for(String key : methodInvocationMap1.keySet()) {
					if(key.startsWith(variable + ".")) {
						foundInInvocation1 = true;
						break;
					}
				}
				boolean foundInInvocation2 = false;
				for(String key : methodInvocationMap2.keySet()) {
					if(key.startsWith(variable + ".")) {
						foundInInvocation2 = true;
						break;
					}
				}
				boolean sameCoverageInvocations = invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
						invocationCoveringTheEntireStatement1.getCoverage().equals(invocationCoveringTheEntireStatement2.getCoverage());
				boolean sameCoverageCreations = creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
						creationCoveringTheEntireStatement1.getCoverage().equals(creationCoveringTheEntireStatement2.getCoverage());
				if((sameCoverageInvocations || sameCoverageCreations) && foundInInvocation1 != foundInInvocation2) {
					variablesToBeRemovedFromTheIntersection.add(variable);
				}
			}
		}
		variableIntersection.removeAll(variablesToBeRemovedFromTheIntersection);
		// remove common variables from the two sets
		variables1.removeAll(variableIntersection);
		variables2.removeAll(variableIntersection);
		
		// replace variables with the corresponding arguments
		replaceVariablesWithArguments(variables1, parameterToArgumentMap);
		replaceVariablesWithArguments(variables2, parameterToArgumentMap);
		
		Set<String> methodInvocations1 = new LinkedHashSet<String>(methodInvocationMap1.keySet());
		Set<String> methodInvocations2 = new LinkedHashSet<String>(methodInvocationMap2.keySet());
		
		Map<String, List<AbstractCall>> creationMap1 = convertToMap(statement1.getCreations());
		Map<String, List<AbstractCall>> creationMap2 = convertToMap(statement2.getCreations());
		Set<String> creations1 = new LinkedHashSet<String>(creationMap1.keySet());
		Set<String> creations2 = new LinkedHashSet<String>(creationMap2.keySet());
		
		Set<String> arguments1 = convertToStringSet(statement1.getArguments());
		Set<String> arguments2 = convertToStringSet(statement2.getArguments());
		removeCommonElements(arguments1, arguments2);
		
		if(!argumentsWithIdenticalMethodCalls(arguments1, arguments2, variables1, variables2)) {
			boolean argsAreMethodCalls = false;
			if(arguments1.size() == arguments2.size()) {
				Iterator<String> it1 = arguments1.iterator();
				Iterator<String> it2 = arguments2.iterator();
				for(int i=0; i<arguments1.size(); i++) {
					String arg1 = it1.next();
					String arg2 = it2.next();
					if(methodInvocationMap1.containsKey(arg1) && methodInvocationMap2.containsKey(arg2)) {
						List<? extends AbstractCall> calls1 = methodInvocationMap1.get(arg1);
						List<? extends AbstractCall> calls2 = methodInvocationMap2.get(arg2);
						if(calls1.get(0).getName().equals(calls2.get(0).getName())) {
							argsAreMethodCalls = true;
							break;
						}
					}
					else {
						String matchingKey1 = null;
						for(String key1 : methodInvocationMap1.keySet()) {
							if(arg1.contains(key1)) {
								matchingKey1 = key1;
								break;
							}
						}
						String matchingKey2 = null;
						for(String key2 : methodInvocationMap2.keySet()) {
							if(arg2.contains(key2)) {
								matchingKey2 = key2;
								break;
							}
						}
						if(matchingKey1 != null && matchingKey2 != null) {
							List<? extends AbstractCall> calls1 = methodInvocationMap1.get(matchingKey1);
							List<? extends AbstractCall> calls2 = methodInvocationMap2.get(matchingKey2);
							if(calls1.get(0).getName().equals(calls2.get(0).getName())) {
								argsAreMethodCalls = true;
								break;
							}
						}
					}
				}
			}
			if(!argsAreMethodCalls) {
				findReplacements(arguments1, variables2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE);
			}
		}
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<Replacement>();
		for(Replacement r : replacementInfo.getReplacements()) {
			map.put(r.getBefore(), r.getAfter());
			if(methodInvocationMap1.containsKey(r.getBefore())) {
				Replacement replacement = new VariableReplacementWithMethodInvocation(r.getBefore(), r.getAfter(), methodInvocationMap1.get(r.getBefore()).get(0), Direction.INVOCATION_TO_VARIABLE);
				replacementsToBeAdded.add(replacement);
				replacementsToBeRemoved.add(r);
			}
		}
		replacementInfo.getReplacements().removeAll(replacementsToBeRemoved);
		replacementInfo.getReplacements().addAll(replacementsToBeAdded);
		
		// replace variables with the corresponding arguments in method invocations
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, parameterToArgumentMap);
		replaceVariablesWithArguments(methodInvocationMap2, methodInvocations2, parameterToArgumentMap);
		
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, map);
		
		//remove methodInvocation covering the entire statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String methodInvocation1 : methodInvocationMap1.keySet()) {
				for(AbstractCall call : methodInvocationMap1.get(methodInvocation1)) {
					if(invocationCoveringTheEntireStatement1.getLocationInfo().equals(call.getLocationInfo())) {
						methodInvocations1.remove(methodInvocation1);
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation2 : methodInvocationMap2.keySet()) {
				for(AbstractCall call : methodInvocationMap2.get(methodInvocation2)) {
					if(invocationCoveringTheEntireStatement2.getLocationInfo().equals(call.getLocationInfo())) {
						methodInvocations2.remove(methodInvocation2);
					}
				}
			}
		}
		Set<String> methodInvocationIntersection = new LinkedHashSet<String>(methodInvocations1);
		methodInvocationIntersection.retainAll(methodInvocations2);
		Set<String> methodInvocationsToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
		for(String methodInvocation : methodInvocationIntersection) {
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
					invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				if(!invocationCoveringTheEntireStatement1.arguments().contains(methodInvocation) &&
						invocationCoveringTheEntireStatement2.arguments().contains(methodInvocation)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
				else if(invocationCoveringTheEntireStatement1.arguments().contains(methodInvocation) &&
						!invocationCoveringTheEntireStatement2.arguments().contains(methodInvocation)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
			for(String key : methodInvocationMap1.keySet()) {
				if(key.startsWith(methodInvocation + ".") && !methodInvocationMap2.containsKey(key)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
			for(String key : methodInvocationMap2.keySet()) {
				if(key.startsWith(methodInvocation + ".") && !methodInvocationMap1.containsKey(key)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
		}
		methodInvocationIntersection.removeAll(methodInvocationsToBeRemovedFromTheIntersection);
		// remove common methodInvocations from the two sets
		methodInvocations1.removeAll(methodInvocationIntersection);
		methodInvocations2.removeAll(methodInvocationIntersection);
		
		Set<String> variablesAndMethodInvocations1 = new LinkedHashSet<String>();
		//variablesAndMethodInvocations1.addAll(methodInvocations1);
		//variablesAndMethodInvocations1.addAll(variables1);
		
		Set<String> variablesAndMethodInvocations2 = new LinkedHashSet<String>();
		variablesAndMethodInvocations2.addAll(methodInvocations2);
		variablesAndMethodInvocations2.addAll(variables2);
		
		Set<String> types1 = new LinkedHashSet<String>(statement1.getTypes());
		for(AnonymousClassDeclarationObject anonymous1 : statement1.getAnonymousClassDeclarations()) {
			types1.addAll(anonymous1.getTypes());
		}
		Set<String> types2 = new LinkedHashSet<String>(statement2.getTypes());
		for(AnonymousClassDeclarationObject anonymous2 : statement2.getAnonymousClassDeclarations()) {
			types2.addAll(anonymous2.getTypes());
		}
		removeCommonTypes(types1, types2, statement1.getTypes(), statement2.getTypes());
		
		// replace variables with the corresponding arguments in object creations
		replaceVariablesWithArguments(creationMap1, creations1, parameterToArgumentMap);
		replaceVariablesWithArguments(creationMap2, creations2, parameterToArgumentMap);
		
		replaceVariablesWithArguments(creationMap1, creations1, map);
		
		//remove objectCreation covering the entire statement
		for(String objectCreation1 : creationMap1.keySet()) {
			for(AbstractCall creation1 : creationMap1.get(objectCreation1)) {
				if(creationCoveringTheEntireStatement1 != null && 
						creationCoveringTheEntireStatement1.getLocationInfo().equals(creation1.getLocationInfo())) {
					creations1.remove(objectCreation1);
				}
				if(((ObjectCreation)creation1).getAnonymousClassDeclaration() != null) {
					creations1.remove(objectCreation1);
				}
			}
		}
		for(String objectCreation2 : creationMap2.keySet()) {
			for(AbstractCall creation2 : creationMap2.get(objectCreation2)) {
				if(creationCoveringTheEntireStatement2 != null &&
						creationCoveringTheEntireStatement2.getLocationInfo().equals(creation2.getLocationInfo())) {
					creations2.remove(objectCreation2);
				}
				if(((ObjectCreation)creation2).getAnonymousClassDeclaration() != null) {
					creations2.remove(objectCreation2);
				}
			}
		}
		Set<String> creationIntersection = new LinkedHashSet<String>(creations1);
		creationIntersection.retainAll(creations2);
		// remove common creations from the two sets
		creations1.removeAll(creationIntersection);
		creations2.removeAll(creationIntersection);
		
		Set<String> stringLiterals1 = convertToStringSet(statement1.getStringLiterals());
		Set<String> stringLiterals2 = convertToStringSet(statement2.getStringLiterals());
		removeCommonElements(stringLiterals1, stringLiterals2);
		
		Set<String> typeLiterals1 = convertToStringSet(statement1.getTypeLiterals());
		Set<String> typeLiterals2 = convertToStringSet(statement2.getTypeLiterals());
		removeCommonElements(typeLiterals1, typeLiterals2);
		
		Set<String> numberLiterals1 = convertToStringSet(statement1.getNumberLiterals());
		Set<String> numberLiterals2 = convertToStringSet(statement2.getNumberLiterals());
		removeCommonElements(numberLiterals1, numberLiterals2);
		
		Set<String> booleanLiterals1 = convertToStringSet(statement1.getBooleanLiterals());
		Set<String> booleanLiterals2 = convertToStringSet(statement2.getBooleanLiterals());
		removeCommonElements(booleanLiterals1, booleanLiterals2);
		
		Set<String> infixOperators1 = new LinkedHashSet<String>(statement1.getInfixOperators());
		Set<String> infixOperators2 = new LinkedHashSet<String>(statement2.getInfixOperators());
		removeCommonElements(infixOperators1, infixOperators2);
		
		Set<String> arrayAccesses1 = convertToStringSet(statement1.getArrayAccesses());
		Set<String> arrayAccesses2 = convertToStringSet(statement2.getArrayAccesses());
		removeCommonElements(arrayAccesses1, arrayAccesses2);
		
		Set<String> prefixExpressions1 = convertToStringSet(statement1.getPrefixExpressions());
		Set<String> prefixExpressions2 = convertToStringSet(statement2.getPrefixExpressions());
		removeCommonElements(prefixExpressions1, prefixExpressions2);
		
		Set<String> parenthesizedExpressions1 = convertToStringSet(statement1.getParenthesizedExpressions());
		Set<String> parenthesizedExpressions2 = convertToStringSet(statement2.getParenthesizedExpressions());
		removeCommonElements(parenthesizedExpressions1, parenthesizedExpressions2);
		
		//perform type replacements
		findReplacements(types1, types2, replacementInfo, ReplacementType.TYPE);
		
		if(statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType())) {
			Set<String> infixExpressions1 = convertToStringSet(statement1.getInfixExpressions());
			infixExpressions1.remove(statement1.infixExpressionCoveringTheEntireFragment());
			Set<String> infixExpressions2 = convertToStringSet(statement2.getInfixExpressions());
			infixExpressions2.remove(statement2.infixExpressionCoveringTheEntireFragment());
			removeCommonElements(infixExpressions1, infixExpressions2);
			
			if(infixExpressions1.size() != infixExpressions2.size()) {
				List<String> infixExpressions1AsList = new ArrayList<>(infixExpressions1);
				Collections.reverse(infixExpressions1AsList);
				Set<String> reverseInfixExpressions1 = new LinkedHashSet<String>(infixExpressions1AsList);
				findReplacements(reverseInfixExpressions1, variables2, replacementInfo, ReplacementType.INFIX_EXPRESSION);
			}
		}
		//perform operator replacements
		findReplacements(infixOperators1, infixOperators2, replacementInfo, ReplacementType.INFIX_OPERATOR);
		
		//apply existing replacements on method invocations
		for(String methodInvocation1 : methodInvocations1) {
			String temp = new String(methodInvocation1);
			for(Replacement replacement : replacementInfo.getReplacements()) {
				temp = ReplacementUtil.performReplacement(temp, replacement.getBefore(), replacement.getAfter());
			}
			if(!temp.equals(methodInvocation1)) {
				variablesAndMethodInvocations1.add(temp);
				methodInvocationMap1.put(temp, methodInvocationMap1.get(methodInvocation1));
			}
		}
		//add updated method invocation to the original list of invocations
		methodInvocations1.addAll(variablesAndMethodInvocations1);
		variablesAndMethodInvocations1.addAll(methodInvocations1);
		variablesAndMethodInvocations1.addAll(variables1);
		
		findReplacements(variables1, creations2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CLASS_INSTANCE_CREATION);
		if(variablesAndMethodInvocations1.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS || variablesAndMethodInvocations2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS) {
			return null;
		}
		if (replacementInfo.getRawDistance() > 0) {
			for(String s1 : variablesAndMethodInvocations1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				int minDistance = replacementInfo.getRawDistance();
				for(String s2 : variablesAndMethodInvocations2) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), minDistance);
					boolean multipleInstances = ReplacementUtil.countInstances(temp, s2) > 1;
					boolean typeContainsVariableName = false;
					if(variableDeclarations1.size() > 0 && !s1.equals(s2)) {
						VariableDeclaration variableDeclaration = variableDeclarations1.get(0);
						UMLType variableType = variableDeclaration.getType();
						if(variableType != null) {
							String typeTolowerCase = variableType.toString().toLowerCase();
							if(typeTolowerCase.contains(variableDeclaration.getVariableName().toLowerCase()) &&	typeTolowerCase.contains(s2.toLowerCase())) {
								typeContainsVariableName = true;
							}
							if(!typeContainsVariableName && statement1.getString().contains(s1 + "=") && statement2.getString().contains(s2 + "=")) {
								String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variableType.toString());
								String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(s2);
								int commonTokens = 0;
								for(String token1 : tokens1) {
									for(String token2 : tokens2) {
										if(token1.toLowerCase().equals(token2.toLowerCase()) || 
												token1.toLowerCase().startsWith(token2.toLowerCase()) ||
												token2.toLowerCase().startsWith(token1.toLowerCase())) {
											commonTokens++;
										}
									}
								}
								if(commonTokens > 1) {
									typeContainsVariableName = true;
								}
							}
						}
					}
					if(distanceRaw == -1 && (multipleInstances || typeContainsVariableName)) {
						distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					}
					boolean allowReplacementIncreasingDistance = (multipleInstances && Math.abs(s1.length() - s2.length()) == Math.abs(distanceRaw - minDistance) && !s1.equals(s2)) || typeContainsVariableName;
					if(distanceRaw >= 0 && (distanceRaw < replacementInfo.getRawDistance() || allowReplacementIncreasingDistance)) {
						minDistance = distanceRaw;
						Replacement replacement = null;
						if(variables1.contains(s1) && variables2.contains(s2) && variablesStartWithSameCase(s1, s2, parameterToArgumentMap, replacementInfo)) {
							replacement = new Replacement(s1, s2, ReplacementType.VARIABLE_NAME);
							if(s1.startsWith("(") && s2.startsWith("(") && s1.contains(")") && s2.contains(")")) {
								String prefix1 = s1.substring(0, s1.indexOf(")")+1);
								String prefix2 = s2.substring(0, s2.indexOf(")")+1);
								if(prefix1.equals(prefix2)) {
									String suffix1 = s1.substring(prefix1.length(), s1.length());
									String suffix2 = s2.substring(prefix2.length(), s2.length());
									replacement = new Replacement(suffix1, suffix2, ReplacementType.VARIABLE_NAME);
								}
							}
							VariableDeclaration v1 = statement1.searchVariableDeclaration(s1);
							if(v1 == null && container1 != null) {
								for(VariableDeclaration declaration : container1.getParameterDeclarationList()) {
									if(declaration.getVariableName().equals(s1)) {
										v1 = declaration;
										break;
									}
								}
							}
							VariableDeclaration v2 = statement2.searchVariableDeclaration(s2);
							if(v2 == null && container2 != null) {
								for(VariableDeclaration declaration : container2.getParameterDeclarationList()) {
									if(declaration.getVariableName().equals(s2)) {
										v2 = declaration;
										break;
									}
								}
							}
							if((inconsistentVariableMappingCount(statement1, statement2, v1, v2) > 1 || mappingsForStatementsInScope(statement1, statement2, v1, v2) == 0) &&
									!existsVariableDeclarationForV2InitializedWithV1(v1, v2, replacementInfo) && !existsVariableDeclarationForV1InitializedWithV2(v1, v2, replacementInfo) &&
									container2 != null && container2.loopWithVariables(v1.getVariableName(), v2.getVariableName()) == null) {
								replacement = null;
							}
						}
						else if(variables1.contains(s1) && methodInvocations2.contains(s2)) {
							AbstractCall invokedOperationAfter = methodInvocationMap2.get(s2).get(0);
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationAfter, Direction.VARIABLE_TO_INVOCATION);
						}
						else if(methodInvocations1.contains(s1) && methodInvocations2.contains(s2)) {
							AbstractCall invokedOperationBefore = methodInvocationMap1.get(s1).get(0);
							AbstractCall invokedOperationAfter = methodInvocationMap2.get(s2).get(0);
							if(invokedOperationBefore.compatibleExpression(invokedOperationAfter)) {
								if(invokedOperationBefore.identicalExpression(invokedOperationAfter) && invokedOperationBefore.equalArguments(invokedOperationAfter)) {
									replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION_NAME);
								}
								else {
									replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION);
								}
							}
						}
						else if(methodInvocations1.contains(s1) && variables2.contains(s2)) {
							AbstractCall invokedOperationBefore = methodInvocationMap1.get(s1).get(0);
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationBefore, Direction.INVOCATION_TO_VARIABLE);
						}
						if(replacement != null) {
							double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
							replacementMap.put(distancenormalized, replacement);
						}
						if(distanceRaw == 0 && !replacementInfo.getReplacements().isEmpty()) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
							invocationCoveringTheEntireStatement1.methodNameContainsArgumentName() &&
							invocationCoveringTheEntireStatement2.methodNameContainsArgumentName() &&
							replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
						for(Replacement r : replacementMap.values()) {
							if(!replacement.equals(r) && r.getType().equals(ReplacementType.VARIABLE_NAME) &&
									invocationCoveringTheEntireStatement1.getName().toLowerCase().endsWith(r.getBefore().toLowerCase()) &&
									invocationCoveringTheEntireStatement2.getName().toLowerCase().endsWith(r.getAfter().toLowerCase())) {
								replacement = r;
								break;
							}
						}
					}
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		
		if(replacementInfo.getReplacements().size() > 0 && replacementInfo.getReplacements(ReplacementType.VARIABLE_NAME).size() == 0) {
			boolean atLeastOneUpperCaseVariable1 = false;
			for(String variable1 : variables1) {
				if(Character.isUpperCase(variable1.charAt(0))) {
					boolean foundInReplacement = false;
					for(Replacement r : replacementInfo.getReplacements()) {
						if(r.getBefore().contains(variable1) && !r.getBefore().equals(variable1)) {
							foundInReplacement = true;
						}
					}
					if(!foundInReplacement) {
						atLeastOneUpperCaseVariable1 = true;
					}
				}
			}
			boolean atLeastOneUpperCaseVariable2 = false;
			for(String variable2 : variables2) {
				if(Character.isUpperCase(variable2.charAt(0))) {
					boolean foundInReplacement = false;
					for(Replacement r : replacementInfo.getReplacements()) {
						if(r.getAfter().contains(variable2) && !r.getAfter().equals(variable2)) {
							foundInReplacement = true;
						}
					}
					if(!foundInReplacement) {
						atLeastOneUpperCaseVariable2 = true;
					}
				}
			}
			if(atLeastOneUpperCaseVariable1 != atLeastOneUpperCaseVariable2) {
				findReplacements(variables1, variables2, replacementInfo, ReplacementType.VARIABLE_NAME);
			}
		}
		//perform creation replacements
		findReplacements(creations1, creations2, replacementInfo, ReplacementType.CLASS_INSTANCE_CREATION);
		
		findReplacements(parenthesizedExpressions1, parenthesizedExpressions2, replacementInfo, ReplacementType.PARENTHESIZED_EXPRESSION);
		
		//perform literal replacements
		findReplacements(stringLiterals1, stringLiterals2, replacementInfo, ReplacementType.STRING_LITERAL);
		findReplacements(numberLiterals1, numberLiterals2, replacementInfo, ReplacementType.NUMBER_LITERAL);
		if(!statement1.containsInitializerOfVariableDeclaration(numberLiterals1) && !statement2.containsInitializerOfVariableDeclaration(variables2) &&
				(!statement1.getString().endsWith("=0;\n") || (statement1.getString().endsWith("=0;\n") && statement2.getString().endsWith(".length;\n")))) {
			findReplacements(numberLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NUMBER_LITERAL);
		}
		findReplacements(variables1, arrayAccesses2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS);
		findReplacements(arrayAccesses1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS);
		
		findReplacements(methodInvocations1, arrayAccesses2, replacementInfo, ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION);
		findReplacements(arrayAccesses1, methodInvocations2, replacementInfo, ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION);
		
		findReplacements(variables1, prefixExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION);
		findReplacements(prefixExpressions1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION);
		if(prefixExpressions1.size() == 1 && prefixExpressions1.iterator().next().startsWith("!") && booleanLiterals1.isEmpty()) {
			findReplacements(prefixExpressions1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_PREFIX_EXPRESSION);
		}
		if(prefixExpressions2.size() == 1 && prefixExpressions2.iterator().next().startsWith("!") && booleanLiterals2.isEmpty()) {
			findReplacements(booleanLiterals1, prefixExpressions2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_PREFIX_EXPRESSION);
		}
		if(statement2.getThisExpressions().size() > 0 && !statement2.getString().equals("return this;\n")) {
			findReplacements(variables1, Set.of("this"), replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_THIS_EXPRESSION);
		}
		if(statement1.getThisExpressions().size() > 0 && !statement1.getString().equals("return this;\n")) {
			findReplacements(Set.of("this"), variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_THIS_EXPRESSION);
		}
		findReplacements(stringLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL);
		findReplacements(parenthesizedExpressions1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PARENTHESIZED_EXPRESSION);
		findReplacements(variables1, parenthesizedExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PARENTHESIZED_EXPRESSION);
		findReplacements(methodInvocations1, stringLiterals2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_STRING_LITERAL);
		if((statement1.getNullLiterals().isEmpty() && !statement2.getNullLiterals().isEmpty()) ||
				bothContainNullInDifferentIndexes(invocationCoveringTheEntireStatement1 != null ? invocationCoveringTheEntireStatement1 : creationCoveringTheEntireStatement1,
						invocationCoveringTheEntireStatement2 != null ? invocationCoveringTheEntireStatement2 : creationCoveringTheEntireStatement2)) {
			Set<String> nullLiterals2 = Set.of("null");
			for(String parameter : parameterToArgumentMap.keySet()) { 
				String argument = parameterToArgumentMap.get(parameter); 
				if(!parameter.equals(argument) && variables1.contains(parameter)) {
					variables1.add(argument);
				}
			}
			findReplacements(variables1, nullLiterals2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL);
			if(invocationCoveringTheEntireStatement1 != null) {
				String expression = invocationCoveringTheEntireStatement1.getExpression();
				if(expression != null && expression.equals("Optional") && invocationCoveringTheEntireStatement1.getName().equals("empty") &&
						invocationCoveringTheEntireStatement1.arguments().size() == 0) {
					Set<String> invocations1 = new LinkedHashSet<String>();
					invocations1.add(invocationCoveringTheEntireStatement1.actualString());
					findReplacements(invocations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
				}
			}
			if(methodInvocations1.contains("Optional.empty()")) {
				findReplacements(methodInvocations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
			}
			if(!creations1.isEmpty()) {
				findReplacements(creations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CREATION);
			}
			if(!stringLiterals1.isEmpty()) {
				findReplacements(stringLiterals1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_STRING_LITERAL);
			}
			if(!typeLiterals1.isEmpty()) {
				findReplacements(typeLiterals1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_TYPE_LITERAL);
			}
		}
		if((!statement1.getNullLiterals().isEmpty() && statement2.getNullLiterals().isEmpty()) ||
				bothContainNullInDifferentIndexes(invocationCoveringTheEntireStatement1 != null ? invocationCoveringTheEntireStatement1 : creationCoveringTheEntireStatement1,
						invocationCoveringTheEntireStatement2 != null ? invocationCoveringTheEntireStatement2 : creationCoveringTheEntireStatement2)) {
			Set<String> nullLiterals1 = Set.of("null");
			for(String parameter : parameterToArgumentMap.keySet()) { 
				String argument = parameterToArgumentMap.get(parameter); 
				if(!parameter.equals(argument) && variables2.contains(parameter)) {
					variables2.add(argument);
				}
			}
			findReplacements(nullLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL);
			if(invocationCoveringTheEntireStatement2 != null) {
				String expression = invocationCoveringTheEntireStatement2.getExpression();
				if(expression != null && expression.equals("Optional") && invocationCoveringTheEntireStatement2.getName().equals("empty") &&
						invocationCoveringTheEntireStatement2.arguments().size() == 0) {
					Set<String> invocations2 = new LinkedHashSet<String>();
					invocations2.add(invocationCoveringTheEntireStatement2.actualString());
					findReplacements(nullLiterals1, invocations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
				}
			}
			if(methodInvocations2.contains("Optional.empty()")) {
				findReplacements(nullLiterals1, methodInvocations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
			}
			if(!creations2.isEmpty()) {
				findReplacements(nullLiterals1, creations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CREATION);
			}
			if(!stringLiterals2.isEmpty()) {
				findReplacements(nullLiterals1, stringLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_STRING_LITERAL);
			}
			if(!typeLiterals2.isEmpty()) {
				findReplacements(nullLiterals1, typeLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_TYPE_LITERAL);
			}
			if(parentMapper == null && variableDeclarations1.size() > 0 && variableDeclarations2.size() > 0 &&
					variableDeclarations1.get(0).getType() != null && variableDeclarations2.get(0).getType() != null &&
					variableDeclarations1.get(0).getType().equals(variableDeclarations2.get(0).getType()) &&
					statement1.getString().endsWith("=null;\n") && invocationCoveringTheEntireStatement2 != null) {
				findReplacements(nullLiterals1, Set.of(invocationCoveringTheEntireStatement2.actualString()), replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_NULL_LITERAL);
			}
		}

		if(statement1.getTernaryOperatorExpressions().isEmpty() && !statement2.getTernaryOperatorExpressions().isEmpty()) {
			if(!statement1.getNullLiterals().isEmpty()) {
				Set<String> nullLiterals1 = new LinkedHashSet<String>();
				nullLiterals1.add("null");
				Set<String> ternaryExpressions2 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
					ternaryExpressions2.add(ternary.getExpression());	
				}
				findReplacements(nullLiterals1, ternaryExpressions2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION);
			}
			if(methodInvocations1.size() > methodInvocations2.size() && !containsMethodSignatureOfAnonymousClass(statement1.getString())) {
				Set<String> ternaryExpressions2 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
					ternaryExpressions2.add(ternary.getExpression());	
				}
				findReplacements(methodInvocations1, ternaryExpressions2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CONDITIONAL_EXPRESSION);
			}
			Set<String> ternaryExpressions2 = new LinkedHashSet<String>();
			Set<String> tmpVariables1 = new LinkedHashSet<String>();
			for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
				List<LeafExpression> thenVariables = ternary.getThenExpression().getVariables();
				List<LeafExpression> elseVariables = ternary.getElseExpression().getVariables();
				if((thenVariables.size() > 0 && ternary.getThenExpression().getExpression().equals(thenVariables.get(0).getString())) ||
						(elseVariables.size() > 0 && ternary.getElseExpression().getExpression().equals(elseVariables.get(0).getString()))) {
					ternaryExpressions2.add(ternary.getExpression());
					tmpVariables1.addAll(convertToStringSet(ternary.getCondition().getVariables()));
					tmpVariables1.addAll(convertToStringSet(ternary.getThenExpression().getVariables()));
					tmpVariables1.addAll(convertToStringSet(ternary.getElseExpression().getVariables()));
				}
			}
			findReplacements(tmpVariables1, ternaryExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CONDITIONAL_EXPRESSION);
		}
		else if(!statement1.getTernaryOperatorExpressions().isEmpty() && statement2.getTernaryOperatorExpressions().isEmpty()) {
			if(!statement2.getNullLiterals().isEmpty()) {
				Set<String> nullLiterals2 = new LinkedHashSet<String>();
				nullLiterals2.add("null");
				Set<String> ternaryExpressions1 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
					ternaryExpressions1.add(ternary.getExpression());	
				}
				findReplacements(ternaryExpressions1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION);
			}
			if(methodInvocations2.size() > methodInvocations1.size() && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
				Set<String> ternaryExpressions1 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
					ternaryExpressions1.add(ternary.getExpression());	
				}
				findReplacements(ternaryExpressions1, methodInvocations2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CONDITIONAL_EXPRESSION);
			}
			Set<String> ternaryExpressions1 = new LinkedHashSet<String>();
			Set<String> tmpVariables2 = new LinkedHashSet<String>();
			for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
				List<LeafExpression> thenVariables = ternary.getThenExpression().getVariables();
				List<LeafExpression> elseVariables = ternary.getElseExpression().getVariables();
				if((thenVariables.size() > 0 && ternary.getThenExpression().getExpression().equals(thenVariables.get(0).getString())) ||
						(elseVariables.size() > 0 && ternary.getElseExpression().getExpression().equals(elseVariables.get(0).getString()))) {
					ternaryExpressions1.add(ternary.getExpression());
					tmpVariables2.addAll(convertToStringSet(ternary.getCondition().getVariables()));
					tmpVariables2.addAll(convertToStringSet(thenVariables));
					tmpVariables2.addAll(convertToStringSet(elseVariables));
				}
			}
			findReplacements(ternaryExpressions1, tmpVariables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CONDITIONAL_EXPRESSION);
		}
		if(!statement1.getString().endsWith("=true;\n") && !statement1.getString().endsWith("=false;\n")) {
			findReplacements(booleanLiterals1, arguments2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT);
			findReplacements(booleanLiterals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
		}
		if(!statement2.getString().endsWith("=true;\n") && !statement2.getString().endsWith("=false;\n")) {
			if(!statement1.getBooleanLiterals().equals(statement2.getBooleanLiterals())) {
				Set<String> literals1 = convertToStringSet(statement1.getBooleanLiterals());
				Set<String> literals2 = convertToStringSet(statement2.getBooleanLiterals());
				if(literals1.equals(literals2) ||
						matchingArgument(arguments1, literals2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
						matchingArgument(arguments1, literals2, creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2)) {
					findReplacements(arguments1, literals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT);
				}
				if(literals1.equals(literals2) ||
						matchingArgument(variables1, literals2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
						matchingArgument(variables1, literals2, creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2)) {
					findReplacements(variables1, literals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
				}
			}
		}
		if((statement2.getString().endsWith("true;\n") && statement1.getString().endsWith("Boolean.TRUE;\n")) ||
				(statement2.getString().endsWith("false;\n") && statement1.getString().endsWith("Boolean.FALSE;\n"))) {
			findReplacements(variables1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
		}
		if((statement1.getString().endsWith("true;\n") && statement2.getString().endsWith("Boolean.TRUE;\n")) ||
				(statement1.getString().endsWith("false;\n") && statement2.getString().endsWith("Boolean.FALSE;\n"))) {
			findReplacements(booleanLiterals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
		}
		if((statement1.getString().endsWith("true;\n") && statement2.getString().endsWith("false;\n")) ||
				(statement1.getString().endsWith("false;\n") && statement2.getString().endsWith("true;\n"))) {
			findReplacements(booleanLiterals1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_LITERAL);
		}
		if(statement1.getString().contains(" != null") || statement1.getString().contains(" == null")) {
			for(String key : methodInvocationMap2.keySet()) {
				List<? extends AbstractCall> calls2 = methodInvocationMap2.get(key);
				for(AbstractCall call : calls2) {
					if(call.getName().equals("isPresent") && call.getExpression() != null) {
						String checkIfNull = call.getExpression() + " == null";
						String checkIfNotNull = call.getExpression() + " != null";
						if(statement1.getString().contains(checkIfNotNull)) {
							Set<String> set1 = Set.of(checkIfNotNull);
							Set<String> set2 = Set.of(call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_PRESENT_CHECK);
						}
						else if(statement1.getString().contains(checkIfNull)) {
							Set<String> set1 = Set.of(checkIfNull);
							Set<String> set2 = Set.of("!" + call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_PRESENT_CHECK);
						}
					}
					else if(call.getName().equals("isEmpty") && call.getExpression() != null) {
						String checkIfNull = call.getExpression() + " == null";
						String checkIfNotNull = call.getExpression() + " != null";
						if(statement1.getString().contains(checkIfNotNull)) {
							Set<String> set1 = Set.of(checkIfNotNull);
							Set<String> set2 = Set.of("!" + call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_EMPTY_CHECK);
						}
						else if(statement1.getString().contains(checkIfNull)) {
							Set<String> set1 = Set.of(checkIfNull);
							Set<String> set2 = Set.of(call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_EMPTY_CHECK);
						}
					}
				}
			}
		}
		if(!argumentsWithIdenticalMethodCalls(arguments1, arguments2, methodInvocations1, methodInvocations2)) {
			findReplacements(arguments1, methodInvocations2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION);
			findReplacements(methodInvocations1, arguments2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION);
			boolean anonymousArgument1 = false;
			if(arguments1.size() == 1 && containsMethodSignatureOfAnonymousClass(arguments1.iterator().next())) {
				anonymousArgument1 = true;
			}
			boolean anonymousArgument2 = false;
			if(arguments2.size() == 1 && containsMethodSignatureOfAnonymousClass(arguments2.iterator().next())) {
				anonymousArgument2 = true;
			}
			if(!anonymousArgument1 && !anonymousArgument2) {
				findReplacements(arguments1, arguments2, replacementInfo, ReplacementType.ARGUMENT);
			}
		}
		if(parentMapper != null && statement1.getParent() != null && statement2.getParent() != null &&
				statement1.getParent().getLocationInfo().getCodeElementType().equals(statement2.getParent().getLocationInfo().getCodeElementType())) {
			if(statement1.getString().equals("return;\n") && statement2.getString().equals("return null;\n")) {
				return replacementInfo.getReplacements();
			}
			else if(statement1.getString().equals("return null;\n") && statement2.getString().equals("return;\n")) {
				return replacementInfo.getReplacements();
			}
			if(statement1.getString().equals("return false;\n") && statement2.getString().equals("return null;\n")) {
				return replacementInfo.getReplacements();
			}
			else if(statement1.getString().equals("return null;\n") && statement2.getString().equals("return false;\n")) {
				return replacementInfo.getReplacements();
			}
		}
		else if(parentMapper == null && statement1.getParent() != null && statement2.getParent() != null &&
				statement1.getParent().getLocationInfo().getCodeElementType().equals(statement2.getParent().getLocationInfo().getCodeElementType())) {
			if(container1 instanceof UMLOperation && container2 instanceof UMLOperation) {
				UMLParameter returnParameter1 = ((UMLOperation)container1).getReturnParameter();
				UMLParameter returnParameter2 = ((UMLOperation)container2).getReturnParameter();
				if(returnParameter1 != null && returnParameter2 != null) {
					UMLType returnType1 = returnParameter1.getType();
					UMLType returnType2 = returnParameter2.getType();
					if(returnType1.getClassType().equals("void") && returnType2.getClassType().equals("boolean")) {
						if(statement1.getString().equals("return;\n") && statement2.getString().equals("return false;\n")) {
							return replacementInfo.getReplacements();
						}
						if(statement1.getString().equals("return;\n") && statement2.getString().equals("return true;\n")) {
							return replacementInfo.getReplacements();
						}
					}
					else if(returnType1.getClassType().equals("boolean") && returnType2.getClassType().equals("void")) {
						if(statement2.getString().equals("return;\n") && statement1.getString().equals("return false;\n")) {
							return replacementInfo.getReplacements();
						}
						if(statement2.getString().equals("return;\n") && statement1.getString().equals("return true;\n")) {
							return replacementInfo.getReplacements();
						}
					}
					//match break with already matched return
					if(statement1.getString().equals("break;\n")) {
						Set<AbstractCodeMapping> mappingsToBeAdded = new LinkedHashSet<>();
						for(AbstractCodeMapping mapping : this.mappings) {
							AbstractCodeFragment fragment2 = mapping.getFragment2();
							if(fragment2.getParent() != null && fragment2.getString().startsWith("return ")) {
								CompositeStatementObject parent1 = statement1.getParent();
								CompositeStatementObject parent2 = fragment2.getParent();
								String signature1 = parent1.getSignature();
								String signature2 = parent2.getSignature();
								if(signature1.equals(signature2)) {
									LeafMapping leafMapping = createLeafMapping(statement1, fragment2, parameterToArgumentMap, false);
									mappingsToBeAdded.add(leafMapping);
									break;
								}
							}
						}
						for(AbstractCodeMapping mapping : mappingsToBeAdded) {
							addMapping(mapping);
						}
					}
				}
			}
		}
		
		String s1 = preprocessInput1(statement1, statement2);
		String s2 = preprocessInput2(statement1, statement2);
		replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		replacementsToBeAdded = new LinkedHashSet<Replacement>();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			s1 = ReplacementUtil.performReplacement(s1, s2, replacement.getBefore(), replacement.getAfter());
			//find variable replacements within method invocation replacements, the boolean value indicates if the remaining part of the original replacement is identical or not
			Map<Replacement, Boolean> nestedReplacementMap = replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, methodInvocations2, methodInvocationMap2, Direction.VARIABLE_TO_INVOCATION);
			nestedReplacementMap.putAll(replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), methodInvocations1, variables2, methodInvocationMap1, Direction.INVOCATION_TO_VARIABLE));
			if(!nestedReplacementMap.isEmpty()) {
				if(!nestedReplacementMap.values().contains(false)) {
					replacementsToBeRemoved.add(replacement);
				}
				replacementsToBeAdded.addAll(nestedReplacementMap.keySet());
			}
			boolean methodInvocationReplacementWithDifferentNumberOfArguments = false;
			if(replacement instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
				AbstractCall invokedOperationBefore = methodInvocationReplacement.getInvokedOperationBefore();
				AbstractCall invokedOperationAfter = methodInvocationReplacement.getInvokedOperationAfter();
				if(invokedOperationBefore.arguments().size() != invokedOperationAfter.arguments().size()) {
					methodInvocationReplacementWithDifferentNumberOfArguments = true;
				}
			}
			if(!methodInvocationReplacementWithDifferentNumberOfArguments) {
				Set<Replacement> r = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, variables2);
				if(!r.isEmpty()) {
					replacementsToBeRemoved.add(replacement);
					replacementsToBeAdded.addAll(r);
				}
				Set<Replacement> r2 = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), stringLiterals1, variables2);
				if(!r2.isEmpty()) {
					replacementsToBeRemoved.add(replacement);
					replacementsToBeAdded.addAll(r2);
				}
			}
		}
		replacementInfo.removeReplacements(replacementsToBeRemoved);
		replacementInfo.addReplacements(replacementsToBeAdded);
		boolean isEqualWithReplacement = s1.equals(s2) || (s1 + ";\n").equals(s2) || (s2 + ";\n").equals(s1) || replacementInfo.argumentizedString1.equals(replacementInfo.argumentizedString2) || equalAfterParenthesisElimination(s1, s2) ||
				differOnlyInCastExpressionOrPrefixOperatorOrInfixOperand(s1, s2, methodInvocationMap1, methodInvocationMap2, statement1.getInfixExpressions(), statement2.getInfixExpressions(), variableDeclarations1, variableDeclarations2, replacementInfo, this) ||
				differOnlyInFinalModifier(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) || differOnlyInThis(s1, s2) || differOnlyInThrow(s1, s2) || matchAsLambdaExpressionArgument(s1, s2, parameterToArgumentMap, replacementInfo, statement1) || differOnlyInDefaultInitializer(s1, s2, variableDeclarations1, variableDeclarations2) ||
				oneIsVariableDeclarationTheOtherIsVariableAssignment(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) || identicalVariableDeclarationsWithDifferentNames(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) ||
				oneIsVariableDeclarationTheOtherIsReturnStatement(s1, s2, variableDeclarations1, variableDeclarations2) || oneIsVariableDeclarationTheOtherIsReturnStatement(statement1.getString(), statement2.getString(), variableDeclarations1, variableDeclarations2) ||
				(containsValidOperatorReplacements(replacementInfo) && (equalAfterInfixExpressionExpansion(s1, s2, replacementInfo, statement1.getInfixExpressions()) || commonConditional(s1, s2, parameterToArgumentMap, replacementInfo, statement1, statement2, this))) ||
				equalAfterArgumentMerge(s1, s2, replacementInfo) ||
				equalAfterNewArgumentAdditions(s1, s2, replacementInfo, container1, container2, operationSignatureDiff, classDiff) ||
				(validStatementForConcatComparison(statement1, statement2) && commonConcat(s1, s2, parameterToArgumentMap, replacementInfo, statement1, statement2, this));
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = statement1.getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = statement2.getAnonymousClassDeclarations();
		List<LambdaExpressionObject> lambdas1 = statement1.getLambdas();
		List<LambdaExpressionObject> lambdas2 = statement2.getLambdas();
		List<UMLOperationBodyMapper> lambdaMappers = new ArrayList<UMLOperationBodyMapper>();
		AbstractCall assignmentInvocationCoveringTheEntireStatement1 = invocationCoveringTheEntireStatement1 == null ? statement1.assignmentInvocationCoveringEntireStatement() : invocationCoveringTheEntireStatement1;
		AbstractCall assignmentInvocationCoveringTheEntireStatement2 = invocationCoveringTheEntireStatement2 == null ? statement2.assignmentInvocationCoveringEntireStatement() : invocationCoveringTheEntireStatement2;
		AbstractCall assignmentCreationCoveringTheEntireStatement1 = creationCoveringTheEntireStatement1 == null ? statement1.assignmentCreationCoveringEntireStatement() : creationCoveringTheEntireStatement1;
		AbstractCall assignmentCreationCoveringTheEntireStatement2 = creationCoveringTheEntireStatement2 == null ? statement2.assignmentCreationCoveringEntireStatement() : creationCoveringTheEntireStatement2;
		if(isEqualWithReplacement) {
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
				List<Replacement> typeReplacements = replacementInfo.getReplacements(ReplacementType.TYPE);
				for(Replacement typeReplacement : typeReplacements) {
					if(invocationCoveringTheEntireStatement1.getName().contains(typeReplacement.getBefore()) && invocationCoveringTheEntireStatement2.getName().contains(typeReplacement.getAfter())) {
						if(invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.equalArguments(invocationCoveringTheEntireStatement2)) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
									invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
							replacementInfo.addReplacement(replacement);
						}
						else {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
						}
						break;
					}
				}
				if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) &&
						invocationCoveringTheEntireStatement1.staticInvokerExpressionReplaced(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) &&
						invocationCoveringTheEntireStatement1.allArgumentsReplaced(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
				}
				for(Replacement r : replacementInfo.getReplacements()) {
					if(r instanceof VariableReplacementWithMethodInvocation) {
						VariableReplacementWithMethodInvocation replacement = (VariableReplacementWithMethodInvocation)r;
						AbstractCall call = replacement.getInvokedOperation();
						if(call.getName().equals("of") || call.getName().equals("asList")) {
							if(replacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION)) {
								for(String argument2 : call.arguments()) {
									for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
										AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
										if(invocation1 != null && invocation1.getExpression() != null && invocation1.getExpression().equals(replacement.getBefore())) {
											boolean argumentMatched = false;
											for(String argument1 : invocation1.arguments()) {
												if(argument1.equals(argument2)) {
													List<LeafExpression> leafExpressions1 = fragment1.findExpression(argument1);
													List<LeafExpression> leafExpressions2 = statement2.findExpression(argument2);
													if(leafExpressions1.size() == 1 && leafExpressions2.size() == 1) {
														LeafMapping mapping = createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, isEqualWithReplacement);
														addMapping(mapping);
													}
													argumentMatched = true;
													break;
												}
											}
											if(argumentMatched) {
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
			if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
					!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
					!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
					!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				return null;
			}
			if(variableAssignmentWithEverythingReplaced(statement1, statement2, replacementInfo, this)) {
				return null;
			}
			if(classInstanceCreationWithEverythingReplaced(statement1, statement2, replacementInfo, parameterToArgumentMap)) {
				return null;
			}
			if(operatorExpressionWithEverythingReplaced(statement1, statement2, replacementInfo, parameterToArgumentMap)) {
				return null;
			}
			if(thisConstructorCallWithEverythingReplaced(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, replacementInfo)) {
				return null;
			}
			if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty()) {
				Set<Replacement> replacementsInsideAnonymous = new LinkedHashSet<Replacement>();
				for(Replacement replacement : replacementInfo.getReplacements()) {
					if(replacement instanceof MethodInvocationReplacement) {
						MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
						for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
							for(int j=0; j<anonymousClassDeclarations2.size(); j++) {
								AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
								AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(j);
								if(anonymousClassDeclaration1.getMethodInvocations().contains(methodInvocationReplacement.getInvokedOperationBefore()) &&
										anonymousClassDeclaration2.getMethodInvocations().contains(methodInvocationReplacement.getInvokedOperationAfter())) {
									replacementsInsideAnonymous.add(replacement);
									break;
								}
							}
							if(replacementsInsideAnonymous.contains(replacement)) {
								break;
							}
						}
					}
				}
				for(Replacement replacement : replacementsInsideAnonymous) {
					equalAfterNewArgumentAdditions(replacement.getBefore(), replacement.getAfter(), replacementInfo, container1, container2, operationSignatureDiff, classDiff);
				}
			}
			processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
					assignmentInvocationCoveringTheEntireStatement1 != null ? assignmentInvocationCoveringTheEntireStatement1 : assignmentCreationCoveringTheEntireStatement1,
					assignmentInvocationCoveringTheEntireStatement2 != null ? assignmentInvocationCoveringTheEntireStatement2 : assignmentCreationCoveringTheEntireStatement2,
					methodInvocationMap1, methodInvocationMap2,	anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, lambdaMappers);
			return replacementInfo.getReplacements();
		}
		Set<Replacement> replacements = processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
				assignmentInvocationCoveringTheEntireStatement1 != null ? assignmentInvocationCoveringTheEntireStatement1 : assignmentCreationCoveringTheEntireStatement1,
				assignmentInvocationCoveringTheEntireStatement2 != null ? assignmentInvocationCoveringTheEntireStatement2 : assignmentCreationCoveringTheEntireStatement2,
				methodInvocationMap1, methodInvocationMap2,	anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, lambdaMappers);
		if(replacements != null) {
			return replacements;
		}
		//match if with switch
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
			CompositeStatementObject if1 = (CompositeStatementObject)statement1;
			CompositeStatementObject switch2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = if1.getExpressions();
			List<AbstractExpression> expressions2 = switch2.getExpressions();
			if(expressions1.size() == 1 && expressions2.size() == 1 && expressions1.get(0).getString().equals(expressions2.get(0).getString())) {
				return replacementInfo.getReplacements();
			}
		}
		//match switch with if
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject switch1 = (CompositeStatementObject)statement1;
			CompositeStatementObject if2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = switch1.getExpressions();
			List<AbstractExpression> expressions2 = if2.getExpressions();
			if(expressions1.size() == 1 && expressions2.size() == 1 && expressions1.get(0).getString().equals(expressions2.get(0).getString())) {
				return replacementInfo.getReplacements();
			}
		}
		//match traditional for with enhanced for
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for1 = (CompositeStatementObject)statement1;
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions2 = for2.getExpressions();
			AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
			VariableDeclaration inlinedVariableDeclaration = null;
			for(AbstractCodeFragment fragment1 : replacementInfo.statements1) {
				for(VariableDeclaration variableDeclaration : fragment1.getVariableDeclarations()) {
					if(variableDeclaration.getInitializer() != null && variableDeclaration.getInitializer().getString().equals(enhancedForExpression.getString())) {
						inlinedVariableDeclaration = variableDeclaration;
						break;
					}
				}
			}
			//search for previous variable declaration mappings having these for loops in their scope
			String renamedVariable = null;
			for(AbstractCodeMapping previousMapping : this.mappings) {
				if(previousMapping.getFragment1().getVariableDeclarations().size() > 0 && previousMapping.getFragment2().getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration1 = previousMapping.getFragment1().getVariableDeclarations().get(0);
					VariableDeclaration declaration2 = previousMapping.getFragment2().getVariableDeclarations().get(0);
					if(declaration1.getScope().subsumes(for1.getLocationInfo()) && declaration2.getScope().subsumes(for2.getLocationInfo())) {
						if(declaration2.getVariableName().equals(enhancedForExpression.getString())) {
							renamedVariable = declaration1.getVariableName();
							break;
						}
					}
				}
			}
			for(AbstractExpression expression1 : for1.getExpressions()) {
				if(expression1.getString().contains(enhancedForExpression.getString() + ".length") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".size()") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".iterator()") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".listIterator()")) {
					return replacementInfo.getReplacements();
				}
				if(renamedVariable != null) {
					if(expression1.getString().contains(renamedVariable + ".length") ||
							expression1.getString().contains(renamedVariable + ".size()") ||
							expression1.getString().contains(renamedVariable + ".iterator()") ||
							expression1.getString().contains(renamedVariable + ".listIterator()")) {
						return replacementInfo.getReplacements();
					}
				}
				if(inlinedVariableDeclaration != null &&
						(expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".length") ||
						expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".size()") ||
						expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".iterator()") ||
						expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".listIterator()"))) {
					return replacementInfo.getReplacements();
				}
			}
		}
		if(statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for1 = (CompositeStatementObject)statement1;
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = for1.getExpressions();
			AbstractExpression enhancedForExpression = expressions1.get(expressions1.size()-1);
			//search for previous variable declaration mappings having these for loops in their scope
			String renamedVariable = null;
			for(AbstractCodeMapping previousMapping : this.mappings) {
				if(previousMapping.getFragment1().getVariableDeclarations().size() > 0 && previousMapping.getFragment2().getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration1 = previousMapping.getFragment1().getVariableDeclarations().get(0);
					VariableDeclaration declaration2 = previousMapping.getFragment2().getVariableDeclarations().get(0);
					if(declaration1.getScope().subsumes(for1.getLocationInfo()) && declaration2.getScope().subsumes(for2.getLocationInfo())) {
						if(declaration1.getVariableName().equals(enhancedForExpression.getString())) {
							renamedVariable = declaration2.getVariableName();
							break;
						}
					}
				}
			}
			for(AbstractExpression expression2 : for2.getExpressions()) {
				if(expression2.getString().contains(enhancedForExpression.getString() + ".length") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".size()") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".iterator()") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".listIterator()")) {
					return replacementInfo.getReplacements();
				}
				if(renamedVariable != null) {
					if(expression2.getString().contains(renamedVariable + ".length") ||
							expression2.getString().contains(renamedVariable + ".size()") ||
							expression2.getString().contains(renamedVariable + ".iterator()") ||
							expression2.getString().contains(renamedVariable + ".listIterator()")) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//match while with enhanced for
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
			List<AbstractExpression> expressions2 = for2.getExpressions();
			AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
			VariableDeclaration iteratorDeclaration1 = null;
			for(AbstractCodeFragment codeFragment : replacementInfo.statements1) {
				AbstractCall invocation = codeFragment.invocationCoveringEntireFragment();
				if(invocation != null && invocation.getExpression() != null) {
					if(invocation.getExpression().equals(enhancedForExpression.getString())) {
						List<VariableDeclaration> variableDeclarations = codeFragment.getVariableDeclarations();
						if(variableDeclarations.size() == 1 && codeFragment.getLocationInfo().before(statement1.getLocationInfo())) {
							boolean iteratorDeclarationFound = false;
							for(String key : methodInvocationMap1.keySet()) {
								for(AbstractCall call : methodInvocationMap1.get(key)) {
									if(call.getExpression() != null && call.getExpression().equals(variableDeclarations.get(0).getVariableName())) {
										iteratorDeclarationFound = true;
										break;
									}
								}
							}
							if(iteratorDeclarationFound) {
								iteratorDeclaration1 = variableDeclarations.get(0);
								additionallyMatchedStatements1.add(codeFragment);
							}
						}
					}
					else if(iteratorDeclaration1 != null && invocation.getExpression().equals(iteratorDeclaration1.getVariableName())) {
						List<VariableDeclaration> variableDeclarations = codeFragment.getVariableDeclarations();
						if(variableDeclarations.size() == 1 && variableDeclarations2.size() == 1 && statement1.getLocationInfo().subsumes(codeFragment.getLocationInfo())) {
							//check if variable name and type are the same with the enhanced-for parameter
							VariableDeclaration v1 = variableDeclarations.get(0);
							VariableDeclaration v2 = variableDeclarations2.get(0);
							if(v1.getVariableName().equals(v2.getVariableName()) && v1.getType().equals(v2.getType())) {
								additionallyMatchedStatements1.add(codeFragment);
							}
						}
					}
				}
			}
			if(additionallyMatchedStatements1.size() > 0) {
				Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
				replacementInfo.addReplacement(composite);
				return replacementInfo.getReplacements();
			}
		}
		//match try-with-resources with regular try
		if(statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
			TryStatementObject try1 = (TryStatementObject)statement1;
			TryStatementObject try2 = (TryStatementObject)statement2;
			if(!try1.isTryWithResources() && try2.isTryWithResources()) {
				List<AbstractStatement> tryStatements1 = try1.getStatements();
				List<AbstractStatement> tryStatements2 = try2.getStatements();
				List<AbstractCodeFragment> matchedChildStatements1 = new ArrayList<>();
				List<AbstractCodeFragment> matchedChildStatements2 = new ArrayList<>();
				for(AbstractCodeMapping mapping : mappings) {
					if(tryStatements1.contains(mapping.getFragment1()) && tryStatements2.contains(mapping.getFragment2())) {
						matchedChildStatements1.add(mapping.getFragment1());
						matchedChildStatements2.add(mapping.getFragment2());
					}
				}
				if(matchedChildStatements1.size() > 0 && matchedChildStatements2.size() > 0) {
					List<AbstractStatement> unmatchedStatementsTry1 = new ArrayList<>();
					for(AbstractStatement tryStatement1 : tryStatements1) {
						if(!matchedChildStatements1.contains(tryStatement1)) {
							unmatchedStatementsTry1.add(tryStatement1);
						}
					}
					List<AbstractExpression> unmatchedExpressionsTry2 = new ArrayList<>();
					for(AbstractExpression tryExpression2 : try2.getExpressions()) {
						unmatchedExpressionsTry2.add(tryExpression2);
					}
					processLeaves(unmatchedStatementsTry1, unmatchedExpressionsTry2, parameterToArgumentMap, false);
					Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
					for(CompositeStatementObject catchClause1 : try1.getCatchClauses()) {
						additionallyMatchedStatements1.add(catchClause1);
					}
					Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
					replacementInfo.addReplacement(composite);
					return replacementInfo.getReplacements();
				}
			}
			else if(try1.isTryWithResources() && try2.isTryWithResources()) {
				if((creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) ||
						(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null)) {
					List<AbstractStatement> tryStatements1 = try1.getStatements();
					List<AbstractStatement> tryStatements2 = try2.getStatements();
					List<AbstractCodeFragment> matchedChildStatements1 = new ArrayList<>();
					List<AbstractCodeFragment> matchedChildStatements2 = new ArrayList<>();
					for(AbstractCodeMapping mapping : mappings) {
						if(tryStatements1.contains(mapping.getFragment1()) && tryStatements2.contains(mapping.getFragment2())) {
							matchedChildStatements1.add(mapping.getFragment1());
							matchedChildStatements2.add(mapping.getFragment2());
						}
					}
					if(matchedChildStatements1.size() > 0 && matchedChildStatements1.size() == matchedChildStatements2.size() &&
							(tryStatements1.size() == matchedChildStatements1.size() || tryStatements2.size() == matchedChildStatements2.size())) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation is identical
		if(assignmentInvocationCoveringTheEntireStatement1 != null && assignmentInvocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(invocation1.identical(assignmentInvocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, lambdaMappers) &&
							!assignmentInvocationCoveringTheEntireStatement1.arguments().contains(key1)) {
						if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
								!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
								!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
							return null;
						}
						String expression1 = assignmentInvocationCoveringTheEntireStatement1.getExpression();
						if(expression1 == null || !expression1.contains(key1)) {
							return replacementInfo.getReplacements();
						}
					}
					else if(invocation1.identicalName(assignmentInvocationCoveringTheEntireStatement2) && invocation1.equalArguments(assignmentInvocationCoveringTheEntireStatement2) &&
							!assignmentInvocationCoveringTheEntireStatement1.arguments().contains(key1) && assignmentInvocationCoveringTheEntireStatement2.getExpression() != null) {
						boolean expressionMatched = false;
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<AbstractCodeFragment>();
						Map<VariableDeclaration, AbstractCodeFragment> variableDeclarationsInUnmatchedStatements2 = new LinkedHashMap<VariableDeclaration, AbstractCodeFragment>();
						for(AbstractCodeFragment codeFragment : replacementInfo.statements2) {
							for(VariableDeclaration variableDeclaration : codeFragment.getVariableDeclarations()) {
								variableDeclarationsInUnmatchedStatements2.put(variableDeclaration, codeFragment);
							}
						}
						for(AbstractCodeFragment codeFragment : replacementInfo.statements2) {
							VariableDeclaration variableDeclaration = codeFragment.getVariableDeclaration(assignmentInvocationCoveringTheEntireStatement2.getExpression());
							AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
							if(variableDeclaration != null && variableDeclaration.getInitializer() != null) {
								String initializer = variableDeclaration.getInitializer().getString();
								if(invocation1.getExpression() != null && invocation1.getExpression().equals(initializer)) {
									Replacement r = new Replacement(invocation1.getExpression(), variableDeclaration.getVariableName(), ReplacementType.VARIABLE_REPLACED_WITH_EXPRESSION_OF_METHOD_INVOCATION);
									replacementInfo.getReplacements().add(r);
									additionallyMatchedStatements2.add(codeFragment);
									expressionMatched = true;
								}
								else if(invocation1.getExpression() != null) {
									String temp = initializer;
									Set<VariableDeclaration> matchingDeclarations = new LinkedHashSet<>();
									for(VariableDeclaration decl : variableDeclarationsInUnmatchedStatements2.keySet()) {
										if(temp.contains(decl.getVariableName() + ".") && decl.getInitializer() != null) {
											temp = ReplacementUtil.performReplacement(temp, decl.getVariableName(), decl.getInitializer().getString());
											matchingDeclarations.add(decl);
										}
									}
									if(invocation1.getExpression().equals(temp)) {
										expressionMatched = true;
										additionallyMatchedStatements2.add(codeFragment);
										for(VariableDeclaration decl : matchingDeclarations) {
											additionallyMatchedStatements2.add(variableDeclarationsInUnmatchedStatements2.get(decl));
										}
									}
									else if(invocation1.getExpression().startsWith(temp + ".")) {
										additionallyMatchedStatements2.add(codeFragment);
										for(VariableDeclaration decl : matchingDeclarations) {
											additionallyMatchedStatements2.add(variableDeclarationsInUnmatchedStatements2.get(decl));
										}
										for(AbstractCodeFragment codeFragment2 : replacementInfo.statements2) {
											AbstractCall invocationCoveringEntireCodeFragment2 = codeFragment2.invocationCoveringEntireFragment();
											if(invocationCoveringEntireCodeFragment2 != null) {
												String extendedTemp = temp + "." + invocationCoveringEntireCodeFragment2.actualString().substring(
														invocationCoveringEntireCodeFragment2.getExpression() != null ? invocationCoveringEntireCodeFragment2.getExpression().length()+1 : 0);
												if(invocation1.getExpression().startsWith(extendedTemp)) {
													additionallyMatchedStatements2.add(codeFragment2);
													temp = extendedTemp;
												}
											}
										}
										if(invocation1.getExpression().equals(temp)) {
											expressionMatched = true;
										}
									}
								}
							}
							if(invocationCoveringEntireCodeFragment != null && assignmentInvocationCoveringTheEntireStatement1.identicalName(invocationCoveringEntireCodeFragment) &&
									assignmentInvocationCoveringTheEntireStatement1.equalArguments(invocationCoveringEntireCodeFragment)) {
								additionallyMatchedStatements2.add(codeFragment);
							}
						}
						if(classDiff != null) {
							boolean removedAttributeMatched = false;
							for(UMLAttribute removedAttribute : classDiff.getRemovedAttributes()) {
								if(removedAttribute.getName().equals(assignmentInvocationCoveringTheEntireStatement1.getExpression())) {
									removedAttributeMatched = true;
									break;
								}
							}
							boolean addedAttributeMatched = false;
							for(UMLAttribute addedAttribute : classDiff.getAddedAttributes()) {
								if(addedAttribute.getName().equals(assignmentInvocationCoveringTheEntireStatement2.getExpression())) {
									addedAttributeMatched = true;
									break;
								}
							}
							if(removedAttributeMatched && addedAttributeMatched) {
								expressionMatched = true;
								Replacement r = new Replacement(assignmentInvocationCoveringTheEntireStatement1.getExpression(), assignmentInvocationCoveringTheEntireStatement2.getExpression(), ReplacementType.VARIABLE_NAME);
								replacementInfo.addReplacement(r);
							}
						}
						for(AbstractCodeMapping mapping : this.mappings) {
							for(Replacement r : mapping.getReplacements()) {
								if(r.getBefore().equals(assignmentInvocationCoveringTheEntireStatement1.getExpression()) &&
										r.getAfter().equals(assignmentInvocationCoveringTheEntireStatement2.getExpression())) {
									expressionMatched = true;
									replacementInfo.addReplacement(r);
									break;
								}
							}
							if(expressionMatched) {
								break;
							}
						}
						if(expressionMatched) {
							if(additionallyMatchedStatements2.size() > 0) {
								Replacement r = new CompositeReplacement(statement1.getString(), statement2.getString(), new LinkedHashSet<AbstractCodeFragment>(), additionallyMatchedStatements2);
								replacementInfo.getReplacements().add(r);
							}
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		//method invocation is identical with a difference in the expression call chain
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1 instanceof OperationInvocation && invocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			if(((OperationInvocation)invocationCoveringTheEntireStatement1).identicalWithExpressionCallChainDifference((OperationInvocation)invocationCoveringTheEntireStatement2)) {
				List<? extends AbstractCall> invokedOperationsBefore = methodInvocationMap1.get(invocationCoveringTheEntireStatement1.getExpression());
				List<? extends AbstractCall> invokedOperationsAfter = methodInvocationMap2.get(invocationCoveringTheEntireStatement2.getExpression());
				if(invokedOperationsBefore != null && invokedOperationsBefore.size() > 0 && invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
					AbstractCall invokedOperationBefore = invokedOperationsBefore.get(0);
					AbstractCall invokedOperationAfter = invokedOperationsAfter.get(0);
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION_EXPRESSION);
					replacementInfo.addReplacement(replacement);
					Set<AbstractCodeFragment> additionallyMatchedStatements1 = additionallyMatchedStatements(variableDeclarations2, replacementInfo.statements1);
					Set<AbstractCodeFragment> additionallyMatchedStatements2 = additionallyMatchedStatements(variableDeclarations1, replacementInfo.statements2);
					if(additionallyMatchedStatements1.size() > 0 || additionallyMatchedStatements2.size() > 0) {
						Replacement r = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
						replacementInfo.getReplacements().add(r);
					}
					return replacementInfo.getReplacements();
				}
				else if(invokedOperationsBefore != null && invokedOperationsBefore.size() > 0) {
					AbstractCall invokedOperationBefore = invokedOperationsBefore.get(0);
					Replacement replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore, Direction.INVOCATION_TO_VARIABLE);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				else if(invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
					AbstractCall invokedOperationAfter = invokedOperationsAfter.get(0);
					Replacement replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationAfter, Direction.VARIABLE_TO_INVOCATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				if(((OperationInvocation)invocationCoveringTheEntireStatement1).numberOfSubExpressions() == ((OperationInvocation)invocationCoveringTheEntireStatement2).numberOfSubExpressions() &&
						invocationCoveringTheEntireStatement1.getExpression().contains(".") == invocationCoveringTheEntireStatement2.getExpression().contains(".")) {
					return replacementInfo.getReplacements();
				}
			}
			String expression1 = invocationCoveringTheEntireStatement1.getExpression();
			String expression2 = invocationCoveringTheEntireStatement2.getExpression();
			boolean staticVSNonStatic = (expression1 == null && expression2 != null && container1 != null && container1.getClassName().endsWith("." + expression2)) ||
					(expression1 != null && expression2 == null && container2 != null && container2.getClassName().endsWith("." + expression1));
			if(!staticVSNonStatic && modelDiff != null) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					if((expression1 == null && expression2 != null && container1 != null && addedClass.getName().endsWith("." + expression2)) ||
							(expression1 != null && expression2 == null && container2 != null && addedClass.getName().endsWith("." + expression1))) {
						staticVSNonStatic = true;
						break;
					}
				}
			}
			boolean additionalCaller = invocationCoveringTheEntireStatement1.actualString().endsWith("." + invocationCoveringTheEntireStatement2.actualString()) ||
					invocationCoveringTheEntireStatement2.actualString().endsWith("." + invocationCoveringTheEntireStatement1.actualString()) ||
					s2.endsWith("." + s1) || s1.endsWith("." + s2);
			if((invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) || invocationCoveringTheEntireStatement1.compatibleName(invocationCoveringTheEntireStatement2)) &&
					(staticVSNonStatic || additionalCaller) && invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation is identical if arguments are replaced
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(String key : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key)) {
					if(invocation2.arguments().size() > 0 && invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocation2, replacementInfo.getReplacements(), lambdaMappers)) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation is identical if expression and one arguments are swapped
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalWithExpressionArgumentSwap(invocationCoveringTheEntireStatement2)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_EXPRESSION_ARGUMENT_SWAPPED);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation is identical if arguments are wrapped or concatenated
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(String key : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key)) {
					if(invocationCoveringTheEntireStatement1.identicalOrWrappedArguments(invocation2)) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT_WRAPPED);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					if(invocationCoveringTheEntireStatement1.identicalOrConcatenatedArguments(invocation2)) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT_CONCATENATED);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation has been renamed but the expression and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null && statement1.getClass().equals(statement2.getClass()) &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers,
						matchPairOfRemovedAddedOperationsWithIdenticalBody(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2))) {
			boolean variableDeclarationMatch = true;
			if(variableDeclarations1.size() > 0  && variableDeclarations2.size() > 0 && !variableDeclarations1.toString().equals(variableDeclarations2.toString()) && !invocationCoveringTheEntireStatement1.arguments().equals(invocationCoveringTheEntireStatement2.arguments())) {
				variableDeclarationMatch = false;
			}
			if(variableDeclarationMatch) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null && statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType()) && creationMap1.size() == 0 && creationMap2.size() == 0 &&
				methodInvocationMap1.size() == methodInvocationMap2.size() && methodInvocationMap1.size() == 1 && methodInvocations1.size() == methodInvocations2.size() && methodInvocations1.size() == 1) {
			AbstractCall invocation1 = methodInvocationMap1.get(methodInvocations1.iterator().next()).get(0);
			AbstractCall invocation2 = methodInvocationMap2.get(methodInvocations2.iterator().next()).get(0);
			if(invocation1.renamedWithIdenticalExpressionAndArguments(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers, matchPairOfRemovedAddedOperationsWithIdenticalBody(invocation1, invocation2))) {
				Replacement replacement = new MethodInvocationReplacement(invocation1.getName(),
						invocation2.getName(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_NAME);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation has been renamed and the expression is different but the arguments are identical, and the variable declarations are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
				invocationCoveringTheEntireStatement1.variableDeclarationInitializersRenamedWithIdenticalArguments(invocationCoveringTheEntireStatement2)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed but the expressions are null and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalArgumentsAndNoExpression(invocationCoveringTheEntireStatement2, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed (one name contains the other), one expression is null, but the other is not null, and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithDifferentExpressionAndIdenticalArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
			boolean variableDeclarationInitializer1 =  invocationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean variableDeclarationInitializer2 =  invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean logStatement1 = invocationCoveringTheEntireStatement1.isLog();
			boolean logStatement2 = invocationCoveringTheEntireStatement2.isLog();
			boolean setMatch = (invocationCoveringTheEntireStatement1.getName().equals("set") && !invocationCoveringTheEntireStatement2.getName().equals("set")) ||
					(!invocationCoveringTheEntireStatement1.getName().equals("set") && invocationCoveringTheEntireStatement2.getName().equals("set"));
			boolean getMatch = (invocationCoveringTheEntireStatement1.getName().equals("get") && !invocationCoveringTheEntireStatement2.getName().equals("get")) ||
					(!invocationCoveringTheEntireStatement1.getName().equals("get") && invocationCoveringTheEntireStatement2.getName().equals("get"));
			boolean callToAddedOperation = false;
			boolean callToDeletedOperation = false;
			if(classDiff != null) {
				callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), container2) != null;
				callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), container1) != null;
			}
			if(variableDeclarationInitializer1 == variableDeclarationInitializer2 && logStatement1 == logStatement2 && !setMatch && !getMatch && callToAddedOperation == callToDeletedOperation) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation has been renamed (one name contains the other), both expressions are null, and one contains all the arguments of the other
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithNoExpressionAndArgumentIntersection(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
			boolean callToAddedOperation = false;
			boolean callToDeletedOperation = false;
			if(classDiff != null && !invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				UMLOperation matchedAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), container2);
				if(matchedAddedOperation != null) {
					callToAddedOperation = true;
					for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
						if(removedOperation.getName().equals(matchedAddedOperation.getName())) {
							callToAddedOperation = false;
							break;
						}
					}
				}
				UMLOperation matchedRemovedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), container1);
				if(matchedRemovedOperation != null) {
					callToDeletedOperation = true;
					for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
						if(addedOperation.getName().equals(matchedRemovedOperation.getName())) {
							callToDeletedOperation = false;
							break;
						}
					}
				}
			}
			if(callToAddedOperation == callToDeletedOperation) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation has been renamed and arguments changed, but the expressions are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndDifferentArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
			ReplacementType type = invocationCoveringTheEntireStatement1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ? ReplacementType.METHOD_INVOCATION_ARGUMENT : ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, type);
			replacementInfo.addReplacement(replacement);
			if(invocationCoveringTheEntireStatement2.arguments().size() > 0) {
				for(String argument2 : invocationCoveringTheEntireStatement2.arguments()) {
					LeafExpression matchingVariable = null;
					for(LeafExpression variable : statement2.getVariables()) {
						if(argument2.startsWith(variable.getString())) {
							matchingVariable = variable;
							break;
						}
					}
					if(matchingVariable != null) {
						for(AbstractCodeFragment codeFragment : replacementInfo.statements2) {
							if(codeFragment.getString().startsWith(matchingVariable.getString() + "." + "add")) {
								for(String argument1 : invocationCoveringTheEntireStatement1.arguments()) {
									List<LeafExpression> leafExpressions2 = codeFragment.findExpression(argument1);
									if(leafExpressions2.size() == 1) {
										List<LeafExpression> leafExpressions1 = statement1.findExpression(argument1);
										if(leafExpressions1.size() == 1) {
											LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), container1, container2);
											addMapping(leafMapping);
										}
									}
								}
							}
						}
					}
				}
			}
			return replacementInfo.getReplacements();
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			boolean variableDeclarationMatchedWithNonVariableDeclaration = false;
			if(invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
				if(invocationCoveringTheEntireStatement1 != null) {
					if(!invocationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
					else if(!variableDeclarations1.get(0).getVariableName().equals(variableDeclarations2.get(0).getVariableName()) &&
							!variableDeclarations1.get(0).equalType(variableDeclarations2.get(0))) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
				}
				else if(creationCoveringTheEntireStatement1 != null) {
					if(!creationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
					else if(!variableDeclarations1.get(0).getVariableName().equals(variableDeclarations2.get(0).getVariableName()) &&
							!variableDeclarations1.get(0).equalType(variableDeclarations2.get(0))) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
				}
			}
			for(String methodInvocation1 : methodInvocations1) {
				for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
					if(operationInvocation1.renamedWithIdenticalExpressionAndDifferentArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers) &&
							!isExpressionOfAnotherMethodInvocation(operationInvocation1, methodInvocationMap1) &&
							!variableDeclarationMatchedWithNonVariableDeclaration) {
						ReplacementType type = operationInvocation1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ? ReplacementType.METHOD_INVOCATION_ARGUMENT : ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, type);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation has only changes in the arguments (different number of arguments)
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			if(invocationCoveringTheEntireStatement1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement1.inlinedStatementBecomesAdditionalArgument(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.statements1)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation1 : methodInvocations1) {
				for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
					if(operationInvocation1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						return replacementInfo.getReplacements();
					}
					else if(operationInvocation1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					else if(operationInvocation1.inlinedStatementBecomesAdditionalArgument(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.statements1)) {
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.getName().equals("add") && invocationCoveringTheEntireStatement2.getName().equals("setWidget") &&
				invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression() != null &&
				invocationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2).size() > 0) {
			if(invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			for(Replacement r : replacementInfo.getReplacements()) {
				if(invocationCoveringTheEntireStatement1.getExpression().equals(r.getBefore()) && invocationCoveringTheEntireStatement2.getExpression().equals(r.getAfter())) {
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//check if the argument of the method call in the first statement is returned in the second statement
		Replacement r;
		if(invocationCoveringTheEntireStatement1 != null && (r = invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation1 : methodInvocations1) {
			for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
				if(statement1.getString().endsWith(methodInvocation1 + ";\n") && (r = operationInvocation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
					if(operationInvocation1.makeReplacementForReturnedArgument(statement2.getString()) != null) {
						replacementInfo.addReplacement(r);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the second statement is returned in the first statement
		if(invocationCoveringTheEntireStatement2 != null && (r = invocationCoveringTheEntireStatement2.makeReplacementForWrappedCall(replacementInfo.getArgumentizedString1())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		if(invocationCoveringTheEntireStatement2 != null && (r = invocationCoveringTheEntireStatement2.makeReplacementForWrappedLambda(replacementInfo.getArgumentizedString1())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation2 : methodInvocations2) {
			for(AbstractCall operationInvocation2 : methodInvocationMap2.get(methodInvocation2)) {
				if(statement2.getString().endsWith(methodInvocation2 + ";\n") && (r = operationInvocation2.makeReplacementForWrappedCall(replacementInfo.getArgumentizedString1())) != null) {
					if(operationInvocation2.makeReplacementForWrappedCall(statement1.getString()) != null) {
						replacementInfo.addReplacement(r);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the second statement is the right hand side of an assignment in the first statement
		if(invocationCoveringTheEntireStatement2 != null &&
				(r = invocationCoveringTheEntireStatement2.makeReplacementForAssignedArgument(replacementInfo.getArgumentizedString1())) != null &&
				(methodInvocationMap1.containsKey(r.getBefore()) || methodInvocationMap1.containsKey(r.getAfter()))) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		//check if the method call in the second statement is the expression (or sub-expression) of the method invocation in the first statement
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n")) {
						if(methodInvocationMap2.keySet().contains(invocation1.getExpression())) {
							Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
									invocationCoveringTheEntireStatement2.arguments().contains(invocation1.getExpression()) &&
									!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
									!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
								return null;
							}
							return replacementInfo.getReplacements();
						}
						if(invocation1 instanceof OperationInvocation) {
							Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<AbstractCodeFragment>();
							for(String subExpression1 : ((OperationInvocation)invocation1).getSubExpressions()) {
								if(methodInvocationMap2.keySet().contains(subExpression1)) {
									AbstractCall subOperationInvocation1 = null;
									for(String key : methodInvocationMap1.keySet()) {
										if(key.endsWith(subExpression1)) {
											subOperationInvocation1 = methodInvocationMap1.get(key).get(0);
											break;
										}
									}
									if(additionallyMatchedStatements2.size() > 0) {
										CompositeReplacement composite = new CompositeReplacement(subExpression1,
												invocationCoveringTheEntireStatement2.actualString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
										replacementInfo.addReplacement(composite);
									}
									Replacement replacement = new MethodInvocationReplacement(subExpression1,
											invocationCoveringTheEntireStatement2.actualString(), subOperationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
									replacementInfo.addReplacement(replacement);
									return replacementInfo.getReplacements();
								}
								for(AbstractCodeFragment codeFragment : replacementInfo.statements2) {
									AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
									if(invocationCoveringEntireCodeFragment != null && subExpression1.equals(invocationCoveringEntireCodeFragment.actualString())) {
										additionallyMatchedStatements2.add(codeFragment);
									}
								}
							}
						}
					}
				}
			}
		}
		//check if the method call in the first statement is the expression (or sub-expression) of the method invocation in the second statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(statement2.getString().endsWith(key2 + ";\n")) {
						if(methodInvocationMap1.keySet().contains(invocation2.getExpression())) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocation2.actualString(), invocationCoveringTheEntireStatement1, invocation2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
									invocationCoveringTheEntireStatement1.arguments().contains(invocation2.getExpression()) &&
									!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
									!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
								return null;
							}
							return replacementInfo.getReplacements();
						}
						if(invocation2 instanceof OperationInvocation) {
							Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
							for(String subExpression2 : ((OperationInvocation)invocation2).getSubExpressions()) {
								if(methodInvocationMap1.keySet().contains(subExpression2)) {
									AbstractCall subOperationInvocation2 = null;
									for(String key : methodInvocationMap2.keySet()) {
										if(key.endsWith(subExpression2)) {
											subOperationInvocation2 = methodInvocationMap2.get(key).get(0);
											break;
										}
									}
									if(additionallyMatchedStatements1.size() > 0) {
										CompositeReplacement composite = new CompositeReplacement(invocationCoveringTheEntireStatement1.actualString(),
												subExpression2, additionallyMatchedStatements1, new LinkedHashSet<>());
										replacementInfo.addReplacement(composite);
									}
									Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
											subExpression2, invocationCoveringTheEntireStatement1, subOperationInvocation2, ReplacementType.METHOD_INVOCATION);
									replacementInfo.addReplacement(replacement);
									return replacementInfo.getReplacements();
								}
								for(AbstractCodeFragment codeFragment : replacementInfo.statements1) {
									AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
									if(invocationCoveringEntireCodeFragment != null && subExpression2.equals(invocationCoveringEntireCodeFragment.actualString())) {
										additionallyMatchedStatements1.add(codeFragment);
									}
								}
							}
						}
					}
				}
			}
		}
		//check if the class instance creation in the first statement is the expression of the method invocation in the second statement
		if(creationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(statement2.getString().endsWith(key2 + ";\n") && invocation2.getExpression() != null &&
							invocation2.getExpression().startsWith(creationCoveringTheEntireStatement1.actualString())) {
						Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
								invocation2.getName(), creationCoveringTheEntireStatement1, invocation2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the class instance creation in the second statement is the class instance creation of the first statement
		if(creationCoveringTheEntireStatement2 != null) {
			for(String key1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n") &&
							creationCoveringTheEntireStatement2.arguments().contains(creation1.actualString())) {
						if(variableDeclarations1.size() > 0) {
							VariableDeclaration declaration1 = variableDeclarations1.get(0);
							for(AbstractCodeFragment fragment1 : replacementInfo.statements1) {
								for(AbstractCall fragmentCreation1 : fragment1.getCreations()) {
									if(fragmentCreation1.arguments().contains(declaration1.getVariableName()) &&
											creationCoveringTheEntireStatement2.identicalName(fragmentCreation1)) {
										Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
										additionallyMatchedStatements1.add(fragment1);
										Replacement replacement = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
						Replacement replacement = new ObjectCreationReplacement(creation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)creation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//builder call chain in the first statement is replaced with class instance creation in the second statement
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement1.getName().equals("build")) {
			if(creationCoveringTheEntireStatement2 != null) {
				int commonArguments = 0;
				for(String key1 : methodInvocationMap1.keySet()) {
					if(invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
						for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
							Set<String> argumentIntersection = invocation1.argumentIntersection(creationCoveringTheEntireStatement2);
							commonArguments += argumentIntersection.size();
						}
					}
				}
				if(commonArguments > 0) {
					Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
							creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement2 != null) {
				int commonArguments = 0;
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<AbstractCodeFragment>(); 
				for(String key1 : methodInvocationMap1.keySet()) {
					if(invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
						for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
							if(invocation1.equalArguments(invocationCoveringTheEntireStatement2)) {
								commonArguments += invocation1.arguments().size();
							}
							else {
								Set<String> argumentIntersection = invocation1.argumentIntersection(invocationCoveringTheEntireStatement2);
								int threshold = Math.max(invocation1.arguments().size(), invocationCoveringTheEntireStatement2.arguments().size())/2;
								if(argumentIntersection.size() > threshold) {
									commonArguments += argumentIntersection.size();
								}
							}
							for(AbstractCodeFragment codeFragment : replacementInfo.statements2) { 
								AbstractCall invocation = codeFragment.invocationCoveringEntireFragment(); 
								if(invocation != null) { 
									if(invocation.identical(invocation1, replacementInfo.getReplacements(), parameterToArgumentMap, lambdaMappers)) { 
										additionallyMatchedStatements2.add(codeFragment); 
									} 
									if((invocation.getExpression() != null && invocation.getExpression().equals(invocation1.actualString())) ||
											invocation.callChainIntersection(invocation1).size() > 0) {
										additionallyMatchedStatements2.add(codeFragment); 
									} 
								} 
							}
						}
					}
				}
				if(commonArguments > 0) {
					if(additionallyMatchedStatements2.size() > 0) { 
						Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), new LinkedHashSet<AbstractCodeFragment>(), additionallyMatchedStatements2); 
						replacementInfo.addReplacement(composite); 
					}
					else {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(),
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
					}
					return replacementInfo.getReplacements();
				}
			}
		}
		//class instance creation in the first statement is replaced with builder call chain in the second statement
		if(invocationCoveringTheEntireStatement2 != null && invocationCoveringTheEntireStatement2.getName().equals("build")) {
			if(creationCoveringTheEntireStatement1 != null) {
				int commonArguments = 0;
				for(String key2 : methodInvocationMap2.keySet()) {
					if(invocationCoveringTheEntireStatement2.actualString().startsWith(key2)) {
						for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
							Set<String> argumentIntersection = invocation2.argumentIntersection(creationCoveringTheEntireStatement1);
							commonArguments += argumentIntersection.size();
						}
					}
				}
				if(commonArguments > 0) {
					Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
							invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement1 != null) {
				int commonArguments = 0;
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
				for(String key2 : methodInvocationMap2.keySet()) {
					if(invocationCoveringTheEntireStatement2.actualString().startsWith(key2)) {
						for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
							if(invocation2.equalArguments(invocationCoveringTheEntireStatement1)) {
								commonArguments += invocation2.arguments().size();
							}
							else {
								Set<String> argumentIntersection = invocation2.argumentIntersection(invocationCoveringTheEntireStatement1);
								int threshold = Math.max(invocation2.arguments().size(), invocationCoveringTheEntireStatement1.arguments().size())/2;
								if(argumentIntersection.size() > threshold) {
									commonArguments += argumentIntersection.size();
								}
							}
							for(AbstractCodeFragment codeFragment : replacementInfo.statements1) {
								AbstractCall invocation = codeFragment.invocationCoveringEntireFragment();
								if(invocation != null) {
									if(invocation.identical(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, lambdaMappers)) {
										additionallyMatchedStatements1.add(codeFragment);
									}
									if((invocation.getExpression() != null && invocation.getExpression().equals(invocation2.actualString())) ||
											invocation.callChainIntersection(invocation2).size() > 0) {
										additionallyMatchedStatements1.add(codeFragment);
									}
								}
							}
						}
					}
				}
				if(commonArguments > 0) {
					if(additionallyMatchedStatements1.size() > 0) {
						Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
						replacementInfo.addReplacement(composite);
					}
					else {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(),
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
					}
					return replacementInfo.getReplacements();
				}
			}
		}
		//object creation is identical
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.identical(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, lambdaMappers)) {
			boolean identicalArrayInitializer = true;
			if(creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray()) {
				identicalArrayInitializer = creationCoveringTheEntireStatement1.identicalArrayInitializer(creationCoveringTheEntireStatement2);
			}
			if(identicalArrayInitializer) {
				String anonymousClassDeclaration1 = creationCoveringTheEntireStatement1.getAnonymousClassDeclaration();
				String anonymousClassDeclaration2 = creationCoveringTheEntireStatement2.getAnonymousClassDeclaration();
				if(anonymousClassDeclaration1 != null && anonymousClassDeclaration2 != null && !anonymousClassDeclaration1.equals(anonymousClassDeclaration2)) {
					Replacement replacement = new Replacement(anonymousClassDeclaration1, anonymousClassDeclaration2, ReplacementType.ANONYMOUS_CLASS_DECLARATION);
					replacementInfo.addReplacement(replacement);
				}
				return replacementInfo.getReplacements();
			}
		}
		//object creation has identical arguments, but different type
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.arguments().size() > 0 && creationCoveringTheEntireStatement1.equalArguments(creationCoveringTheEntireStatement2)) {
			Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.getName(),
					creationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.arguments().size() > 0 && creationCoveringTheEntireStatement1.getType().equalsWithSubType(creationCoveringTheEntireStatement2.getType()) &&
				creationCoveringTheEntireStatement1.getType().getClassType().endsWith("Exception") &&
				creationCoveringTheEntireStatement2.getType().getClassType().endsWith("Exception")) {
			Set<String> argumentIntersection = creationCoveringTheEntireStatement1.argumentIntersection(creationCoveringTheEntireStatement2);
			if(argumentIntersection.size() > 0 && argumentIntersection.size() == Math.min(creationCoveringTheEntireStatement1.arguments().size(), creationCoveringTheEntireStatement2.arguments().size())) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement1.getType().equals(creationCoveringTheEntireStatement2.getType())) {
				CompositeStatementObject parent1 = statement1.getParent();
				CompositeStatementObject parent2 = statement2.getParent();
				while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent1 = parent1.getParent(); 
				}
				while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent2 = parent2.getParent(); 
				}
				if(parent1 != null && parent2 != null && parent1.getString().equals(parent2.getString())) {
					Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
							creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//object creation has only changes in the arguments
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
			if(creationCoveringTheEntireStatement1.identicalWithMergedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.reorderedArguments(creationCoveringTheEntireStatement2)) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.inlinedStatementBecomesAdditionalArgument(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.statements1)) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//check if the argument lists are identical after replacements
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.identicalName(creationCoveringTheEntireStatement2) &&
				creationCoveringTheEntireStatement1.identicalExpression(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) &&
				!creationCoveringTheEntireStatement1.allArgumentsReplaced(creationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
			if(creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains("[") && s2.contains("[") &&
					s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).equals(s2.substring(s2.indexOf("[")+1, s2.lastIndexOf("]"))) &&
					s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).length() > 0) {
				return replacementInfo.getReplacements();
			}
			if(!creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains("(") && s2.contains("(") &&
					s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).equals(s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"))) &&
					s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).length() > 0) {
				return replacementInfo.getReplacements();
			}
		}
		//check if array creation is replaced with data structure creation
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
			VariableDeclaration v1 = variableDeclarations1.get(0);
			VariableDeclaration v2 = variableDeclarations2.get(0);
			String initializer1 = v1.getInitializer() != null ? v1.getInitializer().getString() : null;
			String initializer2 = v2.getInitializer() != null ? v2.getInitializer().getString() : null;
			UMLType v1Type = v1.getType();
			UMLType v2Type = v2.getType();
			if(v1Type != null && v2Type != null) {
				if(v1Type.getArrayDimension() == 1 && v2Type.containsTypeArgument(v1Type.getClassType()) &&
						creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() &&
						initializer1 != null && initializer2 != null &&
						initializer1.substring(initializer1.indexOf("[")+1, initializer1.lastIndexOf("]")).equals(initializer2.substring(initializer2.indexOf("(")+1, initializer2.lastIndexOf(")")))) {
					r = new ObjectCreationReplacement(initializer1, initializer2,
							creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
				if(v2Type.getArrayDimension() == 1 && v1Type.containsTypeArgument(v2Type.getClassType()) &&
						!creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() &&
						initializer1 != null && initializer2 != null &&
						initializer1.substring(initializer1.indexOf("(")+1, initializer1.lastIndexOf(")")).equals(initializer2.substring(initializer2.indexOf("[")+1, initializer2.lastIndexOf("]")))) {
					r = new ObjectCreationReplacement(initializer1, initializer2,
							creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
		}
		if(!creations1.isEmpty() && creationCoveringTheEntireStatement2 != null) {
			for(String creation1 : creations1) {
				for(AbstractCall objectCreation1 : creationMap1.get(creation1)) {
					if(objectCreation1.identicalWithMergedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.reorderedArguments(creationCoveringTheEntireStatement2)) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.identicalWithDifferentNumberOfArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.inlinedStatementBecomesAdditionalArgument(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.statements1)) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					//check if the argument lists are identical after replacements
					if(objectCreation1.identicalName(creationCoveringTheEntireStatement2) &&
							objectCreation1.identicalExpression(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						if(((ObjectCreation)objectCreation1).isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains("[") && s2.contains("[") &&
								s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).equals(s2.substring(s2.indexOf("[")+1, s2.lastIndexOf("]"))) &&
								s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).length() > 0) {
							return replacementInfo.getReplacements();
						}
						if(!((ObjectCreation)objectCreation1).isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains("(") && s2.contains("(") &&
								s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).equals(s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"))) &&
								s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).length() > 0) {
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && (r = creationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String creation1 : creations1) {
			for(AbstractCall objectCreation1 : creationMap1.get(creation1)) {
				if(statement1.getString().endsWith(creation1 + ";\n") && (r = objectCreation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
		}
		if(variableDeclarationWithArrayInitializer1 != null && invocationCoveringTheEntireStatement2 != null && !(invocationCoveringTheEntireStatement2 instanceof MethodReference) && variableDeclarations2.isEmpty() &&
				!containsMethodSignatureOfAnonymousClass(statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
			String args1 = s1.substring(s1.indexOf("{")+1, s1.lastIndexOf("}"));
			String args2 = s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"));
			if(args1.equals(args2)) {
				r = new Replacement(args1, args2, ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(variableDeclarationWithArrayInitializer2 != null && invocationCoveringTheEntireStatement1 != null && !(invocationCoveringTheEntireStatement1 instanceof MethodReference) && variableDeclarations1.isEmpty() &&
				!containsMethodSignatureOfAnonymousClass(statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
			String args1 = s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")"));
			String args2 = s2.substring(s2.indexOf("{")+1, s2.lastIndexOf("}"));
			if(args1.equals(args2)) {
				r = new Replacement(args1, args2, ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		List<TernaryOperatorExpression> ternaryOperatorExpressions1 = statement1.getTernaryOperatorExpressions();
		List<TernaryOperatorExpression> ternaryOperatorExpressions2 = statement2.getTernaryOperatorExpressions();
		if(ternaryOperatorExpressions1.isEmpty() && ternaryOperatorExpressions2.size() == 1) {
			TernaryOperatorExpression ternary = ternaryOperatorExpressions2.get(0);
			for(String creation : creationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(creation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			for(String methodInvocation : methodInvocationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(methodInvocation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement1 != null && (r = ternary.makeReplacementWithTernaryOnTheRight(invocationCoveringTheEntireStatement1.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement1 != null && (r = ternary.makeReplacementWithTernaryOnTheRight(creationCoveringTheEntireStatement1.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			for(String creation2 : creations2) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(creation2)) != null) {
					for(AbstractCall c2 : creationMap2.get(creation2)) {
						for(String creation1 : creations1) {
							for(AbstractCall c1 : creationMap1.get(creation1)) {
								if(((ObjectCreation)c1).getType().compatibleTypes(((ObjectCreation)c2).getType()) && c1.equalArguments(c2)) {
									replacementInfo.addReplacement(r);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		if(ternaryOperatorExpressions1.size() == 1 && ternaryOperatorExpressions2.isEmpty()) {
			TernaryOperatorExpression ternary = ternaryOperatorExpressions1.get(0);
			for(String creation : creationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(creation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			for(String methodInvocation : methodInvocationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(methodInvocation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement2 != null && (r = ternary.makeReplacementWithTernaryOnTheLeft(invocationCoveringTheEntireStatement2.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement2 != null && (r = ternary.makeReplacementWithTernaryOnTheLeft(creationCoveringTheEntireStatement2.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			for(String creation1 : creations1) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(creation1)) != null) {
					for(AbstractCall c1 : creationMap1.get(creation1)) {
						for(String creation2 : creations2) {
							for(AbstractCall c2 : creationMap2.get(creation2)) {
								if(((ObjectCreation)c1).getType().compatibleTypes(((ObjectCreation)c2).getType()) && c1.equalArguments(c2)) {
									replacementInfo.addReplacement(r);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null && statement2.getString().equals(invocationCoveringTheEntireStatement2.actualString() + ";\n") &&
				invocationCoveringTheEntireStatement2.arguments().size() == 1 && statement1.getString().endsWith("=" + invocationCoveringTheEntireStatement2.arguments().get(0) + ";\n") &&
				invocationCoveringTheEntireStatement2.expressionIsNullOrThis() && invocationCoveringTheEntireStatement2.getName().startsWith("set")) {
			String prefix1 = statement1.getString().substring(0, statement1.getString().lastIndexOf("="));
			if(variables1.contains(prefix1)) {
				String before = prefix1 + "=" + invocationCoveringTheEntireStatement2.arguments().get(0);
				String after = invocationCoveringTheEntireStatement2.actualString();
				r = new Replacement(before, after, ReplacementType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER_METHOD_INVOCATION);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				(variableDeclarations1.size() == variableDeclarations2.size() || (variableDeclarations1.size() > 0 && statement2.getString().startsWith("return ")))) {
			if(invocationCoveringTheEntireStatement2.getName().equals("of") && variableDeclarations1.size() > 0) {
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				for(String argument2 : invocationCoveringTheEntireStatement2.arguments()) {
					for(AbstractCodeFragment fragment1 : replacementInfo.statements1) {
						AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
						if(invocation1 != null && invocation1.getExpression() != null && invocation1.getExpression().equals(variableDeclarations1.get(0).getVariableName())) {
							boolean argumentMatched = false;
							for(String argument1 : invocation1.arguments()) {
								if(argument1.equals(argument2)) {
									List<LeafExpression> leafExpressions1 = fragment1.findExpression(argument1);
									List<LeafExpression> leafExpressions2 = statement2.findExpression(argument2);
									if(leafExpressions1.size() == 1 && leafExpressions2.size() == 1) {
										LeafMapping mapping = createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, isEqualWithReplacement);
										addMapping(mapping);
										additionallyMatchedStatements1.add(fragment1);
									}
									argumentMatched = true;
									break;
								}
							}
							if(argumentMatched) {
								break;
							}
						}
					}
				}
				if(additionallyMatchedStatements1.size() > 0) {
					return replacementInfo.getReplacements();
				}
			}
			if(creationCoveringTheEntireStatement1.equalArguments(invocationCoveringTheEntireStatement2) && creationCoveringTheEntireStatement1.arguments().size() > 0) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement2.arguments().size() == 1 && invocationCoveringTheEntireStatement2.arguments().contains(creationCoveringTheEntireStatement1.actualString())) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_WRAPPED_IN_METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(creationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null) {
			for(String key1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n")) {
						for(String key2 : methodInvocationMap2.keySet()) {
							for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
								if(statement2.getString().endsWith(key2 + ";\n")) {
									if(invocation2.getName().equals("of")) {
										String assignedVariable = null;
										if(assignmentCreationCoveringTheEntireStatement1 != null) {
											assignedVariable = statement1.getString().substring(0, statement1.getString().indexOf("="));
										}
										Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
										for(String argument2 : invocation2.arguments()) {
											for(AbstractCodeFragment fragment1 : replacementInfo.statements1) {
												AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
												if(invocation1 != null && invocation1.getExpression() != null && invocation1.getExpression().equals(assignedVariable)) {
													boolean argumentMatched = false;
													for(String argument1 : invocation1.arguments()) {
														if(argument1.equals(argument2)) {
															List<LeafExpression> leafExpressions1 = fragment1.findExpression(argument1);
															List<LeafExpression> leafExpressions2 = statement2.findExpression(argument2);
															if(leafExpressions1.size() == 1 && leafExpressions2.size() == 1) {
																LeafMapping mapping = createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, isEqualWithReplacement);
																addMapping(mapping);
																additionallyMatchedStatements1.add(fragment1);
															}
															argumentMatched = true;
															break;
														}
													}
													if(argumentMatched) {
														break;
													}
												}
											}
										}
										if(additionallyMatchedStatements1.size() > 0) {
											return replacementInfo.getReplacements();
										}
									}
									if(creation1.equalArguments(invocation2) && creation1.arguments().size() > 0) {
										Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.getName(),
												invocation2.getName(), (ObjectCreation)creation1, invocation2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
									else if(invocation2.arguments().size() == 1 && invocation2.arguments().contains(creation1.actualString())) {
										Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.getName(),
												invocation2.getName(), (ObjectCreation)creation1, invocation2, ReplacementType.CLASS_INSTANCE_CREATION_WRAPPED_IN_METHOD_INVOCATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == variableDeclarations2.size()) {
			if(invocationCoveringTheEntireStatement1.equalArguments(creationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.arguments().size() > 0) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement2.arguments().size() == 1 && creationCoveringTheEntireStatement2.arguments().contains(invocationCoveringTheEntireStatement1.actualString())) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(invocationCoveringTheEntireStatement1 == null && creationCoveringTheEntireStatement2 == null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n")) {
						for(String key2 : creationMap2.keySet()) {
							for(AbstractCall creation2 : creationMap2.get(key2)) {
								if(statement2.getString().endsWith(key2 + ";\n")) {
									if(invocation1.equalArguments(creation2) && invocation1.arguments().size() > 0) {
										Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocation1.getName(),
												creation2.getName(), invocation1, (ObjectCreation)creation2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
									else if(creation2.arguments().size() == 1 && creation2.arguments().contains(invocation1.actualString())) {
										Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocation1.getName(),
												creation2.getName(), invocation1, (ObjectCreation)creation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_CLASS_INSTANCE_CREATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			if(creationCoveringTheEntireStatement1.arguments().size() > 1 && creationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2).size() > 0 &&
					creationCoveringTheEntireStatement1.getCoverage().equals(invocationCoveringTheEntireStatement2.getCoverage()) &&
					creationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
			if(invocationCoveringTheEntireStatement1.arguments().size() > 1 && invocationCoveringTheEntireStatement1.argumentIntersection(creationCoveringTheEntireStatement2).size() > 0 &&
					invocationCoveringTheEntireStatement1.getCoverage().equals(creationCoveringTheEntireStatement2.getCoverage()) &&
					invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 instanceof OperationInvocation && invocationCoveringTheEntireStatement2 instanceof MethodReference) {
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_METHOD_REFERENCE);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(invocationCoveringTheEntireStatement1 instanceof MethodReference && invocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_METHOD_REFERENCE);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(classDiff != null && statement1.assignmentInvocationCoveringEntireStatement() != null && statement2.assignmentInvocationCoveringEntireStatement() != null) {
			String assignedVariable1 = statement1.getString().substring(0, statement1.getString().indexOf("="));
			String assignedVariable2 = statement2.getString().substring(0, statement2.getString().indexOf("="));
			if(assignedVariable1.equals(assignedVariable2)) {
				for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
					if(assignmentInvocationCoveringTheEntireStatement1.matchesOperation(removedOperation, container1, classDiff, modelDiff)) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if replacement is wrapped in creation within the first statement
		if(replacementInfo.getReplacements().size() == 1 && creations1.size() > 0) {
			for(String objectCreation1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(objectCreation1)) {
					for(Replacement replacement : replacementInfo.getReplacements()) {
						if(creation1.arguments().contains(replacement.getBefore())) {
							String creationAfterReplacement = ReplacementUtil.performArgumentReplacement(creation1.actualString(), replacement.getBefore(), replacement.getAfter());
							String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), creationAfterReplacement, replacement.getAfter());
							int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), replacementInfo.getRawDistance());
							if(distanceRaw == 0) {
								if(replacement instanceof MethodInvocationReplacement) {
									MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
									AbstractCall invokedOperationAfter = methodInvocationReplacement.getInvokedOperationAfter();
									r = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.actualString(), invokedOperationAfter.actualString(),
											(ObjectCreation)creation1, invokedOperationAfter, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
									replacementInfo.addReplacement(r);
									return replacementInfo.getReplacements();
								}
								else if(replacement instanceof VariableReplacementWithMethodInvocation) {
									VariableReplacementWithMethodInvocation methodInvocationReplacement = (VariableReplacementWithMethodInvocation)replacement;
									if(methodInvocationReplacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION)) {
										AbstractCall invokedOperationAfter = methodInvocationReplacement.getInvokedOperation();
										r = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.actualString(), invokedOperationAfter.actualString(),
												(ObjectCreation)creation1, invokedOperationAfter, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
										replacementInfo.addReplacement(r);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(variableDeclarations1.size() > 0  && variableDeclarations2.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
				invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null && parentMapper == null) {
			int variableDeclarationsInOtherStatements1 = 0;
			for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
				for(VariableDeclaration variableDeclaration : fragment1.getVariableDeclarations()) {
					if(variableDeclarations1.get(0).getScope().overlaps(variableDeclaration.getScope())) {
						variableDeclarationsInOtherStatements1++;
					}
				}
			}
			int variableDeclarationsInOtherStatements2 = 0;
			for(AbstractCodeFragment fragment2 : replacementInfo.getStatements2()) {
				for(VariableDeclaration variableDeclaration : fragment2.getVariableDeclarations()) {
					if(variableDeclarations2.get(0).getScope().overlaps(variableDeclaration.getScope())) {
						variableDeclarationsInOtherStatements2++;
					}
				}
			}
			if(variableDeclarationsInOtherStatements1 == 0 && variableDeclarationsInOtherStatements2 == 0) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			boolean variableDeclarationInitializer1 =  invocationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean variableDeclarationInitializer2 =  invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			if(variableDeclarationInitializer1 && variableDeclarationInitializer2) {
				if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					boolean statementIsExtracted = false;
					if(classDiff != null) {
						UMLOperation addedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), container2);
						statementIsExtracted = checkIfStatementIsExtracted(statement1, statement2, addedOperation);
						callToAddedOperation = addedOperation != null;
						callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), container1) != null;
					}
					if(callToAddedOperation != callToDeletedOperation && !statementIsExtracted) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
				else if(variableDeclarations1.get(0).getVariableName().equals(variableDeclarations2.get(0).getVariableName()) && replacementInfo.getReplacements(ReplacementType.TYPE).size() > 0) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					if(classDiff != null) {
						boolean superCall2 = invocationCoveringTheEntireStatement2.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression().equals("super");
						if(!superCall2) {
							callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getNextClass().getOperations(), container2) != null;
						}
						boolean superCall1 = invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement1.getExpression().equals("super");
						if(!superCall1) {
							callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getOriginalClass().getOperations(), container1) != null;
						}
					}
					if(callToAddedOperation != callToDeletedOperation) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		else if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			boolean variableDeclarationInitializer1 =  creationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean variableDeclarationInitializer2 =  invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			if(variableDeclarationInitializer1 && variableDeclarationInitializer2) {
				if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					if(classDiff != null) {
						boolean superCall2 = invocationCoveringTheEntireStatement2.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression().equals("super");
						if(!superCall2) {
							callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getNextClass().getOperations(), container2) != null;
						}
					}
					if(callToAddedOperation != callToDeletedOperation) {
						Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		else if(invocationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null && methodInvocationMap2.size() > 0) {
			if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0 &&
					variableDeclarations1.get(0).getInitializer() != null && variableDeclarations2.get(0).getInitializer() != null) {
				boolean callToAddedOperation = false;
				boolean statementIsExtracted = false;
				if(classDiff != null) {
					for(String key : methodInvocationMap2.keySet()) {
						AbstractCall call = methodInvocationMap2.get(key).get(0);
						UMLOperation addedOperation = classDiff.matchesOperation(call, classDiff.getAddedOperations(), container2);
						statementIsExtracted = checkIfStatementIsExtracted(statement1, statement2, addedOperation);
						callToAddedOperation = addedOperation != null;
						if(callToAddedOperation) {
							break;
						}
					}
				}
				if(callToAddedOperation && !statementIsExtracted) {
					Replacement replacement = new InitializerReplacement(variableDeclarations1.get(0).getInitializer(), variableDeclarations2.get(0).getInitializer());
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		boolean variableReturn1 = statement1.getVariables().size() > 0 && statement1.getString().equals("return " + statement1.getVariables().get(0).getString() + ";\n");
		boolean variableReturn2 = statement2.getVariables().size() > 0 && statement2.getString().equals("return " + statement2.getVariables().get(0).getString() + ";\n");
		if(parentMapper == null && !variableReturn1 && !variableReturn2 && statement1.getString().startsWith("return ") && statement2.getString().startsWith("return ") && statement1.isLastStatement() && statement2.isLastStatement() &&
				container1 instanceof UMLOperation && container2 instanceof UMLOperation && getOperation1().equalSignature(getOperation2()) && statement1.getLambdas().size() == statement2.getLambdas().size()) {
			boolean callToAddedOperation = false;
			boolean callToDeletedOperation = false;
			boolean isMovedMethod = !container1.getClassName().equals(container2.getClassName());
			if(classDiff != null) {
				if(!container1.getClassName().equals(container2.getClassName()) && modelDiff != null) {
					boolean pushDown = false;
					UMLClassBaseDiff container2Diff = modelDiff.getUMLClassDiff(container2.getClassName());
					if(container2Diff != null && container2Diff.getSuperclass() != null) {
						UMLType superclass = container2Diff.getSuperclass();
						if(container1.getClassName().endsWith("." + superclass.getClassType())) {
							pushDown = true;
						}
					}
					boolean pullUp = false;
					UMLClassBaseDiff container1Diff = modelDiff.getUMLClassDiff(container1.getClassName());
					if(container1Diff != null && container1Diff.getSuperclass() != null) {
						UMLType superclass = container1Diff.getSuperclass();
						if(container2.getClassName().endsWith("." + superclass.getClassType())) {
							pullUp = true;
						}
					}
					if(pullUp || pushDown) {
						isMovedMethod = false;
					}
				}
				if(invocationCoveringTheEntireStatement2 != null) {
					boolean superCall2 = invocationCoveringTheEntireStatement2.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression().equals("super");
					if(!superCall2) {
						UMLOperation addedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getNextClass().getOperations(), container2);
						callToAddedOperation = addedOperation != null && !addedOperation.equals(container2);
						if(callToAddedOperation == false) {
							if(invocationCoveringTheEntireStatement2.getExpression() != null) {
								List<AbstractCall> methodInvocations = methodInvocationMap2.get(invocationCoveringTheEntireStatement2.getExpression());
								if(methodInvocations != null) {
									for(AbstractCall invocation : methodInvocations) {
										if(classDiff.matchesOperation(invocation, classDiff.getNextClass().getOperations(), container2) != null) {
											callToAddedOperation = true;
											break;
										}
									}
								}
							}
						}
					}
				}
				if(invocationCoveringTheEntireStatement1 != null) {
					boolean superCall1 = invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement1.getExpression().equals("super");
					if(!superCall1) {
						UMLOperation removedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getOriginalClass().getOperations(), container1);
						callToDeletedOperation = removedOperation != null && !removedOperation.equals(container1);
						if(callToDeletedOperation == false) {
							if(invocationCoveringTheEntireStatement1.getExpression() != null) {
								List<AbstractCall> methodInvocations = methodInvocationMap1.get(invocationCoveringTheEntireStatement1.getExpression());
								if(methodInvocations != null) {
									for(AbstractCall invocation : methodInvocations) {
										if(classDiff.matchesOperation(invocation, classDiff.getOriginalClass().getOperations(), container1) != null) {
											callToDeletedOperation = true;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			if(callToAddedOperation == callToDeletedOperation && !isMovedMethod) {
				if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
					ReplacementType replacementType = !invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ? ReplacementType.METHOD_INVOCATION_NAME : ReplacementType.METHOD_INVOCATION;
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, replacementType);
					replacementInfo.addReplacement(replacement);
				}
				else if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
					Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							creationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
				}
				else if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
					Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
				}
				else if(methodInvocationMap1.size() > 0 && invocationCoveringTheEntireStatement2 != null) {
					AbstractCall invocation1 = null;
					for(String key : methodInvocationMap1.keySet()) {
						List<AbstractCall> calls = methodInvocationMap1.get(key);
						for(AbstractCall call : calls) {
							if(statement1.getString().endsWith(call.actualString() + ";\n")) {
								invocation1 = call;
								break;
							}
						}
						if(invocation1 != null) {
							break;
						}
					}
					if(invocation1 != null) {
						ReplacementType replacementType = !invocation1.identicalName(invocationCoveringTheEntireStatement2) ? ReplacementType.METHOD_INVOCATION_NAME : ReplacementType.METHOD_INVOCATION;
						Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocation1, invocationCoveringTheEntireStatement2, replacementType);
						replacementInfo.addReplacement(replacement);
					}
				}
				else if(methodInvocationMap2.size() > 0 && invocationCoveringTheEntireStatement1 != null) {
					AbstractCall invocation2 = null;
					for(String key : methodInvocationMap2.keySet()) {
						List<AbstractCall> calls = methodInvocationMap2.get(key);
						for(AbstractCall call : calls) {
							if(statement2.getString().endsWith(call.actualString() + ";\n")) {
								invocation2 = call;
								break;
							}
						}
						if(invocation2 != null) {
							break;
						}
					}
					if(invocation2 != null) {
						ReplacementType replacementType = !invocationCoveringTheEntireStatement1.identicalName(invocation2) ? ReplacementType.METHOD_INVOCATION_NAME : ReplacementType.METHOD_INVOCATION;
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocation2.actualString(), invocationCoveringTheEntireStatement1, invocation2, replacementType);
						replacementInfo.addReplacement(replacement);
					}
				}
				return replacementInfo.getReplacements();
			}
		}
		return null;
	}

	private boolean checkIfStatementIsExtracted(AbstractCodeFragment statement1, AbstractCodeFragment statement2, UMLOperation addedOperation) {
		if(classDiff != null) { 
			AbstractCall invocationCoveringTheEntireStatement2 = statement1.invocationCoveringEntireFragment();
			if(invocationCoveringTheEntireStatement2 != null) {
				if(addedOperation != null && addedOperation.getBody() != null) {
					for(AbstractCodeFragment fragment : addedOperation.getBody().getCompositeStatement().getLeaves()) {
						if(fragment.getString().equals(statement1.getString())) {
							return true;
						}
						if(fragment.getVariableDeclarations().size() > 0 && fragment.getVariableDeclarations().toString().equals(statement1.getVariableDeclarations().toString())) {
							return true;
						}
						if(statement1.getVariableDeclarations().size() > 0 && statement1.getVariableDeclarations().get(0).getInitializer() != null &&
								fragment.getString().equals("return " + statement1.getVariableDeclarations().get(0).getInitializer().getString() + ";\n") &&
								!fragment.getParent().equals(addedOperation.getBody().getCompositeStatement())) {
							return true;
						}
						for(AnonymousClassDeclarationObject anonymous : fragment.getAnonymousClassDeclarations()) {
							UMLAnonymousClass anonymousClass = addedOperation.findAnonymousClass(anonymous);
							if(anonymousClass != null) {
								for(UMLOperation anonymousOperation : anonymousClass.getOperations()) {
									for(AbstractCodeFragment anonymousFragment : anonymousOperation.getBody().getCompositeStatement().getLeaves()) {
										if(anonymousFragment.getString().equals(statement1.getString())) {
											return true;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static Map<String, List<AbstractCall>> convertToMap(List<AbstractCall> calls) {
		Map<String, List<AbstractCall>> methodInvocationMap = new LinkedHashMap<>();
		for(AbstractCall invocation : calls) {
			String methodInvocation = invocation.getString();
			if(methodInvocationMap.containsKey(methodInvocation)) {
				methodInvocationMap.get(methodInvocation).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				methodInvocationMap.put(methodInvocation, list);
			}
		}
		return methodInvocationMap;
	}

	private static Set<String> convertToStringSet(List<? extends LeafExpression> expressions) {
		Set<String> set = new LinkedHashSet<>();
		for(LeafExpression expression : expressions) {
			set.add(expression.getString());
		}
		return set;
	}

	private boolean bothContainNullInDifferentIndexes(AbstractCall call1, AbstractCall call2) {
		if(call1 != null && call2 != null && call1.arguments().contains("null") && call2.arguments().contains("null")) {
			int index1 = call1.arguments().indexOf("null");
			int index2 = call2.arguments().indexOf("null");
			return index1 != index2;
		}
		return false;
	}

	private boolean matchingArgument(Set<String> variables1, Set<String> literals2, AbstractCall call1, AbstractCall call2) {
		if(call1 != null && call2 != null && call1.arguments().size() == call2.arguments().size()) {
			for(int i=0; i<call1.arguments().size(); i++) {
				String arg1 = call1.arguments().get(i);
				String arg2 = call2.arguments().get(i);
				if(variables1.contains(arg1) && literals2.contains(arg2)) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<AbstractCodeFragment> additionallyMatchedStatements(List<VariableDeclaration> variableDeclarations, List<? extends AbstractCodeFragment> unmatchedStatements) {
		Set<AbstractCodeFragment> additionallyMatchedStatements = new LinkedHashSet<AbstractCodeFragment>();
		if(!variableDeclarations.isEmpty()) {
			for(AbstractCodeFragment codeFragment : unmatchedStatements) {
				for(VariableDeclaration variableDeclaration : codeFragment.getVariableDeclarations()) {
					if(variableDeclaration.getVariableName().equals(variableDeclarations.get(0).getVariableName()) &&
							variableDeclaration.equalType(variableDeclarations.get(0))) {
						additionallyMatchedStatements.add(codeFragment);
						break;
					}
				}
			}
		}
		return additionallyMatchedStatements;
	}

	private Set<Replacement> processAnonymousAndLambdas(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo,
			AbstractCall invocationCoveringTheEntireStatement1,
			AbstractCall invocationCoveringTheEntireStatement2,
			Map<String, List<AbstractCall>> methodInvocationMap1,
			Map<String, List<AbstractCall>> methodInvocationMap2,
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations1,
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations2,
			List<LambdaExpressionObject> lambdas1,
			List<LambdaExpressionObject> lambdas2,
			List<UMLOperationBodyMapper> lambdaMappers)
			throws RefactoringMinerTimedOutException {
		boolean replacementAdded = false;
		if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty() && container1 != null && container2 != null) {
			if(anonymousClassDeclarations1.size() == anonymousClassDeclarations2.size()) {
				for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
					AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
					AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(i);
					UMLAnonymousClass anonymousClass1 = findAnonymousClass1(anonymousClassDeclaration1);
					UMLAnonymousClass anonymousClass2 = findAnonymousClass2(anonymousClassDeclaration2);
					boolean anonymousClassDiffFound = false;
					for(UMLAnonymousClassDiff anonymousClassDiff : this.anonymousClassDiffs) {
						if(anonymousClassDiff.getOriginalClass().equals(anonymousClass1) || anonymousClassDiff.getNextClass().equals(anonymousClass2)) {
							anonymousClassDiffFound = true;
							break;
						}
					}
					if(!anonymousClassDiffFound) {
						replacementAdded = processAnonymous(statement1, statement2, parameterToArgumentMap, replacementInfo,
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2,
								replacementAdded, anonymousClassDeclaration1, anonymousClassDeclaration2, lambdaMappers);
					}
				}
			}
			else {
				for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
					for(int j=0; j<anonymousClassDeclarations2.size(); j++) {
						AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
						AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(j);
						replacementAdded = processAnonymous(statement1, statement2, parameterToArgumentMap, replacementInfo,
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2,
								replacementAdded, anonymousClassDeclaration1, anonymousClassDeclaration2, lambdaMappers);
					}
				}
			}
		}
		else if(anonymousClassDeclarations1.size() == 0 && anonymousClassDeclarations2.size() == 1 && container2 != null && parentMapper == null) {
			AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(0);
			UMLAnonymousClass anonymousClass2 = findAnonymousClass2(anonymousClassDeclaration2);
			if(anonymousClass2.getOperations().size() == 1) {
				UMLOperation anonymousClass2Operation = anonymousClass2.getOperations().get(0);
				if(anonymousClass2Operation.getBody() != null) {
					List<AbstractStatement> statements = anonymousClass2Operation.getBody().getCompositeStatement().getStatements();
					if(statements.size() == 1) {
						AbstractStatement statement = statements.get(0);
						AbstractCall invocation2 = statement.invocationCoveringEntireFragment();
						if(invocation2 != null) {
							for(String key1 : methodInvocationMap1.keySet()) {
								for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
									if(invocation1.identical(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, Collections.emptyList())) {
										Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
												invocation2.actualString(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_ANONYMOUS_CLASS_DECLARATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		else if(anonymousClassDeclarations1.size() == 1 && anonymousClassDeclarations2.size() == 0 && container1 != null && parentMapper == null) {
			AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(0);
			UMLAnonymousClass anonymousClass1 = findAnonymousClass1(anonymousClassDeclaration1);
			if(anonymousClass1.getOperations().size() == 1) {
				UMLOperation anonymousClass1Operation = anonymousClass1.getOperations().get(0);
				if(anonymousClass1Operation.getBody() != null) {
					List<AbstractCodeFragment> statements = anonymousClass1Operation.getBody().getCompositeStatement().getLeaves();
					for(AbstractCodeFragment statement : statements) {
						AbstractCall invocation1 = statement.invocationCoveringEntireFragment();
						if(invocation1 != null) {
							for(String key2 : methodInvocationMap2.keySet()) {
								for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
									if(invocation1.identical(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, Collections.emptyList()) ||
											invocation1.identicalWithInlinedStatements(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, statements)) {
										Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
												invocation2.actualString(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_ANONYMOUS_CLASS_DECLARATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(!lambdas1.isEmpty() && !lambdas2.isEmpty()) {
			if(lambdas1.size() == lambdas2.size()) {
				for(int i=0; i<lambdas1.size(); i++) {
					LambdaExpressionObject lambda1 = lambdas1.get(i);
					LambdaExpressionObject lambda2 = lambdas2.get(i);
					processLambdas(lambda1, lambda2, lambdaMappers);
				}
			}
			else {
				for(int i=0; i<lambdas1.size(); i++) {
					for(int j=0; j<lambdas2.size(); j++) {
						LambdaExpressionObject lambda1 = lambdas1.get(i);
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						processLambdas(lambda1, lambda2, lambdaMappers);
					}
				}
			}
		}
		if(anonymousClassDeclarations1.size() >= 1 && container1 != null && lambdas2.size() >= 1) {
			for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
				AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
				UMLAnonymousClass anonymousClass1 = findAnonymousClass1(anonymousClassDeclaration1);
				if(anonymousClass1.getOperations().size() == 1) {
					UMLOperation anonymousClass1Operation = anonymousClass1.getOperations().get(0);
					for(int j=0; j<lambdas2.size(); j++) {
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(anonymousClass1Operation, lambda2, this);
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
									ReplaceAnonymousWithLambdaRefactoring ref = new ReplaceAnonymousWithLambdaRefactoring(anonymousClass1, lambda2, container1, container2);
									this.refactorings.add(ref);
									this.refactorings.addAll(mapper.getRefactorings());
								}
								Replacement replacement = new Replacement(anonymousClassDeclaration1.toString(), lambda2.toString(), ReplacementType.ANONYMOUS_CLASS_DECLARATION_REPLACED_WITH_LAMBDA);
								replacementInfo.addReplacement(replacement);
								replacementAdded = true;
								lambdaMappers.add(mapper);
							}
						}
					}
				}
			}
		}
		if(replacementAdded) {
			return replacementInfo.getReplacements();
		}
		if(lambdaMappers.size() > 0 && lambdaMappers.size() == lambdas1.size() && lambdaMappers.size() == lambdas2.size()) {
			return replacementInfo.getReplacements();
		}
		return null;
	}

	private void processLambdas(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2,
			List<UMLOperationBodyMapper> lambdaMappers) throws RefactoringMinerTimedOutException {
		UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(lambda1, lambda2, this);
		int mappings = mapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int nonMappedElementsT1 = mapper.nonMappedElementsT1();
			int nonMappedElementsT2 = mapper.nonMappedElementsT2();
			List<AbstractCall> invocations1 = streamAPICalls(mapper.getNonMappedLeavesT1());
			List<AbstractCall> invocations2 = streamAPICalls(mapper.getNonMappedLeavesT2());
			if(invocations1.size() > 0 && invocations2.size() == 0) {
				nonMappedElementsT2 = nonMappedElementsT2 - ignoredNonMappedElements(invocations1, mapper.getNonMappedLeavesT2(), mapper.getNonMappedInnerNodesT2());
			}
			else if(invocations1.size() == 0 && invocations2.size() > 0) {
				nonMappedElementsT1 = nonMappedElementsT1 - ignoredNonMappedElements(invocations2, mapper.getNonMappedLeavesT1(), mapper.getNonMappedInnerNodesT1());
			}
			if((mappings >= nonMappedElementsT1 && mappings >= nonMappedElementsT2) ||
					nonMappedElementsT1 == 0 || nonMappedElementsT2 == 0 ||
					(classDiff != null && (classDiff.isPartOfMethodExtracted(lambda1, lambda2) || classDiff.isPartOfMethodInlined(lambda1, lambda2)))) {
				addAllMappings(mapper.mappings);
				this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
				this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
				this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
				this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
				if(this.container1 != null && this.container2 != null) {
					this.refactorings.addAll(mapper.getRefactorings());
				}
				lambdaMappers.add(mapper);
			}
		}
	}

	private boolean processAnonymous(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo,
			AbstractCall invocationCoveringTheEntireStatement1, AbstractCall invocationCoveringTheEntireStatement2,
			boolean replacementAdded, AnonymousClassDeclarationObject anonymousClassDeclaration1,
			AnonymousClassDeclarationObject anonymousClassDeclaration2, List<UMLOperationBodyMapper> lambdaMappers) throws RefactoringMinerTimedOutException {
		String statementWithoutAnonymous1 = statementWithoutAnonymous(statement1, anonymousClassDeclaration1, container1);
		String statementWithoutAnonymous2 = statementWithoutAnonymous(statement2, anonymousClassDeclaration2, container2);
		if(replacementInfo.getRawDistance() == 0 || statementWithoutAnonymous1.equals(statementWithoutAnonymous2) ||
				identicalAfterVariableAndTypeReplacements(statementWithoutAnonymous1, statementWithoutAnonymous2, replacementInfo.getReplacements()) ||
				(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				(onlyDifferentInvoker(statementWithoutAnonymous1, statementWithoutAnonymous2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
				invocationCoveringTheEntireStatement1.identical(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, lambdaMappers) ||
				invocationCoveringTheEntireStatement1.identicalWithOnlyChangesInAnonymousClassArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) ||
				invocationCoveringTheEntireStatement1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) ||
				invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) ||
				invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2()) != null ||
				(invocationCoveringTheEntireStatement1 instanceof ObjectCreation && invocationCoveringTheEntireStatement2 instanceof ObjectCreation && invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2))))) {
			UMLAnonymousClass anonymousClass1 = findAnonymousClass1(anonymousClassDeclaration1);
			UMLAnonymousClass anonymousClass2 = findAnonymousClass2(anonymousClassDeclaration2);
			UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(anonymousClass1, anonymousClass2, classDiff, modelDiff);
			anonymousClassDiff.process();
			List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
			if(matchedOperationMappers.size() > 0) {
				for(UMLOperationBodyMapper mapper : matchedOperationMappers) {
					addAllMappings(mapper.mappings);
					this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
					this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
					this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
					this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
				}
				List<Refactoring> anonymousClassDiffRefactorings = anonymousClassDiff.getRefactorings();
				for(Refactoring r : anonymousClassDiffRefactorings) {
					if(r instanceof ExtractOperationRefactoring) {
						UMLOperationBodyMapper childMapper = ((ExtractOperationRefactoring)r).getBodyMapper();
						addAllMappings(childMapper.mappings);
					}
				}
				this.refactorings.addAll(anonymousClassDiffRefactorings);
				this.anonymousClassDiffs.add(anonymousClassDiff);
				if(classDiff != null && classDiff.getRemovedAnonymousClasses().contains(anonymousClass1)) {
					classDiff.getRemovedAnonymousClasses().remove(anonymousClass1);
				}
				if(classDiff != null && classDiff.getAddedAnonymousClasses().contains(anonymousClass2)) {
					classDiff.getAddedAnonymousClasses().remove(anonymousClass2);
				}
				if(!anonymousClassDeclaration1.toString().equals(anonymousClassDeclaration2.toString())) {
					Replacement replacement = new Replacement(anonymousClassDeclaration1.toString(), anonymousClassDeclaration2.toString(), ReplacementType.ANONYMOUS_CLASS_DECLARATION);
					replacementInfo.addReplacement(replacement);
				}
				replacementAdded = true;
			}
		}
		return replacementAdded;
	}

	private UMLAnonymousClass findAnonymousClass1(AnonymousClassDeclarationObject anonymousClassDeclaration1) {
		UMLAnonymousClass anonymousClass1 = container1.findAnonymousClass(anonymousClassDeclaration1);
		if(anonymousClass1 == null && parentMapper != null) {
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				anonymousClass1 = childMapper.container1.findAnonymousClass(anonymousClassDeclaration1);
				if(anonymousClass1 != null) {
					break;
				}
			}
		}
		return anonymousClass1;
	}

	private UMLAnonymousClass findAnonymousClass2(AnonymousClassDeclarationObject anonymousClassDeclaration2) {
		UMLAnonymousClass anonymousClass2 = container2.findAnonymousClass(anonymousClassDeclaration2);
		if(anonymousClass2 == null && parentMapper != null) {
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				anonymousClass2 = childMapper.container2.findAnonymousClass(anonymousClassDeclaration2);
				if(anonymousClass2 != null) {
					break;
				}
			}
		}
		return anonymousClass2;
	}

	private int ignoredNonMappedElements(List<AbstractCall> invocations, List<AbstractCodeFragment> nonMappedLeaves, List<CompositeStatementObject> nonMappedInnerNodes) {
		int counter = 0;
		for(AbstractCall inv : invocations) {
			if(inv.getName().equals("forEach")) {
				for(CompositeStatementObject comp : nonMappedInnerNodes) {
					if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
						counter++;
					}
				}
				for(AbstractCodeFragment statement : nonMappedLeaves) {
					for(AbstractCall statementInvocation : statement.getMethodInvocations()) {
						if(statementInvocation.getName().equals("iterator") || statementInvocation.getName().equals("next")) {
							counter++;
						}
					}
					for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
						if(declaration.getInitializer() != null) {
							for(LeafExpression numberLiteral : declaration.getInitializer().getNumberLiterals()) {
								if(numberLiteral.getString().equals(declaration.getInitializer().getExpression())) {
									counter++;
									break;
								}
							}
						}
					}
				}
			}
			else if(inv.getName().equals("filter")) {
				for(CompositeStatementObject comp : nonMappedInnerNodes) {
					if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						counter++;
					}
				}
			}
		}
		return counter;
	}

	private boolean matchAsLambdaExpressionArgument(String s1, String s2, Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo, AbstractCodeFragment statement1) {
		if(parentMapper != null && s2.contains(" -> ")) {
			for(String parameterName : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameterName);
				if(!parameterName.equals(argument) && !argument.isEmpty() && s2.contains(argument)) {
					for(VariableDeclaration parameter : container2.getParameterDeclarationList()) {
						if(parameterName.equals(parameter.getVariableName())) {
							String lambdaArrow = "() -> ";
							String supplierGet = ".get()";
							UMLType parameterType = parameter.getType();
							if(parameterType != null && parameterType.getClassType().equals("Supplier") && s2.contains(supplierGet) && s2.contains(lambdaArrow)) {
								String tmp = s2.replace(supplierGet, "");
								tmp = tmp.replace(lambdaArrow, "");
								if(s1.equals(tmp)) {
									for(AbstractCodeFragment nonMappedLeafT2 : parentMapper.getNonMappedLeavesT2()) {
										List<AbstractCall> methodInvocations = nonMappedLeafT2.getMethodInvocations();
										if(methodInvocations.contains(this.operationInvocation)) {
											List<LambdaExpressionObject> lambdas = nonMappedLeafT2.getLambdas();
											for(LambdaExpressionObject lambda : lambdas) {
												AbstractExpression lambdaExpression = lambda.getExpression();
												if(lambdaExpression != null && lambda.toString().equals(argument)) {
													List<VariableDeclaration> variableDeclarations = statement1.getVariableDeclarations();
													for(VariableDeclaration variableDeclaration : variableDeclarations) {
														AbstractExpression initializer = variableDeclaration.getInitializer();
														if(initializer != null && initializer.getString().equals(lambdaExpression.getString())) {
															LeafMapping mapping = createLeafMapping(initializer, lambdaExpression, parameterToArgumentMap, false);
															addMapping(mapping);
															String before = argument.substring(lambdaArrow.length());
															String after = parameterName + supplierGet;
															Replacement r = new Replacement(before, after, ReplacementType.LAMBDA_EXPRESSION_ARGUMENT);
															replacementInfo.addReplacement(r);
															return true;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isExpressionOfAnotherMethodInvocation(AbstractCall invocation, Map<String, List<AbstractCall>> invocationMap) {
		for(String key : invocationMap.keySet()) {
			List<? extends AbstractCall> invocations = invocationMap.get(key);
			for(AbstractCall call : invocations) {
				if(!call.equals(invocation) && call.getExpression() != null && call.getExpression().equals(invocation.actualString())) {
					for(String argument : call.arguments()) {
						if(invocationMap.containsKey(argument)) {
							List<? extends AbstractCall> argumentInvocations = invocationMap.get(argument);
							for(AbstractCall argumentCall : argumentInvocations) {
								if(argumentCall.identicalName(invocation) && argumentCall.equalArguments(invocation)) {
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

	private void removeCommonElements(Set<String> strings1, Set<String> strings2) {
		Set<String> intersection = new LinkedHashSet<String>(strings1);
		intersection.retainAll(strings2);
		strings1.removeAll(intersection);
		strings2.removeAll(intersection);
	}

	private void removeCommonTypes(Set<String> strings1, Set<String> strings2, List<String> types1, List<String> types2) {
		if(types1.size() == types2.size()) {
			Set<String> removeFromIntersection = new LinkedHashSet<String>();
			for(int i=0; i<types1.size(); i++) {
				String type1 = types1.get(i);
				String type2 = types2.get(i);
				if(!type1.equals(type2)) {
					removeFromIntersection.add(type1);
					removeFromIntersection.add(type2);
				}
			}
			Set<String> intersection = new LinkedHashSet<String>(strings1);
			intersection.retainAll(strings2);
			intersection.removeAll(removeFromIntersection);
			strings1.removeAll(intersection);
			strings2.removeAll(intersection);
		}
		else {
			removeCommonElements(strings1, strings2);
		}
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

	private void replaceVariablesWithArguments(Set<String> variables, Map<String, String> parameterToArgumentMap) {
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(variables.contains(parameter)) {
				if(!StringDistance.isNumeric(argument)) {
					variables.add(argument);
				}
				if(argument.contains("(") && argument.contains(")")) {
					int indexOfOpeningParenthesis = argument.indexOf("(");
					int indexOfClosingParenthesis = argument.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(argument, indexOfOpeningParenthesis);
					boolean closingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(argument, indexOfClosingParenthesis);
					boolean openingParenthesisInsideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(argument, indexOfOpeningParenthesis);
					boolean closingParenthesisIndideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(argument, indexOfClosingParenthesis);
					if(indexOfOpeningParenthesis < indexOfClosingParenthesis &&
							!openingParenthesisInsideSingleQuotes && !closingParenthesisInsideSingleQuotes &&
							!openingParenthesisInsideDoubleQuotes && !closingParenthesisIndideDoubleQuotes) {
						String arguments = argument.substring(indexOfOpeningParenthesis+1, indexOfClosingParenthesis);
						if(!arguments.isEmpty() && !arguments.contains(",") && !arguments.contains("(") && !arguments.contains(")") && !StringDistance.isNumeric(arguments)) {
							variables.add(arguments);
						}
					}
				}
			}
		}
	}

	private boolean isCallChain(Collection<List<AbstractCall>> calls) {
		if(calls.size() > 1) {
			AbstractCall previous = null;
			AbstractCall current = null;
			int chainLength = 0;
			for(List<? extends AbstractCall> list : calls) {
				previous = current;
				current = list.get(0);
				if(current != null && previous != null) {
					if(previous.getExpression() != null && previous.getExpression().equals(current.actualString())) {
						chainLength++;
					}
					else {
						return false;
					}
				}
			}
			if(chainLength == calls.size()-1) {
				return true;
			}
		}
		return false;
	}

	private void replaceVariablesWithArguments(Map<String, List<AbstractCall>> callMap,
			Set<String> calls, Map<String, String> parameterToArgumentMap) {
		if(isCallChain(callMap.values())) {
			for(String parameter : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameter);
				if(!parameter.equals(argument)) {
					Set<String> toBeAdded = new LinkedHashSet<String>();
					for(String call : calls) {
						String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
						if(!call.equals(afterReplacement)) {
							toBeAdded.add(afterReplacement);
							List<? extends AbstractCall> oldCalls = callMap.get(call);
							List<AbstractCall> newCalls = new ArrayList<AbstractCall>();
							for(AbstractCall oldCall : oldCalls) {
								AbstractCall newCall = oldCall.update(parameter, argument);
								newCalls.add(newCall);
							}
							callMap.put(afterReplacement, newCalls);
						}
					}
					calls.addAll(toBeAdded);
				}
			}
		}
		else {
			Set<String> finalNewCalls = new LinkedHashSet<String>();
			for(String parameter : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameter);
				if(!parameter.equals(argument)) {
					Set<String> toBeAdded = new LinkedHashSet<String>();
					for(String call : calls) {
						String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
						if(!call.equals(afterReplacement)) {
							toBeAdded.add(afterReplacement);
							List<? extends AbstractCall> oldCalls = callMap.get(call);
							List<AbstractCall> newCalls = new ArrayList<AbstractCall>();
							for(AbstractCall oldCall : oldCalls) {
								AbstractCall newCall = oldCall.update(parameter, argument);
								newCalls.add(newCall);
							}
							callMap.put(afterReplacement, newCalls);
						}
					}
					finalNewCalls.addAll(toBeAdded);
				}
			}
			calls.addAll(finalNewCalls);
		}
	}

	private void findReplacements(Set<String> strings1, Set<String> strings2, ReplacementInfo replacementInfo, ReplacementType type) throws RefactoringMinerTimedOutException {
		if(strings1.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS || strings2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS ||
				strings1.size()*strings2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS*10) {
			return;
		}
		TreeMap<Double, Set<Replacement>> globalReplacementMap = new TreeMap<Double, Set<Replacement>>();
		TreeMap<Double, Set<Replacement>> replacementCache = new TreeMap<Double, Set<Replacement>>();
		if(strings1.size() <= strings2.size()) {
			for(String s1 : strings1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				for(String s2 : strings2) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
					boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
					if(containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
							container1 != null && container2 != null &&
							container1.getVariableDeclaration(s1) == null && container2.getVariableDeclaration(s2) == null &&
							classDiff != null && !classDiff.getOriginalClass().containsAttributeWithName(s1) && !classDiff.getNextClass().containsAttributeWithName(s2)) {
						continue;
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						Replacement replacement = new Replacement(s1, s2, type);
						double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
						replacementMap.put(distancenormalized, replacement);
						if(replacementCache.containsKey(distancenormalized)) {
							replacementCache.get(distancenormalized).add(replacement);
						}
						else {
							Set<Replacement> r = new LinkedHashSet<Replacement>();
							r.add(replacement);
							replacementCache.put(distancenormalized, r);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Double distancenormalized = replacementMap.firstEntry().getKey();
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(globalReplacementMap.containsKey(distancenormalized)) {
						globalReplacementMap.get(distancenormalized).add(replacement);
					}
					else {
						Set<Replacement> r = new LinkedHashSet<Replacement>();
						r.add(replacement);
						globalReplacementMap.put(distancenormalized, r);
					}
					if(distancenormalized == 0) {
						break;
					}
				}
			}
		}
		else {
			for(String s2 : strings2) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				for(String s1 : strings1) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
					boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
					if(containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
							container1 != null && container2 != null &&
							container1.getVariableDeclaration(s1) == null && container2.getVariableDeclaration(s2) == null &&
							classDiff != null && !classDiff.getOriginalClass().containsAttributeWithName(s1) && !classDiff.getNextClass().containsAttributeWithName(s2)) {
						continue;
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						Replacement replacement = new Replacement(s1, s2, type);
						double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
						replacementMap.put(distancenormalized, replacement);
						if(replacementCache.containsKey(distancenormalized)) {
							replacementCache.get(distancenormalized).add(replacement);
						}
						else {
							Set<Replacement> r = new LinkedHashSet<Replacement>();
							r.add(replacement);
							replacementCache.put(distancenormalized, r);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Double distancenormalized = replacementMap.firstEntry().getKey();
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(globalReplacementMap.containsKey(distancenormalized)) {
						globalReplacementMap.get(distancenormalized).add(replacement);
					}
					else {
						Set<Replacement> r = new LinkedHashSet<Replacement>();
						r.add(replacement);
						globalReplacementMap.put(distancenormalized, r);
					}
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		if(!globalReplacementMap.isEmpty()) {
			Double distancenormalized = globalReplacementMap.firstEntry().getKey();
			if(distancenormalized == 0) {
				Set<Replacement> replacements = globalReplacementMap.firstEntry().getValue();
				for(Replacement replacement : replacements) {
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
				}
			}
			else {
				Set<Replacement> conflictingReplacements = conflictingReplacements(globalReplacementMap);
				Set<String> processedBefores = new LinkedHashSet<String>();
				for(Set<Replacement> replacements : globalReplacementMap.values()) {
					for(Replacement replacement : replacements) {
						if(!conflictingReplacements.contains(replacement)) {
							if(!processedBefores.contains(replacement.getBefore())) {
								replacementInfo.addReplacement(replacement);
								replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
								processedBefores.add(replacement.getBefore());
							}
							else {
								//find the next best match for replacement.getAfter() from the replacement cache
								for(Set<Replacement> replacements2 : replacementCache.values()) {
									boolean found = false;
									for(Replacement replacement2 : replacements2) {
										if(replacement2.getAfter().equals(replacement.getAfter()) && !replacement2.equals(replacement)) {
											replacementInfo.addReplacement(replacement2);
											replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement2.getBefore(), replacement2.getAfter()));
											processedBefores.add(replacement2.getBefore());
											found = true;
											break;
										}
									}
									if(found) {
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

	private Set<Replacement> conflictingReplacements(TreeMap<Double, Set<Replacement>> globalReplacementMap) {
		Map<String, Set<Replacement>> map = new LinkedHashMap<String, Set<Replacement>>();
		for(Set<Replacement> replacements : globalReplacementMap.values()) {
			for(Replacement replacement : replacements) {
				String after = replacement.getAfter();
				if(map.containsKey(after)) {
					map.get(after).add(replacement);
				}
				else {
					Set<Replacement> set = new LinkedHashSet<Replacement>();
					set.add(replacement);
					map.put(after, set);
				}
			}
		}
		Set<Replacement> conflictingReplacements = new LinkedHashSet<Replacement>();
		for(String key : map.keySet()) {
			Set<Replacement> replacements = map.get(key);
			if(replacements.size() > 1) {
				conflictingReplacements.add(replacements.iterator().next());
			}
		}
		return conflictingReplacements;
	}

	private Set<Replacement> variableReplacementWithinMethodInvocations(String s1, String s2, Set<String> variables1, Set<String> variables2) {
		Set<Replacement> tempReplacements = new LinkedHashSet<Replacement>();
		for(String variable1 : variables1) {
			String originalVariable1 = variable1;
			if(parameterToArgumentMap1 != null && parameterToArgumentMap1.containsKey(variable1) && !parameterToArgumentMap1.get(variable1).equals(variable1)) {
				variable1 = parameterToArgumentMap1.get(variable1);
			}
			if((ReplacementUtil.contains(s1, variable1) || s1.endsWith(variable1)) && !s1.equals(variable1) && !s1.equals("this." + variable1) && !s1.equals("_" + variable1)) {
				int startIndex1 = s1.indexOf(variable1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + variable1.length(), s1.length());
				for(String variable2 : variables2) {
					if(variable2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						variable2 = variable2.substring(0, variable2.indexOf(substringAfterIndex1));
					}
					if((ReplacementUtil.contains(s2, variable2) || s2.endsWith(variable2)) && !s2.equals(variable2)) {
						int startIndex2 = s2.indexOf(variable2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + variable2.length(), s2.length());
						boolean suffixMatch = substringAfterIndex1.equals(substringAfterIndex2) && !substringAfterIndex1.isEmpty();
						if(!suffixMatch && !tempReplacements.isEmpty() && !substringAfterIndex1.isEmpty()) {
							for(Replacement r : tempReplacements) {
								String tmp1 = substringAfterIndex1.replace(r.getBefore(), r.getAfter());
								if(tmp1.equals(substringAfterIndex2)) {
									suffixMatch = true;
									break;
								}
							}
						}
						boolean prefixMatch = substringBeforeIndex1.equals(substringBeforeIndex2) && !substringBeforeIndex1.isEmpty();
						if(!prefixMatch && !tempReplacements.isEmpty() && !substringBeforeIndex1.isEmpty()) {
							for(Replacement r : tempReplacements) {
								String tmp1 = substringBeforeIndex1.replace(r.getBefore(), r.getAfter());
								if(tmp1.equals(substringBeforeIndex2)) {
									prefixMatch = true;
									break;
								}
							}
						}
						if(prefixMatch || suffixMatch) {
							Replacement r = new Replacement(originalVariable1, variable2, ReplacementType.VARIABLE_NAME);
							tempReplacements.add(r);
						}
					}
				}
			}
		}
		String tmp1 = new String(s1);
		Set<Replacement> finalReplacements = new LinkedHashSet<Replacement>();
		for(Replacement replacement : tempReplacements) {
			tmp1 = ReplacementUtil.performReplacement(tmp1, s2, replacement.getBefore(), replacement.getAfter());
			finalReplacements.add(replacement);
			if(tmp1.equals(s2)) {
				return finalReplacements;
			}
		}
		if(tmp1.equals(s2)) {
			return finalReplacements;
		}
		else {
			return Collections.emptySet();
		}
	}

	private Map<Replacement, Boolean> replacementsWithinMethodInvocations(String s1, String s2, Set<String> set1, Set<String> set2, Map<String, List<AbstractCall>> methodInvocationMap, Direction direction) {
		Map<Replacement, Boolean> replacements = new LinkedHashMap<Replacement, Boolean>();
		for(String element1 : set1) {
			if(s1.contains(element1) && !s1.equals(element1) && !s1.equals("this." + element1) && !s1.equals("_" + element1)) {
				int startIndex1 = s1.indexOf(element1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + element1.length(), s1.length());
				for(String element2 : set2) {
					if(element2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						element2 = element2.substring(0, element2.indexOf(substringAfterIndex1));
					}
					if(s2.contains(element2) && !s2.equals(element2) && !element1.equals(element2)) {
						int startIndex2 = s2.indexOf(element2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + element2.length(), s2.length());
						List<? extends AbstractCall> methodInvocationList = null;
						if(direction.equals(Direction.VARIABLE_TO_INVOCATION))
							methodInvocationList = methodInvocationMap.get(element2);
						else if(direction.equals(Direction.INVOCATION_TO_VARIABLE))
							methodInvocationList = methodInvocationMap.get(element1);
						if(substringBeforeIndex1.equals(substringBeforeIndex2) && !substringAfterIndex1.isEmpty() && !substringAfterIndex2.isEmpty() && methodInvocationList != null) {
							boolean skip = false;
							if(substringAfterIndex1.length() > substringAfterIndex2.length()) {
								skip = s2.contains(substringAfterIndex1);
							}
							else if(substringAfterIndex1.length() < substringAfterIndex2.length()) {
								skip = s1.contains(substringAfterIndex2);
							}
							if(!skip) {
								Replacement r = new VariableReplacementWithMethodInvocation(element1, element2, methodInvocationList.get(0), direction);
								replacements.put(r, substringAfterIndex1.equals(substringAfterIndex2));
							}
						}
						else if(substringAfterIndex1.equals(substringAfterIndex2) && !substringBeforeIndex1.isEmpty() && !substringBeforeIndex2.isEmpty() && methodInvocationList != null) {
							boolean skip = false;
							if(substringBeforeIndex1.length() > substringBeforeIndex2.length()) {
								skip = s2.contains(substringBeforeIndex1);
							}
							else if(substringBeforeIndex1.length() < substringBeforeIndex2.length()) {
								skip = s1.contains(substringBeforeIndex2);
							}
							if(!skip) {
								Replacement r = new VariableReplacementWithMethodInvocation(element1, element2, methodInvocationList.get(0), direction);
								replacements.put(r, substringBeforeIndex1.equals(substringBeforeIndex2));
							}
						}
					}
				}
			}
		}
		return replacements;
	}

	private boolean variablesStartWithSameCase(String s1, String s2, Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo) {
		if(parameterToArgumentMap.values().contains(s2)) {
			return true;
		}
		if(s1.length() > 0 && s2.length() > 0) {
			if(Character.isUpperCase(s1.charAt(0)) && Character.isUpperCase(s2.charAt(0)))
				return true;
			if(Character.isLowerCase(s1.charAt(0)) && Character.isLowerCase(s2.charAt(0)))
				return true;
			if(s1.charAt(0) == '_' || s2.charAt(0) == '_')
				return true;
			if(s1.charAt(0) == '(' || s2.charAt(0) == '(')
				return true;
			if((s1.contains(".") || s2.contains(".")) && !replacementInfo.argumentizedString1.equals("return " + s1 + ";\n") &&
					!replacementInfo.argumentizedString2.equals("return " + s2 + ";\n"))
				return true;
			if(s1.equalsIgnoreCase(s2))
				return true;
			if(replacementInfo.argumentizedString1.startsWith(s1 + "=") && replacementInfo.argumentizedString2.startsWith(s2 + "=")) {
				String suffix1 = replacementInfo.argumentizedString1.substring(s1.length(), replacementInfo.argumentizedString1.length());
				String suffix2 = replacementInfo.argumentizedString2.substring(s2.length(), replacementInfo.argumentizedString2.length());
				if(suffix1.equals(suffix2)) {
					return true;
				}
			}
		}
		return false;
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
		return container1.toString() + " -> " + container2.toString();
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
		if(thisMappings != otherMappings) {
			return -Integer.compare(thisMappings, otherMappings);
		}
		else {
			int thisExactMatches = this.exactMatches();
			int otherExactMatches = operationBodyMapper.exactMatches();
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
					int thisEditDistance = this.editDistance();
					int otherEditDistance = operationBodyMapper.editDistance();
					if(thisEditDistance != otherEditDistance) {
						return Integer.compare(thisEditDistance, otherEditDistance);
					}
					else {
						int thisOperationNameEditDistance = this.operationNameEditDistance();
						int otherOperationNameEditDistance = operationBodyMapper.operationNameEditDistance();
						if(thisOperationNameEditDistance != otherOperationNameEditDistance) {
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

	private boolean existsVariableDeclarationForV2InitializedWithV1(VariableDeclaration v1, VariableDeclaration v2, ReplacementInfo info) {
		for(AbstractCodeFragment fragment2 : info.statements2) {
			if(fragment2.getVariableDeclarations().contains(v2)) {
				AbstractExpression initializer = v2.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v1.getVariableName())) {
							return true;
						}
					}
				}
				
			}
			if(fragment2.getString().equals(v2.getVariableName() + "=" + v1.getVariableName() + ";\n")) {
				return true;
			}
			VariableDeclaration v1DeclarationInFragment2 = fragment2.getVariableDeclaration(v1.getVariableName());
			if(v1DeclarationInFragment2 != null) {
				AbstractExpression initializer = v1DeclarationInFragment2.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v2.getVariableName())) {
							return true;
						}
					}
				}
			}
			if(fragment2.getString().equals(v1.getVariableName() + "=" + v2.getVariableName() + ";\n")) {
				return true;
			}
		}
		return false;
	}

	private boolean existsVariableDeclarationForV1InitializedWithV2(VariableDeclaration v1, VariableDeclaration v2, ReplacementInfo info) {
		for(AbstractCodeFragment fragment1 : info.statements1) {
			if(fragment1.getVariableDeclarations().contains(v1)) {
				AbstractExpression initializer = v1.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v2.getVariableName())) {
							return true;
						}
					}
				}
				
			}
			if(fragment1.getString().equals(v1.getVariableName() + "=" + v2.getVariableName() + ";\n")) {
				return true;
			}
			VariableDeclaration v2DeclarationInFragment1 = fragment1.getVariableDeclaration(v2.getVariableName());
			if(v2DeclarationInFragment1 != null) {
				AbstractExpression initializer = v2DeclarationInFragment1.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v1.getVariableName())) {
							return true;
						}
					}
				}
			}
			if(fragment1.getString().equals(v2.getVariableName() + "=" + v1.getVariableName() + ";\n")) {
				return true;
			}
		}
		return false;
	}

	private int mappingsForStatementsInScope(AbstractCodeFragment statement1, AbstractCodeFragment statement2, VariableDeclaration v1, VariableDeclaration v2) {
		boolean increment = v1 != null && v2 != null && statement1.getString().startsWith(v1.getVariableName() + "+=") && statement2.getString().startsWith(v2.getVariableName() + "+=");
		boolean decrement = v1 != null && v2 != null && statement1.getString().startsWith(v1.getVariableName() + "-=") && statement2.getString().startsWith(v2.getVariableName() + "-=");
		if(v1 != null && v2 != null && (increment || decrement) && !mappings.isEmpty()) {
			int count = 0;
			Set<AbstractCodeFragment> statementsInScope1 = v1.getScope().getStatementsInScopeUsingVariable();
			Set<AbstractCodeFragment> statementsInScope2 = v2.getScope().getStatementsInScopeUsingVariable();
			for(AbstractCodeMapping mapping : mappings) {
				if(statementsInScope1.contains(mapping.getFragment1()) && statementsInScope2.contains(mapping.getFragment2())) {
					count++;
				}
				if(mapping.getFragment1().getVariableDeclarations().contains(v1) && mapping.getFragment2().getVariableDeclarations().contains(v2)) {
					count++;
				}
			}
			return count;
		}
		return -1;
	}

	private int inconsistentVariableMappingCount(AbstractCodeFragment statement1, AbstractCodeFragment statement2, VariableDeclaration v1, VariableDeclaration v2) {
		int count = 0;
		if(v1 != null && v2 != null) {
			boolean variableDeclarationMismatch = false;
			for(AbstractCodeMapping mapping : mappings) {
				List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.contains(v1) &&
						variableDeclarations2.size() > 0 &&
						!variableDeclarations2.contains(v2)) {
					variableDeclarationMismatch = true;
					count++;
				}
				if(variableDeclarations2.contains(v2) &&
						variableDeclarations1.size() > 0 &&
						!variableDeclarations1.contains(v1)) {
					variableDeclarationMismatch = true;
					count++;
				}
				if(mapping.isExact()) {
					boolean containsMapping = true;
					if(statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject &&
							statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
						CompositeStatementObject comp2 = (CompositeStatementObject)statement2;
						containsMapping = comp1.contains(mapping.getFragment1()) && comp2.contains(mapping.getFragment2());
					}
					if(containsMapping) {
						if(VariableReplacementAnalysis.bothFragmentsUseVariable(v1, mapping)) {
							VariableDeclaration otherV1 = mapping.getFragment1().getVariableDeclaration(v1.getVariableName());
							if(otherV1 != null) {
								VariableScope otherV1Scope = otherV1.getScope();
								VariableScope v1Scope = v1.getScope();
								if(otherV1Scope.overlaps(v1Scope)) {
									count++;
								}
							}
							else {
								count++;
							}
						}
						if(VariableReplacementAnalysis.bothFragmentsUseVariable(v2, mapping)) {
							VariableDeclaration otherV2 = mapping.getFragment2().getVariableDeclaration(v2.getVariableName());
							if(otherV2 != null) {
								VariableScope otherV2Scope = otherV2.getScope();
								VariableScope v2Scope = v2.getScope();
								if(otherV2Scope.overlaps(v2Scope)) {
									count++;
								}
							}
							else {
								count++;
							}
						}
					}
				}
				else if(variableDeclarationMismatch && !variableDeclarations1.contains(v1) && !variableDeclarations2.contains(v2)) {
					for(Replacement r : mapping.getReplacements()) {
						if(r.getBefore().equals(v1.getVariableName()) && !r.getAfter().equals(v2.getVariableName())) {
							count++;
						}
						else if(!r.getBefore().equals(v1.getVariableName()) && r.getAfter().equals(v2.getVariableName())) {
							count++;
						}
					}
				}
			}
		}
		return count;
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

	private double compositeChildMatchingScore(CompositeStatementObject comp1, CompositeStatementObject comp2, Set<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		List<AbstractStatement> compStatements1 = comp1.getStatements();
		List<AbstractStatement> compStatements2 = comp2.getStatements();
		int childrenSize1 = compStatements1.size();
		int childrenSize2 = compStatements2.size();
		List<CompositeStatementObject> nestedTryCatch1 = getNestedTryCatch(compStatements1);
		List<CompositeStatementObject> nestedTryCatch2 = getNestedTryCatch(compStatements2);
		boolean equalIfElseIfChain = false;
		
		if(parentMapper != null && comp1.getLocationInfo().getCodeElementType().equals(comp2.getLocationInfo().getCodeElementType()) &&
				childrenSize1 == 1 && childrenSize2 == 1 && !comp1.getString().equals("{") && !comp2.getString().equals("{")) {
			if(compStatements1.get(0).getString().equals("{") && !compStatements2.get(0).getString().equals("{")) {
				CompositeStatementObject block = (CompositeStatementObject)compStatements1.get(0);
				compStatements1 = new ArrayList<>(comp1.getStatements());
				compStatements1.addAll(block.getStatements());
			}
			if(!compStatements1.get(0).getString().equals("{") && compStatements2.get(0).getString().equals("{")) {
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
			int leaveSize1 = leaves1.size();
			int leaveSize2 = leaves2.size();
			int mappedLeavesSize = 0;
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
					if(!mappingUnderNestedTryCatch && !equalIfElseIfChain) {
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
						if(invocation != null && classDiff != null && (matchingOperation = classDiff.matchesOperation(invocation, addedOperations, container2)) != null && !matchingOperation.equals(container2)) {
							int matchingLeaves = matchingLeaves(matchingOperation, leaves1);
							mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
							leaveSize2 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
						}
						if(invocation != null && classDiff != null && invocation.actualString().contains(" -> ")) {
							for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
								for(AbstractCall inv : lambda.getAllOperationInvocations()) {
									if((matchingOperation = classDiff.matchesOperation(inv, addedOperations, container2)) != null && !matchingOperation.equals(container2)) {
										int matchingLeaves = matchingLeaves(matchingOperation, leaves1);
										mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
										leaveSize2 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
									}
								}
							}
						}
						if(matchingOperation == null && classDiff != null) {
							for(AbstractCall call : leaf2.getMethodInvocations()) {
								if((matchingOperation = classDiff.matchesOperation(call, addedOperations, container2)) != null && !matchingOperation.equals(container2) && matchingOperation.stringRepresentation().size() > 3) {
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
						if(invocation != null && classDiff != null && (matchingOperation = classDiff.matchesOperation(invocation, removedOperations, container1)) != null && !matchingOperation.equals(container1)) {
							int matchingLeaves = matchingLeaves(matchingOperation, leaves2);
							mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
							leaveSize1 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
						}
						if(invocation != null && classDiff != null && invocation.actualString().contains(" -> ")) {
							for(LambdaExpressionObject lambda : leaf1.getLambdas()) {
								for(AbstractCall inv : lambda.getAllOperationInvocations()) {
									if((matchingOperation = classDiff.matchesOperation(inv, removedOperations, container1)) != null && !matchingOperation.equals(container1)) {
										int matchingLeaves = matchingLeaves(matchingOperation, leaves2);
										mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
										leaveSize1 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
									}
								}
							}
						}
						if(matchingOperation == null && classDiff != null) {
							for(AbstractCall call : leaf1.getMethodInvocations()) {
								if((matchingOperation = classDiff.matchesOperation(call, removedOperations, container1)) != null && !matchingOperation.equals(container1) && matchingOperation.stringRepresentation().size() > 3) {
									int matchingLeaves = matchingLeaves(matchingOperation, leaves2);
									mappedLeavesSize += matchingLeaves > 0 ? matchingLeaves : 1;
									leaveSize1 += matchingOperation.getBody() != null ? matchingOperation.getBody().getCompositeStatement().getLeaves().size() : 0;
								}
							}
						}
					}
				}
				if(leaveSize1 == 1 && leaveSize2 == 1 && leaves1.get(0).getString().equals("continue;\n") && leaves2.get(0).getString().equals("return null;\n")) {
					mappedLeavesSize++;
				}
				if(leaveSize1 == 2 && leaveSize2 == 1 && !leaves1.get(0).getString().equals("break;\n") && leaves1.get(1).getString().equals("break;\n") && leaves2.get(0).getString().startsWith("return ")) {
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
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
						!logGuard(comp1) && !nullCheck(comp1, comp2) &&
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

	private boolean nullCheck(CompositeStatementObject comp1, CompositeStatementObject comp2) {
		if(comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			for(AbstractExpression expression : comp1.getExpressions()) {
				if(expression.getString().endsWith(" == null")) {
					List<String> commentsWithinStatement1 = extractCommentsWithinStatement(comp1, container1);
					List<String> commentsWithinStatement2 = extractCommentsWithinStatement(comp2, container2);
					if(commentsWithinStatement1.size() > 0 || commentsWithinStatement2.size() > 0) {
						Set<String> intersection = new LinkedHashSet<>(commentsWithinStatement1);
						intersection.retainAll(commentsWithinStatement2);
						if(intersection.isEmpty()) {
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

	private double compositeChildMatchingScore(TryStatementObject try1, TryStatementObject try2, Set<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration) {
		double score = compositeChildMatchingScore((CompositeStatementObject)try1, (CompositeStatementObject)try2, mappings, removedOperations, addedOperations);
		if(!tryWithResourceMigration) {
			List<CompositeStatementObject> catchClauses1 = try1.getCatchClauses();
			List<CompositeStatementObject> catchClauses2 = try2.getCatchClauses();
			if(catchClauses1.size() == catchClauses2.size()) {
				for(int i=0; i<catchClauses1.size(); i++) {
					double tmpScore = compositeChildMatchingScore(catchClauses1.get(i), catchClauses2.get(i), mappings, removedOperations, addedOperations);
					if(tmpScore == 1) {
						score += tmpScore;
					}
				}
			}
			if(try1.getFinallyClause() != null && try2.getFinallyClause() != null) {
				double tmpScore = compositeChildMatchingScore(try1.getFinallyClause(), try2.getFinallyClause(), mappings, removedOperations, addedOperations);
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

	private boolean matchPairOfRemovedAddedOperationsWithIdenticalBody(AbstractCall call1, AbstractCall call2) {
		if(classDiff != null) {
			for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
				if(call1.matchesOperation(removedOperation, container1, classDiff, modelDiff)) {
					for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
						if(removedOperation.getBodyHashCode() == addedOperation.getBodyHashCode()) {
							if(call2.matchesOperation(addedOperation, container2, classDiff, modelDiff)) {
								return true;
							}
						}
					}
				}
			}
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getBodyHashCode() == mapper.getContainer2().getBodyHashCode() &&
						call1.matchesOperation(mapper.getContainer1(), container1, classDiff, modelDiff) &&
						call2.matchesOperation(mapper.getContainer2(), container2, classDiff, modelDiff)) {
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
					return true;
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

	public Set<VariableDeclaration> getRemovedVariables() {
		return removedVariables;
	}

	public Set<VariableDeclaration> getAddedVariables() {
		return addedVariables;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMovedVariables() {
		return movedVariables;
	}
}
