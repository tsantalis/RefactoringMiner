package gui;

import gui.webdiff.DiffDriver;
import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

import java.io.IOException;
import java.nio.file.Path;

public class RunWithTwoDirectories {
    public static void main(String[] args) throws IOException {
        final String projectRoot = System.getProperty("user.dir");
        String folder1 = projectRoot + "/tmp/v1/";
        String folder2 = projectRoot + "/tmp/v2/";

        DiffDriver diffDriver = new DiffDriver();
        diffDriver.setSrc(Path.of(folder1));
        diffDriver.setDst(Path.of(folder2));
        ProjectASTDiff projectASTDiff = diffDriver.getProjectASTDiff();
        new WebDiff(projectASTDiff).run();
    }
}
