package org.refactoringminer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.TimedVcsCommit;
import com.intellij.vcsUtil.VcsFileUtil;
import git4idea.GitRevisionNumber;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryImpl;

import org.refactoringminer.api.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitServiceImpl implements GitService {

	private static final String REMOTE_REFS_PREFIX = "refs/remotes/origin/";
	Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

	@Override
	public GitRepository cloneIfNotExists(Project project, String projectPath, String cloneUrl/*, String branch*/) throws Exception {
		java.io.File folder = new File(projectPath);
		if (folder.exists()) {
			VirtualFile rootDir = LocalFileSystem.getInstance().findFileByIoFile(folder);
			return GitRepositoryImpl.getInstance(rootDir, project, false);
			//logger.info("Project {} is already cloned, current branch is {}", cloneUrl, repository.getBranch());
		} else {
			logger.info("Cloning {} ...", cloneUrl);
//			Git git = Git.cloneRepository()
//					.setDirectory(folder)
//					.setURI(cloneUrl)
//					.setCloneAllBranches(true)
//					.call();
//			repository = git.getRepository();
			//logger.info("Done cloning {}, current branch is {}", cloneUrl, repository.getBranch());
		}
		return null;
	}

	@Override
	public GitRepository openRepository(Project project, String repositoryPath) throws Exception {
		File folder = new File(repositoryPath);
		if (folder.exists()) {
			VirtualFile rootDir = LocalFileSystem.getInstance().findFileByIoFile(folder);
			return GitRepositoryImpl.getInstance(rootDir, project, false);
		} else {
			throw new FileNotFoundException(repositoryPath);
		}
	}

	public List<? extends TimedVcsCommit> createAllRevsWalk(GitRepository repository) throws Exception {
		return this.createAllRevsWalk(repository, null);
	}

	public List<? extends TimedVcsCommit> createAllRevsWalk(GitRepository repository, String branch) throws Exception {
		List<? extends TimedVcsCommit> commits = branch != null ?
				GitHistoryUtils.collectTimedCommits(repository.getProject(), repository.getRoot(), REMOTE_REFS_PREFIX + branch) :
				GitHistoryUtils.collectTimedCommits(repository.getProject(), repository.getRoot());
		List<? extends TimedVcsCommit> filteredMergeCommits = commits.stream().filter(c -> c.getParents().size() == 1).collect(Collectors.toList());
		return filteredMergeCommits;
	}
	
	@Override
	public List<? extends TimedVcsCommit> createRevsWalkBetweenTags(GitRepository repository, String startTag, String endTag)
			throws Exception {
		GitRevisionNumber startCommitId = GitRevisionNumber.resolve(repository.getProject(), repository.getRoot(), startTag);
		GitRevisionNumber endCommitId = GitRevisionNumber.resolve(repository.getProject(), repository.getRoot(), endTag);
		String parameter = startCommitId + ".." + endCommitId;
		List<? extends TimedVcsCommit> commits = GitHistoryUtils.collectTimedCommits(repository.getProject(), repository.getRoot(), parameter);
		List<? extends TimedVcsCommit> filteredMergeCommits = commits.stream().filter(c -> c.getParents().size() == 1).collect(Collectors.toList());
		Collections.reverse(filteredMergeCommits);
		return filteredMergeCommits;
	}

	@Override
	public List<? extends TimedVcsCommit> createRevsWalkBetweenCommits(GitRepository repository, String startCommitId, String endCommitId)
			throws Exception {
		String parameter = startCommitId + ".." + endCommitId;
		List<? extends TimedVcsCommit> commits = GitHistoryUtils.collectTimedCommits(repository.getProject(), repository.getRoot(), parameter);
		List<? extends TimedVcsCommit> filteredMergeCommits = commits.stream().filter(c -> c.getParents().size() == 1).collect(Collectors.toList());
		Collections.reverse(filteredMergeCommits);
		return filteredMergeCommits;
	}

	public boolean isCommitAnalyzed(String sha1) {
		return false;
	}

	public void fileTreeDiff(GitRepository repository, Collection<Change> changes, Set<String> javaFilesBefore, Set<String> javaFilesCurrent, Map<String, String> renamedFilesHint) throws Exception {
		for(Change change : changes) {
			Change.Type changeType = change.getType();
			if (changeType != Change.Type.NEW) {
				String oldRelativePath = VcsFileUtil.relativePath(repository.getRoot(), change.getBeforeRevision().getFile());
				if (isJavafile(oldRelativePath)) {
					javaFilesBefore.add(oldRelativePath);
				}
			}
			if (changeType != Change.Type.DELETED) {
				String newRelativePath = VcsFileUtil.relativePath(repository.getRoot(), change.getAfterRevision().getFile());
				if (isJavafile(newRelativePath)) {
					javaFilesCurrent.add(newRelativePath);
				}
			}
			if (change.isRenamed() || change.isMoved()) {
				String oldRelativePath = VcsFileUtil.relativePath(repository.getRoot(), change.getBeforeRevision().getFile());
				String newRelativePath = VcsFileUtil.relativePath(repository.getRoot(), change.getAfterRevision().getFile());
				if (isJavafile(oldRelativePath) && isJavafile(newRelativePath)) {
					renamedFilesHint.put(oldRelativePath, newRelativePath);
				}
			}
		}
	}

	private boolean isJavafile(String path) {
		return path.endsWith(".java");
	}
}
