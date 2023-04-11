package gr.uom.java.xmi;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

public class AnonymousClassDeclarationVisitor extends ASTVisitor {

	private Set<AnonymousClassDeclaration> anonymousClassDeclarations = new LinkedHashSet<>();
	private Set<TypeDeclarationStatement> typeDeclarationStatements = new LinkedHashSet<>();
	
	public boolean visit(TypeDeclarationStatement node) {
		typeDeclarationStatements.add(node);
		return super.visit(node);
	}
	
	public boolean visit(AnonymousClassDeclaration node) {
		anonymousClassDeclarations.add(node);
		return super.visit(node);
	}

	public Set<TypeDeclarationStatement> getTypeDeclarationStatements() {
		return typeDeclarationStatements;
	}

	public Set<AnonymousClassDeclaration> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}
}
