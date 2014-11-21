package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.eclipse.jgit.revwalk.RevCommit;

public final class Revision {

	private final String id;
	private int time;

	public Revision(RevCommit commit) {
		super();
		this.id = commit.getId().getName();
		this.time = commit.getCommitTime();
	}

	public String getId() {
		return id;
	}

	public int getTime() {
		return time;
	}

}
