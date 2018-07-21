package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	public boolean containsOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignature(operation))
				return true;
		}
		return false;
	}

	public UMLOperation operationWithTheSameSignatureIgnoringChangedTypes(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignatureIgnoringChangedTypes(operation))
				return originalOperation;
		}
		return null;
	}

	public boolean containsOperationWithTheSameSignatureIgnoringChangedTypes(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignatureIgnoringChangedTypes(operation))
				return true;
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

	public boolean hasCommonAttributesAndOperations(UMLAbstractClass umlClass) {
		Set<UMLOperation> commonOperations = new LinkedHashSet<UMLOperation>();
		int totalOperations = 0;
		for(UMLOperation operation : operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
	    		if(umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
	    			commonOperations.add(operation);
	    		}
			}
		}
		for(UMLOperation operation : umlClass.operations) {
			if(!operation.isConstructor() && !operation.overridesObject()) {
				totalOperations++;
	    		if(this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
	    			commonOperations.add(operation);
	    		}
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
		if(this.isTestClass() && umlClass.isTestClass()) {
			return commonOperations.size() > Math.floor(totalOperations/2.0) || commonOperations.containsAll(this.operations);
		}
		return (commonOperations.size() > Math.floor(totalOperations/2.0) && commonAttributes.size() > 2) ||
				(commonAttributes.size() > Math.floor(totalAttributes/2.0) && commonOperations.size() > 2) ||
				(commonOperations.size() == totalOperations && commonOperations.size() > 2 && this.attributes.size() == umlClass.attributes.size());
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

	public String getSourceFile() {
		return locationInfo.getFilePath();
	}
}
