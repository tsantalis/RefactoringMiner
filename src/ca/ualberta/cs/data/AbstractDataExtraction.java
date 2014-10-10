package ca.ualberta.cs.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

public abstract class AbstractDataExtraction {
	protected AbstractRepository repository;
	protected String CHECKOUT_DIR;
	//private List<CommitRefactoring> commitRefactorings;
	//private Map<DateTime, Integer> data;

	public AbstractRepository getRepository() {
		return repository;
	}
	
	protected AbstractRepository loadRepository() {
		AbstractRepository repository = null;
		try {
			FileInputStream fin = new FileInputStream(CHECKOUT_DIR + File.separator + "repository.ser");
			ObjectInputStream ois = new ObjectInputStream(fin);
			repository = (AbstractRepository)ois.readObject();
			ois.close();
		}
		catch(ClassNotFoundException cnfe) { cnfe.printStackTrace(); }
		catch(IOException ioe) { ioe.printStackTrace(); }
		return repository;
	}

	protected void saveRepository() {
		try {
			FileOutputStream fout = new FileOutputStream(CHECKOUT_DIR + File.separator + "repository.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(repository);
			oos.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}

	/*private List<CommitRefactoring> loadCommitRefactorings() {
		List<CommitRefactoring> data = null;
		try {
			FileInputStream fin = new FileInputStream(CHECKOUT_DIR + File.separator + "repository-refactorings.ser");
			ObjectInputStream ois = new ObjectInputStream(fin);
			data = (List<CommitRefactoring>)ois.readObject();
			ois.close();
		}
		catch(ClassNotFoundException cnfe) { cnfe.printStackTrace(); }
		catch(IOException ioe) { ioe.printStackTrace(); }
		return data;
	}
	
	private void saveCommitRefactorings() {
		try {
			FileOutputStream fout = new FileOutputStream(CHECKOUT_DIR + File.separator + "repository-refactorings.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(commitRefactorings);
			oos.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}*/

	protected boolean compile(File directory) {
		String[] fileNames = directory.list();
		boolean maven = false;
		boolean ant = false;
		for(String fileName : fileNames) {
			if(fileName.equalsIgnoreCase("pom.xml")) {
				maven = true;
			}
			else if(fileName.equalsIgnoreCase("build.xml")) {
				ant = true;
			}
		}
		boolean successfullyCompiled = false;
		if(maven) {
			Builder builder = new MavenBuilder();
			successfullyCompiled = builder.build(directory);
		}
		/*else if(ant) {
			Builder builder = new AntBuilder();
			successfullyCompiled = builder.build(directory);
		}*/
		else {
			Builder builder = new JDKBuilder();
			successfullyCompiled = builder.build(directory);
		}
		return successfullyCompiled;
	}

	public abstract void diffRevisions();

	public Map<DateTime, Integer> detectTestPeriods() {
		Map<DateTime, Integer> testCommitCountMap = new LinkedHashMap<DateTime, Integer>();
		
		for(Commit commit : repository.getCommitList()) {
			DateTime date = commit.getDate();
			List<ChangeSet> list = commit.getChangesetList();
			if(list.size() > 0) {
				ChangeSet changeset = list.get(0);
				boolean found = false;
				for(FileChange fileChange : changeset.getFileChangeSet()) {
					if((fileChange.getChangeType().equals(ChangeType.ADD) || fileChange.getChangeType().equals(ChangeType.MODIFY)) && 
							(fileChange.getFileName().contains("Test") || fileChange.getFileName().contains("Mock"))) {
						if(testCommitCountMap.containsKey(date)) {
							testCommitCountMap.put(date, testCommitCountMap.get(date) + 1);
						}
						else {
							testCommitCountMap.put(date, 1);
						}
						found = true;
						break;
					}
				}
				if(!found)
					testCommitCountMap.put(date, 0);
			}
		}
		return testCommitCountMap;
	}

	public Map<DateTime, Integer> detectTestAdditionPeriods() {
		Map<DateTime, Integer> testCommitCountMap = new LinkedHashMap<DateTime, Integer>();
		for(Commit commit : repository.getCommitList()) {
			DateTime date = commit.getDate();
			testCommitCountMap.put(date, 0);
		}
		
		for(Commit commit : repository.getCommitList()) {
			DateTime date = commit.getDate();
			List<ChangeSet> list = commit.getChangesetList();
			if(list.size() > 0) {
				ChangeSet changeset = list.get(0);
				for(FileChange fileChange : changeset.getFileChangeSet()) {
					if(fileChange.getChangeType().equals(ChangeType.ADD) && 
							fileChange.getFileName().contains("Test") ) {
						if(testCommitCountMap.containsKey(date)) {
							testCommitCountMap.put(date, testCommitCountMap.get(date) + 1);
						}
						else {
							testCommitCountMap.put(date, 1);
						}
					}
				}
			}
		}
		return testCommitCountMap;
	}
	public Map<DateTime, Integer> detectTestModificationPeriods() {
		Map<DateTime, Integer> testCommitCountMap = new LinkedHashMap<DateTime, Integer>();
		for(Commit commit : repository.getCommitList()) {
			DateTime date = commit.getDate();
			testCommitCountMap.put(date, 0);
		}
		
		for(Commit commit : repository.getCommitList()) {
			DateTime date = commit.getDate();
			List<ChangeSet> list = commit.getChangesetList();
			if(list.size() > 0) {
				ChangeSet changeset = list.get(0);
				for(FileChange fileChange : changeset.getFileChangeSet()) {
					if(fileChange.getChangeType().equals(ChangeType.MODIFY) && fileChange.getFileName().contains("Test") ) {
						if(testCommitCountMap.containsKey(date)) {
							testCommitCountMap.put(date, testCommitCountMap.get(date) + 1);
						}
						else {
							
						}
					}
				}
			}
		}
		return testCommitCountMap;
	}
}
