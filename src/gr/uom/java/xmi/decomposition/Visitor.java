package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

public class Visitor extends ASTVisitor {
	public static final Pattern METHOD_INVOCATION_PATTERN = Pattern.compile("!(\\w|\\.)*@\\w*");
	public static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
	private CompilationUnit cu;
	private String filePath;
	private List<String> variables = new ArrayList<String>();
	private List<String> types = new ArrayList<String>();
	private Map<String, List<OperationInvocation>> methodInvocationMap = new LinkedHashMap<String, List<OperationInvocation>>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<String> stringLiterals = new ArrayList<String>();
	private List<String> numberLiterals = new ArrayList<String>();
	private List<String> nullLiterals = new ArrayList<String>();
	private List<String> booleanLiterals = new ArrayList<String>();
	private List<String> typeLiterals = new ArrayList<String>();
	private Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<String, List<ObjectCreation>>();
	private List<String> infixOperators = new ArrayList<String>();
	private List<String> arrayAccesses = new ArrayList<String>();
	private List<String> prefixExpressions = new ArrayList<String>();
	private List<String> postfixExpressions = new ArrayList<String>();
	private List<String> arguments = new ArrayList<String>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	private Set<ASTNode> builderPatternChains = new LinkedHashSet<ASTNode>();
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private DefaultMutableTreeNode current = root;

	public Visitor(CompilationUnit cu, String filePath) {
		this.cu = cu;
		this.filePath = filePath;
	}

	public boolean visit(ArrayAccess node) {
		arrayAccesses.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArrayAccesses().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(PrefixExpression node) {
		prefixExpressions.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPrefixExpressions().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(PostfixExpression node) {
		postfixExpressions.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPostfixExpressions().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(ConditionalExpression node) {
		TernaryOperatorExpression ternary = new TernaryOperatorExpression(cu, filePath, node);
		ternaryOperatorExpressions.add(ternary);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTernaryOperatorExpressions().add(ternary);
		}
		return super.visit(node);
	}

	public boolean visit(InfixExpression node) {
		infixOperators.add(node.getOperator().toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getInfixOperators().add(node.getOperator().toString());
		}
		return super.visit(node);
	}

	public boolean visit(ClassInstanceCreation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		ObjectCreation creation = new ObjectCreation(cu, filePath, node);
		String nodeAsString = node.toString();
		if(creationMap.containsKey(nodeAsString)) {
			creationMap.get(nodeAsString).add(creation);
		}
		else {
			List<ObjectCreation> list = new ArrayList<ObjectCreation>();
			list.add(creation);
			creationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<ObjectCreation>> anonymousCreationMap = anonymous.getCreationMap();
			if(anonymousCreationMap.containsKey(nodeAsString)) {
				anonymousCreationMap.get(nodeAsString).add(creation);
			}
			else {
				List<ObjectCreation> list = new ArrayList<ObjectCreation>();
				list.add(creation);
				anonymousCreationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(ArrayCreation node) {
		ObjectCreation creation = new ObjectCreation(cu, filePath, node);
		String nodeAsString = node.toString();
		if(creationMap.containsKey(nodeAsString)) {
			creationMap.get(nodeAsString).add(creation);
		}
		else {
			List<ObjectCreation> list = new ArrayList<ObjectCreation>();
			list.add(creation);
			creationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<ObjectCreation>> anonymousCreationMap = anonymous.getCreationMap();
			if(anonymousCreationMap.containsKey(nodeAsString)) {
				anonymousCreationMap.get(nodeAsString).add(creation);
			}
			else {
				List<ObjectCreation> list = new ArrayList<ObjectCreation>();
				list.add(creation);
				anonymousCreationMap.put(nodeAsString, list);
			}
		}
		ArrayInitializer initializer = node.getInitializer();
		if(initializer != null) {
			List<Expression> expressions = initializer.expressions();
			if(expressions.size() > 10) {
				return false;
			}
		}
		return super.visit(node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		if(!(node.getParent() instanceof LambdaExpression)) {
			VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node);
			variableDeclarations.add(variableDeclaration);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariableDeclarations().add(variableDeclaration);
			}
		}
		return super.visit(node);
	}

	public boolean visit(SingleVariableDeclaration node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node);
		variableDeclarations.add(variableDeclaration);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getVariableDeclarations().add(variableDeclaration);
		}
		return super.visit(node);
	}

	public boolean visit(AnonymousClassDeclaration node) {
		DefaultMutableTreeNode childNode = insertNode(node);
		AnonymousClassDeclarationObject childAnonymous = (AnonymousClassDeclarationObject)childNode.getUserObject();
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			currentAnonymous.getAnonymousClassDeclarations().add(childAnonymous);
		}
		anonymousClassDeclarations.add(childAnonymous);
		this.current = childNode;
		for(ASTNode parent : builderPatternChains) {
			if(isParent(node, parent)) {
				return false;
			}
		}
		return super.visit(node);
	}

	public void endVisit(AnonymousClassDeclaration node) {
		DefaultMutableTreeNode parentNode = deleteNode(node);
		for(ASTNode parent : builderPatternChains) {
			if(isParent(node, parent) || isParent(parent, node)) {
				removeAnonymousData();
				break;
			}
		}
		this.current = parentNode;
	}

	private void removeAnonymousData() {
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			this.variables.removeAll(anonymous.getVariables());
			this.types.removeAll(anonymous.getTypes());
			for(String key : anonymous.getMethodInvocationMap().keySet()) {
				this.methodInvocationMap.remove(key, anonymous.getMethodInvocationMap().get(key));
			}
			for(String key : anonymous.getCreationMap().keySet()) {
				this.creationMap.remove(key, anonymous.getCreationMap().get(key));
			}
			this.variableDeclarations.removeAll(anonymous.getVariableDeclarations());
			this.stringLiterals.removeAll(anonymous.getStringLiterals());
			this.booleanLiterals.removeAll(anonymous.getBooleanLiterals());
			this.typeLiterals.removeAll(anonymous.getTypeLiterals());
			this.numberLiterals.removeAll(anonymous.getNumberLiterals());
			this.infixOperators.removeAll(anonymous.getInfixOperators());
			this.arguments.removeAll(anonymous.getArguments());
			this.ternaryOperatorExpressions.removeAll(anonymous.getTernaryOperatorExpressions());
			this.anonymousClassDeclarations.removeAll(anonymous.getAnonymousClassDeclarations());
			this.lambdas.removeAll(anonymous.getLambdas());
		}
	}

	private DefaultMutableTreeNode deleteNode(AnonymousClassDeclaration childAnonymous) {
		Enumeration enumeration = root.postorderEnumeration();
		DefaultMutableTreeNode childNode = findNode(childAnonymous);
		
		DefaultMutableTreeNode parentNode = root;
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)currentNode.getUserObject();
			if(currentAnonymous != null && isParent(childAnonymous, currentAnonymous.getAstNode())) {
				parentNode = currentNode;
				break;
			}
		}
		parentNode.remove(childNode);
		AnonymousClassDeclarationObject childAnonymousObject = (AnonymousClassDeclarationObject)childNode.getUserObject();
		childAnonymousObject.setAstNode(null);
		return parentNode;
	}

	private DefaultMutableTreeNode insertNode(AnonymousClassDeclaration childAnonymous) {
		Enumeration enumeration = root.postorderEnumeration();
		AnonymousClassDeclarationObject anonymousObject = new AnonymousClassDeclarationObject(cu, filePath, childAnonymous);
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(anonymousObject);
		
		DefaultMutableTreeNode parentNode = root;
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)currentNode.getUserObject();
			if(currentAnonymous != null && isParent(childAnonymous, currentAnonymous.getAstNode())) {
				parentNode = currentNode;
				break;
			}
		}
		parentNode.add(childNode);
		return childNode;
	}

	private DefaultMutableTreeNode findNode(AnonymousClassDeclaration anonymous) {
		Enumeration enumeration = root.postorderEnumeration();
		
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)currentNode.getUserObject();
			if(currentAnonymous != null && currentAnonymous.getAstNode().equals(anonymous)) {
				return currentNode;
			}
		}
		return null;
	}

	private boolean isParent(ASTNode child, ASTNode parent) {
		ASTNode current = child;
		while(current.getParent() != null) {
			if(current.getParent().equals(parent))
				return true;
			current = current.getParent();
		}
		return false;
	}

	public boolean visit(StringLiteral node) {
		stringLiterals.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getStringLiterals().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(NumberLiteral node) {
		numberLiterals.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getNumberLiterals().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(NullLiteral node) {
		nullLiterals.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getNullLiterals().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(BooleanLiteral node) {
		booleanLiterals.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getBooleanLiterals().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(TypeLiteral node) {
		typeLiterals.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypeLiterals().add(node.toString());
		}
		return super.visit(node);
	}

	public boolean visit(ThisExpression node) {
		if(!(node.getParent() instanceof FieldAccess)) {
			variables.add(node.toString());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(node.toString());
			}
		}
		return super.visit(node);
	}

	public boolean visit(SimpleName node) {
		if(node.getParent() instanceof FieldAccess && ((FieldAccess)node.getParent()).getExpression() instanceof ThisExpression) {
			FieldAccess fieldAccess = (FieldAccess)node.getParent();
			variables.add(fieldAccess.toString());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(fieldAccess.toString());
			}
		}
		else if(node.getParent() instanceof MethodInvocation &&
				((MethodInvocation)node.getParent()).getName().equals(node)) {
			// skip method invocation names
		}
		else if(node.getParent() instanceof SuperMethodInvocation &&
				((SuperMethodInvocation)node.getParent()).getName().equals(node)) {
			// skip super method invocation names
		}
		else if(node.getParent() instanceof Type) {
			// skip type names
		}
		else if(node.getParent() instanceof MarkerAnnotation &&
				((MarkerAnnotation)node.getParent()).getTypeName().equals(node)) {
			// skip marker annotation names
		}
		else if(node.getParent() instanceof MethodDeclaration &&
				((MethodDeclaration)node.getParent()).getName().equals(node)) {
			// skip method declaration names
		}
		else if(node.getParent() instanceof SingleVariableDeclaration &&
				node.getParent().getParent() instanceof MethodDeclaration) {
			// skip method parameter names
		}
		else if(node.getParent() instanceof SingleVariableDeclaration &&
				node.getParent().getParent() instanceof CatchClause) {
			// skip catch clause formal parameter names
		}
		else if(node.getParent() instanceof QualifiedName &&
				(node.getParent().getParent() instanceof QualifiedName ||
				node.getParent().getParent() instanceof MethodInvocation ||
				node.getParent().getParent() instanceof SuperMethodInvocation ||
				node.getParent().getParent() instanceof ClassInstanceCreation)) {
			// skip names being part of qualified names
		}
		else {
			variables.add(node.getIdentifier());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(node.getIdentifier());
			}
		}
		return super.visit(node);
	}
	
	public boolean visit(ArrayType node) {
		types.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(node.toString());
		}
		return false;
	}
	
	public boolean visit(ParameterizedType node) {
		types.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(node.toString());
		}
		return false;
	}
	
	public boolean visit(WildcardType node) {
		types.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(node.toString());
		}
		return false;
	}
	
	public boolean visit(QualifiedType node) {
		types.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(node.toString());
		}
		return false;
	}
	
	public boolean visit(PrimitiveType node) {
		types.add(node.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(node.toString());
		}
		return false;
	}
	
	public boolean visit(SimpleType node) {
		Name name = node.getName();
		types.add(name.getFullyQualifiedName());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(name.getFullyQualifiedName());
		}
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		String methodInvocation = null;
		if(METHOD_INVOCATION_PATTERN.matcher(node.toString()).matches()) {
			methodInvocation = processMethodInvocation(node);
		}
		else {
			methodInvocation = node.toString();
		}
		if(methodInvocationMap.isEmpty() && node.getExpression() instanceof MethodInvocation &&
				!(node.getName().getIdentifier().equals("length") && node.arguments().size() == 0)) {
			builderPatternChains.add(node);
		}
		boolean builderPatternChain = false;
		for(String key : methodInvocationMap.keySet()) {
			List<OperationInvocation> invocations = methodInvocationMap.get(key);
			OperationInvocation invocation = invocations.get(0);
			if(key.startsWith(methodInvocation) && invocation.numberOfSubExpressions() > 0 &&
					!(invocation.getName().equals("length") && invocation.getArguments().size() == 0)) {
				builderPatternChains.add(node);
			}
			if(key.startsWith(methodInvocation) && complexInvocation(invocation)) {
				builderPatternChain = true;
			}
		}
		if(builderPatternChain) {
			return false;
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		if(methodInvocationMap.containsKey(methodInvocation)) {
			methodInvocationMap.get(methodInvocation).add(invocation);
		}
		else {
			List<OperationInvocation> list = new ArrayList<OperationInvocation>();
			list.add(invocation);
			methodInvocationMap.put(methodInvocation, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<OperationInvocation>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(methodInvocation)) {
				anonymousMethodInvocationMap.get(methodInvocation).add(invocation);
			}
			else {
				List<OperationInvocation> list = new ArrayList<OperationInvocation>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(methodInvocation, list);
			}
		}
		return super.visit(node);
	}

	private boolean complexInvocation(OperationInvocation invocation) {
		return (invocation.numberOfSubExpressions() > 3 && invocation.containsVeryLongSubExpression()) ||
				invocation.numberOfSubExpressions() > 15;
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
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		String nodeAsString = node.toString();
		if(methodInvocationMap.containsKey(nodeAsString)) {
			methodInvocationMap.get(nodeAsString).add(invocation);
		}
		else {
			List<OperationInvocation> list = new ArrayList<OperationInvocation>();
			list.add(invocation);
			methodInvocationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<OperationInvocation>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(nodeAsString)) {
				anonymousMethodInvocationMap.get(nodeAsString).add(invocation);
			}
			else {
				List<OperationInvocation> list = new ArrayList<OperationInvocation>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(SuperConstructorInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		String nodeAsString = node.toString();
		if(methodInvocationMap.containsKey(nodeAsString)) {
			methodInvocationMap.get(nodeAsString).add(invocation);
		}
		else {
			List<OperationInvocation> list = new ArrayList<OperationInvocation>();
			list.add(invocation);
			methodInvocationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<OperationInvocation>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(nodeAsString)) {
				anonymousMethodInvocationMap.get(nodeAsString).add(invocation);
			}
			else {
				List<OperationInvocation> list = new ArrayList<OperationInvocation>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(ConstructorInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		String nodeAsString = node.toString();
		if(methodInvocationMap.containsKey(nodeAsString)) {
			methodInvocationMap.get(nodeAsString).add(invocation);
		}
		else {
			List<OperationInvocation> list = new ArrayList<OperationInvocation>();
			list.add(invocation);
			methodInvocationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<OperationInvocation>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(nodeAsString)) {
				anonymousMethodInvocationMap.get(nodeAsString).add(invocation);
			}
			else {
				List<OperationInvocation> list = new ArrayList<OperationInvocation>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	private void processArgument(Expression argument) {
		if(argument instanceof SuperMethodInvocation ||
				argument instanceof Name ||
				argument instanceof StringLiteral ||
				argument instanceof BooleanLiteral ||
				(argument instanceof FieldAccess && ((FieldAccess)argument).getExpression() instanceof ThisExpression) ||
				(argument instanceof ArrayAccess && invalidArrayAccess((ArrayAccess)argument)) ||
				(argument instanceof InfixExpression && invalidInfix((InfixExpression)argument)))
			return;
		this.arguments.add(argument.toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArguments().add(argument.toString());
		}
	}

	public boolean visit(QualifiedName node) {
		Name qualifier = node.getQualifier();
		if(Character.isUpperCase(qualifier.getFullyQualifiedName().charAt(0))) {
			types.add(qualifier.getFullyQualifiedName());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getTypes().add(qualifier.getFullyQualifiedName());
			}
			variables.add(node.toString());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(node.toString());
			}
		}
		else if(qualifier instanceof SimpleName && !(node.getParent() instanceof QualifiedName)) {
			if(node.getName().getIdentifier().equals("length")) {
				variables.add(node.toString());
				if(current.getUserObject() != null) {
					AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
					anonymous.getVariables().add(node.toString());
				}
			}
			else {
				String qualifierIdentifier = ((SimpleName)qualifier).getIdentifier();
				MethodDeclaration parentMethodDeclaration = findParentMethodDeclaration(node);
				if(parentMethodDeclaration != null) {
					boolean qualifierIsParameter = false;
					List<SingleVariableDeclaration> parameters = parentMethodDeclaration.parameters();
					for(SingleVariableDeclaration parameter : parameters) {
						if(parameter.getName().getIdentifier().equals(qualifierIdentifier)) {
							qualifierIsParameter = true;
							break;
						}
					}
					if(qualifierIsParameter) {
						variables.add(node.toString());
						if(current.getUserObject() != null) {
							AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
							anonymous.getVariables().add(node.toString());
						}
					}
				}
				EnhancedForStatement enhancedFor = findParentEnhancedForStatement(node);
				if(enhancedFor != null) {
					if(enhancedFor.getParameter().getName().getIdentifier().equals(qualifierIdentifier)) {
						variables.add(node.toString());
						if(current.getUserObject() != null) {
							AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
							anonymous.getVariables().add(node.toString());
						}
					}
				}
			}
		}
		return super.visit(node);
	}

	private EnhancedForStatement findParentEnhancedForStatement(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof EnhancedForStatement) {
				return (EnhancedForStatement)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private MethodDeclaration findParentMethodDeclaration(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof MethodDeclaration) {
				return (MethodDeclaration)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	public boolean visit(CastExpression node) {
		Expression castExpression = node.getExpression();
		if(castExpression instanceof SimpleName) {
			variables.add(node.toString());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(node.toString());
			}
		}
		return super.visit(node);
	}

	public boolean visit(LambdaExpression node) {
		LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, node);
		lambdas.add(lambda);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getLambdas().add(lambda);
		}
		return false;
	}

	public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
		return this.methodInvocationMap;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public List<String> getStringLiterals() {
		return stringLiterals;
	}

	public List<String> getNumberLiterals() {
		return numberLiterals;
	}

	public List<String> getNullLiterals() {
		return nullLiterals;
	}

	public List<String> getBooleanLiterals() {
		return booleanLiterals;
	}

	public List<String> getTypeLiterals() {
		return typeLiterals;
	}

	public Map<String, List<ObjectCreation>> getCreationMap() {
		return creationMap;
	}

	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public List<String> getArrayAccesses() {
		return arrayAccesses;
	}

	public List<String> getPrefixExpressions() {
		return prefixExpressions;
	}

	public List<String> getPostfixExpressions() {
		return postfixExpressions;
	}

	public List<String> getArguments() {
		return this.arguments;
	}

	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	public List<String> getVariables() {
		return variables;
	}

	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	private static boolean invalidArrayAccess(ArrayAccess e) {
		return e.getArray() instanceof SimpleName && simpleNameOrNumberLiteral(e.getIndex());
	}

	private static boolean invalidInfix(InfixExpression e) {
		return simpleNameOrNumberLiteral(e.getLeftOperand()) && simpleNameOrNumberLiteral(e.getRightOperand());
	}

	private static boolean simpleNameOrNumberLiteral(Expression e) {
		return e instanceof SimpleName || e instanceof NumberLiteral;
	}
}
