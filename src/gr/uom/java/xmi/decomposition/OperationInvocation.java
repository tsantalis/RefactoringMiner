package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLOperation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class OperationInvocation {
	private String methodName;
	private int typeArguments;
	private List<String> arguments;
	private volatile int hashCode = 0;
	
	public OperationInvocation(MethodInvocation invocation) {
		this.methodName = invocation.getName().getIdentifier();
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
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

    public List<String> getArguments() {
		return arguments;
	}

    public boolean matchesOperation(UMLOperation operation) {
    	return this.methodName.equals(operation.getName()) && this.typeArguments == operation.getParameterTypeList().size();
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
