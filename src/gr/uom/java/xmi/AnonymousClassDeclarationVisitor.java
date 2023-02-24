package gr.uom.java.xmi;

import java.util.LinkedHashSet;
import java.util.Set;

import com.intellij.psi.*;

public class AnonymousClassDeclarationVisitor extends PsiRecursiveElementWalkingVisitor {

	private Set<PsiAnonymousClass> anonymousClassDeclarations = new LinkedHashSet<>();
	private Set<PsiDeclarationStatement> typeDeclarationStatements = new LinkedHashSet<>();

	public void visitElement(PsiElement element) {
		if (element instanceof PsiAnonymousClass) {
			PsiAnonymousClass anonymousClass = (PsiAnonymousClass) element;
			anonymousClassDeclarations.add(anonymousClass);
		}
		if (element instanceof PsiDeclarationStatement) {
			PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) element;
			PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
			if (declaredElements.length > 0 && declaredElements[0] instanceof PsiClass) {
				typeDeclarationStatements.add(declarationStatement);
			}
		}
		super.visitElement(element);
	}

	public Set<PsiAnonymousClass> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public Set<PsiDeclarationStatement> getTypeDeclarationStatements() {
		return typeDeclarationStatements;
	}
}
