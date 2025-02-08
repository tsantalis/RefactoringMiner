package org.refactoringminer.test;

import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.PullUpOperationRefactoring;
import it.unimi.dsi.fastutil.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            // apache/commons-math 8e995890ea35399b6da6bc86532f0694accd511b -> b31439f3ec9bb216465ae77de5f7cb8433dd3140 -> 1d5a4e2d3d0fbd894b4e344a3d6ea601c14ab80e -> 229c782087d2eaef17d23682fcd8b36a73bb756b -> 5b9f353eeabc824146443b3c413be1f670985b4d
            "https://github.com/victorgveloso/commons-math.git, 1, commons-math-fork-1.txt",
            // apache/commons-math cd6d71b967019626734e81103a897729e70cd64b -> 73812e41db0aa040b53c6ff3f35804c037aa2a9b
            "https://github.com/victorgveloso/commons-math.git, 2, commons-math-fork-2.txt",
    })
    public void testCommonsMathMappings(String url, int pullRequestId, String testResultFileName) throws Exception {
        testRefactoringRangeMappings(url, pullRequestId, testResultFileName, ref -> {
            if (ref instanceof AssertThrowsRefactoring) {
                AssertThrowsRefactoring assertThrowsRefactoring = (AssertThrowsRefactoring) ref;
                Set<AbstractCodeMapping> mapper = assertThrowsRefactoring.getAssertThrowsMappings();
                mapperInfo(mapper, assertThrowsRefactoring.getOperationBefore(), assertThrowsRefactoring.getOperationAfter());
            }
            else if (ref instanceof PullUpOperationRefactoring) {
                PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) ref;
                UMLOperationBodyMapper bodyMapper = pullUpOperationRefactoring.getBodyMapper();
                Set<AbstractCodeMapping> mapper = bodyMapper.getMappings();
                mapperInfo(mapper, pullUpOperationRefactoring.getOriginalOperation(), pullUpOperationRefactoring.getMovedOperation());
            }
            else if (ref instanceof ExtractOperationRefactoring && ((ExtractOperationRefactoring) ref).getRefactoringType() == RefactoringType.EXTRACT_AND_MOVE_OPERATION) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) ref;
                UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                Set<AbstractCodeMapping> mapper = bodyMapper.getMappings();
                mapperInfo(mapper, extractOperationRefactoring.getSourceOperationBeforeExtraction(), extractOperationRefactoring.getExtractedOperation());
            }
            else if (ref instanceof ExtractSuperclassRefactoring) {
                ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) ref;
                Set<Pair<? extends LocationInfoProvider, ? extends LocationInfoProvider>> mappings = matchNameWithClasses(extractSuperclassRefactoring.getUMLSubclassSetBefore(), extractSuperclassRefactoring.getUMLSubclassSetAfter(), extractSuperclassRefactoring.getInvolvedClassesAfterRefactoring());
                mapperInfo(mappings, extractSuperclassRefactoring.getUMLSubclassSetBefore(), extractSuperclassRefactoring.getExtractedClass());
            }
        });
    }

    private static Set<Pair<? extends LocationInfoProvider, ? extends LocationInfoProvider>> matchNameWithClasses(Set<UMLClass> umlSubclassSetBefore, Set<UMLClass> umlSubclassSetAfter, Set<ImmutablePair<String, String>> involvedClassesAfterRefactoring) {
        return involvedClassesAfterRefactoring.stream().map(pair -> {
                    Optional<UMLClass> before = umlSubclassSetBefore.stream().filter(clzz -> clzz.getName().equals(pair.left)).findFirst();
                    Optional<UMLClass> after = umlSubclassSetAfter.stream().filter(clzz -> clzz.getName().equals(pair.right)).findFirst();
                    return Pair.of(before, after);
                }).filter(pair -> pair.left().isPresent() && pair.right().isPresent())
                .map(pair -> Pair.of(pair.left().get(), pair.right().get())).collect(Collectors.toSet());
    }

    @BeforeEach
    void setUp() {
        miner = new GitHistoryRefactoringMinerImpl();
        actual = new ArrayList<>();
        expected = new ArrayList<>();
    }

    private void testRefactoringRangeMappings(String url, int pullRequestId, String testResultFileName, final Consumer<Refactoring> consumer) throws Exception {
        ProjectASTDiff diff = miner.diffAtPullRequest(url, pullRequestId, 500);
        for (Refactoring refactoring : diff.getRefactorings()) {
            consumer.accept(refactoring);
        }
        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
        Assertions.assertDoesNotThrow(() -> {
            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
        }, lazyErrorMessage);
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }

    private <T, Y> void mapperInfo(Set<Y> mappings, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        for(Y mapping : mappings) {
            if (mapping instanceof AbstractCodeMapping) {
                mapperInfo((AbstractCodeMapping) mapping, operationBefore, operationAfter);
            }
            else if (mapping instanceof UMLAbstractClass) {
                mapperInfo((UMLAbstractClass) mapping, operationBefore, operationAfter);
            }
            else {
                throw new IllegalArgumentException("Unknown mapping type: " + mapping.getClass().getName());
            }
        }
    }

    private <T> boolean mapperInfo(AbstractCodeMapping mapping, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        if(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)
            return false;
        String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
        actual.add(line);
        return true;
    }

    private <T> boolean mapperInfo(UMLAbstractClass mapping, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        String line = mapping.getLocationInfo().toString();
        actual.add(line);
        return true;
    }

    private <T,X extends LocationInfoProvider> boolean mapperInfo(Pair<X,X> mapping, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        String line = mapping.left().getLocationInfo() + "==" + mapping.right().getLocationInfo();
        actual.add(line);
        return true;
    }
}
