package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public abstract class UMLClassBaseDiff {

	protected UMLClass originalClass;
	protected UMLClass nextClass;
	protected List<UMLOperation> addedOperations;
	protected List<UMLOperation> removedOperations;
	protected List<UMLAttribute> addedAttributes;
	protected List<UMLAttribute> removedAttributes;
	protected List<UMLOperationBodyMapper> operationBodyMapperList;
	protected boolean visibilityChanged;
	protected String oldVisibility;
	protected String newVisibility;
	protected boolean abstractionChanged;
	protected boolean oldAbstraction;
	protected boolean newAbstraction;
	protected boolean superclassChanged;
	private UMLType oldSuperclass;
	private UMLType newSuperclass;
	protected List<UMLType> addedImplementedInterfaces;
	protected List<UMLType> removedImplementedInterfaces;

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
		processInheritance();
	}

	protected void processAttributes() {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
    		if(!nextClass.containsAttributeIgnoringChangedType(attribute)) {
    			this.removedAttributes.add(attribute);
    		}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		if(!originalClass.containsAttributeIgnoringChangedType(attribute)) {
    			this.addedAttributes.add(attribute);
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
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature);
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
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operationWithTheSameSignature, operation);
				mapper.getMappings();
				this.operationBodyMapperList.add(mapper);
			}
    	}
	}

	private boolean mapperListContainsOperation(UMLOperation operation, UMLOperation operationWithTheSameSignature) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getOperation1().equals(operation) || mapper.getOperation2().equals(operationWithTheSameSignature))
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
	public boolean isInnerClassMove(UMLClassMoveDiff classMoveDiff) {
		if(this.originalClass.isInnerClass(classMoveDiff.originalClass) && this.nextClass.isInnerClass(classMoveDiff.nextClass))
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
}