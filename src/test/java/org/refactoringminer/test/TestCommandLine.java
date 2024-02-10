package org.refactoringminer.test;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.RefactoringMiner;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;

@Isolated
public class TestCommandLine {
    @TempDir
    private static Path REPOS = Path.of("tmp/");
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/commandline/";
    private String jsonPath;

    @BeforeAll
    public static void setUp() throws Exception {
//        GitServiceImpl gitService = new GitServiceImpl();
//        gitService.cloneIfNotExists(REPOS + "mondrian", "https://github.com/pentaho/mondrian.git");
//        gitService.cloneIfNotExists(REPOS + "refactoring-toy-example", "https://github.com/danilofes/refactoring-toy-example.git");
    }
    @AfterEach
    public void tearDown() {
        if (jsonPath != null && Path.of(jsonPath).toFile().exists()) {
            new File(jsonPath).delete();
        }
    }

    @Disabled
    @Test
    public void testBetweenCommits() throws Exception {
        jsonPath = REPOS + "mondrian/mondrian-bc-actual.json";
        String[] args = {
                "-bc",
                REPOS + "mondrian",
                "8cfaafedb27947aa22d71c77635bb8c8a36e23a4",
                "871f7747deded94e721fa098561376ab304b24de",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectBetweenCommits(args);
        waitUntilFileExists(jsonPath);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "mondrian-bc-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    @Disabled
    @Test
    public void testBetweenTags() throws Exception {
        jsonPath = REPOS + "mondrian/mondrian-bt-actual.json";
        String[] args = {
                "-bt",
                REPOS + "mondrian",
                "3.5.14-R",
                "3.5.15-R",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectBetweenTags(args);
        waitUntilFileExists(jsonPath);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "mondrian-bt-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    @Disabled
    @Test
    public void testAll() throws Exception {
        jsonPath = REPOS + "refactoring-toy-example/refactoring-toy-example-all-actual.json";
        String[] args = {
                "-a",
                REPOS + "refactoring-toy-example",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAll(args);
        waitUntilFileExists(jsonPath);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "refactoring-toy-example-all-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    @Disabled
    @Test
    public void testAllBranch() throws Exception {
        jsonPath = REPOS + "refactoring-toy-example/refactoring-toy-example-branch-actual.json";
        String[] args = {
                "-a",
                REPOS + "refactoring-toy-example",
                "branch1",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAll(args);
        waitUntilFileExists(jsonPath);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "refactoring-toy-example-branch-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    @Disabled
    @Test
    public void testCommit() throws Exception {
        jsonPath = REPOS + "refactoring-toy-example/refactoring-toy-example-commit-actual.json";
        String[] args = {
                "-c",
                REPOS + "refactoring-toy-example",
                "36287f7c3b09eff78395267a3ac0d7da067863fd",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAtCommit(args);
        waitUntilFileExists(jsonPath);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "refactoring-toy-example-commit-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
            "097122eb9c39a46a00a5b36117014cea0a3bd34c",
            "7e71cd03b4fb1bb6ca5132e9cffcf56e418b4cb3",
            "1db65a271ef6574e1dd240669dc816fcd17740fd",
            "2b6a91a212e592a66bdad175f7b3fb8041c2ae6b",
            "2800c57981f27d04b97e5994c3f6325ca301f110",
            "1517a87eb1effb2aac0c75b5f5ea6abc25407ab0",
            "a3f8b3669bcb771ecb25de50d6d7f1431e763d8d"
    })
    public void testGitHubCommit(String commit) throws Exception {
        jsonPath = REPOS + "drill-" + commit + "-actual.json";
        String[] args = {
                "-gc",
                "https://github.com/apache/drill.git",
                commit,
                "100",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAtGitHubCommit(args);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "drill-" + commit + "-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGitHubPullRequest() throws Exception {
        jsonPath = REPOS + "drill-gp-actual.json";
        String[] args = {
                "-gp",
                "https://github.com/apache/drill.git",
                "1762",
                "100",
                "-json",
                jsonPath
        };
        RefactoringMiner.detectAtGitHubPullRequest(args);
        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "drill-gp-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        Assertions.assertEquals(expected, actual);
    }

    private void waitUntilFileExists(String path) throws InterruptedException {
        while (!Path.of(path).toFile().exists()) {
            Thread.sleep(100);
        }
    }
}
