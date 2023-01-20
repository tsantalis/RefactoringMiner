package org.refactoringminer.astDiff.utils;

import java.io.Serializable;

public class CaseInfo implements Serializable {
    String repo;
    String commit;

    public CaseInfo(String repo, String commit) {
        this.repo = repo;
        this.commit = commit;
    }

    public CaseInfo() {
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CaseInfo)) return false;
        CaseInfo second = (CaseInfo) obj;
        return second.getRepo().equals(this.getRepo()) &&
                second.getCommit().equals(this.getCommit());
    }
}