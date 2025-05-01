package org.refactoringminer.astDiff.models;

public class DiffMetaInfo {
	final String info;
	final String url;

	public DiffMetaInfo(String info, String url) {
		this.info = info;
		this.url = url;
	}

	public String getInfo() {
		return info;
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
