package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.ArrayList;
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
	
	public OperationInvocation(CompilationUnit cu, String filePath, MethodInvocation invocation) {
		this.locationInfo = new LocationInfo(cu, filePath, invocation, CodeElementType.METHOD_INVOCATION);
		this.methodName = invocation.getName().getIdentifier();
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
		if(invocation.getExpression() != null) {
			this.expression = invocation.getExpression().toString();
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}
	
	private void processExpression(Expression expression, List<String> subExpressions) {
		if(expression instanceof MethodInvocation) {
			MethodInvocation invocation = (MethodInvocation)expression;
			if(invocation.getExpression() != null) {
				String expressionAsString = invocation.getExpression().toString();
				String invocationAsString = invocation.toString();
				String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
				subExpressions.add(0, suffix);
				processExpression(invocation.getExpression(), subExpressions);
			}
			else {
				subExpressions.add(0, invocation.toString());
			}
		}
		else if(expression instanceof ClassInstanceCreation) {
			ClassInstanceCreation creation = (ClassInstanceCreation)expression;
			if(creation.getExpression() != null) {
				String expressionAsString = creation.getExpression().toString();
				String invocationAsString = creation.toString();
				String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
				subExpressions.add(0, suffix);
				processExpression(creation.getExpression(), subExpressions);
			}
			else {
				subExpressions.add(0, creation.toString());
			}
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, SuperMethodInvocation invocation) {
		this.locationInfo = new LocationInfo(cu, filePath, invocation, CodeElementType.SUPER_METHOD_INVOCATION);
		this.methodName = invocation.getName().getIdentifier();
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		this.expression = "super";
		this.subExpressions.add("super");
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, SuperConstructorInvocation invocation) {
		this.locationInfo = new LocationInfo(cu, filePath, invocation, CodeElementType.SUPER_CONSTRUCTOR_INVOCATION);
		this.methodName = "super";
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
		if(invocation.getExpression() != null) {
			this.expression = invocation.getExpression().toString();
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, ConstructorInvocation invocation) {
		this.locationInfo = new LocationInfo(cu, filePath, invocation, CodeElementType.CONSTRUCTOR_INVOCATION);
		this.methodName = "this";
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
	}

	private OperationInvocation() {
		
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

    public int numberOfSubExpressions() {
    	return subExpressions.size();
    }

    public boolean matchesOperation(UMLOperation operation) {
    	return matchesOperation(operation, new HashMap<String, UMLType>(), null);
    }

    public boolean matchesOperation(UMLOperation operation, Map<String, UMLType> variableTypeMap, UMLModelDiff modelDiff) {
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
    		if(variableTypeMap.containsKey(arg)) {
    			inferredArgumentTypes.add(variableTypeMap.get(arg));
    		}
    		else if(arg.startsWith("\"") && arg.endsWith("\"")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("String"));
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
    		else if(arg.endsWith(".getClassLoader()")) {
    			inferredArgumentTypes.add(UMLType.extractTypeObject("ClassLoader"));
    		}
    		else if(arg.contains("+") && !arg.contains("++") && !UMLOperationBodyMapper.containsMethodSignatureOfAnonymousClass(arg)) {
    			String[] tokens = arg.split(UMLOperationBodyMapper.SPLIT_CONCAT_STRING_PATTERN);
    			if(tokens[0].startsWith("\"") && tokens[0].endsWith("\"")) {
    				inferredArgumentTypes.add(UMLType.extractTypeObject("String"));
    			}
    			else {
    				inferredArgumentTypes.add(null);
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
    					!compatibleTypes(parameter, inferredArgumentTypes.get(i), modelDiff)) {
    				return false;
    			}
    		}
    		i++;
    	}
    	return this.methodName.equals(operation.getName()) && (this.typeArguments == operation.getParameterTypeList().size() || varArgsMatch(operation));
    }

    private boolean compatibleTypes(UMLParameter parameter, UMLType type, UMLModelDiff modelDiff) {
    	String type1 = parameter.getType().toString();
    	String type2 = type.toString();
    	if(type1.equals("Throwable") && type2.endsWith("Exception"))
    		return true;
    	if(type1.equals("Exception") && type2.endsWith("Exception"))
    		return true;
    	if(type1.equals("int") && type2.equals("long"))
    		return true;
    	if(type1.equals("long") && type2.equals("int"))
    		return true;
    	if(!parameter.isVarargs() && type1.endsWith("Object") && !type2.endsWith("Object"))
    		return true;
    	if(!parameter.isVarargs() && type1.endsWith("Base") && type2.endsWith("Impl"))
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
    	return false;
    }

    private boolean varArgsMatch(UMLOperation operation) {
    	//0 varargs arguments passed
    	return this.typeArguments == operation.getNumberOfNonVarargsParameters() ||
    			//>=1 varargs arguments passed
    			(operation.hasVarargsParameter() && this.typeArguments > operation.getNumberOfNonVarargsParameters());
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

    public boolean containsVeryLongSubExpression() {
    	for(String expression : subExpressions) {
    		if(expression.length() > 100 && !UMLOperationBodyMapper.containsMethodSignatureOfAnonymousClass(expression)) {
    			return true;
    		}
    	}
    	return false;
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
				modified = commonPrefix + "this." + modified.substring(commonPrefix.length(), modified.length());
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
				modified = commonPrefix + "this." + modified.substring(commonPrefix.length(), modified.length());
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
				int indexOfDot = thisExpression.indexOf(".");
				String subString = thisExpression.substring(0, indexOfDot);
				if(!subExpressions.contains(subString) && !dotInsideArguments(indexOfDot, thisExpression)) {
					subExpressions.add(subString);
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
		String s2 = ((OperationInvocation)call).getMethodName().toLowerCase();
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
                typeArguments == invocation.typeArguments;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        sb.append("(");
        if(typeArguments > 0) {
            for(int i=0; i<typeArguments-1; i++)
                sb.append("arg" + i).append(", ");
            sb.append("arg" + (typeArguments-1));
        }
        sb.append(")");
        return sb.toString();
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + methodName.hashCode();
    		result = 37*result + typeArguments;
    		hashCode = result;
    	}
    	return hashCode;
    }

	public boolean identicalName(AbstractCall call) {
		return getMethodName().equals(((OperationInvocation)call).getMethodName());
	}

	public boolean typeInferenceMatch(UMLOperation operationToBeMatched, Map<String, UMLType> typeInferenceMapFromContext) {
		List<UMLParameter> parameters = operationToBeMatched.getParametersWithoutReturnType();
		if(operationToBeMatched.hasVarargsParameter()) {
			//we expect arguments to be =(parameters-1), or =parameters, or >parameters
			if(getArguments().size() < parameters.size()) {
				int i = 0;
				for(String argument : getArguments()) {
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
					String argument = getArguments().get(i);
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
			for(String argument : getArguments()) {
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
	
	public boolean differentExpressionNameAndArguments(OperationInvocation other) {
		boolean differentExpression = false;
		if(this.expression == null && other.expression != null)
			differentExpression = true;
		if(this.expression != null && other.expression == null)
			differentExpression = true;
		if(this.expression != null && other.expression != null)
			differentExpression = !this.expression.equals(other.expression) &&
			!this.expression.startsWith(other.expression) && !other.expression.startsWith(this.expression);
		boolean differentName = !this.methodName.equals(other.methodName);
		Set<String> argumentIntersection = new LinkedHashSet<String>(this.arguments);
		argumentIntersection.retainAll(other.arguments);
		boolean argumentFoundInExpression = false;
		if(this.expression != null) {
			for(String argument : other.arguments) {
				if(this.expression.contains(argument)) {
					argumentFoundInExpression = true;
				}
			}
		}
		if(other.expression != null) {
			for(String argument : this.arguments) {
				if(other.expression.contains(argument)) {
					argumentFoundInExpression = true;
				}
			}
		}
		boolean differentArguments = !this.arguments.equals(other.arguments) &&
				argumentIntersection.isEmpty() && !argumentFoundInExpression;
		return differentExpression && differentName && differentArguments;
	}

	public boolean identicalWithExpressionCallChainDifference(OperationInvocation other) {
		Set<String> subExpressionIntersection = subExpressionIntersection(other);
		return identicalName(other) &&
				equalArguments(other) &&
				subExpressionIntersection.size() > 0 &&
				(subExpressionIntersection.size() == this.subExpressions().size() ||
				subExpressionIntersection.size() == other.subExpressions().size());
	}
}
