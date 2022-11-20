package gr.uom.java.xmi.decomposition;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableDeclarationReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;
import gr.uom.java.xmi.diff.AddVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddVariableModifierRefactoring;
import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;
import gr.uom.java.xmi.diff.CandidateMergeVariableRefactoring;
import gr.uom.java.xmi.diff.CandidateSplitVariableRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineAttributeRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.ModifyVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveVariableModifierRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnnotationDiff;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLParameterDiff;

public class VariableReplacementAnalysis {
	private UMLOperationBodyMapper mapper;
	private Set<AbstractCodeMapping> mappings;
	private List<AbstractCodeFragment> nonMappedLeavesT1;
	private List<AbstractCodeFragment> nonMappedLeavesT2;
	private List<CompositeStatementObject> nonMappedInnerNodesT1;
	private List<CompositeStatementObject> nonMappedInnerNodesT2;
	private VariableDeclarationContainer operation1;
	private VariableDeclarationContainer operation2;
	private List<UMLOperationBodyMapper> childMappers;
	private Set<Refactoring> refactorings;
	private VariableDeclarationContainer callSiteOperation;
	private UMLOperationDiff operationDiff;
	private UMLAbstractClassDiff classDiff;
	private Set<VariableDeclaration> removedVariables = new LinkedHashSet<>();
	private Set<VariableDeclaration> removedVariablesStoringTheReturnOfInlinedMethod = new LinkedHashSet<>();
	private Set<VariableDeclaration> removedVariablesInAnonymousClassDeclarations = new LinkedHashSet<>();
    private Set<VariableDeclaration> addedVariables = new LinkedHashSet<>();
    private Set<VariableDeclaration> addedVariablesStoringTheReturnOfExtractedMethod = new LinkedHashSet<>();
    private Set<VariableDeclaration> addedVariablesInAnonymousClassDeclarations = new LinkedHashSet<>();
    private Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables = new LinkedHashSet<>();
    private Set<Pair<VariableDeclaration, VariableDeclaration>> movedVariables = new LinkedHashSet<>();
	private Set<RenameVariableRefactoring> variableRenames = new LinkedHashSet<RenameVariableRefactoring>();
	private Set<MergeVariableRefactoring> variableMerges = new LinkedHashSet<MergeVariableRefactoring>();
	private Set<SplitVariableRefactoring> variableSplits = new LinkedHashSet<SplitVariableRefactoring>();
	private Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRefactoring>();
	private Set<CandidateMergeVariableRefactoring> candidateAttributeMerges = new LinkedHashSet<CandidateMergeVariableRefactoring>();
	private Set<CandidateSplitVariableRefactoring> candidateAttributeSplits = new LinkedHashSet<CandidateSplitVariableRefactoring>();
	private boolean insideExtractedOrInlinedMethod = false;
	private Map<String, Set<String>> aliasedVariablesInOriginalMethod;
	private Map<String, Set<String>> aliasedVariablesInNextMethod;

	public VariableReplacementAnalysis(UMLOperationBodyMapper mapper, Set<Refactoring> refactorings, UMLAbstractClassDiff classDiff,
			Set<Pair<VariableDeclaration, VariableDeclaration>> previouslyMatchedVariables) {
		this.mapper = mapper;
		this.mappings = mapper.getMappings();
		this.nonMappedLeavesT1 = mapper.getNonMappedLeavesT1();
		this.nonMappedLeavesT2 = mapper.getNonMappedLeavesT2();
		this.nonMappedInnerNodesT1 = mapper.getNonMappedInnerNodesT1();
		this.nonMappedInnerNodesT2 = mapper.getNonMappedInnerNodesT2();
		this.operation1 = mapper.getContainer1();
		this.operation2 = mapper.getContainer2();
		this.aliasedVariablesInOriginalMethod = operation1.aliasedVariables();
		this.aliasedVariablesInNextMethod = operation2.aliasedVariables();
		this.childMappers = new ArrayList<UMLOperationBodyMapper>();
		this.childMappers.addAll(mapper.getChildMappers());
		UMLOperationBodyMapper parentMapper = mapper.getParentMapper();
		if(parentMapper != null) {
			this.childMappers.addAll(parentMapper.getChildMappers());
		}
		this.refactorings = refactorings;
		this.callSiteOperation = mapper.getCallSiteOperation();
		this.operationDiff = mapper.getOperationSignatureDiff().isPresent() ? mapper.getOperationSignatureDiff().get() : null;
		this.classDiff = classDiff;
		//when the containers of the parent mapper are the same with those of the child mapper, then the child mapper is a nested lambda expression mapper
		if(parentMapper != null && !(parentMapper.getContainer1().equals(mapper.getContainer1()) && parentMapper.getContainer2().equals(mapper.getContainer2()))) {
			this.insideExtractedOrInlinedMethod = true;
		}
		if(parentMapper != null) {
			this.removedVariables.addAll(operation1.getBody() != null ? operation1.getBody().getAllVariableDeclarations() : Collections.emptySet());
			this.addedVariables.addAll(operation2.getBody() != null ? operation2.getBody().getAllVariableDeclarations() : Collections.emptySet());
		}
		else {
			this.removedVariables.addAll(operation1.getAllVariableDeclarations());
			for(UMLAnonymousClass anonymous : operation1.getAnonymousClassList()) {
				for(UMLOperation operation : anonymous.getOperations()) {
					List<VariableDeclaration> allVariableDeclarations = operation.getAllVariableDeclarations();
					this.removedVariables.addAll(allVariableDeclarations);
					this.removedVariablesInAnonymousClassDeclarations.addAll(allVariableDeclarations);
				}
			}
			this.addedVariables.addAll(operation2.getAllVariableDeclarations());
			for(UMLAnonymousClass anonymous : operation2.getAnonymousClassList()) {
				for(UMLOperation operation : anonymous.getOperations()) {
					List<VariableDeclaration> allVariableDeclarations = operation.getAllVariableDeclarations();
					this.addedVariables.addAll(allVariableDeclarations);
					this.addedVariablesInAnonymousClassDeclarations.addAll(allVariableDeclarations);
				}
			}
		}
		for(Pair<VariableDeclaration, VariableDeclaration> matchedVariable : previouslyMatchedVariables) {
			this.removedVariables.remove(matchedVariable.getLeft());
			this.addedVariables.remove(matchedVariable.getRight());
			this.matchedVariables.add(matchedVariable);
			if(!matchedVariable.getLeft().getVariableName().equals(matchedVariable.getRight().getVariableName())) {
				Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(matchedVariable.getLeft(), matchedVariable.getRight(), mappings);
				RenameVariableRefactoring ref = new RenameVariableRefactoring(matchedVariable.getLeft(), matchedVariable.getRight(), operation1, operation2, variableReferences, insideExtractedOrInlinedMethod);
				variableRenames.add(ref);
				getVariableRefactorings(matchedVariable.getLeft(), matchedVariable.getRight(), operation1, operation2, variableReferences, ref);
			}
		}
		processIdenticalAnonymousAndLambdas();
		findVariableSplits();
		findVariableMerges();
		findConsistentVariableRenames();
		findAttributeExtractions();
		findTypeChanges();
		findMatchingVariablesWithoutVariableDeclarationMapping();
		findMovedVariablesToExtractedFromInlinedMethods();
		findMatchingVariablesWithoutReferenceMapping();
	}

	private void processIdenticalAnonymousAndLambdas() {
		List<UMLAnonymousClass> anonymousClassList1 = operation1.getAnonymousClassList();
		List<UMLAnonymousClass> anonymousClassList2 = operation2.getAnonymousClassList();
		if(anonymousClassList1.size() == anonymousClassList2.size()) {
			for(int i=0; i<anonymousClassList1.size(); i++) {
				UMLAnonymousClass anonymous1 = anonymousClassList1.get(i);
				UMLAnonymousClass anonymous2 = anonymousClassList2.get(i);
				List<UMLOperation> operations1 = anonymous1.getOperations();
				List<UMLOperation> operations2 = anonymous2.getOperations();
				if(operations1.size() == operations2.size()) {
					for(int j=0; j<operations1.size(); j++) {
						UMLOperation op1 = operations1.get(j);
						UMLOperation op2 = operations2.get(j);
						OperationBody body1 = op1.getBody();
						OperationBody body2 = op2.getBody();
						if(body1 != null && body2 != null) {
							List<VariableDeclaration> declarations1 = op1.getParameterDeclarationList();
							List<VariableDeclaration> declarations2 = op2.getParameterDeclarationList();
							if(declarations1.toString().equals(declarations2.toString())) {
								processVariableDeclarationsInIdenticalOperations(declarations1, declarations2);
							}
							if(body1.getBodyHashCode() == body2.getBodyHashCode()) {
								processVariableDeclarationsInIdenticalOperations(body1.getAllVariableDeclarations(), body2.getAllVariableDeclarations());
							}
						}
					}
				}
			}
		}
		List<LambdaExpressionObject> lambdas1 = operation1.getAllLambdas();
		List<LambdaExpressionObject> lambdas2 = operation2.getAllLambdas();
		if(lambdas1.size() == lambdas2.size()) {
			for(int i=0; i<lambdas1.size(); i++) {
				LambdaExpressionObject lambda1 = lambdas1.get(i);
				LambdaExpressionObject lambda2 = lambdas2.get(i);
				OperationBody body1 = lambda1.getBody();
				OperationBody body2 = lambda2.getBody();
				if(body1 != null && body2 != null) {
					if(body1.getBodyHashCode() == body2.getBodyHashCode()) {
						processVariableDeclarationsInIdenticalOperations(body1.getAllVariableDeclarations(), body2.getAllVariableDeclarations());
					}
				}
			}
		}
	}

	private void processVariableDeclarationsInIdenticalOperations(List<VariableDeclaration> declarations1, List<VariableDeclaration> declarations2) {
		Iterator<VariableDeclaration> removedVariableIterator = declarations1.iterator();
		Iterator<VariableDeclaration> addedVariableIterator = declarations2.iterator();
		while(removedVariableIterator.hasNext() && addedVariableIterator.hasNext()) {
			VariableDeclaration removedVariable = removedVariableIterator.next();
			VariableDeclaration addedVariable = addedVariableIterator.next();
			Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
			if(!matchedVariables.contains(pair) && removedVariable.getVariableName().equals(addedVariable.getVariableName())) {
				removedVariables.remove(removedVariable);
				addedVariables.remove(addedVariable);
				matchedVariables.add(pair);
			}
		}
	}

	private void findMatchingVariablesWithoutReferenceMapping() {
		Set<VariableDeclaration> removedVariablesToBeRemoved = new LinkedHashSet<>();
		Set<VariableDeclaration> addedVariablesToBeRemoved = new LinkedHashSet<>();
		if(removedVariables.toString().equals(addedVariables.toString())) {
			Iterator<VariableDeclaration> removedVariableIterator = removedVariables.iterator();
			Iterator<VariableDeclaration> addedVariableIterator = addedVariables.iterator();
			while(removedVariableIterator.hasNext() && addedVariableIterator.hasNext()) {
				VariableDeclaration removedVariable = removedVariableIterator.next();
				VariableDeclaration addedVariable = addedVariableIterator.next();
				Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
				if(!matchedVariables.contains(pair) && removedVariable.getVariableName().equals(addedVariable.getVariableName())) {
					removedVariablesToBeRemoved.add(removedVariable);
					addedVariablesToBeRemoved.add(addedVariable);
					matchedVariables.add(pair);
					getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, Collections.emptySet(), null);
				}
			}
		}
		else if(operation1.getBody() != null && operation2.getBody() != null && operation1.getBodyHashCode() == operation2.getBodyHashCode()) {
			if(removedVariables.size() <= addedVariables.size()) {
				for(VariableDeclaration removedVariable : removedVariables) {
					if(!removedVariablesToBeRemoved.contains(removedVariable)) {
						for(VariableDeclaration addedVariable : addedVariables) {
							if(!addedVariablesToBeRemoved.contains(addedVariable)) {
								Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
								if(!matchedVariables.contains(pair) && addedVariable.getVariableName().equals(removedVariable.getVariableName()) && addedVariable.equalType(removedVariable)) {
									removedVariablesToBeRemoved.add(removedVariable);
									addedVariablesToBeRemoved.add(addedVariable);
									matchedVariables.add(pair);
									getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, Collections.emptySet(), null);
								}
							}
						}
					}
				}
			}
			else {
				for(VariableDeclaration addedVariable : addedVariables) {
					if(!addedVariablesToBeRemoved.contains(addedVariable)) {
						for(VariableDeclaration removedVariable : removedVariables) {
							if(!removedVariablesToBeRemoved.contains(removedVariable)) {
								Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
								if(!matchedVariables.contains(pair) && addedVariable.getVariableName().equals(removedVariable.getVariableName()) && addedVariable.equalType(removedVariable)) {
									removedVariablesToBeRemoved.add(removedVariable);
									addedVariablesToBeRemoved.add(addedVariable);
									matchedVariables.add(pair);
									getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, Collections.emptySet(), null);
								}
							}
						}
					}
				}
			}
		}
		else if(removedVariables.size() <= addedVariables.size()) {
			for(VariableDeclaration removedVariable : removedVariables) {
				if(!removedVariablesToBeRemoved.contains(removedVariable) && removedVariable.isParameter()) {
					for(VariableDeclaration addedVariable : addedVariables) {
						if(!addedVariablesToBeRemoved.contains(addedVariable) && !removedVariablesToBeRemoved.contains(removedVariable) && addedVariable.isParameter()) {
							Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
							if(!matchedVariables.contains(pair) && addedVariable.getVariableName().equals(removedVariable.getVariableName()) && addedVariable.equalType(removedVariable)) {
								removedVariablesToBeRemoved.add(removedVariable);
								addedVariablesToBeRemoved.add(addedVariable);
								matchedVariables.add(pair);
								getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, Collections.emptySet(), null);
							}
						}
					}
				}
			}
		}
		else {
			for(VariableDeclaration addedVariable : addedVariables) {
				if(!addedVariablesToBeRemoved.contains(addedVariable) && addedVariable.isParameter()) {
					for(VariableDeclaration removedVariable : removedVariables) {
						if(!removedVariablesToBeRemoved.contains(removedVariable) && !addedVariablesToBeRemoved.contains(addedVariable) && removedVariable.isParameter()) {
							Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
							if(!matchedVariables.contains(pair) && addedVariable.getVariableName().equals(removedVariable.getVariableName()) && addedVariable.equalType(removedVariable)) {
								removedVariablesToBeRemoved.add(removedVariable);
								addedVariablesToBeRemoved.add(addedVariable);
								matchedVariables.add(pair);
								getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, Collections.emptySet(), null);
							}
						}
					}
				}
			}
		}
		removedVariables.removeAll(removedVariablesToBeRemoved);
		addedVariables.removeAll(addedVariablesToBeRemoved);
	}

	private void findMovedVariablesToExtractedFromInlinedMethods() {
		// check in removedVariables for variables moved to extracted methods
		Set<VariableDeclaration> removedVariablesToBeRemoved = new LinkedHashSet<>();
		for(VariableDeclaration removedVariable : removedVariables) {
			for(UMLOperationBodyMapper childMapper : childMappers) {
				Set<Pair<VariableDeclaration, VariableDeclaration>> pairs = childMapper.getMatchedVariables();
				for(Pair<VariableDeclaration, VariableDeclaration> pair : pairs) {
					if(removedVariable.equals(pair.getKey())) {
						removedVariablesToBeRemoved.add(removedVariable);
						movedVariables.add(pair);
						break;
					}
				}
			}
		}
		removedVariables.removeAll(removedVariablesToBeRemoved);
		// check in addedVariables for variables moved from inlined methods
		Set<VariableDeclaration> addedVariablesToBeRemoved = new LinkedHashSet<>();
		for(VariableDeclaration addedVariable : addedVariables) {
			for(UMLOperationBodyMapper childMapper : childMappers) {
				Set<Pair<VariableDeclaration, VariableDeclaration>> pairs = childMapper.getMatchedVariables();
				for(Pair<VariableDeclaration, VariableDeclaration> pair : pairs) {
					if(addedVariable.equals(pair.getValue())) {
						addedVariablesToBeRemoved.add(addedVariable);
						movedVariables.add(pair);
						break;
					}
				}
			}
		}
		addedVariables.removeAll(addedVariablesToBeRemoved);
	}

	private void findMatchingVariablesWithoutVariableDeclarationMapping() {
		Set<VariableDeclaration> removedVariablesToBeRemoved = new LinkedHashSet<>();
		Set<VariableDeclaration> addedVariablesToBeRemoved = new LinkedHashSet<>();
		if(removedVariables.size() <= addedVariables.size()) {
			for(VariableDeclaration removedVariable : removedVariables) {
				if(!removedVariablesToBeRemoved.contains(removedVariable) && callsInlinedMethod(removedVariable)) {
					removedVariablesStoringTheReturnOfInlinedMethod.add(removedVariable);
					removedVariablesToBeRemoved.add(removedVariable);
				}
				for(VariableDeclaration addedVariable : addedVariables) {
					if(!addedVariablesToBeRemoved.contains(addedVariable) && callsExtractedMethod(addedVariable)) {
						addedVariablesStoringTheReturnOfExtractedMethod.add(addedVariable);
						addedVariablesToBeRemoved.add(addedVariable);
					}
					Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
					if(!matchedVariables.contains(pair) && removedVariable.getVariableName().equals(addedVariable.getVariableName()) && !bothCatchExceptionVariables(removedVariable, addedVariable) &&
							!containerElementRelationship(removedVariable, addedVariable) && mappedStatementWithinVariableScopes(removedVariable, addedVariable)) {
						Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(removedVariable, addedVariable, mappings);
						removedVariablesToBeRemoved.add(removedVariable);
						addedVariablesToBeRemoved.add(addedVariable);
						matchedVariables.add(pair);
						getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, variableReferences, null);
					}
				}
			}
		}
		else {
			for(VariableDeclaration addedVariable : addedVariables) {
				if(!addedVariablesToBeRemoved.contains(addedVariable) && callsExtractedMethod(addedVariable)) {
					addedVariablesStoringTheReturnOfExtractedMethod.add(addedVariable);
					addedVariablesToBeRemoved.add(addedVariable);
				}
				for(VariableDeclaration removedVariable : removedVariables) {
					if(!removedVariablesToBeRemoved.contains(removedVariable) && callsInlinedMethod(removedVariable)) {
						removedVariablesStoringTheReturnOfInlinedMethod.add(removedVariable);
						removedVariablesToBeRemoved.add(removedVariable);
					}
					Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(removedVariable, addedVariable);
					if(!matchedVariables.contains(pair) && removedVariable.getVariableName().equals(addedVariable.getVariableName()) && !bothCatchExceptionVariables(removedVariable, addedVariable) &&
							!containerElementRelationship(removedVariable, addedVariable) && mappedStatementWithinVariableScopes(removedVariable, addedVariable)) {
						Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(removedVariable, addedVariable, mappings);
						removedVariablesToBeRemoved.add(removedVariable);
						addedVariablesToBeRemoved.add(addedVariable);
						matchedVariables.add(pair);
						getVariableRefactorings(removedVariable, addedVariable, operation1, operation2, variableReferences, null);
					}
				}
			}
		}
		removedVariables.removeAll(removedVariablesToBeRemoved);
		addedVariables.removeAll(addedVariablesToBeRemoved);
	}

	private boolean containerElementRelationship(VariableDeclaration removedVariable, VariableDeclaration addedVariable) {
		UMLType type1 = removedVariable.getType();
		UMLType type2 = addedVariable.getType();
		if(type1 != null && type2 != null && !type1.equals(type2) && !removedVariable.sameKind(addedVariable)) {
			if(type1.equalClassType(type2) && type1.getArrayDimension() != type2.getArrayDimension()) {
				return true;
			}
		}
		return false;
	}
	private boolean bothCatchExceptionVariables(VariableDeclaration removedVariable, VariableDeclaration addedVariable) {
		boolean isRemovedVariableCatchException = false;
		for(CompositeStatementObject composite : nonMappedInnerNodesT1) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					composite.getVariableDeclarations().contains(removedVariable)) {
				isRemovedVariableCatchException = true;
				break;
			}
		}
		boolean isAddedVariableCatchException = false;
		for(CompositeStatementObject composite : nonMappedInnerNodesT2) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					composite.getVariableDeclarations().contains(addedVariable)) {
				isAddedVariableCatchException = true;
				break;
			}
		}
		return isRemovedVariableCatchException && isAddedVariableCatchException;
	}

	private boolean mappedStatementWithinVariableScopes(VariableDeclaration removedVariable, VariableDeclaration addedVariable) {
		if(removedVariablesInAnonymousClassDeclarations.contains(removedVariable) && addedVariablesInAnonymousClassDeclarations.contains(addedVariable)) {
			return false;
		}
		if(removedVariablesInAnonymousClassDeclarations.contains(removedVariable) != addedVariablesInAnonymousClassDeclarations.contains(addedVariable)) {
			return false;
		}
		List<AbstractCodeFragment> statementsInScope1 = removedVariable.getStatementsInScopeUsingVariable();
		List<AbstractCodeFragment> statementsInScope2 = addedVariable.getStatementsInScopeUsingVariable();
		for(AbstractCodeMapping mapping : mappings) {
			if(statementsInScope1.contains(mapping.getFragment1()) && statementsInScope2.contains(mapping.getFragment2())) {
				return true;
			}
			if(statementsInScope1.contains(mapping.getFragment1())) {
				for(Refactoring refactoring : refactorings) {
					if(refactoring instanceof ExtractVariableRefactoring) {
						ExtractVariableRefactoring ref = (ExtractVariableRefactoring)refactoring;
						boolean referenceFound = false;
						for(AbstractCodeMapping reference : ref.getReferences()) {
							if(mapping.equals(reference)) {
								referenceFound = true;
								break;
							}
						}
						boolean variableDeclarationFound = false;
						for(AbstractCodeFragment fragment : statementsInScope2) {
							if(fragment.getVariableDeclarations().contains(ref.getVariableDeclaration())) {
								variableDeclarationFound = true;
								break;
							}
						}
						if(referenceFound && variableDeclarationFound) {
							return true;
						}
					}
				}
			}
			if(statementsInScope2.contains(mapping.getFragment2())) {
				for(Refactoring refactoring : refactorings) {
					if(refactoring instanceof InlineVariableRefactoring) {
						InlineVariableRefactoring ref = (InlineVariableRefactoring)refactoring;
						boolean referenceFound = false;
						for(AbstractCodeMapping reference : ref.getReferences()) {
							if(mapping.equals(reference)) {
								referenceFound = true;
								break;
							}
						}
						boolean variableDeclarationFound = false;
						for(AbstractCodeFragment fragment : statementsInScope1) {
							if(fragment.getVariableDeclarations().contains(ref.getVariableDeclaration())) {
								variableDeclarationFound = true;
								break;
							}
						}
						if(referenceFound && variableDeclarationFound) {
							return true;
						}
					}
				}
			}
			boolean statementInScopeInsideLambda1 = statementInScopeInsideLambda(removedVariable, statementsInScope1, mapping.getFragment1());
			boolean statementInScopeInsideLambda2 = statementInScopeInsideLambda(addedVariable, statementsInScope2, mapping.getFragment2());
			if(statementInScopeInsideLambda1 && statementInScopeInsideLambda2) {
				return true;
			}
		}
		for(UMLOperationBodyMapper childMapper : childMappers) {
			for(AbstractCodeMapping mapping : childMapper.getMappings()) {
				if(mapping.getFragment1().getVariableDeclarations().contains(removedVariable) || mapping.getFragment2().getVariableDeclarations().contains(addedVariable)) {
					break;
				}
				//statementsInScope2 contains a call to an extracted method
				if(statementsInScope1.contains(mapping.getFragment1()) && containCallToOperation(statementsInScope2, childMapper.getContainer2(), this.operation2)) {
					return true;
				}
				//statementsInScope1 contains a call to an inlined method
				if(statementsInScope2.contains(mapping.getFragment2()) && containCallToOperation(statementsInScope1, childMapper.getContainer1(), this.operation1)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containCallToOperation(List<AbstractCodeFragment> statementsInScope, VariableDeclarationContainer calledOperation, VariableDeclarationContainer callerOperation) {
		UMLModelDiff modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		for(AbstractCodeFragment statement : statementsInScope) {
			Map<String, List<AbstractCall>> map = statement.getMethodInvocationMap();
			for(String key : map.keySet()) {
				List<AbstractCall> invocationList = map.get(key);
				for(AbstractCall invocation : invocationList) {
					if(invocation.matchesOperation(calledOperation, callerOperation, modelDiff)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean statementInScopeInsideLambda(VariableDeclaration variable,
			List<AbstractCodeFragment> statementsInScope, AbstractCodeFragment fragment) {
		List<LambdaExpressionObject> lambdas = fragment.getLambdas();
		if(lambdas.size() > 0) {
			for(LambdaExpressionObject lambda : lambdas) {
				OperationBody lambdaBody = lambda.getBody();
				if(lambdaBody != null && lambdaBody.getAllVariableDeclarations().contains(variable)) {
					List<AbstractStatement> statements = lambdaBody.getCompositeStatement().getStatements();
					if(statements.containsAll(statementsInScope)) {
						return true;
					}
					//List<AbstractCodeFragment> allStatements = new ArrayList<>();
					//allStatements.addAll(lambdaBody.getCompositeStatement().getInnerNodes());
					//allStatements.addAll(lambdaBody.getCompositeStatement().getLeaves());
					//if(allStatements.containsAll(statementsInScope)) {
					//	return true;
					//}
				}
			}
		}
		return false;
	}

	private boolean callsExtractedMethod(VariableDeclaration addedVariable) {
		UMLModelDiff modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		AbstractExpression initializer = addedVariable.getInitializer();
		if(initializer != null) {
			AbstractCall invocation = initializer.invocationCoveringEntireFragment();
			if(invocation != null) {
				for(UMLOperationBodyMapper childMapper : childMappers) {
					if(invocation.matchesOperation(childMapper.getContainer2(), operation2, modelDiff)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean callsInlinedMethod(VariableDeclaration removedVariable) {
		UMLModelDiff modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		AbstractExpression initializer = removedVariable.getInitializer();
		if(initializer != null) {
			AbstractCall invocation = initializer.invocationCoveringEntireFragment();
			if(invocation != null) {
				for(UMLOperationBodyMapper childMapper : childMappers) {
					if(invocation.matchesOperation(childMapper.getContainer1(), operation1, modelDiff)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void findTypeChanges() {
		for(AbstractCodeMapping mapping : mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			List<VariableDeclaration> declarations1 = fragment1.getVariableDeclarations();
			List<VariableDeclaration> declarations2 = fragment2.getVariableDeclarations();
			if(declarations1.size() == declarations2.size()) {
				for(int i=0; i<declarations1.size(); i++) {
					VariableDeclaration declaration1 = declarations1.get(i);
					VariableDeclaration declaration2 = declarations2.get(i);
					if(declaration1.getVariableName().equals(declaration2.getVariableName())) {
						if(declaration1.equalType(declaration2) && declaration1.equalQualifiedType(declaration2)) {
							Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(declaration1, declaration2, mappings);
							removedVariables.remove(declaration1);
							addedVariables.remove(declaration2);
							matchedVariables.add(Pair.of(declaration1, declaration2));
							getVariableRefactorings(declaration1, declaration2, mapping.getOperation1(), mapping.getOperation2(), variableReferences, null);
						}
						else if(!containsVariableDeclarationWithSameNameAndType(declaration1, declarations2) &&
								!containsVariableDeclarationWithSameNameAndType(declaration2, declarations1)) {
							Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(declaration1, declaration2, mappings);
							removedVariables.remove(declaration1);
							addedVariables.remove(declaration2);
							matchedVariables.add(Pair.of(declaration1, declaration2));
							getVariableRefactorings(declaration1, declaration2, mapping.getOperation1(), mapping.getOperation2(), variableReferences, null);
						}
					}
				}
			}
			else if(declarations1.size() > 0 && declarations2.size() > 0) {
				VariableDeclaration declaration1 = declarations1.get(0);
				VariableDeclaration declaration2 = declarations2.get(0);
				if(declaration1.getVariableName().equals(declaration2.getVariableName())) {
					if(declaration1.equalType(declaration2) && declaration1.equalQualifiedType(declaration2)) {
						Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(declaration1, declaration2, mappings);
						removedVariables.remove(declaration1);
						addedVariables.remove(declaration2);
						matchedVariables.add(Pair.of(declaration1, declaration2));
						getVariableRefactorings(declaration1, declaration2, mapping.getOperation1(), mapping.getOperation2(), variableReferences, null);
					}
					else if(!containsVariableDeclarationWithSameNameAndType(declaration1, declarations2) &&
							!containsVariableDeclarationWithSameNameAndType(declaration2, declarations1)) {
						Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(declaration1, declaration2, mappings);
						removedVariables.remove(declaration1);
						addedVariables.remove(declaration2);
						matchedVariables.add(Pair.of(declaration1, declaration2));
						getVariableRefactorings(declaration1, declaration2, mapping.getOperation1(), mapping.getOperation2(), variableReferences, null);
					}
				}
			}
		}
	}

	private boolean containsVariableDeclarationWithSameNameAndType(VariableDeclaration declaration, List<VariableDeclaration> declarations) {
		for(VariableDeclaration d : declarations) {
			if(d.getVariableName().equals(declaration.getVariableName()) && d.equalType(declaration) && d.equalQualifiedType(declaration)) {
				return true;
			}
		}
		return false;
	}

	private void findAttributeExtractions() {
		if(classDiff != null) {
			UMLModelDiff modelDiff = classDiff.getModelDiff();
			List<UMLAttribute> addedAttributes = new ArrayList<>();
			addedAttributes.addAll(classDiff.getAddedAttributes());
			List<UMLAttribute> removedAttributes = new ArrayList<>();
			removedAttributes.addAll(classDiff.getRemovedAttributes());
			if(modelDiff != null) {
				for(UMLAbstractClassDiff otherClassDiff : modelDiff.getCommonClassDiffList()) {
					if(otherClassDiff.getNextClass().isInnerClass(classDiff.getNextClass())) {
						addedAttributes.addAll(otherClassDiff.getAddedAttributes());
						removedAttributes.addAll(otherClassDiff.getRemovedAttributes());
					}
				}
			}
			for(AbstractCodeMapping mapping : mappings) {
				for(Replacement replacement : mapping.getReplacements()) {
					if(replacement.involvesVariable()) {
						for(UMLAttribute addedAttribute : addedAttributes) {
							VariableDeclaration variableDeclaration = addedAttribute.getVariableDeclaration();
							if(addedAttribute.getName().equals(replacement.getAfter()) && extractInlineCondition(variableDeclaration, replacement, replacement.getBefore())) {
								ExtractAttributeRefactoring refactoring = new ExtractAttributeRefactoring(addedAttribute, classDiff.getOriginalClass(), classDiff.getNextClass(), insideExtractedOrInlinedMethod);
								if(refactorings.contains(refactoring)) {
									Iterator<Refactoring> it = refactorings.iterator();
									while(it.hasNext()) {
										Refactoring ref = it.next();
										if(ref.equals(refactoring)) {
											((ExtractAttributeRefactoring)ref).addReference(mapping);
											break;
										}
									}
								}
								else {
									refactoring.addReference(mapping);
									refactorings.add(refactoring);
								}
							}
						}
						for(UMLAttribute removedAttribute : removedAttributes) {
							VariableDeclaration variableDeclaration = removedAttribute.getVariableDeclaration();
							if(removedAttribute.getName().equals(replacement.getBefore()) && extractInlineCondition(variableDeclaration, replacement, replacement.getAfter())) {
								InlineAttributeRefactoring refactoring = new InlineAttributeRefactoring(removedAttribute, classDiff.getOriginalClass(), classDiff.getNextClass(), insideExtractedOrInlinedMethod);
								if(refactorings.contains(refactoring)) {
									Iterator<Refactoring> it = refactorings.iterator();
									while(it.hasNext()) {
										Refactoring ref = it.next();
										if(ref.equals(refactoring)) {
											((InlineAttributeRefactoring)ref).addReference(mapping);
											break;
										}
									}
								}
								else {
									refactoring.addReference(mapping);
									refactorings.add(refactoring);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean extractInlineCondition(VariableDeclaration variableDeclaration, Replacement replacement, String replacementAsString) {
		if(variableDeclaration.getInitializer() != null) {
			if(variableDeclaration.getInitializer().getString().equals(replacementAsString)) {
				return true;
			}
			if(replacement instanceof VariableReplacementWithMethodInvocation) {
				VariableReplacementWithMethodInvocation r = (VariableReplacementWithMethodInvocation)replacement;
				Map<String, List<AbstractCall>> map = variableDeclaration.getInitializer().getMethodInvocationMap();
				for(String key : map.keySet()) {
					List<AbstractCall> list = map.get(key);
					for(AbstractCall call : list) {
						if(call.identicalName(r.getInvokedOperation())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	public Set<RenameVariableRefactoring> getVariableRenames() {
		return variableRenames;
	}

	public Set<MergeVariableRefactoring> getVariableMerges() {
		return variableMerges;
	}

	public Set<SplitVariableRefactoring> getVariableSplits() {
		return variableSplits;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMatchedVariables() {
		return matchedVariables;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMovedVariables() {
		return movedVariables;
	}

	public Set<VariableDeclaration> getRemovedVariables() {
		return removedVariables;
	}

	public Set<VariableDeclaration> getRemovedVariablesStoringTheReturnOfInlinedMethod() {
		return removedVariablesStoringTheReturnOfInlinedMethod;
	}

	public Set<VariableDeclaration> getAddedVariables() {
		return addedVariables;
	}

	public Set<VariableDeclaration> getAddedVariablesStoringTheReturnOfExtractedMethod() {
		return addedVariablesStoringTheReturnOfExtractedMethod;
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

	private void findVariableSplits() {
		Map<SplitVariableReplacement, Set<AbstractCodeMapping>> splitMap = new LinkedHashMap<SplitVariableReplacement, Set<AbstractCodeMapping>>();
		Map<String, Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>> variableInvocationExpressionMap = new LinkedHashMap<String, Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof SplitVariableReplacement) {
					SplitVariableReplacement split = (SplitVariableReplacement)replacement;
					if(splitMap.containsKey(split)) {
						splitMap.get(split).add(mapping);
					}
					else {
						Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
						mappings.add(mapping);
						splitMap.put(split, mappings);
					}
				}
				else if(replacement instanceof VariableReplacementWithMethodInvocation) {
					VariableReplacementWithMethodInvocation variableReplacement = (VariableReplacementWithMethodInvocation)replacement;
					processVariableReplacementWithMethodInvocation(variableReplacement, mapping, variableInvocationExpressionMap, Direction.INVOCATION_TO_VARIABLE);
				}
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
					for(AbstractCodeFragment statement : nonMappedLeavesT1) {
						VariableDeclaration variableDeclaration = statement.getVariableDeclaration(replacement.getBefore());
						if(variableDeclaration != null) {
							AbstractExpression initializer = variableDeclaration.getInitializer();
							if(initializer != null) {
								AbstractCall invocation = initializer.invocationCoveringEntireFragment();
								if(invocation != null) {
									VariableReplacementWithMethodInvocation variableReplacement = new VariableReplacementWithMethodInvocation(initializer.getString(), replacement.getAfter(), invocation, Direction.INVOCATION_TO_VARIABLE);
									processVariableReplacementWithMethodInvocation(variableReplacement, mapping, variableInvocationExpressionMap, Direction.INVOCATION_TO_VARIABLE);
								}
							}
						}
					}
				}
			}
		}
		for(AbstractCodeFragment statement : nonMappedLeavesT1) {
			for(String parameterName : operation2.getParameterNameList()) {
				VariableDeclaration variableDeclaration = statement.getVariableDeclaration(parameterName);
				if(variableDeclaration != null) {
					AbstractExpression initializer = variableDeclaration.getInitializer();
					if(initializer != null) {
						AbstractCall invocation = initializer.invocationCoveringEntireFragment();
						if(invocation != null) {
							String expression = invocation.getExpression();
							if(expression != null) {
								VariableReplacementWithMethodInvocation variableReplacement = new VariableReplacementWithMethodInvocation(initializer.getString(), parameterName, invocation, Direction.INVOCATION_TO_VARIABLE);
								processVariableReplacementWithMethodInvocation(variableReplacement, null, variableInvocationExpressionMap, Direction.INVOCATION_TO_VARIABLE);
							}
						}
					}
				}
			}
		}
		for(String key : variableInvocationExpressionMap.keySet()) {
			Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(key);
			Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
			Set<String> splitVariables = new LinkedHashSet<String>();
			for(VariableReplacementWithMethodInvocation replacement : map.keySet()) {
				if(!PrefixSuffixUtils.normalize(key).equals(PrefixSuffixUtils.normalize(replacement.getAfter()))) {
					splitVariables.add(replacement.getAfter());
					mappings.addAll(map.get(replacement));
				}
			}
			if(splitVariables.size() > 0) {
				SplitVariableReplacement split = new SplitVariableReplacement(key, splitVariables);
				splitMap.put(split, mappings);
			}
		}
		for(SplitVariableReplacement split : splitMap.keySet()) {
			Set<VariableDeclaration> splitVariables = new LinkedHashSet<VariableDeclaration>();
			Set<VariableDeclarationContainer> splitVariableOperations = new LinkedHashSet<VariableDeclarationContainer>();
			for(String variableName : split.getSplitVariables()) {
				SimpleEntry<VariableDeclaration,VariableDeclarationContainer> declaration = getVariableDeclaration2(split, variableName);
				if(declaration != null) {
					splitVariables.add(declaration.getKey());
					splitVariableOperations.add(declaration.getValue());
				}
			}
			Set<SimpleEntry<VariableDeclaration,VariableDeclarationContainer>> oldVariables = getVariableDeclaration1(split);
			SimpleEntry<VariableDeclaration,VariableDeclarationContainer> oldVariable = !oldVariables.isEmpty() ? oldVariables.iterator().next() : null;
			if(splitVariables.size() > 1 && splitVariables.size() == split.getSplitVariables().size() && oldVariable != null) {
				VariableDeclarationContainer operationAfter = splitVariableOperations.iterator().next();
				SplitVariableRefactoring refactoring = new SplitVariableRefactoring(oldVariable.getKey(), splitVariables, oldVariable.getValue(), operationAfter, splitMap.get(split), insideExtractedOrInlinedMethod);
				if(!existsConflictingExtractVariableRefactoring(refactoring) && !existsConflictingParameterRenameInOperationDiff(refactoring)) {
					for(VariableDeclaration removedVariable : removedVariables) {
						for(VariableDeclaration addedVariable : splitVariables) {
							if(removedVariable.getVariableName().equals(addedVariable.getVariableName()) && removedVariable.equalType(addedVariable)) {
								Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(removedVariable, addedVariable, mappings);
								matchedVariables.add(Pair.of(removedVariable, addedVariable));
								getVariableRefactorings(removedVariable, addedVariable, oldVariable.getValue(), operationAfter, variableReferences, null);
								break;
							}
						}
					}
					for(VariableDeclaration addedVariable : addedVariables) {
						VariableDeclaration removedVariable = oldVariable.getKey();
						if(removedVariable.getVariableName().equals(addedVariable.getVariableName()) && removedVariable.equalType(addedVariable)) {
							Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(removedVariable, addedVariable, mappings);
							matchedVariables.add(Pair.of(removedVariable, addedVariable));
							getVariableRefactorings(removedVariable, addedVariable, oldVariable.getValue(), operationAfter, variableReferences, null);
							break;
						}
					}
					variableSplits.add(refactoring);
					removedVariables.remove(oldVariable.getKey());
					addedVariables.removeAll(splitVariables);
				}
			}
			else {
				CandidateSplitVariableRefactoring candidate = new CandidateSplitVariableRefactoring(split.getBefore(), split.getSplitVariables(), operation1, operation2, splitMap.get(split));
				candidateAttributeSplits.add(candidate);
			}
		}
	}

	private void processVariableReplacementWithMethodInvocation(
			VariableReplacementWithMethodInvocation variableReplacement, AbstractCodeMapping mapping,
			Map<String, Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>> variableInvocationExpressionMap, Direction direction) {
		String expression = variableReplacement.getInvokedOperation().getExpression();
		if(expression != null && variableReplacement.getDirection().equals(direction)) {
			if(variableInvocationExpressionMap.containsKey(expression)) {
				Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(expression);
				if(map.containsKey(variableReplacement)) {
					if(mapping != null) {
						map.get(variableReplacement).add(mapping);
					}
				}
				else {
					Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
					if(mapping != null) {
						mappings.add(mapping);
					}
					map.put(variableReplacement, mappings);
				}
			}
			else {
				Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
				if(mapping != null) {
					mappings.add(mapping);
				}
				Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>> map = new LinkedHashMap<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>();
				map.put(variableReplacement, mappings);
				variableInvocationExpressionMap.put(expression, map);
			}
		}
	}

	private void findVariableMerges() {
		Map<MergeVariableReplacement, Set<AbstractCodeMapping>> mergeMap = new LinkedHashMap<MergeVariableReplacement, Set<AbstractCodeMapping>>();
		Map<String, Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>> variableInvocationExpressionMap = new LinkedHashMap<String, Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>>();
		Map<String, Map<Replacement, Set<AbstractCodeMapping>>> variableInvocationVariableMap = new LinkedHashMap<String, Map<Replacement, Set<AbstractCodeMapping>>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof MergeVariableReplacement) {
					MergeVariableReplacement merge = (MergeVariableReplacement)replacement;
					if(mergeMap.containsKey(merge)) {
						mergeMap.get(merge).add(mapping);
					}
					else {
						Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
						mappings.add(mapping);
						mergeMap.put(merge, mappings);
					}
				}
				else if(replacement instanceof VariableReplacementWithMethodInvocation) {
					VariableReplacementWithMethodInvocation variableReplacement = (VariableReplacementWithMethodInvocation)replacement;
					processVariableReplacementWithMethodInvocation(variableReplacement, mapping, variableInvocationExpressionMap, Direction.VARIABLE_TO_INVOCATION);
				}
				else if(replacement instanceof MethodInvocationReplacement) {
					MethodInvocationReplacement invocationReplacement = (MethodInvocationReplacement)replacement;
					AbstractCall invocationBefore = invocationReplacement.getInvokedOperationBefore();
					AbstractCall invocationAfter = invocationReplacement.getInvokedOperationAfter();
					if(invocationBefore.identicalName(invocationAfter) && invocationBefore.identicalExpression(invocationAfter) && !invocationBefore.equalArguments(invocationAfter)) {
						Set<String> argumentIntersection = new LinkedHashSet<String>(invocationBefore.getArguments());
						argumentIntersection.retainAll(invocationAfter.getArguments());
						Set<String> arguments1WithoutCommon = new LinkedHashSet<String>(invocationBefore.getArguments());
						arguments1WithoutCommon.removeAll(argumentIntersection);
						Set<String> arguments2WithoutCommon = new LinkedHashSet<String>(invocationAfter.getArguments());
						arguments2WithoutCommon.removeAll(argumentIntersection);
						if(arguments1WithoutCommon.size() > arguments2WithoutCommon.size() && arguments2WithoutCommon.size() == 1) {
							MergeVariableReplacement merge = new MergeVariableReplacement(arguments1WithoutCommon, arguments2WithoutCommon.iterator().next());
							if(mergeMap.containsKey(merge)) {
								mergeMap.get(merge).add(mapping);
							}
							else {
								Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
								mappings.add(mapping);
								mergeMap.put(merge, mappings);
							}
						}
					}
				}
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
					for(AbstractCodeFragment statement : nonMappedLeavesT2) {
						VariableDeclaration variableDeclaration = statement.getVariableDeclaration(replacement.getBefore());
						if(variableDeclaration != null) {
							AbstractExpression initializer = variableDeclaration.getInitializer();
							if(initializer != null) {
								AbstractCall invocation = initializer.invocationCoveringEntireFragment();
								if(invocation != null) {
									VariableReplacementWithMethodInvocation variableReplacement = new VariableReplacementWithMethodInvocation(replacement.getBefore(), initializer.getString(), invocation, Direction.VARIABLE_TO_INVOCATION);
									processVariableReplacementWithMethodInvocation(variableReplacement, mapping, variableInvocationExpressionMap, Direction.VARIABLE_TO_INVOCATION);
								}
							}
						}
					}
					if(replacement.getAfter().contains(".")) {
						String compositeVariable = replacement.getAfter().substring(0, replacement.getAfter().indexOf("."));
						if(variableInvocationVariableMap.containsKey(compositeVariable)) {
							Map<Replacement, Set<AbstractCodeMapping>> map = variableInvocationVariableMap.get(compositeVariable);
							if(map.containsKey(replacement)) {
								if(mapping != null) {
									map.get(replacement).add(mapping);
								}
							}
							else {
								Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
								if(mapping != null) {
									mappings.add(mapping);
								}
								map.put(replacement, mappings);
							}
						}
						else {
							Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
							if(mapping != null) {
								mappings.add(mapping);
							}
							Map<Replacement, Set<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, Set<AbstractCodeMapping>>();
							map.put(replacement, mappings);
							variableInvocationVariableMap.put(compositeVariable, map);
						}
					}
				}
			}
		}
		for(AbstractCodeFragment statement : nonMappedLeavesT2) {
			boolean matchingParameterFound = false;
			for(String parameterName : operation1.getParameterNameList()) {
				VariableDeclaration variableDeclaration = statement.getVariableDeclaration(parameterName);
				if(variableDeclaration != null) {
					matchingParameterFound = true;
					AbstractExpression initializer = variableDeclaration.getInitializer();
					if(initializer != null) {
						AbstractCall invocation = initializer.invocationCoveringEntireFragment();
						if(invocation != null) {
							VariableReplacementWithMethodInvocation variableReplacement = new VariableReplacementWithMethodInvocation(parameterName, initializer.getString(), invocation, Direction.VARIABLE_TO_INVOCATION);
							processVariableReplacementWithMethodInvocation(variableReplacement, null, variableInvocationExpressionMap, Direction.VARIABLE_TO_INVOCATION);
						}
					}
				}
			}
			if(!matchingParameterFound && statement.getVariableDeclarations().size() > 0) {	
				VariableDeclaration variableDeclaration = statement.getVariableDeclarations().get(0);
				for(VariableDeclaration parameterDeclaration : operation1.getParameterDeclarationList()) {
					if(variableDeclaration.equalType(parameterDeclaration)) {
						AbstractExpression initializer = variableDeclaration.getInitializer();
						UMLType parameterType = parameterDeclaration.getType();
						if(initializer != null && parameterType != null && initializer.getString().startsWith("(" + parameterType + ")")) {
							AbstractCall invocation = initializer.invocationCoveringEntireFragment();
							if(invocation != null) {
								VariableReplacementWithMethodInvocation variableReplacement = new VariableReplacementWithMethodInvocation(parameterDeclaration.getVariableName(), initializer.getString(), invocation, Direction.VARIABLE_TO_INVOCATION);
								processVariableReplacementWithMethodInvocation(variableReplacement, null, variableInvocationExpressionMap, Direction.VARIABLE_TO_INVOCATION);
							}
						}
					}
				}
			}
		}
		for(String key : variableInvocationExpressionMap.keySet()) {
			Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(key);
			Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
			Set<String> mergedVariables = new LinkedHashSet<String>();
			for(VariableReplacementWithMethodInvocation replacement : map.keySet()) {
				if(!PrefixSuffixUtils.normalize(key).equals(PrefixSuffixUtils.normalize(replacement.getBefore())) ||
						replacement.getInvokedOperation().getCoverage().equals(AbstractCall.StatementCoverageType.CAST_CALL)) {
					mergedVariables.add(replacement.getBefore());
					mappings.addAll(map.get(replacement));
				}
			}
			if(mergedVariables.size() > 0) {
				MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
				mergeMap.put(merge, mappings);
			}
		}
		for(String key : variableInvocationVariableMap.keySet()) {
			Map<Replacement, Set<AbstractCodeMapping>> map = variableInvocationVariableMap.get(key);
			Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
			Set<String> mergedVariables = new LinkedHashSet<String>();
			for(Replacement replacement : map.keySet()) {
				if(!PrefixSuffixUtils.normalize(key).equals(PrefixSuffixUtils.normalize(replacement.getBefore()))) {
					mergedVariables.add(replacement.getBefore());
					mappings.addAll(map.get(replacement));
				}
			}
			if(mergedVariables.size() > 0) {
				MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
				mergeMap.put(merge, mappings);
			}
		}
		for(MergeVariableReplacement merge : mergeMap.keySet()) {
			Set<VariableDeclaration> mergedVariables = new LinkedHashSet<VariableDeclaration>();
			Set<VariableDeclarationContainer> mergedVariableOperations = new LinkedHashSet<VariableDeclarationContainer>();
			for(String variableName : merge.getMergedVariables()) {
				SimpleEntry<VariableDeclaration,VariableDeclarationContainer> declaration = getVariableDeclaration1(merge, variableName);
				if(declaration != null) {
					mergedVariables.add(declaration.getKey());
					mergedVariableOperations.add(declaration.getValue());
				}
			}
			SimpleEntry<VariableDeclaration,VariableDeclarationContainer> newVariable = getVariableDeclaration2(merge);
			if(mergedVariables.size() > 1 && mergedVariables.size() == merge.getMergedVariables().size() && newVariable != null) {
				VariableDeclarationContainer operationBefore = mergedVariableOperations.iterator().next();
				MergeVariableRefactoring refactoring = new MergeVariableRefactoring(mergedVariables, newVariable.getKey(), operationBefore, newVariable.getValue(), mergeMap.get(merge), insideExtractedOrInlinedMethod);
				if(!existsConflictingInlineVariableRefactoring(refactoring) && !existsConflictingParameterRenameInOperationDiff(refactoring, variableInvocationExpressionMap)) {
					for(VariableDeclaration removedVariable : removedVariables) {
						VariableDeclaration addedVariable = newVariable.getKey();
						if(removedVariable.getVariableName().equals(addedVariable.getVariableName()) && removedVariable.equalType(addedVariable)) {
							Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(removedVariable, addedVariable, mappings);
							matchedVariables.add(Pair.of(removedVariable, addedVariable));
							getVariableRefactorings(removedVariable, addedVariable, operationBefore, newVariable.getValue(), variableReferences, null);
							break;
						}
					}
					for(VariableDeclaration addedVariable : addedVariables) {
						for(VariableDeclaration removedVariable : mergedVariables) {
							if(removedVariable.getVariableName().equals(addedVariable.getVariableName()) && removedVariable.equalType(addedVariable)) {
								Set<AbstractCodeMapping> variableReferences = VariableReferenceExtractor.findReferences(removedVariable, addedVariable, mappings);
								matchedVariables.add(Pair.of(removedVariable, addedVariable));
								getVariableRefactorings(removedVariable, addedVariable, operationBefore, newVariable.getValue(), variableReferences, null);
								break;
							}
						}
					}
					variableMerges.add(refactoring);
					removedVariables.removeAll(mergedVariables);
					addedVariables.remove(newVariable.getKey());
					VariableDeclaration firstMergedVariable = null;
					boolean allMergedVariablesHaveEqualAnnotations = true;
					for(VariableDeclaration mergedVariable : mergedVariables) {
						if(firstMergedVariable == null) {
							firstMergedVariable = mergedVariable;
						}
						else if(!firstMergedVariable.getAnnotations().equals(mergedVariable.getAnnotations())) {
							allMergedVariablesHaveEqualAnnotations = false;
							break;
						}
					}
					if(allMergedVariablesHaveEqualAnnotations) {
						UMLAnnotationListDiff annotationListDiff = new UMLAnnotationListDiff(firstMergedVariable.getAnnotations(), newVariable.getKey().getAnnotations());
						for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
							AddVariableAnnotationRefactoring ref = new AddVariableAnnotationRefactoring(annotation, firstMergedVariable, newVariable.getKey(), operationBefore, newVariable.getValue(), insideExtractedOrInlinedMethod);
							refactorings.add(ref);
						}
						for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
							RemoveVariableAnnotationRefactoring ref = new RemoveVariableAnnotationRefactoring(annotation, firstMergedVariable, newVariable.getKey(), operationBefore, newVariable.getValue(), insideExtractedOrInlinedMethod);
							refactorings.add(ref);
						}
						for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
							ModifyVariableAnnotationRefactoring ref = new ModifyVariableAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), firstMergedVariable, newVariable.getKey(), operationBefore, newVariable.getValue(), insideExtractedOrInlinedMethod);
							refactorings.add(ref);
						}
					}
				}
			}
			else {
				CandidateMergeVariableRefactoring candidate = new CandidateMergeVariableRefactoring(merge.getMergedVariables(), merge.getAfter(), operation1, operation2, mergeMap.get(merge));
				candidateAttributeMerges.add(candidate);
			}
		}
	}

	private void findConsistentVariableRenames() {
		Map<Replacement, Set<AbstractCodeMapping>> variableDeclarationReplacementOccurrenceMap = getVariableDeclarationReplacementOccurrenceMap();
		Set<Replacement> allConsistentVariableDeclarationRenames = allConsistentRenames(variableDeclarationReplacementOccurrenceMap);
		for(Replacement replacement : allConsistentVariableDeclarationRenames) {
			VariableDeclarationReplacement vdReplacement = (VariableDeclarationReplacement)replacement;
			Set<AbstractCodeMapping> variableReferences = variableDeclarationReplacementOccurrenceMap.get(vdReplacement);
			VariableDeclaration variableDeclaration1 = vdReplacement.getVariableDeclaration1();
			VariableDeclaration variableDeclaration2 = vdReplacement.getVariableDeclaration2();
			VariableDeclarationContainer operation1 = vdReplacement.getOperation1();
			VariableDeclarationContainer operation2 = vdReplacement.getOperation2();
			if((variableReferences.size() > 1 && consistencyCheck(variableDeclaration1, variableDeclaration2, variableReferences)) ||
					(variableReferences.size() == 1 && replacementInLocalVariableDeclaration(vdReplacement.getVariableNameReplacement(), variableReferences))) {
				RenameVariableRefactoring ref = new RenameVariableRefactoring(variableDeclaration1, variableDeclaration2, operation1, operation2, variableReferences, insideExtractedOrInlinedMethod);
				if(!existsConflictingExtractVariableRefactoring(ref) && !existsConflictingMergeVariableRefactoring(ref) && !existsConflictingSplitVariableRefactoring(ref) && !existsConflictingParameter(ref)) {
					variableRenames.add(ref);
					removedVariables.remove(variableDeclaration1);
					addedVariables.remove(variableDeclaration2);
					getVariableRefactorings(variableDeclaration1, variableDeclaration2, operation1, operation2, variableReferences, ref);
				}
			}
			else {
				RenameVariableRefactoring ref = new RenameVariableRefactoring(variableDeclaration1, variableDeclaration2, operation1, operation2, variableReferences, insideExtractedOrInlinedMethod);
				if(refactorings.contains(ref)) {
					refactorings.remove(ref);
				}
			}
		}
		Map<Replacement, Set<AbstractCodeMapping>> replacementOccurrenceMap = getReplacementOccurrenceMap(ReplacementType.VARIABLE_NAME);
		Set<Replacement> allConsistentRenames = allConsistentRenames(replacementOccurrenceMap);
		Map<Replacement, Set<AbstractCodeMapping>> finalConsistentRenames = new LinkedHashMap<Replacement, Set<AbstractCodeMapping>>();
		for(Replacement replacement : allConsistentRenames) {
			Set<SimpleEntry<VariableDeclaration, VariableDeclarationContainer>> v1s = getVariableDeclaration1(replacement);
			SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v1 = !v1s.isEmpty() ? v1s.iterator().next() : null;
			SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v2 = getVariableDeclaration2(replacement);
			Set<AbstractCodeMapping> set = replacementOccurrenceMap.get(replacement);
			if((set.size() > 1 && v1 != null && v2 != null && consistencyCheck(v1.getKey(), v2.getKey(), set)) ||
					potentialParameterRename(replacement, set) ||
					v1 == null || v2 == null ||
					(set.size() == 1 && replacementInLocalVariableDeclaration(replacement, set))) {
				finalConsistentRenames.put(replacement, set);
			}
			if(v1 != null && !v1.getKey().isParameter() && v2 != null && v2.getKey().isParameter() && consistencyCheck(v1.getKey(), v2.getKey(), set) &&
					!operation1.getParameterNameList().contains(v2.getKey().getVariableName())) {
				finalConsistentRenames.put(replacement, set);
			}
		}
		for(Replacement replacement : finalConsistentRenames.keySet()) {
			Set<SimpleEntry<VariableDeclaration, VariableDeclarationContainer>> v1s = getVariableDeclaration1(replacement);
			SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v2 = getVariableDeclaration2(replacement);
			Set<AbstractCodeMapping> variableReferences = finalConsistentRenames.get(replacement);
			if(!v1s.isEmpty() && v2 != null) {
				for(SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v1 : v1s) {
					VariableDeclaration variableDeclaration1 = v1.getKey();
					VariableDeclaration variableDeclaration2 = v2.getKey();
					VariableDeclarationContainer operation1 = v1.getValue();
					VariableDeclarationContainer operation2 = v2.getValue();
					Set<AbstractCodeMapping> actualReferences = new LinkedHashSet<>();
					for(AbstractCodeMapping mapping : variableReferences) {
						if(variableDeclaration1.getScope().subsumes(mapping.getFragment1().getLocationInfo()) &&
								variableDeclaration2.getScope().subsumes(mapping.getFragment2().getLocationInfo())) {
							actualReferences.add(mapping);
						}
					}
					RenameVariableRefactoring ref = new RenameVariableRefactoring(variableDeclaration1, variableDeclaration2, operation1, operation2, actualReferences, insideExtractedOrInlinedMethod);
					if(!existsConflictingExtractVariableRefactoring(ref) && !existsConflictingMergeVariableRefactoring(ref) && !existsConflictingSplitVariableRefactoring(ref) && !existsConflictingParameter(ref) &&
							variableDeclaration1.isVarargsParameter() == variableDeclaration2.isVarargsParameter()) {
						variableRenames.add(ref);
						removedVariables.remove(variableDeclaration1);
						addedVariables.remove(variableDeclaration2);
						getVariableRefactorings(variableDeclaration1, variableDeclaration2, operation1, operation2, actualReferences, ref);
					}
				}
			}
			else if(!PrefixSuffixUtils.normalize(replacement.getBefore()).equals(PrefixSuffixUtils.normalize(replacement.getAfter())) &&
					(!operation1.getAllVariables().contains(replacement.getAfter()) || cyclicRename(finalConsistentRenames.keySet(), replacement)) &&
					(!operation2.getAllVariables().contains(replacement.getBefore()) || cyclicRename(finalConsistentRenames.keySet(), replacement)) &&
					!fieldAssignmentWithPreviouslyExistingParameter(replacementOccurrenceMap.get(replacement)) &&
					!fieldAssignmentToPreviouslyExistingAttribute(replacementOccurrenceMap.get(replacement))) {
				CandidateAttributeRefactoring candidate = new CandidateAttributeRefactoring(
						replacement.getBefore(), replacement.getAfter(), operation1, operation2,
						replacementOccurrenceMap.get(replacement));
				SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v1 = !v1s.isEmpty() ? v1s.iterator().next() : null;
				if(v1 != null)
					candidate.setOriginalVariableDeclaration(v1.getKey());
				if(v2 != null)
					candidate.setRenamedVariableDeclaration(v2.getKey());
				this.candidateAttributeRenames.add(candidate);
			}
		}
	}

	private void getVariableRefactorings(VariableDeclaration variableDeclaration1,
			VariableDeclaration variableDeclaration2, VariableDeclarationContainer operation1, VariableDeclarationContainer operation2,
			Set<AbstractCodeMapping> variableReferences, RenameVariableRefactoring ref) {
		for(UMLAnonymousClass anonymous : operation1.getAnonymousClassList()) {
			for(UMLOperation operation : anonymous.getOperations()) {
				int subsumedReferences = 0;
				for(AbstractCodeMapping mapping : variableReferences) {
					if(operation.getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo())) {
						subsumedReferences++;
					}
				}
				if(subsumedReferences > 0 && subsumedReferences == variableReferences.size() && operation.getAllVariableDeclarations().contains(variableDeclaration1)) {
					operation1 = operation;
					for(VariableDeclaration parameter : operation1.getParameterDeclarationList()) {
						if(parameter.equals(variableDeclaration1)) {
							variableDeclaration1 = parameter;
						}
					}
					break;
				}
			}
		}
		for(UMLAnonymousClass anonymous : operation2.getAnonymousClassList()) {
			for(UMLOperation operation : anonymous.getOperations()) {
				int subsumedReferences = 0;
				for(AbstractCodeMapping mapping : variableReferences) {
					if(operation.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())) {
						subsumedReferences++;
					}
				}
				if(subsumedReferences > 0 && subsumedReferences == variableReferences.size() && operation.getAllVariableDeclarations().contains(variableDeclaration2)) {
					operation2 = operation;
					for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
						if(parameter.equals(variableDeclaration2)) {
							variableDeclaration2 = parameter;
						}
					}
					break;
				}
			}
		}
		if(ref == null && variableDeclaration1.isParameter() != variableDeclaration2.isParameter()) {
			RenameVariableRefactoring refactoring = new RenameVariableRefactoring(variableDeclaration1, variableDeclaration2, operation1, operation2, variableReferences, insideExtractedOrInlinedMethod);
			refactorings.add(refactoring);
		}
		if(variableDeclaration1.getType() != null && variableDeclaration2.getType() != null) {
			if(!variableDeclaration1.equalType(variableDeclaration2) || !variableDeclaration1.equalQualifiedType(variableDeclaration2)) {
				ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(variableDeclaration1, variableDeclaration2, operation1, operation2, variableReferences, insideExtractedOrInlinedMethod);
				if(ref != null) {
					refactoring.addRelatedRefactoring(ref);
				}
				refactorings.add(refactoring);
			}
		}
		UMLAnnotationListDiff annotationListDiff = new UMLAnnotationListDiff(variableDeclaration1.getAnnotations(), variableDeclaration2.getAnnotations());
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, variableDeclaration1, variableDeclaration2, operation1, operation2, insideExtractedOrInlinedMethod);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, variableDeclaration1, variableDeclaration2, operation1, operation2, insideExtractedOrInlinedMethod);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), variableDeclaration1, variableDeclaration2, operation1, operation2, insideExtractedOrInlinedMethod);
			refactorings.add(refactoring);
		}
		if(variableDeclaration1.isFinal() != variableDeclaration2.isFinal()) {
			if(variableDeclaration2.isFinal()) {
				AddVariableModifierRefactoring refactoring = new AddVariableModifierRefactoring("final", variableDeclaration1, variableDeclaration2, operation1, operation2, insideExtractedOrInlinedMethod);
				refactorings.add(refactoring);
			}
			else if(variableDeclaration1.isFinal()) {
				RemoveVariableModifierRefactoring refactoring = new RemoveVariableModifierRefactoring("final", variableDeclaration1, variableDeclaration2, operation1, operation2, insideExtractedOrInlinedMethod);
				refactorings.add(refactoring);
			}
		}
	}

	private boolean fieldAssignmentToPreviouslyExistingAttribute(Set<AbstractCodeMapping> mappings) {
		if(mappings.size() == 1) {
			AbstractCodeMapping mapping = mappings.iterator().next();
			String fragment1 = mapping.getFragment1().getString();
			String fragment2 = mapping.getFragment2().getString();
			if(fragment1.contains("=") && fragment1.endsWith(";\n") && fragment2.contains("=") && fragment2.endsWith(";\n")) {
				String value1 = fragment1.substring(fragment1.indexOf("=")+1, fragment1.lastIndexOf(";\n"));
				String value2 = fragment2.substring(fragment2.indexOf("=")+1, fragment2.lastIndexOf(";\n"));
				String attribute1 = PrefixSuffixUtils.normalize(fragment1.substring(0, fragment1.indexOf("=")));
				String attribute2 = PrefixSuffixUtils.normalize(fragment2.substring(0, fragment2.indexOf("=")));
				if(value1.equals(attribute1) && classDiff != null && classDiff.getOriginalClass().containsAttributeWithName(attribute1) && classDiff.getNextClass().containsAttributeWithName(attribute1)) {
					return true;
				}
				if(value2.equals(attribute2) && classDiff != null && classDiff.getOriginalClass().containsAttributeWithName(attribute2) && classDiff.getNextClass().containsAttributeWithName(attribute2)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean fieldAssignmentWithPreviouslyExistingParameter(Set<AbstractCodeMapping> mappings) {
		if(mappings.size() == 1) {
			AbstractCodeMapping mapping = mappings.iterator().next();
			String fragment1 = mapping.getFragment1().getString();
			String fragment2 = mapping.getFragment2().getString();
			if(fragment1.contains("=") && fragment1.endsWith(";\n") && fragment2.contains("=") && fragment2.endsWith(";\n")) {
				String value1 = fragment1.substring(fragment1.indexOf("=")+1, fragment1.lastIndexOf(";\n"));
				String value2 = fragment2.substring(fragment2.indexOf("=")+1, fragment2.lastIndexOf(";\n"));
				if(operation1.getParameterNameList().contains(value1) && operation2.getParameterNameList().contains(value1) && operationDiff != null) {
					for(UMLParameter addedParameter : operationDiff.getAddedParameters()) {
						if(addedParameter.getName().equals(value2)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private Map<Replacement, Set<AbstractCodeMapping>> getReplacementOccurrenceMap(ReplacementType type) {
		Map<Replacement, Set<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, Set<AbstractCodeMapping>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(type) && !returnVariableMapping(mapping, replacement) && !mapping.containsReplacement(ReplacementType.CONCATENATION) &&
						!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
						replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
					if(map.containsKey(replacement)) {
						map.get(replacement).add(mapping);
					}
					else {
						Set<AbstractCodeMapping> list = new LinkedHashSet<AbstractCodeMapping>();
						list.add(mapping);
						map.put(replacement, list);
					}
				}
				else if(replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS)) {
					String before = replacement.getBefore().contains("[") ? replacement.getBefore().substring(0, replacement.getBefore().indexOf("[")) : replacement.getBefore();
					String after = replacement.getAfter().contains("[") ? replacement.getAfter().substring(0, replacement.getAfter().indexOf("[")) : replacement.getAfter();
					Replacement variableReplacement = new Replacement(before, after, ReplacementType.VARIABLE_NAME);
					if(!returnVariableMapping(mapping, replacement) &&
							!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
							replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
						if(map.containsKey(variableReplacement)) {
							map.get(variableReplacement).add(mapping);
						}
						else {
							Set<AbstractCodeMapping> list = new LinkedHashSet<AbstractCodeMapping>();
							list.add(mapping);
							map.put(variableReplacement, list);
						}
					}
				}
				else if(replacement.getType().equals(ReplacementType.METHOD_INVOCATION)) {
					MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
					AbstractCall invocation1 = methodInvocationReplacement.getInvokedOperationBefore();
					AbstractCall invocation2 = methodInvocationReplacement.getInvokedOperationAfter();
					if(invocation1.getName().equals(invocation2.getName()) && invocation1.getArguments().size() == invocation2.getArguments().size()) {
						for(int i=0; i<invocation1.getArguments().size(); i++) {
							String argument1 = invocation1.getArguments().get(i);
							String argument2 = invocation2.getArguments().get(i);
							if(argument1.contains("[") || argument2.contains("[")) {
								String before = argument1.contains("[") ? argument1.substring(0, argument1.indexOf("[")) : argument1;
								String after = argument2.contains("[") ? argument2.substring(0, argument2.indexOf("[")) : argument2;
								if(!before.equals(after)) {
									Replacement variableReplacement = new Replacement(before, after, ReplacementType.VARIABLE_NAME);
									if(!returnVariableMapping(mapping, replacement) &&
											!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
											replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
										if(map.containsKey(variableReplacement)) {
											map.get(variableReplacement).add(mapping);
										}
										else {
											Set<AbstractCodeMapping> list = new LinkedHashSet<AbstractCodeMapping>();
											list.add(mapping);
											map.put(variableReplacement, list);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return map;
	}

	private Map<Replacement, Set<AbstractCodeMapping>> getVariableDeclarationReplacementOccurrenceMap() {
		Map<Replacement, Set<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, Set<AbstractCodeMapping>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) && !returnVariableMapping(mapping, replacement) && !mapping.containsReplacement(ReplacementType.CONCATENATION) &&
						!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
						replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
					SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v1 = getVariableDeclaration1(replacement, mapping);
					SimpleEntry<VariableDeclaration, VariableDeclarationContainer> v2 = getVariableDeclaration2(replacement, mapping);
					if(v1 != null && v2 != null) {
						VariableDeclarationReplacement r = new VariableDeclarationReplacement(v1.getKey(), v2.getKey(), v1.getValue(), v2.getValue());
						if(map.containsKey(r)) {
							map.get(r).add(mapping);
						}
						else {
							Set<AbstractCodeMapping> list = new LinkedHashSet<AbstractCodeMapping>();
							list.add(mapping);
							map.put(r, list);
						}
					}
				}
			}
		}
		if(operationDiff != null) {
			List<UMLParameterDiff> allParameterDiffs = new ArrayList<UMLParameterDiff>();
			for(UMLParameterDiff parameterDiff : operationDiff.getParameterDiffList()) {
				if(parameterDiff.isNameChanged()) {
					allParameterDiffs.add(parameterDiff);
				}
			}
			List<UMLParameterDiff> matchedParameterDiffs = new ArrayList<UMLParameterDiff>();
			for(UMLParameterDiff parameterDiff : allParameterDiffs) {
				for(Replacement replacement : map.keySet()) {
					VariableDeclarationReplacement vdR = (VariableDeclarationReplacement)replacement;
					if(parameterDiff.getRemovedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration1()) &&
							parameterDiff.getAddedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration2())) {
						matchedParameterDiffs.add(parameterDiff);
						break;
					}
				}
			}
			Set<VariableDeclarationReplacement> keysToBeRemoved = new LinkedHashSet<VariableDeclarationReplacement>();
			for(UMLParameterDiff parameterDiff : matchedParameterDiffs) {
				for(Replacement replacement : map.keySet()) {
					VariableDeclarationReplacement vdR = (VariableDeclarationReplacement)replacement;
					if(parameterDiff.getRemovedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration1()) &&
							!parameterDiff.getAddedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration2())) {
						keysToBeRemoved.add(vdR);
					}
					else if(!parameterDiff.getRemovedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration1()) &&
							parameterDiff.getAddedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration2())) {
						keysToBeRemoved.add(vdR);
					}
				}
			}
			for(VariableDeclarationReplacement key : keysToBeRemoved) {
				map.remove(key);
			}
		}
		return map;
	}

	private boolean returnVariableMapping(AbstractCodeMapping mapping, Replacement replacement) {
		if(!operation1.isDeclaredInAnonymousClass() && !operation2.isDeclaredInAnonymousClass()) {
			return mapping.getFragment1().getString().equals("return " + replacement.getBefore() + ";\n") &&
					mapping.getFragment2().getString().equals("return " + replacement.getAfter() + ";\n");
		}
		return false;
	}

	private boolean containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(Set<Replacement> replacements) {
		for(Replacement replacement : replacements) {
			if(replacement instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement r = (MethodInvocationReplacement)replacement;
				if(r.differentExpressionNameAndArguments())
					return true;
			}
		}
		return false;
	}

	private boolean replacementNotInsideMethodSignatureOfAnonymousClass(AbstractCodeMapping mapping, Replacement replacement) {
		AbstractCodeFragment fragment1 = mapping.getFragment1();
		AbstractCodeFragment fragment2 = mapping.getFragment2();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = fragment1.getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = fragment2.getAnonymousClassDeclarations();
		if(anonymousClassDeclarations1.size() > 0 && anonymousClassDeclarations2.size() > 0) {
			boolean replacementBeforeNotFoundInMethodSignature = false;
			String[] lines1 = fragment1.getString().split("\\n");
			for(String line : lines1) {
				line = prepareLine(line);
				if(!Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches() &&
						ReplacementUtil.contains(line, replacement.getBefore())) {
					replacementBeforeNotFoundInMethodSignature = true;
					break;
				}
			}
			boolean replacementAfterNotFoundInMethodSignature = false;
			String[] lines2 = fragment2.getString().split("\\n");
			for(String line : lines2) {
				line = prepareLine(line);
				if(!Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches() &&
						ReplacementUtil.contains(line, replacement.getAfter())) {
					replacementAfterNotFoundInMethodSignature = true;
					break;
				}
			}
			return replacementBeforeNotFoundInMethodSignature && replacementAfterNotFoundInMethodSignature;
		}
		return true;
	}

	public static String prepareLine(String line) {
		line = line.trim();
		if(line.startsWith("@Nullable")) {
			line = line.substring(9, line.length());
			line = line.trim();
		}
		if(line.startsWith("@Override")) {
			line = line.substring(9, line.length());
			line = line.trim();
		}
		if(line.contains("throws ")) {
			line = line.substring(0, line.indexOf("throws "));
		}
		return line;
	}

	private static boolean cyclicRename(Set<Replacement> finalConsistentRenames, Replacement replacement) {
		for(Replacement r : finalConsistentRenames) {
			if(replacement.getAfter().equals(r.getBefore()))
				return true;
			if(replacement.getBefore().equals(r.getAfter()))
				return true;
		}
		return false;
	}

	private Set<Replacement> allConsistentRenames(Map<Replacement, Set<AbstractCodeMapping>> replacementOccurrenceMap) {
		Set<Replacement> renames = replacementOccurrenceMap.keySet();
		Set<Replacement> allConsistentRenames = new LinkedHashSet<Replacement>();
		Set<Replacement> allInconsistentRenames = new LinkedHashSet<Replacement>();
		ConsistentReplacementDetector.updateRenames(allConsistentRenames, allInconsistentRenames, aliasedVariablesInOriginalMethod, aliasedVariablesInNextMethod, renames);
		allConsistentRenames.removeAll(allInconsistentRenames);
		return allConsistentRenames;
	}

	private boolean replacementInLocalVariableDeclaration(Replacement replacement, Set<AbstractCodeMapping> set) {
		VariableDeclaration v1 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v1 = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				break;
			}
		}
		VariableDeclaration v2 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v2 = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				break;
			}
		}
		Set<VariableDeclaration> allVariableDeclarations1 = new LinkedHashSet<VariableDeclaration>();
		Set<VariableDeclaration> allVariableDeclarations2 = new LinkedHashSet<VariableDeclaration>();
		boolean onlyOneFragmentIncludesDeclarationInReferences = false;
		for(AbstractCodeMapping referenceMapping : set) {
			AbstractCodeFragment statement1 = referenceMapping.getFragment1();
			AbstractCodeFragment statement2 = referenceMapping.getFragment2();
			if(set.size() == 1) {
				if(statement1.getVariableDeclarations().contains(v1) && !statement2.getVariableDeclarations().contains(v2)) {
					if(v2 != null && v2.getInitializer() == null) {
						onlyOneFragmentIncludesDeclarationInReferences = true;
					}
				}
				if(!statement1.getVariableDeclarations().contains(v1) && statement2.getVariableDeclarations().contains(v2)) {
					if(v1 != null && v1.getInitializer() == null) {
						onlyOneFragmentIncludesDeclarationInReferences = true;
					}
				}
			}
			if(statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject &&
					statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
				CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
				CompositeStatementObject comp2 = (CompositeStatementObject)statement2;
				allVariableDeclarations1.addAll(comp1.getAllVariableDeclarations());
				allVariableDeclarations2.addAll(comp2.getAllVariableDeclarations());
			}
			else if(statement1 instanceof AbstractExpression && statement2 instanceof AbstractExpression &&
					statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.LAMBDA_EXPRESSION_BODY) &&
					statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.LAMBDA_EXPRESSION_BODY)) {
				AbstractExpression expr1 = (AbstractExpression)statement1;
				AbstractExpression expr2 = (AbstractExpression)statement2;
				if(expr1.getLambdaOwner() != null) {
					allVariableDeclarations1.addAll(expr1.getLambdaOwner().getParameters());
				}
				if(expr2.getLambdaOwner() != null) {
					allVariableDeclarations2.addAll(expr2.getLambdaOwner().getParameters());
				}
			}
			else {
				boolean parentMappingFound = false;
				for(AbstractCodeMapping mapping : mappings) {
					AbstractCodeFragment s1 = mapping.getFragment1();
					AbstractCodeFragment s2 = mapping.getFragment2();
					if(s1 instanceof CompositeStatementObject && s2 instanceof CompositeStatementObject) {
						CompositeStatementObject comp1 = (CompositeStatementObject)s1;
						CompositeStatementObject comp2 = (CompositeStatementObject)s2;
						if(comp1.getStatements().contains(statement1) && comp2.getStatements().contains(statement2)) {
							parentMappingFound = true;
							allVariableDeclarations1.addAll(comp1.getAllVariableDeclarations());
							allVariableDeclarations2.addAll(comp2.getAllVariableDeclarations());
							break;
						}
					}
				}
				if(!parentMappingFound) {
					allVariableDeclarations1.addAll(operation1.getAllVariableDeclarations());
					allVariableDeclarations2.addAll(operation2.getAllVariableDeclarations());
					break;
				}
			}
		}
		return v1 != null && v2 != null &&
				v1.equalVariableDeclarationType(v2) && !onlyOneFragmentIncludesDeclarationInReferences &&
				!containsVariableDeclarationWithName(v1, allVariableDeclarations1, v2) &&
				(!containsVariableDeclarationWithName(v2, allVariableDeclarations2, v1) || operation2.loopWithVariables(v1.getVariableName(), v2.getVariableName()) != null) &&
				consistencyCheck(v1, v2, set);
	}

	private boolean consistencyCheck(VariableDeclaration v1, VariableDeclaration v2, Set<AbstractCodeMapping> set) {
		return !variableAppearsInExtractedMethod(v1, v2) &&
				!variableAppearsInTheInitializerOfTheOtherVariable(v1, v2) &&
				!inconsistentVariableMapping(v1, v2, set);
	}

	private boolean variableAppearsInTheInitializerOfTheOtherVariable(VariableDeclaration v1, VariableDeclaration v2) {
		if(v1.getInitializer() != null) {
			if(v1.getInitializer().getString().equals(v2.getVariableName())) {
				return true;
			}
			if(v1.getInitializer().getTernaryOperatorExpressions().size() == 1) {
				TernaryOperatorExpression ternary = v1.getInitializer().getTernaryOperatorExpressions().get(0);
				if(ternary.getThenExpression().getVariables().contains(v2.getVariableName()) || ternary.getElseExpression().getVariables().contains(v2.getVariableName())) {
					boolean v2InitializerContainsThisReference = false;
					if(v2.getInitializer() != null && v2.getInitializer().getVariables().contains("this." + v2.getVariableName())) {
						v2InitializerContainsThisReference = true;
					}
					if(!v2InitializerContainsThisReference) {
						return true;
					}
				}
			}
		}
		if(v2.getInitializer() != null) {
			if(v2.getInitializer().getString().equals(v1.getVariableName())) {
				return true;
			}
			if(v2.getInitializer().getTernaryOperatorExpressions().size() == 1) {
				TernaryOperatorExpression ternary = v2.getInitializer().getTernaryOperatorExpressions().get(0);
				if(ternary.getThenExpression().getVariables().contains(v1.getVariableName()) || ternary.getElseExpression().getVariables().contains(v1.getVariableName())) {
					boolean v1InitializerContainsThisReference = false;
					if(v1.getInitializer() != null && v1.getInitializer().getVariables().contains("this." + v1.getVariableName())) {
						v1InitializerContainsThisReference = true;
					}
					if(!v1InitializerContainsThisReference) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean traditionalForToEnhancedForMapping(AbstractCodeMapping mapping) {
		if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
				mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT)) {
			return true;
		}
		else if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			return true;
		}
		return false;
	}

	private boolean inconsistentVariableMapping(VariableDeclaration v1, VariableDeclaration v2, Set<AbstractCodeMapping> set) {
		if(v1 != null && v2 != null) {
			for(AbstractCodeMapping mapping : mappings) {
				List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.contains(v1)) {
					if(variableDeclarations2.size() > 0 && !variableDeclarations2.contains(v2)) {
						if(!traditionalForToEnhancedForMapping(mapping)) {
							return true;
						}
					}
					else if(variableDeclarations2.size() == 0 && v1.getInitializer() != null &&
							mapping.getFragment2().getString().startsWith(v1.getInitializer().getString())) {
						return true;
					}
				}
				if(variableDeclarations2.contains(v2)) {
					if(variableDeclarations1.size() > 0 && !variableDeclarations1.contains(v1)) {
						if(!traditionalForToEnhancedForMapping(mapping)) {
							return true;
						}
					}
					else if(variableDeclarations1.size() == 0 && v2.getInitializer() != null &&
							mapping.getFragment1().getString().startsWith(v2.getInitializer().getString())) {
						return true;
					}
				}
				if(mapping.isExact()) {
					for(AbstractCodeMapping referenceMapping : set) {
						AbstractCodeFragment statement1 = referenceMapping.getFragment1();
						AbstractCodeFragment statement2 = referenceMapping.getFragment2();
						boolean containsMapping = true;
						if(statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject &&
								statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
							CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
							CompositeStatementObject comp2 = (CompositeStatementObject)statement2;
							containsMapping = comp1.contains(mapping.getFragment1()) && comp2.contains(mapping.getFragment2());
						}
						if(containsMapping && operation2.loopWithVariables(v1.getVariableName(), v2.getVariableName()) == null) {
							if(bothFragmentsUseVariable(v1, mapping)) {
								VariableDeclaration otherV1 = mapping.getFragment1().getVariableDeclaration(v1.getVariableName());
								if(otherV1 != null) {
									VariableScope otherV1Scope = otherV1.getScope();
									VariableScope v1Scope = v1.getScope();
									if(otherV1Scope.overlaps(v1Scope)) {
										return true;
									}
								}
								else if(set.size() == 1) {
									return true;
								}
							}
							if(bothFragmentsUseVariable(v2, mapping)) {
								VariableDeclaration otherV2 = mapping.getFragment2().getVariableDeclaration(v2.getVariableName());
								if(otherV2 != null) {
									VariableScope otherV2Scope = otherV2.getScope();
									VariableScope v2Scope = v2.getScope();
									if(otherV2Scope.overlaps(v2Scope)) {
										return true;
									}
								}
								else if(set.size() == 1) {
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

	public static boolean bothFragmentsUseVariable(VariableDeclaration v1, AbstractCodeMapping mapping) {
		return mapping.getFragment1().getVariables().contains(v1.getVariableName()) &&
				mapping.getFragment2().getVariables().contains(v1.getVariableName());
	}

	private static boolean containsVariableDeclarationWithName(VariableDeclaration variableDeclaration, Set<VariableDeclaration> variableDeclarations, VariableDeclaration other) {
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(other.getVariableName()) && declaration.equalVariableDeclarationType(other) && !invokingExpressionOrWrappedAsArgument(declaration.getInitializer(), variableDeclaration)) {
				return true;
			}
		}
		return false;
	}

	private static boolean invokingExpressionOrWrappedAsArgument(AbstractExpression initializer, VariableDeclaration variableDeclaration) {
		if(initializer != null) {
			AbstractCall invocation = initializer.invocationCoveringEntireFragment();
			if(invocation != null) {
				if(invocation.getArguments().contains(variableDeclaration.getVariableName())) {
					return true;
				}
				if(invocation.getExpression() != null && invocation.getExpression().equals(variableDeclaration.getVariableName())) {
					return true;
				}
			}
			ObjectCreation creation = initializer.creationCoveringEntireFragment();
			if(creation != null) {
				if(creation.getArguments().contains(variableDeclaration.getVariableName())) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<SimpleEntry<VariableDeclaration, VariableDeclarationContainer>> getVariableDeclaration1(Replacement replacement) {
		Set<SimpleEntry<VariableDeclaration, VariableDeclarationContainer>> set = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				VariableDeclaration vd = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				if(vd != null) {
					set.add(new SimpleEntry<>(vd, mapping.getOperation1()));
				}
			}
		}
		for(VariableDeclaration parameter : operation1.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(replacement.getBefore())) {
				set.add(new SimpleEntry<>(parameter, operation1));
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getBefore())) {
					set.add(new SimpleEntry<>(parameter, callSiteOperation));
				}
			}
		}
		return set;
	}

	private SimpleEntry<VariableDeclaration, VariableDeclarationContainer> getVariableDeclaration1(MergeVariableReplacement replacement, String variableName) {
		for(AbstractCodeMapping mapping : mappings) {
			Set<String> foundMergedVariables = new LinkedHashSet<String>();
			for(Replacement r : mapping.getReplacements()) {
				if(replacement.getMergedVariables().contains(r.getBefore())) {
					foundMergedVariables.add(r.getBefore());
				}
			}
			if(mapping.getReplacements().contains(replacement) || foundMergedVariables.equals(replacement.getMergedVariables())) {
				VariableDeclaration vd = mapping.getFragment1().searchVariableDeclaration(variableName);
				if(vd != null) {
					return new SimpleEntry<>(vd, mapping.getOperation1());
				}
			}
		}
		for(VariableDeclaration parameter : operation1.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(variableName)) {
				return new SimpleEntry<>(parameter, operation1);
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(variableName)) {
					return new SimpleEntry<>(parameter, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, VariableDeclarationContainer> getVariableDeclaration2(Replacement replacement) {
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				if(vd != null) {
					return new SimpleEntry<>(vd, mapping.getOperation2());
				}
			}
		}
		for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(replacement.getAfter())) {
				return new SimpleEntry<>(parameter, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getAfter())) {
					return new SimpleEntry<>(parameter, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, VariableDeclarationContainer> getVariableDeclaration2(SplitVariableReplacement replacement, String variableName) {
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				Set<String> foundSplitVariables = new LinkedHashSet<String>();
				for(Replacement r : mapping.getReplacements()) {
					if(replacement.getSplitVariables().contains(r.getAfter())) {
						foundSplitVariables.add(r.getAfter());
					}
				}
				if(mapping.getReplacements().contains(replacement) || foundSplitVariables.equals(replacement.getSplitVariables())) {
					VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(variableName);
					if(vd != null) {
						return new SimpleEntry<>(vd, mapping.getOperation2());
					}
				}
			}
		}
		for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(variableName)) {
				return new SimpleEntry<>(parameter, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(variableName)) {
					return new SimpleEntry<>(parameter, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, VariableDeclarationContainer> getVariableDeclaration2(MergeVariableReplacement replacement) {
		for(AbstractCodeMapping mapping : mappings) {
			Set<String> foundMergedVariables = new LinkedHashSet<String>();
			for(Replacement r : mapping.getReplacements()) {
				if(replacement.getMergedVariables().contains(r.getBefore())) {
					foundMergedVariables.add(r.getBefore());
				}
			}
			if(mapping.getReplacements().contains(replacement) || foundMergedVariables.equals(replacement.getMergedVariables())) {
				VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				if(vd != null) {
					return new SimpleEntry<>(vd, mapping.getOperation2());
				}
				else {
					for(Replacement r : mapping.getReplacements()) {
						if(r.getBefore().equals(replacement.getAfter())) {
							vd = mapping.getFragment2().searchVariableDeclaration(r.getAfter());
							if(vd != null) {
								return new SimpleEntry<>(vd, mapping.getOperation2());
							}
						}
					}
				}
			}
		}
		for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(replacement.getAfter())) {
				return new SimpleEntry<>(parameter, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getAfter())) {
					return new SimpleEntry<>(parameter, callSiteOperation);
				}
			}
		}
		return null;
	}

	private boolean variableAppearsInExtractedMethod(VariableDeclaration v1, VariableDeclaration v2) {
		if(v1 != null) {
			UMLModelDiff modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
			for(UMLOperationBodyMapper mapper : childMappers) {
				if(!mapper.equals(this.mapper)) {
					for(AbstractCodeMapping mapping : mapper.getMappings()) {
						if(mapping.getFragment1().getVariableDeclarations().contains(v1)) {
							boolean identicalNonMappedStatementInParentMapper = false;
							for(AbstractCodeFragment fragment2 : nonMappedLeavesT2) {
								if(fragment2.getString().equals(mapping.getFragment2().getString())) {
									identicalNonMappedStatementInParentMapper = true;
									break;
								}
							}
							if(v2 != null && v2.getInitializer() != null) {
								VariableDeclarationContainer extractedMethod = mapper.getContainer2();
								Map<String, List<AbstractCall>> methodInvocationMap = v2.getInitializer().getMethodInvocationMap();
								for(String key : methodInvocationMap.keySet()) {
									for(AbstractCall invocation : methodInvocationMap.get(key)) {
										if(invocation.matchesOperation(extractedMethod, operation2, modelDiff)) {
											return false;
										}
										else {
											//check if the extracted method is called in the initializer of a variable used in the initializer of v2
											List<String> initializerVariables = v2.getInitializer().getVariables();
											for(String variable : initializerVariables) {
												for(VariableDeclaration declaration : operation2.getAllVariableDeclarations()) {
													if(declaration.getVariableName().equals(variable) && declaration.getInitializer() != null) {
														Map<String, List<AbstractCall>> methodInvocationMap2 = declaration.getInitializer().getMethodInvocationMap();
														for(String key2 : methodInvocationMap2.keySet()) {
															for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
																if(invocation2.matchesOperation(extractedMethod, operation2, modelDiff)) {
																	return false;
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
							if(!identicalNonMappedStatementInParentMapper) {
								return true;
							}
						}
					}
					for(AbstractCodeFragment nonMappedStatement : mapper.getNonMappedLeavesT2()) {
						VariableDeclaration variableDeclaration2 = nonMappedStatement.getVariableDeclaration(v1.getVariableName());
						if(variableDeclaration2 != null && variableDeclaration2.equalType(v1)) {
							for(AbstractCodeMapping mapping : mapper.getMappings()) {
								if(mapping.getFragment2().equals(nonMappedStatement.getParent())) {
									if(mapping.getFragment1() instanceof CompositeStatementObject) {
										CompositeStatementObject composite1 = (CompositeStatementObject)mapping.getFragment1();
										List<AbstractCodeFragment> leaves1 = composite1.getLeaves();
										for(AbstractCodeFragment leaf1 : leaves1) {
											VariableDeclaration variableDeclaration1 = leaf1.getVariableDeclaration(variableDeclaration2.getVariableName());
											if(variableDeclaration1 != null) {
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
		return false;
	}

	private boolean existsConflictingParameterRenameInOperationDiff(MergeVariableRefactoring ref, Map<String, Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>>> variableInvocationExpressionMap) {
		if(operationDiff != null) {
			for(UMLParameterDiff parameterDiff : operationDiff.getParameterDiffList()) {
				if(ref.getMergedVariables().contains(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
						ref.getNewVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
					boolean castInvocation = false;
					String variableName = ref.getNewVariable().getVariableName();
					if(variableInvocationExpressionMap.containsKey(variableName)) {
						Map<VariableReplacementWithMethodInvocation, Set<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(variableName);
						for(VariableReplacementWithMethodInvocation replacement : map.keySet()) {
							if(replacement.getBefore().equals(variableName)) {
								if(replacement.getInvokedOperation().getCoverage().equals(AbstractCall.StatementCoverageType.CAST_CALL)) {
									castInvocation = true;
									break;
								}
							}
						}
					}
					if(!castInvocation) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean existsConflictingParameterRenameInOperationDiff(SplitVariableRefactoring ref) {
		if(operationDiff != null) {
			for(UMLParameterDiff parameterDiff : operationDiff.getParameterDiffList()) {
				if(ref.getSplitVariables().contains(parameterDiff.getAddedParameter().getVariableDeclaration()) &&
						ref.getOldVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration())) {
					return true;
					
				}
			}
		}
		return false;
	}

	private boolean existsConflictingExtractVariableRefactoring(RenameVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariableRef = (ExtractVariableRefactoring)refactoring;
				if(extractVariableRef.getVariableDeclaration().equals(ref.getRenamedVariable()) &&
						extractVariableRef.getOperationAfter().equals(ref.getOperationAfter())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean existsConflictingExtractVariableRefactoring(SplitVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariableRef = (ExtractVariableRefactoring)refactoring;
				if(ref.getSplitVariables().contains(extractVariableRef.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean existsConflictingInlineVariableRefactoring(MergeVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof InlineVariableRefactoring) {
				InlineVariableRefactoring inlineVariableRef = (InlineVariableRefactoring)refactoring;
				if(ref.getMergedVariables().contains(inlineVariableRef.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean existsConflictingMergeVariableRefactoring(RenameVariableRefactoring ref) {
		for(MergeVariableRefactoring merge : variableMerges) {
			if(merge.getOperationBefore().equals(ref.getOperationBefore()) &&
					merge.getOperationAfter().equals(ref.getOperationAfter()) &&
					merge.getMergedVariables().contains(ref.getOriginalVariable()) &&
					merge.getNewVariable().equals(ref.getRenamedVariable())) {
				return true;
			}
		}
		return false;
	}

	private boolean existsConflictingSplitVariableRefactoring(RenameVariableRefactoring ref) {
		for(SplitVariableRefactoring split : variableSplits) {
			if(split.getOperationBefore().equals(ref.getOperationBefore()) &&
					split.getOperationAfter().equals(ref.getOperationAfter()) &&
					split.getSplitVariables().contains(ref.getRenamedVariable()) &&
					split.getOldVariable().equals(ref.getOriginalVariable())) {
				return true;
			}
		}
		return false;
	}

	private boolean existsConflictingParameter(RenameVariableRefactoring ref) {
		VariableDeclaration renamedVariable = ref.getRenamedVariable();
		if(renamedVariable.isParameter()) {
			for(VariableDeclaration parameter : operation1.getParameterDeclarationList()) {
				if(renamedVariable.getVariableName().equals(parameter.getVariableName()) &&
						renamedVariable.equalType(parameter)) {
					return true;
				}
			}
		}
		VariableDeclaration originalVariable = ref.getOriginalVariable();
		if(originalVariable.isParameter()) {
			for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
				if(originalVariable.getVariableName().equals(parameter.getVariableName()) &&
						originalVariable.equalType(parameter)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean potentialParameterRename(Replacement replacement, Set<AbstractCodeMapping> set) {
		int index1 = operation1.getParameterNameList().indexOf(replacement.getBefore());
		if(index1 == -1 && callSiteOperation != null) {
			index1 = callSiteOperation.getParameterNameList().indexOf(replacement.getBefore());
		}
		int index2 = operation2.getParameterNameList().indexOf(replacement.getAfter());
		if(index2 == -1 && callSiteOperation != null) {
			index2 = callSiteOperation.getParameterNameList().indexOf(replacement.getAfter());
		}
		if(fieldAssignmentToPreviouslyExistingAttribute(set)) {
			return false;
		}
		if(fieldAssignmentWithPreviouslyExistingParameter(set)) {
			return false;
		}
		return index1 >= 0 && index1 == index2;
	}

	private SimpleEntry<VariableDeclaration, VariableDeclarationContainer> getVariableDeclaration1(Replacement replacement, AbstractCodeMapping mapping) {
		if(mapping.getReplacements().contains(replacement)) {
			VariableDeclaration vd = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
			if(vd != null) {
				return new SimpleEntry<>(vd, mapping.getOperation1());
			}
		}
		for(VariableDeclaration parameter : operation1.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(replacement.getBefore())) {
				return new SimpleEntry<>(parameter, operation1);
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getBefore())) {
					return new SimpleEntry<>(parameter, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, VariableDeclarationContainer> getVariableDeclaration2(Replacement replacement, AbstractCodeMapping mapping) {
		if(mapping.getReplacements().contains(replacement)) {
			VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
			if(vd != null) {
				return new SimpleEntry<>(vd, mapping.getOperation2());
			}
		}
		for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
			if(parameter.getVariableName().equals(replacement.getAfter())) {
				return new SimpleEntry<>(parameter, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(VariableDeclaration parameter : callSiteOperation.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getAfter())) {
					return new SimpleEntry<>(parameter, callSiteOperation);
				}
			}
		}
		return null;
	}
}
