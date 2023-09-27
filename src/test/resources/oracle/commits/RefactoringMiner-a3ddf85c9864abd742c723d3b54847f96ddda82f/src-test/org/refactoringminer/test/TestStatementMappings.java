package org.refactoringminer.test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class TestStatementMappings {
	private static final String REPOS = "tmp1";
	private GitService gitService = new GitServiceImpl();

	@Test
	public void testNestedInlineMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/TestCases",
				"https://github.com/pouryafard75/TestCases.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "e47272d6e1390b6366f577b84c58eae50f8f0a69", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/infinispan-043030723632627b0908dca6b24dae91d3dfd938-reverse.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedInlineMethodStatementMappings2() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/vert.x",
				"https://github.com/eclipse-vertx/vert.x.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "32a8c9086040fd6d6fa11a214570ee4f75a4301f", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/vertx-32a8c9086040fd6d6fa11a214570ee4f75a4301f.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithLambdaParameters() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/TestCases",
				"https://github.com/pouryafard75/TestCases.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "d01dfd14c0f8cae6ad4f78171011cd839b980e00", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/duplicatedCode-d01dfd14c0f8cae6ad4f78171011cd839b980e00.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				".",
				"https://github.com/tsantalis/RefactoringMiner.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "fbd80e76c68558ba58b62311aa1c34fb38baf53a";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("processLeaves") && mapper.getContainer2().getName().equals("processLeaves")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/miner-fbd80e76c68558ba58b62311aa1c34fb38baf53a.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings2() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/javaparser",
				"https://github.com/javaparser/javaparser.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "f4ce6ce924ffbd03518c64cea9b830d04f75b849";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("apply") && mapper.getContainer2().getName().equals("apply")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/javaparser-f4ce6ce924ffbd03518c64cea9b830d04f75b849.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings3() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/commons-lang",
				"https://github.com/apache/commons-lang.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "50c1fdecb4ed33ec1bb0d449f294c299d5369701";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("createNumber") && mapper.getContainer2().getName().equals("createNumber")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/commons-lang-50c1fdecb4ed33ec1bb0d449f294c299d5369701.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNonIsomorphicControlStructureStatementMappings() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/flink",
				"https://github.com/apache/flink.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "e0a4ee07084bc6ab56a20fbc4a18863462da93eb";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("getNextInputSplit") && mapper.getContainer2().getName().equals("getNextInputSplit")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/flink-e0a4ee07084bc6ab56a20fbc4a18863462da93eb.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/infinispan",
				"https://github.com/infinispan/infinispan.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "043030723632627b0908dca6b24dae91d3dfd938", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/infinispan-043030723632627b0908dca6b24dae91d3dfd938.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@Test
	public void testNestedExtractMethodStatementMappings2() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/j2objc",
				"https://github.com/google/j2objc.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "d05d92de40542e85f9f26712d976e710be82914e", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/j2objc-d05d92de40542e85f9f26712d976e710be82914e.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings3() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/jadx",
				"https://github.com/skylot/jadx.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "2d8d4164830631d3125575f055b417c5addaa22f", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jadx-2d8d4164830631d3125575f055b417c5addaa22f.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappings4() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/buck",
				"https://github.com/facebook/buck.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "7e104c3ed4b80ec8e9b72356396f879d1067cc40", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/buck-7e104c3ed4b80ec8e9b72356396f879d1067cc40.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedExtractMethodStatementMappingsWithIntermediateDelegate() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				".",
				"https://github.com/tsantalis/RefactoringMiner.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "447005f5c62ad6236aad9116e932f13c4d449546", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/miner-447005f5c62ad6236aad9116e932f13c4d449546.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/java-algorithms-implementation",
				"https://github.com/phishman3579/java-algorithms-implementation.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/phishman-ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithZeroIdenticalStatements() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/deeplearning4j",
				"https://github.com/deeplearning4j/deeplearning4j.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "91cdfa1ffd937a4cb01cdc0052874ef7831955e2", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/deeplearning4j-91cdfa1ffd937a4cb01cdc0052874ef7831955e2.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithTwoLevelOptimization() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/alluxio",
				"https://github.com/Alluxio/alluxio.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/alluxio-9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedTryFinallyBlockBetweenOriginalAndExtractedMethod() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/android-iconify",
				"https://github.com/JoanZapata/android-iconify.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "eb500cca282e39d01a9882e1d0a83186da6d1a26", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/android-iconify-eb500cca282e39d01a9882e1d0a83186da6d1a26.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedAndNestedExtractMethodStatementMappings() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/spring-boot",
				"https://github.com/spring-projects/spring-boot.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "becced5f0b7bac8200df7a5706b568687b517b90", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/spring-boot-becced5f0b7bac8200df7a5706b568687b517b90.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithSingleCallSite() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/thymeleaf",
				"https://github.com/thymeleaf/thymeleaf.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "378ba37750a9cb1b19a6db434dfa59308f721ea6", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/thymeleaf-378ba37750a9cb1b19a6db434dfa59308f721ea6.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({"https://github.com/k9mail/k-9.git, 23c49d834d3859fc76a604da32d1789d2e863303, /k-9, k9mail-23c49d834d3859fc76a604da32d1789d2e863303.txt",
			"https://github.com/javaparser/javaparser.git, 2d3f5e219af9d1ba916f1dc21a6169a41a254632, /javaparser, javaparser-2d3f5e219af9d1ba916f1dc21a6169a41a254632.txt",
			"https://github.com/checkstyle/checkstyle.git, ab2f93f9bf61816d84154e636d32c81c05854e24, /checkstyle, checkstyle-ab2f93f9bf61816d84154e636d32c81c05854e24.txt",
			"https://github.com/apache/hive.git, 102b23b16bf26cbf439009b4b95542490a082710, /hive, hive-102b23b16bf26cbf439009b4b95542490a082710.txt",
			"https://github.com/osmandapp/OsmAnd.git, c45b9e6615181b7d8f4d7b5b1cc141169081c02c, /OsmAnd, OsmAnd-c45b9e6615181b7d8f4d7b5b1cc141169081c02c.txt",
			"https://github.com/spring-projects/spring-boot.git, 20d39f7af2165c67d5221f556f58820c992d2cc6, /spring-boot, spring-boot-20d39f7af2165c67d5221f556f58820c992d2cc6.txt",
			"https://github.com/languagetool-org/languagetool.git, 01cddc5afb590b4d36cb784637a8ea8aa31d3561, /languagetool, languagetool-01cddc5afb590b4d36cb784637a8ea8aa31d3561.txt",
			"https://github.com/apache/hive.git, 4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6, /hive, hive-4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6.txt",
			"https://github.com/Athou/commafeed.git, 18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7, /commafeed, commafeed-18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7.txt",
			"https://github.com/facebook/buck.git, f26d234e8d3458f34454583c22e3bd5f4b2a5da8, /buck, buck-f26d234e8d3458f34454583c22e3bd5f4b2a5da8.txt",
			"https://github.com/nutzam/nutz.git, de7efe40dad0f4bb900c4fffa80ed377745532b3, /nutz, nutz-de7efe40dad0f4bb900c4fffa80ed377745532b3.txt"})
	public void testExtractMethodStatementMappings(String url, String commit, String folderName, String testResultFileName) throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + folderName,
				url);

		final List<String> actual = new ArrayList<>();

		miner.detectAtCommit(repo, commit, new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/" + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@Test
	public void testExtractMethodStatementMappings9() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/cassandra",
				"https://github.com/apache/cassandra.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/cassandra-9a3fa887cfa03c082f249d1d4003d87c14ba5d24.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@Test
	public void testSlidedStatementMappings() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				".",
				"https://github.com/tsantalis/RefactoringMiner.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "48bb4cfd773ac2363019daf4b38456d91cdc1fb1";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
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
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/miner-48bb4cfd773ac2363019daf4b38456d91cdc1fb1.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedAndExtractedStatementMappings() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				".",
				"https://github.com/tsantalis/RefactoringMiner.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "77ba11175b7d3a3297be5352a512e48e2526569d", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/miner-77ba11175b7d3a3297be5352a512e48e2526569d.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testDuplicatedExtractMethodStatementMappingsWithAddedMethodCall() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				".",
				"https://github.com/tsantalis/RefactoringMiner.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "6095e8477aeb633c5c647776cdeb22f7cdc5031b", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/miner-6095e8477aeb633c5c647776cdeb22f7cdc5031b.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testRenamedMethodCalls() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/GraphHopperStorage-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/GraphHopperStorage-v2.txt"));
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/graphhopper-7f80425b6a0af9bdfef12c8a873676e39e0a04a6.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testForToEnhancedForMigrations() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/PackStreamMessageFormatV1-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/PackStreamMessageFormatV1-v2.txt"));
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/neo4j-e0072aac53b3b88de787e7ca653c7e17f9499018.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMultiReplacement() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/ExecutionUtil-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/ExecutionUtil-v2.txt"));
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/intellij-community-ce5f9ff96e2718e4014655f819314ac2ac4bd8bf.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}


	@Test
	public void testRestructuredStatementMappings2() throws Exception {
		final List<String> actual = new ArrayList<>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		String contentsV1 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/PredicateParser-v1.txt"));
		String contentsV2 = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/src-test/Data/PredicateParser-v2.txt"));
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/undertow-d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@Test
	public void testRestructuredStatementMappings3() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/flink",
				"https://github.com/apache/flink.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "7407076d3990752eb5fa4072cd036efd2f656cbc";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
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
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/flink-7407076d3990752eb5fa4072cd036efd2f656cbc.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@ParameterizedTest
	@CsvSource({
			"/flink, https://github.com/apache/flink.git, 536675b03a5050fda9c3e1fd403818cb50dcc6ff, getUnguardedFileSystem, true, flink-536675b03a5050fda9c3e1fd403818cb50dcc6ff.txt",
			"/spring-framework, https://github.com/spring-projects/spring-framework.git, ad2e0d45875651d9a707b514dd3966fa81a9048c, writeWithMessageConverters, true, spring-framework-ad2e0d45875651d9a707b514dd3966fa81a9048c.txt",
			"/jetty.project, https://github.com/eclipse/jetty.project.git, 06454f64098e01b42347841211afed229d8798a0, send, true, jetty.project-06454f64098e01b42347841211afed229d8798a0.txt",
			"/hibernate-orm, https://github.com/hibernate/hibernate-orm.git, 5329bba1ea724eabf5783c71e5127b8f84ad0fcc, bindClass, true, hibernate-orm-5329bba1ea724eabf5783c71e5127b8f84ad0fcc.txt",
			"/spring-framework, https://github.com/spring-projects/spring-framework.git, 289f35da3a57bb5e491b30c7351072b4e801c519, writeWithMessageConverters, false, spring-framework-289f35da3a57bb5e491b30c7351072b4e801c519.txt",
			"/Terasology, https://github.com/MovingBlocks/Terasology.git, 543a9808a85619dbe5acc2373cb4fe5344442de7, cleanup, true, terasology-543a9808a85619dbe5acc2373cb4fe5344442de7.txt",
			"/jgit, https://github.com/eclipse/jgit.git, 298486a7c320629de12f9506e0133686a7382b01, diff, false, jgit-298486a7c320629de12f9506e0133686a7382b01.txt",
			"/jline2, https://github.com/jline/jline2.git, 1eb3b624b288a4b1a054420d3efb05b8f1d28517, drawBuffer, true, jline2-1eb3b624b288a4b1a054420d3efb05b8f1d28517.txt", // TODO fix block mappings
			"/jgit, https://github.com/eclipse/jgit.git, 5b84e25fa3afe66bbfa7eb953ea0bd332c745ecd, call, true, jgit-5b84e25fa3afe66bbfa7eb953ea0bd332c745ecd.txt",
			"/commons-lang, https://github.com/apache/commons-lang.git, 4f514d5eb3e80703012df9be190ae42d35d25bdc, formatPeriod, false, commons-lang-4f514d5eb3e80703012df9be190ae42d35d25bdc.txt"
	})
	public void testRestructuredStatementMappings(String folderName, String url, String commitId, String containerName, boolean breakOnFirstMatch, String testResultFileName) throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + folderName,
				url);

		final List<String> actual = new ArrayList<>();
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
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
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/" + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}


	@Test
	public void testInlinedMethodMovedToExtractedMethod() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/jgit",
				"https://github.com/eclipse/jgit.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "6658f367682932c0a77061a5aa37c06e480a0c62", new RefactoringHandler() {
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jgit-6658f367682932c0a77061a5aa37c06e480a0c62.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testLogGuardStatementMappings() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/flink",
				"https://github.com/apache/flink.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "a959dd5034127161aafcf9c56222c7d08aa80e54";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("getNextInputSplit") && mapper.getContainer2().getName().equals("getNextInputSplit")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/flink-a959dd5034127161aafcf9c56222c7d08aa80e54.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergeConditionals() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/spring-framework",
				"https://github.com/spring-projects/spring-framework.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "7dd8dc62a5fa08e3cc99d2388ff62f5825151fb9";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("writeWithMessageConverters") && mapper.getContainer2().getName().equals("writeWithMessageConverters")) {
								mapperInfo(mapper, actual);
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/spring-framework-7dd8dc62a5fa08e3cc99d2388ff62f5825151fb9.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSlidedStatementMappings2() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/spring-framework",
				"https://github.com/spring-projects/spring-framework.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "981aefc2c0d2a6fbf9c08d4d54d17923a75a2e01";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("loadBeanDefinitionsForBeanMethod") && mapper.getContainer2().getName().equals("loadBeanDefinitionsForBeanMethod")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/spring-framework-981aefc2c0d2a6fbf9c08d4d54d17923a75a2e01.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergedStatementMappingsMovedOutOfIfElseIfBranch() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		Repository repo = gitService.cloneIfNotExists(
				REPOS + "/liferay-plugins",
				"https://github.com/liferay/liferay-plugins.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "7c7ecf4cffda166938efd0ae34830e2979c25c73", new RefactoringHandler() {
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

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/liferay-plugins-7c7ecf4cffda166938efd0ae34830e2979c25c73.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMergedStatementMappingsMovedOutOfIfElseIfBranch2() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/jfinal",
				"https://github.com/jfinal/jfinal.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "881baed894540031bd55e402933bcad28b74ca88";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("buildActionMapping") && mapper.getContainer2().getName().equals("buildActionMapping")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jfinal-881baed894540031bd55e402933bcad28b74ca88.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSplitStatementMappingsMovedInIfElseBranch() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/gradle",
				"https://github.com/gradle/gradle.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "f841d8dda2bf461f595755f85c3eba786783702d";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("runBuildOperation") && mapper.getContainer2().getName().equals("runBuildOperation")) {
								mapperInfo(mapper, actual);
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/gradle-f841d8dda2bf461f595755f85c3eba786783702d.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void innerTryBlockDeleted() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/jgit",
				"https://github.com/eclipse/jgit.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "d726f0c1e02c196e2dd87de53b54338be15503f1";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jgit-d726f0c1e02c196e2dd87de53b54338be15503f1.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void innerTryBlockDeleted2() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/jgit",
				"https://github.com/eclipse/jgit.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "45e79a526c7ffebaf8e4758a6cb6b7af05716707";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jgit-45e79a526c7ffebaf8e4758a6cb6b7af05716707.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void innerTryBlockDeleted3() throws Exception {
		//improve the mapping of statements: throw new ManifestErrorException(e);
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/jgit",
				"https://github.com/eclipse/jgit.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "9bebb1eae78401e1d3289dc3d84006c10d10c0ef";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("call") && mapper.getContainer2().getName().equals("call")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jgit-9bebb1eae78401e1d3289dc3d84006c10d10c0ef.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void outerTryBlockAdded() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/checkstyle",
				"https://github.com/checkstyle/checkstyle.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "f020066f8bdfb378df36904af3df8b5bc48858fd";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("process") && mapper.getContainer2().getName().equals("process")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/checkstyle-f020066f8bdfb378df36904af3df8b5bc48858fd.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testSplitVariableDeclarationStatement() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/commons-lang",
				"https://github.com/apache/commons-lang.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "4d46f014fb8ee44386feb5fec52509f35d0e36ea";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("toLocale") && mapper.getContainer2().getName().equals("toLocale")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/commons-lang-4d46f014fb8ee44386feb5fec52509f35d0e36ea.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testNestedForLoopsWithVariableRenames() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/drill",
				"https://github.com/apache/drill.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "b2bbd9941be6b132a83d27c0ae02c935e1dec5dd";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("flattenRecords") && mapper.getContainer2().getName().equals("flattenRecords")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/drill-b2bbd9941be6b132a83d27c0ae02c935e1dec5dd.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testIsomorphicControlStructure() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/javaparser",
				"https://github.com/javaparser/javaparser.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "a25f53f8871fd178b6791d1194d7358b55d1ba37";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("apply") && mapper.getContainer2().getName().equals("apply")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/javaparser-a25f53f8871fd178b6791d1194d7358b55d1ba37.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testZeroStatementMappings() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/okhttp",
				"https://github.com/square/okhttp.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "084b06b48bae2b566bb1be3415b6c847d8ea3682";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("getResponse") && mapper.getContainer2().getName().equals("getResponse") && mapper.getContainer1().getClassName().equals("okhttp3.internal.huc.HttpURLConnectionImpl")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/okhttp-084b06b48bae2b566bb1be3415b6c847d8ea3682.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testWhileLoopsWithRenamedVariable() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/jgit",
				"https://github.com/eclipse/jgit.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "733780e8a158b7bc45b8b687ac353ecadc905a63";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().getName().equals("findObjectsToPack") && mapper.getContainer2().getName().equals("findObjectsToPack")) {
								mapperInfo(mapper, actual);
								break;
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/jgit-733780e8a158b7bc45b8b687ac353ecadc905a63.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertMappings() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/commons-lang",
				"https://github.com/apache/commons-lang.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "5111ae7db08a70323a51a21df0bbaf46f21e072e";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
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
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/commons-lang-5111ae7db08a70323a51a21df0bbaf46f21e072e.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testAssertMappings2() throws Exception {
		Repository repository = gitService.cloneIfNotExists(
				REPOS + "/Aeron",
				"https://github.com/real-logic/Aeron.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "35893c115ba23bd62a7036a33390420f074ce660";
		List<Refactoring> refactoringsAtRevision;
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, commit, filePathsBefore, filePathsCurrent, renamedFilesHint);

				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && commit.getParentCount() > 0) {
					RevCommit parentCommit = commit.getParent(0);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);
					UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactoringsAtRevision = modelDiff.getRefactorings();
					refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
					List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
					for(UMLClassDiff classDiff : commonClassDiff) {
						for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
							if(mapper.getContainer1().hasTestAnnotation() && mapper.getContainer2().hasTestAnnotation()) {
								mapperInfo(mapper, actual);
							}
						}
					}
				}
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/Aeron-35893c115ba23bd62a7036a33390420f074ce660.txt"));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	private void mapperInfo(UMLOperationBodyMapper bodyMapper, final List<String> actual) {
		actual.add(bodyMapper.toString());
		//System.out.println(bodyMapper.toString());
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
			actual.add(line);
			//System.out.println(line);
		}
	}
}

