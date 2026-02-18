package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.InferredType;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;

import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.containsMethodSignatureOfAnonymousClass;
import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression;
import org.jetbrains.kotlin.psi.KtTypeProjection;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import extension.ast.node.LangASTNode;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangFieldAccess;
import extension.ast.node.expression.LangInfixExpression;
import extension.ast.node.expression.LangMethodInvocation;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.unit.LangCompilationUnit;
import extension.ast.visitor.LangVisitor;

public class OperationInvocation extends AbstractCall {
	private String methodName;
	private List<String> subExpressions = new ArrayList<String>();
	private volatile int hashCode = 0;
	public static Map<String, String> PRIMITIVE_WRAPPER_CLASS_MAP;
    private static Map<String, List<String>> PRIMITIVE_TYPE_WIDENING_MAP;
    private static Map<String, List<String>> PRIMITIVE_TYPE_NARROWING_MAP;
    private static List<String> PRIMITIVE_TYPE_LIST;
    private final Pattern LAMBDA_ARROW = Pattern.compile(LANG.LAMBDA_ARROW);

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

	public OperationInvocation(CompilationUnit cu, String sourceFolder, String filePath, MethodInvocation invocation, VariableDeclarationContainer container, String javaFileContent) {
		super(cu, sourceFolder, filePath, invocation, CodeElementType.METHOD_INVOCATION, container);
		this.methodName = invocation.getName().getIdentifier();
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Type> typeArgs = invocation.typeArguments();
		for(Type typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, typeArg, 0, javaFileContent));
		}
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
		if(invocation.getExpression() != null) {
			this.expression = stringify(invocation.getExpression());
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}

	public OperationInvocation(LangCompilationUnit cu, String sourceFolder, String filePath, LangMethodInvocation methodInvocation, VariableDeclarationContainer container, String fileContent) {
		super(cu, sourceFolder, filePath, methodInvocation, CodeElementType.METHOD_INVOCATION, container);
		this.methodName = "closure";
		if (methodInvocation.getExpression() instanceof LangSimpleName simpleName) {
			this.methodName = simpleName.getIdentifier();
		} else if (methodInvocation.getExpression() instanceof LangFieldAccess fieldAccess) {
			this.methodName = fieldAccess.getName().getIdentifier();
			this.expression = LangVisitor.stringify(fieldAccess.getExpression()); // "self"
			if (fieldAccess.getExpression() instanceof LangSimpleName simpleName) {
				// CORRECT: Set expression to just the object part
				this.expression = simpleName.getIdentifier(); // "self"
				// The method name is already set via this.methodName = methodInvocation.extractMethodName()
			}
		}
		// FIX: Handle null arguments list
		if (methodInvocation.getArguments() != null) {
			this.numberOfArguments = methodInvocation.getArguments().size();
		} else {
			this.numberOfArguments = 0;
		}
		this.arguments = new ArrayList<>();
		if (methodInvocation.getArguments() != null) {
			for (LangASTNode argument : methodInvocation.getArguments()) {
				String argString = LangVisitor.stringify(argument);
				this.arguments.add(argString);
			}
		}
		if (methodInvocation.getExpression() != null) {
			processExpression(methodInvocation.getExpression(), this.subExpressions);
		}
	}

	private void processExpression(LangASTNode node, List<String> subExpressions) {
		if (node instanceof LangMethodInvocation methodInvocation) {
			LangASTNode expr = methodInvocation.getExpression();
			if (expr != null) {
				String exprAsString = null;
				if(expr instanceof LangFieldAccess fieldAccess) {
					exprAsString = LangVisitor.stringify(fieldAccess.getExpression());
				}
				else {
					exprAsString = LangVisitor.stringify(methodInvocation);
				}
				String invocationAsString = LangVisitor.stringify(methodInvocation);

				// The suffix is the part after the receiver, including the operator (like ".add(x,y)")
				if (invocationAsString.length() > exprAsString.length() + 1) {
					String suffix = invocationAsString.substring(exprAsString.length() + 1);
					subExpressions.add(0, suffix);
				} else {
					subExpressions.add(0, invocationAsString);
				}

				// Process the expression, but handle field access specially
				processExpression(expr, subExpressions);
			} else {
				subExpressions.add(0, LangVisitor.stringify(methodInvocation));
			}
		}
		else if (node instanceof LangFieldAccess fieldAccess) {
			// For "self.add", add just "self" as a variable, not "self.add"
			LangASTNode expression = fieldAccess.getExpression();
			if (expression != null) {
				processExpression(expression, subExpressions);
			}
			// Don't add the field access itself - we already handled the method call above
		}
		else if (node instanceof LangSimpleName simpleName) {
			// Add simple names as variables (like "self", "x", "y")
			// add only if next character is "." and not if next character is "("
			String parentAsString = LangVisitor.stringify(node.getParent());
			if(parentAsString.contains(simpleName.getIdentifier() + ".") && !parentAsString.contains(simpleName.getIdentifier() + "(")) {
				subExpressions.add(0, simpleName.getIdentifier());
			}
		}
		else if (node instanceof LangAssignment assignment) {
			processExpression(assignment.getLeftSide(), subExpressions);
			processExpression(assignment.getRightSide(), subExpressions);
		}
		else if (node instanceof LangInfixExpression infixExpr) {
			processExpression(infixExpr.getLeft(), subExpressions);
			processExpression(infixExpr.getRight(), subExpressions);
		}
		// TODO: Add other node types as needed...
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

	public OperationInvocation(CompilationUnit cu, String sourceFolder, String filePath, SuperMethodInvocation invocation, VariableDeclarationContainer container, String javaFileContent) {
		super(cu, sourceFolder, filePath, invocation, CodeElementType.SUPER_METHOD_INVOCATION, container);
		this.methodName = invocation.getName().getIdentifier();
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Type> typeArgs = invocation.typeArguments();
		for(Type typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, typeArg, 0, javaFileContent));
		}
		this.expression = "super";
		this.subExpressions.add("super");
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
	}

	public OperationInvocation(CompilationUnit cu, String sourceFolder, String filePath, SuperConstructorInvocation invocation, VariableDeclarationContainer container, String javaFileContent) {
		super(cu, sourceFolder, filePath, invocation, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION, container);
		this.methodName = "super";
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Type> typeArgs = invocation.typeArguments();
		for(Type typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, typeArg, 0, javaFileContent));
		}
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
		if(invocation.getExpression() != null) {
			this.expression = stringify(invocation.getExpression());
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}

	public OperationInvocation(CompilationUnit cu, String sourceFolder, String filePath, ConstructorInvocation invocation, VariableDeclarationContainer container) {
		super(cu, sourceFolder, filePath, invocation, CodeElementType.CONSTRUCTOR_INVOCATION, container);
		this.methodName = LANG.THIS;
		this.numberOfArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
	}

	private OperationInvocation(LocationInfo locationInfo) {
		super(locationInfo);
	}

	public OperationInvocation update(String oldExpression, String newExpression) {
		OperationInvocation newOperationInvocation = new OperationInvocation(this.locationInfo);
		newOperationInvocation.methodName = this.methodName;
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
    	String operationName = operation.getName();
    	if(operationName.contains(".")) {
    		operationName = operationName.substring(operationName.lastIndexOf(".") + 1, operationName.length());
    	}
		if(this.methodName.equals(LANG.THIS) && operation.getClassName().equals(callerOperation.getClassName()) && operationName.equals(callerOperation.getName())) {
    		constructorCall = true;
    	}
    	if(!this.methodName.equals(operationName) && !constructorCall) {
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
    		else if(arg.equals(LANG.TRUE)) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("boolean"));
    		}
    		else if(arg.equals(LANG.FALSE)) {
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
    		else if(indexOfOpeningParenthesis == 0 && arg.contains(")") && !arg.contains(LANG.LAMBDA_ARROW) && !arg.contains(LANG.METHOD_REFERENCE) && arg.indexOf(")") < arg.length()) {
    			String cast = arg.substring(indexOfOpeningParenthesis + 1, arg.indexOf(")"));
    			if(cast.charAt(0) != '(') {
    				inferredArgumentTypes.add(UMLType.extractTypeObject(cast));
    			}
    			else {
    				inferredArgumentTypes.add(null);
    			}
    		}
    		else if(arg.endsWith(".getClassLoader()")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("ClassLoader"));
    		}
    		else if(arg.contains(LANG.STRING_CONCATENATION) && !containsMethodSignatureOfAnonymousClass(arg, LANG)) {
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
    			String numberType = handleNumber(arg);
    			if(numberType != null) {
    				inferredArgumentTypes.add(UMLType.extractTypeObject(numberType));
    			}
    			else {
    				UMLType returnType = null;
    				if(classDiff != null) {
    					for(UMLOperation originalClassOperation : classDiff.getOriginalClass().getOperations()) {
    						if(arg.startsWith(originalClassOperation.getName() + "(") && originalClassOperation.getReturnParameter() != null &&
    								!originalClassOperation.getReturnParameter().getType().getClassType().equals("void")) {
    							returnType = originalClassOperation.getReturnParameter().getType();
    							break;
    						}
    					}
    				}
    				inferredArgumentTypes.add(returnType);
    			}
    		}
    	}
    	int i=0;
    	int originalExactlyMatchingArguments = 0;
    	int parametersWithDefaultValue = 0;
    	for(UMLParameter parameter : operation.getParametersWithoutReturnType()) {
    		// handle parameters with a default value
    		if(parameter.getVariableDeclaration().getInitializer() != null) {
    			parametersWithDefaultValue++;
    			continue;
    		}
    		UMLType parameterType = parameter.getType();
    		if(inferredArgumentTypes.size() > i && inferredArgumentTypes.get(i) != null) {
    			if(exactlyMatchingArgumentType(parameterType, inferredArgumentTypes.get(i))) {
    				originalExactlyMatchingArguments++;
    			}
    			if(!parameterType.getClassType().equals(inferredArgumentTypes.get(i).toString()) &&
    					!parameterType.toString().equals(inferredArgumentTypes.get(i).toString()) &&
    					!compatibleTypes(parameter, inferredArgumentTypes.get(i), classDiff, modelDiff)) {
    				return false;
    			}
    		}
    		i++;
    	}
    	UMLType lastInferredArgumentType = inferredArgumentTypes.size() > 0 ? inferredArgumentTypes.get(inferredArgumentTypes.size()-1) : null;
		List<UMLType> parameterTypeList = operation.getParameterTypeList();
		boolean result = this.numberOfArguments == parameterTypeList.size() ||
				(parametersWithDefaultValue > 0 && this.numberOfArguments + parametersWithDefaultValue >= parameterTypeList.size()) ||
				varArgsMatch(operation, lastInferredArgumentType, parameterTypeList);
		if(result && classDiff != null) {
			for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
				if(!addedOperation.equals(operation) && addedOperation.getName().equals(operationName) && addedOperation.getParameterDeclarationList().size() == operation.getParameterDeclarationList().size()) {
					int j = 0;
					int exactlyMatchingArguments = 0;
					for(UMLParameter parameter : addedOperation.getParametersWithoutReturnType()) {
						UMLType parameterType = parameter.getType();
						if(inferredArgumentTypes.size() > j && inferredArgumentTypes.get(j) != null) {
							if(exactlyMatchingArgumentType(parameterType, inferredArgumentTypes.get(j))) {
								exactlyMatchingArguments++;
							}
						}
						j++;
					}
					if(exactlyMatchingArguments > originalExactlyMatchingArguments) {
						return false;
					}
				}
			}
			if(modelDiff != null) {
				for(Refactoring r : modelDiff.getDetectedRefactorings()) {
					if(r instanceof ExtractOperationRefactoring) {
						ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
						UMLOperation addedOperation = extract.getExtractedOperation();
						if(!addedOperation.equals(operation) && addedOperation.getName().equals(operationName) && addedOperation.getParameterDeclarationList().size() == operation.getParameterDeclarationList().size()) {
							int j = 0;
							int exactlyMatchingArguments = 0;
							for(UMLParameter parameter : addedOperation.getParametersWithoutReturnType()) {
								UMLType parameterType = parameter.getType();
								if(inferredArgumentTypes.size() > j && inferredArgumentTypes.get(j) != null) {
									if(exactlyMatchingArgumentType(parameterType, inferredArgumentTypes.get(j))) {
										exactlyMatchingArguments++;
									}
								}
								j++;
							}
							if(exactlyMatchingArguments > originalExactlyMatchingArguments) {
								return false;
							}
						}
					}
				}
			}
		}
		if(result && operation instanceof UMLOperation && ((UMLOperation) operation).isStatic()) {
			if(expression != null) {
				return operation.getClassName().endsWith("." + expression) || operation.getClassName().equals(expression) || expression.equals(LANG.THIS);
			}
			else {
				if(classDiff != null && classDiff.getNextClass().importsType(operation.getClassName() + "." + operationName)) {
					return true;
				}
				if(classDiff != null && classDiff.getNextClass().getSuperclass() != null && modelDiff != null) {
					UMLClassBaseDiff superClassDiff = modelDiff.getUMLClassDiff(classDiff.getNextClass().getSuperclass());
					if(superClassDiff != null && superClassDiff.getNextClass().importsType(operation.getClassName())) {
						return true;
					}
				}
				return callerOperation.getClassName().equals(operation.getClassName()) || callerOperation.getClassName().startsWith(operation.getClassName());
			}
		}
		return result;
    }

	private static boolean exactlyMatchingArgumentType(UMLType parameterType, UMLType argumentType) {
		return parameterType.getClassType().equals(argumentType.toString()) || parameterType.toString().equals(argumentType.toString());
	}

	private static String handleNumber(String argument) {
		try {
		    Integer.parseInt(argument);
		    return "int";
		} catch (NumberFormatException e) {}
		try {
		    Long.parseLong(argument);
		    return "long";
		} catch (NumberFormatException e) {}
		try {
		    Float.parseFloat(argument);
		    return "float";
		} catch (NumberFormatException e) {}
		try {
		    Double.parseDouble(argument);
		    return "double";
		} catch (NumberFormatException e) {}
		return null;
	}

    public static boolean compatibleTypes(UMLParameter parameter, UMLType type, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) {
    	UMLType parameterType = parameter.getType();
    	boolean varargsParameter = parameter.isVarargs();
		return compatibleTypes(parameterType, type, classDiff, modelDiff, varargsParameter);
    }

	public static boolean compatibleTypes(UMLType parameterType, UMLType type, UMLAbstractClassDiff classDiff,
			UMLModelDiff modelDiff, boolean varargsParameter) {
		String type1 = parameterType.toString();
    	String type2 = type.toString();
    	if(type instanceof InferredType || parameterType instanceof InferredType)
    		return true;
    	if(parameterType.getClassType().length() == 1) {
    		return true;
    	}
    	if(type2.equals("var")) {
    		return true;
    	}
    	if(collectionMatch(parameterType, type))
    		return true;
    	if(type1.equals("Throwable") && type2.endsWith("Exception"))
    		return true;
    	if(type1.equals("Exception") && type2.endsWith("Exception"))
    		return true;
    	if(type1.equals("Statement") && type2.equals("Fail"))
    		return true;
    	if(type1.equals("CharSequence") && type2.equals("String"))
    		return true;
    	else if(type2.equals("CharSequence") && type1.equals("String"))
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
	    	if(!varargsParameter && subClassInParentModel instanceof UMLClass) {
	    		UMLClass subClass = (UMLClass)subClassInParentModel;
	    		if(subClass.getSuperclass() != null) {
	    			if(subClass.getSuperclass().equalClassType(parameterType))
	    				return true;
	    		}
				for(UMLType implementedInterface : subClass.getImplementedInterfaces()) {
	    			if(implementedInterface.equalClassType(parameterType))
	    				return true;
	    		}
	    	}
	    	UMLAbstractClass subClassInChildModel = modelDiff.findClassInChildModel(type2);
	    	if(!varargsParameter && subClassInChildModel instanceof UMLClass) {
	    		UMLClass subClass = (UMLClass)subClassInChildModel;
	    		if(subClass.getSuperclass() != null) {
	    			if(subClass.getSuperclass().equalClassType(parameterType))
	    				return true;
	    		}
				for(UMLType implementedInterface : subClass.getImplementedInterfaces()) {
	    			if(implementedInterface.equalClassType(parameterType))
	    				return true;
	    		}
	    	}
    	}
    	if(!varargsParameter && type1.endsWith("Object") && !type2.endsWith("Object"))
    		return true;
    	if(varargsParameter && type1.endsWith("Object[]") && (type2.equals("Throwable") || type2.endsWith("Exception") || isPrimitiveType(type2) || type2.equals("String")))
    		return true;
    	if(parameterType.equalsWithSubType(type))
    		return true;
    	if(parameterType.isParameterized() && type.isParameterized() &&
    			parameterType.getClassType().equals(type.getClassType()))
    		return true;
    	if(modelDiff != null && modelDiff.isSubclassOf(type.getClassType(), parameterType.getClassType())) {
    		return true;
    	}
    	// the super type is available in the modelDiff, but not the subclass type
    	UMLClassBaseDiff subclassDiff = getUMLClassDiff(modelDiff, type);
    	UMLClassBaseDiff superclassDiff = getUMLClassDiff(modelDiff, parameterType);
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
		if(parameterType.getClassType().equals("Iterable") || parameterType.getClassType().equals("Collection") ||
				parameterType.getClassType().equals("List") || parameterType.getClassType().equals("Set")) {
			if(type.getClassType().endsWith("List") || type.getClassType().endsWith("Set") || type.getClassType().endsWith("Collection")) {
				if(parameterType.getTypeArguments().equals(type.getTypeArguments()) || type.getTypeArguments().isEmpty()) {
					return true;
				}
				if(parameterType.getTypeArguments().size() == 1) {
					UMLType typeArgument = parameterType.getTypeArguments().get(0);
					if(typeArgument.toString().length() == 1 && Character.isUpperCase(typeArgument.toString().charAt(0))) {
						return true;
					}
					if(type.getTypeArguments().size() == 1) {
						UMLType typeArgument2 = type.getTypeArguments().get(0);
						if(typeArgument2.getClassType().equals(typeArgument.getClassType())) {
							return true;
						}
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

    private boolean varArgsMatch(VariableDeclarationContainer operation, UMLType lastInferredArgumentType, List<UMLType> parameterTypeList) {
		//0 varargs arguments passed
		if(this.numberOfArguments == operation.getNumberOfNonVarargsParameters()) {
			return true;
		}
		//>=1 varargs arguments passed
		if(operation.hasVarargsParameter() && this.numberOfArguments > operation.getNumberOfNonVarargsParameters()) {
			UMLType lastParameterType = parameterTypeList.get(parameterTypeList.size()-1);
			if(lastParameterType.equals(lastInferredArgumentType)) {
				return true;
			}
			if(lastInferredArgumentType != null && lastParameterType.getClassType().equals(lastInferredArgumentType.getClassType())) {
				return true;
			}
			List<UMLParameter> params = operation.getParametersWithoutReturnType();
			if(lastInferredArgumentType != null && compatibleTypes(params.get(params.size()-1), lastInferredArgumentType, null, null)) {
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

	private boolean differInThisDot(String subExpression1, String subExpression2) {
		if(subExpression1.length() < subExpression2.length()) {
			String modified = subExpression1;
			String previousCommonPrefix = "";
			String commonPrefix = null;
			while((commonPrefix = PrefixSuffixUtils.longestCommonPrefix(modified, subExpression2)).length() > previousCommonPrefix.length()) {
				modified = commonPrefix + LANG.THIS_DOT + modified.substring(commonPrefix.length(), modified.length());
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
				modified = commonPrefix + LANG.THIS_DOT + modified.substring(commonPrefix.length(), modified.length());
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
		if((identicalName(other) || compatibleName(other, false)) &&
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

	public boolean identicalPipeline(OperationInvocation other) {
		if(this.expression != null && other.expression != null) {
			Matcher m1 = LAMBDA_ARROW.matcher(this.expression);
			Matcher m2 = LAMBDA_ARROW.matcher(other.expression);
			List<String> lambdaCalls1 = extractLambdaCalls(m1, this.expression);
			List<String> lambdaCalls2 = extractLambdaCalls(m2, other.expression);
			if(lambdaCalls1.equals(lambdaCalls2) && lambdaCalls1.size() > 0) {
				return true;
			}
		}
		return false;
	}

	private static List<String> extractLambdaCalls(Matcher m, String expression) {
		List<String> lambdaCalls = new ArrayList<String>();
		int start1 = 0;
		while (m.find()) {
		    int start = m.start();
		    String subString = expression.substring(start1, start);
		    if(subString.contains(".")) {
		    	String s = subString.substring(subString.lastIndexOf("."), subString.length());
		    	lambdaCalls.add(s);
		    }
		    start1 = m.end();
		}
		return lambdaCalls;
	}

	public String subExpressionIsCallToSameMethod() {
		for(String expression : subExpressions) {
			if(expression.startsWith(this.getName() + "(")) {
				return expression;
			}
		}
		return null;
	}

	public OperationInvocation(KtFile cu, String sourceFolder, String filePath, KtCallExpression invocation, VariableDeclarationContainer container, String fileContent) {
		super(cu, sourceFolder, filePath, input(invocation), CodeElementType.METHOD_INVOCATION, container);
		KtExpression calleeExpression = invocation.getCalleeExpression();
		if(calleeExpression instanceof KtNameReferenceExpression nameReference) {
			this.methodName = nameReference.getReferencedName();
		}
		else if(calleeExpression instanceof KtLambdaExpression lambda) {
			this.methodName = "lambda";
		}
		this.numberOfArguments = invocation.getValueArguments().size();
		this.arguments = new ArrayList<String>();
		List<KtTypeProjection> typeArgs = invocation.getTypeArguments();
		for(KtTypeProjection typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, fileContent, typeArg, 0));
		}
		List<KtValueArgument> args = invocation.getValueArguments();
		for(KtValueArgument argument : args) {
			// TODO replace with stringify
			this.arguments.add(argument.getText());
		}
		if(invocation.getParent() instanceof KtDotQualifiedExpression dotQualifiedExpression) {
			KtExpression receiver = dotQualifiedExpression.getReceiverExpression();
			if(receiver != null) {
				// TODO replace with stringify
				this.expression = receiver.getText();
				processExpression(receiver, this.subExpressions);
			}
		}
		else if(invocation.getParent() instanceof KtSafeQualifiedExpression safeQualifiedExpression) {
			KtExpression receiver = safeQualifiedExpression.getReceiverExpression();
			if(receiver != null) {
				// TODO replace with stringify
				this.expression = receiver.getText();
				processExpression(receiver, this.subExpressions);
			}
		}
	}

	public OperationInvocation(KtFile cu, String sourceFolder, String filePath, KtDotQualifiedExpression invocation, VariableDeclarationContainer container, String fileContent) {
		super(cu, sourceFolder, filePath, invocation, CodeElementType.METHOD_INVOCATION, container);
		this.methodName = invocation.getSelectorExpression().getText();
		this.numberOfArguments = 0;
		this.arguments = new ArrayList<String>();
		KtExpression receiver = invocation.getReceiverExpression();
		if(receiver != null) {
			// TODO replace with stringify
			this.expression = receiver.getText();
			processExpression(receiver, this.subExpressions);
		}
	}

	private static KtExpression input(KtCallExpression invocation) {
		if(invocation.getParent() instanceof KtDotQualifiedExpression dotQualifiedExpression)
			return dotQualifiedExpression;
		if(invocation.getParent() instanceof KtSafeQualifiedExpression safeQualifiedExpression)
			return safeQualifiedExpression;
		return invocation;
	}

	private void processExpression(KtExpression expression, List<String> subExpressions) {
		if(expression instanceof KtDotQualifiedExpression dotQualified) {
			String expressionAsString = dotQualified.getReceiverExpression().getText();
			String invocationAsString = expression.getText();
			String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
			subExpressions.add(0, suffix);
			processExpression(dotQualified.getReceiverExpression(), subExpressions);
		}
		else if(expression instanceof KtNameReferenceExpression nameReference) {
			subExpressions.add(0, nameReference.getText());
		}
	}
}
