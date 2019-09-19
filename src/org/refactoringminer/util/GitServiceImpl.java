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

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.refactoringminer.api.Churn;
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
		for (Ref ref : repository.getRefDatabase().getRefs()) {
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

	public void fileTreeDiff(Repository repository, RevCommit currentCommit, List<String> javaFilesBefore, List<String> javaFilesCurrent, Map<String, String> renamedFilesHint) throws Exception {
        if (currentCommit.getParentCount() > 0) {
        	ObjectId oldTree = currentCommit.getParent(0).getTree();
	        ObjectId newTree = currentCommit.getTree();
        	final TreeWalk tw = new TreeWalk(repository);
        	tw.setRecursive(true);
        	tw.addTree(oldTree);
        	tw.addTree(newTree);

        	final RenameDetector rd = new RenameDetector(repository);
        	rd.setRenameScore(80);
        	rd.addAll(DiffEntry.scan(tw));

        	for (DiffEntry diff : rd.compute(tw.getObjectReader(), null)) {
        		ChangeType changeType = diff.getChangeType();
        		String oldPath = diff.getOldPath();
        		String newPath = diff.getNewPath();
        		if (changeType != ChangeType.ADD) {
	        		if (isJavafile(oldPath)) {
	        			javaFilesBefore.add(oldPath);
	        		}
	        	}
        		if (changeType != ChangeType.DELETE) {
	        		if (isJavafile(newPath)) {
	        			javaFilesCurrent.add(newPath);
	        		}
        		}
        		if (changeType == ChangeType.RENAME && diff.getScore() >= rd.getRenameScore()) {
        			if (isJavafile(oldPath) && isJavafile(newPath)) {
        				renamedFilesHint.put(oldPath, newPath);
        			}
        		}
        	}
        }
	}

	private boolean isJavafile(String path) {
		return path.endsWith(".java");
	}

	@Override
	public Churn churn(Repository repository, RevCommit currentCommit) throws Exception {
		if (currentCommit.getParentCount() > 0) {
        	ObjectId oldTree = currentCommit.getParent(0).getTree();
	        ObjectId newTree = currentCommit.getTree();
        	final TreeWalk tw = new TreeWalk(repository);
        	tw.setRecursive(true);
        	tw.addTree(oldTree);
        	tw.addTree(newTree);
        	
        	List<DiffEntry> diffs = DiffEntry.scan(tw);
        	DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
    		diffFormatter.setRepository(repository);
    		diffFormatter.setContext(0);
    		
        	int addedLines = 0;
    		int deletedLines = 0;
        	for (DiffEntry entry : diffs) {
    			FileHeader header = diffFormatter.toFileHeader(entry);
            	List<? extends HunkHeader> hunks = header.getHunks();
            	for (HunkHeader hunkHeader : hunks) {
            		for (Edit edit : hunkHeader.toEditList()) {
    					if (edit.getType() == Type.INSERT) {
    						addedLines += edit.getLengthB();
    					} else if (edit.getType() == Type.DELETE) {
    						deletedLines += edit.getLengthA();
    					} else if (edit.getType() == Type.REPLACE) {
    						deletedLines += edit.getLengthA();
    						addedLines += edit.getLengthB();
    					}
    				}
            	}
        	}
        	diffFormatter.close();
        	return new Churn(addedLines, deletedLines);
		}
		return null;
	}
}
