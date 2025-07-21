package org.kohsuke.github;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.github.GHPullRequestReviewComment.Side;

public class GHRepositoryWrapper {
    private final GHRepository ghRepository;

    public GHRepositoryWrapper(GHRepository ghRepository) {
        this.ghRepository = ghRepository;
    }

    public Pair<Side, Integer> getGhPullRequestReviewCommentLine(String url) throws IOException {
        Requester requester = ghRepository.root().createRequest().withUrlPath(url);
        GitHubResponse<GHPullRequestReviewComment> ghPullRequestReviewCommentResponse = requester.client.sendRequest(requester, (responseInfo) -> GitHubResponse.parseBody(responseInfo, GHPullRequestReviewComment.class));
        int line = ghPullRequestReviewCommentResponse.body().getLine();
        Side side = ghPullRequestReviewCommentResponse.body().getSide();
		return Pair.of(side, line);
    }
}
