package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.kotlin.com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.psi.KtArrayAccessExpression;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression;
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtLabeledExpression;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtPostfixExpression;
import org.jetbrains.kotlin.psi.KtPrefixExpression;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.jetbrains.kotlin.psi.KtThisExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.KtVisitor;
import org.jetbrains.kotlin.psi.ValueArgument;

import static org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.*;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.KotlinFileProcessor;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLType;

public class KotlinVisitor extends KtVisitor<Object, Object> {
	private KtFile cu;
	private String sourceFolder;
	private String filePath;
	private VariableDeclarationContainer container;
	private List<LeafExpression> variables = new ArrayList<>();
	private List<String> types = new ArrayList<>();
	private List<AbstractCall> methodInvocations = new ArrayList<>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<LeafExpression> textBlocks = new ArrayList<>();
	private List<LeafExpression> stringLiterals = new ArrayList<>();
	private List<LeafExpression> charLiterals = new ArrayList<>();
	private List<LeafExpression> numberLiterals = new ArrayList<>();
	private List<LeafExpression> nullLiterals = new ArrayList<>();
	private List<LeafExpression> booleanLiterals = new ArrayList<>();
	private List<LeafExpression> typeLiterals = new ArrayList<>();
	private List<AbstractCall> creations = new ArrayList<>();
	private List<LeafExpression> infixExpressions = new ArrayList<>();
	private List<LeafExpression> assignments = new ArrayList<>();
	private List<String> infixOperators = new ArrayList<>();
	private List<LeafExpression> arrayAccesses = new ArrayList<>();
	private List<LeafExpression> prefixExpressions = new ArrayList<>();
	private List<LeafExpression> postfixExpressions = new ArrayList<>();
	private List<LeafExpression> thisExpressions = new ArrayList<>();
	private List<LeafExpression> arguments = new ArrayList<>();
	private List<LeafExpression> parenthesizedExpressions = new ArrayList<>();
	private List<LeafExpression> castExpressions = new ArrayList<>();
	private List<LeafExpression> instanceofExpressions = new ArrayList<>();
	private List<LeafExpression> patternInstanceofExpressions = new ArrayList<>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	private List<ComprehensionExpression> comprehensions = new ArrayList<ComprehensionExpression>();
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private DefaultMutableTreeNode current = root;
	private Map<String, Set<VariableDeclaration>> activeVariableDeclarations; 
	private final String fileContent;

	public KotlinVisitor(KtFile cu, String sourceFolder, String filePath, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.cu = cu;
		this.sourceFolder = sourceFolder;
		this.filePath = filePath;
		this.container = container;
		this.activeVariableDeclarations = activeVariableDeclarations;
		this.fileContent = fileContent;
	}

	@Override
	public Object visitExpression(KtExpression expression, Object data) {
		if (expression instanceof KtBinaryExpression binaryExpression) {
			this.processBinaryExpression(binaryExpression, data);
		} else if (expression instanceof KtBinaryExpressionWithTypeRHS binaryExpression) {
			this.processBinaryExpression(binaryExpression, data);
		} else if (expression instanceof KtReturnExpression) {
			this.processReturnExpression((KtReturnExpression) expression, data);
		} else if (expression instanceof KtDotQualifiedExpression dotQualifiedExpression) {
			this.processDotQualifiedExpression(dotQualifiedExpression, data);
		} else if (expression instanceof KtCallExpression callExpression) {
			this.processCallExpression(callExpression, data);
		} else if (expression instanceof KtConstructorCalleeExpression callExpression) {
			this.processConstructorCallExpression(callExpression, data);
		} else if (expression instanceof KtPrefixExpression prefixExpression) {
			this.processPrefixExpression(prefixExpression, data);
		} else if (expression instanceof KtPostfixExpression postfixExpression) {
			this.processPostfixExpression(postfixExpression, data);
		} else if (expression instanceof KtThisExpression thisExpression) {
			this.processThisExpression(thisExpression);
		} else if (expression instanceof KtConstantExpression constantExpression) {
			this.processConstantExpression(constantExpression);
		} else if (expression instanceof KtNameReferenceExpression nameReferenceExpression) {
			this.processReferenceExpression(nameReferenceExpression);
		} else if (expression instanceof KtParenthesizedExpression parenthesizedExpression) {
			this.processParenthesizedExpression(parenthesizedExpression, data);
		} else if (expression instanceof KtStringTemplateExpression stringTemplate) {
			this.processStringTemplateExpression(stringTemplate);
		} else if (expression instanceof KtArrayAccessExpression arrayAccess) {
			this.processArrayAccess(arrayAccess, data);
		} else if (expression instanceof KtSafeQualifiedExpression qualifiedExpression) {
			this.processSafeQualifiedExpression(qualifiedExpression, data);
		} else if (expression instanceof KtLambdaExpression lambdaExpression) {
			this.processLambdaExpression(lambdaExpression, data);
		} else if (expression instanceof KtProperty property) {
			this.processPropertyExpression(property, data);
		} else if (expression instanceof KtObjectLiteralExpression objectLiteralExpression) {
			this.processObjectLiteralExpression(objectLiteralExpression, data);
		} else if (expression instanceof KtLabeledExpression labeledExpression) {
			this.processLabeledExpression(labeledExpression, data);
		}
		return super.visitExpression(expression, data);
	}

	private void processObjectLiteralExpression(KtObjectLiteralExpression objectLiteralExpression, Object data) {
		KtObjectDeclaration objectDeclaration = objectLiteralExpression.getObjectDeclaration();
		UMLType type = null;
		for(KtSuperTypeListEntry entry : objectDeclaration.getSuperTypeListEntries()) {
			type = UMLType.extractTypeObject(cu, sourceFolder, filePath, fileContent, entry.getTypeReference(), 0);
			break;
		}
		LocationInfo anonymousLocationInfo = new LocationInfo(cu, sourceFolder, filePath, objectDeclaration, CodeElementType.ANONYMOUS_CLASS_DECLARATION);
		String leftSide = null;
		if(objectLiteralExpression.getParent() instanceof KtBinaryExpression binary) {
			leftSide = "." + binary.getLeft().getText() + ".";
		}
		else {
			leftSide = ".";
		}
		String codePath = container.getName() + leftSide + type.getClassType();
		UMLAnonymousClass anonymousClass =  new UMLAnonymousClass(container.getClassName(), codePath, codePath, anonymousLocationInfo, Collections.emptyList());
		KotlinFileProcessor.processClassBody(cu, sourceFolder, filePath, fileContent, Collections.emptyList(), container.getComments(), anonymousClass, activeVariableDeclarations, objectDeclaration.getBody(), null);
		if(container instanceof LambdaExpressionObject lambda) {
			lambda.getOwner().getAnonymousClassList().add(anonymousClass);
		}
		else {
			container.getAnonymousClassList().add(anonymousClass);
		}
		anonymousClass.addParentContainer(container);
		AnonymousClassDeclarationObject anonymousObject = new AnonymousClassDeclarationObject(cu, sourceFolder, filePath, objectDeclaration);
		anonymousClassDeclarations.add(anonymousObject);
	}

	public Object visitDelegatedSuperTypeEntry(KtDelegatedSuperTypeEntry entry, Object data) {
		KtExpression delegateExpression = entry.getDelegateExpression();
		this.visitExpression(delegateExpression, data);
		return super.visitDelegatedSuperTypeEntry(entry, data);
	}

	public Object visitSuperTypeCallEntry(KtSuperTypeCallEntry entry, Object data) {
		ObjectCreation invocation = new ObjectCreation(cu, sourceFolder, filePath, entry, container, fileContent);
		creations.add(invocation);
		KtConstructorCalleeExpression callee = entry.getCalleeExpression();
		this.visitExpression(callee, data);
		List<? extends ValueArgument> arguments = entry.getValueArguments();
		for (ValueArgument argument : arguments) {
			processArgument(argument, data);
		}
		return super.visitSuperTypeCallEntry(entry, data);
	}

	public Object visitSuperTypeEntry(KtSuperTypeEntry entry, Object data) {
		visitTypeReference(entry.getTypeReference(), data);
		return super.visitSuperTypeEntry(entry, data);
	}

	private void processLabeledExpression(KtLabeledExpression labeledExpression, Object data) {
		KtExpression baseExpression = labeledExpression.getBaseExpression();
		if(baseExpression != null) {
			this.visitExpression(baseExpression, data);
		}
	}

	private void processPropertyExpression(KtProperty ktProperty, Object data) {
		if (ktProperty.getInitializer() != null)
			this.visitExpression(ktProperty.getInitializer(), data);
		if (ktProperty.getDelegateExpression() != null)
			this.visitExpression(ktProperty.getDelegateExpression(), data);
	}

	private void processStringTemplateExpression(KtStringTemplateExpression expression) {
		LeafExpression literal = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.STRING_LITERAL, container);
		stringLiterals.add(literal);
	}

	private void processArrayAccess(KtArrayAccessExpression expression, Object data) {
		LeafExpression arrayAccess = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.ARRAY_ACCESS, container);
		arrayAccesses.add(arrayAccess);
		KtExpression arrayExpression = expression.getArrayExpression();
		if (arrayExpression != null) {
			this.visitExpression(arrayExpression, data);
		}
		for(KtExpression index : expression.getIndexExpressions()) {
			this.visitExpression(index, data);
		}
	}

	private void processReferenceExpression(KtReferenceExpression expression) {
		if (expression instanceof KtNameReferenceExpression && !(expression.getParent() instanceof KtCallExpression)) {
			if(expression.getParent() instanceof KtUserType) {
				types.add(expression.getText());
			}
			else {
				LeafExpression name = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.SIMPLE_NAME, container);
				variables.add(name);
			}
		}
	}

	private void processCallExpression(KtCallExpression expression, Object data) {
		List<KtValueArgument> arguments = expression.getValueArguments();
		for (KtValueArgument argument : arguments) {
			processArgument(argument, data);
		}
		KtExpression calleeExpression = expression.getCalleeExpression();
		if(calleeExpression instanceof KtNameReferenceExpression nameReference && Character.isUpperCase(nameReference.getReferencedName().charAt(0))) {
			ObjectCreation creation = new ObjectCreation(cu, sourceFolder, filePath, expression, container, fileContent);
			creations.add(creation);
			types.add(creation.getType().toString());
		}
		OperationInvocation invocation = new OperationInvocation(cu, sourceFolder, filePath, expression, container, fileContent);
		methodInvocations.add(invocation);
		if(invocation.getExpression() != null && Character.isUpperCase(invocation.getExpression().charAt(0))) {
			types.add(invocation.getExpression());
		}
		if(expression.getCalleeExpression() != null) {
			this.visitExpression(expression.getCalleeExpression(), data);
		}
	}

	private void processConstructorCallExpression(KtConstructorCalleeExpression expression, Object data) {
		this.visitExpression(expression.getConstructorReferenceExpression(), data);
	}

	private void processArgument(KtValueArgument argument, Object data) {
		KtExpression argumentExpression = argument.getArgumentExpression();
		this.visitExpression(argumentExpression, data);
	}

	private void processArgument(ValueArgument argument, Object data) {
		KtExpression argumentExpression = argument.getArgumentExpression();
		this.visitExpression(argumentExpression, data);
	}

	private void processThisExpression(KtThisExpression expression) {
		//if(!(expression.getParent() instanceof FieldAccess)) {
			LeafExpression thisExpression = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.THIS_EXPRESSION, container);
			thisExpressions.add(thisExpression);
		//}
	}

	private void processConstantExpression(KtConstantExpression expression) {
		IStubElementType elementType = expression.getElementType();
		if (elementType == BOOLEAN_CONSTANT) {
			LeafExpression literal = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.BOOLEAN_LITERAL, container);
			booleanLiterals.add(literal);
		} else if (elementType == NULL) {
			LeafExpression literal = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.NULL_LITERAL, container);
			nullLiterals.add(literal);
		} else if (elementType == INTEGER_CONSTANT || elementType == FLOAT_CONSTANT) {
			LeafExpression literal = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.NUMBER_LITERAL, container);
			numberLiterals.add(literal);
		} else if (elementType == STRING_TEMPLATE) {
			LeafExpression literal = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.STRING_LITERAL, container);
			stringLiterals.add(literal);
		}
	}

	private void processReturnExpression(KtReturnExpression expression, Object data) {
		if (expression.getReturnedExpression() != null)
			this.visitExpression(expression.getReturnedExpression(), data);
	}

	private void processParenthesizedExpression(KtParenthesizedExpression expression, Object data) {
		LeafExpression parenthesized = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.PARENTHESIZED_EXPRESSION, container);
		parenthesizedExpressions.add(parenthesized);
		KtExpression ktExpression = expression.getExpression();
		if (ktExpression != null) {
			this.visitExpression(ktExpression, data);
		}
	}

	private void processBinaryExpression(KtBinaryExpressionWithTypeRHS expression, Object data) {
		if (expression.getLeft() != null)
			this.visitExpression(expression.getLeft(), data);
		if (expression.getRight() != null)
			this.visitTypeReference(expression.getRight(), data);
	}

	public Object visitTypeReference(KtTypeReference entry, Object data) {
		UMLType type = UMLType.extractTypeObject(cu, sourceFolder, filePath, fileContent, entry, 0);
		types.add(type.toString());
		return super.visitTypeReference(entry, data);
	}

	private void processBinaryExpression(KtBinaryExpression expression, Object data) {
		IElementType operationToken = expression.getOperationToken();
		if(operationToken.toString().equals("EQ")) {
			LeafExpression assignment = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.ASSIGNMENT, container);
			assignments.add(assignment);
		}
		else {
			LeafExpression infix = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.INFIX_EXPRESSION, container);
			infixExpressions.add(infix);
			if (operationToken instanceof KtSingleValueToken singleValueToken) {
				this.infixOperators.add(singleValueToken.getValue());
			}
		}
		if (expression.getLeft() != null)
			this.visitExpression(expression.getLeft(), data);
		if (expression.getRight() != null)
			this.visitExpression(expression.getRight(), data);
	}

	private void processDotQualifiedExpression(KtDotQualifiedExpression expression, Object data) {
		//if(expression.getReceiverExpression() instanceof KtNameReferenceExpression && expression.getSelectorExpression() instanceof KtNameReferenceExpression) {
		//	LeafExpression name = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.SIMPLE_NAME, container);
		//	variables.add(name);
		//}
		this.visitExpression(expression.getReceiverExpression(), data);
		if (expression.getSelectorExpression() != null)
			this.visitExpression(expression.getSelectorExpression(), data);
	}

	private void processSafeQualifiedExpression(KtSafeQualifiedExpression safeQualifiedExpression, Object data) {
		if (safeQualifiedExpression.getSelectorExpression() != null)
			this.visitExpression(safeQualifiedExpression.getSelectorExpression(), data);
		this.visitExpression(safeQualifiedExpression.getReceiverExpression(), data);
	}

	private void processLambdaExpression(KtLambdaExpression expression, Object data) {
		LambdaExpressionObject lambda = new LambdaExpressionObject(cu, sourceFolder, filePath, expression, container, activeVariableDeclarations, fileContent);
		lambdas.add(lambda);
		if (expression.getBodyExpression() != null)
			this.visitExpression(expression.getBodyExpression(), data);
	}

	private void processPrefixExpression(KtPrefixExpression expression, Object data) {
		LeafExpression prefix = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.PREFIX_EXPRESSION, container);
		prefixExpressions.add(prefix);
		if (expression.getBaseExpression() != null)
			this.visitExpression(expression.getBaseExpression(), data);
	}

	private void processPostfixExpression(KtPostfixExpression expression, Object data) {
		LeafExpression postfix = new LeafExpression(cu, sourceFolder, filePath, expression, CodeElementType.POSTFIX_EXPRESSION, container);
		postfixExpressions.add(postfix);
		if (expression.getBaseExpression() != null)
			this.visitExpression(expression.getBaseExpression(), data);
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

	public List<LeafExpression> getTextBlocks() {
		return textBlocks;
	}

	public List<LeafExpression> getStringLiterals() {
		return stringLiterals;
	}

	public List<LeafExpression> getCharLiterals() {
		return charLiterals;
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

	public List<LeafExpression> getAssignments() {
		return assignments;
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

	public List<LeafExpression> getCastExpressions() {
		return castExpressions;
	}

	public List<LeafExpression> getInstanceofExpressions() {
		return instanceofExpressions;
	}

	public List<LeafExpression> getPatternInstanceofExpressions() {
		return patternInstanceofExpressions;
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

	public List<ComprehensionExpression> getComprehensions() {
		return comprehensions;
	}
}
