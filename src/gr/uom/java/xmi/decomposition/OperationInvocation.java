package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLOperation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class OperationInvocation {
	private String methodName;
	private int typeArguments;
	private String expression;
	private List<String> arguments;
	private List<String> subExpressions = new ArrayList<String>();
	private volatile int hashCode = 0;
	
	public OperationInvocation(MethodInvocation invocation) {
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

	public OperationInvocation(SuperMethodInvocation invocation) {
		this.methodName = invocation.getName().getIdentifier();
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
		newOperationInvocation.typeArguments = this.typeArguments;
		if(this.expression != null && this.expression.equals(oldExpression)) {
			newOperationInvocation.expression = newExpression;
		}
		else {
			newOperationInvocation.expression = this.expression;
		}
		newOperationInvocation.arguments = new ArrayList<String>();
		for(String argument : this.arguments) {
			if(argument.equals(oldExpression)) {
				newOperationInvocation.arguments.add(newExpression);
			}
			else {
				newOperationInvocation.arguments.add(argument);
			}
		}
		newOperationInvocation.subExpressions = new ArrayList<String>();
		for(String argument : this.subExpressions) {
			if(argument.equals(oldExpression)) {
				newOperationInvocation.subExpressions.add(newExpression);
			}
			else {
				newOperationInvocation.subExpressions.add(argument);
			}
		}
		return newOperationInvocation;
	}

    public String getMethodName() {
		return methodName;
	}

	public String getExpression() {
		return expression;
	}

    public List<String> getArguments() {
		return arguments;
	}

    public boolean matchesOperation(UMLOperation operation) {
    	return this.methodName.equals(operation.getName()) && this.typeArguments == operation.getParameterTypeList().size();
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
    		Set<String> intersection = new LinkedHashSet<String>(this.subExpressions);
    		intersection.retainAll(other.subExpressions);
    		int thisUnmatchedSubExpressions = this.subExpressions.size() - intersection.size();
    		int otherUnmatchedSubExpressions = other.subExpressions.size() - intersection.size();
    		if(thisUnmatchedSubExpressions > intersection.size() || otherUnmatchedSubExpressions > intersection.size())
    			return false;
    	}
    	return true;
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
}
