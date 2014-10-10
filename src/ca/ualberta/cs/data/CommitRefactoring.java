package ca.ualberta.cs.data;

import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

public class CommitRefactoring extends Commit {
	
	private List<Refactoring> refactorings;
	private List<ChangeSet> changesetList;
	private List<String> parentList;

	public void clone(Commit father){
		setAuthor(father.getAuthor());
		setComment(father.getComment());
		setCommitter(father.getCommitter());
		setDate(father.getDate());
		setRevision(father.getRevision());
		setSuccessfullyCompiled(father.isSuccessfullyCompiled());
		this.changesetList = father.getChangesetList();
		this.parentList = father.getParentList();
	}
	
	public void setRefactorings(List<Refactoring> refactorings){
		this.refactorings = refactorings;		
	}
	
	public List<Refactoring> getRefactorings(){
		return this.refactorings;
	}

	public List<ChangeSet> getChangeList(){
		return this.changesetList;
	}
	
	public List<String> getParentList(){
		return parentList;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Revision: ").append(getRevision()).append("\n");
		sb.append("Author: ").append(getAuthor()).append("\n");
		sb.append("Committer: ").append(getCommitter()).append("\n");
		sb.append("Date: ").append(getDate().toString().substring(0,10)).append("\n");
		sb.append("Comment: ").append("\n");
		sb.append(getComment()).append("\n");
		ChangeSet changeset = getChangeset();
		if(changeset != null) {
			sb.append("File Changes:").append("\n");
			for(FileChange fileChange : changeset.getFileChangeSet()) {
				sb.append(fileChange).append("\n");
			}
		}
		if(!refactorings.isEmpty()) {
			sb.append("Refactorings:").append("\n");
			for(Refactoring ref : refactorings) {
				sb.append(ref).append("\n");
			}
		}
		return sb.toString();
	}
}
