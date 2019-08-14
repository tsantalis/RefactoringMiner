package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableReferenceExtractor;

public class UMLClassDiff extends UMLClassBaseDiff {
	
	private String className;
	public UMLClassDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
		super(originalClass, nextClass, modelDiff);
		this.className = originalClass.getName();
	}

	private void reportAddedOperation(UMLOperation umlOperation) {
		this.addedOperations.add(umlOperation);
	}

	private void reportRemovedOperation(UMLOperation umlOperation) {
		this.removedOperations.add(umlOperation);
	}

	private void reportAddedAttribute(UMLAttribute umlAttribute) {
		this.addedAttributes.add(umlAttribute);
	}

	private void reportRemovedAttribute(UMLAttribute umlAttribute) {
		this.removedAttributes.add(umlAttribute);
	}

	protected void processAttributes() {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = nextClass.containsAttribute(attribute);
    		if(matchingAttribute == null) {
    			this.reportRemovedAttribute(attribute);
    		}
    		else if(!attribute.equalsQualified(matchingAttribute)) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute);
				if(attributeDiff.isTypeChanged() || attributeDiff.isQualifiedTypeChanged()) {
					ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(attribute.getVariableDeclaration(), matchingAttribute.getVariableDeclaration(), originalClass.getName(), nextClass.getName(),
							VariableReferenceExtractor.findReferences(attribute.getVariableDeclaration(), matchingAttribute.getVariableDeclaration(), getOperationBodyMapperList()));
					refactorings.add(ref);
				}
				this.attributeDiffList.add(attributeDiff);
    		}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute matchingAttribute = originalClass.containsAttribute(attribute);
    		if(matchingAttribute == null) {
    			this.reportAddedAttribute(attribute);
    		}
    		else if(!attribute.equalsQualified(matchingAttribute)) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute);
				if(attributeDiff.isTypeChanged() || attributeDiff.isQualifiedTypeChanged()) {
					ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(matchingAttribute.getVariableDeclaration(), attribute.getVariableDeclaration(), originalClass.getName(), nextClass.getName(),
							VariableReferenceExtractor.findReferences(matchingAttribute.getVariableDeclaration(), attribute.getVariableDeclaration(), getOperationBodyMapperList()));
					refactorings.add(ref);
				}
				this.attributeDiffList.add(attributeDiff);
    		}
    	}
	}

	protected void processOperations() {
		for(UMLOperation operation : originalClass.getOperations()) {
    		if(!nextClass.getOperations().contains(operation))
    			this.reportRemovedOperation(operation);
    	}
    	for(UMLOperation operation : nextClass.getOperations()) {
    		if(!originalClass.getOperations().contains(operation))
    			this.reportAddedOperation(operation);
    	}
	}

	protected void processAnonymousClasses() {
		for(UMLAnonymousClass umlAnonymousClass : originalClass.getAnonymousClassList()) {
    		if(!nextClass.getAnonymousClassList().contains(umlAnonymousClass))
    			this.reportRemovedAnonymousClass(umlAnonymousClass);
    	}
    	for(UMLAnonymousClass umlAnonymousClass : nextClass.getAnonymousClassList()) {
    		if(!originalClass.getAnonymousClassList().contains(umlAnonymousClass))
    			this.reportAddedAnonymousClass(umlAnonymousClass);
    	}
	}

	protected void createBodyMappers() throws RefactoringMinerTimedOutException {
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			for(UMLOperation nextOperation : nextClass.getOperations()) {
				if(originalOperation.equalsQualified(nextOperation)) {
					if(getModelDiff() != null) {
						List<UMLOperationBodyMapper> mappers = getModelDiff().findMappersWithMatchingSignature2(nextOperation);
						if(mappers.size() > 0) {
							UMLOperation operation1 = mappers.get(0).getOperation1();
							if(!operation1.equalSignature(originalOperation) &&
									getModelDiff().commonlyImplementedOperations(operation1, nextOperation, this)) {
								if(!removedOperations.contains(originalOperation)) {
									removedOperations.add(originalOperation);
								}
								break;
							}
						}
					}
	    			UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(originalOperation, nextOperation, this);
	    			UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(originalOperation, nextOperation, operationBodyMapper.getMappings());
					refactorings.addAll(operationSignatureDiff.getRefactorings());
	    			this.addOperationBodyMapper(operationBodyMapper);
				}
			}
		}
		for(UMLOperation operation : originalClass.getOperations()) {
			if(!containsMapperForOperation(operation) && nextClass.getOperations().contains(operation) && !removedOperations.contains(operation)) {
    			int index = nextClass.getOperations().indexOf(operation);
    			int lastIndex = nextClass.getOperations().lastIndexOf(operation);
    			int finalIndex = index;
    			if(index != lastIndex) {
    				double d1 = operation.getReturnParameter().getType().normalizedNameDistance(nextClass.getOperations().get(index).getReturnParameter().getType());
    				double d2 = operation.getReturnParameter().getType().normalizedNameDistance(nextClass.getOperations().get(lastIndex).getReturnParameter().getType());
    				if(d2 < d1) {
    					finalIndex = lastIndex;
    				}
    			}
    			UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(operation, nextClass.getOperations().get(finalIndex), this);
    			UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(operation, nextClass.getOperations().get(finalIndex), operationBodyMapper.getMappings());
    			refactorings.addAll(operationSignatureDiff.getRefactorings());
    			this.addOperationBodyMapper(operationBodyMapper);
    		}
    	}
		List<UMLOperation> removedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperation> addedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		for(UMLOperation removedOperation : removedOperations) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperation.equalsIgnoringVisibility(addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(removedOperation, addedOperation, operationBodyMapper.getMappings());
					refactorings.addAll(operationSignatureDiff.getRefactorings());
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
				else if(removedOperation.equalsIgnoringNameCase(addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(removedOperation, addedOperation, operationBodyMapper.getMappings());
					refactorings.addAll(operationSignatureDiff.getRefactorings());
					if(!removedOperation.getName().equals(addedOperation.getName()) &&
							!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(operationBodyMapper);
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
			}
		}
		removedOperations.removeAll(removedOperationsToBeRemoved);
		addedOperations.removeAll(addedOperationsToBeRemoved);
	}

	protected void checkForAttributeChanges() {
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if(removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute);
					if(attributeDiff.isTypeChanged() || attributeDiff.isQualifiedTypeChanged()) {
						ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(removedAttribute.getVariableDeclaration(), addedAttribute.getVariableDeclaration(), originalClass.getName(), nextClass.getName(),
								VariableReferenceExtractor.findReferences(removedAttribute.getVariableDeclaration(), addedAttribute.getVariableDeclaration(), getOperationBodyMapperList()));
						refactorings.add(ref);
					}
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					attributeDiffList.add(attributeDiff);
					break;
				}
			}
		}
	}

	private boolean containsMapperForOperation(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getOperation1().equalsQualified(operation)) {
				return true;
			}
		}
		return false;
	}

	public boolean matches(String className) {
		return this.className.equals(className);
	}

	public boolean matches(UMLType type) {
		return this.className.endsWith("." + type.getClassType());
	}
}
