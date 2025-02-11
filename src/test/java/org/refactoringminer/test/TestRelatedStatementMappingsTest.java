package org.refactoringminer.test;

import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.refactoringminer.utils.Assertions.assertHasSameElementsAs;
import static org.refactoringminer.test.TestJavadocDiff.generateClassDiff;

public class TestRelatedStatementMappingsTest {
    public static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";
    private GitHistoryRefactoringMinerImpl miner;
    private List<String> actual;
    private List<String> expected;


    @ParameterizedTest
    @CsvSource({
            //Migrate Expected Exception
            "https://github.com/apache/camel.git, c30deabcaed4726bce4371d76257db63f2eba87c, camel-c30deabcaed4726bce4371d76257db63f2eba87c.txt",
            "https://github.com/apache/commons-csv.git, e2f0a4d8a83a41eaa984086636a3712c682307ea, commons-csv-e2f0a4d8a83a41eaa984086636a3712c682307ea.txt",
            "https://github.com/apache/jmeter.git, d5b5b2f38c3341cbb934693c17d0574a241ad4f9, jmeter-d5b5b2f38c3341cbb934693c17d0574a241ad4f9.txt",
            "https://github.com/apache/plc4x.git, 4312eb178b6cb000ea8a3c78df70567182341331, plc4x-4312eb178b6cb000ea8a3c78df70567182341331.txt",
            "https://github.com/eclipse/eclipse-collections.git, f26addbce8e843f208805aa22f45dcfb6f8177f7, eclipse-collections-f26addbce8e843f208805aa22f45dcfb6f8177f7.txt",
            "https://github.com/EnMasseProject/enmasse.git, 5e0e683531b756ac62a497cca15ddb7211a34e24, enmasse-5e0e683531b756ac62a497cca15ddb7211a34e24.txt",
            "https://github.com/apache/clerezza.git, d77dbe2085ffa89b2a933637c5432d113b7432b8, clerezza-d77dbe2085ffa89b2a933637c5432d113b7432b8.txt",
            "https://github.com/iluwatar/java-design-patterns.git, 6694d742a370e0f181530734481284de8d5dd8ef, java-design-patterns-6694d742a370e0f181530734481284de8d5dd8ef-assertThrows.txt",
            "https://github.com/LMAX-Exchange/disruptor.git, 340f23ef88a32ceb8341820c15bfd9537303219c, disruptor-340f23ef88a32ceb8341820c15bfd9537303219c.txt",
            "https://github.com/neo4j/neo4j.git, b44c62bcdd6f7218bc97dae183ea0f6587bacd29, neo4j-b44c62bcdd6f7218bc97dae183ea0f6587bacd29.txt",
            "https://github.com/RohanNagar/thunder.git, 48776d511a0152f7b9cbe4460c8967bdd5ec3dc4, thunder-48776d511a0152f7b9cbe4460c8967bdd5ec3dc4.txt",
            "https://github.com/SAP/olingo-jpa-processor-v4.git, 5e77141293d32b015a92e48d099195840f2b2e87, olingo-jpa-processor-v4-5e77141293d32b015a92e48d099195840f2b2e87.txt",
            "https://github.com/zalando/problem.git, 1b987b88ecb5cc2c8df58ac8eda188fb2d6f5998, problem-1b987b88ecb5cc2c8df58ac8eda188fb2d6f5998.txt",
            //"https://github.com/apache/plc4x.git, 86da20c173ad291e5f3d5fe4c56f37d7f3c2c538, plc4x-86da20c173ad291e5f3d5fe4c56f37d7f3c2c538.txt", // FIXME: JUnit 5 to AssertJ expected exception not supported
            //"https://github.com/assertj/assertj-swing.git, 033bdd7832ca9a9647c8f7b7ecaa985d3115b206, assertj-swing-033bdd7832ca9a9647c8f7b7ecaa985d3115b206.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
            //"https://github.com/alexruiz/fest-swing-1.x.git, 033bdd7832ca9a9647c8f7b7ecaa985d3115b206, fest-swing-1.x-033bdd7832ca9a9647c8f7b7ecaa985d3115b206.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
            //"https://github.com/apache/flink.git, 2c5bc580e6c10fb3a2724a945847b5cc6b28df27, flink-2c5bc580e6c10fb3a2724a945847b5cc6b28df27.txt", // FIXME: JUnit 4 to AssertJ expected exception not supported
            //"https://github.com/apache/flink.git, 95c3499ea80d07c448c297e36fa5a1b5b4caea2b, flink-95c3499ea80d07c448c297e36fa5a1b5b4caea2b.txt", // FIXME: JUnit 4 to AssertJ expected exception not supported
            //"https://github.com/apache/cassandra-java-driver.git, 7d962af9291f69f0da6115375efd5bd5224a2353, cassandra-java-driver-7d962af9291f69f0da6115375efd5bd5224a2353.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
            //"https://github.com/OpenGamma/Strata.git, e007f826c49075500def8638de8367960c054c19, Strata-e007f826c49075500def8638de8367960c054c19-assertthrows.txt", // FIXME: TestNG to AssertJ expected exception not supported
            //"https://github.com/rapidoid/rapidoid.git, 8596c1d82e9f0a36f40cd7ec393c6829e697836d, rapidoid-8596c1d82e9f0a36f40cd7ec393c6829e697836d.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
            //"https://github.com/zanata/zanata-platform.git, 0297e0513ac1f487f1570b1cc38979a73ac97da8, zanata-platform-0297e0513ac1f487f1570b1cc38979a73ac97da8-assertthrows.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
            //"https://github.com/apache/commons-math.git, 5fbeb731b9d26a6f340fd3772e86cd23ba61c65a, commons-math-5fbeb731b9d26a6f340fd3772e86cd23ba61c65a.txt", // FIXME: try-fail-catch to JUnit 4 expected exception not supported
    })
    public void testAssertThrowsMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof AssertThrowsRefactoring) {
                AssertThrowsRefactoring assertThrowsRefactoring = (AssertThrowsRefactoring) ref;
                Set<AbstractCodeMapping> mapper = assertThrowsRefactoring.getAssertThrowsMappings();
                mapperInfo(mapper, assertThrowsRefactoring.getOperationBefore(), assertThrowsRefactoring.getOperationAfter());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Custom Runner
            "https://github.com/apache/iceberg.git, fac03ea3c0d8555d85b1e85c8e9f6ce178bc4e9b, iceberg-fac03ea3c0d8555d85b1e85c8e9f6ce178bc4e9b-runner.txt",
            "https://github.com/mapstruct/mapstruct.git, 293a12d7ffa22c29ad3f2d433b6e420514e29a8b, mapstruct-293a12d7ffa22c29ad3f2d433b6e420514e29a8b.txt",
    })
    public void testCustomRunnerMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof AddClassAnnotationRefactoring) {
                AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) ref;
                UMLAnnotation annotation = addClassAnnotationRefactoring.getAnnotation();
                if (Set.of("RunWith", "ExtendWith").contains(annotation.getTypeName())) {
                    mapperInfo(Set.of(Pair.of(null,annotation)), addClassAnnotationRefactoring.getClassBefore(), addClassAnnotationRefactoring.getClassAfter());
                }
            }
            else if (ref instanceof ModifyClassAnnotationRefactoring) {
                ModifyClassAnnotationRefactoring modifyClassAnnotationRefactoring = (ModifyClassAnnotationRefactoring) ref;
                UMLAnnotation annotationAfter = modifyClassAnnotationRefactoring.getAnnotationAfter();
                if (Set.of("RunWith", "ExtendWith").contains(annotationAfter.getTypeName())) {
                    UMLAnnotation annotationBefore = modifyClassAnnotationRefactoring.getAnnotationBefore();
                    mapperInfo(Set.of(Pair.of(annotationBefore,annotationAfter)), modifyClassAnnotationRefactoring.getClassBefore(), modifyClassAnnotationRefactoring.getClassAfter());
                }
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Custom Runner
            "https://github.com/apache/hadoop.git, 5c61ad24887f76dfc5a5935b2c5dceb6bfd99417, org.apache.hadoop.hdfs.web.TestAdlRead, org.apache.hadoop.fs.adl.TestAdlRead, hadoop-5c61ad24887f76dfc5a5935b2c5dceb6bfd99417.txt"
    })
    public void testCustomRunnerMappingsForSlowCommits(String url, String commit, String leftClassName, String rightClassName, String testResultFileName) throws Exception {
        UMLClassDiff classDiff = generateClassDiff(url, commit, new File(REPOS), leftClassName, rightClassName);
        if(classDiff != null) {
            for(UMLAnnotationDiff annotationDiff : classDiff.getAnnotationListDiff().getAnnotationDiffs()) {
                UMLAnnotation annotationAfter = annotationDiff.getAddedAnnotation();
                if (Set.of("RunWith", "ExtendWith").contains(annotationAfter.getTypeName())) {
                    UMLAnnotation annotationBefore = annotationDiff.getRemovedAnnotation();
                    mapperInfo(Set.of(Pair.of(annotationBefore,annotationAfter)), classDiff.getOriginalClass(), classDiff.getNextClass());
                }
            }
        }
        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
        Assertions.assertDoesNotThrow(() -> {
            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
        }, lazyErrorMessage);
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }

    @ParameterizedTest
    @CsvSource({
            //Introduce Equality Method  // TODO: Completely new refactoring needed! Replace multiple property checks with a single assertEqual of the entire object
             //"https://github.com/JodaOrg/joda-time.git, 119f68ba20f38f7b4b9d676d4a7b787e5e005b89, joda-time-119f68ba20f38f7b4b9d676d4a7b787e5e005b89.txt", // FIXME: Replacements are one-to-one, not one-to-many. Thus, it might be hard to detect this refactoring
            //Consolidate Multiple Assertions into a Fluent Assertion
             "https://github.com/cbeust/testng.git, 706dcf52c5df3591e7d9d49f0fb980f041fae385, testng-706dcf52c5df3591e7d9d49f0fb980f041fae385.txt", // FIXME: Missing fluent assertion replacements/mappings (InjectBeforeAndAfterMethodsWithTestResultSampleTest.java:46)
             //"https://github.com/dCache/dcache.git, 4a6e55f40f1c, dcache-4a6e55f40f1c.txt", // FIXME: Hamcrest assertion syntax not supported yet
             "https://github.com/atlanmod/NeoEMF.git, 0188e9aa280b800710848d68a93af4cb28b050da, NeoEMF-0188e9aa280b800710848d68a93af4cb28b050da.txt", // FIXME: Hamcrest assertion syntax not supported yet
            //Replace assertTrue(Double.isInfinite(x)) with assertEqual(Double.POSITIVE_INFINITY, x)
            "https://github.com/apache/commons-math.git, 9b08855c247eb7522fc4b25b8aaece2a0d58d990, commons-math-9b08855c247eb7522fc4b25b8aaece2a0d58d990.txt",
    })
    public void testReplaceAssertionMappings(String url, String commit, String testResultFileName) throws Exception {
        miner.detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), new RefactoringHandler() {
            @Override
            public void handleModelDiff(String commitId, List<Refactoring> refactoringsAtRevision, UMLModelDiff modelDiff) {
                super.handleModelDiff(commitId, refactoringsAtRevision, modelDiff);
                for (UMLClassDiff umlClassDiff : modelDiff.getCommonClassDiffList()) {
                    for (UMLOperationBodyMapper umlOperationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
                        Set<Pair<LocationInfoProvider, LocationInfoProvider>> replacementMappings = new HashSet<>();
                        boolean hasAssertionReplacement = false;
                        for (Replacement replacement : umlOperationBodyMapper.getReplacements()) {
                            switch (replacement.getType()) {
                                case ASSERTION_CONVERSION:
                                case METHOD_INVOCATION:
                                case METHOD_INVOCATION_EXPRESSION:
                                case METHOD_INVOCATION_NAME:
                                case METHOD_INVOCATION_NAME_AND_ARGUMENT:
                                case METHOD_INVOCATION_NAME_AND_EXPRESSION:
                                    //System.out.println(replacement.getType().toString());
                            }
                            if (replacement instanceof MethodInvocationReplacement) {
                                MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement) replacement;
                                //System.out.println(methodInvocationReplacement.getInvokedOperationBefore().getContainer().toQualifiedString());
                                //System.out.println(methodInvocationReplacement.getInvokedOperationAfter().getContainer().toQualifiedString());
                                if (methodInvocationReplacement.getInvokedOperationAfter().getName().contains("assert") ||
                                        methodInvocationReplacement.getInvokedOperationAfter().getName().contains("is") ||
                                        methodInvocationReplacement.getInvokedOperationBefore().getName().contains("assert") ||
                                        methodInvocationReplacement.getInvokedOperationBefore().getName().contains("is")) {
                                    replacementMappings.add(Pair.of(methodInvocationReplacement.getInvokedOperationBefore().asLeafExpression(), methodInvocationReplacement.getInvokedOperationAfter().asLeafExpression()));
                                    hasAssertionReplacement = true;
                                }
                            }
                        }
                        if (hasAssertionReplacement) {
                            //Set<AbstractCodeMapping> mapper = umlOperationBodyMapper.getMappings();
                            mapperInfo(replacementMappings, umlOperationBodyMapper.getOperation1(), umlOperationBodyMapper.getOperation2());
                        }

                    }
                }
            }
        });
        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
        Assertions.assertDoesNotThrow(() -> {
            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
        }, lazyErrorMessage);
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }


    @ParameterizedTest
    @CsvSource({
            //Inline Fixture
            // This is actually two refactorings: one more complex (Replace before fixture with fixture utility method that is invoked in the test methods) and inline after fixture
            "https://github.com/apache/hbase.git, 587f5bc11f9d5d37557baf36c7df110af860a95c, hbase-587f5bc11f9d5d37557baf36c7df110af860a95c-inline.txt",
    })
    public void testInlineFixtureMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
            else if (ref instanceof RemoveMethodAnnotationRefactoring) {
                RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) ref;
                UMLAnnotation annotation = removeMethodAnnotationRefactoring.getAnnotation();
                if (Set.of("Before", "BeforeEach", "BeforeAll", "BeforeClass","After", "AfterEach", "AfterAll", "AfterClass").contains(annotation.getTypeName())) {
                    mapperInfo(Set.of(Pair.of(annotation, null)), removeMethodAnnotationRefactoring.getOperationBefore(), removeMethodAnnotationRefactoring.getOperationAfter());
                }
            }
            else if (ref instanceof InlineOperationRefactoring) { // FIXME: Expected Inline of "@After tearDown()" not detected (probably because fixture is not invoked through a method call)
                InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) ref;
                UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                Set<AbstractCodeMapping> mapper = bodyMapper.getMappings();
                mapperInfo(mapper, inlineOperationRefactoring.getInlinedOperation(), inlineOperationRefactoring.getTargetOperationAfterInline());
            }
        });
    }

    @Disabled
    @ParameterizedTest
    @CsvSource({
            //Split Test
            "https://github.com/apache/commons-math.git, 5fbeb731b9d26a6f340fd3772e86cd23ba61c65a, commons-math-5fbeb731b9d26a6f340fd3772e86cd23ba61c65a.txt", // FIXME: Split not detected
            "https://github.com/apache/commons-math.git, 09c8b57924bc90dfcf93aa35eb79a6bd752add1d, commons-math-09c8b57924bc90dfcf93aa35eb79a6bd752add1d.txt", // FIXME: Split not detected
            "https://github.com/apache/commons-math.git, 5ca553511dea61641f248f71be203b91f1682e95, commons-math-5ca553511dea61641f248f71be203b91f1682e95.txt", // FIXME: Split not detected
            "https://github.com/apache/commons-math.git, 9b08855c247eb7522fc4b25b8aaece2a0d58d990, commons-math-9b08855c247eb7522fc4b25b8aaece2a0d58d990-split.txt", // Empty as it should be: it's a copy-paste rather than a split
            "https://github.com/apache/commons-math.git, de001e7bcf9acb761047bdcf40f48244f8b63642, commons-math-de001e7bcf9acb761047bdcf40f48244f8b63642-split.txt"
    })
    public void testSplitTestMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof SplitOperationRefactoring) {
                SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) ref;
                for (UMLOperationBodyMapper methodMapping : splitOperationRefactoring.getMappers()) {
                    mapperInfo(methodMapping.getMappings(), methodMapping.getOperation1(), methodMapping.getOperation2());
                }
            } else if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Specialize Expected Exception
            "https://github.com/apache/commons-math.git, c6d53a52582d2d4c6fdec7a5f1a8cbee16db0e65, commons-math-c6d53a52582d2d4c6fdec7a5f1a8cbee16db0e65-specialize-exception.txt", // Misses refactoring when try block does not match
            //"https://github.com/apache/commons-math.git, de001e7bcf9acb761047bdcf40f48244f8b63642, commons-math-de001e7bcf9acb761047bdcf40f48244f8b63642-specialize-exception.txt", // FIXME: Misses all refactoring since try blocks do not match (Also a split test)
    })
    public void testSpecializeExpectedExceptionMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof ChangeVariableTypeRefactoring) {
                ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = changeVariableTypeRefactoring.getReferences();
                mapperInfo(mapper, changeVariableTypeRefactoring.getOperationBefore(), changeVariableTypeRefactoring.getOperationAfter());
            }
        });
    }

    @Disabled
    @ParameterizedTest
    @CsvSource({
            //Merge Test
            "https://github.com/apache/commons-math.git, c6d53a52582d2d4c6fdec7a5f1a8cbee16db0e65, commons-math-c6d53a52582d2d4c6fdec7a5f1a8cbee16db0e65-merge.txt",
    })
    public void testMergeTestMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof MergeOperationRefactoring) {
                MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) ref;
                for (UMLOperationBodyMapper methodMapping : mergeOperationRefactoring.getMappers()) {
                    mapperInfo(methodMapping.getMappings(), methodMapping.getOperation1(), methodMapping.getOperation2());
                }
            } else if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Split Fixture
            "https://github.com/apache/druid.git, da32e1ae534a99c29ff60c5535f2d4cb0e344a73, druid-da32e1ae534a99c29ff60c5535f2d4cb0e344a73.txt",
            "https://github.com/spring-projects/spring-integration.git, d5d954def737038a5982ca34ecc8f14610061090, spring-integration-d5d954def737038a5982ca34ecc8f14610061090.txt",
    })
    public void testSplitFixturesMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof SplitOperationRefactoring) {
                SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) ref;
                for (UMLOperationBodyMapper methodMapping : splitOperationRefactoring.getMappers()) {
                    mapperInfo(methodMapping.getMappings(), methodMapping.getOperation1(), methodMapping.getOperation2());
                }
            }
            else if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Merge Fixture
            "https://github.com/apache/hadoop.git, 973987089090b428ae34a86926c8ef8ebca45aa5, hadoop-973987089090b428ae34a86926c8ef8ebca45aa5.txt",
            "https://github.com/apache/hbase.git, 587f5bc11f9d5d37557baf36c7df110af860a95c, hbase-587f5bc11f9d5d37557baf36c7df110af860a95c-merge.txt",
    })
    public void testMergeFixtureMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof MergeOperationRefactoring) {
                MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) ref;
                for (UMLOperationBodyMapper methodMapping : mergeOperationRefactoring.getMappers()) {
                    mapperInfo(methodMapping.getMappings(), methodMapping.getOperation1(), methodMapping.getOperation2());
                }
            }
            else if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Extract Fixture
            // "https://github.com/apache/camel.git, ee55a3bc6e04fea, camel-ee55a3bc6e04fea.txt", // FIXME: No move code refactoring detected
            "https://github.com/apache/struts.git, 0a71e2c3b92d2d58fda40f252a6a5a4392fa58b7, struts-0a71e2c3b92d2d58fda40f252a6a5a4392fa58b7.txt",
            "https://github.com/orientechnologies/orientdb.git, 1b371c7cecbc7ec14b81a3f8a08c2ab71d12577f, orientdb-1b371c7cecbc7ec14b81a3f8a08c2ab71d12577f.txt",
    })
    public void testExtractFixture(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Minimize Fixture
            "https://github.com/apache/hbase.git, 587f5bc11f9d5d37557baf36c7df110af860a95c, hbase-587f5bc11f9d5d37557baf36c7df110af860a95c-minimize.txt",
            //"https://github.com/spring-projects/spring-integration.git, 7edc55f5bf0fce164dabc26f005cc8cb2d008100, spring-integration-7edc55f5bf0fce164dabc26f005cc8cb2d008100.txt", // FIXME: Impure refactoring too different from old code
    })
    public void testMinimizeFixture(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) ref;
                Set<AbstractCodeMapping> mapper = moveCodeRefactoring.getMappings();
                mapperInfo(mapper, moveCodeRefactoring.getSourceContainer(), moveCodeRefactoring.getTargetContainer());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Replace Class Fixture with Method Fixture
            "https://github.com/apache/druid.git, 76cb06a8d8161d29d985ef048b89e6a82b489058, druid-76cb06a8d8161d29d985ef048b89e6a82b489058.txt", // It also has Replace Method Fixture with Class Fixture
            "https://github.com/apache/hadoop.git, 2f6bc250443c8d6fa6f18aab256c2ac8e585983b, hadoop-2f6bc250443c8d6fa6f18aab256c2ac8e585983b.txt",
            "https://github.com/apache/hadoop.git, 4334976187100afe3be499d63ead8f17f09f8a14, hadoop-4334976187100afe3be499d63ead8f17f09f8a14.txt",
            "https://github.com/apache/hadoop.git, 699d4204977cff31ac689457b4b99317e3bdc0d3, hadoop-699d4204977cff31ac689457b4b99317e3bdc0d3.txt",
            "https://github.com/apache/hbase.git, 484ccb7af4d614429f77f0c14d9b14c6fe8c9e17, hbase-484ccb7af4d614429f77f0c14d9b14c6fe8c9e17.txt",
            "https://github.com/orientechnologies/orientdb.git, 04242466a2d96209105515f915cb673f4e98f83a, orientdb-04242466a2d96209105515f915cb673f4e98f83a.txt",
            "https://github.com/orientechnologies/orientdb.git, 279dd2c2120883ee157b924faa0af2fa1981f6a9, orientdb-279dd2c2120883ee157b924faa0af2fa1981f6a9.txt",
            "https://github.com/orientechnologies/orientdb.git, 2949af2283c5e43403c99221ab2ab971925e47c6, orientdb-2949af2283c5e43403c99221ab2ab971925e47c6.txt",
            //Replace Method Fixture with Class Fixture
            "https://github.com/spring-projects/spring-integration.git, 0ead69529ea1fec992483c96f78c94e303eb6818, spring-integration-0ead69529ea1fec992483c96f78c94e303eb6818.txt",
            //Categorize Test Method
            //"https://github.com/debezium/debezium.git, 66bb7958604527aa975e72aa23be45163de39246, debezium-66bb7958604527aa975e72aa23be45163de39246.txt", // FIXME: Categorize not detected
            "https://github.com/strimzi/strimzi-kafka-operator.git, 9ab848e76f4c0b0399fb556c9d853fcbdf1c55f1, strimzi-kafka-operator-9ab848e76f4c0b0399fb556c9d853fcbdf1c55f1.txt",
    })
    public void testAddAndRemoveMethodAnnotationMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof AddMethodAnnotationRefactoring) {
                AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) ref;
                mapperInfo(Set.of(Pair.of(null, addMethodAnnotationRefactoring.getAnnotation())), addMethodAnnotationRefactoring.getOperationBefore(), addMethodAnnotationRefactoring.getOperationAfter());
            } else if (ref instanceof RemoveMethodAnnotationRefactoring) {
                RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) ref;
                mapperInfo(Set.of(Pair.of(removeMethodAnnotationRefactoring.getAnnotation(), null)), removeMethodAnnotationRefactoring.getOperationBefore(), removeMethodAnnotationRefactoring.getOperationAfter());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Replace Test Annotation between Class and Method
            //"https://github.com/FamilySearch/gedcomx-java.git, e6727ae0d1bc2ade6782c8d00398884644e00af, gedcomx-java-e6727ae0d1bc2ade6782c8d00398884644e00af.txt", // FIXME: Annotation replacement not detected (neither addition nor removal)
            "https://github.com/OpenGamma/Strata.git, 1dd64e965041a1e3fb81adf8ce9156c451d8252b, Strata-1dd64e965041a1e3fb81adf8ce9156c451d8252b-annotation.txt",
            "https://github.com/OpenGamma/Strata.git, 3ebc739351b45ac12712ad80852e49555e02929f, Strata-3ebc739351b45ac12712ad80852e49555e02929f.txt",
            "https://github.com/OpenGamma/Strata.git, 956bbd57300d1001bcbf3144b8dd36a6dc3f6e50, Strata-956bbd57300d1001bcbf3144b8dd36a6dc3f6e50.txt",
            "https://github.com/orientechnologies/orientdb.git, a8ac595e36c8b4c2c3069c365dcbed220726424d, orientdb-a8ac595e36c8b4c2c3069c365dcbed220726424d.txt",
            "https://github.com/zanata/zanata-platform.git, 0297e0513ac1f487f1570b1cc38979a73ac97da8, zanata-platform-0297e0513ac1f487f1570b1cc38979a73ac97da8-annotation.txt",
    })
    public void testChangeTestAnnotationGranularityMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            UMLAnnotation annotation = null;
            Object before = null;
            Object after = null;
            Set<Pair<UMLAnnotation, UMLAnnotation>> annotations = new HashSet<>();
            if (ref instanceof AddMethodAnnotationRefactoring) {
                AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) ref;
                annotation = addMethodAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(null, annotation));
                before = addMethodAnnotationRefactoring.getOperationBefore();
                after = addMethodAnnotationRefactoring.getOperationAfter();
            } else if (ref instanceof RemoveMethodAnnotationRefactoring) {
                RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) ref;
                annotation = removeMethodAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(annotation, null));
                before = removeMethodAnnotationRefactoring.getOperationBefore();
                after = removeMethodAnnotationRefactoring.getOperationAfter();
            } else if (ref instanceof AddClassAnnotationRefactoring) {
                AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) ref;
                annotation = addClassAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(null, annotation));
                before = addClassAnnotationRefactoring.getClassBefore();
                after = addClassAnnotationRefactoring.getClassAfter();
            } else if (ref instanceof RemoveClassAnnotationRefactoring) {
                RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) ref;
                annotation = removeClassAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(annotation, null));
                before = removeClassAnnotationRefactoring.getClassBefore();
                after = removeClassAnnotationRefactoring.getClassAfter();
            }
            if (annotation != null && annotation.getTypeName().equals("Test")) {
                mapperInfo(annotations, before, after);
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Rename Test
            "https://github.com/cloudfoundry/uaa.git, 69e3c6d3ce2b263b3fd3da61cabb8ca6d8bd563c, uaa-69e3c6d3ce2b263b3fd3da61cabb8ca6d8bd563c.txt",
            "https://github.com/cqframework/clinical_quality_language.git, 06b42c0bf811df6934138e39cffffa92fa617893, clinical_quality_language-06b42c0bf811df6934138e39cffffa92fa617893.txt",
    })
    public void testRenameTestMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof RenameOperationRefactoring) {
                RenameOperationRefactoring renameTestRefactoring = (RenameOperationRefactoring) ref;
                UMLOperationBodyMapper mapper = renameTestRefactoring.getBodyMapper();
                mapperInfo(mapper.getMappings(), renameTestRefactoring.getOriginalOperation(), renameTestRefactoring.getRenamedOperation());
            }
            else if (ref instanceof RenameClassRefactoring) {
                RenameClassRefactoring renameTestRefactoring = (RenameClassRefactoring) ref;
                mapperInfo(Collections.emptySet(), renameTestRefactoring.getOriginalClass(), renameTestRefactoring.getRenamedClass());
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            //Migrate Parameterize Test
            ////JUnit 4 to JUnit 5
            "https://github.com/apache/flink.git, 9b61b137bdc7eff773847b84e5cde116e6280c1d, flink-9b61b137bdc7eff773847b84e5cde116e6280c1d.txt",
            "https://github.com/iluwatar/java-design-patterns.git, 6694d742a370e0f181530734481284de8d5dd8ef, java-design-patterns-6694d742a370e0f181530734481284de8d5dd8ef-migrate-param.txt",
            "https://github.com/pbauerochse/youtrack-worklog-viewer.git, 40cc4a7a11aa40e08ec710c18e9fadb566685133, youtrack-worklog-viewer-40cc4a7a11aa40e08ec710c18e9fadb566685133.txt",
            ////JUnit's Parameterize Test to JUnit 5 Test Template
            "https://github.com/apache/iceberg.git, fac03ea3c0d8555d85b1e85c8e9f6ce178bc4e9b, iceberg-fac03ea3c0d8555d85b1e85c8e9f6ce178bc4e9b-migrate-param.txt",
            ////TestNG to JUnit 5
            "https://github.com/OpenGamma/Strata.git, 1dd64e965041a1e3fb81adf8ce9156c451d8252b, Strata-1dd64e965041a1e3fb81adf8ce9156c451d8252b-migrate-param.txt",
            //"https://github.com/OpenGamma/Strata.git, b2b9b629685ebc7e89e9a1667de88f2e878d5fc4, Strata-b2b9b629685ebc7e89e9a1667de88f2e878d5fc4.txt", //TODO: Too slow and fails without any error message
            "https://github.com/OpenGamma/Strata.git, e007f826c49075500def8638de8367960c054c19, Strata-e007f826c49075500def8638de8367960c054c19-migrate-param.txt",
            "https://github.com/zanata/zanata-platform.git, 0297e0513ac1f487f1570b1cc38979a73ac97da8, zanata-platform-0297e0513ac1f487f1570b1cc38979a73ac97da8-migrate-param.txt",
    })
    public void testParameterizedTestMigrationMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            UMLAnnotation annotation = null;
            Object before = null;
            Object after = null;
            Set<Pair<UMLAnnotation, UMLAnnotation>> annotations = new HashSet<>();
            if (ref instanceof AddMethodAnnotationRefactoring) {
                AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) ref;
                annotation = addMethodAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(null, annotation));
                before = addMethodAnnotationRefactoring.getOperationBefore();
                after = addMethodAnnotationRefactoring.getOperationAfter();
            } else if (ref instanceof RemoveMethodAnnotationRefactoring) {
                RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) ref;
                annotation = removeMethodAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(annotation, null));
                before = removeMethodAnnotationRefactoring.getOperationBefore();
                after = removeMethodAnnotationRefactoring.getOperationAfter();
            } else if (ref instanceof AddClassAnnotationRefactoring) {
                AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) ref;
                annotation = addClassAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(null, annotation));
                before = addClassAnnotationRefactoring.getClassBefore();
                after = addClassAnnotationRefactoring.getClassAfter();
            } else if (ref instanceof RemoveClassAnnotationRefactoring) {
                RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) ref;
                annotation = removeClassAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(annotation, null));
                before = removeClassAnnotationRefactoring.getClassBefore();
                after = removeClassAnnotationRefactoring.getClassAfter();
            } else if (ref instanceof AddAttributeAnnotationRefactoring) {
                AddAttributeAnnotationRefactoring addAttributeAnnotationRefactoring = (AddAttributeAnnotationRefactoring) ref;
                annotation = addAttributeAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(null, annotation));
                before = addAttributeAnnotationRefactoring.getAttributeBefore();
                after = addAttributeAnnotationRefactoring.getAttributeAfter();
            } else if (ref instanceof RemoveAttributeAnnotationRefactoring) {
                RemoveAttributeAnnotationRefactoring removeAttributeAnnotationRefactoring = (RemoveAttributeAnnotationRefactoring) ref;
                annotation = removeAttributeAnnotationRefactoring.getAnnotation();
                annotations.add(Pair.of(annotation, null));
                before = removeAttributeAnnotationRefactoring.getAttributeBefore();
                after = removeAttributeAnnotationRefactoring.getAttributeAfter();
            }
            if (annotation != null && Set.of("RunWith", "Parameterized.Parameters", "ParameterizedTest", "Test", "Parameters", "Parameter", "DataProvider", "ExtendWith", "ValueSource", "NullSource", "EmptySource", "NullAndEmptySource", "EnumSource", "MethodSource", "FieldSource", "CsvSource", "CsvFileSource", "ArgumentsSource").contains(annotation.getTypeName())) {
                mapperInfo(annotations, before, after);
            }
        });
    }

    @Disabled("All collected cases are edge cases that are not detected by the current implementation")
    @ParameterizedTest
    @CsvSource({
            //Parameterize Test with Framework support
            ////Extract Common Logic from Multiple Test Methods
            "https://github.com/aws/aws-sdk-java-v2.git, 4236a962dc0ca45149845317caa144a1ba768c5f, aws-sdk-java-v2-4236a962dc0ca45149845317caa144a1ba768c5f.txt", //FIXME: JUnit 4 parameterization not supported
            "https://github.com/Atrox/haikunatorjava.git, 42679988419b68dd51f0a7b3c045536b3c5ef37b, haikunatorjava-42679988419b68dd51f0a7b3c045536b3c5ef37b.txt", //FIXME: MethodSource not supported
            "https://github.com/opentripplanner/OpenTripPlanner.git, 1abed1191c2df7a747ef21cd3b669c14d54c3011, OpenTripPlanner-1abed1191c2df7a747ef21cd3b669c14d54c3011.txt", //FIXME: MethodSource not supported
            "https://github.com/samtools/htsjdk.git, 1734eb99e5dcf16d92febead5e1b62323e0b6199, htsjdk-1734eb99e5dcf16d92febead5e1b62323e0b6199.txt", //FIXME: TestNG not supported
            "https://github.com/apache/hbase.git, 2306820df8b41d9af5227465ee2cf9e18b8f0b5c, hbase-2306820df8b41d9af5227465ee2cf9e18b8f0b5c.txt", //FIXME: JUnit 4 parameterization not supported
            ////Add Parameterized Test
            "https://github.com/hapifhir/hapi-fhir/pull/5764.git, ad470cff726d800cbf9baa49abd6a9a536781ec0, hapi-fhir-pull-5764-ad470cff726d800cbf9baa49abd6a9a536781ec0.txt", //TODO: Should test addition of parameterized test be supported?
            ////Merge Data Provider
            "https://github.com/samtools/htsjdk.git, 17c4b9d29dc0ee7573d32e7364d36fc92e4b2493, htsjdk-17c4b9d29dc0ee7573d32e7364d36fc92e4b2493.txt", //FIXME: Merge Data Provider not supported
            ////Multiple data and multiple algorithms become parameterized test with inheritance and fixture overrides
            "https://github.com/apache/hadoop.git, 4d01dbda508691beb07a4c8bfe113ec568166ddc, hadoop-4d01dbda508691beb07a4c8bfe113ec568166ddc.txt", //FIXME: JUnit 4 parameterization not supported
    })
    public void testParameterizedTestMappings(String url, String commit, String testResultFileName) throws Exception {
        testRefactoringMappings(url, commit, testResultFileName, ref -> {
            if (ref instanceof ParameterizeTestRefactoring) {
                ParameterizeTestRefactoring parameterizedTestRefactoring = (ParameterizeTestRefactoring) ref;
                UMLOperationBodyMapper mapper = parameterizedTestRefactoring.getBodyMapper();
                mapperInfo(mapper.getMappings(), parameterizedTestRefactoring.getRemovedOperation(), parameterizedTestRefactoring.getParameterizedTestOperation());
            }
        });
    }

    @BeforeEach
    void setUp() {
        miner = new GitHistoryRefactoringMinerImpl();
        actual = new ArrayList<>();
        expected = new ArrayList<>();
    }

    private void testRefactoringMappings(String url, String commit, String testResultFileName, final Consumer<Refactoring> consumer) {
        miner.detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                for (Refactoring ref : refactorings) {
                    consumer.accept(ref);
                }
            }
        });
        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
        Assertions.assertDoesNotThrow(() -> {
            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
        }, lazyErrorMessage);
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }

    private <T, Y> void mapperInfo(Set<Y> mappings, T before, T after) {
        actual.add(before + " -> " + after);
        for (var mapping : mappings) {
            if (mapping instanceof AbstractCodeMapping) {
                if (!mapperInfo((AbstractCodeMapping) mapping)) {
                    continue;
                }
            } else if (mapping instanceof Pair) {
                if (!mapperInfo((Pair) mapping)) {
                    continue;
                }
            } else if (mapping instanceof AbstractCodeFragment) {
                if (!mapperInfo((AbstractCodeFragment) mapping)) {
                    continue;
                }
            }
            else {
                throw new IllegalArgumentException("Invalid mapping type: " + mapping.getClass());
            }
        }
    }

    private boolean mapperInfo(Pair mapping) {
        String line;
        if (mapping.getLeft() instanceof LeafExpression && mapping.getRight() instanceof LeafExpression)
            return false;
        if (mapping.getLeft() instanceof LocationInfoProvider && mapping.getRight() instanceof LocationInfoProvider) {
            line = ((LocationInfoProvider) mapping.getLeft()).getLocationInfo() + "==" + ((LocationInfoProvider) mapping.getRight()).getLocationInfo();
        } else {
            line = mapping.getLeft() + "==" + mapping.getRight();
        }
        actual.add(line);
        return true;
    }

    private boolean mapperInfo(AbstractCodeMapping mapping) {
        if (mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)
            return false;
        String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
        actual.add(line);
        return true;
    }

    private boolean mapperInfo(AbstractCodeFragment component) {
        if (component instanceof LeafExpression)
            return false;
        String line = component.getLocationInfo().toString();
        actual.add(line);
        return true;
    }
}
