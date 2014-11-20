package gr.uom.java.xmi;

import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLClassDiff;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class UMLClass implements Comparable<UMLClass>, Serializable {
	private String packageName;
    private String name;
    private String qualifiedName;
    private String xmiID;
    private String visibility;
	private boolean isAbstract;
	private boolean isInterface;
    private List<UMLOperation> operations;
    private List<UMLAttribute> attributes;
    private UMLType superclass;

    public UMLClass(String packageName, String name, String xmiID) {
    	this.packageName = packageName;
        this.name = name;
        if(packageName.equals(""))
        	this.qualifiedName = name;
    	else
    		this.qualifiedName = packageName + "." + name;
        this.xmiID = xmiID;
        this.isAbstract = false;
        this.isInterface = false;
        this.operations = new ArrayList<UMLOperation>();
        this.attributes = new ArrayList<UMLAttribute>();
        this.superclass = null;
    }

    public String getName() {
    	return this.qualifiedName;
    }

    //returns true if the "innerClass" parameter is inner class of this
    public boolean isInnerClass(UMLClass innerClass) {
    	if(this.getName().equals(innerClass.packageName))
    		return true;
    	return false;
    }

    public String getXmiID() {
        return xmiID;
    }

	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

    public UMLType getSuperclass() {
		return superclass;
	}

	public void setSuperclass(UMLType superclass) {
		this.superclass = superclass;
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

	public UMLOperation getOperation(String xmiID) {
    	ListIterator<UMLOperation> operationIt = operations.listIterator();
    	while(operationIt.hasNext()) {
    		UMLOperation operation = operationIt.next();
    		if(operation.getXmiID().equals(xmiID))
    			return operation;
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

	public boolean containsOperationWithTheSameSignatureIgnoringChangedTypes(UMLOperation operation) {
		for(UMLOperation originalOperation : operations) {
			if(originalOperation.equalSignatureIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public boolean containsAttributeIgnoringChangedType(UMLAttribute attribute) {
		for(UMLAttribute originalAttribute : attributes) {
			if(originalAttribute.equalsIgnoringChangedType(attribute))
				return true;
		}
		return false;
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

    public UMLOperation matchOperation(UMLOperation otherOperation) {
    	ListIterator<UMLOperation> operationIt = operations.listIterator();
    	while(operationIt.hasNext()) {
    		UMLOperation operation = operationIt.next();
    		if(operation.getName().equals(otherOperation.getName())) {
    			if(operation.getParameters().size() == otherOperation.getParameters().size()) {
    				boolean match = true;
    				int i = 0;
    				for(UMLParameter parameter : operation.getParameters()) {
    					UMLParameter otherParameter = otherOperation.getParameters().get(i);
    					String thisParameterType = parameter.getType().getClassType();
    					String otherParameterType = otherParameter.getType().getClassType();
    					int thisArrayDimension = parameter.getType().getArrayDimension();
    					int otherArrayDimension = otherParameter.getType().getArrayDimension();
    					String thisParameterTypeComparedString = null;
    	    			if(thisParameterType.contains("."))
    	    				thisParameterTypeComparedString = thisParameterType.substring(thisParameterType.lastIndexOf(".")+1);
    	    			else
    	    				thisParameterTypeComparedString = thisParameterType;
    	    			String otherParameterTypeComparedString = null;
    	    			if(otherParameterType.contains("."))
    	    				otherParameterTypeComparedString = otherParameterType.substring(otherParameterType.lastIndexOf(".")+1);
    	    			else
    	    				otherParameterTypeComparedString = otherParameterType;
    	    			if(!thisParameterTypeComparedString.equals(otherParameterTypeComparedString) || thisArrayDimension != otherArrayDimension) {
    						match = false;
    						break;
    					}
    					i++;
    				}
    				if(match)
    					return operation;
    			}
    		}
    	}
    	return null;
    }

    public boolean isMoved(UMLClass umlClass) {
    	if(!this.name.equals(umlClass.name))
    		return false;
    	if(this.isAbstract != umlClass.isAbstract)
    		return false;
    	if(this.isInterface != umlClass.isInterface)
    		return false;
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
    		if(!umlClass.containsAttributeIgnoringChangedType(attribute)) {
    			return false;
    		}
    	}
    	for(UMLAttribute attribute : umlClass.attributes) {
    		if(!this.containsAttributeIgnoringChangedType(attribute)) {
    			return false;
    		}
    	}
    	return true;
    }

    public boolean isRenamed(UMLClass umlClass) {
    	if(this.isAbstract != umlClass.isAbstract)
    		return false;
    	if(this.isInterface != umlClass.isInterface)
    		return false;
    	if(this.attributes.size() != umlClass.attributes.size())
    		return false;
    	if(this.operations.size() != umlClass.operations.size())
    		return false;
    	if(this.operations.size() == 0 && this.attributes.size() == 0)
    		return false;
    	for(UMLOperation operation : operations) {
    		if(!operation.isConstructor() && !umlClass.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
    			return false;
    		}
    	}
    	for(UMLOperation operation : umlClass.operations) {
    		if(!operation.isConstructor() && !this.containsOperationWithTheSameSignatureIgnoringChangedTypes(operation)) {
    			return false;
    		}
    	}
    	for(UMLAttribute attribute : attributes) {
    		if(!umlClass.containsAttributeIgnoringChangedType(attribute)) {
    			return false;
    		}
    	}
    	for(UMLAttribute attribute : umlClass.attributes) {
    		if(!this.containsAttributeIgnoringChangedType(attribute)) {
    			return false;
    		}
    	}
    	return true;
    }

    public UMLClassDiff diff(UMLClass umlClass) {
    	UMLClassDiff classDiff = new UMLClassDiff(this);
    	if(!this.visibility.equals(umlClass.visibility)) {
    		classDiff.setVisibilityChanged(true);
    		classDiff.setOldVisibility(this.visibility);
    		classDiff.setNewVisibility(umlClass.visibility);
    	}
    	if(!this.isInterface && !umlClass.isInterface) {
    		if(this.isAbstract != umlClass.isAbstract) {
    			classDiff.setAbstractionChanged(true);
    			classDiff.setOldAbstraction(this.isAbstract);
    			classDiff.setNewAbstraction(umlClass.isAbstract);
    		}
    	}
    	if(this.superclass != null && umlClass.superclass != null) {
    		if(!this.superclass.equals(umlClass.superclass)) {
    			classDiff.setSuperclassChanged(true);
    		}
    		classDiff.setOldSuperclass(this.superclass);
    		classDiff.setNewSuperclass(umlClass.superclass);
    	}
    	else if(this.superclass != null && umlClass.superclass == null) {
    		classDiff.setSuperclassChanged(true);
    		classDiff.setOldSuperclass(this.superclass);
    		classDiff.setNewSuperclass(umlClass.superclass);
    	}
    	else if(this.superclass == null && umlClass.superclass != null) {
    		classDiff.setSuperclassChanged(true);
    		classDiff.setOldSuperclass(this.superclass);
    		classDiff.setNewSuperclass(umlClass.superclass);
    	}
    	for(UMLOperation operation : this.operations) {
    		if(!umlClass.operations.contains(operation))
    			classDiff.reportRemovedOperation(operation);
    	}
    	for(UMLOperation operation : umlClass.operations) {
    		if(!this.operations.contains(operation))
    			classDiff.reportAddedOperation(operation);
    	}
    	for(UMLAttribute attribute : this.attributes) {
    		if(!umlClass.attributes.contains(attribute))
    			classDiff.reportRemovedAttribute(attribute);
    	}
    	for(UMLAttribute attribute : umlClass.attributes) {
    		if(!this.attributes.contains(attribute))
    			classDiff.reportAddedAttribute(attribute);
    	}
    	classDiff.checkForAttributeChanges();
    	for(UMLOperation operation : this.operations) {
    		if(umlClass.operations.contains(operation)) {
    			int index = umlClass.operations.indexOf(operation);
    			UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(operation, umlClass.operations.get(index));
    			classDiff.addOperationBodyMapper(operationBodyMapper);
    			/*UMLOperationBodyDiff operationBodyDiff = new UMLOperationBodyDiff(operation, umlClass.operations.get(index));
    			if(!operationBodyDiff.isEmpty())
    				classDiff.addOperationBodyDiff(operationBodyDiff);*/
    		}
    	}
    	classDiff.checkForInlinedOperations();
    	classDiff.checkForOperationSignatureChanges();
    	classDiff.checkForStatementsMovedFromRemovedToAddedOperations();
    	classDiff.checkForExtractedOperations();
    	//classDiff.checkForAttributeRenames();
    	//classDiff.checkForOperationRenames();
    	return classDiff;
    }

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLClass) {
    		UMLClass umlClass = (UMLClass)o;
    		return this.packageName.equals(umlClass.packageName) && this.name.equals(umlClass.name);
    	}
    	return false;
    }

    public String toString() {
    	return getName();
    }

	public int compareTo(UMLClass umlClass) {
		return this.toString().compareTo(umlClass.toString());
	}

	public double normalizedNameDistance(UMLClass c) {
		String s1 = name.toLowerCase();
		String s2 = c.name.toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public double normalizedPackageNameDistance(UMLClass c) {
		String s1 = packageName.toLowerCase();
		String s2 = c.packageName.toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}
}
