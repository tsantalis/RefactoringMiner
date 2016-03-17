package org.refactoringminer.api;

import java.io.Serializable;

public interface Refactoring extends Serializable {

	public RefactoringType getRefactoringType();
	
	public String getName();

	public String toString();
	
}