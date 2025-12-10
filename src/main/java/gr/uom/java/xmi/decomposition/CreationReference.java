package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Type;

import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.StringDistance;

public class CreationReference extends AbstractCall {
	private UMLType type;
	private volatile int hashCode = 0;
	
	public CreationReference(CompilationUnit cu, String sourceFolder, String filePath, org.eclipse.jdt.core.dom.CreationReference reference, VariableDeclarationContainer container, String javaFileContent) {
		super(cu, sourceFolder, filePath, reference, CodeElementType.CREATION_REFERENCE, container);
		this.type = UMLType.extractTypeObject(cu, sourceFolder, filePath, reference.getType(), 0, javaFileContent);
		this.arguments = new ArrayList<String>();
		List<Type> typeArgs = reference.typeArguments();
		for(Type typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, typeArg, 0, javaFileContent));
		}
	}

	private CreationReference(LocationInfo locationInfo) {
		super(locationInfo);
	}

	public String getName() {
		return getType().toString();
	}

	public UMLType getType() {
		return type;
	}

	public CreationReference update(String oldExpression, String newExpression) {
		CreationReference newObjectCreation = new CreationReference(this.locationInfo);
		newObjectCreation.type = this.type;
		update(newObjectCreation, oldExpression, newExpression);
		return newObjectCreation;
	}

	public double normalizedNameDistance(AbstractCall call) {
		String s1 = getType().toString().toLowerCase();
		String s2 = ((CreationReference)call).getType().toString().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean identicalName(AbstractCall call) {
		if(call instanceof CreationReference)
			return getType().equals(((CreationReference)call).getType());
		return false;
	}

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof CreationReference) {
        	CreationReference invocation = (CreationReference)o;
            return type.equals(invocation.type);
        }
        else if (o instanceof ObjectCreation) {
        	ObjectCreation invocation = (ObjectCreation)o;
            return type.equals(invocation.getType());
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        return sb.toString();
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + type.hashCode();
    		hashCode = result;
    	}
    	return hashCode;
    }

	public String actualString() {
		StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(LANG.METHOD_REFERENCE).append("new");
        return sb.toString();
	}
}
