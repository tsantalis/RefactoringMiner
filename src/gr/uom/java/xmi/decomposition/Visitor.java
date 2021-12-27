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

import com.intellij.psi.*;
import gr.uom.java.xmi.Formatter;
import org.jetbrains.annotations.NotNull;

public class Visitor extends PsiRecursiveElementWalkingVisitor {
	public static final Pattern METHOD_INVOCATION_PATTERN = Pattern.compile("!(\\w|\\.)*@\\w*");
	public static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
	private PsiFile cu;
	private String filePath;
	private List<String> variables = new ArrayList<String>();
	private List<String> types = new ArrayList<String>();
	private Map<String, List<AbstractCall>> methodInvocationMap = new LinkedHashMap<String, List<AbstractCall>>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<String> stringLiterals = new ArrayList<String>();
	private List<String> numberLiterals = new ArrayList<String>();
	private List<String> nullLiterals = new ArrayList<String>();
	private List<String> booleanLiterals = new ArrayList<String>();
	private List<String> typeLiterals = new ArrayList<String>();
	private Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<String, List<ObjectCreation>>();
	private List<String> infixExpressions = new ArrayList<String>();
	private List<String> infixOperators = new ArrayList<String>();
	private List<String> arrayAccesses = new ArrayList<String>();
	private List<String> prefixExpressions = new ArrayList<String>();
	private List<String> postfixExpressions = new ArrayList<String>();
	private List<String> arguments = new ArrayList<String>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	private Set<PsiMethodCallExpression> builderPatternChains = new LinkedHashSet<PsiMethodCallExpression>();
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private DefaultMutableTreeNode current = root;

	public Visitor(PsiFile cu, String filePath) {
		this.cu = cu;
		this.filePath = filePath;
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
		}
		if (visitSubtree) {
			super.visitElement(element);
		}
	}

	private void visit(PsiArrayAccessExpression node) {
		String source = Formatter.format(node);
		arrayAccesses.add(source);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArrayAccesses().add(source);
		}
	}

	private void visit(PsiPrefixExpression node) {
		String source = Formatter.format(node);
		prefixExpressions.add(source);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPrefixExpressions().add(source);
		}
	}

	private void visit(PsiPostfixExpression node) {
		String source = Formatter.format(node);
		postfixExpressions.add(source);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPostfixExpressions().add(source);
		}
	}

	private void visit(PsiConditionalExpression node) {
		TernaryOperatorExpression ternary = new TernaryOperatorExpression(cu, filePath, node);
		ternaryOperatorExpressions.add(ternary);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTernaryOperatorExpressions().add(ternary);
		}
	}

	private void visit(PsiBinaryExpression node) {
		String binaryExpression = Formatter.format(node);
		String operator = Formatter.format(node.getOperationSign());
		infixExpressions.add(binaryExpression);
		infixOperators.add(operator);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getInfixExpressions().add(binaryExpression);
			anonymous.getInfixOperators().add(operator);
		}
	}

	private void visit(PsiPolyadicExpression node) {
		String polyadicString = Formatter.format(node);
		String operator = Formatter.format(node.getTokenBeforeOperand(node.getOperands()[1]));
		infixExpressions.add(polyadicString);
		infixOperators.add(operator);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getInfixExpressions().add(polyadicString);
			anonymous.getInfixOperators().add(operator);
		}
	}

	private void addCreation(PsiNewExpression node) {
		ObjectCreation creation = new ObjectCreation(cu, filePath, node);
		String nodeAsString = Formatter.format(node);
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
				VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, (PsiLocalVariable) declaredElement);
				variableDeclarations.add(variableDeclaration);
				if(current.getUserObject() != null) {
					AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
					anonymous.getVariableDeclarations().add(variableDeclaration);
				}
			}
			else if(declaredElement instanceof PsiClass) {
				return false;
			}
		}
		return true;
	}

	private void visit(PsiResourceVariable node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node);
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
		for(PsiElement parent : builderPatternChains) {
			if(isParent(node, parent)) {
				return false;
			}
		}
		return true;
	}

	protected void elementFinished(PsiElement element) {
		if (element instanceof PsiAnonymousClass) {
			PsiAnonymousClass node = (PsiAnonymousClass) element;
			DefaultMutableTreeNode parentNode = deleteNode(node);
			for(PsiElement parent : builderPatternChains) {
				if(isParent(node, parent) || isParent(parent, node)) {
					removeAnonymousData();
					break;
				}
			}
			this.current = parentNode;
		}
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
			this.infixExpressions.removeAll(anonymous.getInfixExpressions());
			this.infixOperators.removeAll(anonymous.getInfixOperators());
			this.arguments.removeAll(anonymous.getArguments());
			this.ternaryOperatorExpressions.removeAll(anonymous.getTernaryOperatorExpressions());
			this.anonymousClassDeclarations.removeAll(anonymous.getAnonymousClassDeclarations());
			this.lambdas.removeAll(anonymous.getLambdas());
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
		String source = Formatter.format(node);
		if(value instanceof String) {
			stringLiterals.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getStringLiterals().add(source);
			}
		} else if (value instanceof Number) {
			numberLiterals.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getNumberLiterals().add(source);
			}
		} else if (value instanceof Boolean) {
			booleanLiterals.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getBooleanLiterals().add(source);
			}
		} else if (value == null) {
			nullLiterals.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getNullLiterals().add(source);
			}
		} else {
			// Characters not processed
			assert value instanceof Character;
		}
	}

	private void visit(PsiClassObjectAccessExpression node) {
		String source = Formatter.format(node);
		typeLiterals.add(source);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypeLiterals().add(source);
		}
	}

	public void visit(PsiThisExpression node) {
		if(!(node.getParent() instanceof PsiReferenceExpression)) {
			String source = Formatter.format(node);
			variables.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(source);
			}
		}
	}

	private static boolean isFieldAccessWithThisExpression(PsiElement element) {
		return element instanceof PsiReferenceExpression && ((PsiReferenceExpression)element).getQualifierExpression() instanceof PsiThisExpression;
	}

	private void visit(PsiIdentifier node) {
		if(isFieldAccessWithThisExpression(node.getParent())) {
			PsiReferenceExpression fieldAccess = (PsiReferenceExpression)node.getParent();
			String source = Formatter.format(fieldAccess);
			variables.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(source);
			}
		}
		else if(node.getParent() instanceof PsiMethodCallExpression &&
				Formatter.format(node).equals(((PsiMethodCallExpression)node.getParent()).getMethodExpression().getReferenceName())) {
			// skip method invocation names
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
				node.getParent().getParent() instanceof PsiMethodCallExpression ||
				node.getParent().getParent() instanceof PsiNewExpression)) {
			// skip names being part of qualified names
		}
		else {
			String source = Formatter.format(node);
			variables.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(source);
			}
		}
	}

	private boolean visit(PsiJavaCodeReferenceElement node) {
		// type from PsiNewExpression.getClassReference() or PsiNewExpression.getClassOrAnonymousClassReference()
		String source = Formatter.format(node);
		types.add(source);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(source);
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
		int argumentSize = 0;
		if(argumentList != null) {
			PsiExpression[] arguments = argumentList.getExpressions();
			argumentSize = arguments.length;
			for (PsiExpression argument : arguments) {
				processArgument(argument);
			}
		}
		String methodInvocation = Formatter.format(node);
		PsiReferenceExpression methodExpression = node.getMethodExpression();
		if(methodInvocationMap.isEmpty() && methodExpression.getQualifierExpression() instanceof PsiMethodCallExpression &&
				!(methodExpression.getReferenceName().equals("length") && argumentSize == 0)) {
			builderPatternChains.add(node);
		}
		for(String key : methodInvocationMap.keySet()) {
			List<AbstractCall> invocations = methodInvocationMap.get(key);
			AbstractCall invocation = invocations.get(0);
			if(invocation instanceof OperationInvocation && key.startsWith(methodInvocation) && ((OperationInvocation)invocation).numberOfSubExpressions() > 0 &&
					!(invocation.getName().equals("length") && invocation.getArguments().size() == 0)) {
				builderPatternChains.add(node);
			}
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		if(methodInvocationMap.containsKey(methodInvocation)) {
			methodInvocationMap.get(methodInvocation).add(invocation);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(invocation);
			methodInvocationMap.put(methodInvocation, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(methodInvocation)) {
				anonymousMethodInvocationMap.get(methodInvocation).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(methodInvocation, list);
			}
		}
	}

	private void visit(PsiMethodReferenceExpression node) {
		MethodReference reference = new MethodReference(cu, filePath, node);
		String referenceString = Formatter.format(node);
		if(methodInvocationMap.containsKey(referenceString)) {
			methodInvocationMap.get(referenceString).add(reference);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(reference);
			methodInvocationMap.put(referenceString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(referenceString)) {
				anonymousMethodInvocationMap.get(referenceString).add(reference);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(reference);
				anonymousMethodInvocationMap.put(referenceString, list);
			}
		}
	}

	private void processArgument(PsiExpression argument) {
		if((argument instanceof PsiMethodCallExpression && ((PsiMethodCallExpression)argument).getMethodExpression().getQualifierExpression() instanceof PsiSuperExpression) ||
				(argument instanceof PsiReferenceExpression && !(argument.getParent() instanceof PsiMethodCallExpression || argument instanceof PsiMethodReferenceExpression)) ||
				(argument instanceof PsiLiteral && ((PsiLiteral)argument).getValue() instanceof String) ||
				(argument instanceof PsiLiteral && ((PsiLiteral)argument).getValue() instanceof Boolean) ||
				(argument instanceof PsiLiteral && ((PsiLiteral)argument).getValue() instanceof Number) ||
				isFieldAccessWithThisExpression(argument) ||
				(argument instanceof PsiArrayAccessExpression && invalidArrayAccess((PsiArrayAccessExpression)argument)) ||
				(argument instanceof PsiPolyadicExpression && invalidInfix((PsiPolyadicExpression)argument)))
			return;
		if(argument instanceof PsiMethodReferenceExpression) {
			LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, (PsiMethodReferenceExpression)argument);
			lambdas.add(lambda);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getLambdas().add(lambda);
			}
		}
		String argumentSource = Formatter.format(argument);
		this.arguments.add(argumentSource);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArguments().add(argumentSource);
		}
	}

	private void visit(PsiReferenceExpression node) {
		if (node.getParent() instanceof PsiMethodCallExpression || node instanceof PsiMethodReferenceExpression) {
			return;
		}
		String source = Formatter.format(node);
		PsiExpression qualifier = node.getQualifierExpression();
		if(qualifier != null) {
			String qualifierIdentifier = Formatter.format(qualifier);
			if (Character.isUpperCase(qualifierIdentifier.charAt(0))) {
				types.add(qualifierIdentifier);
				if (current.getUserObject() != null) {
					AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
					anonymous.getTypes().add(qualifierIdentifier);
				}
				variables.add(source);
				if (current.getUserObject() != null) {
					AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
					anonymous.getVariables().add(source);
				}
			} else if (isSimpleName(qualifier) && !(node.getParent() instanceof PsiReferenceExpression)) {
				if (source.equals("length")) {
					variables.add(source);
					if (current.getUserObject() != null) {
						AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
						anonymous.getVariables().add(source);
					}
				} else {
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
						if (qualifierIsParameter) {
							variables.add(source);
							if (current.getUserObject() != null) {
								AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
								anonymous.getVariables().add(source);
							}
						}
					}
					PsiForeachStatement enhancedFor = findParentEnhancedForStatement(node);
					if (enhancedFor != null) {
						if (enhancedFor.getIterationParameter().getName().equals(qualifierIdentifier)) {
							variables.add(source);
							if (current.getUserObject() != null) {
								AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject) current.getUserObject();
								anonymous.getVariables().add(source);
							}
						}
					}
				}
			}
		}
		else {
			variables.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(source);
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
			String source = Formatter.format(node);
			variables.add(source);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(source);
			}
		}
	}

	private boolean visit(PsiLambdaExpression node) {
		LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, node);
		lambdas.add(lambda);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getLambdas().add(lambda);
		}
		return false;
	}

	public Map<String, List<AbstractCall>> getMethodInvocationMap() {
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

	public List<String> getInfixExpressions() {
		return infixExpressions;
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

	private static boolean isSimpleName(PsiElement element) {
		return element instanceof PsiReferenceExpression && ((PsiReferenceExpression)element).getQualifierExpression() == null;
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
