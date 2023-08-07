package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLClass;

public interface MoveBasedRefactoring extends Refactoring {
	public RenamePattern getRenamePattern();
	public UMLClass getOriginalClass();
	public UMLClass getMovedClass();
	public String getOriginalClassName();
	public String getMovedClassName();
}
