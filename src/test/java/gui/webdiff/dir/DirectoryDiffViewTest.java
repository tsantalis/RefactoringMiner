package gui.webdiff.dir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.rendersnake.HtmlCanvas;

class DirectoryDiffViewTest {
	@TempDir
	Path tempDir;

	@Test
	void rendersQuitButtonByDefault() throws Exception {
		DirectoryDiffView view = new DirectoryDiffView(createComparator(), false, new DiffMetaInfo("Local diff", null),
				true);

		String html = render(view);

		assertTrue(html.contains("href=\"/quit\""));
		assertTrue(html.contains(">Quit<"));
	}

	@Test
	void canHideQuitButtonForManagedWebDiffSessions() throws Exception {
		DirectoryDiffView view = new DirectoryDiffView(createComparator(), false, new DiffMetaInfo("Local diff", null),
				true, false);

		String html = render(view);

		assertFalse(html.contains("href=\"/quit\""));
		assertFalse(html.contains(">Quit<"));
	}

	private DirComparator createComparator() throws Exception {
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
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl()
				.diffAtDirectories(tempDir.resolve("before"), tempDir.resolve("after"));
		projectASTDiff.setMetaInfo(new DiffMetaInfo("Local diff", null));
		return new DirComparator(projectASTDiff, diffs -> diffs);
	}

	private static String render(DirectoryDiffView view) throws IOException {
		HtmlCanvas canvas = new HtmlCanvas();
		view.renderOn(canvas);
		return canvas.toHtml();
	}
}
