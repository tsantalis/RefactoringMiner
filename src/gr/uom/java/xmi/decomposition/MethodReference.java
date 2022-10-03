package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TypeMethodReference;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.UMLType;

public class MethodReference extends AbstractCall {
	private String methodName;
	private volatile int hashCode = 0;
	
	public MethodReference(CompilationUnit cu, String filePath, ExpressionMethodReference reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.METHOD_REFERENCE);
		this.methodName = reference.getName().getIdentifier();
		this.expression = reference.getExpression().toString();
		this.arguments = new ArrayList<String>();
	}
	
	public MethodReference(CompilationUnit cu, String filePath, SuperMethodReference reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.METHOD_REFERENCE);
		this.methodName = reference.getName().getIdentifier();
		this.arguments = new ArrayList<String>();
		if(reference.getQualifier() != null) {
			this.expression = reference.getQualifier().getFullyQualifiedName() + ".super";
		}
		else {
			this.expression = "super";
		}
	}
	
	public MethodReference(CompilationUnit cu, String filePath, TypeMethodReference reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.METHOD_REFERENCE);
		this.methodName = reference.getName().getIdentifier();
		this.expression = UMLType.extractTypeObject(cu, filePath, reference.getType(), 0).toQualifiedString();
		this.arguments = new ArrayList<String>();
	}

	public String getMethodName() {
		return methodName;
	}

	@Override
	public boolean identicalName(AbstractCall call) {
		return getMethodName().equals(call.getName());
	}

	@Override
	public String getName() {
		return getMethodName();
	}

	@Override
	public double normalizedNameDistance(AbstractCall call) {
		String s1 = getMethodName().toLowerCase();
		String s2 = call.getName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	private MethodReference() {
		
	}

	@Override
	public AbstractCall update(String oldExpression, String newExpression) {
		MethodReference newReference = new MethodReference();
		newReference.methodName = this.methodName;
		newReference.locationInfo = this.locationInfo;
		update(newReference, oldExpression, newExpression);
		return newReference;
	}

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof MethodReference) {
        	MethodReference invocation = (MethodReference)o;
            return methodName.equals(invocation.methodName) &&
                numberOfArguments == invocation.numberOfArguments &&
                (this.expression != null) == (invocation.expression != null);
        }
        else if (o instanceof OperationInvocation) {
        	OperationInvocation invocation = (OperationInvocation)o;
            return methodName.equals(invocation.getMethodName()) &&
                numberOfArguments == invocation.numberOfArguments &&
                (this.expression != null) == (invocation.expression != null);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
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
   
    public String actualString() {
		StringBuilder sb = new StringBuilder();
		if(expression != null) {
			sb.append(expression).append("::");
		}
		sb.append(getName());
		return sb.toString();
    }
}
