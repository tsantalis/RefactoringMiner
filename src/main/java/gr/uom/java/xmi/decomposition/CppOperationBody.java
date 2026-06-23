package gr.uom.java.xmi.decomposition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTIfStatement;
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
			IASTStatement[] blockStatements = compoundStatement.getStatements();
			CompositeStatementObject child = new CompositeStatementObject(sourceFolder, filePath, compoundStatement, parent.getDepth()+1, CodeElementType.BLOCK, fileContent);
			parent.addStatement(child);
			addStatementInVariableScopes(child);
			for(IASTStatement blockStatement : blockStatements) {
				processStatement(sourceFolder, filePath, child, blockStatement, fileContent);
			}
		}
		else if(statement instanceof IASTIfStatement ifStatement) {
			
		}
		else if(statement instanceof ICPPASTIfStatement cppIfStatement) {
			
		}
	}
}
