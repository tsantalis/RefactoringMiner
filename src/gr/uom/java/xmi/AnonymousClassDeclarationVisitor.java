package gr.uom.java.xmi;

import java.util.LinkedHashSet;
import java.util.Set;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;

public class AnonymousClassDeclarationVisitor extends PsiRecursiveElementWalkingVisitor {

	private Set<PsiAnonymousClass> anonymousClassDeclarations = new LinkedHashSet<>();

	public void visitElement(PsiElement element) {
		if (element instanceof PsiAnonymousClass) {
			PsiAnonymousClass anonymousClass = (PsiAnonymousClass) element;
			anonymousClassDeclarations.add(anonymousClass);
		}
		super.visitElement(element);
	}

	public Set<PsiAnonymousClass> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}
}
