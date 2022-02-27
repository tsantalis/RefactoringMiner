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

    @Test
    public void testGitHubCommit() throws Exception {
    	String[] commits = {
    			"097122eb9c39a46a00a5b36117014cea0a3bd34c",
    			"7e71cd03b4fb1bb6ca5132e9cffcf56e418b4cb3",
    			"1db65a271ef6574e1dd240669dc816fcd17740fd",
    			"2b6a91a212e592a66bdad175f7b3fb8041c2ae6b",
    			"2800c57981f27d04b97e5994c3f6325ca301f110",
    			"1517a87eb1effb2aac0c75b5f5ea6abc25407ab0",
    			"a3f8b3669bcb771ecb25de50d6d7f1431e763d8d"
    	};

    	for(String commit : commits) {
    		String jsonPath = REPOS + "/drill/drill-" + commit + "-actual.json";
    		String[] args = {
    				"-gc",
    				"https://github.com/apache/drill.git",
    				commit,
    				"100",
    				"-json",
    				jsonPath
    		};
    		RefactoringMiner.detectAtGitHubCommit(args);

    		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/drill-" + commit + "-expected.json"));
    		List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
    		Assert.assertEquals(expected, actual);
    	}
    }

    @Test
    public void testGitHubPullRequest() throws Exception {
        String jsonPath = REPOS + "/drill/drill-gp-actual.json";
        String[] args = {
                "-gp",
                "https://github.com/apache/drill.git",
                "1762",
                "100",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAtGitHubPullRequest(args);

        List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/drill-gp-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assert.assertEquals(expected, actual);
    }
}
