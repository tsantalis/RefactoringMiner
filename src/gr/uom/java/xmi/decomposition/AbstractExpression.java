package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import gr.uom.java.xmi.LocationInfo;

public class AbstractExpression extends AbstractCodeFragment {
	
	private String expression;
	private LocationInfo locationInfo;
	private CompositeStatementObject owner;
	private List<String> variables;
	private List<String> types;
	private List<VariableDeclaration> variableDeclarations;
	private Map<String, OperationInvocation> methodInvocationMap;
	private List<String> anonymousClassDeclarations;
	private List<String> stringLiterals;
	private Map<String, ObjectCreation> creationMap;
	private List<String> infixOperators;
    
    public AbstractExpression(CompilationUnit cu, String filePath, Expression expression) {
    	this.locationInfo = new LocationInfo(cu, filePath, expression);
    	Visitor visitor = new Visitor(cu, filePath);
    	expression.accept(visitor);
		this.variables = visitor.getVariables();
		this.types = visitor.getTypes();
		this.variableDeclarations = visitor.getVariableDeclarations();
		this.methodInvocationMap = visitor.getMethodInvocationMap();
		this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
		this.stringLiterals = visitor.getStringLiterals();
		this.creationMap = visitor.getCreationMap();
		this.infixOperators = visitor.getInfixOperators();
    	this.expression = expression.toString();
    	this.owner = null;
    }

    public void setOwner(CompositeStatementObject owner) {
    	this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
    	return this.owner;
    }

    public String getExpression() {
    	return expression;
    }

	public String getString() {
    	return toString();
    }
  
	public String toString() {
		return getExpression().toString();
	}

	@Override
	public List<String> getVariables() {
		return variables;
	}

	@Override
	public List<String> getTypes() {
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	@Override
	public Map<String, OperationInvocation> getMethodInvocationMap() {
		return methodInvocationMap;
	}

	@Override
	public List<String> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	@Override
	public List<String> getStringLiterals() {
		return stringLiterals;
	}

	@Override
	public Map<String, ObjectCreation> getCreationMap() {
		return creationMap;
	}

	@Override
	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}
}
