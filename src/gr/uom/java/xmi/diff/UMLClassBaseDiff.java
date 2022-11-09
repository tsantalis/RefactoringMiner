package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;

public abstract class UMLClassBaseDiff extends UMLAbstractClassDiff implements Comparable<UMLClassBaseDiff> {

	private static final int MAXIMUM_NUMBER_OF_COMPARED_METHODS = 30;
	public static final double MAX_OPERATION_NAME_DISTANCE = 0.4;
	private boolean visibilityChanged;
	private String oldVisibility;
	private String newVisibility;
	private boolean abstractionChanged;
	private boolean oldAbstraction;
	private boolean newAbstraction;
	private boolean staticChanged;
	private boolean finalChanged;
	private boolean superclassChanged;
	private UMLType oldSuperclass;
	private UMLType newSuperclass;
	private List<UMLType> addedImplementedInterfaces;
	private List<UMLType> removedImplementedInterfaces;
	private UMLAnnotationListDiff annotationListDiff;
	private UMLImportListDiff importDiffList;
	private UMLTypeParameterListDiff typeParameterDiffList;
	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> consistentMethodInvocationRenames;

	public UMLClassBaseDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
		super(originalClass, nextClass, modelDiff);
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.superclassChanged = false;
		this.addedImplementedInterfaces = new ArrayList<UMLType>();
		this.removedImplementedInterfaces = new ArrayList<UMLType>();
	}

	public UMLClass getOriginalClass() {
		return (UMLClass) originalClass;
	}

	public UMLClass getNextClass() {
		return (UMLClass) nextClass;
	}

	public void process() throws RefactoringMinerTimedOutException {
		processImports();
		processInitializers();
		processModifiers();
		processTypeParameters();
		processAnnotations();
		processEnumConstants();
		processInheritance();
		processOperations();
		createBodyMappers();
		processAnonymousClasses();
		checkForOperationSignatureChanges();
		processAttributes();
		checkForAttributeChanges();
		checkForInlinedOperations();
		checkForExtractedOperations();
	}

	private void processTypeParameters() {
		this.typeParameterDiffList = new UMLTypeParameterListDiff(getOriginalClass().getTypeParameters(), getNextClass().getTypeParameters());
	}

	private void processImports() {
		if(originalClass.isTopLevel() && nextClass.isTopLevel()) {
			this.importDiffList = new UMLImportListDiff(originalClass.getImportedTypes(), nextClass.getImportedTypes());
		}
	}

	public boolean hasBothAddedAndRemovedImports() {
		if(importDiffList != null) {
			return importDiffList.getAddedImports().size() > 0 && importDiffList.getRemovedImports().size() > 0;
		}
		return false;
	}

	public void findImportChanges(String nameBefore, String nameAfter) {
		if(importDiffList != null) {
			importDiffList.findImportChanges(nameBefore, nameAfter);
		}
	}

	public UMLImportListDiff getImportDiffList() {
		return importDiffList;
	}

	public UMLAnnotationListDiff getAnnotationListDiff() {
		return annotationListDiff;
	}

	public UMLTypeParameterListDiff getTypeParameterDiffList() {
		return typeParameterDiffList;
	}

	protected void processInitializers() throws RefactoringMinerTimedOutException {
		for(UMLInitializer initializer1 : originalClass.getInitializers()) {
			for(UMLInitializer initializer2 : nextClass.getInitializers()) {
				if(initializer1.isStatic() == initializer2.isStatic()) {
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(initializer1, initializer2, this);
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

	private void processModifiers() {
		if(!originalClass.getVisibility().equals(nextClass.getVisibility())) {
			setVisibilityChanged(true);
			setOldVisibility(originalClass.getVisibility());
			setNewVisibility(nextClass.getVisibility());
			ChangeClassAccessModifierRefactoring refactoring = new ChangeClassAccessModifierRefactoring(oldVisibility, newVisibility, originalClass, nextClass);
			refactorings.add(refactoring);
		}
		if(!originalClass.isInterface() && !nextClass.isInterface()) {
			if(originalClass.isAbstract() != nextClass.isAbstract()) {
				setAbstractionChanged(true);
				setOldAbstraction(originalClass.isAbstract());
				setNewAbstraction(nextClass.isAbstract());
				if(nextClass.isAbstract()) {
					AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("abstract", originalClass, nextClass);
					refactorings.add(refactoring);
				}
				else if(originalClass.isAbstract()) {
					RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("abstract", originalClass, nextClass);
					refactorings.add(refactoring);
				}
			}
		}
		if(originalClass.isFinal() != nextClass.isFinal()) {
			finalChanged = true;
			if(nextClass.isFinal()) {
				AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("final", originalClass, nextClass);
				refactorings.add(refactoring);
			}
			else if(originalClass.isFinal()) {
				RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("final", originalClass, nextClass);
				refactorings.add(refactoring);
			}
		}
		if(originalClass.isStatic() != nextClass.isStatic()) {
			staticChanged = true;
			if(nextClass.isStatic()) {
				AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("static", originalClass, nextClass);
				refactorings.add(refactoring);
			}
			else if(originalClass.isStatic()) {
				RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("static", originalClass, nextClass);
				refactorings.add(refactoring);
			}
		}
	}

	private void processAnnotations() {
		this.annotationListDiff = new UMLAnnotationListDiff(originalClass.getAnnotations(), nextClass.getAnnotations());
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddClassAnnotationRefactoring refactoring = new AddClassAnnotationRefactoring(annotation, originalClass, nextClass);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveClassAnnotationRefactoring refactoring = new RemoveClassAnnotationRefactoring(annotation, originalClass, nextClass);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			ModifyClassAnnotationRefactoring refactoring = new ModifyClassAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), originalClass, nextClass);
			refactorings.add(refactoring);
		}
	}

	public UMLOperationBodyMapper findMapperWithMatchingSignatures(UMLOperation operation1, UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getOperation1() != null && mapper.getOperation1().equalSignature(operation1) && mapper.getOperation2() != null && mapper.getOperation2().equalSignature(operation2)) {
				return mapper;
			}
		}
		return null;
	}

	public UMLOperationBodyMapper findMapperWithMatchingSignature2(UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getOperation2() != null && mapper.getOperation2().equalSignature(operation2)) {
				return mapper;
			}
		}
		return null;
	}

	public Set<UMLType> nextClassCommonInterfaces(UMLClassBaseDiff other) {
		Set<UMLType> common = new LinkedHashSet<UMLType>(nextClass.getImplementedInterfaces());
		common.retainAll(other.nextClass.getImplementedInterfaces());
		return common;
	}

	protected void checkForAttributeChanges() throws RefactoringMinerTimedOutException {
		//optional step
	}

	protected void createBodyMappers() throws RefactoringMinerTimedOutException {
		//optional step
	}

	protected void processAnonymousClasses() {
		for(UMLAnonymousClass umlAnonymousClass : originalClass.getAnonymousClassList()) {
    		if(!nextClass.containsAnonymousWithSameAttributesAndOperations(umlAnonymousClass))
    			this.removedAnonymousClasses.add(umlAnonymousClass);
    	}
    	for(UMLAnonymousClass umlAnonymousClass : nextClass.getAnonymousClassList()) {
    		if(!originalClass.containsAnonymousWithSameAttributesAndOperations(umlAnonymousClass))
    			this.addedAnonymousClasses.add(umlAnonymousClass);
    	}
	}

	protected void processEnumConstants() throws RefactoringMinerTimedOutException {
		for(UMLEnumConstant enumConstant : originalClass.getEnumConstants()) {
			UMLEnumConstant matchingEnumConstant = nextClass.containsEnumConstant(enumConstant);
    		if(matchingEnumConstant == null) {
    			this.removedEnumConstants.add(enumConstant);
    		}
    		else {
    			UMLEnumConstantDiff enumConstantDiff = new UMLEnumConstantDiff(enumConstant, matchingEnumConstant, this, modelDiff);
    			if(!enumConstantDiff.isEmpty()) {
	    			refactorings.addAll(enumConstantDiff.getRefactorings());
	    			this.enumConstantDiffList.add(enumConstantDiff);
    			}
    			else {
    				Pair<UMLEnumConstant, UMLEnumConstant> pair = Pair.of(enumConstant, matchingEnumConstant);
    				if(!this.commonEnumConstants.contains(pair)) {
    					this.commonEnumConstants.add(pair);
    					if(enumConstantDiff.getAnonymousClassDiff().isPresent()) {
        					this.enumConstantDiffList.add(enumConstantDiff);
        				}
    				}
    			}
    		}
    	}
    	for(UMLEnumConstant enumConstant : nextClass.getEnumConstants()) {
    		UMLEnumConstant matchingEnumConstant = originalClass.containsEnumConstant(enumConstant);
    		if(matchingEnumConstant == null) {
    			this.addedEnumConstants.add(enumConstant);
    		}
    		else {
    			UMLEnumConstantDiff enumConstantDiff = new UMLEnumConstantDiff(matchingEnumConstant, enumConstant, this, modelDiff);
    			if(!enumConstantDiff.isEmpty()) {
	    			refactorings.addAll(enumConstantDiff.getRefactorings());
					this.enumConstantDiffList.add(enumConstantDiff);
    			}
    			else {
    				Pair<UMLEnumConstant, UMLEnumConstant> pair = Pair.of(matchingEnumConstant, enumConstant);
    				if(!this.commonEnumConstants.contains(pair)) {
    					this.commonEnumConstants.add(pair);
    					if(enumConstantDiff.getAnonymousClassDiff().isPresent()) {
        					this.enumConstantDiffList.add(enumConstantDiff);
        				}
    				}
    			}
    		}
    	}
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
    		UMLAttribute attributeWithTheSameName = nextClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(attributeWithTheSameName == null) {
    			this.removedAttributes.add(attribute);
    		}
			else if(!attributeDiffListContainsAttribute(attribute, attributeWithTheSameName)) {
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, attributeWithTheSameName, this, modelDiff);
				if(!attributeDiff.isEmpty()) {
					refactorings.addAll(attributeDiff.getRefactorings());
					this.attributeDiffList.add(attributeDiff);
				}
				else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, attributeWithTheSameName);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    			}
			}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute attributeWithTheSameName = originalClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(attributeWithTheSameName == null) {
    			this.addedAttributes.add(attribute);
    		}
			else if(!attributeDiffListContainsAttribute(attributeWithTheSameName, attribute)) {
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attributeWithTheSameName, attribute, this, modelDiff);
				if(!attributeDiff.isEmpty()) {
					refactorings.addAll(attributeDiff.getRefactorings());
					this.attributeDiffList.add(attributeDiff);
				}
				else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attributeWithTheSameName, attribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    			}
			}
    	}
	}

	protected void processOperations() throws RefactoringMinerTimedOutException {
		for(UMLOperation operation : originalClass.getOperations()) {
    		UMLOperation operationWithTheSameSignature = nextClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.removedOperations.add(operation);
    		}
			else if(!mapperListContainsOperation(operation, operationWithTheSameSignature)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, this);
				this.operationBodyMapperList.add(mapper);
			}
    	}
    	for(UMLOperation operation : nextClass.getOperations()) {
    		UMLOperation operationWithTheSameSignature = originalClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.addedOperations.add(operation);
    		}
			else if(!mapperListContainsOperation(operationWithTheSameSignature, operation)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operationWithTheSameSignature, operation, this);
				this.operationBodyMapperList.add(mapper);
			}
    	}
	}

	private boolean attributeDiffListContainsAttribute(UMLAttribute attribute1, UMLAttribute attribute2) {
		for(UMLAttributeDiff diff : attributeDiffList) {
			if(diff.getRemovedAttribute().equals(attribute1) || diff.getAddedAttribute().equals(attribute2))
				return true;
		}
		return false;
	}

	private boolean mapperListContainsOperation(UMLOperation operation1, UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getContainer1().equals(operation1) || mapper.getContainer2().equals(operation2))
				return true;
		}
		return false;
	}

	public boolean matches(String className) {
		return this.originalClass.getName().equals(className) ||
				this.nextClass.getName().equals(className);
	}

	public boolean matches(UMLType type) {
		return this.originalClass.getName().endsWith("." + type.getClassType()) ||
				this.nextClass.getName().endsWith("." + type.getClassType());
	}

	//return true if "classMoveDiff" represents the move of a class that is inner to this.originalClass
	public boolean isInnerClassMove(UMLClassBaseDiff classDiff) {
		if(this.originalClass.isInnerClass(classDiff.originalClass) && this.nextClass.isInnerClass(classDiff.nextClass))
			return true;
		return false;
	}

	public boolean nextClassImportsType(String targetClass) {
		return nextClass.importsType(targetClass);
	}

	public boolean originalClassImportsType(String targetClass) {
		return originalClass.importsType(targetClass);
	}

	public List<UMLAttribute> nextClassAttributesOfType(String targetClass) {
		return nextClass.attributesOfType(targetClass);
	}

	public List<UMLAttribute> originalClassAttributesOfType(String targetClass) {
		return originalClass.attributesOfType(targetClass);
	}

	private void reportAddedImplementedInterface(UMLType implementedInterface) {
		this.addedImplementedInterfaces.add(implementedInterface);
	}

	private void reportRemovedImplementedInterface(UMLType implementedInterface) {
		this.removedImplementedInterfaces.add(implementedInterface);
	}

	private void setVisibilityChanged(boolean visibilityChanged) {
		this.visibilityChanged = visibilityChanged;
	}

	private void setOldVisibility(String oldVisibility) {
		this.oldVisibility = oldVisibility;
	}

	private void setNewVisibility(String newVisibility) {
		this.newVisibility = newVisibility;
	}

	private void setAbstractionChanged(boolean abstractionChanged) {
		this.abstractionChanged = abstractionChanged;
	}

	private void setOldAbstraction(boolean oldAbstraction) {
		this.oldAbstraction = oldAbstraction;
	}

	private void setNewAbstraction(boolean newAbstraction) {
		this.newAbstraction = newAbstraction;
	}

	private void setSuperclassChanged(boolean superclassChanged) {
		this.superclassChanged = superclassChanged;
	}

	private void setOldSuperclass(UMLType oldSuperclass) {
		this.oldSuperclass = oldSuperclass;
	}

	private void setNewSuperclass(UMLType newSuperclass) {
		this.newSuperclass = newSuperclass;
	}

	public UMLType getSuperclass() {
		if(!superclassChanged && oldSuperclass != null && newSuperclass != null)
			return oldSuperclass;
		return null;
	}

	public UMLType getOldSuperclass() {
		return oldSuperclass;
	}

	public UMLType getNewSuperclass() {
		return newSuperclass;
	}

	public List<UMLType> getAddedImplementedInterfaces() {
		return addedImplementedInterfaces;
	}

	public List<UMLType> getRemovedImplementedInterfaces() {
		return removedImplementedInterfaces;
	}

	public boolean containsOperationWithTheSameSignatureInOriginalClass(UMLOperation operation) {
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			if(originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public boolean containsOperationWithTheSameSignatureInNextClass(UMLOperation operation) {
		for(UMLOperation originalOperation : nextClass.getOperations()) {
			if(originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public boolean containsConcreteOperationWithTheSameSignatureInNextClass(UMLOperation operation) {
		for(UMLOperation originalOperation : nextClass.getOperations()) {
			if(originalOperation.getBody() != null && originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public UMLOperation containsAddedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation addedOperation : addedOperations) {
			if(addedOperation.equalSignature(operation))
				return addedOperation;
		}
		return null;
	}

	public UMLOperation containsRemovedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.equalSignature(operation))
				return removedOperation;
		}
		return null;
	}

	public UMLAttribute containsAddedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute addedAttribute : addedAttributes) {
			if(addedAttribute.equalsIgnoringChangedVisibility(attribute))
				return addedAttribute;
		}
		return null;
	}

	public UMLAttribute containsRemovedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute removedAttribute : removedAttributes) {
			if(removedAttribute.equalsIgnoringChangedVisibility(attribute))
				return removedAttribute;
		}
		return null;
	}

	private void processInheritance() {
		if(originalClass.getSuperclass() != null && nextClass.getSuperclass() != null) {
			if(!originalClass.getSuperclass().equals(nextClass.getSuperclass())) {
				setSuperclassChanged(true);
			}
			setOldSuperclass(originalClass.getSuperclass());
			setNewSuperclass(nextClass.getSuperclass());
		}
		else if(originalClass.getSuperclass() != null && nextClass.getSuperclass() == null) {
			setSuperclassChanged(true);
			setOldSuperclass(originalClass.getSuperclass());
			setNewSuperclass(nextClass.getSuperclass());
		}
		else if(originalClass.getSuperclass() == null && nextClass.getSuperclass() != null) {
			setSuperclassChanged(true);
			setOldSuperclass(originalClass.getSuperclass());
			setNewSuperclass(nextClass.getSuperclass());
		}
		for(UMLType implementedInterface : originalClass.getImplementedInterfaces()) {
			if(!nextClass.getImplementedInterfaces().contains(implementedInterface))
				reportRemovedImplementedInterface(implementedInterface);
		}
		for(UMLType implementedInterface : nextClass.getImplementedInterfaces()) {
			if(!originalClass.getImplementedInterfaces().contains(implementedInterface))
				reportAddedImplementedInterface(implementedInterface);
		}
	}

	private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation, UMLOperation addedOperation) {
		int index1 = originalClass.getOperations().indexOf(removedOperation);
		int index2 = nextClass.getOperations().indexOf(addedOperation);
		return Math.abs(index1-index2);
	}

	private void checkForOperationSignatureChanges() throws RefactoringMinerTimedOutException {
		consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
		int initialNumberOfRemovedOperations = removedOperations.size();
		int initialNumberOfAddedOperations = addedOperations.size();
		if(removedOperations.size() <= addedOperations.size()) {
			for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
				UMLOperation removedOperation = removedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					if(!containsMapperForOperation1(removedOperation) && !containsMapperForOperation2(addedOperation)) {
						int maxDifferenceInPosition;
						if(removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
							maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
						}
						else {
							maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
						}
						updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
						List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
						for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
							updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
						}
						if(initialNumberOfRemovedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && initialNumberOfAddedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && mapperSet.size() > 0 &&
								removedOperation.getName().equals(addedOperation.getName())) {
							break;
						}
					}
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					if(bestMapper != null) {
						removedOperation = bestMapper.getOperation1();
						UMLOperation addedOperation = bestMapper.getOperation2();
						addedOperations.remove(addedOperation);
						removedOperationIterator.remove();
						if(!removedOperation.getName().equals(addedOperation.getName()) &&
								!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
							Set<MethodInvocationReplacement> callReferences = getCallReferences(removedOperation, addedOperation);
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper, callReferences);
							refactorings.add(rename);
						}
						this.addOperationBodyMapper(bestMapper);
						consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
					}
				}
			}
		}
		else {
			for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
				UMLOperation addedOperation = addedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
					UMLOperation removedOperation = removedOperationIterator.next();
					if(!containsMapperForOperation1(removedOperation) && !containsMapperForOperation2(addedOperation)) {
						int maxDifferenceInPosition;
						if(removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
							maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
						}
						else {
							maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
						}
						updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
						List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
						for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
							updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
						}
						if(initialNumberOfRemovedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && initialNumberOfAddedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && mapperSet.size() > 0 &&
								removedOperation.getName().equals(addedOperation.getName())) {
							break;
						}
					}
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					if(bestMapper != null) {
						UMLOperation removedOperation = bestMapper.getOperation1();
						addedOperation = bestMapper.getOperation2();
						removedOperations.remove(removedOperation);
						addedOperationIterator.remove();
						if(!removedOperation.getName().equals(addedOperation.getName()) &&
								!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
							Set<MethodInvocationReplacement> callReferences = getCallReferences(removedOperation, addedOperation);
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper, callReferences);
							refactorings.add(rename);
						}
						this.addOperationBodyMapper(bestMapper);
						consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
					}
				}
			}
		}
		//infer signature changes for delegate methods calling methods in the operationDiffList
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			AbstractCall removedOperationInvocation = removedOperation.isDelegate();
			if(removedOperationInvocation != null) {
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					AbstractCall addedOperationInvocation = addedOperation.isDelegate();
					if(addedOperationInvocation != null) {
						for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
							if(removedOperationInvocation.matchesOperation(mapper.getContainer1(), removedOperation, modelDiff) &&
									addedOperationInvocation.matchesOperation(mapper.getContainer2(), addedOperation, modelDiff) &&
									removedOperation.getParameterTypeList().equals(addedOperation.getParameterTypeList())) {
								addedOperationIterator.remove();
								removedOperationIterator.remove();
			
								UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(removedOperation, addedOperation);
								refactorings.addAll(operationSignatureDiff.getRefactorings());
								if(!removedOperation.getName().equals(addedOperation.getName()) &&
										!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
									RenameOperationRefactoring rename = new RenameOperationRefactoring(removedOperation, addedOperation);
									refactorings.add(rename);
								}
								break;
							}
						}
					}
				}
			}
		}
	}

	private Set<MethodInvocationReplacement> getCallReferences(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<MethodInvocationReplacement> callReferences = new LinkedHashSet<MethodInvocationReplacement>();
		for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
			UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
			if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), modelDiff) &&
					replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), modelDiff)) {
				callReferences.add(replacement);
			}
		}
		return callReferences;
	}

	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> findConsistentMethodInvocationRenames() {
		Map<MethodInvocationReplacement, UMLOperationBodyMapper> map = new HashMap<MethodInvocationReplacement, UMLOperationBodyMapper>();
		Set<MethodInvocationReplacement> allConsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		Set<MethodInvocationReplacement> allInconsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		for(UMLOperationBodyMapper bodyMapper : operationBodyMapperList) {
			Set<MethodInvocationReplacement> methodInvocationRenames = bodyMapper.getMethodInvocationRenameReplacements();
			for(MethodInvocationReplacement replacement : methodInvocationRenames) {
				map.put(replacement, bodyMapper);
			}
			ConsistentReplacementDetector.updateRenames(allConsistentMethodInvocationRenames, allInconsistentMethodInvocationRenames,
					methodInvocationRenames);
		}
		//allConsistentMethodInvocationRenames.removeAll(allInconsistentMethodInvocationRenames);
		map.keySet().removeAll(allInconsistentMethodInvocationRenames);
		return map;
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation addedOperation, int differenceInPosition) throws RefactoringMinerTimedOutException {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
		List<AbstractCodeMapping> totalMappings = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0 || (removedOperation.getName().equals(addedOperation.getName()) && removedOperation.getBody() != null && addedOperation.getBody() != null)) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(exactMappings(operationBodyMapper)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition) &&
					removedOperation.testMethodCheck(addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodExtracted(removedOperation, addedOperation) || isPartOfMethodMovedToExistingMethod(removedOperation, addedOperation)) &&
					removedOperation.testMethodCheck(addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodInlined(removedOperation, addedOperation) || isPartOfMethodMovedFromExistingMethod(removedOperation, addedOperation)) &&
					removedOperation.testMethodCheck(addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else {
				for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
					UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
					if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), modelDiff) &&
							replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), modelDiff)) {
						mapperSet.add(operationBodyMapper);
						break;
					}
				}
			}
		}
		else {
			for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
				UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
				if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), modelDiff) &&
						replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), modelDiff)) {
					mapperSet.add(operationBodyMapper);
					break;
				}
			}
			if(matchingGetterSetterWithSameRenamePattern(removedOperation, addedOperation) && computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation) <= differenceInPosition) {
				mapperSet.add(operationBodyMapper);
			}
		}
		if(totalMappings.size() > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(singleUnmatchedStatementCallsAddedOperation(operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation operationInsideAnonymousClass, UMLOperation addedOperation, int differenceInPosition) throws RefactoringMinerTimedOutException {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, operationInsideAnonymousClass, this);
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(exactMappings(operationBodyMapper)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodExtracted(removedOperation, addedOperation) || isPartOfMethodMovedToExistingMethod(removedOperation, addedOperation))) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodInlined(removedOperation, addedOperation) || isPartOfMethodMovedFromExistingMethod(removedOperation, addedOperation))) {
				mapperSet.add(operationBodyMapper);
			}
			else {
				for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
					UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
					if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), modelDiff) &&
							replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), modelDiff)) {
						mapperSet.add(operationBodyMapper);
						break;
					}
				}
			}
		}
	}

	private boolean matchingGetterSetterWithSameRenamePattern(UMLOperation removedOperation, UMLOperation addedOperation) {
		String setPrefix = "set";
		String getPrefix = "get";
		if(removedOperation.getName().startsWith(setPrefix) && addedOperation.getName().startsWith(setPrefix)) {
			String removedOperationSuffix = removedOperation.getName().substring(setPrefix.length());
			String addedOperationSuffix = addedOperation.getName().substring(setPrefix.length());
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				if(mapper.getContainer1().getName().startsWith(getPrefix) && mapper.getContainer2().getName().startsWith(getPrefix)) {
					String container1Suffix = mapper.getContainer1().getName().substring(getPrefix.length());
					String container2Suffix = mapper.getContainer2().getName().substring(getPrefix.length());
					if(container1Suffix.equals(removedOperationSuffix) && container2Suffix.equals(addedOperationSuffix)) {
						return true;
					}
				}
			}
		}
		else if(removedOperation.getName().startsWith(getPrefix) && addedOperation.getName().startsWith(getPrefix)) {
			String removedOperationSuffix = removedOperation.getName().substring(getPrefix.length());
			String addedOperationSuffix = addedOperation.getName().substring(getPrefix.length());
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				if(mapper.getContainer1().getName().startsWith(setPrefix) && mapper.getContainer2().getName().startsWith(setPrefix)) {
					String container1Suffix = mapper.getContainer1().getName().substring(setPrefix.length());
					String container2Suffix = mapper.getContainer2().getName().substring(setPrefix.length());
					if(container1Suffix.equals(removedOperationSuffix) && container2Suffix.equals(addedOperationSuffix)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean exactMappings(UMLOperationBodyMapper operationBodyMapper) {
		if(operationBodyMapper.allMappingsAreExactMatches()) {
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0)
				return true;
			else if(operationBodyMapper.nonMappedElementsT1() > 0 && operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.nonMappedElementsT2() == 0) {
				int countableStatements = 0;
				int parameterizedVariableDeclarationStatements = 0;
				VariableDeclarationContainer addedOperation = operationBodyMapper.getContainer2();
				List<String> nonMappedLeavesT1 = new ArrayList<String>();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
					if(statement.countableStatement()) {
						nonMappedLeavesT1.add(statement.getString());
						for(String parameterName : addedOperation.getParameterNameList()) {
							if(statement.getVariableDeclaration(parameterName) != null) {
								parameterizedVariableDeclarationStatements++;
								break;
							}
						}
						countableStatements++;
					}
				}
				int nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation = 0;
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						for(AbstractCodeFragment statement : operation.getBody().getCompositeStatement().getLeaves()) {
							if(nonMappedLeavesT1.contains(statement.getString())) {
								nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation++;
							}
						}
					}
				}
				return (countableStatements == parameterizedVariableDeclarationStatements || countableStatements == nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation + parameterizedVariableDeclarationStatements) && countableStatements > 0;
			}
			else if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() > 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0) {
				int countableStatements = 0;
				int parameterizedVariableDeclarationStatements = 0;
				VariableDeclarationContainer removedOperation = operationBodyMapper.getContainer1();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT2()) {
					if(statement.countableStatement()) {
						for(String parameterName : removedOperation.getParameterNameList()) {
							if(statement.getVariableDeclaration(parameterName) != null) {
								parameterizedVariableDeclarationStatements++;
								break;
							}
						}
						countableStatements++;
					}
				}
				return countableStatements == parameterizedVariableDeclarationStatements && countableStatements > 0;
			}
			else if((operationBodyMapper.nonMappedElementsT1() == 1 || operationBodyMapper.nonMappedElementsT2() == 1) &&
					operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0) {
				AbstractCodeFragment statementUsingParameterAsInvoker1 = null;
				VariableDeclarationContainer removedOperation = operationBodyMapper.getContainer1();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
					if(statement.countableStatement()) {
						for(String parameterName : removedOperation.getParameterNameList()) {
							AbstractCall invocation = statement.invocationCoveringEntireFragment();
							if(invocation != null && invocation.getExpression() != null && invocation.getExpression().equals(parameterName)) {
								statementUsingParameterAsInvoker1 = statement;
								break;
							}
						}
					}
				}
				AbstractCodeFragment statementUsingParameterAsInvoker2 = null;
				VariableDeclarationContainer addedOperation = operationBodyMapper.getContainer2();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT2()) {
					if(statement.countableStatement()) {
						for(String parameterName : addedOperation.getParameterNameList()) {
							AbstractCall invocation = statement.invocationCoveringEntireFragment();
							if(invocation != null && invocation.getExpression() != null && invocation.getExpression().equals(parameterName)) {
								statementUsingParameterAsInvoker2 = statement;
								break;
							}
						}
					}
				}
				if(statementUsingParameterAsInvoker1 != null && statementUsingParameterAsInvoker2 != null) {
					for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
						if(mapping.getFragment1() instanceof CompositeStatementObject && mapping.getFragment2() instanceof CompositeStatementObject) {
							CompositeStatementObject parent1 = (CompositeStatementObject)mapping.getFragment1();
							CompositeStatementObject parent2 = (CompositeStatementObject)mapping.getFragment2();
							if(parent1.getLeaves().contains(statementUsingParameterAsInvoker1) && parent2.getLeaves().contains(statementUsingParameterAsInvoker2)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		List<CompositeReplacement> composites = operationBodyMapper.getCompositeReplacements();
		int additionallyMatchedStatements1 = 0;
		int additionallyMatchedStatements2 = 0;
		for(CompositeReplacement composite : composites) {
			additionallyMatchedStatements1 += composite.getAdditionallyMatchedStatements1().size();
			additionallyMatchedStatements2 += composite.getAdditionallyMatchedStatements2().size();
		}
		mappings += additionallyMatchedStatements1 + additionallyMatchedStatements2;
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1() - additionallyMatchedStatements1;
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2() - additionallyMatchedStatements2;
		int exactMappings = operationBodyMapper.exactMatches();
		return (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
				(nonMappedElementsT1 == 0 && mappings > Math.floor(nonMappedElementsT2/2.0)) ||
				(nonMappedElementsT1 == 0 && exactMappings >= Math.floor(nonMappedElementsT2/2.0)) ||
				(mappings == 1 && nonMappedElementsT1 + nonMappedElementsT2 == 1 && operationBodyMapper.getContainer1().getName().equals(operationBodyMapper.getContainer2().getName()));
	}

	private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
		int nonMappedElementsT2WithoutThoseCallingAddedOperation = nonMappedElementsT2 - nonMappedElementsT2CallingAddedOperation;
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
				nonMappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation) ||
				(operationBodyMapper.getMappings().size() > nonMappedElementsT2 && nonMappedElementsT2CallingAddedOperation > 0 &&
						operationBodyMapper.getContainer1().getClassName().equals(operationBodyMapper.getContainer2().getClassName()));
	}

	private boolean mappedElementsMoreThanNonMappedT1(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT1CallingRemovedOperation = operationBodyMapper.nonMappedElementsT1CallingRemovedOperation(removedOperations);
		int nonMappedElementsT1WithoutThoseCallingRemovedOperation = nonMappedElementsT1 - nonMappedElementsT1CallingRemovedOperation;
		return mappings > nonMappedElementsT1 || (mappings >= nonMappedElementsT1WithoutThoseCallingRemovedOperation &&
				nonMappedElementsT1CallingRemovedOperation >= nonMappedElementsT1WithoutThoseCallingRemovedOperation);
	}

	private UMLOperationBodyMapper findBestMapper(TreeSet<UMLOperationBodyMapper> mapperSet) {
		List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>(mapperSet);
		UMLOperationBodyMapper bestMapper = mapperSet.first();
		VariableDeclarationContainer bestMapperOperation1 = bestMapper.getContainer1();
		VariableDeclarationContainer bestMapperOperation2 = bestMapper.getContainer2();
		boolean identicalBodyWithOperation1OfTheBestMapper = identicalBodyWithAnotherAddedMethod(bestMapper);
		boolean identicalBodyWithOperation2OfTheBestMapper = identicalBodyWithAnotherRemovedMethod(bestMapper);
		if(equalSignatureWithCommonParameterTypes(bestMapperOperation1, bestMapperOperation2) &&
				!identicalBodyWithOperation1OfTheBestMapper && !identicalBodyWithOperation2OfTheBestMapper) {
			return bestMapper;
		}
		for(int i=1; i<mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			if(mapper.getMappings().size() > 0) {
				VariableDeclarationContainer operation2 = mapper.getContainer2();
				List<AbstractCall> operationInvocations2 = operation2.getAllOperationInvocations();
				boolean anotherMapperCallsOperation2OfTheBestMapper = false;
				for(AbstractCall invocation : operationInvocations2) {
					if(invocation.matchesOperation(bestMapper.getContainer2(), operation2, modelDiff) && !invocation.matchesOperation(bestMapper.getContainer1(), operation2, modelDiff) &&
							!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, removedOperations)) {
						anotherMapperCallsOperation2OfTheBestMapper = true;
						break;
					}
				}
				VariableDeclarationContainer operation1 = mapper.getContainer1();
				List<AbstractCall> operationInvocations1 = operation1.getAllOperationInvocations();
				boolean anotherMapperCallsOperation1OfTheBestMapper = false;
				for(AbstractCall invocation : operationInvocations1) {
					if(invocation.matchesOperation(bestMapper.getContainer1(), operation1, modelDiff) && !invocation.matchesOperation(bestMapper.getContainer2(), operation1, modelDiff) &&
							!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, addedOperations)) {
						anotherMapperCallsOperation1OfTheBestMapper = true;
						break;
					}
				}
				if(anotherMapperCallsOperation2OfTheBestMapper || anotherMapperCallsOperation1OfTheBestMapper) {
					return mapper;
				}
			}
		}
		if(mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames.keySet()) && !exactMappings(bestMapper)) {
			return null;
		}
		if(identicalBodyWithOperation2OfTheBestMapper || identicalBodyWithOperation1OfTheBestMapper) {
			return null;
		}
		return bestMapper;
	}

	private boolean equalSignatureWithCommonParameterTypes(VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
		return operation1.equalReturnParameter(operation2) &&
				operation1.getName().equals(operation2.getName()) &&
				operation1.commonParameterTypes(operation2).size() > 0;
	}

	private boolean identicalBodyWithAnotherAddedMethod(UMLOperationBodyMapper mapper) {
		VariableDeclarationContainer operation1 = mapper.getContainer1();
		List<String> stringRepresentation = operation1.stringRepresentation();
		// 3 corresponds to the opening and closing bracket of a method + a single statement
		if(stringRepresentation.size() > 3) {
			for(UMLOperation addedOperation : addedOperations) {
				if(!mapper.getContainer2().equals(addedOperation)) {
					OperationBody body = addedOperation.getBody();
					if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode()) {
						return true;
					}
					else if(equalSignatureWithCommonParameterTypes(operation1, addedOperation)) {
						List<String> commonStatements = new ArrayList<String>();
						List<String> addedOperationStringRepresentation = addedOperation.stringRepresentation();
						for(String statement : addedOperationStringRepresentation) {
							if(!statement.equals("{") && !statement.equals("}") && !statement.equals("try") && !statement.startsWith("catch(") && !statement.startsWith("case ") && !statement.startsWith("default :") &&
									!statement.startsWith("return true;") && !statement.startsWith("return false;") && !statement.startsWith("return this;") && !statement.startsWith("return null;") && !statement.startsWith("return;")) {
								if(stringRepresentation.contains(statement)) {
									commonStatements.add(statement);
								}
							}
						}
						if(commonStatements.size() > mapper.exactMatches()*2) {
							return true;
						}
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					for(UMLOperation addedOperation : addedClass.getOperations()) {
						OperationBody body = addedOperation.getBody();
						List<String> parameterNameList = addedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation1.getParameterNameList())) {
							return true;
						}
					}
				}
			}
		}
		else if(stringRepresentation.size() == 3) {
			int counter = 0;
			for(UMLOperation addedOperation : addedOperations) {
				if(!mapper.getContainer2().equals(addedOperation)) {
					OperationBody body = addedOperation.getBody();
					List<String> parameterNameList = addedOperation.getParameterNameList();
					if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode() &&
							parameterNameList.size() > 0 && parameterNameList.equals(operation1.getParameterNameList())) {
						counter++;
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					for(UMLOperation addedOperation : addedClass.getOperations()) {
						OperationBody body = addedOperation.getBody();
						List<String> parameterNameList = addedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation1.getParameterNameList())) {
							counter++;
						}
					}
				}
			}
			if(counter == 1 && !existingMapperWithIdenticalMapping(stringRepresentation.get(1))) {
				return true;
			}
		}
		return false;
	}

	private boolean identicalBodyWithAnotherRemovedMethod(UMLOperationBodyMapper mapper) {
		VariableDeclarationContainer operation2 = mapper.getContainer2();
		List<String> stringRepresentation = operation2.stringRepresentation();
		// 3 corresponds to the opening and closing bracket of a method + a single statement
		if(stringRepresentation.size() > 3) {
			for(UMLOperation removedOperation : removedOperations) {
				if(!mapper.getContainer1().equals(removedOperation)) {
					OperationBody body = removedOperation.getBody();
					if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode()) {
						return true;
					}
					else if(equalSignatureWithCommonParameterTypes(removedOperation, operation2)) {
						List<String> commonStatements = new ArrayList<String>();
						List<String> removedOperationStringRepresentation = removedOperation.stringRepresentation();
						for(String statement : removedOperationStringRepresentation) {
							if(!statement.equals("{") && !statement.equals("}") && !statement.equals("try") && !statement.startsWith("catch(") && !statement.startsWith("case ") && !statement.startsWith("default :") &&
									!statement.startsWith("return true;") && !statement.startsWith("return false;") && !statement.startsWith("return this;") && !statement.startsWith("return null;") && !statement.startsWith("return;")) {
								if(stringRepresentation.contains(statement)) {
									commonStatements.add(statement);
								}
							}
						}
						if(commonStatements.size() > mapper.exactMatches()*2) {
							return true;
						}
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
					for(UMLOperation removedOperation : removedClass.getOperations()) {
						OperationBody body = removedOperation.getBody();
						List<String> parameterNameList = removedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation2.getParameterNameList())) {
							return true;
						}
					}
				}
			}
		}
		else if(stringRepresentation.size() == 3) {
			int counter = 0;
			for(UMLOperation removedOperation : removedOperations) {
				if(!mapper.getContainer1().equals(removedOperation)) {
					OperationBody body = removedOperation.getBody();
					List<String> parameterNameList = removedOperation.getParameterNameList();
					if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode() &&
							parameterNameList.size() > 0 && parameterNameList.equals(operation2.getParameterNameList())) {
						counter++;
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
					for(UMLOperation removedOperation : removedClass.getOperations()) {
						OperationBody body = removedOperation.getBody();
						List<String> parameterNameList = removedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation2.getParameterNameList())) {
							counter++;
						}
					}
				}
			}
			if(counter == 1 && !existingMapperWithIdenticalMapping(stringRepresentation.get(1))) {
				return true;
			}
		}
		return false;
	}

	private boolean existingMapperWithIdenticalMapping(String stringRepresentation) {
		for(int i=operationBodyMapperList.size()-1; i>=0; i--) {
			UMLOperationBodyMapper mapper = operationBodyMapperList.get(i);
			for(AbstractCodeMapping mapping : mapper.getExactMatches()) {
				if(mapping.getFragment1().getString().equals(stringRepresentation) ||
						mapping.getFragment2().getString().equals(stringRepresentation)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean mismatchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper, Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
		for(MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
			if(mapper.getContainer1().getName().equals(rename.getBefore()) && !mapper.getContainer2().getName().equals(rename.getAfter())) {
				return true;
			}
			else if(!mapper.getContainer1().getName().equals(rename.getBefore()) && mapper.getContainer2().getName().equals(rename.getAfter())) {
				return true;
			}
		}
		return false;
	}

	private boolean operationContainsMethodInvocationWithTheSameNameAndCommonArguments(AbstractCall invocation, List<UMLOperation> operations) {
		for(UMLOperation operation : operations) {
			List<AbstractCall> operationInvocations = operation.getAllOperationInvocations();
			for(AbstractCall operationInvocation : operationInvocations) {
				Set<String> argumentIntersection = new LinkedHashSet<String>(operationInvocation.getArguments());
				argumentIntersection.retainAll(invocation.getArguments());
				if(operationInvocation.getName().equals(invocation.getName()) && !argumentIntersection.isEmpty()) {
					return true;
				}
				else if(argumentIntersection.size() > 0 && argumentIntersection.size() == invocation.getArguments().size()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean singleUnmatchedStatementCallsAddedOperation(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeFragment> nonMappedLeavesT1 = operationBodyMapper.getNonMappedLeavesT1();
		List<AbstractCodeFragment> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		if(nonMappedLeavesT1.size() == 1 && nonMappedLeavesT2.size() == 1) {
			AbstractCodeFragment statementT2 = nonMappedLeavesT2.get(0);
			AbstractCall invocationT2 = statementT2.invocationCoveringEntireFragment();
			if(invocationT2 != null) {
				for(UMLOperation addedOperation : addedOperations) {
					if(invocationT2.matchesOperation(addedOperation, operationBodyMapper.getContainer2(), modelDiff)) {
						AbstractCodeFragment statementT1 = nonMappedLeavesT1.get(0);
						AbstractCall invocationT1 = statementT1.invocationCoveringEntireFragment();
						if(invocationT1 != null && addedOperation.getAllOperationInvocations().contains(invocationT1)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean compatibleSignatures(UMLOperation removedOperation, UMLOperation addedOperation, int absoluteDifferenceInPosition) {
		if(addedOperation.compatibleSignature(removedOperation)) {
			return true;
		}
		if(absoluteDifferenceInPosition == 0 || operationsBeforeAndAfterMatch(removedOperation, addedOperation)) {
			if(!gettersWithDifferentReturnType(removedOperation, addedOperation)) {
				if(addedOperation.getParameterTypeList().equals(removedOperation.getParameterTypeList()) || addedOperation.normalizedNameDistance(removedOperation) <= MAX_OPERATION_NAME_DISTANCE) {
					return true;
				}
				else if(addedOperation.hasTestAnnotation() && removedOperation.hasTestAnnotation()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean gettersWithDifferentReturnType(UMLOperation removedOperation, UMLOperation addedOperation) {
		if(removedOperation.isGetter() && addedOperation.isGetter()) {
			UMLType type1 = removedOperation.getReturnParameter().getType();
			UMLType type2 = addedOperation.getReturnParameter().getType();
			if(!removedOperation.equalReturnParameter(addedOperation) && !type1.compatibleTypes(type2)) {
				return true;
			}
		}
		return false;
	}

	private boolean operationsBeforeAndAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
		UMLOperation operationBefore1 = null;
		UMLOperation operationAfter1 = null;
		List<UMLOperation> originalClassOperations = originalClass.getOperations();
		for(int i=0; i<originalClassOperations.size(); i++) {
			UMLOperation current = originalClassOperations.get(i);
			if(current.equals(removedOperation)) {
				if(i>0) {
					operationBefore1 = originalClassOperations.get(i-1);
				}
				if(i<originalClassOperations.size()-1) {
					operationAfter1 = originalClassOperations.get(i+1);
				}
			}
		}
		
		UMLOperation operationBefore2 = null;
		UMLOperation operationAfter2 = null;
		List<UMLOperation> nextClassOperations = nextClass.getOperations();
		for(int i=0; i<nextClassOperations.size(); i++) {
			UMLOperation current = nextClassOperations.get(i);
			if(current.equals(addedOperation)) {
				if(i>0) {
					operationBefore2 = nextClassOperations.get(i-1);
				}
				if(i<nextClassOperations.size()-1) {
					operationAfter2 = nextClassOperations.get(i+1);
				}
			}
		}
		
		boolean operationsBeforeMatch = false;
		if(operationBefore1 != null && operationBefore2 != null) {
			operationsBeforeMatch = (operationBefore1.equalReturnParameter(operationBefore2) && operationBefore1.equalParameterTypes(operationBefore2) && operationBefore1.getName().equals(operationBefore2.getName()))
					|| (matchingDataProviderAnnotation(removedOperation, operationBefore1) && matchingDataProviderAnnotation(addedOperation, operationBefore2));
		}
		else if(operationBefore1 == null && operationBefore2 == null) {
			//both operations are in the first position
			operationsBeforeMatch = true;
		}
		
		boolean operationsAfterMatch = false;
		if(operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = (operationAfter1.equalReturnParameter(operationAfter2) && operationAfter1.equalParameterTypes(operationAfter2) && operationAfter1.getName().equals(operationAfter2.getName()))
					|| (matchingDataProviderAnnotation(removedOperation, operationAfter1) && matchingDataProviderAnnotation(addedOperation, operationAfter2));
		}
		else if(operationAfter1 == null && operationAfter2 == null) {
			//both operations are in the last position
			operationsAfterMatch = true;
		}
		
		return operationsBeforeMatch || operationsAfterMatch;
	}

	private boolean matchingDataProviderAnnotation(UMLOperation operation1, UMLOperation operation2) {
		UMLAnnotation testAnnotation = null;
		UMLAnnotation dataProviderAnnotation = null;
		if(operation1.hasTestAnnotation() && operation2.hasDataProviderAnnotation()) {
			List<UMLAnnotation> annotations1 = operation1.getAnnotations();
			for(UMLAnnotation annotation1 : annotations1) {
				if(annotation1.getTypeName().equals("Test")) {
					testAnnotation = annotation1;
					break;
				}
			}
			List<UMLAnnotation> annotations2 = operation2.getAnnotations();
			for(UMLAnnotation annotation2 : annotations2) {
				if(annotation2.getTypeName().equals("DataProvider")) {
					dataProviderAnnotation = annotation2;
					break;
				}
			}
		}
		else if(operation2.hasTestAnnotation() && operation1.hasDataProviderAnnotation()) {
			List<UMLAnnotation> annotations2 = operation2.getAnnotations();
			for(UMLAnnotation annotation2 : annotations2) {
				if(annotation2.getTypeName().equals("Test")) {
					testAnnotation = annotation2;
					break;
				}
			}
			List<UMLAnnotation> annotations1 = operation1.getAnnotations();
			for(UMLAnnotation annotation1 : annotations1) {
				if(annotation1.getTypeName().equals("DataProvider")) {
					dataProviderAnnotation = annotation1;
					break;
				}
			}
		}
		if(testAnnotation != null && dataProviderAnnotation != null) {
			Map<String, AbstractExpression> testMemberValuePairs = testAnnotation.getMemberValuePairs();
			if(testMemberValuePairs.containsKey("dataProvider")) {
				Map<String, AbstractExpression> dataProviderMemberValuePairs = dataProviderAnnotation.getMemberValuePairs();
				if(dataProviderMemberValuePairs.containsKey("name")) {
					return testMemberValuePairs.get("dataProvider").getExpression().equals(dataProviderMemberValuePairs.get("name").getExpression());
				}
			}
		}
		return false;
	}

	private void checkForInlinedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> inlinedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				InlineOperationDetection detection = new InlineOperationDetection(mapper, removedOperations, this, modelDiff);
				List<InlineOperationRefactoring> refs = detection.check(removedOperation);
				for(InlineOperationRefactoring refactoring : refs) {
					refactorings.add(refactoring);
					UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
					inlinedOperationMappers.add(operationBodyMapper);
					mapper.addChildMapper(operationBodyMapper);
					operationsToBeRemoved.add(removedOperation);
				}
			}
		}
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			optimizeDuplicateMappingsForInline(mapper);
		}
		for(UMLOperationBodyMapper operationBodyMapper : inlinedOperationMappers) {
			processMapperRefactorings(operationBodyMapper, refactorings);
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}

	private void optimizeDuplicateMappingsForInline(UMLOperationBodyMapper parentMapper) {
		if(parentMapper.getChildMappers().size() > 1) {
			Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings = new HashMap<>();
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers = new HashMap<>();
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				for(AbstractCodeMapping mapping : childMapper.getMappings()) {
					if(oneToManyMappings.containsKey(mapping.getFragment2())) {
						oneToManyMappings.get(mapping.getFragment2()).add(mapping);
						oneToManyMappers.get(mapping.getFragment2()).add(childMapper);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>();
						List<UMLOperationBodyMapper> mappers = new ArrayList<>();
						mappings.add(mapping);
						mappers.add(childMapper);
						oneToManyMappings.put(mapping.getFragment2(), mappings);
						oneToManyMappers.put(mapping.getFragment2(), mappers);
					}
				}
			}
			for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
				if(oneToManyMappings.containsKey(mapping.getFragment2())) {
					List<UMLOperationBodyMapper> childMappers = oneToManyMappers.get(mapping.getFragment2());
					boolean singleStatementInlinedMethod = false;
					for(UMLOperationBodyMapper childMapper : childMappers) {
						if(childMapper.getContainer1().stringRepresentation().size() == 3) {
							singleStatementInlinedMethod = true;
							break;
						}
					}
					if(!singleStatementInlinedMethod) {
						oneToManyMappings.get(mapping.getFragment2()).add(mapping);
						oneToManyMappers.get(mapping.getFragment2()).add(parentMapper);
					}
				}
				else {
					List<AbstractCodeMapping> mappings = new ArrayList<>();
					List<UMLOperationBodyMapper> mappers = new ArrayList<>();
					mappings.add(mapping);
					mappers.add(parentMapper);
					oneToManyMappings.put(mapping.getFragment2(), mappings);
					oneToManyMappers.put(mapping.getFragment2(), mappers);
				}
			}
			optimizeDuplicateMappings(oneToManyMappings, oneToManyMappers);
		}
	}

	private void checkForExtractedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> extractedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			ExtractOperationDetection detection = new ExtractOperationDetection(mapper, addedOperations, this, modelDiff);
			List<UMLOperation> sortedAddedOperations = detection.getAddedOperationsSortedByCalls();
			for(UMLOperation addedOperation : sortedAddedOperations) {
				List<ExtractOperationRefactoring> refs = detection.check(addedOperation);
				for(ExtractOperationRefactoring refactoring : refs) {
					refactorings.add(refactoring);
					UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
					extractedOperationMappers.add(operationBodyMapper);
					mapper.addChildMapper(operationBodyMapper);
					operationsToBeRemoved.add(addedOperation);
				}
			}
		}
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			optimizeDuplicateMappingsForExtract(mapper);
		}
		for(UMLOperationBodyMapper operationBodyMapper : extractedOperationMappers) {
			processMapperRefactorings(operationBodyMapper, refactorings);
		}
		addedOperations.removeAll(operationsToBeRemoved);
	}

	private boolean subexpressionOverlap(List<AbstractCodeMapping> mappings, AbstractCodeMapping newMapping) {
		for(AbstractCodeMapping previousMapping : mappings) {
			AbstractCodeFragment previousFragment2 = previousMapping.getFragment2();
			AbstractCodeFragment newFragment2 = newMapping.getFragment2();
			if(previousFragment2.getString().startsWith("return ") && previousFragment2.getString().endsWith(";\n") &&
					newFragment2.getString().startsWith("return ") && newFragment2.getString().endsWith(";\n")) {
				String previousReturnExpression = previousFragment2.getString().substring("return ".length(), previousFragment2.getString().length()-2);
				String newReturnExpression = newFragment2.getString().substring("return ".length(), newFragment2.getString().length()-2);
				if(previousReturnExpression.contains("(" + newReturnExpression + ")") || newReturnExpression.contains("(" + previousReturnExpression + ")")) {
					return true;
				}
			}
		}
		return false;
	}

	private void optimizeDuplicateMappingsForExtract(UMLOperationBodyMapper parentMapper) {
		if(parentMapper.getChildMappers().size() > 1) {
			Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings = new HashMap<>();
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers = new HashMap<>();
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				for(AbstractCodeMapping mapping : childMapper.getMappings()) {
					if(oneToManyMappings.containsKey(mapping.getFragment1())) {
						if(!subexpressionOverlap(oneToManyMappings.get(mapping.getFragment1()), mapping)) {
							oneToManyMappings.get(mapping.getFragment1()).add(mapping);
							oneToManyMappers.get(mapping.getFragment1()).add(childMapper);
						}
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>();
						List<UMLOperationBodyMapper> mappers = new ArrayList<>();
						mappings.add(mapping);
						mappers.add(childMapper);
						oneToManyMappings.put(mapping.getFragment1(), mappings);
						oneToManyMappers.put(mapping.getFragment1(), mappers);
					}
				}
			}
			for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
				if(oneToManyMappings.containsKey(mapping.getFragment1())) {
					List<UMLOperationBodyMapper> childMappers = oneToManyMappers.get(mapping.getFragment1());
					boolean singleStatementExtractedMethod = false;
					for(UMLOperationBodyMapper childMapper : childMappers) {
						if(childMapper.getContainer2().stringRepresentation().size() == 3) {
							singleStatementExtractedMethod = true;
							break;
						}
					}
					if(!singleStatementExtractedMethod) {
						oneToManyMappings.get(mapping.getFragment1()).add(mapping);
						oneToManyMappers.get(mapping.getFragment1()).add(parentMapper);
					}
				}
				else {
					List<AbstractCodeMapping> mappings = new ArrayList<>();
					List<UMLOperationBodyMapper> mappers = new ArrayList<>();
					mappings.add(mapping);
					mappers.add(parentMapper);
					oneToManyMappings.put(mapping.getFragment1(), mappings);
					oneToManyMappers.put(mapping.getFragment1(), mappers);
				}
			}
			optimizeDuplicateMappings(oneToManyMappings, oneToManyMappers);
		}
	}

	private void optimizeDuplicateMappings(Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings,
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers) {
		for(Iterator<AbstractCodeFragment> it = oneToManyMappers.keySet().iterator(); it.hasNext();) {
			AbstractCodeFragment fragment = it.next();
			if(oneToManyMappings.get(fragment).size() == 1) {
				oneToManyMappings.remove(fragment);
			}
		}
		//sort oneToManyMappings keys to put first composite statements, then blocks, then leaf statements
		TreeSet<AbstractCodeFragment> sortedKeys = new TreeSet<>(new CodeFragmentComparator());
		sortedKeys.addAll(oneToManyMappings.keySet());
		Set<UMLOperationBodyMapper> updatedMappers = new LinkedHashSet<>();
		for(AbstractCodeFragment fragment : sortedKeys) {
			List<AbstractCodeMapping> mappings = oneToManyMappings.get(fragment);
			List<UMLOperationBodyMapper> mappers = oneToManyMappers.get(fragment);
			Iterator<AbstractCodeMapping> mappingIterator = mappings.iterator();
			Iterator<UMLOperationBodyMapper> mapperIterator = mappers.iterator();
			List<Boolean> callsExtractedInlinedMethod = new ArrayList<>();
			List<Boolean> parentMappingFound = new ArrayList<>();
			List<Boolean> parentIsContainerBody = new ArrayList<>();
			List<Boolean> nestedMapper = new ArrayList<>();
			List<Boolean> identical = new ArrayList<>();
			List<Integer> nonMappedNodes = new ArrayList<>();
			List<Integer> replacementTypeCount = new ArrayList<>();
			List<Boolean> replacementCoversEntireStatement = new ArrayList<>();
			List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
			while(mappingIterator.hasNext()) {
				AbstractCodeMapping mapping = mappingIterator.next();
				UMLOperationBodyMapper mapper = mapperIterator.next();
				callsExtractedInlinedMethod.add(mapper.containsExtractedOrInlinedOperationInvocation(mapping));
				parentMappingFound.add(mapper.containsParentMapping(mapping));
				parentIsContainerBody.add(mapper.parentIsContainerBody(mapping));
				nestedMapper.add(mapper.isNested());
				identical.add(mapping.getFragment1().getString().equals(mapping.getFragment2().getString()));
				if(mapper.getParentMapper() != null) {
					if(mapper.getContainer1().equals(mapper.getParentMapper().getContainer1()) && !mapper.getContainer2().equals(mapper.getParentMapper().getContainer2())) {
						//extract method scenario
						nonMappedNodes.add(mapper.nonMappedElementsT2());
					}
					else if(!mapper.getContainer1().equals(mapper.getParentMapper().getContainer1()) && mapper.getContainer2().equals(mapper.getParentMapper().getContainer2())) {
						//inline method scenario
						nonMappedNodes.add(mapper.nonMappedElementsT1());
					}
				}
				else {
					nonMappedNodes.add(0);
				}
				replacementTypeCount.add(mapper.getReplacementTypesExcludingParameterToArgumentMaps(mapping).size());
				boolean replacementFound = false;
				for(Replacement r : mapping.getReplacements()) {
					if((r.getBefore().equals(mapping.getFragment1().getString()) || (r.getBefore() + ";\n").equals(mapping.getFragment1().getString())) &&
							(r.getAfter().equals(mapping.getFragment2().getString()) || (r.getAfter() + ";\n").equals(mapping.getFragment2().getString()))) {
						replacementFound = true;
						break;
					}
				}
				replacementCoversEntireStatement.add(replacementFound);
				parentMappers.add(mapper.getParentMapper());
			}
			Set<Integer> indicesToBeRemoved = new LinkedHashSet<>();
			if(callsExtractedInlinedMethod.contains(true) && callsExtractedInlinedMethod.contains(false)) {
				for(int i=0; i<callsExtractedInlinedMethod.size(); i++) {
					if(callsExtractedInlinedMethod.get(i) == true) {
						indicesToBeRemoved.add(i);
					}
				}
			}
			else if(parentMappingFound.contains(true)) {
				for(int i=0; i<parentMappingFound.size(); i++) {
					if(parentMappingFound.get(i) == false) {
						indicesToBeRemoved.add(i);
					}
				}
				determineIndicesToBeRemoved(nestedMapper, identical, replacementTypeCount, replacementCoversEntireStatement, indicesToBeRemoved);
			}
			else if(parentIsContainerBody.contains(true)) {
				for(int i=0; i<parentIsContainerBody.size(); i++) {
					if(parentIsContainerBody.get(i) == false) {
						indicesToBeRemoved.add(i);
					}
				}
				determineIndicesToBeRemoved(nestedMapper, identical, replacementTypeCount, replacementCoversEntireStatement, indicesToBeRemoved);
			}
			if(indicesToBeRemoved.isEmpty() && matchingParentMappers(parentMappers) == parentMappers.size()) {
				int minimum = nonMappedNodes.get(0);
				for(int i=1; i<nonMappedNodes.size(); i++) {
					if(nonMappedNodes.get(i) < minimum) {
						minimum = nonMappedNodes.get(i);
					}
				}
				for(int i=0; i<nonMappedNodes.size(); i++) {
					if(nonMappedNodes.get(i) > minimum) {
						indicesToBeRemoved.add(i);
					}
				}
			}
			mappingIterator = mappings.iterator();
			mapperIterator = mappers.iterator();
			int index = 0;
			while(mappingIterator.hasNext()) {
				AbstractCodeMapping mapping = mappingIterator.next();
				UMLOperationBodyMapper mapper = mapperIterator.next();
				if(indicesToBeRemoved.contains(index)) {
					mapper.removeMapping(mapping);
					//remove refactorings based on mapping
					Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<Refactoring>();
					Set<Refactoring> refactoringsAfterPostProcessing = mapper.getRefactoringsAfterPostProcessing();
					for(Refactoring r : refactoringsAfterPostProcessing) {
						if(r instanceof ReferenceBasedRefactoring) {
							ReferenceBasedRefactoring referenceBased = (ReferenceBasedRefactoring)r;
							Set<AbstractCodeMapping> references = referenceBased.getReferences();
							if(references.contains(mapping)) {
								refactoringsToBeRemoved.add(r);
							}
						}
					}
					refactoringsAfterPostProcessing.removeAll(refactoringsToBeRemoved);
					updatedMappers.add(mapper);
				}
				index++;
			}
		}
		Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring refactoring = (ExtractOperationRefactoring)ref;
				if(updatedMappers.contains(refactoring.getBodyMapper())) {
					if(refactoring.getBodyMapper().getMappings().size() == 0) {
						refactoringsToBeRemoved.add(refactoring);
					}
					else {
						refactoring.updateMapperInfo();
					}
				}
			}
			else if(ref instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring refactoring = (InlineOperationRefactoring)ref;
				if(updatedMappers.contains(refactoring.getBodyMapper())) {
					if(refactoring.getBodyMapper().getMappings().size() == 0) {
						refactoringsToBeRemoved.add(refactoring);
					}
					else {
						refactoring.updateMapperInfo();
					}
				}
			}
		}
		refactorings.removeAll(refactoringsToBeRemoved);
	}

	private int matchingParentMappers(List<UMLOperationBodyMapper> parentMappers) {
		int matchingParentMappers = 1;
		for(int i=1; i<parentMappers.size(); i++) {
			if(parentMappers.get(i) != null && parentMappers.get(i).equals(parentMappers.get(i-1))) {
				matchingParentMappers++;
			}
		}
		return matchingParentMappers;
	}

	private void determineIndicesToBeRemoved(List<Boolean> nestedMapper, List<Boolean> identical,
			List<Integer> replacementTypeCount, List<Boolean> replacementCoversEntireStatement,
			Set<Integer> indicesToBeRemoved) {
		if(indicesToBeRemoved.isEmpty()) {
			if(nestedMapper.contains(false)) {
				for(int i=0; i<nestedMapper.size(); i++) {
					if(nestedMapper.get(i) == true) {
						indicesToBeRemoved.add(i);
					}
				}
			}
			if(identical.contains(true)) {
				for(int i=0; i<identical.size(); i++) {
					if(identical.get(i) == false) {
						indicesToBeRemoved.add(i);
					}
				}
			}
			else {
				if(replacementCoversEntireStatement.contains(false)) {
					for(int i=0; i<replacementCoversEntireStatement.size(); i++) {
						if(replacementCoversEntireStatement.get(i) == true) {
							indicesToBeRemoved.add(i);
						}
					}
				}
				int minimum = replacementTypeCount.get(0);
				for(int i=1; i<replacementTypeCount.size(); i++) {
					if(replacementTypeCount.get(i) < minimum) {
						minimum = replacementTypeCount.get(i);
					}
				}
				for(int i=0; i<replacementTypeCount.size(); i++) {
					if(replacementTypeCount.get(i) > minimum) {
						indicesToBeRemoved.add(i);
					}
				}
			}
		}
	}

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() &&
			addedAttributes.isEmpty() && removedAttributes.isEmpty() &&
			addedEnumConstants.isEmpty() && removedEnumConstants.isEmpty() &&
			attributeDiffList.isEmpty() &&
			operationBodyMapperList.isEmpty() && enumConstantDiffList.isEmpty() && annotationListDiff.isEmpty() && typeParameterDiffList.isEmpty() &&
			!visibilityChanged && !abstractionChanged && !finalChanged && !staticChanged && !superclassChanged;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(originalClass.getName()).append(":").append("\n");
		if(visibilityChanged) {
			sb.append("\t").append("visibility changed from " + oldVisibility + " to " + newVisibility).append("\n");
		}
		if(abstractionChanged) {
			sb.append("\t").append("abstraction changed from " + (oldAbstraction ? "abstract" : "concrete") + " to " +
					(newAbstraction ? "abstract" : "concrete")).append("\n");
		}
		Collections.sort(removedOperations);
		for(UMLOperation umlOperation : removedOperations) {
			sb.append("operation " + umlOperation + " removed").append("\n");
		}
		Collections.sort(addedOperations);
		for(UMLOperation umlOperation : addedOperations) {
			sb.append("operation " + umlOperation + " added").append("\n");
		}
		Collections.sort(removedAttributes);
		for(UMLAttribute umlAttribute : removedAttributes) {
			sb.append("attribute " + umlAttribute + " removed").append("\n");
		}
		Collections.sort(addedAttributes);
		for(UMLAttribute umlAttribute : addedAttributes) {
			sb.append("attribute " + umlAttribute + " added").append("\n");
		}
		for(UMLAttributeDiff attributeDiff : attributeDiffList) {
			sb.append(attributeDiff);
		}
		Collections.sort(operationBodyMapperList);
		for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
			sb.append(operationBodyMapper).append("\n");
		}
		return sb.toString();
	}

	public int compareTo(UMLClassBaseDiff other) {
		if(!this.originalClass.getName().equals(other.originalClass.getName()))
			return this.originalClass.getName().compareTo(other.originalClass.getName());
		else
			return this.nextClass.getName().compareTo(other.nextClass.getName());
	}

	public boolean samePackage() {
		return originalClass.getPackageName().equals(nextClass.getPackageName());
	}
}