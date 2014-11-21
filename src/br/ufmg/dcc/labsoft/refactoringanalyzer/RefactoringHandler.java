package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

public abstract class RefactoringHandler {

	public void handleDiff(Revision prevRevision, UMLModel prevModel, Revision curRevision, UMLModel curModel, List<Refactoring> refactorings) {}

}
