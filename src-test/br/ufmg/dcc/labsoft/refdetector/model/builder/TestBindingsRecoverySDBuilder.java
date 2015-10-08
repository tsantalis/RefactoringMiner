package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class TestBindingsRecoverySDBuilder {

	@Test
	public void test() throws Exception {
		GitService gitService = new GitServiceImpl();
		
		Repository repo = gitService.cloneIfNotExists(
			    "tmp/refactoring-toy-example",
			    "https://github.com/danilofes/refactoring-toy-example.git");
		
		detectAtCommit(repo, "05c1e773878bbacae64112f70964f4f2f7944398");
	}

	public void detectAtCommit(Repository repository, String commitId) throws Exception {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			walk.parseCommit(commit.getParent(0));
			List<String> filesBefore = new ArrayList<String>();
			List<String> filesCurrent = new ArrayList<String>();
			Map<String, String> renamedFilesHint = new HashMap<String, String>();
			gitService.fileTreeDiff(repository, commit, filesBefore, filesCurrent, renamedFilesHint);
			// If no java files changed, there is no refactoring. Also, if there are
			// only ADD's or only REMOVE's there is no refactoring
			//if (!filesBefore.isEmpty() && !filesCurrent.isEmpty()) {
				// Checkout and build model for current commit
			BindingsRecoverySDBuilder builder = new BindingsRecoverySDBuilder();
			final SDModel model = new SDModel();
			
			model.setAfter();
			gitService.checkout(repository, commitId);
			builder.analyze(projectFolder, filesCurrent, model);
			
			// Checkout and build model for parent commit
			model.setBefore();
			String parentCommit = commit.getParent(0).getName();
			gitService.checkout(repository, parentCommit);
			builder.analyze(projectFolder, filesBefore, model);
			//}
		} finally {
			walk.dispose();
		}
	}

}
