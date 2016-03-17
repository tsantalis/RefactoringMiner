package ca.ualberta.cs.data;

import gr.uom.java.xmi.ASTReader;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.joda.time.DateTime;
import org.refactoringminer.api.Refactoring;

public class GitDataExtraction extends AbstractDataExtraction {

	private final String gitPath = "C:\\repos\\junit";
	public GitDataExtraction(boolean resume) {
		this.CHECKOUT_DIR = Constants.getValue("GIT_CHECKOUT_DIR");
		this.repository = loadRepository();
		if(this.repository == null) {
			this.repository = new AbstractRepository();
			extractDataFromRepositoryToObject();
			//saveRepository();
		}
		if(this.repository != null && resume) {
			extractDataFromRepositoryToObject();
		}
		/*else {
			for(Commit commit : repository.getCommitList()) {
				if(commit.getComment().equals("Deleted deprecated methods and classes, this time really\n"))
					break;
				System.out.println(commit.getAuthor() + "\t" + commit.getDate() + "\t" + commit.getComment());
				File dest = new File(CHECKOUT_DIR + File.separator + commit.getRevision());
				boolean successfullyCompiled = compile(dest);
				commit.setSuccessfullyCompiled(successfullyCompiled);
			}
			saveRepository();
		}*/
	}

	private void extractDataFromRepositoryToObject() {
		try {
			RepositoryBuilder builder = new RepositoryBuilder();
			org.eclipse.jgit.lib.Repository repository = builder.setGitDir(new File(gitPath + File.separator + ".git"))
					.readEnvironment() // scan environment GIT_* variables
					.findGitDir() // scan up the file system tree
					.build();

			Git git = new Git(repository);
			RevWalk walk = new RevWalk(repository);
			Iterable<RevCommit> logs = git.log().call();
			Iterator<RevCommit> i = logs.iterator();

			while (i.hasNext()) {
				RevCommit commit = walk.parseCommit(i.next());
				ObjectId id = commit.getId();
				int time = commit.getCommitTime();
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(time * 1000L);

				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				int day = calendar.get(Calendar.DAY_OF_MONTH);  

				DateTime commitDate = new DateTime(year, month, day, 0, 0, 0, 0);
				
				PersonIdent author = commit.getAuthorIdent();
				PersonIdent committer = commit.getCommitterIdent();
				System.out.println("---------------------------------------------");
				System.out.println("Hash id: " + id.getName());
				System.out.println("Author: " + author.getName());
				System.out.println("Date: " + commitDate);
				System.out.println("Commiter: " + committer.getName());
				System.out.println(commit.getFullMessage());
				
				Commit c = new Commit(id.getName(), commitDate, author.getName(), commit.getFullMessage());
				c.setCommitter(committer.getName());
				
				RevCommit[] parents = commit.getParents();
				for(RevCommit parent : parents) {
					ObjectId parentId = parent.getId();
					c.addParent(parentId.getName());
					TreeWalk treeWalk = new TreeWalk(repository);
					treeWalk.addTree(new RevWalk(repository).parseTree(parentId));
					treeWalk.addTree(new RevWalk(repository).parseTree(id));
					treeWalk.setRecursive(true);
					List<DiffEntry> diffs = DiffEntry.scan(treeWalk);
					ChangeSet changeSet = new ChangeSet();
					for(DiffEntry diff : diffs) {
						ChangeType changeType = ChangeType.getGitChangeType(diff.getChangeType().toString());
						FileChange fileChange = null;
						if(changeType.equals(ChangeType.ADD)) {
							fileChange = new FileChange(diff.getNewPath(), changeType);
						}
						else if(changeType.equals(ChangeType.DELETE) || changeType.equals(ChangeType.MODIFY)) {
							fileChange = new FileChange(diff.getOldPath(), changeType);
						}
						else {
							//change type is COPY or RENAME
							fileChange = new FileChange(diff.getOldPath(), changeType);
							fileChange.setNewFile(diff.getNewPath());
						}
						changeSet.addFileChange(fileChange);
						//System.out.println(diff.getChangeType() + "\t" + diff.getOldPath() + "\t" + diff.getNewPath());
					}
					c.addChangeset(changeSet);
				}
				
				CheckoutCommand checkout = git.checkout().setStartPoint(commit).setName(id.getName());
				checkout.call();
				File dest = new File(CHECKOUT_DIR + File.separator + id.getName());
				FileUtils.copyDirectory(new File(gitPath), dest);
				boolean successfullyCompiled = compile(dest);
				c.setSuccessfullyCompiled(successfullyCompiled);
				
				this.repository.addCommitFirst(c);
				saveRepository();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void diffRevisions() {
		Map<String, List<Refactoring>> refactoringsPerAuthorMap = new LinkedHashMap<String, List<Refactoring>>();
		Map<String, Map<String, Integer>> refactoringCountPerAuthorMap = new LinkedHashMap<String, Map<String, Integer>>();
		
		/*this.commitRefactorings = loadCommitRefactorings();
		if(this.commitRefactorings == null) {
			this.commitRefactorings = new ArrayList<CommitRefactoring>();
		}
		else {
			int count = 0;
			for(CommitRefactoring ref : commitRefactorings) {
				count += ref.getRefactorings().size();
			}
			System.out.println(count);
			return;
		}*/
		
		for(Commit currentCommit : repository.getCommitList()) {
			if(currentCommit.getParentList().size() == 1) {
				for(String parentId : currentCommit.getParentList()) {
					Commit parentCommit = repository.getCommit(parentId);
					if(parentCommit != null /*&& currentCommit.isSuccessfullyCompiled() && parentCommit.isSuccessfullyCompiled()*/) {
						boolean sameAuthorCommiter = true;
						if(currentCommit.getCommitter() != null) {
							if(!currentCommit.getCommitter().equals(currentCommit.getAuthor()))
								sameAuthorCommiter = false;
						}
						if(sameAuthorCommiter) {
							String parentRevision = parentCommit.getRevision();
							String currentRevision = currentCommit.getRevision();
							UMLModel parentModel = new ASTReader(new File(CHECKOUT_DIR + File.separator + parentRevision)).getUmlModel();
							UMLModel currentModel = new ASTReader(new File(CHECKOUT_DIR + File.separator + currentRevision)).getUmlModel();
							UMLModelDiff modelDiff = parentModel.diff(currentModel);
							List<Refactoring> refactorings = modelDiff.getRefactorings();
							/*CommitRefactoring cr = new CommitRefactoring();
							cr.clone(currentCommit);
							cr.setRefactorings(refactorings);
							commitRefactorings.add(cr);
							System.out.println(cr.toString());*/
							String date = currentCommit.getDate().toString().substring(0,10);
							String author = currentCommit.getAuthor();
							System.out.println(parentRevision + "\t" + currentRevision + "\t" + date + "\t" + author);
							if(!refactorings.isEmpty()) {
								/*String date = currentCommit.getDate().toString().substring(0,10);
								String author = currentCommit.getAuthor();
								System.out.println(parentRevision + "\t" + currentRevision + "\t" + date + "\t" + author);*/
								//------------------------------------------------------
								if(refactoringsPerAuthorMap.containsKey(author)) {
									refactoringsPerAuthorMap.get(author).addAll(refactorings);
								}
								else {
									ArrayList<Refactoring> list = new ArrayList<Refactoring>();
									list.addAll(refactorings);
									refactoringsPerAuthorMap.put(author, list);
								}
								//------------------------------------------------------
								Map<String, Integer> refactoringCountMap = new LinkedHashMap<String, Integer>();
								int refactoringCount = 0;
								for(Refactoring refactoring : refactorings) {
									//if(!refactoring.toString().contains("junit.tests.")) {
										refactoringCount++;
										System.out.println(refactoring.toString());
										if(refactoringCountMap.containsKey(refactoring.getName())) {
											refactoringCountMap.put(refactoring.getName(), refactoringCountMap.get(refactoring.getName()) + 1);
										}
										else {
											refactoringCountMap.put(refactoring.getName(), 1);
										}
									//}
								}
								//System.out.println(author + "\t" + date + "\t" + refactoringCount + "\t" + refactoringCountMap);
								//System.out.println(author + "\t" + date + "\t" + refactorings.size() + "\t" + refactoringCountMap);
								if(refactoringCountPerAuthorMap.containsKey(author)) {
									Map<String, Integer> previousMap = refactoringCountPerAuthorMap.get(author);
									for(String refactoringName : refactoringCountMap.keySet()) {
										if(previousMap.containsKey(refactoringName)) {
											previousMap.put(refactoringName, previousMap.get(refactoringName) + refactoringCountMap.get(refactoringName));
										}
										else {
											previousMap.put(refactoringName, refactoringCountMap.get(refactoringName));
										}
									}
								}
								else {
									Map<String, Integer> map = new LinkedHashMap<String, Integer>();
									for(String refactoringName : refactoringCountMap.keySet()) {
										map.put(refactoringName, refactoringCountMap.get(refactoringName));
									}
									refactoringCountPerAuthorMap.put(author, map);
								}
								//------------------------------------------------------
							}
						}
					}
				}
			}
		}
		/*for(String key : refactoringCountPerAuthorMap.keySet()) {
			System.out.println(key + "\t" + refactoringCountPerAuthorMap.get(key));
		}*/
		//saveCommitRefactorings();
	}
}
