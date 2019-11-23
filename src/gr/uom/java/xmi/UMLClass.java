package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.StringDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class UMLClass extends UMLAbstractClass implements Comparable<UMLClass>, Serializable, LocationInfoProvider {
	private String qualifiedName;
    private String sourceFile;
    private String sourceFolder;
    private String visibility;
	private boolean isAbstract;
	private boolean isInterface;
	private boolean isEnum;
	private boolean topLevel;
    private UMLType superclass;
    private List<UMLType> implementedInterfaces;
    private List<UMLAnonymousClass> anonymousClassList;
    private List<String> importedTypes;
    private List<UMLTypeParameter> typeParameters;
    private UMLJavadoc javadoc;
    
    public UMLClass(String packageName, String name, LocationInfo locationInfo, boolean topLevel, List<String> importedTypes) {
    	super();
    	this.locationInfo = locationInfo;
    	this.packageName = packageName;
        this.name = name;
        if(packageName.equals(""))
        	this.qualifiedName = name;
    	else
    		this.qualifiedName = packageName + "." + name;
        
        this.sourceFile = getSourceFile();
        this.sourceFolder = "";
        if(packageName.equals("")) {
        	int index = sourceFile.indexOf(name);
        	if(index != -1) {
    			this.sourceFolder = sourceFile.substring(0, index);
    		}
        }
        else {
        	if(topLevel) {
        		int index = sourceFile.indexOf(packageName.replace('.', '/'));
        		if(index != -1) {
        			this.sourceFolder = sourceFile.substring(0, index);
        		}
        	}
        	else {
        		int index = -1;
        		if(packageName.contains(".")) {
        			String realPackageName = packageName.substring(0, packageName.lastIndexOf('.'));
        			index = sourceFile.indexOf(realPackageName.replace('.', '/'));
        		}
        		else {
        			index = sourceFile.indexOf(packageName);
        		}
        		if(index != -1) {
        			this.sourceFolder = sourceFile.substring(0, index);
        		}
        	}
        }
        this.isAbstract = false;
        this.isInterface = false;
        this.topLevel = topLevel;
        this.superclass = null;
        this.implementedInterfaces = new ArrayList<UMLType>();
        this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
        this.importedTypes = importedTypes;
        this.typeParameters = new ArrayList<UMLTypeParameter>();
    }

    public List<UMLTypeParameter> getTypeParameters() {
		return typeParameters;
	}

    public List<String> getTypeParameterNames() {
    	List<String> typeParameterNames = new ArrayList<String>();
		for(UMLTypeParameter typeParameter : typeParameters) {
			typeParameterNames.add(typeParameter.getName());
		}
		return typeParameterNames;
	}

	public void addTypeParameter(UMLTypeParameter typeParameter) {
    	typeParameters.add(typeParameter);
    }

	public void addAnonymousClass(UMLAnonymousClass anonymousClass) {
    	anonymousClassList.add(anonymousClass);
    }

    public String getPackageName() {
		return this.packageName;
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

    public boolean isTopLevel() {
		return topLevel;
	}

	public void setTopLevel(boolean topLevel) {
		this.topLevel = topLevel;
	}

	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public boolean isEnum() {
		return isEnum;
	}

	public void setEnum(boolean isEnum) {
		this.isEnum = isEnum;
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

	public void addImplementedInterface(UMLType implementedInterface) {
		this.implementedInterfaces.add(implementedInterface);
	}

	public List<UMLType> getImplementedInterfaces() {
		return implementedInterfaces;
	}

	public List<String> getImportedTypes() {
		return importedTypes;
	}

	public List<UMLAnonymousClass> getAnonymousClassList() {
		return anonymousClassList;
	}

	public UMLJavadoc getJavadoc() {
		return javadoc;
	}

	public void setJavadoc(UMLJavadoc javadoc) {
		this.javadoc = javadoc;
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

    public boolean hasSameNameAndKind(UMLClass umlClass) {
    	if(!this.name.equals(umlClass.name))
    		return false;
    	if(!hasSameKind(umlClass))
    		return false;
    	return true;
    }

    public boolean hasSameKind(UMLClass umlClass) {
    	if(this.isAbstract != umlClass.isAbstract)
    		return false;
    	if(this.isInterface != umlClass.isInterface)
    		return false;
    	if(!equalTypeParameters(umlClass))
    		return false;
    	return true;
    }

	private boolean equalTypeParameters(UMLClass umlClass) {
		return this.typeParameters.equals(umlClass.typeParameters) || this.getTypeParameterNames().equals(umlClass.getTypeParameterNames());
	}

    public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLClass) {
    		UMLClass umlClass = (UMLClass)o;
    		return this.packageName.equals(umlClass.packageName) && this.name.equals(umlClass.name) && this.sourceFile.equals(umlClass.sourceFile);
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
	
	public double normalizedSourceFolderDistance(UMLClass c) {
		String s1 = sourceFolder.toLowerCase();
		String s2 = c.sourceFolder.toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean implementsInterface(Set<UMLType> interfaces) {
		for(UMLType type : interfaces) {
			if(implementedInterfaces.contains(type))
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
		for(String importedType : getImportedTypes()) {
			//importedType.startsWith(targetClass) -> special handling for import static
			//importedType.equals(targetClassPackage) -> special handling for import with asterisk (*) wildcard
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

	public boolean containsAnonymousWithSameAttributesAndOperations(UMLAnonymousClass anonymous) {
		for(UMLAnonymousClass thisAnonymous : anonymousClassList) {
			if(thisAnonymous.hasSameAttributesAndOperations(anonymous))
				return true;
		}
		return false;
	}

	public boolean isSingleAbstractMethodInterface() {
		return isInterface && operations.size() == 1;
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
}
