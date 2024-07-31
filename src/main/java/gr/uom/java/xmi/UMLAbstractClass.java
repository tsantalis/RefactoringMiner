package gr.uom.java.xmi;

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
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.RenamePattern;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;

public abstract class UMLAbstractClass {
	protected LocationInfo locationInfo;
	protected String packageName;
	protected String name;
	protected List<UMLOperation> operations;
	protected List<UMLAttribute> attributes;
	protected List<UMLComment> comments;
	private List<UMLAnonymousClass> anonymousClassList;
	private Map<List<String>, Integer> operationIdentifierSignatureMap;
	private Map<String, VariableDeclaration> fieldDeclarationMap;
	private List<UMLInitializer> initializers;
	private UMLType superclass;
    private List<UMLType> implementedInterfaces;
    private List<UMLImport> importedTypes;
    private List<UMLAnnotation> annotations;
    private List<UMLModifier> modifiers;
    private List<UMLEnumConstant> enumConstants;

	public UMLAbstractClass(String packageName, String name, LocationInfo locationInfo, List<UMLImport> importedTypes) {
		this.packageName = packageName;
		this.name = name;
		this.locationInfo = locationInfo;
		this.importedTypes = importedTypes;
        this.operations = new ArrayList<UMLOperation>();
        this.attributes = new ArrayList<UMLAttribute>();
        this.comments = new ArrayList<UMLComment>();
        this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
        this.initializers = new ArrayList<UMLInitializer>();
        this.operationIdentifierSignatureMap = new LinkedHashMap<>();
        this.superclass = null;
        this.implementedInterfaces = new ArrayList<UMLType>();
        this.annotations = new ArrayList<UMLAnnotation>();
        this.modifiers = new ArrayList<UMLModifier>();
        this.enumConstants = new ArrayList<UMLEnumConstant>();
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
		List<String> signature = operation.getSignatureIdentifiers();
		if(operationIdentifierSignatureMap.containsKey(signature)) {
			operationIdentifierSignatureMap.put(signature, operationIdentifierSignatureMap.get(signature) + 1);
		}
		else {
			operationIdentifierSignatureMap.put(signature, 1);
		}
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

	public void addInitializer(UMLInitializer initializer) {
		this.initializers.add(initializer);
	}

	public List<UMLInitializer> getInitializers() {
		return initializers;
	}

    public UMLType getSuperclass() {
		return superclass;
	}

	public void setSuperclass(UMLType superclass) {
		this.superclass = superclass;
	}

	public void addImplementedInterface(UMLType implementedInterface) {
		this.implementedInterfaces.add(implementedInterface);
	}

	public List<UMLType> getImplementedInterfaces() {
		return implementedInterfaces;
	}

	public List<UMLImport> getImportedTypes() {
		return importedTypes;
	}

    public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

    public void addAnnotation(UMLAnnotation annotation) {
    	annotations.add(annotation);
    }

    public List<UMLModifier> getModifiers() {
		return modifiers;
	}

    public void addModifier(UMLModifier modifier) {
    	modifiers.add(modifier);
    }

	public boolean hasDeprecatedAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			if(annotation.getTypeName().equals("Deprecated")) {
				return true;
			}
		}
		return false;
	}

	public void addEnumConstant(UMLEnumConstant enumConstant) {
    	enumConstants.add(enumConstant);
    }

    public List<UMLEnumConstant> getEnumConstants() {
		return enumConstants;
	}

	public UMLEnumConstant containsEnumConstant(UMLEnumConstant otherEnumConstant) {
    	ListIterator<UMLEnumConstant> enumConstantIt = enumConstants.listIterator();
    	while(enumConstantIt.hasNext()) {
    		UMLEnumConstant enumConstant = enumConstantIt.next();
    		if(enumConstant.equals(otherEnumConstant)) {
    			return enumConstant;
    		}
    	}
    	return null;
    }

	public boolean implementsInterface(Set<UMLType> interfaces) {
		for(UMLType type : interfaces) {
			if(implementedInterfaces.contains(type))
				return true;
		}
		return false;
	}

	public boolean extendsSuperclass(Set<UMLType> types) {
		for(UMLType type : types) {
			if(superclass != null && superclass.equals(type))
				return true;
		}
		return false;
	}

	public boolean isSubTypeOf(UMLClass umlClass) {
		if(superclass != null) {
			if(umlClass.getName().endsWith("." + superclass.getClassType())) {
				return true;
			}
		}
		for(UMLType implementedInterface : implementedInterfaces) {
			if(umlClass.getName().endsWith("." + implementedInterface.getClassType())) {
				return true;
			}
		}
		return false;
	}

	public boolean importsType(String targetClass) {
		if(targetClass.startsWith(getPackageName()))
			return true;
		for(UMLImport imported : getImportedTypes()) {
			//importedType.startsWith(targetClass) -> special handling for import static
			//importedType.equals(targetClassPackage) -> special handling for import with asterisk (*) wildcard
			String importedType = imported.getName();
			if(importedType.equals(targetClass) || importedType.startsWith(targetClass)) {
				return true;
			}
			if(targetClass.contains(".")) {
				String targetClassPackage = targetClass.substring(0, targetClass.lastIndexOf("."));
				if(importedType.equals(targetClassPackage)) {
					return true;
				}
			}
		}
		return false;
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
			boolean matchesOperation = isInterface() || originalOperation.isNative() ?
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
				if(!originalOperation.isConstructor() && !originalOperation.equalSignature(operation)) {
					List<String> signature = originalOperation.getSignatureIdentifiers();
					Integer instances = operationIdentifierSignatureMap.get(signature);
					if(instances != null && instances > 1) {
						return false;
					}
				}
				boolean originalOperationEmptyBody = originalOperation.getBody() == null || originalOperation.hasEmptyBody();
				boolean operationEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
				if(originalOperationEmptyBody == operationEmptyBody)
					return true;
			}
		}
		return false;
	}

	public boolean containsOperationWithIdenticalComments(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.identicalComments(operation))
				return true;
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

	public UMLOperation operationWithTheSameName(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.getName().equals(operation.getName()))
				return originalOperation;
		}
		return null;
	}

	public boolean containsOperationWithName(String name) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.getName().equals(name))
				return true;
		}
		return false;
	}

	public boolean containsOperationWithTheSameRenamePattern(UMLOperation operation, RenamePattern pattern) {
		if(pattern == null)
			return false;
		for(UMLOperation originalOperation : operations) {
			String originalOperationName = originalOperation.getName();
			if(originalOperationName.contains(pattern.getBefore()) && !pattern.getBefore().isEmpty()) {
				String originalOperationNameAfterReplacement = originalOperationName.replace(pattern.getBefore(), pattern.getAfter());
				if(originalOperationNameAfterReplacement.equals(operation.getName()))
					return true;
			}
			else if(operation.getName().contains(pattern.getAfter()) && pattern.getBefore().isEmpty()) {
				String operationNameAfterReplacement = operation.getName().replace(pattern.getAfter(), "");
				if(operationNameAfterReplacement.equals(originalOperation.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public UMLOperation operationWithTheSameRenamePattern(UMLOperation operation, RenamePattern pattern) {
		if(pattern == null)
			return null;
		for(UMLOperation originalOperation : operations) {
			String originalOperationName = originalOperation.getName();
			if(originalOperationName.contains(pattern.getBefore()) && !pattern.getBefore().isEmpty()) {
				String originalOperationNameAfterReplacement = originalOperationName.replace(pattern.getBefore(), pattern.getAfter());
				if(originalOperationNameAfterReplacement.equals(operation.getName()))
					return originalOperation;
			}
			else if(operation.getName().contains(pattern.getAfter()) && pattern.getBefore().isEmpty()) {
				String operationNameAfterReplacement = operation.getName().replace(pattern.getAfter(), "");
				if(operationNameAfterReplacement.equals(originalOperation.getName())) {
					return originalOperation;
				}
			}
		}
		return null;
	}

	public boolean containsOperationWithIdenticalBody(UMLOperation operation) {
		if(operation.getBody() != null) {
			for(UMLOperation originalOperation : operations) {
				if(originalOperation.getBody() != null && originalOperation.getBodyHashCode() == operation.getBodyHashCode())
					return true;
			}
		}
		return false;
	}

	private boolean containsSingleStatementWithRenamedCall(UMLAbstractClass umlClass, UMLOperation operation2) {
		if(operation2.stringRepresentation().size() == 3) {
			for(UMLOperation operation1 : operations) {
				if(operation1.stringRepresentation().size() == 3) {
					List<AbstractCodeFragment> leaves2 = operation2.getBody().getCompositeStatement().getLeaves();
					List<AbstractCodeFragment> leaves1 = operation1.getBody().getCompositeStatement().getLeaves();
					if(leaves2.size() == 1 && leaves1.size() == 1) {
						AbstractCodeFragment leaf2 = leaves2.get(0);
						AbstractCodeFragment leaf1 = leaves1.get(0);
						AbstractCall invocation1 = leaf1.invocationCoveringEntireFragment();
						AbstractCall invocation2 = leaf2.invocationCoveringEntireFragment();
						if(invocation1 != null && invocation2 != null &&
								invocation1.equalArguments(invocation2) &&
								invocation1.identicalExpression(invocation2) &&
								this.containsOperationWithName(invocation1.getName()) &&
								umlClass.containsOperationWithName(invocation2.getName())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public UMLAttribute attributeWithTheSameNameIgnoringChangedType(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.equalsIgnoringChangedType(attribute))
				return originalAttribute;
		}
		for(UMLAttribute originalAttribute : enumConstants) {
			if(originalAttribute.equalsIgnoringChangedType(attribute))
				return originalAttribute;
		}
		return null;
	}

	public UMLAttribute attributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.equalsIgnoringChangedVisibility(attribute))
				return originalAttribute;
		}
		for(UMLAttribute originalAttribute : enumConstants) {
			if(originalAttribute.equalsIgnoringChangedVisibility(attribute))
				return originalAttribute;
		}
		return null;
	}

	public boolean containsIdenticalAttributeIncludingAnnotation(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.identicalIncludingAnnotation(attribute))
				return true;
		}
		for(UMLAttribute originalAttribute : enumConstants) {
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
		for(UMLAttribute originalAttribute : enumConstants) {
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
		for(UMLAttribute originalAttribute : enumConstants) {
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
		for(UMLAttribute originalAttribute : enumConstants) {
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
			if(originalAttributeName.contains(pattern.getBefore()) && !pattern.getBefore().isEmpty()) {
				String originalAttributeNameAfterReplacement = originalAttributeName.replace(pattern.getBefore(), pattern.getAfter());
				if(originalAttributeNameAfterReplacement.equals(attribute.getName()))
					return true;
			}
			else if(attribute.getName().contains(pattern.getAfter()) && pattern.getBefore().isEmpty()) {
				String attributeNameAfterReplacement = attribute.getName().replace(pattern.getAfter(), "");
				if(attributeNameAfterReplacement.equals(originalAttribute.getName())) {
					return true;
				}
			}
		}
		for(UMLAttribute originalAttribute : enumConstants) {
			String originalAttributeName = originalAttribute.getName();
			if(originalAttributeName.contains(pattern.getBefore()) && !pattern.getBefore().isEmpty()) {
				String originalAttributeNameAfterReplacement = originalAttributeName.replace(pattern.getBefore(), pattern.getAfter());
				if(originalAttributeNameAfterReplacement.equals(attribute.getName()))
					return true;
			}
			else if(attribute.getName().contains(pattern.getAfter()) && pattern.getBefore().isEmpty()) {
				String attributeNameAfterReplacement = attribute.getName().replace(pattern.getAfter(), "");
				if(attributeNameAfterReplacement.equals(originalAttribute.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsAttributeWithName(String attributeName) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.getName().equals(attributeName))
				return true;
		}
		for(UMLAttribute originalAttribute : enumConstants) {
			if(originalAttribute.getName().equals(attributeName))
				return true;
		}
		return false;
	}

	public MatchResult hasAttributesAndOperationsWithCommonNames(UMLAbstractClass umlClass) {
		List<UMLOperation> commonOperations = new ArrayList<UMLOperation>();
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
		List<UMLAttribute> commonAttributes = new ArrayList<UMLAttribute>();
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
		for(UMLAttribute attribute : enumConstants) {
			totalAttributes++;
			if(umlClass.containsAttributeWithTheSameName(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		for(UMLAttribute attribute : umlClass.enumConstants) {
			totalAttributes++;
			if(this.containsAttributeWithTheSameName(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		if(this.isTestClass() && umlClass.isTestClass()) {
			if(commonOperations.size() > Math.floor(totalOperations/2.0) || commonOperations.containsAll(this.operations)) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
			}
		}
		if(this.isSingleAbstractMethodInterface() && umlClass.isSingleAbstractMethodInterface()) {
			if(commonOperations.size() == totalOperations) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
			}
		}
		if((commonOperations.size() >= Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0)) ||
				(commonAttributes.size() >= Math.floor(totalAttributes/2.0) && (commonOperations.size() > 2 || totalOperations == 0)) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size()) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && totalAttributes == 1)) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
		}
		else {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
		}
	}

	public MatchResult hasCommonAttributesAndOperations(UMLAbstractClass umlClass) {
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(this.name, umlClass.name);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(this.name, umlClass.name);
		String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(this.name);
		String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(umlClass.name);
		RenamePattern pattern = null;
		if(!commonPrefix.isEmpty() || !commonSuffix.isEmpty()) {
			int beginIndexS1 = this.name.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = this.name.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	this.name.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = umlClass.name.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = umlClass.name.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	umlClass.name.substring(beginIndexS2, endIndexS2);
			pattern = new RenamePattern(diff1, diff2);
		}
		List<UMLOperation> commonOperations = new ArrayList<UMLOperation>();
		List<UMLOperation> identicalOperations = new ArrayList<UMLOperation>();
		int totalOperations = 0;
		int totalDefaultOperations = 0;
		int totalAbstractOperations = 0;
		for(UMLOperation operation : operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
				if(operation.isAbstract()) {
					totalAbstractOperations++;
				}
				if(operation.isDefault()) {
					totalDefaultOperations++;
				}
				if(umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
						(umlClass.containsOperationWithIdenticalComments(operation) && !operation.hasTestAnnotation()) ||
						(pattern != null && umlClass.containsOperationWithTheSameRenamePattern(operation, pattern.reverse()))) {
					commonOperations.add(operation);
				}
				if(umlClass.containsOperationWithIdenticalBody(operation) || umlClass.containsSingleStatementWithRenamedCall(this, operation)) {
					identicalOperations.add(operation);
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
			else if(operation.isConstructor() && !commonPrefix.isEmpty() && !commonSuffix.isEmpty() &&
					containsToken(commonPrefix, tokens1) && containsToken(commonPrefix, tokens2) &&
					containsToken(commonSuffix, tokens1) && containsToken(commonSuffix, tokens2)) {
				if(pattern != null && umlClass.containsOperationWithTheSameRenamePattern(operation, pattern.reverse())) {
					commonOperations.add(operation);
				}
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
				if(operation.isAbstract()) {
					totalAbstractOperations++;
				}
				if(operation.isDefault()) {
					totalDefaultOperations++;
				}
				if(this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation) ||
						(this.containsOperationWithIdenticalComments(operation) && !operation.hasTestAnnotation()) ||
						(pattern != null && this.containsOperationWithTheSameRenamePattern(operation, pattern))) {
					commonOperations.add(operation);
				}
				if(this.containsOperationWithIdenticalBody(operation) || this.containsSingleStatementWithRenamedCall(umlClass, operation)) {
					identicalOperations.add(operation);
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
			else if(operation.isConstructor() && !commonPrefix.isEmpty() && !commonSuffix.isEmpty() &&
					containsToken(commonPrefix, tokens1) && containsToken(commonPrefix, tokens2) &&
					containsToken(commonSuffix, tokens1) && containsToken(commonSuffix, tokens2)) {
				if(pattern != null && this.containsOperationWithTheSameRenamePattern(operation, pattern)) {
					commonOperations.add(operation);
				}
			}
		}
		List<UMLAttribute> commonAttributes = new ArrayList<UMLAttribute>();
		List<UMLAttribute> identicalAttributes = new ArrayList<UMLAttribute>();
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
		for(UMLAttribute attribute : enumConstants) {
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
		for(UMLAttribute attribute : umlClass.enumConstants) {
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
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
			}
		}
		if(this.isSingleAbstractMethodInterface() && umlClass.isSingleAbstractMethodInterface()) {
			if(commonOperations.size() == totalOperations) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
			}
		}
		if(this.isSingleMethodClass() && umlClass.isSingleMethodClass() && this.getNonQualifiedName().equals(umlClass.getNonQualifiedName())) {
			if(commonOperations.size() >= totalOperations) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
		}
		if(this.isSingleAbstractMethodInterface() || umlClass.isSingleAbstractMethodInterface()) {
			if(commonOperations.size() == 2) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
			else {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
			}
		}
		if(this.isInterface() && umlClass.isInterface()) {
			if(commonOperations.size() > Math.floor(totalOperations/2.0)) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
			else if(commonOperations.size() >= Math.floor((totalOperations-totalDefaultOperations)/2.0)) {
				return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
			}
		}
		int abstractOperationsToBeDeducted = this.isAbstract() != umlClass.isAbstract() ? totalAbstractOperations : 0;
		if((commonOperations.size() > Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0)) ||
				(commonOperations.size() > Math.floor(totalOperations/3.0*2.0) && (commonAttributes.size() >= 2 || totalAttributes == 0)) ||
				(identicalOperations.size() > Math.floor(commonOperations.size()/3.0*2.0) && commonOperations.size() > 2 && identicalOperations.size() >= Math.floor((totalOperations - abstractOperationsToBeDeducted)/3.0*2.0)) ||
				(commonAttributes.size() > Math.floor(totalAttributes/2.0) && (commonOperations.size() > 2 || totalOperations == 0)) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size()) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && totalAttributes == 1) ||
				(identicalAttributes.size() == totalAttributes && totalAttributes > 0) ||
				(commonOperations.size() >= 10 && commonAttributes.size() >= 10 && identicalOperations.size() > 0 && identicalAttributes.size() > 0)) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
		}
		Set<UMLOperation> unmatchedOperations = new LinkedHashSet<UMLOperation>(umlClass.operations);
		unmatchedOperations.removeAll(commonOperations);
		Set<UMLOperation> unmatchedCalledOperations = new LinkedHashSet<UMLOperation>();
		for(UMLOperation operation : umlClass.operations) {
			if(commonOperations.contains(operation)) {
				for(AbstractCall invocation : operation.getAllOperationInvocations()) {
					for(UMLOperation unmatchedOperation : unmatchedOperations) {
						if(invocation.matchesOperation(unmatchedOperation, operation, null, null)) {
							unmatchedCalledOperations.add(unmatchedOperation);
							break;
						}
					}
				}
			}
		}
		if((commonOperations.size() + unmatchedCalledOperations.size() > Math.floor(totalOperations/2.0) && (commonAttributes.size() > 2 || totalAttributes == 0))) {
			return new MatchResult(commonOperations.size() + unmatchedCalledOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
		}
		if(unmatchedOperations.size() == 0 && commonOperations.size() > Math.floor(totalOperations/3.0*2.0)) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
		}
		return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
	}

	private static boolean containsToken(String input, String[] tokens) {
		for(String token : tokens) {
			if(input.contains(token)) {
				return true;
			}
		}
		return false;
	}

	public MatchResult hasSameAttributesAndOperations(UMLAbstractClass umlClass) {
		List<UMLOperation> commonOperations = new ArrayList<UMLOperation>();
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
		List<UMLAttribute> commonAttributes = new ArrayList<UMLAttribute>();
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
		for(UMLAttribute attribute : enumConstants) {
			totalAttributes++;
			if(umlClass.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		for(UMLAttribute attribute : umlClass.enumConstants) {
			totalAttributes++;
			if(this.containsAttributeWithTheSameNameIgnoringChangedType(attribute)) {
				commonAttributes.add(attribute);
			}
		}
		UMLAnnotationListDiff annotationListDiff = new UMLAnnotationListDiff(this.getAnnotations(), umlClass.getAnnotations());
		boolean identicalAnnotations = this.getAnnotations().size() > 0 && umlClass.getAnnotations().size() > 0 && annotationListDiff.isEmpty();
		if(totalOperations == 0 && totalAttributes == 0 && (this.getEnumConstants().size() == 0 || umlClass.getEnumConstants().size() == 0) && !this.getNonQualifiedName().equals(umlClass.getNonQualifiedName()) && !identicalAnnotations && !identicalComments(umlClass)) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
		}
		if(commonOperations.size() == totalOperations && commonAttributes.size() == totalAttributes) {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, true);
		}
		else {
			return new MatchResult(commonOperations.size(), commonAttributes.size(), totalOperations, totalAttributes, false);
		}
	}

	private boolean identicalComments(UMLAbstractClass umlClass) {
		List<UMLComment> comments1 = this.getComments();
		List<UMLComment> comments2 = umlClass.getComments();
		if(comments1.size() == comments2.size() && comments1.size() > 0) {
			int identicalComments = 0;
			for(int i=0; i<comments1.size(); i++) {
				if(comments1.get(i).getText().equals(comments2.get(i).getText())) {
					identicalComments++;
				}
			}
			return identicalComments == comments1.size();
		}
		return false;
	}

	public MatchResult hasCommonOperationWithTheSameSignature(UMLAbstractClass umlClass) {
		List<UMLOperation> commonOperations = new ArrayList<UMLOperation>();
		int totalOperations = 0;
		for(UMLOperation operation : operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
				if(umlClass.containsOperationWithTheSameSignature(operation)) {
					commonOperations.add(operation);
				}
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
				if(this.containsOperationWithTheSameSignature(operation)) {
					commonOperations.add(operation);
				}
			}
		}
		if(commonOperations.size() > 2) {
			return new MatchResult(commonOperations.size(), 0, totalOperations, 0, true);
		}
		else {
			return new MatchResult(commonOperations.size(), 0, totalOperations, 0, false);
		}
	}

	public boolean isTestClass() {
		for(UMLOperation operation : operations) {
			if(operation.hasTestAnnotation()) {
				return true;
			}
		}
		if(superclass != null && superclass.getClassType().equals("TestCase")) {
			return true;
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

    public UMLAttribute containsAttributeWithTheSameSignature(UMLAttribute otherAttribute) {
    	ListIterator<UMLAttribute> attributeIt = attributes.listIterator();
    	while(attributeIt.hasNext()) {
    		UMLAttribute attribute = attributeIt.next();
    		if(attribute.equalsIgnoringChangedVisibility(otherAttribute)) {
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
	
	public abstract boolean isAbstract();
	
	public abstract String getTypeDeclarationKind();
	public abstract boolean isFinal();
	public abstract boolean isStatic();
	public abstract boolean isTopLevel();
	public abstract Visibility getVisibility();

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

	public boolean uniqueInitializer(UMLAttribute attributeToCheck) {
		AbstractExpression initializer = attributeToCheck.getVariableDeclaration().getInitializer();
		if(initializer == null) {
			return false;
		}
		int matches = 0;
		for(UMLAttribute attribute : attributes) {
			if(!attribute.equals(attributeToCheck)) {
				if(attribute.getVariableDeclaration().getInitializer() != null &&
						attribute.getVariableDeclaration().getInitializer().getString().equals(initializer.getString())) {
					matches++;
				}
			}
		}
		if(matches == 0) {
			return true;
		}
		return false;
	}

	public boolean uniqueJavadoc(UMLAttribute attributeToCheck) {
		UMLJavadoc javadoc = attributeToCheck.getJavadoc();
		if(javadoc == null) {
			return false;
		}
		int matches = 0;
		for(UMLAttribute attribute : attributes) {
			if(!attribute.equals(attributeToCheck)) {
				if(attribute.getJavadoc() != null &&
						attribute.getJavadoc().equalText(javadoc)) {
					matches++;
				}
			}
		}
		if(matches == 0) {
			return true;
		}
		return false;
	}

	public boolean uniqueAnnotation(UMLOperation operationToCheck) {
		List<UMLAnnotation> annotations = operationToCheck.getAnnotations();
		if(annotations.isEmpty()) {
			return false;
		}
		int matches = 0;
		for(UMLOperation operation : operations) {
			if(!operation.equals(operationToCheck)) {
				if(!operation.getAnnotations().isEmpty() &&
						operation.getAnnotations().equals(annotations)) {
					matches++;
				}
			}
		}
		if(matches == 0) {
			return true;
		}
		return false;
	}
}
