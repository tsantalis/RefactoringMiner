package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;
import java.util.Date;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetector;
import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;
import br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetectorImpl;

public class AnalyzeNewCommits extends TaskWithProjectLock {

	private static Logger logger = LoggerFactory.getLogger(AnalyzeNewCommits.class);
	static Pid pid = new Pid();
	private Date startTime = new Date();
	
	public static void main(String[] args) {
		try {
			AnalyzeNewCommits task = new AnalyzeNewCommits(args);
			task.doTask(pid);
		} catch (Exception e) {
			logger.error("Fatal error", e);
		}
	}

	public AnalyzeNewCommits(String[] args) throws Exception {
		super(new Database());
		initWorkingDir(args, pid);
	}

	@Override
	protected ProjectGit findNextProject(Database db, Pid pid) throws Exception {
		return db.findProjectToMonitorAndLock(pid.toString(), startTime);
	}

	@Override
	protected void doTask(Database db, Pid pid, ProjectGit project) throws Exception {
		final Database db1 = db;
		GitService gitService = new GitServiceImpl();
		File projectFile = new File(this.workingDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl());
		
		GitHistoryRefactoringDetector detector = new GitHistoryRefactoringDetectorImpl();
		detector.fetchAndDetectNew(repo, new AnalyzeNewCommitsHandler(db1, project));
		repo.close();
	}

}
