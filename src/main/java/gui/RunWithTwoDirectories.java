package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.IOException;
import java.nio.file.Path;

public class RunWithTwoDirectories {
    public static void main(String[] args) throws IOException {
        final String projectRoot = System.getProperty("user.dir");
        String folder1 = projectRoot + "/tmp/v1/";
        String folder2 = projectRoot + "/tmp/v2/";
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtDirectories(Path.of(folder1), Path.of(folder2));
        new WebDiff(projectASTDiff).run();
    }
}
