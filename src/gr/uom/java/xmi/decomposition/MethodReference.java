package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodReferenceExpression;
import gr.uom.java.xmi.Formatter;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.StringDistance;

public class MethodReference extends AbstractCall {
	private String methodName;
	private volatile int hashCode = 0;
	
	public MethodReference(PsiFile cu, String filePath, PsiMethodReferenceExpression reference) {
		this.locationInfo = new LocationInfo(cu, filePath, reference, CodeElementType.METHOD_REFERENCE);
		if(reference.getQualifierType() != null) {
			this.expression = Formatter.format(reference.getQualifierType());
		}
		else if(reference.getQualifier() != null) {
			this.expression = Formatter.format(reference.getQualifier());
		}
		this.methodName = reference.getReferenceName();
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
                typeArguments == invocation.typeArguments &&
                (this.expression != null) == (invocation.expression != null);
        }
        else if (o instanceof OperationInvocation) {
        	OperationInvocation invocation = (OperationInvocation)o;
            return methodName.equals(invocation.getMethodName()) &&
                typeArguments == invocation.typeArguments &&
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
    		result = 37*result + typeArguments;
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
