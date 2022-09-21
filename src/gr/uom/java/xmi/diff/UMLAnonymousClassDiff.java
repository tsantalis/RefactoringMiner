package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class UMLAnonymousClassDiff extends UMLAbstractClassDiff {
	private UMLAbstractClassDiff classDiff;
	
	public UMLAnonymousClassDiff(UMLAnonymousClass anonymousClass1, UMLAnonymousClass anonymousClass2, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		super(anonymousClass1, anonymousClass2, modelDiff);
		this.classDiff = classDiff;
	}

	@Override
	public void process() throws RefactoringMinerTimedOutException {
		processInitializers();
		processOperations();
		createBodyMappers();
		checkForOperationSignatureChanges();
		processAttributes();
		checkForAttributeChanges();
		checkForInlinedOperations();
		checkForExtractedOperations();
	}

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() &&
			addedAttributes.isEmpty() && removedAttributes.isEmpty() &&
			addedEnumConstants.isEmpty() && removedEnumConstants.isEmpty() &&
			attributeDiffList.isEmpty() && enumConstantDiffList.isEmpty();
	}

	protected void processInitializers() throws RefactoringMinerTimedOutException {
		for(UMLInitializer initializer1 : originalClass.getInitializers()) {
			for(UMLInitializer initializer2 : nextClass.getInitializers()) {
				if(initializer1.isStatic() == initializer2.isStatic()) {
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(initializer1, initializer2, classDiff);
					int mappings = mapper.mappingsWithoutBlocks();
					if(mappings > 0) {
						int nonMappedElementsT1 = mapper.nonMappedElementsT1();
						int nonMappedElementsT2 = mapper.nonMappedElementsT2();
						if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
								isPartOfMethodExtracted(initializer1, initializer2) || isPartOfMethodInlined(initializer1, initializer2)) {
							operationBodyMapperList.add(mapper);
						}
					}
				}
			}
		}
	}

	protected void processOperations() {
		for(UMLOperation operation : originalClass.getOperations()) {
    		if(!nextClass.getOperations().contains(operation))
    			removedOperations.add(operation);
    	}
    	for(UMLOperation operation : nextClass.getOperations()) {
    		if(!originalClass.getOperations().contains(operation))
    			addedOperations.add(operation);
    	}
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = nextClass.containsAttribute(attribute);
    		if(matchingAttribute != null) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, operationBodyMapperList);
    			if(!attributeDiff.isEmpty()) {
    				refactorings.addAll(attributeDiff.getRefactorings());
    				attributeDiffList.add(attributeDiff);
    			}
    			else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, matchingAttribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    			}
    		}
    		else {
    			removedAttributes.add(attribute);
    		}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute matchingAttribute = originalClass.containsAttribute(attribute);
    		if(matchingAttribute != null) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, operationBodyMapperList);
    			if(!attributeDiff.isEmpty()) {
    				refactorings.addAll(attributeDiff.getRefactorings());
    				attributeDiffList.add(attributeDiff);
    			}
    			else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(matchingAttribute, attribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    			}
    		}
    		else {
    			addedAttributes.add(attribute);
    		}
    	}
	}

	@Override
	protected void createBodyMappers() throws RefactoringMinerTimedOutException {
		for(UMLOperation operation1 : originalClass.getOperations()) {
			for(UMLOperation operation2 : nextClass.getOperations()) {
				if(operation1.equals(operation2) || operation1.equalSignature(operation2)) {	
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation1, operation2, classDiff);
					int mappings = mapper.mappingsWithoutBlocks();
					if(mappings > 0) {
						int nonMappedElementsT1 = mapper.nonMappedElementsT1();
						int nonMappedElementsT2 = mapper.nonMappedElementsT2();
						if((mappings > nonMappedElementsT1 || mappings > nonMappedElementsT2) ||
								isPartOfMethodExtracted(operation1, operation2) || isPartOfMethodInlined(operation1, operation2)) {
							operationBodyMapperList.add(mapper);
							removedOperations.remove(operation1);
							addedOperations.remove(operation2);
						}
					}
				}
			}
		}
	}

	private void checkForOperationSignatureChanges() throws RefactoringMinerTimedOutException {
		for(UMLOperation operation1 : originalClass.getOperations()) {
			for(UMLOperation operation2 : nextClass.getOperations()) {
				if(!containsMapperForOperation1(operation1) && !containsMapperForOperation2(operation2)) {
					if(operation1.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation2) ||
						(operation1.getName().equals(operation2.getName()) && operation1.compatibleSignature(operation2))) {
						UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation1, operation2, classDiff);
						int mappings = mapper.mappingsWithoutBlocks();
						if(mappings > 0) {
							int nonMappedElementsT1 = mapper.nonMappedElementsT1();
							int nonMappedElementsT2 = mapper.nonMappedElementsT2();
							if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
									isPartOfMethodExtracted(operation1, operation2) || isPartOfMethodInlined(operation1, operation2)) {
								operationBodyMapperList.add(mapper);
								removedOperations.remove(operation1);
								addedOperations.remove(operation2);
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected void checkForAttributeChanges() throws RefactoringMinerTimedOutException {
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if(removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute, operationBodyMapperList);
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					refactorings.addAll(attributeDiff.getRefactorings());
					attributeDiffList.add(attributeDiff);
					break;
				}
			}
		}
	}

	private void checkForExtractedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
			UMLOperation addedOperation = addedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				ExtractOperationDetection detection = new ExtractOperationDetection(mapper, addedOperations, classDiff, modelDiff);
				List<ExtractOperationRefactoring> refs = detection.check(addedOperation);
				for(ExtractOperationRefactoring refactoring : refs) {
					refactorings.add(refactoring);
					UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
					mapper.addChildMapper(operationBodyMapper);
					operationsToBeRemoved.add(addedOperation);
				}
			}
		}
		addedOperations.removeAll(operationsToBeRemoved);
	}

	private void checkForInlinedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				InlineOperationDetection detection = new InlineOperationDetection(mapper, removedOperations, classDiff, modelDiff);
				List<InlineOperationRefactoring> refs = detection.check(removedOperation);
				for(InlineOperationRefactoring refactoring : refs) {
					refactorings.add(refactoring);
					UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
					mapper.addChildMapper(operationBodyMapper);
					operationsToBeRemoved.add(removedOperation);
				}
			}
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}
}
