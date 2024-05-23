package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class RunWithPullRequest {
    public static void main(String[] args) throws Exception {
        String repo = "https://github.com/JabRef/jabref.git";
        int PR = 10847;
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtPullRequest(repo, PR, 10000000);
        new WebDiff(projectASTDiff).run();
    }
}
