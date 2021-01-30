package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class UMLAnonymousClassDiff {
	private List<UMLOperationBodyMapper> matchedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
	private List<UMLOperationDiff> operationDiffs = new ArrayList<UMLOperationDiff>();
	private List<UMLAttributeDiff> attributeDiffs = new ArrayList<UMLAttributeDiff>();
	
	public UMLAnonymousClassDiff(UMLAnonymousClass anonymousClass1, UMLAnonymousClass anonymousClass2, UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
		for(UMLOperation operation1 : anonymousClass1.getOperations()) {
			for(UMLOperation operation2 : anonymousClass2.getOperations()) {
				if(operation1.equals(operation2) || operation1.equalSignature(operation2) || operation1.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation2)) {	
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation1, operation2, classDiff);
					int mappings = mapper.mappingsWithoutBlocks();
					if(mappings > 0) {
						int nonMappedElementsT1 = mapper.nonMappedElementsT1();
						int nonMappedElementsT2 = mapper.nonMappedElementsT2();
						if(mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) {
							matchedOperationMappers.add(mapper);
							UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2, mapper.getMappings());
							operationDiffs.add(operationDiff);
						}
					}
				}
			}
		}
		if(matchedOperationMappers.size() > 0) {
			for(UMLAttribute attribute : anonymousClass1.getAttributes()) {
				UMLAttribute matchingAttribute = anonymousClass2.containsAttribute(attribute);
	    		if(matchingAttribute != null) {
	    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, matchedOperationMappers);
	    			if(!attributeDiff.isEmpty()) {
	    				attributeDiffs.add(attributeDiff);
	    			}
	    		}
	    	}
	    	for(UMLAttribute attribute : anonymousClass2.getAttributes()) {
	    		UMLAttribute matchingAttribute = anonymousClass1.containsAttribute(attribute);
	    		if(matchingAttribute != null) {
	    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, matchedOperationMappers);
	    			if(!attributeDiff.isEmpty()) {
	    				attributeDiffs.add(attributeDiff);
	    			}
	    		}
	    	}
		}
	}

	public List<UMLOperationBodyMapper> getMatchedOperationMappers() {
		return matchedOperationMappers;
	}

	public List<UMLOperationDiff> getOperationDiffs() {
		return operationDiffs;
	}

	public List<UMLAttributeDiff> getAttributeDiffs() {
		return attributeDiffs;
	}
}
