package gr.uom.java.xmi.decomposition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtBreakExpression;
import org.jetbrains.kotlin.psi.KtCatchClause;
import org.jetbrains.kotlin.psi.KtContinueExpression;
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration;
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry;
import org.jetbrains.kotlin.psi.KtDoWhileExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFinallySection;
import org.jetbrains.kotlin.psi.KtForExpression;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLabeledExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.kotlin.psi.KtThrowExpression;
import org.jetbrains.kotlin.psi.KtTryExpression;
import org.jetbrains.kotlin.psi.KtVariableDeclaration;
import org.jetbrains.kotlin.psi.KtWhenCondition;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.psi.KtWhileExpression;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class KotlinOperationBody extends OperationBody {

	public KotlinOperationBody(KtFile ktFile, String sourceFolder, String filePath, KtBlockExpression methodBody, VariableDeclarationContainer container, List<UMLAttribute> attributes, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(ktFile, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		// TODO replace with stringify
		this.bodyHashCode = methodBody.getText().hashCode();
		this.activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();
		for(UMLAttribute attribute : attributes) {
			addInActiveVariableDeclarations(attribute.getVariableDeclaration());
		}
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
		List<KtExpression> statements = methodBody.getStatements();
		for(KtExpression statement : statements) {
			processStatement(ktFile, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	// Use this constructor when the body of a Kotlin function is when expression
	public KotlinOperationBody(KtFile ktFile, String sourceFolder, String filePath, KtWhenExpression whenExpression, VariableDeclarationContainer container, List<UMLAttribute> attributes, String fileContent) {
		this.comments = container.getComments();
		this.container = container;
		// TODO replace with stringify
		this.bodyHashCode = whenExpression.getText().hashCode();
		this.activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();
		for(UMLAttribute attribute : attributes) {
			addInActiveVariableDeclarations(attribute.getVariableDeclaration());
		}
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
		this.compositeStatement = processWhenStatement(ktFile, sourceFolder, filePath, null, fileContent, whenExpression);
		this.compositeStatement.setOwner(container);
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public KotlinOperationBody(KtFile ktFile, String sourceFolder, String filePath, KtIfExpression ifExpression, VariableDeclarationContainer container, List<UMLAttribute> attributes, String fileContent) {
		this.comments = container.getComments();
		this.container = container;
		// TODO replace with stringify
		this.bodyHashCode = ifExpression.getText().hashCode();
		this.activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();
		for(UMLAttribute attribute : attributes) {
			addInActiveVariableDeclarations(attribute.getVariableDeclaration());
		}
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
		this.compositeStatement = processIfStatement(ktFile, sourceFolder, filePath, null, fileContent, ifExpression);
		this.compositeStatement.setOwner(container);
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	public KotlinOperationBody(KtFile ktFile, String sourceFolder, String filePath, KtBlockExpression methodBody, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(ktFile, sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		// TODO replace with stringify
		this.bodyHashCode = methodBody.getText().hashCode();
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
		List<KtExpression> statements = methodBody.getStatements();
		for(KtExpression statement : statements) {
			processStatement(ktFile, sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		this.activeVariableDeclarations = null;
	}

	private void processStatement(KtFile ktFile, String sourceFolder, String filePath, CompositeStatementObject parent, KtExpression statement, String fileContent) {
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
		if(statement instanceof KtBlockExpression block) {
			List<KtExpression> blockStatements = block.getStatements();
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, block, parent.getDepth()+1, CodeElementType.BLOCK, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(KtExpression blockStatement : blockStatements) {
				processStatement(ktFile, sourceFolder, filePath, child, blockStatement, fileContent);
			}
		}
		else if(statement instanceof KtIfExpression ifStatement) {
			processIfStatement(ktFile, sourceFolder, filePath, parent, fileContent, ifStatement);
		}
		else if(statement instanceof KtWhileExpression whileStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(ktFile, sourceFolder, filePath, child, whileStatement.getBody(), fileContent);
		}
		else if(statement instanceof KtDoWhileExpression doStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT, fileContent);
			parent.addStatement(child);
			AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
			addStatementInVariableScopes(child);
			processStatement(ktFile, sourceFolder, filePath, child, doStatement.getBody(), fileContent);
		}
		else if(statement instanceof KtForExpression forStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, fileContent);
			parent.addStatement(child);
			KtParameter loopParameter = forStatement.getLoopParameter();
			KtDestructuringDeclaration destructuringDeclaration = forStatement.getDestructuringDeclaration();
			if (destructuringDeclaration != null) {
				AbstractExpression variableDeclarationName = new AbstractExpression(ktFile, sourceFolder, filePath, destructuringDeclaration, CodeElementType.ENHANCED_FOR_STATEMENT_DESTRUCTURING_DECLARATION, container, activeVariableDeclarations, fileContent);
				child.addExpression(variableDeclarationName);
				List<KtDestructuringDeclarationEntry> entries = destructuringDeclaration.getEntries();
				for (KtDestructuringDeclarationEntry entry : entries) {
					VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, entry, container, activeVariableDeclarations, fileContent, child.getLocationInfo());
					child.addVariableDeclaration(vd);
				}
			}
			else if (loopParameter != null) {
				VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, loopParameter, container, activeVariableDeclarations, fileContent, child.getLocationInfo());
				child.addVariableDeclaration(vd);
				AbstractExpression variableDeclarationName = new AbstractExpression(ktFile, sourceFolder, filePath, loopParameter, CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, fileContent);
				child.addExpression(variableDeclarationName);
				if(loopParameter.getDefaultValue() != null) {
					AbstractExpression variableDeclarationInitializer = new AbstractExpression(ktFile, sourceFolder, filePath, loopParameter.getDefaultValue(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, fileContent);
					child.addExpression(variableDeclarationInitializer);
				}
			}
			if (forStatement.getLoopRange() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, forStatement.getLoopRange(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			processStatement(ktFile, sourceFolder, filePath, child, forStatement.getBody(), fileContent);
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof KtTryExpression tryStatement) {
			processTryStatement(ktFile, sourceFolder, filePath, parent, fileContent, tryStatement);
		}
		else if(statement instanceof KtWhenExpression whenStatement) {
			processWhenStatement(ktFile, sourceFolder, filePath, parent, fileContent, whenStatement);
		}
		else if(statement instanceof KtLabeledExpression labeledStatement) {
			CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, labeledStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(labeledStatement.getLabelName()), fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			if(labeledStatement.getBaseExpression() != null) {
				processStatement(ktFile, sourceFolder, filePath, child, labeledStatement.getBaseExpression(), fileContent);
			}
		}
		else if(statement instanceof KtReturnExpression returnStatement) {
			KtExpression returnedExpression = returnStatement.getReturnedExpression();
			if(returnedExpression instanceof KtWhenExpression whenStatement) {
				processWhenStatement(ktFile, sourceFolder, filePath, parent, fileContent, whenStatement);
			}
			else if(returnedExpression instanceof KtIfExpression ifStatement) {
				processIfStatement(ktFile, sourceFolder, filePath, parent, fileContent, ifStatement);
			}
			else if(returnedExpression instanceof KtTryExpression tryStatement) {
				processTryStatement(ktFile, sourceFolder, filePath, parent, fileContent, tryStatement);
			}
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtThrowExpression throwStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, throwStatement, parent.getDepth()+1, CodeElementType.THROW_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtBreakExpression breakStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtContinueExpression continueStatement) {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof KtVariableDeclaration variableDeclaration) {
			KtExpression initializer = variableDeclaration.getInitializer();
			if(initializer instanceof KtWhenExpression whenStatement) {
				processWhenStatement(ktFile, sourceFolder, filePath, parent, fileContent, whenStatement);
			}
			else if(initializer instanceof KtIfExpression ifStatement) {
				processIfStatement(ktFile, sourceFolder, filePath, parent, fileContent, ifStatement);
			}
			else if(initializer instanceof KtTryExpression tryStatement) {
				processTryStatement(ktFile, sourceFolder, filePath, parent, fileContent, tryStatement);
			}
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, variableDeclaration, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			VariableDeclaration vd = variableDeclaration instanceof KtProperty ?
					new VariableDeclaration(ktFile, sourceFolder, filePath, (KtProperty)variableDeclaration, container, activeVariableDeclarations, fileContent, parent.getLocationInfo()) :
					new VariableDeclaration(ktFile, sourceFolder, filePath, (KtDestructuringDeclarationEntry)variableDeclaration, container, activeVariableDeclarations, fileContent, parent.getLocationInfo());
			child.getVariableDeclarations().add(vd);
			addStatementInVariableScopes(child);
			addAllInActiveVariableDeclarations(child.getVariableDeclarations());
		}
		else if(statement instanceof KtDestructuringDeclaration destructuringDeclaration) {
			KtExpression initializer = destructuringDeclaration.getInitializer();
			if(initializer instanceof KtWhenExpression whenStatement) {
				processWhenStatement(ktFile, sourceFolder, filePath, parent, fileContent, whenStatement);
			}
			else if(initializer instanceof KtIfExpression ifStatement) {
				processIfStatement(ktFile, sourceFolder, filePath, parent, fileContent, ifStatement);
			}
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, destructuringDeclaration, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			for(KtDestructuringDeclarationEntry entry : destructuringDeclaration.getEntries()) {
				VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, entry, container, activeVariableDeclarations, fileContent, parent.getLocationInfo());
				child.getVariableDeclarations().add(vd);
			}
			addStatementInVariableScopes(child);
			addAllInActiveVariableDeclarations(child.getVariableDeclarations());
		}
		else {
			StatementObject child = new StatementObject(ktFile, sourceFolder, filePath, statement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}

	private TryStatementObject processTryStatement(KtFile ktFile, String sourceFolder, String filePath,
			CompositeStatementObject parent, String fileContent, KtTryExpression tryStatement) {
		int depth = parent != null ? parent.getDepth()+1 : 0;
		TryStatementObject child = new TryStatementObject(ktFile, sourceFolder, filePath, tryStatement, depth, fileContent);
		if (parent != null)
			parent.addStatement(child);
		addStatementInVariableScopes(child);
		List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
		addAllInActiveVariableDeclarations(variableDeclarations);
		KtBlockExpression tryBlock = tryStatement.getTryBlock();
		for(KtExpression blockStatement : tryBlock.getStatements()) {
			processStatement(ktFile, sourceFolder, filePath, child, blockStatement, fileContent);
		}
		removeAllFromActiveVariableDeclarations(variableDeclarations);
		List<KtCatchClause> catchClauses = tryStatement.getCatchClauses();
		for(KtCatchClause catchClause : catchClauses) {
			KtExpression catchClauseBody = catchClause.getCatchBody();
			CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(ktFile, sourceFolder, filePath, catchClause, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE, fileContent);
			child.addCatchClause(catchClauseStatementObject);
			if (parent != null)
				parent.addStatement(catchClauseStatementObject);
			catchClauseStatementObject.setTryContainer(child);
			KtParameter variableDeclaration = catchClause.getCatchParameter();
			VariableDeclaration vd = new VariableDeclaration(ktFile, sourceFolder, filePath, variableDeclaration, container, activeVariableDeclarations, fileContent, catchClauseStatementObject.getLocationInfo());
			catchClauseStatementObject.addVariableDeclaration(vd);
			AbstractExpression variableDeclarationName = new AbstractExpression(ktFile, sourceFolder, filePath, variableDeclaration, CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container, activeVariableDeclarations, fileContent);
			catchClauseStatementObject.addExpression(variableDeclarationName);
			if(variableDeclaration.getDefaultValue() != null) {
				AbstractExpression variableDeclarationInitializer = new AbstractExpression(ktFile, sourceFolder, filePath, variableDeclaration.getDefaultValue(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER, container, activeVariableDeclarations, fileContent);
				catchClauseStatementObject.addExpression(variableDeclarationInitializer);
			}
			addStatementInVariableScopes(catchClauseStatementObject);
			List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
			addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
			if(catchClauseBody instanceof KtBlockExpression catchBlock) {
				for(KtExpression blockStatement : catchBlock.getStatements()) {
					processStatement(ktFile, sourceFolder, filePath, catchClauseStatementObject, blockStatement, fileContent);
				}
			}
			removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
		}
		KtFinallySection finallyBlock = tryStatement.getFinallyBlock();
		if(finallyBlock != null) {
			CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(ktFile, sourceFolder, filePath, finallyBlock, parent.getDepth()+1, CodeElementType.FINALLY_BLOCK, fileContent);
			child.setFinallyClause(finallyClauseStatementObject);
			if (parent != null)
				parent.addStatement(finallyClauseStatementObject);
			finallyClauseStatementObject.setTryContainer(child);
			addStatementInVariableScopes(finallyClauseStatementObject);
			KtBlockExpression finalExpression = finallyBlock.getFinalExpression();
			for(KtExpression blockStatement : finalExpression.getStatements()) {
				processStatement(ktFile, sourceFolder, filePath, finallyClauseStatementObject, blockStatement, fileContent);
			}
		}
		return child;
	}

	private CompositeStatementObject processIfStatement(KtFile ktFile, String sourceFolder, String filePath,
			CompositeStatementObject parent, String fileContent, KtIfExpression ifStatement) {
		int depth = parent != null ? parent.getDepth()+1 : 0;
		CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, ifStatement, depth, CodeElementType.IF_STATEMENT, fileContent);
		if (parent != null)
			parent.addStatement(child);
		AbstractExpression abstractExpression = new AbstractExpression(ktFile, sourceFolder, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
		child.addExpression(abstractExpression);
		addStatementInVariableScopes(child);
		processStatement(ktFile, sourceFolder, filePath, child, ifStatement.getThen(), fileContent);
		if(ifStatement.getElse() != null) {
			processStatement(ktFile, sourceFolder, filePath, child, ifStatement.getElse(), fileContent);
		}
		return child;
	}

	private CompositeStatementObject processWhenStatement(KtFile ktFile, String sourceFolder, String filePath,
			CompositeStatementObject parent, String fileContent, KtWhenExpression whenStatement) {
		int depth = parent != null ? parent.getDepth()+1 : 0;
		CompositeStatementObject child = new CompositeStatementObject(ktFile, sourceFolder, filePath, whenStatement, depth, CodeElementType.WHEN_STATEMENT, fileContent);
		if (parent != null)
			parent.addStatement(child);
		addStatementInVariableScopes(child);
		KtExpression subjectExpression = whenStatement.getSubjectExpression();
		if(subjectExpression != null) {
			AbstractExpression expr = new AbstractExpression(ktFile, sourceFolder, filePath, subjectExpression, CodeElementType.WHEN_SUBJECT_EXPRESSION, container, activeVariableDeclarations, fileContent);
			child.addExpression(expr);
		}
		for(KtWhenEntry whenEntry : whenStatement.getEntries()) {
			CompositeStatementObject grandChild = new CompositeStatementObject(ktFile, sourceFolder, filePath, whenEntry, child.getDepth()+1, CodeElementType.WHEN_ENTRY, fileContent);
			KtWhenCondition[] whenConditions = whenEntry.getConditions();
			for(KtWhenCondition whenCondition : whenConditions) {
				AbstractExpression expr = new AbstractExpression(ktFile, sourceFolder, filePath, whenCondition, CodeElementType.WHEN_ENTRY_CONDITION, container, activeVariableDeclarations, fileContent);
				grandChild.addExpression(expr);
			}
			KtExpression expression = whenEntry.getExpression();
			if(expression != null) {
				if(expression instanceof KtBlockExpression bodyExpression) {
					processStatement(ktFile, sourceFolder, filePath, grandChild, bodyExpression, fileContent);
				}
				else {
					AbstractExpression expr = new AbstractExpression(ktFile, sourceFolder, filePath, expression, CodeElementType.WHEN_ENTRY_EXPRESSION, container, activeVariableDeclarations, fileContent);
					grandChild.addExpression(expr);
				}
			}
			child.addStatement(grandChild);
			addStatementInVariableScopes(grandChild);
		}
		KtExpression elseExpression = whenStatement.getElseExpression();
		if(elseExpression != null) {
			if(elseExpression instanceof KtBlockExpression bodyExpression) {
				processStatement(ktFile, sourceFolder, filePath, child, bodyExpression, fileContent);
			}
			else {
				AbstractExpression expr = new AbstractExpression(ktFile, sourceFolder, filePath, elseExpression, CodeElementType.WHEN_ELSE_EXPRESSION, container, activeVariableDeclarations, fileContent);
				child.addExpression(expr);
			}
		}
		return child;
	}
}
