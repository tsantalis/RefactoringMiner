package ca.ualberta.cs.data;

import java.io.Serializable;

public class FileChange implements Serializable {
	 
	private String fileName;
	private ChangeType changeType;
	private String newFile;
	private int linesAdded; 
	private int linesRemoved;
	private int totalLines; 

	public FileChange (String fileName, ChangeType changeType) { 
		this.fileName = fileName;
		this.changeType = changeType;
	}

	public boolean equals(Object o) {
		if(this == o)
			return true;
		
		if(o instanceof FileChange) {
			FileChange fileChange = (FileChange)o;
			return this.fileName.equals(fileChange.fileName);
		}
		return false;
	}
	
	public int hashCode() {
		return fileName.hashCode();
	}
	
	public String getFileName() {
		return fileName;
	}

	public ChangeType getChangeType() {
		return changeType;
	}
	
	public String getNewFile() {
		return newFile;
	}

	public void setNewFile(String newFile) {
		this.newFile = newFile;
	}

	public int getLinesAdded() {
		return linesAdded;
	}

	public int getLinesRemoved() {
		return linesRemoved;
	}

	public int getTotalLines() {
		return totalLines;
	}

	public void setLinesAdded(int linesAdded) {
		this.linesAdded = linesAdded;
	}

	public void setLinesRemoved(int linesRemoved) {
		this.linesRemoved = linesRemoved;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}
 
	public String toString() {
		return changeType + " " + fileName;
	}
}
