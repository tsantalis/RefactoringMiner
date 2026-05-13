package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorktreeChangeCollectorTest {
	@TempDir
	Path tempDir;

	@Test
	void collectsModifiedAddedDeletedAndUntrackedSupportedFiles() throws Exception {
		Path repo = tempDir.resolve("repo");
		try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
			write(repo, "src/main/java/A.java", "class A { void f() {} }");
			write(repo, "src/main/java/DeleteMe.java", "class DeleteMe {}");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("A", "a@example.com").setCommitter("A", "a@example.com").call();

			write(repo, "src/main/java/A.java", "class A { void f() { int x = 1; } }");
			Files.delete(repo.resolve("src/main/java/DeleteMe.java"));
			write(repo, "src/main/java/NewFile.java", "class NewFile {}");
			write(repo, "src/main/java/UntrackedFile.java", "class UntrackedFile {}");
			write(repo, "README.md", "ignored");
			git.add().addFilepattern("src/main/java/NewFile.java").call();

			WorktreeChangeCollector.WorktreeChanges changes = new WorktreeChangeCollector()
					.collect(repo.toAbsolutePath(), "HEAD", true, 10, 10_000);

			assertEquals("class A { void f() {} }", changes.beforeFiles().get("src/main/java/A.java"));
			assertEquals("class A { void f() { int x = 1; } }", changes.afterFiles().get("src/main/java/A.java"));
			assertTrue(changes.beforeFiles().containsKey("src/main/java/DeleteMe.java"));
			assertFalse(changes.afterFiles().containsKey("src/main/java/DeleteMe.java"));
			assertFalse(changes.beforeFiles().containsKey("src/main/java/NewFile.java"));
			assertTrue(changes.afterFiles().containsKey("src/main/java/NewFile.java"));
			assertTrue(changes.afterFiles().containsKey("src/main/java/UntrackedFile.java"));
			assertFalse(changes.afterFiles().containsKey("README.md"));
			assertTrue(changes.warnings().stream().anyMatch(warning -> warning.contains("README.md")));
		}
	}

	@Test
	void excludesUntrackedFilesWhenFlagIsFalse() throws Exception {
		Path repo = tempDir.resolve("repo");
		try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
			write(repo, "src/main/java/A.java", "class A {}");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("A", "a@example.com").setCommitter("A", "a@example.com").call();
			write(repo, "src/main/java/UntrackedFile.java", "class UntrackedFile {}");

			IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
					() -> new WorktreeChangeCollector().collect(repo.toAbsolutePath(), "HEAD", false, 10, 10_000));

			assertTrue(error.getMessage().contains("No supported worktree changes"));
		}
	}

	@Test
	void rejectsRelativeNonGitAndLimitExceededInputs() throws Exception {
		WorktreeChangeCollector collector = new WorktreeChangeCollector();

		assertThrows(IllegalArgumentException.class,
				() -> collector.collect(Path.of("relative"), "HEAD", true, 10, 10_000));
		assertThrows(IllegalArgumentException.class,
				() -> collector.collect(tempDir.toAbsolutePath(), "HEAD", true, 10, 10_000));

		Path repo = tempDir.resolve("limited");
		try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
			write(repo, "src/main/java/A.java", "class A {}");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("A", "a@example.com").setCommitter("A", "a@example.com").call();
			write(repo, "src/main/java/A.java", "class A { String value = \"too long\"; }");

			assertThrows(IllegalArgumentException.class,
					() -> collector.collect(repo.toAbsolutePath(), "HEAD", true, 0, 10_000));
			assertThrows(IllegalArgumentException.class,
					() -> collector.collect(repo.toAbsolutePath(), "HEAD", true, 10, 5));
		}
	}

	private static void write(Path repo, String relativePath, String content) throws Exception {
		Path file = repo.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}
}
