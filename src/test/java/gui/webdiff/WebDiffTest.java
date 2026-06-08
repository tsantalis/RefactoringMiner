package gui.webdiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WebDiffTest {

	/**
	 * Regression test for the Next/Prev 404 in the static export. The route
	 * prefix must be the viewer folder with both slashes intact (e.g.
	 * "/monaco-page/") so that {@code AbstractMenuBar} builds full links like
	 * "/monaco-page/6". The previous implementation used
	 * {@code request.pathInfo().split("/")[0]}, which is the empty string, and
	 * produced bare-index links like "6" that 404 once relativized.
	 */
	@Test
	void routePrefixKeepsTheViewerFolderWithBothSlashes() {
		assertEquals("/monaco-page/", WebDiff.routePrefix("/monaco-page/5"));
		assertEquals("/vanilla-diff/", WebDiff.routePrefix("/vanilla-diff/3"));
	}

	@Test
	void routePrefixHandlesTheTrailingSlashRouteVariant() {
		assertEquals("/monaco-page/", WebDiff.routePrefix("/monaco-page/5/"));
		assertEquals("/vanilla-diff/", WebDiff.routePrefix("/vanilla-diff/3/"));
	}
}
