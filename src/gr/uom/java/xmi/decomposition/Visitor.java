package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

public class Visitor extends ASTVisitor {
	private CompilationUnit cu;
	private String filePath;
	private List<String> allIdentifiers = new ArrayList<String>();
	private List<String> invokedMethodNames = new ArrayList<String>();
	private List<String> types = new ArrayList<String>();
	private Map<String, OperationInvocation> methodInvocationMap = new LinkedHashMap<String, OperationInvocation>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<String> anonymousClassDeclarations = new ArrayList<String>();
	private List<String> stringLiterals = new ArrayList<String>();
	private Map<String, ObjectCreation> creationMap = new LinkedHashMap<String, ObjectCreation>();
	private List<String> infixOperators = new ArrayList<String>();

	public Visitor(CompilationUnit cu, String filePath) {
		this.cu = cu;
		this.filePath = filePath;
	}

	public boolean visit(InfixExpression node) {
		infixOperators.add(node.getOperator().toString());
		return super.visit(node);
	}

	public boolean visit(ClassInstanceCreation node) {
		creationMap.put(node.toString(), new ObjectCreation(cu, filePath, node));
		return super.visit(node);
	}

	public boolean visit(ArrayCreation node) {
		creationMap.put(node.toString(), new ObjectCreation(cu, filePath, node));
		return super.visit(node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		variableDeclarations.add(new VariableDeclaration(cu, filePath, node));
		return super.visit(node);
	}

	public boolean visit(AnonymousClassDeclaration node) {
		anonymousClassDeclarations.add(node.toString());
		return super.visit(node);
	}

	public boolean visit(StringLiteral node) {
		stringLiterals.add(node.toString());
		return super.visit(node);
	}

	public boolean visit(ThisExpression node) {
		if(!(node.getParent() instanceof FieldAccess)) {
			allIdentifiers.add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(SimpleName node) {
		if(node.getParent() instanceof FieldAccess && ((FieldAccess)node.getParent()).getExpression() instanceof ThisExpression) {
			FieldAccess fieldAccess = (FieldAccess)node.getParent();
			allIdentifiers.add(fieldAccess.toString());
		}
		else {
			allIdentifiers.add(node.getIdentifier());
		}
		return super.visit(node);
	}
	
	public boolean visit(ArrayType node) {
		types.add(node.toString());
		return false;
	}
	
	public boolean visit(ParameterizedType node) {
		types.add(node.toString());
		return false;
	}
	
	public boolean visit(WildcardType node) {
		types.add(node.toString());
		return false;
	}
	
	public boolean visit(QualifiedType node) {
		types.add(node.toString());
		return false;
	}
	
	public boolean visit(SimpleType node) {
		Name name = node.getName();
		types.add(name.getFullyQualifiedName());
		return false;
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
		boolean builderPatternChain = false;
		for(String key : methodInvocationMap.keySet()) {
			OperationInvocation invocation = methodInvocationMap.get(key);
			if(key.startsWith(methodInvocation) && invocation.numberOfSubExpressions() > 15) {
				builderPatternChain = true;
				break;
			}
		}
		if(!builderPatternChain) {
			methodInvocationMap.put(methodInvocation, new OperationInvocation(cu, filePath, node));
		}
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
		methodInvocationMap.put(node.toString(), new OperationInvocation(cu, filePath, node));
		return super.visit(node);
	}

	public boolean visit(QualifiedName node) {
		Name qualifier = node.getQualifier();
		if(Character.isUpperCase(qualifier.getFullyQualifiedName().charAt(0))) {
			types.add(qualifier.getFullyQualifiedName());
		}
		return super.visit(node);
	}

	public Map<String, OperationInvocation> getMethodInvocationMap() {
		return this.methodInvocationMap;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<String> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public List<String> getStringLiterals() {
		return stringLiterals;
	}

	public Map<String, ObjectCreation> getCreationMap() {
		return creationMap;
	}

	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public List<String> getVariables() {
		List<String> variables = new ArrayList<String>(this.allIdentifiers);
		variables.removeAll(this.invokedMethodNames);
		variables.removeAll(this.types);
		return variables;
	}
}
