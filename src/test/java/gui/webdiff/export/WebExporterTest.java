package gui.webdiff.export;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void leavesNonExportedPageLinksAlone() {
		String html = new WebExporter(null).doProperReplacement(
				"<a href=\"/content?side=deleted&path=A.java\">d</a>"
						+ "<a href=\"/quit\">q</a>",
				MONACO_PAGE_DEPTH);

		// The deleted/added content endpoint is not exported here (handled by a
		// follow-up PR), so its link must be left as-is, not pointed at index.html.
		assertTrue(html.contains("content?side=deleted&path=A.java"), html);
		assertFalse(html.contains("A.java/index.html"), html);
		// quit is a server-only endpoint, not an exported page.
		assertFalse(html.contains("quit/index.html"), html);
	}
}
