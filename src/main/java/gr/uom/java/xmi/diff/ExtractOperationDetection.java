package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObjectMapping;
import gr.uom.java.xmi.decomposition.JsxElement;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.MethodReference;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.ReplacementUtil;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
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
	private final Constants LANG1;
	private final Constants LANG2;

	public ExtractOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> addedOperations, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff, boolean invocationsFromOtherMappers) {
		this.LANG1 = mapper.LANG1;
		this.LANG2 = mapper.LANG2;
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

	//an inline function replaced with the added function passed by its name, i.e., onStop={(e) => {...}} replaced with onStop={handleStop}
	private static boolean lambdaReplacedWithFunctionReference(UMLOperationBodyMapper mapper, UMLOperation addedOperation) {
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(!mapping.isExact() && mapping.getFragment1().getLambdas().size() > mapping.getFragment2().getLambdas().size()) {
				for(JsxElement element : mapping.getFragment2().getJsxElements()) {
					for(MethodReference reference : element.getFunctionReferences()) {
						if(reference.getName().equals(addedOperation.getName())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public List<ExtractOperationRefactoring> check(UMLOperation addedOperation) throws RefactoringMinerTimedOutException {
		List<ExtractOperationRefactoring> refactorings = new ArrayList<ExtractOperationRefactoring>();
		if(modelDiff != null && modelDiff.refactoringListContainsAnotherMoveRefactoringWithTheSameAddedOperation(addedOperation)) {
			return refactorings;
		}
		if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
			!mapper.getReplacementsInvolvingMethodInvocation().isEmpty() || mapper.containsCompositeMappingWithoutReplacements() || mapper.containsLambdaMapper() ||
			lambdaReplacedWithFunctionReference(mapper, addedOperation)) {
			List<AbstractCall> addedOperationInvocations = callCountMap != null ? callCountMap.get(addedOperation) : matchingInvocations(addedOperation, operationInvocations, mapper.getContainer2());
			if(addedOperationInvocations.size() > 0 && !invocationMatchesWithOperationHavingDefaultParameterValues(addedOperationInvocations.get(0), mapper.getContainer1().getAllOperationInvocations())) {
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
				//the calls in the tests of a test suite describe('...', () => {...}) are in separate tests, like calls in separate methods
				boolean moduleWithTestSuites = mapper.getContainer1() instanceof ModuleContainer module && !module.getDescribeMap().isEmpty();
				if((otherAddedMethodsCalledWithSameOrMoreCallSites == 0 || moduleWithTestSuites) && (otherAddedMethodsCalled == 0 || mapper.getContainer1().stringRepresentation().size() > addedOperationInvocations.size() * addedOperation.stringRepresentation().size())) {
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
					AbstractCall invocation = addedOperationInvocations.get(0);
					if(addedOperationInvocations.size() > 1) {
						double maxScore = 0;
						for(int i=0; i<addedOperationInvocations.size(); i++) {
							AbstractCodeMapping parentMapping = findParentMapping(mapper, addedOperationInvocations.get(i), addedOperation);
							if(parentMapping != null && parentMapping instanceof CompositeStatementObjectMapping) {
								CompositeStatementObjectMapping compositeMapping = (CompositeStatementObjectMapping)parentMapping;
								double score = compositeMapping.getCompositeChildMatchingScore();
								if(score > maxScore && score > 0.5) {
									invocation = addedOperationInvocations.get(i);
									maxScore = score;
								}
							}
						}
					}
					processAddedOperation(addedOperation, refactorings, addedOperationInvocations, invocation);
				}
			}
		}
		return refactorings;
	}

	private AbstractCodeMapping findParentMapping(UMLOperationBodyMapper parentMapper, AbstractCall operationInvocation, UMLOperation addedOperation) {
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
					boolean extractedStatement = parentMapper.getExtractedStatements().containsKey(addedOperation) && parentMapper.getExtractedStatements().get(addedOperation).contains(mapping.getFragment1());
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
		return null;
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
					if(argument.startsWith(LANG2.THIS_DOT) && !allVariables.contains(argument)) {
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
		StatementObject singleReturnStatement = addedOperation.singleReturnStatement();
		if(operationBodyMapper != null && (operationBodyMapper.getMappings().isEmpty() || containsRefactoringWithIdenticalMappings(refactorings, operationBodyMapper)) && singleReturnStatement != null) {
			String s = singleReturnStatement.getString();
			String expression = s.substring(LANG2.RETURN_SPACE.length(), s.length()-LANG2.STATEMENT_TERMINATION.length());
			for(AbstractCodeMapping mapping : mapper.getMappings()) {
				for(Replacement r : mapping.getReplacements()) {
					if(r.getAfter().contains(addedOperation.getName() + "(")) {
						if(expression.equals(r.getBefore())) {
							List<LeafExpression> expressions1 = mapping.getFragment1().findExpression(r.getBefore());
							List<LeafExpression> expressions2 = singleReturnStatement.findExpression(expression);
							if(expressions2.size() == 1) {
								for(LeafExpression expression1 : expressions1) {
									LeafMapping newMapping = new LeafMapping(expression1, expressions2.get(0), mapper.getContainer1(), addedOperation);
									operationBodyMapper.addMapping(newMapping);
								}
							}
						}
						else if(operationBodyMapper.getParameterToArgumentMap2().isPresent()) {
							Map<String, String> parameterToArgumentMap = operationBodyMapper.getParameterToArgumentMap2().get();
							String before = r.getBefore();
							for(String key : parameterToArgumentMap.keySet()) {
								String value = parameterToArgumentMap.get(key);
								if(!key.equals(value)) {
									before = ReplacementUtil.performReplacement(before, value, key);
								}
							}
							if(expression.equals(before)) {
								List<LeafExpression> expressions1 = mapping.getFragment1().findExpression(r.getBefore());
								List<LeafExpression> expressions2 = singleReturnStatement.findExpression(expression);
								if(expressions2.size() == 1) {
									for(LeafExpression expression1 : expressions1) {
										LeafMapping newMapping = new LeafMapping(expression1, expressions2.get(0), mapper.getContainer1(), addedOperation);
										operationBodyMapper.addMapping(newMapping);
									}
								}
							}
						}
					}
				}
			}
		}
		if(operationBodyMapper != null) {
			removeUnrelatedCallsCopiedToOtherAddedOperation(operationBodyMapper, addedOperation);
		}
		boolean skip = operationBodyMapper != null && operationBodyMapper.hasAnonymousClassDiffNestedUnderLambda() && mapper.hasAnonymousClassDiffNestedUnderLambda() &&
				operationBodyMapper.getAnonymousClassDiffs().iterator().next().getOriginalClass().equals(mapper.getAnonymousClassDiffs().iterator().next().getOriginalClass());
		if(operationBodyMapper != null && (!containsRefactoringWithIdenticalMappings(refactorings, operationBodyMapper) || parentMapperContainsOperationInvocation(mapper, operationBodyMapper, addedOperationInvocation)) && !skip) {
			List<AbstractCodeMapping> additionalExactMatches = new ArrayList<AbstractCodeMapping>();
			List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
			for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
				CallTreeNode node = nodesInBreadthFirstOrder.get(i);
				//if(matchingInvocations(node.getInvokedOperation(), operationInvocations, mapper.getContainer2()).size() == 0) {
					processNestedMapper(operationBodyMapper, operationBodyMapper, refactorings, additionalExactMatches, node);
				//}
			}
			UMLOperation delegateMethod = findDelegateMethod(mapper.getContainer1(), addedOperation, addedOperationInvocation);
			if(extractMatchCondition(operationBodyMapper, additionalExactMatches, false)) {
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

	//a statement matched only by replacing its whole call with an unrelated call of the same name, while another added operation holds an identical copy of it,
	//i.e., await connection.execute("WITH team AS (INSERT ...") -> await admin.execute(sql.SQL("DROP DATABASE {}")...) in _drop_database,
	//while _build_template holds await connection.execute("WITH team AS (INSERT ...") unchanged
	private void removeUnrelatedCallsCopiedToOtherAddedOperation(UMLOperationBodyMapper operationBodyMapper, UMLOperation addedOperation) {
		for(AbstractCodeMapping mapping : new ArrayList<>(operationBodyMapper.getMappings())) {
			if(!(mapping instanceof LeafMapping) || mapping.getReplacements().size() != 1) {
				continue;
			}
			//the statements differ only in the replaced calls, i.e., await call1 -> await call2
			if(!(mapping.getReplacements().iterator().next() instanceof MethodInvocationReplacement replacement) ||
					!replacement.getType().equals(ReplacementType.METHOD_INVOCATION)) {
				continue;
			}
			AbstractCall call1 = replacement.getInvokedOperationBefore();
			AbstractCall call2 = replacement.getInvokedOperationAfter();
			if(call1.arguments().isEmpty() || !call1.argumentIntersection(call2).isEmpty()) {
				continue;
			}
			if(identicalStatementInOtherAddedOperation(mapping.getFragment1(), addedOperation)) {
				operationBodyMapper.removeMapping(mapping);
				operationBodyMapper.getNonMappedLeavesT1().add(mapping.getFragment1());
				operationBodyMapper.getNonMappedLeavesT2().add(mapping.getFragment2());
			}
		}
	}

	private boolean identicalStatementInOtherAddedOperation(AbstractCodeFragment fragment1, UMLOperation addedOperation) {
		for(UMLOperation operation : addedOperations) {
			if(operation != addedOperation && operation.getBody() != null) {
				for(AbstractCodeFragment leaf : operation.getBody().getCompositeStatement().getLeaves()) {
					if(leaf.getString().equals(fragment1.getString())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void processNestedMapper(UMLOperationBodyMapper mapper, UMLOperationBodyMapper operationBodyMapper,
			List<ExtractOperationRefactoring> refactorings, List<AbstractCodeMapping> additionalExactMatches,
			CallTreeNode node) throws RefactoringMinerTimedOutException {
		UMLOperationBodyMapper nestedMapper = createMapperForExtractedMethod(mapper, node.getOriginalOperation(), node.getInvokedOperation(), node.getInvocation(), true);
		if(nestedMapper != null && !containsRefactoringWithIdenticalMappings(refactorings, nestedMapper)) {
			additionalExactMatches.addAll(nestedMapper.getExactMatches());
			if(extractMatchCondition(nestedMapper, new ArrayList<AbstractCodeMapping>(), true) && (extractMatchCondition(operationBodyMapper, additionalExactMatches, true) || node.getOriginalOperation().delegatesTo(node.getInvokedOperation(), classDiff, modelDiff) != null)) {
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

	//the call of the added overload resolved to an operation with default parameter values, i.e., fun start(port: Int = 0) -> start()
	private boolean invocationMatchesWithOperationHavingDefaultParameterValues(AbstractCall addedOperationInvocation, List<AbstractCall> operationInvocationsInOldMethod) {
		if(classDiff != null && operationInvocationsInOldMethod.contains(addedOperationInvocation)) {
			for(UMLOperation operation : classDiff.getOriginalClass().getOperations()) {
				if(operation.getName().equals(addedOperationInvocation.getName()) && operation.hasDefaultParameterValuesAfter(addedOperationInvocation.arguments().size())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean parentMapperContainsOperationInvocation(UMLOperationBodyMapper parentMapper, UMLOperationBodyMapper childMapper, AbstractCall addedOperationInvocation) {
		AbstractCodeMapping callerMapping = null;
		for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
			if(mapping.getFragment2().getLocationInfo().subsumes(addedOperationInvocation.getLocationInfo())) {
				callerMapping = mapping;
				break;
			}
		}
		if(callerMapping != null && (childMapper.getContainer2().isGetter() || childMapper.getContainer2().isSetter())) {
			for(AbstractCodeMapping mapping : childMapper.getMappings()) {
				if(mapping.getFragment1().equals(callerMapping.getFragment1())) {
					return true;
				}
			}
		}
		return false;
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
		if(mapper.getClassDiff() != null) {
			List<String> variableReferences = mapper.getContainer2().getAllVariables();
			for(UMLAttribute attribute : mapper.getClassDiff().getNextClass().getAttributes()) {
				//the module-level attributes are code of the module, i.e., app = create_app() in a Python module
				if((variableReferences.contains(attribute.getName()) || mapper.getContainer2() instanceof ModuleContainer) &&
						attribute.getVariableDeclaration() != null && attribute.getVariableDeclaration().getInitializer() != null) {
					List<AbstractCall> invocations = attribute.getVariableDeclaration().getInitializer().getAllOperationInvocations();
					for(AbstractCall invocation : invocations) {
						if(!operationInvocations.contains(invocation))
							operationInvocations.add(invocation);
					}
				}
			}
		}
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.isExact() && mapping.getReplacementsInvolvingMethodInvocation().isEmpty() && mapping.getFragment1().getLambdas().size() == mapping.getFragment2().getLambdas().size()) {
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
		addFunctionReferencesInJsxElements(operationInvocations, mapper);
		addPytestFixtureParameters(operationInvocations, mapper);
		return operationInvocations;
	}

	//the pytest fixtures requested by the new parameters of a test or fixture, which pytest calls before it,
	//i.e., database_url(empty_database_url) -> database_url(_migrated_template), where the fixture _migrated_template calls the function extracted from database_url
	private static void addPytestFixtureParameters(List<AbstractCall> operationInvocations, UMLOperationBodyMapper mapper) {
		if(mapper.getClassDiff() == null || !(mapper.getContainer2() instanceof UMLOperation operation2) || !operation2.getLANG().equals(Constants.PYTHON)) {
			return;
		}
		List<String> parameterNames1 = mapper.getContainer1().getParameterNameList();
		for(VariableDeclaration parameter : operation2.getParameterDeclarationList()) {
			String name = parameter.getVariableName();
			if(parameterNames1.contains(name)) {
				continue;
			}
			for(UMLOperation fixture : mapper.getClassDiff().getNextClass().getOperations()) {
				if(fixture.getName().equals(name) && fixture.isPytestFixture()) {
					operationInvocations.add(new MethodReference(parameter));
					break;
				}
			}
		}
	}

	//the functions passed by their name to JSX elements outside the exactly mapped statements, i.e., handleStop in <Draggable onStop={handleStop} />,
	//which call the function extracted from the replaced inline function <Draggable onStop={(e) => {...}} />
	private static void addFunctionReferencesInJsxElements(List<AbstractCall> operationInvocations, UMLOperationBodyMapper mapper) {
		if(mapper.getContainer2().getBody() == null) {
			return;
		}
		Set<AbstractCodeFragment> exactlyMappedStatements = new HashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.isExact()) {
				exactlyMappedStatements.add(mapping.getFragment2());
			}
		}
		CompositeStatementObject body = mapper.getContainer2().getBody().getCompositeStatement();
		List<AbstractCodeFragment> statements = new ArrayList<AbstractCodeFragment>(body.getLeaves());
		for(CompositeStatementObject innerNode : body.getInnerNodes()) {
			statements.addAll(innerNode.getExpressions());
		}
		for(AbstractCodeFragment statement : statements) {
			if(!exactlyMappedStatements.contains(statement)) {
				for(JsxElement element : statement.getJsxElements()) {
					for(MethodReference reference : element.getFunctionReferences()) {
						if(!containsInvocation(operationInvocations, reference)) {
							operationInvocations.add(reference);
						}
					}
				}
			}
		}
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
		List<UMLParameter> parameters = addedOperation.getParametersWithoutReturnType();
		List<String> arguments = addedOperationInvocation.argumentsOfParameters(parameters);
		Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
		for(int i=0; i<parameters.size(); i++) {
			String argumentName = arguments.get(i);
			if(argumentName == null) {
				continue;
			}
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
					new LinkedHashMap<String, String>(), parameterToArgumentMap, classDiff, addedOperationInvocation, nested, Optional.empty(), Optional.empty());
		}
		return null;
	}

	//a returned expression extracted into a variable that is returned after more statements use it, i.e., return sign_in_problem_page(message, status) ->
	//page = sign_in_problem_page(message, status_code, sign_in_path); _clear_cookie(page, ...); return page in the extracted method,
	//so the statements between the declaration and the return using the variable complete the returned value
	private static int statementsUsingReturnedVariable(UMLOperationBodyMapper operationBodyMapper, AbstractCodeFragment declaration, AbstractCodeFragment returnStatement, String variableName) {
		int count = 0;
		for(AbstractCodeFragment leaf2 : operationBodyMapper.getNonMappedLeavesT2()) {
			if(leaf2 != returnStatement && leaf2.countableStatement() && leaf2.getVariableDeclarations().isEmpty() &&
					leaf2.getLocationInfo().getStartOffset() > declaration.getLocationInfo().getEndOffset() &&
					leaf2.getLocationInfo().getEndOffset() < returnStatement.getLocationInfo().getStartOffset()) {
				for(LeafExpression variable : leaf2.getVariables()) {
					if(variable.getString().equals(variableName)) {
						count++;
						break;
					}
				}
			}
		}
		return count;
	}

	private boolean extractMatchCondition(UMLOperationBodyMapper operationBodyMapper, List<AbstractCodeMapping> additionalExactMatches, boolean forNestedMapper) {
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
			if(systemCallReplacedWithDelegation(operationBodyMapper)) {
				return false;
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
		boolean synchronizedBlockExactMatch = false;
		boolean constantReturnExactMatch = false;
		if(exactMatchList.size() == 1) {
			AbstractCodeMapping mapping = exactMatchList.get(0);
			//a returned constant, like return true, is no evidence of extracted code, i.e., return {"ok": True} in a new Python method returning the same status as the original
			if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT) &&
					mapping.getFragment1().getVariables().isEmpty() && mapping.getFragment1().getMethodInvocations().isEmpty() && mapping.getFragment1().getCreations().isEmpty()) {
				constantReturnExactMatch = true;
			}
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
			if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT) &&
					mapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
				synchronizedBlockExactMatch = true;
			}
		}
		boolean mappingOwnedByLambda = false;
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.getFragment1().ownedByLambda()) {
				mappingOwnedByLambda = true;
			}
			List<VariableDeclaration> variableDeclarations = mapping.getFragment2().getVariableDeclarations();
			if(variableDeclarations.size() > 0) {
				for(VariableDeclaration variableDeclaration : variableDeclarations) {
					for(AbstractCodeFragment leaf2 : operationBodyMapper.getNonMappedLeavesT2()) {
						if(leaf2.countableStatement() && leaf2.getString().equals(LANG2.RETURN_SPACE + variableDeclaration.getVariableName() + LANG2.STATEMENT_TERMINATION)) {
							nonMappedElementsT2--;
							if(mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
								nonMappedElementsT2 -= statementsUsingReturnedVariable(operationBodyMapper, mapping.getFragment2(), leaf2, variableDeclaration.getVariableName());
							}
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
		else {
			for(AbstractCodeFragment fragment2 : operationBodyMapper.getNonMappedLeavesT2()) {
				if(fragment2.getStringLiterals().size() == 1 && fragment2.getString().equals(LANG2.RETURN_SPACE + fragment2.getStringLiterals().get(0).getString() + LANG2.STATEMENT_TERMINATION)) {
					nonMappedElementsT2--;
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
			if(mappings == 0 && mapping.getFragment1().getString().equals(LANG1.RETURN_THIS) && !mapping.getFragment2().getString().equals(LANG2.RETURN_THIS) &&
					mapping.getFragment2().getString().startsWith(LANG2.RETURN_SPACE) && mapping.getFragment2().getString().endsWith(LANG2.THIS_STATEMENT_TERMINATION)) {
				mappings++;
			}
			if(beforeAfterContains == mapping.getReplacements().size()) {
				exactMatches++;
			}
		}
		int identicalMatches = 0;
		for(AbstractCodeMapping mapping : additionalExactMatches) {
			if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				identicalMatches++;
			}
		}
		return (mappings > 0 || (forNestedMapper && identicalMatches > 1)) && (mappings > nonMappedElementsT2 || (mappings > 1 && mappings >= nonMappedElementsT2) ||
				(exactMatches >= mappings && nonMappedElementsT1 == 0) ||
				(exactMatches == 1 && !throwsNewExceptionExactMatch && !synchronizedBlockExactMatch && !constantReturnExactMatch && nonMappedElementsT2-exactMatches <= 10) ||
				(!exceptionHandlingExactMatch && exactMatches > 1 && additionalExactMatches.size() <= exactMatches && nonMappedElementsT2-exactMatches < 20) ||
				(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2()) ||
				(mappings >= 1 && mappingOwnedByLambda && mappings >= operationBodyMapper.nonMappedLeafElementsT2())) ||
				argumentExtractedWithDefaultReturnAdded(operationBodyMapper);
	}

	//the added method only delegates to an overload, and its only mapped statement is a system call replaced with the delegation, i.e., System.arraycopy(args, 0, newArgs, 0, n) ->
	//arraycopy(source, sourcePos, allocator.get(), destPos, length) in arraycopy(source, sourcePos, destPos, length, allocator) added to ArrayUtils,
	//as in UMLOperationBodyMapper.containsOnlySystemCalls, while a replaced call of the project, i.e., parser.parse(X.class) -> parse(List.of(clazz)), is extracted
	static boolean systemCallReplacedWithDelegation(UMLOperationBodyMapper operationBodyMapper) {
		if(operationBodyMapper.getMappings().size() != 1)
			return false;
		AbstractCodeMapping mapping = operationBodyMapper.getMappings().iterator().next();
		AbstractCall delegateCall = operationBodyMapper.getContainer2().isDelegate();
		if(delegateCall != null && mapping.getFragment1().getString().startsWith("System.") && mapping.getFragment2().getMethodInvocations().contains(delegateCall)) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME) || replacement.getType().equals(ReplacementType.METHOD_INVOCATION_EXPRESSION) ||
						replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean argumentExtractedWithDefaultReturnAdded(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeMapping> totalMappings = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
		List<CompositeStatementObject> nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>(operationBodyMapper.getNonMappedInnerNodesT2());
		ListIterator<CompositeStatementObject> iterator = nonMappedInnerNodesT2.listIterator();
		while(iterator.hasNext()) {
			if(iterator.next().getString().equals(LANG2.OPEN_BLOCK)) {
				iterator.remove();
			}
		}
		List<AbstractCodeFragment> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		return totalMappings.size() == 1 && totalMappings.get(0).containsReplacement(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) &&
				nonMappedInnerNodesT2.size() == 1 && nonMappedInnerNodesT2.get(0).toString().startsWith("if") &&
				nonMappedLeavesT2.size() == 1 && nonMappedLeavesT2.get(0).toString().startsWith(LANG2.RETURN_SPACE);
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
			//a parameter destructured from an object pattern carries the type of the whole pattern, not its own
			if(!key.getVariableDeclaration().isDestructuredFromObjectPattern() && !key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType()) && !key.getType().equalClassType(value.getType()) &&
					!OperationInvocation.compatibleTypes(value, key.getType(), classDiff, modelDiff)) {
				return false;
			}
		}
		return true;
	}
}
