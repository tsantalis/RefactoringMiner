package br.ufmg.dcc.labsoft.refdetector;

import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Handler object that works in conjunction with {@link br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetector}.
 * 
 */
public abstract class RefactoringHandler {

	/**
	 * Indicate commits that should be ignored.
	 * You may override this method to implement custom logic.
	 *  
	 * @param commitId The SHA key that identifies the commit.
	 * @return True to skip the commit, false otherwise.
	 */
	public boolean skipCommit(String commitId) {
		return false;
	}

	/**
	 * This method is called after each commit is analyzed.
	 * You should override this method to do your custom logic with the list of detected refactorings.
	 * 
	 * @param commitData An object (from JGit library) that contains metadata information about the commit such as date, author, etc.
	 * @param refactorings List of refactorings detected in the commit.
	 */
	public void handle(RevCommit commitData, List<Refactoring> refactorings) {}

	/**
	 * This method is called after all commits are analyzed.
	 * You may override this method to implement custom logic.
	 * 
	 * @param refactoringsCount Total number of refactorings detected. 
	 * @param commitsCount Total number of commits analyzed.
	 * @param errorCommitsCount Total number of commits not analyzed due to errors.
	 */
	public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {}
}
