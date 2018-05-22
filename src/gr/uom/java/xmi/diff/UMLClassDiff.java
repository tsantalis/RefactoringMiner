package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.refactoringminer.api.Refactoring;

public class UMLClassDiff implements Comparable<UMLClassDiff> {
	private UMLClass originalClass;
	private UMLClass nextClass;
	private String className;
	private List<UMLOperation> addedOperations;
	private List<UMLOperation> removedOperations;
	private List<UMLAttribute> addedAttributes;
	private List<UMLAttribute> removedAttributes;
	private List<UMLAttributeDiff> attributeDiffList;
	private List<UMLOperationDiff> operationDiffList;
	private List<UMLOperationBodyMapper> operationBodyMapperList;
	private List<Refactoring> refactorings;
	private boolean visibilityChanged;
	private String oldVisibility;
	private String newVisibility;
	private boolean abstractionChanged;
	private boolean oldAbstraction;
	private boolean newAbstraction;
	private boolean superclassChanged;
	private UMLType oldSuperclass;
	private UMLType newSuperclass;
	private List<UMLAnonymousClass> addedAnonymousClasses;
	private List<UMLAnonymousClass> removedAnonymousClasses;
	private List<UMLType> addedImplementedInterfaces;
	private List<UMLType> removedImplementedInterfaces;
	private static final int MAX_OPERATION_POSITION_DIFFERENCE = 5;
	public static final double MAX_OPERATION_NAME_DISTANCE = 0.34;
	
	public UMLClassDiff(UMLClass originalClass, UMLClass nextClass) {
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.className = originalClass.getName();
		this.addedOperations = new ArrayList<UMLOperation>();
		this.removedOperations = new ArrayList<UMLOperation>();
		this.addedAttributes = new ArrayList<UMLAttribute>();
		this.removedAttributes = new ArrayList<UMLAttribute>();
		this.attributeDiffList = new ArrayList<UMLAttributeDiff>();
		this.operationDiffList = new ArrayList<UMLOperationDiff>();
		this.operationBodyMapperList = new ArrayList<UMLOperationBodyMapper>();
		this.refactorings = new ArrayList<Refactoring>();
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.superclassChanged = false;
		this.addedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.removedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.addedImplementedInterfaces = new ArrayList<UMLType>();
		this.removedImplementedInterfaces = new ArrayList<UMLType>();
	}

	public String getClassName() {
		return className;
	}

	public void reportAddedAnonymousClass(UMLAnonymousClass umlClass) {
		this.addedAnonymousClasses.add(umlClass);
	}

	public void reportRemovedAnonymousClass(UMLAnonymousClass umlClass) {
		this.removedAnonymousClasses.add(umlClass);
	}

	public void reportAddedOperation(UMLOperation umlOperation) {
		this.addedOperations.add(umlOperation);
	}

	public void reportRemovedOperation(UMLOperation umlOperation) {
		this.removedOperations.add(umlOperation);
	}

	public void reportAddedAttribute(UMLAttribute umlAttribute) {
		this.addedAttributes.add(umlAttribute);
	}

	public void reportRemovedAttribute(UMLAttribute umlAttribute) {
		this.removedAttributes.add(umlAttribute);
	}

	public void reportAddedImplementedInterface(UMLType implementedInterface) {
		this.addedImplementedInterfaces.add(implementedInterface);
	}

	public void reportRemovedImplementedInterface(UMLType implementedInterface) {
		this.removedImplementedInterfaces.add(implementedInterface);
	}

	public void addOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper) {
		this.operationBodyMapperList.add(operationBodyMapper);
	}

	public void setVisibilityChanged(boolean visibilityChanged) {
		this.visibilityChanged = visibilityChanged;
	}

	public void setOldVisibility(String oldVisibility) {
		this.oldVisibility = oldVisibility;
	}

	public void setNewVisibility(String newVisibility) {
		this.newVisibility = newVisibility;
	}

	public void setAbstractionChanged(boolean abstractionChanged) {
		this.abstractionChanged = abstractionChanged;
	}

	public void setOldAbstraction(boolean oldAbstraction) {
		this.oldAbstraction = oldAbstraction;
	}

	public void setNewAbstraction(boolean newAbstraction) {
		this.newAbstraction = newAbstraction;
	}

	public void setSuperclassChanged(boolean superclassChanged) {
		this.superclassChanged = superclassChanged;
	}

	public void setOldSuperclass(UMLType oldSuperclass) {
		this.oldSuperclass = oldSuperclass;
	}

	public void setNewSuperclass(UMLType newSuperclass) {
		this.newSuperclass = newSuperclass;
	}

	public UMLType getSuperclass() {
		if(!superclassChanged && oldSuperclass != null && newSuperclass != null)
			return oldSuperclass;
		return null;
	}

	public UMLType getOldSuperclass() {
		return oldSuperclass;
	}

	public UMLType getNewSuperclass() {
		return newSuperclass;
	}

	public boolean containsOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			if(originalOperation.equalSignature(operation))
				return true;
		}
		return false;
	}

	public UMLOperation containsRemovedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.equalSignature(operation))
				return removedOperation;
		}
		return null;
	}

	public UMLAttribute containsRemovedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute removedAttribute : removedAttributes) {
			if(removedAttribute.equalsIgnoringChangedVisibility(attribute))
				return removedAttribute;
		}
		return null;
	}

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() &&
			addedAttributes.isEmpty() && removedAttributes.isEmpty() &&
			operationDiffList.isEmpty() && attributeDiffList.isEmpty() &&
			operationBodyMapperList.isEmpty() &&
			!visibilityChanged && !abstractionChanged;
	}

	public List<UMLOperation> getAddedOperations() {
		return addedOperations;
	}

	public List<UMLOperation> getRemovedOperations() {
		return removedOperations;
	}

	public List<UMLAttribute> getAddedAttributes() {
		return addedAttributes;
	}

	public List<UMLAttribute> getRemovedAttributes() {
		return removedAttributes;
	}

	public List<UMLAnonymousClass> getAddedAnonymousClasses() {
		return addedAnonymousClasses;
	}

	public List<UMLAnonymousClass> getRemovedAnonymousClasses() {
		return removedAnonymousClasses;
	}

	public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
		return operationBodyMapperList;
	}

	public List<UMLType> getAddedImplementedInterfaces() {
		return addedImplementedInterfaces;
	}

	public List<UMLType> getRemovedImplementedInterfaces() {
		return removedImplementedInterfaces;
	}

	public List<Refactoring> getRefactorings() {
		return refactorings;
	}

	public void checkForAttributeChanges() {
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if(removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute);
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					attributeDiffList.add(attributeDiff);
					break;
				}
			}
		}
	}
	
	private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation, UMLOperation addedOperation) {
		int index1 = originalClass.getOperations().indexOf(removedOperation);
		int index2 = nextClass.getOperations().indexOf(addedOperation);
		return Math.abs(index1-index2);
	}

	public void checkForOperationSignatureChanges() {
		Set<MethodInvocationReplacement> consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
		int absoluteDifference = Math.abs(removedOperations.size() - addedOperations.size());
		int maxDifferenceInPosition = absoluteDifference > MAX_OPERATION_POSITION_DIFFERENCE ? absoluteDifference : MAX_OPERATION_POSITION_DIFFERENCE;
		if(removedOperations.size() <= addedOperations.size()) {
			for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
				UMLOperation removedOperation = removedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
					List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
					for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
						updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
					}
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet, consistentMethodInvocationRenames);
					if(bestMapper != null) {
						removedOperation = bestMapper.getOperation1();
						UMLOperation addedOperation = bestMapper.getOperation2();
						addedOperations.remove(addedOperation);
						removedOperationIterator.remove();
	
						UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
						operationDiffList.add(operationDiff);
						if(!removedOperation.getName().equals(addedOperation.getName())) {
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
							refactorings.add(rename);
						}
						this.addOperationBodyMapper(bestMapper);
					}
				}
			}
		}
		else {
			for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
				UMLOperation addedOperation = addedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
					UMLOperation removedOperation = removedOperationIterator.next();
					updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
					List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
					for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
						updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
					}
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet, consistentMethodInvocationRenames);
					if(bestMapper != null) {
						UMLOperation removedOperation = bestMapper.getOperation1();
						addedOperation = bestMapper.getOperation2();
						removedOperations.remove(removedOperation);
						addedOperationIterator.remove();
	
						UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
						operationDiffList.add(operationDiff);
						if(!removedOperation.getName().equals(addedOperation.getName())) {
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
							refactorings.add(rename);
						}
						this.addOperationBodyMapper(bestMapper);
					}
				}
			}
		}
	}

	private Set<MethodInvocationReplacement> findConsistentMethodInvocationRenames() {
		Set<MethodInvocationReplacement> allConsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		Set<MethodInvocationReplacement> allInconsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		for(UMLOperationBodyMapper bodyMapper : operationBodyMapperList) {
			Set<MethodInvocationReplacement> methodInvocationRenames = bodyMapper.getMethodInvocationRenameReplacements();
			for(MethodInvocationReplacement newRename : methodInvocationRenames) {
				Set<MethodInvocationReplacement> inconsistentMethodInvocationRenames = inconsistentMethodInvocationRenames(allConsistentMethodInvocationRenames, newRename);
				if(inconsistentMethodInvocationRenames.isEmpty()) {
					allConsistentMethodInvocationRenames.add(newRename);
				}
				else {
					allInconsistentMethodInvocationRenames.addAll(inconsistentMethodInvocationRenames);
					allInconsistentMethodInvocationRenames.add(newRename);
				}
			}
		}
		allConsistentMethodInvocationRenames.removeAll(allInconsistentMethodInvocationRenames);
		return allConsistentMethodInvocationRenames;
	}
	
	private Set<MethodInvocationReplacement> inconsistentMethodInvocationRenames(Set<MethodInvocationReplacement> currentRenames, MethodInvocationReplacement newRename) {
		Set<MethodInvocationReplacement> inconsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		for(MethodInvocationReplacement rename : currentRenames) {
			if(rename.getBefore().equals(newRename.getBefore()) && !rename.getAfter().equals(newRename.getAfter())) {
				inconsistentMethodInvocationRenames.add(rename);
			}
			else if(!rename.getBefore().equals(newRename.getBefore()) && rename.getAfter().equals(newRename.getAfter())) {
				inconsistentMethodInvocationRenames.add(rename);
			}
		}
		return inconsistentMethodInvocationRenames;
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0 && allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodInlined(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation operationInsideAnonymousClass, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, operationInsideAnonymousClass);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0 && allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodInlined(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		return (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
				(mappings == 1 && nonMappedElementsT1 + nonMappedElementsT2 == 1 && operationBodyMapper.getOperation1().getName().equals(operationBodyMapper.getOperation2().getName()));
	}

	private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
		int nonMappedElementsT2WithoutThoseCallingAddedOperation = nonMappedElementsT2 - nonMappedElementsT2CallingAddedOperation;
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
				nonMappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation);
	}

	private boolean mappedElementsMoreThanNonMappedT1(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT1CallingRemovedOperation = operationBodyMapper.nonMappedElementsT1CallingRemovedOperation(removedOperations);
		int nonMappedElementsT1WithoutThoseCallingRemovedOperation = nonMappedElementsT1 - nonMappedElementsT1CallingRemovedOperation;
		return mappings > nonMappedElementsT1 || (mappings >= nonMappedElementsT1WithoutThoseCallingRemovedOperation &&
				nonMappedElementsT1CallingRemovedOperation >= nonMappedElementsT1WithoutThoseCallingRemovedOperation);
	}

	private UMLOperationBodyMapper findBestMapper(TreeSet<UMLOperationBodyMapper> mapperSet, Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
		List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>(mapperSet);
		UMLOperationBodyMapper bestMapper = mapperSet.first();
		for(int i=1; i<mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			UMLOperation operation2 = mapper.getOperation2();
			Set<OperationInvocation> operationInvocations2 = operation2.getAllOperationInvocations();
			boolean anotherMapperCallsOperation2OfTheBestMapper = false;
			for(OperationInvocation invocation : operationInvocations2) {
				if(invocation.matchesOperation(bestMapper.getOperation2()) && !invocation.matchesOperation(bestMapper.getOperation1()) &&
						!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, removedOperations)) {
					anotherMapperCallsOperation2OfTheBestMapper = true;
					break;
				}
			}
			UMLOperation operation1 = mapper.getOperation1();
			Set<OperationInvocation> operationInvocations1 = operation1.getAllOperationInvocations();
			boolean anotherMapperCallsOperation1OfTheBestMapper = false;
			for(OperationInvocation invocation : operationInvocations1) {
				if(invocation.matchesOperation(bestMapper.getOperation1()) && !invocation.matchesOperation(bestMapper.getOperation2()) &&
						!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, addedOperations)) {
					anotherMapperCallsOperation1OfTheBestMapper = true;
					break;
				}
			}
			boolean nextMapperMatchesConsistentRename = matchesConsistentMethodInvocationRename(mapper, consistentMethodInvocationRenames);
			boolean bestMapperMismatchesConsistentRename = mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames);
			if(bestMapperMismatchesConsistentRename && nextMapperMatchesConsistentRename) {
				bestMapper = mapper;
				break;
			}
			if(anotherMapperCallsOperation2OfTheBestMapper || anotherMapperCallsOperation1OfTheBestMapper) {
				bestMapper = mapper;
				break;
			}
		}
		if(mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames)) {
			return null;
		}
		return bestMapper;
	}

	private boolean matchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper, Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
		for(MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
			if(mapper.getOperation1().getName().equals(rename.getBefore()) && mapper.getOperation2().getName().equals(rename.getAfter())) {
				return true;
			}
		}
		return false;
	}

	private boolean mismatchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper, Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
		for(MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
			if(mapper.getOperation1().getName().equals(rename.getBefore()) && !mapper.getOperation2().getName().equals(rename.getAfter())) {
				return true;
			}
			else if(!mapper.getOperation1().getName().equals(rename.getBefore()) && mapper.getOperation2().getName().equals(rename.getAfter())) {
				return true;
			}
		}
		return false;
	}

	private boolean operationContainsMethodInvocationWithTheSameNameAndCommonArguments(OperationInvocation invocation, List<UMLOperation> operations) {
		for(UMLOperation operation : operations) {
			Set<OperationInvocation> operationInvocations = operation.getAllOperationInvocations();
			for(OperationInvocation operationInvocation : operationInvocations) {
				Set<String> argumentIntersection = new LinkedHashSet<String>(operationInvocation.getArguments());
				argumentIntersection.retainAll(invocation.getArguments());
				if(operationInvocation.getMethodName().equals(invocation.getMethodName()) && !argumentIntersection.isEmpty()) {
					return true;
				}
				else if(argumentIntersection.size() > 0 && argumentIntersection.size() == invocation.getArguments().size()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = removedOperationInvocations.size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation)) {
							//addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);
		
		Set<OperationInvocation> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<OperationInvocation> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			OperationInvocation invocation = operationInvocationIterator.next();
			if(invocation.getMethodName().startsWith("get")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
				numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
	}
	
	private boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromAddedOperation = addedOperationInvocations.size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation removedOperationInvocation : removedOperationInvocations) {
			if(!intersection.contains(removedOperationInvocation)) {
				for(UMLOperation operation : removedOperations) {
					if(!operation.equals(removedOperation) && operation.getBody() != null) {
						if(removedOperationInvocation.matchesOperation(operation)) {
							//removedOperation calls another removed method
							operationInvocationsInMethodsCalledByRemovedOperation.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(addedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);
		
		int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations = numberOfInvocationsMissingFromAddedOperation - numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
		return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations;
	}
	
	private boolean allMappingsAreExactMatches(UMLOperationBodyMapper operationBodyMapper, int mappings) {
		if(mappings == operationBodyMapper.exactMatches()) {
			return true;
		}
		int mappingsWithTypeReplacement = 0;
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.containsTypeReplacement()) {
				mappingsWithTypeReplacement++;
			}
		}
		if(mappings == operationBodyMapper.exactMatches() + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement) {
			return true;
		}
		return false;
	}

	private boolean compatibleSignatures(UMLOperation removedOperation, UMLOperation addedOperation, int absoluteDifferenceInPosition) {
		return addedOperation.equalParameterTypes(removedOperation) || addedOperation.overloadedParameterTypes(removedOperation) || addedOperation.replacedParameterTypes(removedOperation) ||
		(
		(absoluteDifferenceInPosition == 0 || operationsBeforeAndAfterMatch(removedOperation, addedOperation)) &&
		(addedOperation.getParameterTypeList().equals(removedOperation.getParameterTypeList()) || addedOperation.normalizedNameDistance(removedOperation) <= MAX_OPERATION_NAME_DISTANCE)
		);
	}
	
	private boolean operationsBeforeAndAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
		UMLOperation operationBefore1 = null;
		UMLOperation operationAfter1 = null;
		List<UMLOperation> originalClassOperations = originalClass.getOperations();
		for(int i=0; i<originalClassOperations.size(); i++) {
			UMLOperation current = originalClassOperations.get(i);
			if(current.equals(removedOperation)) {
				if(i>0) {
					operationBefore1 = originalClassOperations.get(i-1);
				}
				if(i<originalClassOperations.size()-1) {
					operationAfter1 = originalClassOperations.get(i+1);
				}
			}
		}
		
		UMLOperation operationBefore2 = null;
		UMLOperation operationAfter2 = null;
		List<UMLOperation> nextClassOperations = nextClass.getOperations();
		for(int i=0; i<nextClassOperations.size(); i++) {
			UMLOperation current = nextClassOperations.get(i);
			if(current.equals(addedOperation)) {
				if(i>0) {
					operationBefore2 = nextClassOperations.get(i-1);
				}
				if(i<nextClassOperations.size()-1) {
					operationAfter2 = nextClassOperations.get(i+1);
				}
			}
		}
		
		boolean operationsBeforeMatch = false;
		if(operationBefore1 != null && operationBefore2 != null) {
			operationsBeforeMatch = operationBefore1.equalParameterTypes(operationBefore2) && operationBefore1.getName().equals(operationBefore2.getName());
		}
		
		boolean operationsAfterMatch = false;
		if(operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = operationAfter1.equalParameterTypes(operationAfter2) && operationAfter1.getName().equals(operationAfter2.getName());
		}
		
		return operationsBeforeMatch || operationsAfterMatch;
	}

	public void checkForInlinedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if(!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() ||
					!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation1().getAllOperationInvocations();
					OperationInvocation removedOperationInvocation = null;
					for(OperationInvocation invocation : operationInvocations) {
						if(invocation.matchesOperation(removedOperation)) {
							removedOperationInvocation = invocation;
							break;
						}
					}
					if(removedOperationInvocation != null && !invocationMatchesWithAddedOperation(removedOperationInvocation, mapper.getOperation2().getAllOperationInvocations())) {
						List<String> arguments = removedOperationInvocation.getArguments();
						List<String> parameters = removedOperation.getParameterNameList();
						Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
						//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
						int size = Math.min(arguments.size(), parameters.size());
						for(int i=0; i<size; i++) {
							parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
						}
						UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap);
						operationBodyMapper.getMappings();
						int mappings = operationBodyMapper.mappingsWithoutBlocks();
						if(mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT1() || operationBodyMapper.exactMatches() > 0)) {
							InlineOperationRefactoring inlineOperationRefactoring =	new InlineOperationRefactoring(operationBodyMapper, mapper.getOperation1());
							refactorings.add(inlineOperationRefactoring);
							mapper.addAdditionalMapper(operationBodyMapper);
							operationsToBeRemoved.add(removedOperation);
						}
					}
				}
			}
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}

	private boolean invocationMatchesWithAddedOperation(OperationInvocation removedOperationInvocation, Set<OperationInvocation> operationInvocationsInNewMethod) {
		if(operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperationInvocation.matchesOperation(addedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	public void checkForExtractedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
			UMLOperation addedOperation = addedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
					!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation2().getAllOperationInvocations();
					OperationInvocation addedOperationInvocation = null;
					for(OperationInvocation invocation : operationInvocations) {
						if(invocation.matchesOperation(addedOperation)) {
							addedOperationInvocation = invocation;
							break;
						}
					}
					if(addedOperationInvocation != null) {
						List<UMLParameter> originalMethodParameters = mapper.getOperation1().getParametersWithoutReturnType();
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
							UMLOperation delegateMethod = findDelegateMethod(addedOperation, mapper, addedOperationInvocation);
							UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(mapper,
									delegateMethod != null ? delegateMethod : addedOperation,
									new LinkedHashMap<String, String>(), parameterToArgumentMap);
							operationBodyMapper.getMappings();
							int mappings = operationBodyMapper.mappingsWithoutBlocks();
							if(mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT2() || operationBodyMapper.exactMatches() > 0 ||
									(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2()))) {
								ExtractOperationRefactoring extractOperationRefactoring = null;
								if(delegateMethod == null) {
									extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2());
								}
								else {
									extractOperationRefactoring = new ExtractOperationRefactoring(addedOperation, mapper.getOperation1(), mapper.getOperation2());
								}
								refactorings.add(extractOperationRefactoring);
								mapper.addAdditionalMapper(operationBodyMapper);
								operationsToBeRemoved.add(addedOperation);
							}
						}
					}
				}
			}
		}
		addedOperations.removeAll(operationsToBeRemoved);
	}

	private UMLOperation findDelegateMethod(UMLOperation addedOperation, UMLOperationBodyMapper mapper,	OperationInvocation addedOperationInvocation) {
		OperationInvocation delegateMethodInvocation = addedOperation.isDelegate();
		if(mapper.getOperation1().isDelegate() == null && delegateMethodInvocation != null && !mapper.getOperation1().getAllOperationInvocations().contains(addedOperationInvocation)) {
			for(UMLOperation operation : addedOperations) {
				if(delegateMethodInvocation.matchesOperation(operation)) {
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append(className).append(":").append("\n");
		if(visibilityChanged) {
			sb.append("\t").append("visibility changed from " + oldVisibility + " to " + newVisibility).append("\n");
		}
		if(abstractionChanged) {
			sb.append("\t").append("abstraction changed from " + (oldAbstraction ? "abstract" : "concrete") + " to " +
					(newAbstraction ? "abstract" : "concrete")).append("\n");
		}
		Collections.sort(removedOperations);
		for(UMLOperation umlOperation : removedOperations) {
			sb.append("operation " + umlOperation + " removed").append("\n");
		}
		Collections.sort(addedOperations);
		for(UMLOperation umlOperation : addedOperations) {
			sb.append("operation " + umlOperation + " added").append("\n");
		}
		Collections.sort(removedAttributes);
		for(UMLAttribute umlAttribute : removedAttributes) {
			sb.append("attribute " + umlAttribute + " removed").append("\n");
		}
		Collections.sort(addedAttributes);
		for(UMLAttribute umlAttribute : addedAttributes) {
			sb.append("attribute " + umlAttribute + " added").append("\n");
		}
		for(UMLOperationDiff operationDiff : operationDiffList) {
			sb.append(operationDiff);
		}
		for(UMLAttributeDiff attributeDiff : attributeDiffList) {
			sb.append(attributeDiff);
		}
		Collections.sort(operationBodyMapperList);
		for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
			sb.append(operationBodyMapper);
		}
		return sb.toString();
	}

	public int compareTo(UMLClassDiff classDiff) {
		return this.className.compareTo(classDiff.className);
	}

	public boolean nextClassImportsType(String targetClass) {
		return nextClass.importsType(targetClass);
	}

	public boolean originalClassImportsType(String targetClass) {
		return originalClass.importsType(targetClass);
	}
	
	public List<UMLAttribute> originalClassAttributesOfType(String targetClass) {
		return originalClass.attributesOfType(targetClass);
	}
}
