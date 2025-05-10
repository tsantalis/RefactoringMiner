package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.VariableDeclarationContainer;

public interface MethodLevelRefactoring extends Refactoring {
	public VariableDeclarationContainer getOperationBefore();
	public VariableDeclarationContainer getOperationAfter();
}
