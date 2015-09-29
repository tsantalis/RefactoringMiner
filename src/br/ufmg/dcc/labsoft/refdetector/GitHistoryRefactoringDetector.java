package br.ufmg.dcc.labsoft.refdetector;

import org.eclipse.jgit.lib.Repository;

/**
 * Detect refactorings in the git history.
 * 
 */
public interface GitHistoryRefactoringDetector {

	/**
	 * Iterate over each commit of a git repository and detect all refactorings performed in the
	 * entire repository history. Merge commits are ignored to avoid detecting the same refactoring 
	 * multiple times.
	 * 
	 * @param repository A git repository (from JGit library).
	 * @param branch A branch to start the log lookup. If null, commits from all branches are analyzed.
	 * @param handler A handler object that is responsible to process the detected refactorings and
	 *                control when to skip a commit. 
	 * @throws Exception propagated from JGit library.
	 */
	void detectAll(Repository repository, String branch, RefactoringHandler handler) throws Exception;

	/**
	 * Fetch new commits from the remote repo and detect all refactorings performed in these
	 * commits.
	 * 
	 * @param repository A git repository (from JGit library).
	 * @param handler A handler object that is responsible to process the detected refactorings and
	 *                control when to skip a commit. 
	 * @throws Exception propagated from JGit library.
	 */
	void fetchAndDetectNew(Repository repository, RefactoringHandler handler) throws Exception;

	/**
	 * Detect refactorings performed in the specified commit. 
	 * 
	 * @param repository A git repository (from JGit library).
	 * @param commitId The SHA key that identifies the commit.
	 * @param handler A handler object that is responsible to process the detected refactorings. 
	 */
	void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler);

}
