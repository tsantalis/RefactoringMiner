package org.refactoringminer.test;

import java.io.FileReader;
import java.util.*;

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsCommitMetadata;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
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

public class TestStatementMappings extends LightJavaCodeInsightFixtureTestCase {
	private static final String REPOS = "tmp1";
	private GitService gitService = new GitServiceImpl();
	@Test
	public void testMappings() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

		GitRepository repo = gitService.cloneIfNotExists(getProject(),
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMappingsReverseParentChildCommit() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		GitRepository repo = gitService.cloneIfNotExists(getProject(),
			    REPOS + "/TestCases",
			    "https://github.com/pouryafard75/TestCases.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "e47272d6e1390b6366f577b84c58eae50f8f0a69", new RefactoringHandler() {
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
		
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/infinispan-043030723632627b0908dca6b24dae91d3dfd938-reverse.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	@Test
	public void testMultiMappingInDuplicatedCode() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		GitRepository repo = gitService.cloneIfNotExists(getProject(),
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings() throws Exception {
		GitRepository repository = gitService.cloneIfNotExists(getProject(),
				".",
				"https://github.com/tsantalis/RefactoringMiner.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "fbd80e76c68558ba58b62311aa1c34fb38baf53a";
		List<Refactoring> refactoringsAtRevision;
		List<? extends VcsCommitMetadata> childCommits = GitHistoryUtils.collectCommitsMetadata(repository.getProject(), repository.getRoot(), commitId);
		Collection<Change> changes = null;
		VcsCommitMetadata currentCommit = null;
		if(childCommits != null) {
			currentCommit = childCommits.get(0);
			if (currentCommit.getParents().size() > 0) {
				Hash parentCommitId = currentCommit.getParents().get(0);
				changes = GitChangeUtils.getDiff(repository, parentCommitId.asString(), currentCommit.getId().asString(), true);
			}
			else {
				changes = Collections.emptyList();
			}
		}
		Set<String> filePathsBefore = new LinkedHashSet<String>();
		Set<String> filePathsCurrent = new LinkedHashSet<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, changes, filePathsBefore, filePathsCurrent, renamedFilesHint);

		Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
		Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();

		if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParents().size() > 0) {
			Hash parentCommitId = currentCommit.getParents().get(0);
			List<? extends VcsCommitMetadata> parentCommits = GitHistoryUtils.collectCommitsMetadata(repository.getProject(), repository.getRoot(), parentCommitId.asString());
			VcsCommitMetadata parentCommit = null;
			if(parentCommits != null) {
				parentCommit = parentCommits.get(0);
			}
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, changes, GitHistoryRefactoringMinerImpl.RevisionType.BEFORE, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, changes, GitHistoryRefactoringMinerImpl.RevisionType.AFTER, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/miner-fbd80e76c68558ba58b62311aa1c34fb38baf53a.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testCopiedStatementMappings2() throws Exception {
		GitRepository repository = gitService.cloneIfNotExists(getProject(),
				REPOS + "/javaparser",
				"https://github.com/javaparser/javaparser.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "f4ce6ce924ffbd03518c64cea9b830d04f75b849";
		List<Refactoring> refactoringsAtRevision;
		List<? extends VcsCommitMetadata> childCommits = GitHistoryUtils.collectCommitsMetadata(repository.getProject(), repository.getRoot(), commitId);
		Collection<Change> changes = null;
		VcsCommitMetadata currentCommit = null;
		if(childCommits != null) {
			currentCommit = childCommits.get(0);
			if (currentCommit.getParents().size() > 0) {
				Hash parentCommitId = currentCommit.getParents().get(0);
				changes = GitChangeUtils.getDiff(repository, parentCommitId.asString(), currentCommit.getId().asString(), true);
			}
			else {
				changes = Collections.emptyList();
			}
		}
		Set<String> filePathsBefore = new LinkedHashSet<String>();
		Set<String> filePathsCurrent = new LinkedHashSet<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, changes, filePathsBefore, filePathsCurrent, renamedFilesHint);

		Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
		Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();

		if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParents().size() > 0) {
			Hash parentCommitId = currentCommit.getParents().get(0);
			List<? extends VcsCommitMetadata> parentCommits = GitHistoryUtils.collectCommitsMetadata(repository.getProject(), repository.getRoot(), parentCommitId.asString());
			VcsCommitMetadata parentCommit = null;
			if(parentCommits != null) {
				parentCommit = parentCommits.get(0);
			}
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, changes, GitHistoryRefactoringMinerImpl.RevisionType.BEFORE, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, changes, GitHistoryRefactoringMinerImpl.RevisionType.AFTER, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/javaparser-f4ce6ce924ffbd03518c64cea9b830d04f75b849.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMappings3() throws Exception {
		GitRepository repository = gitService.cloneIfNotExists(getProject(),
				REPOS + "/flink",
				"https://github.com/apache/flink.git");

		final List<String> actual = new ArrayList<>();
		String commitId = "e0a4ee07084bc6ab56a20fbc4a18863462da93eb";
		List<Refactoring> refactoringsAtRevision;
		List<? extends VcsCommitMetadata> childCommits = GitHistoryUtils.collectCommitsMetadata(repository.getProject(), repository.getRoot(), commitId);
		Collection<Change> changes = null;
		VcsCommitMetadata currentCommit = null;
		if(childCommits != null) {
			currentCommit = childCommits.get(0);
			if (currentCommit.getParents().size() > 0) {
				Hash parentCommitId = currentCommit.getParents().get(0);
				changes = GitChangeUtils.getDiff(repository, parentCommitId.asString(), currentCommit.getId().asString(), true);
			}
			else {
				changes = Collections.emptyList();
			}
		}
		Set<String> filePathsBefore = new LinkedHashSet<String>();
		Set<String> filePathsCurrent = new LinkedHashSet<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, changes, filePathsBefore, filePathsCurrent, renamedFilesHint);

		Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
		Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();

		if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParents().size() > 0) {
			Hash parentCommitId = currentCommit.getParents().get(0);
			List<? extends VcsCommitMetadata> parentCommits = GitHistoryUtils.collectCommitsMetadata(repository.getProject(), repository.getRoot(), parentCommitId.asString());
			VcsCommitMetadata parentCommit = null;
			if(parentCommits != null) {
				parentCommit = parentCommits.get(0);
			}
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, changes, GitHistoryRefactoringMinerImpl.RevisionType.BEFORE, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, changes, GitHistoryRefactoringMinerImpl.RevisionType.AFTER, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
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
		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/flink-e0a4ee07084bc6ab56a20fbc4a18863462da93eb.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMappings2() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		GitRepository repo = gitService.cloneIfNotExists(getProject(),
				REPOS + "/k-9",
				"https://github.com/k9mail/k-9.git");

		final List<String> actual = new ArrayList<>();
		miner.detectAtCommit(repo, "23c49d834d3859fc76a604da32d1789d2e863303", new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					if(ref instanceof ExtractOperationRefactoring) {
						ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
						UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
						mapperInfo(bodyMapper, actual);
					}
				}
			}
		});

		List<String> expected = IOUtils.readLines(new FileReader(System.getProperty("user.dir") + "/src-test/Data/k9mail-23c49d834d3859fc76a604da32d1789d2e863303.txt"));
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMappings4() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		GitRepository repo = gitService.cloneIfNotExists(getProject(),
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMappings5() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		GitRepository repo = gitService.cloneIfNotExists(getProject(),
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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@Test
	public void testMappings6() throws Exception {
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		GitRepository repo = gitService.cloneIfNotExists(getProject(),
				REPOS + "/deeplearning4j",
				"https://deeplearning4j/deeplearning4j.git");

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
		Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
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
