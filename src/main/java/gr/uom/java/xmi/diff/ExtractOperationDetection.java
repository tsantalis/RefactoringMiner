package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObjectMapping;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class ExtractOperationDetection {
	private UMLOperationBodyMapper mapper;
	private List<UMLOperation> addedOperations;
	private UMLAbstractClassDiff classDiff;
	private UMLModelDiff modelDiff;
	private List<AbstractCall> operationInvocations;
	private Map<CallTreeNode, CallTree> callTreeMap = new LinkedHashMap<CallTreeNode, CallTree>();
	private Map<UMLOperation, List<AbstractCall>> callCountMap = null;
	private List<UMLOperation> potentiallyMovedOperations = new ArrayList<UMLOperation>();

	public ExtractOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> addedOperations, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff, boolean invocationsFromOtherMappers) {
		this.mapper = mapper;
		this.addedOperations = addedOperations;
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		if(invocationsFromOtherMappers) {
			addInvocationsFromOtherMappers();
		}
		else {
			this.operationInvocations = getInvocationsInSourceOperationAfterExtractionExcludingInvocationsInExactlyMappedStatements(mapper);
		}
	}

	public ExtractOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> addedOperations, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
		this(mapper, addedOperations, classDiff, modelDiff, false);
	}

	public ExtractOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> potentiallyMovedOperations, List<UMLOperation> addedOperations, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
		this(mapper, addedOperations, classDiff, modelDiff);
		this.potentiallyMovedOperations = potentiallyMovedOperations;
	}

	private void addInvocationsFromOtherMappers() {
		this.operationInvocations = new ArrayList<>();
		for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
			if(!mapper.equals(this.mapper)) {
				for(AbstractCodeFragment statement : mapper.getNonMappedLeavesT2()) {
					addStatementInvocations(operationInvocations, statement);
				}
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					if(!mapping.isExact()) {
						addStatementInvocations(operationInvocations, mapping.getFragment2());
					}
				}
			}
		}
	}

	public List<UMLOperation> getAddedOperationsSortedByCalls() {
		this.callCountMap = new LinkedHashMap<>();
		List<UMLOperation> sorted = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		for(UMLOperation addedOperation : addedOperations) {
			List<AbstractCall> matchingInvocations = matchingInvocations(addedOperation, operationInvocations, mapper.getContainer2());
			if(!matchingInvocations.isEmpty()) {
				callCountMap.put(addedOperation, matchingInvocations);
				int count = matchingInvocations.size();
				if(sorted.isEmpty()) {
					sorted.add(addedOperation);
					counts.add(count);
				}
				else {
					boolean inserted = false;
					for(int i=0; i<counts.size(); i++) {
						if(count > counts.get(i)) {
							sorted.add(i, addedOperation);
							counts.add(i, count);
							inserted = true;
							break;
						}
					}
					if(!inserted) {
						sorted.add(counts.size(), addedOperation);
						counts.add(counts.size(), count);
					}
				}
			}
		}
		return sorted;
	}

	public List<ExtractOperationRefactoring> check(UMLOperation addedOperation) throws RefactoringMinerTimedOutException {
		List<ExtractOperationRefactoring> refactorings = new ArrayList<ExtractOperationRefactoring>();
		if(modelDiff != null && modelDiff.refactoringListContainsAnotherMoveRefactoringWithTheSameAddedOperation(addedOperation)) {
			return refactorings;
		}
		if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
			!mapper.getReplacementsInvolvingMethodInvocation().isEmpty() || mapper.containsCompositeMappingWithoutReplacements()) {
			List<AbstractCall> addedOperationInvocations = callCountMap != null ? callCountMap.get(addedOperation) : matchingInvocations(addedOperation, operationInvocations, mapper.getContainer2());
			if(addedOperationInvocations.size() > 0) {
				int otherAddedMethodsCalled = 0;
				int otherAddedMethodsCalledWithSameOrMoreCallSites = 0;
				for(UMLOperation addedOperation2 : this.addedOperations) {
					if(!addedOperation.equals(addedOperation2)) {
						List<AbstractCall> addedOperationInvocations2 = callCountMap != null ? callCountMap.get(addedOperation2) : matchingInvocations(addedOperation2, operationInvocations, mapper.getContainer2());
						if(addedOperationInvocations2 != null && addedOperationInvocations2.size() > 0) {
							otherAddedMethodsCalled++;
						}
						if(addedOperationInvocations2 != null && addedOperationInvocations2.size() >= addedOperationInvocations.size()) {
							otherAddedMethodsCalledWithSameOrMoreCallSites++;
						}
					}
				}
				//check if the source method contains more statements than (addedOperationInvocations.size() * addedOperation statements)
				if(otherAddedMethodsCalledWithSameOrMoreCallSites == 0 && (otherAddedMethodsCalled == 0 || mapper.getContainer1().stringRepresentation().size() > addedOperationInvocations.size() * addedOperation.stringRepresentation().size())) {
					List<AbstractCall> sortedInvocations = sortInvocationsBasedOnArgumentOccurrences(addedOperationInvocations);
					for(AbstractCall addedOperationInvocation : sortedInvocations) {
						processAddedOperation(addedOperation, refactorings, sortedInvocations, addedOperationInvocation);
						if(sortedInvocations.size() == 1 && addedOperationInvocation.arguments().size() == 1) {
							String argument = addedOperationInvocation.arguments().get(0);
							Set<VariableDeclaration> declarations = mapper.getContainer2().variableDeclarationMap().get(argument);
							if(declarations != null && declarations.size() == 1) {
								VariableDeclaration declaration = declarations.iterator().next();
								if(declaration.getInitializer() != null && declaration.getInitializer().getTernaryOperatorExpressions().size() == 1) {
									processAddedOperation(addedOperation, refactorings, sortedInvocations, addedOperationInvocation);
								}
							}
						}
					}
				}
				else {
					processAddedOperation(addedOperation, refactorings, addedOperationInvocations, addedOperationInvocations.get(0));
				}
			}
		}
		return refactorings;
	}

	private List<AbstractCall> sortInvocationsBasedOnArgumentOccurrences(List<AbstractCall> invocations) {
		if(invocations.size() > 1) {
			List<AbstractCall> sorted = new ArrayList<AbstractCall>();
			List<String> allVariables = new ArrayList<String>();
			for(CompositeStatementObject composite : mapper.getNonMappedInnerNodesT1()) {
				for(LeafExpression expression : composite.getVariables()) {
					allVariables.add(expression.getString());
				}
			}
			for(AbstractCodeFragment leaf : mapper.getNonMappedLeavesT1()) {
				for(LeafExpression expression : leaf.getVariables()) {
					allVariables.add(expression.getString());
				}
			}
			int max = 0;
			for(AbstractCall invocation : invocations) {
				List<String> arguments = invocation.arguments();
				int occurrences = 0;
				for(String argument : arguments) {
					if(argument.startsWith(JAVA.THIS_DOT) && !allVariables.contains(argument)) {
						String substringAfterThis = argument.substring(5);
						occurrences += Collections.frequency(allVariables, substringAfterThis);
					}
					else {
						occurrences += Collections.frequency(allVariables, argument);
					}
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

	private void processAddedOperation(UMLOperation addedOperation,
			List<ExtractOperationRefactoring> refactorings,
			List<AbstractCall> addedOperationInvocations, AbstractCall addedOperationInvocation)
			throws RefactoringMinerTimedOutException {
		if(addedOperationInvocation.isSuperCall()) {
			return;
		}
		CallTreeNode root = new CallTreeNode(mapper.getContainer1(), addedOperation, addedOperationInvocation);
		CallTree callTree = null;
		if(callTreeMap.containsKey(root)) {
			callTree = callTreeMap.get(root);
		}
		else {
			callTree = new CallTree(root);
			generateCallTree(addedOperation, root, callTree);
			callTreeMap.put(root, callTree);
		}
		UMLOperationBodyMapper operationBodyMapper = createMapperForExtractedMethod(mapper, mapper.getContainer1(), addedOperation, addedOperationInvocation, false);
		if(operationBodyMapper != null && !containsRefactoringWithIdenticalMappings(refactorings, operationBodyMapper)) {
			List<AbstractCodeMapping> additionalExactMatches = new ArrayList<AbstractCodeMapping>();
			List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
			for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
				CallTreeNode node = nodesInBreadthFirstOrder.get(i);
				//if(matchingInvocations(node.getInvokedOperation(), operationInvocations, mapper.getContainer2()).size() == 0) {
					processNestedMapper(operationBodyMapper, operationBodyMapper, refactorings, additionalExactMatches, node);
				//}
			}
			UMLOperation delegateMethod = findDelegateMethod(mapper.getContainer1(), addedOperation, addedOperationInvocation);
			if(extractMatchCondition(operationBodyMapper, additionalExactMatches)) {
				ExtractOperationRefactoring extractOperationRefactoring = null;
				if(delegateMethod == null) {
					extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, mapper.getContainer2(), addedOperationInvocations);
				}
				else {
					extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, addedOperation,
							mapper.getContainer1(), mapper.getContainer2(), addedOperationInvocations);
				}
				refactorings.add(extractOperationRefactoring);
			}
			else {
				//add any mappings back to parent mapper as non-mapped statements
				for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					if(fragment1 instanceof CompositeStatementObject) {
						if(!mapper.getNonMappedInnerNodesT1().contains(fragment1)) {
							mapper.getNonMappedInnerNodesT1().add((CompositeStatementObject)fragment1);
						}
					}
					else {
						if(!mapper.getNonMappedLeavesT1().contains(fragment1)) {
							mapper.getNonMappedLeavesT1().add(fragment1);
						}
					}
				}
			}
		}
	}

	private void processNestedMapper(UMLOperationBodyMapper mapper, UMLOperationBodyMapper operationBodyMapper,
			List<ExtractOperationRefactoring> refactorings, List<AbstractCodeMapping> additionalExactMatches,
			CallTreeNode node) throws RefactoringMinerTimedOutException {
		UMLOperationBodyMapper nestedMapper = createMapperForExtractedMethod(mapper, node.getOriginalOperation(), node.getInvokedOperation(), node.getInvocation(), true);
		if(nestedMapper != null && !containsRefactoringWithIdenticalMappings(refactorings, nestedMapper)) {
			additionalExactMatches.addAll(nestedMapper.getExactMatches());
			if(extractMatchCondition(nestedMapper, new ArrayList<AbstractCodeMapping>()) && (extractMatchCondition(operationBodyMapper, additionalExactMatches) || node.getOriginalOperation().delegatesTo(node.getInvokedOperation(), classDiff, modelDiff) != null)) {
				List<AbstractCall> nestedMatchingInvocations = matchingInvocations(node.getInvokedOperation(), node.getOriginalOperation().getAllOperationInvocations(), node.getOriginalOperation());
				ExtractOperationRefactoring nestedRefactoring = new ExtractOperationRefactoring(nestedMapper, mapper.getContainer2(), nestedMatchingInvocations);
				refactorings.add(nestedRefactoring);
				operationBodyMapper.addChildMapper(nestedMapper);
			}
			else {
				//add any mappings back to parent mapper as non-mapped statements
				for(AbstractCodeMapping mapping : nestedMapper.getMappings()) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					if(fragment1 instanceof CompositeStatementObject) {
						if(!mapper.getNonMappedInnerNodesT1().contains(fragment1)) {
							mapper.getNonMappedInnerNodesT1().add((CompositeStatementObject)fragment1);
						}
					}
					else {
						if(!mapper.getNonMappedLeavesT1().contains(fragment1)) {
							mapper.getNonMappedLeavesT1().add(fragment1);
						}
					}
				}
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

	private static List<AbstractCall> getInvocationsInSourceOperationAfterExtractionExcludingInvocationsInExactlyMappedStatements(UMLOperationBodyMapper mapper) {
		List<AbstractCall> operationInvocations = mapper.getContainer2().getAllOperationInvocations();
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.isExact() && mapping.getReplacementsInvolvingMethodInvocation().isEmpty()) {
				for(AbstractCall invocation : mapping.getFragment2().getMethodInvocations()) {
					for(ListIterator<AbstractCall> iterator = operationInvocations.listIterator(); iterator.hasNext();) {
						AbstractCall matchingInvocation = iterator.next();
						if(invocation == matchingInvocation || invocation.actualString().equals(matchingInvocation.actualString())) {
							iterator.remove();
							break;
						}
					}
				}
			}
		}
		for(AbstractCodeFragment statement : mapper.getNonMappedLeavesT2()) {
			addStatementInvocations(operationInvocations, statement);
		}
		return operationInvocations;
	}

	public static List<AbstractCall> getInvocationsInSourceOperationAfterExtraction(UMLOperationBodyMapper mapper) {
		List<AbstractCall> operationInvocations = getInvocationsInSourceOperationAfterExtractionExcludingInvocationsInExactlyMappedStatements(mapper);
		if(operationInvocations.isEmpty()) {
			return operationInvocations;
		}
		List<AbstractCall> invocationsInSourceOperationBeforeExtraction = mapper.getContainer1().getAllOperationInvocations();
		for(AbstractCall invocation : invocationsInSourceOperationBeforeExtraction) {
			for(ListIterator<AbstractCall> iterator = operationInvocations.listIterator(); iterator.hasNext();) {
				AbstractCall matchingInvocation = iterator.next();
				if(invocation.getName().equals(matchingInvocation.getName())) {
					boolean matchingInvocationFound = false;
					for(String argument : invocation.arguments()) {
						if(argument.equals(matchingInvocation.getExpression())) {
							iterator.remove();
							matchingInvocationFound = true;
							break;
						}
					}
					if(matchingInvocationFound) {
						break;
					}
				}
			}
		}
		return operationInvocations;
	}

	public static void addStatementInvocations(List<AbstractCall> operationInvocations, AbstractCodeFragment statement) {
		for(AbstractCall statementInvocation : statement.getMethodInvocations()) {
			if(!containsInvocation(operationInvocations, statementInvocation)) {
				operationInvocations.add(statementInvocation);
			}
		}
		List<LambdaExpressionObject> lambdas = statement.getLambdas();
		for(LambdaExpressionObject lambda : lambdas) {
			for(AbstractCall statementInvocation : lambda.getAllOperationInvocations()) {
				if(!containsInvocation(operationInvocations, statementInvocation)) {
					operationInvocations.add(statementInvocation);
				}
			}
		}
	}

	public static boolean containsInvocation(List<AbstractCall> operationInvocations, AbstractCall invocation) {
		for(AbstractCall operationInvocation : operationInvocations) {
			if(operationInvocation.getLocationInfo().equals(invocation.getLocationInfo())) {
				return true;
			}
		}
		return false;
	}

	private List<AbstractCall> matchingInvocations(UMLOperation operation,
			List<AbstractCall> operationInvocations, VariableDeclarationContainer callerOperation) {
		List<AbstractCall> addedOperationInvocations = new ArrayList<AbstractCall>();
		for(AbstractCall invocation : operationInvocations) {
			if(invocation.matchesOperation(operation, callerOperation, classDiff, modelDiff)) {
				addedOperationInvocations.add(invocation);
			}
		}
		return addedOperationInvocations;
	}

	private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
		List<AbstractCall> invocations = operation.getAllOperationInvocations();
		for(UMLOperation addedOperation : addedOperations) {
			for(AbstractCall invocation : invocations) {
				if(invocation.matchesOperation(addedOperation, operation, classDiff, modelDiff)) {
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
			VariableDeclarationContainer originalOperation, UMLOperation addedOperation, AbstractCall addedOperationInvocation, boolean nested) throws RefactoringMinerTimedOutException {
		for(UMLOperation potentiallyMovedOperation : potentiallyMovedOperations) {
			if(potentiallyMovedOperation.equalSignature(addedOperation)) {
				return null;
			}
		}
		List<UMLParameter> originalMethodParameters = originalOperation.getParametersWithoutReturnType();
		Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters = new LinkedHashMap<UMLParameter, UMLParameter>();
		List<String> arguments = addedOperationInvocation.arguments();
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
					new LinkedHashMap<String, String>(), parameterToArgumentMap, classDiff, addedOperationInvocation, nested, Optional.empty());
		}
		return null;
	}

	private boolean extractMatchCondition(UMLOperationBodyMapper operationBodyMapper, List<AbstractCodeMapping> additionalExactMatches) {
		if(operationBodyMapper.getMappings().size() == 1) {
			AbstractCodeMapping mapping = operationBodyMapper.getMappings().iterator().next();
			if(mapping.getFragment1() instanceof AbstractExpression) {
				for(AbstractCodeMapping parentMapping : operationBodyMapper.getParentMapper().getMappings()) {
					if(parentMapping instanceof CompositeStatementObjectMapping) {
						CompositeStatementObject parentComp1 = (CompositeStatementObject) parentMapping.getFragment1();
						CompositeStatementObject parentComp2 = (CompositeStatementObject) parentMapping.getFragment2();
						if(parentComp1.getExpressions().contains(mapping.getFragment1()) &&
								!parentComp2.getMethodInvocations().contains(operationBodyMapper.getOperationInvocation())) {
							return false;
						}
					}
				}
			}
			if(operationBodyMapper.isNested() && operationBodyMapper.getParentMapper() != null) {
				if(operationBodyMapper.getParentMapper().getMappings().size() == 1 &&
						operationBodyMapper.getParentMapper().getContainer1().isDelegate() != null &&
						operationBodyMapper.getParentMapper().getContainer2().isDelegate() != null) {
					return false;
				}
			}
		}
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
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			List<VariableDeclaration> variableDeclarations = mapping.getFragment2().getVariableDeclarations();
			if(variableDeclarations.size() > 0) {
				for(VariableDeclaration variableDeclaration : variableDeclarations) {
					for(AbstractCodeFragment leaf2 : operationBodyMapper.getNonMappedLeavesT2()) {
						if(leaf2.countableStatement() && leaf2.getString().equals(JAVA.RETURN_SPACE + variableDeclaration.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
							nonMappedElementsT2--;
							break;
						}
					}
	 			}
			}
			for(Refactoring r : mapping.getRefactorings()) {
				if(r instanceof ExtractVariableRefactoring) {
					ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
					boolean subsumeFound = false;
					for(LeafMapping leafMapping : extract.getSubExpressionMappings()) {
						for(AbstractCodeFragment leaf2 : operationBodyMapper.getNonMappedLeavesT2()) {
							if(leaf2.getLocationInfo().subsumes(leafMapping.getFragment2().getLocationInfo())) {
								subsumeFound = true;
							}
						}
					}
					if(subsumeFound) {
						nonMappedElementsT2--;
						mappings++;
						break;
					}
				}
			}
		}
		if(nonMappedElementsT2 == 1) {
			for(AbstractCodeFragment fragment2 : operationBodyMapper.getNonMappedLeavesT2()) {
				List<VariableDeclaration> variableDeclarations = fragment2.getVariableDeclarations();
				if(variableDeclarations.size() > 0) {
					for(VariableDeclaration variableDeclaration : variableDeclarations) {
						for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
							boolean matchingReplacementFound = false;
							for(Replacement r : mapping.getReplacements()) {
								if(r.getAfter().equals(variableDeclaration.getVariableName())) {
									matchingReplacementFound = true;
								}
							}
							if(matchingReplacementFound) {
								nonMappedElementsT2--;
								break;
							}
						}
		 			}
				}
			}
		}
		exactMatchList.addAll(additionalExactMatches);
		int exactMatches = exactMatchList.size();
		if(exactMatches == 0 && operationBodyMapper.getMappings().size() >= 1 && operationBodyMapper.getMappings().size() <= 2) {
			int beforeAfterContains = 0;
			AbstractCodeMapping mapping = operationBodyMapper.getMappings().iterator().next();
			for(Replacement r : mapping.getReplacements()) {
				if(r.getAfter().contains(r.getBefore()) || r.getBefore().contains(r.getAfter())) {
					beforeAfterContains++;
				}
			}
			if(beforeAfterContains == mapping.getReplacements().size()) {
				exactMatches++;
			}
		}
		return mappings > 0 && (mappings > nonMappedElementsT2 || (mappings > 1 && mappings >= nonMappedElementsT2) ||
				(exactMatches >= mappings && nonMappedElementsT1 == 0) ||
				(exactMatches == 1 && !throwsNewExceptionExactMatch && nonMappedElementsT2-exactMatches <= 10) ||
				(!exceptionHandlingExactMatch && exactMatches > 1 && additionalExactMatches.size() <= exactMatches && nonMappedElementsT2-exactMatches < 20) ||
				(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2())) ||
				argumentExtractedWithDefaultReturnAdded(operationBodyMapper);
	}

	private boolean argumentExtractedWithDefaultReturnAdded(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeMapping> totalMappings = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
		List<CompositeStatementObject> nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>(operationBodyMapper.getNonMappedInnerNodesT2());
		ListIterator<CompositeStatementObject> iterator = nonMappedInnerNodesT2.listIterator();
		while(iterator.hasNext()) {
			if(iterator.next().getString().equals(JAVA.OPEN_BLOCK)) {
				iterator.remove();
			}
		}
		List<AbstractCodeFragment> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		return totalMappings.size() == 1 && totalMappings.get(0).containsReplacement(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) &&
				nonMappedInnerNodesT2.size() == 1 && nonMappedInnerNodesT2.get(0).toString().startsWith("if") &&
				nonMappedLeavesT2.size() == 1 && nonMappedLeavesT2.get(0).toString().startsWith(JAVA.RETURN_SPACE);
	}

	private UMLOperation findDelegateMethod(VariableDeclarationContainer originalOperation, UMLOperation addedOperation, AbstractCall addedOperationInvocation) {
		AbstractCall delegateMethodInvocation = addedOperation.isDelegate();
		if(originalOperation.isDelegate() == null && delegateMethodInvocation != null && !originalOperation.getAllOperationInvocations().contains(addedOperationInvocation)) {
			for(UMLOperation operation : addedOperations) {
				if(delegateMethodInvocation.matchesOperation(operation, addedOperation, classDiff, modelDiff)) {
					return operation;
				}
			}
		}
		return null;
	}

	private boolean parameterTypesMatch(Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
		for(UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
			UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
			if(!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType()) && !key.getType().equalClassType(value.getType()) &&
					!OperationInvocation.compatibleTypes(value, key.getType(), classDiff, modelDiff)) {
				return false;
			}
		}
		return true;
	}
}
