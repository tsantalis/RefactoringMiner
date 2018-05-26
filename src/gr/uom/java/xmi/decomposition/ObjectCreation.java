package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLType;

public class ObjectCreation implements LocationInfoProvider {
	private UMLType type;
	private int typeArguments;
	private String expression;
	private String anonymousClassDeclaration;
	private List<String> arguments;
	private boolean isArray = false;
	private volatile int hashCode = 0;
	private LocationInfo locationInfo;
	
	public ObjectCreation(CompilationUnit cu, String filePath, ClassInstanceCreation creation) {
		this.locationInfo = new LocationInfo(cu, filePath, creation);
		this.type = UMLType.extractTypeObject(creation.getType().toString());
		this.typeArguments = creation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = creation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
		if(creation.getExpression() != null) {
			this.expression = creation.getExpression().toString();
		}
		if(creation.getAnonymousClassDeclaration() != null) {
			this.anonymousClassDeclaration = creation.getAnonymousClassDeclaration().toString();
		}
	}

	public ObjectCreation(CompilationUnit cu, String filePath, ArrayCreation creation) {
		this.locationInfo = new LocationInfo(cu, filePath, creation);
		this.isArray = true;
		this.type = UMLType.extractTypeObject(creation.getType().toString());
		this.typeArguments = creation.dimensions().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = creation.dimensions();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
		if(creation.getInitializer() != null) {
			this.anonymousClassDeclaration = creation.getInitializer().toString();
		}
	}

	public UMLType getType() {
		return type;
	}

	public String getExpression() {
		return expression;
	}

    public List<String> getArguments() {
		return arguments;
	}

	public boolean isArray() {
		return isArray;
	}

	public String getAnonymousClassDeclaration() {
		return anonymousClassDeclaration;
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

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}
}
