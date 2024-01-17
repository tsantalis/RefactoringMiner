package gr.uom.java.xmi.diff;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;

public class UMLClassDiff extends UMLClassBaseDiff {
	
	private String className;
	public UMLClassDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
		super(originalClass, nextClass, modelDiff);
		this.className = originalClass.getName();
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = nextClass.containsAttribute(attribute);
    		if(matchingAttribute == null) {
    			this.reportRemovedAttribute(attribute);
    		}
    		else {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, this, modelDiff);
    			if(!attributeDiff.isEmpty()) {
	    			refactorings.addAll(attributeDiff.getRefactorings());
	    			if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
    			}
    			else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, matchingAttribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
    		}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute matchingAttribute = originalClass.containsAttribute(attribute);
    		if(matchingAttribute == null) {
    			this.reportAddedAttribute(attribute);
    		}
    		else {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, this, modelDiff);
    			if(!attributeDiff.isEmpty()) {
	    			refactorings.addAll(attributeDiff.getRefactorings());
	    			if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
    			}
    			else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(matchingAttribute, attribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
    		}
    	}
	}

	protected void processOperations() {
		for(UMLOperation operation : originalClass.getOperations()) {
			int index = nextClass.getOperations().indexOf(operation);
			UMLOperation operation2 = null;
			if(index != -1) {
				operation2 = nextClass.getOperations().get(index);
			}
    		if(index == -1 || differentParameterNames(operation, operation2))
    			this.reportRemovedOperation(operation);
    	}
    	for(UMLOperation operation : nextClass.getOperations()) {
    		int index = originalClass.getOperations().indexOf(operation);
    		UMLOperation operation1 = null;
    		if(index != -1) {
    			operation1 = originalClass.getOperations().get(index);
    		}
    		if(index == -1 || differentParameterNames(operation1, operation))
    			this.reportAddedOperation(operation);
    	}
	}

	public boolean matches(String className) {
		return this.className.equals(className);
	}

	public boolean matches(UMLType type) {
		return this.className.endsWith("." + type.getClassType());
	}
}
