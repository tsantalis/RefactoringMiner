package org.refactoringminer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.refactoringminer.api.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitServiceImpl implements GitService {

	private static final String REMOTE_REFS_PREFIX = "refs/remotes/origin/";
	Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

	DefaultCommitsFilter commitsFilter = new DefaultCommitsFilter();
	
	@Override
	public Repository cloneIfNotExists(String projectPath, String cloneUrl/*, String branch*/) throws Exception {
		File folder = new File(projectPath);
		Repository repository;
		if (folder.exists()) {
			RepositoryBuilder builder = new RepositoryBuilder();
			repository = builder
					.setGitDir(new File(folder, ".git"))
					.readEnvironment()
					.findGitDir()
					.build();
			
			//logger.info("Project {} is already cloned, current branch is {}", cloneUrl, repository.getBranch());
			
		} else {
			logger.info("Cloning {} ...", cloneUrl);
			Git git = Git.cloneRepository()
					.setDirectory(folder)
					.setURI(cloneUrl)
					.setCloneAllBranches(true)
					.call();
			repository = git.getRepository();
			//logger.info("Done cloning {}, current branch is {}", cloneUrl, repository.getBranch());
		}

//		if (branch != null && !repository.getBranch().equals(branch)) {
//			Git git = new Git(repository);
//			
//			String localBranch = "refs/heads/" + branch;
//			List<Ref> refs = git.branchList().call();
//			boolean branchExists = false;
//			for (Ref ref : refs) {
//				if (ref.getName().equals(localBranch)) {
//					branchExists = true;
//				}
//			}
//			
//			if (branchExists) {
//				git.checkout()
//					.setName(branch)
//					.call();
//			} else {
//				git.checkout()
//					.setCreateBranch(true)
//					.setName(branch)
//					.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
//					.setStartPoint("origin/" + branch)
//					.call();
//			}
//			
//			logger.info("Project {} switched to {}", cloneUrl, repository.getBranch());
//		}
		return repository;
	}

	@Override
	public Repository openRepository(String repositoryPath) throws Exception {
	    File folder = new File(repositoryPath);
	    Repository repository;
	    if (folder.exists()) {
	        RepositoryBuilder builder = new RepositoryBuilder();
	        repository = builder
	            .setGitDir(new File(folder, ".git"))
	            .readEnvironment()
	            .findGitDir()
	            .build();
	    } else {
	        throw new FileNotFoundException(repositoryPath);
	    }
	    return repository;
	}

	public void checkout(Repository repository, String commitId) throws Exception {
	    logger.info("Checking out {} {} ...", repository.getDirectory().getParent().toString(), commitId);
	    try (Git git = new Git(repository)) {
	        CheckoutCommand checkout = git.checkout().setName(commitId);
	        checkout.call();
	    }
//		File workingDir = repository.getDirectory().getParentFile();
//		ExternalProcess.execute(workingDir, "git", "checkout", commitId);
	}

	public void checkout2(Repository repository, String commitId) throws Exception {
	    logger.info("Checking out {} {} ...", repository.getDirectory().getParent().toString(), commitId);
		File workingDir = repository.getDirectory().getParentFile();
		String output = ExternalProcess.execute(workingDir, "git", "checkout", commitId);
		if (output.startsWith("fatal")) {
		    throw new RuntimeException("git error " + output);
		}
	}

	@Override
	public int countCommits(Repository repository, String branch) throws Exception {
		RevWalk walk = new RevWalk(repository);
		try {
			Ref ref = repository.findRef(REMOTE_REFS_PREFIX + branch);
			ObjectId objectId = ref.getObjectId();
			RevCommit start = walk.parseCommit(objectId);
			walk.setRevFilter(RevFilter.NO_MERGES);
			return RevWalkUtils.count(walk, start, null);
		} finally {
			walk.dispose();
		}
	}

	private List<TrackingRefUpdate> fetch(Repository repository) throws Exception {
        logger.info("Fetching changes of repository {}", repository.getDirectory().toString());
        try (Git git = new Git(repository)) {
    		FetchResult result = git.fetch().call();
    		
    		Collection<TrackingRefUpdate> updates = result.getTrackingRefUpdates();
    		List<TrackingRefUpdate> remoteRefsChanges = new ArrayList<TrackingRefUpdate>();
    		for (TrackingRefUpdate update : updates) {
    			String refName = update.getLocalName();
    			if (refName.startsWith(REMOTE_REFS_PREFIX)) {
    				ObjectId newObjectId = update.getNewObjectId();
    				logger.info("{} is now at {}", refName, newObjectId.getName());
    				remoteRefsChanges.add(update);
    			}
    		}
    		if (updates.isEmpty()) {
    			logger.info("Nothing changed");
    		}
    		return remoteRefsChanges;
        }
	}

	public RevWalk fetchAndCreateNewRevsWalk(Repository repository) throws Exception {
		return this.fetchAndCreateNewRevsWalk(repository, null);
	}

	public RevWalk fetchAndCreateNewRevsWalk(Repository repository, String branch) throws Exception {
		List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>(); 
		for (Ref ref : repository.getAllRefs().values()) {
			String refName = ref.getName();
			if (refName.startsWith(REMOTE_REFS_PREFIX)) {
				currentRemoteRefs.add(ref.getObjectId());
			}
		}
		
		List<TrackingRefUpdate> newRemoteRefs = this.fetch(repository);
		
		RevWalk walk = new RevWalk(repository);
		for (TrackingRefUpdate newRef : newRemoteRefs) {
			if (branch == null || newRef.getLocalName().endsWith("/" + branch)) {
				walk.markStart(walk.parseCommit(newRef.getNewObjectId()));
			}
		}
		for (ObjectId oldRef : currentRemoteRefs) {
			walk.markUninteresting(walk.parseCommit(oldRef));
		}
		walk.setRevFilter(commitsFilter);
		return walk;
	}

	public RevWalk createAllRevsWalk(Repository repository) throws Exception {
		return this.createAllRevsWalk(repository, null);
	}

	public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
		List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>(); 
		for (Ref ref : repository.getAllRefs().values()) {
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
	public RevWalk createRevsWalkBetweenTags(Repository repository, String startTag, String endTag)
			throws Exception {
		RevWalk walk = new RevWalk(repository);
		
		Ref startTagRef = repository.findRef(startTag);
		ObjectId startCommit = startTagRef.getObjectId();
        walk.markStart(walk.parseCommit(startCommit));
        
        if (endTag != null) {
	    		Ref endTagRef = repository.findRef(endTag);
	    		ObjectId endCommit = endTagRef.getObjectId();
            walk.markUninteresting(walk.parseCommit(endCommit));
        }
		
        walk.setRevFilter(commitsFilter);
		return walk;
	}
	
	@Override
	public RevWalk createRevsWalkBetweenCommits(Repository repository, String startCommitId, String endCommitId)
			throws Exception {
		RevWalk walk = new RevWalk(repository);
		
		ObjectId startCommit = repository.resolve(startCommitId);
        walk.markStart(walk.parseCommit(startCommit));

        if (endCommitId != null) {
        		ObjectId endCommit = repository.resolve(endCommitId);  
            walk.markUninteresting(walk.parseCommit(endCommit));
        }
		
		walk.setRevFilter(commitsFilter);
		return walk;
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

//	@Override
	public void fileTreeDiff2(Repository repository, RevCommit current, List<String> javaFilesBefore, List<String> javaFilesCurrent, Map<String, String> renamedFilesHint, boolean detectRenames) throws Exception {
        File workingDir = repository.getDirectory().getParentFile();
        String sha = current.getId().getName();
        String output;
        if (detectRenames) {
            output = ExternalProcess.execute(workingDir, "git", "diff", "--name-status", "--find-renames", sha + "^.." + sha);
        } else {
            output = ExternalProcess.execute(workingDir, "git", "diff", "--name-status", sha + "^.." + sha);
        }
        if (output.startsWith("fatal")) {
            throw new RuntimeException("git error " + output);
        }
        for (String line : output.split("\n")) {
        	String[] fields = line.split("\t");
        	char changeType = fields[0].charAt(0);
        	String path = fields[1];
        	if (isJavafile(path)) {
        		if (changeType != 'A') {
        			javaFilesBefore.add(path);
        		}
        		if (changeType != 'D') {
        			if (changeType == 'R') {
        				String newPath = fields[2];
        				javaFilesCurrent.add(newPath);
        				renamedFilesHint.put(path, newPath);
        			} else {
        				javaFilesCurrent.add(path);
        			}
        		}
        	}
        }
	}
	
	@Override
	public void fileTreeDiff(Repository repository, RevCommit current, List<String> javaFilesBefore, List<String> javaFilesCurrent, Map<String, String> renamedFilesHint, boolean detectRenames) throws Exception {
        if (current.getParentCount() > 0) {
			ObjectId oldHead = current.getParent(0).getTree();
	        ObjectId head = current.getTree();
	
	        // prepare the two iterators to compute the diff between
			ObjectReader reader = repository.newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldHead);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, head);
			// finally get the list of changed files
			try (Git git = new Git(repository)) {
				List<DiffEntry> diffs = git.diff()
				                    .setNewTree(newTreeIter)
				                    .setOldTree(oldTreeIter)
				                    .setShowNameAndStatusOnly(true)
				                    .call();
				if (detectRenames) {
				    RenameDetector rd = new RenameDetector(repository);
				    rd.addAll(diffs);
				    diffs = rd.compute();
				}
				
		        for (DiffEntry entry : diffs) {
		        	ChangeType changeType = entry.getChangeType();
		        	if (changeType != ChangeType.ADD) {
		        		String oldPath = entry.getOldPath();
		        		if (isJavafile(oldPath)) {
		        			javaFilesBefore.add(oldPath);
		        		}
		        	}
		    		if (changeType != ChangeType.DELETE) {
		        		String newPath = entry.getNewPath();
		        		if (isJavafile(newPath)) {
		        			javaFilesCurrent.add(newPath);
		        			if (changeType == ChangeType.RENAME) {
		        				String oldPath = entry.getOldPath();
		        				renamedFilesHint.put(oldPath, newPath);
		        			}
		        		}
		    		}
		        }
			}
        }
	}
	
	private boolean isJavafile(String path) {
		return path.endsWith(".java");
	}
}
