package gr.uom.java.xmi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLClassMatcher.MatchResult;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.RenamePattern;
import gr.uom.java.xmi.diff.StringDistance;

public abstract class UMLAbstractClass {
	protected LocationInfo locationInfo;
	protected String packageName;
	protected String name;
	protected List<UMLOperation> operations;
	protected List<UMLAttribute> attributes;
	protected List<UMLComment> comments;
	private List<UMLAnonymousClass> anonymousClassList;
	private Map<String, VariableDeclaration> fieldDeclarationMap;

	public UMLAbstractClass() {
        this.operations = new ArrayList<UMLOperation>();
        this.attributes = new ArrayList<UMLAttribute>();
        this.comments = new ArrayList<UMLComment>();
        this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
	}

	public List<UMLOperation> getOperationsWithOverrideAnnotation() {
		List<UMLOperation> operations = new ArrayList<>();
		for(UMLOperation operation : this.operations) {
			if(operation.hasOverrideAnnotation() && !operation.isConstructor() && !operation.overridesObject()) {
				operations.add(operation);
			}
		}
		return operations;
	}
	
	public List<UMLOperation> getOperationsWithoutOverrideAnnotation() {
		List<UMLOperation> operations = new ArrayList<>();
		for(UMLOperation operation : this.operations) {
			if(!operation.hasOverrideAnnotation() && !operation.isConstructor() && !operation.overridesObject()) {
				operations.add(operation);
			}
		}
		return operations;
	}
	
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public String getPackageName() {
		return this.packageName;
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

	public List<UMLComment> getComments() {
		return comments;
	}

	public Map<String, VariableDeclaration> getFieldDeclarationMap() {
		if(this.fieldDeclarationMap == null) {
			fieldDeclarationMap = new LinkedHashMap<String, VariableDeclaration>();
			for(UMLAttribute attribute : attributes) {
				fieldDeclarationMap.put(attribute.getName(), attribute.getVariableDeclaration());
			}
		}
		return fieldDeclarationMap;
	}

	//returns true if the "innerClass" parameter is inner class of this
	public boolean isInnerClass(UMLAbstractClass innerClass) {
		if(this.getName().equals(innerClass.packageName))
			return true;
		return false;
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
			boolean matchesOperation = isInterface() ?
				originalOperation.equalSignatureIgnoringChangedTypes(operation) :
				originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation);
			if(matchesOperation) {
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

	public boolean containsIdenticalAttributeIncludingAnnotation(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.identicalIncludingAnnotation(attribute))
				return true;
		}
		return false;
	}

	public boolean containsAttributeWithTheSameNameIgnoringChangedType(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.equalsIgnoringChangedType(attribute))
				return true;
		}
		return false;
	}

	public boolean containsRenamedAttributeWithIdenticalTypeAndInitializer(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.renamedWithIdenticalTypeAndInitializer(attribute))
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

	public boolean identicalPackageHeader(UMLAbstractClass umlClass) {
		if(this instanceof UMLClass && umlClass instanceof UMLClass) {
			try {
				return ((UMLClass)this).identicalPackageHeader((UMLClass)umlClass);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public MatchResult hasAttributesAndOperationsWithCommonNames(UMLAbstractClass umlClass) {
		boolean identicalPackageHeader = identicalPackageHeader(umlClass);
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
			if(commonOperations.size() > Math.floor(totalOperations/2.0) || commonOperations.containsAll(this.operations)) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
			}
		}
		if(this.isSingleAbstractMethodInterface() && umlClass.isSingleAbstractMethodInterface()) {
			if(commonOperations.size() == totalOperations) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
			}
		}
		if((commonOperations.size() >= Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0)) ||
				(commonAttributes.size() >= Math.floor(totalAttributes/2.0) && (commonOperations.size() > 2 || totalOperations == 0)) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size()) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && totalAttributes == 1)) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
		}
		else {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
		}
	}

	public MatchResult hasCommonAttributesAndOperations(UMLAbstractClass umlClass) {
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
		boolean identicalPackageHeader = identicalPackageHeader(umlClass);
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
			else if(operation.isConstructor() && commonPrefix.equals(commonSuffix) && commonPrefix.length() > 0) {
				if(umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
						(pattern != null && umlClass.containsOperationWithTheSameRenamePattern(operation, pattern.reverse()))) {
					commonOperations.add(operation);
				}
			}
			else if(operation.isConstructor() && commonPrefix.isEmpty() && commonSuffix.equals(this.name)) {
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
			else if(operation.isConstructor() && commonPrefix.equals(commonSuffix) && commonPrefix.length() > 0) {
				if(this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
						(pattern != null && this.containsOperationWithTheSameRenamePattern(operation, pattern))) {
					commonOperations.add(operation);
				}
			}
			else if(operation.isConstructor() && commonPrefix.isEmpty() && commonSuffix.equals(this.name)) {
				if(this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
						(pattern != null && this.containsOperationWithTheSameRenamePattern(operation, pattern))) {
					commonOperations.add(operation);
				}
			}
		}
		Set<UMLAttribute> commonAttributes = new LinkedHashSet<UMLAttribute>();
		Set<UMLAttribute> identicalAttributes = new LinkedHashSet<UMLAttribute>();
		int totalAttributes = 0;
		for(UMLAttribute attribute : attributes) {
			totalAttributes++;
			if(umlClass.containsAttributeWithTheSameNameIgnoringChangedType(attribute) ||
					umlClass.containsRenamedAttributeWithIdenticalTypeAndInitializer(attribute) ||
					(pattern != null && umlClass.containsAttributeWithTheSameRenamePattern(attribute, pattern.reverse()))) {
				commonAttributes.add(attribute);
				if(umlClass.containsIdenticalAttributeIncludingAnnotation(attribute)) {
					identicalAttributes.add(attribute);
				}
			}
		}
		for(UMLAttribute attribute : umlClass.attributes) {
			totalAttributes++;
			if(this.containsAttributeWithTheSameNameIgnoringChangedType(attribute) ||
					this.containsRenamedAttributeWithIdenticalTypeAndInitializer(attribute) ||
					(pattern != null && this.containsAttributeWithTheSameRenamePattern(attribute, pattern))) {
				commonAttributes.add(attribute);
				if(this.containsIdenticalAttributeIncludingAnnotation(attribute)) {
					identicalAttributes.add(attribute);
				}
			}
		}
		if(this.isTestClass() && umlClass.isTestClass()) {
			if(commonOperations.size() > Math.floor(totalOperations/2.0) || commonOperations.containsAll(this.operations)) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
			}
		}
		if(this.isSingleAbstractMethodInterface() && umlClass.isSingleAbstractMethodInterface()) {
			if(commonOperations.size() == totalOperations) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
			}
		}
		if(this.isSingleMethodClass() && umlClass.isSingleMethodClass() && this.getNonQualifiedName().equals(umlClass.getNonQualifiedName())) {
			if(commonOperations.size() >= totalOperations) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
		}
		if(this.isSingleAbstractMethodInterface() || umlClass.isSingleAbstractMethodInterface()) {
			if(commonOperations.size() == 2) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
			}
		}
		if(this.isInterface() && umlClass.isInterface()) {
			if(commonOperations.size() > Math.floor(totalOperations/2.0)) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
			}
		}
		if((commonOperations.size() > Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0)) ||
				(commonOperations.size() > Math.floor(totalOperations/3.0*2.0) && (commonAttributes.size() >= 2 || totalAttributes == 0)) ||
				(commonAttributes.size() > Math.floor(totalAttributes/2.0) && (commonOperations.size() > 2 || totalOperations == 0)) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size()) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && totalAttributes == 1)) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
		}
		if(identicalAttributes.size() == totalAttributes && totalAttributes > 0) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
		}
		Set<UMLOperation> unmatchedOperations = new LinkedHashSet<UMLOperation>(umlClass.operations);
		unmatchedOperations.removeAll(commonOperations);
		Set<UMLOperation> unmatchedCalledOperations = new LinkedHashSet<UMLOperation>();
		for(UMLOperation operation : umlClass.operations) {
			if(commonOperations.contains(operation)) {
				for(AbstractCall invocation : operation.getAllOperationInvocations()) {
					for(UMLOperation unmatchedOperation : unmatchedOperations) {
						if(invocation.matchesOperation(unmatchedOperation, operation, null)) {
							unmatchedCalledOperations.add(unmatchedOperation);
							break;
						}
					}
				}
			}
		}
		if((commonOperations.size() + unmatchedCalledOperations.size() > Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0))) {
			return new MatchResult(commonOperations.size() + unmatchedCalledOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
		}
		return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
	}

	public MatchResult hasSameAttributesAndOperations(UMLAbstractClass umlClass) {
		boolean identicalPackageHeader = identicalPackageHeader(umlClass);
		Set<UMLOperation> commonOperations = new LinkedHashSet<UMLOperation>();
		int totalOperations = 0;
		for(UMLOperation operation : operations) {
			totalOperations++;
			if(umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
				commonOperations.add(operation);
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			totalOperations++;
			if(this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
				commonOperations.add(operation);
			}
		}
		Set<UMLAttribute> commonAttributes = new LinkedHashSet<UMLAttribute>();
		int totalAttributes = 0;
		for(UMLAttribute attribute : attributes) {
			totalAttributes++;
			if(umlClass.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		for(UMLAttribute attribute : umlClass.attributes) {
			totalAttributes++;
			if(this.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		if(commonOperations.size() == totalOperations && commonAttributes.size() == totalAttributes) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, true);
		}
		else {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, identicalPackageHeader, false);
		}
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

    public UMLAttribute containsAttribute(UMLAttribute otherAttribute) {
    	ListIterator<UMLAttribute> attributeIt = attributes.listIterator();
    	while(attributeIt.hasNext()) {
    		UMLAttribute attribute = attributeIt.next();
    		if(attribute.equals(otherAttribute)) {
    			return attribute;
    		}
    	}
    	return null;
    }

    public UMLAttribute matchAttribute(UMLAttribute otherAttribute) {
    	ListIterator<UMLAttribute> attributeIt = attributes.listIterator();
    	while(attributeIt.hasNext()) {
    		UMLAttribute attribute = attributeIt.next();
    		if(attribute.getName().equals(otherAttribute.getName())) {
    			String thisAttributeType = attribute.getType().getClassType();
				String otherAttributeType = otherAttribute.getType().getClassType();
				int thisArrayDimension = attribute.getType().getArrayDimension();
				int otherArrayDimension = otherAttribute.getType().getArrayDimension();
				String thisAttributeTypeComparedString = null;
    			if(thisAttributeType.contains("."))
    				thisAttributeTypeComparedString = thisAttributeType.substring(thisAttributeType.lastIndexOf(".")+1);
    			else
    				thisAttributeTypeComparedString = thisAttributeType;
    			String otherAttributeTypeComparedString = null;
    			if(otherAttributeType.contains("."))
    				otherAttributeTypeComparedString = otherAttributeType.substring(otherAttributeType.lastIndexOf(".")+1);
    			else
    				otherAttributeTypeComparedString = otherAttributeType;
    			if(thisAttributeTypeComparedString.equals(otherAttributeTypeComparedString) && thisArrayDimension == otherArrayDimension)
    				return attribute;
    		}
    	}
    	return null;
    }

	public abstract boolean isSingleAbstractMethodInterface();
	
	public abstract boolean isSingleMethodClass();

	public abstract boolean isInterface();
	
	public abstract String getName();

	public String getNonQualifiedName() {
		return name;
	}

	public String getSourceFile() {
		return locationInfo.getFilePath();
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public Map<String, Set<String>> aliasedAttributes() {
		for(UMLOperation operation : getOperations()) {
			if(operation.isConstructor()) {
				Map<String, Set<String>> aliased = operation.aliasedAttributes();
				if(!aliased.isEmpty()) {
					return aliased;
				}
			}
		}
		return new LinkedHashMap<String, Set<String>>();
	}

	public void addAnonymousClass(UMLAnonymousClass anonymousClass) {
		anonymousClassList.add(anonymousClass);
	}

	public List<UMLAnonymousClass> getAnonymousClassList() {
		return anonymousClassList;
	}

	public boolean containsAnonymousWithSameAttributesAndOperations(UMLAnonymousClass anonymous) {
		for(UMLAnonymousClass thisAnonymous : anonymousClassList) {
			MatchResult matchResult = thisAnonymous.hasSameAttributesAndOperations(anonymous);
			if(matchResult.isMatch())
				return true;
		}
		return false;
	}
}
