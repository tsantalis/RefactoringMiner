package br.ufmg.dcc.labsoft.refactoringanalyzer;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitServiceImpl implements GitService {

	Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

	@Override
	public Repository cloneIfNotExists(String projectPath, String cloneUrl, String branch) throws Exception {
		File folder = new File(projectPath);
		Repository repository;
		if (folder.exists()) {
			RepositoryBuilder builder = new RepositoryBuilder();
			repository = builder
					.setGitDir(new File(folder, ".git"))
					.readEnvironment()
					.findGitDir()
					.build();
			
			logger.info("Project {} is already cloned, current branch is {}", cloneUrl, repository.getBranch());
			
		} else {
			logger.info("Cloning {} ...", cloneUrl);
			Git git = Git.cloneRepository()
					.setDirectory(folder)
					.setURI(cloneUrl)
					.setCloneAllBranches(true)
					.call();
			repository = git.getRepository();
			logger.info("Done cloning {}, current branch is {}", cloneUrl, repository.getBranch());
		}

		if (branch != null && !repository.getBranch().equals(branch)) {
			Git git = new Git(repository);
			
			String localBranch = "refs/heads/" + branch;
			List<Ref> refs = git.branchList().call();
			boolean branchExists = false;
			for (Ref ref : refs) {
				if (ref.getName().equals(localBranch)) {
					branchExists = true;
				}
			}
			
			if (branchExists) {
				git.checkout()
					.setName(branch)
					.call();
			} else {
				git.checkout()
					.setCreateBranch(true)
					.setName(branch)
					.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
					.setStartPoint("origin/" + branch)
					.call();
			}
			
			logger.info("Project {} switched to {}", cloneUrl, repository.getBranch());
		}
		return repository;
	}

	@Override
	public int countCommits(Repository repository, String branch) throws Exception {
		RevWalk walk = new RevWalk(repository);
		try {
			Ref ref = repository.getRef(branch);
			ObjectId objectId = ref.getObjectId();
			RevCommit start = walk.parseCommit(objectId);
			walk.setRevFilter(RevFilter.NO_MERGES);
			//	      walk.markStart( localCommit );
			//	      walk.markStart( trackingCommit );
			//	      RevCommit mergeBase = walk.next();
			//	      walk.reset();
			//	      walk.setRevFilter( RevFilter.ALL );
			return RevWalkUtils.count( walk, start, null);
		} finally {
			walk.dispose();
		}
	}

}
