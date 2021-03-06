package gr.uom.java.xmi.diff;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class UMLAnonymousClassDiff extends UMLAbstractClassDiff {
	private UMLAnonymousClass anonymousClass1;
	private UMLAnonymousClass anonymousClass2;
	private UMLClassBaseDiff classDiff;
	
	public UMLAnonymousClassDiff(UMLAnonymousClass anonymousClass1, UMLAnonymousClass anonymousClass2, UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
		this.anonymousClass1 = anonymousClass1;
		this.anonymousClass2 = anonymousClass2;
		this.classDiff = classDiff;
	}

	@Override
	public void process() throws RefactoringMinerTimedOutException {
		createBodyMappers();
		checkForAttributeChanges();
	}

	@Override
	protected void checkForAttributeChanges() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : anonymousClass1.getAttributes()) {
			UMLAttribute matchingAttribute = anonymousClass2.containsAttribute(attribute);
    		if(matchingAttribute != null) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, operationBodyMapperList);
    			if(!attributeDiff.isEmpty()) {
    				attributeDiffList.add(attributeDiff);
    			}
    		}
    	}
    	for(UMLAttribute attribute : anonymousClass2.getAttributes()) {
    		UMLAttribute matchingAttribute = anonymousClass1.containsAttribute(attribute);
    		if(matchingAttribute != null) {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, operationBodyMapperList);
    			if(!attributeDiff.isEmpty()) {
    				attributeDiffList.add(attributeDiff);
    			}
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
						if(mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) {
							operationBodyMapperList.add(mapper);
							UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2, mapper.getMappings());
							operationDiffList.add(operationDiff);
						}
					}
				}
			}
		}
	}
}
