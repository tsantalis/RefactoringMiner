package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
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

	protected boolean isPartOfMethodMovedFromExistingMethod(UMLOperation removedOperation, UMLOperation addedOperation) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
			List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
			Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
			Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
			for(AbstractCall invocation : invocationsCalledOnlyInOperation2) {
				if(invocation.matchesOperation(addedOperation, mapper.getContainer2(), modelDiff)) {
					List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
					List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
					Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
					movedInvocations.removeAll(removedOperationInvocations);
					movedInvocations.retainAll(invocationsCalledOnlyInOperation1);
					Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
					intersection.retainAll(removedOperationInvocations);
					int chainedCalls = 0;
					AbstractCall previous = null;
					for(AbstractCall inv : intersection) {
						if(previous != null && previous.getExpression() != null && previous.getExpression().equals(inv.actualString())) {
							chainedCalls++;
						}
						previous = inv;
					}
					if(movedInvocations.size() > 1 && intersection.size() - chainedCalls > 1) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean isPartOfMethodMovedToExistingMethod(UMLOperation removedOperation, UMLOperation addedOperation) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
			List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
			Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
			Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
			for(AbstractCall invocation : invocationsCalledOnlyInOperation1) {
				if(invocation.matchesOperation(removedOperation, mapper.getContainer1(), modelDiff)) {
					List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
					List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
					Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
					movedInvocations.removeAll(addedOperationInvocations);
					movedInvocations.retainAll(invocationsCalledOnlyInOperation2);
					Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
					intersection.retainAll(addedOperationInvocations);
					int chainedCalls = 0;
					AbstractCall previous = null;
					for(AbstractCall inv : intersection) {
						if(previous != null && previous.getExpression() != null && previous.getExpression().equals(inv.actualString())) {
							chainedCalls++;
						}
						previous = inv;
					}
					int renamedCalls = 0;
					for(AbstractCall inv : addedOperationInvocations) {
						if(!intersection.contains(inv)) {
							for(Refactoring ref : refactorings) {
								if(ref instanceof RenameOperationRefactoring) {
									RenameOperationRefactoring rename = (RenameOperationRefactoring)ref;
									if(inv.matchesOperation(rename.getRenamedOperation(), addedOperation, modelDiff)) {
										renamedCalls++;
										break;
									}
								}
							}
						}
					}
					if(movedInvocations.size() > 1 && intersection.size() + renamedCalls - chainedCalls > 1) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = new LinkedHashSet<AbstractCall>(removedOperationInvocations).size() - intersection.size();
		
		Set<String> removedOperationVariableDeclarationNames = getVariableDeclarationNamesInMethodBody(removedOperation);
		Set<String> addedOperationVariableDeclarationNames = getVariableDeclarationNamesInMethodBody(addedOperation);
		Set<String> variableDeclarationIntersection = new LinkedHashSet<String>(removedOperationVariableDeclarationNames);
		variableDeclarationIntersection.retainAll(addedOperationVariableDeclarationNames);
		int numberOfVariableDeclarationsMissingFromRemovedOperation = removedOperationVariableDeclarationNames.size() - variableDeclarationIntersection.size();
		
		Set<AbstractCall> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<AbstractCall>();
		Set<String> variableDeclarationsInMethodsCalledByAddedOperation = new LinkedHashSet<String>();
		Set<AbstractCall> matchedOperationInvocations = new LinkedHashSet<AbstractCall>();
		for(AbstractCall addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation, addedOperation, modelDiff)) {
							//addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
							variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
							matchedOperationInvocations.add(addedOperationInvocation);
						}
					}
				}
			}
		}
		//this is to support the Extract & Move Method scenario
		if(modelDiff != null) {
			for(AbstractCall addedOperationInvocation : addedOperationInvocations) {
				String expression = addedOperationInvocation.getExpression();
				if(expression != null && !expression.equals("this") &&
						!intersection.contains(addedOperationInvocation) && !matchedOperationInvocations.contains(addedOperationInvocation)) {
					UMLOperation operation = modelDiff.findOperationInAddedClasses(addedOperationInvocation, addedOperation);
					if(operation != null) {
						operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
						variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
					}
				}
			}
		}
		Set<AbstractCall> newIntersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);
		
		Set<String> newVariableDeclarationIntersection = new LinkedHashSet<String>(removedOperationVariableDeclarationNames);
		newVariableDeclarationIntersection.retainAll(variableDeclarationsInMethodsCalledByAddedOperation);
		
		Set<AbstractCall> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<AbstractCall> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			AbstractCall invocation = operationInvocationIterator.next();
			if(invocation.getName().startsWith("get") || invocation.getName().equals("add") || invocation.getName().equals("contains")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		
		int numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations = newVariableDeclarationIntersection.size();
		int numberOfVariableDeclarationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfVariableDeclarationsMissingFromRemovedOperation - numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations;
		
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
				numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size() ||
				numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations > numberOfVariableDeclarationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations;
	}

	private Set<String> getVariableDeclarationNamesInMethodBody(UMLOperation operation) {
		if(operation.getBody() != null) {
			Set<String> keySet = new LinkedHashSet<>(operation.variableDeclarationMap().keySet());
			keySet.removeAll(operation.getParameterNameList());
			return keySet;
		}
		return Collections.emptySet();
	}

	protected boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromAddedOperation = new LinkedHashSet<AbstractCall>(addedOperationInvocations).size() - intersection.size();
		
		Set<AbstractCall> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<AbstractCall>();
		for(AbstractCall removedOperationInvocation : removedOperationInvocations) {
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
		Set<AbstractCall> newIntersection = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);
		
		Set<AbstractCall> addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
		addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<AbstractCall> operationInvocationIterator = addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			AbstractCall invocation = operationInvocationIterator.next();
			if(invocation.getName().startsWith("get") || invocation.getName().equals("add") || invocation.getName().equals("contains")) {
				operationInvocationIterator.remove();
			}
		}
		
		int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations = numberOfInvocationsMissingFromAddedOperation - numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
		return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations ||
				numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
	}
}
