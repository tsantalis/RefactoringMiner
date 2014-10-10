package gr.uom.java.xmi.decomposition;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.Expression;

public class AbstractExpression extends AbstractCodeFragment {
	
	private String expression;
	private CompositeStatementObject owner;
	private List<String> variables;
	private Map<String, OperationInvocation> methodInvocationMap;
    
    public AbstractExpression(Expression expression) {
    	SimpleNameVisitor visitor = new SimpleNameVisitor();
    	expression.accept(visitor);
		this.variables = visitor.getVariables();
		this.methodInvocationMap = visitor.getMethodInvocationMap();
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
	public Map<String, OperationInvocation> getMethodInvocationMap() {
		return methodInvocationMap;
	}
}
