package gui;

import java.nio.file.Path;

import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gui.webdiff.WebDiff;

public class RunWithWorktree {
	public static void main(String[] args) throws Exception {
		String pathToClonedRepository = System.getProperty("user.dir");
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtWorktree(Path.of(pathToClonedRepository), "HEAD");
		new WebDiff(projectASTDiff).openInBrowser();
	}
}
