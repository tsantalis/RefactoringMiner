package org.refactoringminer.test;

import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.util.List;
import java.util.Set;

public class TestSpecificRefactoringTest {
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    @ParameterizedTest
    @CsvSource({
            "https://github.com/Kyligence/kylin-on-parquet-v2.git,32044d9522b881ae1b4723667695c9c01ea342aa,/kylin-it/src/test/java/org/apache/kylin/storage/hbase/ITAclTableMigrationToolTest.java,84,1",
            "https://github.com/FudgeMsg/Fudge-Java.git,e4254e385dea9b915ad74d0c8a0375848e9279f0,/tests/unit/org/fudgemsg/taxon/RESTfulTaxonomyResolverTest.java,46,1",
            "https://github.com/EvoSuite/evosuite.git,ed67eeac4f9e0e8710e174db2df6da7985790240,/master/src/test/java/org/evosuite/localsearch/AnonymousClassSystemTest.java,52,3",
            "https://github.com/uber/h3-java.git,8b9d3f230393b4a89a21545745754eeb46f56516,/src/test/java/com/uber/h3core/TestH3CoreSystemInstance.java,35,1",
            // Similar to (4) but inline in the same test method
            "https://github.com/MrSorrow/spring-framework.git,d5ee787e1e6653257720afe31ee3f8819cd4605c,/spring-webmvc/src/test/java/org/springframework/web/servlet/view/ResourceBundleViewResolverTests.java,138,1",
            "https://github.com/antlibs/ant-contrib.git,3975cfdae22ddd462f034d79f51d35e159cdd3d4,/src/test/java/net/sf/antcontrib/platform/ShellScriptTest.java,56,1",
            // Inline variant where if-body ends with return (not detected because condition is inverted!)
            "https://github.com/MrSorrow/spring-framework.git,f47bbb0d9e8023590c0fd965acb009719aea6b67,/spring-beans/src/test/java/org/springframework/beans/factory/serviceloader/ServiceLoaderTests.java,39,3",
            "https://github.com/apache/directory-kerby.git,efc2eed5b0ddb889e39049bc64628772034f91a3,/kerby-kerb/kerb-crypto/src/test/java/org/apache/kerby/kerberos/kerb/crypto/String2keyTest.java,273,15",
            "https://github.com/arangodb/arangodb-java-driver-async.git,01dc48162eac4368161fcbf370827d693a4b0355,/src/test/java/com/arangodb/ArangoCollectionTest.java,808,1",
            "https://github.com/google/guice.git,690e189a7d6830fb61c10fdc46a8985eac0a7d3a,/core/test/com/googlecode/guice/BytecodeGenTest.java,101,1",
            "https://github.com/neo4j-attic/graphdb.git,cd634d05df324d52ec24620ffb8c9ae04c662be2,/kernel/src/test/java/org/neo4j/kernel/impl/batchinsert/TestBigBatchStore.java,98,1",
            // Extract Precondition and extract test utility method
            "https://github.com/ViktorC/PP4J.git,12a40d99481208f74d4d6f796513a107d9f3b2bf,/src/test/java/net/viktorc/pp4j/impl/JPPEPerformanceTest.java,108,1",
            // assert -> assume
            "https://github.com/ViktorC/PP4J.git,12a40d99481208f74d4d6f796513a107d9f3b2bf,/src/test/java/net/viktorc/pp4j/impl/PPEPerformanceTest.java,168,1",
            "https://github.com/google/guice.git,690e189a7d6830fb61c10fdc46a8985eac0a7d3a,/core/test/com/googlecode/guice/OSGiContainerTest.java,75,1",
            // Reuses test utility method from another class (BaseTest) + Inline variant where if-body ends with return
            "https://github.com/arangodb/arangodb-java-driver-async.git,01dc48162eac4368161fcbf370827d693a4b0355,/src/test/java/com/arangodb/ArangoDatabaseTest.java,115,1",
            "https://github.com/arangodb/arangodb-java-driver-async.git,87412560daaa588b49aa5d9e8526d709d830fbb7,/src/test/java/com/arangodb/ArangoGraphTest.java,236,1",
            "https://github.com/arangodb/arangodb-java-driver-async.git,01dc48162eac4368161fcbf370827d693a4b0355,/src/test/java/com/arangodb/ArangoSearchTest.java,56,1",
            // Suddenly uses a wrapper of the dependent variable but in practice does a simple Inline variant where if-body is the whole test method
            "https://github.com/bayofmany/peapod.git,83cb4ba1a66db6033ede34947e2d63eac451728c,/core/src/test/java/peapod/FramedGraphTest.java,112,1",
            // Reuse existing Test Fixture variant where if-body is the whole test method
            "https://github.com/briar/briar.git,61276c81d23793ad5a580304b60e12986ce2637c,/bramble-core/src/test/java/org/briarproject/bramble/system/UnixSecureRandomProviderTest.java,25,1",
            "https://github.com/briar/briar.git,61276c81d23793ad5a580304b60e12986ce2637c,/bramble-core/src/test/java/org/briarproject/bramble/system/UnixSecureRandomSpiTest.java,35,1",
            // Inline variant where if-body is almost the whole test method
            // Simple fix: let method body contains more statements as long as the if is the last statement
            "https://github.com/dropwizard/dropwizard.git,89712d2346c3a08e9a986a4e256ceff6b37a3cb5,/dropwizard-logging/src/test/java/io/dropwizard/logging/DefaultLoggingFactoryPrintErrorMessagesTest.java,78,1",
            // Inline variant where if-body ends with return + co-occurrence with split conditional (each component of the if-expression becomes one individual expression passed to assumptions)
            "https://github.com/findbugsproject/findbugs.git,6013de21efb0945c0c14a1e7f72d3db4fd5e524d,/findbugs/src/junit/edu/umd/cs/findbugs/DetectorsTest.java,72,1",
            // Ignore -> assume
            "https://github.com/latexdraw/latexdraw.git,ced2e95351fca3188e99ac147b0c639c1ac8120c,/src/test/java/net/sf/latexdraw/instrument/TestCanvasSelection.java,96,1",
            // Inline variant where if-body ends with return (with extract variable)
            "https://github.com/Aloisius/hadoop-s3a.git,e8cc8de58bc1b5cd312ab06ab093cdc0b4323d51,/src/test/java/org/apache/hadoop/fs/s3a/S3AFileSystemContractBaseTest.java,59,1",
            // Too complex case (perhaps, I should drop it)
            "https://github.com/apache/metamodel.git,717a3a443624f2da1a883a26d17a25886c77d67e,/hbase/src/test/java/org/apache/metamodel/hbase/HBaseTestCase.java,84,1",
    })
    void testAssumptionIntroducingRefactoring(String url,String currentCommit,String filepath,int line, int expectedNumberOfRefactoringOccurrences) throws Exception {
        Assumptions.assumeTrue(Set.of("32044d9522b881ae1b4723667695c9c01ea342aa", "e4254e385dea9b915ad74d0c8a0375848e9279f0", "ed67eeac4f9e0e8710e174db2df6da7985790240", "8b9d3f230393b4a89a21545745754eeb46f56516", "f47bbb0d9e8023590c0fd965acb009719aea6b67", "efc2eed5b0ddb889e39049bc64628772034f91a3").contains(currentCommit));
        System.out.println(url + " " + currentCommit + " " + filepath + " " + line);
        Set<RefactoringType> expectedRefactoringTypes = Set.of(RefactoringType.REPLACE_ASSERTION_WITH_ASSUMPTION, RefactoringType.REPLACE_CONDITIONAL_WITH_ASSUMPTION, RefactoringType.REPLACE_IGNORE_WITH_ASSUMPTION);
        UMLModelDiff modelDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(url, currentCommit, new File(REPOS)).getModelDiff();
        List<Refactoring> refactorings = modelDiff.getRefactorings().stream().filter(ref -> expectedRefactoringTypes.contains(ref.getRefactoringType())).toList();
        Assertions.assertEquals(expectedNumberOfRefactoringOccurrences, refactorings.size());
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            https://github.com/uber/h3-java.git,8b9d3f230393b4a89a21545745754eeb46f56516,/src/test/java/com/uber/h3core/TestH3CoreSystemInstance.java,35,1
            """)
    void testExtractFixtureRefactoring(String url,String currentCommit,String filepath,int line, int expectedNumberOfRefactoringOccurrences) throws Exception {
        System.out.println(url + " " + currentCommit + " " + filepath + " " + line);
        Set<RefactoringType> expectedRefactoringTypes = Set.of(RefactoringType.EXTRACT_FIXTURE);
        UMLModelDiff modelDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(url, currentCommit, new File(REPOS)).getModelDiff();
        List<Refactoring> refactorings = modelDiff.getRefactorings().stream().filter(ref -> expectedRefactoringTypes.contains(ref.getRefactoringType())).toList();
        Assertions.assertEquals(expectedNumberOfRefactoringOccurrences, refactorings.size());
    }
}
