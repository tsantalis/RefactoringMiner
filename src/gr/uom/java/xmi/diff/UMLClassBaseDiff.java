package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;

public abstract class UMLClassBaseDiff implements Comparable<UMLClassBaseDiff> {

	public static final double MAX_OPERATION_NAME_DISTANCE = 0.4;
	protected UMLClass originalClass;
	protected UMLClass nextClass;
	protected List<UMLOperation> addedOperations;
	protected List<UMLOperation> removedOperations;
	protected List<UMLAttribute> addedAttributes;
	protected List<UMLAttribute> removedAttributes;
	private List<UMLOperationBodyMapper> operationBodyMapperList;
	private boolean visibilityChanged;
	private String oldVisibility;
	private String newVisibility;
	private boolean abstractionChanged;
	private boolean oldAbstraction;
	private boolean newAbstraction;
	private boolean superclassChanged;
	private UMLType oldSuperclass;
	private UMLType newSuperclass;
	private List<UMLType> addedImplementedInterfaces;
	private List<UMLType> removedImplementedInterfaces;
	private List<UMLAnonymousClass> addedAnonymousClasses;
	private List<UMLAnonymousClass> removedAnonymousClasses;
	private List<UMLOperationDiff> operationDiffList;
	protected List<UMLAttributeDiff> attributeDiffList;
	private List<Refactoring> refactorings;
	private Set<MethodInvocationReplacement> consistentMethodInvocationRenames;
	private Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRefactoring>();

	public UMLClassBaseDiff(UMLClass originalClass, UMLClass nextClass) {
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.superclassChanged = false;
		this.addedOperations = new ArrayList<UMLOperation>();
		this.removedOperations = new ArrayList<UMLOperation>();
		this.addedAttributes = new ArrayList<UMLAttribute>();
		this.removedAttributes = new ArrayList<UMLAttribute>();
		this.operationBodyMapperList = new ArrayList<UMLOperationBodyMapper>();
		this.addedImplementedInterfaces = new ArrayList<UMLType>();
		this.removedImplementedInterfaces = new ArrayList<UMLType>();
		this.addedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.removedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.operationDiffList = new ArrayList<UMLOperationDiff>();
		this.attributeDiffList = new ArrayList<UMLAttributeDiff>();
		this.refactorings = new ArrayList<Refactoring>();
	}

	public void process() {
		processInheritance();
		processOperations();
		processAttributes();
		checkForAttributeChanges();
		createBodyMappers();
		processAnonymousClasses();
		checkForOperationSignatureChanges();
		checkForInlinedOperations();
		checkForExtractedOperations();
	}

	protected void checkForAttributeChanges() {
		//optional step
	}

	protected void createBodyMappers() {
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

	protected void processAttributes() {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
    		UMLAttribute attributeWithTheSameName = nextClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(attributeWithTheSameName == null) {
    			this.removedAttributes.add(attribute);
    		}
			else if(!attribute.equals(attributeWithTheSameName) && !attributeDiffListContainsAttribute(attribute, attributeWithTheSameName)) {
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, attributeWithTheSameName);
				this.attributeDiffList.add(attributeDiff);
			}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute attributeWithTheSameName = originalClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(attributeWithTheSameName == null) {
    			this.addedAttributes.add(attribute);
    		}
			else if(!attribute.equals(attributeWithTheSameName) && !attributeDiffListContainsAttribute(attributeWithTheSameName, attribute)) {
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attributeWithTheSameName, attribute);
				this.attributeDiffList.add(attributeDiff);
			}
    	}
	}

	protected void processOperations() {
		for(UMLOperation operation : originalClass.getOperations()) {
    		UMLOperation operationWithTheSameSignature = nextClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.removedOperations.add(operation);
    		}
			else if(!mapperListContainsOperation(operation, operationWithTheSameSignature)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, this);
				mapper.getMappings();
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
				mapper.getMappings();
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
			if(mapper.getOperation1().equals(operation1) || mapper.getOperation2().equals(operation2))
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

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getNextClassName() {
		return nextClass.getName();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getNextClass() {
		return nextClass;
	}

	public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
		return operationBodyMapperList;
	}

	public List<UMLOperation> getAddedOperations() {
		return addedOperations;
	}

	public List<UMLOperation> getRemovedOperations() {
		return removedOperations;
	}

	public List<UMLAttribute> getAddedAttributes() {
		return addedAttributes;
	}

	public List<UMLAttribute> getRemovedAttributes() {
		return removedAttributes;
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

	public void reportAddedAnonymousClass(UMLAnonymousClass umlClass) {
		this.addedAnonymousClasses.add(umlClass);
	}

	public void reportRemovedAnonymousClass(UMLAnonymousClass umlClass) {
		this.removedAnonymousClasses.add(umlClass);
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

	public List<UMLAnonymousClass> getAddedAnonymousClasses() {
		return addedAnonymousClasses;
	}

	public List<UMLAnonymousClass> getRemovedAnonymousClasses() {
		return removedAnonymousClasses;
	}

	public Set<CandidateAttributeRefactoring> getCandidateAttributeRenames() {
		return candidateAttributeRenames;
	}

	public boolean containsOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			if(originalOperation.equalSignature(operation))
				return true;
		}
		return false;
	}

	public UMLOperation containsRemovedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.equalSignature(operation))
				return removedOperation;
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
		if(!originalClass.getVisibility().equals(nextClass.getVisibility())) {
			setVisibilityChanged(true);
			setOldVisibility(originalClass.getVisibility());
			setNewVisibility(nextClass.getVisibility());
		}
		if(!originalClass.isInterface() && !nextClass.isInterface()) {
			if(originalClass.isAbstract() != nextClass.isAbstract()) {
				setAbstractionChanged(true);
				setOldAbstraction(originalClass.isAbstract());
				setNewAbstraction(nextClass.isAbstract());
			}
		}
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

	public void addOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper) {
		this.operationBodyMapperList.add(operationBodyMapper);
	}

	public List<Refactoring> getRefactoringsBeforePostProcessing() {
		return refactorings;
	}

	public List<Refactoring> getRefactorings() {
		List<Refactoring> refactorings = new ArrayList<Refactoring>(this.refactorings);
		Map<Replacement, Set<CandidateAttributeRefactoring>> map = new LinkedHashMap<Replacement, Set<CandidateAttributeRefactoring>>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			refactorings.addAll(mapper.getRefactorings());
			for(CandidateAttributeRefactoring candidate : mapper.getCandidateAttributeRenames()) {
				String before = PrefixSuffixUtils.normalize(candidate.getOriginalVariableName());
				String after = PrefixSuffixUtils.normalize(candidate.getRenamedVariableName());
				Replacement renamePattern = new Replacement(before, after, ReplacementType.VARIABLE_NAME);
				if(map.containsKey(renamePattern)) {
					map.get(renamePattern).add(candidate);
				}
				else {
					Set<CandidateAttributeRefactoring> set = new LinkedHashSet<CandidateAttributeRefactoring>();
					set.add(candidate);
					map.put(renamePattern, set);
				}
			}
		}
		Set<Replacement> renames = map.keySet();
		Set<Replacement> allConsistentRenames = new LinkedHashSet<Replacement>();
		Set<Replacement> allInconsistentRenames = new LinkedHashSet<Replacement>();
		Map<String, Set<String>> aliasedAttributesInOriginalClass = originalClass.aliasedAttributes();
		Map<String, Set<String>> aliasedAttributesInNextClass = nextClass.aliasedAttributes();
		ConsistentReplacementDetector.updateRenames(allConsistentRenames, allInconsistentRenames, renames,
				aliasedAttributesInOriginalClass, aliasedAttributesInNextClass);
		allConsistentRenames.removeAll(allInconsistentRenames);
		for(Replacement pattern : allConsistentRenames) {
			UMLAttribute a1 = findAttributeInOriginalClass(pattern.getBefore());
			UMLAttribute a2 = findAttributeInNextClass(pattern.getAfter());
			Set<CandidateAttributeRefactoring> set = map.get(pattern);
			for(CandidateAttributeRefactoring candidate : set) {
				if(candidate.getOriginalVariableDeclaration() == null && candidate.getRenamedVariableDeclaration() == null) {
					if(a1 != null && a2 != null) {
						if((!originalClass.containsAttributeWithName(pattern.getAfter()) || cyclicRename(map, pattern)) &&
								(!nextClass.containsAttributeWithName(pattern.getBefore()) || cyclicRename(map, pattern)) &&
								!inconsistentAttributeRename(pattern, aliasedAttributesInOriginalClass, aliasedAttributesInNextClass)) {
							RenameAttributeRefactoring ref = new RenameAttributeRefactoring(a1.getVariableDeclaration(), a2.getVariableDeclaration(),
									getOriginalClassName(), getNextClassName(), set);
							if(!refactorings.contains(ref)) {
								refactorings.add(ref);
								break;//it's not necessary to repeat the same process for all candidates in the set
							}
						}
					}
					else {
						candidate.setOriginalAttribute(a1);
						candidate.setRenamedAttribute(a2);
						if(a1 != null)
							candidate.setOriginalVariableDeclaration(a1.getVariableDeclaration());
						if(a2 != null)
							candidate.setRenamedVariableDeclaration(a2.getVariableDeclaration());
						candidateAttributeRenames.add(candidate);
					}
				}
				else if(candidate.getOriginalVariableDeclaration() != null) {
					if(a2 != null) {
						RenameVariableRefactoring ref = new RenameVariableRefactoring(
								candidate.getOriginalVariableDeclaration(), a2.getVariableDeclaration(),
								candidate.getOperationBefore(), candidate.getOperationAfter());
						if(!refactorings.contains(ref)) {
							refactorings.add(ref);
						}
					}
					else {
						//field is declared in a superclass or outer class
						candidateAttributeRenames.add(candidate);
					}
				}
				else if(candidate.getRenamedVariableDeclaration() != null) {
					//inline field
				}
			}
		}
		return refactorings;
	}

	public UMLAttribute findAttributeInOriginalClass(String attributeName) {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			if(attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		return null;
	}

	public UMLAttribute findAttributeInNextClass(String attributeName) {
		for(UMLAttribute attribute : nextClass.getAttributes()) {
			if(attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		return null;
	}

	private boolean inconsistentAttributeRename(Replacement pattern,
			Map<String, Set<String>> aliasedAttributesInOriginalClass,
			Map<String, Set<String>> aliasedAttributesInNextClass) {
		for(String key : aliasedAttributesInOriginalClass.keySet()) {
			if(aliasedAttributesInOriginalClass.get(key).contains(pattern.getBefore())) {
				return false;
			}
		}
		for(String key : aliasedAttributesInNextClass.keySet()) {
			if(aliasedAttributesInNextClass.get(key).contains(pattern.getAfter())) {
				return false;
			}
		}
		int counter = 0;
		int allCases = 0;
		for(UMLOperationBodyMapper mapper : this.operationBodyMapperList) {
			List<String> allVariables1 = mapper.getOperation1().getAllVariables();
			List<String> allVariables2 = mapper.getOperation2().getAllVariables();
			boolean variables1contains = (allVariables1.contains(pattern.getBefore()) &&
					!mapper.getOperation1().getParameterNameList().contains(pattern.getBefore())) ||
					allVariables1.contains("this."+pattern.getBefore());
			boolean variables2Contains = (allVariables2.contains(pattern.getAfter()) &&
					!mapper.getOperation2().getParameterNameList().contains(pattern.getAfter())) ||
					allVariables2.contains("this."+pattern.getAfter());
			if(variables1contains && !variables2Contains) {	
				counter++;
			}
			if(variables2Contains && !variables1contains) {
				counter++;
			}
			if(variables1contains || variables2Contains) {
				allCases++;
			}
		}
		double percentage = (double)counter/(double)allCases;
		if(percentage > 0.5)
			return true;
		return false;
	}

	private static boolean cyclicRename(Map<Replacement, Set<CandidateAttributeRefactoring>> renames, Replacement rename) {
		for(Replacement r : renames.keySet()) {
			if((rename.getAfter().equals(r.getBefore()) || rename.getBefore().equals(r.getAfter())) &&
					(totalOccurrences(renames.get(rename)) > 1 || totalOccurrences(renames.get(r)) > 1))
			return true;
		}
		return false;
	}

	private static int totalOccurrences(Set<CandidateAttributeRefactoring> candidates) {
		int totalCount = 0;
		for(CandidateAttributeRefactoring candidate : candidates) {
			totalCount += candidate.getOccurrences();
		}
		return totalCount;
	}

	private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation, UMLOperation addedOperation) {
		int index1 = originalClass.getOperations().indexOf(removedOperation);
		int index2 = nextClass.getOperations().indexOf(addedOperation);
		return Math.abs(index1-index2);
	}

	private void checkForOperationSignatureChanges() {
		consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
		if(removedOperations.size() <= addedOperations.size()) {
			for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
				UMLOperation removedOperation = removedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
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
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					if(bestMapper != null) {
						removedOperation = bestMapper.getOperation1();
						UMLOperation addedOperation = bestMapper.getOperation2();
						addedOperations.remove(addedOperation);
						removedOperationIterator.remove();
	
						UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
						operationDiffList.add(operationDiff);
						if(!removedOperation.getName().equals(addedOperation.getName()) &&
								!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
							refactorings.add(rename);
						}
						this.addOperationBodyMapper(bestMapper);
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
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					if(bestMapper != null) {
						UMLOperation removedOperation = bestMapper.getOperation1();
						addedOperation = bestMapper.getOperation2();
						removedOperations.remove(removedOperation);
						addedOperationIterator.remove();
	
						UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
						operationDiffList.add(operationDiff);
						if(!removedOperation.getName().equals(addedOperation.getName()) &&
								!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
							refactorings.add(rename);
						}
						this.addOperationBodyMapper(bestMapper);
					}
				}
			}
		}
	}

	private Set<MethodInvocationReplacement> findConsistentMethodInvocationRenames() {
		Set<MethodInvocationReplacement> allConsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		Set<MethodInvocationReplacement> allInconsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		for(UMLOperationBodyMapper bodyMapper : operationBodyMapperList) {
			Set<MethodInvocationReplacement> methodInvocationRenames = bodyMapper.getMethodInvocationRenameReplacements();
			ConsistentReplacementDetector.updateRenames(allConsistentMethodInvocationRenames, allInconsistentMethodInvocationRenames,
					methodInvocationRenames);
		}
		allConsistentMethodInvocationRenames.removeAll(allInconsistentMethodInvocationRenames);
		return allConsistentMethodInvocationRenames;
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
		List<AbstractCodeMapping> totalMappings = operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0 && allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodInlined(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
		else {
			for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames) {
				if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation) &&
						replacement.getInvokedOperationAfter().matchesOperation(addedOperation)) {
					mapperSet.add(operationBodyMapper);
					break;
				}
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

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation operationInsideAnonymousClass, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, operationInsideAnonymousClass, this);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0 && allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodInlined(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		return (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
				(nonMappedElementsT1 == 0 && mappings > Math.floor(nonMappedElementsT2/2.0)) ||
				(mappings == 1 && nonMappedElementsT1 + nonMappedElementsT2 == 1 && operationBodyMapper.getOperation1().getName().equals(operationBodyMapper.getOperation2().getName()));
	}

	private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
		int nonMappedElementsT2WithoutThoseCallingAddedOperation = nonMappedElementsT2 - nonMappedElementsT2CallingAddedOperation;
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
				nonMappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation);
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
		UMLOperation bestMapperOperation1 = bestMapper.getOperation1();
		UMLOperation bestMapperOperation2 = bestMapper.getOperation2();
		if(bestMapperOperation1.equalReturnParameter(bestMapperOperation2) &&
				bestMapperOperation1.getName().equals(bestMapperOperation2.getName()) &&
				bestMapperOperation1.commonParameterTypes(bestMapperOperation2).size() > 0) {
			return bestMapper;
		}
		for(int i=1; i<mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			UMLOperation operation2 = mapper.getOperation2();
			Set<OperationInvocation> operationInvocations2 = operation2.getAllOperationInvocations();
			boolean anotherMapperCallsOperation2OfTheBestMapper = false;
			for(OperationInvocation invocation : operationInvocations2) {
				if(invocation.matchesOperation(bestMapper.getOperation2(), operation2.variableTypeMap()) && !invocation.matchesOperation(bestMapper.getOperation1(), operation2.variableTypeMap()) &&
						!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, removedOperations)) {
					anotherMapperCallsOperation2OfTheBestMapper = true;
					break;
				}
			}
			UMLOperation operation1 = mapper.getOperation1();
			Set<OperationInvocation> operationInvocations1 = operation1.getAllOperationInvocations();
			boolean anotherMapperCallsOperation1OfTheBestMapper = false;
			for(OperationInvocation invocation : operationInvocations1) {
				if(invocation.matchesOperation(bestMapper.getOperation1(), operation1.variableTypeMap()) && !invocation.matchesOperation(bestMapper.getOperation2(), operation1.variableTypeMap()) &&
						!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, addedOperations)) {
					anotherMapperCallsOperation1OfTheBestMapper = true;
					break;
				}
			}
			boolean nextMapperMatchesConsistentRename = matchesConsistentMethodInvocationRename(mapper, consistentMethodInvocationRenames);
			boolean bestMapperMismatchesConsistentRename = mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames);
			if(bestMapperMismatchesConsistentRename && nextMapperMatchesConsistentRename) {
				bestMapper = mapper;
				break;
			}
			if(anotherMapperCallsOperation2OfTheBestMapper || anotherMapperCallsOperation1OfTheBestMapper) {
				bestMapper = mapper;
				break;
			}
		}
		if(mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames)) {
			return null;
		}
		return bestMapper;
	}

	private boolean matchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper, Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
		for(MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
			if(mapper.getOperation1().getName().equals(rename.getBefore()) && mapper.getOperation2().getName().equals(rename.getAfter())) {
				return true;
			}
		}
		return false;
	}

	private boolean mismatchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper, Set<MethodInvocationReplacement> consistentMethodInvocationRenames) {
		for(MethodInvocationReplacement rename : consistentMethodInvocationRenames) {
			if(mapper.getOperation1().getName().equals(rename.getBefore()) && !mapper.getOperation2().getName().equals(rename.getAfter())) {
				return true;
			}
			else if(!mapper.getOperation1().getName().equals(rename.getBefore()) && mapper.getOperation2().getName().equals(rename.getAfter())) {
				return true;
			}
		}
		return false;
	}

	private boolean operationContainsMethodInvocationWithTheSameNameAndCommonArguments(OperationInvocation invocation, List<UMLOperation> operations) {
		for(UMLOperation operation : operations) {
			Set<OperationInvocation> operationInvocations = operation.getAllOperationInvocations();
			for(OperationInvocation operationInvocation : operationInvocations) {
				Set<String> argumentIntersection = new LinkedHashSet<String>(operationInvocation.getArguments());
				argumentIntersection.retainAll(invocation.getArguments());
				if(operationInvocation.getMethodName().equals(invocation.getMethodName()) && !argumentIntersection.isEmpty()) {
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
		List<StatementObject> nonMappedLeavesT1 = operationBodyMapper.getNonMappedLeavesT1();
		List<StatementObject> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		if(nonMappedLeavesT1.size() == 1 && nonMappedLeavesT2.size() == 1) {
			StatementObject statementT2 = nonMappedLeavesT2.get(0);
			OperationInvocation invocationT2 = statementT2.invocationCoveringEntireFragment();
			if(invocationT2 != null) {
				for(UMLOperation addedOperation : addedOperations) {
					if(invocationT2.matchesOperation(addedOperation, operationBodyMapper.getOperation2().variableTypeMap())) {
						StatementObject statementT1 = nonMappedLeavesT1.get(0);
						OperationInvocation invocationT1 = statementT1.invocationCoveringEntireFragment();
						if(invocationT1 != null && addedOperation.getAllOperationInvocations().contains(invocationT1)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = removedOperationInvocations.size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation, addedOperation.variableTypeMap())) {
							//addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);
		
		Set<OperationInvocation> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<OperationInvocation> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			OperationInvocation invocation = operationInvocationIterator.next();
			if(invocation.getMethodName().startsWith("get")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
				numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
	}

	private boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromAddedOperation = addedOperationInvocations.size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation removedOperationInvocation : removedOperationInvocations) {
			if(!intersection.contains(removedOperationInvocation)) {
				for(UMLOperation operation : removedOperations) {
					if(!operation.equals(removedOperation) && operation.getBody() != null) {
						if(removedOperationInvocation.matchesOperation(operation, removedOperation.variableTypeMap())) {
							//removedOperation calls another removed method
							operationInvocationsInMethodsCalledByRemovedOperation.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(addedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);
		
		int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations = numberOfInvocationsMissingFromAddedOperation - numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
		return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations;
	}

	private boolean allMappingsAreExactMatches(UMLOperationBodyMapper operationBodyMapper, int mappings) {
		if(mappings == operationBodyMapper.exactMatches()) {
			return true;
		}
		int mappingsWithTypeReplacement = 0;
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.containsReplacement(ReplacementType.TYPE)) {
				mappingsWithTypeReplacement++;
			}
		}
		if(mappings == operationBodyMapper.exactMatches() + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement) {
			return true;
		}
		return false;
	}

	private boolean compatibleSignatures(UMLOperation removedOperation, UMLOperation addedOperation, int absoluteDifferenceInPosition) {
		return addedOperation.compatibleSignature(removedOperation) ||
		(
		(absoluteDifferenceInPosition == 0 || operationsBeforeAndAfterMatch(removedOperation, addedOperation)) &&
		(addedOperation.getParameterTypeList().equals(removedOperation.getParameterTypeList()) || addedOperation.normalizedNameDistance(removedOperation) <= MAX_OPERATION_NAME_DISTANCE)
		);
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
			operationsBeforeMatch = operationBefore1.equalParameterTypes(operationBefore2) && operationBefore1.getName().equals(operationBefore2.getName());
		}
		
		boolean operationsAfterMatch = false;
		if(operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = operationAfter1.equalParameterTypes(operationAfter2) && operationAfter1.getName().equals(operationAfter2.getName());
		}
		
		return operationsBeforeMatch || operationsAfterMatch;
	}

	private void checkForInlinedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if(!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() ||
					!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation1().getAllOperationInvocations();
					OperationInvocation removedOperationInvocation = matchingInvocation(removedOperation, operationInvocations, mapper.getOperation1().variableTypeMap());
					if(removedOperationInvocation != null && !invocationMatchesWithAddedOperation(removedOperationInvocation, mapper.getOperation1().variableTypeMap(), mapper.getOperation2().getAllOperationInvocations())) {
						List<String> arguments = removedOperationInvocation.getArguments();
						List<String> parameters = removedOperation.getParameterNameList();
						Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
						//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
						int size = Math.min(arguments.size(), parameters.size());
						for(int i=0; i<size; i++) {
							parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
						}
						UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap);
						if(inlineMatchCondition(operationBodyMapper)) {
							InlineOperationRefactoring inlineOperationRefactoring =	new InlineOperationRefactoring(operationBodyMapper, mapper.getOperation1(), removedOperationInvocation);
							refactorings.add(inlineOperationRefactoring);
							refactorings.addAll(operationBodyMapper.getRefactorings());
							mapper.addAdditionalMapper(operationBodyMapper);
							operationsToBeRemoved.add(removedOperation);
						}
					}
				}
			}
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}

	private boolean inlineMatchCondition(UMLOperationBodyMapper operationBodyMapper) {
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
		int exactMatches = exactMatchList.size();
		return mappings > 0 && (mappings > nonMappedElementsT1 ||
				(exactMatches == 1 && !exactMatchList.get(0).getFragment1().throwsNewException() && nonMappedElementsT1-exactMatches < 10) ||
				(exactMatches > 1 && nonMappedElementsT1-exactMatches < 20));
	}

	private boolean invocationMatchesWithAddedOperation(OperationInvocation removedOperationInvocation, Map<String, UMLType> variableTypeMap, Set<OperationInvocation> operationInvocationsInNewMethod) {
		if(operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperationInvocation.matchesOperation(addedOperation, variableTypeMap)) {
					return true;
				}
			}
		}
		return false;
	}

	private void checkForExtractedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
			UMLOperation addedOperation = addedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
					!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation2().getAllOperationInvocations();
					OperationInvocation addedOperationInvocation = matchingInvocation(addedOperation, operationInvocations, mapper.getOperation2().variableTypeMap());
					if(addedOperationInvocation != null) {
						CallTreeNode root = new CallTreeNode(mapper.getOperation1(), addedOperation, addedOperationInvocation);
						CallTree callTree = new CallTree(root);
						generateCallTree(addedOperation, root, callTree);
						UMLOperationBodyMapper operationBodyMapper = createMapperForExtractedMethod(mapper, mapper.getOperation1(), addedOperation, addedOperationInvocation);
						if(operationBodyMapper != null) {
							List<AbstractCodeMapping> additionalExactMatches = new ArrayList<AbstractCodeMapping>();
							List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
							for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
								CallTreeNode node = nodesInBreadthFirstOrder.get(i);
								if(matchingInvocation(node.getInvokedOperation(), operationInvocations, mapper.getOperation2().variableTypeMap()) == null) {
									UMLOperationBodyMapper nestedMapper = createMapperForExtractedMethod(mapper, node.getOriginalOperation(), node.getInvokedOperation(), node.getInvocation());
									if(nestedMapper != null) {
										additionalExactMatches.addAll(nestedMapper.getExactMatches());
									}
								}
							}
							UMLOperation delegateMethod = findDelegateMethod(mapper.getOperation1(), addedOperation, addedOperationInvocation);
							if(extractMatchCondition(operationBodyMapper, additionalExactMatches)) {
								ExtractOperationRefactoring extractOperationRefactoring = null;
								if(delegateMethod == null) {
									extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, mapper.getOperation2(), addedOperationInvocation);
								}
								else {
									extractOperationRefactoring = new ExtractOperationRefactoring(operationBodyMapper, addedOperation,
											mapper.getOperation1(), mapper.getOperation2(), addedOperationInvocation);
								}
								refactorings.add(extractOperationRefactoring);
								refactorings.addAll(operationBodyMapper.getRefactorings());
								mapper.addAdditionalMapper(operationBodyMapper);
								operationsToBeRemoved.add(addedOperation);
							}
						}
					}
				}
			}
		}
		addedOperations.removeAll(operationsToBeRemoved);
	}

	private OperationInvocation matchingInvocation(UMLOperation addedOperation,
			Set<OperationInvocation> operationInvocations, Map<String, UMLType> variableTypeMap) {
		OperationInvocation addedOperationInvocation = null;
		for(OperationInvocation invocation : operationInvocations) {
			if(invocation.matchesOperation(addedOperation, variableTypeMap)) {
				addedOperationInvocation = invocation;
				break;
			}
		}
		return addedOperationInvocation;
	}

	private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
		Set<OperationInvocation> invocations = operation.getAllOperationInvocations();
		for(UMLOperation addedOperation : addedOperations) {
			for(OperationInvocation invocation : invocations) {
				if(invocation.matchesOperation(addedOperation, operation.variableTypeMap())) {
					if(!callTree.contains(addedOperation)) {
						CallTreeNode node = new CallTreeNode(operation, addedOperation, invocation);
						parent.addChild(node);
						generateCallTree(addedOperation, node, callTree);
					}
				}
			}
		}
	}

	private UMLOperationBodyMapper createMapperForExtractedMethod(UMLOperationBodyMapper mapper,
			UMLOperation originalOperation, UMLOperation addedOperation, OperationInvocation addedOperationInvocation) {
		List<UMLParameter> originalMethodParameters = originalOperation.getParametersWithoutReturnType();
		Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters = new LinkedHashMap<UMLParameter, UMLParameter>();
		List<String> arguments = addedOperationInvocation.getArguments();
		List<UMLParameter> parameters = addedOperation.getParametersWithoutReturnType();
		Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
		//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
		int size = Math.min(arguments.size(), parameters.size());
		for(int i=0; i<size; i++) {
			String argumentName = arguments.get(i);
			String parameterName = parameters.get(i).getName();
			parameterToArgumentMap.put(parameterName, argumentName);
			for(UMLParameter originalMethodParameter : originalMethodParameters) {
				if(originalMethodParameter.getName().equals(argumentName)) {
					originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.put(originalMethodParameter, parameters.get(i));
				}
			}
		}
		if(parameterTypesMatch(originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters)) {
			UMLOperation delegateMethod = findDelegateMethod(originalOperation, addedOperation, addedOperationInvocation);
			return new UMLOperationBodyMapper(mapper,
					delegateMethod != null ? delegateMethod : addedOperation,
					new LinkedHashMap<String, String>(), parameterToArgumentMap);
		}
		return null;
	}

	private boolean extractMatchCondition(UMLOperationBodyMapper operationBodyMapper, List<AbstractCodeMapping> additionalExactMatches) {
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		List<AbstractCodeMapping> exactMatchList = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getExactMatches());
		boolean exceptionHandlingExactMatch = false;
		boolean throwsNewExceptionExactMatch = false;
		if(exactMatchList.size() == 1) {
			AbstractCodeMapping mapping = exactMatchList.get(0);
			if(mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
				StatementObject statement1 = (StatementObject)mapping.getFragment1();
				StatementObject statement2 = (StatementObject)mapping.getFragment2();
				if(statement1.getParent().getString().startsWith("catch(") &&
						statement2.getParent().getString().startsWith("catch(")) {
					exceptionHandlingExactMatch = true;
				}
			}
			if(mapping.getFragment1().throwsNewException() && mapping.getFragment2().throwsNewException()) {
				throwsNewExceptionExactMatch = true;
			}
		}
		exactMatchList.addAll(additionalExactMatches);
		int exactMatches = exactMatchList.size();
		return mappings > 0 && (mappings > nonMappedElementsT2 ||
				(exactMatches >= mappings && nonMappedElementsT1 == 0) ||
				(exactMatches == 1 && !throwsNewExceptionExactMatch && nonMappedElementsT2-exactMatches < 10) ||
				(!exceptionHandlingExactMatch && exactMatches > 1 && additionalExactMatches.size() < exactMatches && nonMappedElementsT2-exactMatches < 20) ||
				(mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2())) ||
				argumentExtractedWithDefaultReturnAdded(operationBodyMapper);
	}

	private boolean argumentExtractedWithDefaultReturnAdded(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeMapping> totalMappings = operationBodyMapper.getMappings();
		List<CompositeStatementObject> nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>(operationBodyMapper.getNonMappedInnerNodesT2());
		ListIterator<CompositeStatementObject> iterator = nonMappedInnerNodesT2.listIterator();
		while(iterator.hasNext()) {
			if(iterator.next().toString().equals("{")) {
				iterator.remove();
			}
		}
		List<StatementObject> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		return totalMappings.size() == 1 && totalMappings.get(0).containsReplacement(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) &&
				nonMappedInnerNodesT2.size() == 1 && nonMappedInnerNodesT2.get(0).toString().startsWith("if") &&
				nonMappedLeavesT2.size() == 1 && nonMappedLeavesT2.get(0).toString().startsWith("return ");
	}

	private UMLOperation findDelegateMethod(UMLOperation originalOperation, UMLOperation addedOperation, OperationInvocation addedOperationInvocation) {
		OperationInvocation delegateMethodInvocation = addedOperation.isDelegate();
		if(originalOperation.isDelegate() == null && delegateMethodInvocation != null && !originalOperation.getAllOperationInvocations().contains(addedOperationInvocation)) {
			for(UMLOperation operation : addedOperations) {
				if(delegateMethodInvocation.matchesOperation(operation, addedOperation.variableTypeMap())) {
					return operation;
				}
			}
		}
		return null;
	}

	private boolean parameterTypesMatch(Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
		for(UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
			UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
			if(!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType())) {
				return false;
			}
		}
		return true;
	}

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() &&
			addedAttributes.isEmpty() && removedAttributes.isEmpty() &&
			operationDiffList.isEmpty() && attributeDiffList.isEmpty() &&
			operationBodyMapperList.isEmpty() &&
			!visibilityChanged && !abstractionChanged;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
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
		for(UMLOperationDiff operationDiff : operationDiffList) {
			sb.append(operationDiff);
		}
		for(UMLAttributeDiff attributeDiff : attributeDiffList) {
			sb.append(attributeDiff);
		}
		Collections.sort(operationBodyMapperList);
		for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
			sb.append(operationBodyMapper);
		}
		return sb.toString();
	}

	public int compareTo(UMLClassBaseDiff other) {
		return this.originalClass.getName().compareTo(other.originalClass.getName());
	}

	private class CallTree {
		private CallTreeNode root;
		
		public CallTree(CallTreeNode root) {
			this.root = root;
		}
		
		public List<CallTreeNode> getNodesInBreadthFirstOrder() {
			List<CallTreeNode> nodes = new ArrayList<CallTreeNode>();
			List<CallTreeNode> queue = new LinkedList<CallTreeNode>();
			nodes.add(root);
			queue.add(root);
			while(!queue.isEmpty()) {
				CallTreeNode node = queue.remove(0);
				nodes.addAll(node.children);
				queue.addAll(node.children);
			}
			return nodes;
		}
		
		public boolean contains(UMLOperation invokedOperation) {
			for(CallTreeNode node : getNodesInBreadthFirstOrder()) {
				if(node.getInvokedOperation().equals(invokedOperation)) {
					return true;
				}
			}
			return false;
		}
	}

	private class CallTreeNode {
		private UMLOperation originalOperation;
		private UMLOperation invokedOperation;
		private OperationInvocation invocation;
		private List<CallTreeNode> children = new ArrayList<CallTreeNode>();
		
		public CallTreeNode(UMLOperation originalOperation, UMLOperation invokedOperation,
				OperationInvocation invocation) {
			this.originalOperation = originalOperation;
			this.invokedOperation = invokedOperation;
			this.invocation = invocation;
		}

		public UMLOperation getOriginalOperation() {
			return originalOperation;
		}

		public UMLOperation getInvokedOperation() {
			return invokedOperation;
		}

		public OperationInvocation getInvocation() {
			return invocation;
		}

		public void addChild(CallTreeNode node) {
			children.add(node);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((invocation == null) ? 0 : invocation.hashCode());
			result = prime * result + ((invokedOperation == null) ? 0 : invokedOperation.hashCode());
			result = prime * result + ((originalOperation == null) ? 0 : originalOperation.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CallTreeNode other = (CallTreeNode) obj;
			if (invocation == null) {
				if (other.invocation != null)
					return false;
			} else if (!invocation.equals(other.invocation))
				return false;
			if (invokedOperation == null) {
				if (other.invokedOperation != null)
					return false;
			} else if (!invokedOperation.equals(other.invokedOperation))
				return false;
			if (originalOperation == null) {
				if (other.originalOperation != null)
					return false;
			} else if (!originalOperation.equals(other.originalOperation))
				return false;
			return true;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(invokedOperation);
			sb.append(" called from ");
			sb.append(originalOperation);
			return sb.toString();
		}
	}

	public boolean containsExtractOperationRefactoring(UMLOperation sourceOperationBeforeExtraction, UMLOperation extractedOperation) {
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring)ref;
				if(extractRef.getSourceOperationBeforeExtraction().equals(sourceOperationBeforeExtraction) &&
						extractRef.getExtractedOperation().equalSignature(extractedOperation)) {
					return true;
				}
			}
		}
		return false;
	}
}