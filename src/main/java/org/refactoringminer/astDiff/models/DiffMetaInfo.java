package org.refactoringminer.astDiff.models;

import gui.webdiff.dir.PullRequestReviewComment;

import java.util.List;
import java.util.Map;

public class DiffMetaInfo {
	final String info;
	final String url;
    Map<String, List<PullRequestReviewComment>> comments;

	public DiffMetaInfo(String info, String url) {
        this.info = info;
        this.url = url;
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

	@Override
	public String toString() {
		return "DiffMetaInfo{" +
				"info='" + info + '\'' +
				", url='" + url + '\'' +
				'}';
	}
}
