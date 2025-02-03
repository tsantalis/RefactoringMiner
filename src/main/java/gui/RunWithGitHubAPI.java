package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.IOException;

/* Created by pourya on 2022-12-26 9:30 p.m. */
public class RunWithGitHubAPI {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        String url = "https://github.com/termux/termux-app/commit/f80b46487df539c7e9214550002f461e5c66131c";

        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);

        new WebDiff(projectASTDiff).run();
    }
}
