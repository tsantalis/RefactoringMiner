package org.refactoringminer.astDiff.visitors;

import static com.github.gumtreediff.tree.TypeSet.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtArrayAccessExpression;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtBreakExpression;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression;
import org.jetbrains.kotlin.psi.KtCatchClause;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtClassLiteralExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtContinueExpression;
import org.jetbrains.kotlin.psi.KtDoWhileExpression;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFinallySection;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtForExpression;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtLambdaArgument;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtModifierListOwner;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtPostfixExpression;
import org.jetbrains.kotlin.psi.KtPrefixExpression;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtSuperExpression;
import org.jetbrains.kotlin.psi.KtThisExpression;
import org.jetbrains.kotlin.psi.KtThrowExpression;
import org.jetbrains.kotlin.psi.KtTryExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.KtValueArgumentList;
import org.jetbrains.kotlin.psi.KtVisitor;
import org.jetbrains.kotlin.psi.KtWhenCondition;
import org.jetbrains.kotlin.psi.KtWhenConditionInRange;
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern;
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.psi.KtWhileExpression;
import org.jetbrains.kotlin.psi.ValueArgument;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class KotlinCompatibleTreeVisitor extends KtVisitor<Void, Tree> {
	private static final Map<String, String> KOTLIN_PRIMITIVE_TO_JAVA = Map.ofEntries(
			Map.entry("Byte", "byte"),
			Map.entry("Short", "short"),
			Map.entry("Int", "int"),
			Map.entry("Long", "long"),
			Map.entry("Float", "float"),
			Map.entry("Double", "double"),
			Map.entry("Boolean", "boolean"),
			Map.entry("Char", "char"),
			Map.entry("Unit", "void")
	);

	private final TreeContext context = new TreeContext();
	private final KtFile ktFile;
	private final String source;

	public KotlinCompatibleTreeVisitor(KtFile ktFile, String source) {
		this.ktFile = ktFile;
		this.source = source;
	}

	public TreeContext getTreeContext() {
		Tree root = createTree("CompilationUnit", "", ktFile);
		context.setRoot(root);
		for(PsiElement child : children(ktFile)) {
			if(child instanceof KtPackageDirective packageDirective) {
				add(root, buildPackageDeclaration(packageDirective));
			}
			else if(child instanceof KtImportList importList) {
				for(KtImportDirective importDirective : importList.getImports()) {
					add(root, buildImportDeclaration(importDirective));
				}
			}
			else if(child instanceof KtClass ktClass) {
				add(root, buildClassDeclaration(ktClass));
			}
			else if(child instanceof KtObjectDeclaration objectDeclaration) {
				add(root, buildObjectDeclaration(objectDeclaration));
			}
			else if(child instanceof KtNamedFunction function) {
				add(root, buildFunctionDeclaration(function));
			}
			else if(child instanceof KtProperty property) {
				add(root, buildPropertyDeclaration(property, true));
			}
			else if(child instanceof PsiComment comment) {
				add(root, buildComment(comment));
			}
		}
		return context;
	}

	private Tree buildPackageDeclaration(KtPackageDirective directive) {
		Tree packageDeclaration = createTree("PackageDeclaration", "", directive);
		String qualifiedName = directive.getQualifiedName();
		if(!qualifiedName.isEmpty()) {
			add(packageDeclaration, createContainedTree("QualifiedName", qualifiedName, directive, qualifiedName));
		}
		return packageDeclaration;
	}

	private Tree buildImportDeclaration(KtImportDirective directive) {
		Tree importDeclaration = createTree("ImportDeclaration", "", directive);
		if(directive.getImportedFqName() != null) {
			String importName = directive.getImportedFqName().asString();
			if(directive.isAllUnder()) {
				importName += ".*";
			}
			add(importDeclaration, createContainedTree("QualifiedName", importName, directive, importName));
		}
		return importDeclaration;
	}

	private Tree buildObjectDeclaration(KtObjectDeclaration declaration) {
		Tree typeDeclaration = createTree("TypeDeclaration", "", declaration);
		addAnnotations(typeDeclaration, declaration);
		addTypeDeclarationKind(typeDeclaration, declaration, "object");
		addName(typeDeclaration, declaration.getNameIdentifier(), declaration.getName());
		processClassBody(typeDeclaration, declaration.getBody());
		return typeDeclaration;
	}

	private Tree buildClassDeclaration(KtClass declaration) {
		Tree typeDeclaration = createTree(declaration.isEnum() ? "EnumDeclaration" : "TypeDeclaration", "", declaration);
		addAnnotations(typeDeclaration, declaration);
		String kind = declaration.isInterface() ? "interface" : declaration.isEnum() ? "enum" : "class";
		addTypeDeclarationKind(typeDeclaration, declaration, kind);
		addName(typeDeclaration, declaration.getNameIdentifier(), declaration.getName());
		for(KtParameter parameter : declaration.getPrimaryConstructorParameters()) {
			if(parameter.hasValOrVar()) {
				add(typeDeclaration, buildPrimaryConstructorProperty(parameter));
			}
		}
		processClassBody(typeDeclaration, declaration.getBody());
		return typeDeclaration;
	}

	private Tree buildPrimaryConstructorProperty(KtParameter parameter) {
		Tree fieldDeclaration = createTree("FieldDeclaration", "", parameter);
		addTypeReference(fieldDeclaration, parameter.getTypeReference());
		Tree fragment = createVariableDeclarationFragment(parameter, parameter.getNameIdentifier(), parameter.getDefaultValue());
		addName(fragment, parameter.getNameIdentifier(), parameter.getName());
		if(parameter.getDefaultValue() != null) {
			add(fragment, buildExpression(parameter.getDefaultValue()));
		}
		add(fieldDeclaration, fragment);
		return fieldDeclaration;
	}

	private void processClassBody(Tree parent, KtClassBody body) {
		if(body == null) {
			return;
		}
		for(PsiElement child : children(body)) {
			if(child instanceof KtNamedFunction function) {
				add(parent, buildFunctionDeclaration(function));
			}
			else if(child instanceof KtProperty property) {
				add(parent, buildPropertyDeclaration(property, true));
			}
			else if(child instanceof KtClass ktClass) {
				add(parent, buildClassDeclaration(ktClass));
			}
			else if(child instanceof KtObjectDeclaration objectDeclaration) {
				add(parent, buildObjectDeclaration(objectDeclaration));
			}
			else if(child instanceof PsiComment comment) {
				add(parent, buildComment(comment));
			}
		}
	}

	private Tree buildFunctionDeclaration(KtNamedFunction function) {
		Tree methodDeclaration = createTree("MethodDeclaration", "", function);
		addAnnotations(methodDeclaration, function);
		addName(methodDeclaration, function.getNameIdentifier(), function.getName());
		for(KtParameter parameter : function.getValueParameters()) {
			add(methodDeclaration, buildParameter(parameter));
		}
		addTypeReference(methodDeclaration, function.getTypeReference());
		if(function.getBodyBlockExpression() != null) {
			add(methodDeclaration, buildBlock(function.getBodyBlockExpression()));
		}
		else if(function.getBodyExpression() != null) {
			Tree block = createTree("Block", "", function.getBodyExpression());
			Tree returnStatement = createTree("ReturnStatement", "", function.getBodyExpression());
			add(returnStatement, buildExpression(function.getBodyExpression()));
			add(block, returnStatement);
			add(methodDeclaration, block);
		}
		return methodDeclaration;
	}

	private Tree buildParameter(KtParameter parameter) {
		Tree singleVariableDeclaration = createTree("SingleVariableDeclaration", "", parameter);
		addAnnotations(singleVariableDeclaration, parameter);
		addTypeReference(singleVariableDeclaration, parameter.getTypeReference());
		addName(singleVariableDeclaration, parameter.getNameIdentifier(), parameter.getName());
		if(parameter.getDefaultValue() != null) {
			add(singleVariableDeclaration, buildExpression(parameter.getDefaultValue()));
		}
		return singleVariableDeclaration;
	}

	private Tree buildPropertyDeclaration(KtProperty property, boolean field) {
		Tree declaration = createTree(field ? "FieldDeclaration" : "VariableDeclarationStatement", "", property);
		addAnnotations(declaration, property);
		addTypeReference(declaration, property.getTypeReference());
		Tree fragment = createVariableDeclarationFragment(property, property.getNameIdentifier(), property.getInitializer());
		addName(fragment, property.getNameIdentifier(), property.getName());
		if(property.getInitializer() != null) {
			if(property.getEqualsToken() != null) {
				add(fragment, createTree("ASSIGNMENT_OPERATOR", "=", property.getEqualsToken()));
			}
			add(fragment, buildExpression(property.getInitializer()));
		}
		add(declaration, fragment);
		return declaration;
	}

	private Tree buildBlock(KtBlockExpression blockExpression) {
		Tree block = createTree("Block", "", blockExpression);
		for(PsiElement child : children(blockExpression)) {
			if(child instanceof KtProperty property) {
				add(block, buildPropertyDeclaration(property, false));
			}
			else if(child instanceof KtExpression expression) {
				add(block, buildStatement(expression));
			}
			else if(child instanceof PsiComment comment) {
				add(block, buildComment(comment));
			}
		}
		return block;
	}

	private Tree buildStatement(KtExpression expression) {
		if(expression instanceof KtReturnExpression) {
			return buildReturnStatement((KtReturnExpression) expression);
		}
		if(expression instanceof KtThrowExpression) {
			return buildThrowStatement((KtThrowExpression) expression);
		}
		if(expression instanceof KtBreakExpression) {
			return createTree("BreakStatement", "", expression);
		}
		if(expression instanceof KtContinueExpression) {
			return createTree("ContinueStatement", "", expression);
		}
		if(expression instanceof KtIfExpression) {
			return buildIfStatement((KtIfExpression) expression);
		}
		if(expression instanceof KtWhileExpression) {
			return buildWhileStatement((KtWhileExpression) expression);
		}
		if(expression instanceof KtDoWhileExpression) {
			return buildDoStatement((KtDoWhileExpression) expression);
		}
		if(expression instanceof KtForExpression) {
			return buildForStatement((KtForExpression) expression);
		}
		if(expression instanceof KtTryExpression) {
			return buildTryStatement((KtTryExpression) expression);
		}
		return buildExpressionStatement(expression);
	}

	private Tree buildReturnStatement(KtReturnExpression expression) {
		Tree returnStatement = createTree("ReturnStatement", "", expression);
		if(expression.getReturnedExpression() != null) {
			add(returnStatement, buildExpression(expression.getReturnedExpression()));
		}
		return returnStatement;
	}

	private Tree buildThrowStatement(KtThrowExpression expression) {
		Tree throwStatement = createTree("ThrowStatement", "", expression);
		if(expression.getThrownExpression() != null) {
			add(throwStatement, buildExpression(expression.getThrownExpression()));
		}
		return throwStatement;
	}

	private Tree buildIfStatement(KtIfExpression expression) {
		Tree ifStatement = createTree("IfStatement", "", expression);
		if(expression.getCondition() != null) {
			add(ifStatement, buildExpression(expression.getCondition()));
		}
		if(expression.getThen() != null) {
			add(ifStatement, buildStatementOrBlock(expression.getThen()));
		}
		if(expression.getElse() != null) {
			add(ifStatement, buildStatementOrBlock(expression.getElse()));
		}
		return ifStatement;
	}

	private Tree buildWhileStatement(KtWhileExpression expression) {
		Tree whileStatement = createTree("WhileStatement", "", expression);
		if(expression.getCondition() != null) {
			add(whileStatement, buildExpression(expression.getCondition()));
		}
		if(expression.getBody() != null) {
			add(whileStatement, buildStatementOrBlock(expression.getBody()));
		}
		return whileStatement;
	}

	private Tree buildDoStatement(KtDoWhileExpression expression) {
		Tree doStatement = createTree("DoStatement", "", expression);
		if(expression.getBody() != null) {
			add(doStatement, buildStatementOrBlock(expression.getBody()));
		}
		if(expression.getCondition() != null) {
			add(doStatement, buildExpression(expression.getCondition()));
		}
		return doStatement;
	}

	private Tree buildForStatement(KtForExpression expression) {
		Tree forStatement = createTree("EnhancedForStatement", "", expression);
		if(expression.getLoopParameter() != null) {
			add(forStatement, buildParameter(expression.getLoopParameter()));
		}
		if(expression.getLoopRange() != null) {
			add(forStatement, buildExpression(expression.getLoopRange()));
		}
		if(expression.getBody() != null) {
			add(forStatement, buildStatementOrBlock(expression.getBody()));
		}
		return forStatement;
	}

	private Tree buildTryStatement(KtTryExpression expression) {
		Tree tryStatement = createTree("TryStatement", "", expression);
		if(expression.getTryBlock() != null) {
			add(tryStatement, buildBlock(expression.getTryBlock()));
		}
		for(KtCatchClause catchClause : expression.getCatchClauses()) {
			add(tryStatement, buildCatchClause(catchClause));
		}
		KtFinallySection finallyBlock = expression.getFinallyBlock();
		if(finallyBlock != null && finallyBlock.getFinalExpression() != null) {
			Tree finallyTree = createTree("Finally", "", finallyBlock);
			add(finallyTree, buildBlock(finallyBlock.getFinalExpression()));
			add(tryStatement, finallyTree);
		}
		return tryStatement;
	}

	private Tree buildCatchClause(KtCatchClause catchClause) {
		Tree catchTree = createTree("CatchClause", "", catchClause);
		if(catchClause.getCatchParameter() != null) {
			add(catchTree, buildParameter(catchClause.getCatchParameter()));
		}
		if(catchClause.getCatchBody() != null) {
			add(catchTree, buildStatementOrBlock(catchClause.getCatchBody()));
		}
		return catchTree;
	}

	private Tree buildStatementOrBlock(KtExpression expression) {
		if(expression instanceof KtBlockExpression blockExpression) {
			return buildBlock(blockExpression);
		}
		return buildStatement(expression);
	}

	private Tree buildExpression(KtExpression expression) {
		if(expression instanceof KtDotQualifiedExpression dotQualifiedExpression) {
			return buildQualifiedExpression(dotQualifiedExpression);
		}
		if(expression instanceof KtSafeQualifiedExpression safeQualifiedExpression) {
			return buildQualifiedExpression(safeQualifiedExpression);
		}
		if(expression instanceof KtCallExpression callExpression) {
			return buildCallExpression(callExpression);
		}
		if(expression instanceof KtLambdaExpression lambdaExpression) {
			return buildLambdaExpression(lambdaExpression);
		}
		if(expression instanceof KtBinaryExpression binaryExpression) {
			return buildBinaryExpression(binaryExpression);
		}
		if(expression instanceof KtBinaryExpressionWithTypeRHS binaryExpression) {
			return buildBinaryExpressionWithTypeRHS(binaryExpression);
		}
		if(expression instanceof KtPrefixExpression prefixExpression) {
			return buildPrefixExpression(prefixExpression);
		}
		if(expression instanceof KtPostfixExpression postfixExpression) {
			return buildPostfixExpression(postfixExpression);
		}
		if(expression instanceof KtArrayAccessExpression arrayAccessExpression) {
			return buildArrayAccessExpression(arrayAccessExpression);
		}
		if(expression instanceof KtParenthesizedExpression parenthesizedExpression) {
			return buildParenthesizedExpression(parenthesizedExpression);
		}
		if(expression instanceof KtWhenExpression whenExpression) {
			return buildWhenExpression(whenExpression);
		}
		if(expression instanceof KtTryExpression tryExpression) {
			return buildTryStatement(tryExpression);
		}
		if(expression instanceof KtObjectLiteralExpression objectLiteralExpression) {
			return buildObjectLiteralExpression(objectLiteralExpression);
		}
		if(expression instanceof KtThisExpression thisExpression) {
			return createTree("ThisExpression", thisExpression.getText(), thisExpression);
		}
		if(expression instanceof KtSuperExpression superExpression) {
			return createTree("SuperExpression", superExpression.getText(), superExpression);
		}
		if(expression instanceof KtClassLiteralExpression classLiteralExpression) {
			return buildClassLiteralExpression(classLiteralExpression);
		}
		if(expression instanceof KtCallableReferenceExpression callableReferenceExpression) {
			return buildCallableReferenceExpression(callableReferenceExpression);
		}
		if(expression instanceof KtNameReferenceExpression nameReferenceExpression) {
			return createTree("SimpleName", nameReferenceExpression.getText(), nameReferenceExpression);
		}
		if(expression instanceof KtConstantExpression constantExpression) {
			return buildConstantExpression(constantExpression);
		}
		if(expression instanceof KtStringTemplateExpression stringTemplateExpression) {
			return createTree("StringLiteral", stringTemplateExpression.getText(), stringTemplateExpression);
		}
		if(expression instanceof KtBlockExpression blockExpression) {
			return buildBlock(blockExpression);
		}
		if(expression instanceof KtIfExpression ifExpression) {
			return buildIfStatement(ifExpression);
		}
		if(expression instanceof KtReturnExpression returnExpression) {
			return buildReturnStatement(returnExpression);
		}
		return buildGenericExpression(expression);
	}

	private Tree buildQualifiedExpression(KtQualifiedExpression expression) {
		Tree methodInvocation = createTree("MethodInvocation", "", expression);
		Tree receiver = createTree("METHOD_INVOCATION_RECEIVER", "", expression.getReceiverExpression());
		add(receiver, buildExpression(expression.getReceiverExpression()));
		add(methodInvocation, receiver);
		KtExpression selector = expression.getSelectorExpression();
		if(selector instanceof KtCallExpression callExpression) {
			addCallNameAndArguments(methodInvocation, callExpression);
		}
		else if(selector != null) {
			add(methodInvocation, buildExpression(selector));
		}
		return methodInvocation;
	}

	private Tree buildCallExpression(KtCallExpression expression) {
		Tree methodInvocation = createTree("MethodInvocation", "", expression);
		addCallNameAndArguments(methodInvocation, expression);
		return methodInvocation;
	}

	private void addCallNameAndArguments(Tree methodInvocation, KtCallExpression expression) {
		KtExpression callee = expression.getCalleeExpression();
		if(callee != null) {
			add(methodInvocation, createTree("SimpleName", callee.getText(), callee));
		}
		KtValueArgumentList valueArgumentList = expression.getValueArgumentList();
		if(valueArgumentList != null) {
			Tree arguments = createTree("METHOD_INVOCATION_ARGUMENTS", "", valueArgumentList);
			List<KtValueArgument> valueArguments = expression.getValueArguments();
			for(KtValueArgument argument : valueArguments) {
				KtExpression argumentExpression = argument.getArgumentExpression();
				if(argumentExpression != null) {
					add(arguments, buildExpression(argumentExpression));
				}
			}
			add(methodInvocation, arguments);
		}
		for(KtLambdaArgument lambdaArgument : expression.getLambdaArguments()) {
			if(lambdaArgument.getLambdaExpression() != null) {
				add(methodInvocation, buildLambdaExpression(lambdaArgument.getLambdaExpression()));
			}
		}
	}

	private Tree buildBinaryExpression(KtBinaryExpression expression) {
		String operator = expression.getOperationReference().getText();
		boolean assignment = operator.endsWith("=") && !operator.equals("==") && !operator.equals("!=") &&
				!operator.equals("<=") && !operator.equals(">=");
		Tree binary = createTree(assignment ? "Assignment" : "InfixExpression", "", expression);
		if(expression.getLeft() != null) {
			add(binary, buildExpression(expression.getLeft()));
		}
		add(binary, createTree(assignment ? "ASSIGNMENT_OPERATOR" : "INFIX_EXPRESSION_OPERATOR", operator, expression.getOperationReference()));
		if(expression.getRight() != null) {
			add(binary, buildExpression(expression.getRight()));
		}
		return binary;
	}

	private Tree buildBinaryExpressionWithTypeRHS(KtBinaryExpressionWithTypeRHS expression) {
		Tree castExpression = createTree("CastExpression", "", expression);
		if(expression.getLeft() != null) {
			add(castExpression, buildExpression(expression.getLeft()));
		}
		addTypeReference(castExpression, expression.getRight());
		return castExpression;
	}

	private Tree buildPrefixExpression(KtPrefixExpression expression) {
		Tree prefixExpression = createTree("PrefixExpression", "", expression);
		add(prefixExpression, createTree("PREFIX_EXPRESSION_OPERATOR", expression.getOperationReference().getText(), expression.getOperationReference()));
		if(expression.getBaseExpression() != null) {
			add(prefixExpression, buildExpression(expression.getBaseExpression()));
		}
		return prefixExpression;
	}

	private Tree buildPostfixExpression(KtPostfixExpression expression) {
		Tree postfixExpression = createTree("PostfixExpression", "", expression);
		if(expression.getBaseExpression() != null) {
			add(postfixExpression, buildExpression(expression.getBaseExpression()));
		}
		add(postfixExpression, createTree("POSTFIX_EXPRESSION_OPERATOR", expression.getOperationReference().getText(), expression.getOperationReference()));
		return postfixExpression;
	}

	private Tree buildArrayAccessExpression(KtArrayAccessExpression expression) {
		Tree arrayAccess = createTree("ArrayAccess", "", expression);
		if(expression.getArrayExpression() != null) {
			add(arrayAccess, buildExpression(expression.getArrayExpression()));
		}
		for(KtExpression indexExpression : expression.getIndexExpressions()) {
			add(arrayAccess, buildExpression(indexExpression));
		}
		return arrayAccess;
	}

	private Tree buildParenthesizedExpression(KtParenthesizedExpression expression) {
		Tree parenthesizedExpression = createTree("ParenthesizedExpression", "", expression);
		if(expression.getExpression() != null) {
			add(parenthesizedExpression, buildExpression(expression.getExpression()));
		}
		return parenthesizedExpression;
	}

	private Tree buildWhenExpression(KtWhenExpression expression) {
		Tree switchStatement = createTree("SwitchStatement", "", expression);
		if(expression.getSubjectExpression() != null) {
			add(switchStatement, buildExpression(expression.getSubjectExpression()));
		}
		for(KtWhenEntry entry : expression.getEntries()) {
			Tree switchCase = createTree("SwitchCase", "", entry);
			for(KtWhenCondition condition : entry.getConditions()) {
				add(switchCase, buildWhenCondition(condition));
			}
			if(entry.getElseKeyword() != null) {
				add(switchCase, createTree("SimpleName", entry.getElseKeyword().getText(), entry.getElseKeyword()));
			}
			if(entry.getExpression() != null) {
				add(switchCase, buildStatementOrBlock(entry.getExpression()));
			}
			add(switchStatement, switchCase);
		}
		return switchStatement;
	}

	private Tree buildWhenCondition(KtWhenCondition condition) {
		if(condition instanceof KtWhenConditionWithExpression conditionWithExpression) {
			KtExpression conditionExpression = conditionWithExpression.getExpression();
			if(conditionExpression != null) {
				return buildExpression(conditionExpression);
			}
		}
		if(condition instanceof KtWhenConditionInRange rangeCondition) {
			Tree infixExpression = createTree("InfixExpression", "", rangeCondition);
			add(infixExpression, createTree("INFIX_EXPRESSION_OPERATOR", rangeCondition.getOperationReference().getText(), rangeCondition.getOperationReference()));
			if(rangeCondition.getRangeExpression() != null) {
				add(infixExpression, buildExpression(rangeCondition.getRangeExpression()));
			}
			return infixExpression;
		}
		if(condition instanceof KtWhenConditionIsPattern typeCondition) {
			Tree instanceOfExpression = createTree("InstanceofExpression", "", typeCondition);
			add(instanceOfExpression, createTree("INFIX_EXPRESSION_OPERATOR", typeCondition.isNegated() ? "!is" : "is", typeCondition));
			addTypeReference(instanceOfExpression, typeCondition.getTypeReference());
			return instanceOfExpression;
		}
		Tree genericCondition = createTree("Expression", "", condition);
		appendPsiChildren(genericCondition, condition);
		return genericCondition;
	}

	private Tree buildLambdaExpression(KtLambdaExpression expression) {
		Tree lambdaExpression = createTree("LambdaExpression", "", expression);
		for(KtParameter parameter : expression.getValueParameters()) {
			add(lambdaExpression, buildParameter(parameter));
		}
		if(expression.getBodyExpression() != null) {
			add(lambdaExpression, buildBlock(expression.getBodyExpression()));
		}
		return lambdaExpression;
	}

	private Tree buildObjectLiteralExpression(KtObjectLiteralExpression expression) {
		Tree classInstanceCreation = createTree("ClassInstanceCreation", "", expression);
		if(expression.getObjectDeclaration() != null) {
			add(classInstanceCreation, buildObjectDeclaration(expression.getObjectDeclaration()));
		}
		return classInstanceCreation;
	}

	private Tree buildClassLiteralExpression(KtClassLiteralExpression expression) {
		Tree typeLiteral = createTree("TypeLiteral", "", expression);
		if(expression.getReceiverExpression() != null) {
			add(typeLiteral, buildExpression(expression.getReceiverExpression()));
		}
		return typeLiteral;
	}

	private Tree buildCallableReferenceExpression(KtCallableReferenceExpression expression) {
		Tree methodReference = createTree("ExpressionMethodReference", "", expression);
		appendPsiChildren(methodReference, expression);
		return methodReference;
	}

	private Tree buildGenericExpression(KtExpression expression) {
		Tree genericExpression = createTree(expression.getClass().getSimpleName().replaceFirst("^Kt", ""), "", expression);
		appendPsiChildren(genericExpression, expression);
		return genericExpression;
	}

	private Tree buildConstantExpression(KtConstantExpression expression) {
		String text = expression.getText();
		if("true".equals(text) || "false".equals(text)) {
			return createTree("BooleanLiteral", text, expression);
		}
		if("null".equals(text)) {
			return createTree("NullLiteral", text, expression);
		}
		return createTree("NumberLiteral", text, expression);
	}

	private void addTypeReference(Tree parent, KtTypeReference typeReference) {
		if(typeReference == null) {
			return;
		}
		String typeText = typeReference.getText();
		String normalizedType = normalizeType(typeText);
		String primitive = KOTLIN_PRIMITIVE_TO_JAVA.get(normalizedType);
		if(primitive != null) {
			add(parent, createTree("PrimitiveType", primitive, typeReference));
			return;
		}
		Tree simpleType = createTree("SimpleType", typeText, typeReference);
		add(simpleType, createContainedTree("SimpleName", simpleTypeLabel(typeText), typeReference, simpleTypeLabel(typeText)));
		add(parent, simpleType);
	}

	private String normalizeType(String typeText) {
		String normalized = typeText.trim();
		while(normalized.endsWith("?")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	private String simpleTypeLabel(String typeText) {
		String normalized = normalizeType(typeText);
		int genericStart = normalized.indexOf('<');
		if(genericStart >= 0) {
			normalized = normalized.substring(0, genericStart);
		}
		int dot = normalized.lastIndexOf('.');
		if(dot >= 0) {
			normalized = normalized.substring(dot + 1);
		}
		return normalized;
	}

	private void addTypeDeclarationKind(Tree parent, PsiElement declaration, String kind) {
		add(parent, createContainedTree("TYPE_DECLARATION_KIND", kind, declaration, kind));
	}

	private void addName(Tree parent, PsiElement nameIdentifier, String fallbackName) {
		if(nameIdentifier != null) {
			add(parent, createTree("SimpleName", nameIdentifier.getText(), nameIdentifier));
		}
		else if(fallbackName != null) {
			add(parent, createContainedTree("SimpleName", fallbackName, parent, fallbackName));
		}
	}

	private Tree buildComment(PsiComment comment) {
		if(comment instanceof KDoc) {
			return createTree("Javadoc", comment.getText(), comment);
		}
		String typeName = comment.getText().startsWith("//") ? "LineComment" : "BlockComment";
		return createTree(typeName, comment.getText(), comment);
	}

	private void addAnnotations(Tree parent, KtModifierListOwner owner) {
		KtModifierList modifierList = owner.getModifierList();
		if(modifierList == null) {
			return;
		}
		for(PsiElement modifier : children(modifierList)) {
			if(modifier instanceof KtAnnotationEntry annotationEntry) {
				add(parent, buildAnnotation(annotationEntry));
			}
		}
	}

	private Tree buildAnnotation(KtAnnotationEntry annotationEntry) {
		String typeName = annotationEntry.getValueArguments().isEmpty() ? "MarkerAnnotation" : "NormalAnnotation";
		Tree annotation = createTree(typeName, "", annotationEntry);
		KtTypeReference typeReference = annotationEntry.getTypeReference();
		if(typeReference != null) {
			addTypeReference(annotation, typeReference);
		}
		KtValueArgumentList argumentList = annotationEntry.getValueArgumentList();
		if(argumentList != null) {
			Tree arguments = createTree("METHOD_INVOCATION_ARGUMENTS", "", argumentList);
			for(ValueArgument argument : annotationEntry.getValueArguments()) {
				KtExpression expression = argument.getArgumentExpression();
				if(expression != null) {
					add(arguments, buildExpression(expression));
				}
			}
			add(annotation, arguments);
		}
		return annotation;
	}

	private void appendPsiChildren(Tree parent, PsiElement element) {
		for(PsiElement child : children(element)) {
			if(child instanceof PsiWhiteSpace) {
				continue;
			}
			if(child instanceof PsiComment comment) {
				add(parent, buildComment(comment));
			}
			else if(child instanceof KtClass ktClass) {
				add(parent, buildClassDeclaration(ktClass));
			}
			else if(child instanceof KtObjectDeclaration objectDeclaration) {
				add(parent, buildObjectDeclaration(objectDeclaration));
			}
			else if(child instanceof KtNamedFunction function) {
				add(parent, buildFunctionDeclaration(function));
			}
			else if(child instanceof KtProperty property) {
				add(parent, buildPropertyDeclaration(property, false));
			}
			else if(child instanceof KtParameter parameter) {
				add(parent, buildParameter(parameter));
			}
			else if(child instanceof KtExpression expression) {
				add(parent, buildExpression(expression));
			}
			else if(child instanceof KtTypeReference typeReference) {
				addTypeReference(parent, typeReference);
			}
		}
	}

	private Tree createContainedTree(String typeName, String label, PsiElement owner, String searchText) {
		Tree tree = context.createTree(type(typeName), label);
		TextRange range = owner.getTextRange();
		int ownerStart = range.getStartOffset();
		int ownerEnd = range.getEndOffset();
		int start = source.indexOf(searchText, ownerStart);
		int end = start + searchText.length();
		if(start < ownerStart || end > ownerEnd) {
			start = ownerStart;
			end = Math.min(ownerEnd, start + searchText.length());
		}
		tree.setPos(start);
		tree.setLength(Math.max(0, end - start));
		return tree;
	}

	private Tree createContainedTree(String typeName, String label, Tree owner, String searchText) {
		Tree tree = context.createTree(type(typeName), label);
		tree.setPos(owner.getPos());
		tree.setLength(Math.min(searchText.length(), owner.getLength()));
		return tree;
	}

	private Tree createTree(String typeName, String label, PsiElement element) {
		Tree tree = context.createTree(type(typeName), label == null ? "" : label);
		TextRange range = element.getTextRange();
		tree.setPos(range.getStartOffset());
		tree.setLength(range.getLength());
		return tree;
	}

	private Tree createTree(String typeName, String label, int start, int end) {
		Tree tree = context.createTree(type(typeName), label == null ? "" : label);
		tree.setPos(start);
		tree.setLength(Math.max(0, end - start));
		return tree;
	}

	private Tree createVariableDeclarationFragment(PsiElement owner, PsiElement nameIdentifier, KtExpression initializer) {
		int start = nameIdentifier != null ? nameIdentifier.getTextRange().getStartOffset() : owner.getTextRange().getStartOffset();
		int end = initializer != null ? initializer.getTextRange().getEndOffset() :
				nameIdentifier != null ? nameIdentifier.getTextRange().getEndOffset() : owner.getTextRange().getEndOffset();
		return createTree("VariableDeclarationFragment", "", start, end);
	}

	private Tree buildExpressionStatement(KtExpression expression) {
		TextRange range = expression.getTextRange();
		Tree expressionTree = buildExpression(expression);
		int statementEnd = statementEnd(range.getEndOffset());
		if(statementEnd == range.getEndOffset()) {
			return expressionTree;
		}
		Tree expressionStatement = createTree("ExpressionStatement", "", range.getStartOffset(), statementEnd);
		add(expressionStatement, expressionTree);
		return expressionStatement;
	}

	private int statementEnd(int expressionEnd) {
		if(expressionEnd < source.length() && source.charAt(expressionEnd) == ';') {
			return expressionEnd + 1;
		}
		return expressionEnd;
	}

	private void add(Tree parent, Tree child) {
		if(parent != null && child != null) {
			parent.addChild(child);
		}
	}

	private List<PsiElement> children(PsiElement element) {
		List<PsiElement> children = new ArrayList<>();
		for(PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
			children.add(child);
		}
		return children;
	}
}
