package br.ufmg.dcc.labsoft.refactoringanalyzer;

public interface RefactoringDetector {

	void detectAll(String projectFolder, RefactoringHandler handler);

}
