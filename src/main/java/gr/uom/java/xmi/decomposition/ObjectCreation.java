package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.SPLIT_COMMA_PATTERN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Type;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry;
import org.jetbrains.kotlin.psi.KtTypeProjection;
import org.jetbrains.kotlin.psi.ValueArgument;

import extension.ast.node.LangASTNode;
import extension.ast.node.expression.LangFieldAccess;
import extension.ast.node.expression.LangMethodInvocation;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.unit.LangCompilationUnit;
import extension.ast.visitor.LangVisitor;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.StringDistance;

public class ObjectCreation extends AbstractCall {
	private UMLType type;
	private String anonymousClassDeclaration;
	private boolean isArray = false;
	private volatile int hashCode = 0;

	public ObjectCreation(LangCompilationUnit cu, String sourceFolder, String filePath, LangMethodInvocation creation, VariableDeclarationContainer container, String fileContent) {
		super(cu, sourceFolder, filePath, creation, CodeElementType.CLASS_INSTANCE_CREATION, container);
		if(creation.getExpression() instanceof LangFieldAccess fieldAccess) {
			this.type = UMLType.extractTypeObject(LangVisitor.stringify(fieldAccess), "[", "]", this.locationInfo);
		}
		else if(creation.getExpression() instanceof LangSimpleName simpleName) {
			this.type = UMLType.extractTypeObject(simpleName.getIdentifier(), "[", "]", this.locationInfo);
		}
		this.numberOfArguments = creation.getArguments().size();
		this.arguments = new ArrayList<String>();
		List<LangASTNode> args = creation.getArguments();
		for(LangASTNode argument : args) {
			this.arguments.add(LangVisitor.stringify(argument));
		}
	}

	public ObjectCreation(CompilationUnit cu, String sourceFolder, String filePath, ClassInstanceCreation creation, VariableDeclarationContainer container, String javaFileContent) {
		super(cu, sourceFolder, filePath, creation, CodeElementType.CLASS_INSTANCE_CREATION, container);
		this.type = UMLType.extractTypeObject(cu, sourceFolder, filePath, creation.getType(), 0, javaFileContent);
		this.numberOfArguments = creation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Type> typeArgs = creation.typeArguments();
		for(Type typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, typeArg, 0, javaFileContent));
		}
		List<Expression> args = creation.arguments();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
		if(creation.getExpression() != null) {
			this.expression = stringify(creation.getExpression());
		}
		if(creation.getAnonymousClassDeclaration() != null) {
			this.anonymousClassDeclaration = stringify(creation.getAnonymousClassDeclaration());
		}
	}

	public ObjectCreation(CompilationUnit cu, String sourceFolder, String filePath, ArrayCreation creation, VariableDeclarationContainer container, String javaFileContent) {
		super(cu, sourceFolder, filePath, creation, CodeElementType.ARRAY_CREATION, container);
		this.isArray = true;
		this.type = UMLType.extractTypeObject(cu, sourceFolder, filePath, creation.getType(), 0, javaFileContent);
		this.numberOfArguments = creation.dimensions().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = creation.dimensions();
		for(Expression argument : args) {
			this.arguments.add(stringify(argument));
		}
		if(creation.getInitializer() != null) {
			this.anonymousClassDeclaration = stringify(creation.getInitializer());
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

	private ObjectCreation(LocationInfo locationInfo) {
		super(locationInfo);
	}

	public ObjectCreation update(String oldExpression, String newExpression) {
		ObjectCreation newObjectCreation = new ObjectCreation(this.locationInfo);
		newObjectCreation.type = this.type;
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
                numberOfArguments == creation.numberOfArguments;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(type);
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
    		result = 37*result + type.hashCode();
    		result = 37*result + (isArray ? 1 : 0);
    		result = 37*result + numberOfArguments;
    		hashCode = result;
    	}
    	return hashCode;
    }

    public boolean identicalArrayInitializer(ObjectCreation other) {
    	if(this.isArray && other.isArray) {
    		if(this.anonymousClassDeclaration != null && other.anonymousClassDeclaration != null) {
    			if(this.anonymousClassDeclaration.equals(other.anonymousClassDeclaration)) {
    				return true;
    			}
    			if(this.anonymousClassDeclaration.startsWith(LANG.OPEN_ARRAY_INITIALIZER) && this.anonymousClassDeclaration.endsWith(LANG.CLOSE_ARRAY_INITIALIZER) &&
    					other.anonymousClassDeclaration.startsWith(LANG.OPEN_ARRAY_INITIALIZER) && other.anonymousClassDeclaration.endsWith(LANG.CLOSE_ARRAY_INITIALIZER)) {
    				String s1 = this.anonymousClassDeclaration.substring(1, this.anonymousClassDeclaration.length()-1);
    				String s2 = other.anonymousClassDeclaration.substring(1, other.anonymousClassDeclaration.length()-1);
    				List<String> tokens1 = Arrays.asList(SPLIT_COMMA_PATTERN.split(s1));
    				List<String> tokens2 = Arrays.asList(SPLIT_COMMA_PATTERN.split(s2));
    				if(tokens1.size() > 0 && tokens2.size() > 0 && (tokens1.containsAll(tokens2) || tokens2.containsAll(tokens1))) {
    					return true;
    				}
    			}
    		}
    		else if(this.anonymousClassDeclaration == null && other.anonymousClassDeclaration == null) {
    			return true;
    		}
    	}
    	return false;
    }

	public double normalizedNameDistance(AbstractCall call) {
		String s1 = getType().toString().toLowerCase();
		if(call instanceof ObjectCreation) {
			String s2 = ((ObjectCreation)call).getType().toString().toLowerCase();
			int distance = StringDistance.editDistance(s1, s2);
			double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
			return normalized;
		}
		String s2 = call.getName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean identicalName(AbstractCall call) {
		if(call instanceof ObjectCreation)
			return getType().equals(((ObjectCreation)call).getType());
		return false;
	}

	public String actualString() {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(super.actualString());
		return sb.toString();
	}

	public ObjectCreation(KtFile cu, String sourceFolder, String filePath, KtSuperTypeCallEntry invocation, VariableDeclarationContainer container, String fileContent) {
		super(cu, sourceFolder, filePath, invocation, CodeElementType.CLASS_INSTANCE_CREATION, container);
		this.type = UMLType.extractTypeObject(cu, sourceFolder, filePath, fileContent, invocation.getTypeReference(), 0);
		this.numberOfArguments = invocation.getValueArguments().size();
		this.arguments = new ArrayList<String>();
		List<KtTypeProjection> typeArgs = invocation.getTypeArguments();
		for(KtTypeProjection typeArg : typeArgs) {
			this.typeArguments.add(UMLType.extractTypeObject(cu, sourceFolder, filePath, fileContent, typeArg, 0));
		}
		List<? extends ValueArgument> args = invocation.getValueArguments();
		for(ValueArgument argument : args) {
			// TODO replace with stringify
			this.arguments.add(argument.getArgumentExpression().getText());
		}
	}
}
