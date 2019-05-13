package org.refactoringminer.api;

import java.io.Serializable;
import java.util.List;

public interface Refactoring extends Serializable, CodeRangeProvider {

	public RefactoringType getRefactoringType();
	
	public String getName();

	public String toString();
	
	public List<String> getInvolvedClassesBeforeRefactoring();
	
	public List<String> getInvolvedClassesAfterRefactoring();
}