package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import gr.uom.java.xmi.UMLModelSet;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.GitService;
import br.ufmg.dcc.labsoft.refactoringanalyzer.GitServiceImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetector;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetectorImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringHandler;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RefactoringGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RevisionGit;

public class GitProjectAnalyzer {

	private Logger logger = LoggerFactory.getLogger(GitProjectAnalyzer.class);
	private File workingDir;
	private Database db;

	public GitProjectAnalyzer(File workingDir, Database db) {
		this.workingDir = workingDir;
		this.db = db;
	}

	public GitProjectAnalyzer() {
		this(new File("tmp"), new Database());
	}

	public static void main(String[] args) throws Exception {
		GitProjectAnalyzer analyzer = new GitProjectAnalyzer();
		for (String cloneUrl : args) {
			analyzer.analyzeProject(cloneUrl);
		}
	}

	public void analyzeProject(String cloneUrl) throws Exception {
		final ProjectGit project = db.getProjectByCloneUrl(cloneUrl);
		if (project == null) {
			throw new IllegalArgumentException("Project not found in database: " + cloneUrl);
		}
		this.analyzedProject(project);
	}

	public void analyzedProject(final ProjectGit project)
			throws Exception {
		if (project.isAnalyzed()) {
			logger.info("Project already analyzed: {}", project.getCloneUrl());
			return;
		}

		GitService gitService = new GitServiceImpl();
		File projectFile = new File(workingDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl(), project.getDefault_branch());

		RefactoringDetector detector = new RefactoringDetectorImpl();
		detector.detectAll(repo, new RefactoringHandler() {
			@Override
			public boolean skipRevision(RevCommit curRevision) {
				return db.getRevisionById(project, curRevision.getId().getName()) != null;
			}
			@Override
			public void handleDiff(UMLModelSet prevModel, String commitId, RevCommit curRevision, UMLModelSet curModel, List<Refactoring> refactorings) {
				RevisionGit revision = new RevisionGit();
				revision.setProjectGit(project);
				revision.setIdCommit(curRevision.getId().getName());
				revision.setAuthorName(curRevision.getAuthorIdent().getName());
				revision.setAuthorEmail(curRevision.getAuthorIdent().getEmailAddress());
				revision.setEncoding(curRevision.getEncoding().name());
				revision.setIdCommitParent(curRevision.getParent(0).getId().getName());
				if (curRevision.getShortMessage().length() >= 4999) {
					revision.setShortMessage(curRevision.getShortMessage().substring(0, 4999));
				} else {
					revision.setShortMessage(curRevision.getShortMessage());
				}
				revision.setFullMessage(curRevision.getFullMessage());
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
				if (project.getCommits_count() <= 0) {
					project.setCommits_count(commitsCount);
				}
				//project.setMerge_commits_count(mergeCommitsCount);
				project.setError_commits_count(errorCommitsCount);
				//project.setRunning_pid(null);
				db.update(project);
			}
		});
	}

}
