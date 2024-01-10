package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;

import static gr.uom.java.xmi.Constants.JAVA;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.containsMethodSignatureOfAnonymousClass;
import static gr.uom.java.xmi.decomposition.Visitor.stringify;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.refactoringminer.util.PrefixSuffixUtils;

public class OperationInvocation extends AbstractCall {
	private String methodName;
	private List<String> subExpressions = new ArrayList<String>();
	private volatile int hashCode = 0;
	public static Map<String, String> PRIMITIVE_WRAPPER_CLASS_MAP;
    private static Map<String, List<String>> PRIMITIVE_TYPE_WIDENING_MAP;
    private static Map<String, List<String>> PRIMITIVE_TYPE_NARROWING_MAP;
    private static List<String> PRIMITIVE_TYPE_LIST;

    static {
    	PRIMITIVE_TYPE_LIST = new ArrayList<>(Arrays.asList("byte", "short", "int", "long", "float", "double", "char", "boolean"));
    	
    	PRIMITIVE_WRAPPER_CLASS_MAP = new HashMap<>();
        PRIMITIVE_WRAPPER_CLASS_MAP.put("boolean", "Boolean");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("byte", "Byte");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("char", "Character");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("float", "Float");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("int", "Integer");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("long", "Long");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("short", "Short");
        PRIMITIVE_WRAPPER_CLASS_MAP.put("double", "Double");

        PRIMITIVE_WRAPPER_CLASS_MAP = Collections.unmodifiableMap(PRIMITIVE_WRAPPER_CLASS_MAP);

        PRIMITIVE_TYPE_WIDENING_MAP = new HashMap<>();
        PRIMITIVE_TYPE_WIDENING_MAP.put("byte", Arrays.asList("short", "int", "long", "float", "double"));
        PRIMITIVE_TYPE_WIDENING_MAP.put("short", Arrays.asList("int", "long", "float", "double"));
        PRIMITIVE_TYPE_WIDENING_MAP.put("char", Arrays.asList("int", "long", "float", "double"));
        PRIMITIVE_TYPE_WIDENING_MAP.put("int", Arrays.asList("long", "float", "double"));
        PRIMITIVE_TYPE_WIDENING_MAP.put("long", Arrays.asList("float", "double"));
        PRIMITIVE_TYPE_WIDENING_MAP.put("float", Arrays.asList("double"));

        PRIMITIVE_TYPE_WIDENING_MAP = Collections.unmodifiableMap(PRIMITIVE_TYPE_WIDENING_MAP);

        PRIMITIVE_TYPE_NARROWING_MAP = new HashMap<>();
        PRIMITIVE_TYPE_NARROWING_MAP.put("short", Arrays.asList("byte", "char"));
        PRIMITIVE_TYPE_NARROWING_MAP.put("char", Arrays.asList("byte", "short"));
        PRIMITIVE_TYPE_NARROWING_MAP.put("int", Arrays.asList("byte", "short", "char"));
        PRIMITIVE_TYPE_NARROWING_MAP.put("long", Arrays.asList("byte", "short", "char", "int"));
        PRIMITIVE_TYPE_NARROWING_MAP.put("float", Arrays.asList("byte", "short", "char", "int", "long"));
        PRIMITIVE_TYPE_NARROWING_MAP.put("double", Arrays.asList("byte", "short", "char", "int", "long", "float"));

        PRIMITIVE_TYPE_NARROWING_MAP = Collections.unmodifiableMap(PRIMITIVE_TYPE_NARROWING_MAP);
    }

	public OperationInvocation(CompilationUnit cu, String filePath, MethodInvocation invocation, VariableDeclarationContainer container) {
		super(cu, filePath, invocation, CodeElementType.METHOD_INVOCATION, container);
		this.methodName = invocation.getName().getIdentifier();
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
		if(invocation.getExpression() != null) {
			this.expression = stringify(invocation.getExpression());
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}
	
	private void processExpression(Expression expression, List<String> subExpressions) {
		if(expression instanceof MethodInvocation) {
			MethodInvocation invocation = (MethodInvocation)expression;
			if(invocation.getExpression() != null) {
				String expressionAsString = stringify(invocation.getExpression());
				String invocationAsString = stringify(invocation);
				String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
				subExpressions.add(0, suffix);
				processExpression(invocation.getExpression(), subExpressions);
			}
			else {
				subExpressions.add(0, stringify(invocation));
			}
		}
		else if(expression instanceof ClassInstanceCreation) {
			ClassInstanceCreation creation = (ClassInstanceCreation)expression;
			if(creation.getExpression() != null) {
				String expressionAsString = stringify(creation.getExpression());
				String invocationAsString = stringify(creation);
				String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
				subExpressions.add(0, suffix);
				processExpression(creation.getExpression(), subExpressions);
			}
			else {
				subExpressions.add(0, stringify(creation));
			}
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, SuperMethodInvocation invocation, VariableDeclarationContainer container) {
		super(cu, filePath, invocation, CodeElementType.SUPER_METHOD_INVOCATION, container);
		this.methodName = invocation.getName().getIdentifier();
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		this.expression = "super";
		this.subExpressions.add("super");
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, SuperConstructorInvocation invocation, VariableDeclarationContainer container) {
		super(cu, filePath, invocation, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION, container);
		this.methodName = "super";
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
		if(invocation.getExpression() != null) {
			this.expression = stringify(invocation.getExpression());
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, ConstructorInvocation invocation, VariableDeclarationContainer container) {
		super(cu, filePath, invocation, CodeElementType.CONSTRUCTOR_INVOCATION, container);
		this.methodName = "this";
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
	}

	private OperationInvocation() {
		super();
	}

	public OperationInvocation update(String oldExpression, String newExpression) {
		OperationInvocation newOperationInvocation = new OperationInvocation();
		newOperationInvocation.methodName = this.methodName;
		newOperationInvocation.locationInfo = this.locationInfo;
		update(newOperationInvocation, oldExpression, newExpression);
		newOperationInvocation.subExpressions = new ArrayList<String>();
		for(String argument : this.subExpressions) {
			newOperationInvocation.subExpressions.add(
				ReplacementUtil.performReplacement(argument, oldExpression, newExpression));
		}
		return newOperationInvocation;
	}

	public String getName() {
		return getMethodName();
	}

    public String getMethodName() {
		return methodName;
	}

    public List<String> getSubExpressions() {
		return subExpressions;
	}

	public int numberOfSubExpressions() {
    	return subExpressions.size();
    }

    public boolean matchesOperation(VariableDeclarationContainer operation, VariableDeclarationContainer callerOperation,
    		UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
    	boolean constructorCall = false;
    	if(this.methodName.equals("this") && operation.getClassName().equals(callerOperation.getClassName()) && operation.getName().equals(callerOperation.getName())) {
    		constructorCall = true;
    	}
    	if(!this.methodName.equals(operation.getName()) && !constructorCall) {
    		return false;
    	}
    	Map<String, Set<VariableDeclaration>> variableDeclarationMap = callerOperation.variableDeclarationMap();
    	Map<String, VariableDeclaration> parentFieldDeclarationMap = null;
    	Map<String, VariableDeclaration> childFieldDeclarationMap = null;
    	if(modelDiff != null) {
    		UMLAbstractClass parentCallerClass = modelDiff.findClassInParentModel(callerOperation.getClassName());
    		if(parentCallerClass != null) {
    			parentFieldDeclarationMap = parentCallerClass.getFieldDeclarationMap();
    		}
    		UMLAbstractClass childCallerClass = modelDiff.findClassInChildModel(callerOperation.getClassName());
    		if(childCallerClass != null) {
    			childFieldDeclarationMap = childCallerClass.getFieldDeclarationMap();
    		}
    	}
    	List<UMLType> inferredArgumentTypes = new ArrayList<UMLType>();
    	for(String arg : arguments) {
    		int indexOfOpeningParenthesis = arg.indexOf("(");
    		int indexOfOpeningSquareBracket = arg.indexOf("[");
    		boolean openingParenthesisBeforeSquareBracket = false;
    		boolean openingSquareBracketBeforeParenthesis = false;
    		if(indexOfOpeningParenthesis != -1 && indexOfOpeningSquareBracket != -1) {
    			if(indexOfOpeningParenthesis < indexOfOpeningSquareBracket) {
    				openingParenthesisBeforeSquareBracket = true;
    			}
    			else if(indexOfOpeningSquareBracket < indexOfOpeningParenthesis) {
    				openingSquareBracketBeforeParenthesis = true;
    			}
    		}
    		else if(indexOfOpeningParenthesis != -1 && indexOfOpeningSquareBracket == -1) {
    			openingParenthesisBeforeSquareBracket = true;
    		}
    		else if(indexOfOpeningParenthesis == -1 && indexOfOpeningSquareBracket != -1) {
    			openingSquareBracketBeforeParenthesis = true;
    		}
    		if(variableDeclarationMap.containsKey(arg)) {
    			Set<VariableDeclaration> variableDeclarations = variableDeclarationMap.get(arg);
    			for(VariableDeclaration variableDeclaration : variableDeclarations) {
    				if(variableDeclaration.getScope().subsumes(this.getLocationInfo())) {
    					inferredArgumentTypes.add(variableDeclaration.getType() != null ? variableDeclaration.getType() : null);
    					break;
    				}
    			}
    		}
    		else if((parentFieldDeclarationMap != null && parentFieldDeclarationMap.containsKey(arg)) ||
    				(childFieldDeclarationMap != null && childFieldDeclarationMap.containsKey(arg))) {
    			boolean variableDeclarationFound = false;
    			if(parentFieldDeclarationMap != null && parentFieldDeclarationMap.containsKey(arg)) {
	    			VariableDeclaration variableDeclaration = parentFieldDeclarationMap.get(arg);
	    			if(variableDeclaration.getScope().subsumes(this.getLocationInfo())) {
						inferredArgumentTypes.add(variableDeclaration.getType() != null ? variableDeclaration.getType() : null);
						variableDeclarationFound = true;
					}
    			}
    			if(!variableDeclarationFound && childFieldDeclarationMap != null && childFieldDeclarationMap.containsKey(arg)) {
    				VariableDeclaration variableDeclaration = childFieldDeclarationMap.get(arg);
        			if(variableDeclaration.getScope().subsumes(this.getLocationInfo())) {
    					inferredArgumentTypes.add(variableDeclaration.getType() != null ? variableDeclaration.getType() : null);
    				}
    			}
    		}
    		else if(arg.startsWith("\"") && arg.endsWith("\"")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("String"));
    		}
    		else if(StringDistance.isNumeric(arg)) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("int"));
    		}
    		else if(arg.startsWith("\'") && arg.endsWith("\'")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("char"));
    		}
    		else if(arg.endsWith(".class")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("Class"));
    		}
    		else if(arg.equals("true")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("boolean"));
    		}
    		else if(arg.equals("false")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("boolean"));
    		}
    		else if(arg.startsWith("new ") && arg.contains("(") && openingParenthesisBeforeSquareBracket) {
    			String type = arg.substring(4, arg.indexOf("("));
    			inferredArgumentTypes.add(UMLType.extractTypeObject(type));
    		}
    		else if(arg.startsWith("new ") && arg.contains("[") && openingSquareBracketBeforeParenthesis) {
    			String type = arg.substring(4, arg.indexOf("["));
    			for(int i=0; i<arg.length(); i++) {
    				if(arg.charAt(i) == '[') {
    					type = type + "[]";
    				}
    				else if(arg.charAt(i) == '\n' || arg.charAt(i) == '{') {
    					break;
    				}
    			}
    			inferredArgumentTypes.add(UMLType.extractTypeObject(type));
    		}
    		else if(indexOfOpeningParenthesis == 0 && arg.contains(")") && !arg.contains(JAVA.LAMBDA_ARROW) && !arg.contains(JAVA.METHOD_REFERENCE) && arg.indexOf(")") < arg.length()) {
    			String cast = arg.substring(indexOfOpeningParenthesis + 1, arg.indexOf(")"));
    			if(cast.charAt(0) != '(') {
    				inferredArgumentTypes.add(UMLType.extractTypeObject(cast));
    			}
    		}
    		else if(arg.endsWith(".getClassLoader()")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("ClassLoader"));
    		}
    		else if(arg.contains(JAVA.STRING_CONCATENATION) && !containsMethodSignatureOfAnonymousClass(arg)) {
    			String[] tokens = SPLIT_CONCAT_STRING_PATTERN.split(arg);
    			if(tokens[0].startsWith("\"") && tokens[0].endsWith("\"")) {
    				inferredArgumentTypes.add(UMLType.extractTypeObject("String"));
    			}
    			else {
    				inferredArgumentTypes.add(null);
    			}
    		}
    		else if(arg.contains("[") && openingSquareBracketBeforeParenthesis && arg.lastIndexOf("]") == arg.length() -1) {
    			//array access
    			String arrayVariable = arg.substring(0, indexOfOpeningSquareBracket);
    			if(variableDeclarationMap.containsKey(arrayVariable)) {
        			Set<VariableDeclaration> variableDeclarations = variableDeclarationMap.get(arrayVariable);
        			for(VariableDeclaration variableDeclaration : variableDeclarations) {
        				if(variableDeclaration.getScope().subsumes(this.getLocationInfo())) {
        					UMLType elementType = variableDeclaration.getType() != null ? UMLType.extractTypeObject(variableDeclaration.getType().getClassType()) : null;
        					inferredArgumentTypes.add(elementType);
        					break;
        				}
        			}
        		}
        		else if((parentFieldDeclarationMap != null && parentFieldDeclarationMap.containsKey(arrayVariable)) ||
        				(childFieldDeclarationMap != null && childFieldDeclarationMap.containsKey(arrayVariable))) {
        			boolean variableDeclarationFound = false;
        			if(parentFieldDeclarationMap != null && parentFieldDeclarationMap.containsKey(arrayVariable)) {
    	    			VariableDeclaration variableDeclaration = parentFieldDeclarationMap.get(arrayVariable);
    	    			if(variableDeclaration.getScope().subsumes(this.getLocationInfo())) {
    	    				UMLType elementType = variableDeclaration.getType() != null ? UMLType.extractTypeObject(variableDeclaration.getType().getClassType()) : null;
        					inferredArgumentTypes.add(elementType);
    						variableDeclarationFound = true;
    					}
        			}
        			if(!variableDeclarationFound && childFieldDeclarationMap != null && childFieldDeclarationMap.containsKey(arrayVariable)) {
        				VariableDeclaration variableDeclaration = childFieldDeclarationMap.get(arrayVariable);
            			if(variableDeclaration.getScope().subsumes(this.getLocationInfo())) {
            				UMLType elementType = variableDeclaration.getType() != null ? UMLType.extractTypeObject(variableDeclaration.getType().getClassType()) : null;
        					inferredArgumentTypes.add(elementType);
        				}
        			}
        		}
    		}
    		else {
    			inferredArgumentTypes.add(null);
    		}
    	}
    	int i=0;
    	for(UMLParameter parameter : operation.getParametersWithoutReturnType()) {
    		UMLType parameterType = parameter.getType();
    		if(inferredArgumentTypes.size() > i && inferredArgumentTypes.get(i) != null) {
    			if(!parameterType.getClassType().equals(inferredArgumentTypes.get(i).toString()) &&
    					!parameterType.toString().equals(inferredArgumentTypes.get(i).toString()) &&
    					!compatibleTypes(parameter, inferredArgumentTypes.get(i), classDiff, modelDiff)) {
    				return false;
    			}
    		}
    		i++;
    	}
    	UMLType lastInferredArgumentType = inferredArgumentTypes.size() > 0 ? inferredArgumentTypes.get(inferredArgumentTypes.size()-1) : null;
		return this.numberOfArguments == operation.getParameterTypeList().size() || varArgsMatch(operation, lastInferredArgumentType);
    }

    public static boolean compatibleTypes(UMLParameter parameter, UMLType type, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
    	String type1 = parameter.getType().toString();
    	String type2 = type.toString();
    	if(collectionMatch(parameter.getType(), type))
    		return true;
    	if(type1.equals("Throwable") && type2.endsWith("Exception"))
    		return true;
    	if(type1.equals("Exception") && type2.endsWith("Exception"))
    		return true;
    	if(isPrimitiveType(type1) && isPrimitiveType(type2)) {
            if(isWideningPrimitiveConversion(type2, type1))
                return true;
            else if(isNarrowingPrimitiveConversion(type2, type1))
                return true;
    	}
    	else if(isPrimitiveType(type1) && !isPrimitiveType(type2)) {
    		if(PRIMITIVE_WRAPPER_CLASS_MAP.get(type1).equals(type2))
    			return true;
    	}
    	else if(isPrimitiveType(type2) && !isPrimitiveType(type1)) {
    		if(PRIMITIVE_WRAPPER_CLASS_MAP.get(type2).equals(type1))
    			return true;
    	}
    	//https://docs.oracle.com/javase/specs/jls/se7/html/jls-10.html
    	//The direct superclass of an array type is Object
    	if(type.getArrayDimension() > 0 && type1.equals("Object")) {
    		return true;
    	}
    	if(modelDiff != null) {
	    	UMLAbstractClass subClassInParentModel = modelDiff.findClassInParentModel(type2);
	    	if(!parameter.isVarargs() && subClassInParentModel instanceof UMLClass) {
	    		UMLClass subClass = (UMLClass)subClassInParentModel;
	    		if(subClass.getSuperclass() != null) {
	    			if(subClass.getSuperclass().equalClassType(parameter.getType()))
	    				return true;
	    		}
				for(UMLType implementedInterface : subClass.getImplementedInterfaces()) {
	    			if(implementedInterface.equalClassType(parameter.getType()))
	    				return true;
	    		}
	    	}
	    	UMLAbstractClass subClassInChildModel = modelDiff.findClassInChildModel(type2);
	    	if(!parameter.isVarargs() && subClassInChildModel instanceof UMLClass) {
	    		UMLClass subClass = (UMLClass)subClassInChildModel;
	    		if(subClass.getSuperclass() != null) {
	    			if(subClass.getSuperclass().equalClassType(parameter.getType()))
	    				return true;
	    		}
				for(UMLType implementedInterface : subClass.getImplementedInterfaces()) {
	    			if(implementedInterface.equalClassType(parameter.getType()))
	    				return true;
	    		}
	    	}
    	}
    	if(!parameter.isVarargs() && type1.endsWith("Object") && !type2.endsWith("Object"))
    		return true;
    	if(parameter.isVarargs() && type1.endsWith("Object[]") && (type2.equals("Throwable") || type2.endsWith("Exception")))
    		return true;
    	if(parameter.getType().equalsWithSubType(type))
    		return true;
    	if(parameter.getType().isParameterized() && type.isParameterized() &&
    			parameter.getType().getClassType().equals(type.getClassType()))
    		return true;
    	if(modelDiff != null && modelDiff.isSubclassOf(type.getClassType(), parameter.getType().getClassType())) {
    		return true;
    	}
    	// the super type is available in the modelDiff, but not the subclass type
    	UMLClassBaseDiff subclassDiff = getUMLClassDiff(modelDiff, type);
    	UMLClassBaseDiff superclassDiff = getUMLClassDiff(modelDiff, parameter.getType());
    	if(superclassDiff != null && subclassDiff == null) {
    		return true;
    	}
    	if(classDiff != null && (modelDiff == null || modelDiff.partialModel())) {
    		List<UMLImport> imports = classDiff.getNextClass().getImportedTypes();
			String qualifiedType1Prefix = null;
			String qualifiedType2Prefix = null;
			for(UMLImport umlImport : imports) {
				if(umlImport.getName().endsWith("." + type1)) {
					qualifiedType1Prefix = umlImport.getName().substring(0, umlImport.getName().indexOf("." + type1));
				}
				if(umlImport.getName().endsWith("." + type2)) {
					qualifiedType2Prefix = umlImport.getName().substring(0, umlImport.getName().indexOf("." + type2));
				}
			}
			if(qualifiedType1Prefix != null && qualifiedType2Prefix != null && qualifiedType1Prefix.equals(qualifiedType2Prefix)) {
				return true;
			}
    	}
    	return false;
    }

	private static boolean collectionMatch(UMLType parameterType, UMLType type) {
		if(parameterType.getClassType().equals("Iterable") || parameterType.getClassType().equals("Collection") ) {
			if(type.getClassType().endsWith("List") || type.getClassType().endsWith("Set") || type.getClassType().endsWith("Collection")) {
				if(parameterType.getTypeArguments().equals(type.getTypeArguments())) {
					return true;
				}
				if(parameterType.getTypeArguments().size() == 1) {
					UMLType typeArgument = parameterType.getTypeArguments().get(0);
					if(typeArgument.toString().length() == 1 && Character.isUpperCase(typeArgument.toString().charAt(0))) {
						return true;
					}
				}
			}
		}
		return false;
	}

    private static boolean isWideningPrimitiveConversion(String type1, String type2) {
        return PRIMITIVE_TYPE_WIDENING_MAP.containsKey(type1) && PRIMITIVE_TYPE_WIDENING_MAP.get(type1).contains(type2);
    }

    private static boolean isNarrowingPrimitiveConversion(String type1, String type2) {
        return PRIMITIVE_TYPE_NARROWING_MAP.containsKey(type1) && PRIMITIVE_TYPE_NARROWING_MAP.get(type1).contains(type2);
    }

    private static boolean isPrimitiveType(String argumentTypeClassName) {
        return PRIMITIVE_TYPE_LIST.contains(argumentTypeClassName);
    }

    private static UMLClassBaseDiff getUMLClassDiff(UMLModelDiff modelDiff, UMLType type) {
    	UMLClassBaseDiff classDiff = null;
    	if(modelDiff != null) {
    		classDiff = modelDiff.getUMLClassDiff(type.getClassType());
    		if(classDiff == null) {
    			classDiff = modelDiff.getUMLClassDiff(type);
    		}
    	}
		return classDiff;
    }

    private boolean varArgsMatch(VariableDeclarationContainer operation, UMLType lastInferredArgumentType) {
		//0 varargs arguments passed
		if(this.numberOfArguments == operation.getNumberOfNonVarargsParameters()) {
			return true;
		}
		//>=1 varargs arguments passed
		if(operation.hasVarargsParameter() && this.numberOfArguments > operation.getNumberOfNonVarargsParameters()) {
			List<UMLType> parameterTypeList = operation.getParameterTypeList();
			UMLType lastParameterType = parameterTypeList.get(parameterTypeList.size()-1);
			if(lastParameterType.equals(lastInferredArgumentType)) {
				return true;
			}
			if(lastInferredArgumentType != null && lastParameterType.getClassType().equals(lastInferredArgumentType.getClassType())) {
				return true;
			}
		}
		return false;
    }

    public boolean compatibleExpression(OperationInvocation other) {
    	if(this.expression != null && other.expression != null) {
    		if(this.expression.startsWith("new ") && !other.expression.startsWith("new "))
    			return false;
    		if(!this.expression.startsWith("new ") && other.expression.startsWith("new "))
    			return false;
    	}
    	if(this.expression != null && this.expression.startsWith("new ") && other.expression == null)
    		return false;
    	if(other.expression != null && other.expression.startsWith("new ") && this.expression == null)
    		return false;
    	if(this.subExpressions.size() > 1 || other.subExpressions.size() > 1) {
    		Set<String> intersection = subExpressionIntersection(other);
    		int thisUnmatchedSubExpressions = this.subExpressions().size() - intersection.size();
    		int otherUnmatchedSubExpressions = other.subExpressions().size() - intersection.size();
    		if(thisUnmatchedSubExpressions > intersection.size() || otherUnmatchedSubExpressions > intersection.size())
    			return false;
    	}
    	return true;
    }

    public Set<String> callChainIntersection(OperationInvocation other) {
    	Set<String> s1 = new LinkedHashSet<String>(this.subExpressions);
    	s1.add(this.actualString());
    	Set<String> s2 = new LinkedHashSet<String>(other.subExpressions);
    	s2.add(other.actualString());

    	Set<String> intersection = new LinkedHashSet<String>(s1);
    	intersection.retainAll(s2);
    	return intersection;
    }

    private Set<String> subExpressionIntersection(OperationInvocation other) {
    	Set<String> subExpressions1 = this.subExpressions();
    	Set<String> subExpressions2 = other.subExpressions();
    	Set<String> intersection = new LinkedHashSet<String>(subExpressions1);
    	intersection.retainAll(subExpressions2);
    	if(subExpressions1.size() == subExpressions2.size()) {
    		Iterator<String> it1 = subExpressions1.iterator();
    		Iterator<String> it2 = subExpressions2.iterator();
    		while(it1.hasNext()) {
    			String subExpression1 = it1.next();
    			String subExpression2 = it2.next();
    			if(!intersection.contains(subExpression1) && differInThisDot(subExpression1, subExpression2)) {
    				intersection.add(subExpression1);
    			}
    		}
    	}
    	return intersection;
    }

	private static boolean differInThisDot(String subExpression1, String subExpression2) {
		if(subExpression1.length() < subExpression2.length()) {
			String modified = subExpression1;
			String previousCommonPrefix = "";
			String commonPrefix = null;
			while((commonPrefix = PrefixSuffixUtils.longestCommonPrefix(modified, subExpression2)).length() > previousCommonPrefix.length()) {
				modified = commonPrefix + JAVA.THIS_DOT + modified.substring(commonPrefix.length(), modified.length());
				if(modified.equals(subExpression2)) {
					return true;
				}
				previousCommonPrefix = commonPrefix;
			}
		}
		else if(subExpression1.length() > subExpression2.length()) {
			String modified = subExpression2;
			String previousCommonPrefix = "";
			String commonPrefix = null;
			while((commonPrefix = PrefixSuffixUtils.longestCommonPrefix(modified, subExpression1)).length() > previousCommonPrefix.length()) {
				modified = commonPrefix + JAVA.THIS_DOT + modified.substring(commonPrefix.length(), modified.length());
				if(modified.equals(subExpression1)) {
					return true;
				}
				previousCommonPrefix = commonPrefix;
			}
		}
		return false;
	}

	private Set<String> subExpressions() {
		Set<String> subExpressions = new LinkedHashSet<String>(this.subExpressions);
		String thisExpression = this.expression;
		if(thisExpression != null) {
			if(thisExpression.contains(".")) {
				int start = 0;
				int indexOfDot = 0;
				while(start < thisExpression.length() && (indexOfDot = thisExpression.indexOf(".", start)) != -1) {
					String subString = thisExpression.substring(start, indexOfDot);
					if(!subExpressions.contains(subString) && !dotInsideArguments(indexOfDot, thisExpression)) {
						subExpressions.add(subString);
					}
					start = indexOfDot+1;
				}
			}
			else if(!subExpressions.contains(thisExpression)) {
				subExpressions.add(thisExpression);
			}
		}
		return subExpressions;
	}

	private static boolean dotInsideArguments(int indexOfDot, String thisExpression) {
		boolean openingParenthesisFound = false;
		for(int i=indexOfDot; i>=0; i--) {
			if(thisExpression.charAt(i) == '(') {
				openingParenthesisFound = true;
				break;
			}
		}
		boolean closingParenthesisFound = false;
		for(int i=indexOfDot; i<thisExpression.length(); i++) {
			if(thisExpression.charAt(i) == ')') {
				closingParenthesisFound = true;
				break;
			}
		}
		return openingParenthesisFound && closingParenthesisFound;
	}

	public double normalizedNameDistance(AbstractCall call) {
		String s1 = getMethodName().toLowerCase();
		String s2 = call.getName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof OperationInvocation) {
        	OperationInvocation invocation = (OperationInvocation)o;
            return methodName.equals(invocation.methodName) &&
                numberOfArguments == invocation.numberOfArguments &&
                (this.expression != null) == (invocation.expression != null);
        }
        else if (o instanceof MethodReference) {
        	MethodReference invocation = (MethodReference)o;
            return methodName.equals(invocation.getMethodName()) &&
                numberOfArguments == invocation.numberOfArguments &&
                (this.expression != null) == (invocation.expression != null);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        sb.append("(");
        if(numberOfArguments > 0) {
            for(int i=0; i<numberOfArguments-1; i++)
                sb.append("arg" + i).append(", ");
            sb.append("arg" + (numberOfArguments-1));
        }
        sb.append(")");
        return sb.toString();
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + expression != null ? 1 : 0;
    		result = 37*result + methodName.hashCode();
    		result = 37*result + numberOfArguments;
    		hashCode = result;
    	}
    	return hashCode;
    }

	public boolean identicalName(AbstractCall call) {
		return getMethodName().equals(call.getName());
	}

	public boolean typeInferenceMatch(UMLOperation operationToBeMatched, Map<String, UMLType> typeInferenceMapFromContext) {
		List<UMLParameter> parameters = operationToBeMatched.getParametersWithoutReturnType();
		if(operationToBeMatched.hasVarargsParameter()) {
			//we expect arguments to be =(parameters-1), or =parameters, or >parameters
			if(arguments().size() < parameters.size()) {
				int i = 0;
				for(String argument : arguments()) {
					if(typeInferenceMapFromContext.containsKey(argument)) {
						UMLType argumentType = typeInferenceMapFromContext.get(argument);
						UMLType paremeterType = parameters.get(i).getType();
						if(!argumentType.equals(paremeterType))
							return false;
					}
					i++;
				}
			}
			else {
				int i = 0;
				for(UMLParameter parameter : parameters) {
					String argument = arguments().get(i);
					if(typeInferenceMapFromContext.containsKey(argument)) {
						UMLType argumentType = typeInferenceMapFromContext.get(argument);
						UMLType paremeterType = parameter.isVarargs() ?
								UMLType.extractTypeObject(parameter.getType().getClassType()) :
								parameter.getType();
						if(!argumentType.equals(paremeterType))
							return false;
					}
					i++;
				}
			}
			
		}
		else {
			//we expect an equal number of parameters and arguments
			int i = 0;
			for(String argument : arguments()) {
				if(typeInferenceMapFromContext.containsKey(argument)) {
					UMLType argumentType = typeInferenceMapFromContext.get(argument);
					UMLType paremeterType = parameters.get(i).getType();
					if(!argumentType.equals(paremeterType))
						return false;
				}
				i++;
			}
		}
		return true;
	}

	private int subExpressionsWithStringLiteralArgument() {
		int count = 0;
		for(String subExpression : subExpressions) {
			if(subExpression.contains("(") && subExpression.contains(")")) {
				int startIndex = subExpression.indexOf("(") + 1;
				int endIndex = subExpression.lastIndexOf(")");
				String argument = subExpression.substring(startIndex, endIndex);
				if(isStringLiteral(argument)) {
					count++;
				}
			}
		}
		return count;
	}

	private Set<String> parametersUsedInSubExpressionArguments() {
		List<String> parameterNames = container.getParameterNameList();
		Set<String> matchedParameterNames = new LinkedHashSet<>();
		for(String subExpression : subExpressions) {
			if(subExpression.contains("(") && subExpression.contains(")")) {
				int startIndex = subExpression.indexOf("(") + 1;
				int endIndex = subExpression.lastIndexOf(")");
				String argument = subExpression.substring(startIndex, endIndex);
				if(!argument.isEmpty()) {
					boolean found = false;
					for(String parameterName : parameterNames) {
						if(argument.contains(parameterName)) {
							matchedParameterNames.add(parameterName);
							found = true;
							break;
						}
					}
					if(!found) {
						Map<String, Set<VariableDeclaration>> map = container.variableDeclarationMap();
						for(String variableName : map.keySet()) {
							if(argument.contains(variableName)) {
								Set<VariableDeclaration> variableDeclarations = map.get(variableName);
								for(VariableDeclaration variableDeclaration : variableDeclarations) {
									if(variableDeclaration.getInitializer() != null) {
										for(String parameterName : parameterNames) {
											if(variableDeclaration.getInitializer().getString().contains(parameterName)) {
												matchedParameterNames.add(parameterName);
												found = true;
												break;
											}
										}
										if(found) {
											break;
										}
									}
								}
								if(found) {
									break;
								}
							}
						}
					}
				}
			}
		}
		return matchedParameterNames;
	}

	public boolean identicalWithExpressionCallChainDifference(OperationInvocation other) {
		Set<String> subExpressionIntersection = subExpressionIntersection(other);
		if((identicalName(other) || compatibleName(other)) &&
				(equalArguments(other) || equalArgumentsExceptForStringLiterals(other)) &&
				subExpressionIntersection.size() > 0) {
			if(subExpressionIntersection.size() >= this.subExpressions.size() - this.subExpressionsWithStringLiteralArgument() ||
					subExpressionIntersection.size() >= other.subExpressions.size() - other.subExpressionsWithStringLiteralArgument()) {
				return true;
			}
			Set<String> parametersInArguments1 = this.parametersUsedInSubExpressionArguments();
			Set<String> parametersInArguments2 = other.parametersUsedInSubExpressionArguments();
			if(parametersInArguments1.equals(parametersInArguments2)) {
				if(subExpressionIntersection.size() >= this.subExpressions.size() - parametersInArguments1.size() ||
						subExpressionIntersection.size() >= other.subExpressions.size() - parametersInArguments2.size()) {
					return true;
				}
			}
		}
		return false;
	}

	public String subExpressionIsCallToSameMethod() {
		for(String expression : subExpressions) {
			if(expression.startsWith(this.getName() + "(")) {
				return expression;
			}
		}
		return null;
	}
}
