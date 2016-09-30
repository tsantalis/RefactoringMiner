package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

public class SimpleNameVisitor extends ASTVisitor {

	private List<String> allIdentifiers = new ArrayList<String>();
	private List<String> invokedMethodNames = new ArrayList<String>();
	private List<String> types = new ArrayList<String>();
	private Map<String, OperationInvocation> methodInvocationMap = new LinkedHashMap<String, OperationInvocation>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	
	public boolean visit(VariableDeclarationFragment node) {
		variableDeclarations.add(new VariableDeclaration(node));
		return super.visit(node);
	}

	public boolean visit(SimpleName node) {
		allIdentifiers.add(node.getIdentifier());
		return super.visit(node);
	}
	
	public boolean visit(ParameterizedType node) {
		types.add(node.toString());
		return super.visit(node);
	}
	
	public boolean visit(WildcardType node) {
		types.add(node.toString());
		return super.visit(node);
	}
	
	public boolean visit(SimpleType node) {
		Name name = node.getName();
		types.add(name.getFullyQualifiedName());
		return super.visit(node);
	}
	
	public boolean visit(MethodInvocation node) {
		invokedMethodNames.add(node.getName().getIdentifier());
		String expression = null;
		if(node.getExpression() != null)
			expression = node.getExpression().toString();
		String methodInvocation = null;
		/*if(expression != null) {
			if(expression.matches("!(\\w|\\.)*@\\w*")) {
				methodInvocation = processMethodInvocation(node);
			}
			else {
				methodInvocation = node.toString().substring(expression.length()+1, node.toString().length());
			}
		}*/
		//else {
			if(node.toString().matches("!(\\w|\\.)*@\\w*")) {
				methodInvocation = processMethodInvocation(node);
			}
			else {
				methodInvocation = node.toString();
			}
		//}
		methodInvocationMap.put(methodInvocation, new OperationInvocation(node));
		return super.visit(node);
	}

	public static String processMethodInvocation(MethodInvocation node) {
		StringBuilder sb = new StringBuilder();
		sb.append(node.getName().getIdentifier());
		sb.append("(");
		List<Expression> arguments = node.arguments();
		if(arguments.size() > 0) {
		    for(int i=0; i<arguments.size()-1; i++)
		        sb.append(arguments.get(i).toString()).append(", ");
		    sb.append(arguments.get(arguments.size()-1).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static String processClassInstanceCreation(ClassInstanceCreation node) {
		StringBuilder sb = new StringBuilder();
		sb.append("new").append(" ");
		sb.append(node.getType().toString());
		List<Expression> arguments = node.arguments();
		if(arguments.size() > 0) {
		    for(int i=0; i<arguments.size()-1; i++)
		        sb.append(arguments.get(i).toString()).append(", ");
		    sb.append(arguments.get(arguments.size()-1).toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	public boolean visit(SuperMethodInvocation node) {
		invokedMethodNames.add(node.getName().getIdentifier());
		methodInvocationMap.put(node.toString(), new OperationInvocation(node));
		return super.visit(node);
	}

	public boolean visit(QualifiedName node) {
		Name qualifier = node.getQualifier();
		types.add(qualifier.getFullyQualifiedName());
		return super.visit(node);
	}

	public Map<String, OperationInvocation> getMethodInvocationMap() {
		return this.methodInvocationMap;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<String> getVariables() {
		List<String> variables = new ArrayList<String>(this.allIdentifiers);
		variables.removeAll(this.invokedMethodNames);
		variables.removeAll(this.types);
		return variables;
	}
}
