package gui;

import gui.webdiff.DiffDriver;
import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

public class RunWithPullRequest {
    public static void main(String[] args) throws Exception {
        String url = "https://github.com/JabRef/jabref/pull/10847";

        DiffDriver diffDriver = new DiffDriver();
        diffDriver.setUrl(url);
        ProjectASTDiff projectASTDiff = diffDriver.getProjectASTDiff();
        new WebDiff(projectASTDiff).run();
    }
}
