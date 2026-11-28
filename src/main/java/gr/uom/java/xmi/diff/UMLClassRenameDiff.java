package gr.uom.java.xmi.diff;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAbstractClass;
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
		Set<UMLAttribute> pairableAttributes1 = pairableUnionAttributes(originalClass, nextClass);
		Set<UMLAttribute> pairableAttributes2 = pairableUnionAttributes(nextClass, originalClass);
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = pairableAttributes1 != null && !pairableAttributes1.contains(attribute) ? null : matchingAttribute(nextClass, attribute, pairableAttributes2);
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
    		UMLAttribute matchingAttribute = pairableAttributes2 != null && !pairableAttributes2.contains(attribute) ? null : matchingAttribute(originalClass, attribute, pairableAttributes1);
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

	//the attribute of the class equal to the given attribute, or among the attributes that can be paired when the class is a union type alias, the one with the same name,
	//since the member of the union is the counterpart of the other type, i.e., type: 'oidc-attempt-connect-notify-device-auth' -> type: ActionTypes.OidcNotifyDeviceAuth
	private static UMLAttribute matchingAttribute(UMLAbstractClass umlClass, UMLAttribute otherAttribute, Set<UMLAttribute> pairableAttributes) {
		if(pairableAttributes == null) {
			return umlClass.containsAttribute(otherAttribute);
		}
		for(UMLAttribute attribute : pairableAttributes) {
			if(attribute.getName().equals(otherAttribute.getName())) {
				return attribute;
			}
		}
		return null;
	}

	//the attributes of a union type alias that can be paired with the attributes of a type without union members, since several members declare attributes with the same name:
	//the attributes of the member declaring the most attribute names of the other type, when no other member declares as many, i.e., the member
	//{ type: 'oidc-attempt-connect-notify-device-auth'; connectionInfo; verificationUrl; userCode } of Action for OidcNotifyDeviceAuthAction = { type; connectionId; verificationUrl; userCode },
	//or none, when other members declare as many, i.e., the members { type; connectionInfo } of 'attempt-connect', 'connection-attempt-succeeded', 'edit-connection' and others
	//for ConnectionAttemptStartAction = { type; connectionInfo } (compass 00209272)
	static Set<UMLAttribute> pairableUnionAttributes(UMLAbstractClass unionClass, UMLAbstractClass otherClass) {
		Map<Integer, Set<UMLAttribute>> members = new LinkedHashMap<>();
		for(UMLAttribute attribute : unionClass.getAttributes()) {
			if(attribute.getUnionMember() >= 0) {
				members.computeIfAbsent(attribute.getUnionMember(), k -> new LinkedHashSet<>()).add(attribute);
			}
		}
		if(members.isEmpty()) {
			return null;
		}
		Set<String> otherNames = new LinkedHashSet<>();
		for(UMLAttribute attribute : otherClass.getAttributes()) {
			if(attribute.getUnionMember() >= 0) {
				return null;
			}
			otherNames.add(attribute.getName());
		}
		Set<UMLAttribute> best = new LinkedHashSet<>();
		int max = 0;
		boolean unique = false;
		for(Set<UMLAttribute> member : members.values()) {
			int common = 0;
			for(UMLAttribute attribute : member) {
				if(otherNames.contains(attribute.getName())) {
					common++;
				}
			}
			if(common > max) {
				max = common;
				best = member;
				unique = true;
			}
			else if(common == max) {
				unique = false;
			}
		}
		return max > 0 && unique ? best : new LinkedHashSet<>();
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
