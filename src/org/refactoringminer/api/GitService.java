package org.refactoringminer.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcs.log.TimedVcsCommit;
import git4idea.repo.GitRepository;

/**
 * Simple service to make git related tasks easier.  
 *
 */
public interface GitService {

	/**
	 * Clone the git repository given by {@code cloneUrl} only if is does not exist yet in {@code folder}.
	 * 
	 * @param folder The folder to store the local repo.
	 * @param cloneUrl The repository URL.
	 * @return The repository object (JGit library).
	 * @throws Exception propagated from JGit library.
	 */
	GitRepository cloneIfNotExists(Project project, String folder, String cloneUrl/*, String branch*/) throws Exception;

	GitRepository openRepository(Project project, String folder) throws Exception;

	List<? extends TimedVcsCommit> createAllRevsWalk(GitRepository repository) throws Exception;

	List<? extends TimedVcsCommit> createAllRevsWalk(GitRepository repository, String branch) throws Exception;

	List<? extends TimedVcsCommit> createRevsWalkBetweenTags(GitRepository repository, String startTag, String endTag) throws Exception;

	List<? extends TimedVcsCommit> createRevsWalkBetweenCommits(GitRepository repository, String startCommitId, String endCommitId) throws Exception;

	void fileTreeDiff(GitRepository repository, Collection<Change> changes, Set<String> filesBefore, Set<String> filesCurrent, Map<String, String> renamedFilesHint) throws Exception;
}
