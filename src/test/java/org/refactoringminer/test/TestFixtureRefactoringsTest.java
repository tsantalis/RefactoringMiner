package org.refactoringminer.test;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.MoveCodeRefactoring;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.refactoringminer.utils.Assertions.assertHasSameElementsAs;

/**
 * The fixture refactorings (Extract Fixture, Split Fixture, and Move Code into a set-up or tear-down method) of commits
 * from the Test Fixture study, each chosen for a root cause of a false positive or a false negative of the detection,
 * with the refactorings and statement mappings the code before and after the commit supports (checked against the
 * study's gold). The commits are cached in oracle/commits; the expected output of a commit is in
 * fixture-refactorings/&lt;repository&gt;-&lt;commit&gt;.txt (an empty file: no fixture refactoring). A missing expected file
 * is written from the actual output and the test fails, so that every expected file is reviewed before it counts.
 */
public class TestFixtureRefactoringsTest {
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/fixture-refactorings/";

    @ParameterizedTest
    @CsvSource({
            // FP3 split: part of a fixture moved into a new fixture: Split Fixture, not Extract Fixture
            "https://github.com/o19s/elasticsearch-learning-to-rank.git, 919feb4c77c2414264c02ae6433b57677b7d222c",
            // FP3 split: part of a fixture moved into a new fixture: Split Fixture, not Extract Fixture
            "https://github.com/dialex/JColor.git, 9e35f9de6d4517faa1937794840df1bc17c2fecb",
            // FP4 rename: a fixture renamed: no Extract Fixture
            "https://github.com/jenkinsci/durable-task-plugin.git, dcc5241198a70205706746105c531b6f4973c1d2",
            // FP4 re-scope: class fixture replaced by a method fixture: no Move Code
            "https://github.com/allure-framework/allure1.git, d86cae32453fd8c16804ce90cae5bc1e510984ae",
            // FP4 re-scope: method fixture replaced by a class fixture: no Move Code
            "https://github.com/aliyun/aliyun-log-java-sdk.git, fc05507fba8393ebeb8655bdde7e7602c75c3e2a",
            // FP5 not a fixture: a method that is not a test fixture
            "https://github.com/zhuzhengping911/MySpringBoot.git, 770a22e2b33b8cdb2d040d939b847f43bdd43a7c",
            // FP5 not a fixture: a method that is not a test fixture
            "https://github.com/corningsun/springboot-girl.git, 9d2d117321739100de6809d4e95bbb11c4dfb8ac",
            // FP1 no source: a new fixture written from scratch
            "https://github.com/dwslab/melt.git, 107d89addbae121d9e712d53f6e10aba76c573dd",
            // FP1 no source: a new fixture written from scratch
            "https://github.com/datalogics/pdf-java-toolkit-samples.git, bb8e29043e29a1b4fb6d6c953a4060a35bf62ef1",
            // FP1 no source: an existing fixture gained code no method lost
            "https://github.com/alibaba/innodb-java-reader.git, 4e829225439c02ac7553bcde3dcee9220ef3088d",
            // FP1 no source: an existing fixture gained code no method lost
            "https://github.com/MinecraftForge/FernFlowerLegacy.git, 50776ac962055d7228176238fc1b04a8a33736a7",
            // FP1+FN move: the right source paired instead of a look-alike
            "https://github.com/xebia/Xebium.git, 5da73f85d1924bc8d318de9969d62fd7db2b67a9",
            // FP5+FN extract: not-a-fixture dropped, the real extraction kept
            "https://github.com/sgroschupf/zkclient.git, ece5ae61507c94e873b30bb4b51e45d172869696",
    })
    public void testFixtureRefactoringFalsePositives(String url, String commit) throws IOException {
        testFixtureRefactorings(url, commit);
    }

    @ParameterizedTest
    @CsvSource({
            // FN extract: code of a test extracted into a new fixture
            "https://github.com/fge/json-schema-validator-demo.git, ad96a7d5fc633633f65e1161e489563080b8e39b",
            // FN extract: code of a test extracted into a new fixture
            "https://github.com/CodeforLeipzig/lvz-viz.git, a9c6a63c501f6f362d433e3f70839905c511e322",
            // FN3 re-annotated: a helper re-annotated as a fixture: Extract Fixture
            "https://github.com/openhab/org.openhab.ui.habot.git, 8bbdec1cb1d9f3474fa33c0f991195be28005757",
            // FN3 re-annotated: methods re-annotated as fixtures: Extract Fixture
            "https://github.com/karakun/OpenWebStart.git, 07954df4d833898cdb6fcc278abb0084b94a82eb",
            // FN4 helper: code of a helper moved into a new fixture
            "https://github.com/fge/grappa.git, 2b07815edcc1c05de009608ef928695cf1de9e2c",
            // FN4 helper: code of a helper moved into a new fixture
            "https://github.com/mbedward/jaitools.git, 0150a63358acf9ad20f31e704c793f13e4e477f3",
            // FN5 field: a field initializer moved into a fixture
            "https://github.com/vert-x3/vertx-starter.git, 4a2cafb3beb56657b9741f573e3ff334b644150b",
            // FN5 field: a field initializer moved into a fixture
            "https://github.com/spotify/mobius.git, 3a69b4cff2068c1b514129c972ae503e855a05a8",
            // FN6 TestNG: code moved between TestNG fixtures
            "https://github.com/kpelykh/docker-java.git, ed7985dac43943094f5207939d1700118fdc740c",
            // FN move: code of a fixture moved into another existing fixture
            "https://github.com/leonlee/flume-ng-mongodb-sink.git, 7a713724f08c696200ca31905588508f2f66bf2b",
            // FN1 other class: code of tests moved to another class, into its new fixture
            "https://github.com/zebrunner/carina.git, 112db495b5853ef839d3025074f87422154e7eae",
            // FN1 other class: code moved into an existing fixture of another class
            "https://github.com/unfoldingWord-dev/ts-android.git, 423d36383cbb9066c331c1ca91c140f202b6543d",
            // FN move: code of tests moved into an existing fixture
            "https://github.com/konmik/testable-activity-example.git, bc3b2174ea6efd2f017d8b1974f64fc76f1560cf",
    })
    public void testFixtureRefactoringFalseNegatives(String url, String commit) throws IOException {
        testFixtureRefactorings(url, commit);
    }

    private static boolean isFixtureRefactoring(Refactoring r) {
        if (r.getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE) || r.getRefactoringType().equals(RefactoringType.SPLIT_FIXTURE))
            return true;
        return r instanceof MoveCodeRefactoring move &&
                (move.getTargetContainer().isSetUpMethod() || move.getTargetContainer().isTearDownMethod());
    }

    private void testFixtureRefactorings(String url, String commit) throws IOException {
        List<String> actual = new ArrayList<>();
        new GitHistoryRefactoringMinerImpl().detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), (String commitId, List<Refactoring> refactorings) -> {
            for (Refactoring r : refactorings) {
                if (isFixtureRefactoring(r)) {
                    MoveCodeRefactoring move = (MoveCodeRefactoring) r;
                    actual.add(r.toString());
                    for (AbstractCodeMapping mapping : move.getMappings()) {
                        if (!(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression))
                            actual.add(mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo());
                    }
                }
            }
        });
        String repository = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf(".git"));
        File expectedFile = new File(EXPECTED_PATH + repository + "-" + commit + ".txt");
        Supplier<String> lazyErrorMessage = () -> String.join(System.lineSeparator(), actual);
        if (!expectedFile.exists()) {
            FileUtils.writeLines(expectedFile, StandardCharsets.UTF_8.name(), actual);
            Assertions.fail("No expected output for " + expectedFile.getName() + ": the actual output was written there, review it");
        }
        List<String> expected = FileUtils.readLines(expectedFile, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank()).collect(Collectors.toList());
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }
}
