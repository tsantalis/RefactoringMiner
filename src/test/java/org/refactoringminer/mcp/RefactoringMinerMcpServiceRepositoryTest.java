package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

class RefactoringMinerMcpServiceRepositoryTest {
	@TempDir
	Path tempDir;

	@Test
	void analyzeCommitReadsLocalRepositoryWithoutNetwork() throws Exception {
		Path repositoryPath = tempDir.resolve("repo");
		RevCommit commit;
		try (Git git = Git.init().setDirectory(repositoryPath.toFile()).call()) {
			Path sourceDir = Files.createDirectories(repositoryPath.resolve("src/main/java/example"));
			Path sourceFile = sourceDir.resolve("Sample.java");
			Files.writeString(sourceFile, """
					package example;
					class Sample {
						void beforeName() {
						}
					}
					""");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("Test", "test@example.com")
					.setCommitter("Test", "test@example.com").call();

			Files.writeString(sourceFile, """
					package example;
					class Sample {
						void afterName() {
						}
					}
					""");
			git.add().addFilepattern(".").call();
			commit = git.commit().setMessage("rename method").setAuthor("Test", "test@example.com")
					.setCommitter("Test", "test@example.com").call();
		}

		McpAnalysisResult result = new RefactoringMinerMcpService()
				.analyzeCommit(repositoryPath.toAbsolutePath(), commit.getName(), null, 20);

		assertEquals("ok", result.status());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
		assertFalse(result.summary().isBlank());
	}

	@Test
	void commitRepositoryPathResolvesLinkedWorktreeToCommonWorktree() throws Exception {
		Path mainWorktree = Files.createDirectories(tempDir.resolve("main"));
		Path commonGitDir = Files.createDirectories(mainWorktree.resolve(".git"));
		Path linkedWorktree = Files.createDirectories(tempDir.resolve("linked"));
		Path linkedGitDir = Files.createDirectories(commonGitDir.resolve("worktrees/linked"));
		Files.writeString(linkedWorktree.resolve(".git"), "gitdir: " + linkedGitDir + "\n");
		Files.writeString(linkedGitDir.resolve("commondir"), "../..\n");

		Path resolved = RefactoringMinerMcpService.resolveCommitRepositoryPath(linkedWorktree);

		assertEquals(mainWorktree.toRealPath(), resolved);
	}

	@Test
	void analyzeDirectoriesUsesExistingDirectoryDiff() throws Exception {
		Path beforeDir = Files.createDirectories(tempDir.resolve("before/src/main/java/example"));
		Path afterDir = Files.createDirectories(tempDir.resolve("after/src/main/java/example"));
		Files.writeString(beforeDir.resolve("Sample.java"), """
				package example;
				class Sample {
					int value() { return 1; }
				}
				""");
		Files.writeString(afterDir.resolve("Sample.java"), """
				package example;
				class Sample {
					int value() { return 2; }
				}
				""");

		McpAnalysisResult result = new RefactoringMinerMcpService()
				.analyzeDirectories(tempDir.resolve("before").toAbsolutePath(), tempDir.resolve("after").toAbsolutePath(), 20);

		assertEquals("ok", result.status());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
		assertTrue(result.summary().contains("AST diffs"));
	}

	@Test
	void analyzePullRequestCanBeTestedWithoutLiveNetwork() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> new ProjectASTDiff(before, after),
				(repositoryPath, commitId, parentIndex) -> new ProjectASTDiff(Map.of(), Map.of()),
				(cloneUrl, pullRequestId, timeoutSeconds) -> new ProjectASTDiff(
						Map.of("src/main/java/A.java", "class A {}"),
						Map.of("src/main/java/A.java", "class A { int x; }")),
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()));

		McpAnalysisResult result = service.analyzePullRequest("https://github.com/tsantalis/RefactoringMiner.git", 1, 30, 20);

		assertEquals("ok", result.status());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
	}

	@Test
	void analyzePullRequestValidatesBeforeNetworkCapablePath() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> new ProjectASTDiff(before, after),
				(repositoryPath, commitId, parentIndex) -> new ProjectASTDiff(Map.of(), Map.of()),
				(cloneUrl, pullRequestId, timeoutSeconds) -> {
					throw new AssertionError("pull request differ should not run for invalid input");
				},
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()));

		McpAnalysisResult result = service.analyzePullRequest("https://github.com/tsantalis/RefactoringMiner.git", 0, 30, 20);

		assertEquals("error", result.status());
		assertTrue(result.summary().contains("pullRequestId must be greater than 0"));
	}

	@Test
	void analyzeCommitPrefersGitHubOriginWhenAvailable() throws Exception {
		Path repositoryPath = tempDir.resolve("remote-first-repo");
		try (Git git = Git.init().setDirectory(repositoryPath.toFile()).call()) {
			git.remoteAdd()
					.setName("origin")
					.setUri(new org.eclipse.jgit.transport.URIish("git@github.com:tsantalis/RefactoringMiner.git"))
					.call();
		}
		AtomicBoolean remoteCalled = new AtomicBoolean(false);
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> new ProjectASTDiff(before, after),
				(localRepositoryPath, commitId, parentIndex) -> {
					throw new AssertionError("local commit differ should not run when GitHub succeeds");
				},
				(cloneUrl, commitId, parentIndex, timeoutSeconds) -> {
					remoteCalled.set(true);
					assertEquals("https://github.com/tsantalis/RefactoringMiner.git", cloneUrl);
					return new ProjectASTDiff(
							Map.of("src/main/java/A.java", "class A {}"),
							Map.of("src/main/java/A.java", "class A { int x; }"));
				},
				(cloneUrl, pullRequestId, timeoutSeconds) -> new ProjectASTDiff(Map.of(), Map.of()),
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()));

		McpAnalysisResult result = service.analyzeCommit(repositoryPath.toAbsolutePath(), "abc123", null, 20);

		assertEquals("ok", result.status());
		assertTrue(remoteCalled.get());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
	}

	@Test
	void analyzeCommitFallsBackToLocalWhenGitHubLookupFails() throws Exception {
		Path repositoryPath = tempDir.resolve("local-fallback-repo");
		try (Git git = Git.init().setDirectory(repositoryPath.toFile()).call()) {
			git.remoteAdd()
					.setName("origin")
					.setUri(new org.eclipse.jgit.transport.URIish("https://github.com/tsantalis/RefactoringMiner.git"))
					.call();
		}
		AtomicBoolean localCalled = new AtomicBoolean(false);
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> new ProjectASTDiff(before, after),
				(localRepositoryPath, commitId, parentIndex) -> {
					localCalled.set(true);
					return new ProjectASTDiff(
							Map.of("src/main/java/A.java", "class A {}"),
							Map.of("src/main/java/A.java", "class A { int x; }"));
				},
				(cloneUrl, commitId, parentIndex, timeoutSeconds) -> {
					throw new IllegalArgumentException("commit was not found on GitHub");
				},
				(cloneUrl, pullRequestId, timeoutSeconds) -> new ProjectASTDiff(Map.of(), Map.of()),
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()));

		McpAnalysisResult result = service.analyzeCommit(repositoryPath.toAbsolutePath(), "local-only", null, 20);

		assertEquals("ok", result.status());
		assertTrue(localCalled.get());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
	}
}
