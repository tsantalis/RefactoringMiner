package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.diff.Refactoring;

public interface RefactoringHandler {

	void handleRefactoring(Revision revision, Refactoring refactoring);

}
