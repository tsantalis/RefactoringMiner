package gr.uom.java.xmi.decomposition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTIfStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTStructuredBindingDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTGotoStatement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class CppOperationBody extends OperationBody {

	public CppOperationBody(String sourceFolder, String filePath, IASTCompoundStatement methodBody, VariableDeclarationContainer container, List<UMLAttribute> attributes, String fileContent) {
		this.compositeStatement = new CompositeStatementObject(sourceFolder, filePath, methodBody, 0, CodeElementType.BLOCK, fileContent);
		this.compositeStatement.setOwner(container);
		this.comments = container.getComments();
		this.container = container;
		this.bodyHashCode = methodBody.getRawSignature().hashCode();
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
		IASTStatement[] statements = methodBody.getStatements();
		for(IASTStatement statement : statements) {
			processStatement(sourceFolder, filePath, compositeStatement, statement, fileContent);
		}
		for(AbstractCall invocation : getAllOperationInvocations()) {
			if(invocation.isAssertion()) {
				containsAssertion = true;
				break;
			}
		}
		//this.activeVariableDeclarations = null;
	}

	private void processStatement(String sourceFolder, String filePath, CompositeStatementObject parent, IASTStatement statement, String fileContent) {
		//https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.cdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fcdt%2Fcore%2Fdom%2Fast%2FIASTStatement.html
		if(statement instanceof IASTCompoundStatement compoundStatement) {
			IASTStatement[] blockStatements = compoundStatement.getStatements();
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, compoundStatement, parent.getDepth()+1, CodeElementType.BLOCK, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(IASTStatement blockStatement : blockStatements) {
				processStatement(sourceFolder, filePath, child, blockStatement, fileContent);
			}
		}
		else if(statement instanceof IASTBreakStatement breakStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, breakStatement, parent.getDepth()+1, CodeElementType.BREAK_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTCaseStatement caseStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, caseStatement, parent.getDepth()+1, CodeElementType.SWITCH_CASE, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTContinueStatement continueStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, continueStatement, parent.getDepth()+1, CodeElementType.CONTINUE_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTDeclarationStatement declarationStatement) {
			if(declarationStatement.getDeclaration() instanceof ICPPASTUsingDeclaration cppUsingDeclaration) {
				IASTName name = cppUsingDeclaration.getName();
				String importName = name.toString().replace("::", ".");
				LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, cppUsingDeclaration, CodeElementType.IMPORT_DECLARATION, fileContent);
				UMLImport umlImport = new UMLImport(importName, false, false, locationInfo);
				if(container instanceof UMLOperation op) {
					op.addNestedImport(umlImport);
				}
			}
			else {
				StatementObject child = new StatementObject(sourceFolder, filePath, declarationStatement, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, fileContent);
				parent.addStatement(child);
				addStatementInVariableScopes(child);
				// TODO: handle non-simple C++ local declarations such as structured bindings.
				addAllInActiveVariableDeclarations(child.getVariableDeclarations());
			}
		}
		else if(statement instanceof IASTDefaultStatement defaultStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, defaultStatement, parent.getDepth()+1, CodeElementType.SWITCH_CASE, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTDoStatement doStatement) {
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, doStatement, parent.getDepth()+1, CodeElementType.DO_STATEMENT, fileContent);
			parent.addStatement(child);
			if(doStatement.getCondition() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			if(doStatement.getBody() != null) {
				processStatement(sourceFolder, filePath, child, doStatement.getBody(), fileContent);
			}
		}
		else if(statement instanceof IASTExpressionStatement expressionStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, expressionStatement, parent.getDepth()+1, CodeElementType.EXPRESSION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTForStatement forStatement) {
			// IASTForStatement models a generic for loop; ICPPASTForStatement adds C++ condition declarations.
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, forStatement, parent.getDepth()+1, CodeElementType.FOR_STATEMENT, fileContent);
			parent.addStatement(child);
			if(forStatement.getInitializerStatement() != null) {
				processStatement(sourceFolder, filePath, child, forStatement.getInitializerStatement(), fileContent);
			}
			if(forStatement instanceof ICPPASTForStatement cppForStatement) {
				if(cppForStatement.getConditionDeclaration() != null) {
					addDeclarationExpression(sourceFolder, filePath, child, cppForStatement.getConditionDeclaration(), CodeElementType.FOR_STATEMENT_CONDITION, fileContent);
				}
			}
			if(forStatement.getConditionExpression() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, forStatement.getConditionExpression(), CodeElementType.FOR_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			if(forStatement.getIterationExpression() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, forStatement.getIterationExpression(), CodeElementType.FOR_STATEMENT_UPDATER, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(forStatement.getBody() != null) {
				processStatement(sourceFolder, filePath, child, forStatement.getBody(), fileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof IASTGotoStatement gotoStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, gotoStatement, parent.getDepth()+1, CodeElementType.GOTO_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTIfStatement ifStatement) {
			// IASTIfStatement has a condition expression; ICPPASTIfStatement also supports C++ init-statements, condition declarations, constexpr, and scope.
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, ifStatement, parent.getDepth()+1, CodeElementType.IF_STATEMENT, fileContent);
			parent.addStatement(child);
			if(ifStatement instanceof ICPPASTIfStatement cppIfStatement) {
				if(cppIfStatement.getInitializerStatement() != null) {
					processStatement(sourceFolder, filePath, child, cppIfStatement.getInitializerStatement(), fileContent);
				}
				if(cppIfStatement.getConditionDeclaration() != null) {
					addDeclarationExpression(sourceFolder, filePath, child, cppIfStatement.getConditionDeclaration(), CodeElementType.IF_STATEMENT_CONDITION, fileContent);
				}
			}
			if(ifStatement.getConditionExpression() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, ifStatement.getConditionExpression(), CodeElementType.IF_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(ifStatement.getThenClause() != null) {
				processStatement(sourceFolder, filePath, child, ifStatement.getThenClause(), fileContent);
			}
			if(ifStatement.getElseClause() != null) {
				processStatement(sourceFolder, filePath, child, ifStatement.getElseClause(), fileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof IASTLabelStatement labelStatement) {
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, labelStatement, parent.getDepth()+1, CodeElementType.LABELED_STATEMENT.setName(labelStatement.getName().toString()), fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			if(labelStatement.getNestedStatement() != null) {
				processStatement(sourceFolder, filePath, child, labelStatement.getNestedStatement(), fileContent);
			}
		}
		else if(statement instanceof IASTNullStatement nullStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, nullStatement, parent.getDepth()+1, CodeElementType.EMPTY_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTProblemStatement problemStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, problemStatement, parent.getDepth()+1, CodeElementType.PROBLEM_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTReturnStatement returnStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, returnStatement, parent.getDepth()+1, CodeElementType.RETURN_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTSwitchStatement switchStatement) {
			// IASTSwitchStatement uses a controller expression; ICPPASTSwitchStatement also supports C++ init-statements, controller declarations, and scope.
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, switchStatement, parent.getDepth()+1, CodeElementType.SWITCH_STATEMENT, fileContent);
			parent.addStatement(child);
			if(switchStatement instanceof ICPPASTSwitchStatement cppSwitchStatement) {
				if(cppSwitchStatement.getInitializerStatement() != null) {
					processStatement(sourceFolder, filePath, child, cppSwitchStatement.getInitializerStatement(), fileContent);
				}
				if(cppSwitchStatement.getControllerDeclaration() != null) {
					addDeclarationExpression(sourceFolder, filePath, child, cppSwitchStatement.getControllerDeclaration(), CodeElementType.SWITCH_STATEMENT_CONDITION, fileContent);
				}
			}
			if(switchStatement.getControllerExpression() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, switchStatement.getControllerExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(switchStatement.getBody() != null) {
				processStatement(sourceFolder, filePath, child, switchStatement.getBody(), fileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof IASTWhileStatement whileStatement) {
			// IASTWhileStatement uses a condition expression; ICPPASTWhileStatement also supports C++ condition declarations and scope.
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, fileContent);
			parent.addStatement(child);
			if(whileStatement instanceof ICPPASTWhileStatement cppWhileStatement) {
				if(cppWhileStatement.getConditionDeclaration() != null) {
					addDeclarationExpression(sourceFolder, filePath, child, cppWhileStatement.getConditionDeclaration(), CodeElementType.WHILE_STATEMENT_CONDITION, fileContent);
				}
			}
			if(whileStatement.getCondition() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(whileStatement.getBody() != null) {
				processStatement(sourceFolder, filePath, child, whileStatement.getBody(), fileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof ICPPASTRangeBasedForStatement rangeBasedForStatement) {
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, rangeBasedForStatement, parent.getDepth()+1, CodeElementType.ENHANCED_FOR_STATEMENT, fileContent);
			parent.addStatement(child);
			addRangeBasedForDeclaration(sourceFolder, filePath, child, rangeBasedForStatement.getDeclaration(), fileContent);
			if(rangeBasedForStatement.getInitializerClause() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, rangeBasedForStatement.getInitializerClause(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION, container, activeVariableDeclarations, fileContent);
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			if(rangeBasedForStatement.getBody() != null) {
				processStatement(sourceFolder, filePath, child, rangeBasedForStatement.getBody(), fileContent);
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
		}
		else if(statement instanceof ICPPASTTryBlockStatement tryBlockStatement) {
			TryStatementObject child = new TryStatementObject(sourceFolder, filePath, tryBlockStatement, parent.getDepth()+1, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
			addAllInActiveVariableDeclarations(variableDeclarations);
			IASTStatement tryBody = tryBlockStatement.getTryBody();
			if(tryBody instanceof IASTCompoundStatement compoundStatement) {
				for(IASTStatement blockStatement : compoundStatement.getStatements()) {
					processStatement(sourceFolder, filePath, child, blockStatement, fileContent);
				}
			}
			removeAllFromActiveVariableDeclarations(variableDeclarations);
			for(ICPPASTCatchHandler catchHandler : tryBlockStatement.getCatchHandlers()) {
				CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(sourceFolder, filePath, catchHandler, parent.getDepth()+1, CodeElementType.CATCH_CLAUSE, fileContent);
				child.addCatchClause(catchClauseStatementObject);
				parent.addStatement(catchClauseStatementObject);
				catchClauseStatementObject.setTryContainer(child);
				addCatchHandlerDeclaration(sourceFolder, filePath, catchClauseStatementObject, catchHandler.getDeclaration(), fileContent);
				addStatementInVariableScopes(catchClauseStatementObject);
				List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
				addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
				IASTStatement catchBody = catchHandler.getCatchBody();
				if(catchBody instanceof IASTCompoundStatement compoundStatement) {
					for(IASTStatement blockStatement : compoundStatement.getStatements()) {
						processStatement(sourceFolder, filePath, catchClauseStatementObject, blockStatement, fileContent);
					}
				}
				removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
			}
		}
		else if(statement instanceof IGNUASTGotoStatement gnuGotoStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, gnuGotoStatement, parent.getDepth()+1, CodeElementType.GOTO_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}
	// Normalizes the C++ range-for declaration into the same enhanced-for shape used by other languages:
	// loop variable declarations plus parameter-name expressions on the loop composite.
	private void addRangeBasedForDeclaration(String sourceFolder, String filePath, CompositeStatementObject child, IASTDeclaration declaration, String fileContent) {
		if(declaration instanceof ICPPASTStructuredBindingDeclaration structuredBindingDeclaration) {
			AbstractExpression destructuringDeclaration = new AbstractExpression(sourceFolder, filePath, declaration, CodeElementType.ENHANCED_FOR_STATEMENT_DESTRUCTURING_DECLARATION, container, activeVariableDeclarations, fileContent);
			child.addExpression(destructuringDeclaration);
			for(IASTDeclarator declarator : structuredBindingDeclaration.getDeclarators()) {
				VariableDeclaration variableDeclaration = new VariableDeclaration(
						sourceFolder,
						filePath,
						declarator,
						structuredBindingDeclaration.getDeclSpecifier(),
						container,
						activeVariableDeclarations,
						fileContent
						);
				child.addVariableDeclaration(variableDeclaration);
			}
		}
		else if(declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, declarator, simpleDeclaration.getDeclSpecifier(), container, activeVariableDeclarations, fileContent);
				child.addVariableDeclaration(variableDeclaration);
				IASTName name = declarator.getName();
				if(name != null) {
					AbstractExpression variableDeclarationName = new AbstractExpression(sourceFolder, filePath, name, CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME, container, activeVariableDeclarations, fileContent);
					child.addExpression(variableDeclarationName);
				}
			}
		}
	}

	// Take the C++ catch header declaration and attach it to the catch clause node.
	private void addCatchHandlerDeclaration(String sourceFolder, String filePath, CompositeStatementObject catchClause, IASTDeclaration declaration, String fileContent) {
		if(declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
			for(IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
				IASTName name = declarator.getName();
				if(name != null && !name.toString().isEmpty()) {
					VariableDeclaration variableDeclaration = new VariableDeclaration(sourceFolder, filePath, declarator, simpleDeclaration.getDeclSpecifier(), container, activeVariableDeclarations, fileContent);
					catchClause.addVariableDeclaration(variableDeclaration);
					AbstractExpression variableDeclarationName = new AbstractExpression(sourceFolder, filePath, name, CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME, container, activeVariableDeclarations, fileContent);
					catchClause.addExpression(variableDeclarationName);
				}
			}
		}
	}
	// Helper for turning a CDT declaration node into an AbstractExpression.
	private void addDeclarationExpression(String sourceFolder, String filePath, CompositeStatementObject child, IASTDeclaration declaration, CodeElementType codeElementType, String fileContent) {
		if(declaration != null) {
			AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, declaration, codeElementType, container, activeVariableDeclarations, fileContent);
			child.addExpression(abstractExpression);
		}
	}

	public void addCatchHandlerToFunction(String sourceFolder, String filePath, ICPPASTCatchHandler catchHandler, String fileContent) {
		CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(sourceFolder, filePath, catchHandler, this.compositeStatement.getDepth()+1, CodeElementType.CATCH_CLAUSE, fileContent);
		//child.addCatchClause(catchClauseStatementObject);
		this.compositeStatement.addStatement(catchClauseStatementObject);
		catchClauseStatementObject.setTryContainer(null);
		addCatchHandlerDeclaration(sourceFolder, filePath, catchClauseStatementObject, catchHandler.getDeclaration(), fileContent);
		this.activeVariableDeclarations = new HashMap<String, Set<VariableDeclaration>>();
		addStatementInVariableScopes(catchClauseStatementObject);
		List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
		addAllInActiveVariableDeclarations(catchClauseVariableDeclarations);
		IASTStatement catchBody = catchHandler.getCatchBody();
		if(catchBody instanceof IASTCompoundStatement compoundStatement) {
			for(IASTStatement blockStatement : compoundStatement.getStatements()) {
				processStatement(sourceFolder, filePath, catchClauseStatementObject, blockStatement, fileContent);
			}
		}
		removeAllFromActiveVariableDeclarations(catchClauseVariableDeclarations);
	}
}
