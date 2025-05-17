package gui;

import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gui.webdiff.WebDiff;

public class RunWithCommitRange {

	public static void main(String[] args) throws Exception {
		String repo = "https://github.com/bennidi/mbassador.git";
		String startCommit = "2ae0e5fae09cb0cf4127a5528b21de0e7b2cf60d";
		String endCommit = "9ce3ceb6f4f13ff016ee6c7e24ca6a38eb1c189f";
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitRange(repo, startCommit, endCommit);
		// To visualize the diff add the following line
		new WebDiff(projectASTDiff).openInBrowser();
	}

}
