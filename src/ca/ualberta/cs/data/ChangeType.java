package ca.ualberta.cs.data;

import java.io.Serializable;

public enum ChangeType implements Serializable {
	ADD, COPY, DELETE, MODIFY, RENAME, RENAME_MODIFY, MOVE_MODIFY, RENAME_MOVE_MODIFY, MOVE;
	
	public String toString() {
		return name();
	}
	
	public static ChangeType getGitChangeType(String gitChangeType) throws Exception {
		if(gitChangeType.equals("ADD"))
			return ADD;
		else if(gitChangeType.equals("COPY"))
			return COPY;
		else if(gitChangeType.equals("DELETE"))
			return DELETE;
		else if(gitChangeType.equals("MODIFY"))
			return MODIFY;
		else if(gitChangeType.equals("RENAME"))
			return RENAME;
		else
			throw new Exception("Unknown Git Type");
	}

	public static ChangeType getSVNChangeType(char SVNChangeType) throws Exception {
		switch(SVNChangeType) {
		case 'A':
			return ADD;
		case 'M':
			return MODIFY;
		case 'D':
			return DELETE;
		case 'R':
			return RENAME;
		default: 
			throw new Exception("Unknown SVN Type");
		} 
	}
	
	public static ChangeType getJazzChangeType(String SVNChangeType) throws Exception {
		String changetype = SVNChangeType.toUpperCase().replace("-", "").trim();  
		
		if(changetype.equals("1")) {
			return ADD;
		} else if(changetype.equals("2")) {
			return MODIFY;
		} else if(changetype.equals("4")) {
			return RENAME;
		} else if(changetype.equals("6")) {
			return RENAME_MODIFY ;
		} else if(changetype.equals("8")) {
			return MOVE ;
		} else if (changetype.equals("10")) {
			return MOVE_MODIFY;
		} else if (changetype.equals("14")) {
			return RENAME_MOVE_MODIFY;
		} else if (changetype.equals("16")) {
			return DELETE;
		} 
		
		System.out.println("Null changeType... change type not mapped... " + changetype);
		//if no cases match return null, this should never happen.
		return null; 
	}
}
