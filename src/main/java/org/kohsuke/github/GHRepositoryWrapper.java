package org.kohsuke.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.github.GHPullRequestReviewComment.Side;

public class GHRepositoryWrapper {
    private final GHRepository ghRepository;

    public GHRepositoryWrapper(GHRepository ghRepository) {
        this.ghRepository = ghRepository;
    }

    public GHCommit getCommit(String sha1, List<GHCommit.File> files) throws IOException {
        GitHubResponse<GHCommit> ghCommitGitHubResponse = getGhCommitGitHubResponse(String.format("/repos/%s/%s/commits/%s", ghRepository.getOwnerName(), ghRepository.getName(), sha1));
        GHCommit ghCommit = ghCommitGitHubResponse.body().wrapUp(ghRepository);
        files.addAll(ghCommit.getFiles());
        /*if (ghCommitGitHubResponse.header("Link") != null) {
            String linkHeaderField = ghCommitGitHubResponse.header("Link");
            if (linkHeaderField != null) {
                String[] links = linkHeaderField.split(",");
                if (links.length == 2) {
                    String[] link = links[1].split(";");
                    if (link.length == 2) {
                        String url = link[0];
                        url = url.replaceFirst("<", "").replaceFirst(">", "");
                        String[] urlParts = url.split("=");
                        if (urlParts.length == 2) {
                            url = urlParts[0] + "=";
                            url = url.trim();
                            int lastPage = Integer.valueOf(urlParts[1]);
                            for (int page = 2; page <= lastPage; page++) {
                                files.addAll(getGhCommitGitHubResponse(url + page).body().getFiles());
                            }
                        }
                    }
                }
            }
        }*/
        return ghCommit;
    }

    public List<GHPullRequestFileDetail> listPullRequestFiles(int pullRequestId) throws IOException {
    	GHPullRequest pullRequest = ghRepository.getPullRequest(pullRequestId);
    	return listPullRequestFiles(pullRequest);
    }

	public List<GHPullRequestFileDetail> listPullRequestFiles(GHPullRequest pullRequest) throws IOException {
		List<GHPullRequestFileDetail> files = new ArrayList<>();
		Requester requester = ghRepository.root().createRequest()
        .withUrlPath(String.format("%s/files", pullRequest.getApiRoute()))
        .with("per_page", 100);
		GitHubResponse<GHPullRequestFileDetail[]> gitHubResponse = requester.client.sendRequest(requester, (responseInfo) -> GitHubResponse.parseBody(responseInfo, GHPullRequestFileDetail[].class));
		for(GHPullRequestFileDetail file : gitHubResponse.body()) {
			files.add(file);
		}
		if (gitHubResponse.header("Link") != null) {
            String linkHeaderField = gitHubResponse.header("Link");
            if (linkHeaderField != null) {
                String[] links = linkHeaderField.split(",");
                if (links.length == 2) {
                    String[] link = links[1].split(";");
                    if (link.length == 2) {
                        String url = link[0];
                        url = url.replaceFirst("<", "").replaceFirst(">", "");
                        int index = url.lastIndexOf("=");
                        if (index != -1) {
                            String trimmedUrl = url.substring(0, index);
                            trimmedUrl = trimmedUrl.trim();
                            int lastPage = Integer.valueOf(url.substring(index+1));
                            for (int page = 2; page <= lastPage; page++) {
                            	pullRequestGitHubResponse(trimmedUrl + page, files);
                            }
                        }
                    }
                }
            }
        }
		return files;
	}

	private void pullRequestGitHubResponse(String url, List<GHPullRequestFileDetail> files) throws IOException {
		Requester requester = ghRepository.root().createRequest().withUrlPath(url);
		GitHubResponse<GHPullRequestFileDetail[]> gitHubResponse = requester.client.sendRequest(requester, (responseInfo) -> GitHubResponse.parseBody(responseInfo, GHPullRequestFileDetail[].class));
		for(GHPullRequestFileDetail file : gitHubResponse.body()) {
			files.add(file);
		}
	}

    private GitHubResponse<GHCommit> getGhCommitGitHubResponse(String url) throws IOException {
        Requester requester = ghRepository.root().createRequest().withUrlPath(url);
        GitHubResponse<GHCommit> ghCommitGitHubResponse = requester.client.sendRequest(requester, (responseInfo) -> GitHubResponse.parseBody(responseInfo, GHCommit.class));
        return ghCommitGitHubResponse;
    }

    public Pair<Side, Integer> getGhPullRequestReviewCommentLine(String url) throws IOException {
        Requester requester = ghRepository.root().createRequest().withUrlPath(url);
        GitHubResponse<GHPullRequestReviewComment> ghPullRequestReviewCommentResponse = requester.client.sendRequest(requester, (responseInfo) -> GitHubResponse.parseBody(responseInfo, GHPullRequestReviewComment.class));
        int line = ghPullRequestReviewCommentResponse.body().getLine();
        Side side = ghPullRequestReviewCommentResponse.body().getSide();
		return Pair.of(side, line);
    }
}
