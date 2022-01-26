package org.refactoringminer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsFileUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryImpl;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.refactoringminer.api.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitServiceImpl implements GitService {

	private static final String REMOTE_REFS_PREFIX = "refs/remotes/origin/";
	Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

	DefaultCommitsFilter commitsFilter = new DefaultCommitsFilter();
	
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

	public RevWalk createAllRevsWalk(Repository repository) throws Exception {
		return this.createAllRevsWalk(repository, null);
	}

	public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
		List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>(); 
		for (Ref ref : repository.getRefDatabase().getRefs()) {
			String refName = ref.getName();
			if (refName.startsWith(REMOTE_REFS_PREFIX)) {
				if (branch == null || refName.endsWith("/" + branch)) {
					currentRemoteRefs.add(ref.getObjectId());
				}
			}
		}
		
		RevWalk walk = new RevWalk(repository);
		for (ObjectId newRef : currentRemoteRefs) {
			walk.markStart(walk.parseCommit(newRef));
		}
		walk.setRevFilter(commitsFilter);
		return walk;
	}
	
	@Override
	public Iterable<RevCommit> createRevsWalkBetweenTags(Repository repository, String startTag, String endTag)
			throws Exception {
		Ref refFrom = repository.findRef(startTag);
		Ref refTo = repository.findRef(endTag);
		try (Git git = new Git(repository)) {
			List<RevCommit> revCommits = StreamSupport.stream(git.log().addRange(getActualRefObjectId(refFrom), getActualRefObjectId(refTo)).call()
					.spliterator(), false)
			        .filter(r -> r.getParentCount() == 1)
			        .collect(Collectors.toList());
			Collections.reverse(revCommits);
			return revCommits;
		}
	}

	private ObjectId getActualRefObjectId(Ref ref) {
		if(ref.getPeeledObjectId() != null) {
			return ref.getPeeledObjectId();
		}
		return ref.getObjectId();
	}

	@Override
	public Iterable<RevCommit> createRevsWalkBetweenCommits(Repository repository, String startCommitId, String endCommitId)
			throws Exception {
		ObjectId from = repository.resolve(startCommitId);
		ObjectId to = repository.resolve(endCommitId);
		try (Git git = new Git(repository)) {
			List<RevCommit> revCommits = StreamSupport.stream(git.log().addRange(from, to).call()
					.spliterator(), false)
					.filter(r -> r.getParentCount() == 1)
			        .collect(Collectors.toList());
			Collections.reverse(revCommits);
			return revCommits;
		}
	}

	public boolean isCommitAnalyzed(String sha1) {
		return false;
	}

	private class DefaultCommitsFilter extends RevFilter {
		@Override
		public final boolean include(final RevWalk walker, final RevCommit c) {
			return c.getParentCount() == 1 && !isCommitAnalyzed(c.getName());
		}

		@Override
		public final RevFilter clone() {
			return this;
		}

		@Override
		public final boolean requiresCommitBody() {
			return false;
		}

		@Override
		public String toString() {
			return "RegularCommitsFilter";
		}
	}

	public void fileTreeDiff(GitRepository repository, Collection<Change> changes, List<String> javaFilesBefore, List<String> javaFilesCurrent, Map<String, String> renamedFilesHint) throws Exception {
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
