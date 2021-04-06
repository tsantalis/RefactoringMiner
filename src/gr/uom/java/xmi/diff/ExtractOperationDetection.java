package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class ExtractOperationDetection {
	private UMLOperationBodyMapper mapper;
	private List<UMLOperation> addedOperations;
	private UMLClassBaseDiff classDiff;
	private UMLModelDiff modelDiff;
	private List<OperationInvocation> operationInvocations;
	private Map<CallTreeNode, CallTree> callTreeMap = new LinkedHashMap<CallTreeNode, CallTree>();

	public ExtractOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> addedOperations, UMLClassBaseDiff classDiff, UMLModelDiff modelDiff) {
		this.mapper = mapper;
		this.addedOperations = addedOperations;
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		this.operationInvocations = getInvocationsInSourceOperationAfterExtraction(mapper);
	}

	public List<ExtractOperationRefactoring> check(UMLOperation addedOperation) throws RefactoringMinerTimedOutException {
		List<ExtractOperationRefactoring> refactorings = new ArrayList<ExtractOperationRefactoring>();
		if(modelDiff != null && modelDiff.refactoringListContainsAnotherMoveRefactoringWithTheSameAddedOperation(addedOperation)) {
			return refactorings;
		}
		if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
			!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
			List<OperationInvocation> addedOperationInvocations = matchingInvocations(addedOperation, operationInvocations, mapper.getOperation2());
			if(addedOperationInvocations.size() > 0) {
				int otherAddedMethodsCalled = 0;
				for(UMLOperation addedOperation2 : this.addedOperations) {
					if(!addedOperation.equals(addedOperation2)) {
						List<OperationInvocation> addedOperationInvocations2 = matchingInvocations(addedOperation2, operationInvocations, mapper.getOperation2());
						if(addedOperationInvocations2.size() > 0) {
							otherAddedMethodsCalled++;
						}
					}
				}
				if(otherAddedMethodsCalled == 0) {
					List<OperationInvocation> sortedInvocations = sortInvocationsBasedOnArgumentOccurrences(addedOperationInvocations);
					for(OperationInvocation addedOperationInvocation : sortedInvocations) {
						processAddedOperation(mapper, addedOperation, refactorings, addedOperationInvocations, addedOperationInvocation);
					}
				}
				else {
					processAddedOperation(mapper, addedOperation, refactorings, addedOperationInvocations, addedOperationInvocations.get(0));
				}
			}
		}
		return refactorings;
	}

	private List<OperationInvocation> sortInvocationsBasedOnArgumentOccurrences(List<OperationInvocation> invocations) {
		if(invocations.size() > 1) {
			List<OperationInvocation> sorted = new ArrayList<OperationInvocation>();
			List<String> allVariables = new ArrayList<String>();
			for(CompositeStatementObject composite : mapper.getNonMappedInnerNodesT1()) {
				allVariables.addAll(composite.getVariables());
			}
			for(StatementObject leaf : mapper.getNonMappedLeavesT1()) {
				allVariables.addAll(leaf.getVariables());
			}
			int max = 0;
			for(OperationInvocation invocation : invocations) {
				List<String> arguments = invocation.getArguments();
				int occurrences = 0;
				for(String argument : arguments) {
					occurrences += Collections.frequency(allVariables, argument);
				}
				if(occurrences > max) {
					sorted.add(0, invocation);
					max = occurrences;
				}
				else {
					sorted.add(invocation);
				}
			}
			return sorted;
		}
		else {
			return invocations;
		}
	}

	private void processAddedOperation(UMLOperationBodyMapper mapper, UMLOperation addedOperation,
			List<ExtractOperationRefactoring> refactorings,
			List<OperationInvocation> addedOperationInvocations, OperationInvocation addedOperationInvocation)
			throws RefactoringMinerTimedOutException {
		CallTreeNode root = new CallTreeNode(mapper.getOperation1(), addedOperation, addedOperationInvocation);
		CallTree callTree = null;
		if(callTreeMap.containsKey(root)) {
			callTree = callTreeMap.get(root);
		}
		else {
			callTree = new CallTree(root);
			generateCallTree(addedOperation, root, callTree);
			callTreeMap.put(root, callTree);
		}
		UMLOperationBodyMapper operationBodyMapper = createMapperForExtractedMethod(mapper, mapper.getOperation1(), addedOperation, addedOperationInvocation);
		if(operationBodyMapper != null && !containsRefactoringWithIdenticalMappings(refactorings, operationBodyMapper)) {
			List<AbstractCodeMapping> additionalExactMatches = new ArrayList<AbstractCodeMapping>();
			List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
			for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
				CallTreeNode node = nodesInBreadthFirstOrder.get(i);
				if(matchingInvocations(node.getInvokedOperation(), operationInvocations, mapper.getOperation2()).size() == 0) {
					UMLOperationBodyMapper nestedMapper = createMapperForExtractedMethod(mapper, node.getOriginalOperation(), node.getInvokedOperation(), node.getInvocation());
					if(nestedMapper != null && !containsRefactoringWithIdenticalMappings(refactorings, nestedMapper)) {
						additionalExactMatches.addAll(nestedMapper.getExactMatches());
						if(extractMatchCondition(nestedMapper, new ArrayList<AbstractCodeMapping>()) && extractMatchCondition(operationBodyMapper, additionalExactMatches)) {
							List<OperationInvocation> nestedMatchingInvocations = matchingInvocations(node.getInvokedOperation(), node.getOriginalOperation().getAllOperationInvocations(), node.getOriginalOperation());
							ExtractOperationRefactoring nestedRefactoring = new ExtractOperationRefactoring(nestedMapper, mapper.getOperation2(), nestedMatchingInvocations);
							refactorings.add(nestedRefactoring);
							operationBodyMapper.addChildMapper(nestedMapper);
						}
						//add back to mapper non-exact matches
						for(AbstractCodeMapping mapping : nestedMapper.getMappings()) {
							if(!mapping.isExact() || mapping.getFragment1().getString().equals("{")) {
								AbstractCodeFragment fragment1 = mapping.getFragment1();
								if(fragment1 instanceof StatementObject) {
									if(!mapper.getNonMappedLeavesT1().contains(fragment1)) {
										mapper.getNonMappedLeavesT1().add((StatementObject)fragment1);
									}
								}
								else if(fragment1 instanceof CompositeStatementObject) {
									if(!mapper.getNonMappedInnerNodesT1().contains(fragment1)) {
										mapper.getNonMappedInnerNodesT1().add((CompositeStatementObject)fragment1);
									}
								}
							}
						}
					}
				}
			}
			UMLOperation delegateMethod = findDelegateMethod(mapper.getOperation1(), addedOperation, addedOperationInvocation);
			if(extractMatchCondition(operationBodyMapper, additionalExactMatches)) {
				ExtractOperationRefactoring extractOperationRefactoring = null;
				if(delegateMethod == null) {
					extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(), addedOperationInvocations);
				}
				else {
					extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, addedOperation,
							mapper.getOperation1(), mapper.getOperation2(), addedOperationInvocations);
				}
				refactorings.add(extractOperationRefactoring);
			}
		}
	}

	private boolean containsRefactoringWithIdenticalMappings(List<ExtractOperationRefactoring> refactorings, UMLOperationBodyMapper mapper) {
		Set<AbstractCodeMapping> newMappings = mapper.getMappings();
		for(ExtractOperationRefactoring ref : refactorings) {
			Set<AbstractCodeMapping> oldMappings = ref.getBodyMapper().getMappings();
			if(oldMappings.containsAll(newMappings)) {
				return true;
			}
		}
		return false;
	}

	public static List<OperationInvocation> getInvocationsInSourceOperationAfterExtraction(UMLOperationBodyMapper mapper) {
		List<OperationInvocation> operationInvocations = mapper.getOperation2().getAllOperationInvocations();
		for(StatementObject statement : mapper.getNonMappedLeavesT2()) {
			addStatementInvocations(operationInvocations, statement);
		}
		return operationInvocations;
	}

	public static void addStatementInvocations(List<OperationInvocation> operationInvocations, StatementObject statement) {
		Map<String, List<OperationInvocation>> statementMethodInvocationMap = statement.getMethodInvocationMap();
		for(String key : statementMethodInvocationMap.keySet()) {
			for(OperationInvocation statementInvocation : statementMethodInvocationMap.get(key)) {
				if(!containsInvocation(operationInvocations, statementInvocation)) {
					operationInvocations.add(statementInvocation);
				}
			}
		}
		List<LambdaExpressionObject> lambdas = statement.getLambdas();
		for(LambdaExpressionObject lambda : lambdas) {
			if(lambda.getBody() != null) {
				for(OperationInvocation statementInvocation : lambda.getBody().getAllOperationInvocations()) {
					if(!containsInvocation(operationInvocations, statementInvocation)) {
						operationInvocations.add(statementInvocation);
					}
				}
			}
			if(lambda.getExpression() != null) {
				Map<String, List<OperationInvocation>> methodInvocationMap = lambda.getExpression().getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					for(OperationInvocation statementInvocation : methodInvocationMap.get(key)) {
						if(!containsInvocation(operationInvocations, statementInvocation)) {
							operationInvocations.add(statementInvocation);
						}
					}
				}
			}
		}
	}

	public static boolean containsInvocation(List<OperationInvocation> operationInvocations, OperationInvocation invocation) {
		for(OperationInvocation operationInvocation : operationInvocations) {
			if(operationInvocation.getLocationInfo().equals(invocation.getLocationInfo())) {
				return true;
			}
		}
		return false;
	}

	private List<OperationInvocation> matchingInvocations(UMLOperation operation,
			List<OperationInvocation> operationInvocations, UMLOperation callerOperation) {
		List<OperationInvocation> addedOperationInvocations = new ArrayList<OperationInvocation>();
		for(OperationInvocation invocation : operationInvocations) {
			if(invocation.matchesOperation(operation, callerOperation, modelDiff)) {
				addedOperationInvocations.add(invocation);
			}
		}
		return addedOperationInvocations;
	}

	private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
		List<OperationInvocation> invocations = operation.getAllOperationInvocations();
		for(UMLOperation addedOperation : addedOperations) {
			for(OperationInvocation invocation : invocations) {
				if(invocation.matchesOperation(addedOperation, operation, modelDiff)) {
					if(!callTree.containsInPathToRootOrSibling(parent, addedOperation)) {
						CallTreeNode node = new CallTreeNode(parent, operation, addedOperation, invocation);
						parent.addChild(node);
						generateCallTree(addedOperation, node, callTree);
					}
				}
			}
		}
	}

	private UMLOperationBodyMapper createMapperForExtractedMethod(UMLOperationBodyMapper mapper,
			UMLOperation originalOperation, UMLOperation addedOperation, OperationInvocation addedOperationInvocation) throws RefactoringMinerTimedOutException {
		List<UMLParameter> originalMethodParameters = originalOperation.getParametersWithoutReturnType();
		Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters = new LinkedHashMap<UMLParameter, UMLParameter>();
		List<String> arguments = addedOperationInvocation.getArguments();
		List<UMLParameter> parameters = addedOperation.getParametersWithoutReturnType();
		Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
		//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
		int size = Math.min(arguments.size(), parameters.size());
		for(int i=0; i<size; i++) {
			String argumentName = arguments.get(i);
			String parameterName = parameters.get(i).getName();
			parameterToArgumentMap.put(parameterName, argumentName);
			for(UMLParameter originalMethodParameter : originalMethodParameters) {
				if(originalMethodParameter.getName().equals(argumentName)) {
					originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.put(originalMethodParameter, parameters.get(i));
				}
			}
		}
		if(parameterTypesMatch(originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters)) {
			UMLOperation delegateMethod = findDelegateMethod(originalOperation, addedOperation, addedOperationInvocation);
			return new UMLOperationBodyMapper(mapper,
					delegateMethod != null ? delegateMethod : addedOperation,
					new LinkedHashMap<String, String>(), parameterToArgumentMap, classDiff);
		}
		return null;
	}

	private boolean extractMatchCondition(UMLOperationBodyMapper operationBodyMapper, List<AbstractCodeMapping> additionalExactMatches) {
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		List<AbstractCodeMapping> exactMatchList = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getExactMatches());
		boolean exceptionHandlingExactMatch = false;
		boolean throwsNewExceptionExactMatch = false;
		if(exactMatchList.size() == 1) {
			AbstractCodeMapping mapping = exactMatchList.get(0);
			if(mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
				StatementObject statement1 = (StatementObject)mapping.getFragment1();
				StatementObject statement2 = (StatementObject)mapping.getFragment2();
				if(statement1.getParent().getString().startsWith("catch(") &&
						statement2.getParent().getString().startsWith("catch(")) {
					exceptionHandlingExactMatch = true;
				}
			}
			if(mapping.getFragment1().throwsNewException() && mapping.getFragment2().throwsNewException()) {
				throwsNewExceptionExactMatch = true;
			}
		}
		exactMatchList.addAll(additionalExactMatches);
		int exactMatches = exactMatchList.size();
		return mappings > 0 && (mappings > nonMappedElementsT2 || (mappings > 1 && mappings >= nonMappedElementsT2) ||
				(exactMatches >= mappings && nonMappedElementsT1 == 0) ||
				(exactMatches == 1 && !throwsNewExceptionExactMatch && nonMappedElementsT2-exactMatches <= 10) ||
				(!exceptionHandlingExactMatch && exactMatches > 1 && additionalExactMatches.size() < exactMatches && nonMappedElementsT2-exactMatches < 20) ||
				(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2())) ||
				argumentExtractedWithDefaultReturnAdded(operationBodyMapper);
	}

	private boolean argumentExtractedWithDefaultReturnAdded(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeMapping> totalMappings = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
		List<CompositeStatementObject> nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>(operationBodyMapper.getNonMappedInnerNodesT2());
		ListIterator<CompositeStatementObject> iterator = nonMappedInnerNodesT2.listIterator();
		while(iterator.hasNext()) {
			if(iterator.next().toString().equals("{")) {
				iterator.remove();
			}
		}
		List<StatementObject> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		return totalMappings.size() == 1 && totalMappings.get(0).containsReplacement(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) &&
				nonMappedInnerNodesT2.size() == 1 && nonMappedInnerNodesT2.get(0).toString().startsWith("if") &&
				nonMappedLeavesT2.size() == 1 && nonMappedLeavesT2.get(0).toString().startsWith("return ");
	}

	private UMLOperation findDelegateMethod(UMLOperation originalOperation, UMLOperation addedOperation, OperationInvocation addedOperationInvocation) {
		OperationInvocation delegateMethodInvocation = addedOperation.isDelegate();
		if(originalOperation.isDelegate() == null && delegateMethodInvocation != null && !originalOperation.getAllOperationInvocations().contains(addedOperationInvocation)) {
			for(UMLOperation operation : addedOperations) {
				if(delegateMethodInvocation.matchesOperation(operation, addedOperation, modelDiff)) {
					return operation;
				}
			}
		}
		return null;
	}

	private boolean parameterTypesMatch(Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
		for(UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
			UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
			if(!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType()) &&
					!modelDiff.isSubclassOf(key.getType().getClassType(), value.getType().getClassType())) {
				return false;
			}
		}
		return true;
	}
}
