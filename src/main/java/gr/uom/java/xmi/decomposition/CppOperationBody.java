package gr.uom.java.xmi.decomposition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTIfStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTGotoStatement;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
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
		this.activeVariableDeclarations = null;
	}

	private void processStatement(String sourceFolder, String filePath, CompositeStatementObject parent, IASTStatement statement, String fileContent) {
		//https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.cdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fcdt%2Fcore%2Fdom%2Fast%2FIASTStatement.html
		if(statement instanceof IASTCompoundStatement compoundStatement) {
			// IASTCompoundStatement is the base block API; ICPPASTCompoundStatement is the C++ block form with implicit destructor-name ownership.
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
			StatementObject child = new StatementObject(sourceFolder, filePath, declarationStatement, parent.getDepth()+1, CodeElementType.VARIABLE_DECLARATION_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			// TODO: teach CppVisitor to extract C++ local variable declarations so this updates active scope.
			addAllInActiveVariableDeclarations(child.getVariableDeclarations());
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
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent, Collections.emptyList());
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
			// IASTForStatement models a generic for loop; ICPPASTForStatement adds C++ condition declarations and implicit destructor names.
			// composite
		}
		else if(statement instanceof IASTGotoStatement gotoStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, gotoStatement, parent.getDepth()+1, CodeElementType.GOTO_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
		else if(statement instanceof IASTIfStatement ifStatement) {
			// IASTIfStatement has a condition expression; ICPPASTIfStatement also supports C++ init-statements, condition declarations, constexpr, and scope.
			// composite
		}
		else if(statement instanceof IASTLabelStatement labelStatement) {
			// composite
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
			// composite
		}
		else if(statement instanceof IASTWhileStatement whileStatement) {
			// IASTWhileStatement uses a condition expression; ICPPASTWhileStatement also supports C++ condition declarations and scope.
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, whileStatement, parent.getDepth()+1, CodeElementType.WHILE_STATEMENT, fileContent);
			parent.addStatement(child);
			if(whileStatement.getCondition() != null) {
				AbstractExpression abstractExpression = new AbstractExpression(sourceFolder, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION, container, activeVariableDeclarations, fileContent, Collections.emptyList());
				child.addExpression(abstractExpression);
			}
			addStatementInVariableScopes(child);
			if(whileStatement.getBody() != null) {
				processStatement(sourceFolder, filePath, child, whileStatement.getBody(), fileContent);
			}
		}
		else if(statement instanceof ICPPASTCatchHandler catchHandler) {
			// composite
		}
		else if(statement instanceof ICPPASTCompoundStatement cppCompoundStatement) {
			// ICPPASTCompoundStatement is the C++ block form of IASTCompoundStatement and can own implicit destructor names.
			//C++ objects created in the block may be destroyed automatically at the end of the block, 
			//even though there is no explicit destructor call in the source:
			//{
			//    Widget w;
			//    doWork();
			//} // w.~Widget() happens implicitly here
			// composite
		}
		else if(statement instanceof ICPPASTForStatement cppForStatement) {
			// ICPPASTForStatement is the C++ for-loop form of IASTForStatement with condition declarations and implicit destructor names.
			//example of what you can do in C++
			//for (; Widget w = nextWidget(); ) {
			  //  use(w);
			//}
			//Widget w may have destructors that CDT tracks implicitly, even though no destructor call appears directly in the source.
			// composite
		}
		else if(statement instanceof ICPPASTIfStatement cppIfStatement) {
			// ICPPASTIfStatement is the C++ if form of IASTIfStatement with init-statements, condition declarations, constexpr, and scope.
			//if (int x = getValue(); x > 0) {
		    //use(x);
			//}
			//
			//int x = getValue(); part is the init-statement.
			//also supports a condition declaration:
			//if (Widget w = makeWidget()) {
			//  use(w);
			//}
			//
			//supports constexpr if:
			//if constexpr (std::is_integral_v<T>) {
			//    handleInteger();
			//} else {
			//    handleOther();
			//}
			//That is a compile-time branch in C++ templates.
			// composite
		}
		else if(statement instanceof ICPPASTRangeBasedForStatement rangeBasedForStatement) {
			// composite
		}
		else if(statement instanceof ICPPASTSwitchStatement cppSwitchStatement) {
			// ICPPASTSwitchStatement is the C++ switch form of IASTSwitchStatement with init-statements, controller declarations, and scope.
			//Covers C++ extras, like an init-statement:
			//switch (int code = readCode(); code) {
		    //case 0:
		    //    break;
		    //case 1:
		    //    break;
			//}
			//int code = readCode(); runs first. Then code is used as the switch controller. The variable code is scoped to the switch.
			//
			//also have a controller declaration:
			//switch (int code = readCode()) {
		    //case 0:
		    //    break;
		    //case 1:
		    //    break;
			//}
			//Here the controller itself declares code, instead of being only an existing expression.
			// composite
		}
		else if(statement instanceof ICPPASTTryBlockStatement tryBlockStatement) {
			// composite
		}
		else if(statement instanceof ICPPASTWhileStatement cppWhileStatement) {
			// ICPPASTWhileStatement is the C++ while form of IASTWhileStatement with condition declarations and scope.
			//also covers a C++ condition declaration:
			//while (std::shared_ptr<Node> node = nextNode()) {
			//    use(node);
			//}
			//Here the condition declares node, initializes it with nextNode(), and then tests whether node converts to true.
			//use(node) is scoped only wihtin the loop
			// composite
		}
		else if(statement instanceof IGNUASTGotoStatement gnuGotoStatement) {
			StatementObject child = new StatementObject(sourceFolder, filePath, gnuGotoStatement, parent.getDepth()+1, CodeElementType.GOTO_STATEMENT, container, activeVariableDeclarations, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
		}
	}
}
