package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.RenamePattern;
import gr.uom.java.xmi.diff.StringDistance;

public abstract class UMLAbstractClass {
	protected LocationInfo locationInfo;
	protected String packageName;
	protected String name;
	protected List<UMLOperation> operations;
	protected List<UMLAttribute> attributes;

	public UMLAbstractClass() {
        this.operations = new ArrayList<UMLOperation>();
        this.attributes = new ArrayList<UMLAttribute>();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public void addOperation(UMLOperation operation) {
		this.operations.add(operation);
	}

	public void addAttribute(UMLAttribute attribute) {
		this.attributes.add(attribute);
	}

	public List<UMLOperation> getOperations() {
		return operations;
	}

	public List<UMLAttribute> getAttributes() {
		return attributes;
	}

	public UMLOperation operationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignature(operation))
				return originalOperation;
		}
		return null;
	}

	public boolean containsOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignature(operation))
				return true;
		}
		return false;
	}

	public UMLOperation operationWithTheSameSignatureIgnoringChangedTypes(UMLOperation operation) {
		List<UMLOperation> matchingOperations = new ArrayList<UMLOperation>();
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignatureIgnoringChangedTypes(operation)) {
				boolean originalOperationEmptyBody = originalOperation.getBody() == null || originalOperation.hasEmptyBody();
				boolean operationEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
				if(originalOperationEmptyBody == operationEmptyBody)
					matchingOperations.add(originalOperation);
			}
		}
		if(matchingOperations.size() == 1) {
			return matchingOperations.get(0);
		}
		else if(matchingOperations.size() > 1) {
			int minDistance = StringDistance.editDistance(matchingOperations.get(0).toString(), operation.toString());
			UMLOperation matchingOperation = matchingOperations.get(0);
			for(int i=1; i<matchingOperations.size(); i++) {
				int distance = StringDistance.editDistance(matchingOperations.get(i).toString(), operation.toString());
				if(distance < minDistance) {
					minDistance = distance;
					matchingOperation = matchingOperations.get(i);
				}
			}
			return matchingOperation;
		}
		return null;
	}

	public boolean containsOperationWithTheSameSignatureIgnoringChangedTypes(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignatureIgnoringChangedTypes(operation)) {
				boolean originalOperationEmptyBody = originalOperation.getBody() == null || originalOperation.hasEmptyBody();
				boolean operationEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
				if(originalOperationEmptyBody == operationEmptyBody)
					return true;
			}
		}
		return false;
	}

	public boolean containsOperationWithTheSameName(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.getName().equals(operation.getName()))
				return true;
		}
		return false;
	}

	public boolean containsOperationWithTheSameRenamePattern(UMLOperation operation, RenamePattern pattern) {
		if(pattern == null)
			return false;
		for(UMLOperation originalOperation : operations) {
			String originalOperationName = originalOperation.getName();
			if(originalOperationName.contains(pattern.getBefore())) {
				String originalOperationNameAfterReplacement = originalOperationName.replace(pattern.getBefore(), pattern.getAfter());
				if(originalOperationNameAfterReplacement.equals(operation.getName()))
					return true;
			}
		}
		return false;
	}

	public UMLAttribute attributeWithTheSameNameIgnoringChangedType(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.equalsIgnoringChangedType(attribute))
				return originalAttribute;
		}
		return null;
	}

	public boolean containsAttributeWithTheSameNameIgnoringChangedType(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.equalsIgnoringChangedType(attribute))
				return true;
		}
		return false;
	}

	public boolean containsAttributeWithTheSameName(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.getName().equals(attribute.getName()))
				return true;
		}
		return false;
	}

	public boolean containsAttributeWithTheSameRenamePattern(UMLAttribute attribute, RenamePattern pattern) {
		if(pattern == null)
			return false;
		for(UMLAttribute originalAttribute : attributes) {
			String originalAttributeName = originalAttribute.getName();
			if(originalAttributeName.contains(pattern.getBefore())) {
				String originalAttributeNameAfterReplacement = originalAttributeName.replace(pattern.getBefore(), pattern.getAfter());
				if(originalAttributeNameAfterReplacement.equals(attribute.getName()))
					return true;
			}
		}
		return false;
	}

	public boolean containsAttributeWithName(String attributeName) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.getName().equals(attributeName))
				return true;
		}
		return false;
	}

	public boolean hasAttributesAndOperationsWithCommonNames(UMLAbstractClass umlClass) {
		Set<UMLOperation> commonOperations = new LinkedHashSet<UMLOperation>();
		int totalOperations = 0;
		for(UMLOperation operation : operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
	    		if(umlClass.containsOperationWithTheSameName(operation)) {
	    			commonOperations.add(operation);
	    		}
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
	    		if(this.containsOperationWithTheSameName(operation)) {
	    			commonOperations.add(operation);
	    		}
			}
		}
		Set<UMLAttribute> commonAttributes = new LinkedHashSet<UMLAttribute>();
		int totalAttributes = 0;
		for(UMLAttribute attribute : attributes) {
			totalAttributes++;
			if(umlClass.containsAttributeWithTheSameName(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		for(UMLAttribute attribute : umlClass.attributes) {
			totalAttributes++;
			if(this.containsAttributeWithTheSameName(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		if(this.isTestClass() && umlClass.isTestClass()) {
			return commonOperations.size() > Math.floor(totalOperations/2.0) || commonOperations.containsAll(this.operations);
		}
		if(this.isSingleAbstractMethodInterface() && umlClass.isSingleAbstractMethodInterface()) {
			return commonOperations.size() == totalOperations;
		}
		return (commonOperations.size() >= Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0)) ||
				(commonAttributes.size() >= Math.floor(totalAttributes/2.0) && (commonOperations.size() > 2 || totalOperations == 0)) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size()) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && totalAttributes == 1);
	}

	public boolean hasCommonAttributesAndOperations(UMLAbstractClass umlClass) {
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(this.name, umlClass.name);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(this.name, umlClass.name);
		RenamePattern pattern = null;
		if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
			int beginIndexS1 = this.name.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = this.name.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	this.name.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = umlClass.name.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = umlClass.name.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	umlClass.name.substring(beginIndexS2, endIndexS2);
			pattern = new RenamePattern(diff1, diff2);
		}
		Set<UMLOperation> commonOperations = new LinkedHashSet<UMLOperation>();
		int totalOperations = 0;
		for(UMLOperation operation : operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
	    		if(umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
	    				(pattern != null && umlClass.containsOperationWithTheSameRenamePattern(operation, pattern.reverse()))) {
	    			commonOperations.add(operation);
	    		}
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
	    		if(this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
	    				(pattern != null && this.containsOperationWithTheSameRenamePattern(operation, pattern))) {
	    			commonOperations.add(operation);
	    		}
			}
		}
		Set<UMLAttribute> commonAttributes = new LinkedHashSet<UMLAttribute>();
		int totalAttributes = 0;
		for(UMLAttribute attribute : attributes) {
			totalAttributes++;
			if(umlClass.containsAttributeWithTheSameNameIgnoringChangedType(attribute) ||
    				(pattern != null && umlClass.containsAttributeWithTheSameRenamePattern(attribute, pattern.reverse()))) {
				commonAttributes.add(attribute);
			}
		}
		for(UMLAttribute attribute : umlClass.attributes) {
			totalAttributes++;
			if(this.containsAttributeWithTheSameNameIgnoringChangedType(attribute) ||
    				(pattern != null && this.containsAttributeWithTheSameRenamePattern(attribute, pattern))) {
				commonAttributes.add(attribute);
			}
		}
		if(this.isTestClass() && umlClass.isTestClass()) {
			return commonOperations.size() > Math.floor(totalOperations/2.0) || commonOperations.containsAll(this.operations);
		}
		if(this.isSingleAbstractMethodInterface() && umlClass.isSingleAbstractMethodInterface()) {
			return commonOperations.size() == totalOperations;
		}
		return (commonOperations.size() > Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0)) ||
				(commonOperations.size() > Math.floor(totalOperations/3.0*2.0) && (commonAttributes.size() >= 2 || totalAttributes == 0)) ||
				(commonAttributes.size() > Math.floor(totalAttributes/2.0) && (commonOperations.size() > 2 || totalOperations == 0)) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size()) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && totalAttributes == 1);
	}

	public boolean hasSameAttributesAndOperations(UMLAbstractClass umlClass) {
		if(this.attributes.size() != umlClass.attributes.size())
			return false;
		if(this.operations.size() != umlClass.operations.size())
			return false;
		for(UMLOperation operation : operations) {
			if(!umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
				return false;
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			if(!this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
				return false;
			}
		}
		for(UMLAttribute attribute : attributes) {
			if(!umlClass.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
				return false;
			}
		}
		for(UMLAttribute attribute : umlClass.attributes) {
			if(!this.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
				return false;
			}
		}
		return true;
	}

	public boolean isTestClass() {
		for(UMLOperation operation : operations) {
			if(operation.hasTestAnnotation()) {
				return true;
			}
		}
		return false;
	}

	public List<UMLAttribute> attributesOfType(String targetClass) {
		List<UMLAttribute> attributesOfType = new ArrayList<UMLAttribute>();
		for(UMLAttribute attribute : attributes) {
			if(targetClass.endsWith("." + attribute.getType().getClassType()) ||
					targetClass.equals(attribute.getType().getClassType())) {
				attributesOfType.add(attribute);
			}
		}
		return attributesOfType;
	}

	public abstract boolean isSingleAbstractMethodInterface();

	public String getSourceFile() {
		return locationInfo.getFilePath();
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
