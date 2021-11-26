package org.kohsuke.github;

import java.io.IOException;
import java.util.List;

public class GHRepositoryWrapper {
    private final GHRepository ghRepository;

    public GHRepositoryWrapper(GHRepository ghRepository) {
        this.ghRepository = ghRepository;
    }

    public GHCommit getCommit(String sha1, List<GHCommit.File> files) throws IOException {
        GitHubResponse<GHCommit> ghCommitGitHubResponse = getGhCommitGitHubResponse(String.format("/repos/%s/%s/commits/%s", ghRepository.getOwnerName(), ghRepository.getName(), sha1));
        GHCommit ghCommit = ghCommitGitHubResponse.body().wrapUp(ghRepository);
        files.addAll(ghCommit.getFiles());
        if (ghCommitGitHubResponse.headers().containsKey("Link")) {
            String linkHeaderField = ghCommitGitHubResponse.headerField("Link");
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
        }
        return ghCommit;
    }

    private GitHubResponse<GHCommit> getGhCommitGitHubResponse(String url) throws IOException {
        Requester requester = ghRepository.root().createRequest().withUrlPath(url);
        GitHubResponse<GHCommit> ghCommitGitHubResponse = requester.client.sendRequest(requester, (responseInfo) -> GitHubResponse.parseBody(responseInfo, GHCommit.class));
        return ghCommitGitHubResponse;
    }
}
