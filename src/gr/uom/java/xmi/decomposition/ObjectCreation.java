package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;

import com.intellij.psi.*;
import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.TypeUtils;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.StringDistance;

public class ObjectCreation extends AbstractCall {
	private UMLType type;
	private String anonymousClassDeclaration;
	private boolean isArray = false;
	private volatile int hashCode = 0;
	
	public ObjectCreation(PsiFile cu, String filePath, PsiNewExpression creation) {
		if(creation.isArrayCreation()) {
			this.locationInfo = new LocationInfo(cu, filePath, creation, CodeElementType.ARRAY_CREATION);
			this.isArray = true;
			this.type = TypeUtils.extractType(cu, filePath, creation);
			this.arguments = new ArrayList<String>();
			PsiExpression[] args = creation.getArrayDimensions();
			for(PsiExpression argument : args) {
				this.arguments.add(Formatter.format(argument));
			}
			this.typeArguments = this.arguments.size();
			PsiArrayInitializerExpression arrayInitializer = creation.getArrayInitializer();
			if(arrayInitializer != null) {
				this.anonymousClassDeclaration = Formatter.format(arrayInitializer);
			}
		}
		else {
			this.locationInfo = new LocationInfo(cu, filePath, creation, CodeElementType.CLASS_INSTANCE_CREATION);
			this.type = TypeUtils.extractType(cu, filePath, creation);
			this.arguments = new ArrayList<String>();
			PsiExpressionList argList = creation.getArgumentList();
			if (argList != null) {
				PsiExpression[] args = argList.getExpressions();
				for (PsiExpression argument : args) {
					this.arguments.add(Formatter.format(argument));
				}
			}
			this.typeArguments = this.arguments.size();
			PsiExpression qualifier = creation.getQualifier();
			if (qualifier != null) {
				this.expression = Formatter.format(qualifier);
			}
			PsiAnonymousClass anonymous = creation.getAnonymousClass();
			if (anonymous != null) {
				this.anonymousClassDeclaration = Formatter.format(anonymous);
			}
		}
	}

	public String getName() {
		return getType().toString();
	}

	public UMLType getType() {
		return type;
	}

	public boolean isArray() {
		return isArray;
	}

	public String getAnonymousClassDeclaration() {
		return anonymousClassDeclaration;
	}

	private ObjectCreation() {
		
	}

	public ObjectCreation update(String oldExpression, String newExpression) {
		ObjectCreation newObjectCreation = new ObjectCreation();
		newObjectCreation.type = this.type;
		newObjectCreation.locationInfo = this.locationInfo;
		update(newObjectCreation, oldExpression, newExpression);
		return newObjectCreation;
	}

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof ObjectCreation) {
        	ObjectCreation creation = (ObjectCreation)o;
            return type.equals(creation.type) && isArray == creation.isArray &&
                typeArguments == creation.typeArguments;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(type);
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
    		result = 37*result + type.hashCode();
    		result = 37*result + (isArray ? 1 : 0);
    		result = 37*result + typeArguments;
    		hashCode = result;
    	}
    	return hashCode;
    }

    public boolean identicalArrayInitializer(ObjectCreation other) {
    	if(this.isArray && other.isArray) {
    		if(this.anonymousClassDeclaration != null && other.anonymousClassDeclaration != null) {
    			return this.anonymousClassDeclaration.equals(other.anonymousClassDeclaration);
    		}
    		else if(this.anonymousClassDeclaration == null && other.anonymousClassDeclaration == null) {
    			return true;
    		}
    	}
    	return false;
    }

	public double normalizedNameDistance(AbstractCall call) {
		String s1 = getType().toString().toLowerCase();
		String s2 = ((ObjectCreation)call).getType().toString().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean identicalName(AbstractCall call) {
		return getType().equals(((ObjectCreation)call).getType());
	}

	public String actualString() {
		StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(super.actualString());
        return sb.toString();
	}
}
