package org.refactoringminer.test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import static org.refactoringminer.test.TestJavadocDiff.generateClassDiff;

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

public class TestStatementMappingsJunit4 {
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
	private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";

	@Test
	public void testMultipleInlinedMethodsToTheSameMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/square/okhttp.git", "a55e0090c999d155e4e588a34d9792a510ad8c68", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "okhttp-a55e0090c999d155e4e588a34d9792a510ad8c68.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/infinispan/infinispan.git", "043030723632627b0908dca6b24dae91d3dfd938", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "infinispan-043030723632627b0908dca6b24dae91d3dfd938.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedInlineMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/pouryafard75/TestCases.git", "e47272d6e1390b6366f577b84c58eae50f8f0a69", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "infinispan-043030723632627b0908dca6b24dae91d3dfd938-reverse.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedInlineMethodStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse-vertx/vert.x.git", "32a8c9086040fd6d6fa11a214570ee4f75a4301f", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "vertx-32a8c9086040fd6d6fa11a214570ee4f75a4301f.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedInlineMethodStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/camel.git", "ee55a3bc6e04fea54bd30cc1d3926020ea024661", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "camel-ee55a3bc6e04fea54bd30cc1d3926020ea024661-inline.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithLambdaParameters() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/pouryafard75/TestCases.git", "d01dfd14c0f8cae6ad4f78171011cd839b980e00", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "duplicatedCode-d01dfd14c0f8cae6ad4f78171011cd839b980e00.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "fbd80e76c68558ba58b62311aa1c34fb38baf53a", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("processLeaves") && mapper.getContainer2().getName().equals("processLeaves")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-fbd80e76c68558ba58b62311aa1c34fb38baf53a.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "f4ce6ce924ffbd03518c64cea9b830d04f75b849", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("apply") && mapper.getContainer2().getName().equals("apply")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-f4ce6ce924ffbd03518c64cea9b830d04f75b849.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/commons-lang.git", "50c1fdecb4ed33ec1bb0d449f294c299d5369701", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("createNumber") && mapper.getContainer2().getName().equals("createNumber")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commons-lang-50c1fdecb4ed33ec1bb0d449f294c299d5369701.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/k9mail/k-9.git", "23c49d834d3859fc76a604da32d1789d2e863303", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "k9mail-23c49d834d3859fc76a604da32d1789d2e863303.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/google/j2objc.git", "d05d92de40542e85f9f26712d976e710be82914e", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "j2objc-d05d92de40542e85f9f26712d976e710be82914e.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/skylot/jadx.git", "2d8d4164830631d3125575f055b417c5addaa22f", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jadx-2d8d4164830631d3125575f055b417c5addaa22f.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings4() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/facebook/buck.git", "7e104c3ed4b80ec8e9b72356396f879d1067cc40", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "buck-7e104c3ed4b80ec8e9b72356396f879d1067cc40.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings5() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/k9mail/k-9.git", "9d44f0e06232661259681d64002dd53c7c43099d", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "k9mail-9d44f0e06232661259681d64002dd53c7c43099d.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings6() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "a1e8d7cb0dcb4bd58fc5d210031bd0fb28196034", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-a1e8d7cb0dcb4bd58fc5d210031bd0fb28196034.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings7() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/junit-team/junit5.git", "5b4c642be9b1d09f9b9cebad7dd55fa40aae78bc", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "junit5-5b4c642be9b1d09f9b9cebad7dd55fa40aae78bc.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappingsWithStreamsMigration() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "FetchAndMergeEntry-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "FetchAndMergeEntry-v2.txt"));
		fileContentsBefore.put("src/main/java/org/jabref/gui/mergeentries/FetchAndMergeEntry.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/jabref/gui/mergeentries/FetchAndMergeEntry.java", contentsV2);
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
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jabref-12025.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappingsWithStreamsMigration2() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PdfMergeMetadataImporter-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "PdfMergeMetadataImporter-v2.txt"));
		fileContentsBefore.put("src/main/java/org/jabref/logic/importer/fileformat/PdfMergeMetadataImporter.java", contentsV1);
		fileContentsCurrent.put("src/main/java/org/jabref/logic/importer/fileformat/PdfMergeMetadataImporter.java", contentsV2);
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
		}
		for(UMLOperationBodyMapper parentMapper : parentMappers) {
			mapperInfo(parentMapper, actual);
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jabref-12310.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappingsWithIntermediateDelegate() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "447005f5c62ad6236aad9116e932f13c4d449546", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-447005f5c62ad6236aad9116e932f13c4d449546.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/phishman3579/java-algorithms-implementation.git", "ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "phishman-ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithZeroIdenticalStatements() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/deeplearning4j/deeplearning4j.git", "91cdfa1ffd937a4cb01cdc0052874ef7831955e2", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "deeplearning4j-91cdfa1ffd937a4cb01cdc0052874ef7831955e2.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "68319df7c453a52778d7853b59d5a2bfe5ec5065", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-68319df7c453a52778d7853b59d5a2bfe5ec5065.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "acf37d016bfff710ce5de4f608299385cfb586c6", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-acf37d016bfff710ce5de4f608299385cfb586c6.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithTwoLevelOptimization() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/Alluxio/alluxio.git", "9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "alluxio-9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedTryFinallyBlockBetweenOriginalAndExtractedMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/JoanZapata/android-iconify.git", "eb500cca282e39d01a9882e1d0a83186da6d1a26", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "android-iconify-eb500cca282e39d01a9882e1d0a83186da6d1a26.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedAndNestedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-boot.git", "becced5f0b7bac8200df7a5706b568687b517b90", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-boot-becced5f0b7bac8200df7a5706b568687b517b90.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithSingleCallSite() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/thymeleaf/thymeleaf.git", "378ba37750a9cb1b19a6db434dfa59308f721ea6", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "thymeleaf-378ba37750a9cb1b19a6db434dfa59308f721ea6.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "2d3f5e219af9d1ba916f1dc21a6169a41a254632", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-2d3f5e219af9d1ba916f1dc21a6169a41a254632.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/checkstyle/checkstyle.git", "ab2f93f9bf61816d84154e636d32c81c05854e24", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "checkstyle-ab2f93f9bf61816d84154e636d32c81c05854e24.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings4() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/hive.git", "102b23b16bf26cbf439009b4b95542490a082710", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hive-102b23b16bf26cbf439009b4b95542490a082710.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings5() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/osmandapp/Osmand.git", "c45b9e6615181b7d8f4d7b5b1cc141169081c02c", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "OsmAnd-c45b9e6615181b7d8f4d7b5b1cc141169081c02c.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings6() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-boot.git", "20d39f7af2165c67d5221f556f58820c992d2cc6", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-boot-20d39f7af2165c67d5221f556f58820c992d2cc6.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings7() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "languagetool-01cddc5afb590b4d36cb784637a8ea8aa31d3561.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings8() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/hive.git", "4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hive-4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings9() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "cassandra-9a3fa887cfa03c082f249d1d4003d87c14ba5d24.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings10() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/Athou/commafeed.git", "18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commafeed-18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings11() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/facebook/buck.git", "f26d234e8d3458f34454583c22e3bd5f4b2a5da8", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "buck-f26d234e8d3458f34454583c22e3bd5f4b2a5da8.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings12() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/nutzam/nutz.git", "de7efe40dad0f4bb900c4fffa80ed377745532b3", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "nutz-de7efe40dad0f4bb900c4fffa80ed377745532b3.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings13() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "e4c0aff02b2ed6cb53b5e48b14714c9dc0f451ad", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-e4c0aff02b2ed6cb53b5e48b14714c9dc0f451ad.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings14() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "cec58c7141e9994509268690b91f98e965d3f0b5", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-cec58c7141e9994509268690b91f98e965d3f0b5.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings15() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "7841a00088cea73a8a6d20e63f63f1eb13f528a5", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-7841a00088cea73a8a6d20e63f63f1eb13f528a5.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings16() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "1aab3114cdfcddf44d35c820e643c932c5433122", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-1aab3114cdfcddf44d35c820e643c932c5433122.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings17() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/ta4j/ta4j.git", "364d79c94e6c1aa98bf771a0b7671001e4257838", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "ta4j-364d79c94e6c1aa98bf771a0b7671001e4257838.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings18() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/jOOQ/jOOQ.git", "58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jOOQ-58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings19() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "df3469f6ad81dccb314bf2d5021a3cec2b184985", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-df3469f6ad81dccb314bf2d5021a3cec2b184985.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings20() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/phishman3579/java-algorithms-implementation.git", "4ffcb5a65e6d24c58ef75a5cd7692e875619548d", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "phishman-4ffcb5a65e6d24c58ef75a5cd7692e875619548d.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings21() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/phishman3579/java-algorithms-implementation.git", "f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "phishman-f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings22() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-boot.git", "07766c436cb89128eac5389d33f76329b758d8df", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-boot-07766c436cb89128eac5389d33f76329b758d8df.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings23() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-framework.git", "58fbf60d2d5d28d96c351eb539a52bceffac270a", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-58fbf60d2d5d28d96c351eb539a52bceffac270a.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings24() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "bf7607e24495af5133165ae6ed6b85feecf59148", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-bf7607e24495af5133165ae6ed6b85feecf59148.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings25() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "0894f346564f8b31cf836def67e952fb93a6036d", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-0894f346564f8b31cf836def67e952fb93a6036d.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings26() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse-jgit/jgit.git", "7ff6eb584cf8b83f83a3b5edf897feb53dbf42c0", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-7ff6eb584cf8b83f83a3b5edf897feb53dbf42c0.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings27() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/jenkinsci/git-client-plugin.git", "6d261108e7471db380146f945bb228b5fc8c44cc", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "git-client-plugin-6d261108e7471db380146f945bb228b5fc8c44cc.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings28() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "548fb9c5a72776ec009c5f2f92b1a4c480a05030", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-548fb9c5a72776ec009c5f2f92b1a4c480a05030.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings29() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "025b3cc14180d0459856bc45a6cac7acce3e1265", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-025b3cc14180d0459856bc45a6cac7acce3e1265.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings30() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/ant.git", "52926715b4f4f53da4b63cf660a14f357d7a9b6e", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "ant-52926715b4f4f53da4b63cf660a14f357d7a9b6e.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings31() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/google/guava.git", "31fc19200207ccadc45328037d8a2a62b617c029", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "guava-31fc19200207ccadc45328037d8a2a62b617c029.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testExtractMethodStatementMappings32() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/cassandra.git", "ab51b794735bd5731357d6fd4cb92cf0059a7ad1", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "cassandra-ab51b794735bd5731357d6fd4cb92cf0059a7ad1.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedAndExtractedStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "77ba11175b7d3a3297be5352a512e48e2526569d", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-77ba11175b7d3a3297be5352a512e48e2526569d.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithAddedMethodCall() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/tsantalis/RefactoringMiner.git", "6095e8477aeb633c5c647776cdeb22f7cdc5031b", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-6095e8477aeb633c5c647776cdeb22f7cdc5031b.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "536675b03a5050fda9c3e1fd403818cb50dcc6ff", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("getUnguardedFileSystem") && mapper.getContainer2().getName().equals("getUnguardedFileSystem")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-536675b03a5050fda9c3e1fd403818cb50dcc6ff.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "583722e721a121fa7a6787fe5acb47949b30454a", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("testCreateAndReuseFiles") && mapper.getContainer2().getName().equals("testCreateAndReuseFiles")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-583722e721a121fa7a6787fe5acb47949b30454a.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertStatementMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/flink.git", "841f23c73e4399df91112dd11ddca74f45ea5b37", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof PushDownOperationRefactoring) {
						PushDownOperationRefactoring ex = (PushDownOperationRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
						mapperInfo(bodyMapper, actual);
					}
				}
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "flink-841f23c73e4399df91112dd11ddca74f45ea5b37.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testParameterizedTestMappings() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "TestStatementMappings-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "TestStatementMappings-v2.txt"));
		fileContentsBefore.put("src-test/org/refactoringminer/test/TestStatementMappings.java", contentsV1);
		fileContentsCurrent.put("src-test/org/refactoringminer/test/TestStatementMappings.java", contentsV2);
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
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "miner-TestStatementMappings.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testParameterizedTestMappings4() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + "FileNameCleanerTest-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + "FileNameCleanerTest-v2.txt"));
		fileContentsBefore.put("src/test/java/org/jabref/logic/util/FileNameCleanerTest.java", contentsV1);
		fileContentsCurrent.put("src/test/java/org/jabref/logic/util/FileNameCleanerTest.java", contentsV2);
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
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jabRef-FileNameCleanerTest.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testParameterizedTestMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/apache/camel.git", "b57b72d0e85f2340cb2d55be44d2175c0caa7cc1", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof ParameterizeTestRefactoring) {
						ParameterizeTestRefactoring parameterizeTest = (ParameterizeTestRefactoring)ref;
						UMLOperationBodyMapper mapper = parameterizeTest.getBodyMapper();
						mapperInfo(mapper, actual);
					}
				}
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "camel-b57b72d0e85f2340cb2d55be44d2175c0caa7cc1.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testParameterizedTestMappings3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/dropwizard/dropwizard.git", "9086577e29aba07058619a706701b6d07592aed9", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof ParameterizeTestRefactoring) {
						ParameterizeTestRefactoring parameterizeTest = (ParameterizeTestRefactoring)ref;
						UMLOperationBodyMapper mapper = parameterizeTest.getBodyMapper();
						mapperInfo(mapper, actual);
					}
				}
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "dropwizard-9086577e29aba07058619a706701b6d07592aed9.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings4() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-framework.git", "ad2e0d45875651d9a707b514dd3966fa81a9048c", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("writeWithMessageConverters") && mapper.getContainer2().getName().equals("writeWithMessageConverters")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-ad2e0d45875651d9a707b514dd3966fa81a9048c.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings5() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jetty.project.git", "06454f64098e01b42347841211afed229d8798a0", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("send") && mapper.getContainer2().getName().equals("send")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jetty.project-06454f64098e01b42347841211afed229d8798a0.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings6() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "5329bba1ea724eabf5783c71e5127b8f84ad0fcc", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("bindClass") && mapper.getContainer2().getName().equals("bindClass")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-5329bba1ea724eabf5783c71e5127b8f84ad0fcc.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings7() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-framework.git", "289f35da3a57bb5e491b30c7351072b4e801c519", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("writeWithMessageConverters") && mapper.getContainer2().getName().equals("writeWithMessageConverters")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-289f35da3a57bb5e491b30c7351072b4e801c519.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings8() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/MovingBlocks/Terasology.git", "543a9808a85619dbe5acc2373cb4fe5344442de7", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("cleanup") && mapper.getContainer2().getName().equals("cleanup")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "terasology-543a9808a85619dbe5acc2373cb4fe5344442de7.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings9() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "298486a7c320629de12f9506e0133686a7382b01", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("diff") && mapper.getContainer2().getName().equals("diff")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-298486a7c320629de12f9506e0133686a7382b01.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings10() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/jline/jline2.git", "1eb3b624b288a4b1a054420d3efb05b8f1d28517", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("drawBuffer") && mapper.getContainer2().getName().equals("drawBuffer")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jline2-1eb3b624b288a4b1a054420d3efb05b8f1d28517.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings11() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "5b84e25fa3afe66bbfa7eb953ea0bd332c745ecd", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-5b84e25fa3afe66bbfa7eb953ea0bd332c745ecd.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings12() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/commons-lang.git", "4f514d5eb3e80703012df9be190ae42d35d25bdc", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("formatPeriod") && mapper.getContainer2().getName().equals("formatPeriod")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "commons-lang-4f514d5eb3e80703012df9be190ae42d35d25bdc.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings13() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "545358577376bec8fc140a76ce0f72bf81cc0a94", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-545358577376bec8fc140a76ce0f72bf81cc0a94.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings14() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jetty.project.git", "9c168866ffbb349d56501d11801f0418bdee3596", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("doStart") && mapper.getContainer2().getName().equals("doStart")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jetty.project-9c168866ffbb349d56501d11801f0418bdee3596.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings15() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jetty.project.git", "c285d6f8bbd839906e8c39d23db2f343be22c6ca", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("send") && mapper.getContainer2().getName().equals("send")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		// TODO L115-116 should be matched with R127-128
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jetty.project-c285d6f8bbd839906e8c39d23db2f343be22c6ca.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings16() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-framework.git", "b204437cef0976f5af0e1c5290e77e266b306a51", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("writeWithMessageConverters") && mapper.getContainer2().getName().equals("writeWithMessageConverters")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-b204437cef0976f5af0e1c5290e77e266b306a51.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings17() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-framework.git", "0a42c80c1151380f7f492ec75de5648cfe62d250", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("processConfigBeanDefinitions") && mapper.getContainer2().getName().equals("processConfigBeanDefinitions")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "spring-framework-0a42c80c1151380f7f492ec75de5648cfe62d250.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings18() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "de5c17c37f15a1c134f518ed2754974cc4b9aa15", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("apply") && mapper.getContainer2().getName().equals("apply")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-de5c17c37f15a1c134f518ed2754974cc4b9aa15.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings20() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "8577a68e69d30d9e671024bf3330616000a3ec54", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("processElementAnnotations") && mapper.getContainer2().getName().equals("processElementAnnotations")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-8577a68e69d30d9e671024bf3330616000a3ec54.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings21() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/checkstyle/checkstyle.git", "bf69cf167c9432daabc7b6e4a426fff33650a057", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("visitToken") && mapper.getContainer2().getName().equals("visitToken")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "checkstyle-bf69cf167c9432daabc7b6e4a426fff33650a057.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings22() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-orm.git", "016a02ff506b715e8217b8577594ac62b3f318ce", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("processElementAnnotations") && mapper.getContainer2().getName().equals("processElementAnnotations")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-orm-016a02ff506b715e8217b8577594ac62b3f318ce.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings23() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/javaparser/javaparser.git", "acdac6790f4424f8097b3aa6c888e825cac485f9", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("inferTypes") && mapper.getContainer2().getName().equals("inferTypes")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "javaparser-acdac6790f4424f8097b3aa6c888e825cac485f9.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRestructuredStatementMappings24() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/kafka.git", "d171ff08a70f9fa8065e6661fcc1f3da092d7faf", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("testRejectMinIsrChangeWhenElrEnabled") && mapper.getContainer2().getName().equals("testRejectMinIsrChangeWhenElrEnabled")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "kafka-d171ff08a70f9fa8065e6661fcc1f3da092d7faf.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testLambdaStatementMappingsWithExtractedVariables() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/apache/lucene-solr.git", "4f6469b17387fb1ee05a8304c80ff607cdce7bc1", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("expandRoot") && mapper.getContainer2().getName().equals("expandRoot")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "lucene-solr-4f6469b17387fb1ee05a8304c80ff607cdce7bc1.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testInlinedMethodMovedToExtractedMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "6658f367682932c0a77061a5aa37c06e480a0c62", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-6658f367682932c0a77061a5aa37c06e480a0c62.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testInlinedMethodMovedToExtractedMethod2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse-jgit/jgit.git", "8ac65d33ed7a94f77cb066271669feebf9b882fc", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-8ac65d33ed7a94f77cb066271669feebf9b882fc.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMoveCodeInParentMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-search.git", "fee1c5f90c639ec7fe30699873788b892b84e4c7", new File(REPOS), new RefactoringHandler() {
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
				UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff("org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing.OutboxPollingAutomaticIndexingLifecycleIT");
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("stopWhileOutboxEventsIsBeingProcessed") && mapper.getContainer2().getName().equals("stopWhileOutboxEventsIsBeingProcessed")) {
						mapperInfo(mapper, actual);
						break;
					}
				}
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-search-fee1c5f90c639ec7fe30699873788b892b84e4c7.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMoveCodeStatementMappings() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/checkstyle/checkstyle.git", "1a2c318e22a0b2b22ccc76019217c0892fe2d59b", new File(REPOS), new RefactoringHandler() {
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
				UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff("com.puppycrawl.tools.checkstyle.Main");
				for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
					if(mapper.getContainer1().getName().equals("main") && mapper.getContainer2().getName().equals("main")) {
						mapperInfo(mapper, actual);
						break;
					}
				}
			}
		});
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "checkstyle-1a2c318e22a0b2b22ccc76019217c0892fe2d59b.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMoveCodeStatementMappings2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/junit-team/junit5.git", "3e3b402131a99f01480c57dd82c2e81ad6d9a4ea", new File(REPOS), new RefactoringHandler() {
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergedStatementMappingsMovedOutOfIfElseIfBranch() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/liferay/liferay-plugins.git", "7c7ecf4cffda166938efd0ae34830e2979c25c73", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
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
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "liferay-plugins-7c7ecf4cffda166938efd0ae34830e2979c25c73.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void innerTryBlockDeleted() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "d726f0c1e02c196e2dd87de53b54338be15503f1", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-d726f0c1e02c196e2dd87de53b54338be15503f1.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void innerTryBlockDeleted2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "45e79a526c7ffebaf8e4758a6cb6b7af05716707", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-45e79a526c7ffebaf8e4758a6cb6b7af05716707.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void innerTryBlockDeleted3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "9bebb1eae78401e1d3289dc3d84006c10d10c0ef", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
					mapperInfo(mapper, actual);
					break;
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-9bebb1eae78401e1d3289dc3d84006c10d10c0ef.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testConsecutiveForLoops() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-search.git", "9a25c9dd3e4af54c43878d224c27106384302d25", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("PojoTypeManagerContainer") && mapper.getContainer2().getName().equals("PojoTypeManagerContainer")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-search-9a25c9dd3e4af54c43878d224c27106384302d25.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testConsecutiveForLoops2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/hibernate/hibernate-search.git", "6133c600d0fc15ce3482aaf9c3a29b4a222c98e8", new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals("HibernateOrmTypeContextContainer") && mapper.getContainer2().getName().equals("HibernateOrmTypeContextContainer")) {
					mapperInfo(mapper, actual);
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "hibernate-search-6133c600d0fc15ce3482aaf9c3a29b4a222c98e8.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergeMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse/jgit.git", "2fbcba41e365752681f635c706d577e605d3336a", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof MergeOperationRefactoring) {
						MergeOperationRefactoring ex = (MergeOperationRefactoring)ref;
						for(UMLOperationBodyMapper bodyMapper : ex.getMappers()) {
							mapperInfo(bodyMapper, actual);
						}
					}
				}
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-2fbcba41e365752681f635c706d577e605d3336a.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergeMethod2() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/eclipse-jgit/jgit.git", "f5fe2dca3cb9f57891e1a4b18832fcc158d0c490", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof MergeOperationRefactoring) {
						MergeOperationRefactoring ex = (MergeOperationRefactoring)ref;
						for(UMLOperationBodyMapper bodyMapper : ex.getMappers()) {
							mapperInfo(bodyMapper, actual);
						}
					}
				}
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "jgit-f5fe2dca3cb9f57891e1a4b18832fcc158d0c490.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergeMethod3() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/square/okhttp.git", "0bfd6048574d61c138fd417051ae2a1bcb44638f", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof MergeOperationRefactoring) {
						MergeOperationRefactoring ex = (MergeOperationRefactoring)ref;
						for(UMLOperationBodyMapper bodyMapper : ex.getMappers()) {
							mapperInfo(bodyMapper, actual);
						}
					}
				}
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "okhttp-0bfd6048574d61c138fd417051ae2a1bcb44638f.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSplitMethod() throws Exception {
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		final List<String> actual = new ArrayList<>();
		miner.detectAtCommitWithGitHubAPI("https://github.com/checkstyle/checkstyle.git", "08d6efe49d2960d9bd61bfb9cca65910f0c19b58", new File(REPOS), new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof SplitOperationRefactoring) {
						SplitOperationRefactoring ex = (SplitOperationRefactoring)ref;
						for(UMLOperationBodyMapper bodyMapper : ex.getMappers()) {
							mapperInfo(bodyMapper, actual);
						}
					}
				}
			}
		});
		
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + "checkstyle-08d6efe49d2960d9bd61bfb9cca65910f0c19b58.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	private void mapperInfo(UMLOperationBodyMapper bodyMapper, final List<String> actual) {
		actual.add(bodyMapper.toString());
		System.out.println(bodyMapper.toString());
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			if(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)
				continue;
			String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
			actual.add(line);
			System.out.println(line);
		}
	}
}
