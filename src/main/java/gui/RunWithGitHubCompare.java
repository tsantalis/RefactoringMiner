package gui;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gui.webdiff.WebDiff;

public class RunWithGitHubCompare {

	public static void main(String[] args) throws Exception {
		String url = "https://github.com/bazelbuild/bazel/compare/66a577385539887743bd99b9239b9b70fc55b4f8...b5b551dc2d0117b577506ba69286b243bde181a0";
		String repo = URLHelper.getRepo(url);
		Pair<String, String> commitPair = URLHelper.getCommitPairFromGitHubCompareURL(url);
		
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtGitHubCompare(repo, commitPair.getLeft(), commitPair.getRight());
		// To visualize the diff add the following line
		new WebDiff(projectASTDiff).openInBrowser();
	}
}
