package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

public abstract class RefactoringHandler {

	public boolean skipRevision(String curRevision) {
		return false;
	}

	public void handleDiff(RevCommit commitData, List<Refactoring> refactorings) {}

	public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {}
}
