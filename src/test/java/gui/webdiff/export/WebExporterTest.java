package gui.webdiff.export;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class WebExporterTest {

	// Nesting depth of a monaco-page/<id>/index.html page (two path segments
	// below the export root), so relativized links get a "../../" prefix.
	private static final int MONACO_PAGE_DEPTH = 2;

	@Test
	void rewritesInterPageLinksToTheIndexHtmlFile() {
		String html = new WebExporter(null).doProperReplacement(
				"<a href=\"/monaco-page/5\">m</a>"
						+ "<a href=\"/vanilla-diff/2\">v</a>"
						+ "<a href=\"/list\">l</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../monaco-page/5/index.html\""), html);
		assertTrue(html.contains("href=\"../../vanilla-diff/2/index.html\""), html);
		assertTrue(html.contains("href=\"../../list/index.html\""), html);
	}

	@Test
	void sendsResourceLinksToTheResourcesFolderUntouched() {
		String html = new WebExporter(null).doProperReplacement(
				"<link href=\"/dist/monaco.css\">"
						+ "<script src=\"/monaco/min/vs/loader.js\">",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../resources/dist/monaco.css\""), html);
		assertTrue(html.contains("src=\"../../resources/monaco/min/vs/loader.js\""), html);
		// Resources must not get an index.html appended.
		assertFalse(html.contains("monaco.css/index.html"), html);
	}

	@Test
	void rewritesDeletedAndAddedLinksToTheirExportedContentFolders() {
		WebExporter exporter = new WebExporter(null);
		// Order here is the order the folders are numbered, mirroring the
		// comparator's getRemovedFilesName()/getAddedFilesName() ordering.
		exporter.setContentFiles(List.of("A.java", "src/main/B.java"), List.of("C.java"));

		String html = exporter.doProperReplacement(
				// rendersnake escapes the "&" in attribute values to "&amp;",
				// and paths with slashes are URL-encoded (B.java below).
				"<a href=\"/content?side=deleted&amp;path=A.java\">a</a>"
						+ "<a href=\"/content?side=deleted&amp;path=src%2Fmain%2FB.java\">b</a>"
						+ "<a href=\"/content?side=added&amp;path=C.java\">c</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../content/deleted/0/index.html\""), html);
		assertTrue(html.contains("href=\"../../content/deleted/1/index.html\""), html);
		assertTrue(html.contains("href=\"../../content/added/0/index.html\""), html);
	}

	@Test
	void normalizesLeftRightSidesToDeletedAdded() {
		WebExporter exporter = new WebExporter(null);
		exporter.setContentFiles(List.of("A.java"), List.of("C.java"));

		String html = exporter.doProperReplacement(
				"<a href=\"/content?side=left&amp;path=A.java\">a</a>"
						+ "<a href=\"/content?side=right&amp;path=C.java\">c</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../content/deleted/0/index.html\""), html);
		assertTrue(html.contains("href=\"../../content/added/0/index.html\""), html);
	}

	@Test
	void leavesUnknownContentLinksAndServerOnlyEndpointsAlone() {
		// No content files registered, so a /content link names a file we did
		// not export: leave it as-is rather than pointing at a missing folder.
		String html = new WebExporter(null).doProperReplacement(
				"<a href=\"/content?side=deleted&path=A.java\">d</a>"
						+ "<a href=\"/quit\">q</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("content?side=deleted&path=A.java"), html);
		assertFalse(html.contains("A.java/index.html"), html);
		// quit is a server-only endpoint, not an exported page.
		assertFalse(html.contains("quit/index.html"), html);
	}
}
