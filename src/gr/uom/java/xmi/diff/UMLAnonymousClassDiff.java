package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class UMLAnonymousClassDiff extends UMLAbstractClassDiff {
	private UMLAnonymousClass anonymousClass1;
	private UMLAnonymousClass anonymousClass2;
	private UMLClassBaseDiff classDiff;
	
	public UMLAnonymousClassDiff(UMLAnonymousClass anonymousClass1, UMLAnonymousClass anonymousClass2, UMLClassBaseDiff classDiff, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		super(modelDiff);
		this.anonymousClass1 = anonymousClass1;
		this.anonymousClass2 = anonymousClass2;
		this.classDiff = classDiff;
	}

	@Override
	public void process() throws RefactoringMinerTimedOutException {
		processOperations();
		createBodyMappers();
		processAttributes();
		checkForAttributeChanges();
		checkForInlinedOperations();
		checkForExtractedOperations();
	}

	public List<Refactoring> getRefactorings() {
		return refactorings;
	}

	protected void processOperations() {
		for(UMLOperation operation : anonymousClass1.getOperations()) {
    		if(!anonymousClass2.getOperations().contains(operation))
    			removedOperations.add(operation);
    	}
    	for(UMLOperation operation : anonymousClass2.getOperations()) {
    		if(!anonymousClass1.getOperations().contains(operation))
    			addedOperations.add(operation);
    	}
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : anonymousClass1.getAttributes()) {
			UMLAttribute matchingAttribute = anonymousClass2.containsAttribute(attribute);
    		if(matchingAttribute != null) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, operationBodyMapperList);
    			if(!attributeDiff.isEmpty()) {
    				refactorings.addAll(attributeDiff.getRefactorings());
    				attributeDiffList.add(attributeDiff);
    			}
    		}
    		else {
    			removedAttributes.add(attribute);
    		}
    	}
    	for(UMLAttribute attribute : anonymousClass2.getAttributes()) {
    		UMLAttribute matchingAttribute = anonymousClass1.containsAttribute(attribute);
    		if(matchingAttribute != null) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, operationBodyMapperList);
    			if(!attributeDiff.isEmpty()) {
    				refactorings.addAll(attributeDiff.getRefactorings());
    				attributeDiffList.add(attributeDiff);
    			}
    		}
    		else {
    			addedAttributes.add(attribute);
    		}
    	}
	}

	@Override
	protected void createBodyMappers() throws RefactoringMinerTimedOutException {
		for(UMLOperation operation1 : anonymousClass1.getOperations()) {
			for(UMLOperation operation2 : anonymousClass2.getOperations()) {
				if(operation1.equals(operation2) || operation1.equalSignature(operation2) || operation1.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation2)) {	
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation1, operation2, classDiff);
					int mappings = mapper.mappingsWithoutBlocks();
					if(mappings > 0) {
						int nonMappedElementsT1 = mapper.nonMappedElementsT1();
						int nonMappedElementsT2 = mapper.nonMappedElementsT2();
						if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
								isPartOfMethodExtracted(operation1, operation2) || isPartOfMethodInlined(operation1, operation2)) {
							operationBodyMapperList.add(mapper);
							UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2, mapper.getMappings());
							operationDiffList.add(operationDiff);
							removedOperations.remove(operation1);
							addedOperations.remove(operation2);
							refactorings.addAll(mapper.getRefactorings());
							refactorings.addAll(operationDiff.getRefactorings());
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
