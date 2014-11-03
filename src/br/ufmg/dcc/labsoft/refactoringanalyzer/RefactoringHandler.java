package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

public abstract class RefactoringHandler {

	public void handleRefactoring(Revision revision, Refactoring refactoring) {}

	public void handleDiff(Revision prevRevision, UMLModel prevModel, Revision curRevision, UMLModel curModel) {}

	public void handleCurrent(UMLModel model) {}
}
