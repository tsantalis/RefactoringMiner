package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.eclipse.jgit.revwalk.RevCommit;

public final class Revision {

	private final String id;
	private int time;
	private boolean current = false;

	public Revision(RevCommit commit, boolean current) {
		super();
		this.id = commit.getId().getName();
		this.time = commit.getCommitTime();
		this.current = current;
	}

	public String getId() {
		return id;
	}

	public int getTime() {
		return time;
	}

	public boolean isCurrent() {
		return current;
	}

}
