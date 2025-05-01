package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class RunWithPullRequest {
    public static void main(String[] args) throws Exception {
        String url = "https://github.com/JabRef/jabref/pull/10847";
        String repo = URLHelper.getRepo(url);
        String PR = URLHelper.getPRID(url);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtPullRequest(repo, Integer.parseInt(PR), 10000000);
        new WebDiff(projectASTDiff).openInBrowser();
    }
}
