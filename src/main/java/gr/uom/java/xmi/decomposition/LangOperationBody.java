package gr.uom.java.xmi.decomposition;

import static extension.umladapter.UMLModelAdapter.createUMLClass;
import static extension.umladapter.UMLModelAdapter.createUMLOperation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.statement.LangAssertStatement;
import extension.ast.node.statement.LangAsyncStatement;
import extension.ast.node.statement.LangBlock;
import extension.ast.node.statement.LangBreakStatement;
import extension.ast.node.statement.LangCaseStatement;
import extension.ast.node.statement.LangCatchClause;
import extension.ast.node.statement.LangContinueStatement;
import extension.ast.node.statement.LangDelStatement;
import extension.ast.node.statement.LangExpressionStatement;
import extension.ast.node.statement.LangForStatement;
import extension.ast.node.statement.LangGlobalStatement;
import extension.ast.node.statement.LangIfStatement;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.statement.LangNonLocalStatement;
import extension.ast.node.statement.LangPassStatement;
import extension.ast.node.statement.LangReturnStatement;
import extension.ast.node.statement.LangSwitchStatement;
import extension.ast.node.statement.LangThrowStatement;
import extension.ast.node.statement.LangTryStatement;
import extension.ast.node.statement.LangWhileStatement;
import extension.ast.node.statement.LangWithStatement;
import extension.ast.node.statement.LangYieldStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.ast.visitor.LangVisitor;
import extension.umladapter.UMLAdapterUtil;
import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLClass.ConditionallyCreated;

public class LangOperationBody extends OperationBody {

	// constructor for module class
	public LangOperationBody(LangCompilationUnit cu, String sourceFolder, String filePath, List<LangASTNode> statements, ModuleContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(cu, sourceFolder, filePath, cu, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.bodyHashCode = statements.stream()
				.map(s -> LangVisitor.stringify(s))
				.collect(Collectors.joining()).hashCode();
		this.activeVariableDeclarations = new HashMap<>(activeVariableDeclarations);
		for(LangASTNode statement : statements) {
			processStatement(cu, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		this.activeVariableDeclarations = null;
	}

	// constructor for regular functions and lambdas
	public LangOperationBody(LangCompilationUnit cu, String sourceFolder, String filePath, LangBlock methodBody, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(cu, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.bodyHashCode = LangVisitor.stringify(methodBody).hashCode();
		this.activeVariableDeclarations = new HashMap<>(activeVariableDeclarations);
		addAllInActiveVariableDeclarations(container != null ? container.getParameterDeclarationList() : Collections.emptyList());
		if(container.isDeclaredInAnonymousClass()) {
			UMLAnonymousClass anonymousClassContainer = container.getAnonymousClassContainer().get();
			for(VariableDeclarationContainer parentContainer : anonymousClassContainer.getParentContainers()) {
				for(VariableDeclaration parameterDeclaration : parentContainer.getParameterDeclarationList()) {
					if(parameterDeclaration.isFinal()) {
						addInActiveVariableDeclarations(parameterDeclaration);
					}
				}
			}
		}
		for(LangASTNode statement : methodBody.getStatements()) {
			processStatement(cu, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	private void processStatement(LangCompilationUnit cu, String sourceFolder, String filePath, CompositeStatementObject parent, LangASTNode statement, String fileContent) {
		for(UMLComment comment : comments) {
			if(comment.getParent() != null && comment.getParent().equals(parent))
				continue;
			if(parent.getLocationInfo().subsumes(comment.getLocationInfo())) {
				if(comment.getParent() != null) {
					if(!parent.getLocationInfo().subsumes(comment.getParent().getLocationInfo())) {
						comment.setParent(parent);
					}
				}
				else {
					comment.setParent(parent);
				}
			}
		}
		if(statement instanceof LangBlock) {
			LangBlock block = (LangBlock)statement;
			List<LangASTNode> blockStatements = block.getStatements();
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, block, parent.getDepth()+1, CodeElementType.BLOCK, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(LangASTNode blockStatement : blockStatements) {
				processStatement(cu, sourceFolder, filePath, child, blockStatement, fileContent);
			}
		}
		else if(statement instanceof LangIfStatement) {
			LangIfStatement ifStatement = (LangIfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, ifStatement.getBody(), fileContent);
			if(ifStatement.getElseBody() != null) {
				processStatement(cu, sourceFolder, filePath, child, ifStatement.getElseBody(), fileContent);
			}
		}
		else if(statement instanceof LangForStatement) {
			LangForStatement forStatement = (LangForStatement)statement;
			CompositeStatementObject child = null;
			if(forStatement.getUpdates().isEmpty() && forStatement.getInitializers().size() >= 1) {
				child = new CompositeStatementObject(cu, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, fileContent);
				parent.addStatement(child);
				List<LangSingleVariableDeclaration> initializers = forStatement.getInitializers();
				for(LangSingleVariableDeclaration variableDeclaration : initializers) {
					VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, fileContent);
					child.addVariableDeclaration(vd);
					AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getLangSimpleName(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, fileContent);
					child.addExpression(variableDeclarationName);
				}
				if(forStatement.getCondition() != null) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, forStatement.getCondition(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
			}
			else {
				child = new CompositeStatementObject(cu, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.FOR_STATEMENT, fileContent);
				parent.addStatement(child);
				List<LangSingleVariableDeclaration> initializers = forStatement.getInitializers();
				for(LangSingleVariableDeclaration initializer : initializers) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
				LangASTNode expression = forStatement.getCondition();
				if(expression != null) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, expression, CodeElementType.FOR_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
				List<LangASTNode> updaters = forStatement.getUpdates();
				for(LangASTNode updater : updaters) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER, container, activeVariableDeclarations, fileContent);
					child.addExpression(abstractExpression);
				}
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(cu, sourceFolder, filePath, child, forStatement.getBody(), fileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof LangWhileStatement) {
			LangWhileStatement whileStatement = (LangWhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, sourceFolder, filePath, child, whileStatement.getBody(), fileContent);
		}
		else if(statement instanceof LangExpressionStatement) {
			LangExpressionStatement expressionStatement = (LangExpressionStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, expressionStatement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangSwitchStatement) {
			LangSwitchStatement switchStatement = (LangSwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, switchStatement, parent.getDepth()+1, CodeElementType.SWITCH_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, sourceFolder, filePath, switchStatement.getExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<LangCaseStatement> switchStatements = switchStatement.getCases();
			for(LangCaseStatement switchStatement2 : switchStatements)
				processStatement(cu, sourceFolder, filePath, child, switchStatement2, fileContent);
		}
		else if(statement instanceof LangCaseStatement) {
			LangCaseStatement switchCase = (LangCaseStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, switchCase, parent.getDepth()+1, CodeElementType.SWITCH_CASE, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangYieldStatement) {
			LangYieldStatement yieldStatement = (LangYieldStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, yieldStatement, parent.getDepth()+1, CodeElementType.YIELD_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangAssertStatement) {
			LangAssertStatement assertStatement = (LangAssertStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, assertStatement, parent.getDepth()+1, CodeElementType.ASSERT_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangReturnStatement) {
			LangReturnStatement returnStatement = (LangReturnStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangThrowStatement) {
			LangThrowStatement throwStatement = (LangThrowStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangTryStatement) {
			LangTryStatement tryStatement = (LangTryStatement)statement;
			TryStatementObject child = new TryStatementObject(cu, sourceFolder, filePath, tryStatement, parent.getDepth()+1, fileContent);
			parent.addStatement(child);
			/*List<Expression> resources = tryStatement.resources();
			for(Expression resource : resources) {
				AbstractExpression expression = new AbstractExpression(cu, sourceFolder, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE, container, activeVariableDeclarations, javaFileContent);
				child.addExpression(expression);
			}*/
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(tryStatement.getBody() instanceof LangBlock tryBody) {
				for(LangASTNode blockStatement : tryBody.getStatements()) {
					processStatement(cu, sourceFolder, filePath, child, blockStatement, fileContent);
				}
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
			List<LangCatchClause> catchClauses = tryStatement.getCatchClauses();
			for(LangCatchClause catchClause : catchClauses) {
				LangASTNode catchClauseBody = catchClause.getBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, catchClause, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE, fileContent);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				catchClauseStatementObject.setTryContainer(child);
				LangSimpleName variableDeclaration = catchClause.getExceptionVariable();
				if(variableDeclaration != null) {
					// TODO fix catch exception variable declaration
					//VariableDeclaration vd = new VariableDeclaration(cu, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, javaFileContent);
					//catchClauseStatementObject.addVariableDeclaration(vd);
					AbstractExpression variableDeclarationName = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration, CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container, activeVariableDeclarations, fileContent);
					catchClauseStatementObject.addExpression(variableDeclarationName);
				}
				//if(variableDeclaration.getInitializer() != null) {
				//	AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, sourceFolder, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, javaFileContent);
				//	catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				//}
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
				if(catchClauseBody instanceof LangBlock catchBody) {
					for(LangASTNode blockStatement : catchBody.getStatements()) {
						processStatement(cu, sourceFolder, filePath, catchClauseStatementObject, blockStatement, fileContent);
					}
				}
				removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
			}
			LangASTNode elseBlock = tryStatement.getElseBlock();
			if(elseBlock != null) {
				CompositeStatementObject elseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, elseBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK, fileContent);
				child.setElseClause(elseStatementObject);
				parent.addStatement(elseStatementObject);
				elseStatementObject.setTryContainer(child);
				addStatementInVariableScopes(elseStatementObject);
				if(elseBlock instanceof LangBlock elseBody) {
					for(LangASTNode blockStatement : elseBody.getStatements()) {
						processStatement(cu, sourceFolder, filePath, elseStatementObject, blockStatement, fileContent);
					}
				}
			}
			LangASTNode finallyBlock = tryStatement.getFinallyBlock();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, sourceFolder, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK, fileContent);
				child.setFinallyClause(finallyClauseStatementObject);
				parent.addStatement(finallyClauseStatementObject);
				finallyClauseStatementObject.setTryContainer(child);
				addStatementInVariableScopes(finallyClauseStatementObject);
				if(finallyBlock instanceof LangBlock finallyBody) {
					for(LangASTNode blockStatement : finallyBody.getStatements()) {
						processStatement(cu, sourceFolder, filePath, finallyClauseStatementObject, blockStatement, fileContent);
					}
				}
			}
		}
		else if(statement instanceof LangBreakStatement) {
			LangBreakStatement breakStatement = (LangBreakStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangContinueStatement) {
			LangContinueStatement continueStatement = (LangContinueStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangPassStatement) {
			LangPassStatement emptyStatement = (LangPassStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, emptyStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangWithStatement) {
			LangWithStatement withStatement = (LangWithStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, sourceFolder, filePath, withStatement, parent.getDepth()+1, CodeElementType.WITH_STATEMENT, fileContent);
			parent.addStatement(child);
			List<LangASTNode> contextItems = withStatement.getContextItems();
			for (LangASTNode contextItem : contextItems) {
				// Create expressions for context managers
				AbstractExpression contextExpr = new AbstractExpression(cu, sourceFolder, filePath, contextItem, CodeElementType.VARIABLE_DECLARATION_EXPRESSION, container, activeVariableDeclarations, fileContent);
				child.addExpression(contextExpr);
				// if parent is try statement, then add as resource
				if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) {
					parent.addExpression(contextExpr);
				}
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			LangBlock body = withStatement.getBody();
			processStatement(cu, sourceFolder, filePath, child, body, fileContent);
		}
		else if(statement instanceof LangImportStatement) {	
			LangImportStatement importStatement = (LangImportStatement)statement;
			if(container instanceof UMLOperation op) {
				UMLAdapterUtil.createImports(op.getNestedImports(), sourceFolder, filePath, importStatement);
			}
			else {
				StatementObject child = new StatementObject(cu, sourceFolder, filePath, importStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container, activeVariableDeclarations, fileContent);
				parent.addStatement(child);
				addStatementInVariableScopes(child);
			}
		}
		else if(statement instanceof LangGlobalStatement) {
			// The global statement in Python is a keyword used within a function to declare that a variable being referenced or assigned to within that function refers to a global variable, rather than a local one.
		}
		else if(statement instanceof LangNonLocalStatement) {
			// The nonlocal statement in Python is used within nested functions to declare that a variable refers to a previously bound variable in the nearest enclosing, non-global scope. 
			// It allows an inner function to modify variables defined in an outer (but not global) function's scope. 
		}
		else if(statement instanceof LangDelStatement) {
			// The del statement in Python is used to delete objects, including variables, items within data structures (like lists and dictionaries), and slices of arrays. 
			// It effectively removes references to objects from a given scope or container.
			LangDelStatement delStatement = (LangDelStatement)statement;
			StatementObject child = new StatementObject(cu, sourceFolder, filePath, delStatement, parent.getDepth()+1, CodeElementType.DEL_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof LangAsyncStatement) {
			// In Python, the async statement (keyword) is used to define asynchronous functions, also known as coroutines. 
			// These functions are designed to work with Python's asyncio library for concurrent programming, allowing for efficient handling of I/O-bound operations like network requests or file operations without blocking the main program execution.
		}
		else if(statement instanceof LangMethodDeclaration) {
			LangMethodDeclaration methodDecl = (LangMethodDeclaration)statement;
			String className = container.getClassName() + "." + container.getName();
			UMLOperation nested = createUMLOperation(methodDecl, className, sourceFolder, filePath, fileContent, comments, activeVariableDeclarations);
			if(container instanceof UMLOperation) {
				((UMLOperation)container).addNestedOperation(nested);
			}
		}
		else if(statement instanceof LangTypeDeclaration) {
			LangTypeDeclaration typeDecl = (LangTypeDeclaration)statement;
			UMLClass nestedClass = createUMLClass(typeDecl, filePath, Collections.emptyList(), fileContent, comments);
			if(parent.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) && parent.getParent() != null && parent.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				CompositeStatementObject ifParent = parent.getParent();
				List<AbstractStatement> ifBlocks = ifParent.getStatements();
				if(ifBlocks.size() > 0 && ifBlocks.get(0).equals(parent)) {
					nestedClass.setConditionallyCreated(ConditionallyCreated.IF);
				}
				if(ifBlocks.size() > 1 && ifBlocks.get(1).equals(parent)) {
					nestedClass.setConditionallyCreated(ConditionallyCreated.ELSE);
				}
			}
			if(container instanceof ModuleContainer) {
				((ModuleContainer)container).addNestedClass(nestedClass);
			}
		}
	}
}
