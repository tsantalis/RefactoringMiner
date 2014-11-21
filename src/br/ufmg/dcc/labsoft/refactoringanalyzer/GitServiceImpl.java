package br.ufmg.dcc.labsoft.refactoringanalyzer;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

public class GitServiceImpl implements GitService {

	@Override
	public Repository cloneIfNotExists(String projectPath, String cloneUrl, String branch) throws Exception {
		File folder = new File(projectPath);
		Git git;
		if (folder.exists()) {
			System.out.println("Project " + cloneUrl + " already cloned.");

			RepositoryBuilder builder = new RepositoryBuilder();
			Repository repository = builder
					.setGitDir(new File(folder, ".git"))
					.readEnvironment()
					.findGitDir()
					.build();
			git = new Git(repository);

			git.checkout()
			.setStartPoint(Constants.HEAD)
			.setName(branch)
			.call();
		} else {
			System.out.print("Cloning " + cloneUrl + " ... ");
			git = Git.cloneRepository()
					.setDirectory(folder)
					.setURI(cloneUrl)
					.setCloneAllBranches(false)
					.call();
			System.out.println("Done.");
		}
		return git.getRepository();
	}

}
