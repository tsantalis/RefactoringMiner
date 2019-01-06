package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class ExtractOperationDetection {
	private List<UMLOperation> addedOperations;

	public ExtractOperationDetection(List<UMLOperation> addedOperations) {
		this.addedOperations = addedOperations;
	}

	public ExtractOperationRefactoring check(UMLOperationBodyMapper mapper, UMLOperation addedOperation) {
		if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
			!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
			Set<OperationInvocation> operationInvocations = mapper.getOperation2().getAllOperationInvocations();
			OperationInvocation addedOperationInvocation = matchingInvocation(addedOperation, operationInvocations, mapper.getOperation2().variableTypeMap());
			if(addedOperationInvocation != null) {
				CallTreeNode root = new CallTreeNode(mapper.getOperation1(), addedOperation, addedOperationInvocation);
				CallTree callTree = new CallTree(root);
				generateCallTree(addedOperation, root, callTree);
				UMLOperationBodyMapper operationBodyMapper = createMapperForExtractedMethod(mapper, mapper.getOperation1(), addedOperation, addedOperationInvocation);
				if(operationBodyMapper != null) {
					List<AbstractCodeMapping> additionalExactMatches = new ArrayList<AbstractCodeMapping>();
					List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
					for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
						CallTreeNode node = nodesInBreadthFirstOrder.get(i);
						if(matchingInvocation(node.getInvokedOperation(), operationInvocations, mapper.getOperation2().variableTypeMap()) == null) {
							UMLOperationBodyMapper nestedMapper = createMapperForExtractedMethod(mapper, node.getOriginalOperation(), node.getInvokedOperation(), node.getInvocation());
							if(nestedMapper != null) {
								additionalExactMatches.addAll(nestedMapper.getExactMatches());
							}
						}
					}
					UMLOperation delegateMethod = findDelegateMethod(mapper.getOperation1(), addedOperation, addedOperationInvocation);
					if(extractMatchCondition(operationBodyMapper, additionalExactMatches)) {
						if(delegateMethod == null) {
							return new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(), addedOperationInvocation);
						}
						else {
							return new ExtractOperationRefactoring(operationBodyMapper, addedOperation,
									mapper.getOperation1(), mapper.getOperation2(), addedOperationInvocation);
						}
					}
				}
			}
		}
		return null;
	}

	private OperationInvocation matchingInvocation(UMLOperation addedOperation,
			Set<OperationInvocation> operationInvocations, Map<String, UMLType> variableTypeMap) {
		OperationInvocation addedOperationInvocation = null;
		for(OperationInvocation invocation : operationInvocations) {
			if(invocation.matchesOperation(addedOperation, variableTypeMap)) {
				addedOperationInvocation = invocation;
				break;
			}
		}
		return addedOperationInvocation;
	}

	private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
		Set<OperationInvocation> invocations = operation.getAllOperationInvocations();
		for(UMLOperation addedOperation : addedOperations) {
			for(OperationInvocation invocation : invocations) {
				if(invocation.matchesOperation(addedOperation, operation.variableTypeMap())) {
					if(!callTree.contains(addedOperation)) {
						CallTreeNode node = new CallTreeNode(operation, addedOperation, invocation);
						parent.addChild(node);
						generateCallTree(addedOperation, node, callTree);
					}
				}
			}
		}
	}

	private UMLOperationBodyMapper createMapperForExtractedMethod(UMLOperationBodyMapper mapper,
			UMLOperation originalOperation, UMLOperation addedOperation, OperationInvocation addedOperationInvocation) {
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
					new LinkedHashMap<String, String>(), parameterToArgumentMap);
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
		return mappings > 0 && (mappings > nonMappedElementsT2 ||
				(exactMatches >= mappings && nonMappedElementsT1 == 0) ||
				(exactMatches == 1 && !throwsNewExceptionExactMatch && nonMappedElementsT2-exactMatches < 10) ||
				(!exceptionHandlingExactMatch && exactMatches > 1 && additionalExactMatches.size() < exactMatches && nonMappedElementsT2-exactMatches < 20) ||
				(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2())) ||
				argumentExtractedWithDefaultReturnAdded(operationBodyMapper);
	}

	private boolean argumentExtractedWithDefaultReturnAdded(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeMapping> totalMappings = operationBodyMapper.getMappings();
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
				if(delegateMethodInvocation.matchesOperation(operation, addedOperation.variableTypeMap())) {
					return operation;
				}
			}
		}
		return null;
	}

	private boolean parameterTypesMatch(Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
		for(UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
			UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
			if(!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType())) {
				return false;
			}
		}
		return true;
	}

	private class CallTree {
		private CallTreeNode root;
		
		public CallTree(CallTreeNode root) {
			this.root = root;
		}
		
		public List<CallTreeNode> getNodesInBreadthFirstOrder() {
			List<CallTreeNode> nodes = new ArrayList<CallTreeNode>();
			List<CallTreeNode> queue = new LinkedList<CallTreeNode>();
			nodes.add(root);
			queue.add(root);
			while(!queue.isEmpty()) {
				CallTreeNode node = queue.remove(0);
				nodes.addAll(node.children);
				queue.addAll(node.children);
			}
			return nodes;
		}
		
		public boolean contains(UMLOperation invokedOperation) {
			for(CallTreeNode node : getNodesInBreadthFirstOrder()) {
				if(node.getInvokedOperation().equals(invokedOperation)) {
					return true;
				}
			}
			return false;
		}
	}

	private class CallTreeNode {
		private UMLOperation originalOperation;
		private UMLOperation invokedOperation;
		private OperationInvocation invocation;
		private List<CallTreeNode> children = new ArrayList<CallTreeNode>();
		
		public CallTreeNode(UMLOperation originalOperation, UMLOperation invokedOperation,
				OperationInvocation invocation) {
			this.originalOperation = originalOperation;
			this.invokedOperation = invokedOperation;
			this.invocation = invocation;
		}

		public UMLOperation getOriginalOperation() {
			return originalOperation;
		}

		public UMLOperation getInvokedOperation() {
			return invokedOperation;
		}

		public OperationInvocation getInvocation() {
			return invocation;
		}

		public void addChild(CallTreeNode node) {
			children.add(node);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((invocation == null) ? 0 : invocation.hashCode());
			result = prime * result + ((invokedOperation == null) ? 0 : invokedOperation.hashCode());
			result = prime * result + ((originalOperation == null) ? 0 : originalOperation.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CallTreeNode other = (CallTreeNode) obj;
			if (invocation == null) {
				if (other.invocation != null)
					return false;
			} else if (!invocation.equals(other.invocation))
				return false;
			if (invokedOperation == null) {
				if (other.invokedOperation != null)
					return false;
			} else if (!invokedOperation.equals(other.invokedOperation))
				return false;
			if (originalOperation == null) {
				if (other.originalOperation != null)
					return false;
			} else if (!originalOperation.equals(other.originalOperation))
				return false;
			return true;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(invokedOperation);
			sb.append(" called from ");
			sb.append(originalOperation);
			return sb.toString();
		}
	}
}
