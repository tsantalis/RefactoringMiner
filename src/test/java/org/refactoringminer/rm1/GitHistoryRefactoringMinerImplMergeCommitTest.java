package org.refactoringminer.rm1;

import gui.webdiff.WebDiff;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.RefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.util.GitServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHistoryRefactoringMinerImplMergeCommitTest {

	@TempDir
	Path tempDir;

	@Test
	void diffAtCommitCanTargetANonDefaultMergeParent() throws Exception {
		MergeCommitFixture fixture = createMergeCommitFixture();
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();

		try (Repository repository = new GitServiceImpl().openRepository(fixture.repositoryPath().toString())) {
			ProjectASTDiff defaultDiff = miner.diffAtCommit(repository, fixture.mergeCommit());
			assertNotNull(defaultDiff);
			assertTrue(defaultDiff.getDiffSet().isEmpty());
			assertTrue(defaultDiff.getMoveDiffSet().isEmpty());
			assertTrue(defaultDiff.getMetaInfo().supportsParentSelection());
			assertTrue(defaultDiff.getMetaInfo().getAvailableParentIndices().contains(1));
			WebDiff webDiff = assertDoesNotThrow(() -> new WebDiff(defaultDiff));
			assertDoesNotThrow(() -> webDiff.switchToParent(1));
			assertEquals(1, webDiff.getProjectASTDiff().getMetaInfo().getSelectedParentIndex());
			assertFalse(webDiff.getComparator().getDiffs().isEmpty());

			ProjectASTDiff secondParentDiff = miner.diffAtCommit(repository, fixture.mergeCommit(), 1);
			assertNotNull(secondParentDiff);
			assertEquals(1, secondParentDiff.getMetaInfo().getSelectedParentIndex());
			assertFalse(secondParentDiff.getDiffSet().isEmpty());
			assertTrue(secondParentDiff.getRefactorings().stream()
					.anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.RENAME_METHOD));

			assertThrows(IllegalArgumentException.class, () -> miner.diffAtCommit(repository, fixture.mergeCommit(), 2));
		}
	}

	@Test
	void detectAtCommitCanTargetANonDefaultMergeParent() throws Exception {
		MergeCommitFixture fixture = createMergeCommitFixture();
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();

		try (Repository repository = new GitServiceImpl().openRepository(fixture.repositoryPath().toString())) {
			CollectingHandler defaultHandler = new CollectingHandler();
			miner.detectAtCommit(repository, fixture.mergeCommit(), defaultHandler);
			assertTrue(defaultHandler.refactorings.isEmpty());

			CollectingHandler secondParentHandler = new CollectingHandler();
			miner.detectAtCommit(repository, fixture.mergeCommit(), 1, secondParentHandler);
			assertTrue(secondParentHandler.refactorings.stream()
					.anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.RENAME_METHOD));
		}
	}

	@Test
	void commandLineDetectAtCommitAcceptsParentIndex() throws Exception {
		MergeCommitFixture fixture = createMergeCommitFixture();
		Path jsonPath = tempDir.resolve("merge-parent.json");

		RefactoringMiner.detectAtCommit(new String[] {
				"-c",
				fixture.repositoryPath().toString(),
				fixture.mergeCommit(),
				"--parent-index",
				"1",
				"-json",
				jsonPath.toString()
		});

		String json = Files.readString(jsonPath);
		assertTrue(json.contains("Rename Method"));
	}

	@Test
	void webDiffCanSwitchAcrossOctopusMergeParentsWithoutRemote() throws Exception {
		MergeCommitFixture fixture = createOctopusMergeFixture(false);
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();

		try (Repository repository = new GitServiceImpl().openRepository(fixture.repositoryPath().toString())) {
			ProjectASTDiff defaultDiff = miner.diffAtCommit(repository, fixture.mergeCommit());
			assertEquals(3, defaultDiff.getMetaInfo().getParentCount());
			assertFalse(defaultDiff.getMetaInfo().hasUrl());
			assertTrue(defaultDiff.getMetaInfo().supportsParentSelection());

			WebDiff webDiff = new WebDiff(defaultDiff);
			assertEquals(Set.of(
					"src/main/java/example/Alpha.java",
					"src/main/java/example/Beta.java"), diffPaths(webDiff.getProjectASTDiff()));

			assertDoesNotThrow(() -> webDiff.switchToParent(1));
			assertEquals(Set.of("src/main/java/example/Beta.java"), diffPaths(webDiff.getProjectASTDiff()));

			assertDoesNotThrow(() -> webDiff.switchToParent(2));
			assertEquals(Set.of("src/main/java/example/Alpha.java"), diffPaths(webDiff.getProjectASTDiff()));

			assertDoesNotThrow(() -> webDiff.switchToParent(0));
			assertEquals(Set.of(
					"src/main/java/example/Alpha.java",
					"src/main/java/example/Beta.java"), diffPaths(webDiff.getProjectASTDiff()));

			assertThrows(IllegalArgumentException.class, () -> webDiff.switchToParent(3));
		}
	}

	private MergeCommitFixture createMergeCommitFixture() throws Exception {
		return createTwoParentMergeFixture(true);
	}

	private MergeCommitFixture createTwoParentMergeFixture(boolean addRemote) throws Exception {
		Path repositoryPath = tempDir.resolve("merge-parent-repo");
		Files.createDirectories(repositoryPath);

		runGit(repositoryPath, "init");
		runGit(repositoryPath, "checkout", "-b", "main");
		runGit(repositoryPath, "config", "user.name", "Codex Test");
		runGit(repositoryPath, "config", "user.email", "codex@example.com");
		if (addRemote) {
			runGit(repositoryPath, "remote", "add", "origin", "https://github.com/example/merge-parent-repo.git");
		}

		Path javaFile = repositoryPath.resolve("src/main/java/example/Sample.java");
		Files.createDirectories(javaFile.getParent());
		write(javaFile, """
				package example;

				class Sample {
				    String greet() {
				        return "hello";
				    }
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Initial commit");

		runGit(repositoryPath, "checkout", "-b", "topic");
		Path branchFile = repositoryPath.resolve("branch.txt");
		write(branchFile, "temporary branch file\n");
		runGit(repositoryPath, "add", "branch.txt");
		runGit(repositoryPath, "commit", "-m", "Add branch file");

		Files.delete(branchFile);
		runGit(repositoryPath, "add", "-A");
		runGit(repositoryPath, "commit", "-m", "Revert branch file");

		runGit(repositoryPath, "checkout", "main");
		write(javaFile, """
				package example;

				class Sample {
				    String salute() {
				        return "hello";
				    }
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Rename method");

		runGit(repositoryPath, "merge", "--no-ff", "topic", "-m", "Merge topic");
		return new MergeCommitFixture(repositoryPath, runGit(repositoryPath, "rev-parse", "HEAD").trim());
	}

	private MergeCommitFixture createOctopusMergeFixture(boolean addRemote) throws Exception {
		Path repositoryPath = tempDir.resolve(addRemote ? "octopus-remote-repo" : "octopus-local-repo");
		Files.createDirectories(repositoryPath);

		runGit(repositoryPath, "init");
		runGit(repositoryPath, "checkout", "-b", "main");
		runGit(repositoryPath, "config", "user.name", "Codex Test");
		runGit(repositoryPath, "config", "user.email", "codex@example.com");
		if (addRemote) {
			runGit(repositoryPath, "remote", "add", "origin", "https://github.com/example/octopus-merge-repo.git");
		}

		Path alphaFile = repositoryPath.resolve("src/main/java/example/Alpha.java");
		Path betaFile = repositoryPath.resolve("src/main/java/example/Beta.java");
		Files.createDirectories(alphaFile.getParent());
		write(alphaFile, """
				package example;

				class Alpha {
				    String alpha() {
				        return "alpha";
				    }
				}
				""");
		write(betaFile, """
				package example;

				class Beta {
				    String beta() {
				        return "beta";
				    }
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Initial commit");

		runGit(repositoryPath, "checkout", "-b", "branch-one");
		write(alphaFile, """
				package example;

				class Alpha {
				    String alphaOne() {
				        return "alpha-one";
				    }
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Change Alpha");

		runGit(repositoryPath, "checkout", "main");
		runGit(repositoryPath, "checkout", "-b", "branch-two");
		write(betaFile, """
				package example;

				class Beta {
				    String betaTwo() {
				        return "beta-two";
				    }
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Change Beta");

		runGit(repositoryPath, "checkout", "main");
		runGit(repositoryPath, "merge", "--no-ff", "branch-one", "branch-two", "-m", "Octopus merge");
		return new MergeCommitFixture(repositoryPath, runGit(repositoryPath, "rev-parse", "HEAD").trim());
	}

	private void write(Path path, String content) throws IOException {
		Files.writeString(path, content, StandardCharsets.UTF_8);
	}

	private Set<String> diffPaths(ProjectASTDiff diff) {
		Set<String> paths = new LinkedHashSet<>();
		for (ASTDiff astDiff : diff.getDiffSet()) {
			paths.add(astDiff.getDstPath());
		}
		for (ASTDiff astDiff : diff.getMoveDiffSet()) {
			paths.add(astDiff.getDstPath());
		}
		return paths;
	}

	private String runGit(Path repositoryPath, String... args) throws Exception {
		List<String> command = new ArrayList<>();
		command.add("git");
		for (String arg : args) {
			command.add(arg);
		}
		Process process = new ProcessBuilder(command)
				.directory(repositoryPath.toFile())
				.redirectErrorStream(true)
				.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new IllegalStateException(String.format("git %s failed:%n%s", String.join(" ", args), output));
		}
		return output;
	}

	private record MergeCommitFixture(Path repositoryPath, String mergeCommit) {
	}

	private static class CollectingHandler implements RefactoringHandler {
		private List<Refactoring> refactorings = List.of();

		@Override
		public void handle(String commitId, List<Refactoring> refactorings) {
			this.refactorings = new ArrayList<>(refactorings);
		}
	}
}
