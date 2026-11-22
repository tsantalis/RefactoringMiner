package gr.uom.java.xmi.diff;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

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
		//the pairs of the attributes matched from the original to the next class, whose diffs are not created again from the next to the original class
		Map<UMLAttribute, UMLAttribute> matchedAttributes = new IdentityHashMap<>();
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = nextClass.containsAttribute(attribute);
			if(matchingAttribute == null && originalClass.getTypeDeclarationKind().equals("class") && nextClass.getTypeDeclarationKind().equals("enum")) {
				matchingAttribute = nextClass.containsEnumConstantWithSameName(attribute);
			}
    		if(matchingAttribute == null) {
    			this.reportRemovedAttribute(attribute);
    		}
    		else {
    			matchedAttributes.put(attribute, matchingAttribute);
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, this, modelDiff);
    			if(!attributeDiff.isEmpty()) {
	    			refactorings.addAll(attributeDiff.getRefactorings());
	    			if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
    			}
    			else {
    				addCommonAttributes(attribute, matchingAttribute);
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
    		}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute matchingAttribute = originalClass.containsAttribute(attribute);
    		if(matchingAttribute == null && originalClass.getTypeDeclarationKind().equals("enum") && nextClass.getTypeDeclarationKind().equals("class")) {
				matchingAttribute = originalClass.containsEnumConstantWithSameName(attribute);
			}
    		if(matchingAttribute == null) {
    			this.reportAddedAttribute(attribute);
    		}
    		else {
    			if(matchedAttributes.get(matchingAttribute) == attribute)
    				continue;
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, this, modelDiff);
    			if(!attributeDiff.isEmpty()) {
	    			refactorings.addAll(attributeDiff.getRefactorings());
	    			if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
    			}
    			else {
    				addCommonAttributes(matchingAttribute, attribute);
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
				operation2 = equalOperationWithSameParameterNames(operation, nextClass.getOperations(), index);
				if(testAnnotationMismatch(operation, operation2)) {
					this.reportRemovedOperation(operation);
				}
			}
			if(index == -1 || differentParameterNames(operation, operation2))
				this.reportRemovedOperation(operation);
		}
		for(UMLOperation operation : nextClass.getOperations()) {
			int index = originalClass.getOperations().indexOf(operation);
			UMLOperation operation1 = null;
			if(index != -1) {
				operation1 = equalOperationWithSameParameterNames(operation, originalClass.getOperations(), index);
				if(testAnnotationMismatch(operation1, operation)) {
					this.reportAddedOperation(operation);
				}
			}
			if(index == -1 || differentParameterNames(operation1, operation))
				this.reportAddedOperation(operation);
		}
	}

	//the operation equal to the given operation with the same parameter names among the equal operations starting at the index, or the first equal operation,
	//i.e., the second declaration of decode_null(attribute *patr, char *name, char *rn, char *val) in a C header declaring it also as decode_null(attribute*, char*, char*, char*)
	private UMLOperation equalOperationWithSameParameterNames(UMLOperation operation, List<UMLOperation> operations, int index) {
		UMLOperation first = operations.get(index);
		if(!differentParameterNames(operation, first)) {
			return first;
		}
		for(int i=index+1; i<operations.size(); i++) {
			UMLOperation candidate = operations.get(i);
			if(candidate.equals(operation) && !differentParameterNames(operation, candidate)) {
				return candidate;
			}
		}
		return first;
	}

	public boolean matches(String className) {
		return this.className.equals(className) || this.className.equals(className + ".__module__");
	}

	public boolean matches(UMLType type) {
		return this.className.endsWith("." + type.getClassType());
	}
}
