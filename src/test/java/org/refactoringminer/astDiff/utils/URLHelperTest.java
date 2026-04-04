package org.refactoringminer.astDiff.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class URLHelperTest {

	@Test
	void pullRequestCommitUrlsRetainPullRequestContext() {
		String url = "https://github.com/owner/repo/pull/123/commits/abcdef1234567890?diff=split#discussion_r1";

		assertTrue(URLHelper.hasPullRequestContext(url));
		assertFalse(URLHelper.isPR(url));
		assertEquals(123, URLHelper.getPullRequestID(url));
	}

	@Test
	void pullRequestFilesUrlsStillParsePullRequestId() {
		String url = "https://github.com/owner/repo/pull/456/files";

		assertTrue(URLHelper.hasPullRequestContext(url));
		assertTrue(URLHelper.isPR(url));
		assertEquals(456, URLHelper.getPullRequestID(url));
	}
}
