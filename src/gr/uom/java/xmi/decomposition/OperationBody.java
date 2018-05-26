package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class OperationBody {

	private CompositeStatementObject compositeStatement;

	public OperationBody(CompilationUnit cu, String filePath, Block methodBody) {
		this.compositeStatement = new CompositeStatementObject(cu, filePath, methodBody, 0, "{");
		List<Statement> statements = methodBody.statements();
		for(Statement statement : statements) {
			processStatement(cu, filePath, compositeStatement, statement);
		}
	}

	public int statementCount() {
		return compositeStatement.statementCount();
	}

	public CompositeStatementObject getCompositeStatement() {
		return compositeStatement;
	}

	public List<String> getAllAnonymousClassDeclarations() {
		return new ArrayList<String>(compositeStatement.getAllAnonymousClassDeclarations());
	}

	public Set<OperationInvocation> getAllOperationInvocations() {
		return new LinkedHashSet<OperationInvocation>(compositeStatement.getAllMethodInvocations().values());
	}

	private void processStatement(CompilationUnit cu, String filePath, CompositeStatementObject parent, Statement statement) {
		if(statement instanceof Block) {
			Block block = (Block)statement;
			List<Statement> blockStatements = block.statements();
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, block, parent.getDepth()+1, "{");
			parent.addStatement(child);
			for(Statement blockStatement : blockStatements) {
				processStatement(cu, filePath, child, blockStatement);
			}
		}
		else if(statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, ifStatement, parent.getDepth()+1, "if");
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, ifStatement.getExpression());
			child.addExpression(abstractExpression);
			processStatement(cu, filePath, child, ifStatement.getThenStatement());
			if(ifStatement.getElseStatement() != null) {
				processStatement(cu, filePath, child, ifStatement.getElseStatement());
			}
		}
		else if(statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, forStatement, parent.getDepth()+1, "for");
			parent.addStatement(child);
			List<Expression> initializers = forStatement.initializers();
			for(Expression initializer : initializers) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, initializer);
				child.addExpression(abstractExpression);
			}
			Expression expression = forStatement.getExpression();
			if(expression != null) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, expression);
				child.addExpression(abstractExpression);
			}
			List<Expression> updaters = forStatement.updaters();
			for(Expression updater : updaters) {
				AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, updater);
				child.addExpression(abstractExpression);
			}
			processStatement(cu, filePath, child, forStatement.getBody());
		}
		else if(statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, enhancedForStatement, parent.getDepth()+1, "for");
			parent.addStatement(child);
			SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();
			AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getName());
			child.addExpression(variableDeclarationName);
			if(variableDeclaration.getInitializer() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer());
				child.addExpression(variableDeclarationInitializer);
			}
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, enhancedForStatement.getExpression());
			child.addExpression(abstractExpression);
			processStatement(cu, filePath, child, enhancedForStatement.getBody());
		}
		else if(statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, whileStatement, parent.getDepth()+1, "while");
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, whileStatement.getExpression());
			child.addExpression(abstractExpression);
			processStatement(cu, filePath, child, whileStatement.getBody());
		}
		else if(statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, doStatement, parent.getDepth()+1, "do");
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, doStatement.getExpression());
			child.addExpression(abstractExpression);
			processStatement(cu, filePath, child, doStatement.getBody());
		}
		else if(statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, expressionStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, switchStatement, parent.getDepth()+1, "switch");
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, switchStatement.getExpression());
			child.addExpression(abstractExpression);
			List<Statement> switchStatements = switchStatement.statements();
			for(Statement switchStatement2 : switchStatements)
				processStatement(cu, filePath, child, switchStatement2);
		}
		else if(statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase)statement;
			StatementObject child = new StatementObject(cu, filePath, switchCase, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, assertStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, labeledStatement, parent.getDepth()+1, labeledStatement.getLabel().getIdentifier());
			parent.addStatement(child);
			processStatement(cu, filePath, child, labeledStatement.getBody());
		}
		else if(statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, returnStatement, parent.getDepth()+1);
			parent.addStatement(child);	
		}
		else if(statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, synchronizedStatement, parent.getDepth()+1, "synchronized");
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(cu, filePath, synchronizedStatement.getExpression());
			child.addExpression(abstractExpression);
			processStatement(cu, filePath, child, synchronizedStatement.getBody());
		}
		else if(statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, throwStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement)statement;
			CompositeStatementObject child = new CompositeStatementObject(cu, filePath, tryStatement, parent.getDepth()+1, "try");
			parent.addStatement(child);
			List<Statement> tryStatements = tryStatement.getBody().statements();
			for(Statement blockStatement : tryStatements) {
				processStatement(cu, filePath, child, blockStatement);
			}
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for(CatchClause catchClause : catchClauses) {
				Block catchClauseBody = catchClause.getBody();
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(cu, filePath, catchClauseBody, parent.getDepth()+1, "catch");
				parent.addStatement(catchClauseStatementObject);
				SingleVariableDeclaration variableDeclaration = catchClause.getException();
				AbstractExpression variableDeclarationName = new AbstractExpression(cu, filePath, variableDeclaration.getName());
				catchClauseStatementObject.addExpression(variableDeclarationName);
				if(variableDeclaration.getInitializer() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(cu, filePath, variableDeclaration.getInitializer());
					catchClauseStatementObject.addExpression(variableDeclarationInitializer);
				}
				List<Statement> blockStatements = catchClauseBody.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(cu, filePath, catchClauseStatementObject, blockStatement);
				}
			}
			Block finallyBlock = tryStatement.getFinally();
			if(finallyBlock != null) {
				CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(cu, filePath, finallyBlock, parent.getDepth()+1, "finally");
				parent.addStatement(finallyClauseStatementObject);
				List<Statement> blockStatements = finallyBlock.statements();
				for(Statement blockStatement : blockStatements) {
					processStatement(cu, filePath, finallyClauseStatementObject, blockStatement);
				}
			}
		}
		else if(statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, variableDeclarationStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, filePath, constructorInvocation, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation)statement;
			StatementObject child = new StatementObject(cu, filePath, superConstructorInvocation, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, breakStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, continueStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
		else if(statement instanceof EmptyStatement) {
			EmptyStatement emptyStatement = (EmptyStatement)statement;
			StatementObject child = new StatementObject(cu, filePath, emptyStatement, parent.getDepth()+1);
			parent.addStatement(child);
		}
	}
}
