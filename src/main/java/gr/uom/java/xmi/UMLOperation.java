package gr.uom.java.xmi;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.AnonymousClassDeclarationObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;

import static gr.uom.java.xmi.Constants.JAVA;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.refactoringminer.util.AstUtils;

public class UMLOperation implements Comparable<UMLOperation>, Serializable, VariableDeclarationContainer {
	private LocationInfo locationInfo;
	private String name;
	private Visibility visibility;
	private boolean isAbstract;
	private List<UMLParameter> parameters;
	private String className;
	private boolean isConstructor;
	private boolean isFinal;
	private boolean isStatic;
	private boolean isNative;
	private boolean isSynchronized;
	private boolean isDefault;
	private Optional<UMLAnonymousClass> anonymousClassContainer;
	private OperationBody operationBody;
	private AbstractExpression defaultExpression;
	private List<UMLAnonymousClass> anonymousClassList;
	private List<UMLTypeParameter> typeParameters;
	private List<UMLType> thrownExceptionTypes;
	private UMLJavadoc javadoc;
	private List<UMLAnnotation> annotations;
	private List<UMLModifier> modifiers;
	private List<UMLComment> comments;
	private Map<String, Set<VariableDeclaration>> variableDeclarationMap;
	private String actualSignature;
	
	public UMLOperation(String name, LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
        this.name = name;
        this.parameters = new ArrayList<UMLParameter>();
        this.anonymousClassList = new ArrayList<UMLAnonymousClass>();
        this.typeParameters = new ArrayList<UMLTypeParameter>();
        this.thrownExceptionTypes = new ArrayList<UMLType>();
        this.annotations = new ArrayList<UMLAnnotation>();
        this.modifiers = new ArrayList<UMLModifier>();
        this.comments = new ArrayList<UMLComment>();
    }

	public String getActualSignature() {
		return actualSignature;
	}

	public void setActualSignature(String actualSignature) {
		this.actualSignature = actualSignature;
	}

	public List<UMLTypeParameter> getTypeParameters() {
		return typeParameters;
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

    public List<UMLModifier> getModifiers() {
		return modifiers;
	}

    public void addModifier(UMLModifier modifier) {
    	modifiers.add(modifier);
    }

	public List<UMLType> getThrownExceptionTypes() {
		return thrownExceptionTypes;
	}

	public void addThrownExceptionType(UMLType exceptionType) {
		thrownExceptionTypes.add(exceptionType);
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public String getName() {
		return name;
	}

	public String getElementType() {
		return "method";
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public void setConstructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
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

	public boolean isSynchronized() {
		return isSynchronized;
	}

	public void setSynchronized(boolean isSynchronized) {
		this.isSynchronized = isSynchronized;
	}

	public boolean isNative() {
		return isNative;
	}

	public void setNative(boolean isNative) {
		this.isNative = isNative;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public boolean isDeclaredInAnonymousClass() {
		return anonymousClassContainer != null && anonymousClassContainer.isPresent();
	}

	public Optional<UMLAnonymousClass> getAnonymousClassContainer() {
		return anonymousClassContainer;
	}

	public void setAnonymousClassContainer(UMLAnonymousClass anonymousClass) {
		this.anonymousClassContainer = Optional.of(anonymousClass);
	}

	public OperationBody getBody() {
		return operationBody;
	}

	public AbstractExpression getDefaultExpression() {
		return defaultExpression;
	}

	public boolean hasOverrideAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			if(annotation.getTypeName().equals("Override")) {
				return true;
			}
		}
		return false;
	}

	public boolean hasTestAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			if(annotation.getTypeName().equals("Test")) {
				return true;
			}
		}
		return false;
	}

	public boolean hasParameterizedTestAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			if(UMLOperation.isParameterizedTestAnnotation(annotation)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasSetUpAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			//JUnit 4
			if(annotation.getTypeName().equals("Before") || annotation.getTypeName().equals("BeforeClass") ||
					//JUnit 5
					annotation.getTypeName().equals("BeforeEach") || annotation.getTypeName().equals("BeforeAll")) {
				return true;
			}
		}
		return false;
	}

	public boolean hasTearDownAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			//JUnit 4
			if(annotation.getTypeName().equals("After") || annotation.getTypeName().equals("AfterClass") ||
					//JUnit 5
					annotation.getTypeName().equals("AfterEach") || annotation.getTypeName().equals("AfterAll")) {
				return true;
			}
		}
		return false;
	}

	public boolean hasDataProviderAnnotation() {
		for(UMLAnnotation annotation : annotations) {
			if(annotation.getTypeName().equals("DataProvider")) {
				return true;
			}
		}
		return false;
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

	public List<AbstractCall> getAllOperationInvocations() {
		if(operationBody != null)
			return operationBody.getAllOperationInvocations();
		return Collections.emptyList();
	}

	public List<AbstractCall> getAllCreations() {
		if(operationBody != null)
			return operationBody.getAllCreations();
		return Collections.emptyList();
	}

	public boolean containsAssertion() {
		if(operationBody != null)
			return operationBody.containsAssertion();
		return false;
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		if(operationBody != null)
			return operationBody.getAllLambdas();
		return Collections.emptyList();
	}

	public List<CompositeStatementObject> getSynchronizedStatements() {
		if(operationBody != null)
			return operationBody.getSynchronizedStatements();
		return Collections.emptyList();
	}

	public List<String> getAllVariables() {
		if(operationBody != null)
			return operationBody.getAllVariables();
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

	public double builderStatementRatio() {
		if(operationBody != null)
			return operationBody.builderStatementRatio();
		return 0;
	}

	public int statementCount() {
		if(operationBody != null)
			return operationBody.statementCount();
		return 0;
	}

	public void setBody(OperationBody body) {
		this.operationBody = body;
	}

	public void setDefaultExpression(AbstractExpression defaultExpression) {
		this.defaultExpression = defaultExpression;
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

	public void addParameter(UMLParameter parameter) {
		this.parameters.add(parameter);
	}

	public List<UMLParameter> getParameters() {
		return parameters;
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

	public UMLParameter getReturnParameter() {
		for(UMLParameter parameter : parameters) {
			if(parameter.getKind().equals("return"))
				return parameter;
		}
		return null;
	}

	public boolean equalReturnParameter(UMLOperation operation) {
		UMLParameter thisReturnParameter = this.getReturnParameter();
		UMLParameter otherReturnParameter = operation.getReturnParameter();
		if(thisReturnParameter != null && otherReturnParameter != null)
			return thisReturnParameter.equals(otherReturnParameter);
		else if(thisReturnParameter == null && otherReturnParameter == null)
			return true;
		else
			return false;
	}

	public boolean equalQualifiedReturnParameter(UMLOperation operation) {
		UMLParameter thisReturnParameter = this.getReturnParameter();
		UMLParameter otherReturnParameter = operation.getReturnParameter();
		if(thisReturnParameter != null && otherReturnParameter != null)
			return thisReturnParameter.equalsQualified(otherReturnParameter);
		else if(thisReturnParameter == null && otherReturnParameter == null)
			return true;
		else
			return false;
	}

	public boolean equalSignatureForAbstractMethods(UMLOperation operation) {
		return this.getBody() == null && operation.getBody() == null && this.name.equals(operation.name) &&
				equalTypeParameters(operation) && equalReturnParameter(operation) && this.getParameterTypeList().equals(operation.getParameterTypeList());
	}

	public boolean equalSignature(UMLOperation operation) {
		boolean equalParameterTypes = this.getParameterTypeList().equals(operation.getParameterTypeList());
		boolean compatibleParameterTypes = false;
		if(!equalParameterTypes) {
			List<UMLType> thisParameterTypeList = this.getParameterTypeList();
			List<UMLType> otherParameterTypeList = operation.getParameterTypeList();
			if(thisParameterTypeList.size() == otherParameterTypeList.size()) {
				int compatibleTypes = 0;
				int equalTypes = 0;
				for(int i=0; i<thisParameterTypeList.size(); i++) {
					UMLType thisParameterType = thisParameterTypeList.get(i);
					UMLType otherParameterType = otherParameterTypeList.get(i);
					if((thisParameterType.getClassType().endsWith("." + otherParameterType.getClassType()) ||
							otherParameterType.getClassType().endsWith("." + thisParameterType.getClassType())) &&
							thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) {
						compatibleTypes++;
					}
					else if(thisParameterType.equals(otherParameterType)) {
						equalTypes++;
					}
				}
				if(equalTypes + compatibleTypes == thisParameterTypeList.size()) {
					compatibleParameterTypes = true;
				}
			}
		}
		return this.name.equals(operation.name) && equalTypeParameters(operation) && (equalParameterTypes || compatibleParameterTypes) && equalReturnParameter(operation);
	}

	public boolean equalSignatureIgnoringOperationName(UMLOperation operation) {
		return this.visibility.equals(operation.visibility) &&
				this.isAbstract == operation.isAbstract &&
				this.isFinal == operation.isFinal &&
				this.isStatic == operation.isStatic &&
				this.parameters.equals(operation.parameters) &&
				equalTypeParameters(operation);
	}

	public boolean equalSignatureIgnoringChangedTypes(UMLOperation operation) {
		if(!(this.isConstructor && operation.isConstructor || equivalentName(operation)))
			return false;
		if(this.isAbstract != operation.isAbstract)
			return false;
		/*if(this.isStatic != operation.isStatic)
			return false;
		if(this.isFinal != operation.isFinal)
			return false;*/
		if(this.parameters.size() != operation.parameters.size())
			return false;
		if(!equalTypeParameters(operation))
			return false;
		int i=0;
		for(UMLParameter thisParameter : this.parameters) {
			UMLParameter otherParameter = operation.parameters.get(i);
			if(!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter))
				return false;
			i++;
		}
		return true;
	}

	public boolean equalSignatureWithIdenticalNameIgnoringChangedTypes(UMLOperation operation) {
		if(!(this.isConstructor && operation.isConstructor || this.name.equals(operation.name)))
			return false;
		if(this.isAbstract != operation.isAbstract)
			return false;
		/*if(this.isStatic != operation.isStatic)
			return false;
		if(this.isFinal != operation.isFinal)
			return false;*/
		if(this.parameters.size() != operation.parameters.size())
			return false;
		if(!equalTypeParameters(operation))
			return false;
		int i=0;
		for(UMLParameter thisParameter : this.parameters) {
			UMLParameter otherParameter = operation.parameters.get(i);
			if(!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter))
				return false;
			i++;
		}
		return true;
	}

	public boolean equalSignatureWithIdenticalNameIgnoringChangedTypesToFromObject(UMLOperation operation) {
		if(!(this.isConstructor && operation.isConstructor || this.name.equals(operation.name)))
			return false;
		if(this.isAbstract != operation.isAbstract)
			return false;
		/*if(this.isStatic != operation.isStatic)
			return false;
		if(this.isFinal != operation.isFinal)
			return false;*/
		if(this.parameters.size() != operation.parameters.size())
			return false;
		if(!equalTypeParameters(operation))
			return false;
		int i=0;
		for(UMLParameter thisParameter : this.parameters) {
			UMLParameter otherParameter = operation.parameters.get(i);
			if(!thisParameter.equals(otherParameter) && !thisParameter.equalsExcludingType(otherParameter))
				return false;
			if(thisParameter.getName().equals(otherParameter.getName()) && !thisParameter.getType().equals(otherParameter.getType()) &&
					!(thisParameter.getType().getClassType().equals("Object") || otherParameter.getType().getClassType().equals("Object")))
				return false;
			i++;
		}
		return true;
	}

	private boolean equivalentName(UMLOperation operation) {
		return this.name.equals(operation.name) || equivalentNames(this, operation) || equivalentNames(operation, this);
	}

	private static boolean equivalentNames(UMLOperation operation1, UMLOperation operation2) {
		boolean equalReturn = operation1.equalReturnParameter(operation2) && operation1.getParametersWithoutReturnType().size() > 0 && operation2.getParametersWithoutReturnType().size() > 0;
		if(operation1.name.startsWith(operation2.name) && !operation2.name.equals("get") && !operation2.name.equals("set") && !operation2.name.equals("print")) {
			String suffix1 = operation1.name.substring(operation2.name.length(), operation1.name.length());
			String className2 = operation2.className.contains(".") ? operation2.className.substring(operation2.className.lastIndexOf(".")+1, operation2.className.length()) : operation2.className;
			return operation2.name.length() > operation1.name.length() - operation2.name.length() || equalReturn || className2.contains(suffix1);
		}
		return false;
	}

	public List<UMLParameter> getParametersWithoutReturnType() {
		List<UMLParameter> params = new ArrayList<UMLParameter>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				params.add(parameter);
		}
		return params;
	}

	public List<UMLParameter> getParameterizedTypesInSignature() {
		List<UMLParameter> params = new ArrayList<UMLParameter>();
		for(UMLParameter parameter : parameters) {
			if(parameter.getType().isParameterized()) {
				params.add(parameter);
			}
		}
		return params;
	}

	public List<UMLType> getParameterTypeList() {
		List<UMLType> parameterTypeList = new ArrayList<UMLType>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterTypeList.add(parameter.getType());
		}
		return parameterTypeList;
	}

	public List<String> getParameterNameList() {
		List<String> parameterNameList = new ArrayList<String>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterNameList.add(parameter.getName());
		}
		return parameterNameList;
	}

	public List<VariableDeclaration> getParameterDeclarationList() {
		List<VariableDeclaration> parameterDeclarationList = new ArrayList<VariableDeclaration>();
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return"))
				parameterDeclarationList.add(parameter.getVariableDeclaration());
		}
		return parameterDeclarationList;
	}

	public int getNumberOfNonVarargsParameters() {
		int counter = 0;
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return") && !parameter.isVarargs()) {
				counter++;
			}
		}
		return counter;
	}

	public boolean hasVarargsParameter() {
		for(UMLParameter parameter : parameters) {
			if(!parameter.getKind().equals("return") && parameter.isVarargs()) {
				return true;
			}
		}
		return false;
	}

	public AbstractCall isDelegate() {
		if(getBody() != null) {
			List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
			if(statements.size() == 1 && statements.get(0) instanceof StatementObject) {
				StatementObject statement = (StatementObject)statements.get(0);
				for(AbstractCall operationInvocation : statement.getMethodInvocations()) {
					if(operationInvocation.matchesOperation(this, this, null, null) ||
							(operationInvocation.getName().equals(this.getName()) && (operationInvocation.getExpression() == null || operationInvocation.getExpression().endsWith("this")))) {
						return operationInvocation;
					}
				}
			}
		}
		return null;
	}

	public boolean isGetter() {
		if(getBody() != null) {
			List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
			List<UMLParameter> parameters = getParametersWithoutReturnType();
			if(statements.size() == 1 && statements.get(0) instanceof StatementObject) {
				StatementObject statement = (StatementObject)statements.get(0);
				if(statement.getString().startsWith(JAVA.RETURN_SPACE)) {
					boolean parameterUsed = false;
					for(UMLParameter parameter : parameters) {
						for(LeafExpression variableExpression : statement.getVariables()) {
							if(variableExpression.getString().equals(parameter.getName())) {
								parameterUsed = true;
								break;
							}
						}
					}
					for(LeafExpression variableExpression : statement.getVariables()) {
						String variable = variableExpression.getString();
						if(statement.getString().equals(JAVA.RETURN_SPACE + variable + JAVA.STATEMENT_TERMINATION) && (parameters.size() == 0 || !parameterUsed)) {
							return true;
						}
						else if(statement.getString().equals(JAVA.RETURN_SPACE + variable + ".keySet()" + JAVA.STATEMENT_TERMINATION) && (parameters.size() == 0 || !parameterUsed)) {
							return true;
						}
						else if(statement.getString().equals(JAVA.RETURN_SPACE + variable + ".values()" + JAVA.STATEMENT_TERMINATION) && (parameters.size() == 0 || !parameterUsed)) {
							return true;
						}
					}
					UMLParameter returnParameter = getReturnParameter();
					if((name.startsWith("is") || name.startsWith("has")) && (parameters.size() == 0 || !parameterUsed) &&
							returnParameter != null && returnParameter.getType().getClassType().equals("boolean")) {
						return true;
					}
					if(statement.getString().equals(JAVA.RETURN_NULL)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isSetter() {
		List<String> parameterNames = getParameterNameList();
		if(getBody() != null && parameterNames.size() == 1) {
			List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
			if(statements.size() == 1 && statements.get(0) instanceof StatementObject) {
				StatementObject statement = (StatementObject)statements.get(0);
				for(LeafExpression variableExpression : statement.getVariables()) {
					String variable = variableExpression.getString();
					if(statement.getString().equals(variable + JAVA.ASSIGNMENT + parameterNames.get(0) + JAVA.STATEMENT_TERMINATION)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isMultiSetter() {
		List<String> parameterNames = getParameterNameList();
		int matchCount = 0;
		if(getBody() != null) {
			List<AbstractStatement> statements = getBody().getCompositeStatement().getStatements();
			for(AbstractStatement statement : statements) {
				if(statement instanceof StatementObject) {
					for(LeafExpression variableExpression : statement.getVariables()) {
						String variable = variableExpression.getString();
						if(statement.getString().startsWith(variable + JAVA.ASSIGNMENT)) {
							for(String parameterName : parameterNames) {
								if(statement.getString().equals(variable + JAVA.ASSIGNMENT + parameterName + JAVA.STATEMENT_TERMINATION)) {
									matchCount++;
									break;
								}
							}
						}
					}
				}
			}
		}
		return matchCount > 0;
	}

	public boolean isRecursive() {
		if(hasVarargsParameter())
			return false;
		List<AbstractCall> calls = getAllOperationInvocations();
		for(AbstractCall call : calls) {
			if(call.getName().equals(this.getName()) && call.arguments().size() == this.getParametersWithoutReturnType().size()) {
				return true;
			}
		}
		return false;
	}

	public boolean equalsIgnoringAbstraction(UMLOperation operation) {
		return this.className.equals(operation.className) &&
				this.name.equals(operation.name) &&
				equalReturnParameter(operation) &&
				this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
				equalTypeParameters(operation);
	}

	public boolean equalsIgnoringVisibility(UMLOperation operation) {
		boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
		boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
		return this.className.equals(operation.className) &&
				this.name.equals(operation.name) &&
				this.isAbstract == operation.isAbstract &&
				thisEmptyBody == otherEmptyBody &&
				equalReturnParameter(operation) &&
				this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
				equalTypeParameters(operation);
	}

	public boolean equalsIgnoringNameCase(UMLOperation operation) {
		boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
		boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
		return this.className.equals(operation.className) &&
				this.name.equalsIgnoreCase(operation.name) &&
				this.visibility.equals(operation.visibility) &&
				this.isAbstract == operation.isAbstract &&
				thisEmptyBody == otherEmptyBody &&
				equalReturnParameter(operation) &&
				this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
				equalTypeParameters(operation);
	}

	public boolean equals(Object o) {
		if(this == o) {
            return true;
        }
		
		if(o instanceof UMLOperation) {
			UMLOperation operation = (UMLOperation)o;
			boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
			boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
			return this.className.equals(operation.className) &&
				this.name.equals(operation.name) &&
				this.visibility.equals(operation.visibility) &&
				this.isAbstract == operation.isAbstract &&
				thisEmptyBody == otherEmptyBody &&
				this.getParameterTypeList().equals(operation.getParameterTypeList()) &&
				equalTypeParameters(operation);
		}
		return false;
	}

	public boolean equalsIgoringTypeParameters(UMLOperation operation) {
		boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
		boolean otherEmptyBody = operation.getBody() == null || operation.hasEmptyBody();
		return this.className.equals(operation.className) &&
			this.name.equals(operation.name) &&
			this.visibility.equals(operation.visibility) &&
			this.isAbstract == operation.isAbstract &&
			thisEmptyBody == otherEmptyBody &&
			this.getParameterTypeList().equals(operation.getParameterTypeList());
	}

	public boolean equalsQualified(UMLOperation operation) {
		if(this.className.equals(operation.className) &&
				this.name.equals(operation.name) &&
				this.visibility.equals(operation.visibility) &&
				this.isAbstract == operation.isAbstract &&
				equalTypeParameters(operation)) {
			UMLParameter thisReturnParameter = this.getReturnParameter();
			UMLParameter otherReturnParameter = operation.getReturnParameter();
			if(thisReturnParameter != null && otherReturnParameter != null) {
				if(!thisReturnParameter.getType().equalsQualified(otherReturnParameter.getType())) {
					return false;
				}
			}
			List<UMLType> thisParameterTypeList = this.getParameterTypeList();
			List<UMLType> otherParameterTypeList = operation.getParameterTypeList();
			if(thisParameterTypeList.size() != otherParameterTypeList.size()) {
				return false;
			}
			for(int i=0; i<thisParameterTypeList.size(); i++) {
				UMLType thisParameterType = thisParameterTypeList.get(i);
				UMLType otherParameterType = otherParameterTypeList.get(i);
				if(!thisParameterType.equalsQualified(otherParameterType)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		boolean thisEmptyBody = this.getBody() == null || this.hasEmptyBody();
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + (isAbstract ? 1231 : 1237);
		result = prime * result + (thisEmptyBody ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((getParameterTypeList() == null) ? 0 : getParameterTypeList().hashCode());
		result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
		result = prime * result + ((typeParameters == null) ? 0 : typeParameters.hashCode());
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(visibility);
		sb.append(" ");
		if(isAbstract) {
			sb.append("abstract");
			sb.append(" ");
		}
		sb.append(name);
		UMLParameter returnParameter = getReturnParameter();
		List<UMLParameter> parameters = new ArrayList<UMLParameter>(this.parameters);
		parameters.remove(returnParameter);
		sb.append("(");
		for(int i=0; i<parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				sb.append(parameter);
				if(i < parameters.size()-1)
					sb.append(", ");
			}
		}
		sb.append(")");
		if(returnParameter != null) {
			sb.append(" : ");
			sb.append(returnParameter.toString());
		}
		return sb.toString();
	}

	public String toQualifiedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(visibility);
		sb.append(" ");
		if(isAbstract) {
			sb.append("abstract");
			sb.append(" ");
		}
		sb.append(name);
		UMLParameter returnParameter = getReturnParameter();
		List<UMLParameter> parameters = new ArrayList<UMLParameter>(this.parameters);
		parameters.remove(returnParameter);
		sb.append("(");
		for(int i=0; i<parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				sb.append(parameter.toQualifiedString());
				if(i < parameters.size()-1)
					sb.append(", ");
			}
		}
		sb.append(")");
		if(returnParameter != null) {
			sb.append(" : ");
			sb.append(returnParameter.toQualifiedString());
		}
		return sb.toString();
	}

	public String getKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(className);
		sb.append('#');
		sb.append(name);
		UMLParameter returnParameter = getReturnParameter();
		List<UMLParameter> parameters = new ArrayList<UMLParameter>(this.parameters);
		parameters.remove(returnParameter);
		sb.append("(");
		for (int i = 0; i < parameters.size(); i++) {
			UMLParameter parameter = parameters.get(i);
			if(parameter.getKind().equals("in")) {
				sb.append(AstUtils.stripTypeParamsFromTypeName(parameter.getType().toString()));
				if(i < parameters.size() - 1)
					sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	public int compareTo(UMLOperation operation) {
		return this.toString().compareTo(operation.toString());
	}

	public double normalizedNameDistance(UMLOperation operation) {
		String s1 = getName().toLowerCase();
		String s2 = operation.getName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean testMethodCheck(UMLOperation operation) { 
		if(this.hasTestAnnotation() && !operation.hasTestAnnotation() && !operation.containsAssertion())
			return false;
		if(!this.hasTestAnnotation() && !this.containsAssertion() && operation.hasTestAnnotation())
			return false;
		return true;
	}

	public boolean equalParameters(UMLOperation operation) {
		return this.equalReturnParameter(operation) && this.getParameters().equals(operation.getParameters());
	}

	public boolean equalParameterTypes(UMLOperation operation) {
		return this.getParameterTypeList().equals(operation.getParameterTypeList()) && equalTypeParameters(operation);
	}

	private boolean typeParameterToTypeArgumentMatch(UMLOperation operation, Map<UMLTypeParameter, UMLType> typeParameterToTypeArgumentMap) {
		for(UMLTypeParameter key : typeParameterToTypeArgumentMap.keySet()) {
			UMLType value = typeParameterToTypeArgumentMap.get(key);
			UMLParameter thisReturnParameter = this.getReturnParameter();
			UMLParameter otherReturnParameter = operation.getReturnParameter();
			if(thisReturnParameter != null && otherReturnParameter != null) {
				UMLType thisReturnType = thisReturnParameter.getType();
				UMLType otherReturnType = otherReturnParameter.getType();
				if(thisReturnType.toString().equals(value.toString()) &&
						(otherReturnType.toString().equals(key.toString()) || otherReturnType.toString().equals(key.getName()))) {
					return true;
				}
				if(otherReturnType.toString().equals(value.toString()) &&
						(thisReturnType.toString().equals(key.toString()) || thisReturnType.toString().equals(key.getName()))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean equalTypeParameters(UMLOperation operation) {
		return this.typeParameters.equals(operation.typeParameters);
	}

	private boolean equalParameterNames(UMLOperation operation) {
		return this.getParameterNameList().equals(operation.getParameterNameList());
	}

	private boolean overloadedParameterTypes(UMLOperation operation) {
		return this.getParameterTypeList().containsAll(operation.getParameterTypeList()) || operation.getParameterTypeList().containsAll(this.getParameterTypeList());
	}
	
	public boolean replacedParameterTypes(UMLOperation operation) {
		List<UMLType> thisParameterTypes = this.getParameterTypeList();
		List<UMLType> otherParameterTypes = operation.getParameterTypeList();
		
		if(thisParameterTypes.size() == otherParameterTypes.size() && thisParameterTypes.size() > 0) {
			int commonParameterTypes = 0;
			int differentParameterTypes = 0;
			for(int i=0; i<thisParameterTypes.size(); i++) {
				UMLType thisParameterType = thisParameterTypes.get(i);
				UMLType otherParameterType = otherParameterTypes.get(i);
				if(thisParameterType.equals(otherParameterType) ||
						(thisParameterType.getClassType().equals(otherParameterType.getClassType()) && thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
						thisParameterType.equalsWithSubType(otherParameterType)) {
					commonParameterTypes++;
				}
				else {
					differentParameterTypes++;
				}
			}
			return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
		}
		else if(thisParameterTypes.size() > otherParameterTypes.size() && thisParameterTypes.size() > 0) {
			int commonParameterTypes = 0;
			int differentParameterTypes = 0;
			for(int i=0; i<otherParameterTypes.size(); i++) {
				UMLType thisParameterType = thisParameterTypes.get(i);
				UMLType otherParameterType = otherParameterTypes.get(i);
				if(thisParameterType.equals(otherParameterType) ||
						(thisParameterType.getClassType().equals(otherParameterType.getClassType()) && thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
						thisParameterType.equalsWithSubType(otherParameterType)) {
					commonParameterTypes++;
				}
				else {
					differentParameterTypes++;
				}
			}
			return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
		}
		else if(otherParameterTypes.size() > thisParameterTypes.size() && thisParameterTypes.size() > 0) {
			int commonParameterTypes = 0;
			int differentParameterTypes = 0;
			for(int i=0; i<thisParameterTypes.size(); i++) {
				UMLType thisParameterType = thisParameterTypes.get(i);
				UMLType otherParameterType = otherParameterTypes.get(i);
				if(thisParameterType.equals(otherParameterType) ||
						(thisParameterType.getClassType().equals(otherParameterType.getClassType()) && thisParameterType.getArrayDimension() == otherParameterType.getArrayDimension()) ||
						thisParameterType.equalsWithSubType(otherParameterType)) {
					commonParameterTypes++;
				}
				else {
					differentParameterTypes++;
				}
			}
			return commonParameterTypes >= differentParameterTypes && commonParameterTypes > 0;
		}
		return false;
	}

	public List<UMLOperation> getOperationsInsideAnonymousClass(List<UMLAnonymousClass> allAddedAnonymousClasses) {
		List<UMLOperation> operationsInsideAnonymousClass = new ArrayList<UMLOperation>();
		if(this.operationBody != null) {
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations = this.operationBody.getAllAnonymousClassDeclarations();
			for(AnonymousClassDeclarationObject anonymousClassDeclaration : anonymousClassDeclarations) {
				for(UMLAnonymousClass anonymousClass : allAddedAnonymousClasses) {
					if(anonymousClass.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo())) {
						operationsInsideAnonymousClass.addAll(anonymousClass.getOperations());
					}
				}
			}
		}
		return operationsInsideAnonymousClass;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}

	public boolean overridesObject() {
		return isEquals() || isHashCode() || isToString() || isClone() || isCompareTo();
	}

	public boolean isMain() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("main") && getReturnParameter().getType().getClassType().equals("void") &&
				parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("String") &&
				parameterTypeList.get(0).getArrayDimension() == 1;
	}

	private boolean isEquals() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("equals") && getReturnParameter().getType().getClassType().equals("boolean") &&
				parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("Object");
	}

	private boolean isHashCode() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("hashCode") && getReturnParameter().getType().getClassType().equals("int") && parameterTypeList.size() == 0;
	}

	private boolean isToString() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("toString") && getReturnParameter().getType().getClassType().equals("String") && parameterTypeList.size() == 0;
	}

	private boolean isClone() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("clone") && getReturnParameter().getType().getClassType().equals("Object") && parameterTypeList.size() == 0;
	}

	private boolean isCompareTo() {
		List<UMLType> parameterTypeList = getParameterTypeList();
		return getName().equals("compareTo") && getReturnParameter().getType().getClassType().equals("int") && parameterTypeList.size() == 1;
	}

	public boolean compatibleSignature(UMLOperation removedOperation) {
		return (this.equalReturnParameter(removedOperation) && (equalParameterTypes(removedOperation) || overloadedParameterTypes(removedOperation) || equalParameterNames(removedOperation))) || replacedParameterTypes(removedOperation);
	}

	public boolean compatibleSignature(UMLOperation removedOperation, Map<UMLTypeParameter, UMLType> typeParameterToTypeArgumentMap) {
		return ((this.equalReturnParameter(removedOperation) || typeParameterToTypeArgumentMatch(removedOperation, typeParameterToTypeArgumentMap)) && (equalParameterTypes(removedOperation) || overloadedParameterTypes(removedOperation) || equalParameterNames(removedOperation))) || replacedParameterTypes(removedOperation);
	}

	public boolean hasTwoParametersWithTheSameType() {
		List<UMLType> parameterTypes = getParameterTypeList();
		if(parameterTypes.size() == 2) {
			if(parameterTypes.get(0).equals(parameterTypes.get(1))) {
				return true;
			}
		}
		return false;
	}

	public List<String> getSignatureIdentifiers() {
		List<String> signature = new ArrayList<>();
		signature.add(name);
		signature.addAll(getParameterNameList());
		return signature;
	}

	public static boolean isParameterizedTestAnnotation(UMLAnnotation a) {
		return isJUnit5ParameterizedTest(a) || isTestNGParameterizedAnnotation(a);
	}

	private static boolean isJUnit5ParameterizedTest(UMLAnnotation a) {
		return a.getTypeName().equals("ParameterizedTest");
	}

	public static boolean isTestNGParameterizedAnnotation(UMLAnnotation a) {
		return a.getTypeName().equals("Test") &&
				a.isNormalAnnotation() &&
				a.getMemberValuePairs().containsKey("dataProvider");
	}

}
