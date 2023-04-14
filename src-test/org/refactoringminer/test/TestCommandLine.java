package org.refactoringminer.test;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.RefactoringMiner;
import org.refactoringminer.util.PrefixSuffixUtils;

import java.io.FileReader;
import java.util.List;
import java.util.regex.Pattern;

public class TestCommandLine extends LightJavaCodeInsightFixtureTestCase {
    private static final String REPOS = "tmp1";
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src-test/data/commandline/";
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private static void assertJSON(List<String> expected, List<String> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Patch<String> patch = DiffUtils.diff(expected, actual);
        List<AbstractDelta<String>> deltas = patch.getDeltas();
        int changedLines = 0;
        int linesWithNumericDifference = 0;
        for(AbstractDelta<String> delta : deltas) {
            Chunk<String> source = delta.getSource();
            Chunk<String> target = delta.getTarget();
            List<String> sourceLines = source.getLines();
            List<String> targetLines = target.getLines();
            if(sourceLines.size() == targetLines.size()) {
                for(int i = 0; i< sourceLines.size(); i++) {
                    changedLines++;
                    String s1 = sourceLines.get(i);
                    String s2 = targetLines.get(i);
                    String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
                    String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
                    if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
                        int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
                        int endIndexS1 = s1.lastIndexOf(commonSuffix);
                        String diff1 = beginIndexS1 > endIndexS1 ? "" :	s1.substring(beginIndexS1, endIndexS1);
                        int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
                        int endIndexS2 = s2.lastIndexOf(commonSuffix);
                        String diff2 = beginIndexS2 > endIndexS2 ? "" :	s2.substring(beginIndexS2, endIndexS2);
                        if(!diff1.isEmpty() && INTEGER.matcher(diff1).matches() && !diff2.isEmpty() && INTEGER.matcher(diff2).matches()) {
                            linesWithNumericDifference++;
                        }
                    }
                }
            }
        }
        Assert.assertEquals(changedLines, linesWithNumericDifference);
    }

    @Test
    public void testBetweenCommits() throws Exception {
        RefactoringMiner miner = new RefactoringMiner(getProject());
        String jsonPath = REPOS + "/mondrian/mondrian-bc-actual.json";
        String[] args = {
                "-bc",
                REPOS + "/mondrian",
                "8cfaafedb27947aa22d71c77635bb8c8a36e23a4",
                "871f7747deded94e721fa098561376ab304b24de",
                "-json",
                jsonPath
        };
        miner.detectBetweenCommits(args);

        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "mondrian-bc-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        assertJSON(expected, actual);
    }

    @Test
    public void testBetweenTags() throws Exception {
        RefactoringMiner miner = new RefactoringMiner(getProject());
        String jsonPath = REPOS + "/mondrian/mondrian-bt-actual.json";
        String[] args = {
                "-bt",
                REPOS + "/mondrian",
                "3.5.14-R",
                "3.5.15-R",
                "-json",
                jsonPath
        };
        miner.detectBetweenTags(args);

        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "mondrian-bt-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        assertJSON(expected, actual);
    }

    @Test
    public void testAll() throws Exception {
        RefactoringMiner miner = new RefactoringMiner(getProject());
        String jsonPath = REPOS + "/refactoring-toy-example/refactoring-toy-example-all-actual.json";
        String[] args = {
                "-a",
                REPOS + "/refactoring-toy-example",
                "-json",
                jsonPath
        };
        miner.detectAll(args);

        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "refactoring-toy-example-all-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        assertJSON(expected, actual);
    }

    @Test
    public void testAllBranch() throws Exception {
        RefactoringMiner miner = new RefactoringMiner(getProject());
        String jsonPath = REPOS + "/refactoring-toy-example/refactoring-toy-example-branch-actual.json";
        String[] args = {
                "-a",
                REPOS + "/refactoring-toy-example",
                "branch1",
                "-json",
                jsonPath
        };
        miner.detectAll(args);

        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "refactoring-toy-example-branch-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        assertJSON(expected, actual);
    }

    @Test
    public void testCommit() throws Exception {
        RefactoringMiner miner = new RefactoringMiner(getProject());
        String jsonPath = REPOS + "/refactoring-toy-example/refactoring-toy-example-commit-actual.json";
        String[] args = {
                "-c",
                REPOS + "/refactoring-toy-example",
                "36287f7c3b09eff78395267a3ac0d7da067863fd",
                "-json",
                jsonPath
        };
        miner.detectAtCommit(args);

        List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "refactoring-toy-example-commit-expected.json"));
        List<String> actual = IOUtils.readLines(new FileReader(jsonPath));
        assertJSON(expected, actual);
    }
}
