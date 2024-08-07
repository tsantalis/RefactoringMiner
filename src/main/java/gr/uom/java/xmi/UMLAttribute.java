package gr.uom.java.xmi;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UMLAttribute implements Comparable<UMLAttribute>, Serializable, VariableDeclarationProvider, VariableDeclarationContainer {
	private LocationInfo locationInfo;
	private LocationInfo fieldDeclarationLocationInfo;
	private String name;
	private UMLType type;
	private Visibility visibility;
	private String className;
	private boolean isFinal;
	private boolean isStatic;
	private boolean isTransient;
	private boolean isVolatile;
	private Optional<UMLAnonymousClass> anonymousClassContainer;
	private VariableDeclaration variableDeclaration;
	private List<UMLAnonymousClass> anonymousClassList;
	private UMLJavadoc javadoc;
	private List<UMLComment> comments;
	private Map<String, Set<VariableDeclaration>> variableDeclarationMap;

	public UMLAttribute(String name, UMLType type, LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
		this.name = name;
		this.type = type;
		this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
		this.comments = new ArrayList<UMLComment>();
	}

	public LocationInfo getFieldDeclarationLocationInfo() {
		return fieldDeclarationLocationInfo;
	}

	public void setFieldDeclarationLocationInfo(LocationInfo fieldDeclarationLocationInfo) {
		this.fieldDeclarationLocationInfo = fieldDeclarationLocationInfo;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public String getElementType() {
		return "attribute";
	}

	public UMLType getType() {
		return type;
	}

	public void setType(UMLType type) {
		this.type = type;
	}

	public boolean isDeclaredInAnonymousClass() {
		return anonymousClassContainer != null && anonymousClassContainer.isPresent();
	}

	public boolean isGetter() {
		return false;
	}

	public boolean isConstructor() {
		return false;
	}

	public AbstractCall isDelegate() {
		return null;
	}

	public boolean isRecursive() {
		return false;
	}

	public boolean isMain() {
		return false;
	}

	public Optional<UMLAnonymousClass> getAnonymousClassContainer() {
		return anonymousClassContainer;
	}

	public void setAnonymousClassContainer(UMLAnonymousClass anonymousClass) {
		this.anonymousClassContainer = Optional.of(anonymousClass);
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

	public List<VariableDeclaration> getParameterDeclarationList() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.getParameters();
				}
			}
		}
		return Collections.emptyList();
	}

	public List<UMLType> getParameterTypeList() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.getParameterTypeList();
				}
			}
		}
		return Collections.emptyList();
	}

	public List<String> getParameterNameList() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.getParameterNameList();
				}
			}
		}
		return Collections.emptyList();
	}

	public List<UMLParameter> getParametersWithoutReturnType() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.getUmlParameters();
				}
			}
		}
		return Collections.emptyList();
	}

	public int getNumberOfNonVarargsParameters() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.getNumberOfNonVarargsParameters();
				}
			}
		}
		return 0;
	}

	public boolean hasVarargsParameter() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.hasVarargsParameter();
				}
			}
		}
		return false;
	}

	public OperationBody getBody() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			for(LambdaExpressionObject lambda : initializer.getLambdas()) {
				if(match(initializer, lambda)) {
					return lambda.getBody();
				}
			}
		}
		return null;
	}

	private boolean match(AbstractExpression initializer, LambdaExpressionObject lambda) {
		return lambda.getLocationInfo().getStartLine() == initializer.getLocationInfo().getStartLine() &&
				lambda.getLocationInfo().getEndLine() == initializer.getLocationInfo().getEndLine();
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			return initializer.getLambdas();
		}
		return Collections.emptyList();
	}

	public List<AbstractCall> getAllOperationInvocations() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			return new ArrayList<>(initializer.getMethodInvocations());
		}
		return Collections.emptyList();
	}

	public List<AbstractCall> getAllCreations() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			return new ArrayList<>(initializer.getCreations());
		}
		return Collections.emptyList();
	}

	public List<String> getAllVariables() {
		AbstractExpression initializer = variableDeclaration.getInitializer();
		if(initializer != null) {
			List<String> variables = new ArrayList<>();
			for(LeafExpression variable : initializer.getVariables()) {
				variables.add(variable.getString());
			}
			return variables;
		}
		return Collections.emptyList();
	}

	public Map<String, Set<VariableDeclaration>> variableDeclarationMap() {
		if(this.variableDeclarationMap == null) {
			this.variableDeclarationMap = new LinkedHashMap<String, Set<VariableDeclaration>>();
			for(VariableDeclaration declaration : getAllVariableDeclarations()) {
				if(variableDeclarationMap.containsKey(declaration.getVariableName())) {
					variableDeclarationMap.get(declaration.getVariableName()).add(declaration);
				}
				else {
					Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
					variableDeclarations.add(declaration);
					variableDeclarationMap.put(declaration.getVariableName(), variableDeclarations);
				}
			}
		}
		return variableDeclarationMap;
	}

	@Override
	public boolean hasTestAnnotation() {
		return false;
	}

	@Override
	public boolean hasParameterizedTestAnnotation() {
		return false;
	}

	@Override
	public boolean hasSetUpAnnotation() {
		return false;
	}

	@Override
	public boolean hasTearDownAnnotation() {
		return false;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
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

	public boolean identicalIncludingAnnotation(UMLAttribute attribute) {
		AbstractExpression thisInitializer = this.getVariableDeclaration().getInitializer();
		AbstractExpression otherInitializer = attribute.getVariableDeclaration().getInitializer();
		//ignore type if it is enum constant
		boolean equalType = this instanceof UMLEnumConstant && attribute instanceof UMLEnumConstant ? true : this.type.equals(attribute.type);
		boolean equalQualifiedType = this instanceof UMLEnumConstant && attribute instanceof UMLEnumConstant ? true : this.type.equalsQualified(attribute.type);
		boolean equal = this.name.equals(attribute.name) && equalType && equalQualifiedType &&
				this.visibility.equals(attribute.visibility) && this.getAnnotations().equals(attribute.getAnnotations());
		if(thisInitializer != null && otherInitializer != null) {
			return equal && thisInitializer.getExpression().equals(otherInitializer.getExpression());
		}
		if(thisInitializer == null && otherInitializer == null) {
			return equal;
		}
		return false;
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
		if(!this.type.equals(attribute.type) || !this.type.equalsQualified(attribute.type)) {
			boolean equalAnnotations = true;
			if(this instanceof UMLEnumConstant && attribute instanceof UMLEnumConstant) {
				equalAnnotations = this.getAnnotations().equals(attribute.getAnnotations());
			}
			return equalAnnotations && this.name.equals(attribute.name);
		}
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
