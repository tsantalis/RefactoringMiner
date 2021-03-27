package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public abstract class UMLAbstractClassDiff {
	protected List<UMLOperation> addedOperations;
	protected List<UMLOperation> removedOperations;
	protected List<UMLAttribute> addedAttributes;
	protected List<UMLAttribute> removedAttributes;
	protected List<UMLOperationBodyMapper> operationBodyMapperList;
	protected List<UMLOperationDiff> operationDiffList;
	protected List<UMLAttributeDiff> attributeDiffList;
	protected List<Refactoring> refactorings;
	protected UMLModelDiff modelDiff;
	
	public UMLAbstractClassDiff(UMLModelDiff modelDiff) {
		this.addedOperations = new ArrayList<UMLOperation>();
		this.removedOperations = new ArrayList<UMLOperation>();
		this.addedAttributes = new ArrayList<UMLAttribute>();
		this.removedAttributes = new ArrayList<UMLAttribute>();
		this.operationBodyMapperList = new ArrayList<UMLOperationBodyMapper>();
		this.operationDiffList = new ArrayList<UMLOperationDiff>();
		this.attributeDiffList = new ArrayList<UMLAttributeDiff>();
		this.refactorings = new ArrayList<Refactoring>();
		this.modelDiff = modelDiff;		
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

	public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
		return operationBodyMapperList;
	}

	public List<UMLOperationDiff> getOperationDiffList() {
		return operationDiffList;
	}

	public List<UMLAttributeDiff> getAttributeDiffList() {
		return attributeDiffList;
	}

	public abstract void process() throws RefactoringMinerTimedOutException;
	
	protected abstract void checkForAttributeChanges() throws RefactoringMinerTimedOutException;

	protected abstract void createBodyMappers() throws RefactoringMinerTimedOutException;

	protected boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = new LinkedHashSet<OperationInvocation>(removedOperationInvocations).size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation, addedOperation, modelDiff)) {
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

	protected boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromAddedOperation = new LinkedHashSet<OperationInvocation>(addedOperationInvocations).size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation removedOperationInvocation : removedOperationInvocations) {
			if(!intersection.contains(removedOperationInvocation)) {
				for(UMLOperation operation : removedOperations) {
					if(!operation.equals(removedOperation) && operation.getBody() != null) {
						if(removedOperationInvocation.matchesOperation(operation, removedOperation, modelDiff)) {
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
}
