package org.refactoringminer.test;

import static org.refactoringminer.astDiff.utils.URLHelper.nthIndexOf;
import static org.refactoringminer.test.TestJavadocDiff.generateClassDiff;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import gr.uom.java.xmi.diff.BodyMapperProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.ModelDiffRefactoringHandler;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.MoveCodeRefactoring;
import gr.uom.java.xmi.diff.ParameterizeTestRefactoring;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLEnumConstantDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class TestStatementMappings {
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
	private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";

    @ParameterizedTest
    @CsvSource({
            "https://github.com/Kyligence/kylin-on-parquet-v2.git,32044d9522b881ae1b4723667695c9c01ea342aa",
            "https://github.com/FudgeMsg/Fudge-Java.git,e4254e385dea9b915ad74d0c8a0375848e9279f0",
            "https://github.com/EvoSuite/evosuite.git,ed67eeac4f9e0e8710e174db2df6da7985790240",
            "https://github.com/uber/h3-java.git,8b9d3f230393b4a89a21545745754eeb46f56516",
            // Similar to (4) but inline in the same test method
            "https://github.com/MrSorrow/spring-framework.git,d5ee787e1e6653257720afe31ee3f8819cd4605c",
            "https://github.com/antlibs/ant-contrib.git,3975cfdae22ddd462f034d79f51d35e159cdd3d4",
            // Inline variant where if-body ends with return (not detected because condition is inverted!)
            "https://github.com/MrSorrow/spring-framework.git,f47bbb0d9e8023590c0fd965acb009719aea6b67",
            "https://github.com/apache/directory-kerby.git,efc2eed5b0ddb889e39049bc64628772034f91a3",
            "https://github.com/arangodb/arangodb-java-driver-async.git,01dc48162eac4368161fcbf370827d693a4b0355",
            "https://github.com/google/guice.git,690e189a7d6830fb61c10fdc46a8985eac0a7d3a",
            "https://github.com/neo4j-attic/graphdb.git,cd634d05df324d52ec24620ffb8c9ae04c662be2",
            // Extract Precondition and extract test utility method
            "https://github.com/ViktorC/PP4J.git,12a40d99481208f74d4d6f796513a107d9f3b2bf",
            // assert -> assume
            "https://github.com/ViktorC/PP4J.git,12a40d99481208f74d4d6f796513a107d9f3b2bf",
            "https://github.com/google/guice.git,690e189a7d6830fb61c10fdc46a8985eac0a7d3a",
            // Reuses test utility method from another class (BaseTest) + Inline variant where if-body ends with return
            "https://github.com/arangodb/arangodb-java-driver-async.git,01dc48162eac4368161fcbf370827d693a4b0355",
            "https://github.com/arangodb/arangodb-java-driver-async.git,87412560daaa588b49aa5d9e8526d709d830fbb7",
            "https://github.com/arangodb/arangodb-java-driver-async.git,01dc48162eac4368161fcbf370827d693a4b0355",
            // Suddenly uses a wrapper of the dependent variable but in practice does a simple Inline variant where if-body is the whole test method
            "https://github.com/bayofmany/peapod.git,83cb4ba1a66db6033ede34947e2d63eac451728c",
            // Reuse existing Test Fixture variant where if-body is the whole test method
            "https://github.com/briar/briar.git,61276c81d23793ad5a580304b60e12986ce2637c",
            "https://github.com/briar/briar.git,61276c81d23793ad5a580304b60e12986ce2637c",
            // Inline variant where if-body is almost the whole test method
            // Simple fix: let method body contains more statements as long as the if is the last statement
            "https://github.com/dropwizard/dropwizard.git,89712d2346c3a08e9a986a4e256ceff6b37a3cb5",
            // Inline variant where if-body ends with return + co-occurrence with split conditional (each component of the if-expression becomes one individual expression passed to assumptions)
            "https://github.com/findbugsproject/findbugs.git,6013de21efb0945c0c14a1e7f72d3db4fd5e524d",
            // Ignore -> assume
            "https://github.com/latexdraw/latexdraw.git,ced2e95351fca3188e99ac147b0c639c1ac8120c",
            // Inline variant where if-body ends with return (with extract variable)
            "https://github.com/Aloisius/hadoop-s3a.git,e8cc8de58bc1b5cd312ab06ab093cdc0b4323d51",
            // Too complex case (perhaps, I should drop it)
            "https://github.com/apache/metamodel.git,717a3a443624f2da1a883a26d17a25886c77d67e",
    })
    void testAssumptionIntroducingRefactoring(String url,String currentCommit) throws Exception {
        Assumptions.assumeTrue(Set.of("32044d9522b881ae1b4723667695c9c01ea342aa", "e4254e385dea9b915ad74d0c8a0375848e9279f0", "ed67eeac4f9e0e8710e174db2df6da7985790240", "8b9d3f230393b4a89a21545745754eeb46f56516", "f47bbb0d9e8023590c0fd965acb009719aea6b67", "efc2eed5b0ddb889e39049bc64628772034f91a3").contains(currentCommit));
        Assertions.assertDoesNotThrow(() -> new GitHistoryRefactoringMinerImpl().detectAtCommitWithGitHubAPI(url, currentCommit, new File(REPOS), (ModelDiffRefactoringHandler) (commitId, refactoringsAtRevision, modelDiff) -> {
            final List<String> actual = new ArrayList<>();
            for (Refactoring refactoring : Assertions.assertDoesNotThrow(() -> modelDiff.getRefactorings())) {
                if (refactoring instanceof BodyMapperProvider ref) {
                    mapperInfo(ref.getBodyMapper(), actual);
                }
            }
            String filepath = EXPECTED_PATH + url.substring(url.lastIndexOf('/'), url.indexOf(".git")) + "-" + currentCommit + ".txt";
            List<String> expected = IOUtils.readLines(Assertions.assertDoesNotThrow(() -> new FileReader(filepath)));
            if (expected.size() == 0 && actual.size() != 0) {
                Assertions.assertDoesNotThrow(() -> Files.write(Path.of(filepath), actual));
            }
            expected = IOUtils.readLines(Assertions.assertDoesNotThrow(() -> new FileReader(filepath)));
            Assertions.assertEquals(expected.size(), actual.size());
            Assertions.assertTrue(expected.containsAll(actual));
            Assertions.assertTrue(actual.containsAll(expected));
        }));
    }

	@Test
	public void testMultipleInlinedMethodsToTheSameMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/square/okhttp.git", "a55e0090c999d155e4e588a34d9792a510ad8c68", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof InlineOperationRefactoring) {
                    InlineOperationRefactoring ex = (InlineOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "okhttp-a55e0090c999d155e4e588a34d9792a510ad8c68.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedInlineMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/pouryafard75/TestCases.git", "e47272d6e1390b6366f577b84c58eae50f8f0a69", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            List<UMLOperationBodyMapper> mappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof InlineOperationRefactoring) {
                    InlineOperationRefactoring ex = (InlineOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!mappers.contains(bodyMapper)) {
                        mappers.add(bodyMapper);
                        if(!bodyMapper.isNested()) {
                            if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                                parentMappers.add(bodyMapper.getParentMapper());
                            }
                        }
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "infinispan-043030723632627b0908dca6b24dae91d3dfd938-reverse.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/eclipse-vertx/vert.x.git, 32a8c9086040fd6d6fa11a214570ee4f75a4301f, vertx-32a8c9086040fd6d6fa11a214570ee4f75a4301f.txt",
		"https://github.com/apache/camel.git, ee55a3bc6e04fea54bd30cc1d3926020ea024661, camel-ee55a3bc6e04fea54bd30cc1d3926020ea024661-inline.txt"
	})
	public void testNestedInlineMethodStatementMappings2(String url, String commitId, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS), (commitId1, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof InlineOperationRefactoring) {
                    InlineOperationRefactoring ex = (InlineOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithLambdaParameters() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/pouryafard75/TestCases.git", "d01dfd14c0f8cae6ad4f78171011cd839b980e00", new File(REPOS), (commitId, refactorings) -> {
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    mapperInfo(bodyMapper, actual);
                    UMLOperationBodyMapper parentMapper = bodyMapper.getParentMapper();
                    if(parentMapper != null) {
                        mapperInfo(parentMapper, actual);
                    }
                }
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "duplicatedCode-d01dfd14c0f8cae6ad4f78171011cd839b980e00.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}


	@ParameterizedTest
	@CsvSource({
		"https://github.com/tsantalis/RefactoringMiner.git, fbd80e76c68558ba58b62311aa1c34fb38baf53a, processLeaves, miner-fbd80e76c68558ba58b62311aa1c34fb38baf53a.txt",
		"https://github.com/javaparser/javaparser.git, f4ce6ce924ffbd03518c64cea9b830d04f75b849, apply, javaparser-f4ce6ce924ffbd03518c64cea9b830d04f75b849.txt",
		"https://github.com/apache/commons-lang.git, 50c1fdecb4ed33ec1bb0d449f294c299d5369701, createNumber, commons-lang-50c1fdecb4ed33ec1bb0d449f294c299d5369701.txt"
	})
	public void testCopiedStatementMappings(String url, String commitId, String containerName, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNonIsomorphicControlStructureStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "e0a4ee07084bc6ab56a20fbc4a18863462da93eb", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("getNextInputSplit") && mapper.getContainer2().getName().equals("getNextInputSplit")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-e0a4ee07084bc6ab56a20fbc4a18863462da93eb.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@ParameterizedTest
	@CsvSource({
			"https://github.com/infinispan/infinispan.git, 043030723632627b0908dca6b24dae91d3dfd938, infinispan-043030723632627b0908dca6b24dae91d3dfd938.txt",
			"https://github.com/google/j2objc.git, d05d92de40542e85f9f26712d976e710be82914e, j2objc-d05d92de40542e85f9f26712d976e710be82914e.txt",
			"https://github.com/facebook/buck.git, 7e104c3ed4b80ec8e9b72356396f879d1067cc40, buck-7e104c3ed4b80ec8e9b72356396f879d1067cc40.txt",
			"https://github.com/k9mail/k-9.git, 9d44f0e06232661259681d64002dd53c7c43099d, k9mail-9d44f0e06232661259681d64002dd53c7c43099d.txt", // FIX 3472-3472==3541-3541
			"https://github.com/tsantalis/RefactoringMiner.git, 447005f5c62ad6236aad9116e932f13c4d449546, miner-447005f5c62ad6236aad9116e932f13c4d449546.txt", // WithIntermediateDelegate
			"https://github.com/hibernate/hibernate-orm.git, a1e8d7cb0dcb4bd58fc5d210031bd0fb28196034, hibernate-orm-a1e8d7cb0dcb4bd58fc5d210031bd0fb28196034.txt",
			"https://github.com/junit-team/junit5.git, 5b4c642be9b1d09f9b9cebad7dd55fa40aae78bc, junit5-5b4c642be9b1d09f9b9cebad7dd55fa40aae78bc.txt"
	})
	public void testNestedExtractMethodStatementMappings(String url, String commitId, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS), (commitId1, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/skylot/jadx.git", "2d8d4164830631d3125575f055b417c5addaa22f", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    if(!ex.getSourceOperationBeforeExtraction().getClassName().equals("jadx.core.utils.RegionUtils")) {
                        UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                        if(!bodyMapper.isNested()) {
                            if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                                parentMappers.add(bodyMapper.getParentMapper());
                            }
                        }
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jadx-2d8d4164830631d3125575f055b417c5addaa22f.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
			"FetchAndMergeEntry, src/main/java/org/jabref/gui/mergeentries/FetchAndMergeEntry.java, jabref-12025.txt",
			"PdfMergeMetadataImporter, src/main/java/org/jabref/logic/importer/fileformat/PdfMergeMetadataImporter.java, jabref-12310.txt"
	})
	public void testNestedExtractMethodStatementMappingsWithStreamsMigration(String fileName, String path, String testResultFileName) throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + fileName + "-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + fileName + "-v2.txt"));
		fileContentsBefore.put(path, contentsV1);
		fileContentsCurrent.put(path, contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<Refactoring> refactorings = modelDiff.getRefactorings();
		List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
		for (Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
				UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
				if(!bodyMapper.isNested()) {
					if(!parentMappers.contains(bodyMapper.getParentMapper())) {
						parentMappers.add(bodyMapper.getParentMapper());
					}
				}
				mapperInfo(bodyMapper, actual);
			}
			else if(ref instanceof InlineVariableRefactoring) {
				InlineVariableRefactoring inline = (InlineVariableRefactoring)ref;
				for (LeafMapping mapping : inline.getSubExpressionMappings()) {
					String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
					actual.add(line);
				}
			}
		}
		for(UMLOperationBodyMapper parentMapper : parentMappers) {
			mapperInfo(parentMapper, actual);
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/phishman3579/java-algorithms-implementation.git", "ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            List<UMLOperationBodyMapper> additionalMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    UMLAbstractClassDiff classDiff = bodyMapper.getClassDiff();
                    if(classDiff != null) {
                        for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                            if(!additionalMappers.contains(mapper)) {
                                if(mapper.getContainer1().getName().equals("testJavaMap") && mapper.getContainer2().getName().equals("testJavaMap")) {
                                    additionalMappers.add(mapper);
                                }
                                else if(mapper.getContainer1().getName().equals("testJavaCollection") && mapper.getContainer2().getName().equals("testJavaCollection")) {
                                    additionalMappers.add(mapper);
                                }
                                else if(mapper.getContainer1().getName().equals("showComparison") && mapper.getContainer2().getName().equals("showComparison")) {
                                    additionalMappers.add(mapper);
                                }
                            }
                        }
                    }
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
            for(UMLOperationBodyMapper mapper : additionalMappers) {
                mapperInfo(mapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "phishman-ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
			"https://github.com/tsantalis/RefactoringMiner.git, 68319df7c453a52778d7853b59d5a2bfe5ec5065, miner-68319df7c453a52778d7853b59d5a2bfe5ec5065.txt",
			"https://github.com/tsantalis/RefactoringMiner.git, acf37d016bfff710ce5de4f608299385cfb586c6, miner-acf37d016bfff710ce5de4f608299385cfb586c6.txt"
	})
	public void testDuplicatedExtractMethodStatementMappings2(String url, String commitId, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS), (commitId1, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithZeroIdenticalStatements() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/deeplearning4j/deeplearning4j.git", "91cdfa1ffd937a4cb01cdc0052874ef7831955e2", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(bodyMapper.getContainer1().getClassName().equals("org.deeplearning4j.optimize.solvers.BackTrackLineSearch")) {
                        if(!bodyMapper.isNested()) {
                            if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                                parentMappers.add(bodyMapper.getParentMapper());
                            }
                        }
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "deeplearning4j-91cdfa1ffd937a4cb01cdc0052874ef7831955e2.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithTwoLevelOptimization() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/Alluxio/alluxio.git", "9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "alluxio-9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedTryFinallyBlockBetweenOriginalAndExtractedMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/JoanZapata/android-iconify.git", "eb500cca282e39d01a9882e1d0a83186da6d1a26", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "android-iconify-eb500cca282e39d01a9882e1d0a83186da6d1a26.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedAndNestedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-boot.git", "becced5f0b7bac8200df7a5706b568687b517b90", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-boot-becced5f0b7bac8200df7a5706b568687b517b90.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithSingleCallSite() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/thymeleaf/thymeleaf.git", "378ba37750a9cb1b19a6db434dfa59308f721ea6", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "thymeleaf-378ba37750a9cb1b19a6db434dfa59308f721ea6.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/k9mail/k-9.git, 23c49d834d3859fc76a604da32d1789d2e863303, k9mail-23c49d834d3859fc76a604da32d1789d2e863303.txt",
		"https://github.com/javaparser/javaparser.git, 2d3f5e219af9d1ba916f1dc21a6169a41a254632, javaparser-2d3f5e219af9d1ba916f1dc21a6169a41a254632.txt",
		"https://github.com/checkstyle/checkstyle.git, ab2f93f9bf61816d84154e636d32c81c05854e24, checkstyle-ab2f93f9bf61816d84154e636d32c81c05854e24.txt",
		"https://github.com/apache/hive.git, 102b23b16bf26cbf439009b4b95542490a082710, hive-102b23b16bf26cbf439009b4b95542490a082710.txt",
		"https://github.com/osmandapp/Osmand.git, c45b9e6615181b7d8f4d7b5b1cc141169081c02c, OsmAnd-c45b9e6615181b7d8f4d7b5b1cc141169081c02c.txt",
		"https://github.com/spring-projects/spring-boot.git, 20d39f7af2165c67d5221f556f58820c992d2cc6, spring-boot-20d39f7af2165c67d5221f556f58820c992d2cc6.txt",
		"https://github.com/languagetool-org/languagetool.git, 01cddc5afb590b4d36cb784637a8ea8aa31d3561, languagetool-01cddc5afb590b4d36cb784637a8ea8aa31d3561.txt",
		"https://github.com/apache/hive.git, 4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6, hive-4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6.txt",
		"https://github.com/Athou/commafeed.git, 18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7, commafeed-18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7.txt",
		"https://github.com/facebook/buck.git, f26d234e8d3458f34454583c22e3bd5f4b2a5da8, buck-f26d234e8d3458f34454583c22e3bd5f4b2a5da8.txt",
		"https://github.com/nutzam/nutz.git, de7efe40dad0f4bb900c4fffa80ed377745532b3, nutz-de7efe40dad0f4bb900c4fffa80ed377745532b3.txt",
		"https://github.com/tsantalis/RefactoringMiner.git, e4c0aff02b2ed6cb53b5e48b14714c9dc0f451ad, miner-e4c0aff02b2ed6cb53b5e48b14714c9dc0f451ad.txt",
		"https://github.com/tsantalis/RefactoringMiner.git, cec58c7141e9994509268690b91f98e965d3f0b5, miner-cec58c7141e9994509268690b91f98e965d3f0b5.txt",
		"https://github.com/tsantalis/RefactoringMiner.git, 7841a00088cea73a8a6d20e63f63f1eb13f528a5, miner-7841a00088cea73a8a6d20e63f63f1eb13f528a5.txt",
		"https://github.com/tsantalis/RefactoringMiner.git, 1aab3114cdfcddf44d35c820e643c932c5433122, miner-1aab3114cdfcddf44d35c820e643c932c5433122.txt",
		"https://github.com/ta4j/ta4j.git, 364d79c94e6c1aa98bf771a0b7671001e4257838, ta4j-364d79c94e6c1aa98bf771a0b7671001e4257838.txt",
		"https://github.com/jOOQ/jOOQ.git, 58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357, jOOQ-58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357.txt",
		"https://github.com/eclipse/jgit.git, df3469f6ad81dccb314bf2d5021a3cec2b184985, jgit-df3469f6ad81dccb314bf2d5021a3cec2b184985.txt",
		"https://github.com/phishman3579/java-algorithms-implementation.git, 4ffcb5a65e6d24c58ef75a5cd7692e875619548d, phishman-4ffcb5a65e6d24c58ef75a5cd7692e875619548d.txt",
		"https://github.com/phishman3579/java-algorithms-implementation.git, f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29, phishman-f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29.txt",
		"https://github.com/spring-projects/spring-boot.git, 07766c436cb89128eac5389d33f76329b758d8df, spring-boot-07766c436cb89128eac5389d33f76329b758d8df.txt",
		"https://github.com/spring-projects/spring-framework.git, 58fbf60d2d5d28d96c351eb539a52bceffac270a, spring-framework-58fbf60d2d5d28d96c351eb539a52bceffac270a.txt",
		"https://github.com/hibernate/hibernate-orm.git, bf7607e24495af5133165ae6ed6b85feecf59148, hibernate-orm-bf7607e24495af5133165ae6ed6b85feecf59148.txt",
		"https://github.com/tsantalis/RefactoringMiner.git, 0894f346564f8b31cf836def67e952fb93a6036d, miner-0894f346564f8b31cf836def67e952fb93a6036d.txt",
		"https://github.com/eclipse-jgit/jgit.git, 7ff6eb584cf8b83f83a3b5edf897feb53dbf42c0, jgit-7ff6eb584cf8b83f83a3b5edf897feb53dbf42c0.txt",
		"https://github.com/jenkinsci/git-client-plugin.git, 6d261108e7471db380146f945bb228b5fc8c44cc, git-client-plugin-6d261108e7471db380146f945bb228b5fc8c44cc.txt",
		"https://github.com/javaparser/javaparser.git, 548fb9c5a72776ec009c5f2f92b1a4c480a05030, javaparser-548fb9c5a72776ec009c5f2f92b1a4c480a05030.txt",
		"https://github.com/apache/ant.git, 52926715b4f4f53da4b63cf660a14f357d7a9b6e, ant-52926715b4f4f53da4b63cf660a14f357d7a9b6e.txt",
		"https://github.com/apache/cassandra.git, ab51b794735bd5731357d6fd4cb92cf0059a7ad1, cassandra-ab51b794735bd5731357d6fd4cb92cf0059a7ad1.txt"
	})
	public void testExtractMethodStatementMappings(String url, String commit, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring && ref.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings9() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    if(ex.getExtractedOperation().getName().equals("getSpecifiedTokens")) {
                        UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                        if(!bodyMapper.isNested()) {
                            if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                                parentMappers.add(bodyMapper.getParentMapper());
                            }
                        }
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "cassandra-9a3fa887cfa03c082f249d1d4003d87c14ba5d24.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings29() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "025b3cc14180d0459856bc45a6cac7acce3e1265", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring && ref.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    if(ex.getSourceOperationBeforeExtraction().getName().equals("bindClass") && ex.getSourceOperationBeforeExtraction().getClassName().equals("org.hibernate.cfg.AnnotationBinder")) {
                        UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                        if(!bodyMapper.isNested()) {
                            if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                                parentMappers.add(bodyMapper.getParentMapper());
                            }
                        }
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-025b3cc14180d0459856bc45a6cac7acce3e1265.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings31() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/google/guava.git", "31fc19200207ccadc45328037d8a2a62b617c029", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "guava-31fc19200207ccadc45328037d8a2a62b617c029.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSlidedStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "48bb4cfd773ac2363019daf4b38456d91cdc1fb1", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("UMLOperationBodyMapper") && mapper.getContainer2().getName().equals("UMLOperationBodyMapper") &&
						mapper.getContainer1().getParameterTypeList().size() == 3 && mapper.getContainer2().getParameterTypeList().size() == 3 &&
						mapper.getContainer1().getParameterTypeList().get(0).getClassType().equals("UMLOperation") &&
						mapper.getContainer2().getParameterTypeList().get(0).getClassType().equals("UMLOperation")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-48bb4cfd773ac2363019daf4b38456d91cdc1fb1.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSwappedVariableDeclarationStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "2e257896661ad5e0dca521fa274cad3a0bae7a61", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			if(classDiff.getOriginalClassName().equals("org.apache.flink.runtime.io.network.partition.consumer.InputBuffersMetricsTest")) {
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-2e257896661ad5e0dca521fa274cad3a0bae7a61.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedAndExtractedStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "77ba11175b7d3a3297be5352a512e48e2526569d", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-77ba11175b7d3a3297be5352a512e48e2526569d.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithAddedMethodCall() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "6095e8477aeb633c5c647776cdeb22f7cdc5031b", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-6095e8477aeb633c5c647776cdeb22f7cdc5031b.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRenamedMethodCalls() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "GraphHopperStorage-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "GraphHopperStorage-v2.txt"));
		fileContentsBefore.put("core/src/main/java/com/graphhopper/storage/GraphHopperStorage.java", contentsV1);
		fileContentsCurrent.put("core/src/main/java/com/graphhopper/storage/GraphHopperStorage.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("inPlaceNodeRemove") && mapper.getContainer2().getName().equals("inPlaceNodeRemove")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "graphhopper-7f80425b6a0af9bdfef12c8a873676e39e0a04a6.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testForToEnhancedForMigrations() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PackStreamMessageFormatV1-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PackStreamMessageFormatV1-v2.txt"));
		fileContentsBefore.put("community/ndp/messaging-v1/src/main/java/org/neo4j/ndp/messaging/v1/PackStreamMessageFormatV1.java", contentsV1);
		fileContentsCurrent.put("community/ndp/messaging-v1/src/main/java/org/neo4j/ndp/messaging/v1/PackStreamMessageFormatV1.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("packValue") && mapper.getContainer2().getName().equals("packValue")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "neo4j-e0072aac53b3b88de787e7ca653c7e17f9499018.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMultiReplacement() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ExecutionUtil-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ExecutionUtil-v2.txt"));
		fileContentsBefore.put("platform/lang-api/src/com/intellij/execution/runners/ExecutionUtil.java", contentsV1);
		fileContentsCurrent.put("platform/lang-api/src/com/intellij/execution/runners/ExecutionUtil.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("getLiveIndicator") && mapper.getContainer2().getName().equals("getLiveIndicator")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "intellij-community-ce5f9ff96e2718e4014655f819314ac2ac4bd8bf.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAvoidSplitMethod() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ApplyCommand-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ApplyCommand-v2.txt"));
		fileContentsBefore.put("org.eclipse.jgit/src/org/eclipse/jgit/api/ApplyCommand.java", contentsV1);
		fileContentsCurrent.put("org.eclipse.jgit/src/org/eclipse/jgit/api/ApplyCommand.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("apply") && mapper.getContainer2().getName().equals("applyText")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-10ac4499115965ff10e547a0632c89873a06cf91.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAvoidSplitConditional() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ConstructorResolver-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ConstructorResolver-v2.txt"));
		fileContentsBefore.put("org.springframework.beans/src/main/java/org/springframework/beans/factory/support/ConstructorResolver.java", contentsV1);
		fileContentsCurrent.put("org.springframework.beans/src/main/java/org/springframework/beans/factory/support/ConstructorResolver.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("instantiateUsingFactoryMethod") && mapper.getContainer2().getName().equals("instantiateUsingFactoryMethod")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-14bd47551900ced88eeacf2a5f63c187ff72028c.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings2() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PredicateParser-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PredicateParser-v2.txt"));
		fileContentsBefore.put("core/src/main/java/io/undertow/predicate/PredicateParser.java", contentsV1);
		fileContentsCurrent.put("core/src/main/java/io/undertow/predicate/PredicateParser.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("parse") && mapper.getContainer2().getName().equals("parse")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "undertow-d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertStatementMappings() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "DekaBankPDFExtractorTest-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "DekaBankPDFExtractorTest-v2.txt"));
		fileContentsBefore.put("name/abuchen/portfolio/datatransfer/pdf/dekabank/DekaBankPDFExtractorTest.java", contentsV1);
		fileContentsCurrent.put("name/abuchen/portfolio/datatransfer/pdf/dekabank/DekaBankPDFExtractorTest.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("testQuartalsbericht02") && mapper.getContainer2().getName().equals("testQuartalsbericht02")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "portfolio-DekaBankPDFExtractorTest.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "841f23c73e4399df91112dd11ddca74f45ea5b37", new File(REPOS), (commitId, refactorings) -> {
            for (Refactoring ref : refactorings) {
                if(ref instanceof PushDownOperationRefactoring) {
                    PushDownOperationRefactoring ex = (PushDownOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    mapperInfo(bodyMapper, actual);
                }
            }
        });
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-841f23c73e4399df91112dd11ddca74f45ea5b37.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertStatementMappings4() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "62f44e0118539c1ed0dedf47099326f97c9d0427", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				mapperInfo(mapper, actual);
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-62f44e0118539c1ed0dedf47099326f97c9d0427.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testJUnit3MigrationMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/MrSorrow/spring-framework.git", "d5ee787e1e6653257720afe31ee3f8819cd4605c", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				mapperInfo(mapper, actual);
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-d5ee787e1e6653257720afe31ee3f8819cd4605c.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testBreakStatementMappings() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PosixParser-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PosixParser-v2.txt"));
		fileContentsBefore.put("src/main/java/org/apache/commons/cli/PosixParser.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/apache/commons/cli/PosixParser.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("burstToken") && mapper.getContainer2().getName().equals("burstToken")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commons-cli-PosixParser.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testBreakStatementMappings2() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "TypeFactory-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "TypeFactory-v2.txt"));
		fileContentsBefore.put("src/main/java/com/fasterxml/jackson/databind/type/TypeFactory.java", contentsV1);
		fileContentsCurrent.put("src/main/java/com/fasterxml/jackson/databind/type/TypeFactory.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("constructSpecializedType") && mapper.getContainer2().getName().equals("constructSpecializedType")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jackson-databind-TypeFactory.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testBreakStatementMappings3() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "HtmlTreeBuilderState-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "HtmlTreeBuilderState-v2.txt"));
		fileContentsBefore.put("src/main/java/org/jsoup/parser/HtmlTreeBuilderState.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/jsoup/parser/HtmlTreeBuilderState.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLEnumConstantDiff enumConstantDiff : classDiff.getEnumConstantDiffList()) {
				Optional<UMLAnonymousClassDiff> optional = enumConstantDiff.getAnonymousClassDiff();
				if(optional.isPresent()) {
					for(UMLOperationBodyMapper mapper : optional.get().getOperationBodyMapperList()) {
						if(mapper.getContainer1().getName().equals("process") && mapper.getContainer2().getName().equals("process")) {
							mapperInfo(mapper, actual);
							break;
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jsoup-HtmlTreeBuilderState.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDeletedCode() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "DatasetUtilities-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "DatasetUtilities-v2.txt"));
		fileContentsBefore.put("src/main/java/org/jfree/data/general/DatasetUtilities.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/jfree/data/general/DatasetUtilities.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("iterateRangeBounds") && mapper.getContainer2().getName().equals("iterateRangeBounds")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jfreechart-DatasetUtilities.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"TestStatementMappings-v1.txt, TestStatementMappings-v2.txt, src-test/org/refactoringminer/test/TestStatementMappings.java, miner-TestStatementMappings.txt",
		"FileNameCleanerTest-v1.txt, FileNameCleanerTest-v2.txt, src/test/java/org/jabref/logic/util/FileNameCleanerTest.java, jabRef-FileNameCleanerTest.txt",
		"CopyMoreActionTest-v1.txt, CopyMoreActionTest-v2.txt, jabgui/src/test/java/org/jabref/gui/edit/CopyMoreActionTest.java, jabRef-CopyMoreActionTest.txt",
		"OOPreFormatterTest-v1.txt, OOPreFormatterTest-v2.txt, jablib/src/test/java/org/jabref/logic/openoffice/style/OOPreFormatterTest.java, jabRef-OOPreFormatterTest.txt"
	})
	public void testParameterizedTestMappings(String input1, String input2, String filePath, String expectedOutput) throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + input1));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + input2));
		fileContentsBefore.put(filePath, contentsV1);
		fileContentsCurrent.put(filePath, contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<Refactoring> refactorings = modelDiff.getRefactorings();
		for(Refactoring r : refactorings) {
			if(r instanceof ParameterizeTestRefactoring) {
				ParameterizeTestRefactoring parameterizeTest = (ParameterizeTestRefactoring)r;
				UMLOperationBodyMapper mapper = parameterizeTest.getBodyMapper();
				mapperInfo(mapper, actual);
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + expectedOutput));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"URLUtilTest-v1.txt, URLUtilTest-v2.txt, jablib/src/test/java/org/jabref/logic/net/URLUtilTest.java, jabRef-URLUtilTest.txt"
	})
	public void testSplitToParameterizedTestMappings(String input1, String input2, String filePath, String expectedOutput) throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + input1));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + input2));
		fileContentsBefore.put(filePath, contentsV1);
		fileContentsCurrent.put(filePath, contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<Refactoring> refactorings = modelDiff.getRefactorings();
		for(Refactoring r : refactorings) {
			if(r instanceof SplitOperationRefactoring) {
				SplitOperationRefactoring splitTest = (SplitOperationRefactoring)r;
				for(UMLOperationBodyMapper mapper : splitTest.getMappers()) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + expectedOutput));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergeLambdas() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "JabRefFrame-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "JabRefFrame-v2.txt"));
		fileContentsBefore.put("src/main/java/org/jabref/gui/frame/JabRefFrame.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/jabref/gui/frame/JabRefFrame.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<Refactoring> refactorings = modelDiff.getRefactorings();
		for(Refactoring r : refactorings) {
			if(r instanceof MoveCodeRefactoring) {
				MoveCodeRefactoring in = (MoveCodeRefactoring)r;
				UMLOperationBodyMapper bodyMapper = in.getBodyMapper();
				mapperInfo(bodyMapper, actual);
				if(in.getLambdaMapper().isPresent()) {
					mapperInfo(in.getLambdaMapper().get(), actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jabRef-JabRefFrame-12210.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAnonymousLambdaRestructuring() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ConsistencyCheckAction-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "ConsistencyCheckAction-v2.txt"));
		fileContentsBefore.put("jabgui/src/main/java/org/jabref/gui/consistency/ConsistencyCheckAction.java", contentsV1);
		fileContentsCurrent.put("jabgui/src/main/java/org/jabref/gui/consistency/ConsistencyCheckAction.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());

		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("execute") && mapper.getContainer2().getName().equals("execute")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jabRef-ConsistencyCheckAction-13181.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/apache/camel.git, b57b72d0e85f2340cb2d55be44d2175c0caa7cc1, camel-b57b72d0e85f2340cb2d55be44d2175c0caa7cc1.txt",
		"https://github.com/dropwizard/dropwizard.git, 9086577e29aba07058619a706701b6d07592aed9, dropwizard-9086577e29aba07058619a706701b6d07592aed9.txt"
	})
	public void testParameterizedTestMappings(String url, String commit, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), (commitId, refactorings) -> {
            for (Refactoring ref : refactorings) {
                if(ref instanceof ParameterizeTestRefactoring) {
                    ParameterizeTestRefactoring parameterizeTest = (ParameterizeTestRefactoring)ref;
                    UMLOperationBodyMapper mapper = parameterizeTest.getBodyMapper();
                    mapperInfo(mapper, actual);
                }
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "7407076d3990752eb5fa4072cd036efd2f656cbc", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			if(classDiff.getNextClassName().equals("org.apache.flink.api.java.typeutils.runtime.PojoSerializer")) {
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("deserialize") && mapper.getContainer2().getName().equals("deserialize")) {
						mapperInfo(mapper, actual);
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-7407076d3990752eb5fa4072cd036efd2f656cbc.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings19() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/square/okhttp.git", "a91124b6d4e2eb1bb3c71a7a8ddff7d40b7db55a", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("synStream") && mapper.getContainer2().getName().equals("headers")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "okhttp-a91124b6d4e2eb1bb3c71a7a8ddff7d40b7db55a.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/apache/flink.git, 536675b03a5050fda9c3e1fd403818cb50dcc6ff, getUnguardedFileSystem, true, flink-536675b03a5050fda9c3e1fd403818cb50dcc6ff.txt",
		"https://github.com/spring-projects/spring-framework.git, ad2e0d45875651d9a707b514dd3966fa81a9048c, writeWithMessageConverters, true, spring-framework-ad2e0d45875651d9a707b514dd3966fa81a9048c.txt",
		"https://github.com/eclipse/jetty.project.git, 06454f64098e01b42347841211afed229d8798a0, send, true, jetty.project-06454f64098e01b42347841211afed229d8798a0.txt",
		"https://github.com/hibernate/hibernate-orm.git, 5329bba1ea724eabf5783c71e5127b8f84ad0fcc, bindClass, true, hibernate-orm-5329bba1ea724eabf5783c71e5127b8f84ad0fcc.txt",
		"https://github.com/spring-projects/spring-framework.git, 289f35da3a57bb5e491b30c7351072b4e801c519, writeWithMessageConverters, false, spring-framework-289f35da3a57bb5e491b30c7351072b4e801c519.txt",
		"https://github.com/MovingBlocks/Terasology.git, 543a9808a85619dbe5acc2373cb4fe5344442de7, cleanup, true, terasology-543a9808a85619dbe5acc2373cb4fe5344442de7.txt",
		"https://github.com/eclipse/jgit.git, 298486a7c320629de12f9506e0133686a7382b01, diff, false, jgit-298486a7c320629de12f9506e0133686a7382b01.txt",
		"https://github.com/jline/jline2.git, 1eb3b624b288a4b1a054420d3efb05b8f1d28517, drawBuffer, true, jline2-1eb3b624b288a4b1a054420d3efb05b8f1d28517.txt", // TODO fix block mappings
		"https://github.com/eclipse/jgit.git, 5b84e25fa3afe66bbfa7eb953ea0bd332c745ecd, call, true, jgit-5b84e25fa3afe66bbfa7eb953ea0bd332c745ecd.txt",
		"https://github.com/apache/commons-lang.git, 4f514d5eb3e80703012df9be190ae42d35d25bdc, formatPeriod, false, commons-lang-4f514d5eb3e80703012df9be190ae42d35d25bdc.txt",
		"https://github.com/eclipse/jgit.git, 545358577376bec8fc140a76ce0f72bf81cc0a94, call, true, jgit-545358577376bec8fc140a76ce0f72bf81cc0a94.txt",
		"https://github.com/eclipse/jetty.project.git, 9c168866ffbb349d56501d11801f0418bdee3596, doStart, true, jetty.project-9c168866ffbb349d56501d11801f0418bdee3596.txt",
		"https://github.com/eclipse/jetty.project.git, c285d6f8bbd839906e8c39d23db2f343be22c6ca, send, true, jetty.project-c285d6f8bbd839906e8c39d23db2f343be22c6ca.txt", // TODO L115-116 should be matched with R127-128
		"https://github.com/spring-framework.git, b204437cef0976f5af0e1c5290e77e266b306a51, writeWithMessageConverters, true, spring-framework-b204437cef0976f5af0e1c5290e77e266b306a51.txt",
		"https://github.com/spring-framework.git, 0a42c80c1151380f7f492ec75de5648cfe62d250, processConfigBeanDefinitions, true, spring-framework-0a42c80c1151380f7f492ec75de5648cfe62d250.txt",
		"https://github.com/javaparser/javaparser.git, de5c17c37f15a1c134f518ed2754974cc4b9aa15, apply, true, javaparser-de5c17c37f15a1c134f518ed2754974cc4b9aa15.txt",
		"https://github.com/hibernate/hibernate-orm.git, 8577a68e69d30d9e671024bf3330616000a3ec54, processElementAnnotations, true, hibernate-orm-8577a68e69d30d9e671024bf3330616000a3ec54.txt",
		"https://github.com/checkstyle/checkstyle.git, bf69cf167c9432daabc7b6e4a426fff33650a057, visitToken, true, checkstyle-bf69cf167c9432daabc7b6e4a426fff33650a057.txt",
		"https://github.com/hibernate/hibernate-orm.git, 016a02ff506b715e8217b8577594ac62b3f318ce, processElementAnnotations, true, hibernate-orm-016a02ff506b715e8217b8577594ac62b3f318ce.txt",
		"https://github.com/javaparser/javaparser.git, acdac6790f4424f8097b3aa6c888e825cac485f9, inferTypes, true, javaparser-acdac6790f4424f8097b3aa6c888e825cac485f9.txt",
		"https://github.com/apache/lucene-solr.git, 4f6469b17387fb1ee05a8304c80ff607cdce7bc1, expandRoot, true, lucene-solr-4f6469b17387fb1ee05a8304c80ff607cdce7bc1.txt",
		"https://github.com/apache/kafka.git, d171ff08a70f9fa8065e6661fcc1f3da092d7faf, testRejectMinIsrChangeWhenElrEnabled, true, kafka-d171ff08a70f9fa8065e6661fcc1f3da092d7faf.txt",
		"https://github.com/apache/flink.git, 583722e721a121fa7a6787fe5acb47949b30454a, testCreateAndReuseFiles, true, flink-583722e721a121fa7a6787fe5acb47949b30454a.txt",
		"https://github.com/tsantalis/RefactoringMiner.git, 1b15f4a76b5aaad9959866fa10dcd815934e1789, findReplacementsWithExactMatching, true, miner-1b15f4a76b5aaad9959866fa10dcd815934e1789.txt",
	})
	public void testRestructuredStatementMappings(String url, String commitId, String containerName, boolean breakOnFirstMatch, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
					mapperInfo(mapper, actual);
					if (breakOnFirstMatch)
						break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}


	@ParameterizedTest
	@CsvSource({
		"https://github.com/eclipse-jgit/jgit.git, 6658f367682932c0a77061a5aa37c06e480a0c62, jgit-6658f367682932c0a77061a5aa37c06e480a0c62.txt",
		"https://github.com/eclipse-jgit/jgit.git, 8ac65d33ed7a94f77cb066271669feebf9b882fc, jgit-8ac65d33ed7a94f77cb066271669feebf9b882fc.txt"
	})
	public void testInlinedMethodMovedToExtractedMethod(String url, String commitId, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS), (commitId1, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
                else if(ref instanceof InlineOperationRefactoring) {
                    InlineOperationRefactoring in = (InlineOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = in.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/checkstyle/checkstyle.git, 1a2c318e22a0b2b22ccc76019217c0892fe2d59b, com.puppycrawl.tools.checkstyle.Main, main, checkstyle-1a2c318e22a0b2b22ccc76019217c0892fe2d59b.txt",
		"https://github.com/hibernate/hibernate-search.git, fee1c5f90c639ec7fe30699873788b892b84e4c7, org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing.OutboxPollingAutomaticIndexingLifecycleIT, stopWhileOutboxEventsIsBeingProcessed, hibernate-search-fee1c5f90c639ec7fe30699873788b892b84e4c7.txt"
	})
	public void testMoveCodeStatementMappings(String url, String commitId, String className, String methodName, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {}

			@Override
			public void handleModelDiff(String commitId, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
				List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
				for (Refactoring ref : refactorings) {
					if(ref instanceof ExtractOperationRefactoring) {
						ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
						if(!bodyMapper.isNested()) {
							if(!parentMappers.contains(bodyMapper.getParentMapper())) {
								parentMappers.add(bodyMapper.getParentMapper());
							}
						}
						mapperInfo(bodyMapper, actual);
					}
					else if(ref instanceof InlineOperationRefactoring) {
						InlineOperationRefactoring in = (InlineOperationRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = in.getBodyMapper();
						if(!bodyMapper.isNested()) {
							if(!parentMappers.contains(bodyMapper.getParentMapper())) {
								parentMappers.add(bodyMapper.getParentMapper());
							}
						}
						mapperInfo(bodyMapper, actual);
					}
					else if(ref instanceof MoveCodeRefactoring) {
						MoveCodeRefactoring in = (MoveCodeRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = in.getBodyMapper();
						mapperInfo(bodyMapper, actual);
					}
				}
				for(UMLOperationBodyMapper parentMapper : parentMappers) {
					mapperInfo(parentMapper, actual);
				}
				//add main mapper
				UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff(className);
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals(methodName) && mapper.getContainer2().getName().equals(methodName)) {
						mapperInfo(mapper, actual);
						break;
					}
				}
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMoveCodeStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/junit-team/junit5.git", "3e3b402131a99f01480c57dd82c2e81ad6d9a4ea", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {}

			@Override
			public void handleModelDiff(String commitId, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
				List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
				for (Refactoring ref : refactorings) {
					if(ref instanceof ExtractOperationRefactoring) {
						ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
						if(!bodyMapper.isNested()) {
							if(!parentMappers.contains(bodyMapper.getParentMapper())) {
								parentMappers.add(bodyMapper.getParentMapper());
							}
						}
						mapperInfo(bodyMapper, actual);
					}
					else if(ref instanceof InlineOperationRefactoring) {
						InlineOperationRefactoring in = (InlineOperationRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = in.getBodyMapper();
						if(!bodyMapper.isNested()) {
							if(!parentMappers.contains(bodyMapper.getParentMapper())) {
								parentMappers.add(bodyMapper.getParentMapper());
							}
						}
						mapperInfo(bodyMapper, actual);
					}
					else if(ref instanceof MoveCodeRefactoring) {
						MoveCodeRefactoring in = (MoveCodeRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = in.getBodyMapper();
						mapperInfo(bodyMapper, actual);
					}
				}
				for(UMLOperationBodyMapper parentMapper : parentMappers) {
					mapperInfo(parentMapper, actual);
				}
				//add main mapper
				UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff("org.junit.jupiter.engine.descriptor.ClassTestDescriptor");
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("before") && mapper.getContainer2().getName().equals("before")) {
						mapperInfo(mapper, actual);
					}
					if(mapper.getContainer1().getName().equals("after") && mapper.getContainer2().getName().equals("after")) {
						mapperInfo(mapper, actual);
					}
				}
				classDiff = modelDiff.getUMLClassDiff("org.junit.jupiter.engine.descriptor.MethodTestDescriptor");
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("execute") && mapper.getContainer2().getName().equals("execute")) {
						mapperInfo(mapper, actual);
					}
				}
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "junit5-3e3b402131a99f01480c57dd82c2e81ad6d9a4ea.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testLogGuardStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "a959dd5034127161aafcf9c56222c7d08aa80e54", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("getNextInputSplit") && mapper.getContainer2().getName().equals("getNextInputSplit")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-a959dd5034127161aafcf9c56222c7d08aa80e54.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergeConditionals() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-framework.git", "7dd8dc62a5fa08e3cc99d2388ff62f5825151fb9", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("writeWithMessageConverters") && mapper.getContainer2().getName().equals("writeWithMessageConverters")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-7dd8dc62a5fa08e3cc99d2388ff62f5825151fb9.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSlidedStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-framework.git", "981aefc2c0d2a6fbf9c08d4d54d17923a75a2e01", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("loadBeanDefinitionsForBeanMethod") && mapper.getContainer2().getName().equals("loadBeanDefinitionsForBeanMethod")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-981aefc2c0d2a6fbf9c08d4d54d17923a75a2e01.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergedStatementMappingsToTernary() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "88138d08e731d3084d59bf14cc2fb51bdf4afbd8", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("getMinMaxNetworkBuffersPerResultPartition") && mapper.getContainer2().getName().equals("getMinMaxNetworkBuffersPerResultPartition")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-88138d08e731d3084d59bf14cc2fb51bdf4afbd8.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergedStatementMappingsMovedOutOfIfElseIfBranch() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/liferay/liferay-plugins.git", "7c7ecf4cffda166938efd0ae34830e2979c25c73", new File(REPOS), (commitId, refactorings) -> {
            List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
            for (Refactoring ref : refactorings) {
                if(ref instanceof ExtractOperationRefactoring) {
                    ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
                    UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
                    if(!bodyMapper.isNested()) {
                        if(!parentMappers.contains(bodyMapper.getParentMapper())) {
                            parentMappers.add(bodyMapper.getParentMapper());
                        }
                    }
                    mapperInfo(bodyMapper, actual);
                }
            }
            for(UMLOperationBodyMapper parentMapper : parentMappers) {
                mapperInfo(parentMapper, actual);
            }
        });

		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "liferay-plugins-7c7ecf4cffda166938efd0ae34830e2979c25c73.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergedStatementMappingsMovedOutOfIfElseIfBranch2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/jfinal/jfinal.git", "881baed894540031bd55e402933bcad28b74ca88", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("buildActionMapping") && mapper.getContainer2().getName().equals("buildActionMapping")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jfinal-881baed894540031bd55e402933bcad28b74ca88.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSplitStatementMappingsMovedInIfElseBranch() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/gradle/gradle.git", "f841d8dda2bf461f595755f85c3eba786783702d", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("runBuildOperation") && mapper.getContainer2().getName().equals("runBuildOperation")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "gradle-f841d8dda2bf461f595755f85c3eba786783702d.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}


	@ParameterizedTest
	@CsvSource({
		"https://github.com/eclipse/jgit.git, d726f0c1e02c196e2dd87de53b54338be15503f1, call, jgit-d726f0c1e02c196e2dd87de53b54338be15503f1.txt",
		"https://github.com/eclipse/jgit.git, 45e79a526c7ffebaf8e4758a6cb6b7af05716707, call, jgit-45e79a526c7ffebaf8e4758a6cb6b7af05716707.txt",
		"https://github.com/eclipse/jgit.git, 9bebb1eae78401e1d3289dc3d84006c10d10c0ef, call, jgit-9bebb1eae78401e1d3289dc3d84006c10d10c0ef.txt"
	})
	public void innerTryBlockDeleted(String url, String commitId, String containerName, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void outerTryBlockAdded() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/checkstyle/checkstyle.git", "f020066f8bdfb378df36904af3df8b5bc48858fd", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("process") && mapper.getContainer2().getName().equals("process")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "checkstyle-f020066f8bdfb378df36904af3df8b5bc48858fd.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void tryWithResourceMigration() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/commons-io.git", "4dc97b64005f0083b2facaa70f661138a4fa3fc0", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("run") && mapper.getContainer2().getName().equals("run")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commons-io-4dc97b64005f0083b2facaa70f661138a4fa3fc0.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testStreamAPIMigration() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "f70eef166e4afd92471079a75ba5828049fca500", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			if(classDiff.getOriginalClassName().equals("com.github.javaparser.ast.visitor.VoidVisitorAdapter"))
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				mapperInfo(mapper, actual);
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-f70eef166e4afd92471079a75ba5828049fca500.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testStreamToStreamAPIMigration() throws Exception {
		final List<String> actual = new ArrayList<>();
		UMLClassDiff classDiff = generateClassDiff("https://github.com/apache/drill.git", "1517a87eb1effb2aac0c75b5f5ea6abc25407ab0", new File(REPOS), "org.apache.drill.exec.planner.fragment.DistributedQueueParallelizer");
		classDiff.process();
		for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
			if(mapper.getContainer1().getName().equals("ensureOperatorMemoryWithinLimits") && mapper.getContainer2().getName().equals("ensureOperatorMemoryWithinLimits")) {
				mapperInfo(mapper, actual);
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "drill-1517a87eb1effb2aac0c75b5f5ea6abc25407ab0.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSplitVariableDeclarationStatement() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/commons-lang.git", "4d46f014fb8ee44386feb5fec52509f35d0e36ea", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("toLocale") && mapper.getContainer2().getName().equals("toLocale")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commons-lang-4d46f014fb8ee44386feb5fec52509f35d0e36ea.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedForLoopsWithVariableRenames() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/drill.git", "b2bbd9941be6b132a83d27c0ae02c935e1dec5dd", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("flattenRecords") && mapper.getContainer2().getName().equals("flattenRecords")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "drill-b2bbd9941be6b132a83d27c0ae02c935e1dec5dd.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testIsomorphicControlStructure() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "a25f53f8871fd178b6791d1194d7358b55d1ba37", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("apply") && mapper.getContainer2().getName().equals("apply")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-a25f53f8871fd178b6791d1194d7358b55d1ba37.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testZeroStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/square/okhttp.git", "084b06b48bae2b566bb1be3415b6c847d8ea3682", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("getResponse") && mapper.getContainer2().getName().equals("getResponse") && mapper.getContainer1().getClassName().equals("okhttp3.internal.huc.HttpURLConnectionImpl")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "okhttp-084b06b48bae2b566bb1be3415b6c847d8ea3682.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testWhileLoopsWithRenamedVariable() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "733780e8a158b7bc45b8b687ac353ecadc905a63", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("findObjectsToPack") && mapper.getContainer2().getName().equals("findObjectsToPack")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-733780e8a158b7bc45b8b687ac353ecadc905a63.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testForLoopsWithIdenticalComments() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "d017fb8caf6ccb3343da0062eb2c85262712772c", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("solveMethodAsUsage") && mapper.getContainer2().getName().equals("solveMethodAsUsage")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-d017fb8caf6ccb3343da0062eb2c85262712772c.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/hibernate/hibernate-search.git, 9a25c9dd3e4af54c43878d224c27106384302d25, PojoTypeManagerContainer, hibernate-search-9a25c9dd3e4af54c43878d224c27106384302d25.txt",
		"https://github.com/hibernate/hibernate-search.git, 6133c600d0fc15ce3482aaf9c3a29b4a222c98e8, HibernateOrmTypeContextContainer, hibernate-search-6133c600d0fc15ce3482aaf9c3a29b4a222c98e8.txt"
	})
	public void testConsecutiveForLoops(String url, String commitId, String containerName, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testParametersMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/camel.git", "ee55a3bc6e04fea54bd30cc1d3926020ea024661", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			if(classDiff.getOriginalClassName().equals("org.apache.camel.component.salesforce.BulkApiJobIntegrationTest")) {
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("getJobs") && mapper.getContainer2().getName().equals("getJobs")) {
						mapperInfo(mapper, actual);
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "camel-ee55a3bc6e04fea54bd30cc1d3926020ea024661.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/commons-lang.git", "5111ae7db08a70323a51a21df0bbaf46f21e072e", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			if(classDiff.getOriginalClassName().equals("org.apache.commons.lang.time.DurationFormatUtils")) {
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					mapperInfo(mapper, actual);
				}
			}
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().startsWith("test") && mapper.getContainer2().getName().startsWith("test")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commons-lang-5111ae7db08a70323a51a21df0bbaf46f21e072e.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/real-logic/Aeron.git", "35893c115ba23bd62a7036a33390420f074ce660", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().hasTestAnnotation() && mapper.getContainer2().hasTestAnnotation()) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "Aeron-35893c115ba23bd62a7036a33390420f074ce660.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/hadoop.git", "87fb97777745b2cefed6bef57490b84676d2343d", new File(REPOS));
		List<UMLClassMoveDiff> commonClassDiff = modelDiff.getClassMoveDiffList();
		for(UMLClassMoveDiff classDiff : commonClassDiff) {
			if(classDiff.getOriginalClassName().equals("org.apache.hadoop.fs.TestVectoredReadUtils")) {
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("testSortAndMerge") && mapper.getContainer2().getName().equals("testSortAndMerge")) {
						mapperInfo(mapper, actual);
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hadoop-87fb97777745b2cefed6bef57490b84676d2343d.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAvoidMultiMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/lucene-solr.git", "82eff4eb4de76ff641ddd603d9b8558a4277644d", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("scorer") && mapper.getContainer2().getName().equals("scorer") && mapper.getContainer1().getClassName().equals("org.apache.lucene.search.ConstantScoreQuery.ConstantWeight")) {
					mapperInfo(mapper, actual);
				}
				else if(mapper.getContainer1().getName().equals("rewrite") && mapper.getContainer2().getName().equals("rewrite") && mapper.getContainer1().getClassName().equals("org.apache.lucene.search.ConstantScoreQuery")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "lucene-solr-82eff4eb4de76ff641ddd603d9b8558a4277644d.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAvoidMultiMappings2() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "CheckSideEffects-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "CheckSideEffects-v2.txt"));
		fileContentsBefore.put("src/com/google/javascript/jscomp/CheckSideEffects.java", contentsV1);
		fileContentsCurrent.put("src/com/google/javascript/jscomp/CheckSideEffects.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("visit") && mapper.getContainer2().getName().equals("visit") && mapper.getContainer1().getClassName().equals("com.google.javascript.jscomp.CheckSideEffects")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jscomp-CheckSideEffects.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAvoidMultiMappings3() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "EncodedFieldSection-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "EncodedFieldSection-v2.txt"));
		fileContentsBefore.put("src/main/java/org/eclipse/jetty/http3/qpack/internal/parser/EncodedFieldSection.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/eclipse/jetty/http3/qpack/internal/parser/EncodedFieldSection.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("EncodedFieldSection") && mapper.getContainer2().getName().equals("EncodedFieldSection") && mapper.getContainer1().getClassName().equals("org.eclipse.jetty.http3.qpack.internal.parser.EncodedFieldSection")) {
					mapperInfo(mapper, actual);
					for(Refactoring r : classDiff.getRefactorings()) {
						if(r instanceof RenameOperationRefactoring) {
							RenameOperationRefactoring rename = (RenameOperationRefactoring)r;
							mapperInfo(rename.getBodyMapper(), actual);
						}
					}
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jetty.project-a95fe3bfb83f7dbbbb0fd76580c0b8b24dd3323b.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/eclipse/jgit.git, 2fbcba41e365752681f635c706d577e605d3336a, jgit-2fbcba41e365752681f635c706d577e605d3336a.txt",
		"https://github.com/eclipse/jgit.git, f5fe2dca3cb9f57891e1a4b18832fcc158d0c490, jgit-f5fe2dca3cb9f57891e1a4b18832fcc158d0c490.txt",
		"https://github.com/square/okhttp.git, 0bfd6048574d61c138fd417051ae2a1bcb44638f, okhttp-0bfd6048574d61c138fd417051ae2a1bcb44638f.txt"
	})
	public void testMergeMethod(String cloneURL, String commitId, String testResultFileName) throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI(cloneURL, commitId, new File(REPOS), (commitId1, refactorings) -> {
            for (Refactoring ref : refactorings) {
                if(ref instanceof MergeOperationRefactoring) {
                    MergeOperationRefactoring ex = (MergeOperationRefactoring)ref;
                    for(UMLOperationBodyMapper bodyMapper : ex.getMappers()) {
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRenameMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/junit-team/junit5.git", "b2ba6b95138382f25ca757a5ca2a7295bee4c3b8", new File(REPOS), (commitId, refactorings) -> {
            for (Refactoring ref : refactorings) {
                if(ref instanceof RenameOperationRefactoring) {
                    RenameOperationRefactoring rename = (RenameOperationRefactoring)ref;
                    mapperInfo(rename.getBodyMapper(), actual);
                }
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "junit5-b2ba6b95138382f25ca757a5ca2a7295bee4c3b8.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSplitMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/checkstyle/checkstyle.git", "08d6efe49d2960d9bd61bfb9cca65910f0c19b58", new File(REPOS), (commitId, refactorings) -> {
            for (Refactoring ref : refactorings) {
                if(ref instanceof SplitOperationRefactoring) {
                    SplitOperationRefactoring ex = (SplitOperationRefactoring)ref;
                    for(UMLOperationBodyMapper bodyMapper : ex.getMappers()) {
                        mapperInfo(bodyMapper, actual);
                    }
                }
            }
        });
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "checkstyle-08d6efe49d2960d9bd61bfb9cca65910f0c19b58.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testConsecutiveChangedStatements() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/junit-team/junit5.git", "48dd35c9002c80eeb666f56489785d1bf47f9aa4", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("AvailableOptions") && mapper.getContainer2().getName().equals("AvailableOptions")) {
					mapperInfo(mapper, actual);
				}
				if(mapper.getContainer1().getName().equals("addFiltersFromAnnotations") && mapper.getContainer2().getName().equals("addFiltersFromAnnotations")) {
					mapperInfo(mapper, actual);
				}
				if(mapper.getContainer1().getName().equals("convertsTagFilterOption") && mapper.getContainer2().getName().equals("convertsTagFilterOption")) {
					mapperInfo(mapper, actual);
				}
				if(mapper.getContainer1().getName().equals("applyFilters") && mapper.getContainer2().getName().equals("applyFilters")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "junit5-48dd35c9002c80eeb666f56489785d1bf47f9aa4.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDoNotConsiderAsExtractedMethod() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "FifoScheduler-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "FifoScheduler-v2.txt"));
		fileContentsBefore.put("src/main/java/org/apache/hadoop/yarn/server/resourcemanager/scheduler/fifo/FifoScheduler.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/apache/hadoop/yarn/server/resourcemanager/scheduler/fifo/FifoScheduler.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("completedContainer") && mapper.getContainer2().getName().equals("completedContainerInternal")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hadoop-FifoScheduler.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings25() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "StackResourceDefinition-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "StackResourceDefinition-v2.txt"));
		fileContentsBefore.put("server/integration/jgroups/src/main/java/org/jboss/as/clustering/jgroups/subsystem/StackResourceDefinition.java", contentsV1);
		fileContentsCurrent.put("server/integration/jgroups/src/main/java/org/infinispan/server/jgroups/subsystem/StackResourceDefinition.java", contentsV2);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		List<UMLClassMoveDiff> commonClassDiff = modelDiff.getClassMoveDiffList();
		for(UMLClassMoveDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("registerChildren") && mapper.getContainer2().getName().equals("registerChildren")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "infinispan-8f446b6ddf540e1b1fefca34dd10f45ba7256095.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	private void mapperInfo(UMLOperationBodyMapper bodyMapper, final List<String> actual) {
		actual.add(bodyMapper.toString());
		//System.out.println(bodyMapper.toString());
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			if(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)
				continue;
			String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
			actual.add(line);
			//System.out.println(line);
		}
	}
}

