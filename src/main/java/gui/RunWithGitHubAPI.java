package gui;

import gui.webdiff.DiffDriver;
import gui.webdiff.WebDiff;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

import java.io.IOException;


/* Created by pourya on 2022-12-26 9:30 p.m. */
public class RunWithGitHubAPI {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
	    String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";

	    DiffDriver diffDriver = new DiffDriver();
		diffDriver.setUrl(url);
	    ProjectASTDiff projectASTDiff = diffDriver.getProjectASTDiff();
	    new WebDiff(projectASTDiff).run();

    }
}
