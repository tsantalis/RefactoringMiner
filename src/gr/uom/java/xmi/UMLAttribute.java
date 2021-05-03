package gr.uom.java.xmi;

import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UMLAttribute implements Comparable<UMLAttribute>, Serializable, LocationInfoProvider, VariableDeclarationProvider {
	private LocationInfo locationInfo;
	private String name;
	private UMLType type;
	private String visibility;
	private String className;
	private boolean isFinal;
	private boolean isStatic;
	private boolean isTransient;
	private boolean isVolatile;
	private VariableDeclaration variableDeclaration;
	private List<UMLAnonymousClass> anonymousClassList;
	private UMLJavadoc javadoc;
	private List<UMLComment> comments;

	public UMLAttribute(String name, UMLType type, LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		this.name = name;
		this.type = type;
		this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
		this.comments = new ArrayList<UMLComment>();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public UMLType getType() {
		return type;
	}

	public void setType(UMLType type) {
		this.type = type;
	}

	public void addAnonymousClass(UMLAnonymousClass anonymous) {
		this.anonymousClassList.add(anonymous);
	}

	public List<UMLAnonymousClass> getAnonymousClassList() {
		return anonymousClassList;
	}

	public UMLAnonymousClass findAnonymousClass(AnonymousClassDeclarationObject anonymousClassDeclaration) {
		for(UMLAnonymousClass anonymousClass : this.getAnonymousClassList()) {
			if(anonymousClass.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo())) {
				return anonymousClass;
			}
		}
		return null;
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

	public boolean isTransient() {
		return isTransient;
	}

	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	public void setVolatile(boolean isVolatile) {
		this.isVolatile = isVolatile;
	}

	public String getNonQualifiedClassName() {
		return className.contains(".") ? className.substring(className.lastIndexOf(".")+1, className.length()) : className;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getName() {
		return name;
	}

	public VariableDeclaration getVariableDeclaration() {
		return variableDeclaration;
	}

	public void setVariableDeclaration(VariableDeclaration variableDeclaration) {
		this.variableDeclaration = variableDeclaration;
	}

	public UMLJavadoc getJavadoc() {
		return javadoc;
	}

	public void setJavadoc(UMLJavadoc javadoc) {
		this.javadoc = javadoc;
	}

	public List<UMLComment> getComments() {
		return comments;
	}

	public List<UMLAnnotation> getAnnotations() {
		return variableDeclaration.getAnnotations();
	}

	public boolean renamedWithIdenticalTypeAndInitializer(UMLAttribute attribute) {
		AbstractExpression thisInitializer = this.getVariableDeclaration().getInitializer();
		AbstractExpression otherInitializer = attribute.getVariableDeclaration().getInitializer();
		if(thisInitializer != null && otherInitializer != null && !this.name.equals(attribute.name)) {
			return thisInitializer.getExpression().equals(otherInitializer.getExpression()) && this.type.equals(attribute.type) && this.type.equalsQualified(attribute.type);
		}
		return false;
	}

	public boolean equalsIgnoringChangedType(UMLAttribute attribute) {
		if(this.name.equals(attribute.name) && this.type.equals(attribute.type) && this.type.equalsQualified(attribute.type))
			return true;
		if(!this.type.equals(attribute.type))
			return this.name.equals(attribute.name);
		return false;
	}

	public boolean equalsIgnoringChangedVisibility(UMLAttribute attribute) {
		if(this.name.equals(attribute.name) && this.type.equals(attribute.type))
			return true;
		return false;
	}

	public CodeRange codeRange() {
		LocationInfo info = getLocationInfo();
		return info.codeRange();
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLAttribute) {
    		UMLAttribute umlAttribute = (UMLAttribute)o;
    		return this.name.equals(umlAttribute.name) &&
			this.visibility.equals(umlAttribute.visibility) &&
			this.type.equals(umlAttribute.type);
    	}
    	return false;
	}

	public boolean equalsQualified(UMLAttribute umlAttribute) {
		return this.name.equals(umlAttribute.name) &&
				this.visibility.equals(umlAttribute.visibility) &&
				this.type.equalsQualified(umlAttribute.type);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(visibility);
		sb.append(" ");
		sb.append(name);
		sb.append(" : ");
		sb.append(type);
		return sb.toString();
	}

	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(visibility);
		sb.append(" ");
		sb.append(name);
		sb.append(" : ");
		sb.append(type.toQualifiedString());
		return sb.toString();
	}

	public int compareTo(UMLAttribute attribute) {
		return this.toString().compareTo(attribute.toString());
	}

	public double normalizedNameDistance(UMLAttribute attribute) {
		String s1 = getName().toLowerCase();
		String s2 = attribute.getName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}
}
