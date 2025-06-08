package org.refactoringminer.astDiff.models;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm1.PRComment;

import java.util.List;

public class DiffMetaInfo {
	final String info;
	final String url;
    final List<PRComment> comments;

	public DiffMetaInfo(String info, String url) {
		this(info, url, List.of());
	}


    public DiffMetaInfo(String info, String url, List<PRComment> comments) {
        this.info = info;
        this.url = url;
        this.comments = comments;
	}

    public String getInfo() {
        return info;
    }

    public List<PRComment> getComments() {
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
