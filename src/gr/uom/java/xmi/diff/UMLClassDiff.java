package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

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
    		if(!nextClass.getAttributes().contains(attribute))
    			this.reportRemovedAttribute(attribute);
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		if(!originalClass.getAttributes().contains(attribute))
    			this.reportAddedAttribute(attribute);
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

	protected void createBodyMappers() {
		for(UMLOperation operation : originalClass.getOperations()) {
    		if(nextClass.getOperations().contains(operation)) {
    			int index = nextClass.getOperations().indexOf(operation);
    			int lastIndex = nextClass.getOperations().lastIndexOf(operation);
    			int finalIndex = index;
    			if(index != lastIndex) {
    				if(operation.equalsQualified(nextClass.getOperations().get(lastIndex)))
    					finalIndex = lastIndex;
    			}
    			UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(operation, nextClass.getOperations().get(finalIndex), this);
    			this.addOperationBodyMapper(operationBodyMapper);
    			//map the statements when the method calls a removed operation before refactoring, and an added operation after refactoring
    			if(operationBodyMapper.callsRemovedAndAddedOperation(getRemovedOperations(), getAddedOperations())) {
    				operationBodyMapper.getMappings();
    			}
    		}
    	}
		List<UMLOperation> removedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperation> addedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		for(UMLOperation removedOperation : removedOperations) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperation.equalsIgnoringVisibility(addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					//map the statements when the method calls a removed operation before refactoring, and an added operation after refactoring
					if(operationBodyMapper.callsRemovedAndAddedOperation(getRemovedOperations(), getAddedOperations())) {
						operationBodyMapper.getMappings();
					}
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
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					attributeDiffList.add(attributeDiff);
					break;
				}
			}
		}
	}

	public boolean matches(String className) {
		return this.className.equals(className);
	}

	public boolean matches(UMLType type) {
		return this.className.endsWith("." + type.getClassType());
	}
}
