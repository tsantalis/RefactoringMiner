package ca.ualberta.cs.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

public class AbstractRepository implements Serializable {
	private List<Commit> commitList;
	
	public AbstractRepository() {
		commitList = new ArrayList<Commit>();
	}
	
	public void addCommit(Commit commit) {
		commitList.add(commit);
	}

	public void addCommitFirst(Commit commit) {
		commitList.add(0, commit);
	}

	public List<Commit> getCommitList() {
		return commitList;
	}  

	public Commit getCommit(String id) {
		for(Commit c : commitList) {
			if(c.getRevision().equals(id))
				return c;
		}
		return null;
	}

	public Set<String> getCommitters() {
		Set<String> committers = new LinkedHashSet<String>();
		for(Commit commit : commitList) {
			committers.add(commit.getAuthor());
		}
		return committers;
	}
	
	public LinkedHashSet<String> getExtensions() {
		LinkedHashSet<String> extensions = new LinkedHashSet<String>();
		for(Commit commit : commitList) {
			for(ChangeSet changeset : commit.getChangesetList()) {
				for(FileChange fc : changeset.getFileChangeSet()) {
					extensions.add(getFileExtension(fc.getFileName()));
				}
			}
		}
		return extensions;
	}
	
	private String getFileExtension(String fileName) { 
		return fileName.substring(fileName.lastIndexOf("."), fileName.length());
	}
	
	public DateTime getEarliestCommitDate() {
		DateTime earliestDate = null;
		for(Commit commit : commitList) {  
			DateTime commitDate = commit.getDate();
			if(commitDate != null && (earliestDate == null || (earliestDate.isAfter(commitDate)))) {
				earliestDate = commitDate;
			}
		}
		return earliestDate;
	}
	
	public DateTime getLatestCommitDate() {
		DateTime latestDate = null; 
		for(Commit commit : commitList) {  
			DateTime commitDate = commit.getDate(); 
			if(latestDate == null || (latestDate.isBefore(commitDate))) {
				latestDate = commitDate;
			}
		} 
		return latestDate;
	}

	public Map<String, Integer> getCommitCountPerAuthor() {
		Map<String, Integer> commitCountMap = new LinkedHashMap<String, Integer>();
		for(Commit commit : commitList) {
			String author = commit.getAuthor();
			if(commitCountMap.containsKey(author)) {
				commitCountMap.put(author, commitCountMap.get(author) + 1);
			}
			else {
				commitCountMap.put(author, 1);
			}
		}
		return commitCountMap;
	}
}
