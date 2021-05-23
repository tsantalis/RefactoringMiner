package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.StringDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class UMLClass extends UMLAbstractClass implements Comparable<UMLClass>, Serializable, LocationInfoProvider {
	private String qualifiedName;
    private String sourceFile;
    private String sourceFolder;
    private String visibility;
    private boolean isFinal;
    private boolean isStatic;
	private boolean isAbstract;
	private boolean isInterface;
	private boolean isEnum;
	private boolean topLevel;
    private UMLType superclass;
    private List<UMLType> implementedInterfaces;
    private List<String> importedTypes;
    private List<UMLTypeParameter> typeParameters;
    private UMLJavadoc javadoc;
    private List<UMLAnnotation> annotations;
    private List<UMLEnumConstant> enumConstants;
    
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
        this.importedTypes = importedTypes;
        this.typeParameters = new ArrayList<UMLTypeParameter>();
        this.annotations = new ArrayList<UMLAnnotation>();
        this.enumConstants = new ArrayList<UMLEnumConstant>();
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

    public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

    public void addAnnotation(UMLAnnotation annotation) {
    	annotations.add(annotation);
    }

	public void addEnumConstant(UMLEnumConstant enumConstant) {
    	enumConstants.add(enumConstant);
    }

    public List<UMLEnumConstant> getEnumConstants() {
		return enumConstants;
	}

    public String getName() {
    	return this.qualifiedName;
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

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
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

	public UMLJavadoc getJavadoc() {
		return javadoc;
	}

	public void setJavadoc(UMLJavadoc javadoc) {
		this.javadoc = javadoc;
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
    	if(this.isInterface != umlClass.isInterface)
    		return false;
    	if(!equalTypeParameters(umlClass))
    		return false;
    	return true;
    }

	private boolean equalTypeParameters(UMLClass umlClass) {
		return this.typeParameters.equals(umlClass.typeParameters) || this.getTypeParameterNames().equals(umlClass.getTypeParameterNames()) ||
				this.renamedParameterizedType(umlClass);
	}

	private boolean renamedParameterizedType(UMLClass umlClass) {
		for(UMLOperation operation1 : this.operations) {
			List<UMLParameter> parameterized1 = operation1.getParameterizedTypesInSignature();
			if(!parameterized1.isEmpty()) {
				for(UMLOperation operation2 : umlClass.operations) {
					if(operation1.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation2)) {
						List<UMLParameter> parameterized2 = operation2.getParameterizedTypesInSignature();
						if(parameterized1.size() == parameterized2.size()) {
							int renamed = 0;
							for(int i=0; i<parameterized1.size(); i++) {
								UMLType type1 = parameterized1.get(i).getType();
								UMLType type2 = parameterized2.get(i).getType();
								if(type1.getTypeArguments().toString().equals(this.typeParameters.toString()) &&
										type2.getTypeArguments().toString().equals(umlClass.typeParameters.toString())) {
									renamed++;
								}
							}
							if(renamed == parameterized1.size()) {
								return true;
							}
						}
						break;
					}
				}
			}
		}
		return false;
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

    public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
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

	public boolean isSingleAbstractMethodInterface() {
		return isInterface && operations.size() == 1;
	}
}
