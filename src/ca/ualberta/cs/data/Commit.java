package ca.ualberta.cs.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

public class Commit implements Serializable {
	
	private String revision;
	private DateTime date;
	private String author;
	private String comment;
	private List<ChangeSet> changesetList;
	private String committer;
	private boolean successfullyCompiled;
	private List<String> parentList;
	
	public Commit() {
		changesetList = new ArrayList<ChangeSet>();
		parentList = new ArrayList<String>();
	}

	public Commit(String revision, DateTime date, String author, String comment) {
		this.revision = revision;
		this.date = date;
		this.author = author;
		this.comment = comment;
		changesetList = new ArrayList<ChangeSet>();
		parentList = new ArrayList<String>();
	}

	public boolean isMerge() {
		if(changesetList.size() > 1)
			return true;
		return false;
	}

	public ChangeSet getChangeset() {
		if(!changesetList.isEmpty())
			return changesetList.get(0);
		return null;
	}

	public void addParent(String parent) {
		this.parentList.add(parent);
	}

	public List<String> getParentList() {
		return parentList;
	}

	public String getCommitter() {
		return committer;
	}

	public void setCommitter(String committer) {
		this.committer = committer;
	}

	public boolean isSuccessfullyCompiled() {
		return successfullyCompiled;
	}

	public void setSuccessfullyCompiled(boolean successfullyCompiled) {
		this.successfullyCompiled = successfullyCompiled;
	}

	public String getRevision() {
		return revision;
	}

	public DateTime getDate() {
		return date;
	}

	public String getAuthor() {
		return author;
	}

	public String getComment() {
		return comment;
	}

	public List<ChangeSet> getChangesetList() {
		return changesetList;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public void setDate(DateTime date) {
		this.date = date;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void addChangeset(ChangeSet changeset) {
		this.changesetList.add(changeset);
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		
		if(o instanceof Commit) {
			Commit commit = (Commit) o;
			return this.revision.equals(commit.revision);
		}
		return false;
	}
	
	public int hashCode() {
		return revision.hashCode();
	}
	
	public String toString() {
		return revision + " " + author + " " + date +  " " + comment + " ";
	}

}
