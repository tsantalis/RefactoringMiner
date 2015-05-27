package gr.uom.java.xmi.diff;

import java.io.Serializable;

public interface Refactoring extends Serializable {

	public RefactoringType getRefactoringType();
	
	public String getName();
	
}