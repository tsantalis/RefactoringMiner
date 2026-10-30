package gr.uom.java.xmi.diff;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher.MatchResult;

public class UMLClassMoveDiff extends UMLClassBaseDiff {
	private MatchResult matchResult;
	public UMLClassMoveDiff(UMLClass originalClass, UMLClass movedClass, UMLModelDiff modelDiff, MatchResult matchResult) {
		super(originalClass, movedClass, modelDiff);
		this.matchResult = matchResult;
	}

	public UMLClass getMovedClass() {
		return (UMLClass) nextClass;
	}

	public MatchResult getMatchResult() {
		return matchResult;
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		//the pairs of the attributes matched from the original to the next class, whose diffs are not created again from the next to the original class
		Map<UMLAttribute, UMLAttribute> matchedAttributes = new IdentityHashMap<>();
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = nextClass.containsAttribute(attribute);
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ");
		sb.append(originalClass.getName());
		sb.append(" was moved to ");
		sb.append(nextClass.getName());
		sb.append("\n");
		return sb.toString();
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
		
		if(o instanceof UMLClassMoveDiff) {
			UMLClassMoveDiff classMoveDiff = (UMLClassMoveDiff)o;
			return this.originalClass.equals(classMoveDiff.originalClass) && this.nextClass.equals(classMoveDiff.nextClass);
		}
		return false;
	}

	public RenamePattern extractRenamePattern() {
		String name1 = getOriginalClassName();
		String name2 = getNextClassName();
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(name1, name2);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(name1, name2);
		RenamePattern pattern = null;
		if(!commonPrefix.isEmpty() || !commonSuffix.isEmpty()) {
			int beginIndexS1 = name1.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = name1.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	name1.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = name2.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = name2.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	name2.substring(beginIndexS2, endIndexS2);
			pattern = new RenamePattern(diff1, diff2);
		}
		return pattern;
	}
}
