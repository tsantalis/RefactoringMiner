package gui;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import gui.webdiff.WebDiff;

public class RunWithLocallyClonedCommitRange {

	public static void main(String[] args) throws Exception {
		String repo = "https://github.com/bennidi/mbassador.git";
		String startCommit = "2ae0e5fae09cb0cf4127a5528b21de0e7b2cf60d";
		String endCommit = "9ce3ceb6f4f13ff016ee6c7e24ca6a38eb1c189f";

		GitService gitService = new GitServiceImpl();
		String projectName = repo.substring(repo.lastIndexOf("/") + 1, repo.length() - 4);
		String pathToClonedRepository = System.getProperty("user.dir") + "/tmp/" + projectName;
		Repository repository = gitService.cloneIfNotExists(pathToClonedRepository, repo);

		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitRange(repository, startCommit, endCommit);
		// To visualize the diff add the following line
		new WebDiff(projectASTDiff).openInBrowser();
	}

}
