package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

public abstract class RefactoringHandler {

	public void handleRefactoring(Revision revision, UMLModel curModel, Refactoring refactoring) {}

	public void handleDiff(Revision prevRevision, UMLModel prevModel, Revision curRevision, UMLModel curModel, List<Refactoring> refactorings) {}

	public void handleCurrent(UMLModel model) {}
}
