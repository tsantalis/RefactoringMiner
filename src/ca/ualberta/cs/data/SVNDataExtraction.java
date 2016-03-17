package ca.ualberta.cs.data;

import gr.uom.java.xmi.ASTReader;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.refactoringminer.api.Refactoring;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;


public class SVNDataExtraction extends AbstractDataExtraction {
	private final String SVN_URL = Constants.getValue("SVN_URL");
	private final String SVN_USERNAME = Constants.getValue("SVN_USERNAME");
	private final String SVN_PASSWORD = Constants.getValue("SVN_PASSWORD");
	private final String SVN_CHECKOUT_DIR = Constants.getValue("SVN_CHECKOUT_DIR");

	public SVNDataExtraction(boolean resume) {
		this.CHECKOUT_DIR = SVN_CHECKOUT_DIR;
		this.repository = loadRepository();
		if(this.repository == null) {
			this.repository = new AbstractRepository();
			extractDataFromRepositoryToObject(false);
		}
		if(this.repository != null && resume) {
			extractDataFromRepositoryToObject(true);
		}
	}

	private void extractDataFromRepositoryToObject(boolean resume) {
		try {  	   
			DAVRepositoryFactory.setup();
			SVNRepositoryFactoryImpl.setup();
			FSRepositoryFactory.setup(); 

			long startRevision = 0;
			long endRevision = -1; //HEAD (the latest) revision

			SVNRepository repository = null; 

			repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(SVN_URL));
			ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(SVN_USERNAME, SVN_PASSWORD);
			repository.setAuthenticationManager(authManager); 
			Collection<SVNLogEntry> logEntries = repository.log(new String[] {""}, null, startRevision, endRevision, true, true);
			if(resume) {
				List<SVNLogEntry> logEntriesToBeProcessed = new ArrayList<SVNLogEntry>();
				List<Commit> analyzedCommits = this.repository.getCommitList();
				Commit lastAnalyzedCommit = analyzedCommits.get(analyzedCommits.size()-1);
				for(SVNLogEntry logEntry : logEntries) {
					if(logEntry.getRevision() > Long.parseLong(lastAnalyzedCommit.getRevision()))
						logEntriesToBeProcessed.add(logEntry);
				}
				logEntries = logEntriesToBeProcessed;
			}

			for (Iterator<SVNLogEntry> entries = logEntries.iterator(); entries.hasNext();) {
				SVNLogEntry logEntry = (SVNLogEntry) entries.next();
				System.out.println("---------------------------------------------");
				System.out.println ("revision: " + logEntry.getRevision());
				System.out.println("author: " + logEntry.getAuthor());
				System.out.println("date: " + logEntry.getDate());
				System.out.println("log message: " + logEntry.getMessage());

				if(logEntry.getAuthor() != null) {
					SVNURL svnURL = SVNURL.parseURIDecoded(SVN_URL);
					SVNUpdateClient updateClient = new SVNUpdateClient(SVNWCUtil.createDefaultAuthenticationManager(SVN_USERNAME, SVN_PASSWORD), SVNWCUtil.createDefaultOptions(true));
					File directory = new File(CHECKOUT_DIR + File.separator + logEntry.getRevision());
					updateClient.doCheckout(svnURL, directory, SVNRevision.UNDEFINED, SVNRevision.create(logEntry.getRevision()), SVNDepth.INFINITY, true);
					
					boolean successfullyCompiled = compile(directory);


					Commit commit = new Commit(); 
					commit.setAuthor(logEntry.getAuthor());
					commit.setRevision(String.valueOf(logEntry.getRevision()));
					commit.setComment(logEntry.getMessage());

					commit.setSuccessfullyCompiled(successfullyCompiled);

					Date logEntryDate = logEntry.getDate();

					Calendar c = Calendar.getInstance();
					c.setTime(logEntryDate);

					int year = c.get(Calendar.YEAR);
					int month = c.get(Calendar.MONTH) + 1;
					int day = c.get(Calendar.DAY_OF_MONTH);  

					DateTime dateModified = new DateTime(year, month, day, 0, 0, 0, 0);

					commit.setDate(dateModified);

					this.repository.addCommit(commit);  

					if (logEntry.getChangedPaths().size() > 0) {
						ChangeSet changeset = new ChangeSet();
						Set changedPathsSet = logEntry.getChangedPaths().keySet();
						for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
							SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
							System.out.println(" "
									+ entryPath.getType()
									+ " "
									+ entryPath.getPath()
									+ ((entryPath.getCopyPath() != null ) ? " (from "
											+ entryPath.getCopyPath() + " revision "
											+ entryPath.getCopyRevision() + ")" : "" ));
							FileChange filechange = new FileChange(entryPath.getPath(), ChangeType.getSVNChangeType(entryPath.getType()));
							changeset.addFileChange(filechange);
						}
						commit.addChangeset(changeset);
					}
				}
				saveRepository();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void diffRevisions() {
		List<Commit> commits = repository.getCommitList();
		UMLModel model1 = null, model2 = null;
		String previousRevision = null;
		if(commits.get(0).isSuccessfullyCompiled()) {
			model1 = new ASTReader(new File(CHECKOUT_DIR + File.separator + commits.get(0).getRevision())).getUmlModel();
			previousRevision = commits.get(0).getRevision();
		}
		for(int i=1; i<commits.size(); i++) {
			//UMLModel model1 = new ASTReader(new File(CHECKOUT_DIR + File.separator + revisions.get(i))).getUmlModel();
			Commit nextCommit = commits.get(i);
			if(nextCommit.isSuccessfullyCompiled()) {
				model2 = new ASTReader(new File(CHECKOUT_DIR + File.separator + nextCommit.getRevision())).getUmlModel();
			}
			if(model1 != null && model2 != null && model1 != model2) {
				UMLModelDiff modelDiff = model1.diff(model2);
				List<Refactoring> refactorings = modelDiff.getRefactorings();
				//if(!refactorings.isEmpty())
					System.out.println(previousRevision + "\t" + nextCommit.getRevision() + "\t" + nextCommit.getDate().toString().substring(0,10) +
							"\t" + nextCommit.getAuthor());
				for(Refactoring refactoring : refactorings) {
					System.out.println(refactoring.toString());
				}
			}
			if(model2 != null && model1 != model2) {
				model1 = model2;
				previousRevision = nextCommit.getRevision();
			}
		}
	}
}
