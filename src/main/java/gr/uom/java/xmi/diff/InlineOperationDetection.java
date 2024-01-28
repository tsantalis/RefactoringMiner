package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class InlineOperationDetection {
	private UMLOperationBodyMapper mapper;
	private List<UMLOperation> removedOperations;
	private UMLAbstractClassDiff classDiff;
	private UMLModelDiff modelDiff;
	private List<AbstractCall> operationInvocations;
	private Map<CallTreeNode, CallTree> callTreeMap = new LinkedHashMap<CallTreeNode, CallTree>();
	private Map<UMLOperation, List<AbstractCall>> callCountMap = null;
	
	public InlineOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> removedOperations, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
		this.mapper = mapper;
		this.removedOperations = removedOperations;
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		this.operationInvocations = getInvocationsInTargetOperationBeforeInline(mapper);
	}

	public List<UMLOperation> getRemovedOperationsSortedByCalls() {
		this.callCountMap = new LinkedHashMap<>();
		List<UMLOperation> sorted = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		for(UMLOperation removedOperation : removedOperations) {
			List<AbstractCall> matchingInvocations = matchingInvocations(removedOperation, operationInvocations, mapper.getContainer1());
			if(!matchingInvocations.isEmpty()) {
				callCountMap.put(removedOperation, matchingInvocations);
				int count = matchingInvocations.size();
				if(sorted.isEmpty()) {
					sorted.add(removedOperation);
					counts.add(count);
				}
				else {
					boolean inserted = false;
					for(int i=0; i<counts.size(); i++) {
						if(count > counts.get(i)) {
							sorted.add(i, removedOperation);
							counts.add(i, count);
							inserted = true;
							break;
						}
					}
					if(!inserted) {
						sorted.add(counts.size(), removedOperation);
						counts.add(counts.size(), count);
					}
				}
			}
		}
		return sorted;
	}

	public List<InlineOperationRefactoring> check(UMLOperation removedOperation) throws RefactoringMinerTimedOutException {
		List<InlineOperationRefactoring> refactorings = new ArrayList<InlineOperationRefactoring>();
		if(!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() ||
			!mapper.getReplacementsInvolvingMethodInvocationForInline().isEmpty() || mapper.containsCompositeMappingWithoutReplacements()) {
			List<AbstractCall> removedOperationInvocations = callCountMap != null ? callCountMap.get(removedOperation) : matchingInvocations(removedOperation, operationInvocations, mapper.getContainer1());
			if(removedOperationInvocations.size() > 0 && !invocationMatchesWithAddedOperation(removedOperationInvocations.get(0), mapper.getContainer1(), mapper.getContainer2().getAllOperationInvocations())) {
				int otherAddedMethodsCalled = 0;
				int otherAddedMethodsCalledWithSameOrMoreCallSites = 0;
				for(UMLOperation removedOperation1 : this.removedOperations) {
					if(!removedOperation.equals(removedOperation1)) {
						List<AbstractCall> removedOperationInvocations1 = callCountMap != null ? callCountMap.get(removedOperation1) : matchingInvocations(removedOperation1, operationInvocations, mapper.getContainer1());
						if(removedOperationInvocations1 != null && removedOperationInvocations1.size() > 0) {
							otherAddedMethodsCalled++;
						}
						if(removedOperationInvocations1 != null && removedOperationInvocations1.size() >= removedOperationInvocations.size()) {
							otherAddedMethodsCalledWithSameOrMoreCallSites++;
						}
					}
				}
				if(otherAddedMethodsCalledWithSameOrMoreCallSites == 0 && (otherAddedMethodsCalled == 0 || mapper.getContainer2().stringRepresentation().size() > removedOperationInvocations.size() * removedOperation.stringRepresentation().size())) {
					for(AbstractCall removedOperationInvocation : removedOperationInvocations) {
						processRemovedOperation(removedOperation, refactorings, removedOperationInvocations, removedOperationInvocation);
					}
				}
				else {
					AbstractCall removedOperationInvocation = removedOperationInvocations.get(0);
					processRemovedOperation(removedOperation, refactorings, removedOperationInvocations, removedOperationInvocation);
				}
			}
		}
		return refactorings;
	}

	private void processRemovedOperation(UMLOperation removedOperation, List<InlineOperationRefactoring> refactorings,
			List<AbstractCall> removedOperationInvocations, AbstractCall removedOperationInvocation)
			throws RefactoringMinerTimedOutException {
		if(removedOperationInvocation.isSuperCall()) {
			return;
		}
		CallTreeNode root = new CallTreeNode(mapper.getContainer1(), removedOperation, removedOperationInvocation);
		CallTree callTree = null;
		if(callTreeMap.containsKey(root)) {
			callTree = callTreeMap.get(root);
		}
		else {
			callTree = new CallTree(root);
			generateCallTree(removedOperation, root, callTree);
			callTreeMap.put(root, callTree);
		}
		UMLOperationBodyMapper operationBodyMapper = createMapperForInlinedMethod(mapper, removedOperation, removedOperationInvocation, false);
		if(operationBodyMapper != null && !containsRefactoringWithIdenticalMappings(refactorings, operationBodyMapper)) {
			List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
			for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
				CallTreeNode node = nodesInBreadthFirstOrder.get(i);
				//if(matchingInvocations(node.getInvokedOperation(), operationInvocations, mapper.getContainer1()).size() == 0) {
					processNestedMapper(operationBodyMapper, operationBodyMapper, refactorings, node);
				//}
			}
			if(inlineMatchCondition(operationBodyMapper, mapper)) {
				InlineOperationRefactoring inlineOperationRefactoring =	new InlineOperationRefactoring(operationBodyMapper, mapper.getContainer1(), removedOperationInvocations);
				refactorings.add(inlineOperationRefactoring);
			}
			else {
				//add any mappings back to parent mapper as non-mapped statements
				for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					if(fragment2 instanceof CompositeStatementObject) {
						if(!mapper.getNonMappedInnerNodesT2().contains(fragment2)) {
							mapper.getNonMappedInnerNodesT2().add((CompositeStatementObject)fragment2);
						}
					}
					else {
						if(!mapper.getNonMappedLeavesT2().contains(fragment2)) {
							mapper.getNonMappedLeavesT2().add(fragment2);
						}
					}
				}
			}
		}
	}

	private void processNestedMapper(UMLOperationBodyMapper mapper,
			UMLOperationBodyMapper operationBodyMapper, List<InlineOperationRefactoring> refactorings, CallTreeNode node) throws RefactoringMinerTimedOutException {
		for(InlineOperationRefactoring inline : refactorings) {
			if(inline.getBodyMapper().getContainer1().equals(node.getInvokedOperation()) && inline.getBodyMapper().getContainer2().equals(mapper.getContainer2())) {
				return;
			}
		}
		UMLOperationBodyMapper nestedMapper = createMapperForInlinedMethod(mapper, node.getInvokedOperation(), node.getInvocation(), true);
		if(inlineMatchCondition(nestedMapper, mapper)) {
			List<AbstractCall> nestedMatchingInvocations = matchingInvocations(node.getInvokedOperation(), node.getOriginalOperation().getAllOperationInvocations(), node.getOriginalOperation());
			InlineOperationRefactoring nestedRefactoring = new InlineOperationRefactoring(nestedMapper, mapper.getContainer1(), nestedMatchingInvocations);
			refactorings.add(nestedRefactoring);
			operationBodyMapper.addChildMapper(nestedMapper);
		}
		else {
			//add any mappings back to parent mapper as non-mapped statements
			for(AbstractCodeMapping mapping : nestedMapper.getMappings()) {
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(fragment2 instanceof CompositeStatementObject) {
					if(!mapper.getNonMappedInnerNodesT2().contains(fragment2)) {
						mapper.getNonMappedInnerNodesT2().add((CompositeStatementObject)fragment2);
					}
				}
				else {
					if(!mapper.getNonMappedLeavesT2().contains(fragment2)) {
						mapper.getNonMappedLeavesT2().add(fragment2);
					}
				}
			}
		}
	}

	private boolean containsRefactoringWithIdenticalMappings(List<InlineOperationRefactoring> refactorings, UMLOperationBodyMapper mapper) {
		Set<AbstractCodeMapping> newMappings = mapper.getMappings();
		for(InlineOperationRefactoring ref : refactorings) {
			Set<AbstractCodeMapping> oldMappings = ref.getBodyMapper().getMappings();
			if(oldMappings.containsAll(newMappings)) {
				return true;
			}
		}
		return false;
	}

	private List<AbstractCall> matchingInvocations(UMLOperation removedOperation, List<AbstractCall> operationInvocations, VariableDeclarationContainer callerOperation) {
		List<AbstractCall> removedOperationInvocations = new ArrayList<AbstractCall>();
		for(AbstractCall invocation : operationInvocations) {
			if(invocation.matchesOperation(removedOperation, callerOperation, classDiff, modelDiff)) {
				removedOperationInvocations.add(invocation);
			}
		}
		return removedOperationInvocations;
	}

	private UMLOperationBodyMapper createMapperForInlinedMethod(UMLOperationBodyMapper mapper,
			UMLOperation removedOperation, AbstractCall removedOperationInvocation, boolean nested) throws RefactoringMinerTimedOutException {
		List<String> arguments = removedOperationInvocation.arguments();
		List<String> parameters = removedOperation.getParameterNameList();
		Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
		//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
		int size = Math.min(arguments.size(), parameters.size());
		for(int i=0; i<size; i++) {
			parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
		}
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap, new LinkedHashMap<String, String>(), classDiff, removedOperationInvocation, nested);
		return operationBodyMapper;
	}

	private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
		List<AbstractCall> invocations = operation.getAllOperationInvocations();
		for(UMLOperation removedOperation : removedOperations) {
			for(AbstractCall invocation : invocations) {
				if(invocation.matchesOperation(removedOperation, operation, classDiff, modelDiff)) {
					if(!callTree.containsInPathToRootOrSibling(parent, removedOperation)) {
						CallTreeNode node = new CallTreeNode(parent, operation, removedOperation, invocation);
						parent.addChild(node);
						generateCallTree(removedOperation, node, callTree);
					}
				}
			}
		}
	}

	private List<AbstractCall> getInvocationsInTargetOperationBeforeInline(UMLOperationBodyMapper mapper) {
		List<AbstractCall> operationInvocations = mapper.getContainer1().getAllOperationInvocations();
		for(AbstractCodeFragment statement : mapper.getNonMappedLeavesT1()) {
			ExtractOperationDetection.addStatementInvocations(operationInvocations, statement);
			if(classDiff != null) {
				for(UMLAnonymousClass anonymousClass : classDiff.getRemovedAnonymousClasses()) {
					if(statement.getLocationInfo().subsumes(anonymousClass.getLocationInfo())) {
						for(UMLOperation anonymousOperation : anonymousClass.getOperations()) {
							for(AbstractCall anonymousInvocation : anonymousOperation.getAllOperationInvocations()) {
								if(!ExtractOperationDetection.containsInvocation(operationInvocations, anonymousInvocation)) {
									operationInvocations.add(anonymousInvocation);
								}
							}
						}
					}
				}
			}
		}
		return operationInvocations;
	}

	private boolean inlineMatchCondition(UMLOperationBodyMapper operationBodyMapper, UMLOperationBodyMapper parentMapper) {
		if(operationBodyMapper.getContainer1().getBody() != null) {
			List<AbstractStatement> statements = operationBodyMapper.getContainer1().getBody().getCompositeStatement().getStatements();
			if(statements.size() == 1 && statements.get(0) instanceof StatementObject) {
				StatementObject statement = (StatementObject)statements.get(0);
				if(statement.getVariables().size() == 1) {
					String variable = statement.getVariables().get(0).getString();
					if(statement.getString().equals(JAVA.RETURN_SPACE + variable + JAVA.STATEMENT_TERMINATION)) {
						return false;
					}
				}
			}
		}
		int delegateStatements = 0;
		for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation != null && invocation.matchesOperation(operationBodyMapper.getContainer1(), parentMapper.getContainer1(), classDiff, modelDiff)) {
				delegateStatements++;
			}
		}
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1()-delegateStatements;
		if(nonMappedElementsT1 == 1) {
			for(AbstractCodeFragment fragment1 : operationBodyMapper.getNonMappedLeavesT1()) {
				List<VariableDeclaration> variableDeclarations = fragment1.getVariableDeclarations();
				if(variableDeclarations.size() > 0) {
					for(VariableDeclaration variableDeclaration : variableDeclarations) {
						for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
							boolean matchingReplacementFound = false;
							for(Replacement r : mapping.getReplacements()) {
								if(r.getBefore().equals(variableDeclaration.getVariableName())) {
									matchingReplacementFound = true;
								}
							}
							if(matchingReplacementFound) {
								nonMappedElementsT1--;
								break;
							}
						}
		 			}
				}
			}
		}
		List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
		List<AbstractCodeMapping> exactMatchListWithoutMatchesInNestedContainers = operationBodyMapper.getExactMatchesWithoutMatchesInNestedContainers();
		int exactMatches = exactMatchList.size();
		int exactMatchesWithoutMatchesInNestedContainers = exactMatchListWithoutMatchesInNestedContainers.size();
		return mappings > 0 && (mappings > nonMappedElementsT1 ||
				(exactMatchesWithoutMatchesInNestedContainers == 1 && !exactMatchListWithoutMatchesInNestedContainers.get(0).getFragment1().throwsNewException() && nonMappedElementsT1-exactMatchesWithoutMatchesInNestedContainers < 10) ||
				(exactMatches > 1 && nonMappedElementsT1-exactMatches < 20));
	}

	private boolean invocationMatchesWithAddedOperation(AbstractCall removedOperationInvocation, VariableDeclarationContainer callerOperation, List<AbstractCall> operationInvocationsInNewMethod) {
		if(operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			if(classDiff != null) {
				for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
					if(removedOperationInvocation.matchesOperation(addedOperation, callerOperation, classDiff, modelDiff)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
