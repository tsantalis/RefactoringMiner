package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

public abstract class RefactoringHandler {

	public boolean skipRevision(RevCommit curRevision) {
		return false;
	}

	public void handleDiff(RevCommit prevRevision, UMLModel prevModel, RevCommit curRevision, UMLModel curModel, List<Refactoring> refactorings) {}

	public void onFinish(int refactoringsCount, int commitsCount, int mergeCommitsCount, int errorCommitsCount) {}
}
