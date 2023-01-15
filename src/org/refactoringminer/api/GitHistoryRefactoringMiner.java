package org.refactoringminer.api;

import git4idea.repo.GitRepository;
import org.refactoringminer.astDiff.actions.ASTDiff;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

/**
 * Detect refactorings in the git history.
 * 
 */
public interface GitHistoryRefactoringMiner {

	/**
	 * Iterate over each commit of a git repository and detect all refactorings performed in the
	 * entire repository history. Merge commits are ignored to avoid detecting the same refactoring 
	 * multiple times.
	 * 
	 * @param repository A git repository (from Git4Idea library).
	 * @param branch A branch to start the log lookup. If null, commits from all branches are analyzed.
	 * @param handler A handler object that is responsible to process the detected refactorings and
	 *                control when to skip a commit. 
	 * @throws Exception propagated from Git4Idea library.
	 */
	void detectAll(GitRepository repository, String branch, RefactoringHandler handler) throws Exception;

	/**
	 * Iterate over commits between two release tags of a git repository and detect the performed refactorings.
	 * 
	 * @param repository A git repository (from Git4Idea library).
	 * @param startTag An annotated tag to start the log lookup.
	 * @param endTag An annotated tag to end the log lookup.
	 * @param handler A handler object that is responsible to process the detected refactorings and
	 *                control when to skip a commit. 
	 * @throws Exception propagated from Git4Idea library.
	 */
	void detectBetweenTags(GitRepository repository, String startTag, String endTag, RefactoringHandler handler)
			throws Exception;
	
	/**
	 * Iterate over commits between two commits of a git repository and detect the performed refactorings.
	 * 
	 * @param repository A git repository (from Git4Idea library).
	 * @param startCommitId The SHA key that identifies the commit to start the log lookup.
	 * @param endCommitId The SHA key that identifies the commit to end the log lookup.
	 * @param handler A handler object that is responsible to process the detected refactorings and
	 *                control when to skip a commit. 
	 * @throws Exception propagated from Git4Idea library.
	 */
	void detectBetweenCommits(GitRepository repository, String startCommitId, String endCommitId, RefactoringHandler handler)
			throws Exception;

	/**
	 * Detect refactorings performed in the specified commit. 
	 * 
	 * @param repository A git repository (from Git4Idea library).
	 * @param commitId The SHA key that identifies the commit.
	 * @param handler A handler object that is responsible to process the detected refactorings. 
	 */
	void detectAtCommit(GitRepository repository, String commitId, RefactoringHandler handler);

	/**
	 * Detect refactorings performed in the specified commit.
	 *
	 * @param repository A git repository (from Git4Idea library).
	 * @param commitId The SHA key that identifies the commit.
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 * @param timeout A timeout, in seconds. When timeout is reached, the operation stops and returns no refactorings.
	 */
	void detectAtCommit(GitRepository repository, String commitId, RefactoringHandler handler, int timeout);

	/**
	 * Detect refactorings performed in the specified commit. All required information is extracted using the GitHub API.
	 *
	 * @param gitURL The git URL of the repository.
	 * @param commitId The SHA key that identifies the commit.
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 * @param timeout A timeout, in seconds. When timeout is reached, the operation stops and returns no refactorings.
	 */
	void detectAtCommit(String gitURL, String commitId, RefactoringHandler handler, int timeout);

	/**
	 * Detect refactorings performed in the specified pull request. All required information is extracted using the GitHub API.
	 *
	 * @param gitURL The git URL of the repository.
	 * @param pullRequest The pull request ID.
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 * @param timeout A timeout, in seconds, per commit in the pull request. When timeout is reached, the operation stops and returns no refactorings.
	 * @throws Exception propagated from org.kohsuke.github API
	 */
	void detectAtPullRequest(String gitURL, int pullRequest, RefactoringHandler handler, int timeout) throws Exception;

	/**
	 * Detect refactorings performed between two directories (or files) representing two versions of Java programs.
	 *
	 * @param previousPath The directory (or file) corresponding to the previous version.
	 * @param nextPath The directory (or file) corresponding to the next version.
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 */
	void detectAtDirectories(Path previousPath, Path nextPath, RefactoringHandler handler);

	/**
	 * Detect refactorings performed between two directories (or files) representing two versions of Java programs.
	 *
	 * @param previousFile The directory (or file) corresponding to the previous version.
	 * @param nextFile The directory (or file) corresponding to the next version.
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 */
	void detectAtDirectories(File previousFile, File nextFile, RefactoringHandler handler);

	/**
	 * @return An ID that represents the current configuration for the Refactoring Miner algorithm in use.
	 */
	String getConfigId();

	/**
	 * Generate the AST diff for the specified commit.
	 *
	 * @param repository A git repository (from Git4Idea library).
	 * @param commitId The SHA key that identifies the commit.
	 * @return A set of ASTDiff objects. Each ASTDiff corresponds to a pair of Java compilation units.
	 */
	Set<ASTDiff> diffAtCommit(GitRepository repository, String commitId);

	/**
	 * Generate the AST diff for the specified commit. All required information is extracted using the GitHub API.
	 *
	 * @param gitURL The git URL of the repository.
	 * @param commitId The SHA key that identifies the commit.
	 * @param timeout A timeout, in seconds. When timeout is reached, the operation stops and returns no AST diffs.
	 * @return A set of ASTDiff objects. Each ASTDiff corresponds to a pair of Java compilation units.
	 */
	Set<ASTDiff> diffAtCommit(String gitURL, String commitId, int timeout);

	/**
	 * Generate the AST diff between two directories (or files) representing two versions of Java programs.
	 *
	 * @param previousPath The directory (or file) corresponding to the previous version.
	 * @param nextPath The directory (or file) corresponding to the next version.
	 * @return A set of ASTDiff objects. Each ASTDiff corresponds to a pair of Java compilation units.
	 */
	Set<ASTDiff> diffAtDirectories(Path previousPath, Path nextPath);

	/**
	 * Generate the AST diff between two directories (or files) representing two versions of Java programs.
	 *
	 * @param previousFile The directory (or file) corresponding to the previous version.
	 * @param nextFile The directory (or file) corresponding to the next version.
	 * @return A set of ASTDiff objects. Each ASTDiff corresponds to a pair of Java compilation units.
	 */
	Set<ASTDiff> diffAtDirectories(File previousFile, File nextFile);
}
