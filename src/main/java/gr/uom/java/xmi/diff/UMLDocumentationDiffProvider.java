package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.VariableDeclarationContainer;

public interface UMLDocumentationDiffProvider {
	public Optional<UMLJavadocDiff> getJavadocDiff();
	public UMLCommentListDiff getCommentListDiff();
	public VariableDeclarationContainer getContainer1();
	public VariableDeclarationContainer getContainer2();
}
