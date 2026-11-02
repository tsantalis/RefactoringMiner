package org.refactoringminer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * The commit walks of GitServiceImpl on a local repository created in a temporary directory, without network access.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class GitServiceImplLocalRepositoryTest {
	@TempDir
	Path repositoryPath;

	//c1 <- c2 (annotated tag start) <- c3 <- c4 (lightweight tag end) <- c5
	private List<RevCommit> createRepository(Git git) throws Exception {
		List<RevCommit> commits = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			Files.writeString(repositoryPath.resolve("A.java"), "class A { int f" + i + "; }\n");
			git.add().addFilepattern("A.java").call();
			commits.add(git.commit().setMessage("c" + i).setAuthor("author", "author@example.com").setCommitter("author", "author@example.com").setSign(false).call());
			if (i == 2) {
				git.tag().setName("start").setAnnotated(true).setMessage("start").setObjectId(commits.get(1)).setSigned(false).call();
			}
			if (i == 4) {
				git.tag().setName("end").setAnnotated(false).setObjectId(commits.get(3)).call();
			}
		}
		return commits;
	}

	@Test
	void walkBetweenTagsResolvesAnnotatedAndLightweightTags() throws Exception {
		try (Git git = Git.init().setDirectory(repositoryPath.toFile()).setInitialBranch("main").call()) {
			List<RevCommit> commits = createRepository(git);
			Repository repository = git.getRepository();
			//the commits after the start tag up to the end tag, oldest first
			assertEquals(List.of(commits.get(2), commits.get(3)), toList(new GitServiceImpl().createRevsWalkBetweenTags(repository, "start", "end")));
		}
	}

	@Test
	void walkBetweenTagsResolvesPackedTags() throws Exception {
		try (Git git = Git.init().setDirectory(repositoryPath.toFile()).setInitialBranch("main").call()) {
			List<RevCommit> commits = createRepository(git);
			git.packRefs().setAll(true).call();
			Repository repository = git.getRepository();
			assertEquals(List.of(commits.get(2), commits.get(3)), toList(new GitServiceImpl().createRevsWalkBetweenTags(repository, "start", "end")));
		}
	}

	@Test
	void walkBetweenCommitsIncludesTheStartCommit() throws Exception {
		try (Git git = Git.init().setDirectory(repositoryPath.toFile()).setInitialBranch("main").call()) {
			List<RevCommit> commits = createRepository(git);
			Repository repository = git.getRepository();
			assertEquals(List.of(commits.get(1), commits.get(2), commits.get(3)),
					toList(new GitServiceImpl().createRevsWalkBetweenCommits(repository, commits.get(1).getName(), commits.get(3).getName())));
		}
	}

	private static List<RevCommit> toList(Iterable<RevCommit> iterable) {
		List<RevCommit> list = new ArrayList<>();
		iterable.forEach(list::add);
		return list;
	}
}
