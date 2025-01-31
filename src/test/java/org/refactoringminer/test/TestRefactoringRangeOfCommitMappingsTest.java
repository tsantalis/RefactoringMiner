package org.refactoringminer.test;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.refactoringminer.utils.Assertions.assertHasSameElementsAs;

public class TestRefactoringRangeOfCommitMappingsTest {
    public static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";
    private GitHistoryRefactoringMinerImpl miner;
    private List<String> actual;
    private List<String> expected;


    @ParameterizedTest
    @CsvSource({
            // https://github.com/apache/commons-math/commit/8e995890ea35399b6da6bc86532f0694accd511b -> b31439f3ec9bb216465ae77de5f7cb8433dd3140 -> 1d5a4e2d3d0fbd894b4e344a3d6ea601c14ab80e -> 229c782087d2eaef17d23682fcd8b36a73bb756b -> 5b9f353eeabc824146443b3c413be1f670985b4d
            // "https://github.com/apache/commons-math.git, 8e995890ea35399b6da6bc86532f0694accd511b, commons-math-8e995890ea35399b6da6bc86532f0694accd511b.txt",
            // "https://github.com/apache/commons-math.git, b31439f3ec9bb216465ae77de5f7cb8433dd3140, commons-math-b31439f3ec9bb216465ae77de5f7cb8433dd3140.txt",
            // "https://github.com/apache/commons-math.git, 1d5a4e2d3d0fbd894b4e344a3d6ea601c14ab80e, commons-math-1d5a4e2d3d0fbd894b4e344a3d6ea601c14ab80e.txt",
            // "https://github.com/apache/commons-math.git, 229c782087d2eaef17d23682fcd8b36a73bb756b, commons-math-229c782087d2eaef17d23682fcd8b36a73bb756b.txt",
            // "https://github.com/apache/commons-math.git, 5b9f353eeabc824146443b3c413be1f670985b4d, commons-math-5b9f353eeabc824146443b3c413be1f670985b4d.txt",
            "https://github.com/victorgveloso/commons-math.git, 1, commons-math-fork-1.txt",
            // https://github.com/apache/commons-math/commit/cd6d71b967019626734e81103a897729e70cd64b -> 73812e41db0aa040b53c6ff3f35804c037aa2a9b
            // "https://github.com/apache/commons-math.git, cd6d71b967019626734e81103a897729e70cd64b, commons-math-cd6d71b967019626734e81103a897729e70cd64b.txt",
            // "https://github.com/apache/commons-math.git, 73812e41db0aa040b53c6ff3f35804c037aa2a9b, commons-math-73812e41db0aa040b53c6ff3f35804c037aa2a9b.txt",
            "https://github.com/victorgveloso/commons-math.git, 2, commons-math-fork-2.txt",
    })
    public void testAssertThrowsMappings(String url, int pullRequestId, String testResultFileName) throws Exception {
        testRefactoringRangeMappings(url, pullRequestId, testResultFileName, ref -> {
            if (ref instanceof AssertThrowsRefactoring) { // TODO: Replace with correct Refactoring Type (probably need to create it)
                AssertThrowsRefactoring assertThrowsRefactoring = (AssertThrowsRefactoring) ref; // TODO: Replace with correct Refactoring Type (probably need to create it)
                Set<AbstractCodeMapping> mapper = assertThrowsRefactoring.getAssertThrowsMappings();
                mapperInfo(mapper, assertThrowsRefactoring.getOperationBefore(), assertThrowsRefactoring.getOperationAfter());
            }
        });
    }

    @BeforeEach
    void setUp() {
        miner = new GitHistoryRefactoringMinerImpl();
        actual = new ArrayList<>();
        expected = new ArrayList<>();
    }

    private void testRefactoringRangeMappings(String url, int pullRequestId, String testResultFileName, final Consumer<Refactoring> consumer) throws Exception {
        ProjectASTDiff diff = miner.diffAtPullRequest(url, pullRequestId, 500);
        // TODO: Complete this test
//        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
//        Assertions.assertDoesNotThrow(() -> {
//            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
//        }, lazyErrorMessage);
//        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }

    private <T> void mapperInfo(Set<AbstractCodeMapping> mappings, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        for(AbstractCodeMapping mapping : mappings) {
            if(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)
                continue;
            String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
            actual.add(line);
        }
    }
}
