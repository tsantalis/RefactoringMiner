package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intellij.psi.*;
import gr.uom.java.xmi.Formatter;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

import static gr.uom.java.xmi.decomposition.PsiUtils.isSuperConstructorInvocation;
import static gr.uom.java.xmi.decomposition.PsiUtils.isThisConstructorInvocation;

public class OperationBody {

	private CompositeStatementObject compositeStatement;
	private List<String> stringRepresentation;
	private boolean containsAssertion;
	private Set<VariableDeclaration> activeVariableDeclarations;
	private int bodyHashCode;

	public OperationBody(PsiFile cu, String filePath, PsiCodeBlock methodBody) {
		this(cu, filePath, methodBody, Collections.emptyList());
	}

	public OperationBody(PsiFile cu, String filePath, PsiCodeBlock methodBody, List<VariableDeclaration> parameters) {
		this.compositeStatement = new CompositeStatementObject(cu, filePath, methodBody, 0, CodeElementType.BLOCK);
		this.bodyHashCode = Formatter.format(methodBody).hashCode();
		this.activeVariableDeclarations = new HashSet<VariableDeclaration>();
		this.activeVariableDeclarations.addAll(parameters);
		PsiStatement[] statements = methodBody.getStatements();
		for(PsiStatement statement : statements) {
			processStatement(cu, filePath, compositeStatement, statement);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.getName().startsWith("assert")) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public int statementCount() {
		return compositeStatement.statementCount();
	}

	public CompositeStatementObject getCompositeStatement() {
		return compositeStatement;
	}

	public boolean containsAssertion() {
		return containsAssertion;
	}

	public List<AnonymousClassDeclarationObject> getAllAnonymousClassDeclarations() {
		return new ArrayList<AnonymousClassDeclarationObject>(compositeStatement.getAllAnonymousClassDeclarations());
	}

	public List<AbstractCall> getAllOperationInvocations() {
		List<AbstractCall> invocations = new ArrayList<AbstractCall>();
		Map<String, List<AbstractCall>> invocationMap = compositeStatement.getAllMethodInvocations();
		for(String key : invocationMap.keySet()) {
			invocations.addAll(invocationMap.get(key));
		}
		return invocations;
	}

	public List<LambdaExpressionObject> getAllLambdas() {
		return new ArrayList<LambdaExpressionObject>(compositeStatement.getAllLambdas());
	}

	public List<String> getAllVariables() {
		return new ArrayList<String>(compositeStatement.getAllVariables());
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		return new ArrayList<VariableDeclaration>(compositeStatement.getAllVariableDeclarations());
	}

	public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
		return new ArrayList<VariableDeclaration>(compositeStatement.getVariableDeclarationsInScope(location));
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		return compositeStatement.getVariableDeclaration(variableName);
	}

	private void processStatement(PsiFile cu, String filePath, CompositeStatementObject parent, PsiStatement statement) {
		if(statement instanceof PsiBlockStatement) {
			PsiCodeBlock codeBlock = ((PsiBlockStatement) statement).getCodeBlock();
			processBlock(cu, filePath, parent, codeBlock);
		}
		else if(statement instanceof PsiIfStatement) {
			PsiIfStatement ifStatement = (PsiIfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, ifStatement.getThenBranch());
			if(ifStatement.getElseBranch() != null) {
				processStatement(cu, filePath, child, ifStatement.getElseBranch());
			}
		}
		else if(statement instanceof PsiForStatement) {
			PsiForStatement forStatement = (PsiForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, forStatement, parent.getDepth()+1, CodeElementType.FOR_STATEMENT);
			parent.addStatement(child);
			PsiStatement initializers = forStatement.getInitialization();
			if(initializers instanceof PsiDeclarationStatement) {
				PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) initializers;
				for (PsiElement initializer : declarationStatement.getDeclaredElements()) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER);
					child.addExpression(abstractExpression);
				}
			}
			PsiExpression expression = forStatement.getCondition();
			if(expression != null) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, expression, CodeElementType.FOR_STATEMENT_CONDITION);
				child.addExpression(abstractExpression);
			}
			PsiStatement updaters = forStatement.getUpdate();
			if(updaters instanceof PsiExpressionStatement) {
				PsiExpressionStatement expressionStatement = (PsiExpressionStatement) updaters;
				PsiExpression updater = expressionStatement.getExpression();
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER);
				child.addExpression(abstractExpression);
			}
			else if(updaters instanceof PsiExpressionListStatement) {
				PsiExpressionListStatement expressionListStatement = (PsiExpressionListStatement) updaters;
				PsiExpressionList expressionList = expressionListStatement.getExpressionList();
				for(PsiExpression updater : expressionList.getExpressions()) {
					AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER);
					child.addExpression(abstractExpression);
				}
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			this.activeVariableDeclarations.addAll(variableDeclarations);
			processStatement(cu, filePath, child, forStatement.getBody());
			this.activeVariableDeclarations.removeAll(variableDeclarations);
		}
		else if(statement instanceof PsiForeachStatement) {
			PsiForeachStatement enhancedForStatement = (PsiForeachStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, enhancedForStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT);
			parent.addStatement(child);
			PsiParameter variableDeclaration = enhancedForStatement.getIterationParameter();
			VariableDeclaration vd = new VariableDeclaration(cu, filePath, variableDeclaration, CodeElementType.SINGLE_VARIABLE_DECLARATION);
			child.addVariableDeclaration(vd);
			AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getNameIdentifier(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME);
			child.addExpression(variableDeclarationName);
			if(variableDeclaration.getInitializer() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER);
				child.addExpression(variableDeclarationInitializer);
			}
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, enhancedForStatement.getIteratedValue(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			this.activeVariableDeclarations.addAll(variableDeclarations);
			processStatement(cu, filePath, child, enhancedForStatement.getBody());
			this.activeVariableDeclarations.removeAll(variableDeclarations);
		}
		else if(statement instanceof PsiWhileStatement) {
			PsiWhileStatement whileStatement = (PsiWhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, whileStatement.getBody());
		}
		else if(statement instanceof PsiDoWhileStatement) {
			PsiDoWhileStatement doStatement = (PsiDoWhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, doStatement.getBody());
		}
		else if(statement instanceof PsiExpressionStatement) {
			PsiExpressionStatement expressionStatement = (PsiExpressionStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, expressionStatement, parent.getDepth()+1, getCodeElementType(expressionStatement.getExpression()));
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiSwitchStatement) {
			PsiSwitchStatement switchStatement = (PsiSwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, switchStatement, parent.getDepth()+1, CodeElementType.SWITCH_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, switchStatement.getExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			PsiStatement[] switchStatements = switchStatement.getBody().getStatements();
			for(PsiStatement switchStatement2 : switchStatements)
				processStatement(cu, filePath, child, switchStatement2);
		}
		else if(statement instanceof PsiSwitchLabelStatement) {
			PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, switchCase, parent.getDepth()+1, CodeElementType.SWITCH_CASE);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiAssertStatement) {
			PsiAssertStatement assertStatement = (PsiAssertStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, assertStatement, parent.getDepth()+1, CodeElementType.ASSERT_STATEMENT);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiLabeledStatement) {
			PsiLabeledStatement labeledStatement = (PsiLabeledStatement)statement;
			String label = Formatter.format(labeledStatement.getLabelIdentifier());
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, labeledStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(label));
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			processStatement(cu, filePath, child, labeledStatement.getStatement());
		}
		else if(statement instanceof PsiReturnStatement) {
			PsiReturnStatement returnStatement = (PsiReturnStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiSynchronizedStatement) {
			PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, synchronizedStatement, parent.getDepth()+1, CodeElementType.SYNCHRONIZED_STATEMENT);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, synchronizedStatement.getLockExpression(), CodeElementType.SYNCHRONIZED_STATEMENT_EXPRESSION);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processBlock(cu, filePath, child, synchronizedStatement.getBody());
		}
		else if(statement instanceof PsiThrowStatement) {
			PsiThrowStatement throwStatement = (PsiThrowStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiTryStatement) {
			PsiTryStatement tryStatement = (PsiTryStatement)statement;
			TryStatementObject child = new TryStatementObject(cu, filePath, tryStatement, parent.getDepth()+1);
			parent.addStatement(child);
			PsiResourceList resources = tryStatement.getResourceList();
			if (resources != null) {
				for (PsiResourceListElement resource : resources) {
					AbstractExpression expression = new AbstractExpression(cu, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE);
					child.addExpression(expression);
				}
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			this.activeVariableDeclarations.addAll(variableDeclarations);
			PsiStatement[] tryStatements = tryStatement.getTryBlock().getStatements();
			for(PsiStatement blockStatement : tryStatements) {
				processStatement(cu, filePath, child, blockStatement);
			}
			this.activeVariableDeclarations.removeAll(variableDeclarations);
			PsiCatchSection[] catchClauses = tryStatement.getCatchSections();
			for(PsiCatchSection catchClause : catchClauses) {
				PsiCodeBlock catchClauseBody = catchClause.getCatchBlock();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, filePath, catchClauseBody, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				PsiParameter variableDeclaration = catchClause.getParameter();
				VariableDeclaration vd = new VariableDeclaration(cu, filePath, variableDeclaration, CodeElementType.SINGLE_VARIABLE_DECLARATION);
				catchClauseStatementObject.addVariableDeclaration(vd);
				AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getNameIdentifier(), CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME);
				catchClauseStatementObject.addExpression(variableDeclarationName);
				if(variableDeclaration.getInitializer() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER);
					catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				}
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				this.activeVariableDeclarations.addAll(catchClauseVariableDeclarations);
				PsiStatement[] blockStatements = catchClauseBody.getStatements();
				for(PsiStatement blockStatement : blockStatements) {
					processStatement(cu, filePath, catchClauseStatementObject, blockStatement);
				}
				this.activeVariableDeclarations.removeAll(catchClauseVariableDeclarations);
			}
			PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK);
				child.setFinallyClause(finallyClauseStatementObject);
				parent.addStatement(finallyClauseStatementObject);
				addStatementInVariableScopes(finallyClauseStatementObject);
				PsiStatement[] blockStatements = finallyBlock.getStatements();
				for(PsiStatement blockStatement : blockStatements) {
					processStatement(cu, filePath, finallyClauseStatementObject, blockStatement);
				}
			}
		}
		else if(statement instanceof PsiDeclarationStatement) {
			PsiDeclarationStatement variableDeclarationStatement = (PsiDeclarationStatement)statement;
			if (variableDeclarationStatement.getDeclaredElements()[0] instanceof PsiVariable) {
				StatementObject child = new StatementObject(cu, filePath, variableDeclarationStatement, parent.getDepth() + 1, CodeElementType.VARIABLE_DECLARATION_STATEMENT);
				parent.addStatement(child);
				addStatementInVariableScopes(child);
				this.activeVariableDeclarations.addAll(child.getVariableDeclarations());
			}
		}
		else if(statement instanceof PsiBreakStatement) {
			PsiBreakStatement breakStatement = (PsiBreakStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiContinueStatement) {
			PsiContinueStatement continueStatement = (PsiContinueStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof PsiEmptyStatement) {
			PsiEmptyStatement emptyStatement = (PsiEmptyStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, emptyStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}

	private void processBlock(PsiFile cu, String filePath, CompositeStatementObject parent, PsiCodeBlock codeBlock) {
		CompositeStatementObject child = new CompositeStatementObject(cu, filePath, codeBlock, parent.getDepth()+1, CodeElementType.BLOCK);
		parent.addStatement(child);
		addStatementInVariableScopes(child);
		PsiStatement[] blockStatements = codeBlock.getStatements();
		for(PsiStatement blockStatement : blockStatements) {
			processStatement(cu, filePath, child, blockStatement);
		}
	}

	private CodeElementType getCodeElementType(PsiExpression expression) {
		//TODO check if PsiConstructorCall is more appropriate
		if (expression instanceof PsiMethodCallExpression) {
			PsiMethodCallExpression callExpression = (PsiMethodCallExpression) expression;
			if (isThisConstructorInvocation(callExpression)) {
				return CodeElementType.CONSTRUCTOR_INVOCATION;
			} else if (isSuperConstructorInvocation(callExpression)) {
				return CodeElementType.SUPER_CONSTRUCTOR_INVOCATION;
			}
		}
		return CodeElementType.EXPRESSION_STATEMENT;
	}

	private void addStatementInVariableScopes(AbstractStatement statement) {
		for(VariableDeclaration variableDeclaration : activeVariableDeclarations) {
			variableDeclaration.addStatementInScope(statement);
		}
	}

	public Map<String, Set<String>> aliasedAttributes() {
		return compositeStatement.aliasedAttributes();
	}

	public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
		return compositeStatement.loopWithVariables(currentElementName, collectionName);
	}

	public int getBodyHashCode() {
		return bodyHashCode;
	}

	public List<String> stringRepresentation() {
		if(stringRepresentation == null) {
			stringRepresentation = compositeStatement.stringRepresentation();
		}
		return stringRepresentation;
	}
}
