package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import gr.uom.java.xmi.diff.Refactoring;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;

import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringHandler;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RefactoringGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RevisionGit;

class AnalyzeProjectsHandler extends RefactoringHandler {
	protected final Database db;
	protected final ProjectGit project;

	AnalyzeProjectsHandler(Database db, ProjectGit project) {
		this.db = db;
		this.project = project;
	}

	@Override
	public boolean skipRevision(String curRevision) {
		return db.getRevisionById(project, curRevision) != null;
	}

	@Override
	public void handleDiff(RevCommit curRevision, List<Refactoring> refactorings) {
		RevisionGit revision = new RevisionGit();
		revision.setProjectGit(db.getProjectById(project.getId()));
		revision.setIdCommit(curRevision.getId().getName());
		revision.setAuthorName(curRevision.getAuthorIdent().getName());
		revision.setAuthorEmail(curRevision.getAuthorIdent().getEmailAddress());
		revision.setCommitterName(curRevision.getCommitterIdent().getName());
		revision.setCommitterEmail(curRevision.getCommitterIdent().getEmailAddress());
		revision.setEncoding(curRevision.getEncoding().name());
		revision.setIdCommitParent(curRevision.getParent(0).getId().getName());
		if (curRevision.getShortMessage().length() >= 4999) {
			revision.setShortMessage(curRevision.getShortMessage().substring(0, 4999));
		} else {
			revision.setShortMessage(curRevision.getShortMessage());
		}
		String fullMessage = curRevision.getFullMessage();
		if (fullMessage.length() > 10000) {
			revision.setFullMessage(fullMessage.substring(0, 10000));
		} else {
			revision.setFullMessage(fullMessage);
		}
		revision.setCommitTime(new java.util.Date((long) curRevision.getCommitTime() * 1000));

		Set<RefactoringGit> refactoringSet = new HashSet<RefactoringGit>();
		for (Refactoring refactoring : refactorings) {
			RefactoringGit refact = new RefactoringGit();
			refact.setRefactoringType(refactoring.getName());
			refact.setDescription(refactoring.toString());
			refact.setRevision(revision);
			refactoringSet.add(refact);
		}
		revision.setRefactorings(refactoringSet);
		db.insert(revision);
	}

	@Override
	public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
		project.setAnalyzed(true);
		project.setStatus("analyzed");
		project.setMachine(AnalyzeProjects.pid.getMachine());
		if (project.getCommits_count() <= 0) {
			project.setCommits_count(commitsCount);
		}
		project.setError_commits_count(errorCommitsCount);
		db.update(project);
	}
}