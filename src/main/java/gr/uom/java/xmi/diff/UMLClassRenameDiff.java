package gr.uom.java.xmi.diff;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher.MatchResult;

public class UMLClassRenameDiff extends UMLClassBaseDiff {
	private MatchResult matchResult;
	public UMLClassRenameDiff(UMLClass originalClass, UMLClass renamedClass, UMLModelDiff modelDiff, MatchResult matchResult) {
		super(originalClass, renamedClass, modelDiff);
		this.matchResult = matchResult;
	}

	public UMLClass getRenamedClass() {
		return (UMLClass) nextClass;
	}

	public MatchResult getMatchResult() {
		return matchResult;
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
    				addCommonAttributes(attribute, matchingAttribute);
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
    				addCommonAttributes(matchingAttribute, attribute);
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
    		}
    	}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ");
		sb.append(originalClass.getName());
		sb.append(" was renamed to ");
		sb.append(nextClass.getName());
		sb.append("\n");
		return sb.toString();
	}
}
