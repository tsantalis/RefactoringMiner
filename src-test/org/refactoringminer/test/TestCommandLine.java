package org.refactoringminer.test;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.RefactoringMiner;

import java.io.FileReader;
import java.util.List;

public class TestCommandLine {
    private static final String REPOS = "tmp1";
    @Test
    public void testBetweenCommits() throws Exception {
        String jsonPath = REPOS + "/mondrian/mondrian-bc-actual.json";
        String[] args = {
                "-bc",
                REPOS + "/mondrian",
                "8cfaafedb27947aa22d71c77635bb8c8a36e23a4",
                "871f7747deded94e721fa098561376ab304b24de",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectBetweenCommits(args);

        List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/mondrian-bc-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBetweenTags() throws Exception {
        String jsonPath = REPOS + "/mondrian/mondrian-bt-actual.json";
        String[] args = {
                "-bt",
                REPOS + "/mondrian",
                "3.5.14-R",
                "3.5.15-R",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectBetweenTags(args);

        List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/mondrian-bt-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAll() throws Exception {
        String jsonPath = REPOS + "/refactoring-toy-example/refactoring-toy-example-all-actual.json";
        String[] args = {
                "-a",
                REPOS + "/refactoring-toy-example",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAll(args);

        List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/refactoring-toy-example-all-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAllBranch() throws Exception {
        String jsonPath = REPOS + "/refactoring-toy-example/refactoring-toy-example-branch-actual.json";
        String[] args = {
                "-a",
                REPOS + "/refactoring-toy-example",
                "branch1",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAll(args);

        List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/refactoring-toy-example-branch-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCommit() throws Exception {
        String jsonPath = REPOS + "/refactoring-toy-example/refactoring-toy-example-commit-actual.json";
        String[] args = {
                "-c",
                REPOS + "/refactoring-toy-example",
                "36287f7c3b09eff78395267a3ac0d7da067863fd",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAtCommit(args);

        List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/refactoring-toy-example-commit-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assert.assertEquals(expected, actual);
    }
}
