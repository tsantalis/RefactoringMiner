package org.refactoringminer.test;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import gr.uom.java.xmi.diff.ModifyClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.ModifyMethodAnnotationRefactoring;
import org.apache.commons.io.IOUtils;
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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

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
            "https://github.com/iluwatar/java-design-patterns.git, 6694d742a370e0f181530734481284de8d5dd8ef, java-design-patterns-6694d742a370e0f181530734481284de8d5dd8ef.txt",
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
            //"https://github.com/OpenGamma/Strata.git, e007f826c49075500def8638de8367960c054c19, Strata-e007f826c49075500def8638de8367960c054c19.txt", // FIXME: TestNG to AssertJ expected exception not supported
            //"https://github.com/rapidoid/rapidoid.git, 8596c1d82e9f0a36f40cd7ec393c6829e697836d, rapidoid-8596c1d82e9f0a36f40cd7ec393c6829e697836d.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
            //"https://github.com/zanata/zanata-platform.git, 0297e0513ac1f487f1570b1cc38979a73ac97da8, zanata-platform-0297e0513ac1f487f1570b1cc38979a73ac97da8.txt", // FIXME: TestNG to JUnit 4 expected exception not supported
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
        Assertions.assertIterableEquals(expected, actual, lazyErrorMessage);
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
