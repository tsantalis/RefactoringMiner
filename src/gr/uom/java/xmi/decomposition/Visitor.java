package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;

import com.intellij.psi.*;
import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import org.jetbrains.annotations.NotNull;

public class Visitor extends PsiRecursiveElementWalkingVisitor {
	public static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
	private PsiFile cu;
	private String filePath;
	private VariableDeclarationContainer container;
	private List<LeafExpression> variables = new ArrayList<>();
	private List<String> types = new ArrayList<>();
	private List<AbstractCall> methodInvocations = new ArrayList<>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<LeafExpression> stringLiterals = new ArrayList<>();
	private List<LeafExpression> numberLiterals = new ArrayList<>();
	private List<LeafExpression> nullLiterals = new ArrayList<>();
	private List<LeafExpression> booleanLiterals = new ArrayList<>();
	private List<LeafExpression> typeLiterals = new ArrayList<>();
	private List<AbstractCall> creations = new ArrayList<>();
	private List<LeafExpression> infixExpressions = new ArrayList<>();
	private List<String> infixOperators = new ArrayList<>();
	private List<LeafExpression> arrayAccesses = new ArrayList<>();
	private List<LeafExpression> prefixExpressions = new ArrayList<>();
	private List<LeafExpression> postfixExpressions = new ArrayList<>();
	private List<LeafExpression> thisExpressions = new ArrayList<>();
	private List<LeafExpression> arguments = new ArrayList<>();
	private List<LeafExpression> parenthesizedExpressions = new ArrayList<>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private DefaultMutableTreeNode current = root;

	public Visitor(PsiFile cu, String filePath, VariableDeclarationContainer container) {
		this.cu = cu;
		this.filePath = filePath;
		this.container = container;
	}

	public void visitElement(@NotNull PsiElement element) {
		boolean visitSubtree = true;
		if (element instanceof PsiArrayAccessExpression) {
			visit((PsiArrayAccessExpression) element);
		} else if (element instanceof PsiPrefixExpression) {
			visit((PsiPrefixExpression) element);
		} else if (element instanceof PsiPostfixExpression) {
			visit((PsiPostfixExpression) element);
		} else if (element instanceof PsiConditionalExpression) {
			visit((PsiConditionalExpression) element);
		} else if (element instanceof PsiBinaryExpression) {
			visit((PsiBinaryExpression) element);
		} else if (element instanceof PsiPolyadicExpression) {
			visit((PsiPolyadicExpression) element);
		} else if (element instanceof PsiNewExpression) {
			visitSubtree = visit((PsiNewExpression) element);
		} else if (element instanceof PsiDeclarationStatement) {
			visitSubtree = visit((PsiDeclarationStatement) element);
		} else if (element instanceof PsiResourceVariable) {
			visit((PsiResourceVariable) element);
		} else if (element instanceof PsiParameter) {
			visit((PsiParameter) element);
		} else if (element instanceof PsiField) {
			visit((PsiField) element);
		} else if (element instanceof PsiAnonymousClass) {
			visitSubtree = visit((PsiAnonymousClass) element);
		} else if (element instanceof PsiLiteralExpression) {
			visit((PsiLiteralExpression) element);
		} else if (element instanceof PsiClassObjectAccessExpression) {
			visit((PsiClassObjectAccessExpression) element);
		} else if (element instanceof PsiThisExpression) {
			visit((PsiThisExpression) element);
		} else if (element instanceof PsiIdentifier) {
			visit((PsiIdentifier) element);
		} else if (element instanceof  PsiMethodReferenceExpression) {
			visit((PsiMethodReferenceExpression) element);
		} else if (element instanceof PsiReferenceExpression) {
			visit((PsiReferenceExpression) element);
		} else if (element instanceof PsiJavaCodeReferenceElement) {
			visitSubtree = visit((PsiJavaCodeReferenceElement) element);
		} else if (element instanceof PsiTypeElement) {
			visitSubtree = visit((PsiTypeElement) element);
		} else if (element instanceof PsiTypeCastExpression) {
			visit((PsiTypeCastExpression) element);
		} else if (element instanceof PsiMethodCallExpression) {
			visit((PsiMethodCallExpression) element);
		} else if (element instanceof PsiLambdaExpression) {
			visitSubtree = visit((PsiLambdaExpression) element);
		} else if (element instanceof PsiParenthesizedExpression) {
			visit((PsiParenthesizedExpression) element);
		}
		if (visitSubtree) {
			super.visitElement(element);
		}
	}

	private void visit(PsiArrayAccessExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.ARRAY_ACCESS, container);
		arrayAccesses.add(expression);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArrayAccesses().add(expression);
		}
	}

	private void visit(PsiPrefixExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.PREFIX_EXPRESSION, container);
		prefixExpressions.add(expression);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPrefixExpressions().add(expression);
		}
	}

	private void visit(PsiPostfixExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.POSTFIX_EXPRESSION, container);
		postfixExpressions.add(expression);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPostfixExpressions().add(expression);
		}
	}

	private void visit(PsiConditionalExpression node) {
		TernaryOperatorExpression ternary = new TernaryOperatorExpression(cu, filePath, node, container);
		ternaryOperatorExpressions.add(ternary);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTernaryOperatorExpressions().add(ternary);
		}
	}

	private void visit(PsiBinaryExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.INFIX_EXPRESSION, container);
		String operator = Formatter.format(node.getOperationSign());
		infixExpressions.add(expression);
		infixOperators.add(operator);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getInfixExpressions().add(expression);
			anonymous.getInfixOperators().add(operator);
		}
	}

	private void visit(PsiPolyadicExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.INFIX_EXPRESSION, container);
		String operator = Formatter.format(node.getTokenBeforeOperand(node.getOperands()[1]));
		infixExpressions.add(expression);
		infixOperators.add(operator);
		//special handling for adding intermediate composite infix expressions
		List<LeafExpression> intermediateInfixExpressions = new ArrayList<>();
		int count = node.getOperands().length;
		String polyadicString = expression.getString();
		while (count > 2 && !operator.equals("+")) {
			PsiExpression lastOperand = node.getOperands()[count-1];
			String lastOperandString = Formatter.format(lastOperand);
			String suffix = " " + operator + " " + lastOperandString;
			if (polyadicString.contains(suffix)) {
				String intermediateInfix = polyadicString.substring(0, polyadicString.lastIndexOf(suffix));
				LeafExpression intermediateInfixExpression = new LeafExpression(cu, filePath, node, intermediateInfix, CodeElementType.INFIX_EXPRESSION, container);
				if (!infixExpressions.contains(intermediateInfixExpression)) {
					infixExpressions.add(intermediateInfixExpression);
					intermediateInfixExpressions.add(intermediateInfixExpression);
				}
			}
			count--;
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getInfixExpressions().add(expression);
			anonymous.getInfixExpressions().addAll(intermediateInfixExpressions);
			anonymous.getInfixOperators().add(operator);
		}
	}

	private void addCreation(PsiNewExpression node) {
		PsiJavaCodeReferenceElement classOrAnonymousClassReference = node.getClassOrAnonymousClassReference();
		if(classOrAnonymousClassReference != null) {
			visit(classOrAnonymousClassReference);
		}
		ObjectCreation creation = new ObjectCreation(cu, filePath, node, container);
		creations.add(creation);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getCreations().add(creation);
		}
	}

	private boolean visit(PsiNewExpression node) {
		addCreation(node);
		if(node.isArrayCreation()) {
			PsiArrayInitializerExpression initializer = node.getArrayInitializer();
			if(initializer != null) {
				PsiExpression[] expressions = initializer.getInitializers();
				if(expressions.length > 10) {
					return false;
				}
			}
		}
		else {
			PsiExpressionList argList = node.getArgumentList();
			if (argList != null) {
				PsiExpression[] arguments = argList.getExpressions();
				for (PsiExpression argument : arguments) {
					processArgument(argument);
				}
			}
		}
		return true;
	}

	private boolean visit(PsiDeclarationStatement node) {
		for (PsiElement declaredElement : node.getDeclaredElements()) {
			if (declaredElement instanceof PsiLocalVariable) {
				visit((PsiLocalVariable) declaredElement);
			}
			else if(declaredElement instanceof PsiClass) {
				return false;
			}
		}
		return true;
	}

	private void visit(PsiLocalVariable node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node, container);
		variableDeclarations.add(variableDeclaration);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getVariableDeclarations().add(variableDeclaration);
		}
	}

	private void visit(PsiResourceVariable node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node, container);
		variableDeclarations.add(variableDeclaration);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getVariableDeclarations().add(variableDeclaration);
		}
	}

	private void visit(PsiParameter node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node, CodeElementType.SINGLE_VARIABLE_DECLARATION, container, node.isVarArgs());
		variableDeclarations.add(variableDeclaration);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getVariableDeclarations().add(variableDeclaration);
		}
	}

	private void visit(PsiField node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node, container);
		variableDeclarations.add(variableDeclaration);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getVariableDeclarations().add(variableDeclaration);
		}
	}

	public boolean visit(PsiAnonymousClass node) {
		DefaultMutableTreeNode childNode = insertNode(node);
		AnonymousClassDeclarationObject childAnonymous = (AnonymousClassDeclarationObject)childNode.getUserObject();
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			currentAnonymous.getAnonymousClassDeclarations().add(childAnonymous);
		}
		anonymousClassDeclarations.add(childAnonymous);
		this.current = childNode;
		return true;
	}

	protected void elementFinished(PsiElement element) {
		if (element instanceof PsiAnonymousClass) {
			PsiAnonymousClass node = (PsiAnonymousClass) element;
			DefaultMutableTreeNode parentNode = deleteNode(node);
			removeAnonymousData();
			this.current = parentNode;
		}
		else if (element instanceof PsiExpressionList && current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
			if(element.getParent().equals(anonymous.getAstNode())) {
				anonymous.clearAll();
			}
		}
	}

	private void removeAnonymousData() {
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			removeLast(this.variables, anonymous.getVariables());
			removeLastString(this.types, anonymous.getTypes());
			removeLast(this.methodInvocations, anonymous.getMethodInvocations());
			removeLast(this.creations, anonymous.getCreations());
			this.variableDeclarations.removeAll(anonymous.getVariableDeclarations());
			removeLast(this.stringLiterals, anonymous.getStringLiterals());
			removeLast(this.nullLiterals, anonymous.getNullLiterals());
			removeLast(this.booleanLiterals, anonymous.getBooleanLiterals());
			removeLast(this.typeLiterals, anonymous.getTypeLiterals());
			removeLast(this.numberLiterals, anonymous.getNumberLiterals());
			removeLast(this.infixExpressions, anonymous.getInfixExpressions());
			removeLastString(this.infixOperators, anonymous.getInfixOperators());
			removeLast(this.postfixExpressions, anonymous.getPostfixExpressions());
			removeLast(this.prefixExpressions, anonymous.getPrefixExpressions());
			removeLast(this.thisExpressions, anonymous.getThisExpressions());
			removeLast(this.parenthesizedExpressions, anonymous.getParenthesizedExpressions());
			removeLast(this.arguments, anonymous.getArguments());
			this.ternaryOperatorExpressions.removeAll(anonymous.getTernaryOperatorExpressions());
			this.anonymousClassDeclarations.removeAll(anonymous.getAnonymousClassDeclarations());
			this.lambdas.removeAll(anonymous.getLambdas());
			removeLast(this.arrayAccesses, anonymous.getArrayAccesses());
		}
	}

	private static void removeLastString(List<String> parentList, List<String> childList) {
		for(int i=childList.size()-1; i>=0; i--) {
			String element = childList.get(i);
			int lastIndex = parentList.lastIndexOf(element);
			parentList.remove(lastIndex);
		}
	}

	private static void removeLast(List<? extends LeafExpression> parentList, List<? extends LeafExpression> childList) {
		for(int i=childList.size()-1; i>=0; i--) {
			LeafExpression element = childList.get(i);
			int lastIndex = parentList.lastIndexOf(element);
			parentList.remove(lastIndex);
		}
	}

	private DefaultMutableTreeNode deleteNode(PsiAnonymousClass childAnonymous) {
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

	private DefaultMutableTreeNode insertNode(PsiAnonymousClass childAnonymous) {
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

	private DefaultMutableTreeNode findNode(PsiAnonymousClass anonymous) {
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

	private boolean isParent(PsiElement child, PsiElement parent) {
		PsiElement current = child;
		while(current.getParent() != null) {
			if(current.getParent().equals(parent))
				return true;
			current = current.getParent();
		}
		return false;
	}

	private void visit(PsiLiteralExpression node) {
		Object value = node.getValue();
		if(value instanceof String) {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.STRING_LITERAL, container);
			stringLiterals.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getStringLiterals().add(expression);
			}
		} else if (value instanceof Number) {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.NUMBER_LITERAL, container);
			numberLiterals.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getNumberLiterals().add(expression);
			}
		} else if (value instanceof Boolean) {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.BOOLEAN_LITERAL, container);
			booleanLiterals.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getBooleanLiterals().add(expression);
			}
		} else if (value == null) {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.NULL_LITERAL, container);
			nullLiterals.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getNullLiterals().add(expression);
			}
		} else {
			// Characters not processed
			assert value instanceof Character;
		}
	}

	private void visit(PsiClassObjectAccessExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.TYPE_LITERAL, container);
		typeLiterals.add(expression);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypeLiterals().add(expression);
		}
	}

	public void visit(PsiThisExpression node) {
		if(!isFieldAccessWithThisExpression(node.getParent())) {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.THIS_EXPRESSION, container);
			thisExpressions.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getThisExpressions().add(expression);
			}
		}
	}

	private static boolean isFieldAccessWithThisExpression(PsiElement element) {
		return element instanceof PsiReferenceExpression && ((PsiReferenceExpression)element).getQualifierExpression() instanceof PsiThisExpression &&
				!methodCallReferenceExpression(element);
	}

	private static boolean methodCallReferenceExpression(PsiElement element) {
		return element.getParent() instanceof PsiMethodCallExpression || element instanceof PsiMethodReferenceExpression;
	}

	private void visit(PsiIdentifier node) {
		if(isFieldAccessWithThisExpression(node.getParent())) {
			PsiReferenceExpression fieldAccess = (PsiReferenceExpression)node.getParent();
			LeafExpression fieldAccessExpression = new LeafExpression(cu, filePath, fieldAccess, CodeElementType.FIELD_ACCESS, container);
			variables.add(fieldAccessExpression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(fieldAccessExpression);
			}
		}
		else if(node.getParent() instanceof PsiAnnotation &&
				((PsiAnnotation)node.getParent()).getNameReferenceElement().getReferenceName().equals(Formatter.format(node))) {
			// skip marker annotation names
		}
		else if(node.getParent() instanceof PsiMethod &&
				((PsiMethod)node.getParent()).getName().equals(Formatter.format(node))) {
			// skip method declaration names
		}
		else if(node.getParent() instanceof PsiParameter &&
				node.getParent().getParent() instanceof PsiMethod) {
			// skip method parameter names
		}
		else if(node.getParent() instanceof PsiParameter &&
				node.getParent().getParent() instanceof PsiCatchSection) {
			// skip catch clause formal parameter names
		}
		else if(node.getParent() instanceof PsiReferenceExpression &&
				(node.getParent().getParent() instanceof PsiReferenceExpression ||
				node.getParent().getParent() instanceof PsiExpressionList ||
				node.getParent().getParent() instanceof PsiMethodCallExpression ||
				node.getParent().getParent() instanceof PsiNewExpression ||
				node.getParent().getParent() instanceof PsiIfStatement ||
				node.getParent().getParent() instanceof PsiBinaryExpression)) {
			// skip names being part of qualified names, or method invocation names
		}
		else {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.SIMPLE_NAME, container);
			variables.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(expression);
			}
		}
	}

	private boolean visit(PsiJavaCodeReferenceElement node) {
		// type from PsiNewExpression.getClassReference() or PsiNewExpression.getClassOrAnonymousClassReference()
		if(!(node.getParent() instanceof PsiAnnotation)) {
			String source = Formatter.format(node);
			types.add(source);
			if (current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
				anonymous.getTypes().add(source);
			}
		}
		return false;
	}

	private boolean visit(PsiTypeElement node) {
		String source = Formatter.format(node);
		types.add(source);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(source);
		}
		return false;
	}

	private void visit(PsiMethodCallExpression node) {
		PsiExpressionList argumentList = node.getArgumentList();
		PsiExpression[] arguments = argumentList.getExpressions();
		for (PsiExpression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node, container);
		methodInvocations.add(invocation);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getMethodInvocations().add(invocation);
		}
	}

	private void visit(PsiMethodReferenceExpression node) {
		MethodReference reference = new MethodReference(cu, filePath, node, container);
		methodInvocations.add(reference);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getMethodInvocations().add(reference);
		}
	}

	private void processArgument(PsiExpression argument) {
		if((argument instanceof PsiMethodCallExpression && ((PsiMethodCallExpression)argument).getMethodExpression().getQualifierExpression() instanceof PsiSuperExpression) ||
				isSimpleName(argument) || isQualifiedName(argument) ||
				(argument instanceof PsiLiteral && ((PsiLiteral)argument).getValue() instanceof String) ||
				(argument instanceof PsiLiteral && ((PsiLiteral)argument).getValue() instanceof Boolean) ||
				(argument instanceof PsiLiteral && ((PsiLiteral)argument).getValue() instanceof Number) ||
				isFieldAccessWithThisExpression(argument) ||
				(argument instanceof PsiArrayAccessExpression && invalidArrayAccess((PsiArrayAccessExpression)argument)) ||
				(argument instanceof PsiPolyadicExpression && invalidInfix((PsiPolyadicExpression)argument)) ||
				castExpressionInParenthesizedExpression(argument))
			return;
		if(argument instanceof PsiMethodReferenceExpression) {
			LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, (PsiMethodReferenceExpression)argument, container);
			lambdas.add(lambda);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getLambdas().add(lambda);
			}
		}
		LeafExpression expression = new LeafExpression(cu, filePath, argument, CodeElementType.EXPRESSION, container);
		this.arguments.add(expression);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArguments().add(expression);
		}
	}

	private boolean castExpressionInParenthesizedExpression(PsiExpression argument) {
		if(argument instanceof PsiParenthesizedExpression) {
			PsiExpression parenthesizedExpression = ((PsiParenthesizedExpression)argument).getExpression();
			if(parenthesizedExpression instanceof PsiTypeCastExpression) {
				PsiTypeCastExpression castExpression = (PsiTypeCastExpression)parenthesizedExpression;
				if(isSimpleName(castExpression.getOperand())) {
					return true;
				}
			}
		}
		return false;
	}

	private void visit(PsiReferenceExpression node) {
		if (methodCallReferenceExpression(node)) {
			return;
		}
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.QUALIFIED_NAME, container);
		PsiExpression qualifier = node.getQualifierExpression();
		if(qualifier != null) {
			String qualifierIdentifier = Formatter.format(qualifier);
			if (Character.isUpperCase(qualifierIdentifier.charAt(0))) {
				types.add(qualifierIdentifier);
				if (current.getUserObject() != null) {
					AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
					anonymous.getTypes().add(qualifierIdentifier);
				}
				variables.add(expression);
				if (current.getUserObject() != null) {
					AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
					anonymous.getVariables().add(expression);
				}
			} else if (isSimpleName(qualifier) && !isQualifiedName(node.getParent())) {
				PsiMethod parentMethodDeclaration = findParentMethodDeclaration(node);
				if (parentMethodDeclaration != null) {
					boolean qualifierIsParameter = false;
					PsiParameter[] parameters = parentMethodDeclaration.getParameterList().getParameters();
					for (PsiParameter parameter : parameters) {
						if (parameter.getName().equals(qualifierIdentifier)) {
							qualifierIsParameter = true;
							break;
						}
					}
					boolean qualifiedIsField = false;
					if (!qualifierIsParameter && !parentMethodDeclaration.isConstructor()) {
						PsiClass psiClass = findParentTypeDeclaration(parentMethodDeclaration);
						if (psiClass != null) {
							PsiField[] fields = psiClass.getFields();
							for (PsiField field : fields) {
								if (field.getName().equals(qualifierIdentifier)) {
									qualifiedIsField = true;
									break;
								}
							}
						}
					}
					if (qualifierIsParameter || qualifiedIsField) {
						variables.add(expression);
						if (current.getUserObject() != null) {
							AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
							anonymous.getVariables().add(expression);
						}
					}
				}
				PsiForeachStatement enhancedFor = findParentEnhancedForStatement(node);
				if (enhancedFor != null) {
					if (enhancedFor.getIterationParameter().getName().equals(qualifierIdentifier)) {
						variables.add(expression);
						if (current.getUserObject() != null) {
							AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
							anonymous.getVariables().add(expression);
						}
					}
				}
			}
		}
		else {
			variables.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(expression);
			}
		}
	}

	private PsiForeachStatement findParentEnhancedForStatement(PsiElement node) {
		PsiElement parent = node.getParent();
		while(parent != null) {
			if(parent instanceof PsiForeachStatement) {
				return (PsiForeachStatement)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private PsiClass findParentTypeDeclaration(PsiElement node) {
		PsiElement parent = node.getParent();
		while(parent != null) {
			if(parent instanceof PsiClass) {
				return (PsiClass)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private PsiMethod findParentMethodDeclaration(PsiElement node) {
		PsiElement parent = node.getParent();
		while(parent != null) {
			if(parent instanceof PsiMethod) {
				return (PsiMethod)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private void visit(PsiTypeCastExpression node) {
		PsiExpression castExpression = node.getOperand();
		if(isSimpleName(castExpression)) {
			LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.CAST_EXPRESSION, container);
			variables.add(expression);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(expression);
			}
		}
	}

	private boolean visit(PsiLambdaExpression node) {
		LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, node, container);
		lambdas.add(lambda);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getLambdas().add(lambda);
		}
		return false;
	}

	private void visit(PsiParenthesizedExpression node) {
		LeafExpression expression = new LeafExpression(cu, filePath, node, CodeElementType.PARENTHESIZED_EXPRESSION, container);
		parenthesizedExpressions.add(expression);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getParenthesizedExpressions().add(expression);
		}
	}

	public List<AbstractCall> getMethodInvocations() {
		return methodInvocations;
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

	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
	}

	public List<LeafExpression> getNumberLiterals() {
		return numberLiterals;
	}

	public List<LeafExpression> getNullLiterals() {
		return nullLiterals;
	}

	public List<LeafExpression> getBooleanLiterals() {
		return booleanLiterals;
	}

	public List<LeafExpression> getTypeLiterals() {
		return typeLiterals;
	}

	public List<AbstractCall> getCreations() {
		return creations;
	}

	public List<LeafExpression> getInfixExpressions() {
		return infixExpressions;
	}

	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public List<LeafExpression> getArrayAccesses() {
		return arrayAccesses;
	}

	public List<LeafExpression> getPrefixExpressions() {
		return prefixExpressions;
	}

	public List<LeafExpression> getPostfixExpressions() {
		return postfixExpressions;
	}

	public List<LeafExpression> getThisExpressions() {
		return thisExpressions;
	}

	public List<LeafExpression> getArguments() {
		return this.arguments;
	}

	public List<LeafExpression> getParenthesizedExpressions() {
		return parenthesizedExpressions;
	}

	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	public List<LeafExpression> getVariables() {
		return variables;
	}

	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	private static boolean isSimpleName(PsiElement element) {
		return element instanceof PsiReferenceExpression && ((PsiReferenceExpression)element).getQualifierExpression() == null;
	}

	private static boolean isQualifiedName(PsiElement element) {
		if(element instanceof PsiReferenceExpression && !methodCallReferenceExpression(element)) {
			PsiExpression qualifier = ((PsiReferenceExpression)element).getQualifierExpression();
			if(qualifier != null) {
				if(isSimpleName(qualifier) || isQualifiedName(qualifier)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean invalidArrayAccess(PsiArrayAccessExpression e) {
		return isSimpleName(e.getArrayExpression()) && simpleNameOrNumberLiteral(e.getIndexExpression());
	}

	private static boolean invalidInfix(PsiPolyadicExpression e) {
		PsiExpression[] operands = e.getOperands();
		for(PsiExpression operand : operands) {
			if(!simpleNameOrNumberLiteral(operand)) {
				return false;
			}
		}
		return true;
	}

	private static boolean simpleNameOrNumberLiteral(PsiExpression e) {
		return isSimpleName(e) || (e instanceof PsiLiteral && ((PsiLiteral) e).getValue() instanceof Number);
	}
}
