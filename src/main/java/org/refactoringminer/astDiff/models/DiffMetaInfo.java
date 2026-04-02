package org.refactoringminer.astDiff.models;

import gui.webdiff.dir.PullRequestReviewComment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DiffMetaInfo {
	final String info;
	final String url;
    Map<String, List<PullRequestReviewComment>> comments;
    private String cloneURL;
    private String repositoryPath;
    private String commitId;
    private Integer selectedParentIndex;
    private Integer parentCount;
    private Integer timeout;

	public DiffMetaInfo(String info, String url) {
        this.info = info;
        this.url = url;
	}

    public DiffMetaInfo setCommitContext(String cloneURL, String repositoryPath, String commitId,
                                         Integer selectedParentIndex, Integer parentCount, Integer timeout) {
        this.cloneURL = cloneURL;
        this.repositoryPath = repositoryPath;
        this.commitId = commitId;
        this.selectedParentIndex = selectedParentIndex;
        this.parentCount = parentCount;
        this.timeout = timeout;
        return this;
    }

    public void setComments(Map<String, List<PullRequestReviewComment>> comments) {
        this.comments = comments;
    }

    public String getInfo() {
        return info;
    }

    public Map<String, List<PullRequestReviewComment>> getComments() {
        return comments;
    }

    public String getUrl() {
		return url;
	}

    public String getCloneURL() {
        return cloneURL;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public String getCommitId() {
        return commitId;
    }

    public Integer getSelectedParentIndex() {
        return selectedParentIndex;
    }

    public Integer getParentCount() {
        return parentCount;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public boolean hasUrl() {
        return url != null && !url.isEmpty();
    }

    public boolean supportsParentSelection() {
        return commitId != null &&
                parentCount != null &&
                parentCount > 1 &&
                ((cloneURL != null && !cloneURL.isEmpty()) || (repositoryPath != null && !repositoryPath.isEmpty()));
    }

    public List<Integer> getAvailableParentIndices() {
        if (!supportsParentSelection()) {
            return Collections.emptyList();
        }
        List<Integer> parentIndices = new ArrayList<>();
        for (int parentIndex = 0; parentIndex < parentCount; parentIndex++) {
            parentIndices.add(parentIndex);
        }
        return Collections.unmodifiableList(parentIndices);
    }

	@Override
	public String toString() {
		return "DiffMetaInfo{" +
				"info='" + info + '\'' +
				", url='" + url + '\'' +
                ", commitId='" + commitId + '\'' +
                ", selectedParentIndex=" + selectedParentIndex +
                ", parentCount=" + parentCount +
				'}';
	}
}
